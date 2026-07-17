/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.Nullable

/** Red test for ISS-597: the Java copy constructor `Collisions(Collisions other)` (Collisions.java:59-80) has no Scala counterpart.
  *
  * Java reference (original-src/jbump/jbump/src/com/dongbat/jbump/Collisions.java:59-80):
  *   - `public Collisions(Collisions other)` copies every one of the 16 primitive struct-of-arrays (overlaps, tis, moveXs, moveYs, normalXs, normalYs, touchXs, touchYs, x1s, y1s, w1s, h1s, x2s, y2s,
  *     w2s, h2s via `addAll`), plus the three reference lists (`items`, `others`, `types` via `addAll`), plus `size = other.size`.
  *   - The copy owns fresh backing arrays, so mutating the copy must not disturb the source (independent buffers).
  *
  * This currently fails to COMPILE: `Collisions(src)` resolves to no constructor (only the no-arg `Collisions()` exists). `enforce compare --strict` flags it as "Dropped ctor args: Collisions.other".
  */
class CollisionsCopyCtorRedSuite extends munit.FunSuite {

  test("copy constructor duplicates all struct-of-arrays and reference lists (Collisions.java:59-80)") {
    val wall = Item[String]("wall")
    val src  = Collisions()
    src.add(
      true, // overlap
      0.5f, // ti
      1f, // moveX
      2f, // moveY
      -1, // normalX
      0, // normalY
      3f, // touchX
      4f, // touchY
      10f, // x1
      11f, // y1
      12f, // w1
      13f, // h1
      20f, // x2
      21f, // y2
      22f, // w2
      23f, // h2
      Nullable[Item[?]](wall),
      Nullable.Null,
      Response.slide
    )

    // ISS-597 contract: the auxiliary copy constructor exists and duplicates all state.
    val copy = Collisions(src)

    assertEquals(copy.size, src.size)
    assertEquals(copy.size, 1)

    val col = copy.get(0).get
    assert(col.overlaps)
    assertEqualsFloat(col.ti, 0.5f, 0.001f)
    assertEqualsFloat(col.move.x, 1f, 0.001f)
    assertEqualsFloat(col.move.y, 2f, 0.001f)
    assertEquals(col.normal.x, -1)
    assertEquals(col.normal.y, 0)
    assertEqualsFloat(col.touch.x, 3f, 0.001f)
    assertEqualsFloat(col.touch.y, 4f, 0.001f)
    assert(col.item.get eq wall)
    assert(col.other.isEmpty)
    assert(col.`type`.get eq Response.slide)

    // The copy owns independent backing buffers: clearing it must not disturb the source.
    copy.clear()
    assertEquals(copy.size, 0)
    assertEquals(src.size, 1)
  }
}
