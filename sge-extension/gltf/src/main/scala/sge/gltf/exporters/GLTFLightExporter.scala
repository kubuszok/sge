/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer

import sge.graphics.g3d.environment.{ BaseLight, DirectionalLight, PointLight, SpotLight }
import sge.graphics.g3d.model.Node
import sge.gltf.data.GLTFExtensions
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.data.extensions.KHRLightsPunctual.{ GLTFLight, GLTFLightNode, GLTFLights, GLTFSpotLight }
import sge.gltf.data.scene.GLTFNode
import sge.gltf.loaders.exceptions.{ GLTFRuntimeException, GLTFUnsupportedException }
import sge.gltf.scene3d.lights.{ DirectionalLightEx, PointLightEx, SpotLightEx }
import sge.utils.{ Nullable, ObjectMap }

private[exporters] class GLTFLightExporter(private val base: GLTFExporter) {

  def exportLights(lights: ObjectMap[Node, BaseLight[?]]): Unit = {
    base.useExtension(KHRLightsPunctual.EXT, true)

    lights.foreachEntry { (key, value) =>
      val nodeID = base.nodeMapping.indexOfByRef(key)
      if (nodeID < 0) throw new GLTFRuntimeException("node not found")
      val glNode = base.root.nodes.get(nodeID)

      if (base.root.extensions.isEmpty) {
        base.root.extensions = Nullable(new GLTFExtensions())
      }
      val extLightsOpt = base.root.extensions.get.get(classOf[GLTFLights], KHRLightsPunctual.EXT)
      val extLights: GLTFLights = extLightsOpt.fold {
        val el = new GLTFLights()
        base.root.extensions.get.set(KHRLightsPunctual.EXT, el)
        el
      }(identity)
      if (extLights.lights.isEmpty) {
        extLights.lights = Nullable(ArrayBuffer[GLTFLight]())
      }
      val glLight = GLTFLightExporter.map(new GLTFLight(), value)
      glLight.name = glNode.name.getOrElse("")
      extLights.lights.get += glLight

      if (glNode.extensions.isEmpty) {
        glNode.extensions = Nullable(new GLTFExtensions())
      }
      val nodeLightOpt = glNode.extensions.get.get(classOf[GLTFLightNode], KHRLightsPunctual.EXT)
      val nodeLight: GLTFLightNode = nodeLightOpt.fold {
        val nl = new GLTFLightNode()
        glNode.extensions.get.set(KHRLightsPunctual.EXT, nl)
        nl
      }(identity)
      nodeLight.light = Nullable(extLights.lights.get.size - 1)
    }
  }
}

private[exporters] object GLTFLightExporter {

  def map(glLight: GLTFLight, light: BaseLight[?]): GLTFLight = {
    var intensityScale: Float = 1f
    light match {
      case dl: DirectionalLight =>
        glLight.`type` = Nullable(GLTFLight.TYPE_DIRECTIONAL)
        dl match {
          case dlx: DirectionalLightEx =>
            glLight.intensity = dlx.intensity
          case _ =>
            glLight.intensity = 1f
        }
        intensityScale = 1f

      case pl: PointLight =>
        glLight.`type` = Nullable(GLTFLight.TYPE_POINT)
        pl match {
          case plx: PointLightEx =>
            glLight.intensity = plx.intensity
            glLight.range = plx.range
          case _ =>
            glLight.intensity = 1f
        }
        intensityScale = 10f

      case sl: SpotLight =>
        glLight.`type` = Nullable(GLTFLight.TYPE_SPOT)
        glLight.spot = Nullable(new GLTFSpotLight())
        sl match {
          case slx: SpotLightEx =>
            glLight.intensity = slx.intensity
            glLight.range = slx.range
          case _ =>
            glLight.intensity = 1f
        }
        intensityScale = 10f
        // https://github.com/KhronosGroup/glTF/blob/master/extensions/2.0/Khronos/KHR_lights_punctual/README.md#inner-and-outer-cone-angles
        // inverse formula
        val cosDeltaAngle = 1f / sl.exponent
        val cosOuterAngle = -sl.cutoffAngle / sl.exponent
        glLight.spot.get.outerConeAngle = Math.acos(cosOuterAngle.toDouble).toFloat
        glLight.spot.get.innerConeAngle = Math.acos((cosOuterAngle + cosDeltaAngle).toDouble).toFloat

      case _ =>
        throw new GLTFUnsupportedException("unsupported light type " + light.getClass)
    }

    // rescale color based on intensity
    val scaledColor = light.color.cpy().mul(1f / glLight.intensity)
    glLight.color = Array(scaledColor.r, scaledColor.g, scaledColor.b)
    glLight.intensity *= intensityScale

    glLight
  }
}
