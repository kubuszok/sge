/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/NodePlus.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Node hack to store morph targets weights
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 44
 * Covenant-baseline-methods: NodePlus,copy,morphTargetNames,result,set,weights
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package model

import sge.graphics.g3d.model.Node
import sge.utils.{ DynamicArray, Nullable }

class NodePlus extends Node {

  /** null if no morph targets */
  var weights: Nullable[WeightVector] = Nullable.empty

  /** optional morph target names (eg. exported from Blender with custom properties enabled). shared with others nodes with same mesh.
    */
  var morphTargetNames: Nullable[DynamicArray[String]] = Nullable.empty

  override def copy(): Node = {
    val result = NodePlus()
    result.set(this)
    result
  }

  override protected def set(other: Node): Node = {
    other match {
      case np: NodePlus =>
        np.weights.foreach { w =>
          weights = Nullable(w.cpy())
          morphTargetNames = np.morphTargetNames
        }
      case _ => ()
    }
    super.set(other)
  }
}
