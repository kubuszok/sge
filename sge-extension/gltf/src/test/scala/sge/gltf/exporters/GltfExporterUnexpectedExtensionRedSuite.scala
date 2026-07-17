/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-820 (wave 2026-07-17-F, territory W) — GLTFExporterJson.writeExtensions
 * silently drops a KHR_lights_punctual entry whose stored object is neither GLTFLights nor
 * GLTFLightNode.
 *
 * Defect (GLTFExporterJson.scala:743-755): the exporter does
 *   ext.get(classOf[AnyRef], KHRLightsPunctual.EXT).foreach {
 *     case lights: GLTFLights   => ...
 *     case ln: GLTFLightNode    => ...
 *     case _ => ()                       // <-- silent skip
 *   }
 * A user who mis-populates the extension via GLTFExtensions.set(EXT, wrongTypedObject) gets the
 * extension SILENTLY OMITTED from the exported document. The parallel jsoniter codec path fails
 * loudly for exactly this situation — GLTFCodecs.gltfExtensionsCodec.encodeValue throws
 * GLTFUnsupportedException for any typed entry with no registered encoder (GLTFCodecs.scala:350-361),
 * and the original libGDX reflective write (net/mgsx/gltf/data/GLTFExtensions.java:16-21) would
 * serialize the object. The two SGE serialization paths must agree: align GLTFExporterJson on the
 * same loud failure (throw GLTFUnsupportedException) instead of dropping data.
 *
 * Reachable only via user error through GLTFExtensions.set, but a silent drop corrupts the exported
 * glTF (the declared extension vanishes) with no diagnostic — worse than a crash.
 *
 * Platform scope: shared (src/test/scala) — GLTFExporterJson is pure StringBuilder code over the
 * data model; no GL/filesystem dependency.
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer
import sge.gltf.data.{ GLTF, GLTFExtensions }
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.data.scene.GLTFNode
import sge.gltf.loaders.exceptions.GLTFUnsupportedException
import lowlevel.Nullable

class GltfExporterUnexpectedExtensionRedSuite extends munit.FunSuite {

  /** Root GLTF whose KHR_lights_punctual extension holds a GLTFLight (a SINGLE light) instead of the expected GLTFLights (the lights array) or GLTFLightNode (a light index) — a plausible user mistake
    * through GLTFExtensions.set. GLTFExporterJson's writeExtensions pattern-match has no arm for GLTFLight, so it hits `case _ => ()` today.
    */
  private def gltfWithMistypedLightExtension(): GLTF = {
    val ext = new GLTFExtensions()
    ext.set(KHRLightsPunctual.EXT, new KHRLightsPunctual.GLTFLight())

    val gltf = new GLTF()
    gltf.extensions = Nullable(ext)
    gltf
  }

  // GREEN control: the loud codec path already fails for a mistyped entry, proving the intended
  // contract (extensions with an unencodable typed object must not be silently emitted). This
  // isolates the divergence to the GLTFExporterJson path exercised by the red test below.
  test("ISS-820 control: the jsoniter codec path throws for a mistyped extension entry (GREEN, GLTFCodecs.scala:350-361)") {
    val ext = new GLTFExtensions()
    ext.set(KHRLightsPunctual.EXT, new KHRLightsPunctual.GLTFLight())
    intercept[GLTFUnsupportedException] {
      import sge.utils.given
      import sge.gltf.data.GLTFCodecs.given
      sge.utils.writeToString(ext)
    }
  }

  test(
    "ISS-820: exporting a mistyped KHR_lights_punctual entry must fail loudly, not silently drop it (GLTFExporterJson.scala:743-755)"
  ) {
    // Today writeExtensions matches `case _ => ()` and emits an empty extensions object, so no
    // exception is thrown and the intercept fails — the red.
    intercept[GLTFUnsupportedException] {
      GLTFExporterJson.writeGltf(gltfWithMistypedLightExtension())
    }
  }

  // GREEN control: a root with NO extensions exports cleanly — proves the fixture/writer path.
  test("ISS-820 control: a root with no extensions exports without an extensions block (GREEN)") {
    val node = new GLTFNode()
    node.name = Nullable("Plain")
    val gltf = new GLTF()
    gltf.nodes = Nullable(ArrayBuffer(node))
    val json = GLTFExporterJson.writeGltf(gltf)
    assert(json.contains("\"Plain\""))
    assert(!json.contains("extensions"))
  }
}
