package io.github.mirrgieriana.xarpeg

import io.github.mirrgieriana.xarpeg.DefaultParseContext
import io.github.mirrgieriana.xarpeg.parsers.fail
import io.github.mirrgieriana.xarpeg.parsers.fixed
import io.github.mirrgieriana.xarpeg.parsers.leftAssociative
import io.github.mirrgieriana.xarpeg.parsers.list
import io.github.mirrgieriana.xarpeg.parsers.map
import io.github.mirrgieriana.xarpeg.parsers.not
import io.github.mirrgieriana.xarpeg.parsers.oneOrMore
import io.github.mirrgieriana.xarpeg.parsers.optional
import io.github.mirrgieriana.xarpeg.parsers.or
import io.github.mirrgieriana.xarpeg.parsers.plus
import io.github.mirrgieriana.xarpeg.parsers.ref
import io.github.mirrgieriana.xarpeg.parsers.rightAssociative
import io.github.mirrgieriana.xarpeg.parsers.times
import io.github.mirrgieriana.xarpeg.parsers.unaryMinus
import io.github.mirrgieriana.xarpeg.parsers.unaryPlus
import io.github.mirrgieriana.xarpeg.parsers.zeroOrMore
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.fail

class ParserTest {

    @Test
    fun parse() {
        val parser = +'a'
        assertEquals('a', parser.parseAll("a").getOrThrow()) // 全体にマッチできる
        assertExtraCharacters { parser.parseAll("ab").getOrThrow() } // 末尾にゴミがあると失敗
        assertUnmatchedInput { parser.parseAll("ba").getOrThrow() } // 先頭にゴミがあると失敗
    }

    @Test
    fun charParser() {
        val parser = +'a'
        assertEquals('a', parser.parseAll("a").getOrThrow()) // 同じ文字で成功
        assertUnmatchedInput { parser.parseAll("b").getOrThrow() } // 異なる文字で失敗
    }

    @Test
    fun stringParser() {
        val parser = +"abc"
        assertEquals("abc", parser.parseAll("abc").getOrThrow()) // 同じ文字列で成功
        assertUnmatchedInput { parser.parseAll("abd").getOrThrow() } // 異なる文字列で失敗
    }

    @Test
    fun regexParser() {
        val parser = +Regex("[1-9]+")
        assertNotNull(parser.parseAll("123").getOrThrow()) // 正規表現にマッチする文字列で成功
        assertUnmatchedInput { parser.parseAll("abc").getOrThrow() } // 正規表現にマッチしない文字列で失敗
        assertUnmatchedInput { parser.parseAll("a123").getOrThrow() } // 先頭にゴミがあると失敗
    }

    @Test
    fun unitParser() {
        val parser = fixed(1)
        assertEquals(1, parser.parseAll("").getOrThrow()) // 空文字で成功
        assertExtraCharacters { parser.parseAll("a").getOrThrow() } // 何も消費しない
    }

    @Test
    fun nothingParser() {
        val parser = fail
        assertUnmatchedInput { parser.parseAll<Unit>("").getOrThrow() } // 何を与えても失敗
        assertUnmatchedInput { parser.parseAll<Unit>("a").getOrThrow() } // 何を与えても失敗
    }

    @Test
    fun tupleParsers() {

        // Tuple5
        run {
            val parser = +'a' * +'b' * +'c' * +'d' * +'e'

            // それぞれ順番にマッチする文字列で成功
            assertEquals(
                Tuple5('a', 'b', 'c', 'd', 'e'),
                parser.parseAll("abcde").getOrThrow(),
            )

            // どこかでマッチしないと失敗
            assertUnmatchedInput { parser.parseAll("fffff").getOrThrow() }
            assertUnmatchedInput { parser.parseAll("affff").getOrThrow() }
            assertUnmatchedInput { parser.parseAll("abfff").getOrThrow() }
            assertUnmatchedInput { parser.parseAll("abcff").getOrThrow() }
            assertUnmatchedInput { parser.parseAll("abcdf").getOrThrow() }
        }

        // Tuple16
        run {
            val parser =
                +'a' * +'b' * +'c' * +'d' *
                    +'e' * +'f' * +'g' * +'h' *
                    +'i' * +'j' * +'k' * +'l' *
                    +'m' * +'n' * +'o' * +'p'

            assertEquals(
                Tuple16('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p'),
                parser.parseAll("abcdefghijklmnop").getOrThrow(),
            )
        }

        // Tuple0同士の結合でも副作用は適用される
        run {
            val parser = (+'a' map { Tuple0 }) * (+'b' map { Tuple0 })
            assertEquals(Tuple0, parser.parseAll("ab").getOrThrow()) // マッチする文字列で成功
        }

    }

