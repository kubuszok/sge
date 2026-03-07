/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Game.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: (using Sge) on class; Nullable screen field
 *   Idiom: getScreen/setScreen → def screen/def screen_= (Scala property)
 *   Audited: 2026-03-04
 */
package sge

import sge.utils.Nullable

/** <p> An {@link ApplicationListener} that delegates to a {@link Screen} . This allows an application to easily have multiple screens. </p> <p> Screens are not disposed automatically. You must handle
  * whether you want to keep screens around or dispose of them when another screen is set. </p>
  */
abstract class Game()(using Sge) extends ApplicationListener {
  private var _screen: Nullable[Screen] = Nullable.empty

  /** @return the currently active {@link Screen}. */
  def screen: Nullable[Screen] = _screen

  /** Sets the current screen. {@link Screen#hide()} is called on any old screen, and {@link Screen#show()} is called on the new screen, if any.
    * @param newScreen
    *   may be {@code null}
    */
  def screen_=(newScreen: Nullable[Screen]): Unit = {
    _screen.foreach(_.hide())
    _screen = newScreen
    _screen.foreach { s =>
      s.show()
      s.resize(Sge().graphics.getWidth(), Sge().graphics.getHeight())
    }
  }

  override def dispose(): Unit =
    _screen.foreach(_.hide())

  override def pause(): Unit =
    _screen.foreach(_.pause())

  override def resume(): Unit =
    _screen.foreach(_.resume())

  override def render(): Unit =
    _screen.foreach(_.render(Sge().graphics.getDeltaTime()))

  override def resize(width: Pixels, height: Pixels): Unit =
    _screen.foreach(_.resize(width, height))
}
