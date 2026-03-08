// SGE — Android live wallpaper service implementation
//
// Extends WallpaperService to manage multiple engines sharing a single
// SGE application instance. Delegates lifecycle events to
// LiveWallpaperAppCallbacks so the sge-side Application can respond
// without depending on Android SDK types.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
//   Renames: AndroidLiveWallpaperService → AndroidLiveWallpaperServiceImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.content.Context
import _root_.android.os.Bundle
import _root_.android.service.wallpaper.WallpaperService
import _root_.android.util.Log
import _root_.android.view.{MotionEvent, SurfaceHolder, WindowManager}

/** Abstract WallpaperService that manages the lifecycle of wallpaper engines and delegates to [[LiveWallpaperAppCallbacks]].
  *
  * The inner [[WallpaperEngine]] manages surface creation/destruction and visibility. Multiple engines can exist simultaneously
  * but share a single SGE application instance. The linked engine is the one currently providing the surface.
  *
  * Subclasses must call [[setAppCallbacks]] and [[setTouchHandler]] during initialization (typically in the application
  * callbacks' surface-created handler).
  */
abstract class AndroidLiveWallpaperServiceImpl extends WallpaperService with LiveWallpaperServiceOps {

  private val Tag = "WallpaperService"

  @volatile private var callbacks: LiveWallpaperAppCallbacks = scala.compiletime.uninitialized

  // Engine tracking
  private var engines:        Int = 0
  private var visibleEngines: Int = 0

  // Currently linked engine (provides the surface)
  @volatile private var linkedEngine: WallpaperEngine = scala.compiletime.uninitialized

  // Synchronization object
  private val sync = new Array[Int](0)

  // Touch event handler (set by sge side after initialization)
  @volatile private var touchHandler: (AnyRef, AnyRef) => Unit = scala.compiletime.uninitialized // (View?, MotionEvent) => Unit

  // Surface format cache
  private var viewFormat: Int = 0
  private var viewWidth:  Int = 0
  private var viewHeight: Int = 0

  // Preview state tracking
  @volatile private var isPreviewNotified:    Boolean = false
  @volatile private var notifiedPreviewState: Boolean = false

  // ── LiveWallpaperServiceOps ────────────────────────────────────────────

  override def getSurfaceHolder(): AnyRef | Null = sync.synchronized {
    if (linkedEngine == null) null // scalafix:ok
    else linkedEngine.getSurfaceHolder()
  }

  override def getContext(): AnyRef = this

