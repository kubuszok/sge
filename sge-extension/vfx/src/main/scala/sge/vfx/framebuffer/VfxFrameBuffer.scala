/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package framebuffer

import sge.graphics.glutils.FrameBuffer
import sge.graphics.{ GL20, OrthographicCamera, Pixmap, Texture }
import sge.math.Matrix4
import sge.vfx.gl.{ VfxGLUtils, VfxGlViewport }
import sge.utils.Nullable

import scala.collection.mutable.ArrayBuffer

/** Wraps [[FrameBuffer]] and manages currently bound OpenGL FBO.
  *
  * This implementation supports nested frame buffer drawing approach. You can use multiple instances of this class to draw into one
  * frame buffer while you drawing into another one, the OpenGL state will be managed properly.
  *
  * <b>NOTE:</b> Depth and stencil buffers are not supported.
  */
class VfxFrameBuffer(val pixelFormat: Pixmap.Format)(using Sge) extends AutoCloseable {

  private val localProjection: Matrix4 = Matrix4()
  private val localTransform: Matrix4 = Matrix4()

  private val renderers: VfxFrameBuffer.RendererManager = VfxFrameBuffer.RendererManager()

  private val preservedViewport: VfxGlViewport = VfxGlViewport()
  private var previousFboHandle: Int = 0

  private var fbo: Nullable[FrameBuffer] = Nullable.empty
  private var _initialized: Boolean = false
  private var _drawing: Boolean = false

  override def close(): Unit =
    reset()

  def initialize(width: Int, height: Int): Unit = {
    if (_initialized) { close() }

    _initialized = true

    val boundFboHandle = getBoundFboHandle()
    fbo = Nullable(FrameBuffer(pixelFormat, Pixels(width), Pixels(height), false))
    fbo.get.colorBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    Sge().graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, boundFboHandle)

    val cam = OrthographicCamera()
    cam.setToOrtho(false, WorldUnits(width.toFloat), WorldUnits(height.toFloat))
    localProjection.set(cam.combined)
    localTransform.idt()
  }

  def reset(): Unit = {
    if (!_initialized) {
      // do nothing
    } else {
      _initialized = false
      fbo.get.close()
      fbo = Nullable.empty
    }
  }

  def getFbo: Nullable[FrameBuffer] = fbo

  def texture: Nullable[Texture] =
    if (fbo.isEmpty) Nullable.empty else Nullable(fbo.get.colorBufferTexture)

  def initialized: Boolean = _initialized

  /** @return true means [[begin]] has been called */
  def isDrawing: Boolean = _drawing

  def addRenderer(renderer: VfxFrameBuffer.Renderer): Unit =
    renderers.addRenderer(renderer)

  def removeRenderer(renderer: VfxFrameBuffer.Renderer): Unit =
    renderers.removeRenderer(renderer)

  def clearRenderers(): Unit =
    renderers.clearRenderers()

  def projectionMatrix: Matrix4 = localProjection

  def projectionMatrix_=(matrix: Matrix4): Unit =
    localProjection.set(matrix)

  def transformMatrix: Matrix4 = localTransform

  def transformMatrix_=(matrix: Matrix4): Unit =
    localTransform.set(matrix)

  def begin(): Unit = {
    VfxFrameBuffer.bufferNesting += 1

    if (!_initialized) throw IllegalStateException("VfxFrameBuffer must be initialized first")
    if (_drawing) throw IllegalStateException("Already drawing")

    _drawing = true

    renderers.flush()
    previousFboHandle = getBoundFboHandle()
    preservedViewport.set(getViewport())
    val f = fbo.get
    Sge().graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, f.getFramebufferHandle)
    Sge().graphics.gl20.glViewport(Pixels.zero, Pixels.zero, f.width, f.height)
    renderers.assignLocalMatrices(localProjection, localTransform)
  }

  def end(): Unit = {
    VfxFrameBuffer.bufferNesting -= 1

    if (!_initialized) throw IllegalStateException("VfxFrameBuffer must be initialized first")
    if (!_drawing) throw IllegalStateException("Is not drawing")

    val f = fbo.get
    if (getBoundFboHandle() != f.getFramebufferHandle) {
      throw IllegalStateException(
        "Current bound OpenGL FBO's handle doesn't match to wrapped one. It seems like begin/end order was violated."
      )
    }

    _drawing = false

    renderers.flush()
    Sge().graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, previousFboHandle)
    Sge().graphics.gl20.glViewport(
      Pixels(preservedViewport.x),
      Pixels(preservedViewport.y),
      Pixels(preservedViewport.width),
      Pixels(preservedViewport.height)
    )
    renderers.restoreOwnMatrices()
  }

  protected def getBoundFboHandle(): Int =
    VfxGLUtils.getBoundFboHandle()

  protected def getViewport(): VfxGlViewport =
    VfxGLUtils.getViewport()
}

object VfxFrameBuffer {
  /** Current depth of buffer nesting rendering (keeps track of how many buffers are currently activated). */
  var bufferNesting: Int = 0

  trait Renderer {
    def flush(): Unit
    def assignLocalMatrices(projection: Matrix4, transform: Matrix4): Unit
    def restoreOwnMatrices(): Unit
  }

  abstract class RendererAdapter extends Renderer {
    private val preservedProjection: Matrix4 = Matrix4()
    private val preservedTransform: Matrix4 = Matrix4()

    override def assignLocalMatrices(projection: Matrix4, transform: Matrix4): Unit = {
      preservedProjection.set(getProjection)
      preservedTransform.set(getTransform)
      setProjection(projection)
    }

    override def restoreOwnMatrices(): Unit =
      setProjection(preservedProjection)

    protected def getProjection: Matrix4
    protected def getTransform: Matrix4
    protected def setProjection(projection: Matrix4): Unit
    protected def setTransform(transform: Matrix4): Unit
  }

  private class RendererManager extends Renderer {
    private val _renderers: ArrayBuffer[Renderer] = ArrayBuffer.empty

    def addRenderer(renderer: Renderer): Unit =
      _renderers += renderer

    def removeRenderer(renderer: Renderer): Unit =
      _renderers -= renderer

    def clearRenderers(): Unit =
      _renderers.clear()

    override def flush(): Unit = {
      var i = 0
      while (i < _renderers.size) {
        _renderers(i).flush()
        i += 1
      }
    }

    override def assignLocalMatrices(projection: Matrix4, transform: Matrix4): Unit = {
      var i = 0
      while (i < _renderers.size) {
        _renderers(i).assignLocalMatrices(projection, transform)
        i += 1
      }
    }

    override def restoreOwnMatrices(): Unit = {
      var i = 0
      while (i < _renderers.size) {
        _renderers(i).restoreOwnMatrices()
        i += 1
      }
    }
  }
}
