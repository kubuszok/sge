/*
 * SGE Demo — shared utilities for demo scenes.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** Shared helpers for demo scenes. */
object DemoUtils {

  /** Simple HSV to RGB conversion.
    * @param h
    *   hue in [0, 1]
    * @param s
    *   saturation in [0, 1]
    * @param v
    *   value/brightness in [0, 1]
    * @return
    *   (r, g, b) tuple, each in [0, 1]
    */
  def hsvToRgb(h: Float, s: Float, v: Float): (Float, Float, Float) = {
    val i = (h * 6f).toInt
    val f = h * 6f - i
    val p = v * (1f - s)
    val q = v * (1f - f * s)
    val t = v * (1f - (1f - f) * s)
    (i % 6) match {
      case 0 => (v, t, p)
      case 1 => (q, v, p)
      case 2 => (p, v, t)
      case 3 => (p, q, v)
      case 4 => (t, p, v)
      case _ => (v, p, q)
    }
  }
}
