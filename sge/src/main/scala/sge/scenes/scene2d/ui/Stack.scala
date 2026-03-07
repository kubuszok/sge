/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Stack.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.scenes.scene2d.utils.Layout

/** A stack is a container that sizes its children to its size and positions them at 0,0 on top of each other. <p> The preferred and min size of the stack is the largest preferred and min size of any
  * children. The max size of the stack is the smallest max size of any children.
  * @author
  *   Nathan Sweet
  */
class Stack()(using Sge) extends WidgetGroup() {
  private var _prefWidth:  Float   = 0
  private var _prefHeight: Float   = 0
  private var _minWidth:   Float   = 0
  private var _minHeight:  Float   = 0
  private var _maxWidth:   Float   = 0
  private var _maxHeight:  Float   = 0
  private var sizeInvalid: Boolean = true

  setTransform(false)
  setWidth(150)
  setHeight(150)
  touchable = Touchable.childrenOnly

  def this(actors: Actor*)(using Sge) = {
    this()
    actors.foreach(addActor)
  }

  override def invalidate(): Unit = {
    super.invalidate()
    sizeInvalid = true
  }

  private def computeSize(): Unit = {
    sizeInvalid = false
    _prefWidth = 0
    _prefHeight = 0
    _minWidth = 0
    _minHeight = 0
    _maxWidth = 0
    _maxHeight = 0
    val children = getChildren
    var i        = 0
    val n        = children.size
    while (i < n) {
      val child = children(i)
      var childMaxWidth:  Float = 0
      var childMaxHeight: Float = 0
      child match {
        case layout: Layout =>
          _prefWidth = Math.max(_prefWidth, layout.getPrefWidth)
          _prefHeight = Math.max(_prefHeight, layout.getPrefHeight)
          _minWidth = Math.max(_minWidth, layout.getMinWidth)
          _minHeight = Math.max(_minHeight, layout.getMinHeight)
          childMaxWidth = layout.getMaxWidth
          childMaxHeight = layout.getMaxHeight
        case _ =>
          _prefWidth = Math.max(_prefWidth, child.width)
          _prefHeight = Math.max(_prefHeight, child.height)
          _minWidth = Math.max(_minWidth, child.width)
          _minHeight = Math.max(_minHeight, child.height)
          childMaxWidth = 0
          childMaxHeight = 0
      }
      if (childMaxWidth > 0) _maxWidth = if (_maxWidth == 0) childMaxWidth else Math.min(_maxWidth, childMaxWidth)
      if (childMaxHeight > 0) _maxHeight = if (_maxHeight == 0) childMaxHeight else Math.min(_maxHeight, childMaxHeight)
      i += 1
    }
  }

  def add(actor: Actor): Unit =
    addActor(actor)

  override def layout(): Unit = {
    if (sizeInvalid) computeSize()
    val width    = this.width
    val height   = this.height
    val children = getChildren
    var i        = 0
    val n        = children.size
    while (i < n) {
      val child = children(i)
      child.setBounds(0, 0, width, height)
      child match {
        case layout: Layout => layout.validate()
        case _ =>
      }
      i += 1
    }
  }

  override def getPrefWidth: Float = {
    if (sizeInvalid) computeSize()
    _prefWidth
  }

  override def getPrefHeight: Float = {
    if (sizeInvalid) computeSize()
    _prefHeight
  }

  override def getMinWidth: Float = {
    if (sizeInvalid) computeSize()
    _minWidth
  }

  override def getMinHeight: Float = {
    if (sizeInvalid) computeSize()
    _minHeight
  }

  override def getMaxWidth: Float = {
    if (sizeInvalid) computeSize()
    _maxWidth
  }

  override def getMaxHeight: Float = {
    if (sizeInvalid) computeSize()
    _maxHeight
  }
}
