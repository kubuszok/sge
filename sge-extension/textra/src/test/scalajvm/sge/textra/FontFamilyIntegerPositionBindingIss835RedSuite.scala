/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-835 (textra wave 2026-07-17-F, territory Y), against
 * TextraTypist upstream commit 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 * (original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   In drawGlyph, the drawn font is the family-connected font, resolved at
 *   Font.java:5336-5338:
 *       Font font = null;
 *       if (family != null) font = family.connected[(int)(glyph >>> 16 & 15)];
 *       if (font == null) font = this;
 *   Upstream then writes EVERY position vertex through THAT connected font's
 *   handleIntegerPosition — the main-glyph quad at Font.java:5712-5713:
 *       vertices[0] = font.handleIntegerPosition(x + cos*p0x - sin*p0y); ...
 *   and the outline / HALO copies at Font.java:5659-5660 / 5680-5681 likewise
 *   bind to `font.` (the connected font), NOT to `this`.
 *
 *   The port binds those same writes to `this` instead: the main-glyph writes
 *   call the unqualified handleIntegerPosition (Font.scala:2820-2825) and the
 *   outline/HALO copies funnel through setQuadVertices, whose body also calls
 *   the unqualified handleIntegerPosition (Font.scala:2419-2424) — both resolve
 *   to `this.handleIntegerPosition`, never the family-connected font's. When a
 *   FontFamily mixes fonts whose handleIntegerPosition behaves differently, the
 *   port applies the WRONG font's integer-position handling.
 *
 * Isolation of the binding (independent of the rounding-body question in
 * ISS-836): the family-connected font overrides handleIntegerPosition to return
 * a fixed SENTINEL for any input. Under the upstream binding, drawGlyph routes
 * every main-glyph position write through THAT font, so all main-glyph position
 * vertices collapse to SENTINEL (vertex 15/16 = v0 - v5 + v10 = SENTINEL too).
 * The port binds those writes to `this` — a plain parent font whose
 * handleIntegerPosition is the identity — so the vertices keep their ordinary
 * fractional geometry and never reach SENTINEL. A constant override survives
 * however many call sites the fix rebinds and whatever ISS-836 decides the base
 * body does, because it ignores its argument entirely.
 *
 * Written by the reproducer agent; MUST NOT be modified by the fixer — it
 * encodes upstream TextraTypist font-binding semantics, not the port's.
 */
package sge
package textra

class FontFamilyIntegerPositionBindingIss835RedSuite extends munit.FunSuite {

  private val Sentinel = 512f

  /** A Font whose handleIntegerPosition ignores its argument and returns a fixed sentinel, so we can observe which font instance drawGlyph binds its position writes to. */
  final private class SentinelFont extends Font {
    override protected def handleIntegerPosition(x: Float): Float = Sentinel
  }

  private def prepare(font: Font, tex: sge.graphics.Texture): Unit = {
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f
    font.mapping.put('A'.toInt, HeadlessTextraSge.texturedGlyph(tex, 12, 16, 10f))
  }

  // Positive control: when the font drawGlyph is invoked on IS the sentinel font
  // (this == font, no family), the `this`-bound writes already route through the
  // sentinel handleIntegerPosition, so every main-glyph position vertex collapses
  // to SENTINEL. This proves the sentinel mechanism reaches the recorded quad, so
  // the red test below can only fail because of the this-vs-font binding.
  test("ISS-835 control: with this == the sentinel font, main-glyph position vertices collapse to SENTINEL") {
    given Sge = HeadlessTextraSge.make()
    val tex   = HeadlessTextraSge.newTexture()

    val font = new SentinelFont()
    prepare(font, tex)

    val batch = new HeadlessTextraSge.RecordingBatch
    font.drawGlyph(batch, 'A'.toLong, 5f, 40f)

    assertEquals(batch.quads.size, 1, "a plain glyph emits exactly one main-glyph quad")
    val quad = batch.quads(0)
    List(0, 1, 5, 6, 10, 11, 15, 16).foreach { i =>
      assertEquals(quad.verts(i), Sentinel, s"control: main-glyph position vertex $i is written through handleIntegerPosition")
    }
  }

  test(
    "ISS-835: drawGlyph binds position writes to the family-connected font (upstream Font.java:5712-5713 font.handleIntegerPosition), not to `this`"
  ) {
    given Sge = HeadlessTextraSge.make()
    val tex   = HeadlessTextraSge.newTexture()

    // `this` — the font drawGlyph is invoked on. Plain identity handleIntegerPosition.
    val parent = new Font()
    prepare(parent, tex)

    // Family slot 1 is the sentinel font; drawGlyph must route the glyph's
    // position writes through IT, because upstream binds to the connected font.
    val sentinelSlot = new SentinelFont()
    prepare(sentinelSlot, tex)

    val fam = new Font.FontFamily()
    fam.connected(1) = sentinelSlot
    parent.setFamily(fam)

    // Family index lives in glyph bits 16-19 (glyph >>> 16 & 15); no style flag
    // occupies those bits, so this is an otherwise-plain 'A' glyph in slot 1.
    val glyph = 'A'.toLong | (1L << 16)

    val batch = new HeadlessTextraSge.RecordingBatch
    parent.drawGlyph(batch, glyph, 5f, 40f)

    assertEquals(batch.quads.size, 1, "a plain glyph emits exactly one main-glyph quad")
    val quad = batch.quads(0)

    // Upstream routes every main-glyph position write through the connected
    // sentinel font's handleIntegerPosition, collapsing all eight position
    // vertices to SENTINEL. The port binds to `this` (parent, identity) and
    // leaves ordinary fractional geometry, so this assertion is red until the
    // writes bind to the connected font.
    List(0, 1, 5, 6, 10, 11, 15, 16).foreach { i =>
      assertEquals(
        quad.verts(i),
        Sentinel,
        s"ISS-835: main-glyph position vertex $i must be written through the connected font's handleIntegerPosition (upstream font.handleIntegerPosition, Font.java:5712-5713); the port binds to `this` (Font.scala:2820-2825) and keeps parent geometry"
      )
    }
  }
}
