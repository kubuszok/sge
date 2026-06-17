/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package physics

import munit.FunSuite
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future

/** Behavioral coverage for the real Rapier2D WASM physics backend on Scala.js (ISS-676).
  *
  * [[PhysicsGravityJsRedSuite]] verifies a single gravity smoke; this suite ports a curated cross-section of the JVM [[PhysicsIntegrationSuite]] scenarios so each major subsystem (forces/impulses,
  * velocity, damping, mass, sensors, material round-trip + restitution, raycast, AABB/point query, revolute joint, contact events) is exercised against the actual Rapier WASM build with a strict
  * behavioral assertion.
  *
  * Every test is ASYNC: it loads the Rapier WASM module via [[sge.platform.PhysicsExtension.load]] and runs the scenario inside the returned `Future[Unit]`. No `Await`/blocking — that does not link
  * on Scala.js.
  *
  * Tolerances: the JS Rapier is a different version (v0.19) from the native/JVM build, so exact solver numbers may differ. Assertions therefore check DIRECTION, SIGN, ordering, presence/absence, and
  * loose magnitude bounds rather than exact floats — but each remains strong enough that a genuinely broken Rapier mapping fails it.
  */
class PhysicsBackendJsSuite extends FunSuite {

  /** Minimal [[Sge]] context — physics does not touch graphics/audio/input. Mirrors [[PhysicsGravityJsRedSuite]]. */
  private given Sge =
    Sge(StubApplication, new sge.noop.NoopGraphics(), new sge.noop.NoopAudio(), StubFiles, new sge.noop.NoopInput(), StubNet)

  /** Runs `scenario` against a fresh, real Rapier backend, closing the world afterwards. */
  private def withWorld(gravityX: Float, gravityY: Float)(scenario: PhysicsWorld => Unit): Future[Unit] =
    sge.platform.PhysicsExtension.load().map { _ =>
      val world = new PhysicsWorld(gravityX, gravityY)
      try scenario(world)
      finally world.close()
    }

  // ── 1. Body forces / impulses ───────────────────────────────────────────
  test("forces/impulses: a linear impulse to the right gives a dynamic body rightward velocity and motion") {
    withWorld(0f, 0f) { world =>
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      body.attachCollider(Shape.Circle(1f), density = 1f)

      val (x0, _) = body.position
      // Instantaneous rightward impulse at the center of mass.
      body.applyImpulse(5f, 0f)
      val (vx0, vy0) = body.linearVelocity
      // With zero gravity, impulse must set a strictly rightward velocity immediately.
      assert(vx0 > 0.1f, s"impulse must produce rightward velocity, got vx=$vx0")
      assert(scala.math.abs(vy0) < 0.1f, s"a purely horizontal impulse must not produce vertical velocity, got vy=$vy0")

      var i = 0
      while (i < 30) {
        world.step(1f / 60f)
        i += 1
      }
      val (x1, _) = body.position
      assert(x1 > x0 + 0.5f, s"body must travel rightward after impulse, x went $x0 -> $x1")
    }
  }

  // ── 2. Linear / angular velocity get/set ────────────────────────────────
  test("velocity: set linvel moves the body in that direction; set angvel changes the angle") {
    withWorld(0f, 0f) { world =>
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      body.attachCollider(Shape.Circle(1f), density = 1f)

      // Linear: set a known velocity, verify the getter round-trips and the body advances accordingly.
      body.linearVelocity = (2f, 0f)
      val (vx, vy) = body.linearVelocity
      assert(scala.math.abs(vx - 2f) < 0.001f, s"linvel x should round-trip to 2, got $vx")
      assert(scala.math.abs(vy) < 0.001f, s"linvel y should round-trip to 0, got $vy")
      val (x0, _) = body.position
      var i       = 0
      while (i < 30) {
        world.step(1f / 60f)
        i += 1
      }
      val (x1, _) = body.position
      // 2 m/s for ~0.5 s -> ~1 m; allow loose bounds for integrator specifics.
      assert(x1 - x0 > 0.6f, s"body moving at vx=2 for 0.5s should advance ~1m, got dx=${x1 - x0}")

      // Angular: set a known spin, verify the angle advances in the positive direction.
      val a0 = body.angle
      body.angularVelocity = 3f
      val w = body.angularVelocity
      assert(scala.math.abs(w - 3f) < 0.001f, s"angvel should round-trip to 3, got $w")
      var j = 0
      while (j < 30) {
        world.step(1f / 60f)
        j += 1
      }
      val a1 = body.angle
      assert(a1 - a0 > 0.5f, s"positive angular velocity must increase the angle, went $a0 -> $a1")
    }
  }

