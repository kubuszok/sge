package sge
package textra

import sge.utils.Nullable

class LayoutLineSuite extends munit.FunSuite {

  // ---------- Layout construction ----------

  test("new Layout has one empty line") {
    val layout = new Layout()
    assertEquals(layout.lines.size, 1)
    assertEquals(layout.lines(0).glyphs.size, 0)
  }

  test("Layout auxiliary fields start empty") {
    val layout = new Layout()
    assert(layout.offsets.isEmpty, "offsets should be empty")
    assert(layout.sizing.isEmpty, "sizing should be empty")
    assert(layout.rotations.isEmpty, "rotations should be empty")
    assert(layout.advances.isEmpty, "advances should be empty")
  }

  test("Layout defaults") {
    val layout = new Layout()
    assertEquals(layout.maxLines, Int.MaxValue)
    assertEquals(layout.atLimit, false)
    assertEquals(layout.targetWidth, 0f)
  }

  // ---------- Line ----------

  test("Line reset clears glyphs and sizes") {
    val line = new Line()
    line.glyphs += 65L // 'A'
    line.width = 10f
    line.height = 12f
    line.reset()
    assertEquals(line.glyphs.size, 0)
    assertEquals(line.width, 0f)
    assertEquals(line.height, 0f)
  }

  test("Line size sets width and height") {
    val line   = new Line()
    val result = line.size(100f, 20f)
    assertEquals(line.width, 100f)
    assertEquals(line.height, 20f)
    assert(result eq line, "size should return this for chaining")
  }

  test("Line appendTo produces expected format") {
    val line = new Line()
    line.glyphs += 'H'.toLong
    line.glyphs += 'i'.toLong
    line.width = 16f
    line.height = 12f
    val sb = line.appendTo(new StringBuilder)
    val s  = sb.toString
    assert(s.contains("Hi"), s"Expected glyphs in output: $s")
    assert(s.contains("w=16") && !s.contains("w=160"), s"Expected width: $s")
    assert(s.contains("h=12") && !s.contains("h=120"), s"Expected height: $s")
  }

  test("Line toString matches appendTo") {
    val line = new Line()
    line.glyphs += 'X'.toLong
    line.width = 8f
    line.height = 10f
    val fromToString = line.toString
    val fromAppendTo = line.appendTo(new StringBuilder).toString
    assertEquals(fromToString, fromAppendTo)
  }

  test("Line with custom capacity") {
    val line = new Line(capacity = 128)
    // Just verify it constructs and works
    line.glyphs += 'A'.toLong
    assertEquals(line.glyphs.size, 1)
  }

  // ---------- Layout insertLine ----------

  test("insertLine adds a new line after the given index") {
    val layout = new Layout()
    // Layout starts with 1 empty line at index 0
    val result = layout.insertLine(0)
    assert(result.isDefined, "insertLine should return non-empty")
    assertEquals(layout.lines.size, 2)
    // The new line is at index 1
    assertEquals(layout.lines(1).height, 0f)
  }

  test("insertLine appends newline glyph to the line at the given index") {
    val layout = new Layout()
    layout.lines(0).glyphs += 'A'.toLong
    layout.insertLine(0)
    // Line at index 0 should now have 'A' and '\n'
    val glyphs = layout.lines(0).glyphs
    assertEquals(glyphs.last.toChar, '\n')
  }

  test("insertLine respects maxLines limit") {
    val layout = new Layout()
    layout.maxLines = 1
    val result = layout.insertLine(0)
    assert(result.isEmpty, "should return empty when at max lines")
    assert(layout.atLimit, "atLimit should be true")
    assertEquals(layout.lines.size, 1) // no new line added
  }

  test("insertLine with negative index returns empty") {
    val layout = new Layout()
    val result = layout.insertLine(-1)
    assert(result.isEmpty)
  }

  test("insertLine with index >= maxLines returns empty") {
    val layout = new Layout()
    layout.maxLines = 5
    val result = layout.insertLine(5) // index >= maxLines
    assert(result.isEmpty)
  }

  test("multiple insertLine calls build multi-line layout") {
    val layout = new Layout()
    layout.maxLines = 10
    layout.insertLine(0) // now 2 lines
    layout.insertLine(1) // now 3 lines
    layout.insertLine(2) // now 4 lines
    assertEquals(layout.lines.size, 4)
  }

  // ---------- Layout line management ----------

  test("lineCount reflects number of lines") {
    val layout = new Layout()
    assertEquals(layout.lineCount, 1)
    layout.insertLine(0)
    assertEquals(layout.lineCount, 2)
  }

  test("getLine returns line for valid index") {
    val layout = new Layout()
    val line   = layout.getLine(0)
    assert(line.isDefined)
  }

  test("getLine returns empty for out-of-bounds index") {
    val layout = new Layout()
    val result = layout.getLine(5)
    assert(result.isEmpty)
  }

  test("peekLine returns the last line") {
    val layout = new Layout()
    val last   = layout.peekLine
    assert(last eq layout.lines.last)
  }

  test("pushLineBare adds a line without newline glyph") {
    val layout = new Layout()
    val result = layout.pushLineBare()
    assert(result.isDefined)
    assertEquals(layout.lines.size, 2)
    // Original line should NOT have a newline glyph added
    assertEquals(layout.lines(0).glyphs.size, 0)
  }

  test("pushLineBare respects maxLines") {
    val layout = new Layout()
    layout.maxLines = 1
    val result = layout.pushLineBare()
    assert(result.isEmpty)
    assert(layout.atLimit)
  }

  // ---------- Layout clear / reset ----------

