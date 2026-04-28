/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 170
 * Covenant-baseline-methods: ColorUtils,HSVtoRGB,RGBtoHSV,b,c,delta,f,g,h,hh,i,max,min,p,q,r,s,ss,t,v,vv
 * Covenant-source-reference: com/kotcrab/vis/ui/util/ColorUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

import sge.graphics.Color
import sge.math.MathUtils

/** Utilities for converting between RGB and HSV color systems.
  * @author
  *   Kotcrab
  * @since 0.6.0
  */
object ColorUtils {

  /** Converts HSV to RGB
    * @param h
    *   hue 0-360
    * @param s
    *   saturation 0-100
    * @param v
    *   value 0-100
    * @param alpha
    *   0-1
    * @return
    *   RGB values in libGDX [[Color]] class
    */
  def HSVtoRGB(h: Float, s: Float, v: Float, alpha: Float): Color = {
    val c = HSVtoRGB(h, s, v)
    c.a = alpha
    c
  }

  /** Converts HSV color system to RGB
    * @param h
    *   hue 0-360
    * @param s
    *   saturation 0-100
    * @param v
    *   value 0-100
    * @return
    *   RGB values in libGDX [[Color]] class
    */
  def HSVtoRGB(h: Float, s: Float, v: Float): Color = {
    val c = new Color(1, 1, 1, 1)
    HSVtoRGB(h, s, v, c)
    c
  }

  /** Converts HSV color system to RGB
    * @param h
    *   hue 0-360
    * @param s
    *   saturation 0-100
    * @param v
    *   value 0-100
    * @param targetColor
    *   color that result will be stored in
    * @return
    *   targetColor
    */
  def HSVtoRGB(h: Float, s: Float, v: Float, targetColor: Color): Color = {
    var hh = if (h == 360) 359f else h
    hh = scala.math.max(0.0f, scala.math.min(360.0f, hh))
    var ss = scala.math.max(0.0f, scala.math.min(100.0f, s))
    var vv = scala.math.max(0.0f, scala.math.min(100.0f, v))
    ss /= 100f
    vv /= 100f
    hh /= 60f
    val i = MathUtils.floor(hh)
    val f = hh - i
    val p = vv * (1 - ss)
    val q = vv * (1 - ss * f)
    val t = vv * (1 - ss * (1 - f))

    var r = 0
    var g = 0
    var b = 0
    i match {
      case 0 =>
        r = MathUtils.round(255 * vv)
        g = MathUtils.round(255 * t)
        b = MathUtils.round(255 * p)
      case 1 =>
        r = MathUtils.round(255 * q)
        g = MathUtils.round(255 * vv)
        b = MathUtils.round(255 * p)
      case 2 =>
        r = MathUtils.round(255 * p)
        g = MathUtils.round(255 * vv)
        b = MathUtils.round(255 * t)
      case 3 =>
        r = MathUtils.round(255 * p)
        g = MathUtils.round(255 * q)
        b = MathUtils.round(255 * vv)
      case 4 =>
        r = MathUtils.round(255 * t)
        g = MathUtils.round(255 * p)
        b = MathUtils.round(255 * vv)
      case _ =>
        r = MathUtils.round(255 * vv)
        g = MathUtils.round(255 * p)
        b = MathUtils.round(255 * q)
    }

    targetColor.set(r / 255.0f, g / 255.0f, b / 255.0f, targetColor.a)
    targetColor
  }

  /** Converts [[Color]] to HSV color system
    * @return
    *   3 element int array with hue (0-360), saturation (0-100) and value (0-100)
    */
  def RGBtoHSV(c: Color): Array[Int] = RGBtoHSV(c.r, c.g, c.b)

  /** Converts RGB to HSV color system
    * @param r
    *   red 0-1
    * @param g
    *   green 0-1
    * @param b
    *   blue 0-1
    * @return
    *   3 element int array with hue (0-360), saturation (0-100) and value (0-100)
    */
  def RGBtoHSV(r: Float, g: Float, b: Float): Array[Int] = {
    var h: Float = 0f
    var s: Float = 0f

    val min = scala.math.min(scala.math.min(r, g), b)
    val max = scala.math.max(scala.math.max(r, g), b)
    val v   = max

    val delta = max - min

    if (max == 0) {
      s = 0
      h = 0
      Array(MathUtils.round(h), MathUtils.round(s), MathUtils.round(v))
    } else {
      s = delta / max

      if (delta == 0) {
        h = 0
      } else {
        if (r == max) {
          h = (g - b) / delta
        } else if (g == max) {
          h = 2 + (b - r) / delta
        } else {
          h = 4 + (r - g) / delta
        }
      }

      h *= 60
      if (h < 0) h += 360

      s *= 100
      val vScaled = v * 100

      Array(MathUtils.round(h), MathUtils.round(s), MathUtils.round(vScaled))
    }
  }
}
