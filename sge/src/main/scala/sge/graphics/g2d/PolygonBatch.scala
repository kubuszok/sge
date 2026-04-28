/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PolygonBatch.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait extending Batch
 *   Idiom: boundary/break, Nullable, split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: PolygonBatch,draw
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/PolygonBatch.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 2932c93f5fddfd29dd241fee44282cc9e695ab44
 */
package sge
package graphics
package g2d

/** A PolygonBatch is an extension of the Batch interface that provides additional render methods specifically for rendering polygons.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
trait PolygonBatch extends Batch {

  /** Draws a polygon region with the bottom left corner at x,y having the width and height of the region. */
  def draw(region: PolygonRegion, x: Float, y: Float): Unit

  /** Draws a polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height. */
  def draw(region: PolygonRegion, x: Float, y: Float, width: Float, height: Float): Unit

  /** Draws the polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height. The polygon region is offset by originX, originY relative to the
    * origin. Scale specifies the scaling factor by which the polygon region should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the rectangle
    * around originX, originY.
    */
  def draw(region: PolygonRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit

  /** Draws the polygon using the given vertices and triangles. Each vertices must be made up of 5 elements in this order: x, y, color, u, v.
    */
  def draw(texture: Texture, polygonVertices: Array[Float], verticesOffset: Int, verticesCount: Int, polygonTriangles: Array[Short], trianglesOffset: Int, trianglesCount: Int): Unit
}
