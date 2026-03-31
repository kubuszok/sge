/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/IBLBuilder.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Quick procedural IBL environment generation.
 * Should be closed when no longer used.
 *
 * TODO: Complete port — many ShapeRenderer/FrameBuffer API methods need SGE adaptation
 */
package sge
package gltf
package scene3d
package utils

import sge.Sge
import sge.graphics.{ Color, Cubemap, Pixmap, Texture }
import sge.graphics.g3d.environment.DirectionalLight
import sge.graphics.glutils.{ FrameBuffer, FrameBufferCubemap, ShaderProgram, ShapeRenderer }
import sge.math.{ Matrix4, Vector3 }
import sge.utils.{ DynamicArray, Nullable, ScreenUtils, SgeError }

class IBLBuilder private (using sge: Sge) extends AutoCloseable {

  val nearGroundColor: Color = Color()
  val farGroundColor:  Color = Color()
  val nearSkyColor:    Color = Color()
  val farSkyColor:     Color = Color()

  val lights: DynamicArray[IBLBuilder.Light] = DynamicArray[IBLBuilder.Light]()

  var renderSun:      Boolean = true
  var renderGradient: Boolean = true

  private val sunShader: ShaderProgram = ShaderProgram(
    sge.files.classpath("net/mgsx/gltf/shaders/ibl-sun.vs.glsl"),
    sge.files.classpath("net/mgsx/gltf/shaders/ibl-sun.fs.glsl"))
  if (!sunShader.compiled) throw SgeError.InvalidInput(sunShader.log)

  private val shapes:    ShapeRenderer = ShapeRenderer(20)
  private val sunShapes: ShapeRenderer = ShapeRenderer(20, Nullable(sunShader))

  shapes.projectionMatrix.setToOrtho2D(0f, 0f, 1f, 1f)
  sunShapes.projectionMatrix.setToOrtho2D(0f, 0f, 1f, 1f)

  override def close(): Unit = {
    sunShader.close()
    sunShapes.close()
    shapes.close()
  }

  /** Create an environment map for SceneSkybox */
  def buildEnvMap(size: Int): Cubemap = ??? // TODO: port — requires FrameBufferCubemap side iteration + ShapeRenderer.rect

  /** Creates an irradiance map for PBRCubemapAttribute.DiffuseEnv */
  def buildIrradianceMap(size: Int): Cubemap = ??? // TODO: port — requires FrameBufferCubemap side iteration + ShapeRenderer.rect

  /** Creates a radiance map for PBRCubemapAttribute.SpecularEnv with mipmaps */
  def buildRadianceMap(mipMapLevels: Int): Cubemap = ??? // TODO: port — requires FrameBuffer + ScreenUtils.getFrameBufferPixmap with Pixels

  private def renderGradientForSide(side: Cubemap.CubemapSide, blur: Float): Unit = {
    ??? // TODO: port — requires ShapeRenderer.rect with vertex colors
  }

  private def renderLightsForSide(side: Cubemap.CubemapSide, blured: Boolean): Unit = {
    ??? // TODO: port — requires GL enable/blend constants + Light.render
  }
}

object IBLBuilder {

  def createOutdoor(sun: DirectionalLight)(using Sge): IBLBuilder = {
    val ibl = IBLBuilder()
    ibl.nearGroundColor.set(0.5f, 0.45f, 0.4f, 1f)
    ibl.farGroundColor.set(0.3f, 0.25f, 0.2f, 1f)
    ibl.nearSkyColor.set(0.7f, 0.8f, 1f, 1f)
    ibl.farSkyColor.set(0.9f, 0.95f, 1f, 1f)
    val light = Light()
    light.direction.set(sun.direction).nor()
    light.color.set(sun.color)
    light.exponent = 30f
    ibl.lights.add(light)
    ibl
  }

  def createIndoor(sun: DirectionalLight)(using Sge): IBLBuilder = {
    val ibl  = IBLBuilder()
    val tint = Color(1f, 0.9f, 0.8f, 1f).mul(0.3f)
    ibl.nearGroundColor.set(tint).mul(0.7f)
    ibl.farGroundColor.set(tint)
    ibl.farSkyColor.set(tint)
    ibl.nearSkyColor.set(tint).mul(2f)
    val light = Light()
    light.direction.set(sun.direction).nor()
    light.color.set(1f, 0.5f, 0f, 1f).mul(0.3f)
    light.exponent = 3f
    ibl.lights.add(light)
    ibl
  }

  def createCustom(sun: DirectionalLight)(using Sge): IBLBuilder = {
    val ibl = IBLBuilder()
    val light = Light()
    light.direction.set(sun.direction).nor()
    light.color.set(sun.color)
    light.exponent = 100f
    ibl.lights.add(light)
    ibl
  }

  class Light {
    val color:     Color   = Color(1f, 1f, 1f, 1f)
    val direction: Vector3 = Vector3(0f, -1f, 0f)
    var exponent:  Float   = 30f

    private val localSunDir: Vector3 = Vector3()
    private val localDir:    Vector3 = Vector3()
    private val localUp:     Vector3 = Vector3()
    private val matrix:      Matrix4 = Matrix4()

    def render(side: Cubemap.CubemapSide, shapes: ShapeRenderer, shader: ShaderProgram, strength: Float): Unit = {
      render(side, shapes, shader, strength, exponent)
    }

    def render(side: Cubemap.CubemapSide, shapes: ShapeRenderer, shader: ShaderProgram, strength: Float, exponent: Float): Unit = {
      ??? // TODO: port — requires ShapeRenderer.rect and shader uniform binding
    }
  }
}
