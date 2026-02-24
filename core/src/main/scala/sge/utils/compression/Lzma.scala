/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/Lzma.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils
package compression

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Adapted from LZMA SDK version 9.22.
  *
  * This was modified to be used directly on streams, rather than via the command line as in the LZMA SDK.
  *
  * We only currently allow the default LZMA options to be used, as we know it works on for our target usage.
  */
object Lzma {
  class CommandLine {
    val kEncode:   Int = 0
    val kDecode:   Int = 1
    val kBenchmak: Int = 2

    var Command:            Int = -1
    var NumBenchmarkPasses: Int = 10

    var DictionarySize:          Int     = 1 << 23
    var DictionarySizeIsDefined: Boolean = false

    var Lc: Int = 3
    var Lp: Int = 0
    var Pb: Int = 2

    var Fb:          Int     = 128
    var FbIsDefined: Boolean = false

    var Eos: Boolean = false

    var Algorithm:   Int = 2
    var MatchFinder: Int = 1

    var InFile:  String = scala.compiletime.uninitialized
    var OutFile: String = scala.compiletime.uninitialized
  }

  /** Compresses the given InputStream into the given OutputStream. */
  def compress(in: InputStream, out: OutputStream): Unit = {
    val params = new CommandLine()
    var eos    = false
    if (params.Eos) eos = true
    val encoder = new sge.utils.compression.lzma.Encoder()
    if (!encoder.SetAlgorithm(params.Algorithm)) throw new RuntimeException("Incorrect compression mode")
    if (!encoder.SetDictionarySize(params.DictionarySize)) throw new RuntimeException("Incorrect dictionary size")
    if (!encoder.SetNumFastBytes(params.Fb)) throw new RuntimeException("Incorrect -fb value")
    if (!encoder.SetMatchFinder(params.MatchFinder)) throw new RuntimeException("Incorrect -mf value")
    if (!encoder.SetLcLpPb(params.Lc, params.Lp, params.Pb)) throw new RuntimeException("Incorrect -lc or -lp or -pb value")
    encoder.SetEndMarkerMode(eos)
    encoder.WriteCoderProperties(out)
    var fileSize: Long = 0L
    if (eos) {
      fileSize = -1
    } else {
      fileSize = in.available()
      if (fileSize == 0) fileSize = -1
    }
    for (i <- 0 until 8)
      out.write(((fileSize >>> (8 * i)) & 0xff).toInt)
    encoder.Code(in, out, -1, -1, null)
  }

  /** Decompresses the given InputStream into the given OutputStream. */
  def decompress(in: InputStream, out: OutputStream): Unit = {
    val propertiesSize = 5
    val properties     = new Array[Byte](propertiesSize)
    if (in.read(properties, 0, propertiesSize) != propertiesSize) throw new RuntimeException("input .lzma file is too short")
    val decoder = new sge.utils.compression.lzma.Decoder()
    if (!decoder.SetDecoderProperties(properties)) throw new RuntimeException("Incorrect stream properties")
    var outSize: Long = 0L
    for (i <- 0 until 8) {
      val v = in.read()
      if (v < 0) throw new RuntimeException("Can't read stream size")
      outSize |= (v.toLong) << (8 * i)
    }
    if (!decoder.Code(in, out, outSize)) throw new RuntimeException("Error in data stream")
  }
}
