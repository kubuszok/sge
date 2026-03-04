/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ActorGestureListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - implicit Sge parameter added (replaces Gdx global)
 * - Fields event/actor/touchDownTarget: Java package-private -> Scala private + Nullable
 * - Null-safe foreach/fold used for nullable actor/event callbacks
 * - stageToLocalAmount takes explicit actor param (Java accesses outer mutable field directly)
 * - amount.-(v) used instead of amount.sub(v) (Scala operator convention)
 * - getStage returns Nullable -> uses foreach for null-safety
 * TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 */
package sge
package scenes
package scene2d
package utils

import sge.input.GestureDetector
import sge.math.Vector2
import sge.utils.Nullable

/** Detects tap, long press, fling, pan, zoom, and pinch gestures on an actor. If there is only a need to detect tap, use {@link ClickListener}.
  * @see
  *   GestureDetector
  * @author
  *   Nathan Sweet
  */
class ActorGestureListener(halfTapSquareSize: Float, tapCountInterval: Float, longPressDuration: Float, maxFlingDelay: Float)(implicit sge: Sge) extends EventListener {
  import ActorGestureListener.*

  private var event:            Nullable[InputEvent] = Nullable.empty
  private var _actor:           Nullable[Actor]      = Nullable.empty
  private var _touchDownTarget: Nullable[Actor]      = Nullable.empty

  private val detector: GestureDetector = GestureDetector(
    halfTapSquareSize,
    tapCountInterval,
    longPressDuration,
    maxFlingDelay,
    new GestureDetector.GestureAdapter {
      private val initialPointer1 = Vector2()
      private val initialPointer2 = Vector2()
      private val pointer1        = Vector2()
      private val pointer2        = Vector2()

      override def tap(stageX: Float, stageY: Float, count: Int, button: Int): Boolean = {
        _actor.foreach { a =>
          a.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
          event.foreach(e => ActorGestureListener.this.tap(e, tmpCoords.x, tmpCoords.y, count, button))
        }
        true
      }

      override def longPress(stageX: Float, stageY: Float): Boolean =
        _actor.exists { a =>
          a.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
          ActorGestureListener.this.longPress(a, tmpCoords.x, tmpCoords.y)
        }

      override def fling(velocityX: Float, velocityY: Float, button: Int): Boolean = {
        _actor.foreach { a =>
          stageToLocalAmount(a, tmpCoords.set(velocityX, velocityY))
          event.foreach(e => ActorGestureListener.this.fling(e, tmpCoords.x, tmpCoords.y, button))
        }
        true
      }

      override def pan(stageX: Float, stageY: Float, deltaX: Float, deltaY: Float): Boolean = {
        _actor.foreach { a =>
          stageToLocalAmount(a, tmpCoords.set(deltaX, deltaY))
          val dx = tmpCoords.x
          val dy = tmpCoords.y
          a.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
          event.foreach(e => ActorGestureListener.this.pan(e, tmpCoords.x, tmpCoords.y, dx, dy))
        }
        true
      }

      override def panStop(stageX: Float, stageY: Float, pointer: Int, button: Int): Boolean = {
        _actor.foreach { a =>
          a.stageToLocalCoordinates(tmpCoords.set(stageX, stageY))
          event.foreach(e => ActorGestureListener.this.panStop(e, tmpCoords.x, tmpCoords.y, pointer, button))
        }
        true
      }

      override def zoom(initialDistance: Float, distance: Float): Boolean = {
        event.foreach(e => ActorGestureListener.this.zoom(e, initialDistance, distance))
        true
      }

      override def pinch(stageInitialPointer1: Vector2, stageInitialPointer2: Vector2, stagePointer1: Vector2, stagePointer2: Vector2): Boolean = {
        _actor.foreach { a =>
          a.stageToLocalCoordinates(initialPointer1.set(stageInitialPointer1))
          a.stageToLocalCoordinates(initialPointer2.set(stageInitialPointer2))
          a.stageToLocalCoordinates(pointer1.set(stagePointer1))
          a.stageToLocalCoordinates(pointer2.set(stagePointer2))
          event.foreach(e => ActorGestureListener.this.pinch(e, initialPointer1, initialPointer2, pointer1, pointer2))
        }
        true
      }

      private def stageToLocalAmount(actor: Actor, amount: Vector2): Unit = {
        actor.stageToLocalCoordinates(amount)
        amount.-(actor.stageToLocalCoordinates(tmpCoords2.set(0, 0)))
      }
    }
  )

  /** @see GestureDetector#GestureDetector(com.badlogic.gdx.input.GestureDetector.GestureListener) */
  def this()(implicit sge: Sge) = this(20, 0.4f, 1.1f, Integer.MAX_VALUE.toFloat)

  override def handle(e: Event): Boolean =
    e match {
      case event: InputEvent =>
        event.getType match {
          case InputEvent.Type.touchDown =>
            _actor = Nullable(event.getListenerActor)
            _touchDownTarget = Nullable(event.getTarget)
            detector.touchDown(event.getStageX, event.getStageY, event.getPointer, event.getButton)
            _actor.foreach { a =>
              a.stageToLocalCoordinates(tmpCoords.set(event.getStageX, event.getStageY))
              touchDown(event, tmpCoords.x, tmpCoords.y, event.getPointer, event.getButton)
            }
            if (event.getTouchFocus) event.getStage.foreach(_.addTouchFocus(this, event.getListenerActor, event.getTarget, event.getPointer, event.getButton))
            true
          case InputEvent.Type.touchUp =>
            val touchFocusCancel = event.isTouchFocusCancel
            if (touchFocusCancel)
              detector.reset()
            else {
              this.event = Nullable(event)
              _actor = Nullable(event.getListenerActor)
              detector.touchUp(event.getStageX, event.getStageY, event.getPointer, event.getButton)
              _actor.foreach { a =>
                a.stageToLocalCoordinates(tmpCoords.set(event.getStageX, event.getStageY))
                touchUp(event, tmpCoords.x, tmpCoords.y, event.getPointer, event.getButton)
              }
            }
            this.event = Nullable.empty
            _actor = Nullable.empty
            _touchDownTarget = Nullable.empty
            !touchFocusCancel
          case InputEvent.Type.touchDragged =>
            this.event = Nullable(event)
            _actor = Nullable(event.getListenerActor)
            detector.touchDragged(event.getStageX, event.getStageY, event.getPointer)
            true
          case _ => false
        }
      case _ => false
    }

  def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit = {}

  def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit = {}

  def tap(event: InputEvent, x: Float, y: Float, count: Int, button: Int): Unit = {}

  /** If true is returned, additional gestures will not be triggered. No event is provided because this event is triggered by time passing, not by an InputEvent.
    */
  def longPress(actor: Actor, x: Float, y: Float): Boolean = false

  def fling(event: InputEvent, velocityX: Float, velocityY: Float, button: Int): Unit = {}

  /** The delta is the difference in stage coordinates since the last pan. */
  def pan(event: InputEvent, x: Float, y: Float, deltaX: Float, deltaY: Float): Unit = {}

  def panStop(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit = {}

  def zoom(event: InputEvent, initialDistance: Float, distance: Float): Unit = {}

  def pinch(event: InputEvent, initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Unit = {}

  def getGestureDetector: GestureDetector = detector

  def getTouchDownTarget: Nullable[Actor] = _touchDownTarget
}

object ActorGestureListener {
  private[utils] val tmpCoords:  Vector2 = Vector2()
  private[utils] val tmpCoords2: Vector2 = Vector2()
}
