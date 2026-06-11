/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-506 (BaseTmxMapLoader.getTileIds premature-EOF detection
 * defeated).
 *
 * Root cause being reproduced: in sge/src/main/scala/sge/maps/tiled/
 * BaseTmxMapLoader.scala (object BaseTmxMapLoader.getTileIds, refill loop
 * around lines 1006-1013) the port translates Java's `break` as
 *
 *   if (curr == -1) read = temp.length // break out
 *
 * which exits the inner refill loop by FORGING a full read, so the guard
 *
 *   if (read != temp.length) throw ... "Premature end of tile data"
 *
 * is unreachable. The original Java (original-src/libgdx/gdx/src/com/badlogic/
 * gdx/maps/tiled/BaseTmxMapLoader.java lines 709-721) breaks out with `read`
 * still short and THROWS GdxRuntimeException("Error Reading TMX Layer Data:
 * Premature end of tile data"). The port instead builds the tile id from a
 * partially refilled 4-byte buffer whose tail still holds STALE bytes of the
 * previous tile — corrupt maps load "successfully".
 *
 * Crafted-data construction (important — naive truncation does NOT repro):
 * cutting the gzip/zlib COMPRESSED bytes (or the base64 text) makes the
 * inflater throw EOFException mid-stream, which both Java and the port catch
 * as IOException and rethrow — green everywhere. And a payload short by a
 * MULTIPLE of 4 makes the next group's first `is.read(temp)` return -1, so
 * `is.read(temp, -1, 5)` throws IndexOutOfBoundsException in both. The red
 * scenario is a COMPLETE, well-formed gzip/zlib stream whose DECOMPRESSED
 * payload is short by a non-multiple of 4 and ends mid-final-tile-group:
 * a 4x4 layer needs 16*4 = 64 raw bytes; we compress only 62. The stream
 * then EOFs cleanly (read() == -1) on the last group with read == 2:
 * Java -> break -> 2 != 4 -> throws; port -> read := 4 -> guard passes ->
 * ids(15) = 2 fresh bytes | 2 stale bytes of tile 15's buffer -> returns.
 *
 * Raw payload: tile i (1-based) has all four little-endian bytes == i, so
 * the expected id of tile i is i * 0x01010101 and any stale-byte mixing is
 * visible (at the red commit the port yields 0x0f0f1010 for the last tile
 * instead of throwing).
 *
 * Platform placement: JVM and Native copies only (byte-identical), like the
 * sibling RedSuites: XmlReaderImpl is platform-split (scaladesktop covers
 * JVM+Native) and java.util.zip.GZIP*Stream does not exist on Scala.js, so
 * the JS test linker would reject this path.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package maps
package tiled

import java.io.ByteArrayOutputStream
import java.util.zip.{ DeflaterOutputStream, GZIPOutputStream }

import scala.util.{ Failure, Success, Try }

import sge.utils.XmlReader

class TmxTileIdsEofRedSuite extends munit.FunSuite {

  /** Raw little-endian layer data: tile i (1-based) has all four bytes == i, hence id i * 0x01010101. */
  private def rawTileBytes(tileCount: Int): Array[Byte] = {
    val bytes = new Array[Byte](tileCount * 4)
    var i     = 0
    while (i < bytes.length) {
      bytes(i) = (i / 4 + 1).toByte
      i += 1
    }
    bytes
  }

  /** Complete, well-formed gzip stream of `raw`, base64-encoded (no line breaks — the port uses the basic decoder). */
  private def gzipBase64(raw: Array[Byte]): String = {
    val bos = new ByteArrayOutputStream()
    val gz  = new GZIPOutputStream(bos)
    gz.write(raw)
    gz.close()
    java.util.Base64.getEncoder().encodeToString(bos.toByteArray)
  }

  /** Complete, well-formed zlib stream of `raw`, base64-encoded. */
  private def zlibBase64(raw: Array[Byte]): String = {
    val bos = new ByteArrayOutputStream()
    val df  = new DeflaterOutputStream(bos)
    df.write(raw)
    df.close()
    java.util.Base64.getEncoder().encodeToString(bos.toByteArray)
  }

  /** Minimal TMX layer element with base64 data; payload kept on one line so the basic Base64 decoder accepts it. */
  private def layerElement(payload: String, compression: String): XmlReader.Element =
    new XmlReader().parse(
      s"""<layer name="layer1" width="4" height="4"><data encoding="base64" compression="$compression">$payload</data></layer>"""
    )

  /** 64 raw bytes for the full 4x4 layer; the truncated variants keep only 62 (mid final tile group). */
  private val fullRaw      = rawTileBytes(16)
  private val truncatedRaw = java.util.Arrays.copyOf(fullRaw, 62)

  private def assertPrematureEof(element: XmlReader.Element): Unit =
    Try(BaseTmxMapLoader.getTileIds(element, 4, 4)) match {
      case Success(ids) =>
        fail(
          "getTileIds silently loaded a layer whose decompressed data is 62 of 64 bytes; " +
            "last tile id = 0x" + ids(15).toHexString +
            " (2 fresh bytes | 2 stale bytes of the previous tile's buffer) — " +
            "Java throws 'Error Reading TMX Layer Data: Premature end of tile data'"
        )
      case Failure(e: IllegalArgumentException) =>
        assert(
          e.getMessage.contains("Premature end of tile data"),
          s"expected the premature-EOF message, got: ${e.getMessage}"
        )
      case Failure(e) =>
        fail(s"expected IllegalArgumentException('... Premature end of tile data'), got ${e.getClass.getName}: ${e.getMessage}")
    }

  test("ISS-506: gzip data decompressing to 62 of 64 bytes must throw 'Premature end of tile data'") {
    assertPrematureEof(layerElement(gzipBase64(truncatedRaw), "gzip"))
  }

  test("ISS-506: zlib data decompressing to 62 of 64 bytes must throw 'Premature end of tile data'") {
    assertPrematureEof(layerElement(zlibBase64(truncatedRaw), "zlib"))
  }

  test("ISS-506 control (green at red commit): complete gzip layer parses to the exact 16 tile ids") {
    val ids = BaseTmxMapLoader.getTileIds(layerElement(gzipBase64(fullRaw), "gzip"), 4, 4)
    // Pins the happy path so the fix cannot over-throw on well-formed data.
    assertEquals(ids.toList, (1 to 16).map(_ * 0x01010101).toList)
  }

  test("ISS-506 control (green at red commit): complete zlib layer parses to the exact 16 tile ids") {
    val ids = BaseTmxMapLoader.getTileIds(layerElement(zlibBase64(fullRaw), "zlib"), 4, 4)
    assertEquals(ids.toList, (1 to 16).map(_ * 0x01010101).toList)
  }
}
