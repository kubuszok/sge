/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * DESIGN-PIN red tests for ISS-770 (the Align opaque type is not used at API
 * boundaries: font/layout alignment parameters are raw `halign: Int` in
 * GlyphLayout, BitmapFont and BitmapFontCache, and scene2d/ui bridges to them
 * via `Align.left.asInstanceOf[Int]`).
 *
 * These are TYPE-LEVEL assertions, not value traces: they pin the *design
 * contract* that alignment parameters should be typed `sge.utils.Align`, not a
 * bare `Int`. The seam is exactly where ISS-584/ISS-581 slipped in (halign = 0
 * silently accepted where Align.left = 8 was meant): because the parameter is a
 * plain Int, any meaningless Int compiles, and a real `Align` value does NOT
 * compile without a cast.
 *
 * Evidence of the untyped seam in the current tree:
 *   - GlyphLayout.scala:53,58,66,69 — `halign: Int` on every setText/ctor.
 *   - BitmapFontCache.scala:345,350,355,368,371,374 — `halign: Int`.
 *   - BitmapFont.scala:133,140 — `halign: Int` on draw().
 *   - scene2d/ui/Label.scala:170,211,237 — must launder its own `Align`
 *     fields through `.asInstanceOf[Int]` to call GlyphLayout.setText, and
 *     Label.scala:49-50 store the same values as `Align`. The cast is only
 *     necessary because the boundary is typed Int; it round-trips an Align to
 *     Int and back, defeating the opaque type. (Territory note: Label is cited
 *     as evidence only; the pin below is asserted at the GlyphLayout boundary,
 *     which is the representative seam Label bridges.)
 *
 * Original LibGDX types these the same raw way (int halign), so this is a
 * *design improvement* pin (SGE > LibGDX via opaque types, cf.
 * guide-improvements), NOT a faithfulness bug — marked clearly. It turns GREEN
 * when GlyphLayout's halign parameter is retyped to `sge.utils.Align`.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the target design contract.
 */
package sge
package graphics
package g2d

import scala.compiletime.testing.*

import lowlevel.Nullable
import lowlevel.util.DynamicArray

class AlignApiBoundaryIss770RedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font/layout fixtures (same pattern as GlyphLayoutAlignRedSuite) ---
  //
  // typeCheckErrors compiles its argument in the enclosing scope, so `font`
  // and `layout` below must be in scope for the checked snippets. Neither
  // snippet is executed — only type-checked — so a texture-less font is fine.

  private def makeFont(): BitmapFont = {
    val data = new BitmapFontData()
    data.capHeight = 10f
    data.down = -12f
    data.spaceXadvance = 10f
    val regions = DynamicArray[TextureRegion]()
    regions.add(new TextureRegion())
    new BitmapFont(data, Nullable(regions), true)
  }

  private val font:   BitmapFont  = makeFont()
  private val layout: GlyphLayout = new GlyphLayout()

  test("ISS-770 (design pin): an Align value must be accepted directly at GlyphLayout.setText's halign boundary") {
    // TARGET design: halign is typed `sge.utils.Align`, so passing the opaque
    // `Align.left` compiles with no cast.
    // CURRENT tree: halign is `Int`; `Align` is an opaque type (Align.scala:37,
    // `opaque type Align = Int`) that is NOT `Int` outside its own scope, so
    // this snippet fails to type-check — exactly why Label.scala:170 must write
    // `Align.left.asInstanceOf[Int]`.
    val errors: List[Error] = typeCheckErrors(
      """layout.setText(font, "ab", sge.graphics.Color.WHITE, 50f, sge.utils.Align.left, false)"""
    )
    assert(
      errors.isEmpty,
      s"an Align value must be accepted directly at the halign boundary (ISS-770); " +
        s"it does not because halign is raw Int, forcing Label.scala's .asInstanceOf[Int]. Errors: ${errors.map(_.message)}"
    )
  }

  test("ISS-770 (design pin): a meaningless raw Int must NOT type-check as an alignment at GlyphLayout.setText") {
    // TARGET design: with halign typed `Align`, a bare Int like 999 (not a
    // valid alignment bitmask, and not an `Align`) is rejected at compile time.
    // CURRENT tree: halign is `Int`, so 999 compiles silently — this is the
    // untyped seam through which halign = 0 (ISS-584) and mis-set bits pass
    // unchecked.
    val errors: List[Error] = typeCheckErrors(
      """layout.setText(font, "ab", sge.graphics.Color.WHITE, 50f, 999, false)"""
    )
    assert(
      errors.nonEmpty,
      "a raw Int (999) must NOT type-check as an alignment argument (ISS-770); " +
        "it currently does because halign is typed Int instead of sge.utils.Align, making the ISS-584 bug class representable"
    )
  }
}
