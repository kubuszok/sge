/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/rangecoder/BitTreeDecoder.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.utils.compression.rangecoder

class BitTreeDecoder(numBitLevels: Int) {
  val NumBitLevels: Int          = numBitLevels
  val Models:       Array[Short] = Array.ofDim[Short](1 << numBitLevels)

  def init(): Unit =
    Decoder.initBitModels(Models)

  @throws[java.io.IOException]
  def decode(rangeDecoder: Decoder): Int = {
    var m        = 1
    var bitIndex = NumBitLevels
    while (bitIndex != 0) {
      m = (m << 1) + rangeDecoder.decodeBit(Models, m)
      bitIndex -= 1
    }
    m - (1 << NumBitLevels)
  }

  @throws[java.io.IOException]
  def reverseDecode(rangeDecoder: Decoder): Int = {
    var m      = 1
    var symbol = 0
    for (bitIndex <- 0 until NumBitLevels) {
      val bit = rangeDecoder.decodeBit(Models, m)
      m = (m << 1) + bit
      symbol |= (bit << bitIndex)
    }
    symbol
  }
}

object BitTreeDecoder {
  @throws[java.io.IOException]
  def reverseDecode(models: Array[Short], startIndex: Int, rangeDecoder: Decoder, numBitLevels: Int): Int = {
    var m      = 1
    var symbol = 0
    for (bitIndex <- 0 until numBitLevels) {
      val bit = rangeDecoder.decodeBit(models, startIndex + m)
      m = (m << 1) + bit
      symbol |= (bit << bitIndex)
    }
    symbol
  }
}
