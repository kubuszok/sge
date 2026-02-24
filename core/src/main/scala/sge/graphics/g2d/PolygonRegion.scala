/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PolygonRegion.java
 * Original authors: Stefan Bachmann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

/** Defines a polygon shape on top of a texture region to avoid drawing transparent pixels.
  * @see
  *   PolygonRegionLoader
  *
  * @param region
  *   the region used for drawing
  * @param vertices
  *   contains 2D polygon coordinates in pixels relative to source region
  * @author
  *   Stefan Bachmann
  * @author
  *   Nathan Sweet
  */
class PolygonRegion(region: TextureRegion, vertices: Array[Float], triangles: Array[Short]) {
  val textureCoords = new Array[Float](vertices.length) // texture coordinates in atlas coordinates

  {
    val u        = region.u
    val v        = region.v
    val uvWidth  = region.u2 - u
    val uvHeight = region.v2 - v
    val width    = region.regionWidth
    val height   = region.regionHeight
    for (i <- 0 until vertices.length by 2) {
      textureCoords(i) = u + uvWidth * (vertices(i) / width)
      textureCoords(i + 1) = v + uvHeight * (1 - (vertices(i + 1) / height))
    }
  }

  /** Returns the vertices in local space. */
  def getVertices(): Array[Float] = vertices

  def getTriangles(): Array[Short] = triangles

  def getTextureCoords(): Array[Float] = textureCoords

  def getRegion(): TextureRegion = region
}
