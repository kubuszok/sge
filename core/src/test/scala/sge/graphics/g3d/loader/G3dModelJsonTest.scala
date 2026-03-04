package sge
package graphics
package g3d
package loader

import com.github.plokhotnyuk.jsoniter_scala.core.{ readFromString, writeToString }

class G3dModelJsonTest extends munit.FunSuite {

  // ---------------------------------------------------------------------------
  // Round-trip tests
  // ---------------------------------------------------------------------------

  test("round-trip: full model with all optional fields") {
    val original = G3dModelJson(
      version = List[Short](0, 1),
      id = "full-model",
      meshes = List(
        G3dMeshJson(
          id = "mesh1",
          attributes = List("POSITION", "NORMAL", "TEXCOORD0"),
          vertices = List(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f),
          parts = List(
            G3dMeshPartJson(id = "part1", tpe = "TRIANGLES", indices = List[Short](0, 1, 2))
          )
        )
      ),
      materials = List(
        G3dMaterialJson(
          id = "mat1",
          diffuse = Some(List(1f, 0f, 0f)),
          specular = Some(List(1f, 1f, 1f)),
          shininess = 20f,
          opacity = 0.8f,
          textures = List(
            G3dTextureJson(id = "tex1", filename = "diffuse.png", tpe = "DIFFUSE")
          )
        )
      ),
      nodes = List(
        G3dNodeJson(
          id = "root",
          translation = Some(List(1f, 2f, 3f)),
          rotation = Some(List(0f, 0f, 0f, 1f)),
          scale = Some(List(1f, 1f, 1f)),
          parts = List(
            G3dNodePartJson(
              meshpartid = "part1",
              materialid = "mat1",
              bones = List(
                G3dBoneJson(
                  node = "bone1",
                  translation = Some(List(0f, 0f, 0f)),
                  rotation = Some(List(0f, 0f, 0f, 1f))
                )
              )
            )
          ),
          children = List(
            G3dNodeJson(id = "child1")
          )
        )
      ),
      animations = List(
        G3dAnimationJson(
          id = "walk",
          bones = List(
            G3dAnimBoneJson(
              boneId = "bone1",
              keyframes = Some(
                List(
                  G3dKeyframeV1Json(keytime = 0f, translation = Some(List(0f, 0f, 0f)))
                )
              )
            )
          )
        )
      )
    )

    val json     = writeToString(original)
    val restored = readFromString[G3dModelJson](json)

    assertEquals(restored, original)
  }

  // ---------------------------------------------------------------------------
  // Parse real .g3dj JSON snippets
  // ---------------------------------------------------------------------------

  test("parse: minimal model with version and empty collections") {
    val json = """{"version":[0,1]}"""
    val m    = readFromString[G3dModelJson](json)
    assertEquals(m.version, List[Short](0, 1))
    assertEquals(m.id, "")
    assertEquals(m.meshes, Nil)
    assertEquals(m.materials, Nil)
    assertEquals(m.nodes, Nil)
    assertEquals(m.animations, Nil)
  }

  test("parse: mesh with POSITION + NORMAL + TEXCOORD0 attributes") {
    val json =
      """{
        |  "version": [0, 1],
        |  "meshes": [{
        |    "id": "cube",
        |    "attributes": ["POSITION", "NORMAL", "TEXCOORD0"],
        |    "vertices": [0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.5, 0.5,
        |                 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.5],
        |    "parts": [
        |      {"id": "part0", "type": "TRIANGLES", "indices": [0, 1, 2]}
        |    ]
        |  }]
        |}""".stripMargin

    val m = readFromString[G3dModelJson](json)
    assertEquals(m.meshes.size, 1)
    val mesh = m.meshes.head
    assertEquals(mesh.id, "cube")
    assertEquals(mesh.attributes, List("POSITION", "NORMAL", "TEXCOORD0"))
    assertEquals(mesh.vertices.size, 16)
    assertEquals(mesh.parts.size, 1)
    assertEquals(mesh.parts.head.id, "part0")
    assertEquals(mesh.parts.head.tpe, "TRIANGLES")
    assertEquals(mesh.parts.head.indices, List[Short](0, 1, 2))
  }

