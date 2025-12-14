package io.github.mirrgieriana.xarpite.xarpeg

import io.github.mirrgieriana.xarpite.xarpeg.Parser
import io.github.mirrgieriana.xarpite.xarpeg.parseAllOrThrow
import io.github.mirrgieriana.xarpite.xarpeg.parsers.map
import io.github.mirrgieriana.xarpite.xarpeg.parsers.optional
import io.github.mirrgieriana.xarpite.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpite.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.times
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpite.xarpeg.parsers.zeroOrMore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Complete JSON parser implementation example.
 * 
 * This demonstrates how to build a full-featured parser for a real-world format using
 * the Xarpeg PEG parser library. The JSON parser handles all JSON data types:
 * - Strings with escape sequences
 * - Numbers (integers and floating-point)
 * - Booleans (true/false)
 * - Null
 * - Arrays (with recursive nesting)
 * - Objects (with recursive nesting)
 */
class JsonParserTest {

    /**
     * Sealed class representing all possible JSON values
     */
    sealed class JsonValue {
        data class JsonString(val value: String) : JsonValue()
        data class JsonNumber(val value: Double) : JsonValue()
        data class JsonBoolean(val value: Boolean) : JsonValue()
        data object JsonNull : JsonValue()
        data class JsonArray(val values: List<JsonValue>) : JsonValue()
        data class JsonObject(val properties: Map<String, JsonValue>) : JsonValue()
    }

    companion object {
        // Whitespace parser
        private val ws = +Regex("[ \t\r\n]*")
        
        // Helper to wrap parsers with optional whitespace
        private fun <T : Any> Parser<T>.trimmed(): Parser<T> = -ws * this * -ws
        
        // Helper to parse comma-separated lists
        private fun <T : Any> Parser<T>.commaSeparated(): Parser<List<T>> {
            val first = this
            val rest = (-ws * -',' * -ws * this).zeroOrMore
            return (first * rest).optional map { tuple ->
                if (tuple.a == null) {
                    emptyList()
                } else {
                    listOf(tuple.a.a) + tuple.a.b
                }
            }
        }

        // JSON string parser with escape sequences
        private val jsonString: Parser<JsonValue.JsonString> = object {
            // Parse escape sequences - each branch returns a String
            val escapeQuote = +'\"' map { "\"" }
            val escapeBackslash = +"\\\\" map { "\\" }
            val escapeSlash = +'/' map { "/" }
            val escapeB = +'b' map { "\b" }
            val escapeF = +'f' map { "\u000c" }
            val escapeN = +'n' map { "\n" }
            val escapeR = +'r' map { "\r" }
            val escapeT = +'t' map { "\t" }
            val escapeUnicode = +'u' * +Regex("[0-9a-fA-F]{4}") map { (_, hex) ->
                hex.value.toInt(16).toChar().toString()
            }
            val escape = +'\\' * (
                escapeQuote + escapeBackslash + escapeSlash + 
                escapeB + escapeF + escapeN + escapeR + escapeT + 
                escapeUnicode
            ) map { (_, str) -> str }
            
            // Regular characters (not backslash or quote)
            val charContent = +Regex("[^\\\\\"]+") map { it.value }
            
            // String content is a sequence of escape sequences or regular characters
            val stringContent = (escape + charContent).zeroOrMore map { chars ->
                chars.joinToString("")
            }
            val parser = -'\"' * stringContent * -'\"' map { content ->
                JsonValue.JsonString(content)
            }
        }.parser

        // JSON number parser (supports integers and floating-point)
        private val jsonNumber: Parser<JsonValue.JsonNumber> = 
            +Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?") map { match ->
                JsonValue.JsonNumber(match.value.toDouble())
            }

        // JSON boolean parser
        private val jsonBoolean: Parser<JsonValue.JsonBoolean> =
            (+"true" map { JsonValue.JsonBoolean(true) }) +
            (+"false" map { JsonValue.JsonBoolean(false) })

        // JSON null parser
        private val jsonNull: Parser<JsonValue.JsonNull> =
            +"null" map { JsonValue.JsonNull }

        // JSON array parser (recursive)
        private val jsonArray: Parser<JsonValue.JsonArray> = run {
            val element = ref { jsonValue }.trimmed()
            val elements = element.commaSeparated()
            -'[' * -ws * elements * -ws * -']' map { values ->
                JsonValue.JsonArray(values)
            }
        }

        // JSON object parser (recursive)
        private val jsonObject: Parser<JsonValue.JsonObject> = run {
            val key = jsonString.trimmed() map { it.value }
            val pair = key * -':' * -ws * ref { jsonValue }.trimmed() map { (k, v) ->
                k to v
            }
            val pairs = pair.commaSeparated()
            -'{' * -ws * pairs * -ws * -'}' map { properties ->
                JsonValue.JsonObject(properties.toMap())
            }
        }

