/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapImageLayer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All 11 methods match Java 1:1 (supportsTransparency, get/set for region, x, y, repeatX, repeatY)
 *   - Private helper checkTransparencySupport + formatHasAlpha faithfully ported
 *   - Java switch → Scala match in formatHasAlpha
 *   - Split package, braces, no-return conventions satisfied
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (5 pairs → vars)
 */
package sge
package maps
package tiled

import sge.graphics.Pixmap
import sge.graphics.g2d.TextureRegion
import sge.maps.MapLayer
import sge.utils.Nullable

class TiledMapImageLayer(
  private var region:  TextureRegion,
  private var x:       Float,
  private var y:       Float,
  private var repeatX: Boolean,
  private var repeatY: Boolean
) extends MapLayer {

  private val _supportsTransparency: Boolean = checkTransparencySupport(region)

  /** TiledMap ImageLayers can support transparency through tint color if the image provided supports the proper pixel format. Here we check to see if the file supports transparency by checking the
    * format of the TextureData.
    *
    * @param region
    *   TextureRegion of the ImageLayer
    * @return
    *   boolean
    */
  private def checkTransparencySupport(region: TextureRegion): Boolean = {
    val format = region.getTexture().getTextureData().getFormat
    Nullable(format).isDefined && formatHasAlpha(format)
  }

  // Check if pixel format supports alpha channel
  private def formatHasAlpha(format: Pixmap.Format): Boolean = format match {
    case Pixmap.Format.Alpha          => true
    case Pixmap.Format.LuminanceAlpha => true
    case Pixmap.Format.RGBA4444       => true
    case Pixmap.Format.RGBA8888       => true
    case _                            => false
  }

  def supportsTransparency: Boolean = _supportsTransparency

  def getTextureRegion: TextureRegion = region

  def setTextureRegion(region: TextureRegion): Unit =
    this.region = region

  def getX: Float = x

  def setX(x: Float): Unit =
    this.x = x

  def getY: Float = y

  def setY(y: Float): Unit =
    this.y = y

  def isRepeatX: Boolean = repeatX

  def setRepeatX(repeatX: Boolean): Unit =
    this.repeatX = repeatX

  def isRepeatY: Boolean = repeatY

  def setRepeatY(repeatY: Boolean): Unit =
    this.repeatY = repeatY
}
