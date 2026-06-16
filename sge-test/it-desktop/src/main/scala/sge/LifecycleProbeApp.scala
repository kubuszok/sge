// SGE — ISS-551 clause-3 support: a minimal DesktopApplicationBase
//
// DesktopWindow.create() calls application.createInput(this); nothing else on
// the application is touched on the close-before-first-update path under test.
// This stub provides a real DefaultDesktopInput (so create()/close() exercise
// the genuine input lifecycle) and a no-op ApplicationListener factory; every
// other Application member is unused on this path and throws if reached, so the
// stub can never silently substitute for product behaviour. It lives in
// `package sge` so it can construct DefaultDesktopInput (private[sge]).

package sge

import sge.platform.WindowingOps
import sge.utils.Clipboard

/** Throwaway DesktopApplicationBase for the ISS-551 clause-3 close-before-first-update check. Only createInput is exercised. */
final class LifecycleProbeApp(windowing: WindowingOps) extends DesktopApplicationBase {

  override def createInput(window: DesktopWindow): DesktopInput =
    DefaultDesktopInput(window, windowing)

  override def createAudio(config: DesktopApplicationConfig): DesktopAudio =
    throw new UnsupportedOperationException("LifecycleProbeApp.createAudio is not used by the ISS-551 close-before-update check")

  override def applicationListener: ApplicationListener =
    throw new UnsupportedOperationException("LifecycleProbeApp.applicationListener is not used by the ISS-551 close-before-update check")

  override def graphics: Graphics =
    throw new UnsupportedOperationException("LifecycleProbeApp.graphics is not used by the ISS-551 close-before-update check")

  override def audio: Audio =
    throw new UnsupportedOperationException("LifecycleProbeApp.audio is not used by the ISS-551 close-before-update check")

  override def input: Input =
    throw new UnsupportedOperationException("LifecycleProbeApp.input is not used by the ISS-551 close-before-update check")

  override def files: Files =
    throw new UnsupportedOperationException("LifecycleProbeApp.files is not used by the ISS-551 close-before-update check")

  override def net: Net =
    throw new UnsupportedOperationException("LifecycleProbeApp.net is not used by the ISS-551 close-before-update check")

  override def applicationType: Application.ApplicationType = Application.ApplicationType.Desktop

  override def version: Int = 0

  override def javaHeap: Long = 0L

  override def nativeHeap: Long = 0L

  override def getPreferences(name: String): Preferences =
    throw new UnsupportedOperationException("LifecycleProbeApp.getPreferences is not used by the ISS-551 close-before-update check")

  override def clipboard: Clipboard =
    throw new UnsupportedOperationException("LifecycleProbeApp.clipboard is not used by the ISS-551 close-before-update check")

  override def postRunnable(runnable: Runnable): Unit =
    throw new UnsupportedOperationException("LifecycleProbeApp.postRunnable is not used by the ISS-551 close-before-update check")

  override def exit(): Unit =
    throw new UnsupportedOperationException("LifecycleProbeApp.exit is not used by the ISS-551 close-before-update check")

  override def addLifecycleListener(listener: LifecycleListener): Unit =
    throw new UnsupportedOperationException("LifecycleProbeApp.addLifecycleListener is not used by the ISS-551 close-before-update check")

  override def removeLifecycleListener(listener: LifecycleListener): Unit =
    throw new UnsupportedOperationException("LifecycleProbeApp.removeLifecycleListener is not used by the ISS-551 close-before-update check")
}

object LifecycleProbeApp {

  /** A no-op ApplicationListener factory: the close-before-first-update check never runs update(), so the listener is never materialized — but the field must carry a factory. */
  val listenerFactory: Sge ?=> ApplicationListener = new ApplicationListener {
    override def create():                              Unit = ()
    override def resize(width: Pixels, height: Pixels): Unit = ()
    override def render():                              Unit = ()
    override def pause():                               Unit = ()
    override def resume():                              Unit = ()
    override def dispose():                             Unit = ()
  }
}
