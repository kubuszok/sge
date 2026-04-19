/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/Cullable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface -> Scala trait
 * - @Null Rectangle -> Nullable[Rectangle]
 * - Faithful port, no API changes
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: Cullable,setCullingArea
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/Cullable.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package utils

import sge.utils.Nullable
import sge.math.Rectangle

/** Allows a parent to set the area that is visible on a child actor to allow the child to cull when drawing itself. This must only be used for actors that are not rotated or scaled.
  * @author
  *   Nathan Sweet
  */
trait Cullable {

  /** @param cullingArea The culling area in the child actor's coordinates. */
  def setCullingArea(cullingArea: Nullable[Rectangle]): Unit
}
