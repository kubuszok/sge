/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/MoveByAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.moveBy -> target.foreach(_.moveBy(...))
 *   TODO: Java-style getters/setters -- getAmountX/setAmountX, getAmountY/setAmountY
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Moves an actor to a relative position.
  * @author
  *   Nathan Sweet
  */
class MoveByAction extends RelativeTemporalAction {
  private var amountX: Float = 0
  private var amountY: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.moveBy(amountX * percentDelta, amountY * percentDelta))

  def setAmount(x: Float, y: Float): Unit = { amountX = x; amountY = y }

  def getAmountX: Float = amountX

  def setAmountX(x: Float): Unit = this.amountX = x

  def getAmountY: Float = amountY

  def setAmountY(y: Float): Unit = this.amountY = y
}
