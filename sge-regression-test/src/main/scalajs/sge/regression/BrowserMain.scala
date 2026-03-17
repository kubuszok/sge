/*
 * SGE Regression Test — Browser (Scala.js) entry point.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

import org.scalajs.dom

/** Browser (Scala.js) entry point for the SGE regression test.
  *
  * Creates a [[BrowserApplication]] with the regression test scenes and starts the requestAnimationFrame loop.
  */
object BrowserMain {

  def main(args: Array[String]): Unit = {
    val app    = BrowserLauncher.createApp()
    val config = BrowserLauncher.createConfig()
    val brApp  = new BrowserApplication(app, config)
    brApp.start()
  }
}
