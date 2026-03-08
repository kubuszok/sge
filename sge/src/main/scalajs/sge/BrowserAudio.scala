/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtAudio.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtAudio -> BrowserAudio
 *   Convention: Java interface -> Scala trait; adds AutoCloseable for lifecycle consistency
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Browser-specific audio trait. Extends [[Audio]] with [[AutoCloseable]] for resource cleanup when the application shuts down.
  *
  * The browser implementation uses the Web Audio API for sound effects and music playback.
  */
trait BrowserAudio extends Audio with AutoCloseable
