/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/ScaleByAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.scaleBy -> target.foreach(_.scaleBy(...))
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: ScaleByAction,amountX,amountY,setAmount,updateRelative
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/ScaleByAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
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
  var amountX: Float = 0
  var amountY: Float = 0

  override protected def updateRelative(percentDelta: Float): Unit =
    target.foreach(_.scaleBy(amountX * percentDelta, amountY * percentDelta))

  def setAmount(x: Float, y: Float): Unit = { amountX = x; amountY = y }

  def setAmount(scale: Float): Unit = { amountX = scale; amountY = scale }
}
