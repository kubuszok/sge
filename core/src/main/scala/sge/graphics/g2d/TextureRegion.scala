/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/TextureRegion.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: static split(Texture,...) in companion object; splitWithMargins added (not in original)
 *   Idiom: boundary/break, Nullable, split packages
 *   Issues: (1) Flat package (package sge.graphics.g2d) should be split. (2) Missing instance split(Int, Int) method (only companion static split is ported).
 *   TODO: Java-style boolean getters -- isFlipX, isFlipY → def flipX, def flipY
 *   TODO: uses flat package declaration -- convert to split (package sge / package graphics / package g2d)
 *   TODO: opaque Pixels for setRegion x/y/width/height, regionWidth/Height -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.graphics.g2d

import sge.graphics.Texture
import scala.compiletime.uninitialized

/** Defines a rectangular area of a texture. The coordinate system used has its origin in the upper left corner with the x-axis pointing to the right and the y axis pointing downwards.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
class TextureRegion() {
  var texture:      Texture = uninitialized
  var u:            Float   = 0f
  var v:            Float   = 0f
  var u2:           Float   = 0f
  var v2:           Float   = 0f
  var regionWidth:  Int     = 0
  var regionHeight: Int     = 0

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
    regionWidth = math.abs(width)
    regionHeight = math.abs(height)
  }

  def setRegion(u: Float, v: Float, u2: Float, v2: Float): Unit = {
    val texWidth  = texture.getWidth
    val texHeight = texture.getHeight
    regionWidth = math.round(math.abs(u2 - u) * texWidth)
    regionHeight = math.round(math.abs(v2 - v) * texHeight)

    // For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
    if (regionWidth == 1 && regionHeight == 1) {
      val adjustX = 0.25f / texWidth
      this.u = u + adjustX
      this.u2 = u2 - adjustX
      val adjustY = 0.25f / texHeight
      this.v = v + adjustY
      this.v2 = v2 - adjustY
    } else {
      this.u = u
      this.v = v
      this.u2 = u2
      this.v2 = v2
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
    setRegion(region.getRegionX() + x, region.getRegionY() + y, width, height)
  }

  def getTexture(): Texture = texture

  def setTexture(texture: Texture): Unit =
    this.texture = texture

  def getU(): Float = u

  def setU(u: Float): Unit = {
    this.u = u
    regionWidth = math.round(math.abs(u2 - u) * texture.getWidth)
  }

  def getV(): Float = v

  def setV(v: Float): Unit = {
    this.v = v
    regionHeight = math.round(math.abs(v2 - v) * texture.getHeight)
  }

  def getU2(): Float = u2

  def setU2(u2: Float): Unit = {
    this.u2 = u2
    regionWidth = math.round(math.abs(u2 - u) * texture.getWidth)
  }

  def getV2(): Float = v2

  def setV2(v2: Float): Unit = {
    this.v2 = v2
    regionHeight = math.round(math.abs(v2 - v) * texture.getHeight)
  }

  def getRegionX(): Int = math.round(u * texture.getWidth)

  def setRegionX(x: Int): Unit =
    setU(x / texture.getWidth.toFloat)

  def getRegionY(): Int = math.round(v * texture.getHeight)

  def setRegionY(y: Int): Unit =
    setV(y / texture.getHeight.toFloat)

  /** Returns the region's width. */
  def getRegionWidth(): Int = regionWidth

  def setRegionWidth(width: Int): Unit =
    if (isFlipX()) {
      setU(u2 + width / texture.getWidth.toFloat)
    } else {
      setU2(u + width / texture.getWidth.toFloat)
    }

  /** Returns the region's height. */
  def getRegionHeight(): Int = regionHeight

  def setRegionHeight(height: Int): Unit =
    if (isFlipY()) {
      setV(v2 + height / texture.getHeight.toFloat)
    } else {
      setV2(v + height / texture.getHeight.toFloat)
    }

  def flip(x: Boolean, y: Boolean): Unit = {
    if (x) {
      val temp = u
      u = u2
      u2 = temp
    }
    if (y) {
      val temp = v
      v = v2
      v2 = temp
    }
  }

  def isFlipX(): Boolean = u > u2

  def isFlipY(): Boolean = v > v2

  /** Offsets the region relative to the current region. Generally the region's size should be the entire size of the texture in the direction(s) it is scrolled.
    * @param xAmount
    *   The percentage to offset horizontally.
    * @param yAmount
    *   The percentage to offset vertically. This is done in texture space, so up is negative.
    */
  def scroll(xAmount: Float, yAmount: Float): Unit = {
    if (xAmount != 0) {
      val width = (u2 - u) * texture.getWidth
      u = (u + xAmount) % 1
      u2 = u + width / texture.getWidth
    }
    if (yAmount != 0) {
      val height = (v2 - v) * texture.getHeight
      v = (v + yAmount) % 1
      v2 = v + height / texture.getHeight
    }
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
    new TextureRegion(region, x, y, width, height)

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
    region.getTexture()
    val regionWidth  = region.getRegionWidth()
    val regionHeight = region.getRegionHeight()
    val xSlices      = (regionWidth - 2 * margin + spacing) / (tileWidth + spacing)
    val ySlices      = (regionHeight - 2 * margin + spacing) / (tileHeight + spacing)

    val tiles = Array.ofDim[TextureRegion](xSlices, ySlices)
    for {
      x <- 0 until xSlices
      y <- 0 until ySlices
    }
      tiles(x)(y) = new TextureRegion(
        region,
        region.getRegionX() + margin + x * (tileWidth + spacing),
        region.getRegionY() + margin + y * (tileHeight + spacing),
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
    val region = new TextureRegion(texture)
    val rows   = region.getRegionHeight() / tileHeight
    val cols   = region.getRegionWidth() / tileWidth

    val tiles = Array.ofDim[TextureRegion](rows, cols)
    for {
      row <- 0 until rows
      col <- 0 until cols
    }
      tiles(row)(col) = new TextureRegion(texture, col * tileWidth, row * tileHeight, tileWidth, tileHeight)
    tiles
  }
}
