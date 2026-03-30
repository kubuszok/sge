/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package rgb

import sge.graphics.glutils.ShaderProgram

/** Constants and shader source strings for the RGB ColorfulBatch. This uses the RGB color space like SpriteBatch, but the batch color is additive for R, G, B channels (0.5 = neutral), while the tweak
  * is multiplicative, and the neutral value for all RGB channels is 0.5 (instead of 1.0 for SpriteBatch).
  *
  * The "tweak" is a second color providing multiplicative adjustments:
  *   - R tweak: multiplicative red (0.5 = no change)
  *   - G tweak: multiplicative green (0.5 = no change)
  *   - B tweak: multiplicative blue (0.5 = no change)
  *   - Contrast: exponential-like contrast adjustment (0.5 = no change)
  */
object ColorfulBatch {

  /** How many floats are used for one "sprite". */
  val SPRITE_SIZE: Int = 24

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** A packed float that resets all tweak adjustments. */
  val TWEAK_RESET: Float = ColorTools.rgb(0.5f, 0.5f, 0.5f, 0.5f)

  /** The default vertex shader for the RGB ColorfulBatch. */
  val vertexShader: String =
    "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "attribute vec4 " + TWEAK_ATTRIBUTE + ";\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec4 v_tweak;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "  v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "  v_color.w = v_color.w * (255.0/254.0);\n" +
      "  v_tweak = " + TWEAK_ATTRIBUTE + ";\n" +
      "  v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "  gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

  /** The default fragment shader for the RGB ColorfulBatch. */
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
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 rgb = (tgt.rgb - 0.5) * 2.0;\n" +
      "  float contrast = exp(v_tweak.w * (-2.0 * 255.0 / 254.0) + 1.0);\n" +
      "  float luma = dot(rgb, vec3(0.32627, 0.3678, 0.30593));\n" +
      "  rgb = pow(abs(rgb), vec3(contrast)) * sign(rgb);\n" +
      "  rgb = clamp(rgb * v_tweak.rgb * 2.0 + v_color.rgb - 0.5, -0.5, 0.5) + 0.5;\n" +
      "  gl_FragColor = vec4(rgb, v_color.a * tgt.a);\n" +
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
