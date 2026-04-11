/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraField.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Disableable → disabled field,
 *     FloatArray → ArrayBuffer[Float], Clipboard → Sge clipboard (deferred),
 *     InputListener/ClickListener → handler methods, Timer.Task → timer state,
 *     Replacer (RegExodus) → deferred (emoji replacement),
 *     UIUtils → helper methods, ChangeEvent → direct callback
 *   Convention: Full text-editing behavior preserved: cursor movement,
 *     selection, copy/cut/paste, password mode, undo, key repeat, draw.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

/** A single-line text input field using a Font. */
class TextraField {

  /** Used as the default char to replace content when passwordMode is on. */
  val BULLET: Char = 8226.toChar // u2022, or bullet

  protected var text:        String      = ""
  protected var cursor:      Int         = 0
  protected var writeEnters: Boolean     = false
  protected var label:       TypingLabel = new TypingLabel()

  protected val glyphPositions: ArrayBuffer[Float] = ArrayBuffer.empty

  protected var style:          Nullable[Styles.TextFieldStyle] = Nullable.empty
  protected var messageText:    Nullable[String]                = Nullable.empty
  protected var showingMessage: Boolean                         = false
  protected var focusTraversal: Boolean                         = true
  protected var onlyFontChars:  Boolean                         = true
  protected var disabled:       Boolean                         = false
  protected var textHAlign:     Int                             = Align.left.toInt

  protected var undoText:       String = ""
  protected var lastChangeTime: Long   = 0

  protected var passwordMode:      Boolean = false
  protected var passwordCharacter: Char    = BULLET

  protected var fontOffset:       Float = 0f
  protected var textOffset:       Float = 0f
  protected var renderOffset:     Float = 0f
  protected var visibleTextStart: Int   = 0
  protected var visibleTextEnd:   Int   = 0
  protected var maxLength:        Int   = 0

  protected var focused:      Boolean = false
  protected var cursorOn:     Boolean = false
  protected var blinkEnabled: Boolean = true
  protected var blinkTime:    Float   = 0.32f

  protected var programmaticChangeEvents: Boolean = false

  // Listener/filter fields
  protected var listener: Nullable[TextFieldListener] = Nullable.empty
  protected var filter:   Nullable[TextFieldFilter]   = Nullable.empty

  // Clipboard contents held locally; will connect to Sge clipboard when available
  private var _clipboardContents: String = ""

  // Blink state
  private var _blinkTimer: Float = 0f

  // Key repeat state
  private var _keyRepeatKeycode: Int     = -1
  private var _keyRepeatTimer:   Float   = 0f
  private var _keyRepeatActive:  Boolean = false

  // Widget-like fields
  private var _x:      Float = 0f
  private var _y:      Float = 0f
  private var _width:  Float = 0f
  private var _height: Float = 0f
  private val _color:  Color = new Color(Color.WHITE)

  // Keyboard focus state
  private var _hasKeyboardFocus: Boolean = false

  // --- Constants ---
  private val BACKSPACE:       Char = '\b'
  private val CARRIAGE_RETURN: Char = '\r'
  private val NEWLINE:         Char = '\n'
  private val TAB:             Char = '\t'
  private val DELETE:          Char = 127.toChar

  // --- Constructors ---

  def this(text: Nullable[String], style: Styles.TextFieldStyle) = {
    this()
    val s = new Styles.TextFieldStyle(style)
    Nullable.foreach(s.font) { f =>
      f.enableSquareBrackets = false
      f.omitCurlyBraces = false
    }
    this.style = Nullable(s)
    Nullable.foreach(s.font) { f =>
      f.enableSquareBrackets = false
      f.omitCurlyBraces = false
      label = new TypingLabel("", new Styles.LabelStyle(f, style.fontColor))
      label.workingLayout.targetWidth = Float.MaxValue
      label.workingLayout.setMaxLines(1)
      label.setWrap(false)
      label.setSelectable(true)
      Nullable.foreach(style.selection) {
        case d: Drawable => label.selectionDrawable = Nullable(d)
        case _ => ()
      }
    }
    initialize()
    setText(text)
    label.setSize(getPrefWidth, getPrefHeight)
    label.skipToTheEnd(true, true)
    updateDisplayText()
  }

  def this(text: Nullable[String], style: Styles.TextFieldStyle, replacementFont: Font) = {
    this()
    this.style = Nullable(style)
    val rf = new Font(replacementFont)
    rf.enableSquareBrackets = false
    rf.omitCurlyBraces = false
    label = new TypingLabel("", new Styles.LabelStyle(rf, style.fontColor))
    label.workingLayout.targetWidth = Float.MaxValue
    label.workingLayout.setMaxLines(1)
    label.setWrap(false)
    label.setSelectable(true)
    Nullable.foreach(style.selection) {
      case d: Drawable => label.selectionDrawable = Nullable(d)
      case _ => ()
    }
    initialize()
    setText(text)
    label.setSize(getPrefWidth, getPrefHeight)
    label.skipToTheEnd(true, true)
    updateDisplayText()
  }

