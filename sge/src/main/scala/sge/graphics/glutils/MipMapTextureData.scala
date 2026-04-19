/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/MipMapTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: varargs constructor; mips is Array[TextureData]
 *   Idiom: split packages
 *   Convention: constructor takes (using Sge) to propagate context into consumeCustomData for uploadImageData call
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 55
 * Covenant-baseline-methods: MipMapTextureData,consumeCustomData,consumePixmap,dataType,disposePixmap,getFormat,height,isManaged,isPrepared,mips,prepare,useMipMaps,width
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/MipMapTextureData.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package glutils

import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.graphics.TextureData
import sge.graphics.TextureData.TextureDataType
import sge.graphics.GLTexture
import sge.utils.SgeError

/** This class will load each contained TextureData to the chosen mipmap level. All the mipmap levels must be defined and cannot be null.
  */
class MipMapTextureData(mipMapData: TextureData*)(using Sge) extends TextureData {
  val mips: Array[TextureData] = mipMapData.toArray

  override def dataType: TextureDataType = TextureDataType.Custom

  override def isPrepared: Boolean = true

  override def prepare(): Unit = {}

  override def consumePixmap(): Pixmap =
    throw SgeError.GraphicsError("It's compressed, use the compressed method")

  override def disposePixmap: Boolean = false

  override def consumeCustomData(target: TextureTarget): Unit =
    for (i <- mips.indices)
      GLTexture.uploadImageData(target, mips(i), i)

  override def width: Int = mips(0).width

  override def height: Int = mips(0).height

  override def getFormat: Format = mips(0).getFormat

  override def useMipMaps: Boolean = false

  override def isManaged: Boolean = true
}
