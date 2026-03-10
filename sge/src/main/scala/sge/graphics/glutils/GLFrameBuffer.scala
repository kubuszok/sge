/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/GLFrameBuffer.java
 * Original authors: mzechner, realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses DynamicArray for managed buffer tracking; unbind() moved to companion; builder uses Scala 3 type parameters
 *   Idiom: split packages
 *   Idiom: opaque Pixels for getWidth/Height, builder width/height
 *   Convention: typed GL enums — TextureTarget, PixelFormat, DataType, ClearMask for GL method params
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.{ Buffer, ByteBuffer, ByteOrder, IntBuffer }
import scala.collection.mutable

import sge.Application
import sge.utils.SgeError
import sge.graphics.GL20
import sge.graphics.GL30
import sge.graphics.GLTexture
import sge.graphics.Pixmap
import sge.Sge
import sge.utils.{ BufferUtils, DynamicArray, MkArray, Nullable }

/** <p> Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It will automatically create a gltexture for the color attachment and a
  * renderbuffer for the depth buffer. You can get a hold of the gltexture by GLFrameBuffer.getColorBufferTexture(). This class will only work with OpenGL ES 2.0. </p>
  *
  * <p> FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to another application or receives an incoming call, the framebuffer will be
  * automatically recreated. </p>
  *
  * <p> A FrameBuffer must be disposed if it is no longer needed </p>
  *
  * @author
  *   mzechner, realitix
  */
abstract class GLFrameBuffer[T <: GLTexture](using Sge) extends AutoCloseable {

