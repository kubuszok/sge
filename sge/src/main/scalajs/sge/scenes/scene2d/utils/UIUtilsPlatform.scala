/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/src/com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Scala.js (browser) OS-detection seam for UIUtils (shared UIUtils delegates
 *   here). Super-sources the GWT UIUtils emulation: flags are derived from
 *   window.navigator.platform (Navigator.getPlatform()) with the EXACT GWT
 *   tokens (capitalized, NOT lowercased), because os.name is empty on Scala.js.
 *     isAndroid = platform.contains("Android")
 *     isMac     = platform.contains("Mac")
 *     isWindows = platform.contains("Win")
 *     isLinux   = platform.contains("Linux") || platform.contains("FreeBSD")
 *     isIos     = platform.contains("iPhone") || ("iPod") || ("iPad")
 * - The GWT token logic is also exposed as pure `*For(platform: String)`
 *   predicates so tests can exercise OS detection with literal platform
 *   strings — order-independent and without depending on the cache-once vals
 *   (which initialize once from the live navigator and cannot be re-stubbed in
 *   a shared jsdom realm).
 */
package sge
package scenes
package scene2d
package utils

import org.scalajs.dom.window

private[utils] object UIUtilsPlatform {

  // GWT UIUtils.java token semantics (window.navigator.platform), pure & order-independent.
  private[utils] def isAndroidFor(platform: String): Boolean = platform.contains("Android")
  private[utils] def isMacFor(platform:     String): Boolean = platform.contains("Mac")
  private[utils] def isWindowsFor(platform: String): Boolean = platform.contains("Win")
  private[utils] def isLinuxFor(platform:   String): Boolean = platform.contains("Linux") || platform.contains("FreeBSD")
  private[utils] def isIosFor(platform: String):     Boolean =
    platform.contains("iPhone") || platform.contains("iPod") || platform.contains("iPad")

  private val platform: String = window.navigator.platform

  val isAndroid: Boolean = isAndroidFor(platform)
  val isMac:     Boolean = isMacFor(platform)
  val isWindows: Boolean = isWindowsFor(platform)
  val isLinux:   Boolean = isLinuxFor(platform)
  val isIos:     Boolean = isIosFor(platform)
}
