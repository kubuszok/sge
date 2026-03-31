/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBRDepthShader.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package shaders

import sge.Sge
import sge.gltf.scene3d.attributes.{ PBRTextureAttribute, PBRVertexAttributes }
import sge.gltf.scene3d.model.WeightVector
import sge.graphics.UniformLocation
import sge.graphics.g3d.{ Attributes, Renderable }
import sge.graphics.g3d.shaders.DepthShader
import sge.math.Matrix3

class PBRDepthShader(
  renderable: Renderable,
  config:     DepthShader.Config,
  prefix:     String
)(using Sge) extends DepthShader(renderable, config, prefix) {

  val morphTargetsMask: Long = computeMorphTargetsMask(renderable)

  private var u_morphTargets1:     UniformLocation = UniformLocation.notFound
  private var u_morphTargets2:     UniformLocation = UniformLocation.notFound
  private var u_texCoordTransform: UniformLocation = UniformLocation.notFound

  protected def computeMorphTargetsMask(renderable: Renderable): Long = {
    var morphTargetsFlag = 0
    val vertexAttributes = renderable.meshPart.mesh.vertexAttributes
    val n                = vertexAttributes.size
    var i                = 0
    while (i < n) {
      val attr = vertexAttributes.get(i)
      if (attr.usage == PBRVertexAttributes.Usage.PositionTarget) morphTargetsFlag |= (1 << attr.unit)
      i += 1
    }
    morphTargetsFlag.toLong
  }

  override def canRender(renderable: Renderable): Boolean = {
    if (this.morphTargetsMask != computeMorphTargetsMask(renderable)) return false // @nowarn
    super.canRender(renderable)
  }

  override def init(): Unit = {
    super.init()
    program.foreach { p =>
      u_morphTargets1 = p.fetchUniformLocation("u_morphTargets1", false)
      u_morphTargets2 = p.fetchUniformLocation("u_morphTargets2", false)
      u_texCoordTransform = p.fetchUniformLocation("u_texCoordTransform", false)
    }
  }

  override protected def bindMaterial(attributes: Attributes): Unit = {
    super.bindMaterial(attributes)
    if (u_texCoordTransform != UniformLocation.notFound) {
      val attr = attributes.getAs[PBRTextureAttribute](PBRTextureAttribute.BaseColorTexture)
      val textureTransform = Matrix3()
      PBRCommon.setTextureTransform(textureTransform, attr)
      program.foreach(_.setUniformMatrix(u_texCoordTransform, textureTransform))
    }
  }

  override def render(renderable: Renderable, combinedAttributes: Attributes): Unit = {
    program.foreach { p =>
      if (u_morphTargets1 != UniformLocation.notFound) {
        renderable.userData match {
          case wv: WeightVector =>
            p.setUniformf(u_morphTargets1, wv.get(0), wv.get(1), wv.get(2), wv.get(3))
          case _ =>
            p.setUniformf(u_morphTargets1, 0f, 0f, 0f, 0f)
        }
      }
      if (u_morphTargets2 != UniformLocation.notFound) {
        renderable.userData match {
          case wv: WeightVector =>
            p.setUniformf(u_morphTargets2, wv.get(4), wv.get(5), wv.get(6), wv.get(7))
          case _ =>
            p.setUniformf(u_morphTargets2, 0f, 0f, 0f, 0f)
        }
      }
    }
    super.render(renderable, combinedAttributes)
  }
}
