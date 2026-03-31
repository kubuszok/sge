/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/TransmissionSource.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Transmission source renders non-transmitting material objects into a framebuffer
 * for high quality refraction effects.
 */
package sge
package gltf
package scene3d
package scene

import sge.{ Pixels, Sge }
import sge.gltf.scene3d.attributes.{ PBRFloatAttribute, PBRTextureAttribute }
import sge.graphics.{ Camera, ClearMask, Pixmap, Texture, TextureTarget }
import sge.graphics.g3d.{ Environment, ModelBatch, Renderable, RenderableProvider }
import sge.graphics.g3d.utils.{ RenderableSorter, ShaderProvider }
import sge.graphics.glutils.FrameBuffer
import sge.utils.{ DynamicArray, Nullable, Pool }

class TransmissionSource(shaderProvider: ShaderProvider, sorter: RenderableSorter)(using Sge) extends AutoCloseable {

  def this(shaderProvider: ShaderProvider)(using Sge) =
    this(shaderProvider, SceneRenderableSorter())

  private val batch: ModelBatch = ModelBatch(shaderProvider, sorter)
  private var fbo:   FrameBuffer = scala.compiletime.uninitialized
  private var width:  Int     = 0
  private var height: Int     = 0
  private var hasTransmission: Boolean = false
  private var camera: Camera  = scala.compiletime.uninitialized

  val attribute: PBRTextureAttribute = PBRTextureAttribute(PBRTextureAttribute.TransmissionSourceTexture)

  private val allRenderables:      DynamicArray[Renderable] = DynamicArray[Renderable]()
  private val selectedRenderables: DynamicArray[Renderable] = DynamicArray[Renderable]()
  private val renderablePool: Pool[Renderable] = new Pool[Renderable] {
    override protected val initialCapacity: Int = 16
    override protected val max: Int = Int.MaxValue
    override protected def newObject(): Renderable = Renderable()
  }

  attribute.textureDescription.minFilter = Texture.TextureFilter.MipMap
  attribute.textureDescription.magFilter = Texture.TextureFilter.Linear

  protected def createFrameBuffer(width: Int, height: Int): FrameBuffer =
    FrameBuffer(Pixmap.Format.RGBA8888, Pixels(width), Pixels(height), true)

  def setSize(width: Int, height: Int): Unit = {
    this.width = width
    this.height = height
  }

  def begin(camera: Camera)(using sge: Sge): Unit = {
    this.camera = camera
    ensureFrameBufferSize(width, height)
    this.hasTransmission = false
  }

  def render(providers: Iterable[RenderableProvider], environment: Environment): Unit = {
    providers.foreach(render(_, environment))
  }

  def render(provider: RenderableProvider, environment: Environment): Unit = {
    val start = allRenderables.size
    provider.getRenderables(allRenderables, renderablePool)
    var i = start
    while (i < allRenderables.size) {
      val renderable = allRenderables(i)
      if (shouldBeRendered(renderable)) {
        renderable.environment = Nullable(environment)
        selectedRenderables.add(renderable)
      }
      i += 1
    }
  }

  def render(provider: RenderableProvider): Unit = {
    val start = allRenderables.size
    provider.getRenderables(allRenderables, renderablePool)
    var i = start
    while (i < allRenderables.size) {
      val renderable = allRenderables(i)
      if (shouldBeRendered(renderable)) {
        selectedRenderables.add(renderable)
      }
      i += 1
    }
  }

  def end()(using sge: Sge): Unit = {
    if (hasTransmission) {
      fbo.begin()
      sge.graphics.gl.glClearColor(0f, 0f, 0f, 0f)
      sge.graphics.gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
      batch.begin(camera)
      var i = 0
      while (i < selectedRenderables.size) {
        batch.render(selectedRenderables(i))
        i += 1
      }
      batch.end()
      fbo.end()

      val texture = fbo.colorBufferTexture
      texture.bind()
      sge.graphics.gl.glGenerateMipmap(TextureTarget.Texture2D)
    }
    attribute.textureDescription.texture = Nullable(fbo.colorBufferTexture)
    renderablePool.clear()
    selectedRenderables.clear()
    allRenderables.clear()
  }

  private def shouldBeRendered(renderable: Renderable): Boolean = {
    val mat = renderable.material
    val hasTransmission = mat.exists(_.has(PBRTextureAttribute.TransmissionTexture)) ||
      (mat.exists(_.has(PBRFloatAttribute.TransmissionFactor)) &&
        mat.flatMap(_.getAs[PBRFloatAttribute](PBRFloatAttribute.TransmissionFactor)).exists(_.value > 0f))
    this.hasTransmission |= hasTransmission
    !hasTransmission
  }

  private def ensureFrameBufferSize(width: Int, height: Int)(using sge: Sge): Unit = {
    val w = if (width <= 0) sge.graphics.backBufferWidth.toInt else width
    val h = if (height <= 0) sge.graphics.backBufferHeight.toInt else height
    if (fbo == null || fbo.width.toInt != w || fbo.height.toInt != h) { // @nowarn
      if (fbo != null) fbo.close() // @nowarn
      fbo = createFrameBuffer(w, h)
    }
  }

  override def close(): Unit = {
    if (fbo != null) fbo.close() // @nowarn
    batch.close()
  }
}
