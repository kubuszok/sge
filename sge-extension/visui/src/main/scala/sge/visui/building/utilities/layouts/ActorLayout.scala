/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package building
package utilities
package layouts

import sge.Sge
import sge.scenes.scene2d.Actor

/** An interface that allows to convert multiple widgets into one, providing utilities for complex tables building.
  * @author
  *   MJ
  */
trait ActorLayout {
  /** @return passed actors merged into one widget. */
  def convertToActor(widgets: Actor*)(using Sge): Actor

  /** @return passed wrapped actors merged into one widget. */
  def convertToActorFromCells(widgets: CellWidget[?]*)(using Sge): Actor
}
