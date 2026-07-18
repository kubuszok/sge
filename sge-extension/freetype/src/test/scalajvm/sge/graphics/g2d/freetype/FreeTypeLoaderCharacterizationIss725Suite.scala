/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-725 (freetype clause) characterization suite for the AssetManager loader
 * wiring of the freetype package: FreetypeFontLoader and
 * FreeTypeFontGeneratorLoader. These paths are pure asset-graph logic — they
 * build AssetDescriptor dependencies and enforce the "parameter is required"
 * contract WITHOUT ever touching the native FreeType library, so the suite is a
 * deterministic green with no native-lib availability guard needed.
 *
 * They need a headless [[Sge]] only because the loader constructors carry a
 * `(using Sge)` context (via AsynchronousAssetLoader / SynchronousAssetLoader),
 * which keeps them in the JVM row alongside FreetypeTestFixture. The pure
 * config surface (parameter defaults, enums, constants) is characterized
 * cross-platform by FreeTypeConfigCharacterizationIss725Suite.
 *
 * Every value pinned here is fixed by the ORIGINAL libGDX FreetypeFontLoader.java
 * / FreeTypeFontGeneratorLoader.java.
 */
package sge
package graphics
package g2d
package freetype

import sge.assets.AssetManager
import sge.assets.loaders.FileHandleResolver
import sge.files.FileHandle

class FreeTypeLoaderCharacterizationIss725Suite extends munit.FunSuite {

  private given Sge = FreetypeTestFixture.headlessSge()

  /** A resolver that must never be consulted by dependency computation / the null guard. */
  private val throwingResolver: FileHandleResolver =
    new FileHandleResolver {
      override def resolve(fileName: String): FileHandle =
        throw new IllegalStateException(s"resolver must not be touched: $fileName")
    }

  // ── FreetypeFontLoader.getDependencies (FreetypeFontLoader.java) ──────────

  test("FreetypeFontLoader.getDependencies requests one '.gen' FreeTypeFontGenerator keyed by fontFileName") {
    val loader = FreetypeFontLoader(throwingResolver)
    val param  = FreetypeFontLoader.FreeTypeFontLoaderParameter()
    param.fontFileName = "fonts/myfont.ttf"
    val file = FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont)

    val deps = loader.getDependencies("fonts/myfont.ttf", file, param)
    assertEquals(deps.size, 1, "exactly one generator dependency is declared")
    val dep = deps(0)
    assertEquals(dep.fileName, "fonts/myfont.ttf.gen", "the generator is keyed by fontFileName + \".gen\"")
    assertEquals(dep.`type`.getName, classOf[FreeTypeFontGenerator].getName, "the dependency's asset type is the generator")
  }

  // ── FreetypeFontLoader.loadAsync / loadSync null-parameter contract ───────

  test("FreetypeFontLoader.loadAsync rejects a missing FreeTypeFontLoaderParameter") {
    val loader = FreetypeFontLoader(throwingResolver)
    val file   = FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont)
    // A missing parameter drives the port's OWN documented guard (original
    // contract: "FreetypeFontParameter must be set in AssetManager#load ..."):
    // the loader must reject the load before touching the manager or file. The
    // absent parameter is the interop boundary the guard exists to catch.
    val absentParameter: FreetypeFontLoader.FreeTypeFontLoaderParameter = null
    val absentManager:   AssetManager                                   = null
    val ex = intercept[RuntimeException] {
      loader.loadAsync(absentManager, "fonts/myfont.ttf", file, absentParameter)
    }
    assertEquals(ex.getMessage, "FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!")
  }

  // ── FreeTypeFontGeneratorLoader.getDependencies ──────────────────────────

  test("FreeTypeFontGeneratorLoader.getDependencies has no transitive dependencies") {
    val loader = FreeTypeFontGeneratorLoader(throwingResolver)
    val param  = FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters()
    val file   = FreetypeTestFixture.fontHandle(FreetypeTestFixture.OutlineFont)

    val deps = loader.getDependencies("Inconsolata-LGC.ttf", file, param)
    assertEquals(deps.size, 0, "the generator loads standalone, with no dependency graph")
  }
}
