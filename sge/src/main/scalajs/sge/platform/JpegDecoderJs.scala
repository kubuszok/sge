// SGE - Scala Game Engine
// Copyright 2024-2026 Mateusz Kubuszok
// Licensed under the Apache License, Version 2.0
//
// Synchronous pure-Scala baseline JPEG decoder for the Scala.js platform.
//
// Companion to PngDecoderJs: restores non-PNG synchronous image decoding on the
// Scala.js baseline (assets are embedded at build time and served
// synchronously, so there is no async browser-Canvas decode path). Output is
// always RGBA8888, matching the gdx2d decode contract (GDX2D_FORMAT_RGBA8888).
//
// Supported subset: BASELINE SEQUENTIAL DCT (SOF0/SOF1) and PROGRESSIVE DCT
// (SOF2) — Huffman entropy coding, quantization tables (DQT), grayscale (1
// component) and YCbCr (3 components), chroma subsampling 4:4:4 / 4:2:2 / 4:2:0
// (any integer Hi/Vi sampling factors), restart markers (DRI / RST0-7),
// multi-scan spectral-selection + successive bit-plane refinement (ISS-784,
// for cross-platform parity with the JVM/Native gdx2d/stb_image loaders), and the
// standard YCbCr->RGB / level-shift conversion. A floating-point separable 8x8
// IDCT is used.
//
// OUT OF SCOPE (returns None, never crashes): arithmetic coding (SOF9-11),
// lossless (SOF3), hierarchical, CMYK / YCCK and Adobe APP14 transform variants,
// and 12-bit samples.

package sge
package platform

