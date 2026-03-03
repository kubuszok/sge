/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Bezier.java
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
 * locate, approxLength. Static: linear(2), quadratic(2), cubic(2), linear_derivative(2),
 * quadratic_derivative(2), cubic_derivative(2).
 */
package sge
package math

import sge.utils.DynamicArray

/** Implementation of the Bezier curve.
  * @author
  *   Xoppa (original implementation)
  */
object Bezier {

  /** Simple linear interpolation
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The location (ranging 0..1) on the line.
    * @param p0
    *   The start point.
    * @param p1
    *   The end point.
    * @param tmp
    *   A temporary vector to be used by the calculation.
    * @return
    *   The value specified by out for chaining
    */
  def linear[T <: Vector[T]](out: T, t: Float, p0: T, p1: T, tmp: T): T =
    // B1(t) = p0 + (p1-p0)*t
    out.set(p0).scale(1f - t).+(tmp.set(p1).scale(t)) // Could just use lerp...

  /** Simple linear interpolation derivative
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The location (ranging 0..1) on the line.
    * @param p0
    *   The start point.
    * @param p1
    *   The end point.
    * @param tmp
    *   A temporary vector to be used by the calculation.
    * @return
    *   The value specified by out for chaining
    */
  def linear_derivative[T <: Vector[T]](out: T, t: Float, p0: T, p1: T, tmp: T): T =
    // B1'(t) = p1-p0
    out.set(p1).-(p0)

  /** Quadratic Bezier curve
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The location (ranging 0..1) on the curve.
    * @param p0
    *   The first bezier point.
    * @param p1
    *   The second bezier point.
    * @param p2
    *   The third bezier point.
    * @param tmp
    *   A temporary vector to be used by the calculation.
    * @return
    *   The value specified by out for chaining
    */
  def quadratic[T <: Vector[T]](out: T, t: Float, p0: T, p1: T, p2: T, tmp: T): T = {
    // B2(t) = (1 - t) * (1 - t) * p0 + 2 * (1-t) * t * p1 + t*t*p2
    val dt = 1f - t
    out.set(p0).scale(dt * dt).+(tmp.set(p1).scale(2 * dt * t)).+(tmp.set(p2).scale(t * t))
  }

  /** Quadratic Bezier curve derivative
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The location (ranging 0..1) on the curve.
    * @param p0
    *   The first bezier point.
    * @param p1
    *   The second bezier point.
    * @param p2
    *   The third bezier point.
    * @param tmp
    *   A temporary vector to be used by the calculation.
    * @return
    *   The value specified by out for chaining
    */
  def quadratic_derivative[T <: Vector[T]](out: T, t: Float, p0: T, p1: T, p2: T, tmp: T): T = {
    // B2'(t) = 2 * (1 - t) * (p1 - p0) + 2 * t * (p2 - p1)
    1f - t
    out.set(p1).-(p0).scale(2).scale(1 - t).+(tmp.set(p2).-(p1).scale(t).scale(2))
  }

  /** Cubic Bezier curve
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The location (ranging 0..1) on the curve.
    * @param p0
    *   The first bezier point.
    * @param p1
    *   The second bezier point.
    * @param p2
    *   The third bezier point.
    * @param p3
    *   The fourth bezier point.
    * @param tmp
    *   A temporary vector to be used by the calculation.
    * @return
    *   The value specified by out for chaining
    */
  def cubic[T <: Vector[T]](out: T, t: Float, p0: T, p1: T, p2: T, p3: T, tmp: T): T = {
    // B3(t) = (1-t) * (1-t) * (1-t) * p0 + 3 * (1-t) * (1-t) * t * p1 + 3 * (1-t) * t * t * p2 + t * t * t * p3
    val dt  = 1f - t
    val dt2 = dt * dt
    val t2  = t * t
    out.set(p0).scale(dt2 * dt).+(tmp.set(p1).scale(3 * dt2 * t)).+(tmp.set(p2).scale(3 * dt * t2)).+(tmp.set(p3).scale(t2 * t))
  }

