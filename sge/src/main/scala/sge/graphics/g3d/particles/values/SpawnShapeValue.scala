/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/SpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public/abstract methods ported: spawnAux, spawn, init, start, load, copy, save, load(AssetManager)
 * - ResourceData.Configurable trait used instead of separate Json.Serializable interface
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: SpawnShapeValue,copy,init,load,save,shape,spawn,spawnAux,start,this,xOffsetValue,yOffsetValue,zOffsetValue
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/values/SpawnShapeValue.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package values

import sge.assets.AssetManager
import sge.math.Vector3

/** Encapsulate the formulas to spawn a particle on a shape.
  * @author
  *   Inferno
  */
abstract class SpawnShapeValue extends ParticleValue with ResourceData.Configurable {

  var xOffsetValue: RangedNumericValue = RangedNumericValue()
  var yOffsetValue: RangedNumericValue = RangedNumericValue()
  var zOffsetValue: RangedNumericValue = RangedNumericValue()

  def this(spawnShapeValue: SpawnShapeValue) =
    this()

  def spawnAux(vector: Vector3, percent: Float): Unit

  final def spawn(vector: Vector3, percent: Float): Vector3 = {
    spawnAux(vector, percent)
    if (xOffsetValue.active) vector.x += xOffsetValue.newLowValue()
    if (yOffsetValue.active) vector.y += yOffsetValue.newLowValue()
    if (zOffsetValue.active) vector.z += zOffsetValue.newLowValue()
    vector
  }

  def init(): Unit = {}

  def start(): Unit = {}

  override def load(value: ParticleValue): Unit = {
    super.load(value)
    val shape = value.asInstanceOf[SpawnShapeValue]
    xOffsetValue.load(shape.xOffsetValue)
    yOffsetValue.load(shape.yOffsetValue)
    zOffsetValue.load(shape.zOffsetValue)
  }

  def copy(): SpawnShapeValue

  override def save(manager: AssetManager, data: ResourceData[?]): Unit = {}

  override def load(manager: AssetManager, data: ResourceData[?]): Unit = {}

}
