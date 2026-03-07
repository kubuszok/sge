/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/SplitPane.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) context; GdxRuntimeException -> SgeError.GraphicsError; boundary/break in touchDown; Skin constructors present
 *   Idiom: split packages
 *   Fixes: Removed redundant Java-style getters (splitAmount, minAmount, maxAmount, vertical are public vars; style via Styleable)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.math.{ Rectangle, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.utils.{ Drawable, Layout, ScissorStack }
import sge.Input.Button
import sge.utils.{ Nullable, SgeError }

/** A container that contains two widgets and is divided either horizontally or vertically. The user may resize the widgets. The child widgets are always sized to fill their side of the SplitPane. <p>
  * Minimum and maximum split amounts can be set to limit the motion of the resizing handle. The handle position is also prevented from shrinking the children below their minimum sizes. If these
  * limits over-constrain the handle, it will be locked and placed at an averaged location, resulting in cropped children. The minimum child size can be ignored (allowing dynamic cropping) by wrapping
  * the child in a {@linkplain Container} with a minimum size of 0 and {@linkplain Container#fill() fill()} set, or by overriding {@link #clampSplitAmount()}. <p> The preferred size of a SplitPane is
  * that of the child widgets and the size of the {@link SplitPaneStyle#handle}. The widgets are sized depending on the SplitPane size and the {@link #setSplitAmount(float) split position}.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class SplitPane(
  firstWidget:  Nullable[Actor],
  secondWidget: Nullable[Actor],
  var vertical: Boolean,
  style:        SplitPane.SplitPaneStyle
)(using Sge)
    extends WidgetGroup
    with Styleable[SplitPane.SplitPaneStyle] {
  import SplitPane._

  private var _style:        SplitPaneStyle  = scala.compiletime.uninitialized
  private var _firstWidget:  Nullable[Actor] = Nullable.empty
  private var _secondWidget: Nullable[Actor] = Nullable.empty
  var splitAmount:           Float           = 0.5f
  var minAmount:             Float           = 0f
  var maxAmount:             Float           = 1f

  private val firstWidgetBounds:  Rectangle = Rectangle()
  private val secondWidgetBounds: Rectangle = Rectangle()
  val handleBounds:               Rectangle = Rectangle()
  var cursorOverHandle:           Boolean   = false
  private val tempScissors:       Rectangle = Rectangle()

  var lastPoint:      Vector2 = Vector2()
  var handlePosition: Vector2 = Vector2()

  setStyle(style)
  setFirstWidget(firstWidget)
  setSecondWidget(secondWidget)
  setSize(getPrefWidth, getPrefHeight)
  initialize()

  def this(firstWidget: Nullable[Actor], secondWidget: Nullable[Actor], vertical: Boolean, skin: Skin)(using Sge) = {
    this(
      firstWidget,
      secondWidget,
      vertical,
      skin.get("default-" + (if (vertical) "vertical" else "horizontal"), classOf[SplitPane.SplitPaneStyle])
    )
  }

  private def initialize(): Unit = {
    val self = this
    addListener(
      new InputListener() {
        var draggingPointer: Int = -1

        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = scala.util.boundary {
          if (draggingPointer != -1) scala.util.boundary.break(false)
          if (pointer == 0 && button != Button(0)) scala.util.boundary.break(false)
          if (self.handleBounds.contains(x, y)) {
            draggingPointer = pointer
            self.lastPoint.set(x, y)
            self.handlePosition.set(self.handleBounds.x, self.handleBounds.y)
            scala.util.boundary.break(true)
          }
          false
        }

        override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Unit =
          if (pointer == draggingPointer) draggingPointer = -1

        override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
          if (pointer != draggingPointer) ()
          else {

            val handle = self._style.handle
            if (!self.vertical) {
              val delta      = x - self.lastPoint.x
              val availWidth = self.width - handle.minWidth
              var dragX      = self.handlePosition.x + delta
              self.handlePosition.x = dragX
              dragX = Math.max(0, dragX)
              dragX = Math.min(availWidth, dragX)
              self.splitAmount = dragX / availWidth
              self.lastPoint.set(x, y)
            } else {
              val delta       = y - self.lastPoint.y
              val availHeight = self.height - handle.minHeight
              var dragY       = self.handlePosition.y + delta
              self.handlePosition.y = dragY
              dragY = Math.max(0, dragY)
              dragY = Math.min(availHeight, dragY)
              self.splitAmount = 1 - (dragY / availHeight)
              self.lastPoint.set(x, y)
            }
            invalidate()
          }

        override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
          self.cursorOverHandle = self.handleBounds.contains(x, y)
          false
        }
      }
    )
  }

  override def setStyle(style: SplitPaneStyle): Unit = {
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the split pane's style. Modifying the returned style may not have an effect until {@link #setStyle(SplitPaneStyle)} is called.
    */
  override def getStyle: SplitPaneStyle = _style

  override def layout(): Unit = {
    clampSplitAmount()
    if (!vertical)
      calculateHorizBoundsAndPositions()
    else
      calculateVertBoundsAndPositions()

    _firstWidget.foreach { fw =>
      val b = firstWidgetBounds
      fw.setBounds(b.x, b.y, b.width, b.height)
      fw match {
        case l: Layout => l.validate()
        case _ =>
      }
    }
    _secondWidget.foreach { sw =>
      val b = secondWidgetBounds
      sw.setBounds(b.x, b.y, b.width, b.height)
      sw match {
        case l: Layout => l.validate()
        case _ =>
      }
    }
  }

  private def widgetPrefWidth(w: Nullable[Actor]): Float = w.fold(0f) {
    case l: Layout => l.getPrefWidth
    case a => a.width
  }

  private def widgetPrefHeight(w: Nullable[Actor]): Float = w.fold(0f) {
    case l: Layout => l.getPrefHeight
    case a => a.height
  }

  private def widgetMinWidth(w: Nullable[Actor]): Float = w.fold(0f) {
    case l: Layout => l.getMinWidth
    case _ => 0f
  }

  private def widgetMinHeight(w: Nullable[Actor]): Float = w.fold(0f) {
    case l: Layout => l.getMinHeight
    case _ => 0f
  }

  override def getPrefWidth: Float = {
    val first  = widgetPrefWidth(_firstWidget)
    val second = widgetPrefWidth(_secondWidget)
    if (vertical) Math.max(first, second)
    else first + _style.handle.minWidth + second
  }

  override def getPrefHeight: Float = {
    val first  = widgetPrefHeight(_firstWidget)
    val second = widgetPrefHeight(_secondWidget)
    if (!vertical) Math.max(first, second)
    else first + _style.handle.minHeight + second
  }

  override def getMinWidth: Float = {
    val first  = widgetMinWidth(_firstWidget)
    val second = widgetMinWidth(_secondWidget)
    if (vertical) Math.max(first, second)
    else first + _style.handle.minWidth + second
  }

  override def getMinHeight: Float = {
    val first  = widgetMinHeight(_firstWidget)
    val second = widgetMinHeight(_secondWidget)
    if (!vertical) Math.max(first, second)
    else first + _style.handle.minHeight + second
  }

  def setVertical(vertical: Boolean): Unit =
    if (this.vertical != vertical) {
      this.vertical = vertical
      invalidateHierarchy()
    }

  private def calculateHorizBoundsAndPositions(): Unit = {
    val handle         = _style.handle
    val height         = this.height
    val availWidth     = this.width - handle.minWidth
    val leftAreaWidth  = (availWidth * splitAmount).toInt.toFloat
    val rightAreaWidth = availWidth - leftAreaWidth
    val handleWidth    = handle.minWidth

    firstWidgetBounds.set(0, 0, leftAreaWidth, height)
    secondWidgetBounds.set(leftAreaWidth + handleWidth, 0, rightAreaWidth, height)
    handleBounds.set(leftAreaWidth, 0, handleWidth, height)
  }

  private def calculateVertBoundsAndPositions(): Unit = {
    val handle           = _style.handle
    val width            = this.width
    val height           = this.height
    val availHeight      = height - handle.minHeight
    val topAreaHeight    = (availHeight * splitAmount).toInt.toFloat
    val bottomAreaHeight = availHeight - topAreaHeight
    val handleHeight     = handle.minHeight

    firstWidgetBounds.set(0, height - topAreaHeight, width, topAreaHeight)
    secondWidgetBounds.set(0, 0, width, bottomAreaHeight)
    handleBounds.set(0, bottomAreaHeight, width, handleHeight)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit =
    if (stage.isEmpty) ()
    else {

      validate()

      val color = this.color
      val alpha = color.a * parentAlpha

      applyTransform(batch, computeTransform())
      stage.foreach { stage =>
        _firstWidget.foreach { fw =>
          if (fw.visible) {
            batch.flush()
            stage.calculateScissors(firstWidgetBounds, tempScissors)
            if (ScissorStack.pushScissors(tempScissors)) {
              fw.draw(batch, alpha)
              batch.flush()
              ScissorStack.popScissors()
            }
          }
        }
        _secondWidget.foreach { sw =>
          if (sw.visible) {
            batch.flush()
            stage.calculateScissors(secondWidgetBounds, tempScissors)
            if (ScissorStack.pushScissors(tempScissors)) {
              sw.draw(batch, alpha)
              batch.flush()
              ScissorStack.popScissors()
            }
          }
        }
      }
      batch.setColor(color.r, color.g, color.b, alpha)
      _style.handle.draw(batch, handleBounds.x, handleBounds.y, handleBounds.width, handleBounds.height)
      resetTransform(batch)
    }

  /** @param splitAmount
    *   The split amount between the min and max amount. This parameter is clamped during layout. See {@link #clampSplitAmount()}.
    */
  def setSplitAmount(splitAmount: Float): Unit = {
    this.splitAmount = splitAmount // will be clamped during layout
    invalidate()
  }

  /** Called during layout to clamp the {@link #splitAmount} within the set limits. By default it imposes the limits of the {@linkplain #getMinSplitAmount() min amount},
    * {@linkplain #getMaxSplitAmount() max amount}, and min sizes of the children. This method is internally called in response to layout, so it should not call {@link #invalidate()}.
    */
  protected def clampSplitAmount(): Unit = {
    var effectiveMinAmount = minAmount
    var effectiveMaxAmount = maxAmount

    if (vertical) {
      val availableHeight = height - _style.handle.minHeight
      _firstWidget.foreach {
        case l: Layout => effectiveMinAmount = Math.max(effectiveMinAmount, Math.min(l.getMinHeight / availableHeight, 1))
        case _ =>
      }
      _secondWidget.foreach {
        case l: Layout => effectiveMaxAmount = Math.min(effectiveMaxAmount, 1 - Math.min(l.getMinHeight / availableHeight, 1))
        case _ =>
      }
    } else {
      val availableWidth = width - _style.handle.minWidth
      _firstWidget.foreach {
        case l: Layout => effectiveMinAmount = Math.max(effectiveMinAmount, Math.min(l.getMinWidth / availableWidth, 1))
        case _ =>
      }
      _secondWidget.foreach {
        case l: Layout => effectiveMaxAmount = Math.min(effectiveMaxAmount, 1 - Math.min(l.getMinWidth / availableWidth, 1))
        case _ =>
      }
    }

    if (effectiveMinAmount > effectiveMaxAmount) // Locked handle. Average the position.
      splitAmount = 0.5f * (effectiveMinAmount + effectiveMaxAmount)
    else
      splitAmount = Math.max(Math.min(splitAmount, effectiveMaxAmount), effectiveMinAmount)
  }

  /** @throws SgeError.GraphicsError if minAmount is not between 0 and 1 */
  def setMinSplitAmount(minAmount: Float): Unit = {
    if (minAmount < 0 || minAmount > 1) throw SgeError.GraphicsError("minAmount has to be >= 0 and <= 1")
    this.minAmount = minAmount
  }

  /** @throws SgeError.GraphicsError if maxAmount is not between 0 and 1 */
  def setMaxSplitAmount(maxAmount: Float): Unit = {
    if (maxAmount < 0 || maxAmount > 1) throw SgeError.GraphicsError("maxAmount has to be >= 0 and <= 1")
    this.maxAmount = maxAmount
  }

  /** @param widget May be null. */
  def setFirstWidget(widget: Nullable[Actor]): Unit = {
    _firstWidget.foreach(fw => super.removeActor(fw))
    _firstWidget = widget
    widget.foreach(w => super.addActor(w))
    invalidate()
  }

  /** @param widget May be null. */
  def setSecondWidget(widget: Nullable[Actor]): Unit = {
    _secondWidget.foreach(sw => super.removeActor(sw))
    _secondWidget = widget
    widget.foreach(w => super.addActor(w))
    invalidate()
  }

  override def addActor(actor: Actor): Unit =
    throw new UnsupportedOperationException("Use SplitPane#setWidget.")

  override def addActorAt(index: Int, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use SplitPane#setWidget.")

  override def addActorBefore(actorBefore: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use SplitPane#setWidget.")

  override def removeActor(actor: Actor): Boolean =
    if (_firstWidget.exists(_ eq actor)) {
      setFirstWidget(Nullable.empty)
      true
    } else if (_secondWidget.exists(_ eq actor)) {
      setSecondWidget(Nullable.empty)
      true
    } else {
      true
    }

  override def removeActor(actor: Actor, unfocus: Boolean): Boolean =
    if (_firstWidget.exists(_ eq actor)) {
      super.removeActor(actor, unfocus)
      _firstWidget = Nullable.empty
      invalidate()
      true
    } else if (_secondWidget.exists(_ eq actor)) {
      super.removeActor(actor, unfocus)
      _secondWidget = Nullable.empty
      invalidate()
      true
    } else {
      false
    }

  override def removeActorAt(index: Int, unfocus: Boolean): Actor = {
    val actor = super.removeActorAt(index, unfocus)
    if (_firstWidget.exists(_ eq actor)) {
      super.removeActor(actor, unfocus)
      _firstWidget = Nullable.empty
      invalidate()
    } else if (_secondWidget.exists(_ eq actor)) {
      super.removeActor(actor, unfocus)
      _secondWidget = Nullable.empty
      invalidate()
    }
    actor
  }

  def isCursorOverHandle: Boolean = cursorOverHandle
}

object SplitPane {

  /** The style for a splitpane, see {@link SplitPane}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class SplitPaneStyle() {
    var handle: Drawable = scala.compiletime.uninitialized

    def this(handle: Drawable) = {
      this()
      this.handle = handle
    }

    def this(style: SplitPaneStyle) = {
      this()
      handle = style.handle
    }
  }
}