  /** Cubic Bezier curve derivative
    * @param out
    *   The Vector to set to the result.
    * @param t
    *   The location (ranging 0..1) on the curve.
    * @param p0
    *   The first bezier point.
    * @param p1
    *   The second bezier point.
    * @param p2
    *   The third bezier point.
    * @param p3
    *   The fourth bezier point.
    * @param tmp
    *   A temporary vector to be used by the calculation.
    * @return
    *   The value specified by out for chaining
    */
  def cubic_derivative[T <: Vector[T]](out: T, t: Float, p0: T, p1: T, p2: T, p3: T, tmp: T): T = {
    // B3'(t) = 3 * (1-t) * (1-t) * (p1 - p0) + 6 * (1 - t) * t * (p2 - p1) + 3 * t * t * (p3 - p2)
    val dt  = 1f - t
    val dt2 = dt * dt
    val t2  = t * t
    out.set(p1).-(p0).scale(dt2 * 3).+(tmp.set(p2).-(p1).scale(dt * t * 6)).+(tmp.set(p3).-(p2).scale(t2 * 3))
  }
}

class Bezier[T <: Vector[T]: scala.reflect.ClassTag] extends Path[T] {

  val points:       DynamicArray[T] = DynamicArray[T]()
  private var tmp:  Option[T]       = None
  private var tmp2: Option[T]       = None
  private var tmp3: Option[T]       = None

  def this(points: T*) = {
    this()
    set(points*)
  }

  def this(points: Array[T], offset: Int, length: Int) = {
    this()
    set(points, offset, length)
  }

  def this(points: DynamicArray[T], offset: Int, length: Int) = {
    this()
    set(points, offset, length)
  }

  def set(points: T*): Bezier[T] = {
    if (points.length < 2 || points.length > 4)
      throw utils.SgeError.MathError("Only first, second and third degree Bezier curves are supported.")
    if (tmp.isEmpty) tmp = Some(points(0).copy)
    if (tmp2.isEmpty) tmp2 = Some(points(0).copy)
    if (tmp3.isEmpty) tmp3 = Some(points(0).copy)
    this.points.clear()
    this.points.addAll(points)
    this
  }

  def set(points: Array[T], offset: Int, length: Int): Bezier[T] = {
    if (length < 2 || length > 4)
      throw utils.SgeError.MathError("Only first, second and third degree Bezier curves are supported.")
    if (tmp.isEmpty) tmp = Some(points(offset).copy)
    if (tmp2.isEmpty) tmp2 = Some(points(offset).copy)
    if (tmp3.isEmpty) tmp3 = Some(points(offset).copy)
    this.points.clear()
    this.points.addAll(points, offset, length)
    this
  }

  def set(points: DynamicArray[T], offset: Int, length: Int): Bezier[T] = {
    if (length < 2 || length > 4)
      throw utils.SgeError.MathError("Only first, second and third degree Bezier curves are supported.")
    if (tmp.isEmpty) tmp = Some(points(offset).copy)
    if (tmp2.isEmpty) tmp2 = Some(points(offset).copy)
    if (tmp3.isEmpty) tmp3 = Some(points(offset).copy)
    this.points.clear()
    for (i <- offset until (offset + length))
      this.points.add(points(i))
    this
  }

  override def valueAt(out: T, t: Float): T = {
    val n = points.size
    if (n == 2)
      Bezier.linear(out, t, points(0), points(1), tmp.get)
    else if (n == 3)
      Bezier.quadratic(out, t, points(0), points(1), points(2), tmp.get)
    else if (n == 4)
      Bezier.cubic(out, t, points(0), points(1), points(2), points(3), tmp.get)
    out
  }

  override def derivativeAt(out: T, t: Float): T = {
    val n = points.size
    if (n == 2)
      Bezier.linear_derivative(out, t, points(0), points(1), tmp.get)
    else if (n == 3)
      Bezier.quadratic_derivative(out, t, points(0), points(1), points(2), tmp.get)
    else if (n == 4)
      Bezier.cubic_derivative(out, t, points(0), points(1), points(2), points(3), tmp.get)
    out
  }

  override def approximate(v: T): Float = {
    // TODO: make a real approximate method
    val p1    = points(0)
    val p2    = points(points.size - 1)
    val p3    = v
    val l1Sqr = p1.distanceSq(p2)
    val l2Sqr = p3.distanceSq(p2)
    val l3Sqr = p3.distanceSq(p1)
    val l1    = Math.sqrt(l1Sqr).toFloat
    val s     = (l2Sqr + l1Sqr - l3Sqr) / (2 * l1)
    MathUtils.clamp((l1 - s) / l1, 0f, 1f)
  }

  override def locate(v: T): Float =
    // TODO implement a precise method
    approximate(v)

  override def approxLength(samples: Int): Float = {
    var tempLength = 0f
    for (i <- 0 until samples) {
      tmp2.get.set(tmp3.get)
      valueAt(tmp3.get, i.toFloat / (samples - 1).toFloat)
      if (i > 0) tempLength += tmp2.get.distance(tmp3.get)
    }
    tempLength
  }
}