  // ── 3. Damping ──────────────────────────────────────────────────────────
  test("damping: a heavily-damped body travels strictly less than an undamped one from the same start") {
    withWorld(0f, 0f) { world =>
      def runWith(damping: Float): Float = {
        val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
        body.attachCollider(Shape.Circle(1f), density = 1f)
        body.linearDamping = damping
        body.linearVelocity = (5f, 0f)
        var i = 0
        while (i < 60) {
          world.step(1f / 60f)
          i += 1
        }
        body.position._1
      }
      val undampedX = runWith(0f)
      val dampedX   = runWith(5f)
      assert(undampedX > 0.5f, s"undamped body should travel a meaningful distance, got $undampedX")
      assert(dampedX < undampedX, s"damped body must travel less than undamped: damped=$dampedX undamped=$undampedX")
    }
  }

  // ── 4. Body mass ────────────────────────────────────────────────────────
  test("mass: bodyGetMass reflects density x area for a 1x1 box (half-extents 0.5) at density 4") {
    withWorld(0f, 0f) { world =>
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      // Box(0.5, 0.5) is a 1x1 area; mass = density * area = 4 * 1 = ~4.
      body.attachCollider(Shape.Box(0.5f, 0.5f), density = 4f)
      val mass = body.mass
      assert(mass > 0f, s"mass must be positive for a dense collider, got $mass")
      // Loose bound around the analytic value 4.0 (tolerant of solver mass-lumping differences).
      assert(mass > 2f && mass < 8f, s"mass for 1x1 box at density 4 should be ~4, got $mass")
    }
  }

  // ── 5. Collider sensor ──────────────────────────────────────────────────
  test("sensor: a sensor collider does NOT push a falling body but IS reported by an overlap query") {
    withWorld(0f, -10f) { world =>
      // Static sensor "floor" at y=0.
      val floor    = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val floorCol = floor.attachCollider(Shape.Box(5f, 0.5f))
      floorCol.isSensor = true
      assert(floorCol.isSensor, "sensor flag must round-trip true")

      // Dynamic ball falls from above; a sensor must NOT stop it.
      val ball = world.createBody(BodyType.Dynamic, x = 0f, y = 5f)
      ball.attachCollider(Shape.Circle(0.5f))

      var i = 0
      while (i < 120) {
        world.step(1f / 60f)
        i += 1
      }
      val (_, y) = ball.position
      assert(y < -1f, s"ball must pass THROUGH a sensor floor (no physical response), got y=$y")

      // But the sensor must still report the overlap via a shape-intersection query at the floor.
      val overlaps = world.intersectShape(Shape.Box(5f, 0.5f), posX = 0f, posY = 0f)
      assert(overlaps.contains(floorCol.handle), s"overlap query must report the sensor collider, got $overlaps")
    }
  }

  // ── 6. Collider properties + restitution rebound ────────────────────────
  test("material: friction/restitution round-trip; a bouncy body rebounds with upward velocity") {
    withWorld(0f, -10f) { world =>
      // Round-trip friction and restitution on a probe collider.
      val probeBody = world.createBody(BodyType.Dynamic, x = 20f, y = 20f)
      val probe     = probeBody.attachCollider(Shape.Circle(0.5f), friction = 0.7f, restitution = 0.9f)
      assert(scala.math.abs(probe.friction - 0.7f) < 0.001f, s"friction should round-trip to 0.7, got ${probe.friction}")
      assert(scala.math.abs(probe.restitution - 0.9f) < 0.001f, s"restitution should round-trip to 0.9, got ${probe.restitution}")

      // Bouncy ball drops onto a bouncy floor; after impact it must move back upward.
      val floor = world.createBody(BodyType.Static, x = 0f, y = 0f)
      floor.attachCollider(Shape.Box(5f, 0.5f), restitution = 1f)
      val ball = world.createBody(BodyType.Dynamic, x = 0f, y = 5f)
      ball.attachCollider(Shape.Circle(0.5f), restitution = 1f)

      var reboundedUp = false
      var i           = 0
      // Step long enough to fall, contact, and rebound. Detect any frame with upward velocity after the fall began.
      while (i < 240) {
        world.step(1f / 60f)
        val (_, vy) = ball.linearVelocity
        if (i > 10 && vy > 1f) reboundedUp = true
        i += 1
      }
      assert(reboundedUp, "a restitution=1 ball must rebound upward (positive vy) after hitting a bouncy floor")
    }
  }

