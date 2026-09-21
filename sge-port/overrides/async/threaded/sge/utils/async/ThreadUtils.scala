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
 */
package sge.utils.async

/** Utilities for threaded programming. */
object ThreadUtils {
  def `yield`(): scala.Unit =
    java.lang.Thread.`yield`()
}
