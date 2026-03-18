/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/CustomTexture3DData.java
 * Original authors: mgsx
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: constructor params are private vals; Nullable used instead of null for isManaged check
 *   Idiom: split packages
 *   Convention: typed GL enums — TextureTarget, PixelFormat, DataType for glTexImage3D
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.ByteBuffer

import sge.graphics.GL20
import sge.graphics.GL30
import sge.graphics.Texture3DData
import sge.utils.{ BufferUtils, Nullable, SgeError }

/** A {@link Texture3DData} implementation that addresses 2 use cases :
  *
  * You can use it as a GL only texture (feed by a compute shader). In this case the texture is not managed.
  *
  * Or you can use it to upload pixels to GPU. In this case you should call {@link #getPixels()} to fill the buffer prior to consuming it (eg. before new Texture3D(data)).
  *
  * @author
  *   mgsx
  */
class CustomTexture3DData(
  val width:            Int,
  val height:           Int,
  val depth:            Int,
  val mipMapLevel:      Int,
  val glFormat:         Int,
  val glInternalFormat: Int,
  val glType:           Int
)(using Sge)
    extends Texture3DData {

  private var managed = false

  lazy val pixels: ByteBuffer = {
    val numChannels = glFormat match {
      case GL30.GL_RED | GL30.GL_RED_INTEGER | GL20.GL_LUMINANCE | GL20.GL_ALPHA => 1
      case GL30.GL_RG | GL30.GL_RG_INTEGER | GL20.GL_LUMINANCE_ALPHA             => 2
      case GL20.GL_RGB | GL30.GL_RGB_INTEGER                                     => 3
      case GL20.GL_RGBA | GL30.GL_RGBA_INTEGER                                   => 4
      case _                                                                     => throw SgeError.InvalidInput(s"unsupported glFormat: $glFormat")
    }

    val bytesPerChannel = glType match {
      case GL20.GL_UNSIGNED_BYTE | GL20.GL_BYTE                        => 1
      case GL20.GL_UNSIGNED_SHORT | GL20.GL_SHORT | GL30.GL_HALF_FLOAT => 2
      case GL20.GL_UNSIGNED_INT | GL20.GL_INT | GL20.GL_FLOAT          => 4
      case _                                                           => throw SgeError.InvalidInput(s"unsupported glType: $glType")
    }

    val bytesPerPixel = numChannels * bytesPerChannel
    managed = true
    BufferUtils.newByteBuffer(width * height * depth * bytesPerPixel)
  }

  override def internalFormat: Int = glInternalFormat

  override def isPrepared: Boolean = true

  override def prepare(): Unit = {}

  override def useMipMaps: Boolean = false

  override def isManaged: Boolean = managed

  override def consume3DData(): Unit = {
    val gl30 = Sge().graphics.gl30
    gl30.fold(
      onEmpty = throw SgeError.GraphicsError("GL30 is not available")
    )(
      onSome = gl =>
        gl.glTexImage3D(
          TextureTarget.Texture3D,
          mipMapLevel,
          glInternalFormat,
          width,
          height,
          depth,
          0,
          PixelFormat(glFormat),
          DataType(glType),
          pixels
        )
    )
  }
}
