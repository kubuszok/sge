/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ModelInstanceControllerRenderData.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
