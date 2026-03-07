/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/DragAndDrop.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - ObjectMap -> scala.collection.mutable.Map (MutableMap)
 * - Array<Target> -> DynamicArray[Target]
 * - Extensive null -> Nullable conversions throughout drag/drop logic
 * - boundary/break used for loop-with-break in target search
 * - Source.actor null-check in Java constructor -> Scala val parameter (cannot be null)
 * - Payload fields: @Null Actor -> public var with uninitialized
 * - Payload.object -> Payload.obj (object is Scala keyword)
 * - Inner static classes -> companion object nested classes
 * - All public API methods faithfully ported
 * - Renames: getDragActor → def currentDragActor, getDragPayload → def currentPayload,
 *   getDragSource → def currentSource, getDragTime/setDragTime → var dragTime,
 *   isDragging → def dragging; Source/Target: getActor removed (actor is public val);
 *   Payload: getDragActor/setDragActor/getObject/setObject/etc removed (fields are public vars)
 */
package sge
package scenes
package scene2d
package utils

import scala.collection.mutable.{ Map => MutableMap }
import scala.util.boundary
import scala.util.boundary.break
import sge.math.Vector2
import sge.utils.{ DynamicArray, Nullable }

/** Manages drag and drop operations through registered drag sources and drop targets.
  * @author
  *   Nathan Sweet
  */
class DragAndDrop {
  import DragAndDrop.*

  private var dragSource:      Nullable[Source]                 = Nullable.empty
  private var payload:         Nullable[Payload]                = Nullable.empty
  private var dragActor:       Nullable[Actor]                  = Nullable.empty
  private var removeDragActor: Boolean                          = false
  private var _target:         Nullable[Target]                 = Nullable.empty
  private var isValidTarget:   Boolean                          = false
  private val targets:         DynamicArray[Target]             = DynamicArray[Target]()
  private val sourceListeners: MutableMap[Source, DragListener] = MutableMap.empty

  /** The distance a touch must travel before being considered a drag. */
  var tapSquareSize: Float = 8

  /** The button to listen for, all other buttons are ignored. Default is {@link Buttons#LEFT}. Use -1 for any button. */
  var button:                Int   = 0
  private var dragActorX:    Float = 0
  private var dragActorY:    Float = 0
  private var touchOffsetX:  Float = 0
  private var touchOffsetY:  Float = 0
  private var dragValidTime: Long  = 0
  var dragTime:              Int   = 250
  private var activePointer: Int   = -1

  /** When true (default), the {@link Stage#cancelTouchFocus()} touch focus} is cancelled if {@link Source#dragStart(InputEvent, float, float, int) dragStart} returns non-null. This ensures the
    * DragAndDrop is the only touch focus listener, eg when the source is inside a {@link ScrollPane} with flick scroll enabled.
    */
  var cancelTouchFocus: Boolean = true
  var keepWithinStage:  Boolean = true

