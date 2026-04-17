/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package physics

import munit.FunSuite

/** Integration tests for physics module — requires native library.
  *
  * These tests are skipped if the native library is not available. To run them locally, build sge-native-components first:
  * {{{
  * cd ../sge-native-components && cargo build --release -p sge_physics
  * }}}
  */
class PhysicsIntegrationSuite extends FunSuite {

  /** Check if native library is available. */
  private lazy val nativeAvailable: Boolean =
    try {
      val w = new PhysicsWorld()
      w.close()
      true
    } catch {
      case _: UnsatisfiedLinkError | _: ExceptionInInitializerError => false
    }

  /** Skip test if native library is not available. */
  private def requireNative(): Unit =
    assume(nativeAvailable, "Native physics library not available — skipping test")

  test("PhysicsWorld creation and basic step") {
    requireNative()
    val world = new PhysicsWorld(0f, -10f)
    try {
      world.step(1f / 60f)
      val (gx, gy) = world.gravity
      assertEqualsFloat(gx, 0f, 0.001f)
      assertEqualsFloat(gy, -10f, 0.001f)
    } finally
      world.close()
  }

  test("RigidBody position and velocity") {
    requireNative()
    val world = new PhysicsWorld(0f, -10f)
    try {
      val body   = world.createBody(BodyType.Dynamic, x = 5f, y = 10f)
      val (x, y) = body.position
      assertEqualsFloat(x, 5f, 0.001f)
      assertEqualsFloat(y, 10f, 0.001f)

      body.linearVelocity = (1f, 2f)
      val (vx, vy) = body.linearVelocity
      assertEqualsFloat(vx, 1f, 0.001f)
      assertEqualsFloat(vy, 2f, 0.001f)
    } finally
      world.close()
  }

  test("RigidBody mass and inertia queries") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      // Attach a box collider with density 2.0
      body.attachCollider(Shape.Box(1f, 1f), density = 2f)

      // Mass should be area * density = 4 * 2 = 8
      val mass = body.mass
      assert(mass > 0f, s"Mass should be positive, got $mass")

      val inertia = body.inertia
      assert(inertia > 0f, s"Inertia should be positive, got $inertia")

