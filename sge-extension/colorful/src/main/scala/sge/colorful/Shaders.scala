/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1195
 * Covenant-baseline-methods: Shaders,fragmentShader,fragmentShaderCielab,fragmentShaderColorize,fragmentShaderColorizeOklab,fragmentShaderConfigurableContrast,fragmentShaderContrastUniform,fragmentShaderDayNight,fragmentShaderDoubleSaturation,fragmentShaderFlatLightness,fragmentShaderHSI,fragmentShaderHSL,fragmentShaderHSL4,fragmentShaderHSLA,fragmentShaderHSLA2,fragmentShaderHSLARoundTrip,fragmentShaderHSLC,fragmentShaderHSLC2,fragmentShaderHSLC3,fragmentShaderHSLP,fragmentShaderHigherContrast,fragmentShaderHigherContrastRGBA,fragmentShaderIPT,fragmentShaderIPT_HQ,fragmentShaderInvertedChroma,fragmentShaderInvertedLightness,fragmentShaderInvertedRGB,fragmentShaderLowerContrast,fragmentShaderLowerContrastRGBA,fragmentShaderMultiplyRGBA,fragmentShaderOklab,fragmentShaderPixelArt,fragmentShaderRGBA,fragmentShaderReplacement,fragmentShaderRotateHSL,fragmentShaderSwapWhite,fragmentShaderYCwCm,partialCodeHSL,partialHueRodrigues,vertexShader,vertexShaderDayNight,vertexShaderHSI,vertexShaderHSLC
 * Covenant-source-reference: com/github/tommyettinger/colorful/Shaders.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful

import sge.graphics.glutils.ShaderProgram

/** Shader code to construct a [[ShaderProgram]] that can render the specialized colors produced by the rest of this library. The shader code is meant for use in a SpriteBatch; many of the shaders
  * here are experimental and meant as a basis for user code, rather than a complete solution.
  *
  * None of the shaders here specify a `#version`, and some combinations of OS, driver, and GPU hardware may require the version to be set to a specific value.
  *
  * Note that you don't need to specify a shader from here at all if you use a ColorfulBatch from this library! There is a ColorfulBatch in each of the color space subpackages, such as `rgb`, `oklab`,
  * and `ipt`.
  */
object Shaders {

  /** This is the default vertex shader from libGDX. It is used without change by most of the fragment shaders here. */
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

  /** The simplest fragment shader libGDX can use, and the default in SpriteBatch. This tints a Texture's color by multiplying red, green, blue, and alpha by the batch color's channels.
    *
    * Meant for use with [[vertexShader]].
    */
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

  /** A simple shader that uses additive blending with "normal" RGBA colors (alpha is still multiplicative). With this, 50% gray is the neutral color.
    *
    * Meant for use with [[vertexShader]].
    */
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

  /** A shader meant for upscaling and/or rotating pixel art. This needs the uniform u_textureResolution set to the size of the current Texture being rendered. On OpenGL ES platforms, this needs the
    * extension `GL_OES_standard_derivatives` queried before it can be used.
    *
    * This style of shader was probably rediscovered many times, but this particular one is from [[https://www.shadertoy.com/view/ltfXWS this ShaderToy by Permutator]], CC0 licensed.
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderPixelArt: String =
    "#ifdef GL_ES\n" +
      "#extension GL_OES_standard_derivatives : enable\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "uniform vec2 u_textureResolution;\n" +
      "\n" +
      "vec2 v2len(vec2 a, vec2 b) {\n" +
      "    return sqrt(a*a+b*b);\n" +
      "}\n" +
      "\n" +
      "void main() {\n" +
      "    vec2 uv = v_texCoords * u_textureResolution;\n" +
      "    vec2 seam = floor(uv+.5);\n" +
      "    uv = seam + clamp((uv-seam)/v2len(dFdx(uv),dFdy(uv)), -.5, .5);\n" +
      "    gl_FragColor = texture2D(u_texture, uv/u_textureResolution) * v_color;\n" +
      "}\n"

  /** Adjusts RGBA colors so the RGB values are exaggerated towards or away from 0.0 or 1.0, depending on a uniform. This uses a uniform float, 0.0 or greater, called "contrast"; when contrast is 1.0,
    * the image is rendered without changes, but when it is 0.0, everything will be gray, and if it is greater than 1.0, contrast will be stronger.
    *
    * Meant for use with [[vertexShader]]. Make sure to set the `contrast` uniform before using!
    */
  val fragmentShaderContrastUniform: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "\n" +
      "uniform float contrast;\n" +
      "\n" +
      "void main()\n" +
      "{\n" +
      "    vec4 color = texture2D( u_texture, v_texCoords );\n" +
      "    color.rgb = clamp((color.rgb - 0.5) * contrast + 0.5, 0.0, 1.0);\n" +
      "    gl_FragColor = color;\n" +
      "}"

  /** A simple shader that uses multiplicative blending with "normal" RGBA colors, and is simpler than [[fragmentShaderRGBA]] but can make changes in color smoother. With this, 50% gray is the neutral
    * color, white multiplies the RGB channels by 2.0 (brightening it), and black multiplies them by 0 (reducing to black).
    *
    * Meant for use with [[vertexShader]].
    */
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
      "   vec4 tgt = texture2D(u_texture, v_texCoords);\n" +
      "   gl_FragColor = clamp(vec4(tgt.rgb * v_color.rgb * 2.0, v_color.a * tgt.a), 0.0, 1.0);\n" +
      "}"

