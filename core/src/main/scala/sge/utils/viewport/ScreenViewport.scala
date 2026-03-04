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
 * TODO: Java-style getters/setters — getUnitsPerPixel/setUnitsPerPixel
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
  private var unitsPerPixel: Float = 1

  setCamera(camera)

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this()(using Sge) = {
    this(new OrthographicCamera())
  }

  override def update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean): Unit = {
    setScreenBounds(0, 0, screenWidth, screenHeight)
    setWorldSize(screenWidth * unitsPerPixel, screenHeight * unitsPerPixel)
    apply(centerCamera)
  }

  def getUnitsPerPixel(): Float =
    unitsPerPixel

  /** Sets the number of pixels for each world unit. Eg, a scale of 2.5 means there are 2.5 world units for every 1 screen pixel. Default is 1.
    */
  def setUnitsPerPixel(unitsPerPixel: Float): Unit =
    this.unitsPerPixel = unitsPerPixel
}
