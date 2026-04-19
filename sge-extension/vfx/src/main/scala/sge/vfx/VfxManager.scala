/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 329
 * Covenant-baseline-methods: MAX_FRAME_BUFFER_SIDE,VfxManager,_applyingEffects,_blendingEnabled,_capturing,_disabled,_height,_width,addEffect,anyEnabledEffects,applyEffects,applyingEffects,beginInputCapture,blendingEnabled,blendingEnabled_,capturing,cleanUpBuffers,close,constrainFrameBufferSize,constrainedSize,context,disabled,disabled_,effects,endInputCapture,filterEnabledEffects,getPingPongWrapper,gl,h,height,i,pingPongWrapper,pixelFormat,rebind,removeAllEffects,removeEffect,renderContext,renderToFbo,renderToScreen,resize,resultBuffer,setEffectPriority,setEffectTextureParams,this,tmpEffectArray,update,useAsInput,w,width
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx

import sge.graphics.{ Color, EnableCap, GL20, Pixmap, Texture }
import sge.math.Vector2
import sge.utils.Scaling
import sge.vfx.effects.ChainVfxEffect
import sge.vfx.framebuffer.*
import sge.vfx.utils.PrioritizedArray

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

/** Provides a way to capture the rendered scene to an off-screen buffer and to apply a chain of effects on it before rendering to screen.
  *
  * Effects can be added or removed via [[addEffect]] and [[removeEffect]].
  *
  * @author
  *   metaphore
  */
final class VfxManager(fboFormat: Pixmap.Format, bufferWidth: Int, bufferHeight: Int)(using Sge) extends AutoCloseable {

  def this(fboFormat: Pixmap.Format)(using Sge) =
    this(fboFormat, Sge().graphics.backBufferWidth.toInt, Sge().graphics.backBufferHeight.toInt)

  private val effects:        PrioritizedArray[ChainVfxEffect] = PrioritizedArray()
  private val tmpEffectArray: ArrayBuffer[ChainVfxEffect]      = ArrayBuffer.empty

  private val context: VfxRenderContext = VfxRenderContext(fboFormat, bufferWidth, bufferHeight)

  // VfxFrameBufferPool will manage both ping-pong VfxFrameBuffer instances for us.
  private val pingPongWrapper: VfxPingPongWrapper = VfxPingPongWrapper(context.bufferPool)

  private var _capturing:       Boolean = false
  private var _disabled:        Boolean = false
  private var _applyingEffects: Boolean = false
  private var _blendingEnabled: Boolean = false

  private var _width:  Int = bufferWidth
  private var _height: Int = bufferHeight

  override def close(): Unit = {
    pingPongWrapper.reset()
    context.close()
  }

  def width:  Int = _width
  def height: Int = _height

  def capturing: Boolean = _capturing

  def disabled:                   Boolean = _disabled
  def disabled_=(value: Boolean): Unit    = _disabled = value

  def blendingEnabled:                   Boolean = _blendingEnabled
  def blendingEnabled_=(value: Boolean): Unit    = _blendingEnabled = value

  /** Returns the internal framebuffers' pixel format, computed from the parameters specified during construction. NOTE: the returned Format will be valid after construction and NOT early!
    */
  def pixelFormat: Pixmap.Format = context.pixelFormat

  def setEffectTextureParams(
    textureWrapU:     Texture.TextureWrap,
    textureWrapV:     Texture.TextureWrap,
    textureFilterMin: Texture.TextureFilter,
    textureFilterMag: Texture.TextureFilter
  ): Unit =
    context.bufferPool.setTextureParams(textureWrapU, textureWrapV, textureFilterMin, textureFilterMag)

  def applyingEffects: Boolean = _applyingEffects

  /** @return the last active destination frame buffer. */
  def resultBuffer: VfxFrameBuffer = pingPongWrapper.dstBuffer

  /** @return the internal ping-pong buffer. */
  def getPingPongWrapper: VfxPingPongWrapper = pingPongWrapper

  def renderContext: VfxRenderContext = context

