/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/RegionInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.assets.AssetManager
import sge.graphics.Texture
import sge.graphics.g2d.{ TextureAtlas, TextureRegion }
import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ResourceData
import sge.utils.DynamicArray
import sge.utils.Nullable

/** It's an {@link Influencer} which assigns a region of a {@link Texture} to the particles.
  * @author
  *   Inferno
  */
abstract class RegionInfluencer extends Influencer {

  import RegionInfluencer.*

  var regions:                 DynamicArray[AspectTextureRegion] = scala.compiletime.uninitialized
  protected var regionChannel: FloatChannel                      = scala.compiletime.uninitialized
  var atlasName:               Nullable[String]                  = Nullable.empty

  def this(regionsCount: Int) = {
    this()
    this.regions = DynamicArray[AspectTextureRegion](regionsCount)
  }

  /** All the regions must be defined on the same Texture */
  def this(textureRegions: Array[TextureRegion]) = {
    this()
    atlasName = Nullable.empty
    this.regions = DynamicArray[AspectTextureRegion](textureRegions.length)
    add(textureRegions*)
  }

  def this(texture: Texture) = {
    this(Array(new TextureRegion(texture)))
  }

  def this(regionInfluencer: RegionInfluencer) = {
    this()
    this.regions = DynamicArray[AspectTextureRegion](regionInfluencer.regions.size)
    var i = 0
    while (i < regionInfluencer.regions.size) {
      regions.add(new AspectTextureRegion(regionInfluencer.regions(i)))
      i += 1
    }
  }

  /** Initializes with a single default region covering the full texture */
  protected def initDefault(): Unit = {
    this.regions = DynamicArray[AspectTextureRegion](1)
    val aspectRegion = new AspectTextureRegion()
    aspectRegion.u = 0
    aspectRegion.v = 0
    aspectRegion.u2 = 1
    aspectRegion.v2 = 1
    aspectRegion.halfInvAspectRatio = 0.5f
    regions.add(aspectRegion)
  }

  def setAtlasName(atlasName: Nullable[String]): Unit =
    this.atlasName = atlasName

  def add(regions: TextureRegion*): Unit =
    for (region <- regions)
      this.regions.add(new AspectTextureRegion(region))

  def clear(): Unit = {
    atlasName = Nullable.empty
    regions.clear()
  }

  override def load(manager: AssetManager, resources: ResourceData[?]): Unit = {
    super.load(manager, resources)
    val data = resources.getSaveData(ASSET_DATA)
    if (data.isEmpty) {
      ()
    } else {
      data.foreach { d =>
        d.loadAsset().foreach { assetDesc =>
          val atlas = manager.get(assetDesc.fileName, classOf[TextureAtlas])
          for (atr <- regions)
            atr.updateUV(atlas)
        }
      }
    }
  }

  override def save(manager: AssetManager, resources: ResourceData[?]): Unit = {
    super.save(manager, resources)
    atlasName.foreach { name =>
      var data = resources.getSaveData(ASSET_DATA)
      if (data.isEmpty) {
        data = Nullable(resources.createSaveData(ASSET_DATA))
      }
      data.foreach { d =>
        d.saveAsset(name, classOf[TextureAtlas])
      }
    }
  }

  override def allocateChannels(): Unit =
    regionChannel = controller.particles.addChannel(ParticleChannels.TextureRegion)
}

object RegionInfluencer {

  private val ASSET_DATA: String = "atlasAssetData"

  /** It's a class used internally by the {@link RegionInfluencer} to represent a texture region. It contains the uv coordinates of the region and the region inverse aspect ratio.
    */
  class AspectTextureRegion {
    var u:                  Float            = 0f
    var v:                  Float            = 0f
    var u2:                 Float            = 0f
    var v2:                 Float            = 0f
    var halfInvAspectRatio: Float            = 0f
    var imageName:          Nullable[String] = Nullable.empty

    def this(aspectTextureRegion: AspectTextureRegion) = {
      this()
      set(aspectTextureRegion)
    }

    def this(region: TextureRegion) = {
      this()
      set(region)
    }

    def set(region: TextureRegion): Unit = {
      this.u = region.getU()
      this.v = region.getV()
      this.u2 = region.getU2()
      this.v2 = region.getV2()
      this.halfInvAspectRatio = 0.5f * (region.getRegionHeight().toFloat / region.getRegionWidth())
      region match {
        case atlasRegion: TextureAtlas.AtlasRegion =>
          this.imageName = Nullable(atlasRegion.name)
        case _ =>
      }
    }

    def set(aspectTextureRegion: AspectTextureRegion): Unit = {
      u = aspectTextureRegion.u
      v = aspectTextureRegion.v
      u2 = aspectTextureRegion.u2
      v2 = aspectTextureRegion.v2
      halfInvAspectRatio = aspectTextureRegion.halfInvAspectRatio
      imageName = aspectTextureRegion.imageName
    }

