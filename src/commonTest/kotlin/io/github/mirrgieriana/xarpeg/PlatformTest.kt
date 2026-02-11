package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.parsers.toParser
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * プラットフォーム固有の動作をテストする
 */
class PlatformTest {

    @Test
    fun stringParserCachesOnNonNativePlatforms() {
        if (isNative) return

        val first = "abc".toParser()
        val second = "abc".toParser()

        assertSame(first, second)
    }
}
