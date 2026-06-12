/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Tests for the SaveData value codec round-trip, covering two gaps left by
 * the ISS-507+550 fix (auditor findings, bounce 1):
 *
 * FINDING 1 — BillboardParticleBatch.Config must survive the SaveData
 *   round-trip. Java serializes the Config through LibGDX reflective Json
 *   (original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/particles/
 *   batches/BillboardParticleBatch.java lines 654-668: save writes
 *   `new Config(useGPU, mode)` under "cfg", load reads `(Config)data.load("cfg")`).
 *   ParticleEffectLoader.save (ParticleEffectLoader.scala line 111) stores a
 *   Config on every batched save, so without a registered SaveValueCodec the
 *   save path itself throws ("No SaveData codec registered ...") in
 *   ResourceData.saveValueToJson. BillboardParticleBatch now registers the
 *   codec eagerly; these tests pin that the round-trip preserves useGPU and
 *   mode.
 *
 * FINDING 2 — saveValueFromJson must accept the wire format LibGDX Json
 *   actually writes: a tagged wrapper { "class": "java.lang.Integer",
 *   "value": 7 } for every boxed-primitive / String SaveData value
 *   (original-src/libgdx/gdx/src/com/badlogic/gdx/utils/Json.java lines
 *   506-515 emit the wrapper whenever the known type is null — always the
 *   case for ObjectMap<String,Object> SaveData values; writeType lines
 *   768-771 emit type.getName, e.g. "java.lang.Integer"). A genuine
 *   LibGDX/Flame .pfx (e.g. every MeshSpawnShapeValue "index") uses these
 *   java.lang.* tags; they must alias the short tags this codec writes.
 *
 * Bounce-2 findings (re-audit), closed here:
 *
 * FINDING A — codec registration must NOT be contingent on constructing a
 *   BillboardParticleBatch.Config. A load-only program (ships a .pfx authored
 *   elsewhere, never builds a Config) reaches the decode on the loader's LOAD
 *   path — ParticleEffectLoader.getDependencies -> ResourceData.fromJson ->
 *   saveDataFromJson -> saveValueFromJson — before any Config exists. The fix
 *   moves registration into `object BillboardParticleBatch`'s own initializer
 *   (BillboardParticleBatch.scala registerConfigCodec), so merely referencing
 *   the BillboardParticleBatch TYPE — which a batched load must do to obtain
 *   the batch it loads into — registers the codec. The test below decodes a
 *   Config-tagged value WITHOUT constructing a Config, triggering registration
 *   only through that production-path companion reference.
 *
 * FINDING B — saveValueFromJson must also accept the wire format LibGDX Json
 *   writes for a GENUINE, non-Serializable application object such as a
 *   Flame/LibGDX-authored Config: the class tag followed by the object's
 *   fields INLINE, with NO "value" key — e.g.
 *   {"class":"...BillboardParticleBatch$Config","useGPU":true,"mode":"ViewPoint"}.
 *   Json.java's default object branch (line 689 writeObjectStart(actualType,
 *   knownType) emitting only the class tag via writeType lines 768-771, then
 *   line 690 writeFields(value) appending the fields to the same object)
 *   produces this; the {class,value} wrapper at Json.java 506-515 is ONLY for
 *   boxed primitives / String, never a plain object. saveValueFromJson now
 *   strips the class tag from such a block and hands the remaining fields to
 *   the registered codec, whose decode accepts that exact field shape.
 */
package sge
package graphics
package g3d
package particles

import sge.graphics.g3d.particles.batches.BillboardParticleBatch
import sge.utils.{ Json, readFromString, writeToString }
import sge.utils.given

class SaveDataCodecSuite extends munit.FunSuite {

  /** ResourceData → JSON text → ResourceData, exactly what the loader save + getDependencies path does (writeToString of toJson, fromJson of the parsed text). Goes through saveValueToJson /
    * saveValueFromJson for every stored SaveData value.
    */
  private def textRoundTrip(rd: ResourceData[ParticleEffect]): ResourceData[ParticleEffect] = {
    val text = writeToString[Json](rd.toJson)
    ResourceData.fromJson[ParticleEffect](readFromString[Json](text))
  }

  // --- FINDING A: registration is not contingent on constructing a Config ----
  // Placed FIRST so that, within this suite's JVM run, no sibling test has
  // constructed a Config or referenced the BillboardParticleBatch type before
  // it: the ONLY registration trigger reaching saveValueFromJson here is this
  // test's own reference to the BillboardParticleBatch companion — the exact
  // production-load-path trigger (a batched load names the batch type). If the
  // main-source fix is reverted (registration contingent on Config), this test
  // sees no registered codec and the decode below throws — RED.

