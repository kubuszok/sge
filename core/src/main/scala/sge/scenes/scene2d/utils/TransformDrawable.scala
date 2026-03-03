/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/TransformDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface -> Scala trait
 * - Faithful port, no API changes
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
