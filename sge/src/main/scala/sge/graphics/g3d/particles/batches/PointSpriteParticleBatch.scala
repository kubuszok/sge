/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/PointSpriteParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - ParticleShader integrated: shader initialized in constructor with Config or default Point type
 * - Fixes (2026-03-06): shaderConfig Nullable[AnyRef] → Nullable[ParticleShader.Config]; shader init enabled
 * - Constructor chain: Java 4 constructors → Scala primary + 2 secondary; (using Sge) added
 * - Gdx.gl/Gdx.app → Sge().graphics.gl/Sge().application via (using Sge)
 * - null fields → Nullable wrapping (getTexture returns Nullable[Texture], setTexture
 *   navigates Nullable material/attribute chain)
 * - dispose() → close() (Disposable → AutoCloseable)
 * - FloatChannel.data[] → floatData() (field rename in ParallelArray port)
 * - findByUsage(usage).offset/4 → getOffset(usage) (helper method in SGE VertexAttributes)
 * - Static fields → companion object vals; static method enablePointSprites → private def
 * - load(): Java returns null check inline; Scala uses nested foreach/fold
 * - save(): Java getTexture() inline; Scala getTexture().foreach
 * - All public methods faithfully ported: texture (property), blendingAttribute,
 *   flush, getRenderables, save, load, allocParticlesData, allocRenderable
 * - Fixes (2026-03-04): setTexture/getTexture → texture/texture_=;
 *   getBlendingAttribute → blendingAttribute
 * - Audit: pass (2026-03-03)
 * Convention: typed GL enums (EnableCap)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 211
 * Covenant-baseline-methods: CPU_ATTRIBUTES,CPU_COLOR_OFFSET,CPU_POSITION_OFFSET,CPU_REGION_OFFSET,CPU_SIZE_AND_ROTATION_OFFSET,CPU_VERTEX_SIZE,PointSpriteParticleBatch,TMP_V1,_blendingAttribute,_depthTestAttribute,allocParticlesData,allocRenderable,blendingAttribute,cfg,data,enablePointSprites,flush,getRenderables,load,pointSpritesEnabled,renderable,save,shader,sizeAndRotationUsage,texture,texture_,this,tp,vertices
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/batches/PointSpriteParticleBatch.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 34cc595deb4ac09ee476c6b1aba1b805f4dc81a7
 */
package sge.graphics.g3d.particles.batches

import sge.Application
import sge.Sge
import sge.graphics.EnableCap
import sge.graphics.GL20
import sge.graphics.Mesh
import sge.graphics.PrimitiveMode
import sge.graphics.Texture
import sge.graphics.VertexAttribute
import sge.graphics.VertexAttributes
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.Material
import sge.graphics.g3d.Renderable
import sge.graphics.g3d.attributes.BlendingAttribute
import sge.graphics.g3d.attributes.DepthTestAttribute
import sge.graphics.g3d.attributes.TextureAttribute
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleShader
import sge.graphics.g3d.particles.ResourceData
import sge.graphics.g3d.particles.renderers.PointSpriteControllerRenderData
import sge.graphics.glutils.ShaderProgram
import sge.math.Vector3
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.Pool

/** This class is used to draw particles as point sprites.
  * @author
  *   Inferno
  */
