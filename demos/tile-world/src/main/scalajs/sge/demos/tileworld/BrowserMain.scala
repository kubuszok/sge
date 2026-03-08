package sge
package demos
package tileworld

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(new TileWorldGame())
}
