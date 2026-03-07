/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DefaultTextureBinder.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Constants (ROUNDROBIN, LRU, MAX_GLES_UNITS) -> companion object vals
 *   - Static getMaxTextureUnits -> companion object private method (using Sge)
 *   - GLTexture[] -> Array[GLTexture]; int[] -> Array[Int]
 *   - Gdx.gl -> Sge().graphics.gl
 *   - bindTexture: Java switch -> Scala match; null texture check via Nullable fold
 *   - unsafeSetWrap/unsafeSetFilter: Java passes nullable enums directly;
 *     Scala wraps in for-comprehension on Nullable (only applies when both present)
 *   - bindTextureLRU: complex loop with break -> boundary/break; logic matches Java
 *   - bindTextureRoundRobin: loop with return -> boundary/break
 *   - Minor: bindTextureLRU has slightly restructured loop but same semantics
 *   - All methods fully ported
 *   - Audit: pass (2026-03-03)
 *   Idiom: typed GL enums -- TextureTarget
 */
package sge
package graphics
package g3d
package utils

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.GL20
import sge.utils.{ BufferUtils, Nullable, SgeError }

/** Class that you assign a range of texture units and binds textures for you within that range. It does some basic usage tracking to avoid unnecessary bind calls.
  * @author
  *   xoppa
  */
final class DefaultTextureBinder(
  val method: Int,
  val offset: Int,
  count:      Int
)(using Sge)
    extends TextureBinder {

  private val _count: Int = {
    val max = Math.min(DefaultTextureBinder.getMaxTextureUnits(), DefaultTextureBinder.MAX_GLES_UNITS)
    val c   = if (count < 0) max - offset else count
    if (offset < 0 || c < 0 || (offset + c) > max) throw SgeError.InvalidInput("Illegal arguments")
    c
  }

  /** The textures currently exclusive bound */
  private val textures: Array[GLTexture] = new Array[GLTexture](_count)

  /** Texture units ordered from most to least recently used */
  private val unitsLRU: Array[Int] = if (method == DefaultTextureBinder.LRU) new Array[Int](_count) else Array.emptyIntArray

  /** Flag to indicate the current texture is reused */
  private var reused: Boolean = false

  private var reuseCount: Int = 0 // Profiling stats -- used by getBindCount/getReuseCount
  private var bindCount:  Int = 0 // Profiling stats -- used by getBindCount/getReuseCount

  /** Uses all available texture units and reuse weight of 3 */
  def this(method: Int)(using Sge) = {
    this(method, 0, -1)
  }

  /** Uses all remaining texture units and reuse weight of 3 */
  def this(method: Int, offset: Int)(using Sge) = {
    this(method, offset, -1)
  }

  override def begin(): Unit =
    for (i <- 0 until _count) {
      textures(i) = null
      if (unitsLRU.nonEmpty) unitsLRU(i) = i
    }

  override def end(): Unit =
    /*
     * No need to unbind and textures are set to null in begin() for(int i = 0; i < count; i++) { if (textures[i] != null) {
     * Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + offset + i); Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0); textures[i] = null; }
     * }
     */
    Sge().graphics.gl.glActiveTexture(GL20.GL_TEXTURE0)

  override def bind(textureDescriptor: TextureDescriptor[?]): Int =
    bindTexture(textureDescriptor, false)

  private val tempDesc: TextureDescriptor[GLTexture] = TextureDescriptor[GLTexture]()

  override def bind(texture: GLTexture): Int = {
    tempDesc.set(texture, Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
    bindTexture(tempDesc, false)
  }

  private def bindTexture(textureDesc: TextureDescriptor[?], rebind: Boolean): Int = {
    reused = false
    textureDesc.texture.fold(-1) { texture =>
      val (_, result) = method match {
        case DefaultTextureBinder.ROUNDROBIN =>
          val i = bindTextureRoundRobin(texture)
          (i, offset + i)
        case DefaultTextureBinder.LRU =>
          val i = bindTextureLRU(texture)
          (i, offset + i)
        case _ => (-1, -1)
      }

      if (result < 0) result
      else {
        if (reused) {
          reuseCount += 1
          if (rebind)
            texture.bind(result)
          else
            Sge().graphics.gl.glActiveTexture(GL20.GL_TEXTURE0 + result)
        } else {
          bindCount += 1
        }
        // Apply texture parameters if present
        for {
          u <- textureDesc.uWrap
          v <- textureDesc.vWrap
        } texture.unsafeSetWrap(u, v)
        for {
          min <- textureDesc.minFilter
          mag <- textureDesc.magFilter
        } texture.unsafeSetFilter(min, mag)
        result
      }
    }
  }

  private var currentTexture: Int = 0

  private def bindTextureRoundRobin(texture: GLTexture): Int = boundary {
    for (i <- 0 until _count) {
      val idx = (currentTexture + i) % _count
      if (textures(idx) eq texture) {
        reused = true
        break(idx)
      }
    }
    currentTexture = (currentTexture + 1) % _count
    textures(currentTexture) = texture
    texture.bind(offset + currentTexture)
    currentTexture
  }

  private def bindTextureLRU(texture: GLTexture): Int = boundary {
    var i = 0
    while (i < _count) {
      val idx = unitsLRU(i)
      if (textures(idx) eq texture) {
        reused = true
        i += 1 // break equivalent - we found it
        // shift LRU entries
        var j = i - 1
        while (j > 0) {
          unitsLRU(j) = unitsLRU(j - 1)
          j -= 1
        }
        unitsLRU(0) = idx
        if (!reused) {
          textures(idx) = texture
          texture.bind(offset + idx)
        }
        break(idx)
      }
      if (Nullable(textures(idx)).isEmpty) {
        i += 1
        // shift LRU entries
        var j = i - 1
        while (j > 0) {
          unitsLRU(j) = unitsLRU(j - 1)
          j -= 1
        }
        unitsLRU(0) = idx
        textures(idx) = texture
        texture.bind(offset + idx)
        break(idx)
      }
      i += 1
    }
    // Use the least recently used
    if (i >= _count) i = _count - 1
    val idx = unitsLRU(i)
    while (i > 0) {
      unitsLRU(i) = unitsLRU(i - 1)
      i -= 1
    }
    unitsLRU(0) = idx
    textures(idx) = texture
    texture.bind(offset + idx)
    idx
  }

  override def getBindCount: Int = bindCount

  override def getReuseCount: Int = reuseCount

  override def resetCounts(): Unit = {
    bindCount = 0
    reuseCount = 0
  }
}

object DefaultTextureBinder {
  val ROUNDROBIN: Int = 0
  val LRU:        Int = 1

  /** GLES only supports up to 32 textures */
  val MAX_GLES_UNITS: Int = 32

  private def getMaxTextureUnits()(using Sge): Int = {
    val buffer = BufferUtils.newIntBuffer(16)
    Sge().graphics.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, buffer)
    buffer.get(0)
  }
}
