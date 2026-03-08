package sge
package demos
package hextactics

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(HexTacticsGame)
}
