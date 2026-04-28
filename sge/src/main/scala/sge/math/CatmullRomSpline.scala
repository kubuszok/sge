/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/CatmullRomSpline.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: set(4), valueAt, derivativeAt, approximate(3),
 * locate, approxLength. Static: calculate, derivative.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 236
 * Covenant-baseline-methods: CatmullRomSpline,L1,L1Sqr,L2Sqr,L3Sqr,approxLength,approximate,calculate,continuous,controlPoints,derivative,derivativeAt,dst,dstNext2,dstPrev2,i,locate,n,nearest,next,previous,result,s,set,spanCount,startIndex,tempLength,this,tmp,tmp2,tmp3,u,u2,u3,valueAt
 * Covenant-source-reference: com/badlogic/gdx/math/CatmullRomSpline.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package math

import sge.utils.Nullable

/** @author Xoppa (original implementation) */
class CatmullRomSpline[T <: Vector[T]] extends Path[T] {

  var controlPoints: Array[T] = scala.compiletime.uninitialized
  var continuous:    Boolean  = scala.compiletime.uninitialized
  var spanCount:     Int      = scala.compiletime.uninitialized
  private var tmp:   T        = scala.compiletime.uninitialized
  private var tmp2:  T        = scala.compiletime.uninitialized
  private var tmp3:  T        = scala.compiletime.uninitialized

  def this(controlPoints: Array[T], continuous: Boolean) = {
    this()
    set(controlPoints, continuous)
  }

  def set(controlPoints: Array[T], continuous: Boolean): CatmullRomSpline[T] = {
    if (Nullable(tmp).isEmpty) tmp = controlPoints(0).copy
    if (Nullable(tmp2).isEmpty) tmp2 = controlPoints(0).copy
    if (Nullable(tmp3).isEmpty) tmp3 = controlPoints(0).copy
    this.controlPoints = controlPoints
    this.continuous = continuous
    this.spanCount = if (continuous) controlPoints.length else controlPoints.length - 3
    this
  }

  override def valueAt(out: T, t: Float): T = {
    val n = spanCount
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    valueAt(out, i, u)
  }

  /** @return The value of the spline at position u of the specified span */
  def valueAt(out: T, span: Int, u: Float): T =
    CatmullRomSpline.calculate(out, if (continuous) span else span + 1, u, controlPoints, continuous, tmp)

  override def derivativeAt(out: T, t: Float): T = {
    val n = spanCount
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    derivativeAt(out, i, u)
  }

  /** @return The derivative of the spline at position u of the specified span */
  def derivativeAt(out: T, span: Int, u: Float): T =
    CatmullRomSpline.derivative(out, if (continuous) span else span + 1, u, controlPoints, continuous, tmp)

  /** @return The span closest to the specified value */
  def nearest(in: T): Int =
    nearest(in, 0, spanCount)

  /** @return The span closest to the specified value, restricting to the specified spans. */
  def nearest(in: T, start: Int, count: Int): Int = {
    var startIndex = start
    while (startIndex < 0)
      startIndex += spanCount
    var result = startIndex % spanCount
    var dst    = in.distanceSq(controlPoints(result))
    for (i <- 1 until count) {
      val idx = (startIndex + i) % spanCount
      val d   = in.distanceSq(controlPoints(idx))
      if (d < dst) {
        dst = d
        result = idx
      }
    }
    result
  }

  override def approximate(v: T): Float =
    approximate(v, nearest(v))

  def approximate(in: T, start: Int, count: Int): Float =
    approximate(in, nearest(in, start, count))

