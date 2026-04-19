/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: BufferOps,isEmpty,size,toFloatBuffer,toIntBuffer,toShortBuffer
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
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
