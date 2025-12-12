# Step 2: パーサーを組み合わせる

ここでは DSL の基本コンビネータを使って、複数の要素を組み合わせたパーサーを作ります。

## よく使う演算子

- `parserA * parserB` … シーケンス結合。戻り値は `TupleX` にまとまります。
- `parserA + parserB` … 代替（順に試行）。左側が成功すれば右側は試しません。
- `-parser` … マッチはするが値を捨てます。デリミタやキーワード除去に最適です。
- `parser.optional` … 省略可能。失敗しても位置を巻き戻して後続に影響しません。
- `parser.zeroOrMore` / `oneOrMore` / `list` … 繰り返し系。戻り値は `List<T>`。

## オプションと繰り返しの組み合わせ

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

- `optional` は必ず巻き戻すので、後ろに来るパーサーの邪魔をしません。戻り値は `Tuple1` なので `it.a` で取り出すか、`map { (value) -> ... }` と分解できます。
- 繰り返し系の戻り値はすぐ `map` で加工できます。上の例では `joinToString` で文字列化しています。

## シーケンス結果の整形

`*` で組んだ結果は `TupleX` になるため、`map { (a, b, c) -> … }` のように分解して目的の型に整形します。  
デリミタや不要な値は `-parser` で落としておくと、`Tuple` の arity を抑えられます。

次は再帰や結合規則を扱い、式パーサーを少ないコードで構築します。  
→ [Step 3: 式と再帰を扱う](03-expressions.md)
