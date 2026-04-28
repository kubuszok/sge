/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/AmbientCubemap.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 13 methods ported: set x4, getColor, clear, clamp, add x5, toString
 *   - 3 constructors: no-arg, array (validated via companion apply), copy
 *   - dst() → distance() (SGE Vector3 rename)
 *   - GdxRuntimeException → SgeError.InvalidInput
 *   - clamp helper preserved as private in companion object
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 177
 * Covenant-baseline-methods: AmbientCubemap,NUM_VALUES,add,apply,arr,clamp,clear,copyArray,d,getColor,i,idx,s,sb,set,t,this,toString,x2,y2,z2
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/environment/AmbientCubemap.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package environment

import sge.math.Vector3
import sge.utils.SgeError

class AmbientCubemap private[environment] (val data: Array[Float]) {

  def this() =
    this(new Array[Float](AmbientCubemap.NUM_VALUES))

  def this(copyFrom: AmbientCubemap) =
    this(AmbientCubemap.copyArray(copyFrom.data))

  def set(values: Array[Float]): AmbientCubemap = {
    var i = 0
    while (i < data.length) {
      data(i) = values(i)
      i += 1
    }
    this
  }

  def set(other: AmbientCubemap): AmbientCubemap =
    set(other.data)

  def set(color: Color): AmbientCubemap =
    set(color.r, color.g, color.b)

  def set(r: Float, g: Float, b: Float): AmbientCubemap = {
    var idx = 0
    while (idx < AmbientCubemap.NUM_VALUES) {
      data(idx) = r
      data(idx + 1) = g
      data(idx + 2) = b
      idx += 3
    }
    this
  }

  def getColor(out: Color, side: Int): Color = {
    val s = side * 3
    out.set(data(s), data(s + 1), data(s + 2), 1f)
  }

  def clear(): AmbientCubemap = {
    var i = 0
    while (i < data.length) {
      data(i) = 0f
      i += 1
    }
    this
  }

  def clamp(): AmbientCubemap = {
    var i = 0
    while (i < data.length) {
      data(i) = AmbientCubemap.clamp(data(i))
      i += 1
    }
    this
  }

  def add(r: Float, g: Float, b: Float): AmbientCubemap = {
    var idx = 0
    while (idx < data.length) {
      data(idx) += r
      idx += 1
      data(idx) += g
      idx += 1
      data(idx) += b
      idx += 1
    }
    this
  }

  def add(color: Color): AmbientCubemap =
    add(color.r, color.g, color.b)

  def add(r: Float, g: Float, b: Float, x: Float, y: Float, z: Float): AmbientCubemap = {
    val x2 = x * x
    val y2 = y * y
    val z2 = z * z
    var d  = x2 + y2 + z2
    if (d == 0f) {
      this
    } else {
      d = 1f / d * (d + 1f)
      val rd  = r * d
      val gd  = g * d
      val bd  = b * d
      var idx = if (x > 0) 0 else 3
      data(idx) += x2 * rd
      data(idx + 1) += x2 * gd
      data(idx + 2) += x2 * bd
      idx = if (y > 0) 6 else 9
      data(idx) += y2 * rd
      data(idx + 1) += y2 * gd
      data(idx + 2) += y2 * bd
      idx = if (z > 0) 12 else 15
      data(idx) += z2 * rd
      data(idx + 1) += z2 * gd
      data(idx + 2) += z2 * bd
      this
    }
  }

  def add(color: Color, direction: Vector3): AmbientCubemap =
    add(color.r, color.g, color.b, direction.x, direction.y, direction.z)

  def add(r: Float, g: Float, b: Float, direction: Vector3): AmbientCubemap =
    add(r, g, b, direction.x, direction.y, direction.z)

  def add(color: Color, x: Float, y: Float, z: Float): AmbientCubemap =
    add(color.r, color.g, color.b, x, y, z)

  def add(color: Color, point: Vector3, target: Vector3): AmbientCubemap =
    add(color.r, color.g, color.b, target.x - point.x, target.y - point.y, target.z - point.z)

  def add(color: Color, point: Vector3, target: Vector3, intensity: Float): AmbientCubemap = {
    val t = intensity / (1f + target.distance(point))
    add(color.r * t, color.g * t, color.b * t, target.x - point.x, target.y - point.y, target.z - point.z)
  }

  override def toString: String = {
    val sb = new StringBuilder
    var i  = 0
    while (i < data.length) {
      sb.append(data(i).toString).append(", ").append(data(i + 1).toString).append(", ").append(data(i + 2).toString).append("\n")
      i += 3
    }
    sb.toString
  }
}

object AmbientCubemap {
  private[environment] val NUM_VALUES = 6 * 3

  /** Creates an AmbientCubemap from an array of float values.
    * @throws SgeError.InvalidInput
    *   if the array size is not 18 (6 * 3)
    */
  def apply(copyFrom: Array[Float]): AmbientCubemap = {
    if (copyFrom.length != NUM_VALUES) throw SgeError.InvalidInput("Incorrect array size")
    val arr = new Array[Float](copyFrom.length)
    System.arraycopy(copyFrom, 0, arr, 0, arr.length)
    AmbientCubemap(arr)
  }

  private[environment] def copyArray(src: Array[Float]): Array[Float] = {
    if (src.length != NUM_VALUES) throw SgeError.InvalidInput("Incorrect array size")
    val arr = new Array[Float](src.length)
    System.arraycopy(src, 0, arr, 0, arr.length)
    arr
  }

  private def clamp(v: Float): Float =
    if (v < 0f) 0f else if (v > 1f) 1f else v
}
