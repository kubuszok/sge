/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package physics3d

import munit.FunSuite
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future

/** Behavioral coverage for the real Rapier3D WASM physics backend on Scala.js (ISS-677).
  *
  * This is the 3D mirror of the proven 2D [[sge.physics.PhysicsBackendJsSuite]]. It front-loads a comprehensive cross-section of the JVM [[PhysicsIntegration3dSuite]] scenarios so each major
  * subsystem (gravity, forces/impulses, linear/angular velocity, mass, sensors, material round-trip + restitution rebound, raycast, AABB/point query, a 3D joint, contact events) AND every collider
  * shape's `colliderGetAabb` + `colliderGetShapeType` branch is exercised against the actual Rapier3D WASM build with a strict behavioral assertion.
  *
  * Every test is ASYNC: it loads the Rapier3D WASM module via [[sge.platform.PhysicsExtension3d.load]] and runs the scenario inside the returned `Future[Unit]`. No `Await`/blocking — that does not
  * link on Scala.js.
  *
  * Tolerances: the JS Rapier is a different version from the native/JVM build, so exact solver numbers may differ. Assertions therefore check DIRECTION, SIGN, ordering, presence/absence, and loose
  * magnitude bounds rather than exact floats — but each remains strong enough that a genuinely broken Rapier3D mapping fails it. The per-shape AABB assertions are hand-computed from the known
  * geometry with a ~1e-3 tolerance.
  *
  * Pre-implementation expectation: `sge.platform.PhysicsExtension3d` does not exist AND `PhysicsOpsJs3d` throws `UnsupportedOperationException` on every call, so this suite FAILS — it does not
  * compile because `sge.platform.PhysicsExtension3d` is not found. That capability-absent compile failure is the honest red for this brand-new backend.
  */
class PhysicsBackend3dJsSuite extends FunSuite {

  /** Minimal [[Sge]] context — physics does not touch graphics/audio/input. Mirrors the 2D JS suites. */
  private given Sge =
    Sge(StubApplication, new sge.noop.NoopGraphics(), new sge.noop.NoopAudio(), StubFiles, new sge.noop.NoopInput(), StubNet)

  /** Runs `scenario` against a fresh, real Rapier3D backend, closing the world afterwards. */
  private def withWorld(gravityX: Float, gravityY: Float, gravityZ: Float)(scenario: PhysicsWorld3d => Unit): Future[Unit] =
    sge.platform.PhysicsExtension3d.load().map { _ =>
      val world = new PhysicsWorld3d(gravityX, gravityY, gravityZ)
      try scenario(world)
      finally world.close()
    }

  // ── 1. Gravity ──────────────────────────────────────────────────────────
  test("gravity: a dynamic body with a collider at y=10 falls below y=9 after ~1s") {
    withWorld(0f, -9.81f, 0f) { world =>
      val body = world.createBody(BodyType3d.Dynamic, x = 0f, y = 10f, z = 0f)
      body.attachCollider(Shape3d.Box(0.5f, 0.5f, 0.5f), density = 1f)
      var i = 0
      while (i < 60) {
        world.step(1f / 60f)
        i += 1
      }
      val (_, y, _) = body.position
      assert(y < 9.0f, s"body must fall under gravity (y should drop well below 10), got y=$y")
    }
  }

  // ── 2. Forces / impulses ────────────────────────────────────────────────
  test("forces/impulses: a linear impulse to the right gives a dynamic body rightward velocity and motion") {
    withWorld(0f, 0f, 0f) { world =>
      val body = world.createBody(BodyType3d.Dynamic, x = 0f, y = 0f, z = 0f)
      body.attachCollider(Shape3d.Sphere(1f), density = 1f)

      val (x0, _, _) = body.position
      body.applyImpulse(5f, 0f, 0f)
      val (vx0, vy0, vz0) = body.linearVelocity
      assert(vx0 > 0.1f, s"impulse must produce rightward velocity, got vx=$vx0")
      assert(scala.math.abs(vy0) < 0.1f, s"a purely +x impulse must not produce y velocity, got vy=$vy0")
      assert(scala.math.abs(vz0) < 0.1f, s"a purely +x impulse must not produce z velocity, got vz=$vz0")

      var i = 0
      while (i < 30) {
        world.step(1f / 60f)
        i += 1
      }
      val (x1, _, _) = body.position
      assert(x1 > x0 + 0.5f, s"body must travel rightward after impulse, x went $x0 -> $x1")
    }
  }

