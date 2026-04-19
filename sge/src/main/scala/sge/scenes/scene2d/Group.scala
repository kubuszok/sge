/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Group.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: SnapshotArray -> DynamicArray (with toArray snapshots); implements -> extends/with
 *   Convention: null -> Nullable[A]; no return (boundary/break); split packages; (using Sge) on act()
 *   Idiom: Java for-loop -> while; continue -> if-guard; parent null-loop -> Nullable iteration;
 *     indexOf(actor, true) -> indexOf(actor); children.swap -> manual swap; static tmp -> companion object
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 532
 * Covenant-baseline-methods: Group,_cullingArea,act,actor,addActor,addActorAfter,addActorAt,addActorBefore,alpha,applyTransform,buffer,children,childrenChanged,clear,clearChildren,computeTransform,computedTransform,cullingArea,debugAll,draw,drawChildren,drawDebug,drawDebugChildren,findActor,firstIndex,getChild,hasChildren,hit,i,index,localToDescendantCoordinates,maxIndex,n,oldTransform,originX,originY,p,parentGroup,removeActor,removeActorAt,resetTransform,secondIndex,setCullingArea,setDebug,setStage,snapshot,swapActor,toString,transform,worldTransform
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/Group.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d

import sge.utils.{ DynamicArray, Nullable, Seconds }

import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShapeRenderer
import sge.math.{ Affine2, Matrix4, Rectangle, Vector2 }
import sge.scenes.scene2d.utils.Cullable

