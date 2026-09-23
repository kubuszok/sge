/*
 * Injected opaque type for GL uniform locations.
 * Original source: no upstream java class — this type is new in the port.
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: an `int` that is really a distinct domain value -> opaque type.
 *   sge's `GLHandle.scala` declares `opaque type UniformLocation = Int` with
 *   comparison extensions. This file is the INJECTED replacement, re-pointed by
 *   PrimitiveToOpaqueTransform(Existing). Every ported declaration java typed
 *   `int location` (where the value is a GL uniform location) becomes
 *   `sge.graphics.UniformLocation`.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.graphics

/** Opaque type for GL uniform locations, preventing accidental mixing with
  * attribute locations or raw indices. */
opaque type UniformLocation = Int
object UniformLocation {

  def apply(raw: Int): UniformLocation = raw

  val notFound: UniformLocation = -1

  /** Array coercions for O3 — the phase emits calls to these for `int[]` <-> `Array[UniformLocation]`
    * conversions. Inside the companion object, `UniformLocation` and `Int` are the same type, so
    * these are identity functions with no cast — correct on JVM, JS and Native. */
  def wrapArray(v: Array[Int]): Array[UniformLocation] = v
  def unwrapArray(v: Array[UniformLocation]): Array[Int] = v

  extension (l: UniformLocation) {
    inline def toInt: Int = l
    inline def +(offset: Int): UniformLocation = l + offset
    inline def -(other: UniformLocation): Int = l - other
    inline def >=(rhs: Int): Boolean = l >= rhs
    inline def <(rhs: Int): Boolean = l < rhs
  }
}
