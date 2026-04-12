/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: value class for collision group bitmasks
 *   Audited: 2026-04-12
 */
package sge
package physics

/** Collision groups for filtering which colliders interact.
  *
  * Rapier2D (and Box2D) use bitmasks for collision filtering:
  *   - `memberships`: which groups this collider belongs to (each bit = one group)
  *   - `filter`: which groups this collider can interact with
  *
  * Two colliders A and B collide if: (A.memberships & B.filter) != 0 && (B.memberships & A.filter) != 0
  *
  * @param memberships
  *   Bitmask of groups this collider belongs to. Default 0xFFFFFFFF (all groups).
  * @param filter
  *   Bitmask of groups this collider can collide with. Default 0xFFFFFFFF (all groups).
  */
final case class CollisionGroups(memberships: Int = 0xffffffff, filter: Int = 0xffffffff)

object CollisionGroups {

  /** Default collision groups: belongs to all groups, collides with all groups. */
  val All: CollisionGroups = CollisionGroups(0xffffffff, 0xffffffff)

  /** No collision groups: belongs to no groups, collides with nothing. */
  val None: CollisionGroups = CollisionGroups(0, 0)

  /** Creates collision groups from a single group index (0-31).
    *
    * @param group
    *   Group index (0-31). The collider will belong to this group and only collide with this group.
    */
  def single(group: Int): CollisionGroups = {
    require(group >= 0 && group < 32, s"Group index must be 0-31, got $group")
    val mask = 1 << group
    CollisionGroups(mask, mask)
  }

  /** Creates collision groups from multiple group indices.
    *
    * @param groups
    *   Group indices (0-31). The collider will belong to and collide with all specified groups.
    */
  def of(groups: Int*): CollisionGroups = {
    val mask = groups.foldLeft(0) { (acc, g) =>
      require(g >= 0 && g < 32, s"Group index must be 0-31, got $g")
      acc | (1 << g)
    }
    CollisionGroups(mask, mask)
  }
}
