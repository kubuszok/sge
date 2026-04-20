/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package physics3d

import munit.FunSuite

/** Integration tests for 3D physics module — requires native library.
  *
  * These tests are skipped if the native library is not available. To run them locally, build sge-native-providers first:
  * {{{
  * cd ../sge-native-providers && cargo build --release -p sge_physics3d
  * }}}
  */
class PhysicsIntegration3dSuite extends FunSuite {

  /** Check if native library is available. */
  private lazy val nativeAvailable: Boolean =
    try {
      val w = new PhysicsWorld3d()
      w.close()
      true
    } catch {
      case _: UnsatisfiedLinkError | _: ExceptionInInitializerError => false
    }

  /** Skip test if native library is not available. */
  private def requireNative(): Unit =
    assume(nativeAvailable, "Native physics3d library not available — skipping test")

  // ─── World ──────────────────────────────────────────────────────────────

  test("PhysicsWorld3d creation and basic step") {
    requireNative()
    val world = new PhysicsWorld3d(0f, -10f, 0f)
    try {
      world.step(1f / 60f)
      val (gx, gy, gz) = world.gravity
      assertEqualsFloat(gx, 0f, 0.001f)
      assertEqualsFloat(gy, -10f, 0.001f)
      assertEqualsFloat(gz, 0f, 0.001f)
    } finally world.close()
  }

  // ─── Body position and velocity ─────────────────────────────────────────

  test("RigidBody3d position and velocity") {
    requireNative()
    val world = new PhysicsWorld3d(0f, -10f, 0f)
    try {
      val body      = world.createBody(BodyType3d.Dynamic, x = 5f, y = 10f, z = 3f)
      val (x, y, z) = body.position
      assertEqualsFloat(x, 5f, 0.001f)
      assertEqualsFloat(y, 10f, 0.001f)
      assertEqualsFloat(z, 3f, 0.001f)

      body.linearVelocity = (1f, 2f, -1f)
      val (vx, vy, vz) = body.linearVelocity
      assertEqualsFloat(vx, 1f, 0.001f)
      assertEqualsFloat(vy, 2f, 0.001f)
      assertEqualsFloat(vz, -1f, 0.001f)
    } finally world.close()
  }

  // ─── Gravity ────────────────────────────────────────────────────────────

  test("RigidBody3d falls under gravity") {
    requireNative()
    val world = new PhysicsWorld3d(0f, -10f, 0f)
    try {
      val body = world.createBody(BodyType3d.Dynamic, x = 0f, y = 10f, z = 0f)
      body.attachCollider(Shape3d.Sphere(1f))
      for (_ <- 1 to 60)
        world.step(1f / 60f)
      val (_, y, _) = body.position
      assert(y < 10f, s"Body should have fallen, y=$y")
    } finally world.close()
  }

  test("Static body stays in place") {
    requireNative()
    val world = new PhysicsWorld3d(0f, -10f, 0f)
    try {
      val body = world.createBody(BodyType3d.Static, x = 0f, y = 5f, z = 0f)
      body.attachCollider(Shape3d.Sphere(1f))
      for (_ <- 1 to 60)
        world.step(1f / 60f)
      val (_, y, _) = body.position
      assertEqualsFloat(y, 5f, 0.001f)
    } finally world.close()
  }

  // ─── Damping and gravity scale ──────────────────────────────────────────

