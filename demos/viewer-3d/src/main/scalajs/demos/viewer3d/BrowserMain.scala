package demos.viewer3d

import demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(Viewer3dGame)
}
