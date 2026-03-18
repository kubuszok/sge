package demos.shaders

import demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(new ShaderLabGame(), "SGE Shader Lab")
}
