@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.*
import io.github.mirrgieriana.xarpite.xarpeg.text
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// Variable table with inheritance (parent scope lookup)
data class VariableTable(
    val variables: MutableMap<String, Value> = mutableMapOf(),
    val parent: VariableTable? = null
) {
    fun get(name: String): Value? {
        return variables[name] ?: parent?.get(name)
    }
    
    fun set(name: String, value: Value) {
        variables[name] = value
    }
    
    fun createChild(): VariableTable {
        return VariableTable(mutableMapOf(), this)
    }
}

// Evaluation context that holds call stack information and variable scope
data class EvaluationContext(
    val callStack: List<CallFrame> = emptyList(),
    val sourceCode: String? = null,
    val variableTable: VariableTable = VariableTable()
) {
    fun pushFrame(functionName: String, callPosition: SourcePosition): EvaluationContext {
        return copy(callStack = callStack + CallFrame(functionName, callPosition))
    }
    
    fun withNewScope(): EvaluationContext {
        return copy(variableTable = variableTable.createChild())
    }
}

// Represents a single call frame in the stack
data class CallFrame(val functionName: String, val position: SourcePosition)

// Represents a position in the source code
data class SourcePosition(val start: Int, val end: Int, val text: String) {
    fun formatLineColumn(source: String): String {
        val beforeStart = source.substring(0, start)
        val line = beforeStart.count { it == '\n' } + 1
        val column = start - (beforeStart.lastIndexOf('\n') + 1) + 1
        return "line $line, column $column"
    }
}

// Value types that can be stored in variables
sealed class Value {
    data class NumberValue(val value: Double) : Value() {
        override fun toString() = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
    data class BooleanValue(val value: Boolean) : Value() {
        override fun toString() = value.toString()
    }
    data class LambdaValue(
        val params: List<String>,
        val body: (EvaluationContext) -> Value,
        val capturedVars: MutableMap<String, Value>,
        val name: String? = null,
        val definitionPosition: SourcePosition? = null
    ) : Value() {
        override fun toString() = "<lambda(${params.joinToString(", ")})>"
    }
}

// Custom exception that includes call stack
class EvaluationException(
    message: String,
    val context: EvaluationContext? = null,
    val sourceCode: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    fun formatWithCallStack(): String {
        val sb = StringBuilder()
        sb.append("Error: $message")
        
        if (context != null && context.callStack.isNotEmpty()) {
            context.callStack.asReversed().forEach { frame ->
                val location = if (sourceCode != null) {
                    frame.position.formatLineColumn(sourceCode)
                } else {
                    "position ${frame.position.start}-${frame.position.end}"
                }
                sb.append("\n  at $location: ${frame.position.text}")
            }
        }
        
        return sb.toString()
    }
}

private object ExpressionGrammar {
    private val whitespace = -Regex("[ \\t\\r\\n]*")

    // Identifier: alphanumeric and _, but first character cannot be a digit
    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value }

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { Value.NumberValue(it.value.toDouble()) }
    
    // Function call counter to prevent infinite recursion
    var functionCallCount = 0
    private const val MAX_FUNCTION_CALLS = 100
    
    // Helper function for left-associative binary operator aggregation
    // Takes a term parser and operators that return (left, ctx) -> result functions
    private fun leftAssociativeBinaryOp(
        term: Parser<(EvaluationContext) -> Value>,
        operators: Parser<(Value, EvaluationContext) -> Value>
    ): Parser<(EvaluationContext) -> Value> {
        return (term * operators.zeroOrMore) map { (first, rest) ->
            { ctx: EvaluationContext ->
                var result = first(ctx)
                for (opFunc in rest) {
                    result = opFunc(result, ctx)
                }
                result
            }
        }
    }

    // Variable reference
    private val variableRef: Parser<(EvaluationContext) -> Value> = identifier map { name ->
        { ctx -> 
            ctx.variableTable.get(name) ?: throw EvaluationException("Undefined variable: $name", ctx, ctx.sourceCode)
        }
    }