  // ── 3. Linear / angular velocity get/set ────────────────────────────────
  test("velocity: set linvel moves the body in that direction; set angvel changes the orientation") {
    withWorld(0f, 0f, 0f) { world =>
      val body = world.createBody(BodyType3d.Dynamic, x = 0f, y = 0f, z = 0f)
      body.attachCollider(Shape3d.Sphere(1f), density = 1f)

      // Linear: set a known velocity, verify the getter round-trips and the body advances accordingly.
      body.linearVelocity = (2f, 0f, 0f)
      val (vx, vy, vz) = body.linearVelocity
      assert(scala.math.abs(vx - 2f) < 0.001f, s"linvel x should round-trip to 2, got $vx")
      assert(scala.math.abs(vy) < 0.001f, s"linvel y should round-trip to 0, got $vy")
      assert(scala.math.abs(vz) < 0.001f, s"linvel z should round-trip to 0, got $vz")
      val (x0, _, _) = body.position
      var i          = 0
      while (i < 30) {
        world.step(1f / 60f)
        i += 1
      }
      val (x1, _, _) = body.position
      // 2 m/s for ~0.5 s -> ~1 m; allow loose bounds for integrator specifics.
      assert(x1 - x0 > 0.6f, s"body moving at vx=2 for 0.5s should advance ~1m, got dx=${x1 - x0}")

      // Angular: set a known spin about Z, verify the getter round-trips and the orientation quaternion changes.
      val (q0x, q0y, q0z, q0w) = body.rotation
      body.angularVelocity = (0f, 0f, 3f)
      val (wx, wy, wz) = body.angularVelocity
      assert(scala.math.abs(wx) < 0.001f, s"angvel x should round-trip to 0, got $wx")
      assert(scala.math.abs(wy) < 0.001f, s"angvel y should round-trip to 0, got $wy")
      assert(scala.math.abs(wz - 3f) < 0.001f, s"angvel z should round-trip to 3, got $wz")
      var j = 0
      while (j < 30) {
        world.step(1f / 60f)
        j += 1
      }
      val (q1x, q1y, q1z, q1w) = body.rotation
      val quatDelta            =
        scala.math.abs(q1x - q0x) + scala.math.abs(q1y - q0y) + scala.math.abs(q1z - q0z) + scala.math.abs(q1w - q0w)
      assert(quatDelta > 0.05f, s"angular velocity about Z must change the orientation quaternion, delta=$quatDelta")
    }
  }

  // ── 4. Body mass ────────────────────────────────────────────────────────
  test("mass: bodyGetMass reflects density x volume for a 1x1x1 box (half-extents 0.5) at density 4") {
    withWorld(0f, 0f, 0f) { world =>
      val body = world.createBody(BodyType3d.Dynamic, x = 0f, y = 0f, z = 0f)
      // Box(0.5,0.5,0.5) is a 1x1x1 volume; mass = density * volume = 4 * 1 = ~4.
      body.attachCollider(Shape3d.Box(0.5f, 0.5f, 0.5f), density = 4f)
      val mass = body.mass
      assert(mass > 0f, s"mass must be positive for a dense collider, got $mass")
      // Loose bound around the analytic value 4.0 (tolerant of solver mass-lumping differences).
      assert(mass > 2f && mass < 8f, s"mass for 1x1x1 box at density 4 should be ~4, got $mass")
    }
  }

  // ── 5. Collider sensor ──────────────────────────────────────────────────
  test("sensor: a sensor collider does NOT push a falling body but IS reported by intersection events") {
    withWorld(0f, -10f, 0f) { world =>
      // Static sensor "floor" at y=0.
      val floor    = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val floorCol = floor.attachCollider(Shape3d.Box(5f, 0.5f, 5f))
      floorCol.isSensor = true
      assert(floorCol.isSensor, "sensor flag must round-trip true")

      // Dynamic ball falls from above; a sensor must NOT stop it.
      val ball    = world.createBody(BodyType3d.Dynamic, x = 0f, y = 5f, z = 0f)
      val ballCol = ball.attachCollider(Shape3d.Sphere(0.5f))

      var sawIntersection = false
      var i               = 0
      while (i < 240) {
        world.step(1f / 60f)
        val events = world.pollIntersectionStartEvents()
        events.foreach { case (c1, c2) =>
          val pair = Set(c1, c2)
          if (pair == Set(floorCol.handle, ballCol.handle)) sawIntersection = true
        }
        i += 1
      }
      val (_, y, _) = ball.position
      assert(y < -1f, s"ball must pass THROUGH a sensor floor (no physical response), got y=$y")
      assert(sawIntersection, "the sensor must report an intersection-start event between the ball and the sensor floor")
    }
  }

