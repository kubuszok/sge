/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 673
 * Covenant-baseline-methods: Direction,DungeonGenerator,Point,addConnector,carveConnector,carveCorridor,carveMaze,connectorIterator,connectors,connectorsToRegions,corridorThreshold,currentRegion,deadEndRemovalIterations,destinations,di,dirValues,directions,equals,findConnectors,findDeadEndNeighbor,floorThreshold,found,gen,generate,getDefaultRoomsAmount,getDestinations,getInstance,getRegion,hashCode,i,index,instance,isCarveable,isCorridor,isDeadEnd,isNeighbor,isWall,joinRegions,lastDirection,lastRoomRegion,maxRooms,merged,next,nextRegion,nextX,nextY,normalizePosition,normalizeSize,overlapsAny,randomConnectorChance,regions,removeDeadEnds,reset,roomGenerationAttempts,rooms,running,spawnCorridors,spawnRooms,tempSet,toString,unjoined,validateRoomSizes,w,wallThreshold,windingChance,x,y
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator
package room
package dungeon

import java.util.{ ArrayList, HashMap, HashSet, Iterator as JIterator, LinkedList, Set as JSet }

import sge.noise.generator.util.Generators

/** Generates a set of rooms with a maze-like system of corridors connecting them. This particular implementation requires the map and rooms to have odd sizes - if the passed map is not odd, last row
  * and column might be filled with corridors.
  *
  * This algorithm fills the whole map. Even if the map was not empty before, [[generate]] method will override the previous cell settings.
  *
  * @author
  *   MJ
  */
class DungeonGenerator extends AbstractRoomGenerator {

  // Settings.
  var roomGenerationAttempts: Int = 0

  /** Cells are considered walls if they are equal to or bigger than this value. */
  var wallThreshold: Float = 1f

  /** Cells are considered room floor if they are equal this value. */
  var floorThreshold: Float = 0.5f

  /** Cells are considered corridors if they are equal this value. */
  var corridorThreshold: Float = 0f

  /** Chance to wind the currently generated corridor in range of 0 to 1. */
  var windingChance: Float = 0.15f

  /** Chance of a random carved cell between two regions (rooms and corridors) in range of 0 to 1. */
  var randomConnectorChance: Float = 0.01f

  /** Amount of iterations performed to remove all dead ends in the corridors. */
  var deadEndRemovalIterations: Int = Int.MaxValue

  // Control variables.
  private val rooms:          ArrayList[AbstractRoomGenerator.Room] = new ArrayList[AbstractRoomGenerator.Room]()
  private val directions:     ArrayList[Direction]                  = new ArrayList[Direction]()
  private var regions:        Int2dArray                            = scala.compiletime.uninitialized
  private var currentRegion:  Int                                   = -1
  private var lastRoomRegion: Int                                   = -1

  override def generate(grid: Grid): Unit = {
    validateRoomSizes()
    reset()
    // Mirroring grid with a 2D int array - each non-wall mirrored cell will contain region index:
    regions = new Int2dArray(grid.width, grid.height)
    // Filling grid with wall tiles:
    grid.set(wallThreshold)
    // Generating rooms:
    spawnRooms(grid, if (roomGenerationAttempts == 0) getDefaultRoomsAmount(grid) else roomGenerationAttempts)
    // Generating corridors:
    spawnCorridors(grid)
    // Joining spawned rooms and corridor regions:
    joinRegions(grid)
    // Removing corridors leading to nowhere:
    removeDeadEnds(grid)
    reset() // Removing all unnecessary references.
  }

  /** Resets control variables. */
  protected def reset(): Unit = {
    currentRegion = -1
    lastRoomRegion = -1
    rooms.clear()
    directions.clear()
    regions = null // @nowarn -- intentional null for GC after generation
  }

  /** Increases current region index. */
  protected def nextRegion(): Unit =
    currentRegion += 1

  /** @throws IllegalStateException
    *   if room sizes are not odd.
    */
  protected def validateRoomSizes(): Unit =
    if (minRoomSize % 2 == 0 || maxRoomSize % 2 == 0) {
      throw new IllegalStateException("Min and max room sizes have to be odd.")
    }