    // Helper to parse comma-separated list of identifiers
    private val identifierList: Parser<List<String>> = run {
        val restItem = whitespace * -',' * whitespace * identifier
        (identifier * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Lambda parameter list: (param1, param2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty parameter lists: ()
    private val paramList: Parser<List<String>> = 
        -'(' * whitespace * (identifierList + (whitespace map { emptyList<String>() })) * whitespace * -')'

    // Lambda expression: (param1, param2, ...) -> body
    private val lambda: Parser<(EvaluationContext) -> Value> =
        ((paramList * whitespace * -Regex("->") * whitespace * ref { expression }) mapEx { parseCtx, result ->
            val (params, bodyParser) = result.value
            val lambdaText = result.text(parseCtx)
            val position = SourcePosition(result.start, result.end, lambdaText)
            val evalFunc: (EvaluationContext) -> Value = { ctx: EvaluationContext ->
                // Don't capture variables - use dynamic scoping to allow recursion
                // The lambda will see whatever is in scope when it's called
                Value.LambdaValue(params, bodyParser, mutableMapOf(), definitionPosition = position)
            }
            evalFunc
        })

    // Helper to parse comma-separated list of expressions
    private val exprList: Parser<List<(EvaluationContext) -> Value>> = run {
        val restItem = whitespace * -',' * whitespace * ref { expression }
        (ref { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Argument list for function calls: (arg1, arg2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty argument lists: ()
    private val argList: Parser<List<(EvaluationContext) -> Value>> =
        -'(' * whitespace * (exprList + (whitespace map { emptyList<(EvaluationContext) -> Value>() })) * whitespace * -')'

    // Function call: identifier(arg1, arg2, ...)
    private val functionCall: Parser<(EvaluationContext) -> Value> =
        ((identifier * whitespace * argList) mapEx { parseCtx, result ->
            val (name, args) = result.value
            val callText = result.text(parseCtx)
            val callPosition = SourcePosition(result.start, result.end, callText)
            val evalFunc: (EvaluationContext) -> Value = { ctx: EvaluationContext ->
                val func = ctx.variableTable.get(name) ?: throw EvaluationException("Undefined function: $name", ctx, ctx.sourceCode)
                when (func) {
                    is Value.LambdaValue -> {
                        if (args.size != func.params.size) {
                            throw EvaluationException(
                                "Function $name expects ${func.params.size} arguments, but got ${args.size}",
                                ctx,
                                parseCtx.src
                            )
                        }
                        // Check function call limit before making the call
                        functionCallCount++
                        if (functionCallCount >= MAX_FUNCTION_CALLS) {
                            throw EvaluationException(
                                "Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded",
                                ctx,
                                parseCtx.src
                            )
                        }
                        
                        // Create a new scope for the function call
                        // Push call frame onto the stack and create new variable scope
                        val newContext = ctx.pushFrame(name, callPosition).withNewScope()
                        
                        // Evaluate arguments in the caller's context and bind to parameters in the new scope
                        func.params.zip(args).forEach { (param, argParser) ->
                            newContext.variableTable.set(param, argParser(ctx))
                        }
                        
                        // Execute function body in the new context
                        func.body(newContext)
                    }
                    else -> throw EvaluationException("$name is not a function", ctx, ctx.sourceCode)
                }
            }
            evalFunc
        })

    // Primary expression: number, variable reference, function call, lambda, or grouped expression
    private val primary: Parser<(EvaluationContext) -> Value> =
        lambda + functionCall + variableRef + (number map { v -> { _: EvaluationContext -> v } }) + 
            (-'(' * whitespace * ref { expression } * whitespace * -')')

    private val factor: Parser<(EvaluationContext) -> Value> = primary

    // Multiplication operator parser
    private val multiplyOp = (whitespace * +'*' * whitespace * factor) mapEx { parseCtx, result ->
        val (_, rightParser: (EvaluationContext) -> Value) = result.value
        val opText = result.text(parseCtx)
        val opPosition = SourcePosition(result.start, result.end, opText)
        val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
            val rightVal = rightParser(ctx)
            if (left !is Value.NumberValue) throw EvaluationException("Left operand of * must be a number", ctx, ctx.sourceCode)
            if (rightVal !is Value.NumberValue) throw EvaluationException("Right operand of * must be a number", ctx, ctx.sourceCode)
            Value.NumberValue(left.value * rightVal.value)
        }
        opFunc
    }
    
    // Division operator parser
    private val divideOp = (whitespace * +'/' * whitespace * factor) mapEx { parseCtx, result ->
        val (_, rightParser: (EvaluationContext) -> Value) = result.value
        val opText = result.text(parseCtx)
        val opPosition = SourcePosition(result.start, result.end, opText)
        val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
            val rightVal = rightParser(ctx)
            if (left !is Value.NumberValue) throw EvaluationException("Left operand of / must be a number", ctx, ctx.sourceCode)
            if (rightVal !is Value.NumberValue) throw EvaluationException("Right operand of / must be a number", ctx, ctx.sourceCode)
            if (rightVal.value == 0.0) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("division", opPosition))
                throw EvaluationException("Division by zero", newCtx, ctx.sourceCode)
            }
            Value.NumberValue(left.value / rightVal.value)
        }
        opFunc
    }
    
    private val product: Parser<(EvaluationContext) -> Value> = 
        leftAssociativeBinaryOp(factor, multiplyOp + divideOp)

    // Addition operator parser
    private val addOp = (whitespace * +'+' * whitespace * product) mapEx { parseCtx, result ->
        val (_, rightParser: (EvaluationContext) -> Value) = result.value
        val opText = result.text(parseCtx)
        val opPosition = SourcePosition(result.start, result.end, opText)
        val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
            val rightVal = rightParser(ctx)
            if (left !is Value.NumberValue) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("+ operator", opPosition))
                throw EvaluationException("Left operand of + must be a number", newCtx, ctx.sourceCode)
            }
            if (rightVal !is Value.NumberValue) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("+ operator", opPosition))
                throw EvaluationException("Right operand of + must be a number", newCtx, ctx.sourceCode)
            }
            Value.NumberValue(left.value + rightVal.value)
        }
        opFunc
    }
    
    // Subtraction operator parser
    private val subtractOp = (whitespace * +'-' * whitespace * product) mapEx { parseCtx, result ->
        val (_, rightParser: (EvaluationContext) -> Value) = result.value
        val opText = result.text(parseCtx)
        val opPosition = SourcePosition(result.start, result.end, opText)
        val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
            val rightVal = rightParser(ctx)
            if (left !is Value.NumberValue) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("- operator", opPosition))
                throw EvaluationException("Left operand of - must be a number", newCtx, ctx.sourceCode)
            }
            if (rightVal !is Value.NumberValue) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("- operator", opPosition))
                throw EvaluationException("Right operand of - must be a number", newCtx, ctx.sourceCode)
            }
            Value.NumberValue(left.value - rightVal.value)
        }
        opFunc
    }
    
    private val sum: Parser<(EvaluationContext) -> Value> = 
        leftAssociativeBinaryOp(product, addOp + subtractOp)

    // Ordering comparison operators: <, <=, >, >=
    private val orderingComparison: Parser<(EvaluationContext) -> Value> = run {
        // Less than or equal operator parser (must come before < to match correctly)
        val lessEqualOp = (whitespace * +"<=" * whitespace * sum) mapEx { parseCtx, result ->
            val (_, rightParser: (EvaluationContext) -> Value) = result.value
            
            val opText = result.text(parseCtx)
            val opPosition = SourcePosition(result.start, result.end, opText)
            val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
                val rightVal = rightParser(ctx)
                if (left !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("<= operator", opPosition))
                    throw EvaluationException("Left operand of <= must be a number", newCtx, ctx.sourceCode)
                }
                if (rightVal !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("<= operator", opPosition))
                    throw EvaluationException("Right operand of <= must be a number", newCtx, ctx.sourceCode)
                }
                Value.BooleanValue(left.value <= rightVal.value)
            }
            opFunc
        }
        
        // Greater than or equal operator parser (must come before > to match correctly)
        val greaterEqualOp = (whitespace * +">=" * whitespace * sum) mapEx { parseCtx, result ->
            val (_, rightParser: (EvaluationContext) -> Value) = result.value
            val opText = result.text(parseCtx)
            val opPosition = SourcePosition(result.start, result.end, opText)
            val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
                val rightVal = rightParser(ctx)
                if (left !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame(">= operator", opPosition))
                    throw EvaluationException("Left operand of >= must be a number", newCtx, ctx.sourceCode)
                }
                if (rightVal !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame(">= operator", opPosition))
                    throw EvaluationException("Right operand of >= must be a number", newCtx, ctx.sourceCode)
                }
                Value.BooleanValue(left.value >= rightVal.value)
            }
            opFunc
        }
        
        // Less than operator parser
        val lessOp = (whitespace * +'<' * whitespace * sum) mapEx { parseCtx, result ->
            val (_, rightParser: (EvaluationContext) -> Value) = result.value
            val opText = result.text(parseCtx)
            val opPosition = SourcePosition(result.start, result.end, opText)
            val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
                val rightVal = rightParser(ctx)
                if (left !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("< operator", opPosition))
                    throw EvaluationException("Left operand of < must be a number", newCtx, ctx.sourceCode)
                }
                if (rightVal !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("< operator", opPosition))
                    throw EvaluationException("Right operand of < must be a number", newCtx, ctx.sourceCode)
                }
                Value.BooleanValue(left.value < rightVal.value)
            }
            opFunc
        }
        
        // Greater than operator parser
        val greaterOp = (whitespace * +'>' * whitespace * sum) mapEx { parseCtx, result ->
            val (_, rightParser: (EvaluationContext) -> Value) = result.value
            val opText = result.text(parseCtx)
            val opPosition = SourcePosition(result.start, result.end, opText)
            val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
                val rightVal = rightParser(ctx)
                if (left !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("> operator", opPosition))
                    throw EvaluationException("Left operand of > must be a number", newCtx, ctx.sourceCode)
                }
                if (rightVal !is Value.NumberValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("> operator", opPosition))
                    throw EvaluationException("Right operand of > must be a number", newCtx, ctx.sourceCode)
                }
                Value.BooleanValue(left.value > rightVal.value)
            }
            opFunc
        }
        
        val restItem = lessEqualOp + greaterEqualOp + lessOp + greaterOp
        
        (sum * restItem.zeroOrMore) map { (first, rest) ->
            { ctx: EvaluationContext ->
                var result = first(ctx)
                for (opFunc in rest) {
                    result = opFunc(result, ctx)
                }
                result
            }
        }
    }

    // Equality comparison operators: ==, !=
    private val equalityComparison: Parser<(EvaluationContext) -> Value> = run {
        // Equality operator parser
        val equalOp = (whitespace * +"==" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            val (_, rightParser: (EvaluationContext) -> Value) = result.value
            val opText = result.text(parseCtx)
            val opPosition = SourcePosition(result.start, result.end, opText)
            val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
                val rightVal = rightParser(ctx)
                val compareResult = when {
                    left is Value.NumberValue && rightVal is Value.NumberValue -> left.value == rightVal.value
                    left is Value.BooleanValue && rightVal is Value.BooleanValue -> left.value == rightVal.value
                    else -> {
                        val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("== operator", opPosition))
                        throw EvaluationException("Operands of == must be both numbers or both booleans", newCtx, ctx.sourceCode)
                    }
                }
                Value.BooleanValue(compareResult)
            }
            opFunc
        }
        
        // Inequality operator parser
        val notEqualOp = (whitespace * +"!=" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            val (_, rightParser: (EvaluationContext) -> Value) = result.value
            val opText = result.text(parseCtx)
            val opPosition = SourcePosition(result.start, result.end, opText)
            val opFunc: (Value, EvaluationContext) -> Value = { left, ctx ->
                val rightVal = rightParser(ctx)
                val compareResult = when {
                    left is Value.NumberValue && rightVal is Value.NumberValue -> left.value != rightVal.value
                    left is Value.BooleanValue && rightVal is Value.BooleanValue -> left.value != rightVal.value
                    else -> {
                        val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("!= operator", opPosition))
                        throw EvaluationException("Operands of != must be both numbers or both booleans", newCtx, ctx.sourceCode)
                    }
                }
                Value.BooleanValue(compareResult)
            }
            opFunc
        }
        
        val restItem = equalOp + notEqualOp
        
        (orderingComparison * restItem.zeroOrMore) map { (first, rest) ->
            { ctx: EvaluationContext ->
                var result = first(ctx)
                for (opFunc in rest) {
                    result = opFunc(result, ctx)
                }
                result
            }
        }
    }

    // Ternary operator: condition ? trueExpr : falseExpr
    private val ternary: Parser<(EvaluationContext) -> Value> = run {
        val ternaryExpr = ref { equalityComparison } * whitespace * -'?' * whitespace *
            ref { equalityComparison } * whitespace * -':' * whitespace *
            ref { equalityComparison }
        ((ternaryExpr mapEx { parseCtx, result ->
            val (cond, trueExpr, falseExpr) = result.value
            val ternaryText = result.text(parseCtx)
            val ternaryPosition = SourcePosition(result.start, result.end, ternaryText)
            val evalFunc: (EvaluationContext) -> Value = { ctx: EvaluationContext ->
                val condVal = cond(ctx)
                if (condVal !is Value.BooleanValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("ternary operator", ternaryPosition))
                    throw EvaluationException("Condition in ternary operator must be a boolean", newCtx, ctx.sourceCode)
                }
                if (condVal.value) trueExpr(ctx) else falseExpr(ctx)
            }
            evalFunc
        }) + equalityComparison)
    }

    // Assignment: variable = expression
    private val assignment: Parser<(EvaluationContext) -> Value> = run {
        ((identifier * whitespace * -'=' * whitespace * ref { expression }) map { (name, valueParser) ->
            val evalFunc: (EvaluationContext) -> Value = { ctx: EvaluationContext ->
                val value = valueParser(ctx)
                ctx.variableTable.set(name, value)
                value
            }
            evalFunc
        }) + ternary
    }

    // Root expression parser
    val expression: Parser<(EvaluationContext) -> Value> = assignment

    val root = whitespace * expression * whitespace
}

