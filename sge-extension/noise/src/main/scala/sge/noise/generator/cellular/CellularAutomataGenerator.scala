/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 244
 * Covenant-baseline-methods: CellularAutomataGenerator,aliveChance,array,birthLimit,consume,count,countLivingNeighbors,deathLimit,gen,generate,getInstance,getTemporaryGrid,index,initiate,instance,isAlive,iterationIndex,iterationsAmount,length,livingNeighbors,marker,radius,random,setAlive,setDead,shouldBeBorn,shouldDie,spawnLivingCells,temporaryGrid,xOffset
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator
package cellular

import sge.noise.generator.util.Generators

/** Contains a marker - a single float value; every cell below this value is considered dead, the others are alive. During each iteration, if a living cell has too few living neighbors, it will die
  * (marker will be subtracted from its value). If a dead cell has enough living neighbors, it will become alive (marker will modify its current value according to [[mode]] - by default, marker will
  * be added). This usually results in a cave-like map. The more iterations, the smoother the map is.
  *
  * Since this generator creates pretty much boolean-based maps (sets each cell as dead or alive), this generator is usually used first to create the general layout of the map - like a caverns system
  * or islands.
  *
  * @author
  *   MJ
  */
class CellularAutomataGenerator extends AbstractGenerator with Grid.CellConsumer {

  /** If true, some alive cells will be spawned before the generation. */
  var initiate: Boolean = true

  /** If cell is equal to or greater than this value, it is considered alive. When the cell dies, this value is subtracted from it. If the cell becomes alive, this value modifies the current cell
    * value according to current mode.
    */
  var marker: Float = 1f

  /** If [[initiate]] is true, some alive cells will be spawned before the generation. This is the chance of the cell becoming alive before generating. In range from 0 to 1.
    */
  var aliveChance: Float = 0.5f

  /** Amount of generation iterations. The more iterations, the smoother the result. */
  var iterationsAmount: Int = 3

  /** Dead cell becomes alive if it has more alive neighbors than this value. The lesser this value is, the more alive cells will be present.
    */
  var birthLimit: Int = 4

  /** Living cell dies if it has less alive neighbors than this value. The higher this value is, the less smooth the map becomes.
    */
  var deathLimit: Int = 3

  /** Determines how far the cells can be from a cell to be considered neighbors. Defaults to 1 - only direct cell neighbors (sides + corners) are counted.
    */
  var radius: Int = 1

  private var temporaryGrid: Grid = scala.compiletime.uninitialized

  override def generate(grid: Grid): Unit = {
    if (initiate) {
      spawnLivingCells(grid)
    }
    // Grid is copied to keep the correct living neighbors count. Otherwise it would change during iterations.
    temporaryGrid = grid.copy()
    var iterationIndex = 0
    while (iterationIndex < iterationsAmount) {
      grid.forEach(this)
      grid.set(temporaryGrid)
      iterationIndex += 1
    }
  }

  /** @param grid
    *   some of its cells will become alive, according to the current chance settings. The others will die, if they were already alive.
    * @see
    *   [[aliveChance]]
    */
  protected def spawnLivingCells(grid: Grid): Unit =
    CellularAutomataGenerator.initiate(grid, aliveChance, marker)

  /** @return
    *   temporary grid, copied to preserve the correct amounts of living neighbors during iterations.
    */
  protected def getTemporaryGrid: Grid = temporaryGrid

  override def consume(grid: Grid, x: Int, y: Int, value: Float): Boolean = {
    val livingNeighbors = countLivingNeighbors(grid, x, y)
    if (isAlive(value)) {
      if (shouldDie(livingNeighbors)) {
        setDead(x, y)
      }
    } else if (shouldBeBorn(livingNeighbors)) {
      setAlive(x, y)
    }
    Grid.CellConsumer.Continue
  }

  /** Makes the cell alive in temporary cached grid copy.
    *
    * @param x
    *   column index of temporary grid.
    * @param y
    *   row index of temporary grid.
    */
  protected def setAlive(x: Int, y: Int): Unit =
    modifyCell(temporaryGrid, x, y, marker)

