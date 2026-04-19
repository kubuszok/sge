/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1036
 * Covenant-baseline-methods: A,A0,A2,B,B0,B2,ColorTools,L,L0,W,X,Y,Z,a,abgr,ac,al,alc,alpha,alphaInt,b,bc,bits,bl,blot,blue,blueInt,cA,cB,cL,cbrtPositive,cc,channelA,channelB,channelL,chroma,chromaLimit,cielab,cielabByHCL,cielabByHSL,cielabHue,cielabLightness,cielabSaturation,clamp,contrast,contrastBits,cos,d,darken,decoded,differentiateLightness,dullen,e,eL,editCIELAB,enrich,fade,floatGetHSL,forwardGamma,forwardXYZ,fromColor,fromRGBA,fromRGBA8888,g,gl,green,greenInt,h,hue,inGamut,inverseLightness,ix,l,lc,lessenChange,li,lighten,lightness,limit,limitToGamut,lowerA,lowerB,main,maximizeSaturation,offsetLightness,op,r,raiseA,raiseB,randomColor,randomEdit,red,redInt,result,reverseGamma,reverseXYZ,rl,s,sL,saturation,sc,sd,sin,toCIELABColor,toColor,toEditedFloat,toRGBA,toRGBA8888,x,x0,y,z
 * Covenant-source-reference: com/github/tommyettinger/colorful/cielab/ColorTools.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful
package cielab

import scala.util.boundary
import scala.util.boundary.break