  /** the color buffer texture * */
  protected val textureAttachments: DynamicArray[T] = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)

  /** the framebuffer handle * */
  protected var framebufferHandle: Int = scala.compiletime.uninitialized

  /** the depthbuffer render object handle * */
  protected var depthbufferHandle: Int = scala.compiletime.uninitialized

  /** the stencilbuffer render object handle * */
  protected var stencilbufferHandle: Int = scala.compiletime.uninitialized

  /** the depth stencil packed render buffer object handle * */
  protected var depthStencilPackedBufferHandle: Int = scala.compiletime.uninitialized

  /** if has depth stencil packed buffer * */
  protected var hasDepthStencilPackedBuffer: Boolean = scala.compiletime.uninitialized

  /** the colorbuffer render object handles * */
  protected val colorBufferHandles: DynamicArray[Int] = DynamicArray[Int]()

  /** if multiple texture attachments are present * */
  protected var isMRT: Boolean = scala.compiletime.uninitialized

  protected var bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[T]] = scala.compiletime.uninitialized

  private var defaultDrawBuffers: Nullable[IntBuffer] = Nullable.empty

  private var drawBuffersForTransfer: Nullable[IntBuffer] = Nullable.empty

  /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder * */
  protected def this(bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[T]])(using Sge) = {
    this()
    this.bufferBuilder = bufferBuilder
    build()
  }

  /** Convenience method to return the first Texture attachment present in the fbo * */
  def getColorBufferTexture(): T =
    textureAttachments.first

  /** Return the Texture attachments attached to the fbo * */
  def getTextureAttachments(): DynamicArray[T] =
    textureAttachments

  /** Override this method in a derived class to set up the backing texture as you like. */
  protected def createTexture(attachmentSpec: GLFrameBuffer.FrameBufferTextureAttachmentSpec): T

  /** Override this method in a derived class to dispose the backing texture as you like. */
  protected def disposeColorTexture(colorTexture: T): Unit

  /** Override this method in a derived class to attach the backing texture to the GL framebuffer object. */
  protected def attachFrameBufferColorTexture(texture: T): Unit

  private def checkValidBuilder(): Unit = {
    if (bufferBuilder.samples > 0 && !Sge().graphics.isGL30Available()) {
      throw SgeError.GraphicsError("Framebuffer multisample requires GLES 3.0+")
    }
    if (bufferBuilder.samples > 0 && bufferBuilder.textureAttachmentSpecs.size > 0) {
      throw SgeError.GraphicsError("Framebuffer multisample with texture attachments not yet supported")
    }

    val runningGL30 = Sge().graphics.isGL30Available()

    if (!runningGL30) {
      val supportsPackedDepthStencil = Sge().graphics.supportsExtension("GL_OES_packed_depth_stencil") ||
        Sge().graphics.supportsExtension("GL_EXT_packed_depth_stencil")

      if (bufferBuilder.hasPackedStencilDepthRenderBuffer && !supportsPackedDepthStencil) {
        throw SgeError.GraphicsError("Packed Stencil/Render render buffers are not available on GLES 2.0")
      }
      if (bufferBuilder.textureAttachmentSpecs.size > 1) {
        throw SgeError.GraphicsError("Multiple render targets not available on GLES 2.0")
      }
      bufferBuilder.textureAttachmentSpecs.foreach { spec =>
        if (spec.isDepth) throw SgeError.GraphicsError("Depth texture FrameBuffer Attachment not available on GLES 2.0")
        if (spec.isStencil) throw SgeError.GraphicsError("Stencil texture FrameBuffer Attachment not available on GLES 2.0")
        if (spec.isFloat) {
          if (!Sge().graphics.supportsExtension("OES_texture_float")) {
            throw SgeError.GraphicsError("Float texture FrameBuffer Attachment not available on GLES 2.0")
          }
        }
      }
    }

    if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
      if (bufferBuilder.hasDepthRenderBuffer || bufferBuilder.hasStencilRenderBuffer)
        throw SgeError.GraphicsError(
          "Frame buffer couldn't be constructed: packed stencil depth buffer cannot be specified together with separated depth or stencil buffer"
        )
    }
  }

  protected def build(): Unit = {
    val gl = Sge().graphics.gl20

    checkValidBuilder()

    // iOS uses a different framebuffer handle! (not necessarily 0)
    if (!GLFrameBuffer.defaultFramebufferHandleInitialized) {
      GLFrameBuffer.defaultFramebufferHandleInitialized = true
      if (Sge().application.getType() == Application.ApplicationType.iOS) {
        val intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()
        gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf)
        GLFrameBuffer.defaultFramebufferHandle = intbuf.get(0)
      } else {
        GLFrameBuffer.defaultFramebufferHandle = 0
      }
    }

    framebufferHandle = gl.glGenFramebuffer()
    gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)

    val width  = bufferBuilder.width
    val height = bufferBuilder.height

    if (bufferBuilder.hasDepthRenderBuffer) {
      depthbufferHandle = gl.glGenRenderbuffer()
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthbufferHandle)
      if (bufferBuilder.samples > 0) {
        @scala.annotation.nowarn("msg=deprecated")
        val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary
        gl30.glRenderbufferStorageMultisample(
          GL20.GL_RENDERBUFFER,
          bufferBuilder.samples,
          bufferBuilder.depthRenderBufferSpec.internalFormat,
          width.toInt,
          height.toInt
        )
      } else {
        gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.depthRenderBufferSpec.internalFormat, width, height)
      }
    }

    if (bufferBuilder.hasStencilRenderBuffer) {
      stencilbufferHandle = gl.glGenRenderbuffer()
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, stencilbufferHandle)
      if (bufferBuilder.samples > 0) {
        @scala.annotation.nowarn("msg=deprecated")
        val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary
        gl30.glRenderbufferStorageMultisample(
          GL20.GL_RENDERBUFFER,
          bufferBuilder.samples,
          bufferBuilder.stencilRenderBufferSpec.internalFormat,
          width.toInt,
          height.toInt
        )
      } else {
        gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.stencilRenderBufferSpec.internalFormat, width, height)
      }
    }

    if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
      depthStencilPackedBufferHandle = gl.glGenRenderbuffer()
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
      if (bufferBuilder.samples > 0) {
        @scala.annotation.nowarn("msg=deprecated")
        val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary
        gl30.glRenderbufferStorageMultisample(
          GL20.GL_RENDERBUFFER,
          bufferBuilder.samples,
          bufferBuilder.packedStencilDepthRenderBufferSpec.internalFormat,
          width.toInt,
          height.toInt
        )
      } else {
        gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.packedStencilDepthRenderBufferSpec.internalFormat, width, height)
      }
      hasDepthStencilPackedBuffer = true
    }

    isMRT = bufferBuilder.textureAttachmentSpecs.size > 1
    var colorAttachmentCounter = 0
    if (isMRT) {
      bufferBuilder.textureAttachmentSpecs.foreach { attachmentSpec =>
        val texture = createTexture(attachmentSpec)
        textureAttachments.add(texture)
        if (attachmentSpec.isColorTexture()) {
          gl.glFramebufferTexture2D(
            GL20.GL_FRAMEBUFFER,
            GL20.GL_COLOR_ATTACHMENT0 + colorAttachmentCounter,
            TextureTarget.Texture2D,
            texture.getTextureObjectHandle().toInt,
            0
          )
          colorAttachmentCounter += 1
        } else if (attachmentSpec.isDepth) {
          gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, TextureTarget.Texture2D, texture.getTextureObjectHandle().toInt, 0)
        } else if (attachmentSpec.isStencil) {
          gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, TextureTarget.Texture2D, texture.getTextureObjectHandle().toInt, 0)
        }
      }
    } else if (bufferBuilder.textureAttachmentSpecs.size > 0) {
      val texture = createTexture(bufferBuilder.textureAttachmentSpecs.first)
      textureAttachments.add(texture)
      gl.glBindTexture(texture.glTarget, texture.getTextureObjectHandle().toInt)
    }

    bufferBuilder.colorRenderBufferSpecs.foreach { colorBufferSpec =>
      val colorbufferHandle = gl.glGenRenderbuffer()
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, colorbufferHandle)
      if (bufferBuilder.samples > 0) {
        @scala.annotation.nowarn("msg=deprecated")
        val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary
        gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples, colorBufferSpec.internalFormat, width.toInt, height.toInt)
      } else {
        gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, colorBufferSpec.internalFormat, width, height)
      }
      Sge().graphics.gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0 + colorAttachmentCounter, GL20.GL_RENDERBUFFER, colorbufferHandle)
      colorBufferHandles.add(colorbufferHandle)
      colorAttachmentCounter += 1
    }

    if (isMRT || bufferBuilder.samples > 0) {
      val drawBuffers = BufferUtils.newIntBuffer(colorAttachmentCounter)
      var i           = 0
      while (i < colorAttachmentCounter) {
        drawBuffers.put(GL20.GL_COLOR_ATTACHMENT0 + i)
        i += 1
      }
      drawBuffers.asInstanceOf[Buffer].position(0)
      defaultDrawBuffers = Nullable(drawBuffers)
      @scala.annotation.nowarn("msg=deprecated")
      val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary
      gl30.glDrawBuffers(colorAttachmentCounter, drawBuffers)
    } else if (bufferBuilder.textureAttachmentSpecs.size > 0) {
      attachFrameBufferColorTexture(textureAttachments.first)
    }

    if (bufferBuilder.hasDepthRenderBuffer) {
      gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER, depthbufferHandle)
    }

    if (bufferBuilder.hasStencilRenderBuffer) {
      gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, stencilbufferHandle)
    }

    if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
      gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
    }

    gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0)
    textureAttachments.foreach { texture =>
      gl.glBindTexture(texture.glTarget, 0)
    }

    var result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)

    if (
      result == GL20.GL_FRAMEBUFFER_UNSUPPORTED && bufferBuilder.hasDepthRenderBuffer && bufferBuilder.hasStencilRenderBuffer
      && (Sge().graphics.supportsExtension("GL_OES_packed_depth_stencil")
        || Sge().graphics.supportsExtension("GL_EXT_packed_depth_stencil"))
    ) {
      if (bufferBuilder.hasDepthRenderBuffer) {
        gl.glDeleteRenderbuffer(depthbufferHandle)
        depthbufferHandle = 0
      }
      if (bufferBuilder.hasStencilRenderBuffer) {
        gl.glDeleteRenderbuffer(stencilbufferHandle)
        stencilbufferHandle = 0
      }
      if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
        gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
        depthStencilPackedBufferHandle = 0
      }

      depthStencilPackedBufferHandle = gl.glGenRenderbuffer()
      hasDepthStencilPackedBuffer = true
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
      if (bufferBuilder.samples > 0) {
        @scala.annotation.nowarn("msg=deprecated")
        val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary
        gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples, GL30.GL_DEPTH24_STENCIL8, width.toInt, height.toInt)
      } else {
        gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, width, height)
      }
      gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0)

      gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
      gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
      result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)
    }

    gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, GLFrameBuffer.defaultFramebufferHandle)

    if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
      textureAttachments.foreach { texture =>
        disposeColorTexture(texture)
      }

      colorBufferHandles.foreach { handle =>
        gl.glDeleteRenderbuffer(handle)
      }

      if (hasDepthStencilPackedBuffer) {
        gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
      } else {
        if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
        if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
      }

      gl.glDeleteFramebuffer(framebufferHandle)

      if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
        throw new IllegalStateException("Frame buffer couldn't be constructed: incomplete attachment")
      if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
        throw new IllegalStateException("Frame buffer couldn't be constructed: incomplete dimensions")
      if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
        throw new IllegalStateException("Frame buffer couldn't be constructed: missing attachment")
      if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED)
        throw new IllegalStateException("Frame buffer couldn't be constructed: unsupported combination of formats")
      if (result == GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE)
        throw new IllegalStateException("Frame buffer couldn't be constructed: multisample mismatch")
      throw new IllegalStateException("Frame buffer couldn't be constructed: unknown error " + result)
    }

    GLFrameBuffer.addManagedFrameBuffer(Sge().application, this)
  }

  /** Releases all resources associated with the FrameBuffer. */
  override def close(): Unit = {
    val gl = Sge().graphics.gl20

    textureAttachments.foreach { texture =>
      disposeColorTexture(texture)
    }

    colorBufferHandles.foreach { handle =>
      gl.glDeleteRenderbuffer(handle)
    }

    gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
    gl.glDeleteRenderbuffer(depthbufferHandle)
    gl.glDeleteRenderbuffer(stencilbufferHandle)

    gl.glDeleteFramebuffer(framebufferHandle)

    val managedResources = GLFrameBuffer.buffers.get(Sge().application)
    if (managedResources.isDefined) managedResources.get.removeValueByRef(this)
  }

  /** Makes the frame buffer current so everything gets drawn to it. */
  def bind(): Unit =
    Sge().graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)

  /** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it. */
  def begin(): Unit = {
    bind()
    setFrameBufferViewport()
  }

  /** Sets viewport to the dimensions of framebuffer. Called by begin(). */
  protected def setFrameBufferViewport(): Unit =
    Sge().graphics.gl20.glViewport(Pixels.zero, Pixels.zero, bufferBuilder.width, bufferBuilder.height)

  /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
  def end(): Unit =
    end(Pixels.zero, Pixels.zero, Sge().graphics.getBackBufferWidth(), Sge().graphics.getBackBufferHeight())

  /** Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
    *
    * @param x
    *   the x-axis position of the viewport in pixels
    * @param y
    *   the y-asis position of the viewport in pixels
    * @param width
    *   the width of the viewport in pixels
    * @param height
    *   the height of the viewport in pixels
    */
  def end(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {
    GLFrameBuffer.unbind()
    Sge().graphics.gl20.glViewport(x, y, width, height)
  }

  /** Transfer pixels from this frame buffer to the destination frame buffer. Usually used when using multisample, it resolves samples from this multisample FBO to a non-multisample as destination in
    * order to be used as textures. This is a convenient method that automatically choose which of stencil, depth, and colors buffers attachment to be copied.
    * @param destination
    *   the destination of the copy.
    */
  def transfer(destination: GLFrameBuffer[T]): Unit = {
    var copyBits = ClearMask(0)
    destination.bufferBuilder.textureAttachmentSpecs.foreach { attachment =>
      if (attachment.isDepth && (bufferBuilder.hasDepthRenderBuffer || bufferBuilder.hasPackedStencilDepthRenderBuffer)) {
        copyBits = copyBits | ClearMask.DepthBufferBit
      } else if (
        attachment.isStencil
        && (bufferBuilder.hasStencilRenderBuffer || bufferBuilder.hasPackedStencilDepthRenderBuffer)
      ) {
        copyBits = copyBits | ClearMask.StencilBufferBit
      } else if (colorBufferHandles.size > 0) {
        copyBits = copyBits | ClearMask.ColorBufferBit
      }
    }

    transfer(destination, copyBits)
  }

  /** Transfer pixels from this frame buffer to the destination frame buffer. Usually used when using multisample, it resolves samples from this multisample FBO to a non-multisample as destination in
    * order to be used as textures.
    * @param destination
    *   the destination of the copy (should be same size as this frame buffer).
    * @param copyBits
    *   combination of GL20.GL_COLOR_BUFFER_BIT, GL20.GL_STENCIL_BUFFER_BIT, and GL20.GL_DEPTH_BUFFER_BIT. When GL20.GL_COLOR_BUFFER_BIT is present, every color buffers will be copied to each
    *   corresponding color texture buffers in the destination framebuffer.
    */
  def transfer(destination: GLFrameBuffer[T], copyBits: ClearMask): Unit = {
    var bits = copyBits

    if (drawBuffersForTransfer.isEmpty) {
      // set it to max color attachments
      val tempBuf = BufferUtils.newIntBuffer(1)
      Sge().graphics.gl.glGetIntegerv(GL30.GL_MAX_COLOR_ATTACHMENTS, tempBuf)
      drawBuffersForTransfer = Nullable(BufferUtils.newIntBuffer(tempBuf.get(0)))
    }

    if (destination.getWidth() != getWidth() || destination.getHeight() != getHeight()) {
      throw new IllegalArgumentException("source and destination frame buffers must have same size.")
    }

    Sge().graphics.gl.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle)
    Sge().graphics.gl.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.framebufferHandle)

    @scala.annotation.nowarn("msg=deprecated")
    val gl30 = Sge().graphics.gl30.orNull // Nullable -> GL30 at Java interop boundary

    if ((bits.toInt & ClearMask.ColorBufferBit.toInt) == ClearMask.ColorBufferBit.toInt) {
      var totalColorAttachments = 0
      destination.bufferBuilder.textureAttachmentSpecs.foreach { textureAttachmentSpec =>
        if (textureAttachmentSpec.isColorTexture()) {
          totalColorAttachments += 1
        }
      }

      var colorBufferIndex = 0
      val transferBuf      = drawBuffersForTransfer.getOrElse(throw new AssertionError("unreachable"))
      transferBuf.clear()
      destination.bufferBuilder.textureAttachmentSpecs.foreach { attachment =>
        if (attachment.isColorTexture()) {
          gl30.glReadBuffer(GL20.GL_COLOR_ATTACHMENT0 + colorBufferIndex)

          // Webgl doesn't like it when you put a single out of order buffer in for glDrawBuffers
          // Must be sequential.

          // drawBuffers[COLOR0] is ok
          // drawBuffers[COLOR1, COLOR2] is ok
          // drawBuffers[COLOR1] is not ok
          // drawBuffers[COLOR1, COLOR3] is not ok
          // drawBuffers[NONE, NONE, COLOR3] is ok
          var i = 0
          while (i < totalColorAttachments) {
            if (colorBufferIndex == i) {
              transferBuf.put(GL20.GL_COLOR_ATTACHMENT0 + i)
            } else {
              transferBuf.put(GL20.GL_NONE)
            }
            i += 1
          }
          transferBuf.flip()

          gl30.glDrawBuffers(transferBuf.limit(), transferBuf)

          gl30.glBlitFramebuffer(
            0,
            0,
            getWidth().toInt,
            getHeight().toInt,
            0,
            0,
            destination.getWidth().toInt,
            destination.getHeight().toInt,
            bits,
            GL20.GL_NEAREST
          )

          bits = ClearMask.ColorBufferBit
          colorBufferIndex += 1
        }
      }
    }

    // case of depth and/or stencil only
    if (bits.toInt != ClearMask.ColorBufferBit.toInt) {
      gl30.glBlitFramebuffer(
        0,
        0,
        getWidth().toInt,
        getHeight().toInt,
        0,
        0,
        destination.getWidth().toInt,
        destination.getHeight().toInt,
        bits,
        GL20.GL_NEAREST
      )
    }

    // restore draw buffers for destination (in case of MRT only)
    destination.defaultDrawBuffers.foreach { destDrawBuffers =>
      gl30.glDrawBuffers(destDrawBuffers.limit(), destDrawBuffers)
    }

    Sge().graphics.gl.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0)
    Sge().graphics.gl.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0)
  }

  /** @return The OpenGL handle of the framebuffer (see GL20.glGenFramebuffer()) */
  def getFramebufferHandle(): Int = framebufferHandle

  /** @return
    *   The OpenGL handle of the (optional) depth buffer (see GL20.glGenRenderbuffer()). May return 0 even if depth buffer enabled
    */
  def getDepthBufferHandle(): Int = depthbufferHandle

  /** @param n
    *   index of the color buffer as added to the frame buffer builder.
    * @return
    *   The OpenGL handle of a color buffer (see GL20.glGenRenderbuffer()). *
    */
  def getColorBufferHandle(n: Int): Int = colorBufferHandles(n)

  /** @return
    *   The OpenGL handle of the (optional) stencil buffer (see GL20.glGenRenderbuffer()). May return 0 even if stencil buffer enabled
    */
  def getStencilBufferHandle(): Int = stencilbufferHandle

  /** @return The OpenGL handle of the packed depth & stencil buffer (GL_DEPTH24_STENCIL8_OES) or 0 if not used. * */
  protected def getDepthStencilPackedBuffer(): Int = depthStencilPackedBufferHandle

  /** @return the height of the framebuffer in pixels */
  def getHeight(): Pixels = bufferBuilder.height

  /** @return the width of the framebuffer in pixels */
  def getWidth(): Pixels = bufferBuilder.width
}

