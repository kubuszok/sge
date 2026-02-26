/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleSorter.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Camera
import sge.graphics.g3d.particles.renderers.ParticleControllerRenderData
import sge.math.{ Matrix4, Vector3 }

/** This class is used by particle batches to sort the particles before rendering.
  * @author
  *   Inferno
  */
abstract class ParticleSorter {

  protected var camera: Camera = scala.compiletime.uninitialized

  /** @return
    *   an array of offsets where each particle should be put in the resulting mesh (also if more than one mesh will be generated, this is an absolute offset considering a BIG output array).
    */
  def sort[T <: ParticleControllerRenderData](renderData: ArrayBuffer[T]): Array[Int]

  def setCamera(camera: Camera): Unit =
    this.camera = camera

  /** This method is called when the batch has increased the underlying particle buffer. In this way the sorter can increase the data structures used to sort the particles (i.e increase backing array
    * size)
    */
  def ensureCapacity(capacity: Int): Unit = {}
}

object ParticleSorter {
  private val TMP_V1: Vector3 = new Vector3()

  /** Using this class will not apply sorting */
  class None extends ParticleSorter {
    private var currentCapacity: Int        = 0
    private var indices:         Array[Int] = Array.empty

    override def ensureCapacity(capacity: Int): Unit =
      if (currentCapacity < capacity) {
        indices = new Array[Int](capacity)
        var i = 0
        while (i < capacity) {
          indices(i) = i
          i += 1
        }
        currentCapacity = capacity
      }

    override def sort[T <: ParticleControllerRenderData](renderData: ArrayBuffer[T]): Array[Int] =
      indices
  }

  /** This class will sort all the particles using the distance from camera. */
  class Distance extends ParticleSorter {
    private var distances:       Array[Float] = Array.empty
    private var particleIndices: Array[Int]   = Array.empty
    private var particleOffsets: Array[Int]   = Array.empty
    private var currentSize:     Int          = 0

    override def ensureCapacity(capacity: Int): Unit =
      if (currentSize < capacity) {
        distances = new Array[Float](capacity)
        particleIndices = new Array[Int](capacity)
        particleOffsets = new Array[Int](capacity)
        currentSize = capacity
      }

    override def sort[T <: ParticleControllerRenderData](renderData: ArrayBuffer[T]): Array[Int] = {
      val values = camera.view.values
      val cx     = values(Matrix4.M20)
      val cy     = values(Matrix4.M21)
      val cz     = values(Matrix4.M22)
      var count  = 0
      var i      = 0
      for (data <- renderData) {
        var k = 0
        val c = i + data.controller.particles.size
        while (i < c) {
          distances(i) = cx * data.positionChannel.floatData(k + ParticleChannels.XOffset) +
            cy * data.positionChannel.floatData(k + ParticleChannels.YOffset) +
            cz * data.positionChannel.floatData(k + ParticleChannels.ZOffset)
          particleIndices(i) = i
          i += 1
          k += data.positionChannel.strideSize
        }
        count += data.controller.particles.size
      }

      qsort(0, count - 1)

      i = 0
      while (i < count) {
        particleOffsets(particleIndices(i)) = i
        i += 1
      }
      particleOffsets
    }

    def qsort(si: Int, ei: Int): Unit =
      // base case
      if (si < ei) {
        var tmp      = 0f
        var tmpIndex = 0

        // insertion
        if (ei - si <= 8) {
          var i = si
          while (i <= ei) {
            var j = i
            while (j > si && distances(j - 1) > distances(j)) {
              tmp = distances(j)
              distances(j) = distances(j - 1)
              distances(j - 1) = tmp

              // Swap indices
              tmpIndex = particleIndices(j)
              particleIndices(j) = particleIndices(j - 1)
              particleIndices(j - 1) = tmpIndex
              j -= 1
            }
            i += 1
          }
        } else {
          // Quick
          val pivot               = distances(si)
          var i                   = si + 1
          val particlesPivotIndex = particleIndices(si)

          // partition array
          var j = si + 1
          while (j <= ei) {
            if (pivot > distances(j)) {
              if (j > i) {
                // Swap distances
                tmp = distances(j)
                distances(j) = distances(i)
                distances(i) = tmp

                // Swap indices
                tmpIndex = particleIndices(j)
                particleIndices(j) = particleIndices(i)
                particleIndices(i) = tmpIndex
              }
              i += 1
            }
            j += 1
          }

          // put pivot in right position
          distances(si) = distances(i - 1)
          distances(i - 1) = pivot
          particleIndices(si) = particleIndices(i - 1)
          particleIndices(i - 1) = particlesPivotIndex

          // call qsort on right and left sides of pivot
          qsort(si, i - 2)
          qsort(i, ei)
        }
      }
  }
}
