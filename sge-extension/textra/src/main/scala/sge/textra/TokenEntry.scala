/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TokenEntry.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 26
 * Covenant-baseline-methods: TokenEntry,category,compareTo,effect,endIndex,floatValue,index,stringValue,token
 * Covenant-source-reference: com/github/tommyettinger/textra/TokenEntry.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import sge.utils.Nullable

/** Container representing a token, parsed parameters and its position in text. */
class TokenEntry(
  var token:       String,
  var category:    TokenCategory,
  var index:       Int,
  var endIndex:    Int,
  var floatValue:  Float,
  var stringValue: Nullable[String]
) extends Comparable[TokenEntry] {

  var effect: Nullable[Effect] = Nullable.empty

  override def compareTo(o: TokenEntry): Int = Integer.compare(o.index, index)
}
