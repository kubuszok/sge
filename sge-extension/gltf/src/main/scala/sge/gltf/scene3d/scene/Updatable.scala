/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/Updatable.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package scene

import sge.graphics.Camera

trait Updatable {
  def update(camera: Camera, delta: Float): Unit
}
