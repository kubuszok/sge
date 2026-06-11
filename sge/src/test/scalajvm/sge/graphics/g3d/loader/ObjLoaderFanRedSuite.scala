/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-498 (ObjLoader face-fan loop drops triangles of any
 * polygon with more than 3 vertices).
 *
 * Root cause being reproduced: the face-parsing loop in
 * sge/src/main/scala/sge/graphics/g3d/loader/ObjLoader.scala lines 135-159
 * advances `i` by THREE per iteration (the `i += 1` at lines 147, 152 and
 * 158), so a quad emits one triangle instead of two and a pentagon one
 * instead of three.
 *
 * The original Java loop (original-src/libgdx/gdx/src/com/badlogic/gdx/
 * graphics/g3d/loader/ObjLoader.java lines 148-168) reads:
 *
 *   for (int i = 1; i < tokens.length - 2; i--) {   // line 148
 *     parts = tokens[1].split("/");                 // line 149: apex, ALWAYS tokens[1]
 *     ...
 *     parts = tokens[++i].split("/");               // line 159
 *     ...
 *     parts = tokens[++i].split("/");               // line 163
 *     ...
 *     activeGroup.numFaces++;                       // line 167
 *   }
 *
 * The `i--` in the update clause looks like a bug but nets out with the two
 * `++i` in the body to +1 per iteration, producing a triangle FAN anchored
 * at the first face vertex. Trace for `f 1 2 3 4` (tokens.length = 5,
 * guard i < 3):
 *
 *   iter 1: i=1 -> apex tokens[1], ++i=2 -> tokens[2], ++i=3 -> tokens[3];
 *           numFaces=1; update i-- -> i=2          => triangle (v1, v2, v3)
 *   iter 2: i=2 -> apex tokens[1], ++i=3 -> tokens[3], ++i=4 -> tokens[4];
 *           numFaces=2; update i-- -> i=3; guard 3 < 3 fails
 *                                                  => triangle (v1, v3, v4)
 *
 * so faces = [0,1,2, 0,2,3] (getIndex subtracts 1, Java lines 305-312) and
 * numFaces = 2. For a pentagon `f 1 2 3 4 5` (guard i < 4) a third
 * iteration adds (v1, v4, v5): numFaces = 3.
 *
 * There is NO vertex dedup downstream: the vertex-build loop (Java lines
 * 216-233) copies one position per face element into finalVerts, and
 * finalIndices is the sequential ramp 0..numFaces*3-1 (Java lines 236-242).
 * Hence the expectations below: a quad yields 6 indices and 18 position
 * floats laid out as (v1,v2,v3)(v1,v3,v4); a pentagon yields 9 indices.
 *
 * The current port nets +3 per iteration, so its single quad iteration
 * consumes tokens (1)(2)(3) and exits: 3 indices — RED. The pure-triangle
 * test is the pinned CONTROL: one iteration is exactly right there, so it
 * is green at the red commit and must stay green after the fix.
 *
 * Platform placement: JVM and Native copies only (byte-identical). The suite
 * itself is pure in-memory data, but ObjLoader.loadModelData reaches the
 * FileHandle BASE CLASS read()/exists() bodies, which reference
 * java.io.FileInputStream / Class.getResourceAsStream — non-existent on
 * Scala.js, so the JS test linker rejects any suite touching this path.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g3d
package loader

import java.io.{ ByteArrayInputStream, InputStream }
import java.nio.charset.StandardCharsets

import sge.assets.loaders.FileHandleResolver
import sge.files.{ FileHandle, FileHandleStream }
import sge.graphics.g3d.model.data.ModelData
import lowlevel.Nullable

class ObjLoaderFanRedSuite extends munit.FunSuite {

  /** In-memory OBJ "file": only read() is exercised by loadModelData when the OBJ has no mtllib line, so no real filesystem (and no GL) is touched on any platform. */
  private final class StringObjFileHandle(content: String) extends FileHandleStream("iss498-test.obj") {
    override def read(): InputStream =
      new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
  }