    @Test
    fun listParser() {

        // zeroOrMore
        run {
            val parser = (+'a').zeroOrMore
            assertEquals(listOf(), parser.parseAll("").getOrThrow()) // 0回で成功
            assertEquals(listOf('a'), parser.parseAll("a").getOrThrow()) // 1回で成功
            assertEquals(listOf('a', 'a', 'a'), parser.parseAll("aaa").getOrThrow()) // 複数回で成功
            assertExtraCharacters { parser.parseAll("abc").getOrThrow() } // 余計な文字があると失敗
        }

        // oneOrMore
        run {
            val parser = (+'a').oneOrMore
            assertUnmatchedInput { parser.parseAll("").getOrThrow() } // 0回で失敗
            assertEquals(listOf('a'), parser.parseAll("a").getOrThrow())
            assertEquals(listOf('a', 'a', 'a'), parser.parseAll("aaa").getOrThrow())
            assertExtraCharacters { parser.parseAll("abc").getOrThrow() }
        }

        // 範囲指定
        run {
            val parser1 = (+'a').list(min = 2, max = 4)
            assertUnmatchedInput { parser1.parseAll("a").getOrThrow() }
            assertEquals(listOf('a', 'a'), parser1.parseAll("aa").getOrThrow())
            assertEquals(listOf('a', 'a', 'a', 'a'), parser1.parseAll("aaaa").getOrThrow())
            assertExtraCharacters { parser1.parseAll("aaaaa").getOrThrow() }
        }

        // 後続のマッチする部分を無視する
        run {
            val parser1 = (+'a').list(min = 2, max = 2).list(min = 2, max = 2)
            assertEquals(listOf(listOf('a', 'a'), listOf('a', 'a')), parser1.parseAll("aaaa").getOrThrow())
        }

    }

    @Test
    fun optionalParser() {

        // 単体
        run {
            val parser = (+'a').optional
            assertEquals(Tuple1('a'), parser.parseAll("a").getOrThrow()) // マッチする場合に成功
            assertEquals(Tuple1(null), parser.parseAll("").getOrThrow()) // 省略された場合に成功
            assertExtraCharacters { parser.parseAll("b").getOrThrow() } // マッチしない文字で失敗
        }

        // マッチしない場合は解析位置も変更しない
        run {
            val parser = (+'a').optional * +'b'
            assertEquals(Tuple2('a', 'b'), parser.parseAll("ab").getOrThrow()) // マッチする場合に成功
            assertEquals(Tuple2(null, 'b'), parser.parseAll("b").getOrThrow()) // 省略された場合に成功
        }

    }

    @Test
    fun orParser() {

        // 0項
        run {
            val parser = or<Char>()
            assertUnmatchedInput { parser.parseAll("").getOrThrow() } // 何を与えても失敗
            assertUnmatchedInput { parser.parseAll("a").getOrThrow() }
        }

        // 1項
        run {
            val parser = or(+'a')
            assertEquals('a', parser.parseAll("a").getOrThrow()) // 最初の選択肢にマッチ
            assertUnmatchedInput { parser.parseAll("b").getOrThrow() } // マッチしなかった
        }

        // 2項
        run {
            val parser = +'a' + +'b'
            assertEquals('a', parser.parseAll("a").getOrThrow())
            assertEquals('b', parser.parseAll("b").getOrThrow()) // 2番目の選択肢にマッチ
            assertUnmatchedInput { parser.parseAll("c").getOrThrow() }
        }

    }

    @Test
    fun notParser() {
        val parser = !+'a' * +'b'
        assertEquals('b', parser.parseAll("b").getOrThrow()) // 最初の子パーサーがマッチしない文字で成功
        assertUnmatchedInput { parser.parseAll("a").getOrThrow() } // 最初の子パーサーがマッチする文字で失敗
    }

    @Test
    fun map() {
        val parser = +Regex("[1-9a-z]+") map { it.value.toInt() }
        assertEquals(123, parser.parseAll("123").getOrThrow()) // 正規表現にマッチしつつ数値に変換できる
        assertFails { parser.parseAll("123a").getOrThrow() } // 数値化部分が失敗すると失敗
    }

