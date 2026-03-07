package sge
package maps
package tiled

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

// TODO: test: decode a real .tmx file from libgdx/tests/ end-to-end through BaseTmxMapLoader
// TODO: test: decode a real .tmj file from libgdx/tests/ end-to-end through BaseTmjMapLoader
// TODO: test: decode a real .tiled-project file end-to-end through BaseTiledMapLoader.loadProjectFile
class BaseTiledMapLoaderTest extends munit.ScalaCheckSuite {

  // ---- tiledColorToLibGDXColor unit tests ----

  test("converts #AARRGGBB (9-char) to RRGGBBAA") {
    // Tiled format: #AARRGGBB → LibGDX format: RRGGBBAA
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#ff112233")
    assertEquals(result, "112233ff")
  }

  test("converts 7-char color by appending ff alpha") {
    // Tiled format: #RRGGBB → LibGDX format: RRGGBBff
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#aabbcc")
    assertEquals(result, "aabbccff")
  }

  test("alpha prefix is moved to suffix for 9-char colors") {
    // #80ff0000 → alpha=80, color=ff0000 → ff000080
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#80ff0000")
    assertEquals(result, "ff000080")
  }

  test("fully opaque 9-char color") {
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#ffffffff")
    assertEquals(result, "ffffffff")
  }

  test("fully transparent 9-char color") {
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#00123456")
    assertEquals(result, "12345600")
  }

  test("7-char white") {
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#ffffff")
    assertEquals(result, "ffffffff")
  }

  test("7-char black") {
    val result = BaseTiledMapLoader.tiledColorToLibGDXColor("#000000")
    assertEquals(result, "000000ff")
  }

  // ---- ScalaCheck property tests ----

  private val hexChar: Gen[Char]   = Gen.oneOf(('0' to '9') ++ ('a' to 'f'))
  private val hexPair: Gen[String] = Gen.listOfN(2, hexChar).map(_.mkString)

  property("9-char output has 8 hex characters") {
    forAll(hexPair, hexPair, hexPair, hexPair) { (aa: String, rr: String, gg: String, bb: String) =>
      val input  = s"#$aa$rr$gg$bb"
      val result = BaseTiledMapLoader.tiledColorToLibGDXColor(input)
      assertEquals(result.length, 8)
    }
  }

  property("7-char output has 8 hex characters") {
    forAll(hexPair, hexPair, hexPair) { (rr: String, gg: String, bb: String) =>
      val input  = s"#$rr$gg$bb"
      val result = BaseTiledMapLoader.tiledColorToLibGDXColor(input)
      assertEquals(result.length, 8)
    }
  }

  property("9-char: alpha prefix becomes suffix") {
    forAll(hexPair, hexPair, hexPair, hexPair) { (aa: String, rr: String, gg: String, bb: String) =>
      val input  = s"#$aa$rr$gg$bb"
      val result = BaseTiledMapLoader.tiledColorToLibGDXColor(input)
      // result should be rrggbb + aa
      assertEquals(result, s"$rr$gg$bb$aa")
    }
  }

  property("7-char: output is RRGGBB + ff") {
    forAll(hexPair, hexPair, hexPair) { (rr: String, gg: String, bb: String) =>
      val input  = s"#$rr$gg$bb"
      val result = BaseTiledMapLoader.tiledColorToLibGDXColor(input)
      assertEquals(result, s"$rr$gg$bb" + "ff")
    }
  }
}
