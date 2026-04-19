/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/ScalingViewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Scaling is a SAM trait (not a Java enum)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * --- AUDIT (2026-03-03) ---
 * API-complete: YES — all methods ported (update, getScaling, setScaling)
 * Behavioural parity: YES — update logic identical, centering calculation matches
 * Conventions: OK — no return, no null, split packages, braces on multiline defs
 * Notes:
 *   - currentScaling backing field pattern used correctly (constructor param + private var)
 *   - Scaling is a SAM trait (not Java enum); apply() invocation matches
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 62
 * Covenant-baseline-methods: ScalingViewport,currentScaling,scaled,sh,sw,this,update,viewportHeight,viewportWidth
 * Covenant-source-reference: com/badlogic/gdx/utils/viewport/ScalingViewport.java
 * Covenant-verified: 2026-04-19
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera
import sge.utils.Scaling

/** A viewport that scales the world using {@link Scaling} . <p> {@link Scaling#fit} keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the
  * remaining space. <p> {@link Scaling#fill} keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off screen). <p> {@link Scaling#stretch} does not keep
  * the aspect ratio, the world is scaled to take the whole screen. <p> {@link Scaling#none} keeps the aspect ratio by using a fixed size world (the world may not fill the screen or some of the world
  * may be off screen).
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class ScalingViewport(scaling: Scaling, initialWorldWidth: WorldUnits, initialWorldHeight: WorldUnits, camera: Camera)(using Sge) extends Viewport {
  var currentScaling: Scaling = scaling

  setWorldSize(initialWorldWidth, initialWorldHeight)
  this.camera = camera

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(scaling: Scaling, worldWidth: WorldUnits, worldHeight: WorldUnits)(using Sge) =
    this(scaling, worldWidth, worldHeight, OrthographicCamera())

  override def update(screenWidth: Pixels, screenHeight: Pixels, centerCamera: Boolean): Unit = {
    val sw             = screenWidth.toInt
    val sh             = screenHeight.toInt
    val scaled         = currentScaling.apply(worldWidth.toFloat, worldHeight.toFloat, sw.toFloat, sh.toFloat)
    val viewportWidth  = Math.round(scaled.x)
    val viewportHeight = Math.round(scaled.y)

    // Center.
    setScreenBounds(Pixels((sw - viewportWidth) / 2), Pixels((sh - viewportHeight) / 2), Pixels(viewportWidth), Pixels(viewportHeight))

    apply(centerCamera)
  }

}
