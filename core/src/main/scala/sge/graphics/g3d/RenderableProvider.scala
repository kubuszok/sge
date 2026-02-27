/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/RenderableProvider.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d

import sge.utils.DynamicArray

/** Returns a list of [[Renderable]] instances to be rendered by a ModelBatch.
  * @author
  *   badlogic
  */
trait RenderableProvider {

  /** Returns [[Renderable]] instances. Renderables are obtained from the provided [[sge.utils.Pool]] and added to the provided array. The Renderables obtained using [[sge.utils.Pool.obtain]] will
    * later be put back into the pool, do not store them internally. The resulting array can be rendered via a ModelBatch.
    * @param renderables
    *   the output array
    * @param pool
    *   the pool to obtain Renderables from
    */
  def getRenderables(renderables: DynamicArray[Renderable], pool: sge.utils.Pool[Renderable]): Unit
}
