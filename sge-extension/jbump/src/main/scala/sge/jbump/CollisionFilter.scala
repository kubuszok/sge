/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: CollisionFilter,defaultFilter,filter
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.Nullable

/** Filter trait that determines collision response type for item pairs. */
trait CollisionFilter {

  def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response]
}

object CollisionFilter {

  val defaultFilter: CollisionFilter = new CollisionFilter {
    override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] = Response.slide
  }
}
