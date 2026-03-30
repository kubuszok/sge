/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ipt

import sge.graphics.glutils.ShaderProgram

/** Constants and shader source strings for the IPT ColorfulBatch. The ColorfulBatch uses the IPT color space where SpriteBatch normally uses RGBA. In the batch color, red maps to additive I
  * (intensity/lightness), green maps to additive P (protan, green-to-red), blue maps to additive T (tritan, blue-to-yellow), and alpha is multiplicative alpha.
  *
  * The "tweak" is a second color that provides multiplicative adjustments to I, P, T, and contrast:
  *   - I tweak: multiplicative intensity (0.5 = no change)
  *   - P tweak: multiplicative protan (0.5 = no change)
  *   - T tweak: multiplicative tritan (0.5 = no change)
  *   - Contrast: exponential-like contrast adjustment (0.5 = no change)
  */
object ColorfulBatch {

  /** How many floats are used for one "sprite" (position, color, texcoord, tweak). */
  val SPRITE_SIZE: Int = 24

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** A constant packed float that can be assigned to the tweak to make all the tweak adjustments virtually imperceptible.
    */
  val TWEAK_RESET: Float = ColorTools.ipt(0.5f, 0.5f, 0.5f, 0.5f)

  /** The default vertex shader for the IPT ColorfulBatch. */
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

  /** The default fragment shader for the IPT ColorfulBatch. */
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
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 ipt = mat3(0.189786, 0.669665 , 0.286498, 0.576951, -0.73741 , 0.655205, 0.233221, 0.0681367, -0.941748)\n" +
      "             * tgt.rgb;\n" +
      "  ipt.x = pow(ipt.x, v_tweak.a) * v_lightFix * v_tweak.r + v_color.r - 0.5;\n" +
      "  ipt.yz = (ipt.yz * v_tweak.gb + v_color.gb - 0.5) * 2.0;\n" +
      "  vec3 back = mat3(0.999779, 1.00015, 0.999769, 1.07094, -0.377744, 0.0629496, 0.324891, 0.220439, -0.809638) * ipt;\n" +
      "  gl_FragColor = clamp(vec4(back, v_color.a * tgt.a), 0.0, 1.0);\n" +
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
