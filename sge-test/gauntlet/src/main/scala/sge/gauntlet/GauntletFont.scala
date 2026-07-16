/*
 * SGE Gauntlet — programmatic 5x7 bitmap font (no font asset ships with sge yet).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.graphics.{ Pixmap, Texture }
import sge.graphics.g2d.{ BitmapFont, BitmapFontData, TextureRegion }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

/** Builds a [[BitmapFont]] entirely in code: a classic 5x7 pixel font baked into a 128x64 texture at runtime. Uppercase letters, digits and common punctuation; lowercase input maps onto the uppercase
  * glyphs. Used by the interactive results UI and the font probes — the engine ships no default font (lsans-15 is unported), so the gauntlet brings its own.
  */
object GauntletFont {

  private val CellW = 6
  private val CellH = 8
  private val Cols  = 16

  // 5x7 pixel patterns, '#' = ink. Row 0 is the glyph top.
  private val patterns: List[(Char, List[String])] = List(
    'A' -> List(" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
    'B' -> List("#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### "),
    'C' -> List(" ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### "),
    'D' -> List("#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "),
    'E' -> List("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"),
    'F' -> List("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    "),
    'G' -> List(" ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ### "),
    'H' -> List("#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
    'I' -> List(" ### ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "),
    'J' -> List("  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  "),
    'K' -> List("#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"),
    'L' -> List("#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"),
    'M' -> List("#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #"),
    'N' -> List("#   #", "##  #", "# # #", "#  ##", "#   #", "#   #", "#   #"),
    'O' -> List(" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
    'P' -> List("#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "),
    'Q' -> List(" ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #"),
    'R' -> List("#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"),
    'S' -> List(" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "),
    'T' -> List("#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "),
    'U' -> List("#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "),
    'V' -> List("#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  "),
    'W' -> List("#   #", "#   #", "#   #", "# # #", "# # #", "## ##", "#   #"),
    'X' -> List("#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #"),
    'Y' -> List("#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "),
    'Z' -> List("#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####"),
    '0' -> List(" ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### "),
    '1' -> List("  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "),
    '2' -> List(" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"),
    '3' -> List(" ### ", "#   #", "    #", "  ## ", "    #", "#   #", " ### "),
    '4' -> List("   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # "),
    '5' -> List("#####", "#    ", "#### ", "    #", "    #", "#   #", " ### "),
    '6' -> List(" ### ", "#    ", "#    ", "#### ", "#   #", "#   #", " ### "),
    '7' -> List("#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   "),
    '8' -> List(" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "),
    '9' -> List(" ### ", "#   #", "#   #", " ####", "    #", "    #", " ### "),
    '-' -> List("     ", "     ", "     ", "#####", "     ", "     ", "     "),
    '_' -> List("     ", "     ", "     ", "     ", "     ", "     ", "#####"),
    '.' -> List("     ", "     ", "     ", "     ", "     ", " ##  ", " ##  "),
    ',' -> List("     ", "     ", "     ", "     ", " ##  ", " ##  ", " #   "),
    ':' -> List("     ", " ##  ", " ##  ", "     ", " ##  ", " ##  ", "     "),
    ';' -> List("     ", " ##  ", " ##  ", "     ", " ##  ", " ##  ", " #   "),
    '!' -> List("  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "     ", "  #  "),
    '?' -> List(" ### ", "#   #", "    #", "   # ", "  #  ", "     ", "  #  "),
    '/' -> List("    #", "    #", "   # ", "  #  ", " #   ", "#    ", "#    "),
    '\\' -> List("#    ", "#    ", " #   ", "  #  ", "   # ", "    #", "    #"),
    '(' -> List("   # ", "  #  ", " #   ", " #   ", " #   ", "  #  ", "   # "),
    ')' -> List(" #   ", "  #  ", "   # ", "   # ", "   # ", "  #  ", " #   "),
    '[' -> List(" ### ", " #   ", " #   ", " #   ", " #   ", " #   ", " ### "),
    ']' -> List(" ### ", "   # ", "   # ", "   # ", "   # ", "   # ", " ### "),
    '+' -> List("     ", "  #  ", "  #  ", "#####", "  #  ", "  #  ", "     "),
    '=' -> List("     ", "     ", "#####", "     ", "#####", "     ", "     "),
    '%' -> List("##   ", "##  #", "   # ", "  #  ", " #   ", "#  ##", "   ##"),
    '\'' -> List("  #  ", "  #  ", "  #  ", "     ", "     ", "     ", "     "),
    '"' -> List(" # # ", " # # ", " # # ", "     ", "     ", "     ", "     "),
    '<' -> List("   # ", "  #  ", " #   ", "#    ", " #   ", "  #  ", "   # "),
    '>' -> List(" #   ", "  #  ", "   # ", "    #", "   # ", "  #  ", " #   "),
    '#' -> List(" # # ", " # # ", "#####", " # # ", "#####", " # # ", " # # "),
    ' ' -> List("     ", "     ", "     ", "     ", "     ", "     ", "     ")
  )

  /** Bakes the font texture and metrics. Caller closes the returned font (it owns nothing but the cache; close the texture via [[BitmapFont.close]] after use). */
  def create()(using Sge): BitmapFont = {
    val texW   = 128
    val texH   = 64
    val pixmap = Pixmap(texW, texH, Pixmap.Format.RGBA8888)
    pixmap.setColor(0x00000000)
    pixmap.fill()

    val white = 0xffffffff
    patterns.zipWithIndex.foreach { case ((_, rows), idx) =>
      val baseX = (idx % Cols) * CellW
      val baseY = (idx / Cols) * CellH
      rows.zipWithIndex.foreach { case (row, ry) =>
        row.zipWithIndex.foreach { case (c, rx) =>
          if (c == '#') pixmap.drawPixel(Pixels(baseX + rx), Pixels(baseY + ry), white)
        }
      }
    }

    val texture = Texture(pixmap)
    pixmap.close()
    val region = TextureRegion(texture)

    val data = new BitmapFontData()
    data.lineHeight = 10f
    data.capHeight = 7f
    data.ascent = 0f
    data.descent = 0f
    data.down = -10f
    data.spaceXadvance = 6f
    data.xHeight = 5f

    def makeGlyph(ch: Char, idx: Int): BitmapFont.Glyph = {
      val g = new BitmapFont.Glyph()
      g.id = ch.toInt
      g.srcX = (idx % Cols) * CellW
      g.srcY = (idx / Cols) * CellH
      g.width = 5
      g.height = 7
      g.xoffset = 0
      g.yoffset = -7
      g.xadvance = 6
      g.page = 0
      g
    }

    patterns.zipWithIndex.foreach { case ((ch, _), idx) =>
      val glyph = makeGlyph(ch, idx)
      data.setGlyph(ch.toInt, glyph)
      if (ch == '?') data.missingGlyph = Nullable(glyph)
      // Map lowercase input onto the uppercase glyphs.
      if (ch >= 'A' && ch <= 'Z') data.setGlyph(ch.toLower.toInt, makeGlyph(ch.toLower, idx))
    }

    val regions = DynamicArray[TextureRegion]()
    regions.add(region)
    new BitmapFont(data, Nullable(regions), true)
  }
}