  protected def initialize(): Unit = {
    // Clipboard will be connected when scene2d/Sge integration lands.
    // Emoji replacer requires EmojiProcessor integration.
  }

  // --- Widget-like accessors ---

  def getX:                Float = _x
  def getY:                Float = _y
  def setX(x:      Float): Unit  = _x = x
  def setY(y:      Float): Unit  = _y = y
  def getWidth:            Float = _width
  def getHeight:           Float = _height
  def setWidth(w:  Float): Unit  = _width = w
  def setHeight(h: Float): Unit  = _height = h
  def getColor:            Color = _color
  def setColor(c:  Color): Unit  = if (c != null) _color.set(c)

  def setPosition(x: Float, y: Float): Unit = {
    _x = x
    _y = y
    label.setPosition(x, y)
  }

  def setSize(width: Float, height: Float): Unit = {
    _width = width
    _height = height
    label.setSize(width, height)
    updateDisplayText()
  }

  def hasKeyboardFocus:                 Boolean = _hasKeyboardFocus
  def setKeyboardFocus(focus: Boolean): Unit    = _hasKeyboardFocus = focus

  // --- Style ---

  def setStyle(style: Styles.TextFieldStyle): Unit = {
    require(style != null, "style cannot be null.")
    this.style = Nullable(style)
    if (text != null) updateDisplayText()
  }

  def getStyle: Nullable[Styles.TextFieldStyle] = style

  // --- Text ---

  /** Sets the text. If null, "" is used. */
  def setText(str: Nullable[String]): Unit = {
    val s = Nullable.fold(str)("")(identity)
    if (s != text) {
      clearSelection()
      val oldText = text
      text = ""
      label.layout.clear()
      cursor = 0
      paste(Nullable(s), fireChangeEvent = false)
      if (programmaticChangeEvents) {
        changeText(oldText, text)
      }
      cursor = 0
    }
  }

  def getText: String = text

  /** Appends text at the cursor position. */
  def appendText(str: Nullable[String]): Unit = {
    val s = Nullable.fold(str)("")(identity)
    clearSelection()
    cursor = text.length
    paste(Nullable(s), programmaticChangeEvents)
  }

  // --- Max length ---

  def withinMaxLength(size: Int): Boolean =
    maxLength <= 0 || size < maxLength

  def setMaxLength(maxLength: Int): Unit =
    this.maxLength = maxLength

  def getMaxLength: Int = maxLength

  // --- Font chars ---

  def setOnlyFontChars(onlyFontChars: Boolean): Unit =
    this.onlyFontChars = onlyFontChars

  // --- Word/space detection ---

  protected def isWordCharacter(c: Char): Boolean =
    c.isLetterOrDigit || c == '_'

  protected def isSpaceCharacter(c: Char): Boolean =
    c == ' ' || c == '\t'

  protected def isWordCharacter(glyph: Long): Boolean =
    isWordCharacter((glyph & 0xffff).toChar)

  protected def isSpaceCharacter(glyph: Long): Boolean =
    isSpaceCharacter((glyph & 0xffff).toChar)

  /** Finds the word under the cursor. Returns (left, right) packed as a Long. */
  protected def wordUnderCursor(): Long = boundary {
    val lb = this.label
    if (label.overIndex < 0) {
      break(lb.length().toLong - 1)
    }
    val len   = lb.length()
    val start = label.overIndex
    var right = len - 1
    var left  = start
    var index = start

    if (start >= len) {
      index = len - 1
      while (index > -1)
        if (isSpaceCharacter(lb.getInWorkingLayout(index))) {
          left = Math.min(len - 1, index + 1)
          index = -1 // break
        } else {
          left = index
          index -= 1
        }
    } else {
      index = start
      while (index < len)
        if (isSpaceCharacter(lb.getInWorkingLayout(index))) {
          right = Math.max(0, index - 1)
          index = len // break
        } else {
          right = index
          index += 1
        }
      index = start - 1
      while (index >= 0)
        if (isSpaceCharacter(lb.getInWorkingLayout(index))) {
          left = Math.min(len - 1, index + 1)
          index = -1 // break
        } else {
          left = index
          index -= 1
        }
    }
    left.toLong << 32 | (right & 0xffffffffL)
  }

  // --- Calculate offsets ---

