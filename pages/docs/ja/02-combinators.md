---
layout: docs-ja
title: ステップ2 – コンビネータ
---

# ステップ2：コンビネータ

シーケンス、選択、繰り返しなどを使用してパーサを組み合わせ、複雑な文法を構築する方法を学びます。

## コアコンビネータ

### `+`による選択

順番に代替案を試します。最初のマッチが勝ちます：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val keyword = (+"if" + +"while" + +"for") named "keyword"

fun main() {
    keyword.parseAll("if").getOrThrow()      // ✓ "if"にマッチ
    keyword.parseAll("while").getOrThrow()   // ✓ "while"にマッチ
}
```

### オプショナル解析

`optional`はマッチを試みますが、失敗時には巻き戻します。`Tuple1<T?>`を返します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val sign = (+'+' map { '+' }) + (+'-' map { '-' })
val signOpt = sign.optional map { it.a ?: '+' }
val unsigned = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val signedInt = signOpt * unsigned map { (s, value) ->
    if (s == '-') -value else value
}

fun main() {
    check(signedInt.parseAll("-42").getOrThrow() == -42)
    check(signedInt.parseAll("99").getOrThrow() == 99)
}
```

オプショナル値にアクセスするには`it.a`を使用するか、`map { (value) -> ... }`で分解します。

#### オプショナルとタプルの組み合わせ

複数のオプショナルパーサを`*`で組み合わせる場合、タプルは自動的にフラット化され、nullable値を直接含むようになります：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val optA = (+'a').optional
val optB = (+'b').optional
val combined = optA * optB

fun main() {
    // 結果の型は Tuple2<Char?, Char?> （フラット化）
    // Tuple2<Tuple1<Char?>, Tuple1<Char?>> （ネスト）ではない
    val result1 = combined.parseAll("ab").getOrThrow()
    check(result1.a == 'a')  // nullable Charに直接アクセス
    check(result1.b == 'b')
    
    val result2 = combined.parseAll("a").getOrThrow()
    check(result2.a == 'a')
    check(result2.b == null)  // 欠落したoptionalはnull
}
```

このフラット化により、オプショナルの組み合わせがより使いやすくなります—ネストされたタプルではなく、nullable型を直接扱えます。

### 繰り返し

複数のマッチをリストに収集します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val digits = (+Regex("[0-9]") map { it.value } named "digit").oneOrMore map { matches -> 
    matches.joinToString("")
}

val letters = (+Regex("[a-z]") map { it.value } named "letter").zeroOrMore map { matches -> 
    matches
}

fun main() {
    digits.parseAll("123").getOrThrow()    // => "123"
    letters.parseAll("abc").getOrThrow()   // => ["a", "b", "c"]
    letters.parseAll("").getOrThrow()      // => []
}
```

- **`.zeroOrMore`** - 0回以上マッチ（失敗しない）
- **`.oneOrMore`** - 1回以上マッチ（マッチがない場合は失敗）
- **`.list(min, max)`** - `min`から`max`回の間マッチ

### シリアル解析

タプルの制限なしに、同じ型の複数の異なるパーサを順番に解析する必要がある場合は、`serial`を使用します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val article = +"the" + +"a"
val adjective = +"quick" + +"lazy"
val noun = +"fox" + +"dog"

val phrase = serial(article, +" ", adjective, +" ", noun)

fun main() {
    check(phrase.parseAll("the quick fox").getOrThrow() == listOf("the", " ", "quick", " ", "fox"))
    check(phrase.parseAll("a lazy dog").getOrThrow() == listOf("a", " ", "lazy", " ", "dog"))
}
```

`serial`は`List<T>`を返し、理論上の上限はありません。タプルパーサは16要素に制限されています。次の場合に使用します：
- 結合するパーサが多数ある場合（特にタプルの制限を超える場合）
- 選択可能な部分を持つ長い自然言語フレーズが必要な場合
- タプルではなくリスト結果が必要な場合

同じパーサを繰り返す場合は、代わりに`.list()`または`.oneOrMore`を使用してください。

## 結果の整形

`*`によるシーケンスはタプルを返します。不要な値をドロップするには`-parser`を使用します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

// ドロップなし：Tuple3<Char, MatchResult, Char>
val word = +Regex("[a-z]+") named "word"
val withDelimiters = +'(' * word * +')'

// ドロップあり：MatchResult（中央の値のみ）
val cleanResult = -'(' * word * -')' map { it.value }

fun main() {
    cleanResult.parseAll("(hello)").getOrThrow()  // => "hello"
}
```