import sge.colorful.{ FloatColors, TrigTools }
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the CIE L*A*B* color space. This is the old standard (and for some things, gold standard) of color spaces, introduced in
  * 1976 and never fully superseded by a newer color space.
  *
  * The CIE L*A*B* color space has 3 channels, L, A, and B, each gamma-corrected. L is lightness, A is a chroma axis that is (roughly) cyan-vs.-red, and B is a chroma axis that is (roughly)
  * blue-vs.-yellow. This also has an alpha channel in each color, which acts like it does in every other color space package here (multiplicative alpha, higher is more opaque).
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components: L (luminance or lightness), A (a chromatic component ranging from cyan to red), B (a chromatic component ranging from
    * blue to yellow), and alpha (or opacity). L should be between 0 and 1, inclusive. A and B range from 0.0 to 1.0, with grayscale results when both are about 0.5. Alpha is the multiplicative
    * opacity of the color, and acts like RGBA's alpha.
    */
  def cielab(l: Float, a: Float, b: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255.999f).toInt << 24 & 0xfe000000) | ((b * 255.999f).toInt << 16 & 0xff0000)
        | ((a * 255.999f).toInt << 8 & 0xff00) | ((l * 255.999f).toInt & 0xff)
    )

  /** Gets a packed float representation of a color given as 4 float components, L, A, B, and alpha, with each component clamped to the 0f to 1f range before being entered into the packed float color.
    * This is only different from `cielab(float, float, float, float)` in that it clamps each component.
    */
  def clamp(l: Float, a: Float, b: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      (Math.min(Math.max((alpha * 127.999f).toInt, 0), 127) << 25)
        | (Math.min(Math.max((b * 255.999f).toInt, 0), 255) << 16)
        | (Math.min(Math.max((a * 255.999f).toInt, 0), 255) << 8)
        | Math.min(Math.max((l * 255.999f).toInt, 0), 255)
    )

  /** An approximation of the cube-root function for float inputs and outputs. This can be about twice as fast as Math.cbrt(double). This version does not tolerate negative inputs, because in the
    * narrow use case it has in this class, it never is given negative inputs.
    *
    * Has very low relative error (less than 1E-9) when inputs are uniformly distributed between 0 and 512, and absolute mean error of less than 1E-6 in the same scenario. Uses a bit-twiddling method
    * similar to one presented in Hacker's Delight and also used in early 3D graphics (see https://en.wikipedia.org/wiki/Fast_inverse_square_root for more, but this code approximates cbrt(x) and not
    * 1/sqrt(x)). This specific code was originally by Marc B. Reynolds, posted in his "Stand-alone-junk" repo:
    * https://github.com/Marc-B-Reynolds/Stand-alone-junk/blob/master/src/Posts/ballcube.c#L182-L197 .
    *
    * This is used when converting from RGB to CIELAB, as an intermediate step.
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

  /** Used when given non-linear sRGB inputs to make them linear, using an exact gamma of 2.4 and accounting for the darkest colors with a different formula.
    */
  private def forwardGamma(component: Float): Float =
    if (component < 0.04045f) component * (1f / 12.92f)
    else Math.pow((component + 0.055f) * (1f / 1.055f), 2.4f).toFloat

  /** Used to return from a linear, gamma-corrected input to an sRGB, non-linear output, using an exact gamma of 2.4 and accounting for the darkest colors with a different formula.
    */
  private def reverseGamma(component: Float): Float =
    if (component < 0.0031308f) component * 12.92f
    else Math.pow(component, 1f / 2.4f).toFloat * 1.055f - 0.055f

  private def forwardXYZ(t: Float): Float =
    if (t < 0.00885645f) 7.787037f * t + 0.139731f else cbrtPositive(t)

  private def reverseXYZ(t: Float): Float =
    if (t < 0.20689655f) 0.1284185f * (t - 0.139731f) else t * t * t

  /** Converts a packed float color in the format produced by `cielab(float, float, float, float)` to an RGBA8888 int.
    */
  def toRGBA8888(packed: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    val r       = (reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f)) * 255.999f).toInt
    val g       = (reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f)) * 255.999f).toInt
    val b       = (reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f)) * 255.999f).toInt
    r << 24 | g << 16 | b << 8 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Converts a packed float color in the format produced by `cielab(float, float, float, float)` to a packed float in ABGR8888 format.
    */
  def toRGBA(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    val r       = (reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f)) * 255.999f).toInt
    val g       = (reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f)) * 255.999f).toInt
    val b       = (reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f)) * 255.999f).toInt
    java.lang.Float.intBitsToFloat(r | g << 8 | b << 16 | (decoded & 0xfe000000))
  }

  /** Writes a CIELAB-format packed float color into an RGBA8888 Color as used by libGDX (called `editing`). */
  def toColor(editing: Color, packed: Float): Color = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    //        final float A = ((decoded >>> 8 & 0xff) - 127.5f) * (1f / 127.5f);
    //        final float B = ((decoded >>> 16 & 0xff) - 127.5f) * (1f / 127.5f);
    val A = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x = reverseXYZ(L + A)
    val y = reverseXYZ(L)
    val z = reverseXYZ(L - B)
    editing.r = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    editing.g = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    editing.b = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    // 0x1.020408p-7f is 1/127 as a float = 0.007874016f
    editing.a = (decoded >>> 25) * 0.007874016f
    editing.clamp()
  }

  /** Writes a CIELAB-format packed float color into a CIELAB-format Color called `editing`. This is mostly useful if the rest of your application expects colors in CIELAB format. Internally, this
    * simply calls Color.abgr8888ToColor and returns the edited Color.
    */
  def toCIELABColor(editing: Color, cielab: Float): Color = {
    Color.abgr8888ToColor(editing, cielab)
    editing
  }

  /** Takes a color encoded as an RGBA8888 int and converts to a packed float in the CIELAB format this uses. */
  def fromRGBA8888(rgba: Int): Float = {
    // 0x1.010101010101p-8 = 0.003921569f (1/255 with higher precision)
    val r = forwardGamma((rgba >>> 24) * 0.003921569f)
    val g = forwardGamma((rgba >>> 16 & 0xff) * 0.003921569f)
    val b = forwardGamma((rgba >>> 8 & 0xff) * 0.003921569f)

    val x = forwardXYZ(0.4124564f * r + 0.3575761f * g + 0.1804375f * b)
    val y = forwardXYZ(0.2126729f * r + 0.7151522f * g + 0.0721750f * b)
    val z = forwardXYZ(0.0193339f * r + 0.1191920f * g + 0.9503041f * b)

    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((1.16f * y - 0.16f) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((x - y) * (127.999f * 5f) + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((y - z) * (127.999f * 2f) + 127.5f).toInt, 0), 255) << 16
        | (rgba & 0xfe) << 24
    )
  }

  /** Takes a color encoded as an ABGR packed float and converts to a packed float in the CIELAB format this uses. */
  def fromRGBA(packed: Float): Float = {
    val abgr = java.lang.Float.floatToRawIntBits(packed)
    val r    = forwardGamma((abgr & 0xff) * 0.003921569f)
    val g    = forwardGamma((abgr >>> 8 & 0xff) * 0.003921569f)
    val b    = forwardGamma((abgr >>> 16 & 0xff) * 0.003921569f)

    val x = forwardXYZ(0.4124564f * r + 0.3575761f * g + 0.1804375f * b)
    val y = forwardXYZ(0.2126729f * r + 0.7151522f * g + 0.0721750f * b)
    val z = forwardXYZ(0.0193339f * r + 0.1191920f * g + 0.9503041f * b)

    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((1.16f * y - 0.16f) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((x - y) * (127.999f * 5f) + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((y - z) * (127.999f * 2f) + 127.5f).toInt, 0), 255) << 16
        | (abgr & 0xfe000000)
    )
  }

  /** Takes a libGDX Color that uses RGBA8888 channels and converts to a packed float in the CIELAB format this uses. */
  def fromColor(color: Color): Float = {
    val r = forwardGamma(color.r)
    val g = forwardGamma(color.g)
    val b = forwardGamma(color.b)
    val x = forwardXYZ(0.4124564f * r + 0.3575761f * g + 0.1804375f * b)
    val y = forwardXYZ(0.2126729f * r + 0.7151522f * g + 0.0721750f * b)
    val z = forwardXYZ(0.0193339f * r + 0.1191920f * g + 0.9503041f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((1.16f * y - 0.16f) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((x - y) * (127.999f * 5f) + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((y - z) * (127.999f * 2f) + 127.5f).toInt, 0), 255) << 16
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )
  }

  /** Takes RGBA components from 0.0 to 1.0 each and converts to a packed float in the CIELAB format this uses. */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float = {
    val rl = forwardGamma(r)
    val gl = forwardGamma(g)
    val bl = forwardGamma(b)
    val x  = forwardXYZ(0.4124564f * rl + 0.3575761f * gl + 0.1804375f * bl)
    val y  = forwardXYZ(0.2126729f * rl + 0.7151522f * gl + 0.0721750f * bl)
    val z  = forwardXYZ(0.0193339f * rl + 0.1191920f * gl + 0.9503041f * bl)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((1.16f * y - 0.16f) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((x - y) * (127.999f * 5f) + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((y - z) * (127.999f * 2f) + 127.5f).toInt, 0), 255) << 16
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )
  }

  /** Gets the red channel value of the given encoded color, as an int ranging from 0 to 255, inclusive. */
  def redInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    (reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the green channel value of the given encoded color, as an int ranging from 0 to 255, inclusive. */
  def greenInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    (reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the blue channel value of the given encoded color, as an int ranging from 0 to 255, inclusive. */
  def blueInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    (reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the alpha channel value of the given encoded color, as an even int ranging from 0 to 254, inclusive. Because of how alpha is stored in libGDX, no odd-number values are possible for alpha.
    */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the red channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def red(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
  }

  /** Gets the green channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def green(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
  }

  /** Gets the blue channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def blue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
  }

  /** Gets the alpha channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def alpha(encoded: Float): Float =
    // 0x1.020408p-8f = 0.003937008f (1/254)
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** Gets the "chroma" or "colorfulness" of a given CIELAB color. Chroma is similar to saturation in that grayscale values have 0 saturation and 0 chroma, while brighter colors have high saturation
    * and chroma. The result of this method can't be negative, grayscale values have very close to 0 chroma, and the most colorful value (a shade of purple) should have 1.26365817 chroma.
    */
  def chroma(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val a       = ((decoded >>> 7 & 0x1fe) - 255) / 255f
    val b       = ((decoded >>> 15 & 0x1fe) - 255) / 255f
    Math.sqrt(a * a + b * b).toFloat
  }

  /** Given a hue and lightness, this gets the (very approximate) maximum chroma possible for that hue-lightness combination, using CIELAB's versions of lightness and hue (not HSL). This is useful to
    * know the bounds of `chroma(float)`. This should be no greater than 1.26365817f. Note that this version of chromaLimit() is much slower than Oklab's version, because this has to go to much
    * greater lengths to become accurate.
    */
  def chromaLimit(hue: Float, lightness: Float): Float = {
    val h  = hue - MathUtils.floor(hue)
    val L  = (1f / 1.16f) * (lightness + 0.16f)
    val A  = TrigTools.cosTurns(h) * 1.26365817f
    val B  = TrigTools.sinTurns(h) * 1.26365817f
    val y  = reverseXYZ(L)
    var A2 = A
    var B2 = B
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x = reverseXYZ(L + A2)
        val z = reverseXYZ(L - B2)
        val r = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val g = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val b = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(Math.sqrt(A2 * A2 + B2 * B2).toFloat)
        // 0x1p-7f = 0.0078125f (1/128)
        val progress = attempt * 0.0078125f
        A2 = A * progress
        B2 = B * progress
        attempt -= 1
      }
      Math.sqrt(A2 * A2 + B2 * B2).toFloat
    }
  }

  /** Gets the color with the same L as the CIELAB color stored in the given packed float, but the furthest A B from gray possible for that lightness while keeping the same hue as the given color.
    * This is very similar to calling `enrich(float, float)` with a very large `change` value.
    */
  def maximizeSaturation(packed: Float): Float = {
    val decoded   = java.lang.Float.floatToRawIntBits(packed)
    val lightness = (decoded & 255) / 255f
    val h         = TrigTools.atan2Turns((decoded >>> 16 & 0xff) - 127.5f, (decoded >>> 8 & 0xff) - 127.5f)
    val L         = (1f / 1.16f) * (lightness + 0.16f)
    val A         = TrigTools.cosTurns(h) * 1.26365817f * 0.2f
    val B         = TrigTools.sinTurns(h) * 1.26365817f * 0.5f
    val y         = reverseXYZ(L)
    var A2        = A
    var B2        = B
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x = reverseXYZ(L + A2)
        val z = reverseXYZ(L - B2)
        val r = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val g = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val b = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(cielab(lightness, A2 * 0.5f + 0.5f, B2 * 0.5f + 0.5f, (decoded >>> 25) / 127f))
        // 0x1p-7f = 0.0078125f
        val progress = attempt * 0.0078125f
        A2 = A * progress
        B2 = B * progress
        attempt -= 1
      }
      cielab(lightness, A2 * 0.5f + 0.5f, B2 * 0.5f + 0.5f, (decoded >>> 25) / 127f)
    }
  }

  /** Gets the color with the same L as the CIELAB color stored in the given packed float, but the furthest A B from gray possible for that lightness while keeping the same hue as the given color.
    * This overload takes L, A, B, alpha as separate components.
    */
  def maximizeSaturation(L: Float, A: Float, B: Float, alpha: Float): Float = {
    val lc  = Math.min(Math.max(L, 0f), 1f)
    val ac  = Math.min(Math.max(A, 0f), 1f)
    val bc  = Math.min(Math.max(B, 0f), 1f)
    val alc = Math.min(Math.max(alpha, 0f), 1f)
    val h   = TrigTools.atan2Turns(bc - 0.5f, ac - 0.5f)
    val L0  = (1f / 1.16f) * (lc + 0.16f)
    val A0  = TrigTools.cosTurns(h) * 1.26365817f
    val B0  = TrigTools.sinTurns(h) * 1.26365817f
    val y   = reverseXYZ(L0)
    var A2  = A0
    var B2  = B0
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x = reverseXYZ(L0 + A2)
        val z = reverseXYZ(L0 - B2)
        val r = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val g = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val b = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(cielab(lc, A2, B2, alc))
        // 0x1p-7f = 0.0078125f
        val progress = attempt * 0.0078125f
        A2 = A0 * progress
        B2 = B0 * progress
        attempt -= 1
      }
      cielab(lc, A2, B2, alc)
    }
  }

  /** Gets the hue of the given CIELAB float color, but as CIELAB understands hue rather than how HSL does. This is different from `hue(float)`, which uses HSL. This gives a float between 0
    * (inclusive) and 1 (exclusive).
    */
  def cielabHue(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    TrigTools.atan2Turns(B, A)
  }

  /** Gets the saturation of the given CIELAB float color, but as CIELAB understands saturation rather than how HSL does. Saturation here is a fraction of the chroma limit for a given hue and
    * lightness, and is between 0 and 1.
    */
  def cielabSaturation(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = (1f / 1.16f) * (((decoded & 0xff) / 255f) + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val h       = TrigTools.atan2Turns(B, A)
    val L0      = (1f / 1.16f) * (L + 0.16f)
    val A0      = TrigTools.cosTurns(h) * 1.26365817f
    val B0      = TrigTools.sinTurns(h) * 1.26365817f
    val y       = reverseXYZ(L0)
    var A2      = A0
    var B2      = B0
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x = reverseXYZ(L + A2)
        val z = reverseXYZ(L - B2)
        val r = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val g = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val b = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
          val dist = Math.sqrt(A2 * A2 + B2 * B2).toFloat
          break(Math.sqrt(A * A + B * B).toFloat / dist)
        }
        // 0x1p-7f = 0.0078125f
        val progress = attempt * 0.0078125f
        A2 = A0 * progress
        B2 = B0 * progress
        attempt -= 1
      }
      val dist = Math.sqrt(A2 * A2 + B2 * B2).toFloat
      Math.sqrt(A * A + B * B).toFloat / dist
    }
  }

  /** Gets the lightness of the given CIELAB float color, but as CIELAB understands lightness rather than how HSL does. This is the same as `channelL(float)`.
    */
  def cielabLightness(packed: Float): Float =
    (java.lang.Float.floatToRawIntBits(packed) & 0xff) / 255f

  /** A different way to specify a CIELAB color, using hue, saturation, lightness, and alpha like a normal HSL(A) color but calculating them directly in the CIELAB color space. This is more efficient
    * than `floatGetHSL(float, float, float, float)`. You may prefer using `cielabByHCL(float, float, float, float)`, which takes an absolute chroma as opposed to the saturation here (which is a
    * fraction of the maximum chroma). This method is likely to be significantly slower than `cielabByHCL` because this needs to calculate the gamut.
    */
  def cielabByHSL(hue: Float, saturation: Float, lightness: Float, alpha: Float): Float = {
    val lc  = Math.min(Math.max(lightness, 0f), 1f)
    val sc  = Math.min(Math.max(saturation, 0f), 1f)
    val h   = hue - MathUtils.floor(hue)
    val alc = Math.min(Math.max(alpha, 0f), 1f)
    val L   = (1f / 1.16f) * (lc + 0.16f)
    val L0  = (1f / 1.16f) * (L + 0.16f)
    val cos = TrigTools.cosTurns(h)
    val sin = TrigTools.sinTurns(h)
    val A0  = cos * 1.26365817f * 0.2f
    val B0  = sin * 1.26365817f * 0.5f
    val y   = reverseXYZ(L0)
    var A2  = A0
    var B2  = B0
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x = reverseXYZ(L + A2)
        val z = reverseXYZ(L - B2)
        val r = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val g = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val b = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
          val dist = Math.sqrt(A2 * A2 + B2 * B2).toFloat * sc
          break(
            java.lang.Float.intBitsToFloat(
              (alc * 127.999f).toInt << 25
                | Math.min(Math.max((sin * dist + 127.5f).toInt, 0), 255) << 16
                | Math.min(Math.max((cos * dist + 127.5f).toInt, 0), 255) << 8
                | (lc * 255.999f).toInt
            )
          )
        }
        // 0x1p-7f = 0.0078125f
        val progress = attempt * 0.0078125f
        A2 = A0 * progress
        B2 = B0 * progress
        attempt -= 1
      }
      val dist = Math.sqrt(A2 * A2 + B2 * B2).toFloat * sc
      java.lang.Float.intBitsToFloat(
        (alc * 127.999f).toInt << 25
          | Math.min(Math.max((sin * dist + 127.5f).toInt, 0), 255) << 16
          | Math.min(Math.max((cos * dist + 127.5f).toInt, 0), 255) << 8
          | (lc * 255.999f).toInt
      )
    }
  }

  /** A different way to specify a CIELAB color, using hue, chroma, lightness, and alpha something like a normal HSL(A) color but calculating them directly in the CIELAB color space. This has you
    * specify the desired chroma directly, as obtainable with `chroma(float)`, rather than the saturation. This method should be significantly faster than `cielabByHSL` because it doesn't need to
    * calculate the gamut.
    */
  def cielabByHCL(hue: Float, chroma: Float, lightness: Float, alpha: Float): Float = {
    val lc  = Math.min(Math.max(lightness, 0f), 1f)
    val cc  = Math.max(chroma, 0f) * 127.5f
    val h   = hue - MathUtils.floor(hue)
    val alc = Math.min(Math.max(alpha, 0f), 1f)
    java.lang.Float.intBitsToFloat(
      (alc * 127.999f).toInt << 25
        | Math.min(Math.max((TrigTools.sinTurns(h) /* * 2f */ * cc + 127.5f).toInt, 0), 255) << 16
        | Math.min(Math.max((TrigTools.cosTurns(h) /* * 5f */ * cc + 127.5f).toInt, 0), 255) << 8
        | (lc * 255.999f).toInt
    )
  }

  /** Gets a color as a CIELAB packed float given floats representing hue, saturation, lightness, and opacity. All parameters should normally be between 0 and 1 inclusive, though any hue is tolerated
    * (precision loss may affect the color if the hue is too large). A lightness of 0.001f or less is always black (also using a shortcut if this is the case, respecting opacity), while a lightness of
    * 1f is white.
    */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Gets the saturation of the given encoded color as HSL would calculate it, as a float ranging from 0.0f to 1.0f, inclusive. This is different from `chroma(float)`; see that method's documentation
    * for details.
    */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    if (Math.abs(L - 0.5) > 0.495f) 0f
    else {
      val A = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
      val B = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
      val x = reverseXYZ(L + A)
      val y = reverseXYZ(L)
      val z = reverseXYZ(L - B)
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
  }

  /** Defined as per HSL; normally you only need `channelL(float)` to get accurate lightness for CIELAB. This ranges from 0.0f (black) to 1.0f (white).
    */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    val r       = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    val g       = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    val b       = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    var X       = 0f
    var Y       = 0f
    var W       = 0f
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
    */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    val r       = reverseGamma(Math.min(Math.max(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z, 0f), 1f))
    val g       = reverseGamma(Math.min(Math.max(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z, 0f), 1f))
    val b       = reverseGamma(Math.min(Math.max(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z, 0f), 1f))
    var X       = 0f
    var Y       = 0f
    var Z       = 0f
    var W       = 0f
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
    * HSL space, not CIELAB! Takes floats representing the amounts of change to apply to hue, saturation, lightness, and opacity; these can be between -1f and 1f.
    */
  def toEditedFloat(basis: Float, hue: Float, saturation: Float, light: Float, opacity: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(basis)
    val li      = Math.min(Math.max(light + (decoded & 0xff) / 255f, 0f), 1f)
    val op      = Math.min(Math.max(opacity + (decoded >>> 25) * (1f / 127f), 0f), 1f)
    if (li <= 0.001f) {
      java.lang.Float.intBitsToFloat(((op * 255f).toInt << 24 & 0xfe000000) | 0x808000)
    } else {
      val L = (1f / 1.16f) * (li + 0.16f)
      val A = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
      val B = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
      val x = reverseXYZ(L + A)
      val y = reverseXYZ(L)
      val z = reverseXYZ(L - B)
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
      val d   = X - Math.min(W, Y)
      val lum = X * (1f - 0.5f * d / (X + 1e-10f))
      val h   = hue + Math.abs(Z + (W - Y) / (6f * d + 1e-10f)) + 1f
      val sat = saturation + (X - lum) / (Math.min(lum, 1f - lum) + 1e-10f)
      fromRGBA(FloatColors.hsl2rgb(h - h.toInt, Math.min(Math.max(sat, 0f), 1f), lum, op))
    }
  }

  /** Given a packed float CIELAB color, this edits its L, A, B, and alpha channels by adding the corresponding "add" parameter and then clamping. This returns a different float value. You can give a
    * value of 0 for any "add" parameter you want to stay unchanged. This clamps the resulting color so it contains in-range L, A, B, and alpha values, but it doesn't guarantee it stays in-gamut.
    */
  def editCIELAB(encoded: Float, addL: Float, addA: Float, addB: Float, addAlpha: Float): Float =
    editCIELAB(encoded, addL, addA, addB, addAlpha, 1f, 1f, 1f, 1f)

  /** Given a packed float CIELAB color, this edits its L, A, B, and alpha channels by first multiplying each channel by the corresponding "mul" parameter and then adding the corresponding "add"
    * parameter, before clamping. This means the lightness value L is multiplied by mulL, then has addL added, and then is clamped to the normal range for L (0 to 1). Note that this manipulates A and
    * B in the -0.5 to 0.5 range, so if you multiply by a small number like 0.25f, then this will produce a less-saturated color. This clamps the resulting color so it contains in-range L, A, B, and
    * alpha values, but it doesn't guarantee it stays in-gamut.
    */
  def editCIELAB(
    encoded:  Float,
    addL:     Float,
    addA:     Float,
    addB:     Float,
    addAlpha: Float,
    mulL:     Float,
    mulA:     Float,
    mulB:     Float,
    mulAlpha: Float
  ): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    var L       = (decoded & 0xff) / 255f
    var A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    var B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    var al      = (decoded >>> 25) / 127f

    L = Math.min(Math.max(L * mulL + addL, 0f), 1f)
    A = Math.min(Math.max(A * mulA + addA * 2f, -1f), 1f) * 0.5f
    B = Math.min(Math.max(B * mulB + addB * 2f, -1f), 1f) * 0.5f
    al = Math.min(Math.max(al * mulAlpha + addAlpha, 0f), 1f)
    clamp(L, A, B, al)
  }

  /** The "L" channel of the given packed float in CIELAB format, which is its lightness; ranges from 0.0f to 1.0f. You can edit the L of a color with `lighten(float, float)` and
    * `darken(float, float)`.
    */
  def channelL(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "A" channel of the given packed float in CIELAB format, which when combined with the B channel describes the hue and saturation of a color; ranges from 0f to 1f. If A is 0f, the color will
    * be cooler, more green or blue; if A is 1f, the color will be warmer, from magenta to orange. You can edit the A of a color with `raiseA(float, float)` and `lowerA(float, float)`.
    */
  def channelA(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "B" channel of the given packed float in CIELAB format, which when combined with the A channel describes the hue and saturation of a color; ranges from 0f to 1f. If B is 0f, the color will
    * be more "artificial", more blue or purple; if B is 1f, the color will be more "natural", from green to yellow to orange. You can edit the B of a color with `raiseB(float, float)` and
    * `lowerB(float, float)`.
    */
  def channelB(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

  /** Interpolates from start towards white by change. */
  def lighten(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i + (0xff - i) * change).toInt & 0xff) | other)
  }

  /** Interpolates from start towards black by change. */
  def darken(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i * (1f - change)).toInt & 0xff) | other)
  }

  /** Interpolates from start towards a warmer color (orange to magenta) by change. */
  def raiseA(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val p = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p + (0xff - p) * change).toInt << 8 & 0xff00) | other)
  }

  /** Interpolates from start towards a cooler color (green to blue) by change. */
  def lowerA(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val p = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Interpolates from start towards "natural" (between green and orange) by change. */
  def raiseB(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 16 & 0xff0000) | other)
  }

  /** Interpolates from start towards "artificial" (between blue and purple) by change. */
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
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity * (1f - change)).toInt & 0xfe) << 24 | other)
  }

  /** Brings the chromatic components of start closer to grayscale by change (desaturating). */
  def dullen(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    cielab(
      (s & 0xff) / 255f,
      ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f,
      ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f,
      (s >>> 25) / 127f
    )
  }

  /** Pushes the chromatic components of start away from grayscale by change (saturating). This prevents high values for change from pushing A or B out of the valid range by using
    * `clamp(float, float, float, float)`; this doesn't actually keep the color in-gamut, but usually rendering code can handle out-of-gamut colors in some way.
    */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    clamp(
      (s & 0xff) / 255f,
      ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f,
      ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f,
      (s >>> 25) / 127f
    )
  }

  /** Given a packed float CIELAB color `mainColor` and another CIELAB color that it should be made to contrast with, gets a packed float CIELAB color with roughly inverted lightness but the same
    * chromatic channels and opacity. This won't ever produce black or other very dark colors, and also has a gap in the range it produces for intensity values between 0.5 and 0.55. This will leave
    * the lightness unchanged if the chromatic channels of the contrastingColor and those of the mainColor are already very different.
    */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float = {
    val bits         = java.lang.Float.floatToRawIntBits(mainColor)
    val contrastBits = java.lang.Float.floatToRawIntBits(contrastingColor)
    val L            = bits & 0xff
    val A            = bits >>> 8 & 0xff
    val B            = bits >>> 16 & 0xff
    val cL           = contrastBits & 0xff
    val cA           = contrastBits >>> 8 & 0xff
    val cB           = contrastBits >>> 16 & 0xff
    if ((A - cA) * (A - cA) + (B - cB) * (B - cB) >= 0x10000) {
      mainColor
    } else {
      // 0x1.0p-8f = 0.00390625f (1/256)
      cielab(
        if (cL < 128) L * (0.45f / 255f) + 0.5f else 0.5f - L * (0.45f / 255f),
        A / 255f,
        B / 255f,
        0.00390625f * (bits >>> 24)
      )
    }
  }

  /** Given a packed float CIELAB color `mainColor` and another CIELAB color that it should be made to contrast with, gets a packed float CIELAB color with L that should be quite different from
    * `contrastingColor`'s L, but the same chromatic channels and opacity. This is similar to `inverseLightness(float, float)`, but is considerably simpler, and this method will change the lightness
    * of mainColor when the two given colors have close lightness but distant chroma. Because it averages the original L of mainColor with the modified one, this tends to not produce harsh color
    * changes.
    */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(mainColor)
    val contrast = java.lang.Float.floatToRawIntBits(contrastingColor)
    java.lang.Float.intBitsToFloat((main & 0xfeffff00) | (contrast + 128 & 0xff) + (main & 0xff) >>> 1)
  }

  /** Pretty simple; adds 0.5 to the given color's L and wraps it around if it would go above 1.0, then averages that with the original L. This means light colors become darker, and dark colors become
    * lighter, with almost all results in the middle-range of possible lightness.
    */
  def offsetLightness(mainColor: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(mainColor)
    java.lang.Float.intBitsToFloat((decoded & 0xfeffff00) | (decoded + 128 & 0xff) + (decoded & 0xff) >>> 1)
  }

  /** Makes the additive CIELAB color cause less of a change when used as a tint. */
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

  /** Returns true if the given packed float color, as CIELAB, is valid to convert losslessly back to RGBA. */
  def inGamut(packed: Float): Boolean = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val x       = reverseXYZ(L + A)
    val y       = reverseXYZ(L)
    val z       = reverseXYZ(L - B)
    // -0x1p-8f = -0.00390625f, 0x1.01p0f = 1.00390625f
    val r = +3.2404542f * x + -1.5371385f * y + -0.4985314f * z
    if (r <= -0.00390625f || r >= 1.00390625f) false
    else {
      val g = -0.9692660f * x + +1.8760108f * y + +0.0415560f * z
      if (g <= -0.00390625f || g >= 1.00390625f) false
      else {
        val b = +0.0556434f * x + -0.2040259f * y + +1.0572252f * z
        b > -0.00390625f && b < 1.00390625f
      }
    }
  }

  /** Returns true if the given CIELAB values are valid to convert losslessly back to RGBA. */
  def inGamut(L: Float, A: Float, B: Float): Boolean = {
    val l = (1f / 1.16f) * (L + 0.16f)
    val a = (A - 0.5f) * 0.4f
    val b = B - 0.5f
    val x = reverseXYZ(l + a)
    val y = reverseXYZ(l)
    val z = reverseXYZ(l - b)
    val r = +3.2404542f * x + -1.5371385f * y + -0.4985314f * z
    if (r < 0f || r > 1.0f) false
    else {
      val g = -0.9692660f * x + +1.8760108f * y + +0.0415560f * z
      if (g < 0f || g > 1.0f) false
      else {
        val bv = +0.0556434f * x + -0.2040259f * y + +1.0572252f * z
        bv >= 0f && bv <= 1.0f
      }
    }
  }

  /** Iteratively checks whether the given CIELAB color is in-gamut, and either brings the color closer to grayscale if it isn't in-gamut, or returns it as soon as it is in-gamut. Maintains the L of
    * the color, only bringing A and B closer to grayscale. Note that this version of limitToGamut() is much slower than Oklab's version, because Oklab stores its entire gamut as a large constant,
    * while this has to calculate it.
    */
  def limitToGamut(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val L       = (1f / 1.16f) * ((decoded & 0xff) / 255f + 0.16f)
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
    val y       = reverseXYZ(L)
    var A2      = A
    var B2      = B
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x = reverseXYZ(L + A2)
        val z = reverseXYZ(L - B2)
        val r = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val g = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val b = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(cielab(L, A2 * 0.5f + 0.5f, B2 * 0.5f + 0.5f, (decoded >>> 25) / 127f))
        // 0x1p-7f = 0.0078125f
        val progress = attempt * 0.0078125f
        A2 = A * progress
        B2 = B * progress
        attempt -= 1
      }
      cielab(L, A2 * 0.5f + 0.5f, B2 * 0.5f + 0.5f, (decoded >>> 25) / 127f)
    }
  }

  /** Iteratively checks whether the given CIELAB color is in-gamut, and either brings the color closer to grayscale if it isn't in-gamut, or returns it as soon as it is in-gamut. Maintains the L of
    * the color, only bringing A and B closer to grayscale. This always produces an opaque color.
    */
  def limitToGamut(L: Float, A: Float, B: Float): Float =
    limitToGamut(L, A, B, 1f)

  /** Iteratively checks whether the given CIELAB color is in-gamut, and either brings the color closer to grayscale if it isn't in-gamut, or returns it as soon as it is in-gamut. Note that this
    * version of limitToGamut() is much slower than Oklab's version, because Oklab stores its entire gamut as a large constant, while this has to calculate it.
    */
  def limitToGamut(L: Float, A: Float, B: Float, alpha: Float): Float = {
    val l  = (1f / 1.16f) * (Math.min(Math.max(L, 0f), 1f) + 0.16f)
    val a  = (Math.min(Math.max(A, 0f), 1f) - 0.5f) * 0.4f
    val b  = Math.min(Math.max(B, 0f), 1f) - 0.5f
    val al = Math.min(Math.max(alpha, 0f), 1f)

    val y  = reverseXYZ(l)
    var A2 = a
    var B2 = b
    boundary[Float] {
      var attempt = 127
      while (attempt >= 0) {
        val x  = reverseXYZ(l + A2)
        val z  = reverseXYZ(l - B2)
        val rv = reverseGamma(+3.2404542f * x + -1.5371385f * y + -0.4985314f * z)
        val gv = reverseGamma(-0.9692660f * x + +1.8760108f * y + +0.0415560f * z)
        val bv = reverseGamma(+0.0556434f * x + -0.2040259f * y + +1.0572252f * z)
        if (rv >= 0f && rv <= 1f && gv >= 0f && gv <= 1f && bv >= 0f && bv <= 1f)
          break(cielab(l, A2 * 0.5f + 0.5f, B2 * 0.5f + 0.5f, al))
        // 0x1p-7f = 0.0078125f
        val progress = attempt * 0.0078125f
        A2 = a * progress
        B2 = b * progress
        attempt -= 1
      }
      cielab(l, A2 * 0.5f + 0.5f, B2 * 0.5f + 0.5f, al)
    }
  }

  /** Makes a quasi-randomly-edited variant on the given `color`, allowing typically a small amount of `variance` (such as 0.05 to 0.25) between the given color and what this can return. The `seed`
    * should be different each time this is called, and can be obtained from a random number generator to make the colors more random, or can be incremented on each call.
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val L       = (decoded & 0xff) / 255f
    val A       = ((decoded >>> 8 & 0xff) - 127.5f) * (0.2f / 127.5f)
    val B       = ((decoded >>> 16 & 0xff) - 127.5f) * (0.5f / 127.5f)
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
          break(clamp(x + L, (A + y) * 0.5f + 0.5f, (B + z) * 0.5f + 0.5f, (decoded >>> 25) / 127f))
        }
        j += 1
      }
      color
    }
  }

  /** Produces a random packed float color that is always in-gamut and should be uniformly distributed. */
  def randomColor(random: java.util.Random): Float = {
    var L = random.nextFloat()
    var A = random.nextFloat()
    var B = random.nextFloat()
    while (!inGamut(L, A, B)) {
      L = random.nextFloat()
      A = random.nextFloat()
      B = random.nextFloat()
    }
    cielab(L, A, B, 1f)
  }
}
