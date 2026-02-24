/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RelativeTemporalAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
