/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 24
 * Covenant-baseline-methods: Focusable,focusGained,focusLost
 * Covenant-source-reference: com/kotcrab/vis/ui/Focusable.java
 * Covenant-verified: 2026-04-19
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