  override def getWindowManager(): AnyRef =
    getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]

  override def setTouchEventsEnabled(enabled: Boolean): Unit = {
    if (linkedEngine != null) linkedEngine.setTouchEventsEnabled(enabled)
  }

  override def isPreview(): Boolean = {
    if (linkedEngine != null) linkedEngine.isPreview() else false
  }

  override def getEngineCount(): Int = engines

  override def getVisibleEngineCount(): Int = visibleEngines

  override def setAppCallbacks(cbs: LiveWallpaperAppCallbacks): Unit = {
    callbacks = cbs
  }

  /** Sets the touch event handler. Called from the sge-side after input is initialized.
    * @param handler
    *   function that takes (view: AnyRef|Null, motionEvent: AnyRef) and forwards to input processing
    */
  def setTouchHandler(handler: (AnyRef, AnyRef) => Unit): Unit = {
    touchHandler = handler
  }

  // ── WallpaperService lifecycle ─────────────────────────────────────────

  override def onCreate(): Unit = {
    Log.i(Tag, "service created")
    super.onCreate()
  }

  override def onCreateEngine(): WallpaperService.Engine = {
    Log.i(Tag, "engine created")
    new WallpaperEngine()
  }

  override def onDestroy(): Unit = {
    Log.i(Tag, "service destroyed")
    super.onDestroy()
    if (callbacks != null) {
      callbacks.onServiceDestroy()
      callbacks = scala.compiletime.uninitialized
    }
  }

  // ── Inner Engine ──────────────────────────────────────────────────────

  /** Wallpaper engine that bridges Android surface lifecycle to [[LiveWallpaperAppCallbacks]].
    *
    * Multiple instances may exist simultaneously. The "linked" engine is the one whose surface is currently active. Engine
    * transitions (e.g. from preview to home screen) are handled by unlinking the old engine and linking the new one.
    */
  class WallpaperEngine extends WallpaperService.Engine {

    private var engineIsVisible: Boolean = false
    private var engineFormat:    Int     = 0
    private var engineWidth:     Int     = 0
    private var engineHeight:    Int     = 0

    // Icon drop state
    private var iconDropConsumed: Boolean = true
    private var xIconDrop:        Int     = 0
    private var yIconDrop:        Int     = 0

    // Offset state
    private var offsetsConsumed: Boolean = true
    private var xOffset:         Float   = 0f
    private var yOffset:         Float   = 0f
    private var xOffsetStep:     Float   = 0f
    private var yOffsetStep:     Float   = 0f
    private var xPixelOffset:    Int     = 0
    private var yPixelOffset:    Int     = 0

    override def onCreate(surfaceHolder: SurfaceHolder): Unit = {
      super.onCreate(surfaceHolder)
    }

    override def onSurfaceCreated(holder: SurfaceHolder): Unit = {
      engines += 1
      sync.synchronized { linkedEngine = this }

      Log.i(Tag, "engine surface created")
      super.onSurfaceCreated(holder)

      if (engines == 1) visibleEngines = 0

      val isFirst = engines == 1

      if (callbacks != null) {
        callbacks.onSurfaceCreated(holder, isFirst)
      }

      // Inherit format from shared view
      engineFormat = viewFormat
      engineWidth = viewWidth
      engineHeight = viewHeight

      notifyPreviewState()
      notifyOffsetsChanged()
    }

    override def onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int): Unit = {
      Log.i(Tag, "engine surface changed")
      super.onSurfaceChanged(holder, format, width, height)

      engineFormat = format
      engineWidth = width
      engineHeight = height

      if (linkedEngine == this) {
        viewFormat = engineFormat
        viewWidth = engineWidth
        viewHeight = engineHeight
        if (callbacks != null) {
          callbacks.onSurfaceChanged(holder, viewFormat, viewWidth, viewHeight)
        }
      }
    }

    override def onVisibilityChanged(visible: Boolean): Unit = {
      val reportedVisible = isVisible()
      super.onVisibilityChanged(visible)

      // Filter fake visibility events from Android WallpaperService
      if (!reportedVisible && visible) return

      notifyVisibilityChanged(visible)
    }

    private def notifyVisibilityChanged(visible: Boolean): Unit = {
      if (engineIsVisible != visible) {
        engineIsVisible = visible
        if (engineIsVisible) onResume()
        else onPause()
      }
    }

    private def onResume(): Unit = {
      visibleEngines += 1
      Log.i(Tag, "engine resumed")

      if (linkedEngine != null) {
        if (linkedEngine != this) {
          sync.synchronized { linkedEngine = this }
          // Surface transition: the new engine takes over
          if (callbacks != null) {
            callbacks.onSurfaceDestroyed(getSurfaceHolder(), false)
            callbacks.onSurfaceCreated(getSurfaceHolder(), false)
          }
        }

        if (callbacks != null) {
          callbacks.onVisibilityChanged(true, visibleEngines)
        }

        notifyPreviewState()
        notifyOffsetsChanged()
      }
    }

    private def onPause(): Unit = {
      visibleEngines -= 1
      Log.i(Tag, "engine paused")

      // Safety: fix over-count
      if (visibleEngines >= engines) {
        Log.e(Tag, "wallpaper lifecycle error, counted too many visible engines! repairing..")
        visibleEngines = Math.max(engines - 1, 0)
      }

      if (callbacks != null) {
        callbacks.onVisibilityChanged(false, visibleEngines)
      }
    }

    override def onSurfaceDestroyed(holder: SurfaceHolder): Unit = {
      engines -= 1
      Log.i(Tag, "engine surface destroyed")

      if (engines == 0 && callbacks != null) callbacks.onDeepPause()

      if (linkedEngine == this && callbacks != null) {
        callbacks.onSurfaceDestroyed(holder, engines == 0)
      }

      engineFormat = 0
      engineWidth = 0
      engineHeight = 0

      if (engines == 0) sync.synchronized { linkedEngine = scala.compiletime.uninitialized }

      super.onSurfaceDestroyed(holder)
    }

    override def onCommand(action: String, x: Int, y: Int, z: Int, extras: Bundle, resultRequested: Boolean): Bundle = {
      if (action == "android.home.drop") {
        iconDropConsumed = false
        xIconDrop = x
        yIconDrop = y
        notifyIconDropped()
      }
      super.onCommand(action, x, y, z, extras, resultRequested)
    }

    private def notifyIconDropped(): Unit = {
      if (linkedEngine == this && callbacks != null && !iconDropConsumed) {
        iconDropConsumed = true
        callbacks.onIconDropped(xIconDrop, yIconDrop)
      }
    }

    override def onTouchEvent(event: MotionEvent): Unit = {
      if (linkedEngine == this && touchHandler != null) {
        touchHandler(null, event) // scalafix:ok — null view is normal for wallpaper touch
      }
    }

    override def onOffsetsChanged(
      xOffset:      Float,
      yOffset:      Float,
      xOffsetStep:  Float,
      yOffsetStep:  Float,
      xPixelOffset: Int,
      yPixelOffset: Int
    ): Unit = {
      this.offsetsConsumed = false
      this.xOffset = xOffset
      this.yOffset = yOffset
      this.xOffsetStep = xOffsetStep
      this.yOffsetStep = yOffsetStep
      this.xPixelOffset = xPixelOffset
      this.yPixelOffset = yPixelOffset

      notifyOffsetsChanged()

      super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
    }

    private def notifyOffsetsChanged(): Unit = {
      if (linkedEngine == this && callbacks != null && !offsetsConsumed) {
        offsetsConsumed = true
        callbacks.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
      }
    }

    private def notifyPreviewState(): Unit = {
      if (linkedEngine == this && callbacks != null) {
        val currentPreviewState = linkedEngine.isPreview()
        sync.synchronized {
          if (!isPreviewNotified || notifiedPreviewState != currentPreviewState) {
            notifiedPreviewState = currentPreviewState
            isPreviewNotified = true
            callbacks.onPreviewStateChanged(currentPreviewState)
          }
        }
      }
    }
  }
}