class PointSpriteParticleBatch(
  capacity:            Int,
  shaderConfig:        Nullable[ParticleShader.Config],
  initialBlendingAttr: Nullable[BlendingAttribute],
  depthTestAttribute:  Nullable[DepthTestAttribute]
)(using Sge)
    extends BufferedParticleBatch[PointSpriteControllerRenderData] {

  import PointSpriteParticleBatch.*

  if (!pointSpritesEnabled) enablePointSprites()

  private var vertices:             Array[Float]      = scala.compiletime.uninitialized
  var renderable:                   Renderable        = scala.compiletime.uninitialized
  protected var _blendingAttribute: BlendingAttribute =
    initialBlendingAttr.getOrElse(BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f))
  protected var _depthTestAttribute: DepthTestAttribute =
    depthTestAttribute.getOrElse(DepthTestAttribute(GL20.GL_LEQUAL, false))

  allocRenderable()
  ensureCapacity(capacity)
  locally {
    val cfg    = shaderConfig.getOrElse(ParticleShader.Config(ParticleShader.ParticleType.Point))
    val shader = ParticleShader(renderable, cfg)
    shader.init()
    renderable.shader = Nullable(shader)
  }

  def this()(using Sge) =
    this(1000, Nullable.empty, Nullable.empty, Nullable.empty)

  def this(capacity: Int)(using Sge) =
    this(capacity, Nullable.empty, Nullable.empty, Nullable.empty)

  override protected def allocParticlesData(capacity: Int): Unit = {
    vertices = new Array[Float](capacity * CPU_VERTEX_SIZE)
    Nullable(renderable.meshPart.mesh).foreach(_.close())
    renderable.meshPart.mesh = Mesh(false, capacity, 0, CPU_ATTRIBUTES)
  }

  protected def allocRenderable(): Unit = {
    renderable = Renderable()
    renderable.meshPart.primitiveType = PrimitiveMode.Points
    renderable.meshPart.offset = 0
    renderable.material = Nullable(Material(_blendingAttribute, _depthTestAttribute, TextureAttribute(TextureAttribute.Diffuse)))
  }

  def texture: Nullable[Texture] =
    renderable.material.flatMap(_.get(TextureAttribute.Diffuse)).flatMap(attr => attr.asInstanceOf[TextureAttribute].textureDescription.texture)

  def texture_=(texture: Texture): Unit =
    renderable.material.foreach { mat =>
      mat.get(TextureAttribute.Diffuse).foreach { attr =>
        attr.asInstanceOf[TextureAttribute].textureDescription.texture = Nullable(texture)
      }
    }

  def blendingAttribute: BlendingAttribute =
    _blendingAttribute

  override protected def flush(offsets: Array[Int]): Unit = {
    var tp = 0
    for (data <- renderData) {
      val scaleChannel    = data.scaleChannel
      val regionChannel   = data.regionChannel
      val positionChannel = data.positionChannel
      val colorChannel    = data.colorChannel
      val rotationChannel = data.rotationChannel

      var p = 0
      while (p < data.controller.particles.size) {
        val offset         = offsets(tp) * CPU_VERTEX_SIZE
        val regionOffset   = p * regionChannel.strideSize
        val positionOffset = p * positionChannel.strideSize
        val colorOffset    = p * colorChannel.strideSize
        val rotationOffset = p * rotationChannel.strideSize

        vertices(offset + CPU_POSITION_OFFSET) = positionChannel.floatData(positionOffset + ParticleChannels.XOffset)
        vertices(offset + CPU_POSITION_OFFSET + 1) = positionChannel.floatData(positionOffset + ParticleChannels.YOffset)
        vertices(offset + CPU_POSITION_OFFSET + 2) = positionChannel.floatData(positionOffset + ParticleChannels.ZOffset)
        vertices(offset + CPU_COLOR_OFFSET) = colorChannel.floatData(colorOffset + ParticleChannels.RedOffset)
        vertices(offset + CPU_COLOR_OFFSET + 1) = colorChannel.floatData(colorOffset + ParticleChannels.GreenOffset)
        vertices(offset + CPU_COLOR_OFFSET + 2) = colorChannel.floatData(colorOffset + ParticleChannels.BlueOffset)
        vertices(offset + CPU_COLOR_OFFSET + 3) = colorChannel.floatData(colorOffset + ParticleChannels.AlphaOffset)
        vertices(offset + CPU_SIZE_AND_ROTATION_OFFSET) = scaleChannel.floatData(p * scaleChannel.strideSize)
        vertices(offset + CPU_SIZE_AND_ROTATION_OFFSET + 1) = rotationChannel.floatData(
          rotationOffset
            + ParticleChannels.CosineOffset
        )
        vertices(offset + CPU_SIZE_AND_ROTATION_OFFSET + 2) = rotationChannel.floatData(
          rotationOffset
            + ParticleChannels.SineOffset
        )
        vertices(offset + CPU_REGION_OFFSET) = regionChannel.floatData(regionOffset + ParticleChannels.UOffset)
        vertices(offset + CPU_REGION_OFFSET + 1) = regionChannel.floatData(regionOffset + ParticleChannels.VOffset)
        vertices(offset + CPU_REGION_OFFSET + 2) = regionChannel.floatData(regionOffset + ParticleChannels.U2Offset)
        vertices(offset + CPU_REGION_OFFSET + 3) = regionChannel.floatData(regionOffset + ParticleChannels.V2Offset)

        p += 1
        tp += 1
      }
    }

    renderable.meshPart.size = bufferedParticlesCount
    renderable.meshPart.mesh.setVertices(vertices, 0, bufferedParticlesCount * CPU_VERTEX_SIZE)
    renderable.meshPart.update()
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    if (bufferedParticlesCount > 0) renderables.add(pool.obtain().set(renderable))

  override def save(manager: sge.assets.AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.createSaveData("pointSpriteBatch")
    texture.foreach { tex =>
      data.saveAsset[Texture](manager.assetFileName(tex).getOrElse(""))
    }
  }

  override def load(manager: sge.assets.AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.getSaveData("pointSpriteBatch")
    data.foreach { d =>
      d.loadAsset().foreach { asset =>
        this.texture = manager(asset.fileName, asset.`type`).asInstanceOf[Texture]
      }
    }
  }
}

object PointSpriteParticleBatch {
  private var pointSpritesEnabled:    Boolean          = false
  protected val TMP_V1:               Vector3          = Vector3()
  protected val sizeAndRotationUsage: Int              = 1 << 9
  protected val CPU_ATTRIBUTES:       VertexAttributes = VertexAttributes(
    VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
    VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
    VertexAttribute(Usage.TextureCoordinates, 4, "a_region"),
    VertexAttribute(sizeAndRotationUsage, 3, "a_sizeAndRotation")
  )
  protected val CPU_VERTEX_SIZE:              Int = CPU_ATTRIBUTES.vertexSize / 4
  protected val CPU_POSITION_OFFSET:          Int = CPU_ATTRIBUTES.offset(Usage.Position)
  protected val CPU_COLOR_OFFSET:             Int = CPU_ATTRIBUTES.offset(Usage.ColorUnpacked)
  protected val CPU_REGION_OFFSET:            Int = CPU_ATTRIBUTES.offset(Usage.TextureCoordinates)
  protected val CPU_SIZE_AND_ROTATION_OFFSET: Int = CPU_ATTRIBUTES.offset(sizeAndRotationUsage)

  private def enablePointSprites()(using Sge): Unit = {
    Sge().graphics.gl.glEnable(EnableCap.VertexProgramPointSize)
    if (Sge().application.applicationType == Application.ApplicationType.Desktop) {
      Sge().graphics.gl.glEnable(EnableCap(0x8861)) // GL_POINT_OES
    }
    pointSpritesEnabled = true
  }
}