private[platform] object JpegDecoderJs {

  /** Decodes a baseline-JPEG byte range to an RGBA8888 result, or None if `data` is not a baseline JPEG this decoder understands.
    */
  def decode(data: Array[Byte], offset: Int, len: Int): Option[Gdx2dOps.DecodeResult] = {
    // SOI marker: FF D8.
    val hasSignature = len >= 2 && (data(offset) & 0xff) == 0xff && (data(offset + 1) & 0xff) == 0xd8
    if (!hasSignature) None
    else
      try new Decoder(data, offset, offset + len).run()
      catch {
        case _: IndexOutOfBoundsException => None
        case _: JpegError                 => None
      }
  }

  final private class JpegError(msg: String) extends RuntimeException(msg)

  // ---- Huffman table: canonical decode via (mincode/maxcode/valptr). ----
  final private class HuffTable {
    val minCode = new Array[Int](17)
    val maxCode = new Array[Int](18)
    val valPtr  = new Array[Int](17)
    var values: Array[Int] = new Array[Int](0)

    def build(counts: Array[Int], vals: Array[Int]): Unit = {
      values = vals
      val sizes = new Array[Int](256)
      var k     = 0
      var l     = 1
      while (l <= 16) {
        var i = 0
        while (i < counts(l)) {
          sizes(k) = l
          k += 1
          i += 1
        }
        l += 1
      }
      val total = k
      // Assign canonical codes.
      val codes = new Array[Int](256)
      var code  = 0
      var si    = if (total > 0) sizes(0) else 0
      k = 0
      while (k < total) {
        while (sizes(k) == si) {
          codes(k) = code
          code += 1
          k += 1
          if (k >= total) si = -1
        }
        if (k < total) {
          code <<= 1
          si += 1
        }
      }
      // Build min/max/valptr per length.
      var p = 0
      l = 1
      while (l <= 16) {
        if (counts(l) > 0) {
          valPtr(l) = p
          minCode(l) = codes(p)
          p += counts(l)
          maxCode(l) = codes(p - 1)
        } else {
          maxCode(l) = -1
        }
        l += 1
      }
      maxCode(17) = 0xfffff
    }
  }

  // ---- Component metadata. ----
  final private class Component {
    var id    = 0
    var h     = 1 // horizontal sampling factor
    var v     = 1 // vertical sampling factor
    var quant = 0 // quant table index
    var dcTab = 0
    var acTab = 0
    var pred  = 0 // running DC predictor
    // Per-component fully-upsampled plane (filled during MCU assembly).
    var plane: Array[Byte] = Array.emptyByteArray
    var planeW = 0
    var planeH = 0
    // Progressive-mode coefficient storage (natural 8x8 order per block, not
    // dequantized). blocksPerLine/Column are the component's actual block grid
    // (used for non-interleaved AC scans); the *ForMcu variants are padded to whole
    // MCUs (the addressing stride and interleaved DC scan bounds).
    var blockData: Array[Int] = new Array[Int](0)
    var blocksPerLine         = 0
    var blocksPerColumn       = 0
    var blocksPerLineForMcu   = 0
    var blocksPerColumnForMcu = 0
  }

  final private class Decoder(data: Array[Byte], base: Int, end: Int) {
    private var pos = base

    private val quantTables = Array.ofDim[Int](4, 64)
    private val dcTables    = Array.fill(4)(new HuffTable())
    private val acTables    = Array.fill(4)(new HuffTable())

    private var width  = 0
    private var height = 0
    private var components: Array[Component] = Array.empty
    private var restartInterval = 0

    // Progressive (SOF2) state.
    private var progressive   = false
    private var hMax          = 1
    private var vMax          = 1
    private var mcusPerLine   = 0
    private var mcusPerColumn = 0
    // Current scan's spectral selection + successive bit-plane (Ah/Al) parameters.
    private var spectralStart  = 0
    private var spectralEnd    = 0
    private var successiveHigh = 0
    private var successiveLow  = 0
    private var eobrun         = 0 // end-of-band run counter (AC progressive scans)

    // Bit reader state (entropy-coded segment, with FF00 byte-stuffing and
    // marker handling).
    private var bitBuf = 0
    private var bitCnt = 0
    private var marker = 0 // a marker encountered mid-scan (e.g. RSTn / EOI)

    private def u8(): Int = {
      val v = data(pos) & 0xff
      pos += 1
      v
    }
    private def u16(): Int = {
      val hi = u8()
      val lo = u8()
      (hi << 8) | lo
    }

    def run(): Option[Gdx2dOps.DecodeResult] =
      if (u16() != 0xffd8) None // SOI
      else decodeMarkers()

    private def decodeMarkers(): Option[Gdx2dOps.DecodeResult] =
      scala.util.boundary {
        var sawSOF = false
        while (true) {
          if (pos >= end) scala.util.boundary.break(if (progressive && sawSOF) Some(assembleProgressive()) else None)
          // Find next marker (skip fill bytes).
          var b = u8()
          while (b != 0xff && pos < end) b = u8()
          if (pos >= end) scala.util.boundary.break(if (progressive && sawSOF) Some(assembleProgressive()) else None)
          var m = u8()
          while (m == 0xff && pos < end) m = u8() // skip fill FFs
          m match {
            case 0xc0 => // SOF0 baseline
              readSOF(false)
              sawSOF = true
            case 0xc1 => // SOF1 extended sequential — same layout; treat as baseline
              readSOF(false)
              sawSOF = true
            case 0xc2 => // SOF2 progressive DCT
              readSOF(true)
              sawSOF = true
            case 0xc3 | 0xc5 | 0xc6 | 0xc7 | 0xc9 | 0xca | 0xcb | 0xcd | 0xce | 0xcf =>
              // Lossless / arithmetic / hierarchical / differential: unsupported.
              scala.util.boundary.break(None)
            case 0xc4 => readDHT()
            case 0xdb => readDQT()
            case 0xdd => readDRI()
            case 0xda => // SOS
              if (!sawSOF) scala.util.boundary.break(None)
              else if (progressive) readProgressiveScan() // one of possibly many scans; keep looping
              else {
                readSOS()
                // first scan completes a baseline image
                scala.util.boundary.break(Some(assemble()))
              }
            case 0xd9 =>
              // EOI: a progressive image is complete once all scans are read.
              scala.util.boundary.break(if (progressive && sawSOF) Some(assembleProgressive()) else None)
            case 0x01                        => () // TEM, no payload
            case x if x >= 0xd0 && x <= 0xd7 => () // RSTn outside scan, no payload
            case _                           =>
              // Skip any other marker segment by its length.
              val seg = u16()
              pos += (seg - 2)
          }
        }
        None // unreachable: the while(true) only exits via boundary.break
      }

    private def readSOF(isProgressive: Boolean): Unit = {
      progressive = isProgressive
      val _    = u16() // segment length
      val prec = u8()
      if (prec != 8) throw new JpegError("only 8-bit precision supported")
      height = u16()
      width = u16()
      val nComp = u8()
      if (nComp != 1 && nComp != 3) throw new JpegError(s"unsupported component count $nComp")
      val comps = new Array[Component](nComp)
      var i     = 0
      while (i < nComp) {
        val c = new Component()
        c.id = u8()
        val hv = u8()
        c.h = (hv >> 4) & 0x0f
        c.v = hv & 0x0f
        c.quant = u8()
        if (c.h < 1 || c.v < 1) throw new JpegError("invalid sampling factor")
        comps(i) = c
        i += 1
      }
      components = comps
      if (progressive) setupProgressive()
    }

    // Compute the frame/component block geometry and allocate the per-component
    // coefficient stores used to accumulate progressive scans.
    private def setupProgressive(): Unit = {
      hMax = 1
      vMax = 1
      var ci = 0
      while (ci < components.length) {
        if (components(ci).h > hMax) hMax = components(ci).h
        if (components(ci).v > vMax) vMax = components(ci).v
        ci += 1
      }
      mcusPerLine = (width + 8 * hMax - 1) / (8 * hMax)
      mcusPerColumn = (height + 8 * vMax - 1) / (8 * vMax)
      val widthBlocks  = (width + 7) / 8
      val heightBlocks = (height + 7) / 8
      ci = 0
      while (ci < components.length) {
        val c = components(ci)
        c.blocksPerLine = (widthBlocks * c.h + hMax - 1) / hMax
        c.blocksPerColumn = (heightBlocks * c.v + vMax - 1) / vMax
        c.blocksPerLineForMcu = mcusPerLine * c.h
        c.blocksPerColumnForMcu = mcusPerColumn * c.v
        c.blockData = new Array[Int](c.blocksPerLineForMcu * c.blocksPerColumnForMcu * 64)
        ci += 1
      }
    }

    private def readDQT(): Unit = {
      var segLen = u16() - 2
      while (segLen > 0) {
        val pq_tq = u8()
        segLen -= 1
        val pq = (pq_tq >> 4) & 0x0f // precision: 0 => 8-bit, 1 => 16-bit
        val tq = pq_tq & 0x0f
        if (tq > 3) throw new JpegError("bad quant table id")
        val tbl = quantTables(tq)
        var i   = 0
        while (i < 64) {
          val v =
            if (pq == 0) { val x = u8(); segLen -= 1; x }
            else { val x = u16(); segLen -= 2; x }
          tbl(i) = v
          i += 1
        }
      }
    }

    private def readDHT(): Unit = {
      var segLen = u16() - 2
      while (segLen > 0) {
        val tc_th = u8()
        segLen -= 1
        val tc = (tc_th >> 4) & 0x0f // 0 => DC, 1 => AC
        val th = tc_th & 0x0f
        if (th > 3) throw new JpegError("bad huffman table id")
        val counts = new Array[Int](17)
        var total  = 0
        var l      = 1
        while (l <= 16) {
          val n = u8()
          counts(l) = n
          total += n
          segLen -= 1
          l += 1
        }
        val vals = new Array[Int](total)
        var i    = 0
        while (i < total) {
          vals(i) = u8()
          segLen -= 1
          i += 1
        }
        if (tc == 0) dcTables(th).build(counts, vals)
        else acTables(th).build(counts, vals)
      }
    }

    private def readDRI(): Unit = {
      val _ = u16() // length (always 4)
      restartInterval = u16()
    }

    private def readSOS(): Unit = {
      val _  = u16() // length
      val ns = u8()
      if (ns != components.length) throw new JpegError("SOS component count mismatch")
      var i = 0
      while (i < ns) {
        val cs   = u8()
        val tdta = u8()
        // Find the component with this id.
        var ci    = 0
        var found = -1
        while (ci < components.length) {
          if (components(ci).id == cs) found = ci
          ci += 1
        }
        if (found < 0) throw new JpegError("SOS references unknown component")
        components(found).dcTab = (tdta >> 4) & 0x0f
        components(found).acTab = tdta & 0x0f
        i += 1
      }
      u8() // Ss (0 for baseline)
      u8() // Se (63)
      u8() // Ah/Al
      // Entropy-coded data starts at `pos`.
    }

    // -- Bit reading over the entropy-coded segment. --
    private def resetBits(): Unit = {
      bitBuf = 0
      bitCnt = 0
      marker = 0
    }

    private def nextBit(): Int =
      if (marker != 0) 0 // a marker was hit; feed zero bits until the caller realigns
      else {
        if (bitCnt == 0) {
          if (pos >= end) marker = 0xd9
          else {
            var b = u8()
            if (b == 0xff) {
              var b2 = u8()
              while (b2 == 0xff && pos < end) b2 = u8() // fill bytes before a marker
              if (b2 == 0x00) {
                // Stuffed byte: literal 0xFF.
              } else {
                // Any real marker (RSTn, EOI, ...) ends entropy data here.
                marker = b2
                pos -= 2 // leave "FF Mn" in the stream for the restart/EOI handler
                b = 0
              }
            }
            bitBuf = b
            bitCnt = 8
          }
        }
        if (marker != 0) 0
        else {
          bitCnt -= 1
          (bitBuf >> bitCnt) & 1
        }
      }

    private def receive(n: Int): Int = {
      var v = 0
      var i = 0
      while (i < n) {
        v = (v << 1) | nextBit()
        i += 1
      }
      v
    }

    // Extend a received value of magnitude category `t` to a signed value.
    private def extend(v: Int, t: Int): Int =
      if (t == 0) 0
      else if (v < (1 << (t - 1))) v - (1 << t) + 1
      else v

    private def decodeHuff(tbl: HuffTable): Int = {
      var code = 0
      var l    = 1
      var res  = -1
      scala.util.boundary {
        while (l <= 16) {
          code = (code << 1) | nextBit()
          if (tbl.maxCode(l) >= 0 && code <= tbl.maxCode(l) && code >= tbl.minCode(l)) {
            res = tbl.values(tbl.valPtr(l) + (code - tbl.minCode(l)))
            scala.util.boundary.break()
          }
          l += 1
        }
      }
      if (res < 0) throw new JpegError("invalid huffman code")
      res
    }

    // Zig-zag order.
    private val zigzag = Array(
      0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52,
      45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63
    )

    // Decode one 8x8 block into a dequantized, IDCT'd, level-shifted byte block.
    private def decodeBlock(comp: Component, outBlock: Array[Int]): Unit = {
      val coef = new Array[Int](64)
      val q    = quantTables(comp.quant)
      val dcT  = dcTables(comp.dcTab)
      val acT  = acTables(comp.acTab)

      // DC.
      val t    = decodeHuff(dcT)
      val diff = extend(receive(t), t)
      comp.pred += diff
      coef(0) = comp.pred * q(0)

      // AC.
      var k = 1
      scala.util.boundary {
        while (k < 64) {
          val rs = decodeHuff(acT)
          val r  = (rs >> 4) & 0x0f
          val s  = rs & 0x0f
          if (s == 0) {
            if (r == 15) k += 16 // ZRL: skip 16 zeros
            else scala.util.boundary.break() // EOB
          } else {
            k += r
            if (k >= 64) scala.util.boundary.break()
            val v = extend(receive(s), s)
            coef(zigzag(k)) = v * q(k)
            k += 1
          }
        }
      }
      idct8x8(coef, outBlock)
    }

    // Separable float IDCT (8x8) with level shift + clamp to [0,255].
    private val idctTmp = new Array[Double](64)
    private def idct8x8(in: Array[Int], out: Array[Int]): Unit = {
      // Rows.
      var i = 0
      while (i < 8) {
        idct1d(in, i * 8, 1, idctTmp, i * 8, 1)
        i += 1
      }
      // Columns.
      var j = 0
      while (j < 8) {
        idct1d8col(idctTmp, j)
        j += 1
      }
      // Level shift + clamp. The separable 1/2 + 1/2 factors already yield the
      // 1/4 normalization of the 2D IDCT, so no further scaling is needed.
      var p = 0
      while (p < 64) {
        val v = scala.math.round(idctTmp(p)).toInt + 128
        out(p) = if (v < 0) 0 else if (v > 255) 255 else v
        p += 1
      }
    }

    // Precomputed cosine table: C[u][x] = cos((2x+1)u*pi/16) * (u==0 ? 1/sqrt2 : 1).
    private val cosTab: Array[Array[Double]] = {
      val t = Array.ofDim[Double](8, 8)
      var u = 0
      while (u < 8) {
        val cu = if (u == 0) 1.0 / scala.math.sqrt(2.0) else 1.0
        var x  = 0
        while (x < 8) {
          t(u)(x) = cu * scala.math.cos(((2 * x + 1) * u * scala.math.Pi) / 16.0)
          x += 1
        }
        u += 1
      }
      t
    }

    // 1D IDCT over 8 samples: in[inOff + k*inStride] -> out[outOff + x*outStride].
    private def idct1d(in: Array[Int], inOff: Int, inStride: Int, out: Array[Double], outOff: Int, outStride: Int): Unit = {
      var x = 0
      while (x < 8) {
        var sum = 0.0
        var u   = 0
        while (u < 8) {
          sum += cosTab(u)(x) * in(inOff + u * inStride)
          u += 1
        }
        out(outOff + x * outStride) = sum * 0.5
        x += 1
      }
    }

    // 1D IDCT over a column of the temp buffer (in place).
    private def idct1d8col(buf: Array[Double], col: Int): Unit = {
      val tmp = new Array[Double](8)
      var x   = 0
      while (x < 8) {
        var sum = 0.0
        var u   = 0
        while (u < 8) {
          sum += cosTab(u)(x) * buf(u * 8 + col)
          u += 1
        }
        tmp(x) = sum * 0.5
        x += 1
      }
      x = 0
      while (x < 8) {
        buf(x * 8 + col) = tmp(x)
        x += 1
      }
    }

    private def assemble(): Gdx2dOps.DecodeResult = {
      val nComp = components.length
      var hMax  = 1
      var vMax  = 1
      var ci    = 0
      while (ci < nComp) {
        if (components(ci).h > hMax) hMax = components(ci).h
        if (components(ci).v > vMax) vMax = components(ci).v
        ci += 1
      }
      val mcuW  = 8 * hMax
      val mcuH  = 8 * vMax
      val mcusX = (width + mcuW - 1) / mcuW
      val mcusY = (height + mcuH - 1) / mcuH

      // Allocate per-component planes sized to the padded component grid.
      ci = 0
      while (ci < nComp) {
        val c = components(ci)
        c.planeW = mcusX * c.h * 8
        c.planeH = mcusY * c.v * 8
        c.plane = new Array[Byte](c.planeW * c.planeH)
        c.pred = 0
        ci += 1
      }

      resetBits()
      val block        = new Array[Int](64)
      var restartCount = 0

      var my = 0
      while (my < mcusY) {
        var mx = 0
        while (mx < mcusX) {
          // Handle restart interval boundaries.
          if (restartInterval > 0 && restartCount == restartInterval) {
            consumeRestart()
            ci = 0
            while (ci < nComp) { components(ci).pred = 0; ci += 1 }
            restartCount = 0
          }
          ci = 0
          while (ci < nComp) {
            val c  = components(ci)
            var by = 0
            while (by < c.v) {
              var bx = 0
              while (bx < c.h) {
                decodeBlock(c, block)
                // Place block into the component plane.
                val px0 = (mx * c.h + bx) * 8
                val py0 = (my * c.v + by) * 8
                var yy  = 0
                while (yy < 8) {
                  val rowBase = (py0 + yy) * c.planeW + px0
                  var xx      = 0
                  while (xx < 8) {
                    c.plane(rowBase + xx) = block(yy * 8 + xx).toByte
                    xx += 1
                  }
                  yy += 1
                }
                bx += 1
              }
              by += 1
            }
            ci += 1
          }
          restartCount += 1
          mx += 1
        }
        my += 1
      }

      Gdx2dOps.DecodeResult(width, height, 4, java.nio.ByteBuffer.wrap(planesToRgba(hMax, vMax)))
    }

    // Upsample the per-component planes and color-convert to RGBA8888. Shared by the
    // baseline and progressive paths (both populate component.plane / planeW).
    private def planesToRgba(hMaximum: Int, vMaximum: Int): Array[Byte] = {
      val nComp = components.length
      val rgba  = new Array[Byte](width * height * 4)
      if (nComp == 1) {
        val c = components(0)
        var y = 0
        while (y < height) {
          var x = 0
          while (x < width) {
            // Component plane may be subsampled relative to the image (rare for
            // a single component, but honor h/v anyway).
            val sx = x * c.h / hMaximum
            val sy = y * c.v / vMaximum
            val g  = c.plane(sy * c.planeW + sx) & 0xff
            val di = (y * width + x) * 4
            rgba(di) = g.toByte; rgba(di + 1) = g.toByte; rgba(di + 2) = g.toByte; rgba(di + 3) = 0xff.toByte
            x += 1
          }
          y += 1
        }
      } else {
        val cy = components(0)
        val cb = components(1)
        val cr = components(2)
        var y  = 0
        while (y < height) {
          var x = 0
          while (x < width) {
            val yv  = cy.plane((y * cy.v / vMaximum) * cy.planeW + (x * cy.h / hMaximum)) & 0xff
            val cbv = cb.plane((y * cb.v / vMaximum) * cb.planeW + (x * cb.h / hMaximum)) & 0xff
            val crv = cr.plane((y * cr.v / vMaximum) * cr.planeW + (x * cr.h / hMaximum)) & 0xff
            // YCbCr -> RGB (JFIF / ITU-R BT.601 full-range).
            val r  = yv + 1.402 * (crv - 128)
            val g  = yv - 0.344136 * (cbv - 128) - 0.714136 * (crv - 128)
            val b  = yv + 1.772 * (cbv - 128)
            val di = (y * width + x) * 4
            rgba(di) = clampByte(r)
            rgba(di + 1) = clampByte(g)
            rgba(di + 2) = clampByte(b)
            rgba(di + 3) = 0xff.toByte
            x += 1
          }
          y += 1
        }
      }
      rgba
    }

    // ---- Progressive (SOF2) decoding. ----

    // Reads a progressive scan header (SOS) and decodes its entropy-coded segment
    // into the per-component coefficient stores (accumulated across scans).
    private def readProgressiveScan(): Unit = {
      val _  = u16() // length
      val ns = u8()
      if (ns < 1 || ns > components.length) throw new JpegError("bad SOS component count")
      val scanComps = new Array[Component](ns)
      var i         = 0
      while (i < ns) {
        val cs    = u8()
        val tdta  = u8()
        var ci    = 0
        var found = -1
        while (ci < components.length) {
          if (components(ci).id == cs) found = ci
          ci += 1
        }
        if (found < 0) throw new JpegError("SOS references unknown component")
        val c = components(found)
        c.dcTab = (tdta >> 4) & 0x0f
        c.acTab = tdta & 0x0f
        scanComps(i) = c
        i += 1
      }
      spectralStart = u8()
      spectralEnd = u8()
      val ahal = u8()
      successiveHigh = (ahal >> 4) & 0x0f
      successiveLow = ahal & 0x0f
      decodeProgressiveScan(scanComps)
    }

    // Reset the DC predictors and end-of-band run at scan start and restart markers.
    private def resetScanState(): Unit = {
      eobrun = 0
      var ci = 0
      while (ci < components.length) { components(ci).pred = 0; ci += 1 }
    }

    private def decodeProgressiveScan(scanComps: Array[Component]): Unit = {
      resetBits()
      resetScanState()
      val ri           = restartInterval
      var restartCount = 0
      if (scanComps.length == 1) {
        // Non-interleaved scan: iterate the single component's own block grid.
        val c     = scanComps(0)
        val total = c.blocksPerLine * c.blocksPerColumn
        var n     = 0
        while (n < total) {
          if (ri > 0 && restartCount == ri) { consumeRestart(); resetScanState(); restartCount = 0 }
          val row         = n / c.blocksPerLine
          val col         = n % c.blocksPerLine
          val blockOffset = (row * c.blocksPerLineForMcu + col) * 64
          decodeProgressiveBlock(c, blockOffset)
          restartCount += 1
          n += 1
        }
      } else {
        // Interleaved scan (DC): iterate MCUs, each component's h*v blocks in order.
        val total = mcusPerLine * mcusPerColumn
        var mcu   = 0
        while (mcu < total) {
          if (ri > 0 && restartCount == ri) { consumeRestart(); resetScanState(); restartCount = 0 }
          val mcuRow = mcu / mcusPerLine
          val mcuCol = mcu % mcusPerLine
          var ci     = 0
          while (ci < scanComps.length) {
            val c  = scanComps(ci)
            var by = 0
            while (by < c.v) {
              var bx = 0
              while (bx < c.h) {
                val blockRow    = mcuRow * c.v + by
                val blockCol    = mcuCol * c.h + bx
                val blockOffset = (blockRow * c.blocksPerLineForMcu + blockCol) * 64
                decodeProgressiveBlock(c, blockOffset)
                bx += 1
              }
              by += 1
            }
            ci += 1
          }
          restartCount += 1
          mcu += 1
        }
      }
    }

    private def decodeProgressiveBlock(c: Component, blockOffset: Int): Unit =
      if (spectralStart == 0) {
        if (successiveHigh == 0) decodeDCFirst(c, blockOffset) else decodeDCSuccessive(c, blockOffset)
      } else {
        if (successiveHigh == 0) decodeACFirst(c, blockOffset) else decodeACSuccessive(c, blockOffset)
      }

    // DC first scan: full DC magnitude, low-order bits point-transformed away.
    private def decodeDCFirst(c: Component, blockOffset: Int): Unit = {
      val t    = decodeHuff(dcTables(c.dcTab))
      val diff = if (t == 0) 0 else extend(receive(t), t)
      c.pred += diff
      c.blockData(blockOffset) = c.pred << successiveLow
    }

    // DC refinement scan: one more low-order DC bit per block.
    private def decodeDCSuccessive(c: Component, blockOffset: Int): Unit =
      if (nextBit() != 0) c.blockData(blockOffset) = c.blockData(blockOffset) | (1 << successiveLow)

    // AC first scan: spectral-selection coefficients Ss..Se, point-transformed,
    // with end-of-band (EOB) run-length skipping (per libjpeg decode_mcu_AC_first).
    private def decodeACFirst(c: Component, blockOffset: Int): Unit =
      if (eobrun > 0) eobrun -= 1
      else {
        val se = spectralEnd
        val al = successiveLow
        var k  = spectralStart
        scala.util.boundary {
          while (k <= se) {
            val rs = decodeHuff(acTables(c.acTab))
            val r  = rs >> 4
            val s  = rs & 15
            if (s == 0) {
              if (r != 15) {
                eobrun = 1 << r
                if (r != 0) eobrun += receive(r)
                eobrun -= 1
                scala.util.boundary.break()
              } else {
                k += 16 // ZRL: skip 16 zero coefficients
              }
            } else {
              k += r
              // libjpeg always consumes the s magnitude bits (GET_BITS) before storing the
              // coefficient; keep the bit reader in sync even when k has run past Se, guarding only
              // the out-of-range array write (ISS-813).
              val coeff = extend(receive(s), s) << al
              if (k <= se) c.blockData(blockOffset + zigzag(k)) = coeff
              k += 1
            }
          }
        }
      }

    // AC refinement scan: one more low-order bit for coefficients Ss..Se, threading
    // correction bits through already-nonzero coefficients (per libjpeg
    // decode_mcu_AC_refine).
    private def decodeACSuccessive(c: Component, blockOffset: Int): Unit = {
      val se = spectralEnd
      val p1 = 1 << successiveLow
      val m1 = -(1 << successiveLow)
      var k  = spectralStart
      if (eobrun == 0) {
        scala.util.boundary {
          while (k <= se) {
            val rs = decodeHuff(acTables(c.acTab))
            var r  = rs >> 4
            var s  = rs & 15
            if (s == 0 && r != 15) {
              eobrun = 1 << r
              if (r != 0) eobrun += receive(r)
              scala.util.boundary.break()
            } else {
              if (s != 0) {
                // s must be 1: the sign bit picks the new coefficient's value.
                s = if (nextBit() != 0) p1 else m1
              }
              // Advance over the run of r zero-history coefficients, refining any
              // already-nonzero coefficient encountered along the way.
              scala.util.boundary {
                while (k <= se) {
                  val z   = zigzag(k)
                  val cur = c.blockData(blockOffset + z)
                  if (cur != 0) {
                    if (nextBit() != 0 && (cur & p1) == 0) c.blockData(blockOffset + z) = if (cur >= 0) cur + p1 else cur + m1
                    k += 1
                  } else {
                    r -= 1
                    if (r < 0) scala.util.boundary.break()
                    else k += 1
                  }
                }
              }
              if (s != 0 && k <= se) c.blockData(blockOffset + zigzag(k)) = s
              k += 1
            }
          }
        }
      }
      if (eobrun > 0) {
        // Within an EOB run: only refine the remaining already-nonzero coefficients.
        while (k <= se) {
          val z   = zigzag(k)
          val cur = c.blockData(blockOffset + z)
          if (cur != 0 && nextBit() != 0 && (cur & p1) == 0) c.blockData(blockOffset + z) = if (cur >= 0) cur + p1 else cur + m1
          k += 1
        }
        eobrun -= 1
      }
    }

    // Dequantize + IDCT every block of every component into its plane, then upsample
    // and color-convert (mirrors the tail of `assemble()`).
    private def assembleProgressive(): Gdx2dOps.DecodeResult = {
      val outBlock = new Array[Int](64)
      val coef     = new Array[Int](64)
      var ci       = 0
      while (ci < components.length) {
        val c = components(ci)
        c.planeW = c.blocksPerLineForMcu * 8
        c.planeH = c.blocksPerColumnForMcu * 8
        c.plane = new Array[Byte](c.planeW * c.planeH)
        val q        = quantTables(c.quant)
        var blockRow = 0
        while (blockRow < c.blocksPerColumnForMcu) {
          var blockCol = 0
          while (blockCol < c.blocksPerLineForMcu) {
            val blockOffset = (blockRow * c.blocksPerLineForMcu + blockCol) * 64
            // Dequantize into natural order (blockData holds natural-order
            // coefficients; the quant table is stored in zig-zag order).
            var k = 0
            while (k < 64) {
              val z = zigzag(k)
              coef(z) = c.blockData(blockOffset + z) * q(k)
              k += 1
            }
            idct8x8(coef, outBlock)
            val px0 = blockCol * 8
            val py0 = blockRow * 8
            var yy  = 0
            while (yy < 8) {
              val rowBase = (py0 + yy) * c.planeW + px0
              var xx      = 0
              while (xx < 8) {
                c.plane(rowBase + xx) = outBlock(yy * 8 + xx).toByte
                xx += 1
              }
              yy += 1
            }
            blockCol += 1
          }
          blockRow += 1
        }
        ci += 1
      }
      Gdx2dOps.DecodeResult(width, height, 4, java.nio.ByteBuffer.wrap(planesToRgba(hMax, vMax)))
    }

    private def clampByte(v: Double): Byte = {
      val i = scala.math.round(v).toInt
      (if (i < 0) 0 else if (i > 255) 255 else i).toByte
    }

    // Align to the next RSTn marker and consume it.
    private def consumeRestart(): Unit = {
      // Discard any partial bits.
      bitCnt = 0
      bitBuf = 0
      // The bit reader leaves the marker in the stream (pos rewound). Scan for it.
      // Skip until FF Dn.
      scala.util.boundary {
        while (pos + 1 < end)
          if ((data(pos) & 0xff) == 0xff) {
            val m = data(pos + 1) & 0xff
            if (m >= 0xd0 && m <= 0xd7) {
              pos += 2
              scala.util.boundary.break()
            } else if (m == 0x00) {
              pos += 2 // stuffed byte, keep scanning
            } else {
              pos += 1
            }
          } else pos += 1
      }
      marker = 0
    }
  }
}
