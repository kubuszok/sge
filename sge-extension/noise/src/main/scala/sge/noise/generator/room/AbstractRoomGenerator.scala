/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noise
package generator
package room

import java.util.ArrayList

import sge.noise.generator.util.Generators

/** Abstract base for room-generating algorithms.
  *
  * @author
  *   MJ
  */
abstract class AbstractRoomGenerator extends AbstractGenerator {

  private val roomTypes: ArrayList[RoomType] = new ArrayList[RoomType]()

  /** Minimum room's width and height. */
  private var _minRoomSize: Int = 3

  /** Maximum room's width and height. */
  private var _maxRoomSize: Int = 7

  /** Maximum difference between room's width and height. */
  var tolerance: Int = 2

  /** Maximum amount of generated rooms. If 0 or negative, there is no limit. */
  var maxRoomsAmount: Int = 0

  /** @return
    *   direct reference to internal list of accepted room types.
    */
  def getRoomTypes: ArrayList[RoomType] = roomTypes

  /** @param roomType
    *   determines how the room is carved. Room type is chosen at random during room generating. Note that you can add a single room type multiple times to make it more likely for the type to be
    *   chosen.
    */
  def addRoomType(roomType: RoomType): Unit =
    roomTypes.add(roomType)

  /** @param roomType
    *   determines how the room is carved.
    * @param times
    *   this is how many times the room will be added to the list.
    */
  def addRoomType(roomType: RoomType, times: Int): Unit = {
    var index = 0
    while (index < times) {
      addRoomType(roomType)
      index += 1
    }
  }

  /** @param types
    *   determine how the rooms are carved. Room type is chosen at random during room generating.
    */
  def addRoomTypes(types: RoomType*): Unit =
    types.foreach(addRoomType)

  /** @param grid
    *   contains the room.
    * @param room
    *   was just spawned. Should fill its values in the grid.
    * @param value
    *   value used to fill the room.
    */
  protected def carveRoom(grid: Grid, room: AbstractRoomGenerator.Room, value: Float): Unit =
    if (roomTypes.isEmpty) { // No types specified: carving whole room:
      room.fill(grid, value)
    } else {
      var index         = Generators.randomIndex(roomTypes)
      val originalIndex = index
      var roomType      = roomTypes.get(index)
      var noValidType   = false
      while (!roomType.isValid(room) && !noValidType) {
        index = (index + 1) % roomTypes.size()
        if (index == originalIndex) { // No valid types found for the room:
          room.fill(grid, value)
          noValidType = true
        } else {
          roomType = roomTypes.get(index)
        }
      }
      if (!noValidType) roomType.carve(room, grid, value)
    }

  /** @param grid
    *   will be used to generate bounds of the room.
    * @return
    *   a new random-sized room within grid's bounds.
    */
  protected def getRandomRoom(grid: Grid): AbstractRoomGenerator.Room = {
    val w = randomSize()
    val h = randomSize(w)
    if (w > grid.width || h > grid.height) {
      throw new IllegalStateException(
        "maxRoomSize is higher than grid's size, which resulted in spawning a room bigger than the whole map. Set maxRoomSize to a lower value."
      )
    }
    val random = Generators.getRandom
    val x      = normalizePosition(random.nextInt(grid.width - w))
    val y      = normalizePosition(random.nextInt(grid.height - h))
    new AbstractRoomGenerator.Room(x, y, w, h)
  }

  /** @param position
    *   row or column index.
    * @return
    *   validated and normalized position.
    */
  protected def normalizePosition(position: Int): Int = position

  /** @param size
    *   random room size value.
    * @return
    *   validated and normalized room size.
    */
  protected def normalizeSize(size: Int): Int = size

  /** @return
    *   random room size within [[minRoomSize]] and [[maxRoomSize]] range.
    */
  protected def randomSize(): Int =
    normalizeSize(
      if (_minRoomSize == _maxRoomSize) _minRoomSize
      else Generators.randomInt(_minRoomSize, _maxRoomSize)
    )

  /** @param bound
    *   second size variable.
    * @return
    *   random room size within [[minRoomSize]] and [[maxRoomSize]] range, respecting [[tolerance]].
    */
  protected def randomSize(bound: Int): Int = {
    val size = Generators.randomInt(
      Math.max(_minRoomSize, bound - tolerance),
      Math.min(_maxRoomSize, bound + tolerance)
    )
    normalizeSize(size)
  }

  def minRoomSize: Int = _minRoomSize

  /** @param minRoomSize
    *   minimum room's width and height. Some algorithms might require this value to be odd.
    */
  def minRoomSize_=(minRoomSize: Int): Unit = {
    if (minRoomSize <= 0 || minRoomSize > _maxRoomSize) {
      throw new IllegalArgumentException("minRoomSize cannot be bigger than max or lower than 1.")
    }
    _minRoomSize = minRoomSize
  }

  def maxRoomSize: Int = _maxRoomSize

  /** @param maxRoomSize
    *   maximum room's width and height. Some algorithms might require this value to be odd.
    */
  def maxRoomSize_=(maxRoomSize: Int): Unit = {
    if (maxRoomSize <= 0 || _minRoomSize > maxRoomSize) {
      throw new IllegalArgumentException("maxRoomSize cannot be lower than min or 1.")
    }
    _maxRoomSize = maxRoomSize
  }
}

object AbstractRoomGenerator {

  /** Basic rectangle class. Contains position and size of a single room. Provides simple, common math operations.
    *
    * @author
    *   MJ
    */
  class Room(val x: Int, val y: Int, val width: Int, val height: Int) {

    /** @param room
      *   another room instance.
      * @return
      *   true if the two rooms overlap with each other.
      */
    def overlaps(room: Room): Boolean =
      x < room.x + room.width && x + width > room.x && y < room.y + room.height && y + height > room.y

    /** @param grid
      *   its cells will be modified.
      * @param value
      *   will be used to fill all cells contained by the room.
      */
    def fill(grid: Grid, value: Float): Unit = {
      var cx    = this.x
      val sizeX = this.x + width
      while (cx < sizeX) {
        var cy    = this.y
        val sizeY = this.y + height
        while (cy < sizeY) {
          grid.set(cx, cy, value)
          cy += 1
        }
        cx += 1
      }
    }

    /** @param grid
      *   its cells will be modified.
      * @param value
      *   will be used to fill all cells contained by the room.
      */
    def fill(grid: Int2dArray, value: Int): Unit = {
      var cx    = this.x
      val sizeX = this.x + width
      while (cx < sizeX) {
        var cy    = this.y
        val sizeY = this.y + height
        while (cy < sizeY) {
          grid.set(cx, cy, value)
          cy += 1
        }
        cx += 1
      }
    }

    /** @param x
      *   column index.
      * @param y
      *   row index.
      * @return
      *   true if the passed position is on the bounds of the room.
      */
    def isBorder(x: Int, y: Int): Boolean =
      this.x == x || this.y == y || this.x + width - 1 == x || this.y + height - 1 == y

    override def toString: String =
      s"Room [x=$x, y=$y, width=$width, height=$height]"
  }
}
