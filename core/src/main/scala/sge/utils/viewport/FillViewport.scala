/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/FillViewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils
package viewport

import sge.graphics.Camera;
import sge.graphics.OrthographicCamera;

/** A ScalingViewport that uses {@link Scaling#fill} so it keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off screen).
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class FillViewport(worldWidth: Float, worldHeight: Float, camera: Camera)(using sge: Sge) extends ScalingViewport(Scaling.fill, worldWidth, worldHeight, camera) {

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(worldWidth: Float, worldHeight: Float)(using sge: Sge) = {
    this(worldWidth, worldHeight, new OrthographicCamera())
  }
}
