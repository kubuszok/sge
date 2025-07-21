package sge
package utils
package compression
package lzma

import java.io.IOException

import sge.utils.compression.ICodeProgress
import sge.utils.compression.rangecoder.BitTreeEncoder
import sge.utils.compression.lz.BinTree

// NOTE: Begin conversion from Java to Scala. Static members moved to companion object. Further conversion needed for methods and nested classes.
class Encoder {
  import Encoder._

  // Instance fields (converted types)
  var _state:        Int        = Base.stateInit()
  var _previousByte: Byte       = 0
  var _repDistances: Array[Int] = Array.ofDim[Int](Base.kNumRepDistances)

  // Instance methods (leave method bodies as-is for now)
  def baseInit(): Unit = {
    _state = Base.stateInit()
    _previousByte = 0
    for (i <- 0 until Base.kNumRepDistances)
      _repDistances(i) = 0
  }

  var _optimum:      Array[Optimal]                           = Array.ofDim[Optimal](Encoder.kNumOpts)
  var _matchFinder:  sge.utils.compression.lz.BinTree         = null
  var _rangeEncoder: sge.utils.compression.rangecoder.Encoder = new sge.utils.compression.rangecoder.Encoder()

  var _isMatch:    Array[Short] = Array.ofDim[Short](Base.kNumStates << Base.kNumPosStatesBitsMax)
  var _isRep:      Array[Short] = Array.ofDim[Short](Base.kNumStates)
  var _isRepG0:    Array[Short] = Array.ofDim[Short](Base.kNumStates)
  var _isRepG1:    Array[Short] = Array.ofDim[Short](Base.kNumStates)
  var _isRepG2:    Array[Short] = Array.ofDim[Short](Base.kNumStates)
  var _isRep0Long: Array[Short] = Array.ofDim[Short](Base.kNumStates << Base.kNumPosStatesBitsMax)

  var _posSlotEncoder: Array[BitTreeEncoder] = Array.ofDim[BitTreeEncoder](Base.kNumLenToPosStates) // kNumPosSlotBits

  var _posEncoders:     Array[Short]   = Array.ofDim[Short](Base.kNumFullDistances - Base.kEndPosModelIndex)
  var _posAlignEncoder: BitTreeEncoder = new BitTreeEncoder(Base.kNumAlignBits)

  var _lenEncoder:         LenPriceTableEncoder = new LenPriceTableEncoder()
  var _repMatchLenEncoder: LenPriceTableEncoder = new LenPriceTableEncoder()

  var _literalEncoder: LiteralEncoder = new LiteralEncoder()

  var _matchDistances: Array[Int] = Array.ofDim[Int](Base.kMatchMaxLen * 2 + 2)

  var _numFastBytes:       Int = Encoder.kNumFastBytesDefault
  var _longestMatchLength: Int = 0
  var _numDistancePairs:   Int = 0

  var _additionalOffset: Int = 0

  var _optimumEndIndex:     Int = 0
  var _optimumCurrentIndex: Int = 0

  var _longestMatchWasFound: Boolean = false

  var _posSlotPrices:   Array[Int] = Array.ofDim[Int](1 << (Base.kNumPosSlotBits + Base.kNumLenToPosStatesBits))
  var _distancesPrices: Array[Int] = Array.ofDim[Int](Base.kNumFullDistances << Base.kNumLenToPosStatesBits)
  var _alignPrices:     Array[Int] = Array.ofDim[Int](Base.kAlignTableSize)
  var _alignPriceCount: Int        = 0

  var _distTableSize: Int = Encoder.kDefaultDictionaryLogSize * 2

  var _posStateBits:           Int = 2
  var _posStateMask:           Int = 4 - 1
  var _numLiteralPosStateBits: Int = 0
  var _numLiteralContextBits:  Int = 3

  var _dictionarySize:     Int = 1 << Encoder.kDefaultDictionaryLogSize
  var _dictionarySizePrev: Int = -1
  var _numFastBytesPrev:   Int = -1

  var nowPos64:  Long                = 0L
  var _finished: Boolean             = false
  var _inStream: java.io.InputStream = null

  var _matchFinderType: Int     = Encoder.EMatchFinderTypeBT4
  var _writeEndMark:    Boolean = false

  var _needReleaseMFStream: Boolean = false

  val properties: Array[Byte] = Array.ofDim[Byte](Encoder.kPropSize)

  def create(): Unit = {
    if (_matchFinder == null) {
      val bt           = new sge.utils.compression.lz.BinTree()
      var numHashBytes = 4
      if (_matchFinderType == Encoder.EMatchFinderTypeBT2) numHashBytes = 2
      bt.setType(numHashBytes)
      _matchFinder = bt
    }
    _literalEncoder.create(_numLiteralPosStateBits, _numLiteralContextBits)

    if (_dictionarySize == _dictionarySizePrev && _numFastBytesPrev == _numFastBytes) return
    _matchFinder.create(_dictionarySize, Encoder.kNumOpts, _numFastBytes, Base.kMatchMaxLen + 1)
    _dictionarySizePrev = _dictionarySize
    _numFastBytesPrev = _numFastBytes
  }

  def encoder(): Unit = {
    for (i <- 0 until Encoder.kNumOpts)
      _optimum(i) = new Optimal()
    for (i <- 0 until Base.kNumLenToPosStates)
      _posSlotEncoder(i) = new BitTreeEncoder(Base.kNumPosSlotBits)
  }

  def setWriteEndMarkerMode(writeEndMarker: Boolean): Unit =
    _writeEndMark = writeEndMarker

  def init(): Unit = {
    baseInit()
    _rangeEncoder.init()

    sge.utils.compression.rangecoder.Encoder.initBitModels(_isMatch)
    sge.utils.compression.rangecoder.Encoder.initBitModels(_isRep0Long)
    sge.utils.compression.rangecoder.Encoder.initBitModels(_isRep)
    sge.utils.compression.rangecoder.Encoder.initBitModels(_isRepG0)
    sge.utils.compression.rangecoder.Encoder.initBitModels(_isRepG1)
    sge.utils.compression.rangecoder.Encoder.initBitModels(_isRepG2)
    sge.utils.compression.rangecoder.Encoder.initBitModels(_posEncoders)

    _literalEncoder.init()
    for (i <- 0 until Base.kNumLenToPosStates)
      _posSlotEncoder(i).init()

    _lenEncoder.init(1 << _posStateBits)
    _repMatchLenEncoder.init(1 << _posStateBits)

    _posAlignEncoder.init()

    _longestMatchWasFound = false
    _optimumEndIndex = 0
    _optimumCurrentIndex = 0
    _additionalOffset = 0
  }

