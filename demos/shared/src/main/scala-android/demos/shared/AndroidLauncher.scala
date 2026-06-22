/*
 * SGE Demos — Android launcher Activity base class.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.shared

import sge.{ ApplicationListener, Sge, SgeActivity }
import sge.platform.android._

/** Abstract Activity that bootstraps an [[sge.AndroidApplication]] for a [[DemoScene]].
  *
  * Subclasses override [[scene]] to provide the demo to run. All Android lifecycle / surface / frame / input wiring is inherited from the library's [[sge.SgeActivity]] (driven by
  * `sge.SgeAndroidDriver`); this base only supplies the platform provider, the listener (a [[SingleSceneApp]] wrapping the scene), and the demo config (sensors disabled). It no longer hand-copies the
  * GL renderer bridge, the `asInstanceOf[AndroidGraphics]` casts, the direct `_width`/`_height` mutation, or the `created`-flag state machine — those now live once in the library.
  *
  * Compiled only when android.jar is on the classpath (`scala-android/` source root).
  */
abstract class AndroidLauncherActivity extends SgeActivity {

  /** The demo scene to run. Override in each demo's Activity. */
  def scene: DemoScene

  override protected def platformProvider: AndroidPlatformProvider = AndroidPlatformProviderImpl

  override protected def createListener(using Sge): ApplicationListener = new SingleSceneApp(scene)

  override protected def createConfig(provider: AndroidPlatformProvider): AndroidConfigOps = {
    val config = provider.defaultConfig()
    config.useAccelerometer = false
    config.useCompass = false
    config
  }
}
