// SGE — Live wallpaper application callbacks interface
//
// Callbacks invoked by the WallpaperService engine on the SGE-side
// Application when surface, visibility, offsets, or preview state change.
// Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidLiveWallpaper + AndroidLiveWallpaperService
//   Convention: ops interface pattern; callback trait
//   Audited: 2026-03-08

package sge
package platform
package android

/** Callbacks from the Android WallpaperService engine to the SGE Application.
  *
  * The sge-side live wallpaper Application implements this trait. The Android-side service invokes these methods from the WallpaperEngine lifecycle to drive surface creation, visibility changes,
  * offset updates, etc.
  */
trait LiveWallpaperAppCallbacks {

  // ── Surface lifecycle ─────────────────────────────────────────────────

  /** Called when the engine's surface is first created.
    * @param holder
    *   the SurfaceHolder (as AnyRef)
    * @param isFirstEngine
    *   true if this is the first engine (app should initialize)
    */
  def onSurfaceCreated(holder: AnyRef, isFirstEngine: Boolean): Unit

  /** Called when the engine's surface format or dimensions change.
    * @param holder
    *   the SurfaceHolder (as AnyRef)
    * @param format
    *   the pixel format
    * @param width
    *   the new surface width
    * @param height
    *   the new surface height
    */
  def onSurfaceChanged(holder: AnyRef, format: Int, width: Int, height: Int): Unit

  /** Called when the engine's surface is destroyed (engine going away).
    * @param holder
    *   the SurfaceHolder (as AnyRef)
    * @param isLastEngine
    *   true if this is the last engine (trigger deep pause)
    */
  def onSurfaceDestroyed(holder: AnyRef, isLastEngine: Boolean): Unit

  // ── Visibility ────────────────────────────────────────────────────────

  /** Called when the wallpaper visibility changes.
    * @param visible
    *   true if the wallpaper becomes visible
    * @param visibleEngineCount
    *   total number of currently visible engines after this change
    */
  def onVisibilityChanged(visible: Boolean, visibleEngineCount: Int): Unit

  // ── Wallpaper-specific events ─────────────────────────────────────────

  /** Called when wallpaper offsets change (user scrolling home screens).
    * @param xOffset
    *   horizontal offset (0.0 to 1.0)
    * @param yOffset
    *   vertical offset (0.0 to 1.0)
    * @param xOffsetStep
    *   horizontal offset step between pages
    * @param yOffsetStep
    *   vertical offset step between pages
    * @param xPixelOffset
    *   horizontal pixel offset
    * @param yPixelOffset
    *   vertical pixel offset
    */
  def onOffsetsChanged(
    xOffset:      Float,
    yOffset:      Float,
    xOffsetStep:  Float,
    yOffsetStep:  Float,
    xPixelOffset: Int,
    yPixelOffset: Int
  ): Unit

  /** Called when the preview state changes (entering/exiting wallpaper preview). */
  def onPreviewStateChanged(isPreview: Boolean): Unit

  /** Called when an icon is dropped onto the wallpaper (home screen shortcut). */
  def onIconDropped(x: Int, y: Int): Unit

  // ── Deep pause ────────────────────────────────────────────────────────

  /** Called when all engines are gone and the app should release native resources. */
  def onDeepPause(): Unit

  // ── Destroy ─────────────────────────────────────────────────────────

  /** Called when the service is being destroyed. */
  def onServiceDestroy(): Unit
}
