---
layout: docs-ja
title: ステップ6 – テンプレート文字列
---

# ステップ6：テンプレート文字列

PEGスタイルのパーサが、トークン化なしで埋め込み式を含むテンプレート文字列を自然に処理する方法を学びます。

## なぜトークナイザーが不要なのか？

字句解析器/トークナイザーフェーズを別に持つ従来のパーサは、`"hello $(1+2) world"`のようなテンプレート文字列で苦労します：

- **曖昧な境界** - `$`は文字列の一部か式の区切り文字か？
- **コンテキスト切り替え** - トークンルールはすべての可能なコンテキストを事前に処理する必要がある
- **ネストした構造** - 式内の文字列内の式は複雑な先読みが必要

文字単位で動作するPEGパーサは、複雑なトークンルールを設計することなく、自然にコンテキスト切り替えを処理します。

## 完全なテンプレート文字列パーサ

以下は、埋め込み算術式を持つテンプレート文字列のパーサです：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

sealed class TemplateElement
data class StringPart(val text: String) : TemplateElement()
data class ExpressionPart(val value: Int) : TemplateElement()

val templateStringParser: Parser<String> = object {
    // 式パーサ（優先順位付き算術）
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val grouped: Parser<Int> = -'(' * ref { sum } * -')'
    val factor: Parser<Int> = number + grouped
    val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
    val expression = sum

    // 文字列部分：$(と閉じ引用符"以外のすべてにマッチ
    val stringPart: Parser<TemplateElement> =
        +Regex("""[^"$]+|\$(?!\()""") map { match ->
            StringPart(match.value)
        } named "string_part"

    // 式部分：$(...)
    val expressionPart: Parser<TemplateElement> =
        -+"$(" * expression * -')' map { value ->
            ExpressionPart(value)
        }

    // テンプレート要素は文字列部分または式部分
    val templateElement = expressionPart + stringPart

    // 完全なテンプレート文字列：任意の数の要素を持つ"..."
    val templateString: Parser<String> =
        -'"' * templateElement.zeroOrMore * -'"' map { elements ->
            elements.joinToString("") { element ->
                when (element) {
                    is StringPart -> element.text
                    is ExpressionPart -> element.value.toString()
                }
            }
        }

    val root = templateString
}.root

