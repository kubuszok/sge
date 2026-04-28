/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 19
 * Covenant-baseline-methods: BorderOwner,focusBorderEnabled,focusBorderEnabled_
 * Covenant-source-reference: com/kotcrab/vis/ui/util/BorderOwner.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

/** Implemented by actors that have a VisUI focus border; actor implementing this trait must support disabling its border.
  * @author
  *   Kotcrab
  */
trait BorderOwner {
  def focusBorderEnabled:                     Boolean
  def focusBorderEnabled_=(enabled: Boolean): Unit
}
