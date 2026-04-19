/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: mzechner, Nathan Sweet, Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 248
 * Covenant-baseline-methods: DigitsOnlyFilter,TextFieldFilter,TextFieldListener,VisTextField,VisTextFieldStyle,_cursorPercentHeight,_enterKeyFocusTraversal,_focusBorderEnabled,_ignoreEqualsTextChange,_inputValid,_readOnly,_visStyle,acceptChar,backgroundDrawable,backgroundOver,beforeChangeEventFired,changeText,clearText,clickListener,cursorHeight,cursorPercentHeight,cursorYPadding,disabled_,draw,drawBorder,drawCursor,enter,errorBorder,exit,focusBorder,focusBorderEnabled,focusBorderEnabled_,focusField,focusGained,focusLost,initialize,isEmpty,isEnterKeyFocusTraversal,isIgnoreEqualsTextChange,isInputValid,isReadOnly,keyTyped,setCursorAtTextEnd,setCursorPercentHeight,setEnterKeyFocusTraversal,setIgnoreEqualsTextChange,setInputValid,setReadOnly,this,toString,touchDown
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisTextField.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.graphics.{ Color, Cursor }
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.ui.TextField
import sge.scenes.scene2d.utils.{ ClickListener, Drawable }
import sge.utils.Nullable
import sge.visui.{ FocusManager, Focusable, VisUI }
import sge.visui.util.{ BorderOwner, CursorManager }

/** Extends functionality of standard [[TextField]]. Style supports focus border and error border. Adds input validation support.
  *
  * Note: The original VisUI VisTextField was a complete reimplementation of TextField. This port extends SGE's existing TextField and adds the VisUI-specific features (focus border, error border,
  * input validity state, backgroundOver, cursor percent height, read-only mode, I-beam cursor on hover, FocusManager integration).
  * @author
  *   mzechner, Nathan Sweet, Kotcrab
  * @see
  *   [[TextField]]
  */