  test("parse: material with color arrays and textures") {
    val json =
      """{
        |  "version": [0, 1],
        |  "materials": [{
        |    "id": "green_material",
        |    "diffuse": [0.0, 0.8, 0.0],
        |    "ambient": [0.1, 0.1, 0.1],
        |    "emissive": [0.0, 0.0, 0.0],
        |    "specular": [1.0, 1.0, 1.0],
        |    "reflection": [0.5, 0.5, 0.5],
        |    "shininess": 25.0,
        |    "opacity": 0.9,
        |    "textures": [
        |      {"id": "diffuseTexture", "filename": "green.png", "type": "DIFFUSE",
        |       "uvTranslation": [0.0, 0.0], "uvScaling": [1.0, 1.0]},
        |      {"id": "normalTexture", "filename": "green_n.png", "type": "NORMAL"}
        |    ]
        |  }]
        |}""".stripMargin

    val m   = readFromString[G3dModelJson](json)
    val mat = m.materials.head
    assertEquals(mat.id, "green_material")
    assertEquals(mat.diffuse, Some(List(0.0f, 0.8f, 0.0f)))
    assertEquals(mat.ambient, Some(List(0.1f, 0.1f, 0.1f)))
    assertEquals(mat.emissive, Some(List(0.0f, 0.0f, 0.0f)))
    assertEquals(mat.specular, Some(List(1.0f, 1.0f, 1.0f)))
    assertEquals(mat.reflection, Some(List(0.5f, 0.5f, 0.5f)))
    assertEquals(mat.shininess, 25.0f)
    assertEquals(mat.opacity, 0.9f)
    assertEquals(mat.textures.size, 2)

    val tex0 = mat.textures(0)
    assertEquals(tex0.id, "diffuseTexture")
    assertEquals(tex0.filename, "green.png")
    assertEquals(tex0.tpe, "DIFFUSE")
    assertEquals(tex0.uvTranslation, Some(List(0.0f, 0.0f)))
    assertEquals(tex0.uvScaling, Some(List(1.0f, 1.0f)))

    val tex1 = mat.textures(1)
    assertEquals(tex1.id, "normalTexture")
    assertEquals(tex1.tpe, "NORMAL")
    assertEquals(tex1.uvTranslation, None)
    assertEquals(tex1.uvScaling, None)
  }

  test("parse: material with defaults when optional fields absent") {
    val json =
      """{
        |  "version": [0, 1],
        |  "materials": [{"id": "bare"}]
        |}""".stripMargin

    val mat = readFromString[G3dModelJson](json).materials.head
    assertEquals(mat.diffuse, None)
    assertEquals(mat.ambient, None)
    assertEquals(mat.emissive, None)
    assertEquals(mat.specular, None)
    assertEquals(mat.reflection, None)
    assertEquals(mat.shininess, 0f)
    assertEquals(mat.opacity, 1f)
    assertEquals(mat.textures, Nil)
  }

  test("parse: node with translation, rotation, scale, and children") {
    val json =
      """{
        |  "version": [0, 1],
        |  "nodes": [{
        |    "id": "root",
        |    "translation": [1.0, 2.0, 3.0],
        |    "rotation": [0.0, 0.707, 0.0, 0.707],
        |    "scale": [2.0, 2.0, 2.0],
        |    "children": [
        |      {"id": "child_a"},
        |      {"id": "child_b", "translation": [10.0, 0.0, 0.0]}
        |    ]
        |  }]
        |}""".stripMargin

    val root = readFromString[G3dModelJson](json).nodes.head
    assertEquals(root.id, "root")
    assertEquals(root.translation, Some(List(1.0f, 2.0f, 3.0f)))
    assertEquals(root.rotation, Some(List(0.0f, 0.707f, 0.0f, 0.707f)))
    assertEquals(root.scale, Some(List(2.0f, 2.0f, 2.0f)))
    assertEquals(root.children.size, 2)
    assertEquals(root.children(0).id, "child_a")
    assertEquals(root.children(1).id, "child_b")
    assertEquals(root.children(1).translation, Some(List(10.0f, 0.0f, 0.0f)))
  }

