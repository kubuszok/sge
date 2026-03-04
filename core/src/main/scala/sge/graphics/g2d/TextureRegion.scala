/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/TextureRegion.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: static split(Texture,...) in companion object; splitWithMargins added (not in original)
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Added instance split(Int, Int) method (was only companion static split).
 *   Fixes: Java-style boolean getters -- isFlipX() → flipX, isFlipY() → flipY
 *   Fixes: Java-style getters/setters → Scala property accessors (u, v, u2, v2, regionWidth, regionHeight,
 *     regionX, regionY); removed redundant getTexture()/setTexture()
 *   Improvement: opaque Pixels for setRegion x/y/width/height, regionWidth/Height -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.Texture
import scala.compiletime.uninitialized
import scala.math.{ abs, round }

/** Defines a rectangular area of a texture. The coordinate system used has its origin in the upper left corner with the x-axis pointing to the right and the y axis pointing downwards.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
class TextureRegion() {
  var texture:               Texture = uninitialized
  private var _u:            Float   = 0f
  private var _v:            Float   = 0f
  private var _u2:           Float   = 0f
  private var _v2:           Float   = 0f
  private var _regionWidth:  Int     = 0
  private var _regionHeight: Int     = 0

  /** Constructs a region the size of the specified texture. */
  def this(texture: Texture) = {
    this()
    this.texture = texture
    setRegion(0, 0, texture.getWidth, texture.getHeight)
  }

  /** @param width
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param height
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(texture: Texture, width: Int, height: Int) = {
    this()
    this.texture = texture
    setRegion(0, 0, width, height)
  }

  /** @param width
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param height
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(texture: Texture, x: Int, y: Int, width: Int, height: Int) = {
    this()
    this.texture = texture
    setRegion(x, y, width, height)
  }

  def this(texture: Texture, u: Float, v: Float, u2: Float, v2: Float) = {
    this()
    this.texture = texture
    setRegion(u, v, u2, v2)
  }

  /** Constructs a region with the same texture and coordinates of the specified region. */
  def this(region: TextureRegion) = {
    this()
    setRegion(region)
  }

  /** Constructs a region with the same texture as the specified region and sets the coordinates relative to the specified region.
    * @param width
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param height
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def this(region: TextureRegion, x: Int, y: Int, width: Int, height: Int) = {
    this()
    setRegion(region, x, y, width, height)
  }

  /** Sets the texture and sets the coordinates to the size of the specified texture. */
  def setRegion(texture: Texture): Unit = {
    this.texture = texture
    setRegion(0, 0, texture.getWidth, texture.getHeight)
  }

  /** @param width
    *   The width of the texture region. May be negative to flip the sprite when drawn.
    * @param height
    *   The height of the texture region. May be negative to flip the sprite when drawn.
    */
  def setRegion(x: Int, y: Int, width: Int, height: Int): Unit = {
    val invTexWidth  = 1f / texture.getWidth
    val invTexHeight = 1f / texture.getHeight
    setRegion(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight)
    _regionWidth = abs(width)
    _regionHeight = abs(height)
  }

  def setRegion(u: Float, v: Float, u2: Float, v2: Float): Unit = {
    val texWidth  = texture.getWidth
    val texHeight = texture.getHeight
    _regionWidth = round(abs(u2 - u) * texWidth)
    _regionHeight = round(abs(v2 - v) * texHeight)

    // For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
    if (_regionWidth == 1 && _regionHeight == 1) {
      val adjustX = 0.25f / texWidth
      _u = u + adjustX
      _u2 = u2 - adjustX
      val adjustY = 0.25f / texHeight
      _v = v + adjustY
      _v2 = v2 - adjustY
    } else {
      _u = u
      _v = v
      _u2 = u2
      _v2 = v2
    }
  }

  /** Sets the texture and coordinates to the specified region. */
  def setRegion(region: TextureRegion): Unit = {
    texture = region.texture
    setRegion(region.u, region.v, region.u2, region.v2)
  }

  /** Sets the texture to that of the specified region and sets the coordinates relative to the specified region. */
  def setRegion(region: TextureRegion, x: Int, y: Int, width: Int, height: Int): Unit = {
    texture = region.texture
    setRegion(region.regionX + x, region.regionY + y, width, height)
  }

  def u: Float = _u

  def u_=(u: Float): Unit = {
    _u = u
    _regionWidth = round(abs(_u2 - _u) * texture.getWidth)
  }

  def v: Float = _v

  def v_=(v: Float): Unit = {
    _v = v
    _regionHeight = round(abs(_v2 - _v) * texture.getHeight)
  }

  def u2: Float = _u2

  def u2_=(u2: Float): Unit = {
    _u2 = u2
    _regionWidth = round(abs(_u2 - _u) * texture.getWidth)
  }

  def v2: Float = _v2

  def v2_=(v2: Float): Unit = {
    _v2 = v2
    _regionHeight = round(abs(_v2 - _v) * texture.getHeight)
  }

  def regionX: Int = round(_u * texture.getWidth)

  def regionX_=(x: Int): Unit =
    u = x / texture.getWidth.toFloat

  def regionY: Int = round(_v * texture.getHeight)

  def regionY_=(y: Int): Unit =
    v = y / texture.getHeight.toFloat

  /** Returns the region's width. */
  def regionWidth: Int = _regionWidth

  def regionWidth_=(width: Int): Unit =
    if (flipX) {
      u = _u2 + width / texture.getWidth.toFloat
    } else {
      u2 = _u + width / texture.getWidth.toFloat
    }

  /** Returns the region's height. */
  def regionHeight: Int = _regionHeight

  def regionHeight_=(height: Int): Unit =
    if (flipY) {
      v = _v2 + height / texture.getHeight.toFloat
    } else {
      v2 = _v + height / texture.getHeight.toFloat
    }

  def flip(x: Boolean, y: Boolean): Unit = {
    if (x) {
      val temp = _u
      _u = _u2
      _u2 = temp
    }
    if (y) {
      val temp = _v
      _v = _v2
      _v2 = temp
    }
  }

  def flipX: Boolean = _u > _u2

  def flipY: Boolean = _v > _v2

  /** Offsets the region relative to the current region. Generally the region's size should be the entire size of the texture in the direction(s) it is scrolled.
    * @param xAmount
    *   The percentage to offset horizontally.
    * @param yAmount
    *   The percentage to offset vertically. This is done in texture space, so up is negative.
    */
  def scroll(xAmount: Float, yAmount: Float): Unit = {
    if (xAmount != 0) {
      val width = (_u2 - _u) * texture.getWidth
      _u = (_u + xAmount) % 1
      _u2 = _u + width / texture.getWidth
    }
    if (yAmount != 0) {
      val height = (_v2 - _v) * texture.getHeight
      _v = (_v + yAmount) % 1
      _v2 = _v + height / texture.getHeight
    }
  }

  /** Splits this region into a two-dimensional array of TextureRegions.
    * @param tileWidth
    *   a tile's width in pixels
    * @param tileHeight
    *   a tile's height in pixels
    * @return
    *   a two-dimensional array of TextureRegions indexed [row][col]
    */
  def split(tileWidth: Int, tileHeight: Int): Array[Array[TextureRegion]] = {
    var x      = regionX
    var y      = regionY
    val rows   = _regionHeight / tileHeight
    val cols   = _regionWidth / tileWidth
    val startX = x
    val tiles  = Array.ofDim[TextureRegion](rows, cols)
    for (row <- 0 until rows) {
      x = startX
      for (col <- 0 until cols) {
        tiles(row)(col) = TextureRegion(texture, x, y, tileWidth, tileHeight)
        x += tileWidth
      }
      y += tileHeight
    }
    tiles
  }
}

