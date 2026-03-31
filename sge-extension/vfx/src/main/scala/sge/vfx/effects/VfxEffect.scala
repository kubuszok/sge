/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

/** Base trait for all visual effects.
  *
  * Whether or not this effect is disabled and shouldn't be processed can be controlled via [[disabled]]. The method [[resize]] will be called on every application resize as usual. Also it will be
  * called once the filter has been added to [[sge.vfx.VfxManager]].
  */
trait VfxEffect extends AutoCloseable {

  /** Whether or not this effect is disabled and shouldn't be processed */
  var disabled: Boolean

  /** The method will be called on every application resize as usual. Also it will be called once the filter has been added to [[sge.vfx.VfxManager]].
    */
  def resize(width: Int, height: Int): Unit

  /** Update any time based values.
    * @param delta
    *   in seconds.
    */
  def update(delta: Float): Unit

  /** Concrete objects shall be responsible to recreate or rebind its own resources whenever its needed, usually when the OpenGL context is lost (e.g. framebuffer textures should be updated and shader
    * parameters should be reuploaded/rebound.
    */
  def rebind(): Unit
}