  def readMatchDistances(): Int = {
    var lenRes = 0
    _numDistancePairs = _matchFinder.getMatches(_matchDistances)
    if (_numDistancePairs > 0) {
      lenRes = _matchDistances(_numDistancePairs - 2)
      if (lenRes == _numFastBytes) lenRes += _matchFinder.getMatchLen((lenRes - 1).toInt, _matchDistances(_numDistancePairs - 1), Base.kMatchMaxLen - lenRes)
    }
    _additionalOffset += 1
    lenRes
  }

  def movePos(num: Int): Unit =
    if (num > 0) {
      _matchFinder.skip(num)
      _additionalOffset += num
    }

  def getRepLen1Price(state: Int, posState: Int): Int =
    sge.utils.compression.rangecoder.Encoder.getPrice0(_isRepG0(state))
      + sge.utils.compression.rangecoder.Encoder.getPrice0(_isRep0Long((state << Base.kNumPosStatesBitsMax) + posState))

  def getPureRepPrice(repIndex: Int, state: Int, posState: Int): Int = {
    var price: Int = 0
    if (repIndex == 0) {
      price = sge.utils.compression.rangecoder.Encoder.getPrice0(_isRepG0(state))
      price += sge.utils.compression.rangecoder.Encoder.getPrice1(_isRep0Long((state << Base.kNumPosStatesBitsMax) + posState))
    } else {
      price = sge.utils.compression.rangecoder.Encoder.getPrice1(_isRepG0(state))
      if (repIndex == 1)
        price += sge.utils.compression.rangecoder.Encoder.getPrice0(_isRepG1(state))
      else {
        price += sge.utils.compression.rangecoder.Encoder.getPrice1(_isRepG1(state))
        price += sge.utils.compression.rangecoder.Encoder.getPrice(_isRepG2(state), repIndex - 2)
      }
    }
    price
  }

  def getRepPrice(repIndex: Int, len: Int, state: Int, posState: Int): Int = {
    val price = _repMatchLenEncoder.getPrice(len - Base.kMatchMinLen, posState)
    price + getPureRepPrice(repIndex, state, posState)
  }

  def getPosLenPrice(pos: Int, len: Int, posState: Int): Int = {
    var price: Int = 0
    val lenToPosState = Base.getLenToPosState(len)
    if (pos < Base.kNumFullDistances)
      price = _distancesPrices(lenToPosState * Base.kNumFullDistances + pos)
    else
      price = _posSlotPrices(lenToPosState << Base.kNumPosSlotBits) + Encoder.GetPosSlot2(pos) + _alignPrices(pos & Base.kAlignMask)
    price + _lenEncoder.getPrice(len - Base.kMatchMinLen, posState)
  }

  def backward(curInit: Int): Int = {
    var cur = curInit
    _optimumEndIndex = cur
    var posMem  = _optimum(cur).PosPrev
    var backMem = _optimum(cur).BackPrev
    while (cur > 0) {
      if (_optimum(cur).Prev1IsChar) {
        _optimum(posMem).makeAsChar()
        _optimum(posMem).PosPrev = posMem - 1
        if (_optimum(cur).Prev2) {
          _optimum(posMem - 1).Prev1IsChar = false
          _optimum(posMem - 1).PosPrev = _optimum(cur).PosPrev2
          _optimum(posMem - 1).BackPrev = _optimum(cur).BackPrev2
        }
      }
      val posPrev = posMem
      val backCur = backMem
      backMem = _optimum(posPrev).BackPrev
      posMem = _optimum(posPrev).PosPrev
      _optimum(posPrev).BackPrev = backCur
      _optimum(posPrev).PosPrev = cur
      cur = posPrev
    }
    backRes = _optimum(0).BackPrev
    _optimumCurrentIndex = _optimum(0).PosPrev
    cur
  }

  var reps:    Array[Int] = Array.ofDim[Int](Base.kNumRepDistances)
  var repLens: Array[Int] = Array.ofDim[Int](Base.kNumRepDistances)
  var backRes: Int        = 0

