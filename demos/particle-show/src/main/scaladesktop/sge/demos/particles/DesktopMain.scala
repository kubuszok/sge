package sge
package demos
package particles

import _root_.sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(ParticleShowGame, "SGE Particles")
}
