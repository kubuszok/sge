/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapImageLayer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-04):
 *   - All methods match Java 1:1
 *   - Renames: getTextureRegion/setTextureRegion -> var region, getX/setX -> var x,
 *     getY/setY -> var y, isRepeatX/setRepeatX -> var repeatX, isRepeatY/setRepeatY -> var repeatY
 *   - Private helper checkTransparencySupport + formatHasAlpha faithfully ported
 *   - Java switch -> Scala match in formatHasAlpha
 *   - Split package, braces, no-return conventions satisfied
 */
package sge
package maps
package tiled

import sge.graphics.Pixmap
import sge.graphics.g2d.TextureRegion
import sge.maps.MapLayer
import sge.utils.Nullable

class TiledMapImageLayer(
  var region:  TextureRegion,
  var x:       Float,
  var y:       Float,
  var repeatX: Boolean,
  var repeatY: Boolean
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
    val format = region.texture.textureData.getFormat
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
}