  // ── 7. Raycast ──────────────────────────────────────────────────────────
  test("raycast: a ray hits a known collider at the expected distance and reports its body handle") {
    withWorld(0f, 0f) { world =>
      // Target box centered at x=5, spanning x in [4,6]; ray from origin going +X should hit near x=4.
      val target = world.createBody(BodyType.Static, x = 5f, y = 0f)
      target.attachCollider(Shape.Box(1f, 1f))
      world.step(0f) // refresh the query pipeline

      val hitN = world.rayCast(0f, 0f, 1f, 0f, 20f)
      assert(!hitN.isEmpty, "ray pointing at the box must register a hit")
      val hit = hitN.get
      // Near face of a box centered at 5 with half-width 1 is x=4.
      assert(hit.hitX > 3.5f && hit.hitX < 4.5f, s"hit X should be near the box near-face (~4), got ${hit.hitX}")
      assert(hit.timeOfImpact > 3.5f && hit.timeOfImpact < 4.5f, s"time-of-impact should be ~4, got ${hit.timeOfImpact}")
      assert(hit.bodyHandle == target.handle, s"ray must report the target body handle ${target.handle}, got ${hit.bodyHandle}")

      // A ray pointing away from the box must miss.
      val miss = world.rayCast(0f, 0f, -1f, 0f, 20f)
      assert(miss.isEmpty, "ray pointing away from the only collider must miss")
    }
  }

  // ── 8. AABB / point query ───────────────────────────────────────────────
  test("query: AABB and point queries find colliders at known locations and nothing in empty space") {
    withWorld(0f, 0f) { world =>
      val b1 = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val b2 = world.createBody(BodyType.Static, x = 5f, y = 5f)
      val b3 = world.createBody(BodyType.Static, x = 10f, y = 10f)
      b1.attachCollider(Shape.Circle(1f))
      b2.attachCollider(Shape.Circle(1f))
      b3.attachCollider(Shape.Circle(1f))
      world.step(0f)

      val twoOf = world.queryAABB(-2f, -2f, 7f, 7f)
      assertEquals(twoOf.size, 2, s"AABB [-2,-2]..[7,7] should contain b1 and b2, got ${twoOf.size}")
      val allOf = world.queryAABB(-2f, -2f, 12f, 12f)
      assertEquals(allOf.size, 3, s"AABB covering all three should find 3, got ${allOf.size}")
      val none = world.queryAABB(100f, 100f, 110f, 110f)
      assertEquals(none.size, 0, s"empty region should find nothing, got ${none.size}")

      // Point query: inside b2's circle finds b2; far away finds nothing.
      val atB2 = world.queryPoint(5f, 5f)
      assert(atB2.contains(b2.handle), s"point at (5,5) must find b2 ${b2.handle}, got $atB2")
      val empty = world.queryPoint(50f, 50f)
      assert(empty.isEmpty, s"point in empty space must find nothing, got $empty")
    }
  }

