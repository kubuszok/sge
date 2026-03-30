/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
