/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.Nullable

/** Red test for ISS-595 (ISS-503 clause-2 split): `CollisionFilter.filter`'s FIRST parameter must be `Nullable[Item[?]]`, not a non-Nullable `Item[?]`.
  *
  * Java reference (original-src/jbump/jbump/src/com/dongbat/jbump/):
  *   - `World.project` (World.java:240-280) supports `item == null` (line 244 guards `visited.add(item)`) and passes that possibly-null item STRAIGHT into `filter.filter(item, other)`
  *     (World.java:264). So a `CollisionFilter` is contractually handed a null (absent) item during null-item projections.
  *   - `CollisionFilter.filter(Item item, Item other)` (CollisionFilter.java:24): the Java `item` parameter is a plain reference that may be null.
  *
  * The Scala port currently declares `filter(item: Item[?], other: Nullable[Item[?]])` (CollisionFilter.scala:24) with a NON-Nullable first param, forcing World.scala:350 to launder the absent item
  * through a `getOrElse` fallback that manufactures a raw null Item (whitelisted only as a stop-gap). The faithful contract is a `Nullable[Item[?]]` first param, so an absent item can be passed
  * straight to the filter (as World.java:264 does) with no null laundering.
  *
  * This currently fails to COMPILE: `filter` is called with a `Nullable[Item[?]]` as its first argument, which does not conform to the current non-Nullable `Item[?]` parameter (there is no
  * `Nullable[A] => A` conversion — only the wrapping `A => Nullable[A]`). Once the parameter becomes `Nullable[Item[?]]`, this call type-checks and the assertions hold.
  */
class CollisionFilterNullableItemRedSuite extends munit.FunSuite {

  test("CollisionFilter.filter accepts an absent (Nullable) item directly (World.java:264 passes null)") {
    // The projected item may be absent; ISS-595 requires the filter's first param to carry that as a Nullable.
    val absentItem: Nullable[Item[?]]  = Nullable.Null
    val response:   Nullable[Response] = CollisionFilter.defaultFilter.filter(absentItem, Nullable.Null)
    assert(response.isDefined)
    assertEquals(response.get, Response.slide)
  }
}
