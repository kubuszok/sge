/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-507 (3D particle effect save AND load are dead) and
 * ISS-550 (ResourceData SaveData round-trip corrupts non-string values).
 * The two issues are fixed together; this suite pins both.
 *
 * ISS-507 — expected behaviour is the original Java contract:
 *   - SAVE: ResourceData.write (original-src/libgdx/gdx/src/com/badlogic/gdx/
 *     graphics/g3d/particles/ResourceData.java lines 211-217) serializes the
 *     resource itself — `json.writeValue("resource", resource, null)` — i.e.
 *     the full effect definition (controllers with their emitter, influencers
 *     and renderer). The port's ResourceData.toJson (ResourceData.scala lines
 *     77-103) writes ONLY assets/data/unique, and ParticleEffectLoader.save
 *     (ParticleEffectLoader.scala line 115) serializes that AST verbatim, so
 *     the saved file contains no controller name, no emitter, nothing that
 *     could ever reconstruct the effect — RED.
 *   - LOAD: Java loadSync (ParticleEffectLoader.java lines 117-142) returns
 *     `effectData.resource`, restored by ResourceData.read (line 232:
 *     `resource = json.readValue("resource", null, jsonData)`). The port's
 *     ResourceData.fromJson (ResourceData.scala lines 338-380) documents "The
 *     resource field is NOT populated - that must be handled by the caller"
 *     and NO caller does, so loadSync (ParticleEffectLoader.scala line 161)
 *     always throws `RuntimeException("ResourceData has no resource for ...")`
 *     for any effect saved by the loader itself — RED.
 *     ParticleEffectCodecs.scala (2,124 lines of jsoniter codecs that CAN
 *     encode/decode every controller component) has zero call sites.
 *
 * ISS-550 — expected behaviour is the original Java contract: SaveData
 * write/read (ResourceData.java lines 96-105) go through LibGDX Json typed
 * values, so a stored java.lang.Integer is restored as an Integer and a
 * stored Array<IntArray> is restored identically typed. The port instead:
 *   - serializes any non-primitive value via toString — saveDataToJson
 *     (ResourceData.scala line 227: `case other =>
 *     Json.fromString(other.toString)`) — corrupting e.g. the nested
 *     DynamicArray[DynamicArray[Int]] "indices" payload written by
 *     ParticleControllerInfluencer.save (ParticleControllerInfluencer.scala
 *     line 136) — RED;
 *   - restores every integral JSON number as java.lang.Long — saveDataFromJson
 *     (ResourceData.scala lines 304-310) — so the MeshSpawnShapeValue.load
 *     access pattern `saveData.load[Int]("index")` (MeshSpawnShapeValue.scala
 *     line 88) unboxes a Long as an Int and dies with ClassCastException — RED.
 *
 * Headless fixture: the whole effect is data + math, no GL is touched.
 * RegularEmitter, ColorInfluencer.Single, ScaleInfluencer and SpawnInfluencer
 * (default PointSpawnShapeValue) are plain value holders; BillboardRenderer's
 * no-arg constructor only allocates a BillboardControllerRenderData (channel
 * references stay uninitialized because init() is never called); batches are
 * deliberately absent (Nullable.empty) so no shader/GL path is reachable.
 * ParticleEffect/ParticleController/AssetManager/ParticleEffectLoader only
 * need (using Sge) — provided by SgeTestFixture.testSge(). The minimal effect
 * stores no assets, so the AssetManager (defaultLoaders = false) and the
 * throwing FileHandleResolver are never consulted. File I/O is an in-memory
 * FileHandleStream subclass per the ObjLoaderFanRedSuite precedent: it
 * captures FileHandle.writeString through an overridden writer() and serves
 * the captured bytes back through read().
 *
 * Platform placement: JVM and Native copies only (byte-identical). The suite
 * exercises FileHandle.writeString/read() whose base-class bodies reference
 * java.io.FileOutputStream / OutputStreamWriter / FileInputStream — present
 * on JVM and Scala Native (javalib) but rejected by the Scala.js test linker
 * (same reason ObjLoaderFanRedSuite has no JS copy).
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g3d
package particles

import java.io.{ ByteArrayInputStream, InputStream, StringWriter, Writer }
import java.nio.charset.StandardCharsets

import sge.assets.AssetManager
import sge.assets.loaders.FileHandleResolver
import sge.files.{ FileHandle, FileHandleStream }
import sge.graphics.g3d.particles.ParticleEffectLoader.{ ParticleEffectLoadParameter, ParticleEffectSaveParameter }
import sge.graphics.g3d.particles.batches.ParticleBatch
import sge.graphics.g3d.particles.emitters.RegularEmitter
import sge.graphics.g3d.particles.influencers.{ ColorInfluencer, ScaleInfluencer, SpawnInfluencer }
import sge.graphics.g3d.particles.renderers.BillboardRenderer
import lowlevel.Nullable
import lowlevel.util.DynamicArray
import sge.utils.{ Json, readFromString, writeToString }
import sge.utils.given

