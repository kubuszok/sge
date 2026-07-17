/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-723 clause c10: the previous GLTFDataSuite was a pure in-memory var set/get smoke test — it
 * constructed a GLTF, wrote a field, and read the very same field back, so no assertion could ever
 * fail on a real defect (the object graph is just plain mutable state). It exercised NONE of the
 * serialization machinery that turns a GLTF into a glTF document and back.
 *
 * This replacement builds an equivalent GLTF programmatically, then round-trips it through the
 * actual codec path (sge.utils.writeToString via GLTFCodecs.given -> GLTFJsonParser.parse) and
 * asserts every field survives the RE-DECODED model. Because the assertions read the post-round-trip
 * object (not the object we constructed), any encoder that drops or nulls a field — or any decoder
 * that fails to read it back — makes the corresponding assertion go red. This mirrors the round-trip
 * strategy of GltfCorpusRoundTripSuite / KhrLightsPunctualRedSuite, but seeded from an in-code GLTF
 * rather than a corpus string, keeping the field coverage of the original suite (asset metadata,
 * scenes/nodes, meshes, extension-name arrays) while making the suite able to fail.
 *
 * Platform scope: shared (src/test/scala) — pure jsoniter-scala codecs and the data model, no GL or
 * filesystem dependency.
 */
package sge
package gltf
package data

import scala.collection.mutable.ArrayBuffer
import sge.gltf.data.geometry.GLTFMesh
import sge.gltf.data.scene.{ GLTFNode, GLTFScene }
import sge.gltf.data.GLTFCodecs.given
import sge.gltf.loaders.gltf.GLTFJsonParser
import sge.utils.{ given_JsonCodec_Json, writeToString }
import lowlevel.Nullable

class GLTFDataSuite extends munit.FunSuite {

  /** Builds a GLTF whose fields cover the surface the original smoke test touched: asset metadata, a scene referencing a node, named scene/node/mesh, and the two extension-name arrays.
    */
  private def buildGltf(): GLTF = {
    val gltf = new GLTF()

    val asset = new GLTFAsset()
    asset.version = Nullable("2.0")
    asset.generator = Nullable("SGE Test")
    asset.copyright = Nullable("2026 Test")
    gltf.asset = Nullable(asset)

    val scene = new GLTFScene()
    scene.name = Nullable("MainScene")
    scene.nodes = Nullable(ArrayBuffer(0))
    gltf.scenes = Nullable(ArrayBuffer(scene))
    gltf.scene = 0

    val node = new GLTFNode()
    node.name = Nullable("RootNode")
    gltf.nodes = Nullable(ArrayBuffer(node))

    val mesh = new GLTFMesh()
    mesh.name = Nullable("CubeMesh")
    gltf.meshes = Nullable(ArrayBuffer(mesh))

    gltf.extensionsUsed = Nullable(ArrayBuffer("KHR_materials_unlit", "KHR_lights_punctual"))
    gltf.extensionsRequired = Nullable(ArrayBuffer("KHR_materials_unlit"))

    gltf
  }

  /** Encodes a GLTF to a glTF document and decodes it back, returning the RE-DECODED model so every assertion observes the post-round-trip state — a dropped/nulled encoder field or a decoder that
    * cannot read it back is therefore visible as a missing value.
    */
  private def roundTrip(gltf: GLTF): GLTF =
    GLTFJsonParser.parse(writeToString(gltf))

  test("GLTF round-trip: asset metadata survives encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assert(Nullable.isDefined(gltf.asset), "asset must survive round-trip")
    val asset = gltf.asset.get
    assertEquals(asset.version.get, "2.0")
    assertEquals(asset.generator.get, "SGE Test")
    assertEquals(asset.copyright.get, "2026 Test")
    // minVersion was never set and must stay absent after the round-trip.
    assert(Nullable.isEmpty(asset.minVersion), "absent minVersion must stay absent")
  }

  test("GLTF round-trip: default scene index survives encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assertEquals(gltf.scene, 0)
  }

  test("GLTF round-trip: scenes + node references survive encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assert(Nullable.isDefined(gltf.scenes), "scenes must survive round-trip")
    val scenes = gltf.scenes.get
    assertEquals(scenes.size, 1)
    val scene = scenes(0)
    assertEquals(scene.name.get, "MainScene")
    assert(Nullable.isDefined(scene.nodes), "scene node references must survive round-trip")
    assertEquals(scene.nodes.get.size, 1)
    assertEquals(scene.nodes.get(0), 0)
  }

  test("GLTF round-trip: nodes survive encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assert(Nullable.isDefined(gltf.nodes), "nodes must survive round-trip")
    val nodes = gltf.nodes.get
    assertEquals(nodes.size, 1)
    assertEquals(nodes(0).name.get, "RootNode")
  }

  test("GLTF round-trip: meshes survive encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assert(Nullable.isDefined(gltf.meshes), "meshes must survive round-trip")
    val meshes = gltf.meshes.get
    assertEquals(meshes.size, 1)
    assertEquals(meshes(0).name.get, "CubeMesh")
  }

  test("GLTF round-trip: extensionsUsed / extensionsRequired survive encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assert(Nullable.isDefined(gltf.extensionsUsed), "extensionsUsed must survive round-trip")
    assert(Nullable.isDefined(gltf.extensionsRequired), "extensionsRequired must survive round-trip")
    val used     = gltf.extensionsUsed.get
    val required = gltf.extensionsRequired.get
    assertEquals(used.size, 2)
    assert(used.contains("KHR_materials_unlit"))
    assert(used.contains("KHR_lights_punctual"))
    assertEquals(required.size, 1)
    assert(required.contains("KHR_materials_unlit"))
  }

  test("GLTF round-trip: unset extensions / extras stay empty after encode -> decode") {
    val gltf = roundTrip(buildGltf())
    assert(Nullable.isEmpty(gltf.extensions), "unset extensions must stay empty")
    assert(Nullable.isEmpty(gltf.extras), "unset extras must stay empty")
  }
}
