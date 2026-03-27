---
layout: docs-ja
title: ステップ7 – 識別子デリミタ文字列
---

# ステップ7：識別子デリミタ文字列

解析中に得た識別子をデリミタとして使い、対応する閉じデリミタまでの文字列を解析する方法を学びます。

## デリミタが識別子である文字列とは？

多くの言語で、文字列リテラルのデリミタに識別子が使われます：

- **XML要素** - `<tag>content</tag>`：開始タグの名前が閉じタグを決定する
- **ヒアドキュメント** - `<<EOF ... EOF`：開始時に指定した識別子が終端を決定する

これらの構造の共通点は、**解析中に得た値が後続の解析を決定する**ことです。固定の区切り文字（`"`や`'`）と異なり、デリミタ自体が入力の一部として動的に決まります。

## XML要素パーサ

以下は、開始タグと閉じタグが一致するXMLスタイルの要素を解析するパーサです：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

data class XmlElement(val tag: String, val content: String)

val xmlElementParser: Parser<XmlElement> = run {
    val tagName = +Regex("[a-zA-Z][a-zA-Z0-9]*") map { it.value }
    val openTag = -'<' * tagName * -'>'

    Parser { context, start ->
        val openResult = context.parseOrNull(openTag, start) ?: return@Parser null
        val name = openResult.value

        val closingTag = +"</${name}>"

        var pos = openResult.end
        while (pos <= context.src.length) {
            val closeResult = context.parseOrNull(closingTag, pos)
            if (closeResult != null) {
                val content = context.src.substring(openResult.end, pos)
                return@Parser ParseResult(XmlElement(name, content), start, closeResult.end)
            }
            if (pos >= context.src.length) break
            pos++
        }
        null
    }
}

fun main() {
    check(xmlElementParser.parseAll("<hello>world</hello>").getOrThrow() == XmlElement("hello", "world"))
    check(xmlElementParser.parseAll("<div>content here</div>").getOrThrow() == XmlElement("div", "content here"))
    check(xmlElementParser.parseAll("<x></x>").getOrThrow() == XmlElement("x", ""))
}
```

## 仕組み

### キー：`Parser { }` コンストラクタによる動的パーサ生成

通常のコンビネータ（`*`、`+`、`map`など）は静的なパーサを組み合わせます。しかし、識別子デリミタ文字列では、解析した結果に基づいて閉じデリミタを**動的に**決定する必要があります。

`Parser { context, start -> ... }` コンストラクタを使うと、解析ロジックを直接記述できます。この中で：

1. 既存のパーサを `context.parseOrNull(parser, position)` で呼び出す
2. 解析結果に基づいて新しいパーサを動的に構築する
3. 入力を1文字ずつ進めて閉じデリミタを探す

XML例では、`context.parseOrNull(openTag, start)` が事前定義された静的パーサで開始タグを解析します。その結果 `name` を使って `+"</${name}>"` という、特定の閉じタグにマッチする新しいパーサを構築します。

### 静的パーサと動的パーサの組み合わせ

`run { }` ブロック内で、静的なパーサ（`openTag`）を事前に定義し、動的なパーサ（`closingTag`）を`Parser { }` 内で構築します。静的パーサはメモ化の恩恵を受けますが、動的パーサは毎回生成されます。

### コンテンツの走査

閉じデリミタを探すために、`pos`を1文字ずつ進めながら `context.parseOrNull(closingTag, pos)` を試みます。閉じデリミタがマッチしたら、開始タグの直後から閉じデリミタの直前までをコンテンツとして抽出します。

## ヒアドキュメントパーサ

同じテクニックで、ヒアドキュメントも解析できます：

```kotlin
import io.github.mirrgieriana.xarpeg.*
import io.github.mirrgieriana.xarpeg.parsers.*

data class Heredoc(val delimiter: String, val content: String)

val heredocParser: Parser<Heredoc> = run {
    val identifier = +Regex("[A-Za-z_][A-Za-z0-9_]*") map { it.value }
    val header = -"<<" * identifier * -'\n'

    Parser { context, start ->
        val headerResult = context.parseOrNull(header, start) ?: return@Parser null
        val delimiter = headerResult.value

        val endMarker = "\n${delimiter}"
        var pos = headerResult.end
        while (pos + endMarker.length <= context.src.length) {
            if (context.src.startsWith(endMarker, pos)) {
                val content = context.src.substring(headerResult.end, pos)
                return@Parser ParseResult(Heredoc(delimiter, content), start, pos + endMarker.length)
            }
            pos++
        }
        null
    }
}

fun main() {
    check(heredocParser.parseAll("<<EOF\nhello world\nEOF").getOrThrow() == Heredoc("EOF", "hello world"))
    check(heredocParser.parseAll("<<END\nline 1\nline 2\nEND").getOrThrow() == Heredoc("END", "line 1\nline 2"))
    check(heredocParser.parseAll("<<X\n\nX").getOrThrow() == Heredoc("X", ""))
}
```

ヒアドキュメントは `<<IDENTIFIER` で始まり、改行の後に本文が続きます。行頭に同じ識別子が現れた時点で終了します。

### XML要素パーサとの違い

ヒアドキュメントでは、閉じデリミタの検索に `context.src.startsWith(endMarker, pos)` を直接使っています。これは、閉じデリミタのパターンが単純（改行＋識別子）であるため、パーサを毎回構築するよりも効率的です。

どちらのアプローチを選ぶかは状況次第です：

- **パーサを構築する方法**（XML例）- 閉じデリミタが複雑な場合に有用
- **文字列操作を直接使う方法**（ヒアドキュメント例）- 閉じデリミタが単純な場合に効率的

## 利点のまとめ

**動的な文法：**
パーサが解析中に文法を決定するため、コンテキストに応じた柔軟な解析が可能です。

**コンビネータとの統合：**
`Parser { }` コンストラクタの中で既存のコンビネータを自由に使えるため、静的なパーサと動的なパーサをシームレスに組み合わせられます。

**パラメトリックな解析：**
解析した値をパラメータとして後続の解析に渡すことで、入力に応じた適応的な解析が実現できます。

## 重要なポイント

- **`Parser { context, start -> }` コンストラクタ** - カスタム解析ロジックを直接記述
- **動的パーサ生成** - 解析した値から閉じデリミタのパーサを構築
- **`context.parseOrNull(parser, pos)`** - 他のパーサを内部から呼び出し
- **走査パターン** - 1文字ずつ進めて閉じデリミタを検索
- **静的と動的の使い分け** - 固定部分はコンビネータ、動的部分は `Parser { }` で記述

## おめでとうございます！

Xarpegチュートリアルを完了しました！以下の方法を習得しました：
- 演算子ベースのDSLでパーサを構築
- シーケンス、選択、繰り返しでパーサを組み合わせる
- 再帰と演算子の優先順位を処理
- エラー、キャッシング、デバッグを扱う
- 位置情報を抽出
- 複雑なネストした構造を解析
- 識別子に基づく動的なデリミタを処理

### 次のステップ

- **[例を探る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples)** - 完全なアプリケーションを学ぶ
- **[テストを読む](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonTest/kotlin)** - すべての機能が動作しているのを見る
- **[ソースを閲覧](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/importedMain/kotlin/io/github/mirrgieriana/xarpeg)** - 実装の詳細を理解
- **[何かを作る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser)** - 自分のパーサを作成！

→ **[チュートリアルインデックスに戻る](index.html)**
