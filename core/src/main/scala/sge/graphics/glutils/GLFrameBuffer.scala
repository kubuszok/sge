package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.compiletime.uninitialized

import sge.Application
import sge.Application.ApplicationType
import sge.utils.BufferUtils
import sge.utils.SgeError
import sge.graphics.GL20
import sge.graphics.GL30
import sge.graphics.GL31
import sge.graphics.GLTexture
import sge.graphics.Pixmap
import sge.Sge

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
abstract class GLFrameBuffer[T <: GLTexture](using sge: Sge) extends AutoCloseable {

  /** the color buffer texture * */
  protected val textureAttachments: ArrayBuffer[T] = ArrayBuffer[T]()

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
  protected val colorBufferHandles: ArrayBuffer[Int] = ArrayBuffer[Int]()

  /** if multiple texture attachments are present * */
  protected var isMRT: Boolean = scala.compiletime.uninitialized

  protected var bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[T]] = scala.compiletime.uninitialized

  private var defaultDrawBuffers: IntBuffer = scala.compiletime.uninitialized

  /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder * */
  protected def this(bufferBuilder: GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[T]])(using sge: Sge) = {
    this()
    this.bufferBuilder = bufferBuilder
    build()
  }

  /** Convenience method to return the first Texture attachment present in the fbo * */
  def getColorBufferTexture(): T =
    textureAttachments.head

  /** Return the Texture attachments attached to the fbo * */
  def getTextureAttachments(): ArrayBuffer[T] =
    textureAttachments

  /** Override this method in a derived class to set up the backing texture as you like. */
  protected def createTexture(attachmentSpec: GLFrameBuffer.FrameBufferTextureAttachmentSpec): T

  /** Override this method in a derived class to dispose the backing texture as you like. */
  protected def disposeColorTexture(colorTexture: T): Unit

  /** Override this method in a derived class to attach the backing texture to the GL framebuffer object. */
  protected def attachFrameBufferColorTexture(texture: T): Unit

  protected def build(): Unit =
    // TODO: Convert the complex build method implementation
    throw SgeError.GraphicsError("GLFrameBuffer.build() implementation not yet converted to Scala")

  private def checkValidBuilder(): Unit =
    // TODO: Convert the checkValidBuilder method implementation
    throw SgeError.GraphicsError("GLFrameBuffer.checkValidBuilder() implementation not yet converted to Scala")

  /** Releases all resources associated with the FrameBuffer. */
  override def close(): Unit =
    // TODO: Convert the dispose method implementation
    throw SgeError.GraphicsError("GLFrameBuffer.close() implementation not yet converted to Scala")

  /** Makes the frame buffer current so everything gets drawn to it. */
  def bind(): Unit =
    sge.graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)

  /** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it. */
  def begin(): Unit = {
    bind()
    setFrameBufferViewport()
  }

  /** Sets viewport to the dimensions of framebuffer. Called by begin(). */
  protected def setFrameBufferViewport(): Unit =
    sge.graphics.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height)

  /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
  def end(): Unit =
    end(0, 0, sge.graphics.getBackBufferWidth(), sge.graphics.getBackBufferHeight())

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
  def end(x: Int, y: Int, width: Int, height: Int): Unit = {
    GLFrameBuffer.unbind()
    sge.graphics.gl20.glViewport(x, y, width, height)
  }

  /** Transfer pixels from this frame buffer to the destination frame buffer. Usually used when using multisample, it resolves samples from this multisample FBO to a non-multisample as destination in
    * order to be used as textures. This is a convenient method that automatically choose which of stencil, depth, and colors buffers attachment to be copied.
    * @param destination
    *   the destination of the copy.
    */
  def transfer(destination: GLFrameBuffer[T]): Unit =
    // TODO: Convert the transfer method implementation
    throw SgeError.GraphicsError("GLFrameBuffer.transfer() implementation not yet converted to Scala")

  /** Transfer pixels from this frame buffer to the destination frame buffer. Usually used when using multisample, it resolves samples from this multisample FBO to a non-multisample as destination in
    * order to be used as textures.
    * @param destination
    *   the destination of the copy (should be same size as this frame buffer).
    * @param copyBits
    *   combination of GL20.GL_COLOR_BUFFER_BIT, GL20.GL_STENCIL_BUFFER_BIT, and GL20.GL_DEPTH_BUFFER_BIT. When GL20.GL_COLOR_BUFFER_BIT is present, every color buffers will be copied to each
    *   corresponding color texture buffers in the destination framebuffer.
    */
  def transfer(destination: GLFrameBuffer[T], copyBits: Int): Unit =
    // TODO: Convert the transfer method implementation
    throw SgeError.GraphicsError("GLFrameBuffer.transfer() implementation not yet converted to Scala")

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
  def getHeight(): Int = bufferBuilder.height

  /** @return the width of the framebuffer in pixels */
  def getWidth(): Int = bufferBuilder.width
}

