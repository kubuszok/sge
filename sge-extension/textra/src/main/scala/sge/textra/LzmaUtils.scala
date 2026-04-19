/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/LzmaUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError
 *   Convention: utility object instead of final class with private constructor
 *   Idiom: split packages; try-with-resources -> try/finally with AutoCloseable
 *   Note: Self-contained LZMA implementation (adapted from LZMA SDK 9.22 via
 *     LibGDX utils.compression) inlined as private nested types since SGE core
 *     does not port the compression sub-package.
 */
package sge
package textra

import java.io.{ BufferedInputStream, InputStream, OutputStream }

import sge.files.FileHandle
import sge.utils.SgeError

/** Simple static utilities to make using LZMA compression easier. This simply handles opening and closing compressed and decompressed files without needing to catch IOException. If something goes
  * wrong, this will throw an unchecked exception, an SgeError, with more information than the original IOException would have.
  */
object LzmaUtils {

  /** Given an {@code input} FileHandle compressed with LZMA, and an {@code output} FileHandle that will be overwritten, decompresses input into output.
    * @param input
    *   the LZMA-compressed FileHandle to read; typically the file extension ends in ".lzma"
    * @param output
    *   the FileHandle to write to; will be overwritten
    */
  def decompress(input: FileHandle, output: FileHandle): Unit = {
    var is: BufferedInputStream = null.asInstanceOf[BufferedInputStream] // @nowarn — Java stream interop
    var os: OutputStream        = null.asInstanceOf[OutputStream] // @nowarn — Java stream interop
    try {
      is = input.read(4096)
      os = output.write(false)
      Lzma.decompress(is, os)
    } catch {
      case e: Exception =>
        System.out.println(
          "Decompression failed! " + input + " could not be decompressed to " + output +
            " because of the IOException: " + e.getMessage
        )
        throw SgeError.InvalidInput("Decompression failed! " + input + " could not be decompressed to " + output, Some(e))
    } finally {
      if (is != null)
        try is.close()
        catch { case _: Exception => () } // @nowarn — Java stream interop
      if (os != null)
        try os.close()
        catch { case _: Exception => () } // @nowarn — Java stream interop
    }
  }

  /** Given an {@code input} FileHandle and an {@code output} FileHandle that will be overwritten, compresses input using LZMA and writes the result into output.
    * @param input
    *   the FileHandle to read; will not be modified
    * @param output
    *   the FileHandle to write LZMA-compressed output to; typically the file extension ends in ".lzma"
    */
  def compress(input: FileHandle, output: FileHandle): Unit = {
    var is: BufferedInputStream = null.asInstanceOf[BufferedInputStream] // @nowarn — Java stream interop
    var os: OutputStream        = null.asInstanceOf[OutputStream] // @nowarn — Java stream interop
    try {
      is = input.read(4096)
      os = output.write(false)
      Lzma.compress(is, os)
    } catch {
      case e: Exception =>
        System.out.println(
          "Compression failed! " + input + " could not be compressed to " + output +
            " because of the IOException: " + e.getMessage
        )
        throw SgeError.InvalidInput("Compression failed! " + input + " could not be compressed to " + output, Some(e))
    } finally {
      if (is != null)
        try is.close()
        catch { case _: Exception => () } // @nowarn — Java stream interop
      if (os != null)
        try os.close()
        catch { case _: Exception => () } // @nowarn — Java stream interop
    }
  }

  // ---------------------------------------------------------------------------
  // Self-contained LZMA implementation adapted from LZMA SDK 9.22 (public domain)
  // via LibGDX com.badlogic.gdx.utils.compression. Only the subset required for
  // Lzma.decompress and Lzma.compress is included. The implementation is private
  // to this object, keeping it isolated from the rest of the codebase.
  // ---------------------------------------------------------------------------

  /** LZMA compress/decompress facade, adapted from LibGDX Lzma.java. */
  private object Lzma {

    /** Compresses the given InputStream into the given OutputStream. */
    def compress(in: InputStream, out: OutputStream): Unit = {
      val encoder = new LzmaEncoder()
      // Use default LZMA SDK parameters
      val dictionarySize = 1 << 23
      val lc             = 3
      val lp             = 0
      val pb             = 2
      val fb             = 128
      val algorithm      = 2
      val matchFinder    = 1

      if (!encoder.setAlgorithm(algorithm)) throw new RuntimeException("Incorrect compression mode")
      if (!encoder.setDictionarySize(dictionarySize)) throw new RuntimeException("Incorrect dictionary size")
      if (!encoder.setNumFastBytes(fb)) throw new RuntimeException("Incorrect -fb value")
      if (!encoder.setMatchFinder(matchFinder)) throw new RuntimeException("Incorrect -mf value")
      if (!encoder.setLcLpPb(lc, lp, pb)) throw new RuntimeException("Incorrect -lc or -lp or -pb value")
      encoder.setEndMarkerMode(false)
      encoder.writeCoderProperties(out)
      var fileSize: Long = in.available().toLong
      if (fileSize == 0L) fileSize = -1L
      var i = 0
      while (i < 8) {
        out.write((fileSize >>> (8 * i)).toInt & 0xff)
        i += 1
      }
      encoder.code(in, out, -1L, -1L)
    }

    /** Decompresses the given InputStream into the given OutputStream. */
    def decompress(in: InputStream, out: OutputStream): Unit = {
      val propertiesSize = 5
      val properties     = new Array[Byte](propertiesSize)
      if (in.read(properties, 0, propertiesSize) != propertiesSize) {
        throw new RuntimeException("input .lzma file is too short")
      }
      val decoder = new LzmaDecoder()
      if (!decoder.setDecoderProperties(properties)) {
        throw new RuntimeException("Incorrect stream properties")
      }
      var outSize = 0L
      var i       = 0
      while (i < 8) {
        val v = in.read()
        if (v < 0) throw new RuntimeException("Can't read stream size")
        outSize |= v.toLong << (8 * i)
        i += 1
      }
      if (!decoder.code(in, out, outSize)) {
        throw new RuntimeException("Error in data stream")
      }
    }
  }

  // -- LZMA Base constants --

  private object LzmaBase {
    val kNumRepDistances = 4
    val kNumStates       = 12

    def stateInit(): Int = 0

    def stateUpdateChar(index: Int): Int =
      if (index < 4) 0
      else if (index < 10) index - 3
      else index - 6

    def stateUpdateMatch(index: Int): Int = if (index < 7) 7 else 10

    def stateUpdateRep(index: Int): Int = if (index < 7) 8 else 11

    def stateUpdateShortRep(index: Int): Int = if (index < 7) 9 else 11

    def stateIsCharState(index: Int): Boolean = index < 7

    val kNumPosSlotBits        = 6
    val kDicLogSizeMin         = 0
    val kNumLenToPosStatesBits = 2
    val kNumLenToPosStates     = 1 << kNumLenToPosStatesBits
    val kMatchMinLen           = 2

    def getLenToPosState(len: Int): Int = {
      val l = len - kMatchMinLen
      if (l < kNumLenToPosStates) l else kNumLenToPosStates - 1
    }

    val kNumAlignBits   = 4
    val kAlignTableSize = 1 << kNumAlignBits
    val kAlignMask      = kAlignTableSize - 1

    val kStartPosModelIndex = 4
    val kEndPosModelIndex   = 14
    val kNumPosModels       = kEndPosModelIndex - kStartPosModelIndex
    val kNumFullDistances   = 1 << (kEndPosModelIndex / 2)

    val kNumLitPosStatesBitsEncodingMax = 4
    val kNumLitContextBitsMax           = 8

    val kNumPosStatesBitsMax         = 4
    val kNumPosStatesMax             = 1 << kNumPosStatesBitsMax
    val kNumPosStatesBitsEncodingMax = 4
    val kNumPosStatesEncodingMax     = 1 << kNumPosStatesBitsEncodingMax

    val kNumLowLenBits    = 3
    val kNumMidLenBits    = 3
    val kNumHighLenBits   = 8
    val kNumLowLenSymbols = 1 << kNumLowLenBits
    val kNumMidLenSymbols = 1 << kNumMidLenBits
    val kNumLenSymbols    = kNumLowLenSymbols + kNumMidLenSymbols + (1 << kNumHighLenBits)
    val kMatchMaxLen      = kMatchMinLen + kNumLenSymbols - 1
  }

  // -- Range coder decoder --

  private class RangeDecoder {
    private val kTopMask              = ~((1 << 24) - 1)
    private val kNumBitModelTotalBits = 11
    private val kBitModelTotal        = 1 << kNumBitModelTotalBits
    private val kNumMoveBits          = 5

    private var range:  Int         = 0
    private var code:   Int         = 0
    private var stream: InputStream = null.asInstanceOf[InputStream] // @nowarn — Java stream interop

    def setStream(s: InputStream): Unit = stream = s

    def releaseStream(): Unit = stream = null.asInstanceOf[InputStream] // @nowarn — Java stream interop

    def init(): Unit = {
      code = 0
      range = -1
      var i = 0
      while (i < 5) {
        code = (code << 8) | stream.read()
        i += 1
      }
    }

    def decodeDirectBits(numTotalBits: Int): Int = {
      var result = 0
      var i      = numTotalBits
      while (i != 0) {
        range >>>= 1
        val t = (code - range) >>> 31
        code -= range & (t - 1)
        result = (result << 1) | (1 - t)
        if ((range & kTopMask) == 0) {
          code = (code << 8) | stream.read()
          range <<= 8
        }
        i -= 1
      }
      result
    }

    def decodeBit(probs: Array[Short], index: Int): Int = {
      val prob     = probs(index)
      val newBound = (range >>> kNumBitModelTotalBits) * prob
      if ((code ^ 0x80000000) < (newBound ^ 0x80000000)) {
        range = newBound
        probs(index) = (prob + ((kBitModelTotal - prob) >>> kNumMoveBits)).toShort
        if ((range & kTopMask) == 0) {
          code = (code << 8) | stream.read()
          range <<= 8
        }
        0
      } else {
        range -= newBound
        code -= newBound
        probs(index) = (prob - (prob >>> kNumMoveBits)).toShort
        if ((range & kTopMask) == 0) {
          code = (code << 8) | stream.read()
          range <<= 8
        }
        1
      }
    }
  }

  private object RangeDecoder {
    private val kNumBitModelTotalBits = 11
    private val kBitModelTotal        = 1 << kNumBitModelTotalBits

    def initBitModels(probs: Array[Short]): Unit = {
      var i = 0
      while (i < probs.length) {
        probs(i) = (kBitModelTotal >>> 1).toShort
        i += 1
      }
    }
  }

  // -- Bit tree decoder --

  private class BitTreeDecoder(numBitLevels: Int) {
    val models: Array[Short] = new Array[Short](1 << numBitLevels)

    def init(): Unit = RangeDecoder.initBitModels(models)

    def decode(rangeDecoder: RangeDecoder): Int = {
      var m        = 1
      var bitIndex = numBitLevels
      while (bitIndex != 0) {
        m = (m << 1) + rangeDecoder.decodeBit(models, m)
        bitIndex -= 1
      }
      m - (1 << numBitLevels)
    }

    def reverseDecode(rangeDecoder: RangeDecoder): Int = {
      var m        = 1
      var symbol   = 0
      var bitIndex = 0
      while (bitIndex < numBitLevels) {
        val bit = rangeDecoder.decodeBit(models, m)
        m <<= 1
        m += bit
        symbol |= (bit << bitIndex)
        bitIndex += 1
      }
      symbol
    }
  }

