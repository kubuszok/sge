package sge
package ai
package steer
package behaviors

import sge.ai.DefaultTimepiece
import sge.ai.Timepiece
import sge.ai.steer.{ SimpleSteerable, SimpleLocation }
import sge.math.Vector2
import sge.utils.Nullable

class SteeringBehaviorsSuite extends munit.FunSuite {

  private val Eps = 1e-3f

  // ── Wander ───────────────────────────────────────────────────────────

  test("Wander: produces non-zero steering") {
    given tp: Timepiece = {
      val t = new DefaultTimepiece()
      t.update(1.0f) // set time to 1 so delta > 0
      t
    }

    val owner = new SimpleSteerable(Vector2(0, 0))
    val wander = new Wander[Vector2](owner)
    wander.wanderOffset = 5f
    wander.wanderRadius = 2f
    wander.wanderRate = 0.5f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    wander.calculateSteering(output)

    assert(!output.isZero, "Wander should produce non-zero steering")
  }

  test("Wander: steering magnitude is bounded by maxLinearAcceleration") {
    given tp: Timepiece = {
      val t = new DefaultTimepiece()
      t.update(0.5f)
      t
    }

    val owner = new SimpleSteerable(Vector2(0, 0), _maxLinearAcceleration = 50f)
    val wander = new Wander[Vector2](owner)
    wander.wanderOffset = 5f
    wander.wanderRadius = 2f
    wander.wanderRate = 1.0f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    wander.calculateSteering(output)

    val mag = output.linear.length
    // Wander applies full linear acceleration, so magnitude should equal maxLinearAcceleration
    assertEqualsFloat(mag, 50f, 0.1f)
  }

  test("Wander: wander center is offset from owner position") {
    given tp: Timepiece = {
      val t = new DefaultTimepiece()
      t.update(1.0f)
      t
    }

    val owner = new SimpleSteerable(Vector2(0, 0))
    val wander = new Wander[Vector2](owner)
    wander.wanderOffset = 10f
    wander.wanderRadius = 2f
    wander.wanderRate = 0.5f
    wander.wanderOrientation = 0f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    wander.calculateSteering(output)

    // The wander center should be approximately wanderOffset distance from the owner
    val center = wander.getWanderCenter
    val dist = center.length
    // Center is at owner.position + direction * wanderOffset, owner at (0,0) facing (1,0)
    assertEqualsFloat(dist, 10f, 0.1f)
  }

  test("Wander: getInternalTargetPosition returns current target") {
    given tp: Timepiece = {
      val t = new DefaultTimepiece()
      t.update(1.0f)
      t
    }

    val owner = new SimpleSteerable(Vector2(0, 0))
    val wander = new Wander[Vector2](owner)
    wander.wanderOffset = 5f
    wander.wanderRadius = 2f
    wander.wanderRate = 0.5f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    wander.calculateSteering(output)

    val targetPos = wander.getInternalTargetPosition
    // Target should be roughly wanderOffset away from origin, within wanderRadius
    val dist = targetPos.length
    assert(dist > 0, "Internal target position should be non-zero")
  }

  test("Wander: disabled wander returns zero") {
    given tp: Timepiece = {
      val t = new DefaultTimepiece()
      t.update(1.0f)
      t
    }

    val owner = new SimpleSteerable(Vector2(0, 0))
    val wander = new Wander[Vector2](owner)
    wander.wanderOffset = 5f
    wander.wanderRadius = 2f
    wander.wanderRate = 0.5f
    wander.enabled = false

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    wander.calculateSteering(output)

    assert(output.isZero, "Disabled wander should return zero acceleration")
  }

  // ── Hide ─────────────────────────────────────────────────────────────

