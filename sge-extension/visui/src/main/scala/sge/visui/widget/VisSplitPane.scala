/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: mzechner, Nathan Sweet, Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import sge.graphics.g2d.Batch
import sge.math.{ Rectangle, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.ui.{ SplitPane, WidgetGroup }
import sge.scenes.scene2d.utils.{ Drawable, Layout, ScissorStack }
import sge.utils.Nullable
import sge.visui.{ FocusManager, VisUI }
import sge.visui.widget.internal.SplitPaneCursorManager

/** Extends functionality of standard [[SplitPane]]. Style supports handle over [[Drawable]]. Due to scope of changes made this widget is not compatible with [[SplitPane]].
  * @author
  *   mzechner, Nathan Sweet, Kotcrab
  * @see
  *   [[SplitPane]]
  */
class VisSplitPane(private var firstWidget: Nullable[Actor], private var secondWidget: Nullable[Actor], var vertical: Boolean, initStyle: VisSplitPane.VisSplitPaneStyle)(using Sge)
    extends WidgetGroup {

  var style:       VisSplitPane.VisSplitPaneStyle = initStyle
  var splitAmount: Float                          = 0.5f
  var minAmount:   Float                          = 0f
  var maxAmount:   Float                          = 1f

  private val firstWidgetBounds:    Rectangle = new Rectangle()
  private val secondWidgetBounds:   Rectangle = new Rectangle()
  private[widget] val handleBounds: Rectangle = new Rectangle()
  private val firstScissors:        Rectangle = new Rectangle()
  private val secondScissors:       Rectangle = new Rectangle()

  private[widget] val lastPoint:      Vector2 = new Vector2()
  private[widget] val handlePosition: Vector2 = new Vector2()

  private var mouseOnHandle: Boolean = false

  setFirstWidget(firstWidget)
  setSecondWidget(secondWidget)
  setSize(prefWidth, prefHeight)
  initialize()

  def this(firstWidget: Nullable[Actor], secondWidget: Nullable[Actor], vertical: Boolean, styleName: String)(using Sge) =
    this(firstWidget, secondWidget, vertical, VisUI.getSkin.get[VisSplitPane.VisSplitPaneStyle](styleName))

  def this(firstWidget: Nullable[Actor], secondWidget: Nullable[Actor], vertical: Boolean)(using Sge) =
    this(firstWidget, secondWidget, vertical, "default-" + (if (vertical) "vertical" else "horizontal"))

  private def initialize(): Unit = {
    addListener(
      new SplitPaneCursorManager(this, vertical) {
        override protected def handleBoundsContains(x: Float, y: Float): Boolean =
          handleBounds.contains(x, y)

        override protected def contains(x: Float, y: Float): Boolean =
          firstWidgetBounds.contains(x, y) || secondWidgetBounds.contains(x, y) || handleBounds.contains(x, y)
      }
    )

    addListener(
      new InputListener() {
        private var draggingPointer: Int = -1

        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean =
          if (!isTouchable) false
          else if (draggingPointer != -1) false
          else if (pointer == 0 && button != sge.Input.Buttons.LEFT) false
          else if (handleBounds.contains(x, y)) {
            FocusManager.resetFocus(stage)
            draggingPointer = pointer
            lastPoint.set(x, y)
            handlePosition.set(handleBounds.x, handleBounds.y)
            true
          } else {
            false
          }

        override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
          if (pointer == draggingPointer) draggingPointer = -1

        override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
          mouseOnHandle = handleBounds.contains(x, y)
          false
        }

        override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
          if (pointer != draggingPointer) ()
          else {
            val handle = style.handle
            if (!vertical) {
              val delta      = x - lastPoint.x
              val availWidth = width - handle.minWidth
              val dragX      = Math.max(0, Math.min(availWidth, handlePosition.x + delta))
              handlePosition.x = handlePosition.x + delta
              splitAmount = Math.max(minAmount, Math.min(maxAmount, dragX / availWidth))
              lastPoint.set(x, y)
            } else {
              val delta       = y - lastPoint.y
              val availHeight = height - handle.minHeight
              val dragY       = Math.max(0, Math.min(availHeight, handlePosition.y + delta))
              handlePosition.y = handlePosition.y + delta
              splitAmount = Math.max(minAmount, Math.min(maxAmount, 1 - (dragY / availHeight)))
              lastPoint.set(x, y)
            }
            invalidate()
          }
      }
    )
  }

  override def layout(): Unit = {
    if (!vertical) calculateHorizBoundsAndPositions()
    else calculateVertBoundsAndPositions()

    firstWidget.foreach { fw =>
      fw.setBounds(firstWidgetBounds.x, firstWidgetBounds.y, firstWidgetBounds.width, firstWidgetBounds.height)
      fw match {
        case l: Layout => l.validate()
        case _ => ()
      }
    }
    secondWidget.foreach { sw =>
      sw.setBounds(secondWidgetBounds.x, secondWidgetBounds.y, secondWidgetBounds.width, secondWidgetBounds.height)
      sw match {
        case l: Layout => l.validate()
        case _ => ()
      }
    }
  }

  override def prefWidth: Float = {
    var w = 0f
    firstWidget.foreach { fw =>
      w = fw match {
        case l: Layout => l.prefWidth
        case a => a.width
      }
    }
    secondWidget.foreach { sw =>
      w += (sw match {
        case l: Layout => l.prefWidth
        case a => a.width
      })
    }
    if (!vertical) w += style.handle.minWidth
    w
  }

  override def prefHeight: Float = {
    var h = 0f
    firstWidget.foreach { fw =>
      h = fw match {
        case l: Layout => l.prefHeight
        case a => a.height
      }
    }
    secondWidget.foreach { sw =>
      h += (sw match {
        case l: Layout => l.prefHeight
        case a => a.height
      })
    }
    if (vertical) h += style.handle.minHeight
    h
  }

  override def minWidth:  Float = 0
  override def minHeight: Float = 0

  /** @return first widgets bounds, changing returned rectangle values does not have any effect */
  def getFirstWidgetBounds: Rectangle = new Rectangle(firstWidgetBounds)

  /** @return seconds widgets bounds, changing returned rectangle values does not have any effect */
  def getSecondWidgetBounds: Rectangle = new Rectangle(secondWidgetBounds)

  private def calculateHorizBoundsAndPositions(): Unit = {
    val handle      = style.handle
    val h           = height
    val availWidth  = width - handle.minWidth
    val leftAreaW   = (availWidth * splitAmount).toInt.toFloat
    val rightAreaW  = availWidth - leftAreaW
    val handleWidth = handle.minWidth

    firstWidgetBounds.set(0, 0, leftAreaW, h)
    secondWidgetBounds.set(leftAreaW + handleWidth, 0, rightAreaW, h)
    handleBounds.set(leftAreaW, 0, handleWidth, h)
  }

  private def calculateVertBoundsAndPositions(): Unit = {
    val handle       = style.handle
    val w            = width
    val h            = height
    val availHeight  = h - handle.minHeight
    val topAreaH     = (availHeight * splitAmount).toInt.toFloat
    val bottomAreaH  = availHeight - topAreaH
    val handleHeight = handle.minHeight

    firstWidgetBounds.set(0, h - topAreaH, w, topAreaH)
    secondWidgetBounds.set(0, 0, w, bottomAreaH)
    handleBounds.set(0, bottomAreaH, w, handleHeight)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    val clr = this.color
    applyTransform(batch, computeTransform())

    firstWidget.foreach { fw =>
      stage.foreach { stg =>
        stg.calculateScissors(firstWidgetBounds, firstScissors)
        if (ScissorStack.pushScissors(firstScissors)) {
          if (fw.visible) fw.draw(batch, parentAlpha * clr.a)
          batch.flush()
          ScissorStack.popScissors()
        }
      }
    }
    secondWidget.foreach { sw =>
      stage.foreach { stg =>
        stg.calculateScissors(secondWidgetBounds, secondScissors)
        if (ScissorStack.pushScissors(secondScissors)) {
          if (sw.visible) sw.draw(batch, parentAlpha * clr.a)
          batch.flush()
          ScissorStack.popScissors()
        }
      }
    }

    var handle = style.handle
    if (mouseOnHandle && isTouchable && style.handleOver.isDefined) handle = style.handleOver.get
    batch.setColor(clr.r, clr.g, clr.b, parentAlpha * clr.a)
    handle.draw(batch, handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height)
    resetTransform(batch)
  }

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] =
    if (touchable && this.touchable == Touchable.disabled) Nullable.empty
    else if (handleBounds.contains(x, y)) Nullable(this)
    else super.hit(x, y, touchable)

  def setSplitAmount(split: Float): Unit = {
    this.splitAmount = Math.max(Math.min(maxAmount, split), minAmount)
    invalidate()
  }

  def getSplit: Float = splitAmount

  def setMinSplitAmount(minAmount: Float): Unit = {
    require(minAmount >= 0, "minAmount has to be >= 0")
    require(minAmount < maxAmount, "minAmount has to be < maxAmount")
    this.minAmount = minAmount
  }

  def setMaxSplitAmount(maxAmount: Float): Unit = {
    require(maxAmount <= 1, "maxAmount has to be <= 1")
    require(maxAmount > minAmount, "maxAmount has to be > minAmount")
    this.maxAmount = maxAmount
  }

  def setWidgets(first: Nullable[Actor], second: Nullable[Actor]): Unit = {
    setFirstWidget(first)
    setSecondWidget(second)
  }

  def setFirstWidget(widget: Nullable[Actor]): Unit = {
    firstWidget.foreach(super.removeActor(_))
    firstWidget = widget
    widget.foreach(super.addActor(_))
    invalidate()
  }

  def setSecondWidget(widget: Nullable[Actor]): Unit = {
    secondWidget.foreach(super.removeActor(_))
    secondWidget = widget
    widget.foreach(super.addActor(_))
    invalidate()
  }

  override def addActor(actor: Actor): Unit =
    throw new UnsupportedOperationException("Use VisSplitPane#setWidget.")

  override def addActorAt(index: Int, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use VisSplitPane#setWidget.")

  override def addActorBefore(actorBefore: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use VisSplitPane#setWidget.")

  override def removeActor(actor: Actor): Boolean =
    if (firstWidget.isDefined && firstWidget.get == actor) { setFirstWidget(Nullable.empty); true }
    else if (secondWidget.isDefined && secondWidget.get == actor) { setSecondWidget(Nullable.empty); true }
    else true

  override def removeActor(actor: Actor, unfocus: Boolean): Boolean =
    if (firstWidget.isDefined && firstWidget.get == actor) {
      super.removeActor(actor, unfocus)
      firstWidget = Nullable.empty
      invalidate()
      true
    } else if (secondWidget.isDefined && secondWidget.get == actor) {
      super.removeActor(actor, unfocus)
      secondWidget = Nullable.empty
      invalidate()
      true
    } else {
      false
    }
}

object VisSplitPane {

  class VisSplitPaneStyle extends SplitPane.SplitPaneStyle {

    /** Optional */
    var handleOver: Nullable[Drawable] = Nullable.empty

    def this(style: VisSplitPaneStyle) = {
      this()
      this.handle = style.handle
      this.handleOver = style.handleOver
    }

    def this(handle: Drawable, handleOver: Nullable[Drawable]) = {
      this()
      this.handle = handle
      this.handleOver = handleOver
    }
  }
}
