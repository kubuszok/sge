/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ParticleControllerRenderData.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Abstract class with 2 public fields faithfully ported
 * - Fields use scala.compiletime.uninitialized (Java null default)
 * - Audited 2026-03-03: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: ParticleControllerRenderData,controller,positionChannel
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/renderers/ParticleControllerRenderData.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package renderers

import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleController

/** Render data used by particle controller renderer
  * @author
  *   Inferno
  */
abstract class ParticleControllerRenderData {
  var controller:      ParticleController = scala.compiletime.uninitialized
  var positionChannel: FloatChannel       = scala.compiletime.uninitialized
}
