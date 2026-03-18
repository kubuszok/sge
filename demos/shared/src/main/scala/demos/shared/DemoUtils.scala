/*
 * SGE Demos — shared utility helpers for demo scenes.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.shared

import sge.graphics.{Color, Pixmap}

object DemoUtils {

  /** Convert HSV to an SGE Color.
    *
    * @param h
    *   hue in degrees (0–360)
    * @param s
    *   saturation (0–1)
    * @param v
    *   value/brightness (0–1)
    */
  def hsvToColor(h: Float, s: Float, v: Float): Color = {
    val c = v * s
    val x = c * (1f - scala.math.abs((h / 60f) % 2f - 1f).toFloat)
    val m = v - c
    val (r, g, b) =
      if (h < 60f) (c, x, 0f)
      else if (h < 120f) (x, c, 0f)
      else if (h < 180f) (0f, c, x)
      else if (h < 240f) (0f, x, c)
      else if (h < 300f) (x, 0f, c)
      else (c, 0f, x)
    Color(r + m, g + m, b + m, 1f)
  }

  /** Create a solid-color [[sge.graphics.Pixmap]] of the given size. */
  def solidPixmap(width: Int, height: Int, color: Color): Pixmap = {
    val pm = Pixmap(width, height, Pixmap.Format.RGBA8888)
    pm.setColor(color)
    pm.fill()
    pm
  }
}
