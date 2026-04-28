/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/GlyphLayout.java
 * Original authors: Nathan Sweet, davebaol, Alexander Dorokhov, Thomas Creutzenberg
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: DynamicArray instead of libGDX Array/FloatArray
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: wrapGlyphs returns Nullable[GlyphRun]; getGlyphs accepts Nullable[BitmapFont.Glyph]; truncateRun passes Nullable.empty.
 *   Issues: lineRun/lastGlyph local vars in setText now use Nullable; .get used where non-null is guaranteed by control flow.
 *   Convention: GlyphLayout + GlyphRun extend Pool.Poolable; Poolable type class auto-derived via Poolable.fromTrait
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 570
 * Covenant-baseline-methods: GlyphLayout,GlyphRun,adjustedTargetWidth,alignRuns,appendRun,buffer,calculateWidths,colorStack,colors,count,currentColor,down,droppedGlyphCount,first,firstEnd,fontData,getGlyphWidth,getLineOffset,glyphCount,glyphRunPool,glyphs,glyphs2,height,i,isLastRun,last,lastGlyph,lineRun,markupEnabled,maxWidth,nextColor,parseColorMarkup,reset,runEnded,runStart,runs,second,secondStart,setLastGlyphXAdvance,setText,this,toString,truncateRun,truncateWidth,width,wrapGlyphs,wrapOrTruncate,x,xAdvances,xAdvances2,y
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/GlyphLayout.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: a729bf1f0de099ebcc60562d72f008157677b559
 */
package sge
package graphics
package g2d

import sge.graphics.{ Color, Colors }
import sge.utils.{ DynamicArray, Nullable, Pool }
import sge.utils.Pool.Poolable

import scala.language.implicitConversions

class GlyphLayout extends Poolable {
  import GlyphLayout.glyphRunPool

  val runs:       DynamicArray[GlyphRun] = DynamicArray[GlyphRun]()
  val colors:     DynamicArray[Int]      = DynamicArray[Int]()
  var glyphCount: Int                    = 0
  var width:      Float                  = 0f
  var height:     Float                  = 0f

  def this(font: BitmapFont, str: CharSequence) = {
    this()
    setText(font, str)
  }

  def this(font: BitmapFont, str: CharSequence, color: Color, targetWidth: Float, halign: Int, wrap: Boolean) = {
    this()
    setText(font, str, color, targetWidth, halign, wrap)
  }

  def this(font: BitmapFont, str: CharSequence, start: Int, end: Int, color: Color, targetWidth: Float, halign: Int, wrap: Boolean, truncate: Nullable[String]) = {
    this()
    setText(font, str, start, end, color, targetWidth, halign, wrap, truncate)
  }

  def setText(font: BitmapFont, str: CharSequence): Unit =
    setText(font, str, 0, str.length(), font.color, 0, 0, false, Nullable.empty[String]) // Align.left = 0

  def setText(font: BitmapFont, str: CharSequence, color: Color, targetWidth: Float, halign: Int, wrap: Boolean): Unit =
    setText(font, str, 0, str.length(), color, targetWidth, halign, wrap, Nullable.empty[String])

