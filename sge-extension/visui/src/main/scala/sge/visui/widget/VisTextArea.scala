/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab (few modifications, original TextArea is missing authors)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 440
 * Covenant-baseline-methods: ENTER_ANDROID,ENTER_DESKTOP,TextAreaListener,VisTextArea,_cursorXField,availableHeight,background,calculateCurrentLineIndex,calculateOffsets,continueCursor,count,createInputListener,cursorLine,cursorX,cursorY,drawCursor,drawSelection,drawText,firstLineShowing,font,getTextY,goEnd,goHome,i,index,initialize,keyDown,keyTyped,lastText,letterUnderCursor,line,lines,linesBreak,linesShowing,maxIndex,minIndex,moveCursor,moveCursorLine,moveOffset,newLineAtEnd,offsetY,pos,prefHeight,prefRows,setCursorPosition,setPrefRows,setSelection,showCursor,sizeChanged,softwrap,textOffset,textY,this,updateCurrentLine
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisTextArea.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import scala.util.boundary

import sge.Input.Key
import sge.graphics.g2d.{ Batch, BitmapFont, GlyphLayout }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, DynamicArray, Nullable }
import sge.visui.VisUI

/** A multiple-line text input field, entirely based on [[sge.scenes.scene2d.ui.TextField]].
  *
  * Extends [[VisTextField]] to inherit all VisUI-specific features (focus border, error border, input validity state, backgroundOver, cursor percent height, read-only mode, I-beam cursor on hover,
  * FocusManager integration) while providing multi-line text editing functionality equivalent to [[sge.scenes.scene2d.ui.TextArea]].
  * @author
  *   Kotcrab (few modifications, original [[sge.scenes.scene2d.ui.TextArea]] is missing authors)
  * @see
  *   [[sge.scenes.scene2d.ui.TextArea]]
  */
