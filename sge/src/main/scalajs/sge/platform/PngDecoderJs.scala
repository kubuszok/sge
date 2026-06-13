// SGE - Scala Game Engine
// Copyright 2024-2026 Mateusz Kubuszok
// Licensed under the Apache License, Version 2.0
//
// Synchronous pure-Scala PNG decoder for the Scala.js platform (ISS-533 /
// ISS-651).
//
// The browser image pipeline (BrowserAssetLoader -> HTMLImageElement -> Canvas)
// is asynchronous and unavailable when a Pixmap is built synchronously from
// in-memory bytes that were never preloaded (e.g. the bytes PixmapIO.writePNG
// just produced, or headless test/tool code under Node). This decoder gives
// Gdx2dOpsJs.decodeImage a synchronous fallback so such PNGs round-trip on the
// JS baseline exactly as they do on JVM and Native.
//
// Supports the 8-bit non-interlaced PNG color types SGE actually emits and the
// common variants: greyscale (0), truecolor (2), indexed (3), greyscale+alpha
// (4), truecolor+alpha (6), with all five scanline filters (0-4). Output is
// always RGBA8888, matching the gdx2d decode contract (GDX2D_FORMAT_RGBA8888).

package sge
package platform

import java.io.{ ByteArrayInputStream, DataInputStream }
import java.util.zip.{ DataFormatException, Inflater }