  def setText(font: BitmapFont, str: CharSequence, start: Int, end: Int, color: Color, targetWidth: Float, halign: Int, wrap: Boolean, truncate: Nullable[String]): Unit = scala.util.boundary {

    reset()

    val fontData = font.data
    if (start == end) { // Empty string.
      height = fontData.capHeight
      scala.util.boundary.break(())
    }

    // Avoid wrapping one line per character, which is very inefficient.
    var adjustedTargetWidth = targetWidth
    if (wrap) adjustedTargetWidth = Math.max(targetWidth, fontData.spaceXadvance * 3)
    val wrapOrTruncate = wrap || truncate.isDefined

    var currentColor = color.toIntBits()
    var nextColor    = currentColor
    colors.add(0)
    colors.add(currentColor)
    val markupEnabled = fontData.markupEnabled

    var isLastRun = false
    var y         = 0f
    val down      = fontData.down
    var lineRun:   Nullable[GlyphRun]         = Nullable.empty // Collects glyphs for the current line.
    var lastGlyph: Nullable[BitmapFont.Glyph] = Nullable.empty // Last glyph of the previous run on the same line, used for kerning between runs.
    var runStart = start
    var i        = start

    var runEnded = false
    while (!runEnded) {
      var runEnd: Int = 0
      var newline = false
      if (i == end) { // End of text.
        if (runStart == end) scala.util.boundary.break(()) // No run to process, we're done.
        runEnd = end // Process the final run.
        isLastRun = true
      } else {
        var delimFound = false
        scala.util.boundary {
          while (i < end)
            // Each run is delimited by newline or left square bracket.
            str.charAt(i) match {
              case '\n' => // End of line.
                runEnd = i
                newline = true
                delimFound = true
                scala.util.boundary.break(())
              case '[' => // Possible color tag.
                if (markupEnabled) {
                  val length = parseColorMarkup(str, i + 1, end)
                  if (length >= 0) {
                    runEnd = i
                    i += length + 2
                    if (i == end)
                      isLastRun = true // Color tag is the last element in the string.
                    else
                      nextColor = colorStack.last
                    delimFound = true
                    scala.util.boundary.break(())
                  } else if (length == -2) {
                    i += 1 // Skip first of "[[" escape sequence.
                  }
                }
                if (!delimFound) i += 1
              case _ =>
                i += 1
            }
        }
        if (runEnd == 0 && !delimFound) {
          // Continue to next iteration
        }
      }

      if (runEnd > 0 || isLastRun) {
        // Store the run that has ended.
        val run = glyphRunPool.obtain()
        run.x = 0
        run.y = y
        fontData.getGlyphs(run, str, runStart, runEnd, lastGlyph)
        glyphCount += run.glyphs.size

        if (nextColor != currentColor) { // Can only be different if markupEnabled.
          if (colors(colors.size - 2) == glyphCount) {
            // Consecutive color changes, or after an empty run, or at the beginning of the string.
            colors(colors.size - 1) = nextColor
          } else {
            colors.add(glyphCount)
            colors.add(nextColor)
          }
          currentColor = nextColor
        }

        if (run.glyphs.isEmpty) {
          glyphRunPool.free(run)
          if (lineRun.isEmpty) {
            runEnded = true // Otherwise wrap and truncate must still be processed for lineRun.
          }
        } else if (lineRun.isEmpty) {
          lineRun = Nullable(run)
          runs.add(run)
        } else {
          lineRun.get.appendRun(run)
          glyphRunPool.free(run)
        }

        if (newline || isLastRun) {
          setLastGlyphXAdvance(fontData, lineRun.get)
          lastGlyph = Nullable.empty
        } else {
          val lr = lineRun.get
          lastGlyph = if (lr.glyphs.nonEmpty) Nullable(lr.glyphs.last) else Nullable.empty
        }

        if (!wrapOrTruncate || lineRun.get.glyphs.isEmpty) {
          runEnded = true // No wrap or truncate, or no glyphs.
        }

        if (!runEnded && (newline || isLastRun)) {
          val lr = lineRun.get
          // Wrap or truncate. First xadvance is the first glyph's X offset relative to the drawing position.
          var runWidth = lr.xAdvances(0) + lr.xAdvances(1) // At least the first glyph will fit.
          var ii       = 2
          var wrapping = true
          while (wrapping && ii < lr.xAdvances.size) {
            val glyph      = lr.glyphs(ii - 1)
            val glyphWidth = getGlyphWidth(glyph, fontData)
            if (runWidth + glyphWidth - 0.0001f <= adjustedTargetWidth) {
              // Glyph fits.
              runWidth += lr.xAdvances(ii)
              ii += 1
            } else {
              truncate.foreach { truncateStr =>
                // Truncate.
                truncateRun(fontData, lr, adjustedTargetWidth, truncateStr)
                scala.util.boundary.break(())
              }

              // Wrap.
              var wrapIndex = fontData.getWrapIndex(lr.glyphs, ii)
              if (
                (wrapIndex == 0 && lr.x == 0) // Require at least one glyph per line.
                || wrapIndex >= lr.glyphs.size
              ) { // Wrap at least the glyph that didn't fit.
                wrapIndex = ii - 1
              }
              val wrappedLine = wrapGlyphs(fontData, lr, wrapIndex)
              if (wrappedLine.isEmpty) {
                lineRun = Nullable.empty // Reset sentinel — all wrapped glyphs were whitespace.
                runEnded = true
                wrapping = false // Prevent while-loop from accessing Nullable.empty lineRun.
              } else {
                val wl = wrappedLine.get
                lineRun = wrappedLine
                runs.add(wl)

                y += down
                wl.x = 0
                wl.y = y

                // Start the wrap loop again, another wrap might be necessary.
                runWidth = wl.xAdvances(0) + wl.xAdvances(1) // At least the first glyph will fit.
                ii = 2
              }
            }
          }
          if (ii >= lineRun.get.xAdvances.size) wrapping = false
        }

        if (newline) {
          lineRun = Nullable.empty
          lastGlyph = Nullable.empty

          // Next run will be on the next line.
          if (runEnd == runStart) // Blank line.
            y += down * fontData.blankLineScale
          else
            y += down
        }

        runStart = i
        if (isLastRun) runEnded = true
      }
    }

    height = fontData.capHeight + Math.abs(y)

    calculateWidths(fontData)

    alignRuns(adjustedTargetWidth, halign)

    // Clear the color stack.
    if (markupEnabled) colorStack.clear()
  }