    @Test
    fun ignoreParser() {
        val parser = -'a'
        assertEquals(Tuple0, parser.parseAll("a").getOrThrow()) // マッチする文字で成功
        assertUnmatchedInput { parser.parseAll("b").getOrThrow() } // マッチしない文字で失敗
    }

    @Test
    fun delegationParser() {
        val parser = object {
            val number = +Regex("[0-9]+") map { it.value.toInt() }
            val brackets: Parser<Int> = -'(' * ref { root } * -')'
            val factor = number + brackets
            val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
            val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
            val root = add
        }.root

        assertEquals(26, parser.parseAll("2*3+4*5").getOrThrow()) // まずはデリゲート使わない
        assertEquals(70, parser.parseAll("2*(3+4)*5").getOrThrow()) // デリゲートを使う
        assertEquals(7, parser.parseAll("(((((((((((((((((((((((((3+4)))))))))))))))))))))))))").getOrThrow()) // 大量の入れ子デリゲート
    }

    @Test
    fun cache() {
        val language = object {
            var counter = 0

            // 評価する度に評価された回数をカウントして多すぎる場合に例外を出すパーサー
            val a = -"a" map {
                counter++
                // 入力文字列に対してこのパーサーを1000回も呼び出す時点でおかしい
                if (counter >= 1000) throw CancellationException(null, null)
                1
            }
            val aa = -"aa" map { 2 }

            // 入力されたaを分割する全パターンを試そうとするパーサー
            val root: Parser<Int> = run {
                or(
                    // bの位置で確定で失敗し、次の選択肢に進む
                    // bより前に自分自身が居るので、rootが評価される度にrootが合計2回呼ばれる
                    // これにより入力されたaの長さに対して指数関数的に計算時間が伸びる
                    a * ref { root } * -"b" map { it.a + it.b },
                    aa * ref { root } map { it.a + it.b },

                    // 成功ケース用の終端
                    fixed(0),
                )
            }
        }
        val parser = language.root

        // キャッシュを使わない場合、計算回数が指数関数的に増加するのでキャンセルを踏む
        try {
            language.counter = 0
            parser.parseAll("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa") { DefaultParseContext(it).also { c -> c.useMemoization = false } }.getOrThrow()
            fail("Expected CancellationException, but no exception was thrown.")
        } catch (_: CancellationException) {
            // ok
        }

        // キャッシュを使うことで計算回数が下がり成功するようになる
        language.counter = 0
        assertEquals(40, parser.parseAll("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").getOrThrow())

    }

    @Test
    fun associative() {

        // leftAssociative
        run {
            val number = +Regex("[0-9]+") map { it.value }
            val add = leftAssociative(number, -'+') { a, _, b -> "[$a+$b]" }

            assertEquals("[[1+2]+3]", add.parseAll("1+2+3").getOrThrow()) // 左優先結合
            assertEquals("[1+2]", add.parseAll("1+2").getOrThrow()) // 1回の場合
            assertEquals("1", add.parseAll("1").getOrThrow()) // 0回の場合
            assertUnmatchedInput { add.parseAll("").getOrThrow() } // どれも来ない場合は失敗
        }

        // rightAssociative
        run {
            val number = +Regex("[0-9]+") map { it.value }
            val add = rightAssociative(number, -'+') { a, _, b -> "[$a+$b]" }

            assertEquals("[1+[2+3]]", add.parseAll("1+2+3").getOrThrow()) // 右優先結合
            assertEquals("[1+2]", add.parseAll("1+2").getOrThrow()) // 1回の場合
            assertEquals("1", add.parseAll("1").getOrThrow()) // 0回の場合
            assertUnmatchedInput { add.parseAll("").getOrThrow() } // どれも来ない場合は失敗
        }

    }

    companion object {

        private fun assertExtraCharacters(block: () -> Unit) {
            try {
                block()
                fail("Expected ParseException, but no exception was thrown.")
            } catch (_: ParseException) {
                // ok
            } catch (e: Throwable) {
                fail("Expected ParseException, but got ${e::class}", e)
            }
        }

        private fun assertUnmatchedInput(block: () -> Unit) {
            try {
                block()
                fail("Expected ParseException, but no exception was thrown.")
            } catch (_: ParseException) {
                // ok
            } catch (e: Throwable) {
                fail("Expected ParseException, but got ${e::class}", e)
            }
        }

    }
}
