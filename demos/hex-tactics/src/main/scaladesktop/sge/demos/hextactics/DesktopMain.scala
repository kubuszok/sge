package sge
package demos
package hextactics

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(HexTacticsGame, "SGE Hex Tactics")
}