  /** @param grid
    *   will contain generated dungeon.
    * @return
    *   maximum amount of placed rooms. Used if [[roomGenerationAttempts]] is not set.
    */
  private def getDefaultRoomsAmount(grid: Grid): Int =
    grid.width / maxRoomSize * (grid.height / maxRoomSize)

  /** @param grid
    *   is being generated.
    * @param attempts
    *   amount of attempts of placing rooms before the generator gives up.
    */
  protected def spawnRooms(grid: Grid, attempts: Int): Unit = {
    val maxRooms = maxRoomsAmount
    var index    = 0
    while (index < attempts && !(maxRooms > 0 && rooms.size() >= maxRooms)) {
      val newRoom = getRandomRoom(grid)
      if (!overlapsAny(newRoom)) {
        rooms.add(newRoom)
        carveRoom(grid, newRoom, floorThreshold)
        nextRegion()
        newRoom.fill(regions, currentRegion) // Assigning region values to all cells.
      }
      index += 1
    }
    lastRoomRegion = currentRegion
  }

  /** @param room
    *   validated room.
    * @return
    *   true if passed room overlaps with any of the current rooms.
    */
  protected def overlapsAny(room: AbstractRoomGenerator.Room): Boolean = {
    var i     = 0
    var found = false
    while (i < rooms.size() && !found) {
      if (rooms.get(i).overlaps(room)) {
        found = true
      }
      i += 1
    }
    found
  }

  /** @param grid
    *   will contain mazes spawned on the non-grid cells.
    */
  protected def spawnCorridors(grid: Grid): Unit = {
    lastRoomRegion = currentRegion
    var x = 1
    while (x < grid.width) {
      var y = 1
      while (y < grid.height) {
        if (isCarveable(grid, x, y)) {
          carveMaze(grid, new DungeonGenerator.Point(x, y))
        }
        y += 2
      }
      x += 2
    }
  }

  /** @param grid
    *   contains the cell.
    * @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   true if the selected cell is a wall.
    */
  protected def isWall(grid: Grid, x: Int, y: Int): Boolean =
    grid.isIndexValid(x, y) && grid.get(x, y) >= wallThreshold

  /** @param grid
    *   contains the point.
    * @param point
    *   will start carving maze at this point. Stops when it reaches a dead end.
    */
  protected def carveMaze(grid: Grid, point: DungeonGenerator.Point): Unit = {
    nextRegion()
    var lastDirection: Direction = null // @nowarn -- internal mutable state, null means "no previous direction"
    var running = true
    while (running) {
      // Carving current point:
      carveCorridor(grid, point)
      regions.set(point.x, point.y, currentRegion)

      directions.clear()
      // Checking neighbors - getting possible carving directions:
      val dirValues = Direction.values
      var di        = 0
      while (di < dirValues.length) {
        val dir = dirValues(di)
        if (isCarveable(point, grid, dir)) {
          directions.add(dir)
        }
        di += 1
      }
      if (directions.isEmpty) {
        running = false
      } else {
        // Getting actual carving direction:
        val carvingDirection =
          if (lastDirection != null && directions.contains(lastDirection) && Generators.randomPercent() > windingChance) {
            lastDirection
          } else {
            Generators.randomElement(directions)
          }
        lastDirection = carvingDirection
        // Carving "ignored" even-indexed corridor cell:
        carvingDirection.next(point)
        carveCorridor(grid, point)
        regions.set(point.x, point.y, currentRegion)
        // Switching to next odd-index cell, repeating until no viable neighbors left:
        carvingDirection.next(point)
      }
    }
  }

  /** @param grid
    *   contains the point.
    * @param point
    *   a point representing a part of a corridor. Should set its value in the grid.
    */
  protected def carveCorridor(grid: Grid, point: DungeonGenerator.Point): Unit =
    grid.set(point.x, point.y, corridorThreshold)

  /** @param point
    *   part of the corridor.
    * @param grid
    *   contains the point.
    * @param direction
    *   possible carving direction.
    * @return
    *   true if can carve in the selected direction.
    */
  protected def isCarveable(point: DungeonGenerator.Point, grid: Grid, direction: Direction): Boolean = {
    val x = direction.nextX(point.x, 2) // Omitting 1 field, checking the next odd one.
    val y = direction.nextY(point.y, 2)
    // Checking if index within grid bounds and not in a region yet:
    isCarveable(grid, x, y)
  }