fun main() {
    check(templateStringParser.parseAll(""""hello"""").getOrThrow() == "hello")
    check(templateStringParser.parseAll(""""result: $(1+2)"""").getOrThrow() == "result: 3")
    check(templateStringParser.parseAll(""""$(2*(3+4)) = answer"""").getOrThrow() == "14 = answer")
    check(templateStringParser.parseAll(""""a$(1)b$(2)c$(3)d"""").getOrThrow() == "a1b2c3d")
}
```

**注意：** Kotlinの文字列リテラルでは、`""""hello""""`は入力`"hello"`を表します。なぜなら、内側の引用符をエスケープする必要があるからです。

## 仕組み

### キー：スマートな正規表現境界

パターン`+Regex("""[^"$]+|\$(?!\()""")`は以下にマッチします：
- **`[^"$]+`** - `"`でも`$`でもない1つ以上の文字
- **`\$(?!\()`** - `(`が続かない`$`（否定先読み）

正規表現は明示的なトークン化なしで、テンプレート境界（`$(`）で自然に停止します。`$(`に遭遇すると、制御は`expressionPart`に渡され、これが再帰的に式パーサを呼び出します。

### コンテキスト切り替え

選択コンビネータ`val templateElement = expressionPart + stringPart`がコンテキスト切り替えを処理します。

最初に式の解析を試みます。それが失敗した場合（`$(`が見つからない場合）、文字列部分を解析します。これにより、必要に応じて自然にコンテキスト間を切り替えます。

### 再帰

`grouped`パーサは`ref { sum }`（例：`val grouped: Parser<Int> = -'(' * ref { sum } * -')'`）を使用して、括弧で囲まれた部分式を許可します。

これにより、`$(2*(3+4))`のようなネストした式が可能になります。

## ネストしたテンプレート文字列

式内の文字列を処理するためにパターンを拡張します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

sealed class TemplateElement
data class StringPart(val text: String) : TemplateElement()
data class ExpressionPart(val value: Int) : TemplateElement()

object TemplateWithNestedStrings {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val grouped: Parser<Int> = -'(' * ref { sum } * -')'

    val stringPart: Parser<TemplateElement> =
        +Regex("""[^"$]+|\$(?!\()""") map { match -> StringPart(match.value) } named "string_part"

    val expressionPart: Parser<TemplateElement> =
        -+"$(" * ref { sum } * -')' map { value ->
            ExpressionPart(value)
        }

    val templateElement = expressionPart + stringPart

    val templateString: Parser<String> = ref {
        -'"' * templateElement.zeroOrMore * -'"' map { elements ->
            elements.joinToString("") { element ->
                when (element) {
                    is StringPart -> element.text
                    is ExpressionPart -> element.value.toString()
                }
            }
        }
    }

    // 式にテンプレート文字列を含めることができるようになった
    val factor: Parser<Int> = number + grouped + (templateString map { it.length })
    val sum: Parser<Int> = leftAssociative(factor, -'+') { a, _, b -> a + b }
}

fun main() {
    TemplateWithNestedStrings.templateString.parseAll("\"nested $(1+2).getOrThrow()\"")
}
```

式パーサはテンプレート文字列パーサを再帰的に呼び出すことができ、その逆も可能です。この相互再帰はPEGで自然に機能します—事前トークン化の複雑さはありません。

## 利点のまとめ

**自然なコンテキスト切り替え：**
パーサは、事前に決定されたトークン境界ではなく、見たものに基づいて適応します。

**よりシンプルな文法：**
すべての可能なコンテキストを事前に処理しなければならない複雑なトークンルールがありません。

**再帰的な埋め込み：**
式は文字列を含むことができ、文字列は式を含むことができます—特別なケースなし。

**正規表現ベースの境界：**
否定先読みと文字クラスを使用して、自然な停止ポイントを定義します。

## さらなる拡張

このアプローチはより複雑なシナリオにもうまくスケールします：

**複数の区切り文字** - 複数の式部分パーサを作成し、それらを組み合わせることで、`$(...)`と`#{...}`の両方をサポート。

**エスケープシーケンス** - `\$(`をリテラルテキストとして`+Regex("""(?:[^"$\\]|\\.)+|\$(?!\()""")`のようなパターンを使用してマッチ。

**異なる引用符スタイル** - それぞれのスタイルに対してパーサを作成し、選択で組み合わせることで、単一引用符と二重引用符をサポート。

各追加は、トークンボキャブラリ全体の再設計ではなく、関連するパーサへの局所的な変更です。

## 重要なポイント

- **トークナイザーなし** - 文字から直接解析
- **正規表現境界** - 否定先読みを使用して停止ポイントを定義
- **選択コンビネータ** - 代替案間の自然なコンテキスト切り替え
- **再帰** - 文字列と式パーサ間の相互再帰
- **スケーラブル** - 新しい区切り文字やエスケープシーケンスを簡単に拡張

## おめでとうございます！

Xarpegチュートリアルを完了しました！以下の方法を習得しました：
- 演算子ベースのDSLでパーサを構築
- シーケンス、選択、繰り返しでパーサを組み合わせる
- 再帰と演算子の優先順位を処理
- エラー、キャッシング、デバッグを扱う
- 位置情報を抽出
- 複雑なネストした構造を解析

### 次のステップ

- **[例を探る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples)** - 完全なアプリケーションを学ぶ
- **[テストを読む](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonTest/kotlin)** - すべての機能が動作しているのを見る
- **[ソースを閲覧](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/importedMain/kotlin/io/github/mirrgieriana/xarpeg)** - 実装の詳細を理解
- **[何かを作る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser)** - 自分のパーサを作成！

→ **[チュートリアルインデックスに戻る](index.html)**
