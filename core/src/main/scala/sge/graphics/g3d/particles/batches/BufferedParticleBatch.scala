/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/BufferedParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package batches

import sge.graphics.Camera
import sge.graphics.g3d.particles.ParticleSorter
import sge.graphics.g3d.particles.renderers.ParticleControllerRenderData
import sge.utils.DynamicArray

/** Base class of all the batches requiring to buffer {@link ParticleControllerRenderData}
  * @author
  *   Inferno
  */
abstract class BufferedParticleBatch[T <: ParticleControllerRenderData] extends ParticleBatch[T] {
  protected var renderData:             DynamicArray[T] = DynamicArray[ParticleControllerRenderData](10).asInstanceOf[DynamicArray[T]]
  protected var bufferedParticlesCount: Int             = 0
  protected var currentCapacity:        Int             = 0
  protected var sorter:                 ParticleSorter  = new ParticleSorter.Distance()
  protected var camera:                 Camera          = scala.compiletime.uninitialized

  override def begin(): Unit = {
    renderData.clear()
    bufferedParticlesCount = 0
  }

  override def draw(data: T): Unit =
    if (data.controller.particles.size > 0) {
      renderData.add(data)
      bufferedParticlesCount += data.controller.particles.size
    }

  override def end(): Unit =
    if (bufferedParticlesCount > 0) {
      ensureCapacity(bufferedParticlesCount)
      flush(sorter.sort(renderData))
    }

  /** Ensure the batch can contain the passed in amount of particles */
  def ensureCapacity(capacity: Int): Unit =
    if (currentCapacity >= capacity) {
      // already big enough
    } else {
      sorter.ensureCapacity(capacity)
      allocParticlesData(capacity)
      currentCapacity = capacity
    }

  def resetCapacity(): Unit = {
    currentCapacity = 0
    bufferedParticlesCount = 0
  }

  protected def allocParticlesData(capacity: Int): Unit

  def setCamera(camera: Camera): Unit = {
    this.camera = camera
    sorter.setCamera(camera)
  }

  def getSorter(): ParticleSorter =
    sorter

  def setSorter(sorter: ParticleSorter): Unit = {
    this.sorter = sorter
    sorter.setCamera(camera)
    sorter.ensureCapacity(currentCapacity)
  }

  /** Sends the data to the gpu. This method must use the calculated offsets to build the particles meshes. The offsets represent the position at which a particle should be placed into the vertex
    * array.
    * @param offsets
    *   the calculated offsets
    */
  protected def flush(offsets: Array[Int]): Unit

  def getBufferedCount(): Int =
    bufferedParticlesCount
}
