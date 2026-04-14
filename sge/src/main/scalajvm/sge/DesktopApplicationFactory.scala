/*
 * JVM-specific factory for DesktopApplication.
 * Uses Panama FFM to load GLFW, ANGLE, and miniaudio shared libraries.
 * The DesktopApplication class itself is in scaladesktop/ (shared JVM + Native).
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** JVM entry point for desktop applications. Loads shared libraries via Panama FFM. */
object DesktopApplicationFactory {

  /** Creates and runs a desktop application on JVM using Panama FFM.
    *
    * Loads GLFW, ANGLE, and miniaudio shared libraries from `java.library.path`, initializes the windowing/audio/GL subsystems, and blocks until the application exits.
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
    val windowing = sge.platform.WindowingOpsJvm()
    val audioOps  = sge.platform.AudioOpsJvm()
    val glOps     = sge.platform.GlOpsJvm()
    val glLookup  = {
      val found = multiarch.core.NativeLibLoader.load("GLESv2")
      java.lang.foreign.SymbolLookup.libraryLookup(found, java.lang.foreign.Arena.global())
    }
    new DesktopApplication(
      listenerFactory,
      config,
      windowing,
      audioOps,
      glOps,
      () => sge.graphics.AngleGL32(glLookup),
      (rate, mono) => new sge.audio.DesktopAudioRecorder(rate, mono)
    )
  }
}
