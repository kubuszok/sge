/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/EllipseSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: spawnAux, side (public var), load, copy
 * - Java `side` is package-private; Scala version is `var side` (public) -- minor visibility widening
 * - spawnAux uses boundary/break instead of return (correct pattern)
 * - Fixes (2026-03-04): removed redundant getSide/setSide (field already public var)
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 98
 * Covenant-baseline-methods: EllipseSpawnShapeValue,copy,depth,height,load,maxT,minT,r,radiusX,radiusY,radiusZ,shape,side,spawnAux,t,this,width,z
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/values/EllipseSpawnShapeValue.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package values

import scala.util.boundary
import scala.util.boundary.break

import sge.math.{ MathUtils, Vector3 }

/** Encapsulate the formulas to spawn a particle on a ellipse shape.
  * @author
  *   Inferno
  */
final class EllipseSpawnShapeValue extends PrimitiveSpawnShapeValue {
  import PrimitiveSpawnShapeValue.SpawnSide

  var side: SpawnSide = SpawnSide.both

  def this(value: EllipseSpawnShapeValue) = {
    this()
    load(value)
  }

  override def spawnAux(vector: Vector3, percent: Float): Unit = boundary {
    // Generate the point on the surface of the sphere
    val width  = spawnWidth + spawnWidthDiff * spawnWidthValue.getScale(percent)
    val height = spawnHeight + spawnHeightDiff * spawnHeightValue.getScale(percent)
    val depth  = spawnDepth + spawnDepthDiff * spawnDepthValue.getScale(percent)

    var radiusX = 0f
    var radiusY = 0f
    var radiusZ = 0f
    // Where generate the point, on edges or inside ?
    val minT = 0f
    var maxT = MathUtils.PI2
    if (side == SpawnSide.top) {
      maxT = MathUtils.PI
    } else if (side == SpawnSide.bottom) {
      maxT = -MathUtils.PI
    }
    val t = MathUtils.random(minT, maxT)

    // Where generate the point, on edges or inside ?
    if (edges) {
      if (width == 0) {
        vector.set(0, height / 2 * MathUtils.sin(t), depth / 2 * MathUtils.cos(t))
        break(())
      }
      if (height == 0) {
        vector.set(width / 2 * MathUtils.cos(t), 0, depth / 2 * MathUtils.sin(t))
        break(())
      }
      if (depth == 0) {
        vector.set(width / 2 * MathUtils.cos(t), height / 2 * MathUtils.sin(t), 0)
        break(())
      }

      radiusX = width / 2
      radiusY = height / 2
      radiusZ = depth / 2
    } else {
      radiusX = MathUtils.random(width / 2)
      radiusY = MathUtils.random(height / 2)
      radiusZ = MathUtils.random(depth / 2)
    }

    val z = MathUtils.random(-1, 1f)
    val r = Math.sqrt(1f - z * z).toFloat
    vector.set(radiusX * r * MathUtils.cos(t), radiusY * r * MathUtils.sin(t), radiusZ * z)
  }

  override def load(value: ParticleValue): Unit = {
    super.load(value)
    val shape = value.asInstanceOf[EllipseSpawnShapeValue]
    side = shape.side
  }

  override def copy(): SpawnShapeValue =
    EllipseSpawnShapeValue(this)
}
