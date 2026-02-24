/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/GLOnlyTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.Pixmap.Format
import sge.utils.SgeError
import sge.utils.Nullable

/** A TextureData implementation which should be used to create gl only textures. This TextureData fits perfectly for FrameBuffer. The data is not managed.
  */
class GLOnlyTextureData(using sge: Sge) extends TextureData {

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
  def this(width: Int, height: Int, mipMapLevel: Int, internalFormat: Int, format: Int, `type`: Int)(using sge: Sge) = {
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
    sge.graphics.gl.glTexImage2D(target, mipLevel, internalFormat, width, height, 0, format, `type`, null)

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
