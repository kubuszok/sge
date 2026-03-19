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
 */
package sge
package graphics
package g3d
package environment

abstract class BaseLight[T <: BaseLight[T]] {
  val color: Color = Color(0, 0, 0, 1)
}
