/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraArea.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: TextraField → extends TextraField
 *   Convention: Multi-line text input; drawCursor/getTextY/getPrefHeight/
 *     moveCursorVertically all overridden for multi-line behavior.
 *   Note: This widget is noted as "not ready for production yet" in the original.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 172
 * Covenant-baseline-methods: TextraArea,drawCursor,getPrefHeight,getTextY,layoutHeight,lineHeight,linesHeight,moveCursorVertically,rf,s,textY,this
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraArea.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

/** A multiple-line TextraField using a Font; not ready for production yet.
  *
  * If you have to use Font but don't need multiple lines, TextraField should work. If you do need multiple-line input, you can use a libGDX BitmapFont with a scene2d.ui TextField. If you don't need
  * input, just selectable text, you can make a TypingLabel selectable with TypingLabel.setSelectable(boolean).
  */
class TextraArea(initialText: Nullable[String], areaStyle: Styles.TextFieldStyle) extends TextraField {

  // --- Initialization ---
  {
    val s = new Styles.TextFieldStyle(areaStyle)
    Nullable.foreach(s.font) { f =>
      f.enableSquareBrackets = false
      f.omitCurlyBraces = false
    }
    setStyle(s)
    Nullable.foreach(this.getStyle) { st =>
      Nullable.foreach(st.font) { f =>
        label = new TypingLabel("", new Styles.LabelStyle(f, areaStyle.fontColor))
        label.workingLayout.targetWidth = 1f
        label.setMaxLines(Int.MaxValue)
        label.setAlignment(Align.topLeft)
        label.setWrap(true)
        label.setSelectable(true)
        Nullable.foreach(areaStyle.selection) {
          case d: Drawable => label.selectionDrawable = Nullable(d)
          case _ => ()
        }
      }
    }
    writeEnters = true
    initialize()
    label.setWidth(getPrefWidth)
    setText(initialText)
    updateDisplayText()
  }

  def this(initialText: Nullable[String], areaStyle: Styles.TextFieldStyle, replacementFont: Font) = {
    this(initialText, areaStyle)
    setStyle(areaStyle)
    val rf = new Font(replacementFont)
    rf.enableSquareBrackets = false
    rf.omitCurlyBraces = false
    label = new TypingLabel("", new Styles.LabelStyle(rf, areaStyle.fontColor))
    label.workingLayout.targetWidth = 1f
    label.setMaxLines(Int.MaxValue)
    label.setAlignment(Align.topLeft)
    label.setWrap(true)
    label.setSelectable(true)
    Nullable.foreach(areaStyle.selection) {
      case d: Drawable => label.selectionDrawable = Nullable(d)
      case _ => ()
    }
    writeEnters = true
    initialize()
    label.setWidth(getPrefWidth)
    setText(initialText)
    updateDisplayText()
  }

  // --- Draw cursor (multi-line) ---

  override protected def drawCursor(cursorPatch: Drawable, batch: Batch, font: Font, x: Float, y: Float): Unit = {
    val layoutHeight = label.getHeight
    val linesHeight  = label.getCumulativeLineHeight(cursor)
    val lineHeight   = label.getLineHeight(cursor)

    if (cursor < glyphPositions.size && visibleTextStart < glyphPositions.size) {
      cursorPatch.draw(
        batch,
        x + textOffset + glyphPositions(cursor) - glyphPositions(visibleTextStart) + fontOffset,
        y + layoutHeight - linesHeight,
        cursorPatch.minWidth,
        lineHeight
      )
    }
  }

  // --- Text Y (multi-line) ---

  override protected def getTextY(font: Font, background: Nullable[Drawable]): Float = {
    var textY = 0f
    Nullable.foreach(background) { bg =>
      textY = textY - bg.topHeight
    }
    if (font.integerPosition) textY = textY.toInt.toFloat
    textY
  }

  // --- Pref height (multi-line) ---

  override def getPrefHeight: Float =
    label.getPrefHeight

  // --- Vertical cursor movement (multi-line) ---

  override protected def moveCursorVertically(forward: Boolean, jump: Boolean): Unit = boundary {
    if (jump) {
      cursor = if (forward) text.length else 0
    } else if (cursor >= glyphPositions.size) {
      break(())
    } else {
      val gp          = glyphPositions(cursor)
      val currentLine = label.getLineIndexInLayout(label.workingLayout, cursor)
      if (forward) {
        if (currentLine >= label.getWorkingLayout.lines.size - 1) {
          cursor = text.length
        } else {
          var i     = label.getWorkingLayout.countGlyphsBeforeLine(currentLine + 1)
          var found = false
          Nullable.foreach(label.getWorkingLayout.getLine(currentLine + 1)) { nextLine =>
            val n = i + nextLine.glyphs.size
            while (i < n && !found)
              if (
                i < label.workingLayout.advances.size &&
                glyphPositions.size > i &&
                glyphPositions(i) + label.workingLayout.advances(i) * 0.5f > gp
              ) {
                found = true
              } else {
                i += 1
              }
          }
          cursor = i
        }
      } else {
        if (currentLine <= 0) {
          cursor = 0
        } else {
          var i     = label.getWorkingLayout.countGlyphsBeforeLine(currentLine - 1)
          var found = false
          Nullable.foreach(label.getWorkingLayout.getLine(currentLine - 1)) { prevLine =>
            val n = i + prevLine.glyphs.size
            while (i < n && !found)
              if (
                i < label.workingLayout.advances.size &&
                glyphPositions.size > i &&
                glyphPositions(i) + label.workingLayout.advances(i) * 0.5f > gp
              ) {
                found = true
              } else {
                i += 1
              }
          }
          cursor = i
        }
      }
    }
  }
}
