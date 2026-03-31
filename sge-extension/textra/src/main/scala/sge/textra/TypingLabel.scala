/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingLabel.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: ObjectMap → HashMap, ArrayList → ArrayBuffer,
 *     Vector2 → deferred, Batch/Sprite/Drawable → deferred,
 *     NumberUtils → java.lang.Float
 *   Convention: TypingLabel extends TextraLabel with typing animation and
 *     effect token support. Full rendering deferred until scene2d wiring.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.utils.Nullable

/** An extension of TextraLabel that progressively shows the text as if it was being typed in real time, and allows the use of tokens in the format: {TOKEN=PARAMETER;ANOTHER_PARAMETER;MORE}. These
  * tokens can add various effects to spans of text.
  */
class TypingLabel extends TextraLabel {

  // --- Collections ---
  private val variables: scala.collection.mutable.HashMap[String, String] = scala.collection.mutable.HashMap.empty
  val tokenEntries:      ArrayBuffer[TokenEntry]                          = ArrayBuffer.empty

  // --- Config ---
  private val clearColor:     Color                    = new Color(TypingConfig.DEFAULT_CLEAR_COLOR)
  private var typingListener: Nullable[TypingListener] = Nullable.empty

  // --- Internal state ---
  private val originalText:     StringBuilder = new StringBuilder()
  private val intermediateText: StringBuilder = new StringBuilder()
  val workingLayout:            Layout        = new Layout()

  protected var defaultJustify: Justify = Justify.NONE

  /** If true, this will attempt to track which glyph the user's mouse or other pointer is over. */
  var trackingInput: Boolean = false

  /** If true, this label will allow clicking and dragging to select a range of text. */
  var selectable: Boolean = false

  var selectionDrawable: Nullable[AnyRef] = Nullable.empty // Drawable in scene2d

  /** The global glyph index of the last glyph touched by the user. */
  var lastTouchedIndex: Int = -1

  /** The global glyph index of the last glyph hovered or dragged over by the user. */
  var overIndex: Int = -1

  /** The inclusive start index for the selected text, or -1 if no selection. */
  var selectionStart: Int = -1

  /** The inclusive end index for the selected text, or -1 if no selection. */
  var selectionEnd: Int = -1

  protected var dragging:      Boolean             = false
  protected val activeEffects: ArrayBuffer[Effect] = ArrayBuffer.empty
  private var textSpeed:       Float               = TypingConfig.DEFAULT_SPEED_PER_CHAR
  @annotation.nowarn("msg=mutated but not read")
  private var charCooldown: Float = textSpeed
  @annotation.nowarn("msg=mutated but not read")
  private var rawCharIndex: Int = -2
  @annotation.nowarn("msg=mutated but not read")
  private var glyphCharIndex: Int = -1
  @annotation.nowarn("msg=mutated but not read")
  private var glyphCharCompensation: Int = 0
  @annotation.nowarn("msg=mutated but not read")
  private var parsed:          Boolean = false
  private var paused:          Boolean = false
  private var ended:           Boolean = false
  private var skipping:        Boolean = false
  private var ignoringEvents:  Boolean = false
  private var ignoringEffects: Boolean = false

  // --- Constructors ---

  /** Creates a TypingLabel with the given text and style. */
  def this(text: String, style: Styles.LabelStyle) = {
    this()
    this.font = Nullable.fold(style.font)(new Font())(identity)
    this.layout = new Layout()
    Nullable.foreach(style.fontColor)(c => layout.setBaseColor(c))
    this.style = Nullable(style)
    defaultToken = TypingConfig.getDefaultInitialText
    workingLayout.font = Nullable(this.font)
    setText(text, modifyStored = true)
  }

  /** Creates a TypingLabel with the given text, style, and replacement font. */
  def this(text: String, style: Styles.LabelStyle, replacementFont: Font) = {
    this()
    this.font = replacementFont
    this.layout = new Layout()
    Nullable.foreach(style.fontColor)(c => layout.setBaseColor(c))
    this.style = Nullable(style)
    defaultToken = TypingConfig.getDefaultInitialText
    workingLayout.font = Nullable(this.font)
    setText(text, modifyStored = true)
  }

