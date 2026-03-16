/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Runtime native library loader for JVM.
 * Resolves libraries from java.library.path or classpath JAR resources,
 * extracting to a temp directory if needed.
 */
package sge
package platform

import java.nio.file.{ Files, Path, StandardCopyOption }
import java.util.concurrent.ConcurrentHashMap

/** Loads platform-specific native shared libraries at runtime.
  *
  * Resolution order:
  *   1. `java.library.path` (dev override / existing behavior)
  *   2. Classpath resource at `native/<platform>/<mapped-name>`
  *   3. Throws `UnsatisfiedLinkError` with diagnostic message
  *
  * Extracted libraries go to a per-JVM temp directory that is deleted on exit. Thread-safe: each library is extracted at most once per JVM.
  */
private[sge] object NativeLibLoader {

  // Cache: library name → extracted (or found) path
  private val cache = new ConcurrentHashMap[String, Path]()

  // Lazy-init temp dir (only created if we actually extract something)
  @volatile private var tempDir: Path = scala.compiletime.uninitialized

  private def ensureTempDir(): Path = {
    if (tempDir == null) {
      synchronized {
        if (tempDir == null) {
          tempDir = Files.createTempDirectory("sge-native-")
          tempDir.toFile.deleteOnExit()
        }
      }
    }
    tempDir
  }

  /** Whether we're running on Android (ART/Dalvik).
    *
    * We check the VM name rather than `Class.forName("android.os.Build")` because android.jar classes may be on the classpath even on desktop JVM (merged into sge JAR for cross-compilation).
    */
  private val isAndroid: Boolean = {
    val vmName  = System.getProperty("java.vm.name", "").toLowerCase
    val vendor  = System.getProperty("java.vm.vendor", "").toLowerCase
    val jVendor = System.getProperty("java.vendor", "").toLowerCase
    vmName.contains("dalvik") || vmName.contains("art") ||
    vendor.contains("android") || jVendor.contains("android")
  }

  /** The host platform classifier (e.g. `"macos-aarch64"`, `"linux-x86_64"`, `"android-aarch64"`). */
  private val hostClassifier: String = {
    val osName = System.getProperty("os.name", "").toLowerCase
    val os     =
      if (isAndroid) "android"
      else if (osName.contains("mac")) "macos"
      else if (osName.contains("linux")) "linux"
      else if (osName.contains("win")) "windows"
      else throw new UnsatisfiedLinkError(s"Unsupported OS: $osName")
    val archProp = System.getProperty("os.arch", "")
    val arch     = archProp match {
      case "amd64" | "x86_64"               => "x86_64"
      case "aarch64" | "arm64"              => "aarch64"
      case "armv7l" | "armeabi-v7a" | "arm" => "armv7"
      case other                            => throw new UnsatisfiedLinkError(s"Unsupported arch: $other")
    }
    s"$os-$arch"
  }

  /** Maps a logical library name to the platform-specific file name.
    *
    * Most libraries follow `System.mapLibraryName` conventions, but some (like GLFW on Windows) use non-standard names.
    */
  private def mappedFileName(libName: String): String = {
    val osName = System.getProperty("os.name", "").toLowerCase
    if (osName.contains("win")) {
      libName match {
        case "glfw" => "glfw3.dll"
        case other  => s"$other.dll"
      }
    } else {
      System.mapLibraryName(libName)
    }
  }

  /** Locate and return the filesystem path to the given native library.
    *
    * @param libName
    *   the logical library name (e.g. `"sge_native_ops"`, `"glfw"`, `"EGL"`)
    * @return
    *   the absolute path to the library file
    * @throws UnsatisfiedLinkError
    *   if the library cannot be found
    */
  def load(libName: String): Path = {
    val cached = cache.get(libName)
    if (cached != null) return cached

    val mapped = mappedFileName(libName)
    val result = findOnLibraryPath(mapped).orElse(loadViaSystemOnAndroid(libName, mapped)).orElse(extractFromClasspath(libName, mapped)).getOrElse {
      val libPath = System.getProperty("java.library.path", "")
      throw new UnsatisfiedLinkError(
        s"Cannot find native library '$mapped' (logical name: '$libName').\n" +
          s"  Searched java.library.path: $libPath\n" +
          s"  Searched classpath resource: native/$hostClassifier/$mapped\n" +
          s"  Host platform: $hostClassifier"
      )
    }

    cache.putIfAbsent(libName, result)
    cache.get(libName)
  }

  /** Search `java.library.path` for the mapped library name. */
  private def findOnLibraryPath(mapped: String): Option[Path] = {
    val libPath = System.getProperty("java.library.path", "")
    val paths   = libPath.split(java.io.File.pathSeparator)
    paths.iterator.map(dir => Path.of(dir, mapped)).find(Files.exists(_))
  }

  /** On Android, find native .so files from the APK's lib/<abi>/ directory.
    *
    * Android extracts native libs to a private directory not on `java.library.path`. The app's `BaseDexClassLoader.findLibrary(name)` returns the full filesystem path. We use reflection to call it,
    * falling back to `System.loadLibrary` + `java.library.path` scan.
    */
  private def loadViaSystemOnAndroid(libName: String, mapped: String): Option[Path] = {
    if (!isAndroid) return None
    // Android's BaseDexClassLoader.findLibrary returns the absolute path to the .so file
    try {
      val cl   = getClass.getClassLoader
      val m    = cl.getClass.getMethod("findLibrary", classOf[String])
      val path = m.invoke(cl, libName).asInstanceOf[String]
      if (path != null) return Some(Path.of(path))
    } catch {
      case _: Exception => () // fall through to System.loadLibrary attempt
    }
    // Fallback: load via system and try to find on java.library.path
    try {
      System.loadLibrary(libName)
      findOnLibraryPath(mapped)
    } catch {
      case _: UnsatisfiedLinkError => None
    }
  }

  /** Extract the library from a classpath resource (`native/<platform>/<name>`). */
  private def extractFromClasspath(libName: String, mapped: String): Option[Path] = {
    val resourcePath = s"native/$hostClassifier/$mapped"
    val stream       = getClass.getClassLoader.getResourceAsStream(resourcePath)
    if (stream == null) None
    else {
      try {
        val dir    = ensureTempDir()
        val target = dir.resolve(mapped)
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING)
        target.toFile.deleteOnExit()
        Some(target)
      } finally stream.close()
    }
  }
}