  def approximate(in: T, near: Int): Float = {
    var n            = near
    val nearest      = controlPoints(n)
    val previous     = controlPoints(if (n > 0) n - 1 else spanCount - 1)
    val next         = controlPoints((n + 1) % spanCount)
    val dstPrev2     = in.distanceSq(previous)
    val dstNext2     = in.distanceSq(next)
    val (p1, p2, p3) = if (dstNext2 < dstPrev2) {
      (nearest, next, in)
    } else {
      n = if (n > 0) n - 1 else spanCount - 1
      (previous, nearest, in)
    }
    val L1Sqr = p1.distanceSq(p2)
    val L2Sqr = p3.distanceSq(p2)
    val L3Sqr = p3.distanceSq(p1)
    val L1    = scala.math.sqrt(L1Sqr).toFloat
    val s     = (L2Sqr + L1Sqr - L3Sqr) / (2f * L1)
    val u     = MathUtils.clamp((L1 - s) / L1, 0f, 1f)
    (n + u) / spanCount
  }

  override def locate(v: T): Float =
    approximate(v)

  override def approxLength(samples: Int): Float = {
    var tempLength = 0f
    for (i <- 0 until samples) {
      tmp2.set(tmp3)
      valueAt(tmp3, i / (samples - 1).toFloat)
      if (i > 0) tempLength += tmp2.distance(tmp3)
    }
    tempLength
  }
}

object CatmullRomSpline {

  /** Calculates the catmullrom value for the given position (t).
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The position (0<=t<=1) on the spline
    * @param points
    *   The control points
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def calculate[T <: Vector[T]](out: T, t: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n = if (continuous) points.length else points.length - 3
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    calculate(out, i, u, points, continuous, tmp)
  }

  /** Calculates the catmullrom value for the given span (i) at the given position (u).
    * @param out
    *   The Vector to set to the result.
    * @param i
    *   The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - degree
    * @param u
    *   The position (0<=u<=1) on the span
    * @param points
    *   The control points
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def calculate[T <: Vector[T]](out: T, i: Int, u: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n  = points.length
    val u2 = u * u
    val u3 = u2 * u
    out.set(points(i)).scale(1.5f * u3 - 2.5f * u2 + 1.0f)
    if (continuous || i > 0) out.+(tmp.set(points((n + i - 1) % n)).scale(-0.5f * u3 + u2 - 0.5f * u))
    if (continuous || i < (n - 1)) out.+(tmp.set(points((i + 1) % n)).scale(-1.5f * u3 + 2f * u2 + 0.5f * u))
    if (continuous || i < (n - 2)) out.+(tmp.set(points((i + 2) % n)).scale(0.5f * u3 - 0.5f * u2))
    out
  }

  /** Calculates the derivative of the catmullrom spline for the given position (t).
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The position (0<=t<=1) on the spline
    * @param points
    *   The control points
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def derivative[T <: Vector[T]](out: T, t: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n = if (continuous) points.length else points.length - 3
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    derivative(out, i, u, points, continuous, tmp)
  }

  /** Calculates the derivative of the catmullrom spline for the given span (i) at the given position (u).
    * @param out
    *   The Vector to set to the result.
    * @param i
    *   The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - degree
    * @param u
    *   The position (0<=u<=1) on the span
    * @param points
    *   The control points
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def derivative[T <: Vector[T]](out: T, i: Int, u: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    /*
     * catmull'(u) = 0.5 *((-p0 + p2) + 2 * (2*p0 - 5*p1 + 4*p2 - p3) * u + 3 * (-p0 + 3*p1 - 3*p2 + p3) * u * u)
     */
    val n  = points.length
    val u2 = u * u
    // val u3 = u2 * u
    out.set(points(i)).scale(-u * 5 + u2 * 4.5f)
    if (continuous || i > 0) out.+(tmp.set(points((n + i - 1) % n)).scale(-0.5f + u * 2 - u2 * 1.5f))
    if (continuous || i < (n - 1)) out.+(tmp.set(points((i + 1) % n)).scale(0.5f + u * 4 - u2 * 4.5f))
    if (continuous || i < (n - 2)) out.+(tmp.set(points((i + 2) % n)).scale(-u + u2 * 1.5f))
    out
  }
}
