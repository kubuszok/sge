/*
 * SGE Demos — browser launcher for Scala.js.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package shared

import _root_.sge.{BrowserApplication, BrowserApplicationConfig}

/** Creates a WebGL canvas and runs a [[DemoScene]] via requestAnimationFrame.
  *
  * Scala.js only — uses [[_root_.sge.BrowserApplication]] which wraps WebGL1/WebGL2.
  */
object BrowserLauncher {

  /** Launch a browser canvas running the given scene.
    *
    * @param scene
    *   the demo scene to run
    * @param width
    *   canvas width in CSS pixels (0 = fill available space)
    * @param height
    *   canvas height in CSS pixels (0 = fill available space)
    */
  def launch(scene: DemoScene, width: Int = 800, height: Int = 600): Unit = {
    val config = new BrowserApplicationConfig(width, height)
    val app    = new BrowserApplication(new SingleSceneApp(scene), config)
    app.start()
  }
}
