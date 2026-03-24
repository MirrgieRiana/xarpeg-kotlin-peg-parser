package io.github.mirrgieriana.xarpeg.samples.online.parser

/**
 * A unit of execution that mutates an [ExecutionFrame].
 * Unlike [Expression], a statement does not return a value directly.
 */
interface Statement {
    /**
     * Executes this statement, potentially updating the frame's context and last value.
     */
    fun execute(frame: ExecutionFrame)
}
