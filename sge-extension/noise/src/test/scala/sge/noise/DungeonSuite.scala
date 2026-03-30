/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noise

import java.util.Random

import sge.noise.generator.room.RoomType
import sge.noise.generator.room.dungeon.DungeonGenerator
import sge.noise.generator.util.Generators

class DungeonSuite extends munit.FunSuite {

  override def beforeEach(context: BeforeEach): Unit = {
    Generators.setRandom(new Random(42L))
  }

  test("DungeonGenerator produces rooms and corridors") {
    val grid = new Grid(31, 31)
    val gen = new DungeonGenerator
    gen.roomGenerationAttempts = 30
    gen.minRoomSize = 3
    gen.maxRoomSize = 7
    gen.generate(grid)

    val arr = grid.getArray
    var wallCount = 0
    var floorCount = 0
    var corridorCount = 0
    var i = 0
    while (i < arr.length) {
      if (arr(i) >= 1f) wallCount += 1
      else if (arr(i) == 0.5f) floorCount += 1
      else if (arr(i) == 0f) corridorCount += 1
      i += 1
    }

    assert(wallCount > 0, "No walls found")
    assert(floorCount > 0, "No room floors found")
    assert(corridorCount > 0, "No corridors found")
  }

  test("DungeonGenerator static convenience method works") {
    val grid = new Grid(31, 31)
    DungeonGenerator.generate(grid, 30)

    val arr = grid.getArray
    var hasFloor = false
    var i = 0
    while (i < arr.length) {
      if (arr(i) == 0.5f) hasFloor = true
      i += 1
    }
    assert(hasFloor, "Static generate produced no floor tiles")
  }

  test("DungeonGenerator with room types produces valid rooms") {
    val grid = new Grid(31, 31)
    val gen = new DungeonGenerator
    gen.roomGenerationAttempts = 50
    gen.minRoomSize = 5
    gen.maxRoomSize = 9
    gen.addRoomTypes(
      RoomType.DefaultRoomType.SQUARE,
      RoomType.DefaultRoomType.CROSS,
      RoomType.DefaultRoomType.ROUNDED
    )
    gen.generate(grid)

    val arr = grid.getArray
    var floorCount = 0
    var i = 0
    while (i < arr.length) {
      if (arr(i) == 0.5f) floorCount += 1
      i += 1
    }
    assert(floorCount > 0, "No room floors found with room types")
  }

  test("DungeonGenerator requires odd room sizes") {
    val gen = new DungeonGenerator
    gen.minRoomSize = 4
    gen.maxRoomSize = 8
    val grid = new Grid(31, 31)
    interceptMessage[IllegalStateException]("Min and max room sizes have to be odd.") {
      gen.generate(grid)
    }
  }

  test("DungeonGenerator dead end removal removes dead ends") {
    val grid1 = new Grid(31, 31)
    Generators.setRandom(new Random(42L))
    val gen1 = new DungeonGenerator
    gen1.roomGenerationAttempts = 20
    gen1.deadEndRemovalIterations = 0
    gen1.generate(grid1)

    val grid2 = new Grid(31, 31)
    Generators.setRandom(new Random(42L))
    val gen2 = new DungeonGenerator
    gen2.roomGenerationAttempts = 20
    gen2.deadEndRemovalIterations = Int.MaxValue
    gen2.generate(grid2)

    // Grid with dead end removal should have fewer corridor tiles
    var corridors1 = 0
    var corridors2 = 0
    val arr1 = grid1.getArray
    val arr2 = grid2.getArray
    var i = 0
    while (i < arr1.length) {
      if (arr1(i) == 0f) corridors1 += 1
      if (arr2(i) == 0f) corridors2 += 1
      i += 1
    }
    assert(corridors2 <= corridors1, s"Dead end removal should reduce corridors: $corridors2 > $corridors1")
  }
}
