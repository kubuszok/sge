package sge
package demos
package viewer3d

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(Viewer3dGame)
}
