/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 2055
 * Covenant-baseline-methods: C,Ce,ColorTools,Cs,H,He,Hs,L,L0,Le,Ls,S,S0,U,Ue,Us,V,Ve,Vs,W,X,Y,Z,a,abgr,ae,al,alpha,alphaInt,as,b,bb,bits,blot,blue,blueInt,cH,cL,cbrtPositive,ch,channelH,channelL,channelS,chroma,chromaLimit,clamp,contrast,contrastBits,cos,d,darken,decoded,differentiateLightness,dullen,e,eAlpha,eH,eL,eS,editHSLuv,end,enrich,epsilon,fade,floatGetHSL,forwardGamma,forwardLight,fromColor,fromRGBA,fromRGBA8888,g,gg,green,greenInt,h,he,hs,hsluv,hsluvByHCL,hsluvHue,hsluvLightness,hsluvSaturation,hue,i,inGamut,intersectLength,inverseLightness,ix,kappa,l,le,lerpFloatColors,lerpFloatColorsBlended,lessenChange,li,lighten,lightness,limit,limitToGamut,ls,m,main,maximizeSaturation,min,mix,offsetLightness,op,opacity,other,p,r,randomColor,randomEdit,red,redInt,refU,refV,result,reverseGamma,reverseLight,rotateH,rr,s,sL,sS,saturation,sd,se,shape,sin,ss,sub1,sub2,sz,t,toColor,toEditedFloat,toHsluvColor,toRGBA,toRGBA8888,turning,unevenMix,x,x0,y,z
 * Covenant-source-reference: com/github/tommyettinger/colorful/hsluv/ColorTools.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful
package hsluv

import scala.util.boundary
import scala.util.boundary.break