  // ── 8b. Collider AABB ───────────────────────────────────────────────────
  test("aabb: colliderGetAabb returns the tight world-space box for a translated box and circle collider") {
    withWorld(0f, 0f) { world =>
      // A 1x1 box (half-extents 0.5) centered at (3, 7) with no rotation must report
      // exactly [minX=2.5, minY=6.5, maxX=3.5, maxY=7.5]. The prior implementation called a
      // non-existent `shape.computeAABB` and threw a TypeError here for every caller.
      val boxBody                      = world.createBody(BodyType.Static, x = 3f, y = 7f)
      val boxCol                       = boxBody.attachCollider(Shape.Box(0.5f, 0.5f))
      val (bMinX, bMinY, bMaxX, bMaxY) = boxCol.aabb
      val tol                          = 1e-3f
      assert(scala.math.abs(bMinX - 2.5f) < tol, s"box AABB minX should be 2.5, got $bMinX")
      assert(scala.math.abs(bMinY - 6.5f) < tol, s"box AABB minY should be 6.5, got $bMinY")
      assert(scala.math.abs(bMaxX - 3.5f) < tol, s"box AABB maxX should be 3.5, got $bMaxX")
      assert(scala.math.abs(bMaxY - 7.5f) < tol, s"box AABB maxY should be 7.5, got $bMaxY")

      // A radius-1 circle centered at (-2, 4) spans [-3, 3]..[-1, 5].
      val circBody                     = world.createBody(BodyType.Static, x = -2f, y = 4f)
      val circCol                      = circBody.attachCollider(Shape.Circle(1f))
      val (cMinX, cMinY, cMaxX, cMaxY) = circCol.aabb
      assert(scala.math.abs(cMinX - -3f) < tol, s"circle AABB minX should be -3, got $cMinX")
      assert(scala.math.abs(cMinY - 3f) < tol, s"circle AABB minY should be 3, got $cMinY")
      assert(scala.math.abs(cMaxX - -1f) < tol, s"circle AABB maxX should be -1, got $cMaxX")
      assert(scala.math.abs(cMaxY - 5f) < tol, s"circle AABB maxY should be 5, got $cMaxY")
    }
  }

  // ── 8c. Per-shape AABB + shapeType coverage ─────────────────────────────
  // One test per factory-produced shape. Each EXECUTES that shape's `colliderGetAabb` branch (a TypeError in
  // any branch — e.g. reading Triangle `a/b/c` off a Polyline — fails the test) and asserts the world-space AABB
  // computed by hand for the known geometry, plus the SGE shapeType code (exercising `mapShapeType` per shape).
  // SGE shapeType contract (PhysicsOps.colliderGetShapeType): 0=ball,1=cuboid,2=capsule,3=segment,4=triangle,
  // 5=trimesh,6=polyline,7=heightfield,9=convex_polygon.
  private val aabbTol = 1e-3f

  private def assertAabb(actual: (Float, Float, Float, Float), eMinX: Float, eMinY: Float, eMaxX: Float, eMaxY: Float, label: String): Unit = {
    val (minX, minY, maxX, maxY) = actual
    assert(scala.math.abs(minX - eMinX) < aabbTol, s"$label AABB minX should be $eMinX, got $minX")
    assert(scala.math.abs(minY - eMinY) < aabbTol, s"$label AABB minY should be $eMinY, got $minY")
    assert(scala.math.abs(maxX - eMaxX) < aabbTol, s"$label AABB maxX should be $eMaxX, got $maxX")
    assert(scala.math.abs(maxY - eMaxY) < aabbTol, s"$label AABB maxY should be $eMaxY, got $maxY")
  }

  test("shape coverage: circle collider AABB + shapeType (ball=0)") {
    withWorld(0f, 0f) { world =>
      // radius-1 circle centered at (-2, 4) spans [-3,3]..[-1,5].
      val body = world.createBody(BodyType.Static, x = -2f, y = 4f)
      val col  = body.attachCollider(Shape.Circle(1f))
      assertAabb(col.aabb, -3f, 3f, -1f, 5f, "circle")
      assertEquals(col.shapeType, 0, s"circle shapeType should be 0 (ball), got ${col.shapeType}")
    }
  }

  test("shape coverage: box collider AABB + shapeType (cuboid=1)") {
    withWorld(0f, 0f) { world =>
      // 1x1 box (half-extents 0.5) centered at (3, 7) spans [2.5,6.5]..[3.5,7.5].
      val body = world.createBody(BodyType.Static, x = 3f, y = 7f)
      val col  = body.attachCollider(Shape.Box(0.5f, 0.5f))
      assertAabb(col.aabb, 2.5f, 6.5f, 3.5f, 7.5f, "box")
      assertEquals(col.shapeType, 1, s"box shapeType should be 1 (cuboid), got ${col.shapeType}")
    }
  }