  private def calculateWidths(fontData: BitmapFontData): Unit = {
    var maxWidth = 0f
    var i        = 0
    while (i < runs.size) {
      val run       = runs(i)
      val xAdvances = run.xAdvances
      var runWidth  = run.x + xAdvances(0)
      var max       = 0f // run.x is needed to ensure floats are rounded same as above.
      val glyphs    = run.glyphs
      var ii        = 0
      while (ii < glyphs.size) {
        val glyph      = glyphs(ii)
        val glyphWidth = getGlyphWidth(glyph, fontData)
        max = Math.max(max, runWidth + glyphWidth) // A glyph can extend past the right edge of subsequent glyphs.
        if (ii + 1 < xAdvances.size) {
          runWidth += xAdvances(ii + 1)
        }
        ii += 1
      }
      run.width = Math.max(runWidth, max) - run.x
      maxWidth = Math.max(maxWidth, run.x + run.width)
      i += 1
    }
    this.width = maxWidth
  }

  private def alignRuns(targetWidth: Float, halign: Int): Unit =
    if ((halign & 1) == 0) { // Not left aligned, so must be center or right aligned. (Align.left = 1)
      val center = (halign & 2) != 0 // Align.center = 2
      var i      = 0
      while (i < runs.size) {
        val run = runs(i)
        run.x += (if (center) 0.5f * (targetWidth - run.width) else targetWidth - run.width)
        i += 1
      }
    }

  private def truncateRun(fontData: BitmapFontData, run: GlyphRun, targetWidth: Float, truncate: String): Unit = scala.util.boundary {
    val glyphCount = run.glyphs.size

    // Determine truncate string size.
    val truncateRun = glyphRunPool.obtain()
    fontData.getGlyphs(truncateRun, truncate, 0, truncate.length(), Nullable.empty)
    var truncateWidth = 0f
    if (truncateRun.xAdvances.nonEmpty) {
      setLastGlyphXAdvance(fontData, truncateRun)
      val xAdvances = truncateRun.xAdvances
      for (i <- 1 until truncateRun.xAdvances.size) // Skip first for tight bounds.
        truncateWidth += xAdvances(i)
    }
    val adjustedTargetWidth = targetWidth - truncateWidth

    // Determine visible glyphs.
    var count     = 0
    var width     = run.x
    val xAdvances = run.xAdvances
    while (count < run.xAdvances.size) {
      val xAdvance = xAdvances(count)
      width += xAdvance
      if (width > adjustedTargetWidth) scala.util.boundary.break(())
      count += 1
    }

    if (count > 1) {
      // Some run glyphs fit, append truncate glyphs.
      run.glyphs.removeRange(count - 1, run.glyphs.size)
      run.xAdvances.removeRange(count, run.xAdvances.size)
      setLastGlyphXAdvance(fontData, run)
      if (truncateRun.xAdvances.nonEmpty) {
        run.xAdvances.addAll(truncateRun.xAdvances.items, 1, truncateRun.xAdvances.size - 1)
      }
    } else {
      // No run glyphs fit, use only truncate glyphs.
      run.glyphs.clear()
      run.xAdvances.clear()
      run.xAdvances.addAll(truncateRun.xAdvances)
    }

    val droppedGlyphCount = glyphCount - run.glyphs.size
    if (droppedGlyphCount > 0) {
      this.glyphCount -= droppedGlyphCount
      if (fontData.markupEnabled) {
        while (colors.size > 2 && colors(colors.size - 2) >= this.glyphCount) {
          colors.removeIndex(colors.size - 2)
          colors.removeIndex(colors.size - 1)
        }
      }
    }

    run.glyphs.addAll(truncateRun.glyphs)
    this.glyphCount += truncate.length()

    glyphRunPool.free(truncateRun)
  }

