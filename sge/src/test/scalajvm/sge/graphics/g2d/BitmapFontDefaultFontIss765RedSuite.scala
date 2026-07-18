/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-765 (the no-arg `BitmapFont()` constructor — and its
 * boolean-flip sibling `BitmapFont(flip)` — which the original ships to display
 * text without generating a font, are NOT ported, and the default Liberation
 * Sans 15pt font that backs them does not ship in sge resources).
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
 * The default-font pngs/fnt live in the LibGDX JAR at
 * com/badlogic/gdx/utils/lsans-15.{fnt,png}
 * (original-src/libgdx/gdx/res/com/badlogic/gdx/utils/lsans-15.{fnt,png}).
 * The sge port has neither the constructors nor the resources: no `.fnt`
 * ships anywhere under sge (verified: no lsans-15.* in sge/src/main/resources),
 * so `new BitmapFont()` is impossible today.
 *
 * The port even documents the gap as an open question in BitmapFont.scala's own
 * migration notes ("Issue: test: needs .fnt fixture file ...") yet carries a
 * `Covenant: full-port` header — so this is a genuine unported-behaviour red,
 * not an intentional documented drop.
 *
 * FORM (documented per task): the constructor red is a COMPILE-LEVEL assertion
 * (typeCheckErrors) — the no-arg/boolean-flip constructors do not exist, so the
 * snippets fail to type-check today. The resource red is a RESOURCE-PRESENCE
 * assertion on the JVM classpath. The path is the faithful sge mapping of the
 * LibGDX classpath resource (`com/badlogic/gdx/utils/...` -> `sge/utils/...`,
 * per docs/contributing/type-mappings path rule); if the implementer ships the
 * font under a different sge path, they update SGE_DEFAULT_FONT_PATHS here.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer beyond the documented resource-path constant.
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

  // Faithful sge mapping of com/badlogic/gdx/utils/lsans-15.{fnt,png}; the
  // implementer updates these if the default font ships under another sge path.
  private val SGE_DEFAULT_FONT_PATHS: List[String] =
    List("sge/utils/lsans-15.fnt", "sge/utils/lsans-15.png")

  test("ISS-765: the default Liberation Sans 15pt font (.fnt + .png) must ship on the classpath") {
    // TARGET: the default-font resources are packaged in sge so the no-arg
    // constructor can load them via Sge().files.classpath(...). CURRENT tree:
    // no lsans-15.* ships under sge resources, so getResource returns null for
    // every candidate path — `new BitmapFont()` could not work even if the
    // constructor were added.
    val cl      = getClass.getClassLoader
    val missing = SGE_DEFAULT_FONT_PATHS.filter(p => cl.getResource(p) == null)
    assert(
      missing.isEmpty,
      s"the default font resources must ship on the classpath (ISS-765); missing: $missing. " +
        "LibGDX ships com/badlogic/gdx/utils/lsans-15.{fnt,png}; sge ships none."
    )
  }

  // ISS-851: zinc-visible dependency anchor. The compile-shape assertions above
  // exercise BitmapFont's no-arg and boolean-flip constructors ONLY inside
  // typeCheckErrors string literals, which zinc's incremental compiler cannot see
  // — so removing/retyping those ctors would NOT recompile this suite, leaving a
  // stale pass (the wave-G false-green trap). A bare `classOf[BitmapFont]` is
  // insufficient: zinc name-hashing only invalidates dependents that use the
  // *changed member's* name, and the ctors are referenced solely in the strings.
  // These anchors therefore MIRROR the asserted surface in real code — the exact
  // `new BitmapFont()` / `new BitmapFont(flip)` ctors the assertions pin — so any
  // regression invalidates these method bodies and recompiles the suite. They are
  // type-level references in uncalled methods: never executed (which also avoids
  // the default-font classpath load, absent on the Native axis), no side effects.
  def zincAnchorNoArg: BitmapFont = new BitmapFont()
  def zincAnchorFlip:  BitmapFont = new BitmapFont(true)
}