  /** Kills the cell in temporary cached grid copy.
    *
    * @param x
    *   column index of temporary grid.
    * @param y
    *   row index of temporary grid.
    */
  protected def setDead(x: Int, y: Int): Unit =
    temporaryGrid.subtract(x, y, marker)

  /** @param aliveNeighbors
    *   amount of alive tile's neighbors.
    * @return
    *   true if tile has less alive neighbors than the current death limit.
    */
  protected def shouldDie(aliveNeighbors: Int): Boolean =
    aliveNeighbors < deathLimit

  /** @param aliveNeighbors
    *   amount of alive tile's neighbors.
    * @return
    *   true if tile has more alive neighbors than the current birth limit.
    */
  protected def shouldBeBorn(aliveNeighbors: Int): Boolean =
    aliveNeighbors > birthLimit

  /** @param value
    *   current cell's value.
    * @return
    *   true if the cell is currently considered alive.
    */
  protected def isAlive(value: Float): Boolean =
    value >= marker

  /** @param grid
    *   processed grid.
    * @param x
    *   column index of a cell.
    * @param y
    *   row index of a cell.
    * @return
    *   amount of neighbor cells that are considered alive.
    */
  protected def countLivingNeighbors(grid: Grid, x: Int, y: Int): Int = {
    var count   = 0
    var xOffset = -radius
    while (xOffset <= radius) {
      var yOffset = -radius
      while (yOffset <= radius) {
        if (!(xOffset == 0 && yOffset == 0)) { // Not the same tile.
          val neighborX = x + xOffset
          val neighborY = y + yOffset
          if (grid.isIndexValid(neighborX, neighborY) && isAlive(grid.get(neighborX, neighborY))) {
            count += 1
          }
        }
        yOffset += 1
      }
      xOffset += 1
    }
    count
  }
}

object CellularAutomataGenerator {

  private var instance: CellularAutomataGenerator = scala.compiletime.uninitialized

  /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or obtaining an instance of the generator is generally preferred.
    *
    * @param grid
    *   its cells will be affected.
    * @param iterationsAmount
    *   see [[CellularAutomataGenerator.iterationsAmount]]
    */
  def generate(grid: Grid, iterationsAmount: Int): Unit =
    generate(grid, iterationsAmount, 1f, initiate = true)

  /** Not thread-safe. Uses static generator instance.
    *
    * @param grid
    *   its cells will be affected.
    * @param iterationsAmount
    *   see [[CellularAutomataGenerator.iterationsAmount]]
    * @param marker
    *   see [[CellularAutomataGenerator.marker]]
    * @param initiate
    *   see [[CellularAutomataGenerator.initiate]]
    */
  def generate(grid: Grid, iterationsAmount: Int, marker: Float, initiate: Boolean): Unit = {
    val gen = getInstance()
    gen.iterationsAmount = iterationsAmount
    gen.marker = marker
    gen.initiate = initiate
    gen.generate(grid)
  }

  /** @return
    *   static instance of the generator. Not thread-safe.
    */
  def getInstance(): CellularAutomataGenerator = {
    if (instance == null) {
      instance = new CellularAutomataGenerator
    }
    instance
  }

  /** @param grid
    *   its cells will be initiated.
    * @param generator
    *   its settings will be used.
    */
  def initiate(grid: Grid, generator: CellularAutomataGenerator): Unit =
    initiate(grid, generator.aliveChance, generator.marker)

  /** @param grid
    *   its cells will be initiated.
    * @param aliveChance
    *   see [[CellularAutomataGenerator.aliveChance]].
    * @param marker
    *   see [[CellularAutomataGenerator.marker]]. If value is already above the marker and rolled as alive, its value will not be changed. If cell's value is above the marker and it is rolled as dead,
    *   marker will be subtracted from its value.
    */
  def initiate(grid: Grid, aliveChance: Float, marker: Float): Unit = {
    val random = Generators.getRandom
    val array  = grid.getArray
    var index  = 0
    val length = array.length
    while (index < length) {
      if (random.nextFloat() > aliveChance) {
        if (array(index) < marker) {
          grid.add(grid.toX(index), grid.toY(index), marker)
        }
      } else if (array(index) >= marker) { // Is alive - killing it.
        grid.subtract(grid.toX(index), grid.toY(index), marker)
      }
      index += 1
    }
  }
}
