/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/DecalMaterial.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package decals

import sge.graphics.g2d.TextureRegion
import sge.utils.Nullable

import scala.compiletime.uninitialized

/** Material used by the {@link Decal} class */
class DecalMaterial {

  var textureRegion:  TextureRegion = uninitialized
  var srcBlendFactor: Int           = 0
  var dstBlendFactor: Int           = 0

  /** Binds the material's texture to the OpenGL context and changes the glBlendFunc to the values used by it. */
  def set()(using sge: Sge): Unit = {
    textureRegion.getTexture().bind(0)
    if (!isOpaque) {
      sge.graphics.gl.glBlendFunc(srcBlendFactor, dstBlendFactor)
    }
  }

  /** @return true if the material is completely opaque, false if it is not and therefor requires blending */
  def isOpaque: Boolean =
    srcBlendFactor == DecalMaterial.NO_BLEND

  def getSrcBlendFactor: Int = srcBlendFactor

  def getDstBlendFactor: Int = dstBlendFactor

  override def equals(o: Any): Boolean =
    o match {
      case material: DecalMaterial =>
        dstBlendFactor == material.dstBlendFactor && srcBlendFactor == material.srcBlendFactor &&
        textureRegion.getTexture() == material.textureRegion.getTexture()
      case _ => false
    }

  override def hashCode(): Int = {
    var result = Nullable(textureRegion.getTexture()).fold(0)(_.hashCode())
    result = 31 * result + srcBlendFactor
    result = 31 * result + dstBlendFactor
    result
  }
}

object DecalMaterial {
  final val NO_BLEND: Int = -1
}