  def addSource(source: Source): Unit = {
    val self     = this
    val listener = new DragListener {
      override def dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        if (activePointer != -1) {
          event.stop()
        } else {
          activePointer = pointer

          dragValidTime = System.currentTimeMillis() + dragTime
          dragSource = Nullable(source)
          payload = Nullable(source.dragStart(event, touchDownX, touchDownY, pointer))
          event.stop()

          if (cancelTouchFocus && payload.isDefined) {
            source.actor.stage.foreach(_.cancelTouchFocusExcept(Nullable(this), Nullable(source.actor)))
          }
        }

      override def drag(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        if (payload.isEmpty || pointer != activePointer) ()
        else {
          source.drag(event, x, y, pointer)

          event.stage.foreach { stage =>
            // Move the drag actor away, so it cannot be hit.
            val oldDragActor  = dragActor
            var oldDragActorX = 0f
            var oldDragActorY = 0f
            oldDragActor.foreach { oda =>
              oldDragActorX = oda.x
              oldDragActorY = oda.y
              oda.setPosition(Integer.MAX_VALUE.toFloat, Integer.MAX_VALUE.toFloat)
            }

            val stageX = event.stageX + touchOffsetX
            val stageY = event.stageY + touchOffsetY
            var hit: Nullable[Actor] = stage.hit(stageX, stageY, true) // Prefer touchable actors.
            if (hit.isEmpty) hit = stage.hit(stageX, stageY, false)

            oldDragActor.foreach(_.setPosition(oldDragActorX, oldDragActorY))

            // Find target.
            var newTarget: Nullable[Target] = Nullable.empty
            isValidTarget = false
            hit.foreach { hitActor =>
              boundary {
                var i = 0
                val n = targets.size
                while (i < n) {
                  val target = targets(i)
                  if (target.actor.isAscendantOf(hitActor)) {
                    newTarget = Nullable(target)
                    target.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY))
                    break()
                  }
                  i += 1
                }
              }
            }

            // If over a new target, notify the former target that it's being left behind.
            if (newTarget != _target) {
              _target.foreach { t =>
                dragSource.foreach { ds =>
                  payload.foreach { p =>
                    t.reset(ds, p)
                  }
                }
              }
              _target = newTarget
            }

            // Notify new target of drag.
            newTarget.foreach { nt =>
              dragSource.foreach { ds =>
                payload.foreach { p =>
                  isValidTarget = nt.drag(ds, p, tmpVector.x, tmpVector.y, pointer)
                }
              }
            }

            // Determine the drag actor, remove the old one if it was added by DragAndDrop, and add the new one.
            var actor: Nullable[Actor] = Nullable.empty
            _target.foreach { _ =>
              payload.foreach { p =>
                actor = if (isValidTarget) Nullable(p.validDragActor) else Nullable(p.invalidDragActor)
              }
            }
            if (actor.isEmpty) payload.foreach(p => actor = Nullable(p.dragActor))
            if (actor != oldDragActor) {
              if (oldDragActor.isDefined && removeDragActor) oldDragActor.foreach(_.remove())
              dragActor = actor
              actor.foreach { a =>
                removeDragActor = a.stage.isEmpty // Only remove later if not already in the stage now.
                if (removeDragActor) stage.addActor(a)
              }
            }
            if (actor.isEmpty) ()
            else {
              actor.foreach { a =>
                // Position the drag actor.
                var actorX = event.stageX - a.width + dragActorX
                var actorY = event.stageY + dragActorY
                if (keepWithinStage) {
                  if (actorX < 0) actorX = 0
                  if (actorY < 0) actorY = 0
                  if (actorX + a.width > stage.getWidth) actorX = stage.getWidth - a.width
                  if (actorY + a.height > stage.getHeight) actorY = stage.getHeight - a.height
                }
                a.setPosition(actorX, actorY)
              }
            }
          }
        }

      override def dragStop(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        if (pointer != activePointer) ()
        else {
          activePointer = -1
          if (payload.isEmpty) ()
          else {
            if (System.currentTimeMillis() < dragValidTime)
              isValidTarget = false
            else if (!isValidTarget && _target.isDefined) {
              val stageX = event.stageX + touchOffsetX
              val stageY = event.stageY + touchOffsetY
              _target.foreach { t =>
                t.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY))
                dragSource.foreach { ds =>
                  payload.foreach { p =>
                    isValidTarget = t.drag(ds, p, tmpVector.x, tmpVector.y, pointer)
                  }
                }
              }
            }
            if (dragActor.isDefined && removeDragActor) dragActor.foreach(_.remove())
            if (isValidTarget) {
              val stageX = event.stageX + touchOffsetX
              val stageY = event.stageY + touchOffsetY
              _target.foreach { t =>
                dragSource.foreach { ds =>
                  payload.foreach { p =>
                    t.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY))
                    t.drop(ds, p, tmpVector.x, tmpVector.y, pointer)
                  }
                }
              }
            }
            dragSource.foreach(_.dragStop(event, x, y, pointer, payload, if (isValidTarget) _target else Nullable.empty))
            _target.foreach { t =>
              dragSource.foreach { ds =>
                payload.foreach { p =>
                  t.reset(ds, p)
                }
              }
            }
            dragSource = Nullable.empty
            payload = Nullable.empty
            self._target = Nullable.empty
            isValidTarget = false
            dragActor = Nullable.empty
          }
        }
    }
    listener.tapSquareSize = tapSquareSize
    listener.button = button
    source.actor.addCaptureListener(listener)
    sourceListeners.put(source, listener)
  }

  def removeSource(source: Source): Unit =
    sourceListeners.remove(source).foreach { dragListener =>
      source.actor.removeCaptureListener(dragListener)
    }

  def addTarget(target: Target): Unit =
    targets.add(target)

  def removeTarget(target: Target): Unit =
    targets.removeValue(target)

  /** Removes all targets and sources. */
  def clear(): Unit = {
    targets.clear()
    sourceListeners.foreach { case (source, listener) =>
      source.actor.removeCaptureListener(listener)
    }
    sourceListeners.clear()
  }

  /** Cancels the touch focus for everything except the specified source. */
  def cancelTouchFocusExcept(except: Source): Unit =
    sourceListeners.get(except).foreach { listener =>
      except.actor.stage.foreach(_.cancelTouchFocusExcept(Nullable(listener), Nullable(except.actor)))
    }

  def setDragActorPosition(dragActorX: Float, dragActorY: Float): Unit = {
    this.dragActorX = dragActorX
    this.dragActorY = dragActorY
  }

  /** Sets an offset in stage coordinates from the touch position which is used to determine the drop location. Default is 0,0.
    */
  def setTouchOffset(touchOffsetX: Float, touchOffsetY: Float): Unit = {
    this.touchOffsetX = touchOffsetX
    this.touchOffsetY = touchOffsetY
  }

  def dragging: Boolean = payload.isDefined

  /** Returns the current drag actor, or null. */
  def currentDragActor: Nullable[Actor] = dragActor

  /** Returns the current drag payload, or null. */
  def currentPayload: Nullable[Payload] = payload

  /** Returns the current drag source, or null. */
  def currentSource: Nullable[Source] = dragSource

  /** Returns true if a drag is in progress and the {@link #setDragTime(int) drag time} has elapsed since the drag started. */
  def isDragValid: Boolean = payload.isDefined && System.currentTimeMillis() >= dragValidTime
}

