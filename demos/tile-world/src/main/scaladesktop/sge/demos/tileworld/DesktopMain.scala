package sge
package demos
package tileworld

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(new TileWorldGame(), "SGE Tile World")
}
