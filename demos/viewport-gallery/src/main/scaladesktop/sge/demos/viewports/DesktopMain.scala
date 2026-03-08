package sge
package demos
package viewports

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(ViewportGalleryGame, "SGE Viewports")
}
