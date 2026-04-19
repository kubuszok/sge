/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: ManagedScreen,_inputProcessors,addInputProcessor,clearColor,close,hide,inputProcessors,pause,render,resize,resume,show
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen

import scala.collection.mutable.ArrayBuffer

import sge.graphics.Color
import sge.utils.{ Nullable, Seconds }

/** A basic screen for use with a {@link ScreenManager}. To render it, the screen has to be {@linkplain ScreenManager#pushScreen pushed}.
  *
  * Use {@link #addInputProcessor} to add input processors that are automatically registered and unregistered whenever the screen is shown/hidden.
  *
  * Note that only under certain conditions {@link #close()} is called automatically. Check out the method's scaladoc for more information!
  *
  * @author
  *   damios
  */
abstract class ManagedScreen extends sge.Screen {

  /** @see #addInputProcessor */
  private val _inputProcessors: ArrayBuffer[InputProcessor] = ArrayBuffer.empty

  /** Adds an input processor that is automatically registered and unregistered whenever the screen is shown/hidden.
    *
    * Input processors added during rendering (so after show(), but before hide()) are only registered when the screen is shown a second time.
    *
    * @param processor
    *   the processor to add
    */
  protected def addInputProcessor(processor: InputProcessor): Unit =
    _inputProcessors += processor

  /** Called when this screen becomes the active screen. Note that at first, the screen may be rendered as part of a transition.
    *
    * If you want to reuse screen instances, this is the place where the screen should be reset. Right after this method, resize() is called.
    */
  override def show(): Unit = {
    // don't do anything by default
  }

  /** Called when this screen is no longer the active screen and a possible transition has finished. */
  override def hide(): Unit = {
    // don't do anything by default
  }

  /** Called when the screen should render itself.
    *
    * Before this method is called, the previously rendered stuff is cleared with the clear color.
    *
    * @param delta
    *   the time in seconds since the last render pass
    */
  override def render(delta: Seconds): Unit

  /** Called when the game is resized while this screen is rendered and the new size is different to the previous one. In addition, this method is called right after show().
    *
    * @param width
    *   the new width in pixels
    * @param height
    *   the new height in pixels
    */
  override def resize(width: Pixels, height: Pixels): Unit

  /** Called when the application is paused while this screen is rendered. */
  override def pause(): Unit = {
    // don't do anything by default
  }

  /** Called when the application is resumed from a paused state. */
  override def resume(): Unit = {
    // don't do anything by default
  }

  /** Called when this screen should release all resources.
    *
    * Is called automatically in two cases:
    *   - when the screen manager is closed and this screen was pushed but not yet hidden
    *   - if auto-dispose is enabled via ScreenManager#setAutoClose
    */
  override def close(): Unit

  /** Returns the input processors registered for this screen. */
  def inputProcessors: ArrayBuffer[InputProcessor] = _inputProcessors

  /** @return
    *   the color to clear the screen with before the rendering is started, or empty to not clear the screen
    */
  def clearColor: Nullable[Color] = Nullable(Color.BLACK)
}
