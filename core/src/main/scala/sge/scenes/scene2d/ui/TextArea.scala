/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/TextArea.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import scala.util.boundary

import sge.graphics.g2d.{ Batch, BitmapFont, GlyphLayout }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, DynamicArray, Nullable }

/** A text input field with multiple lines. */
class TextArea(text: Nullable[String], style: TextField.TextFieldStyle)(using sge: Sge) extends TextField(text, style) {

  import TextField._

  /** Array storing lines breaks positions * */
  private[ui] var linesBreak: DynamicArray[Int] = scala.compiletime.uninitialized

  /** Last text processed. This attribute is used to avoid unnecessary computations while calculating offsets * */
  private var lastText: Nullable[String] = Nullable.empty

  /** Current line for the cursor * */
  private[ui] var cursorLine: Int = 0

  /** Index of the first line showed by the text area * */
  private[ui] var firstLineShowing: Int = 0

  /** Number of lines showed by the text area * */
  private var linesShowing: Int = 0

  /** Variable to maintain the x offset of the cursor when moving up and down. If it's set to -1, the offset is reset * */
  private[ui] var moveOffset: Float = 0

  private var prefRows: Float = 0

  // // TODO: uncomment when Skin is ported
  // def this(text: String, skin: Skin)(using Sge) = this(Nullable(text), skin.get(classOf[TextFieldStyle]))

  // // TODO: uncomment when Skin is ported
  // def this(text: String, skin: Skin, styleName: String)(using Sge) = this(Nullable(text), skin.get(styleName, classOf[TextFieldStyle]))

  override protected def initialize(): Unit = {
    super.initialize()
    writeEnters = true
    linesBreak = DynamicArray[Int]()
    cursorLine = 0
    firstLineShowing = 0
    moveOffset = -1
    linesShowing = 0
  }

  override protected def letterUnderCursor(x: Float): Int = boundary {
    if (linesBreak.size > 0) {
      if (cursorLine * 2 >= linesBreak.size) {
        boundary.break(_text.length())
      } else {
        val glyphPositionsItems = this.glyphPositions
        val start               = linesBreak.items(cursorLine * 2)
        val adjustedX           = x + glyphPositionsItems(start)
        val end                 = linesBreak.items(cursorLine * 2 + 1)
        var i                   = start
        boundary {
          while (i < end) {
            if (glyphPositionsItems(i) > adjustedX) boundary.break(())
            i += 1
          }
        }
        if (i > 0 && glyphPositionsItems(i) - adjustedX <= adjustedX - glyphPositionsItems(i - 1)) boundary.break(i)
        boundary.break(Math.max(0, i - 1))
      }
    } else {
      boundary.break(0)
    }
  }

  override def setStyle(style: TextField.TextFieldStyle): Unit = {
    // same as super(), just different textHeight. no super() so we don't do same work twice
    if (style == null) throw new IllegalArgumentException("style cannot be null.")
    this._style = style

    // no extra descent to fake line height
    textHeight = style.font.getCapHeight() - style.font.getDescent()
    if (_text != null) updateDisplayText()
    invalidateHierarchy()
  }

  /** Sets the preferred number of rows (lines) for this text area. Used to calculate preferred height */
  def setPrefRows(prefRows: Float): Unit =
    this.prefRows = prefRows

  override def getPrefHeight: Float =
    if (prefRows <= 0) {
      super.getPrefHeight
    } else {
      // without ceil we might end up with one less row then expected
      // due to how linesShowing is calculated in #sizeChanged and #getHeight() returning rounded value
      var prefHeight = Math.ceil(getStyle.font.getLineHeight() * prefRows).toFloat
      getStyle.background.foreach { bg =>
        prefHeight = Math.max(prefHeight + bg.getBottomHeight + bg.getTopHeight, bg.getMinHeight)
      }
      prefHeight
    }

  /** Returns total number of lines that the text occupies * */
  def getLines: Int =
    linesBreak.size / 2 + (if (newLineAtEnd()) 1 else 0)

  /** Returns if there's a new line at then end of the text * */
  def newLineAtEnd(): Boolean =
    _text.length() != 0 &&
      (_text.charAt(_text.length() - 1) == NEWLINE || _text.charAt(_text.length() - 1) == CARRIAGE_RETURN)

