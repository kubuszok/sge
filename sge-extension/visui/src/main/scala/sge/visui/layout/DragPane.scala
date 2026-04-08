/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: vis-ui/ui/src/main/java/com/kotcrab/vis/ui/layout/DragPane.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Draggable integration not implemented: the gdx Draggable class is not yet ported.
 *     Once Draggable lands, the drag-and-drop wiring needs to be added back.
 */
package sge
package visui
package layout

import scala.language.implicitConversions

import sge.scenes.scene2d.{ Actor, Touchable }
import sge.scenes.scene2d.ui.{ Container, HorizontalGroup, VerticalGroup, WidgetGroup }
import sge.utils.DynamicArray
import sge.utils.Nullable

/** Stores actors in an internally managed [[WidgetGroup]]. Allows actors to be dropped and added into its group's content. This is a simplified port that does not include Draggable support since the
  * Draggable class is not yet ported.
  * @author
  *   MJ
  * @since 0.9.3
  */
class DragPane(group: WidgetGroup)(using Sge) extends Container[WidgetGroup]() {
  require(group != null, "Group cannot be null.") // @nowarn -- Java interop boundary
  super.setActor(Nullable(group))
  touchable = Touchable.enabled

  private def grp: WidgetGroup = actor.get

  /** Creates a new horizontal drag pane. */
  def this()(using Sge) = this(new HorizontalGroup())

  /** @param vertical if true, actors will be stored vertically, if false - horizontally. */
  def this(vertical: Boolean)(using Sge) = this(if (vertical) new VerticalGroup() else new HorizontalGroup())

  /** @return true if children are displayed vertically in a [[VerticalGroup]]. */
  def isVertical: Boolean = grp.isInstanceOf[VerticalGroup]

  /** @return true if children are displayed horizontally in a [[HorizontalGroup]]. */
  def isHorizontal: Boolean = grp.isInstanceOf[HorizontalGroup]

  /** @return true if children are displayed as a grid in a [[GridGroup]]. */
  def isGrid: Boolean = grp.isInstanceOf[GridGroup]

  /** @return true if children are displayed with a [[VerticalFlowGroup]]. */
  def isVerticalFlow: Boolean = grp.isInstanceOf[VerticalFlowGroup]

  /** @return true if children are displayed with a [[HorizontalFlowGroup]]. */
  def isHorizontalFlow: Boolean = grp.isInstanceOf[HorizontalFlowGroup]

  /** @return true if children are displayed with a [[FloatingGroup]]. */
  def isFloating: Boolean = grp.isInstanceOf[FloatingGroup]

  def getChildren: DynamicArray[Actor] = grp.children

  /** @return internally managed group of actors. */
  def getGroup: WidgetGroup = grp

  /** @param g will replace the internally managed group. All current children will be moved to this group. */
  def setGroup(g: WidgetGroup): Unit = setActor(Nullable(g))

  override def setActor(g: Nullable[WidgetGroup]): Unit = {
    require(Nullable(g).isDefined, "Group cannot be null.")
    val previousGroup = grp
    super.setActor(g)
    val prevChildren = previousGroup.children
    var i            = 0
    while (i < prevChildren.size) {
      g.get.addActor(prevChildren(i))
      i += 1
    }
  }

  def getHorizontalGroup:     HorizontalGroup     = grp.asInstanceOf[HorizontalGroup]
  def getVerticalGroup:       VerticalGroup       = grp.asInstanceOf[VerticalGroup]
  def getGridGroup:           GridGroup           = grp.asInstanceOf[GridGroup]
  def getHorizontalFlowGroup: HorizontalFlowGroup = grp.asInstanceOf[HorizontalFlowGroup]
  def getVerticalFlowGroup:   VerticalFlowGroup   = grp.asInstanceOf[VerticalFlowGroup]
  def getFloatingGroup:       FloatingGroup       = grp.asInstanceOf[FloatingGroup]

  /** @param a
    *   might be in the drag pane.
    * @return
    *   true if actor is added to the pane's internal group.
    */
  def contains(a: Actor): Boolean =
    a.parent.exists(_ eq grp)

  override def removeActor(a: Actor): Boolean = removeActor(a, true)

  override def removeActor(a: Actor, unfocus: Boolean): Boolean =
    if (grp.children.contains(a)) {
      val stageRef = a.stage
      grp.removeActor(a, false)
      if (unfocus && stageRef.isDefined) {
        stageRef.get.unfocus(a)
      }
      true
    } else {
      false
    }

  override def clear(): Unit = grp.clear()

  override def addActor(a: Actor): Unit = grp.addActor(a)

  override def addActorAfter(actorAfter: Actor, a: Actor): Unit = grp.addActorAfter(actorAfter, a)

  override def addActorAt(index: Int, a: Actor): Unit = grp.addActorAt(index, a)

  override def addActorBefore(actorBefore: Actor, a: Actor): Unit = grp.addActorBefore(actorBefore, a)

  override def findActor[T <: Actor](name: String): Nullable[T] = grp.findActor(name)

  override def invalidate(): Unit = {
    super.invalidate()
    grp.invalidate()
  }

  override def validate(): Unit = {
    super.validate()
    grp.validate()
  }

  override def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    super.setBounds(x, y, width, height)
    grp.setWidth(width)
    grp.setHeight(height)
  }

  override def setWidth(width: Float): Unit = {
    super.setWidth(width)
    grp.setWidth(width)
  }

  override def setHeight(height: Float): Unit = {
    super.setHeight(height)
    grp.setHeight(height)
  }

  /** Allows to select children added to the group. */
  private var _listener: Nullable[DragPane.DragPaneListener] = Nullable.empty

  def setListener(listener: DragPane.DragPaneListener): Unit = _listener = Nullable(listener)

  /** @param a
    *   is dragged over the pane.
    * @return
    *   true if actor can be added to the pane.
    */
  protected def accept(a: Actor): Boolean =
    _listener.isEmpty || _listener.get.accept(this, a)
}

object DragPane {

  /** Allows to select children added to the group. */
  trait DragPaneListener {
    def accept(dragPane: DragPane, actor: Actor): Boolean
  }

  object DragPaneListener {

    /** When actors are dragged into the [[DragPane]], they are accepted and added into the pane only if their direct parent is the pane itself.
      */
    class AcceptOwnChildren extends DragPaneListener {
      override def accept(dragPane: DragPane, actor: Actor): Boolean = dragPane.contains(actor)
    }

    /** Limits [[DragPane]] children amount to a certain number. Never rejects own children. */
    class LimitChildren(max: Int) extends DragPaneListener {
      override def accept(dragPane: DragPane, actor: Actor): Boolean =
        dragPane.contains(actor) || dragPane.getChildren.size < max
    }
  }
}
