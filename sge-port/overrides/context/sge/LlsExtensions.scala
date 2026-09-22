/*
 * SGE-original file, no LibGDX counterpart: extension methods giving the lls base's collections
 * the member names sge's hand-written code uses.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 21
 * Covenant-baseline-methods: first,length,right,top
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-09-22
 */
package sge

extension [A](da: lowlevel.util.DynamicArray[A]) {
  def length: Int = da.size
  /** sge keeps `first` on `DynamicArray`; the lls port renamed it to `head` (Scala convention) */
  inline def first: A = da.head
}
extension [A <: AnyRef](os: lowlevel.util.OrderedSet[A]) {
  /** sge keeps `first` on `OrderedSet`; the lls port renamed it to `head` */
  inline def first: A = os.head
}
extension (a: sge.scenes.scene2d.Actor) {
  def right: Float = a.x + a.width
  def top: Float = a.y + a.height
}
