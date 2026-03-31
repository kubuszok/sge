/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package shared
package data

import java.nio.{ByteBuffer, ByteOrder, FloatBuffer, IntBuffer, ShortBuffer}
import sge.gltf.data.GLTF
import sge.gltf.data.data.{GLTFAccessor, GLTFBufferView}
import sge.gltf.loaders.exceptions.GLTFUnsupportedException
import sge.gltf.loaders.shared.GLTFTypes

class DataResolver(private val glModel: GLTF, private val dataFileResolver: DataFileResolver) {

  def getAccessor(accessorID: Int): GLTFAccessor = {
    glModel.accessors.get(accessorID)
  }

  def readBufferFloat(accessorID: Int): Array[Float] = {
    val accessor = glModel.accessors.get(accessorID)
    val accessorBuffer = getAccessorBuffer(accessor)
    val bytes = accessorBuffer.prepareForReading()
    val data = new Array[Float](GLTFTypes.accessorSize(accessor) / 4)

    val nbFloatsPerVertex = GLTFTypes.accessorTypeSize(accessor)
    val nbBytesToSkip = accessorBuffer.getByteStride - nbFloatsPerVertex * 4
    if (nbBytesToSkip == 0) {
      bytes.asFloatBuffer().get(data)
    } else {
      var i = 0
      while (i < accessor.count) {
        var j = 0
        while (j < nbFloatsPerVertex) {
          data(i * nbFloatsPerVertex + j) = bytes.getFloat()
          j += 1
        }
        // skip remaining bytes
        bytes.position(bytes.position() + nbBytesToSkip)
        i += 1
      }
    }
    data
  }

  def readBufferUByte(accessorID: Int): Array[Int] = {
    val accessor = glModel.accessors.get(accessorID)
    val accessorBuffer = getAccessorBuffer(accessor)
    val bytes = accessorBuffer.prepareForReading()
    val data = new Array[Int](GLTFTypes.accessorSize(accessor))

    val nbBytesPerVertex = GLTFTypes.accessorTypeSize(accessor)
    val nbBytesToSkip = accessorBuffer.getByteStride - nbBytesPerVertex
    if (nbBytesToSkip == 0) {
      var i = 0
      while (i < data.length) {
        data(i) = bytes.get() & 0xFF
        i += 1
      }
    } else {
      var i = 0
      while (i < accessor.count) {
        var j = 0
        while (j < nbBytesPerVertex) {
          data(i * nbBytesPerVertex + j) = bytes.get() & 0xFF
          j += 1
        }
        bytes.position(bytes.position() + nbBytesToSkip)
        i += 1
      }
    }
    data
  }

  def readBufferUShort(accessorID: Int): Array[Int] = {
    val accessor = glModel.accessors.get(accessorID)
    val accessorBuffer = getAccessorBuffer(accessor)
    val bytes = accessorBuffer.prepareForReading()
    val data = new Array[Int](GLTFTypes.accessorSize(accessor) / 2)

    val nbShortsPerVertex = GLTFTypes.accessorTypeSize(accessor)
    val nbBytesToSkip = accessorBuffer.getByteStride - nbShortsPerVertex * 2
    if (nbBytesToSkip == 0) {
      val shorts = bytes.asShortBuffer()
      var i = 0
      while (i < data.length) {
        data(i) = shorts.get() & 0xFFFF
        i += 1
      }
    } else {
      var i = 0
      while (i < accessor.count) {
        var j = 0
        while (j < nbShortsPerVertex) {
          data(i * nbShortsPerVertex + j) = bytes.getShort() & 0xFFFF
          j += 1
        }
        bytes.position(bytes.position() + nbBytesToSkip)
        i += 1
      }
    }
    data
  }

  def readBufferUShortAsFloat(accessorID: Int): Array[Float] = {
    val intBuffer = readBufferUShort(accessorID)
    val floatBuffer = new Array[Float](intBuffer.length)
    var i = 0
    while (i < intBuffer.length) {
      floatBuffer(i) = intBuffer(i) / 65535f
      i += 1
    }
    floatBuffer
  }

