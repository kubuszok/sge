/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/lz/OutWindow.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All 5 fields and 7 methods faithfully ported
 * - create(): Java `_buffer == null` -> Nullable(_buffer).isEmpty (correct)
 * - releaseStream(): sets _stream = null (raw null at Java interop boundary)
 * - CopyBlock: Java `_buffer[_pos++] = _buffer[pos++]` split into separate statements (correct)
 * - PutByte: Java `_buffer[_pos++] = b` split into separate statements (correct)
 * - _buffer, _stream: scala.compiletime.uninitialized (correct for Java interop)
 * - Package: uses flat `package sge.utils.compression.lz` instead of split; functionally equivalent
 * - Method names lowercased: Create->create, SetStream->setStream, etc.
 * - TODO: uses flat package declaration — convert to split (package sge / package utils / package compression / package lz)
 */
package sge.utils.compression.lz

import java.io.IOException

import sge.utils.Nullable

class OutWindow {
  var _buffer:     Array[Byte]          = scala.compiletime.uninitialized
  var _pos:        Int                  = 0
  var _windowSize: Int                  = 0
  var _streamPos:  Int                  = 0
  var _stream:     java.io.OutputStream = scala.compiletime.uninitialized

  def create(windowSize: Int): Unit = {
    if (Nullable(_buffer).isEmpty || _windowSize != windowSize) _buffer = new Array[Byte](windowSize)
    _windowSize = windowSize
    _pos = 0
    _streamPos = 0
  }

  @throws[IOException]
  def setStream(stream: java.io.OutputStream): Unit = {
    releaseStream()
    _stream = stream
  }

  @throws[IOException]
  def releaseStream(): Unit = {
    flush()
    _stream = null
  }

  def init(solid: Boolean): Unit =
    if (!solid) {
      _streamPos = 0
      _pos = 0
    }

  @throws[IOException]
  def flush(): Unit = {
    val size = _pos - _streamPos
    if (size != 0) {
      _stream.write(_buffer, _streamPos, size)
      if (_pos >= _windowSize) _pos = 0
      _streamPos = _pos
    }
  }

  @throws[IOException]
  def copyBlock(distance: Int, len: Int): Unit = {
    var l   = len
    var pos = _pos - distance - 1
    if (pos < 0) pos += _windowSize
    while (l != 0) {
      if (pos >= _windowSize) pos = 0
      _buffer(_pos) = _buffer(pos)
      _pos += 1
      pos += 1
      if (_pos >= _windowSize) flush()
      l -= 1
    }
  }

  @throws[IOException]
  def putByte(b: Byte): Unit = {
    _buffer(_pos) = b
    _pos += 1
    if (_pos >= _windowSize) flush()
  }

  def getByte(distance: Int): Byte = {
    var pos = _pos - distance - 1
    if (pos < 0) pos += _windowSize
    _buffer(pos)
  }
}