  /** A simple shader that uses additive blending with "normal" RGBA colors (alpha is still multiplicative), and increases contrast somewhat, making the lightness change more sharply or harshly.
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderHigherContrastRGBA: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "const float contrast =    1.5   ;\n" +
      "vec3 barronSpline(vec3 x, float shape) {\n" +
      "        const float turning = 0.5;\n" +
      "        vec3 d = turning - x;\n" +
      "        return mix(\n" +
      "          ((1. - turning) * (x - 1.)) / (1. - (x + shape * d)) + 1.,\n" +
      "          (turning * x) / (1.0e-3 + (x + shape * d)),\n" +
      "          step(0.0, d));\n" +
      "}\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  tgt.rgb = barronSpline(clamp(tgt.rgb + v_color.rgb - 0.5, 0.0, 1.0), contrast);\n" +
      "  tgt.a *= v_color.a;\n" +
      "  gl_FragColor = tgt;\n" +
      "}"

  /** A simple shader that uses additive blending with "normal" RGBA colors (alpha is still multiplicative), and reduces contrast somewhat, making the lightness more murky and uniform.
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderLowerContrastRGBA: String = fragmentShaderHigherContrastRGBA.replace("   1.5   ", "0.5")

  /** Where the magic happens; this converts a batch color from the YCwCm format to RGBA. The vertex color will be split up into 4 channels just as a normal shader does, but the channels here are
    * luma, chromatic warmth, chromatic mildness, and alpha; alpha acts just like a typical RGBA shader, but the others are additive instead of multiplicative, with 0.5 as a neutral value.
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderYCwCm: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "const vec3 bright = vec3(0.375, 0.5, 0.125);\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "////use the following line to match the color exactly\n" +
      "   vec3 ycc = vec3(v_color.r - 0.5 + dot(tgt.rgb, bright), ((v_color.g - 0.5) * 2.0 + tgt.r - tgt.b), ((v_color.b - 0.5) * 2.0 + tgt.g - tgt.b));\n" +
      "////use the following line to increase contrast\n" +
      "//   vec3 ycc = vec3(v_color.r * dot(sin(tgt.rgb * 1.5707963267948966) * sqrt(tgt.rgb), bright), ((v_color.g - 0.5) * 2.0 + tgt.r - tgt.b), ((v_color.b - 0.5) * 2.0 + tgt.g - tgt.b));\n" +
      "////use the following line to increase contrast more\n" +
      "//   vec3 ycc = vec3(v_color.r * pow(dot(tgt.rgb, bright), 1.25), ((v_color.g - 0.5) * 2.0 + tgt.r - tgt.b), ((v_color.b - 0.5) * 2.0 + tgt.g - tgt.b));\n" +
      // uses a specific matrix (related to bright, above) multiplied with ycc to get back to rgb.
      "   gl_FragColor = vec4( (clamp(mat3(1.0, 1.0, 1.0, 0.625, -0.375, -0.375, -0.5, 0.5, -0.5) * ycc, 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** A variant on [[fragmentShaderYCwCm]] that adjusts luma to make mid-range colors darker, while keeping light colors light.
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderHigherContrast: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "const float contrast =    1.375   ; // You can make contrast a uniform if you want.\n" +
      "const vec3 bright = vec3(0.375, 0.5, 0.125) * (4.0 / 3.0);\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   vec3 ycc = vec3(v_color.r - 0.5 + pow(dot(tgt.rgb, bright), contrast) * 0.75, ((v_color.g - 0.5) * 2.0 + tgt.r - tgt.b), ((v_color.b - 0.5) * 2.0 + tgt.g - tgt.b));\n" +
      "   gl_FragColor = vec4( (clamp(mat3(1.0, 1.0, 1.0, 0.625, -0.375, -0.375, -0.5, 0.5, -0.5) * ycc, 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** An alternative shader that effectively reduces luma contrast, bringing all but the darkest colors to the upper-mid luma range.
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderLowerContrast: String = fragmentShaderHigherContrast.replace("   1.375   ", "0.625")

  /** Similar to [[fragmentShaderYCwCm]], but this uses the very perceptually-accurate IPT color space as described by Ebner and Fairchild, instead of the custom YCwCm color space.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderIPT: String =
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
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    vec4 adj = v_color;\n" +
      "    adj.yz = adj.yz * 2.0 - 0.5;\n" +
      "    vec3 ipt = (mat3(0.189786, 0.669665 , 0.286498, 0.576951, -0.73741 , 0.655205, 0.233221, 0.0681367, -0.941748)\n" +
      "         * (tgt.rgb)) + adj.xyz - 0.5;\n" +
      "    ipt.x = clamp(ipt.x, 0.0, 1.0);\n" +
      "    ipt.yz = clamp(ipt.yz, -1.0, 1.0);\n" +
      "    vec3 back = mat3(0.999779, 1.00015, 0.999769, 1.07094, -0.377744, 0.0629496, 0.324891, 0.220439, -0.809638) * ipt;\n" +
      "    gl_FragColor = vec4(clamp(back, 0.0, 1.0), adj.a * tgt.a);\n" +
      "}"

  /** Just like [[fragmentShaderIPT]], but gamma-corrects the input and output RGB values and uses an exponential step internally to change how colors are distributed within the gamut.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderIPT_HQ: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "const vec3 forward = vec3(0.43);\n" +
      "const vec3 reverse = vec3(1.0 / 0.43);\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 ipt = mat3(0.40000, 4.45500, 0.80560, 0.40000, -4.8510, 0.35720, 0.20000, 0.39600, -1.1628) *" +
      "             pow(mat3(0.313921, 0.151693, 0.017753, 0.639468, 0.748209, 0.109468, 0.0465970, 0.1000044, 0.8729690) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  ipt.x = clamp(ipt.x + v_color.x - 0.55, 0.0, 1.0);\n" +
      "  ipt.yz = clamp(ipt.yz + v_color.yz * 2.0 - 1.0, -1.0, 1.0);\n" +
      "  ipt = mat3(1.0, 1.0, 1.0, 0.097569, -0.11388, 0.032615, 0.205226, 0.133217, -0.67689) * ipt;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(5.432622, -1.10517, 0.028104, -4.67910, 2.311198, -0.19466, 0.246257, -0.20588, 1.166325) *\n" +
      "                 (sign(ipt) * pow(abs(ipt), reverse))," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** Just like [[fragmentShaderIPT_HQ]], but uses the Oklab color space instead of the very similar IPT_HQ one. This also gamma-corrects the inputs and outputs, though it uses subtly different math
    * internally.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderOklab: String =
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
      "  lab.x = fromOklab(clamp(toOklab(lab.x) + v_color.r - 0.5, 0.0, 1.0));\n" +
      "  lab.yz = clamp(lab.yz + v_color.gb * 2.0 - 1.0, -1.0, 1.0);\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** A vertex shader that does the bulk of processing HSI-format batch colors and converting them to a format [[fragmentShaderHSI]] can use.
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val vertexShaderHSI: String = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
    "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
    "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
    "uniform mat4 u_projTrans;\n" +
    "varying vec4 v_color;\n" +
    "varying vec2 v_texCoords;\n" +
    "const vec3 yellow  = vec3( 0.16155326,0.020876605,-0.26078433 );\n" +
    "const vec3 magenta = vec3(-0.16136102,0.122068435,-0.070396   );\n" +
    "const vec3 cyan    = vec3( 0.16420607,0.3481738,   0.104959644);\n" +
    "void main()\n" +
    "{\n" +
    "    v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
    "    v_color.a = " + ShaderProgram.COLOR_ATTRIBUTE + ".a * (255.0/254.0);\n" +
    "    vec3 hsi = v_color.rgb;\n" +
    "    v_color.x = (hsi.z - 0.5) * 0.9999;\n" +
    "    hsi.x *= 6.28318;\n" +
    "    hsi.y *= 0.5;\n" +
    "    v_color.y = cos(hsi.x) * hsi.y;\n" +
    "    v_color.z = sin(hsi.x) * hsi.y;\n" +
    "    float crMid = dot(cyan.yz, v_color.yz);\n" +
    "    float mgMid = dot(magenta.yz, v_color.yz);\n" +
    "    float ybMid = dot(yellow.yz, v_color.yz);\n" +
    "    float crScale = (v_color.x - 0.5 + step(crMid, 0.0)) * cyan.x / (0.00001 - crMid);\n" +
    "    float mgScale = (v_color.x + 0.5 - step(mgMid, 0.0)) * magenta.x / (0.00001 - mgMid);\n" +
    "    float ybScale = (v_color.x - 0.5 + step(ybMid, 0.0)) * yellow.x / (0.00001 - ybMid);\n" +
    "    float scale = 4.0 * min(crScale, min(mgScale, ybScale));\n" +
    "    v_color.yz *= scale * length(v_color.yz) / cos(3.14159 * v_color.x);\n" +
    "    v_color.xyz += 0.5;\n" +
    "    v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
    "    gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
    "}\n"

  /** This is an alias for [[fragmentShaderIPT]]. If used with [[vertexShaderHSI]], you can specify a batch color using an HSL-like system.
    *
    * Meant for use with [[vertexShaderHSI]].
    */
  var fragmentShaderHSI: String = fragmentShaderIPT

