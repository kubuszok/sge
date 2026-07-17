/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-846 (wave 2026-07-18-G, territory G5) — fresh-JVM harness for the ISS-782(c)
 * eager-codec-registration pin.
 *
 * Runs in a SPAWNED, pristine JVM (see GltfEagerCodecRegistrationIsolationSuite)
 * where `object GLTFCodecs` has never been initialised. It reproduces the exact
 * ISS-782(c) failure ordering:
 *
 *   1. Build a raw JSON AST for a KHR_lights_punctual node payload WITHOUT
 *      touching `object GLTFCodecs` — the `Json` codec lives in `sge.utils`,
 *      independent of the GLTF codec registry.
 *   2. Construct a `GLTFExtensions`. Its constructor is the FIRST thing to touch
 *      the GLTF codec machinery; it must eagerly force `object GLTFCodecs` to
 *      initialise (`ensureCodecsRegistered()`), which registers the lazy-parse
 *      decoders via `GLTFExtensions.registerDecoder`.
 *   3. Store the raw payload and immediately `get` it as the typed
 *      `KHRLightsPunctual.GLTFLightNode`.
 *
 * With the eager init present, step 3 returns the parsed object (light index 7).
 * Without it (ISS-782(c)), `GLTFExtensions.decode` finds no registered decoder
 * and silently returns empty — the exact silent-drop this pins against.
 *
 * The result ("PASS:" / "FAIL:") is written to args(0) and echoed; a non-PASS
 * result also exits with code 1 so the parent test fails on either signal.
 */
package sge
package gltf
package data

import sge.gltf.data.extensions.KHRLightsPunctual
import sge.utils.{ Json, given_JsonCodec_Json, readFromString }
import lowlevel.Nullable

object GltfEagerCodecRegistrationHarnessMain {

  def main(args: Array[String]): Unit = {
    val resultPath = args(0)
    val result: String =
      try {
        // (1) Raw AST built via the sge.utils Json codec — NOT via GLTFCodecs.
        val raw: Json = readFromString[Json]("""{"light":7}""")(using given_JsonCodec_Json)

        // (2) First touch of the GLTF codec machinery. The ctor must eagerly
        //     register the lazy-parse decoders (ISS-782c / ISS-622 pattern).
        val ext = new GLTFExtensions()

        // (3) Store raw + lazily decode into the requested typed extension.
        ext.setRaw(KHRLightsPunctual.EXT, raw)
        val decoded = ext.get(classOf[KHRLightsPunctual.GLTFLightNode], KHRLightsPunctual.EXT)

        if (Nullable.isEmpty(decoded))
          "FAIL: GLTFExtensions.get returned EMPTY in a fresh JVM — the GLTFExtensions " +
            "constructor did not eagerly register the GLTF codecs, so decode silently " +
            "dropped the stored KHR_lights_punctual extension (ISS-782c regression)"
        else {
          val node = decoded.get
          if (Nullable.isEmpty(node.light))
            "FAIL: decoded GLTFLightNode.light is empty (raw payload not parsed)"
          else if (node.light.get != 7)
            s"FAIL: decoded GLTFLightNode.light = ${node.light.get}, expected 7"
          else
            "PASS: eager codec registration parsed GLTFLightNode.light=7 in a fresh JVM"
        }
      } catch {
        case t: Throwable => s"FAIL: ${t.getClass.getName}: ${t.getMessage}"
      }

    java.nio.file.Files.write(java.nio.file.Paths.get(resultPath), result.getBytes("UTF-8"))
    if (result.startsWith("PASS:")) {
      System.out.println(result)
    } else {
      System.err.println(result)
      System.exit(1)
    }
  }
}
