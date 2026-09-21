/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/src/com/badlogic/gdx/backends/gwt/emu/com/badlogic/gdx/utils/async/AsyncResult.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Platform row: JS — libGDX's own emulation: the task ran inside `submit`, so the result is
 *     complete on construction.
 *   Renames: isDone -> done (parenless getter, as the port spells it)
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.utils.async

/** GWT emulation of [[AsyncResult]]: returned by [[AsyncExecutor.submit]], already complete. */
class AsyncResult[T <: java.lang.Object] private[async] (private val result: T) {

  /** @return whether the [[AsyncTask]] is done — always, on this row */
  def done: scala.Boolean = true

  /** @return the result the task computed inside `submit` */
  def get(): T = this.result
}
