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
 * Covenant-source-reference: com/dongbat/jbump/CollisionFilter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

import scala.language.implicitConversions

import lowlevel.Nullable

/** Filter trait that determines collision response type for item pairs. */
trait CollisionFilter {

  // Both parameters are possibly-null Item references in the Java original (CollisionFilter.java:24):
  // World.project supports a null projected item (World.java:244) and passes it straight to
  // filter.filter(item, other) (World.java:264), so the first param must be Nullable too (ISS-595).
  def filter(item: Nullable[Item[?]], other: Nullable[Item[?]]): Nullable[Response]
}

object CollisionFilter {

  val defaultFilter: CollisionFilter = new CollisionFilter {
    override def filter(item: Nullable[Item[?]], other: Nullable[Item[?]]): Nullable[Response] = Response.slide
  }
}