  /** Not a full shader, this is a snippet used by most of the other HSL-based shaders to implement the complex rgb2hsl() and hsl2rgb() methods.
    *
    * [[https://gamedev.stackexchange.com/a/59808 Credit to Sam Hocevar]].
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val partialCodeHSL: String =
    "const float eps = 1.0e-3;\n" +
      "vec4 rgb2hsl(vec4 c)\n" +
      "{\n" +
      "    const vec4 J = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
      "    vec4 p = mix(vec4(c.bg, J.wz), vec4(c.gb, J.xy), step(c.b, c.g));\n" +
      "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
      "    float d = q.x - min(q.w, q.y);\n" +
      "    float l = q.x * (1.0 - 0.5 * d / (q.x + eps));\n" +
      "    return vec4(abs(q.z + (q.w - q.y) / (6.0 * d + eps)), (q.x - l) / (min(l, 1.0 - l) + eps), l, c.a);\n" +
      "}\n" +
      "\n" +
      "vec4 hsl2rgb(vec4 c)\n" +
      "{\n" +
      "    const vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
      "    vec3 p = abs(fract(c.x + K.xyz) * 6.0 - K.www);\n" +
      "    float v = (c.z + c.y * min(c.z, 1.0 - c.z));\n" +
      "    return vec4(v * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), 2.0 * (1.0 - c.z / (v + eps))), c.w);\n" +
      "}" +
      "////Call this to go from the official HSL hue distribution (where blue is opposite yellow) to a\n" +
      "////different distribution that matches primary colors in painting (where purple is opposite yellow).\n" +
      "//float official2primaries(float hue) {\n" +
      "//    return  hue * (  2.137\n" +
      "//          + hue * (  0.542\n" +
      "//          + hue * (-15.141\n" +
      "//          + hue * ( 30.120\n" +
      "//          + hue * (-22.541\n" +
      "//          + hue *   5.883)))));\n" +
      "//}\n" +
      "////Call this to go to the official HSL hue distribution (where blue is opposite yellow) from a\n" +
      "////different distribution that matches primary colors in painting (where purple is opposite yellow).\n" +
      "//float primaries2official(float hue) {\n" +
      "//    return  hue * (  0.677\n" +
      "//          + hue * ( -0.123\n" +
      "//          + hue * (-11.302\n" +
      "//          + hue * ( 46.767\n" +
      "//          + hue * (-58.493\n" +
      "//          + hue *   23.474)))));\n" +
      "//}\n"

  /** This GLSL snippet takes an RGB vec3 and a float that represents a hue rotation in radians, and returns the rotated RGB vec3.
    *
    * Credit for this challenging method goes to Andrey-Postelzhuk, [[https://forum.unity.com/threads/hue-saturation-brightness-contrast-shader.260649/ Unity Forums]].
    */
  val partialHueRodrigues: String =
    "vec3 applyHue(vec3 rgb, float hue)\n" +
      "{\n" +
      "    vec3 k = vec3(0.57735);\n" +
      "    float c = cos(hue);\n" +
      "    //Rodrigues' rotation formula\n" +
      "    return rgb * c + cross(k, rgb) * sin(hue) + k * dot(k, rgb) * (1.0 - c);\n" +
      "}\n"

