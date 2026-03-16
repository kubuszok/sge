/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/ETC1TextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: fields use Nullable[A] instead of raw null
 *   Idiom: split packages
 *   Fixes: consumeCustomData() fully implemented with ETC1 decode fallback + compressed upload; added (using Sge)
 *   Convention: typed GL enums — TextureTarget for glTexImage2D/glCompressedTexImage2D/glGenerateMipmap
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
import sge.graphics.TextureData
import sge.graphics.glutils.ETC1.ETC1Data
import sge.utils.{ Nullable, SgeError }

class ETC1TextureData(
  val file:            Nullable[FileHandle],
  var useMipMapsValue: Boolean
)(using Sge)
    extends TextureData {

  private var data:         Nullable[ETC1Data] = Nullable.empty
  private var width:        Int                = 0
  private var height:       Int                = 0
  private var preparedFlag: Boolean            = false

  def this(file: FileHandle)(using Sge) = {
    this(Nullable(file), false)
  }

  def this(encodedImage: ETC1Data, useMipMaps: Boolean)(using Sge) = {
    this(file = Nullable.empty, useMipMapsValue = useMipMaps)
    this.data = Nullable(encodedImage)
  }

  override def getType(): TextureData.TextureDataType =
    TextureData.TextureDataType.Custom

  override def isPrepared: Boolean =
    preparedFlag

  override def prepare(): Unit = {
    if (preparedFlag) throw SgeError.GraphicsError("Already prepared")
    if (file.isEmpty && data.isEmpty) throw SgeError.GraphicsError("Can only load once from ETC1Data")
    file.foreach { f =>
      data = Nullable(ETC1Data(f))
    }
    data.foreach { d =>
      width = d.width
      height = d.height
    }
    preparedFlag = true
  }

  override def consumeCustomData(target: TextureTarget): Unit = {
    if (!preparedFlag) throw SgeError.GraphicsError("Call prepare() before calling consumeCompressedData()")

    val gl = Sge().graphics
    data.foreach { d =>
      if (!gl.supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
        val pixmap = ETC1.decodeImage(d, Format.RGB565)
        gl.gl.glTexImage2D(
          target,
          0,
          pixmap.getGLInternalFormat(),
          pixmap.getWidth(),
          pixmap.getHeight(),
          0,
          PixelFormat(pixmap.getGLFormat()),
          DataType(pixmap.getGLType()),
          pixmap.getPixels()
        )
        if (useMipMapsValue) MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth().toInt, pixmap.getHeight().toInt)
        pixmap.close()
        useMipMapsValue = false
      } else {
        gl.gl.glCompressedTexImage2D(target, 0, ETC1.ETC1_RGB8_OES, Pixels(width), Pixels(height), 0, d.compressedData.capacity() - d.dataOffset, d.compressedData)
        if (useMipMapsValue) gl.gl20.glGenerateMipmap(TextureTarget.Texture2D)
      }
    }
    data.foreach(_.close())
    data = Nullable.empty
    preparedFlag = false
  }

  override def consumePixmap(): Pixmap =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def disposePixmap: Boolean =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def getWidth: Int =
    width

  override def getHeight: Int =
    height

  override def getFormat: Format =
    Format.RGB565

  override def useMipMaps: Boolean =
    useMipMapsValue

  override def isManaged: Boolean =
    true
}
