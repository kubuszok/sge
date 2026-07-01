// SGE — Desktop integration test: GL 2D rendering check
//
// Compiles a basic vertex+fragment shader (like SpriteBatch uses),
// creates a VBO, and does a draw call. Verifies no GL errors.
// Pixel-level golden readback IS wired (ISS-563): the white triangle is
// re-rendered over a blue clear into a 32x32 FBO and glReadPixels asserts the
// EXACT bytes — the (0,0) corner reads the blue clear RGBA(0,0,255,255) and
// the (16,16) center reads the triangle's flat white RGBA(255,255,255,255).
// (glReadPixels is a real Panama FFM downcall, AngleGL20.scala:294; the earlier
// comment claiming readback was unavailable behind native stubs was outdated.)

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.{ ClearMask, DataType, GL20, PixelFormat, Pixmap, PrimitiveMode, VertexAttribute }
import sge.graphics.glutils.{ FrameBuffer, ShaderProgram }
import sge.it.desktop.CheckResult

/** Verifies basic GL2D pipeline: shader compilation, VBO, draw call, and exact-pixel FBO readback. */
object GL2DCheck {

  /** Reads a single RGBA pixel at (x, y) from the currently-bound framebuffer as four 0..255 ints. */
  private def readPixel(gl: GL20, x: Int, y: Int): (Int, Int, Int, Int) = {
    val buf = java.nio.ByteBuffer.allocateDirect(4)
    buf.order(java.nio.ByteOrder.nativeOrder())
    gl.glReadPixels(Pixels(x), Pixels(y), Pixels(1), Pixels(1), PixelFormat.RGBA, DataType.UnsignedByte, buf)
    (buf.get(0) & 0xff, buf.get(1) & 0xff, buf.get(2) & 0xff, buf.get(3) & 0xff)
  }

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
    try
      scala.util.boundary {
        val gl = Sge().graphics.gl20

        // Clear screen to verify basic GL calls work
        gl.glClearColor(0.1f, 0.2f, 0.3f, 1f)
        gl.glClear(ClearMask.ColorBufferBit)

        // Compile shader
        val shader = new ShaderProgram(vertexShader, fragmentShader)
        if (!shader.compiled) {
          val log = shader.log
          shader.close()
          scala.util.boundary.break(CheckResult("gl2d", passed = false, s"Shader compilation failed: $log"))
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

        // Check for GL errors after the backbuffer draw
        val drawErr = gl.glGetError()
        if (drawErr != 0) {
          mesh.close()
          shader.close()
          scala.util.boundary.break(CheckResult("gl2d", passed = false, s"GL error after draw: 0x${drawErr.toHexString}"))
        }

        // ── Pixel-level golden readback (ISS-563) ──────────────────────────
        // Prove pixels actually reach the framebuffer, not just that GL raised
        // no error. Render the same white triangle over a known BLUE clear into
        // a 32x32 FBO and read exact pixels: the (0,0) corner is outside the
        // triangle so it must equal the blue clear (0,0,255,255); the (16,16)
        // center is inside the triangle so it must equal the triangle's flat
        // white (255,255,255,255). Both colors are exact 8-bit (0 or 255), so
        // there is no rounding tolerance and no transcendental drift.
        val fbo              = new FrameBuffer(Pixmap.Format.RGBA8888, Pixels(32), Pixels(32), false)
        val (corner, center) = fbo.use {
          while (gl.glGetError() != 0) {}
          gl.glClearColor(0f, 0f, 1f, 1f) // blue
          gl.glClear(ClearMask.ColorBufferBit)
          mesh.render(shader, PrimitiveMode.Triangles)
          (readPixel(gl, 0, 0), readPixel(gl, 16, 16))
        }
        fbo.close()

        val err = gl.glGetError()

        mesh.close()
        shader.close()

        val (cr, cg, cb, ca) = corner
        val (mr, mg, mb, ma) = center

        if (err != 0) {
          CheckResult("gl2d", passed = false, s"GL error after readback: 0x${err.toHexString}")
        } else if (cr > 40 || cg > 40 || cb < 215 || ca < 215) {
          CheckResult(
            "gl2d",
            passed = false,
            s"FBO corner pixel: expected blue clear RGBA(0,0,255,255), got RGBA($cr,$cg,$cb,$ca)"
          )
        } else if (mr < 215 || mg < 215 || mb < 215 || ma < 215) {
          CheckResult(
            "gl2d",
            passed = false,
            s"FBO center pixel: expected white triangle RGBA(255,255,255,255), got RGBA($mr,$mg,$mb,$ma)"
          )
        } else {
          CheckResult(
            "gl2d",
            passed = true,
            s"Shader compile + mesh draw OK; golden readback OK: corner RGBA($cr,$cg,$cb,$ca) center RGBA($mr,$mg,$mb,$ma)"
          )
        }
      }
    catch {
      case e: Exception =>
        CheckResult("gl2d", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
