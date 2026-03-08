package sge
package demos
package pong

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(PongGame, "SGE Pong")
}
