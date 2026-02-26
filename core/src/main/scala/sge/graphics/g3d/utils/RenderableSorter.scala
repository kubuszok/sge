/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/RenderableSorter.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import scala.collection.mutable.ArrayBuffer

/** Responsible for sorting [[Renderable]] lists by whatever criteria (material, distance to camera, etc.)
  * @author
  *   badlogic
  */
trait RenderableSorter {

  /** Sorts the array of [[Renderable]] instances based on some criteria, e.g. material, distance to camera etc.
    * @param camera
    *   the camera to use for sorting
    * @param renderables
    *   the array of renderables to be sorted
    */
  def sort(camera: Camera, renderables: ArrayBuffer[Renderable]): Unit
}