  /** Treats the color as hue, saturation, lightness, and alpha. You probably want [[fragmentShaderHSLC]] or [[fragmentShaderHSLA]].
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val fragmentShaderHSL: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   vec4 hsl = rgb2hsl(tgt);\n" +
      "   hsl.x = fract(v_color.x + hsl.x);\n" +
      "   hsl.y = hsl.y * v_color.y;\n" +
      "   hsl.z = hsl.z * v_color.z * 2.0;\n" +
      "   gl_FragColor = hsl2rgb(hsl);\n" +
      "}"

  /** EXPERIMENTAL. Meant more for reading and editing than serious usage. */
  val fragmentShaderRotateHSL: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   vec4 hsl = rgb2hsl(tgt);\n" +
      "   hsl.x = fract(v_color.x + hsl.x + 0.5);\n" +
      "   hsl.yz = clamp(hsl.yz * v_color.yz * 2.0, 0.0, 1.0);\n" +
      "   gl_FragColor = hsl2rgb(hsl);\n" +
      "}"

  /** This is similar to the default vertex shader from libGDX, but also sets a varying value for contrast. It is needed if you use [[fragmentShaderHSLC]].
    */
  val vertexShaderHSLC: String = "attribute vec4 a_position;\n" +
    "attribute vec4 a_color;\n" +
    "attribute vec2 a_texCoord0;\n" +
    "uniform mat4 u_projTrans;\n" +
    "varying vec4 v_color;\n" +
    "varying vec2 v_texCoords;\n" +
    "varying float v_lightFix;\n" +
    "\n" +
    "void main()\n" +
    "{\n" +
    "   v_color = a_color;\n" +
    "   v_texCoords = a_texCoord0;\n" +
    "   v_color.a = pow(v_color.a * (255.0/254.0) + 0.5, 1.709);\n" +
    "   v_lightFix = 1.0 + pow(v_color.a, 1.41421356);\n" +
    "   gl_Position =  u_projTrans * a_position;\n" +
    "}\n"

