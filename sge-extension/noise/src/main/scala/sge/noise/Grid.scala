/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noise

import java.util.Arrays

/** A float array wrapper. Allows to use a single 1D float array as a 2D array.
  *
  * @param grid
  *   internal array. Its size must equal width * height.
  * @param width
  *   amount of columns.
  * @param height
  *   amount of rows.
  * @author
  *   MJ
  */
class Grid(private val grid: Array[Float], val width: Int, val height: Int) {

  if (grid.length != width * height) {
    throw new IllegalArgumentException(
      s"Array with length: ${grid.length} is too small or too big to store a grid with $width columns and $height rows."
    )
  }

  /** @param size
    *   amount of columns and rows.
    */
  def this(size: Int) = this(new Array[Float](size * size), size, size)

  /** @param width
    *   amount of columns.
    * @param height
    *   amount of rows.
    */
  def this(width: Int, height: Int) = this(new Array[Float](width * height), width, height)

  /** @param initialValue
    *   all cells will start with this value.
    * @param width
    *   amount of columns.
    * @param height
    *   amount of rows.
    */
  def this(initialValue: Float, width: Int, height: Int) = {
    this(new Array[Float](width * height), width, height)
    set(initialValue)
  }

  /** @return
    *   direct reference to the stored array. Use in extreme cases, try to use getters instead.
    */
  def getArray: Array[Float] = grid

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   true if the coordinates are valid and can be safely used with getter methods.
    */
  def isIndexValid(x: Int, y: Int): Boolean = {
    x >= 0 && x < width && y >= 0 && y < height
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   actual array index of the cell.
    */
  def toIndex(x: Int, y: Int): Int = x + y * width

  /** @param index
    *   actual array index of a cell.
    * @return
    *   column index.
    */
  def toX(index: Int): Int = index % width

  /** @param index
    *   actual array index of a cell.
    * @return
    *   row index.
    */
  def toY(index: Int): Int = index / width

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   value stored in the chosen cell.
    */
  def get(x: Int, y: Int): Float = grid(toIndex(x, y))

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will be set as the value in the chosen cell.
    * @return
    *   value (parameter), for chaining.
    */
  def set(x: Int, y: Int, value: Float): Float = {
    val idx = toIndex(x, y)
    grid(idx) = value
    value
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will be added to the current value stored in the chosen cell.
    * @return
    *   current cell value after adding the passed parameter.
    */
  def add(x: Int, y: Int, value: Float): Float = {
    val idx = toIndex(x, y)
    grid(idx) += value
    grid(idx)
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will be subtracted from the current value stored in the chosen cell.
    * @return
    *   current cell value after subtracting the passed parameter.
    */
  def subtract(x: Int, y: Int, value: Float): Float = {
    val idx = toIndex(x, y)
    grid(idx) -= value
    grid(idx)
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will be multiplied by the current value stored in the chosen cell.
    * @return
    *   current cell value after multiplying by the passed parameter.
    */
  def multiply(x: Int, y: Int, value: Float): Float = {
    val idx = toIndex(x, y)
    grid(idx) *= value
    grid(idx)
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will divide the current value stored in the chosen cell.
    * @return
    *   current cell value after dividing by the passed parameter.
    */
  def divide(x: Int, y: Int, value: Float): Float = {
    val idx = toIndex(x, y)
    grid(idx) /= value
    grid(idx)
  }

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param mod
    *   will be used to perform modulo operation on the current cell value.
    * @return
    *   current cell value after modulo operation.
    */
  def modulo(x: Int, y: Int, mod: Float): Float = {
    val idx = toIndex(x, y)
    grid(idx) %= mod
    grid(idx)
  }

  /** Iterates over the whole grid.
    *
    * @param cellConsumer
    *   will consume each cell. If returns true, further iteration will be cancelled.
    */
  def forEach(cellConsumer: Grid.CellConsumer): Unit = {
    iterate(cellConsumer, 0, grid.length)
  }

  /** Iterates over the grid from a starting point.
    *
    * @param cellConsumer
    *   will consume each cell. If returns true, further iteration will be cancelled.
    * @param fromX
    *   first cell column index. Min is 0.
    * @param fromY
    *   first cell row index. Min is 0.
    */
  def forEach(cellConsumer: Grid.CellConsumer, fromX: Int, fromY: Int): Unit = {
    iterate(cellConsumer, toIndex(fromX, fromY), grid.length)
  }

  /** Iterates over chosen cells range in the grid.
    *
    * @param cellConsumer
    *   will consume each cell. If returns true, further iteration will be cancelled.
    * @param fromX
    *   first cell column index. Min is 0.
    * @param fromY
    *   first cell row index. Min is 0.
    * @param toX
    *   last cell column index (excluded). Max is [[width]].
    * @param toY
    *   last cell row index (excluded). Max is [[height]].
    */
  def forEach(cellConsumer: Grid.CellConsumer, fromX: Int, fromY: Int, toX: Int, toY: Int): Unit = {
    iterate(cellConsumer, toIndex(fromX, fromY), toIndex(toX, toY))
  }

  /** @param cellConsumer
    *   will consume each cell. If returns true, further iteration will be cancelled.
    * @param fromIndex
    *   actual array index, begins the iteration. Min is 0.
    * @param toIndex
    *   actual array index (excluded); iteration ends with this value -1. Max is length of array.
    */
  protected def iterate(cellConsumer: Grid.CellConsumer, fromIndex: Int, toIndex: Int): Unit = {
    var index = fromIndex
    while (index < toIndex) {
      if (cellConsumer.consume(this, this.toX(index), this.toY(index), grid(index))) {
        return // scalastyle:ignore -- boundary/break not needed for simple early exit in private method
      }
      index += 1
    }
  }

  /** @param other
    *   its values will replace this grid's values.
    */
  def set(other: Grid): Unit = {
    validateGrid(other)
    System.arraycopy(other.grid, 0, this.grid, 0, this.grid.length)
  }

  /** @param other
    *   its values will be added to this grid's values.
    */
  def add(other: Grid): Unit = {
    validateGrid(other)
    var index = 0
    val length = this.grid.length
    while (index < length) {
      this.grid(index) += other.grid(index)
      index += 1
    }
  }

  /** @param other
    *   its values will be subtracted from this grid's values.
    */
  def subtract(other: Grid): Unit = {
    validateGrid(other)
    var index = 0
    val length = this.grid.length
    while (index < length) {
      this.grid(index) -= other.grid(index)
      index += 1
    }
  }

  /** @param other
    *   its values will multiply this grid's values.
    */
  def multiply(other: Grid): Unit = {
    validateGrid(other)
    var index = 0
    val length = this.grid.length
    while (index < length) {
      this.grid(index) *= other.grid(index)
      index += 1
    }
  }

  /** @param other
    *   its values will be used to divide this grid's values.
    */
  def divide(other: Grid): Unit = {
    validateGrid(other)
    var index = 0
    val length = this.grid.length
    while (index < length) {
      this.grid(index) /= other.grid(index)
      index += 1
    }
  }

  /** @param other
    *   will be validated.
    * @throws IllegalStateException
    *   if sizes do not match.
    */
  protected def validateGrid(other: Grid): Unit = {
    if (other.width != width || other.height != height) {
      throw new IllegalStateException("Grid's sizes do not match. Unable to perform operation.")
    }
  }

  /** Sets all values in the map.
    *
    * @param value
    *   will be set.
    * @return
    *   this, for chaining.
    */
  def set(value: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) = value
      index += 1
    }
    this
  }

  /** Sets all values in the map.
    *
    * @param value
    *   will be set.
    * @return
    *   this, for chaining.
    * @see
    *   [[set(Float)]]
    */
  def fill(value: Float): Grid = set(value)

  /** Sets all values in the selected column.
    *
    * @param x
    *   column index.
    * @param value
    *   will be set.
    * @return
    *   this, for chaining.
    */
  def fillColumn(x: Int, value: Float): Grid = {
    var y = 0
    while (y < height) {
      grid(toIndex(x, y)) = value
      y += 1
    }
    this
  }

  /** Sets all values in the selected row.
    *
    * @param y
    *   row index.
    * @param value
    *   will be set.
    * @return
    *   this, for chaining.
    */
  def fillRow(y: Int, value: Float): Grid = {
    var x = 0
    while (x < width) {
      grid(toIndex(x, y)) = value
      x += 1
    }
    this
  }

  /** Increases all values in the map.
    *
    * @param value
    *   will be added.
    * @return
    *   this, for chaining.
    */
  def add(value: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) += value
      index += 1
    }
    this
  }

  /** Decreases all values in the map.
    *
    * @param value
    *   will be subtracted.
    * @return
    *   this, for chaining.
    */
  def subtract(value: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) -= value
      index += 1
    }
    this
  }

  /** Multiplies all values in the map.
    *
    * @param value
    *   will be used.
    * @return
    *   this, for chaining.
    */
  def multiply(value: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) *= value
      index += 1
    }
    this
  }

  /** Divides all values in the map.
    *
    * @param value
    *   will be used.
    * @return
    *   this, for chaining.
    */
  def divide(value: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) /= value
      index += 1
    }
    this
  }

  /** Performs modulo operation on all values in the map.
    *
    * @param modulo
    *   will be used.
    * @return
    *   this, for chaining.
    */
  def modulo(modulo: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) %= modulo
      index += 1
    }
    this
  }

  /** Negates all values in the map.
    *
    * @return
    *   this, for chaining.
    */
  def negate(): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) = -grid(index)
      index += 1
    }
    this
  }

  /** @param min
    *   all values lower than this value will be converted to this value.
    * @param max
    *   all values higher than this value will be converted to this value.
    * @return
    *   this, for chaining.
    */
  def clamp(min: Float, max: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      val value = grid(index)
      grid(index) = if (value > max) max else if (value < min) min else value
      index += 1
    }
    this
  }

  /** @param value
    *   cells storing this value will be replaced.
    * @param withValue
    *   this value will replace the affected cells.
    * @return
    *   this, for chaining.
    */
  def replace(value: Float, withValue: Float): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      if (java.lang.Float.compare(grid(index), value) == 0) {
        grid(index) = withValue
      }
      index += 1
    }
    this
  }

  /** Increases value in each cell by 1.
    *
    * @return
    *   this, for chaining.
    */
  def increment(): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) += 1f
      index += 1
    }
    this
  }

  /** Decreases value in each cell by 1.
    *
    * @return
    *   this, for chaining.
    */
  def decrement(): Grid = {
    var index = 0
    val length = grid.length
    while (index < length) {
      grid(index) -= 1f
      index += 1
    }
    this
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case that: Grid => (this eq that) || (that.width == width && Arrays.equals(that.grid, grid))
      case _          => false
    }
  }

  override def hashCode(): Int = Arrays.hashCode(grid)

  /** [[clone]] alternative with casted result.
    *
    * @return
    *   a new instance of the grid with same size and values.
    */
  def copy(): Grid = {
    val copyArr = new Array[Float](grid.length)
    System.arraycopy(grid, 0, copyArr, 0, copyArr.length)
    new Grid(copyArr, width, height)
  }

  override def toString: String = {
    val logger = new StringBuilder
    forEach(new Grid.CellConsumer {
      override def consume(grid: Grid, x: Int, y: Int, value: Float): Boolean = {
        logger.append('[').append(x).append(',').append(y).append('|').append(value).append(']')
        if (x == grid.width - 1) {
          logger.append('\n')
        } else {
          logger.append(' ')
        }
        Grid.CellConsumer.Continue
      }
    })
    logger.toString
  }
}

object Grid {

  /** Allows to perform an action on [[Grid]]'s cells.
    *
    * @author
    *   MJ
    */
  trait CellConsumer {

    /** @param grid
      *   contains the cell.
      * @param x
      *   column index of the current cell.
      * @param y
      *   row index of the current cell.
      * @param value
      *   value stored in the current cell.
      * @return
      *   if true, further iteration will be cancelled.
      * @see
      *   [[CellConsumer.Break]]
      * @see
      *   [[CellConsumer.Continue]]
      */
    def consume(grid: Grid, x: Int, y: Int, value: Float): Boolean
  }

  object CellConsumer {

    /** Should be returned by [[CellConsumer.consume]] for code clarity. */
    val Break: Boolean = true

    /** Should be returned by [[CellConsumer.consume]] for code clarity. */
    val Continue: Boolean = false
  }
}
