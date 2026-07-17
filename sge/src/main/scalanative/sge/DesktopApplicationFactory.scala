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
    * The shared libraries (GLFW, ANGLE, miniaudio) must be available on the system library path. Initializes the windowing/audio/GL subsystems and blocks until the application exits.
    *
    * @param listenerFactory
    *   a context function that creates the application listener when given an [[Sge]] context
    * @param config
    *   the application configuration (defaults to standard settings)
    */
  def apply(
    listenerFactory: Sge ?=> ApplicationListener,
    config:          DesktopApplicationConfig = DesktopApplicationConfig()
  ): DesktopApplication = {
    val windowing = sge.platform.WindowingOpsNative
    val audioOps  = sge.platform.AudioOpsNative
    val glOps     = sge.platform.GlOpsNative
    // No recorderFactory is wired here (unlike the JVM factory, which passes a DesktopAudioRecorder):
    // the shipped miniaudio provider FFI surface (AudioOpsNative) exposes no capture/recording
    // symbols, and Scala Native links @extern symbols eagerly, so one cannot be bound without the
    // native library defining it (ISS-785). The DesktopApplication default recorderFactory therefore
    // signals the missing capability via SgeError.Unsupported — parity with the JVM in that a recorder
    // request produces an SgeError rather than a raw JDK runtime exception (ISS-771).
    new DesktopApplication(listenerFactory, config, windowing, audioOps, glOps, () => new sge.graphics.AngleGL32Native())
  }
}
