/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): vfx ViewportQuadMesh — the fullscreen
 * quad geometry the whole vfx pipeline renders through. Until now the only vfx
 * suite was PrioritizedArraySuite (a pure util); the geometry/state-machine
 * classes had ZERO tests.
 *
 * Every expected value is hand-traced from the faithful full-port
 * ViewportQuadMesh.scala (covenant: full-port, source reference
 * com/crashinvaders/vfx/utils/ViewportQuadMesh.java). Cited "port" line numbers
 * refer to ViewportQuadMesh.scala under test:
 *   - verts (port lines 57-70): a 16-float, VERT_SIZE-wide (port line 55)
 *     interleaved [x, y, u, v] x 4 array. Vertex corners at float indices
 *     0,1 / 4,5 / 8,9 / 12,13 = (-1,-1),(1,-1),(1,1),(-1,1); tex coords at
 *     2,3 / 6,7 / 10,11 / 14,15 = (0,0),(1,0),(1,1),(0,1).
 *   - default attributes (port lines 31-36, taken when no attributes are
 *     passed): Position(size 2, "a_position"), TextureCoordinates(size 2,
 *     "a_texCoord0").
 *   - render (port line 48): mesh.render(program, PrimitiveMode.TriangleFan, 0,
 *     4) — a 4-vertex TriangleFan draw. The mesh is a VertexBufferObject-backed
 *     Mesh (Mesh(true, 4, 0), isVertexArray = false, no indices), so render's
 *     non-indexed VBO branch issues glDrawArrays(TriangleFan, 0, 4)
 *     (Mesh.scala line 682).
 *
 * Mutations these tests catch (canary DoD):
 *   - swap a corner coordinate (e.g. verts(0) = 1 instead of -1): the layout
 *     test fails on that exact float.
 *   - pass the wrong default attribute (size 3, or alias "a_texCoord1", or
 *     swap Position/TextureCoordinates): the default-attributes test fails.
 *   - render with the wrong primitive/count (e.g. Triangles, or count 6): the
 *     render test fails — mode/first/count are pinned exactly.
 */
package sge
package vfx
package utils

import sge.graphics.{ GL20, PrimitiveMode, VertexAttribute, VertexAttributes }
import sge.graphics.glutils.ShaderProgram
import sge.noop.NoopGL20

class ViewportQuadMeshISS561Suite extends munit.FunSuite {

  // --- pure geometry: the 16-float verts layout -------------------------------

  test("ISS561: ViewportQuadMesh.verts is the exact 16-float [x,y,u,v]x4 fullscreen-quad layout") {
    val v = ViewportQuadMesh.verts

    assertEquals(v.length, 16, "verts is VERT_SIZE (16) floats wide (port line 55-58)")

    // Vertex corner positions (port lines 60-63).
    assertEquals(v(0), -1f, "v0.x = -1")
    assertEquals(v(1), -1f, "v0.y = -1")
    assertEquals(v(4), 1f, "v1.x = 1")
    assertEquals(v(5), -1f, "v1.y = -1")
    assertEquals(v(8), 1f, "v2.x = 1")
    assertEquals(v(9), 1f, "v2.y = 1")
    assertEquals(v(12), -1f, "v3.x = -1")
    assertEquals(v(13), 1f, "v3.y = 1")

    // Texture coordinates (port lines 65-68).
    assertEquals(v(2), 0f, "v0.u = 0")
    assertEquals(v(3), 0f, "v0.v = 0")
    assertEquals(v(6), 1f, "v1.u = 1")
    assertEquals(v(7), 0f, "v1.v = 0")
    assertEquals(v(10), 1f, "v2.u = 1")
    assertEquals(v(11), 1f, "v2.v = 1")
    assertEquals(v(14), 0f, "v3.u = 0")
    assertEquals(v(15), 1f, "v3.v = 1")
  }

  // --- default vertex attributes ----------------------------------------------

  test("ISS561: with no attributes, ViewportQuadMesh builds Position(2,'a_position') + TextureCoordinates(2,'a_texCoord0')") {
    given Sge = VfxHeadless.headlessSge()
    val quad  = new ViewportQuadMesh()
    try {
      val attrs = quad.getMesh.vertexAttributes
      assertEquals(attrs.size, 2, "default layout has exactly two attributes (port lines 32-35)")

      val pos = attrs.get(0)
      assertEquals(pos.usage, VertexAttributes.Usage.Position, "attribute 0 is Position (port line 33)")
      assertEquals(pos.numComponents, 2, "Position has 2 components (port line 33)")
      assertEquals(pos.alias, "a_position", "Position alias is a_position (port line 33)")

      val tex = attrs.get(1)
      assertEquals(tex.usage, VertexAttributes.Usage.TextureCoordinates, "attribute 1 is TextureCoordinates (port line 34)")
      assertEquals(tex.numComponents, 2, "TextureCoordinates has 2 components (port line 34)")
      assertEquals(tex.alias, "a_texCoord0", "TextureCoordinates alias is a_texCoord0 (port line 34)")
    } finally quad.close()
  }

  test("ISS561: explicit attributes override the defaults") {
    given Sge = VfxHeadless.headlessSge()
    // A single 4-component attribute keeps the 16-byte vertex stride the verts
    // array requires while still being structurally different from the 2-attr
    // default, proving the supplied attributes are used instead.
    val customAttr = new VertexAttribute(VertexAttributes.Usage.Position, 4, "a_custom")
    val quad       = new ViewportQuadMesh(customAttr)
    try {
      val attrs = quad.getMesh.vertexAttributes
      assertEquals(attrs.size, 1, "when attributes are supplied the default branch (port lines 32-35) is NOT taken")
      assertEquals(attrs.get(0).alias, "a_custom", "the supplied attribute is used verbatim")
      assertEquals(attrs.get(0).numComponents, 4, "the supplied attribute's component count is used, not the default 2")
    } finally quad.close()
  }

  // --- render() draw call -----------------------------------------------------

  /** A recorded draw call: which entry point plus the args we pin. */
  final private case class DrawArraysCall(mode: PrimitiveMode, first: Int, count: Int)

  /** Recording GL20: hands out a nonzero buffer handle (NoopGL20.glGenBuffer == 0 would trip VertexBufferObject.bind's "No buffer allocated!" check) and records glDrawArrays. Everything else is
    * no-op.
    */
  final private class RecordingGL20 extends GL20 {
    val draws: scala.collection.mutable.ListBuffer[DrawArraysCall] = scala.collection.mutable.ListBuffer.empty

    private val underlying: GL20 = NoopGL20
    export underlying.{ glDrawArrays as _, glGenBuffer as _, * }

    def glGenBuffer(): Int = 1

    def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit =
      draws += DrawArraysCall(mode, first, count)
  }

  test("ISS561: render(program) issues a single glDrawArrays(TriangleFan, 0, 4)") {
    val gl     = new RecordingGL20()
    given Sge  = VfxHeadless.headlessSge(gl)
    val quad   = new ViewportQuadMesh()
    val shader = new ShaderProgram("void main(){}", "void main(){}")
    try {
      quad.render(shader)

      assertEquals(gl.draws.size, 1, "render must issue exactly one draw call (port line 48)")
      assertEquals(
        gl.draws.head,
        DrawArraysCall(PrimitiveMode.TriangleFan, 0, 4),
        "render must be a 4-vertex TriangleFan starting at 0 — NOT Triangles and NOT a different count (port line 48)"
      )
    } finally {
      shader.close()
      quad.close()
    }
  }
}
