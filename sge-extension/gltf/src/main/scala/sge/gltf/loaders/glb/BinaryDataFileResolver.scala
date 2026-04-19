/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 111
 * Covenant-baseline-methods: BinaryDataFileResolver,bufferMap,getBuffer,getRoot,glModel,i,jsonData,length,load,loadInternal,magic,path,readBuffer,version
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package glb

import java.io.{ ByteArrayInputStream, InputStream }
import java.nio.{ ByteBuffer, ByteOrder }
import scala.collection.mutable.HashMap
import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.gltf.data.GLTF
import sge.gltf.data.texture.GLTFImage
import sge.gltf.loaders.exceptions.{ GLTFIllegalException, GLTFRuntimeException }
import sge.gltf.loaders.gltf.{ GLTFJsonParser, SeparatedDataFileResolver }
import sge.gltf.loaders.shared.GLTFLoaderBase
import sge.gltf.loaders.shared.data.DataFileResolver
import sge.gltf.loaders.shared.texture.PixmapBinaryLoaderHack
import sge.utils.{ LittleEndianInputStream, Nullable }

class BinaryDataFileResolver extends DataFileResolver {

  private val bufferMap: HashMap[Int, ByteBuffer] = HashMap.empty
  private var glModel:   Nullable[GLTF]           = Nullable.empty
  private var path:      Nullable[FileHandle]     = Nullable.empty

  override def load(file: FileHandle): Unit = {
    path = Nullable(file.parent())
    load(file.read())
  }

  def load(bytes: Array[Byte]): Unit =
    load(new ByteArrayInputStream(bytes))

  def load(stream: InputStream): Unit =
    load(new LittleEndianInputStream(stream))

  def load(stream: LittleEndianInputStream): Unit =
    try
      loadInternal(stream)
    catch {
      case e: java.io.IOException =>
        throw new GLTFRuntimeException(e)
    } finally
      try stream.close()
      catch { case _: Exception => () }

  private def loadInternal(stream: LittleEndianInputStream): Unit = {
    val magic = stream.readInt()
    if (magic != 0x46546c67) throw new GLTFIllegalException("bad magic")
    val version = stream.readInt()
    if (version != 2) throw new GLTFIllegalException("bad version")
    val length = stream.readInt()

    val readBuffer = new Array[Byte](1024 * 1024) // 1MB buffer
    var jsonData: Nullable[String] = Nullable.empty
    var i = 12
    while (i < length) {
      val chunkLen  = stream.readInt()
      val chunkType = stream.readInt()
      i += 8
      if (chunkType == 0x4e4f534a) {
        val data = new Array[Byte](chunkLen)
        stream.readFully(data, 0, chunkLen)
        jsonData = Nullable(new String(data))
      } else if (chunkType == 0x004e4942) {
        val bufferData = ByteBuffer.allocate(chunkLen)
        bufferData.order(ByteOrder.LITTLE_ENDIAN)
        var bytesToRead = chunkLen
        var bytesRead   = 0
        while (bytesToRead > 0 && { bytesRead = stream.read(readBuffer, 0, scala.math.min(readBuffer.length, bytesToRead)); bytesRead != -1 }) {
          bufferData.put(readBuffer, 0, bytesRead)
          bytesToRead -= bytesRead
        }
        if (bytesToRead > 0) throw new GLTFIllegalException("premature end of file")
        bufferData.flip()
        bufferMap.put(bufferMap.size, bufferData)
      } else {
        // skip unknown chunk
        if (chunkLen > 0) {
          stream.skip(chunkLen.toLong)
        }
      }
      i += chunkLen
    }
    glModel = jsonData.map(json => GLTFJsonParser.parse(json))
  }

  override def getRoot: GLTF = glModel.get

  override def getBuffer(buffer: Int): ByteBuffer = bufferMap(buffer)

  override def load(glImage: GLTFImage): Pixmap =
    if (glImage.bufferView.isDefined) {
      val bufferView = glModel.get.bufferViews.get(glImage.bufferView.get)
      val buffer     = bufferMap(bufferView.buffer.get)
      buffer.position(bufferView.byteOffset)
      val data = new Array[Byte](bufferView.byteLength)
      buffer.get(data)
      PixmapBinaryLoaderHack.load(data, 0, data.length)
    } else if (glImage.uri.isDefined) {
      new Pixmap(path.get.child(SeparatedDataFileResolver.decodePath(glImage.uri.get)))
    } else {
      throw new GLTFIllegalException("GLB image should have bufferView or uri")
    }
}