  /** Adds an effect to the effect chain and transfers ownership to the VfxManager. The order of the inserted effects IS important, since effects will be applied in a FIFO fashion, the first added is
    * the first being applied.
    *
    * For more control over the order supply the effect with a priority - [[addEffect(effect:sge\.vfx\.effects\.ChainVfxEffect,priority:Int)*]].
    */
  def addEffect(effect: ChainVfxEffect): Unit =
    addEffect(effect, 0)

  def addEffect(effect: ChainVfxEffect, priority: Int): Unit = {
    effects.add(effect, priority)
    effect.resize(_width, _height)
  }

  /** Removes the specified effect from the effect chain. */
  def removeEffect(effect: ChainVfxEffect): Unit =
    effects.remove(effect)

  /** Removes all effects from the effect chain. */
  def removeAllEffects(): Unit =
    effects.clear()

  /** Changes the order of the effect in the effect chain. */
  def setEffectPriority(effect: ChainVfxEffect, priority: Int): Unit =
    effects.setPriority(effect, priority)

  /** Cleans up the [[VfxPingPongWrapper]]'s buffers with [[Color.CLEAR]]. */
  def cleanUpBuffers(): Unit = cleanUpBuffers(Color.CLEAR)

  /** Cleans up the [[VfxPingPongWrapper]]'s buffers with the color specified. */
  def cleanUpBuffers(color: Color): Unit = {
    if (_applyingEffects) throw IllegalStateException("Cannot clean up buffers when applying effects.")
    if (_capturing) throw IllegalStateException("Cannot clean up buffers when capturing a scene.")

    pingPongWrapper.cleanUpBuffers(color)
  }

  def resize(width: Int, height: Int): Unit = {
    val constrainedSize = VfxManager.constrainFrameBufferSize(width, height)
    _width = constrainedSize.x.toInt
    _height = constrainedSize.y.toInt

    context.resize(_width, _height)

    var i = 0
    while (i < effects.size) {
      effects.get(i).resize(_width, _height)
      i += 1
    }
  }

  def rebind(): Unit = {
    context.rebind()

    var i = 0
    while (i < effects.size) {
      effects.get(i).rebind()
      i += 1
    }
  }

  def update(delta: Float): Unit = {
    var i = 0
    while (i < effects.size) {
      effects.get(i).update(delta)
      i += 1
    }
  }

  /** Starts capturing the input buffer. */
  def beginInputCapture(): Unit = {
    if (_applyingEffects) {
      throw IllegalStateException("Capture is not available when VfxManager is applying the effects.")
    }
    if (_capturing) {
      // already capturing
    } else {
      _capturing = true
      pingPongWrapper.begin()
    }
  }

  /** Stops capturing the input buffer. */
  def endInputCapture(): Unit = {
    if (!_capturing) throw IllegalStateException("The capturing is not started. Forgot to call #beginInputCapture()?")

    _capturing = false
    pingPongWrapper.end()
  }

  /** Sets up a (captured?) source scene that will be used later as an input for effect processing. Updates the effect chain src buffer with the data provided.
    */
  def useAsInput(frameBuffer: VfxFrameBuffer): Unit =
    useAsInput(frameBuffer.texture.get)

  /** Sets up a (captured?) source scene that will be used later as an input for effect processing. Updates the effect chain src buffer with the data provided.
    */
  def useAsInput(texture: Texture): Unit = {
    if (_capturing) {
      throw IllegalStateException("Cannot set captured input when capture helper is currently capturing.")
    }
    if (_applyingEffects) {
      throw IllegalStateException("Cannot update the input buffer when applying effects.")
    }

    context.bufferRenderer.renderToFbo(texture, pingPongWrapper.dstBuffer)
  }

