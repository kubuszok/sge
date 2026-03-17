package sge
package demos
package assets

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(AssetShowcaseGame, "SGE Asset Showcase")
}
