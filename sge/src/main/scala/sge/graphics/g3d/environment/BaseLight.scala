/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/BaseLight.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All methods ported: setColor(r,g,b,a), setColor(Color)
 *   - Java final field → Scala val
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 21
 * Covenant-baseline-methods: BaseLight,color
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/environment/BaseLight.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package environment

abstract class BaseLight[T <: BaseLight[T]] {
  val color: Color = Color(0, 0, 0, 1)
}