private[platform] object PngDecoderJs {

  private val Signature = Array[Byte](-119, 80, 78, 71, 13, 10, 26, 10)

  /** Decodes a PNG byte range to an RGBA8888 result, or None if `data` is not a PNG this decoder understands (so the caller can fall back to its own error path).
    */
  def decode(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    val hasSignature =
      len >= 8 && {
        var i  = 0
        var ok = true
        while (i < 8 && ok) { if (data(offset + i) != Signature(i)) ok = false; i += 1 }
        ok
      }
    if (!hasSignature) None
    else
      try Some(decodeChecked(data, offset, len))
      catch {
        case _: DataFormatException       => None
        case _: PngError                  => None
        case _: java.io.IOException       => None
        case _: IndexOutOfBoundsException => None
      }
  }

  final private class PngError(msg: String) extends RuntimeException(msg)

  private def decodeChecked(data: Array[Byte], offset: Int, len: Int): Gdx2dOps.DecodeResult = {
    val in = new DataInputStream(new ByteArrayInputStream(data, offset, len))
    in.skipBytes(8) // signature

    var width     = 0
    var height    = 0
    var bitDepth  = 0
    var colorType = 0
    var interlace = 0
    val idat      = new java.io.ByteArrayOutputStream()
    var palette:      Array[Byte] = null // RGB triplets
    var transparency: Array[Byte] = null // tRNS
    var sawIHDR = false

    scala.util.boundary {
      while (true) {
        val length    = in.readInt()
        val typeBytes = new Array[Byte](4)
        in.readFully(typeBytes)
        val chunkType = new String(typeBytes, "US-ASCII")
        chunkType match {
          case "IHDR" =>
            width = in.readInt()
            height = in.readInt()
            bitDepth = in.readUnsignedByte()
            colorType = in.readUnsignedByte()
            in.readUnsignedByte() // compression method (always 0)
            in.readUnsignedByte() // filter method (always 0)
            interlace = in.readUnsignedByte()
            in.readInt() // CRC
            sawIHDR = true
          case "PLTE" =>
            palette = new Array[Byte](length)
            in.readFully(palette)
            in.readInt() // CRC
          case "tRNS" =>
            transparency = new Array[Byte](length)
            in.readFully(transparency)
            in.readInt() // CRC
          case "IDAT" =>
            val buf = new Array[Byte](length)
            in.readFully(buf)
            idat.write(buf, 0, length)
            in.readInt() // CRC
          case "IEND" =>
            in.readInt() // CRC
            scala.util.boundary.break() // last chunk
          case _ =>
            in.skipBytes(length + 4) // skip data + CRC
        }
      }
    }

    if (!sawIHDR) throw new PngError("missing IHDR")
    if (bitDepth != 8) throw new PngError(s"unsupported bit depth $bitDepth")
    if (interlace != 0) throw new PngError("interlaced PNG not supported")

    val channels = colorType match {
      case 0     => 1 // greyscale
      case 2     => 3 // truecolor
      case 3     => 1 // indexed (palette index)
      case 4     => 2 // greyscale + alpha
      case 6     => 4 // truecolor + alpha
      case other => throw new PngError(s"unsupported color type $other")
    }

    // Inflate the concatenated IDAT (zlib stream).
    val inflater = new Inflater()
    inflater.setInput(idat.toByteArray)
    val rawOut = new java.io.ByteArrayOutputStream()
    val ibuf   = new Array[Byte](16384)
    var r      = inflater.inflate(ibuf)
    while (r > 0) { rawOut.write(ibuf, 0, r); r = inflater.inflate(ibuf) }
    inflater.end()
    val raw = rawOut.toByteArray

    val bpp       = channels // bytes per pixel (8-bit depth)
    val lineBytes = width * bpp
    if (raw.length < (lineBytes + 1) * height) throw new PngError("truncated image data")

    // Unfilter scanlines in place into `recon` (no filter byte).
    val recon = new Array[Byte](lineBytes * height)
    var y     = 0
    while (y < height) {
      val filterType = raw((lineBytes + 1) * y) & 0xff
      val srcOff     = (lineBytes + 1) * y + 1
      val dstOff     = lineBytes * y
      var x          = 0
      while (x < lineBytes) {
        val rawByte = raw(srcOff + x) & 0xff
        val a       = if (x >= bpp) recon(dstOff + x - bpp) & 0xff else 0
        val b       = if (y > 0) recon(dstOff - lineBytes + x) & 0xff else 0
        val c       = if (x >= bpp && y > 0) recon(dstOff - lineBytes + x - bpp) & 0xff else 0
        val value   = filterType match {
          case 0     => rawByte
          case 1     => rawByte + a
          case 2     => rawByte + b
          case 3     => rawByte + ((a + b) >> 1)
          case 4     => rawByte + paeth(a, b, c)
          case other => throw new PngError(s"unsupported filter type $other")
        }
        recon(dstOff + x) = (value & 0xff).toByte
        x += 1
      }
      y += 1
    }

    // Expand to RGBA8888.
    val rgba       = new Array[Byte](width * height * 4)
    var p          = 0
    val pixelCount = width * height
    while (p < pixelCount) {
      val si = p * bpp
      val di = p * 4
      colorType match {
        case 0 => // greyscale
          val g = recon(si) & 0xff
          rgba(di) = g.toByte; rgba(di + 1) = g.toByte; rgba(di + 2) = g.toByte; rgba(di + 3) = 0xff.toByte
        case 2 => // RGB
          rgba(di) = recon(si); rgba(di + 1) = recon(si + 1); rgba(di + 2) = recon(si + 2); rgba(di + 3) = 0xff.toByte
        case 3 => // palette
          val idx = recon(si) & 0xff
          if (palette == null || idx * 3 + 2 >= palette.length) throw new PngError("palette index out of range")
          rgba(di) = palette(idx * 3); rgba(di + 1) = palette(idx * 3 + 1); rgba(di + 2) = palette(idx * 3 + 2)
          rgba(di + 3) = (if (transparency != null && idx < transparency.length) transparency(idx) & 0xff else 0xff).toByte
        case 4 => // greyscale + alpha
          val g = recon(si) & 0xff
          rgba(di) = g.toByte; rgba(di + 1) = g.toByte; rgba(di + 2) = g.toByte; rgba(di + 3) = recon(si + 1)
        case 6 => // RGBA
          rgba(di) = recon(si); rgba(di + 1) = recon(si + 1); rgba(di + 2) = recon(si + 2); rgba(di + 3) = recon(si + 3)
        case _ => // unreachable (validated above)
      }
      p += 1
    }

    Gdx2dOps.DecodeResult(width, height, 4, java.nio.ByteBuffer.wrap(rgba))
  }

  private def paeth(a: Int, b: Int, c: Int): Int = {
    val p  = a + b - c
    val pa = scala.math.abs(p - a)
    val pb = scala.math.abs(p - b)
    val pc = scala.math.abs(p - c)
    if (pa <= pb && pa <= pc) a
    else if (pb <= pc) b
    else c
  }
}