  protected def calculateOffsets(): Unit = boundary {
    var visibleWidth = _width
    Nullable.foreach(getBackgroundDrawable) { bg =>
      visibleWidth -= bg.leftWidth + bg.rightWidth
    }

    val glyphCount = this.glyphPositions.size
    if (glyphCount == 0) {
      break(())
    }

    cursor = Math.max(0, Math.min(cursor, glyphCount - 1))
    val distance = glyphPositions(Math.max(0, cursor - 1)) + renderOffset
    if (distance <= 0) {
      renderOffset -= distance
    } else {
      val index = Math.min(glyphCount - 1, cursor + 1)
      val minX  = glyphPositions(index) - visibleWidth
      if (-renderOffset < minX) renderOffset = -minX
    }

    // Prevent renderOffset from starting too close to the end
    var maxOffset = 0f
    val width2    = glyphPositions(glyphCount - 1)
    var i         = glyphCount - 2
    while (i >= 0) {
      val x = glyphPositions(i)
      if (width2 - x > visibleWidth) {
        i = -1 // break
      } else {
        maxOffset = x
        i -= 1
      }
    }
    if (-renderOffset > maxOffset) renderOffset = -maxOffset

    // Calculate first visible char
    visibleTextStart = 0
    var startX = 0f
    i = 0
    while (i < glyphCount)
      if (glyphPositions(i) >= -renderOffset) {
        visibleTextStart = i
        startX = glyphPositions(i)
        i = glyphCount // break
      } else {
        i += 1
      }

    // Calculate last visible char
    var end  = visibleTextStart + 1
    val endX = visibleWidth - renderOffset
    val n    = Math.min(label.length(), glyphCount)
    while (end <= n && glyphPositions(end) <= endX)
      end += 1
    visibleTextEnd = Math.max(0, end - 1)

    if ((textHAlign & Align.left.toInt) == 0) {
      textOffset = visibleWidth - glyphPositions(visibleTextEnd) - fontOffset + startX
      if ((textHAlign & Align.center.toInt) != 0) textOffset = Math.round(textOffset * 0.5f).toFloat
    } else {
      textOffset = startX + renderOffset
    }
  }

  // --- Background ---

  protected def getBackgroundDrawable: Nullable[Drawable] = boundary {
    Nullable.foreach(style) { s =>
      if (disabled) {
        Nullable.foreach(s.disabledBackground) {
          case d: Drawable => break(Nullable(d))
          case _ => ()
        }
      }
      if (_hasKeyboardFocus) {
        Nullable.foreach(s.focusedBackground) {
          case d: Drawable => break(Nullable(d))
          case _ => ()
        }
      }
      Nullable.foreach(s.background) {
        case d: Drawable => break(Nullable(d))
        case _ => ()
      }
    }
    Nullable.empty
  }

  // --- Draw ---

