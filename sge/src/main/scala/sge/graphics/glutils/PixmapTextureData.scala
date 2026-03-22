/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/PixmapTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: private primary constructor with public secondary constructors; Nullable[Format] for nullable format parameter
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.graphics.TextureData
import sge.graphics.TextureData.TextureDataType
import sge.utils.SgeError
import sge.utils.Nullable

class PixmapTextureData private (format: Format, pixmap: Pixmap, val useMipMaps: Boolean, val disposePixmap: Boolean, val isManaged: Boolean) extends TextureData {

  def this(pixmap: Pixmap, format: Nullable[Format], useMipMaps: Boolean, disposePixmap: Boolean, isManaged: Boolean) = {
    this(format.getOrElse(pixmap.format), pixmap, useMipMaps, disposePixmap, isManaged)
  }

  def this(pixmap: Pixmap, format: Format, useMipMaps: Boolean, disposePixmap: Boolean) = {
    this(format, pixmap, useMipMaps, disposePixmap, isManaged = false)
  }

  override def consumePixmap(): Pixmap = pixmap

  override def width: Int = pixmap.width.toInt

  override def height: Int = pixmap.height.toInt

  override def getFormat: Format = format

  override def dataType: TextureDataType = TextureDataType.Pixmap

  override def consumeCustomData(target: TextureTarget): Unit =
    throw SgeError.GraphicsError("This TextureData implementation does not upload data itself")

  override def isPrepared: Boolean = true

  override def prepare(): Unit =
    throw SgeError.GraphicsError("prepare() must not be called on a PixmapTextureData instance as it is already prepared.")
}