  // ── 6. Collider material + restitution rebound ──────────────────────────
  test("material: friction/restitution round-trip; a bouncy body rebounds with upward velocity") {
    withWorld(0f, -10f, 0f) { world =>
      // Round-trip friction and restitution on a probe collider.
      val probeBody = world.createBody(BodyType3d.Dynamic, x = 20f, y = 20f, z = 20f)
      val probe     = probeBody.attachCollider(Shape3d.Sphere(0.5f), friction = 0.7f, restitution = 0.9f)
      assert(scala.math.abs(probe.friction - 0.7f) < 0.001f, s"friction should round-trip to 0.7, got ${probe.friction}")
      assert(scala.math.abs(probe.restitution - 0.9f) < 0.001f, s"restitution should round-trip to 0.9, got ${probe.restitution}")

      // Bouncy ball drops onto a bouncy floor; after impact it must move back upward.
      val floor = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      floor.attachCollider(Shape3d.Box(5f, 0.5f, 5f), restitution = 1f)
      val ball = world.createBody(BodyType3d.Dynamic, x = 0f, y = 5f, z = 0f)
      ball.attachCollider(Shape3d.Sphere(0.5f), restitution = 1f)

      var reboundedUp = false
      var i           = 0
      while (i < 240) {
        world.step(1f / 60f)
        val (_, vy, _) = ball.linearVelocity
        if (i > 10 && vy > 1f) reboundedUp = true
        i += 1
      }
      assert(reboundedUp, "a restitution=1 ball must rebound upward (positive vy) after hitting a bouncy floor")
    }
  }

  // ── 7. Raycast ──────────────────────────────────────────────────────────
  test("raycast: a ray hits a known collider at the expected distance and reports its collider handle") {
    withWorld(0f, 0f, 0f) { world =>
      // Target box centered at x=5, spanning x in [4,6]; ray from origin going +X should hit near x=4.
      val target = world.createBody(BodyType3d.Static, x = 5f, y = 0f, z = 0f)
      val tgtCol = target.attachCollider(Shape3d.Box(1f, 1f, 1f))
      world.step(0f) // refresh the query pipeline

      val hitN = world.rayCast(0f, 0f, 0f, 1f, 0f, 0f, 20f)
      assert(hitN.isDefined, "ray pointing at the box must register a hit")
      val hit = hitN.get
      // Near face of a box centered at 5 with half-width 1 is x=4.
      assert(hit.hitX > 3.5f && hit.hitX < 4.5f, s"hit X should be near the box near-face (~4), got ${hit.hitX}")
      assert(hit.timeOfImpact > 3.5f && hit.timeOfImpact < 4.5f, s"time-of-impact should be ~4, got ${hit.timeOfImpact}")
      assert(
        hit.colliderHandle == tgtCol.handle,
        s"ray must report the target collider handle ${tgtCol.handle}, got ${hit.colliderHandle}"
      )

      // A ray pointing away from the box must miss.
      val miss = world.rayCast(0f, 0f, 0f, -1f, 0f, 0f, 20f)
      assert(miss.isEmpty, "ray pointing away from the only collider must miss")
    }
  }

