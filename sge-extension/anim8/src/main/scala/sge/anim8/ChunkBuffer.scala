/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Copied straight out of libGDX, in the PixmapIO class.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: ChunkBuffer,endChunk,this
 * Covenant-source-reference: com/github/tommyettinger/anim8/ChunkBuffer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 38634cefd749a9a8af4534ca285c8e72437fe181
 */
package sge
package anim8

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream

/** PNG chunk writing utility. Copied straight out of libGDX, in the PixmapIO class. */
private[anim8] class ChunkBuffer private (val buffer: ByteArrayOutputStream, val crc: CRC32) extends DataOutputStream(new CheckedOutputStream(buffer, crc)) {

  def this(initialSize: Int) =
    this(new ByteArrayOutputStream(initialSize), new CRC32())

  @throws[IOException]
  def endChunk(target: DataOutputStream): Unit = {
    flush()
    target.writeInt(buffer.size() - 4)
    buffer.writeTo(target)
    target.writeInt(crc.getValue.toInt)
    buffer.reset()
    crc.reset()
  }
}