  private object BitTreeDecoder {
    def reverseDecode(models: Array[Short], startIndex: Int, rangeDecoder: RangeDecoder, numBitLevels: Int): Int = {
      var m        = 1
      var symbol   = 0
      var bitIndex = 0
      while (bitIndex < numBitLevels) {
        val bit = rangeDecoder.decodeBit(models, startIndex + m)
        m <<= 1
        m += bit
        symbol |= (bit << bitIndex)
        bitIndex += 1
      }
      symbol
    }
  }

  // -- LZ OutWindow --

  private class OutWindow {
    private var buffer:     Array[Byte]  = null.asInstanceOf[Array[Byte]] // @nowarn — Java interop, allocated in create()
    private var pos:        Int          = 0
    private var windowSize: Int          = 0
    private var streamPos:  Int          = 0
    private var stream:     OutputStream = null.asInstanceOf[OutputStream] // @nowarn — Java stream interop

    def create(ws: Int): Unit = {
      if (buffer == null || windowSize != ws) buffer = new Array[Byte](ws) // @nowarn — null check for Java interop
      windowSize = ws
      pos = 0
      streamPos = 0
    }

    def setStream(s: OutputStream): Unit = {
      flush()
      stream = s
    }

    def releaseStream(): Unit = {
      flush()
      stream = null.asInstanceOf[OutputStream] // @nowarn — Java stream interop
    }

    def init(solid: Boolean): Unit =
      if (!solid) {
        streamPos = 0
        pos = 0
      }

    def flush(): Unit = {
      val size = pos - streamPos
      if (size == 0) () // @nowarn — early exit
      else {
        stream.write(buffer, streamPos, size)
        if (pos >= windowSize) pos = 0
        streamPos = pos
      }
    }

    def copyBlock(distance: Int, len: Int): Unit = {
      var p = pos - distance - 1
      if (p < 0) p += windowSize
      var remaining = len
      while (remaining != 0) {
        if (p >= windowSize) p = 0
        buffer(pos) = buffer(p)
        pos += 1
        p += 1
        if (pos >= windowSize) flush()
        remaining -= 1
      }
    }

    def putByte(b: Byte): Unit = {
      buffer(pos) = b
      pos += 1
      if (pos >= windowSize) flush()
    }

    def getByte(distance: Int): Byte = {
      var p = pos - distance - 1
      if (p < 0) p += windowSize
      buffer(p)
    }
  }

  // -- LZMA Decoder --

  private class LzmaDecoder {
    import LzmaBase.*

    private class LenDecoder {
      val choice:       Array[Short]          = new Array[Short](2)
      val lowCoder:     Array[BitTreeDecoder] = new Array[BitTreeDecoder](kNumPosStatesMax)
      val midCoder:     Array[BitTreeDecoder] = new Array[BitTreeDecoder](kNumPosStatesMax)
      val highCoder:    BitTreeDecoder        = new BitTreeDecoder(kNumHighLenBits)
      var numPosStates: Int                   = 0

      def create(numPS: Int): Unit =
        while (numPosStates < numPS) {
          lowCoder(numPosStates) = new BitTreeDecoder(kNumLowLenBits)
          midCoder(numPosStates) = new BitTreeDecoder(kNumMidLenBits)
          numPosStates += 1
        }

      def init(): Unit = {
        RangeDecoder.initBitModels(choice)
        var posState = 0
        while (posState < numPosStates) {
          lowCoder(posState).init()
          midCoder(posState).init()
          posState += 1
        }
        highCoder.init()
      }

      def decode(rangeDecoder: RangeDecoder, posState: Int): Int =
        if (rangeDecoder.decodeBit(choice, 0) == 0) {
          lowCoder(posState).decode(rangeDecoder)
        } else {
          var symbol = kNumLowLenSymbols
          if (rangeDecoder.decodeBit(choice, 1) == 0) {
            symbol += midCoder(posState).decode(rangeDecoder)
          } else {
            symbol += kNumMidLenSymbols + highCoder.decode(rangeDecoder)
          }
          symbol
        }
    }

    private class LiteralDecoder {
      class Decoder2 {
        val decoders: Array[Short] = new Array[Short](0x300)

        def init(): Unit = RangeDecoder.initBitModels(decoders)

        def decodeNormal(rangeDecoder: RangeDecoder): Byte = {
          var symbol = 1
          while (symbol < 0x100)
            symbol = (symbol << 1) | rangeDecoder.decodeBit(decoders, symbol)
          symbol.toByte
        }

        def decodeWithMatchByte(rangeDecoder: RangeDecoder, matchByteIn: Byte): Byte = {
          var symbol = 1
          var mb     = matchByteIn
          var done   = false
          while (symbol < 0x100 && !done) {
            val matchBit = (mb >> 7) & 1
            mb = (mb << 1).toByte
            val bit = rangeDecoder.decodeBit(decoders, ((1 + matchBit) << 8) + symbol)
            symbol = (symbol << 1) | bit
            if (matchBit != bit) {
              while (symbol < 0x100)
                symbol = (symbol << 1) | rangeDecoder.decodeBit(decoders, symbol)
              done = true
            }
          }
          symbol.toByte
        }
      }

      private var coders:      Array[Decoder2] = null.asInstanceOf[Array[Decoder2]] // @nowarn — Java interop
      private var numPrevBits: Int             = 0
      private var numPosBits:  Int             = 0
      private var posMask:     Int             = 0

      def create(npb: Int, nprevb: Int): Unit =
        if (coders != null && numPrevBits == nprevb && numPosBits == npb) () // @nowarn — already created
        else {
          numPosBits = npb
          posMask = (1 << npb) - 1
          numPrevBits = nprevb
          val numStates = 1 << (numPrevBits + numPosBits)
          coders = new Array[Decoder2](numStates)
          var i = 0
          while (i < numStates) {
            coders(i) = new Decoder2()
            i += 1
          }
        }

      def init(): Unit = {
        val numStates = 1 << (numPrevBits + numPosBits)
        var i         = 0
        while (i < numStates) {
          coders(i).init()
          i += 1
        }
      }

      def getDecoder(pos: Int, prevByte: Byte): Decoder2 =
        coders(((pos & posMask) << numPrevBits) + ((prevByte & 0xff) >>> (8 - numPrevBits)))
    }

    private val outWindow    = new OutWindow()
    private val rangeDecoder = new RangeDecoder()

    private val isMatchDecoders    = new Array[Short](kNumStates << kNumPosStatesBitsMax)
    private val isRepDecoders      = new Array[Short](kNumStates)
    private val isRepG0Decoders    = new Array[Short](kNumStates)
    private val isRepG1Decoders    = new Array[Short](kNumStates)
    private val isRepG2Decoders    = new Array[Short](kNumStates)
    private val isRep0LongDecoders = new Array[Short](kNumStates << kNumPosStatesBitsMax)

    private val posSlotDecoder: Array[BitTreeDecoder] = {
      val arr = new Array[BitTreeDecoder](kNumLenToPosStates)
      var i   = 0
      while (i < kNumLenToPosStates) {
        arr(i) = new BitTreeDecoder(kNumPosSlotBits)
        i += 1
      }
      arr
    }
    private val posDecoders     = new Array[Short](kNumFullDistances - kEndPosModelIndex)
    private val posAlignDecoder = new BitTreeDecoder(kNumAlignBits)

    private val lenDecoder     = new LenDecoder()
    private val repLenDecoder  = new LenDecoder()
    private val literalDecoder = new LiteralDecoder()

    private var dictionarySize      = -1
    private var dictionarySizeCheck = -1
    private var posStateMask: Int = 0

    private def setDictionarySize(ds: Int): Boolean =
      if (ds < 0) false
      else {
        if (dictionarySize != ds) {
          dictionarySize = ds
          dictionarySizeCheck = Math.max(dictionarySize, 1)
          outWindow.create(Math.max(dictionarySizeCheck, 1 << 12))
        }
        true
      }

    private def setLcLpPb(lc: Int, lp: Int, pb: Int): Boolean =
      if (lc > kNumLitContextBitsMax || lp > 4 || pb > kNumPosStatesBitsMax) false
      else {
        literalDecoder.create(lp, lc)
        val numPosStates = 1 << pb
        lenDecoder.create(numPosStates)
        repLenDecoder.create(numPosStates)
        posStateMask = numPosStates - 1
        true
      }

    private def init(): Unit = {
      outWindow.init(false)
      RangeDecoder.initBitModels(isMatchDecoders)
      RangeDecoder.initBitModels(isRep0LongDecoders)
      RangeDecoder.initBitModels(isRepDecoders)
      RangeDecoder.initBitModels(isRepG0Decoders)
      RangeDecoder.initBitModels(isRepG1Decoders)
      RangeDecoder.initBitModels(isRepG2Decoders)
      RangeDecoder.initBitModels(posDecoders)
      literalDecoder.init()
      var i = 0
      while (i < kNumLenToPosStates) {
        posSlotDecoder(i).init()
        i += 1
      }
      lenDecoder.init()
      repLenDecoder.init()
      posAlignDecoder.init()
      rangeDecoder.init()
    }

    def code(inStream: InputStream, outStream: OutputStream, outSize: Long): Boolean = {
      rangeDecoder.setStream(inStream)
      outWindow.setStream(outStream)
      init()

      var state    = stateInit()
      var rep0     = 0
      var rep1     = 0
      var rep2     = 0
      var rep3     = 0
      var nowPos64 = 0L
      var prevByte: Byte = 0

      var earlyExit = false
      var result    = true

      while (!earlyExit && (outSize < 0 || nowPos64 < outSize)) {
        val posState = nowPos64.toInt & posStateMask
        if (rangeDecoder.decodeBit(isMatchDecoders, (state << kNumPosStatesBitsMax) + posState) == 0) {
          val decoder2 = literalDecoder.getDecoder(nowPos64.toInt, prevByte)
          if (!stateIsCharState(state)) {
            prevByte = decoder2.decodeWithMatchByte(rangeDecoder, outWindow.getByte(rep0))
          } else {
            prevByte = decoder2.decodeNormal(rangeDecoder)
          }
          outWindow.putByte(prevByte)
          state = stateUpdateChar(state)
          nowPos64 += 1
        } else {
          var len: Int = 0
          if (rangeDecoder.decodeBit(isRepDecoders, state) == 1) {
            len = 0
            if (rangeDecoder.decodeBit(isRepG0Decoders, state) == 0) {
              if (rangeDecoder.decodeBit(isRep0LongDecoders, (state << kNumPosStatesBitsMax) + posState) == 0) {
                state = stateUpdateShortRep(state)
                len = 1
              }
            } else {
              var distance: Int = 0
              if (rangeDecoder.decodeBit(isRepG1Decoders, state) == 0) {
                distance = rep1
              } else {
                if (rangeDecoder.decodeBit(isRepG2Decoders, state) == 0) {
                  distance = rep2
                } else {
                  distance = rep3
                  rep3 = rep2
                }
                rep2 = rep1
              }
              rep1 = rep0
              rep0 = distance
            }
            if (len == 0) {
              len = repLenDecoder.decode(rangeDecoder, posState) + kMatchMinLen
              state = stateUpdateRep(state)
            }
          } else {
            rep3 = rep2
            rep2 = rep1
            rep1 = rep0
            len = kMatchMinLen + lenDecoder.decode(rangeDecoder, posState)
            state = stateUpdateMatch(state)
            val posSlot = posSlotDecoder(getLenToPosState(len)).decode(rangeDecoder)
            if (posSlot >= kStartPosModelIndex) {
              val numDirectBits = (posSlot >> 1) - 1
              rep0 = (2 | (posSlot & 1)) << numDirectBits
              if (posSlot < kEndPosModelIndex) {
                rep0 += BitTreeDecoder.reverseDecode(posDecoders, rep0 - posSlot - 1, rangeDecoder, numDirectBits)
              } else {
                rep0 += rangeDecoder.decodeDirectBits(numDirectBits - kNumAlignBits) << kNumAlignBits
                rep0 += posAlignDecoder.reverseDecode(rangeDecoder)
                if (rep0 < 0) {
                  if (rep0 == -1) {
                    earlyExit = true
                  } else {
                    result = false
                    earlyExit = true
                  }
                }
              }
            } else {
              rep0 = posSlot
            }
          }
          if (!earlyExit) {
            if (rep0 >= nowPos64 || rep0 >= dictionarySizeCheck) {
              result = false
              earlyExit = true
            } else {
              outWindow.copyBlock(rep0, len)
              nowPos64 += len
              prevByte = outWindow.getByte(0)
            }
          }
        }
      }
      outWindow.flush()
      outWindow.releaseStream()
      rangeDecoder.releaseStream()
      result
    }