import sge.colorful.{ FloatColors, TrigTools }
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the HSLuv color space. See [[https://www.hsluv.org/ HSLuv's website]] for more info.
  *
  * The HSLuv color space has 3 channels, Hue, Saturation, and Lightness. Lightness should be much more even when hue and saturation change, when compared with "vanilla" HSL or HSV. This also has an
  * alpha channel in each color, which acts like it does in every other color space package here (multiplicative alpha, higher is more opaque).
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components, here, hue, saturation, lightness, and alpha (or opacity). As long as you use a batch with `Shaders#fragmentShaderHsluv`
    * as its shader, colors passed with `Batch#setPackedColor(float)` will be interpreted as HSLuv. H should be between 0 and 1, inclusive, with 0.0f or so meaning reddish, 0.3f or so meaning
    * greenish, and 0.7 or so meaning bluish. S should be between 0 and 1, inclusive, with 0 meaning grayscale and 1 meaning fully bold/bright. L should be between 0 and 1, inclusive, with 0 used for
    * very dark colors (almost only black), and 1 used for very light colors (almost only white). Alpha is the multiplicative opacity of the color, and acts like RGBA's alpha.
    *
    * @param h
    *   0f to 1f, hue component, with 0.0f meaning red, 0.3 meaning green, and 0.7 meaning blue
    * @param s
    *   0f to 1f, saturation component, with 0.0f meaning grayscale and 1.0f meaning fully bold/bright
    * @param l
    *   0f to 1f, lightness component, with 0.5f meaning "no change" and 1f brightening
    * @param alpha
    *   0f to 1f, 0f makes the color transparent and 1f makes it opaque
    * @return
    *   a float encoding a color with the given properties
    */
  def hsluv(h: Float, s: Float, l: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255.999f).toInt << 24 & 0xfe000000) | ((l * 255.999f).toInt << 16 & 0xff0000)
        | ((s * 255.999f).toInt << 8 & 0xff00) | ((h * 255.999f).toInt & 0xff)
    )

  /** Gets a packed float representation of a color given as 4 float components, H, S, L, and alpha, with the S, L, and alpha components clamped to the 0f to 1f range before being entered into the
    * packed float color, while H is wrapped into the same range. This is only different from [[hsluv]] in that it clamps or wraps each component.
    *
    * @see
    *   [[hsluv]] This uses the same definitions for H, S, L, and alpha as hsluv().
    * @param h
    *   0f to 1f, hue component, with 0.0f meaning red, 0.3 meaning green, and 0.7 meaning blue
    * @param s
    *   0f to 1f, saturation component, with 0.0f meaning grayscale and 1.0f meaning fully bold/bright
    * @param l
    *   0f to 1f, lightness component, with 0.5f meaning "no change" and 1f brightening
    * @param alpha
    *   0f to 1f, 0f makes the color transparent and 1f makes it opaque
    * @return
    *   a float encoding a color with the given properties
    */
  def clamp(h: Float, s: Float, l: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      (Math.min(Math.max((alpha * 127.999f).toInt, 0), 127) << 25)
        | (Math.min(Math.max((l * 255.999f).toInt, 0), 255) << 16)
        | (Math.min(Math.max((s * 255.999f).toInt, 0), 255) << 8)
        | ((h - MathUtils.floor(h)) * 256f).toInt
    )

  /** An approximation of the cube-root function for float inputs and outputs. This can be about twice as fast as `Math.cbrt(double)`. This version does not tolerate negative inputs, because in the
    * narrow use case it has in this class, it never is given negative inputs.
    *
    * Has very low relative error (less than 1E-9) when inputs are uniformly distributed between 0 and 512, and absolute mean error of less than 1E-6 in the same scenario. Uses a bit-twiddling method
    * similar to one presented in Hacker's Delight and also used in early 3D graphics (see https://en.wikipedia.org/wiki/Fast_inverse_square_root for more, but this code approximates cbrt(x) and not
    * 1/sqrt(x)). This specific code was originally by Marc B. Reynolds, posted in his "Stand-alone-junk" repo:
    * https://github.com/Marc-B-Reynolds/Stand-alone-junk/blob/master/src/Posts/ballcube.c#L182-L197 .
    *
    * This is used when converting from RGB to HSLuv, as an intermediate step.
    * @param x
    *   any non-negative finite float to find the cube root of
    * @return
    *   the cube root of x, approximated
    */
  private def cbrtPositive(x: Float): Float = {
    var ix = java.lang.Float.floatToRawIntBits(x)
    val x0 = x
    ix = (ix >>> 2) + (ix >>> 4)
    ix += (ix >>> 4)
    ix += (ix >>> 8) + 0x2a5137a0
    var result = java.lang.Float.intBitsToFloat(ix)
    result = 0.33333334f * (2f * result + x0 / (result * result))
    result = 0.33333334f * (1.9999999f * result + x0 / (result * result))
    result
  }

  /** Used when given non-linear sRGB inputs to make them linear, using an exact gamma of 2.4 and accounting for the darkest colors with a different formula. This is rather close to squaring
    * `component`.
    * @param component
    *   any non-linear channel of a color, to be made linear
    * @return
    *   a linear version of component
    */
  private def forwardGamma(component: Float): Float =
    if (component < 0.04045f) component * (1f / 12.92f)
    else Math.pow((component + 0.055f) * (1f / 1.055f), 2.4f).toFloat

  /** Used to return from a linear, gamma-corrected input to an sRGB, non-linear output, using an exact gamma of 2.4 and accounting for the darkest colors with a different formula. This is mostly
    * similar to the square root, but is more precise for very dark colors.
    * @param component
    *   a linear channel of a color, to be made non-linear
    * @return
    *   a non-linear version of component
    */
  private def reverseGamma(component: Float): Float =
    if (component < 0.0031308f) component * 12.92f
    else Math.pow(component, 1f / 2.4f).toFloat * 1.055f - 0.055f

  /** Changes the curve of a requested L value so that it matches the internally-used curve. This takes a curve with a dark area similar to sRGB (a fairly small one), and makes it significantly
    * larger. This is typically used on "to HSLuv" conversions. This is much less potent of a change than the method used by Oklab.
    * @param L
    *   lightness, from 0 to 1 inclusive
    * @return
    *   an adjusted L value that can be used internally
    */
  def forwardLight(L: Float): Float = {
    val shape   = 0.8528f
    val turning = 0.1f
    val d       = turning - L
    if (d < 0) ((1f - turning) * (L - 1f)) / (1f - (L + shape * d)) + 1f
    else (turning * L) / (1e-20f + (L + shape * d))
  }

  /** Changes the curve of the internally-used lightness when it is output to another format. This makes the dark area area smaller, matching (kind-of) the curve that the standard sRGB lightness uses.
    * This is typically used on "from HSLuv" conversions. This is much less potent of a change than the method used by Oklab.
    * @param L
    *   lightness, from 0 to 1 inclusive
    * @return
    *   an adjusted L value that can be fed into a conversion to RGBA or something similar
    */
  def reverseLight(L: Float): Float = {
    val shape   = 1.1726f
    val turning = 0.1f
    val d       = turning - L
    if (d < 0) ((1f - turning) * (L - 1f)) / (1f - (L + shape * d)) + 1f
    else (turning * L) / (1e-20f + (L + shape * d))
  }

  // HSLuv-specific constants
  private val m: Array[Array[Float]] = Array(
    Array(+3.2404542f, -1.5371385f, -0.4985314f),
    Array(-0.9692660f, +1.8760108f, +0.0415560f),
    Array(+0.0556434f, -0.2040259f, +1.0572252f)
  )
  private val refU:    Float = 0.19783000664283f
  private val refV:    Float = 0.46831999493879f
  private val kappa:   Float = 9.032962962f
  private val epsilon: Float = 0.0088564516f

  private def intersectLength(sin: Float, cos: Float, line1: Float, line2: Float): Float =
    line2 / (sin - line1 * cos)

  /** Converts a packed float color in the format produced by [[hsluv]] to an RGBA8888 int. This format of int can be used with Pixmap and in some other places in libGDX.
    * @param packed
    *   a packed float color, as produced by [[hsluv]]
    * @return
    *   an RGBA8888 int color
    */
  def toRGBA8888(packed: Float): Int = boundary {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      break((decoded & 0xfe000000) >>> 24 | decoded >>> 31)
    } else if (L > 0.9999f) {
      break(0xffffff00 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31)
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    val r = (reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z) * 255.999f).toInt
    val g = (reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z) * 255.999f).toInt
    val b = (reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z) * 255.999f).toInt
    r << 24 | g << 16 | b << 8 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Converts a packed float color in the format produced by [[hsluv]] to a packed float in ABGR8888 format. This format of float can be used with the standard SpriteBatch and in some other places in
    * libGDX.
    * @param packed
    *   a packed float color, as produced by [[hsluv]]
    * @return
    *   a packed float color as ABGR8888
    */
  def toRGBA(packed: Float): Float = boundary {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      break(java.lang.Float.intBitsToFloat(decoded & 0xfe000000))
    } else if (L > 0.9999f) {
      break(java.lang.Float.intBitsToFloat(0xffffff | (decoded & 0xfe000000)))
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    val r = (reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f)) * 255.999f).toInt
    val g = (reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f)) * 255.999f).toInt
    val b = (reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f)) * 255.999f).toInt
    java.lang.Float.intBitsToFloat(r | g << 8 | b << 16 | (decoded & 0xfe000000))
  }

  /** Writes a HSLuv-format packed float color (the format produced by [[hsluv]]) into an RGBA8888 Color as used by libGDX (called `editing`).
    * @param editing
    *   a libGDX color that will be filled in-place with an RGBA conversion of `packed`
    * @param packed
    *   a packed float color, as produced by [[hsluv]]
    * @return
    *   an RGBA8888 Color
    */
  def toColor(editing: Color, packed: Float): Color = boundary {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      editing.r = 0f
      editing.g = 0f
      editing.b = 0f
      editing.a = (decoded >>> 25) / 127f
      break(editing)
    } else if (L > 0.9999f) {
      editing.r = 1f
      editing.g = 1f
      editing.b = 1f
      editing.a = (decoded >>> 25) / 127f
      break(editing)
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 2.25f * varU * y / varV
      z = (3f / varV - 5f) * y - x / 3f
    }
    editing.r = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    editing.g = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    editing.b = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    editing.a = (decoded >>> 25) / 127f
    editing
  }

  /** Writes a HSLuv-format packed float color (the format produced by [[hsluv]]) into a HSLuv-format Color called `editing`. This is mostly useful if the rest of your application expects colors in
    * HSLuv format, such as because you use `Shaders#fragmentShaderHsluv` or `ColorfulBatch`.
    *
    * Internally, this simply calls `Color.abgr8888ToColor(Color, float)` and returns the edited Color.
    * @param editing
    *   a libGDX Color that will be filled in-place with the color `hsluv`, unchanged from its color space
    * @param hsluvColor
    *   a packed float color, as produced by [[hsluv]]
    * @return
    *   the edited Color
    */
  def toHsluvColor(editing: Color, hsluvColor: Float): Color = {
    Color.abgr8888ToColor(editing, hsluvColor)
    editing
  }

  /** Takes a color encoded as an RGBA8888 int and converts to a packed float in the HSLuv format this uses.
    * @param rgba
    *   an int with the channels (in order) red, green, blue, alpha; should have 8 bits per channel
    * @return
    *   a packed float as HSLuv, which this class can use
    */
  def fromRGBA8888(rgba: Int): Float = {
    // 0x1.010101010101p-8 = 0.003921568627450980...  (1/255)
    val r = forwardGamma((rgba >>> 24) * 0.003921569f)
    val g = forwardGamma((rgba >>> 16 & 0xff) * 0.003921569f)
    val b = forwardGamma((rgba >>> 8 & 0xff) * 0.003921569f)

    val x = /* forwardXYZ */ 0.4124564f * r + 0.3575761f * g + 0.1804375f * b
    val y = /* forwardXYZ */ 0.2126729f * r + 0.7151522f * g + 0.0721750f * b
    val z = /* forwardXYZ */ 0.0193339f * r + 0.1191920f * g + 0.9503041f * b

    // XYZ to Luv
    var L = 1.16f * cbrtPositive(y) - 0.16f
    var U = 0f
    var V = 0f
    var h = 0f
    var s = 0f
    var l = 0f
    if (L < 0.00001f) {
      L = 0
      U = 0
      V = 0
    } else {
      U = 13 * L * (4 * x / (x + 15 * y + 3 * z) - refU)
      V = 13 * L * (9 * y / (x + 15 * y + 3 * z) - refV)
    }

    // Luv to Lch
    val C = Math.sqrt(U * U + V * V).toFloat
    h = TrigTools.atan2Turns(V, U)

    // Lch to HSLuv
    if (L > 0.99999f) {
      s = 0
      l = 1
    } else if (L < 0.00001f) {
      s = 0
      l = 0
    } else {
      l = forwardLight(L)
      s = Math.min(C / chromaLimit(h, l), 1)
    }
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((h * 255.999f).toInt, 0), 255)
        | Math.min(Math.max((s * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max((l * 255.999f).toInt, 0), 255) << 16
        | (rgba & 0xfe) << 24
    )
  }

  /** Takes a color encoded as an ABGR packed float and converts to a packed float in the HSLuv format this uses.
    * @param packed
    *   a packed float in ABGR8888 format, with A in the MSB and R in the LSB
    * @return
    *   a packed float as HSLuv, which this class can use
    */
  def fromRGBA(packed: Float): Float = {
    val abgr = java.lang.Float.floatToRawIntBits(packed)
    // 0x1.010101010101p-8 = 0.003921568627450980...  (1/255)
    val r = forwardGamma((abgr & 0xff) * 0.003921569f)
    val g = forwardGamma((abgr >>> 8 & 0xff) * 0.003921569f)
    val b = forwardGamma((abgr >>> 16 & 0xff) * 0.003921569f)

    val x = /* forwardXYZ */ 0.4124564f * r + 0.3575761f * g + 0.1804375f * b
    val y = /* forwardXYZ */ 0.2126729f * r + 0.7151522f * g + 0.0721750f * b
    val z = /* forwardXYZ */ 0.0193339f * r + 0.1191920f * g + 0.9503041f * b

    // XYZ to Luv
    var L = 1.16f * cbrtPositive(y) - 0.16f
    var U = 0f
    var V = 0f
    var h = 0f
    var s = 0f
    var l = 0f
//        var L = 1.16f * y - 0.16f
    if (L < 0.00001f) {
      L = 0
      U = 0
      V = 0
    } else {
      U = 13 * L * (4 * x / (x + 15 * y + 3 * z) - refU)
      V = 13 * L * (9 * y / (x + 15 * y + 3 * z) - refV)
    }

    // Luv to Lch
    val C = Math.sqrt(U * U + V * V).toFloat
    h = TrigTools.atan2Turns(V, U)

    // Lch to HSLuv
    if (L > 0.99999f) {
      s = 0
      l = 1
    } else if (L < 0.00001f) {
      s = 0
      l = 0
    } else {
      l = forwardLight(L)
      s = Math.min(C / chromaLimit(h, l), 1)
    }
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((h * 255.999f).toInt, 0), 255)
        | Math.min(Math.max((s * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max((l * 255.999f).toInt, 0), 255) << 16
        | (abgr & 0xfe000000)
    )
  }

  /** Takes a libGDX Color that uses RGBA8888 channels and converts to a packed float in the HSLuv format this uses.
    * @param color
    *   a libGDX RGBA8888 Color
    * @return
    *   a packed float as HSLuv, which this class can use
    */
  def fromColor(color: Color): Float = {
    val r = forwardGamma(color.r)
    val g = forwardGamma(color.g)
    val b = forwardGamma(color.b)

    val x = /* forwardXYZ */ 0.4124564f * r + 0.3575761f * g + 0.1804375f * b
    val y = /* forwardXYZ */ 0.2126729f * r + 0.7151522f * g + 0.0721750f * b
    val z = /* forwardXYZ */ 0.0193339f * r + 0.1191920f * g + 0.9503041f * b

    // XYZ to Luv
    var L = 1.16f * cbrtPositive(y) - 0.16f
    var U = 0f
    var V = 0f
    var h = 0f
    var s = 0f
    var l = 0f
    if (L < 0.00001f) {
      L = 0
      U = 0
      V = 0
    } else {
      U = 13 * L * (4 * x / (x + 15 * y + 3 * z) - refU)
      V = 13 * L * (9 * y / (x + 15 * y + 3 * z) - refV)
    }

    // Luv to Lch
    val C = Math.sqrt(U * U + V * V).toFloat

    h = TrigTools.atan2Turns(V, U)

    // Lch to HSLuv
    if (L > 0.99999f) {
      s = 0
      l = 1
    } else if (L < 0.00001f) {
      s = 0
      l = 0
    } else {
      l = forwardLight(L)
      s = Math.min(C / chromaLimit(h, l), 1)
    }
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((h * 255.999f).toInt, 0), 255)
        | Math.min(Math.max((s * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max((l * 255.999f).toInt, 0), 255) << 16
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )
  }

  /** Takes RGBA components from 0.0 to 1.0 each and converts to a packed float in the HSLuv format this uses.
    * @param r
    *   red, from 0.0 to 1.0 (both inclusive)
    * @param g
    *   green, from 0.0 to 1.0 (both inclusive)
    * @param b
    *   blue, from 0.0 to 1.0 (both inclusive)
    * @param a
    *   alpha, from 0.0 to 1.0 (both inclusive)
    * @return
    *   a packed float as HSLuv, which this class can use
    */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float = {
    val rr = forwardGamma(r)
    val gg = forwardGamma(g)
    val bb = forwardGamma(b)

    val x = /* forwardXYZ */ 0.4124564f * rr + 0.3575761f * gg + 0.1804375f * bb
    val y = /* forwardXYZ */ 0.2126729f * rr + 0.7151522f * gg + 0.0721750f * bb
    val z = /* forwardXYZ */ 0.0193339f * rr + 0.1191920f * gg + 0.9503041f * bb

    // XYZ to Luv
    var L = 1.16f * cbrtPositive(y) - 0.16f
    var U = 0f
    var V = 0f
    var h = 0f
    var s = 0f
    var l = 0f
    if (L < 0.00001f) {
      L = 0
      U = 0
      V = 0
    } else {
      U = 13 * L * (4 * x / (x + 15 * y + 3 * z) - refU)
      V = 13 * L * (9 * y / (x + 15 * y + 3 * z) - refV)
    }

    // Luv to Lch
    val C = Math.sqrt(U * U + V * V).toFloat
    h = TrigTools.atan2Turns(V, U)

    // Lch to HSLuv
    if (L > 0.99999f) {
      s = 0
      l = 1
    } else if (L < 0.00001f) {
      s = 0
      l = 0
    } else {
      l = forwardLight(L)
      s = Math.min(C / chromaLimit(h, l), 1)
    }
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((h * 255.999f).toInt, 0), 255)
        | Math.min(Math.max((s * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max((l * 255.999f).toInt, 0), 255) << 16
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )
  }

  /** Gets the red channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   an int from 0 to 255, inclusive, representing the red channel value of the given encoded color
    */
  def redInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    (reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the green channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   an int from 0 to 255, inclusive, representing the green channel value of the given encoded color
    */
  def greenInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    (reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the blue channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   an int from 0 to 255, inclusive, representing the blue channel value of the given encoded color
    */
  def blueInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    (reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the alpha channel value of the given encoded color, as an even int ranging from 0 to 254, inclusive. Because of how alpha is stored in libGDX, no odd-number values are possible for alpha.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   an even int from 0 to 254, inclusive, representing the alpha channel value of the given encoded color
    */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the red channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the red channel value of the given encoded color
    */
  def red(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
  }

  /** Gets the green channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the green channel value of the given encoded color
    */
  def green(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
  }

  /** Gets the blue channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the blue channel value of the given encoded color
    */
  def blue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
  }

  /** Gets the alpha channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the alpha channel value of the given encoded color
    */
  def alpha(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 25) / 127f

  /** Gets the "chroma" or "colorfulness" of a given HSLuv color. Chroma is similar to saturation in that grayscale values have 0 saturation and 0 chroma, while brighter colors have high saturation
    * and chroma. The difference is that colors that are perceptually more-colorful have higher chroma than colors that are perceptually less-colorful, regardless of hue, whereas saturation changes
    * its meaning depending on the hue and lightness. That is, the most saturated color for a given hue and lightness always has a saturation of 1, but if that color isn't perceptually very colorful
    * (as is the case for very dark and very light colors), it will have a chroma that is much lower than the maximum. The result of this method can't be negative, grayscale values have very close to
    * 0 chroma, and the most colorful value (a shade of purple) should have the highest chroma.
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   a non-negative float that represents how colorful the given value is
    */
  def chroma(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    val L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    if (L > 0.99999f) {
      0f
    } else if (L < 0.00001f) {
      0f
    } else {
      chromaLimit(H, L) * S
    }
  }

  /** Given a hue and lightness, this gets the (exact) maximum chroma possible for that hue-lightness combination, using HSLuv's versions of lightness and hue (not raw HSL). This is useful to know the
    * bounds of [[chroma]]. This should be no greater than 1.0f . Note that this version of chromaLimit() is a little slower than Oklab's version, because this has to go to greater lengths to become
    * accurate.
    * @param hue
    *   the hue, typically between 0.0f and 1.0f, to look up
    * @param lightness
    *   the lightness, clamped between 0.0f and 1.0f, to look up
    * @return
    *   the maximum possible chroma for the given hue and lightness, between 0.0f and 1.0f
    */
  def chromaLimit(hue: Float, lightness: Float): Float = {
    val h    = hue - MathUtils.floor(hue)
    val sin  = TrigTools.sinTurns(h)
    val cos  = TrigTools.cosTurns(h)
    var sub1 = (lightness + 0.16f) / 1.16f
    sub1 *= sub1 * sub1
    val sub2 = if (sub1 > epsilon) sub1 else lightness / kappa
    var min  = Float.MaxValue
    var i    = 0
    while (i < 3) {
      val m1 = m(i)(0) * sub2
      var m2 = m(i)(1) * sub2
      val m3 = m(i)(2) * sub2
      var t  = 0
      while (t < 2) {
        m2 -= t
        val top1   = 2845.17f * m1 - 948.39f * m3
        val top2   = (8384.22f * m3 + 7698.60f * m2 + 7317.18f * m1) * lightness
        val bottom = 6322.60f * m3 - 1264.52f * m2
        val length = intersectLength(sin, cos, top1 / bottom, top2 / bottom)
        if (length >= 0) min = Math.min(min, length)
        t += 1
      }
      i += 1
    }
    min
  }

  /** Gets the color with the same L as the HSLuv color stored in the given packed float, but the furthest S from gray possible for that lightness while keeping the same hue as the given color. This
    * is very similar to calling [[enrich]] with a very large `change` value.
    * @param packed
    *   a packed float color in HSLuv format; does not need to be in-gamut
    * @return
    *   the color that is as far from grayscale as this can get while keeping the L and hue of packed
    * @see
    *   [[limitToGamut(packed:Float)*]] You can use limitToGamut() if you only want max saturation for out-of-gamut colors.
    */
  def maximizeSaturation(packed: Float): Float =
    java.lang.Float.intBitsToFloat(java.lang.Float.floatToRawIntBits(packed) | 0x0000ff00)

  /** Gets the color with the given H, L, and alpha, but ignores the given S and instead uses the furthest S from gray possible for that lightness while keeping the same hue as the given color. This
    * is very similar to calling [[enrich]] with a very large `change` value.
    * @param H
    *   hue component; will be clamped between 0 and 1 if it isn't already
    * @param S
    *   saturation; ignored, and will always be maximized
    * @param L
    *   lightness component; will be clamped between 0 and 1 if it isn't already
    * @param alpha
    *   alpha component; will be clamped between 0 and 1 if it isn't already
    * @return
    *   the color that is as far from grayscale as this can get while keeping the H and L of packed
    */
  def maximizeSaturation(H: Float, @scala.annotation.unused S: Float, L: Float, alpha: Float): Float =
    clamp(H, 1f, L, alpha)

  /** Gets the hue of the given HSLuv float color, but as HSLuv understands hue rather than how HSL does. This is different from [[hue]], which uses HSL. This gives a float between 0 (inclusive) and 1
    * (exclusive).
    *
    * This is the same as [[channelH]].
    *
    * @param packed
    *   a packed HSLuv float color
    * @return
    *   a float between 0 (inclusive) and 1 (exclusive) that represents hue in the HSLuv color space
    */
  def hsluvHue(packed: Float): Float =
    (java.lang.Float.floatToRawIntBits(packed) & 0xff) / 255f

  /** Gets the saturation of the given HSLuv float color, but as HSLuv understands saturation rather than how HSL does. Saturation here is a fraction of the chroma limit (see [[chromaLimit]]) for a
    * given hue and lightness, and is between 0 and 1. This gives a float between 0 (inclusive) and 1 (inclusive).
    *
    * This is the same as [[channelS]].
    *
    * @param packed
    *   a packed HSLuv float color
    * @return
    *   a float between 0 (inclusive) and 1 (inclusive) that represents saturation in the HSLuv color space
    */
  def hsluvSaturation(packed: Float): Float =
    (java.lang.Float.floatToRawIntBits(packed) >>> 8 & 0xff) / 255f

  /** Gets the lightness of the given HSLuv float color, but as HSLuv understands lightness rather than how HSL does. This is different from [[lightness]], which uses HSL. This gives a float between 0
    * (inclusive) and 1 (inclusive).
    *
    * This is the same as [[channelL]].
    *
    * @param packed
    *   a packed HSLuv float color
    * @return
    *   a float between 0 (inclusive) and 1 (inclusive) that represents lightness in the HSLuv color space
    */
  def hsluvLightness(packed: Float): Float =
    (java.lang.Float.floatToRawIntBits(packed) >>> 16 & 0xff) / 255f

  /** A different way to specify a HSLuv color, using hue, chroma, lightness, and alpha something like a normal HSL(A) color but calculating them directly in the HSLuv color space. This has you
    * specify the desired chroma directly, as obtainable with [[chroma]], rather than the saturation, which is a fraction of the maximum chroma. Note that this takes a different value for its `hue`
    * that the method [[hue]] produces, just like `lightness` and the method [[lightness]]. The hue is just distributed differently, and the lightness should be equivalent to [[channelL]]. If you use
    * this to get two colors with the same chroma and lightness, but different hue, then the resulting colors should have similar colorfulness unless one or both chroma values exceeded the gamut limit
    * (you can get this limit with [[chromaLimit]]). If a chroma value given is greater than the chroma limit, this clamps chroma to that limit. You can use [[hsluvHue]], [[chroma]], and
    * [[hsluvLightness]] to get the hue, chroma, and lightness values from an existing color that this will understand ([[alpha]] too).
    * @param hue
    *   between 0 and 1, usually, but this will automatically wrap if too high or too low
    * @param chroma
    *   will be clamped between 0 and the maximum chroma possible for the given hue and lightness
    * @param lightness
    *   will be clamped between 0 and 1
    * @param alpha
    *   will be clamped between 0 and 1
    * @return
    *   a packed HSLuv float color that tries to match the requested hue, chroma, and lightness
    */
  def hsluvByHCL(hue: Float, chroma: Float, lightness: Float, alpha: Float): Float = {
    val h = hue - MathUtils.floor(hue)
    val a = Math.min(Math.max(alpha, 0f), 1f)
    if (lightness <= 0f) hsluv(h, 0f, 0f, a)
    else if (lightness >= 1f) hsluv(h, 0f, 1f, a)
    else hsluv(h, Math.max(chroma, 0f) / (chromaLimit(h, lightness) + 0.0001f), lightness, a)
  }

  /** Gets a color as a HSLuv packed float given floats representing HSL hue, saturation, lightness, and opacity. You should usually prefer just using [[hsluv]] to get colors with these values. All
    * parameters should normally be between 0 and 1 inclusive, though any hue is tolerated (precision loss may affect the color if the hue is too large). A hue of 0 is red, progressively higher hue
    * values go to orange, yellow, green, blue, and purple before wrapping around to red as it approaches 1. A saturation of 0 is grayscale, a saturation of 1 is brightly colored, and values close to
    * 1 will usually appear more distinct than values close to 0, especially if the hue is different. A lightness of 0.001f or less is always black (also using a shortcut if this is the case,
    * respecting opacity), while a lightness of 1f is white. Very bright colors are mostly in a band of high-saturation where lightness is 0.5f.
    *
    * @param hue
    *   0f to 1f, color wheel position
    * @param saturation
    *   0f to 1f, 0f is grayscale and 1f is brightly colored
    * @param lightness
    *   0f to 1f, 0f is black and 1f is white
    * @param opacity
    *   0f to 1f, 0f is fully transparent and 1f is opaque
    * @return
    *   a HSLuv float encoding a color with the given properties
    */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24) & 0xfe000000)
    } else if (lightness >= 0.999f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x00ff0000)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Gets the saturation of the given encoded color as HSL would calculate it, as a float ranging from 0.0f to 1.0f, inclusive. This is different from [[chroma]]; see that method's documentation for
    * details.
    *
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   the saturation of the color from 0.0 (a grayscale color; inclusive) to 1.0 (a bright color, inclusive)
    */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    val r = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    val g = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    val b = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    var X = 0f
    var Y = 0f
    var W = 0f
    if (g < b) {
      X = b
      Y = g
    } else {
      X = g
      Y = b
    }
    if (r < X) {
      W = r
    } else {
      W = X
      X = r
    }
    X - Math.min(W, Y)
  }

  /** Defined as per HSL; normally you only need [[channelL]] to get accurate lightness for HSLuv. This ranges from 0.0f (black) to 1.0f (white).
    *
    * @param encoded
    *   a packed float HSLuv color
    * @return
    *   the lightness of the given color as HSL would calculate it
    */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    val r = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    val g = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    val b = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    var X = 0f
    var Y = 0f
    var W = 0f
    if (g < b) {
      X = b
      Y = g
    } else {
      X = g
      Y = b
    }
    if (r < X) {
      W = r
    } else {
      W = X
      X = r
    }
    val d = X - Math.min(W, Y)
    X * (1f - 0.5f * d / (X + 1e-10f))
  }

  /** Gets the hue of the given encoded color, as a float from 0f (inclusive, red and approaching orange if increased) to 1f (exclusive, red and approaching purple if decreased).
    *
    * @param encoded
    *   a color as a packed float that can be obtained by [[hsluv]]
    * @return
    *   The hue of the color from 0.0 (red, inclusive) towards orange, then yellow, and eventually to purple before looping back to almost the same red (1.0, exclusive)
    */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    var L       = reverseLight((decoded >>> 16 & 0xff) / 255f)

    // HSLuv to Lch
    var C = 0f
    if (L > 0.99999f) {
      L = 1
      C = 0
    } else if (L < 0.00001f) {
      L = 0
      C = 0
    } else {
      C = chromaLimit(H, L) * S
    }

    // Lch to Luv
    val U = TrigTools.cosTurns(H) * C
    val V = TrigTools.sinTurns(H) * C

    // Luv to XYZ
    var x = 0f
    var y = 0f
    var z = 0f
    if (L < 0.00001f) {
      x = 0
      y = 0
      z = 0
    } else {
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      x = 9 * varU * y / (4 * varV)
      z = (3 * y / varV) - x / 3 - 5 * y
    }
    val r = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    val g = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    val b = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    var X = 0f
    var Y = 0f
    var Z = 0f
    var W = 0f
    if (g < b) {
      X = b
      Y = g
      Z = -1f
      W = 2f / 3f
    } else {
      X = g
      Y = b
      Z = 0f
      W = -1f / 3f
    }
    if (r < X) {
      Z = W
      W = r
    } else {
      W = X
      X = r
    }
    val d = X - Math.min(W, Y)
    Math.abs(Z + (W - Y) / (6f * d + 1e-10f))
  }

  /** Gets a variation on the packed float color basis as another packed float that has its hue, saturation, lightness, and opacity adjusted by the specified amounts. Note that this edits the color in
    * HSL space, not HSLuv! Takes floats representing the amounts of change to apply to hue, saturation, lightness, and opacity; these can be between -1f and 1f. Returns a float that can be used as a
    * packed or encoded color with methods like `Batch#setPackedColor(float)`. The float is likely to be different than the result of `basis` unless hue, saturation, lightness, and opacity are all 0.
    * This won't allocate any objects.
    *
    * The parameters this takes all specify additive changes for a color component, clamping the final values so they can't go above 1 or below 0, with an exception for hue, which can rotate around if
    * lower or higher hues would be used. As an example, if you give this 0.4f for saturation, and the current color has saturation 0.7f, then the resulting color will have 1f for saturation. If you
    * gave this -0.1f for saturation and the current color again has saturation 0.7f, then resulting color will have 0.6f for saturation.
    *
    * @param basis
    *   a packed float color that will be used as the starting point to make the next color
    * @param hue
    *   -1f to 1f, the hue change that can be applied to the new float color (not clamped, wraps)
    * @param saturation
    *   -1f to 1f, the saturation change that can be applied to the new float color
    * @param light
    *   -1f to 1f, the light/brightness change that can be applied to the new float color
    * @param opacity
    *   -1f to 1f, the opacity/alpha change that can be applied to the new float color
    * @return
    *   a float encoding a variation of basis with the given changes
    */
  def toEditedFloat(basis: Float, hue: Float, saturation: Float, light: Float, opacity: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(basis)
    val li      = Math.min(Math.max(light + reverseLight((decoded >>> 16 & 0xff) / 255f), 0f), 1f)
    val op      = Math.min(Math.max(opacity + (decoded >>> 25) / 127f, 0f), 1f)
    if (li <= 0.001f) {
      java.lang.Float.intBitsToFloat(((op * 255f).toInt << 24) & 0xfe000000)
    } else {
      val H = (decoded & 0xff) / 255f
      val S = (decoded >>> 8 & 0xff) / 255f
      var L = li
//        var L = (1f/1.16f)*(li + 0.16f)

      // HSLuv to Lch
      var C = 0f
      if (L > 0.99999f) {
        L = 1
        C = 0
      } else {
        C = chromaLimit(H, L) * S
      }

      // Lch to Luv
      val U = TrigTools.cosTurns(H) * C
      val V = TrigTools.sinTurns(H) * C

      // Luv to XYZ
      var y = 0f
      if (L <= 0.08f) {
        y = L / kappa
      } else {
        y = (L + 0.16f) / 1.16f
        y *= y * y
      }
      val iL   = 1f / (13f * L)
      val varU = U * iL + refU
      val varV = V * iL + refV
      val x    = 9 * varU * y / (4 * varV)
      val z    = (3 * y / varV) - x / 3 - 5 * y
      val r    = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
      val g    = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
      val b    = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
      var X    = 0f
      var Y    = 0f
      var Z    = 0f
      var W    = 0f
      if (g < b) {
        X = b
        Y = g
        Z = -1f
        W = 2f / 3f
      } else {
        X = g
        Y = b
        Z = 0f
        W = -1f / 3f
      }
      if (r < X) {
        Z = W
        W = r
      } else {
        W = X
        X = r
      }
      val d   = X - Math.min(W, Y)
      val lum = X * (1f - 0.5f * d / (X + 1e-10f))
      val h   = hue + Math.abs(Z + (W - Y) / (6f * d + 1e-10f)) + 1f
      val sat = saturation + (X - lum) / (Math.min(lum, 1f - lum) + 1e-10f)
      fromRGBA(FloatColors.hsl2rgb(h - h.toInt, Math.min(Math.max(sat, 0f), 1f), lum, op))
    }
  }

  /** Given a packed float HSLuv color, this edits its H, S, L, and alpha channels by adding the corresponding "add" parameter and then clamping. This returns a different float value (of course, the
    * given float can't be edited in-place). You can give a value of 0 for any "add" parameter you want to stay unchanged. H is wrapped, while S, L, and alpha are clamped.
    * @param encoded
    *   a packed float HSLuv color
    * @param addH
    *   how much to add to the H channel; typically in the -1 to 1 range
    * @param addS
    *   how much to add to the S channel; typically in the -1 to 1 range
    * @param addL
    *   how much to add to the L channel; typically in the -1 to 1 range
    * @param addAlpha
    *   how much to add to the alpha channel; typically in the -1 to 1 range
    * @return
    *   a packed float HSLuv color with the requested edits applied to `encoded`
    */
  def editHSLuv(encoded: Float, addH: Float, addS: Float, addL: Float, addAlpha: Float): Float =
    editHSLuv(encoded, addH, addS, addL, addAlpha, 1f, 1f, 1f, 1f)

  /** Given a packed float HSLuv color, this edits its H, S, L, and alpha channels by first multiplying each channel by the corresponding "mul" parameter and then adding the corresponding "add"
    * parameter, before clamping (this wraps H instead of clamping it). This means the lightness value `L` is multiplied by `mulL`, then has `addL` added, and then is clamped to the normal range for L
    * (0 to 1). This returns a different float value (of course, the given float can't be edited in-place). You can give a value of 0 for any "add" parameter you want to stay unchanged, or a value of
    * 1 for any "mul" parameter that shouldn't change. You can multiply S by 0 to make a grayscale color, or by a large value to make any non-grayscale color more vibrant. Multiplying H generally
    * isn't very useful, but adding to H can be used to do hue cycling (because H wraps).
    * @param encoded
    *   a packed float HSLuv color
    * @param addH
    *   how much to add to the H channel; typically in the -1 to 1 range
    * @param addS
    *   how much to add to the S channel; typically in the -1 to 1 range
    * @param addL
    *   how much to add to the L channel; typically in the -1 to 1 range
    * @param addAlpha
    *   how much to add to the alpha channel; typically in the -1 to 1 range
    * @param mulH
    *   how much to multiply the H channel by; should be non-negative (not always)
    * @param mulS
    *   how much to multiply the S channel by; usually non-negative
    * @param mulL
    *   how much to multiply the L channel by; usually non-negative (not always)
    * @param mulAlpha
    *   how much to multiply the alpha channel by; should be non-negative
    * @return
    *   a packed float HSLuv color with the requested edits applied to `encoded`
    */
  def editHSLuv(
    encoded:  Float,
    addH:     Float,
    addS:     Float,
    addL:     Float,
    addAlpha: Float,
    mulH:     Float,
    mulS:     Float,
    mulL:     Float,
    mulAlpha: Float
  ): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    var H       = (decoded & 0xff) / 255f
    val S0      = (decoded >>> 8 & 0xff) / 255f
    val L0      = reverseLight((decoded >>> 16 & 0xff) / 255f)
    var al      = (decoded >>> 25) / 127f

    H = H * mulH + addH
    H -= MathUtils.floor(H)
    val S = Math.min(Math.max(S0 * mulS + addS, 0f), 1f)
    val L = Math.min(Math.max(L0 * mulL + addL, 0f), 1f)
    al = Math.min(Math.max(al * mulAlpha + addAlpha, 0f), 1f)
    hsluv(H, S, L, al)
  }

  /** The "H" channel of the given packed float in HSLuv format, which is its hue; ranges from 0.0f to 1.0f . You can edit the H of a color with [[rotateH]].
    *
    * @param encoded
    *   a color encoded as a packed float, as by [[hsluv]]
    * @return
    *   the H value as a float from 0.0f to 1.0f
    */
  def channelH(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "S" channel of the given packed float in HSLuv format, which is its saturation; ranges from 0.0f to 1.0f . You can edit the S of a color with [[enrich]] and [[dullen]].
    * @param encoded
    *   a color encoded as a packed float, as by [[hsluv]]
    * @return
    *   the S value as a float from 0.0f to 1.0f
    */
  def channelS(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "L" channel of the given packed float in HSLuv format, which is its lightness; ranges from 0.0f to 1.0f . You can edit the L of a color with [[lighten]] and [[darken]].
    *
    * @param encoded
    *   a color encoded as a packed float, as by [[hsluv]]
    * @return
    *   the L value as a float from 0.0f to 1.0f
    */
  def channelL(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

  /** Interpolates from the packed float color start towards white by change. While change should be between 0f (return start as-is) and 1f (return white), start should be a packed color, as from
    * [[hsluv]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using `FloatColors.lerpFloatColors` to lerp towards white. Unlike
    * `FloatColors.lerpFloatColors`, this keeps the alpha, hue, and saturation of start as-is.
    * @see
    *   [[darken]] the counterpart method that darkens a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward white, as a float between 0 and 1; higher means closer to white
    * @return
    *   a packed float that represents a color between start and white
    */
  def lighten(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val t     = s >>> 16 & 0xff
    val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 16 & 0xff0000) | other)
  }

  /** Interpolates from the packed float color start towards black by change. While change should be between 0f (return start as-is) and 1f (return black), start should be a packed color, as from
    * [[hsluv]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using `FloatColors.lerpFloatColors` to lerp towards black. Unlike
    * `FloatColors.lerpFloatColors`, this keeps the alpha, hue, and saturation of start as-is.
    * @see
    *   [[lighten]] the counterpart method that lightens a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward black, as a float between 0 and 1; higher means closer to black
    * @return
    *   a packed float that represents a color between start and black
    */
  def darken(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val t     = s >>> 16 & 0xff
    val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) << 16 | other)
  }

  /** Moves the color of `start` away from grayscale by change (saturating the color). While change should be between 0f (return start as-is) and 1f (return maximally saturated), start should be a
    * packed color, as from [[hsluv]]. This changes only S, and won't change hue, lightness, or alpha.
    * @see
    *   [[dullen]] the counterpart method that makes a float color less saturated
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a saturated color, as a float between 0 and 1; higher means a more saturated result
    * @return
    *   a packed float that represents a color between start and a saturated color
    */
  def enrich(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val p     = s >>> 8 & 0xff
    val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p + (0xff - p) * change).toInt << 8 & 0xff00) | other)
  }

  /** Brings the color of `start` closer to grayscale by `change` (desaturating the color). While change should be between 0f (return start as-is) and 1f (return fully gray), start should be a packed
    * color, as from [[hsluv]]. This changes only S, and won't change hue, lightness, or alpha.
    * @see
    *   [[enrich]] the counterpart method that makes a float color more saturated
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a desaturated color, as a float between 0 and 1; higher means a less saturated result
    * @return
    *   a packed float that represents a color between start and a desaturated color
    */
  def dullen(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val p     = s >>> 8 & 0xff
    val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Cycles the hue of the packed float color by change. If change is 0f, this returns start as-is. If change is positive, this rotates from red to orange to yellow to green, and so on. If change is
    * negative, this instead rotates from green to yellow to orange to red. A change value is typically between -1f and 1f; if change is exactly an integer, then this will wrap around in a perfect
    * circle and produce no change. The start value should be a packed color, as from [[hsluv]]. This is a good way to reduce allocations of temporary Colors. This only changes H, and won't change
    * saturation, lightness, or alpha.
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to rotate hue by, as a float typically between -1 and 1; further from 0 rotates more
    * @return
    *   a packed float that represents a color like start but with a rotated hue
    */
  def rotateH(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val i     = s & 0xff
    val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i + (256f * change)).toInt & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards that color made opaque by change. While change should be between 0f (return start as-is) and 1f (return start with full alpha), start
    * should be a packed color, as from [[hsluv]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using `FloatColors.lerpFloatColors` to
    * lerp towards transparent. This won't change the H, S, or L of the color.
    * @see
    *   [[fade]] the counterpart method that makes a float color more translucent
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward opaque, as a float between 0 and 1; higher means closer to opaque
    * @return
    *   a packed float that represents a color between start and its opaque version
    */
  def blot(start: Float, change: Float): Float = {
    val s       = java.lang.Float.floatToRawIntBits(start)
    val opacity = s >>> 24 & 0xfe
    val other   = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity + (0xfe - opacity) * change).toInt & 0xfe) << 24 | other)
  }

  /** Interpolates from the packed float color start towards transparent by change. While change should be between 0 (return start as-is) and 1f (return the color with 0 alpha), start should be a
    * packed color, as from [[hsluv]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using `FloatColors.lerpFloatColors` to lerp towards
    * transparent. This won't change the H, S, or L of the color.
    * @see
    *   [[blot]] the counterpart method that makes a float color more opaque
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward transparent, as a float between 0 and 1; higher means closer to transparent
    * @return
    *   a packed float that represents a color between start and transparent
    */
  def fade(start: Float, change: Float): Float = {
    val s       = java.lang.Float.floatToRawIntBits(start)
    val opacity = s & 0xfe
    val other   = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity * (1f - change)).toInt & 0xfe) << 24 | other)
  }

  /** Given a packed float HSLuv color `mainColor` and another HSLuv color that it should be made to contrast with, gets a packed float HSLuv color with roughly inverted lightness but the same hue,
    * saturation, and alpha. This won't ever produce black or other very dark colors, and also has a gap in the range it produces for lightness values between 0.5 and 0.55. That allows most of the
    * colors this method produces to contrast well as a foreground when displayed on a background of `contrastingColor`, or vice versa. This will leave the lightness unchanged if the hues of the
    * contrastingColor and those of the mainColor are already very different. This has nothing to do with the contrast channel of the tweak in ColorfulBatch; where that part of the tweak can make
    * too-similar lightness values further apart by just a little, this makes a modification on `mainColor` to maximize its lightness difference from `contrastingColor` without losing its other
    * qualities.
    * @param mainColor
    *   a packed float color, as produced by [[hsluv]]; this is the color that will be adjusted
    * @param contrastingColor
    *   a packed float color, as produced by [[hsluv]]; the adjusted mainColor will contrast with this
    * @return
    *   a different HSLuv packed float color, based on mainColor but with potentially very different lightness
    */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float = {
    val bits         = java.lang.Float.floatToRawIntBits(mainColor)
    val contrastBits = java.lang.Float.floatToRawIntBits(contrastingColor)
    val H            = bits & 0xff
//        val S = bits >>> 8 & 0xff
    val L  = bits >>> 16 & 0xff
    val cH = contrastBits & 0xff
//        val cS = contrastBits >>> 8 & 0xff
    val cL = contrastBits >>> 16 & 0xff
    if (Math.abs(H - cH) >= 90) mainColor
    else java.lang.Float.intBitsToFloat((bits & 0xfe00ffff) | (if (cL < 128) L * 0.45f + 128 else 128 - L * 0.45f).toInt << 16)
  }

  /** Given a packed float HSLuv color `mainColor` and another HSLuv color that it should be made to contrast with, gets a packed float HSLuv color with L that should be quite different from
    * `contrastingColor`'s L, but the same hue, saturation, and opacity. This allows most of the colors this method produces to contrast well as a foreground when displayed on a background of
    * `contrastingColor`, or vice versa.
    *
    * This is similar to [[inverseLightness]], but is considerably simpler, and this method will change the lightness of mainColor when the two given colors have close lightness but distant hues.
    * Because it averages the original L of mainColor with the modified one, this tends to not produce harsh color changes.
    * @param mainColor
    *   a packed HSLuv float color; this is the color that will be adjusted
    * @param contrastingColor
    *   a packed HSLuv float color; the adjusted mainColor will contrast with the L of this
    * @return
    *   a different packed HSLuv float color, based on mainColor but typically with different lightness
    */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(mainColor)
    val contrast = java.lang.Float.floatToRawIntBits(contrastingColor)
    java.lang.Float.intBitsToFloat((main & 0xfe00ffff) | ((contrast >>> 16) + 128 & 0xff) + (main >>> 16 & 0xff) >>> 1)
  }

  /** Pretty simple; adds 0.5 to the given color's L and wraps it around if it would go above 1.0, then averages that with the original L. This means light colors become darker, and dark colors become
    * lighter, with almost all results in the middle-range of possible lightness.
    *
    * Calling `offsetLightness(mainColor)` is the same as calling `ColorTools.differentiateLightness(mainColor, mainColor)`.
    * @param mainColor
    *   a packed HSLuv float color
    * @return
    *   a different packed HSLuv float color, with its L channel changed.
    */
  def offsetLightness(mainColor: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(mainColor)
    java.lang.Float.intBitsToFloat((decoded & 0xfe00ffff) | ((decoded >>> 16) + 128 & 0xff) + (decoded >>> 16 & 0xff) >>> 1)
  }

  /** Makes the additive HSLuv color stored in `color` cause less of a change when used as a tint, as if it were mixed with neutral gray. When `fraction` is 1.0, this returns color unchanged; when
    * fraction is 0.0, it returns `Palette#GRAY`, and when it is in-between 0.0 and 1.0 it returns something between the two. This is meant for things like area of effect abilities that make smaller
    * color changes toward their periphery. This only affects the saturation and lightness of the color; its hue and alpha are unchanged.
    * @param color
    *   a color that should have its tinting effect potentially weakened
    * @param fraction
    *   how much of `color` should be kept, from 0.0 to 1.0
    * @return
    *   a HSLuv float color between gray and `color`
    */
  def lessenChange(color: Float, fraction: Float): Float = {
    val e      = java.lang.Float.floatToRawIntBits(color)
    val sS     = 0x80
    val sL     = 0x80
    val eH     = e & 0xff
    val eS     = (e >>> 8) & 0xff
    val eL     = (e >>> 16) & 0xff
    val eAlpha = e >>> 24 & 0xfe
    java.lang.Float.intBitsToFloat(
      eH
        | (((sS + fraction * (eS - sS)).toInt & 0xff) << 8)
        | (((sL + fraction * (eL - sL)).toInt & 0xff) << 16)
        | (eAlpha << 24)
    )
  }

  /** Returns true always; HSLuv colors are always in-gamut.
    * @param packed
    *   a packed float color as HSLuv
    * @return
    *   true
    */
  def inGamut(packed: Float): Boolean =
    true

  /** Returns true if S and L are each between 0 and 1; if valid, HSLuv colors are always in-gamut.
    * @param H
    *   hue, as an unbounded float
    * @param S
    *   saturation, as a float from 0 to 1
    * @param L
    *   lightness, as a float from 0 to 1
    * @return
    *   true if S and L are both between 0 and 1
    */
  def inGamut(H: Float, S: Float, L: Float): Boolean =
    S >= 0f && S <= 1.0f && L >= 0f && L <= 1.0f

  /** Returns its argument unchanged; HSLuv colors are always in-gamut.
    * @param packed
    *   a packed float color in HSLuv format
    * @return
    *   `packed`, unchanged
    */
  def limitToGamut(packed: Float): Float =
    packed

  /** Identical to calling [[clamp]] with 1f as its last parameter.
    * @param H
    *   hue; will be wrapped between 0 and 1
    * @param S
    *   saturation; will be clamped between 0 and 1 if it isn't already
    * @param L
    *   lightness; will be clamped between 0 and 1 if it isn't already
    * @return
    *   an HSLuv color with the specified channel values, wrapped or clamped as appropriate
    */
  def limitToGamut(H: Float, S: Float, L: Float): Float =
    clamp(H - MathUtils.floor(H), S, L, 1f)

  /** Identical to calling [[clamp]] with the given parameters.
    * @param H
    *   hue; will be wrapped between 0 and 1
    * @param S
    *   saturation; will be clamped between 0 and 1 if it isn't already
    * @param L
    *   lightness; will be clamped between 0 and 1 if it isn't already
    * @param alpha
    *   opacity; will be clamped between 0 and 1 if it isn't already
    * @return
    *   an HSLuv color with the specified channel values, wrapped or clamped as appropriate
    */
  def limitToGamut(H: Float, S: Float, L: Float, alpha: Float): Float =
    clamp(H, S, L, alpha)

  /** Makes a quasi-randomly-edited variant on the given `color`, allowing typically a small amount of `variance` (such as 0.05 to 0.25) between the given color and what this can return. The `seed`
    * should be different each time this is called, and can be obtained from a random number generator to make the colors more random, or can be incremented on each call. If the seed is only
    * incremented or decremented, then this shouldn't produce two similar colors in a row unless variance is very small. The variance affects the H, S, and L of the generated color, and each of those
    * channels can go up or down by the given variance as long as the total distance isn't greater than the variance.
    * @param color
    *   a packed float color, as produced by [[hsluv]]
    * @param seed
    *   a long seed that should be different on each call; should not be 0
    * @param variance
    *   max amount of difference between the given color and the generated color; always less than 1
    * @return
    *   a generated packed float color that should be at least somewhat different from `color`
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val H       = (decoded & 0xff) / 255f
    val S       = (decoded >>> 8 & 0xff) / 255f
    val L       = reverseLight((decoded >>> 16 & 0xff) / 255f)
    val limit   = variance * variance
    var sd      = seed
    // 0x7FFFFFp-1f = 4194303.5f, 0x1p-22f = 2.3841858e-7f
    boundary[Float] {
      var j = 0
      while (j < 50) {
        val x = (((sd * 0xd1b54a32d192ed03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        val y = (((sd * 0xabc98388fb8fac03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        val z = (((sd * 0x8cb92ba72f3d8dd7L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        sd += 0x9e3779b97f4a7c15L
        val dist = x * x + y * y + z * z
        if (dist <= limit) {
          break(clamp(H + x, S + y, L + z, (decoded >>> 25) / 127f))
        }
        j += 1
      }
      color
    }
  }

  /** Produces a random packed float color that is always in-gamut and should be uniformly distributed.
    * @param random
    *   a Random object (preferably a subclass of Random)
    * @return
    *   a packed float color that is always in-gamut
    */
  def randomColor(random: java.util.Random): Float =
    hsluv(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f)

  /** Interpolates from the packed float color start towards end by change. Both start and end should be packed colors, as from [[hsluv]], and change can be between 0f (keep start) and 1f (only use
    * end). Both start and end must use HSLuv. This is a good way to reduce allocations of temporary Colors.
    * @param start
    *   the starting color as a packed float
    * @param end
    *   the target color as a packed float
    * @param change
    *   how much to go from start toward end, as a float between 0 and 1; higher means closer to end
    * @return
    *   a packed float that represents a color between start and end
    */
  def lerpFloatColors(start: Float, end: Float, change: Float): Float = {
    val s  = java.lang.Float.floatToRawIntBits(start)
    val e  = java.lang.Float.floatToRawIntBits(end)
    val hs = s & 0xff
    val ss = (s >>> 8) & 0xff
    val ls = (s >>> 16) & 0xff
    val as = s >>> 24 & 0xfe
    val he = e & 0xff
    val se = (e >>> 8) & 0xff
    val le = (e >>> 16) & 0xff
    val ae = e >>> 24 & 0xfe

    val Hs = hs / 255f
    var Cs = 0f
    var Ls = 0f
    if (ls == 255) { Ls = 1f; Cs = 0f }
    else if (ls == 0) { Ls = 0f; Cs = 0f }
    else { Ls = ls / 255f; Cs = chromaLimit(Hs, Ls) * (ss / 255f) }
    // Lch to Luv
    val Us = TrigTools.cosTurns(Hs) * Cs
    val Vs = TrigTools.sinTurns(Hs) * Cs

    val He = he / 255f
    var Ce = 0f
    var Le = 0f
    if (le == 255) { Le = 1f; Ce = 0f }
    else if (le == 0) { Le = 0f; Ce = 0f }
    else { Le = le / 255f; Ce = chromaLimit(He, Le) * (se / 255f) }
    // Lch to Luv
    val Ue = TrigTools.cosTurns(He) * Ce
    val Ve = TrigTools.sinTurns(He) * Ce

    val L = Ls + change * (Le - Ls)
    val U = Us + change * (Ue - Us)
    val V = Vs + change * (Ve - Vs)

    // Luv to Lch
    val H = TrigTools.atan2Turns(V, U)

    // Lch to HSLuv
    if (L > 0.99999f) {
      Palette.WHITE
    } else if (L < 0.00001f) {
      Palette.BLACK
    } else {
      val S = Math.min(Math.sqrt(U * U + V * V).toFloat / chromaLimit(H, L), 1)
      java.lang.Float.intBitsToFloat(
        Math.min(Math.max((H * 255.999f).toInt, 0), 255)
          | Math.min(Math.max((S * 255.999f).toInt, 0), 255) << 8
          | Math.min(Math.max((L * 255.999f).toInt, 0), 255) << 16
          | (((as + change * (ae - as)).toInt & 0xfe) << 24)
      )
    }
  }

  /** Interpolates from the packed float color start towards end by change, but keeps the alpha of start and uses the alpha of end as an extra factor that can affect how much to change. Both start and
    * end should be packed colors, as from [[hsluv]], and change can be between 0f (keep start) and 1f (only use end). Both start and end must use HSLuv. This is a good way to reduce allocations of
    * temporary Colors.
    * @param start
    *   the starting color as a packed float; alpha will be preserved
    * @param end
    *   the target color as a packed float; alpha will not be used directly, and will instead be multiplied with change
    * @param change
    *   how much to go from start toward end, as a float between 0 and 1; higher means closer to end
    * @return
    *   a packed float that represents a color between start and end
    */
  def lerpFloatColorsBlended(start: Float, end: Float, change: Float): Float = {
    val s  = java.lang.Float.floatToRawIntBits(start)
    val e  = java.lang.Float.floatToRawIntBits(end)
    val as = s & 0xfe000000
    val ch = change * ((e >>> 25) / 127f)
    lerpFloatColors(start, java.lang.Float.intBitsToFloat(as | (e & 0xffffff)), ch)
  }

  /** Returns a 1:1 mix of color0 and color1. All colors should use the same color space. This is the same as calling [[lerpFloatColors]] with a change of 0.5.
    * @param color0
    *   the first color to mix, as a packed float color
    * @param color1
    *   the second color to mix, as a packed float color
    * @return
    *   an even mix of all colors given, as a packed float color
    */
  def mix(color0: Float, color1: Float): Float =
    lerpFloatColors(color0, color1, 0.5f)

  /** Returns a 1:1:1 mix of color0, color1, and color2. All colors should use the same color space.
    * @param color0
    *   the first color to mix, as a packed float color
    * @param color1
    *   the second color to mix, as a packed float color
    * @param color2
    *   the third color to mix, as a packed float color
    * @return
    *   an even mix of all colors given, as a packed float color
    */
  def mix(color0: Float, color1: Float, color2: Float): Float =
    lerpFloatColors(lerpFloatColors(color0, color1, 0.5f), color2, 0.33333f)

  /** Returns a 1:1:1:1 mix of color0, color1, color2, and color3. All colors should use the same color space.
    * @param color0
    *   the first color to mix, as a packed float color
    * @param color1
    *   the second color to mix, as a packed float color
    * @param color2
    *   the third color to mix, as a packed float color
    * @param color3
    *   the fourth color to mix, as a packed float color
    * @return
    *   an even mix of all colors given, as a packed float color
    */
  def mix(color0: Float, color1: Float, color2: Float, color3: Float): Float =
    lerpFloatColors(lerpFloatColors(lerpFloatColors(color0, color1, 0.5f), color2, 0.33333f), color3, 0.25f)

  /** Given several colors, this gets an even mix of all colors in equal measure. If `colors` is null or has no items, this returns 0f (usually transparent in most color spaces).
    * @param colors
    *   an array or varargs of packed float colors; all should use the same color space
    * @return
    *   an even mix of all colors given, as a packed float color
    */
  def mix(colors: Float*): Float =
    if (colors == null || colors.isEmpty) 0f
    else {
      var result = colors(0)
      var i      = 1
      while (i < colors.length) {
        result = lerpFloatColors(result, colors(i), 1f / (i + 1f))
        i += 1
      }
      result
    }

  /** Given several colors, this gets an even mix of all colors in equal measure. If `colors` is null or has no items, this returns 0f (usually transparent in most color spaces). This is mostly useful
    * in conjunction with `FloatArray`, using its `items` for colors, typically 0 for offset, and its `size` for size.
    * @param colors
    *   an array of packed float colors; all should use the same color space
    * @param offset
    *   the index of the first item in `colors` to use
    * @param size
    *   how many items from `colors` to use
    * @return
    *   an even mix of all colors given, as a packed float color
    */
  def mix(colors: Array[Float], offset: Int, size: Int): Float = {
    val end = offset + size
    if (colors == null || colors.length < end || offset < 0 || size <= 0) {
      0f // transparent, usually
    } else {
      var result = colors(offset)
      var i      = offset + 1
      var denom  = 2
      while (i < end) {
        result = lerpFloatColors(result, colors(i), 1f / denom)
        i += 1
        denom += 1
      }
      result
    }
  }

  /** Mixes any number of colors with arbitrary weights per-color. Takes an array or varargs of alternating floats representing colors and weights, as with `color, weight, color, weight...`. If
    * `colors` is null or has no items, this returns 0f (usually transparent in most color spaces).
    * @param colors
    *   an array or varargs that should contain alternating `color, weight, color, weight...` floats
    * @return
    *   a mix of all colors given respecting their weights, as a packed float color
    */
  def unevenMix(colors: Float*): Float =
    if (colors == null || colors.isEmpty) 0f
    else if (colors.length <= 2) colors(0)
    else unevenMix(colors.toArray, 0, colors.length)

  /** Mixes any number of colors with arbitrary weights per-color. Takes an array of alternating floats representing colors and weights, as with `color, weight, color, weight...`, starting at `offset`
    * in the array and continuing for `size` indices in the array. The `size` should be an even number 2 or greater, otherwise it will be reduced by 1. The weights can be any non-negative finite float
    * values; this method handles normalizing them internally.
    * @param colors
    *   starting at `offset`, this should contain alternating `color, weight, color, weight...` floats
    * @param offset
    *   where to start reading from in `colors`
    * @param size
    *   how many indices to read from `colors`; must be an even number
    * @return
    *   the mixed color, as a packed float in the same color space as the given float colors
    */
  def unevenMix(colors: Array[Float], offset: Int, size: Int): Float = {
    val sz  = size & -2
    val end = offset + sz
    if (colors == null || colors.length < end || offset < 0 || sz <= 0) {
      0f // transparent, usually
    } else {
      var result  = colors(offset)
      var current = colors(offset + 1)
      var total   = current
      var i       = offset + 3
      while (i < end) {
        total += colors(i)
        i += 2
      }
      total = 1f / total
      current *= total
      i = offset + 3
      while (i < end) {
        val mixColor = colors(i - 1)
        val weight   = colors(i) * total
        current += weight
        result = lerpFloatColors(result, mixColor, weight / current)
        i += 2
      }
      result
    }
  }
}
