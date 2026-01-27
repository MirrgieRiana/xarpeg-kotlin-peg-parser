---
layout: docs-ja
title: Xarpegチュートリアル - パーサコンビネータを学ぶ
---

# Xarpegチュートリアル

Kotlinで強力なパーサを構築する方法を学びましょう。このチュートリアルでは、基本的な概念から高度なテクニックまで、段階的にガイドします。

## 前提条件

- Kotlinの基本的な知識（関数、ラムダ、クラス）
- 正規表現の知識（あると便利ですが必須ではありません）
- コード補完に対応したKotlin対応IDE

## インストール

`build.gradle.kts`にXarpegを追加します：

### Kotlin Multiplatformプロジェクト

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-kotlinMultiplatform:<latest-version>")
}
```

### JVM専用プロジェクト

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-jvm:<latest-version>")
}
```

### JS専用プロジェクト

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mirrgieriana:xarpeg-js:<latest-version>")
}
```

### その他のプラットフォーム

その他のプラットフォーム固有のアーティファクト（Nativeターゲット、WASM）については、以下を参照してください：
**[Maven Centralリポジトリ](https://repo1.maven.org/maven2/io/github/mirrgieriana/)**

最新バージョンは[Releases](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/releases)で確認できます。

---

## チュートリアルステップ

### 1. [クイックスタート](01-quickstart.html)
数分で最初のパーサを構築します。基本的な構文を学び、シンプルなキーバリューパーサを実行します。

**学習内容：** リテラルと正規表現からのパーサ作成、`*`による連結、`-`によるトークンの無視、`map`による結果の変換

---

### 2. [コンビネータ](02-combinators.html)
ビルディングブロックをマスターします：シーケンス、選択、繰り返し、オプショナル解析。

**学習内容：** `+`による選択、繰り返し（`.zeroOrMore`、`.oneOrMore`）、オプショナル、入力境界、より良いエラーメッセージのためのパーサへの名前付け

---

### 3. [式と再帰](03-expressions.html)
再帰的な文法と式解析のための演算子優先順位を扱います。

**学習内容：** `ref { }`による前方参照、左/右結合性、算術パーサの構築、適切な型宣言

---

### 4. [実行時の動作](04-runtime.html)
パーサがエラー、入力消費、キャッシングをどのように処理するかを理解します。

**学習内容：** 例外の種類、`ParseContext`によるエラー追跡、メモ化制御、デバッグ技法

---

### 5. [解析位置](05-positions.html)
より良いエラーメッセージとソースマッピングのための位置情報を抽出します。

**学習内容：** `mapEx`による位置追跡、行/列番号の計算、マッチしたテキストの抽出

---

### 6. [テンプレート文字列](06-template-strings.html)
トークン化なしで複雑なネストした構造を解析します。

**学習内容：** 埋め込み式の処理、PEGによるコンテキスト切り替え、再帰的な文字列/式解析

---

## 完全な例

### JSONパーサ
エスケープシーケンス、ネストした構造、包括的なテストを含む、すべてのJSON型を処理する完全な実装。

→ **[JSONパーサソースを見る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/JsonParserTest.kt)**

機能：
- 文字列エスケープシーケンス（`\"`、`\\`、`\n`、`\uXXXX`）
- 数値（整数、小数、科学記法）
- `ref { }`による再帰的な配列とオブジェクト
- カスタム区切り文字処理

### 算術インタプリタ
行/列位置を含むエラー報告を備えた、評価機能付き式パーサ。

→ **[インタプリタソースを見る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples/interpreter)**

機能：
- 優先順位を持つ4つの算術演算
- グループ化のための括弧
- 位置情報付きゼロ除算エラー報告
- コマンドラインインターフェース

### オンラインパーサデモ
リアルタイムの解析と評価を示すインタラクティブなブラウザベースのパーサ。

→ **[ライブデモを試す](https://mirrgieriana.github.io/xarpeg-kotlin-peg-parser/online-parser/)** | **[ソースを見る](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/samples/online-parser)**

---

## 追加リソース

### APIドキュメント
- **IDE内のKDoc** - インラインドキュメントのためにコード補完を使用
- **[Parser.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/Parser.kt)** - コアインターフェースとヘルパー
- **[parsersパッケージ](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/tree/main/src/commonMain/kotlin/io/github/mirrgieriana/xarpite/xarpeg/parsers)** - コンビネータの実装

### テスト
- **[ParserTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ParserTest.kt)** - 包括的な動作例
- **[ErrorContextTest.kt](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/src/commonTest/kotlin/io/github/mirrgieriana/xarpite/xarpeg/ErrorContextTest.kt)** - エラー追跡の例

### 実際の使用例
- **[Xarpite](https://github.com/MirrgieRiana/Xarpite)** - 複雑な文法解析にXarpegを使用している本番アプリケーション

---

## ヘルプが必要ですか？

- **[GitHub Issues](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/issues)** - バグ報告や機能リクエスト
- **[メインREADME](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser)** - クイックリファレンスと概要
- **[コントリビューティングガイド](https://github.com/MirrgieRiana/xarpeg-kotlin-peg-parser/blob/main/CONTRIBUTING.md)** - 開発環境のセットアップ