  def getOptimum(_position: Int): Int = scala.util.boundary { optimum ?=>
    var position = _position
    if (_optimumEndIndex != _optimumCurrentIndex) {
      val lenRes = _optimum(_optimumCurrentIndex).PosPrev - _optimumCurrentIndex
      backRes = _optimum(_optimumCurrentIndex).BackPrev
      _optimumCurrentIndex = _optimum(_optimumCurrentIndex).PosPrev
      scala.util.boundary.break(lenRes)
    }
    _optimumCurrentIndex = 0
    _optimumEndIndex = 0

    var lenMain:          Int = 0
    var numDistancePairs: Int = 0
    if (!_longestMatchWasFound) {
      lenMain = readMatchDistances()
    } else {
      lenMain = _longestMatchLength
      _longestMatchWasFound = false
    }
    numDistancePairs = _numDistancePairs

    var numAvailableBytes = _matchFinder.getNumAvailableBytes() + 1
    if (numAvailableBytes < 2) {
      backRes = -1
      scala.util.boundary.break(1)
    }
    if (numAvailableBytes > Base.kMatchMaxLen) numAvailableBytes = Base.kMatchMaxLen

    var repMaxIndex = 0
    var i: Int = 0
    for (i <- 0 until Base.kNumRepDistances) {
      reps(i) = _repDistances(i)
      repLens(i) = _matchFinder.getMatchLen(0 - 1, reps(i), Base.kMatchMaxLen)
      if (repLens(i) > repLens(repMaxIndex)) repMaxIndex = i
    }
    if (repLens(repMaxIndex) >= _numFastBytes) {
      backRes = repMaxIndex
      val lenRes = repLens(repMaxIndex)
      movePos(lenRes - 1)
      scala.util.boundary.break(lenRes)
    }

    if (lenMain >= _numFastBytes) {
      backRes = _matchDistances(numDistancePairs - 1) + Base.kNumRepDistances
      movePos(lenMain - 1)
      scala.util.boundary.break(lenMain)
    }

    val currentByte = _matchFinder.getIndexByte(0 - 1)
    val matchByte   = _matchFinder.getIndexByte(0 - reps(0) - 1 - 1)

    if (lenMain < 2 && currentByte != matchByte && repLens(repMaxIndex) < 2) {
      backRes = -1
      scala.util.boundary.break(1)
    }

    _optimum(0).State = _state

    val posState = position & _posStateMask

    _optimum(1).Price = sge.utils.compression.rangecoder.Encoder.getPrice0(_isMatch((_state << Base.kNumPosStatesBitsMax) + posState))
      + _literalEncoder.getSubCoder(position, _previousByte).getPrice(!Base.stateIsCharState(_state), matchByte, currentByte)
    _optimum(1).makeAsChar()

    val matchPrice    = sge.utils.compression.rangecoder.Encoder.getPrice1(_isMatch((_state << Base.kNumPosStatesBitsMax) + posState))
    val repMatchPrice = matchPrice + sge.utils.compression.rangecoder.Encoder.getPrice1(_isRep(_state))

    if (matchByte == currentByte) {
      val shortRepPrice = repMatchPrice + getRepLen1Price(_state, posState)
      if (shortRepPrice < _optimum(1).Price) {
        _optimum(1).Price = shortRepPrice
        _optimum(1).makeAsShortRep()
      }
    }

    val lenEnd = if (lenMain >= repLens(repMaxIndex)) lenMain else repLens(repMaxIndex)

    if (lenEnd < 2) {
      backRes = _optimum(1).BackPrev
      scala.util.boundary.break(1)
    }

    _optimum(1).PosPrev = 0

    _optimum(0).Backs0 = reps(0)
    _optimum(0).Backs1 = reps(1)
    _optimum(0).Backs2 = reps(2)
    _optimum(0).Backs3 = reps(3)

    var l = lenEnd
    while (l >= 2) {
      _optimum(l).Price = Encoder.kIfinityPrice
      l -= 1
    }

    for (i <- 0 until Base.kNumRepDistances) {
      val repLen = repLens(i)
      if (repLen >= 2) {
        val price = repMatchPrice + getPureRepPrice(i, _state, posState)
        var rl    = repLen
        while (rl >= 2) {
          val curAndLenPrice = price + _repMatchLenEncoder.getPrice(rl - 2, posState)
          val optimum        = _optimum(rl)
          if (curAndLenPrice < optimum.Price) {
            optimum.Price = curAndLenPrice
            optimum.PosPrev = 0
            optimum.BackPrev = i
            optimum.Prev1IsChar = false
          }
          rl -= 1
        }
      }
    }

    val normalMatchPrice = matchPrice + sge.utils.compression.rangecoder.Encoder.getPrice0(_isRep(_state))

    var len = if (repLens(0) >= 2) repLens(0) + 1 else 2
    if (len <= lenMain) {
      var offs = 0
      while (len > _matchDistances(offs))
        offs += 2
      while (len <= lenMain) {
        val distance       = _matchDistances(offs + 1)
        val curAndLenPrice = normalMatchPrice + getPosLenPrice(distance, len, posState)
        val optimum        = _optimum(len)
        if (curAndLenPrice < optimum.Price) {
          optimum.Price = curAndLenPrice
          optimum.PosPrev = 0
          optimum.BackPrev = distance + Base.kNumRepDistances
          optimum.Prev1IsChar = false
        }
        if (len == _matchDistances(offs)) {
          offs += 2
          if (offs == numDistancePairs) scala.util.boundary.break(0)
        }
        len += 1
      }
    }

    var cur: Int = 0

    while (true) scala.util.boundary[Unit] { loop ?=>
      cur += 1
      if (cur == lenEnd) scala.util.boundary.break(backward(cur))
      var newLen = readMatchDistances()
      numDistancePairs = _numDistancePairs
      if (newLen >= _numFastBytes) {

        _longestMatchLength = newLen
        _longestMatchWasFound = true
        scala.util.boundary.break(backward(cur))(using optimum)
      }
      position += 1
      var posPrev = _optimum(cur).PosPrev
      var state: Int = 0
      if (_optimum(cur).Prev1IsChar) {
        posPrev -= 1
        if (_optimum(cur).Prev2) {
          state = _optimum(_optimum(cur).PosPrev2).State
          if (_optimum(cur).BackPrev2 < Base.kNumRepDistances)
            state = Base.stateUpdateRep(state)
          else
            state = Base.stateUpdateMatch(state)
        } else
          state = _optimum(posPrev).State
        state = Base.stateUpdateChar(state)
      } else
        state = _optimum(posPrev).State
      if (posPrev == cur - 1) {
        if (_optimum(cur).isShortRep())
          state = Base.stateUpdateShortRep(state)
        else
          state = Base.stateUpdateChar(state)
      } else {
        var pos: Int = 0
        if (_optimum(cur).Prev1IsChar && _optimum(cur).Prev2) {
          posPrev = _optimum(cur).PosPrev2
          pos = _optimum(cur).BackPrev2
          state = Base.stateUpdateRep(state)
        } else {
          pos = _optimum(cur).BackPrev
          if (pos < Base.kNumRepDistances)
            state = Base.stateUpdateRep(state)
          else
            state = Base.stateUpdateMatch(state)
        }
        val opt = _optimum(posPrev)
        if (pos < Base.kNumRepDistances) {
          if (pos == 0) {
            reps(0) = opt.Backs0
            reps(1) = opt.Backs1
            reps(2) = opt.Backs2
            reps(3) = opt.Backs3
          } else if (pos == 1) {
            reps(0) = opt.Backs1
            reps(1) = opt.Backs0
            reps(2) = opt.Backs2
            reps(3) = opt.Backs3
          } else if (pos == 2) {
            reps(0) = opt.Backs2
            reps(1) = opt.Backs0
            reps(2) = opt.Backs1
            reps(3) = opt.Backs3
          } else {
            reps(0) = opt.Backs3
            reps(1) = opt.Backs0
            reps(2) = opt.Backs1
            reps(3) = opt.Backs2
          }
        } else {
          reps(0) = pos - Base.kNumRepDistances
          reps(1) = opt.Backs0
          reps(2) = opt.Backs1
          reps(3) = opt.Backs2
        }
      }
      _optimum(cur).State = state
      _optimum(cur).Backs0 = reps(0)
      _optimum(cur).Backs1 = reps(1)
      _optimum(cur).Backs2 = reps(2)
      _optimum(cur).Backs3 = reps(3)
      val curPrice = _optimum(cur).Price

      val currentByte = _matchFinder.getIndexByte(0 - 1)
      val matchByte   = _matchFinder.getIndexByte(0 - reps(0) - 1 - 1)

      val posState = position & _posStateMask

      val curAnd1Price = curPrice
        + sge.utils.compression.rangecoder.Encoder.getPrice0(_isMatch((state << Base.kNumPosStatesBitsMax) + posState))
        + _literalEncoder.getSubCoder(position, _matchFinder.getIndexByte(0 - 2)).getPrice(!Base.stateIsCharState(state), matchByte, currentByte)

      val nextOptimum = _optimum(cur + 1)

      var nextIsChar: Boolean = false
      if (curAnd1Price < nextOptimum.Price) {
        nextOptimum.Price = curAnd1Price
        nextOptimum.PosPrev = cur
        nextOptimum.makeAsChar()
        nextIsChar = true
      }

      val matchPrice    = curPrice + sge.utils.compression.rangecoder.Encoder.getPrice1(_isMatch((state << Base.kNumPosStatesBitsMax) + posState))
      val repMatchPrice = matchPrice + sge.utils.compression.rangecoder.Encoder.getPrice1(_isRep(state))

      if (matchByte == currentByte && !(nextOptimum.PosPrev < cur && nextOptimum.BackPrev == 0)) {
        val shortRepPrice = repMatchPrice + getRepLen1Price(state, posState)
        if (shortRepPrice <= nextOptimum.Price) {
          nextOptimum.Price = shortRepPrice
          nextOptimum.PosPrev = cur
          nextOptimum.makeAsShortRep()
          nextIsChar = true
        }
      }

      var numAvailableBytesFull = _matchFinder.getNumAvailableBytes() + 1
      var numAvailableBytes     = Math.min(Encoder.kNumOpts - 1 - cur, numAvailableBytesFull)

      if (numAvailableBytes < 2) scala.util.boundary.break()(using loop)
      if (numAvailableBytes > _numFastBytes) numAvailableBytes = _numFastBytes
      if (!nextIsChar && matchByte != currentByte) {
        // try Literal + rep0
        val t        = Math.min(numAvailableBytesFull - 1, _numFastBytes)
        val lenTest2 = _matchFinder.getMatchLen(0, reps(0), t)
        if (lenTest2 >= 2) {
          val state2 = Base.stateUpdateChar(state)

          val posStateNext = (position + 1) & _posStateMask
          val nextRepMatchPrice = curAnd1Price
            + sge.utils.compression.rangecoder.Encoder.getPrice1(_isMatch((state2 << Base.kNumPosStatesBitsMax) + posStateNext))
            + sge.utils.compression.rangecoder.Encoder.getPrice1(_isRep(state2))
          locally {
            val offset = cur + 1 + lenTest2
            var le     = lenEnd
            while (le < offset) {
              le += 1
              _optimum(le).Price = Encoder.kIfinityPrice
            }
            val curAndLenPrice = nextRepMatchPrice + getRepPrice(0, lenTest2, state2, posStateNext)
            val optimum        = _optimum(offset)
            if (curAndLenPrice < optimum.Price) {
              optimum.Price = curAndLenPrice
              optimum.PosPrev = cur + 1
              optimum.BackPrev = 0
              optimum.Prev1IsChar = true
              optimum.Prev2 = false
            }
          }
        }
      }

      var startLen: Int = 2 // speed optimization

      for (repIndex <- 0 until Base.kNumRepDistances) scala.util.boundary { forLoop ?=>
        var lenTest = _matchFinder.getMatchLen(0 - 1, reps(repIndex), numAvailableBytes)
        if (lenTest < 2) scala.util.boundary.break()(using forLoop)
        val lenTestTemp = lenTest
        var lt          = lenTest
        while (lt >= 2) {
          var le = lenEnd
          while (le < cur + lt) {
            le += 1
            _optimum(le).Price = Encoder.kIfinityPrice
          }
          val curAndLenPrice = repMatchPrice + getRepPrice(repIndex, lt, state, posState)
          val optimum        = _optimum(cur + lt)
          if (curAndLenPrice < optimum.Price) {
            optimum.Price = curAndLenPrice
            optimum.PosPrev = cur
            optimum.BackPrev = repIndex
            optimum.Prev1IsChar = false
          }
          lt -= 1
        }
        lenTest = lenTestTemp

        if (repIndex == 0) startLen = lenTest + 1

        // if (_maxMode)
        if (lenTest < numAvailableBytesFull) {
          val t        = Math.min(numAvailableBytesFull - 1 - lenTest, _numFastBytes)
          val lenTest2 = _matchFinder.getMatchLen(lenTest, reps(repIndex), t)
          if (lenTest2 >= 2) {
            var state2 = Base.stateUpdateRep(state)

            var posStateNext = (position + lenTest) & _posStateMask
            val curAndLenCharPrice = repMatchPrice + getRepPrice(repIndex, lenTest, state, posState)
              + sge.utils.compression.rangecoder.Encoder.getPrice0(_isMatch((state2 << Base.kNumPosStatesBitsMax) + posStateNext))
              + _literalEncoder
                .getSubCoder(position + lenTest, _matchFinder.getIndexByte(lenTest - 1 - 1))
                .getPrice(true, _matchFinder.getIndexByte(lenTest - (reps(repIndex) + 1)), _matchFinder.getIndexByte(lenTest - 1))
            state2 = Base.stateUpdateChar(state2)
            posStateNext = (position + lenTest + 1) & _posStateMask
            val nextMatchPrice = curAndLenCharPrice + sge.utils.compression.rangecoder.Encoder.getPrice1(_isMatch((state2 << Base.kNumPosStatesBitsMax) + posStateNext))
            val nextRepMatchPrice = nextMatchPrice
              + sge.utils.compression.rangecoder.Encoder.getPrice1(_isRep(state2))

            // for(; lenTest2 >= 2; lenTest2--)
            {
              val offset = lenTest + 1 + lenTest2
              var le     = lenEnd
              while (le < cur + offset) {
                le += 1
                _optimum(le).Price = Encoder.kIfinityPrice
              }
              val curAndLenPrice = nextRepMatchPrice + getRepPrice(0, lenTest2, state2, posStateNext)
              val optimum        = _optimum(cur + offset)
              if (curAndLenPrice < optimum.Price) {
                optimum.Price = curAndLenPrice
                optimum.PosPrev = cur + lenTest + 1
                optimum.BackPrev = 0
                optimum.Prev1IsChar = true
                optimum.Prev2 = true
                optimum.PosPrev2 = cur
                optimum.BackPrev2 = repIndex
              }
            }
          }
        }
      }

      if (newLen > numAvailableBytes) {
        newLen = numAvailableBytes
        var ndp = 0
        while (newLen > _matchDistances(ndp))
          ndp += 2
        _matchDistances(ndp) = newLen
        ndp += 2
      }
      if (newLen >= startLen) {
        val normalMatchPrice = matchPrice + sge.utils.compression.rangecoder.Encoder.getPrice0(_isRep(state))
        var l                = lenEnd
        while (l < cur + newLen) {
          l += 1
          _optimum(l).Price = Encoder.kIfinityPrice
        }

        var offs: Int = 0
        while (startLen > _matchDistances(offs))
          offs += 2

        while (startLen <= lenMain) {
          val curBack        = _matchDistances(offs + 1)
          var curAndLenPrice = normalMatchPrice + getPosLenPrice(curBack, startLen, posState)
          val optimum        = _optimum(cur + startLen)
          if (curAndLenPrice < optimum.Price) {
            optimum.Price = curAndLenPrice
            optimum.PosPrev = cur
            optimum.BackPrev = curBack + Base.kNumRepDistances
            optimum.Prev1IsChar = false
          }

          if (startLen == _matchDistances(offs)) {
            if (startLen < numAvailableBytesFull) {
              val t        = Math.min(numAvailableBytesFull - 1 - startLen, _numFastBytes)
              val lenTest2 = _matchFinder.getMatchLen(startLen, curBack, t)
              if (lenTest2 >= 2) {
                var state2 = Base.stateUpdateMatch(state)

                var posStateNext = (position + startLen) & _posStateMask
                val curAndLenCharPrice: Int = curAndLenPrice
                  + sge.utils.compression.rangecoder.Encoder.getPrice0(_isMatch((state2 << Base.kNumPosStatesBitsMax) + posStateNext))
                  + _literalEncoder
                    .getSubCoder(position + startLen, _matchFinder.getIndexByte(startLen - 1 - 1))
                    .getPrice(true, _matchFinder.getIndexByte(startLen - (curBack + 1) - 1), _matchFinder.getIndexByte(startLen - 1))
                state2 = Base.stateUpdateChar(state2)
                posStateNext = (position + startLen + 1) & _posStateMask
                val nextMatchPrice = curAndLenCharPrice + sge.utils.compression.rangecoder.Encoder.getPrice1(_isMatch((state2 << Base.kNumPosStatesBitsMax) + posStateNext))
                val nextRepMatchPrice = nextMatchPrice
                  + sge.utils.compression.rangecoder.Encoder.getPrice1(_isRep(state2))

                val offset = startLen + 1 + lenTest2
                var le     = lenEnd
                while (le < cur + offset) {
                  le += 1
                  _optimum(le).Price = Encoder.kIfinityPrice
                }
                curAndLenPrice = nextRepMatchPrice + getRepPrice(0, lenTest2, state2, posStateNext)
                val optimum = _optimum(cur + offset)
                if (curAndLenPrice < optimum.Price) {
                  optimum.Price = curAndLenPrice
                  optimum.PosPrev = cur + startLen + 1
                  optimum.BackPrev = 0
                  optimum.Prev1IsChar = true
                  optimum.Prev2 = true
                  optimum.PosPrev2 = cur
                  optimum.BackPrev2 = curBack + Base.kNumRepDistances
                }
              }
            }
            offs += 2
            if (offs == numDistancePairs) scala.util.boundary.break(0)
          }
          startLen += 1
        }
      }
    }
    ??? // should never happen
  }

