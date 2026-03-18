/*
 * SGE Demos — shared scene abstraction for demo applications.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.shared

import sge.{Pixels, Sge}

/** A single demo scene that exercises specific SGE features.
  *
  * Each scene has a lifecycle: `init` is called once when the scene becomes active, `render` is
  * called every frame with the frame delta time, and `dispose` is called when the application exits.
  * Scenes may also respond to window resizes via `resize`.
  */
trait DemoScene {

  /** Human-readable name of this demo. */
  def name: String

  /** Called once after the GL context is available. Create resources here. */
  def init()(using Sge): Unit

  /** Called every frame.
    *
    * @param dt
    *   seconds since the last frame (delta time)
    */
  def render(dt: Float)(using Sge): Unit

  /** Called when the window is resized. Update viewports here. */
  def resize(width: Pixels, height: Pixels)(using Sge): Unit = ()

  /** Called when the application exits. Release GL resources here. */
  def dispose()(using Sge): Unit
}
