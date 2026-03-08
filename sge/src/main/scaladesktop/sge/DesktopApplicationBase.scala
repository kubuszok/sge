/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3ApplicationBase.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3ApplicationBase -> DesktopApplicationBase
 *   Convention: Java interface -> Scala trait; factory pattern for audio/input
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Base trait for desktop [[Application]] implementations, providing factory methods for creating the audio and input subsystems.
  *
  * Both JVM (Panama) and Native (@extern) desktop applications implement this trait.
  *
  * @author
  *   See AUTHORS file (original implementation)
  */
trait DesktopApplicationBase extends Application {

  /** Creates the audio subsystem for the given configuration.
    *
    * @param config
    *   the application configuration
    * @return
    *   the desktop audio implementation
    */
  def createAudio(config: DesktopApplicationConfig): DesktopAudio

  /** Creates the input subsystem for the given window.
    *
    * @param window
    *   the window to receive input from
    * @return
    *   the desktop input implementation
    */
  def createInput(window: DesktopWindow): DesktopInput
}