  def changePair(smallDist: Int, bigDist: Int): Boolean = {
    val kDif = 7
    smallDist < (1 << (32 - kDif)) && bigDist >= (smallDist << kDif)
  }

  def writeEndMarker(posState: Int): Unit = {
    if (!_writeEndMark) return

    _rangeEncoder.encode(_isMatch, (_state << Base.kNumPosStatesBitsMax) + posState, 1)
    _rangeEncoder.encode(_isRep, _state, 0)
    _state = Base.stateUpdateMatch(_state)
    val len = Base.kMatchMinLen
    _lenEncoder.encode(_rangeEncoder, len - Base.kMatchMinLen, posState)
    val posSlot       = (1 << Base.kNumPosSlotBits) - 1
    val lenToPosState = Base.getLenToPosState(len)
    _posSlotEncoder(lenToPosState).encode(_rangeEncoder, posSlot)
    val footerBits = 30
    val posReduced = (1 << footerBits) - 1
    _rangeEncoder.encodeDirectBits(posReduced >> Base.kNumAlignBits, footerBits - Base.kNumAlignBits)
    _posAlignEncoder.reverseEncode(_rangeEncoder, posReduced & Base.kAlignMask)
  }

  def flush(nowPos: Int): Unit = {
    ReleaseMFStream()
    writeEndMarker(nowPos & _posStateMask)
    _rangeEncoder.flushData()
    _rangeEncoder.flushStream()
  }

