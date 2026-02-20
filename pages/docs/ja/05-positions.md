---
layout: docs-ja
title: ステップ5 – 解析位置
---

# ステップ5：解析位置

エラー報告、ソースマッピング、デバッグのために解析結果から位置情報を抽出します。

## 解析結果と位置

すべての成功した解析は、以下を含む`ParseResult<T>`を返します：
- **`value: T`** - 解析された値
- **`start: Int`** - 入力の開始位置
- **`end: Int`** - 入力の終了位置

位置情報は常に利用可能ですが、通常は位置が必要になるまで`Parser<Int>`のような単純な型で作業します。

## `map`による単純な変換

`map`コンビネータは値だけで動作し、型をシンプルに保ちます：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"

fun main() {
    val result = number.parseAll("42").getOrThrow()
    check(result == 42)  // 値だけ、位置情報なし
}
```

## `mapEx`による位置へのアクセス

位置情報が必要な場合は`mapEx`を使用します。`DefaultParseContext`と完全な`ParseResult`を受け取ります：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") named "identifier"

val identifierWithPosition = identifier mapEx { ctx, result ->
    "${result.value.value}@${result.start}-${result.end}"
}

fun main() {
    val result = identifierWithPosition.parseAll("hello").getOrThrow()
    check(result == "hello@0-5")  // 位置情報を含む
}
```

**注意：** `+Regex(...)`は`Parser<MatchResult>`を返すため、`result.value.value`で文字列にアクセスします。

## 完全なParseResultの取得

完全な`ParseResult`オブジェクト（値、開始位置、終了位置を含む）が必要な場合は、`.result`拡張を使用します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val word = +"hello"
val wordWithResult = word.result

fun main() {
    val result = wordWithResult.parseAll("hello").getOrThrow()
    check(result.value == "hello")
    check(result.start == 0)
    check(result.end == 5)
}
```

`.result`拡張は`Parser<T>`を`Parser<ParseResult<T>>`に変換し、`mapEx`を使用せずにすべての位置情報に直接アクセスできるようにします。

## マッチしたテキストの抽出

`text()`拡張を使用して元のマッチした部分文字列を取得します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val number = +Regex("[0-9]+") named "number"

val numberWithText = number mapEx { ctx, result ->
    val matched = result.text(ctx)
    val value = matched.toInt()
    "Parsed '$matched' as $value"
}

fun main() {
    val result = numberWithText.parseAll("123").getOrThrow()
    check(result == "Parsed '123' as 123")  // マッチしたテキストが抽出される
}
```

## 行番号と列番号の計算

行/列情報を使用して強化されたエラー報告を構築します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

data class Located<T>(val value: T, val line: Int, val column: Int)

fun <T : Any> Parser<T>.withLocation(): Parser<Located<T>> = this mapEx { ctx, result ->
    val text = ctx.src.substring(0, result.start)
    val line = text.count { it == '\n' } + 1
    val column = text.length - (text.lastIndexOf('\n') + 1) + 1
    Located(result.value, line, column)
}

val keyword = +Regex("[a-z]+") map { it.value } named "keyword"
val keywordWithLocation = keyword.withLocation()

fun main() {
    val result = keywordWithLocation.parseAll("hello").getOrThrow()
    check(result.value == "hello" && result.line == 1 && result.column == 1)
}
```

## 複数行の位置追跡

複数行にわたる位置を追跡します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

data class Token(val value: String, val line: Int, val col: Int)

fun <T : Any> Parser<T>.withPos(): Parser<Token> = this mapEx { ctx, result ->
    val prefix = ctx.src.substring(0, result.start)
    val line = prefix.count { it == '\n' } + 1
    val col = prefix.length - (prefix.lastIndexOf('\n') + 1) + 1
    Token(result.text(ctx), line, col)
}

fun main() {
    val word = +Regex("[a-z]+") map { it.value } named "word"
    val wordWithPos = word.withPos()
    
    // 解析は入力内の位置を追跡
    val result = wordWithPos.parseAll("hello").getOrThrow()
    check(result == Token("hello", 1, 1))
}
```

## 実用的な例：エラーメッセージ

位置追跡をエラーコンテキストと組み合わせて、役立つメッセージを作成します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

fun main() {
    val parser = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    
    fun parseWithErrors(input: String): Result<Int> {
        val result = parser.parseAll(input)
        val exception = result.exceptionOrNull() as? ParseException
        
        return if (exception != null) {
            val pos = exception.context.errorPosition ?: 0
            val prefix = input.substring(0, pos)
            val line = prefix.count { it == '\n' } + 1
            val column = prefix.length - (prefix.lastIndexOf('\n') + 1) + 1
            val expected = exception.context.suggestedParsers.orEmpty().mapNotNull { it.name }
            
            Result.failure(Exception(
                "Syntax error at line $line, column $column. Expected: ${expected.joinToString()}"
            ))
        } else {
            result
        }
    }
    
    val result = parseWithErrors("abc")
    check(result.isFailure)  // 期待通り解析失敗
}
```

## ベストプラクティス

**デフォルトで`map`を使用** - 位置が不要な場合は型をシンプルに保つ（例：`val simple = +Regex("[0-9]+") map { it.value.toInt() } named "number"`）。

**必要な場合は`mapEx`を使用** - 必要な場所でのみ位置を抽出。

**位置ロジックを分離** - 位置追跡のために`fun <T : Any> Parser<T>.withLocation(): Parser<Located<T>>`のような再利用可能なヘルパーを作成。

**覚えておいてください：位置は常にそこにあります** - 文法全体でパーサの戻り値の型を変更する必要はありません。必要な境界で位置情報を抽出します。

## 重要なポイント

- **`ParseResult`** `value`、`start`、`end`を含む
- **`map`** 値を変換し、型をシンプルに保つ
- **`mapEx`** コンテキストと位置情報にアクセス
- **`.result`** すべての位置データに直接アクセスするための完全な`ParseResult<T>`を返す
- **`.text(ctx)`** マッチした部分文字列を抽出
- **行/列の計算** 改行のカウントが必要
- **位置ヘルパー** 文法コードをクリーンに保つ

## 次のステップ

PEGパーサが埋め込み式を使用してテンプレート文字列を自然に処理する方法を発見します。

→ **[ステップ6：テンプレート文字列](06-template-strings.html)**