class VisTextField(text: Nullable[String], visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends TextField(text, visStyle) with Focusable with BorderOwner {
  import VisTextField._

  private val _visStyle:               VisTextFieldStyle = visStyle
  private var clickListener:           ClickListener     = scala.compiletime.uninitialized
  private var drawBorder:              Boolean           = false
  private var _focusBorderEnabled:     Boolean           = true
  private var _inputValid:             Boolean           = true
  private var _readOnly:               Boolean           = false
  private var _ignoreEqualsTextChange: Boolean           = true
  private var _cursorPercentHeight:    Float             = 0.8f
  private var _enterKeyFocusTraversal: Boolean           = false

  // Replace the basic focus-switch listener with the full initialize() pattern
  override protected def initialize(): Unit = {
    super.initialize()
    clickListener = new ClickListener() {
      override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit = {
        super.enter(event, x, y, pointer, fromActor)
        if (pointer == -1 && !disabled) {
          Sge().graphics.setSystemCursor(Cursor.SystemCursor.Ibeam)
        }
      }

      override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
        super.exit(event, x, y, pointer, toActor)
        if (pointer == -1) {
          CursorManager.restoreDefaultCursor()
        }
      }
    }
    addListener(clickListener)

    // Add FocusManager integration listener
    addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          if (!disabled) FocusManager.switchFocus(stage, VisTextField.this)
          false
        }
      }
    )
  }

  def this()(using Sge) = this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String, styleName: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))

  /** Overrides the background drawable selection to support backgroundOver from VisUI style. */
  override protected def backgroundDrawable: Nullable[Drawable] =
    if (disabled && _style.disabledBackground.isDefined) _style.disabledBackground
    else if (!disabled && _visStyle.backgroundOver.isDefined && (clickListener != null && clickListener.over || hasKeyboardFocus))
      _visStyle.backgroundOver
    else if (_style.focusedBackground.isDefined && hasKeyboardFocus) _style.focusedBackground
    else _style.background

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    // Draw VisUI borders on top
    if (!disabled && !_inputValid && _visStyle.errorBorder.isDefined) {
      _visStyle.errorBorder.get.draw(batch, x, y, width, height)
    } else if (_focusBorderEnabled && drawBorder && _visStyle.focusBorder.isDefined) {
      _visStyle.focusBorder.get.draw(batch, x, y, width, height)
    }
  }

  /** Override cursor drawing to support cursorPercentHeight. */
  override protected def drawCursor(cursorPatch: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    val cursorHeight   = textHeight * _cursorPercentHeight
    val cursorYPadding = (textHeight - cursorHeight) / 2
    cursorPatch.draw(
      batch,
      x + textOffset + glyphPositions(cursor) - glyphPositions(visibleTextStart) + fontOffset + font.data.cursorX,
      y - textHeight - font.descent + cursorYPadding,
      cursorPatch.minWidth,
      cursorHeight
    )
  }

  def isInputValid:                  Boolean = _inputValid
  def setInputValid(valid: Boolean): Unit    = _inputValid = valid

  override def focusBorderEnabled:                     Boolean = _focusBorderEnabled
  override def focusBorderEnabled_=(enabled: Boolean): Unit    = _focusBorderEnabled = enabled

  override def focusLost():   Unit = drawBorder = false
  override def focusGained(): Unit = drawBorder = true

  def isReadOnly:                     Boolean = _readOnly
  def setReadOnly(readOnly: Boolean): Unit    = _readOnly = readOnly

  def isIgnoreEqualsTextChange:                   Boolean = _ignoreEqualsTextChange
  def setIgnoreEqualsTextChange(ignore: Boolean): Unit    = _ignoreEqualsTextChange = ignore

  def cursorPercentHeight: Float = _cursorPercentHeight

  /** @param value
    *   cursor size, value from 0..1 range
    */
  def setCursorPercentHeight(value: Float): Unit = {
    if (value < 0 || value > 1) throw IllegalArgumentException("cursorPercentHeight must be >= 0 and <= 1")
    _cursorPercentHeight = value
  }

  /** If true, enter will move to the next text field that has focus traversal enabled. False by default. Note that to enable or disable focus traversal completely you must use
    * {@link #setFocusTraversal(boolean)}
    */
  def isEnterKeyFocusTraversal: Boolean = _enterKeyFocusTraversal

  def setEnterKeyFocusTraversal(enterKeyFocusTraversal: Boolean): Unit =
    _enterKeyFocusTraversal = enterKeyFocusTraversal

  override def disabled_=(value: Boolean): Unit = {
    super.disabled_=(value)
    if (value) {
      FocusManager.resetFocus(stage, this)
    }
  }

  /** Focuses this field, field must be added to stage before this method can be called */
  def focusField(): Unit =
    if (!disabled) {
      FocusManager.switchFocus(stage, VisTextField.this)
      setCursorPosition(0)
      _selectionStart = 0
      // make sure textOffset was updated, prevent issue when there was long text selected and it was changed to short text
      // and field was focused. Without it textOffset would stay at max value and only one last letter will be visible in field
      calculateOffsets()
      stage.foreach { s =>
        s.setKeyboardFocus(Nullable(VisTextField.this))
      }
      keyboard.show(VisTextField.this)
      hasSelection = true
    }

  /** Returns true if the text is empty. */
  def isEmpty: Boolean = _text.length() == 0

  /** Clears VisTextField text. If programmatic change events are disabled then this will not fire change event. */
  def clearText(): Unit = setText(Nullable(""))

  /** Sets the cursor position to the end of the text. */
  def setCursorAtTextEnd(): Unit = {
    setCursorPosition(0)
    calculateOffsets()
    setCursorPosition(_text.length())
  }

  override def toString: String = _text

  /** Hook called right before ChangeEvent is fired. Subclasses (e.g. VisValidatableTextField) override this to trigger validation. */
  protected def beforeChangeEventFired(): Unit = ()

  override private[sge] def changeText(oldText: String, newText: String): Boolean =
    if (_ignoreEqualsTextChange && newText == oldText) false
    else {
      beforeChangeEventFired()
      super.changeText(oldText, newText)
    }
}

object VisTextField {

  /** Interface for listening to typed characters in a VisTextField.
    * @author
    *   mzechner
    */
  trait TextFieldListener {
    def keyTyped(textField: VisTextField, c: Char): Unit
  }

  /** Interface for filtering characters entered into a VisTextField.
    * @author
    *   mzechner
    */
  trait TextFieldFilter {
    def acceptChar(textField: VisTextField, c: Char): Boolean
  }

  object TextFieldFilter {
    class DigitsOnlyFilter extends TextFieldFilter {
      def acceptChar(textField: VisTextField, c: Char): Boolean =
        Character.isDigit(c)
    }
  }

  class VisTextFieldStyle() extends TextField.TextFieldStyle() {
    var focusBorder:    Nullable[Drawable] = Nullable.empty
    var errorBorder:    Nullable[Drawable] = Nullable.empty
    var backgroundOver: Nullable[Drawable] = Nullable.empty

    def this(font: BitmapFont, fontColor: Color, cursor: Nullable[Drawable], selection: Nullable[Drawable], background: Nullable[Drawable]) = {
      this()
      this.font = font
      this.fontColor = fontColor
      this.cursor = cursor
      this.selection = selection
      this.background = background
    }

    def this(style: VisTextFieldStyle) = {
      this()
      this.font = style.font
      this.fontColor = style.fontColor
      this.focusedFontColor = style.focusedFontColor
      this.disabledFontColor = style.disabledFontColor
      this.background = style.background
      this.focusedBackground = style.focusedBackground
      this.disabledBackground = style.disabledBackground
      this.cursor = style.cursor
      this.selection = style.selection
      this.messageFont = style.messageFont
      this.messageFontColor = style.messageFontColor
      this.focusBorder = style.focusBorder
      this.errorBorder = style.errorBorder
      this.backgroundOver = style.backgroundOver
    }
  }
}
