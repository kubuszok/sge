/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/GdxNativesLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `GdxNativesLoader` -> `SgeNativesLoader`
 *   Convention: `SharedLibraryLoader` usage replaced with placeholder; native loading handled differently in SGE
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: SgeNativesLoader,disableNativesLoading,load,nativesLoaded
 * Covenant-source-reference: com/badlogic/gdx/utils/GdxNativesLoader.java
 * Covenant-verified: 2026-04-19
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