    def setDecoderProperties(properties: Array[Byte]): Boolean =
      if (properties.length < 5) false
      else {
        val v         = properties(0) & 0xff
        val lc        = v % 9
        val remainder = v / 9
        val lp        = remainder % 5
        val pb        = remainder / 5
        var ds        = 0
        var i         = 0
        while (i < 4) {
          ds += (properties(1 + i) & 0xff) << (i * 8)
          i += 1
        }
        if (!setLcLpPb(lc, lp, pb)) false
        else setDictionarySize(ds)
      }
  }

  // -- Range coder encoder --

  private class RangeEncoder {
    private val kTopMask              = ~((1 << 24) - 1)
    private val kNumBitModelTotalBits = 11
    private val kBitModelTotal        = 1 << kNumBitModelTotalBits
    private val kNumMoveBits          = 5

    private var stream:    OutputStream = null.asInstanceOf[OutputStream] // @nowarn — Java stream interop
    private var low:       Long         = 0L
    private var range:     Int          = 0
    private var cacheSize: Int          = 0
    private var cache:     Int          = 0
    private var position:  Long         = 0L

    def setStream(s: OutputStream): Unit = stream = s
    def releaseStream():            Unit = stream = null.asInstanceOf[OutputStream] // @nowarn — Java stream interop

    def init(): Unit = {
      position = 0
      low = 0
      range = -1
      cacheSize = 1
      cache = 0
    }

    def flushData(): Unit = {
      var i = 0
      while (i < 5) {
        shiftLow()
        i += 1
      }
    }

    def flushStream(): Unit = stream.flush()

    def shiftLow(): Unit = {
      val lowHi = (low >>> 32).toInt
      if (lowHi != 0 || low < 0xff000000L) {
        position += cacheSize
        var temp = cache
        while (cacheSize > 0) {
          stream.write(temp + lowHi)
          temp = 0xff
          cacheSize -= 1
        }
        cache = (low.toInt) >>> 24
      }
      cacheSize += 1
      low = (low & 0xffffffL) << 8
    }

    def encodeDirectBits(v: Int, numTotalBits: Int): Unit = {
      var i = numTotalBits - 1
      while (i >= 0) {
        range >>>= 1
        if (((v >>> i) & 1) == 1) low += range.toLong & 0xffffffffL
        if ((range & kTopMask) == 0) {
          range <<= 8
          shiftLow()
        }
        i -= 1
      }
    }

    def getProcessedSizeAdd: Long = cacheSize + position + 4

    def encode(probs: Array[Short], index: Int, symbol: Int): Unit = {
      val prob     = probs(index)
      val newBound = (range >>> kNumBitModelTotalBits) * prob
      if (symbol == 0) {
        range = newBound
        probs(index) = (prob + ((kBitModelTotal - prob) >>> kNumMoveBits)).toShort
      } else {
        low += newBound.toLong & 0xffffffffL
        range -= newBound
        probs(index) = (prob - (prob >>> kNumMoveBits)).toShort
      }
      if ((range & kTopMask) == 0) {
        range <<= 8
        shiftLow()
      }
    }
  }

  private object RangeEncoder {
    private val kNumBitModelTotalBits = 11
    private val kBitModelTotal        = 1 << kNumBitModelTotalBits
    private val kNumMoveReducingBits  = 2
    val kNumBitPriceShiftBits         = 6

    def initBitModels(probs: Array[Short]): Unit = {
      var i = 0
      while (i < probs.length) {
        probs(i) = (kBitModelTotal >>> 1).toShort
        i += 1
      }
    }

    private val probPrices: Array[Int] = {
      val arr      = new Array[Int](kBitModelTotal >>> kNumMoveReducingBits)
      val kNumBits = kNumBitModelTotalBits - kNumMoveReducingBits
      var i        = kNumBits - 1
      while (i >= 0) {
        val start = 1 << (kNumBits - i - 1)
        val end   = 1 << (kNumBits - i)
        var j     = start
        while (j < end) {
          arr(j) = (i << kNumBitPriceShiftBits) + (((end - j) << kNumBitPriceShiftBits) >>> (kNumBits - i - 1))
          j += 1
        }
        i -= 1
      }
      arr
    }

    def getPrice(prob: Int, symbol: Int): Int =
      probPrices((((prob - symbol) ^ (-symbol)) & (kBitModelTotal - 1)) >>> kNumMoveReducingBits)

    def getPrice0(prob: Int): Int = probPrices(prob >>> kNumMoveReducingBits)

    def getPrice1(prob: Int): Int = probPrices((kBitModelTotal - prob) >>> kNumMoveReducingBits)
  }

  // -- Bit tree encoder --

  private class BitTreeEncoder(numBitLevels: Int) {
    val models: Array[Short] = new Array[Short](1 << numBitLevels)

    def init(): Unit = RangeDecoder.initBitModels(models)

    def encode(rangeEncoder: RangeEncoder, symbol: Int): Unit = {
      var m        = 1
      var bitIndex = numBitLevels
      while (bitIndex != 0) {
        bitIndex -= 1
        val bit = (symbol >>> bitIndex) & 1
        rangeEncoder.encode(models, m, bit)
        m = (m << 1) | bit
      }
    }

    def reverseEncode(rangeEncoder: RangeEncoder, symbol: Int): Unit = {
      var m = 1
      var s = symbol
      var i = 0
      while (i < numBitLevels) {
        val bit = s & 1
        rangeEncoder.encode(models, m, bit)
        m = (m << 1) | bit
        s >>= 1
        i += 1
      }
    }

    def getPrice(symbol: Int): Int = {
      var price    = 0
      var m        = 1
      var bitIndex = numBitLevels
      while (bitIndex != 0) {
        bitIndex -= 1
        val bit = (symbol >>> bitIndex) & 1
        price += RangeEncoder.getPrice(models(m), bit)
        m = (m << 1) + bit
      }
      price
    }

    def reverseGetPrice(symbol: Int): Int = {
      var price = 0
      var m     = 1
      var s     = symbol
      var i     = numBitLevels
      while (i != 0) {
        val bit = s & 1
        s >>>= 1
        price += RangeEncoder.getPrice(models(m), bit)
        m = (m << 1) | bit
        i -= 1
      }
      price
    }
  }

  private object BitTreeEncoder {
    def reverseGetPrice(models: Array[Short], startIndex: Int, numBitLevels: Int, symbol: Int): Int = {
      var price = 0
      var m     = 1
      var s     = symbol
      var i     = numBitLevels
      while (i != 0) {
        val bit = s & 1
        s >>>= 1
        price += RangeEncoder.getPrice(models(startIndex + m), bit)
        m = (m << 1) | bit
        i -= 1
      }
      price
    }

    def reverseEncode(models: Array[Short], startIndex: Int, rangeEncoder: RangeEncoder, numBitLevels: Int, symbol: Int): Unit = {
      var m = 1
      var s = symbol
      var i = 0
      while (i < numBitLevels) {
        val bit = s & 1
        rangeEncoder.encode(models, startIndex + m, bit)
        m = (m << 1) | bit
        s >>= 1
        i += 1
      }
    }
  }

  // -- LZ InWindow --

  private class InWindow {
    var bufferBase:                        Array[Byte] = null.asInstanceOf[Array[Byte]] // @nowarn — Java interop, allocated in create()
    private var stream:                    InputStream = null.asInstanceOf[InputStream] // @nowarn — Java stream interop
    private var posLimit:                  Int         = 0
    private var streamEndWasReached:       Boolean     = false
    private var pointerToLastSafePosition: Int         = 0
    var bufferOffset:                      Int         = 0
    var blockSize:                         Int         = 0
    var pos:                               Int         = 0
    private var keepSizeBefore:            Int         = 0
    private var keepSizeAfter:             Int         = 0
    var streamPos:                         Int         = 0

    def moveBlock(): Unit = {
      var offset = bufferOffset + pos - keepSizeBefore
      if (offset > 0) offset -= 1
      val numBytes = bufferOffset + streamPos - offset
      var i        = 0
      while (i < numBytes) {
        bufferBase(i) = bufferBase(offset + i)
        i += 1
      }
      bufferOffset -= offset
    }

    def readBlock(): Unit =
      if (!streamEndWasReached) {
        var cont = true
        while (cont) {
          val size = (0 - bufferOffset) + blockSize - streamPos
          if (size == 0) { cont = false }
          else {
            val numReadBytes = stream.read(bufferBase, bufferOffset + streamPos, size)
            if (numReadBytes == -1) {
              posLimit = streamPos
              val pointerToPosition = bufferOffset + posLimit
              if (pointerToPosition > pointerToLastSafePosition) posLimit = pointerToLastSafePosition - bufferOffset
              streamEndWasReached = true
              cont = false
            } else {
              streamPos += numReadBytes
              if (streamPos >= pos + keepSizeAfter) posLimit = streamPos - keepSizeAfter
            }
          }
        }
      }

    def create(ksb: Int, ksa: Int, keepSizeReserv: Int): Unit = {
      keepSizeBefore = ksb
      keepSizeAfter = ksa
      val bs = ksb + ksa + keepSizeReserv
      if (bufferBase == null || blockSize != bs) { // @nowarn — null check for Java interop
        bufferBase = new Array[Byte](bs)
        blockSize = bs
      }
      pointerToLastSafePosition = blockSize - keepSizeAfter
    }

    def setStream(s: InputStream): Unit = stream = s

    def releaseStream(): Unit = stream = null.asInstanceOf[InputStream] // @nowarn — Java stream interop

    def init(): Unit = {
      bufferOffset = 0
      pos = 0
      streamPos = 0
      streamEndWasReached = false
      readBlock()
    }

    def movePos(): Unit = {
      pos += 1
      if (pos > posLimit) {
        val pointerToPosition = bufferOffset + pos
        if (pointerToPosition > pointerToLastSafePosition) moveBlock()
        readBlock()
      }
    }

    def getIndexByte(index: Int): Byte = bufferBase(bufferOffset + pos + index)

    def getMatchLen(index: Int, distance: Int, limit: Int): Int = {
      var lim = limit
      if (streamEndWasReached) {
        if ((pos + index) + lim > streamPos) lim = streamPos - (pos + index)
      }
      val dist = distance + 1
      val pby  = bufferOffset + pos + index
      var i    = 0
      while (i < lim && bufferBase(pby + i) == bufferBase(pby + i - dist))
        i += 1
      i
    }

    def getNumAvailableBytes: Int = streamPos - pos

    def reduceOffsets(subValue: Int): Unit = {
      bufferOffset += subValue
      posLimit -= subValue
      pos -= subValue
      streamPos -= subValue
    }
  }

  // -- LZ BinTree --

  private class BinTree extends InWindow {
    private var cyclicBufferPos:  Int = 0
    private var cyclicBufferSize: Int = 0
    private var matchMaxLen:      Int = 0

