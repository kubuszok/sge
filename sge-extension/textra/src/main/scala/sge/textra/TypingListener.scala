/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingListener.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: TypingListener,end,event,onChar,replaceVariable
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.utils.Nullable

/** Simple listener for label events. */
trait TypingListener {

  /** Called each time an `EVENT` token is processed. */
  def event(event: String): Unit

  /** Called when the char progression reaches the end. */
  def end(): Unit

  /** Called when variable tokens are replaced in text. Replacements returned by this method have priority over direct values, unless null is returned.
    */
  def replaceVariable(variable: String): Nullable[String]

  /** Called when a new character is displayed. */
  def onChar(ch: Long): Unit
}
