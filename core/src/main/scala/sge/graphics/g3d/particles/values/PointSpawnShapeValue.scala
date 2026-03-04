/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/PointSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - All public methods ported: spawnAux, copy
 * - Faithful conversion, no issues
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package values

import sge.math.Vector3

/** Encapsulate the formulas to spawn a particle on a point shape.
  * @author
  *   Inferno
  */
final class PointSpawnShapeValue extends PrimitiveSpawnShapeValue {

  def this(value: PointSpawnShapeValue) = {
    this()
    load(value)
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit = {
    vector.x = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
    vector.y = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
    vector.z = spawnDepth + (spawnDepthDiff * spawnDepthValue.getScale(percent))
  }

  override def copy(): SpawnShapeValue =
    new PointSpawnShapeValue(this)
}
