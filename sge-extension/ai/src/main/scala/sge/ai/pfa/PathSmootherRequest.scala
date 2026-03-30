/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathSmootherRequest.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages
 *   Idiom: `= _` -> `scala.compiletime.uninitialized`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package pfa

import sge.math.Vector

/** A request for interruptible path smoothing.
  *
  * @tparam N
  *   Type of node
  * @tparam V
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class PathSmootherRequest[N, V <: Vector[V]] {

  var isNew:       Boolean                   = true
  var outputIndex: Int                       = 0
  var inputIndex:  Int                       = 0
  var path:        SmoothableGraphPath[N, V] = scala.compiletime.uninitialized

  def refresh(path: SmoothableGraphPath[N, V]): Unit = {
    this.path = path
    this.isNew = true
  }
}