  // ── 8. AABB / point query (3D) ──────────────────────────────────────────
  test("query: AABB and point queries find colliders at known locations and nothing in empty space") {
    withWorld(0f, 0f, 0f) { world =>
      val b1 = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val b2 = world.createBody(BodyType3d.Static, x = 5f, y = 5f, z = 5f)
      val b3 = world.createBody(BodyType3d.Static, x = 10f, y = 10f, z = 10f)
      b1.attachCollider(Shape3d.Sphere(1f))
      val b2Col = b2.attachCollider(Shape3d.Sphere(1f))
      b3.attachCollider(Shape3d.Sphere(1f))
      world.step(0f)

      val twoOf = world.queryAABB(-2f, -2f, -2f, 7f, 7f, 7f)
      assertEquals(twoOf.size, 2, s"AABB [-2..7]^3 should contain b1 and b2, got ${twoOf.size}")
      val allOf = world.queryAABB(-2f, -2f, -2f, 12f, 12f, 12f)
      assertEquals(allOf.size, 3, s"AABB covering all three should find 3, got ${allOf.size}")
      val none = world.queryAABB(100f, 100f, 100f, 110f, 110f, 110f)
      assertEquals(none.size, 0, s"empty region should find nothing, got ${none.size}")

      // Point query: inside b2's sphere finds b2's collider; far away finds nothing.
      val atB2 = world.queryPoint(5f, 5f, 5f)
      assert(atB2.contains(b2Col.handle), s"point at (5,5,5) must find b2's collider ${b2Col.handle}, got $atB2")
      val empty = world.queryPoint(50f, 50f, 50f)
      assert(empty.isEmpty, s"point in empty space must find nothing, got $empty")
    }
  }

  // ── 9. Revolute joint (3D) ──────────────────────────────────────────────
  test("joint: a revolute joint keeps the dynamic body near its anchor while gravity pulls on it") {
    withWorld(0f, -10f, 0f) { world =>
      // Static pivot at origin; dynamic body 1m to the right (+x), hinged about the Z axis at the origin.
      val pivot = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val arm   = world.createBody(BodyType3d.Dynamic, x = 1f, y = 0f, z = 0f)
      pivot.attachCollider(Shape3d.Sphere(0.1f))
      arm.attachCollider(Shape3d.Sphere(0.1f))

      // Revolute around Z axis at the pivot origin.
      val joint = world.createJoint(JointDef3d.Revolute(pivot, arm, 0f, 0f, 0f, 0f, 0f, 1f))
      assert(joint.isInstanceOf[RevoluteJoint3d], "should create a RevoluteJoint3d")

      var i = 0
      while (i < 120) {
        world.step(1f / 60f)
        i += 1
      }
      // The arm swings under gravity but the hinge keeps it at radius ~1 from the pivot (in the XY plane).
      val (ax, ay, _) = arm.position
      val dist        = scala.math.sqrt((ax * ax + ay * ay).toDouble).toFloat
      assert(dist > 0.7f && dist < 1.3f, s"revolute joint must hold the arm ~1m from the pivot, got dist=$dist")
      // And gravity must actually have pulled it down (constraint isn't pinning it in place).
      assert(ay < -0.1f, s"under gravity the hinged arm should swing downward, got y=$ay")
    }
  }

  // ── 9b. Spring joint (3D) ───────────────────────────────────────────────
  // Spring/Rope/Motor joints are backed by base Rapier3D ImpulseJoint subclasses (Spring/Rope/Fixed), which do NOT
  // expose the UnitImpulseJoint motor/limit mutators. Each of these tests EXECUTES the previously-throwing setter, so a
  // `TypeError: ... is not a function` would fail the test outright; they also assert the high-level getter round-trips
  // the set value and a step does not crash.
  test("joint: a spring joint pulls two separated bodies toward each other and the setters round-trip") {
    withWorld(0f, 0f, 0f) { world =>
      // Two dynamic bodies 4m apart on X; a spring with rest length 1 should pull them together over time.
      val a = world.createBody(BodyType3d.Dynamic, x = -2f, y = 0f, z = 0f)
      val b = world.createBody(BodyType3d.Dynamic, x = 2f, y = 0f, z = 0f)
      a.attachCollider(Shape3d.Sphere(0.1f), density = 1f)
      b.attachCollider(Shape3d.Sphere(0.1f), density = 1f)

      val joint = world.createJoint(JointDef3d.Spring(a, b, restLength = 1f, stiffness = 50f, damping = 2f))
      assert(joint.isInstanceOf[SpringJoint3d], "should create a SpringJoint3d")
      val spring = joint.asInstanceOf[SpringJoint3d]

      val (ax0, _, _) = a.position
      val (bx0, _, _) = b.position
      val dist0       = scala.math.abs(bx0 - ax0)

      // EXECUTE the previously-crashing setters (configureMotorPosition on a base SpringImpulseJoint -> TypeError).
      spring.restLength = 1f
      assert(scala.math.abs(spring.restLength - 1f) < 0.001f, s"restLength should round-trip to 1, got ${spring.restLength}")
      spring.setParams(stiffness = 80f, damping = 3f)

      var i = 0
      while (i < 180) {
        world.step(1f / 60f)
        i += 1
      }
      val (ax1, _, _) = a.position
      val (bx1, _, _) = b.position
      val dist1       = scala.math.abs(bx1 - ax1)
      // The spring (rest length 1) must contract the 4m separation toward the rest length.
      assert(dist1 < dist0 - 0.5f, s"spring should pull the bodies closer (rest length 1), dist went $dist0 -> $dist1")
    }
  }

