package sge.utils.compression.rangecoder

import java.io.IOException

class BitTreeEncoder(numBitLevels: Int) {
  val NumBitLevels: Int = numBitLevels
  val Models: Array[Short] = Array.ofDim[Short](1 << numBitLevels)

  def init(): Unit = {
    Decoder.initBitModels(Models)
  }

  def encode(rangeEncoder: Encoder, symbol: Int): Unit = {
    var m = 1
    var bitIndex = NumBitLevels
    while (bitIndex != 0) {
      bitIndex -= 1
      val bit = (symbol >>> bitIndex) & 1
      rangeEncoder.encode(Models, m, bit)
      m = (m << 1) | bit
    }
  }

  def reverseEncode(rangeEncoder: Encoder, symbol: Int): Unit = {
    var m = 1
    var s = symbol
    for (i <- 0 until NumBitLevels) {
      val bit = s & 1
      rangeEncoder.encode(Models, m, bit)
      m = (m << 1) | bit
      s >>= 1
    }
  }

  def getPrice(symbol: Int): Int = {
    var price = 0
    var m = 1
    var bitIndex = NumBitLevels
    while (bitIndex != 0) {
      bitIndex -= 1
      val bit = (symbol >>> bitIndex) & 1
      price += Encoder.getPrice(Models(m), bit)
      m = (m << 1) + bit
    }
    price
  }

  def reverseGetPrice(symbol: Int): Int = {
    var price = 0
    var m = 1
    var s = symbol
    var i = NumBitLevels
    while (i != 0) {
      val bit = s & 1
      s >>>= 1
      price += Encoder.getPrice(Models(m), bit)
      m = (m << 1) | bit
      i -= 1
    }
    price
  }
}

object BitTreeEncoder {
  def reverseGetPrice(models: Array[Short], startIndex: Int, numBitLevels: Int, symbol: Int): Int = {
    var price = 0
    var m = 1
    var s = symbol
    var i = numBitLevels
    while (i != 0) {
      val bit = s & 1
      s >>>= 1
      price += Encoder.getPrice(models(startIndex + m), bit)
      m = (m << 1) | bit
      i -= 1
    }
    price
  }

  def reverseEncode(models: Array[Short], startIndex: Int, rangeEncoder: Encoder, numBitLevels: Int, symbol: Int): Unit = {
    var m = 1
    var s = symbol
    for (i <- 0 until numBitLevels) {
      val bit = s & 1
      rangeEncoder.encode(models, startIndex + m, bit)
      m = (m << 1) | bit
      s >>= 1
    }
  }
}