    private var son:  Array[Int] = null.asInstanceOf[Array[Int]] // @nowarn — Java interop, allocated in create()
    private var hash: Array[Int] = null.asInstanceOf[Array[Int]] // @nowarn — Java interop, allocated in create()

    private var cutValue:    Int = 0xff
    private var hashMask:    Int = 0
    private var hashSizeSum: Int = 0

    private var hashArray: Boolean = true

    private val kHash2Size          = 1 << 10
    private val kHash3Size          = 1 << 16
    private val kBT2HashSize        = 1 << 16
    private val kStartMaxLen        = 1
    private val kHash3Offset        = kHash2Size
    private val kEmptyHashValue     = 0
    private val kMaxValForNormalize = (1 << 30) - 1

    private var kNumHashDirectBytes = 0
    private var kMinMatchCheck      = 4
    private var kFixHashSize        = kHash2Size + kHash3Size

    def setType(numHashBytes: Int): Unit = {
      hashArray = numHashBytes > 2
      if (hashArray) {
        kNumHashDirectBytes = 0
        kMinMatchCheck = 4
        kFixHashSize = kHash2Size + kHash3Size
      } else {
        kNumHashDirectBytes = 2
        kMinMatchCheck = 2 + 1
        kFixHashSize = 0
      }
    }

    override def init(): Unit = {
      super.init()
      var i = 0
      while (i < hashSizeSum) {
        hash(i) = kEmptyHashValue
        i += 1
      }
      cyclicBufferPos = 0
      reduceOffsets(-1)
    }

    override def movePos(): Unit = {
      cyclicBufferPos += 1
      if (cyclicBufferPos >= cyclicBufferSize) cyclicBufferPos = 0
      super.movePos()
      if (pos == kMaxValForNormalize) normalize()
    }

    def create(historySize: Int, keepAddBufferBefore: Int, mml: Int, keepAddBufferAfter: Int): Boolean =
      if (historySize > kMaxValForNormalize - 256) false
      else {
        cutValue = 16 + (mml >> 1)
        val windowReservSize = (historySize + keepAddBufferBefore + mml + keepAddBufferAfter) / 2 + 256
        super.create(historySize + keepAddBufferBefore, mml + keepAddBufferAfter, windowReservSize)
        matchMaxLen = mml
        val cbs = historySize + 1
        if (cyclicBufferSize != cbs) {
          cyclicBufferSize = cbs
          son = new Array[Int](cyclicBufferSize * 2)
        }
        var hs = kBT2HashSize
        if (hashArray) {
          hs = historySize - 1
          hs |= (hs >> 1)
          hs |= (hs >> 2)
          hs |= (hs >> 4)
          hs |= (hs >> 8)
          hs >>= 1
          hs |= 0xffff
          if (hs > (1 << 24)) hs >>= 1
          hashMask = hs
          hs += 1
          hs += kFixHashSize
        }
        if (hs != hashSizeSum) {
          hashSizeSum = hs
          hash = new Array[Int](hashSizeSum)
        }
        true
      }

    def getMatches(distances: Array[Int]): Int = {
      var lenLimit: Int = 0
      if (pos + matchMaxLen <= streamPos) {
        lenLimit = matchMaxLen
      } else {
        lenLimit = streamPos - pos
        if (lenLimit < kMinMatchCheck) {
          movePos()
          return 0 // @nowarn — LZMA SDK algorithm, early exit required
        }
      }

      var offset      = 0
      val matchMinPos = if (pos > cyclicBufferSize) pos - cyclicBufferSize else 0
      val cur         = bufferOffset + pos
      var maxLen      = kStartMaxLen
      var hashValue   = 0
      var hash2Value  = 0
      var hash3Value  = 0

      if (hashArray) {
        var temp = CrcTable(bufferBase(cur) & 0xff) ^ (bufferBase(cur + 1) & 0xff)
        hash2Value = temp & (kHash2Size - 1)
        temp ^= (bufferBase(cur + 2) & 0xff) << 8
        hash3Value = temp & (kHash3Size - 1)
        hashValue = (temp ^ (CrcTable(bufferBase(cur + 3) & 0xff) << 5)) & hashMask
      } else {
        hashValue = (bufferBase(cur) & 0xff) ^ ((bufferBase(cur + 1) & 0xff) << 8)
      }

      var curMatch = hash(kFixHashSize + hashValue)
      if (hashArray) {
        var curMatch2 = hash(hash2Value)
        val curMatch3 = hash(kHash3Offset + hash3Value)
        hash(hash2Value) = pos
        hash(kHash3Offset + hash3Value) = pos
        if (curMatch2 > matchMinPos) {
          if (bufferBase(bufferOffset + curMatch2) == bufferBase(cur)) {
            maxLen = 2
            distances(offset) = maxLen
            offset += 1
            distances(offset) = pos - curMatch2 - 1
            offset += 1
          }
        }
        if (curMatch3 > matchMinPos) {
          if (bufferBase(bufferOffset + curMatch3) == bufferBase(cur)) {
            if (curMatch3 == curMatch2) offset -= 2
            maxLen = 3
            distances(offset) = maxLen
            offset += 1
            distances(offset) = pos - curMatch3 - 1
            offset += 1
            curMatch2 = curMatch3
          }
        }
        if (offset != 0 && curMatch2 == curMatch) {
          offset -= 2
          maxLen = kStartMaxLen
        }
      }

      hash(kFixHashSize + hashValue) = pos

      var ptr0 = (cyclicBufferPos << 1) + 1
      var ptr1 = cyclicBufferPos << 1

      var len0 = kNumHashDirectBytes
      var len1 = kNumHashDirectBytes

      if (kNumHashDirectBytes != 0) {
        if (curMatch > matchMinPos) {
          if (bufferBase(bufferOffset + curMatch + kNumHashDirectBytes) != bufferBase(cur + kNumHashDirectBytes)) {
            maxLen = kNumHashDirectBytes
            distances(offset) = maxLen
            offset += 1
            distances(offset) = pos - curMatch - 1
            offset += 1
          }
        }
      }

      var count = cutValue
      var cont  = true
      while (cont)
        if (curMatch <= matchMinPos || count == 0) {
          son(ptr0) = kEmptyHashValue
          son(ptr1) = kEmptyHashValue
          cont = false
        } else {
          count -= 1
          val delta     = pos - curMatch
          val cyclicPos = (if (delta <= cyclicBufferPos) cyclicBufferPos - delta else cyclicBufferPos - delta + cyclicBufferSize) << 1
          val pby1      = bufferOffset + curMatch
          var len       = Math.min(len0, len1)
          if (bufferBase(pby1 + len) == bufferBase(cur + len)) {
            len += 1
            while (len != lenLimit && bufferBase(pby1 + len) == bufferBase(cur + len)) len += 1
            if (maxLen < len) {
              maxLen = len
              distances(offset) = maxLen
              offset += 1
              distances(offset) = delta - 1
              offset += 1
              if (len == lenLimit) {
                son(ptr1) = son(cyclicPos)
                son(ptr0) = son(cyclicPos + 1)
                cont = false
              }
            }
          }
          if (cont) {
            if ((bufferBase(pby1 + len) & 0xff) < (bufferBase(cur + len) & 0xff)) {
              son(ptr1) = curMatch
              ptr1 = cyclicPos + 1
              curMatch = son(ptr1)
              len1 = len
            } else {
              son(ptr0) = curMatch
              ptr0 = cyclicPos
              curMatch = son(ptr0)
              len0 = len
            }
          }
        }
      movePos()
      offset
    }

    def skip(num: Int): Unit = {
      var remaining = num
      while (remaining != 0) {
        var lenLimit: Int = 0
        if (pos + matchMaxLen <= streamPos) {
          lenLimit = matchMaxLen
        } else {
          lenLimit = streamPos - pos
          if (lenLimit < kMinMatchCheck) {
            movePos()
            remaining -= 1
          }
        }
        if (remaining != 0 && lenLimit >= kMinMatchCheck) {
          val matchMinPos = if (pos > cyclicBufferSize) pos - cyclicBufferSize else 0
          val cur         = bufferOffset + pos
          var hashValue: Int = 0

          if (hashArray) {
            var temp       = CrcTable(bufferBase(cur) & 0xff) ^ (bufferBase(cur + 1) & 0xff)
            val hash2Value = temp & (kHash2Size - 1)
            hash(hash2Value) = pos
            temp ^= (bufferBase(cur + 2) & 0xff) << 8
            val hash3Value = temp & (kHash3Size - 1)
            hash(kHash3Offset + hash3Value) = pos
            hashValue = (temp ^ (CrcTable(bufferBase(cur + 3) & 0xff) << 5)) & hashMask
          } else {
            hashValue = (bufferBase(cur) & 0xff) ^ ((bufferBase(cur + 1) & 0xff) << 8)
          }

          var curMatch = hash(kFixHashSize + hashValue)
          hash(kFixHashSize + hashValue) = pos

          var ptr0 = (cyclicBufferPos << 1) + 1
          var ptr1 = cyclicBufferPos << 1

          var len0 = kNumHashDirectBytes
          var len1 = kNumHashDirectBytes

          var count = cutValue
          var cont  = true
          while (cont)
            if (curMatch <= matchMinPos || count == 0) {
              son(ptr0) = kEmptyHashValue
              son(ptr1) = kEmptyHashValue
              cont = false
            } else {
              count -= 1
              val delta     = pos - curMatch
              val cyclicPos = (if (delta <= cyclicBufferPos) cyclicBufferPos - delta else cyclicBufferPos - delta + cyclicBufferSize) << 1
              val pby1      = bufferOffset + curMatch
              var len       = Math.min(len0, len1)
              if (bufferBase(pby1 + len) == bufferBase(cur + len)) {
                len += 1
                while (len != lenLimit && bufferBase(pby1 + len) == bufferBase(cur + len)) len += 1
                if (len == lenLimit) {
                  son(ptr1) = son(cyclicPos)
                  son(ptr0) = son(cyclicPos + 1)
                  cont = false
                }
              }
              if (cont) {
                if ((bufferBase(pby1 + len) & 0xff) < (bufferBase(cur + len) & 0xff)) {
                  son(ptr1) = curMatch
                  ptr1 = cyclicPos + 1
                  curMatch = son(ptr1)
                  len1 = len
                } else {
                  son(ptr0) = curMatch
                  ptr0 = cyclicPos
                  curMatch = son(ptr0)
                  len0 = len
                }
              }
            }
          movePos()
          remaining -= 1
        }
      }
    }

    private def normalizeLinks(items: Array[Int], numItems: Int, subValue: Int): Unit = {
      var i = 0
      while (i < numItems) {
        var value = items(i)
        if (value <= subValue) value = kEmptyHashValue
        else value -= subValue
        items(i) = value
        i += 1
      }
    }

    private def normalize(): Unit = {
      val subValue = pos - cyclicBufferSize
      normalizeLinks(son, cyclicBufferSize * 2, subValue)
      normalizeLinks(hash, hashSizeSum, subValue)
      reduceOffsets(subValue)
    }
  }

  private val CrcTable: Array[Int] = {
    val table = new Array[Int](256)
    var i     = 0
    while (i < 256) {
      var r = i
      var j = 0
      while (j < 8) {
        if ((r & 1) != 0) r = (r >>> 1) ^ 0xedb88320
        else r >>>= 1
        j += 1
      }
      table(i) = r
      i += 1
    }
    table
  }

  // -- LZMA Encoder --

  private class LzmaEncoder {
    import LzmaBase.*

