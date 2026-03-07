/*
 * SGE-specific trait for ApplicationListeners that need the Sge context.
 *
 * Since ApplicationListener.create() does not receive (using Sge), listeners that need the Sge
 * context early (before render()) can implement this trait to receive it. Platform application
 * classes (DesktopApplication, BrowserApplication) call sgeAvailable() after creating the Sge
 * context and before calling create().
 *
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Mixin for [[ApplicationListener]] implementations that need the [[Sge]] context at initialization.
  *
  * Platform application classes call [[sgeAvailable]] after creating the Sge context and before calling [[ApplicationListener.create]]. This allows listeners to store the Sge reference for use in
  * lifecycle methods.
  */
trait SgeAware {

  /** Called by the platform application after the [[Sge]] context is created. */
  def sgeAvailable(sge: Sge): Unit
}
