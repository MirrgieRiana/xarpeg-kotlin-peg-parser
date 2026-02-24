---
layout: docs-ja
title: ステップ4 – 実行時の動作
---

# ステップ4：実行時の動作

パーサがエラーを処理し、入力を消費し、最適なパフォーマンスのためにキャッシングを制御する方法を理解します。

## 解析メソッド

### `parseAll().getOrThrow()`

入力全体が消費されることを要求します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"

fun main() {
    number.parseAll("123").getOrThrow()      // ✓ 123を返す
    // number.parseAll("123abc").getOrThrow() // ✗ ParseException
    // number.parseAll("abc").getOrThrow()    // ✗ ParseException
}
```

### 例外の種類

- **`ParseException`** - 現在位置でパーサがマッチしなかった場合、または解析は成功したが末尾の入力が残っている場合

この例外は、詳細なエラー情報のための`context`プロパティを提供します。

## エラーコンテキスト

`ParseContext`は、ユーザーフレンドリーなエラーメッセージを構築するために解析の失敗を追跡します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val letter = +Regex("[a-z]") map { it.value } named "letter"
val digit = +Regex("[0-9]") map { it.value } named "digit"
val identifier = letter * (letter + digit).zeroOrMore

fun main() {
    val result = identifier.parseAll("1abc")
    val exception = result.exceptionOrNull() as? ParseException

    check(exception != null)  // 解析失敗
    check(exception.context.errorPosition == 0)  // 位置0で失敗

    val expected = exception.context.suggestedParsers
        ?.mapNotNull { it.name }
        ?.distinct()
        ?.sorted()
        ?.joinToString(", ") ?: ""

    check(expected == "letter")  // "letter"が期待される
}
```

### エラー追跡プロパティ

- **`errorPosition`** - 解析中に試行された最も遠い位置
- **`suggestedParsers`** - `errorPosition`で失敗したパーサのセット

解析が進むにつれて：
1. パーサが`errorPosition`より遠くで失敗した場合、更新され、`suggestedParsers`がクリアされます
2. 現在の`errorPosition`で失敗したパーサが`suggestedParsers`に追加されます
3. 名前付きパーサは割り当てられた名前を使用して表示されます

### 例外でのエラーコンテキストの使用

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val operator = (+'*' + +'+') named "operator"
val expr = number * operator * number

fun main() {
    val result = expr.parseAll("42 + 10")
    val exception = result.exceptionOrNull() as? ParseException

    check(exception != null)  // 解析失敗
    check((exception.context.errorPosition ?: 0) > 0)  // エラー位置が追跡される
    val suggestions = exception.context.suggestedParsers?.mapNotNull { it.name } ?: emptyList()
    check(suggestions.isNotEmpty())  // 提案がある
}
```

### リッチなエラーメッセージ

`formatMessage`拡張関数を使用すると、エラー位置、期待される要素、該当行のソースコード、エラー箇所を示すキャレット表示を含むユーザーフレンドリーなエラーメッセージを生成できます：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val operator = +'+' + +'-'
val expr = number * operator * number

fun main() {
    val input = "42*10"
    try {
        expr.parseAll(input).getOrThrow()
    } catch (exception: ParseException) {
        val message = exception.formatMessage()
        val lines = message.lines()
        check(lines[0] == "Syntax Error at 1:3")
        check(lines[1] == "Expect: \"+\", \"-\"")
        check(lines[2] == "Actual: \"*\"")
        check(lines[3] == "42*10")
        check(lines[4] == "  ^")
    }
}
```

`formatMessage`関数は以下を提供します：
- エラーの行番号と列番号
- 期待される名前付きパーサのリスト（利用可能な場合）
- 実際に見つかった文字（またはEOF）
- エラーが発生した行のソースコード
- エラー位置を示すキャレット（`^`）記号

## メモ化とキャッシング

### デフォルトの動作

`ParseContext`はデフォルトでメモ化を使用して、バックトラッキングを予測可能にします：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = +Regex("[a-z]+") map { it.value } named "word"

fun main() {
    // メモ化有効（デフォルト）
    parser.parseAll("hello") { ctx ->
        DefaultParseContext(ctx).apply { useMemoization = true }
    }.getOrThrow()
}
```

各`(parser, position)`ペアがメモ化されるため、同じ位置での繰り返しの試行はメモ化された結果を返します。

### メモ化の無効化

文法が大量のバックトラックをしない場合、メモリ使用量を減らすためにメモ化を無効化します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = +Regex("[a-z]+") map { it.value } named "word"

fun main() {
    parser.parseAll("hello") { ctx ->
        DefaultParseContext(ctx).apply { useMemoization = false }
    }.getOrThrow()
}
```

