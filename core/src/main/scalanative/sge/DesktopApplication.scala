/*
 * Scala Native-specific factory for DesktopApplication.
 * Uses @extern C FFI to load GLFW, ANGLE, and miniaudio shared libraries.
 * The DesktopApplication class itself is in scaladesktop/ (shared JVM + Native).
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Native entry point for desktop applications. Uses @extern C FFI for GLFW, ANGLE, miniaudio. */
object DesktopApplicationFactory {

  /** Creates and runs a desktop application on Scala Native using @extern C FFI.
    *
    * The shared libraries (GLFW, ANGLE, miniaudio) must be available on the system library path. Initializes the
    * windowing/audio/GL subsystems and blocks until the application exits.
    *
    * @param listener
    *   the application logic callbacks
    * @param config
    *   the application configuration (defaults to standard settings)
    */
  def apply(
    listener: ApplicationListener,
    config:   DesktopApplicationConfig = DesktopApplicationConfig()
  ): DesktopApplication = {
    val windowing = sge.platform.WindowingOpsNative
    val audioOps  = sge.platform.AudioOpsNative
    new DesktopApplication(listener, config, windowing, audioOps, () => new sge.graphics.AngleGL32Native())
  }
}
