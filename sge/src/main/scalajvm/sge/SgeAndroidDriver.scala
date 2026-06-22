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

  /** Forward of `GLSurfaceView.Renderer.onSurfaceCreated()`. Sets up the [[AndroidGraphics]] GL instances from the detected GL strings, then materializes the [[Sge]] context — in THAT order (setupGL
    * before initializeSge, mirroring the former host-Activity sequence). Operates through [[AndroidApplication.setupGraphicsGL]], which holds the graphics typed, so no `asInstanceOf[AndroidGraphics]`
    * is needed here or in the host shell.
    *
    * @param version
    *   the GL_VERSION string
    * @param vendor
    *   the GL_VENDOR string
    * @param renderer
    *   the GL_RENDERER string
    */
  def onSurfaceCreated(version: String, vendor: String, renderer: String): Unit =
    app.setupGraphicsGL(version, vendor, renderer)

  /** Forward of `GLSurfaceView.Renderer.onSurfaceChanged()`. Updates the [[AndroidGraphics]] back-buffer dimensions first, then on the FIRST call drives `listener.create()` exactly once before
    * resizing; on every call drives `listener.resize(width, height)` (SmokeActivity:83-87). The graphics resize precedes the listener resize so the game observes the new dimensions during
    * `create()`/`resize()`.
    */
  def onSurfaceChanged(width: Int, height: Int): Unit = {
    app.resizeGraphics(width, height)
    if (!created) {
      app.listener.create()
      created = true
    }
    app.listener.resize(Pixels(width), Pixels(height))
  }

  /** Forward of `GLSurfaceView.Renderer.onDrawFrame()`. Drains queued input, executes posted runnables, and advances graphics frame timing every frame; but drives the per-frame hooks and
    * `listener.render()` ONLY once the first `onSurfaceChanged()` has driven `create()` (i.e. once `created` is true). The GL thread can legitimately fire a frame before the surface is configured,
    * and rendering before `create()` would force every game to null-guard its un-created state (ISS-554, the render-before-create race). Order, post-create: processInputEvents -> executeRunnables ->
    * graphics frame timing -> per-frame hooks -> render (SmokeActivity:93-99). The input drain and runnable execution still run pre-create so queued input is observed on the first rendered frame.
    */
  def onDrawFrame(): Unit = {
    app.processInputEvents()
    app.executeRunnables()
    app.updateGraphicsFrameTiming()
    if (created) {
      // Drive per-frame hooks (SGE-original) before render so polling-based
      // subsystems (e.g. the controllers extension) refresh their state and the
      // game observes it during this frame's render(). Gated by `created` so the
      // render-before-create race (ISS-554) cannot drive render() (nor the hooks
      // feeding it) before listener.create() has fired.
      app.runFrameHooks()
      app.listener.render()
    }
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