object GLFrameBuffer {

  /** the frame buffers * */
  private val buffers: mutable.Map[Application, ArrayBuffer[GLFrameBuffer[?]]] = mutable.Map[Application, ArrayBuffer[GLFrameBuffer[?]]]()

  private val GL_DEPTH24_STENCIL8_OES: Int = 0x88f0

  /** the default framebuffer handle, a.k.a screen. */
  private var defaultFramebufferHandle: Int = scala.compiletime.uninitialized

  /** true if we have polled for the default handle already. */
  private var defaultFramebufferHandleInitialized: Boolean = false

  private val singleInt: IntBuffer = BufferUtils.newIntBuffer(1)

  /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
  def unbind()(using sge: Sge): Unit =
    sge.graphics.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)

  private def addManagedFrameBuffer(app: Application, frameBuffer: GLFrameBuffer[?]): Unit = {
    val managedResources = buffers.getOrElseUpdate(app, ArrayBuffer[GLFrameBuffer[?]]())
    managedResources += frameBuffer
  }

  /** Invalidates all frame buffers. This can be used when the OpenGL context is lost to rebuild all managed frame buffers. This assumes that the texture attached to this buffer has already been
    * rebuild! Use with care.
    */
  def invalidateAllFrameBuffers(app: Application)(using sge: Sge): Unit = {
    if (sge.graphics.gl20 == null) return

    val bufferArray = buffers.get(app)
    if (bufferArray.isEmpty) return
    for (buffer <- bufferArray.get)
      buffer.build()
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

  class FrameBufferTextureAttachmentSpec(val internalFormat: Int, val format: Int, val `type`: Int) {
    var isFloat:   Boolean = false
    var isGpuOnly: Boolean = false
    var isDepth:   Boolean = false
    var isStencil: Boolean = false

    def isColorTexture(): Boolean = !isDepth && !isStencil
  }

  class FrameBufferRenderBufferAttachmentSpec(val internalFormat: Int)

  abstract class GLFrameBufferBuilder[U <: GLFrameBuffer[? <: GLTexture]](val width: Int, val height: Int, val samples: Int = 0) {

    protected val textureAttachmentSpecs: ArrayBuffer[FrameBufferTextureAttachmentSpec]      = ArrayBuffer[FrameBufferTextureAttachmentSpec]()
    protected val colorRenderBufferSpecs: ArrayBuffer[FrameBufferRenderBufferAttachmentSpec] = ArrayBuffer[FrameBufferRenderBufferAttachmentSpec]()

    protected var stencilRenderBufferSpec:            FrameBufferRenderBufferAttachmentSpec = scala.compiletime.uninitialized
    protected var depthRenderBufferSpec:              FrameBufferRenderBufferAttachmentSpec = scala.compiletime.uninitialized
    protected var packedStencilDepthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec = scala.compiletime.uninitialized

    protected var hasStencilRenderBuffer:            Boolean = false
    protected var hasDepthRenderBuffer:              Boolean = false
    protected var hasPackedStencilDepthRenderBuffer: Boolean = false

    def this(width: Int, height: Int) = this(width, height, 0)

    def addColorTextureAttachment(internalFormat: Int, format: Int, `type`: Int): GLFrameBufferBuilder[U] = {
      textureAttachmentSpecs += new FrameBufferTextureAttachmentSpec(internalFormat, format, `type`)
      this
    }

    def addBasicColorTextureAttachment(format: Pixmap.Format): GLFrameBufferBuilder[U] = {
      val glFormat = Pixmap.Format.toGlFormat(format)
      val glType   = Pixmap.Format.toGlType(format)
      addColorTextureAttachment(glFormat, glFormat, glType)
    }

    def addFloatAttachment(internalFormat: Int, format: Int, `type`: Int, gpuOnly: Boolean): GLFrameBufferBuilder[U] = {
      val spec = new FrameBufferTextureAttachmentSpec(internalFormat, format, `type`)
      spec.isFloat = true
      spec.isGpuOnly = gpuOnly
      textureAttachmentSpecs += spec
      this
    }

    def addDepthTextureAttachment(internalFormat: Int, `type`: Int): GLFrameBufferBuilder[U] = {
      val spec = new FrameBufferTextureAttachmentSpec(internalFormat, GL20.GL_DEPTH_COMPONENT, `type`)
      spec.isDepth = true
      textureAttachmentSpecs += spec
      this
    }

    def addStencilTextureAttachment(internalFormat: Int, `type`: Int): GLFrameBufferBuilder[U] = {
      val spec = new FrameBufferTextureAttachmentSpec(internalFormat, GL20.GL_STENCIL_ATTACHMENT, `type`)
      spec.isStencil = true
      textureAttachmentSpecs += spec
      this
    }

    def addDepthRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      depthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat)
      hasDepthRenderBuffer = true
      this
    }

    def addColorRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      colorRenderBufferSpecs += new FrameBufferRenderBufferAttachmentSpec(internalFormat)
      this
    }

    def addStencilRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      stencilRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat)
      hasStencilRenderBuffer = true
      this
    }

    def addStencilDepthPackedRenderBuffer(internalFormat: Int): GLFrameBufferBuilder[U] = {
      packedStencilDepthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat)
      hasPackedStencilDepthRenderBuffer = true
      this
    }

    def addBasicDepthRenderBuffer(): GLFrameBufferBuilder[U] =
      addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16)

    def addBasicStencilRenderBuffer(): GLFrameBufferBuilder[U] =
      addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8)

    def addBasicStencilDepthPackedRenderBuffer(): GLFrameBufferBuilder[U] =
      addStencilDepthPackedRenderBuffer(GL30.GL_DEPTH24_STENCIL8)

    def build()(using sge: Sge): U
  }

  class FrameBufferBuilder(width: Int, height: Int, samples: Int = 0) extends GLFrameBufferBuilder[FrameBuffer](width, height, samples) {
    def this(width: Int, height: Int) = this(width, height, 0)

    override def build()(using sge: Sge): FrameBuffer =
      new FrameBuffer(this.asInstanceOf[GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Texture]]])
  }

  class FloatFrameBufferBuilder(width: Int, height: Int, samples: Int = 0) extends GLFrameBufferBuilder[FloatFrameBuffer](width, height, samples) {
    def this(width: Int, height: Int) = this(width, height, 0)

    override def build()(using sge: Sge): FloatFrameBuffer =
      new FloatFrameBuffer(this)
  }

  class FrameBufferCubemapBuilder(width: Int, height: Int, samples: Int = 0) extends GLFrameBufferBuilder[FrameBufferCubemap](width, height, samples) {
    def this(width: Int, height: Int) = this(width, height, 0)

    override def build()(using sge: Sge): FrameBufferCubemap =
      new FrameBufferCubemap(this.asInstanceOf[GLFrameBuffer.GLFrameBufferBuilder[? <: GLFrameBuffer[Cubemap]]])
  }
}