@JsExport
fun parseExpression(input: String): String {
    return try {
        // Reset function call counter for each evaluation to ensure each call is independent
        ExpressionGrammar.functionCallCount = 0
        
        // Create initial evaluation context with empty call stack, source code, and fresh variable table
        val initialContext = EvaluationContext(sourceCode = input)
        
        // Try to parse as a single expression first
        // If parsing succeeds, evaluate and return the result
        try {
            val resultParser = ExpressionGrammar.root.parseAllOrThrow(input)
            val result = resultParser(initialContext)
            return result.toString()
        } catch (e: Exception) {
            // If single expression fails, try as multi-line program
            // Split input into lines and evaluate each line
            val lines = input.lines().filter { it.trim().isNotEmpty() }
            if (lines.isEmpty()) {
                return ""
            }
            
            // If there's only one line, rethrow the original error
            if (lines.size == 1) {
                throw e
            }
            
            val results = mutableListOf<Value>()
            for (line in lines) {
                val lineParser = ExpressionGrammar.root.parseAllOrThrow(line)
                val lineResult = lineParser(initialContext)
                results.add(lineResult)
            }
            
            // Return the last result
            return results.last().toString()
        }
    } catch (e: EvaluationException) {
        // Use custom formatting if call stack is available
        if (e.context != null && e.context.callStack.isNotEmpty()) {
            e.formatWithCallStack()
        } else {
            "Error: ${e.message}"
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
