package sge
package demos
package curves

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(CurvePlayground, "SGE Curves")
}
