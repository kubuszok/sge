/*
 * SGE Demos — shared desktop launcher for JVM and Scala Native.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package shared

import _root_.sge.{DesktopApplicationConfig, DesktopApplicationFactory}

/** Creates a desktop window and runs a [[DemoScene]] until the user closes it.
  *
  * Shared between JVM (Panama FFM) and Scala Native (@extern C FFI) platforms.
  * The platform-specific `DesktopApplicationFactory` is resolved at link time.
  */
object DesktopLauncher {

  /** Launch a desktop window running the given scene.
    *
    * Blocks until the window is closed.
    *
    * @param scene
    *   the demo scene to run
    * @param title
    *   window title
    * @param width
    *   initial window width in pixels
    * @param height
    *   initial window height in pixels
    */
  def launch(scene: DemoScene, title: String, width: Int = 800, height: Int = 600): Unit = {
    val config = DesktopApplicationConfig()
    config.title        = title
    config.windowWidth  = width
    config.windowHeight = height
    DesktopApplicationFactory(new SingleSceneApp(scene), config)
  }
}
