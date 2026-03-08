package sge
package demos
package viewports

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(ViewportGalleryGame)
}
