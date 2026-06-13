/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-522 (EGL_SAMPLES never passed on Scala Native).
 *
 * Root cause being reproduced: GlOpsNative.createContext (sge/src/main/
 * scalanative/sge/platform/GlOpsNative.scala) builds the EGL config attribute
 * list appending ONLY EGL_SAMPLE_BUFFERS when samples > 0 — it never appends
 * the EGL_SAMPLES key/value pair (EGL_SAMPLES = 0x3031 is not even defined in
 * the Native file). As a result the requested MSAA sample count is silently
 * dropped on Scala Native. The fixed 19-int stackalloc could not even hold the
 * extra EGL_SAMPLES pair.
 *
 * The authoritative reference for correct behavior is the in-repo JVM sibling
 * GlOpsJvm.createContext (sge/src/main/scalajvm/sge/platform/GlOpsJvm.scala
 * ~line 177), which appends BOTH:
 *
 *   if (samples > 0) Array(EGL_SAMPLE_BUFFERS, 1, EGL_SAMPLES, samples)
 *   else Array.empty[Int]
 *
 * followed by Array(EGL_NONE). The Native path must mirror this exactly.
 *
 * The testable seam is GlOpsNative.buildConfigAttribs, which constructs the
 * EGL config attribute Array[Int] (key/value pairs terminated by EGL_NONE)
 * from the requested r/g/b/a/depth/stencil/samples — no live EGL display is
 * required. The reproducer extracted this helper from the inline construction
 * in createContext (behavior-preserving); the implementer fixes the helper to
 * also emit EGL_SAMPLES.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the JVM-sibling semantics, not the broken port's.
 */
package sge
package platform

class GlOpsNativeEglSamplesRedSuite extends munit.FunSuite {

  // EGL token values — must match the JVM sibling GlOpsJvm and the EGL headers.
  private val EGL_NONE           = 0x3038
  private val EGL_SAMPLE_BUFFERS = 0x3032
  private val EGL_SAMPLES        = 0x3031

  /** Returns the value paired with the given EGL key in the attribute list, or None if the key is absent. The list is a flat sequence of key/value pairs terminated by EGL_NONE.
    */
  private def valueOf(attribs: Array[Int], key: Int): Option[Int] = {
    val terminator = attribs.indexOf(EGL_NONE)
    val end        = if (terminator < 0) attribs.length else terminator
    var i          = 0
    var result     = Option.empty[Int]
    while (i + 1 < end) {
      if (attribs(i) == key) result = Some(attribs(i + 1))
      i += 2
    }
    result
  }

  private def build(samples: Int): Array[Int] =
    GlOpsNative.buildConfigAttribs(8, 8, 8, 8, 16, 0, samples)

  test("samples > 0 sets EGL_SAMPLE_BUFFERS = 1 (matches JVM GlOpsJvm)") {
    val attribs = build(4)
    assertEquals(
      valueOf(attribs, EGL_SAMPLE_BUFFERS),
      Some(1),
      "EGL_SAMPLE_BUFFERS must be 1 when samples > 0"
    )
  }

  test("samples > 0 sets EGL_SAMPLES = config.samples (RED: never passed on Native)") {
    val attribs = build(4)
    assertEquals(
      valueOf(attribs, EGL_SAMPLES),
      Some(4),
      "EGL_SAMPLES (0x3031) must equal the requested sample count — " +
        "GlOpsNative never appends this key, so MSAA is silently ignored (ISS-522)"
    )
  }

  test("samples == 0 emits neither EGL_SAMPLE_BUFFERS nor EGL_SAMPLES") {
    val attribs = build(0)
    assertEquals(
      valueOf(attribs, EGL_SAMPLE_BUFFERS),
      None,
      "EGL_SAMPLE_BUFFERS must be absent when samples == 0"
    )
    assertEquals(
      valueOf(attribs, EGL_SAMPLES),
      None,
      "EGL_SAMPLES must be absent when samples == 0 (no stray key)"
    )
  }

  test("attribute list is EGL_NONE-terminated and large enough to hold every pair") {
    val attribs = build(4)
    // The fix must size the array to fit the extra EGL_SAMPLES pair; assert the
    // list is properly terminated and that EGL_NONE is the LAST element (no
    // truncation that would drop trailing pairs).
    val terminator = attribs.indexOf(EGL_NONE)
    assert(terminator >= 0, "attribute list must contain the EGL_NONE terminator")
    assertEquals(
      terminator,
      attribs.length - 1,
      s"EGL_NONE must be the final element; got terminator at $terminator of ${attribs.length}"
    )
    // base = 16 ints, MSAA = EGL_SAMPLE_BUFFERS,1,EGL_SAMPLES,samples = 4 ints,
    // plus EGL_NONE = 21 ints total when samples > 0.
    assertEquals(
      attribs.length,
      21,
      "with samples > 0 the list must hold all 4 MSAA ints + EGL_NONE (16 + 4 + 1)"
    )
  }

  test("samples == 0 attribute list holds only the base pairs + EGL_NONE") {
    val attribs = build(0)
    assertEquals(
      attribs.length,
      17,
      "with samples == 0: 16 base ints + EGL_NONE"
    )
    assertEquals(attribs.last, EGL_NONE, "list must end with EGL_NONE")
  }
}
