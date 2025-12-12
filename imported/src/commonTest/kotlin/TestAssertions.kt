import mirrg.xarpite.parser.ExtraCharactersParseException
import mirrg.xarpite.parser.UnmatchedInputParseException
import kotlin.test.fail

fun assertExtraCharacters(block: () -> Unit) {
    try {
        block()
        fail("Expected ExtraCharactersParseException, but no exception was thrown.")
    } catch (_: ExtraCharactersParseException) {
        // ok
    } catch (e: Throwable) {
        fail("Expected ExtraCharactersParseException, but got ${e::class}", e)
    }
}

fun assertUnmatchedInput(block: () -> Unit) {
    try {
        block()
        fail("Expected UnmatchedInputParseException, but no exception was thrown.")
    } catch (_: UnmatchedInputParseException) {
        // ok
    } catch (e: Throwable) {
        fail("Expected UnmatchedInputParseException, but got ${e::class}", e)
    }
}
