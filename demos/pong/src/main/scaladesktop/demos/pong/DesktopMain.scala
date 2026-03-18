package demos.pong

import demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(PongGame, "SGE Pong")
}
