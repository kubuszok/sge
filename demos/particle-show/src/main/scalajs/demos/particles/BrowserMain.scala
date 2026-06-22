package demos.particles

import demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit = {
    // Keep the build-time-generated, self-registering embedded-resources object
    // (Scala.js DCE drops it otherwise — it has no @JSExportTopLevel).
    val _ : AnyRef = _root_.demos.particles.GeneratedEmbeddedResources
    BrowserLauncher.launch(ParticleShowGame)
  }
}
