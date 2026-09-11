// SGE — Live wallpaper service operations interface
//
// Abstracts the Android WallpaperService + Engine surface lifecycle.
// Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
//   Convention: ops interface pattern; AnyRef for Android types
//   Audited: 2026-03-08

package sge
package platform
package android

/** Operations provided by the Android WallpaperService to the SGE application.
  *
  * Abstracts the WallpaperService lifecycle so the sge-side Application can interact with the wallpaper engine without depending on android.* classes.
  */
trait LiveWallpaperServiceOps {

  /** Returns the current SurfaceHolder (as AnyRef), or null if no engine is linked. */
  def getSurfaceHolder(): AnyRef | Null

  /** Returns the Android Context (the WallpaperService itself, as AnyRef). */
  def getContext(): AnyRef

  /** Returns the Android WindowManager (as AnyRef). */
  def getWindowManager(): AnyRef

  /** Enables or disables touch events on the linked engine. */
  def setTouchEventsEnabled(enabled: Boolean): Unit

  /** Returns true if the linked engine is in preview mode. */
  def isPreview(): Boolean

  /** Returns the number of active wallpaper engines. */
  def getEngineCount(): Int

  /** Returns the number of currently visible engines. */
  def getVisibleEngineCount(): Int

  /** Registers the application callbacks that the service will invoke on lifecycle events. */
  def setAppCallbacks(callbacks: LiveWallpaperAppCallbacks): Unit
}
