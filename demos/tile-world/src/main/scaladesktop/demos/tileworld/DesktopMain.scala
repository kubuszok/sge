package demos.tileworld

import demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(new TileWorldGame(), "SGE Tile World")
}
