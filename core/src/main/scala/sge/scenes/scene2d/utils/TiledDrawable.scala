/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/TiledDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.utils.Align

/** Draws a {@link TextureRegion} repeatedly to fill the area, instead of stretching it.
  * @author
  *   Nathan Sweet
  * @author
  *   Thomas Creutzenberg
  */
class TiledDrawable() extends TextureRegionDrawable {
  private val color:  Color = new Color(1, 1, 1, 1)
  private var scale:  Float = 1
  private var _align: Align = Align.bottomLeft

  def this(region: TextureRegion) = {
    this()
    setRegion(region)
  }

  def this(drawable: TextureRegionDrawable) = {
    this()
    // Copy base drawable properties
    drawable match {
      case bd: BaseDrawable =>
        setLeftWidth(bd.getLeftWidth)
        setRightWidth(bd.getRightWidth)
        setTopHeight(bd.getTopHeight)
        setBottomHeight(bd.getBottomHeight)
        setMinWidth(bd.getMinWidth)
        setMinHeight(bd.getMinHeight)
        setName(bd.getName)
    }
    setRegion(drawable.getRegion)
  }

  override def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit = {
    val oldColor = batch.getPackedColor()
    batch.setColor(batch.getColor().mul(color))

    TiledDrawable.draw(batch, getRegion, x, y, width, height, scale, _align)

    batch.setPackedColor(oldColor)
  }

  override def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    throw new UnsupportedOperationException()

  def getColor: Color = color

  def setScale(scale: Float): Unit = this.scale = scale

  def getScale: Float = scale

  def getAlign: Align = _align

  def setAlign(align: Align): Unit = this._align = align

  override def tint(tint: Color): TiledDrawable = {
    val drawable = new TiledDrawable(this)
    drawable.color.set(tint)
    drawable.setLeftWidth(getLeftWidth)
    drawable.setRightWidth(getRightWidth)
    drawable.setTopHeight(getTopHeight)
    drawable.setBottomHeight(getBottomHeight)
    drawable
  }
}

