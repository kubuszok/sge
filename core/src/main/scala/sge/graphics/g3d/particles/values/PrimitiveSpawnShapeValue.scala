/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/PrimitiveSpawnShapeValue.java
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

import sge.math.Vector3

/** The base class of all the {@link SpawnShapeValue} values which spawn the particles on a geometric primitive.
  * @author
  *   Inferno
  */
abstract class PrimitiveSpawnShapeValue extends SpawnShapeValue {

  var spawnWidthValue:           ScaledNumericValue = new ScaledNumericValue()
  var spawnHeightValue:          ScaledNumericValue = new ScaledNumericValue()
  var spawnDepthValue:           ScaledNumericValue = new ScaledNumericValue()
  protected var spawnWidth:      Float              = 0f
  protected var spawnWidthDiff:  Float              = 0f
  protected var spawnHeight:     Float              = 0f
  protected var spawnHeightDiff: Float              = 0f
  protected var spawnDepth:      Float              = 0f
  protected var spawnDepthDiff:  Float              = 0f
  var edges:                     Boolean            = false

  def this(value: PrimitiveSpawnShapeValue) = {
    this()
    // Note: super(value) is called implicitly via this() then load pattern
  }

  override def setActive(active: Boolean): Unit = {
    super.setActive(active)
    spawnWidthValue.setActive(true)
    spawnHeightValue.setActive(true)
    spawnDepthValue.setActive(true)
  }

  def isEdges: Boolean = edges

  def setEdges(edges: Boolean): Unit =
    this.edges = edges

  def getSpawnWidth: ScaledNumericValue = spawnWidthValue

  def getSpawnHeight: ScaledNumericValue = spawnHeightValue

  def getSpawnDepth: ScaledNumericValue = spawnDepthValue

  def setDimensions(width: Float, height: Float, depth: Float): Unit = {
    spawnWidthValue.setHigh(width)
    spawnHeightValue.setHigh(height)
    spawnDepthValue.setHigh(depth)
  }

  override def start(): Unit = {
    spawnWidth = spawnWidthValue.newLowValue()
    spawnWidthDiff = spawnWidthValue.newHighValue()
    if (!spawnWidthValue.isRelative()) spawnWidthDiff -= spawnWidth

    spawnHeight = spawnHeightValue.newLowValue()
    spawnHeightDiff = spawnHeightValue.newHighValue()
    if (!spawnHeightValue.isRelative()) spawnHeightDiff -= spawnHeight

    spawnDepth = spawnDepthValue.newLowValue()
    spawnDepthDiff = spawnDepthValue.newHighValue()
    if (!spawnDepthValue.isRelative()) spawnDepthDiff -= spawnDepth
  }

  override def load(value: ParticleValue): Unit = {
    super.load(value)
    val shape = value.asInstanceOf[PrimitiveSpawnShapeValue]
    edges = shape.edges
    spawnWidthValue.load(shape.spawnWidthValue)
    spawnHeightValue.load(shape.spawnHeightValue)
    spawnDepthValue.load(shape.spawnDepthValue)
  }

}

object PrimitiveSpawnShapeValue {

  protected val TMP_V1: Vector3 = new Vector3()

  enum SpawnSide {
    case both, top, bottom
  }

}
