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
 * NOTE: Build methods (buildEnvMap, buildIrradianceMap, buildRadianceMap) require GL context
 * and throw UnsupportedOperationException until FrameBufferCubemap + ShapeRenderer.rect are ported.
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/utils/IBLBuilder.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - buildEnvMap, buildIrradianceMap, buildRadianceMap throw UnsupportedOperationException
 *   - Requires FrameBufferCubemap side iteration and ShapeRenderer.rect with vertex colors
 *   - Also requires ScreenUtils.getFrameBufferPixmap and FrameBuffer with Pixels
 */
package sge
package gltf
package scene3d
package utils

import sge.Sge
import sge.graphics.{ Color, Cubemap }
import sge.graphics.g3d.environment.DirectionalLight
import sge.graphics.glutils.{ ShaderProgram, ShapeRenderer }
import sge.math.Vector3
import sge.utils.{ DynamicArray, Nullable, SgeError }

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
    sge.files.classpath("net/mgsx/gltf/shaders/ibl-sun.fs.glsl")
  )
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

  /** Create an environment map for SceneSkybox.
    *
    * Not yet implemented: requires FrameBufferCubemap side iteration and ShapeRenderer.rect with vertex colors, which depend on GL context being active on the render thread.
    */
  def buildEnvMap(size: Int): Cubemap =
    throw new UnsupportedOperationException(
      "IBLBuilder.buildEnvMap requires GL context — call from render thread. Port pending: FrameBufferCubemap + ShapeRenderer.rect"
    )

  /** Creates an irradiance map for PBRCubemapAttribute.DiffuseEnv.
    *
    * Not yet implemented: requires FrameBufferCubemap side iteration and ShapeRenderer.rect with vertex colors, which depend on GL context being active on the render thread.
    */
  def buildIrradianceMap(size: Int): Cubemap =
    throw new UnsupportedOperationException(
      "IBLBuilder.buildIrradianceMap requires GL context — call from render thread. Port pending: FrameBufferCubemap + ShapeRenderer.rect"
    )

  /** Creates a radiance map for PBRCubemapAttribute.SpecularEnv with mipmaps.
    *
    * Not yet implemented: requires FrameBuffer and ScreenUtils.getFrameBufferPixmap with Pixels, which depend on GL context being active on the render thread.
    */
  def buildRadianceMap(mipMapLevels: Int): Cubemap =
    throw new UnsupportedOperationException(
      "IBLBuilder.buildRadianceMap requires GL context — call from render thread. Port pending: FrameBuffer + ScreenUtils.getFrameBufferPixmap"
    )

  @scala.annotation.nowarn("msg=unused") // placeholder for future port — called by buildEnvMap/buildIrradianceMap
  private def renderGradientForSide(side: Cubemap.CubemapSide, blur: Float): Unit =
    throw new UnsupportedOperationException(
      "IBLBuilder.renderGradientForSide requires GL context — call from render thread. Port pending: ShapeRenderer.rect with vertex colors"
    )

  @scala.annotation.nowarn("msg=unused") // placeholder for future port — called by buildEnvMap/buildIrradianceMap
  private def renderLightsForSide(side: Cubemap.CubemapSide, blured: Boolean): Unit =
    throw new UnsupportedOperationException(
      "IBLBuilder.renderLightsForSide requires GL context — call from render thread. Port pending: GL enable/blend + Light.render"
    )
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
    val ibl   = IBLBuilder()
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

    // Restore when implementing render(): localSunDir, localDir, localUp (Vector3), matrix (Matrix4)

    def render(side: Cubemap.CubemapSide, shapes: ShapeRenderer, shader: ShaderProgram, strength: Float): Unit =
      render(side, shapes, shader, strength, exponent)

    def render(side: Cubemap.CubemapSide, shapes: ShapeRenderer, shader: ShaderProgram, strength: Float, exponent: Float): Unit =
      throw new UnsupportedOperationException(
        "IBLBuilder.Light.render requires GL context — call from render thread. Port pending: ShapeRenderer.rect + shader uniform binding"
      )
  }
}