  /** Creates a TypingLabel with the given text and font. */
  def this(text: String, font: Font) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    defaultToken = TypingConfig.getDefaultInitialText
    workingLayout.font = Nullable(this.font)
    setText(text, modifyStored = true)
  }

  /** Creates a TypingLabel with the given text, font, and default color. */
  def this(text: String, font: Font, color: Color) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    if (color != null) layout.setBaseColor(color)
    defaultToken = TypingConfig.getDefaultInitialText
    workingLayout.font = Nullable(this.font)
    setText(text, modifyStored = true)
  }

  // --- Getters/Setters ---

  override def getFont: Font = font

  def getTypingListener:                                     Nullable[TypingListener] = typingListener
  def setTypingListener(listener: Nullable[TypingListener]): Unit                     = typingListener = listener

  def getVariables: scala.collection.mutable.HashMap[String, String] = variables

  override def getDefaultToken:                String = defaultToken
  override def setDefaultToken(token: String): Unit   =
    defaultToken = if (token != null) token else ""

  def getIntermediateText:                                                               CharSequence = intermediateText
  def setIntermediateText(text: CharSequence, preserve: Boolean, modifyStored: Boolean): Unit         = {
    intermediateText.clear()
    intermediateText.append(text)
  }

  def getOriginalText: CharSequence = originalText

  /** Sets the text for this TypingLabel. If modifyStored is true, also updates originalText. If restart is true, resets the typing animation.
    */
  def setText(text: CharSequence, modifyStored: Boolean, restart: Boolean): Unit = {
    if (modifyStored) {
      originalText.clear()
      originalText.append(text)
    }
    intermediateText.clear()
    intermediateText.append(text)
    storedText = defaultToken + text.toString
    if (wrap) layout.setTargetWidth(getWidth)
    font.markup(storedText, layout.clear())
    if (restart) {
      parsed = false
      ended = false
      paused = false
      glyphCharIndex = -1
      rawCharIndex = -2
      glyphCharCompensation = 0
      charCooldown = textSpeed
      activeEffects.clear()
      tokenEntries.clear()
    }
  }

  /** Convenience overload. */
  def setText(text: CharSequence, modifyStored: Boolean): Unit =
    setText(text, modifyStored, restart = true)

  /** Sets text and restarts. */
  override def setText(markupText: String): Unit =
    setText(markupText, modifyStored = true, restart = true)

  def getClearColor: Color = clearColor

  override def getLineHeight(globalIndex: Int): Float =
    if (font != null) font.cellHeight else 16f

  // getOffsets, getSizing, getRotations, getAdvances inherited from TextraLabel

  def setInWorkingLayout(globalIndex: Int, glyph: Long): Unit = {
    var idx   = globalIndex
    val lines = workingLayout.lines
    var i     = 0
    while (i < lines.size) {
      val line = lines(i)
      if (idx < line.glyphs.size) {
        line.glyphs(idx) = glyph
        return
      }
      idx -= line.glyphs.size
      i += 1
    }
  }

  def getInLayout(layout: Layout, globalIndex: Int): Long = boundary {
    var idx   = globalIndex
    val lines = layout.lines
    var i     = 0
    while (i < lines.size) {
      val line = lines(i)
      if (idx < line.glyphs.size) {
        break(line.glyphs(idx))
      }
      idx -= line.glyphs.size
      i += 1
    }
    0L
  }

  def getInWorkingLayout(globalIndex: Int): Long =
    getInLayout(workingLayout, globalIndex)

  def setTextSpeed(speed: Float): Unit  = textSpeed = speed
  def getTextSpeed:               Float = textSpeed

  def triggerEvent(event: String, fromEffect: Boolean): Unit =
    if (!ignoringEvents) {
      Nullable.foreach(typingListener)(_.event(event))
    }

  def isPaused:                   Boolean = paused
  def setPaused(paused: Boolean): Unit    = this.paused = paused

  def isEnded: Boolean = ended

  def isSkipping: Boolean = skipping

  def setIgnoringEvents(ignoring: Boolean): Unit    = ignoringEvents = ignoring
  def isIgnoringEvents:                     Boolean = ignoringEvents

  def setIgnoringEffects(ignoring: Boolean): Unit    = ignoringEffects = ignoring
  def isIgnoringEffects:                     Boolean = ignoringEffects

  /** Returns the length (number of glyphs) in the working layout. */
  def length(): Int = workingLayout.countGlyphs

  def setSelectable(selectable: Boolean): Unit = {
    this.selectable = selectable
    this.trackingInput = selectable
  }

  /** Skips to the end of the typing animation, immediately showing all text. */
  override def skipToTheEnd(): TextraLabel =
    skipToTheEnd(ignoreEvents = false, ignoreEffects = false)

  /** Skips to the end with optional event/effect ignoring. */
  def skipToTheEnd(ignoreEvents: Boolean, ignoreEffects: Boolean): TextraLabel = {
    skipping = true
    ignoringEvents = ignoreEvents
    ignoringEffects = ignoreEffects
    ended = true
    skipping = false
    ignoringEvents = false
    ignoringEffects = false
    this
  }

  /** Restarts the typing animation. */
  def restart(): Unit =
    setText(originalText, modifyStored = false, restart = true)

  /** Restarts with new text. */
  def restart(newText: String): Unit =
    setText(newText, modifyStored = true, restart = true)

  override def setMaxLines(maxLines: Int): Unit = {
    layout.setMaxLines(maxLines)
    workingLayout.setMaxLines(maxLines)
  }

  override def setWrap(wrap: Boolean): TypingLabel = {
    super.setWrap(wrap)
    this
  }

  override def setAlignment(alignment: Int): Unit =
    align = alignment

  def getSelectedText(): Nullable[String] =
    if (selectionStart < 0 || selectionEnd < 0 || selectionEnd < selectionStart) Nullable.empty
    else {
      val s = Math.min(selectionStart, selectionEnd)
      val e = Math.max(selectionStart, selectionEnd)
      Nullable(substring(s, e + 1))
    }

  def copySelectedText(): Unit = {
    // Deferred: requires Clipboard integration
  }
}

/** Helper class to provide incr() on the float arrays in Layout, matching the libGDX FloatArray.incr() method. */
class FloatArrayHelper(val data: ArrayBuffer[Float]) {
  def incr(index: Int, value: Float): Unit = {
    while (data.size <= index) data += 0f
    data(index) = data(index) + value
  }

  def set(index: Int, value: Float): Unit = {
    while (data.size <= index) data += 0f
    data(index) = value
  }

  def get(index: Int): Float =
    if (index < data.size) data(index) else 0f

  def setSize(newSize: Int): Unit = {
    while (data.size < newSize) data += 0f
    if (data.size > newSize) data.dropRightInPlace(data.size - newSize)
  }
}
