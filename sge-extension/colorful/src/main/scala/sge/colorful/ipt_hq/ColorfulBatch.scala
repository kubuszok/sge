/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ipt_hq

import sge.graphics.glutils.ShaderProgram

/** Constants and shader source strings for the IPT_HQ ColorfulBatch. IPT_HQ is a higher-quality variant of IPT that uses more complex gamma adjustments for better lightness perception. In the batch
  * color, red maps to additive I (intensity/lightness), green maps to additive P (protan, green-to-red), blue maps to additive T (tritan, blue-to-yellow), and alpha is multiplicative alpha.
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

  /** The default vertex shader for the IPT_HQ ColorfulBatch. */
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

  /** The default fragment shader for the IPT_HQ ColorfulBatch. */
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
      "const vec3 forward = vec3(0.43);\n" +
      "const vec3 reverse = vec3(1.0 / 0.43);\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 ipt = mat3(0.40000, 4.45500, 0.80560, 0.40000, -4.8510, 0.35720, 0.20000, 0.39600, -1.1628) *" +
      "             pow(mat3(0.313921, 0.151693, 0.017753, 0.639468, 0.748209, 0.109468, 0.0465970, 0.1000044, 0.8729690) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  ipt.x = clamp(pow(ipt.x, v_tweak.a) * v_lightFix * v_tweak.r + v_color.r - 0.55, 0.0, 1.0);\n" +
      "  ipt.yz = clamp((ipt.yz * v_tweak.gb + v_color.gb - 0.5) * 2.0, -1.0, 1.0);\n" +
      "  ipt = mat3(1.0, 1.0, 1.0, 0.097569, -0.11388, 0.032615, 0.205226, 0.133217, -0.67689) * ipt;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(5.432622, -1.10517, 0.028104, -4.67910, 2.311198, -0.19466, 0.246257, -0.20588, 1.166325) *\n" +
      "                 (sign(ipt) * pow(abs(ipt), reverse))," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
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
