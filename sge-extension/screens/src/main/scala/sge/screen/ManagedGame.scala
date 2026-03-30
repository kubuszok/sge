/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen

import sge.screen.transition.ScreenTransition

/** A game class that utilizes a screen manager. Use the provided instance to push screens.
  *
  * Input listeners have to be added via the game's input multiplexer. Otherwise, the automatic registration and unregistration of a screen's input processors does not work.
  *
  * @author
  *   damios
  */
class ManagedGame[S <: ManagedScreen, T <: ScreenTransition](using Sge) extends ApplicationListener {

  /** The input multiplexer of the game. Should be used to add input processors instead of directly calling Sge().input.inputProcessor=. Otherwise, the automatic registration and unregistration of a
    * screen's input processors does not work.
    */
  protected val inputMultiplexer: InputMultiplexer = InputMultiplexer()

  /** The game's screen manager. Is used to push new screens/transitions. */
  protected val screenManager: ScreenManager[S, T] = ScreenManager[S, T]()

  override def create(): Unit = {
    Sge().input.setInputProcessor(inputMultiplexer)
    screenManager.initialize(
      inputMultiplexer,
      Sge().graphics.width,
      Sge().graphics.height,
      false
    )
  }

  override def render(): Unit =
    screenManager.render(Sge().graphics.deltaTime)

  /** Called when the application is resized. This can happen at any point during a non-paused state, but will never happen before a call to create().
    *
    * resize(0, 0) calls, which may happen when the game is minimized on Windows, are ignored.
    */
  override def resize(width: Pixels, height: Pixels): Unit =
    // if the game is minimized on Windows, resize(0, 0) is called.
    // This causes problems, as a framebuffer with these dimensions cannot be created.
    // Therefore, it is simply ignored.
    if (width.toInt != 0 && height.toInt != 0) {
      screenManager.resize(width, height)
    }

  override def pause(): Unit =
    screenManager.pause()

  override def resume(): Unit =
    screenManager.resume()

  override def dispose(): Unit =
    screenManager.close()
}
