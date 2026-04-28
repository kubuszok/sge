/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-source-reference: com/kotcrab/vis/ui/layout/DragPane.java
 * Covenant: full-port
 * Covenant-verified: 2026-04-11
 * Covenant-verified: 2026-04-11
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package layout

import scala.language.implicitConversions

import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, Touchable }
import sge.scenes.scene2d.ui.{ Container, HorizontalGroup, VerticalGroup, WidgetGroup }
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.visui.widget.Draggable
import sge.visui.widget.Draggable.DragListener

/** Stores actors in an internally managed [[WidgetGroup]]. Allows actors with specialized [[Draggable]] listener attached to be dropped and added into its group's content. <p> Note that unless
  * [[Draggable]] with appropriate listener (preferably [[DragPane.DefaultDragListener]]) is attached to dragged actors, this widget will act like a regular group with no extra functionalities. It's
  * usually a good idea to use [[setDraggable]] method, as it will attach the listener to all its children, making them all draggable. If you want to filter widgets accepted by this pane, use
  * [[setListener]] method.
  * @author
  *   MJ
  * @see
  *   [[setDraggable]]
  * @see
  *   [[setListener]]
  * @since 0.9.3
  */
class DragPane(group: WidgetGroup)(using Sge) extends Container[WidgetGroup]() {
  require(group != null, "Group cannot be null.") // @nowarn -- Java interop boundary
  super.setActor(Nullable(group))
  touchable = Touchable.enabled

  private var _draggable: Nullable[Draggable]                 = Nullable.empty
  private var _listener:  Nullable[DragPane.DragPaneListener] = Nullable.empty

  private def grp: WidgetGroup = actor.get

  /** Creates a new horizontal drag pane. */
  def this()(using Sge) = this(new HorizontalGroup())

  /** @param vertical if true, actors will be stored vertically, if false - horizontally. */
  def this(vertical: Boolean)(using Sge) = this(if (vertical) new VerticalGroup() else new HorizontalGroup())

  /** @return
    *   true if children are displayed vertically in a [[VerticalGroup]].
    * @see
    *   [[getVerticalGroup]]
    */
  def isVertical: Boolean = grp.isInstanceOf[VerticalGroup]

  /** @return
    *   true if children are displayed horizontally in a [[HorizontalGroup]].
    * @see
    *   [[getHorizontalGroup]]
    */
  def isHorizontal: Boolean = grp.isInstanceOf[HorizontalGroup]

  /** @return
    *   true if children are displayed as a grid in a [[GridGroup]].
    * @see
    *   [[getGridGroup]]
    */
  def isGrid: Boolean = grp.isInstanceOf[GridGroup]

  /** @return
    *   true if children are displayed with a [[VerticalFlowGroup]].
    * @see
    *   [[getVerticalFlowGroup]]
    */
  def isVerticalFlow: Boolean = grp.isInstanceOf[VerticalFlowGroup]

  /** @return
    *   true if children are displayed with a [[HorizontalFlowGroup]].
    * @see
    *   [[getHorizontalFlowGroup]]
    */
  def isHorizontalFlow: Boolean = grp.isInstanceOf[HorizontalFlowGroup]

  /** @return
    *   true if children are displayed with a [[FloatingGroup]].
    * @see
    *   [[getFloatingGroup]]
    */
  def isFloating: Boolean = grp.isInstanceOf[FloatingGroup]

  def getChildren: DynamicArray[Actor] = grp.children

  /** @return internally managed group of actors. */
  def getGroup: WidgetGroup = grp

  /** @param g will replace the internally managed group. All current children will be moved to this group. */
  def setGroup(g: WidgetGroup): Unit = setActor(Nullable(g))