    private val kIfinityPrice             = 0xfffffff
    private val kDefaultDictionaryLogSize = 22
    private val kNumFastBytesDefault      = 0x20
    private val kNumOpts                  = 1 << 12

    private val gFastPos: Array[Byte] = {
      val arr        = new Array[Byte](1 << 11)
      val kFastSlots = 22
      var c          = 2
      arr(0) = 0
      arr(1) = 1
      var slotFast = 2
      while (slotFast < kFastSlots) {
        val k = 1 << ((slotFast >> 1) - 1)
        var j = 0
        while (j < k) {
          arr(c) = slotFast.toByte
          c += 1
          j += 1
        }
        slotFast += 1
      }
      arr
    }

    private def getPosSlot(pos: Int): Int =
      if (pos < (1 << 11)) gFastPos(pos) & 0xff
      else if (pos < (1 << 21)) (gFastPos(pos >> 10) & 0xff) + 20
      else (gFastPos(pos >> 20) & 0xff) + 40

    private def getPosSlot2(pos: Int): Int =
      if (pos < (1 << 17)) (gFastPos(pos >> 6) & 0xff) + 12
      else if (pos < (1 << 27)) (gFastPos(pos >> 16) & 0xff) + 32
      else (gFastPos(pos >> 26) & 0xff) + 52

    private var state = stateInit()
    private var previousByte: Byte = 0
    private val repDistances = new Array[Int](kNumRepDistances)

    private def baseInit(): Unit = {
      state = stateInit()
      previousByte = 0
      var i = 0
      while (i < kNumRepDistances) {
        repDistances(i) = 0
        i += 1
      }
    }

    // -- Literal encoder inner class --
    private class LiteralEncoder {
      class Encoder2 {
        val encoders: Array[Short] = new Array[Short](0x300)

        def init(): Unit = RangeEncoder.initBitModels(encoders)

        def encode(rangeEnc: RangeEncoder, symbol: Byte): Unit = {
          var context = 1
          var i       = 7
          while (i >= 0) {
            val bit = (symbol >> i) & 1
            rangeEnc.encode(encoders, context, bit)
            context = (context << 1) | bit
            i -= 1
          }
        }

        def encodeMatched(rangeEnc: RangeEncoder, matchByte: Byte, symbol: Byte): Unit = {
          var context = 1
          var same    = true
          var i       = 7
          while (i >= 0) {
            val bit      = (symbol >> i) & 1
            var encState = context
            if (same) {
              val matchBit = (matchByte >> i) & 1
              encState += (1 + matchBit) << 8
              same = matchBit == bit
            }
            rangeEnc.encode(encoders, encState, bit)
            context = (context << 1) | bit
            i -= 1
          }
        }

        def getPrice(matchMode: Boolean, matchByte: Byte, symbol: Byte): Int = {
          var price   = 0
          var context = 1
          var i       = 7
          if (matchMode) {
            while (i >= 0) {
              val matchBit = (matchByte >> i) & 1
              val bit      = (symbol >> i) & 1
              price += RangeEncoder.getPrice(encoders(((1 + matchBit) << 8) + context), bit)
              context = (context << 1) | bit
              if (matchBit != bit) {
                i -= 1
                var cont = true
                while (i >= 0 && cont) {
                  val b = (symbol >> i) & 1
                  price += RangeEncoder.getPrice(encoders(context), b)
                  context = (context << 1) | b
                  i -= 1
                }
                cont = false // stop outer while
              } else {
                i -= 1
              }
            }
          }
          while (i >= 0) {
            val bit = (symbol >> i) & 1
            price += RangeEncoder.getPrice(encoders(context), bit)
            context = (context << 1) | bit
            i -= 1
          }
          price
        }
      }

      private var coders:      Array[Encoder2] = null.asInstanceOf[Array[Encoder2]] // @nowarn — Java interop
      private var numPrevBits: Int             = 0
      private var numPosBits:  Int             = 0
      private var posMask:     Int             = 0

      def create(npb: Int, nprevb: Int): Unit =
        if (coders != null && numPrevBits == nprevb && numPosBits == npb) () // @nowarn — already created
        else {
          numPosBits = npb
          posMask = (1 << npb) - 1
          numPrevBits = nprevb
          val numStates = 1 << (numPrevBits + numPosBits)
          coders = new Array[Encoder2](numStates)
          var i = 0
          while (i < numStates) {
            coders(i) = new Encoder2()
            i += 1
          }
        }

      def init(): Unit = {
        val numStates = 1 << (numPrevBits + numPosBits)
        var i         = 0
        while (i < numStates) {
          coders(i).init()
          i += 1
        }
      }

      def getSubCoder(pos: Int, prevByte: Byte): Encoder2 =
        coders(((pos & posMask) << numPrevBits) + ((prevByte & 0xff) >>> (8 - numPrevBits)))
    }

    // -- Len encoder inner class --
    private class LenEncoder {
      val choice:   Array[Short]          = new Array[Short](2)
      val lowCoder: Array[BitTreeEncoder] = {
        val arr = new Array[BitTreeEncoder](kNumPosStatesEncodingMax)
        var i   = 0
        while (i < kNumPosStatesEncodingMax) {
          arr(i) = new BitTreeEncoder(kNumLowLenBits)
          i += 1
        }
        arr
      }
      val midCoder: Array[BitTreeEncoder] = {
        val arr = new Array[BitTreeEncoder](kNumPosStatesEncodingMax)
        var i   = 0
        while (i < kNumPosStatesEncodingMax) {
          arr(i) = new BitTreeEncoder(kNumMidLenBits)
          i += 1
        }
        arr
      }
      val highCoder: BitTreeEncoder = new BitTreeEncoder(kNumHighLenBits)

      def init(numPosStates: Int): Unit = {
        RangeEncoder.initBitModels(choice)
        var posState = 0
        while (posState < numPosStates) {
          lowCoder(posState).init()
          midCoder(posState).init()
          posState += 1
        }
        highCoder.init()
      }

      def encode(rangeEnc: RangeEncoder, symbol: Int, posState: Int): Unit =
        if (symbol < kNumLowLenSymbols) {
          rangeEnc.encode(choice, 0, 0)
          lowCoder(posState).encode(rangeEnc, symbol)
        } else {
          val s = symbol - kNumLowLenSymbols
          rangeEnc.encode(choice, 0, 1)
          if (s < kNumMidLenSymbols) {
            rangeEnc.encode(choice, 1, 0)
            midCoder(posState).encode(rangeEnc, s)
          } else {
            rangeEnc.encode(choice, 1, 1)
            highCoder.encode(rangeEnc, s - kNumMidLenSymbols)
          }
        }

      def setPrices(posState: Int, numSymbols: Int, prices: Array[Int], st: Int): Unit = {
        val a0 = RangeEncoder.getPrice0(choice(0))
        val a1 = RangeEncoder.getPrice1(choice(0))
        val b0 = a1 + RangeEncoder.getPrice0(choice(1))
        val b1 = a1 + RangeEncoder.getPrice1(choice(1))
        var i  = 0
        while (i < kNumLowLenSymbols && i < numSymbols) {
          prices(st + i) = a0 + lowCoder(posState).getPrice(i)
          i += 1
        }
        while (i < kNumLowLenSymbols + kNumMidLenSymbols && i < numSymbols) {
          prices(st + i) = b0 + midCoder(posState).getPrice(i - kNumLowLenSymbols)
          i += 1
        }
        while (i < numSymbols) {
          prices(st + i) = b1 + highCoder.getPrice(i - kNumLowLenSymbols - kNumMidLenSymbols)
          i += 1
        }
      }
    }

    // -- LenPriceTableEncoder --
    private class LenPriceTableEncoder extends LenEncoder {
      val prices:    Array[Int] = new Array[Int](kNumLenSymbols << kNumPosStatesBitsEncodingMax)
      var tableSize: Int        = 0
      val counters:  Array[Int] = new Array[Int](kNumPosStatesEncodingMax)

      def setTableSize(ts: Int): Unit = tableSize = ts

      def getPrice(symbol: Int, posState: Int): Int = prices(posState * kNumLenSymbols + symbol)

      def updateTable(posState: Int): Unit = {
        setPrices(posState, tableSize, prices, posState * kNumLenSymbols)
        counters(posState) = tableSize
      }

      def updateTables(numPosStates: Int): Unit = {
        var posState = 0
        while (posState < numPosStates) {
          updateTable(posState)
          posState += 1
        }
      }

      override def encode(rangeEnc: RangeEncoder, symbol: Int, posState: Int): Unit = {
        super.encode(rangeEnc, symbol, posState)
        counters(posState) -= 1
        if (counters(posState) == 0) updateTable(posState)
      }
    }

    // -- Optimal --
    private class Optimal {
      var state:       Int     = 0
      var prev1IsChar: Boolean = false
      var prev2:       Boolean = false
      var posPrev2:    Int     = 0
      var backPrev2:   Int     = 0
      var price:       Int     = 0
      var posPrev:     Int     = 0
      var backPrev:    Int     = 0
      var backs0:      Int     = 0
      var backs1:      Int     = 0
      var backs2:      Int     = 0
      var backs3:      Int     = 0

      def makeAsChar(): Unit = {
        backPrev = -1
        prev1IsChar = false
      }

      def makeAsShortRep(): Unit = {
        backPrev = 0
        prev1IsChar = false
      }

      def isShortRep: Boolean = backPrev == 0
    }

    private val optimum: Array[Optimal] = {
      val arr = new Array[Optimal](kNumOpts)
      var i   = 0
      while (i < kNumOpts) {
        arr(i) = new Optimal()
        i += 1
      }
      arr
    }

    private var matchFinder: BinTree = null.asInstanceOf[BinTree] // @nowarn — Java interop
    private val rangeEnc = new RangeEncoder()

    private val isMatch    = new Array[Short](kNumStates << kNumPosStatesBitsMax)
    private val isRep      = new Array[Short](kNumStates)
    private val isRepG0    = new Array[Short](kNumStates)
    private val isRepG1    = new Array[Short](kNumStates)
    private val isRepG2    = new Array[Short](kNumStates)
    private val isRep0Long = new Array[Short](kNumStates << kNumPosStatesBitsMax)

    private val posSlotEncoder: Array[BitTreeEncoder] = {
      val arr = new Array[BitTreeEncoder](kNumLenToPosStates)
      var i   = 0
      while (i < kNumLenToPosStates) {
        arr(i) = new BitTreeEncoder(kNumPosSlotBits)
        i += 1
      }
      arr
    }

    private val posEncoders     = new Array[Short](kNumFullDistances - kEndPosModelIndex)
    private val posAlignEncoder = new BitTreeEncoder(kNumAlignBits)

    private val lenEncoder         = new LenPriceTableEncoder()
    private val repMatchLenEncoder = new LenPriceTableEncoder()
    private val literalEncoder     = new LiteralEncoder()

    private val matchDistances = new Array[Int](kMatchMaxLen * 2 + 2)
    private var numFastBytes   = kNumFastBytesDefault
    private var longestMatchLength:   Int     = 0
    private var numDistancePairs:     Int     = 0
    private var additionalOffset:     Int     = 0
    private var optimumEndIndex:      Int     = 0
    private var optimumCurrentIndex:  Int     = 0
    private var longestMatchWasFound: Boolean = false

    private val posSlotPrices   = new Array[Int](1 << (kNumPosSlotBits + kNumLenToPosStatesBits))
    private val distancesPrices = new Array[Int](kNumFullDistances << kNumLenToPosStatesBits)
    private val alignPrices     = new Array[Int](kAlignTableSize)
    private var alignPriceCount: Int = 0

