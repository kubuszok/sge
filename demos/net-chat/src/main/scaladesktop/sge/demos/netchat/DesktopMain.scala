package sge
package demos
package netchat

import sge.demos.shared.DesktopLauncher

object DesktopMain {
  def main(args: Array[String]): Unit =
    DesktopLauncher.launch(NetChatGame, "SGE Net Chat")
}
