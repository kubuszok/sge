/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/PatchShapeBuilder.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: GdxRuntimeException → SgeError.InvalidInput
 *   Convention: null → Nullable.empty
 *   Idiom: Java static class → Scala object
 *   Audited: 2026-03-04 — pass
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 113
 * Covenant-baseline-methods: PatchShapeBuilder,build
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/PatchShapeBuilder.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.g3d.utils.MeshPartBuilder
import sge.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import sge.math.Vector3
import sge.utils.Nullable

/** Helper class with static methods to build patch shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
object PatchShapeBuilder {
  import BaseShapeBuilder._

  def build(
    builder:    MeshPartBuilder,
    corner00:   VertexInfo,
    corner10:   VertexInfo,
    corner11:   VertexInfo,
    corner01:   VertexInfo,
    divisionsU: Int,
    divisionsV: Int
  ): Unit = {
    if (divisionsU < 1 || divisionsV < 1)
      throw new sge.utils.SgeError.InvalidInput("divisionsU and divisionsV must be > 0, got " + divisionsU + ", " + divisionsV)

    builder.ensureVertices((divisionsV + 1) * (divisionsU + 1))
    builder.ensureRectangleIndices(divisionsV * divisionsU)
    for (u <- 0 to divisionsU) {
      val alphaU = u.toFloat / divisionsU
      vertTmp5.set(Nullable(corner00)).lerp(corner10, alphaU)
      vertTmp6.set(Nullable(corner01)).lerp(corner11, alphaU)
      for (v <- 0 to divisionsV) {
        val idx = builder.vertex(vertTmp7.set(Nullable(vertTmp5)).lerp(vertTmp6, v.toFloat / divisionsV))
        if (u > 0 && v > 0)
          builder.rect(
            (idx - divisionsV - 2).toShort,
            (idx - 1).toShort,
            idx,
            (idx - divisionsV - 1).toShort
          )
      }
    }
  }

  def build(
    builder:    MeshPartBuilder,
    corner00:   Vector3,
    corner10:   Vector3,
    corner11:   Vector3,
    corner01:   Vector3,
    normal:     Vector3,
    divisionsU: Int,
    divisionsV: Int
  ): Unit =
    build(
      builder,
      vertTmp1.set(Nullable(corner00), Nullable(normal), Nullable.empty, Nullable.empty).setUV(0f, 1f),
      vertTmp2.set(Nullable(corner10), Nullable(normal), Nullable.empty, Nullable.empty).setUV(1f, 1f),
      vertTmp3.set(Nullable(corner11), Nullable(normal), Nullable.empty, Nullable.empty).setUV(1f, 0f),
      vertTmp4.set(Nullable(corner01), Nullable(normal), Nullable.empty, Nullable.empty).setUV(0f, 0f),
      divisionsU,
      divisionsV
    )

  def build(
    builder:    MeshPartBuilder,
    x00:        Float,
    y00:        Float,
    z00:        Float,
    x10:        Float,
    y10:        Float,
    z10:        Float,
    x11:        Float,
    y11:        Float,
    z11:        Float,
    x01:        Float,
    y01:        Float,
    z01:        Float,
    normalX:    Float,
    normalY:    Float,
    normalZ:    Float,
    divisionsU: Int,
    divisionsV: Int
  ): Unit =
    build(
      builder,
      vertTmp1.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x00, y00, z00).setNor(normalX, normalY, normalZ).setUV(0f, 1f),
      vertTmp2.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x10, y10, z10).setNor(normalX, normalY, normalZ).setUV(1f, 1f),
      vertTmp3.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x11, y11, z11).setNor(normalX, normalY, normalZ).setUV(1f, 0f),
      vertTmp4.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(x01, y01, z01).setNor(normalX, normalY, normalZ).setUV(0f, 0f),
      divisionsU,
      divisionsV
    )
}
