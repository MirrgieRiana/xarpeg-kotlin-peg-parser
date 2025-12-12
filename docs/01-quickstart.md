# Step 1: 最初のパーサーを作る

このステップでは、最小構成のパーサーを作り、入力をパースして値に変換するところまでを体験します。

## 最小サンプル

```kotlin
import mirrg.xarpite.parser.Parser
import mirrg.xarpite.parser.parseAllOrThrow
import mirrg.xarpite.parser.parsers.*

val identifier = +Regex("[a-zA-Z][a-zA-Z0-9_]*")
val number = +Regex("[0-9]+") map { it.value.toInt() }
val kv: Parser<Pair<String, Int>> =
    identifier * -'=' * number map { (key, value) -> key to value }

fun main() {
    println(kv.parseAllOrThrow("count=42")) // => (count, 42)
}
```

- `+literal` / `+Regex("...")` で現在位置にマッチするパーサーを作ります。
- `identifier` は「英字で始まり、英数字と `_` を含められる」識別子を例にしています。
- `*` はシーケンス結合で、結果は `Tuple` 系にまとめられます。
- `-parser` は値を捨ててマッチだけ行うので、デリミタを除外するのに便利です。
- `map` で戻り値を任意の型に整形します。

## 実行と動作確認

1. 上記コードを任意の Kotlin エントリーポイントに配置するか、スニペットとして実行します。
2. `parseAllOrThrow` は入力を最後まで消費できなかった場合に例外を投げるので、間違った入力もすぐに気付けます。
3. IDE の補完と KDoc を使うと、各コンビネータの型や戻り値を確認できます。

次のステップでは、合成パターンを増やして複雑な構文を組み立てていきます。  
→ [Step 2: パーサーを組み合わせる](02-combinators.md)
