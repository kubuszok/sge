/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/lz/BinTree.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.utils.compression.lz

import java.io.IOException

class BinTree extends InWindow {
  var _cyclicBufferPos:  Int = 0
  var _cyclicBufferSize: Int = 0
  var _matchMaxLen:      Int = 0

  var _son:  Array[Int] = scala.compiletime.uninitialized
  var _hash: Array[Int] = scala.compiletime.uninitialized

  var _cutValue:    Int = 0xff
  var _hashMask:    Int = 0
  var _hashSizeSum: Int = 0

  var hashArray: Boolean = true

  var kNumHashDirectBytes: Int = 0
  var kMinMatchCheck:      Int = 4
  var kFixHashSize:        Int = BinTree.kHash2Size + BinTree.kHash3Size

  def setType(numHashBytes: Int): Unit = {
    hashArray = numHashBytes > 2
    if (hashArray) {
      kNumHashDirectBytes = 0
      kMinMatchCheck = 4
      kFixHashSize = BinTree.kHash2Size + BinTree.kHash3Size
    } else {
      kNumHashDirectBytes = 2
      kMinMatchCheck = 3
      kFixHashSize = 0
    }
  }

  @throws[IOException]
  override def init(): Unit = {
    super.init()
    if (_hash != null)
      for (i <- 0 until _hashSizeSum)
        _hash(i) = BinTree.kEmptyHashValue
    _cyclicBufferPos = 0
    reduceOffsets(-1)
  }

  @throws[IOException]
  override def movePos(): Unit = {
    _cyclicBufferPos += 1
    if (_cyclicBufferPos >= _cyclicBufferSize) _cyclicBufferPos = 0
    super.movePos()
    if (_pos == BinTree.kMaxValForNormalize) normalize()
  }

  def create(historySize: Int, keepAddBufferBefore: Int, matchMaxLen: Int, keepAddBufferAfter: Int): Boolean =
    if (historySize > BinTree.kMaxValForNormalize - 256) false
    else {
      _cutValue = 16 + (matchMaxLen >> 1)
      val windowReservSize = (historySize + keepAddBufferBefore + matchMaxLen + keepAddBufferAfter) / 2 + 256
      super.create(historySize + keepAddBufferBefore, matchMaxLen + keepAddBufferAfter, windowReservSize)
      _matchMaxLen = matchMaxLen
      val cyclicBufferSize = historySize + 1
      if (_cyclicBufferSize != cyclicBufferSize) _son = new Array[Int]({ _cyclicBufferSize = cyclicBufferSize; _cyclicBufferSize * 2 })
      var hs = BinTree.kBT2HashSize
      if (hashArray) {
        hs = historySize - 1
        hs |= (hs >> 1)
        hs |= (hs >> 2)
        hs |= (hs >> 4)
        hs |= (hs >> 8)
        hs >>= 1
        hs |= 0xffff
        if (hs > (1 << 24)) hs >>= 1
        _hashMask = hs
        hs += 1
        hs += kFixHashSize
      }
      if (hs != _hashSizeSum) _hash = new Array[Int]({ _hashSizeSum = hs; _hashSizeSum })
      true
    }