  /** Applies the effect chain. */
  def applyEffects(): Unit = {
    if (_capturing) {
      throw IllegalStateException("You should call VfxManager.endCapture() before applying the effects.")
    }

    if (_disabled) {
      // do nothing
    } else {
      val effectChain = filterEnabledEffects(tmpEffectArray)
      if (effectChain.isEmpty) {
        effectChain.clear()
      } else {
        _applyingEffects = true

        val gl = Sge().graphics.gl20

        // Enable blending to preserve buffer's alpha values.
        if (_blendingEnabled) {
          gl.glEnable(EnableCap.Blend)
        }

        gl.glDisable(EnableCap.CullFace)
        gl.glDisable(EnableCap.DepthTest)

        pingPongWrapper.swap() // Swap buffers to get the input buffer in the src buffer.
        pingPongWrapper.begin()

        // Render the effect chain.
        var i = 0
        while (i < effectChain.size) {
          val effect = effectChain(i)
          effect.render(context, pingPongWrapper)
          if (i < effectChain.size - 1) {
            pingPongWrapper.swap()
          }
          i += 1
        }
        effectChain.clear()
        pingPongWrapper.end()

        // Ensure default texture unit #0 is active.
        gl.glActiveTexture(GL20.GL_TEXTURE0)

        if (_blendingEnabled) {
          gl.glDisable(EnableCap.Blend)
        }

        _applyingEffects = false
      }
    }
  }

  def renderToScreen(): Unit = {
    if (_capturing) {
      throw IllegalStateException("You should call endCapture() before rendering the result.")
    }

    val gl = Sge().graphics.gl20
    // Enable blending to preserve buffer's alpha values.
    if (_blendingEnabled) { gl.glEnable(EnableCap.Blend) }
    context.bufferRenderer.renderToScreen(pingPongWrapper.dstBuffer)
    if (_blendingEnabled) { gl.glDisable(EnableCap.Blend) }
  }

  def renderToScreen(x: Int, y: Int, width: Int, height: Int): Unit = {
    if (_capturing) {
      throw IllegalStateException("You should call endCapture() before rendering the result.")
    }

    val gl = Sge().graphics.gl20
    // Enable blending to preserve buffer's alpha values.
    if (_blendingEnabled) { gl.glEnable(EnableCap.Blend) }
    context.bufferRenderer.renderToScreen(pingPongWrapper.dstBuffer, x, y, width, height)
    if (_blendingEnabled) { gl.glDisable(EnableCap.Blend) }
  }

  def renderToFbo(output: VfxFrameBuffer): Unit = {
    if (_capturing) {
      throw IllegalStateException("You should call endCapture() before rendering the result.")
    }

    val gl = Sge().graphics.gl20
    // Enable blending to preserve buffer's alpha values.
    if (_blendingEnabled) { gl.glEnable(EnableCap.Blend) }
    context.bufferRenderer.renderToFbo(pingPongWrapper.dstBuffer, output)
    if (_blendingEnabled) { gl.glDisable(EnableCap.Blend) }
  }

  def anyEnabledEffects: Boolean =
    boundary {
      var i = 0
      while (i < effects.size) {
        if (!effects.get(i).disabled) {
          break(true)
        }
        i += 1
      }
      false
    }

  private def filterEnabledEffects(out: ArrayBuffer[ChainVfxEffect]): ArrayBuffer[ChainVfxEffect] = {
    var i = 0
    while (i < effects.size) {
      val effect = effects.get(i)
      if (!effect.disabled) {
        out += effect
      }
      i += 1
    }
    out
  }
}

object VfxManager {

  /** The maximum side size of a frame buffer managed by any VfxManager instance. This value constrains the internal size of a VfxManager and in case width or height is greater than this value the
    * result size values will be fitted within MAX_FRAME_BUFFER_SIDE by MAX_FRAME_BUFFER_SIDE square keeping the aspect ratio.
    */
  val MAX_FRAME_BUFFER_SIDE: Int = 8192

  def constrainFrameBufferSize(width: Int, height: Int): Vector2 = {
    val w = if (width < 1) 1 else width
    val h = if (height < 1) 1 else height

    if (w <= MAX_FRAME_BUFFER_SIDE && h <= MAX_FRAME_BUFFER_SIDE) {
      Vector2(w.toFloat, h.toFloat)
    } else {
      // Fit the desired aspect ratio in the maximum size square.
      val result = Scaling.fit(w.toFloat, h.toFloat, MAX_FRAME_BUFFER_SIDE.toFloat, MAX_FRAME_BUFFER_SIDE.toFloat)
      if (result.x < 1) result.x = 1
      if (result.y < 1) result.y = 1
      result
    }
  }
}