object GLFrameBuffer {

  /** the frame buffers * */
  private val buffers: mutable.Map[Application, DynamicArray[GLFrameBuffer[?]]] = mutable.Map[Application, DynamicArray[GLFrameBuffer[?]]]()

  /** the default framebuffer handle, a.k.a screen. */
  private var defaultFramebufferHandle: Int = scala.compiletime.uninitialized

  /** true if we have polled for the default handle already. */
  private var defaultFramebufferHandleInitialized: Boolean = false

  /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
  def unbind()(using Sge): Unit =
    Sge().graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)

  private def addManagedFrameBuffer(app: Application, frameBuffer: GLFrameBuffer[?]): Unit = {
    val managedResources = buffers.getOrElse(app, DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[GLFrameBuffer[?]]], 16, true))
    managedResources.add(frameBuffer)
    buffers.put(app, managedResources)
  }

  /** Invalidates all frame buffers. This can be used when the OpenGL context is lost to rebuild all managed frame buffers. This assumes that the texture attached to this buffer has already been
    * rebuild! Use with care.
    */
  def invalidateAllFrameBuffers(app: Application)(using Sge): Unit = {
    val bufferArray = buffers.get(app)
    if (bufferArray.isDefined) {
      for (buffer <- bufferArray.get)
        buffer.build()
    }
  }

  def clearAllFrameBuffers(app: Application): Unit =
    buffers.remove(app)

  def getManagedStatus(builder: StringBuilder): StringBuilder = {
    builder.append("Managed buffers/app: { ")
    for (app <- buffers.keys) {
      builder.append(buffers(app).size)
      builder.append(" ")
    }
    builder.append("}")
    builder
  }

  def getManagedStatus(): String =
    getManagedStatus(new StringBuilder()).toString()

  class FrameBufferTextureAttachmentSpec(val internalFormat: Int, val format: PixelFormat, val `type`: DataType) {
    var isFloat:   Boolean = false
    var isGpuOnly: Boolean = false
    var isDepth:   Boolean = false
    var isStencil: Boolean = false

    def isColorTexture(): Boolean = !isDepth && !isStencil
  }

  class FrameBufferRenderBufferAttachmentSpec(val internalFormat: Int)

  abstract class GLFrameBufferBuilder[U <: GLFrameBuffer[? <: GLTexture]](val width: Pixels, val height: Pixels, val samples: Int = 0)(using Sge) {

    private[glutils] val textureAttachmentSpecs: DynamicArray[FrameBufferTextureAttachmentSpec]      = DynamicArray[FrameBufferTextureAttachmentSpec]()
    private[glutils] val colorRenderBufferSpecs: DynamicArray[FrameBufferRenderBufferAttachmentSpec] = DynamicArray[FrameBufferRenderBufferAttachmentSpec]()

    private[glutils] var stencilRenderBufferSpec:            FrameBufferRenderBufferAttachmentSpec = scala.compiletime.uninitialized
    private[glutils] var depthRenderBufferSpec:              FrameBufferRenderBufferAttachmentSpec = scala.compiletime.uninitialized
    private[glutils] var packedStencilDepthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec = scala.compiletime.uninitialized

    private[glutils] var hasStencilRenderBuffer:            Boolean = false
    private[glutils] var hasDepthRenderBuffer:              Boolean = false
    private[glutils] var hasPackedStencilDepthRenderBuffer: Boolean = false

    def this(width: Pixels, height: Pixels)(using Sge) = this(width, height, 0)

    def addColorTextureAttachment(internalFormat: Int, format: PixelFormat, `type`: DataType): GLFrameBufferBuilder[U] = {
      textureAttachmentSpecs.add(FrameBufferTextureAttachmentSpec(internalFormat, format, `type`))
      this
    }

    def addBasicColorTextureAttachment(format: Pixmap.Format): GLFrameBufferBuilder[U] = {
      val glFormat = Pixmap.Format.toGlFormat(format)
      val glType   = Pixmap.Format.toGlType(format)
      addColorTextureAttachment(glFormat, PixelFormat(glFormat), DataType(glType))
    }

    def addFloatAttachment(internalFormat: Int, format: PixelFormat, `type`: DataType, gpuOnly: Boolean): GLFrameBufferBuilder[U] = {
      val spec = FrameBufferTextureAttachmentSpec(internalFormat, format, `type`)
      spec.isFloat = true
      spec.isGpuOnly = gpuOnly
      textureAttachmentSpecs.add(spec)
      this
    }

    def addDepthTextureAttachment(internalFormat: Int, `type`: DataType): GLFrameBufferBuilder[U] = {
      val spec = FrameBufferTextureAttachmentSpec(internalFormat, PixelFormat.DepthComponent, `type`)
      spec.isDepth = true
      textureAttachmentSpecs.add(spec)
      this
    }

    def addStencilTextureAttachment(internalFormat: Int, `type`: DataType): GLFrameBufferBuilder[U] = {
      val spec = FrameBufferTextureAttachmentSpec(internalFormat, PixelFormat(GL20.GL_STENCIL_ATTACHMENT), `type`)
      spec.isStencil = true
      textureAttachmentSpecs.add(spec)
      this
    }

    def addDepthRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      depthRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
      hasDepthRenderBuffer = true
      this
    }

    def addColorRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      colorRenderBufferSpecs.add(FrameBufferRenderBufferAttachmentSpec(internalFormat))
      this
    }

    def addStencilRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      stencilRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
      hasStencilRenderBuffer = true
      this
    }

    def addStencilDepthPackedRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      packedStencilDepthRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
      hasPackedStencilDepthRenderBuffer = true
      this
    }

    def addBasicDepthRenderBuffer(): GLFrameBufferBuilder[U] =
      addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16)

    def addBasicStencilRenderBuffer(): GLFrameBufferBuilder[U] =
      addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8)

    def addBasicStencilDepthPackedRenderBuffer(): GLFrameBufferBuilder[U] =
      addStencilDepthPackedRenderBuffer(GL30.GL_DEPTH24_STENCIL8)

    def build(): U
  }

  class FrameBufferBuilder(width: Pixels, height: Pixels, samples: Int = 0)(using Sge) extends GLFrameBufferBuilder[FrameBuffer](width, height, samples) {
    def this(width: Pixels, height: Pixels)(using Sge) = this(width, height, 0)

    override def build(): FrameBuffer =
      FrameBuffer(this.asInstanceOf[GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Texture]]])
  }

  class FloatFrameBufferBuilder(width: Pixels, height: Pixels, samples: Int = 0)(using Sge) extends GLFrameBufferBuilder[FloatFrameBuffer](width, height, samples) {
    def this(width: Pixels, height: Pixels)(using Sge) = this(width, height, 0)

    override def build(): FloatFrameBuffer =
      FloatFrameBuffer(this)
  }

  class FrameBufferCubemapBuilder(width: Pixels, height: Pixels, samples: Int = 0)(using Sge) extends GLFrameBufferBuilder[FrameBufferCubemap](width, height, samples) {
    def this(width: Pixels, height: Pixels)(using Sge) = this(width, height, 0)

    override def build(): FrameBufferCubemap =
      FrameBufferCubemap(this.asInstanceOf[GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Cubemap]]])
  }
}