  test(
    "FINDING A: a Config-tagged value decodes WITHOUT ever constructing a Config (registration fires from the batch type)"
  ) {
    // Reproduces the load-only process: the codec must already be registered
    // when saveValueFromJson runs, even though no Config has been constructed.
    // The ONLY registration trigger this test fires is the SAME call
    // `class BillboardParticleBatch`'s constructor makes —
    // `BillboardParticleBatch.ensureCodecRegistered()` — which forces
    // `object BillboardParticleBatch`'s initializer (registerConfigCodec). A
    // batched load constructs a BillboardParticleBatch to obtain its batch, so
    // this is exactly the production-load-path trigger; we exercise it directly
    // here without GL/Sge and without ever constructing a Config (the trigger
    // the old, buggy fix wrongly depended on). Merely naming the nested
    // Config / AlignMode types does NOT force the module initializer — only a
    // member call like this one does.
    BillboardParticleBatch.ensureCodecRegistered()

    // The legacy LibGDX/Flame inline-field block (Finding B shape): a class tag
    // plus the object's fields inline, no "value" key. This is what
    // saveDataFromJson hands to saveValueFromJson on the load path.
    val text =
      """{"class":"com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch$Config",""" +
        """"useGPU":true,"mode":"ViewPoint"}"""
    val decoded = ResourceData.saveValueFromJson(readFromString[Json](text))
    assert(
      decoded.isInstanceOf[BillboardParticleBatch.Config],
      s"Config-tagged value must decode without a Config in play; got: $decoded"
    )
    val cfg = decoded.asInstanceOf[BillboardParticleBatch.Config]
    assertEquals(cfg.useGPU, true)
    assertEquals(cfg.mode, BillboardParticleBatch.AlignMode.ViewPoint)
  }

  // --- FINDING 1: BillboardParticleBatch.Config round-trip -------------------

  test("FINDING 1: Config (useGPU=true, ViewPoint) round-trips through saveValueToJson/FromJson") {
    // Force the companion-init registration the same way the batch constructor does.
    BillboardParticleBatch.ensureCodecRegistered()
    val cfg = new BillboardParticleBatch.Config(true, BillboardParticleBatch.AlignMode.ViewPoint)
    // saveValueToJson must find a registered codec for Config; without the
    // registration forced by ensureCodecRegistered it throws
    // SgeError.InvalidInput("No SaveData codec registered for value of type:
    // ...BillboardParticleBatch$Config").
    val encoded = ResourceData.saveValueToJson(cfg)
    val decoded = ResourceData.saveValueFromJson(encoded)
    assert(decoded.isInstanceOf[BillboardParticleBatch.Config], s"decoded value must be a Config; got: $decoded")
    val back = decoded.asInstanceOf[BillboardParticleBatch.Config]
    assertEquals(back.useGPU, true, "useGPU must survive the round-trip")
    assertEquals(back.mode, BillboardParticleBatch.AlignMode.ViewPoint, "mode must survive the round-trip")
  }

  test("FINDING 1: Config (useGPU=false, Screen) round-trips through a full ResourceData text round-trip") {
    BillboardParticleBatch.ensureCodecRegistered()
    val rd = ResourceData[ParticleEffect]()
    // Mirrors BillboardParticleBatch.save: data.save("cfg", new Config(...)).
    val sd = rd.createSaveData("billboardBatch")
    sd.save("cfg", new BillboardParticleBatch.Config(false, BillboardParticleBatch.AlignMode.Screen))

    val rd2 = textRoundTrip(rd)
    val sd2 = rd2.getSaveData("billboardBatch").getOrElse(fail("billboardBatch SaveData missing after round-trip"))
    val cfg = sd2.load[BillboardParticleBatch.Config]("cfg").getOrElse(fail("cfg value missing after round-trip"))
    assertEquals(cfg.useGPU, false, "useGPU=false must survive the full round-trip")
    assertEquals(cfg.mode, BillboardParticleBatch.AlignMode.Screen, "mode=Screen must survive the full round-trip")
  }

  // --- FINDING B: genuine LibGDX inline-field wire format ---------------------

  test("FINDING B: the EXACT inline-field Config block LibGDX Json writes (class tag + inline fields, no \"value\") decodes") {
    BillboardParticleBatch.ensureCodecRegistered()
    // The byte-exact wire shape from the finding: Json.java's default object
    // branch (line 689 writeObjectStart writing only the class tag, line 690
    // writeFields appending useGPU/mode inline). No "value" key — that wrapper
    // (Json.java 506-515) is only for boxed primitives / String.
    val text =
      """{"class":"com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch$Config",""" +
        """"useGPU":true,"mode":"ViewPoint"}"""
    val decoded = ResourceData.saveValueFromJson(readFromString[Json](text))
    assert(decoded.isInstanceOf[BillboardParticleBatch.Config], s"inline-field Config must decode; got: $decoded")
    val cfg = decoded.asInstanceOf[BillboardParticleBatch.Config]
    assertEquals(cfg.useGPU, true, "useGPU must decode from the inline field")
    assertEquals(cfg.mode, BillboardParticleBatch.AlignMode.ViewPoint, "mode must decode from the inline field")
  }

