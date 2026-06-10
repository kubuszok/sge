/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-488, ISS-489, ISS-490, ISS-491 (GlyphLayout port bugs).
 *
 * Every expected value below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/GlyphLayout.java (original-src/libgdx, commit
 * a729bf1f0de099ebcc60562d72f008157677b559). Java line numbers cited in the
 * test comments refer to that file.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g2d

import lowlevel.Nullable
import lowlevel.util.DynamicArray
import sge.utils.Align

class GlyphLayoutRedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font construction -------------------------------------------
  //
  // BitmapFontData is built programmatically (no file, no GL): the no-arg
  // constructor skips load(), and glyphs are registered with setGlyph AFTER
  // the BitmapFont is constructed so that BitmapFont.load(data) never calls
  // setGlyphRegion against the texture-less dummy TextureRegion.
  // BitmapFontCache only requires regions.size >= 1, which the dummy provides.

  /** All regular glyphs: xadvance 10, width 9, xoffset 0, not fixed-width. */
  private def mkGlyph(ch: Char, xadvance: Int, width: Int): BitmapFont.Glyph = {
    val g = new BitmapFont.Glyph()
    g.id = ch.toInt
    g.xadvance = xadvance
    g.width = width
    g.height = 8
    g
  }

  /** Registers a glyph by writing the page array directly. BitmapFontData.setGlyph cannot be used here: data.glyphs pages are zero-length (not null) arrays, so its `Nullable(page).isEmpty` check
    * never allocates a page and indexing throws ArrayIndexOutOfBoundsException — an unrelated pre-existing bug this suite must not depend on.
    */
  private def registerGlyph(data: BitmapFontData, g: BitmapFont.Glyph): Unit = {
    val page = g.id / BitmapFont.PAGE_SIZE
    if (data.glyphs(page).length == 0) data.glyphs(page) = new Array[BitmapFont.Glyph](BitmapFont.PAGE_SIZE)
    data.glyphs(page)(g.id & (BitmapFont.PAGE_SIZE - 1)) = g
  }

  /** Font metrics used in every trace below: capHeight=10, down=-12, scaleX=1, padLeft=padRight=0, spaceXadvance=10. Regular glyph: xadvance=10, width=9 => getGlyphWidth (Java line 449) = (9+0)*1-0 =
    * 9, first xAdvance (BitmapFontData.getGlyphs) = -xoffset*scaleX-padLeft = 0. Space: xadvance=10, width=0 => glyph width 0. Dot: xadvance=4, width=3.
    */
  private def makeFont(markupEnabled: Boolean): BitmapFont = {
    val data = new BitmapFontData()
    data.capHeight = 10f
    data.down = -12f
    data.spaceXadvance = 10f
    data.markupEnabled = markupEnabled
    val regions = DynamicArray[TextureRegion]()
    regions.add(new TextureRegion())
    val font = new BitmapFont(data, Nullable(regions), true)
    // Register glyphs after the BitmapFont constructor ran (see note above).
    for (ch <- "abefortxRED[]") registerGlyph(data, mkGlyph(ch, 10, 9))
    registerGlyph(data, mkGlyph('.', 4, 3))
    registerGlyph(data, mkGlyph(' ', 10, 0))
    font
  }

  private def layoutOf(font: BitmapFont, str: CharSequence, targetWidth: Float = 0f, wrap: Boolean = false, truncate: Nullable[String] = Nullable.empty[String]): GlyphLayout = {
    val layout = new GlyphLayout()
    // Align.left so Java's alignRuns (Java lines 297-306) is a no-op; run.x is
    // deliberately never asserted below.
    layout.setText(font, str, 0, str.length(), Color.WHITE, targetWidth, Align.left.toInt, wrap, truncate)
    layout
  }

  private def glyphString(run: GlyphRun): String =
    (0 until run.glyphs.size).map(i => run.glyphs(i).id.toChar).mkString

  private def advances(run: GlyphRun): List[Float] =
    (0 until run.xAdvances.size).map(run.xAdvances(_)).toList

  private def colorsList(layout: GlyphLayout): List[Int] =
    (0 until layout.colors.size).map(layout.colors(_)).toList

  private val white = Color.WHITE.toIntBits()
  private val red   = Color.RED.toIntBits()

  /** CharSequence that throws after `maxCalls` charAt calls. Converts the documented infinite loop (ISS-488, leading newline) into a fast, deterministic failure on all platforms — a busy loop cannot
    * be interrupted on JS/Native, so a timeout is not an option. A correct layout of a 2-char string needs well under 100 charAt calls; 10000 is generous.
    */
  private final class CountingCharSequence(underlying: String, maxCalls: Int) extends CharSequence {
    private var calls = 0
    def length(): Int = underlying.length
    def charAt(index: Int): Char = {
      calls += 1
      if (calls > maxCalls) {
        throw new IllegalStateException(s"charAt called more than $maxCalls times for a ${underlying.length}-char string: setText is stuck in an infinite loop (ISS-488)")
      }
      underlying.charAt(index)
    }
    def subSequence(start: Int, end: Int): CharSequence = underlying.subSequence(start, end)
    override def toString(): String = underlying
  }

  // --- ISS-488: multi-line setText broken ------------------------------------

  test("ISS-488: setText(\"a\\nb\") lays out two runs on two lines") {
    // Java trace ("a\nb", no wrap/truncate, Align.left):
    //  - Java line 147 `str.charAt(start++)` consumes the '\n' (start advances
    //    past the delimiter); line 149 runEnd = start-1.
    //  - 1st run "a" at y=0 (lines 174-207); newline => lines 250-258:
    //    lineRun=null, y += down = -12, runStart = start = 2.
    //  - 2nd run "b" at y=-12 via the end-of-text branch (lines 141-144).
    //  - height = capHeight + |y| = 10 + 12 = 22 (line 264).
    // The port (GlyphLayout.scala lines 110-115) never advances `i` past the
    // newline; after the first run it either rescans the same '\n' or, on the
    // `runEnded=true` flag path (line 182-184, a mistranslation of Java's
    // labeled `break runEnded;` at line 209 which only exits the BLOCK, not
    // the loop), terminates the outer loop and silently drops "b".
    val layout = layoutOf(makeFont(markupEnabled = false), "a\nb")
    assertEquals(layout.runs.size, 2, "expected two runs, one per line (Java lines 141-262)")
    assertEquals(glyphString(layout.runs(0)), "a")
    assertEquals(glyphString(layout.runs(1)), "b")
    assertEqualsFloat(layout.runs(0).y, 0f, 0.0001f)
    assertEqualsFloat(layout.runs(1).y, -12f, 0.0001f, "second line must sit one `down` lower (Java line 258)")
    assertEquals(layout.glyphCount, 2)
    assertEqualsFloat(layout.height, 22f, 0.0001f, "height = capHeight + |down| (Java line 264)")
  }

  test("ISS-488: setText(\"\\na\") with a leading newline terminates and lays out one run") {
    // Java trace ("\na"): line 147 consumes '\n' (start 0 -> 1), runEnd=0.
    //  - Empty first run: lines 192-194 free it, lineRun==null => exit block.
    //  - newline && runEnd==runStart => blank line: y += down*blankLineScale
    //    = -12 (Java lines 255-256); runStart = 1.
    //  - Run "a" at y=-12 via end-of-text branch; height = 10+12 = 22.
    // The port never advances `i` for the newline case AND only processes a
    // run when `runEnd > 0 || isLastRun` (Scala line 142) — for a newline at
    // index 0, runEnd==0, so nothing happens and the outer while loop rescans
    // charAt(0) forever. CountingCharSequence turns that loop into an
    // IllegalStateException after 10000 charAt calls.
    val str    = new CountingCharSequence("\na", 10000)
    val layout = layoutOf(makeFont(markupEnabled = false), str)
    assertEquals(layout.runs.size, 1, "blank first line produces no run; only \"a\" (Java lines 192-194, 255-256)")
    assertEquals(glyphString(layout.runs(0)), "a")
    assertEqualsFloat(layout.runs(0).y, -12f, 0.0001f, "y += down * blankLineScale (Java line 256)")
    assertEquals(layout.glyphCount, 1)
    assertEqualsFloat(layout.height, 22f, 0.0001f)
  }

  test("ISS-488: leading color tag \"[RED]text\" does not emit the tag characters as glyphs") {
    // Java trace ("[RED]text", markup enabled):
    //  - line 147 consumes '[' (start 0 -> 1); parseColorMarkup returns 3
    //    ("RED"); line 156 runEnd = start-1 = 0; line 157 start += 4 => 5;
    //    line 161 nextColor = colorStack.peek() = RED.
    //  - The run [runStart=0, runEnd=0) IS processed (the runEnded block at
    //    lines 172-209 runs unconditionally for every delimiter): it is empty,
    //    glyphCount==0 == colors.get(0), so line 184 sets colors = [0, RED];
    //    line 261 runStart = start = 5.
    //  - Final run is str[5,9) = "text": 4 glyphs, colors stay [0, RED].
    // The port's `runEnd > 0 || isLastRun` sentinel (Scala line 142) conflates
    // "no delimiter" with "delimiter at index 0": the tag run is skipped,
    // runStart stays 0, and the final run renders str[0,9) = "[RED]text",
    // emitting '[','R','E','D',']' as glyphs.
    val layout = layoutOf(makeFont(markupEnabled = true), "[RED]text")
    assertEquals(layout.runs.size, 1)
    assertEquals(glyphString(layout.runs(0)), "text", "tag characters must not become glyphs (Java lines 152-168, 261)")
    assertEquals(layout.glyphCount, 4)
    assertEquals(colorsList(layout), List(0, red), "leading tag rewrites the base color entry in place (Java lines 182-184)")
  }

  // --- ISS-489: truncate is a no-op that aborts layout ------------------------

  test("ISS-489: truncate(\"..\") on overlong text yields truncated glyphs plus truncate glyphs") {
    // Java trace ("aaaaaa", targetWidth=35, truncate=".."):
    //  - xAdvances of the single run: [0,10,10,10,10,10,9] (last entry set to
    //    glyph width 9 by setLastGlyphXAdvance, Java lines 441-445).
    //  - Wrap/truncate scan (lines 213-227): runWidth 10 -> 20 -> 30; at i=4,
    //    30+9-eps > 35 => truncate(fontData, lineRun, 35, "..") + break outer.
    //  - truncate() (lines 309-361): truncateRun("..") xAdvances [0,4,3] =>
    //    truncateWidth = 7 (line 319-320), targetWidth 35-7 = 28.
    //  - Count loop (lines 328-333): widths 0,10,20 fit, 30 > 28 => count=3.
    //    NB: Java's `break` exits only this while loop; the rest of truncate()
    //    still runs. The port turned it into a method-level boundary.break in
    //    truncateRun (Scala lines 300, 322), so everything below is skipped.
    //  - count>1: glyphs.truncate(2) => "aa"; xAdvances.truncate(3) =>
    //    [0,10,10]; setLastGlyphXAdvance => [0,10,9]; append truncateRun
    //    advances [4,3] => [0,10,9,4,3] (lines 335-340).
    //  - glyphCount: 6 - 4 dropped + 2 truncate chars = 4 (lines 348-358).
    //  - Glyphs become "aa" + ".." = "aa.." (line 357).
    val layout = layoutOf(makeFont(markupEnabled = false), "aaaaaa", targetWidth = 35f, truncate = Nullable(".."))
    assertEquals(layout.runs.size, 1)
    assertEquals(glyphString(layout.runs(0)), "aa..", "run must hold 2 fitting glyphs + truncate glyphs (Java lines 335-357)")
    assertEquals(advances(layout.runs(0)), List(0f, 10f, 9f, 4f, 3f), "Java lines 337-340")
    assertEquals(layout.glyphCount, 4, "Java lines 348-358")
  }

  test("ISS-489: truncated layout still computes width and height (layout not aborted)") {
    // After Java's `break outer` (line 226-227) execution CONTINUES at line
    // 264: height = capHeight + |y| = 10, then calculateWidths (lines
    // 275-294) gives width = 0+10+9+4+3 = 26 for the "aa.." run, then
    // alignRuns. The port's call site (Scala lines 200-204) instead breaks
    // the METHOD-level setText boundary (Scala line 68), skipping all three,
    // so width/height are left at their reset() values of 0.
    val layout = layoutOf(makeFont(markupEnabled = false), "aaaaaa", targetWidth = 35f, truncate = Nullable(".."))
    assertEqualsFloat(layout.height, 10f, 0.0001f, "height must be set after truncation (Java line 264)")
    assertEqualsFloat(layout.width, 26f, 0.0001f, "width must be computed after truncation (Java lines 266, 275-294)")
  }

  // --- ISS-490: markup base color never pushed to colorStack ------------------

  test("ISS-490: \"[RED]foo[]bar\" pop tag restores the base color") {
    // Java pushes the base color onto colorStack when markup is enabled (line
    // 130: `if (markupEnabled) colorStack.add(currentColor);`). Trace:
    //  - [RED]: stack [WHITE,RED]; empty tag run sets colors = [0, RED]
    //    (lines 182-184).
    //  - "foo" run ends at "[]": parseColorMarkup pops RED (stack.size>1,
    //    lines 485-487) leaving [WHITE]; nextColor = peek() = WHITE (line
    //    161); glyphCount=3 => colors = [0, RED, 3, WHITE] (lines 186-187).
    //  - "bar" appended to the same line run => single run "foobar",
    //    glyphCount 6.
    // The port never pushes the base color (Scala lines 83-87), so the stack
    // is [RED] at the pop tag: size>1 is false, RED is never popped, and
    // colorStack.last keeps returning RED instead of WHITE.
    val layout = layoutOf(makeFont(markupEnabled = true), "[RED]foo[]bar")
    assertEquals(colorsList(layout), List(0, red, 3, white), "pop tag must restore the pushed base color (Java lines 130, 161, 485-487)")
    assertEquals(layout.runs.size, 1)
    assertEquals(glyphString(layout.runs(0)), "foobar")
    assertEquals(layout.glyphCount, 6)
  }

  test("ISS-490: \"[]\" pop tag before any push tag does not crash") {
    // Java ("[]bar", markup enabled): stack starts as [WHITE] (line 130);
    // the pop tag finds stack.size == 1, pops nothing (line 486), returns 0;
    // line 161 nextColor = colorStack.peek() = WHITE — no crash, no color
    // change. Result: one run "bar", colors [0, WHITE].
    // The port's stack starts EMPTY, so `nextColor = colorStack.last` (Scala
    // line 125) throws IndexOutOfBoundsException("Array is empty.").
    val layout = layoutOf(makeFont(markupEnabled = true), "[]bar")
    assertEquals(layout.runs.size, 1)
    assertEquals(glyphString(layout.runs(0)), "bar")
    assertEquals(colorsList(layout), List(0, white), "base color only (Java lines 128, 485-487)")
    assertEquals(layout.glyphCount, 3)
  }

  // --- ISS-491: wrap loop measures stale run / removeRange off-by-one ---------

  test("ISS-491: 3-line wrap yields 3 runs, each with xAdvances.size == glyphs.size+1") {
    // Java trace ("aa aa aa", wrap=true, targetWidth=30 (== spaceXadvance*3,
    // so line 124 keeps it), Align.left). Single initial run: 8 glyphs
    // [a,a,' ',a,a,' ',a,a], xAdvances [0,10,10,10,10,10,10,10,9].
    //  - Wrap scan (lines 213-246): runWidth 10 -> 20 -> 30; at i=4 the 4th
    //    glyph 'a' (width 9) doesn't fit (39 > 30) => wrapIndex =
    //    getWrapIndex(glyphs,4) = 3.
    //  - wrap() (lines 365-439): firstEnd=2 (whitespace skipped), secondStart
    //    =3. libgdx Array.removeRange / FloatArray.removeRange are
    //    end-INCLUSIVE: line 388 removeRange(0, secondStart-1) drops glyphs
    //    0..2; line 394 xAdvances2.removeRange(1, secondStart) drops indices
    //    1..3 (THREE entries), leaving [0,10,10,10,10,9] — size 6 == 5
    //    glyphs+1. lls DynamicArray.removeRange is end-EXCLUSIVE; the port
    //    adjusted line 382 (glyphs) but NOT line 388 (xAdvances), which drops
    //    only indices 1..2 and keeps a stale advance on every wrapped line.
    //  - Line 235 reassigns the `lineRun` LOCAL, and the loop (lines 214-219)
    //    re-reads lineRun for every measurement, so the second line "aa a"...
    //    is measured and wrapped AGAIN at its own 4th glyph => third line.
    //    The port binds `val lr = lineRun.get` once (Scala line 187) and keeps
    //    measuring the stale first line, so the second wrap never happens.
    //  - Result: 3 runs "aa"/"aa"/"aa" at y 0/-12/-24, each xAdvances
    //    [0,10,9]; dropped 2 whitespace glyphs => glyphCount 6; height =
    //    10 + 24 = 34; width = 10+9 = 19.
    val layout = layoutOf(makeFont(markupEnabled = false), "aa aa aa", targetWidth = 30f, wrap = true)
    for (i <- 0 until layout.runs.size) {
      val run = layout.runs(i)
      assertEquals(run.xAdvances.size, run.glyphs.size + 1, s"run $i (\"${glyphString(run)}\"): xAdvances must have glyphs.size+1 entries (GlyphRun contract, Java lines 530-534)")
    }
    assertEquals(layout.runs.size, 3, "two wraps must produce three lines (Java lines 235-245)")
    for (i <- 0 until 3) assertEquals(glyphString(layout.runs(i)), "aa", s"run $i")
    assertEquals(advances(layout.runs(1)), List(0f, 10f, 9f), "second line advances (Java lines 392-395, 441-445)")
    assertEquals(layout.glyphCount, 6, "one space dropped per wrap (Java lines 399-402)")
    assertEqualsFloat(layout.height, 34f, 0.0001f)
    assertEqualsFloat(layout.width, 19f, 0.0001f)
  }

  test("ISS-491: wrap where all wrapped glyphs are whitespace does not crash") {
    // Java trace ("aa   ", wrap=true, targetWidth=30): run [a,a,' ',' ',' '],
    // xAdvances [0,10,10,10,10,0] (trailing space width 0). Wrap scan:
    // runWidth 10 -> 20 -> 30 -> 40; at i=5 the 5th glyph ' ' doesn't fit
    // (40 > 30) => wrapIndex = 4. In wrap(), firstEnd=2, secondStart=5 ==
    // glyphCount => second run is null (all wrapped glyphs whitespace):
    // glyphs truncated to "aa", xAdvances to [0,10,9], glyphCount = 2 (lines
    // 412-428); wrap() returns null and line 236 `break runEnded` simply
    // continues the outer loop. Result: 1 run "aa", height 10.
    // The port resets lineRun to Nullable.empty (Scala lines 215-218) but
    // then unconditionally evaluates `lineRun.get` (Scala line 234), throwing
    // NullPointerException("Nullable.get called on empty value").
    val layout = layoutOf(makeFont(markupEnabled = false), "aa   ", targetWidth = 30f, wrap = true)
    assertEquals(layout.runs.size, 1, "whitespace-only second run is discarded (Java lines 412-429, 236)")
    assertEquals(glyphString(layout.runs(0)), "aa")
    assertEquals(advances(layout.runs(0)), List(0f, 10f, 9f))
    assertEquals(layout.glyphCount, 2, "trailing whitespace glyphs dropped (Java lines 417-419)")
    assertEqualsFloat(layout.height, 10f, 0.0001f)
  }
}