  /** Moves the cursor to the given number line * */
  def moveCursorLine(line: Int): Unit =
    if (line < 0) {
      cursorLine = 0
      cursor = 0
      moveOffset = -1
    } else if (line >= getLines) {
      val newLine = getLines - 1
      cursor = _text.length()
      if (line > getLines || newLine == cursorLine) {
        moveOffset = -1
      }
      cursorLine = newLine
    } else if (line != cursorLine) {
      if (moveOffset < 0) {
        moveOffset =
          if (linesBreak.size <= cursorLine * 2) 0
          else glyphPositions(cursor) - glyphPositions(linesBreak(cursorLine * 2))
      }
      cursorLine = line
      cursor = if (cursorLine * 2 >= linesBreak.size) _text.length() else linesBreak(cursorLine * 2)
      while (
        cursor < _text.length() && cursor <= linesBreak(cursorLine * 2 + 1) - 1
        && glyphPositions(cursor) - glyphPositions(linesBreak(cursorLine * 2)) < moveOffset
      )
        cursor += 1
      showCursor()
    }

  /** Updates the current line, checking the cursor position in the text * */
  private[ui] def updateCurrentLine(): Unit = {
    val index = calculateCurrentLineIndex(cursor)
    val line  = index / 2
    // Special case when cursor moves to the beginning of the line from the end of another and a word
    // wider than the box
    if (
      index % 2 == 0 || index + 1 >= linesBreak.size || cursor != linesBreak.items(index)
      || linesBreak.items(index + 1) != linesBreak.items(index)
    ) {
      if (
        line < linesBreak.size / 2 || _text.length() == 0 || _text.charAt(_text.length() - 1) == NEWLINE
        || _text.charAt(_text.length() - 1) == CARRIAGE_RETURN
      ) {
        cursorLine = line
      }
    }
    updateFirstLineShowing() // fix for drag-selecting text out of the TextArea's bounds
  }

  /** Scroll the text area to show the line of the cursor * */
  private[ui] def showCursor(): Unit = {
    updateCurrentLine()
    updateFirstLineShowing()
  }

  private[ui] def updateFirstLineShowing(): Unit =
    if (cursorLine != firstLineShowing) {
      val step = if (cursorLine >= firstLineShowing) 1 else -1
      while (firstLineShowing > cursorLine || firstLineShowing + linesShowing - 1 < cursorLine)
        firstLineShowing += step
    }

  /** Calculates the text area line for the given cursor position * */
  private def calculateCurrentLineIndex(cursor: Int): Int = {
    var index = 0
    while (index < linesBreak.size && cursor > linesBreak.items(index))
      index += 1
    index
  }

  // OVERRIDE from TextField

  override protected def sizeChanged(): Unit = {
    lastText = Nullable.empty // Cause calculateOffsets to recalculate the line breaks.

    // The number of lines showed must be updated whenever the height is updated
    val font            = getStyle.font
    val background      = getStyle.background
    val availableHeight = getHeight - background.fold(0f)(bg => bg.getBottomHeight + bg.getTopHeight)
    linesShowing = Math.floor(availableHeight / font.getLineHeight()).toInt
  }

  override protected def getTextY(font: BitmapFont, background: Nullable[Drawable]): Float = {
    var textY = getHeight
    background.foreach { bg =>
      textY = textY - bg.getTopHeight
    }
    if (font.usesIntegerPositions()) textY = textY.toInt.toFloat
    textY
  }

