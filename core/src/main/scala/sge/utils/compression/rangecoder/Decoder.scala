package sge.utils.compression.rangecoder

import java.io.IOException

class Decoder {
  import Decoder._

  var Range: Int = 0
  var Code: Int = 0
  var Stream: java.io.InputStream = scala.compiletime.uninitialized

  def setStream(stream: java.io.InputStream): Unit = {
    Stream = stream
  }

  def releaseStream(): Unit = {
    Stream = null
  }

  @throws[IOException]
  def init(): Unit = {
    Code = 0
    Range = -1
    for (_ <- 0 until 5)
      Code = (Code << 8) | Stream.read()
  }

  @throws[IOException]
  def decodeDirectBits(numTotalBits: Int): Int = {
    var result = 0
    var i = numTotalBits
    while (i != 0) {
      Range >>>= 1
      val t = ((Code - Range) >>> 31)
      Code -= Range & (t - 1)
      result = (result << 1) | (1 - t)
      if ((Range & kTopMask) == 0) {
        Code = (Code << 8) | Stream.read()
        Range <<= 8
      }
      i -= 1
    }
    result
  }

  @throws[IOException]
  def decodeBit(probs: Array[Short], index: Int): Int = {
    val prob = probs(index)
    val newBound = (Range >>> kNumBitModelTotalBits) * prob
    if ((Code ^ 0x80000000) < (newBound ^ 0x80000000)) {
      Range = newBound
      probs(index) = (prob + ((kBitModelTotal - prob) >>> kNumMoveBits)).toShort
      if ((Range & kTopMask) == 0) {
        Code = (Code << 8) | Stream.read()
        Range <<= 8
      }
      0
    } else {
      Range -= newBound
      Code -= newBound
      probs(index) = (prob - (prob >>> kNumMoveBits)).toShort
      if ((Range & kTopMask) == 0) {
        Code = (Code << 8) | Stream.read()
        Range <<= 8
      }
      1
    }
  }
}

object Decoder {
  val kTopMask: Int = ~((1 << 24) - 1)
  val kNumBitModelTotalBits: Int = 11
  val kBitModelTotal: Int = 1 << kNumBitModelTotalBits
  val kNumMoveBits: Int = 5

  def initBitModels(probs: Array[Short]): Unit = {
    for (i <- probs.indices)
      probs(i) = (kBitModelTotal >>> 1).toShort
  }
}
