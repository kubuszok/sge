package sge
package colorful

import sge.graphics.glutils.ShaderProgram

/** Green control suite for ISS-526: asserts adjacent shader constants that DO exist in the port today, in exactly the same style as HsluvShadersRedSuite. It compiles and passes at the red sha,
  * proving that the breakage demonstrated by HsluvShadersRedSuite is scoped to the omitted HSLuv shaders, partialCodeHSLStretched, and make* factory methods — not to the test approach.
  */
class ShadersControlSuite extends munit.FunSuite {

  test("vertexShader matches the upstream default vertex shader") {
    val vs = Shaders.vertexShader
    assert(
      vs.startsWith("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE),
      "must start with the position attribute declaration"
    )
    assert(vs.contains("   v_color.a = v_color.a * (255.0/254.0);\n"), "must rescale alpha like the upstream vertex shader")
    assert(
      vs.contains("   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"),
      "must project the position attribute"
    )
  }

  test("partialCodeHSL matches the upstream snippet") {
    val partial = Shaders.partialCodeHSL
    assert(partial.startsWith("const float eps = 1.0e-3;\n"), "must start with the eps constant")
    assert(partial.contains("vec4 rgb2hsl(vec4 c)\n"), "must define rgb2hsl")
    assert(partial.contains("vec4 hsl2rgb(vec4 c)\n"), "must define hsl2rgb")
    assert(
      partial.contains("    return vec4(abs(q.z + (q.w - q.y) / (6.0 * d + eps)), (q.x - l) / (min(l, 1.0 - l) + eps), l, c.a);\n"),
      "rgb2hsl must compute the un-stretched hue (unlike partialCodeHSLStretched)"
    )
  }

  test("fragmentShaderSwapWhite matches the upstream constant used by makeSwappingBatch") {
    val fs = Shaders.fragmentShaderSwapWhite
    assert(fs.startsWith("#ifdef GL_ES\n"), "must start with the GL_ES preamble")
    assert(fs.contains("   if(tgt.r + tgt.g + tgt.b == 3.0)\n"), "must test for white texels")
    assert(
      fs.contains("     gl_FragColor = vec4(   1.00, 0.00, 0.00   , tgt.a) * v_color;\n"),
      "must keep the replaceable '   1.00, 0.00, 0.00   ' token that makeSwappingBatch substitutes"
    )
  }

  test("fragmentShaderDoubleSaturation keeps the token that makeSaturatingBatch substitutes") {
    val fs = Shaders.fragmentShaderDoubleSaturation
    assert(
      fs.contains("  lab.yz *=   2.000  ;\n"),
      "must keep the replaceable '   2.000   ' saturation token used by makeSaturatingBatch"
    )
    assert(fs.contains("const vec3 forward = vec3(1.0 / 3.0);\n"), "must declare the cube-root constant")
  }
}
