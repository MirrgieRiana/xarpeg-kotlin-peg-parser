import mirrg.xarpite.parser.ExtraCharactersParseException
import mirrg.xarpite.parser.UnmatchedInputParseException
import kotlin.test.fail

fun assertExtraCharacters(block: () -> Unit) {
    try {
        block()
        fail("Expected ExtraCharactersParseException")
    } catch (_: ExtraCharactersParseException) {
        // ok
    }
}

fun assertUnmatchedInput(block: () -> Unit) {
    try {
        block()
        fail("Expected UnmatchedInputParseException")
    } catch (_: UnmatchedInputParseException) {
        // ok
    }
}
