/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/RectangleSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package values

import sge.math.{ MathUtils, Vector3 }

/** Encapsulate the formulas to spawn a particle on a rectangle shape.
  * @author
  *   Inferno
  */
final class RectangleSpawnShapeValue extends PrimitiveSpawnShapeValue {

  def this(value: RectangleSpawnShapeValue) = {
    this()
    load(value)
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit = {
    val width  = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
    val height = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
    val depth  = spawnDepth + (spawnDepthDiff * spawnDepthValue.getScale(percent))
    // Where generate the point, on edges or inside ?
    if (edges) {
      val a  = MathUtils.random(-1, 1)
      var tx = 0f
      var ty = 0f
      var tz = 0f
      if (a == -1) {
        tx = if (MathUtils.random(1) == 0) -width / 2 else width / 2
        if (tx == 0) {
          ty = if (MathUtils.random(1) == 0) -height / 2 else height / 2
          tz = if (MathUtils.random(1) == 0) -depth / 2 else depth / 2
        } else {
          ty = MathUtils.random(height) - height / 2
          tz = MathUtils.random(depth) - depth / 2
        }
      } else if (a == 0) {
        // Z
        tz = if (MathUtils.random(1) == 0) -depth / 2 else depth / 2
        if (tz == 0) {
          ty = if (MathUtils.random(1) == 0) -height / 2 else height / 2
          tx = if (MathUtils.random(1) == 0) -width / 2 else width / 2
        } else {
          ty = MathUtils.random(height) - height / 2
          tx = MathUtils.random(width) - width / 2
        }
      } else {
        // Y
        ty = if (MathUtils.random(1) == 0) -height / 2 else height / 2
        if (ty == 0) {
          tx = if (MathUtils.random(1) == 0) -width / 2 else width / 2
          tz = if (MathUtils.random(1) == 0) -depth / 2 else depth / 2
        } else {
          tx = MathUtils.random(width) - width / 2
          tz = MathUtils.random(depth) - depth / 2
        }
      }
      vector.x = tx
      vector.y = ty
      vector.z = tz
    } else {
      vector.x = MathUtils.random(width) - width / 2
      vector.y = MathUtils.random(height) - height / 2
      vector.z = MathUtils.random(depth) - depth / 2
    }
  }

  override def copy(): SpawnShapeValue =
    new RectangleSpawnShapeValue(this)
}
