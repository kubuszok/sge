/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBRDepthShaderProvider.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package shaders

import sge.Sge
import sge.gltf.scene3d.attributes.PBRVertexAttributes
import sge.graphics.g3d.{ Renderable, Shader }
import sge.graphics.g3d.shaders.DepthShader
import sge.graphics.g3d.utils.DepthShaderProvider
import sge.utils.Nullable

import scala.language.implicitConversions

class PBRDepthShaderProvider(config: DepthShader.Config)(using Sge) extends DepthShaderProvider(config) {

  if (config.vertexShader.isEmpty) config.vertexShader = Nullable(PBRDepthShaderProvider.getDefaultVertexShader())
  if (config.fragmentShader.isEmpty) config.fragmentShader = Nullable(PBRDepthShaderProvider.getDefaultFragmentShader())

  protected def morphTargetsPrefix(renderable: Renderable): String = {
    val sb               = new StringBuilder
    val vertexAttributes = renderable.meshPart.mesh.vertexAttributes
    val n                = vertexAttributes.size
    var j                = 0
    while (j < n) {
      val att = vertexAttributes.get(j)
      var i   = 0
      while (i < PBRCommon.MAX_MORPH_TARGETS) {
        if (att.usage == PBRVertexAttributes.Usage.PositionTarget && att.unit == i) {
          sb.append("#define position").append(i).append("Flag\n")
        }
        i += 1
      }
      j += 1
    }
    sb.toString
  }

  override protected def createShader(renderable: Renderable): Shader = {
    PBRCommon.checkVertexAttributes(renderable)
    PBRDepthShader(renderable, config, DepthShader.createPrefix(renderable, config) + morphTargetsPrefix(renderable))
  }
}

object PBRDepthShaderProvider {

  @volatile private var defaultVertexShader: String = null.asInstanceOf[String] // @nowarn — lazy init

  def getDefaultVertexShader()(using sge: Sge): String = {
    if (defaultVertexShader == null) { // @nowarn — null check for lazy init
      defaultVertexShader = sge.files.classpath("net/mgsx/gltf/shaders/depth.vs.glsl").readString()
    }
    defaultVertexShader
  }

  @volatile private var defaultFragmentShader: String = null.asInstanceOf[String] // @nowarn — lazy init

  def getDefaultFragmentShader()(using sge: Sge): String = {
    if (defaultFragmentShader == null) { // @nowarn — null check for lazy init
      defaultFragmentShader = sge.files.classpath("net/mgsx/gltf/shaders/depth.fs.glsl").readString()
    }
    defaultFragmentShader
  }

  def createDefaultConfig()(using Sge): DepthShader.Config = {
    val config = DepthShader.Config()
    config.vertexShader = Nullable(getDefaultVertexShader())
    config.fragmentShader = Nullable(getDefaultFragmentShader())
    config
  }
}