  /** @param g will replace the internally managed group. All current children will be moved to this group. */
  override def setActor(g: Nullable[WidgetGroup]): Unit = {
    require(Nullable(g).isDefined, "Group cannot be null.")
    val previousGroup = grp
    super.setActor(g)
    attachListener() // Attaches draggable to all previous group children.
    val prevChildren = previousGroup.children
    var i            = 0
    while (i < prevChildren.size) {
      g.get.addActor(prevChildren(i)) // No need to attach draggable, child was already in pane.
      i += 1
    }
  }

  /** @return
    *   internally managed group of actors.
    * @throws ClassCastException
    *   if drag pane is not horizontal.
    * @see
    *   [[isHorizontal]]
    */
  def getHorizontalGroup: HorizontalGroup = grp.asInstanceOf[HorizontalGroup]

  /** @return
    *   internally managed group of actors.
    * @throws ClassCastException
    *   if drag pane is not vertical.
    * @see
    *   [[isVertical]]
    */
  def getVerticalGroup: VerticalGroup = grp.asInstanceOf[VerticalGroup]

  /** @return
    *   internally managed group of actors.
    * @throws ClassCastException
    *   if drag pane is not a grid.
    * @see
    *   [[isGrid]]
    */
  def getGridGroup: GridGroup = grp.asInstanceOf[GridGroup]

  /** @return
    *   internally managed group of actors.
    * @throws ClassCastException
    *   if drag pane is not horizontal flow.
    * @see
    *   [[isHorizontalFlow]]
    */
  def getHorizontalFlowGroup: HorizontalFlowGroup = grp.asInstanceOf[HorizontalFlowGroup]

  /** @return
    *   internally managed group of actors.
    * @throws ClassCastException
    *   if drag pane is not vertical flow.
    * @see
    *   [[isVerticalFlow]]
    */
  def getVerticalFlowGroup: VerticalFlowGroup = grp.asInstanceOf[VerticalFlowGroup]

  /** @return
    *   internally managed group of actors.
    * @throws ClassCastException
    *   if drag pane is not floating.
    * @see
    *   [[isFloating]]
    */
  def getFloatingGroup: FloatingGroup = grp.asInstanceOf[FloatingGroup]

  /** @return dragging listener automatically added to all panes' children. */
  def getDraggable: Nullable[Draggable] = _draggable

  /** @param draggable will be automatically added to all children. */
  def setDraggable(draggable: Draggable): Unit = {
    removeListener()
    _draggable = Nullable(draggable)
    attachListener()
  }

