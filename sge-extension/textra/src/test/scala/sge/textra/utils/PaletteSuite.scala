package sge
package textra
package utils

class PaletteSuite extends munit.FunSuite {

  // user ruling 2026-10-17: the generated Palette.NAMED is sge's ObjectIntMap and NAMES an lls DynamicArray
  test("named colors RED, GREEN, BLUE, WHITE, BLACK exist") {
    assert(Palette.NAMED.containsKey("RED"), "RED missing")
    assert(Palette.NAMED.containsKey("GREEN"), "GREEN missing")
    assert(Palette.NAMED.containsKey("BLUE"), "BLUE missing")
    assert(Palette.NAMED.containsKey("WHITE"), "WHITE missing")
    assert(Palette.NAMED.containsKey("BLACK"), "BLACK missing")
  }

  test("lowercase color names exist") {
    assert(Palette.NAMED.containsKey("red"), "red missing")
    assert(Palette.NAMED.containsKey("green"), "green missing")
    assert(Palette.NAMED.containsKey("blue"), "blue missing")
    assert(Palette.NAMED.containsKey("white"), "white missing")
    assert(Palette.NAMED.containsKey("black"), "black missing")
  }

  test("color values match expected RGBA8888") {
    assertEquals(Palette.white, 0xffffffff)
    assertEquals(Palette.black, 0x000000ff)
    assertEquals(Palette.red, 0xff0000ff.toInt)
    assertEquals(Palette.green, 0x00ff00ff)
    assertEquals(Palette.blue, 0x0000ffff)
  }

  test("LIST is populated with expected count") {
    // 49 lowercase + 34 uppercase = 83 entries (including aliases merged into NAMED,
    // but LIST is populated from the entries Seq which has 83 items)
    assert(Palette.LIST.size >= 80, s"Expected at least 80 colors in LIST, got ${Palette.LIST.size}")
  }

  test("NAMES is sorted alphabetically") {
    val names  = (0 until Palette.NAMES.size).map(i => Palette.NAMES(i)) // user ruling 2026-10-17: lls DynamicArray
    val sorted = names.sorted
    assertEquals(names, sorted)
  }
}
