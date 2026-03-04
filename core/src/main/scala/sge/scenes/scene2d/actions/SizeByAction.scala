/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/SizeByAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.sizeBy -> target.foreach(_.sizeBy(...))
 *   TODO: Java-style getters/setters -- getAmountWidth/setAmountWidth, getAmountHeight/setAmountHeight
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Moves an actor from its current size to a relative size.
  * @author
  *   Nathan Sweet
  */
class SizeByAction extends RelativeTemporalAction {
  private var amountWidth:  Float = 0
  private var amountHeight: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.sizeBy(amountWidth * percentDelta, amountHeight * percentDelta))

  def setAmount(width: Float, height: Float): Unit = { amountWidth = width; amountHeight = height }

  def getAmountWidth: Float = amountWidth

  def setAmountWidth(width: Float): Unit = amountWidth = width

  def getAmountHeight: Float = amountHeight

  def setAmountHeight(height: Float): Unit = amountHeight = height
}
