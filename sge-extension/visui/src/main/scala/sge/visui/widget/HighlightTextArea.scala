/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 206
 * Covenant-baseline-methods: Chunk,HighlightTextArea,_highlighter,calculateOffsets,chunkUpdateScheduled,createCompatibleScrollPane,defaultColor,drawText,highlighter,highlighter_,highlights,i,init,maxAreaHeight,maxAreaWidth,offsetY,parentAlpha,prefHeight,prefWidth,processHighlighter,renderChunks,scrollPane,this,updateDisplayText
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/HighlightTextArea.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import scala.util.boundary

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont, GlyphLayout }
import sge.scenes.scene2d.ui.ScrollPane
import sge.utils.{ DynamicArray, Nullable }
import sge.visui.util.highlight.{ BaseHighlighter, Highlight }

/** Text area implementation supporting highlighting words and scrolling in both X and Y directions.
  *
  * For best scroll pane settings you should create scroll pane using [[createCompatibleScrollPane]].
  *
  * Note about overlapping highlights: this text area can handle overlapping highlights, highlights that starts earlier have higher priority. If two highlights have the exactly the same start point,
  * then it is undefined which highlight will be used and depends on how array containing highlights will be sorted.
  * @author
  *   Kotcrab
  * @see
  *   [[sge.visui.util.highlight.Highlighter]]
  * @since 1.1.2
  */
class HighlightTextArea(text: String, visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends ScrollableTextArea(text, visStyle) {

  private val highlights:           DynamicArray[Highlight]               = DynamicArray[Highlight]()
  private val renderChunks:         DynamicArray[HighlightTextArea.Chunk] = DynamicArray[HighlightTextArea.Chunk]()
  private var chunkUpdateScheduled: Boolean                               = true
  private val defaultColor:         Color                                 = Color.WHITE

  private var _highlighter: Nullable[BaseHighlighter] = Nullable.empty

  private var maxAreaWidth:  Float = 0
  private var maxAreaHeight: Float = 0

  init()

  def this(text: String)(using Sge) = this(text, VisUI.getSkin.get[VisTextField.VisTextFieldStyle])

  def this(text: String, styleName: String)(using Sge) = this(text, VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))

  private def init(): Unit =
    softwrap = false

  override protected def updateDisplayText(): Unit = {
    super.updateDisplayText()
    processHighlighter()
  }

  override protected def calculateOffsets(): Unit = {
    super.calculateOffsets()
    if (!chunkUpdateScheduled) {
      // no chunk update needed
    } else {
      chunkUpdateScheduled = false
      highlights.sort()(using (a: Highlight, b: Highlight) => a.compareTo(b))
      renderChunks.clear()

      val currentText = this.text

      val layout         = GlyphLayout()
      var carryHighlight = false
      var lineIdx        = 0
      var highlightIdx   = 0
      while (lineIdx < linesBreak.size) {
        val lineStart    = linesBreak.items(lineIdx)
        val lineEnd      = linesBreak.items(lineIdx + 1)
        var lineProgress = lineStart
        var chunkOffset  = 0f

        boundary {
          while (highlightIdx < highlights.size) {
            val highlight = highlights(highlightIdx)
            if (highlight.start > lineEnd) {
              boundary.break(())
            }

            if (highlight.start == lineProgress || carryHighlight) {
              renderChunks.add(
                HighlightTextArea.Chunk(currentText.substring(lineProgress, Math.min(highlight.end, lineEnd)), highlight.color, chunkOffset, lineIdx)
              )
              lineProgress = Math.min(highlight.end, lineEnd)

              if (highlight.end > lineEnd) {
                carryHighlight = true
              } else {
                carryHighlight = false
                highlightIdx += 1
              }
            } else {
              // protect against overlapping highlights
              var noMatch = false
              var h       = highlight
              boundary {
                while (h.start <= lineProgress) {
                  highlightIdx += 1
                  if (highlightIdx >= highlights.size) {
                    noMatch = true
                    boundary.break(())
                  }
                  h = highlights(highlightIdx)
                  if (h.start > lineEnd) {
                    noMatch = true
                    boundary.break(())
                  }
                }
              }
              if (noMatch) boundary.break(())
              renderChunks.add(HighlightTextArea.Chunk(currentText.substring(lineProgress, h.start), defaultColor, chunkOffset, lineIdx))
              lineProgress = h.start
            }

            val chunk = renderChunks.peek
            layout.setText(style.font, chunk.text)
            chunkOffset += layout.width
            // current highlight needs to be applied to next line meaning that there is no other highlights that can be applied to currently parsed line
            if (carryHighlight) boundary.break(())
          }
        }

        if (lineProgress < lineEnd) {
          renderChunks.add(HighlightTextArea.Chunk(currentText.substring(lineProgress, lineEnd), defaultColor, chunkOffset, lineIdx))
        }

        lineIdx += 2
      }

      maxAreaWidth = 0
      val lines = currentText.split("\\n")
      var li    = 0
      while (li < lines.length) {
        layout.setText(style.font, lines(li))
        maxAreaWidth = Math.max(maxAreaWidth, layout.width + 30)
        li += 1
      }

      updateScrollLayout()
    }
  }

  override protected def drawText(batch: Batch, font: BitmapFont, x: Float, y: Float): Unit = {
    maxAreaHeight = 0
    var offsetY     = 0f
    val parentAlpha = font.color.a
    var i           = firstLineShowing * 2
    while (i < (firstLineShowing + linesShowing) * 2 && i < linesBreak.size) {
      var j = 0
      while (j < renderChunks.size) {
        val chunk = renderChunks(j)
        if (chunk.lineIndex == i) {
          font.color = chunk.color
          font.color.a *= parentAlpha
          font.draw(batch, chunk.text, x + chunk.offsetX, y + offsetY)
        }
        j += 1
      }

      offsetY -= font.lineHeight
      maxAreaHeight += font.lineHeight
      i += 2
    }

    maxAreaHeight += 30
  }

  /** Processes highlighter rules, collects created highlights and schedules text area displayed text update. This should be called after highlighter rules has changed to update highlights.
    */
  def processHighlighter(): Unit = {
    highlights.clear()
    _highlighter.foreach(_.process(this, highlights))
    chunkUpdateScheduled = true
  }

  /** Changes highlighter of text area. Note that you don't have to call [[processHighlighter]] after changing highlighter - you only have to call it when highlighter rules has changed.
    */
  def highlighter: Nullable[BaseHighlighter] = _highlighter

  def highlighter_=(h: BaseHighlighter): Unit = {
    _highlighter = Nullable(h)
    processHighlighter()
  }

  override def prefWidth: Float =
    maxAreaWidth + 5

  override def prefHeight: Float =
    maxAreaHeight + 5

  override def createCompatibleScrollPane(): ScrollPane = {
    val scrollPane = super.createCompatibleScrollPane()
    scrollPane.setScrollingDisabled(false, false)
    scrollPane
  }
}

object HighlightTextArea {
  final private case class Chunk(text: String, color: Color, offsetX: Float, lineIndex: Int)
}
