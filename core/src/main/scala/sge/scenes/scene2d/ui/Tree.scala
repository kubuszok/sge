/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Tree.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.math.{ Rectangle, Vector2 }
import sge.scenes.scene2d.{ Actor, Group, InputEvent }
import sge.scenes.scene2d.utils.{ ClickListener, Drawable, Layout, Selection, UIUtils }
import sge.utils.{ DynamicArray, MkArray, Nullable }

/** A tree widget where each node has an icon, actor, and child nodes. <p> The preferred size of the tree is determined by the preferred size of the actors for the expanded nodes. <p>
  * {@link ChangeEvent} is fired when the selected node changes.
  * @tparam N
  *   The type of nodes in the tree.
  * @tparam V
  *   The type of values for each node.
  * @author
  *   Nathan Sweet
  */
class Tree[N <: Tree.Node[N, V, ? <: Actor], V](style: Tree.TreeStyle)(using sge: Sge) extends WidgetGroup with Styleable[Tree.TreeStyle] {

  private var _style: Tree.TreeStyle  = scala.compiletime.uninitialized
  val rootNodes:      DynamicArray[N] = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[N]], 16, true)
  val selection:      Selection[N]    = new Selection[N]() {
    override protected def changed(): Unit =
      size match {
        case 0 =>
          rangeStart = Nullable.empty
        case 1 =>
          rangeStart = first
        case _ =>
      }
  }
  var ySpacing:               Float         = 4
  var iconSpacingLeft:        Float         = 2
  var iconSpacingRight:       Float         = 2
  var paddingLeft:            Float         = 0
  var paddingRight:           Float         = 0
  var indentSpacing:          Float         = 0
  private var prefWidth:      Float         = 0
  private var prefHeight:     Float         = 0
  private var sizeInvalid:    Boolean       = true
  private var foundNode:      Nullable[N]   = Nullable.empty
  private var _overNode:      Nullable[N]   = Nullable.empty
  var rangeStart:             Nullable[N]   = Nullable.empty
  private var _clickListener: ClickListener = scala.compiletime.uninitialized

  selection.setActor(Nullable(this))
  selection.setMultiple(true)
  setStyle(style)
  initialize()

  def this(skin: Skin)(using sge: Sge) = this(skin.get(classOf[Tree.TreeStyle]))
  def this(skin: Skin, styleName: String)(using sge: Sge) = this(skin.get(styleName, classOf[Tree.TreeStyle]))

  private def initialize(): Unit = {
    val self = this
    _clickListener = new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit =
        scala.util.boundary {
          val node: Nullable[N] = self.getNodeAt(y)
          if (node.isEmpty) scala.util.boundary.break(())
          node.foreach { n =>
            val nodeAtTouchDown: Nullable[N] = self.getNodeAt(getTouchDownY)
            if (nodeAtTouchDown.fold(true)(_ ne n)) scala.util.boundary.break(())
            if (self.selection.getMultiple && self.selection.notEmpty && UIUtils.shift()) {
              // Select range (shift).
              if (self.rangeStart.isEmpty) self.rangeStart = Nullable(n)
              self.rangeStart.foreach { rs =>
                if (!UIUtils.ctrl()) self.selection.clear()
                val start = rs.actor.getY
                val end   = n.actor.getY
                if (start > end)
                  self.selectNodes(self.rootNodes, end, start)
                else {
                  self.selectNodes(self.rootNodes, start, end)
                  // Note: In LibGDX, orderedItems().reverse() reverses the selection's internal order in-place.
                  // LinkedHashSet does not support in-place reversal; this is a minor behavioral difference.
                }

                self.selection.fireChangeEvent()
                self.rangeStart = Nullable(rs)
              }
              scala.util.boundary.break(())
            }
            if (n.children.size > 0 && (!self.selection.getMultiple || !UIUtils.ctrl())) {
              // Toggle expanded if left of icon.
              var rowX = n.actor.getX
              n.icon.foreach { icon =>
                rowX -= self.iconSpacingRight + icon.getMinWidth
              }
              if (x < rowX) {
                n.setExpanded(!n.expanded)
                scala.util.boundary.break(())
              }
            }
            if (!n.isSelectable) scala.util.boundary.break(())
            self.selection.choose(n)
            if (!self.selection.isEmpty) self.rangeStart = Nullable(n)
          }
        }

      override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
        self.setOverNode(self.getNodeAt(y))
        false
      }

      override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit = {
        super.enter(event, x, y, pointer, fromActor)
        self.setOverNode(self.getNodeAt(y))
      }

      override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
        super.exit(event, x, y, pointer, toActor)
        toActor.fold(self.setOverNode(Nullable.empty)) { ta =>
          if (!ta.isDescendantOf(self)) self.setOverNode(Nullable.empty)
        }
      }
    }
    addListener(_clickListener)
  }

  def setStyle(style: Tree.TreeStyle): Unit = {
    this._style = style

    // Reasonable default.
    if (indentSpacing == 0) indentSpacing = plusMinusWidth()
  }

  def getStyle: Tree.TreeStyle = _style

  def add(node: N): Unit =
    insert(rootNodes.size, node)

  def insert(index: Int, node: N): Unit = scala.util.boundary {
    var idx = index
    node.parent.fold {
      val existingIndex = rootNodes.indexOf(node)
      if (existingIndex != -1) {
        if (existingIndex == idx) scala.util.boundary.break(())
        if (existingIndex < idx) idx -= 1
        rootNodes.removeIndex(existingIndex)
        val actorIndex = node.actor.getZIndex
        if (actorIndex != -1) node.removeFromTree(this, actorIndex)
      }
    } { p =>
      p.remove(node)
      node.parent = Nullable.empty
    }

    rootNodes.insert(idx, node)

    val actorIndex: Int =
      if (idx == 0)
        0
      else if (idx < rootNodes.size - 1)
        rootNodes(idx + 1).actor.getZIndex
      else {
        val before = rootNodes(idx - 1)
        before.actor.getZIndex + before.countActors()
      }
    node.addToTree(this, actorIndex)
  }

  def remove(node: N): Unit =
    scala.util.boundary {
      node.parent.foreach { p =>
        p.remove(node)
        scala.util.boundary.break(())
      }
      val idx = rootNodes.indexOf(node)
      if (idx == -1) scala.util.boundary.break(())
      rootNodes.removeIndex(idx)
      val actorIndex = node.actor.getZIndex
      if (actorIndex != -1) node.removeFromTree(this, actorIndex)
    }

  /** Removes all tree nodes. */
  override def clearChildren(unfocus: Boolean): Unit = {
    super.clearChildren(unfocus)
    setOverNode(Nullable.empty)
    rootNodes.clear()
    selection.clear()
  }

  override def invalidate(): Unit = {
    super.invalidate()
    sizeInvalid = true
  }

  private def plusMinusWidth(): Float = {
    var width = Math.max(_style.plus.getMinWidth, _style.minus.getMinWidth)
    _style.plusOver.foreach { po =>
      width = Math.max(width, po.getMinWidth)
    }
    _style.minusOver.foreach { mo =>
      width = Math.max(width, mo.getMinWidth)
    }
    width
  }

  private def computeSize(): Unit = {
    sizeInvalid = false
    prefWidth = plusMinusWidth()
    prefHeight = 0
    computeSize(rootNodes, 0, prefWidth)
    prefWidth += paddingLeft + paddingRight
  }

  private def computeSize(nodes: DynamicArray[N], indent: Float, plusMinusWidth: Float): Unit = {
    val ySpacing = this.ySpacing
    val spacing  = iconSpacingLeft + iconSpacingRight
    var i        = 0
    val n        = nodes.size
    while (i < n) {
      val node     = nodes(i)
      var rowWidth = indent + plusMinusWidth
      val actor    = node.actor
      actor match {
        case layout: Layout =>
          rowWidth += layout.getPrefWidth
          node.height = layout.getPrefHeight
        case _ =>
          rowWidth += actor.getWidth
          node.height = actor.getHeight
      }
      node.icon.foreach { icon =>
        rowWidth += spacing + icon.getMinWidth
        node.height = Math.max(node.height, icon.getMinHeight)
      }
      prefWidth = Math.max(prefWidth, rowWidth)
      prefHeight += node.height + ySpacing
      if (node.expanded) computeSize(node.children, indent + indentSpacing, plusMinusWidth)
      i += 1
    }
  }

  override def layout(): Unit = {
    if (sizeInvalid) computeSize()
    layout(rootNodes, paddingLeft, getHeight - ySpacing / 2, plusMinusWidth())
  }

  private def layout(nodes: DynamicArray[N], indent: Float, y: Float, plusMinusWidth: Float): Float = {
    val ySpacing        = this.ySpacing
    val iconSpacingLeft = this.iconSpacingLeft
    val spacing         = iconSpacingLeft + iconSpacingRight
    var yy              = y
    var i               = 0
    val n               = nodes.size
    while (i < n) {
      val node = nodes(i)
      var x    = indent + plusMinusWidth
      node.icon.fold {
        x += iconSpacingLeft
      } { icon =>
        x += spacing + icon.getMinWidth
      }
      node.actor match {
        case layout: Layout => layout.pack()
        case _ =>
      }
      yy -= node.getHeight()
      node.actor.setPosition(x, yy)
      yy -= ySpacing
      if (node.expanded) yy = layout(node.children, indent + indentSpacing, yy, plusMinusWidth)
      i += 1
    }
    yy
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    drawBackground(batch, parentAlpha)
    val color = getColor
    val a     = color.a * parentAlpha
    batch.setColor(color.r, color.g, color.b, a)
    drawIcons(batch, color.r, color.g, color.b, a, Nullable.empty, rootNodes, paddingLeft, plusMinusWidth())
    super.draw(batch, parentAlpha) // Draw node actors.
  }

  /** Called to draw the background. Default implementation draws the style background drawable. */
  protected def drawBackground(batch: Batch, parentAlpha: Float): Unit =
    _style.background.foreach { bg =>
      val color = getColor
      batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
      bg.draw(batch, getX, getY, getWidth, getHeight)
    }

  /** Draws selection, icons, and expand icons.
    * @param parent
    *   null for the root nodes.
    * @return
    *   The Y position of the last visible actor for the nodes.
    */
  protected def drawIcons(
    batch:          Batch,
    r:              Float,
    g:              Float,
    b:              Float,
    a:              Float,
    parent:         Nullable[N],
    nodes:          DynamicArray[N],
    indent:         Float,
    plusMinusWidth: Float
  ): Float = {

    val cullingArea = getCullingArea
    var cullBottom  = 0f
    var cullTop     = 0f
    cullingArea.foreach { ca =>
      cullBottom = ca.y
      cullTop = cullBottom + ca.height
    }
    val style   = this._style
    val x       = getX
    val y       = getY
    val expandX = x + indent
    val iconX   = expandX + plusMinusWidth + iconSpacingLeft
    var actorY  = 0f
    var i       = 0
    val n       = nodes.size
    scala.util.boundary {
      while (i < n) {
        val node  = nodes(i)
        val actor = node.actor
        actorY = actor.getY
        val height = node.height
        if (cullingArea.isEmpty || (actorY + height >= cullBottom && actorY <= cullTop)) {
          if (selection.contains(Nullable(node)) && style.selection.isDefined) {
            style.selection.foreach { sel =>
              drawSelection(node, sel, batch, x, y + actorY - ySpacing / 2, getWidth, height + ySpacing)
            }
          } else if (_overNode.fold(false)(_ eq node) && style.over.isDefined) {
            style.over.foreach { ov =>
              drawOver(node, ov, batch, x, y + actorY - ySpacing / 2, getWidth, height + ySpacing)
            }
          }

          node.icon.foreach { icon =>
            val iconY      = y + actorY + Math.round((height - icon.getMinHeight) / 2)
            val actorColor = actor.getColor
            batch.setColor(actorColor.r, actorColor.g, actorColor.b, actorColor.a * a)
            drawIcon(node, icon, batch, iconX, iconY)
            batch.setColor(r, g, b, a)
          }

          if (node.children.size > 0) {
            val expandIcon = getExpandIcon(node, iconX)
            val iconY      = y + actorY + Math.round((height - expandIcon.getMinHeight) / 2)
            drawExpandIcon(node, expandIcon, batch, expandX, iconY)
          }
        } else if (actorY < cullBottom) //
          scala.util.boundary.break(())
        if (node.expanded && node.children.size > 0)
          drawIcons(batch, r, g, b, a, Nullable(node), node.children, indent + indentSpacing, plusMinusWidth)
        i += 1
      }
    }
    actorY
  }

  protected def drawSelection(node: N, selection: Drawable, batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit =
    selection.draw(batch, x, y, width, height)

  protected def drawOver(node: N, over: Drawable, batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit =
    over.draw(batch, x, y, width, height)

  protected def drawExpandIcon(node: N, expandIcon: Drawable, batch: Batch, x: Float, y: Float): Unit =
    expandIcon.draw(batch, x, y, expandIcon.getMinWidth, expandIcon.getMinHeight)

  protected def drawIcon(node: N, icon: Drawable, batch: Batch, x: Float, y: Float): Unit =
    icon.draw(batch, x, y, icon.getMinWidth, icon.getMinHeight)

  /** Returns the drawable for the expand icon. The default implementation returns {@link TreeStyle#plusOver} or {@link TreeStyle#minusOver} on the desktop if the node is the
    * {@link #getOverNode() over node}, the mouse is left of <code>iconX</code>, and clicking would expand the node.
    * @param iconX
    *   The X coordinate of the over node's icon.
    */
  protected def getExpandIcon(node: N, iconX: Float): Drawable =
    if (
      _overNode.fold(false)(_ eq node) //
      && sge.application.getType() == Application.ApplicationType.Desktop //
      && (!selection.getMultiple || (!UIUtils.ctrl() && !UIUtils.shift())) //
    ) {
      val mouseX = screenToLocalCoordinates(Tree.tmp.set(sge.input.getX().toFloat, 0)).x + getX
      if (mouseX >= 0 && mouseX < iconX) {
        val icon: Nullable[Drawable] = if (node.expanded) _style.minusOver else _style.plusOver
        val defaultIcon = if (node.expanded) _style.minus else _style.plus
        icon.getOrElse(defaultIcon)
      } else {
        if (node.expanded) _style.minus else _style.plus
      }
    } else {
      if (node.expanded) _style.minus else _style.plus
    }

  /** @return May be null. */
  def getNodeAt(y: Float): Nullable[N] = {
    foundNode = Nullable.empty
    getNodeAt(rootNodes, y, getHeight)
    val result = foundNode
    foundNode = Nullable.empty
    result
  }

  private def getNodeAt(nodes: DynamicArray[N], y: Float, rowY: Float): Float = scala.util.boundary {
    var rY = rowY
    var i  = 0
    val n  = nodes.size
    while (i < n) {
      val node   = nodes(i)
      val height = node.height
      rY -= node.getHeight() - height // Node subclass may increase getHeight.
      if (y >= rY - height - ySpacing && y < rY) {
        foundNode = Nullable(node)
        scala.util.boundary.break(-1f)
      }
      rY -= height + ySpacing
      if (node.expanded) {
        rY = getNodeAt(node.children, y, rY)
        if (rY == -1) scala.util.boundary.break(-1f)
      }
      i += 1
    }
    rY
  }

  private[ui] def selectNodes(nodes: DynamicArray[N], low: Float, high: Float): Unit =
    scala.util.boundary {
      var i = 0
      val n = nodes.size
      while (i < n) {
        val node = nodes(i)
        if (node.actor.getY < low) scala.util.boundary.break(())
        scala.util.boundary {
          if (!node.isSelectable) scala.util.boundary.break(())
          if (node.actor.getY <= high) selection.add(node)
          if (node.expanded) selectNodes(node.children, low, high)
        }
        i += 1
      }
    }

  def getSelection: Selection[N] = selection

  /** Returns the first selected node, or null. */
  def getSelectedNode: Nullable[N] = selection.first

  /** Returns the first selected value, or null. */
  def getSelectedValue: Nullable[V] = {
    val node = selection.first
    node.fold(Nullable.empty[V])(n => n.getValue)
  }

  /** If the order of the root nodes is changed, {@link #updateRootNodes()} must be called to ensure the nodes' actors are in the correct order.
    */
  def getRootNodes: DynamicArray[N] = rootNodes

  /** @deprecated Use {@link #getRootNodes()}. */
  @deprecated("Use getRootNodes()", "")
  def getNodes: DynamicArray[N] = rootNodes

  /** Updates the order of the actors in the tree for all root nodes and all child nodes. This is useful after changing the order of {@link #getRootNodes()}.
    * @see
    *   Node#updateChildren()
    */
  def updateRootNodes(): Unit = {
    var i = 0
    var n = rootNodes.size
    while (i < n) {
      val node       = rootNodes(i)
      val actorIndex = node.actor.getZIndex
      if (actorIndex != -1) node.removeFromTree(this, actorIndex)
      i += 1
    }
    i = 0
    n = rootNodes.size
    var actorIndex = 0
    while (i < n) {
      actorIndex += rootNodes(i).addToTree(this, actorIndex)
      i += 1
    }
  }

  /** @return May be null. */
  def getOverNode: Nullable[N] = _overNode

  /** @return May be null. */
  def getOverValue: Nullable[V] =
    _overNode.fold(Nullable.empty[V])(n => n.getValue)

  /** @param overNode May be null. */
  def setOverNode(overNode: Nullable[N]): Unit =
    this._overNode = overNode

  /** Sets the amount of horizontal space between the nodes and the left/right edges of the tree. */
  def setPadding(padding: Float): Unit = {
    paddingLeft = padding
    paddingRight = padding
  }

  /** Sets the amount of horizontal space between the nodes and the left/right edges of the tree. */
  def setPadding(left: Float, right: Float): Unit = {
    this.paddingLeft = left
    this.paddingRight = right
  }

  def setIndentSpacing(indentSpacing: Float): Unit =
    this.indentSpacing = indentSpacing

  /** Returns the amount of horizontal space for indentation level. */
  def getIndentSpacing: Float = indentSpacing

  /** Sets the amount of vertical space between nodes. */
  def setYSpacing(ySpacing: Float): Unit =
    this.ySpacing = ySpacing

  def getYSpacing: Float = ySpacing

  /** Sets the amount of horizontal space left and right of the node's icon. If a node has no icon, the left spacing is used between the plus/minus drawable and the node's actor.
    */
  def setIconSpacing(left: Float, right: Float): Unit = {
    this.iconSpacingLeft = left
    this.iconSpacingRight = right
  }

  override def getPrefWidth: Float = {
    if (sizeInvalid) computeSize()
    prefWidth
  }

  override def getPrefHeight: Float = {
    if (sizeInvalid) computeSize()
    prefHeight
  }

  def findExpandedValues(values: DynamicArray[V]): Unit =
    Tree.findExpandedValues(rootNodes, values.asInstanceOf[DynamicArray[Any]])

  def restoreExpandedValues(values: DynamicArray[V]): Unit = {
    var i = 0
    val n = values.size
    while (i < n) {
      val node = findNode(values(i))
      node.foreach { nd =>
        nd.setExpanded(true)
        nd.expandTo()
      }
      i += 1
    }
  }

  /** Returns the node with the specified value, or null. */
  def findNode(value: V): Nullable[N] =
    Tree.findNode(rootNodes, value).asInstanceOf[Nullable[N]]

  def collapseAll(): Unit =
    Tree.collapseAll(rootNodes)

  def expandAll(): Unit =
    Tree.expandAll(rootNodes)

  /** Returns the click listener the tree uses for clicking on nodes and the over node. */
  def getClickListener: ClickListener = _clickListener
}

object Tree {
  private val tmp: Vector2 = new Vector2()

  /** Helper to remove a node from a tree without running into F-bounded type bounds issues. Replicates the logic of Tree.remove() to avoid the type system constraints.
    */
  private[ui] def removeNodeFromTree(tree: Tree[? <: Node[?, ?, ?], ?], node: Node[?, ?, ?]): Unit = scala.util.boundary {
    node.parent.foreach { p =>
      val idx = p.children.asInstanceOf[DynamicArray[Any]].indexOf(node)
      if (idx != -1) {
        p.children.removeIndex(idx)
        if (p.expanded) {
          node.removeFromTree(tree, node.actor.getZIndex)
        }
        node.parent = Nullable.empty
      }
      scala.util.boundary.break()
    }
    val rootNodes = tree.rootNodes.asInstanceOf[DynamicArray[Any]]
    val idx       = rootNodes.indexOf(node)
    if (idx != -1) {
      rootNodes.removeIndex(idx)
      val actorIndex = node.actor.getZIndex
      if (actorIndex != -1) node.removeFromTree(tree, actorIndex)
    }
  }

  private[ui] def findExpandedValues[N <: Node[N, ?, ? <: Actor]](nodes: DynamicArray[? <: Node[?, ?, ? <: Actor]], values: DynamicArray[Any]): Boolean = {
    var expanded = false
    var i        = 0
    val n        = nodes.size
    while (i < n) {
      val node = nodes(i)
      if (node.expanded && !findExpandedValues(node.children, values)) values.add(node.value)
      i += 1
    }
    expanded
  }

  private[ui] def findNode(nodes: DynamicArray[? <: Node[?, ?, ? <: Actor]], value: Any): Nullable[Node[?, ?, ? <: Actor]] = scala.util.boundary {
    var i = 0
    var n = nodes.size
    while (i < n) {
      val node: Node[?, ?, ? <: Actor] = nodes(i)
      if (value == node.value) scala.util.boundary.break(Nullable(node))
      i += 1
    }
    i = 0
    n = nodes.size
    while (i < n) {
      val node  = nodes(i)
      val found = findNode(node.children, value)
      if (found.isDefined) scala.util.boundary.break(found)
      i += 1
    }
    Nullable.empty
  }

  private[ui] def collapseAll(nodes: DynamicArray[? <: Node[?, ?, ? <: Actor]]): Unit = {
    var i = 0
    val n = nodes.size
    while (i < n) {
      val node = nodes(i)
      node.setExpanded(false)
      collapseAll(node.children)
      i += 1
    }
  }

  private[ui] def expandAll(nodes: DynamicArray[? <: Node[?, ?, ? <: Actor]]): Unit = {
    var i = 0
    val n = nodes.size
    while (i < n) {
      nodes(i).expandAll()
      i += 1
    }
  }

  /** A {@link Tree} node which has an actor and value. <p> A subclass can be used so the generic type parameters don't need to be specified repeatedly.
    * @tparam N
    *   The type for the node's parent and child nodes.
    * @tparam V
    *   The type for the node's value.
    * @tparam A
    *   The type for the node's actor.
    * @author
    *   Nathan Sweet
    */
  abstract class Node[N <: Node[N, V, A], V, A <: Actor] {
    var actor:      A                  = scala.compiletime.uninitialized
    var parent:     Nullable[N]        = Nullable.empty
    val children:   DynamicArray[N]    = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[N]], 16, true)
    var selectable: Boolean            = true
    var expanded:   Boolean            = false
    var icon:       Nullable[Drawable] = Nullable.empty
    var height:     Float              = 0
    var value:      Nullable[V]        = Nullable.empty

    def this(actor: A) = {
      this()
      this.actor = actor
    }

    /** Creates a node without an actor. An actor must be set using {@link #setActor(Actor)} before this node can be used. */

    def setExpanded(expanded: Boolean): Unit =
      if (expanded == this.expanded) {
        // nothing to do
      } else {
        this.expanded = expanded
        if (children.size == 0) {
          // nothing to do
        } else {
          val tree = getTree()
          tree.fold(()) { t =>
            var actorIndex = actor.getZIndex + 1
            if (expanded) {
              var i = 0
              val n = children.size
              while (i < n) {
                actorIndex += children(i).addToTree(t, actorIndex)
                i += 1
              }
            } else {
              var i = 0
              val n = children.size
              while (i < n) {
                children(i).removeFromTree(t, actorIndex)
                i += 1
              }
            }
          }
        }
      }

    /** Called to add the actor to the tree when the node's parent is expanded.
      * @return
      *   The number of node actors added to the tree.
      */
    protected[ui] def addToTree(tree: Tree[? <: Node[?, ?, ?], ?], actorIndex: Int): Int = {
      tree.addActorAt(actorIndex, actor)
      if (!expanded) 1
      else {
        var childIndex = actorIndex + 1
        var i          = 0
        val n          = children.size
        while (i < n) {
          childIndex += children(i).addToTree(tree, childIndex)
          i += 1
        }
        childIndex - actorIndex
      }
    }

    /** Called to remove the actor from the tree, eg when the node is removed or the node's parent is collapsed. */
    protected[ui] def removeFromTree(tree: Tree[? <: Node[?, ?, ?], ?], actorIndex: Int): Unit = {
      val removeActorAt = tree.removeActorAt(actorIndex, true)
      // assert removeActorAt != actor; // If false, either 1) there's a bug, or 2) the children were modified.
      if (!expanded) {
        // nothing to do
      } else {
        var i = 0
        val n = children.size
        while (i < n) {
          children(i).removeFromTree(tree, actorIndex)
          i += 1
        }
      }
    }

    def add(node: N): Unit =
      insert(children.size, node)

    def addAll(nodes: DynamicArray[N]): Unit = {
      var i = 0
      val n = nodes.size
      while (i < n) {
        insert(children.size, nodes(i))
        i += 1
      }
    }

    def insert(childIndex: Int, node: N): Unit = {
      node.parent = Nullable(this.asInstanceOf[N])
      children.insert(childIndex, node)
      if (expanded) {
        val tree = getTree()
        tree.foreach { t =>
          val actorIndex: Int =
            if (childIndex == 0)
              actor.getZIndex + 1
            else if (childIndex < children.size - 1)
              children(childIndex + 1).actor.getZIndex
            else {
              val before = children(childIndex - 1)
              before.actor.getZIndex + before.countActors()
            }
          node.addToTree(t, actorIndex)
        }
      }
    }

    private[ui] def countActors(): Int =
      if (!expanded) 1
      else {
        var count = 1
        var i     = 0
        val n     = children.size
        while (i < n) {
          count += children(i).countActors()
          i += 1
        }
        count
      }

    /** Remove this node from its parent. */
    def remove(): Unit = {
      val tree = getTree()
      tree.fold {
        parent.foreach(_.remove(this.asInstanceOf[N]))
      } { t =>
        Tree.removeNodeFromTree(t, this)
      }
    }

    /** Remove the specified child node from this node. Does nothing if the node is not a child of this node. */
    def remove(node: N): Unit = {
      val idx = children.indexOf(node)
      if (idx != -1) {
        children.removeIndex(idx)
        if (expanded) {
          val tree = getTree()
          tree.foreach { t =>
            node.removeFromTree(t, node.actor.getZIndex)
          }
        }
      }
    }

    /** Removes all children from this node. */
    def clearChildren(): Unit = {
      if (expanded) {
        val tree = getTree()
        tree.foreach { t =>
          val actorIndex = actor.getZIndex + 1
          var i          = 0
          val n          = children.size
          while (i < n) {
            children(i).removeFromTree(t, actorIndex)
            i += 1
          }
        }
      }
      children.clear()
    }

    /** Returns the tree this node's actor is currently in, or null. The actor is only in the tree when all of its parent nodes are expanded.
      */
    def getTree(): Nullable[Tree[? <: Node[?, ?, ?], ?]] = {
      val parent = actor.getParent
      parent.fold(Nullable.empty[Tree[? <: Node[?, ?, ?], ?]]) {
        case tree: Tree[?, ?] => Nullable(tree.asInstanceOf[Tree[? <: Node[?, ?, ?], ?]])
        case _ => Nullable.empty
      }
    }

    def setActor(newActor: A): Unit = {
      Nullable(actor).foreach { currentActor =>
        getTree().foreach { t =>
          val index = currentActor.getZIndex
          t.removeActorAt(index, true)
          t.addActorAt(index, newActor)
        }
      }
      actor = newActor
    }

    def getActor: A = actor

    def isExpanded: Boolean = expanded

    /** If the children order is changed, {@link #updateChildren()} must be called to ensure the node's actors are in the correct order. That is not necessary if this node is not in the tree or is not
      * expanded, because then the child node's actors are not in the tree.
      */
    def getChildren: DynamicArray[N] = children

    def hasChildren: Boolean = children.size > 0

    /** Updates the order of the actors in the tree for this node and all child nodes. This is useful after changing the order of {@link #getChildren()}.
      * @see
      *   Tree#updateRootNodes()
      */
    def updateChildren(): Unit =
      if (expanded) {
        val tree = getTree()
        tree.foreach { t =>
          val n          = children.size
          var actorIndex = actor.getZIndex + 1
          var i          = 0
          while (i < n) {
            children(i).removeFromTree(t, actorIndex)
            i += 1
          }
          i = 0
          while (i < n) {
            actorIndex += children(i).addToTree(t, actorIndex)
            i += 1
          }
        }
      }

    /** @return May be null. */
    def getParent: Nullable[N] = parent

    /** Sets an icon that will be drawn to the left of the actor. */
    def setIcon(icon: Nullable[Drawable]): Unit =
      this.icon = icon

    def getValue: Nullable[V] = value

    /** Sets an application specific value for this node. */
    def setValue(value: Nullable[V]): Unit =
      this.value = value

    def getIcon: Nullable[Drawable] = icon

    def getLevel: Int = {
      var level = 0
      var current: Nullable[Node[?, ?, ?]] = Nullable(this)
      while (current.isDefined) {
        level += 1
        current = current.fold(Nullable.empty[Node[?, ?, ?]])(_.getParent.asInstanceOf[Nullable[Node[?, ?, ?]]])
      }
      level
    }

    /** Returns this node or the child node with the specified value, or null. */
    def findNode(value: V): Nullable[N] =
      if (value == this.value) Nullable(this.asInstanceOf[N])
      else Tree.findNode(children, value).asInstanceOf[Nullable[N]]

    /** Collapses all nodes under and including this node. */
    def collapseAll(): Unit = {
      setExpanded(false)
      Tree.collapseAll(children)
    }

    /** Expands all nodes under and including this node. */
    def expandAll(): Unit = {
      setExpanded(true)
      if (children.size > 0) Tree.expandAll(children)
    }

    /** Expands all parent nodes of this node. */
    def expandTo(): Unit = {
      var node = parent
      while (node.isDefined)
        node.foreach { n =>
          n.setExpanded(true)
          node = n.parent
        }
    }

    def isSelectable: Boolean = selectable

    def setSelectable(selectable: Boolean): Unit =
      this.selectable = selectable

    def findExpandedValues(values: DynamicArray[V]): Unit =
      if (expanded && !Tree.findExpandedValues(children, values.asInstanceOf[DynamicArray[Any]])) {
        value.foreach(v => values.add(v))
      }

    def restoreExpandedValues(values: DynamicArray[V]): Unit = {
      var i = 0
      val n = values.size
      while (i < n) {
        val node = findNode(values(i))
        node.foreach { nd =>
          nd.setExpanded(true)
          nd.expandTo()
        }
        i += 1
      }
    }

    /** Returns the height of the node as calculated for layout. A subclass may override and increase the returned height to create a blank space in the tree above the node, eg for a separator.
      */
    def getHeight(): Float = height

    /** Returns true if the specified node is this node or an ascendant of this node. */
    def isAscendantOf(node: N): Boolean = scala.util.boundary {
      var current: Nullable[Node[?, ?, ?]] = Nullable(node)
      while (current.isDefined) {
        if (current.fold(false)(_ eq this)) scala.util.boundary.break(true)
        current = current.fold(Nullable.empty[Node[?, ?, ?]])(_.parent.asInstanceOf[Nullable[Node[?, ?, ?]]])
      }
      false
    }

    /** Returns true if the specified node is this node or an descendant of this node. */
    def isDescendantOf(node: N): Boolean = scala.util.boundary {
      var current: Nullable[Node[?, ?, ?]] = Nullable(this)
      while (current.isDefined) {
        if (current.fold(false)(_ eq node)) scala.util.boundary.break(true)
        current = current.fold(Nullable.empty[Node[?, ?, ?]])(_.parent.asInstanceOf[Nullable[Node[?, ?, ?]]])
      }
      false
    }
  }

  /** The style for a {@link Tree}.
    * @author
    *   Nathan Sweet
    */
  class TreeStyle() {
    var plus:       Drawable           = scala.compiletime.uninitialized
    var minus:      Drawable           = scala.compiletime.uninitialized
    var plusOver:   Nullable[Drawable] = Nullable.empty
    var minusOver:  Nullable[Drawable] = Nullable.empty
    var over:       Nullable[Drawable] = Nullable.empty
    var selection:  Nullable[Drawable] = Nullable.empty
    var background: Nullable[Drawable] = Nullable.empty

    def this(plus: Drawable, minus: Drawable, selection: Nullable[Drawable]) = {
      this()
      this.plus = plus
      this.minus = minus
      this.selection = selection
    }

    def this(style: TreeStyle) = {
      this()
      plus = style.plus
      minus = style.minus

      plusOver = style.plusOver
      minusOver = style.minusOver

      over = style.over
      selection = style.selection
      background = style.background
    }
  }
}
