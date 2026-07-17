/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import scala.language.implicitConversions

// ISS-769 design contract: jbump must NOT ship a duplicate `sge.jbump.util.Nullable`. It should
// depend on the standalone `lls` library and use `lowlevel.Nullable` (the same Nullable the rest of
// SGE uses), so no conversion is forced at the extension<->core seam and only ONE Nullable freezes
// into the 0.1.0 API. Two facts make this a COMPILE-RED today:
//   (1) `lls` is not on the jbump module's classpath, so `lowlevel.Nullable` does not resolve
//       (the fix must add `libraryDependencies += "com.kubuszok" %% "lls"` to the jbump matrix — an
//        orchestrator/build-owned change, NOT an lls-source change: the current lls surface already
//        covers every jbump/util/Nullable member, see the reproducer's surface-delta table).
//   (2) even with lls present, jbump's public APIs are typed against `sge.jbump.util.Nullable`, a
//       DISTINCT opaque type, so a `lowlevel.Nullable[Item[?]]` is not assignable to them until the
//       collapse lands.
import lowlevel.Nullable

/** Red test for ISS-769: jbump APIs should accept and return `lowlevel.Nullable`, not a bespoke `sge.jbump.util.Nullable`. */
class JbumpLowlevelNullableContractRedSuite extends munit.FunSuite {

  test("World.project accepts lowlevel.Nullable[Item] and Collisions.get returns lowlevel.Nullable[Collision]") {
    val world = World[String](1f)
    val wall  = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val collisions = Collisions()
    // `Nullable.empty` is an lls-only member (jbump/util/Nullable has no `empty`); the projected item is absent.
    val absentItem: Nullable[Item[?]] = Nullable.empty
    val result:     Collisions        = world.project(absentItem, 2.5f, 0f, 1f, 1f, 2.5f, 0f, collisions)

    // Collisions.get must hand back an lls Nullable that the shared SGE code can consume without conversion.
    val col: Nullable[Collision] = result.get(0)
    assert(col.isDefined)
    assert(col.get.item.isEmpty) // lowlevel.Nullable.isEmpty on the stored (absent) item
    assert(col.get.other.get eq wall)
  }

  test("CollisionFilter is typed against lowlevel.Nullable") {
    val filter: CollisionFilter = new CollisionFilter {
      override def filter(item: Nullable[Item[?]], other: Nullable[Item[?]]): Nullable[Response] =
        Nullable(Response.slide)
    }
    // The default filter must also be an lls-Nullable-returning filter.
    assert(filter.filter(Nullable.empty, Nullable.empty).isDefined)
  }
}
