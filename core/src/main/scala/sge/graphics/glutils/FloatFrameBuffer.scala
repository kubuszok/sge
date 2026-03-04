/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FloatFrameBuffer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses GL20.GL_RGBA/GL20.GL_FLOAT where Java uses GL30 constants (values are identical)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.Application.ApplicationType
import sge.graphics.Texture.TextureFilter
import sge.graphics.Texture.TextureWrap
import sge.graphics.Texture
import sge.graphics.GL20
import sge.graphics.GL30
import sge.graphics.glutils.FrameBuffer
import sge.graphics.glutils.FloatTextureData
import sge.utils.SgeError
import sge.Sge

/** This is a {@link FrameBuffer} variant backed by a float texture. */
class FloatFrameBuffer()(using Sge) extends FrameBuffer {

  checkExtensions()

  /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder
    *
    * @param bufferBuilder
    *   *
    */
  def this(bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Texture]])(using Sge) = {
    this()
    this.bufferBuilder = bufferBuilder
    build()
    checkExtensions()
  }

  /** Creates a new FrameBuffer with a float backing texture, having the given dimensions and potentially a depth buffer attached.
    *
    * @param width
    *   the width of the framebuffer in pixels
    * @param height
    *   the height of the framebuffer in pixels
    * @param hasDepth
    *   whether to attach a depth buffer
    * @throws SgeError.GraphicsError
    *   in case the FrameBuffer could not be created
    */
  def this(width: Int, height: Int, hasDepth: Boolean)(using Sge) = {
    this()
    checkExtensions()
    val bufferBuilder = GLFrameBuffer.FloatFrameBufferBuilder(width, height)
    bufferBuilder.addFloatAttachment(GL30.GL_RGBA32F, GL20.GL_RGBA, GL20.GL_FLOAT, false)
    if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer()
    this.bufferBuilder = bufferBuilder

    build()
  }

  override protected def createTexture(attachmentSpec: GLFrameBuffer.FrameBufferTextureAttachmentSpec): Texture = {
    val data = FloatTextureData(
      bufferBuilder.width,
      bufferBuilder.height,
      attachmentSpec.internalFormat,
      attachmentSpec.format,
      attachmentSpec.`type`,
      attachmentSpec.isGpuOnly
    )
    val result = Texture(data)
    if (Sge().application.getType() == ApplicationType.Desktop || Sge().application.getType() == ApplicationType.Applet)
      result.setFilter(TextureFilter.Linear, TextureFilter.Linear)
    else
      // no filtering for float textures in OpenGL ES
      result.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
    result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)
    result
  }

  /** Check for support for any required extensions on the current platform. */
  private def checkExtensions(): Unit =
    if (Sge().graphics.isGL30Available() && Sge().application.getType() == ApplicationType.WebGL) {
      // For WebGL2, Rendering to a Floating Point Texture requires this extension
      if (!Sge().graphics.supportsExtension("EXT_color_buffer_float"))
        throw SgeError.GraphicsError("Extension EXT_color_buffer_float not supported!")
    }
}
