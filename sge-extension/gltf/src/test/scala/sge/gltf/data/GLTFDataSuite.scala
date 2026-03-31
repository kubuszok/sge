package sge
package gltf
package data

import scala.collection.mutable.ArrayBuffer
import sge.gltf.data.geometry.GLTFMesh
import sge.gltf.data.scene.{ GLTFNode, GLTFScene }
import sge.utils.Nullable

class GLTFDataSuite extends munit.FunSuite {

  test("GLTF: create empty model with defaults") {
    val gltf = new GLTF()
    assertEquals(gltf.scene, 0)
    assert(Nullable.isEmpty(gltf.asset))
    assert(Nullable.isEmpty(gltf.scenes))
    assert(Nullable.isEmpty(gltf.nodes))
    assert(Nullable.isEmpty(gltf.meshes))
    assert(Nullable.isEmpty(gltf.materials))
    assert(Nullable.isEmpty(gltf.accessors))
    assert(Nullable.isEmpty(gltf.buffers))
    assert(Nullable.isEmpty(gltf.bufferViews))
    assert(Nullable.isEmpty(gltf.images))
    assert(Nullable.isEmpty(gltf.textures))
    assert(Nullable.isEmpty(gltf.animations))
    assert(Nullable.isEmpty(gltf.skins))
    assert(Nullable.isEmpty(gltf.extensionsUsed))
    assert(Nullable.isEmpty(gltf.extensionsRequired))
  }

  test("GLTF: set asset metadata") {
    val gltf  = new GLTF()
    val asset = new GLTFAsset()
    asset.version = Nullable("2.0")
    asset.generator = Nullable("SGE Test")
    asset.copyright = Nullable("2026 Test")
    gltf.asset = Nullable(asset)

    assert(Nullable.isDefined(gltf.asset))
    assertEquals(gltf.asset.get.version.get, "2.0")
    assertEquals(gltf.asset.get.generator.get, "SGE Test")
    assertEquals(gltf.asset.get.copyright.get, "2026 Test")
    assert(Nullable.isEmpty(gltf.asset.get.minVersion))
  }

  test("GLTF: populate scenes and nodes") {
    val gltf = new GLTF()

    val scene = new GLTFScene()
    scene.name = Nullable("MainScene")
    gltf.scenes = Nullable(ArrayBuffer(scene))
    gltf.scene = 0

    val node = new GLTFNode()
    node.name = Nullable("RootNode")
    gltf.nodes = Nullable(ArrayBuffer(node))

    assertEquals(gltf.scenes.get.size, 1)
    assertEquals(gltf.scenes.get(0).name.get, "MainScene")
    assertEquals(gltf.nodes.get.size, 1)
    assertEquals(gltf.nodes.get(0).name.get, "RootNode")
  }

  test("GLTF: populate meshes") {
    val gltf = new GLTF()
    val mesh = new GLTFMesh()
    mesh.name = Nullable("CubeMesh")
    gltf.meshes = Nullable(ArrayBuffer(mesh))

    assertEquals(gltf.meshes.get.size, 1)
    assertEquals(gltf.meshes.get(0).name.get, "CubeMesh")
  }

  test("GLTF: set extensions metadata") {
    val gltf = new GLTF()
    gltf.extensionsUsed = Nullable(ArrayBuffer("KHR_materials_unlit", "KHR_lights_punctual"))
    gltf.extensionsRequired = Nullable(ArrayBuffer("KHR_materials_unlit"))

    assertEquals(gltf.extensionsUsed.get.size, 2)
    assertEquals(gltf.extensionsRequired.get.size, 1)
    assert(gltf.extensionsUsed.get.contains("KHR_lights_punctual"))
    assert(gltf.extensionsRequired.get.contains("KHR_materials_unlit"))
  }

  test("GLTFObject: extensions and extras default to empty") {
    val gltf = new GLTF()
    assert(Nullable.isEmpty(gltf.extensions))
    assert(Nullable.isEmpty(gltf.extras))
  }
}
