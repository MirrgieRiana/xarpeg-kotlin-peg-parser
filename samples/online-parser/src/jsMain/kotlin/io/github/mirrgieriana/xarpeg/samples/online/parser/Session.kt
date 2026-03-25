package io.github.mirrgieriana.xarpeg.samples.online.parser

/**
 * Shared state for a single evaluation session.
 * Holds session-wide constants and resource limits that persist across all contexts.
 */
class Session(
    val sourceCode: String? = null,
) {
    private var callCount = 0

    /**
     * Increments the function call count and throws if the limit is exceeded.
     * Prevents infinite recursion from freezing the page.
     */
    fun incrementCallCount(ctx: Expression.EvaluationContext) {
        callCount++
        if (callCount >= MAX_FUNCTION_CALLS) {
            throw EvaluationException("Maximum function call limit ($MAX_FUNCTION_CALLS) exceeded", ctx, sourceCode)
        }
    }

    companion object {
        private const val MAX_FUNCTION_CALLS = 100
    }
}