  override def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    super.setBounds(x, y, width, height)
    grp.setWidth(width)
    grp.setHeight(height)
    // Child position omitted on purpose.
  }

  override def setWidth(width: Float): Unit = {
    super.setWidth(width)
    grp.setWidth(width)
  }

  override def setHeight(height: Float): Unit = {
    super.setHeight(height)
    grp.setHeight(height)
  }

  private def removeListener(): Unit =
    _draggable.foreach { d =>
      val children = getChildren
      var i        = 0
      while (i < children.size) {
        children(i).removeListener(d)
        i += 1
      }
    }

  private def attachListener(): Unit =
    _draggable.foreach { d =>
      val children = getChildren
      var i        = 0
      while (i < children.size) {
        d.attachTo(children(i))
        i += 1
      }
    }

  /** @param a
    *   might be in the drag pane.
    * @return
    *   true if actor is added to the pane's internal group.
    */
  def contains(a: Actor): Boolean =
    a.parent.exists(_ eq grp)

  /** Removes an actor from this group. If the actor will not be used again and has actions, they should be cleared so the actions will be returned to their pool, if any. This is not done
    * automatically. <p> Note that the direct parent of [[DragPane]]'s children is the internal pane's group accessible through [[getGroup]] - and since this removal method is overridden and extended,
    * pane's children should be deleted with `dragPane.removeActor(child)` rather than `actor.remove()` method.
    * @param a
    *   will be removed, if present in the internal [[WidgetGroup]].
    * @return
    *   true if the actor was removed from this group.
    */
  override def removeActor(a: Actor): Boolean = removeActor(a, true)

  /** Removes an actor from this group. If the actor will not be used again and has actions, they should be cleared so the actions will be returned to their pool, if any. This is not done
    * automatically. <p> Note that the direct parent of [[DragPane]]'s children is the internal pane's group accessible through [[getGroup]] - and since this removal method is overridden and extended,
    * pane's children should be deleted with `dragPane.removeActor(child, true)` rather than `actor.remove()` method.
    * @param unfocus
    *   if true, unfocus is called on the stage.
    * @param a
    *   will be removed, if present in the internal [[WidgetGroup]].
    * @return
    *   true if the actor was removed from this group.
    */
  override def removeActor(a: Actor, unfocus: Boolean): Boolean =
    if (grp.children.contains(a)) {
      // Stage input focus causes problems, as touchUp is called in Draggable. Reproducing input unfocus after stage removed.
      val stageRef = a.stage
      grp.removeActor(a, false) // Stage is cleared.
      if (unfocus && stageRef.isDefined) {
        stageRef.get.unfocus(a)
      }
      true
    } else {
      false
    }

  override def clear(): Unit = grp.clear()

  override def addActor(a: Actor): Unit = {
    grp.addActor(a)
    doOnAdd(a)
  }

  override def addActorAfter(actorAfter: Actor, a: Actor): Unit = {
    grp.addActorAfter(actorAfter, a)
    doOnAdd(a)
  }

  override def addActorAt(index: Int, a: Actor): Unit = {
    grp.addActorAt(index, a)
    doOnAdd(a)
  }

  override def addActorBefore(actorBefore: Actor, a: Actor): Unit = {
    grp.addActorBefore(actorBefore, a)
    doOnAdd(a)
  }

  /** @param a was just added to the group. */
  protected def doOnAdd(a: Actor): Unit =
    _draggable.foreach(_.attachTo(a))

  override def findActor[T <: Actor](name: String): Nullable[T] = grp.findActor(name)

  override def invalidate(): Unit = {
    super.invalidate()
    grp.invalidate()
  }

  override def validate(): Unit = {
    super.validate()
    grp.validate()
  }

  /** @param listener manages children appended to the drag pane. */
  def setListener(listener: DragPane.DragPaneListener): Unit =
    _listener = Nullable(listener)

  /** @param a
    *   is dragged over the pane.
    * @return
    *   true if actor can be added to the pane.
    */
  protected def accept(a: Actor): Boolean =
    _listener.isEmpty || _listener.get.accept(this, a)
}

object DragPane {

  /** Default [[DragListener]] implementation. Implements [[DragPane]] behavior.
    * @author
    *   MJ
    * @since 0.9.3
    */
  class DefaultDragListener(private var _policy: Policy)(using Sge) extends DragListener {

    /** Contains stage drag end position, which might be changed to local widget coordinates by some methods. */
    protected val DRAG_POSITION: Vector2 = new Vector2()

    /** Creates a new drag listener with default policy. */
    def this()(using Sge) = this(DefaultPolicy.ALLOW_REMOVAL)

    /** @param policy
      *   determines behavior of dragged actors. Allows to prohibit actors from being added to a [[DragPane]]. Cannot be null.
      * @see
      *   [[DefaultPolicy]]
      */
    def setPolicy(policy: Policy): Unit = {
      require(policy != null, "Policy cannot be null.") // @nowarn -- Java interop boundary
      _policy = policy
    }

    override def onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean =
      APPROVE

