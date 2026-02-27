/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/Node.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package model

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** A node is part of a hierarchy of Nodes in a {@link Model}. A Node encodes a transform relative to its parents. A Node can have child nodes. Optionally a node can specify a {@link MeshPart} and a
  * {@link Material} to be applied to the mesh part.
  * @author
  *   badlogic
  */
class Node {

  /** the id, may be null, FIXME is this unique? * */
  var id: String = scala.compiletime.uninitialized

  /** Whether this node should inherit the transformation of its parent node, defaults to true. When this flag is false the value of {@link #globalTransform} will be the same as the value of
    * {@link #localTransform} causing the transform to be independent of its parent transform.
    */
  var inheritTransform: Boolean = true

  /** Whether this node is currently being animated, if so the translation, rotation and scale values are not used. */
  var isAnimated: Boolean = false

  /** the translation, relative to the parent, not modified by animations * */
  val translation: Vector3 = new Vector3()

  /** the rotation, relative to the parent, not modified by animations * */
  val rotation: Quaternion = new Quaternion(0, 0, 0, 1)

  /** the scale, relative to the parent, not modified by animations * */
  val scale: Vector3 = new Vector3(1, 1, 1)

  /** the local transform, based on translation/rotation/scale ({@link #calculateLocalTransform()}) or any applied animation * */
  val localTransform: Matrix4 = new Matrix4()

  /** the global transform, product of local transform and transform of the parent node, calculated via {@link #calculateWorldTransform()} *
    */
  val globalTransform: Matrix4 = new Matrix4()

  var parts: DynamicArray[NodePart] = DynamicArray[NodePart]()

  protected var parent: Nullable[Node]     = Nullable.empty
  private val children: DynamicArray[Node] = DynamicArray[Node]()

  /** Calculates the local transform based on the translation, scale and rotation
    * @return
    *   the local transform
    */
  def calculateLocalTransform(): Matrix4 = {
    if (!isAnimated) localTransform.set(translation, rotation, scale)
    localTransform
  }

  /** Calculates the world transform; the product of local transform and the parent's world transform.
    * @return
    *   the world transform
    */
  def calculateWorldTransform(): Matrix4 = {
    if (inheritTransform && parent.isDefined) {
      parent.foreach(p => globalTransform.set(p.globalTransform).mul(localTransform))
    } else {
      globalTransform.set(localTransform)
    }
    globalTransform
  }

  /** Calculates the local and world transform of this node and optionally all its children.
    *
    * @param recursive
    *   whether to calculate the local/world transforms for children.
    */
  def calculateTransforms(recursive: Boolean): Unit = {
    calculateLocalTransform()
    calculateWorldTransform()

    if (recursive) {
      for (child <- children)
        child.calculateTransforms(true)
    }
  }

  def calculateBoneTransforms(recursive: Boolean): Unit = {
    for (part <- parts)
      part.invBoneBindTransforms.foreach { binds =>
        part.bones.foreach { bns =>
          if (binds.size == bns.length) {
            val n = binds.size
            for (i <- 0 until n)
              bns(i).set(binds.getKeyAt(i).globalTransform).mul(binds.getValueAt(i))
          }
        }
      }
    if (recursive) {
      for (child <- children)
        child.calculateBoneTransforms(true)
    }
  }

  /** Calculate the bounding box of this Node. This is a potential slow operation, it is advised to cache the result. */
  def calculateBoundingBox(out: BoundingBox): BoundingBox = {
    out.inf()
    extendBoundingBox(out)
  }

  /** Calculate the bounding box of this Node. This is a potential slow operation, it is advised to cache the result. */
  def calculateBoundingBox(out: BoundingBox, transform: Boolean): BoundingBox = {
    out.inf()
    extendBoundingBox(out, transform)
  }

  /** Extends the bounding box with the bounds of this Node. This is a potential slow operation, it is advised to cache the result.
    */
  def extendBoundingBox(out: BoundingBox): BoundingBox =
    extendBoundingBox(out, true)

