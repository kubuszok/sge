/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-652 (Scala.js java.util.zip codec layer is non-functional).
 *
 * Root cause being reproduced: the Scala.js shims under
 * sge/src/main/scalajs/java/util/zip/ are NOT working codecs — they are
 * throwing placeholders. CRC32.update / getValue, Deflater.setInput /
 * deflate, InflaterInputStream.read, CheckedOutputStream.write etc. all
 * `throw new UnsupportedOperationException(...)`, and the layer does not even
 * provide Inflater, Adler32, or DataFormatException at all. On the JVM and on
 * Scala Native these same classes are backed by the real javalib and WORK, so
 * this suite is GREEN there and RED on Scala.js.
 *
 * This is a SHARED suite (sge/src/test/scala) deliberately: the java.util.zip
 * contract is platform-independent (RFC 1950 zlib, RFC 1951 DEFLATE,
 * RFC 1952 gzip, ISO-HDLC CRC-32, Adler-32). Placing it shared proves that the
 * Scala.js implementation must MATCH the JVM/Native javalib contract rather
 * than encode a JS-specific fiction. On JVM/Native it links and runs green; on
 * Scala.js it fails (the codec layer is missing/throwing).
 *
 * All ground-truth constants below are platform-independent published vectors
 * or were produced by a real JVM `java.util.zip` (scala-cli snapshot) — never
 * self-referential against the port under test:
 *
 *   - CRC-32/ISO-HDLC check value of "123456789" = 0xCBF43926
 *     (the canonical CRC catalogue check value).
 *   - CRC32("The quick brown fox jumps over the lazy dog") = 0x414FA339
 *     (confirmed against JVM java.util.zip.CRC32).
 *   - Adler-32 of "Wikipedia" = 0x11E60398 (the canonical RFC 1950 example),
 *     Adler-32 of "" = 1.
 *   - The interop byte arrays (zlib short, zlib dynamic-Huffman, raw DEFLATE,
 *     gzip) were emitted by JVM java.util.zip.Deflater / GZIPOutputStream and
 *     pasted as literals; the JS Inflater/InflaterInputStream/GZIPInputStream
 *     must decode them back to the exact original plaintext.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the real javalib semantics, not the port's.
 */
package sge
package platform

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.util.zip.{ Adler32, CRC32, CheckedOutputStream, Deflater, DeflaterOutputStream, GZIPInputStream, GZIPOutputStream, Inflater, InflaterInputStream }

class ZipCodecRedSuite extends munit.FunSuite {

  // --- helpers ---------------------------------------------------------------

  private def ascii(s: String): Array[Byte] = s.getBytes("US-ASCII")

  /** Full zlib/raw deflate of `data` via java.util.zip.Deflater, draining to EOF. */
  private def deflate(data: Array[Byte], nowrap: Boolean, level: Int = Deflater.DEFAULT_COMPRESSION): Array[Byte] = {
    val d = new Deflater(level, nowrap)
    d.setInput(data)
    d.finish()
    val out = new ByteArrayOutputStream()
    val buf = new Array[Byte](256)
    while (!d.finished()) {
      val n = d.deflate(buf)
      out.write(buf, 0, n)
    }
    d.end()
    out.toByteArray()
  }

  /** Full inflate via java.util.zip.Inflater, draining to EOF. */
  private def inflate(data: Array[Byte], nowrap: Boolean): Array[Byte] = {
    val inf = new Inflater(nowrap)
    inf.setInput(data)
    val out  = new ByteArrayOutputStream()
    val buf  = new Array[Byte](256)
    var done = false
    while (!done && !inf.finished()) {
      val n = inf.inflate(buf)
      out.write(buf, 0, n)
      if (n == 0 && inf.needsInput()) {
        // All single-shot input consumed and the stream produced everything it
        // can (e.g. an empty payload whose trailer was already read). Stop
        // draining; finished() may legitimately lag here for zero-length data.
        done = true
      }
    }
    inf.end()
    out.toByteArray()
  }