  override protected def drawSelection(selection: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    var i          = firstLineShowing * 2
    var offsetY    = 0f
    val minIndex   = Math.min(cursor, selectionStart)
    val maxIndex   = Math.max(cursor, selectionStart)
    val fontData   = font.getData()
    val lineHeight = getStyle.font.getLineHeight()
    while (i + 1 < linesBreak.size && i < (firstLineShowing + linesShowing) * 2) {

      val lineStart = linesBreak(i)
      val lineEnd   = linesBreak(i + 1)

      if (
        !((minIndex < lineStart && minIndex < lineEnd && maxIndex < lineStart && maxIndex < lineEnd)
          || (minIndex > lineStart && minIndex > lineEnd && maxIndex > lineStart && maxIndex > lineEnd))
      ) {

        val start = Math.max(lineStart, minIndex)
        val end   = Math.min(lineEnd, maxIndex)

        var fontLineOffsetX     = 0f
        var fontLineOffsetWidth = 0f
        // We can't use fontOffset as it is valid only for first glyph/line in the text.
        // We will grab first character in this line and calculate proper offset for this line.
        val lineFirst: Nullable[BitmapFont.Glyph] = fontData.getGlyph(displayText.charAt(lineStart))
        lineFirst.foreach { glyph =>
          // See BitmapFontData.getGlyphs() for offset calculation.
          // If selection starts when line starts we want to offset width instead of moving the start as it looks better.
          if (start == lineStart) {
            fontLineOffsetWidth = if (glyph.fixedWidth) 0 else -glyph.xoffset * fontData.scaleX - fontData.padLeft
          } else {
            fontLineOffsetX = if (glyph.fixedWidth) 0 else -glyph.xoffset * fontData.scaleX - fontData.padLeft
          }
        }
        val selectionX     = glyphPositions(start) - glyphPositions(lineStart)
        val selectionWidth = glyphPositions(end) - glyphPositions(start)
        selection.draw(batch, x + selectionX + fontLineOffsetX, y - lineHeight - offsetY, selectionWidth + fontLineOffsetWidth, font.getLineHeight())
      }

      offsetY += font.getLineHeight()
      i += 2
    }
  }

  override protected def drawText(batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    var offsetY = -(getStyle.font.getLineHeight() - textHeight) / 2
    var i       = firstLineShowing * 2
    while (i < (firstLineShowing + linesShowing) * 2 && i < linesBreak.size) {
      font.draw(batch, displayText, x, y + offsetY, linesBreak.items(i), linesBreak.items(i + 1), 0, Align.left.toInt, false)
      offsetY -= font.getLineHeight()
      i += 2
    }
  }

  override protected def drawCursor(cursorPatch: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float): Unit =
    cursorPatch.draw(batch, x + getCursorX, y + getCursorY, cursorPatch.getMinWidth, font.getLineHeight())

  override protected def calculateOffsets(): Unit = {
    super.calculateOffsets()
    if (!this._text.equals(lastText.orNull)) {
      this.lastText = Nullable(_text)
      val font         = getStyle.font
      val maxWidthLine = this.getWidth -
        getStyle.background.fold(0f)(bg => bg.getLeftWidth + bg.getRightWidth)
      linesBreak.clear()
      var lineStart = 0
      var lastSpace = 0
      var lastCharacter: Char = 0
      val layout = Actor.POOLS.obtain(classOf[GlyphLayout])
      var i      = 0
      while (i < _text.length()) {
        lastCharacter = _text.charAt(i)
        if (lastCharacter == CARRIAGE_RETURN || lastCharacter == NEWLINE) {
          linesBreak.add(lineStart)
          linesBreak.add(i)
          lineStart = i + 1
        } else {
          lastSpace = if (continueCursor(i, 0)) lastSpace else i
          layout.setText(font, _text.subSequence(lineStart, i + 1))
          if (layout.width > maxWidthLine) {
            if (lineStart >= lastSpace) {
              lastSpace = i - 1
            }
            linesBreak.add(lineStart)
            linesBreak.add(lastSpace + 1)
            lineStart = lastSpace + 1
            lastSpace = lineStart
          }
        }
        i += 1
      }
      Actor.POOLS.free(layout)
      // Add last line
      if (lineStart < _text.length()) {
        linesBreak.add(lineStart)
        linesBreak.add(_text.length())
      }
      showCursor()
    }
  }

  override protected def createInputListener(): InputListener =
    new TextAreaListener()

  override def setSelection(selectionStart: Int, selectionEnd: Int): Unit = {
    super.setSelection(selectionStart, selectionEnd)
    updateCurrentLine()
  }

  override protected def moveCursor(forward: Boolean, jump: Boolean): Unit = {
    val count = if (forward) 1 else -1
    val index = (cursorLine * 2) + count
    if (
      index >= 0 && index + 1 < linesBreak.size && linesBreak.items(index) == cursor
      && linesBreak.items(index + 1) == cursor
    ) {
      cursorLine += count
      if (jump) {
        super.moveCursor(forward, jump)
      }
      showCursor()
    } else {
      super.moveCursor(forward, jump)
    }
    updateCurrentLine()
  }

