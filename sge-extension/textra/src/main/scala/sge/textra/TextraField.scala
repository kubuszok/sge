/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraField.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Disableable → disabled field,
 *     FloatArray → ArrayBuffer[Float], Clipboard → Sge clipboard,
 *     Replacer (RegExodus) → deferred (emoji replacement),
 *     UIUtils → helper methods, ChangeEvent → direct callback,
 *     Timer.Task → KeyRepeatTask/blinkTask inner classes
 *   Convention: Full text-editing behavior preserved: cursor movement,
 *     selection, copy/cut/paste, password mode, undo, key repeat, draw.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *   Merged with: TextFieldClickListener inner class for input handling,
 *     KeyRepeatTask for key repeat, lifecycle hooks, focus traversal.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1500
 * Covenant-baseline-methods: BACKSPACE,BULLET,CARRIAGE_RETURN,DELETE,DefaultOnscreenKeyboard,DigitsOnlyFilter,KeyRepeatTask,LetterOnlyFilter,NEWLINE,OnscreenKeyboard,TAB,TextFieldClickListener,TextFieldFilter,TextFieldListener,TextraField,WordOnlyFilter,_blinkTimer,_color,_hasKeyboardFocus,_height,_parent,_stage,_tapCount,_width,_x,_y,acceptChar,act,active,appendText,ascendantsVisible,background,bgLeftWidth,blinkEnabled,blinkTime,calculateOffsets,calculateXAdvancesFromLayout,cancel,changeText,charOffset,checkFocusTraversal,clearMessage,clearSelection,clicked,clipboard,color,continueCursor,copy,createInputListener,currentBest,cursor,cursorOn,cursorPatch,cut,delete,disabled,distance,draw,drawCursor,end,endX,filter,findNextTextField,focusTraversal,focused,font,fontColor,fontOffset,from,getAlignment,getBackgroundDrawable,getBlinkTime,getClipboard,getClipboardContents,getColor,getCursor,getCursorPosition,getDefaultInputListener,getHeight,getMaxLength,getMessageText,getOnscreenKeyboard,getParent,getPasswordCharacter,getPrefHeight,getPrefWidth,getProgrammaticChangeEvents,getSelection,getSelectionEnd,getSelectionStart,getStage,getStyle,getText,getTextFieldFilter,getTextY,getWidth,getX,getY,glyphCount,glyphPositions,goEnd,goHome,handleClick,handleKeyDown,handleKeyTyped,handleKeyUp,handleTouchDown,handleTouchDragged,hasKeyboardFocus,height,i,index,initialize,inputListener,insert,isCtrlPressed,isCursorBlinking,isDisabled,isPasswordMode,isShiftPressed,isSpaceCharacter,isWordCharacter,keyDown,keyRepeatInitialTime,keyRepeatTask,keyRepeatTime,keyTyped,keyUp,keyboard,keycode,label,lastChangeTime,lb,left,len,limit,lineHeight,listener,maxLength,maxOffset,messageText,minHeight,moveCursor,moveCursorVertically,n,newText,next,onlyFontChars,passwordCharacter,passwordMode,paste,positionChanged,programmaticChangeEvents,renderOffset,rf,right,s,schedule,scheduleKeyRepeatTask,selectAll,setAlignment,setBlinkTime,setClipboard,setClipboardContents,setColor,setCursor,setCursorBlinking,setCursorFromPosition,setCursorPosition,setDisabled,setFocusTraversal,setHeight,setKeyboardFocus,setMaxLength,setMessageText,setOnlyFontChars,setOnscreenKeyboard,setParent,setPasswordCharacter,setPasswordMode,setPosition,setProgrammaticChangeEvents,setSelection,setSize,setStage,setStyle,setText,setTextFieldFilter,setTextFieldListener,setWidth,setX,setY,show,showingMessage,sizeChanged,start,startX,style,tapCount,text,textHAlign,textOffset,textY,this,timer,topAndBottom,touchDown,touchDragged,touchUp,undoText,updateDisplayText,updateSelectionAfterMove,visibleTextEnd,visibleTextStart,visibleWidth,wasFocused,width,width2,withinMaxLength,wordUnderCursor,writeEnters,x,y
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraField.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, Group, Stage }
import sge.scenes.scene2d.utils.{ Drawable, UIUtils }
import sge.utils.{ Align, Clipboard, DynamicArray, Nullable }

