package sge
package demos
package shaders

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(new ShaderLabGame(), "SGE Shader Lab")
}