class VisTextArea(text: Nullable[String], visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends VisTextField(text, visStyle) {

  /** Character constants matching TextField's ENTER_DESKTOP / ENTER_ANDROID */
  private val ENTER_DESKTOP: Char = '\r'
  private val ENTER_ANDROID: Char = '\n'

  /** Array storing lines breaks positions * */
  private[sge] var linesBreak: DynamicArray[Int] = scala.compiletime.uninitialized

  /** Last text processed. This attribute is used to avoid unnecessary computations while calculating offsets * */
  private var lastText: Nullable[String] = Nullable.empty

  /** Current line for the cursor * */
  private[visui] var cursorLine: Int = 0

  /** Index of the first line showed by the text area * */
  private[sge] var firstLineShowing: Int = 0

  /** Number of lines showed by the text area * */
  private[sge] var linesShowing: Int = 0

  /** Variable to maintain the x offset of the cursor when moving up and down. If it's set to -1, the offset is reset * */
  private[visui] var moveOffset: Float = 0

  private var prefRows: Float = 0

  /** Allows to disable, enable disabling softwrapping. Note this isn't exposed property because TextArea can't handle it by default. You must have text area which can calculate its max width such as
    * [[HighlightTextArea]]
    */
  private[visui] var softwrap: Boolean = true
  private var _cursorXField:   Float   = 0

  def this()(using Sge) = this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String, styleName: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))
  def this(text: String, style:     VisTextField.VisTextFieldStyle)(using Sge) = this(Nullable(text), style)

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
        if (i > 0 && glyphPositionsItems(i) - adjustedX <= adjustedX - glyphPositionsItems(i - 1)) boundary.break(Math.min(i, _text.length()))
        boundary.break(Math.max(0, i - 1))
      }
    } else {
      boundary.break(0)
    }
  }

  /** Sets the preferred number of rows (lines) for this text area. Used to calculate preferred height */
  def setPrefRows(prefRows: Float): Unit =
    this.prefRows = prefRows

  override def prefHeight: Float =
    if (prefRows <= 0) {
      super.prefHeight
    } else {
      var prefHeight = textHeight * prefRows
      style.background.foreach { bg =>
        prefHeight = Math.max(prefHeight + bg.bottomHeight + bg.topHeight, bg.minHeight)
      }
      prefHeight
    }

  /** Returns total number of lines that the text occupies * */
  def lines: Int =
    linesBreak.size / 2 + (if (newLineAtEnd()) 1 else 0)

  /** Returns if there's a new line at then end of the text * */
  def newLineAtEnd(): Boolean =
    _text.length() != 0 &&
      (_text.charAt(_text.length() - 1) == ENTER_ANDROID || _text.charAt(_text.length() - 1) == ENTER_DESKTOP)

  /** Moves the cursor to the given number line * */
  def moveCursorLine(line: Int): Unit =
    if (line < 0) {
      cursorLine = 0
      cursor = 0
      moveOffset = -1
    } else if (line >= lines) {
      val newLine = lines - 1
      cursor = _text.length()
      if (line > lines || newLine == cursorLine) {
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
  private[visui] def updateCurrentLine(): Unit = {
    val index = calculateCurrentLineIndex(cursor)
    val line  = index / 2
    // Special case when cursor moves to the beginning of the line from the end of another and a word
    // wider than the box
    if (
      index % 2 == 0 || index + 1 >= linesBreak.size || cursor != linesBreak.items(index)
      || linesBreak.items(index + 1) != linesBreak.items(index)
    ) {
      if (
        line < linesBreak.size / 2 || _text.length() == 0 || _text.charAt(_text.length() - 1) == ENTER_ANDROID
        || _text.charAt(_text.length() - 1) == ENTER_DESKTOP
      ) {
        cursorLine = line
      }
    }
  }

  /** Scroll the text area to show the line of the cursor * */
  private[visui] def showCursor(): Unit = {
    updateCurrentLine()
    if (cursorLine != firstLineShowing) {
      val step = if (cursorLine >= firstLineShowing) 1 else -1
      while (firstLineShowing > cursorLine || firstLineShowing + linesShowing - 1 < cursorLine)
        firstLineShowing += step
    }
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
    val font            = style.font
    val background      = style.background
    val availableHeight = height - background.map(bg => bg.bottomHeight + bg.topHeight).getOrElse(0f)
    linesShowing = Math.floor(availableHeight / font.lineHeight).toInt
  }

  override protected def getTextY(font: BitmapFont, background: Nullable[Drawable]): Float = {
    var textY = height
    background.foreach { bg =>
      textY = (textY - bg.topHeight).toInt.toFloat
    }
    textY
  }

  override protected def drawSelection(selection: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    var i        = firstLineShowing * 2
    var offsetY  = 0f
    val minIndex = Math.min(cursor, _selectionStart)
    val maxIndex = Math.max(cursor, _selectionStart)
    while (i + 1 < linesBreak.size && i < (firstLineShowing + linesShowing) * 2) {

      val lineStart = linesBreak(i)
      val lineEnd   = linesBreak(i + 1)

      if (
        !((minIndex < lineStart && minIndex < lineEnd && maxIndex < lineStart && maxIndex < lineEnd)
          || (minIndex > lineStart && minIndex > lineEnd && maxIndex > lineStart && maxIndex > lineEnd))
      ) {

        val start = Math.max(linesBreak(i), minIndex)
        val end   = Math.min(linesBreak(i + 1), maxIndex)

        val selectionX     = glyphPositions(start) - glyphPositions(linesBreak(i))
        val selectionWidth = glyphPositions(end) - glyphPositions(start)

        selection.draw(batch, x + selectionX + fontOffset, y - textHeight - font.descent - offsetY, selectionWidth, font.lineHeight)
      }

      offsetY += font.lineHeight
      i += 2
    }
  }

  override protected def drawText(batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    var offsetY = 0f
    var i       = firstLineShowing * 2
    while (i < (firstLineShowing + linesShowing) * 2 && i < linesBreak.size) {
      font.draw(batch, displayText, x, y + offsetY, linesBreak.items(i), linesBreak.items(i + 1), 0, Align.left.toInt, false)
      offsetY -= font.lineHeight
      i += 2
    }
  }

  override protected def drawCursor(cursorPatch: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    val textOffset =
      if (cursor >= glyphPositions.size || cursorLine * 2 >= linesBreak.size) 0f
      else glyphPositions(cursor) - glyphPositions(linesBreak.items(cursorLine * 2))
    _cursorXField = textOffset + fontOffset + font.data.cursorX
    cursorPatch.draw(
      batch,
      x + _cursorXField,
      y - font.descent / 2 - (cursorLine - firstLineShowing + 1) * font.lineHeight,
      cursorPatch.minWidth,
      font.lineHeight
    )
  }

  override protected def calculateOffsets(): Unit = {
    super.calculateOffsets()
    if (lastText.forall(_ != this._text)) {
      this.lastText = Nullable(_text)
      val font         = style.font
      val maxWidthLine = this.width -
        style.background.map(bg => bg.leftWidth + bg.rightWidth).getOrElse(0f)
      linesBreak.clear()
      var lineStart = 0
      var lastSpace = 0
      var lastCharacter: Char = 0
      val layout = Actor.POOLS.obtain[GlyphLayout]
      var i      = 0
      while (i < _text.length()) {
        lastCharacter = _text.charAt(i)
        if (lastCharacter == ENTER_DESKTOP || lastCharacter == ENTER_ANDROID) {
          linesBreak.add(lineStart)
          linesBreak.add(i)
          lineStart = i + 1
        } else {
          lastSpace = if (continueCursor(i, 0)) lastSpace else i
          layout.setText(font, _text.subSequence(lineStart, i + 1))
          if (layout.width > maxWidthLine && softwrap) {
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
    TextAreaListener()

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

  def cursorX: Float = _cursorXField

  def cursorY: Float = {
    val font = style.font
    -(-font.descent / 2 - (cursorLine - firstLineShowing + 1) * font.lineHeight)
  }

  /** Input listener for the text area * */
  class TextAreaListener extends TextFieldClickListener {

    override protected def setCursorPosition(x: Float, y: Float): Unit = {
      moveOffset = -1

      val background = style.background
      val font       = style.font

      val height = VisTextArea.this.height

      var adjustedX      = x
      var adjustedY      = y
      var adjustedHeight = height

      background.foreach { bg =>
        adjustedHeight -= bg.topHeight
        adjustedX -= bg.leftWidth
      }
      adjustedX = Math.max(0, adjustedX)
      background.foreach { bg =>
        adjustedY -= bg.topHeight
      }

      cursorLine = Math.floor((adjustedHeight - adjustedY) / font.lineHeight).toInt + firstLineShowing
      cursorLine = Math.max(0, Math.min(cursorLine, lines - 1))

      super.setCursorPosition(adjustedX, adjustedY)
      updateCurrentLine()
    }

    override def keyDown(event: InputEvent, keycode: Key): Boolean = {
      val result = super.keyDown(event, keycode)
      if (hasKeyboardFocus) {
        var repeat = false
        val shift  = Sge().input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Sge().input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
        if (keycode == Input.Keys.DOWN) {
          if (shift) {
            if (!hasSelection) {
              _selectionStart = cursor
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
              _selectionStart = cursor
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
      if (jump || cursorLine >= lines) {
        cursor = _text.length()
      } else if (cursorLine * 2 + 1 < linesBreak.size) {
        cursor = linesBreak(cursorLine * 2 + 1)
      }
  }
}
