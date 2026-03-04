/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Tooltip.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) context
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getManager, getContainer, getActor/setActor, setInstant, setAlways
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
import sge.utils.Nullable

/** A listener that shows a tooltip actor when the mouse is over another actor.
  * @author
  *   Nathan Sweet
  */
class Tooltip[T <: Actor](contents: Nullable[T], val manager: TooltipManager)(using Sge) extends InputListener {
  import Tooltip._

  val container: Container[T] = new Container[T](contents) {
    override def act(delta: Float): Unit = {
      super.act(delta)
      targetActor.foreach { ta =>
        if (ta.getStage.isEmpty) remove()
      }
    }
  }
  container.setTouchable(Touchable.disabled)

  var instant:          Boolean         = false
  var always:           Boolean         = false
  var touchIndependent: Boolean         = false
  var targetActor:      Nullable[Actor] = Nullable.empty

  /** @param contents May be null. */
  def this(contents: Nullable[T])(using Sge) =
    this(contents, TooltipManager.getInstance())

  def getManager: TooltipManager = manager

  def getContainer: Container[T] = container

  def setActor(contents: Nullable[T]): Unit =
    container.setActor(contents)

  def getActor: Nullable[T] = container.getActor

  /** If true, this tooltip is shown without delay when hovered. */
  def setInstant(instant: Boolean): Unit =
    this.instant = instant

  /** If true, this tooltip is shown even when tooltips are not {@link TooltipManager#enabled}. */
  def setAlways(always: Boolean): Unit =
    this.always = always

  /** If true, this tooltip will be shown even when screen is touched simultaneously with entering tooltip's targetActor */
  def setTouchIndependent(touchIndependent: Boolean): Unit =
    this.touchIndependent = touchIndependent

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
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
      setContainerPosition(event.getListenerActor, x, y)
      true
    }

  private def setContainerPosition(actor: Actor, x: Float, y: Float): Unit = {
    this.targetActor = Nullable(actor)
    val stage = actor.getStage
    stage.foreach { stg =>
      container.setSize(manager.maxWidth, Int.MaxValue.toFloat)
      container.validate()
      container.getActor.foreach { a =>
        container.width(a.getWidth)
      }
      container.pack()

      val offsetX = manager.offsetX
      val offsetY = manager.offsetY
      val dist    = manager.edgeDistance
      var point   = actor.localToStageCoordinates(tmp.set(x + offsetX, y - offsetY - container.getHeight))
      if (point.y < dist) point = actor.localToStageCoordinates(tmp.set(x + offsetX, y + offsetY))
      if (point.x < dist) point.x = dist
      if (point.x + container.getWidth > stg.getWidth - dist) point.x = stg.getWidth - dist - container.getWidth
      if (point.y + container.getHeight > stg.getHeight - dist) point.y = stg.getHeight - dist - container.getHeight
      container.setPosition(point.x, point.y)

      point = actor.localToStageCoordinates(tmp.set(actor.getWidth / 2, actor.getHeight / 2))
      point.-(container.getX, container.getY)
      container.setOrigin(point.x, point.y)
    }
  }

  override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
    if (pointer != -1) ()
    else if (touchIndependent && Sge().input.isTouched()) ()
    else {
      val actor      = event.getListenerActor
      val descendant = fromActor.exists(fa => fa.isDescendantOf(actor))
      if (!descendant) {
        setContainerPosition(actor, x, y)
        manager.enter(this)
      }
    }

  override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
    val descendant = toActor.exists(ta => ta.isDescendantOf(event.getListenerActor))
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
