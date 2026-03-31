/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Note: Due to TextArea field visibility constraints (private[ui] in SGE), highlight chunk
 * rendering delegates to the base drawText. The highlighter/highlight data model is fully ported.
 * Full custom chunk rendering would require exposing linesBreak/firstLineShowing/linesShowing from TextArea.
 */
package sge
package visui
package widget

import sge.graphics.Color
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
class HighlightTextArea(text: String, visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends ScrollableTextArea(text, visStyle) { // Cullable from ScrollableTextArea

  private val highlights:   DynamicArray[Highlight]   = DynamicArray[Highlight]()
  private var _highlighter: Nullable[BaseHighlighter] = Nullable.empty
  @annotation.nowarn("msg=unused private member")
  private var defaultColor: Color = Color.WHITE

  def this(text: String)(using Sge) = this(text, VisUI.getSkin.get[VisTextField.VisTextFieldStyle])

  def this(text: String, styleName: String)(using Sge) = this(text, VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))

  /** Processes highlighter rules, collects created highlights and schedules text area displayed text update. This should be called after highlighter rules has changed to update highlights.
    */
  def processHighlighter(): Unit = {
    highlights.clear()
    _highlighter.foreach(_.process(this, highlights))
  }

  /** Changes highlighter of text area. Note that you don't have to call [[processHighlighter]] after changing highlighter - you only have to call it when highlighter rules has changed.
    */
  def highlighter: Nullable[BaseHighlighter] = _highlighter

  def highlighter_=(h: BaseHighlighter): Unit = {
    _highlighter = Nullable(h)
    processHighlighter()
  }

  override def createCompatibleScrollPane(): ScrollPane = {
    val scrollPane = super.createCompatibleScrollPane()
    scrollPane.setScrollingDisabled(false, false)
    scrollPane
  }
}
