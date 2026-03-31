package sge
package textra

class FontSuite extends munit.FunSuite {

  test("GlyphRegion extends TextureRegion") {
    val gr = new Font.GlyphRegion(1.5f, 2.5f, 10f)
    assertEquals(gr.offsetX, 1.5f)
    assertEquals(gr.offsetY, 2.5f)
    assertEquals(gr.xAdvance, 10f)
    // Inherits TextureRegion properties (default zeros since no texture set)
    assertEquals(gr.regionWidth, 0)
    assertEquals(gr.regionHeight, 0)
  }

  test("GlyphRegion copy constructor preserves metrics") {
    val original = new Font.GlyphRegion(3f, 4f, 12f)
    val copy     = new Font.GlyphRegion(original)
    assertEquals(copy.offsetX, 3f)
    assertEquals(copy.offsetY, 4f)
    assertEquals(copy.xAdvance, 12f)
  }

  test("GlyphRegion flip adjusts offsets") {
    val gr = new Font.GlyphRegion(5f, 7f, 10f)
    gr.flip(true, false)
    assertEquals(gr.offsetX, -5f)
    assertEquals(gr.xAdvance, -10f)
    assertEquals(gr.offsetY, 7f)
  }

  test("GlyphRegion maxDimension returns max of width and height") {
    val gr = new Font.GlyphRegion(0f, 0f, 0f)
    // regionWidth and regionHeight are 0 by default (no texture)
    assertEqualsFloat(gr.maxDimension, 0f, 0.001f)
  }

  test("Font default fields") {
    val font = new Font()
    assertEquals(font.name, "Unnamed Font")
    assertEqualsFloat(font.scaleX, 1f, 0.001f)
    assertEqualsFloat(font.scaleY, 1f, 0.001f)
    assertEqualsFloat(font.cellWidth, 1f, 0.001f)
    assertEqualsFloat(font.cellHeight, 1f, 0.001f)
    assertEquals(font.isMono, false)
    assertEquals(font.getDistanceField, Font.DistanceFieldType.STANDARD)
    assertEquals(font.parents.isEmpty, true)
  }

  test("Font scale methods") {
    val font = new Font()
    font.cellWidth = 10f
    font.cellHeight = 20f
    font.scale(2f)
    assertEqualsFloat(font.scaleX, 2f, 0.001f)
    assertEqualsFloat(font.scaleY, 2f, 0.001f)
    assertEqualsFloat(font.cellWidth, 20f, 0.001f)
    assertEqualsFloat(font.cellHeight, 40f, 0.001f)
  }

  test("Font scaleTo sets exact dimensions") {
    val font = new Font()
    font.originalCellWidth = 10f
    font.originalCellHeight = 20f
    font.scaleTo(30f, 40f)
    assertEqualsFloat(font.scaleX, 3f, 0.001f)
    assertEqualsFloat(font.scaleY, 2f, 0.001f)
    assertEqualsFloat(font.cellWidth, 30f, 0.001f)
    assertEqualsFloat(font.cellHeight, 40f, 0.001f)
  }

  test("Font kerningPair encodes two chars") {
    val font = new Font()
    val pair = font.kerningPair('A', 'V')
    assertEquals(pair, ('A'.toInt << 16) | 'V'.toInt)
  }

  test("Font copy constructor copies fields") {
    val font = new Font()
    font.name = "TestFont"
    font.scaleX = 2f
    font.cellWidth = 16f
    font.cellHeight = 24f
    font.isMono = true
    font.mapping.put('A'.toInt, new Font.GlyphRegion(0f, 0f, 8f))

    val copy = new Font(font)
    assertEquals(copy.name, "TestFont")
    assertEqualsFloat(copy.scaleX, 2f, 0.001f)
    assertEqualsFloat(copy.cellWidth, 16f, 0.001f)
    assertEquals(copy.isMono, true)
    // Copy should have the same mapping entries
    assert(copy.mapping.contains('A'.toInt))
  }

  test("Font DistanceFieldType enum values") {
    assertEquals(Font.DistanceFieldType.STANDARD.filePart, "-standard")
    assertEquals(Font.DistanceFieldType.SDF.filePart, "-sdf")
    assertEquals(Font.DistanceFieldType.MSDF.filePart, "-msdf")
    assertEquals(Font.DistanceFieldType.SDF_OUTLINE.filePart, "-sdf")
    assertEquals(Font.DistanceFieldType.SDF.namePart, " (SDF)")
  }

  test("Font style bit flags are distinct") {
    assert(Font.BOLD != Font.OBLIQUE)
    assert(Font.UNDERLINE != Font.STRIKETHROUGH)
    assert(Font.SUPERSCRIPT != Font.SUBSCRIPT)
    assert(Font.ERROR != Font.WARN)
    assert(Font.NOTE != Font.CONTEXT)
    // Ensure no overlap between mode flags
    assert((Font.BOLD & Font.OBLIQUE) == 0L)
    assert((Font.UNDERLINE & Font.STRIKETHROUGH) == 0L)
  }

  test("Font packed color constants are non-zero") {
    val font = new Font()
    assert(font.PACKED_BLACK != 0f)
    assert(font.PACKED_WHITE != 0f)
    assert(font.PACKED_RED != 0f)
    assert(font.PACKED_ERROR_COLOR != 0f)
    assert(font.PACKED_WARN_COLOR != 0f)
    assert(font.PACKED_NOTE_COLOR != 0f)
  }

  test("Font addSpacingGlyph requires space glyph") {
    val font = new Font()
    font.mapping.put(' '.toInt, new Font.GlyphRegion(0f, 0f, 8f))
    val result = font.addSpacingGlyph('\t', 32f)
    assert(result eq font)
    assert(font.mapping.contains('\t'.toInt))
    assertEqualsFloat(font.mapping('\t'.toInt).xAdvance, 32f, 0.001f)
  }

  test("Font resizeDistanceField adjusts crispness") {
    val font = new Font()
    font.distanceFieldCrispness = 1f
    font.setDistanceField(Font.DistanceFieldType.SDF)
    font.resizeDistanceField(1920f, 1080f)
    assert(font.actualCrispness > 0f)
  }
}
