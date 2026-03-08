package sge
package demos
package pong

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(PongGame)
}
