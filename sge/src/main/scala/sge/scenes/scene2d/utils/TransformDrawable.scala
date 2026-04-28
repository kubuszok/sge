/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/TransformDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface -> Scala trait
 * - Faithful port, no API changes
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: TransformDrawable,draw
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/TransformDrawable.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.g2d.Batch

/** A drawable that supports scale and rotation. */
trait TransformDrawable extends Drawable {
  def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit
}
