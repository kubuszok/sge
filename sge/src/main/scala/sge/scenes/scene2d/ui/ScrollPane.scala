/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ScrollPane.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: cancelTouchFocus() method -> cancelTouchFocusMethod() (avoids collision with cancelTouchFocus Boolean field)
 *   Convention: null -> Nullable; (using Sge) context; Interpolation.fade SAM; boundary/break
 *   Idiom: split packages
 *   Note: Java-style getters/setters retained — ~50 methods with complex state coordination; converting would be a large refactor
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShapeRenderer
import sge.math.{ Interpolation, MathUtils, Rectangle, Vector2 }
import sge.scenes.scene2d.{ Actor, Event, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.utils.{ ActorGestureListener, Cullable, Drawable, Layout }
import sge.Input.Button
import sge.utils.Nullable

/** A group that scrolls a child actor using scrollbars and/or mouse or touch dragging. <p> The actor is sized to its preferred size. If the actor's preferred width or height is less than the size of
  * this scroll pane, it is set to the size of this scroll pane. Scrollbars appear when the actor is larger than the scroll pane. <p> The scroll pane's preferred size is that of the child actor. At
  * this size, the child actor will not need to scroll, so the scroll pane is typically sized by ignoring the preferred size in one or both directions.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class ScrollPane(actor: Nullable[Actor], initialStyle: ScrollPane.ScrollPaneStyle)(using Sge) extends WidgetGroup with Styleable[ScrollPane.ScrollPaneStyle] {
  import ScrollPane._

  private var _style: ScrollPaneStyle = scala.compiletime.uninitialized
  private var _actor: Nullable[Actor] = Nullable.empty

  val actorArea:                   Rectangle            = Rectangle()
  val hScrollBounds:               Rectangle            = Rectangle()
  val hKnobBounds:                 Rectangle            = Rectangle()
  val vScrollBounds:               Rectangle            = Rectangle()
  val vKnobBounds:                 Rectangle            = Rectangle()
  private val actorCullingArea:    Rectangle            = Rectangle()
  private var flickScrollListener: ActorGestureListener = scala.compiletime.uninitialized

  var scrollX:          Boolean = false
  var scrollY:          Boolean = false
  var vScrollOnRight:   Boolean = true
  var hScrollOnBottom:  Boolean = true
  var amountX:          Float   = 0f
  var amountY:          Float   = 0f
  var visualAmountX:    Float   = 0f
  var visualAmountY:    Float   = 0f
  var maxX:             Float   = 0f
  var maxY:             Float   = 0f
  var touchScrollH:     Boolean = false
  var touchScrollV:     Boolean = false
  val lastPoint:        Vector2 = Vector2()
  var fadeScrollBars:   Boolean = true
  var smoothScrolling:  Boolean = true
  var scrollBarTouch:   Boolean = true
  var fadeAlpha:        Float   = 0f
  var fadeAlphaSeconds: Float   = 1f
  var fadeDelay:        Float   = 0f
  var fadeDelaySeconds: Float   = 1f
  var cancelTouchFocus: Boolean = true

  var flickScroll:                Boolean = true
  var flingTime:                  Float   = 1f
  var flingTimer:                 Float   = 0f
  var velocityX:                  Float   = 0f
  var velocityY:                  Float   = 0f
  private var overscrollX:        Boolean = true
  private var overscrollY:        Boolean = true
  private var overscrollDistance: Float   = 50f
  private var overscrollSpeedMin: Float   = 30f
  private var overscrollSpeedMax: Float   = 200f
  private var forceScrollX:       Boolean = false
  private var forceScrollY:       Boolean = false
  var disableX:                   Boolean = false
  var disableY:                   Boolean = false
  private var _clamp:             Boolean = true
  private var scrollbarsOnTop:    Boolean = false
  private var variableSizeKnobs:  Boolean = true
  var draggingPointer:            Int     = -1

  _style = initialStyle
  setActor(actor)
  setSize(150, 150)

  addCaptureListener()
  flickScrollListener = createFlickScrollListener()
  addListener(flickScrollListener)
  addScrollListener()

  /** @param actor May be null. */
  def this(actor: Nullable[Actor])(using Sge) = {
    this(actor, ScrollPane.ScrollPaneStyle())
  }

  /** @param actor May be null. */
  def this(actor: Nullable[Actor], skin: Skin)(using Sge) = {
    this(actor, skin.get[ScrollPane.ScrollPaneStyle])
  }

  /** @param actor May be null. */
  def this(actor: Nullable[Actor], skin: Skin, styleName: String)(using Sge) = {
    this(actor, skin.get[ScrollPane.ScrollPaneStyle](styleName))
  }

  protected def addCaptureListener(): Unit = {
    val self = this
    addCaptureListener(
      new InputListener() {
        private var handlePosition: Float = 0f

        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = scala.util.boundary {
          if (self.draggingPointer != -1) scala.util.boundary.break(false)
          if (pointer == 0 && button != Button(0)) scala.util.boundary.break(false)
          self.stage.foreach { stage =>
            stage.setScrollFocus(Nullable(self))
          }

          if (!self.flickScroll) self.setScrollbarsVisible(true)

          if (self.fadeAlpha == 0) scala.util.boundary.break(false)

          if (self.scrollBarTouch && self.scrollX && self.hScrollBounds.contains(x, y)) {
            event.stop()
            self.setScrollbarsVisible(true)
            if (self.hKnobBounds.contains(x, y)) {
              self.lastPoint.set(x, y)
              handlePosition = self.hKnobBounds.x
              self.touchScrollH = true
              self.draggingPointer = pointer
              scala.util.boundary.break(true)
            }
            self.setScrollX(self.amountX + self.actorArea.width * (if (x < self.hKnobBounds.x) -1 else 1))
            scala.util.boundary.break(true)
          }
          if (self.scrollBarTouch && self.scrollY && self.vScrollBounds.contains(x, y)) {
            event.stop()
            self.setScrollbarsVisible(true)
            if (self.vKnobBounds.contains(x, y)) {
              self.lastPoint.set(x, y)
              handlePosition = self.vKnobBounds.y
              self.touchScrollV = true
              self.draggingPointer = pointer
              scala.util.boundary.break(true)
            }
            self.setScrollY(self.amountY + self.actorArea.height * (if (y < self.vKnobBounds.y) 1 else -1))
            scala.util.boundary.break(true)
          }
          false
        }

        override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Unit =
          if (pointer != self.draggingPointer) {} else self.cancel()

        override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
          if (pointer != self.draggingPointer) {}
          else if (self.touchScrollH) {
            val delta   = x - self.lastPoint.x
            var scrollH = handlePosition + delta
            handlePosition = scrollH
            scrollH = Math.max(self.hScrollBounds.x, scrollH)
            scrollH = Math.min(self.hScrollBounds.x + self.hScrollBounds.width - self.hKnobBounds.width, scrollH)
            val total = self.hScrollBounds.width - self.hKnobBounds.width
            if (total != 0) self.setScrollPercentX((scrollH - self.hScrollBounds.x) / total)
            self.lastPoint.set(x, y)
          } else if (self.touchScrollV) {
            val delta   = y - self.lastPoint.y
            var scrollV = handlePosition + delta
            handlePosition = scrollV
            scrollV = Math.max(self.vScrollBounds.y, scrollV)
            scrollV = Math.min(self.vScrollBounds.y + self.vScrollBounds.height - self.vKnobBounds.height, scrollV)
            val total = self.vScrollBounds.height - self.vKnobBounds.height
            if (total != 0) self.setScrollPercentY(1 - (scrollV - self.vScrollBounds.y) / total)
            self.lastPoint.set(x, y)
          }

        override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
          if (!self.flickScroll) self.setScrollbarsVisible(true)
          false
        }
      }
    )
  }

  /** Called by constructor. */
  protected def createFlickScrollListener(): ActorGestureListener = {
    val self = this
    new ActorGestureListener() {
      override def pan(event: InputEvent, x: Float, y: Float, deltaX: Float, deltaY: Float): Unit = {
        self.setScrollbarsVisible(true)
        var dx = deltaX
        var dy = deltaY
        if (!self.scrollX) dx = 0
        if (!self.scrollY) dy = 0
        self.amountX -= dx
        self.amountY += dy
        self.clamp()
        if (self.cancelTouchFocus && (dx != 0 || dy != 0)) self.cancelTouchFocusMethod()
      }

      override def fling(event: InputEvent, x: Float, y: Float, button: Button): Unit = {
        val velX = if (Math.abs(x) > 150 && self.scrollX) x else 0f
        val velY = if (Math.abs(y) > 150 && self.scrollY) -y else 0f
        if (velX != 0 || velY != 0) {
          if (self.cancelTouchFocus) self.cancelTouchFocusMethod()
          self.fling(self.flingTime, velX, velY)
        }
      }

      override def handle(event: Event): Boolean =
        if (super.handle(event)) {
          event match {
            case ie: InputEvent if ie.eventType == InputEvent.Type.touchDown => self.flingTimer = 0
            case _ =>
          }
          true
        } else {
          event match {
            case ie: InputEvent if ie.isTouchFocusCancel => self.cancel()
            case _ =>
          }
          false
        }
    }
  }

  protected def addScrollListener(): Unit = {
    val self = this
    addListener(
      new InputListener() {
        override def scrolled(event: InputEvent, x: Float, y: Float, scrollAmountX: Float, scrollAmountY: Float): Boolean = scala.util.boundary {
          event.cancel()
          self.setScrollbarsVisible(true)
          if (self.scrollY || self.scrollX) {
            var amtX = scrollAmountX
            var amtY = scrollAmountY
            if (self.scrollY) {
              if (!self.scrollX && amtY == 0) amtY = amtX
            } else {
              if (self.scrollX && amtX == 0) amtX = amtY
            }
            self.setScrollY(self.amountY + self.mouseWheelY * amtY)
            self.setScrollX(self.amountX + self.mouseWheelX * amtX)
          } else {
            scala.util.boundary.break(false)
          }
          true
        }
      }
    )
  }

  /** Shows or hides the scrollbars for when using {@link #setFadeScrollBars(boolean)}. */
  def setScrollbarsVisible(visible: Boolean): Unit =
    if (visible) {
      fadeAlpha = fadeAlphaSeconds
      fadeDelay = fadeDelaySeconds
    } else {
      fadeAlpha = 0
      fadeDelay = 0
    }

  /** Cancels the stage's touch focus for all listeners except this scroll pane's flick scroll listener. This causes any actors inside the scrollpane that have received touchDown to receive touchUp.
    * @see
    *   #setCancelTouchFocus(boolean)
    */
  def cancelTouchFocusMethod(): Unit =
    stage.foreach { stage =>
      stage.cancelTouchFocusExcept(Nullable(flickScrollListener), Nullable(this))
    }

  /** If currently scrolling by tracking a touch down, stop scrolling. */
  def cancel(): Unit = {
    draggingPointer = -1
    touchScrollH = false
    touchScrollV = false
    flickScrollListener.gestureDetector.cancel()
  }

  def clamp(): Unit =
    if (!_clamp) {}
    else {
      scrollAmountX(
        if (overscrollX) MathUtils.clamp(amountX, -overscrollDistance, maxX + overscrollDistance)
        else MathUtils.clamp(amountX, 0, maxX)
      )
      scrollAmountY(
        if (overscrollY) MathUtils.clamp(amountY, -overscrollDistance, maxY + overscrollDistance)
        else MathUtils.clamp(amountY, 0, maxY)
      )
    }

  override def setStyle(style: ScrollPaneStyle): Unit = {
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the scroll pane's style. Modifying the returned style may not have an effect until {@link #setStyle(ScrollPaneStyle)} is called.
    */
  override def style: ScrollPaneStyle = _style

  override def act(delta: Float): Unit = {
    super.act(delta)

    val panning   = flickScrollListener.gestureDetector.isPanning()
    var animating = false

    if (fadeAlpha > 0 && fadeScrollBars && !panning && !touchScrollH && !touchScrollV) {
      fadeDelay -= delta
      if (fadeDelay <= 0) fadeAlpha = Math.max(0, fadeAlpha - delta)
      animating = true
    }

    if (flingTimer > 0) {
      setScrollbarsVisible(true)

      val alpha = flingTimer / flingTime
      amountX -= velocityX * alpha * delta
      amountY -= velocityY * alpha * delta
      clamp()

      // Stop fling if hit overscroll distance.
      if (amountX == -overscrollDistance) velocityX = 0
      if (amountX >= maxX + overscrollDistance) velocityX = 0
      if (amountY == -overscrollDistance) velocityY = 0
      if (amountY >= maxY + overscrollDistance) velocityY = 0

      flingTimer -= delta
      if (flingTimer <= 0) {
        velocityX = 0
        velocityY = 0
      }

      animating = true
    }

    if (
      smoothScrolling && flingTimer <= 0 && !panning &&
      // Scroll smoothly when grabbing the scrollbar if one pixel of scrollbar movement is > 10% of the scroll area.
      ((!touchScrollH || (scrollX && maxX / (hScrollBounds.width - hKnobBounds.width) > actorArea.width * 0.1f)) &&
        (!touchScrollV || (scrollY && maxY / (vScrollBounds.height - vKnobBounds.height) > actorArea.height * 0.1f)))
    ) {
      if (visualAmountX != amountX) {
        if (visualAmountX < amountX)
          visualScrollAmountX(Math.min(amountX, visualAmountX + Math.max(200 * delta, (amountX - visualAmountX) * 7 * delta)))
        else
          visualScrollAmountX(Math.max(amountX, visualAmountX - Math.max(200 * delta, (visualAmountX - amountX) * 7 * delta)))
        animating = true
      }
      if (visualAmountY != amountY) {
        if (visualAmountY < amountY)
          visualScrollAmountY(Math.min(amountY, visualAmountY + Math.max(200 * delta, (amountY - visualAmountY) * 7 * delta)))
        else
          visualScrollAmountY(Math.max(amountY, visualAmountY - Math.max(200 * delta, (visualAmountY - amountY) * 7 * delta)))
        animating = true
      }
    } else {
      if (visualAmountX != amountX) visualScrollAmountX(amountX)
      if (visualAmountY != amountY) visualScrollAmountY(amountY)
    }

    if (!panning) {
      if (overscrollX && scrollX) {
        if (amountX < 0) {
          setScrollbarsVisible(true)
          amountX += (overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountX / overscrollDistance) * delta
          if (amountX > 0) scrollAmountX(0)
          animating = true
        } else if (amountX > maxX) {
          setScrollbarsVisible(true)
          amountX -= (overscrollSpeedMin +
            (overscrollSpeedMax - overscrollSpeedMin) * -(maxX - amountX) / overscrollDistance) * delta
          if (amountX < maxX) scrollAmountX(maxX)
          animating = true
        }
      }
      if (overscrollY && scrollY) {
        if (amountY < 0) {
          setScrollbarsVisible(true)
          amountY += (overscrollSpeedMin + (overscrollSpeedMax - overscrollSpeedMin) * -amountY / overscrollDistance) * delta
          if (amountY > 0) scrollAmountY(0)
          animating = true
        } else if (amountY > maxY) {
          setScrollbarsVisible(true)
          amountY -= (overscrollSpeedMin +
            (overscrollSpeedMax - overscrollSpeedMin) * -(maxY - amountY) / overscrollDistance) * delta
          if (amountY < maxY) scrollAmountY(maxY)
          animating = true
        }
      }
    }

    if (animating) {
      stage.foreach { stage =>
        if (stage.actionsRequestRendering) Sge().graphics.requestRendering()
      }
    }
  }

  override def layout(): Unit = {
    val bg             = _style.background
    val hScrollKnob    = _style.hScrollKnob
    val vScrollKnob    = _style.vScrollKnob
    var bgLeftWidth    = 0f
    var bgRightWidth   = 0f
    var bgTopHeight    = 0f
    var bgBottomHeight = 0f
    bg.foreach { b =>
      bgLeftWidth = b.leftWidth
      bgRightWidth = b.rightWidth
      bgTopHeight = b.topHeight
      bgBottomHeight = b.bottomHeight
    }
    val width  = this.width
    val height = this.height
    actorArea.set(bgLeftWidth, bgBottomHeight, width - bgLeftWidth - bgRightWidth, height - bgTopHeight - bgBottomHeight)

    if (_actor.isEmpty) {}
    else {
      var scrollbarHeight = 0f
      var scrollbarWidth  = 0f
      hScrollKnob.foreach { hsk => scrollbarHeight = hsk.minHeight }
      _style.hScroll.foreach { hs => scrollbarHeight = Math.max(scrollbarHeight, hs.minHeight) }
      vScrollKnob.foreach { vsk => scrollbarWidth = vsk.minWidth }
      _style.vScroll.foreach { vs => scrollbarWidth = Math.max(scrollbarWidth, vs.minWidth) }

      // Get actor's desired width.
      var actorWidth:  Float = 0f
      var actorHeight: Float = 0f
      _actor.foreach {
        case layout: Layout =>
          actorWidth = layout.prefWidth
          actorHeight = layout.prefHeight
        case a =>
          actorWidth = a.width
          actorHeight = a.height
      }

      // Determine if horizontal/vertical scrollbars are needed.
      scrollX = forceScrollX || (actorWidth > actorArea.width && !disableX)
      scrollY = forceScrollY || (actorHeight > actorArea.height && !disableY)

      // Adjust actor area for scrollbar sizes and check if it causes the other scrollbar to show.
      if (!scrollbarsOnTop) {
        if (scrollY) {
          actorArea.width -= scrollbarWidth
          if (!vScrollOnRight) actorArea.x += scrollbarWidth
          // Horizontal scrollbar may cause vertical scrollbar to show.
          if (!scrollX && actorWidth > actorArea.width && !disableX) scrollX = true
        }
        if (scrollX) {
          actorArea.height -= scrollbarHeight
          if (hScrollOnBottom) actorArea.y += scrollbarHeight
          // Vertical scrollbar may cause horizontal scrollbar to show.
          if (!scrollY && actorHeight > actorArea.height && !disableY) {
            scrollY = true
            actorArea.width -= scrollbarWidth
            if (!vScrollOnRight) actorArea.x += scrollbarWidth
          }
        }
      }

      // If the actor is smaller than the available space, make it take up the available space.
      actorWidth = if (disableX) actorArea.width else Math.max(actorArea.width, actorWidth)
      actorHeight = if (disableY) actorArea.height else Math.max(actorArea.height, actorHeight)

      maxX = actorWidth - actorArea.width
      maxY = actorHeight - actorArea.height
      scrollAmountX(MathUtils.clamp(amountX, 0, maxX))
      scrollAmountY(MathUtils.clamp(amountY, 0, maxY))

      // Set the scrollbar and knob bounds.
      if (scrollX) {
        hScrollKnob.fold {
          hScrollBounds.set(0, 0, 0, 0)
          hKnobBounds.set(0, 0, 0, 0)
        } { hsk =>
          val bx = if (scrollbarsOnTop) bgLeftWidth else actorArea.x
          val by = if (hScrollOnBottom) bgBottomHeight else height - bgTopHeight - scrollbarHeight
          hScrollBounds.set(bx, by, actorArea.width, scrollbarHeight)
          if (scrollY && scrollbarsOnTop) {
            hScrollBounds.width -= scrollbarWidth
            if (!vScrollOnRight) hScrollBounds.x += scrollbarWidth
          }

          if (variableSizeKnobs)
            hKnobBounds.width = Math.max(hsk.minWidth, (hScrollBounds.width * actorArea.width / actorWidth).toInt.toFloat)
          else
            hKnobBounds.width = hsk.minWidth
          if (hKnobBounds.width > actorWidth) hKnobBounds.width = 0
          hKnobBounds.height = hsk.minHeight
          hKnobBounds.x = hScrollBounds.x + ((hScrollBounds.width - hKnobBounds.width) * scrollPercentX).toInt.toFloat
          hKnobBounds.y = hScrollBounds.y
        }
      }
      if (scrollY) {
        vScrollKnob.fold {
          vScrollBounds.set(0, 0, 0, 0)
          vKnobBounds.set(0, 0, 0, 0)
        } { vsk =>
          val bx = if (vScrollOnRight) width - bgRightWidth - scrollbarWidth else bgLeftWidth
          val by = if (scrollbarsOnTop) bgBottomHeight else actorArea.y
          vScrollBounds.set(bx, by, scrollbarWidth, actorArea.height)
          if (scrollX && scrollbarsOnTop) {
            vScrollBounds.height -= scrollbarHeight
            if (hScrollOnBottom) vScrollBounds.y += scrollbarHeight
          }

          vKnobBounds.width = vsk.minWidth
          if (variableSizeKnobs)
            vKnobBounds.height = Math.max(vsk.minHeight, (vScrollBounds.height * actorArea.height / actorHeight).toInt.toFloat)
          else
            vKnobBounds.height = vsk.minHeight
          if (vKnobBounds.height > actorHeight) vKnobBounds.height = 0
          vKnobBounds.x = if (vScrollOnRight) width - bgRightWidth - vsk.minWidth else bgLeftWidth
          vKnobBounds.y = vScrollBounds.y + ((vScrollBounds.height - vKnobBounds.height) * (1 - scrollPercentY)).toInt.toFloat
        }
      }

      updateActorPosition()
      _actor.foreach { a =>
        a match {
          case layout: Layout =>
            a.setSize(actorWidth, actorHeight)
            layout.validate()
          case _ =>
        }
      }
    }
  }

  private def updateActorPosition(): Unit =
    _actor.foreach { a =>
      // Calculate the actor's position depending on the scroll state and available actor area.
      val x = actorArea.x - (if (scrollX) visualAmountX.toInt.toFloat else 0)
      val y = actorArea.y - (if (scrollY) (maxY - visualAmountY).toInt.toFloat else maxY.toInt.toFloat)
      a.setPosition(x, y)

      a match {
        case cullable: Cullable =>
          actorCullingArea.x = actorArea.x - x
          actorCullingArea.y = actorArea.y - y
          actorCullingArea.width = actorArea.width
          actorCullingArea.height = actorArea.height
          cullable.setCullingArea(Nullable(actorCullingArea))
        case _ =>
      }
    }

  override def draw(batch: Batch, parentAlpha: Float): Unit =
    if (_actor.isEmpty) {}
    else {
      validate()

      // Setup transform for this group.
      applyTransform(batch, computeTransform())

      if (scrollX) hKnobBounds.x = hScrollBounds.x + ((hScrollBounds.width - hKnobBounds.width) * visualScrollPercentX).toInt.toFloat
      if (scrollY)
        vKnobBounds.y = vScrollBounds.y + ((vScrollBounds.height - vKnobBounds.height) * (1 - visualScrollPercentY)).toInt.toFloat

      updateActorPosition()

      // Draw the background ninepatch.
      val c     = this.color
      val alpha = c.a * parentAlpha
      _style.background.foreach { bg =>
        batch.setColor(c.r, c.g, c.b, alpha)
        bg.draw(batch, 0, 0, width, height)
      }

      batch.flush()
      if (clipBegin(actorArea.x, actorArea.y, actorArea.width, actorArea.height)) {
        drawChildren(batch, parentAlpha)
        batch.flush()
        clipEnd()
      }

      // Render scrollbars and knobs on top if they will be visible.
      batch.setColor(color.r, color.g, color.b, alpha)
      val drawAlpha = if (fadeScrollBars) alpha * Interpolation.fade(fadeAlpha / fadeAlphaSeconds) else alpha
      drawScrollBars(batch, color.r, color.g, color.b, drawAlpha)

      resetTransform(batch)
    }

  /** Renders the scrollbars after the children have been drawn. If the scrollbars faded out, a is zero and rendering can be skipped.
    */
  protected def drawScrollBars(batch: Batch, r: Float, g: Float, b: Float, a: Float): Unit =
    if (a <= 0) {}
    else {
      batch.setColor(r, g, b, a)

      val x = scrollX && hKnobBounds.width > 0
      val y = scrollY && vKnobBounds.height > 0
      if (x) {
        if (y) _style.corner.foreach { corner =>
          corner.draw(batch, hScrollBounds.x + hScrollBounds.width, hScrollBounds.y, vScrollBounds.width, vScrollBounds.y)
        }

        _style.hScroll.foreach { hs =>
          hs.draw(batch, hScrollBounds.x, hScrollBounds.y, hScrollBounds.width, hScrollBounds.height)
        }
        _style.hScrollKnob.foreach { hsk =>
          hsk.draw(batch, hKnobBounds.x, hKnobBounds.y, hKnobBounds.width, hKnobBounds.height)
        }
      }
      if (y) {
        _style.vScroll.foreach { vs =>
          vs.draw(batch, vScrollBounds.x, vScrollBounds.y, vScrollBounds.width, vScrollBounds.height)
        }
        _style.vScrollKnob.foreach { vsk =>
          vsk.draw(batch, vKnobBounds.x, vKnobBounds.y, vKnobBounds.width, vKnobBounds.height)
        }
      }
    }

  /** Generate fling gesture.
    * @param flingTime
    *   Time in seconds for which you want to fling last.
    * @param velocityX
    *   Velocity for horizontal direction.
    * @param velocityY
    *   Velocity for vertical direction.
    */
  def fling(flingTime: Float, velocityX: Float, velocityY: Float): Unit = {
    this.flingTimer = flingTime
    this.velocityX = velocityX
    this.velocityY = velocityY
  }

  override def prefWidth: Float = {
    var width = 0f
    _actor.foreach {
      case layout: Layout => width = layout.prefWidth
      case a => width = a.width
    }

    _style.background.foreach { background =>
      width = Math.max(width + background.leftWidth + background.rightWidth, background.minWidth)
    }

    if (scrollY) {
      var scrollbarWidth = 0f
      _style.vScrollKnob.foreach { vsk => scrollbarWidth = vsk.minWidth }
      _style.vScroll.foreach { vs => scrollbarWidth = Math.max(scrollbarWidth, vs.minWidth) }
      width += scrollbarWidth
    }
    width
  }

  override def prefHeight: Float = {
    var height = 0f
    _actor.foreach {
      case layout: Layout => height = layout.prefHeight
      case a => height = a.height
    }

    _style.background.foreach { background =>
      height = Math.max(height + background.topHeight + background.bottomHeight, background.minHeight)
    }

    if (scrollX) {
      var scrollbarHeight = 0f
      _style.hScrollKnob.foreach { hsk => scrollbarHeight = hsk.minHeight }
      _style.hScroll.foreach { hs => scrollbarHeight = Math.max(scrollbarHeight, hs.minHeight) }
      height += scrollbarHeight
    }
    height
  }

  override def minWidth: Float = 0

  override def minHeight: Float = 0

  /** Sets the {@link Actor} embedded in this scroll pane.
    * @param actor
    *   May be null to remove any current actor.
    */
  def setActor(actor: Nullable[Actor]): Unit = {
    _actor.foreach(a => super.removeActor(a))
    _actor = actor
    actor.foreach { a =>
      if (a eq this) throw new IllegalArgumentException("actor cannot be the ScrollPane.")
      super.addActor(a)
    }
  }

  /** Returns the actor embedded in this scroll pane, or null. */
  def getActor: Nullable[Actor] = _actor

  /** @deprecated Use {@link #setActor(Actor)}. */
  @deprecated("Use setActor", "")
  def setWidget(actor: Nullable[Actor]): Unit = setActor(actor)

  /** @deprecated Use {@link #getActor()}. */
  @deprecated("Use getActor", "")
  def widget: Nullable[Actor] = _actor

  /** @deprecated
    *   ScrollPane may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use ScrollPane#setActor", "")
  override def addActor(actor: Actor): Unit =
    throw new UnsupportedOperationException("Use ScrollPane#setActor.")

  /** @deprecated
    *   ScrollPane may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use ScrollPane#setActor", "")
  override def addActorAt(index: Int, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use ScrollPane#setActor.")

  /** @deprecated
    *   ScrollPane may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use ScrollPane#setActor", "")
  override def addActorBefore(actorBefore: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use ScrollPane#setActor.")

  /** @deprecated
    *   ScrollPane may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use ScrollPane#setActor", "")
  override def addActorAfter(actorAfter: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use ScrollPane#setActor.")

  override def removeActor(actor: Actor): Boolean =
    if (!_actor.exists(_ eq actor)) false
    else {
      setActor(Nullable.empty)
      true
    }

  override def removeActor(actor: Actor, unfocus: Boolean): Boolean =
    if (!_actor.exists(_ eq actor)) false
    else {
      _actor = Nullable.empty
      super.removeActor(actor, unfocus)
    }

  override def removeActorAt(index: Int, unfocus: Boolean): Actor = {
    val actor = super.removeActorAt(index, unfocus)
    _actor.foreach { a =>
      if (a eq actor) _actor = Nullable.empty
    }
    actor
  }

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] =
    if (x < 0 || x >= width || y < 0 || y >= height) Nullable.empty
    else if (touchable && this.touchable == Touchable.enabled && visible) {
      if (scrollX && touchScrollH && hScrollBounds.contains(x, y)) Nullable(this)
      else if (scrollY && touchScrollV && vScrollBounds.contains(x, y)) Nullable(this)
      else super.hit(x, y, touchable)
    } else {
      super.hit(x, y, touchable)
    }

  /** Called whenever the x scroll amount is changed. */
  protected def scrollAmountX(pixelsX: Float): Unit =
    this.amountX = pixelsX

  /** Called whenever the y scroll amount is changed. */
  protected def scrollAmountY(pixelsY: Float): Unit =
    this.amountY = pixelsY

  /** Called whenever the visual x scroll amount is changed. */
  protected def visualScrollAmountX(pixelsX: Float): Unit =
    this.visualAmountX = pixelsX

  /** Called whenever the visual y scroll amount is changed. */
  protected def visualScrollAmountY(pixelsY: Float): Unit =
    this.visualAmountY = pixelsY

  /** Returns the amount to scroll horizontally when the mouse wheel is scrolled. */
  protected def mouseWheelX: Float =
    Math.min(actorArea.width, Math.max(actorArea.width * 0.9f, maxX * 0.1f) / 4)

  /** Returns the amount to scroll vertically when the mouse wheel is scrolled. */
  protected def mouseWheelY: Float =
    Math.min(actorArea.height, Math.max(actorArea.height * 0.9f, maxY * 0.1f) / 4)

  def setScrollX(pixels: Float): Unit =
    scrollAmountX(MathUtils.clamp(pixels, 0, maxX))

  /** Returns the x scroll position in pixels, where 0 is the left of the scroll pane. */
  def getScrollX: Float = amountX

  def setScrollY(pixels: Float): Unit =
    scrollAmountY(MathUtils.clamp(pixels, 0, maxY))

  /** Returns the y scroll position in pixels, where 0 is the top of the scroll pane. */
  def getScrollY: Float = amountY

  /** Sets the visual scroll amount equal to the scroll amount. This can be used when setting the scroll amount without animating.
    */
  def updateVisualScroll(): Unit = {
    visualAmountX = amountX
    visualAmountY = amountY
  }

  def visualScrollX: Float = if (!scrollX) 0 else visualAmountX

  def visualScrollY: Float = if (!scrollY) 0 else visualAmountY

  def visualScrollPercentX: Float =
    if (maxX == 0) 0
    else MathUtils.clamp(visualAmountX / maxX, 0, 1)

  def visualScrollPercentY: Float =
    if (maxY == 0) 0
    else MathUtils.clamp(visualAmountY / maxY, 0, 1)

  def scrollPercentX: Float =
    if (maxX == 0) 0
    else MathUtils.clamp(amountX / maxX, 0, 1)

  def setScrollPercentX(percentX: Float): Unit =
    scrollAmountX(maxX * MathUtils.clamp(percentX, 0, 1))

  def scrollPercentY: Float =
    if (maxY == 0) 0
    else MathUtils.clamp(amountY / maxY, 0, 1)

  def setScrollPercentY(percentY: Float): Unit =
    scrollAmountY(maxY * MathUtils.clamp(percentY, 0, 1))

  def setFlickScroll(flickScroll: Boolean): Unit =
    if (this.flickScroll == flickScroll) {}
    else {
      this.flickScroll = flickScroll
      if (flickScroll)
        addListener(flickScrollListener)
      else
        removeListener(flickScrollListener)
      invalidate()
    }

  def setFlickScrollTapSquareSize(halfTapSquareSize: Float): Unit =
    flickScrollListener.gestureDetector.setTapSquareSize(halfTapSquareSize)

  /** Sets the scroll offset so the specified rectangle is fully in view, if possible. Coordinates are in the scroll pane actor's coordinate system.
    */
  def scrollTo(x: Float, y: Float, width: Float, height: Float): Unit =
    scrollTo(x, y, width, height, centerHorizontal = false, centerVertical = false)

  /** Sets the scroll offset so the specified rectangle is fully in view, and optionally centered vertically and/or horizontally, if possible. Coordinates are in the scroll pane actor's coordinate
    * system.
    */
  def scrollTo(x: Float, y: Float, width: Float, height: Float, centerHorizontal: Boolean, centerVertical: Boolean): Unit = {
    validate()

    var amtX = this.amountX
    if (centerHorizontal)
      amtX = x + (width - actorArea.width) / 2
    else
      amtX = MathUtils.clamp(amtX, x, x + width - actorArea.width)
    scrollAmountX(MathUtils.clamp(amtX, 0, maxX))

    var amtY = this.amountY
    val ny   = maxY - y
    if (centerVertical)
      amtY = ny + (actorArea.height + height) / 2
    else
      amtY = MathUtils.clamp(amtY, ny + height, ny + actorArea.height)
    scrollAmountY(MathUtils.clamp(amtY, 0, maxY))
  }

  // maxX is a public var (declared above)

  // maxY is a public var (declared above)

  def scrollBarHeight: Float =
    if (!scrollX) 0
    else {
      var height = 0f
      _style.hScrollKnob.foreach { hsk => height = hsk.minHeight }
      _style.hScroll.foreach { hs => height = Math.max(height, hs.minHeight) }
      height
    }

  def scrollBarWidth: Float =
    if (!scrollY) 0
    else {
      var width = 0f
      _style.vScrollKnob.foreach { vsk => width = vsk.minWidth }
      _style.vScroll.foreach { vs => width = Math.max(width, vs.minWidth) }
      width
    }

  /** Returns the width of the scrolled viewport. */
  def scrollWidth: Float = actorArea.width

  /** Returns the height of the scrolled viewport. */
  def scrollHeight: Float = actorArea.height

  /** Returns true if the actor is larger than the scroll pane horizontally. */
  def isScrollX: Boolean = scrollX

  /** Returns true if the actor is larger than the scroll pane vertically. */
  def isScrollY: Boolean = scrollY

  /** Disables scrolling in a direction. The actor will be sized to the FlickScrollPane in the disabled direction. */
  def setScrollingDisabled(x: Boolean, y: Boolean): Unit =
    if (x == disableX && y == disableY) {}
    else {
      disableX = x
      disableY = y
      invalidate()
    }

  def isScrollingDisabledX: Boolean = disableX

  def isScrollingDisabledY: Boolean = disableY

  def isLeftEdge: Boolean = !scrollX || amountX <= 0

  def isRightEdge: Boolean = !scrollX || amountX >= maxX

  def isTopEdge: Boolean = !scrollY || amountY <= 0

  def isBottomEdge: Boolean = !scrollY || amountY >= maxY

  def isDragging: Boolean = draggingPointer != -1

  def isPanning: Boolean = flickScrollListener.gestureDetector.isPanning()

  def isFlinging: Boolean = flingTimer > 0

  // velocityX is a public var (declared above)
  // velocityY is a public var (declared above)

  /** For flick scroll, if true the actor can be scrolled slightly past its bounds and will animate back to its bounds when scrolling is stopped. Default is true.
    */
  def setOverscroll(overscrollX: Boolean, overscrollY: Boolean): Unit = {
    this.overscrollX = overscrollX
    this.overscrollY = overscrollY
  }

  /** For flick scroll, sets the overscroll distance in pixels and the speed it returns to the actor's bounds in seconds. Default is 50, 30, 200.
    */
  def setupOverscroll(distance: Float, speedMin: Float, speedMax: Float): Unit = {
    overscrollDistance = distance
    overscrollSpeedMin = speedMin
    overscrollSpeedMax = speedMax
  }

  def getOverscrollDistance: Float = overscrollDistance

  /** Forces enabling scrollbars (for non-flick scroll) and overscrolling (for flick scroll) in a direction, even if the contents do not exceed the bounds in that direction.
    */
  def setForceScroll(x: Boolean, y: Boolean): Unit = {
    forceScrollX = x
    forceScrollY = y
  }

  def isForceScrollX: Boolean = forceScrollX

  def isForceScrollY: Boolean = forceScrollY

  /** For flick scroll, sets the amount of time in seconds that a fling will continue to scroll. Default is 1. */
  def setFlingTime(flingTime: Float): Unit =
    this.flingTime = flingTime

  /** For flick scroll, prevents scrolling out of the actor's bounds. Default is true. */
  def setClamp(clamp: Boolean): Unit =
    this._clamp = clamp

  /** Set the position of the vertical and horizontal scroll bars. */
  def setScrollBarPositions(bottom: Boolean, right: Boolean): Unit = {
    hScrollOnBottom = bottom
    vScrollOnRight = right
  }

  /** When true the scrollbars don't reduce the scrollable size and fade out after some time of not being used. */
  def setFadeScrollBars(fadeScrollBars: Boolean): Unit =
    if (this.fadeScrollBars == fadeScrollBars) {}
    else {
      this.fadeScrollBars = fadeScrollBars
      if (!fadeScrollBars) fadeAlpha = fadeAlphaSeconds
      invalidate()
    }

  def setupFadeScrollBars(fadeAlphaSeconds: Float, fadeDelaySeconds: Float): Unit = {
    this.fadeAlphaSeconds = fadeAlphaSeconds
    this.fadeDelaySeconds = fadeDelaySeconds
  }

  def getFadeScrollBars: Boolean = fadeScrollBars

  /** When false, the scroll bars don't respond to touch or mouse events. Default is true. */
  def setScrollBarTouch(scrollBarTouch: Boolean): Unit =
    this.scrollBarTouch = scrollBarTouch

  def setSmoothScrolling(smoothScrolling: Boolean): Unit =
    this.smoothScrolling = smoothScrolling

  /** When false (the default), the actor is clipped so it is not drawn under the scrollbars. When true, the actor is clipped to the entire scroll pane bounds and the scrollbars are drawn on top of
    * the actor. If {@link #setFadeScrollBars(boolean)} is true, the scroll bars are always drawn on top.
    */
  def setScrollbarsOnTop(scrollbarsOnTop: Boolean): Unit = {
    this.scrollbarsOnTop = scrollbarsOnTop
    invalidate()
  }

  def getVariableSizeKnobs: Boolean = variableSizeKnobs

  /** If true, the scroll knobs are sized based on {@link #getMaxX()} or {@link #getMaxY()}. If false, the scroll knobs are sized based on {@link Drawable#getMinWidth()} or
    * {@link Drawable#getMinHeight()}. Default is true.
    */
  def setVariableSizeKnobs(variableSizeKnobs: Boolean): Unit =
    this.variableSizeKnobs = variableSizeKnobs

  /** When true (default) and flick scrolling begins, {@link #cancelTouchFocus()} is called. This causes any actors inside the scrollpane that have received touchDown to receive touchUp when flick
    * scrolling begins.
    */
  def setCancelTouchFocus(cancelTouchFocus: Boolean): Unit =
    this.cancelTouchFocus = cancelTouchFocus

  override def drawDebug(shapes: ShapeRenderer): Unit = {
    drawDebugBounds(shapes)
    applyTransform(shapes, computeTransform())
    if (clipBegin(actorArea.x, actorArea.y, actorArea.width, actorArea.height)) {
      drawDebugChildren(shapes)
      shapes.flush()
      clipEnd()
    }
    resetTransform(shapes)
  }
}

object ScrollPane {

  /** The style for a scroll pane, see {@link ScrollPane}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class ScrollPaneStyle() {
    var background:  Nullable[Drawable] = Nullable.empty
    var corner:      Nullable[Drawable] = Nullable.empty
    var hScroll:     Nullable[Drawable] = Nullable.empty
    var hScrollKnob: Nullable[Drawable] = Nullable.empty
    var vScroll:     Nullable[Drawable] = Nullable.empty
    var vScrollKnob: Nullable[Drawable] = Nullable.empty

    def this(
      background:  Nullable[Drawable],
      hScroll:     Nullable[Drawable],
      hScrollKnob: Nullable[Drawable],
      vScroll:     Nullable[Drawable],
      vScrollKnob: Nullable[Drawable]
    ) = {
      this()
      this.background = background
      this.hScroll = hScroll
      this.hScrollKnob = hScrollKnob
      this.vScroll = vScroll
      this.vScrollKnob = vScrollKnob
    }

    def this(style: ScrollPaneStyle) = {
      this()
      background = style.background
      corner = style.corner
      hScroll = style.hScroll
      hScrollKnob = style.hScrollKnob
      vScroll = style.vScroll
      vScrollKnob = style.vScrollKnob
    }
  }
}
