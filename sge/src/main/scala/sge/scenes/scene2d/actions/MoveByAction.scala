/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/MoveByAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.moveBy -> target.foreach(_.moveBy(...))
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
  var amountX: Float = 0
  var amountY: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.moveBy(amountX * percentDelta, amountY * percentDelta))

  def setAmount(x: Float, y: Float): Unit = { amountX = x; amountY = y }
}
