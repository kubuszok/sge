/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/PointSpriteControllerRenderData.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All 4 public fields faithfully ported as vars
 * - Fields use scala.compiletime.uninitialized (Java null default)
 * - Audited 2026-03-03: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: PointSpriteControllerRenderData,colorChannel,regionChannel,rotationChannel,scaleChannel
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/renderers/PointSpriteControllerRenderData.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package renderers

import sge.graphics.g3d.particles.ParallelArray.FloatChannel

/** Render data used by point sprites batches
  * @author
  *   Inferno
  */
class PointSpriteControllerRenderData extends ParticleControllerRenderData {
  var regionChannel:   FloatChannel = scala.compiletime.uninitialized
  var colorChannel:    FloatChannel = scala.compiletime.uninitialized
  var scaleChannel:    FloatChannel = scala.compiletime.uninitialized
  var rotationChannel: FloatChannel = scala.compiletime.uninitialized
}