object DragAndDrop {
  private[utils] val tmpVector: Vector2 = Vector2()

  /** A source where a payload can be dragged from.
    * @author
    *   Nathan Sweet
    */
  abstract class Source(val actor: Actor) {

    /** Called when a drag is started on the source. The coordinates are in the source's local coordinate system.
      * @return
      *   If null the drag will not affect any targets.
      */
    def dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Payload

    /** Called repeatedly during a drag which started on this source. */
    def drag(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

    /** Called when a drag for the source is stopped. The coordinates are in the source's local coordinate system.
      * @param payload
      *   null if dragStart returned null.
      * @param target
      *   null if not dropped on a valid target.
      */
    def dragStop(event: InputEvent, x: Float, y: Float, pointer: Int, payload: Nullable[Payload], target: Nullable[Target]): Unit = {}
  }

  /** A target where a payload can be dropped to.
    * @author
    *   Nathan Sweet
    */
  abstract class Target(val actor: Actor) {
    actor.stage.foreach { stage =>
      if (actor == stage.getRoot)
        throw new IllegalArgumentException("The stage root cannot be a drag and drop target.")
    }

    /** Called when the payload is dragged over the target. The coordinates are in the target's local coordinate system.
      * @return
      *   true if this is a valid target for the payload.
      */
    def drag(source: Source, payload: Payload, x: Float, y: Float, pointer: Int): Boolean

    /** Called when the payload is no longer over the target, whether because the touch was moved or a drop occurred. This is called even if {@link #drag(Source, Payload, float, float, int)} returned
      * false.
      */
    def reset(source: Source, payload: Payload): Unit = {}

    /** Called when the payload is dropped on the target. The coordinates are in the target's local coordinate system. This is not called if {@link #drag(Source, Payload, float, float, int)} returned
      * false.
      */
    def drop(source: Source, payload: Payload, x: Float, y: Float, pointer: Int): Unit
  }

  /** The payload of a drag and drop operation. Actors can be optionally provided to follow the cursor and change when over a target. Such actors will be added the stage automatically during the drag
    * operation as necessary and they will only be removed from the stage if they were added automatically. A source actor can be used as a payload drag actor.
    */
  class Payload {
    var dragActor:        Actor            = scala.compiletime.uninitialized
    var validDragActor:   Actor            = scala.compiletime.uninitialized
    var invalidDragActor: Actor            = scala.compiletime.uninitialized
    var obj:              Nullable[AnyRef] = Nullable.empty
  }
}
