/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/MathUtils.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

import java.util.Random

/** Utility and fast math functions. <p> Thanks to Riven on JavaGaming.org for the basis of sin/cos/floor/ceil.
  * @author
  *   Nathan Sweet (original implementation)
  */
object MathUtils {

  val nanoToSec = 1 / 1000000000f

  // ---
  val FLOAT_ROUNDING_ERROR = 0.000001f // 32 bits
  val PI                   = Math.PI.toFloat
  val PI2                  = PI * 2
  val HALF_PI              = PI / 2

  val E = Math.E.toFloat

  private val SIN_BITS       = 14 // 16KB. Adjust for accuracy.
  private[math] val SIN_MASK = ~(-1 << SIN_BITS)
  private val SIN_COUNT      = SIN_MASK + 1

  private val radFull          = PI2
  private val degFull          = 360
  private[math] val radToIndex = SIN_COUNT / radFull
  private[math] val degToIndex = SIN_COUNT / degFull

  /** multiply by this to convert from radians to degrees */
  val radiansToDegrees = 180f / PI
  val radDeg           = radiansToDegrees

  /** multiply by this to convert from degrees to radians */
  val degreesToRadians = PI / 180
  val degRad           = degreesToRadians

  private[math] object Sin {
    val table = new Array[Float](SIN_COUNT)

    {
      for (i <- 0 until SIN_COUNT)
        table(i) = Math.sin((i + 0.5f) / SIN_COUNT * radFull).toFloat
      // The four right angles get extra-precise values, because they are
      // the most likely to need to be correct.
      table(0) = 0f
      table(((90 * degToIndex) & SIN_MASK).toInt) = 1f
      table(((180 * degToIndex) & SIN_MASK).toInt) = 0f
      table(((270 * degToIndex) & SIN_MASK).toInt) = -1f
    }
  }

  /** Returns the sine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both inclusive).
    */
  def sin(radians: Float): Float =
    Sin.table((radians * radToIndex).toInt & SIN_MASK)

  /** Returns the cosine in radians from a lookup table. For optimal precision, use radians between -PI2 and PI2 (both inclusive).
    */
  def cos(radians: Float): Float =
    Sin.table(((radians + HALF_PI) * radToIndex).toInt & SIN_MASK)

  /** Returns the sine in degrees from a lookup table. For optimal precision, use degrees between -360 and 360 (both inclusive).
    */
  def sinDeg(degrees: Float): Float =
    Sin.table((degrees * degToIndex).toInt & SIN_MASK)

  /** Returns the cosine in degrees from a lookup table. For optimal precision, use degrees between -360 and 360 (both inclusive).
    */
  def cosDeg(degrees: Float): Float =
    Sin.table(((degrees + 90) * degToIndex).toInt & SIN_MASK)

  /** Returns the tangent given an input in radians, using a Padé approximant. <br> Padé approximants tend to be most accurate when they aren't producing results of extreme magnitude; in the tan()
    * function, those results occur on and near odd multiples of {@code PI/2} , and this method is least accurate when given inputs near those multiples. <br> For inputs between -1.57 to 1.57 (just
    * inside half-pi), separated by 0x1p-20f, absolute error is 0.00890192, relative error is 0.00000090, and the maximum error is 17.98901367 when given 1.56999838. The maximum error might seem
    * concerning, but it's the difference between the correct 1253.22167969 and the 1235.23266602 this returns, so for many purposes the difference won't be noticeable. <br> For inputs between -1.55
    * to 1.55 (getting less close to half-pi), separated by 0x1p-20f, absolute error is 0.00023368, relative error is -0.00000009, and the maximum error is 0.02355957 when given -1.54996467. The
    * maximum error is the difference between the correct -47.99691010 and the -47.97335052 this returns. <br> While you don't have to use a dedicated method for tan(), and you can use
    * {@code sin(x)/cos(x)} , approximating tan() in that way is very susceptible to error building up from any of sin(), cos() or the division. Where this tan() has a maximum error in the -1.55 to
    * 1.55 range of 0.02355957, that simpler division technique on the same range has a maximum error of 1.25724030 (about 50 times worse), as well as larger absolute and relative errors. Casting the
    * double result of {@link Math#tan(double)} to float will get the highest precision, but can be anywhere from 2.5x to nearly 4x slower than this, depending on JVM. <br> Based on <a
    * href="https://math.stackexchange.com/a/4453027">this Stack Exchange answer by Soonts</a>.
    *
    * @param radians
    *   a float angle in radians, where 0 to {@link #PI2} is one rotation
    * @return
    *   a float approximation of tan()
    */
  def tan(radians: Float): Float = {
    var r = radians / PI
    r += 0.5f
    r = (r - Math.floor(r)).toFloat
    r -= 0.5f
    r *= PI
    val x2 = radians * radians
    val x4 = x2 * x2
    radians * (0.0010582010582010583f * x4 - 0.1111111111111111f * x2 + 1f) /
      (0.015873015873015872f * x4 - 0.4444444444444444f * x2 + 1f)
    // How we calculated those long constants above (from Stack Exchange, by Soonts):
    // return x * ((1.0/945.0) * x4 - (1.0/9.0) * x2 + 1.0) / ((1.0/63.0) * x4 - (4.0/9.0) * x2 + 1.0);
    // Normally, it would be best to show the division steps, but if GWT isn't computing mathematical constants at
    // compile-time, which I don't know if it does, that would make the shown-division way slower by 4 divisions.
  }

