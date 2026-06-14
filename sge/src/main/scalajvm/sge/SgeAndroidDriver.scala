/*
 * Part of the SGE Android backend.
 *
 * Migration notes:
 *   Origin: the canonical Android wiring sequence hand-copied in every game's
 *     Activity (see sge-test/android-smoke/.../SmokeActivity.scala). LibGDX bakes
 *     this loop into AndroidGraphics.onDrawFrame + AndroidApplication's Activity
 *     subclass; SGE keeps AndroidApplication a plain class and extracts the
 *     orchestration here so it can be unit-tested on a plain JVM.
 *   Convention: android-type-free — only Int/Char/AnyRef/String cross this
 *     boundary, so the scala-android SgeActivity shell stays a thin forwarder.
 *   Idiom: split packages; no return; created-once flag is a simple var.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 101
 * Covenant-baseline-methods: SgeAndroidDriver,_,created,genericMotion,keyDown,keyTyped,keyUp,onDestroy,onDrawFrame,onPause,onResume,onSurfaceChanged,touchEvent
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-14
 */
package sge

/** Drives the canonical Android lifecycle/surface/frame/input wiring for an [[AndroidApplication]].
  *
  * This is the library-level component that replaces the ~80 lines of wiring every game previously hand-copied from `SmokeActivity`. It is deliberately android-type-free: the host scala-android
  * `SgeActivity` shell translates Android framework callbacks into the plain `Int`/`Char`/`AnyRef` arguments accepted here, so this core is fully unit-testable on a plain JVM.
  *
  * The driver only ORCHESTRATES [[AndroidApplication]]'s existing public and `private[sge]` API — it never modifies the application. The `sgeContext` null-guard is essential: before
  * [[AndroidApplication.initializeSge]] runs, the listener must NOT be driven (mirrors SmokeActivity:124-125,131).
  *
  * @param app
  *   the plain-JVM application instance whose lifecycle this driver pumps
  */
final class SgeAndroidDriver(app: AndroidApplication) {

  /** Tracks whether `listener.create()` has already fired so a subsequent surface change resizes only — mirrors SmokeActivity:83-87.
    */
  private var created: Boolean = false

  /** Forward of `Activity.onResume()`. Resumes subsystems via `app.onResume()`, then — only once the [[Sge]] context has been materialized — drives `listener.resume()` (SmokeActivity:124-125).
    */
  def onResume(): Unit = {
    app.onResume()
    if (app.sgeContext != null) app.listener.resume() // scalafix:ok
  }

  /** Forward of `Activity.onPause()`. Drives `listener.pause()` first — only if the [[Sge]] context exists — then pauses subsystems via `app.onPause()` (SmokeActivity:131-132).
    */
  def onPause(): Unit = {
    if (app.sgeContext != null) app.listener.pause() // scalafix:ok
    app.onPause()
  }

  /** Forward of `Activity.onDestroy()`. Drives `listener.dispose()` first — only if the [[Sge]] context exists — then disposes subsystems via `app.onDestroy()` (SmokeActivity:105,137-140).
    */
  def onDestroy(): Unit = {
    if (app.sgeContext != null) app.listener.dispose() // scalafix:ok
    app.onDestroy()
  }

  /** Forward of `GLSurfaceView.Renderer.onSurfaceChanged()`. On the FIRST call drives `listener.create()` exactly once before resizing; on every call drives `listener.resize(width, height)`
    * (SmokeActivity:83-87).
    */
  def onSurfaceChanged(width: Int, height: Int): Unit = {
    if (!created) {
      app.listener.create()
      created = true
    }
    app.listener.resize(Pixels(width), Pixels(height))
  }

  /** Forward of `GLSurfaceView.Renderer.onDrawFrame()`. Drains queued input, executes posted runnables, then renders — IN THAT ORDER (SmokeActivity:93-99).
    */
  def onDrawFrame(): Unit = {
    app.processInputEvents()
    app.executeRunnables()
    app.listener.render()
  }

  /** Route an Android `KeyEvent.ACTION_DOWN` keycode to [[AndroidInput.onKeyDown]] (AndroidInput:167).
    */
  def keyDown(keyCode: Int): Unit = {
    val _ = app.input.asInstanceOf[AndroidInput].onKeyDown(keyCode)
  }

  /** Route an Android `KeyEvent.ACTION_UP` keycode to [[AndroidInput.onKeyUp]] (AndroidInput:182).
    */
  def keyUp(keyCode: Int): Unit = {
    val _ = app.input.asInstanceOf[AndroidInput].onKeyUp(keyCode)
  }

  /** Route a typed character to [[AndroidInput.onKeyTyped]] (AndroidInput:195). */
  def keyTyped(ch: Char): Unit =
    app.input.asInstanceOf[AndroidInput].onKeyTyped(ch)

  /** Route an Android `MotionEvent` (as `AnyRef`) to [[AndroidInput.onTouchEvent]] (AndroidInput:422).
    */
  def touchEvent(event: AnyRef): Unit =
    app.input.asInstanceOf[AndroidInput].onTouchEvent(event)

  /** Route an Android generic `MotionEvent` (as `AnyRef`) to [[AndroidInput.onGenericMotionEvent]] (AndroidInput:432).
    */
  def genericMotion(event: AnyRef): Unit = {
    val _ = app.input.asInstanceOf[AndroidInput].onGenericMotionEvent(event)
  }
}
