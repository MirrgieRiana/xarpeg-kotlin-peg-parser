# `by lazy` 削除による影響の調査結果
# Investigation Results: Removing `by lazy` from Parser Definitions

## 概要 / Overview

文法定義コードから `val parser = by lazy { ... }` を削除し、通常の代入 `val parser = ...` に変更した場合の影響を調査しました。

This document investigates what happens when `val parser = by lazy { ... }` is replaced with normal assignment `val parser = ...` in grammar definition code.

## 実施内容

以下のファイルのすべてのパーサーフィールド定義を `by lazy` から通常の代入に書き換えました：

1. `samples/minimal-jvm-sample/src/main/kotlin/io/github/mirrgieriana/xarpite/xarpeg/samples/java_run/Main.kt`
2. `samples/interpreter/src/main/kotlin/io/github/mirrgieriana/xarpite/xarpeg/samples/interpreter/Main.kt`
3. `samples/online-parser/src/jsMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/samples/online/parser/OnlineParser.kt`
4. `src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/JsonParserTest.kt`
5. `src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/LazyArithmeticTest.kt`
6. `src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/TemplateStringTutorialTest.kt`
7. `src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserAdditionalTest.kt`
8. `imported/src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt`
9. `imported/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers/DelegationParser.kt`

## 発生したエラー

### 1. コンパイルエラー: 変数の初期化順序の問題

**影響を受けたファイル**: `samples/interpreter/src/main/kotlin/io/github/mirrgieriana/xarpite/xarpeg/samples/interpreter/Main.kt`

**エラー内容**:
```
e: file:///.../Main.kt:42:35 Variable 'sum' must be initialized.
```

**エラーの原因**:
```kotlin
private object ArithmeticParser {
    // ...
    val expr: Parser<LazyValue> = sum  // ← エラー: sumはまだ定義されていない
    // ...
    val sum: Parser<LazyValue> = leftAssociative(product, +'+' + +'-') { ... }  // ← ここで定義
}
```

このケースでは、`expr` が `sum` を参照していますが、`sum` は後で定義されているため、前方参照（forward reference）の問題が発生します。Kotlinでは、同じスコープ内で後に定義される変数を参照することはできません。

### 2. 実行時エラー: NullPointerException

**影響を受けたファイル**: テストファイル全般

**テスト実行結果**:
```
133 tests completed, 16 failed
```

**失敗したテスト**:
- `JsonParserTest`: 8件失敗
  - `testJsonSimpleObject`
  - `testJsonWithWhitespace`
  - `testJsonArrayWithNumbers`
  - `testJsonArrayMixed`
  - `testJsonEmptyArray`
  - `testJsonNestedObject`
  - `testComplexJsonStructure`
  - `testJsonNestedArray`
- `LazyArithmeticTest`: 3件失敗
  - `positionMarkerWithComplexExpression`
  - `positionMarkerInsideParentheses`
  - `positionMarkerDeepNesting`
- `ParserAdditionalTest`: 1件失敗
  - `delegationParserAllowsMutualRecursion`
- `ParserTest`: 2件失敗
  - `cache`
  - `delegationParser`
- `TemplateStringTutorialTest`: 2件失敗
  - `complexExpression`
  - `expressionAtStart`

**エラーの原因**:
すべてのテストで `java.lang.NullPointerException` が発生しました。これは、再帰的なパーサー定義において、初期化中に未初期化の変数にアクセスしようとしたために発生します。

例えば、`DelegationParser` の場合:
```kotlin
class DelegationParser<out T : Any>(val parserGetter: () -> Parser<T>) : Parser<T> {
    private val parser = parserGetter()  // ← parserGetterが未初期化の変数を参照する可能性がある
    override fun parseOrNull(context: ParseContext, start: Int): ParseResult<T>? {
        return context.parseOrNull(parser, start)
    }
}
```

`by lazy` を使用していた場合、`parserGetter()` は最初にアクセスされるまで実行されませんが、通常の代入では即座に実行されるため、循環参照や未初期化の変数にアクセスすることになります。

### 3. 正常に動作したケース

**成功したファイル**:
- `samples/minimal-jvm-sample/src/main/kotlin/io/github/mirrgieriana/xarpite/xarpeg/samples/java_run/Main.kt`
- `samples/online-parser/src/jsMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/samples/online/parser/OnlineParser.kt`