  /** @param grid
    *   contains the point.
    * @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   true if the point can be a corridor.
    */
  protected def isCarveable(grid: Grid, x: Int, y: Int): Boolean =
    isWall(grid, x, y) &&
      // Diagonal:
      isWall(grid, x + 1, y + 1) && isWall(grid, x - 1, y + 1) &&
      isWall(grid, x + 1, y - 1) && isWall(grid, x - 1, y - 1)

  /** @param grid
    *   contains unconnected room and corridor regions.
    */
  protected def joinRegions(grid: Grid): Unit = { // DRAGON.
    nextRegion()
    // Working on boxed primitives, because lawl, Java generics and collections.
    val connectorsToRegions = findConnectors(grid)
    val connectors          = new ArrayList[DungeonGenerator.Point](connectorsToRegions.keySet())
    val merged              = new Array[Int](currentRegion) // Keeps track of merged regions.
    val unjoined            = new HashSet[Int]() // Keeps track of unconnected regions.
    var index               = 0
    while (index < currentRegion) {
      // All regions point to themselves at first:
      merged(index) = index
      // All regions start unjoined:
      unjoined.add(index)
      index += 1
    }
    Generators.shuffle(connectors)
    val tempSet = new HashSet[Int]()
    // Looping until all regions point to one source:
    val connectorIterator = connectors.iterator()
    while (connectorIterator.hasNext && unjoined.size() > 1) {
      val connector = connectorIterator.next()
      // These are the regions that the connector originally pointed to - we need to convert them to the "new",
      // merged region indexes:
      val connectorRegions = connectorsToRegions.get(connector)
      tempSet.clear()
      val regIter = connectorRegions.iterator()
      while (regIter.hasNext)
        tempSet.add(merged(regIter.next()))
      if (tempSet.size() <= 1) { // All connector's regions point to the same region group...
        if (Generators.randomPercent() < randomConnectorChance) {
          // This connector is not actually needed, but it got lucky - carving:
          carveConnector(grid, connector.x, connector.y)
        }
      } else {
        carveConnector(grid, connector.x, connector.y)
        connectorRegions.clear()
        val tempIter = tempSet.iterator()
        while (tempIter.hasNext)
          connectorRegions.add(tempIter.next())
        val regionsIterator = connectorRegions.iterator()
        // Using first region as our "source":
        val source = regionsIterator.next() // Safe, has at least 2 regions.
        // Using the rest of the regions as destinations:
        val destinations = getDestinations(connectorRegions, regionsIterator, merged)
        // Changing merged status - all regions that currently point to destinations will now point to source:
        var regionIndex = 0
        while (regionIndex < currentRegion) {
          var destIdx = 0
          while (destIdx < destinations.length) {
            if (merged(regionIndex) == destinations(destIdx)) {
              merged(regionIndex) = source
            }
            destIdx += 1
          }
          regionIndex += 1
        }
        // Removing destinations from unjoined regions:
        var di = 0
        while (di < destinations.length) {
          unjoined.remove(destinations(di))
          di += 1
        }
      }
    }
  }

  /** Should change selected point's value. Note that connector might connect both two corridors and two rooms.
    *
    * @param grid
    *   contains the point.
    * @param x
    *   column index.
    * @param y
    *   row index.
    */
  protected def carveConnector(grid: Grid, x: Int, y: Int): Unit = {
    grid.set(x, y, corridorThreshold)
    regions.set(x, y, lastRoomRegion + 1) // Treating connector as corridor.
  }

  /** @param grid
    *   contains unconnected room and corridor regions.
    * @return
    *   map of points that are neighbors to at least 2 different regions mapped to set of IDs of their neighbors.
    */
  protected def findConnectors(grid: Grid): HashMap[DungeonGenerator.Point, HashSet[Int]] = {
    val connectorsToRegions = new HashMap[DungeonGenerator.Point, HashSet[Int]]()
    var x                   = 1
    val w                   = grid.width - 1
    while (x < w) {
      var y = 1
      val h = grid.height - 1
      while (y < h) {
        addConnector(grid, connectorsToRegions, x, y)
        y += 1
      }
      x += 1
    }
    connectorsToRegions
  }