    private var distTableSize          = kDefaultDictionaryLogSize * 2
    private var posStateBits           = 2
    private var posStateMask           = 4 - 1
    private var numLiteralPosStateBits = 0
    private var numLiteralContextBits  = 3
    private var dictionarySize         = 1 << kDefaultDictionaryLogSize
    private var dictionarySizePrev     = -1
    private var numFastBytesPrev       = -1

    private var nowPos64: Long        = 0L
    private var finished: Boolean     = false
    private var inStream: InputStream = null.asInstanceOf[InputStream] // @nowarn — Java stream interop

    private var matchFinderType     = 1 // EMatchFinderTypeBT4
    private var writeEndMark        = false
    private var needReleaseMFStream = false

    private def createMatchFinder(): Unit = {
      if (matchFinder == null) { // @nowarn — Java interop null check
        val bt           = new BinTree()
        val numHashBytes = if (matchFinderType == 0) 2 else 4
        bt.setType(numHashBytes)
        matchFinder = bt
      }
      literalEncoder.create(numLiteralPosStateBits, numLiteralContextBits)
      if (dictionarySize != dictionarySizePrev || numFastBytesPrev != numFastBytes) {
        matchFinder.create(dictionarySize, kNumOpts, numFastBytes, kMatchMaxLen + 1)
        dictionarySizePrev = dictionarySize
        numFastBytesPrev = numFastBytes
      }
    }

    private def initEncoder(): Unit = {
      baseInit()
      rangeEnc.init()
      RangeEncoder.initBitModels(isMatch)
      RangeEncoder.initBitModels(isRep0Long)
      RangeEncoder.initBitModels(isRep)
      RangeEncoder.initBitModels(isRepG0)
      RangeEncoder.initBitModels(isRepG1)
      RangeEncoder.initBitModels(isRepG2)
      RangeEncoder.initBitModels(posEncoders)
      literalEncoder.init()
      var i = 0
      while (i < kNumLenToPosStates) {
        posSlotEncoder(i).init()
        i += 1
      }
      lenEncoder.init(1 << posStateBits)
      repMatchLenEncoder.init(1 << posStateBits)
      posAlignEncoder.init()
      longestMatchWasFound = false
      optimumEndIndex = 0
      optimumCurrentIndex = 0
      additionalOffset = 0
    }

    private def readMatchDistances(): Int = {
      var lenRes = 0
      numDistancePairs = matchFinder.getMatches(matchDistances)
      if (numDistancePairs > 0) {
        lenRes = matchDistances(numDistancePairs - 2)
        if (lenRes == numFastBytes) {
          lenRes += matchFinder.getMatchLen(lenRes - 1, matchDistances(numDistancePairs - 1), kMatchMaxLen - lenRes)
        }
      }
      additionalOffset += 1
      lenRes
    }

    private def movePos(num: Int): Unit =
      if (num > 0) {
        matchFinder.skip(num)
        additionalOffset += num
      }

    private def getRepLen1Price(st: Int, posState: Int): Int =
      RangeEncoder.getPrice0(isRepG0(st)) +
        RangeEncoder.getPrice0(isRep0Long((st << kNumPosStatesBitsMax) + posState))

    private def getPureRepPrice(repIndex: Int, st: Int, posState: Int): Int = {
      var price: Int = 0
      if (repIndex == 0) {
        price = RangeEncoder.getPrice0(isRepG0(st))
        price += RangeEncoder.getPrice1(isRep0Long((st << kNumPosStatesBitsMax) + posState))
      } else {
        price = RangeEncoder.getPrice1(isRepG0(st))
        if (repIndex == 1) {
          price += RangeEncoder.getPrice0(isRepG1(st))
        } else {
          price += RangeEncoder.getPrice1(isRepG1(st))
          price += RangeEncoder.getPrice(isRepG2(st), repIndex - 2)
        }
      }
      price
    }

    private def getRepPrice(repIndex: Int, len: Int, st: Int, posState: Int): Int =
      repMatchLenEncoder.getPrice(len - kMatchMinLen, posState) + getPureRepPrice(repIndex, st, posState)

    private def getPosLenPrice(pos: Int, len: Int, posState: Int): Int = {
      val lenToPosState = getLenToPosState(len)
      val price         = if (pos < kNumFullDistances) {
        distancesPrices(lenToPosState * kNumFullDistances + pos)
      } else {
        posSlotPrices((lenToPosState << kNumPosSlotBits) + getPosSlot2(pos)) + alignPrices(pos & kAlignMask)
      }
      price + lenEncoder.getPrice(len - kMatchMinLen, posState)
    }

    private val reps    = new Array[Int](kNumRepDistances)
    private val repLens = new Array[Int](kNumRepDistances)
    private var backRes: Int = 0

    private def backward(cur: Int): Int = {
      optimumEndIndex = cur
      var posMem  = optimum(cur).posPrev
      var backMem = optimum(cur).backPrev
      var c       = cur
      while (c > 0) {
        if (optimum(c).prev1IsChar) {
          optimum(posMem).makeAsChar()
          optimum(posMem).posPrev = posMem - 1
          if (optimum(c).prev2) {
            optimum(posMem - 1).prev1IsChar = false
            optimum(posMem - 1).posPrev = optimum(c).posPrev2
            optimum(posMem - 1).backPrev = optimum(c).backPrev2
          }
        }
        val posPrev = posMem
        val backCur = backMem
        backMem = optimum(posPrev).backPrev
        posMem = optimum(posPrev).posPrev
        optimum(posPrev).backPrev = backCur
        optimum(posPrev).posPrev = c
        c = posPrev
      }
      backRes = optimum(0).backPrev
      optimumCurrentIndex = optimum(0).posPrev
      optimumCurrentIndex
    }

