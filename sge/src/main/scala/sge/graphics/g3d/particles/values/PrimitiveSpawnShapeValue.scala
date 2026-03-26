/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/PrimitiveSpawnShapeValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: active_= (override), edges (private[particles] var),
 *   spawnWidthValue/spawnHeightValue/spawnDepthValue (public vars), setDimensions, start, load
 * - SpawnSide inner enum ported to Scala 3 enum in companion object
 * - TMP_V1 static field moved to companion object (protected static final -> protected val)
 * - Java `edges` is package-private; Scala version is `private[particles] var edges` -- matching visibility
 * - Java API methods isEdges()/setEdges()/getSpawnWidth()/getSpawnHeight()/getSpawnDepth() retained
 *   as parenthesized methods delegating to the underlying vars, for API parity with LibGDX
 * - Fixes (2026-03-04): override setActive → active_=; .setActive() → .active = ; .relative → .relative
 * - Status: pass
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

  var spawnWidthValue:           ScaledNumericValue = ScaledNumericValue()
  var spawnHeightValue:          ScaledNumericValue = ScaledNumericValue()
  var spawnDepthValue:           ScaledNumericValue = ScaledNumericValue()
  protected var spawnWidth:      Float              = 0f
  protected var spawnWidthDiff:  Float              = 0f
  protected var spawnHeight:     Float              = 0f
  protected var spawnHeightDiff: Float              = 0f
  protected var spawnDepth:      Float              = 0f
  protected var spawnDepthDiff:  Float              = 0f
  private[particles] var edges:  Boolean            = false

  def this(value: PrimitiveSpawnShapeValue) =
    this()
  // Note: super(value) is called implicitly via this() then load pattern

  override def active_=(value: Boolean): Unit = {
    super.active_=(value)
    spawnWidthValue.active = true
    spawnHeightValue.active = true
    spawnDepthValue.active = true
  }

  /** @return whether particles are spawned on the primitive's edge rather than its interior */
  def isEdges: Boolean = edges

  /** Sets whether particles are spawned on the primitive's edge rather than its interior. */
  def setEdges(e: Boolean): Unit = edges = e

  /** @return the width value used for spawn size calculations */
  def getSpawnWidth: ScaledNumericValue = spawnWidthValue

  /** @return the height value used for spawn size calculations */
  def getSpawnHeight: ScaledNumericValue = spawnHeightValue

  /** @return the depth value used for spawn size calculations */
  def getSpawnDepth: ScaledNumericValue = spawnDepthValue

  def setDimensions(width: Float, height: Float, depth: Float): Unit = {
    spawnWidthValue.setHigh(width)
    spawnHeightValue.setHigh(height)
    spawnDepthValue.setHigh(depth)
  }

  override def start(): Unit = {
    spawnWidth = spawnWidthValue.newLowValue()
    spawnWidthDiff = spawnWidthValue.newHighValue()
    if (!spawnWidthValue.relative) spawnWidthDiff -= spawnWidth

    spawnHeight = spawnHeightValue.newLowValue()
    spawnHeightDiff = spawnHeightValue.newHighValue()
    if (!spawnHeightValue.relative) spawnHeightDiff -= spawnHeight

    spawnDepth = spawnDepthValue.newLowValue()
    spawnDepthDiff = spawnDepthValue.newHighValue()
    if (!spawnDepthValue.relative) spawnDepthDiff -= spawnDepth
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

  protected val TMP_V1: Vector3 = Vector3()

  enum SpawnSide {
    case both, top, bottom
  }

}
