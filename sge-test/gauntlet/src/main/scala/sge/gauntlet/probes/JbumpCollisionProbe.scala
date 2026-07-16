/*
 * SGE Gauntlet — ext/jbump: deterministic World move/collide scenario.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.jbump.{ CollisionFilter, Item, World }
// jbump ships its own Nullable (sge.jbump.util.Nullable, see ISS in review E.1) — Item wants that one, not lowlevel.Nullable.
import sge.jbump.util.{ Nullable => JbumpNullable }

import scala.collection.mutable.ListBuffer

/** Adds two AABB items to a jbump World and moves one into the other: the default (slide) filter must stop the mover flush against the obstacle and report exactly one collision. */
object JbumpCollisionProbe extends FeatureProbe {

  override def id: String = "ext/jbump-collision"

  override def area: String = "ext/jbump"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val world    = new World[String](64f)
    val mover    = world.add(new Item[String](JbumpNullable("mover")), 0f, 0f, 10f, 10f)
    val obstacle = world.add(new Item[String](JbumpNullable("obstacle")), 20f, 0f, 10f, 10f)

    // Move into the obstacle: slide response must clamp x at 10 (obstacle.left - mover.width).
    val blocked = world.move(mover, 20f, 0f, CollisionFilter.defaultFilter)
    checks += Check.eq("blocked-goal-x", 10f, blocked.goalX)
    checks += Check.eq("blocked-goal-y", 0f, blocked.goalY)
    checks += Check.eq("blocked-collision-count", 1, blocked.projectedCollisions.size)

    // Free move: no collision, goal reached exactly.
    val free = world.move(mover, 0f, 30f, CollisionFilter.defaultFilter)
    checks += Check.eq("free-goal-y", 30f, free.goalY)
    checks += Check.eq("free-collision-count", 0, free.projectedCollisions.size)

    // Removal empties the world for the removed item.
    world.remove(obstacle)
    val afterRemove = world.move(mover, 20f, 30f, CollisionFilter.defaultFilter)
    checks += Check.eq("after-remove-goal-x", 20f, afterRemove.goalX)
    checks += Check.eq("after-remove-collision-count", 0, afterRemove.projectedCollisions.size)
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