        // Main JSON value parser (combines all types)
        val jsonValue: Parser<JsonValue> =
            jsonString +
            jsonNumber +
            jsonBoolean +
            jsonNull +
            jsonArray +
            jsonObject
    }

    @Test
    fun testJsonString() {
        val result = jsonValue.parseAllOrThrow("\"hello\"")
        assertEquals(JsonValue.JsonString("hello"), result)
    }

    @Test
    fun testJsonStringWithEscapes() {
        val result = jsonValue.parseAllOrThrow("\"hello\\nworld\\t!\"")
        assertEquals(JsonValue.JsonString("hello\nworld\t!"), result)
    }

    @Test
    fun testJsonStringWithUnicode() {
        val result = jsonValue.parseAllOrThrow("\"\\u0048\\u0065\\u006C\\u006C\\u006F\"")
        assertEquals(JsonValue.JsonString("Hello"), result)
    }

    @Test
    fun testJsonNumber() {
        val result = jsonValue.parseAllOrThrow("42")
        assertEquals(JsonValue.JsonNumber(42.0), result)
    }

    @Test
    fun testJsonNumberNegative() {
        val result = jsonValue.parseAllOrThrow("-123.45")
        assertEquals(JsonValue.JsonNumber(-123.45), result)
    }

    @Test
    fun testJsonNumberScientific() {
        val result = jsonValue.parseAllOrThrow("1.23e10")
        assertEquals(JsonValue.JsonNumber(1.23e10), result)
    }

    @Test
    fun testJsonBooleanTrue() {
        val result = jsonValue.parseAllOrThrow("true")
        assertEquals(JsonValue.JsonBoolean(true), result)
    }

    @Test
    fun testJsonBooleanFalse() {
        val result = jsonValue.parseAllOrThrow("false")
        assertEquals(JsonValue.JsonBoolean(false), result)
    }

    @Test
    fun testJsonNull() {
        val result = jsonValue.parseAllOrThrow("null")
        assertEquals(JsonValue.JsonNull, result)
    }

    @Test
    fun testJsonEmptyArray() {
        val result = jsonValue.parseAllOrThrow("[]")
        assertEquals(JsonValue.JsonArray(emptyList()), result)
    }

    @Test
    fun testJsonArrayWithNumbers() {
        val result = jsonValue.parseAllOrThrow("[1, 2, 3]")
        assertEquals(
            JsonValue.JsonArray(
                listOf(
                    JsonValue.JsonNumber(1.0),
                    JsonValue.JsonNumber(2.0),
                    JsonValue.JsonNumber(3.0)
                )
            ),
            result
        )
    }

    @Test
    fun testJsonArrayMixed() {
        val result = jsonValue.parseAllOrThrow("[1, \"hello\", true, null]")
        assertEquals(
            JsonValue.JsonArray(
                listOf(
                    JsonValue.JsonNumber(1.0),
                    JsonValue.JsonString("hello"),
                    JsonValue.JsonBoolean(true),
                    JsonValue.JsonNull
                )
            ),
            result
        )
    }

    @Test
    fun testJsonEmptyObject() {
        val result = jsonValue.parseAllOrThrow("{}")
        assertEquals(JsonValue.JsonObject(emptyMap()), result)
    }

    @Test
    fun testJsonSimpleObject() {
        val result = jsonValue.parseAllOrThrow("{\"name\": \"John\", \"age\": 30}")
        assertEquals(
            JsonValue.JsonObject(
                mapOf(
                    "name" to JsonValue.JsonString("John"),
                    "age" to JsonValue.JsonNumber(30.0)
                )
            ),
            result
        )
    }

    @Test
    fun testJsonNestedObject() {
        val json = """
            {
                "person": {
                    "name": "Alice",
                    "age": 25
                },
                "active": true
            }
        """.trimIndent()
        
        val result = jsonValue.parseAllOrThrow(json)
        assertEquals(
            JsonValue.JsonObject(
                mapOf(
                    "person" to JsonValue.JsonObject(
                        mapOf(
                            "name" to JsonValue.JsonString("Alice"),
                            "age" to JsonValue.JsonNumber(25.0)
                        )
                    ),
                    "active" to JsonValue.JsonBoolean(true)
                )
            ),
            result
        )
    }

    @Test
    fun testJsonNestedArray() {
        val result = jsonValue.parseAllOrThrow("[[1, 2], [3, 4], [5]]")
        assertEquals(
            JsonValue.JsonArray(
                listOf(
                    JsonValue.JsonArray(listOf(JsonValue.JsonNumber(1.0), JsonValue.JsonNumber(2.0))),
                    JsonValue.JsonArray(listOf(JsonValue.JsonNumber(3.0), JsonValue.JsonNumber(4.0))),
                    JsonValue.JsonArray(listOf(JsonValue.JsonNumber(5.0)))
                )
            ),
            result
        )
    }

    @Test
    fun testComplexJsonStructure() {
        val json = """
            {
                "name": "Product Catalog",
                "version": 1.5,
                "items": [
                    {
                        "id": 1,
                        "name": "Widget",
                        "price": 29.99,
                        "inStock": true,
                        "tags": ["electronics", "gadget"]
                    },
                    {
                        "id": 2,
                        "name": "Gizmo",
                        "price": 19.99,
                        "inStock": false,
                        "tags": ["tool"]
                    }
                ],
                "metadata": {
                    "lastUpdated": "2023-12-01",
                    "count": 2
                }
            }
        """.trimIndent()
        
        val result = jsonValue.parseAllOrThrow(json)
        
        // Verify structure exists and is correct type
        when (result) {
            is JsonValue.JsonObject -> {
                assertEquals(4, result.properties.size)
                assertEquals(JsonValue.JsonString("Product Catalog"), result.properties["name"])
                assertEquals(JsonValue.JsonNumber(1.5), result.properties["version"])
                
                val items = result.properties["items"] as JsonValue.JsonArray
                assertEquals(2, items.values.size)
                
                val metadata = result.properties["metadata"] as JsonValue.JsonObject
                assertEquals(2, metadata.properties.size)
            }
            else -> error("Expected JsonObject")
        }
    }

    @Test
    fun testJsonWithWhitespace() {
        val json = """
            {
              "key1"  :  "value1"  ,
              "key2"  :  42
            }
        """.trimIndent()
        
        val result = jsonValue.parseAllOrThrow(json)
        assertEquals(
            JsonValue.JsonObject(
                mapOf(
                    "key1" to JsonValue.JsonString("value1"),
                    "key2" to JsonValue.JsonNumber(42.0)
                )
            ),
            result
        )
    }
}
