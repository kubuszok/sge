package sge
package demos
package shaders

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(new ShaderLabGame())
}
