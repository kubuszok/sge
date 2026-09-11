// SGE — Android platform abstraction layer
//
// Self-contained interfaces using only JDK types (no sge.* dependencies).
// Implementations live in sge-jvm-platform-android (compiled against android.jar).
// sge core discovers and adapts these at runtime via reflection.
//
// Pattern: same as PanamaProvider — define ops here, implement there, bridge in sge.

package sge
package platform
package android

/** Top-level provider for Android platform services.
  *
  * Implementations create concrete instances of clipboard, preferences, logger, etc. All types and method signatures use only JDK types to avoid depending on sge core. sge core wraps these into its
  * own trait implementations (Clipboard, Preferences, etc.) using adapters + mix-ins.
  */
trait AndroidPlatformProvider {

  // ── Lifecycle ───────────────────────────────────────────────────────

  /** Creates lifecycle operations for the given Activity.
    * @param activity
    *   an Android Activity (as AnyRef)
    */
  def createLifecycle(activity: AnyRef): AndroidLifecycleOps

  // ── Existing services ─────────────────────────────────────────────────

  /** Creates a clipboard backed by Android's ClipboardManager.
    * @param context
    *   an Android Context (passed as AnyRef to avoid android.* dependency)
    */
  def createClipboard(context: AnyRef): ClipboardOps

  /** Creates preferences backed by Android's SharedPreferences.
    * @param context
    *   an Android Context
    * @param name
    *   the preferences file name
    */
  def createPreferences(context: AnyRef, name: String): PreferencesOps

  /** Opens a URI via Android intents.
    * @param context
    *   an Android Context
    * @param uri
    *   the URI string to open
    * @return
    *   true if the URI was opened successfully
    */
  def openURI(context: AnyRef, uri: String): Boolean

  /** Returns the Android application configuration. */
  def defaultConfig(): AndroidConfigOps

  // ── Files ─────────────────────────────────────────────────────────────

  /** Creates file operations backed by Android's AssetManager and storage paths.
    * @param context
    *   an Android Context
    * @param useExternalFiles
    *   whether to enable external storage access
    */
  def createFiles(context: AnyRef, useExternalFiles: Boolean): FilesOps

  // ── Audio ─────────────────────────────────────────────────────────────

  /** Creates the audio engine backed by SoundPool + MediaPlayer.
    * @param context
    *   an Android Context
    * @param config
    *   the application configuration
    */
  def createAudioEngine(context: AnyRef, config: AndroidConfigOps): AudioEngineOps

  // ── Haptics ───────────────────────────────────────────────────────────

  /** Creates haptic feedback backed by Android's Vibrator service.
    * @param context
    *   an Android Context
    */
  def createHaptics(context: AnyRef): HapticsOps

  // ── Cursor ──────────────────────────────────────────────────────────

  /** Creates cursor operations backed by Android's PointerIcon.
    * @param context
    *   an Android Context
    */
  def createCursor(context: AnyRef): CursorOps

  // ── Display metrics ─────────────────────────────────────────────────

  /** Creates display metrics backed by Android's DisplayMetrics + DisplayCutout.
    * @param windowManager
    *   an Android WindowManager (as AnyRef)
    */
  def createDisplayMetrics(windowManager: AnyRef): DisplayMetricsOps

  // ── Sensors ─────────────────────────────────────────────────────────

  /** Creates sensor operations backed by Android's SensorManager.
    * @param context
    *   an Android Context
    * @param windowManager
    *   an Android WindowManager (as AnyRef)
    */
  def createSensors(context: AnyRef, windowManager: AnyRef): SensorOps

  // ── Touch input ────────────────────────────────────────────────────

  /** Creates touch input operations for MotionEvent data extraction.
    * @param context
    *   an Android Context
    */
  def createTouchInput(context: AnyRef): TouchInputOps

  // ── Input method ────────────────────────────────────────────────────

  /** Creates input method operations for keyboard/dialog control.
    * @param context
    *   an Android Context
    * @param handler
    *   an Android Handler (as AnyRef) for posting to the UI thread
    */
  def createInputMethod(context: AnyRef, handler: AnyRef): InputMethodOps

  // ── GL surface view ─────────────────────────────────────────────────

  /** Creates a GL surface view with EGL configuration.
    * @param context
    *   an Android Context
    * @param config
    *   the application configuration
    * @param resolutionStrategy
    *   the resolution strategy for measuring
    */
  def createGLSurfaceView(
    context:            AnyRef,
    config:             AndroidConfigOps,
    resolutionStrategy: ResolutionStrategyOps
  ): GLSurfaceViewOps

  // ── GL ES ─────────────────────────────────────────────────────────────

  /** Creates GL ES 2.0 operations backed by android.opengl.GLES20. */
  def createGL20(): GL20Ops

  /** Creates GL ES 3.0 operations backed by android.opengl.GLES30. The returned object also implements GL20Ops (extends it).
    */
  def createGL30(): GL30Ops
}