    private def getOptimum(position: Int): Int = {
      if (optimumEndIndex != optimumCurrentIndex) {
        val lenRes = optimum(optimumCurrentIndex).posPrev - optimumCurrentIndex
        backRes = optimum(optimumCurrentIndex).backPrev
        optimumCurrentIndex = optimum(optimumCurrentIndex).posPrev
        return lenRes // @nowarn — LZMA SDK algorithm, early exit required
      }
      optimumCurrentIndex = 0
      optimumEndIndex = 0

      var lenMain: Int = 0
      if (!longestMatchWasFound) {
        lenMain = readMatchDistances()
      } else {
        lenMain = longestMatchLength
        longestMatchWasFound = false
      }
      var ndp = numDistancePairs

      var numAvailableBytes = matchFinder.getNumAvailableBytes + 1
      if (numAvailableBytes < 2) {
        backRes = -1
        return 1 // @nowarn — LZMA SDK algorithm
      }
      if (numAvailableBytes > kMatchMaxLen) numAvailableBytes = kMatchMaxLen

      var repMaxIndex = 0
      var i           = 0
      while (i < kNumRepDistances) {
        reps(i) = repDistances(i)
        repLens(i) = matchFinder.getMatchLen(0 - 1, reps(i), kMatchMaxLen)
        if (repLens(i) > repLens(repMaxIndex)) repMaxIndex = i
        i += 1
      }
      if (repLens(repMaxIndex) >= numFastBytes) {
        backRes = repMaxIndex
        val lenRes = repLens(repMaxIndex)
        movePos(lenRes - 1)
        return lenRes // @nowarn — LZMA SDK algorithm
      }

      if (lenMain >= numFastBytes) {
        backRes = matchDistances(ndp - 1) + kNumRepDistances
        movePos(lenMain - 1)
        return lenMain // @nowarn — LZMA SDK algorithm
      }

      var currentByte = matchFinder.getIndexByte(0 - 1)
      var matchByte   = matchFinder.getIndexByte(0 - repDistances(0) - 1 - 1)

      if (lenMain < 2 && currentByte != matchByte && repLens(repMaxIndex) < 2) {
        backRes = -1
        return 1 // @nowarn — LZMA SDK algorithm
      }

      optimum(0).state = state
      val posState = position & posStateMask

      optimum(1).price = RangeEncoder.getPrice0(isMatch((state << kNumPosStatesBitsMax) + posState)) +
        literalEncoder.getSubCoder(position, previousByte).getPrice(!stateIsCharState(state), matchByte, currentByte)
      optimum(1).makeAsChar()

      var matchPrice    = RangeEncoder.getPrice1(isMatch((state << kNumPosStatesBitsMax) + posState))
      var repMatchPrice = matchPrice + RangeEncoder.getPrice1(isRep(state))

      if (matchByte == currentByte) {
        val shortRepPrice = repMatchPrice + getRepLen1Price(state, posState)
        if (shortRepPrice < optimum(1).price) {
          optimum(1).price = shortRepPrice
          optimum(1).makeAsShortRep()
        }
      }

      var lenEnd = if (lenMain >= repLens(repMaxIndex)) lenMain else repLens(repMaxIndex)

      if (lenEnd < 2) {
        backRes = optimum(1).backPrev
        return 1 // @nowarn — LZMA SDK algorithm
      }

      optimum(1).posPrev = 0
      optimum(0).backs0 = reps(0)
      optimum(0).backs1 = reps(1)
      optimum(0).backs2 = reps(2)
      optimum(0).backs3 = reps(3)

      var len = lenEnd
      while (len >= 2) {
        optimum(len).price = kIfinityPrice
        len -= 1
      }

      i = 0
      while (i < kNumRepDistances) {
        var repLen = repLens(i)
        if (repLen >= 2) {
          val price = repMatchPrice + getPureRepPrice(i, state, posState)
          while (repLen >= 2) {
            val curAndLenPrice = price + repMatchLenEncoder.getPrice(repLen - 2, posState)
            val opt            = optimum(repLen)
            if (curAndLenPrice < opt.price) {
              opt.price = curAndLenPrice
              opt.posPrev = 0
              opt.backPrev = i
              opt.prev1IsChar = false
            }
            repLen -= 1
          }
        }
        i += 1
      }

      var normalMatchPrice = matchPrice + RangeEncoder.getPrice0(isRep(state))

      len = if (repLens(0) >= 2) repLens(0) + 1 else 2
      if (len <= lenMain) {
        var offs = 0
        while (len > matchDistances(offs)) offs += 2
        var cont = true
        while (cont) {
          val distance       = matchDistances(offs + 1)
          val curAndLenPrice = normalMatchPrice + getPosLenPrice(distance, len, posState)
          val opt            = optimum(len)
          if (curAndLenPrice < opt.price) {
            opt.price = curAndLenPrice
            opt.posPrev = 0
            opt.backPrev = distance + kNumRepDistances
            opt.prev1IsChar = false
          }
          if (len == matchDistances(offs)) {
            offs += 2
            if (offs == ndp) cont = false
          }
          if (cont) len += 1
        }
      }

      var cur      = 0
      var mainCont = true
      while (mainCont) {
        cur += 1
        if (cur == lenEnd) {
          mainCont = false
          return backward(cur) // @nowarn — LZMA SDK algorithm
        } else {
          val newLen = readMatchDistances()
          ndp = numDistancePairs
          if (newLen >= numFastBytes) {
            longestMatchLength = newLen
            longestMatchWasFound = true
            mainCont = false
            return backward(cur) // @nowarn — LZMA SDK algorithm
          } else {
            val pos2    = position + cur
            var posPrev = optimum(cur).posPrev
            var st: Int = 0
            if (optimum(cur).prev1IsChar) {
              posPrev -= 1
              if (optimum(cur).prev2) {
                st = optimum(optimum(cur).posPrev2).state
                if (optimum(cur).backPrev2 < kNumRepDistances) st = stateUpdateRep(st)
                else st = stateUpdateMatch(st)
              } else {
                st = optimum(posPrev).state
              }
              st = stateUpdateChar(st)
            } else {
              st = optimum(posPrev).state
            }
            if (posPrev == cur - 1) {
              if (optimum(cur).isShortRep) st = stateUpdateShortRep(st)
              else st = stateUpdateChar(st)
            } else {
              var pos3: Int = 0
              if (optimum(cur).prev1IsChar && optimum(cur).prev2) {
                posPrev = optimum(cur).posPrev2
                pos3 = optimum(cur).backPrev2
                st = stateUpdateRep(st)
              } else {
                pos3 = optimum(cur).backPrev
                if (pos3 < kNumRepDistances) st = stateUpdateRep(st)
                else st = stateUpdateMatch(st)
              }
              val opt = optimum(posPrev)
              if (pos3 < kNumRepDistances) {
                if (pos3 == 0) {
                  reps(0) = opt.backs0; reps(1) = opt.backs1; reps(2) = opt.backs2; reps(3) = opt.backs3
                } else if (pos3 == 1) {
                  reps(0) = opt.backs1; reps(1) = opt.backs0; reps(2) = opt.backs2; reps(3) = opt.backs3
                } else if (pos3 == 2) {
                  reps(0) = opt.backs2; reps(1) = opt.backs0; reps(2) = opt.backs1; reps(3) = opt.backs3
                } else {
                  reps(0) = opt.backs3; reps(1) = opt.backs0; reps(2) = opt.backs1; reps(3) = opt.backs2
                }
              } else {
                reps(0) = pos3 - kNumRepDistances; reps(1) = opt.backs0; reps(2) = opt.backs1; reps(3) = opt.backs2
              }
            }
            optimum(cur).state = st
            optimum(cur).backs0 = reps(0)
            optimum(cur).backs1 = reps(1)
            optimum(cur).backs2 = reps(2)
            optimum(cur).backs3 = reps(3)
            val curPrice = optimum(cur).price

            currentByte = matchFinder.getIndexByte(0 - 1)
            matchByte = matchFinder.getIndexByte(0 - reps(0) - 1 - 1)

            val ps = (pos2.toInt) & posStateMask

            val curAnd1Price = curPrice +
              RangeEncoder.getPrice0(isMatch((st << kNumPosStatesBitsMax) + ps)) +
              literalEncoder.getSubCoder(pos2.toInt, matchFinder.getIndexByte(0 - 2)).getPrice(!stateIsCharState(st), matchByte, currentByte)

            val nextOptimum = optimum(cur + 1)
            var nextIsChar  = false
            if (curAnd1Price < nextOptimum.price) {
              nextOptimum.price = curAnd1Price
              nextOptimum.posPrev = cur
              nextOptimum.makeAsChar()
              nextIsChar = true
            }

            matchPrice = curPrice + RangeEncoder.getPrice1(isMatch((st << kNumPosStatesBitsMax) + ps))
            repMatchPrice = matchPrice + RangeEncoder.getPrice1(isRep(st))

            if (matchByte == currentByte && !(nextOptimum.posPrev < cur && nextOptimum.backPrev == 0)) {
              val shortRepPrice = repMatchPrice + getRepLen1Price(st, ps)
              if (shortRepPrice <= nextOptimum.price) {
                nextOptimum.price = shortRepPrice
                nextOptimum.posPrev = cur
                nextOptimum.makeAsShortRep()
                nextIsChar = true
              }
            }

            var numAvailableBytesFull = matchFinder.getNumAvailableBytes + 1
            numAvailableBytesFull = Math.min(kNumOpts - 1 - cur, numAvailableBytesFull)
            numAvailableBytes = numAvailableBytesFull

            if (numAvailableBytes >= 2) {
              if (numAvailableBytes > numFastBytes) numAvailableBytes = numFastBytes
              if (!nextIsChar && matchByte != currentByte) {
                val t        = Math.min(numAvailableBytesFull - 1, numFastBytes)
                val lenTest2 = matchFinder.getMatchLen(0, reps(0), t)
                if (lenTest2 >= 2) {
                  val state2            = stateUpdateChar(st)
                  val posStateNext      = (pos2.toInt + 1) & posStateMask
                  val nextRepMatchPrice = curAnd1Price +
                    RangeEncoder.getPrice1(isMatch((state2 << kNumPosStatesBitsMax) + posStateNext)) +
                    RangeEncoder.getPrice1(isRep(state2))
                  val offset = cur + 1 + lenTest2
                  while (lenEnd < offset) {
                    lenEnd += 1
                    optimum(lenEnd).price = kIfinityPrice
                  }
                  val curAndLenPrice = nextRepMatchPrice + getRepPrice(0, lenTest2, state2, posStateNext)
                  val opt2           = optimum(offset)
                  if (curAndLenPrice < opt2.price) {
                    opt2.price = curAndLenPrice
                    opt2.posPrev = cur + 1
                    opt2.backPrev = 0
                    opt2.prev1IsChar = true
                    opt2.prev2 = false
                  }
                }
              }

              var startLen = 2
              var repIndex = 0
              while (repIndex < kNumRepDistances) {
                var lenTest = matchFinder.getMatchLen(0 - 1, reps(repIndex), numAvailableBytes)
                if (lenTest >= 2) {
                  val lenTestTemp = lenTest
                  while (lenTest >= 2) {
                    while (lenEnd < cur + lenTest) {
                      lenEnd += 1
                      optimum(lenEnd).price = kIfinityPrice
                    }
                    val curAndLenPrice = repMatchPrice + getRepPrice(repIndex, lenTest, st, ps)
                    val opt2           = optimum(cur + lenTest)
                    if (curAndLenPrice < opt2.price) {
                      opt2.price = curAndLenPrice
                      opt2.posPrev = cur
                      opt2.backPrev = repIndex
                      opt2.prev1IsChar = false
                    }
                    lenTest -= 1
                  }
                  lenTest = lenTestTemp

                  if (repIndex == 0) startLen = lenTest + 1

                  if (lenTest < numAvailableBytesFull) {
                    val t        = Math.min(numAvailableBytesFull - 1 - lenTest, numFastBytes)
                    val lenTest2 = matchFinder.getMatchLen(lenTest, reps(repIndex), t)
                    if (lenTest2 >= 2) {
                      var state2             = stateUpdateRep(st)
                      var posStateNext       = (pos2.toInt + lenTest) & posStateMask
                      val curAndLenCharPrice = repMatchPrice + getRepPrice(repIndex, lenTest, st, ps) +
                        RangeEncoder.getPrice0(isMatch((state2 << kNumPosStatesBitsMax) + posStateNext)) +
                        literalEncoder
                          .getSubCoder(pos2.toInt + lenTest, matchFinder.getIndexByte(lenTest - 1 - 1))
                          .getPrice(true, matchFinder.getIndexByte(lenTest - 1 - (reps(repIndex) + 1)), matchFinder.getIndexByte(lenTest - 1))
                      state2 = stateUpdateChar(state2)
                      posStateNext = (pos2.toInt + lenTest + 1) & posStateMask
                      val nextMatchPrice2 = curAndLenCharPrice +
                        RangeEncoder.getPrice1(isMatch((state2 << kNumPosStatesBitsMax) + posStateNext))
                      val nextRepMatchPrice2 = nextMatchPrice2 + RangeEncoder.getPrice1(isRep(state2))
                      val offset             = lenTest + 1 + lenTest2
                      while (lenEnd < cur + offset) {
                        lenEnd += 1
                        optimum(lenEnd).price = kIfinityPrice
                      }
                      val curAndLenPrice = nextRepMatchPrice2 + getRepPrice(0, lenTest2, state2, posStateNext)
                      val opt2           = optimum(cur + offset)
                      if (curAndLenPrice < opt2.price) {
                        opt2.price = curAndLenPrice
                        opt2.posPrev = cur + lenTest + 1
                        opt2.backPrev = 0
                        opt2.prev1IsChar = true
                        opt2.prev2 = true
                        opt2.posPrev2 = cur
                        opt2.backPrev2 = repIndex
                      }
                    }
                  }
                }
                repIndex += 1
              }

              var newLen2 = newLen
              if (newLen2 > numAvailableBytes) {
                newLen2 = numAvailableBytes
                ndp = 0
                while (newLen2 > matchDistances(ndp)) ndp += 2
                matchDistances(ndp) = newLen2
                ndp += 2
                numDistancePairs = ndp
              }
              if (newLen2 >= startLen) {
                normalMatchPrice = matchPrice + RangeEncoder.getPrice0(isRep(st))
                while (lenEnd < cur + newLen2) {
                  lenEnd += 1
                  optimum(lenEnd).price = kIfinityPrice
                }
                var offs = 0
                while (startLen > matchDistances(offs)) offs += 2
                var lenTest   = startLen
                var innerCont = true
                while (innerCont) {
                  val curBack        = matchDistances(offs + 1)
                  val curAndLenPrice = normalMatchPrice + getPosLenPrice(curBack, lenTest, ps)
                  val opt2           = optimum(cur + lenTest)
                  if (curAndLenPrice < opt2.price) {
                    opt2.price = curAndLenPrice
                    opt2.posPrev = cur
                    opt2.backPrev = curBack + kNumRepDistances
                    opt2.prev1IsChar = false
                  }

                  if (lenTest == matchDistances(offs)) {
                    if (lenTest < numAvailableBytesFull) {
                      val t        = Math.min(numAvailableBytesFull - 1 - lenTest, numFastBytes)
                      val lenTest2 = matchFinder.getMatchLen(lenTest, curBack, t)
                      if (lenTest2 >= 2) {
                        var state2             = stateUpdateMatch(st)
                        var posStateNext       = (pos2.toInt + lenTest) & posStateMask
                        val curAndLenCharPrice = curAndLenPrice +
                          RangeEncoder.getPrice0(isMatch((state2 << kNumPosStatesBitsMax) + posStateNext)) +
                          literalEncoder
                            .getSubCoder(pos2.toInt + lenTest, matchFinder.getIndexByte(lenTest - 1 - 1))
                            .getPrice(true, matchFinder.getIndexByte(lenTest - (curBack + 1) - 1), matchFinder.getIndexByte(lenTest - 1))
                        state2 = stateUpdateChar(state2)
                        posStateNext = (pos2.toInt + lenTest + 1) & posStateMask
                        val nextMatchPrice2 = curAndLenCharPrice +
                          RangeEncoder.getPrice1(isMatch((state2 << kNumPosStatesBitsMax) + posStateNext))
                        val nextRepMatchPrice2 = nextMatchPrice2 + RangeEncoder.getPrice1(isRep(state2))
                        val offset             = lenTest + 1 + lenTest2
                        while (lenEnd < cur + offset) {
                          lenEnd += 1
                          optimum(lenEnd).price = kIfinityPrice
                        }
                        val clp  = nextRepMatchPrice2 + getRepPrice(0, lenTest2, state2, posStateNext)
                        val opt3 = optimum(cur + offset)
                        if (clp < opt3.price) {
                          opt3.price = clp
                          opt3.posPrev = cur + lenTest + 1
                          opt3.backPrev = 0
                          opt3.prev1IsChar = true
                          opt3.prev2 = true
                          opt3.posPrev2 = cur
                          opt3.backPrev2 = curBack + kNumRepDistances
                        }
                      }
                    }
                    offs += 2
                    if (offs == ndp) innerCont = false
                  }
                  if (innerCont) lenTest += 1
                }
              }
            }
          }
        }
      }
      // should not reach here
      0
    }