object TiledDrawable {
  def draw(batch: Batch, textureRegion: TextureRegion, x: Float, y: Float, width: Float, height: Float, scale: Float, align: Align): Unit = {
    val regionWidth  = textureRegion.getRegionWidth() * scale
    val regionHeight = textureRegion.getRegionHeight() * scale

    val texture       = textureRegion.getTexture()
    val textureWidth  = texture.getWidth * scale
    val textureHeight = texture.getHeight * scale
    val u             = textureRegion.getU()
    val v             = textureRegion.getV()
    val u2            = textureRegion.getU2()
    val v2            = textureRegion.getV2()

    var fullX = (width / regionWidth).toInt
    val leftPartialWidth: Float =
      if (align.isLeft) 0f
      else if (align.isRight) width - (regionWidth * fullX)
      else {
        if (fullX != 0) {
          fullX = if (fullX % 2 == 1) fullX else fullX - 1
          0.5f * (width - (regionWidth * fullX))
        } else 0f
      }
    val rightPartialWidth: Float =
      if (align.isLeft) width - (regionWidth * fullX)
      else if (align.isRight) 0f
      else leftPartialWidth

    var fullY = (height / regionHeight).toInt
    val bottomPartialHeight: Float =
      if (align.isTop) height - (regionHeight * fullY)
      else if (align.isBottom) 0f
      else {
        if (fullY != 0) {
          fullY = if (fullY % 2 == 1) fullY else fullY - 1
          0.5f * (height - (regionHeight * fullY))
        } else 0f
      }
    val topPartialHeight: Float =
      if (align.isTop) 0f
      else if (align.isBottom) height - (regionHeight * fullY)
      else bottomPartialHeight

    var drawX = x
    var drawY = y

    // Left edge
    if (leftPartialWidth > 0f) {
      val leftEdgeU = u2 - (leftPartialWidth / textureWidth)

      // Left bottom partial
      if (bottomPartialHeight > 0f) {
        val leftBottomV = v + (bottomPartialHeight / textureHeight)
        batch.draw(texture, drawX, drawY, leftPartialWidth, bottomPartialHeight, leftEdgeU, leftBottomV, u2, v)
        drawY += bottomPartialHeight
      }

      // Left center partials
      if (fullY == 0 && align.isCenterVertical) {
        val vOffset      = 0.5f * (v2 - v) * (1f - (height / regionHeight))
        val leftCenterV  = v2 - vOffset
        val leftCenterV2 = v + vOffset
        batch.draw(texture, drawX, drawY, leftPartialWidth, height, leftEdgeU, leftCenterV, u2, leftCenterV2)
        drawY += height
      } else {
        var i = 0
        while (i < fullY) {
          batch.draw(texture, drawX, drawY, leftPartialWidth, regionHeight, leftEdgeU, v2, u2, v)
          drawY += regionHeight
          i += 1
        }
      }

      // Left top partial
      if (topPartialHeight > 0f) {
        val leftTopV = v2 - (topPartialHeight / textureHeight)
        batch.draw(texture, drawX, drawY, leftPartialWidth, topPartialHeight, leftEdgeU, v2, u2, leftTopV)
      }
    }

    // Center full texture regions
    {
      // Center bottom partials
      if (bottomPartialHeight > 0f) {
        drawX = x + leftPartialWidth
        drawY = y

        val centerBottomV = v + (bottomPartialHeight / textureHeight)

        if (fullX == 0 && align.isCenterHorizontal) {
          val uOffset        = 0.5f * (u2 - u) * (1f - (width / regionWidth))
          val centerBottomU  = u + uOffset
          val centerBottomU2 = u2 - uOffset
          batch.draw(texture, drawX, drawY, width, bottomPartialHeight, centerBottomU, centerBottomV, centerBottomU2, v)
          drawX += width
        } else {
          var i = 0
          while (i < fullX) {
            batch.draw(texture, drawX, drawY, regionWidth, bottomPartialHeight, u, centerBottomV, u2, v)
            drawX += regionWidth
            i += 1
          }
        }
      }

      // Center full texture regions
      {
        drawX = x + leftPartialWidth

        val originalFullX = fullX
        val originalFullY = fullY

        var centerCenterDrawWidth  = regionWidth
        var centerCenterDrawHeight = regionHeight
        var centerCenterU          = u
        var centerCenterU2         = u2
        var centerCenterV          = v2
        var centerCenterV2         = v
        if (fullX == 0 && align.isCenterHorizontal) {
          fullX = 1
          centerCenterDrawWidth = width
          val uOffset = 0.5f * (u2 - u) * (1f - (width / regionWidth))
          centerCenterU = u + uOffset
          centerCenterU2 = u2 - uOffset
        }
        if (fullY == 0 && align.isCenterVertical) {
          fullY = 1
          centerCenterDrawHeight = height
          val vOffset = 0.5f * (v2 - v) * (1f - (height / regionHeight))
          centerCenterV = v2 - vOffset
          centerCenterV2 = v + vOffset
        }
        var i = 0
        while (i < fullX) {
          drawY = y + bottomPartialHeight
          var ii = 0
          while (ii < fullY) {
            batch.draw(
              texture,
              drawX,
              drawY,
              centerCenterDrawWidth,
              centerCenterDrawHeight,
              centerCenterU,
              centerCenterV,
              centerCenterU2,
              centerCenterV2
            )
            drawY += centerCenterDrawHeight
            ii += 1
          }
          drawX += centerCenterDrawWidth
          i += 1
        }

        fullX = originalFullX
        fullY = originalFullY
      }

      // Center top partials
      if (topPartialHeight > 0f) {
        drawX = x + leftPartialWidth

        val centerTopV = v2 - (topPartialHeight / textureHeight)

        if (fullX == 0 && align.isCenterHorizontal) {
          val uOffset     = 0.5f * (u2 - u) * (1f - (width / regionWidth))
          val centerTopU  = u + uOffset
          val centerTopU2 = u2 - uOffset
          batch.draw(texture, drawX, drawY, width, topPartialHeight, centerTopU, v2, centerTopU2, centerTopV)
          drawX += width
        } else {
          var i = 0
          while (i < fullX) {
            batch.draw(texture, drawX, drawY, regionWidth, topPartialHeight, u, v2, u2, centerTopV)
            drawX += regionWidth
            i += 1
          }
        }
      }
    }

    // Right edge
    if (rightPartialWidth > 0f) {
      drawY = y

      val rightEdgeU2 = u + (rightPartialWidth / textureWidth)

      // Right bottom partial
      if (bottomPartialHeight > 0f) {
        val rightBottomV = v + (bottomPartialHeight / textureHeight)
        batch.draw(texture, drawX, drawY, rightPartialWidth, bottomPartialHeight, u, rightBottomV, rightEdgeU2, v)
        drawY += bottomPartialHeight
      }

      // Right center partials
      if (fullY == 0 && align.isCenterVertical) {
        val vOffset       = 0.5f * (v2 - v) * (1f - (height / regionHeight))
        val rightCenterV  = v2 - vOffset
        val rightCenterV2 = v + vOffset
        batch.draw(texture, drawX, drawY, rightPartialWidth, height, u, rightCenterV, rightEdgeU2, rightCenterV2)
        drawY += height
      } else {
        var i = 0
        while (i < fullY) {
          batch.draw(texture, drawX, drawY, rightPartialWidth, regionHeight, u, v2, rightEdgeU2, v)
          drawY += regionHeight
          i += 1
        }
      }

      // Right top partial
      if (topPartialHeight > 0f) {
        val rightTopV = v2 - (topPartialHeight / textureHeight)
        batch.draw(texture, drawX, drawY, rightPartialWidth, topPartialHeight, u, v2, rightEdgeU2, rightTopV)
      }
    }
  }
}
