/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.nio.{ ByteBuffer, FloatBuffer, IntBuffer, ShortBuffer }

object BufferOps {
  extension (buf: FloatBuffer) {
    inline def size:    Int     = buf.remaining()
    inline def isEmpty: Boolean = !buf.hasRemaining()
  }
  extension (buf: ByteBuffer) {
    inline def size:    Int         = buf.remaining()
    inline def isEmpty: Boolean     = !buf.hasRemaining()
    def toFloatBuffer:  FloatBuffer = buf.asFloatBuffer()
    def toIntBuffer:    IntBuffer   = buf.asIntBuffer()
    def toShortBuffer:  ShortBuffer = buf.asShortBuffer()
  }
  extension (buf: IntBuffer) {
    inline def size:    Int     = buf.remaining()
    inline def isEmpty: Boolean = !buf.hasRemaining()
  }
  extension (buf: ShortBuffer) {
    inline def size:    Int     = buf.remaining()
    inline def isEmpty: Boolean = !buf.hasRemaining()
  }
}
