/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/KTXTextureData.java
 * Original authors: Vincent Bousquet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.zip.GZIPInputStream

import sge.files.FileHandle
import sge.graphics.{ GL20, Pixmap, TextureData }
import sge.graphics.Pixmap.Format
import sge.graphics.TextureData.TextureDataType
import sge.graphics.glutils.ETC1.ETC1Data
import sge.utils.BufferUtils
import sge.utils.SgeError
import sge.utils.StreamUtils
import sge.utils.Nullable
import sge.Sge

/** A KTXTextureData holds the data from a KTX (or zipped KTX file, aka ZKTX). That is to say an OpenGL ready texture data. The KTX file format is just a thin wrapper around OpenGL textures and
  * therefore is compatible with most OpenGL texture capabilities like texture compression, cubemapping, mipmapping, etc.
  *
  * For example, KTXTextureData can be used for {@link Texture} or {@link Cubemap} .
  *
  * @author
  *   Vincent Bousquet
  */
class KTXTextureData(file: FileHandle, useMipMapsParam: Boolean)(using sge: Sge) extends TextureData, CubemapData {

  // KTX header (only available after preparing)
  private var glType:               Int = scala.compiletime.uninitialized
  private var glTypeSize:           Int = scala.compiletime.uninitialized
  private var glFormat:             Int = scala.compiletime.uninitialized
  private var glInternalFormat:     Int = scala.compiletime.uninitialized
  private var glBaseInternalFormat: Int = scala.compiletime.uninitialized
  private var pixelWidth  = -1
  private var pixelHeight = -1
  private var pixelDepth  = -1
  private var numberOfArrayElements: Int = scala.compiletime.uninitialized
  private var numberOfFaces:         Int = scala.compiletime.uninitialized
  private var numberOfMipmapLevels:  Int = scala.compiletime.uninitialized
  private var imagePos:              Int = scala.compiletime.uninitialized

  // KTX image data (only available after preparing and before consuming)
  private var compressedData: Nullable[ByteBuffer] = Nullable.empty

  override def getType(): TextureDataType = TextureDataType.Custom

  override def isPrepared: Boolean = compressedData.isDefined