  test("shape coverage: capsule collider AABB + shapeType (capsule=2)") {
    withWorld(0f, 0f) { world =>
      // Capsule halfHeight=1, radius=0.5 at origin: segment tips at (0,±1) padded by 0.5 → [-0.5,-1.5]..[0.5,1.5].
      val body = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val col  = body.attachCollider(Shape.Capsule(1f, 0.5f))
      assertAabb(col.aabb, -0.5f, -1.5f, 0.5f, 1.5f, "capsule")
      assertEquals(col.shapeType, 2, s"capsule shapeType should be 2 (capsule), got ${col.shapeType}")
    }
  }

  test("shape coverage: segment collider AABB + shapeType (segment=3)") {
    withWorld(0f, 0f) { world =>
      // Segment from (-1,-2) to (3,4), body at origin: AABB encloses both endpoints → [-1,-2]..[3,4].
      val body = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val col  = body.attachCollider(Shape.Segment(-1f, -2f, 3f, 4f))
      assertAabb(col.aabb, -1f, -2f, 3f, 4f, "segment")
      assertEquals(col.shapeType, 3, s"segment shapeType should be 3 (segment), got ${col.shapeType}")
    }
  }

  test("shape coverage: polygon collider AABB + shapeType (convex_polygon=9)") {
    withWorld(0f, 0f) { world =>
      // Convex-hull of a unit square, body at (5, 0): corners ±1 → world [4,-1]..[6,1].
      val body = world.createBody(BodyType.Static, x = 5f, y = 0f)
      val col  = body.attachCollider(Shape.Polygon(Array(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f)))
      assertAabb(col.aabb, 4f, -1f, 6f, 1f, "polygon")
      assertEquals(col.shapeType, 9, s"polygon shapeType should be 9 (convex_polygon), got ${col.shapeType}")
    }
  }

  test("shape coverage: polyline collider AABB + shapeType (polyline=6)") {
    withWorld(0f, 0f) { world =>
      // Polyline [(0,0),(2,0),(2,2)], body at (1, 1): vertex AABB [0,0]..[2,2] → world [1,1]..[3,3].
      // This is the branch the prior code mis-handled as a Triangle (reading shape.a/b/c → TypeError).
      val body = world.createBody(BodyType.Static, x = 1f, y = 1f)
      val col  = body.attachCollider(Shape.Polyline(Array(0f, 0f, 2f, 0f, 2f, 2f)))
      assertAabb(col.aabb, 1f, 1f, 3f, 3f, "polyline")
      assertEquals(col.shapeType, 6, s"polyline shapeType should be 6 (polyline), got ${col.shapeType}")
    }
  }

  test("shape coverage: trimesh collider AABB + shapeType (trimesh=5)") {
    withWorld(0f, 0f) { world =>
      // TriMesh of a single triangle (0,0),(4,0),(0,3), body at origin: vertex AABB [0,0]..[4,3].
      val body = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val col  = body.attachCollider(Shape.TriMesh(Array(0f, 0f, 4f, 0f, 0f, 3f), Array(0, 1, 2)))
      assertAabb(col.aabb, 0f, 0f, 4f, 3f, "trimesh")
      assertEquals(col.shapeType, 5, s"trimesh shapeType should be 5 (trimesh), got ${col.shapeType}")
    }
  }

  test("shape coverage: heightfield collider AABB + shapeType (heightfield=7)") {
    withWorld(0f, 0f) { world =>
      // Heightfield heights [0,1,0], scaleX=4, scaleY=2, body at origin: samples at x∈{-2,0,2}, y∈{0,2,0}
      // → AABB [-2,0]..[2,2].
      val body = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val col  = body.attachCollider(Shape.Heightfield(Array(0f, 1f, 0f), 4f, 2f))
      assertAabb(col.aabb, -2f, 0f, 2f, 2f, "heightfield")
      assertEquals(col.shapeType, 7, s"heightfield shapeType should be 7 (heightfield), got ${col.shapeType}")
    }
  }

