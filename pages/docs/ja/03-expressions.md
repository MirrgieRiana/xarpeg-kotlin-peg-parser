---
layout: docs-ja
title: ステップ3 – 式と再帰
---

# ステップ3：式と再帰

再帰的な文法を構築し、式解析のための演算子の優先順位を処理します。

## 課題

`2*(3+4)`のような式を解析するには以下が必要です：
1. **再帰** - 式は他の式を含むことができる
2. **優先順位** - 乗算は加算より強く結合する
3. **結合性** - `2+3+4`は`(2+3)+4`としてグループ化されるべき

Xarpegは再帰のための`ref { }`と、演算子の優先順位のための`leftAssociative`/`rightAssociative`ヘルパーを提供します。

## 式パーサの例

以下は、再帰と優先順位を持つ完全な算術式パーサです：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val paren: Parser<Int> = -'(' * ref { root } * -')'
    val factor = number + paren
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    check(expr.parseAll("2*(3+4).getOrThrow()") == 14)
    check(expr.parseAll("5+3*2").getOrThrow() == 11)
}
```

## コードの理解

### `ref { }`による前方参照

`paren`パーサは、後で定義される`root`を参照する必要があります。前方参照を作成するには`ref { }`を使用します（例：`val paren: Parser<Int> = -'(' * ref { root } * -')'`）。

**重要：** `ref`を使用するプロパティには、型解決のために明示的な型宣言（`Parser<Int>`）が必要です。

### 演算子の優先順位

パーサを最高優先順位から最低優先順位まで階層化します：
- `val factor = number + paren` — 最高：数値と括弧
- `val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }` — 中間：乗算
- `val add = leftAssociative(mul, -'+') { a, _, b -> a + b }` — 最低：加算

各レベルは前のレベルの上に構築され、正しい優先順位を保証します：
- `5+3*2`は`5+(3*2)`として解析され、`(5+3)*2`ではありません
- `2*(3+4)`は`2*((3+4))`として解析されます

### 結合性ヘルパー

`leftAssociative(term, operator) { left, op, right -> result }`は左結合演算子チェーンを構築します。

これは明示的な再帰なしで`2+3+4+5`のような式を`((2+3)+4)+5`として処理します。

**パラメータ：**
- `term` - オペランド（数値、部分式など）のパーサ
- `operator` - 演算子のパーサ（通常は`-`を使用して無視）
- 結合関数は、左オペランド、演算子の結果、右オペランドを受け取ります

同様に、`rightAssociative`は右からグループ化します：`2^3^4` → `2^(3^4)`。

## `by lazy`を避ける

**再帰パーサに`by lazy`を使用しないでください**—無限再帰を引き起こします。`ref { }`メカニズムはすでに遅延評価を処理しています。

`by lazy`は、再帰とは無関係なまれな初期化エラーの最後の手段としてのみ使用してください。

## 複数の優先順位レベル

より多くの演算子のためにパターンを拡張します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val paren: Parser<Int> = -'(' * ref { root } * -')'

    val factor = number + paren
    val power = rightAssociative(factor, -'^') { a, _, b ->
        var result = 1
        repeat(b) { result *= a }
        result
    }
    val mulOp = (+'*' map { '*' }) + (+'/' map { '/' })
    val mul = leftAssociative(power, mulOp) { a, op, b ->
        if (op == '*') a * b else a / b
    }
    val addOp = (+'+' map { '+' }) + (+'-' map { '-' })
    val add = leftAssociative(mul, addOp) { a, op, b ->
        if (op == '+') a + b else a - b
    }
    val root = add
}.root

fun main() {
    check(expr.parseAll("2^3^2").getOrThrow() == 512)  // 右結合：2^(3^2)
    check(expr.parseAll("10-3-2").getOrThrow() == 5)   // 左結合：(10-3)-2
}
```

## 単項演算子

前処理で前置/後置演算子を処理します：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() } named "number"
    val paren: Parser<Int> = -'(' * ref { root } * -')'

    // 単項マイナス
    val unary: Parser<Int> = (-'-' * ref { unary } map { -it }) + number + paren

    val mul = leftAssociative(unary, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

fun main() {
    check(expr.parseAll("-5+3").getOrThrow() == -2)
    check(expr.parseAll("-(2+3).getOrThrow()") == -5)
}
```

## 重要なポイント

- **`ref { }`** 再帰的な文法のための前方参照を有効化
- **明示的な型** `ref`を使用するプロパティに必要
- **`leftAssociative` / `rightAssociative`** 演算子チェーンを処理
- **優先順位の階層化** 高から低へ（factor → multiply → add）
- **`by lazy`を決して使わない** 再帰パーサでは

## 次のステップ

パーサがエラー、キャッシング、デバッグ情報をどのように処理するかを学びます。

→ **[ステップ4：実行時の動作](04-runtime.html)**