  override protected def continueCursor(index: Int, offset: Int): Boolean = {
    val pos = calculateCurrentLineIndex(index + offset)
    super.continueCursor(index, offset) && (pos < 0 || pos >= linesBreak.size - 2 || (linesBreak.items(pos + 1) != index)
      || (linesBreak.items(pos + 1) == linesBreak.items(pos + 2)))
  }

  def getCursorLine: Int = cursorLine

  def getFirstLineShowing: Int = firstLineShowing

  def getLinesShowing: Int = linesShowing

  def getCursorX: Float = {
    var textOffset = 0f
    val fontData   = getStyle.font.getData()
    if (!(cursor >= glyphPositions.size || cursorLine * 2 >= linesBreak.size)) {
      val lineStart   = linesBreak.items(cursorLine * 2)
      var glyphOffset = 0f
      val lineFirst: Nullable[BitmapFont.Glyph] = fontData.getGlyph(displayText.charAt(lineStart))
      lineFirst.foreach { glyph =>
        // See BitmapFontData.getGlyphs() for offset calculation.
        glyphOffset = if (glyph.fixedWidth) 0 else -glyph.xoffset * fontData.scaleX - fontData.padLeft
      }
      textOffset = glyphPositions(cursor) - glyphPositions(lineStart) + glyphOffset
    }
    textOffset + fontData.cursorX
  }

  def getCursorY: Float = {
    val font = getStyle.font
    -(cursorLine - firstLineShowing + 1) * font.getLineHeight()
  }

  /** Input listener for the text area * */
  class TextAreaListener extends TextFieldClickListener {

    override protected def setCursorPosition(x: Float, y: Float): Unit = {
      moveOffset = -1

      val background = getStyle.background
      val font       = getStyle.font

      val height = getHeight

      var adjustedX      = x
      var adjustedY      = y
      var adjustedHeight = height

      background.foreach { bg =>
        adjustedHeight -= bg.getTopHeight
        adjustedX -= bg.getLeftWidth
      }
      adjustedX = Math.max(0, adjustedX)
      background.foreach { bg =>
        adjustedY -= bg.getTopHeight
      }

      cursorLine = Math.floor((adjustedHeight - adjustedY) / font.getLineHeight()).toInt + firstLineShowing
      cursorLine = Math.max(0, Math.min(cursorLine, getLines - 1))

      super.setCursorPosition(adjustedX, adjustedY)
      updateCurrentLine()
    }

    override def keyDown(event: InputEvent, keycode: Int): Boolean = {
      val result = super.keyDown(event, keycode)
      if (hasKeyboardFocus) {
        var repeat = false
        val shift  = sge.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || sge.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
        if (keycode == Input.Keys.DOWN) {
          if (shift) {
            if (!hasSelection) {
              selectionStart = cursor
              hasSelection = true
            }
          } else {
            clearSelection()
          }
          moveCursorLine(cursorLine + 1)
          repeat = true

        } else if (keycode == Input.Keys.UP) {
          if (shift) {
            if (!hasSelection) {
              selectionStart = cursor
              hasSelection = true
            }
          } else {
            clearSelection()
          }
          moveCursorLine(cursorLine - 1)
          repeat = true

        } else {
          moveOffset = -1
        }
        if (repeat) {
          scheduleKeyRepeatTask(keycode)
        }
        showCursor()
        true
      } else {
        result
      }
    }

    override protected def checkFocusTraversal(character: Char): Boolean =
      focusTraversal && character == TAB

    override def keyTyped(event: InputEvent, character: Char): Boolean = {
      val result = super.keyTyped(event, character)
      showCursor()
      result
    }

    override protected def goHome(jump: Boolean): Unit =
      if (jump) {
        cursor = 0
      } else if (cursorLine * 2 < linesBreak.size) {
        cursor = linesBreak(cursorLine * 2)
      }

    override protected def goEnd(jump: Boolean): Unit =
      if (jump || cursorLine >= getLines) {
        cursor = _text.length()
      } else if (cursorLine * 2 + 1 < linesBreak.size) {
        cursor = linesBreak(cursorLine * 2 + 1)
      }
  }
}