  /** Allows changing Hue/Saturation/Lightness/Contrast, with hue as a rotation.
    *
    * Credit for HLSL version goes to Andrey-Postelzhuk, [[https://forum.unity.com/threads/hue-saturation-brightness-contrast-shader.260649/ Unity Forums]].
    *
    * Meant only for use with [[vertexShaderHSLC]].
    */
  val fragmentShaderHSLC: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_lightFix;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialHueRodrigues +
      "void main()\n" +
      "{\n" +
      "    float hue = 6.2831853 * (v_color.x - 0.5);\n" +
      "    float saturation = v_color.y * 2.0;\n" +
      "    float brightness = v_color.z - 0.5;\n" +
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    tgt.rgb = applyHue(tgt.rgb, hue);\n" +
      "    tgt.rgb = vec3(\n" +
      "     (0.5 * pow(dot(tgt.rgb, vec3(0.375, 0.5, 0.125)), v_color.w) * v_lightFix + brightness),\n" +
      "     ((tgt.r - tgt.b) * saturation),\n" +
      "     ((tgt.g - tgt.b) * saturation));\n" +
      "    gl_FragColor = clamp(vec4(\n" +
      "     dot(tgt.rgb, vec3(1.0, 0.625, -0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, 0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, -0.5)),\n" +
      "     tgt.a), 0.0, 1.0);\n" +
      "}"

  /** Allows changing Hue/Saturation/Lightness/Alpha, with hue as a rotation.
    *
    * Meant to be used with [[vertexShader]], unlike what [[fragmentShaderHSLC]] expects.
    */
  val fragmentShaderHSLA: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialHueRodrigues +
      "void main()\n" +
      "{\n" +
      "    float hue = 6.2831853 * (v_color.x - 0.5);\n" +
      "    float saturation = v_color.y * 2.0;\n" +
      "    float brightness = v_color.z - 0.5;\n" +
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    tgt.rgb = applyHue(tgt.rgb, hue);\n" +
      "    tgt.rgb = vec3(\n" +
      "     (dot(tgt.rgb, vec3(0.375, 0.5, 0.125)) + brightness),\n" +
      "     ((tgt.r - tgt.b) * saturation),\n" +
      "     ((tgt.g - tgt.b) * saturation));\n" +
      "    gl_FragColor = clamp(vec4(\n" +
      "     dot(tgt.rgb, vec3(1.0, 0.625, -0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, 0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, -0.5)),\n" +
      "     tgt.a * v_color.w), 0.0, 1.0);\n" +
      "}"

  /** Allows changing Hue/Saturation/Lightness/Alpha, with hue as a rotation.
    *
    * Meant to be used with [[vertexShader]], unlike what [[fragmentShaderHSLC]] expects.
    */
  val fragmentShaderHSLA2: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   vec4 hsl = rgb2hsl(tgt);\n" +
      "   hsl.x = fract(v_color.x + hsl.x);\n" +
      "   hsl.yz = hsl.yz * v_color.yz;\n" +
      "   gl_FragColor = hsl2rgb(hsl);\n" +
      "}"

  /** EXPERIMENTAL. */
  val fragmentShaderHSLARoundTrip: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   tgt = hsl2rgb(rgb2hsl(tgt));\n" +
      "   gl_FragColor = tgt * v_color;\n" +
      "}"

  /** Generally a lower-quality hue rotation than [[fragmentShaderHSLC]]; this is here as a work in progress.
    *
    * Meant to be used with [[vertexShaderHSLC]].
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val fragmentShaderHSLC2: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_lightFix;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "void main()\n" +
      "{\n" +
      "    float hue = (v_color.x - 0.5);\n" +
      "    float saturation = v_color.y * 2.0;\n" +
      "    float brightness = v_color.z - 0.5;\n" +
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    tgt = rgb2hsl(tgt);\n" +
      "    tgt.r = fract(tgt.r + hue);\n" +
      "    tgt = hsl2rgb(tgt);\n" +
      "    tgt.rgb = vec3(\n" +
      "     (0.5 * pow(dot(tgt.rgb, vec3(0.375, 0.5, 0.125)), v_color.w) * v_lightFix + brightness),\n" +
      "     ((tgt.r - tgt.b) * saturation),\n" +
      "     ((tgt.g - tgt.b) * saturation));\n" +
      "    gl_FragColor = clamp(vec4(\n" +
      "     dot(tgt.rgb, vec3(1.0, 0.625, -0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, 0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, -0.5)),\n" +
      "     tgt.a), 0.0, 1.0);\n" +
      "}"

  /** Cycles lightness in a psychedelic way as hue and lightness change; not a general-purpose usage.
    *
    * Meant to be used with [[vertexShaderHSLC]].
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val fragmentShaderHSLC3: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying float v_lightFix;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialHueRodrigues +
      "void main()\n" +
      "{\n" +
      "    float hue = 6.2831853 * (v_color.x - 0.5);\n" +
      "    float saturation = v_color.y * 2.0;\n" +
      "    float brightness = v_color.z - 0.5;\n" +
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    tgt.rgb = applyHue(tgt.rgb, hue);\n" +
      "    tgt.rgb = vec3(\n" +
      "     (0.5 * pow(dot(tgt.rgb, vec3(0.375, 0.5, 0.125)), v_color.w) * v_lightFix),\n" +
      "     ((tgt.r - tgt.b) * saturation),\n" +
      "     ((tgt.g - tgt.b) * saturation));\n" +
      "    tgt.r = sin((tgt.r + brightness) * 6.2831853) * 0.5 + 0.5;\n" +
      "    gl_FragColor = clamp(vec4(\n" +
      "     dot(tgt.rgb, vec3(1.0, 0.625, -0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, 0.5)),\n" +
      "     dot(tgt.rgb, vec3(1.0, -0.375, -0.5)),\n" +
      "     tgt.a), 0.0, 1.0);\n" +
      "}"

  /** Cycles hue, but not lightness; otherwise this is like [[fragmentShaderHSLC3]] without contrast.
    *
    * Expects the vertex shader to be [[vertexShader]], not the HSLC variant.
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val fragmentShaderHSL4: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "void main()\n" +
      "{\n" +
      "    float hue = v_color.x - 0.5;\n" +
      "    float saturation = v_color.y * 2.0;\n" +
      "    float brightness = v_color.z - 0.5;\n" +
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    tgt = rgb2hsl(tgt);\n" +
      "    tgt.x = fract(tgt.x + hue);\n" +
      "    tgt.y = clamp(tgt.y * saturation, 0.0, 1.0);\n" +
      "    tgt.z = clamp(brightness + tgt.z, 0.0, 1.0);\n" +
      "    gl_FragColor = hsl2rgb(tgt);\n" +
      "}"

  /** One of the more useful HSL shaders here, this takes a batch color as hue, saturation, lightness, and power, with hue as a target hue and power used to determine how much of the target color
    * should be used.
    *
    * Expects the vertex shader to be [[vertexShader]], not the HSLC variant.
    */
  val fragmentShaderHSLP: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      partialCodeHSL +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "   vec4 hsl = rgb2hsl(tgt);\n" +
      "   hsl.x *= 6.2831853;\n" +
      "   hsl.xy = vec2(cos(hsl.x), sin(hsl.x)) * hsl.y;\n" +
      "   vec3 tint = vec3(cos(v_color.x * 6.2831853) * v_color.y, sin(v_color.x * 6.2831853) * v_color.y * 2.0, v_color.z);\n" +
      "   hsl.xyz = mix(hsl.xyz, tint, v_color.w);\n" +
      "   hsl.xy = vec2(fract(atan(hsl.y, hsl.x) / 6.2831853), length(hsl.xy));\n" +
      "   gl_FragColor = hsl2rgb(hsl);\n" +
      "}"

