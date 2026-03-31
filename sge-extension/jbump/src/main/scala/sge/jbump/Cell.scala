/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import scala.collection.mutable

/** Grid cell storing items. */
class Cell {
  var itemCount: Int                            = 0
  var x:         Float                          = 0f
  var y:         Float                          = 0f
  val items:     mutable.LinkedHashSet[Item[?]] = mutable.LinkedHashSet.empty
}
