package io.github.mirrgieriana.xarpeg

/**
 * 現在のプラットフォームがKotlin/Nativeかどうかを示します。
 *
 * 異なるプラットフォームに対してパーサーのキャッシュ戦略を最適化するために内部的に使用されます。
 */
expect val isNative: Boolean