  @throws[IOException]
  def getMatches(distances: Array[Int]): Int = {
    var lenLimit = 0
    if (_pos + _matchMaxLen <= _streamPos)
      lenLimit = _matchMaxLen
    else {
      lenLimit = _streamPos - _pos
      if (lenLimit < kMinMatchCheck) {
        movePos()
      }
    }
    if (lenLimit < kMinMatchCheck) 0
    else {
      var offset      = 0
      val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
      val cur         = _bufferOffset + _pos
      var maxLen      = BinTree.kStartMaxLen
      var hashValue   = 0
      var hash2Value  = 0
      var hash3Value  = 0
      if (hashArray) {
        var temp = BinTree.CrcTable(_bufferBase(cur) & 0xff) ^ (_bufferBase(cur + 1) & 0xff)
        hash2Value = temp & (BinTree.kHash2Size - 1)
        temp ^= (_bufferBase(cur + 2) & 0xff) << 8
        hash3Value = temp & (BinTree.kHash3Size - 1)
        hashValue = (temp ^ (BinTree.CrcTable(_bufferBase(cur + 3) & 0xff) << 5)) & _hashMask
      } else
        hashValue = (_bufferBase(cur) & 0xff) ^ ((_bufferBase(cur + 1) & 0xff) << 8)
      var curMatch = _hash(kFixHashSize + hashValue)
      if (hashArray) {
        var curMatch2 = _hash(hash2Value)
        var curMatch3 = _hash(BinTree.kHash3Offset + hash3Value)
        _hash(hash2Value) = _pos
        _hash(BinTree.kHash3Offset + hash3Value) = _pos
        if (curMatch2 > matchMinPos && _bufferBase(_bufferOffset + curMatch2) == _bufferBase(cur)) {
          distances(offset) = maxLen; offset += 1; maxLen = 2
          distances(offset) = _pos - curMatch2 - 1; offset += 1
        }
        if (curMatch3 > matchMinPos && _bufferBase(_bufferOffset + curMatch3) == _bufferBase(cur)) {
          if (curMatch3 == curMatch2) { offset -= 2 }
          distances(offset) = maxLen; offset += 1; maxLen = 3
          distances(offset) = _pos - curMatch3 - 1; offset += 1
          curMatch2 = curMatch3
        }
        if (offset != 0 && curMatch2 == curMatch) {
          offset -= 2
          maxLen = BinTree.kStartMaxLen
        }
      }
      _hash(kFixHashSize + hashValue) = _pos
      val ptr0 = (_cyclicBufferPos << 1) + 1
      val ptr1 = _cyclicBufferPos << 1
      var len0 = kNumHashDirectBytes
      var len1 = kNumHashDirectBytes
      if (kNumHashDirectBytes != 0) {
        if (curMatch > matchMinPos) {
          if (_bufferBase(_bufferOffset + curMatch + kNumHashDirectBytes) != _bufferBase(cur + kNumHashDirectBytes)) {
            distances(offset) = maxLen; offset += 1; maxLen = kNumHashDirectBytes
            distances(offset) = _pos - curMatch - 1; offset += 1
          }
        }
      }
      var count = _cutValue
      var done  = false
      while (!done)
        if (curMatch <= matchMinPos || { count -= 1; count < 0 }) {
          _son(ptr0) = BinTree.kEmptyHashValue
          _son(ptr1) = BinTree.kEmptyHashValue
          done = true
        } else {
          val delta     = _pos - curMatch
          val cyclicPos = (if (delta <= _cyclicBufferPos) _cyclicBufferPos - delta else _cyclicBufferPos - delta + _cyclicBufferSize) << 1
          val pby1      = _bufferOffset + curMatch
          var len       = math.min(len0, len1)
          if (_bufferBase(pby1 + len) == _bufferBase(cur + len)) {
            var break = false
            while (!break && { len += 1; len != lenLimit })
              if (_bufferBase(pby1 + len) != _bufferBase(cur + len)) break = true
            if (maxLen < len) {
              distances(offset) = maxLen; offset += 1; maxLen = len
              distances(offset) = delta - 1; offset += 1
              if (len == lenLimit) {
                _son(ptr1) = _son(cyclicPos)
                _son(ptr0) = _son(cyclicPos + 1)
                done = true
              }
            }
          }
          if ((_bufferBase(pby1 + len) & 0xff) < (_bufferBase(cur + len) & 0xff)) {
            _son(ptr1) = curMatch
            val newPtr1 = cyclicPos + 1
            curMatch = _son(newPtr1)
            len1 = len
          } else {
            _son(ptr0) = curMatch
            val newPtr0 = cyclicPos
            curMatch = _son(newPtr0)
            len0 = len
          }
        }
      movePos()
      offset
    }
  }