  test("Hide: returns zero when no obstacles nearby") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(10, 0)) // the hunter

    val emptyProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = 0
    }

    val hide = new Hide[Vector2](owner, Nullable(target), Nullable(emptyProximity))
    hide.arrivalTolerance = 0.1f
    hide.decelerationRadius = 5f
    hide.distanceFromBoundary = 2f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    hide.calculateSteering(output)

    assert(output.isZero, "Hide should return zero when no obstacles are found")
  }

  test("Hide: steers toward hiding spot behind obstacle") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(10, 0)) // the hunter at (10,0)
    val obstacle = new SimpleSteerable(Vector2(5, 0), _maxLinearAcceleration = 0f) // obstacle between owner and hunter

    // Proximity that always returns the single obstacle
    val singleObstacleProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(obstacle)
        1
      }
    }

    val hide = new Hide[Vector2](owner, Nullable(target), Nullable(singleObstacleProximity))
    hide.arrivalTolerance = 0.1f
    hide.decelerationRadius = 5f
    hide.distanceFromBoundary = 2f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    hide.calculateSteering(output)

    assert(!output.isZero, "Hide should produce non-zero steering when obstacle is available")
    // The hiding spot is on the opposite side of the obstacle from the hunter.
    // Hunter at (10,0), obstacle at (5,0) with radius 1, distFromBoundary=2.
    // Hiding spot should be at ~(5 - 3, 0) = (2, 0), i.e. left of obstacle.
    // Owner at (0,0) should steer toward ~(2,0), so positive x.
    assert(output.linear.x > 0 || output.linear.y != 0,
      "Should steer toward hiding spot behind obstacle")
  }

  test("Hide: chooses closest hiding spot among multiple obstacles") {
    val owner = new SimpleSteerable(Vector2(0, 0))
    val target = new SimpleLocation(Vector2(20, 0)) // the hunter far away

    val nearObstacle = new SimpleSteerable(Vector2(3, 0))
    val farObstacle = new SimpleSteerable(Vector2(15, 0))

    val multiObstacleProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(nearObstacle)
        callback.reportNeighbor(farObstacle)
        2
      }
    }

    val hide = new Hide[Vector2](owner, Nullable(target), Nullable(multiObstacleProximity))
    hide.arrivalTolerance = 0.1f
    hide.decelerationRadius = 5f
    hide.distanceFromBoundary = 1f

    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    hide.calculateSteering(output)

    assert(!output.isZero, "Hide should produce non-zero steering with multiple obstacles")
  }

  // ── CollisionAvoidance ──────────────────────────────────────────────

  test("CollisionAvoidance: no neighbors returns zero steering") {
    val owner = new SimpleSteerable(Vector2(0, 0), Vector2(1, 0))

    val emptyProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = 0
    }

    val ca = new CollisionAvoidance[Vector2](owner, emptyProximity)
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    ca.calculateSteering(output)

    assert(output.isZero, "No neighbors should produce zero steering")
  }

  test("CollisionAvoidance: avoids approaching neighbor") {
    // Owner moving right, neighbor moving left - head-on collision course
    val owner = new SimpleSteerable(Vector2(0, 0), Vector2(5, 0))
    val neighbor = new SimpleSteerable(Vector2(10, 0), Vector2(-5, 0))

    val approachingProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(neighbor)
        1
      }
    }

    val ca = new CollisionAvoidance[Vector2](owner, approachingProximity)
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    ca.calculateSteering(output)

    assert(!output.isZero, "Should produce steering when neighbor is approaching")
  }

  test("CollisionAvoidance: no steering when neighbor moves away") {
    // Owner and neighbor both moving in same direction, neighbor ahead
    val owner = new SimpleSteerable(Vector2(0, 0), Vector2(1, 0))
    val neighbor = new SimpleSteerable(Vector2(10, 0), Vector2(5, 0)) // moving faster away

    val departingProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(neighbor)
        1
      }
    }

    val ca = new CollisionAvoidance[Vector2](owner, departingProximity)
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    ca.calculateSteering(output)

    // When relative velocity shows agents diverging, timeToCollision is negative, so no avoidance needed
    assert(output.isZero, "Should produce zero steering when neighbor is moving away")
  }

  test("CollisionAvoidance: no angular acceleration") {
    val owner = new SimpleSteerable(Vector2(0, 0), Vector2(5, 0))
    val neighbor = new SimpleSteerable(Vector2(10, 0), Vector2(-5, 0))

    val proximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(neighbor)
        1
      }
    }

    val ca = new CollisionAvoidance[Vector2](owner, proximity)
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    ca.calculateSteering(output)

    assertEqualsFloat(output.angular, 0f, Eps)
  }

  test("CollisionAvoidance: disabled returns zero") {
    val owner = new SimpleSteerable(Vector2(0, 0), Vector2(5, 0))
    val neighbor = new SimpleSteerable(Vector2(10, 0), Vector2(-5, 0))

    val proximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(neighbor)
        1
      }
    }

    val ca = new CollisionAvoidance[Vector2](owner, proximity)
    ca.enabled = false
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    ca.calculateSteering(output)

    assert(output.isZero, "Disabled collision avoidance should return zero")
  }

  test("CollisionAvoidance: selects most imminent collision among multiple neighbors") {
    val owner = new SimpleSteerable(Vector2(0, 0), Vector2(5, 0))
    val farNeighbor = new SimpleSteerable(Vector2(20, 0), Vector2(-5, 0)) // far away
    val closeNeighbor = new SimpleSteerable(Vector2(5, 0), Vector2(-5, 0)) // very close

    val multiProximity = new Proximity[Vector2] {
      private var _owner: Steerable[Vector2] = owner
      override def owner: Steerable[Vector2] = _owner
      override def owner_=(owner: Steerable[Vector2]): Unit = { _owner = owner }
      override def findNeighbors(callback: Proximity.ProximityCallback[Vector2]): Int = {
        callback.reportNeighbor(farNeighbor)
        callback.reportNeighbor(closeNeighbor)
        2
      }
    }

    val ca = new CollisionAvoidance[Vector2](owner, multiProximity)
    val output = new SteeringAcceleration[Vector2](Vector2(0, 0))
    ca.calculateSteering(output)

    assert(!output.isZero, "Should produce avoidance steering for closest collision")
  }
}
