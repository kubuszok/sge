/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/ScaleByAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's scale from its current value to a relative value.
  * @author
  *   Nathan Sweet
  */
class ScaleByAction extends RelativeTemporalAction {
  private var amountX: Float = 0
  private var amountY: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.scaleBy(amountX * percentDelta, amountY * percentDelta))

  def setAmount(x: Float, y: Float): Unit = { amountX = x; amountY = y }

  def setAmount(scale: Float): Unit = { amountX = scale; amountY = scale }

  def getAmountX: Float = amountX

  def setAmountX(x: Float): Unit = this.amountX = x

  def getAmountY: Float = amountY

  def setAmountY(y: Float): Unit = this.amountY = y
}