  test("parse: node parts with bones") {
    val json =
      """{
        |  "version": [0, 1],
        |  "nodes": [{
        |    "id": "skinned",
        |    "mesh": "mesh0",
        |    "parts": [{
        |      "meshpartid": "body",
        |      "materialid": "skin_mat",
        |      "bones": [
        |        {"node": "hip", "translation": [0, 1, 0], "rotation": [0, 0, 0, 1], "scale": [1, 1, 1]},
        |        {"node": "leg", "translation": [0, 0.5, 0]}
        |      ]
        |    }]
        |  }]
        |}""".stripMargin

    val node = readFromString[G3dModelJson](json).nodes.head
    assertEquals(node.mesh, Some("mesh0"))
    val part = node.parts.head
    assertEquals(part.meshpartid, "body")
    assertEquals(part.materialid, "skin_mat")
    assertEquals(part.bones.size, 2)
    assertEquals(part.bones(0).node, "hip")
    assertEquals(part.bones(0).translation, Some(List(0f, 1f, 0f)))
    assertEquals(part.bones(0).rotation, Some(List(0f, 0f, 0f, 1f)))
    assertEquals(part.bones(0).scale, Some(List(1f, 1f, 1f)))
    assertEquals(part.bones(1).node, "leg")
    assertEquals(part.bones(1).rotation, None)
    assertEquals(part.bones(1).scale, None)
  }

  test("parse: v0.1 animation keyframes (combined channels)") {
    val json =
      """{
        |  "version": [0, 1],
        |  "animations": [{
        |    "id": "walk",
        |    "bones": [{
        |      "boneId": "hip",
        |      "keyframes": [
        |        {"keytime": 0.0, "translation": [0, 0, 0], "rotation": [0, 0, 0, 1], "scale": [1, 1, 1]},
        |        {"keytime": 0.5, "translation": [0, 1, 0], "rotation": [0, 0.707, 0, 0.707]},
        |        {"keytime": 1.0, "translation": [0, 0, 0]}
        |      ]
        |    }]
        |  }]
        |}""".stripMargin

    val anim = readFromString[G3dModelJson](json).animations.head
    assertEquals(anim.id, "walk")
    assertEquals(anim.bones.size, 1)

    val bone = anim.bones.head
    assertEquals(bone.boneId, "hip")
    assert(bone.keyframes.isDefined)
    assertEquals(bone.translation, None)
    assertEquals(bone.rotation, None)
    assertEquals(bone.scaling, None)

    val kfs = bone.keyframes.get
    assertEquals(kfs.size, 3)
    assertEquals(kfs(0).keytime, 0.0f)
    assertEquals(kfs(0).translation, Some(List(0f, 0f, 0f)))
    assertEquals(kfs(0).rotation, Some(List(0f, 0f, 0f, 1f)))
    assertEquals(kfs(0).scale, Some(List(1f, 1f, 1f)))
    assertEquals(kfs(1).keytime, 0.5f)
    assertEquals(kfs(1).translation, Some(List(0f, 1f, 0f)))
    assertEquals(kfs(1).scale, None)
    assertEquals(kfs(2).keytime, 1.0f)
    assertEquals(kfs(2).rotation, None)
  }

  test("parse: v0.2 animation keyframes (split channels)") {
    val json =
      """{
        |  "version": [0, 1],
        |  "animations": [{
        |    "id": "idle",
        |    "bones": [{
        |      "boneId": "spine",
        |      "translation": [
        |        {"keytime": 0.0, "value": [0, 0, 0]},
        |        {"keytime": 1.0, "value": [0, 0.1, 0]}
        |      ],
        |      "rotation": [
        |        {"keytime": 0.0, "value": [0, 0, 0, 1]}
        |      ],
        |      "scaling": [
        |        {"keytime": 0.0, "value": [1, 1, 1]}
        |      ]
        |    }]
        |  }]
        |}""".stripMargin

    val bone = readFromString[G3dModelJson](json).animations.head.bones.head
    assertEquals(bone.boneId, "spine")
    assertEquals(bone.keyframes, None)

    assert(bone.translation.isDefined)
    val trans = bone.translation.get
    assertEquals(trans.size, 2)
    assertEquals(trans(0).keytime, 0.0f)
    assertEquals(trans(0).value, List(0f, 0f, 0f))
    assertEquals(trans(1).keytime, 1.0f)
    assertEquals(trans(1).value, List(0f, 0.1f, 0f))

    assert(bone.rotation.isDefined)
    assertEquals(bone.rotation.get.size, 1)
    assertEquals(bone.rotation.get.head.value, List(0f, 0f, 0f, 1f))

    assert(bone.scaling.isDefined)
    assertEquals(bone.scaling.get.size, 1)
    assertEquals(bone.scaling.get.head.value, List(1f, 1f, 1f))
  }

