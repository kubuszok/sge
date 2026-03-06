/*
 * SGE Demo — Browser (Scala.js) entry point.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

import org.scalajs.dom

/** Browser (Scala.js) entry point for the SGE demo.
  *
  * Creates a [[BrowserApplication]] with the demo scenes and starts the requestAnimationFrame loop.
  */
object BrowserDemoMain {

  def main(args: Array[String]): Unit = {
    val demoApp = BrowserDemoLauncher.createDemoApp()
    val config  = BrowserDemoLauncher.createConfig()
    val app     = new BrowserApplication(demoApp, config)
    app.start()
  }
}