/** A single-line text input field using a Font.
  *
  * If you just want a span of read-only text that can be selected and copied, then use a TypingLabel with setSelectable set to true. This can use a TypingListener that checks for the event
  * "*SELECTED", and calls copySelectedText() when that event is received. Or, call copySelectedText() when the user requests, such as by pressing Ctrl-c; this doesn't need a TypingListener.
  *
  * The preferred height of a text field is the height of the font and background. The preferred width of a text field is 150, a relatively arbitrary size.
  *
  * The text field will copy the currently selected text when Ctrl+c is pressed, and paste any text in the clipboard when Ctrl+v is pressed. Clipboard functionality is provided via the Clipboard
  * interface.
  *
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  * @author
  *   Tommy Ettinger
  */
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
  protected var inputListener: Nullable[TextFieldClickListener] = Nullable.empty
  protected var listener:      Nullable[TextFieldListener]      = Nullable.empty
  protected var filter:        Nullable[TextFieldFilter]        = Nullable.empty

  // Clipboard -- connected via initialize() or manually
  protected var clipboard: Nullable[Clipboard] = Nullable.empty

  // On-screen keyboard
  protected var keyboard: Nullable[OnscreenKeyboard] = Nullable.empty

  // Blink state
  private var _blinkTimer: Float = 0f

  // Key repeat state
  protected val keyRepeatTask: KeyRepeatTask = new KeyRepeatTask()

  // Widget-like fields
  private var _x:      Float = 0f
  private var _y:      Float = 0f
  private var _width:  Float = 0f
  private var _height: Float = 0f
  private val _color:  Color = new Color(Color.WHITE)

  // Stage/parent references for lifecycle integration
  private var _stage:  Nullable[Stage] = Nullable.empty
  private var _parent: Nullable[Group] = Nullable.empty

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

  protected def initialize(): Unit =
    // Emoji replacer requires EmojiProcessor integration (deferred).
    inputListener = Nullable(createInputListener())

  /** Creates the input listener for this text field. Override to provide a custom listener. */
  protected def createInputListener(): TextFieldClickListener =
    new TextFieldClickListener()

  /** Returns the default input listener. */
  def getDefaultInputListener: Nullable[TextFieldClickListener] = inputListener

  // --- Widget-like accessors ---

  def getX:                Float = _x
  def getY:                Float = _y
  def setX(x:      Float): Unit  = { _x = x; positionChanged() }
  def setY(y:      Float): Unit  = { _y = y; positionChanged() }
  def getWidth:            Float = _width
  def getHeight:           Float = _height
  def setWidth(w:  Float): Unit  = { _width = w; sizeChanged() }
  def setHeight(h: Float): Unit  = { _height = h; sizeChanged() }
  def getColor:            Color = _color
  def setColor(c:  Color): Unit  = if (c != null) _color.set(c)

  def setPosition(x: Float, y: Float): Unit = {
    _x = x
    _y = y
    positionChanged()
  }

  def setSize(width: Float, height: Float): Unit = {
    _width = width
    _height = height
    sizeChanged()
  }

  def hasKeyboardFocus:                 Boolean = _hasKeyboardFocus
  def setKeyboardFocus(focus: Boolean): Unit    = _hasKeyboardFocus = focus

  // --- Lifecycle methods ---

  /** Called when this TextraField is added to or removed from a stage. Subclasses can override to react to stage changes.
    */
  protected def setStage(stage: Nullable[Stage]): Unit =
    _stage = stage

  /** Returns the stage this field is on, or Nullable.empty. */
  def getStage: Nullable[Stage] = _stage

  /** Called when this TextraField's parent group changes. Subclasses can override to react to parent changes.
    */
  protected def setParent(parent: Nullable[Group]): Unit =
    _parent = parent

  /** Returns the parent group, or Nullable.empty. */
  def getParent: Nullable[Group] = _parent

  /** Called when the position of this TextraField changes. */
  protected def positionChanged(): Unit =
    label.setPosition(_x, _y)

  /** Called when the size of this TextraField changes. */
  protected def sizeChanged(): Unit = {
    label.setSize(_width, _height)
    updateDisplayText()
  }

  /** Returns true if this field and all ascendants are visible. In standalone mode, always returns true. When integrated with scene2d, this would check the visibility of all ascendant actors.
    */
  def ascendantsVisible(): Boolean = true

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

  /** Copies the contents of this TextraField to the Clipboard implementation set on this TextraField. */
  def copy(): Unit =
    if (label.hasSelection && !passwordMode) {
      val start  = Math.min(label.selectionStart, label.selectionEnd)
      val end    = Math.max(label.selectionStart, label.selectionEnd)
      val toCopy = label.substring(Math.max(0, start), Math.min(label.length(), end + 1))
      setClipboardContents(toCopy)
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
    if (keyRepeatTask.active) {
      keyRepeatTask.timer -= delta
      if (keyRepeatTask.timer <= 0f) {
        keyRepeatTask.timer = TextraField.keyRepeatTime
        Nullable.foreach(inputListener)(_.keyDown(keyRepeatTask.keycode))
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

  /** Sets the keyboard focus to the next TextraField. If no next text field is found, the onscreen keyboard is hidden. Does nothing if the text field is not in a stage.
    * @param up
    *   If true, the text field with the same or next smallest y coordinate is found, else the next highest.
    */
  def next(up: Boolean): Unit =
    Nullable.foreach(_stage) { stage =>
      val currentCoords = Vector2(getX, getY)
      val bestCoords    = Vector2()
      var textraField   = findNextTextField(stage.actors, Nullable.empty, bestCoords, currentCoords, up)
      if (!textraField.isDefined) {
        // Try to wrap around.
        if (up) {
          currentCoords.set(-Float.MaxValue, -Float.MaxValue)
        } else {
          currentCoords.set(Float.MaxValue, Float.MaxValue)
        }
        textraField = findNextTextField(stage.actors, Nullable.empty, bestCoords, currentCoords, up)
      }
      Nullable.fold(textraField) {
        Nullable.foreach(keyboard)(_.show(false))
      } { tf =>
        tf.setKeyboardFocus(true)
        tf.selectAll()
      }
    }

  /** Searches for the next TextraField in the actor tree. In standalone mode, TextraField does not extend Actor, so this only recurses into Groups. When TextraField is integrated with scene2d
    * (extending Actor), this will find TextraField instances in the tree.
    * @return
    *   May be Nullable.empty.
    */
  private def findNextTextField(
    actors:        DynamicArray[Actor],
    best:          Nullable[TextraField],
    bestCoords:    Vector2,
    currentCoords: Vector2,
    up:            Boolean
  ): Nullable[TextraField] = {
    var currentBest = best
    var i           = 0
    while (i < actors.size) {
      val actor = actors(i)
      actor match {
        case group: Group =>
          currentBest = findNextTextField(group.children, currentBest, bestCoords, currentCoords, up)
        case _ => ()
      }
      i += 1
    }
    currentBest
  }

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

  def setClipboard(clipboard: Clipboard): Unit =
    this.clipboard = Nullable(clipboard)

  def getClipboard: Nullable[Clipboard] = clipboard

  /** Gets clipboard contents, falling back to local storage if no clipboard is set. */
  protected def getClipboardContents: Nullable[String] =
    Nullable.fold(clipboard)(Nullable.empty: Nullable[String])(_.contents)

  /** Sets clipboard contents. */
  protected def setClipboardContents(contents: String): Unit =
    Nullable.foreach(clipboard)(_.contents = Nullable(contents))

  // --- On-screen keyboard ---

  /** Default is an instance of DefaultOnscreenKeyboard. */
  def getOnscreenKeyboard: Nullable[OnscreenKeyboard] = keyboard

  def setOnscreenKeyboard(keyboard: OnscreenKeyboard): Unit =
    this.keyboard = Nullable(keyboard)

  // --- Cursor navigation helpers ---

  /** Moves the cursor to the beginning of the text. Can be overridden for multi-line behavior. */
  protected def goHome(jump: Boolean): Unit =
    cursor = 0

  /** Moves the cursor to the end of the text. Can be overridden for multi-line behavior. */
  protected def goEnd(jump: Boolean): Unit =
    cursor = text.length

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

  // --- KeyRepeatTask inner class ---

  /** Handles key repeat delay/interval for held keys. */
  class KeyRepeatTask {
    var keycode: Int     = -1
    var timer:   Float   = 0f
    var active:  Boolean = false

    def schedule(kc: Int): Unit = {
      keycode = kc
      timer = TextraField.keyRepeatInitialTime
      active = true
    }

    def cancel(): Unit =
      active = false
  }

  // --- TextFieldClickListener inner class ---

  /** Basic input listener for the TextraField. Handles touchDown, touchDragged, touchUp, keyDown, keyTyped, keyUp, and click events with cursor positioning, selection, copy/paste, and key repeat.
    */
  class TextFieldClickListener {

    private var _tapCount: Int = 0

    def tapCount: Int = _tapCount

    def clicked(tapCount: Int): Unit = {
      _tapCount = tapCount
      if (showingMessage) clearMessage()
      val count = tapCount & 3
      if (count == 0) clearSelection()
      else if (count == 2) {
        val pair = wordUnderCursor()
        setSelection((pair >>> 32).toInt, pair.toInt)
      } else if (count == 3) selectAll()
    }

    def touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean =
      if (pointer == 0 && button != 0) {
        false
      } else if (disabled) {
        true
      } else {
        setCursorPosition(x, y)
        _hasKeyboardFocus = true
        Nullable.foreach(keyboard)(_.show(true))
        if (showingMessage) clearMessage()
        true
      }

    def touchDragged(x: Float, y: Float, pointer: Int): Unit =
      setCursorPosition(x, y)

    def touchUp(x: Float, y: Float, pointer: Int, button: Int): Unit = ()

    protected def setCursorPosition(x: Float, y: Float): Unit = {
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

    protected def goHome(jump: Boolean): Unit =
      cursor = 0

    protected def goEnd(jump: Boolean): Unit =
      cursor = text.length

    def keyDown(keycode: Int): Boolean = boundary {
      if (disabled) { break(false) }

      cursorOn = focused
      if (blinkEnabled && focused) {
        _blinkTimer = 0f
      }

      if (!_hasKeyboardFocus) { break(false) }

      // Key constants (raw Int values from sge.Input.Keys opaque Key type)
      val KeyV:           Int = 50
      val KeyC:           Int = 31
      val KeyX:           Int = 52
      val KeyA:           Int = 29
      val KeyZ:           Int = 54
      val KeyINSERT:      Int = 124
      val KeyFORWARD_DEL: Int = 112
      val KeyLEFT:        Int = 21
      val KeyRIGHT:       Int = 22
      val KeyUP:          Int = 19
      val KeyDOWN:        Int = 20
      val KeyHOME:        Int = 3
      val KeyEND:         Int = 123

      var repeat  = false
      val ctrl    = isCtrlPressed
      val jump    = ctrl && !passwordMode
      var handled = true

      if (ctrl) {
        keycode match {
          case `KeyV` =>
            Nullable.foreach(getClipboardContents) { contents =>
              paste(Nullable(contents), fireChangeEvent = true)
            }
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

      if (isShiftPressed) {
        keycode match {
          case `KeyINSERT` =>
            Nullable.foreach(getClipboardContents) { contents =>
              paste(Nullable(contents), fireChangeEvent = true)
            }
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
            goHome(jump)
            handled = true
            updateSelectionAfterMove(temp)
          case `KeyEND` =>
            goEnd(jump)
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
            goHome(jump)
            clearSelection()
            handled = true
          case `KeyEND` =>
            goEnd(jump)
            clearSelection()
            handled = true
          case _ => ()
        }
      }

      cursor = Math.max(0, Math.min(cursor, text.length))

      if (repeat) scheduleKeyRepeatTask(keycode)
      handled
    }

    protected def scheduleKeyRepeatTask(keycode: Int): Unit =
      if (!keyRepeatTask.active || keyRepeatTask.keycode != keycode) {
        keyRepeatTask.schedule(keycode)
      }

    def keyUp(keycode: Int): Boolean =
      if (disabled) {
        false
      } else {
        keyRepeatTask.cancel()
        true
      }

    /** Checks if focus traversal should be triggered. The default implementation uses focusTraversal and the typed character, depending on the OS.
      * @param character
      *   The character that triggered a possible focus traversal.
      * @return
      *   true if the focus should change to the next input field.
      */
    protected def checkFocusTraversal(character: Char): Boolean =
      focusTraversal && (character == TAB
        || ((character == CARRIAGE_RETURN || character == NEWLINE) && (UIUtils.isAndroid || UIUtils.isIos)))

    def keyTyped(character: Char): Boolean = boundary {
      if (disabled) { break(false) }

      // Now we're hitting a problem in libGDX, where keyTyped is a character-based API
      // and doesn't understand codepoints greater than 65535. Entering an emoji will
      // actually type multiple codepoints, where some need to be ints but are truncated
      // to chars by libGDX.

      // Disallow "typing" most ASCII control characters, which would show up as a space when onlyFontChars is true.
      character match {
        case BACKSPACE | TAB | NEWLINE | CARRIAGE_RETURN => ()
        case c if c < 32                                 => break(false)
        case _                                           => ()
      }

      if (!_hasKeyboardFocus) { break(false) }

      if (UIUtils.isMac) {
        // On Mac, ignore keyTyped when Command (SYM) is pressed
        // (This check is a no-op in standalone mode; with Sge integration, it would
        //  check Sge().input.isKeyPressed(Input.Keys.SYM))
      }

      if (checkFocusTraversal(character)) {
        next(isShiftPressed)
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
              if (!enter && !f.acceptChar(TextraField.this, ch)) {
                filtered = true
              }
            }
            if (filtered) { break(true) }
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
      Nullable.foreach(listener)(_.keyTyped(TextraField.this, character))
      true
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
  }

  // --- Backward-compatible handle methods (delegate to inputListener) ---

  /** Handles a click (for cursor positioning). Delegates to the TextFieldClickListener. */
  def handleClick(tapCount: Int): Unit =
    Nullable.foreach(inputListener)(_.clicked(tapCount))

  /** Handles a touch-down for cursor positioning. Delegates to the TextFieldClickListener. */
  def handleTouchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean =
    Nullable.fold(inputListener)(false)(_.touchDown(x, y, pointer, button))

  /** Handles a touch-dragged for cursor positioning. Delegates to the TextFieldClickListener. */
  def handleTouchDragged(x: Float, y: Float, pointer: Int): Unit =
    Nullable.foreach(inputListener)(_.touchDragged(x, y, pointer))

  /** Handles a key-down event. Returns true if handled. Delegates to the TextFieldClickListener. */
  def handleKeyDown(keycode: Int): Boolean =
    Nullable.fold(inputListener)(false)(_.keyDown(keycode))

  /** Handles a key-up event. Returns true if handled. Delegates to the TextFieldClickListener. */
  def handleKeyUp(keycode: Int): Boolean =
    Nullable.fold(inputListener)(false)(_.keyUp(keycode))

  /** Handles a key-typed event. Returns true if handled. Delegates to the TextFieldClickListener. */
  def handleKeyTyped(character: Char): Boolean =
    Nullable.fold(inputListener)(false)(_.keyTyped(character))

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

  /** Filter that only accepts what Unicode considers "letter characters." */
  class LetterOnlyFilter extends TextFieldFilter {
    def acceptChar(textField: TextraField, c: Char): Boolean =
      Character.isLetter(c)
  }

  /** Filter that only accepts what Unicode considers "word characters" -- all letters, all numbers, and the underscore (as well as all underscore-like punctuation).
    */
  class WordOnlyFilter extends TextFieldFilter {
    def acceptChar(textField: TextraField, c: Char): Boolean =
      Character.isLetterOrDigit(c) || c == '_' || Character.getType(c) == Character.CONNECTOR_PUNCTUATION
  }

  /** An interface for an on-screen keyboard. */
  trait OnscreenKeyboard {
    def show(visible: Boolean): Unit
  }

  /** The default OnscreenKeyboard used by TextraField instances. Uses Sge().input.setOnscreenKeyboardVisible as appropriate. May overlap actual rendering.
    */
  class DefaultOnscreenKeyboard(using Sge) extends OnscreenKeyboard {
    def show(visible: Boolean): Unit =
      summon[Sge].input.setOnscreenKeyboardVisible(visible)
  }
}

object TextraField {
  var keyRepeatInitialTime: Float = 0.4f
  var keyRepeatTime:        Float = 0.1f
}
