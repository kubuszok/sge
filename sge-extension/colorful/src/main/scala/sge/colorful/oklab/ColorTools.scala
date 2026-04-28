/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 849
 * Covenant-baseline-methods: A,AA,ADiff,B,BA,BDiff,ColorTools,L,LA,LDiff,a,a2,abgr,ac,al,alc,alpha,alphaInt,b,b2,bc,bits,blot,blue,blueInt,cL,cbrtPositive,cc,channelA,channelB,channelL,chroma,chromaLimit,contrast,contrastBits,cube,d,dA,dB,darken,decoded,differentiateLightness,dist,distance,distanceSquared,dullen,e,eL,editOklab,enrich,fade,floatGetHSL,forwardGamma,forwardLight,fromColor,fromRGBA,fromRGBA8888,g,getRawGamutValue,green,greenInt,h,hue,idx,inGamut,inverseLightness,ix,l,lc,lessenChange,lighten,lightness,limit,limitToGamut,lowerA,lowerB,m,main,maximizeSaturation,multiplyChroma,offsetLightness,oklab,oklabByHCL,oklabByHSL,oklabHue,oklabLightness,oklabSaturation,op,r,raiseA,raiseB,randomColor,randomEdit,red,redInt,result,reverseGamma,reverseLight,rl,s,sL,saturation,sc,sd,toColor,toEditedFloat,toOklabColor,toRGBA,toRGBA8888,w,x,x0,y,z
 * Covenant-source-reference: com/github/tommyettinger/colorful/oklab/ColorTools.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: e4a5fd960eef746ca5aa826063432fb79666d74f
 */
package sge
package colorful
package oklab

import scala.util.boundary
import scala.util.boundary.break

import sge.colorful.{ FloatColors, TrigTools }
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the Oklab color space. Oklab is a perceptual color space that builds on the same foundation as IPT, but is
  * better-calibrated for uniform lightness and colorfulness. The difference between two Oklab colors is approximated by the Euclidean distance between their components.
  *
  * Here, the L channel represents lightness (0 to 1), the A channel represents green-to-red chroma (0 to 1, with 0.5 being gray), and the B channel represents blue-to-yellow chroma (0 to 1, with 0.5
  * being gray). Alpha is the multiplicative opacity.
  */
object ColorTools {

  import Gamut.GAMUT_DATA