  private def loadObj(content: String): ModelData = {
    given Sge = SgeTestFixture.testSge()
    val loader = new ObjLoader(
      new FileHandleResolver {
        def resolve(fileName: String): FileHandle = throw new UnsupportedOperationException
      }
    )
    val data = loader.loadModelData(new StringObjFileHandle(content), Nullable.empty[ObjLoader.ObjLoaderParameters])
    assert(data.isDefined, "loadModelData returned no ModelData for a valid OBJ")
    data.get
  }

  /** The single mesh part produced for the default group. */
  private def soleIndices(data: ModelData): Array[Short] = {
    assertEquals(data.meshes.size, 1, "expected exactly one mesh")
    assertEquals(data.meshes(0).parts.length, 1, "expected exactly one mesh part")
    data.meshes(0).parts(0).indices
  }

  // Distinct positions so the fan order is visible in the vertex stream.
  private val QuadObj =
    """v 0 0 0
      |v 1 0 0
      |v 1 1 0
      |v 0 1 0
      |f 1 2 3 4
      |""".stripMargin

  private val PentagonObj =
    """v 0 0 0
      |v 1 0 0
      |v 2 1 0
      |v 1 2 0
      |v 0 1 0
      |f 1 2 3 4 5
      |""".stripMargin

  private val TriangleObj =
    """v 0 0 0
      |v 1 0 0
      |v 0 1 0
      |f 1 2 3
      |""".stripMargin

  test("ISS-498: quad face f 1 2 3 4 triangulates to a 2-triangle fan (6 indices)") {
    val data = loadObj(QuadObj)
    // Java: numFaces = 2 (lines 148-168 trace above), finalIndices length
    // numFaces * 3 = 6, filled with the sequential ramp 0..5 (lines 251-258).
    assertEquals(
      soleIndices(data).toList,
      List[Short](0, 1, 2, 3, 4, 5),
      "quad must produce two fan triangles (v1,v2,v3)(v1,v3,v4) — 6 sequential indices"
    )
  }

  test("ISS-498: quad fan vertex stream is (v1,v2,v3)(v1,v3,v4) per the Java loop trace") {
    val data = loadObj(QuadObj)
    // No dedup: one 3-float position per face element (Java lines 224-249),
    // in fan order anchored at tokens[1] (Java line 149).
    val expected = List[Float](
      0, 0, 0, // v1  (triangle 1: apex)
      1, 0, 0, // v2
      1, 1, 0, // v3
      0, 0, 0, // v1  (triangle 2: apex again)
      1, 1, 0, // v3
      0, 1, 0 // v4
    )
    assertEquals(
      data.meshes(0).vertices.toList,
      expected,
      "quad vertex stream must be the fan (v1,v2,v3)(v1,v3,v4)"
    )
  }

  test("ISS-498: pentagon face f 1 2 3 4 5 triangulates to a 3-triangle fan (9 indices)") {
    val data = loadObj(PentagonObj)
    // Java guard i < 4 runs three iterations: (v1,v2,v3)(v1,v3,v4)(v1,v4,v5),
    // numFaces = 3 -> 9 sequential indices.
    assertEquals(
      soleIndices(data).toList,
      (0 until 9).map(_.toShort).toList,
      "pentagon must produce three fan triangles — 9 sequential indices"
    )
  }

  test("ISS-498 control (green at red commit): pure triangle face keeps 3 indices") {
    val data = loadObj(TriangleObj)
    // tokens.length = 4, guard i < 2: exactly one iteration in Java AND in
    // the current port — this pins the non-fan path so the fix cannot
    // overshoot.
    assertEquals(
      soleIndices(data).toList,
      List[Short](0, 1, 2),
      "single triangle face must produce exactly one triangle"
    )
    assertEquals(
      data.meshes(0).vertices.toList,
      List[Float](0, 0, 0, 1, 0, 0, 0, 1, 0),
      "triangle vertex stream must be (v1,v2,v3)"
    )
  }
}