  private def wrapGlyphs(fontData: BitmapFontData, first: GlyphRun, wrapIndex: Int): Nullable[GlyphRun] = scala.util.boundary {
    val glyphs2    = first.glyphs // Starts with all the glyphs.
    val glyphCount = first.glyphs.size
    val xAdvances2 = first.xAdvances // Starts with all the xadvances.

    // Skip whitespace before the wrap index.
    var firstEnd = wrapIndex
    while (firstEnd > 0 && fontData.isWhitespace(glyphs2(firstEnd - 1).id.toChar))
      firstEnd -= 1

    // Skip whitespace after the wrap index.
    var secondStart = wrapIndex
    while (secondStart < glyphCount && fontData.isWhitespace(glyphs2(secondStart).id.toChar))
      secondStart += 1

    // Copy wrapped glyphs and xadvances to second run.
    // The second run will contain the remaining glyph data, so swap instances rather than copying.
    var second: Nullable[GlyphRun] = Nullable.empty
    if (secondStart < glyphCount) {
      val s = glyphRunPool.obtain()
      second = s

      val glyphs1 = DynamicArray[BitmapFont.Glyph]() // Starts empty.
      glyphs1.addAll(glyphs2.items, 0, firstEnd)
      glyphs2.removeRange(0, secondStart)
      first.glyphs = glyphs1
      s.glyphs = glyphs2

      val xAdvances1 = DynamicArray[Float]() // Starts empty.
      xAdvances1.addAll(xAdvances2.items, 0, firstEnd + 1)
      xAdvances2.removeRange(1, secondStart) // Leave first entry to be overwritten by next line.
      xAdvances2(0) = getLineOffset(glyphs2, fontData)
      first.xAdvances = xAdvances1
      s.xAdvances = xAdvances2

      val firstGlyphCount   = first.glyphs.size // After wrapping it.
      val secondGlyphCount  = s.glyphs.size
      val droppedGlyphCount = glyphCount - firstGlyphCount - secondGlyphCount
      this.glyphCount -= droppedGlyphCount

      if (fontData.markupEnabled && droppedGlyphCount > 0) {
        val reductionThreshold = this.glyphCount - secondGlyphCount
        var i                  = colors.size - 2
        scala.util.boundary {
          while (i >= 2) { // i >= 1 because first 2 values always determine the base color.
            val colorChangeIndex = colors(i)
            if (colorChangeIndex <= reductionThreshold) scala.util.boundary.break(())
            colors(i) = colorChangeIndex - droppedGlyphCount
            i -= 2
          }
        }
      }
    } else {
      // Second run is empty, just trim whitespace glyphs from end of first run.
      glyphs2.removeRange(firstEnd, glyphs2.size)
      xAdvances2.removeRange(firstEnd + 1, xAdvances2.size)

      val droppedGlyphCount = secondStart - firstEnd
      if (droppedGlyphCount > 0) {
        this.glyphCount -= droppedGlyphCount
        if (fontData.markupEnabled && colors(colors.size - 2) > this.glyphCount) {
          // Many color changes can be hidden in the dropped whitespace, so keep only the very last color entry.
          val lastColor = colors.last
          while (colors(colors.size - 2) > this.glyphCount) {
            colors.removeIndex(colors.size - 2)
            colors.removeIndex(colors.size - 1)
          }
          colors(colors.size - 2) = this.glyphCount // Update color change index.
          colors(colors.size - 1) = lastColor // Update color entry.
        }
      }
    }

    if (firstEnd == 0) {
      // If the first run is now empty, remove it.
      glyphRunPool.free(first)
      runs.removeIndex(runs.size - 1)
    } else {
      setLastGlyphXAdvance(fontData, first)
    }

    second
  }

  private def setLastGlyphXAdvance(fontData: BitmapFontData, run: GlyphRun): Unit = {
    val last = run.glyphs.last
    if (!last.fixedWidth) run.xAdvances(run.xAdvances.size - 1) = getGlyphWidth(last, fontData)
  }

  private def getGlyphWidth(glyph: BitmapFont.Glyph, fontData: BitmapFontData): Float =
    (if (glyph.fixedWidth) glyph.xadvance else glyph.width + glyph.xoffset) * fontData.scaleX - fontData.padRight

