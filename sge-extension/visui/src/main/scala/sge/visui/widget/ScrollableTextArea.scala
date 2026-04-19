/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 124
 * Covenant-baseline-methods: ScrollTextAreaListener,ScrollableTextArea,changeText,changed,createCompatibleScrollPane,createInputListener,cullingArea,keyDown,keyTyped,prefHeight,scrollPane,setCullingArea,setParent,setText,sizeChanged,this,updateScrollLayout,updateScrollPosition
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/ScrollableTextArea.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.math.Rectangle
import sge.scenes.scene2d.{ Group, InputEvent, InputListener }
import sge.scenes.scene2d.ui.ScrollPane
import sge.scenes.scene2d.utils.Cullable
import sge.utils.Nullable

/** Custom [[VisTextArea]] supporting embedding in scroll pane by calculating required space needed for current text.
  *
  * Warning: By default this can only support vertical scrolling. Scrolling in X direction MUST be disabled. It is NOT possible to use vertical scrolling without child class properly implementing
  * [[prefWidth]] and disabling soft wraps. Example of such class is [[HighlightTextArea]].
  *
  * For best scroll pane settings you should create scroll pane using [[createCompatibleScrollPane]].
  * @author
  *   Kotcrab
  * @since 1.1.2
  */
class ScrollableTextArea(text: String, visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends VisTextArea(Nullable(text), visStyle) with Cullable {

  private var cullingArea: Nullable[Rectangle] = Nullable.empty

  def this(text: String)(using Sge) = this(text, VisUI.getSkin.get[VisTextField.VisTextFieldStyle]("textArea"))

  def this(text: String, styleName: String)(using Sge) = this(text, VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))

  override protected def createInputListener(): InputListener =
    new ScrollTextAreaListener()

  override protected[sge] def setParent(parent: Nullable[Group]): Unit = {
    super.setParent(parent)
    parent.foreach {
      case _: ScrollPane => calculateOffsets()
      case _ => ()
    }
  }

  private def updateScrollPosition(): Unit =
    if (cullingArea.isEmpty) ()
    else {
      parent.foreach {
        case scrollPane: ScrollPane =>
          if (!cullingArea.get.contains(cursorX, cullingArea.get.y)) {
            scrollPane.setScrollPercentX(cursorX / width)
          }
          if (!cullingArea.get.contains(cullingArea.get.x, height - cursorY)) {
            scrollPane.setScrollPercentY(cursorY / height)
          }
        case _ => ()
      }
    }

  override def setCullingArea(cullingArea: Nullable[Rectangle]): Unit =
    this.cullingArea = cullingArea

  /** Creates scroll pane for this scrolling text area with best possible default settings. Note that text area can belong to only one scroll pane, calling this multiple times will break previously
    * created scroll pane. The scroll pane should be embedded in container with fixed size or optionally grow property.
    * @return
    *   newly created scroll pane which can be added directly to container.
    */
  def createCompatibleScrollPane(): ScrollPane = {
    val scrollPane = new VisScrollPane(this)
    scrollPane.setOverscroll(false, false)
    scrollPane.setFlickScroll(false)
    scrollPane.setFadeScrollBars(false)
    scrollPane.setScrollbarsOnTop(true)
    scrollPane.setScrollingDisabled(true, false)
    scrollPane
  }

  override protected def sizeChanged(): Unit = {
    super.sizeChanged()
    linesShowing = 1000000000 // aka a lot, forces text area not to use its internal scrolling
  }

  override def prefHeight: Float =
    lines * style.font.lineHeight

  override def setText(str: Nullable[String]): Unit = {
    super.setText(str)
    if (!getProgrammaticChangeEvents) { // changeText WILL NOT be called when programmaticChangeEvents are disabled
      updateScrollLayout()
    }
  }

  override private[sge] def changeText(oldText: String, newText: String): Boolean = {
    val changed = super.changeText(oldText, newText)
    updateScrollLayout()
    changed
  }

  private[widget] def updateScrollLayout(): Unit = {
    invalidateHierarchy()
    layout()
    parent.foreach {
      case scrollPane: ScrollPane => scrollPane.layout()
      case _ => ()
    }
    updateScrollPosition()
  }

  class ScrollTextAreaListener extends TextAreaListener {
    override def keyDown(event: InputEvent, keycode: sge.Input.Key): Boolean = {
      updateScrollPosition()
      super.keyDown(event, keycode)
    }

    override def keyTyped(event: InputEvent, character: Char): Boolean = {
      updateScrollPosition()
      super.keyTyped(event, character)
    }
  }
}
