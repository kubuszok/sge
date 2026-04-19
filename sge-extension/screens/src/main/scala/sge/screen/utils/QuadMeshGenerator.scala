/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 68
 * Covenant-baseline-methods: QuadMeshGenerator,createFullScreenQuad,mesh
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package utils

import sge.graphics.{ Mesh, VertexAttribute, VertexAttributes }

/** Utility for creating full-screen quad meshes for use in shader-based transitions.
  *
  * @author
  *   damios
  */
object QuadMeshGenerator {

  /** Creates a mesh representing a full-screen quad with the given dimensions.
    *
    * The quad covers the area from (0,0) to (width, height) with UV coordinates from (0,0) to (1,1).
    *
    * @param width
    *   the width of the quad
    * @param height
    *   the height of the quad
    * @return
    *   a Mesh with 4 vertices forming a full-screen quad
    */
  def createFullScreenQuad(width: Float, height: Float)(using Sge): Mesh = {
    val mesh = Mesh(
      true,
      4,
      6
    )(
      VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
      VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
    )

    // Four corners: position (x, y), texcoord (u, v)
    mesh.setVertices(
      Array[Float](
        0f,
        0f,
        0f,
        0f, // bottom-left
        width,
        0f,
        1f,
        0f, // bottom-right
        width,
        height,
        1f,
        1f, // top-right
        0f,
        height,
        0f,
        1f // top-left
      )
    )

    mesh.setIndices(Array[Short](0, 1, 2, 2, 3, 0))

    mesh
  }
}
