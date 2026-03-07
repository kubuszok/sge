/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FloatTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: buffer is Nullable[FloatBuffer]; @nowarn for orNull at GL interop boundary
 *   Idiom: split packages
 *   Issues: prepare() checks all GL formats regardless of GL type (Java guards on GLVersion.Type == OpenGL); getBuffer() returns Nullable[FloatBuffer] instead of raw FloatBuffer
 *   Convention: typed GL enums (TextureTarget, PixelFormat, DataType) for consumeCustomData and glTexImage2D
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.FloatBuffer

import sge.Application.ApplicationType
import sge.graphics.GL20
import sge.graphics.GL30
import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.graphics.TextureData
import sge.utils.BufferUtils
import sge.utils.SgeError
import sge.utils.Nullable
import scala.annotation.nowarn
import sge.Sge

/** A {@link TextureData} implementation which should be used to create float textures. */
class FloatTextureData(
  val width:          Int,
  val height:         Int,
  val internalFormat: Int,
  val format:         PixelFormat,
  val `type`:         DataType,
  val isGpuOnly:      Boolean
)(using Sge)
    extends TextureData {

  private var isPreparedState: Boolean               = false
  private var buffer:          Nullable[FloatBuffer] = Nullable.empty

  override def getType(): TextureData.TextureDataType = TextureData.TextureDataType.Custom

  override def isPrepared: Boolean = isPreparedState

  override def prepare(): Unit = {
    if (isPreparedState) throw SgeError.InvalidInput("Already prepared")
    if (!isGpuOnly) {
      var amountOfFloats = 4
      // Determine amount of floats based on internal format
      if (internalFormat == GL30.GL_RGBA16F || internalFormat == GL30.GL_RGBA32F) amountOfFloats = 4
      if (internalFormat == GL30.GL_RGB16F || internalFormat == GL30.GL_RGB32F) amountOfFloats = 3
      if (internalFormat == GL30.GL_RG16F || internalFormat == GL30.GL_RG32F) amountOfFloats = 2
      if (internalFormat == GL30.GL_R16F || internalFormat == GL30.GL_R32F) amountOfFloats = 1

      this.buffer = Nullable(BufferUtils.newFloatBuffer(width * height * amountOfFloats))
    }
    isPreparedState = true
  }

  override def consumeCustomData(target: TextureTarget): Unit =
    if (
      Sge().application.getType() == ApplicationType.Android || Sge().application.getType() == ApplicationType.iOS
      || (Sge().application.getType() == ApplicationType.WebGL && !Sge().graphics.isGL30Available())
    ) {

      if (!Sge().graphics.supportsExtension("OES_texture_float"))
        throw SgeError.GraphicsError("Extension OES_texture_float not supported!")

      // GLES and WebGL defines texture format by 3rd and 8th argument,
      // so to get a float texture one needs to supply GL_RGBA and GL_FLOAT there.
      // orNull required: GL20.glTexImage2D Java API accepts null buffer for GPU-only allocation
      @nowarn("msg=deprecated") val buf1 = buffer.orNull
      Sge().graphics.gl.glTexImage2D(target, 0, GL20.GL_RGBA, Pixels(width), Pixels(height), 0, PixelFormat.RGBA, DataType.Float, buf1)

    } else {
      if (!Sge().graphics.isGL30Available()) {
        if (!Sge().graphics.supportsExtension("GL_ARB_texture_float"))
          throw SgeError.GraphicsError("Extension GL_ARB_texture_float not supported!")
      }
      // in desktop OpenGL the texture format is defined only by the third argument,
      // hence we need to use GL_RGBA32F there (this constant is unavailable in GLES/WebGL)
      // orNull required: GL20.glTexImage2D Java API accepts null buffer for GPU-only allocation
      @nowarn("msg=deprecated") val buf2 = buffer.orNull
      Sge().graphics.gl.glTexImage2D(target, 0, internalFormat, Pixels(width), Pixels(height), 0, format, DataType.Float, buf2)
    }

  override def consumePixmap(): Pixmap =
    throw SgeError.InvalidInput("This TextureData implementation does not return a Pixmap")

  override def disposePixmap: Boolean =
    throw SgeError.InvalidInput("This TextureData implementation does not return a Pixmap")

  override def getWidth: Int = width

  override def getHeight: Int = height

  override def getFormat: Format = Format.RGBA8888 // it's not true, but FloatTextureData.getFormat() isn't used anywhere

  override def useMipMaps: Boolean = false

  override def isManaged: Boolean = true

  def getBuffer(): Nullable[FloatBuffer] = buffer
}
