/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Window.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Window -> DesktopWindow
 *   Convention: stub for Phase 1 — full implementation requires windowing FFI
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Represents a desktop window managed by the windowing system (SDL3/GLFW).
  *
  * This is a forward-declaration stub. The full implementation will be added when the windowing FFI layer is ported in Phase 1.
  *
  * @author
  *   badlogic (original implementation)
  */
class DesktopWindow private[sge] () {
  // Phase 1: will contain window handle, listener, config, focus/iconified state, etc.
}
