/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package utils

import sge.graphics.{ Mesh, PrimitiveMode, VertexAttribute, VertexAttributes }
import sge.graphics.glutils.ShaderProgram

/** Encapsulates a fullscreen quad mesh. Geometry is aligned to the viewport corners.
  *
  * @author
  *   bmanuel
  * @author
  *   metaphore
  */
class ViewportQuadMesh(attributes: VertexAttribute*)(using Sge) extends AutoCloseable {

  private val mesh: Mesh = {
    val attrs = if (attributes.isEmpty) {
      Seq(
        VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
        VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
      )
    } else attributes

    val m = Mesh(true, 4, 0)(attrs*)
    m.setVertices(ViewportQuadMesh.verts)
    m
  }

  override def close(): Unit =
    mesh.close()

  /** Renders the quad with the specified shader program. */
  def render(program: ShaderProgram): Unit =
    mesh.render(program, PrimitiveMode.TriangleFan, 0, 4)

  def getMesh: Mesh = mesh
}

object ViewportQuadMesh {

  private val VERT_SIZE = 16

  val verts: Array[Float] = {
    val v = new Array[Float](VERT_SIZE)
    // Vertex coords
    v(0) = -1; v(1) = -1 // x1, y1
    v(4) = 1; v(5) = -1 // x2, y2
    v(8) = 1; v(9) = 1 // x3, y3
    v(12) = -1; v(13) = 1 // x4, y4
    // Tex coords
    v(2) = 0f; v(3) = 0f // u1, v1
    v(6) = 1f; v(7) = 0f // u2, v2
    v(10) = 1f; v(11) = 1f // u3, v3
    v(14) = 0f; v(15) = 1f // u4, v4
    v
  }
}
