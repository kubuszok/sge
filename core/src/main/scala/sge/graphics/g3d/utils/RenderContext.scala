/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/RenderContext.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Gdx.gl -> Sge().graphics.gl (using Sge context)
 *   - All state management methods fully ported
 *   - setDepthTest, setBlending, setCullFace: logic matches Java source
 *   - begin()/end(): state initialization matches Java
 *   - Audit: pass (2026-03-03)
 *   TODO: typed GL enums -- EnableCap, CompareFunc, BlendFactor, CullFace -- see docs/improvements/opaque-types.md
 */
package sge
package graphics
package g3d
package utils

/** Manages OpenGL state and tries to reduce state changes. Uses a [[TextureBinder]] to reduce texture binds as well. Call [[begin]] to setup the context, call [[end]] to undo all state changes. Use
  * the setters to change state, use [[textureBinder]] to bind textures.
  * @author
  *   badlogic, Xoppa
  */
class RenderContext(
  /** used to bind textures * */
  val textureBinder: TextureBinder
)(using Sge) {

  private var blending:               Boolean = false
  private var blendSourceRgbFactor:   Int     = 0
  private var blendDestRgbFactor:     Int     = 0
  private var blendSourceAlphaFactor: Int     = 0
  private var blendDestAlphaFactor:   Int     = 0
  private var depthFunc:              Int     = 0
  private var depthRangeNear:         Float   = 0f
  private var depthRangeFar:          Float   = 0f
  private var depthMask:              Boolean = false
  private var cullFace:               Int     = 0

  /** Sets up the render context, must be matched with a call to [[end]]. */
  def begin(): Unit = {
    val gl = Sge().graphics.gl
    gl.glDisable(GL20.GL_DEPTH_TEST)
    depthFunc = 0
    gl.glDepthMask(true)
    depthMask = true
    gl.glDisable(GL20.GL_BLEND)
    blending = false
    gl.glDisable(GL20.GL_CULL_FACE)
    cullFace = 0
    blendSourceRgbFactor = 0
    blendDestRgbFactor = 0
    blendSourceAlphaFactor = 0
    blendDestAlphaFactor = 0
    textureBinder.begin()
  }

  /** Resets all changed OpenGL states to their defaults. */
  def end(): Unit = {
    val gl = Sge().graphics.gl
    if (depthFunc != 0) gl.glDisable(GL20.GL_DEPTH_TEST)
    if (!depthMask) gl.glDepthMask(true)
    if (blending) gl.glDisable(GL20.GL_BLEND)
    if (cullFace > 0) gl.glDisable(GL20.GL_CULL_FACE)
    textureBinder.end()
  }

  def setDepthMask(depthMask: Boolean): Unit =
    if (this.depthMask != depthMask) {
      this.depthMask = depthMask
      Sge().graphics.gl.glDepthMask(depthMask)
    }

  def setDepthTest(depthFunction: Int): Unit =
    setDepthTest(depthFunction, 0f, 1f)

  def setDepthTest(depthFunction: Int, depthRangeNear: Float, depthRangeFar: Float): Unit = {
    val gl         = Sge().graphics.gl
    val wasEnabled = depthFunc != 0
    val enabled    = depthFunction != 0
    if (depthFunc != depthFunction) {
      depthFunc = depthFunction
      if (enabled) {
        gl.glEnable(GL20.GL_DEPTH_TEST)
        gl.glDepthFunc(depthFunction)
      } else {
        gl.glDisable(GL20.GL_DEPTH_TEST)
      }
    }
    if (enabled) {
      if (!wasEnabled || depthFunc != depthFunction) {
        depthFunc = depthFunction
        gl.glDepthFunc(depthFunction)
      }
      if (!wasEnabled || this.depthRangeNear != depthRangeNear || this.depthRangeFar != depthRangeFar) {
        this.depthRangeNear = depthRangeNear
        this.depthRangeFar = depthRangeFar
        gl.glDepthRangef(depthRangeNear, depthRangeFar)
      }
    }
  }

  def setBlending(enabled: Boolean, sFactor: Int, dFactor: Int): Unit =
    setBlending(enabled, sFactor, dFactor, sFactor, dFactor)

  def setBlending(enabled: Boolean, sRgbFactor: Int, dRgbFactor: Int, sAlphaFactor: Int, dAlphaFactor: Int): Unit = {
    val gl = Sge().graphics.gl
    if (enabled != blending) {
      blending = enabled
      if (enabled)
        gl.glEnable(GL20.GL_BLEND)
      else
        gl.glDisable(GL20.GL_BLEND)
    }
    if (
      enabled && (blendSourceRgbFactor != sRgbFactor || blendDestRgbFactor != dRgbFactor
        || blendSourceAlphaFactor != sAlphaFactor || blendDestAlphaFactor != dAlphaFactor)
    ) {
      gl.glBlendFuncSeparate(sRgbFactor, dRgbFactor, sAlphaFactor, dAlphaFactor)
      blendSourceRgbFactor = sRgbFactor
      blendDestRgbFactor = dRgbFactor
      blendSourceAlphaFactor = sAlphaFactor
      blendDestAlphaFactor = dAlphaFactor
    }
  }

  def setCullFace(face: Int): Unit = {
    val gl = Sge().graphics.gl
    if (face != cullFace) {
      cullFace = face
      if (face == GL20.GL_FRONT || face == GL20.GL_BACK || face == GL20.GL_FRONT_AND_BACK) {
        gl.glEnable(GL20.GL_CULL_FACE)
        gl.glCullFace(face)
      } else {
        gl.glDisable(GL20.GL_CULL_FACE)
      }
    }
  }
}
