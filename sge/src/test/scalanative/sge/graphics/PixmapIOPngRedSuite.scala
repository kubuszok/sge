/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-494 (PixmapIO PNG writer emits structurally corrupt PNGs).
 *
 * Root cause being reproduced: ChunkBuffer (sge/src/main/scala/sge/graphics/
 * PixmapIO.scala lines 333-351) extends DataOutputStream over an anonymous
 * CheckedOutputStream(buffer, crc) built from CONSTRUCTOR-LOCAL buffer/crc
 * instances, while the class fields `buffer`/`crc` are SEPARATE new instances
 * that endChunk reads. As a result endChunk writes chunk length
 * buffer.size() - 4 = -4 (0xFFFFFFFC), zero payload bytes, and the CRC of
 * nothing (0x00000000) for every chunk — the IHDR/IDAT/IEND tags and all
 * pixel data vanish into the discarded constructor-local buffer.
 *
 * The original Java (original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/
 * PixmapIO.java, ChunkBuffer at lines 324-345) shares ONE
 * ByteArrayOutputStream/CRC32 pair between the CheckedOutputStream and the
 * fields via the private constructor at lines 332-336:
 *
 *   private ChunkBuffer (ByteArrayOutputStream buffer, CRC32 crc) {
 *     super(new CheckedOutputStream(buffer, crc));
 *     this.buffer = buffer;
 *     this.crc = crc;
 *   }
 *
 * Expected byte layout is derived from the PNG constants already present in
 * both sources: SIGNATURE {-119, 80, 78, 71, 13, 10, 26, 10} (Java line 168,
 * Scala line 188), IHDR = 0x49484452 (Java 169, Scala 189), IDAT = 0x49444154
 * (Java 170, Scala 190), IEND = 0x49454E44 (Java 171, Scala 191), and the
 * 13-byte IHDR payload written at Java lines 206-214 / Scala lines 231-238
 * (width int, height int, bit depth, color type, compression, filter,
 * interlace). Each chunk on the wire is: 4-byte big-endian data length,
 * 4-byte type tag, data, 4-byte CRC32 computed over type tag + data
 * (endChunk, Java lines 338-345).
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class PixmapIOPngRedSuite extends munit.FunSuite {

  // --- Headless pixmap construction ----------------------------------------
  //
  // A 2x2 RGBA8888 pixmap built with pure gdx2d ops (clear via fill, setPixel
  // via drawPixel) — exactly the operations the shared Gdx2DPixmapTest already
  // exercises on every platform, so no GL context or Sge instance is needed.

  private val Red   = 0xff0000ff // RGBA8888 opaque red
  private val Green = 0x00ff00ff // RGBA8888 opaque green

  private def makePixmap(): Pixmap = {
    val pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888)
    pixmap.setBlending(Pixmap.Blending.None)
    pixmap.setColor(Red)
    pixmap.fill()
    pixmap.drawPixel(Pixels(1), Pixels(1), Green)
    pixmap
  }

  /** Encodes the pixmap fully in memory through the same ChunkBuffer-backed path used by PixmapIO.writePNG (which merely wraps PNG.write(OutputStream, Pixmap) with a FileHandle stream). */
  private def writePng(pixmap: Pixmap): Array[Byte] = {
    val png = new PixmapIO.PNG(64)
    try {
      png.setFlipY(false)
      val out = new ByteArrayOutputStream()
      png.write(out, pixmap)
      out.toByteArray()
    } finally {
      png.close()
    }
  }

  private def encodedPng(): Array[Byte] = {
    val pixmap = makePixmap()
    try {
      writePng(pixmap)
    } finally {
      pixmap.close()
    }
  }

  // --- Byte-level helpers ----------------------------------------------------

  private def readIntBE(bytes: Array[Byte], offset: Int): Int =
    ((bytes(offset) & 0xff) << 24) | ((bytes(offset + 1) & 0xff) << 16) | ((bytes(offset + 2) & 0xff) << 8) | (bytes(offset + 3) & 0xff)

  private def typeTag(bytes: Array[Byte], offset: Int): String =
    new String(Array(bytes(offset), bytes(offset + 1), bytes(offset + 2), bytes(offset + 3)), "US-ASCII")

  private final case class Chunk(tag: String, length: Int, dataOffset: Int, storedCrc: Int)

  /** Walks the chunk stream after the 8-byte signature. Fails the test with a precise message if any chunk header is structurally invalid (negative length or overrun). */
  private def walkChunks(bytes: Array[Byte]): List[Chunk] = {
    val chunks = List.newBuilder[Chunk]
    var offset = 8
    while (offset < bytes.length) {
      assert(
        offset + 8 <= bytes.length,
        s"truncated chunk header at offset $offset (file length ${bytes.length})"
      )
      val length = readIntBE(bytes, offset)
      val tag    = typeTag(bytes, offset + 4)
      assert(
        length >= 0,
        s"chunk '$tag' at offset $offset declares negative data length $length — " +
          "endChunk wrote buffer.size() - 4 of an always-empty buffer (PixmapIO.scala line 345)"
      )
      val dataOffset = offset + 8
      assert(
        dataOffset + length + 4 <= bytes.length,
        s"chunk '$tag' at offset $offset (data length $length) overruns the file (length ${bytes.length})"
      )
      chunks += Chunk(tag, length, dataOffset, readIntBE(bytes, dataOffset + length))
      offset = dataOffset + length + 4
    }
    chunks.result()
  }

  /** CRC32 over type tag + data, as PNG mandates and as endChunk is supposed to produce (Java lines 338-345). */
  private def expectedCrc(bytes: Array[Byte], chunk: Chunk): Int = {
    val crc = new CRC32()
    crc.update(bytes, chunk.dataOffset - 4, chunk.length + 4)
    crc.getValue().toInt
  }

  /** Index of the first occurrence of the 4-byte ASCII tag in the stream, or -1. */
  private def findTag(bytes: Array[Byte], tag: String): Int = {
    val needle  = tag.getBytes("US-ASCII")
    var found   = -1
    var offset  = 0
    val lastOff = bytes.length - 4
    while (found < 0 && offset <= lastOff) {
      if (bytes(offset) == needle(0) && bytes(offset + 1) == needle(1) && bytes(offset + 2) == needle(2) && bytes(offset + 3) == needle(3)) {
        found = offset
      }
      offset += 1
    }
    found
  }

  // --- Tests -----------------------------------------------------------------

  test("first chunk after the PNG signature is IHDR with data length 13") {
    val bytes = encodedPng()
    // Signature: 89 50 4E 47 0D 0A 1A 0A (Java line 168 / Scala line 188).
    // This part is written directly to the target stream and survives the bug.
    val expectedSignature = Array[Byte](-119, 80, 78, 71, 13, 10, 26, 10)
    assertEquals(bytes.take(8).toList, expectedSignature.toList, "PNG signature mismatch")
    // IHDR data: width(4) + height(4) + depth(1) + color(1) + compression(1) +
    // filter(1) + interlace(1) = 13 bytes (Java lines 206-214).
    // RED: the bugged endChunk emits length -4 because the field buffer never
    // receives the bytes written through the CheckedOutputStream.
    assertEquals(readIntBE(bytes, 8), 13, "IHDR chunk data length must be 13")
    assertEquals(typeTag(bytes, 12), "IHDR", "first chunk type tag must be IHDR")
    val width  = readIntBE(bytes, 16)
    val height = readIntBE(bytes, 20)
    assertEquals(width, 2, "IHDR width")
    assertEquals(height, 2, "IHDR height")
  }

  test("every chunk's stored CRC matches CRC32 over type tag + data") {
    val bytes  = encodedPng()
    val chunks = walkChunks(bytes)
    assert(chunks.nonEmpty, "no chunks found after the signature")
    chunks.foreach { chunk =>
      assertEquals(
        chunk.storedCrc,
        expectedCrc(bytes, chunk),
        s"stored CRC of chunk '${chunk.tag}' must equal CRC32(tag + data) — " +
          "the bugged endChunk stores the CRC of a never-written-to CRC32 field"
      )
    }
  }

  test("an IDAT chunk with data length > 0 exists") {
    val bytes = encodedPng()
    // The IDAT tag itself is written through the ChunkBuffer (Scala line 241),
    // so with the bug it lands in the discarded constructor-local buffer and
    // the tag never reaches the output at all.
    val tagOffset = findTag(bytes, "IDAT")
    assert(tagOffset >= 8 + 4, s"no IDAT tag found in the ${bytes.length}-byte output")
    val length = readIntBE(bytes, tagOffset - 4)
    assert(length > 0, s"IDAT data length must be > 0, got $length")
  }

  test("stream terminates with a valid IEND chunk") {
    val bytes = encodedPng()
    assert(bytes.length >= 20, s"output too short to hold signature + IEND (${bytes.length} bytes)")
    val offset = bytes.length - 12
    // IEND: length 0, tag "IEND", CRC32("IEND") = 0xAE426082.
    assertEquals(readIntBE(bytes, offset), 0, "IEND data length must be 0")
    assertEquals(typeTag(bytes, offset + 4), "IEND", "last chunk type tag must be IEND")
    assertEquals(readIntBE(bytes, offset + 8), 0xae426082, "IEND CRC must be CRC32 of the tag alone")
  }

  test("round-trip: the written PNG decodes back to the original 2x2 pixels") {
    val bytes = encodedPng()
    // Decode through gdx2d (same native decode path on JVM and Native) — no
    // ImageIO, so both platform copies of this suite stay byte-identical.
    val decoded = new Pixmap(bytes, 0, bytes.length)
    try {
      assertEquals(decoded.width, Pixels(2), "decoded width")
      assertEquals(decoded.height, Pixels(2), "decoded height")
      assertEquals(decoded.getPixel(Pixels(0), Pixels(0)), Red, "pixel (0,0)")
      assertEquals(decoded.getPixel(Pixels(1), Pixels(0)), Red, "pixel (1,0)")
      assertEquals(decoded.getPixel(Pixels(0), Pixels(1)), Red, "pixel (0,1)")
      assertEquals(decoded.getPixel(Pixels(1), Pixels(1)), Green, "pixel (1,1)")
    } finally {
      decoded.close()
    }
  }
}
