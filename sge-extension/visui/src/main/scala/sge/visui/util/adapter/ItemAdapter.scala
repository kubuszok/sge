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
package adapter

import sge.scenes.scene2d.Actor

/** Generic use adapter used to create views for given objects.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
trait ItemAdapter[ItemT] {
  def getView(item: ItemT): Actor
}
