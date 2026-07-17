/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-765 (the no-arg `BitmapFont()` constructor — and its
 * boolean-flip sibling `BitmapFont(flip)` — which the original ships to display
 * text without generating a font, are NOT ported).
 *
 * Original: com/badlogic/gdx/graphics/g2d/BitmapFont.java lines 71-99
 * (original-src/libgdx, commit 146c7a9152b1c9348907b440c606c5828d412389):
 *
 *   // lines 71-80
 *   public BitmapFont () {
 *     this(Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.fnt"),
 *          Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.png"), false, true);
 *   }
 *   public BitmapFont (boolean flip) {
 *     this(Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.fnt"),
 *          Gdx.files.classpath("com/badlogic/gdx/utils/lsans-15.png"), flip, true);
 *   }
 *
 * The sge port has only file-based secondary constructors
 * (BitmapFont.scala:56-96) — no `def this()(using Sge)` and no
 * `def this(flip: Boolean)(using Sge)` — so `new BitmapFont()` is impossible.
 *
 * FORM (documented per task): COMPILE-LEVEL assertion (typeCheckErrors) — the
 * constructors do not exist, so the snippets fail to type-check today. The
 * companion resource-presence assertion (that lsans-15.{fnt,png} must ship on
 * the classpath) lives in the JVM copy of this suite, where classpath resource
 * semantics are well-defined.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer.
 */
package sge
package graphics
package g2d

import scala.compiletime.testing.*

class BitmapFontDefaultFontIss765RedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  test("ISS-765: no-arg `new BitmapFont()` must construct the default 15pt Liberation Sans font (BitmapFont.java:71-80)") {
    // TARGET: the no-arg constructor exists and type-checks (a `given Sge` is
    // in scope above). CURRENT tree: BitmapFont has only file-based secondary
    // constructors (BitmapFont.scala:56-96) — no `def this()(using Sge)` — so
    // this fails to type-check.
    val errors: List[Error] = typeCheckErrors("new BitmapFont()")
    assert(
      errors.isEmpty,
      s"`new BitmapFont()` must compile (default classpath font, BitmapFont.java:71-80); " +
        s"it does not — the no-arg constructor is not ported. Errors: ${errors.map(_.message)}"
    )
  }

  test("ISS-765: boolean-flip `new BitmapFont(flip)` must construct the default font (BitmapFont.java:82-88)") {
    // TARGET: the boolean-only constructor exists and type-checks. CURRENT
    // tree: `BitmapFont(true)` matches no constructor (the only single-arg one
    // takes a FileHandle) — fails to type-check.
    val errors: List[Error] = typeCheckErrors("new BitmapFont(true)")
    assert(
      errors.isEmpty,
      s"`new BitmapFont(flip)` must compile (default classpath font, BitmapFont.java:82-88); " +
        s"it does not — the boolean-flip constructor is not ported. Errors: ${errors.map(_.message)}"
    )
  }
}
