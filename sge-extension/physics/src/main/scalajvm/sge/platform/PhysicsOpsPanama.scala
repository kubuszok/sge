/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: Panama FFM delegation to Rust C ABI native lib
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import java.lang.invoke.MethodHandle

/** JVM implementation of [[PhysicsOps]] using Panama Foreign Function & Memory API.
  *
  * Downcall handles invoke `sge_phys_*` C functions exported by the Rust `sge_physics` native library.
  */
private[platform] class PhysicsOpsPanama(val p: PanamaProvider) extends PhysicsOps {
  import p.*

  // ─── Native library + linker setup ─────────────────────────────────────

  private val linker: p.Linker = p.Linker.nativeLinker()

  private val lib: p.SymbolLookup = {
    val found = sge.platform.NativeLibLoader.load("sge_physics")
    p.SymbolLookup.libraryLookup(found, p.Arena.global())
  }

  private def lookup(name: String): p.MemorySegment =
    lib.findOrThrow(name)

  // ─── Method handle cache ──────────────────────────────────────────────

  // World lifecycle
  private val hCreateWorld: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_world"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hDestroyWorld: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_destroy_world"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG)
  )
  private val hWorldStep: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_step"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hWorldSetGravity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_set_gravity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hWorldGetGravity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_get_gravity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS)
  )

  // Rigid body creation
  private val hCreateDynamicBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_dynamic_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateStaticBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_static_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateKinematicBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_kinematic_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hDestroyBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_destroy_body"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Body getters
  private val hBodyGetPosition: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_position"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetAngle: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_angle"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetLinearVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_linear_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetAngularVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_angular_velocity"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Body setters
  private val hBodySetPosition: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_position"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodySetAngle: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_angle"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodySetLinearVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_linear_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodySetAngularVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_angular_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )

  // Body forces
  private val hBodyApplyForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_apply_force"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyImpulse: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_apply_impulse"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_apply_torque"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )

  // Body properties
  private val hBodySetLinearDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_linear_damping"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodySetAngularDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_angular_damping"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodySetGravityScale: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_gravity_scale"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodyIsAwake: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_is_awake"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyWakeUp: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_wake_up"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetFixedRotation: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_fixed_rotation"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )

  // Collider creation
  private val hCreateCircleCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_circle_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hCreateBoxCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_box_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateCapsuleCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_capsule_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreatePolygonCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_polygon_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT)
  )
  private val hDestroyCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_destroy_collider"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Collider properties
  private val hColliderSetDensity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_density"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetFriction: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_friction"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetRestitution: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_restitution"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetSensor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_sensor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )

  // Joints
  private val hCreateRevoluteJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_revolute_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreatePrismaticJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_prismatic_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateFixedJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_fixed_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hDestroyJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_destroy_joint"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Queries
  private val hRayCast: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_ray_cast"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
  )
  private val hQueryPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_query_point"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS, p.JAVA_INT)
  )

  // Contact events
  private val hPollContactStartEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_poll_contact_start_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )
  private val hPollContactStopEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_poll_contact_stop_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )

  // ─── World lifecycle ──────────────────────────────────────────────────

  override def createWorld(gravityX: Float, gravityY: Float): Long =
    hCreateWorld.invoke(gravityX, gravityY).asInstanceOf[Long]

  override def destroyWorld(world: Long): Unit =
    hDestroyWorld.invoke(world)

  override def worldStep(world: Long, dt: Float): Unit =
    hWorldStep.invoke(world, dt)

  override def worldSetGravity(world: Long, gx: Float, gy: Float): Unit =
    hWorldSetGravity.invoke(world, gx, gy)

  override def worldGetGravity(world: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hWorldGetGravity.invoke(world, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  // ─── Rigid body ───────────────────────────────────────────────────────

  override def createDynamicBody(world: Long, x: Float, y: Float, angle: Float): Long =
    hCreateDynamicBody.invoke(world, x, y, angle).asInstanceOf[Long]

  override def createStaticBody(world: Long, x: Float, y: Float, angle: Float): Long =
    hCreateStaticBody.invoke(world, x, y, angle).asInstanceOf[Long]

  override def createKinematicBody(world: Long, x: Float, y: Float, angle: Float): Long =
    hCreateKinematicBody.invoke(world, x, y, angle).asInstanceOf[Long]

  override def destroyBody(world: Long, body: Long): Unit =
    hDestroyBody.invoke(world, body)

  override def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hBodyGetPosition.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def bodyGetAngle(world: Long, body: Long): Float =
    hBodyGetAngle.invoke(world, body).asInstanceOf[Float]

  override def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hBodyGetLinearVelocity.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def bodyGetAngularVelocity(world: Long, body: Long): Float =
    hBodyGetAngularVelocity.invoke(world, body).asInstanceOf[Float]

  override def bodySetPosition(world: Long, body: Long, x: Float, y: Float): Unit =
    hBodySetPosition.invoke(world, body, x, y)

  override def bodySetAngle(world: Long, body: Long, angle: Float): Unit =
    hBodySetAngle.invoke(world, body, angle)

  override def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float): Unit =
    hBodySetLinearVelocity.invoke(world, body, vx, vy)

  override def bodySetAngularVelocity(world: Long, body: Long, omega: Float): Unit =
    hBodySetAngularVelocity.invoke(world, body, omega)

  override def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float): Unit =
    hBodyApplyForce.invoke(world, body, fx, fy)

  override def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float): Unit =
    hBodyApplyImpulse.invoke(world, body, ix, iy)

  override def bodyApplyTorque(world: Long, body: Long, torque: Float): Unit =
    hBodyApplyTorque.invoke(world, body, torque)

  override def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit =
    hBodySetLinearDamping.invoke(world, body, damping)

  override def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit =
    hBodySetAngularDamping.invoke(world, body, damping)

  override def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit =
    hBodySetGravityScale.invoke(world, body, scale)

  override def bodyIsAwake(world: Long, body: Long): Boolean = {
    val result = hBodyIsAwake.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyWakeUp(world: Long, body: Long): Unit =
    hBodyWakeUp.invoke(world, body)

  override def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit =
    hBodySetFixedRotation.invoke(world, body, if (fixed) 1 else 0)

  // ─── Collider ─────────────────────────────────────────────────────────

  override def createCircleCollider(world: Long, body: Long, radius: Float): Long =
    hCreateCircleCollider.invoke(world, body, radius).asInstanceOf[Long]

  override def createBoxCollider(world: Long, body: Long, halfWidth: Float, halfHeight: Float): Long =
    hCreateBoxCollider.invoke(world, body, halfWidth, halfHeight).asInstanceOf[Long]

  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    hCreateCapsuleCollider.invoke(world, body, halfHeight, radius).asInstanceOf[Long]

  override def createPolygonCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, vertices.length.toLong)
      p.MemorySegment.copyFromFloats(vertices, 0, seg, 0L, vertices.length)
      hCreatePolygonCollider.invoke(world, body, seg, vertexCount).asInstanceOf[Long]
    } finally arena.arenaClose()
  }

  override def destroyCollider(world: Long, collider: Long): Unit =
    hDestroyCollider.invoke(world, collider)

  override def colliderSetDensity(world: Long, collider: Long, density: Float): Unit =
    hColliderSetDensity.invoke(world, collider, density)

  override def colliderSetFriction(world: Long, collider: Long, friction: Float): Unit =
    hColliderSetFriction.invoke(world, collider, friction)

  override def colliderSetRestitution(world: Long, collider: Long, restitution: Float): Unit =
    hColliderSetRestitution.invoke(world, collider, restitution)

  override def colliderSetSensor(world: Long, collider: Long, sensor: Boolean): Unit =
    hColliderSetSensor.invoke(world, collider, if (sensor) 1 else 0)

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long =
    hCreateRevoluteJoint.invoke(world, body1, body2, anchorX, anchorY).asInstanceOf[Long]

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long =
    hCreatePrismaticJoint.invoke(world, body1, body2, axisX, axisY).asInstanceOf[Long]

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    hCreateFixedJoint.invoke(world, body1, body2).asInstanceOf[Long]

  override def destroyJoint(world: Long, joint: Long): Unit =
    hDestroyJoint.invoke(world, joint)

  // ─── Queries ──────────────────────────────────────────────────────────

  override def rayCast(
    world:   Long,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    out:     Array[Float]
  ): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      // out layout: [hitX, hitY, normalX, normalY, toi, bodyHandleLo, bodyHandleHi]
      val seg    = arena.allocateElems(p.JAVA_FLOAT, 7L)
      val hitInt = hRayCast.invoke(world, originX, originY, dirX, dirY, maxDist, seg).asInstanceOf[Int]
      val hit    = hitInt != 0
      if (hit) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, 7)
      }
      hit
    } finally arena.arenaClose()
  }

  override def queryPoint(world: Long, x: Float, y: Float, outBodies: Array[Long], maxResults: Int): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg   = arena.allocateElems(p.JAVA_LONG, maxResults.toLong)
      val count = hQueryPoint.invoke(world, x, y, seg, maxResults).asInstanceOf[Int]
      // Copy results back to the output array
      var i = 0
      while (i < count) {
        outBodies(i) = seg.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  // ─── Contact events ───────────────────────────────────────────────────

  override def pollContactStartEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg1  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val seg2  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val count = hPollContactStartEvents.invoke(world, seg1, seg2, maxEvents).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outCollider1(i) = seg1.getLong(i.toLong * 8L)
        outCollider2(i) = seg2.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  override def pollContactStopEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg1  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val seg2  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val count = hPollContactStopEvents.invoke(world, seg1, seg2, maxEvents).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outCollider1(i) = seg1.getLong(i.toLong * 8L)
        outCollider2(i) = seg2.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }
}
