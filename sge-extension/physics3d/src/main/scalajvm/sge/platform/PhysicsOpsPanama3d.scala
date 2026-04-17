/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: Panama FFM delegation to Rust C ABI native lib
 *   Idiom: split packages
 */
package sge
package platform

import java.lang.invoke.MethodHandle

/** JVM implementation of [[PhysicsOps3d]] using Panama Foreign Function & Memory API.
  *
  * Downcall handles invoke `sge_phys3d_*` C functions exported by the Rust `sge_physics3d` native library.
  */
private[platform] class PhysicsOpsPanama3d(val p: PanamaProvider) extends PhysicsOps3d {
  import p.*

  // ─── Native library + linker setup ─────────────────────────────────────

  private val linker: p.Linker = p.Linker.nativeLinker()

  private val lib: p.SymbolLookup = {
    val found = multiarch.core.NativeLibLoader.load("sge_physics3d")
    p.SymbolLookup.libraryLookup(found, p.Arena.global())
  }

  private def lookup(name: String): p.MemorySegment =
    lib.findOrThrow(name)

  // ─── Method handle cache ──────────────────────────────────────────────

  // World lifecycle
  private val hCreateWorld: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_world"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hDestroyWorld: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_destroy_world"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG)
  )
  private val hWorldStep: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_step"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hWorldSetGravity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_set_gravity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hWorldGetGravity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_get_gravity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.ADDRESS)
  )

  // Rigid body creation — 7 float params: x, y, z, qx, qy, qz, qw
  private val hCreateDynamicBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_dynamic_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateStaticBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_static_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateKinematicBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_kinematic_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hDestroyBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_destroy_body"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Body getters
  private val hBodyGetPosition: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_position"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetRotation: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_rotation"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetLinearVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_linear_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetAngularVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_angular_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )

  // Body setters
  private val hBodySetPosition: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_position"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodySetRotation: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_rotation"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodySetLinearVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_linear_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodySetAngularVelocity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_angular_velocity"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )

  // Body forces
  private val hBodyApplyForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_apply_force"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyImpulse: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_apply_impulse"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_apply_torque"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyForceAtPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_apply_force_at_point"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyImpulseAtPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_apply_impulse_at_point"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )

  // Body properties
  private val hBodySetLinearDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_linear_damping"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodyGetLinearDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_linear_damping"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetAngularDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_angular_damping"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodyGetAngularDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_angular_damping"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetGravityScale: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_gravity_scale"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodyGetGravityScale: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_gravity_scale"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsAwake: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_awake"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyWakeUp: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_wake_up"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySleep: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_sleep"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetFixedRotation: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_fixed_rotation"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyEnableCcd: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_enable_ccd"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyIsCcdEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_ccd_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_enabled"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyIsEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetDominanceGroup: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_dominance_group"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyGetDominanceGroup: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_dominance_group"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_mass"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyRecomputeMassProperties: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_recompute_mass_properties"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Collider creation
  private val hCreateSphereCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_sphere_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hCreateBoxCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_box_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateCapsuleCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_capsule_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateCylinderCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_cylinder_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateConeCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_cone_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateConvexHullCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_convex_hull_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT)
  )
  private val hCreateTriMeshCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_trimesh_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )
  private val hDestroyCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_destroy_collider"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Collider properties
  private val hColliderSetDensity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_density"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetFriction: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_friction"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetRestitution: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_restitution"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetSensor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_sensor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )

  // Joints
  private val hCreateFixedJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_fixed_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hCreateRopeJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_rope_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hDestroyJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_destroy_joint"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Queries
  private val hRayCast: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_ray_cast"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
  )

  // Contact events
  private val hPollContactStartEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_poll_contact_start_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )
  private val hPollContactStopEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_poll_contact_stop_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )

  // ─── World lifecycle ──────────────────────────────────────────────────

  override def createWorld(gravityX: Float, gravityY: Float, gravityZ: Float): Long =
    hCreateWorld.invoke(gravityX, gravityY, gravityZ).asInstanceOf[Long]

  override def destroyWorld(world: Long): Unit =
    hDestroyWorld.invoke(world)

  override def worldStep(world: Long, dt: Float): Unit =
    hWorldStep.invoke(world, dt)

  override def worldSetGravity(world: Long, gx: Float, gy: Float, gz: Float): Unit =
    hWorldSetGravity.invoke(world, gx, gy, gz)

  override def worldGetGravity(world: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hWorldGetGravity.invoke(world, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  // ─── Rigid body ───────────────────────────────────────────────────────

  override def createDynamicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long =
    hCreateDynamicBody.invoke(world, x, y, z, qx, qy, qz, qw).asInstanceOf[Long]

  override def createStaticBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long =
    hCreateStaticBody.invoke(world, x, y, z, qx, qy, qz, qw).asInstanceOf[Long]

  override def createKinematicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long =
    hCreateKinematicBody.invoke(world, x, y, z, qx, qy, qz, qw).asInstanceOf[Long]

  override def destroyBody(world: Long, body: Long): Unit =
    hDestroyBody.invoke(world, body)

  override def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hBodyGetPosition.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def bodyGetRotation(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 4L)
      hBodyGetRotation.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 4)
    } finally arena.arenaClose()
  }

  override def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hBodyGetLinearVelocity.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def bodyGetAngularVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hBodyGetAngularVelocity.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def bodySetPosition(world: Long, body: Long, x: Float, y: Float, z: Float): Unit =
    hBodySetPosition.invoke(world, body, x, y, z)

  override def bodySetRotation(world: Long, body: Long, qx: Float, qy: Float, qz: Float, qw: Float): Unit =
    hBodySetRotation.invoke(world, body, qx, qy, qz, qw)

  override def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float, vz: Float): Unit =
    hBodySetLinearVelocity.invoke(world, body, vx, vy, vz)

  override def bodySetAngularVelocity(world: Long, body: Long, wx: Float, wy: Float, wz: Float): Unit =
    hBodySetAngularVelocity.invoke(world, body, wx, wy, wz)

  override def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float, fz: Float): Unit =
    hBodyApplyForce.invoke(world, body, fx, fy, fz)

  override def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float, iz: Float): Unit =
    hBodyApplyImpulse.invoke(world, body, ix, iy, iz)

  override def bodyApplyTorque(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit =
    hBodyApplyTorque.invoke(world, body, tx, ty, tz)

  override def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, fz: Float, px: Float, py: Float, pz: Float): Unit =
    hBodyApplyForceAtPoint.invoke(world, body, fx, fy, fz, px, py, pz)

  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, iz: Float, px: Float, py: Float, pz: Float): Unit =
    hBodyApplyImpulseAtPoint.invoke(world, body, ix, iy, iz, px, py, pz)

  override def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit =
    hBodySetLinearDamping.invoke(world, body, damping)

  override def bodyGetLinearDamping(world: Long, body: Long): Float =
    hBodyGetLinearDamping.invoke(world, body).asInstanceOf[Float]

  override def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit =
    hBodySetAngularDamping.invoke(world, body, damping)

  override def bodyGetAngularDamping(world: Long, body: Long): Float =
    hBodyGetAngularDamping.invoke(world, body).asInstanceOf[Float]

  override def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit =
    hBodySetGravityScale.invoke(world, body, scale)

  override def bodyGetGravityScale(world: Long, body: Long): Float =
    hBodyGetGravityScale.invoke(world, body).asInstanceOf[Float]

  override def bodyIsAwake(world: Long, body: Long): Boolean = {
    val result = hBodyIsAwake.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyWakeUp(world: Long, body: Long): Unit =
    hBodyWakeUp.invoke(world, body)

  override def bodySleep(world: Long, body: Long): Unit =
    hBodySleep.invoke(world, body)

  override def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit =
    hBodySetFixedRotation.invoke(world, body, if (fixed) 1 else 0)

  override def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit =
    hBodyEnableCcd.invoke(world, body, if (enable) 1 else 0)

  override def bodyIsCcdEnabled(world: Long, body: Long): Boolean = {
    val result = hBodyIsCcdEnabled.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit =
    hBodySetEnabled.invoke(world, body, if (enabled) 1 else 0)

  override def bodyIsEnabled(world: Long, body: Long): Boolean = {
    val result = hBodyIsEnabled.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit =
    hBodySetDominanceGroup.invoke(world, body, group)

  override def bodyGetDominanceGroup(world: Long, body: Long): Int =
    hBodyGetDominanceGroup.invoke(world, body).asInstanceOf[Int]

  override def bodyGetMass(world: Long, body: Long): Float =
    hBodyGetMass.invoke(world, body).asInstanceOf[Float]

  override def bodyRecomputeMassProperties(world: Long, body: Long): Unit =
    hBodyRecomputeMassProperties.invoke(world, body)

  // ─── Collider ─────────────────────────────────────────────────────────

  override def createSphereCollider(world: Long, body: Long, radius: Float): Long =
    hCreateSphereCollider.invoke(world, body, radius).asInstanceOf[Long]

  override def createBoxCollider(world: Long, body: Long, hx: Float, hy: Float, hz: Float): Long =
    hCreateBoxCollider.invoke(world, body, hx, hy, hz).asInstanceOf[Long]

  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    hCreateCapsuleCollider.invoke(world, body, halfHeight, radius).asInstanceOf[Long]

  override def createCylinderCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    hCreateCylinderCollider.invoke(world, body, halfHeight, radius).asInstanceOf[Long]

  override def createConeCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    hCreateConeCollider.invoke(world, body, halfHeight, radius).asInstanceOf[Long]

  override def createConvexHullCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, vertices.length.toLong)
      p.MemorySegment.copyFromFloats(vertices, 0, seg, 0L, vertices.length)
      hCreateConvexHullCollider.invoke(world, body, seg, vertexCount).asInstanceOf[Long]
    } finally arena.arenaClose()
  }

  override def createTriMeshCollider(
    world:       Long,
    body:        Long,
    vertices:    Array[Float],
    vertexCount: Int,
    indices:     Array[Int],
    indexCount:  Int
  ): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val vertSeg = arena.allocateElems(p.JAVA_FLOAT, vertices.length.toLong)
      p.MemorySegment.copyFromFloats(vertices, 0, vertSeg, 0L, vertices.length)
      val idxSeg = arena.allocateElems(p.JAVA_INT, indices.length.toLong)
      var i      = 0
      while (i < indices.length) {
        idxSeg.setInt(i.toLong * 4L, indices(i))
        i += 1
      }
      hCreateTriMeshCollider.invoke(world, body, vertSeg, vertexCount, idxSeg, indexCount).asInstanceOf[Long]
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

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    hCreateFixedJoint.invoke(world, body1, body2).asInstanceOf[Long]

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long =
    hCreateRopeJoint.invoke(world, body1, body2, maxDist).asInstanceOf[Long]

  override def destroyJoint(world: Long, joint: Long): Unit =
    hDestroyJoint.invoke(world, joint)

  // ─── Queries ──────────────────────────────────────────────────────────

  override def rayCast(
    world:   Long,
    originX: Float,
    originY: Float,
    originZ: Float,
    dirX:    Float,
    dirY:    Float,
    dirZ:    Float,
    maxDist: Float,
    out:     Array[Float]
  ): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      // out layout: [hitX, hitY, hitZ, normalX, normalY, normalZ, toi, colliderHandleLo, colliderHandleHi]
      val seg    = arena.allocateElems(p.JAVA_FLOAT, 9L)
      val hitInt = hRayCast.invoke(world, originX, originY, originZ, dirX, dirY, dirZ, maxDist, seg).asInstanceOf[Int]
      val hit    = hitInt != 0
      if (hit) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, 9)
      }
      hit
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
