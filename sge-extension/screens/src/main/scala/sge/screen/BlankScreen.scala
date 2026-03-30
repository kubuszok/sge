/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen

import sge.utils.Seconds

/** A blank screen. Used internally when no screen has been pushed yet.
  *
  * @author
  *   damios
  */
private[screen] class BlankScreen extends ManagedScreen {

  override def render(delta: Seconds): Unit = {
    // don't do anything by default
  }

  override def resize(width: Pixels, height: Pixels): Unit = {
    // don't do anything by default
  }

  override def close(): Unit = {
    // don't do anything by default
  }
}
