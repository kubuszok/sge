/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ModelInstanceControllerRenderData.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All 4 public fields faithfully ported as vars (1 ObjectChannel + 3 FloatChannel)
 * - Fields use scala.compiletime.uninitialized (Java null default)
 * - Audited 2026-03-03: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: ModelInstanceControllerRenderData,colorChannel,modelInstanceChannel,rotationChannel,scaleChannel
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/renderers/ModelInstanceControllerRenderData.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package renderers

import sge.graphics.g3d.particles.ParallelArray.{ FloatChannel, ObjectChannel }

/** Render data used by model instance particle batches
  * @author
  *   Inferno
  */
class ModelInstanceControllerRenderData extends ParticleControllerRenderData {
  var modelInstanceChannel: ObjectChannel[sge.graphics.g3d.ModelInstance] = scala.compiletime.uninitialized
  var colorChannel:         FloatChannel                                  = scala.compiletime.uninitialized
  var scaleChannel:         FloatChannel                                  = scala.compiletime.uninitialized
  var rotationChannel:      FloatChannel                                  = scala.compiletime.uninitialized
}
