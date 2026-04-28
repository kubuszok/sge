/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RelativeTemporalAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: split packages; braces on class
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: RelativeTemporalAction,begin,lastPercent,update,updateRelative
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/RelativeTemporalAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

/** Base class for actions that transition over time using the percent complete since the last frame.
  * @author
  *   Nathan Sweet
  */
abstract class RelativeTemporalAction extends TemporalAction {
  private var lastPercent: Float = 0

  override protected def begin(): Unit =
    lastPercent = 0

  override protected def update(percent: Float): Unit = {
    updateRelative(percent - lastPercent)
    lastPercent = percent
  }

  protected def updateRelative(percentDelta: Float): Unit
}
