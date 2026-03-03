/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/BSpline.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: set(5), valueAt, derivativeAt, approximate(3),
 * locate, approxLength, cubic(4), cubic_derivative(4). Static helpers ported.
 * Uses ClassTag[T] for array creation instead of Java reflection.
 */
package sge
package math

import scala.reflect.ClassTag
import sge.utils.Nullable

/** @author Xoppa (original implementation) */
object BSpline {
  final private val d6 = 1f / 6f

  /** Calculates the cubic b-spline value for the given position (t).
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
  def cubic[T <: Vector[T]](out: T, t: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n = if (continuous) points.length else points.length - 3
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    cubic(out, i, u, points, continuous, tmp)
  }

  /** Calculates the cubic b-spline derivative for the given position (t).
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
  def cubic_derivative[T <: Vector[T]](out: T, t: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n = if (continuous) points.length else points.length - 3
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    cubic_derivative(out, i, u, points, continuous, tmp)
  }

  /** Calculates the cubic b-spline value for the given span (i) at the given position (u).
    * @param out
    *   The Vector to set to the result.
    * @param i
    *   The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - 3 (cubic degree)
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
  def cubic[T <: Vector[T]](out: T, i: Int, u: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n  = points.length
    val dt = 1f - u
    val t2 = u * u
    val t3 = t2 * u
    out.set(points(i)).scale((3f * t3 - 6f * t2 + 4f) * d6)
    if (continuous || i > 0) out.+(tmp.set(points((n + i - 1) % n)).scale(dt * dt * dt * d6))
    if (continuous || i < (n - 1)) out.+(tmp.set(points((i + 1) % n)).scale((-3f * t3 + 3f * t2 + 3f * u + 1f) * d6))
    if (continuous || i < (n - 2)) out.+(tmp.set(points((i + 2) % n)).scale(t3 * d6))
    out
  }

  /** Calculates the cubic b-spline derivative for the given span (i) at the given position (u).
    * @param out
    *   The Vector to set to the result.
    * @param i
    *   The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - 3 (cubic degree)
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
  def cubic_derivative[T <: Vector[T]](out: T, i: Int, u: Float, points: Array[T], continuous: Boolean, tmp: T): T = {
    val n  = points.length
    val dt = 1f - u
    val t2 = u * u
    out.set(points(i)).scale(1.5f * t2 - 2 * u)
    if (continuous || i > 0) out.+(tmp.set(points((n + i - 1) % n)).scale(-0.5f * dt * dt))
    if (continuous || i < (n - 1)) out.+(tmp.set(points((i + 1) % n)).scale(-1.5f * t2 + u + 0.5f))
    if (continuous || i < (n - 2)) out.+(tmp.set(points((i + 2) % n)).scale(0.5f * t2))
    out
  }

  /** Calculates the n-degree b-spline value for the given position (t).
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The position (0<=t<=1) on the spline
    * @param points
    *   The control points
    * @param degree
    *   The degree of the b-spline
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def calculate[T <: Vector[T]](out: T, t: Float, points: Array[T], degree: Int, continuous: Boolean, tmp: T): T = {
    val n = if (continuous) points.length else points.length - degree
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    calculate(out, i, u, points, degree, continuous, tmp)
  }

  /** Calculates the n-degree b-spline derivative for the given position (t).
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The position (0<=t<=1) on the spline
    * @param points
    *   The control points
    * @param degree
    *   The degree of the b-spline
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def derivative[T <: Vector[T]](out: T, t: Float, points: Array[T], degree: Int, continuous: Boolean, tmp: T): T = {
    val n = if (continuous) points.length else points.length - degree
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    derivative(out, i, u, points, degree, continuous, tmp)
  }

  /** Calculates the n-degree b-spline value for the given span (i) at the given position (u).
    * @param out
    *   The Vector to set to the result.
    * @param i
    *   The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - degree
    * @param u
    *   The position (0<=u<=1) on the span
    * @param points
    *   The control points
    * @param degree
    *   The degree of the b-spline, only 3 is supported
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def calculate[T <: Vector[T]](out: T, i: Int, u: Float, points: Array[T], degree: Int, continuous: Boolean, tmp: T): T =
    degree match {
      case 3 => cubic(out, i, u, points, continuous, tmp)
      case _ => throw new IllegalArgumentException()
    }

  /** Calculates the n-degree b-spline derivative for the given span (i) at the given position (u).
    * @param out
    *   The Vector to set to the result.
    * @param i
    *   The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - degree
    * @param u
    *   The position (0<=u<=1) on the span
    * @param points
    *   The control points
    * @param degree
    *   The degree of the b-spline, only 3 is supported
    * @param continuous
    *   If true the b-spline restarts at 0 when reaching 1
    * @param tmp
    *   A temporary vector used for the calculation
    * @return
    *   The value of out
    */
  def derivative[T <: Vector[T]](out: T, i: Int, u: Float, points: Array[T], degree: Int, continuous: Boolean, tmp: T): T =
    degree match {
      case 3 => cubic_derivative(out, i, u, points, continuous, tmp)
      case _ => throw new IllegalArgumentException()
    }
}

