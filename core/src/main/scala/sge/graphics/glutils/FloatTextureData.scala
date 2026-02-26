/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FloatTextureData.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
import sge.graphics.glutils.GLVersion
import sge.utils.BufferUtils
import sge.utils.SgeError
import sge.utils.Nullable
import sge.Sge
import scala.compiletime.uninitialized

/** A {@link TextureData} implementation which should be used to create float textures. */
class FloatTextureData(
  val width:          Int,
  val height:         Int,
  val internalFormat: Int,
  val format:         Int,
  val `type`:         Int,
  val isGpuOnly:      Boolean
)(using sge: Sge)
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

  override def consumeCustomData(target: Int): Unit =
    if (
      sge.application.getType() == ApplicationType.Android || sge.application.getType() == ApplicationType.iOS
      || (sge.application.getType() == ApplicationType.WebGL && !sge.graphics.isGL30Available())
    ) {

      if (!sge.graphics.supportsExtension("OES_texture_float"))
        throw SgeError.GraphicsError("Extension OES_texture_float not supported!")

      // GLES and WebGL defines texture format by 3rd and 8th argument,
      // so to get a float texture one needs to supply GL_RGBA and GL_FLOAT there.
      sge.graphics.gl.glTexImage2D(target, 0, GL20.GL_RGBA, width, height, 0, GL20.GL_RGBA, GL20.GL_FLOAT, buffer.orNull)

    } else {
      if (!sge.graphics.isGL30Available()) {
        if (!sge.graphics.supportsExtension("GL_ARB_texture_float"))
          throw SgeError.GraphicsError("Extension GL_ARB_texture_float not supported!")
      }
      // in desktop OpenGL the texture format is defined only by the third argument,
      // hence we need to use GL_RGBA32F there (this constant is unavailable in GLES/WebGL)
      sge.graphics.gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, GL20.GL_FLOAT, buffer.orNull)
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
