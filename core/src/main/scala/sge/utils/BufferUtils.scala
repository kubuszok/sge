package sge
package utils

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.nio.ShortBuffer

import sge.math.Matrix3;
import sge.math.Matrix4;

/** Class with static helper methods to increase the speed of array/direct buffer and direct buffer/direct buffer transfers
  *
  * @author
  *   mzechner, xoppa (original implementation)
  */
object BufferUtils {

  val unsafeBuffers:   scala.collection.mutable.ArrayBuffer[ByteBuffer] = new scala.collection.mutable.ArrayBuffer[ByteBuffer](0)
  var allocatedUnsafe: Int                                              = 0

  // Import the native methods from the Gdx library.
  import com.badlogic.gdx.utils.BufferUtils._

  /** Copies numFloats floats from src starting at offset to dst. Dst is assumed to be a direct {@link Buffer} . The method will crash if that is not the case. The position and limit of the buffer are
    * ignored, the copy is placed at position 0 in the buffer. After the copying process the position of the buffer is set to 0 and its limit is set to numFloats * 4 if it is a ByteBuffer and
    * numFloats if it is a FloatBuffer. In case the Buffer is neither a ByteBuffer nor a FloatBuffer the limit is not set. This is an expert method, use at your own risk.
    *
    * @param src
    *   the source array
    * @param dst
    *   the destination buffer, has to be a direct Buffer
    * @param numFloats
    *   the number of floats to copy
    * @param offset
    *   the offset in src to start copying from
    */
  def copy(src: Array[Float], dst: Buffer, numFloats: Int, offset: Int): Unit = {
    if (dst.isInstanceOf[ByteBuffer])
      dst.limit(numFloats << 2);
    else if (dst.isInstanceOf[FloatBuffer]) dst.limit(numFloats);

    copyJni(src, dst, numFloats, offset);
    dst.position(0);
  }

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Array[Byte], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
    dst.limit(dst.position() + bytesToElements(dst, numElements))
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements)
  }

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Array[Short], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
    dst.limit(dst.position() + bytesToElements(dst, numElements << 1))
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1)
  }

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position and limit will stay the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param numElements
    *   the number of elements to copy.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    */
  def copy(src: Array[Char], srcOffset: Int, numElements: Int, dst: Buffer): Unit =
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1)

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position and limit will stay the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param numElements
    *   the number of elements to copy.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    */
  //def copy(src: Array[Int], srcOffset: Int, numElements: Int, dst: Buffer): Unit =
  //  copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2)
  // TODO: report compiler error because this method IS implemented in the Gdx library, but the compiler doesn't see it.

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position and limit will stay the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param numElements
    *   the number of elements to copy.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    */
  def copy(src: Array[Long], srcOffset: Int, numElements: Int, dst: Buffer): Unit =
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3)

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position and limit will stay the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param numElements
    *   the number of elements to copy.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    */
  def copy(src: Array[Float], srcOffset: Int, numElements: Int, dst: Buffer): Unit =
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2)

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position and limit will stay the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param numElements
    *   the number of elements to copy.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    */
  def copy(src: Array[Double], srcOffset: Int, numElements: Int, dst: Buffer): Unit =
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3)

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Array[Char], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
    dst.limit(dst.position() + bytesToElements(dst, numElements << 1))
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1)
  }

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  // def copy(src: Array[Int], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
  //   dst.limit(dst.position() + bytesToElements(dst, numElements << 2))
  //   copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2)
  // }
  // TODO: report compiler error because this method IS implemented in the Gdx library, but the compiler doesn't see it.

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Array[Long], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
    dst.limit(dst.position() + bytesToElements(dst, numElements << 3))
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3)
  }

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Array[Float], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
    dst.limit(dst.position() + bytesToElements(dst, numElements << 2))
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2)
  }

  /** Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer} instance's {@link Buffer#position()} is used to define the offset into the
    * Buffer itself. The position will stay the same, the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
    *
    * @param src
    *   the source array.
    * @param srcOffset
    *   the offset into the source array.
    * @param dst
    *   the destination Buffer, its position is used as an offset.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Array[Double], srcOffset: Int, dst: Buffer, numElements: Int): Unit = {
    dst.limit(dst.position() + bytesToElements(dst, numElements << 3))
    copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3)
  }

  /** Copies the contents of src to dst, starting from the current position of src, copying numElements elements (using the data type of src, no matter the datatype of dst). The dst
    * {@link Buffer#position()} is used as the writing offset. The position of both Buffers will stay the same. The limit of the src Buffer will stay the same. The limit of the dst Buffer will be set
    * to dst.position() + numElements, where numElements are translated to the number of elements appropriate for the dst Buffer data type. <b>The Buffers must be direct Buffers with native byte
    * order. No error checking is performed</b>.
    *
    * @param src
    *   the source Buffer.
    * @param dst
    *   the destination Buffer.
    * @param numElements
    *   the number of elements to copy.
    */
  def copy(src: Buffer, dst: Buffer, numElements: Int): Unit = {
    val numBytes = elementsToBytes(src, numElements)
    dst.limit(dst.position() + bytesToElements(dst, numBytes))
    copyJni(src, positionInBytes(src), dst, positionInBytes(dst), numBytes)
  }

  /** Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with
    */
  def transform(data: Buffer, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4): Unit =
    transform(data, dimensions, strideInBytes, count, matrix, 0)

  /** Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with
    */
  def transform(data: Array[Float], dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4): Unit =
    transform(data, dimensions, strideInBytes, count, matrix, 0)

  /** Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the {@link Buffer#position()} and used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with
    * @param offset
    *   The offset within the buffer (in bytes relative to the current position) to the vector
    */
  def transform(data: Buffer, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4, offset: Int): Unit =
    dimensions match {
      case 4 =>
        transformV4M4Jni(data, strideInBytes, count, matrix.values, positionInBytes(data) + offset)
      case 3 =>
        transformV3M4Jni(data, strideInBytes, count, matrix.values, positionInBytes(data) + offset)
      case 2 =>
        transformV2M4Jni(data, strideInBytes, count, matrix.values, positionInBytes(data) + offset)
      case _ =>
        throw new IllegalArgumentException()
    }

  /** Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the {@link Buffer#position()} and used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with
    * @param offset
    *   The offset within the buffer (in bytes relative to the current position) to the vector
    */
  def transform(data: Array[Float], dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix4, offset: Int): Unit =
    dimensions match {
      case 4 =>
        transformV4M4Jni(data, strideInBytes, count, matrix.values, offset)
      case 3 =>
        transformV3M4Jni(data, strideInBytes, count, matrix.values, offset)
      case 2 =>
        transformV2M4Jni(data, strideInBytes, count, matrix.values, offset)
      case _ =>
        throw new IllegalArgumentException()
    }

  /** Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with
    */
  def transform(data: Buffer, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3): Unit =
    transform(data, dimensions, strideInBytes, count, matrix, 0)

  /** Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with
    */
  def transform(data: Array[Float], dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3): Unit =
    transform(data, dimensions, strideInBytes, count, matrix, 0)

  /** Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the {@link Buffer#position()} and used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with,
    * @param offset
    *   The offset within the buffer (in bytes relative to the current position) to the vector
    */
  def transform(data: Buffer, dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3, offset: Int): Unit =
    dimensions match {
      case 3 =>
        transformV3M3Jni(data, strideInBytes, count, matrix.values, positionInBytes(data) + offset)
      case 2 =>
        transformV2M3Jni(data, strideInBytes, count, matrix.values, positionInBytes(data) + offset)
      case _ =>
        throw new IllegalArgumentException()
    }

  /** Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the {@link Buffer#position()} and used as the offset.
    * @param data
    *   The buffer to transform.
    * @param dimensions
    *   The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
    * @param strideInBytes
    *   The offset between the first and the second vector to transform
    * @param count
    *   The number of vectors to transform
    * @param matrix
    *   The matrix to multiply the vector with,
    * @param offset
    *   The offset within the buffer (in bytes relative to the current position) to the vector
    */
  def transform(data: Array[Float], dimensions: Int, strideInBytes: Int, count: Int, matrix: Matrix3, offset: Int): Unit =
    dimensions match {
      case 3 =>
        transformV3M3Jni(data, strideInBytes, count, matrix.values, offset)
      case 2 =>
        transformV2M3Jni(data, strideInBytes, count, matrix.values, offset)
      case _ =>
        throw new IllegalArgumentException()
    }

  def findFloats(vertex: Buffer, strideInBytes: Int, vertices: Buffer, numVertices: Int): Long =
    find(vertex, positionInBytes(vertex), strideInBytes, vertices, positionInBytes(vertices), numVertices)

  def findFloats(vertex: Array[Float], strideInBytes: Int, vertices: Buffer, numVertices: Int): Long =
    find(vertex, 0, strideInBytes, vertices, positionInBytes(vertices), numVertices)

  def findFloats(vertex: Buffer, strideInBytes: Int, vertices: Array[Float], numVertices: Int): Long =
    find(vertex, positionInBytes(vertex), strideInBytes, vertices, 0, numVertices)

  def findFloats(vertex: Array[Float], strideInBytes: Int, vertices: Array[Float], numVertices: Int): Long =
    find(vertex, 0, strideInBytes, vertices, 0, numVertices)

  def findFloats(vertex: Buffer, strideInBytes: Int, vertices: Buffer, numVertices: Int, epsilon: Float): Long =
    find(vertex, positionInBytes(vertex), strideInBytes, vertices, positionInBytes(vertices), numVertices, epsilon)

  def findFloats(vertex: Array[Float], strideInBytes: Int, vertices: Buffer, numVertices: Int, epsilon: Float): Long =
    find(vertex, 0, strideInBytes, vertices, positionInBytes(vertices), numVertices, epsilon)

  def findFloats(vertex: Buffer, strideInBytes: Int, vertices: Array[Float], numVertices: Int, epsilon: Float): Long =
    find(vertex, positionInBytes(vertex), strideInBytes, vertices, 0, numVertices, epsilon)

  def findFloats(vertex: Array[Float], strideInBytes: Int, vertices: Array[Float], numVertices: Int, epsilon: Float): Long =
    find(vertex, 0, strideInBytes, vertices, 0, numVertices, epsilon)

  private def positionInBytes(dst: Buffer): Int =
    dst match {
      case _: ByteBuffer   => dst.position()
      case _: ShortBuffer  => dst.position() << 1
      case _: CharBuffer   => dst.position() << 1
      case _: IntBuffer    => dst.position() << 2
      case _: LongBuffer   => dst.position() << 3
      case _: FloatBuffer  => dst.position() << 2
      case _: DoubleBuffer => dst.position() << 3
      case _ => throw new RuntimeException("Can't copy to a " + dst.getClass.getName + " instance")
    }

  private def bytesToElements(dst: Buffer, bytes: Int): Int =
    dst match {
      case _: ByteBuffer   => bytes
      case _: ShortBuffer  => bytes >>> 1
      case _: CharBuffer   => bytes >>> 1
      case _: IntBuffer    => bytes >>> 2
      case _: LongBuffer   => bytes >>> 3
      case _: FloatBuffer  => bytes >>> 2
      case _: DoubleBuffer => bytes >>> 3
      case _ => throw new RuntimeException("Can't copy to a " + dst.getClass.getName + " instance")
    }

  private def elementsToBytes(dst: Buffer, elements: Int): Int =
    dst match {
      case _: ByteBuffer   => elements
      case _: ShortBuffer  => elements << 1
      case _: CharBuffer   => elements << 1
      case _: IntBuffer    => elements << 2
      case _: LongBuffer   => elements << 3
      case _: FloatBuffer  => elements << 2
      case _: DoubleBuffer => elements << 3
      case _ => throw new RuntimeException("Can't copy to a " + dst.getClass.getName + " instance")
    }

  def newFloatBuffer(numFloats: Int): FloatBuffer = {
    val buffer = ByteBuffer.allocateDirect(numFloats * 4)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asFloatBuffer()
  }

  def newDoubleBuffer(numDoubles: Int): DoubleBuffer = {
    val buffer = ByteBuffer.allocateDirect(numDoubles * 8)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asDoubleBuffer()
  }

  def newByteBuffer(numBytes: Int): ByteBuffer = {
    val buffer = ByteBuffer.allocateDirect(numBytes)
    buffer.order(ByteOrder.nativeOrder())
    buffer
  }

  def newShortBuffer(numShorts: Int): ShortBuffer = {
    val buffer = ByteBuffer.allocateDirect(numShorts * 2)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asShortBuffer()
  }

  def newCharBuffer(numChars: Int): CharBuffer = {
    val buffer = ByteBuffer.allocateDirect(numChars * 2)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asCharBuffer()
  }

  def newIntBuffer(numInts: Int): IntBuffer = {
    val buffer = ByteBuffer.allocateDirect(numInts * 4)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asIntBuffer()
  }

  def newLongBuffer(numLongs: Int): LongBuffer = {
    val buffer = ByteBuffer.allocateDirect(numLongs * 8)
    buffer.order(ByteOrder.nativeOrder())
    buffer.asLongBuffer()
  }

  def disposeUnsafeByteBuffer(buffer: ByteBuffer): Unit = {
    val size = buffer.capacity()
    unsafeBuffers.synchronized {
      val index = unsafeBuffers.indexOf(buffer)
      if (index < 0)
        throw new IllegalArgumentException("buffer not allocated with newUnsafeByteBuffer or already disposed")
      unsafeBuffers.remove(index)
    }
    allocatedUnsafe -= size
    freeMemory(buffer)
  }

  def isUnsafeByteBuffer(buffer: ByteBuffer): Boolean =
    unsafeBuffers.synchronized {
      unsafeBuffers.contains(buffer)
    }

  /** Allocates a new direct ByteBuffer from native heap memory using the native byte order. Needs to be disposed with {@link #disposeUnsafeByteBuffer(ByteBuffer)} .
    */
  def newUnsafeByteBuffer(numBytes: Int): ByteBuffer = {
    val buffer = newDisposableByteBuffer(numBytes)
    buffer.order(ByteOrder.nativeOrder())
    allocatedUnsafe += numBytes
    unsafeBuffers.synchronized {
      unsafeBuffers += buffer
    }
    buffer
  }

  /** Returns the address of the Buffer, it assumes it is an unsafe buffer.
    * @param buffer
    *   The Buffer to ask the address for.
    * @return
    *   the address of the Buffer.
    */
  def getUnsafeBufferAddress(buffer: Buffer): Long =
    getBufferAddress(buffer) + buffer.position()

  /** Registers the given ByteBuffer as an unsafe ByteBuffer. The ByteBuffer must have been allocated in native code, pointing to a memory region allocated via malloc. Needs to be disposed with
    * {@link #disposeUnsafeByteBuffer(ByteBuffer)} .
    * @param buffer
    *   the {@link ByteBuffer} to register
    * @return
    *   the ByteBuffer passed to the method
    */
  def newUnsafeByteBuffer(buffer: ByteBuffer): ByteBuffer = {
    allocatedUnsafe += buffer.capacity()
    unsafeBuffers.synchronized {
      unsafeBuffers += buffer
    }
    buffer
  }

  /** @return the number of bytes allocated with {@link #newUnsafeByteBuffer(int)} */
  def getAllocatedBytesUnsafe(): Int =
    allocatedUnsafe
}
