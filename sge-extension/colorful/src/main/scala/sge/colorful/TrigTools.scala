/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 652
 * Covenant-baseline-methods: COS_TABLE,ETA,ETA_D,HALF_PI,HALF_PI_D,PI,PI2,PI_INVERSE,QUARTER_CIRCLE_INDEX,QUARTER_PI,QUARTER_PI_D,SIN_TABLE,SIN_TO_COS,TABLE_BITS,TABLE_MASK,TABLE_SIZE,TAU,TrigTools,acos,acosDeg,acosTurns,asin,asinDeg,asinTurns,atan,atan2,atan2Deg,atan2Deg360,atan2Turns,atanDeg,atanTurns,c,c11,c2,c3,c5,c7,c9,cos,cosDeg,cosDegPrecise,cosPrecise,cosTurns,cosTurnsPrecise,d,degToIndex,degreesToRadians,degreesToTableIndex,floor,from,fromC,fromS,i,masked,n,p,quadrant,r,radToIndex,radiansToDegrees,radiansToTableIndex,sin,sinDeg,sinDegPrecise,sinPrecise,sinTurns,sinTurnsPrecise,t,tan,tanDeg,tanDegPrecise,tanPrecise,tanTurns,tanTurnsPrecise,to,turnToIndex,turnsToTableIndex,x,x2,xr
 * Covenant-source-reference: com/github/tommyettinger/colorful/TrigTools.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful

