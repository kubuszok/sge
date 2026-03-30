/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.math.Vector2
import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class CrtEffect(lineStyle: CrtEffect.LineStyle, brightnessMin: Float, brightnessMax: Float)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/crt.frag"),
        "#define SL_BRIGHTNESS_MIN " + brightnessMin + "\n" +
          "#define SL_BRIGHTNESS_MAX " + brightnessMax + "\n" +
          "#define LINE_TYPE " + lineStyle.ordinal
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private val viewportSize: Vector2 = Vector2()
  private var _sizeSource: CrtEffect.SizeSource = CrtEffect.SizeSource.VIEWPORT

  def this()(using Sge) = this(CrtEffect.LineStyle.HORIZONTAL_HARD, 1.3f, 0.5f)

  rebind()

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    viewportSize.set(width.toFloat, height.toFloat)
    rebind()
  }

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    _sizeSource match {
      case CrtEffect.SizeSource.VIEWPORT =>
        program.setUniformf("u_resolution", viewportSize)
      case CrtEffect.SizeSource.SCREEN =>
        program.setUniformf("u_resolution", Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)
    }
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def sizeSource: CrtEffect.SizeSource = _sizeSource

  /** Set shader resolution parameter source.
    * @see
    *   CrtEffect.SizeSource
    */
  def sizeSource_=(value: CrtEffect.SizeSource): Unit = {
    if (this._sizeSource != value) {
      _sizeSource = value
      rebind()
    }
  }
}

object CrtEffect {
  /** Constant name/ordinal values match the respected #define constants from crt.frag */
  enum LineStyle extends java.lang.Enum[LineStyle] {
    case CROSSLINE_HARD, VERTICAL_HARD, HORIZONTAL_HARD, VERTICAL_SMOOTH, HORIZONTAL_SMOOTH
  }

  /** Shader resolution parameter source. */
  enum SizeSource extends java.lang.Enum[SizeSource] {
    /** Resolution will be resolved from the application internal viewport. */
    case VIEWPORT
    /** Resolution will be resolved from the application window size. */
    case SCREEN
  }
}