  test("RigidBody3d damping and gravity scale getters") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      body.linearDamping = 0.5f
      body.angularDamping = 0.3f
      body.gravityScale = 2f
      assertEqualsFloat(body.linearDamping, 0.5f, 0.001f)
      assertEqualsFloat(body.angularDamping, 0.3f, 0.001f)
      assertEqualsFloat(body.gravityScale, 2f, 0.001f)
    } finally world.close()
  }

  // ─── Enable/disable ─────────────────────────────────────────────────────

  test("RigidBody3d enable and disable") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      assert(body.isEnabled, "Body should be enabled by default")
      body.isEnabled = false
      assert(!body.isEnabled, "Body should be disabled")
      body.isEnabled = true
      assert(body.isEnabled, "Body should be re-enabled")
    } finally world.close()
  }

  // ─── CCD ────────────────────────────────────────────────────────────────

  test("RigidBody3d CCD enable/disable") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      assert(!body.isCcdEnabled, "CCD should be disabled by default")
      body.isCcdEnabled = true
      assert(body.isCcdEnabled, "CCD should be enabled")
    } finally world.close()
  }

  // ─── Translation locking ───────────────────────────────────────────────

  test("RigidBody3d translation locking") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      body.setEnabledTranslations(allowX = true, allowY = false, allowZ = true)
      assert(!body.isTranslationLockedX, "X should not be locked")
      assert(body.isTranslationLockedY, "Y should be locked")
      assert(!body.isTranslationLockedZ, "Z should not be locked")
    } finally world.close()
  }

  // ─── Rotation locking ─────────────────────────────────────────────────

  test("RigidBody3d rotation locking") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      body.setEnabledRotations(allowX = false, allowY = true, allowZ = false)
      assert(body.isRotationLockedX, "X rotation should be locked")
      assert(!body.isRotationLockedY, "Y rotation should not be locked")
      assert(body.isRotationLockedZ, "Z rotation should be locked")
    } finally world.close()
  }

  // ─── Dominance group ──────────────────────────────────────────────────

  test("RigidBody3d dominance group") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      body.dominanceGroup = 5
      assertEquals(body.dominanceGroup, 5)
    } finally world.close()
  }

  // ─── Force at point ───────────────────────────────────────────────────

  test("RigidBody3d force and impulse at point") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Sphere(1f))
      body.applyForceAtPoint(10f, 0f, 0f, 1f, 0f, 0f)
      body.applyImpulseAtPoint(0f, 5f, 0f, 0f, 1f, 0f)
      body.applyTorqueImpulse(1f, 0f, 0f)
      world.step(1f / 60f)
      val (vx, vy, vz) = body.linearVelocity
      assert(vx != 0f || vy != 0f || vz != 0f, "Body should have velocity after impulse")
    } finally world.close()
  }

  // ─── World center of mass ─────────────────────────────────────────────

  test("RigidBody3d world center of mass") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic, x = 3f, y = 4f, z = 5f)
      body.attachCollider(Shape3d.Sphere(1f))
      val (cx, cy, cz) = body.worldCenterOfMass
      assertEqualsFloat(cx, 3f, 0.1f)
      assertEqualsFloat(cy, 4f, 0.1f)
      assertEqualsFloat(cz, 5f, 0.1f)
    } finally world.close()
  }

  // ─── Mass and inertia ─────────────────────────────────────────────────

  test("RigidBody3d mass and inertia") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      body.attachCollider(Shape3d.Box(1f, 1f, 1f), density = 2f)
      val mass = body.mass
      assert(mass > 0f, s"Mass should be positive, got $mass")
      val inertia = body.inertia
      assert(inertia > 0f, s"Inertia should be positive, got $inertia")
      val (cx, cy, cz) = body.localCenterOfMass
      assertEqualsFloat(cx, 0f, 0.1f)
      assertEqualsFloat(cy, 0f, 0.1f)
      assertEqualsFloat(cz, 0f, 0.1f)
    } finally world.close()
  }

  // ─── Collider properties ──────────────────────────────────────────────

  test("Collider3d property getters") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      val col  = body.attachCollider(Shape3d.Sphere(1f), density = 2f, friction = 0.7f, restitution = 0.4f)
      assertEqualsFloat(col.density, 2f, 0.001f)
      assertEqualsFloat(col.friction, 0.7f, 0.001f)
      assertEqualsFloat(col.restitution, 0.4f, 0.001f)
      assert(!col.isSensor, "Should not be a sensor by default")
      col.isSensor = true
      assert(col.isSensor, "Should be a sensor after setting")
    } finally world.close()
  }

  // ─── Collider enable/disable ──────────────────────────────────────────

  test("Collider3d enable and disable") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      val col  = body.attachCollider(Shape3d.Sphere(1f))
      assert(col.isEnabled, "Collider should be enabled by default")
      col.isEnabled = false
      assert(!col.isEnabled, "Collider should be disabled")
    } finally world.close()
  }

  // ─── Collider mass and parent ─────────────────────────────────────────

  test("Collider3d mass and parent body") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Dynamic)
      val col  = body.attachCollider(Shape3d.Sphere(1f), density = 3f)
      val mass = col.mass
      assert(mass > 0f, s"Mass should be positive, got $mass")
      val parent = col.parentBody
      assertEquals(parent, body.handle, "Should have the correct parent body")
    } finally world.close()
  }

  // ─── Collider AABB ────────────────────────────────────────────────────

  test("Collider3d AABB") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body = world.createBody(BodyType3d.Static, x = 5f, y = 3f, z = 2f)
      val col  = body.attachCollider(Shape3d.Box(1f, 1f, 1f))
      world.step(0f)
      val (minX, minY, minZ, maxX, maxY, maxZ) = col.aabb
      assert(minX < 5f && maxX > 5f, s"AABB x should span body position: $minX..$maxX")
      assert(minY < 3f && maxY > 3f, s"AABB y should span body position: $minY..$maxY")
      assert(minZ < 2f && maxZ > 2f, s"AABB z should span body position: $minZ..$maxZ")
    } finally world.close()
  }

  // ─── Collision groups ─────────────────────────────────────────────────

  test("Collider3d collision groups") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body          = world.createBody(BodyType3d.Dynamic)
      val collider      = body.attachCollider(Shape3d.Sphere(1f))
      val defaultGroups = collider.collisionGroups
      assertEquals(defaultGroups.memberships, 0xffffffff)
      assertEquals(defaultGroups.filter, 0xffffffff)

      collider.collisionGroups = CollisionGroups3d(memberships = 0x0001, filter = 0x0002)
      val custom = collider.collisionGroups
      assertEquals(custom.memberships, 0x0001)
      assertEquals(custom.filter, 0x0002)
    } finally world.close()
  }

  // ─── RevoluteJoint3d ──────────────────────────────────────────────────

  test("RevoluteJoint3d limits") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body1 = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val body2 = world.createBody(BodyType3d.Dynamic, x = 1f, y = 0f, z = 0f)
      body1.attachCollider(Shape3d.Sphere(0.1f))
      body2.attachCollider(Shape3d.Sphere(0.1f))

      // Revolute around Z axis at origin
      val joint = world.createJoint(JointDef3d.Revolute(body1, body2, 0f, 0f, 0f, 0f, 0f, 1f))
      assert(joint.isInstanceOf[RevoluteJoint3d], "Should create RevoluteJoint3d")
      val revolute = joint.asInstanceOf[RevoluteJoint3d]

      revolute.enableLimits(true)
      assert(revolute.isLimitEnabled, "Limits should be enabled")

      val lower = -scala.math.Pi.toFloat / 4
      val upper = scala.math.Pi.toFloat / 4
      revolute.setLimits(lower, upper)
      val (lo, hi) = revolute.limits
      assertEqualsFloat(lo, lower, 0.001f)
      assertEqualsFloat(hi, upper, 0.001f)

      revolute.enableLimits(false)
      assert(!revolute.isLimitEnabled, "Limits should be disabled")
    } finally world.close()
  }

  // ─── PrismaticJoint3d ─────────────────────────────────────────────────

  test("PrismaticJoint3d motor") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body1 = world.createBody(BodyType3d.Static)
      val body2 = world.createBody(BodyType3d.Dynamic, x = 1f, y = 0f, z = 0f)
      body1.attachCollider(Shape3d.Sphere(0.1f))
      body2.attachCollider(Shape3d.Sphere(0.1f))

      // Prismatic along X axis
      val joint     = world.createJoint(JointDef3d.Prismatic(body1, body2, 1f, 0f, 0f))
      val prismatic = joint.asInstanceOf[PrismaticJoint3d]

      prismatic.enableMotor(true)
      prismatic.motorSpeed = 5f
      prismatic.maxMotorForce = 1000f

      assertEqualsFloat(prismatic.motorSpeed, 5f, 0.001f)
      assertEqualsFloat(prismatic.maxMotorForce, 1000f, 0.001f)
    } finally world.close()
  }

  // ─── FixedJoint3d ─────────────────────────────────────────────────────

  test("FixedJoint3d creation") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      val body1 = world.createBody(BodyType3d.Static)
      val body2 = world.createBody(BodyType3d.Dynamic, x = 1f, y = 1f, z = 1f)
      body1.attachCollider(Shape3d.Sphere(0.1f))
      body2.attachCollider(Shape3d.Sphere(0.1f))

      val joint = world.createJoint(JointDef3d.Fixed(body1, body2))
      assert(joint.isInstanceOf[FixedJoint3d], "Should create FixedJoint3d")
      // Step to verify no crash
      world.step(1f / 60f)
    } finally world.close()
  }

  // ─── RopeJoint3d ──────────────────────────────────────────────────────

  test("RopeJoint3d max distance") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      val body1 = world.createBody(BodyType3d.Static)
      val body2 = world.createBody(BodyType3d.Dynamic, x = 2f, y = 0f, z = 0f)
      body1.attachCollider(Shape3d.Sphere(0.1f))
      body2.attachCollider(Shape3d.Sphere(0.1f))

      val joint = world.createJoint(JointDef3d.Rope(body1, body2, maxDistance = 5f))
      assert(joint.isInstanceOf[RopeJoint3d], "Should create RopeJoint3d")
      val rope = joint.asInstanceOf[RopeJoint3d]

      assertEqualsFloat(rope.maxDistance, 5f, 0.001f)
      rope.maxDistance = 10f
      assertEqualsFloat(rope.maxDistance, 10f, 0.001f)
    } finally world.close()
  }

  // ─── SpringJoint3d ────────────────────────────────────────────────────

  test("SpringJoint3d properties") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      val body1 = world.createBody(BodyType3d.Static)
      val body2 = world.createBody(BodyType3d.Dynamic, x = 5f, y = 0f, z = 0f)
      body1.attachCollider(Shape3d.Sphere(0.1f))
      body2.attachCollider(Shape3d.Sphere(0.1f))

      val joint = world.createJoint(JointDef3d.Spring(body1, body2, restLength = 3f, stiffness = 100f, damping = 10f))
      assert(joint.isInstanceOf[SpringJoint3d], "Should create SpringJoint3d")
      val spring = joint.asInstanceOf[SpringJoint3d]

      assertEqualsFloat(spring.restLength, 3f, 0.001f)
      spring.restLength = 5f
      assertEqualsFloat(spring.restLength, 5f, 0.001f)
      spring.setParams(200f, 20f)
      // Step to verify no crash
      for (_ <- 1 to 60) world.step(1f / 60f)
    } finally world.close()
  }

  // ─── MotorJoint3d ─────────────────────────────────────────────────────

  test("MotorJoint3d offset") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      val body1 = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val body2 = world.createBody(BodyType3d.Dynamic, x = 1f, y = 0f, z = 0f)
      body1.attachCollider(Shape3d.Sphere(0.1f))
      body2.attachCollider(Shape3d.Sphere(0.1f))

      val joint = world.createJoint(JointDef3d.Motor(body1, body2))
      assert(joint.isInstanceOf[MotorJoint3d], "Should create MotorJoint3d")
      val motor = joint.asInstanceOf[MotorJoint3d]

      motor.linearOffset = (2f, 3f, -1f)
      val (ox, oy, oz) = motor.linearOffset
      assertEqualsFloat(ox, 2f, 0.001f)
      assertEqualsFloat(oy, 3f, 0.001f)
      assertEqualsFloat(oz, -1f, 0.001f)

      motor.maxForce = 100f
      motor.maxTorque = 50f
      motor.correctionFactor = 0.5f
      assertEqualsFloat(motor.maxForce, 100f, 0.001f)
      assertEqualsFloat(motor.maxTorque, 50f, 0.001f)
      assertEqualsFloat(motor.correctionFactor, 0.5f, 0.01f)
    } finally world.close()
  }

  // ─── Ray cast ─────────────────────────────────────────────────────────

  test("Ray cast 3D") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      // Place a box at origin
      val body = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      body.attachCollider(Shape3d.Box(1f, 1f, 1f))
      world.step(0f) // Update pipeline

      // Cast ray downward from above
      val hit = world.rayCast(0f, 10f, 0f, 0f, -1f, 0f, 20f)
      import sge.utils.Nullable
      assert(!Nullable.isEmpty(hit), "Ray should hit the box")
    } finally world.close()
  }

  // ─── AABB query ───────────────────────────────────────────────────────

  test("AABB query 3D") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      val body1 = world.createBody(BodyType3d.Static, x = 0f, y = 0f, z = 0f)
      val body2 = world.createBody(BodyType3d.Static, x = 5f, y = 5f, z = 5f)
      val body3 = world.createBody(BodyType3d.Static, x = 10f, y = 10f, z = 10f)
      body1.attachCollider(Shape3d.Sphere(1f))
      body2.attachCollider(Shape3d.Sphere(1f))
      body3.attachCollider(Shape3d.Sphere(1f))
      world.step(0f) // Update pipeline

      // Query AABB that includes body1 and body2 but not body3
      val results = world.queryAABB(-2f, -2f, -2f, 7f, 7f, 7f)
      assertEquals(results.size, 2, s"Should find 2 colliders, found ${results.size}")

      // Query AABB that includes all
      val allResults = world.queryAABB(-2f, -2f, -2f, 12f, 12f, 12f)
      assertEquals(allResults.size, 3, s"Should find 3 colliders, found ${allResults.size}")

      // Query AABB that includes none
      val noResults = world.queryAABB(100f, 100f, 100f, 110f, 110f, 110f)
      assertEquals(noResults.size, 0, "Should find no colliders")
    } finally world.close()
  }

  // ─── Solver parameters ────────────────────────────────────────────────

  test("Solver parameters") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      world.numSolverIterations = 8
      assertEquals(world.numSolverIterations, 8)
      // Just verify these don't crash
      world.setNumAdditionalFrictionIterations(2)
      world.setNumInternalPgsIterations(1)
    } finally world.close()
  }

  // ─── All 3D shapes ────────────────────────────────────────────────────

  test("All 3D shapes creation") {
    requireNative()
    val world = new PhysicsWorld3d()
    try {
      // Sphere
      val b1 = world.createBody(BodyType3d.Dynamic, x = 0f, y = 0f, z = 0f)
      b1.attachCollider(Shape3d.Sphere(1f))

      // Box
      val b2 = world.createBody(BodyType3d.Dynamic, x = 5f, y = 0f, z = 0f)
      b2.attachCollider(Shape3d.Box(1f, 1f, 1f))

      // Capsule
      val b3 = world.createBody(BodyType3d.Dynamic, x = 10f, y = 0f, z = 0f)
      b3.attachCollider(Shape3d.Capsule(halfHeight = 1f, radius = 0.5f))

      // Cylinder
      val b4 = world.createBody(BodyType3d.Dynamic, x = 15f, y = 0f, z = 0f)
      b4.attachCollider(Shape3d.Cylinder(halfHeight = 1f, radius = 0.5f))

      // Cone
      val b5 = world.createBody(BodyType3d.Dynamic, x = 20f, y = 0f, z = 0f)
      b5.attachCollider(Shape3d.Cone(halfHeight = 1f, radius = 0.5f))

      // ConvexHull — a tetrahedron
      val b6        = world.createBody(BodyType3d.Dynamic, x = 25f, y = 0f, z = 0f)
      val hullVerts = Array(
        0f, 0f, 0f, 1f, 0f, 0f, 0.5f, 1f, 0f, 0.5f, 0.5f, 1f
      )
      b6.attachCollider(Shape3d.ConvexHull(hullVerts))

      // TriMesh — a single triangle
      val b7         = world.createBody(BodyType3d.Static, x = 30f, y = 0f, z = 0f)
      val triVerts   = Array(0f, 0f, 0f, 1f, 0f, 0f, 0.5f, 1f, 0f)
      val triIndices = Array(0, 1, 2)
      b7.attachCollider(Shape3d.TriMesh(triVerts, triIndices))

      // Heightfield — 3x3 grid
      val b8      = world.createBody(BodyType3d.Static, x = 35f, y = 0f, z = 0f)
      val heights = Array(
        0f, 1f, 0f, 1f, 2f, 1f, 0f, 1f, 0f
      )
      b8.attachCollider(Shape3d.Heightfield(heights, nrows = 3, ncols = 3, scaleX = 10f, scaleY = 1f, scaleZ = 10f))

      // Step to verify no crash
      world.step(1f / 60f)
    } finally world.close()
  }

  // ─── rayCastAll ───────────────────────────────────────────────────────

  test("rayCastAll 3D returns multiple hits") {
    requireNative()
    val world = new PhysicsWorld3d(0f, 0f, 0f)
    try {
      val b1 = world.createBody(BodyType3d.Static, x = 0f, y = 5f, z = 0f)
      b1.attachCollider(Shape3d.Box(5f, 0.5f, 5f))
      val b2 = world.createBody(BodyType3d.Static, x = 0f, y = 10f, z = 0f)
      b2.attachCollider(Shape3d.Box(5f, 0.5f, 5f))
      world.step(0f)
      val hits = world.rayCastAll(0f, 0f, 0f, 0f, 1f, 0f, 20f)
      assert(hits.size >= 2, s"Should hit at least 2 colliders, got ${hits.size}")
    } finally world.close()
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float)(implicit loc: munit.Location): Unit =
    assert(scala.math.abs(actual - expected) <= delta, s"Expected $expected +/- $delta, got $actual")
}
