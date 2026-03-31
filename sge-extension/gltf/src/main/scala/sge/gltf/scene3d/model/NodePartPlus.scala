/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/NodePartPlus.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * NodePart hack to store morph targets
 */
package sge
package gltf
package scene3d
package model

import sge.graphics.g3d.Renderable
import sge.graphics.g3d.model.NodePart
import sge.utils.Nullable

class NodePartPlus extends NodePart {

  /** null if no morph targets */
  var morphTargets: Nullable[WeightVector] = Nullable.empty

  override def setRenderable(out: Renderable): Renderable = {
    out.material = Nullable(material)
    out.meshPart.set(meshPart)
    out.bones = bones
    out.userData = morphTargets.map(_.asInstanceOf[AnyRef]).getOrElse(null) // @nowarn — Java interop for userData
    out
  }

  override def copy(): NodePart = {
    val result = NodePartPlus()
    result.set(this)
    result
  }

  override protected def set(other: NodePart): NodePart = {
    super.set(other)
    other match {
      case npp: NodePartPlus =>
        morphTargets = npp.morphTargets.map(_.cpy())
      case _ => ()
    }
    this
  }
}
