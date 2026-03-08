// SGE — Android platform provider implementation
//
// Implements AndroidPlatformProvider using Android SDK APIs.
// Compiled against android.jar. Never loaded on Desktop JVM.

package sge
package platform
package android

import _root_.android.app.Activity
import _root_.android.content.{ActivityNotFoundException, Context, ContextWrapper, Intent}
import _root_.android.net.Uri
import _root_.android.os.Handler
import _root_.android.view.WindowManager

/** Concrete Android platform provider. Loaded via reflection on Android. */
object AndroidPlatformProviderImpl extends AndroidPlatformProvider {

  override def createLifecycle(activity: AnyRef): AndroidLifecycleOps =
    AndroidLifecycleImpl(activity.asInstanceOf[_root_.android.app.Activity])

  override def createClipboard(context: AnyRef): ClipboardOps =
    AndroidClipboardImpl(context.asInstanceOf[Context])

  override def createPreferences(context: AnyRef, name: String): PreferencesOps =
    AndroidPreferencesImpl(
      context.asInstanceOf[Context].getSharedPreferences(name, Context.MODE_PRIVATE)
    )

  override def openURI(context: AnyRef, uri: String): Boolean = {
    val ctx       = context.asInstanceOf[Context]
    val parsedUri = Uri.parse(uri)
    try {
      val intent = new Intent(Intent.ACTION_VIEW, parsedUri)
      // LiveWallpaper and Daydream contexts need this flag
      if (!ctx.isInstanceOf[Activity]) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      ctx.startActivity(intent)
      true
    } catch {
      case _: ActivityNotFoundException => false
    }
  }

  override def defaultConfig(): AndroidConfigOps = AndroidConfigOps()

  override def createFiles(context: AnyRef, useExternalFiles: Boolean): FilesOps = {
    val ctx = context.asInstanceOf[ContextWrapper]
    AndroidFilesOpsImpl(ctx.getAssets, ctx, useExternalFiles)
  }

  override def createAudioEngine(context: AnyRef, config: AndroidConfigOps): AudioEngineOps =
    AndroidAudioEngineImpl(context.asInstanceOf[Context], config)

  override def createHaptics(context: AnyRef): HapticsOps =
    AndroidHapticsImpl(context.asInstanceOf[Context])

  override def createCursor(context: AnyRef): CursorOps =
    AndroidCursorImpl

  override def createDisplayMetrics(windowManager: AnyRef): DisplayMetricsOps =
    AndroidDisplayMetricsImpl(windowManager.asInstanceOf[WindowManager])

  override def createSensors(context: AnyRef, windowManager: AnyRef): SensorOps =
    AndroidSensorImpl(context.asInstanceOf[Context], windowManager.asInstanceOf[WindowManager])

  override def createTouchInput(context: AnyRef): TouchInputOps =
    AndroidTouchInputImpl(context.asInstanceOf[_root_.android.content.Context])

  override def createInputMethod(context: AnyRef, handler: AnyRef): InputMethodOps =
    AndroidInputMethodImpl(context.asInstanceOf[Context], handler.asInstanceOf[Handler])

  override def createGLSurfaceView(
      context: AnyRef,
      config: AndroidConfigOps,
      resolutionStrategy: ResolutionStrategyOps
  ): GLSurfaceViewOps =
    AndroidGLSurfaceViewImpl(context.asInstanceOf[Context], config, resolutionStrategy)

  override def createGL20(): GL20Ops = new AndroidGL20Impl

  override def createGL30(): GL30Ops = new AndroidGL30Impl
}
