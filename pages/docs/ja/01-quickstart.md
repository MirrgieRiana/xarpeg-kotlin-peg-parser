---
layout: docs-ja
title: ステップ1 – クイックスタート
---

# ステップ1：クイックスタート

数分で最初のパーサを構築します。このガイドでは、コアDSLの概念を示す最小限の例を説明します。

## 最初のパーサ

`count=42`のようなパターンにマッチするシンプルなキーバリューパーサを構築してみましょう：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    val result = kv.parseAll("count=42").getOrThrow()
    check(result == ("count" to 42))  // 結果が(count, 42)であることを確認
}
```

## コードの理解

**単項`+`演算子**は、リテラルや正規表現パターンをパーサに変換します：
- `+Regex("[a-zA-Z][a-zA-Z0-9_]*")`は、文字で始まる識別子にマッチします
- `+Regex("[0-9]+")`は、1つ以上の数字にマッチします

**`*`演算子**は、パーサを順番に連結します（例：`identifier * -'=' * number`）。結果は型付きタプルにパッケージされます。

**単項`-`演算子**は、パーサにマッチしますが、その値を結果のタプルから除外します（例：`-'='`は`=`文字をドロップします）。

**`map`関数**は、解析された値を変換します：
- `map { it.value }`はMatchResultから文字列を抽出します
- `map { it.value.toInt() }`は文字列を整数に変換します
- `map { (key, value) -> ... }`はタプルを分解して変換します

**`named`関数**は、より良いエラーメッセージのためにパーサに名前を割り当てます（例：`named "identifier"`）。

## パーサの実行

`parseAll(...).getOrThrow()`は、入力全体が消費されることを要求します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*") map { it.value } named "identifier"
val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    // 成功ケース
    check(kv.parseAll("count=42").getOrThrow() == ("count" to 42))  // ✓
    check(kv.parseAll("x=100").getOrThrow() == ("x" to 100))        // ✓

    // エラーケースは例外をスローします：
    // kv.parseAll("=42").getOrThrow()        // ✗ ParseException
    // kv.parseAll("count").getOrThrow()      // ✗ ParseException
    // kv.parseAll("count=42x").getOrThrow()  // ✗ ParseException
}
```

**例外の種類：**
- `ParseException` パーサがマッチしなかった場合、または末尾の入力が残っている場合

## 重要なポイント

- **単項`+`** リテラル、文字、または正規表現からパーサを作成
- **二項`*`** パーサを連結してタプルを生成
- **単項`-`** 値にマッチするが結果からドロップ
- **`map`** 解析された値をドメイン型に変換
- **`named`** エラーメッセージを改善
- **`parseAll(...).getOrThrow()`** 完全な入力を解析するか、例外をスロー

## ベストプラクティス

パーサの型を選択する際は、最適なパフォーマンスと明確性のために以下のガイドラインに従ってください：

**単一文字にはCharトークンを使用：**
- 良い：`+'x'` - 効率的な文字マッチング
- 悪い：`+"x"` - 不要な文字列オーバーヘッド
- 悪い：`+Regex("x")` - 固定文字に対する正規表現のオーバーヘッド

**固定文字列にはStringトークンを使用：**
- 良い：`+"xyz"` - 効率的な文字列マッチング
- 悪い：`+Regex("xyz")` - 固定文字列に対する正規表現のオーバーヘッド

**パターンには`named`を付けたRegexトークンを使用：**
- 良い：`+Regex("[0-9]+") named "number"` - 名前付き正規表現は明確なエラーメッセージを提供
- 悪い：`+Regex("[0-9]+")` - 名前なし正規表現は不十分なエラーメッセージを提供

**まとめ：**
- 単一文字 → `+'x'`を使用、`+"x"`や`+Regex("x")`は使わない
- 固定文字列 → `+"xyz"`を使用、`+Regex("xyz")`は使わない
- パターン/可変コンテンツ → `+Regex("...") named "name"`を使用

## 次のステップ

基本を理解したので、より洗練された方法でパーサを組み合わせる方法を学びましょう。

→ **[ステップ2：コンビネータ](02-combinators.html)**
