/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBREmissiveShaderProvider.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package shaders

import sge.Sge
import sge.gltf.scene3d.attributes.PBRColorAttribute
import sge.graphics.g3d.{ Renderable, Shader }
import sge.graphics.g3d.attributes.{ BlendingAttribute, FloatAttribute, TextureAttribute }
import sge.utils.{ Nullable, SgeError }

class PBREmissiveShaderProvider(config: PBRShaderConfig)(using Sge) extends PBRShaderProvider(config) {

  override protected def createShader(renderable: Renderable): Shader = {
    val cfg = this.config.asInstanceOf[PBRShaderConfig]

    val mat = renderable.material.get
    val hasAlpha = mat.has(BlendingAttribute.Type) || mat.has(FloatAttribute.AlphaTest)

    val sb = new StringBuilder(createPrefixBase(renderable, cfg))
    sb.append(PBRShaderProvider.morphTargetsPrefix(renderable))
    sb.append(createPrefixSRGB(renderable, cfg))

    if (mat.has(PBRColorAttribute.BaseColorFactor)) {
      sb.append("#define baseColorFactorFlag\n")
    }

    var maxUVIndex = 0
    mat.getAs[TextureAttribute](TextureAttribute.Emissive).foreach { attribute =>
      sb.append("#define v_emissiveUV v_texCoord").append(attribute.uvIndex).append("\n")
      maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex)
    }
    if (hasAlpha) {
      mat.getAs[TextureAttribute](TextureAttribute.Diffuse).foreach { attribute =>
        sb.append("#define v_diffuseUV v_texCoord").append(attribute.uvIndex).append("\n")
        maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex)
      }
    }

    if (maxUVIndex >= 0) sb.append("#define textureFlag\n")
    if (maxUVIndex == 1) sb.append("#define textureCoord1Flag\n")
    else if (maxUVIndex > 1) throw SgeError.InvalidInput("more than 2 texture coordinates attribute not supported")

    val shader = PBRShader(renderable, cfg, sb.toString)
    checkShaderCompilation(shader.program.get)
    if (!shader.canRender(renderable)) throw SgeError.InvalidInput("cannot render with this shader")
    shader
  }
}

object PBREmissiveShaderProvider {

  def createConfig(maxBones: Int)(using sge: Sge): PBRShaderConfig = {
    val config = PBRShaderProvider.createDefaultConfig()
    config.numBones = maxBones
    config.vertexShader = Nullable(sge.files.classpath("net/mgsx/gltf/shaders/gdx-pbr.vs.glsl").readString())
    config.fragmentShader = Nullable(sge.files.classpath("net/mgsx/gltf/shaders/emissive-only.fs.glsl").readString())
    config
  }
}
