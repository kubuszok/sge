// SGE — Android GL surface view operations interface
//
// Self-contained (JDK types only). Abstracts GLSurfaceView lifecycle
// (pause/resume, continuous rendering, EGL context setup).
// Implemented in sge-jvm-platform-android using android.opengl.GLSurfaceView.

package sge
package platform
package android

/** GL surface view lifecycle operations for Android. Uses only JDK types.
  *
  * The underlying view is an Android GLSurfaceView with custom EGL config chooser and resolution strategy.
  */
trait GLSurfaceViewOps {

  /** Returns the underlying Android View (as AnyRef). */
  def view: AnyRef

  /** Pauses the GL rendering thread. */
  def onPause(): Unit

  /** Resumes the GL rendering thread. */
  def onResume(): Unit

  /** Requests a single frame render (for non-continuous mode). */
  def requestRender(): Unit

  /** Sets continuous rendering mode.
    * @param continuous
    *   true for continuous, false for render-on-demand
    */
  def setContinuousRendering(continuous: Boolean): Unit

  /** Posts a runnable to be executed on the GL thread. */
  def queueEvent(runnable: Runnable): Unit

  /** Sets whether the EGL context is preserved on pause. */
  def setPreserveEGLContextOnPause(preserve: Boolean): Unit

  /** Sets the view as focusable and focusable in touch mode. */
  def setFocusable(focusable: Boolean): Unit

  /** Returns the GL ES version the surface was created with (2 or 3). */
  def glEsVersion: Int

  /** Returns whether GL ES 2.0 is supported on this device. */
  def checkGL20Support: Boolean
}