      val (cx, cy) = body.localCenterOfMass
      // Center of mass should be at origin for symmetric shape
      assertEqualsFloat(cx, 0f, 0.1f)
      assertEqualsFloat(cy, 0f, 0.1f)
    } finally
      world.close()
  }

  test("Collider collision groups") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body     = world.createBody(BodyType.Dynamic)
      val collider = body.attachCollider(Shape.Circle(1f))

      // Default should be all groups
      val defaultGroups = collider.collisionGroups
      assertEquals(defaultGroups.memberships, 0xffffffff)
      assertEquals(defaultGroups.filter, 0xffffffff)

      // Set custom groups
      collider.collisionGroups = CollisionGroups(memberships = 0x0001, filter = 0x0002)
      val custom = collider.collisionGroups
      assertEquals(custom.memberships, 0x0001)
      assertEquals(custom.filter, 0x0002)
    } finally
      world.close()
  }

  test("Collider solver groups") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body     = world.createBody(BodyType.Dynamic)
      val collider = body.attachCollider(Shape.Circle(1f))

      // Set solver groups (controls force response separately from detection)
      collider.solverGroups = CollisionGroups(memberships = 0x0004, filter = 0x0008)
      val solver = collider.solverGroups
      assertEquals(solver.memberships, 0x0004)
      assertEquals(solver.filter, 0x0008)
    } finally
      world.close()
  }

  test("RevoluteJoint limits") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body1 = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint = world.createJoint(JointDef.Revolute(body1, body2, 0f, 0f))
      assert(joint.isInstanceOf[RevoluteJoint], "Should create RevoluteJoint")
      val revolute = joint.asInstanceOf[RevoluteJoint]

      // Enable limits
      revolute.enableLimits(true)
      assert(revolute.isLimitEnabled, "Limits should be enabled")

      // Set limits
      val lower = -scala.math.Pi.toFloat / 4
      val upper = scala.math.Pi.toFloat / 4
      revolute.setLimits(lower, upper)
      val (lo, hi) = revolute.limits
      assertEqualsFloat(lo, lower, 0.001f)
      assertEqualsFloat(hi, upper, 0.001f)

      // Disable limits
      revolute.enableLimits(false)
      assert(!revolute.isLimitEnabled, "Limits should be disabled")
    } finally
      world.close()
  }

  test("RevoluteJoint motor") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint    = world.createJoint(JointDef.Revolute(body1, body2, 0f, 0f))
      val revolute = joint.asInstanceOf[RevoluteJoint]

      // Enable motor and set speed
      revolute.enableMotor(true)
      revolute.motorSpeed = 2f
      revolute.maxMotorTorque_=(100f)

      val speed = revolute.motorSpeed
      assertEqualsFloat(speed, 2f, 0.001f)
    } finally
      world.close()
  }

  test("RevoluteJoint angle query") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f) // No gravity for predictable angles
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint    = world.createJoint(JointDef.Revolute(body1, body2, 0f, 0f))
      val revolute = joint.asInstanceOf[RevoluteJoint]

      // Initial angle should be close to 0
      val angle = revolute.angle
      assertEqualsFloat(angle, 0f, 0.1f)
    } finally
      world.close()
  }

  test("PrismaticJoint limits") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      // Prismatic joint along X axis
      val joint = world.createJoint(JointDef.Prismatic(body1, body2, 1f, 0f))
      assert(joint.isInstanceOf[PrismaticJoint], "Should create PrismaticJoint")
      val prismatic = joint.asInstanceOf[PrismaticJoint]

      // Enable and set limits
      prismatic.enableLimits(true)
      prismatic.setLimits(-2f, 2f)
      val (lo, hi) = prismatic.limits
      assertEqualsFloat(lo, -2f, 0.001f)
      assertEqualsFloat(hi, 2f, 0.001f)
    } finally
      world.close()
  }

  test("PrismaticJoint motor") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint     = world.createJoint(JointDef.Prismatic(body1, body2, 1f, 0f))
      val prismatic = joint.asInstanceOf[PrismaticJoint]

      prismatic.enableMotor(true)
      prismatic.motorSpeed_=(5f)
      prismatic.maxMotorForce_=(1000f)
    } finally
      world.close()
  }

  test("PrismaticJoint translation query") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 2f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint     = world.createJoint(JointDef.Prismatic(body1, body2, 1f, 0f))
      val prismatic = joint.asInstanceOf[PrismaticJoint]

      val translation = prismatic.translation
      // Translation should reflect initial body offset
      assert(translation >= 0f, s"Translation should be non-negative, got $translation")
    } finally
      world.close()
  }

  test("FixedJoint creation") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 1f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint = world.createJoint(JointDef.Fixed(body1, body2))
      assert(joint.isInstanceOf[FixedJoint], "Should create FixedJoint")
    } finally
      world.close()
  }

  test("AABB query finds colliders") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      // Create bodies at known positions
      val body1 = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val body2 = world.createBody(BodyType.Static, x = 5f, y = 5f)
      val body3 = world.createBody(BodyType.Static, x = 10f, y = 10f)
      body1.attachCollider(Shape.Circle(1f))
      body2.attachCollider(Shape.Circle(1f))
      body3.attachCollider(Shape.Circle(1f))

      // Step to update query pipeline (required before spatial queries)
      world.step(0f)

      // Query AABB that includes body1 and body2 but not body3
      val results = world.queryAABB(-2f, -2f, 7f, 7f)
      assertEquals(results.size, 2, s"Should find 2 colliders, found ${results.size}")

      // Query AABB that includes all
      val allResults = world.queryAABB(-2f, -2f, 12f, 12f)
      assertEquals(allResults.size, 3, s"Should find 3 colliders, found ${allResults.size}")

      // Query AABB that includes none
      val noResults = world.queryAABB(100f, 100f, 110f, 110f)
      assertEquals(noResults.size, 0, "Should find no colliders")
    } finally
      world.close()
  }

  test("Collision filtering prevents collision") {
    requireNative()
    val world = new PhysicsWorld(0f, -10f)
    try {
      // Body 1 in group 0, only collides with group 0
      val body1     = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val collider1 = body1.attachCollider(Shape.Box(5f, 0.5f))
      collider1.collisionGroups = CollisionGroups.single(0)

      // Body 2 in group 1, only collides with group 1 — should NOT collide with body1
      val body2     = world.createBody(BodyType.Dynamic, x = 0f, y = 5f)
      val collider2 = body2.attachCollider(Shape.Circle(1f))
      collider2.collisionGroups = CollisionGroups.single(1)

      // Step simulation — body2 should fall through body1
      for (_ <- 1 to 60)
        world.step(1f / 60f)

      val (_, y2) = body2.position
      assert(y2 < 0f, s"Body2 should fall through body1, y=$y2")
    } finally
      world.close()
  }

  test("Collision filtering allows collision") {
    requireNative()
    val world = new PhysicsWorld(0f, -10f)
    try {
      // Body 1 in group 0
      val body1     = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val collider1 = body1.attachCollider(Shape.Box(5f, 0.5f))
      collider1.collisionGroups = CollisionGroups.single(0)

      // Body 2 also in group 0 — should collide with body1
      val body2     = world.createBody(BodyType.Dynamic, x = 0f, y = 5f)
      val collider2 = body2.attachCollider(Shape.Circle(1f))
      collider2.collisionGroups = CollisionGroups.single(0)

      // Step simulation — body2 should land on body1
      for (_ <- 1 to 120)
        world.step(1f / 60f)

      val (_, y2) = body2.position
      assert(y2 > 0f, s"Body2 should rest on body1, y=$y2")
    } finally
      world.close()
  }

  test("MotorJoint creation and offset control") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f) // No gravity for predictable behavior
    try {
      val body1 = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val body2 = world.createBody(BodyType.Dynamic, x = 1f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))

      val joint = world.createJoint(JointDef.Motor(body1, body2))
      assert(joint.isInstanceOf[MotorJoint], "Should create MotorJoint")
      val motor = joint.asInstanceOf[MotorJoint]

      // Set and read linear offset
      motor.linearOffset = (2f, 3f)
      val (ox, oy) = motor.linearOffset
      assertEqualsFloat(ox, 2f, 0.001f)
      assertEqualsFloat(oy, 3f, 0.001f)

      // Set and read angular offset
      motor.angularOffset = 1.5f
      val angle = motor.angularOffset
      assertEqualsFloat(angle, 1.5f, 0.001f)

      // Set motor parameters (no getter needed, just verify no crash)
      motor.maxForce_=(100f)
      motor.maxTorque_=(50f)
      motor.correctionFactor_=(0.5f)
    } finally
      world.close()
  }

  test("MouseJoint creation and target setting") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f) // No gravity for predictable behavior
    try {
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      body.attachCollider(Shape.Circle(0.5f))

      // Create mouse joint targeting (5, 5)
      val joint = world.createJoint(JointDef.Mouse(body, 5f, 5f))
      assert(joint.isInstanceOf[MouseJoint], "Should create MouseJoint")
      val mouse = joint.asInstanceOf[MouseJoint]

      // Verify initial target
      val (tx, ty) = mouse.target
      assertEqualsFloat(tx, 5f, 0.001f)
      assertEqualsFloat(ty, 5f, 0.001f)

      // Update target
      mouse.target = (10f, -3f)
      val (tx2, ty2) = mouse.target
      assertEqualsFloat(tx2, 10f, 0.001f)
      assertEqualsFloat(ty2, -3f, 0.001f)

      // Set motor parameters (verify no crash)
      mouse.maxForce_=(500f)
      mouse.correctionFactor_=(0.5f)
    } finally
      world.close()
  }

  test("MouseJoint target tracking") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f) // No gravity
    try {
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      body.attachCollider(Shape.Circle(0.5f))

      val joint = world.createJoint(JointDef.Mouse(body, 5f, 0f))
      val mouse = joint.asInstanceOf[MouseJoint]

      // Configure for strong pull
      mouse.maxForce_=(10000f)
      mouse.correctionFactor_=(0.8f)

      // Step simulation — body should move toward target
      for (_ <- 1 to 60)
        world.step(1f / 60f)

      val (bx, _) = body.position
      assert(bx > 1f, s"Body should move toward target (x=5), got x=$bx")
    } finally
      world.close()
  }

  test("MouseJoint destruction cleans up anchor body") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      val body = world.createBody(BodyType.Dynamic, x = 0f, y = 0f)
      body.attachCollider(Shape.Circle(0.5f))

      val joint = world.createJoint(JointDef.Mouse(body, 5f, 5f))
      // Destroying the mouse joint should not crash (also destroys the anchor body)
      world.destroyJoint(joint)
      // Step after destruction — should not crash
      world.step(1f / 60f)
    } finally
      world.close()
  }

  test("Contact detail query between overlapping bodies") {
    requireNative()
    val world = new PhysicsWorld(0f, -10f)
    try {
      // Create a static floor
      val floor    = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val floorCol = floor.attachCollider(Shape.Box(10f, 0.5f))

      // Create a dynamic body that will fall onto the floor
      val ball    = world.createBody(BodyType.Dynamic, x = 0f, y = 5f)
      val ballCol = ball.attachCollider(Shape.Circle(1f))

      // Step simulation until ball contacts the floor
      for (_ <- 1 to 120)
        world.step(1f / 60f)

      // Query contact points
      val contacts = world.getContactPoints(ballCol, floorCol)
      // The ball should be resting on the floor, so there should be at least 1 contact point
      assert(contacts.nonEmpty, s"Should have contact points, got ${contacts.length}")

      // Contact normal should point roughly upward (from floor toward ball)
      val cp = contacts(0)
      // normalY should be positive (pointing up) or negative (pointing down) depending on convention
      // Just verify it's a valid normal with nonzero magnitude
      val normalMag = scala.math.sqrt(cp.normalX * cp.normalX + cp.normalY * cp.normalY).toFloat
      assert(normalMag > 0.9f && normalMag < 1.1f, s"Normal should be unit length, got $normalMag")
    } finally
      world.close()
  }

  test("Contact pair count returns zero for non-overlapping colliders") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f) // No gravity
    try {
      val body1 = world.createBody(BodyType.Static, x = 0f, y = 0f)
      val col1  = body1.attachCollider(Shape.Circle(1f))

      val body2 = world.createBody(BodyType.Static, x = 100f, y = 100f)
      val col2  = body2.attachCollider(Shape.Circle(1f))

      world.step(0f) // Update pipeline

      val contacts = world.getContactPoints(col1, col2)
      assertEquals(contacts.length, 0, "Should have no contact points for non-overlapping colliders")
    } finally
      world.close()
  }

  test("RigidBody damping and gravity scale getters") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      body.attachCollider(Shape.Circle(1f))
      body.linearDamping = 0.5f
      body.angularDamping = 0.3f
      body.gravityScale = 2f
      assertEqualsFloat(body.linearDamping, 0.5f, 0.001f)
      assertEqualsFloat(body.angularDamping, 0.3f, 0.001f)
      assertEqualsFloat(body.gravityScale, 2f, 0.001f)
    } finally world.close()
  }

  test("RigidBody enable and disable") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      body.attachCollider(Shape.Circle(1f))
      assert(body.isEnabled, "Body should be enabled by default")
      body.isEnabled = false
      assert(!body.isEnabled, "Body should be disabled")
      body.isEnabled = true
      assert(body.isEnabled, "Body should be re-enabled")
    } finally world.close()
  }

  test("RigidBody CCD enable/disable") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      body.attachCollider(Shape.Circle(1f))
      assert(!body.isCcdEnabled, "CCD should be disabled by default")
      body.isCcdEnabled = true
      assert(body.isCcdEnabled, "CCD should be enabled")
    } finally world.close()
  }

  test("RigidBody translation locking") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      body.attachCollider(Shape.Circle(1f))
      body.setEnabledTranslations(allowX = true, allowY = false)
      assert(!body.isTranslationLockedX, "X should not be locked")
      assert(body.isTranslationLockedY, "Y should be locked")
    } finally world.close()
  }

  test("RigidBody dominance group") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      body.attachCollider(Shape.Circle(1f))
      body.dominanceGroup = 5
      assertEquals(body.dominanceGroup, 5)
    } finally world.close()
  }

  test("RigidBody force and impulse at point") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      val body = world.createBody(BodyType.Dynamic)
      body.attachCollider(Shape.Circle(1f))
      body.applyForceAtPoint(10f, 0f, 1f, 0f)
      body.applyImpulseAtPoint(0f, 5f, 0f, 1f)
      body.applyTorqueImpulse(1f)
      world.step(1f / 60f)
      // Body should have moved
      val (vx, vy) = body.linearVelocity
      assert(vx != 0f || vy != 0f, "Body should have velocity after impulse")
    } finally world.close()
  }

  test("RigidBody world center of mass") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic, x = 3f, y = 4f)
      body.attachCollider(Shape.Circle(1f))
      val (cx, cy) = body.worldCenterOfMass
      assertEqualsFloat(cx, 3f, 0.1f)
      assertEqualsFloat(cy, 4f, 0.1f)
    } finally world.close()
  }

  test("Collider property getters") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      val col  = body.attachCollider(Shape.Circle(1f), density = 2f, friction = 0.7f, restitution = 0.4f)
      assertEqualsFloat(col.density, 2f, 0.001f)
      assertEqualsFloat(col.friction, 0.7f, 0.001f)
      assertEqualsFloat(col.restitution, 0.4f, 0.001f)
      assert(!col.isSensor, "Should not be a sensor by default")
      col.isSensor = true
      assert(col.isSensor, "Should be a sensor after setting")
    } finally world.close()
  }

  test("Collider position and AABB") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Static, x = 5f, y = 3f)
      val col  = body.attachCollider(Shape.Box(1f, 1f))
      world.step(0f)
      val (wx, wy, _) = col.worldPosition
      assertEqualsFloat(wx, 5f, 0.1f)
      assertEqualsFloat(wy, 3f, 0.1f)
      val (minX, minY, maxX, maxY) = col.aabb
      assert(minX < 5f && maxX > 5f, s"AABB should span body position: $minX..$maxX")
      assert(minY < 3f && maxY > 3f, s"AABB should span body position: $minY..$maxY")
    } finally world.close()
  }

  test("Collider enable and disable") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      val col  = body.attachCollider(Shape.Circle(1f))
      assert(col.isEnabled, "Collider should be enabled by default")
      col.isEnabled = false
      assert(!col.isEnabled, "Collider should be disabled")
    } finally world.close()
  }

  test("Collider mass and parent body") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body = world.createBody(BodyType.Dynamic)
      val col  = body.attachCollider(Shape.Circle(1f), density = 3f)
      val mass = col.mass
      assert(mass > 0f, s"Mass should be positive, got $mass")
      val parent = col.parentBody
      assert(parent != 0L, "Should have a parent body")
    } finally world.close()
  }

  test("SpringJoint creation and properties") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      val body1 = world.createBody(BodyType.Static)
      val body2 = world.createBody(BodyType.Dynamic, x = 5f, y = 0f)
      body1.attachCollider(Shape.Circle(0.1f))
      body2.attachCollider(Shape.Circle(0.1f))
      val joint = world.createJoint(JointDef.Spring(body1, body2, restLength = 3f, stiffness = 100f, damping = 10f))
      assert(joint.isInstanceOf[SpringJoint], "Should create SpringJoint")
      val spring = joint.asInstanceOf[SpringJoint]
      assertEqualsFloat(spring.restLength, 3f, 0.001f)
      spring.restLength = 5f
      assertEqualsFloat(spring.restLength, 5f, 0.001f)
      spring.setParams(200f, 20f)
      // Step to verify no crash
      for (_ <- 1 to 60) world.step(1f / 60f)
    } finally world.close()
  }

  test("Joint property getters") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      val b1 = world.createBody(BodyType.Static)
      val b2 = world.createBody(BodyType.Dynamic, x = 1f)
      b1.attachCollider(Shape.Circle(0.1f))
      b2.attachCollider(Shape.Circle(0.1f))
      // Revolute
      val rev = world.createJoint(JointDef.Revolute(b1, b2, 0f, 0f)).asInstanceOf[RevoluteJoint]
      rev.enableMotor(true)
      rev.maxMotorTorque = 50f
      assertEqualsFloat(rev.maxMotorTorque, 50f, 0.001f)
      world.destroyJoint(rev)
      // Prismatic
      val pris = world.createJoint(JointDef.Prismatic(b1, b2, 1f, 0f)).asInstanceOf[PrismaticJoint]
      pris.enableMotor(true)
      pris.motorSpeed = 3f
      pris.maxMotorForce = 100f
      assertEqualsFloat(pris.motorSpeed, 3f, 0.001f)
      assertEqualsFloat(pris.maxMotorForce, 100f, 0.001f)
      world.destroyJoint(pris)
      // Motor
      val motor = world.createJoint(JointDef.Motor(b1, b2)).asInstanceOf[MotorJoint]
      motor.maxForce = 200f
      motor.maxTorque = 100f
      motor.correctionFactor = 0.5f
      assertEqualsFloat(motor.maxForce, 200f, 0.001f)
      assertEqualsFloat(motor.maxTorque, 100f, 0.001f)
      assertEqualsFloat(motor.correctionFactor, 0.5f, 0.01f)
    } finally world.close()
  }

  test("Solver parameters") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      world.numSolverIterations = 8
      assertEquals(world.numSolverIterations, 8)
      // Just verify these don't crash
      world.setNumAdditionalFrictionIterations(2)
      world.setNumInternalPgsIterations(1)
    } finally world.close()
  }

  test("rayCastAll returns multiple hits") {
    requireNative()
    val world = new PhysicsWorld(0f, 0f)
    try {
      val b1 = world.createBody(BodyType.Static, x = 0f, y = 5f)
      b1.attachCollider(Shape.Box(5f, 0.5f))
      val b2 = world.createBody(BodyType.Static, x = 0f, y = 10f)
      b2.attachCollider(Shape.Box(5f, 0.5f))
      world.step(0f)
      val hits = world.rayCastAll(0f, 0f, 0f, 1f, 20f)
      assert(hits.size >= 2, s"Should hit at least 2 colliders, got ${hits.size}")
    } finally world.close()
  }

  test("TriMesh collider creation") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body    = world.createBody(BodyType.Static)
      val verts   = Array(0f, 0f, 1f, 0f, 0.5f, 1f) // triangle
      val indices = Array(0, 1, 2)
      val col     = body.attachCollider(Shape.TriMesh(verts, indices))
      assert(col.shapeType == 5, s"TriMesh shape type should be 5, got ${col.shapeType}")
    } finally world.close()
  }

  test("Heightfield collider creation") {
    requireNative()
    val world = new PhysicsWorld()
    try {
      val body    = world.createBody(BodyType.Static)
      val heights = Array(0f, 1f, 0.5f, 0.2f, 0f)
      val col     = body.attachCollider(Shape.Heightfield(heights, scaleX = 10f, scaleY = 1f))
      assert(col.shapeType == 7, s"Heightfield shape type should be 7, got ${col.shapeType}")
    } finally world.close()
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float)(implicit loc: munit.Location): Unit =
    assert(scala.math.abs(actual - expected) <= delta, s"Expected $expected +/- $delta, got $actual")
}
