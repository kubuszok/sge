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
package highlight

import sge.graphics.Color

/** Represents single highlight.
  * @author
  *   Kotcrab
  * @since 1.1.2
  */
final case class Highlight(color: Color, start: Int, end: Int) extends Comparable[Highlight] {
  require(start < end, s"start can't be >= end: $start >= $end")

  override def compareTo(o: Highlight): Int = start - o.start
}
