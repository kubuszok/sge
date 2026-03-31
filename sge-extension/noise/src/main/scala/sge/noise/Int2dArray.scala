/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noise

/** A simple 2D int array wrapper. Stores a 1D primitive int array, treating it as a 2D array.
  *
  * @author
  *   MJ
  */
class Int2dArray(val width: Int, val height: Int) {

  private val array: Array[Int] = new Array[Int](width * height)

  /** @param size
    *   amount of columns and rows.
    */
  def this(size: Int) = this(size, size)

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   true if the coordinates are valid.
    */
  def isIndexValid(x: Int, y: Int): Boolean =
    x >= 0 && x < width && y >= 0 && y < height

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   actual array index of the cell.
    */
  def toIndex(x: Int, y: Int): Int = x + y * width

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @return
    *   cell value with the selected index.
    */
  def get(x: Int, y: Int): Int = array(toIndex(x, y))

  /** @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will become the value stored in the selected cell.
    */
  def set(x: Int, y: Int, value: Int): Unit =
    array(toIndex(x, y)) = value

  /** @param value
    *   will replace all cells' values.
    * @return
    *   this, for chaining.
    */
  def set(value: Int): Int2dArray = {
    var index  = 0
    val length = array.length
    while (index < length) {
      array(index) = value
      index += 1
    }
    this
  }
}
