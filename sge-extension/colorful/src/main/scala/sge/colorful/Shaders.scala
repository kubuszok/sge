/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful

import sge.graphics.glutils.ShaderProgram

/** Shader code to construct a [[ShaderProgram]] that can render the specialized colors produced by the rest of this library. Many of the shaders here are experimental and meant as a basis for user
  * code.
  */
object Shaders {

  /** This is the default vertex shader from libGDX. */
  val vertexShader: String =
    "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "uniform mat4 u_projTrans;\n" +
      "varying vec4 v_color;\n" +
      "varying vec2 v_texCoords;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
      "   v_color.a = v_color.a * (255.0/254.0);\n" +
      "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
      "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      "}\n"

  /** The simplest fragment shader: multiplicative tinting. */
  val fragmentShader: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 target = texture2D( u_texture, v_texCoords );\n" +
      "  gl_FragColor = target * v_color;\n" +
      "}"

  /** Additive blending with RGBA colors (alpha is still multiplicative). */
  val fragmentShaderRGBA: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   gl_FragColor = clamp(vec4(tgt.rgb + (v_color.rgb - 0.5), v_color.a * tgt.a), 0.0, 1.0);\n" +
      "}"

  /** Multiplicative RGBA shader, where 50% gray is neutral. */
  val fragmentShaderMultiplyRGBA: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  gl_FragColor = vec4(tgt.rgb * v_color.rgb * 2.0, tgt.a * v_color.a);\n" +
      "}"

  /** Oklab fragment shader for use with SpriteBatch (single color attribute, additive L/A/B). */
  val fragmentShaderOklab: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "const vec3 forward = vec3(1.0 / 3.0);\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *\n" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.x = pow(lab.x, 1.5);\n" +
      "  lab.x = clamp(lab.x + v_color.x - 0.5, 0.0, 1.0);\n" +
      "  lab.x = pow(lab.x, 0.666666);\n" +
      "  lab.yz = clamp((lab.yz + v_color.yz - 0.5) * 2.0, -1.0, 1.0);\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"
}
