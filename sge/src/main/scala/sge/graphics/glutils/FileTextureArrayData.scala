/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FileTextureArrayData.java
 * Original authors: Tomski
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: constructor uses varargs files: FileHandle* instead of FileHandle[]; boundary/break for early return
 *   Idiom: split packages
 *   Convention: secondary constructor (Format, Boolean, TextureData[]) added for pre-built TextureData arrays
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.files.FileHandle
import sge.utils.Nullable
import scala.annotation.nowarn
import sge.utils.SgeError

/** @author Tomski * */
class FileTextureArrayData(format: Pixmap.Format, useMipMaps: Boolean, files: FileHandle*)(using Sge) extends TextureArrayData {

  private var textureDatas:  Array[TextureData] = scala.compiletime.uninitialized
  private var prepared:      Boolean            = false
  private val formatVar:     Pixmap.Format      = format
  private var _depth:        Int                = files.length
  private val useMipMapsVar: Boolean            = useMipMaps

  // Constructor body (equivalent to the Java FileHandle[] constructor)
  locally {
    this._depth = files.length
    textureDatas = Array.ofDim[TextureData](files.length)
    for (i <- files.indices)
      textureDatas(i) = TextureData.Factory.loadFromFile(files(i), Nullable(format), useMipMaps)
  }

  /** Secondary constructor accepting pre-built TextureData array (matches Java's {@code (Format, Boolean, TextureData[])} constructor) */
  def this(format: Pixmap.Format, useMipMaps: Boolean, textureDatas: Array[TextureData])(using Sge) = {
    this(format, useMipMaps)
    this._depth = textureDatas.length
    this.textureDatas = textureDatas
  }

  override def isPrepared: Boolean = prepared

  override def prepare(): Unit = {
    var width  = -1
    var height = -1
    for (data <- textureDatas) {
      if (!data.isPrepared) data.prepare()
      if (width == -1) {
        width = data.width
        height = data.height
      } else if (width != data.width || height != data.height) {
        throw SgeError.GraphicsError("Error whilst preparing TextureArray: TextureArray Textures must have equal dimensions.")
      }
    }
    prepared = true
  }

  override def consumeTextureArrayData(): Unit = {
    var containsCustomData = false
    for (i <- textureDatas.indices)
      if (textureDatas(i).dataType == TextureData.TextureDataType.Custom) {
        textureDatas(i).consumeCustomData(TextureTarget.Texture2DArray)
        containsCustomData = true
      } else {
        val texData       = textureDatas(i)
        var pixmap        = texData.consumePixmap()
        var disposePixmap = texData.disposePixmap
        if (texData.getFormat != pixmap.format) {
          val temp = Pixmap(pixmap.width.toInt, pixmap.height.toInt, texData.getFormat)
          temp.setBlending(Pixmap.Blending.None)
          temp.drawPixmap(pixmap, Pixels.zero, Pixels.zero, Pixels.zero, Pixels.zero, pixmap.width, pixmap.height)
          if (texData.disposePixmap) {
            pixmap.close()
          }
          pixmap = temp
          disposePixmap = true
        }
        // orNull required: GL30 Java API needs direct reference for OpenGL call
        @nowarn("msg=deprecated") val gl30 = Sge().graphics.gl30.orNull
        gl30.glTexSubImage3D(
          TextureTarget.Texture2DArray,
          0,
          0,
          0,
          i,
          pixmap.width.toInt,
          pixmap.height.toInt,
          1,
          PixelFormat(pixmap.gLInternalFormat),
          DataType(pixmap.glType),
          pixmap.pixels
        )
        if (disposePixmap) pixmap.close()
      }
    if (useMipMapsVar && !containsCustomData) {
      Sge().graphics.gl20.glGenerateMipmap(TextureTarget.Texture2DArray)
    }
  }

  override def width: Int = textureDatas(0).width

  override def height: Int = textureDatas(0).height

  override def depth: Int = _depth

  override def internalFormat: Int = Pixmap.Format.toGlFormat(formatVar)

  override def glType: Int = Pixmap.Format.toGlType(formatVar)

  override def isManaged: Boolean = scala.util.boundary {
    for (data <- textureDatas)
      if (!data.isManaged) {
        scala.util.boundary.break(false)
      }
    true
  }
}
