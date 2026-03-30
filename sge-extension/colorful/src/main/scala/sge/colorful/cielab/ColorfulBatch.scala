/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package cielab

import sge.graphics.glutils.ShaderProgram

/** Constants and shader source strings for the CIELAB ColorfulBatch. The ColorfulBatch uses the CIE L*A*B* color space where SpriteBatch normally uses RGBA. In the batch color, red maps to additive L
  * (lightness), green maps to additive A (green-to-red), blue maps to additive B (blue-to-yellow), and alpha is multiplicative alpha.
  *
  * The "tweak" is a second color that provides multiplicative adjustments to L, A, B, and contrast:
  *   - L tweak: multiplicative lightness (0.5 = no change)
  *   - A tweak: multiplicative A (0.5 = no change, lower = less colorful)
  *   - B tweak: multiplicative B (0.5 = no change, lower = less colorful)
  *   - Contrast: exponential-like contrast adjustment (0.5 = no change)
  */
object ColorfulBatch {

  /** How many floats are used for one "sprite" (position, color, texcoord, tweak). */
  val SPRITE_SIZE: Int = 24

  /** The name of the attribute used for the tweak color in GLSL shaders. */
  val TWEAK_ATTRIBUTE: String = "a_tweak"

  /** A constant packed float that can be assigned to the tweak to make all the tweak adjustments virtually imperceptible. When set, it won't change L, A, or B multipliers or contrast.
    */
  val TWEAK_RESET: Float = ColorTools.cielab(0.5f, 0.5f, 0.5f, 0.5f)

  /** The default vertex shader for the CIELAB ColorfulBatch. */
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
      "   v_color.w = v_color.w * (255.0/254.0);\n" +
      "   v_tweak = " + TWEAK_ATTRIBUTE + ";\n" +
      "   v_tweak.w = pow(v_tweak.w * (255.0/254.0) + 0.5, 1.709);\n" +
      "   v_lightFix = 1.0 + pow(v_tweak.w, 1.41421356);\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

  /** The default fragment shader for the CIELAB ColorfulBatch. */
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
      "const vec3 forward = vec3(1.0 / 3.0);\n" +
      "const vec3 sRGBFrom = vec3(2.4);\n" +
      "const vec3 sRGBThresholdFrom = vec3(0.04045);\n" +
      "const vec3 sRGBTo = vec3(1.0 / 2.4);\n" +
      "const vec3 sRGBThresholdTo = vec3(0.0031308);\n" +
      "const vec3 epsilon = vec3(0.00885645);\n" +
      "vec3 linear(vec3 t){ return mix(pow((t + 0.055) * (1.0 / 1.055), sRGBFrom), t * (1.0/12.92), step(t, sRGBThresholdFrom)); }\n" +
      "vec3 sRGB(vec3 t){ return mix(1.055 * pow(t, sRGBTo) - 0.055, 12.92*t, step(t, sRGBThresholdTo)); }\n" +
      "float xyzF(float t){ return mix(pow(t,1./3.), 7.787037 * t + 0.139731, step(t, 0.00885645)); }\n" +
      "vec3 xyzF(vec3 t){ return mix(pow(t, forward), 7.787037 * t + 0.139731, step(t, epsilon)); }\n" +
      "float xyzR(float t){ return mix(t*t*t , 0.1284185 * (t - 0.139731), step(t, 0.20689655)); }\n" +
      "vec3 rgb2lab(vec3 c)\n" +
      "{\n" +
      "    c *= mat3(0.4124, 0.3576, 0.1805,\n" +
      "              0.2126, 0.7152, 0.0722,\n" +
      "              0.0193, 0.1192, 0.9505);\n" +
      "    c = xyzF(c);\n" +
      "    vec3 lab = vec3(max(0.,1.16*c.y - 0.16), (c.x - c.y) * 5.0, (c.y - c.z) * 2.0); \n" +
      "    return lab;\n" +
      "}\n" +
      "vec3 lab2rgb(vec3 c)\n" +
      "{\n" +
      "    float lg = 1./1.16*(c.x + 0.16);\n" +
      "    vec3 xyz = vec3(xyzR(lg + c.y * 0.2),\n" +
      "                    xyzR(lg),\n" +
      "                    xyzR(lg - c.z * 0.5));\n" +
      "    vec3 rgb = xyz*mat3( 3.2406, -1.5372,-0.4986,\n" +
      "                        -0.9689,  1.8758, 0.0415,\n" +
      "                         0.0557, -0.2040, 1.0570);\n" +
      "    return rgb;\n" +
      "}\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 lab = rgb2lab(linear(tgt.rgb));\n" +
      "  lab.x = clamp(pow(lab.x, v_tweak.w) * v_lightFix * v_tweak.x + v_color.x - 0.5372549, 0.0, 1.0);\n" +
      "  lab.yz = (lab.yz * v_tweak.yz * 2.0) + (v_color.yz - 0.5) * 2.0;\n" +
      "  gl_FragColor = vec4(sRGB(clamp(lab2rgb(lab), 0.0, 1.0)), v_color.a * tgt.a);\n" +
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
