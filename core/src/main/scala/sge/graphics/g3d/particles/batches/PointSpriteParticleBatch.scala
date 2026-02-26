/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/PointSpriteParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package batches

import scala.collection.mutable.ArrayBuffer

import sge.graphics.GL20
import sge.graphics.Mesh
import sge.graphics.Texture
import sge.graphics.VertexAttribute
import sge.graphics.VertexAttributes
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.Material
import sge.graphics.g3d.Renderable
import sge.graphics.g3d.attributes.BlendingAttribute
import sge.graphics.g3d.attributes.DepthTestAttribute
import sge.graphics.g3d.attributes.TextureAttribute
import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ResourceData
import sge.graphics.g3d.particles.ResourceData.SaveData
import sge.graphics.g3d.particles.renderers.PointSpriteControllerRenderData
import sge.graphics.glutils.ShaderProgram
import sge.math.Vector3
import sge.utils.Nullable
import sge.utils.Pool

/** This class is used to draw particles as point sprites.
  * @author
  *   Inferno
  */
class PointSpriteParticleBatch(
  capacity:           Int,
  shaderConfig:       Nullable[AnyRef], // ParticleShader.Config - not yet ported
  blendingAttribute:  Nullable[BlendingAttribute],
  depthTestAttribute: Nullable[DepthTestAttribute]
)(using sge: Sge)
    extends BufferedParticleBatch[PointSpriteControllerRenderData] {

  import PointSpriteParticleBatch.*

  if (!pointSpritesEnabled) enablePointSprites()

  private var vertices:             Array[Float]      = scala.compiletime.uninitialized
  var renderable:                   Renderable        = scala.compiletime.uninitialized
  protected var _blendingAttribute: BlendingAttribute =
    blendingAttribute.getOrElse(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f))
  protected var _depthTestAttribute: DepthTestAttribute =
    depthTestAttribute.getOrElse(new DepthTestAttribute(GL20.GL_LEQUAL, false))

  allocRenderable()
  ensureCapacity(capacity)
  // ParticleShader is not yet ported; shader initialization deferred
  // renderable.shader = new ParticleShader(renderable, shaderConfig)
  // renderable.shader.foreach(_.init())

  def this()(using sge: Sge) =
    this(1000, Nullable.empty, Nullable.empty, Nullable.empty)

  def this(capacity: Int)(using sge: Sge) =
    this(capacity, Nullable.empty, Nullable.empty, Nullable.empty)

  override protected def allocParticlesData(capacity: Int): Unit = {
    vertices = new Array[Float](capacity * CPU_VERTEX_SIZE)
    if (renderable.meshPart.mesh != null) renderable.meshPart.mesh.close()
    renderable.meshPart.mesh = new Mesh(false, capacity, 0, CPU_ATTRIBUTES)
  }

  protected def allocRenderable(): Unit = {
    renderable = new Renderable()
    renderable.meshPart.primitiveType = GL20.GL_POINTS
    renderable.meshPart.offset = 0
    renderable.material = Nullable(new Material(_blendingAttribute, _depthTestAttribute, new TextureAttribute(TextureAttribute.Diffuse)))
  }

  def setTexture(texture: Texture): Unit =
    renderable.material.foreach { mat =>
      mat.get(TextureAttribute.Diffuse).foreach { attr =>
        attr.asInstanceOf[TextureAttribute].textureDescription.texture = Nullable(texture)
      }
    }

  def getTexture(): Nullable[Texture] =
    renderable.material.fold(Nullable.empty[Texture]) { mat =>
      mat.get(TextureAttribute.Diffuse).fold(Nullable.empty[Texture]) { attr =>
        attr.asInstanceOf[TextureAttribute].textureDescription.texture
      }
    }

  def getBlendingAttribute(): BlendingAttribute =
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

  override def getRenderables(renderables: ArrayBuffer[Renderable], pool: Pool[Renderable]): Unit =
    if (bufferedParticlesCount > 0) renderables += pool.obtain().set(renderable)

  override def save(manager: _root_.sge.assets.AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.createSaveData("pointSpriteBatch")
    getTexture().foreach { tex =>
      data.saveAsset(manager.getAssetFileName(tex), classOf[Texture])
    }
  }

  override def load(manager: _root_.sge.assets.AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.getSaveData("pointSpriteBatch")
    data.foreach { d =>
      d.loadAsset().foreach { asset =>
        setTexture(manager.get(asset.fileName, asset.`type`).asInstanceOf[Texture])
      }
    }
  }
}

object PointSpriteParticleBatch {
  private var pointSpritesEnabled:    Boolean          = false
  protected val TMP_V1:               Vector3          = new Vector3()
  protected val sizeAndRotationUsage: Int              = 1 << 9
  protected val CPU_ATTRIBUTES:       VertexAttributes = new VertexAttributes(
    new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
    new VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
    new VertexAttribute(Usage.TextureCoordinates, 4, "a_region"),
    new VertexAttribute(sizeAndRotationUsage, 3, "a_sizeAndRotation")
  )
  protected val CPU_VERTEX_SIZE:              Int = CPU_ATTRIBUTES.vertexSize / 4
  protected val CPU_POSITION_OFFSET:          Int = CPU_ATTRIBUTES.getOffset(Usage.Position)
  protected val CPU_COLOR_OFFSET:             Int = CPU_ATTRIBUTES.getOffset(Usage.ColorUnpacked)
  protected val CPU_REGION_OFFSET:            Int = CPU_ATTRIBUTES.getOffset(Usage.TextureCoordinates)
  protected val CPU_SIZE_AND_ROTATION_OFFSET: Int = CPU_ATTRIBUTES.getOffset(sizeAndRotationUsage)

  private def enablePointSprites()(using sge: Sge): Unit = {
    sge.graphics.gl.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE)
    if (sge.application.getType() == Application.ApplicationType.Desktop) {
      sge.graphics.gl.glEnable(0x8861) // GL_POINT_OES
    }
    pointSpritesEnabled = true
  }
}