  /** Returns the tangent given an input in degrees, using a Padé approximant. Based on <a href="https://math.stackexchange.com/a/4453027">this Stack Exchange answer</a>.
    *
    * @param degrees
    *   an angle in degrees, where 0 to 360 is one rotation
    * @return
    *   a float approximation of tan()
    */
  def tanDeg(degrees: Float): Float = {
    var r = degrees * (1f / 180f)
    r += 0.5f
    r = (r - Math.floor(r)).toFloat
    r -= 0.5f
    r *= PI
    val x2 = r * r
    val x4 = x2 * x2
    r * (0.0010582010582010583f * x4 - 0.1111111111111111f * x2 + 1f) /
      (0.015873015873015872f * x4 - 0.4444444444444444f * x2 + 1f)
  }

  // ---

  /** A variant on {@link #atan(float)} that does not tolerate infinite inputs for speed reasons. This can be given a double parameter, but is otherwise the same as atan(float), and returns a float
    * like that method. It uses the same approximation, from sheet 11 of "Approximations for Digital Computers." This is mostly meant to be used inside {@link #atan2(float, float)} , but it may be a
    * tiny bit faster than atan(float) in other code.
    * @param i
    *   any finite double or float, but more commonly a float
    * @return
    *   an output from the inverse tangent function, from {@code -HALF_PI} to {@code HALF_PI} inclusive
    */
  def atanUnchecked(i: Double): Float = {
    // We use double precision internally, because some constants need double precision.
    val n = Math.abs(i)
    // c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
    val c = (n - 1.0) / (n + 1.0)
    // The approximation needs 6 odd powers of c.
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * ((Math.PI * 0.25)
      + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11))).toFloat
  }

  /** Close approximation of the frequently-used trigonometric method atan2. Average error is 1.057E-6 radians; maximum error is 1.922E-6. Takes y and x (in that unusual order) as floats, and returns
    * the angle from the origin to that point in radians. It is about 4 times faster than {@link Math#atan2(double, double)} (roughly 15 ns instead of roughly 60 ns for Math, on Java 8 HotSpot). <br>
    * Credit for this goes to the 1955 research study "Approximations for Digital Computers," by RAND Corporation. This is sheet 11's algorithm, which is the fourth-fastest and fourth-least precise.
    * The algorithms on sheets 8-10 are faster, but only by a very small degree, and are considerably less precise. That study provides an {@link #atan(float)} method, and that cleanly translates to
    * atan2().
    * @param y
    *   y-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @param x
    *   x-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @return
    *   the angle to the given point, in radians as a float; ranges from {@code -PI} to {@code PI}
    */
  def atan2(y: Float, x: Float): Float = {
    var n  = y / x
    var x_ = x
    if (n != n)
      n = if y == x then 1f else -1f // if both y and x are infinite, n would be NaN
    else if (n - n != n - n) x_ = 0f // if n is infinite, y is infinitely larger than x.
    if (x_ > 0)
      atanUnchecked(n)
    else if (x_ < 0) {
      if (y >= 0) atanUnchecked(n) + PI
      else atanUnchecked(n) - PI
    } else if (y > 0)
      x + HALF_PI
    else if (y < 0) x - HALF_PI
    else x + y // returns 0 for 0,0 or NaN if either y or x is NaN
  }

  /** A variant on {@link #atanDeg(float)} that does not tolerate infinite inputs for speed reasons. This can be given a double parameter, but is otherwise the same as atanDeg(float), and returns a
    * float like that method. It uses the same approximation, from sheet 11 of "Approximations for Digital Computers." This is mostly meant to be used inside {@link #atan2(float, float)} , but it may
    * be a tiny bit faster than atanDeg(float) in other code.
    * @param i
    *   any finite double or float, but more commonly a float
    * @return
    *   an output from the inverse tangent function in degrees, from {@code -90} to {@code 90} inclusive
    */
  def atanUncheckedDeg(i: Double): Float = {
    // We use double precision internally, because some constants need double precision.
    val n = Math.abs(i)
    // c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
    val c = (n - 1.0) / (n + 1.0)
    // The approximation needs 6 odd powers of c.
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * (45.0 + (57.2944766070562 * c - 19.05792099799635 * c3 + 11.089223410359068 * c5
      - 6.6711120475953765 * c7 + 3.016813013351768 * c9 - 0.6715752908287405 * c11))).toFloat
  }

  /** Close approximation of the frequently-used trigonometric method atan2, using positive or negative degrees. Average absolute error is 0.00006037 degrees; relative error is 0 degrees, maximum
    * error is 0.00010396 degrees. Takes y and x (in that unusual order) as floats, and returns the angle from the origin to that point in degrees. <br> Credit for this goes to the 1955 research study
    * "Approximations for Digital Computers," by RAND Corporation. This is sheet 11's algorithm, which is the fourth-fastest and fourth-least precise. The algorithms on sheets 8-10 are faster, but
    * only by a very small degree, and are considerably less precise. That study provides an {@link #atan(float)} method, and that cleanly translates to atan2().
    * @param y
    *   y-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @param x
    *   x-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @return
    *   the angle to the given point, in degrees as a float; ranges from {@code -180} to {@code 180}
    */
  def atan2Deg(y: Float, x: Float): Float = {
    var n  = y / x
    var x_ = x
    if (n != n)
      n = if y == x then 1f else -1f // if both y and x are infinite, n would be NaN
    else if (n - n != n - n) x_ = 0f // if n is infinite, y is infinitely larger than x.
    if (x_ > 0)
      atanUncheckedDeg(n)
    else if (x_ < 0) {
      if (y >= 0) atanUncheckedDeg(n) + 180.0f
      else atanUncheckedDeg(n) - 180.0f
    } else if (y > 0)
      x + 90f
    else if (y < 0) x - 90f
    else x + y // returns 0 for 0,0 or NaN if either y or x is NaN
  }

  /** Close approximation of the frequently-used trigonometric method atan2, using non-negative degrees only. Average absolute error is 0.00006045 degrees; relative error is 0 degrees; maximum error
    * is 0.00011178 degrees. Takes y and x (in that unusual order) as floats, and returns the angle from the origin to that point in degrees. <br> This can be useful when a negative result from atan()
    * would require extra work to handle. <br> Credit for this goes to the 1955 research study "Approximations for Digital Computers," by RAND Corporation. This is sheet 11's algorithm, which is the
    * fourth-fastest and fourth-least precise. The algorithms on sheets 8-10 are faster, but only by a very small degree, and are considerably less precise. That study provides an {@link #atan(float)}
    * method, and that cleanly translates to atan2Deg360().
    * @param y
    *   y-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @param x
    *   x-component of the point to find the angle towards; note the parameter order is unusual by convention
    * @return
    *   the angle to the given point, in degrees as a float; ranges from {@code 0} to {@code 360}
    */
  def atan2Deg360(y: Float, x: Float): Float = {
    var n  = y / x
    var x_ = x
    if (n != n)
      n = if y == x then 1f else -1.0f // if both y and x are infinite, n would be NaN
    else if (n - n != n - n) x_ = 0f // if n is infinite, y is infinitely larger than x.
    if (x_ > 0) {
      if (y >= 0)
        atanUncheckedDeg(n)
      else
        atanUncheckedDeg(n) + 360.0f
    } else if (x_ < 0) {
      atanUncheckedDeg(n) + 180.0f
    } else if (y > 0)
      x + 90f
    else if (y < 0) x + 270f
    else x + y // returns 0 for 0,0 or NaN if either y or x is NaN
  }

  /** Returns acos in radians; less accurate than Math.acos but may be faster. Average error of 0.00002845 radians (0.0016300649 degrees), largest error of 0.000067548 radians (0.0038702153 degrees).
    * This implementation does not return NaN if given an out-of-range input (Math.acos does return NaN), unless the input is NaN.
    * @param a
    *   acos is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between {@code 0} and {@code PI} when a is in the defined range
    */
  def acos(a: Float): Float = {
    val a2 = a * a // a squared
    val a3 = a * a2 // a cubed
    if (a >= 0f) {
      Math.sqrt(1f - a).toFloat * (1.5707288f - 0.2121144f * a + 0.0742610f * a2 - 0.0187293f * a3)
    } else {
      3.14159265358979323846f -
        Math.sqrt(1f + a).toFloat * (1.5707288f + 0.2121144f * a + 0.0742610f * a2 + 0.0187293f * a3)
    }
  }

  /** Returns asin in radians; less accurate than Math.asin but may be faster. Average error of 0.000028447 radians (0.0016298931 degrees), largest error of 0.000067592 radians (0.0038727364 degrees).
    * This implementation does not return NaN if given an out-of-range input (Math.asin does return NaN), unless the input is NaN.
    * @param a
    *   asin is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between {@code -HALF_PI} and {@code HALF_PI} when a is in the defined range
    */
  def asin(a: Float): Float = {
    val a2 = a * a // a squared
    val a3 = a * a2 // a cubed
    if (a >= 0f) {
      1.5707963267948966f -
        Math.sqrt(1f - a).toFloat * (1.5707288f - 0.2121144f * a + 0.0742610f * a2 - 0.0187293f * a3)
    } else {
      -1.5707963267948966f + Math.sqrt(1f + a).toFloat * (1.5707288f + 0.2121144f * a + 0.0742610f * a2 + 0.0187293f * a3)
    }
  }

  /** Arc tangent approximation with very low error, using an algorithm from the 1955 research study "Approximations for Digital Computers," by RAND Corporation (this is sheet 11's algorithm, which is
    * the fourth-fastest and fourth-least precise). This method is usually about 4x faster than {@link Math#atan(double)} , but is somewhat less precise than Math's implementation. For finite inputs
    * only, you may get a tiny speedup by using {@link #atanUnchecked(double)} , but this method will be correct enough for infinite inputs, and atanUnchecked() will not be.
    * @param i
    *   an input to the inverse tangent function; any float is accepted
    * @return
    *   an output from the inverse tangent function, from {@code -HALF_PI} to {@code HALF_PI} inclusive
    * @see
    *   #atanUnchecked(double) If you know the input will be finite, you can use atanUnchecked() instead.
    */
  def atan(i: Float): Float = {
    // We use double precision internally, because some constants need double precision.
    // This clips infinite inputs at Double.MAX_VALUE, which still probably becomes infinite
    // again when converted back to float.
    val n = Math.min(Math.abs(i), Double.MaxValue)
    // c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
    val c = (n - 1.0) / (n + 1.0)
    // The approximation needs 6 odd powers of c.
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * ((Math.PI * 0.25)
      + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11))).toFloat
  }

  /** Returns arcsine in degrees. This implementation does not return NaN if given an out-of-range input (Math.asin does return NaN), unless the input is NaN.
    * @param a
    *   asin is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between {@code -90} and {@code 90} when a is in the defined range
    */
  def asinDeg(a: Float): Float = {
    val a2 = a * a // a squared
    val a3 = a * a2 // a cubed
    if (a >= 0f) {
      90f - Math.sqrt(1f - a).toFloat *
        (89.99613099964837f - 12.153259893949748f * a + 4.2548418824210055f * a2 - 1.0731098432343729f * a3)
    } else {
      Math.sqrt(1f + a).toFloat *
        (89.99613099964837f + 12.153259893949748f * a + 4.2548418824210055f * a2 + 1.0731098432343729f * a3) - 90f
    }
  }

  /** Returns arccosine in degrees. This implementation does not return NaN if given an out-of-range input (Math.acos does return NaN), unless the input is NaN.
    * @param a
    *   acos is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between {@code 0} and {@code 180} when a is in the defined range
    */
  def acosDeg(a: Float): Float = {
    val a2 = a * a // a squared
    val a3 = a * a2 // a cubed
    if (a >= 0f) {
      Math.sqrt(1f - a).toFloat *
        (89.99613099964837f - 12.153259533621753f * a + 4.254842010910525f * a2 - 1.0731098035209208f * a3)
    } else {
      180f - Math.sqrt(1f + a).toFloat *
        (89.99613099964837f + 12.153259533621753f * a + 4.254842010910525f * a2 + 1.0731098035209208f * a3)
    }
  }

  /** Arc tangent approximation returning a value measured in positive or negative degrees, using an algorithm from the 1955 research study "Approximations for Digital Computers," by RAND Corporation
    * (this is sheet 11's algorithm, which is the fourth-fastest and fourth-least precise). For finite inputs only, you may get a tiny speedup by using {@link #atanUncheckedDeg(double)} , but this
    * method will be correct enough for infinite inputs, and atanUnchecked() will not be.
    * @param i
    *   an input to the inverse tangent function; any float is accepted
    * @return
    *   an output from the inverse tangent function in degrees, from {@code -90} to {@code 90} inclusive
    * @see
    *   #atanUncheckedDeg(double) If you know the input will be finite, you can use atanUncheckedDeg() instead.
    */
  def atanDeg(i: Float): Float = {
    // We use double precision internally, because some constants need double precision.
    // This clips infinite inputs at Double.MAX_VALUE, which still probably becomes infinite
    // again when converted back to float.
    val n = Math.min(Math.abs(i), Double.MaxValue)
    // c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
    val c = (n - 1.0) / (n + 1.0)
    // The approximation needs 6 odd powers of c.
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * (45.0 + (57.2944766070562 * c - 19.05792099799635 * c3 + 11.089223410359068 * c5
      - 6.6711120475953765 * c7 + 3.016813013351768 * c9 - 0.6715752908287405 * c11))).toFloat
  }

  // ---

  val randomGenerator = new Random()

  /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
  def random(range: Int): Int =
    randomGenerator.nextInt(range + 1)

  /** Returns a random number between start (inclusive) and end (inclusive). */
  def random(start: Int, end: Int): Int =
    start + randomGenerator.nextInt(end - start + 1)

  /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
  def random(range: Long): Long =
    // Uses the lower-bounded overload defined below, which is simpler and doesn't lose much optimization.
    random(0L, range)

  /** Returns a random number between start (inclusive) and end (inclusive). */
  def random(start: Long, end: Long): Long = {
    val rand = randomGenerator.nextLong()
    // In order to get the range to go from start to end, instead of overflowing after end and going
    // back around to start, start must be less than end.
    var startVar = start
    var endVar   = end
    if (endVar < startVar) {
      val t = endVar
      endVar = startVar
      startVar = t
    }
    val bound = endVar - startVar + 1L // inclusive on end
    // Credit to https://oroboro.com/large-random-in-range/ for the following technique
    // It's a 128-bit-product where only the upper 64 of 128 bits are used.
    val randLow   = rand & 0xffffffffL
    val boundLow  = bound & 0xffffffffL
    val randHigh  = rand >>> 32
    val boundHigh = bound >>> 32
    startVar + (randHigh * boundLow >>> 32) + (randLow * boundHigh >>> 32) + randHigh * boundHigh
  }

  /** Returns a random boolean value. */
  def randomBoolean(): Boolean =
    randomGenerator.nextBoolean()

  /** Returns true if a random value between 0 and 1 is less than the specified value. */
  def randomBoolean(chance: Float): Boolean =
    MathUtils.random() < chance

  /** Returns random number between 0.0 (inclusive) and 1.0 (exclusive). */
  def random(): Float =
    randomGenerator.nextFloat()

  /** Returns a random number between 0 (inclusive) and the specified value (exclusive). */
  def random(range: Float): Float =
    randomGenerator.nextFloat() * range

  /** Returns a random number between start (inclusive) and end (exclusive). */
  def random(start: Float, end: Float): Float =
    start + randomGenerator.nextFloat() * (end - start)

  /** Returns -1 or 1, randomly. */
  def randomSign(): Int =
    1 | (randomGenerator.nextInt() >> 31)

  /** Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are more likely. <p> This is an optimized version of
    * {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)}
    */
  def randomTriangular(): Float =
    randomGenerator.nextFloat() - randomGenerator.nextFloat()

  /** Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values around zero are more likely. <p> This is an optimized version of
    * {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)}
    * @param max
    *   the upper limit
    */
  def randomTriangular(max: Float): Float =
    (randomGenerator.nextFloat() - randomGenerator.nextFloat()) * max

  /** Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the {@code mode} argument defaults to the midpoint between the bounds, giving
    * a symmetric distribution. <p> This method is equivalent of {@link #randomTriangular(float, float, float) randomTriangular(min, max, (min + max) * 0.5f)}
    * @param min
    *   the lower limit
    * @param max
    *   the upper limit
    */
  def randomTriangular(min: Float, max: Float): Float =
    randomTriangular(min, max, (min + max) * 0.5f)

  /** Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values around {@code mode} are more likely.
    * @param min
    *   the lower limit
    * @param max
    *   the upper limit
    * @param mode
    *   the point around which the values are more likely
    */
  def randomTriangular(min: Float, max: Float, mode: Float): Float = {
    val u = randomGenerator.nextFloat()
    val d = max - min
    if (u <= (mode - min) / d) min + Math.sqrt(u * d * (mode - min)).toFloat
    else max - Math.sqrt((1 - u) * d * (max - mode)).toFloat
  }

  // ---

  /** Returns the next power of two. Returns the specified value if the value is already a power of two. */
  def nextPowerOfTwo(value: Int): Int =
    if (value == 0) 1
    else {
      var v = value
      v -= 1
      v |= v >> 1
      v |= v >> 2
      v |= v >> 4
      v |= v >> 8
      v |= v >> 16
      v + 1
    }

  def isPowerOfTwo(value: Int): Boolean =
    value != 0 && (value & (value - 1)) == 0

  // ---

  def clamp(value: Short, min: Short, max: Short): Short =
    if (value < min) min else if (value > max) max else value

  def clamp(value: Int, min: Int, max: Int): Int =
    if (value < min) min else if (value > max) max else value

  def clamp(value: Long, min: Long, max: Long): Long =
    if (value < min) min else if (value > max) max else value

  def clamp(value: Float, min: Float, max: Float): Float =
    if (value < min) min else if (value > max) max else value

  def clamp(value: Double, min: Double, max: Double): Double =
    if (value < min) min else if (value > max) max else value

  // ---

  /** Linearly interpolates between fromValue to toValue on progress position. */
  def lerp(fromValue: Float, toValue: Float, progress: Float): Float =
    fromValue + (toValue - fromValue) * progress

  /** Linearly normalizes value from a range. Range must not be empty. This is the inverse of {@link #lerp(float, float, float)} .
    * @param rangeStart
    *   Range start normalized to 0
    * @param rangeEnd
    *   Range end normalized to 1
    * @param value
    *   Value to normalize
    * @return
    *   Normalized value. Values outside of the range are not clamped to 0 and 1
    */
  def norm(rangeStart: Float, rangeEnd: Float, value: Float): Float =
    (value - rangeStart) / (rangeEnd - rangeStart)

  /** Linearly map a value from one range to another. Input range must not be empty. This is the same as chaining {@link #norm(float, float, float)} from input range and
    * {@link #lerp(float, float, float)} to output range.
    * @param inRangeStart
    *   Input range start
    * @param inRangeEnd
    *   Input range end
    * @param outRangeStart
    *   Output range start
    * @param outRangeEnd
    *   Output range end
    * @param value
    *   Value to map
    * @return
    *   Mapped value. Values outside of the input range are not clamped to output range
    */
  def map(inRangeStart: Float, inRangeEnd: Float, outRangeStart: Float, outRangeEnd: Float, value: Float): Float =
    outRangeStart + (value - inRangeStart) * (outRangeEnd - outRangeStart) / (inRangeEnd - inRangeStart)

  /** Linearly interpolates between two angles in radians. Takes into account that angles wrap at two pi and always takes the direction with the smallest delta angle.
    *
    * @param fromRadians
    *   start angle in radians
    * @param toRadians
    *   target angle in radians
    * @param progress
    *   interpolation value in the range [0, 1]
    * @return
    *   the interpolated angle in the range [0, PI2[
    */
  def lerpAngle(fromRadians: Float, toRadians: Float, progress: Float): Float = {
    val delta = (((toRadians - fromRadians) % PI2 + PI2 + PI) % PI2) - PI
    ((fromRadians + delta * progress) % PI2 + PI2) % PI2
  }

  /** Linearly interpolates between two angles in degrees. Takes into account that angles wrap at 360 degrees and always takes the direction with the smallest delta angle.
    *
    * @param fromDegrees
    *   start angle in degrees
    * @param toDegrees
    *   target angle in degrees
    * @param progress
    *   interpolation value in the range [0, 1]
    * @return
    *   the interpolated angle in the range [0, 360[
    */
  def lerpAngleDeg(fromDegrees: Float, toDegrees: Float, progress: Float): Float = {
    val delta = (((toDegrees - fromDegrees) % 360f + 360f + 180f) % 360f) - 180f
    ((fromDegrees + delta * progress) % 360f + 360f) % 360f
  }

  // ---

  private val BIG_ENOUGH_INT   = 16 * 1024
  private val BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT.toDouble
  private val CEIL             = 0.9999999
  private val BIG_ENOUGH_CEIL  = 16384.999999999996
  private val BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5f

  /** Returns the largest integer less than or equal to the specified float. This method will only properly floor floats from -(2^14) to (Float.MAX_VALUE - 2^14).
    */
  def floor(value: Float): Int =
    (value + BIG_ENOUGH_FLOOR).toInt - BIG_ENOUGH_INT

  /** Returns the largest integer less than or equal to the specified float. This method will only properly floor floats that are positive. Note this method simply casts the float to int.
    */
  def floorPositive(value: Float): Int =
    value.toInt

  /** Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats from -(2^14) to (Float.MAX_VALUE - 2^14).
    */
  def ceil(value: Float): Int =
    BIG_ENOUGH_INT - (BIG_ENOUGH_FLOOR - value).toInt

  /** Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats that are positive.
    */
  def ceilPositive(value: Float): Int =
    (value + CEIL).toInt

  /** Returns the closest integer to the specified float. This method will only properly round floats from -(2^14) to (Float.MAX_VALUE - 2^14).
    */
  def round(value: Float): Int =
    (value + BIG_ENOUGH_ROUND).toInt - BIG_ENOUGH_INT

  /** Returns the closest integer to the specified float. This method will only properly round floats that are positive. */
  def roundPositive(value: Float): Int =
    (value + 0.5f).toInt

  /** Returns true if the value is zero (using the default tolerance as upper bound) */
  def isZero(value: Float): Boolean =
    Math.abs(value) <= FLOAT_ROUNDING_ERROR

  /** Returns true if the value is zero.
    * @param tolerance
    *   represent an upper bound below which the value is considered zero.
    */
  def isZero(value: Float, tolerance: Float): Boolean =
    Math.abs(value) <= tolerance

  /** Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
    * @param a
    *   the first value.
    * @param b
    *   the second value.
    */
  def isEqual(a: Float, b: Float): Boolean =
    Math.abs(a - b) <= FLOAT_ROUNDING_ERROR

  /** Returns true if a is nearly equal to b.
    * @param a
    *   the first value.
    * @param b
    *   the second value.
    * @param tolerance
    *   represent an upper bound below which the two values are considered equal.
    */
  def isEqual(a: Float, b: Float, tolerance: Float): Boolean =
    Math.abs(a - b) <= tolerance

  /** @return the logarithm of value with base a */
  def log(a: Float, value: Float): Float =
    (Math.log(value) / Math.log(a)).toFloat

  /** @return the logarithm of value with base 2 */
  def log2(value: Float): Float =
    log(2, value)
}
