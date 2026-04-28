/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Screen.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait; dispose() -> close()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: Screen,close,hide,pause,render,resize,resume,show
 * Covenant-source-reference: com/badlogic/gdx/Screen.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d862041b45cdf5cf90ef370035867b25bea2af89
 */
package sge

import sge.utils.Seconds

/** <p> Represents one of many application screens, such as a main menu, a settings menu, the game screen and so on. </p> <p> Note that {@link #close()} is not called automatically. </p>
  * @see
  *   Game
  */
trait Screen extends AutoCloseable {

  /** Called when this screen becomes the current screen for a {@link Game}. */
  def show(): Unit

  /** Called when the screen should render itself.
    * @param delta
    *   The time in seconds since the last render.
    */
  def render(delta: Seconds): Unit

  /** @see ApplicationListener#resize(int, int) */
  def resize(width: Pixels, height: Pixels): Unit

  /** @see ApplicationListener#pause() */
  def pause(): Unit

  /** @see ApplicationListener#resume() */
  def resume(): Unit

  /** Called when this screen is no longer the current screen for a {@link Game}. */
  def hide(): Unit

  /** Called when this screen should release all resources. */
  def close(): Unit
}
