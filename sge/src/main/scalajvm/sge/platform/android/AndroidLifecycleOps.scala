// SGE — Android application lifecycle operations interface
//
// Self-contained (JDK types only). Abstracts the Android Activity/Fragment
// lifecycle hooks and UI thread posting. Implemented in sge-jvm-platform-android.

package sge
package platform
package android

/** Lifecycle operations for Android applications. Uses only JDK types.
  *
  * Wraps the Android Activity or Fragment lifecycle so sge core can respond to pause/resume/destroy without depending on android.* classes. The Android-side Activity/Fragment creates this ops
  * instance and calls its lifecycle methods from the corresponding Activity/Fragment callbacks.
  */
trait AndroidLifecycleOps {

  // ── UI thread ─────────────────────────────────────────────────────────

  /** Post a runnable to the Android UI thread. */
  def runOnUiThread(runnable: Runnable): Unit

  // ── Window / Display ──────────────────────────────────────────────────

  /** Sets immersive mode (full-screen with hidden system bars).
    * @param use
    *   true to enable immersive mode, false to disable
    */
  def useImmersiveMode(use: Boolean): Unit

  /** Returns the Android API level (Build.VERSION.SDK_INT). */
  def getAndroidVersion(): Int

  /** Returns native heap memory usage in bytes (from android.os.Debug). */
  def getNativeHeapAllocatedSize(): Long

  // ── Activity control ──────────────────────────────────────────────────

  /** Requests the Activity/Fragment to finish (triggers onPause → onDestroy). */
  def finish(): Unit

  /** Whether a hardware keyboard is currently attached. */
  def hasHardwareKeyboard(): Boolean

  // ── GL surface ────────────────────────────────────────────────────────

  /** Store the GL surface view ops for lifecycle management (pause/resume). */
  def setGLSurfaceView(view: GLSurfaceViewOps): Unit

  /** Get the GL surface view (as AnyRef) for setting as content view. */
  def getGLSurfaceView(): AnyRef | Null

  /** Resume the GL surface view rendering. */
  def resumeGLSurfaceView(): Unit

  /** Pause the GL surface view rendering. */
  def pauseGLSurfaceView(): Unit
}
