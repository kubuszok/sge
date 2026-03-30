/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui

/** Implemented by objects that can acquire VisUI focus.
  * @author
  *   Kotcrab
  * @see
  *   [[FocusManager]]
  */
trait Focusable {

  /** Called by VisUI when object lost focus. Do not call manually, see [[FocusManager]]. */
  def focusLost(): Unit

  /** Called by VisUI when object gained focus. Do not call manually, see [[FocusManager]]. */
  def focusGained(): Unit
}
