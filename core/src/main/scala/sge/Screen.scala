/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Screen.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait; dispose() -> close()
 *   Idiom: split packages
 *   TODO: opaque Pixels for resize(width, height) params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

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
  def render(delta: Float): Unit

  /** @see ApplicationListener#resize(int, int) */
  def resize(width: Int, height: Int): Unit

  /** @see ApplicationListener#pause() */
  def pause(): Unit

  /** @see ApplicationListener#resume() */
  def resume(): Unit

  /** Called when this screen is no longer the current screen for a {@link Game}. */
  def hide(): Unit

  /** Called when this screen should release all resources. */
  def close(): Unit
}
