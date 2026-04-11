/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import java.nio.{ Buffer, ByteBuffer, ByteOrder, FloatBuffer, ShortBuffer }

import scala.collection.mutable.ArrayBuffer

import sge.{ Application, Pixels, Sge }
import sge.files.FileHandle
import sge.graphics.{ ClearMask, GL20, Pixmap, Texture }
import sge.graphics.g2d.SpriteBatch
import sge.graphics.glutils.FrameBuffer
import sge.gltf.data.data.{ GLTFBuffer, GLTFBufferView }
import sge.gltf.data.texture.GLTFImage
import sge.gltf.loaders.exceptions.{ GLTFRuntimeException, GLTFUnsupportedException }
import sge.utils.{ Nullable, ScreenUtils }

private[exporters] class GLTFBinaryExporter(
  private val folder: FileHandle,
  private val config: GLTFExporterConfig
) {

  private val buffers:       ArrayBuffer[ByteBuffer]     = ArrayBuffer.empty
  val views:                 ArrayBuffer[GLTFBufferView] = ArrayBuffer.empty
  private var currentBuffer: Nullable[Buffer]            = Nullable.empty

  def reset(): Unit = {
    buffers.clear()
    views.clear()
    currentBuffer = Nullable.empty
  }

  private def createBuffer(): ByteBuffer = {
    val buffer = ByteBuffer.allocate(config.maxBinaryFileSize)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer
  }

  private def begin(size: Int): ByteBuffer = {
    val buffer: ByteBuffer =
      if (buffers.isEmpty) {
        val b = createBuffer()
        buffers += b
        b
      } else {
        val b = buffers.last
        if (b.remaining() < size) {
          val nb = createBuffer()
          buffers += nb
          nb
        } else {
          b
        }
      }
    buffer
  }

  def beginFloats(count: Int): FloatBuffer = {
    val floatBuffer = begin(count * 4).asFloatBuffer()
    currentBuffer = Nullable(floatBuffer.asInstanceOf[Buffer])
    floatBuffer
  }

  def beginShorts(count: Int): ShortBuffer = {
    val shortBuffer = begin(count * 2).asShortBuffer()
    currentBuffer = Nullable(shortBuffer.asInstanceOf[Buffer])
    shortBuffer
  }

  /** end local buffering
    * @return
    *   GLTFBufferView id
    */
  def end(): Int = {
    val view = new GLTFBufferView()
    view.buffer = Nullable(buffers.size - 1)

    // update position
    val position = buffers.last.position()
    view.byteOffset = position
    val buf = currentBuffer.get
    val size: Int =
      if (buf.isInstanceOf[FloatBuffer]) {
        buf.position() * 4
      } else if (buf.isInstanceOf[ShortBuffer]) {
        buf.position() * 2
      } else {
        throw new GLTFUnsupportedException("bad buffer type")
      }
    currentBuffer = Nullable.empty
    view.byteLength = size
    val newPosition = position + size
    buffers.last.position(newPosition)
    views += view
    views.size - 1
  }

  def flushAllToFiles(baseName: String): ArrayBuffer[GLTFBuffer] = {
    val out   = ArrayBuffer[GLTFBuffer]()
    var count = 0
    for (b <- buffers) {
      val buffer = new GLTFBuffer()
      buffer.byteLength = b.position()
      buffer.uri = Nullable(if (buffers.size == 1) baseName + ".bin" else baseName + (count + 1) + ".bin")
      val bytes = new Array[Byte](b.position())
      b.flip()
      b.get(bytes)
      folder.child(buffer.uri.get).writeBytes(bytes, false)
      out += buffer
      count += 1
    }
    out
  }

  def exportImage(image: GLTFImage, texture: Texture, baseName: String)(using sge: Sge): Unit = {
    val fileName = baseName + ".png"
    image.uri = Nullable(fileName)
    val file = folder.child(fileName)
    val fbo  = new FrameBuffer(texture.textureData.getFormat, texture.width, texture.height, false)
    fbo.begin()
    sge.graphics.gl.glClearColor(0, 0, 0, 0)
    sge.graphics.gl.glClear(ClearMask.ColorBufferBit)
    val batch = new SpriteBatch()
    batch.projectionMatrix.setToOrtho2D(0, 0, 1, 1)
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
    batch.begin()
    batch.draw(texture, 0, 0, 1, 1, 0, 0, 1, 1)
    batch.end()
    val pixmap = ScreenUtils.getFrameBufferPixmap(Pixels(0), Pixels(0), texture.width, texture.height)
    fbo.end()
    batch.close()
    fbo.close()
    GLTFBinaryExporter.savePNG(file, pixmap)
    pixmap.close()
  }
}

private[exporters] object GLTFBinaryExporter {

  def savePNG(file: FileHandle, pixmap: Pixmap)(using sge: Sge): Unit =
    if (sge.application.applicationType == Application.ApplicationType.WebGL) {
      throw new GLTFUnsupportedException("saving pixmap not supported for WebGL")
    } else {
      // call PixmapIO.writePNG(file, pixmap); via reflection to
      // avoid compilation error with GWT.
      try {
        val pixmapIO = Class.forName("sge.graphics.PixmapIO")
        val writePNG = pixmapIO.getMethod("writePNG", classOf[FileHandle], classOf[Pixmap])
        writePNG.invoke(null, file, pixmap) // @nowarn — Java reflection interop requires null receiver for static method
      } catch {
        case e: Exception =>
          throw new GLTFRuntimeException(e)
      }
    }
}