  /** Extends the bounding box with the bounds of this Node. This is a potential slow operation, it is advised to cache the result.
    */
  def extendBoundingBox(out: BoundingBox, transform: Boolean): BoundingBox = {
    for (part <- parts)
      if (part.enabled) {
        val mp = part.meshPart
        if (transform)
          mp.mesh.extendBoundingBox(out, mp.offset, mp.size, globalTransform)
        else
          mp.mesh.extendBoundingBox(out, mp.offset, mp.size)
      }
    for (child <- children)
      child.extendBoundingBox(out)
    out
  }

  /** Adds this node as child to specified parent Node, synonym for: <code>parent.addChild(this)</code>
    * @param parent
    *   The Node to attach this Node to.
    */
  def attachTo[T <: Node](parent: T): Unit =
    parent.addChild(this)

  /** Removes this node from its current parent, if any. Short for: <code>this.getParent().removeChild(this)</code> */
  def detach(): Unit =
    parent.foreach { p =>
      p.removeChild(this)
      parent = Nullable.empty
    }

  /** @return whether this Node has one or more children (true) or not (false) */
  def hasChildren: Boolean = children.nonEmpty

  /** @return
    *   The number of child nodes that this Node current contains.
    * @see
    *   #getChild(int)
    */
  def getChildCount: Int = children.size

  /** @param index
    *   The zero-based index of the child node to get, must be: 0 <= index < {@link #getChildCount()}.
    * @return
    *   The child node at the specified index
    */
  def getChild(index: Int): Node = children(index)

  /** @param recursive
    *   false to fetch a root child only, true to search the entire node tree for the specified node.
    * @return
    *   The node with the specified id, or null if not found.
    */
  def getChild(id: String, recursive: Boolean, ignoreCase: Boolean): Nullable[Node] =
    Node.getNode(children, id, recursive, ignoreCase)

  /** Adds the specified node as the currently last child of this node. If the node is already a child of another node, then it is removed from its current parent.
    * @param child
    *   The Node to add as child of this Node
    * @return
    *   the zero-based index of the child
    */
  def addChild[T <: Node](child: T): Int =
    insertChild(-1, child)

  /** Adds the specified nodes as the currently last child of this node. If the node is already a child of another node, then it is removed from its current parent.
    * @param nodes
    *   The Node to add as child of this Node
    * @return
    *   the zero-based index of the first added child
    */
  def addChildren[T <: Node](nodes: DynamicArray[T]): Int =
    insertChildren(-1, nodes)

  /** Insert the specified node as child of this node at the specified index. If the node is already a child of another node, then it is removed from its current parent. If the specified index is less
    * than zero or equal or greater than {@link #getChildCount()} then the Node is added as the currently last child.
    * @param index
    *   The zero-based index at which to add the child
    * @param child
    *   The Node to add as child of this Node
    * @return
    *   the zero-based index of the child
    */
  def insertChild[T <: Node](index: Int, child: T): Int = {
    // Check that child is not an ancestor of this node
    var p: Nullable[Node] = Nullable(this: Node)
    while (p.isDefined) {
      p.foreach(n => if (n eq child) throw SgeError.InvalidInput("Cannot add a parent as a child"))
      p = p.fold(Nullable.empty[Node])(_.parent)
    }
    // Remove child from current parent
    child.parent.foreach { parentNode =>
      if (!parentNode.removeChild(child)) throw SgeError.InvalidInput("Could not remove child from its current parent")
    }
    val actualIndex = if (index < 0 || index >= children.size) {
      children.add(child)
      children.size - 1
    } else {
      children.insert(index, child)
      index
    }
    child.parent = Nullable(this)
    actualIndex
  }

