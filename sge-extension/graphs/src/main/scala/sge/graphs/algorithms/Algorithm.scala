/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 20
 * Covenant-baseline-methods: Algorithm,finish,isFinished,update
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs
package algorithms

/** Abstract base for graph algorithms that can be stepped or run to completion. */
abstract class Algorithm[V](protected val id: Int) {

  def update(): Boolean

  def isFinished: Boolean

  def finish(): Unit =
    while (!update()) {}
}