  // ── 9. Revolute joint ───────────────────────────────────────────────────
  test("joint: a revolute joint keeps the dynamic body near its anchor while gravity pulls on it") {
    withWorld(0f, -10f) { world =>
      // Static pivot at origin; dynamic body 1m to the right, hinged at the origin.
      val pivot = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val arm   = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      pivot.attachCollider(Shape.Circle(0.1f))
      arm.attachCollider(Shape.Circle(0.1f))

      val joint = world.createJoint(JointDef.Revolute(pivot, arm, 0f, 0f))
      assert(joint.isInstanceOf[RevoluteJoint], "should create a RevoluteJoint")

      var i = 0
      while (i < 120) {
        world.step(1f / 60f)
        i += 1
      }
      // The arm swings under gravity but the hinge keeps it at radius ~1 from the pivot.
      val (ax, ay) = arm.position
      val dist     = scala.math.sqrt((ax * ax + ay * ay).toDouble).toFloat
      assert(dist > 0.7f && dist < 1.3f, s"revolute joint must hold the arm ~1m from the pivot, got dist=$dist")
      // And gravity must actually have pulled it down (constraint isn't pinning it in place).
      assert(ay < -0.1f, s"under gravity the hinged arm should swing downward, got y=$ay")
    }
  }

  // ── 9b. Spring joint ────────────────────────────────────────────────────
  // Spring/Rope/Motor joints are backed by base Rapier2D ImpulseJoint subclasses (Spring/Rope/Fixed), which do NOT
  // expose the UnitImpulseJoint motor/limit mutators. Each of these tests EXECUTES the previously-throwing setter, so a
  // `TypeError: ... is not a function` would fail the test outright; they also assert the high-level getter round-trips
  // the set value and a step does not crash.
  test("joint: a spring joint pulls two separated bodies toward each other and the setters round-trip") {
    withWorld(0f, 0f) { world =>
      // Two dynamic bodies 4m apart on X; a spring with rest length 1 should pull them together over time.
      val a = world.createBody(BodyType.Dynamic, x = -2f, y = 0f)
      val b = world.createBody(BodyType.Dynamic, x = 2f, y = 0f)
      a.attachCollider(Shape.Circle(0.1f), density = 1f)
      b.attachCollider(Shape.Circle(0.1f), density = 1f)

      val joint = world.createJoint(JointDef.Spring(a, b, restLength = 1f, stiffness = 50f, damping = 2f))
      assert(joint.isInstanceOf[SpringJoint], "should create a SpringJoint")
      val spring = joint.asInstanceOf[SpringJoint]

      val (ax0, _) = a.position
      val (bx0, _) = b.position
      val dist0    = scala.math.abs(bx0 - ax0)

      // EXECUTE the previously-crashing setters (configureMotorPosition on a base SpringImpulseJoint -> TypeError).
      spring.restLength = 1f
      assert(scala.math.abs(spring.restLength - 1f) < 0.001f, s"restLength should round-trip to 1, got ${spring.restLength}")
      spring.setParams(stiffness = 80f, damping = 3f)

      var i = 0
      while (i < 180) {
        world.step(1f / 60f)
        i += 1
      }
      val (ax1, _) = a.position
      val (bx1, _) = b.position
      val dist1    = scala.math.abs(bx1 - ax1)
      // The spring (rest length 1) must contract the 4m separation toward the rest length.
      assert(dist1 < dist0 - 0.5f, s"spring should pull the bodies closer (rest length 1), dist went $dist0 -> $dist1")
    }
  }

  // ── 9c. Rope joint ──────────────────────────────────────────────────────
  test("joint: a rope joint keeps a falling body within its max distance and the setter round-trips") {
    withWorld(0f, -10f) { world =>
      // Static anchor at the origin; a dynamic body hangs from a rope of max distance 2.
      val anchor  = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val hanging = world.createBody(BodyType.Dynamic, x = 0f, y = -0.5f)
      anchor.attachCollider(Shape.Circle(0.1f))
      hanging.attachCollider(Shape.Circle(0.1f), density = 1f)

      val joint = world.createJoint(JointDef.Rope(anchor, hanging, maxDistance = 2f))
      assert(joint.isInstanceOf[RopeJoint], "should create a RopeJoint")
      val rope = joint.asInstanceOf[RopeJoint]

      // EXECUTE the previously-crashing setter (setLimits on a base RopeImpulseJoint -> TypeError).
      rope.maxDistance = 2f
      assert(scala.math.abs(rope.maxDistance - 2f) < 0.001f, s"maxDistance should round-trip to 2, got ${rope.maxDistance}")

      var i = 0
      while (i < 240) {
        world.step(1f / 60f)
        i += 1
      }
      // Gravity pulls the body down, but the rope must keep it within ~maxDistance of the anchor.
      val (hx, hy) = hanging.position
      val dist     = scala.math.sqrt((hx * hx + hy * hy).toDouble).toFloat
      assert(hy < -0.5f, s"under gravity the hanging body should drop, got y=$hy")
      assert(dist < 2.5f, s"rope (max distance 2) must keep the body within ~2m of the anchor, got dist=$dist")
    }
  }