/** Trigonometric approximations for sin(), cos(), tan(), asin(), acos(), atan(), and atan2(), with variants for most that allow users to trade speed for precision or vice versa. This is primarily
  * derived from libGDX's MathUtils class. Unlike MathUtils, it exposes much more of its internal data, trusting that users can know what they are doing. A key difference here is that all methods have
  * variants that treat angles as radians, as degrees, and as turns. That is, while a full rotation around a circle is `2.0 * PI` radians, it is 360.0 degrees, and it is 1.0 turns.
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

  /** Math.PI divided by 2.0; the same as ETA_D. */
  val HALF_PI_D: Double = Math.PI * 0.5

  /** Math.PI divided by 2.0; the same as HALF_PI_D. */
  val ETA_D: Double = HALF_PI_D

  /** PI divided by 4f. */
  val QUARTER_PI: Float = PI * 0.25f

  /** Math.PI divided by 4.0. */
  val QUARTER_PI_D: Double = Math.PI * 0.25

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

  /** Gets an approximation of the tangent of degrees. */
  def tanDeg(degrees: Float): Float = {
    var d      = degrees * degToIndex
    val floor  = (d + 16384.0).toInt - 16384
    val masked = floor & TABLE_MASK
    d -= floor
    val fromS = SIN_TABLE(masked); val toS = SIN_TABLE(masked + 1)
    val fromC = COS_TABLE(masked); val toC = COS_TABLE(masked + 1)
    (fromS + (toS - fromS) * d) / (fromC + (toC - fromC) * d)
  }

  /** Gets an approximation of the tangent of turns. */
  def tanTurns(turns: Float): Float = {
    var t      = turns * turnToIndex
    val floor  = (t + 16384.0).toInt - 16384
    val masked = floor & TABLE_MASK
    t -= floor
    val fromS = SIN_TABLE(masked); val toS = SIN_TABLE(masked + 1)
    val fromC = COS_TABLE(masked); val toC = COS_TABLE(masked + 1)
    (fromS + (toS - fromS) * t) / (fromC + (toC - fromC) * t)
  }

  /** A non-tabular sine approximation in degrees, accurate to within two ULPs for inputs in the 0 to 360 range. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def sinDegPrecise(degrees: Float): Float = {
    val x        = Math.abs(degrees)
    val quadrant = (0.011111111f * x + 0.5f).toInt
    val xr       = (x - quadrant * 90f) * (HALF_PI / 90f)
    val x2       = xr * xr
    (quadrant ^ (java.lang.Float.floatToRawIntBits(degrees) >>> 30 & 2)) & 3 match {
      case 0 => ((-1.9515295891e-4f * x2 + 8.3321608736e-3f) * x2 - 1.6666654611e-1f) * x2 * xr + xr
      case 1 => ((2.443315711809948e-5f * x2 - 1.388731625493765e-3f) * x2 + 4.166664568298827e-2f) * x2 * x2 - 0.5f * x2 + 1f
      case 2 => ((1.9515295891e-4f * x2 - 8.3321608736e-3f) * x2 + 1.6666654611e-1f) * x2 * xr - xr
      case _ => ((-2.443315711809948e-5f * x2 + 1.388731625493765e-3f) * x2 - 4.166664568298827e-2f) * x2 * x2 + 0.5f * x2 - 1f
    }
  }

  /** A non-tabular cosine approximation in degrees, accurate to within two ULPs for inputs in the 0 to 360 range. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L.
    * Moshier.
    */
  def cosDegPrecise(degrees: Float): Float = {
    val x        = Math.abs(degrees)
    val quadrant = (0.011111111f * x + 0.5f).toInt
    val xr       = (x - quadrant * 90f) * (HALF_PI / 90f)
    val x2       = xr * xr
    quadrant & 3 match {
      case 3 => ((-1.9515295891e-4f * x2 + 8.3321608736e-3f) * x2 - 1.6666654611e-1f) * x2 * xr + xr
      case 0 => ((2.443315711809948e-5f * x2 - 1.388731625493765e-3f) * x2 + 4.166664568298827e-2f) * x2 * x2 - 0.5f * x2 + 1f
      case 1 => ((1.9515295891e-4f * x2 - 8.3321608736e-3f) * x2 + 1.6666654611e-1f) * x2 * xr - xr
      case _ => ((-2.443315711809948e-5f * x2 + 1.388731625493765e-3f) * x2 - 4.166664568298827e-2f) * x2 * x2 + 0.5f * x2 - 1f
    }
  }

  /** A non-tabular sine approximation in turns, accurate to within two ULPs for inputs in the 0 to 1 range. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def sinTurnsPrecise(turns: Float): Float = {
    val x        = Math.abs(turns)
    val quadrant = (4f * x + 0.5f).toInt
    val xr       = (x - quadrant * 0.25f) * PI2
    val x2       = xr * xr
    (quadrant ^ (java.lang.Float.floatToRawIntBits(turns) >>> 30 & 2)) & 3 match {
      case 0 => ((-1.9515295891e-4f * x2 + 8.3321608736e-3f) * x2 - 1.6666654611e-1f) * x2 * xr + xr
      case 1 => ((2.443315711809948e-5f * x2 - 1.388731625493765e-3f) * x2 + 4.166664568298827e-2f) * x2 * x2 - 0.5f * x2 + 1f
      case 2 => ((1.9515295891e-4f * x2 - 8.3321608736e-3f) * x2 + 1.6666654611e-1f) * x2 * xr - xr
      case _ => ((-2.443315711809948e-5f * x2 + 1.388731625493765e-3f) * x2 - 4.166664568298827e-2f) * x2 * x2 + 0.5f * x2 - 1f
    }
  }

  /** A non-tabular cosine approximation in turns, accurate to within two ULPs for inputs in the 0 to 1 range. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def cosTurnsPrecise(turns: Float): Float = {
    val x        = Math.abs(turns)
    val quadrant = (4f * x + 0.5f).toInt
    val xr       = (x - quadrant * 0.25f) * PI2
    val x2       = xr * xr
    quadrant & 3 match {
      case 3 => ((-1.9515295891e-4f * x2 + 8.3321608736e-3f) * x2 - 1.6666654611e-1f) * x2 * xr + xr
      case 0 => ((2.443315711809948e-5f * x2 - 1.388731625493765e-3f) * x2 + 4.166664568298827e-2f) * x2 * x2 - 0.5f * x2 + 1f
      case 1 => ((1.9515295891e-4f * x2 - 8.3321608736e-3f) * x2 + 1.6666654611e-1f) * x2 * xr - xr
      case _ => ((-2.443315711809948e-5f * x2 + 1.388731625493765e-3f) * x2 - 4.166664568298827e-2f) * x2 * x2 + 0.5f * x2 - 1f
    }
  }

  /** Returns the tangent in radians; non-tabular and very precise, but about half as fast as [[tan]]. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def tanPrecise(radians: Float): Float = {
    val x        = Math.abs(radians)
    val quadrant = (0.6366197723675814f * x + 0.5f).toInt
    val xr       = ((x - quadrant * 1.5703125f) - quadrant * 0.0004837512969970703125f) - quadrant * 7.549789948768648e-8f
    val x2       = xr * xr
    val p        = (((((9.38540185543e-3f * x2 + 3.11992232697e-3f) * x2 + 2.44301354525e-2f) * x2 +
      5.34112807005e-2f) * x2 + 1.33387994085e-1f) * x2 + 3.33331568548e-1f) * x2 * xr + xr
    if ((quadrant & 1) == 1) -Math.signum(radians) / p
    else Math.signum(radians) * p
  }

  /** Returns the tangent in degrees; non-tabular and very precise. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def tanDegPrecise(degrees: Float): Float = {
    val x        = Math.abs(degrees)
    val quadrant = (0.011111111f * x + 0.5f).toInt
    val xr       = (x - quadrant * 90f) * (HALF_PI / 90f)
    val x2       = xr * xr
    val p        = (((((9.38540185543e-3f * x2 + 3.11992232697e-3f) * x2 + 2.44301354525e-2f) * x2 +
      5.34112807005e-2f) * x2 + 1.33387994085e-1f) * x2 + 3.33331568548e-1f) * x2 * xr + xr
    if ((quadrant & 1) == 1) -Math.signum(degrees) / p
    else Math.signum(degrees) * p
  }

  /** Returns the tangent in turns; non-tabular and very precise. Based on Jolt Physics by Jorrit Rouwe (MIT-licensed), using code by Stephen L. Moshier.
    */
  def tanTurnsPrecise(turns: Float): Float = {
    val x        = Math.abs(turns)
    val quadrant = (4 * x + 0.5f).toInt
    val xr       = (x - quadrant * 0.25f) * PI2
    val x2       = xr * xr
    val p        = (((((9.38540185543e-3f * x2 + 3.11992232697e-3f) * x2 + 2.44301354525e-2f) * x2 +
      5.34112807005e-2f) * x2 + 1.33387994085e-1f) * x2 + 3.33331568548e-1f) * x2 * xr + xr
    if ((quadrant & 1) == 1) -Math.signum(turns) / p
    else Math.signum(turns) * p
  }

  /** Converts radians to an index that can be used in [[SIN_TABLE]] or [[COS_TABLE]].
    * @param radians
    *   an angle in radians; may be positive or negative
    * @return
    *   the index into [[SIN_TABLE]] of the sine of radians
    */
  def radiansToTableIndex(radians: Float): Int =
    (radians * radToIndex + 16384.5f).toInt & TABLE_MASK

  /** Converts degrees to an index that can be used in [[SIN_TABLE]] or [[COS_TABLE]].
    * @param degrees
    *   an angle in degrees; may be positive or negative
    * @return
    *   the index into [[SIN_TABLE]] of the sine of degrees
    */
  def degreesToTableIndex(degrees: Float): Int =
    (degrees * degToIndex + 16384.5f).toInt & TABLE_MASK

  /** Converts turns to an index that can be used in [[SIN_TABLE]] or [[COS_TABLE]].
    * @param turns
    *   an angle in turns; may be positive or negative
    * @return
    *   the index into [[SIN_TABLE]] of the sine of turns
    */
  def turnsToTableIndex(turns: Float): Int =
    (turns * turnToIndex + 16384.5f).toInt & TABLE_MASK

  /** Arc tangent approximation with very low error, using an algorithm from the 1955 RAND Corporation study "Approximations for Digital Computers" (sheet 11). Usually about 4x faster than
    * `Math.atan`.
    *
    * @param i
    *   an input to the inverse tangent function; any float is accepted
    * @return
    *   an output from the inverse tangent function in radians, from `-HALF_PI` to `HALF_PI` inclusive
    */
  def atan(i: Float): Float = {
    // We use double precision internally, because some constants need double precision.
    val n   = Math.min(Math.abs(i), Double.MaxValue)
    val c   = (n - 1.0) / (n + 1.0)
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * (QUARTER_PI_D
      + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11))).toFloat
  }

  /** Arc tangent approximation returning a value measured in positive or negative degrees, using an algorithm from the 1955 RAND Corporation study "Approximations for Digital Computers" (sheet 11).
    *
    * @param i
    *   an input to the inverse tangent function; any float is accepted
    * @return
    *   an output from the inverse tangent function in degrees, from `-90` to `90` inclusive
    */
  def atanDeg(i: Float): Float = {
    val n   = Math.min(Math.abs(i), Double.MaxValue)
    val c   = (n - 1.0) / (n + 1.0)
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * (45.0
      + (57.2944766070562 * c - 19.05792099799635 * c3 + 11.089223410359068 * c5 - 6.6711120475953765 * c7 + 3.016813013351768 * c9 - 0.6715752908287405 * c11))).toFloat
  }

  /** Arc tangent approximation with very low error, using an algorithm from the 1955 RAND Corporation study "Approximations for Digital Computers" (sheet 11).
    *
    * @param i
    *   an input to the inverse tangent function; any float is accepted
    * @return
    *   an output from the inverse tangent function in turns, from `-0.25` to `0.25` inclusive
    */
  def atanTurns(i: Float): Float = {
    val n   = Math.min(Math.abs(i), Double.MaxValue)
    val c   = (n - 1.0) / (n + 1.0)
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    (Math.signum(i) * (0.125
      + (0.15915132390848943 * c - 0.052938669438878753 * c3 + 0.030803398362108523 * c5
        - 0.01853086679887605 * c7 + 0.008380036148199356 * c9 - 0.0018654869189687236 * c11))).toFloat
  }

  /** Arc tangent approximation (double precision) with very low error, using an algorithm from the 1955 RAND Corporation study "Approximations for Digital Computers" (sheet 11).
    *
    * @param i
    *   an input to the inverse tangent function; any double is accepted
    * @return
    *   an output from the inverse tangent function in radians, from `-HALF_PI` to `HALF_PI` inclusive
    */
  def atan(i: Double): Double = {
    val n   = Math.min(Math.abs(i), Double.MaxValue)
    val c   = (n - 1.0) / (n + 1.0)
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    Math.signum(i) * (QUARTER_PI_D
      + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11))
  }

  /** Arc tangent approximation (double precision) returning a value measured in positive or negative degrees.
    *
    * @param i
    *   an input to the inverse tangent function; any double is accepted
    * @return
    *   an output from the inverse tangent function in degrees, from `-90` to `90` inclusive
    */
  def atanDeg(i: Double): Double = {
    val n   = Math.min(Math.abs(i), Double.MaxValue)
    val c   = (n - 1.0) / (n + 1.0)
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    Math.signum(i) * (45.0
      + (57.2944766070562 * c - 19.05792099799635 * c3 + 11.089223410359068 * c5 - 6.6711120475953765 * c7 + 3.016813013351768 * c9 - 0.6715752908287405 * c11))
  }

  /** Arc tangent approximation (double precision) with very low error.
    *
    * @param i
    *   an input to the inverse tangent function; any double is accepted
    * @return
    *   an output from the inverse tangent function in turns, from `-0.25` to `0.25` inclusive
    */
  def atanTurns(i: Double): Double = {
    val n   = Math.min(Math.abs(i), Double.MaxValue)
    val c   = (n - 1.0) / (n + 1.0)
    val c2  = c * c
    val c3  = c * c2
    val c5  = c3 * c2
    val c7  = c5 * c2
    val c9  = c7 * c2
    val c11 = c9 * c2
    Math.signum(i) * (0.125
      + (0.15915132390848943 * c - 0.052938669438878753 * c3 + 0.030803398362108523 * c5
        - 0.01853086679887605 * c7 + 0.008380036148199356 * c9 - 0.0018654869189687236 * c11))
  }

  /** A fast approximation of atan2() that is only defined for finite input arguments. Returns the angle in radians from the origin to the given point.
    *
    * @param y
    *   any finite float; note the unusual argument order (y is first here!)
    * @param x
    *   any finite float; note the unusual argument order (x is second here!)
    * @return
    *   the angle in radians from the origin to the given point
    */
  def atan2(y: Float, x: Float): Float =
    if (y == 0f && x >= 0f) y
    else {
      val ay     = Math.abs(y)
      val ax     = Math.abs(x)
      val invert = ay > ax
      var z      = if (invert) ax / ay else ay / ax
      val s      = z * z
      z *= (((((((-0.004054058f * s + 0.0218612288f) * s - 0.0559098861f) * s + 0.0964200441f) *
        s - 0.1390853351f) * s + 0.1994653599f) * s - 0.3332985605f) * s + 0.9999993329f)
      if (invert) z = HALF_PI - z
      if (x < 0f) z = PI - z
      if (y < 0f) -z else z
    }

  /** A fast approximation of atan2() in degrees that is only defined for finite input arguments.
    *
    * @param y
    *   any finite float; note the unusual argument order (y is first here!)
    * @param x
    *   any finite float; note the unusual argument order (x is second here!)
    * @return
    *   the angle in degrees from the origin to the given point
    */
  def atan2Deg(y: Float, x: Float): Float =
    if (y == 0f && x >= 0f) y
    else {
      val ay     = Math.abs(y)
      val ax     = Math.abs(x)
      val invert = ay > ax
      var z      = if (invert) ax / ay else ay / ax
      val s      = z * z
      z *= ((((((-0.2322804062831325f * s + 1.2525561619334924f) * s - 3.2034005556446465f) * s + 5.52446147949459f) *
        s - 7.969002832028255f) * s + 11.428523528717331f) * s - 19.09660103251952f) * s + 57.29574194704188f
      if (invert) z = 90f - z
      if (x < 0f) z = 180f - z
      if (y < 0f) -z else z
    }

  /** A fast approximation of atan2() in non-negative degrees that is only defined for finite input arguments. Returns a float from 0.0 to 360.0, counterclockwise when y points up.
    *
    * @param y
    *   any finite float; note the unusual argument order (y is first here!)
    * @param x
    *   any finite float; note the unusual argument order (x is second here!)
    * @return
    *   the angle in degrees from the origin to the given point, from 0 to 360
    */
  def atan2Deg360(y: Float, x: Float): Float =
    if (y == 0f && x >= 0f) 0f
    else {
      val ay     = Math.abs(y)
      val ax     = Math.abs(x)
      val invert = ay > ax
      var z      = if (invert) ax / ay else ay / ax
      val s      = z * z
      z *= ((((((-0.2322804062831325f * s + 1.2525561619334924f) * s - 3.2034005556446465f) * s + 5.52446147949459f) * s - 7.969002832028255f) *
        s + 11.428523528717331f) * s - 19.09660103251952f) * s + 57.29574194704188f
      if (invert) z = 90f - z
      if (x < 0f) z = 180f - z
      if (y < 0f) 360f - z else z
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

  /** Returns arcsine in radians. This implementation does not return NaN if given an out-of-range input (Math.asin does return NaN), unless the input is NaN.
    *
    * @param a
    *   asin is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between `-HALF_PI` and `HALF_PI` when a is in the defined range
    */
  def asin(a: Float): Float =
    if (a >= 0f) {
      (HALF_PI_D - Math.sqrt(1.0 - a) * (1.5707288 + a * (-0.2121144 + a * (0.0742610 + a * -0.0187293)))).toFloat
    } else {
      (Math.sqrt(1.0 + a) * (1.5707288 + a * (0.2121144 + a * (0.0742610 + a * 0.0187293))) - HALF_PI_D).toFloat
    }

  /** Returns arcsine in degrees. This implementation does not return NaN if given an out-of-range input (Math.asin does return NaN), unless the input is NaN.
    *
    * @param a
    *   asin is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between `-90` and `90` when a is in the defined range
    */
  def asinDeg(a: Float): Float =
    if (a >= 0f) {
      (90.0 - Math.sqrt(1.0 - a) * (89.99613099964837 + a * (-12.153259893949748 + a * (4.2548418824210055 + a * -1.0731098432343729)))).toFloat
    } else {
      (Math.sqrt(1.0 + a) * (89.99613099964837 + a * (12.153259893949748 + a * (4.2548418824210055 + a * 1.0731098432343729))) - 90.0).toFloat
    }

  /** Returns arcsine in turns. This implementation does not return NaN if given an out-of-range input (Math.asin does return NaN), unless the input is NaN. Note that unlike [[atan2Turns]], this can
    * return negative turn values.
    *
    * @param a
    *   asin is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between `-0.25` and `0.25` when a is in the defined range
    */
  def asinTurns(a: Float): Float =
    if (a >= 0f) {
      (0.25 - Math.sqrt(1.0 - a) * (0.24998925277680104 + a * (-0.033759055260971525 + a * (0.011819005228947238 + a * -0.0029808606756510357)))).toFloat
    } else {
      (Math.sqrt(1.0 + a) * (0.24998925277680104 + a * (0.033759055260971525 + a * (0.011819005228947238 + a * 0.0029808606756510357))) - 0.25).toFloat
    }

  /** Returns arccosine in radians. This implementation does not return NaN if given an out-of-range input (Math.acos does return NaN), unless the input is NaN.
    *
    * @param a
    *   acos is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between `0` and `PI` when a is in the defined range
    */
  def acos(a: Float): Float =
    if (a >= 0f) {
      (Math.sqrt(1.0 - a) * (1.5707288 + a * (-0.2121144 + a * (0.0742610 + a * -0.0187293)))).toFloat
    } else {
      (Math.PI - Math.sqrt(1.0 + a) * (1.5707288 + a * (0.2121144 + a * (0.0742610 + a * 0.0187293)))).toFloat
    }

  /** Returns arccosine in degrees. This implementation does not return NaN if given an out-of-range input (Math.acos does return NaN), unless the input is NaN.
    *
    * @param a
    *   acos is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between `0` and `180` when a is in the defined range
    */
  def acosDeg(a: Float): Float =
    if (a >= 0f) {
      (Math.sqrt(1.0 - a) * (89.99613099964837 + a * (-12.153259533621753 + a * (4.254842010910525 + a * -1.0731098035209208)))).toFloat
    } else {
      (180.0 - Math.sqrt(1.0 + a) * (89.99613099964837 + a * (12.153259533621753 + a * (4.254842010910525 + a * 1.0731098035209208)))).toFloat
    }

  /** Returns arccosine in turns. This implementation does not return NaN if given an out-of-range input (Math.acos does return NaN), unless the input is NaN.
    *
    * @param a
    *   acos is defined only when a is between -1f and 1f, inclusive
    * @return
    *   between `0` and `0.5` when a is in the defined range
    */
  def acosTurns(a: Float): Float =
    if (a >= 0f) {
      (Math.sqrt(1.0 - a) * (0.24998925277680104 + a * (-0.033759055260971525 + a * (0.011819005228947238 + a * -0.0029808606756510357)))).toFloat
    } else {
      (0.5 - Math.sqrt(1.0 + a) * (0.24998925277680104 + a * (0.033759055260971525 + a * (0.011819005228947238 + a * 0.0029808606756510357)))).toFloat
    }
}
