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

import sge.graphics.{ GL20, Texture }
import sge.graphics.g2d.TextureRegion
import sge.graphics.glutils.FrameBuffer
import sge.screen.ManagedScreen
import sge.utils.{ ScreenUtils, Seconds }

import java.nio.{ ByteBuffer, ByteOrder, IntBuffer }

/** Utility methods for rendering screens into framebuffer textures.
  *
  * @author
  *   damios
  */
object ScreenFboUtils {

  private val tmpIntBuf: IntBuffer =
    ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()

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

  /** Retrieves the current FBO binding and viewport state so it can be restored later.
    *
    * The returned array contains 5 elements: `[fboHandle, viewportX, viewportY, viewportWidth, viewportHeight]`.
    *
    * @return
    *   an array capturing the current FBO handle and viewport parameters
    */
  def retrieveFboStatus()(using Sge): Array[Int] = {
    val gl = Sge().graphics.gl20

    tmpIntBuf.clear()
    gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, tmpIntBuf)
    val previousFBOHandle = tmpIntBuf.get(0)

    tmpIntBuf.clear()
    gl.glGetIntegerv(GL20.GL_VIEWPORT, tmpIntBuf)
    val viewportX = tmpIntBuf.get(0)
    val viewportY = tmpIntBuf.get(1)
    val viewportW = tmpIntBuf.get(2)
    val viewportH = tmpIntBuf.get(3)

    Array(previousFBOHandle, viewportX, viewportY, viewportW, viewportH)
  }

  /** Restores FBO binding and viewport state previously captured by [[retrieveFboStatus]].
    *
    * @param status
    *   an array of exactly 5 elements: `[fboHandle, viewportX, viewportY, viewportWidth, viewportHeight]`
    */
  def restoreFboStatus(status: Array[Int])(using Sge): Unit = {
    require(status.length == 5, s"Expected status array of length 5, got ${status.length}")
    val gl = Sge().graphics.gl20
    gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, status(0))
    gl.glViewport(Pixels(status(1)), Pixels(status(2)), Pixels(status(3)), Pixels(status(4)))
  }
}
