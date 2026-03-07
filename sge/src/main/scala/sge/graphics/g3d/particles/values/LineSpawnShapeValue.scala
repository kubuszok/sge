/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/LineSpawnShapeValue.java
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

import sge.math.{ MathUtils, Vector3 }

/** Encapsulate the formulas to spawn a particle on a line shape.
  * @author
  *   Inferno
  */
final class LineSpawnShapeValue extends PrimitiveSpawnShapeValue {

  def this(value: LineSpawnShapeValue) = {
    this()
    load(value)
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit = {
    val width  = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
    val height = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
    val depth  = spawnDepth + (spawnDepthDiff * spawnDepthValue.getScale(percent))

    val a = MathUtils.random()
    vector.x = a * width
    vector.y = a * height
    vector.z = a * depth
  }

  override def copy(): SpawnShapeValue =
    LineSpawnShapeValue(this)
}
