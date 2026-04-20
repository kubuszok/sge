package sge
package anim8

import java.io.{ ByteArrayOutputStream, DataOutputStream }

class ChunkBufferSuite extends munit.FunSuite {

  private val checkedOutputStreamAvailable: Boolean =
    try { new java.util.zip.CheckedOutputStream(java.io.OutputStream.nullOutputStream(), new java.util.zip.CRC32); true }
    catch { case _: Throwable => false }

  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ new TestTransform(
      "requireCheckedOutputStream",
      { test => test.withBody { () => assume(checkedOutputStreamAvailable, "CheckedOutputStream not available (Scala.js)"); test.body() } }
    )

  test("write data and endChunk produces output") {
    val chunk = ChunkBuffer(256)
    // Write a 4-byte chunk type (e.g., "IHDR")
    chunk.writeInt(0x49484452) // "IHDR"
    // Write some payload data
    chunk.writeInt(100) // width
    chunk.writeInt(200) // height

    val baos   = ByteArrayOutputStream()
    val target = DataOutputStream(baos)
    chunk.endChunk(target)
    target.flush()

    val bytes = baos.toByteArray
    // Output should contain: 4-byte length + chunk type + data + 4-byte CRC
    // length = 12 bytes (4 type + 4 width + 4 height), but endChunk subtracts 4 for the type
    // So length field = 8
    assert(bytes.length > 8, s"expected output > 8 bytes, got ${bytes.length}")
  }

  test("CRC32 is computed and appended") {
    val chunk = ChunkBuffer(64)
    chunk.writeInt(0x49444154) // "IDAT"
    chunk.writeByte(0x42)

    val baos   = ByteArrayOutputStream()
    val target = DataOutputStream(baos)
    chunk.endChunk(target)
    target.flush()

    val bytes = baos.toByteArray
    // Last 4 bytes should be the CRC
    // Verify they exist and aren't all zeros (CRC of non-empty data)
    val crcBytes = bytes.takeRight(4)
    assert(crcBytes.exists(_ != 0), "CRC should not be all zeros for non-empty data")
  }

  test("buffer resets after endChunk") {
    val chunk = ChunkBuffer(64)
    chunk.writeInt(0x49444154) // "IDAT"
    chunk.writeInt(1)

    val baos1   = ByteArrayOutputStream()
    val target1 = DataOutputStream(baos1)
    chunk.endChunk(target1)
    target1.flush()
    val size1 = baos1.size()

    // Write a different chunk
    chunk.writeInt(0x49454e44) // "IEND"

    val baos2   = ByteArrayOutputStream()
    val target2 = DataOutputStream(baos2)
    chunk.endChunk(target2)
    target2.flush()
    val size2 = baos2.size()

    // Second chunk should be smaller (only type, no extra int payload)
    assert(size2 < size1, s"second chunk ($size2 bytes) should be smaller than first ($size1 bytes)")
  }
}
