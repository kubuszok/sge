/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-625 — GLTFExporterJson.writeExtensions double-gets the same
 * extension name under two incompatible types.
 *
 * Problem (GLTFExporterJson.scala:733-744): for KHR_lights_punctual the exporter
 * calls
 *   ext.get(classOf[GLTFLights],    KHRLightsPunctual.EXT).foreach(writeLights)
 *   ext.get(classOf[GLTFLightNode], KHRLightsPunctual.EXT).foreach(... light ...)
 * back-to-back on the SAME GLTFExtensions. GLTFExtensions.get is an unchecked
 * `asInstanceOf[T]` (GLTFExtensions.scala:41-61), so when the extension holds a
 * node light (GLTFLightNode, as produced by GLTFLightExporter and stored via
 * GLTFExtensions.set), the FIRST get returns that GLTFLightNode cast unchecked to
 * GLTFLights; writeLights then reads `lights.lights` (GLTFExporterJson.scala:829)
 * and the JVM CHECKCAST to GLTFLights throws ClassCastException. Exporting any
 * document whose node carries a KHR_lights_punctual light therefore crashes.
 *
 * Original behavior (original-src/gdx-gltf, never fetched): the node light is a
 * GLTFLightNode carrying a `light` index; the exporter must emit
 * `"KHR_lights_punctual": { "light": N }` for that node (the GLTFLightNode branch
 * at GLTFExporterJson.scala:737-744 already knows how). The fix must type-check
 * (pattern-match) the stored object instead of double-getting under both types.
 *
 * Platform scope: shared (src/test/scala) — the exporter is pure StringBuilder
 * code over the data model; no GL/filesystem.
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer
import sge.gltf.data.GLTF
import sge.gltf.data.GLTFExtensions
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.data.scene.GLTFNode
import lowlevel.Nullable

class GltfExporterNodeLightRedSuite extends munit.FunSuite {

  /** A one-node glTF whose node carries a KHR_lights_punctual node light (index 0). */
  private def gltfWithNodeLight(): GLTF = {
    val lightNode = new KHRLightsPunctual.GLTFLightNode()
    lightNode.light = Nullable(0)

    val ext = new GLTFExtensions()
    // Mirrors GLTFLightExporter storing the per-node light typed under the extension name.
    ext.set(KHRLightsPunctual.EXT, lightNode)

    val node = new GLTFNode()
    node.name = Nullable("LampNode")
    node.extensions = Nullable(ext)

    val gltf = new GLTF()
    gltf.nodes = Nullable(ArrayBuffer(node))
    gltf
  }

  test(
    "ISS-625: exporting a node light emits KHR_lights_punctual.light, not a ClassCastException (GLTFExporterJson.scala:733-744)"
  ) {
    val json = GLTFExporterJson.writeGltf(gltfWithNodeLight())
    assert(json.contains(KHRLightsPunctual.EXT), s"extension name dropped from export:\n$json")
    assert(json.contains("\"light\""), s"node light index dropped from export:\n$json")
  }

  // GREEN control: a node with NO extensions exports cleanly — proves the fixture and the
  // node/extension writer path work, isolating the failure above to the double-get.
  test("ISS-625 control: a plain node exports without the extensions block (GREEN)") {
    val node = new GLTFNode()
    node.name = Nullable("Plain")
    val gltf = new GLTF()
    gltf.nodes = Nullable(ArrayBuffer(node))
    val json = GLTFExporterJson.writeGltf(gltf)
    assert(json.contains("\"Plain\""))
    assert(!json.contains("extensions"))
  }
}