`map`でタプルを分解して結果を変換します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val wordPart = +Regex("[a-z]+") named "word"
val numPart = +Regex("[0-9]+") named "number"
val pair = wordPart * -',' * numPart map { (word, num) ->
    word.value to num.value.toInt()
}

fun main() {
    pair.parseAll("hello,42").getOrThrow()  // => ("hello", 42)
}
```

## 入力境界

`startOfInput`と`endOfInput`は、入力を消費せずに位置境界でマッチします：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val word = +Regex("[a-z]+") map { it.value } named "word"

fun main() {
    // 入力の開始でマッチ
    val atStart = (startOfInput * word).parseAll("hello").getOrThrow()
    check(atStart == "hello")  // 成功
}
```

**注意：** `parseAll(...).getOrThrow()`を使用する場合、境界チェックは冗長です—入力全体が消費されることをすでに検証しています。これらのパーサは`parseOrNull`またはサブ文法内で使用してください。

## パーサへの名前付け

より良いエラーメッセージのために名前を割り当てます：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val digit = +Regex("[0-9]") named "digit"
val letter = +Regex("[a-z]") named "letter"
val identifier = (letter * (letter + digit).zeroOrMore) named "identifier"

fun main() {
    val result = identifier.parseAll("123abc")
    val exception = result.exceptionOrNull() as? ParseException
    
    check(exception != null)  // 解析失敗
    check(exception.message!!.contains("Syntax Error"))
}
```

### 名前付き複合パーサ

名前付き複合パーサは、構成要素パーサをエラー提案から隠します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

fun main() {
    val parserA = +'a' named "letter_a"
    val parserB = +'b' named "letter_b"
    
    // 名前付き複合：エラーには"ab_sequence"のみ
    val namedComposite = (parserA * parserB) named "ab_sequence"
    
    // 名前なし複合：エラーには"letter_a"
    val unnamedComposite = parserA * parserB
    
    val result1 = namedComposite.parseAll("c")
    val exception1 = result1.exceptionOrNull() as? ParseException
    val names1 = exception1?.context?.suggestedParsers?.mapNotNull { it.name } ?: emptyList()
    check(names1.contains("ab_sequence"))
    
    val result2 = unnamedComposite.parseAll("c")
    val exception2 = result2.exceptionOrNull() as? ParseException
    val names2 = exception2?.context?.suggestedParsers?.mapNotNull { it.name } ?: emptyList()
    check(names2.contains("letter_a"))
}
```

**ベストプラクティス：** 意味的なエラー（"Expected: identifier"）のために複合パーサに名前を付け、開発中の詳細なトークンレベルのエラーのためにコンポーネントは名前なしのままにします。

### 非表示パーサ

パーサを内部的に追跡する必要があるが、エラー提案に表示したくない場合があります。どこにでも出現可能な空白文字のようなパーサには`.hidden`を使用します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

fun main() {
    val whitespace = (+Regex("\\s+")).hidden
    val number = +Regex("[0-9]+") named "number" map { it.value.toInt() }
    val operator = (+'*' + +'+') named "operator"

    // 空白をオプションで受け入れるパーサ
    val expr = number * whitespace.optional * operator * whitespace.optional * number

    val result = expr.parseAll("42abc")  // 失敗：演算子または数値が必要

    val exception = result.exceptionOrNull() as? ParseException
    check(exception != null)

    val suggestions = exception.context.suggestedParsers?.mapNotNull { it.name?.ifEmpty { null } } ?: emptyList()
    // 意味のあるパーサを含むが、非表示の空白は含まない
    check(suggestions.contains("operator") || suggestions.contains("number"))
    check(!suggestions.contains(""))
}
```

`.hidden`は`named("")`と同等です - パーサ名を空文字列に設定し、内部的には追跡しつつエラー提案からは除外します。

**ユースケース：** どこにでも出現可能なパーサ（空白、コメント）に適用して、エラーメッセージを意味のあるトークンに集中させます。

## 重要なポイント

- **`+`** 代替案用（最初のマッチが勝つ）
- **`.optional`** 失敗時に巻き戻し、`Tuple1<T?>`を返す
- **`.zeroOrMore` / `.oneOrMore`** マッチをリストに収集
- **`-parser`** タプルから値をドロップ
- **分解** `map`でタプル結果を変換
- **`startOfInput` / `endOfInput`** 境界でマッチ
- **`named`** エラーメッセージを改善
- **`.hidden`** エラー提案からパーサを除外

## 次のステップ

再帰的な文法と演算子の優先順位を扱う方法を学びます。

→ **[ステップ3：式と再帰](03-expressions.html)**