  // ── 9d. Motor joint ─────────────────────────────────────────────────────
  test("joint: a motor joint's setters execute and round-trip without throwing") {
    withWorld(0f, 0f) { world =>
      val a = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      val b = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      a.attachCollider(Shape.Circle(0.1f), density = 1f)
      b.attachCollider(Shape.Circle(0.1f), density = 1f)

      val joint = world.createJoint(JointDef.Motor(a, b))
      assert(joint.isInstanceOf[MotorJoint], "should create a MotorJoint")
      val motor = joint.asInstanceOf[MotorJoint]

      // EXECUTE every previously-crashing motor setter (applyMotorTarget -> configureMotorPosition on a base
      // FixedImpulseJoint -> TypeError). The linear offset is applied via setAnchor2; the max-force/torque/correction
      // factor are accepted and stored. All getters must round-trip the set values.
      motor.linearOffset = (0.5f, 0f)
      val (ox, oy) = motor.linearOffset
      assert(scala.math.abs(ox - 0.5f) < 0.001f, s"linearOffset x should round-trip to 0.5, got $ox")
      assert(scala.math.abs(oy) < 0.001f, s"linearOffset y should round-trip to 0, got $oy")

      motor.maxForce = 100f
      assert(scala.math.abs(motor.maxForce - 100f) < 0.001f, s"maxForce should round-trip to 100, got ${motor.maxForce}")
      motor.maxTorque = 50f
      assert(scala.math.abs(motor.maxTorque - 50f) < 0.001f, s"maxTorque should round-trip to 50, got ${motor.maxTorque}")
      motor.correctionFactor = 0.3f
      assert(
        scala.math.abs(motor.correctionFactor - 0.3f) < 0.001f,
        s"correctionFactor should round-trip to 0.3, got ${motor.correctionFactor}"
      )

      // Stepping after configuring the motor must not throw.
      var i = 0
      while (i < 60) {
        world.step(1f / 60f)
        i += 1
      }
      assert(true, "stepping a configured motor joint must not throw")
    }
  }

  // ── 10. Contact events ──────────────────────────────────────────────────
  test("contacts: a falling body landing on a floor produces a contact-start event for the right colliders") {
    withWorld(0f, -10f) { world =>
      val floor    = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val floorCol = floor.attachCollider(Shape.Box(5f, 0.5f))
      val ball     = world.createBody(BodyType.Dynamic, x = 0f, y = 3f)
      val ballCol  = ball.attachCollider(Shape.Circle(0.5f))

      var sawContact = false
      var i          = 0
      while (i < 240) {
        world.step(1f / 60f)
        val events = world.pollContactStartEvents()
        events.foreach { case (c1, c2) =>
          val pair = Set(c1, c2)
          if (pair == Set(floorCol.handle, ballCol.handle)) sawContact = true
        }
        i += 1
      }
      assert(sawContact, "a contact-start event between the ball and floor colliders must be reported during the fall")
    }
  }

  // ─── Minimal stub subsystems (mirror PhysicsGravityJsRedSuite) ───────────

  private object StubApplication extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.WebGL
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
    def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object StubFiles extends Files {
    def getFileHandle(path: String, fileType: files.FileType): files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                           files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                           files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                   String           = ""
    def isExternalStorageAvailable:                            Boolean          = false
    def localStoragePath:                                      String           = ""
    def isLocalStorageAvailable:                               Boolean          = false
  }

  private object StubNet extends Net {
    import Net._
    def httpClient:                                                                                     net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                              Boolean           = false
  }
}
