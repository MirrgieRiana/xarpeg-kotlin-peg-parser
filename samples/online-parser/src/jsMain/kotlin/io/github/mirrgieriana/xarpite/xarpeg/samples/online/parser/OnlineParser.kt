@file:OptIn(ExperimentalJsExport::class)

package io.github.mirrgieriana.xarpite.xarpeg.samples.online.parser

import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// Value types that can be stored in variables
sealed class Value {
    data class NumberValue(val value: Double) : Value() {
        override fun toString() = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
    data class BooleanValue(val value: Boolean) : Value() {
        override fun toString() = value.toString()
    }
    data class LambdaValue(val params: List<String>, val body: (EvaluationContext) -> Value, val capturedVars: MutableMap<String, Value>) : Value() {
        override fun toString() = "<lambda(${params.joinToString(", ")})>"
    }
}

// Stack frame for tracking function calls
data class StackFrame(val functionName: String, val position: String)

// Execution context that tracks the call stack
class EvaluationContext {
    val callStack = mutableListOf<StackFrame>()
    
    fun pushFrame(functionName: String, position: String) {
        callStack.add(StackFrame(functionName, position))
    }
    
    fun popFrame() {
        if (callStack.isNotEmpty()) {
            callStack.removeLast()
        }
    }
    
    fun formatCallStack(): String {
        if (callStack.isEmpty()) return "  (no function calls)"
        return callStack.reversed().joinToString("\n") { frame ->
            "  at ${frame.functionName} (position ${frame.position})"
        }
    }
}

class EvaluationException(message: String, val context: EvaluationContext? = null, cause: Throwable? = null) : Exception(message, cause) {
    fun getFormattedMessage(): String {
        val baseMessage = "Error: $message"
        val stackInfo = if (context != null && context.callStack.isNotEmpty()) {
            "\n\nCall stack:\n${context.formatCallStack()}"
        } else {
            "\n\nStack trace:\n  (top level)"
        }
        return baseMessage + stackInfo
    }
}

private object ExpressionGrammar {
    private val whitespace = -Regex("[ \\t\\r\\n]*")

    // Identifier: alphanumeric and _, but first character cannot be a digit
    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value }

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { Value.NumberValue(it.value.toDouble()) }

    // Variable table for storing values
    val variables = mutableMapOf<String, Value>()
    
    // Function call counter to prevent infinite recursion
    var functionCallCount = 0
    private const val MAX_FUNCTION_CALLS = 100

    // Forward declarations
    val expression: Parser<(EvaluationContext) -> Value> by lazy { assignment }

    // Variable reference
    private val variableRef: Parser<(EvaluationContext) -> Value> = identifier map { name ->
        { ctx -> 
            variables[name] ?: throw EvaluationException("Undefined variable: $name", ctx)
        }
    }

