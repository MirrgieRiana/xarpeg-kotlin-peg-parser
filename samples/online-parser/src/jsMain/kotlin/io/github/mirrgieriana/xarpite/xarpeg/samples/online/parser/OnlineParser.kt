@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import io.github.mirrgieriana.xarpite.xarpeg.ParseContext
import io.github.mirrgieriana.xarpite.xarpeg.ParseResult
import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.mapEx
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.zeroOrMore
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

// Expression interface that evaluates to a Value in a given context
fun interface Expression {
    fun evaluate(ctx: EvaluationContext): Value
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
        val beforeStart = source.substring(0, start.coerceAtMost(source.length))
        val line = beforeStart.count { it == '\n' } + 1
        val column = start - (beforeStart.lastIndexOf('\n') + 1) + 1
        return "line $line, column $column"
    }
    
    fun formatWithContext(source: String): String {
        val beforeStart = source.substring(0, start.coerceAtMost(source.length))
        val line = beforeStart.count { it == '\n' } + 1
        val column = start - (beforeStart.lastIndexOf('\n') + 1) + 1
        
        // Get the line containing this position
        val lineStart = beforeStart.lastIndexOf('\n') + 1
        val lineEnd = source.indexOf('\n', start).let { if (it == -1) source.length else it }
        val sourceLine = source.substring(lineStart, lineEnd)
        
        // Calculate positions within the line
        val highlightStart = start - lineStart
        val highlightEnd = (end - lineStart).coerceAtMost(sourceLine.length)
        
        // Build the formatted output with highlighted range
        val before = sourceLine.substring(0, highlightStart)
        val highlighted = sourceLine.substring(highlightStart, highlightEnd)
        val after = sourceLine.substring(highlightEnd)
        
        return "line $line, column $column: $before[$highlighted]$after"
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
        val body: Expression,
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
                    frame.position.formatWithContext(sourceCode)
                } else {
                    "position ${frame.position.start}-${frame.position.end}: ${frame.position.text}"
                }
                sb.append("\n  at $location")
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
    
    // Binary operator function interface
    fun interface BinaryOperator {
        fun apply(left: Value, ctx: EvaluationContext): Value
    }
    
    // Helper function for left-associative binary operator aggregation
    // Takes a term parser and operators
    private fun leftAssociativeBinaryOp(
        term: Parser<Expression>,
        operators: Parser<BinaryOperator>
    ): Parser<Expression> {
        return (term * operators.zeroOrMore) map { (first, rest) ->
            Expression { ctx ->
                var result = first.evaluate(ctx)
                for (opFunc in rest) {
                    result = opFunc.apply(result, ctx)
                }
                result
            }
        }
    }

    // Variable reference
    private val variableRef: Parser<Expression> = identifier map { name ->
        Expression { ctx -> 
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
    private val lambda: Parser<Expression> =
        ((paramList * whitespace * -Regex("->") * whitespace * ref { expression }) mapEx { parseCtx, result ->
            val (params, bodyParser) = result.value
            val lambdaText = result.text(parseCtx)
            val position = SourcePosition(result.start, result.end, lambdaText)
            Expression { ctx ->
                // Don't capture variables - use dynamic scoping to allow recursion
                // The lambda will see whatever is in scope when it's called
                Value.LambdaValue(params, bodyParser, mutableMapOf(), definitionPosition = position)
            }
        })

    // Helper to parse comma-separated list of expressions
    private val exprList: Parser<List<Expression>> = run {
        val restItem = whitespace * -',' * whitespace * ref { expression }
        (ref { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Argument list for function calls: (arg1, arg2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty argument lists: ()
    private val argList: Parser<List<Expression>> =
        -'(' * whitespace * (exprList + (whitespace map { emptyList<Expression>() })) * whitespace * -')'

    // Function call: identifier(arg1, arg2, ...)
    private val functionCall: Parser<Expression> =
        ((identifier * whitespace * argList) mapEx { parseCtx, result ->
            val (name, args) = result.value
            val callText = result.text(parseCtx)
            val callPosition = SourcePosition(result.start, result.end, callText)
            Expression { ctx ->
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
                        func.params.zip(args).forEach { (param, argExpr) ->
                            newContext.variableTable.set(param, argExpr.evaluate(ctx))
                        }
                        
                        // Execute function body in the new context
                        func.body.evaluate(newContext)
                    }
                    else -> throw EvaluationException("$name is not a function", ctx, ctx.sourceCode)
                }
            }
        })

    // Primary expression: number, variable reference, function call, lambda, or grouped expression
    private val primary: Parser<Expression> =
        lambda + functionCall + variableRef + (number map { v -> Expression { _ -> v } }) + 
            (-'(' * whitespace * ref { expression } * whitespace * -')')

    private val factor: Parser<Expression> = primary

    // Helper function to create arithmetic operator with type checking
    private fun arithmeticOp(
        opSymbol: String,
        opName: String,
        operation: (Double, Double) -> Double,
        additionalCheck: ((Value.NumberValue, EvaluationContext, SourcePosition) -> Unit)? = null
    ): (Expression, SourcePosition) -> BinaryOperator = { rightExpr, opPosition ->
        BinaryOperator { left, ctx ->
            val rightVal = rightExpr.evaluate(ctx)
            if (left !is Value.NumberValue) throw EvaluationException("Left operand of $opSymbol must be a number", ctx, ctx.sourceCode)
            if (rightVal !is Value.NumberValue) throw EvaluationException("Right operand of $opSymbol must be a number", ctx, ctx.sourceCode)
            additionalCheck?.invoke(rightVal, ctx, opPosition)
            Value.NumberValue(operation(left.value, rightVal.value))
        }
    }

    // Multiplication operator parser
    private val multiplyOp = (whitespace * (+'*' * whitespace * factor) mapEx { parseCtx, result ->
        val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
        val (_, rightExpr: Expression) = result.value
        Pair(opPosition, rightExpr)
    }) map { result -> 
        val (_, pair) = result.value
        val (opPosition, rightExpr) = pair
        arithmeticOp("*", "multiplication", Double::times)(rightExpr, opPosition)
    }
    
    // Division operator parser
    private val divideOp = (whitespace * (+'/' * whitespace * factor) mapEx { parseCtx, result ->
        val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
        val (_, rightExpr: Expression) = result.value
        Pair(opPosition, rightExpr)
    }) map { result ->
        val (_, pair) = result.value
        val (opPosition, rightExpr) = pair
        arithmeticOp("/", "division", Double::div, additionalCheck = { rightVal, ctx, opPos ->
            if (rightVal.value == 0.0) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("division", opPos))
                throw EvaluationException("Division by zero", newCtx, ctx.sourceCode)
            }
        })(rightExpr, opPosition)
    }
    
    private val product: Parser<Expression> = 
        leftAssociativeBinaryOp(factor, multiplyOp + divideOp)

    // Addition operator parser
    private val addOp = (whitespace * (+'+' * whitespace * product) mapEx { parseCtx, result ->
        val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
        val (_, rightExpr: Expression) = result.value
        Pair(opPosition, rightExpr)
    }) map { result ->
        val (_, pair) = result.value
        val (opPosition, rightExpr) = pair
        arithmeticOp("+", "addition", Double::plus)(rightExpr, opPosition)
    }
    
    // Subtraction operator parser
    private val subtractOp = (whitespace * (+'-' * whitespace * product) mapEx { parseCtx, result ->
        val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
        val (_, rightExpr: Expression) = result.value
        Pair(opPosition, rightExpr)
    }) map { result ->
        val (_, pair) = result.value
        val (opPosition, rightExpr) = pair
        arithmeticOp("-", "subtraction", Double::minus)(rightExpr, opPosition)
    }
    
    private val sum: Parser<Expression> = 
        leftAssociativeBinaryOp(product, addOp + subtractOp)

    // Helper function to create comparison operator with type checking
    private fun comparisonOp(
        opSymbol: String,
        comparison: (Double, Double) -> Boolean
    ): (Expression, SourcePosition) -> BinaryOperator = { rightExpr, opPosition ->
        BinaryOperator { left, ctx ->
            val rightVal = rightExpr.evaluate(ctx)
            if (left !is Value.NumberValue) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("$opSymbol operator", opPosition))
                throw EvaluationException("Left operand of $opSymbol must be a number", newCtx, ctx.sourceCode)
            }
            if (rightVal !is Value.NumberValue) {
                val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("$opSymbol operator", opPosition))
                throw EvaluationException("Right operand of $opSymbol must be a number", newCtx, ctx.sourceCode)
            }
            Value.BooleanValue(comparison(left.value, rightVal.value))
        }
    }

    // Ordering comparison operators: <, <=, >, >=
    private val orderingComparison: Parser<Expression> = run {
        // Less than or equal operator parser (must come before < to match correctly)
        val lessEqualOp = (whitespace * (+"<=" * whitespace * sum) mapEx { parseCtx, result ->
            val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
            val (_, rightExpr: Expression) = result.value
            Pair(opPosition, rightExpr)
        }) map { result ->
            val (_, pair) = result.value
            val (opPosition, rightExpr) = pair
            comparisonOp("<=") { l, r -> l <= r }(rightExpr, opPosition)
        }
        
        // Greater than or equal operator parser (must come before > to match correctly)
        val greaterEqualOp = (whitespace * (+">=" * whitespace * sum) mapEx { parseCtx, result ->
            val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
            val (_, rightExpr: Expression) = result.value
            Pair(opPosition, rightExpr)
        }) map { result ->
            val (_, pair) = result.value
            val (opPosition, rightExpr) = pair
            comparisonOp(">=") { l, r -> l >= r }(rightExpr, opPosition)
        }
        
        // Less than operator parser
        val lessOp = (whitespace * (+'<' * whitespace * sum) mapEx { parseCtx, result ->
            val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
            val (_, rightExpr: Expression) = result.value
            Pair(opPosition, rightExpr)
        }) map { result ->
            val (_, pair) = result.value
            val (opPosition, rightExpr) = pair
            comparisonOp("<") { l, r -> l < r }(rightExpr, opPosition)
        }
        
        // Greater than operator parser
        val greaterOp = (whitespace * (+'>' * whitespace * sum) mapEx { parseCtx, result ->
            val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
            val (_, rightExpr: Expression) = result.value
            Pair(opPosition, rightExpr)
        }) map { result ->
            val (_, pair) = result.value
            val (opPosition, rightExpr) = pair
            comparisonOp(">") { l, r -> l > r }(rightExpr, opPosition)
        }
        
        val restItem = lessEqualOp + greaterEqualOp + lessOp + greaterOp
        
        (sum * restItem.zeroOrMore) map { (first, rest) ->
            Expression { ctx ->
                var result = first.evaluate(ctx)
                for (opFunc in rest) {
                    result = opFunc.apply(result, ctx)
                }
                result
            }
        }
    }

    // Equality comparison operators: ==, !=
    private val equalityComparison: Parser<Expression> = run {
        // Equality operator parser
        val equalOp = (whitespace * (+"==" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
            val (_, rightExpr: Expression) = result.value
            Pair(opPosition, rightExpr)
        }) map { result ->
            val (_, pair) = result.value
            val (opPosition, rightExpr) = pair
            BinaryOperator { left, ctx ->
                val rightVal = rightExpr.evaluate(ctx)
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
        }
        
        // Inequality operator parser
        val notEqualOp = (whitespace * (+"!=" * whitespace * orderingComparison) mapEx { parseCtx, result ->
            val opPosition = SourcePosition(result.start, result.end, result.text(parseCtx))
            val (_, rightExpr: Expression) = result.value
            Pair(opPosition, rightExpr)
        }) map { result ->
            val (_, pair) = result.value
            val (opPosition, rightExpr) = pair
            BinaryOperator { left, ctx ->
                val rightVal = rightExpr.evaluate(ctx)
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
        }
        
        val restItem = equalOp + notEqualOp
        
        (orderingComparison * restItem.zeroOrMore) map { (first, rest) ->
            Expression { ctx ->
                var result = first.evaluate(ctx)
                for (opFunc in rest) {
                    result = opFunc.apply(result, ctx)
                }
                result
            }
        }
    }

    // Ternary operator: condition ? trueExpr : falseExpr
    private val ternary: Parser<Expression> = run {
        val ternaryExpr = ref { equalityComparison } * whitespace * -'?' * whitespace *
            ref { equalityComparison } * whitespace * -':' * whitespace *
            ref { equalityComparison }
        ((ternaryExpr mapEx { parseCtx, result ->
            val (cond, trueExpr, falseExpr) = result.value
            val ternaryText = result.text(parseCtx)
            val ternaryPosition = SourcePosition(result.start, result.end, ternaryText)
            Expression { ctx ->
                val condVal = cond.evaluate(ctx)
                if (condVal !is Value.BooleanValue) {
                    val newCtx = ctx.copy(callStack = ctx.callStack + CallFrame("ternary operator", ternaryPosition))
                    throw EvaluationException("Condition in ternary operator must be a boolean", newCtx, ctx.sourceCode)
                }
                if (condVal.value) trueExpr.evaluate(ctx) else falseExpr.evaluate(ctx)
            }
        }) + equalityComparison)
    }

    // Assignment: variable = expression
    private val assignment: Parser<Expression> = run {
        ((identifier * whitespace * -'=' * whitespace * ref { expression }) map { (name, valueExpr) ->
            Expression { ctx ->
                val value = valueExpr.evaluate(ctx)
                ctx.variableTable.set(name, value)
                value
            }
        }) + ternary
    }

    // Root expression parser
    val expression: Parser<Expression> = assignment

    // Multi-statement parser: parses multiple expressions separated by newlines
    val program: Parser<Expression> = run {
        val newlineSep = -Regex("[ \\t]*\\r?\\n[ \\t\\r\\n]*")
        ((expression * (newlineSep * expression).zeroOrMore) map { (first, rest) ->
            Expression { ctx ->
                var result = first.evaluate(ctx)
                for (expr in rest) {
                    result = expr.evaluate(ctx)
                }
                result
            }
        })
    }

    val root = whitespace * expression * whitespace
    val programRoot = whitespace * program * whitespace
}

@JsExport
fun parseExpression(input: String): String {
    return try {
        // Reset function call counter for each evaluation to ensure each call is independent
        ExpressionGrammar.functionCallCount = 0
        
        // Create initial evaluation context with empty call stack, source code, and fresh variable table
        val initialContext = EvaluationContext(sourceCode = input)
        
        // Try to parse as a multi-statement program first (handles both single and multiple expressions)
        val resultExpr = ExpressionGrammar.programRoot.parseAllOrThrow(input)
        val result = resultExpr.evaluate(initialContext)
        result.toString()
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
