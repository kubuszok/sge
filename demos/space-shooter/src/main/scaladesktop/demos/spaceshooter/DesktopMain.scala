package demos.spaceshooter

import demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(SpaceShooterGame, "SGE Space Shooter")
}
