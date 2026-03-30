/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Copied straight out of libGDX, in the PixmapIO class.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
