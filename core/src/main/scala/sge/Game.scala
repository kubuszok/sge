/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Game.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (getScreen/setScreen)
 *   Audited: 2026-03-03
 */
package sge

import sge.utils.Nullable

/** <p> An {@link ApplicationListener} that delegates to a {@link Screen} . This allows an application to easily have multiple screens. </p> <p> Screens are not disposed automatically. You must handle
  * whether you want to keep screens around or dispose of them when another screen is set. </p>
  */
abstract class Game extends ApplicationListener {
  protected var screen: Nullable[Screen] = Nullable.empty

  override def dispose(): Unit =
    screen.foreach(_.hide())

  override def pause(): Unit =
    screen.foreach(_.pause())

  override def resume(): Unit =
    screen.foreach(_.resume())

  override def render(): Unit =
    // TODO: Need to get delta time from somewhere - placeholder for now
    screen.foreach(_.render(0.016f)) // 60 FPS default

  override def resize(width: Int, height: Int): Unit =
    screen.foreach(_.resize(width, height))

  /** Sets the current screen. {@link Screen#hide()} is called on any old screen, and {@link Screen#show()} is called on the new screen, if any.
    * @param screen
    *   may be {@code null}
    */
  def setScreen(newScreen: Nullable[Screen]): Unit = {
    screen.foreach(_.hide())
    this.screen = newScreen
    screen.foreach { s =>
      s.show()
      // TODO: Need to get screen dimensions from somewhere - using placeholder resize call
      s.resize(800, 600) // Default size placeholder
    }
  }

  /** @return the currently active {@link Screen}. */
  def getScreen(): Nullable[Screen] =
    screen
}
