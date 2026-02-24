/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps

import sge.graphics.OrthographicCamera
import sge.math.Matrix4

/** Models a common way of rendering {@link Map} objects */
trait MapRenderer {

  /** Sets the projection matrix and viewbounds from the given camera. If the camera changes, you have to call this method again. The viewbounds are taken from the camera's position and viewport size
    * as well as the scale. This method will only work if the camera's direction vector is (0,0,-1) and its up vector is (0, 1, 0), which are the defaults.
    * @param camera
    *   the {@link OrthographicCamera}
    */
  def setView(camera: OrthographicCamera): Unit

  /** Sets the projection matrix for rendering, as well as the bounds of the map which should be rendered. Make sure that the frustum spanned by the projection matrix coincides with the viewbounds.
    * @param projectionMatrix
    * @param viewboundsX
    * @param viewboundsY
    * @param viewboundsWidth
    * @param viewboundsHeight
    */
  def setView(projectionMatrix: Matrix4, viewboundsX: Float, viewboundsY: Float, viewboundsWidth: Float, viewboundsHeight: Float): Unit

  /** Renders all the layers of a map. */
  def render(): Unit

  /** Renders the given layers of a map.
    *
    * @param layers
    *   the layers to render.
    */
  def render(layers: Array[Int]): Unit
}
