/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 102
 * Covenant-baseline-methods: VfxFrameBufferRenderer,close,fragSrc,getMesh,mesh,rebind,renderToFbo,renderToScreen,shader,vertSrc
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package framebuffer

import sge.graphics.Texture
import sge.graphics.glutils.ShaderProgram
import sge.vfx.utils.ViewportQuadMesh

/** Simple renderer that is capable of drawing [[VfxFrameBuffer]]'s texture onto the screen or into another buffer.
  *
  * This is a lightweight [[sge.graphics.g2d.SpriteBatch]] replacement for the library's needs.
  */
class VfxFrameBufferRenderer(using Sge) extends AutoCloseable {

  private val mesh: ViewportQuadMesh = ViewportQuadMesh()

  private val shader: ShaderProgram = {
    val vertSrc =
      "#ifdef GL_ES\n" +
        "    #define PRECISION mediump\n" +
        "    precision PRECISION float;\n" +
        "#else\n" +
        "    #define PRECISION\n" +
        "#endif\n" +
        "attribute vec4 a_position;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = a_position;\n" +
        "}"
    val fragSrc =
      "#ifdef GL_ES\n" +
        "    #define PRECISION mediump\n" +
        "    precision PRECISION float;\n" +
        "#else\n" +
        "    #define PRECISION\n" +
        "#endif\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture0;\n" +
        "void main() {\n" +
        "    gl_FragColor = texture2D(u_texture0, v_texCoords);\n" +
        "}"
    ShaderProgram(vertSrc, fragSrc)
  }

  rebind()

  override def close(): Unit = {
    shader.close()
    mesh.close()
  }

  def rebind(): Unit = {
    shader.bind()
    shader.setUniformi("u_texture0", 0)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def renderToScreen(srcBuf: VfxFrameBuffer): Unit =
    renderToScreen(srcBuf.texture.get)

  def renderToScreen(srcTexture: Texture): Unit =
    renderToScreen(srcTexture, 0, 0, Sge().graphics.backBufferWidth.toInt, Sge().graphics.backBufferHeight.toInt)

  def renderToScreen(srcBuf: VfxFrameBuffer, x: Int, y: Int, width: Int, height: Int): Unit =
    renderToScreen(srcBuf.texture.get, x, y, width, height)

  def renderToScreen(srcTexture: Texture, x: Int, y: Int, width: Int, height: Int): Unit = {
    srcTexture.bind(0)

    // Update viewport to fit the area specified.
    Sge().graphics.gl20.glViewport(Pixels(x), Pixels(y), Pixels(width), Pixels(height))

    shader.bind()
    mesh.render(shader)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def renderToFbo(srcBuf: VfxFrameBuffer, dstBuf: VfxFrameBuffer): Unit =
    renderToFbo(srcBuf.texture.get, dstBuf)

  def renderToFbo(srcTexture: Texture, dstBuf: VfxFrameBuffer): Unit = {
    srcTexture.bind(0)

    // Viewport will be set from VfxFrameBuffer#begin() method.

    dstBuf.begin()
    shader.bind()
    mesh.render(shader)
    Sge().graphics.gl20.glUseProgram(0)
    dstBuf.end()
  }

  def getMesh: ViewportQuadMesh = mesh
}
