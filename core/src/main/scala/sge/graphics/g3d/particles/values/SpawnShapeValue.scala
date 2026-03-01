/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/SpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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

  var xOffsetValue: RangedNumericValue = new RangedNumericValue()
  var yOffsetValue: RangedNumericValue = new RangedNumericValue()
  var zOffsetValue: RangedNumericValue = new RangedNumericValue()

  def this(spawnShapeValue: SpawnShapeValue) = {
    this()
  }

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
