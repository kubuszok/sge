package sge.utils.compression.lz

import java.io.IOException

class InWindow {
  var _bufferBase: Array[Byte] = scala.compiletime.uninitialized // pointer to buffer with data
  var _stream: java.io.InputStream = scala.compiletime.uninitialized
  var _posLimit: Int = 0 // offset (from _buffer) of first byte when new block reading must be done
  var _streamEndWasReached: Boolean = false // if (true) then _streamPos shows real end of stream

  var _pointerToLastSafePosition: Int = 0

  var _bufferOffset: Int = 0

  var _blockSize: Int = 0 // Size of Allocated memory block
  var _pos: Int = 0 // offset (from _buffer) of curent byte
  var _keepSizeBefore: Int = 0 // how many BYTEs must be kept in buffer before _pos
  var _keepSizeAfter: Int = 0 // how many BYTEs must be kept buffer after _pos
  var _streamPos: Int = 0 // offset (from _buffer) of first not read byte from Stream

  def moveBlock(): Unit = {
    var offset = _bufferOffset + _pos - _keepSizeBefore
    // we need one additional byte, since movePos moves on 1 byte.
    if (offset > 0) offset -= 1

    val numBytes = _bufferOffset + _streamPos - offset

    // check negative offset ???
    for (i <- 0 until numBytes)
      _bufferBase(i) = _bufferBase(offset + i)
    _bufferOffset -= offset
  }

  @throws[IOException]
  def readBlock(): Unit = {
    if (_streamEndWasReached) return
    while (true) {
      val size = (0 - _bufferOffset) + _blockSize - _streamPos
      if (size == 0) return
      val numReadBytes = _stream.read(_bufferBase, _bufferOffset + _streamPos, size)
      if (numReadBytes == -1) {
        _posLimit = _streamPos
        val pointerToPostion = _bufferOffset + _posLimit
        if (pointerToPostion > _pointerToLastSafePosition) _posLimit = _pointerToLastSafePosition - _bufferOffset
        _streamEndWasReached = true
        return
      }
      _streamPos += numReadBytes
      if (_streamPos >= _pos + _keepSizeAfter) _posLimit = _streamPos - _keepSizeAfter
    }
  }

  def free(): Unit = {
    _bufferBase = null
  }

  def create(keepSizeBefore: Int, keepSizeAfter: Int, keepSizeReserv: Int): Unit = {
    _keepSizeBefore = keepSizeBefore
    _keepSizeAfter = keepSizeAfter
    val blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv
    if (_bufferBase == null || _blockSize != blockSize) {
      free()
      _blockSize = blockSize
      _bufferBase = new Array[Byte](_blockSize)
    }
    _pointerToLastSafePosition = _blockSize - keepSizeAfter
  }

  def setStream(stream: java.io.InputStream): Unit = {
    _stream = stream
  }

  def releaseStream(): Unit = {
    _stream = null
  }

  @throws[IOException]
  def init(): Unit = {
    _bufferOffset = 0
    _pos = 0
    _streamPos = 0
    _streamEndWasReached = false
    readBlock()
  }

  @throws[IOException]
  def movePos(): Unit = {
    _pos += 1
    if (_pos > _posLimit) {
      val pointerToPostion = _bufferOffset + _pos
      if (pointerToPostion > _pointerToLastSafePosition) moveBlock()
      readBlock()
    }
  }

  def getIndexByte(index: Int): Byte = {
    _bufferBase(_bufferOffset + _pos + index)
  }

  // index + limit have not to exceed _keepSizeAfter;
  def getMatchLen(index: Int, distance: Int, limit: Int): Int = {
    var lim = limit
    if (_streamEndWasReached && (_pos + index) + lim > _streamPos)
      lim = _streamPos - (_pos + index)
    val dist = distance + 1
    val pby = _bufferOffset + _pos + index
    var i = 0
    while (i < lim && _bufferBase(pby + i) == _bufferBase(pby + i - dist))
      i += 1
    i
  }

  def getNumAvailableBytes(): Int = _streamPos - _pos

  def reduceOffsets(subValue: Int): Unit = {
    _bufferOffset += subValue
    _posLimit -= subValue
    _pos -= subValue
    _streamPos -= subValue
  }
}