**実行結果**:
```
> Task :run
2*(3+4)+5 = 19

BUILD SUCCESSFUL
```

**成功の理由**:
これらのファイルでは、パーサーの定義順序が適切であるため、前方参照の問題が発生しませんでした。具体的には：

1. `minimal-jvm-sample` の場合:
```kotlin
private val expression: Parser<Int> = object {
    val number = +Regex("[0-9]+") map { match -> match.value.toInt() }
    val grouped: Parser<Int> = -'(' * parser { sum } * -')'  // parser { } を使用
    val factor: Parser<Int> = number + grouped
    val product = leftAssociative(factor, -'*') { a, _, b -> a * b }
    val sum: Parser<Int> = leftAssociative(product, -'+') { a, _, b -> a + b }
}.sum
```

ここでは `parser { sum }` という形式を使用しており、`parser()` 関数（`DelegationParser` のファクトリ関数）がラムダ式を受け取るため、`sum` へのアクセスは遅延されます。

2. `online-parser` の場合も同様に、すべての参照が `parser { }` を通じて行われているため、遅延評価が保たれています。

## 結論

### `by lazy` が必要な理由

1. **前方参照の解決**: 再帰的な文法定義では、パーサーが相互に参照し合うことが一般的です。`by lazy` を使用することで、定義順序に関係なく相互参照が可能になります。

2. **循環参照の回避**: `DelegationParser` のような仕組みと組み合わせることで、初期化時の循環参照を回避できます。

3. **初期化の遅延**: `by lazy` により、パーサーの実際の初期化をオブジェクト全体の初期化後に遅延させることができます。

### `by lazy` なしでも動作する条件

1. **適切な定義順序**: パーサーを依存関係に従って順番に定義する
2. **`parser { }` の使用**: 再帰的な参照には必ず `parser { }` を使用して遅延評価を行う

ただし、これらの条件を満たすのは複雑な文法では困難であり、`by lazy` を使用する方が安全で保守性が高いと言えます。

## エラーの種類まとめ

| エラータイプ | 発生タイミング | 影響範囲 | 対処方法 |
|------------|-------------|---------|---------|
| Variable must be initialized | コンパイル時 | 前方参照を含むコード | 定義順序の調整または`by lazy`の使用 |
| NullPointerException | 実行時 | 相互参照を含むパーサー定義 | `by lazy`の使用 |
| 動作成功 | - | `parser { }`を適切に使用しているコード | そのまま使用可能 |

## 推奨事項

再帰的または相互参照を含むパーサー定義では、`by lazy` の使用を継続することを強く推奨します。これにより：

- コンパイルエラーを回避
- 実行時エラーを防止
- コードの保守性を向上
- 定義順序を気にする必要がなくなる

という利点があります。

## 更新: `parser { }` を使用した解決方法

### 調査の続き

`DelegationParser` 内の `by lazy` を維持しつつ、パーサーフィールド定義から `by lazy` を削除し、前方参照や再帰参照には `parser { }` を使用する方法を検証しました。

### 変更内容

1. **`DelegationParser` の `by lazy` を維持**: `DelegationParser` クラス内の `by lazy` は維持し、遅延評価を保持
2. **パーサーフィールド定義から `by lazy` を削除**: すべてのパーサーフィールド定義から `by lazy` を削除
3. **前方参照に `parser { }` を使用**: 後で定義されるパーサーや自己参照には `parser { }` でラップ

### 修正例

**修正前**:
```kotlin
val expr: Parser<LazyValue> = sum  // ← コンパイルエラー
```

**修正後**:
```kotlin
val expr: Parser<LazyValue> = parser { sum }  // ← parser { } でラップすることで遅延評価
```

### 結果

**すべてのテストが成功**:
- コンパイルエラーなし
- 実行時エラーなし
- すべてのサンプルプログラムが正常に動作

### 結論

理論通り、`DelegationParser` 内の `by lazy` と、前方参照・再帰参照での `parser { }` の組み合わせで十分機能します。パーサーフィールド定義自体に `by lazy` を使用する必要はありません。

**重要なポイント**:
- `DelegationParser` の `by lazy` は必須（遅延評価のため）
- パーサーフィールドの `by lazy` は不要（`parser { }` で代替可能）
- 前方参照・自己参照には必ず `parser { }` を使用する