  private def getLineOffset(glyphs: DynamicArray[BitmapFont.Glyph], fontData: BitmapFontData): Float = {
    val first = glyphs.first
    (if (first.fixedWidth) 0 else -first.xoffset * fontData.scaleX) - fontData.padLeft
  }

  private val colorStack = DynamicArray[Int]()

  private def parseColorMarkup(str: CharSequence, start: Int, end: Int): Int = scala.util.boundary {
    if (start == end) scala.util.boundary.break(-1) // String ended with "[".
    str.charAt(start) match {
      case '#' =>
        // Parse hex color RRGGBBAA to an ABGR int, where AA is optional and defaults to FF if omitted.
        var color = 0
        var i     = start + 1
        while (i < end) {
          val ch = str.charAt(i)
          if (ch == ']') {
            if (i < start + 2 || i > start + 9) scala.util.boundary.break(-1) // Illegal number of hex digits.
            if (i - start < 8) color = color << (9 - (i - start) << 2) | 0xff // RRGGBB or fewer chars.
            colorStack.add(Integer.reverseBytes(color))
            scala.util.boundary.break(i - start)
          }
          color = (color << 4) + ch
          if (ch >= '0' && ch <= '9')
            color -= '0'
          else if (ch >= 'A' && ch <= 'F')
            color -= 'A' - 10
          else if (ch >= 'a' && ch <= 'f')
            color -= 'a' - 10
          else
            scala.util.boundary.break(-1) // Unexpected character in hex color.
          i += 1
        }
        -1
      case '[' => // "[[" is an escaped left square bracket.
        -2
      case ']' => // "[]" is a "pop" color tag.
        if (colorStack.size > 1) colorStack.removeIndex(colorStack.size - 1)
        0
      case _ =>
        // Parse named color.
        var i = start + 1
        while (i < end) {
          val ch = str.charAt(i)
          if (ch != ']') {
            i += 1
          } else {
            val color = Colors.get(str.subSequence(start, i).toString())
            if (color.isEmpty) scala.util.boundary.break(-1) // Unknown color name.
            colorStack.add(color.map(_.toIntBits()).getOrElse(0))
            scala.util.boundary.break(i - start)
          }
        }
        -1 // Unclosed color tag.
    }
  }

  def reset(): Unit = {
    glyphRunPool.freeAll(runs)
    runs.clear()
    colors.clear()
    glyphCount = 0
    width = 0
    height = 0
  }

  override def toString(): String =
    if (runs.isEmpty) ""
    else {
      val buffer = new StringBuilder(128)
      buffer.append(width)
      buffer.append('x')
      buffer.append(height)
      buffer.append('\n')
      var i = 0
      while (i < runs.size) {
        buffer.append(runs(i).toString())
        buffer.append('\n')
        i += 1
      }
      buffer.setLength(buffer.length() - 1)
      buffer.toString()
    }
}

object GlyphLayout {
  private[g2d] val glyphRunPool: Pool[GlyphRun] = Pool.Default[GlyphRun](() => GlyphRun())
}

class GlyphRun extends Poolable {
  var glyphs:    DynamicArray[BitmapFont.Glyph] = DynamicArray[BitmapFont.Glyph]()
  var xAdvances: DynamicArray[Float]            = DynamicArray[Float]()
  var x:         Float                          = 0f
  var y:         Float                          = 0f
  var width:     Float                          = 0f

  def appendRun(run: GlyphRun): Unit = {
    glyphs.addAll(run.glyphs)
    // Remove the width of the last glyph. The first xadvance of the appended run has kerning for the last glyph of this run.
    if (xAdvances.nonEmpty) xAdvances.removeIndex(xAdvances.size - 1)
    xAdvances.addAll(run.xAdvances)
  }

  def reset(): Unit = {
    glyphs.clear()
    xAdvances.clear()
  }

  override def toString(): String = {
    val buffer = new StringBuilder(glyphs.size + 32)
    var i      = 0
    while (i < glyphs.size) {
      val g = glyphs(i)
      buffer.append(g.id.toChar)
      i += 1
    }
    buffer.append(", ")
    buffer.append(x)
    buffer.append(", ")
    buffer.append(y)
    buffer.append(", ")
    buffer.append(width)
    buffer.toString()
  }
}
