// SGE — Android GL surface view implementation
//
// Wraps android.opengl.GLSurfaceView with custom EGL context factory,
// config chooser, and resolution strategy.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.surfaceview.GLSurfaceView20
//            com.badlogic.gdx.backends.android.surfaceview.GdxEglConfigChooser
//   Renames: GLSurfaceView20 → AndroidGLSurfaceViewImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.content.Context
import _root_.android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.{ EGL10, EGLConfig, EGLContext, EGLDisplay }

class AndroidGLSurfaceViewImpl(
  context:            Context,
  config:             AndroidConfigOps,
  resolutionStrategy: ResolutionStrategyOps
) extends GLSurfaceViewOps {

  private val targetVersion: Int = if (config.useGL30) 3 else 2

  private val surfaceView: GLSurfaceView = new GLSurfaceView(context) {
    override protected def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
      val (w, h) = resolutionStrategy.calcMeasures(widthMeasureSpec, heightMeasureSpec)
      setMeasuredDimension(w, h)
    }
  }

  // Set up EGL context factory
  surfaceView.setEGLContextFactory(
    new GLSurfaceView.EGLContextFactory {
      private val EGL_CONTEXT_CLIENT_VERSION = 0x3098

      override def createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext = {
        var version = targetVersion
        var ctx: EGLContext = null
        var success = false

        while (ctx == null && version >= 2) {
          val attribs = Array(EGL_CONTEXT_CLIENT_VERSION, version, EGL10.EGL_NONE)
          ctx = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attribs)
          success = egl.eglGetError() == EGL10.EGL_SUCCESS
          if (!success || ctx == null) {
            ctx = null
            version -= 1
          }
        }
        if (ctx == null) {
          throw new RuntimeException("Failed to create EGL context")
        }
        ctx
      }

      override def destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext): Unit =
        egl.eglDestroyContext(display, context)
    }
  )

  // Set EGL config chooser with MSAA/CSAA support
  surfaceView.setEGLConfigChooser(
    new AndroidEglConfigChooser(config.r, config.g, config.b, config.a, config.depth, config.stencil, config.numSamples)
  )

  private var _glEsVersion: Int = targetVersion

  override def view: AnyRef = surfaceView

  override def onPause(): Unit = surfaceView.onPause()

  override def onResume(): Unit = surfaceView.onResume()

  override def requestRender(): Unit = surfaceView.requestRender()

  override def setContinuousRendering(continuous: Boolean): Unit = {
    val mode = if (continuous) GLSurfaceView.RENDERMODE_CONTINUOUSLY else GLSurfaceView.RENDERMODE_WHEN_DIRTY
    surfaceView.setRenderMode(mode)
  }

  override def queueEvent(runnable: Runnable): Unit = surfaceView.queueEvent(runnable)

  override def setPreserveEGLContextOnPause(preserve: Boolean): Unit =
    surfaceView.setPreserveEGLContextOnPause(preserve)

  override def setFocusable(focusable: Boolean): Unit = {
    surfaceView.setFocusable(focusable)
    surfaceView.setFocusableInTouchMode(focusable)
  }

  override def glEsVersion: Int = _glEsVersion

  override def checkGL20Support: Boolean = {
    val egl     = EGLContext.getEGL.asInstanceOf[EGL10]
    val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
    val version = new Array[Int](2)
    egl.eglInitialize(display, version)

    val EGL_OPENGL_ES2_BIT = 4
    val configAttribs      = Array(
      EGL10.EGL_RED_SIZE,
      4,
      EGL10.EGL_GREEN_SIZE,
      4,
      EGL10.EGL_BLUE_SIZE,
      4,
      EGL10.EGL_RENDERABLE_TYPE,
      EGL_OPENGL_ES2_BIT,
      EGL10.EGL_NONE
    )

    val configs   = new Array[EGLConfig](10)
    val numConfig = new Array[Int](1)
    egl.eglChooseConfig(display, configAttribs, configs, 10, numConfig)
    egl.eglTerminate(display)
    numConfig(0) > 0
  }
}

/** EGL config chooser with MSAA/CSAA support.
  *
  * Migration notes: Source: com.badlogic.gdx.backends.android.surfaceview.GdxEglConfigChooser Renames: GdxEglConfigChooser → AndroidEglConfigChooser
  */
private[android] class AndroidEglConfigChooser(
  r:          Int,
  g:          Int,
  b:          Int,
  a:          Int,
  depth:      Int,
  stencil:    Int,
  numSamples: Int
) extends GLSurfaceView.EGLConfigChooser {

  private val EGL_OPENGL_ES2_BIT      = 4
  private val EGL_COVERAGE_BUFFERS_NV = 0x30e0
  private val EGL_COVERAGE_SAMPLES_NV = 0x30e1

  private val configAttribs = Array(
    EGL10.EGL_RED_SIZE,
    4,
    EGL10.EGL_GREEN_SIZE,
    4,
    EGL10.EGL_BLUE_SIZE,
    4,
    EGL10.EGL_RENDERABLE_TYPE,
    EGL_OPENGL_ES2_BIT,
    EGL10.EGL_NONE
  )

  private val value = new Array[Int](1)

  override def chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig = {
    val numConfig = new Array[Int](1)
    egl.eglChooseConfig(display, configAttribs, null, 0, numConfig)
    if (numConfig(0) <= 0) {
      throw new IllegalArgumentException("No configs match configSpec")
    }
    val configs = new Array[EGLConfig](numConfig(0))
    egl.eglChooseConfig(display, configAttribs, configs, numConfig(0), numConfig)
    selectConfig(egl, display, configs)
  }

  private def selectConfig(egl: EGL10, display: EGLDisplay, configs: Array[EGLConfig]): EGLConfig = {
    var best:   EGLConfig = null
    var bestAA: EGLConfig = null
    var safe:   EGLConfig = null

    for (config <- configs) {
      val d = findAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0)
      val s = findAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0)
      if (d >= depth && s >= stencil) {
        val cr = findAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0)
        val cg = findAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0)
        val cb = findAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0)
        val ca = findAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0)

        // RGB565 fallback
        if (safe == null && cr == 5 && cg == 6 && cb == 5 && ca == 0) {
          safe = config
        }
        // Exact match
        if (best == null && cr == r && cg == g && cb == b && ca == a) {
          best = config
          if (numSamples == 0) return best
        }
        // MSAA check
        val hasSB = findAttrib(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS, 0)
        val ns    = findAttrib(egl, display, config, EGL10.EGL_SAMPLES, 0)
        if (
          bestAA == null && hasSB == 1 && ns >= numSamples &&
          cr == r && cg == g && cb == b && ca == a
        ) {
          bestAA = config
        } else {
          // CSAA (NVidia coverage sampling)
          val hasCB = findAttrib(egl, display, config, EGL_COVERAGE_BUFFERS_NV, 0)
          val cns   = findAttrib(egl, display, config, EGL_COVERAGE_SAMPLES_NV, 0)
          if (
            bestAA == null && hasCB == 1 && cns >= numSamples &&
            cr == r && cg == g && cb == b && ca == a
          ) {
            bestAA = config
          }
        }
      }
    }
    if (bestAA != null) bestAA
    else if (best != null) best
    else safe
  }

  private def findAttrib(egl: EGL10, display: EGLDisplay, config: EGLConfig, attrib: Int, default: Int): Int =
    if (egl.eglGetConfigAttrib(display, config, attrib, value)) value(0)
    else default
}