  /** This is supposed to look for RGBA colors that are similar to `search`, and if it finds one, to replace it with `replace` (also an RGBA color).
    *
    * EXPERIMENTAL. Meant more for reading and editing than serious usage.
    */
  val fragmentShaderReplacement: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "uniform vec4 u_search;\n" +
      "uniform vec4 u_replace;\n" +
      "void main()\n" +
      "{\n" +
      "   vec4 tgt = texture2D(u_texture, v_texCoords);\n" +
      "   float curve = smoothstep(0.0, 1.0, 1.25 - distance(tgt.rgb, u_search.rgb) * 2.0);\n" +
      "   gl_FragColor = vec4(mix(tgt.rgb, u_replace.rgb, curve), tgt.a) * v_color;\n" +
      "}"

  /** A drop-in replacement for the default fragment shader that eliminates lightness differences in the output colors.
    *
    * Meant for use with [[vertexShader]].
    *
    * @see
    *   [[fragmentShaderConfigurableContrast]] a per-sprite-configurable version of this
    */
  var fragmentShaderFlatLightness: String =
    "#ifdef GL_ES\n" +
      "#define LOWP lowp\n" +
      "precision mediump float;\n" +
      "#else\n" +
      "#define LOWP \n" +
      "#endif\n" +
      "#define TARGET_LIGHTNESS 0.5 \n" +
      "#define SATURATION_CHANGE 1.0 \n" +
      "varying vec2 v_texCoords;\n" +
      "varying LOWP vec4 v_color;\n" +
      "uniform sampler2D u_texture;\n" +
      "void main()\n" +
      "{\n" +
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    vec3 ipt = (mat3(0.189786, 0.669665 , 0.286498, 0.576951, -0.73741 , 0.655205, 0.233221, 0.0681367, -0.941748)\n" +
      "         * (tgt.rgb * v_color.rgb));\n" +
      "    ipt.x = TARGET_LIGHTNESS;\n" +
      "//    ipt.x = (ipt.x - 0.5) * 0.25 + TARGET_LIGHTNESS;\n" +
      "    ipt.yz *= SATURATION_CHANGE;\n" +
      "    vec3 back = clamp(mat3(0.999779, 1.00015, 0.999769, 1.07094, -0.377744, 0.0629496, 0.324891, 0.220439, -0.809638) * ipt, 0.0, 1.0);\n" +
      "    gl_FragColor = vec4(back, v_color.a * tgt.a);\n" +
      "}"

  /** A specialized shader that can reduce lightness differences in the output colors, saturate/desaturate them, and can be configured to use some of the existing lightness in the image to add to a
    * main flat lightness.
    *
    * Meant for use with [[vertexShader]].
    *
    * @see
    *   [[fragmentShaderFlatLightness]] if you only need one contrast setting and still want to set color tints
    */
  var fragmentShaderConfigurableContrast: String =
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
      "    vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "    vec3 ipt = (mat3(0.189786, 0.669665 , 0.286498, 0.576951, -0.73741 , 0.655205, 0.233221, 0.0681367, -0.941748)\n" +
      "         * tgt.rgb);\n" +
      "    ipt.x = (ipt.x - 0.5) * v_color.b + v_color.r;\n" +
      "    ipt.yz *= v_color.g * 2.0;\n" +
      "    vec3 back = clamp(mat3(0.999779, 1.00015, 0.999769, 1.07094, -0.377744, 0.0629496, 0.324891, 0.220439, -0.809638) * ipt, 0.0, 1.0);\n" +
      "    gl_FragColor = vec4(back, v_color.a * tgt.a);\n" +
      "}"

  /** A day/night cycle vertex shader. This is meant to be used with [[fragmentShaderDayNight]]. */
  var vertexShaderDayNight: String = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
    "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
    "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
    "uniform mat4 u_projTrans;\n" +
    "uniform float u_timeOfDay;\n" +
    "varying vec4 v_color;\n" +
    "varying vec4 v_tweak;\n" +
    "varying vec2 v_texCoords;\n" +
    "varying float v_lightFix;\n" +
    "const vec3 forward = vec3(1.0 / 3.0);\n" +
    "\n" +
    "void main()\n" +
    "{\n" +
    "   float st = sin(1.5707963 * sin(0.2617994 * u_timeOfDay)); // Whenever st is very high or low... \n" +
    "   float ct = sin(1.5707963 * cos(0.2617994 * u_timeOfDay)); // ...ct is close to 0, and vice versa. \n" +
    "   float dd = ct * ct; // Positive, small; used for dawn and dusk. \n" +
    "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
    "   v_color.w = v_color.w * (255.0/254.0);\n" +
    "   vec3 oklab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
    "     pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
    "     * (v_color.rgb * v_color.rgb), forward);\n" +
    "   // The next four lines make use of the time-based variables st, ct, and dd. Edit to fit. \n" +
    "   v_color.x = clamp(oklab.x + (0.0625 * st), 0.0, 1.0);\n" +
    "   v_color.yz = clamp(oklab.yz + vec2(0.0625 * dd + 0.03125 * st, 0.1 * st), -1.0, 1.0) * ((dd + 0.25) * 0.5);\n" +
    "   v_tweak = vec4(0.2 * st + 0.5);\n" +
    "   v_tweak.w = pow((1.0 - 0.125 * st), 1.709);\n" +
    "   v_lightFix = 1.0 + pow(v_tweak.w, 1.41421356);\n" +
    "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
    "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
    "}\n"

  /** The fragment shader counterpart to [[vertexShaderDayNight]]; must be used with that vertex shader. */
  var fragmentShaderDayNight: String =
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
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords );\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.x = clamp(pow(lab.x, v_tweak.w) * v_lightFix * v_tweak.x + v_color.x - 1.0, 0.0, 1.0);\n" +
      "  lab.yz = clamp((lab.yz * v_tweak.yz + v_color.yz) * 1.5, -1.0, 1.0);\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** Takes a batch color in CIE LAB format (but ranging from 0 to 1 instead of its normal larger range). Adapted from [[https://www.shadertoy.com/view/lsdGzN This ShaderToy by nmz]].
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderCielab: String =
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
      "  lab.x = lab.x + v_color.r - 0.5372549;\n" +
      "  lab.yz = lab.yz + (v_color.gb - 0.5) * 2.0;\n" +
      "  gl_FragColor = vec4(sRGB(clamp(lab2rgb(lab), 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** Makes the colors in the given textures almost-grayscale, then moves their chromatic channels much closer to the batch color, without changing the lightness. This uses an RGB batch color.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderColorize: String =
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
      "//// Ugly repeated matrix math to convert from RGB to Oklab. Oklab keeps lightness separate from hue and saturation.\n" +
      "  vec3 base = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "              pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "              * (tgt.rgb * tgt.rgb), forward);\n" +
      "  vec3 tint = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "              pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "              * (v_color.rgb * v_color.rgb), forward);\n" +
      "//// Sharply increases lightness contrast, to counteract the gray-ing caused by averaging base and tint lightness.\n" +
      "  tint.x = (tint.x + base.x) - 1.0;\n" +
      "  tint.x = sign(tint.x) * pow(abs(tint.x), 0.7) * 0.5 + 0.5;\n" +
      "//// Uncomment these next 3 lines if you want the original image to contribute some color, if it has any.\n" +
      "  float blen = length(base.yz);\n" +
      "  blen *= blen;\n" +
      "  tint.yz = clamp(tint.yz * (0.7 + blen) + base.yz * (0.3 - blen), -1.0, 1.0);\n" +
      "//// Reverse the Oklab conversion to get back to RGB. Uses the batch color's alpha normally.\n" +
      "  tint = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * tint;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (tint * tint * tint)," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** Makes the colors in the given textures almost-grayscale, then moves their chromatic channels much closer to the batch color's chromatic channels, without changing the lightness. This uses an
    * Oklab batch color.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderColorizeOklab: String =
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
      "  vec3 base = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "              pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "              * (tgt.rgb * tgt.rgb), forward);\n" +
      "  vec2 tint = v_color.gb - 0.5;\n" +
      "  base.x = clamp(base.x, 0.0, 1.0);\n" +
      "  float blen = length(base.yz);\n" +
      "  blen *= blen;\n" +
      "  base.gb = clamp(tint * (v_color.r + blen) + base.yz * (1.0 - v_color.r - blen), -1.0, 1.0);\n" +
      "  base = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * base;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (base * base * base)," +
      "                 0.0, 1.0)), v_color.a * tgt.a);\n" +
      "}"

  /** The simplest possible color-inverting shader for a SpriteBatch.
    *
    * Meant only for use with [[vertexShader]].
    */
  val fragmentShaderInvertedRGB: String =
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
      "   vec4 tgt = texture2D(u_texture, v_texCoords);\n" +
      "   gl_FragColor = vec4(1.0 - tgt.rgb * v_color.rgb, v_color.a * tgt.a);\n" +
      "}"

  /** Just like [[fragmentShaderInvertedRGB]], but internally converts to Oklab, so it can invert just lightness without changing color hue or saturation. This uses an RGBA batch color.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderInvertedLightness: String =
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
      "float toOklab(float L) {\n" +
      "        const float shape = 0.64516133, turning = 0.95;\n" +
      "        float d = turning - L;\n" +
      "        float r = mix(\n" +
      "          ((1. - turning) * (L - 1.)) / (1. - (L + shape * d)) + 1.,\n" +
      "          (turning * L) / (1.0e-3 + (L + shape * d)),\n" +
      "          step(0.0, d));\n" +
      "        return r * r;\n" +
      "}\n" +
      "float fromOklab(float L) {\n" +
      "        const float shape = 1.55, turning = 0.95;\n" +
      "        L = sqrt(L);\n" +
      "        float d = turning - L;\n" +
      "        return mix(\n" +
      "          ((1. - turning) * (L - 1.)) / (1. - (L + shape * d)) + 1.,\n" +
      "          (turning * L) / (1.0e-3 + (L + shape * d)),\n" +
      "          step(0.0, d));\n" +
      "}\n" +
      "void main()\n" +
      "{\n" +
      "  vec4 tgt = texture2D( u_texture, v_texCoords ) * v_color;\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.x = fromOklab(1.0 - toOklab(lab.x));\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), tgt.a);\n" +
      "}"

  /** Just like [[fragmentShaderInvertedLightness]], but instead of inverting lightness, this tries to change only hue, without changing lightness or (generally) saturation. This uses an RGBA batch
    * color.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderInvertedChroma: String =
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
      "  vec4 tgt = texture2D( u_texture, v_texCoords ) * v_color;\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.yz = -lab.yz;\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), tgt.a);\n" +
      "}"

  /** Just like [[fragmentShaderInvertedChroma]], but instead of inverting chroma, this tries to significantly increase the saturation of a color (by pushing chroma away from gray). This uses an RGBA
    * batch color.
    *
    * Meant for use with [[vertexShader]].
    */
  var fragmentShaderDoubleSaturation: String =
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
      "  vec4 tgt = texture2D( u_texture, v_texCoords ) * v_color;\n" +
      "  vec3 lab = mat3(+0.2104542553, +1.9779984951, +0.0259040371, +0.7936177850, -2.4285922050, +0.7827717662, -0.0040720468, +0.4505937099, -0.8086757660) *" +
      "             pow(mat3(0.4121656120, 0.2118591070, 0.0883097947, 0.5362752080, 0.6807189584, 0.2818474174, 0.0514575653, 0.1074065790, 0.6302613616) \n" +
      "             * (tgt.rgb * tgt.rgb), forward);\n" +
      "  lab.yz *=   2.000  ;\n" +
      "  lab = mat3(1.0, 1.0, 1.0, +0.3963377774, -0.1055613458, -0.0894841775, +0.2158037573, -0.0638541728, -1.2914855480) * lab;\n" +
      "  gl_FragColor = vec4(sqrt(clamp(" +
      "                 mat3(+4.0767245293, -1.2681437731, -0.0041119885, -3.3072168827, +2.6093323231, -0.7034763098, +0.2307590544, -0.3411344290, +1.7068625689) *\n" +
      "                 (lab * lab * lab)," +
      "                 0.0, 1.0)), tgt.a);\n" +
      "}"

  /** A simple shader that renders just like SpriteBatch's default shader, except that when it would draw white, it instead draws red (which can be replaced in the shader code with a custom color).
    *
    * Meant for use with [[vertexShader]].
    */
  val fragmentShaderSwapWhite: String =
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
      "   vec4 tgt = texture2D(u_texture, v_texCoords);\n" +
      "   if(tgt.r + tgt.g + tgt.b == 3.0)\n" +
      "     gl_FragColor = vec4(   1.00, 0.00, 0.00   , tgt.a) * v_color;\n" +
      "   else\n" +
      "     gl_FragColor = tgt * v_color;\n" +
      "}"

  // Note: The vertexShaderHsluv, fragmentShaderHsluv, partialCodeHSLStretched, and the various
  // make*Batch() / make*Shader() factory methods from the original are intentionally omitted.
  // The HSLuv vertex shader is extremely long (~170 lines of GLSL) and highly experimental.
  // The factory methods create SpriteBatch/ShaderProgram instances, which in SGE are handled
  // differently (via ColorfulBatch subclasses in each color space package). Users who need these
  // specialized shaders can construct ShaderProgram instances directly from the string constants.
}
