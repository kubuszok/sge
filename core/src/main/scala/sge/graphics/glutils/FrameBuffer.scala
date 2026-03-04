/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FrameBuffer.java
 * Original authors: mzechner, realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses (using Sge) context parameter; handles WebGL depth texture workaround
 *   Idiom: split packages
 *   TODO: opaque Pixels for getWidth/Height, end(x, y, width, height) params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.Texture.TextureFilter
import sge.graphics.Texture.TextureWrap
import sge.graphics.Pixmap
import sge.graphics.Texture
import sge.graphics.GL20
import sge.Application
import sge.Sge

/** <p> Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It will automatically create a texture for the color attachment and a
  * renderbuffer for the depth buffer. You can get a hold of the texture by FrameBuffer.getColorBufferTexture(). This class will only work with OpenGL ES 2.0. </p>
  *
  * <p> FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to another application or receives an incoming call, the framebuffer will be
  * automatically recreated. </p>
  *
  * <p> A FrameBuffer must be disposed if it is no longer needed </p>
  *
  * @author
  *   mzechner, realitix
  */
class FrameBuffer(using Sge) extends GLFrameBuffer[Texture] {

  /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder
    *
    * @param bufferBuilder
    *   *
    */
  def this(bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Texture]])(using Sge) = {
    this()
    this.bufferBuilder = bufferBuilder
    build()
  }

  /** Creates a new FrameBuffer having the given dimensions and potentially a depth and a stencil buffer attached.
    *
    * @param format
    *   the format of the color buffer; according to the OpenGL ES 2.0 spec, only RGB565, RGBA4444 and RGB5_A1 are color-renderable
    * @param width
    *   the width of the framebuffer in pixels
    * @param height
    *   the height of the framebuffer in pixels
    * @param hasDepth
    *   whether to attach a depth buffer
    * @throws SgeError.GraphicsError
    *   in case the FrameBuffer could not be created
    */
  def this(format: Pixmap.Format, width: Int, height: Int, hasDepth: Boolean, hasStencil: Boolean)(using Sge) = {
    this()
    val frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(width, height)
    frameBufferBuilder.addBasicColorTextureAttachment(format)
    if (hasDepth) frameBufferBuilder.addBasicDepthRenderBuffer()
    if (hasStencil) frameBufferBuilder.addBasicStencilRenderBuffer()
    this.bufferBuilder = frameBufferBuilder

    build()
  }

  /** Creates a new FrameBuffer having the given dimensions and potentially a depth buffer attached. */
  def this(format: Pixmap.Format, width: Int, height: Int, hasDepth: Boolean)(using Sge) = {
    this(format, width, height, hasDepth, false)
  }

  override protected def createTexture(attachmentSpec: GLFrameBuffer.FrameBufferTextureAttachmentSpec): Texture = {
    val data   = new GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat, attachmentSpec.format, attachmentSpec.`type`)
    val result = new Texture(data)
    // Filtering support for depth textures on WebGL is spotty https://github.com/KhronosGroup/OpenGL-API/issues/84
    val webGLDepth = attachmentSpec.isDepth && Sge().application.getType() == Application.ApplicationType.WebGL
    if (!webGLDepth) {
      result.setFilter(TextureFilter.Linear, TextureFilter.Linear)
    }
    result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)
    result
  }

  override protected def disposeColorTexture(colorTexture: Texture): Unit =
    colorTexture.close()

  override protected def attachFrameBufferColorTexture(texture: Texture): Unit =
    Sge().graphics.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D, texture.getTextureObjectHandle().toInt, 0)
}

object FrameBuffer {

  /** See GLFrameBuffer.unbind() */
  def unbind()(using Sge): Unit =
    GLFrameBuffer.unbind()
}