  test("parse: v0.2 keyframe with missing value defaults to Nil") {
    // jsoniter-scala defaults collection fields to empty even without explicit defaults.
    // The loader checks value.size >= 3 before use, so empty list is safely skipped.
    val json =
      """{
        |  "version": [0, 1],
        |  "animations": [{
        |    "id": "test",
        |    "bones": [{
        |      "boneId": "bone1",
        |      "translation": [{"keytime": 0.0}]
        |    }]
        |  }]
        |}""".stripMargin

    val bone = readFromString[G3dModelJson](json).animations.head.bones.head
    assertEquals(bone.translation.get.head.value, Nil)
  }

  test("parse: unknown fields are silently ignored") {
    val json =
      """{
        |  "version": [0, 1],
        |  "unknownTopLevel": 42,
        |  "meshes": [{
        |    "id": "m",
        |    "attributes": ["POSITION"],
        |    "vertices": [0],
        |    "parts": [{"id": "p", "type": "POINTS", "indices": [0], "extraField": true}],
        |    "someOtherStuff": "ignored"
        |  }]
        |}""".stripMargin

    val m = readFromString[G3dModelJson](json)
    assertEquals(m.meshes.head.id, "m")
    assertEquals(m.meshes.head.parts.head.tpe, "POINTS")
  }

  test("parse: real-world cube model (based on libGDX cube.g3dj)") {
    val json =
      """{
        |  "version": [0, 1],
        |  "id": "",
        |  "meshes": [
        |    {
        |      "attributes": ["POSITION", "NORMAL", "TEXCOORD0"],
        |      "vertices": [
        |        -0.5, -0.5,  0.5,  0.0,  0.0,  1.0,  0.0,  0.0,
        |         0.5, -0.5,  0.5,  0.0,  0.0,  1.0,  1.0,  0.0,
        |         0.5,  0.5,  0.5,  0.0,  0.0,  1.0,  1.0,  1.0,
        |        -0.5,  0.5,  0.5,  0.0,  0.0,  1.0,  0.0,  1.0
        |      ],
        |      "parts": [
        |        {
        |          "id": "part0",
        |          "type": "TRIANGLES",
        |          "indices": [0, 1, 2, 2, 3, 0]
        |        }
        |      ]
        |    }
        |  ],
        |  "materials": [
        |    {
        |      "id": "material0",
        |      "diffuse": [0.8, 0.8, 0.8],
        |      "specular": [0.0, 0.0, 0.0],
        |      "shininess": 0.0,
        |      "textures": [
        |        {"id": "diffuseTexture", "filename": "badlogic.jpg", "type": "DIFFUSE"}
        |      ]
        |    }
        |  ],
        |  "nodes": [
        |    {
        |      "id": "Cube",
        |      "parts": [
        |        {
        |          "meshpartid": "part0",
        |          "materialid": "material0"
        |        }
        |      ]
        |    }
        |  ],
        |  "animations": []
        |}""".stripMargin

    val m = readFromString[G3dModelJson](json)
    assertEquals(m.version, List[Short](0, 1))
    assertEquals(m.meshes.size, 1)
    assertEquals(m.meshes.head.attributes, List("POSITION", "NORMAL", "TEXCOORD0"))
    // 4 vertices * (3 pos + 3 normal + 2 uv) = 32 floats
    assertEquals(m.meshes.head.vertices.size, 32)
    assertEquals(m.meshes.head.parts.head.indices, List[Short](0, 1, 2, 2, 3, 0))
    assertEquals(m.materials.head.id, "material0")
    assertEquals(m.materials.head.textures.head.filename, "badlogic.jpg")
    assertEquals(m.nodes.head.id, "Cube")
    assertEquals(m.nodes.head.parts.head.meshpartid, "part0")
    assertEquals(m.nodes.head.parts.head.materialid, "material0")
    assertEquals(m.animations, Nil)
  }
}
