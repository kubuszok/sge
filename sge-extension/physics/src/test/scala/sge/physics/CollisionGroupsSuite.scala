/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package physics

import munit.FunSuite

class CollisionGroupsSuite extends FunSuite {

  test("CollisionGroups.All has all bits set") {
    assertEquals(CollisionGroups.All.memberships, 0xffffffff)
    assertEquals(CollisionGroups.All.filter, 0xffffffff)
  }

  test("CollisionGroups.None has no bits set") {
    assertEquals(CollisionGroups.None.memberships, 0)
    assertEquals(CollisionGroups.None.filter, 0)
  }

  test("CollisionGroups.single sets correct bit") {
    val g0 = CollisionGroups.single(0)
    assertEquals(g0.memberships, 1)
    assertEquals(g0.filter, 1)

    val g5 = CollisionGroups.single(5)
    assertEquals(g5.memberships, 32)
    assertEquals(g5.filter, 32)

    val g31 = CollisionGroups.single(31)
    assertEquals(g31.memberships, 0x80000000)
    assertEquals(g31.filter, 0x80000000)
  }

  test("CollisionGroups.single rejects out of range") {
    intercept[IllegalArgumentException] {
      CollisionGroups.single(-1)
    }
    intercept[IllegalArgumentException] {
      CollisionGroups.single(32)
    }
  }

  test("CollisionGroups.of combines multiple groups") {
    val groups = CollisionGroups.of(0, 2, 4)
    assertEquals(groups.memberships, 1 | 4 | 16) // bits 0, 2, 4
    assertEquals(groups.filter, 1 | 4 | 16)
  }

  test("CollisionGroups.of with empty creates zero mask") {
    val groups = CollisionGroups.of()
    assertEquals(groups.memberships, 0)
    assertEquals(groups.filter, 0)
  }

  test("CollisionGroups.of rejects invalid group index") {
    intercept[IllegalArgumentException] {
      CollisionGroups.of(0, 32, 1)
    }
  }

  test("default CollisionGroups collides with everything") {
    val default = CollisionGroups()
    assertEquals(default.memberships, 0xffffffff)
    assertEquals(default.filter, 0xffffffff)
  }

  test("collision detection formula: A and B collide") {
    // Two colliders collide if: (A.memberships & B.filter) != 0 && (B.memberships & A.filter) != 0
    val groupA = CollisionGroups(memberships = 0x0001, filter = 0x0002)
    val groupB = CollisionGroups(memberships = 0x0002, filter = 0x0001)

    // A belongs to group 0, filters group 1; B belongs to group 1, filters group 0
    val aCollidesWithB = (groupA.memberships & groupB.filter) != 0
    val bCollidesWithA = (groupB.memberships & groupA.filter) != 0
    assert(aCollidesWithB && bCollidesWithA, "A and B should collide")
  }

  test("collision detection formula: A and B don't collide (one-way filter)") {
    val groupA = CollisionGroups(memberships = 0x0001, filter = 0x0002)
    val groupB = CollisionGroups(memberships = 0x0002, filter = 0x0004) // B doesn't filter group 0

    val aCollidesWithB = (groupA.memberships & groupB.filter) != 0
    val bCollidesWithA = (groupB.memberships & groupA.filter) != 0
    assert(!aCollidesWithB, "A should not pass B's filter")
    assert(bCollidesWithA, "B should pass A's filter")
    // Overall: no collision since both conditions must be true
  }
}
