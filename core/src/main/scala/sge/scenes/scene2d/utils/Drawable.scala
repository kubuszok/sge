/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/Drawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface with default methods -> Scala trait with def implementations
 * - All methods faithfully ported
 * - TODO: Java-style getters/setters — trait methods: getLeftWidth/setLeftWidth, getRightWidth/setRightWidth, getTopHeight/setTopHeight, getBottomHeight/setBottomHeight, getMinWidth/setMinWidth, getMinHeight/setMinHeight
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.g2d.Batch

/** A drawable knows how to draw itself at a given rectangular size. It provides padding sizes and a minimum size so that other code can determine how to size and position content.
  * @author
  *   Nathan Sweet
  */
trait Drawable {

  /** Draws this drawable at the specified bounds. The drawable should be tinted with {@link Batch#getColor()}, possibly by mixing its own color.
    */
  def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit

  def getLeftWidth: Float

  def setLeftWidth(leftWidth: Float): Unit

  def getRightWidth: Float

  def setRightWidth(rightWidth: Float): Unit

  def getTopHeight: Float

  def setTopHeight(topHeight: Float): Unit

  def getBottomHeight: Float

  def setBottomHeight(bottomHeight: Float): Unit

  def setPadding(topHeight: Float, leftWidth: Float, bottomHeight: Float, rightWidth: Float): Unit = {
    setTopHeight(topHeight)
    setLeftWidth(leftWidth)
    setBottomHeight(bottomHeight)
    setRightWidth(rightWidth)
  }

  def setPadding(padding: Float): Unit =
    setPadding(padding, padding, padding, padding)

  def setPadding(from: Drawable): Unit =
    setPadding(from.getTopHeight, from.getLeftWidth, from.getBottomHeight, from.getRightWidth)

  def getMinWidth: Float

  def setMinWidth(minWidth: Float): Unit

  def getMinHeight: Float

  def setMinHeight(minHeight: Float): Unit

  def setMinSize(minWidth: Float, minHeight: Float): Unit = {
    setMinWidth(minWidth)
    setMinHeight(minHeight)
  }
}