  def codeOneBlock(inSize: Array[Long], outSize: Array[Long], finished: Array[Boolean]): Unit = {
    inSize(0) = 0
    outSize(0) = 0
    finished(0) = true

    if (_inStream != null) {
      _matchFinder.setStream(_inStream)
      _matchFinder.init()
      _needReleaseMFStream = true
      _inStream = null
    }

    if (_finished) return
    _finished = true

    val progressPosValuePrev = nowPos64
    if (nowPos64 == 0) {
      if (_matchFinder.getNumAvailableBytes() == 0) {
        flush(nowPos64.toInt)
        return
      }

      readMatchDistances()
      val posState = (nowPos64.toInt) & _posStateMask
      _rangeEncoder.encode(_isMatch, (_state << Base.kNumPosStatesBitsMax) + posState, 0)
      _state = Base.stateUpdateChar(_state)
      val curByte = _matchFinder.getIndexByte(0 - _additionalOffset)
      _literalEncoder.getSubCoder(nowPos64.toInt, _previousByte).encode(_rangeEncoder, curByte)
      _previousByte = curByte
      _additionalOffset -= 1
      nowPos64 += 1
    }
    if (_matchFinder.getNumAvailableBytes() == 0) {
      flush(nowPos64.toInt)
      return
    }
    while (true) {

      val len          = getOptimum(nowPos64.toInt)
      var pos          = backRes
      val posState     = (nowPos64.toInt) & _posStateMask
      val complexState = (_state << Base.kNumPosStatesBitsMax) + posState
      if (len == 1 && pos == -1) {
        _rangeEncoder.encode(_isMatch, complexState, 0)
        val curByte  = _matchFinder.getIndexByte(0 - _additionalOffset)
        val subCoder = _literalEncoder.getSubCoder(nowPos64.toInt, _previousByte)
        if (!Base.stateIsCharState(_state)) {
          val matchByte = _matchFinder.getIndexByte(0 - _repDistances(0) - 1 - _additionalOffset)
          subCoder.encodeMatched(_rangeEncoder, matchByte, curByte)
        } else
          subCoder.encode(_rangeEncoder, curByte)
        _previousByte = curByte
        _state = Base.stateUpdateChar(_state)
      } else {
        _rangeEncoder.encode(_isMatch, complexState, 1)
        if (pos < Base.kNumRepDistances) {
          _rangeEncoder.encode(_isRep, _state, 1)
          if (pos == 0) {
            _rangeEncoder.encode(_isRepG0, _state, 0)
            if (len == 1)
              _rangeEncoder.encode(_isRep0Long, complexState, 0)
            else
              _rangeEncoder.encode(_isRep0Long, complexState, 1)
          } else {
            _rangeEncoder.encode(_isRepG0, _state, 1)
            if (pos == 1)
              _rangeEncoder.encode(_isRepG1, _state, 0)
            else {
              _rangeEncoder.encode(_isRepG1, _state, 1)
              _rangeEncoder.encode(_isRepG2, _state, pos - 2)
            }
          }
          if (len == 1)
            _state = Base.stateUpdateShortRep(_state)
          else {
            _repMatchLenEncoder.encode(_rangeEncoder, len - Base.kMatchMinLen, posState)
            _state = Base.stateUpdateRep(_state)
          }
          val distance = _repDistances(pos)
          if (pos != 0) {
            for (i <- pos until 1 by -1)
              _repDistances(i) = _repDistances(i - 1)
            _repDistances(0) = distance
          }
        } else {
          _rangeEncoder.encode(_isRep, _state, 0)
          _state = Base.stateUpdateMatch(_state)
          _lenEncoder.encode(_rangeEncoder, len - Base.kMatchMinLen, posState)
          pos -= Base.kNumRepDistances
          val posSlot       = Encoder.GetPosSlot(pos)
          val lenToPosState = Base.getLenToPosState(len)
          _posSlotEncoder(lenToPosState).encode(_rangeEncoder, posSlot)

          if (posSlot >= Base.kStartPosModelIndex) {
            val footerBits = (posSlot >> 1) - 1
            val baseVal    = (2 | (posSlot & 1)) << footerBits
            val posReduced = pos - baseVal

            if (posSlot < Base.kEndPosModelIndex)
              BitTreeEncoder.reverseEncode(_posEncoders, baseVal - posSlot - 1, _rangeEncoder, footerBits, posReduced)
            else {
              _rangeEncoder.encodeDirectBits(posReduced >> Base.kNumAlignBits, footerBits - Base.kNumAlignBits)
              _posAlignEncoder.reverseEncode(_rangeEncoder, posReduced & Base.kAlignMask)
              _alignPriceCount += 1
            }
          }
          val distance = pos
          for (i <- Base.kNumRepDistances - 1 to 1 by -1)
            _repDistances(i) = _repDistances(i - 1)
          _repDistances(0) = distance
          _matchPriceCount += 1
        }
        _previousByte = _matchFinder.getIndexByte(len - 1 - _additionalOffset)
      }
      _additionalOffset -= len
      nowPos64 += len
      if (_additionalOffset == 0) {
        // if (!_fastMode)
        if (_matchPriceCount >= (1 << 7)) FillDistancesPrices()
        if (_alignPriceCount >= Base.kAlignTableSize) FillAlignPrices()
        inSize(0) = nowPos64
        outSize(0) = _rangeEncoder.getProcessedSizeAdd()
        if (_matchFinder.getNumAvailableBytes() == 0) {
          flush(nowPos64.toInt)
          return
        }

        if (nowPos64 - progressPosValuePrev >= (1 << 12)) {
          _finished = false
          finished(0) = false
          return
        }
      }
    }
  }