  /** Insert the specified nodes as children of this node at the specified index. If the node is already a child of another node, then it is removed from its current parent. If the specified index is
    * less than zero or equal or greater than {@link #getChildCount()} then the Node is added as the currently last child.
    * @param index
    *   The zero-based index at which to add the child
    * @param nodes
    *   The nodes to add as child of this Node
    * @return
    *   the zero-based index of the first inserted child
    */
  def insertChildren[T <: Node](index: Int, nodes: DynamicArray[T]): Int = {
    val startIdx = if (index < 0 || index > children.size) children.size else index
    var i        = startIdx
    for (child <- nodes) {
      insertChild(i, child)
      i += 1
    }
    startIdx
  }

  /** Removes the specified node as child of this node. On success, the child node will be not attached to any parent node (its {@link #getParent()} method will return null). If the specified node
    * currently isn't a child of this node then the removal is considered to be unsuccessful and the method will return false.
    * @param child
    *   The child node to remove.
    * @return
    *   Whether the removal was successful.
    */
  def removeChild[T <: Node](child: T): Boolean = {
    val idx = children.indexWhere(_ eq child)
    if (idx < 0) false
    else {
      children.removeIndex(idx)
      child.parent = Nullable.empty
      true
    }
  }

  /** @return A {@link DynamicArray} of all child nodes that this node contains. */
  def getChildren: DynamicArray[Node] = children

  /** @return The parent node that holds this node as child node, may be null. */
  def getParent: Nullable[Node] = parent

  /** @return Whether (true) is this Node is a child node of another node or not (false). */
  def hasParent: Boolean = parent.isDefined

  /** Creates a nested copy of this Node, any child nodes are copied using this method as well. The {@link #parts} are copied using the {@link NodePart#copy()} method. Note that that method copies the
    * material and nodes (bones) by reference. If you intend to use the copy in a different node tree (e.g. a different Model or ModelInstance) then you will need to update these references
    * afterwards.
    *
    * Override this method in your custom Node class to instantiate that class, in that case you should override the {@link #set(Node)} method as well.
    */
  def copy(): Node = new Node().set(this)

  /** Creates a nested copy of this Node, any child nodes are copied using the {@link #copy()} method. This will detach this node from its parent, but does not attach it to the parent of node being
    * copied. The {@link #parts} are copied using the {@link NodePart#copy()} method. Note that that method copies the material and nodes (bones) by reference. If you intend to use this node in a
    * different node tree (e.g. a different Model or ModelInstance) then you will need to update these references afterwards.
    *
    * Override this method in your custom Node class to copy any additional fields you've added.
    * @return
    *   This Node for chaining
    */
  protected def set(other: Node): Node = {
    detach()
    id = other.id
    isAnimated = other.isAnimated
    inheritTransform = other.inheritTransform
    translation.set(other.translation)
    rotation.set(other.rotation)
    scale.set(other.scale)
    localTransform.set(other.localTransform)
    globalTransform.set(other.globalTransform)
    parts.clear()
    for (nodePart <- other.parts)
      parts.add(nodePart.copy())
    children.clear()
    for (child <- other.getChildren)
      addChild(child.copy())
    this
  }
}

object Node {

  /** Helper method to recursive fetch a node from an array
    * @param recursive
    *   false to fetch a root node only, true to search the entire node tree for the specified node.
    * @return
    *   The node with the specified id, or null if not found.
    */
  def getNode(nodes: DynamicArray[Node], id: String, recursive: Boolean, ignoreCase: Boolean): Nullable[Node] = boundary {
    val n = nodes.size
    if (ignoreCase) {
      for (i <- 0 until n) {
        val node = nodes(i)
        if (node.id.equalsIgnoreCase(id)) break(Nullable(node))
      }
    } else {
      for (i <- 0 until n) {
        val node = nodes(i)
        if (node.id == id) break(Nullable(node))
      }
    }
    if (recursive) {
      for (i <- 0 until n) {
        val found = getNode(nodes(i).children, id, true, ignoreCase)
        if (found.isDefined) break(found)
      }
    }
    Nullable.empty
  }
}
