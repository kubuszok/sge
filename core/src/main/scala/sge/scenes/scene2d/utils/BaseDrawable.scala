/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/BaseDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.g2d.Batch
import sge.utils.Nullable

/** Drawable that stores the size information but doesn't draw anything.
  * @author
  *   Nathan Sweet
  */
class BaseDrawable() extends Drawable {
  private var name:         Nullable[String] = Nullable.empty
  private var leftWidth:    Float            = 0
  private var rightWidth:   Float            = 0
  private var topHeight:    Float            = 0
  private var bottomHeight: Float            = 0
  private var minWidth:     Float            = 0
  private var minHeight:    Float            = 0

  /** Creates a new empty drawable with the same sizing information as the specified drawable. */
  def this(drawable: Drawable) = {
    this()
    drawable match {
      case bd: BaseDrawable => name = bd.getName
      case _ =>
    }
    leftWidth = drawable.getLeftWidth
    rightWidth = drawable.getRightWidth
    topHeight = drawable.getTopHeight
    bottomHeight = drawable.getBottomHeight
    minWidth = drawable.getMinWidth
    minHeight = drawable.getMinHeight
  }

  def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit = {}

  def getLeftWidth: Float = leftWidth

  def setLeftWidth(leftWidth: Float): Unit = this.leftWidth = leftWidth

  def getRightWidth: Float = rightWidth

  def setRightWidth(rightWidth: Float): Unit = this.rightWidth = rightWidth

  def getTopHeight: Float = topHeight

  def setTopHeight(topHeight: Float): Unit = this.topHeight = topHeight

  def getBottomHeight: Float = bottomHeight

  def setBottomHeight(bottomHeight: Float): Unit = this.bottomHeight = bottomHeight

  def getMinWidth: Float = minWidth

  def setMinWidth(minWidth: Float): Unit = this.minWidth = minWidth

  def getMinHeight: Float = minHeight

  def setMinHeight(minHeight: Float): Unit = this.minHeight = minHeight

  def getName: Nullable[String] = name

  def setName(name: Nullable[String]): Unit = this.name = name

  override def toString: String =
    name.getOrElse(getClass.getSimpleName)
}
