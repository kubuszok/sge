/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/RenderContext.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Gdx.gl -> Sge().graphics.gl (using Sge context)
 *   - All state management methods fully ported
 *   - setDepthTest, setBlending, setCullFace: logic matches Java source
 *   - begin()/end(): state initialization matches Java
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 150
 * Covenant-baseline-methods: RenderContext,blendDestAlphaFactor,blendDestRgbFactor,blendSourceAlphaFactor,blendSourceRgbFactor,blending,cullFace,depthFunc,depthMask,depthRangeFar,depthRangeNear,enabled,gl,rendering,setBlending,setCullFace,setDepthMask,setDepthTest,textureBinder,wasEnabled
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/RenderContext.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 96852061e1bd8c1924511b4b3338c77da961fdb9
 */
package sge
package graphics
package g3d
package utils

import scala.annotation.publicInBinary

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
  @publicInBinary private[sge] def begin(): Unit = {
    val gl = Sge().graphics.gl
    gl.glDisable(EnableCap.DepthTest)
    depthFunc = 0
    gl.glDepthMask(true)
    depthMask = true
    gl.glDisable(EnableCap.Blend)
    blending = false
    gl.glDisable(EnableCap.CullFace)
    cullFace = 0
    blendSourceRgbFactor = 0
    blendDestRgbFactor = 0
    blendSourceAlphaFactor = 0
    blendDestAlphaFactor = 0
    textureBinder.begin()
  }

  /** Resets all changed OpenGL states to their defaults. */
  @publicInBinary private[sge] def end(): Unit = {
    val gl = Sge().graphics.gl
    if (depthFunc != 0) gl.glDisable(EnableCap.DepthTest)
    if (!depthMask) gl.glDepthMask(true)
    if (blending) gl.glDisable(EnableCap.Blend)
    if (cullFace > 0) gl.glDisable(EnableCap.CullFace)
    textureBinder.end()
  }

  /** Executes `body` between [[begin]] and [[end]], ensuring [[end]] is called even if `body` throws. */
  inline def rendering[A](inline body: => A): A = {
    begin()
    try body
    finally end()
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
        gl.glEnable(EnableCap.DepthTest)
        gl.glDepthFunc(CompareFunc(depthFunction))
      } else {
        gl.glDisable(EnableCap.DepthTest)
      }
    }
    if (enabled) {
      if (!wasEnabled || depthFunc != depthFunction) {
        depthFunc = depthFunction
        gl.glDepthFunc(CompareFunc(depthFunction))
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
        gl.glEnable(EnableCap.Blend)
      else
        gl.glDisable(EnableCap.Blend)
    }
    if (
      enabled && (blendSourceRgbFactor != sRgbFactor || blendDestRgbFactor != dRgbFactor
        || blendSourceAlphaFactor != sAlphaFactor || blendDestAlphaFactor != dAlphaFactor)
    ) {
      gl.glBlendFuncSeparate(BlendFactor(sRgbFactor), BlendFactor(dRgbFactor), BlendFactor(sAlphaFactor), BlendFactor(dAlphaFactor))
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
      if (face == CullFace.Front.toInt || face == CullFace.Back.toInt || face == CullFace.FrontAndBack.toInt) {
        gl.glEnable(EnableCap.CullFace)
        gl.glCullFace(CullFace(face))
      } else {
        gl.glDisable(EnableCap.CullFace)
      }
    }
  }
}
