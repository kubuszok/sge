/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/RenderableSorter.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java interface -> Scala trait
 *   - Array<Renderable> -> DynamicArray[Renderable]
 *   - sort(Camera, Array) signature preserved
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: RenderableSorter,sort
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/RenderableSorter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package utils

import sge.utils.{ DynamicArray, Nullable }

/** Responsible for sorting [[Renderable]] lists by whatever criteria (material, distance to camera, etc.)
  * @author
  *   badlogic
  */
trait RenderableSorter {

  /** Sorts the array of [[Renderable]] instances based on some criteria, e.g. material, distance to camera etc.
    * @param camera
    *   the camera to use for sorting, may be empty
    * @param renderables
    *   the array of renderables to be sorted
    */
  def sort(camera: Nullable[Camera], renderables: DynamicArray[Renderable]): Unit
}
