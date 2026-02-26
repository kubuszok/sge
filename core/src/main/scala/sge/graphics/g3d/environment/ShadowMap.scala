/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/ShadowMap.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package environment

import sge.graphics.g3d.utils.TextureDescriptor
import sge.math.Matrix4

trait ShadowMap {
  def getProjViewTrans(): Matrix4

  def getDepthMap(): TextureDescriptor[?]
}
