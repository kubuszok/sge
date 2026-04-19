/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 264
 * Covenant-baseline-methods: BlurType,Convolve1DEffect,Convolve2DEffect,GaussianBlurEffect,Tap,_amount,_passes,_type,amount,amount_,blurType,close,computeBlurWeightings,computeKernel,computeOffsets,convolve,hor,i,invHeight,invWidth,j,length,offsetsHor,offsetsVert,passes,passes_,rebind,render,resize,setType,sigma,sigmaRoot,size,this,total,twoSigmaSquare,update,vert,weights
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class GaussianBlurEffect(using Sge) extends AbstractVfxEffect with ChainVfxEffect {

  import GaussianBlurEffect.*

  private var _type:   BlurType = BlurType.Gaussian5x5
  private var _amount: Float    = 1f
  private var _passes: Int      = 1

  private var invWidth:  Float            = 0f
  private var invHeight: Float            = 0f
  private var convolve:  Convolve2DEffect = Convolve2DEffect(_type.tap.radius)

  def this(blurType: GaussianBlurEffect.BlurType)(using Sge) = {
    this()
    setType(blurType)
  }

  override def close(): Unit =
    convolve.close()

  override def resize(width: Int, height: Int): Unit = {
    this.invWidth = 1f / width.toFloat
    this.invHeight = 1f / height.toFloat
    convolve.resize(width, height)
    computeBlurWeightings()
  }

  override def rebind(): Unit = {
    convolve.rebind()
    computeBlurWeightings()
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit = {
    var i = 0
    while (i < _passes) {
      convolve.render(context, buffers)
      if (i < _passes - 1) {
        buffers.swap()
      }
      i += 1
    }
  }

  override def update(delta: Float): Unit = {
    // Do nothing.
  }

  def blurType: BlurType = _type

  def setType(blurType: BlurType): Unit = {
    require(blurType != null, "Blur type cannot be null.") // @nowarn — Java interop boundary
    if (this._type != blurType) {
      this._type = blurType
      if (convolve != null) { // @nowarn — init guard
        convolve.close()
      }
      convolve = Convolve2DEffect(_type.tap.radius)
      computeBlurWeightings()
    }
  }

  /** Warning: Not all blur types support custom amounts at this time */
  def amount:                 Float = _amount
  def amount_=(value: Float): Unit  = {
    _amount = value
    computeBlurWeightings()
  }

  def passes:               Int  = _passes
  def passes_=(value: Int): Unit = {
    require(value >= 1, "Passes should be greater than 0.")
    _passes = value
  }

  private def computeBlurWeightings(): Unit =
    if (convolve == null) { // @nowarn — init guard
      // do nothing
    } else {
      var hasData = true

      val outWeights  = convolve.weights
      val outOffsetsH = convolve.offsetsHor
      val outOffsetsV = convolve.offsetsVert

      val dx = this.invWidth
      val dy = this.invHeight

      _type match {
        case BlurType.Gaussian3x3 | BlurType.Gaussian5x5 =>
          computeKernel(_type.tap.radius, _amount, outWeights)
          computeOffsets(_type.tap.radius, invWidth, invHeight, outOffsetsH, outOffsetsV)

        case BlurType.Gaussian3x3b =>
          outWeights(0) = 0.352941f
          outWeights(1) = 0.294118f
          outWeights(2) = 0.352941f
          outOffsetsH(0) = -1.33333f; outOffsetsH(1) = 0f; outOffsetsH(2) = 0f
          outOffsetsH(3) = 0f; outOffsetsH(4) = 1.33333f; outOffsetsH(5) = 0f
          outOffsetsV(0) = 0f; outOffsetsV(1) = -1.33333f; outOffsetsV(2) = 0f
          outOffsetsV(3) = 0f; outOffsetsV(4) = 0f; outOffsetsV(5) = 1.33333f
          var i = 0
          while (i < convolve.length * 2) {
            outOffsetsH(i) *= dx
            outOffsetsV(i) *= dy
            i += 1
          }

        case BlurType.Gaussian5x5b =>
          outWeights(0) = 0.0702703f
          outWeights(1) = 0.316216f
          outWeights(2) = 0.227027f
          outWeights(3) = 0.316216f
          outWeights(4) = 0.0702703f
          outOffsetsH(0) = -3.23077f; outOffsetsH(1) = 0f
          outOffsetsH(2) = -1.38462f; outOffsetsH(3) = 0f
          outOffsetsH(4) = 0f; outOffsetsH(5) = 0f
          outOffsetsH(6) = 1.38462f; outOffsetsH(7) = 0f
          outOffsetsH(8) = 3.23077f; outOffsetsH(9) = 0f
          outOffsetsV(0) = 0f; outOffsetsV(1) = -3.23077f
          outOffsetsV(2) = 0f; outOffsetsV(3) = -1.38462f
          outOffsetsV(4) = 0f; outOffsetsV(5) = 0f
          outOffsetsV(6) = 0f; outOffsetsV(7) = 1.38462f
          outOffsetsV(8) = 0f; outOffsetsV(9) = 3.23077f
          var j = 0
          while (j < convolve.length * 2) {
            outOffsetsH(j) *= dx
            outOffsetsV(j) *= dy
            j += 1
          }

        case null => // @nowarn — exhaustivity guard
          hasData = false
      }

      if (hasData) {
        convolve.rebind()
      }
    }

  private def computeKernel(blurRadius: Int, blurAmount: Float, outKernel: Array[Float]): Unit = {
    val sigma          = blurAmount
    val twoSigmaSquare = 2.0f * sigma * sigma
    val sigmaRoot      = Math.sqrt(twoSigmaSquare * Math.PI).toFloat
    var total          = 0.0f

    var i = -blurRadius
    while (i <= blurRadius) {
      val distance = (i * i).toFloat
      val index    = i + blurRadius
      outKernel(index) = (Math.exp(-distance / twoSigmaSquare) / sigmaRoot).toFloat
      total += outKernel(index)
      i += 1
    }

    val size = (blurRadius * 2) + 1
    i = 0
    while (i < size) {
      outKernel(i) /= total
      i += 1
    }
  }

  private def computeOffsets(blurRadius: Int, dx: Float, dy: Float, outOffsetH: Array[Float], outOffsetV: Array[Float]): Unit = {
    var i = -blurRadius
    var j = 0
    while (i <= blurRadius) {
      outOffsetH(j) = i * dx
      outOffsetH(j + 1) = 0
      outOffsetV(j) = 0
      outOffsetV(j + 1) = i * dy
      i += 1
      j += 2
    }
  }
}

object GaussianBlurEffect {

  enum Tap(val radius: Int) extends java.lang.Enum[Tap] {
    case Tap3x3 extends Tap(1)
    case Tap5x5 extends Tap(2)
  }

  enum BlurType(val tap: Tap) extends java.lang.Enum[BlurType] {
    case Gaussian3x3 extends BlurType(Tap.Tap3x3)
    case Gaussian3x3b extends BlurType(Tap.Tap3x3) // R=5 (11x11, policy "higher-then-discard")
    case Gaussian5x5 extends BlurType(Tap.Tap5x5)
    case Gaussian5x5b extends BlurType(Tap.Tap5x5) // R=9 (19x19, policy "higher-then-discard")
  }

  /** Single-pass 1D convolution effect. */
  final class Convolve1DEffect(val length: Int, var weights: Array[Float], var offsets: Array[Float])(using Sge)
      extends ShaderVfxEffect(
        VfxGLUtils.compileShader(
          Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
          Sge().files.classpath("sge/vfx/shaders/convolve-1d.frag"),
          "#define LENGTH " + length
        )
      )
      with ChainVfxEffect {

    import ShaderVfxEffect.*

    def this(length: Int)(using Sge) =
      this(length, new Array[Float](length), new Array[Float](length * 2))

    def this(length: Int, weightsData: Array[Float])(using Sge) =
      this(length, weightsData, new Array[Float](length * 2))

    rebind()

    override def rebind(): Unit = {
      super.rebind()
      program.bind()
      program.setUniformi("u_texture0", TEXTURE_HANDLE0)
      program.setUniform2fv("u_sampleOffsets", offsets, 0, length * 2) // LibGDX asks for number of floats, NOT number of elements.
      program.setUniform1fv("u_sampleWeights", weights, 0, length)
      Sge().graphics.gl20.glUseProgram(0)
    }

    override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
      render(context, buffers.srcBuffer, buffers.dstBuffer)

    def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
      // Bind src buffer's texture as a primary one.
      src.texture.get.bind(TEXTURE_HANDLE0)
      // Apply shader effect.
      renderShader(context, dst)
    }
  }

  /** Encapsulates a separable 2D convolution kernel filter. */
  final class Convolve2DEffect(val radius: Int)(using Sge) extends CompositeVfxEffect with ChainVfxEffect {

    val length: Int = (radius * 2) + 1

    private val hor:  Convolve1DEffect = register(Convolve1DEffect(length))
    private val vert: Convolve1DEffect = register(Convolve1DEffect(length, hor.weights))

    val weights:     Array[Float] = hor.weights
    val offsetsHor:  Array[Float] = hor.offsets
    val offsetsVert: Array[Float] = vert.offsets

    override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit = {
      hor.render(context, buffers)
      buffers.swap()
      vert.render(context, buffers)
    }
  }
}
