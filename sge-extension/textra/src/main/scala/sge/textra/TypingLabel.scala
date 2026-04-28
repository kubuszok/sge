/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingLabel.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: ObjectMap → HashMap, ArrayList → ArrayBuffer,
 *     Vector2 → (inX, inY) locals, Batch → sge.graphics.g2d.Batch,
 *     NumberUtils → java.lang.Float, MathUtils → sge.math.MathUtils
 *   Convention: TypingLabel extends TextraLabel with typing animation and
 *     effect token support. Scene2d Actor methods (act, hasParent,
 *     invalidateHierarchy, sizeChanged, isTouchable, screenToLocalCoordinates)
 *     implemented as standalone equivalents — no scene2d base class.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1416
 * Covenant-baseline-methods: FloatArrayHelper,TypingLabel,_hasParent,act,activeEffects,actualEnd,actualWidth,adjustedWidth,ai,appendText,baseX,baseY,breakAllLines,cancelSkipping,changed,charCooldown,charCounter,clearColor,clearVariables,continue_,copySelectedText,cs,cumulative,curly,current,defaultJustify,doLayout,dragging,draw,drawSection,dt,e,ended,first,get,getAdvances,getClearColor,getCumulativeLineHeight,getDefaultToken,getEllipsis,getFont,getFromIntermediate,getInLayout,getInWorkingLayout,getIntermediateText,getLineHeight,getLineInLayout,getLineIndexInLayout,getMaxLines,getOffsets,getOriginalText,getPrefHeight,getPrefWidth,getRotations,getSelectedText,getSizing,getTextSpeed,getTypingListener,getVariables,getWorkingLayout,gi,globalIndex,glyphCharCompensation,glyphCharIndex,glyphCount,hasEnded,hasEndedBefore,hasParent,hasSelection,height,i,idx,ignoringEffects,ignoringEvents,incr,index,insertInLayout,intermediateText,invalidateHierarchy,isEnded,isIgnoringEffects,isIgnoringEvents,isPaused,isSelectable,isSkipping,isTouchable,lastTouchedIndex,layoutHeight,length,lines,ln,n,o,oi,onStage,originX,originY,originalHeight,originalText,overIndex,parseTokens,parsed,pause,paused,processCharProgression,r,randomize,rawCharIndex,regenerateLayout,removeVariable,resetShader,restart,restoreOriginalText,resume,ri,rot,s,saveOriginalText,sb,selectable,selectionDrawable,selectionEnd,selectionStart,set,setAlignment,setDefaultToken,setEllipsis,setHasParent,setHeight,setIgnoringEffects,setIgnoringEvents,setInLayout,setInWorkingLayout,setIntermediateText,setMaxLines,setPaused,setSelectable,setSize,setSuperHeight,setSuperWidth,setText,setTextSpeed,setTypingListener,setVariable,setVariables,setWidth,setWrap,si,size,sizeChanged,skipToTheEnd,skipping,sn,subAct,substring,text,textSpeed,this,toSkip,toString,tokenEntries,trackingInput,triggerEvent,typingListener,variables,widgetHeight,widgetWidth,width,workingLayout
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingLabel.java
 * Covenant-verified: 2026-04-19
 *
 * Partial-port debt:
 *   - Clipboard integration pending SGE Clipboard backend.
 *   - Input tracking (trackingInput / selectable) partial —
 *     screenToLocalCoordinates, justTouched, isTouched require scene2d wiring.
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.math.MathUtils
import sge.scenes.scene2d.utils.Drawable
import sge.scenes.scene2d.utils.TransformDrawable
import sge.textra.utils.ColorUtils
import sge.utils.Align
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

  var selectionDrawable: Nullable[Drawable] = Nullable.empty

  /** The global glyph index of the last glyph touched by the user. */
  var lastTouchedIndex: Int = -1

  /** The global glyph index of the last glyph hovered or dragged over by the user. */
  var overIndex: Int = -1

  /** The inclusive start index for the selected text, or -1 if no selection. */
  var selectionStart: Int = -1

  /** The inclusive end index for the selected text, or -1 if no selection. */
  var selectionEnd: Int = -1

  protected var dragging:            Boolean             = false
  protected val activeEffects:       ArrayBuffer[Effect] = ArrayBuffer.empty
  private var textSpeed:             Float               = TypingConfig.DEFAULT_SPEED_PER_CHAR
  private var charCooldown:          Float               = textSpeed
  private var rawCharIndex:          Int                 = -2 // All chars, including color codes
  private var glyphCharIndex:        Int                 = -1 // Only renderable chars, excludes color codes
  private var glyphCharCompensation: Int                 = 0
  private var parsed:                Boolean             = false
  private var paused:                Boolean             = false
  private var ended:                 Boolean             = false
  private var skipping:              Boolean             = false
  private var ignoringEvents:        Boolean             = false
  private var ignoringEffects:       Boolean             = false
  private var onStage:               Boolean             = false

  // --- Scene2d Actor/Widget equivalents ---
  // These would normally come from Actor/Widget; since TextraLabel is standalone,
  // they are implemented directly here.
  private var _hasParent: Boolean = false

  /** Returns whether this label has a parent container. Scene2d Actor equivalent. */
  def hasParent: Boolean = _hasParent

  /** Allows setting parent status for layout triggering. */
  def setHasParent(v: Boolean): Unit = _hasParent = v

  /** Invalidates this label and all ancestors in the widget hierarchy. Scene2d Widget equivalent. */
  def invalidateHierarchy(): Unit = invalidate()

  /** Called when the actor's size has been changed. Scene2d Actor equivalent. */
  protected def sizeChanged(): Unit = ()

  /** Returns whether this label accepts touch events. Scene2d Actor equivalent. */
  def isTouchable: Boolean = true

  /** Sets width without triggering layout recalculations on the base layout. */
  override def setSuperWidth(width: Float): Unit =
    // Directly set the TextraLabel width backing field via its setter,
    // but we override setWidth below so we need a way to bypass it.
    // We call the parent's direct field access.
    super.setSuperWidth(width)

  /** Sets height without triggering layout recalculations on the base layout. */
  override def setSuperHeight(height: Float): Unit =
    super.setSuperHeight(height)

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
    workingLayout.setBaseColor(layout.baseColor)
    Color.abgr8888ToColor(clearColor, layout.getBaseColor)
    setText(storedText, modifyOriginalText = true)
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
    workingLayout.setBaseColor(layout.baseColor)
    Color.abgr8888ToColor(clearColor, layout.getBaseColor)
    setText(storedText, modifyOriginalText = true)
  }

  /** Creates a TypingLabel with the given text and font. */
  def this(text: String, font: Font) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    defaultToken = TypingConfig.getDefaultInitialText
    workingLayout.font = Nullable(this.font)
    setText(storedText, modifyOriginalText = true)
  }

  /** Creates a TypingLabel with the given text, font, and default color. */
  def this(text: String, font: Font, color: Color) = {
    this()
    this.font = font
    this.layout = new Layout()
    this.style = Nullable(new Styles.LabelStyle())
    if (color != null) layout.setBaseColor(color) // @nowarn — constructor param from Java callers
    defaultToken = TypingConfig.getDefaultInitialText
    workingLayout.font = Nullable(this.font)
    workingLayout.setBaseColor(layout.baseColor)
    Color.abgr8888ToColor(clearColor, layout.getBaseColor)
    setText(storedText, modifyOriginalText = true)
  }

  // --- Getters/Setters ---

  override def getFont: Font = font

  def getTypingListener:                                     Nullable[TypingListener] = typingListener
  def setTypingListener(listener: Nullable[TypingListener]): Unit                     = typingListener = listener

  def getVariables: scala.collection.mutable.HashMap[String, String] = variables

  override def getDefaultToken: String = defaultToken

  override def setDefaultToken(token: String): Unit = {
    val dt = if (token == null) "" else token // @nowarn — Java interop boundary
    if (this.defaultToken != dt) {
      this.parsed = false
    }
    this.defaultToken = dt
  }

  def getIntermediateText: StringBuilder = intermediateText

  def setIntermediateText(text: CharSequence, modifyOriginalText: Boolean, restart: Boolean): Unit = {
    val hasEndedBefore = this.ended
    if (text ne intermediateText) {
      intermediateText.setLength(0)
      intermediateText.append(text)
    }
    if (modifyOriginalText) saveOriginalText(text)
    if (restart) {
      this.restart()
    }
    if (hasEndedBefore) {
      this.skipToTheEnd(ignoreEvents = true, ignoreEffects = false)
    }
  }

  def getOriginalText: StringBuilder = originalText

  /** Copies the content of the given text to the original text StringBuilder. */
  protected def saveOriginalText(text: CharSequence): Unit =
    if (text ne originalText) {
      originalText.setLength(0)
      originalText.append(text)
    }

  /** Restores the original text with all tokens unchanged to this label. */
  protected def restoreOriginalText(): Unit = {
    super.setText(originalText.toString)
    this.parsed = false
  }

  //////////////////////////////////
  /// --- Text Handling ---      ///
  //////////////////////////////////

  /** Modifies the text of this label. Restarts the typing effect. */
  override def setText(markupText: String): Unit =
    this.setText(markupText, modifyOriginalText = true)

  /** Sets the text of this label. If modifyOriginalText is true, also preprocesses and updates the original text. */
  def setText(newText: String, modifyOriginalText: Boolean): Unit = {
    var text = newText
    if (modifyOriginalText) {
      if (font.omitCurlyBraces) {
        text = Parser.preprocess("{NORMAL}" + getDefaultToken + text)
      } else if (font.enableSquareBrackets) {
        text = Parser.preprocess(getDefaultToken + text)
      } else {
        text = getDefaultToken + text
      }
    }
    setText(text, modifyOriginalText, restart = true)
  }

  /** Sets the text of this label. Allows control over original text update and restart. */
  def setText(newText: String, modifyOriginalText: Boolean, restart: Boolean): Unit = {
    val hasEndedBefore = this.ended
    val text           = Parser.handleBracketMinusMarkup(newText)
    font.markup(text, layout.clear().setJustification(defaultJustify))

    if (wrap) {
      workingLayout.setTargetWidth(getWidth)
      font.markup(text, workingLayout.clear().setJustification(defaultJustify))
    } else {
      workingLayout.setTargetWidth(0f)
      font.markup(text, workingLayout.clear().setJustification(defaultJustify))
      var sw = workingLayout.getWidth
      Nullable.foreach(style) { s =>
        Nullable.foreach(s.background) { bgAny =>
          bgAny match {
            case bg: Drawable =>
              sw += bg.leftWidth + bg.rightWidth
            case _ => ()
          }
        }
      }
      setSuperWidth(sw)
    }
    if (modifyOriginalText) saveOriginalText(text)

    if (restart) {
      this.restart()
    }
    if (hasEndedBefore) {
      this.skipToTheEnd(ignoreEvents = true, ignoreEffects = false)
    }
  }

  def getClearColor: Color = clearColor

  override def regenerateLayout(): Unit = {
    font.regenerateLayout(workingLayout)
    font.calculateSize(workingLayout)
  }

  //////////////////////////////////
  /// --- External API ---       ///
  //////////////////////////////////

  /** Parses all tokens of this label. Use this after setting the text and any variables that should be replaced. */
  def parseTokens(): Unit = {
    parsed = true
    val actualEnd = ended
    ended = false
    if (font.omitCurlyBraces) {
      this.setText(Parser.preprocess("{NORMAL}" + getDefaultToken + originalText), modifyOriginalText = false, restart = false)
    } else if (font.enableSquareBrackets) {
      this.setText(Parser.preprocess(getDefaultToken + originalText), modifyOriginalText = false, restart = false)
    } else {
      this.setText(getDefaultToken + originalText.toString, modifyOriginalText = false, restart = false)
    }
    Parser.parseTokens(this)
    ended = actualEnd
  }

  /** Skips the char progression to the end, showing the entire label. Ignores all subsequent events by default. */
  override def skipToTheEnd(): TextraLabel =
    skipToTheEnd(ignoreEvents = true)

  /** Skips the char progression to the end. */
  def skipToTheEnd(ignoreEvents: Boolean): TextraLabel =
    skipToTheEnd(ignoreEvents, ignoreEffects = false)

  /** Skips the char progression to the end with optional event/effect ignoring. */
  def skipToTheEnd(ignoreEvents: Boolean, ignoreEffects: Boolean): TextraLabel = {
    skipping = true
    ignoringEvents = ignoreEvents
    ignoringEffects = ignoreEffects
    subAct(0f)
    this
  }

  /** Cancels calls to skipToTheEnd(). Useful to restore normal behavior after skipping. */
  def cancelSkipping(): Unit =
    if (skipping) {
      skipping = false
      ignoringEvents = false
      ignoringEffects = false
    }

  def isSkipping: Boolean = skipping

  def isPaused: Boolean = paused

  /** Pauses this label's character progression. */
  def pause(): Unit = paused = true

  /** Resumes this label's character progression. */
  def resume(): Unit = paused = false

  /** Returns whether this label's char progression has ended. */
  def hasEnded: Boolean = ended

  /** Restarts this label with the original text and starts the char progression right away. */
  def restart(): Unit =
    restart(getOriginalText)

  /** Restarts this label with the given text and starts the char progression right away. */
  def restart(newText: CharSequence): Unit = {
    workingLayout.atLimit = false

    // Reset cache collections
    val first = workingLayout.lines(0)
    first.glyphs.clear()
    first.width = 0
    first.height = 0
    workingLayout.lines.clear()
    workingLayout.lines += first
    activeEffects.clear()

    // Reset state
    textSpeed = TypingConfig.DEFAULT_SPEED_PER_CHAR
    charCooldown = textSpeed
    rawCharIndex = -2
    glyphCharIndex = -1
    glyphCharCompensation = 0
    parsed = false
    paused = false
    ended = false
    skipping = false
    ignoringEvents = false
    ignoringEffects = false

    // Set new text
    saveOriginalText(newText)
    invalidate()

    // Parse tokens
    parseTokens()
  }

  /** Continues this label's typing effect after it has previously ended, appending the given text. */
  def appendText(newText: CharSequence): Unit = boundary {
    if (newText == null || newText.length == 0) { // @nowarn — Java interop boundary
      break(())
    }

    if (!ended) skipToTheEnd(ignoreEvents = false, ignoreEffects = false)

    workingLayout.atLimit = false

    // Reset state
    charCooldown = textSpeed
    parsed = false
    paused = false
    ended = false
    skipping = false
    ignoringEvents = false
    ignoringEffects = false

    // Set new text
    invalidate()
    saveOriginalText(originalText.append(newText))

    // Parse tokens
    parseTokens()
  }

  /** Registers a variable and its respective replacement value to this label. */
  def setVariable(varName: String, value: String): Unit =
    if (varName != null) { // @nowarn — Java interop boundary
      val old = variables.put(varName.toUpperCase, value)
      if (value.contains("[") || value.contains("{") || (old.isDefined && (old.get.contains("[") || old.get.contains("{")))) {
        parsed = false
      }
    }

  /** Removes a variable and its respective replacement value from this label's variable map. */
  def removeVariable(varName: String): Unit =
    if (varName != null) { // @nowarn — Java interop boundary
      val old = variables.remove(varName.toUpperCase)
      if (old.isDefined && (old.get.contains("[") || old.get.contains("{"))) {
        parsed = false
      }
    }

  /** Registers a set of variables and their respective replacement values to this label. */
  def setVariables(variableMap: scala.collection.mutable.HashMap[String, String]): Unit = {
    this.variables.clear()
    if (variableMap != null) { // @nowarn — Java interop boundary
      for ((key, value) <- variableMap) {
        val old = this.variables.put(key.toUpperCase, value)
        if (value.contains("[") || value.contains("{") || (old.isDefined && (old.get.contains("[") || old.get.contains("{")))) {
          parsed = false
        }
      }
    }
  }

  /** Registers a set of variables from a Java-style Map. */
  def setVariables(variableMap: java.util.Map[String, String]): Unit = {
    this.variables.clear()
    if (variableMap != null) { // @nowarn — Java interop boundary
      val it = variableMap.entrySet().iterator()
      while (it.hasNext) {
        val entry = it.next()
        if (entry.getKey != null) { // @nowarn — Java interop boundary
          val value = entry.getValue
          val old   = this.variables.put(entry.getKey.toUpperCase, value)
          if (value.contains("[") || value.contains("{") || (old.isDefined && (old.get.contains("[") || old.get.contains("{")))) {
            parsed = false
          }
        }
      }
    }
  }

  /** Removes all variables from this label. */
  def clearVariables(): Unit =
    this.variables.clear()

  def isEnded: Boolean = ended

  def setIgnoringEvents(ignoring: Boolean): Unit    = ignoringEvents = ignoring
  def isIgnoringEvents:                     Boolean = ignoringEvents

  def setIgnoringEffects(ignoring: Boolean): Unit    = ignoringEffects = ignoring
  def isIgnoringEffects:                     Boolean = ignoringEffects

  /** Returns the length (number of glyphs) in the working layout. */
  def length(): Int = workingLayout.countGlyphs

  /** Returns true if and only if selectable is true and trackingInput is true. */
  def isSelectable: Boolean = selectable && trackingInput

  /** If given true, makes the text selectable and ensures trackingInput is true. */
  def setSelectable(selectable: Boolean): TypingLabel = {
    this.selectable = selectable
    this.trackingInput |= selectable
    this
  }

  def getTextSpeed:               Float = textSpeed
  def setTextSpeed(speed: Float): Unit  = textSpeed = speed

  def triggerEvent(event: String, always: Boolean): Unit =
    if (typingListener.isDefined && (always || !ignoringEvents)) {
      Nullable.foreach(typingListener)(_.event(event))
    }

  def setPaused(paused: Boolean): Unit = this.paused = paused

  override def getLineHeight(globalIndex: Int): Float = boundary {
    var idx = globalIndex
    var i   = 0
    val n   = workingLayout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = workingLayout.lines(i).glyphs
      if (idx < glyphs.size) break(workingLayout.lines(i).height)
      else idx -= glyphs.size
      i += 1
    }
    font.cellHeight
  }

  /** Gets the height of the Line containing the glyph at the given index, plus the heights of all preceding lines. */
  def getCumulativeLineHeight(globalIndex: Int): Float = boundary {
    var cumulative = 0f
    var idx        = globalIndex
    var i          = 0
    val n          = workingLayout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = workingLayout.lines(i).glyphs
      if (idx < glyphs.size) {
        break(cumulative + workingLayout.lines(i).height)
      }
      idx -= glyphs.size
      cumulative += workingLayout.lines(i).height
      i += 1
    }
    cumulative
  }

  /** Contains one float per glyph; each is a rotation in degrees to apply to that glyph (around its center). */
  override def getRotations: FloatArrayHelper = new FloatArrayHelper(workingLayout.rotations)

  /** Contains two floats per glyph; even items are x offsets, odd items are y offsets. */
  override def getOffsets: FloatArrayHelper = new FloatArrayHelper(workingLayout.offsets)

  /** Contains two floats per glyph, as size multipliers; even items apply to x, odd items apply to y. */
  override def getSizing: FloatArrayHelper = new FloatArrayHelper(workingLayout.sizing)

  /** Contains one float per glyph; each is a multiplier that affects the x-advance of that glyph. */
  override def getAdvances: FloatArrayHelper = new FloatArrayHelper(workingLayout.advances)

  /** Returns the working layout, which is the layout that gets displayed. */
  def getWorkingLayout: Layout = workingLayout

  def setInWorkingLayout(globalIndex: Int, glyph: Long): Unit = boundary {
    var idx = globalIndex
    var i   = 0
    val n   = workingLayout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = workingLayout.lines(i).glyphs
      if (idx < glyphs.size) {
        glyphs(idx) = glyph
        break(())
      }
      idx -= glyphs.size
      i += 1
    }
  }

  def getInLayout(layout: Layout, globalIndex: Int): Long = boundary {
    var idx = globalIndex
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) {
        break(glyphs(idx))
      }
      idx -= glyphs.size
      i += 1
    }
    0xffffffL
  }

  def getInWorkingLayout(globalIndex: Int): Long =
    getInLayout(workingLayout, globalIndex)

  def setInLayout(layout: Layout, globalIndex: Int, newGlyph: Long): Unit = boundary {
    var idx = globalIndex
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) {
        glyphs(idx) = newGlyph
        break(())
      }
      idx -= glyphs.size
      i += 1
    }
  }

  def insertInLayout(layout: Layout, globalIndex: Int, newGlyph: Long): Unit = boundary {
    var idx = globalIndex
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx <= glyphs.size) {
        glyphs.insert(idx, newGlyph)
        break(())
      }
      idx -= glyphs.size
      i += 1
    }
  }

  def insertInLayout(layout: Layout, globalIndex: Int, text: CharSequence): Unit = boundary {
    var current = (Integer.reverseBytes(java.lang.Float.floatToIntBits(layout.baseColor)).toLong & -2L) << 32
    var idx     = globalIndex
    var i       = 0
    val n       = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) {
        // inserting mid-line
        current = glyphs(idx) & 0xffffffffffff0000L
        var j = 0
        while (j < text.length) {
          glyphs.insert(idx + j, current | text.charAt(j).toLong)
          j += 1
        }
        break(())
      } else if (idx == glyphs.size) {
        // appending to a line
        if (idx != 0) {
          current = glyphs(idx - 1) & 0xffffffffffff0000L
        }
        var j = 0
        while (j < text.length) {
          glyphs.insert(idx + j, current | text.charAt(j).toLong)
          j += 1
        }
        break(())
      } else {
        idx -= glyphs.size
      }
      i += 1
    }
  }

  def getFromIntermediate(globalIndex: Int): Long =
    if (globalIndex >= 0 && intermediateText.length > globalIndex) intermediateText.charAt(globalIndex).toLong
    else 0xffffffL

  /** Given a Layout and an index, returns the Line object that holds the glyph at that index. */
  def getLineInLayout(layout: Layout, globalIndex: Int): Nullable[Line] = boundary {
    var idx = globalIndex
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) break(Nullable(layout.lines(i)))
      idx -= glyphs.size
      i += 1
    }
    Nullable.empty
  }

  /** Given a Layout and an index, returns the index of the Line holding the glyph. */
  def getLineIndexInLayout(layout: Layout, globalIndex: Int): Int = boundary {
    if (globalIndex == -1) break(0)
    if (globalIndex == -2) break(layout.lineCount - 1)
    var idx = globalIndex
    var i   = 0
    val n   = layout.lineCount
    while (i < n && idx >= 0) {
      val glyphs = layout.lines(i).glyphs
      if (idx < glyphs.size) break(i)
      idx -= glyphs.size
      i += 1
    }
    layout.lineCount - 1
  }

  /** The maximum number of Lines this label can display. */
  override def getMaxLines: Int = workingLayout.maxLines

  override def setMaxLines(maxLines: Int): Unit = {
    layout.setMaxLines(maxLines)
    workingLayout.setMaxLines(maxLines)
  }

  /** Gets the ellipsis. */
  override def getEllipsis: Nullable[String] = workingLayout.ellipsis

  /** Sets the ellipsis text. */
  override def setEllipsis(ellipsis: Nullable[String]): Unit =
    workingLayout.setEllipsis(ellipsis)

  /** Gets a String from the working layout from start (inclusive) to end (exclusive). */
  override def substring(start: Int, end: Int): String =
    substring(start, end, multiLine = true)

  /** Gets a String from the working layout from start (inclusive) to end (exclusive). */
  def substring(start: Int, end: Int, multiLine: Boolean): String = boundary {
    val s          = Math.max(0, start)
    val e          = Math.min(Math.max(workingLayout.countGlyphs, s), end)
    var index      = s
    val sb         = new StringBuilder(e - s)
    var glyphCount = 0
    var i          = 0
    val n          = workingLayout.lineCount
    while (i < n && index >= 0) {
      val glyphs = workingLayout.lines(i).glyphs
      if (index < glyphs.size) {
        val fin = index - s - glyphCount + e
        while (index < fin && index < glyphs.size) {
          val c = (glyphs(index) & 0xffff).toChar
          if (c >= '\ue000' && c <= '\uf800') {
            Nullable.fold(font.namesByCharCode) {
              sb.append(c)
            } { nbc =>
              nbc.get(c.toInt) match {
                case Some(name) => sb.append(name)
                case None       => sb.append(c)
              }
            }
          } else {
            if (c == '\u0002') sb.append('[')
            else if (c != '\u200b') sb.append(c) // do not print zero-width space
          }
          glyphCount += 1
          index += 1
        }
        if (glyphCount == e - s) break(sb.toString)
        index = 0
      } else {
        index -= glyphs.size
      }
      if (multiLine) sb.append('\n')
      i += 1
    }
    ""
  }

  /** If this label is selectable and there is a selected range, returns that text; otherwise empty. */
  def getSelectedText(): String =
    if (!selectable || selectionStart < 0 || selectionEnd < 0) ""
    else substring(selectionStart, selectionEnd + 1)

  /** If this label is selectable and there is a selected range, copies it to the clipboard. Returns false when Clipboard integration is unavailable.
    */
  def copySelectedText(): Boolean =
    if (!selectable || selectionStart < 0 || selectionEnd < 0) false
    else {
      // Clipboard integration depends on SGE Clipboard backend (see partial-port debt)
      false
    }

  /** If this label is selectable and there is a selected range of text, returns true. */
  def hasSelection: Boolean =
    selectable && selectionEnd >= 0 && selectionEnd >= selectionStart

  override def setWrap(wrap: Boolean): TypingLabel = {
    super.setWrap(wrap)
    this
  }

  override def setAlignment(alignment: sge.utils.Align): Unit =
    align = alignment

  override def toString: String = substring(0, Int.MaxValue)

  //////////////////////////////////
  /// --- Core Functionality --- ///
  //////////////////////////////////

  /** Called each frame. Updates typing animation. */
  override def act(delta: Float): Unit =
    // In a scene2d setup, super.act(delta) processes Actions.
    // Since we have no Actor base class, we just call subAct directly.
    subAct(delta)

  /** Performs the non-Action-related logic of parsing tokens, skipping/advancing (when needed), ensuring the layout and workingLayout fields match internally, calculating size changes, and handling
    * all effects.
    */
  protected def subAct(delta: Float): Unit = {
    // Force token parsing
    if (!parsed) {
      parseTokens()
    }

    // Update cooldown and process char progression
    if (skipping || (!ended && !paused)) {
      if (skipping || { charCooldown -= delta; charCooldown < 0.0f }) {
        processCharProgression()
      }
    }
    val glyphCount = layout.countGlyphs

    getOffsets.setSize(glyphCount + glyphCount)
    var oi = 0
    while (oi < glyphCount + glyphCount) {
      workingLayout.offsets(oi) = layout.offsets(oi)
      oi += 1
    }
    getSizing.setSize(glyphCount + glyphCount)
    var si = 0
    while (si < glyphCount + glyphCount) {
      workingLayout.sizing(si) = layout.sizing(si)
      si += 1
    }
    getRotations.setSize(glyphCount)
    var ri = 0
    while (ri < glyphCount) {
      workingLayout.rotations(ri) = layout.rotations(ri)
      ri += 1
    }
    getAdvances.setSize(glyphCount)
    var ai = 0
    while (ai < glyphCount) {
      workingLayout.advances(ai) = layout.advances(ai)
      ai += 1
    }

    font.calculateSize(workingLayout)

    // Apply effects
    if (!ignoringEffects) {
      var i = activeEffects.size - 1
      while (i >= 0) {
        val effect = activeEffects(i)
        effect.update(delta)
        val start = effect.indexStart
        val end   = if (effect.indexEnd >= 0) effect.indexEnd else glyphCharIndex

        // If effect is finished, remove it
        if (effect.isFinished) {
          activeEffects.remove(i)
        } else {
          // Apply effect to glyphs
          var j = Math.max(0, start)
          while (j <= glyphCharIndex && j <= end && j < glyphCount) {
            val glyph = getInLayout(workingLayout, j)
            if (glyph == 0xffffffL) {
              j = glyphCharIndex + 1 // break inner loop
            } else {
              effect.apply(glyph, j, delta)
              j += 1
            }
          }
        }
        i -= 1
      }
    }
  }

  /** Returns a seeded random float between -2.4f and -0.4f for natural typing speed variation. */
  private def randomize(seed: Int): Float =
    java.lang.Float.intBitsToFloat(
      ((seed.toLong ^ 0x9e3779b97f4a7c15L) * 0xd1b54a32d192ed03L >>> 41).toInt | 0x40000000
    ) - 4.4f

  /** Process char progression according to current cooldown and process all tokens in the current index. */
  private def processCharProgression(): Unit = {
    // Keep a counter of how many chars we're processing in this tick.
    var charCounter = 0
    // Process chars while there's room for it
    var continue_ = true
    while (continue_ && (skipping || charCooldown < 0.0f)) {
      // Apply compensation to glyph index, if any
      if (glyphCharCompensation != 0) {
        if (glyphCharCompensation > 0) {
          glyphCharIndex += 1
          glyphCharCompensation -= 1
        } else {
          glyphCharIndex -= 1
          glyphCharCompensation += 1
        }

        // Increment cooldown and wait for it
        if (textSpeed < 0f) {
          // "natural" text speed
          charCooldown += textSpeed * randomize(glyphCharIndex)
        } else {
          charCooldown += textSpeed
        }
      } else {
        // Increase raw char index
        rawCharIndex += 1

        // Get next character and calculate cooldown increment
        val layoutSize = layout.countGlyphs

        // If char progression is finished, or if text is empty, notify listener and abort routine
        if (layoutSize == 0 || glyphCharIndex >= layoutSize) {
          if (!ended) {
            ended = true
            skipping = false
            Nullable.foreach(typingListener)(_.end())
          }
          continue_ = false
        } else {
          // Process tokens according to the current index
          var tokenProcessed = false
          if (tokenEntries.nonEmpty && tokenEntries.last.index == rawCharIndex) {
            val entry    = tokenEntries.remove(tokenEntries.size - 1)
            val token    = entry.token
            val category = entry.category
            rawCharIndex = entry.endIndex - 1
            // Process tokens
            category match {
              case TokenCategory.SPEED =>
                textSpeed = entry.floatValue
                tokenProcessed = true
              case TokenCategory.WAIT =>
                charCooldown += entry.floatValue
                tokenProcessed = true
              case TokenCategory.EVENT =>
                triggerEvent(Nullable.getOrElse(entry.stringValue)(""), false)
                tokenProcessed = true
              case TokenCategory.EFFECT_START | TokenCategory.EFFECT_END =>
                // Get effect class
                val isStart    = category == TokenCategory.EFFECT_START
                val effectName = if (isStart) token else token.substring(3)

                // End all effects of the same type
                var ei = 0
                val es = activeEffects.size
                while (ei < es) {
                  val eff = activeEffects(ei)
                  if (eff.indexEnd < 0) {
                    if (Nullable.fold(eff.name)(false)(_.equals(effectName))) {
                      eff.indexEnd = glyphCharIndex
                    }
                  }
                  ei += 1
                }

                // Create new effect if necessary
                if (isStart) {
                  Nullable.foreach(entry.effect) { eff =>
                    eff.indexStart = glyphCharIndex + 1
                    activeEffects += eff
                  }
                }
                tokenProcessed = true
              case _ =>
                continue_ = false
                tokenProcessed = true
            }
          }

          if (!tokenProcessed) {
            val safeIndex = MathUtils.clamp(glyphCharIndex + 1, 0, layoutSize - 1)
            if (layoutSize > 0) {
              val baseChar           = getInLayout(layout, safeIndex)
              val intervalMultiplier = TypingConfig.INTERVAL_MULTIPLIERS_BY_CHAR.getOrElse((baseChar & 0xffff).toInt, 1f)
              if (textSpeed < 0f) {
                charCooldown += textSpeed * randomize(glyphCharIndex) * intervalMultiplier
              } else {
                charCooldown += textSpeed * intervalMultiplier
              }
            }

            // Increase glyph char index for all characters
            if (rawCharIndex > 0) {
              glyphCharIndex += 1
            }

            // Notify listener about char progression
            if (glyphCharIndex >= 0 && glyphCharIndex < layoutSize && rawCharIndex >= 0) {
              Nullable.foreach(typingListener)(_.onChar(getInLayout(layout, glyphCharIndex)))
            }

            // Break loop if this was our first glyph to prevent glyph issues.
            if (glyphCharIndex == 0 && !skipping) {
              charCooldown = Math.abs(textSpeed)
              continue_ = false
            }

            // Break loop if enough chars were processed
            if (continue_) {
              charCounter += 1
              val charLimit = TypingConfig.CHAR_LIMIT_PER_FRAME
              if (!skipping && charLimit > 0 && charCounter > charLimit && textSpeed != 0f) {
                charCooldown = Math.max(charCooldown, Math.abs(textSpeed))
                continue_ = false
              }
            }
          }
        }
      }
    }

    if (wrap) {
      val actualWidth = getWidth
      if (actualWidth != 0f) {
        workingLayout.setTargetWidth(actualWidth)
      }
    }

    invalidate()
  }

  //////////////////////////////////
  /// --- Size / Layout ---      ///
  //////////////////////////////////

  override def setWidth(width: Float): Unit =
    if (this.getWidth != width) {
      this.setSuperWidth(width)
      sizeChanged()
      if (workingLayout != null) { // @nowarn — guard for init ordering
        workingLayout.setTargetWidth(width)
        workingLayout.justification = defaultJustify
        font.regenerateLayout(workingLayout)
        invalidateHierarchy()
      }
    }

  override def setHeight(height: Float): Unit =
    if (this.getHeight != height) {
      this.setSuperHeight(height)
      sizeChanged()
      if (workingLayout != null) { // @nowarn — guard for init ordering
        workingLayout.justification = defaultJustify
        font.regenerateLayout(workingLayout)
        invalidateHierarchy()
      }
    }

  override def setSize(width: Float, height: Float): Unit = {
    var changed = false
    if (this.getWidth != width) {
      this.setSuperWidth(width)
      changed = true
    }
    if (this.getHeight != height) {
      this.setSuperHeight(height)
      changed = true
    }
    if (changed) {
      sizeChanged()
      if (workingLayout != null) { // @nowarn — guard for init ordering
        workingLayout.setTargetWidth(width)
        workingLayout.justification = defaultJustify
        font.regenerateLayout(workingLayout)
        invalidateHierarchy()
      }
    }
  }

  override def getPrefWidth: Float = {
    if (!parsed) {
      parseTokens()
    }
    if (wrap) 0f
    else {
      var width = workingLayout.getWidth
      Nullable.foreach(style) { s =>
        Nullable.foreach(s.background) { bgAny =>
          bgAny match {
            case bg: Drawable =>
              width = Math.max(width + bg.leftWidth + bg.rightWidth, bg.minWidth)
            case _ => ()
          }
        }
      }
      width
    }
  }

  override def getPrefHeight: Float = {
    if (!parsed) {
      parseTokens()
    }
    var height = workingLayout.getHeight
    Nullable.foreach(style) { s =>
      Nullable.foreach(s.background) { bgAny =>
        bgAny match {
          case bg: Drawable =>
            height = Math.max(height + bg.bottomHeight + bg.topHeight, bg.minHeight)
          case _ => ()
        }
      }
    }
    height
  }

  override def doLayout(): Unit = {
    val width         = getWidth
    var adjustedWidth = width
    Nullable.foreach(style) { s =>
      Nullable.foreach(s.background) { bgAny =>
        bgAny match {
          case bg: Drawable =>
            adjustedWidth -= (bg.leftWidth + bg.rightWidth)
          case _ => ()
        }
      }
    }
    val originalHeight = workingLayout.getHeight
    val actualWidth    = font.calculateSize(workingLayout)

    if (wrap) {
      if (adjustedWidth == 0f || workingLayout.getTargetWidth != adjustedWidth || actualWidth > adjustedWidth) {
        if (adjustedWidth != 0f) {
          workingLayout.setTargetWidth(adjustedWidth)
        }
        workingLayout.justification = defaultJustify
        font.regenerateLayout(workingLayout)
        font.calculateSize(workingLayout)
      }

      // If the call to calculateSize() changed workingLayout's height, update and invalidateHierarchy().
      val newHeight = workingLayout.getHeight
      if (!MathUtils.isEqual(originalHeight, newHeight)) {
        setSuperHeight(newHeight)
        invalidateHierarchy()
      }
    }
    // Once a TypingLabel has been added to the Stage, restart the effect so it
    // can do more than what it could do before it knew its own dimensions.
    if (!onStage && hasParent) {
      onStage = true
      if (!ended) {
        restart()
      }
    }
  }

  //////////////////////////////////
  /// --- Drawing ---            ///
  //////////////////////////////////

  /** Draws this TypingLabel. Delegates to drawSection with full range. */
  override def draw(batch: Batch, parentAlpha: Float): Unit =
    drawSection(batch, parentAlpha, 0, -1)

  /** Renders a subsection of the glyphs in this label. */
  def drawSection(batch: Batch, parentAlpha: Float, startIndex: Int, endIndex: Int): Unit = boundary {
    validate()

    val rot     = getRotation
    val originX = getOriginX
    val originY = getOriginY
    val sn      = MathUtils.sinDeg(rot)
    val cs      = MathUtils.cosDeg(rot)

    val lines = workingLayout.lineCount
    var baseX = getX
    var baseY = getY

    // These two blocks use different height measurements, so center vertical is offset once by half the layout
    // height, and once by half the widget height.
    val layoutHeight = workingLayout.getHeight * getScaleY
    if (align.isBottom) {
      baseX -= sn * layoutHeight
      baseY += cs * layoutHeight
    } else if (align.isCenterVertical) {
      baseX -= sn * layoutHeight * 0.5f
      baseY += cs * layoutHeight * 0.5f
    }
    val widgetHeight = getHeight * getScaleY
    if (align.isTop) {
      baseX -= sn * widgetHeight
      baseY += cs * widgetHeight
    } else if (align.isCenterVertical) {
      baseX -= sn * widgetHeight * 0.5f
      baseY += cs * widgetHeight * 0.5f
    }

    val widgetWidth = getWidth * getScaleX
    if (align.isRight) {
      baseX += cs * widgetWidth
      baseY += sn * widgetWidth
    } else if (align.isCenterHorizontal) {
      baseX += cs * widgetWidth * 0.5f
      baseY += sn * widgetWidth * 0.5f
    }

    Nullable.foreach(style) { s =>
      Nullable.foreach(s.background) { bgAny =>
        bgAny match {
          case bg: Drawable =>
            if (align.isLeft) {
              baseX += cs * bg.leftWidth
              baseY += sn * bg.leftWidth
            } else if (align.isRight) {
              baseX -= cs * bg.rightWidth
              baseY -= sn * bg.rightWidth
            } else {
              baseX += cs * (bg.leftWidth - bg.rightWidth) * 0.5f
              baseY += sn * (bg.leftWidth - bg.rightWidth) * 0.5f
            }
            if (align.isBottom) {
              baseX -= sn * bg.bottomHeight
              baseY += cs * bg.bottomHeight
            } else if (align.isTop) {
              baseX += sn * bg.topHeight
              baseY -= cs * bg.topHeight
            } else {
              baseX -= sn * (bg.bottomHeight - bg.topHeight) * 0.5f
              baseY += cs * (bg.bottomHeight - bg.topHeight) * 0.5f
            }
            bg match {
              case td: TransformDrawable =>
                try
                  td.draw(batch, getX, getY, originX, originY, getWidth, getHeight, 1f, 1f, rot)
                catch {
                  case _: UnsupportedOperationException | _: ClassCastException =>
                    bg.draw(batch, getX, getY, getWidth, getHeight)
                }
              case _ =>
                bg.draw(batch, getX, getY, getWidth, getHeight)
            }
          case _ => ()
        }
      }
    }

    if (layout.lines.isEmpty || parentAlpha <= 0f) break(())

    val resetShader = font.getDistanceField != Font.DistanceFieldType.STANDARD &&
      Nullable.fold(font.shader)(true)(sh => batch.shader ne sh)
    if (resetShader) {
      font.enableShader(batch)
    }
    batch.color.set(getColor).a *= parentAlpha
    batch.color = batch.color

    var globalIndex = startIndex - 1
    var o           = 0
    var s           = 0
    var r           = 0
    var gi          = 0
    var curly       = false
    var toSkip      = 0

    // --- Main glyph rendering loop ---
    var ln            = 0
    var breakAllLines = false
    while (ln < lines && !breakAllLines) {
      val line = workingLayout.lines(ln)

      if (line.glyphs.nonEmpty) {
        toSkip += line.glyphs.size
        if (toSkip >= startIndex) {
          val lineWidth  = line.width * getScaleX
          val lineHeight = line.height * getScaleY

          baseX += sn * lineHeight
          baseY -= cs * lineHeight

          var x = baseX
          var y = baseY

          val worldOriginX = x + originX
          val worldOriginY = y + originY
          val fx           = -originX
          val fy           = -originY
          x = cs * fx - sn * fy + worldOriginX
          y = sn * fx + cs * fy + worldOriginY

          var xChange = 0f
          var yChange = 0f

          if (align.isCenterHorizontal) {
            x -= cs * (lineWidth * 0.5f)
            y -= sn * (lineWidth * 0.5f)
          } else if (align.isRight) {
            x -= cs * lineWidth
            y -= sn * lineWidth
          }

          var f: Nullable[Font] = Nullable.empty
          var kern  = -1
          var start = if (toSkip - line.glyphs.size < startIndex) startIndex - (toSkip - line.glyphs.size) else 0
          val end   = if (endIndex < 0) glyphCharIndex else Math.min(glyphCharIndex, endIndex - 1)
          val lim   = Math.min(Math.min(Math.min(getRotations.size, getAdvances.size), getOffsets.size >> 1), getSizing.size >> 1)
          var i     = start
          val n     = line.glyphs.size
          while (i < n && r < lim && !breakAllLines)
            if (gi > end) {
              breakAllLines = true
            } else {
              val glyph = line.glyphs(i)
              val ch    = (glyph & 0xffff).toChar
              Nullable.foreach(font.family) { fam =>
                f = Nullable(fam.connected((glyph >>> 16 & 15).toInt))
              }
              val fFont   = Nullable.getOrElse(f)(font)
              val descent = fFont.descent * fFont.scaleY * getScaleY

              var skipGlyph = false
              if (font.omitCurlyBraces) {
                if (curly) {
                  if (i == start) {
                    start += 1
                  }
                  if (ch == '}') {
                    curly = false
                    skipGlyph = true
                  } else if (ch == '{') {
                    curly = false
                    start -= 1
                  } else {
                    skipGlyph = true
                  }
                } else if (ch == '{') {
                  curly = true
                  if (i == start) {
                    start += 1
                  }
                  skipGlyph = true
                }
              }

              if (!skipGlyph) {
                val a          = getAdvances.get(r) * getScaleX
                val halfWidth  = fFont.cellWidth * 0.5f * getScaleX
                val halfHeight = fFont.cellHeight * 0.5f * getScaleY

                if (i == start) {
                  x -= halfWidth

                  x += cs * halfWidth
                  y += sn * halfWidth

                  y += descent
                  x += sn * (descent - halfHeight)
                  y -= cs * (descent - halfHeight)

                  val reg = fFont.mapping.getOrElse(ch.toInt, null) // @nowarn — Java interop: HashMap getOrElse default
                  if (reg != null && reg.offsetX < 0 && !fFont.isMono && !(ch >= '\ue000' && ch < '\uf800')) {
                    val ox = reg.offsetX * fFont.scaleX * a
                    if (ox < 0) {
                      xChange -= cs * ox
                      yChange -= sn * ox
                    }
                  }
                }

                Nullable.foreach(fFont.kerning) { kernMap =>
                  kern = kern << 16 | (line.glyphs(i) & 0xffff).toInt
                  val amt = kernMap.getOrElse(kern, 0f) * fFont.scaleX * a
                  xChange += cs * amt
                  yChange += sn * amt
                }
                if (Nullable.isEmpty(fFont.kerning)) {
                  kern = -1
                }
                globalIndex += 1
                if (endIndex >= 0 && globalIndex >= endIndex) {
                  breakAllLines = true
                } else {
                  val bgc =
                    if (selectable && Nullable.isEmpty(selectionDrawable) && selectionStart <= globalIndex && selectionEnd >= globalIndex) {
                      ColorUtils.offsetLightness((glyph >>> 32).toInt, 0.5f)
                    } else {
                      0
                    }

                  var xx = x + xChange + getOffsets.get(o) * getScaleX
                  o += 1
                  var yy = y + yChange + getOffsets.get(o) * getScaleY
                  o += 1
                  if (fFont.integerPosition) {
                    xx = xx.toInt.toFloat
                    yy = yy.toInt.toFloat
                  }

                  val single = fFont.drawGlyph(batch, glyph, xx, yy, getRotations.get(r) + rot, getSizing.get(s) * getScaleX, getSizing.get(s + 1) * getScaleY, bgc, a)
                  s += 2
                  r += 1

                  xChange += cs * single
                  yChange += sn * single
                }
              }
              gi += 1
              i += 1
            }
        }
      }
      ln += 1
    }

    if (resetShader) {
      batch.shader = Nullable.empty
    }
  }
}

/** Helper class to provide incr() on the float arrays in Layout, matching the libGDX FloatArray.incr() method. */
class FloatArrayHelper(val data: ArrayBuffer[Float]) {

  def size: Int = data.size

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
