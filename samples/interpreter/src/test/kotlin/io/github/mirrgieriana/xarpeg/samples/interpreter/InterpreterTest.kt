package io.github.mirrgieriana.xarpeg.samples.interpreter

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * インタープリターの動作をテストする
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InterpreterTest {

    private lateinit var classPath: String

    @BeforeAll
    fun setup() {
        val projectDir = File(System.getProperty("user.dir"))
        val gradlew = if (System.getProperty("os.name").startsWith("Windows")) {
            File(projectDir, "gradlew.bat")
        } else {
            File(projectDir, "gradlew")
        }

        val buildProcess = ProcessBuilder(gradlew.absolutePath, "installDist", "--console=plain")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        val buildExitCode = buildProcess.waitFor()
        if (buildExitCode != 0) {
            val output = buildProcess.inputStream.bufferedReader().readText()
            throw AssertionError("Failed to build distribution: $output")
        }

        val libDir = File(projectDir, "build/install/interpreter/lib")
        assertTrue(libDir.exists(), "Distribution lib directory should exist: ${libDir.absolutePath}")

        val jarFiles = libDir.listFiles { file -> file.extension == "jar" }
            ?: throw AssertionError("No jar files found in ${libDir.absolutePath}")

        classPath = jarFiles.joinToString(File.pathSeparator) { it.absolutePath }
    }

    private fun runInterpreter(expression: String): Pair<Int, String> {
        val process = ProcessBuilder(
            "java",
            "-cp",
            classPath,
            "io.github.mirrgieriana.xarpeg.samples.interpreter.MainKt",
            "-e",
            expression
        )
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        return Pair(exitCode, output)
    }

    @Test
    fun testBasicAddition() {
        val (exitCode, output) = runInterpreter("2+3")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("5", output, "2+3 should equal 5")
    }

    @Test
    fun testMultiplication() {
        val (exitCode, output) = runInterpreter("3*4")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("12", output, "3*4 should equal 12")
    }

    @Test
    fun testOperatorPrecedence() {
        val (exitCode, output) = runInterpreter("2+3*4")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("14", output, "2+3*4 should equal 14 (multiplication before addition)")
    }

    @Test
    fun testParentheses() {
        val (exitCode, output) = runInterpreter("(2+3)*4")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("20", output, "(2+3)*4 should equal 20")
    }

    @Test
    fun testDivision() {
        val (exitCode, output) = runInterpreter("20/4")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("5", output, "20/4 should equal 5")
    }

    @Test
    fun testSubtraction() {
        val (exitCode, output) = runInterpreter("10-3")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("7", output, "10-3 should equal 7")
    }

    @Test
    fun testComplexExpression() {
        val (exitCode, output) = runInterpreter("10+20*3-5")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("65", output, "10+20*3-5 should equal 65")
    }

    @Test
    fun testDivisionByZero() {
        val (exitCode, output) = runInterpreter("10/0")
        assertEquals(0, exitCode, "Should exit successfully (error handled)")
        assertTrue(output.contains("Division by zero"), "Should report division by zero error")
        assertTrue(output.contains("line 1"), "Should report line number")
        assertTrue(output.contains("column 3"), "Should report column number")
    }

    @Test
    fun testDivisionByZeroInComplexExpression() {
        val (exitCode, output) = runInterpreter("10+20/(5-5)")
        assertEquals(0, exitCode, "Should exit successfully (error handled)")
        assertTrue(output.contains("Division by zero"), "Should report division by zero error")
        assertTrue(output.contains("line 1"), "Should report line number")
        assertTrue(output.contains("column 6"), "Should report column number")
    }

    @Test
    fun testNestedParentheses() {
        val (exitCode, output) = runInterpreter("((2+3)*4)+5")
        assertEquals(0, exitCode, "Should exit successfully")
        assertEquals("25", output, "((2+3)*4)+5 should equal 25")
    }
}