  def ReleaseMFStream(): Unit =
    if (_matchFinder != null && _needReleaseMFStream) {
      _matchFinder.releaseStream()
      _needReleaseMFStream = false
    }

  def SetOutStream(outStream: java.io.OutputStream): Unit =
    _rangeEncoder.setStream(outStream)

  def ReleaseOutStream(): Unit =
    _rangeEncoder.releaseStream()

  def ReleaseStreams(): Unit = {
    ReleaseMFStream()
    ReleaseOutStream()
  }

  def SetStreams(inStream: java.io.InputStream, outStream: java.io.OutputStream, inSize: Long, outSize: Long): Unit = {
    _inStream = inStream
    _finished = false
    create()
    SetOutStream(outStream)
    init()

    // if (!_fastMode)
    {
      FillDistancesPrices()
      FillAlignPrices()
    }

    _lenEncoder.setTableSize(_numFastBytes + 1 - Base.kMatchMinLen)
    _lenEncoder.updateTables(1 << _posStateBits)
    _repMatchLenEncoder.setTableSize(_numFastBytes + 1 - Base.kMatchMinLen)
    _repMatchLenEncoder.updateTables(1 << _posStateBits)

    nowPos64 = 0
  }

  val processedInSize:  Array[Long]    = Array.ofDim[Long](1)
  val processedOutSize: Array[Long]    = Array.ofDim[Long](1)
  val finished:         Array[Boolean] = Array.ofDim[Boolean](1)

  def Code(inStream: java.io.InputStream, outStream: java.io.OutputStream, inSize: Long, outSize: Long, progress: sge.utils.compression.ICodeProgress): Unit = {
    _needReleaseMFStream = false
    try {
      SetStreams(inStream, outStream, inSize, outSize)
      while (true) {

        codeOneBlock(processedInSize, processedOutSize, finished)
        if (finished(0)) return
        if (progress != null) {
          progress.SetProgress(processedInSize(0), processedOutSize(0))
        }
      }
    } finally
      ReleaseStreams()
  }

  def WriteCoderProperties(outStream: java.io.OutputStream): Unit = {
    properties(0) = (properties(0) | ((_posStateBits * 5 + _numLiteralPosStateBits) * 9 + _numLiteralContextBits)).toByte
    for (i <- 0 until 4)
      properties(1 + i) = (properties(1 + i) | (_dictionarySize >> (8 * i))).toByte
    outStream.write(properties, 0, Encoder.kPropSize)
  }

  val tempPrices:       Array[Int] = Array.ofDim[Int](Base.kNumFullDistances)
  var _matchPriceCount: Int        = 0

