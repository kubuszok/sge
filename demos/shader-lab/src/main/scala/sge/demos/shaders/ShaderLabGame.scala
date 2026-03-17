/*
 * SGE Demo — Shader Lab
 * A custom shader effects demo with a procedural checkerboard texture,
 * fullscreen quad mesh, and three switchable GLSL fragment effects.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package shaders

import scala.compiletime.uninitialized

import _root_.sge.{Pixels, Sge}
import _root_.sge.graphics.{ClearMask, Color, GL20, Mesh, Pixmap, PrimitiveMode, Texture, VertexAttribute, VertexAttributes}
import _root_.sge.graphics.glutils.ShaderProgram
import _root_.sge.graphics.profiling.GLProfiler
import _root_.sge.graphics.glutils.ShapeRenderer
import _root_.sge.graphics.OrthographicCamera
import _root_.sge.utils.viewport.ScreenViewport
import _root_.sge.Input
import sge.demos.shared.DemoScene

class ShaderLabGame extends DemoScene {

  val name: String = "Shader Lab"

  private val CheckerSize = 32
  private val TexSize     = 256
  private val NumEffects  = 3

  private var checkerPixmap:  Pixmap          = uninitialized
  private var checkerTexture: Texture         = uninitialized
  private var mesh:           Mesh            = uninitialized
  private var shaders:        Array[ShaderProgram] = uninitialized
  private var shapeRenderer:  ShapeRenderer   = uninitialized
  private var viewport:       ScreenViewport  = uninitialized
  private var profiler:       GLProfiler      = uninitialized

  private var currentEffect:  Int     = 0
  private var time:           Float   = 0f
  private var tabWasPressed:  Boolean = false
  private var touchWasDown:   Boolean = false

  private val effectColors = Array(
    Color(0.2f, 0.6f, 1.0f, 1f),
    Color(0.6f, 0.6f, 0.6f, 1f),
    Color(1.0f, 0.3f, 0.5f, 1f)
  )

  // --- GLSL Shaders (ES 100 for WebGL compat) ---

  private val vertexSrc: String =
    """#ifdef GL_ES
      |precision mediump float;
      |#endif
      |attribute vec4 a_position;
      |attribute vec2 a_texCoord0;
      |varying vec2 v_texCoord;
      |void main() {
      |  v_texCoord = a_texCoord0;
      |  gl_Position = a_position;
      |}""".stripMargin

  private val fragmentWave: String =
    """#ifdef GL_ES
      |precision mediump float;
      |#endif
      |varying vec2 v_texCoord;
      |uniform sampler2D u_texture;
      |uniform float u_time;
      |void main() {
      |  vec2 uv = v_texCoord;
      |  uv.x += sin(uv.y * 10.0 + u_time * 3.0) * 0.03;
      |  uv.y += cos(uv.x * 10.0 + u_time * 2.0) * 0.03;
      |  gl_FragColor = texture2D(u_texture, uv);
      |}""".stripMargin

  private val fragmentGray: String =
    """#ifdef GL_ES
      |precision mediump float;
      |#endif
      |varying vec2 v_texCoord;
      |uniform sampler2D u_texture;
      |uniform float u_time;
      |void main() {
      |  vec4 color = texture2D(u_texture, v_texCoord);
      |  float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
      |  float blend = 0.5 + 0.5 * sin(u_time * 2.0);
      |  gl_FragColor = vec4(mix(color.rgb, vec3(lum), blend), color.a);
      |}""".stripMargin

  private val fragmentInvert: String =
    """#ifdef GL_ES
      |precision mediump float;
      |#endif
      |varying vec2 v_texCoord;
      |uniform sampler2D u_texture;
      |uniform float u_time;
      |void main() {
      |  vec4 color = texture2D(u_texture, v_texCoord);
      |  float pulse = 0.5 + 0.5 * sin(u_time * 4.0);
      |  vec3 inverted = vec3(1.0) - color.rgb;
      |  gl_FragColor = vec4(mix(color.rgb, inverted, pulse), color.a);
      |}""".stripMargin

  override def init()(using Sge): Unit = {
    // --- Procedural checkerboard texture ---
    checkerPixmap = Pixmap(TexSize, TexSize, Pixmap.Format.RGBA8888)
    val colorA = Color(0.9f, 0.4f, 0.1f, 1f)
    val colorB = Color(0.1f, 0.2f, 0.6f, 1f)
    for (cy <- 0 until TexSize / CheckerSize; cx <- 0 until TexSize / CheckerSize) {
      val c = if ((cx + cy) % 2 == 0) colorA else colorB
      checkerPixmap.setColor(c)
      checkerPixmap.fillRectangle(cx * CheckerSize, cy * CheckerSize, CheckerSize, CheckerSize)
    }
    checkerTexture = Texture(checkerPixmap)

    // --- Fullscreen quad mesh (two triangles, position + texcoord) ---
    mesh = Mesh(
      true, 4, 6
    )(
      VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
      VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
    )

    // Vertices: x, y, u, v
    mesh.setVertices(Array[Float](
      -1f, -1f, 0f, 0f,
       1f, -1f, 1f, 0f,
       1f,  1f, 1f, 1f,
      -1f,  1f, 0f, 1f
    ))
    mesh.setIndices(Array[Short](0, 1, 2, 2, 3, 0))

    // --- Compile shaders ---
    shaders = Array(
      ShaderProgram(vertexSrc, fragmentWave),
      ShaderProgram(vertexSrc, fragmentGray),
      ShaderProgram(vertexSrc, fragmentInvert)
    )
    for (s <- shaders) {
      if (!s.compiled) {
        System.err.println("Shader compilation failed: " + s.getLog())
      }
    }

    // --- HUD / indicator ---
    shapeRenderer = ShapeRenderer()
    viewport = ScreenViewport()

    // --- GL profiler ---
    profiler = GLProfiler(Sge().graphics)
    profiler.enable()
  }

  override def render(dt: Float)(using Sge): Unit = {
    time += dt
    profiler.reset()

    handleInput()

    val gl = Sge().graphics.gl20
    gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
    gl.glClear(ClearMask.ColorBufferBit)

    // --- Draw textured quad with current shader ---
    val shader = shaders(currentEffect)
    if (shader.compiled) {
      checkerTexture.bind()
      shader.bind()
      shader.setUniformi("u_texture", 0)
      shader.setUniformf("u_time", time)
      mesh.render(shader, PrimitiveMode.Triangles)
    }

    // --- HUD: effect indicator (colored rectangles) ---
    viewport.apply()
    val cam = viewport.camera.asInstanceOf[OrthographicCamera]
    cam.update()
    shapeRenderer.setProjectionMatrix(cam.combined)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

    val screenW = Sge().graphics.getWidth().toFloat
    val barW    = 40f
    val barH    = 12f
    val gap     = 6f
    val startX  = 10f
    val startY  = 10f

    // Draw a small rectangle for each effect; highlight the active one
    for (i <- 0 until NumEffects) {
      val x = startX + i * (barW + gap)
      if (i == currentEffect) {
        shapeRenderer.setColor(effectColors(i))
        shapeRenderer.rectangle(x, startY, barW, barH)
      } else {
        shapeRenderer.setColor(Color(0.3f, 0.3f, 0.3f, 1f))
        shapeRenderer.rectangle(x, startY, barW, barH)
      }
    }

    // Draw call count as a vertical bar (right side)
    val drawCalls = profiler.drawCalls
    val barHeight = drawCalls * 10f
    shapeRenderer.setColor(Color(0.2f, 0.9f, 0.2f, 1f))
    shapeRenderer.rectangle(screenW - 30f, startY, 16f, barHeight)

    shapeRenderer.end()
  }

  private def handleInput()(using Sge): Unit = {
    val input = Sge().input

    // Tab or touch: cycle shader effect
    val tabDown = input.isKeyPressed(Input.Keys.TAB)
    if (tabDown && !tabWasPressed) {
      currentEffect = (currentEffect + 1) % NumEffects
    }
    tabWasPressed = tabDown

    val touched = input.isTouched()
    if (touched && !touchWasDown) {
      currentEffect = (currentEffect + 1) % NumEffects
    }
    touchWasDown = touched
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    profiler.disable()
    shapeRenderer.close()
    for (s <- shaders) { s.close() }
    mesh.close()
    checkerTexture.close()
    checkerPixmap.close()
  }
}
