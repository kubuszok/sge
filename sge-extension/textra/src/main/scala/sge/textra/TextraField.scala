/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraField.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Disableable → trait field,
 *     FloatArray → ArrayBuffer[Float], Clipboard → deferred,
 *     InputListener/ClickListener → deferred, Timer.Task → deferred,
 *     Replacer (RegExodus) → java.util.regex (deferred for emoji),
 *     UIUtils → deferred, ChangeEvent → deferred
 *   Convention: Full text-editing behavior preserved in API;
 *     actual rendering and input handling deferred until scene2d wiring.
 *   TODOs: Full input handling (key events, clipboard, cursor blinking,
 *     password mode) deferred until scene2d wiring.
 */
package sge
package textra

import sge.utils.Nullable

/** A single-line text input field using a Font. */
class TextraField {

  protected val BACKSPACE:       Char = '\b'
  protected val CARRIAGE_RETURN: Char = '\r'
  protected val NEWLINE:         Char = '\n'
  protected val TAB:             Char = '\t'
  protected val DELETE:          Char = 127.toChar
  val BULLET:                    Char = 8226.toChar // u2022, or bullet

  protected var text:        String      = ""
  protected var cursor:      Int         = 0
  protected var writeEnters: Boolean     = false
  protected var label:       TypingLabel = new TypingLabel()

  protected var style:          Nullable[Styles.TextFieldStyle] = Nullable.empty
  protected var messageText:    Nullable[String]                = Nullable.empty
  protected var showingMessage: Boolean                         = false
  protected var focusTraversal: Boolean                         = true
  protected var onlyFontChars:  Boolean                         = true
  protected var disabled:       Boolean                         = false
  protected var textHAlign:     Int                             = 8 // Align.left

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

  def this(text: Nullable[String], style: Styles.TextFieldStyle) = {
    this()
    val s = new Styles.TextFieldStyle(style)
    Nullable.foreach(s.font) { f =>
      f.enableSquareBrackets = false
      f.omitCurlyBraces = false
    }
    this.style = Nullable(s)
    Nullable.foreach(s.font) { f =>
      label = new TypingLabel("", new Styles.LabelStyle(f, style.fontColor))
    }
    setText(text)
  }

  def this(text: Nullable[String], style: Styles.TextFieldStyle, replacementFont: Font) = {
    this()
    this.style = Nullable(style)
    val rf = new Font(replacementFont)
    rf.enableSquareBrackets = false
    rf.omitCurlyBraces = false
    label = new TypingLabel("", new Styles.LabelStyle(rf, style.fontColor))
    setText(text)
  }

  def setStyle(style: Styles.TextFieldStyle): Unit =
    this.style = Nullable(style)

  def getStyle: Nullable[Styles.TextFieldStyle] = style

  def setText(text: Nullable[String]): Unit =
    this.text = Nullable.fold(text)("")(identity)

  def getText: String = text

  def setMaxLength(maxLength: Int): Unit =
    this.maxLength = maxLength

  def getMaxLength: Int = maxLength

  def setOnlyFontChars(onlyFontChars: Boolean): Unit =
    this.onlyFontChars = onlyFontChars

  def getCursor: Int = cursor

  def setCursor(cursor: Int): Unit =
    this.cursor = cursor

  def isPasswordMode: Boolean = passwordMode

  def setPasswordMode(passwordMode: Boolean): Unit =
    this.passwordMode = passwordMode

  def setPasswordCharacter(passwordCharacter: Char): Unit =
    this.passwordCharacter = passwordCharacter

  def isDisabled: Boolean = disabled

  def setDisabled(disabled: Boolean): Unit =
    this.disabled = disabled

  def setFocusTraversal(focusTraversal: Boolean): Unit =
    this.focusTraversal = focusTraversal

  def setMessageText(messageText: Nullable[String]): Unit =
    this.messageText = messageText

  def getMessageText: Nullable[String] = messageText

  def setAlignment(alignment: Int): Unit =
    this.textHAlign = alignment

  protected def updateDisplayText(): Unit = {
    // In full implementation, updates the label with current text
    // considering password mode, cursor position, etc.
  }

  /** A listener for individual characters typed. */
  trait TextFieldListener {
    def keyTyped(textField: TextraField, c: Char): Unit
  }

  /** An interface for filtering characters entered into the text field. */
  trait TextFieldFilter {
    def acceptChar(textField: TextraField, c: Char): Boolean
  }

  /** An interface for an on-screen keyboard. */
  trait OnscreenKeyboard {
    def show(visible: Boolean): Unit
  }
}