  @throws[IOException]
  def skip(num: Int): Unit = {
    var n = num
    while (n > 0) {
      var lenLimit = 0
      if (_pos + _matchMaxLen <= _streamPos)
        lenLimit = _matchMaxLen
      else {
        lenLimit = _streamPos - _pos
        if (lenLimit < kMinMatchCheck) {
          movePos()
          n -= 1
        } else {
          val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
          val cur         = _bufferOffset + _pos
          var hashValue   = 0
          if (hashArray) {
            var temp       = BinTree.CrcTable(_bufferBase(cur) & 0xff) ^ (_bufferBase(cur + 1) & 0xff)
            val hash2Value = temp & (BinTree.kHash2Size - 1)
            _hash(hash2Value) = _pos
            temp ^= (_bufferBase(cur + 2) & 0xff) << 8
            val hash3Value = temp & (BinTree.kHash3Size - 1)
            _hash(BinTree.kHash3Offset + hash3Value) = _pos
            hashValue = (temp ^ (BinTree.CrcTable(_bufferBase(cur + 3) & 0xff) << 5)) & _hashMask
          } else
            hashValue = (_bufferBase(cur) & 0xff) ^ ((_bufferBase(cur + 1) & 0xff) << 8)
          var curMatch = _hash(kFixHashSize + hashValue)
          _hash(kFixHashSize + hashValue) = _pos
          val ptr0  = (_cyclicBufferPos << 1) + 1
          val ptr1  = _cyclicBufferPos << 1
          var len0  = kNumHashDirectBytes
          var len1  = kNumHashDirectBytes
          var count = _cutValue
          var done  = false
          while (!done)
            if (curMatch <= matchMinPos || { count -= 1; count < 0 }) {
              _son(ptr0) = BinTree.kEmptyHashValue
              _son(ptr1) = BinTree.kEmptyHashValue
              done = true
            } else {
              val delta     = _pos - curMatch
              val cyclicPos = (if (delta <= _cyclicBufferPos) _cyclicBufferPos - delta else _cyclicBufferPos - delta + _cyclicBufferSize) << 1
              val pby1      = _bufferOffset + curMatch
              var len       = math.min(len0, len1)
              if (_bufferBase(pby1 + len) == _bufferBase(cur + len)) {
                var break = false
                while (!break && { len += 1; len != lenLimit })
                  if (_bufferBase(pby1 + len) != _bufferBase(cur + len)) break = true
                if (len == lenLimit) {
                  _son(ptr1) = _son(cyclicPos)
                  _son(ptr0) = _son(cyclicPos + 1)
                  done = true
                }
              }
              if ((_bufferBase(pby1 + len) & 0xff) < (_bufferBase(cur + len) & 0xff)) {
                _son(ptr1) = curMatch
                val newPtr1 = cyclicPos + 1
                curMatch = _son(newPtr1)
                len1 = len
              } else {
                _son(ptr0) = curMatch
                val newPtr0 = cyclicPos
                curMatch = _son(newPtr0)
                len0 = len
              }
            }
          movePos()
          n -= 1
        }
      }
    }
  }

  def normalizeLinks(items: Array[Int], numItems: Int, subValue: Int): Unit =
    for (i <- 0 until numItems) {
      var value = items(i)
      if (value <= BinTree.kEmptyHashValue)
        value = BinTree.kEmptyHashValue
      else
        value -= subValue
      items(i) = value
    }

  def normalize(): Unit = {
    val subValue = _pos - _cyclicBufferSize
    normalizeLinks(_son, _cyclicBufferSize * 2, subValue)
    normalizeLinks(_hash, _hashSizeSum, subValue)
    reduceOffsets(subValue)
  }

  def setCutValue(cutValue: Int): Unit =
    _cutValue = cutValue
}

object BinTree {
  final val kHash2Size:          Int = 1 << 10
  final val kHash3Size:          Int = 1 << 16
  final val kBT2HashSize:        Int = 1 << 16
  final val kStartMaxLen:        Int = 1
  final val kHash3Offset:        Int = kHash2Size
  final val kEmptyHashValue:     Int = 0
  final val kMaxValForNormalize: Int = (1 << 30) - 1

  val CrcTable: Array[Int] = {
    val arr = new Array[Int](256)
    for (i <- 0 until 256) {
      var r = i
      for (j <- 0 until 8)
        if ((r & 1) != 0)
          r = (r >>> 1) ^ 0xedb88320
        else
          r >>>= 1
      arr(i) = r
    }
    arr
  }
}
