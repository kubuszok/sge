/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Convention: opaque type wrapping Float; replaces raw Float for world/viewport dimensions
 *   Idiom: split packages
 *   Audited: 2026-03-19
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 54
 * Covenant-baseline-methods: WorldUnits,abs,apply,max,min,toDouble,toFloat,toInt,unary_,zero
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge

/** Opaque type for world-space coordinates and dimensions (camera viewport size, world width/height).
  *
  * Wraps `Float` with zero runtime overhead. Prevents accidental mixing of world-unit values with unrelated floats (delta time, angles, pixel counts, etc.).
  *
  * Use `WorldUnits(floatValue)` to construct from raw Float, and `.toFloat` to unwrap when raw arithmetic or Java/GL interop is needed.
  */
opaque type WorldUnits = Float
object WorldUnits {
  inline def apply(value: Float): WorldUnits = value
  val zero:                       WorldUnits = 0f

  given utils.MkArray[WorldUnits] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[WorldUnits]]

  extension (wu: WorldUnits) {
    inline def toFloat:  Float  = wu
    inline def toInt:    Int    = wu.toInt
    inline def toDouble: Double = wu.toDouble

    def +(other:  WorldUnits): WorldUnits = wu + other
    def -(other:  WorldUnits): WorldUnits = wu - other
    def *(scalar: Float):      WorldUnits = wu * scalar
    @scala.annotation.targetName("divFloat")
    def /(scalar: Float):      WorldUnits = wu / scalar
    def /(other:  WorldUnits): Float      = wu / other
    def %(other:  WorldUnits): WorldUnits = wu % other
    def unary_-              : WorldUnits = -wu

    def >(other:  WorldUnits): Boolean = wu > other
    def >=(other: WorldUnits): Boolean = wu >= other
    def <(other:  WorldUnits): Boolean = wu < other
    def <=(other: WorldUnits): Boolean = wu <= other

    inline def max(other: WorldUnits): WorldUnits = Math.max(wu, other)
    inline def min(other: WorldUnits): WorldUnits = Math.min(wu, other)
    inline def abs:                    WorldUnits = Math.abs(wu)
  }

  /** Allow `floatValue * worldUnits` in addition to `worldUnits * floatValue`. */
  extension (scalar: Float) {
    @scala.annotation.targetName("floatTimesWorldUnits")
    def *(wu: WorldUnits): WorldUnits = scalar * wu
  }
}
