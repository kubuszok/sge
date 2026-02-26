/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ParticleControllerRenderData.java
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
