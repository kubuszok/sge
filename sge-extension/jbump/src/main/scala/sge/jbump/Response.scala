/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

/** Collision response trait with built-in implementations (slide, cross, touch, bounce). */
trait Response {

  def response(
      world: World[?],
      collision: Collision,
      x: Float,
      y: Float,
      w: Float,
      h: Float,
      goalX: Float,
      goalY: Float,
      filter: CollisionFilter,
      result: Response.Result
  ): Response.Result
}

object Response {

  class Result {
    var goalX: Float = 0f
    var goalY: Float = 0f
    val projectedCollisions: Collisions = Collisions()

    def set(goalX: Float, goalY: Float): Unit = {
      this.goalX = goalX
      this.goalY = goalY
    }
  }

  val slide: Response = new Response {
    override def response(
        world: World[?],
        collision: Collision,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        goalX: Float,
        goalY: Float,
        filter: CollisionFilter,
        result: Response.Result
    ): Response.Result = {
      val tch = collision.touch
      val move = collision.move
      var sx = tch.x
      var sy = tch.y
      if (move.x != 0 || move.y != 0) {
        if (collision.normal.x == 0) {
          sx = goalX
        } else {
          sy = goalY
        }
      }

      val newX = tch.x
      val newY = tch.y
      val newGoalX = sx
      val newGoalY = sy
      result.projectedCollisions.clear()
      world.project(collision.item, newX, newY, w, h, newGoalX, newGoalY, filter, result.projectedCollisions)
      result.set(newGoalX, newGoalY)
      result
    }
  }

  val touch: Response = new Response {
    override def response(
        world: World[?],
        collision: Collision,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        goalX: Float,
        goalY: Float,
        filter: CollisionFilter,
        result: Response.Result
    ): Response.Result = {
      result.projectedCollisions.clear()
      result.set(collision.touch.x, collision.touch.y)
      result
    }
  }

  val cross: Response = new Response {
    override def response(
        world: World[?],
        collision: Collision,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        goalX: Float,
        goalY: Float,
        filter: CollisionFilter,
        result: Response.Result
    ): Response.Result = {
      result.projectedCollisions.clear()
      world.project(collision.item, x, y, w, h, goalX, goalY, filter, result.projectedCollisions)
      result.set(goalX, goalY)
      result
    }
  }

  val bounce: Response = new Response {
    override def response(
        world: World[?],
        collision: Collision,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        goalX: Float,
        goalY: Float,
        filter: CollisionFilter,
        result: Response.Result
    ): Response.Result = {
      val tch = collision.touch
      val move = collision.move
      var bx = tch.x
      var by = tch.y
      if (move.x != 0 || move.y != 0) {
        var bnx = goalX - tch.x
        var bny = goalY - tch.y
        if (collision.normal.x == 0) {
          bny = -bny
        } else {
          bnx = -bnx
        }
        bx = tch.x + bnx
        by = tch.y + bny
      }

      val newX = tch.x
      val newY = tch.y
      val newGoalX = bx
      val newGoalY = by
      result.projectedCollisions.clear()
      world.project(collision.item, newX, newY, w, h, newGoalX, newGoalY, filter, result.projectedCollisions)
      result.set(newGoalX, newGoalY)
      result
    }
  }
}