  override def prepare(): Unit = {
    if (compressedData.isDefined) throw SgeError.GraphicsError("Already prepared")
    // We support normal ktx files as well as 'zktx' which are gzip ktx file with an int length at the beginning (like ETC1).
    if (file.name().endsWith(".zktx")) {
      val buffer = new Array[Byte](1024 * 10)
      val in     = new DataInputStream(new BufferedInputStream(new GZIPInputStream(file.read())))
      try {
        val fileSize = in.readInt()
        val cd       = BufferUtils.newUnsafeByteBuffer(fileSize)
        compressedData = Nullable(cd)
        var readBytes = in.read(buffer)
        while (readBytes != -1) {
          cd.put(buffer, 0, readBytes)
          readBytes = in.read(buffer)
        }
        cd.asInstanceOf[Buffer].position(0)
        cd.asInstanceOf[Buffer].limit(cd.capacity())
      } catch {
        case e: Exception => throw SgeError.GraphicsError(s"Couldn't load zktx file '$file'", Some(e))
      } finally
        StreamUtils.closeQuietly(in)
    } else {
      compressedData = Nullable(ByteBuffer.wrap(file.readBytes()))
    }
    val cd = compressedData.getOrElse(throw SgeError.GraphicsError("Failed to load KTX data"))
    if (cd.get() != 0x0ab.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x04b.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x054.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x058.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x020.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x031.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x031.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x0bb.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x00d.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x00a.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x01a.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    if (cd.get() != 0x00a.toByte) throw SgeError.GraphicsError("Invalid KTX Header")
    val endianTag = cd.getInt()
    if (endianTag != 0x04030201 && endianTag != 0x01020304) throw SgeError.GraphicsError("Invalid KTX Header")
    if (endianTag != 0x04030201)
      cd.order(if (cd.order() == ByteOrder.BIG_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
    glType = cd.getInt()
    glTypeSize = cd.getInt()
    glFormat = cd.getInt()
    glInternalFormat = cd.getInt()
    glBaseInternalFormat = cd.getInt()
    pixelWidth = cd.getInt()
    pixelHeight = cd.getInt()
    pixelDepth = cd.getInt()
    numberOfArrayElements = cd.getInt()
    numberOfFaces = cd.getInt()
    numberOfMipmapLevels = cd.getInt()
    if (numberOfMipmapLevels == 0) {
      numberOfMipmapLevels = 1
      // useMipMapsParam = true // This was a val parameter, can't be reassigned
    }
    val bytesOfKeyValueData = cd.getInt()
    imagePos = cd.position() + bytesOfKeyValueData
    if (!cd.isDirect()) {
      var pos = imagePos
      for (level <- 0 until numberOfMipmapLevels) {
        val faceLodSize        = cd.getInt(pos)
        val faceLodSizeRounded = (faceLodSize + 3) & ~3
        pos += faceLodSizeRounded * numberOfFaces + 4
      }
      cd.asInstanceOf[Buffer].limit(pos)
      cd.asInstanceOf[Buffer].position(0)
      val directBuffer = BufferUtils.newUnsafeByteBuffer(pos)
      directBuffer.order(cd.order())
      directBuffer.put(cd)
      compressedData = Nullable(directBuffer)
    }
  }

  def consumeCubemapData(): Unit =
    consumeCustomData(GL20.GL_TEXTURE_CUBE_MAP)

  override def consumeCustomData(target: Int): Unit = {
    if (compressedData.isEmpty) throw SgeError.GraphicsError("Call prepare() before calling consumeCompressedData()")
    val buffer = BufferUtils.newIntBuffer(16)

    // Check OpenGL type and format, detect compressed data format (no type & format)
    val compressed = glType == 0 || glFormat == 0
    if (compressed) {
      if (glType + glFormat != 0) throw SgeError.GraphicsError("either both or none of glType, glFormat must be zero")
    }

    // find OpenGL texture target and dimensions
    var textureDimensions = 1
    var glTarget          = KTXTextureData.GL_TEXTURE_1D
    if (pixelHeight > 0) {
      textureDimensions = 2
      glTarget = GL20.GL_TEXTURE_2D
    }
    if (pixelDepth > 0) {
      textureDimensions = 3
      glTarget = KTXTextureData.GL_TEXTURE_3D
    }
    if (numberOfFaces == 6) {
      if (textureDimensions == 2)
        glTarget = GL20.GL_TEXTURE_CUBE_MAP
      else
        throw SgeError.GraphicsError("cube map needs 2D faces")
    } else if (numberOfFaces != 1) {
      throw SgeError.GraphicsError("numberOfFaces must be either 1 or 6")
    }
    if (numberOfArrayElements > 0) {
      if (glTarget == KTXTextureData.GL_TEXTURE_1D)
        glTarget = KTXTextureData.GL_TEXTURE_1D_ARRAY_EXT
      else if (glTarget == GL20.GL_TEXTURE_2D)
        glTarget = KTXTextureData.GL_TEXTURE_2D_ARRAY_EXT
      else
        throw SgeError.GraphicsError("No API for 3D and cube arrays yet")
      textureDimensions += 1
    }
    if (glTarget == 0x1234)
      throw SgeError.GraphicsError("Unsupported texture format (only 2D texture are supported in LibGdx for the time being)")

    var singleFace   = -1
    var actualTarget = target
    if (numberOfFaces == 6 && target != GL20.GL_TEXTURE_CUBE_MAP) {
      // Load a single face of the cube (should be avoided since the data is unloaded afterwards)
      if (!(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X <= target && target <= GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z))
        throw SgeError.GraphicsError(
          "You must specify either GL_TEXTURE_CUBE_MAP to bind all 6 faces of the cube or the requested face GL_TEXTURE_CUBE_MAP_POSITIVE_X and followings."
        )
      singleFace = target - GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
      actualTarget = GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
    } else if (numberOfFaces == 6 && target == GL20.GL_TEXTURE_CUBE_MAP) {
      // Load the 6 faces
      actualTarget = GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X
    } else {
      // Load normal texture
      if (
        target != glTarget && !(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X <= target
          && target <= GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z && target == GL20.GL_TEXTURE_2D)
      )
        throw SgeError.GraphicsError(s"Invalid target requested : 0x${Integer.toHexString(target)}, expecting : 0x${Integer.toHexString(glTarget)}")
    }

    // KTX files require an unpack alignment of 4
    sge.graphics.gl.glGetIntegerv(GL20.GL_UNPACK_ALIGNMENT, buffer)
    val previousUnpackAlignment = buffer.get(0)
    if (previousUnpackAlignment != 4) sge.graphics.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4)
    val localGlInternalFormat = this.glInternalFormat
    val localGlFormat         = this.glFormat
    val cd                    = compressedData.getOrElse(throw SgeError.GraphicsError("Call prepare() before calling consumeCompressedData()"))
    var pos                   = imagePos
    for (level <- 0 until numberOfMipmapLevels) {
      val levelPixelWidth  = Math.max(1, this.pixelWidth >> level)
      var levelPixelHeight = Math.max(1, this.pixelHeight >> level)
      var levelPixelDepth  = Math.max(1, this.pixelDepth >> level)
      cd.asInstanceOf[Buffer].position(pos)
      val faceLodSize        = cd.getInt()
      val faceLodSizeRounded = (faceLodSize + 3) & ~3
      pos += 4
      for (face <- 0 until numberOfFaces) {
        cd.asInstanceOf[Buffer].position(pos)
        pos += faceLodSizeRounded
        if (singleFace != -1 && singleFace != face) {
          // continue to next iteration
        } else {
          val data = cd.slice()
          data.asInstanceOf[Buffer].limit(faceLodSizeRounded)
          if (textureDimensions == 1) {
            // if (compressed)
            // sge.graphics.gl.glCompressedTexImage1D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, 0, faceLodSize, data)
            // else
            // sge.graphics.gl.glTexImage1D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, 0, localGlFormat, glType, data)
          } else if (textureDimensions == 2) {
            if (numberOfArrayElements > 0) levelPixelHeight = numberOfArrayElements
            if (compressed) {
              if (localGlInternalFormat == ETC1.ETC1_RGB8_OES) {
                if (!sge.graphics.supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
                  val etcData = new ETC1Data(levelPixelWidth, levelPixelHeight, data, 0)
                  val pixmap  = ETC1.decodeImage(etcData, Format.RGB888)
                  sge.graphics.gl.glTexImage2D(
                    actualTarget + face,
                    level,
                    pixmap.getGLInternalFormat(),
                    pixmap.getWidth(),
                    pixmap.getHeight(),
                    0,
                    pixmap.getGLFormat(),
                    pixmap.getGLType(),
                    pixmap.getPixels()
                  )
                  pixmap.close()
                } else {
                  sge.graphics.gl.glCompressedTexImage2D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, levelPixelHeight, 0, faceLodSize, data)
                }
              } else {
                // Try to load (no software unpacking fallback)
                sge.graphics.gl.glCompressedTexImage2D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, levelPixelHeight, 0, faceLodSize, data)
              }
            } else
              sge.graphics.gl.glTexImage2D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, levelPixelHeight, 0, localGlFormat, glType, data)
          } else if (textureDimensions == 3) {
            if (numberOfArrayElements > 0) levelPixelDepth = numberOfArrayElements
            // if (compressed)
            // sge.graphics.gl.glCompressedTexImage3D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, levelPixelHeight, levelPixelDepth, 0, faceLodSize, data)
            // else
            // sge.graphics.gl.glTexImage3D(actualTarget + face, level, localGlInternalFormat, levelPixelWidth, levelPixelHeight, levelPixelDepth, 0, localGlFormat, glType, data)
          }
        }
      }
    }
    if (previousUnpackAlignment != 4) sge.graphics.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, previousUnpackAlignment)
    if (useMipMaps) sge.graphics.gl.glGenerateMipmap(actualTarget)

    // dispose data once transfered to GPU
    disposePreparedData()
  }

  def disposePreparedData(): Unit = {
    compressedData.foreach(BufferUtils.disposeUnsafeByteBuffer)
    compressedData = Nullable.empty
  }

  override def consumePixmap(): Pixmap =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def disposePixmap: Boolean =
    throw SgeError.GraphicsError("This TextureData implementation does not return a Pixmap")

  override def getWidth: Int = pixelWidth

  override def getHeight: Int = pixelHeight

  def getNumberOfMipMapLevels(): Int = numberOfMipmapLevels

  def getNumberOfFaces(): Int = numberOfFaces

  def getGlInternalFormat(): Int = glInternalFormat

  def getData(requestedLevel: Int, requestedFace: Int): Nullable[ByteBuffer] = {
    val cd  = compressedData.getOrElse(throw SgeError.GraphicsError("No data available — call prepare() first"))
    var pos = imagePos
    var result: Nullable[ByteBuffer] = Nullable.empty

    for (level <- 0 until numberOfMipmapLevels if result.isEmpty) {
      val faceLodSize        = cd.getInt(pos)
      val faceLodSizeRounded = (faceLodSize + 3) & ~3
      pos += 4
      if (level == requestedLevel) {
        for (face <- 0 until numberOfFaces if result.isEmpty) {
          if (face == requestedFace) {
            cd.asInstanceOf[Buffer].position(pos)
            val data = cd.slice()
            data.asInstanceOf[Buffer].limit(faceLodSizeRounded)
            result = Nullable(data)
          }
          pos += faceLodSizeRounded
        }
      } else {
        pos += faceLodSizeRounded * numberOfFaces
      }
    }
    result
  }

  override def getFormat: Format =
    throw SgeError.GraphicsError("This TextureData implementation directly handles texture formats.")

  override def useMipMaps: Boolean = useMipMapsParam

  override def isManaged: Boolean = true
}

object KTXTextureData {
  private val GL_TEXTURE_1D           = 0x1234
  private val GL_TEXTURE_3D           = 0x1234
  private val GL_TEXTURE_1D_ARRAY_EXT = 0x1234
  private val GL_TEXTURE_2D_ARRAY_EXT = 0x1234
}