  def draw(batch: Batch, parentAlpha: Float): Unit = boundary {
    val wasFocused = focused
    focused = _hasKeyboardFocus
    if (focused != wasFocused) {
      cursorOn = focused
      if (focused && blinkEnabled) {
        _blinkTimer = 0f
      }
    } else if (!focused) {
      cursorOn = false
    }

    val s = Nullable.fold(style)(null: Styles.TextFieldStyle)(identity)
    if (s == null) { break(()) }

    val font      = label.getFont
    val fontColor = if (disabled) {
      Nullable.fold(s.disabledFontColor)(Nullable.fold(s.fontColor)(Color.WHITE)(identity))(identity)
    } else if (focused) {
      Nullable.fold(s.focusedFontColor)(Nullable.fold(s.fontColor)(Color.WHITE)(identity))(identity)
    } else {
      Nullable.fold(s.fontColor)(Color.WHITE)(identity)
    }

    val cursorPatch = Nullable.fold(s.cursor)(null: Drawable) {
      case d: Drawable => d
      case _ => null
    }

    val background = getBackgroundDrawable

    val color  = _color
    val x      = _x
    val y      = _y
    val width  = _width
    val height = _height

    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
    var bgLeftWidth = 0f
    Nullable.foreach(background) { bg =>
      bg.draw(batch, x, y, width, height)
      bgLeftWidth = bg.leftWidth
    }

    val textY = getTextY(font, background)

    if (label.length() == 0) {
      if ((!focused || disabled) && messageText.isDefined) {
        Nullable.foreach(style) { st =>
          Nullable.fold(st.messageFontColor) {
            label.setColor(0.7f, 0.7f, 0.7f, color.a)
          } { mc =>
            label.setColor(mc.r, mc.g, mc.b, mc.a * color.a)
          }
        }
        Nullable.foreach(messageText) { msg =>
          label.restart(msg)
          label.skipToTheEnd(false, false)
          showingMessage = true
          updateDisplayText()
          calculateOffsets()
          label.setPosition(x + bgLeftWidth + textOffset, y + textY)
          label.drawSection(batch, parentAlpha, visibleTextStart, visibleTextEnd)
        }
      } else if (focused && !disabled) {
        if (showingMessage) clearMessage()
        calculateOffsets()
      } else {
        calculateOffsets()
      }
    } else {
      label.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * color.a)
      calculateOffsets()
      label.setPosition(x + bgLeftWidth + textOffset, y + textY)
      label.drawSection(batch, parentAlpha, visibleTextStart, visibleTextEnd)
    }
    if (!disabled && cursorOn && cursorPatch != null) {
      drawCursor(cursorPatch, batch, font, x + bgLeftWidth, y + textY)
    }
  }

  protected def getTextY(font: Font, background: Nullable[Drawable]): Float = {
    var textY = 0f
    Nullable.foreach(background) { bg =>
      val bottom = bg.bottomHeight
      textY = (-bg.topHeight - bottom) * 0.5f + bottom
    }
    if (font.integerPosition) textY = textY.toInt.toFloat
    textY
  }

  protected def drawCursor(cursorPatch: Drawable, batch: Batch, font: Font, x: Float, y: Float): Unit = {
    val lineHeight = label.getLineHeight(cursor)
    if (cursor < glyphPositions.size && visibleTextStart < glyphPositions.size) {
      cursorPatch.draw(
        batch,
        x + textOffset + glyphPositions(cursor) - glyphPositions(visibleTextStart) + fontOffset,
        y + lineHeight * 0.5f,
        cursorPatch.minWidth,
        lineHeight
      )
    }
  }

  // --- Update display text ---

  protected def updateDisplayText(): Unit = {
    val font = label.getFont
    font.defaultValue = font.mapping.getOrElse(' '.toInt, null) // @nowarn — Java interop: HashMap default

    var newText = text.replace('\r', ' ').replace('\n', ' ')
    Nullable.foreach(messageText) { msg =>
      if (newText.isEmpty) {
        newText = msg
        showingMessage = true
      } else if (showingMessage) {
        showingMessage = false
      }
    }
    if (!messageText.isDefined && newText.isEmpty && !showingMessage) {
      // no message text and empty -- just restart with empty
    }
    label.restart(newText)

    if (passwordMode && font.mapping.contains(passwordCharacter.toInt)) {
      val passwordGlyph = java.lang.Integer.reverseBytes(java.lang.Float.floatToIntBits(label.workingLayout.baseColor)).toLong << 32 | passwordCharacter.toLong
      var ln            = 0
      while (ln < label.workingLayout.lines.size) {
        Nullable.foreach(label.workingLayout.getLine(ln)) { line =>
          var gi = 0
          while (gi < line.glyphs.size) {
            line.glyphs(gi) = passwordGlyph
            gi += 1
          }
        }
        ln += 1
      }
    }
    label.skipToTheEnd(true, true)

    var end = 0f
    if (label.workingLayout.lines.nonEmpty) {
      glyphPositions.clear()
      // Calculate x advances from the working layout glyph data
      end = calculateXAdvancesFromLayout()
    } else {
      fontOffset = 0
    }
    glyphPositions += end
    visibleTextStart = Math.min(visibleTextStart, glyphPositions.size - 1)
    visibleTextEnd = Math.max(visibleTextStart, Math.min(visibleTextEnd, glyphPositions.size - 1))

    label.selectionStart = Math.min(label.selectionStart, label.length() - 1)
  }

  /** Calculates x advances from the working layout and populates glyphPositions. */
  private def calculateXAdvancesFromLayout(): Float = {
    val font = label.getFont
    var x    = 0f
    if (label.workingLayout.lines.nonEmpty) {
      Nullable.foreach(label.workingLayout.getLine(0)) { line =>
        var i = 0
        while (i < line.glyphs.size) {
          glyphPositions += x
          val glyph  = line.glyphs(i)
          val ch     = (glyph & 0xffff).toChar
          val region = font.mapping.getOrElse(ch.toInt, null) // @nowarn — Java interop
          if (region != null) {
            x += region.xAdvance * font.scaleX
          } else {
            x += font.cellWidth * font.scaleX
          }
          i += 1
        }
      }
    }
    x
  }

  // --- Copy/Cut/Paste ---

  /** Copies the selected contents to the clipboard. */
  def copy(): Unit =
    if (label.hasSelection && !passwordMode) {
      val start  = Math.min(label.selectionStart, label.selectionEnd)
      val end    = Math.max(label.selectionStart, label.selectionEnd)
      val toCopy = label.substring(Math.max(0, start), Math.min(label.length(), end + 1))
      _clipboardContents = toCopy
    }

  /** Copies the selected contents to the clipboard, then removes it. */
  def cut(): Unit =
    cut(programmaticChangeEvents)

  protected def cut(fireChangeEvent: Boolean): Unit =
    if (label.hasSelection && !passwordMode) {
      copy()
      cursor = delete(fireChangeEvent)
      updateDisplayText()
    }

  protected def paste(content: Nullable[String], fireChangeEvent: Boolean): Unit =
    Nullable.foreach(content) { c =>
      val sb         = new StringBuilder()
      var textLength = label.length()
      if (label.hasSelection) textLength -= Math.abs(cursor - label.selectionStart)

      val mapping = label.getFont.mapping
      var i       = 0
      while (i < c.length)
        if (!withinMaxLength(textLength + sb.length)) {
          i = c.length // break
        } else {
          var ch = c.charAt(i)
          if (!(writeEnters && (ch == NEWLINE || ch == CARRIAGE_RETURN))) {
            if (ch == '\r' || ch == '\n') { i += 1 }
            else {
              if (onlyFontChars && !mapping.contains(ch.toInt)) ch = '\u200b' // zero-width space
              Nullable.foreach(filter) { f =>
                if (!f.acceptChar(this, ch)) { ch = 0 }
              }
              if (ch != 0) sb.append(ch)
              i += 1
            }
          } else {
            sb.append(ch)
            i += 1
          }
        }

      if (label.hasSelection) {
        cursor = delete(fireChangeEvent)
      }
      if (fireChangeEvent) {
        changeText(cursor, sb)
      } else {
        insert(cursor, sb)
      }
      val bLen = sb.length
      sb.setLength(0)
      text = label.layout.appendIntoDirect(sb).toString
      updateDisplayText()
      cursor += bLen
    }

  protected def insert(position: Int, inserting: CharSequence): Boolean =
    if (inserting.length == 0) {
      false
    } else {
      if (showingMessage) {
        showingMessage = false
        label.layout.clear()
      }
      label.insertInLayout(label.layout, position, inserting)
      true
    }

  protected def insert(position: Int, text: CharSequence, to: String): String =
    if (to.isEmpty) text.toString
    else to.substring(0, position) + text + to.substring(position)

  protected def delete(fireChangeEvent: Boolean): Int = boundary {
    val from = label.selectionStart
    if (showingMessage) {
      break(label.selectionStart)
    }
    Nullable.foreach(label.layout.getLine(0)) { line =>
      val glyphs = line.glyphs
      if (glyphs.nonEmpty && label.selectionEnd >= 0 && label.selectionStart <= label.selectionEnd) {
        val removeEnd = Math.max(Math.min(glyphs.size - 1, label.selectionEnd), 0)
        var ri        = removeEnd
        while (ri >= label.selectionStart) {
          glyphs.remove(ri)
          ri -= 1
        }
      }
    }
    if (fireChangeEvent) {
      changeText(text, label.layout.appendIntoDirect(new StringBuilder()).toString)
    } else {
      text = label.layout.appendIntoDirect(new StringBuilder()).toString
    }
    clearSelection()
    from
  }

  // --- Change text ---

  def changeText(oldText: String, newText: String): Boolean =
    if (newText == oldText) {
      false
    } else {
      text = newText
      // In full scene2d integration, fire ChangeEvent and check cancellation.
      true
    }

  def changeText(position: Int, inserting: CharSequence): Boolean =
    // In full scene2d, this would fire a ChangeEvent and potentially cancel.
    insert(position, inserting)

  // --- Selection ---

  def getSelectionStart: Int = label.selectionStart
  def getSelectionEnd:   Int = label.selectionEnd

  def getSelection: String = label.getSelectedText()

  def setSelection(selectionStart: Int, selectionEnd: Int): Unit =
    if (selectionStart < 0 || selectionEnd < 0) {
      cursor = 0
      clearSelection()
    } else {
      val ss = Math.min(text.length - 1, selectionStart)
      val se = Math.min(text.length - 1, selectionEnd)
      label.selectionStart = Math.min(ss, se)
      cursor = { label.selectionEnd = Math.max(ss, se); label.selectionEnd + 1 }
    }

  def selectAll(): Unit =
    setSelection(0, text.length - 1)

  def clearSelection(): Unit = {
    label.selectionStart = -1
    label.selectionEnd = -1
  }

  // --- Cursor ---

  def setCursorPosition(cursorPosition: Int): Unit = {
    if (cursorPosition < 0) throw new IllegalArgumentException("cursorPosition must be >= 0")
    clearSelection()
    cursor = Math.min(cursorPosition, text.length)
  }

  def getCursorPosition: Int = cursor

  def getCursor: Int = cursor

  def setCursor(cursor: Int): Unit =
    this.cursor = cursor

  private def clearMessage(): Unit = {
    showingMessage = false
    label.restart { text = ""; text }
    label.skipToTheEnd(true, true)
    var end = 0f
    if (label.workingLayout.lines.nonEmpty) {
      glyphPositions.clear()
      end = calculateXAdvancesFromLayout()
    } else {
      fontOffset = 0
    }
    glyphPositions += end
    visibleTextStart = 0
    visibleTextEnd = 0
    clearSelection()
  }

  // --- Pref size ---

  def getPrefWidth: Float = 150f

  def getPrefHeight: Float = {
    val s            = Nullable.fold(style)(new Styles.TextFieldStyle())(identity)
    var topAndBottom = 0f
    var minHeight    = 0f
    Nullable.foreach(s.background) {
      case bg: Drawable =>
        topAndBottom = Math.max(topAndBottom, bg.bottomHeight + bg.topHeight)
        minHeight = Math.max(minHeight, bg.minHeight)
      case _ => ()
    }
    Nullable.foreach(s.focusedBackground) {
      case bg: Drawable =>
        topAndBottom = Math.max(topAndBottom, bg.bottomHeight + bg.topHeight)
        minHeight = Math.max(minHeight, bg.minHeight)
      case _ => ()
    }
    Nullable.foreach(s.disabledBackground) {
      case bg: Drawable =>
        topAndBottom = Math.max(topAndBottom, bg.bottomHeight + bg.topHeight)
        minHeight = Math.max(minHeight, bg.minHeight)
      case _ => ()
    }
    Math.max(topAndBottom + label.getFont.cellHeight, minHeight)
  }

  // --- Alignment ---

  def setAlignment(alignment: Int): Unit =
    this.textHAlign = alignment

  def getAlignment: Int = textHAlign

  // --- Act ---

  def act(delta: Float): Unit = {
    label.act(delta)

    // Blink cursor
    if (focused && blinkEnabled) {
      _blinkTimer += delta
      if (_blinkTimer >= blinkTime) {
        _blinkTimer -= blinkTime
        cursorOn = !cursorOn
      }
    }

    // Key repeat
    if (_keyRepeatActive) {
      _keyRepeatTimer -= delta
      if (_keyRepeatTimer <= 0f) {
        _keyRepeatTimer = TextraField.keyRepeatTime
        handleKeyDown(_keyRepeatKeycode)
      }
    }
  }

  // --- Password mode ---

  def setPasswordMode(passwordMode: Boolean): Unit =
    if (this.passwordMode != passwordMode) {
      this.passwordMode = passwordMode
      updateDisplayText()
    }

  def isPasswordMode: Boolean = passwordMode

  def getPasswordCharacter: Char = passwordCharacter

  def setPasswordCharacter(passwordCharacter: Char): Unit =
    if (
      label.getFont.mapping.contains(passwordCharacter.toInt) &&
      this.passwordCharacter != passwordCharacter && this.passwordMode
    ) {
      this.passwordCharacter = passwordCharacter
      updateDisplayText()
    }

  // --- Blink ---

  def getBlinkTime: Float = blinkTime

  def setBlinkTime(blinkTime: Float): Unit =
    this.blinkTime = blinkTime

  def isCursorBlinking: Boolean = blinkEnabled

  def setCursorBlinking(blinkEnabled: Boolean): Unit =
    this.blinkEnabled = blinkEnabled

  // --- Disabled ---

  def setDisabled(disabled: Boolean): Unit =
    this.disabled = disabled

  def isDisabled: Boolean = disabled

  // --- Cursor movement ---

  protected def moveCursor(forward: Boolean, jump: Boolean): Unit = {
    val limit      = if (forward) text.length else 0
    val charOffset = if (forward) 0 else -1
    if (forward) {
      cursor += 1
      while (cursor < limit && jump && continueCursor(cursor, charOffset))
        cursor += 1
    } else {
      cursor -= 1
      while (cursor > limit && jump && continueCursor(cursor, charOffset))
        cursor -= 1
    }
  }

  protected def moveCursorVertically(forward: Boolean, jump: Boolean): Unit = {
    if (jump) {
      cursor = if (forward) text.length else 0
    }
    // For single-line fields, up/down without jump is a no-op
  }

  protected def continueCursor(index: Int, offset: Int): Boolean = {
    val i = index + offset
    if (i >= 0 && i < text.length) isWordCharacter(text.charAt(i))
    else false
  }

  // --- Focus traversal ---

  def setFocusTraversal(focusTraversal: Boolean): Unit =
    this.focusTraversal = focusTraversal

  // --- Message text ---

  def setMessageText(messageText: Nullable[String]): Unit =
    this.messageText = messageText

  def getMessageText: Nullable[String] = messageText

  // --- Programmatic change events ---

  def setProgrammaticChangeEvents(programmaticChangeEvents: Boolean): Unit =
    this.programmaticChangeEvents = programmaticChangeEvents

  def getProgrammaticChangeEvents: Boolean = programmaticChangeEvents

  // --- Listener/filter ---

  def setTextFieldListener(listener: Nullable[TextFieldListener]): Unit =
    this.listener = listener

  def setTextFieldFilter(filter: Nullable[TextFieldFilter]): Unit =
    this.filter = filter

  def getTextFieldFilter: Nullable[TextFieldFilter] = filter

  // --- Clipboard ---

  def setClipboard(contents: String): Unit =
    _clipboardContents = contents

  def getClipboardContents: String = _clipboardContents

  // --- Key handling ---

  /** Handles a key-down event. Returns true if handled. */
  def handleKeyDown(keycode: Int): Boolean = boundary {
    if (disabled) {
      break(false)
    }

    cursorOn = focused
    if (focused && blinkEnabled) {
      _blinkTimer = 0f
    }

    if (!_hasKeyboardFocus) {
      break(false)
    }

    // Key constants (from sge.Input.Key, opaque Int)
    val KeyV      = 50; val KeyC            = 31; val KeyX  = 52; val KeyA    = 29; val KeyZ = 54
    val KeyINSERT = 124; val KeyFORWARD_DEL = 112
    val KeyLEFT   = 21; val KeyRIGHT        = 22; val KeyUP = 19; val KeyDOWN = 20
    val KeyHOME   = 3; val KeyEND           = 123

    var repeat  = false
    val ctrl    = isCtrlPressed
    val shift   = isShiftPressed
    val jump    = ctrl && !passwordMode
    var handled = true

    if (ctrl) {
      keycode match {
        case `KeyV` =>
          paste(Nullable(_clipboardContents), fireChangeEvent = true)
          repeat = true
        case `KeyC` | `KeyINSERT` =>
          copy()
          break(true)
        case `KeyX` =>
          cut(fireChangeEvent = true)
          break(true)
        case `KeyA` =>
          selectAll()
          break(true)
        case `KeyZ` =>
          val oldText = text
          setText(Nullable(undoText))
          undoText = oldText
          updateDisplayText()
          break(true)
        case _ =>
          handled = false
      }
    }

    if (shift) {
      keycode match {
        case `KeyINSERT` =>
          paste(Nullable(_clipboardContents), fireChangeEvent = true)
        case `KeyFORWARD_DEL` =>
          cut(fireChangeEvent = true)
        case _ => ()
      }

      // Selection with shift
      val temp = cursor
      keycode match {
        case `KeyLEFT` =>
          moveCursor(false, jump)
          repeat = true
          handled = true
          updateSelectionAfterMove(temp)
        case `KeyRIGHT` =>
          moveCursor(true, jump)
          repeat = true
          handled = true
          updateSelectionAfterMove(temp)
        case `KeyUP` =>
          moveCursorVertically(false, jump)
          repeat = true
          handled = true
          updateSelectionAfterMove(temp)
        case `KeyDOWN` =>
          moveCursorVertically(true, jump)
          repeat = true
          handled = true
          updateSelectionAfterMove(temp)
        case `KeyHOME` =>
          cursor = 0
          handled = true
          updateSelectionAfterMove(temp)
        case `KeyEND` =>
          cursor = text.length
          handled = true
          updateSelectionAfterMove(temp)
        case _ => ()
      }
    } else {
      // Cursor movement without shift (kills selection)
      keycode match {
        case `KeyLEFT` =>
          moveCursor(false, jump)
          clearSelection()
          repeat = true
          handled = true
        case `KeyRIGHT` =>
          moveCursor(true, jump)
          clearSelection()
          repeat = true
          handled = true
        case `KeyUP` =>
          moveCursorVertically(false, jump)
          clearSelection()
          repeat = true
          handled = true
        case `KeyDOWN` =>
          moveCursorVertically(true, jump)
          clearSelection()
          repeat = true
          handled = true
        case `KeyHOME` =>
          cursor = 0
          clearSelection()
          handled = true
        case `KeyEND` =>
          cursor = text.length
          clearSelection()
          handled = true
        case _ => ()
      }
    }

    cursor = Math.max(0, Math.min(cursor, text.length))

    if (repeat) scheduleKeyRepeat(keycode)
    handled
  }

  private def updateSelectionAfterMove(temp: Int): Unit =
    if (!label.hasSelection) {
      label.selectionStart = temp
      label.selectionEnd = cursor
      if (temp < cursor) label.selectionEnd -= 1
      else label.selectionStart -= 1
    } else {
      val start = label.selectionStart
      val end   = label.selectionEnd
      if (cursor < start) {
        label.selectionStart = cursor
      } else if (cursor > end) {
        label.selectionEnd = cursor - 1
      } else if (temp < cursor) {
        label.selectionStart = cursor
      } else {
        label.selectionEnd = cursor - 1
      }
    }

  /** Handles a key-up event. Returns true if handled. */
  def handleKeyUp(keycode: Int): Boolean =
    if (disabled) {
      false
    } else {
      _keyRepeatActive = false
      true
    }

  protected def checkFocusTraversal(character: Char): Boolean =
    focusTraversal && character == TAB

  /** Handles a key-typed event. Returns true if handled. */
  def handleKeyTyped(character: Char): Boolean = boundary {
    if (disabled) {
      break(false)
    }

    // Disallow most ASCII control characters
    character match {
      case BACKSPACE | TAB | NEWLINE | CARRIAGE_RETURN => ()
      case c if c < 32                                 => break(false)
      case _                                           => ()
    }

    if (!_hasKeyboardFocus) {
      break(false)
    }

    if (checkFocusTraversal(character)) {
      // Focus traversal would happen in full scene2d integration
    } else {
      val enter     = character == CARRIAGE_RETURN || character == NEWLINE
      val deleteKey = character == DELETE
      val backspace = character == BACKSPACE
      var ch        = character
      if (ch == '[') ch = if (isShiftPressed) '{' else '\u0002'
      val add    = if (enter) writeEnters else !onlyFontChars || label.getFont.mapping.contains(ch.toInt)
      val remove = backspace || deleteKey

      if (add || remove) {
        val oldText   = text
        val oldCursor = cursor
        if (remove) {
          if (label.hasSelection) {
            cursor = delete(fireChangeEvent = false)
          } else {
            if (backspace && cursor > 0) {
              text = text.substring(0, cursor - 1) + text.substring(cursor)
              cursor -= 1
              renderOffset = 0
            }
            if (deleteKey && cursor < text.length) {
              text = text.substring(0, cursor) + text.substring(cursor + 1)
            }
          }
        }
        if (add && !remove) {
          var filtered = false
          Nullable.foreach(filter) { f =>
            if (!enter && !f.acceptChar(this, ch)) {
              filtered = true
            }
          }
          if (filtered) {
            break(true)
          }
          if (!withinMaxLength(text.length - (if (label.hasSelection) Math.abs(cursor - label.selectionStart) else 0))) {
            break(true)
          }
          if (label.hasSelection) {
            cursor = delete(fireChangeEvent = false)
          }
          val insertion = if (enter) "\n" else String.valueOf(ch)
          insert(cursor, insertion)
          cursor += 1
          text = label.layout.appendIntoDirect(new StringBuilder()).toString
        }
        if (changeText(oldText, text)) {
          val time = System.currentTimeMillis()
          if (time - 750 > lastChangeTime) undoText = oldText
          lastChangeTime = time
          updateDisplayText()
        } else if (text != oldText) {
          cursor = oldCursor
        }
      }
    }
    Nullable.foreach(listener)(_.keyTyped(this, character))
    true
  }

  // --- Key repeat ---

  private def scheduleKeyRepeat(keycode: Int): Unit = {
    _keyRepeatKeycode = keycode
    _keyRepeatTimer = TextraField.keyRepeatInitialTime
    _keyRepeatActive = true
  }

  // --- Click handling ---

  /** Handles a click (for cursor positioning). */
  def handleClick(tapCount: Int): Unit = {
    if (showingMessage) clearMessage()
    val count = tapCount & 3
    if (count == 0) clearSelection()
    else if (count == 2) {
      val pair = wordUnderCursor()
      setSelection((pair >>> 32).toInt, pair.toInt)
    } else if (count == 3) selectAll()
  }

  /** Handles a touch-down for cursor positioning. */
  def handleTouchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean =
    if (pointer == 0 && button != 0) {
      false
    } else if (disabled) {
      true
    } else {
      setCursorFromPosition(x, y)
      _hasKeyboardFocus = true
      if (showingMessage) clearMessage()
      true
    }

  /** Handles a touch-dragged for cursor positioning. */
  def handleTouchDragged(x: Float, y: Float, pointer: Int): Unit =
    setCursorFromPosition(x, y)

  protected def setCursorFromPosition(x: Float, y: Float): Unit = {
    if (label.overIndex < 0) {
      if (x < label.workingLayout.getWidth * 0.5f) cursor = 0
      else cursor = label.length()
    } else {
      cursor = label.overIndex
    }
    cursorOn = focused
    if (blinkEnabled) {
      _blinkTimer = 0f
    }
  }

  // --- Keyboard state helpers ---

  /** Whether ctrl (or cmd on Mac) is pressed. Override for platform integration. */
  protected def isCtrlPressed: Boolean = false

  /** Whether shift is pressed. Override for platform integration. */
  protected def isShiftPressed: Boolean = false

  // --- Listener/Filter interfaces ---

  /** Interface for listening to typed characters. */
  trait TextFieldListener {
    def keyTyped(textField: TextraField, c: Char): Unit
  }

  /** Interface for filtering characters entered into the text field. */
  trait TextFieldFilter {
    def acceptChar(textField: TextraField, c: Char): Boolean
  }

  /** Filter that only accepts digits 0-9. */
  class DigitsOnlyFilter extends TextFieldFilter {
    def acceptChar(textField: TextraField, c: Char): Boolean =
      c >= '0' && c <= '9'
  }

  /** An interface for an on-screen keyboard. */
  trait OnscreenKeyboard {
    def show(visible: Boolean): Unit
  }
}

object TextraField {
  var keyRepeatInitialTime: Float = 0.4f
  var keyRepeatTime:        Float = 0.1f
}
