/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Convention: opaque type wrapping Int; replaces raw Int pixel coordinates/dimensions
 *   Idiom: split packages
 *   Audited: 2026-03-07
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 53
 * Covenant-baseline-methods: Pixels,abs,apply,max,min,toDouble,toFloat,toInt,unary_,zero
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge

/** Opaque type for pixel coordinates and dimensions (screen pixels, backbuffer pixels).
  *
  * Wraps `Int` with zero runtime overhead. Prevents accidental mixing of pixel values with unrelated integers (key codes, pointer IDs, button codes, etc.).
  *
  * Use `Pixels(intValue)` to construct from raw Int, and `.toInt` to unwrap when raw arithmetic or Java/GL interop is needed.
  */
opaque type Pixels = Int
object Pixels {
  inline def apply(value: Int): Pixels = value
  val zero:                     Pixels = 0

  given utils.MkArray[Pixels] = utils.MkArray.mkInt.asInstanceOf[utils.MkArray[Pixels]]

  extension (p: Pixels) {
    inline def toInt:    Int    = p
    inline def toFloat:  Float  = p.toFloat
    inline def toDouble: Double = p.toDouble

    def +(other:  Pixels): Pixels = p + other
    def -(other:  Pixels): Pixels = p - other
    def *(scalar: Int):    Pixels = p * scalar
    @scala.annotation.targetName("divInt")
    def /(scalar: Int):    Pixels = p / scalar
    def %(other:  Pixels): Pixels = p % other
    def unary_-          : Pixels = -p

    def >(other:  Pixels): Boolean = p > other
    def >=(other: Pixels): Boolean = p >= other
    def <(other:  Pixels): Boolean = p < other
    def <=(other: Pixels): Boolean = p <= other

    inline def max(other: Pixels): Pixels = Math.max(p, other)
    inline def min(other: Pixels): Pixels = Math.min(p, other)
    inline def abs:                Pixels = Math.abs(p)
  }

  /** Allow `intValue * pixels` in addition to `pixels * intValue`. */
  extension (scalar: Int) {
    @scala.annotation.targetName("intTimesPixels")
    def *(p: Pixels): Pixels = scalar * p
  }
}
