/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 127
 * Covenant-baseline-methods: SeparatedDataFileResolver,bufferMap,bytes,decodePath,getBuffer,getImageFile,getRoot,glModel,i,load,loadBuffers,path,pos,src
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package gltf

import java.nio.{ ByteBuffer, ByteOrder }
import scala.collection.mutable.HashMap
import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.gltf.data.GLTF
import sge.gltf.data.data.GLTFBuffer
import sge.gltf.data.texture.GLTFImage
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.data.DataFileResolver
import sge.gltf.loaders.shared.texture.PixmapBinaryLoaderHack
import sge.utils.Nullable

class SeparatedDataFileResolver extends DataFileResolver {

  private val bufferMap: HashMap[Int, ByteBuffer] = HashMap.empty
  private var glModel:   Nullable[GLTF]           = Nullable.empty
  private var path:      Nullable[FileHandle]     = Nullable.empty

  override def load(file: FileHandle): Unit = {
    glModel = Nullable(GLTFJsonParser.parse(file.readString()))
    path = Nullable(file.parent())
    loadBuffers(path.get)
  }

  override def getRoot: GLTF = glModel.get

  private def loadBuffers(path: FileHandle): Unit =
    glModel.foreach { model =>
      model.buffers.foreach { buffers =>
        var i = 0
        while (i < buffers.size) {
          val glBuffer = buffers(i)
          val buffer   = ByteBuffer.allocate(glBuffer.byteLength)
          buffer.order(ByteOrder.LITTLE_ENDIAN)
          glBuffer.uri.foreach { uri =>
            if (uri.startsWith("data:")) {
              // data:application/octet-stream;base64,
              val parts = uri.split(",", 2)
              val body  = parts(1)
              val data  = java.util.Base64.getDecoder.decode(body)
              buffer.put(data)
            } else {
              val file = path.child(SeparatedDataFileResolver.decodePath(uri))
              buffer.put(file.readBytes())
            }
          }
          bufferMap.put(i, buffer)
          i += 1
        }
      }
    }

  override def getBuffer(buffer: Int): ByteBuffer = bufferMap(buffer)

  override def load(glImage: GLTFImage): Pixmap =
    if (glImage.uri.isEmpty) {
      // load from buffer view
      if (glImage.mimeType.isEmpty) {
        throw new GLTFIllegalException("GLTF image: both URI and mimeType cannot be null")
      }
      val mimeType = glImage.mimeType.get
      if (mimeType == "image/png" || mimeType == "image/jpeg") {
        val bufferView = glModel.get.bufferViews.get(glImage.bufferView.get)
        val data       = bufferMap(bufferView.buffer.get)
        val bytes      = new Array[Byte](bufferView.byteLength)
        data.position(bufferView.byteOffset)
        data.get(bytes, 0, bufferView.byteLength)
        data.rewind()
        PixmapBinaryLoaderHack.load(bytes, 0, bytes.length)
      } else {
        throw new GLTFIllegalException("GLTF image: unexpected mimeType: " + mimeType)
      }
    } else {
      val uri = glImage.uri.get
      if (uri.startsWith("data:")) {
        // data:application/octet-stream;base64,
        val parts = uri.split(",", 2)
        val body  = parts(1)
        val data  = java.util.Base64.getDecoder.decode(body)
        PixmapBinaryLoaderHack.load(data, 0, data.length)
      } else {
        new Pixmap(path.get.child(SeparatedDataFileResolver.decodePath(uri)))
      }
    }

  def getImageFile(glImage: GLTFImage): Nullable[FileHandle] =
    glImage.uri.fold(Nullable.empty[FileHandle]) { uri =>
      if (!uri.startsWith("data:")) {
        Nullable(path.get.child(SeparatedDataFileResolver.decodePath(uri)))
      } else {
        Nullable.empty[FileHandle]
      }
    }
}

object SeparatedDataFileResolver {

  def decodePath(uri: String): String = {
    val src   = uri.getBytes()
    val bytes = new Array[Byte](src.length)
    var pos   = 0
    var i     = 0
    while (i < src.length) {
      val c = src(i)
      if (c == '%') {
        val code = Integer.parseInt(uri.substring(i + 1, i + 3), 16)
        bytes(pos) = code.toByte; pos += 1
        i += 2
      } else {
        bytes(pos) = c; pos += 1
      }
      i += 1
    }
    new String(bytes, 0, pos, "UTF-8")
  }
}
