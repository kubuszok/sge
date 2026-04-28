/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: com/crashinvaders/vfx/gl/VfxGlExtension.java
 * Covenant: partial-port
 * Covenant-verified: 2026-04-08
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Per-platform extension methods not yet implemented (matches upstream gdx-vfx state:
 *     "methods not yet implemented/supported by the official LibGDX backends").
 *
 * upstream-commit: ece6757aa75974d6396325d9b8e0d0b8c8b5c28e
 */
package sge
package vfx
package gl

/** Extra [platform specific] OpenGL functionality required for the library. (methods not yet implemented/supported by the official LibGDX backends).
  */
trait VfxGlExtension {
  def boundFboHandle: Int
}
