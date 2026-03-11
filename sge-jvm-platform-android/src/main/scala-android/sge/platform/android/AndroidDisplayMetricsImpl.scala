// SGE — Android display metrics implementation
//
// Uses android.util.DisplayMetrics for PPI/density and android.view.DisplayCutout
// for safe area insets.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidGraphics (display part)
//   Renames: AndroidGraphics metrics → AndroidDisplayMetricsImpl
//   Convention: ops interface pattern; _root_.android.* imports; Build.VERSION checks
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.content.Context
import _root_.android.hardware.display.DisplayManager
import _root_.android.os.Build
import _root_.android.util.DisplayMetrics
import _root_.android.view.{ Display, WindowManager }

class AndroidDisplayMetricsImpl(windowManager: WindowManager) extends DisplayMetricsOps {

  private var _ppiX:    Float = 0f
  private var _ppiY:    Float = 0f
  private var _density: Float = 1f

  private var _safeInsetLeft:   Int = 0
  private var _safeInsetTop:    Int = 0
  private var _safeInsetRight:  Int = 0
  private var _safeInsetBottom: Int = 0

  // Initialize from current display
  updateMetrics(windowManager)

  override def ppiX:    Float = _ppiX
  override def ppiY:    Float = _ppiY
  override def ppcX:    Float = _ppiX / 2.54f
  override def ppcY:    Float = _ppiY / 2.54f
  override def density: Float = _density

  override def safeInsetLeft:   Int = _safeInsetLeft
  override def safeInsetTop:    Int = _safeInsetTop
  override def safeInsetRight:  Int = _safeInsetRight
  override def safeInsetBottom: Int = _safeInsetBottom

  override def updateMetrics(windowManager: AnyRef): Unit = {
    val wm      = windowManager.asInstanceOf[WindowManager]
    val metrics = new DisplayMetrics()
    wm.getDefaultDisplay.getMetrics(metrics)
    _ppiX = metrics.xdpi
    _ppiY = metrics.ydpi
    _density = metrics.density
  }

  override def updateSafeInsets(window: AnyRef): Unit = {
    _safeInsetLeft = 0
    _safeInsetTop = 0
    _safeInsetRight = 0
    _safeInsetBottom = 0

    if (window != null && Build.VERSION.SDK_INT >= 28) { // Build.VERSION_CODES.P
      try {
        val androidWindow = window.asInstanceOf[_root_.android.view.Window]
        val cutout        = androidWindow.getDecorView.getRootWindowInsets.getDisplayCutout
        if (cutout != null) {
          _safeInsetLeft = cutout.getSafeInsetLeft
          _safeInsetTop = cutout.getSafeInsetTop
          _safeInsetRight = cutout.getSafeInsetRight
          _safeInsetBottom = cutout.getSafeInsetBottom
        }
      } catch {
        case _: UnsupportedOperationException => // Live wallpapers, etc.
      }
    }
  }

  override def displayMode(context: AnyRef, bitsPerPixel: Int): (Int, Int, Int, Int) = {
    val ctx            = context.asInstanceOf[Context]
    val displayManager = ctx.getSystemService(classOf[DisplayManager])
    val display        = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    val metrics        = new DisplayMetrics()
    display.getRealMetrics(metrics)
    val width       = metrics.widthPixels
    val height      = metrics.heightPixels
    val refreshRate = Math.round(display.getRefreshRate)
    (width, height, refreshRate, bitsPerPixel)
  }
}
