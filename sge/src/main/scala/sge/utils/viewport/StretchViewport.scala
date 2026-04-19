/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/StretchViewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * --- AUDIT (2026-03-03) ---
 * API-complete: YES — both constructors ported
 * Behavioural parity: YES — delegates to ScalingViewport(Scaling.stretch, ...)
 * Conventions: OK — no return, no null, split packages
 * Fix applied: removed trailing semicolons from imports
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: StretchViewport,this
 * Covenant-source-reference: com/badlogic/gdx/utils/viewport/StretchViewport.java
 * Covenant-verified: 2026-04-19
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera

/** A ScalingViewport that uses {@link Scaling#stretch} so it does not keep the aspect ratio, the world is scaled to take the whole screen.
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class StretchViewport(worldWidth: WorldUnits, worldHeight: WorldUnits, camera: Camera)(using Sge) extends ScalingViewport(Scaling.stretch, worldWidth, worldHeight, camera) {

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(worldWidth: WorldUnits, worldHeight: WorldUnits)(using Sge) =
    this(worldWidth, worldHeight, OrthographicCamera())
}