/** 2D scene graph node that may contain other actors. <p> Actors have a z-order equal to the order they were inserted into the group. Actors inserted later will be drawn on top of actors added
  * earlier. Touch events that hit more than one actor are distributed to topmost actors first.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class Group()(using Sge) extends Actor() with Cullable {

  val children:                  DynamicArray[Actor] = DynamicArray[Actor]()
  private val worldTransform:    Affine2             = Affine2()
  private val computedTransform: Matrix4             = Matrix4()
  private val oldTransform:      Matrix4             = Matrix4()
  var transform:                 Boolean             = true
  private var _cullingArea:      Nullable[Rectangle] = Nullable.empty

  override def act(delta: Seconds): Unit = {
    super.act(delta)
    val snapshot = children.toArray
    var i        = 0
    while (i < snapshot.length) {
      snapshot(i).act(delta)
      i += 1
    }
  }

  /** Draws the group and its children. The default implementation calls {@link #applyTransform(Batch, Matrix4)} if needed, then {@link #drawChildren(Batch, float)}, then
    * {@link #resetTransform(Batch)} if needed.
    */
  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    if (transform) applyTransform(batch, computeTransform())
    drawChildren(batch, parentAlpha)
    if (transform) resetTransform(batch)
  }

  /** Draws all children. {@link #applyTransform(Batch, Matrix4)} should be called before and {@link #resetTransform(Batch)} after this method if {@link #setTransform(boolean) transform} is true. If
    * {@link #setTransform(boolean) transform} is false these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This method avoids drawing
    * children completely outside the {@link #setCullingArea(Rectangle) culling area}, if set.
    */
  protected def drawChildren(batch: Batch, parentAlpha: Float): Unit = {
    val alpha    = parentAlpha * this.color.a
    val snapshot = children.toArray
    val n        = snapshot.length
    _cullingArea.fold {
      // No culling, draw all children.
      if (transform) {
        var i = 0
        while (i < n) {
          val child = snapshot(i)
          if (child.visible) child.draw(batch, alpha)
          i += 1
        }
      } else {
        // No transform for this group, offset each child.
        val offsetX = x
        val offsetY = y
        x = 0
        y = 0
        var i = 0
        while (i < n) {
          val child = snapshot(i)
          if (child.visible) {
            val cx = child.x
            val cy = child.y
            child.x = cx + offsetX
            child.y = cy + offsetY
            child.draw(batch, alpha)
            child.x = cx
            child.y = cy
          }
          i += 1
        }
        x = offsetX
        y = offsetY
      }
    } { ca =>
      // Draw children only if inside culling area.
      val cullLeft   = ca.x
      val cullRight  = cullLeft + ca.width
      val cullBottom = ca.y
      val cullTop    = cullBottom + ca.height
      if (transform) {
        var i = 0
        while (i < n) {
          val child = snapshot(i)
          if (child.visible) {
            val cx = child.x
            val cy = child.y
            if (cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom)
              child.draw(batch, alpha)
          }
          i += 1
        }
      } else {
        // No transform for this group, offset each child.
        val offsetX = x
        val offsetY = y
        x = 0
        y = 0
        var i = 0
        while (i < n) {
          val child = snapshot(i)
          if (child.visible) {
            val cx = child.x
            val cy = child.y
            if (cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom) {
              child.x = cx + offsetX
              child.y = cy + offsetY
              child.draw(batch, alpha)
              child.x = cx
              child.y = cy
            }
          }
          i += 1
        }
        x = offsetX
        y = offsetY
      }
    }
  }

  /** Draws this actor's debug lines if {@link #getDebug()} is true and, regardless of {@link #getDebug()}, calls {@link Actor#drawDebug(ShapeRenderer)} on each child.
    */
  override def drawDebug(shapes: ShapeRenderer): Unit = {
    drawDebugBounds(shapes)
    if (transform) applyTransform(shapes, computeTransform())
    drawDebugChildren(shapes)
    if (transform) resetTransform(shapes)
  }

  /** Draws all children. {@link #applyTransform(Batch, Matrix4)} should be called before and {@link #resetTransform(Batch)} after this method if {@link #setTransform(boolean) transform} is true. If
    * {@link #setTransform(boolean) transform} is false these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This method avoids drawing
    * children completely outside the {@link #setCullingArea(Rectangle) culling area}, if set.
    */
  protected def drawDebugChildren(shapes: ShapeRenderer): Unit = {
    val snapshot = children.toArray
    val n        = snapshot.length
    // No culling, draw all children.
    if (transform) {
      var i = 0
      while (i < n) {
        val child = snapshot(i)
        if (child.visible && (child.isDebug || child.isInstanceOf[Group]))
          child.drawDebug(shapes)
        i += 1
      }
      shapes.flush()
    } else {
      // No transform for this group, offset each child.
      val offsetX = x
      val offsetY = y
      x = 0
      y = 0
      var i = 0
      while (i < n) {
        val child = snapshot(i)
        if (child.visible && (child.isDebug || child.isInstanceOf[Group])) {
          val cx = child.x
          val cy = child.y
          child.x = cx + offsetX
          child.y = cy + offsetY
          child.drawDebug(shapes)
          child.x = cx
          child.y = cy
        }
        i += 1
      }
      x = offsetX
      y = offsetY
    }
  }

  /** Returns the transform for this group's coordinate system. */
  protected def computeTransform(): Matrix4 = {
    val worldTransform = this.worldTransform
    val originX        = this.originX
    val originY        = this.originY
    worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY)
    if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY)

    // Find the first parent that transforms.
    var parentGroup = parent
    while (parentGroup.isDefined)
      parentGroup.foreach { pg =>
        if (pg.transform) {
          worldTransform.preMul(pg.worldTransform)
          parentGroup = Nullable.empty // break
        } else {
          parentGroup = pg.parent
        }
      }

    computedTransform.set(worldTransform)
    computedTransform
  }

  /** Set the batch's transformation matrix, often with the result of {@link #computeTransform()}. Note this causes the batch to be flushed. {@link #resetTransform(Batch)} will restore the transform
    * to what it was before this call.
    */
  protected def applyTransform(batch: Batch, transform: Matrix4): Unit = {
    oldTransform.set(batch.transformMatrix)
    batch.transformMatrix = transform
  }

  /** Restores the batch transform to what it was before {@link #applyTransform(Batch, Matrix4)}. Note this causes the batch to be flushed.
    */
  protected def resetTransform(batch: Batch): Unit =
    batch.transformMatrix = oldTransform

  /** Set the shape renderer transformation matrix, often with the result of {@link #computeTransform()}. Note this causes the shape renderer to be flushed. {@link #resetTransform(ShapeRenderer)} will
    * restore the transform to what it was before this call.
    */
  protected def applyTransform(shapes: ShapeRenderer, transform: Matrix4): Unit = {
    oldTransform.set(shapes.transformMatrix)
    shapes.setTransformMatrix(transform)
    shapes.flush()
  }

  /** Restores the shape renderer transform to what it was before {@link #applyTransform(Batch, Matrix4)}. Note this causes the shape renderer to be flushed.
    */
  protected def resetTransform(shapes: ShapeRenderer): Unit =
    shapes.setTransformMatrix(oldTransform)

  /** Children completely outside of this rectangle will not be drawn. This is only valid for use with unrotated and unscaled actors.
    * @param cullingArea
    *   May be null.
    */
  def setCullingArea(cullingArea: Nullable[Rectangle]): Unit =
    this._cullingArea = cullingArea

  /** @return
    *   May be null.
    * @see
    *   #setCullingArea(Rectangle)
    */
  def cullingArea: Nullable[Rectangle] = _cullingArea

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (touchable && this.touchable == Touchable.disabled) scala.util.boundary.break(Nullable.empty)
    else if (!visible) scala.util.boundary.break(Nullable.empty)
    else {
      val point = Vector2()
      var i     = children.size - 1
      while (i >= 0) {
        val child = children(i)
        child.parentToLocalCoordinates(point.set(x, y))
        val hit = child.hit(point.x, point.y, touchable)
        if (hit.isDefined) scala.util.boundary.break(hit)
        i -= 1
      }
      super.hit(x, y, touchable)
    }
  }

  /** Called when actors are added to or removed from the group. */
  protected def childrenChanged(): Unit = {}

  /** Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this group, no changes are made.
    */
  def addActor(actor: Actor): Unit =
    if (!actor.parent.exists(_ eq this)) {
      actor.parent.foreach(_.removeActor(actor, unfocus = false))
      children.add(actor)
      actor.setParent(Nullable(this))
      stage.foreach(s => actor.setStage(Nullable(s)))
      childrenChanged()
    }

  /** Adds an actor as a child of this group at a specific index, removing it from its previous parent. If the actor is already a child of this group, no changes are made.
    * @param index
    *   May be greater than the number of children.
    */
  def addActorAt(index: Int, actor: Actor): Unit =
    if (!actor.parent.exists(_ eq this)) {
      actor.parent.foreach(_.removeActor(actor, unfocus = false))
      if (index >= children.size)
        children.add(actor)
      else
        children.insert(index, actor)
      actor.setParent(Nullable(this))
      stage.foreach(s => actor.setStage(Nullable(s)))
      childrenChanged()
    }

  /** Adds an actor as a child of this group immediately before another child actor, removing it from its previous parent. If the actor is already a child of this group, no changes are made.
    */
  def addActorBefore(actorBefore: Actor, actor: Actor): Unit =
    if (!actor.parent.exists(_ eq this)) {
      actor.parent.foreach(_.removeActor(actor, unfocus = false))
      val index = children.indexOf(actorBefore)
      children.insert(index, actor)
      actor.setParent(Nullable(this))
      stage.foreach(s => actor.setStage(Nullable(s)))
      childrenChanged()
    }

  /** Adds an actor as a child of this group immediately after another child actor, removing it from its previous parent. If the actor is already a child of this group, no changes are made. If
    * <code>actorAfter</code> is not in this group, the actor is added as the last child.
    */
  def addActorAfter(actorAfter: Actor, actor: Actor): Unit =
    if (!actor.parent.exists(_ eq this)) {
      actor.parent.foreach(_.removeActor(actor, unfocus = false))
      val index = children.indexOf(actorAfter)
      if (index == children.size || index == -1)
        children.add(actor)
      else
        children.insert(index + 1, actor)
      actor.setParent(Nullable(this))
      stage.foreach(s => actor.setStage(Nullable(s)))
      childrenChanged()
    }

  /** Removes an actor from this group and unfocuses it. Calls {@link #removeActor(Actor, boolean)} with true. */
  def removeActor(actor: Actor): Boolean =
    removeActor(actor, true)

  /** Removes an actor from this group. Calls {@link #removeActorAt(int, boolean)} with the actor's child index. */
  def removeActor(actor: Actor, unfocus: Boolean): Boolean = {
    val index = children.indexOf(actor)
    if (index == -1) false
    else {
      removeActorAt(index, unfocus)
      true
    }
  }

  /** Removes an actor from this group. If the actor will not be used again and has actions, they should be {@link Actor#clearActions() cleared} so the actions will be returned to their
    * {@link Action#setPool(com.badlogic.gdx.utils.Pool) pool}, if any. This is not done automatically.
    * @param unfocus
    *   If true, {@link Stage#unfocus(Actor)} is called.
    * @return
    *   the actor removed from this group.
    */
  def removeActorAt(index: Int, unfocus: Boolean): Actor = {
    val actor = children.removeIndex(index)
    stage.foreach { stage =>
      if (unfocus) stage.unfocus(actor)
      stage.actorRemoved(actor)
    }
    actor.setParent(Nullable.empty)
    actor.setStage(Nullable.empty)
    childrenChanged()
    actor
  }

  /** Removes all actors from this group. */
  def clearChildren(unfocus: Boolean = true): Unit = {
    val snapshot = children.toArray
    var i        = 0
    while (i < snapshot.length) {
      val child = snapshot(i)
      if (unfocus) {
        stage.foreach(_.unfocus(child))
      }
      child.setStage(Nullable.empty)
      child.setParent(Nullable.empty)
      i += 1
    }
    children.clear()
    childrenChanged()
  }

  /** Removes all children, actions, and listeners from this group. The children are unfocused. */
  override def clear(): Unit = {
    super.clear()
    clearChildren(unfocus = true)
  }

  /** Removes all children, actions, and listeners from this group. */
  def clear(unfocus: Boolean): Unit = {
    super.clear()
    clearChildren(unfocus)
  }

  /** Returns the first actor found with the specified name. Note this recursively compares the name of every actor in the group.
    */
  def findActor[T <: Actor](name: String): Nullable[T] = scala.util.boundary {
    var i = 0
    while (i < children.size) {
      children(i).name.foreach { n =>
        if (name == n) scala.util.boundary.break(Nullable(children(i).asInstanceOf[T]))
      }
      i += 1
    }
    i = 0
    while (i < children.size) {
      val child = children(i)
      child match {
        case group: Group =>
          val actor: Nullable[T] = group.findActor(name)
          if (actor.isDefined) scala.util.boundary.break(actor)
        case _ =>
      }
      i += 1
    }
    Nullable.empty
  }

  override protected[scene2d] def setStage(stage: Nullable[Stage]): Unit = {
    super.setStage(stage)
    var i = 0
    while (i < children.size) {
      children(i).setStage(stage) // StackOverflowError here means the group is its own ascendant.
      i += 1
    }
  }

  /** Swaps two actors by index. Returns false if the swap did not occur because the indexes were out of bounds. */
  def swapActor(first: Int, second: Int): Boolean = {
    val maxIndex = children.size
    if (first < 0 || first >= maxIndex) false
    else if (second < 0 || second >= maxIndex) false
    else {
      val tmp = children(first)
      children(first) = children(second)
      children(second) = tmp
      true
    }
  }

  /** Swaps two actors. Returns false if the swap did not occur because the actors are not children of this group. */
  def swapActor(first: Actor, second: Actor): Boolean = {
    val firstIndex  = children.indexOf(first)
    val secondIndex = children.indexOf(second)
    if (firstIndex == -1 || secondIndex == -1) false
    else {
      val tmp = children(firstIndex)
      children(firstIndex) = children(secondIndex)
      children(secondIndex) = tmp
      true
    }
  }

  /** Returns the child at the specified index. */
  def getChild(index: Int): Actor = children(index)

  def hasChildren: Boolean = children.nonEmpty

  // transform is a public var (declared above)

  /** Converts coordinates for this group to those of a descendant actor. The descendant does not need to be an immediate child.
    * @throws IllegalArgumentException
    *   if the specified actor is not a descendant of this group.
    */
  def localToDescendantCoordinates(descendant: Actor, localCoords: Vector2): Vector2 = {
    val p = descendant.parent
    if (p.isEmpty) throw new IllegalArgumentException("Actor is not a descendant: " + descendant)
    // First convert to the actor's parent coordinates.
    p.foreach { pp =>
      if (!(pp eq this)) localToDescendantCoordinates(pp, localCoords)
    }
    // Then from each parent down to the descendant.
    descendant.parentToLocalCoordinates(localCoords)
    localCoords
  }

  /** If true, {@link #drawDebug(ShapeRenderer)} will be called for this group and, optionally, all children recursively. */
  def setDebug(enabled: Boolean, recursively: Boolean): Unit = {
    setDebug(enabled)
    if (recursively) {
      children.foreach {
        case group: Group => group.setDebug(enabled, recursively)
        case child => child.setDebug(enabled)
      }
    }
  }

  /** Calls {@link #setDebug(boolean, boolean)} with {@code true, true}. */
  def debugAll(): Group = {
    setDebug(true, true)
    this
  }

  /** Returns a description of the actor hierarchy, recursively. */
  override def toString: String = {
    val buffer = new StringBuilder(128)
    toString(buffer, 1)
    buffer.setLength(buffer.length - 1)
    buffer.toString
  }

  private[scene2d] def toString(buffer: StringBuilder, indent: Int): Unit = {
    buffer.append(super.toString)
    buffer.append('\n')

    val snapshot = children.toArray
    var i        = 0
    while (i < snapshot.length) {
      var ii = 0
      while (ii < indent) {
        buffer.append("|  ")
        ii += 1
      }
      val actor = snapshot(i)
      actor match {
        case group: Group => group.toString(buffer, indent + 1)
        case _ =>
          buffer.append(actor)
          buffer.append('\n')
      }
      i += 1
    }
  }
}

object Group {}
