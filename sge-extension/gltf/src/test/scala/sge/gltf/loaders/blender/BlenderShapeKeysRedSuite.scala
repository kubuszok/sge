/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-513 — BlenderShapeKeys.parse is an unconditional no-op:
 * `extras.fold(empty)(_ => empty)` (BlenderShapeKeys.scala:50), so Blender morph
 * target names from glTF `extras.targetNames` never reach NodePlus. The in-code
 * excuse ("targetNames parsing from glMesh.extras requires GLTF JSON codecs (not
 * yet ported)", BlenderShapeKeys.scala:12) is stale: GLTFExtras.value carries the
 * parsed Json AST today (GLTFExtras.scala:27, populated by GLTFCodecs.scala:385-388).
 *
 * Expected behavior from the original source (original-src/gdx-gltf/gltf/src/net/mgsx/gltf/):
 *   - loaders/blender/BlenderShapeKeys.java:27-34 `parse(GLTFMesh)`:
 *     line 28 returns null when extras is null;
 *     line 29 reads `glMesh.extras.value.get("targetNames")`;
 *     lines 30-32 return `new Array<String>(targetNames.asStringArray())` when it
 *     is a JSON array; line 33 returns null otherwise.
 *   - loaders/shared/geometry/MeshLoader.java:46
 *     `((NodePlus)node).morphTargetNames = BlenderShapeKeys.parse(glMesh);` — the
 *     parsed names land on NodePlus.morphTargetNames (the SGE port mirrors this
 *     assignment at MeshLoader.scala:46).
 */
package sge
package gltf
package loaders
package blender

import sge.gltf.data.geometry.GLTFMesh
import sge.gltf.loaders.gltf.GLTFJsonParser
import sge.gltf.scene3d.model.NodePlus
import lowlevel.Nullable

class BlenderShapeKeysRedSuite extends munit.FunSuite {

  /** Parses a minimal glTF document whose single mesh carries Blender-style `extras.targetNames` (the exact shape documented in BlenderShapeKeys.java:10-26). */
  private def meshWithTargetNames(): GLTFMesh = {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "meshes": [
        |    {
        |      "name": "Plane",
        |      "extras": {
        |        "targetNames": ["Water", "Mountains"]
        |      },
        |      "primitives": [{"attributes": {"POSITION": 0}}],
        |      "weights": [0.6, 0.3]
        |    }
        |  ]
        |}""".stripMargin
    val gltf = GLTFJsonParser.parse(json)
    gltf.meshes.get(0)
  }

  test("red: mesh extras targetNames reach NodePlus.morphTargetNames (BlenderShapeKeys.java:27-34, MeshLoader.java:46)") {
    val glMesh = meshWithTargetNames()

    // Precondition proving the "codecs not yet ported" excuse is stale: the parsed Json AST is available on GLTFExtras.value today.
    assert(Nullable.isDefined(glMesh.extras), "extras must be decoded from the glTF JSON")
    assert(Nullable.isDefined(glMesh.extras.get.value), "GLTFExtras.value must carry the parsed Json AST")

    // Mirror MeshLoader.java:46 / MeshLoader.scala:46 — where upstream puts the parsed names.
    val node = new NodePlus()
    node.morphTargetNames = BlenderShapeKeys.parse(glMesh)

    assert(
      Nullable.isDefined(node.morphTargetNames),
      "morphTargetNames must be set when extras.targetNames is a JSON array (BlenderShapeKeys.java:30-32)"
    )
    val names = node.morphTargetNames.get
    assertEquals(names.size, 2)
    assertEquals(names(0), "Water")
    assertEquals(names(1), "Mountains")
  }

  test("control: mesh without extras yields no morph target names (BlenderShapeKeys.java:28)") {
    val glMesh = new GLTFMesh()
    assert(!Nullable.isDefined(glMesh.extras))
    assert(!Nullable.isDefined(BlenderShapeKeys.parse(glMesh)))
  }

  test("control: extras without targetNames yields no morph target names (BlenderShapeKeys.java:33)") {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "meshes": [
        |    {
        |      "name": "Plane",
        |      "extras": {"somethingElse": 1},
        |      "primitives": [{"attributes": {"POSITION": 0}}]
        |    }
        |  ]
        |}""".stripMargin
    val glMesh = GLTFJsonParser.parse(json).meshes.get(0)
    assert(Nullable.isDefined(glMesh.extras))
    assert(!Nullable.isDefined(BlenderShapeKeys.parse(glMesh)))
  }
}
