/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1204
 * Covenant-baseline-methods: ColorTools,abgr,al,alpha,alphaInt,b,bc,bits,blot,blue,blueInt,bv,ci,contrast,contrastBits,cp,crMid,crScale,ct,d,darken,decoded,differentiateLightness,dullen,e,eAlpha,eI,eP,eT,editIPT,enrich,fade,floatGetHSL,fromColor,fromHSI,fromRGBA,fromRGBA8888,g,gc,green,greenInt,gv,h,hue,i,i2,ib,ig,inGamut,intensity,inverseLightness,ipt,ir,lessenChange,lighten,lightness,limit,limitToGamut,main,mgMid,mgScale,offsetLightness,op,opacity,other,p,p2,pOrig,protan,protanDown,protanUp,r,randomColor,randomEdit,rc,red,redInt,rv,s,sI,sP,sT,saturation,scale,subrandomColor,t,t2,tOrig,toColor,toEditedFloat,toIPTColor,toRGBA,toRGBA8888,tritan,tritanDown,tritanUp,w,x,y,ybMid,ybScale,z
 * Covenant-source-reference: com/github/tommyettinger/colorful/ipt/ColorTools.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: e4a5fd960eef746ca5aa826063432fb79666d74f
 */
package sge
package colorful
package ipt

import scala.util.boundary
import scala.util.boundary.break

