/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen
package utils

import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.graphics.glutils.FrameBuffer
import sge.screen.ManagedScreen
import sge.utils.{ ScreenUtils, Seconds }

/** Utility methods for rendering screens into framebuffer textures.
  *
  * @author
  *   damios
  */
object ScreenFboUtils {

  /** Renders a screen into a texture region using the given framebuffer.
    *
    * @param screen
    *   the screen to be rendered
    * @param fbo
    *   the framebuffer the screen gets rendered into
    * @param delta
    *   the time delta
    * @return
    *   a texture region which contains the rendered screen
    */
  def screenToTexture(screen: ManagedScreen, fbo: FrameBuffer, delta: Seconds)(using Sge): TextureRegion = {
    fbo.use {
      screen.clearColor.foreach { color =>
        ScreenUtils.clear(color, true)
      }
      screen.render(delta)
    }

    val texture: Texture = fbo.colorBufferTexture

    // flip the texture
    val textureRegion = TextureRegion(texture)
    textureRegion.flip(false, true)

    textureRegion
  }
}
