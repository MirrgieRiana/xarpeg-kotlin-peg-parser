package io.github.mirrgieriana.xarpeg

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
        assertEquals('a', parser.parseAllOrThrow("a")) // 全体にマッチできる
        assertExtraCharacters { parser.parseAllOrThrow("ab") } // 末尾にゴミがあると失敗
        assertUnmatchedInput { parser.parseAllOrThrow("ba") } // 先頭にゴミがあると失敗
    }

    @Test
    fun charParser() {
        val parser = +'a'
        assertEquals('a', parser.parseAllOrThrow("a")) // 同じ文字で成功
        assertUnmatchedInput { parser.parseAllOrThrow("b") } // 異なる文字で失敗
    }

    @Test
    fun stringParser() {
        val parser = +"abc"
        assertEquals("abc", parser.parseAllOrThrow("abc")) // 同じ文字列で成功
        assertUnmatchedInput { parser.parseAllOrThrow("abd") } // 異なる文字列で失敗
    }

    @Test
    fun regexParser() {
        val parser = +Regex("[1-9]+")
        assertNotNull(parser.parseAllOrThrow("123")) // 正規表現にマッチする文字列で成功
        assertUnmatchedInput { parser.parseAllOrThrow("abc") } // 正規表現にマッチしない文字列で失敗
        assertUnmatchedInput { parser.parseAllOrThrow("a123") } // 先頭にゴミがあると失敗
    }

    @Test
    fun unitParser() {
        val parser = fixed(1)
        assertEquals(1, parser.parseAllOrThrow("")) // 空文字で成功
        assertExtraCharacters { parser.parseAllOrThrow("a") } // 何も消費しない
    }

    @Test
    fun nothingParser() {
        val parser = fail
        assertUnmatchedInput { parser.parseAllOrThrow("") } // 何を与えても失敗
        assertUnmatchedInput { parser.parseAllOrThrow("a") } // 何を与えても失敗
    }

    @Test
    fun tupleParsers() {

        // Tuple5
        run {
            val parser = +'a' * +'b' * +'c' * +'d' * +'e'

            // それぞれ順番にマッチする文字列で成功
            assertEquals(
                Tuple5('a', 'b', 'c', 'd', 'e'),
                parser.parseAllOrThrow("abcde"),
            )

            // どこかでマッチしないと失敗
            assertUnmatchedInput { parser.parseAllOrThrow("fffff") }
            assertUnmatchedInput { parser.parseAllOrThrow("affff") }
            assertUnmatchedInput { parser.parseAllOrThrow("abfff") }
            assertUnmatchedInput { parser.parseAllOrThrow("abcff") }
            assertUnmatchedInput { parser.parseAllOrThrow("abcdf") }
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
                parser.parseAllOrThrow("abcdefghijklmnop"),
            )
        }

        // Tuple0同士の結合でも副作用は適用される
        run {
            val parser = (+'a' map { Tuple0 }) * (+'b' map { Tuple0 })
            assertEquals(Tuple0, parser.parseAllOrThrow("ab")) // マッチする文字列で成功
        }

    }

    @Test
    fun listParser() {

        // zeroOrMore
        run {
            val parser = (+'a').zeroOrMore
            assertEquals(listOf(), parser.parseAllOrThrow("")) // 0回で成功
            assertEquals(listOf('a'), parser.parseAllOrThrow("a")) // 1回で成功
            assertEquals(listOf('a', 'a', 'a'), parser.parseAllOrThrow("aaa")) // 複数回で成功
            assertExtraCharacters { parser.parseAllOrThrow("abc") } // 余計な文字があると失敗
        }

        // oneOrMore
        run {
            val parser = (+'a').oneOrMore
            assertUnmatchedInput { parser.parseAllOrThrow("") } // 0回で失敗
            assertEquals(listOf('a'), parser.parseAllOrThrow("a"))
            assertEquals(listOf('a', 'a', 'a'), parser.parseAllOrThrow("aaa"))
            assertExtraCharacters { parser.parseAllOrThrow("abc") }
        }

        // 範囲指定
        run {
            val parser1 = (+'a').list(min = 2, max = 4)
            assertUnmatchedInput { parser1.parseAllOrThrow("a") }
            assertEquals(listOf('a', 'a'), parser1.parseAllOrThrow("aa"))
            assertEquals(listOf('a', 'a', 'a', 'a'), parser1.parseAllOrThrow("aaaa"))
            assertExtraCharacters { parser1.parseAllOrThrow("aaaaa") }
        }

        // 後続のマッチする部分を無視する
        run {
            val parser1 = (+'a').list(min = 2, max = 2).list(min = 2, max = 2)
            assertEquals(listOf(listOf('a', 'a'), listOf('a', 'a')), parser1.parseAllOrThrow("aaaa"))
        }

    }

    @Test
    fun optionalParser() {

        // 単体
        run {
            val parser = (+'a').optional
            assertEquals(Tuple1('a'), parser.parseAllOrThrow("a")) // マッチする場合に成功
            assertEquals(Tuple1(null), parser.parseAllOrThrow("")) // 省略された場合に成功
            assertExtraCharacters { parser.parseAllOrThrow("b") } // マッチしない文字で失敗
        }

        // マッチしない場合は解析位置も変更しない
        run {
            val parser = (+'a').optional * +'b'
            assertEquals(Tuple2('a', 'b'), parser.parseAllOrThrow("ab")) // マッチする場合に成功
            assertEquals(Tuple2(null, 'b'), parser.parseAllOrThrow("b")) // 省略された場合に成功
        }

    }

    @Test
    fun orParser() {

        // 0項
        run {
            val parser = or<Char>()
            assertUnmatchedInput { parser.parseAllOrThrow("") } // 何を与えても失敗
            assertUnmatchedInput { parser.parseAllOrThrow("a") }
        }

        // 1項
        run {
            val parser = or(+'a')
            assertEquals('a', parser.parseAllOrThrow("a")) // 最初の選択肢にマッチ
            assertUnmatchedInput { parser.parseAllOrThrow("b") } // マッチしなかった
        }

        // 2項
        run {
            val parser = +'a' + +'b'
            assertEquals('a', parser.parseAllOrThrow("a"))
            assertEquals('b', parser.parseAllOrThrow("b")) // 2番目の選択肢にマッチ
            assertUnmatchedInput { parser.parseAllOrThrow("c") }
        }

    }

    @Test
    fun notParser() {
        val parser = !+'a' * +'b'
        assertEquals('b', parser.parseAllOrThrow("b")) // 最初の子パーサーがマッチしない文字で成功
        assertUnmatchedInput { parser.parseAllOrThrow("a") } // 最初の子パーサーがマッチする文字で失敗
    }

    @Test
    fun map() {
        val parser = +Regex("[1-9a-z]+") map { it.value.toInt() }
        assertEquals(123, parser.parseAllOrThrow("123")) // 正規表現にマッチしつつ数値に変換できる
        assertFails { parser.parseAllOrThrow("123a") } // 数値化部分が失敗すると失敗
    }

    @Test
    fun ignoreParser() {
        val parser = -'a'
        assertEquals(Tuple0, parser.parseAllOrThrow("a")) // マッチする文字で成功
        assertUnmatchedInput { parser.parseAllOrThrow("b") } // マッチしない文字で失敗
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

        assertEquals(26, parser.parseAllOrThrow("2*3+4*5")) // まずはデリゲート使わない
        assertEquals(70, parser.parseAllOrThrow("2*(3+4)*5")) // デリゲートを使う
        assertEquals(7, parser.parseAllOrThrow("(((((((((((((((((((((((((3+4)))))))))))))))))))))))))")) // 大量の入れ子デリゲート
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
            parser.parseAllOrThrow("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", useMemoization = false)
            fail("Expected CancellationException, but no exception was thrown.")
        } catch (_: CancellationException) {
            // ok
        }

        // キャッシュを使うことで計算回数が下がり成功するようになる
        language.counter = 0
        assertEquals(40, parser.parseAllOrThrow("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", useMemoization = true))

    }

    @Test
    fun associative() {

        // leftAssociative
        run {
            val number = +Regex("[0-9]+") map { it.value }
            val add = leftAssociative(number, -'+') { a, _, b -> "[$a+$b]" }

            assertEquals("[[1+2]+3]", add.parseAllOrThrow("1+2+3")) // 左優先結合
            assertEquals("[1+2]", add.parseAllOrThrow("1+2")) // 1回の場合
            assertEquals("1", add.parseAllOrThrow("1")) // 0回の場合
            assertUnmatchedInput { add.parseAllOrThrow("") } // どれも来ない場合は失敗
        }

        // rightAssociative
        run {
            val number = +Regex("[0-9]+") map { it.value }
            val add = rightAssociative(number, -'+') { a, _, b -> "[$a+$b]" }

            assertEquals("[1+[2+3]]", add.parseAllOrThrow("1+2+3")) // 右優先結合
            assertEquals("[1+2]", add.parseAllOrThrow("1+2")) // 1回の場合
            assertEquals("1", add.parseAllOrThrow("1")) // 0回の場合
            assertUnmatchedInput { add.parseAllOrThrow("") } // どれも来ない場合は失敗
        }

    }

    companion object {

        private fun assertExtraCharacters(block: () -> Unit) {
            try {
                block()
                fail("Expected ExtraCharactersParseException, but no exception was thrown.")
            } catch (_: ExtraCharactersParseException) {
                // ok
            } catch (e: Throwable) {
                fail("Expected ExtraCharactersParseException, but got ${e::class}", e)
            }
        }

        private fun assertUnmatchedInput(block: () -> Unit) {
            try {
                block()
                fail("Expected UnmatchedInputParseException, but no exception was thrown.")
            } catch (_: UnmatchedInputParseException) {
                // ok
            } catch (e: Throwable) {
                fail("Expected UnmatchedInputParseException, but got ${e::class}", e)
            }
        }

    }
}
