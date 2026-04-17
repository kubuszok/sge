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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1598
 * Covenant-baseline-methods: PhysicsOpsPanama,arena,bodyApplyForce,bodyApplyForceAtPoint,bodyApplyImpulse,bodyApplyImpulseAtPoint,bodyApplyTorque,bodyApplyTorqueImpulse,bodyEnableCcd,bodyGetAngle,bodyGetAngularDamping,bodyGetAngularVelocity,bodyGetDominanceGroup,bodyGetGravityScale,bodyGetInertia,bodyGetLinearDamping,bodyGetLinearVelocity,bodyGetLocalCenterOfMass,bodyGetMass,bodyGetPosition,bodyGetType,bodyGetVelocityAtPoint,bodyGetWorldCenterOfMass,bodyIsAwake,bodyIsCcdEnabled,bodyIsEnabled,bodyIsRotationLocked,bodyIsTranslationLockedX,bodyIsTranslationLockedY,bodyRecomputeMassProperties,bodyResetForces,bodyResetTorques,bodySetAngle,bodySetAngularDamping,bodySetAngularVelocity,bodySetDominanceGroup,bodySetEnabled,bodySetEnabledTranslations,bodySetFixedRotation,bodySetGravityScale,bodySetLinearDamping,bodySetLinearVelocity,bodySetPosition,bodySleep,bodyWakeUp,castShape,colliderGetAabb,colliderGetActiveCollisionTypes,colliderGetActiveEvents,colliderGetActiveHooks,colliderGetCollisionGroups,colliderGetContactForceEventThreshold,colliderGetDensity,colliderGetFriction,colliderGetMass,colliderGetOneWayDirection,colliderGetParentBody,colliderGetPosition,colliderGetPositionWrtParent,colliderGetRestitution,colliderGetShapeType,colliderGetSolverGroups,colliderIsEnabled,colliderIsSensor,colliderSetActiveCollisionTypes,colliderSetActiveEvents,colliderSetActiveHooks,colliderSetCollisionGroups,colliderSetContactForceEventThreshold,colliderSetContactSkin,colliderSetDensity,colliderSetEnabled,colliderSetFriction,colliderSetMass,colliderSetOneWayDirection,colliderSetPositionWrtParent,colliderSetRestitution,colliderSetSensor,colliderSetSolverGroups,contactPairCount,contactPairPoints,createBoxCollider,createCapsuleCollider,createCircleCollider,createDynamicBody,createFixedJoint,createHeightfieldCollider,createKinematicBody,createMotorJoint,createPolygonCollider,createPolylineCollider,createPrismaticJoint,createRevoluteJoint,createRopeJoint,createSegmentCollider,createSpringJoint,createStaticBody,createTriMeshCollider,createWorld,destroyBody,destroyCollider,destroyJoint,destroyWorld,found,hBodyApplyForce,hBodyApplyForceAtPoint,hBodyApplyImpulse,hBodyApplyImpulseAtPoint,hBodyApplyTorque,hBodyApplyTorqueImpulse,hBodyEnableCcd,hBodyGetAngle,hBodyGetAngularDamping,hBodyGetAngularVelocity,hBodyGetDominanceGroup,hBodyGetGravityScale,hBodyGetInertia,hBodyGetLinearDamping,hBodyGetLinearVelocity,hBodyGetLocalCenterOfMass,hBodyGetMass,hBodyGetPosition,hBodyGetType,hBodyGetVelocityAtPoint,hBodyGetWorldCenterOfMass,hBodyIsAwake,hBodyIsCcdEnabled,hBodyIsEnabled,hBodyIsRotationLocked,hBodyIsTranslationLockedX,hBodyIsTranslationLockedY,hBodyRecomputeMassProperties,hBodyResetForces,hBodyResetTorques,hBodySetAngle,hBodySetAngularDamping,hBodySetAngularVelocity,hBodySetDominanceGroup,hBodySetEnabled,hBodySetEnabledTranslations,hBodySetFixedRotation,hBodySetGravityScale,hBodySetLinearDamping,hBodySetLinearVelocity,hBodySetPosition,hBodySleep,hBodyWakeUp,hCastShape,hColliderGetAabb,hColliderGetActiveCollisionTypes,hColliderGetActiveEvents,hColliderGetActiveHooks,hColliderGetCollisionGroups,hColliderGetContactForceEventThreshold,hColliderGetDensity,hColliderGetFriction,hColliderGetMass,hColliderGetOneWayDirection,hColliderGetParentBody,hColliderGetPosition,hColliderGetPositionWrtParent,hColliderGetRestitution,hColliderGetShapeType,hColliderGetSolverGroups,hColliderIsEnabled,hColliderIsSensor,hColliderSetActiveCollisionTypes,hColliderSetActiveEvents,hColliderSetActiveHooks,hColliderSetCollisionGroups,hColliderSetContactForceEventThreshold,hColliderSetContactSkin,hColliderSetDensity,hColliderSetEnabled,hColliderSetFriction,hColliderSetMass,hColliderSetOneWayDirection,hColliderSetPositionWrtParent,hColliderSetRestitution,hColliderSetSensor,hColliderSetSolverGroups,hContactPairCount,hContactPairPoints,hCreateBoxCollider,hCreateCapsuleCollider,hCreateCircleCollider,hCreateDynamicBody,hCreateFixedJoint,hCreateHeightfieldCollider,hCreateKinematicBody,hCreateMotorJoint,hCreatePolygonCollider,hCreatePolylineCollider,hCreatePrismaticJoint,hCreateRevoluteJoint,hCreateRopeJoint,hCreateSegmentCollider,hCreateSpringJoint,hCreateStaticBody,hCreateTriMeshCollider,hCreateWorld,hDestroyBody,hDestroyCollider,hDestroyJoint,hDestroyWorld,hIntersectShape,hMotorJointGetAngularOffset,hMotorJointGetCorrectionFactor,hMotorJointGetLinearOffset,hMotorJointGetMaxForce,hMotorJointGetMaxTorque,hMotorJointSetAngularOffset,hMotorJointSetCorrectionFactor,hMotorJointSetLinearOffset,hMotorJointSetMaxForce,hMotorJointSetMaxTorque,hPollContactForceEvents,hPollContactStartEvents,hPollContactStopEvents,hPollIntersectionStartEvents,hPollIntersectionStopEvents,hPrismaticJointEnableLimits,hPrismaticJointEnableMotor,hPrismaticJointGetLimits,hPrismaticJointGetMaxMotorForce,hPrismaticJointGetMotorSpeed,hPrismaticJointGetTranslation,hPrismaticJointSetLimits,hPrismaticJointSetMaxMotorForce,hPrismaticJointSetMotorSpeed,hProjectPoint,hQueryAABB,hQueryPoint,hRayCast,hRayCastAll,hRevoluteJointEnableLimits,hRevoluteJointEnableMotor,hRevoluteJointGetAngle,hRevoluteJointGetLimits,hRevoluteJointGetMaxMotorTorque,hRevoluteJointGetMotorSpeed,hRevoluteJointIsLimitEnabled,hRevoluteJointSetLimits,hRevoluteJointSetMaxMotorTorque,hRevoluteJointSetMotorSpeed,hRopeJointGetMaxDistance,hRopeJointSetMaxDistance,hSpringJointGetRestLength,hSpringJointSetParams,hSpringJointSetRestLength,hWorldGetGravity,hWorldGetNumSolverIterations,hWorldSetGravity,hWorldSetNumAdditionalFrictionIterations,hWorldSetNumInternalPgsIterations,hWorldSetNumSolverIterations,hWorldStep,intersectShape,lib,linker,lookup,motorJointGetAngularOffset,motorJointGetCorrectionFactor,motorJointGetLinearOffset,motorJointGetMaxForce,motorJointGetMaxTorque,motorJointSetAngularOffset,motorJointSetCorrectionFactor,motorJointSetLinearOffset,motorJointSetMaxForce,motorJointSetMaxTorque,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,prismaticJointEnableLimits,prismaticJointEnableMotor,prismaticJointGetLimits,prismaticJointGetMaxMotorForce,prismaticJointGetMotorSpeed,prismaticJointGetTranslation,prismaticJointSetLimits,prismaticJointSetMaxMotorForce,prismaticJointSetMotorSpeed,projectPoint,queryAABB,queryPoint,rayCast,rayCastAll,result,revoluteJointEnableLimits,revoluteJointEnableMotor,revoluteJointGetAngle,revoluteJointGetLimits,revoluteJointGetMaxMotorTorque,revoluteJointGetMotorSpeed,revoluteJointIsLimitEnabled,revoluteJointSetLimits,revoluteJointSetMaxMotorTorque,revoluteJointSetMotorSpeed,ropeJointGetMaxDistance,ropeJointSetMaxDistance,springJointGetRestLength,springJointSetParams,springJointSetRestLength,worldGetGravity,worldGetNumSolverIterations,worldSetGravity,worldSetNumAdditionalFrictionIterations,worldSetNumInternalPgsIterations,worldSetNumSolverIterations,worldStep
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
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
  private val hBodyApplyForceAtPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_apply_force_at_point"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyImpulseAtPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_apply_impulse_at_point"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyApplyTorqueImpulse: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_apply_torque_impulse"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hBodyResetForces: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_reset_forces"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyResetTorques: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_reset_torques"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
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
  private val hBodyGetLinearDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_linear_damping"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetAngularDamping: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_angular_damping"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetGravityScale: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_gravity_scale"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetType: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_type"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_enabled"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyIsEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_is_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetEnabledTranslations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_enabled_translations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )
  private val hBodyIsTranslationLockedX: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_is_translation_locked_x"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsTranslationLockedY: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_is_translation_locked_y"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsRotationLocked: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_is_rotation_locked"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetDominanceGroup: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_set_dominance_group"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyGetDominanceGroup: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_dominance_group"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetWorldCenterOfMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_world_center_of_mass"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyEnableCcd: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_enable_ccd"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hBodyIsCcdEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_is_ccd_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySleep: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_sleep"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetVelocityAtPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_body_get_velocity_at_point"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
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
  private val hCreateTriMeshCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_trimesh_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )
  private val hCreateHeightfieldCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_heightfield_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.JAVA_FLOAT, p.JAVA_FLOAT)
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
  private val hColliderGetDensity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_density"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetFriction: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_friction"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetRestitution: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_restitution"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderIsSensor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_is_sensor"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_enabled"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderIsEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_is_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetPositionWrtParent: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_position_wrt_parent"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderSetPositionWrtParent: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_position_wrt_parent"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hColliderGetPosition: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_position"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderGetShapeType: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_shape_type"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetAabb: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_aabb"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderGetParentBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_parent_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_mass"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_mass"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetContactSkin: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_contact_skin"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetActiveEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_active_events"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderGetActiveEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_active_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetActiveCollisionTypes: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_active_collision_types"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderGetActiveCollisionTypes: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_active_collision_types"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
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
  private val hRevoluteJointGetMaxMotorTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_revolute_joint_get_max_motor_torque"),
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
  private val hPrismaticJointGetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_get_motor_speed"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hPrismaticJointGetMaxMotorForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_prismatic_joint_get_max_motor_force"),
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
  private val hMotorJointGetMaxForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_get_max_force"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hMotorJointGetMaxTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_get_max_torque"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hMotorJointGetCorrectionFactor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_motor_joint_get_correction_factor"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
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

  // Spring joint
  private val hCreateSpringJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_create_spring_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hSpringJointSetRestLength: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_spring_joint_set_rest_length"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hSpringJointGetRestLength: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_spring_joint_get_rest_length"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hSpringJointSetParams: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_spring_joint_set_params"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
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

  // Advanced queries
  private val hCastShape: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_cast_shape"),
    p.FunctionDescriptor.of(
      p.JAVA_INT,
      p.JAVA_LONG,
      p.JAVA_INT,
      p.ADDRESS,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.ADDRESS
    )
  )
  private val hRayCastAll: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_ray_cast_all"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS, p.JAVA_INT)
  )
  private val hProjectPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_project_point"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
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

  // Intersection events
  private val hPollIntersectionStartEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_poll_intersection_start_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )
  private val hPollIntersectionStopEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_poll_intersection_stop_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )

  // Solver parameters
  private val hWorldSetNumSolverIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_set_num_solver_iterations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT)
  )
  private val hWorldGetNumSolverIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_get_num_solver_iterations"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG)
  )
  private val hWorldSetNumAdditionalFrictionIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_set_num_additional_friction_iterations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT)
  )
  private val hWorldSetNumInternalPgsIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_world_set_num_internal_pgs_iterations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT)
  )

  // Shape intersection
  private val hIntersectShape: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_intersect_shape"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_INT, p.ADDRESS, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS, p.JAVA_INT)
  )

  // Contact force events
  private val hPollContactForceEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_poll_contact_force_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )
  private val hColliderSetContactForceEventThreshold: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_contact_force_event_threshold"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderGetContactForceEventThreshold: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_contact_force_event_threshold"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Active hooks / one-way direction
  private val hColliderSetActiveHooks: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_active_hooks"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderGetActiveHooks: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_active_hooks"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetOneWayDirection: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_set_one_way_direction"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hColliderGetOneWayDirection: MethodHandle = linker.downcallHandle(
    lookup("sge_phys_collider_get_one_way_direction"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
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

  override def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, px: Float, py: Float): Unit =
    hBodyApplyForceAtPoint.invoke(world, body, fx, fy, px, py)

  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, px: Float, py: Float): Unit =
    hBodyApplyImpulseAtPoint.invoke(world, body, ix, iy, px, py)

  override def bodyApplyTorqueImpulse(world: Long, body: Long, impulse: Float): Unit =
    hBodyApplyTorqueImpulse.invoke(world, body, impulse)

  override def bodyResetForces(world: Long, body: Long): Unit =
    hBodyResetForces.invoke(world, body)

  override def bodyResetTorques(world: Long, body: Long): Unit =
    hBodyResetTorques.invoke(world, body)

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

  override def bodyGetLinearDamping(world: Long, body: Long): Float =
    hBodyGetLinearDamping.invoke(world, body).asInstanceOf[Float]

  override def bodyGetAngularDamping(world: Long, body: Long): Float =
    hBodyGetAngularDamping.invoke(world, body).asInstanceOf[Float]

  override def bodyGetGravityScale(world: Long, body: Long): Float =
    hBodyGetGravityScale.invoke(world, body).asInstanceOf[Float]

  override def bodyGetType(world: Long, body: Long): Int =
    hBodyGetType.invoke(world, body).asInstanceOf[Int]

  override def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit =
    hBodySetEnabled.invoke(world, body, if (enabled) 1 else 0)

  override def bodyIsEnabled(world: Long, body: Long): Boolean = {
    val result = hBodyIsEnabled.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean): Unit =
    hBodySetEnabledTranslations.invoke(world, body, if (allowX) 1 else 0, if (allowY) 1 else 0)

  override def bodyIsTranslationLockedX(world: Long, body: Long): Boolean = {
    val result = hBodyIsTranslationLockedX.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyIsTranslationLockedY(world: Long, body: Long): Boolean = {
    val result = hBodyIsTranslationLockedY.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyIsRotationLocked(world: Long, body: Long): Boolean = {
    val result = hBodyIsRotationLocked.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit =
    hBodySetDominanceGroup.invoke(world, body, group)

  override def bodyGetDominanceGroup(world: Long, body: Long): Int =
    hBodyGetDominanceGroup.invoke(world, body).asInstanceOf[Int]

  override def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hBodyGetWorldCenterOfMass.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

  override def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit =
    hBodyEnableCcd.invoke(world, body, if (enable) 1 else 0)

  override def bodyIsCcdEnabled(world: Long, body: Long): Boolean = {
    val result = hBodyIsCcdEnabled.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodySleep(world: Long, body: Long): Unit =
    hBodySleep.invoke(world, body)

  override def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 2L)
      hBodyGetVelocityAtPoint.invoke(world, body, px, py, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 2)
    } finally arena.arenaClose()
  }

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

  override def createHeightfieldCollider(
    world:   Long,
    body:    Long,
    heights: Array[Float],
    numCols: Int,
    scaleX:  Float,
    scaleY:  Float
  ): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, heights.length.toLong)
      p.MemorySegment.copyFromFloats(heights, 0, seg, 0L, heights.length)
      hCreateHeightfieldCollider.invoke(world, body, seg, numCols, scaleX, scaleY).asInstanceOf[Long]
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

  override def colliderGetDensity(world: Long, collider: Long): Float =
    hColliderGetDensity.invoke(world, collider).asInstanceOf[Float]

  override def colliderGetFriction(world: Long, collider: Long): Float =
    hColliderGetFriction.invoke(world, collider).asInstanceOf[Float]

  override def colliderGetRestitution(world: Long, collider: Long): Float =
    hColliderGetRestitution.invoke(world, collider).asInstanceOf[Float]

  override def colliderIsSensor(world: Long, collider: Long): Boolean = {
    val result = hColliderIsSensor.invoke(world, collider).asInstanceOf[Int]
    result != 0
  }

  override def colliderSetEnabled(world: Long, collider: Long, enabled: Boolean): Unit =
    hColliderSetEnabled.invoke(world, collider, if (enabled) 1 else 0)

  override def colliderIsEnabled(world: Long, collider: Long): Boolean = {
    val result = hColliderIsEnabled.invoke(world, collider).asInstanceOf[Int]
    result != 0
  }

  override def colliderGetPositionWrtParent(world: Long, collider: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hColliderGetPositionWrtParent.invoke(world, collider, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, angle: Float): Unit =
    hColliderSetPositionWrtParent.invoke(world, collider, x, y, angle)

  override def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hColliderGetPosition.invoke(world, collider, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def colliderGetShapeType(world: Long, collider: Long): Int =
    hColliderGetShapeType.invoke(world, collider).asInstanceOf[Int]

  override def colliderGetAabb(world: Long, collider: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 4L)
      hColliderGetAabb.invoke(world, collider, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 4)
    } finally arena.arenaClose()
  }

  override def colliderGetParentBody(world: Long, collider: Long): Long =
    hColliderGetParentBody.invoke(world, collider).asInstanceOf[Long]

  override def colliderGetMass(world: Long, collider: Long): Float =
    hColliderGetMass.invoke(world, collider).asInstanceOf[Float]

  override def colliderSetMass(world: Long, collider: Long, mass: Float): Unit =
    hColliderSetMass.invoke(world, collider, mass)

  override def colliderSetContactSkin(world: Long, collider: Long, skin: Float): Unit =
    hColliderSetContactSkin.invoke(world, collider, skin)

  override def colliderSetActiveEvents(world: Long, collider: Long, flags: Int): Unit =
    hColliderSetActiveEvents.invoke(world, collider, flags)

  override def colliderGetActiveEvents(world: Long, collider: Long): Int =
    hColliderGetActiveEvents.invoke(world, collider).asInstanceOf[Int]

  override def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags: Int): Unit =
    hColliderSetActiveCollisionTypes.invoke(world, collider, flags)

  override def colliderGetActiveCollisionTypes(world: Long, collider: Long): Int =
    hColliderGetActiveCollisionTypes.invoke(world, collider).asInstanceOf[Int]

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

  override def revoluteJointGetMaxMotorTorque(world: Long, joint: Long): Float =
    hRevoluteJointGetMaxMotorTorque.invoke(world, joint).asInstanceOf[Float]

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

  override def prismaticJointGetMotorSpeed(world: Long, joint: Long): Float =
    hPrismaticJointGetMotorSpeed.invoke(world, joint).asInstanceOf[Float]

  override def prismaticJointGetMaxMotorForce(world: Long, joint: Long): Float =
    hPrismaticJointGetMaxMotorForce.invoke(world, joint).asInstanceOf[Float]

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

  override def motorJointGetMaxForce(world: Long, joint: Long): Float =
    hMotorJointGetMaxForce.invoke(world, joint).asInstanceOf[Float]

  override def motorJointGetMaxTorque(world: Long, joint: Long): Float =
    hMotorJointGetMaxTorque.invoke(world, joint).asInstanceOf[Float]

  override def motorJointGetCorrectionFactor(world: Long, joint: Long): Float =
    hMotorJointGetCorrectionFactor.invoke(world, joint).asInstanceOf[Float]

  // ─── Rope joint ───────────────────────────────────────────────────────

  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit =
    hRopeJointSetMaxDistance.invoke(world, joint, maxDist)

  override def ropeJointGetMaxDistance(world: Long, joint: Long): Float =
    hRopeJointGetMaxDistance.invoke(world, joint).asInstanceOf[Float]

  // ─── Spring joint ─────────────────────────────────────────────────────

  override def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long =
    hCreateSpringJoint.invoke(world, body1, body2, restLength, stiffness, damping).asInstanceOf[Long]

  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float): Unit =
    hSpringJointSetRestLength.invoke(world, joint, restLength)

  override def springJointGetRestLength(world: Long, joint: Long): Float =
    hSpringJointGetRestLength.invoke(world, joint).asInstanceOf[Float]

  override def springJointSetParams(world: Long, joint: Long, stiffness: Float, damping: Float): Unit =
    hSpringJointSetParams.invoke(world, joint, stiffness, damping)

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

  // ─── Advanced queries ─────────────────────────────────────────────────

  override def castShape(
    world:       Long,
    shapeType:   Int,
    shapeParams: Array[Float],
    originX:     Float,
    originY:     Float,
    dirX:        Float,
    dirY:        Float,
    maxDist:     Float,
    out:         Array[Float]
  ): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      val paramSeg = arena.allocateElems(p.JAVA_FLOAT, shapeParams.length.toLong)
      p.MemorySegment.copyFromFloats(shapeParams, 0, paramSeg, 0L, shapeParams.length)
      val outSeg = arena.allocateElems(p.JAVA_FLOAT, 7L)
      val hitInt = hCastShape.invoke(world, shapeType, paramSeg, originX, originY, dirX, dirY, maxDist, outSeg).asInstanceOf[Int]
      val hit    = hitInt != 0
      if (hit) {
        p.MemorySegment.copyToFloats(outSeg, 0L, out, 0, 7)
      }
      hit
    } finally arena.arenaClose()
  }

  override def rayCastAll(
    world:   Long,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    outHits: Array[Float],
    maxHits: Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val totalFloats = maxHits.toLong * 7L
      val seg         = arena.allocateElems(p.JAVA_FLOAT, totalFloats)
      val count       = hRayCastAll.invoke(world, originX, originY, dirX, dirY, maxDist, seg, maxHits).asInstanceOf[Int]
      if (count > 0) {
        p.MemorySegment.copyToFloats(seg, 0L, outHits, 0, count * 7)
      }
      count
    } finally arena.arenaClose()
  }

  override def projectPoint(world: Long, x: Float, y: Float, out: Array[Float]): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      val seg      = arena.allocateElems(p.JAVA_FLOAT, 5L)
      val foundInt = hProjectPoint.invoke(world, x, y, seg).asInstanceOf[Int]
      val found    = foundInt != 0
      if (found) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, 5)
      }
      found
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

  // ─── Intersection events ──────────────────────────────────────────────

  override def pollIntersectionStartEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg1  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val seg2  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val count = hPollIntersectionStartEvents.invoke(world, seg1, seg2, maxEvents).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outCollider1(i) = seg1.getLong(i.toLong * 8L)
        outCollider2(i) = seg2.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  override def pollIntersectionStopEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg1  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val seg2  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val count = hPollIntersectionStopEvents.invoke(world, seg1, seg2, maxEvents).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outCollider1(i) = seg1.getLong(i.toLong * 8L)
        outCollider2(i) = seg2.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  // ─── Solver parameters ────────────────────────────────────────────────

  override def worldSetNumSolverIterations(world: Long, iters: Int): Unit =
    hWorldSetNumSolverIterations.invoke(world, iters)

  override def worldGetNumSolverIterations(world: Long): Int =
    hWorldGetNumSolverIterations.invoke(world).asInstanceOf[Int]

  override def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit =
    hWorldSetNumAdditionalFrictionIterations.invoke(world, iters)

  override def worldSetNumInternalPgsIterations(world: Long, iters: Int): Unit =
    hWorldSetNumInternalPgsIterations.invoke(world, iters)

  // ─── Shape intersection ───────────────────────────────────────────────

  override def intersectShape(
    world:        Long,
    shapeType:    Int,
    shapeParams:  Array[Float],
    posX:         Float,
    posY:         Float,
    angle:        Float,
    outColliders: Array[Long],
    maxResults:   Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val paramSeg = arena.allocateElems(p.JAVA_FLOAT, shapeParams.length.toLong)
      p.MemorySegment.copyFromFloats(shapeParams, 0, paramSeg, 0L, shapeParams.length)
      val outSeg = arena.allocateElems(p.JAVA_LONG, maxResults.toLong)
      val count  = hIntersectShape.invoke(world, shapeType, paramSeg, posX, posY, angle, outSeg, maxResults).asInstanceOf[Int]
      var i      = 0
      while (i < count) {
        outColliders(i) = outSeg.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  // ─── Contact force events ─────────────────────────────────────────────

  override def pollContactForceEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    outForce:     Array[Float],
    maxEvents:    Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg1  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val seg2  = arena.allocateElems(p.JAVA_LONG, maxEvents.toLong)
      val segF  = arena.allocateElems(p.JAVA_FLOAT, maxEvents.toLong)
      val count = hPollContactForceEvents.invoke(world, seg1, seg2, segF, maxEvents).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outCollider1(i) = seg1.getLong(i.toLong * 8L)
        outCollider2(i) = seg2.getLong(i.toLong * 8L)
        i += 1
      }
      if (count > 0) p.MemorySegment.copyToFloats(segF, 0L, outForce, 0, count)
      count
    } finally arena.arenaClose()
  }

  override def colliderSetContactForceEventThreshold(world: Long, collider: Long, threshold: Float): Unit =
    hColliderSetContactForceEventThreshold.invoke(world, collider, threshold)

  override def colliderGetContactForceEventThreshold(world: Long, collider: Long): Float =
    hColliderGetContactForceEventThreshold.invoke(world, collider).asInstanceOf[Float]

  // ─── Active hooks / one-way direction ─────────────────────────────────

  override def colliderSetActiveHooks(world: Long, collider: Long, flags: Int): Unit =
    hColliderSetActiveHooks.invoke(world, collider, flags)

  override def colliderGetActiveHooks(world: Long, collider: Long): Int =
    hColliderGetActiveHooks.invoke(world, collider).asInstanceOf[Int]

  override def colliderSetOneWayDirection(world: Long, collider: Long, nx: Float, ny: Float, allowedAngle: Float): Unit =
    hColliderSetOneWayDirection.invoke(world, collider, nx, ny, allowedAngle)

  override def colliderGetOneWayDirection(world: Long, collider: Long, out: Array[Float]): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      val seg    = arena.allocateElems(p.JAVA_FLOAT, 3L)
      val result = hColliderGetOneWayDirection.invoke(world, collider, seg).asInstanceOf[Int]
      if (result != 0) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
      }
      result != 0
    } finally arena.arenaClose()
  }
}
