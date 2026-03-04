/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/FitViewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * --- AUDIT (2026-03-03) ---
 * API-complete: YES — both constructors ported
 * Behavioural parity: YES — delegates to ScalingViewport(Scaling.fit, ...)
 * Conventions: OK — no return, no null, split packages
 * Fix applied: removed trailing semicolons from imports
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera

/** A ScalingViewport that uses {@link Scaling#fit} so it keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the remaining space.
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class FitViewport(worldWidth: Float, worldHeight: Float, camera: Camera)(using Sge) extends ScalingViewport(Scaling.fit, worldWidth, worldHeight, camera) {

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(worldWidth: Float, worldHeight: Float)(using Sge) = {
    this(worldWidth, worldHeight, new OrthographicCamera())
  }
}
