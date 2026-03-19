/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Tooltip.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) context
 *   Idiom: split packages
 *   Fixes: Java-style getters/setters removed (manager/container already public vals, instant/always/touchIndependent already public vars)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.Input.Button
import sge.utils.{ Nullable, Seconds }

/** A listener that shows a tooltip actor when the mouse is over another actor.
  * @author
  *   Nathan Sweet
  */
class Tooltip[T <: Actor](contents: Nullable[T], val manager: TooltipManager)(using Sge) extends InputListener {
  import Tooltip._

  val container: Container[T] = new Container[T](contents) {
    override def act(delta: Seconds): Unit = {
      super.act(delta)
      targetActor.foreach { ta =>
        if (ta.stage.isEmpty) remove()
      }
    }
  }
  container.touchable = Touchable.disabled

  var instant:          Boolean         = false
  var always:           Boolean         = false
  var touchIndependent: Boolean         = false
  var targetActor:      Nullable[Actor] = Nullable.empty

  /** @param contents May be null. */
  def this(contents: Nullable[T])(using Sge) = {
    this(contents, TooltipManager.instance)
  }

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean =
    if (instant) {
      container.toFront()
      false
    } else {
      manager.touchDown(this)
      false
    }

  override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean =
    if (container.hasParent) false
    else {
      event.listenerActor.foreach(a => setContainerPosition(a, x, y))
      true
    }

  private def setContainerPosition(actor: Actor, x: Float, y: Float): Unit = {
    this.targetActor = Nullable(actor)
    val stage = actor.stage
    stage.foreach { stg =>
      container.setSize(manager.maxWidth, Int.MaxValue.toFloat)
      container.validate()
      container.actor.foreach { a =>
        container.width(a.width)
      }
      container.pack()

      val offsetX = manager.offsetX
      val offsetY = manager.offsetY
      val dist    = manager.edgeDistance
      var point   = actor.localToStageCoordinates(tmp.set(x + offsetX, y - offsetY - container.height))
      if (point.y < dist) point = actor.localToStageCoordinates(tmp.set(x + offsetX, y + offsetY))
      if (point.x < dist) point.x = dist
      if (point.x + container.width > stg.width - dist) point.x = stg.width - dist - container.width
      if (point.y + container.height > stg.height - dist) point.y = stg.height - dist - container.height
      container.setPosition(point.x, point.y)

      point = actor.localToStageCoordinates(tmp.set(actor.width / 2, actor.height / 2))
      point.-(container.x, container.y)
      container.setOrigin(point.x, point.y)
    }
  }

  override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
    if (pointer != -1) ()
    else if (touchIndependent && Sge().input.touched) ()
    else {
      event.listenerActor.foreach { actor =>
        val descendant = fromActor.exists(fa => fa.isDescendantOf(actor))
        if (!descendant) {
          setContainerPosition(actor, x, y)
          manager.enter(this)
        }
      }
    }

  override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
    val descendant = event.listenerActor.exists(la => toActor.exists(ta => ta.isDescendantOf(la)))
    if (!descendant) {
      hide()
    }
  }

  def hide(): Unit =
    manager.hide(this)
}

object Tooltip {
  private val tmp: Vector2 = Vector2()
}
