/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/WeightVector.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 91
 * Covenant-baseline-methods: WeightVector,count,cpy,get,i,lerp,mulAdd,sb,scl,set,this,toString,values
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package model

import sge.math.MathUtils
import sge.utils.SgeError

class WeightVector(
  var count:  Int,
  var values: Array[Float]
) {

  def this() =
    this(0, new Array[Float](8))

  def this(count: Int) =
    this(count, new Array[Float](Math.max(count, 8)))

  def this(count: Int, max: Int) =
    this(count, new Array[Float](Math.max(count, max)))

  def set(weights: WeightVector): WeightVector = {
    if (weights.count > values.length) {
      values = new Array[Float](weights.count)
    }
    this.count = weights.count
    var i = 0
    while (i < weights.values.length) {
      values(i) = weights.values(i)
      i += 1
    }
    this
  }

  def set(): WeightVector = {
    this.count = 0
    this
  }

  def lerp(value: WeightVector, t: Float): Unit = {
    if (count != value.count) throw SgeError.InvalidInput("WeightVector count mismatch")
    var i = 0
    while (i < count) {
      values(i) = MathUtils.lerp(values(i), value.values(i), t)
      i += 1
    }
  }

  def get(index: Int): Float =
    values(index)

  def cpy(): WeightVector =
    WeightVector(count, values.length).set(this)

  def scl(s: Float): WeightVector = {
    var i = 0
    while (i < count) {
      values(i) *= s
      i += 1
    }
    this
  }

  def mulAdd(w: WeightVector, s: Float): WeightVector = {
    var i = 0
    while (i < count) {
      values(i) += w.values(i) * s
      i += 1
    }
    this
  }

  override def toString: String = {
    val sb = new StringBuilder("WeightVector(")
    var i  = 0
    while (i < count) {
      if (i > 0) sb.append(", ")
      sb.append(values(i))
      i += 1
    }
    sb.append(")").toString
  }
}