  // ── 9c. Rope joint (3D) ─────────────────────────────────────────────────
  test("joint: a rope joint keeps a falling body within its max distance and the setter round-trips") {
    withWorld(0f, -10f, 0f) { world =>
      // Static anchor at the origin; a dynamic body hangs from a rope of max distance 2.
      val anchor  = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val hanging = world.createBody(BodyType3d.Dynamic, x = 0f, y = -0.5f, z = 0f)
      anchor.attachCollider(Shape3d.Sphere(0.1f))
      hanging.attachCollider(Shape3d.Sphere(0.1f), density = 1f)

      val joint = world.createJoint(JointDef3d.Rope(anchor, hanging, maxDistance = 2f))
      assert(joint.isInstanceOf[RopeJoint3d], "should create a RopeJoint3d")
      val rope = joint.asInstanceOf[RopeJoint3d]

      // EXECUTE the previously-crashing setter (setLimits on a base RopeImpulseJoint -> TypeError).
      rope.maxDistance = 2f
      assert(scala.math.abs(rope.maxDistance - 2f) < 0.001f, s"maxDistance should round-trip to 2, got ${rope.maxDistance}")

      var i = 0
      while (i < 240) {
        world.step(1f / 60f)
        i += 1
      }
      // Gravity pulls the body down, but the rope must keep it within ~maxDistance of the anchor.
      val (hx, hy, hz) = hanging.position
      val dist         = scala.math.sqrt((hx * hx + hy * hy + hz * hz).toDouble).toFloat
      assert(hy < -0.5f, s"under gravity the hanging body should drop, got y=$hy")
      assert(dist < 2.5f, s"rope (max distance 2) must keep the body within ~2m of the anchor, got dist=$dist")
    }
  }

