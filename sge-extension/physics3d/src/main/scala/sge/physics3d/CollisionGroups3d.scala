/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: value class for collision group bitmasks
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: All,CollisionGroups3d,None,mask,single
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package physics3d

/** Collision groups for filtering which 3D colliders interact.
  *
  * @param memberships
  *   Bitmask of groups this collider belongs to. Default 0xFFFFFFFF (all groups).
  * @param filter
  *   Bitmask of groups this collider can collide with. Default 0xFFFFFFFF (all groups).
  */
final case class CollisionGroups3d(memberships: Int = 0xffffffff, filter: Int = 0xffffffff)

object CollisionGroups3d {
  val All:  CollisionGroups3d = CollisionGroups3d(0xffffffff, 0xffffffff)
  val None: CollisionGroups3d = CollisionGroups3d(0, 0)

  def single(group: Int): CollisionGroups3d = {
    require(group >= 0 && group < 32, s"Group index must be 0-31, got $group")
    val mask = 1 << group
    CollisionGroups3d(mask, mask)
  }
}
