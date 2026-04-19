/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package g2d

import sge.utils.Nullable

/** Tests for TextureAtlas inner data classes that don't require GL context. */
class TextureAtlasDataTest extends munit.FunSuite {

  private def mkData(): TextureAtlas.TextureAtlasData = TextureAtlas.TextureAtlasData()

  // --- Region.findValue ---

  test("Region findValue returns empty when no names") {
    val data   = mkData()
    val region = data.Region()
    val result = region.findValue("split")
    assert(result.isEmpty)
  }

  test("Region findValue returns empty when name not found") {
    val data   = mkData()
    val region = data.Region()
    region.names = Nullable(Array("split", "pad"))
    region.values = Nullable(Array(Array(1, 2, 3, 4), Array(5, 6, 7, 8)))
    val result = region.findValue("nonexistent")
    assert(result.isEmpty)
  }

  test("Region findValue returns matching value array") {
    val data   = mkData()
    val region = data.Region()
    region.names = Nullable(Array("split", "pad"))
    region.values = Nullable(Array(Array(1, 2, 3, 4), Array(5, 6, 7, 8)))

    val split = region.findValue("split")
    assert(split.isDefined)
    split.foreach { s =>
      assertEquals(s.toSeq, Seq(1, 2, 3, 4))
    }

    val pad = region.findValue("pad")
    assert(pad.isDefined)
    pad.foreach { p =>
      assertEquals(p.toSeq, Seq(5, 6, 7, 8))
    }
  }

  // --- Region default values ---

  test("Region default values") {
    val data   = mkData()
    val region = data.Region()
    assertEquals(region.left, 0)
    assertEquals(region.top, 0)
    assertEquals(region.width, 0)
    assertEquals(region.height, 0)
    assertEqualsFloat(region.offsetX, 0f, 0.001f)
    assertEqualsFloat(region.offsetY, 0f, 0.001f)
    assertEquals(region.originalWidth, 0)
    assertEquals(region.originalHeight, 0)
    assertEquals(region.degrees, 0)
    assert(!region.rotate)
    assertEquals(region.index, -1)
    assert(region.names.isEmpty)
    assert(region.values.isEmpty)
    assert(!region.flip)
  }

  // --- Page default values ---

  test("Page default values") {
    val data = mkData()
    val page = data.Page()
    assertEqualsFloat(page.width, 0f, 0.001f)
    assertEqualsFloat(page.height, 0f, 0.001f)
    assert(!page.useMipMaps)
    assert(!page.pma)
    assert(page.texture.isEmpty)
    assert(page.textureFile.isEmpty)
  }

  // --- TextureAtlasData empty construction ---

  test("TextureAtlasData empty construction has no pages or regions") {
    val data = mkData()
    assertEquals(data.pages.size, 0)
    assertEquals(data.regions.size, 0)
  }
}