  def FillDistancesPrices(): Unit = {
    for (i <- Base.kStartPosModelIndex until Base.kNumFullDistances) {
      val posSlot    = Encoder.GetPosSlot(i)
      val footerBits = (posSlot >> 1) - 1
      val baseVal    = (2 | (posSlot & 1)) << footerBits
      tempPrices(i) = BitTreeEncoder.reverseGetPrice(_posEncoders, baseVal - posSlot - 1, footerBits, i - baseVal)
    }

    for (lenToPosState <- 0 until Base.kNumLenToPosStates) {
      var posSlot: Int = 0
      val encoder = _posSlotEncoder(lenToPosState)

      val st = lenToPosState << Base.kNumPosSlotBits
      for (posSlot <- 0 until _distTableSize)
        _posSlotPrices(st + posSlot) = encoder.getPrice(posSlot)
      for (posSlot <- Base.kEndPosModelIndex until _distTableSize)
        _posSlotPrices(st + posSlot) += ((((posSlot >> 1) - 1)
          - Base.kNumAlignBits) << sge.utils.compression.rangecoder.Encoder.kNumBitPriceShiftBits)

      val st2 = lenToPosState * Base.kNumFullDistances
      var i: Int = 0
      for (i <- 0 until Base.kStartPosModelIndex)
        _distancesPrices(st2 + i) = _posSlotPrices(st + i)
      for (i <- Base.kStartPosModelIndex until Base.kNumFullDistances)
        _distancesPrices(st2 + i) = _posSlotPrices(st + Encoder.GetPosSlot(i)) + tempPrices(i)
    }
    _matchPriceCount = 0
  }

  def FillAlignPrices(): Unit = {
    for (i <- 0 until Base.kAlignTableSize)
      _alignPrices(i) = _posAlignEncoder.reverseGetPrice(i)
    _alignPriceCount = 0
  }

  def SetAlgorithm(algorithm: Int): Boolean =
    /*
     * _fastMode = (algorithm == 0); _maxMode = (algorithm >= 2);
     */
    true

  def SetDictionarySize(dictionarySize: Int): Boolean = {
    val kDicLogSizeMaxCompress = 29
    if (dictionarySize < (1 << Base.kDicLogSizeMin) || dictionarySize > (1 << kDicLogSizeMaxCompress)) return false
    _dictionarySize = dictionarySize
    var dicLogSize: Int = 0
    while (dictionarySize > (1 << dicLogSize))
      dicLogSize += 1
    _distTableSize = dicLogSize * 2
    true
  }

  def SetNumFastBytes(numFastBytes: Int): Boolean = {
    if (numFastBytes < 5 || numFastBytes > Base.kMatchMaxLen) return false
    _numFastBytes = numFastBytes
    true
  }

  def SetMatchFinder(matchFinderIndex: Int): Boolean = {
    if (matchFinderIndex < 0 || matchFinderIndex > 2) return false
    val matchFinderIndexPrev = _matchFinderType
    _matchFinderType = matchFinderIndex
    if (_matchFinder != null && matchFinderIndexPrev != _matchFinderType) {
      _dictionarySizePrev = -1
      _matchFinder = null
    }
    true
  }

  def SetLcLpPb(lc: Int, lp: Int, pb: Int): Boolean = {
    if (
      lp < 0 || lp > Base.kNumLitPosStatesBitsEncodingMax || lc < 0 || lc > Base.kNumLitContextBitsMax || pb < 0
      || pb > Base.kNumPosStatesBitsEncodingMax
    ) return false
    _numLiteralPosStateBits = lp
    _numLiteralContextBits = lc
    _posStateBits = pb
    _posStateMask = (1 << _posStateBits) - 1
    true
  }

  def SetEndMarkerMode(endMarkerMode: Boolean): Unit =
    _writeEndMark = endMarkerMode
}

object Encoder {
  // Static members (converted types)
  final val EMatchFinderTypeBT2: Int = 0
  final val EMatchFinderTypeBT4: Int = 1
  final val kIfinityPrice:       Int = 0xfffffff
  val g_FastPos: Array[Byte] = {
    val arr        = Array.ofDim[Byte](1 << 11)
    val kFastSlots = 22
    var c          = 2
    arr(0) = 0
    arr(1) = 1
    for (slotFast <- 2 until kFastSlots) {
      val k = 1 << ((slotFast >> 1) - 1)
      for (j <- 0 until k) {
        arr(c) = slotFast.toByte
        c += 1
      }
    }
    arr
  }

  def GetPosSlot(pos: Int): Int = {
    if (pos < (1 << 11)) return g_FastPos(pos)
    if (pos < (1 << 21)) return g_FastPos(pos >> 10) + 20
    g_FastPos(pos >> 20) + 40
  }

  def GetPosSlot2(pos: Int): Int = {
    if (pos < (1 << 17)) return g_FastPos(pos >> 6) + 12
    if (pos < (1 << 27)) return g_FastPos(pos >> 16) + 32
    g_FastPos(pos >> 26) + 52
  }

  final val kDefaultDictionaryLogSize: Int = 22
  final val kNumFastBytesDefault:      Int = 0x20
  final val kNumLenSpecSymbols:        Int = Base.kNumLowLenSymbols + Base.kNumMidLenSymbols
  final val kNumOpts:                  Int = 1 << 12
  final val kPropSize:                 Int = 5

  class LiteralEncoder {
    class Encoder2 {
      var m_Encoders: Array[Short] = Array.ofDim[Short](0x300)

      def init(): Unit =
        sge.utils.compression.rangecoder.Encoder.initBitModels(m_Encoders)

      def encode(rangeEncoder: sge.utils.compression.rangecoder.Encoder, symbol: Byte): Unit = {
        var context = 1
        for (i <- 7 to 0 by -1) {
          val bit = (symbol >> i) & 1
          rangeEncoder.encode(m_Encoders, context, bit)
          context = (context << 1) | bit
        }
      }

      def encodeMatched(rangeEncoder: sge.utils.compression.rangecoder.Encoder, matchByte: Byte, symbol: Byte): Unit = {
        var context = 1
        var same    = true
        for (i <- 7 to 0 by -1) {
          val bit   = (symbol >> i) & 1
          var state = context
          if (same) {
            val matchBit = (matchByte >> i) & 1
            state += ((1 + matchBit) << 8)
            same = matchBit == bit
          }
          rangeEncoder.encode(m_Encoders, state, bit)
          context = (context << 1) | bit
        }
      }

      def getPrice(matchMode: Boolean, matchByte: Byte, symbol: Byte): Int = {
        var price   = 0
        var context = 1
        var i       = 7
        if (matchMode) {
          var done = false
          while (i >= 0 && !done) {
            val matchBit = (matchByte >> i) & 1
            val bit      = (symbol >> i) & 1
            price += sge.utils.compression.rangecoder.Encoder.getPrice(m_Encoders(((1 + matchBit) << 8) + context), bit)
            context = (context << 1) | bit
            if (matchBit != bit) {
              done = true
            } else {
              i -= 1
            }
          }
        }
        while (i >= 0) {
          val bit = (symbol >> i) & 1
          price += sge.utils.compression.rangecoder.Encoder.getPrice(m_Encoders(context), bit)
          context = (context << 1) | bit
          i -= 1
        }
        price
      }
    }

