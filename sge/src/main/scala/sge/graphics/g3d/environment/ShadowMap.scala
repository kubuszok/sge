/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/ShadowMap.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - Java interface → Scala trait
 *   - Raw TextureDescriptor → TextureDescriptor[?] (existential wildcard)
 *   - Fixes (2026-03-04): getProjViewTrans()/getDepthMap() → property accessors
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: ShadowMap,depthMap,projViewTrans
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/environment/ShadowMap.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package environment

import sge.graphics.g3d.utils.TextureDescriptor
import sge.math.Matrix4

trait ShadowMap {
  def projViewTrans: Matrix4

  def depthMap: TextureDescriptor[?]
}
