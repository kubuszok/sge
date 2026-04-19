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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 92
 * Covenant-baseline-methods: FileTextureData,_height,_width,consumeCustomData,consumePixmap,dataType,disposePixmap,file,fileHandle,format,getFormat,height,isManaged,isPrepared,isPreparedState,pixmap,pixmapToReturn,prepare,toString,useMipMaps,useMipMapsParam,width
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/FileTextureData.java
 * Covenant-verified: 2026-04-19
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

  private var _width:          Int              = 0
  private var _height:         Int              = 0
  private var format:          Nullable[Format] = formatArg
  private var pixmap:          Nullable[Pixmap] = preloadedPixmap
  private var isPreparedState: Boolean          = false

  // Initialize from constructor parameters
  pixmap.foreach { p =>
    _width = p.width.toInt
    _height = p.height.toInt
    if (format.isEmpty) this.format = Nullable(p.format)
  }

  override def isPrepared: Boolean = isPreparedState

  override def prepare(): Unit = {
    if (isPreparedState) throw SgeError.InvalidInput("Already prepared")
    if (pixmap.isEmpty) {
      if (file.extension.equals("cim"))
        pixmap = Nullable(PixmapIO.readCIM(file))
      else
        pixmap = Nullable(Pixmap(file))
    }
    pixmap.foreach { p =>
      _width = p.width.toInt
      _height = p.height.toInt
      if (format.isEmpty) format = Nullable(p.format)
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

  override def width: Int = _width

  override def height: Int = _height

  override def getFormat: Format = format.getOrElse(Format.RGBA8888)

  override def useMipMaps: Boolean = useMipMapsParam

  override def isManaged: Boolean = true

  def fileHandle: FileHandle = file

  override def dataType: TextureData.TextureDataType = TextureData.TextureDataType.Pixmap

  override def consumeCustomData(target: TextureTarget): Unit =
    throw SgeError.InvalidInput("This TextureData implementation does not upload data itself")

  override def toString(): String = file.toString()
}
