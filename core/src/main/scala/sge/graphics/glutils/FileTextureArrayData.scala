/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FileTextureArrayData.java
 * Original authors: Tomski
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: constructor uses varargs files: FileHandle* instead of FileHandle[]; boundary/break for early return
 *   Idiom: split packages
 *   Issues: missing second constructor (Format, Boolean, TextureData[]); prepare() lacks isPrepared() guard present in Java source
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
  private var depth:         Int                = files.length
  private val useMipMapsVar: Boolean            = useMipMaps

  // Constructor body (equivalent to the Java constructor)
  locally {
    this.depth = files.length
    textureDatas = Array.ofDim[TextureData](files.length)
    for (i <- files.indices)
      textureDatas(i) = TextureData.Factory.loadFromFile(files(i), Nullable(format), useMipMaps)
  }

  override def isPrepared(): Boolean = prepared

  override def prepare(): Unit = {
    var width  = -1
    var height = -1
    for (data <- textureDatas) {
      data.prepare()
      if (width == -1) {
        width = data.getWidth
        height = data.getHeight
      } else if (width != data.getWidth || height != data.getHeight) {
        throw SgeError.GraphicsError("Error whilst preparing TextureArray: TextureArray Textures must have equal dimensions.")
      }
    }
    prepared = true
  }

  override def consumeTextureArrayData(): Unit = {
    var containsCustomData = false
    for (i <- textureDatas.indices)
      if (textureDatas(i).getType() == TextureData.TextureDataType.Custom) {
        textureDatas(i).consumeCustomData(GL30.GL_TEXTURE_2D_ARRAY)
        containsCustomData = true
      } else {
        val texData       = textureDatas(i)
        var pixmap        = texData.consumePixmap()
        var disposePixmap = texData.disposePixmap
        if (texData.getFormat != pixmap.getFormat()) {
          val temp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), texData.getFormat)
          temp.setBlending(Pixmap.Blending.None)
          temp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight())
          if (texData.disposePixmap) {
            pixmap.close()
          }
          pixmap = temp
          disposePixmap = true
        }
        // orNull required: GL30 Java API needs direct reference for OpenGL call
        @nowarn("msg=deprecated") val gl30 = Sge().graphics.gl30.orNull
        gl30.glTexSubImage3D(
          GL30.GL_TEXTURE_2D_ARRAY,
          0,
          0,
          0,
          i,
          pixmap.getWidth(),
          pixmap.getHeight(),
          1,
          pixmap.getGLInternalFormat(),
          pixmap.getGLType(),
          pixmap.getPixels()
        )
        if (disposePixmap) pixmap.close()
      }
    if (useMipMapsVar && !containsCustomData) {
      Sge().graphics.gl20.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY)
    }
  }

  override def getWidth(): Int = textureDatas(0).getWidth

  override def getHeight(): Int = textureDatas(0).getHeight

  override def getDepth(): Int = depth

  override def getInternalFormat(): Int = Pixmap.Format.toGlFormat(formatVar)

  override def getGLType(): Int = Pixmap.Format.toGlType(formatVar)

  override def isManaged(): Boolean = scala.util.boundary {
    for (data <- textureDatas)
      if (!data.isManaged) {
        scala.util.boundary.break(false)
      }
    true
  }
}
