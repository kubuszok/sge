/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Path.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: derivativeAt, valueAt, approximate, locate, approxLength
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: Path,approxLength,approximate,derivativeAt,locate,valueAt
 * Covenant-source-reference: com/badlogic/gdx/math/Path.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math

/** Interface that specifies a path of type T within the window 0.0<=t<=1.0.
  * @author
  *   Xoppa (original implementation)
  */
trait Path[T] {

  def derivativeAt(out: T, t: Float): T

  /** @return The value of the path at t where 0<=t<=1 */
  def valueAt(out: T, t: Float): T

  /** @return
    *   The approximated value (between 0 and 1) on the path which is closest to the specified value. Note that the implementation of this method might be optimized for speed against precision, see
    *   {@link #locate(Object)} for a more precise (but more intensive) method.
    */
  def approximate(v: T): Float

  /** @return
    *   The precise location (between 0 and 1) on the path which is closest to the specified value. Note that the implementation of this method might be CPU intensive, see {@link #approximate(Object)}
    *   for a faster (but less precise) method.
    */
  def locate(v: T): Float

  /** @param samples
    *   The amount of divisions used to approximate length. Higher values will produce more precise results, but will be more CPU intensive.
    * @return
    *   An approximated length of the spline through sampling the curve and accumulating the euclidean distances between the sample points.
    */
  def approxLength(samples: Int): Float
}
