/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ycwcm

import sge.graphics.glutils.ShaderProgram

/** Constants and shader source strings for the YCwCm ColorfulBatch. The ColorfulBatch uses the YCwCm color space where SpriteBatch normally uses RGBA. In the batch color, red maps to additive Y
  * (luma/lightness), green maps to additive Cw (chromatic warmth), blue maps to additive Cm (chromatic mildness), and alpha is multiplicative alpha.
  *
  * The "tweak" is a second color that provides multiplicative adjustments to Y, Cw, Cm, and contrast:
  *   - Y tweak: multiplicative luma (0.5 = no change)
  *   - Cw tweak: multiplicative warmth (0.5 = no change)
  *   - Cm tweak: multiplicative mildness (0.5 = no change)
  *   - Contrast: exponential-like contrast adjustment (0.5 = no change)
  */
object ColorfulBatch {

  /** How many floats are used for one "sprite" (position, color, texcoord, tweak). */
  val SPRITE_SIZE: Int = 24

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** A constant packed float that can be assigned to the tweak to make all the tweak adjustments virtually imperceptible.
    */
  val TWEAK_RESET: Float = ColorTools.ycwcm(0.5f, 0.5f, 0.5f, 0.5f)

  /** The default vertex shader for the YCwCm ColorfulBatch. */
  val vertexShader: String =
    "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "attribute vec4 " + TWEAK_ATTRIBUTE + ";\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec4 v_tweak;\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_lightFix;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "   v_color.a = v_color.a * (255.0/254.0);\n" +
      "   v_tweak = " + TWEAK_ATTRIBUTE + ";\n" +
      "   v_tweak.a = pow(v_tweak.a * (255.0/254.0) + 0.5, 1.709);\n" +
      "   v_lightFix = 1.0 + pow(v_tweak.a, 1.41421356);\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

  /** The default fragment shader for the YCwCm ColorfulBatch. */
  val fragmentShader: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "varying LOWP vec4 v_tweak;\n" +
      "varying float v_lightFix;\n" +
      "uniform sampler2D u_texture;\n" +
      "const vec3 bright = vec3(0.375, 0.5, 0.125);\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   vec3 ycc = vec3(\n" +
      "     (v_color.r - 0.5 + v_tweak.r * pow(dot(tgt.rgb, bright), v_tweak.a) * v_lightFix),\n" +
      "     (v_color.g - 0.5 + (tgt.r - tgt.b) * v_tweak.g) * 2.0,\n" +
      "     (v_color.b - 0.5 + (tgt.g - tgt.b) * v_tweak.b) * 2.0);\n" +
      "   gl_FragColor = vec4( (clamp(mat3(1.0, 1.0, 1.0, 0.625, -0.375, -0.375, -0.5, 0.5, -0.5) * ycc, 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** Creates the default ShaderProgram for this ColorfulBatch. */
  def createDefaultShader()(using sge.Sge): ShaderProgram = {
    val shader = new ShaderProgram(vertexShader, fragmentShader)
    if (!shader.compiled) {
      throw new IllegalArgumentException("Error compiling shader: " + shader.log)
    }
    shader
  }
}
