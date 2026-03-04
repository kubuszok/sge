/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RotateByAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.rotateBy -> target.foreach(_.rotateBy(...))
 *   TODO: Java-style getters/setters -- getAmount/setAmount
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's rotation from its current value to a relative value.
  * @author
  *   Nathan Sweet
  */
class RotateByAction extends RelativeTemporalAction {
  private var amount: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.rotateBy(amount * percentDelta))

  def getAmount: Float = amount

  def setAmount(rotationAmount: Float): Unit = amount = rotationAmount
}