    def updateUV(atlas: TextureAtlas): Unit =
      if (imageName.isEmpty) {
        ()
      } else {
        imageName.foreach { name =>
          atlas.findRegion(name).foreach { region =>
            this.u = region.getU()
            this.v = region.getV()
            this.u2 = region.getU2()
            this.v2 = region.getV2()
            this.halfInvAspectRatio = 0.5f * (region.getRegionHeight().toFloat / region.getRegionWidth())
          }
        }
      }
  }

  /** Assigns the first region of {@link RegionInfluencer#regions} to the particles. */
  class Single extends RegionInfluencer {

    initDefault()

    def this(regionInfluencer: Single) = {
      this()
      this.regions = DynamicArray[AspectTextureRegion](regionInfluencer.regions.size)
      var i = 0
      while (i < regionInfluencer.regions.size) {
        regions.add(new AspectTextureRegion(regionInfluencer.regions(i)))
        i += 1
      }
    }

    def this(textureRegion: TextureRegion) = {
      this()
      atlasName = Nullable.empty
      this.regions = DynamicArray[AspectTextureRegion](1)
      add(textureRegion)
    }

    def this(texture: Texture) = {
      this(new TextureRegion(texture))
    }

    override def init(): Unit = {
      val region = regions(0)
      var i      = 0
      val c      = controller.emitter.maxParticleCount * regionChannel.strideSize
      while (i < c) {
        regionChannel.floatData(i + ParticleChannels.UOffset) = region.u
        regionChannel.floatData(i + ParticleChannels.VOffset) = region.v
        regionChannel.floatData(i + ParticleChannels.U2Offset) = region.u2
        regionChannel.floatData(i + ParticleChannels.V2Offset) = region.v2
        regionChannel.floatData(i + ParticleChannels.HalfWidthOffset) = 0.5f
        regionChannel.floatData(i + ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
        i += regionChannel.strideSize
      }
    }

    override def copy(): Single =
      new Single(this)
  }

  /** Assigns a random region of {@link RegionInfluencer#regions} to the particles. */
  class Random extends RegionInfluencer {

    initDefault()

    def this(regionInfluencer: Random) = {
      this()
      this.regions = DynamicArray[AspectTextureRegion](regionInfluencer.regions.size)
      var i = 0
      while (i < regionInfluencer.regions.size) {
        regions.add(new AspectTextureRegion(regionInfluencer.regions(i)))
        i += 1
      }
    }

    def this(textureRegion: TextureRegion) = {
      this()
      atlasName = Nullable.empty
      this.regions = DynamicArray[AspectTextureRegion](1)
      add(textureRegion)
    }

    def this(texture: Texture) = {
      this(new TextureRegion(texture))
    }

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex * regionChannel.strideSize
      val c = i + count * regionChannel.strideSize
      while (i < c) {
        val regionIdx = sge.math.MathUtils.random(regions.size - 1)
        val region    = regions(regionIdx)
        regionChannel.floatData(i + ParticleChannels.UOffset) = region.u
        regionChannel.floatData(i + ParticleChannels.VOffset) = region.v
        regionChannel.floatData(i + ParticleChannels.U2Offset) = region.u2
        regionChannel.floatData(i + ParticleChannels.V2Offset) = region.v2
        regionChannel.floatData(i + ParticleChannels.HalfWidthOffset) = 0.5f
        regionChannel.floatData(i + ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
        i += regionChannel.strideSize
      }
    }

    override def copy(): Random =
      new Random(this)
  }

  /** Assigns a region to the particles using the particle life percent to calculate the current index in the {@link RegionInfluencer#regions} array.
    */
  class Animated extends RegionInfluencer {

    initDefault()

    private var lifeChannel: FloatChannel = scala.compiletime.uninitialized

    def this(regionInfluencer: Animated) = {
      this()
      this.regions = DynamicArray[AspectTextureRegion](regionInfluencer.regions.size)
      var i = 0
      while (i < regionInfluencer.regions.size) {
        regions.add(new AspectTextureRegion(regionInfluencer.regions(i)))
        i += 1
      }
    }

    def this(textureRegion: TextureRegion) = {
      this()
      atlasName = Nullable.empty
      this.regions = DynamicArray[AspectTextureRegion](1)
      add(textureRegion)
    }

    def this(texture: Texture) = {
      this(new TextureRegion(texture))
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
    }

    override def update(): Unit = {
      var i = 0
      var l = ParticleChannels.LifePercentOffset
      val c = controller.particles.size * regionChannel.strideSize
      while (i < c) {
        val region = regions((lifeChannel.floatData(l) * (regions.size - 1)).toInt)
        regionChannel.floatData(i + ParticleChannels.UOffset) = region.u
        regionChannel.floatData(i + ParticleChannels.VOffset) = region.v
        regionChannel.floatData(i + ParticleChannels.U2Offset) = region.u2
        regionChannel.floatData(i + ParticleChannels.V2Offset) = region.v2
        regionChannel.floatData(i + ParticleChannels.HalfWidthOffset) = 0.5f
        regionChannel.floatData(i + ParticleChannels.HalfHeightOffset) = region.halfInvAspectRatio
        i += regionChannel.strideSize
        l += lifeChannel.strideSize
      }
    }

    override def copy(): Animated =
      new Animated(this)
  }
}
