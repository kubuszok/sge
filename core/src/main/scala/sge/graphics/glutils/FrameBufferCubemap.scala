/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FrameBufferCubemap.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.Texture.TextureFilter
import sge.graphics.Texture.TextureWrap
import sge.graphics.Cubemap
import sge.graphics.GL20
import sge.graphics.Pixmap
import sge.utils.SgeError
import sge.Sge

/** <p> Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It will automatically create a cubemap for the color attachment and a
  * renderbuffer for the depth buffer. You can get a hold of the cubemap by FrameBufferCubemap.getColorBufferTexture(). This class will only work with OpenGL ES 2.0. </p>
  *
  * <p> FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to another application or receives an incoming call, the framebuffer will be
  * automatically recreated. </p>
  *
  * <p> A FrameBuffer must be disposed if it is no longer needed </p>
  *
  * <p> Typical use: <br /> FrameBufferCubemap frameBuffer = new FrameBufferCubemap(Format.RGBA8888, fSize, fSize, true); <br /> frameBuffer.begin(); <br /> while( frameBuffer.nextSide() ) { <br />
  * frameBuffer.getSide().getUp(camera.up); <br /> frameBuffer.getSide().getDirection(camera.direction);<br /> camera.update(); <br />
  *
  * sge.graphics.gl.glClearColor(0, 0, 0, 1); <br /> sge.graphics.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT); <br /> modelBatch.begin(camera); <br />
  * modelBatch.render(renderableProviders); <br /> modelBatch.end(); <br /> } <br /> frameBuffer.end(); <br /> Cubemap cubemap = frameBuffer.getColorBufferCubemap(); </p>
  *
  * @author
  *   realitix
  */
class FrameBufferCubemap()(using Sge) extends GLFrameBuffer[Cubemap] {

  /** the zero-based index of the active side * */
  private var currentSide: Int = -1

  /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder
    *
    * @param bufferBuilder
    *   *
    */
  def this(bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Cubemap]])(using Sge) = {
    this()
    this.bufferBuilder = bufferBuilder
    build()
  }

  /** Creates a new FrameBuffer having the given dimensions and potentially a depth and a stencil buffer attached.
    *
    * @param format
    *   the format of the color buffer; according to the OpenGL ES 2.0 spec, only RGB565, RGBA4444 and RGB5_A1 are color-renderable
    * @param width
    *   the width of the cubemap in pixels
    * @param height
    *   the height of the cubemap in pixels
    * @param hasDepth
    *   whether to attach a depth buffer
    * @param hasStencil
    *   whether to attach a stencil buffer
    * @throws SgeError.GraphicsError
    *   in case the FrameBuffer could not be created
    */
  def this(format: Pixmap.Format, width: Int, height: Int, hasDepth: Boolean, hasStencil: Boolean)(using Sge) = {
    this()
    val frameBufferBuilder = new GLFrameBuffer.FrameBufferCubemapBuilder(width, height)
    frameBufferBuilder.addBasicColorTextureAttachment(format)
    if (hasDepth) frameBufferBuilder.addBasicDepthRenderBuffer()
    if (hasStencil) frameBufferBuilder.addBasicStencilRenderBuffer()
    this.bufferBuilder = frameBufferBuilder

    build()
  }

  /** Creates a new FrameBuffer having the given dimensions and potentially a depth buffer attached.
    *
    * @param format
    * @param width
    * @param height
    * @param hasDepth
    */
  def this(format: Pixmap.Format, width: Int, height: Int, hasDepth: Boolean)(using Sge) =
    this(format, width, height, hasDepth, false)

  // TODO: Convert and implement when all dependencies are converted
  override protected def createTexture(attachmentSpec: GLFrameBuffer.FrameBufferTextureAttachmentSpec): Cubemap = {
    val data   = new GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat, attachmentSpec.format, attachmentSpec.`type`)
    val result = new Cubemap(data, data, data, data, data, data)
    result.setFilter(TextureFilter.Linear, TextureFilter.Linear)
    result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)
    result
  }

  override protected def disposeColorTexture(colorTexture: Cubemap): Unit =
    throw SgeError.GraphicsError("FrameBufferCubemap.disposeColorTexture() not yet fully converted to Scala")
  // colorTexture.close()

  override protected def attachFrameBufferColorTexture(texture: Cubemap): Unit = {
    val gl       = Sge().graphics.gl20
    val glHandle = texture.getTextureObjectHandle()
    val sides    = Cubemap.CubemapSide.values
    for (side <- sides)
      gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, side.glEnum, glHandle.toInt, 0)
  }

  /** Makes the frame buffer current so everything gets drawn to it, must be followed by call to either nextSide() or bindSide(Cubemap.CubemapSide) to activate the side to render onto.
    */
  override def bind(): Unit = {
    currentSide = -1
    super.bind()
  }

  /** Bind the next side of cubemap and return false if no more side. Should be called in between a call to begin() and end to cycle to each side of the cubemap to render on.
    */
  def nextSide(): Boolean =
    if (currentSide > 5) {
      throw SgeError.GraphicsError("No remaining sides.")
    } else if (currentSide == 5) {
      false
    } else {
      currentSide += 1
      bindSide(getSide())
      true
    }

  /** Bind the side, making it active to render on. Should be called in between a call to begin() and end().
    * @param side
    *   The side to bind
    */
  protected def bindSide(side: Cubemap.CubemapSide): Unit =
    Sge().graphics.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, side.glEnum, getColorBufferTexture().getTextureObjectHandle().toInt, 0)

  /** Get the currently bound side. */
  def getSide(): Cubemap.CubemapSide =
    if (currentSide < 0) null else FrameBufferCubemap.cubemapSides(currentSide)
}

object FrameBufferCubemap {

  /** cubemap sides cache */
  private val cubemapSides: Array[Cubemap.CubemapSide] = Cubemap.CubemapSide.values
}