class ParticleEffectIoRedSuite extends munit.FunSuite {

  // --- Headless fixture ------------------------------------------------------

  /** In-memory "file": writeString goes through writer() (FileHandles.scala line 356), so capturing the Writer is enough to observe the saved JSON; read() serves it back for the loader's readJson. */
  final private class MemoryFileHandle(path: String) extends FileHandleStream(path) {

    var content: String = ""

    override def read(): InputStream =
      new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))

    override def writer(append: Boolean, charset: Nullable[String]): Writer =
      new StringWriter() {
        override def close(): Unit = {
          content = this.toString
          super.close()
        }
      }
  }

  /** Never consulted: the minimal effect saves no assets, so getDependencies' resolve() loop never runs and the AssetManager is never queried. */
  private val resolver: FileHandleResolver = new FileHandleResolver {
    def resolve(fileName: String): FileHandle = throw new UnsupportedOperationException
  }

  /** One controller: RegularEmitter + 3 asset-free influencers + BillboardRenderer. All construction is data-only — no GL, no batches, no textures. */
  private def makeEffect()(using Sge): ParticleEffect = {
    val emitter = new RegularEmitter()
    emitter.minParticleCount = 11
    emitter.maxParticleCount = 23
    emitter.durationValue.setLow(3000f)
    emitter.continuous = false
    val controller = ParticleController(
      "iss507-emitter-ctrl",
      emitter,
      new BillboardRenderer(),
      new ColorInfluencer.Single(),
      new ScaleInfluencer(),
      new SpawnInfluencer()
    )
    ParticleEffect(controller)
  }

  /** Runs the documented save path (ParticleEffectLoader.save) and returns the serialized JSON text. */
  private def saveToString(effect: ParticleEffect, file: MemoryFileHandle)(using Sge): String = {
    val loader  = new ParticleEffectLoader(resolver)
    val manager = AssetManager(resolver, defaultLoaders = false)
    loader.save(effect, new ParticleEffectSaveParameter(file, manager))
    file.content
  }

  /** ResourceData → JSON text → ResourceData, exactly what save + getDependencies do (writeToString at ParticleEffectLoader.scala line 119, readJson + fromJson at lines 69-70). */
  private def textRoundTrip(rd: ResourceData[ParticleEffect]): ResourceData[ParticleEffect] = {
    val text = writeToString[Json](rd.toJson)
    ResourceData.fromJson[ParticleEffect](readFromString[Json](text))
  }

  // --- ISS-507 ---------------------------------------------------------------

  test("ISS-507: save writes the effect definition (controller name + emitter fields), not just assets/data/unique") {
    given Sge = SgeTestFixture.testSge()

    val serialized = saveToString(makeEffect(), new MemoryFileHandle("iss507-save.pfx"))
    // Java ResourceData.write line 216 serializes the resource: any faithful
    // serialization of the effect must mention the controller's name and the
    // emitter's particle-count fields (ResourceData.java 211-217; the port's
    // own ParticleEffectCodecs writeEmitterFields would emit
    // "minParticleCount" too). Current output is only
    // {"assets":[],"data":[],"unique":{}} — both assertions are RED.
    assert(
      serialized.contains("iss507-emitter-ctrl"),
      s"saved JSON must contain the effect definition (controller name); got: $serialized"
    )
    assert(
      serialized.contains("minParticleCount"),
      s"saved JSON must contain the emitter configuration; got: $serialized"
    )
  }

  test("ISS-507: save → loadSync round-trip restores controller count and emitter config") {
    given Sge = SgeTestFixture.testSge()

    val fileName = "iss507-roundtrip.pfx"
    val file     = new MemoryFileHandle(fileName)
    val loader   = new ParticleEffectLoader(resolver)
    val manager  = AssetManager(resolver, defaultLoaders = false)
    loader.save(makeEffect(), new ParticleEffectSaveParameter(file, manager))

    val loadParam = new ParticleEffectLoadParameter(Nullable.empty[DynamicArray[ParticleBatch[?]]])
    // getDependencies parses the file and caches the ResourceData (the
    // loader's documented sync contract: getDependencies then loadSync).
    val deps = loader.getDependencies(fileName, file, loadParam)
    assertEquals(deps.size, 0, "minimal effect must have no asset dependencies")

    // Java loadSync returns effectData.resource (ParticleEffectLoader.java
    // 132-141). The port throws RuntimeException("ResourceData has no
    // resource for iss507-roundtrip.pfx") at ParticleEffectLoader.scala:161
    // because nothing ever populates ResourceData.resource — RED.
    val loaded = loader.loadSync(manager, fileName, file, loadParam)

    assertEquals(loaded.controllers.size, 1, "round-trip must preserve the controller count")
    val controller = loaded.controllers(0)
    assertEquals(controller.name, "iss507-emitter-ctrl", "round-trip must preserve the controller name")
    assert(
      controller.emitter.isInstanceOf[RegularEmitter],
      s"round-trip must preserve the emitter type; got: ${controller.emitter}"
    )
    val emitter = controller.emitter.asInstanceOf[RegularEmitter]
    assertEquals(emitter.minParticleCount, 11, "round-trip must preserve minParticleCount")
    assertEquals(emitter.maxParticleCount, 23, "round-trip must preserve maxParticleCount")
    assertEquals(emitter.durationValue.lowMin, 3000f, "round-trip must preserve the emitter duration")
    assert(!emitter.continuous, "round-trip must preserve the continuous flag")
    assertEquals(controller.influencers.size, 3, "round-trip must preserve the influencer count")
  }

  // --- ISS-550 ---------------------------------------------------------------

  test("ISS-550: SaveData Int value (MeshSpawnShapeValue-style \"index\") round-trips as an Int") {
    val rd = ResourceData[ParticleEffect]()
    val sd = rd.createSaveData()
    // MeshSpawnShapeValue.save stores Integer.valueOf(...) under "index"
    // (MeshSpawnShapeValue.scala line 80).
    sd.save("index", Integer.valueOf(7))

    val sd2 = textRoundTrip(rd).saveData
    // saveDataFromJson restores integral numbers as java.lang.Long
    // (ResourceData.scala 304-310), so the unboxing below throws
    // ClassCastException (Long cannot be cast to Integer) — exactly the
    // MeshSpawnShapeValue.load crash (MeshSpawnShapeValue.scala line 88) — RED.
    val restored: Int = sd2.load[Int]("index").getOrElse(-1)
    assertEquals(restored, 7, "Integer SaveData value must survive the round-trip as an Int")
  }

  test(
    "ISS-550: SaveData nested int-array payload (ParticleControllerInfluencer-style \"indices\") round-trips structurally"
  ) {
    val rd = ResourceData[ParticleEffect]()
    val sd = rd.createSaveData()
    // ParticleControllerInfluencer.save stores DynamicArray[DynamicArray[Int]]
    // under "indices" (ParticleControllerInfluencer.scala line 136); its load
    // reads it back identically typed (line 141). Java restores Array<IntArray>
    // through typed Json values (ResourceData.java 96-105).
    val first = DynamicArray[Int]()
    first.add(0)
    first.add(2)
    val second = DynamicArray[Int]()
    second.add(1)
    val nested = DynamicArray[DynamicArray[Int]]()
    nested.add(first)
    nested.add(second)
    sd.save("indices", nested.asInstanceOf[AnyRef])

    val sd2 = textRoundTrip(rd).saveData
    // saveDataToJson serialized the payload via toString (ResourceData.scala
    // line 227), so what comes back is a useless String — the typed access
    // below fails with ClassCastException instead of returning the arrays — RED.
    val restored: Nullable[DynamicArray[DynamicArray[Int]]] = sd2.load("indices")
    val arrays = restored.getOrElse(fail("\"indices\" payload missing after round-trip"))
    assertEquals(arrays.size, 2, "outer array size must survive the round-trip")
    assertEquals(arrays(0).size, 2)
    assertEquals(arrays(0)(0), 0)
    assertEquals(arrays(0)(1), 2)
    assertEquals(arrays(1).size, 1)
    assertEquals(arrays(1)(0), 1)
  }

  // --- Control (green at red commit) -----------------------------------------

  test("ISS-507 control (green at red commit): ResourceData.fromJson still parses a minimal assets-only file") {
    // Pins the half of the pipeline that already works: assets / data /
    // unique parsing (ResourceData.scala 341-380). Must stay green after the fix.
    val text =
      """{"assets":[{"filename":"smoke.png","type":"sge.graphics.Texture"}],"data":[{"data":{"key":"flame"},"indices":[0]}],"unique":{}}"""
    val rd = ResourceData.fromJson[ParticleEffect](readFromString[Json](text))
    assertEquals(rd.assets.size, 1, "one shared asset must be parsed")
    assertEquals(rd.assets(0).filename, "smoke.png")
    assertEquals(rd.assets(0).`type`: Class[?], classOf[sge.graphics.Texture]: Class[?])
    val sd = rd.saveData
    assertEquals(sd.load[String]("key").getOrElse(""), "flame", "String SaveData values already round-trip")
    val descriptor = sd.loadAsset()
    assertEquals(descriptor.map(_.fileName).getOrElse(""), "smoke.png", "asset indices must resolve to descriptors")
  }
}
