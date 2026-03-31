/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful

/** Trigonometric approximations for sin(), cos(), tan(), and atan2(), with variants that allow users to trade speed for precision or vice versa. This is primarily derived from libGDX's MathUtils
  * class.
  *
  * This contains two fairly-sizeable lookup tables (in total RAM usage, about 128KB); one stores 16385 results of sin() and the other stores 16385 results of cos().
  */
object TrigTools {

  /** The float value closest to pi. */
  val PI: Float = Math.PI.toFloat

  /** 1.0f divided by PI. */
  val PI_INVERSE: Float = (1.0 / Math.PI).toFloat

  /** 2f times PI; the same as TAU. */
  val PI2: Float = PI * 2f

  /** 2f times PI; the same as PI2. */
  val TAU: Float = PI2

  /** PI divided by 2f. */
  val HALF_PI: Float = PI * 0.5f

  /** PI divided by 2f; the same as HALF_PI. */
  val ETA: Float = HALF_PI

  /** PI divided by 4f. */
  val QUARTER_PI: Float = PI * 0.25f

  /** The hard-coded size of SIN_TABLE and COS_TABLE in bits; this is 14 now. */
  val TABLE_BITS: Int = 14

  /** The size of SIN_TABLE. */
  val TABLE_SIZE: Int = 1 << TABLE_BITS

  /** If you add this to an index used in SIN_TABLE, you get the result of the cosine instead. */
  val SIN_TO_COS: Int = TABLE_SIZE >>> 2

  /** Equivalent to SIN_TO_COS. */
  val QUARTER_CIRCLE_INDEX: Int = SIN_TO_COS

  /** The bitmask that can be used to confine any int to wrap within TABLE_SIZE. */
  val TABLE_MASK: Int = TABLE_SIZE - 1

  /** Multiply by this to convert from radians to an index in SIN_TABLE. */
  val radToIndex: Float = TABLE_SIZE / PI2

  /** Multiply by this to convert from degrees to an index in SIN_TABLE. */
  val degToIndex: Float = TABLE_SIZE / 360f

  /** Multiply by this to convert from turns to an index in SIN_TABLE. */
  val turnToIndex: Float = TABLE_SIZE.toFloat

  /** Multiply by this to convert from radians to degrees. */
  val radiansToDegrees: Float = (180.0 / Math.PI).toFloat

  /** Multiply by this to convert from degrees to radians. */
  val degreesToRadians: Float = (Math.PI / 180.0).toFloat

  /** A precalculated table of 16385 floats for sin(). */
  val SIN_TABLE: Array[Float] = new Array[Float](TABLE_SIZE + 1)

  /** A precalculated table of 16385 floats for cos(). */
  val COS_TABLE: Array[Float] = new Array[Float](TABLE_SIZE + 1)

  // Initialize lookup tables
  locally {
    var i = 0
    while (i <= TABLE_SIZE) {
      val theta = i.toFloat / TABLE_SIZE * PI2
      SIN_TABLE(i) = sinPrecise(theta)
      COS_TABLE(i) = cosPrecise(theta)
      i += 1
    }
    // The four right angles get extra-precise values
    SIN_TABLE(0) = 0f
    SIN_TABLE(QUARTER_CIRCLE_INDEX) = 1f
    SIN_TABLE(QUARTER_CIRCLE_INDEX * 2) = 0f
    SIN_TABLE(QUARTER_CIRCLE_INDEX * 3) = -1.0f
    SIN_TABLE(QUARTER_CIRCLE_INDEX * 4) = 0f

    COS_TABLE(0) = 1f
    COS_TABLE(QUARTER_CIRCLE_INDEX) = 0f
    COS_TABLE(QUARTER_CIRCLE_INDEX * 2) = -1f
    COS_TABLE(QUARTER_CIRCLE_INDEX * 3) = 0f
    COS_TABLE(QUARTER_CIRCLE_INDEX * 4) = 1f
  }

  /** Gets an approximation of the sine of radians using the lookup table. */
  def sin(radians: Float): Float = {
    val r      = radians * radToIndex
    val floor  = (r + 16384f).toInt - 16384
    val masked = floor & TABLE_MASK
    val from   = SIN_TABLE(masked)
    val to     = SIN_TABLE(masked + 1)
    from + (to - from) * (r - floor)
  }

  /** Gets an approximation of the cosine of radians using the lookup table. */
  def cos(radians: Float): Float = {
    val r      = Math.abs(radians) * radToIndex
    val floor  = r.toInt
    val masked = floor & TABLE_MASK
    val from   = COS_TABLE(masked)
    val to     = COS_TABLE(masked + 1)
    from + (to - from) * (r - floor)
  }

  /** Gets an approximation of the tangent of radians. */
  def tan(radians: Float): Float = {
    var r      = radians * radToIndex
    val floor  = (r + 16384.0).toInt - 16384
    val masked = floor & TABLE_MASK
    r -= floor
    val fromS = SIN_TABLE(masked); val toS = SIN_TABLE(masked + 1)
    val fromC = COS_TABLE(masked); val toC = COS_TABLE(masked + 1)
    (fromS + (toS - fromS) * r) / (fromC + (toC - fromC) * r)
  }

