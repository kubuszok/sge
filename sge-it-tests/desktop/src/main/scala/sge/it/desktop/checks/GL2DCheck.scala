// SGE — Desktop integration test: GL 2D rendering check
//
// Compiles a basic vertex+fragment shader (like SpriteBatch uses),
// creates a VBO, and does a draw call. Verifies no GL errors.
// Note: Pixmap/Texture/FBO readback not yet available (native stubs).

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.{ ClearMask, PrimitiveMode, VertexAttribute }
import sge.graphics.glutils.ShaderProgram
import sge.it.desktop.CheckResult

/** Verifies basic GL2D pipeline: shader compilation, VBO, draw call. */
object GL2DCheck {

  // Minimal GLES 2.0 shaders matching SpriteBatch's pattern
  private val vertexShader: String =
    """#version 100
      |attribute vec4 a_position;
      |attribute vec4 a_color;
      |varying vec4 v_color;
      |void main() {
      |  gl_Position = a_position;
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
      val gl = Sge().graphics.getGL20()

      // Clear screen to verify basic GL calls work
      gl.glClearColor(0.1f, 0.2f, 0.3f, 1f)
      gl.glClear(ClearMask.ColorBufferBit)

      // Compile shader
      val shader = new ShaderProgram(vertexShader, fragmentShader)
      if (!shader.compiled) {
        val log = shader.getLog()
        shader.close()
        return CheckResult("gl2d", passed = false, s"Shader compilation failed: $log")
      }

      // Create mesh with position + color, render a triangle
      val mesh = new sge.graphics.Mesh(true, 3, 0)(
        VertexAttribute.Position(),
        VertexAttribute.ColorPacked()
      )
      val white = java.lang.Float.intBitsToFloat(0xffffffff.toInt)
      mesh.setVertices(
        Array[Float](
          -0.5f,
          -0.5f,
          0f,
          white,
          0.5f,
          -0.5f,
          0f,
          white,
          0f,
          0.5f,
          0f,
          white
        )
      )

      shader.bind()
      mesh.render(shader, PrimitiveMode.Triangles)

      // Check for GL errors
      val err = gl.glGetError()

      mesh.close()
      shader.close()

      if (err == 0) {
        CheckResult("gl2d", passed = true, "Shader compile + mesh draw OK, no GL errors")
      } else {
        CheckResult("gl2d", passed = false, s"GL error after draw: 0x${err.toHexString}")
      }
    } catch {
      case e: Exception =>
        CheckResult("gl2d", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
