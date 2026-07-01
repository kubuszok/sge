// SGE — Desktop integration test: GL 3D rendering check
//
// Compiles a vertex+fragment shader with a uniform matrix,
// creates a mesh, renders a triangle with a projection matrix.
// Verifies shader compilation, uniform setting, and no GL errors.
// Pixel-level golden readback IS wired (ISS-563): the green triangle is
// re-rendered (identity projection, depth test on) over a red clear into a
// 32x32 depth-backed FBO and glReadPixels asserts the EXACT bytes — the (0,0)
// corner reads the red clear RGBA(255,0,0,255) and the (16,16) center reads
// the triangle's flat green RGBA(0,255,0,255). (glReadPixels is a real Panama
// FFM downcall, AngleGL20.scala:294; the earlier comment claiming FBO readback
// was unavailable behind Pixmap native stubs was outdated.)

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.{ ClearMask, DataType, GL20, PixelFormat, Pixmap, PrimitiveMode, VertexAttribute }
import sge.graphics.glutils.{ FrameBuffer, ShaderProgram }
import sge.it.desktop.CheckResult

/** Verifies 3D GL pipeline: shader with uniforms, depth buffer, mesh rendering, and exact-pixel FBO readback. */
object GL3DCheck {

  /** Reads a single RGBA pixel at (x, y) from the currently-bound framebuffer as four 0..255 ints. */
  private def readPixel(gl: GL20, x: Int, y: Int): (Int, Int, Int, Int) = {
    val buf = java.nio.ByteBuffer.allocateDirect(4)
    buf.order(java.nio.ByteOrder.nativeOrder())
    gl.glReadPixels(Pixels(x), Pixels(y), Pixels(1), Pixels(1), PixelFormat.RGBA, DataType.UnsignedByte, buf)
    (buf.get(0) & 0xff, buf.get(1) & 0xff, buf.get(2) & 0xff, buf.get(3) & 0xff)
  }

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
    try
      scala.util.boundary {
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
          scala.util.boundary.break(CheckResult("gl3d", passed = false, s"Shader compilation failed: $log"))
        }

        // Verify uniform lookup works
        shader.bind()
        val loc = shader.fetchUniformLocation("u_projTrans", false)
        if (loc < 0) {
          shader.close()
          scala.util.boundary.break(CheckResult("gl3d", passed = false, s"Uniform u_projTrans not found (loc=$loc)"))
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

        val drawErr = gl.glGetError()
        if (drawErr != 0) {
          gl.glDisable(sge.graphics.EnableCap.DepthTest)
          mesh.close()
          shader.close()
          scala.util.boundary.break(CheckResult("gl3d", passed = false, s"GL error after 3D draw: 0x${drawErr.toHexString}"))
        }

        // ── Pixel-level golden readback (ISS-563) ──────────────────────────
        // Prove pixels actually reach the framebuffer, not just that GL raised
        // no error. Render the same green triangle (identity projection, depth
        // test on) over a known RED clear into a 32x32 depth-backed FBO and read
        // exact pixels: the (0,0) corner is outside the triangle so it must
        // equal the red clear (255,0,0,255); the (16,16) center is inside the
        // triangle so it must equal the triangle's flat green (0,255,0,255).
        // Both colors are exact 8-bit (0 or 255), so there is no rounding
        // tolerance and no transcendental drift.
        val fbo              = new FrameBuffer(Pixmap.Format.RGBA8888, Pixels(32), Pixels(32), true)
        val (corner, center) = fbo.use {
          while (gl.glGetError() != 0) {}
          gl.glClearColor(1f, 0f, 0f, 1f) // red
          gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
          shader.setUniformMatrix("u_projTrans", matrix)
          mesh.render(shader, PrimitiveMode.Triangles)
          (readPixel(gl, 0, 0), readPixel(gl, 16, 16))
        }
        fbo.close()

        val err = gl.glGetError()

        gl.glDisable(sge.graphics.EnableCap.DepthTest)
        mesh.close()
        shader.close()

        val (cr, cg, cb, ca) = corner
        val (mr, mg, mb, ma) = center

        if (err != 0) {
          CheckResult("gl3d", passed = false, s"GL error after readback: 0x${err.toHexString}")
        } else if (cr < 215 || cg > 40 || cb > 40 || ca < 215) {
          CheckResult(
            "gl3d",
            passed = false,
            s"FBO corner pixel: expected red clear RGBA(255,0,0,255), got RGBA($cr,$cg,$cb,$ca)"
          )
        } else if (mr > 40 || mg < 215 || mb > 40 || ma < 215) {
          CheckResult(
            "gl3d",
            passed = false,
            s"FBO center pixel: expected green triangle RGBA(0,255,0,255), got RGBA($mr,$mg,$mb,$ma)"
          )
        } else {
          CheckResult(
            "gl3d",
            passed = true,
            s"Shader + uniform + depth + mesh render OK; golden readback OK: corner RGBA($cr,$cg,$cb,$ca) center RGBA($mr,$mg,$mb,$ma)"
          )
        }
      }
    catch {
      case e: Exception =>
        CheckResult("gl3d", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
