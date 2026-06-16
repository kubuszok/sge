/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Scala Native OS-detection seam for UIUtils (shared UIUtils delegates here).
 * - Mirrors the JVM implementation exactly: System.getProperty("os.name")
 *   .toLowerCase with the tokens mac / windows / android / (linux && !android)
 *   / ios. Scala Native exposes os.name on desktop platforms.
 */
package sge
package scenes
package scene2d
package utils

private[utils] object UIUtilsPlatform {

  val isAndroid: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("android")
  }
  val isMac: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("mac")
  }
  val isWindows: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("windows")
  }
  val isLinux: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("linux") && !os.contains("android")
  }
  val isIos: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("ios")
  }
}