  /** @param grid
    *   contains the regions.
    * @param connectorsToRegions
    *   map of possible connectors to the collection of regions that are their neighbors.
    * @param x
    *   column index of possible connector.
    * @param y
    *   row index of possible connector.
    */
  protected def addConnector(
    grid:                Grid,
    connectorsToRegions: HashMap[DungeonGenerator.Point, HashSet[Int]],
    x:                   Int,
    y:                   Int
  ): Unit =
    if (isWall(grid, x, y)) {
      val connRegions = new HashSet[Int](4, 1f)
      val dirValues   = Direction.values
      var di          = 0
      while (di < dirValues.length) {
        val direction = dirValues(di)
        val nx        = direction.nextX(x)
        val ny        = direction.nextY(y)
        val region    = getRegion(nx, ny)
        if (region >= 0 && !isWall(grid, nx, ny)) {
          connRegions.add(region)
        }
        di += 1
      }
      if (connRegions.size() > 1) { // At least 2 regions.
        connectorsToRegions.put(new DungeonGenerator.Point(x, y), connRegions)
      }
    }

  /** @param regions
    *   all regions of a connector.
    * @param regionsIterator
    *   regions' iterator. Should have one value skipped (source).
    * @param merged
    *   contains mapping of regions to the IDs of their supergroups.
    * @return
    *   regions marked as destinations.
    * @see
    *   [[joinRegions]]
    */
  protected def getDestinations(
    regions:         JSet[Int],
    regionsIterator: JIterator[Int],
    merged:          Array[Int]
  ): Array[Int] = {
    val destinations = new Array[Int](regions.size() - 1)
    var index        = 0
    while (regionsIterator.hasNext) {
      destinations(index) = merged(regionsIterator.next())
      index += 1
    }
    destinations
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   region index of the cell. -1 if not in a region.
    */
  protected def getRegion(x: Int, y: Int): Int =
    if (regions.isIndexValid(x, y)) {
      regions.get(x, y)
    } else {
      -1
    }

  /** @param grid
    *   will have its cells with 3 or 4 wall neighbors removed.
    */
  protected def removeDeadEnds(grid: Grid): Unit =
    if (deadEndRemovalIterations <= 0) {
      // user wants us to leave all dead ends
    } else {
      val deadEnds = new LinkedList[DungeonGenerator.Point]()
      var x        = 0
      while (x < grid.width) {
        var y = 0
        while (y < grid.height) {
          if (isDeadEnd(grid, x, y)) {
            deadEnds.add(new DungeonGenerator.Point(x, y))
          }
          y += 1
        }
        x += 1
      }
      // Removing dead ends until there are none left or we've done enough iterations:
      var iteration = 0
      while (iteration < deadEndRemovalIterations && !deadEnds.isEmpty) {
        val iterator = deadEnds.iterator()
        while (iterator.hasNext) {
          val deadEnd = iterator.next()
          // Closing dead end:
          grid.set(deadEnd.x, deadEnd.y, wallThreshold)
          // Checking dead end neighbors - one (and only one) of them can be a dead end too:
          if (!findDeadEndNeighbor(grid, deadEnd)) {
            // No dead end neighbors found - removing dead end from list:
            iterator.remove()
          } // else { Point becomes its neighbor - will be removed on next iteration (or never). }
        }
        iteration += 1
      }
    }

  /** @param grid
    *   contains the cell.
    * @param deadEnd
    *   a currently closed dead end that can possibly have a single dead end neighbor.
    * @return
    *   true if dead end neighbor present.
    */
  private def findDeadEndNeighbor(grid: Grid, deadEnd: DungeonGenerator.Point): Boolean = {
    val dirValues = Direction.values
    var di        = 0
    var found     = false
    while (di < dirValues.length && !found) {
      val direction = dirValues(di)
      val nx        = direction.nextX(deadEnd.x)
      val ny        = direction.nextY(deadEnd.y)
      if (isDeadEnd(grid, nx, ny)) {
        // Setting dead end as its neighbor:
        deadEnd.x = nx
        deadEnd.y = ny
        found = true
      }
      di += 1
    }
    found
  }

  /** @param grid
    *   contains the cell.
    * @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   true if the cell has at least 3 wall neighbors.
    */
  protected def isDeadEnd(grid: Grid, x: Int, y: Int): Boolean =
    if (grid.isIndexValid(x, y) && !isWall(grid, x, y) && isCorridor(x, y)) {
      var wallNeighbors = 0
      val dirValues     = Direction.values
      var di            = 0
      while (di < dirValues.length) {
        val direction = dirValues(di)
        val nx        = direction.nextX(x)
        val ny        = direction.nextY(y)
        if (grid.isIndexValid(nx, ny) && isWall(grid, nx, ny)) {
          wallNeighbors += 1
        }
        di += 1
      }
      wallNeighbors >= 3
    } else {
      false
    }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   true if selected cell is a corridor. Works only if the cell is not a wall. This check works if all rooms and corridors are already spawned.
    */
  protected def isCorridor(x: Int, y: Int): Boolean =
    getRegion(x, y) > lastRoomRegion

  override protected def normalizePosition(position: Int): Int =
    if (position == 0) {
      1
    } else if (position % 2 == 0) {
      position - 1
    } else {
      position
    }

  override protected def normalizeSize(size: Int): Int =
    if (size % 2 != 1) {
      if (Generators.getRandom.nextBoolean()) size - 1 else size + 1
    } else {
      size
    }
}

object DungeonGenerator {

