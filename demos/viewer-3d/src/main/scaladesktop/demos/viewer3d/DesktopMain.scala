package demos.viewer3d

import demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(Viewer3dGame, "SGE 3D Viewer")
}