  /** Gets an approximation of the sine of degrees. */
  def sinDeg(degrees: Float): Float = {
    val d      = degrees * degToIndex
    val floor  = (d + 16384f).toInt - 16384
    val masked = floor & TABLE_MASK
    val from   = SIN_TABLE(masked); val to = SIN_TABLE(masked + 1)
    from + (to - from) * (d - floor)
  }

  /** Gets an approximation of the cosine of degrees. */
  def cosDeg(degrees: Float): Float = {
    val d      = Math.abs(degrees) * degToIndex
    val floor  = d.toInt
    val masked = floor & TABLE_MASK
    val from   = COS_TABLE(masked); val to = COS_TABLE(masked + 1)
    from + (to - from) * (d - floor)
  }

  /** Gets an approximation of the sine of turns. */
  def sinTurns(turns: Float): Float = {
    val t      = turns * turnToIndex
    val floor  = (t + 16384f).toInt - 16384
    val masked = floor & TABLE_MASK
    val from   = SIN_TABLE(masked); val to = SIN_TABLE(masked + 1)
    from + (to - from) * (t - floor)
  }

  /** Gets an approximation of the cosine of turns. */
  def cosTurns(turns: Float): Float = {
    val t      = Math.abs(turns) * turnToIndex
    val floor  = t.toInt
    val masked = floor & TABLE_MASK
    val from   = COS_TABLE(masked); val to = COS_TABLE(masked + 1)
    from + (to - from) * (t - floor)
  }

  /** A non-tabular sine approximation in radians, accurate to within two ULPs for inputs in the 0 to PI2 range. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def sinPrecise(radians: Float): Float = {
    val x        = Math.abs(radians)
    val quadrant = (0.6366197723675814f * x + 0.5f).toInt
    val xr       = ((x - quadrant * 1.5703125f) - quadrant * 0.0004837512969970703125f) - quadrant * 7.549789948768648e-8f
    val x2       = xr * xr
    (quadrant ^ (java.lang.Float.floatToRawIntBits(radians) >>> 30 & 2)) & 3 match {
      case 0 => ((-1.9515295891e-4f * x2 + 8.3321608736e-3f) * x2 - 1.6666654611e-1f) * x2 * xr + xr
      case 1 => ((2.443315711809948e-5f * x2 - 1.388731625493765e-3f) * x2 + 4.166664568298827e-2f) * x2 * x2 - 0.5f * x2 + 1f
      case 2 => ((1.9515295891e-4f * x2 - 8.3321608736e-3f) * x2 + 1.6666654611e-1f) * x2 * xr - xr
      case _ => ((-2.443315711809948e-5f * x2 + 1.388731625493765e-3f) * x2 - 4.166664568298827e-2f) * x2 * x2 + 0.5f * x2 - 1f
    }
  }

  /** A non-tabular cosine approximation in radians, accurate to within two ULPs for inputs in the 0 to PI2 range. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L.
    * Moshier.
    */
  def cosPrecise(radians: Float): Float = {
    val x        = Math.abs(radians)
    val quadrant = (0.6366197723675814f * x + 0.5f).toInt
    val xr       = ((x - quadrant * 1.5703125f) - quadrant * 0.0004837512969970703125f) - quadrant * 7.549789948768648e-8f
    val x2       = xr * xr
    quadrant & 3 match {
      case 3 => ((-1.9515295891e-4f * x2 + 8.3321608736e-3f) * x2 - 1.6666654611e-1f) * x2 * xr + xr
      case 0 => ((2.443315711809948e-5f * x2 - 1.388731625493765e-3f) * x2 + 4.166664568298827e-2f) * x2 * x2 - 0.5f * x2 + 1f
      case 1 => ((1.9515295891e-4f * x2 - 8.3321608736e-3f) * x2 + 1.6666654611e-1f) * x2 * xr - xr
      case _ => ((-2.443315711809948e-5f * x2 + 1.388731625493765e-3f) * x2 - 4.166664568298827e-2f) * x2 * x2 + 0.5f * x2 - 1f
    }
  }

  /** The atan2Turns() function takes y first, then x, and returns the angle in turns from the origin to the given point. Returns a float from 0.0 to 1.0, counterclockwise when y points up.
    */
  def atan2Turns(y: Float, x: Float): Float =
    if (y == 0f && x >= 0f) 0f
    else {
      val ay     = Math.abs(y)
      val ax     = Math.abs(x)
      val invert = ay > ax
      var z      = if (invert) ax / ay else ay / ax
      val s      = z * z
      z *= (((((((-6.452233507864792e-4f * s + 0.003479322672037479f) * s - 0.008898334876790684f) * s + 0.015345726331929417f) * s - 0.022136118977856264f) *
        s + 0.03174589869088148f) * s - 0.05304611397922089f) * s + 0.15915483874178302f)
      if (invert) z = 0.25f - z
      if (x < 0f) z = 0.5f - z
      if (y < 0f) 1f - z else z
    }
}
