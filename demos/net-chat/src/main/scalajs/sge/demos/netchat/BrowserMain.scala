package sge
package demos
package netchat

import sge.demos.shared.BrowserLauncher

object BrowserMain {
  def main(args: Array[String]): Unit =
    BrowserLauncher.launch(NetChatGame)
}
