/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/src/com/badlogic/gdx/backends/gwt/emu/com/badlogic/gdx/utils/async/ThreadUtils.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Platform row: JS — libGDX's own emulation: the browser has one thread and nothing to yield to
 *     (Scala.js ships no `java.lang.Thread.yield`, the link fails on it).
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.utils.async

/** GWT emulation of [[ThreadUtils]]: single-threaded, `yield` does nothing. */
object ThreadUtils {
  def `yield`(): scala.Unit = ()
}
