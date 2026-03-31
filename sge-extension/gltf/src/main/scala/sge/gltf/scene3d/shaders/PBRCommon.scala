/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBRCommon.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package shaders

import sge.{ Application, Sge }
import sge.gltf.scene3d.attributes.PBRTextureAttribute
import sge.graphics.{ EnableCap, GL20 }
import sge.graphics.g3d.Renderable
import sge.math.Matrix3
import sge.utils.{ BufferUtils, Nullable, SgeError }

import scala.util.boundary
import scala.util.boundary.break

object PBRCommon {

  val MAX_MORPH_TARGETS: Int = 8

  private val intBuffer = BufferUtils.newIntBuffer(16)

  def getCapability(pname: Int)(using sge: Sge): Int = {
    intBuffer.clear()
    sge.graphics.gl.glGetIntegerv(pname, intBuffer)
    intBuffer.get()
  }

  def checkVertexAttributes(renderable: Renderable)(using sge: Sge): Unit = {
    val numVertexAttributes = renderable.meshPart.mesh.vertexAttributes.size
    val maxVertexAttribs    = getCapability(GL20.GL_MAX_VERTEX_ATTRIBS)
    if (numVertexAttributes > maxVertexAttribs) {
      throw SgeError.InvalidInput("too many vertex attributes : " + numVertexAttributes + " > " + maxVertexAttribs)
    }
  }

  @volatile private var seamlessCubemapsShouldBeEnabled: Nullable[Boolean] = Nullable.empty

  def enableSeamlessCubemaps()(using sge: Sge): Unit = {
    if (seamlessCubemapsShouldBeEnabled.isEmpty) {
      val seamlessCubemapsSupported: Boolean =
        if (sge.application.applicationType == Application.ApplicationType.Desktop) {
          // Cubemaps seamless are partially supported for desktop
          val supported = sge.graphics.glVersion.isVersionEqualToOrHigher(3, 2) ||
            sge.graphics.supportsExtension("GL_ARB_seamless_cube_map")
          seamlessCubemapsShouldBeEnabled = Nullable(supported)
          supported
        } else {
          // Cubemaps seamless supported and always enabled for GLES 3 and WebGL 2.
          val supported = sge.graphics.gl30.isDefined
          seamlessCubemapsShouldBeEnabled = Nullable(false)
          supported
        }
      if (!seamlessCubemapsSupported) {
        System.err.println("[PBR] Warning seamless CubeMap is not supported by this platform and may cause filtering artifacts")
      }
    }
    seamlessCubemapsShouldBeEnabled.foreach { enabled =>
      if (enabled) {
        val GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884f // from GL32
        sge.graphics.gl.glEnable(EnableCap(GL_TEXTURE_CUBE_MAP_SEAMLESS))
      }
    }
  }

  def setTextureTransform(transform: Matrix3, attribute: Nullable[PBRTextureAttribute]): Unit =
    attribute.fold {
      transform.idt()
    } { attr =>
      transform.idt()
      transform.translate(attr.offsetU, attr.offsetV)
      transform.rotateRad(-attr.rotationUV)
      transform.scale(attr.scaleU, attr.scaleV)
    }
}
