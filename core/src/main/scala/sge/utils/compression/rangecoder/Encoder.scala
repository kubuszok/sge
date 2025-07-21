package sge.utils.compression.rangecoder

import java.io.IOException

class Encoder {
  import Encoder._

  var Stream: java.io.OutputStream = scala.compiletime.uninitialized
  var Low: Long = 0L
  var Range: Int = 0
  var _cacheSize: Int = 1
  var _cache: Int = 0
  var _position: Long = 0L

  def setStream(stream: java.io.OutputStream): Unit = {
    Stream = stream
  }

  def releaseStream(): Unit = {
    Stream = null
  }

  def init(): Unit = {
    _position = 0L
    Low = 0L
    Range = -1
    _cacheSize = 1
    _cache = 0
  }

  @throws[IOException]
  def flushData(): Unit = {
    for (_ <- 0 until 5)
      shiftLow()
  }

  @throws[IOException]
  def flushStream(): Unit = {
    Stream.flush()
  }

  @throws[IOException]
  def shiftLow(): Unit = {
    val LowHi = (Low >>> 32).toInt
    if (LowHi != 0 || Low < 0xFF000000L) {
      _position += _cacheSize
      var temp = _cache
      while ({
        Stream.write(temp + LowHi)
        temp = 0xFF
        _cacheSize -= 1
        _cacheSize != 0
      }) ()
      _cache = ((Low.toInt) >>> 24)
    }
    _cacheSize += 1
    Low = (Low & 0xFFFFFF) << 8
  }

  @throws[IOException]
  def encodeDirectBits(v: Int, numTotalBits: Int): Unit = {
    for (i <- (numTotalBits - 1) to 0 by -1) {
      Range >>>= 1
      if (((v >>> i) & 1) == 1) Low += Range
      if ((Range & Encoder.kTopMask) == 0) {
        Range <<= 8
        shiftLow()
      }
    }
  }

  def getProcessedSizeAdd(): Long = _cacheSize + _position + 4

  @throws[IOException]
  def encode(probs: Array[Short], index: Int, symbol: Int): Unit = {
    val prob = probs(index)
    val newBound = (Range >>> kNumBitModelTotalBits) * prob
    if (symbol == 0) {
      Range = newBound
      probs(index) = (prob + ((kBitModelTotal - prob) >>> kNumMoveBits)).toShort
    } else {
      Low += (newBound & 0xFFFFFFFFL)
      Range -= newBound
      probs(index) = (prob - (prob >>> kNumMoveBits)).toShort
    }
    if ((Range & kTopMask) == 0) {
      Range <<= 8
      shiftLow()
    }
  }
}

object Encoder {
  val kTopMask: Int = ~((1 << 24) - 1)
  val kNumBitModelTotalBits: Int = 11
  val kBitModelTotal: Int = 1 << kNumBitModelTotalBits
  val kNumMoveBits: Int = 5
  val kNumMoveReducingBits: Int = 2
  val kNumBitPriceShiftBits: Int = 6

  private val ProbPrices: Array[Int] = {
    val arr = Array.ofDim[Int](kBitModelTotal >>> kNumMoveReducingBits)
    val kNumBits = kNumBitModelTotalBits - kNumMoveReducingBits
    for (i <- (kNumBits - 1) to 0 by -1) {
      val start = 1 << (kNumBits - i - 1)
      val end = 1 << (kNumBits - i)
      for (j <- start until end)
        arr(j) = (i << kNumBitPriceShiftBits) + (((end - j) << kNumBitPriceShiftBits) >>> (kNumBits - i - 1))
    }
    arr
  }

  def initBitModels(probs: Array[Short]): Unit = {
    for (i <- probs.indices)
      probs(i) = (kBitModelTotal >>> 1).toShort
  }

  def getPrice(prob: Int, symbol: Int): Int = {
    ProbPrices((((prob - symbol) ^ (-symbol)) & (kBitModelTotal - 1)) >>> kNumMoveReducingBits)
  }

  def getPrice0(prob: Int): Int = {
    ProbPrices(prob >>> kNumMoveReducingBits)
  }

  def getPrice1(prob: Int): Int = {
    ProbPrices((kBitModelTotal - prob) >>> kNumMoveReducingBits)
  }
}


