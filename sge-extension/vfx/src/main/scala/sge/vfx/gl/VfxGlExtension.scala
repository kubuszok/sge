/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package gl

/** Extra [platform specific] OpenGL functionality required for the library. (methods not yet implemented/supported by the official LibGDX backends).
  */
trait VfxGlExtension {
  def boundFboHandle: Int
}