  private var instance: DungeonGenerator = scala.compiletime.uninitialized

  /** Not thread-safe. Uses static generator instance.
    *
    * @param grid
    *   will be used to generate the dungeon.
    * @param roomGenerationAttempts
    *   see [[DungeonGenerator.roomGenerationAttempts]].
    */
  def generate(grid: Grid, roomGenerationAttempts: Int): Unit = {
    val gen = getInstance()
    gen.roomGenerationAttempts = roomGenerationAttempts
    gen.generate(grid)
  }

  /** @return
    *   static instance of the generator. Not thread-safe.
    */
  def getInstance(): DungeonGenerator = {
    if (instance == null) {
      instance = new DungeonGenerator
    }
    instance
  }

  /** A simple container class, storing 2 values.
    *
    * @author
    *   MJ
    */
  protected[dungeon] class Point(var x: Int, var y: Int) {

    /** @param point
      *   another point.
      * @return
      *   true if the passed point is a direct (non-diagonal) neighbor of this point.
      */
    def isNeighbor(point: Point): Boolean = {
      val xDifference = Math.abs(x - point.x)
      if (xDifference == 0) {
        Math.abs(y - point.y) == 1
      } else if (xDifference == 1) {
        Math.abs(y - point.y) == 0
      } else {
        false
      }
    }

    override def equals(obj: Any): Boolean =
      obj match {
        case that: Point => (this eq that) || (that.x == x && that.y == y)
        case _ => false
      }

    override def hashCode(): Int = x + y * 653

    override def toString: String = s"[$x,$y]"
  }
}

/** Contains all possible corridor carving directions.
  *
  * @author
  *   MJ
  */
private[dungeon] enum Direction {
  case UP, DOWN, LEFT, RIGHT

  /** @param point
    *   a point in the grid. Its coordinates will be modified to represent the next cell in the chosen direction.
    */
  def next(point: DungeonGenerator.Point): Unit =
    this match {
      case UP    => point.y += 1
      case DOWN  => point.y -= 1
      case LEFT  => point.x -= 1
      case RIGHT => point.x += 1
    }

  /** @param x
    *   current column index.
    * @return
    *   column index of the next cell.
    */
  def nextX(x: Int): Int = nextX(x, 1)

  /** @param y
    *   current row index.
    * @return
    *   row index of the next cell.
    */
  def nextY(y: Int): Int = nextY(y, 1)

  /** @param x
    *   current column index.
    * @param amount
    *   distance from the selected cell.
    * @return
    *   column index of the selected cell.
    */
  def nextX(x: Int, amount: Int): Int =
    this match {
      case LEFT  => x - amount
      case RIGHT => x + amount
      case _     => x
    }

  /** @param y
    *   current row index.
    * @param amount
    *   distance from the selected cell.
    * @return
    *   row index of the selected cell.
    */
  def nextY(y: Int, amount: Int): Int =
    this match {
      case UP   => y + amount
      case DOWN => y - amount
      case _    => y
    }
}