  /** Gets a packed float representation of a color given as 4 float components in Oklab.
    * @param l
    *   0f to 1f, lightness
    * @param a
    *   0f to 1f, green-to-red chromatic component (0.5 is gray)
    * @param b
    *   0f to 1f, blue-to-yellow chromatic component (0.5 is gray)
    * @param alpha
    *   0f to 1f, opacity
    * @return
    *   a packed float encoding the color
    */
  def oklab(l: Float, a: Float, b: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255).toInt << 24 & 0xfe000000) | ((b * 255).toInt << 16 & 0xff0000)
        | ((a * 255).toInt << 8 & 0xff00) | ((l * 255).toInt & 0xff)
    )

  /** An approximation of the cube-root function for non-negative float inputs. This can be about twice as fast as Math.cbrt(double).
    */
  def cbrtPositive(x: Float): Float = {
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

  private def cube(x: Float): Float = x * x * x

  /** Used when given non-linear sRGB inputs to make them linear, approximating with gamma 2.0. */
  def forwardGamma(component: Float): Float = component * component

  /** Used to return from a linear, gamma-corrected input to an sRGB, non-linear output, using gamma 2.0. */
  def reverseGamma(component: Float): Float = Math.sqrt(component).toFloat

  /** Changes the curve of a requested L value so that it matches the internally-used curve. Internally, this is similar to `Math.pow(L, 1.5f)`.
    */
  def forwardLight(L: Float): Float = Math.sqrt(L * L * L).toFloat

  /** Changes the curve of the internally-used lightness when it is output to another format. Internally, this is similar to `Math.pow(L, 2f/3f)`.
    */
  def reverseLight(L: Float): Float = {
    var ix = java.lang.Float.floatToRawIntBits(L)
    val x0 = L
    ix = (ix >>> 2) + (ix >>> 4)
    ix += (ix >>> 4)
    ix += (ix >>> 8) + 0x2a5137a0
    var result = java.lang.Float.intBitsToFloat(ix)
    result = 0.33333334f * (2f * result + x0 / (result * result))
    result = 0.33333334f * (1.9999999f * result + x0 / (result * result))
    result * result
  }

  /** Converts a packed float Oklab color to an RGBA8888 int. */
  def toRGBA8888(packed: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    val r       = (reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f)) * 255.999f).toInt
    val g       = (reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f)) * 255.999f).toInt
    val b       = (reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f)) * 255.999f).toInt
    r << 24 | g << 16 | b << 8 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Converts a packed float Oklab color to a packed float in RGBA format. */
  def toRGBA(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    val r       = (reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f)) * 255.999f).toInt
    val g       = (reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f)) * 255.999f).toInt
    val b       = (reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f)) * 255.999f).toInt
    java.lang.Float.intBitsToFloat(r | g << 8 | b << 16 | (decoded & 0xfe000000))
  }

  /** Writes an Oklab packed float color into an RGBA8888 Color. */
  def toColor(editing: Color, packed: Float): Color = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    editing.r = reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f))
    editing.g = reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f))
    editing.b = reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f))
    editing.a = (decoded >>> 25) * 0.007874016f
    editing.clamp()
  }

  /** Takes an RGBA8888 int and converts to a packed float in Oklab format. */
  def fromRGBA8888(rgba: Int): Float = {
    val r = forwardGamma((rgba >>> 24) * 0.003921569f)
    val g = forwardGamma((rgba >>> 16 & 0xff) * 0.003921569f)
    val b = forwardGamma((rgba >>> 8 & 0xff) * 0.003921569f)
    val l = cbrtPositive(0.4121656120f * r + 0.5362752080f * g + 0.0514575653f * b)
    val m = cbrtPositive(0.2118591070f * r + 0.6807189584f * g + 0.1074065790f * b)
    val s = cbrtPositive(0.0883097947f * r + 0.2818474174f * g + 0.6302613616f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((forwardLight(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | (rgba & 0xfe) << 24
    )
  }

  /** Takes a packed float RGBA color and converts to a packed float in Oklab format. */
  def fromRGBA(packed: Float): Float = {
    val abgr = java.lang.Float.floatToRawIntBits(packed)
    val r    = forwardGamma((abgr & 0xff) * 0.003921569f)
    val g    = forwardGamma((abgr >>> 8 & 0xff) * 0.003921569f)
    val b    = forwardGamma((abgr >>> 16 & 0xff) * 0.003921569f)
    val l    = cbrtPositive(0.4121656120f * r + 0.5362752080f * g + 0.0514575653f * b)
    val m    = cbrtPositive(0.2118591070f * r + 0.6807189584f * g + 0.1074065790f * b)
    val s    = cbrtPositive(0.0883097947f * r + 0.2818474174f * g + 0.6302613616f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((forwardLight(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | (abgr & 0xfe000000)
    )
  }

  /** Takes a libGDX Color (RGBA8888) and converts to a packed float in Oklab format. */
  def fromColor(color: Color): Float = {
    val r = forwardGamma(color.r)
    val g = forwardGamma(color.g)
    val b = forwardGamma(color.b)
    val l = cbrtPositive(0.4121656120f * r + 0.5362752080f * g + 0.0514575653f * b)
    val m = cbrtPositive(0.2118591070f * r + 0.6807189584f * g + 0.1074065790f * b)
    val s = cbrtPositive(0.0883097947f * r + 0.2818474174f * g + 0.6302613616f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((forwardLight(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((color.a * 255).toInt << 24 & 0xfe000000)
    )
  }

  /** Takes RGBA components from 0.0 to 1.0 and converts to a packed float in Oklab format. */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float = {
    val rl = forwardGamma(r); val gl = forwardGamma(g); val bl = forwardGamma(b)
    val l  = cbrtPositive(0.4121656120f * rl + 0.5362752080f * gl + 0.0514575653f * bl)
    val m  = cbrtPositive(0.2118591070f * rl + 0.6807189584f * gl + 0.1074065790f * bl)
    val s  = cbrtPositive(0.0883097947f * rl + 0.2818474174f * gl + 0.6302613616f * bl)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((forwardLight(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((a * 255).toInt << 24 & 0xfe000000)
    )
  }

  /** Gets the red channel value of the given Oklab encoded color, as an int from 0 to 255. */
  def redInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    (reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the green channel value of the given Oklab encoded color, as an int from 0 to 255. */
  def greenInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    (reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the blue channel value of the given Oklab encoded color, as an int from 0 to 255. */
  def blueInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    (reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the alpha channel value as an even int from 0 to 254. */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the red channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def red(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f))
  }

  /** Gets the green channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def green(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f))
  }

  /** Gets the blue channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def blue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f))
  }

  /** Gets the alpha channel as a float from 0.0f to 1.0f. */
  def alpha(encoded: Float): Float =
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** The "L" channel of the given packed float in Oklab format (lightness, 0 to 1). */
  def channelL(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "A" channel of the given packed float in Oklab format (green-to-red, 0 to 1). */
  def channelA(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "B" channel of the given packed float in Oklab format (blue-to-yellow, 0 to 1). */
  def channelB(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

  /** Gets the chroma (colorfulness) of a given Oklab float color. Result is between 0 and ~0.334. */
  def chroma(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val a       = ((decoded >>> 7 & 0x1fe) - 0xff) * 0.00390625f
    val b       = ((decoded >>> 15 & 0x1fe) - 0xff) * 0.00390625f
    Math.sqrt(a * a + b * b).toFloat
  }

  /** Gets the approximate maximum chroma for a given hue and lightness. */
  def chromaLimit(hue: Float, lightness: Float): Float = {
    val idx = (Math.min(Math.max(lightness, 0f), 1f) * 255.999f).toInt << 8 |
      (256f * (hue - MathUtils.floor(hue))).toInt
    GAMUT_DATA(idx) * 0.00390625f
  }

  /** Returns true if the given packed float Oklab color is valid to convert losslessly back to RGBA. */
  def inGamut(packed: Float): Boolean = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 255f
    val B       = ((decoded >>> 16 & 0xff) - 127f) / 255f
    val g       = GAMUT_DATA((decoded & 0xff) << 8 | (256f * TrigTools.atan2Turns(B, A)).toInt)
    g * g * 3.8146973e-6f + 6.1035156e-5f >= (A * A + B * B)
  }

  /** Returns true if the given Oklab values are valid to convert losslessly back to RGBA. */
  def inGamut(L: Float, A: Float, B: Float): Boolean = {
    val a2 = ((A * 255).toInt - 127f) / 255f
    val b2 = ((B * 255).toInt - 127f) / 255f
    val g  = GAMUT_DATA(((L * 255).toInt & 0xff) << 8 | (256f * TrigTools.atan2Turns(b2, a2)).toInt)
    L >= 0f && L <= 1f && g * g * 3.8146973e-6f + 6.1035156e-5f >= (a2 * a2 + b2 * b2)
  }

  /** Checks whether the given Oklab color is in-gamut; if not, brings it just inside the gamut. */
  def limitToGamut(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val A       = (decoded >>> 8 & 0xff) - 127f
    val B       = (decoded >>> 16 & 255) - 127f
    val hue     = TrigTools.atan2Turns(B, A)
    val idx     = (decoded & 0xff) << 8 | (256f * hue).toInt
    val dist    = GAMUT_DATA(idx) * 0.5f
    if (dist * dist >= (A * A + B * B)) {
      packed
    } else {
      java.lang.Float.intBitsToFloat(
        (decoded & 0xfe0000ff) |
          (TrigTools.sinTurns(hue) * dist + 127.5f).toInt << 16 |
          (TrigTools.cosTurns(hue) * dist + 127.5f).toInt << 8
      )
    }
  }

  /** Checks whether the given Oklab color (L, A, B) is in-gamut; if not, brings it inside. Always opaque. */
  def limitToGamut(L: Float, A: Float, B: Float): Float =
    limitToGamut(L, A, B, 1f)

  /** Checks whether the given Oklab color (L, A, B, alpha) is in-gamut; if not, brings it inside. */
  def limitToGamut(L: Float, A: Float, B: Float, alpha: Float): Float = {
    val lc   = Math.min(Math.max(L, 0f), 1f)
    val ac   = Math.min(Math.max(A, 0f), 1f)
    val bc   = Math.min(Math.max(B, 0f), 1f)
    val alc  = Math.min(Math.max(alpha, 0f), 1f)
    val a2   = ((ac * 255).toInt - 127f) / 255f
    val b2   = ((bc * 255).toInt - 127f) / 255f
    val hue  = TrigTools.atan2Turns(b2, a2)
    val idx  = (lc * 255f).toInt << 8 | (256f * hue).toInt
    val dist = GAMUT_DATA(idx) * 0.5f
    if (dist * dist * 1.5258789e-5f >= (a2 * a2 + b2 * b2)) {
      oklab(lc, ac, bc, alc)
    } else {
      java.lang.Float.intBitsToFloat(
        (alc * 127.999f).toInt << 25 |
          (TrigTools.sinTurns(hue) * dist + 127.5f).toInt << 16 |
          (TrigTools.cosTurns(hue) * dist + 127.5f).toInt << 8 |
          (lc * 255f).toInt
      )
    }
  }

  /** Interpolates from start towards white by change. Keeps alpha and chroma of start. */
  def lighten(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i + (0xff - i) * change).toInt & 0xff) | other)
  }

  /** Interpolates from start towards black by change. Keeps alpha and chroma of start. */
  def darken(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i * (1f - change)).toInt & 0xff) | other)
  }

  /** Interpolates from start towards warmer (orange to magenta) by change. */
  def raiseA(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val p = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p + (0xff - p) * change).toInt << 8 & 0xff00) | other)
  }

  /** Interpolates from start towards cooler (green to blue) by change. */
  def lowerA(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val p = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Interpolates from start towards "natural" (green to orange) by change. */
  def raiseB(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 16 & 0xff0000) | other)
  }

  /** Interpolates from start towards "artificial" (blue to purple) by change. */
  def lowerB(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) << 16 | other)
  }

  /** Interpolates from start towards opaque by change. */
  def blot(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s >>> 24 & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity + (0xfe - opacity) * change).toInt & 0xfe) << 24 | other)
  }

  /** Interpolates from start towards transparent by change. */
  def fade(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s >>> 24 & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity * (1f - change)).toInt & 0xfe) << 24 | other)
  }

  /** Brings the chromatic components of start closer to grayscale by change (desaturating). */
  def dullen(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    oklab((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Pushes the chromatic components of start away from grayscale by change (saturating). */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    limitToGamut((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Gets the Oklab hue of the given packed float color, from 0 (inclusive) to 1 (exclusive). */
  def oklabHue(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val A       = (decoded >>> 8 & 0xff) - 127f
    val B       = (decoded >>> 16 & 255) - 127f
    TrigTools.atan2Turns(B, A)
  }

  /** Gets the Oklab saturation of the given packed float color, from 0 to 1. */
  def oklabSaturation(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val A       = (decoded >>> 8 & 0xff) - 127f
    val B       = (decoded >>> 16 & 255) - 127f
    val hue     = TrigTools.atan2Turns(B, A)
    val idx     = (decoded & 0xff) << 8 | (256f * hue).toInt
    val dist    = GAMUT_DATA(idx)
    if (dist <= 2.5f) 0f else Math.sqrt(A * A + B * B).toFloat * 2f / dist
  }

  /** Gets the Oklab lightness of the given packed float color, from 0 to 1. Same as channelL(). */
  def oklabLightness(packed: Float): Float =
    (java.lang.Float.floatToRawIntBits(packed) & 0xff) / 255f

  /** A different way to specify an Oklab color, using hue, saturation, lightness, and alpha calculated directly in the Oklab color space.
    */
  def oklabByHSL(hue: Float, saturation: Float, lightness: Float, alpha: Float): Float = {
    val lc   = Math.min(Math.max(lightness, 0f), 1f)
    val sc   = Math.min(Math.max(saturation, 0f), 1f)
    val h    = hue - MathUtils.floor(hue)
    val ac   = Math.min(Math.max(alpha, 0f), 1f)
    val idx  = (lc * 255.999f).toInt << 8 | (256f * h).toInt
    val dist = GAMUT_DATA(idx) * sc * 0.5f
    limitToGamut(
      java.lang.Float.intBitsToFloat(
        (ac * 127.999f).toInt << 25 |
          (TrigTools.sinTurns(h) * dist + 127.5f).toInt << 16 |
          (TrigTools.cosTurns(h) * dist + 127.5f).toInt << 8 |
          (lc * 255.999f).toInt
      )
    )
  }

  /** A different way to specify an Oklab color, using hue, chroma (absolute), lightness, and alpha.
    */
  def oklabByHCL(hue: Float, chroma: Float, lightness: Float, alpha: Float): Float = {
    val lc   = Math.min(Math.max(lightness, 0f), 1f)
    val cc   = Math.max(chroma, 0f)
    val h    = hue - MathUtils.floor(hue)
    val ac   = Math.min(Math.max(alpha, 0f), 1f)
    val idx  = (lc * 255.999f).toInt << 8 | (256f * h).toInt
    val dist = Math.min(cc * 127.5f, GAMUT_DATA(idx) * 0.5f)
    limitToGamut(
      java.lang.Float.intBitsToFloat(
        (ac * 127.999f).toInt << 25 |
          (TrigTools.sinTurns(h) * dist + 127.5f).toInt << 16 |
          (TrigTools.cosTurns(h) * dist + 127.5f).toInt << 8 |
          (lc * 255.999f).toInt
      )
    )
  }

  /** Gets a color as an Oklab packed float given floats for HSL and opacity (using HSL color space). */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat((((opacity * 255f).toInt << 24) & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Given a packed float Oklab color, edits its L, A, B, and alpha channels by first multiplying each channel by the corresponding "mul" parameter and then adding the corresponding "add" parameter.
    */
  def editOklab(encoded: Float, addL: Float, addA: Float, addB: Float, addAlpha: Float, mulL: Float = 1f, mulA: Float = 1f, mulB: Float = 1f, mulAlpha: Float = 1f): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    var L       = (decoded & 0xff) / 255f
    var A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    var B       = ((decoded >>> 16 & 255) - 127f) / 127f
    var al      = (decoded >>> 25) / 127f
    L = Math.min(Math.max(L * mulL + addL, 0f), 1f)
    A = Math.min(Math.max(A * mulA + addA * 2f, -1f), 1f) * 0.5f
    B = Math.min(Math.max(B * mulB + addB * 2f, -1f), 1f) * 0.5f
    al = Math.min(Math.max(al * mulAlpha + addAlpha, 0f), 1f)
    val hue  = TrigTools.atan2Turns(B, A)
    val idx  = (L * 255f).toInt << 8 | (256f * hue).toInt
    val dist = GAMUT_DATA(idx) * 0.5f
    if (dist * dist * 1.5258789e-5f >= (A * A + B * B)) {
      oklab(L, A + 0.5f, B + 0.5f, al)
    } else {
      java.lang.Float.intBitsToFloat(
        (al * 127.999f).toInt << 25 |
          (TrigTools.sinTurns(hue) * dist + 127.5f).toInt << 16 |
          (TrigTools.cosTurns(hue) * dist + 127.5f).toInt << 8 |
          (L * 255f).toInt
      )
    }
  }

  /** Gets the squared Euclidean distance between two Oklab packed float colors. */
  def distanceSquared(encodedA: Float, encodedB: Float): Float = {
    val dA    = java.lang.Float.floatToRawIntBits(encodedA)
    val LA    = reverseLight((dA & 0xff) / 255f)
    val AA    = ((dA >>> 8 & 0xff) - 127f) / 127f
    val BA    = ((dA >>> 16 & 255) - 127f) / 127f
    val dB    = java.lang.Float.floatToRawIntBits(encodedB)
    val LDiff = reverseLight((dB & 0xff) / 255f) - LA
    val ADiff = ((dB >>> 8 & 0xff) - 127f) / 127f - AA
    val BDiff = ((dB >>> 16 & 255) - 127f) / 127f - BA
    LDiff * LDiff + ADiff * ADiff + BDiff * BDiff
  }

  /** Gets the actual Euclidean distance between two Oklab packed float colors. */
  def distance(encodedA: Float, encodedB: Float): Float =
    Math.sqrt(distanceSquared(encodedA, encodedB)).toFloat

  /** Gets the byte value at the given index from the Oklab gamut data. */
  def getRawGamutValue(index: Int): Byte =
    GAMUT_DATA(index)

  /** Gets the color with the same L but the furthest A/B from gray possible, keeping the same hue. */
  def maximizeSaturation(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val A       = (decoded >>> 8 & 0xff) - 127f
    val B       = (decoded >>> 16 & 255) - 127f
    val hue     = TrigTools.atan2Turns(B, A)
    val idx     = (decoded & 0xff) << 8 | (256f * hue).toInt
    val dist    = GAMUT_DATA(idx) * 0.5f
    java.lang.Float.intBitsToFloat(
      (decoded & 0xfe0000ff) |
        (TrigTools.sinTurns(hue) * dist + 127.5f).toInt << 16 |
        (TrigTools.cosTurns(hue) * dist + 127.5f).toInt << 8
    )
  }

  /** Makes the additive Oklab color cause less of a change when used as a tint. */
  def lessenChange(color: Float, fraction: Float): Float = {
    val e  = java.lang.Float.floatToRawIntBits(color)
    val sL = 0x80; val sA     = 0x80; val sB             = 0x80
    val eL = e & 0xff; val eA = (e >>> 8) & 0xff; val eB = (e >>> 16) & 0xff; val eAlpha = e >>> 24 & 0xfe
    java.lang.Float.intBitsToFloat(
      ((sL + fraction * (eL - sL)).toInt & 0xff)
        | (((sA + fraction * (eA - sA)).toInt & 0xff) << 8)
        | (((sB + fraction * (eB - sB)).toInt & 0xff) << 16)
        | (eAlpha << 24)
    )
  }

  /** Pretty simple; inverts lightness for contrast against contrastingColor. */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float = {
    val bits         = java.lang.Float.floatToRawIntBits(mainColor)
    val contrastBits = java.lang.Float.floatToRawIntBits(contrastingColor)
    val L            = bits & 0xff; val A          = bits >>> 8 & 0xff; val B          = bits >>> 16 & 0xff
    val cL           = contrastBits & 0xff; val cA = contrastBits >>> 8 & 0xff; val cB = contrastBits >>> 16 & 0xff
    if ((A - cA) * (A - cA) + (B - cB) * (B - cB) >= 0x10000) {
      mainColor
    } else {
      java.lang.Float.intBitsToFloat((bits & 0xfeffff00) | (if (cL < 128) (L * 0.45f + 140).toInt else (127 - L * 0.45f).toInt))
    }
  }

  /** Modifies mainColor to maximize its lightness difference from contrastingColor. */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(mainColor)
    val contrast = java.lang.Float.floatToRawIntBits(contrastingColor)
    limitToGamut(java.lang.Float.intBitsToFloat((main & 0xfeffff00) | (contrast + 128 & 0xff) + (main & 0xff) >>> 1))
  }

  /** Adds 0.5 to the given color's L and wraps, then averages with original. */
  def offsetLightness(mainColor: Float): Float = {
    val oklab = java.lang.Float.floatToRawIntBits(mainColor)
    limitToGamut(java.lang.Float.intBitsToFloat((oklab & 0xfeffff00) | (oklab + 128 & 0xff) + (oklab & 0xff) >>> 1))
  }

  /** Writes an Oklab-format packed float color into an Oklab-format Color (unchanged from its color space). Internally, this simply calls Color.abgr8888ToColor and returns the edited Color.
    */
  def toOklabColor(editing: Color, oklab: Float): Color = {
    Color.abgr8888ToColor(editing, oklab)
    editing
  }

  /** Gets the saturation of the given encoded color as HSL would calculate it, as a float from 0.0f to 1.0f. */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    if (Math.abs(L - 0.5) > 0.495f) 0f
    else {
      val A = ((decoded >>> 8 & 0xff) - 127f) / 127f
      val B = ((decoded >>> 16 & 255) - 127f) / 127f
      val l = cube(L + 0.3963377774f * A + 0.2158037573f * B)
      val m = cube(L - 0.1055613458f * A - 0.0638541728f * B)
      val s = cube(L - 0.0894841775f * A - 1.2914855480f * B)
      val r = reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f))
      val g = reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f))
      val b = reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f))
      var x = 0f
      var y = 0f
      var w = 0f
      if (g < b) {
        x = b
        y = g
      } else {
        x = g
        y = b
      }
      if (r < x) {
        w = r
      } else {
        w = x
        x = r
      }
      x - Math.min(w, y)
    }
  }

  /** Defined as per HSL; normally you only need channelL() to get accurate lightness for Oklab. */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    val r       = reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f))
    val g       = reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f))
    val b       = reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f))
    var x       = 0f
    var y       = 0f
    var w       = 0f
    if (g < b) {
      x = b
      y = g
    } else {
      x = g
      y = b
    }
    if (r < x) {
      w = r
    } else {
      w = x
      x = r
    }
    val d = x - Math.min(w, y)
    x * (1f - 0.5f * d / (x + 1e-10f))
  }

  /** Gets the hue of the given encoded color, as a float from 0f (inclusive, red) to 1f (exclusive, red). */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val l       = cube(L + 0.3963377774f * A + 0.2158037573f * B)
    val m       = cube(L - 0.1055613458f * A - 0.0638541728f * B)
    val s       = cube(L - 0.0894841775f * A - 1.2914855480f * B)
    val r       = reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f))
    val g       = reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f))
    val b       = reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f))
    var x       = 0f
    var y       = 0f
    var z       = 0f
    var w       = 0f
    if (g < b) {
      x = b
      y = g
      z = -1f
      w = 2f / 3f
    } else {
      x = g
      y = b
      z = 0f
      w = -1f / 3f
    }
    if (r < x) {
      z = w
      w = r
    } else {
      w = x
      x = r
    }
    val d = x - Math.min(w, y)
    Math.abs(z + (w - y) / (6f * d + 1e-10f))
  }

  /** Gets a variation on the packed float color basis as another packed float that has its hue, saturation, lightness, and opacity adjusted by the specified amounts. Note that this edits the color in
    * HSL space, not Oklab! Takes floats representing the amounts of change to apply to hue, saturation, lightness, and opacity; these can be between -1f and 1f.
    */
  def toEditedFloat(basis: Float, hue: Float, saturation: Float, light: Float, opacity: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(basis)
    val L       = Math.min(Math.max(light + reverseLight((decoded & 0xff) / 255f), 0f), 1f)
    val op      = Math.min(Math.max(opacity + (decoded >>> 25) * (1f / 127f), 0f), 1f)
    if (L <= 0.001f) {
      java.lang.Float.intBitsToFloat(((op * 255f).toInt << 24 & 0xfe000000) | 0x808000)
    } else {
      val A = ((decoded >>> 8 & 0xff) - 127f) / 127f
      val B = ((decoded >>> 16 & 255) - 127f) / 127f
      val l = cube(L + 0.3963377774f * A + 0.2158037573f * B)
      val m = cube(L - 0.1055613458f * A - 0.0638541728f * B)
      val s = cube(L - 0.0894841775f * A - 1.2914855480f * B)
      val r = reverseGamma(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f))
      val g = reverseGamma(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f))
      val b = reverseGamma(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f))
      var x = 0f
      var y = 0f
      var z = 0f
      var w = 0f
      if (g < b) {
        x = b
        y = g
        z = -1f
        w = 2f / 3f
      } else {
        x = g
        y = b
        z = 0f
        w = -1f / 3f
      }
      if (r < x) {
        z = w
        w = r
      } else {
        w = x
        x = r
      }
      val d   = x - Math.min(w, y)
      val lum = x * (1f - 0.5f * d / (x + 1e-10f))
      val h   = hue + Math.abs(z + (w - y) / (6f * d + 1e-10f)) + 1f
      val sat = saturation + (x - lum) / (Math.min(lum, 1f - lum) + 1e-10f)
      fromRGBA(FloatColors.hsl2rgb(h - h.toInt, Math.min(Math.max(sat, 0f), 1f), lum, op))
    }
  }

  /** Makes a quasi-randomly-edited variant on the given color, allowing typically a small amount of variance between the given color and what this can return. The seed should be different each time
    * this is called.
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val L       = reverseLight((decoded & 0xff) / 255f)
    val A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    val B       = ((decoded >>> 16 & 255) - 127f) / 127f
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
          val nx = x + L
          val ny = (A + y) * 0.5f + 0.5f
          val nz = (B + z) * 0.5f + 0.5f
          if (inGamut(nx, ny, nz)) {
            break(
              java.lang.Float.intBitsToFloat(
                (decoded & 0xfe000000) | ((nz * 255.5f).toInt << 16 & 0xff0000)
                  | ((ny * 255.5f).toInt << 8 & 0xff00) | (nx * 255.5f).toInt
              )
            )
          }
        }
        j += 1
      }
      color
    }
  }

  /** Gets the color with the same L but the furthest A/B from gray possible, keeping the same hue. This overload takes L, A, B, alpha as separate components.
    */
  def maximizeSaturation(L: Float, A: Float, B: Float, alpha: Float): Float = {
    val lc   = Math.min(Math.max(L, 0f), 1f)
    val ac   = Math.min(Math.max(A, 0f), 1f) - 0.5f
    val bc   = Math.min(Math.max(B, 0f), 1f) - 0.5f
    val alc  = Math.min(Math.max(alpha, 0f), 1f)
    val hue  = TrigTools.atan2Turns(bc, ac)
    val idx  = (lc * 255f).toInt << 8 | (256f * hue).toInt
    val dist = GAMUT_DATA(idx) * 0.5f
    java.lang.Float.intBitsToFloat(
      (alc * 127.999f).toInt << 25 |
        (TrigTools.sinTurns(hue) * dist + 127.5f).toInt << 16 |
        (TrigTools.cosTurns(hue) * dist + 127.5f).toInt << 8 |
        (lc * 255f).toInt
    )
  }

  /** Multiplies the A and B channels of encoded by mul. Typically, mul is non-negative, but if it is negative, that will do some amount of inversion of A and B.
    */
  def multiplyChroma(encoded: Float, mul: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (decoded & 0xff) / 255f
    var A       = ((decoded >>> 8 & 0xff) - 127f) / 127f
    var B       = ((decoded >>> 16 & 255) - 127f) / 127f
    val al      = (decoded >>> 25) / 127f
    A = Math.min(Math.max(A * mul, -1f), 1f) * 0.5f
    B = Math.min(Math.max(B * mul, -1f), 1f) * 0.5f
    val hue  = TrigTools.atan2Turns(B, A)
    val idx  = (L * 255f).toInt << 8 | (256f * hue).toInt
    val dist = GAMUT_DATA(idx) * 0.5f
    if (dist * dist * 1.5258789e-5f >= (A * A + B * B)) {
      oklab(L, A + 0.5f, B + 0.5f, al)
    } else {
      java.lang.Float.intBitsToFloat(
        (al * 127.999f).toInt << 25 |
          (TrigTools.sinTurns(hue) * dist + 127.5f).toInt << 16 |
          (TrigTools.cosTurns(hue) * dist + 127.5f).toInt << 8 |
          (L * 255f).toInt
      )
    }
  }

  /** Produces a random packed float color that is always in-gamut (and opaque) and should be uniformly distributed. */
  def randomColor(random: java.util.Random): Float = {
    var L = random.nextFloat()
    var A = random.nextFloat()
    var B = random.nextFloat()
    while (!inGamut(L, A, B)) {
      L = random.nextFloat()
      A = random.nextFloat()
      B = random.nextFloat()
    }
    oklab(L, A, B, 1f)
  }
}