  def readBufferUByteAsFloat(accessorID: Int): Array[Float] = {
    val intBuffer = readBufferUByte(accessorID)
    val floatBuffer = new Array[Float](intBuffer.length)
    var i = 0
    while (i < intBuffer.length) {
      floatBuffer(i) = intBuffer(i) / 255f
      i += 1
    }
    floatBuffer
  }

  def getBufferFloat(accessorID: Int): FloatBuffer = {
    getBufferFloat(glModel.accessors.get(accessorID))
  }

  def getBufferView(bufferViewID: Int): GLTFBufferView = {
    glModel.bufferViews.get(bufferViewID)
  }

  def getBufferFloat(glAccessor: GLTFAccessor): FloatBuffer = {
    getBufferByte(glAccessor).asFloatBuffer()
  }

  def getBufferInt(glAccessor: GLTFAccessor): IntBuffer = {
    getBufferByte(glAccessor).asIntBuffer()
  }

  def getBufferShort(glAccessor: GLTFAccessor): ShortBuffer = {
    getBufferByte(glAccessor).asShortBuffer()
  }

  def getBufferByte(glAccessor: GLTFAccessor): ByteBuffer = {
    val buffer = getAccessorBuffer(glAccessor)
    buffer.prepareForReading()
  }

  def getAccessorBuffer(glAccessor: GLTFAccessor): AccessorBuffer = {
    val buffer =
      if (glAccessor.bufferView.isDefined) {
        val bufferView = glModel.bufferViews.get(glAccessor.bufferView.get)
        AccessorBuffer.fromBufferView(glAccessor, bufferView, dataFileResolver)
      } else {
        AccessorBuffer.fromZeros(glAccessor)
      }
    if (glAccessor.sparse.isDefined) {
      buffer.prepareForWriting()
      patchSparseValues(glAccessor, buffer)
    }
    buffer.prepareForReading()
    buffer
  }

  private def patchSparseValues(glAccessor: GLTFAccessor, outputBuffer: AccessorBuffer): Unit = {
    val sparse = glAccessor.sparse.get
    val indicesBufferView = getBufferView(sparse.indices.get.bufferView)
    val indicesBuffer = dataFileResolver.getBuffer(indicesBufferView.buffer.get).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    indicesBuffer.position(sparse.indices.get.byteOffset + indicesBufferView.byteOffset)
    val replacementValueBufferView = getBufferView(sparse.values.get.bufferView)
    val replacementValuesBuffer = dataFileResolver.getBuffer(replacementValueBufferView.buffer.get).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    replacementValuesBuffer.position(sparse.values.get.byteOffset + replacementValueBufferView.byteOffset)
    val bytesPerValue = GLTFTypes.accessorStrideSize(glAccessor)
    val replacementValueBytes = new Array[Byte](bytesPerValue)
    var i = 0
    while (i < sparse.count) {
      val indexToReplace: Int = sparse.indices.get.componentType match {
        case GLTFTypes.C_UBYTE =>
          indicesBuffer.get().toInt & 0xff
        case GLTFTypes.C_USHORT =>
          indicesBuffer.getShort().toInt & 0xffff
        case GLTFTypes.C_UINT =>
          // java does not have uint, so read as signed long
          val asLong = indicesBuffer.getInt().toLong & 0xffffffffL
          if (asLong > Int.MaxValue) {
            throw new GLTFUnsupportedException("very large indices can not be parsed")
          }
          asLong.toInt
        case _ =>
          throw new GLTFUnsupportedException("unsupported indices type")
      }
      replacementValuesBuffer.get(replacementValueBytes)
      val data = outputBuffer.getData
      val elementOffset = indexToReplace * outputBuffer.getByteStride
      data.position(outputBuffer.getByteOffset + elementOffset)
      data.put(replacementValueBytes)
      i += 1
    }
  }

  def getBufferByte(bufferView: GLTFBufferView): ByteBuffer = {
    val bytes = dataFileResolver.getBuffer(bufferView.buffer.get)
    bytes.position(bufferView.byteOffset)
    bytes
  }
}
