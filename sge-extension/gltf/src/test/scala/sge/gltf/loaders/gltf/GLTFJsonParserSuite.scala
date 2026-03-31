package sge
package gltf
package loaders
package gltf

import sge.gltf.data.GLTFCodecs.given
import sge.utils.Nullable

class GLTFJsonParserSuite extends munit.FunSuite {

  test("parse minimal GLTF") {
    val json =
      """{"asset":{"version":"2.0","generator":"test"},"scene":0}"""
    val gltf = GLTFJsonParser.parse(json)
    assertEquals(gltf.scene, 0)
    assert(Nullable.isDefined(gltf.asset))
    assertEquals(gltf.asset.get.version.get, "2.0")
    assertEquals(gltf.asset.get.generator.get, "test")
  }

  test("parse GLTF with scenes and nodes") {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "scene": 0,
        |  "scenes": [{"name": "Scene", "nodes": [0]}],
        |  "nodes": [
        |    {"name": "Cube", "mesh": 0, "translation": [1.0, 2.0, 3.0]}
        |  ],
        |  "meshes": [{"name": "CubeMesh", "primitives": [{"attributes": {"POSITION": 0}, "indices": 1}]}]
        |}""".stripMargin
    val gltf = GLTFJsonParser.parse(json)
    assertEquals(gltf.scenes.get.size, 1)
    assertEquals(gltf.scenes.get(0).name.get, "Scene")
    assertEquals(gltf.scenes.get(0).nodes.get.size, 1)
    assertEquals(gltf.scenes.get(0).nodes.get(0), 0)

    assertEquals(gltf.nodes.get.size, 1)
    assertEquals(gltf.nodes.get(0).name.get, "Cube")
    assertEquals(gltf.nodes.get(0).mesh.get, 0)
    val t = gltf.nodes.get(0).translation.get
    assertEqualsFloat(t(0), 1.0f, 0.001f)
    assertEqualsFloat(t(1), 2.0f, 0.001f)
    assertEqualsFloat(t(2), 3.0f, 0.001f)

    assertEquals(gltf.meshes.get.size, 1)
    assertEquals(gltf.meshes.get(0).primitives.get.size, 1)
    assertEquals(gltf.meshes.get(0).primitives.get(0).attributes.get("POSITION"), 0)
    assertEquals(gltf.meshes.get(0).primitives.get(0).indices.get, 1)
  }

  test("parse GLTF with materials") {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "materials": [{
        |    "name": "DefaultMat",
        |    "pbrMetallicRoughness": {
        |      "baseColorFactor": [1.0, 0.5, 0.0, 1.0],
        |      "metallicFactor": 0.0,
        |      "roughnessFactor": 0.8
        |    },
        |    "doubleSided": true,
        |    "alphaMode": "BLEND"
        |  }]
        |}""".stripMargin
    val gltf = GLTFJsonParser.parse(json)
    val mat  = gltf.materials.get(0)
    assertEquals(mat.name.get, "DefaultMat")
    assertEquals(mat.doubleSided.get, true)
    assertEquals(mat.alphaMode.get, "BLEND")
    val pbr = mat.pbrMetallicRoughness.get
    assertEqualsFloat(pbr.metallicFactor, 0.0f, 0.001f)
    assertEqualsFloat(pbr.roughnessFactor, 0.8f, 0.001f)
    assertEqualsFloat(pbr.baseColorFactor.get(0), 1.0f, 0.001f)
    assertEqualsFloat(pbr.baseColorFactor.get(1), 0.5f, 0.001f)
  }

  test("parse GLTF with buffers and accessors") {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "buffers": [{"uri": "data:application/octet-stream;base64,AAAA", "byteLength": 3}],
        |  "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 3, "target": 34962}],
        |  "accessors": [{"bufferView": 0, "componentType": 5126, "count": 1, "type": "VEC3"}]
        |}""".stripMargin
    val gltf = GLTFJsonParser.parse(json)
    assertEquals(gltf.buffers.get.size, 1)
    assertEquals(gltf.buffers.get(0).byteLength, 3)
    assert(gltf.buffers.get(0).uri.isDefined)

    assertEquals(gltf.bufferViews.get.size, 1)
    assertEquals(gltf.bufferViews.get(0).buffer.get, 0)
    assertEquals(gltf.bufferViews.get(0).target.get, 34962)

    assertEquals(gltf.accessors.get.size, 1)
    assertEquals(gltf.accessors.get(0).componentType, 5126)
    assertEquals(gltf.accessors.get(0).`type`.get, "VEC3")
  }

  test("parse GLTF with animations") {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "animations": [{
        |    "name": "Walk",
        |    "channels": [{"sampler": 0, "target": {"node": 0, "path": "translation"}}],
        |    "samplers": [{"input": 0, "output": 1, "interpolation": "LINEAR"}]
        |  }]
        |}""".stripMargin
    val gltf = GLTFJsonParser.parse(json)
    val anim = gltf.animations.get(0)
    assertEquals(anim.name.get, "Walk")
    assertEquals(anim.channels.get(0).sampler.get, 0)
    assertEquals(anim.channels.get(0).target.get.node.get, 0)
    assertEquals(anim.channels.get(0).target.get.path.get, "translation")
    assertEquals(anim.samplers.get(0).interpolation.get, "LINEAR")
  }

  test("parse GLTF with extensionsUsed") {
    val json =
      """{
        |  "asset": {"version": "2.0"},
        |  "extensionsUsed": ["KHR_materials_unlit", "KHR_lights_punctual"],
        |  "extensionsRequired": ["KHR_materials_unlit"]
        |}""".stripMargin
    val gltf = GLTFJsonParser.parse(json)
    assertEquals(gltf.extensionsUsed.get.size, 2)
    assert(gltf.extensionsUsed.get.contains("KHR_materials_unlit"))
    assert(gltf.extensionsUsed.get.contains("KHR_lights_punctual"))
    assertEquals(gltf.extensionsRequired.get.size, 1)
  }

  test("parse invalid JSON throws GLTFRuntimeException") {
    intercept[sge.gltf.loaders.exceptions.GLTFRuntimeException] {
      GLTFJsonParser.parse("not valid json")
    }
  }

  test("parse unknown fields are skipped") {
    val json =
      """{"asset":{"version":"2.0","unknownField":"ignored","nestedUnknown":{"a":1}},"scene":0,"futureField":[1,2,3]}"""
    val gltf = GLTFJsonParser.parse(json)
    assertEquals(gltf.asset.get.version.get, "2.0")
    assertEquals(gltf.scene, 0)
  }
}