  // ── 9d. Motor joint (3D) ────────────────────────────────────────────────
  test("joint: a motor joint's setters execute and round-trip without throwing") {
    withWorld(0f, 0f, 0f) { world =>
      val a = world.createBody(BodyType3d.Dynamic, x = 0f, y = 0f, z = 0f)
      val b = world.createBody(BodyType3d.Dynamic, x = 1f, y = 0f, z = 0f)
      a.attachCollider(Shape3d.Sphere(0.1f), density = 1f)
      b.attachCollider(Shape3d.Sphere(0.1f), density = 1f)

      val joint = world.createJoint(JointDef3d.Motor(a, b))
      assert(joint.isInstanceOf[MotorJoint3d], "should create a MotorJoint3d")
      val motor = joint.asInstanceOf[MotorJoint3d]

      // EXECUTE every previously-crashing motor setter (applyMotorTarget -> configureMotorPosition on a base
      // FixedImpulseJoint -> TypeError). The linear offset is applied via setAnchor2; the max-force/torque/correction
      // factor are accepted and stored (a documented divergence from Panama where Rapier3D's fixed joint can't reflect
      // them). All getters must round-trip the set values.
      motor.linearOffset = (0.5f, 0f, 0f)
      val (ox, oy, oz) = motor.linearOffset
      assert(scala.math.abs(ox - 0.5f) < 0.001f, s"linearOffset x should round-trip to 0.5, got $ox")
      assert(scala.math.abs(oy) < 0.001f, s"linearOffset y should round-trip to 0, got $oy")
      assert(scala.math.abs(oz) < 0.001f, s"linearOffset z should round-trip to 0, got $oz")

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
    withWorld(0f, -10f, 0f) { world =>
      val floor    = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val floorCol = floor.attachCollider(Shape3d.Box(5f, 0.5f, 5f))
      val ball     = world.createBody(BodyType3d.Dynamic, x = 0f, y = 3f, z = 0f)
      val ballCol  = ball.attachCollider(Shape3d.Sphere(0.5f))

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

  // ── 11-18. Per-shape colliderGetAabb + colliderGetShapeType coverage ─────
  // One test per factory-produced 3D shape. Each EXECUTES that shape's `colliderGetAabb` branch (a TypeError in
  // any branch fails the test) and asserts the world-space 3D AABB computed by hand for the known geometry, plus
  // the SGE shapeType code (exercising the shape-type mapping per shape).
  // SGE 3D shapeType contract: 0=ball, 1=cuboid, 2=capsule, 3=cylinder, 4=cone, 5=convex, 6=trimesh, 7=heightfield.
  private val aabbTol = 1e-3f

  private def assertAabb(
    actual: (Float, Float, Float, Float, Float, Float),
    eMinX:  Float,
    eMinY:  Float,
    eMinZ:  Float,
    eMaxX:  Float,
    eMaxY:  Float,
    eMaxZ:  Float,
    label:  String
  ): Unit = {
    val (minX, minY, minZ, maxX, maxY, maxZ) = actual
    assert(scala.math.abs(minX - eMinX) < aabbTol, s"$label AABB minX should be $eMinX, got $minX")
    assert(scala.math.abs(minY - eMinY) < aabbTol, s"$label AABB minY should be $eMinY, got $minY")
    assert(scala.math.abs(minZ - eMinZ) < aabbTol, s"$label AABB minZ should be $eMinZ, got $minZ")
    assert(scala.math.abs(maxX - eMaxX) < aabbTol, s"$label AABB maxX should be $eMaxX, got $maxX")
    assert(scala.math.abs(maxY - eMaxY) < aabbTol, s"$label AABB maxY should be $eMaxY, got $maxY")
    assert(scala.math.abs(maxZ - eMaxZ) < aabbTol, s"$label AABB maxZ should be $eMaxZ, got $maxZ")
  }

  test("shape coverage: sphere collider AABB + shapeType (ball=0)") {
    withWorld(0f, 0f, 0f) { world =>
      // radius-1 sphere centered at (-2,4,3) spans [-3,3,2]..[-1,5,4].
      val body = world.createBody(BodyType3d.Static, x = -2f, y = 4f, z = 3f)
      val col  = body.attachCollider(Shape3d.Sphere(1f))
      world.step(0f)
      assertAabb(col.aabb, -3f, 3f, 2f, -1f, 5f, 4f, "sphere")
      assertEquals(col.shapeType, 0, s"sphere shapeType should be 0 (ball), got ${col.shapeType}")
    }
  }

  test("shape coverage: box collider AABB + shapeType (cuboid=1)") {
    withWorld(0f, 0f, 0f) { world =>
      // 1x1x1 box (half-extents 0.5) centered at (3,7,2) spans [2.5,6.5,1.5]..[3.5,7.5,2.5].
      val body = world.createBody(BodyType3d.Static, x = 3f, y = 7f, z = 2f)
      val col  = body.attachCollider(Shape3d.Box(0.5f, 0.5f, 0.5f))
      world.step(0f)
      assertAabb(col.aabb, 2.5f, 6.5f, 1.5f, 3.5f, 7.5f, 2.5f, "box")
      assertEquals(col.shapeType, 1, s"box shapeType should be 1 (cuboid), got ${col.shapeType}")
    }
  }

  test("shape coverage: capsule collider AABB + shapeType (capsule=2)") {
    withWorld(0f, 0f, 0f) { world =>
      // Capsule halfHeight=1, radius=0.5 along Y at origin: segment tips at y=±1 padded by 0.5 in all axes →
      // x,z ∈ [-0.5,0.5], y ∈ [-1.5,1.5].
      val body = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val col  = body.attachCollider(Shape3d.Capsule(1f, 0.5f))
      world.step(0f)
      assertAabb(col.aabb, -0.5f, -1.5f, -0.5f, 0.5f, 1.5f, 0.5f, "capsule")
      assertEquals(col.shapeType, 2, s"capsule shapeType should be 2 (capsule), got ${col.shapeType}")
    }
  }

  test("shape coverage: cylinder collider AABB + shapeType (cylinder=3)") {
    withWorld(0f, 0f, 0f) { world =>
      // Cylinder halfHeight=1, radius=0.5 along Y at origin: x,z ∈ [-0.5,0.5], y ∈ [-1,1].
      val body = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val col  = body.attachCollider(Shape3d.Cylinder(1f, 0.5f))
      world.step(0f)
      assertAabb(col.aabb, -0.5f, -1f, -0.5f, 0.5f, 1f, 0.5f, "cylinder")
      assertEquals(col.shapeType, 3, s"cylinder shapeType should be 3 (cylinder), got ${col.shapeType}")
    }
  }

  test("shape coverage: cone collider AABB + shapeType (cone=4)") {
    withWorld(0f, 0f, 0f) { world =>
      // Cone halfHeight=1, base radius=0.5 along Y at origin (centered): x,z ∈ [-0.5,0.5], y ∈ [-1,1].
      val body = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val col  = body.attachCollider(Shape3d.Cone(1f, 0.5f))
      world.step(0f)
      assertAabb(col.aabb, -0.5f, -1f, -0.5f, 0.5f, 1f, 0.5f, "cone")
      assertEquals(col.shapeType, 4, s"cone shapeType should be 4 (cone), got ${col.shapeType}")
    }
  }

  test("shape coverage: convex-hull collider AABB + shapeType (convex=5)") {
    withWorld(0f, 0f, 0f) { world =>
      // Convex hull of a unit cube (corners ±1 on each axis), body at (5,0,0): local [-1,-1,-1]..[1,1,1] →
      // world [4,-1,-1]..[6,1,1].
      val cubeVerts = Array(
        -1f, -1f, -1f, 1f, -1f, -1f, 1f, 1f, -1f, -1f, 1f, -1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f
      )
      val body = world.createBody(BodyType3d.Static, x = 5f, y = 0f, z = 0f)
      val col  = body.attachCollider(Shape3d.ConvexHull(cubeVerts))
      world.step(0f)
      assertAabb(col.aabb, 4f, -1f, -1f, 6f, 1f, 1f, "convexHull")
      assertEquals(col.shapeType, 5, s"convexHull shapeType should be 5 (convex), got ${col.shapeType}")
    }
  }

  test("shape coverage: trimesh collider AABB + shapeType (trimesh=6)") {
    withWorld(0f, 0f, 0f) { world =>
      // TriMesh of a single triangle (0,0,0),(4,0,0),(0,3,0), body at origin: vertex AABB [0,0,0]..[4,3,0].
      val verts   = Array(0f, 0f, 0f, 4f, 0f, 0f, 0f, 3f, 0f)
      val indices = Array(0, 1, 2)
      val body    = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val col     = body.attachCollider(Shape3d.TriMesh(verts, indices))
      world.step(0f)
      assertAabb(col.aabb, 0f, 0f, 0f, 4f, 3f, 0f, "trimesh")
      assertEquals(col.shapeType, 6, s"trimesh shapeType should be 6 (trimesh), got ${col.shapeType}")
    }
  }

  test("shape coverage: heightfield collider AABB + shapeType (heightfield=7)") {
    withWorld(0f, 0f, 0f) { world =>
      // 3x3 heightfield, scaleX=4, scaleY=2, scaleZ=4, body at origin. Rapier centers the field on the body:
      // x ∈ [-2,2], z ∈ [-2,2]; heights are in [0,1] so scaled y ∈ [0,2]. AABB [-2,0,-2]..[2,2,2].
      val heights = Array(
        0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f
      )
      val body = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val col  = body.attachCollider(Shape3d.Heightfield(heights, nrows = 3, ncols = 3, scaleX = 4f, scaleY = 2f, scaleZ = 4f))
      world.step(0f)
      assertAabb(col.aabb, -2f, 0f, -2f, 2f, 2f, 2f, "heightfield")
      assertEquals(col.shapeType, 7, s"heightfield shapeType should be 7 (heightfield), got ${col.shapeType}")
    }
  }

  // ─── Minimal stub subsystems (mirror the 2D JS suites) ───────────────────

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