class BSpline[T <: Vector[T]: ClassTag] extends Path[T] {
  var controlPoints: Array[T] = scala.compiletime.uninitialized
  var knots:         Array[T] = scala.compiletime.uninitialized
  var degree:        Int      = scala.compiletime.uninitialized
  var continuous:    Boolean  = scala.compiletime.uninitialized
  var spanCount:     Int      = scala.compiletime.uninitialized
  private var tmp:   T        = scala.compiletime.uninitialized
  private var tmp2:  T        = scala.compiletime.uninitialized
  private var tmp3:  T        = scala.compiletime.uninitialized

  def this(controlPoints: Array[T], degree: Int, continuous: Boolean) = {
    this()
    set(controlPoints, degree, continuous)
  }

  def set(controlPoints: Array[T], degree: Int, continuous: Boolean): BSpline[T] = {
    if (Nullable(tmp).isEmpty) tmp = controlPoints(0).copy
    if (Nullable(tmp2).isEmpty) tmp2 = controlPoints(0).copy
    if (Nullable(tmp3).isEmpty) tmp3 = controlPoints(0).copy
    this.controlPoints = controlPoints
    this.degree = degree
    this.continuous = continuous
    this.spanCount = if (continuous) controlPoints.length else controlPoints.length - degree
    // We use knots.length instead of storing another variable in each BSpline.
    val knotCount = if (continuous) controlPoints.length else controlPoints.length - 1
    knots = new Array[T](knotCount)
    for (i <- 0 until knotCount)
      knots(i) = BSpline.calculate(controlPoints(0).copy, if (continuous) i else i + (0.5f * degree).toInt, 0f, controlPoints, degree, continuous, tmp)
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
    BSpline.calculate(out, if (continuous) span else span + (degree * 0.5f).toInt, u, controlPoints, degree, continuous, tmp)

  override def derivativeAt(out: T, t: Float): T = {
    val n = spanCount
    var u = t * n
    val i = if (t >= 1f) n - 1 else u.toInt
    u -= i
    derivativeAt(out, i, u)
  }

  /** @return The derivative of the spline at position u of the specified span */
  def derivativeAt(out: T, span: Int, u: Float): T =
    BSpline.derivative(out, if (continuous) span else span + (degree * 0.5f).toInt, u, controlPoints, degree, continuous, tmp)

  /** @return The span closest to the specified value */
  def nearest(in: T): Int =
    nearest(in, 0, spanCount)

  /** @return The span closest to the specified value, restricting to the specified spans. */
  def nearest(in: T, start: Int, count: Int): Int = {
    val knotCount     = knots.length
    var adjustedStart = start
    while (adjustedStart < 0)
      adjustedStart += knotCount
    var result = adjustedStart % knotCount
    var dst    = in.distanceSq(knots(result))
    for (i <- 1 until count) {
      val idx = (adjustedStart + i) % knotCount
      val d   = in.distanceSq(knots(idx))
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
    var n           = near
    val nearestKnot = knots(n)
    val previous    = knots(if (n > 0) n - 1 else knots.length - 1)
    val next        = knots((n + 1) % knots.length)
    val dstPrev2    = in.distanceSq(previous)
    val dstNext2    = in.distanceSq(next)

    if (dstNext2 < dstPrev2) {
      val L1Sqr = nearestKnot.distanceSq(next) + 1e-10f // arbitrary epsilon value to avoid division by 0
      val L2Sqr = in.distanceSq(next)
      val L3Sqr = in.distanceSq(nearestKnot)
      val L1    = Math.sqrt(L1Sqr).toFloat
      val s     = (L2Sqr + L1Sqr - L3Sqr) / (2 * L1)
      val u     = MathUtils.clamp((L1 - s) / L1, 0f, 1f)
      (n + u) / spanCount
    } else {
      n = if (n > 0) n - 1 else knots.length - 1
      val L1Sqr = previous.distanceSq(nearestKnot) + 1e-10f // arbitrary epsilon value to avoid division by 0
      val L2Sqr = in.distanceSq(nearestKnot)
      val L3Sqr = in.distanceSq(previous)
      val L1    = Math.sqrt(L1Sqr).toFloat
      val s     = (L2Sqr + L1Sqr - L3Sqr) / (2 * L1)
      val u     = MathUtils.clamp((L1 - s) / L1, 0f, 1f)
      (n + u) / spanCount
    }
  }

  override def locate(v: T): Float =
    // TODO Add a precise method
    approximate(v)

  override def approxLength(samples: Int): Float = {
    var tempLength = 0f
    for (i <- 0 until samples) {
      tmp2.set(tmp3)
      valueAt(tmp3, i.toFloat / (samples - 1).toFloat)
      if (i > 0) tempLength += tmp2.distance(tmp3)
    }
    tempLength
  }
}
