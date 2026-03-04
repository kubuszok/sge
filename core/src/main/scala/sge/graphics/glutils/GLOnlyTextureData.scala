/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/GLOnlyTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses (using Sge) for GL calls; raw null in glTexImage2D is correct Java interop for GPU-only allocation
 *   Idiom: split packages
 *   TODO: typed GL enums -- TextureTarget, PixelFormat, DataType -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.Pixmap.Format
import sge.utils.SgeError

/** A TextureData implementation which should be used to create gl only textures. This TextureData fits perfectly for FrameBuffer. The data is not managed.
  */
class GLOnlyTextureData(using Sge) extends TextureData {

  /** width and height */
  var width:                 Int     = 0
  var height:                Int     = 0
  private var preparedState: Boolean = false

  /** properties of opengl texture */
  var mipLevel:       Int = 0
  var internalFormat: Int = scala.compiletime.uninitialized
  var format:         Int = scala.compiletime.uninitialized
  var `type`:         Int = scala.compiletime.uninitialized

  /** @see
    *   "https://www.khronos.org/opengles/sdk/docs/man/xhtml/glTexImage2D.xml"
    * @param internalFormat
    *   Specifies the internal format of the texture. Must be one of the following symbolic constants: GL20.GL_ALPHA, GL20.GL_LUMINANCE, GL20.GL_LUMINANCE_ALPHA, GL20.GL_RGB, GL20.GL_RGBA.
    * @param format
    *   Specifies the format of the texel data. Must match internalformat. The following symbolic values are accepted: GL20.GL_ALPHA, GL20.GL_RGB, GL20.GL_RGBA, GL20.GL_LUMINANCE, and
    *   GL20.GL_LUMINANCE_ALPHA.
    * @param type
    *   Specifies the data type of the texel data. The following symbolic values are accepted: GL20.GL_UNSIGNED_BYTE, GL20.GL_UNSIGNED_SHORT_5_6_5, GL20.GL_UNSIGNED_SHORT_4_4_4_4, and
    *   GL20.GL_UNSIGNED_SHORT_5_5_5_1.
    */
  def this(width: Int, height: Int, mipMapLevel: Int, internalFormat: Int, format: Int, `type`: Int)(using Sge) = {
    this()
    this.width = width
    this.height = height
    this.mipLevel = mipMapLevel
    this.internalFormat = internalFormat
    this.format = format
    this.`type` = `type`
  }

  override def getType(): TextureData.TextureDataType = TextureData.TextureDataType.Custom

  override def isPrepared: Boolean = preparedState

  override def prepare(): Unit = {
    if (preparedState) throw SgeError.GraphicsError("Already prepared")
    preparedState = true
  }

  override def consumeCustomData(target: Int): Unit =
    Sge().graphics.gl.glTexImage2D(target, mipLevel, internalFormat, width, height, 0, format, `type`, null)

  override def consumePixmap(): Pixmap =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def disposePixmap: Boolean =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def getWidth: Int = width

  override def getHeight: Int = height

  override def getFormat: Format = Format.RGBA8888

  override def useMipMaps: Boolean = false

  override def isManaged: Boolean = false
}
