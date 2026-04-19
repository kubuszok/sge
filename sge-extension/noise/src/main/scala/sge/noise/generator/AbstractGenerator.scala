/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: AbstractGenerator,_mode,mode,mode_,modifyCell
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator

/** Abstract base for map generators. Manages current [[GenerationMode]].
  *
  * @author
  *   MJ
  */
abstract class AbstractGenerator extends Generator {

  private var _mode: GenerationMode = GenerationMode.ADD

  override def mode: GenerationMode = _mode

  override def mode_=(mode: GenerationMode): Unit = {
    if (mode == null) { // @nowarn -- Java interop boundary guard
      throw new IllegalArgumentException("Generation mode cannot be null.")
    }
    _mode = mode
  }

  /** @param grid
    *   processed grid.
    * @param x
    *   column index.
    * @param y
    *   row index.
    * @param value
    *   will modify current cell value.
    */
  protected def modifyCell(grid: Grid, x: Int, y: Int, value: Float): Unit =
    _mode.modify(grid, x, y, value)
}
