/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: ItemInfo,weightComparator,x1,x2,y1,y2
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

/** Item metadata for segment/ray query results. */
class ItemInfo(val item: Item[?], var ti1: Float, var ti2: Float, var weight: Float) {

  /** The x coordinate where the line segment intersects the Rect of the Item. */
  var x1: Float = 0f

  /** The y coordinate where the line segment intersects the Rect of the Item. */
  var y1: Float = 0f

  /** The x coordinate where the line segment exits the Rect of the Item. */
  var x2: Float = 0f

  /** The y coordinate where the line segment exits the Rect of the Item. */
  var y2: Float = 0f
}

object ItemInfo {

  val weightComparator: Ordering[ItemInfo] = (o1: ItemInfo, o2: ItemInfo) => java.lang.Float.compare(o1.weight, o2.weight)
}
