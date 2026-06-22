package demos.pong

import demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit = {
    // Force the generated embedded-resources object's initializer (which
    // registers pong's assets) so Scala.js DCE keeps it. Matches the pattern
    // in every other demo's BrowserMain + sge's BrowserApplication.
    val _ : AnyRef = _root_.demos.pong.GeneratedEmbeddedResources
    BrowserLauncher.launch(PongGame)
  }
}
