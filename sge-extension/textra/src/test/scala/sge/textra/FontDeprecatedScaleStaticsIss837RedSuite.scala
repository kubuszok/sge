/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * COMPILE-RED annotation-presence suite for ISS-837 (textra wave 2026-07-17-F,
 * territory Y), against TextraTypist upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   @Deprecated public static float extractScale(long glyph)      — Font.java:8184-8185
 *   @Deprecated public static int   extractIntScale(long glyph)   — Font.java:8200-8201
 *   @Deprecated public static long  applyScale(long glyph, float) — Font.java:8216-8217
 *
 * Upstream marks all three compatibility shims @Deprecated. The port carries the
 * deprecation ONLY in scaladoc (`@deprecated` text at Font.scala:3860/3873/3887)
 * but DROPPED the Scala @deprecated ANNOTATION during wave-E, because under
 * `-deprecation -Werror` a real annotation made the frozen
 * FontScaleStaticsIss816RedSuite call sites fail to compile.
 *
 * This suite pins the annotation's ABSENCE as a compile error. textra builds
 * with `-Werror` + `-Wunused:...,nowarn` (SgePlugin.strictScalacOptions; the
 * android-only `-Wconf:cat=deprecation:s` silencer does NOT apply here). Each
 * call below is wrapped in `@nowarn("cat=deprecation")`. That @nowarn suppresses
 * a warning ONLY if the callee is actually @deprecated:
 *
 *   - NOW (annotation missing): the call raises no deprecation warning, so the
 *     @nowarn is redundant; `-Wunused:nowarn` reports it and `-Werror` turns that
 *     into a COMPILE ERROR — this file does not compile (RED), taking sge-textra
 *     test compilation (and its sibling suites) down with it, exactly like the
 *     ISS-816 compile-red precedent, so it is committed SEPARATELY as the LAST
 *     commit of the wave.
 *   - AFTER the fix (@deprecated restored on the three statics): each call raises
 *     a deprecation warning that the @nowarn suppresses, so the @nowarn is used
 *     and the file compiles (GREEN). The value assertions then pin the unchanged
 *     shim semantics (1f / 4 / glyph-unchanged).
 *
 * FROZEN-SUITE COUPLING (implementer + auditor MUST read): restoring the
 * annotation ALSO breaks FontScaleStaticsIss816RedSuite, whose call sites
 * (lines 38-40, 44-46, 51-53) invoke the same statics WITHOUT @nowarn. That
 * frozen suite must gain a deprecation suppression at those call sites — see the
 * reproducer report's ISS-837 frozen-suite-annotation spec. This is an annotation-
 * only exception to the "frozen suite is immutable" rule; the auditor must
 * forensically verify the suite's assertion logic stays byte-identical.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist's @Deprecated API surface, not the port's.
 */
package sge
package textra

import scala.annotation.nowarn

class FontDeprecatedScaleStaticsIss837RedSuite extends munit.FunSuite {

  test(
    "ISS-837: Font.extractScale/extractIntScale/applyScale carry the @deprecated annotation (upstream @Deprecated Font.java:8184/8200/8216)"
  ) {
    @nowarn("cat=deprecation") val scale    = Font.extractScale('A'.toLong)
    @nowarn("cat=deprecation") val intScale = Font.extractIntScale('A'.toLong)
    @nowarn("cat=deprecation") val applied  = Font.applyScale('A'.toLong, 2.5f)

    assertEquals(scale, 1f, "extractScale is a fixed 1f shim (upstream Font.java:8185-8187)")
    assertEquals(intScale, 4, "extractIntScale is a fixed 4 shim (upstream Font.java:8201-8203)")
    assertEquals(applied, 'A'.toLong, "applyScale returns glyph unchanged (upstream Font.java:8217-8219)")
  }
}