    private def writeEndMarker(posState: Int): Unit =
      if (writeEndMark) {
        rangeEnc.encode(isMatch, (state << kNumPosStatesBitsMax) + posState, 1)
        rangeEnc.encode(isRep, state, 0)
        state = stateUpdateMatch(state)
        val len = kMatchMinLen
        lenEncoder.encode(rangeEnc, len - kMatchMinLen, posState)
        val posSlot       = (1 << kNumPosSlotBits) - 1
        val lenToPosState = getLenToPosState(len)
        posSlotEncoder(lenToPosState).encode(rangeEnc, posSlot)
        val footerBits = 30
        val posReduced = (1 << footerBits) - 1
        rangeEnc.encodeDirectBits(posReduced >> kNumAlignBits, footerBits - kNumAlignBits)
        posAlignEncoder.reverseEncode(rangeEnc, posReduced & kAlignMask)
      }

    private def flush(nowPos: Int): Unit = {
      releaseMFStream()
      writeEndMarker(nowPos & posStateMask)
      rangeEnc.flushData()
      rangeEnc.flushStream()
    }

    private var matchPriceCount: Int = 0

    private def codeOneBlock(inSize: Array[Long], outSize: Array[Long], isFinished: Array[Boolean]): Unit = {
      inSize(0) = 0
      outSize(0) = 0
      isFinished(0) = true

      if (inStream != null) { // @nowarn — Java interop null check
        matchFinder.setStream(inStream)
        matchFinder.init()
        needReleaseMFStream = true
        inStream = null.asInstanceOf[InputStream] // @nowarn — Java stream interop
      }

      if (finished) () // @nowarn — already finished
      else {
        finished = true
        val progressPosValuePrev = nowPos64
        if (nowPos64 == 0L) {
          if (matchFinder.getNumAvailableBytes == 0) {
            flush(nowPos64.toInt)
          } else {
            readMatchDistances()
            val posState = nowPos64.toInt & posStateMask
            rangeEnc.encode(isMatch, (state << kNumPosStatesBitsMax) + posState, 0)
            state = stateUpdateChar(state)
            val curByte = matchFinder.getIndexByte(0 - additionalOffset)
            literalEncoder.getSubCoder(nowPos64.toInt, previousByte).encode(rangeEnc, curByte)
            previousByte = curByte
            additionalOffset -= 1
            nowPos64 += 1
          }
        }
        if (matchFinder.getNumAvailableBytes == 0) {
          flush(nowPos64.toInt)
        } else {
          var cont = true
          while (cont) {
            val len          = getOptimum(nowPos64.toInt)
            var pos          = backRes
            val posState     = nowPos64.toInt & posStateMask
            val complexState = (state << kNumPosStatesBitsMax) + posState
            if (len == 1 && pos == -1) {
              rangeEnc.encode(isMatch, complexState, 0)
              val curByte  = matchFinder.getIndexByte(0 - additionalOffset)
              val subCoder = literalEncoder.getSubCoder(nowPos64.toInt, previousByte)
              if (!stateIsCharState(state)) {
                val mb = matchFinder.getIndexByte(0 - repDistances(0) - 1 - additionalOffset)
                subCoder.encodeMatched(rangeEnc, mb, curByte)
              } else {
                subCoder.encode(rangeEnc, curByte)
              }
              previousByte = curByte
              state = stateUpdateChar(state)
            } else {
              rangeEnc.encode(isMatch, complexState, 1)
              if (pos < kNumRepDistances) {
                rangeEnc.encode(isRep, state, 1)
                if (pos == 0) {
                  rangeEnc.encode(isRepG0, state, 0)
                  if (len == 1) rangeEnc.encode(isRep0Long, complexState, 0)
                  else rangeEnc.encode(isRep0Long, complexState, 1)
                } else {
                  rangeEnc.encode(isRepG0, state, 1)
                  if (pos == 1) rangeEnc.encode(isRepG1, state, 0)
                  else {
                    rangeEnc.encode(isRepG1, state, 1)
                    rangeEnc.encode(isRepG2, state, pos - 2)
                  }
                }
                if (len == 1) {
                  state = stateUpdateShortRep(state)
                } else {
                  repMatchLenEncoder.encode(rangeEnc, len - kMatchMinLen, posState)
                  state = stateUpdateRep(state)
                }
                val distance = repDistances(pos)
                if (pos != 0) {
                  var idx = pos
                  while (idx >= 1) {
                    repDistances(idx) = repDistances(idx - 1)
                    idx -= 1
                  }
                  repDistances(0) = distance
                }
              } else {
                rangeEnc.encode(isRep, state, 0)
                state = stateUpdateMatch(state)
                lenEncoder.encode(rangeEnc, len - kMatchMinLen, posState)
                pos -= kNumRepDistances
                val posSlot       = getPosSlot(pos)
                val lenToPosState = getLenToPosState(len)
                posSlotEncoder(lenToPosState).encode(rangeEnc, posSlot)

                if (posSlot >= kStartPosModelIndex) {
                  val footerBits = (posSlot >> 1) - 1
                  val baseVal    = (2 | (posSlot & 1)) << footerBits
                  val posReduced = pos - baseVal

                  if (posSlot < kEndPosModelIndex) {
                    BitTreeEncoder.reverseEncode(posEncoders, baseVal - posSlot - 1, rangeEnc, footerBits, posReduced)
                  } else {
                    rangeEnc.encodeDirectBits(posReduced >> kNumAlignBits, footerBits - kNumAlignBits)
                    posAlignEncoder.reverseEncode(rangeEnc, posReduced & kAlignMask)
                    alignPriceCount += 1
                  }
                }
                val distance = pos
                var idx      = kNumRepDistances - 1
                while (idx >= 1) {
                  repDistances(idx) = repDistances(idx - 1)
                  idx -= 1
                }
                repDistances(0) = distance
                matchPriceCount += 1
              }
              previousByte = matchFinder.getIndexByte(len - 1 - additionalOffset)
            }
            additionalOffset -= len
            nowPos64 += len
            if (additionalOffset == 0) {
              if (matchPriceCount >= (1 << 7)) fillDistancesPrices()
              if (alignPriceCount >= kAlignTableSize) fillAlignPrices()
              inSize(0) = nowPos64
              outSize(0) = rangeEnc.getProcessedSizeAdd
              if (matchFinder.getNumAvailableBytes == 0) {
                flush(nowPos64.toInt)
                cont = false
              } else if (nowPos64 - progressPosValuePrev >= (1 << 12)) {
                finished = false
                isFinished(0) = false
                cont = false
              }
            }
          }
        }
      }
    }

    private def releaseMFStream(): Unit =
      if (matchFinder != null && needReleaseMFStream) { // @nowarn — Java interop null check
        matchFinder.releaseStream()
        needReleaseMFStream = false
      }

    private def releaseStreams(): Unit = {
      releaseMFStream()
      rangeEnc.releaseStream()
    }

    private def setStreams(is: InputStream, os: OutputStream, inSize: Long, outSize: Long): Unit = {
      inStream = is
      finished = false
      createMatchFinder()
      rangeEnc.setStream(os)
      initEncoder()

      fillDistancesPrices()
      fillAlignPrices()

      lenEncoder.setTableSize(numFastBytes + 1 - kMatchMinLen)
      lenEncoder.updateTables(1 << posStateBits)
      repMatchLenEncoder.setTableSize(numFastBytes + 1 - kMatchMinLen)
      repMatchLenEncoder.updateTables(1 << posStateBits)

      nowPos64 = 0
    }

    private val processedInSize  = new Array[Long](1)
    private val processedOutSize = new Array[Long](1)
    private val isFinished       = new Array[Boolean](1)

    def code(is: InputStream, os: OutputStream, inSize: Long, outSize: Long): Unit = {
      needReleaseMFStream = false
      try {
        setStreams(is, os, inSize, outSize)
        var cont = true
        while (cont) {
          codeOneBlock(processedInSize, processedOutSize, isFinished)
          if (isFinished(0)) cont = false
        }
      } finally
        releaseStreams()
    }

    def writeCoderProperties(outStream: OutputStream): Unit = {
      val properties = new Array[Byte](5)
      properties(0) = ((posStateBits * 5 + numLiteralPosStateBits) * 9 + numLiteralContextBits).toByte
      var i = 0
      while (i < 4) {
        properties(1 + i) = (dictionarySize >> (8 * i)).toByte
        i += 1
      }
      outStream.write(properties, 0, 5)
    }

    private val tempPrices = new Array[Int](kNumFullDistances)

    private def fillDistancesPrices(): Unit = {
      var i = kStartPosModelIndex
      while (i < kNumFullDistances) {
        val posSlot    = getPosSlot(i)
        val footerBits = (posSlot >> 1) - 1
        val baseVal    = (2 | (posSlot & 1)) << footerBits
        tempPrices(i) = BitTreeEncoder.reverseGetPrice(posEncoders, baseVal - posSlot - 1, footerBits, i - baseVal)
        i += 1
      }

      var lenToPosState = 0
      while (lenToPosState < kNumLenToPosStates) {
        val encoder = posSlotEncoder(lenToPosState)
        val st      = lenToPosState << kNumPosSlotBits
        var posSlot = 0
        while (posSlot < distTableSize) {
          posSlotPrices(st + posSlot) = encoder.getPrice(posSlot)
          posSlot += 1
        }
        posSlot = kEndPosModelIndex
        while (posSlot < distTableSize) {
          posSlotPrices(st + posSlot) += ((((posSlot >> 1) - 1) - kNumAlignBits) << RangeEncoder.kNumBitPriceShiftBits)
          posSlot += 1
        }

        val st2 = lenToPosState * kNumFullDistances
        i = 0
        while (i < kStartPosModelIndex) {
          distancesPrices(st2 + i) = posSlotPrices(st + i)
          i += 1
        }
        while (i < kNumFullDistances) {
          distancesPrices(st2 + i) = posSlotPrices(st + getPosSlot(i)) + tempPrices(i)
          i += 1
        }
        lenToPosState += 1
      }
      matchPriceCount = 0
    }

    private def fillAlignPrices(): Unit = {
      var i = 0
      while (i < kAlignTableSize) {
        alignPrices(i) = posAlignEncoder.reverseGetPrice(i)
        i += 1
      }
      alignPriceCount = 0
    }

    def setAlgorithm(algorithm: Int): Boolean = true

    def setDictionarySize(ds: Int): Boolean = {
      val kDicLogSizeMaxCompress = 29
      if (ds < (1 << kDicLogSizeMin) || ds > (1 << kDicLogSizeMaxCompress)) false
      else {
        dictionarySize = ds
        var dicLogSize = 0
        while (ds > (1 << dicLogSize)) dicLogSize += 1
        distTableSize = dicLogSize * 2
        true
      }
    }

    def setNumFastBytes(nfb: Int): Boolean =
      if (nfb < 5 || nfb > kMatchMaxLen) false
      else {
        numFastBytes = nfb
        true
      }

    def setMatchFinder(matchFinderIndex: Int): Boolean =
      if (matchFinderIndex < 0 || matchFinderIndex > 2) false
      else {
        val prev = matchFinderType
        matchFinderType = matchFinderIndex
        if (matchFinder != null && prev != matchFinderType) { // @nowarn — Java interop null check
          dictionarySizePrev = -1
          matchFinder = null.asInstanceOf[BinTree] // @nowarn — Java interop
        }
        true
      }

    def setLcLpPb(lc: Int, lp: Int, pb: Int): Boolean =
      if (
        lp < 0 || lp > kNumLitPosStatesBitsEncodingMax || lc < 0 || lc > kNumLitContextBitsMax ||
        pb < 0 || pb > kNumPosStatesBitsEncodingMax
      ) {
        false
      } else {
        numLiteralPosStateBits = lp
        numLiteralContextBits = lc
        posStateBits = pb
        posStateMask = (1 << posStateBits) - 1
        true
      }

    def setEndMarkerMode(endMarkerMode: Boolean): Unit =
      writeEndMark = endMarkerMode
  }
}
