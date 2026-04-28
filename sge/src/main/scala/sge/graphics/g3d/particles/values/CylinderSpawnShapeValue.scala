/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/CylinderSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - All public methods ported: spawnAux, copy
 * - Faithful conversion, no issues
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 71
 * Covenant-baseline-methods: CylinderSpawnShapeValue,copy,depth,height,hf,isRadiusXZero,isRadiusZZero,radiusX,radiusZ,spawnAux,spawnTheta,this,ty,width
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/values/CylinderSpawnShapeValue.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: f087ba76cac7ff2cc4ded91efc009f52bf0987c2
 */
package sge
package graphics
package g3d
package particles
package values

import sge.math.{ MathUtils, Vector3 }

/** Encapsulate the formulas to spawn a particle on a cylinder shape.
  * @author
  *   Inferno
  */
final class CylinderSpawnShapeValue extends PrimitiveSpawnShapeValue {

  def this(cylinderSpawnShapeValue: CylinderSpawnShapeValue) = {
    this()
    load(cylinderSpawnShapeValue)
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit = {
    // Generate the point on the surface of the sphere
    val width  = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
    val height = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
    val depth  = spawnDepth + (spawnDepthDiff * spawnDepthValue.getScale(percent))

    var radiusX = 0f
    var radiusZ = 0f
    val hf      = height / 2
    val ty      = MathUtils.random(height) - hf

    // Where generate the point, on edges or inside ?
    if (edges) {
      radiusX = width / 2
      radiusZ = depth / 2
    } else {
      radiusX = MathUtils.random(width) / 2
      radiusZ = MathUtils.random(depth) / 2
    }

    var spawnTheta = 0f

    // Generate theta
    val isRadiusXZero = radiusX == 0
    val isRadiusZZero = radiusZ == 0
    if (!isRadiusXZero && !isRadiusZZero)
      spawnTheta = MathUtils.random(360f)
    else {
      if (isRadiusXZero)
        spawnTheta = if (MathUtils.random(1) == 0) -90 else 90
      else if (isRadiusZZero) spawnTheta = if (MathUtils.random(1) == 0) 0 else 180
    }

    vector.set(radiusX * MathUtils.cosDeg(spawnTheta), ty, radiusZ * MathUtils.sinDeg(spawnTheta))
  }

  override def copy(): SpawnShapeValue =
    CylinderSpawnShapeValue(this)
}