**トレードオフ：**
- **メモ化有効** - 高メモリ、大量のバックトラックで予測可能なパフォーマンス
- **メモ化無効** - 低メモリ、代替案で潜在的なパフォーマンス問題

## エラー伝播

`map`関数が例外をスローした場合、それは伝播して解析を中止します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val divisionByZero = +Regex("[0-9]+") map { value ->
    val n = value.value.toInt()
    if (n == 0) error("Cannot divide by zero")
    100 / n
} named "number"

fun main() {
    divisionByZero.parseAll("10").getOrThrow()  // ✓ 10を返す
    // divisionByZero.parseAll("0").getOrThrow()  // ✗ IllegalStateException
}
```

マッピング前に検証するか、回復が必要な場合はエラーをキャッチしてラップします。

## デバッグのヒント

### 結果からエラー詳細を検査

解析結果からエラーコンテキストにアクセス：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = +Regex("[a-z]+") named "word"

fun main() {
    val result = parser.parseAll("123")
    val exception = result.exceptionOrNull() as? ParseException

    check(exception != null)  // 解析失敗
    check(exception.context.errorPosition == 0)  // 位置0でエラー
    check(exception.context.suggestedParsers?.any { it.name == "word" } ?: false)  // "word"を提案
}
```

### 巻き戻し動作の確認

`optional`と`zeroOrMore`が失敗時にどのように巻き戻すかを確認：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val parser = (+Regex("[a-z]+") named "letters").optional * +Regex("[0-9]+") named "digits"

fun main() {
    // optionalは失敗するが巻き戻し、数値パーサが成功できる
    val result = parser.parseAll("123").getOrThrow()
    check(result != null)  // 成功
}
```

### リファレンスとしてテストを使用

観測された動作についてはテストスイートを確認：
- **[ErrorContextTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpeg/ErrorContextTest.kt)** - エラー追跡の例
- **[ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpeg/ParserTest.kt)** - 包括的な動作テスト

## ParseContextの拡張

`ParseContext`は`open class`として宣言されているため、特殊な解析ニーズに応じてカスタム状態を持つ拡張が可能です。

### 例：インデント方式言語のサポート

Python風の言語のインデントレベルを追跡するために`DefaultParseContext`を拡張できます：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*
import io.github.mirrgieriana.xarpeg.DefaultParseContext

fun main() {
    class IndentParseContext(
        src: String,
    ) : DefaultParseContext(src) {
        private val indentStack = mutableListOf(0)

        val currentIndent: Int get() = indentStack.last()
        val isInIndentBlock: Boolean get() = indentStack.size > 1

        fun pushIndent(indent: Int) {
            require(indent > currentIndent)
            indentStack.add(indent)
        }

        fun popIndent() {
            require(indentStack.size > 1)
            indentStack.removeLast()
        }
    }

    // このカスタムコンテキストをパーサで使用してインデントを検証できます：
    fun indent(context: IndentParseContext, start: Int): ParseResult<String>? {
        val expectedIndent = context.currentIndent
        // インデントを解析して検証...
        return null
    }
}
```

完全な実装については[online-parserサンプルのOnlineParser.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/samples/online-parser/src/jsMain/kotlin/io/github/mirrgieriana/xarpeg/samples/online/parser/OnlineParser.kt)と[OnlineParserParseContext.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/samples/online-parser/src/jsMain/kotlin/io/github/mirrgieriana/xarpeg/samples/online/parser/OnlineParserParseContext.kt)を参照してください。

## 重要なポイント

- **`parseAll().getOrThrow()`** 完全な消費を要求し、失敗時にスロー
- **エラーコンテキスト** `errorPosition`と`suggestedParsers`を提供
- **名前付きパーサ** 割り当てられた名前でエラーメッセージに表示
- **メモ化** デフォルトで有効；`useMemoization = false`で無効化
- **`map`での例外** 伝播して解析を中止
- **`parseOrNull`** `ParseContext`とともに詳細なデバッグを可能にする
- **`DefaultParseContext`は拡張可能** カスタム解析要件に対応

## 次のステップ

エラー報告とソースマッピングのための位置情報を抽出する方法を学びます。

→ **[ステップ5：解析位置](05-positions.html)**
