/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBRShaderConfig.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 51
 * Covenant-baseline-methods: DEFAULT_GAMMA,PBRShaderConfig,SRGB,gamma,glslVersion,manualGammaCorrection,manualSRGB,mirrorSRGB,numVertexColors,prefix,transmissionSRGB
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package shaders

import sge.graphics.g3d.shaders.DefaultShader
import sge.utils.Nullable

class PBRShaderConfig extends DefaultShader.Config {

  /** Enable conversion of SRGB space textures into linear space in shader. */
  var manualSRGB: PBRShaderConfig.SRGB = PBRShaderConfig.SRGB.ACCURATE

  /** Enable conversion of SRGB space frame buffer into linear space in shader for transmission source. */
  var transmissionSRGB: PBRShaderConfig.SRGB = PBRShaderConfig.SRGB.ACCURATE

  /** Enable conversion of SRGB space frame buffer into linear space in shader for mirror source. */
  var mirrorSRGB: PBRShaderConfig.SRGB = PBRShaderConfig.SRGB.ACCURATE

  /** Enable/Disable gamma correction. Default is true. */
  var manualGammaCorrection: Boolean = true

  /** Gamma value used when manualGammaCorrection is enabled. Default is 2.2. */
  var gamma: Float = PBRShaderConfig.DEFAULT_GAMMA

  /** string to prepend to shaders (version), automatic if null */
  var glslVersion: Nullable[String] = Nullable.empty

  /** Max vertex color layers. Default PBRShader only uses 1 layer. */
  var numVertexColors: Int = 1

  /** Some custom GLSL code to inject in shaders. If not null it will be added after #version. */
  var prefix: Nullable[String] = Nullable.empty
}

object PBRShaderConfig {

  /** Default gamma factor that gives good results on most monitors. */
  val DEFAULT_GAMMA: Float = 2.2f

  enum SRGB extends java.lang.Enum[SRGB] {
    case NONE, FAST, ACCURATE
  }
}
