/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/async/ThreadUtils.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Platform row: JVM and Native (a real thread yield). The JS row ships libGDX's own GWT emulation,
 *     where `yield` is a no-op (Scala.js has no `java.lang.Thread.yield`).
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 20
 * Covenant-baseline-methods: ThreadUtils,yield
 * Covenant-source-reference: com/badlogic/gdx/utils/async/ThreadUtils.java
 * Covenant-verified: 2026-09-22
 */
package sge.utils.async

/** Utilities for threaded programming. */
object ThreadUtils {
  def `yield`(): scala.Unit =
    java.lang.Thread.`yield`()
}
