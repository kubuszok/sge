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

class WorldSuite extends munit.FunSuite {

  test("add and hasItem") {
    val world = World[String]()
    val item  = Item[String]("block")
    world.add(item, 0, 0, 32, 32)
    assert(world.hasItem(item))
    assertEquals(world.countItems, 1)
  }

  test("remove item") {
    val world = World[String]()
    val item  = Item[String]("block")
    world.add(item, 0, 0, 32, 32)
    world.remove(item)
    assert(!world.hasItem(item))
    assertEquals(world.countItems, 0)
  }

  test("getRect returns correct rect") {
    val world = World[String]()
    val item  = Item[String]("block")
    world.add(item, 10, 20, 30, 40)
    val rect = world.getRect(item)
    assertEqualsFloat(rect.x, 10f, 0.001f)
    assertEqualsFloat(rect.y, 20f, 0.001f)
    assertEqualsFloat(rect.w, 30f, 0.001f)
    assertEqualsFloat(rect.h, 40f, 0.001f)
  }

  test("move with slide response stops at wall") {
    val world = World[String](1f)
    // A player at (0, 0) size 1x1
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    // A wall at (3, 0) size 1x1
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    // Move player towards wall
    val result = world.move(player, 5f, 0f, CollisionFilter.defaultFilter)

    // Player should be stopped at x=2 (touching the wall)
    assertEqualsFloat(result.goalX, 2f, 0.001f)
    assertEqualsFloat(result.goalY, 0f, 0.001f)
    assert(result.projectedCollisions.size > 0)
  }

  test("move with slide response allows sliding") {
    val world  = World[String](1f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    // Move diagonally towards wall — should slide along Y
    val result = world.move(player, 5f, 2f, CollisionFilter.defaultFilter)

    // Player x should be stopped at 2, y should reach 2
    assertEqualsFloat(result.goalX, 2f, 0.001f)
    assertEqualsFloat(result.goalY, 2f, 0.001f)
  }

  test("move with cross response passes through") {
    val world  = World[String](1f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val trigger = Item[String]("trigger")
    world.add(trigger, 2, 0, 1, 1)

    val crossFilter: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] = Response.cross
    }

    val result = world.move(player, 5f, 0f, crossFilter)

    // Player should pass through trigger zone
    assertEqualsFloat(result.goalX, 5f, 0.001f)
    assertEqualsFloat(result.goalY, 0f, 0.001f)
    assert(result.projectedCollisions.size > 0)
  }

  test("move with touch response stops at first contact") {
    val world  = World[String](1f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val touchFilter: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] = Response.touch
    }

    val result = world.move(player, 5f, 0f, touchFilter)
    assertEqualsFloat(result.goalX, 2f, 0.001f)
    assertEqualsFloat(result.goalY, 0f, 0.001f)
  }

  test("queryRect finds intersecting items") {
    val world = World[String](1f)
    val item1 = Item[String]("a")
    world.add(item1, 0, 0, 2, 2)
    val item2 = Item[String]("b")
    world.add(item2, 5, 5, 2, 2)
    val item3 = Item[String]("c")
    world.add(item3, 1, 1, 2, 2)

    val items = ArrayBuffer.empty[Item[?]]
    world.queryRect(0, 0, 3, 3, CollisionFilter.defaultFilter, items)

    // item1 and item3 should be found (they intersect [0,0,3,3])
    assert(items.contains(item1))
    assert(items.contains(item3))
    assert(!items.contains(item2))
  }

  test("queryPoint finds items containing the point") {
    val world = World[String](1f)
    val item1 = Item[String]("a")
    world.add(item1, 0, 0, 2, 2)
    val item2 = Item[String]("b")
    world.add(item2, 5, 5, 2, 2)

    val items = ArrayBuffer.empty[Item[?]]
    world.queryPoint(1, 1, CollisionFilter.defaultFilter, items)

    assert(items.contains(item1))
    assert(!items.contains(item2))
  }

  test("update changes item position") {
    val world = World[String]()
    val item  = Item[String]("block")
    world.add(item, 0, 0, 32, 32)
    world.update(item, 100, 100)
    val rect = world.getRect(item)
    assertEqualsFloat(rect.x, 100f, 0.001f)
    assertEqualsFloat(rect.y, 100f, 0.001f)
  }

  test("reset clears the world") {
    val world = World[String]()
    val item  = Item[String]("block")
    world.add(item, 0, 0, 32, 32)
    world.reset()
    assertEquals(world.countItems, 0)
    assertEquals(world.countCells, 0)
  }

  test("multiple items and collision detection") {
    val world = World[String](1f)
    // Create a floor
    for (i <- 0 until 10) {
      val wall = Item[String](s"wall$i")
      world.add(wall, i.toFloat, 0, 1, 1)
    }
    // Player above floor
    val player = Item[String]("player")
    world.add(player, 5, 1, 1, 1)

    // Move player downward into the floor
    val result = world.move(player, 5f, -1f, CollisionFilter.defaultFilter)

    // Player should stop on top of the floor
    assertEqualsFloat(result.goalX, 5f, 0.001f)
    assertEqualsFloat(result.goalY, 1f, 0.001f)
  }

  test("bounce response reflects movement") {
    val world  = World[String](1f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val bounceFilter: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] = Response.bounce
    }

    val result = world.move(player, 5f, 0f, bounceFilter)

    // Bounce should reflect: touch at x=2, remaining distance=3, reflected to x=2-3=-1
    assertEqualsFloat(result.goalX, -1f, 0.001f)
    assertEqualsFloat(result.goalY, 0f, 0.001f)
  }

  test("add returns same item if already present") {
    val world     = World[String]()
    val item      = Item[String]("block")
    val returned1 = world.add(item, 0, 0, 32, 32)
    val returned2 = world.add(item, 100, 100, 32, 32)
    assert(returned1 eq returned2)
    // Position should not have changed
    val rect = world.getRect(item)
    assertEqualsFloat(rect.x, 0f, 0.001f)
    assertEqualsFloat(rect.y, 0f, 0.001f)
  }
}
