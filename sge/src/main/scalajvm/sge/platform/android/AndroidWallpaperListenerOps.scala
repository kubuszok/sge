// SGE — Android live wallpaper listener interface
//
// Callback for wallpaper-specific events: offset changes, preview state,
// and icon drops. Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidWallpaperListener
//   Convention: ops interface pattern
//   Audited: 2026-03-08

package sge
package platform
package android

/** Listener for live wallpaper-specific events.
  *
  * Implement this alongside your ApplicationListener if you want to receive wallpaper-specific callbacks. These will only fire when running inside an AndroidLiveWallpaperService.
  *
  * @author
  *   Jaroslaw Wisniewski (original implementation)
  */
trait AndroidWallpaperListenerOps {

  /** Called on the rendering thread after the live wallpaper's offset has changed.
    * @param xOffset
    *   horizontal offset (0..1)
    * @param yOffset
    *   vertical offset (0..1)
    * @param xOffsetStep
    *   horizontal offset step
    * @param yOffsetStep
    *   vertical offset step
    * @param xPixelOffset
    *   horizontal pixel offset
    * @param yPixelOffset
    *   vertical pixel offset
    */
  def offsetChange(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int): Unit

  /** Called after the 'isPreview' state has changed. First called just after application initialization.
    * @param isPreview
    *   true if the wallpaper is in preview mode
    */
  def previewStateChange(isPreview: Boolean): Unit

  /** Called in response to an icon being dropped on the home screen. Not all Android launchers support this. */
  def iconDropped(x: Int, y: Int): Unit
}
