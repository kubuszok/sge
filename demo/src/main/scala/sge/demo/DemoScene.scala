/*
 * SGE Demo — scene abstraction for cycling through feature demos.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** A single demo scene that exercises a specific SGE feature.
  *
  * Each scene has a lifecycle: `init` is called once when the scene becomes active, `render` is called every frame, and `dispose` is called when switching to the next scene. The scene receives the
  * elapsed time since it became active and the total duration it will be shown.
  */
trait DemoScene {

  /** Human-readable name of this demo, shown on screen. */
  def name: String

  /** Called once when this scene becomes the active scene. GL context is available. */
  def init()(using Sge): Unit

  /** Called every frame while this scene is active.
    *
    * @param elapsed
    *   seconds since this scene became active
    */
  def render(elapsed: Float)(using Sge): Unit

  /** Called when this scene is being replaced by the next scene. Clean up GL resources here. */
  def dispose()(using Sge): Unit
}
