/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/BufferedParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java Array<T> → DynamicArray[T]; Class<T>/ArraySupplier constructors collapsed to no-arg
 * - Deprecated Class<T> constructor removed (erased generics not needed with DynamicArray)
 * - `return` in ensureCapacity replaced with if/else guard (no-return convention)
 * - camera: uninitialized (Java null) → scala.compiletime.uninitialized
 * - All public methods faithfully ported: begin, draw, end, ensureCapacity, resetCapacity,
 *   camera (property), sorter (property), bufferedCount
 * - Fixes (2026-03-04): setCamera → camera_=; getSorter/setSorter → sorter/sorter_=;
 *   getBufferedCount → bufferedCount; renamed backing fields to _camera/_sorter
 * - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 100
 * Covenant-baseline-methods: BufferedParticleBatch,_camera,_sorter,allocParticlesData,begin,bufferedCount,bufferedParticlesCount,camera,camera_,currentCapacity,draw,end,ensureCapacity,flush,renderData,resetCapacity,sorter,sorter_
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/batches/BufferedParticleBatch.java
 * Covenant-verified: 2026-04-19
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
  protected var _sorter:                ParticleSorter  = ParticleSorter.Distance()
  protected var _camera:                Camera          = scala.compiletime.uninitialized

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
      flush(_sorter.sort(renderData))
    }

  /** Ensure the batch can contain the passed in amount of particles */
  def ensureCapacity(capacity: Int): Unit =
    if (currentCapacity >= capacity) {
      // already big enough
    } else {
      _sorter.ensureCapacity(capacity)
      allocParticlesData(capacity)
      currentCapacity = capacity
    }

  def resetCapacity(): Unit = {
    currentCapacity = 0
    bufferedParticlesCount = 0
  }

  protected def allocParticlesData(capacity: Int): Unit

  def camera: Camera = _camera

  def camera_=(camera: Camera): Unit = {
    this._camera = camera
    _sorter.camera = camera
  }

  def sorter: ParticleSorter = _sorter

  def sorter_=(sorter: ParticleSorter): Unit = {
    this._sorter = sorter
    sorter.camera = _camera
    sorter.ensureCapacity(currentCapacity)
  }

  /** Sends the data to the gpu. This method must use the calculated offsets to build the particles meshes. The offsets represent the position at which a particle should be placed into the vertex
    * array.
    * @param offsets
    *   the calculated offsets
    */
  protected def flush(offsets: Array[Int]): Unit

  def bufferedCount: Int =
    bufferedParticlesCount
}
