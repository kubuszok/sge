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

import scala.collection.mutable.ArrayBuffer

/** Red tests for ISS-503: World.project with a null (absent) item must work like the Java original.
  *
  * Java reference (original-src/jbump/jbump/src/com/dongbat/jbump/):
  *   - World.java:240-280 `project` explicitly supports `item == null` (line 244 guards `visited.add(item)`), passes the null item straight to `filter.filter(item, other)` (line 264) and to
  *     `collisions.add(..., item, other, response)` (line 271).
  *   - Collisions.java:88-109 `add` stores the item via plain `items.add(item)` (line 107) — null items are stored fine.
  *   - RectHelper.java:40-122 `rect_detectCollision`: for itemRect (2.5,0,1,1) vs otherRect (3,0,1,1) with goal == position (dx=dy=0): diff = (-0.5,-1,2,2) (Rect.java:125-127), contains origin
  *     (Rect.java:129-132) so overlaps=true; nearest corner = (-0.5,-1) (Rect.java:45-47), wi=0.5, hi=1, ti=-0.5 (RectHelper.java:63-65); zero-movement displacement branch (RectHelper.java:89-102):
  *     px=-0.5, py forced to 0, normal=(-1,0), touch=(2.5-0.5, 0)=(2,0).
  *   - Tunnel case for the non-null control: itemRect (0,0,1,1) → goal (5,0) vs otherRect (3,0,1,1): diff=(2,-1,2,2), segment (0,0)+(5,0)t hits x∈[2,4] at ti1=0.4, ti2=0.8, normal=(-1,0)
  *     (RectHelper.java:68-81), touch=(0+5*0.4, 0)=(2,0) (RectHelper.java:117-118).
  *
  * The Scala port currently NPEs in Collisions.add (Collisions.scala:108-110 calls `.get` on the Nullable item) on the FIRST detected collision of any null-item projection, and World.scala:350 passes
  * `item.getOrElse(null.asInstanceOf[Item[?]])` into the filter.
  */
class JbumpNullProjectRedSuite extends munit.FunSuite {

  test("project with null item returns collisions like Java (Collisions.add must accept absent item)") {
    val world = World[String](1f)
    val wall  = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    // Query rect (2.5,0,1,1) overlaps the wall (3,0,1,1); goal == position (pure overlap query).
    // Java: World.java:240-280 returns one collision; the null item is stored (Collisions.java:107).
    val collisions = Collisions()
    val result     = world.project(Nullable.Null, 2.5f, 0f, 1f, 1f, 2.5f, 0f, collisions)

    assertEquals(result.size, 1)
    val col = result.get(0).get
    assert(col.other.get eq wall)
    // Java stores the null item; the read-back collision has item == null.
    assert(col.item.isEmpty)
    assert(col.overlaps)
    assertEqualsFloat(col.ti, -0.5f, 0.001f) // negative area of intersection: -(0.5 * 1)
    assertEquals(col.normal.x, -1)
    assertEquals(col.normal.y, 0)
    assertEqualsFloat(col.touch.x, 2f, 0.001f)
    assertEqualsFloat(col.touch.y, 0f, 0.001f)
    assertEqualsFloat(col.move.x, 0f, 0.001f)
    assertEqualsFloat(col.move.y, 0f, 0.001f)
    assert(col.`type`.get eq Response.slide) // defaultFilter response
  }

  test("project with null item invokes a user filter that safely inspects the item param") {
    val world = World[String](1f)
    val wall  = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    // Java: World.java:264 passes the null item directly to filter.filter(item, other); a user filter
    // may inspect it and still respond. The filter must receive a representation it can safely inspect.
    val received = ArrayBuffer.empty[Option[Item[?]]]
    val inspectingFilter: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] = {
        received += Option(item) // safe inspection of the possibly-absent item
        Response.slide
      }
    }

    val collisions = Collisions()
    val result     = world.project(Nullable.Null, 2.5f, 0f, 1f, 1f, 2.5f, 0f, inspectingFilter, collisions)

    // The filter was invoked exactly once, with an absent item (Java passes null).
    assertEquals(received.size, 1)
    assertEquals(received.head, None)
    // And the projection itself succeeds with the collision recorded.
    assertEquals(result.size, 1)
    assert(result.get(0).get.other.get eq wall)
  }

  test("project with non-null item (control, green at red-sha)") {
    val world  = World[String](1f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    // Tunnel case: player at (0,0,1,1) projected towards (5,0).
    val collisions = Collisions()
    val result     = world.project(Nullable[Item[?]](player), 0f, 0f, 1f, 1f, 5f, 0f, collisions)

    assertEquals(result.size, 1)
    val col = result.get(0).get
    assert(col.item.get eq player)
    assert(col.other.get eq wall)
    assert(!col.overlaps)
    assertEqualsFloat(col.ti, 0.4f, 0.001f) // ti1 = (3 - 1) / 5
    assertEquals(col.normal.x, -1)
    assertEquals(col.normal.y, 0)
    assertEqualsFloat(col.touch.x, 2f, 0.001f)
    assertEqualsFloat(col.touch.y, 0f, 0.001f)
    assert(col.`type`.get eq Response.slide)
  }
}
