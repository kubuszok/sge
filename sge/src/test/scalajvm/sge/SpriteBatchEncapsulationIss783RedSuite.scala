/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Compile-shape red test — ISS-783 (minor), wave 2026-07-17-F, territory X.
 * Reproducer-authored: MUST NOT be modified by the fixer.
 *
 * DEFECT. SpriteBatch exposes `idx`, `lastTexture`, and `drawing` as PUBLIC
 * `var`s (SpriteBatch.scala lines 60, 61, 65). In the original
 * com/badlogic/gdx/graphics/g2d/SpriteBatch.java (original-src/libgdx) these are
 * PACKAGE-PRIVATE fields (no access modifier), lines 53-57:
 *   int idx = 0;
 *   Texture lastTexture = null;
 *   boolean drawing = false;
 * and the port's own PolygonSpriteBatch keeps them `private` (PolygonSpriteBatch
 * lines 64-68). The faithful Scala visibility is `private[g2d]` (package-private
 * to the g2d package), which matches Java's package-private and keeps in-package
 * readers working.
 *
 * This suite lives in `package sge` (OUTSIDE g2d) and asserts, via
 * scala.compiletime.testing.typeCheckErrors, that external code cannot ASSIGN
 * these fields. Today (public var) every assignment compiles, so the
 * "must-not-compile" assertions fail — this is the red. After the fields become
 * `private[g2d]`, external assignment stops compiling and the assertions pass.
 *
 * INTERNAL/TEST USERS that legitimately need `private[g2d]` (not fully private):
 *   - SpriteBatchVertexGeometryISS561Suite (package sge.graphics.g2d) READS
 *     batch.idx and batch.vertices — these must keep compiling, so the fix must
 *     be `private[g2d]`, NOT `private`.
 *   - SpriteCacheRedSuite (package sge.graphics.g2d) READS a DIFFERENT class's
 *     `drawing` (SpriteCache.drawing), unaffected.
 * No production or test code ASSIGNS SpriteBatch.idx/lastTexture/drawing
 * externally (verified across sge/src), so tightening to private[g2d] is safe.
 */
package sge

import scala.compiletime.testing.*

class SpriteBatchEncapsulationIss783RedSuite extends munit.FunSuite {

  test("ISS-783: external code must not be able to assign SpriteBatch.idx (Java package-private int idx; PolygonSpriteBatch keeps it private)") {
    val errors: List[Error] = typeCheckErrors(
      """(??? : sge.graphics.g2d.SpriteBatch).idx = 42"""
    )
    assert(
      errors.nonEmpty,
      "external assignment to SpriteBatch.idx must not compile (should be private[g2d], SpriteBatch.java:53); it currently does because idx is a public var"
    )
  }

  test("ISS-783: external code must not be able to assign SpriteBatch.lastTexture (Java package-private Texture lastTexture)") {
    val errors: List[Error] = typeCheckErrors(
      """(??? : sge.graphics.g2d.SpriteBatch).lastTexture = ???"""
    )
    assert(
      errors.nonEmpty,
      "external assignment to SpriteBatch.lastTexture must not compile (should be private[g2d], SpriteBatch.java:54); it currently does because lastTexture is a public var"
    )
  }

  test("ISS-783: external code must not be able to assign SpriteBatch.drawing (Java package-private boolean drawing)") {
    val errors: List[Error] = typeCheckErrors(
      """(??? : sge.graphics.g2d.SpriteBatch).drawing = true"""
    )
    assert(
      errors.nonEmpty,
      "external assignment to SpriteBatch.drawing must not compile (should be private[g2d], SpriteBatch.java:57); it currently does because drawing is a public var"
    )
  }

  test("ISS-783 (control): genuinely public SpriteBatch API stays externally assignable") {
    // renderCalls is `public int renderCalls` in Java (SpriteBatch.java) and
    // must remain public. This control passes on both the current tree and the
    // fixed version, proving the compile-check mechanism isn't just flagging a
    // broken snippet.
    val errors: List[Error] = typeCheckErrors(
      """(??? : sge.graphics.g2d.SpriteBatch).renderCalls = 7"""
    )
    assert(errors.isEmpty, s"renderCalls must stay publicly assignable; errors: ${errors.map(_.message)}")
  }
}
