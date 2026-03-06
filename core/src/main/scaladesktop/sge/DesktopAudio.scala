/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/Lwjgl3Audio.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Audio -> DesktopAudio
 *   Convention: Java interface -> Scala trait; extends AutoCloseable for lifecycle management
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Desktop-specific extension of [[Audio]] that adds lifecycle management.
  *
  * Backend audio implementations (e.g. miniaudio) implement this trait. The desktop application calls [[update]] once per frame to advance audio state, and [[close]] on shutdown.
  *
  * @author
  *   See AUTHORS file (original implementation)
  */
trait DesktopAudio extends Audio with AutoCloseable {

  /** Called once per frame by the application loop to update audio state (e.g. streaming buffers, fading). */
  def update(): Unit
}