    override def onDrag(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Unit = ()

    override def onEnd(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean =
      if (actor.stage.isEmpty) {
        CANCEL
      } else {
        val overActorN = actor.stage.get.hit(stageX, stageY, true)
        if (overActorN.isEmpty || (overActorN.get eq actor)) {
          CANCEL
        } else {
          val overActor = overActorN.get
          if (overActor.isAscendantOf(actor)) {
            val dragPane = getDragPane(actor)
            if (dragPane.isDefined && dragPane.get.isFloating) {
              DRAG_POSITION.set(stageX, stageY)
              addToFloatingGroup(draggable, actor, dragPane.get)
            } else {
              CANCEL
            }
          } else {
            DRAG_POSITION.set(stageX, stageY)
            overActor match {
              case dp: DragPane =>
                addDirectlyToPane(draggable, actor, dp)
              case _ =>
                val dragPane = getDragPane(overActor)
                if (accept(actor, dragPane)) {
                  addActor(draggable, actor, overActor, dragPane.get)
                } else {
                  CANCEL
                }
            }
          }
        }
      }

    /** @param draggable
      *   is attached to the actor.
      * @param actor
      *   dragged actor.
      * @param dragPane
      *   is directly under the dragged actor. If accepts the actor, it should be added to its content.
      * @return
      *   true if actor was accepted.
      */
    protected def addDirectlyToPane(draggable: Draggable, actor: Actor, dragPane: DragPane): Boolean =
      if (accept(actor, Nullable(dragPane))) {
        if (dragPane.isFloating) {
          addToFloatingGroup(draggable, actor, dragPane)
        } else {
          // Dragged directly to a pane. Assuming no padding, adding last:
          dragPane.addActor(actor)
          APPROVE
        }
      } else {
        CANCEL
      }

    /** @param actor
      *   has just been dragged.
      * @param dragPane
      *   is under the dragged actor (if exists). Can be null.
      * @return
      *   true if the actor can be added to the dragPane.
      */
    protected def accept(actor: Actor, dragPane: Nullable[DragPane]): Boolean =
      dragPane.isDefined && dragPane.get.accept(actor) && _policy.accept(dragPane.get, actor)

    /** @param draggable
      *   is attached to the actor.
      * @param actor
      *   is being dragged.
      * @param overActor
      *   is directly under the dragged actor.
      * @param dragPane
      *   contains the actor under dragged widget.
      * @return
      *   true if actor is accepted and added to the group.
      */
    protected def addActor(draggable: Draggable, actor: Actor, overActor: Actor, dragPane: DragPane): Boolean = {
      val directPaneChild = getActorInDragPane(overActor, dragPane)
      directPaneChild.foreach(_.stageToLocalCoordinates(DRAG_POSITION))
      if (dragPane.isVertical || dragPane.isVerticalFlow) {
        addToVerticalGroup(actor, dragPane, directPaneChild.get)
      } else if (dragPane.isHorizontal || dragPane.isHorizontalFlow) {
        addToHorizontalGroup(actor, dragPane, directPaneChild.get)
      } else if (dragPane.isFloating) {
        addToFloatingGroup(draggable, actor, dragPane)
      } else {
        // This is the default behavior for grid and unknown groups:
        addToOtherGroup(actor, dragPane, directPaneChild.get)
      }
    }

    /** @param actor
      *   is being dragged.
      * @param dragPane
      *   is under the actor. Stores a [[HorizontalGroup]].
      * @param directPaneChild
      *   actor under the cursor.
      * @return
      *   true if actor was accepted by the group.
      */
    protected def addToHorizontalGroup(actor: Actor, dragPane: DragPane, directPaneChild: Actor): Boolean = {
      val children            = dragPane.getChildren
      val indexOfDraggedActor = children.indexOfByRef(actor)
      actor.remove()
      if (indexOfDraggedActor >= 0) {
        val indexOfDirectChild = children.indexOfByRef(directPaneChild)
        if (indexOfDirectChild > indexOfDraggedActor) {
          dragPane.addActorAfter(directPaneChild, actor)
        } else {
          dragPane.addActorBefore(directPaneChild, actor)
        }
      } else if (DRAG_POSITION.x > directPaneChild.width / 2f) {
        dragPane.addActorAfter(directPaneChild, actor)
      } else {
        dragPane.addActorBefore(directPaneChild, actor)
      }
      APPROVE
    }

    /** @param actor
      *   is being dragged.
      * @param dragPane
      *   is under the actor. Stores a [[VerticalGroup]].
      * @param directPaneChild
      *   actor under the cursor.
      * @return
      *   true if actor was accepted by the group.
      */
    protected def addToVerticalGroup(actor: Actor, dragPane: DragPane, directPaneChild: Actor): Boolean = {
      val children            = dragPane.getChildren
      val indexOfDraggedActor = children.indexOfByRef(actor)
      actor.remove()
      if (indexOfDraggedActor >= 0) {
        val indexOfDirectChild = children.indexOfByRef(directPaneChild)
        if (indexOfDirectChild > indexOfDraggedActor) {
          dragPane.addActorAfter(directPaneChild, actor)
        } else {
          dragPane.addActorBefore(directPaneChild, actor)
        }
      } else if (DRAG_POSITION.y < directPaneChild.height / 2f) { // Y inverted.
        dragPane.addActorAfter(directPaneChild, actor)
      } else {
        dragPane.addActorBefore(directPaneChild, actor)
      }
      APPROVE
    }

    /** @param draggable
      *   attached to dragged actor.
      * @param actor
      *   is being dragged.
      * @param dragPane
      *   is under the actor. Stores a [[FloatingGroup]].
      * @return
      *   true if actor was accepted by the group.
      */
    protected def addToFloatingGroup(draggable: Draggable, actor: Actor, dragPane: DragPane): Boolean = scala.util.boundary {
      val group = dragPane.getFloatingGroup
      dragPane.stageToLocalCoordinates(DRAG_POSITION)
      var x = DRAG_POSITION.x + draggable.offsetX
      if (x < 0f || x + actor.width > dragPane.width) {
        // Normalizing value if set to keep within parent's bounds:
        if (draggable.isKeptWithinParent) {
          x = if (x < 0f) 0f else dragPane.width - actor.width - 1f
        } else {
          scala.util.boundary.break(CANCEL)
        }
      }
      var y = DRAG_POSITION.y + draggable.offsetY
      if (y < 0f || y + actor.height > dragPane.height) {
        if (draggable.isKeptWithinParent) {
          y = if (y < 0f) 0f else dragPane.height - actor.height - 1f
        } else {
          scala.util.boundary.break(CANCEL)
        }
      }
      actor.remove()
      actor.setPosition(x, y)
      group.addActor(actor)
      APPROVE
    }

    /** @param actor
      *   is being dragged.
      * @param dragPane
      *   is under the actor. Stores a [[GridGroup]] or unknown group.
      * @param directPaneChild
      *   actor under the cursor.
      * @return
      *   true if actor was accepted by the group.
      */
    protected def addToOtherGroup(actor: Actor, dragPane: DragPane, directPaneChild: Actor): Boolean = {
      val children            = dragPane.getChildren
      val indexOfDirectChild  = children.indexOfByRef(directPaneChild)
      val indexOfDraggedActor = children.indexOfByRef(actor)
      actor.remove()
      if (indexOfDraggedActor >= 0) { // Dragging own actor.
        if (indexOfDraggedActor > indexOfDirectChild) { // Dropped after current position.
          dragPane.addActorBefore(directPaneChild, actor)
        } else { // Dropped before current position.
          dragPane.addActorAfter(directPaneChild, actor)
        }
      } else if (indexOfDirectChild == children.size - 1) { // Dragged into last element.
        if (DRAG_POSITION.y < directPaneChild.height / 2f || DRAG_POSITION.x > directPaneChild.width / 2f) {
          // Adding last:
          dragPane.addActor(actor)
        } else {
          dragPane.addActorBefore(directPaneChild, actor)
        }
      } else if (indexOfDirectChild == 0) { // Dragged into first element.
        if (DRAG_POSITION.y < directPaneChild.height / 2f || DRAG_POSITION.x > directPaneChild.width / 2f) {
          dragPane.addActorAfter(directPaneChild, actor)
        } else { // Adding first:
          dragPane.addActorBefore(directPaneChild, actor)
        }
      } else { // Replacing hovered actor:
        dragPane.addActorBefore(directPaneChild, actor)
      }
      APPROVE
    }

    /** @param actor
      *   if in the drag pane, but does not have to be added directly.
      * @param dragPane
      *   contains the actor.
      * @return
      *   passed actor or the parent of the actor added directly to the pane.
      */
    protected def getActorInDragPane(actor: Actor, dragPane: DragPane): Nullable[Actor] = scala.util.boundary {
      var current: Nullable[Actor] = Nullable(actor)
      while (current.isDefined && !(current.get eq dragPane)) {
        if (dragPane.contains(current.get)) {
          scala.util.boundary.break(current)
        }
        // Actor might not be added directly to the drag pane. Trying out the parent:
        current = current.get.parent.map(_.asInstanceOf[Actor])
      }
      Nullable.empty
    }

    /** @param fromActor
      *   might be in a drag pane.
      * @return
      *   drag pane parent or null.
      */
    protected def getDragPane(fromActor: Actor): Nullable[DragPane] = scala.util.boundary {
      var current: Nullable[Actor] = Nullable(fromActor)
      while (current.isDefined)
        current.get match {
          case dp: DragPane => scala.util.boundary.break(Nullable(dp))
          case _ => current = current.get.parent.map(_.asInstanceOf[Actor])
        }
      Nullable.empty
    }
  }

  /** Determines behavior of [[DefaultDragListener]].
    * @author
    *   MJ
    * @since 0.9.3
    */
  trait Policy {

    /** @param dragPane
      *   is under the actor.
      * @param actor
      *   was dragged into the drag pane.
      * @return
      *   true if the actor can be added to the [[DragPane]]
      */
    def accept(dragPane: DragPane, actor: Actor): Boolean
  }

  /** Contains basic [[DefaultDragListener]] behaviors, allowing to modify the listener without extending it.
    * @author
    *   MJ
    * @since 0.9.3
    */
  enum DefaultPolicy extends Policy {

    /** Allows children to be moved to different [[DragPane]]s. */
    case ALLOW_REMOVAL

    /** Prohibits from removing children from the [[DragPane]], allowing them only to be moved within their own group. */
    case KEEP_CHILDREN

    override def accept(dragPane: DragPane, actor: Actor): Boolean = this match {
      case ALLOW_REMOVAL => true
      case KEEP_CHILDREN => dragPane.contains(actor) // dragPane must be the direct parent of actor.
    }
  }

  /** Allows to select children added to the group.
    * @author
    *   MJ
    * @since 0.9.3
    */
  trait DragPaneListener {

    /** Return in [[accept]] method for code clarity. */
    val ACCEPT: Boolean = true

    /** Return in [[accept]] method for code clarity. */
    val REFUSE: Boolean = false

    /** @param dragPane
      *   has this listener attached.
      * @param actor
      *   if being dragged over the [[DragPane]].
      * @return
      *   true if actor can be added to the drag pane. False if it cannot.
      */
    def accept(dragPane: DragPane, actor: Actor): Boolean
  }

  object DragPaneListener {

    /** When actors are dragged into the [[DragPane]], they are accepted and added into the pane only if their direct parent is the pane itself.
      * @author
      *   MJ
      * @since 0.9.3
      */
    class AcceptOwnChildren extends DragPaneListener {
      override def accept(dragPane: DragPane, actor: Actor): Boolean = dragPane.contains(actor)
    }

    /** Limits [[DragPane]] children amount to a certain number. Never rejects own children.
      * @author
      *   MJ
      * @since 0.9.3
      */
    class LimitChildren(max: Int) extends DragPaneListener {
      override def accept(dragPane: DragPane, actor: Actor): Boolean =
        dragPane.contains(actor) || dragPane.getChildren.size < max
    }
  }
}
