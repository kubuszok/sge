/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Graphics.java (inner class Lwjgl3Monitor)
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Graphics.Lwjgl3Monitor -> DesktopMonitor
 *   Convention: extracted from inner class to standalone; wraps core Monitor with native handle
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** A desktop-specific monitor descriptor that carries the native windowing system handle alongside the core [[Graphics.Monitor]] data.
  *
  * @param monitorHandle
  *   native handle (GLFW monitor pointer on JVM/Native)
  * @param virtualX
  *   the x position in virtual screen coordinates
  * @param virtualY
  *   the y position in virtual screen coordinates
  * @param name
  *   human-readable monitor name
  * @author
  *   badlogic (original implementation)
  */
final case class DesktopMonitor(
  monitorHandle: Long,
  virtualX:      Int,
  virtualY:      Int,
  name:          String
) {

  /** Converts to the core [[Graphics.Monitor]] representation (without native handle). */
  def toMonitor: Graphics.Monitor = Graphics.Monitor(virtualX, virtualY, name)
}
