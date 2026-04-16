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
    val found = multiarch.core.NativeLibLoader.load("sge_physics")
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
  private val hCreateSegmentCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_segment_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreatePolylineCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_polyline_collider"),
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

  // Collision filtering
  private val hColliderSetCollisionGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_collision_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )
  private val hColliderGetCollisionGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_collision_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderSetSolverGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_solver_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )
  private val hColliderGetSolverGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_solver_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
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
  private val hCreateRopeJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_rope_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hDestroyJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_destroy_joint"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Revolute joint limits/motors
  private val hRevoluteJointEnableLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_enable_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hRevoluteJointSetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_set_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hRevoluteJointGetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_get_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hRevoluteJointIsLimitEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_is_limit_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hRevoluteJointEnableMotor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_enable_motor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hRevoluteJointSetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_set_motor_speed"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hRevoluteJointSetMaxMotorTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_set_max_motor_torque"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hRevoluteJointGetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_get_motor_speed"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hRevoluteJointGetAngle: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_get_angle"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Prismatic joint limits/motors
  private val hPrismaticJointEnableLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_enable_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hPrismaticJointSetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_set_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hPrismaticJointGetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_get_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hPrismaticJointEnableMotor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_enable_motor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hPrismaticJointSetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_set_motor_speed"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hPrismaticJointSetMaxMotorForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_set_max_motor_force"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hPrismaticJointGetTranslation: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_get_translation"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Motor joint
  private val hCreateMotorJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_motor_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hMotorJointSetLinearOffset: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_set_linear_offset"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hMotorJointGetLinearOffset: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_get_linear_offset"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hMotorJointSetAngularOffset: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_set_angular_offset"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hMotorJointGetAngularOffset: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_get_angular_offset"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hMotorJointSetMaxForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_set_max_force"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hMotorJointSetMaxTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_set_max_torque"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hMotorJointSetCorrectionFactor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_set_correction_factor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )

  // Rope joint
  private val hRopeJointSetMaxDistance: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_rope_joint_set_max_distance"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hRopeJointGetMaxDistance: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_rope_joint_get_max_distance"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Body mass/inertia
  private val hBodyGetMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_mass"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetInertia: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_inertia"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetLocalCenterOfMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_local_center_of_mass"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyRecomputeMassProperties: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_recompute_mass_properties"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )

  // Queries
  private val hQueryAABB: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_query_aabb"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS, p.JAVA_INT)
  )
  private val hRayCast: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_ray_cast"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
  )
  private val hQueryPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_query_point"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS, p.JAVA_INT)
  )

  // Contact detail queries
  private val hContactPairCount: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_contact_pair_count"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hContactPairPoints: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_contact_pair_points"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT)
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

  override def createSegmentCollider(world: Long, body: Long, x1: Float, y1: Float, x2: Float, y2: Float): Long =
    hCreateSegmentCollider.invoke(world, body, x1, y1, x2, y2).asInstanceOf[Long]

  override def createPolylineCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, vertices.length.toLong)
      p.MemorySegment.copyFromFloats(vertices, 0, seg, 0L, vertices.length)
      hCreatePolylineCollider.invoke(world, body, seg, vertexCount).asInstanceOf[Long]
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

  // ─── Collision filtering ──────────────────────────────────────────────

  override def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    hColliderSetCollisionGroups.invoke(world, collider, memberships, filter)

  override def colliderGetCollisionGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_INT, 2L)
      hColliderGetCollisionGroups.invoke(world, collider, seg)
      out(0) = seg.getInt(0L)
      out(1) = seg.getInt(4L)
    } finally arena.arenaClose()
  }

  override def colliderSetSolverGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    hColliderSetSolverGroups.invoke(world, collider, memberships, filter)

  override def colliderGetSolverGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_INT, 2L)
      hColliderGetSolverGroups.invoke(world, collider, seg)
      out(0) = seg.getInt(0L)
      out(1) = seg.getInt(4L)
    } finally arena.arenaClose()
  }

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long =
    hCreateRevoluteJoint.invoke(world, body1, body2, anchorX, anchorY).asInstanceOf[Long]

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long =
    hCreatePrismaticJoint.invoke(world, body1, body2, axisX, axisY).asInstanceOf[Long]

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    hCreateFixedJoint.invoke(world, body1, body2).asInstanceOf[Long]

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long =
    hCreateRopeJoint.invoke(world, body1, body2, maxDist).asInstanceOf[Long]

  override def destroyJoint(world: Long, joint: Long): Unit =
    hDestroyJoint.invoke(world, joint)

  // ─── Revolute joint limits and motors ─────────────────────────────────

  override def revoluteJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit =
    hRevoluteJointEnableLimits.invoke(world, joint, if (enable) 1 else 0)

  override def revoluteJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit =
    hRevoluteJointSetLimits.invoke(world, joint, lower, upper)

  override def revoluteJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hRevoluteJointGetLimits.invoke(world, joint, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def revoluteJointIsLimitEnabled(world: Long, joint: Long): Boolean = {
    val result = hRevoluteJointIsLimitEnabled.invoke(world, joint).asInstanceOf[Int]
    result != 0
  }

  override def revoluteJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit =
    hRevoluteJointEnableMotor.invoke(world, joint, if (enable) 1 else 0)

  override def revoluteJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit =
    hRevoluteJointSetMotorSpeed.invoke(world, joint, speed)

  override def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float): Unit =
    hRevoluteJointSetMaxMotorTorque.invoke(world, joint, torque)

  override def revoluteJointGetMotorSpeed(world: Long, joint: Long): Float =
    hRevoluteJointGetMotorSpeed.invoke(world, joint).asInstanceOf[Float]

  override def revoluteJointGetAngle(world: Long, joint: Long): Float =
    hRevoluteJointGetAngle.invoke(world, joint).asInstanceOf[Float]

  // ─── Prismatic joint limits and motors ────────────────────────────────

  override def prismaticJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit =
    hPrismaticJointEnableLimits.invoke(world, joint, if (enable) 1 else 0)

  override def prismaticJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit =
    hPrismaticJointSetLimits.invoke(world, joint, lower, upper)

  override def prismaticJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hPrismaticJointGetLimits.invoke(world, joint, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def prismaticJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit =
    hPrismaticJointEnableMotor.invoke(world, joint, if (enable) 1 else 0)

  override def prismaticJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit =
    hPrismaticJointSetMotorSpeed.invoke(world, joint, speed)

  override def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force: Float): Unit =
    hPrismaticJointSetMaxMotorForce.invoke(world, joint, force)

  override def prismaticJointGetTranslation(world: Long, joint: Long): Float =
    hPrismaticJointGetTranslation.invoke(world, joint).asInstanceOf[Float]

  // ─── Motor joint ───────────────────────────────────────────────────────

  override def createMotorJoint(world: Long, body1: Long, body2: Long): Long =
    hCreateMotorJoint.invoke(world, body1, body2).asInstanceOf[Long]

  override def motorJointSetLinearOffset(world: Long, joint: Long, x: Float, y: Float): Unit =
    hMotorJointSetLinearOffset.invoke(world, joint, x, y)

  override def motorJointGetLinearOffset(world: Long, joint: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hMotorJointGetLinearOffset.invoke(world, joint, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def motorJointSetAngularOffset(world: Long, joint: Long, angle: Float): Unit =
    hMotorJointSetAngularOffset.invoke(world, joint, angle)

  override def motorJointGetAngularOffset(world: Long, joint: Long): Float =
    hMotorJointGetAngularOffset.invoke(world, joint).asInstanceOf[Float]

  override def motorJointSetMaxForce(world: Long, joint: Long, force: Float): Unit =
    hMotorJointSetMaxForce.invoke(world, joint, force)

  override def motorJointSetMaxTorque(world: Long, joint: Long, torque: Float): Unit =
    hMotorJointSetMaxTorque.invoke(world, joint, torque)

  override def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float): Unit =
    hMotorJointSetCorrectionFactor.invoke(world, joint, factor)

  // ─── Rope joint ───────────────────────────────────────────────────────

  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit =
    hRopeJointSetMaxDistance.invoke(world, joint, maxDist)

  override def ropeJointGetMaxDistance(world: Long, joint: Long): Float =
    hRopeJointGetMaxDistance.invoke(world, joint).asInstanceOf[Float]

  // ─── Body mass/inertia ────────────────────────────────────────────────

  override def bodyGetMass(world: Long, body: Long): Float =
    hBodyGetMass.invoke(world, body).asInstanceOf[Float]

  override def bodyGetInertia(world: Long, body: Long): Float =
    hBodyGetInertia.invoke(world, body).asInstanceOf[Float]

  override def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hBodyGetLocalCenterOfMass.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def bodyRecomputeMassProperties(world: Long, body: Long): Unit =
    hBodyRecomputeMassProperties.invoke(world, body)

  // ─── Queries ──────────────────────────────────────────────────────────

  override def queryAABB(
    world:        Long,
    minX:         Float,
    minY:         Float,
    maxX:         Float,
    maxY:         Float,
    outColliders: Array[Long],
    maxResults:   Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg   = arena.allocateElems(p.JAVA_LONG, maxResults.toLong)
      val count = hQueryAABB.invoke(world, minX, minY, maxX, maxY, seg, maxResults).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outColliders(i) = seg.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

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

  // ─── Contact detail queries ───────────────────────────────────────────

  override def contactPairCount(world: Long, collider1: Long, collider2: Long): Int =
    hContactPairCount.invoke(world, collider1, collider2).asInstanceOf[Int]

  override def contactPairPoints(
    world:     Long,
    collider1: Long,
    collider2: Long,
    out:       Array[Float],
    maxPoints: Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val totalFloats = maxPoints.toLong * 5L
      val seg         = arena.allocateElems(p.JAVA_FLOAT, totalFloats)
      val count       = hContactPairPoints.invoke(world, collider1, collider2, seg, maxPoints).asInstanceOf[Int]
      if (count > 0) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, count * 5)
      }
      count
    } finally arena.arenaClose()
  }
}