import sge.colorful.{ FloatColors, TrigTools }
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the IPT color space. IPT has more perceptually-uniform handling of hue than some other color spaces, like YCwCm, and
  * even though the version here gives up the complex exponential adjustments to various components that the original IPT paper used, it still is pretty good at preserving perceptual lightness. In
  * most regards, this is a more thoroughly-constructed color space than YCwCm, but YCwCm may still be useful because of how it maps to aesthetic components of color. See
  * [[ipt(float,float,float,float)]] for docs on the I, P, and T channels.
  *
  * You may prefer the IPT_HQ color space, [[sge.colorful.ipt_hq.ColorTools]], if you want better lightness handling at the expense of a small amount of speed. The Oklab color space is related to IPT
  * and IPT_HQ, [[sge.colorful.oklab.ColorTools]], and is faster at some complex operations.
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components, here, I (intensity or lightness), P (protan, a chromatic component ranging from greenish to reddish), T (tritan, a
    * chromatic component ranging from bluish to yellowish), and A (alpha or opacity). Intensity should be between 0 and 1, inclusive, with 0 used for very dark colors (almost only black), and 1 used
    * for very light colors (almost only white). Protan and tritan range from 0.0 to 1.0, with grayscale results when both are about 0.5. There's some aesthetic value in changing just one chroma
    * value. When protan is high and tritan is low, the color is more purple/magenta, when both are low it is more bluish, when tritan is high and protan is low, the color tends to be greenish, and
    * when both are high it tends to be orange. When protan and tritan are both near 0.5f, the color is closer to gray. Alpha is the multiplicative opacity of the color, and acts like RGBA's alpha.
    *
    * This method bit-masks the resulting color's byte values, so any values can technically be given to this as intensity, protan, and tritan, but they will only be reversible from the returned float
    * color to the original I, P, and T values if the original values were in the range that [[intensity(float)]], [[protan(float)]], and [[tritan(float)]] return.
    *
    * @param intens
    *   0f to 1f, intensity or I component of IPT, with 0.5f meaning "no change" and 1f brightening
    * @param protan
    *   0f to 1f, protan or P component of IPT, with 1f more orange, red, or magenta
    * @param tritan
    *   0f to 1f, tritan or T component of IPT, with 1f more green, yellow, or red
    * @param alpha
    *   0f to 1f, 0f makes the color transparent and 1f makes it opaque
    * @return
    *   a float encoding a color with the given properties
    */
  def ipt(intens: Float, protan: Float, tritan: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255).toInt << 24 & 0xfe000000) | ((tritan * 255).toInt << 16 & 0xff0000)
        | ((protan * 255).toInt << 8 & 0xff00) | ((intens * 255).toInt & 0xff)
    )

  /** Converts a packed float color in the format produced by [[ipt(float,float,float,float)]] to an RGBA8888 int. This format of int can be used with Pixmap and in some other places.
    * @param packed
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]
    * @return
    *   an RGBA8888 int color
    */
  def toRGBA8888(packed: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val r       = Math.min(Math.max(((0.999779f * i + 1.0709400f * p + 0.324891f * t) * 256.0).toInt, 0), 255)
    val g       = Math.min(Math.max(((1.000150f * i - 0.3777440f * p + 0.220439f * t) * 256.0).toInt, 0), 255)
    val b       = Math.min(Math.max(((0.999769f * i + 0.0629496f * p - 0.809638f * t) * 256.0).toInt, 0), 255)
    r << 24 | g << 16 | b << 8 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Converts a packed float color in the format produced by [[ipt(float,float,float,float)]] to a packed float in RGBA format. This format of float can be used with the standard SpriteBatch and in
    * some other places.
    * @param packed
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]
    * @return
    *   a packed float color as RGBA
    */
  def toRGBA(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val r       = Math.min(Math.max(((0.999779f * i + 1.0709400f * p + 0.324891f * t) * 256.0).toInt, 0), 255)
    val g       = Math.min(Math.max(((1.000150f * i - 0.3777440f * p + 0.220439f * t) * 256.0).toInt, 0), 255)
    val b       = Math.min(Math.max(((0.999769f * i + 0.0629496f * p - 0.809638f * t) * 256.0).toInt, 0), 255)
    java.lang.Float.intBitsToFloat(r | g << 8 | b << 16 | (decoded & 0xfe000000))
  }

  /** Writes an IPT-format packed float color (the format produced by [[ipt(float,float,float,float)]]) into an RGBA8888 Color (called `editing`).
    * @param editing
    *   a Color that will be filled in-place with an RGBA conversion of `packed`
    * @param packed
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]
    * @return
    *   an RGBA8888 Color
    */
  def toColor(editing: Color, packed: Float): Color = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    editing.r = 0.999779f * i + 1.0709400f * p + 0.324891f * t
    editing.g = 1.000150f * i - 0.3777440f * p + 0.220439f * t
    editing.b = 0.999769f * i + 0.0629496f * p - 0.809638f * t
    // 0x1.020408p-7f = 0.007874016f (this is 1/127 as a float)
    editing.a = (decoded >>> 25) * 0.007874016f
    editing.clamp()
  }

  /** Writes an IPT-format packed float color (the format produced by [[ipt(float,float,float,float)]]) into an IPT-format Color called `editing`. This is mostly useful if the rest of your application
    * expects colors in IPT format, such as because you use a ColorfulBatch.
    *
    * Internally, this simply calls [[Color.abgr8888ToColor(Color,float)]] and returns the edited Color.
    * @param editing
    *   a Color that will be filled in-place with the color `iptColor`, unchanged from its color space
    * @param iptColor
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]
    * @return
    *   the edited Color
    */
  def toIPTColor(editing: Color, iptColor: Float): Color = {
    Color.abgr8888ToColor(editing, iptColor)
    editing
  }

  /** Takes a color encoded as an RGBA8888 int and converts to a packed float in the IPT format this uses.
    * @param rgba
    *   an int with the channels (in order) red, green, blue, alpha; should have 8 bits per channel
    * @return
    *   a packed float as IPT, which this class can use
    */
  def fromRGBA8888(rgba: Int): Float = {
    // 0x1.010101010101p-8f = 0.003921569f
    val r = (rgba >>> 24) * 0.003921569f
    val g = (rgba >>> 16 & 0xff) * 0.003921569f
    val b = (rgba >>> 8 & 0xff) * 0.003921569f
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * r + 0.576951f * g + 0.233221f * b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * r - 0.73741f * g + 0.0681367f * b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * r + 0.655205f * g - 0.941748f * b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | (rgba & 0xfe) << 24
    )
  }

  /** Takes a color encoded as an RGBA8888 packed float and converts to a packed float in the IPT format this uses.
    * @param packed
    *   a packed float in RGBA8888 format, with A in the MSB and R in the LSB
    * @return
    *   a packed float as IPT, which this class can use
    */
  def fromRGBA(packed: Float): Float = {
    val abgr = java.lang.Float.floatToRawIntBits(packed)
    // 0x1.010101010101p-8f = 0.003921569f
    val r = (abgr & 0xff) * 0.003921569f
    val g = (abgr >>> 8 & 0xff) * 0.003921569f
    val b = (abgr >>> 16 & 0xff) * 0.003921569f
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * r + 0.576951f * g + 0.233221f * b) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * r - 0.73741f * g + 0.0681367f * b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * r + 0.655205f * g - 0.941748f * b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | (abgr & 0xfe000000)
    )
  }

  /** Takes a Color that uses RGBA8888 channels and converts to a packed float in the IPT format this uses.
    * @param color
    *   an RGBA8888 Color
    * @return
    *   a packed float as IPT, which this class can use
    */
  def fromColor(color: Color): Float =
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * color.r + 0.576951f * color.g + 0.233221f * color.b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * color.r - 0.73741f * color.g + 0.0681367f * color.b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * color.r + 0.655205f * color.g - 0.941748f * color.b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Takes RGBA components from 0.0 to 1.0 each and converts to a packed float in the IPT format this uses.
    * @param r
    *   red, from 0.0 to 1.0 (both inclusive)
    * @param g
    *   green, from 0.0 to 1.0 (both inclusive)
    * @param b
    *   blue, from 0.0 to 1.0 (both inclusive)
    * @param a
    *   alpha, from 0.0 to 1.0 (both inclusive)
    * @return
    *   a packed float as IPT, which this class can use
    */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float =
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * r + 0.576951f * g + 0.233221f * b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * r - 0.73741f * g + 0.0681367f * b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * r + 0.655205f * g - 0.941748f * b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Gets the red channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   an int from 0 to 255, inclusive, representing the red channel value of the given encoded color
    */
  def redInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    Math.min(Math.max(((0.999779f * i + 1.0709400f * p + 0.324891f * t) * 256.0).toInt, 0), 255)
  }

  /** Gets the green channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   an int from 0 to 255, inclusive, representing the green channel value of the given encoded color
    */
  def greenInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    Math.min(Math.max(((1.000150f * i - 0.3777440f * p + 0.220439f * t) * 256.0).toInt, 0), 255)
  }

  /** Gets the blue channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   an int from 0 to 255, inclusive, representing the blue channel value of the given encoded color
    */
  def blueInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    Math.min(Math.max(((0.999769f * i + 0.0629496f * p - 0.809638f * t) * 256.0).toInt, 0), 255)
  }

  /** Gets the alpha channel value of the given encoded color, as an even int ranging from 0 to 254, inclusive. Because of how alpha is stored, no odd-number values are possible for alpha.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   an even int from 0 to 254, inclusive, representing the alpha channel value of the given encoded color
    */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the red channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the red channel value of the given encoded color
    */
  def red(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    Math.min(Math.max(0.999779f * i + 1.0709400f * p + 0.324891f * t, 0f), 1f)
  }

  /** Gets the green channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the green channel value of the given encoded color
    */
  def green(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    Math.min(Math.max(1.000150f * i - 0.3777440f * p + 0.220439f * t, 0f), 1f)
  }

  /** Gets the blue channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the blue channel value of the given encoded color
    */
  def blue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    Math.min(Math.max(0.999769f * i + 0.0629496f * p - 0.809638f * t, 0f), 1f)
  }

  /** Gets the alpha channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the alpha channel value of the given encoded color
    */
  // 0x1.020408p-8f = 0.003937008f
  def alpha(encoded: Float): Float =
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** Gets a color as an IPT packed float given floats representing hue, saturation, lightness, and opacity. All parameters should normally be between 0 and 1 inclusive, though any hue is tolerated
    * (precision loss may affect the color if the hue is too large). A hue of 0 is red, progressively higher hue values go to orange, yellow, green, blue, and purple before wrapping around to red as
    * it approaches 1. A saturation of 0 is grayscale, a saturation of 1 is brightly colored, and values close to 1 will usually appear more distinct than values close to 0, especially if the hue is
    * different. A lightness of 0.001f or less is always black (also using a shortcut if this is the case, respecting opacity), while a lightness of 1f is white. Very bright colors are mostly in a
    * band of high-saturation where lightness is 0.5f.
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
    *   a float encoding a color with the given properties
    */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Gets the saturation of the given encoded color, as a float ranging from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   the saturation of the color from 0.0 (a grayscale color; inclusive) to 1.0 (a bright color, inclusive)
    */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    if (Math.abs(i - 0.5) > 0.495f) 0f
    else {
      val p  = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
      val t  = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
      val rv = Math.min(Math.max(0.999779f * i + 1.0709400f * p + 0.324891f * t, 0f), 1f)
      val gv = Math.min(Math.max(1.000150f * i - 0.3777440f * p + 0.220439f * t, 0f), 1f)
      val bv = Math.min(Math.max(0.999769f * i + 0.0629496f * p - 0.809638f * t, 0f), 1f)
      var x  = 0f
      var y  = 0f
      var w  = 0f
      if (gv < bv) {
        x = bv
        y = gv
      } else {
        x = gv
        y = bv
      }
      if (rv < x) {
        w = rv
      } else {
        w = x
        x = rv
      }
      x - Math.min(w, y)
    }
  }

  /** Gets the lightness of the given encoded color, as a float ranging from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   the lightness of the color from 0.0 (black; inclusive) to 1.0 (white, inclusive)
    */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val rv      = Math.min(Math.max(0.999779f * i + 1.0709400f * p + 0.324891f * t, 0f), 1f)
    val gv      = Math.min(Math.max(1.000150f * i - 0.3777440f * p + 0.220439f * t, 0f), 1f)
    val bv      = Math.min(Math.max(0.999769f * i + 0.0629496f * p - 0.809638f * t, 0f), 1f)

    var x = 0f
    var y = 0f
    var w = 0f
    if (gv < bv) {
      x = bv
      y = gv
    } else {
      x = gv
      y = bv
    }
    if (rv < x) {
      w = rv
    } else {
      w = x
      x = rv
    }
    val d = x - Math.min(w, y)
    x * (1f - 0.5f * d / (x + 1e-10f))
  }

  /** Gets the hue of the given encoded color, as a float from 0f (inclusive, red and approaching orange if increased) to 1f (exclusive, red and approaching purple if decreased).
    * @param encoded
    *   a color as a packed float that can be obtained by [[ipt(float,float,float,float)]]
    * @return
    *   The hue of the color from 0.0 (red, inclusive) towards orange, then yellow, and eventually to purple before looping back to almost the same red (1.0, exclusive)
    */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val rv      = Math.min(Math.max(0.999779f * i + 1.0709400f * p + 0.324891f * t, 0f), 1f)
    val gv      = Math.min(Math.max(1.000150f * i - 0.3777440f * p + 0.220439f * t, 0f), 1f)
    val bv      = Math.min(Math.max(0.999769f * i + 0.0629496f * p - 0.809638f * t, 0f), 1f)
    var x       = 0f
    var y       = 0f
    var z       = 0f
    var w       = 0f
    if (gv < bv) {
      x = bv
      y = gv
      z = -1f
      w = 2f / 3f
    } else {
      x = gv
      y = bv
      z = 0f
      w = -1f / 3f
    }
    if (rv < x) {
      z = w
      w = rv
    } else {
      w = x
      x = rv
    }
    val d = x - Math.min(w, y)
    Math.abs(z + (w - y) / (6f * d + 1e-10f))
  }

  /** The "intensity" of the given packed float in IPT format, which is like its lightness; ranges from 0.0f to 1.0f. You can edit the intensity of a color with [[lighten(float,float)]] and
    * [[darken(float,float)]].
    *
    * @param encoded
    *   a color encoded as a packed float, as by [[ipt(float,float,float,float)]]
    * @return
    *   the intensity value as a float from 0.0f to 1.0f
    */
  def intensity(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "protan" of the given packed float in IPT format, which when combined with tritan describes the hue and saturation of a color; ranges from 0f to 1f. If protan is 0f, the color will be
    * cooler, more green or blue; if protan is 1f, the color will be warmer, from magenta to orange. You can edit the protan of a color with [[protanUp(float,float)]] and [[protanDown(float,float)]].
    * @param encoded
    *   a color encoded as a packed float, as by [[ipt(float,float,float,float)]]
    * @return
    *   the protan value as a float from 0.0f to 1.0f
    */
  def protan(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "tritan" of the given packed float in IPT format, which when combined with protan describes the hue and saturation of a color; ranges from 0f to 1f. If tritan is 0f, the color will be more
    * "artificial", more blue or purple; if tritan is 1f, the color will be more "natural", from green to yellow to orange. You can edit the tritan of a color with [[tritanUp(float,float)]] and
    * [[tritanDown(float,float)]].
    * @param encoded
    *   a color encoded as a packed float, as by [[ipt(float,float,float,float)]]
    * @return
    *   the tritan value as a float from 0.0f to 1.0f
    */
  def tritan(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

  /** Gets a variation on the packed float color basis as another packed float that has its hue, saturation, lightness, and opacity adjusted by the specified amounts. Note that this edits the color in
    * HSL space, not IPT! Takes floats representing the amounts of change to apply to hue, saturation, lightness, and opacity; these can be between -1f and 1f. Returns a float that can be used as a
    * packed or encoded color. The float is likely to be different than the result of [[ipt(float,float,float,float)]] unless hue, saturation, lightness, and opacity are all 0. This won't allocate any
    * objects.
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
    val e = java.lang.Float.floatToRawIntBits(basis)
    val i = Math.min(Math.max(light + (e & 0xff) / 255f, 0f), 1f)
    // 0x1.020408p-8f = 0.003937008f
    val op = Math.min(Math.max(opacity + (e >>> 24 & 0xfe) * 0.003937008f, 0f), 1f)
    if (i <= 0.001f)
      java.lang.Float.intBitsToFloat(((op * 255f).toInt << 24 & 0xfe000000) | 0x808000)
    else {
      val p  = ((e >>> 7 & 0x1fe) - 0xff) / 255f
      val t  = ((e >>> 15 & 0x1fe) - 0xff) / 255f
      val rv = Math.min(Math.max(0.999779f * i + 1.0709400f * p + 0.324891f * t, 0f), 1f)
      val gv = Math.min(Math.max(1.000150f * i - 0.3777440f * p + 0.220439f * t, 0f), 1f)
      val bv = Math.min(Math.max(0.999769f * i + 0.0629496f * p - 0.809638f * t, 0f), 1f)
      var x  = 0f
      var y  = 0f
      var z  = 0f
      var w  = 0f
      if (gv < bv) {
        x = bv
        y = gv
        z = -1f
        w = 2f / 3f
      } else {
        x = gv
        y = bv
        z = 0f
        w = -1f / 3f
      }
      if (rv < x) {
        z = w
        w = rv
      } else {
        w = x
        x = rv
      }
      val d   = x - Math.min(w, y)
      val lum = x * (1f - 0.5f * d / (x + 1e-10f))
      val h   = Math.abs(z + (w - y) / (6f * d + 1e-10f)) + hue + 1f
      val sat = (x - lum) / (Math.min(lum, 1f - lum) + 1e-10f) + saturation
      fromRGBA(FloatColors.hsl2rgb(h - h.toInt, Math.min(Math.max(sat, 0f), 1f), lum, op))
    }
  }

  /** Interpolates from the packed float color start towards white by change. While change should be between 0f (return start as-is) and 1f (return white), start should be a packed color, as from
    * [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using
    * [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards white. Unlike [[FloatColors.lerpFloatColors(float,float,float)]], this keeps the alpha and both chroma of start as-is.
    * @see
    *   [[darken(float,float)]] the counterpart method that darkens a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward white, as a float between 0 and 1; higher means closer to white
    * @return
    *   a packed float that represents a color between start and white
    */
  def lighten(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val i     = s & 0xff
    val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i + (0xff - i) * change).toInt & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards black by change. While change should be between 0f (return start as-is) and 1f (return black), start should be a packed color, as from
    * [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using
    * [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards black. Unlike [[FloatColors.lerpFloatColors(float,float,float)]], this keeps the alpha and both chroma of start as-is.
    * @see
    *   [[lighten(float,float)]] the counterpart method that lightens a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward black, as a float between 0 and 1; higher means closer to black
    * @return
    *   a packed float that represents a color between start and black
    */
  def darken(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val i     = s & 0xff
    val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i * (1f - change)).toInt & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards a warmer color (orange to magenta) by change. While change should be between 0f (return start as-is) and 1f (return fully warmed), start
    * should be a packed color, as from [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using
    * [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards a warmer color. Unlike [[FloatColors.lerpFloatColors(float,float,float)]], this keeps the alpha and intensity of start as-is.
    * @see
    *   [[protanDown(float,float)]] the counterpart method that cools a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to warm start, as a float between 0 and 1; higher means a warmer result
    * @return
    *   a packed float that represents a color between start and a warmer color
    */
  def protanUp(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val p     = s >>> 8 & 0xff
    val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p + (0xff - p) * change).toInt << 8 & 0xff00) | other)
  }

  /** Interpolates from the packed float color start towards a cooler color (green to blue) by change. While change should be between 0f (return start as-is) and 1f (return fully cooled), start should
    * be a packed color, as from [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using
    * [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards a cooler color. Unlike [[FloatColors.lerpFloatColors(float,float,float)]], this keeps the alpha and intensity of start as-is.
    * @see
    *   [[protanUp(float,float)]] the counterpart method that warms a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to cool start, as a float between 0 and 1; higher means a cooler result
    * @return
    *   a packed float that represents a color between start and a cooler color
    */
  def protanDown(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val p     = s >>> 8 & 0xff
    val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Interpolates from the packed float color start towards a "natural" color (between green and orange) by change. While change should be between 0f (return start as-is) and 1f (return fully
    * natural), start should be a packed color, as from [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than
    * using [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards a more natural color. Unlike [[FloatColors.lerpFloatColors(float,float,float)]], this keeps the alpha and intensity of
    * start as-is.
    * @see
    *   [[tritanDown(float,float)]] the counterpart method that makes a float color less natural
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a natural color, as a float between 0 and 1; higher means a more natural result
    * @return
    *   a packed float that represents a color between start and a more natural color
    */
  def tritanUp(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val t     = s >>> 16 & 0xff
    val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 16 & 0xff0000) | other)
  }

  /** Interpolates from the packed float color start towards an "artificial" color (between blue and purple) by change. While change should be between 0f (return start as-is) and 1f (return fully
    * artificial), start should be a packed color, as from [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than
    * using [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards a more artificial color. Unlike [[FloatColors.lerpFloatColors(float,float,float)]], this keeps the alpha and intensity of
    * start as-is.
    * @see
    *   [[tritanUp(float,float)]] the counterpart method that makes a float color less artificial
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a bolder color, as a float between 0 and 1; higher means a more artificial result
    * @return
    *   a packed float that represents a color between start and a more artificial color
    */
  def tritanDown(start: Float, change: Float): Float = {
    val s     = java.lang.Float.floatToRawIntBits(start)
    val t     = s >>> 16 & 0xff
    val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) << 16 | other)
  }

  /** Interpolates from the packed float color start towards that color made opaque by change. While change should be between 0f (return start as-is) and 1f (return start with full alpha), start
    * should be a packed color, as from [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using
    * [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards transparent. This won't change the intensity, protan, or tritan of the color.
    * @see
    *   [[fade(float,float)]] the counterpart method that makes a float color more translucent
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
    * packed color, as from [[ipt(float,float,float,float)]]. This is a good way to reduce allocations of temporary Colors, and is a little more efficient and clear than using
    * [[FloatColors.lerpFloatColors(float,float,float)]] to lerp towards transparent. This won't change the intensity, protan, or tritan of the color.
    * @see
    *   [[blot(float,float)]] the counterpart method that makes a float color more opaque
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

  /** Brings the chromatic components of `start` closer to grayscale by `change` (desaturating them). While change should be between 0f (return start as-is) and 1f (return fully gray), start should be
    * a packed color, as from [[ipt(float,float,float,float)]]. This only changes protan and tritan; it leaves intensity and alpha alone, unlike [[lessenChange(float,float)]], which usually changes
    * intensity.
    * @see
    *   [[enrich(float,float)]] the counterpart method that makes a float color more saturated
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a desaturated color, as a float between 0 and 1; higher means a less saturated result
    * @return
    *   a packed float that represents a color between start and a desaturated color
    */
  def dullen(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    ipt(
      (s & 0xff) / 255f,
      ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f,
      ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f,
      (s >>> 25) / 127f
    )
  }

  /** Pushes the chromatic components of `start` away from grayscale by change (saturating them). While change should be between 0f (return start as-is) and 1f (return maximally saturated), start
    * should be a packed color, as from [[ipt(float,float,float,float)]]. This usually changes only protan and tritan, but higher values for `change` can force the color out of the gamut, which this
    * corrects using [[limitToGamut(float,float,float,float)]] (and that can change intensity somewhat). If the color stays in-gamut, then intensity won't change; alpha never changes.
    * @see
    *   [[dullen(float,float)]] the counterpart method that makes a float color less saturated
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a saturated color, as a float between 0 and 1; higher means a more saturated result
    * @return
    *   a packed float that represents a color between start and a saturated color
    */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    limitToGamut(
      (s & 0xff) / 255f,
      ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f,
      ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f,
      (s >>> 25) / 127f
    )
  }

  /** Given a packed float IPT color `mainColor` and another IPT color that it should be made to contrast with, gets a packed float IPT color with roughly inverted intensity but the same chromatic
    * channels and opacity (P and T are likely to be clamped if the result gets close to white or black). This won't ever produce black or other very dark colors, and also has a gap in the range it
    * produces for intensity values between 0.5 and 0.55. That allows most of the colors this method produces to contrast well as a foreground when displayed on a background of `contrastingColor`, or
    * vice versa. This will leave the intensity unchanged if the chromatic channels of the contrastingColor and those of the mainColor are already very different. This has nothing to do with the
    * contrast channel of the tweak in ColorfulBatch; where that part of the tweak can make too-similar lightness values further apart by just a little, this makes a modification on `mainColor` to
    * maximize its lightness difference from `contrastingColor` without losing its other qualities.
    * @param mainColor
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]; this is the color that will be adjusted
    * @param contrastingColor
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]; the adjusted mainColor will contrast with this
    * @return
    *   a different IPT packed float color, based on mainColor but with potentially very different lightness
    */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float = {
    val bits         = java.lang.Float.floatToRawIntBits(mainColor)
    val contrastBits = java.lang.Float.floatToRawIntBits(contrastingColor)
    val i            = bits & 0xff
    val p            = bits >>> 8 & 0xff
    val t            = bits >>> 16 & 0xff
    val ci           = contrastBits & 0xff
    val cp           = contrastBits >>> 8 & 0xff
    val ct           = contrastBits >>> 16 & 0xff
    if ((p - cp) * (p - cp) + (t - ct) * (t - ct) >= 0x10000)
      mainColor
    else
      // 0x1.0p-8f = 0.00390625f
      ipt(
        if (ci < 128) i * (0.45f / 255f) + 0.55f else 0.5f - i * (0.45f / 255f),
        p / 255f,
        t / 255f,
        0.00390625f * (bits >>> 24)
      )
  }

  /** Given a packed float IPT color `mainColor` and another IPT color that it should be made to contrast with, gets a packed float IPT color with I that should be quite different from
    * `contrastingColor`'s I, but the same chromatic channels and opacity (P and T are likely to be clamped if the result gets close to white or black). This allows most of the colors this method
    * produces to contrast well as a foreground when displayed on a background of `contrastingColor`, or vice versa.
    *
    * This is similar to [[inverseLightness(float,float)]], but is considerably simpler, and this method will change the lightness of mainColor when the two given colors have close lightness but
    * distant chroma. Because it averages the original I of mainColor with the modified one, this tends to not produce harsh color changes.
    * @param mainColor
    *   a packed IPT float color; this is the color that will be adjusted
    * @param contrastingColor
    *   a packed IPT float color; the adjusted mainColor will contrast with the I of this
    * @return
    *   a different packed IPT float color, based on mainColor but typically with different lightness
    */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(mainColor)
    val contrast = java.lang.Float.floatToRawIntBits(contrastingColor)
    limitToGamut(java.lang.Float.intBitsToFloat((main & 0xfeffff00) | (contrast + 128 & 0xff) + (main & 0xff) >>> 1))
  }

  /** Pretty simple; adds 0.5 to the given color's I and wraps it around if it would go above 1.0, then averages that with the original I. This means light colors become darker, and dark colors become
    * lighter, with almost all results in the middle-range of possible lightness.
    * @param mainColor
    *   a packed IPT float color
    * @return
    *   a different packed IPT float color, with its I channel changed and limited to the correct gamut
    */
  def offsetLightness(mainColor: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(mainColor)
    limitToGamut(java.lang.Float.intBitsToFloat((decoded & 0xfeffff00) | (decoded + 128 & 0xff) + (decoded & 0xff) >>> 1))
  }

  /** Makes the additive IPT color stored in `color` cause less of a change when used as a tint, as if it were mixed with neutral gray. When `fraction` is 1.0, this returns color unchanged; when
    * fraction is 0.0, it returns gray, and when it is in-between 0.0 and 1.0 it returns something between the two. This is meant for things like area of effect abilities that make smaller color
    * changes toward their periphery.
    * @param color
    *   a color that should have its tinting effect potentially weakened
    * @param fraction
    *   how much of `color` should be kept, from 0.0 to 1.0
    * @return
    *   an IPT float color between gray and `color`
    */
  def lessenChange(color: Float, fraction: Float): Float = {
    val e      = java.lang.Float.floatToRawIntBits(color)
    val sI     = 0x80
    val sP     = 0x80
    val sT     = 0x80
    val eI     = e & 0xff
    val eP     = (e >>> 8) & 0xff
    val eT     = (e >>> 16) & 0xff
    val eAlpha = e >>> 24 & 0xfe
    java.lang.Float.intBitsToFloat(
      ((sI + fraction * (eI - sI)).toInt & 0xff)
        | (((sP + fraction * (eP - sP)).toInt & 0xff) << 8)
        | (((sT + fraction * (eT - sT)).toInt & 0xff) << 16)
        | (eAlpha << 24)
    )
  }

  /** Makes a quasi-randomly-edited variant on the given `color`, allowing typically a small amount of `variance` (such as 0.05 to 0.25) between the given color and what this can return. The `seed`
    * should be different each time this is called, and can be obtained from a random number generator to make the colors more random, or can be incremented on each call. If the seed is only
    * incremented or decremented, then this shouldn't produce two similar colors in a row unless variance is very small. The variance affects the I, P, and T of the generated color, and each of those
    * channels can go up or down by the given variance as long as the total distance isn't greater than the variance (this considers P and T extra-wide, going from -1 to 1, while I goes from 0 to 1,
    * but only internally for measuring distance).
    * @param color
    *   a packed float color, as produced by [[ipt(float,float,float,float)]]
    * @param seed
    *   a long seed that should be different on each call; should not be 0
    * @param variance
    *   max amount of difference between the given color and the generated color; always less than 1
    * @return
    *   a generated packed float color that should be at least somewhat different from `color`
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val limit   = variance * variance
    var s       = seed
    boundary[Float] {
      var j = 0
      while (j < 50) {
        // 0x7FFFFFp-1f = 4194303.5f; 0x1p-22f = 2.3841858E-7f
        val x = (((s * 0xd1b54a32d192ed03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        val y = (((s * 0xabc98388fb8fac03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        val z = (((s * 0x8cb92ba72f3d8dd7L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        s += 0x9e3779b97f4a7c15L
        val dist = x * x + y * y + z * z
        val nx   = x + i
        val ny   = (p + y) * 0.5f + 0.5f
        val nz   = (t + z) * 0.5f + 0.5f
        if (dist <= limit && inGamut(nx, ny, nz))
          break(
            java.lang.Float.intBitsToFloat(
              (decoded & 0xfe000000) | ((nz * 255.5f).toInt << 16 & 0xff0000)
                | ((ny * 255.5f).toInt << 8 & 0xff00) | (nx * 255.5f).toInt
            )
          )
        j += 1
      }
      color
    }
  }

  /** Returns true if the given packed float color, as IPT, is valid to convert losslessly back to RGBA.
    * @param packed
    *   a packed float color as IPT
    * @return
    *   true if the given packed float color can be converted back and forth to RGBA
    */
  def inGamut(packed: Float): Boolean = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val r       = 0.999779f * i + 1.0709400f * p + 0.324891f * t
    if (r < -0.006f || r > 1.003f) false
    else {
      val g = 1.000150f * i - 0.3777440f * p + 0.220439f * t
      if (g < -0.006f || g > 1.003f) false
      else {
        val b = 0.999769f * i + 0.0629496f * p - 0.809638f * t
        b >= -0.006f && b <= 1.003f
      }
    }
  }

  /** Returns true if the given IPT values are valid to convert losslessly back to RGBA.
    * @param i
    *   intensity channel, as a float from 0 to 1
    * @param p
    *   protan channel, as a float from 0 to 1
    * @param t
    *   tritan channel, as a float from 0 to 1
    * @return
    *   true if the given packed float color can be converted back and forth to RGBA
    */
  def inGamut(i: Float, p: Float, t: Float): Boolean = {
    val p2 = (p - 0.5f) * 2f
    val t2 = (t - 0.5f) * 2f
    val r  = 0.999779f * i + 1.0709400f * p2 + 0.324891f * t2
    if (r < -0.006f || r > 1.003f) false
    else {
      val g = 1.000150f * i - 0.3777440f * p2 + 0.220439f * t2
      if (g < -0.006f || g > 1.003f) false
      else {
        val b = 0.999769f * i + 0.0629496f * p2 - 0.809638f * t2
        b >= -0.006f && b <= 1.003f
      }
    }
  }

  /** Iteratively checks whether the given IPT color is in-gamut, and either brings the color closer to 50% gray if it isn't in-gamut, or returns it as soon as it is in-gamut.
    * @param packed
    *   a packed float color in IPT format; often this color is not in-gamut
    * @return
    *   the first color this finds that is between the given IPT color and 50% gray, and is in-gamut
    * @see
    *   [[inGamut(float)]] You can use inGamut() if you just want to check whether a color is in-gamut.
    */
  def limitToGamut(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    var p2      = p
    var t2      = t
    boundary[Float] {
      var attempt = 31
      while (attempt >= 0) {
        val r = 0.999779f * i + 1.0709400f * p2 + 0.324891f * t2
        val g = 1.000150f * i - 0.3777440f * p2 + 0.220439f * t2
        val b = 0.999769f * i + 0.0629496f * p2 - 0.809638f * t2
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(ipt(i, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, (decoded >>> 25) / 127f))
        // 0x1p-5f = 0.03125f
        val progress = attempt * 0.03125f
        p2 = MathUtils.lerp(0, p, progress)
        t2 = MathUtils.lerp(0, t, progress)
        attempt -= 1
      }
      ipt(i, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, (decoded >>> 25) / 127f)
    }
  }

  /** Iteratively checks whether the given IPT color is in-gamut, and either brings the color closer to 50% gray if it isn't in-gamut, or returns it as soon as it is in-gamut. This always produces an
    * opaque color.
    * @param i
    *   intensity component; will be clamped between 0 and 1 if it isn't already
    * @param p
    *   protan component; will be clamped between 0 and 1 if it isn't already
    * @param t
    *   tritan component; will be clamped between 0 and 1 if it isn't already
    * @return
    *   the first color this finds that is between the given IPT color and 50% gray, and is in-gamut
    * @see
    *   [[inGamut(float,float,float)]] You can use inGamut() if you just want to check whether a color is in-gamut.
    */
  def limitToGamut(i: Float, p: Float, t: Float): Float =
    limitToGamut(i, p, t, 1f)

  /** Iteratively checks whether the given IPT color is in-gamut, and either brings the color closer to 50% gray if it isn't in-gamut, or returns it as soon as it is in-gamut.
    * @param i
    *   intensity component; will be clamped between 0 and 1 if it isn't already
    * @param p
    *   protan component; will be clamped between 0 and 1 if it isn't already
    * @param t
    *   tritan component; will be clamped between 0 and 1 if it isn't already
    * @param a
    *   alpha component; will be clamped between 0 and 1 if it isn't already
    * @return
    *   the first color this finds that is between the given IPT color and 50% gray, and is in-gamut
    * @see
    *   [[inGamut(float,float,float)]] You can use inGamut() if you just want to check whether a color is in-gamut.
    */
  def limitToGamut(i: Float, p: Float, t: Float, a: Float): Float = {
    val i2    = Math.min(Math.max(i, 0f), 1f)
    val pOrig = Math.min(Math.max((p - 0.5f) * 2f, -1f), 1f)
    val tOrig = Math.min(Math.max((t - 0.5f) * 2f, -1f), 1f)
    val al    = Math.min(Math.max(a, 0f), 1f)
    var p2    = pOrig
    var t2    = tOrig
    boundary[Float] {
      var attempt = 31
      while (attempt >= 0) {
        val r = 0.999779f * i2 + 1.0709400f * p2 + 0.324891f * t2
        val g = 1.000150f * i2 - 0.3777440f * p2 + 0.220439f * t2
        val b = 0.999769f * i2 + 0.0629496f * p2 - 0.809638f * t2
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(ipt(i2, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, al))
        // 0x1p-5f = 0.03125f
        val progress = attempt * 0.03125f
        p2 = MathUtils.lerp(0, pOrig, progress)
        t2 = MathUtils.lerp(0, tOrig, progress)
        attempt -= 1
      }
      ipt(i2, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, al)
    }
  }

  /** Given a packed float IPT color, this edits its intensity, protan, tritan, and alpha channels by adding the corresponding "add" parameter and then clamping. This returns a different float value
    * (of course, the given float can't be edited in-place). You can give a value of 0 for any "add" parameter you want to stay unchanged. This clamps the resulting color to remain in-gamut, so it
    * should be safe to convert it back to RGBA.
    * @param encoded
    *   a packed float IPT color
    * @param addI
    *   how much to add to the intensity channel; typically in the -1 to 1 range
    * @param addP
    *   how much to add to the protan channel; typically in the -2 to 2 range
    * @param addT
    *   how much to add to the tritan channel; typically in the -2 to 2 range
    * @param addAlpha
    *   how much to add to the alpha channel; typically in the -1 to 1 range
    * @return
    *   a packed float IPT color with the requested edits applied to `encoded`
    */
  def editIPT(encoded: Float, addI: Float, addP: Float, addT: Float, addAlpha: Float): Float =
    editIPT(encoded, addI, addP, addT, addAlpha, 1f, 1f, 1f, 1f)

  /** Given a packed float IPT color, this edits its intensity, protan, tritan, and alpha channels by first multiplying each channel by the corresponding "mul" parameter and then adding the
    * corresponding "add" parameter, before clamping. This means the intensity value is multiplied by `mulI`, then has `addI` added, and then is clamped to the normal range for intensity (0 to 1).
    * This returns a different float value (of course, the given float can't be edited in-place). You can give a value of 0 for any "add" parameter you want to stay unchanged, or a value of 1 for any
    * "mul" parameter that shouldn't change. Note that this manipulates protan and tritan in the -1 to 1 range, so if you multiply by a small number like 0.25f, then this will produce a less-saturated
    * color, and if you multiply by a larger number like 4f, then you will get a much more-saturated color. This clamps the resulting color to remain in-gamut, so it should be safe to convert it back
    * to RGBA.
    * @param encoded
    *   a packed float IPT color
    * @param addI
    *   how much to add to the intensity channel; typically in the -1 to 1 range
    * @param addP
    *   how much to add to the protan channel; typically in the -2 to 2 range
    * @param addT
    *   how much to add to the tritan channel; typically in the -2 to 2 range
    * @param addAlpha
    *   how much to add to the alpha channel; typically in the -1 to 1 range
    * @param mulI
    *   how much to multiply the intensity channel by; should be non-negative
    * @param mulP
    *   how much to multiply the protan channel by; usually non-negative (not always)
    * @param mulT
    *   how much to multiply the tritan channel by; usually non-negative (not always)
    * @param mulAlpha
    *   how much to multiply the alpha channel by; should be non-negative
    * @return
    *   a packed float IPT color with the requested edits applied to `encoded`
    */
  def editIPT(
    encoded:  Float,
    addI:     Float,
    addP:     Float,
    addT:     Float,
    addAlpha: Float,
    mulI:     Float,
    mulP:     Float,
    mulT:     Float,
    mulAlpha: Float
  ): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    var al      = (decoded >>> 25) / 127f

    val i2    = Math.min(Math.max(i * mulI + addI, 0f), 1f)
    val pOrig = Math.min(Math.max(p * mulP + addP, -1f), 1f)
    val tOrig = Math.min(Math.max(t * mulT + addT, -1f), 1f)
    al = Math.min(Math.max(al * mulAlpha + addAlpha, 0f), 1f)
    var p2 = pOrig
    var t2 = tOrig
    boundary[Float] {
      var attempt = 31
      while (attempt >= 0) {
        val r = 0.999779f * i2 + 1.0709400f * p2 + 0.324891f * t2
        val g = 1.000150f * i2 - 0.3777440f * p2 + 0.220439f * t2
        val b = 0.999769f * i2 + 0.0629496f * p2 - 0.809638f * t2
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f)
          break(ipt(i2, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, al))
        // 0x1p-5f = 0.03125f
        val progress = attempt * 0.03125f
        p2 = MathUtils.lerp(0, pOrig, progress)
        t2 = MathUtils.lerp(0, tOrig, progress)
        attempt -= 1
      }
      ipt(i2, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, al)
    }
  }

  /** Converts from a packed float in HSI format to a packed float in IPT format.
    * @param packed
    *   a packed float in HSI format
    * @return
    *   a packed float in IPT format
    */
  def fromHSI(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val h       = (decoded & 0xff) / 255f
    val s       = (decoded >>> 8 & 0xff) / 255f
    val i       = (decoded >>> 16 & 0xff) / 255f
    val y       = TrigTools.cosTurns(h) * s
    val z       = TrigTools.sinTurns(h) * s
    val crMid   = 0.3481738f * y + 0.104959644f * z
    val crScale = (i - 0.5f + (java.lang.Float.floatToRawIntBits(crMid) >>> 31)) * 0.16420607f / -crMid
    val mgMid   = 0.122068435f * y + -0.070396f * z
    val mgScale = (i + 0.5f - (java.lang.Float.floatToRawIntBits(mgMid) >>> 31)) * -0.16136102f / -mgMid
    val ybMid   = 0.020876605f * y + -0.26078433f * z
    val ybScale = (i - 0.5f + (java.lang.Float.floatToRawIntBits(ybMid) >>> 31)) * 0.16155326f / -ybMid
    val scale   = Math.max(crScale, Math.max(mgScale, ybScale))
    val d       = 4f * s * scale / (MathUtils.sin(3.14159f * i) + 0.000001f)

    val p = y * d
    val t = z * d
    java.lang.Float.intBitsToFloat(
      (decoded & 0xfe000000) | ((t * 255).toInt << 16 & 0xff0000)
        | ((p * 255).toInt << 8 & 0xff00) | (decoded >>> 16 & 0xff)
    )
  }

  /** Converts from hue, saturation, intensity, and alpha components (each ranging from 0 to 1 inclusive) to a packed float color in IPT format.
    * @param hue
    *   hue, from 0 to 1 inclusive; 0 is red, 0.25 is yellow, 0.75 is blue
    * @param saturation
    *   saturation from 0 (grayscale) to a limit between 0 and 1 depending on intensity (it can be 1 only when intensity is 0.5)
    * @param intensity
    *   intensity, or lightness, from 0 (black) to 1 (white)
    * @param alpha
    *   alpha transparency/opacity, from 0 (fully transparent) to 1 (fully opaque)
    * @return
    *   a packed float in IPT format
    */
  def fromHSI(hue: Float, saturation: Float, intensity: Float, alpha: Float): Float = {
    val y       = TrigTools.cosTurns(hue) * saturation
    val z       = TrigTools.sinTurns(hue) * saturation
    val crMid   = 0.3481738f * y + 0.104959644f * z
    val crScale = (intensity - 0.5f + (java.lang.Float.floatToRawIntBits(crMid) >>> 31)) * 0.16420607f / -crMid
    val mgMid   = 0.122068435f * y + -0.070396f * z
    val mgScale = (intensity + 0.5f - (java.lang.Float.floatToRawIntBits(mgMid) >>> 31)) * -0.16136102f / -mgMid
    val ybMid   = 0.020876605f * y + -0.26078433f * z
    val ybScale = (intensity - 0.5f + (java.lang.Float.floatToRawIntBits(ybMid) >>> 31)) * 0.16155326f / -ybMid
    val scale   = Math.max(crScale, Math.max(mgScale, ybScale))
    val d       = 4f * saturation * scale / (MathUtils.sin(3.14159f * intensity) + 0.000001f)

    val p = y * d
    val t = z * d
    java.lang.Float.intBitsToFloat(
      ((alpha * 255).toInt << 24 & 0xfe000000) | ((t * 255).toInt << 16 & 0xff0000)
        | ((p * 255).toInt << 8 & 0xff00) | ((intensity * 255).toInt & 0xff)
    )
  }

  /** Produces a random packed float color that is always in-gamut and should be uniformly distributed.
    * @param random
    *   a Random object (or preferably a subclass of Random)
    * @return
    *   a packed float color that is always in-gamut
    */
  def randomColor(random: java.util.Random): Float = {
    val ir = 0.1882353f; val pr  = 0.83137256f - 0.5f; val tr = 0.6431373f - 0.5f
    val ig = 0.5764706f; val pg  = 0.12941177f - 0.5f; val tg = 0.827451f - 0.5f
    val ib = 0.23137255f; val pb = 0.53333336f - 0.5f; val tb = 0.02745098f - 0.5f
    val r  = random.nextFloat()
    val g  = random.nextFloat()
    val b  = random.nextFloat()
    java.lang.Float.intBitsToFloat(
      0xfe000000
        | (((tr * r + tg * g + tb * b) * 128f + 128f).toInt << 16 & 0xff0000)
        | (((pr * r + pg * g + pb * b) * 128f + 128f).toInt << 8 & 0xff00)
        | (((ir * r + ig * g + ib * b) * 256f).toInt & 0xff)
    )
  }

  /** Limited-use; like [[randomColor(Random)]] but for cases where you already have three floats (r, g, and b) distributed how you want. This can be somewhat useful if you are using a "subrandom" or
    * "quasi-random" sequence, like the Halton, Sobol, or R3 sequences, to get 3D points and map them to colors. It can also be useful if you want to randomly generate the RGB channels yourself and
    * track the values produced, as you would if you wanted to avoid generating too many colors with high blue, for instance. This approximately maps the r, g, and b parameters to distances on the RGB
    * axes of a rectangular prism, which is stretched and rotated to form the IPT gamut.
    * @param r
    *   red value to use; will be clamped between 0 and 1
    * @param g
    *   green value to use; will be clamped between 0 and 1
    * @param b
    *   blue value to use; will be clamped between 0 and 1
    * @return
    *   a packed float color that is always opaque
    */
  def subrandomColor(r: Float, g: Float, b: Float): Float = {
    val rc = Math.min(Math.max(r, 0f), 0.999f)
    val gc = Math.min(Math.max(g, 0f), 0.999f)
    val bc = Math.min(Math.max(b, 0f), 0.999f)
    val ir = 0.1882353f; val pr  = 0.83137256f - 0.5f; val tr = 0.6431373f - 0.5f
    val ig = 0.5764706f; val pg  = 0.12941177f - 0.5f; val tg = 0.827451f - 0.5f
    val ib = 0.23137255f; val pb = 0.53333336f - 0.5f; val tb = 0.02745098f - 0.5f
    java.lang.Float.intBitsToFloat(
      0xfe000000
        | (((tr * rc + tg * gc + tb * bc) * 127.5f + 127.5f).toInt << 16 & 0xff0000)
        | (((pr * rc + pg * gc + pb * bc) * 127.5f + 127.5f).toInt << 8 & 0xff00)
        | (((ir * rc + ig * gc + ib * bc) * 255f).toInt & 0xff)
    )
  }
}
