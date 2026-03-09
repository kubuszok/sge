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
    * @param listener
    *   the application logic callbacks
    * @param config
    *   the application configuration (defaults to standard settings)
    */
  def apply(
    listener: ApplicationListener,
    config:   DesktopApplicationConfig = DesktopApplicationConfig()
  ): DesktopApplication = {
    val windowing = sge.platform.WindowingOpsJvm()
    val audioOps  = sge.platform.AudioOpsJvm()
    val glOps     = sge.platform.GlOpsJvm()
    val glLookup  = loadLibrary("GLESv2")
    new DesktopApplication(
      listener,
      config,
      windowing,
      audioOps,
      glOps,
      () => sge.graphics.AngleGL32(glLookup),
      (rate, mono) => new sge.audio.DesktopAudioRecorder(rate, mono)
    )
  }

  private def loadLibrary(libName: String): java.lang.foreign.SymbolLookup = {
    val mappedName = System.mapLibraryName(libName)
    val libPath    = System.getProperty("java.library.path", "")
    val paths      = libPath.split(java.io.File.pathSeparator)
    val found      = paths.iterator
      .map(p => java.nio.file.Path.of(p, mappedName))
      .find(java.nio.file.Files.exists(_))
      .getOrElse(
        throw new UnsatisfiedLinkError(s"Cannot find $mappedName in java.library.path: $libPath")
      )
    java.lang.foreign.SymbolLookup.libraryLookup(found, java.lang.foreign.Arena.global())
  }
}
