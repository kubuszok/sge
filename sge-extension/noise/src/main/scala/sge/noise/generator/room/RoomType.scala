/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 209
 * Covenant-baseline-methods: CastleMinSize,CastleMinTower,CrossMinSize,DefaultRoomType,Interceptor,RoomType,carve,isValid
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator
package room

/** Represents a single room type. Determines how the room is carved in the map.
  *
  * Note that even though some room types create non-rectangle rooms, room collisions during map generating are still checked with room's original rectangle bounds to simplify calculations.
  *
  * @author
  *   MJ
  * @see
  *   [[RoomType.Interceptor]]
  */
trait RoomType {

  /** @param room
    *   should be filled.
    * @param grid
    *   should contain the room in its selected position.
    * @param value
    *   value with which the room should be filled.
    */
  def carve(room: AbstractRoomGenerator.Room, grid: Grid, value: Float): Unit

  /** @param room
    *   is about to be filled.
    * @return
    *   true if this type can handle this room.
    */
  def isValid(room: AbstractRoomGenerator.Room): Boolean
}

object RoomType {

  /** Wraps around an existing type, allowing to slightly modify its behavior. A common usage can be changing of tile value in carve method to a custom one, allowing different room types to use
    * different tile sets.
    *
    * @author
    *   MJ
    */
  class Interceptor(protected val roomType: RoomType, protected val value: Float) extends RoomType {

    override def carve(room: AbstractRoomGenerator.Room, grid: Grid, value: Float): Unit =
      roomType.carve(room, grid, this.value)

    override def isValid(room: AbstractRoomGenerator.Room): Boolean =
      roomType.isValid(room)
  }

  /** Contains default implementations of [[RoomType]].
    *
    * @author
    *   MJ
    */
  enum DefaultRoomType extends RoomType {

    /** Fills all of room's cells. Default behavior if room types are not used. Works with any room size. */
    case SQUARE

    /** Uses a very simple algorithm to round room's corners. Works best for about 5 to 25 room size. */
    case ROUNDED

    /** Instead of carving a simple rectangle, forms a rectangle with four "towers" in rooms' corners. Works with pretty much any room size, but requires the room to be at least 7x7 squares big.
      */
    case CASTLE

    /** Forms a pyramid-like structure. Can handle only square rooms with side size bigger than 2. Works best on odd room sizes.
      */
    case DIAMOND

    /** Forms a cross-shaped room, dividing the room into 9 (usually) equal parts and removing the corner ones. Requires the room to have at least 3x3 size. Works best with square rooms.
      */
    case CROSS

    override def carve(room: AbstractRoomGenerator.Room, grid: Grid, value: Float): Unit = {
      this match {
        case SQUARE =>
          room.fill(grid, value)

        case ROUNDED =>
          val halfSize              = (room.width + room.height) / 2
          val maxDistanceFromCenter = halfSize * 9 / 10
          var x                     = 0
          while (x < room.width) {
            var y = 0
            while (y < room.height) {
              val distanceFromCenter = Math.abs(x - room.width / 2) + Math.abs(y - room.height / 2)
              if (distanceFromCenter < maxDistanceFromCenter) {
                grid.set(x + room.x, y + room.y, value)
              }
              y += 1
            }
            x += 1
          }

        case CASTLE =>
          val size      = Math.min(room.width, room.height)
          val towerSize = Math.max((size - 1) / 4, DefaultRoomType.CastleMinTower)
          val offset    = Math.max(towerSize / 4, if (towerSize == DefaultRoomType.CastleMinTower) 1 else 2)
          // Main room:
          var x = offset
          while (x < room.width - offset) {
            var y = offset
            while (y < room.height - offset) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }
          // Towers:
          x = 0
          while (x < towerSize) {
            var y = 0
            while (y < towerSize) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }
          x = room.width - towerSize
          while (x < room.width) {
            var y = 0
            while (y < towerSize) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }
          x = 0
          while (x < towerSize) {
            var y = room.height - towerSize
            while (y < room.height) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }
          x = room.width - towerSize
          while (x < room.width) {
            var y = room.height - towerSize
            while (y < room.height) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }

        case DIAMOND =>
          val halfSize = room.width / 2
          var x        = 0
          while (x < room.width) {
            var y = 0
            while (y < room.height) {
              val distanceFromCenter = Math.abs(x - halfSize) + Math.abs(y - halfSize)
              if (distanceFromCenter <= halfSize) {
                grid.set(x + room.x, y + room.y, value)
              }
              y += 1
            }
            x += 1
          }

        case CROSS =>
          val offsetX = room.width / 3
          val offsetY = room.height / 3
          var x       = 0
          while (x < room.width) {
            var y = offsetY
            while (y < room.height - offsetY) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }
          x = offsetX
          while (x < room.width - offsetX) {
            var y = 0
            while (y < room.height) {
              grid.set(x + room.x, y + room.y, value)
              y += 1
            }
            x += 1
          }
      }
    }

    override def isValid(room: AbstractRoomGenerator.Room): Boolean =
      this match {
        case CASTLE  => room.width >= DefaultRoomType.CastleMinSize && room.height >= DefaultRoomType.CastleMinSize
        case DIAMOND => room.width > 2 && room.width == room.height
        case CROSS   => room.width >= DefaultRoomType.CrossMinSize && room.height >= DefaultRoomType.CrossMinSize
        case _       => true
      }
  }

  object DefaultRoomType {
    val CastleMinSize:  Int = 7
    val CastleMinTower: Int = 3
    val CrossMinSize:   Int = 3
  }
}
