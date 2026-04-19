/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 26
 * Covenant-baseline-methods: NodeResolver,get,nodeMap,put
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package shared
package scene

import scala.collection.mutable.HashMap
import sge.graphics.g3d.model.Node
import sge.utils.Nullable

class NodeResolver {

  private val nodeMap: HashMap[Int, Node] = HashMap.empty

  def get(index: Int): Nullable[Node] =
    Nullable.fromOption(nodeMap.get(index))

  def put(index: Int, node: Node): Unit =
    nodeMap.put(index, node)
}
