/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen
package transition

import sge.graphics.Color
import sge.graphics.g2d.TextureRegion
import sge.utils.{ Nullable, Seconds }

/** A transition effect between two screens for use with a {@link ScreenManager}.
  *
  * Note that only under certain conditions close() is called automatically. Check out the method's scaladoc for more information!
  *
  * @author
  *   damios
  */
abstract class ScreenTransition extends AutoCloseable {

  /** Called before this transition starts rendering. If you want to reuse transition instances, this is the place where the transition should be reset. Right after this method, resize() is called.
    */
  def show(): Unit = {
    // don't do anything by default
  }

  /** Called after this transition stops rendering. This is the last chance to obtain the last screen which was rendered as part of the transition.
    */
  def hide(): Unit = {
    // don't do anything by default
  }

  /** Takes care of actually rendering the transition.
    *
    * @param delta
    *   the time delta in seconds
    * @param lastScreen
    *   the old screen as a texture region
    * @param currScreen
    *   the screen the manager is transitioning to as a texture region
    */
  def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit

  /** @return
    *   whether the transition is done; after that is the case, the transition stops and hide() is called
    */
  def isDone: Boolean

  /** Called when the game is resized while this transition is rendered and the new size is different to the previous one. In addition, this method is called right after show().
    *
    * @param width
    *   the new width in pixels
    * @param height
    *   the new height in pixels
    */
  def resize(width: Pixels, height: Pixels): Unit

  /** Called when this transition should release all resources.
    *
    * Is called automatically in two cases:
    *   - when the screen manager is closed and this transition was pushed but not yet hidden
    *   - if auto-close is enabled via ScreenManager#setAutoClose
    */
  override def close(): Unit

  /** @return
    *   the color to clear the screen with before the rendering is started, or empty to not clear the screen
    */
  def clearColor: Nullable[Color] = Nullable(Color.BLACK)
}