  test("FINDING B: the SGE class name with inline fields also decodes (dual-name registration)") {
    // The same inline-field shape under the SGE class name, proving the codec
    // is registered under both names and the inline-field branch is name-agnostic.
    // The SGE tag is the runtime class name the SAVE path writes
    // (saveValueToJson uses other.getClass.getName), derived here so the test is
    // not tied to a single platform's getName spelling.
    BillboardParticleBatch.ensureCodecRegistered()
    val sgeName = classOf[BillboardParticleBatch.Config].getName
    val text    =
      "{\"class\":\"" + sgeName + "\"," +
        """"useGPU":false,"mode":"Screen"}"""
    val decoded = ResourceData.saveValueFromJson(readFromString[Json](text))
    assert(decoded.isInstanceOf[BillboardParticleBatch.Config], s"SGE-named inline-field Config must decode; got: $decoded")
    val cfg = decoded.asInstanceOf[BillboardParticleBatch.Config]
    assertEquals(cfg.useGPU, false)
    assertEquals(cfg.mode, BillboardParticleBatch.AlignMode.Screen)
  }

  // --- FINDING 2: LibGDX wire-format tags ------------------------------------

  test("FINDING 2: java.lang.Integer-tagged SaveData value restores as an Int") {
    // The exact wrapper LibGDX Json writes for a boxed Integer SaveData value
    // (Json.java 506-515 + writeType 768-771). Restored as an Int via load[Int]
    // (the MeshSpawnShapeValue.load access pattern).
    val text     = """{"class":"java.lang.Integer","value":7}"""
    val restored = ResourceData.saveValueFromJson(readFromString[Json](text))
    assert(restored.isInstanceOf[java.lang.Integer], s"java.lang.Integer tag must restore an Integer; got: $restored")
    assertEquals(restored.asInstanceOf[java.lang.Integer].intValue, 7)
  }

  test("FINDING 2: hand-written libgdx-format SaveData block (java.lang.Integer + java.lang.String) loads") {
    // A SaveData "data" map exactly as LibGDX Json serializes it: each value is
    // a tagged wrapper whose class is the java.lang.* boxed name.
    val text =
      """{"assets":[],"data":[{"data":{""" +
        """"index":{"class":"java.lang.Integer","value":42},""" +
        """"name":{"class":"java.lang.String","value":"flame"}""" +
        """},"indices":[]}],"unique":{}}"""
    val rd = ResourceData.fromJson[ParticleEffect](readFromString[Json](text))
    val sd = rd.saveData
    assertEquals(sd.load[Int]("index").getOrElse(-1), 42, "java.lang.Integer value must load as an Int")
    assertEquals(sd.load[String]("name").getOrElse(""), "flame", "java.lang.String value must load as a String")
  }

  test("FINDING 2: java.lang.Long / Float / Double / Boolean tags restore their exact boxed types") {
    val longV = ResourceData.saveValueFromJson(readFromString[Json]("""{"class":"java.lang.Long","value":9000000000}"""))
    assert(longV.isInstanceOf[java.lang.Long], s"got: $longV")
    assertEquals(longV.asInstanceOf[java.lang.Long].longValue, 9000000000L)

    val floatV = ResourceData.saveValueFromJson(readFromString[Json]("""{"class":"java.lang.Float","value":1.5}"""))
    assert(floatV.isInstanceOf[java.lang.Float], s"got: $floatV")
    assertEquals(floatV.asInstanceOf[java.lang.Float].floatValue, 1.5f)

    val doubleV = ResourceData.saveValueFromJson(readFromString[Json]("""{"class":"java.lang.Double","value":2.25}"""))
    assert(doubleV.isInstanceOf[java.lang.Double], s"got: $doubleV")
    assertEquals(doubleV.asInstanceOf[java.lang.Double].doubleValue, 2.25)

    val boolV = ResourceData.saveValueFromJson(readFromString[Json]("""{"class":"java.lang.Boolean","value":true}"""))
    assert(boolV.isInstanceOf[java.lang.Boolean], s"got: $boolV")
    assertEquals(boolV.asInstanceOf[java.lang.Boolean].booleanValue, true)
  }
}
