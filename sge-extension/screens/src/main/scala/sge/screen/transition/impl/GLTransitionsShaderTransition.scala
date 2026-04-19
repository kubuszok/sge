/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 104
 * Covenant-baseline-methods: FRAG_SHADER_POSTPEND,FRAG_SHADER_PREPEND,GLTransitionsShaderTransition,VERT_SHADER,this
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition
package impl

import sge.math.Interpolation
import sge.utils.Nullable

/** A transition that is using shader code conforming to the ''GL Transition Specification v1''. This allows using the shaders provided at [[https://gl-transitions.com/gallery gl-transitions.com]]
  * without having to adapt their code.
  *
  * What is a GL Transition? It is a GLSL code that implements a `transition` function which takes a `vec2 uv` pixel position and returns a `vec4 color`. This color represents the mix of the `from` to
  * the `to` textures based on the variation of a contextual progress value from `0.0` to `1.0`.
  *
  * This transition can be reused.
  *
  * @since 0.4.0
  * @author
  *   damios
  *
  * @see
  *   [[https://github.com/gl-transitions/gl-transitions#gl-transition Additional information on the GL Transition spec]]
  */
class GLTransitionsShaderTransition(
  glTransitionsCode: String,
  duration:          Float,
  interpolation:     Nullable[Interpolation] = Nullable.empty
)(using Sge)
    extends ShaderTransition(
      GLTransitionsShaderTransition.VERT_SHADER,
      GLTransitionsShaderTransition.FRAG_SHADER_PREPEND + glTransitionsCode + GLTransitionsShaderTransition.FRAG_SHADER_POSTPEND,
      true,
      duration,
      interpolation
    ) {

  /** Creates a GL Transitions shader transition with default (no) interpolation.
    *
    * @param glTransitionsCode
    *   the GL Transitions shader code
    * @param duration
    *   the transition's duration in seconds
    */
  def this(glTransitionsCode: String, duration: Float)(using Sge) =
    this(glTransitionsCode, duration, Nullable.empty)
}

object GLTransitionsShaderTransition {

  // @formatter:off
  private val VERT_SHADER: String =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "#endif\n" +
    "\n" +
    "attribute vec3 a_position;\n" +
    "attribute vec2 a_texCoord0;\n" +
    "\n" +
    "uniform mat4 u_projTrans;\n" +
    "\n" +
    "varying vec3 v_position;\n" +
    "varying vec2 v_texCoord0;\n" +
    "\n" +
    "void main() {\n" +
    "	v_position = a_position;\n" +
    "	v_texCoord0 = a_texCoord0;\n" +
    "	gl_Position = u_projTrans * vec4(a_position, 1.0);\n" +
    "}"

  private val FRAG_SHADER_PREPEND: String =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "#endif\n" +
    "\n" +
    "varying vec3 v_position;\n" +
    "varying vec2 v_texCoord0;\n" +
    "\n" +
    "\n" +
    "\n" +
    "uniform sampler2D lastScreen;\n" +
    "uniform sampler2D currScreen;\n" +
    "uniform float progress;\n" +
    "\n" +
    "vec4 getToColor(vec2 uv) {\n" +
    "		return texture2D(currScreen, uv);\n" +
    "}\n" +
    "\n" +
    "vec4 getFromColor(vec2 uv) {\n" +
    "		return texture2D(lastScreen, uv);\n" +
    "}\n"

  private val FRAG_SHADER_POSTPEND: String =
    "\nvoid main() {\n" +
    "	gl_FragColor = transition(v_texCoord0);\n" +
    "}\n"
  // @formatter:on
}
