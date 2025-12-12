# ドキュメント（チュートリアル）

KDoc でシグネチャを追うだけでは伝わりづらい「DSL の組み立て方」と「使い所」を、チュートリアル形式でまとめます。API 詳細は IDE の補完や KDoc を参照してください。

## 1. 最初のパーサーを作る

このライブラリは **演算子ベースの DSL** でパーサーを合成します。よく使う演算子は次のとおりです。

- `+literal` / `+Regex("...")` : 先頭でリテラル・正規表現にマッチするパーサーを作る  
- `*` : シーケンス（順番にマッチ）し、結果は `TupleX` に詰められる  
- `-parser` : マッチはするが結果を捨てる（`Tuple0`）  
- `parserA + parserB` : 代替（どちらかにマッチ）  
- `parser.optional` / `zeroOrMore` / `oneOrMore` : 省略や繰り返し

最小限のキー・値パーサーは次のように書けます。

```kotlin
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*")
val number = +Regex("[0-9]+") map { it.value.toInt() }
val kv = identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    println(kv.parseAllOrThrow("count=42")) // => (count, 42)
}
```

`*` でシーケンス化すると `Tuple2` が返るので、`map` で欲しい形に変換するのが基本パターンです。先に読み捨てたいデリミタは `-` で包んでおくとタプルから消えます。

## 2. 繰り返し・オプションを組み合わせる

リストやオプションはビルダーを重ねるだけです。符号付き整数と連続文字列の例を見てみます。

```kotlin
val sign = (+'+' + +'-').optional map { it.a ?: '+' }
val unsigned = +Regex("[0-9]+") map { it.value.toInt() }
val signedInt = sign * unsigned map { (s, value) ->
    if (s == '-') -value else value
}

val repeatedA = (+'a').oneOrMore map { it.joinToString("") }

signedInt.parseAllOrThrow("-42") // => -42
signedInt.parseAllOrThrow("99")  // => 99
repeatedA.parseAllOrThrow("aaaa") // => "aaaa"
```

`optional` はマッチしなかった場合でも解析位置を戻してくれるため、後続のパーサーに影響を与えません。繰り返し系 (`zeroOrMore` / `oneOrMore` / `list`) は `List<T>` を返すので、`map` でそのまま好きな形に変換できます。

## 3. 再帰と結合規則で表現式を作る

再帰的な構造は `parser { ... }` か `by lazy` を使います。左結合・右結合のヘルパーを組み合わせると、再帰下降を手で書かずに済みます。

```kotlin
val expr: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { it.value.toInt() }
    val paren: Parser<Int> by lazy { -'(' * root * -')' }
    val factor = number + paren
    val mul = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val add = leftAssociative(mul, -'+') { a, _, b -> a + b }
    val root = add
}.root

expr.parseAllOrThrow("2*(3+4)") // => 14
```

`leftAssociative` / `rightAssociative` は「項」「演算子」「どう結合するか」の 3 つだけで連鎖演算子を組み立てます。演算子も通常のパーサーなので、空白を吸収する・複数文字演算子を扱う、といった拡張も簡単です。

## 4. エラーと全体消費

`parseAllOrThrow` は入力全体を消費できなければ例外を投げます。

- 先頭で何もマッチしない: `UnmatchedInputParseException`
- 一部だけマッチして末尾にゴミが残る: `ExtraCharactersParseException`

途中の `map` で例外を投げればその分岐は失敗として扱われるため、変換時のバリデーションも組み込みやすいです。

## 5. キャッシュの有効/無効

`ParseContext` は既定でメモ化するため、バックトラッキングが多い文法でも計算量が暴れにくくなります。メモリ節約や副作用の再実行が必要な場合は `parseAllOrThrow(..., useCache = false)` で無効化できます。

## 6. 次の一歩

- より細かい API の使い方や戻り値の形は IDE の KDoc からたどれます。  
- 実際の挙動を確認したい場合は `imported/src/commonTest/kotlin/ParserTest.kt` のテストケースが参考になります。  
- 実装の仕組みを読みたい場合は `imported/src/commonMain/kotlin/mirrg/xarpite/parser` 以下のシンプルな実装を覗いてみてください。