    // Helper to parse comma-separated list of identifiers
    private val identifierList: Parser<List<String>> by lazy {
        val restItem = whitespace * -',' * whitespace * identifier
        (identifier * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Lambda parameter list: (param1, param2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty parameter lists: ()
    private val paramList: Parser<List<String>> = 
        -'(' * whitespace * (identifierList + (whitespace map { emptyList<String>() })) * whitespace * -')'

    // Lambda expression: (param1, param2, ...) -> body
    private val lambda: Parser<(EvaluationContext) -> Value> by lazy {
        (paramList * whitespace * -Regex("->") * whitespace * parser { expression }) map { (params, bodyParser) ->
            { ctx ->
                // Don't capture variables - use dynamic scoping to allow recursion
                // The lambda will see whatever is in scope when it's called
                Value.LambdaValue(params, bodyParser, mutableMapOf())
            }
        }
    }

    // Helper to parse comma-separated list of expressions
    private val exprList: Parser<List<(EvaluationContext) -> Value>> by lazy {
        val restItem = whitespace * -',' * whitespace * parser { expression }
        (parser { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Argument list for function calls: (arg1, arg2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty argument lists: ()
    private val argList: Parser<List<(EvaluationContext) -> Value>> by lazy {
        -'(' * whitespace * (exprList + (whitespace map { emptyList<(EvaluationContext) -> Value>() })) * whitespace * -')'
    }

    // Function call: identifier(arg1, arg2, ...)
    private val functionCall: Parser<(EvaluationContext) -> Value> by lazy {
        (identifier * whitespace * argList) mapEx { parseContext, result ->
            val (name, args) = result.value
            val callPosition = "${result.start}-${result.end}"
            { ctx ->
                val func = variables[name] ?: throw EvaluationException("Undefined function: $name", ctx)
                when (func) {
                    is Value.LambdaValue -> {
                        if (args.size != func.params.size) {
                            throw EvaluationException("Function $name expects ${func.params.size} arguments, but got ${args.size}", ctx)
                        }
                        // Check function call limit before making the call
                        functionCallCount++
                        if (functionCallCount >= MAX_FUNCTION_CALLS) {
                            throw EvaluationException("Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded", ctx)
                        }
                        // Push call frame onto stack
                        ctx.pushFrame(name, callPosition)
                        // Save current variables
                        val savedVariables = variables.toMutableMap()
                        try {
                            // Use current variables (dynamic scoping) to enable recursion
                            // Just add parameters on top of current scope
                            func.params.zip(args).forEach { (param, argParser) ->
                                variables[param] = argParser(ctx)
                            }
                            func.body(ctx)
                        } finally {
                            // Pop call frame from stack
                            ctx.popFrame()
                            // Restore variables
                            variables.clear()
                            variables.putAll(savedVariables)
                        }
                    }
                    else -> throw EvaluationException("$name is not a function", ctx)
                }
            }
        }
    }

    // Primary expression: number, variable reference, function call, lambda, or grouped expression
    private val primary: Parser<(EvaluationContext) -> Value> by lazy {
        lambda + functionCall + variableRef + (number map { v -> { _: EvaluationContext -> v } }) + 
            (-'(' * whitespace * parser { expression } * whitespace * -')')
    }

    private val factor: Parser<(EvaluationContext) -> Value> by lazy { primary }

    private val product: Parser<(EvaluationContext) -> Value> = leftAssociative(factor, whitespace * (+'*' + +'/') * whitespace) { a, op, b ->
        { ctx ->
            val aVal = a(ctx)
            val bVal = b(ctx)
            if (aVal !is Value.NumberValue) throw EvaluationException("Left operand of $op must be a number", ctx)
            if (bVal !is Value.NumberValue) throw EvaluationException("Right operand of $op must be a number", ctx)
            when (op) {
                '*' -> Value.NumberValue(aVal.value * bVal.value)
                '/' -> {
                    if (bVal.value == 0.0) throw EvaluationException("Division by zero", ctx)
                    Value.NumberValue(aVal.value / bVal.value)
                }
                else -> aVal
            }
        }
    }

    private val sum: Parser<(EvaluationContext) -> Value> = leftAssociative(product, whitespace * (+'+' + +'-') * whitespace) { a, op, b ->
        { ctx ->
            val aVal = a(ctx)
            val bVal = b(ctx)
            if (aVal !is Value.NumberValue) throw EvaluationException("Left operand of $op must be a number", ctx)
            if (bVal !is Value.NumberValue) throw EvaluationException("Right operand of $op must be a number", ctx)
            when (op) {
                '+' -> Value.NumberValue(aVal.value + bVal.value)
                '-' -> Value.NumberValue(aVal.value - bVal.value)
                else -> aVal
            }
        }
    }

    // Ordering comparison operators: <, <=, >, >=
    private val orderingComparison: Parser<(EvaluationContext) -> Value> = leftAssociative(
        sum,
        whitespace * (+Regex("<=|>=|<|>") map { it.value }) * whitespace
    ) { a, op, b ->
        { ctx ->
            val aVal = a(ctx)
            val bVal = b(ctx)
            if (aVal !is Value.NumberValue) throw EvaluationException("Left operand of $op must be a number", ctx)
            if (bVal !is Value.NumberValue) throw EvaluationException("Right operand of $op must be a number", ctx)
            val result = when (op) {
                "<" -> aVal.value < bVal.value
                "<=" -> aVal.value <= bVal.value
                ">" -> aVal.value > bVal.value
                ">=" -> aVal.value >= bVal.value
                else -> throw EvaluationException("Unknown comparison operator: $op", ctx)
            }
            Value.BooleanValue(result)
        }
    }

    // Equality comparison operators: ==, !=
    private val equalityComparison: Parser<(EvaluationContext) -> Value> by lazy {
        leftAssociative(
            orderingComparison,
            whitespace * (+Regex("==|!=") map { it.value }) * whitespace
        ) { a, op, b ->
            { ctx ->
                val aVal = a(ctx)
                val bVal = b(ctx)
                val result = when (op) {
                    "==" -> {
                        when {
                            aVal is Value.NumberValue && bVal is Value.NumberValue -> aVal.value == bVal.value
                            aVal is Value.BooleanValue && bVal is Value.BooleanValue -> aVal.value == bVal.value
                            else -> throw EvaluationException("Operands of == must be both numbers or both booleans", ctx)
                        }
                    }
                    "!=" -> {
                        when {
                            aVal is Value.NumberValue && bVal is Value.NumberValue -> aVal.value != bVal.value
                            aVal is Value.BooleanValue && bVal is Value.BooleanValue -> aVal.value != bVal.value
                            else -> throw EvaluationException("Operands of != must be both numbers or both booleans", ctx)
                        }
                    }
                    else -> throw EvaluationException("Unknown comparison operator: $op", ctx)
                }
                Value.BooleanValue(result)
            }
        }
    }

    // Ternary operator: condition ? trueExpr : falseExpr
    private val ternary: Parser<(EvaluationContext) -> Value> by lazy {
        val ternaryExpr = parser { equalityComparison } * whitespace * -'?' * whitespace *
            parser { equalityComparison } * whitespace * -':' * whitespace *
            parser { equalityComparison }
        (ternaryExpr map { (cond: (EvaluationContext) -> Value, trueExpr: (EvaluationContext) -> Value, falseExpr: (EvaluationContext) -> Value) ->
            val result: (EvaluationContext) -> Value = { ctx: EvaluationContext ->
                val condVal = cond(ctx)
                if (condVal !is Value.BooleanValue) throw EvaluationException("Condition in ternary operator must be a boolean", ctx)
                if (condVal.value) trueExpr(ctx) else falseExpr(ctx)
            }
            result
        }) + equalityComparison
    }

    // Assignment: variable = expression
    private val assignment: Parser<(EvaluationContext) -> Value> by lazy {
        ((identifier * whitespace * -'=' * whitespace * parser { expression }) map { (name: String, valueParser: (EvaluationContext) -> Value) ->
            val result: (EvaluationContext) -> Value = { ctx: EvaluationContext ->
                val value = valueParser(ctx)
                variables[name] = value
                value
            }
            result
        }) + ternary
    }

    val root = whitespace * expression * whitespace
}

@JsExport
fun parseExpression(input: String): String {
    return try {
        // Reset variables and function call counter for each evaluation to ensure each call is independent
        ExpressionGrammar.variables.clear()
        ExpressionGrammar.functionCallCount = 0
        
        // Create evaluation context
        val ctx = EvaluationContext()
        
        // Try to parse as a single expression first
        // If parsing succeeds, evaluate and return the result
        try {
            val resultParser = ExpressionGrammar.root.parseAllOrThrow(input)
            val result = resultParser(ctx)
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
                val lineResult = lineParser(ctx)
                results.add(lineResult)
            }
            
            // Return the last result
            return results.last().toString()
        }
    } catch (e: EvaluationException) {
        e.getFormattedMessage()
    } catch (e: Exception) {
        val stackTrace = e.stackTraceToString()
            .lines()
            .take(10)  // Limit stack trace to 10 lines
            .joinToString("\n")
        "Error: ${e.message}\n\nStack trace:\n$stackTrace"
    }
}
