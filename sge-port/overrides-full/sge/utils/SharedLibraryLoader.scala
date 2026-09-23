package sge.utils

import java.nio.file.Files

/** INJECTED (Substitutions.inject): a self-contained stand-in for libGDX's `SharedLibraryLoader`,
  * which sge removed in favour of a dedicated extraction library the ported corpus still
  * references (`UIUtils.os`, `GdxNativesLoader.load`). Swap point: route [[load]] to that library
  * and add it as a dependency instead — the substitution seam, nothing else in the port changes. */
object SharedLibraryLoader {
  /** The platform the JVM is running on, derived from `os.name`. */
  val os: Os = {
    val n = System.getProperty("os.name", "").toLowerCase
    if (n.contains("windows")) { Os.Windows }
    else if (n.contains("mac") || n.contains("darwin")) { Os.MacOsX }
    else if (n.contains("linux") || n.contains("nix") || n.contains("nux")) { Os.Linux }
    else if (n.contains("ios")) { Os.IOS }
    else { Os.Linux }
  }

  val isWindows: Boolean = os == Os.Windows
  val isLinux: Boolean   = os == Os.Linux
  val isMac: Boolean     = os == Os.MacOsX
  val isAndroid: Boolean = os == Os.Android
  val isIos: Boolean     = os == Os.IOS
  val is64Bit: Boolean   = System.getProperty("os.arch", "").contains("64")
}

final class SharedLibraryLoader {
  /** Extract the platform-mapped native artifact for `sharedLibName` from the
    * classpath resources to a temp file and load it; fall back to the JVM library
    * path when it is not bundled as a resource. */
  def load(sharedLibName: String): Unit = {
    val mapped = System.mapLibraryName(sharedLibName)
    Option(getClass.getResourceAsStream("/" + mapped)) match {
      case Some(in) =>
        val tmp = Files.createTempFile(sharedLibName, mapped)
        tmp.toFile.deleteOnExit()
        try { Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
        finally { in.close() }
        System.load(tmp.toAbsolutePath.toString)
      case None =>
        System.loadLibrary(sharedLibName)
    }
  }
}
