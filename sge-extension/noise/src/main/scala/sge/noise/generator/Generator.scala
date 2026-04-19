/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 74
 * Covenant-baseline-methods: GenerationMode,Generator,generate,mode,mode_,modify
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator

/** Common interface for all map generators.
  *
  * @author
  *   MJ
  */
trait Generator {

  /** @param grid
    *   all (or most) of its cells will be affected, usually by adding or subtracting their current cell value.
    */
  def generate(grid: Grid): Unit

  /** @return
    *   current generation mode, deciding how values modify current grid's cells.
    */
  def mode: GenerationMode

  /** @param mode
    *   decides how values modify current grid's cells.
    */
  def mode_=(mode: GenerationMode): Unit
}

/** Decides how values modify current grid's cells.
  *
  * @author
  *   MJ
  */
enum GenerationMode {

  /** Adds value to the current cell's value. Default. */
  case ADD

  /** Subtracts value from the current cell's value. */
  case SUBTRACT

  /** Multiplies current cell's value. */
  case MULTIPLY

  /** Divides current cell's value. */
  case DIVIDE

  /** Replaces current cell's value. */
  case REPLACE

  /** @param grid
    *   contains a cell.
    * @param x
    *   cell column index.
    * @param y
    *   cell row index.
    * @param value
    *   will modify current cell value.
    */
  def modify(grid: Grid, x: Int, y: Int, value: Float): Unit =
    this match {
      case ADD      => grid.add(x, y, value)
      case SUBTRACT => grid.subtract(x, y, value)
      case MULTIPLY => grid.multiply(x, y, value)
      case DIVIDE   => grid.divide(x, y, value)
      case REPLACE  => grid.set(x, y, value)
    }
}