object TextureRegion {

  /** Helper function to create a new TextureRegion that represents a sub-region of another TextureRegion. The new TextureRegion may be used independently from the other region.
    * @param region
    *   The TextureRegion this new region is based on
    * @param x
    *   The x position (in pixels) where to start the new region
    * @param y
    *   The y position (in pixels) where to start the new region
    * @param width
    *   The width of the new region (in pixels)
    * @param height
    *   The height of the new region (in pixels)
    * @return
    *   The new TextureRegion
    */
  def createSubRegion(region: TextureRegion, x: Int, y: Int, width: Int, height: Int): TextureRegion =
    TextureRegion(region, x, y, width, height)

  /** Splits a texture into a two-dimensional array of TextureRegions based on the specified region, tile size, and margins.
    * @param region
    *   The TextureRegion this grid is based on
    * @param tileWidth
    *   a tile's width in pixels
    * @param tileHeight
    *   a tile's height in pixels
    * @param margin
    *   the margin around tiles in pixels
    * @param spacing
    *   the spacing between tiles in pixels
    * @return
    *   a two-dimensional array of TextureRegions based on the specified region.
    */
  def splitWithMargins(region: TextureRegion, tileWidth: Int, tileHeight: Int, margin: Int, spacing: Int): Array[Array[TextureRegion]] = {
    val regionWidth  = region.regionWidth
    val regionHeight = region.regionHeight
    val xSlices      = (regionWidth - 2 * margin + spacing) / (tileWidth + spacing)
    val ySlices      = (regionHeight - 2 * margin + spacing) / (tileHeight + spacing)

    val tiles = Array.ofDim[TextureRegion](xSlices, ySlices)
    for {
      x <- 0 until xSlices
      y <- 0 until ySlices
    }
      tiles(x)(y) = TextureRegion(
        region,
        region.regionX + margin + x * (tileWidth + spacing),
        region.regionY + margin + y * (tileHeight + spacing),
        tileWidth,
        tileHeight
      )
    tiles
  }

  /** Splits a texture into a two-dimensional array of TextureRegions.
    * @param texture
    *   the Texture to split
    * @param tileWidth
    *   a tile's width in pixels
    * @param tileHeight
    *   a tile's height in pixels
    * @return
    *   a two-dimensional array of TextureRegions
    */
  def split(texture: Texture, tileWidth: Int, tileHeight: Int): Array[Array[TextureRegion]] = {
    val region = TextureRegion(texture)
    val rows   = region.regionHeight / tileHeight
    val cols   = region.regionWidth / tileWidth

    val tiles = Array.ofDim[TextureRegion](rows, cols)
    for {
      row <- 0 until rows
      col <- 0 until cols
    }
      tiles(row)(col) = TextureRegion(texture, col * tileWidth, row * tileHeight, tileWidth, tileHeight)
    tiles
  }
}
