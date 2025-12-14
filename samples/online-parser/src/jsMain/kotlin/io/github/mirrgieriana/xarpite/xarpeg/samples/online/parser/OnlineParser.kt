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
    data class LambdaValue(val params: List<String>, val body: () -> Value, val capturedVars: MutableMap<String, Value>) : Value() {
        override fun toString() = "<lambda(${params.joinToString(", ")})>"
    }
}

class EvaluationException(message: String, cause: Throwable? = null) : Exception(message, cause)

private object ExpressionGrammar {
    private val whitespace = -Regex("[ \\t\\r\\n]*")

    // Identifier: alphanumeric and _, but first character cannot be a digit
    private val identifier = +Regex("[a-zA-Z_][a-zA-Z0-9_]*") map { it.value }

    private val number = +Regex("[0-9]+(?:\\.[0-9]+)?") map { Value.NumberValue(it.value.toDouble()) }

    // Variable table for storing values
    val variables = mutableMapOf<String, Value>()

    // Forward declarations
    val expression: Parser<() -> Value> by lazy { assignment }

    // Variable reference
    private val variableRef: Parser<() -> Value> = identifier map { name ->
        { 
            variables[name] ?: throw EvaluationException("Undefined variable: $name")
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
    private val lambda: Parser<() -> Value> by lazy {
        (paramList * whitespace * -Regex("->") * whitespace * parser { expression }) map { (params, bodyParser) ->
            {
                // Capture current variable state
                val capturedVariables = variables.toMutableMap()
                Value.LambdaValue(params, bodyParser, capturedVariables)
            }
        }
    }

    // Helper to parse comma-separated list of expressions
    private val exprList: Parser<List<() -> Value>> by lazy {
        val restItem = whitespace * -',' * whitespace * parser { expression }
        (parser { expression } * restItem.zeroOrMore) map { (first, rest) -> listOf(first) + rest }
    }

    // Argument list for function calls: (arg1, arg2) or ()
    // The alternative (whitespace map { emptyList() }) handles empty argument lists: ()
    private val argList: Parser<List<() -> Value>> by lazy {
        -'(' * whitespace * (exprList + (whitespace map { emptyList<() -> Value>() })) * whitespace * -')'
    }

    // Function call: identifier(arg1, arg2, ...)
    private val functionCall: Parser<() -> Value> by lazy {
        (identifier * whitespace * argList) map { (name, args) ->
            {
                val func = variables[name] ?: throw EvaluationException("Undefined function: $name")
                when (func) {
                    is Value.LambdaValue -> {
                        if (args.size != func.params.size) {
                            throw EvaluationException("Function $name expects ${func.params.size} arguments, but got ${args.size}")
                        }
                        // Save current variables
                        val savedVariables = variables.toMutableMap()
                        try {
                            // Restore captured variables and add parameters
                            variables.clear()
                            variables.putAll(func.capturedVars)
                            func.params.zip(args).forEach { (param, argParser) ->
                                variables[param] = argParser()
                            }
                            func.body()
                        } finally {
                            // Restore variables
                            variables.clear()
                            variables.putAll(savedVariables)
                        }
                    }
                    else -> throw EvaluationException("$name is not a function")
                }
            }
        }
    }

    // Primary expression: number, variable reference, function call, lambda, or grouped expression
    private val primary: Parser<() -> Value> by lazy {
        lambda + functionCall + variableRef + (number map { v -> { v } }) + 
            (-'(' * whitespace * parser { expression } * whitespace * -')')
    }

    private val factor: Parser<() -> Value> by lazy { primary }

    private val product: Parser<() -> Value> = leftAssociative(factor, whitespace * (+'*' + +'/') * whitespace) { a, op, b ->
        {
            val aVal = a()
            val bVal = b()
            if (aVal !is Value.NumberValue) throw EvaluationException("Left operand of $op must be a number")
            if (bVal !is Value.NumberValue) throw EvaluationException("Right operand of $op must be a number")
            when (op) {
                '*' -> Value.NumberValue(aVal.value * bVal.value)
                '/' -> {
                    if (bVal.value == 0.0) throw EvaluationException("Division by zero")
                    Value.NumberValue(aVal.value / bVal.value)
                }
                else -> aVal
            }
        }
    }

    private val sum: Parser<() -> Value> = leftAssociative(product, whitespace * (+'+' + +'-') * whitespace) { a, op, b ->
        {
            val aVal = a()
            val bVal = b()
            if (aVal !is Value.NumberValue) throw EvaluationException("Left operand of $op must be a number")
            if (bVal !is Value.NumberValue) throw EvaluationException("Right operand of $op must be a number")
            when (op) {
                '+' -> Value.NumberValue(aVal.value + bVal.value)
                '-' -> Value.NumberValue(aVal.value - bVal.value)
                else -> aVal
            }
        }
    }

    // Assignment: variable = expression
    private val assignment: Parser<() -> Value> by lazy {
        ((identifier * whitespace * -'=' * whitespace * parser { expression }) map { (name, valueParser) ->
            {
                val value = valueParser()
                variables[name] = value
                value
            }
        }) + sum
    }

    val root = whitespace * expression * whitespace
}

@JsExport
fun parseExpression(input: String): String {
    return try {
        // Reset variables for each evaluation to ensure each call is independent
        ExpressionGrammar.variables.clear()
        
        // Try to parse as a single expression first
        // If parsing succeeds, evaluate and return the result
        try {
            val resultParser = ExpressionGrammar.root.parseAllOrThrow(input)
            val result = resultParser()
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
                val lineResult = lineParser()
                results.add(lineResult)
            }
            
            // Return the last result
            return results.last().toString()
        }
    } catch (e: EvaluationException) {
        val stackTrace = e.stackTraceToString()
            .lines()
            .take(10)  // Limit stack trace to 10 lines
            .joinToString("\n")
        "Error: ${e.message}\n\nStack trace:\n$stackTrace"
    } catch (e: Exception) {
        val stackTrace = e.stackTraceToString()
            .lines()
            .take(10)  // Limit stack trace to 10 lines
            .joinToString("\n")
        "Error: ${e.message}\n\nStack trace:\n$stackTrace"
    }
}