  /** Decode an InflaterInputStream fully to a byte array. */
  private def drain(in: java.io.InputStream): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val buf = new Array[Byte](256)
    var n   = in.read(buf)
    while (n != -1) {
      out.write(buf, 0, n)
      n = in.read(buf)
    }
    in.close()
    out.toByteArray()
  }

  private val Empty:          Array[Byte] = Array.emptyByteArray
  private val OneByte:        Array[Byte] = Array[Byte](0x42)
  private val ShortAscii:     Array[Byte] = ascii("hello, zip world")
  private val Repetitive:     Array[Byte] = Array.fill(10000)('a'.toByte)
  private val Incompressible: Array[Byte] = {
    // Fixed, varied, pseudo-incompressible pattern (deterministic LCG) — no RNG,
    // so the bytes are identical on every platform/run.
    val a    = new Array[Byte](4096)
    var seed = 0x1234abcd
    var i    = 0
    while (i < a.length) {
      seed = seed * 1103515245 + 12345
      a(i) = ((seed >>> 16) & 0xff).toByte
      i += 1
    }
    a
  }
  private val MixedText: Array[Byte] = ascii(
    ("The quick brown fox jumps over the lazy dog. " * 60) +
      ("Pack my box with five dozen liquor jugs. " * 40) +
      ("How vexingly quick daft zebras jump! " * 40)
  )

  private val RoundTripInputs: List[(String, Array[Byte])] = List(
    "empty" -> Empty,
    "one byte" -> OneByte,
    "short ascii" -> ShortAscii,
    "repetitive 10k" -> Repetitive,
    "incompressible" -> Incompressible,
    "mixed text" -> MixedText
  )

  // ==========================================================================
  // 1. CRC32 known vectors (platform-independent ground truth)
  // ==========================================================================

  test("CRC32 of empty input is 0x00000000") {
    val crc = new CRC32()
    assertEquals(crc.getValue(), 0L)
  }

  test("CRC32 of \"123456789\" is the canonical 0xCBF43926") {
    val crc = new CRC32()
    crc.update(ascii("123456789"))
    assertEquals(crc.getValue(), 0xcbf43926L)
  }

  test("CRC32 of the pangram is 0x414FA339") {
    val crc = new CRC32()
    crc.update(ascii("The quick brown fox jumps over the lazy dog"))
    assertEquals(crc.getValue(), 0x414fa339L)
  }

  test("CRC32 incremental byte-by-byte equals bulk update") {
    val data = ascii("The quick brown fox jumps over the lazy dog")

    val bulk = new CRC32()
    bulk.update(data)

    val incremental = new CRC32()
    var i           = 0
    while (i < data.length) {
      incremental.update(data(i) & 0xff)
      i += 1
    }

    assertEquals(incremental.getValue(), bulk.getValue())
    assertEquals(incremental.getValue(), 0x414fa339L)
  }

  test("CRC32 reset clears state back to 0") {
    val crc = new CRC32()
    crc.update(ascii("123456789"))
    assertEquals(crc.getValue(), 0xcbf43926L)
    crc.reset()
    assertEquals(crc.getValue(), 0L)
  }

  // ==========================================================================
  // 2. Adler32 known vectors
  // ==========================================================================

  test("Adler32 of empty input is 1") {
    val a = new Adler32()
    assertEquals(a.getValue(), 1L)
  }

  test("Adler32 of \"Wikipedia\" is the canonical 0x11E60398") {
    val a = new Adler32()
    a.update(ascii("Wikipedia"))
    assertEquals(a.getValue(), 0x11e60398L)
  }

  // ==========================================================================
  // 3. Deflater -> Inflater round-trip, zlib-wrapped (nowrap=false)
  // ==========================================================================

  RoundTripInputs.foreach { case (name, data) =>
    test(s"zlib round-trip (nowrap=false): $name") {
      val compressed = deflate(data, nowrap = false)
      val restored   = inflate(compressed, nowrap = false)
      assertEquals(restored.toList, data.toList)
    }
  }

  // ==========================================================================
  // 4. Deflater -> Inflater round-trip, raw DEFLATE (nowrap=true)
  // ==========================================================================

  RoundTripInputs.foreach { case (name, data) =>
    test(s"raw round-trip (nowrap=true): $name") {
      val compressed = deflate(data, nowrap = true)
      val restored   = inflate(compressed, nowrap = true)
      assertEquals(restored.toList, data.toList)
    }
  }

  // ==========================================================================
  // 5. Interop decode of JVM-produced streams (proves RFC correctness
  //    independent of the JS Deflater).
  // ==========================================================================

  test("Inflater decodes a JVM-produced zlib stream (short, stored/fixed block)") {
    // JVM java.util.zip.Deflater(level=6, nowrap=false) of "hello, zip world".
    val jvmZlib  = Array[Byte](120, -100, -53, 72, -51, -55, -55, -41, 81, -88, -54, 44, 80, 40, -49, 47, -54, 73, 1, 0, 50, 58, 5, -4)
    val restored = inflate(jvmZlib, nowrap = false)
    assertEquals(new String(restored, "US-ASCII"), "hello, zip world")
  }

  test("Inflater decodes a JVM-produced zlib stream using DYNAMIC Huffman coding") {
    // JVM java.util.zip.Deflater(level=9, nowrap=false) of ~5.8 KB of natural
    // English text — the JVM emits a DYNAMIC-Huffman DEFLATE block (BTYPE=2),
    // so this exercises the Inflater's dynamic-Huffman code-length decoding path
    // against a real zlib stream, not against the JS Deflater's own output.
    val jvmDynamic = Array[Byte](120, -38, -19, -43, 59, 18, -62, 32, 0, -124, -31, -85, -84, 23, -16, 28, -106, 22, -71, 0, 24, 32, 40, 1, -61, 51, -31, -12, 50, -114, 39, 112, 44, 44, -74, -34, 111,
                                 -21, 127, 90, 20, -74, 98, 111, 15, -56, 24, -102, -121, 14, 59, -18, 101, 125, 38, -124, -86, 34, -14, -104, -99, -24, 7, -26, 96, -50, -104, -120, -119, -119, -119,
                                 -119, -119, -119, -119, -119, -119, 127, -121, -81, 98, -72, -11, -128, 28, -88, -39, -68, 64, -37, -86, -58, -44, -107, -121, -77, 91, 9, 113, 124, 77, 34, 36, 36,
                                 36, 36, 36, -4, 10, 94, 66, 67, 85, -69, -11, -58, 29, -97, 58, -51, 66, 103, 116, 37, -93, 72, -17, 62, -99, -120, -120, -120, -120, -120, -2, 23, -67, 0, -28, -99,
                                 50, -43)
    val expected =
      ("The quick brown fox jumps over the lazy dog. " * 60) +
        ("Pack my box with five dozen liquor jugs. " * 40) +
        ("How vexingly quick daft zebras jump! " * 40)
    val restored = inflate(jvmDynamic, nowrap = false)
    assertEquals(new String(restored, "US-ASCII"), expected)
  }

  test("Inflater decodes a JVM-produced RAW DEFLATE stream (nowrap=true)") {
    // JVM java.util.zip.Deflater(level=6, nowrap=true) of "raw deflate stream".
    val jvmRaw   = Array[Byte](43, 74, 44, 87, 72, 73, 77, -53, 73, 44, 73, 85, 40, 46, 41, 74, 77, -52, 5, 0)
    val restored = inflate(jvmRaw, nowrap = true)
    assertEquals(new String(restored, "US-ASCII"), "raw deflate stream")
  }

  // ==========================================================================
  // 6. DeflaterOutputStream -> InflaterInputStream round-trip
  // ==========================================================================

  RoundTripInputs.foreach { case (name, data) =>
    test(s"DeflaterOutputStream -> InflaterInputStream round-trip: $name") {
      val bos = new ByteArrayOutputStream()
      val dos = new DeflaterOutputStream(bos)
      dos.write(data)
      dos.close()
      val compressed = bos.toByteArray()

      val restored = drain(new InflaterInputStream(new ByteArrayInputStream(compressed)))
      assertEquals(restored.toList, data.toList)
    }
  }

  // ==========================================================================
  // 7. GZIPOutputStream -> GZIPInputStream round-trip + JVM interop decode
  // ==========================================================================

  RoundTripInputs.foreach { case (name, data) =>
    test(s"GZIPOutputStream -> GZIPInputStream round-trip: $name") {
      val bos = new ByteArrayOutputStream()
      val gos = new GZIPOutputStream(bos)
      gos.write(data)
      gos.close()
      val compressed = bos.toByteArray()

      val restored = drain(new GZIPInputStream(new ByteArrayInputStream(compressed)))
      assertEquals(restored.toList, data.toList)
    }
  }

  test("GZIPInputStream decodes a JVM-produced gzip stream (RFC 1952)") {
    // JVM java.util.zip.GZIPOutputStream of "hello, zip world" with the 4-byte
    // MTIME field zeroed (GZIPInputStream ignores MTIME on read, so the decode
    // is deterministic). Magic 1f 8b, CM=8, then CRC32 + ISIZE trailer.
    val jvmGzip  = Array[Byte](31, -117, 8, 0, 0, 0, 0, 0, 0, -1, -53, 72, -51, -55, -55, -41, 81, -88, -54, 44, 80, 40, -49, 47, -54, 73, 1, 0, 117, -13, -108, -63, 16, 0, 0, 0)
    val restored = drain(new GZIPInputStream(new ByteArrayInputStream(jvmGzip)))
    assertEquals(new String(restored, "US-ASCII"), "hello, zip world")
  }

  // ==========================================================================
  // 8. CheckedOutputStream wired to a CRC32
  // ==========================================================================

  test("CheckedOutputStream with CRC32 yields the CRC32 of the bytes written") {
    val data = ascii("The quick brown fox jumps over the lazy dog")

    val bos = new ByteArrayOutputStream()
    val crc = new CRC32()
    val cos = new CheckedOutputStream(bos, crc)
    cos.write(data)
    cos.close()

    // Bytes pass through unchanged...
    assertEquals(bos.toByteArray().toList, data.toList)
    // ...and the running checksum equals CRC32 of those bytes (0x414FA339).
    assertEquals(cos.getChecksum().getValue(), 0x414fa339L)
    assertEquals(crc.getValue(), 0x414fa339L)
  }
}
