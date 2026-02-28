/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/GdxNativesLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

object SgeNativesLoader {
  var disableNativesLoading = false

  private var nativesLoaded = false

  def load(): Unit = synchronized {
    if (!nativesLoaded && !disableNativesLoading) {
      // Blocked: needs platform-specific native library loading strategy
      nativesLoaded = true
    }
  }
}
