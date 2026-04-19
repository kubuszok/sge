/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingAdapter.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: TypingAdapter,end,event,onChar,replaceVariable
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingAdapter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import sge.utils.Nullable

/** Simple listener for label events. You can derive from this and only override what you are interested in. */
class TypingAdapter extends TypingListener {

  override def event(event: String): Unit = {}

  override def end(): Unit = {}

  override def replaceVariable(variable: String): Nullable[String] = Nullable.empty

  override def onChar(ch: Long): Unit = {}
}
