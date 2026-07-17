/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * COMPILE-RED for ISS-820 (wave 2026-07-17-F, territory W) — sge.utils.JsonCodecs re-exports
 * jsoniter-scala's WriterConfig but NOT the symmetric ReaderConfig.
 *
 * Evidence: sge/src/main/scala/sge/utils/JsonCodecs.scala:59-61 re-exports WriterConfig
 * (type + companion val) so consumers never import `com.github.plokhotnyuk.jsoniter_scala.core`
 * directly. There is no matching ReaderConfig re-export, so GLTFJsonParser (the ISS-626 fix) had
 * to import jsoniter's ReaderConfig directly, breaking the re-export convention documented in
 * JsonCodecs.scala:8-9 ("re-exports jsoniter-scala types so consumers don't import plokhotnyuk
 * directly"). Add the symmetric `type ReaderConfig` + `val ReaderConfig` re-export.
 *
 * This is a COMPILE-RED: `sge.utils.ReaderConfig` does not resolve today, so this file fails to
 * compile. It is committed LAST in the wave-F territory-W batch (after all runnable red suites) so
 * earlier suites stay runnable until the implementer lands the re-export.
 *
 * OUT-OF-TERRITORY FIX: the one-line re-export lives in sge core
 * (sge/src/main/scala/sge/utils/JsonCodecs.scala), OUTSIDE the gltf reproducer territory. Flagged
 * as a single-file grant request in the reproducer report — the reproducer did NOT edit it.
 */
package sge
package gltf

class JsonReaderConfigReexportRedSuite extends munit.FunSuite {

  test("ISS-820: sge.utils re-exports ReaderConfig symmetrically with WriterConfig") {
    // GREEN baseline — WriterConfig re-export already exists (JsonCodecs.scala:59-61).
    val w: sge.utils.WriterConfig = sge.utils.WriterConfig
    assert(w != null)

    // RED — the symmetric ReaderConfig re-export is missing; this reference does not resolve.
    val r: sge.utils.ReaderConfig = sge.utils.ReaderConfig
    assert(r != null)
  }
}
