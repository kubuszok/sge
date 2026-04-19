/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/BoxShapeBuilder.java
 * Original authors: realitix, xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java static utility class -> Scala object; null params -> Nullable[T]
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 178
 * Covenant-baseline-methods: BoxShapeBuilder,build,hd,hh,hw,i000,i001,i010,i011,i100,i101,i110,i111,primitiveType,x0,x1,y0,y1,z0,z1
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/BoxShapeBuilder.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.PrimitiveMode
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.utils.MeshPartBuilder
import sge.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import sge.math.{ Matrix4, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.Nullable

/** Helper class with static methods to build box shapes using {@link MeshPartBuilder}.
  * @author
  *   realitix, xoppa
  */
@scala.annotation.nowarn("msg=deprecated")
object BoxShapeBuilder {
  import BaseShapeBuilder._

  /** Build a box with the shape of the specified {@link BoundingBox}.
    * @param box
    */
  def build(builder: MeshPartBuilder, box: BoundingBox): Unit = {
    builder.box(
      box.corner000(obtainV3()),
      box.corner010(obtainV3()),
      box.corner100(obtainV3()),
      box.corner110(obtainV3()),
      box.corner001(obtainV3()),
      box.corner011(obtainV3()),
      box.corner101(obtainV3()),
      box.corner111(obtainV3())
    )
    freeAll()
  }

  /** Add a box. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def build(
    builder:   MeshPartBuilder,
    corner000: VertexInfo,
    corner010: VertexInfo,
    corner100: VertexInfo,
    corner110: VertexInfo,
    corner001: VertexInfo,
    corner011: VertexInfo,
    corner101: VertexInfo,
    corner111: VertexInfo
  ): Unit = {
    builder.ensureVertices(8)
    val i000 = builder.vertex(corner000)
    val i100 = builder.vertex(corner100)
    val i110 = builder.vertex(corner110)
    val i010 = builder.vertex(corner010)
    val i001 = builder.vertex(corner001)
    val i101 = builder.vertex(corner101)
    val i111 = builder.vertex(corner111)
    val i011 = builder.vertex(corner011)

    val primitiveType = builder.primitiveType
    if (primitiveType == PrimitiveMode.Lines) {
      builder.ensureIndices(24)
      builder.rect(i000, i100, i110, i010)
      builder.rect(i101, i001, i011, i111)
      builder.index(i000, i001, i010, i011, i110, i111, i100, i101)
    } else if (primitiveType == PrimitiveMode.Points) {
      builder.ensureRectangleIndices(2)
      builder.rect(i000, i100, i110, i010)
      builder.rect(i101, i001, i011, i111)
    } else { // GL20.GL_TRIANGLES
      builder.ensureRectangleIndices(6)
      builder.rect(i000, i100, i110, i010)
      builder.rect(i101, i001, i011, i111)
      builder.rect(i000, i010, i011, i001)
      builder.rect(i101, i111, i110, i100)
      builder.rect(i101, i100, i000, i001)
      builder.rect(i110, i111, i011, i010)
    }
  }

  /** Add a box. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def build(
    builder:   MeshPartBuilder,
    corner000: Vector3,
    corner010: Vector3,
    corner100: Vector3,
    corner110: Vector3,
    corner001: Vector3,
    corner011: Vector3,
    corner101: Vector3,
    corner111: Vector3
  ): Unit =
    if ((builder.attributes.mask & (Usage.Normal | Usage.BiNormal | Usage.Tangent | Usage.TextureCoordinates)) == 0) {
      build(
        builder,
        vertTmp1.set(Nullable(corner000), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp2.set(Nullable(corner010), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp3.set(Nullable(corner100), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp4.set(Nullable(corner110), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp5.set(Nullable(corner001), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp6.set(Nullable(corner011), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp7.set(Nullable(corner101), Nullable.empty, Nullable.empty, Nullable.empty),
        vertTmp8.set(Nullable(corner111), Nullable.empty, Nullable.empty, Nullable.empty)
      )
    } else {
      builder.ensureVertices(24)
      builder.ensureRectangleIndices(6)
      var nor = tmpV1.set(corner000).lerp(corner110, 0.5f).sub(tmpV2.set(corner001).lerp(corner111, 0.5f)).nor()
      builder.rect(corner000, corner010, corner110, corner100, nor)
      builder.rect(corner011, corner001, corner101, corner111, nor.scl(-1))
      nor = tmpV1.set(corner000).lerp(corner101, 0.5f).sub(tmpV2.set(corner010).lerp(corner111, 0.5f)).nor()
      builder.rect(corner001, corner000, corner100, corner101, nor)
      builder.rect(corner010, corner011, corner111, corner110, nor.scl(-1))
      nor = tmpV1.set(corner000).lerp(corner011, 0.5f).sub(tmpV2.set(corner100).lerp(corner111, 0.5f)).nor()
      builder.rect(corner001, corner011, corner010, corner000, nor)
      builder.rect(corner100, corner110, corner111, corner101, nor.scl(-1))
    }

  /** Add a box given the matrix. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def build(builder: MeshPartBuilder, transform: Matrix4): Unit = {
    build(
      builder,
      obtainV3().set(-0.5f, -0.5f, -0.5f).mul(transform),
      obtainV3().set(-0.5f, 0.5f, -0.5f).mul(transform),
      obtainV3().set(0.5f, -0.5f, -0.5f).mul(transform),
      obtainV3().set(0.5f, 0.5f, -0.5f).mul(transform),
      obtainV3().set(-0.5f, -0.5f, 0.5f).mul(transform),
      obtainV3().set(-0.5f, 0.5f, 0.5f).mul(transform),
      obtainV3().set(0.5f, -0.5f, 0.5f).mul(transform),
      obtainV3().set(0.5f, 0.5f, 0.5f).mul(transform)
    )
    freeAll()
  }

  /** Add a box with the specified dimensions. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float): Unit =
    build(builder, 0, 0, 0, width, height, depth)

  /** Add a box at the specified location, with the specified dimensions */
  def build(builder: MeshPartBuilder, x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float): Unit = {
    val hw = width * 0.5f
    val hh = height * 0.5f
    val hd = depth * 0.5f
    val x0 = x - hw
    val y0 = y - hh
    val z0 = z - hd
    val x1 = x + hw
    val y1 = y + hh
    val z1 = z + hd
    build(
      builder,
      obtainV3().set(x0, y0, z0),
      obtainV3().set(x0, y1, z0),
      obtainV3().set(x1, y0, z0),
      obtainV3().set(x1, y1, z0),
      obtainV3().set(x0, y0, z1),
      obtainV3().set(x0, y1, z1),
      obtainV3().set(x1, y0, z1),
      obtainV3().set(x1, y1, z1)
    )
    freeAll()
  }
}
