/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/CustomTexture3DData.java
 * Original authors: mgsx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.ByteBuffer

import sge.graphics.GL20
import sge.graphics.GL30
import sge.graphics.Texture3DData
import sge.utils.BufferUtils
import sge.utils.SgeError

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
  private val width:            Int,
  private val height:           Int,
  private val depth:            Int,
  private val mipMapLevel:      Int,
  private val glFormat:         Int,
  private val glInternalFormat: Int,
  private val glType:           Int
)(using sde: Sge)
    extends Texture3DData {

  private var pixels: ByteBuffer = scala.compiletime.uninitialized

  override def isPrepared(): Boolean = true

  override def prepare(): Unit = {}

  override def getWidth(): Int = width

  override def getHeight(): Int = height

  def getDepth(): Int = depth

  override def useMipMaps(): Boolean = false

  override def isManaged(): Boolean = pixels != null

  def getInternalFormat(): Int = glInternalFormat

  def getGLType(): Int = glType

  def getGLFormat(): Int = glFormat

  def getMipMapLevel(): Int = mipMapLevel

  def getPixels(): ByteBuffer = {
    if (pixels == null) {
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
      pixels = BufferUtils.newByteBuffer(width * height * depth * bytesPerPixel)
    }
    pixels
  }

  override def consume3DData(): Unit = {
    val gl30 = sde.graphics.gl30
    gl30.fold(
      onEmpty = throw SgeError.GraphicsError("GL30 is not available")
    )(
      onSome = gl => gl.glTexImage3D(GL30.GL_TEXTURE_3D, mipMapLevel, glInternalFormat, width, height, depth, 0, glFormat, glType, pixels)
    )
  }
}
