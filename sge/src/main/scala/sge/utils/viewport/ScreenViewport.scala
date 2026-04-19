/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/ScreenViewport.java
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
 * API-complete: YES — both constructors, update, getUnitsPerPixel, setUnitsPerPixel
 * Behavioural parity: YES — update logic identical
 * Conventions: OK — no return, no null, split packages, braces on multiline defs
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 48
 * Covenant-baseline-methods: ScreenViewport,this,unitsPerPixel,update
 * Covenant-source-reference: com/badlogic/gdx/utils/viewport/ScreenViewport.java
 * Covenant-verified: 2026-04-19
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera

/** A viewport where the world size is based on the size of the screen. By default 1 world unit == 1 screen pixel, but this ratio can be {@link #setUnitsPerPixel(float) changed} .
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class ScreenViewport(camera: Camera)(using Sge) extends Viewport {

  /** The number of pixels for each world unit. Eg, a scale of 2.5 means there are 2.5 world units for every 1 screen pixel. Default is 1. */
  var unitsPerPixel: Float = 1

  this.camera = camera

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this()(using Sge) =
    this(OrthographicCamera())

  override def update(screenWidth: Pixels, screenHeight: Pixels, centerCamera: Boolean): Unit = {
    setScreenBounds(Pixels.zero, Pixels.zero, screenWidth, screenHeight)
    setWorldSize(WorldUnits(screenWidth.toInt * unitsPerPixel), WorldUnits(screenHeight.toInt * unitsPerPixel))
    apply(centerCamera)
  }

}
