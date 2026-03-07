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
  var amountWidth:  Float = 0
  var amountHeight: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.sizeBy(amountWidth * percentDelta, amountHeight * percentDelta))

  def setAmount(width: Float, height: Float): Unit = { amountWidth = width; amountHeight = height }
}
