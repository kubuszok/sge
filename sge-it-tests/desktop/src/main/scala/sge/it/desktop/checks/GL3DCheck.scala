// SGE — Desktop integration test: GL 3D rendering check
//
// Compiles a vertex+fragment shader with a uniform matrix,
// creates a mesh, renders a triangle with a projection matrix.
// Verifies shader compilation, uniform setting, and no GL errors.
// Note: FBO readback not yet available (Pixmap native stubs).

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.{ ClearMask, PrimitiveMode, VertexAttribute }
import sge.graphics.glutils.ShaderProgram
import sge.it.desktop.CheckResult

/** Verifies 3D GL pipeline: shader with uniforms, depth buffer, mesh rendering. */
object GL3DCheck {

  private val vertexShader: String =
    """#version 100
      |uniform mat4 u_projTrans;
      |attribute vec4 a_position;
      |attribute vec4 a_color;
      |varying vec4 v_color;
      |void main() {
      |  gl_Position = u_projTrans * a_position;
      |  v_color = a_color;
      |}""".stripMargin

  private val fragmentShader: String =
    """#version 100
      |precision mediump float;
      |varying vec4 v_color;
      |void main() {
      |  gl_FragColor = v_color;
      |}""".stripMargin

  def run()(using Sge): CheckResult =
    try {
      val gl = Sge().graphics.gl20

      // Enable depth testing for 3D verification
      gl.glEnable(sge.graphics.EnableCap.DepthTest)
      gl.glClearColor(0f, 0f, 0f, 1f)
      gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)

      // Compile shader with uniform
      val shader = new ShaderProgram(vertexShader, fragmentShader)
      if (!shader.compiled) {
        val log = shader.log
        shader.close()
        return CheckResult("gl3d", passed = false, s"Shader compilation failed: $log")
      }

      // Verify uniform lookup works
      shader.bind()
      val loc = shader.fetchUniformLocation("u_projTrans", false)
      if (loc < 0) {
        shader.close()
        return CheckResult("gl3d", passed = false, s"Uniform u_projTrans not found (loc=$loc)")
      }

      // Set identity-like projection matrix
      val matrix = new sge.math.Matrix4()
      shader.setUniformMatrix("u_projTrans", matrix)

      // Create mesh with position + color
      val mesh = new sge.graphics.Mesh(true, 3, 0)(
        VertexAttribute.Position(),
        VertexAttribute.ColorPacked()
      )
      val green = java.lang.Float.intBitsToFloat(0xff00ff00.toInt)
      mesh.setVertices(
        Array[Float](
          -0.5f,
          -0.5f,
          0f,
          green,
          0.5f,
          -0.5f,
          0f,
          green,
          0f,
          0.5f,
          0f,
          green
        )
      )

      mesh.render(shader, PrimitiveMode.Triangles)

      val err = gl.glGetError()

      gl.glDisable(sge.graphics.EnableCap.DepthTest)
      mesh.close()
      shader.close()

      if (err == 0) {
        CheckResult("gl3d", passed = true, "Shader + uniform + depth + mesh render OK")
      } else {
        CheckResult("gl3d", passed = false, s"GL error after 3D draw: 0x${err.toHexString}")
      }
    } catch {
      case e: Exception =>
        CheckResult("gl3d", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
