/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/Drawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface with default methods -> Scala trait with def implementations
 * - All methods faithfully ported
 * - Renames: getLeftWidth→leftWidth, setLeftWidth→leftWidth_=, getRightWidth→rightWidth, etc.
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

  def leftWidth: Float

  def leftWidth_=(leftWidth: Float): Unit

  def rightWidth: Float

  def rightWidth_=(rightWidth: Float): Unit

  def topHeight: Float

  def topHeight_=(topHeight: Float): Unit

  def bottomHeight: Float

  def bottomHeight_=(bottomHeight: Float): Unit

  def setPadding(top: Float, left: Float, bottom: Float, right: Float): Unit = {
    topHeight = top
    leftWidth = left
    bottomHeight = bottom
    rightWidth = right
  }

  def setPadding(padding: Float): Unit =
    setPadding(padding, padding, padding, padding)

  def setPadding(from: Drawable): Unit =
    setPadding(from.topHeight, from.leftWidth, from.bottomHeight, from.rightWidth)

  def minWidth: Float

  def minWidth_=(minWidth: Float): Unit

  def minHeight: Float

  def minHeight_=(minHeight: Float): Unit

  def setMinSize(mw: Float, mh: Float): Unit = {
    minWidth = mw
    minHeight = mh
  }
}
