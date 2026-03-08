package sge
package demos
package spaceshooter

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(SpaceShooterGame)
}
