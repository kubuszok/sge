package sge
package colorful

import sge.graphics.g2d.SpriteBatch
import sge.graphics.glutils.ShaderProgram

/** Red test for ISS-526: the HSLuv shaders ([[Shaders.vertexShaderHsluv]], [[Shaders.fragmentShaderHsluv]]), the [[Shaders.partialCodeHSLStretched]] snippet, and every `make*Batch`/`make*Shader`
  * factory method from the original `com.github.tommyettinger.colorful.Shaders` were omitted from the port.
  *
  * Upstream reference: original-src/colorful-gdx/colorful/src/main/java/com/github/tommyettinger/colorful/Shaders.java (commit e4a5fd960eef746ca5aa826063432fb79666d74f) — vertexShaderHsluv at line
  * 1801, fragmentShaderHsluv at line 1894, partialCodeHSLStretched at line 803, makeRGBAShader at line 344, makeMultiplyRGBAShader at line 363, makeRGBABatch at line 388, makeYCwCmBatch at lines 492
  * and 517, makeBatchHSLC at line 542, makeSaturatingBatch at line 1789, makeSwappingBatch at line 2087.
  *
  * The shader-string tests run headlessly and assert distinctive GLSL substrings quoted verbatim from the upstream constants. The factory methods construct ShaderProgram/SpriteBatch instances and
  * need a live GL context, so they are only pinned at compile time (referenced, never invoked).
  */
class HsluvShadersRedSuite extends munit.FunSuite {

  test("vertexShaderHsluv matches the upstream HSLuv vertex shader") {
    val vs = Shaders.vertexShaderHsluv
    assert(
      vs.startsWith("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE),
      "must start with the position attribute declaration"
    )
    assert(vs.contains("const float kappa = 9.032962962;\n"), "must declare the kappa constant")
    assert(vs.contains("float intersectLength (float sn, float cs, float line1, float line2) {\n"), "must define intersectLength")
    assert(vs.contains("float chromaLimit(float hue, float lightness) {\n"), "must define chromaLimit")
    assert(vs.contains("        top1 = 2845.17 * ms.x - 948.39 * ms.z;\n"), "must contain the chromaLimit bound coefficients")
    assert(vs.contains("vec3 hsl2luv(vec3 c)\n"), "must define hsl2luv")
    assert(vs.contains("   v_color.x *= 6.2831;\n"), "must scale the hue channel to radians in main()")
    assert(vs.contains("   v_color.rgb = hsl2luv(v_color.rgb);\n"), "must convert the batch color with hsl2luv in main()")
    assert(vs.contains("   v_color.w = v_color.w * (255.0/254.0);\n"), "must rescale alpha like the upstream vertex shader")
  }

  test("fragmentShaderHsluv matches the upstream HSLuv fragment shader") {
    val fs = Shaders.fragmentShaderHsluv
    assert(fs.startsWith("#ifdef GL_ES\n"), "must start with the GL_ES preamble")
    assert(fs.contains("const vec2 refUV = vec2(0.19783000664283681, 0.468319994938791);\n"), "must declare the refUV constant")
    assert(fs.contains("float chromaLimit(float hue, float lightness) {\n"), "must define chromaLimit")
    assert(fs.contains("vec3 rgb2luv(vec3 c)\n"), "must define rgb2luv")
    assert(fs.contains("vec3 luv2rgb(vec3 c)\n"), "must define luv2rgb")
    assert(fs.contains("float forwardLight(float L) {\n"), "must define forwardLight")
    assert(fs.contains("float reverseLight(float L) {\n"), "must define reverseLight")
    assert(
      fs.contains("        const float shape = 0.8528, turning = 0.1;\n"),
      "must use the upstream forwardLight shape constant"
    )
    assert(
      fs.contains("        const float shape = 1.1726, turning = 0.1;\n"),
      "must use the upstream reverseLight shape constant"
    )
    assert(
      fs.contains("  luv.x = forwardLight(clamp(luv.x + v_color.x - 0.5372549, 0.0, 1.0));\n"),
      "must offset lightness exactly like upstream main()"
    )
    assert(
      fs.contains(
        "vec3 linear(vec3 t){ return mix(pow((t + 0.055) * (1.0 / 1.055), sRGBFrom), t * (1.0/12.92), step(t, sRGBThresholdFrom)); }\n"
      ),
      "must gamma-correct inputs"
    )
  }

  test("partialCodeHSLStretched matches the upstream snippet") {
    val partial = Shaders.partialCodeHSLStretched
    assert(partial.startsWith("const float eps = 1.0e-3;\n"), "must start with the eps constant")
    assert(partial.contains("float official2primaries(float hue) {\n"), "must define official2primaries")
    assert(
      partial.contains("    return (sqrt(hue + 0.050625) - 0.225) * 1.25;\n"),
      "must use the active official2primaries formula"
    )
    assert(partial.contains("float primaries2official(float hue) {\n"), "must define primaries2official")
    assert(
      partial.contains("    return pow(hue * 0.8 + 0.225, 2.0) - 0.050625;\n"),
      "must use the active primaries2official formula"
    )
    assert(partial.contains("vec4 rgb2hsl(vec4 c)\n"), "must define rgb2hsl")
    assert(
      partial.contains(
        "    return vec4(official2primaries(abs(q.z + (q.w - q.y) / (6.0 * d + eps))), (q.x - l) / (min(l, 1.0 - l) + eps), l, c.a);\n"
      ),
      "rgb2hsl must stretch the hue through official2primaries (this is what distinguishes it from partialCodeHSL)"
    )
    assert(partial.contains("vec4 hsl2rgb(vec4 c)\n"), "must define hsl2rgb")
    assert(
      partial.contains("    vec3 p = abs(fract(primaries2official(c.x) + K.xyz) * 6.0 - K.www);\n"),
      "hsl2rgb must un-stretch the hue through primaries2official"
    )
  }

  test("make* factory methods exist with the upstream signatures") {
    // The factories construct ShaderProgram/SpriteBatch instances, which require a live GL
    // context that is not available in headless unit tests. This test therefore only pins
    // their existence, parameter lists, and return types at compile time: the lambda below
    // is deliberately never invoked.
    val pinned: Sge => Unit = ctx => factorySignatures(using ctx)
    val _ = pinned
  }

  private def factorySignatures(using Sge): Unit = {
    val _: ShaderProgram = Shaders.makeRGBAShader()
    val _: ShaderProgram = Shaders.makeMultiplyRGBAShader()
    val _: SpriteBatch   = Shaders.makeRGBABatch(0.625f)
    val _: SpriteBatch   = Shaders.makeYCwCmBatch()
    val _: SpriteBatch   = Shaders.makeYCwCmBatch(0.875f)
    val _: SpriteBatch   = Shaders.makeBatchHSLC()
    val _: SpriteBatch   = Shaders.makeSaturatingBatch(1.5f)
    val _: SpriteBatch   = Shaders.makeSwappingBatch(1.0f, 0.5f, 0.25f)
  }
}
