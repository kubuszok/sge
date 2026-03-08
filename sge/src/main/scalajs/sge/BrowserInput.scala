/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtInput.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtInput -> BrowserInput
 *   Convention: Java interface -> Scala trait; adds reset() lifecycle method
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Browser-specific extension of [[Input]] with a lifecycle method for resetting input state.
  *
  * The browser application calls [[reset]] on the main loop after rendering to clear per-frame input state (e.g. justTouched, justPressed).
  */
trait BrowserInput extends Input {

  /** Resets all per-frame input events (called on main loop after rendering). */
  def reset(): Unit
}
