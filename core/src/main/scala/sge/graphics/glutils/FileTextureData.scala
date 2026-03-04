/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FileTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: fields use Nullable[Pixmap]/Nullable[Format]; constructor uses default params instead of single 4-arg constructor
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.graphics.PixmapIO
import sge.graphics.TextureData
import sge.utils.SgeError
import sge.utils.Nullable

class FileTextureData(
  val file:            FileHandle,
  preloadedPixmap:     Nullable[Pixmap] = Nullable.empty,
  formatArg:           Nullable[Format] = Nullable.empty,
  val useMipMapsParam: Boolean = false
) extends TextureData {

  private var width:           Int              = 0
  private var height:          Int              = 0
  private var format:          Nullable[Format] = formatArg
  private var pixmap:          Nullable[Pixmap] = preloadedPixmap
  private var isPreparedState: Boolean          = false

  // Initialize from constructor parameters
  pixmap.foreach { p =>
    width = p.getWidth()
    height = p.getHeight()
    if (format.isEmpty) this.format = Nullable(p.getFormat())
  }

  override def isPrepared: Boolean = isPreparedState

  override def prepare(): Unit = {
    if (isPreparedState) throw SgeError.InvalidInput("Already prepared")
    if (pixmap.isEmpty) {
      if (file.extension().equals("cim"))
        pixmap = Nullable(PixmapIO.readCIM(file))
      else
        pixmap = Nullable(Pixmap(file))
    }
    pixmap.foreach { p =>
      width = p.getWidth()
      height = p.getHeight()
      if (format.isEmpty) format = Nullable(p.getFormat())
    }
    isPreparedState = true
  }

  override def consumePixmap(): Pixmap = {
    if (!isPreparedState) throw SgeError.InvalidInput("Call prepare() before calling getPixmap()")
    isPreparedState = false
    val pixmapToReturn = this.pixmap.getOrElse(throw SgeError.InvalidInput("No pixmap available"))
    this.pixmap = Nullable.empty
    pixmapToReturn
  }

  override def disposePixmap: Boolean = true

  override def getWidth: Int = width

  override def getHeight: Int = height

  override def getFormat: Format = format.getOrElse(Format.RGBA8888)

  override def useMipMaps: Boolean = useMipMapsParam

  override def isManaged: Boolean = true

  def getFileHandle(): FileHandle = file

  override def getType(): TextureData.TextureDataType = TextureData.TextureDataType.Pixmap

  override def consumeCustomData(target: Int): Unit =
    throw SgeError.InvalidInput("This TextureData implementation does not upload data itself")

  override def toString(): String = file.toString()
}
