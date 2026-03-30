/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package oklab

import sge.graphics.glutils.ShaderProgram

/** Constants and shader source strings for the Oklab ColorfulBatch. The ColorfulBatch uses the Oklab color space where SpriteBatch normally uses RGBA. In the batch color, red maps to additive L
  * (lightness), green maps to additive A (green-to-red), blue maps to additive B (violet-to-yellow), and alpha is multiplicative alpha.
  *
  * The "tweak" is a second color that provides multiplicative adjustments to L, A, B, and contrast:
  *   - L tweak: multiplicative lightness (0.5 = no change)
  *   - A tweak: multiplicative A (0.5 = no change, lower = less colorful)
  *   - B tweak: multiplicative B (0.5 = no change, lower = less colorful)
  *   - Contrast: exponential-like contrast adjustment (0.5 = no change)
  *
  * This provides the shader source strings and tweak management API. For a full Batch implementation, use these shaders with a custom SpriteBatch subclass that adds the tweak vertex attribute.
  */
object ColorfulBatch {

  /** How many floats are used for one "sprite" (position, color, texcoord, tweak). */
  val SPRITE_SIZE: Int = 24

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** A constant packed float that can be assigned to the tweak to make all the tweak adjustments virtually imperceptible. When set, it won't change L, A, or B multipliers or contrast.
    */
  val TWEAK_RESET: Float = ColorTools.oklab(0.5f, 0.5f, 0.5f, 0.5f)

  /** The default vertex shader for the Oklab ColorfulBatch. */
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

  /** The default fragment shader for the Oklab ColorfulBatch. */
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
      "const vec3 forward = vec3(1.0 / 3.0);\n" +
      "float toOklab(float L) {\n" +
      "  return pow(L, 1.5);\n" +
      "}\n" +
      "float fromOklab(float L) {\n" +
      "  return pow(L, 0.666666);\n" +
      "}\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.x = toOklab(lab.x);\n" +
      "  lab.x = (lab.x - 0.5) * 2.0;\n" +
      "  float contrast = exp(v_tweak.w * (-2.0 * 255.0 / 254.0) + 1.0);\n" +
      "  lab.x = pow(abs(lab.x), contrast) * sign(lab.x);\n" +
      "  lab.x = fromOklab(clamp(lab.x * v_tweak.x + v_color.x, 0.0, 1.0));\n" +
      "  lab.yz = clamp((lab.yz * v_tweak.yz + v_color.yz - 0.5) * 2.0, -1.0, 1.0);\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
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
