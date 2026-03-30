package sge
package textra

class LayoutLineSuite extends munit.FunSuite {

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
}