    var m_Coders:      Array[Encoder2] = null
    var m_NumPrevBits: Int             = 0
    var m_NumPosBits:  Int             = 0
    var m_PosMask:     Int             = 0

    def create(numPosBits: Int, numPrevBits: Int): Unit = {
      if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits) return
      m_NumPosBits = numPosBits
      m_PosMask = (1 << numPosBits) - 1
      m_NumPrevBits = numPrevBits
      val numStates = 1 << (m_NumPrevBits + m_NumPosBits)
      m_Coders = Array.ofDim[Encoder2](numStates)
      for (i <- 0 until numStates)
        m_Coders(i) = new Encoder2()
    }

    def init(): Unit = {
      val numStates = 1 << (m_NumPrevBits + m_NumPosBits)
      for (i <- 0 until numStates)
        m_Coders(i).init()
    }

    def getSubCoder(pos: Int, prevByte: Byte): Encoder2 =
      m_Coders((pos & m_PosMask) << m_NumPrevBits + ((prevByte & 0xff) >>> (8 - m_NumPrevBits)))
  }

  class LenEncoder {
    var _choice:    Array[Short]          = Array.ofDim[Short](2)
    var _lowCoder:  Array[BitTreeEncoder] = Array.ofDim[BitTreeEncoder](Base.kNumPosStatesEncodingMax)
    var _midCoder:  Array[BitTreeEncoder] = Array.ofDim[BitTreeEncoder](Base.kNumPosStatesEncodingMax)
    var _highCoder: BitTreeEncoder        = new BitTreeEncoder(Base.kNumHighLenBits)

    // Move initialization logic into the class body
    for (posState <- 0 until Base.kNumPosStatesEncodingMax) {
      _lowCoder(posState) = new BitTreeEncoder(Base.kNumLowLenBits)
      _midCoder(posState) = new BitTreeEncoder(Base.kNumMidLenBits)
    }

    def init(numPosStates: Int): Unit = {
      sge.utils.compression.rangecoder.Encoder.initBitModels(_choice)
      for (posState <- 0 until numPosStates) {
        _lowCoder(posState).init()
        _midCoder(posState).init()
      }
      _highCoder.init()
    }

    def encode(rangeEncoder: sge.utils.compression.rangecoder.Encoder, symbol: Int, posState: Int): Unit =
      if (symbol < Base.kNumLowLenSymbols) {
        rangeEncoder.encode(_choice, 0, 0)
        _lowCoder(posState).encode(rangeEncoder, symbol)
      } else {
        val symbolAdj = symbol - Base.kNumLowLenSymbols
        rangeEncoder.encode(_choice, 0, 1)
        if (symbolAdj < Base.kNumMidLenSymbols) {
          rangeEncoder.encode(_choice, 1, 0)
          _midCoder(posState).encode(rangeEncoder, symbolAdj)
        } else {
          rangeEncoder.encode(_choice, 1, 1)
          _highCoder.encode(rangeEncoder, symbolAdj - Base.kNumMidLenSymbols)
        }
      }

    def setPrices(posState: Int, numSymbols: Int, prices: Array[Int], st: Int): Unit = scala.util.boundary {
      val a0 = sge.utils.compression.rangecoder.Encoder.getPrice0(_choice(0))
      val a1 = sge.utils.compression.rangecoder.Encoder.getPrice1(_choice(0))
      val b0 = a1 + sge.utils.compression.rangecoder.Encoder.getPrice0(_choice(1))
      val b1 = a1 + sge.utils.compression.rangecoder.Encoder.getPrice1(_choice(1))
      var i  = 0
      for (i <- 0 until Base.kNumLowLenSymbols) {
        if (i >= numSymbols) scala.util.boundary.break()
        prices(st + i) = a0 + _lowCoder(posState).getPrice(i)
      }
      val midStart = Base.kNumLowLenSymbols
      val midEnd   = Base.kNumLowLenSymbols + Base.kNumMidLenSymbols
      for (j <- midStart until midEnd) {
        if (j >= numSymbols) scala.util.boundary.break()
        prices(st + j) = b0 + _midCoder(posState).getPrice(j - Base.kNumLowLenSymbols)
      }
      for (k <- midEnd until numSymbols)
        prices(st + k) = b1 + _highCoder.getPrice(k - Base.kNumLowLenSymbols - Base.kNumMidLenSymbols)
    }
  };

  class LenPriceTableEncoder extends LenEncoder {
    var _prices:    Array[Int] = Array.ofDim[Int](Base.kNumLenSymbols << Base.kNumPosStatesBitsEncodingMax)
    var _tableSize: Int        = 0
    var _counters:  Array[Int] = Array.ofDim[Int](Base.kNumPosStatesEncodingMax)

    def setTableSize(tableSize: Int): Unit =
      _tableSize = tableSize

    def getPrice(symbol: Int, posState: Int): Int =
      _prices(posState * Base.kNumLenSymbols + symbol)

    def updateTable(posState: Int): Unit = {
      setPrices(posState, _tableSize, _prices, posState * Base.kNumLenSymbols)
      _counters(posState) = _tableSize
    }

    def updateTables(numPosStates: Int): Unit =
      for (posState <- 0 until numPosStates)
        updateTable(posState)

    override def encode(rangeEncoder: sge.utils.compression.rangecoder.Encoder, symbol: Int, posState: Int): Unit = {
      super.encode(rangeEncoder, symbol, posState)
      _counters(posState) -= 1; if (_counters(posState) == 0) updateTable(posState)
    }
  }

  class Optimal {
    var State: Int = 0

    var Prev1IsChar: Boolean = false
    var Prev2:       Boolean = false

    var PosPrev2:  Int = 0
    var BackPrev2: Int = 0

    var Price:    Int = 0
    var PosPrev:  Int = 0
    var BackPrev: Int = 0

    var Backs0: Int = 0
    var Backs1: Int = 0
    var Backs2: Int = 0
    var Backs3: Int = 0

    def makeAsChar(): Unit = {
      BackPrev = -1
      Prev1IsChar = false
    }

    def makeAsShortRep(): Unit = {
      BackPrev = 0;
      Prev1IsChar = false
    }

    def isShortRep(): Boolean =
      BackPrev == 0
  };
}
