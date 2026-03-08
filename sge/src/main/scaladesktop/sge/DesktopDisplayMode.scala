/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Graphics.java (inner class Lwjgl3DisplayMode)
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Graphics.Lwjgl3DisplayMode -> DesktopDisplayMode
 *   Convention: extracted from inner class to standalone; wraps core DisplayMode with monitor handle
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** A desktop-specific display mode that carries the native monitor handle alongside the core [[Graphics.DisplayMode]] data. Used when setting fullscreen mode on a specific monitor.
  *
  * @param monitorHandle
  *   native handle of the monitor this display mode belongs to
  * @param width
  *   the width in physical pixels
  * @param height
  *   the height in physical pixels
  * @param refreshRate
  *   the refresh rate in Hertz
  * @param bitsPerPixel
  *   the number of bits per pixel, may exclude alpha
  * @author
  *   badlogic (original implementation)
  */
final case class DesktopDisplayMode(
  monitorHandle: Long,
  width:         Int,
  height:        Int,
  refreshRate:   Int,
  bitsPerPixel:  Int
) {

  /** Converts to the core [[Graphics.DisplayMode]] representation (without native handle). */
  def toDisplayMode: Graphics.DisplayMode = Graphics.DisplayMode(width, height, refreshRate, bitsPerPixel)
}
