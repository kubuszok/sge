/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 357
 * Covenant-baseline-methods: MultiSplitPane,MultiSplitPaneStyle,_handleBounds,actors,addActor,addActorAfter,addActorAt,addActorBefore,areaUsed,availHeight,availWidth,calculateHorizBoundsAndPositions,calculateVertBoundsAndPositions,clr,contains,currentSplit,currentX,currentY,draw,getHandleContaining,getStyle,h,handle,handleBoundsContains,handleHeight,handleOver,handleOverD,handleOverIndex,handlePosition,handleWidth,hit,i,initialize,lastPoint,layout,maxSplit,minHeight,minSplit,minWidth,mouseMoved,prefHeight,prefWidth,removeActor,result,scissors,setSplit,setStyle,setWidgets,splitAdvance,splits,style,this,touchDown,touchDragged,touchUp,w,widgetBounds
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/MultiSplitPane.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import sge.graphics.g2d.Batch
import sge.math.{ MathUtils, Rectangle, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.ui.WidgetGroup
import sge.scenes.scene2d.utils.{ Drawable, Layout, ScissorStack }
import sge.utils.{ DynamicArray, Nullable }
import sge.visui.{ FocusManager, VisUI }
import sge.visui.widget.internal.SplitPaneCursorManager

/** Similar to [[VisSplitPane]] but supports multiple widgets with multiple split bars at once. Use [[setWidgets]] after creating to set pane widgets.
  * @author
  *   Kotcrab
  * @see
  *   [[VisSplitPane]]
  * @since 1.1.4
  */
class MultiSplitPane(private val vertical: Boolean, initStyle: MultiSplitPane.MultiSplitPaneStyle)(using Sge) extends WidgetGroup {

  private var style: MultiSplitPane.MultiSplitPaneStyle = initStyle

  private val widgetBounds:  DynamicArray[Rectangle] = DynamicArray[Rectangle]()
  private val scissors:      DynamicArray[Rectangle] = DynamicArray[Rectangle]()
  private val _handleBounds: DynamicArray[Rectangle] = DynamicArray[Rectangle]()
  private val splits:        DynamicArray[Float]     = DynamicArray[Float]()

  private val handlePosition: Vector2 = new Vector2()
  private val lastPoint:      Vector2 = new Vector2()

  private var handleOver:      Nullable[Rectangle] = Nullable.empty
  private var handleOverIndex: Int                 = 0

  setSize(prefWidth, prefHeight)
  initialize()

  def this(vertical: Boolean, styleName: String)(using Sge) =
    this(vertical, VisUI.getSkin.get[MultiSplitPane.MultiSplitPaneStyle](styleName))

  def this(vertical: Boolean)(using Sge) =
    this(vertical, "default-" + (if (vertical) "vertical" else "horizontal"))

  private def initialize(): Unit = {
    addListener(
      new SplitPaneCursorManager(this, vertical) {
        override protected def handleBoundsContains(x: Float, y: Float): Boolean =
          getHandleContaining(x, y).isDefined

        override protected def contains(x: Float, y: Float): Boolean = {
          var i     = 0
          var found = false
          while (i < widgetBounds.size && !found) {
            if (widgetBounds(i).contains(x, y)) found = true
            i += 1
          }
          found || getHandleContaining(x, y).isDefined
        }
      }
    )

    addListener(
      new InputListener() {
        private var draggingPointer: Int = -1

        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean =
          if (!isTouchable) false
          else if (draggingPointer != -1) false
          else if (pointer == 0 && button != sge.Input.Buttons.LEFT) false
          else {
            val containingHandle = getHandleContaining(x, y)
            if (containingHandle.isDefined) {
              handleOverIndex = _handleBounds.indexOf(containingHandle.get)
              FocusManager.resetFocus(stage)
              draggingPointer = pointer
              lastPoint.set(x, y)
              handlePosition.set(containingHandle.get.x, containingHandle.get.y)
              true
            } else {
              false
            }
          }

        override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit = {
          if (pointer == draggingPointer) draggingPointer = -1
          handleOver = getHandleContaining(x, y)
        }

        override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
          handleOver = getHandleContaining(x, y)
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
              val targetSplit = dragX / availWidth
              setSplit(handleOverIndex, targetSplit)
              lastPoint.set(x, y)
            } else {
              val delta       = y - lastPoint.y
              val availHeight = height - handle.minHeight
              val dragY       = Math.max(0, Math.min(availHeight, handlePosition.y + delta))
              handlePosition.y = handlePosition.y + delta
              val targetSplit = 1 - (dragY / availHeight)
              setSplit(handleOverIndex, targetSplit)
              lastPoint.set(x, y)
            }
            invalidate()
          }
      }
    )
  }

  private def getHandleContaining(x: Float, y: Float): Nullable[Rectangle] = {
    var i = 0
    var result: Nullable[Rectangle] = Nullable.empty
    while (i < _handleBounds.size && result.isEmpty) {
      val rect = _handleBounds(i)
      if (rect.contains(x, y)) result = Nullable(rect)
      i += 1
    }
    result
  }

  def getStyle: MultiSplitPane.MultiSplitPaneStyle = style

  def setStyle(style: MultiSplitPane.MultiSplitPaneStyle): Unit = {
    this.style = style
    invalidateHierarchy()
  }

  override def layout(): Unit = {
    if (!vertical) calculateHorizBoundsAndPositions()
    else calculateVertBoundsAndPositions()

    val actors = children
    var i      = 0
    while (i < actors.size) {
      val actor  = actors(i)
      val bounds = widgetBounds(i)
      actor.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
      actor match {
        case l: Layout => l.validate()
        case _ => ()
      }
      i += 1
    }
  }

  override def prefWidth: Float = {
    var w      = 0f
    val actors = children
    var i      = 0
    while (i < actors.size) {
      w = actors(i) match {
        case l: Layout => l.prefWidth
        case a => a.width
      }
      i += 1
    }
    if (!vertical) w += _handleBounds.size * style.handle.minWidth
    w
  }

  override def prefHeight: Float = {
    var h      = 0f
    val actors = children
    var i      = 0
    while (i < actors.size) {
      h = actors(i) match {
        case l: Layout => l.prefHeight
        case a => a.height
      }
      i += 1
    }
    if (vertical) h += _handleBounds.size * style.handle.minHeight
    h
  }

  override def minWidth:  Float = 0
  override def minHeight: Float = 0

  private def calculateHorizBoundsAndPositions(): Unit = {
    val h           = height
    val w           = width
    val handleWidth = style.handle.minWidth
    val availWidth  = w - (_handleBounds.size * handleWidth)
    var areaUsed    = 0f
    var currentX    = 0f

    var i = 0
    while (i < splits.size) {
      val areaWidthFromLeft = (availWidth * splits(i)).toInt.toFloat
      val areaWidth         = areaWidthFromLeft - areaUsed
      areaUsed += areaWidth
      widgetBounds(i).set(currentX, 0, areaWidth, h)
      currentX += areaWidth
      _handleBounds(i).set(currentX, 0, handleWidth, h)
      currentX += handleWidth
      i += 1
    }
    if (widgetBounds.size != 0) widgetBounds.peek.set(currentX, 0, availWidth - areaUsed, h)
  }

  private def calculateVertBoundsAndPositions(): Unit = {
    val w            = width
    val h            = height
    val handleHeight = style.handle.minHeight
    val availHeight  = h - (_handleBounds.size * handleHeight)
    var areaUsed     = 0f
    var currentY     = h

    var i = 0
    while (i < splits.size) {
      val areaHeightFromTop = (availHeight * splits(i)).toInt.toFloat
      val areaHeight        = areaHeightFromTop - areaUsed
      areaUsed += areaHeight
      widgetBounds(i).set(0, currentY - areaHeight, w, areaHeight)
      currentY -= areaHeight
      _handleBounds(i).set(0, currentY - handleHeight, w, handleHeight)
      currentY -= handleHeight
      i += 1
    }
    if (widgetBounds.size != 0) widgetBounds.peek.set(0, 0, w, availHeight - areaUsed)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    val clr = this.color
    applyTransform(batch, computeTransform())

    val actors = children
    var i      = 0
    while (i < actors.size) {
      val actor   = actors(i)
      val bounds  = widgetBounds(i)
      val scissor = scissors(i)
      stage.foreach { stg =>
        stg.calculateScissors(bounds, scissor)
        if (ScissorStack.pushScissors(scissor)) {
          if (actor.visible) actor.draw(batch, parentAlpha * clr.a)
          batch.flush()
          ScissorStack.popScissors()
        }
      }
      i += 1
    }

    batch.setColor(clr.r, clr.g, clr.b, parentAlpha * clr.a)
    val handle      = style.handle
    val handleOverD = if (isTouchable && style.handleOver.isDefined) style.handleOver.get else style.handle
    i = 0
    while (i < _handleBounds.size) {
      val rect = _handleBounds(i)
      if (handleOver.isDefined && handleOver.get == rect) {
        handleOverD.draw(batch, rect.x, rect.y, rect.width, rect.height)
      } else {
        handle.draw(batch, rect.x, rect.y, rect.width, rect.height)
      }
      i += 1
    }
    resetTransform(batch)
  }

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] =
    if (touchable && this.touchable == Touchable.disabled) Nullable.empty
    else if (getHandleContaining(x, y).isDefined) Nullable(this)
    else super.hit(x, y, touchable)

  /** Changes widgets of this split pane. You can pass any number of actors even 1 or 0. */
  def setWidgets(actors: Actor*): Unit = setWidgets(actors)

  /** Changes widgets of this split pane. You can pass any number of actors even 1 or 0. */
  def setWidgets(actors: Iterable[Actor]): Unit = {
    clearChildren()
    widgetBounds.clear()
    scissors.clear()
    _handleBounds.clear()
    splits.clear()

    for (actor <- actors) {
      super.addActor(actor)
      widgetBounds.add(new Rectangle())
      scissors.add(new Rectangle())
    }
    var currentSplit = 0f
    val splitAdvance = 1f / children.size
    var i            = 0
    while (i < children.size - 1) {
      _handleBounds.add(new Rectangle())
      currentSplit += splitAdvance
      splits.add(currentSplit)
      i += 1
    }
    invalidate()
  }

  /** @param handleBarIndex
    *   index of handle bar starting from zero, max index is number of widgets - 1
    * @param split
    *   new value of split, must be greater than 0 and lesser than 1 and must be smaller and bigger than previous and next split value. Invalid values will be clamped to closest valid one.
    */
  def setSplit(handleBarIndex: Int, split: Float): Unit = {
    require(handleBarIndex >= 0, "handleBarIndex can't be < 0")
    require(handleBarIndex < splits.size, "handleBarIndex can't be >= splits size")
    val minSplit = if (handleBarIndex == 0) 0f else splits(handleBarIndex - 1)
    val maxSplit = if (handleBarIndex == splits.size - 1) 1f else splits(handleBarIndex + 1)
    splits(handleBarIndex) = MathUtils.clamp(split, minSplit, maxSplit)
  }

  override def addActorAfter(actorAfter: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use MultiSplitPane#setWidgets")

  override def addActor(actor: Actor): Unit =
    throw new UnsupportedOperationException("Use MultiSplitPane#setWidgets")

  override def addActorAt(index: Int, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use MultiSplitPane#setWidgets")

  override def addActorBefore(actorBefore: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use MultiSplitPane#setWidgets")

  override def removeActor(actor: Actor): Boolean =
    throw new UnsupportedOperationException("Use MultiSplitPane#setWidgets")
}

object MultiSplitPane {

  class MultiSplitPaneStyle extends VisSplitPane.VisSplitPaneStyle {
    def this(style: VisSplitPane.VisSplitPaneStyle) = {
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