  test("clear resets to single empty line") {
    val layout = new Layout()
    layout.insertLine(0)
    layout.insertLine(1)
    layout.offsets += 1f
    layout.sizing += 2f
    layout.rotations += 3f
    layout.advances += 4f
    layout.atLimit = true
    layout.clear()
    assertEquals(layout.lines.size, 1)
    assertEquals(layout.lines(0).glyphs.size, 0)
    assert(layout.offsets.isEmpty)
    assert(layout.sizing.isEmpty)
    assert(layout.rotations.isEmpty)
    assert(layout.advances.isEmpty)
    assert(!layout.atLimit)
  }

  test("reset clears everything including config") {
    val layout = new Layout()
    layout.targetWidth = 100f
    layout.maxLines = 5
    layout.atLimit = true
    layout.reset()
    assertEquals(layout.targetWidth, 0f)
    assertEquals(layout.maxLines, Int.MaxValue)
    assert(!layout.atLimit)
    assertEquals(layout.justification, Justify.NONE)
    assertEquals(layout.lines.size, 1)
  }

  // ---------- Layout setters with chaining ----------

  test("setTargetWidth returns this for chaining") {
    val layout = new Layout()
    val result = layout.setTargetWidth(200f)
    assert(result eq layout)
    assertEquals(layout.targetWidth, 200f)
  }

  test("setMaxLines enforces minimum of 1") {
    val layout = new Layout()
    layout.setMaxLines(0)
    assertEquals(layout.maxLines, 1)
    layout.setMaxLines(-5)
    assertEquals(layout.maxLines, 1)
    layout.setMaxLines(10)
    assertEquals(layout.maxLines, 10)
  }

  test("setEllipsis stores the value") {
    val layout = new Layout()
    layout.setEllipsis(Nullable("..."))
    assert(layout.ellipsis.isDefined)
  }

  test("setJustification stores the value") {
    val layout = new Layout()
    layout.setJustification(Justify.FULL_ON_ALL_LINES)
    assertEquals(layout.justification, Justify.FULL_ON_ALL_LINES)
  }

  // ---------- Layout glyph counting ----------

  test("countGlyphs counts across all lines") {
    val layout = new Layout()
    layout.lines(0).glyphs += 'A'.toLong
    layout.lines(0).glyphs += 'B'.toLong
    layout.insertLine(0)
    layout.lines(1).glyphs += 'C'.toLong
    // Line 0 has A, B, \n (3) ; Line 1 has C (1)
    assertEquals(layout.countGlyphs, 4)
  }

  test("countGlyphsBeforeLine counts glyphs in lines before index") {
    val layout = new Layout()
    layout.lines(0).glyphs += 'A'.toLong
    layout.lines(0).glyphs += 'B'.toLong
    layout.insertLine(0)
    layout.lines(1).glyphs += 'C'.toLong
    // Before line 0: 0 glyphs
    assertEquals(layout.countGlyphsBeforeLine(0), 0)
    // Before line 1: glyphs in line 0 = A, B, \n = 3
    assertEquals(layout.countGlyphsBeforeLine(1), 3)
  }

  // ---------- Layout copy constructor ----------

  test("copy constructor reproduces layout") {
    val original = new Layout()
    original.targetWidth = 300f
    original.maxLines = 5
    original.justification = Justify.SPACES_ON_ALL_LINES
    original.lines(0).glyphs += 'X'.toLong
    original.lines(0).size(50f, 12f)
    original.offsets += 1f += 2f
    original.sizing += 3f += 4f
    original.rotations += 5f
    original.advances += 6f

    val copy = new Layout(original)
    assertEquals(copy.targetWidth, 300f)
    assertEquals(copy.maxLines, 5)
    assertEquals(copy.justification, Justify.SPACES_ON_ALL_LINES)
    assertEquals(copy.lineCount, 1)
    assertEquals(copy.lines(0).glyphs.size, 1)
    assertEquals(copy.lines(0).width, 50f)
    assertEquals(copy.offsets.size, 2)
    assertEquals(copy.sizing.size, 2)
    assertEquals(copy.rotations.size, 1)
    assertEquals(copy.advances.size, 1)
  }

  // ---------- Layout getWidth / getHeight ----------

  test("getWidth returns max line width when no justification") {
    val layout = new Layout()
    layout.lines(0).width = 100f
    layout.insertLine(0)
    layout.lines(1).width = 200f
    assertEquals(layout.getWidth, 200f)
  }

  test("getHeight sums all line heights") {
    val layout = new Layout()
    layout.lines(0).height = 10f
    layout.insertLine(0)
    layout.lines(1).height = 15f
    assertEquals(layout.getHeight, 25f)
  }

  // ---------- Layout truncateExtra ----------

  test("truncateExtra trims auxiliary arrays") {
    val layout = new Layout()
    // Add some auxiliary data
    for (_ <- 0 until 10) {
      layout.advances += 1f
      layout.rotations += 2f
      layout.sizing += 3f += 3f
      layout.offsets += 4f += 4f
    }
    assertEquals(layout.advances.size, 10)
    layout.truncateExtra(5)
    assertEquals(layout.advances.size, 5)
    assertEquals(layout.rotations.size, 5)
    assertEquals(layout.sizing.size, 10) // 5 * 2
    assertEquals(layout.offsets.size, 10) // 5 * 2
  }

  test("truncateExtra with larger limit is a no-op") {
    val layout = new Layout()
    layout.advances += 1f += 2f
    layout.truncateExtra(100)
    assertEquals(layout.advances.size, 2)
  }
}
