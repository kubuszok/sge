/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: Panama FFM delegation to Rust C ABI native lib
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1604
 * Covenant-baseline-methods: PhysicsOpsPanama3d,arena,bodyApplyForce,bodyApplyForceAtPoint,bodyApplyImpulse,bodyApplyImpulseAtPoint,bodyApplyTorque,bodyApplyTorqueImpulse,bodyEnableCcd,bodyGetAngularDamping,bodyGetAngularVelocity,bodyGetDominanceGroup,bodyGetGravityScale,bodyGetInertia,bodyGetLinearDamping,bodyGetLinearVelocity,bodyGetLocalCenterOfMass,bodyGetMass,bodyGetPosition,bodyGetRotation,bodyGetType,bodyGetVelocityAtPoint,bodyGetWorldCenterOfMass,bodyIsAwake,bodyIsCcdEnabled,bodyIsEnabled,bodyIsRotationLockedX,bodyIsRotationLockedY,bodyIsRotationLockedZ,bodyIsTranslationLockedX,bodyIsTranslationLockedY,bodyIsTranslationLockedZ,bodyRecomputeMassProperties,bodyResetForces,bodyResetTorques,bodySetAngularDamping,bodySetAngularVelocity,bodySetDominanceGroup,bodySetEnabled,bodySetEnabledRotations,bodySetEnabledTranslations,bodySetFixedRotation,bodySetGravityScale,bodySetLinearDamping,bodySetLinearVelocity,bodySetPosition,bodySetRotation,bodySleep,bodyWakeUp,colliderGetAabb,colliderGetActiveCollisionTypes,colliderGetActiveEvents,colliderGetActiveHooks,colliderGetCollisionGroups,colliderGetContactForceEventThreshold,colliderGetDensity,colliderGetFriction,colliderGetMass,colliderGetOneWayDirection,colliderGetParentBody,colliderGetPosition,colliderGetPositionWrtParent,colliderGetRestitution,colliderGetShapeType,colliderGetSolverGroups,colliderIsEnabled,colliderIsSensor,colliderSetActiveCollisionTypes,colliderSetActiveEvents,colliderSetActiveHooks,colliderSetCollisionGroups,colliderSetContactForceEventThreshold,colliderSetContactSkin,colliderSetDensity,colliderSetEnabled,colliderSetFriction,colliderSetMass,colliderSetOneWayDirection,colliderSetPositionWrtParent,colliderSetRestitution,colliderSetSensor,colliderSetSolverGroups,contactPairCount,contactPairPoints,createBoxCollider,createCapsuleCollider,createConeCollider,createConvexHullCollider,createCylinderCollider,createDynamicBody,createFixedJoint,createHeightfieldCollider,createKinematicBody,createMotorJoint,createPrismaticJoint,createRevoluteJoint,createRopeJoint,createSphereCollider,createSpringJoint,createStaticBody,createTriMeshCollider,createWorld,destroyBody,destroyCollider,destroyJoint,destroyWorld,found,hBodyApplyForce,hBodyApplyForceAtPoint,hBodyApplyImpulse,hBodyApplyImpulseAtPoint,hBodyApplyTorque,hBodyApplyTorqueImpulse,hBodyEnableCcd,hBodyGetAngularDamping,hBodyGetAngularVelocity,hBodyGetDominanceGroup,hBodyGetGravityScale,hBodyGetInertia,hBodyGetLinearDamping,hBodyGetLinearVelocity,hBodyGetLocalCenterOfMass,hBodyGetMass,hBodyGetPosition,hBodyGetRotation,hBodyGetType,hBodyGetVelocityAtPoint,hBodyGetWorldCenterOfMass,hBodyIsAwake,hBodyIsCcdEnabled,hBodyIsEnabled,hBodyIsRotationLockedX,hBodyIsRotationLockedY,hBodyIsRotationLockedZ,hBodyIsTranslationLockedX,hBodyIsTranslationLockedY,hBodyIsTranslationLockedZ,hBodyRecomputeMassProperties,hBodyResetForces,hBodyResetTorques,hBodySetAngularDamping,hBodySetAngularVelocity,hBodySetDominanceGroup,hBodySetEnabled,hBodySetEnabledRotations,hBodySetEnabledTranslations,hBodySetFixedRotation,hBodySetGravityScale,hBodySetLinearDamping,hBodySetLinearVelocity,hBodySetPosition,hBodySetRotation,hBodySleep,hBodyWakeUp,hColliderGetAabb,hColliderGetActiveCollisionTypes,hColliderGetActiveEvents,hColliderGetActiveHooks,hColliderGetCollisionGroups,hColliderGetContactForceEventThreshold,hColliderGetDensity,hColliderGetFriction,hColliderGetMass,hColliderGetOneWayDirection,hColliderGetParentBody,hColliderGetPosition,hColliderGetPositionWrtParent,hColliderGetRestitution,hColliderGetShapeType,hColliderGetSolverGroups,hColliderIsEnabled,hColliderIsSensor,hColliderSetActiveCollisionTypes,hColliderSetActiveEvents,hColliderSetActiveHooks,hColliderSetCollisionGroups,hColliderSetContactForceEventThreshold,hColliderSetContactSkin,hColliderSetDensity,hColliderSetEnabled,hColliderSetFriction,hColliderSetMass,hColliderSetOneWayDirection,hColliderSetPositionWrtParent,hColliderSetRestitution,hColliderSetSensor,hColliderSetSolverGroups,hContactPairCount,hContactPairPoints,hCreateBoxCollider,hCreateCapsuleCollider,hCreateConeCollider,hCreateConvexHullCollider,hCreateCylinderCollider,hCreateDynamicBody,hCreateFixedJoint,hCreateHeightfieldCollider,hCreateKinematicBody,hCreateMotorJoint,hCreatePrismaticJoint,hCreateRevoluteJoint,hCreateRopeJoint,hCreateSphereCollider,hCreateSpringJoint,hCreateStaticBody,hCreateTriMeshCollider,hCreateWorld,hDestroyBody,hDestroyCollider,hDestroyJoint,hDestroyWorld,hMotorJointGetCorrectionFactor,hMotorJointGetLinearOffset,hMotorJointGetMaxForce,hMotorJointGetMaxTorque,hMotorJointSetCorrectionFactor,hMotorJointSetLinearOffset,hMotorJointSetMaxForce,hMotorJointSetMaxTorque,hPollContactForceEvents,hPollContactStartEvents,hPollContactStopEvents,hPollIntersectionStartEvents,hPollIntersectionStopEvents,hPrismaticJointEnableLimits,hPrismaticJointEnableMotor,hPrismaticJointGetLimits,hPrismaticJointGetMaxMotorForce,hPrismaticJointGetMotorSpeed,hPrismaticJointGetTranslation,hPrismaticJointSetLimits,hPrismaticJointSetMaxMotorForce,hPrismaticJointSetMotorSpeed,hProjectPoint,hQueryAABB,hQueryPoint,hRayCast,hRayCastAll,hRevoluteJointEnableLimits,hRevoluteJointEnableMotor,hRevoluteJointGetAngle,hRevoluteJointGetLimits,hRevoluteJointGetMaxMotorTorque,hRevoluteJointGetMotorSpeed,hRevoluteJointIsLimitEnabled,hRevoluteJointSetLimits,hRevoluteJointSetMaxMotorTorque,hRevoluteJointSetMotorSpeed,hRopeJointGetMaxDistance,hRopeJointSetMaxDistance,hSpringJointGetRestLength,hSpringJointSetParams,hSpringJointSetRestLength,hWorldGetGravity,hWorldGetNumSolverIterations,hWorldSetGravity,hWorldSetNumAdditionalFrictionIterations,hWorldSetNumInternalPgsIterations,hWorldSetNumSolverIterations,hWorldStep,lib,linker,lookup,motorJointGetCorrectionFactor,motorJointGetLinearOffset,motorJointGetMaxForce,motorJointGetMaxTorque,motorJointSetCorrectionFactor,motorJointSetLinearOffset,motorJointSetMaxForce,motorJointSetMaxTorque,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,prismaticJointEnableLimits,prismaticJointEnableMotor,prismaticJointGetLimits,prismaticJointGetMaxMotorForce,prismaticJointGetMotorSpeed,prismaticJointGetTranslation,prismaticJointSetLimits,prismaticJointSetMaxMotorForce,prismaticJointSetMotorSpeed,projectPoint,queryAABB,queryPoint,rayCast,rayCastAll,result,revoluteJointEnableLimits,revoluteJointEnableMotor,revoluteJointGetAngle,revoluteJointGetLimits,revoluteJointGetMaxMotorTorque,revoluteJointGetMotorSpeed,revoluteJointIsLimitEnabled,revoluteJointSetLimits,revoluteJointSetMaxMotorTorque,revoluteJointSetMotorSpeed,ropeJointGetMaxDistance,ropeJointSetMaxDistance,springJointGetRestLength,springJointSetParams,springJointSetRestLength,worldGetGravity,worldGetNumSolverIterations,worldSetGravity,worldSetNumAdditionalFrictionIterations,worldSetNumInternalPgsIterations,worldSetNumSolverIterations,worldStep
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
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
  private val hBodyApplyTorqueImpulse: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_apply_torque_impulse"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hBodyResetForces: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_reset_forces"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyResetTorques: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_reset_torques"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetType: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_type"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetEnabledTranslations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_enabled_translations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT)
  )
  private val hBodyIsTranslationLockedX: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_translation_locked_x"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsTranslationLockedY: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_translation_locked_y"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsTranslationLockedZ: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_translation_locked_z"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodySetEnabledRotations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_set_enabled_rotations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT, p.JAVA_INT)
  )
  private val hBodyIsRotationLockedX: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_rotation_locked_x"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsRotationLockedY: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_rotation_locked_y"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyIsRotationLockedZ: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_is_rotation_locked_z"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetWorldCenterOfMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_world_center_of_mass"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetLocalCenterOfMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_local_center_of_mass"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hBodyGetInertia: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_inertia"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hBodyGetVelocityAtPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_body_get_velocity_at_point"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
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
  private val hColliderGetDensity: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_density"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetFriction: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_friction"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetRestitution: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_restitution"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderIsSensor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_is_sensor"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_enabled"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderIsEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_is_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetPositionWrtParent: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_position_wrt_parent"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderSetPositionWrtParent: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_position_wrt_parent"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hColliderGetPosition: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_position"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderGetShapeType: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_shape_type"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetAabb: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_aabb"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderGetParentBody: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_parent_body"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderGetMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_mass"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetMass: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_mass"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetContactSkin: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_contact_skin"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderSetActiveEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_active_events"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderGetActiveEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_active_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetActiveCollisionTypes: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_active_collision_types"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderGetActiveCollisionTypes: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_active_collision_types"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Collision filtering
  private val hColliderSetCollisionGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_collision_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )
  private val hColliderGetCollisionGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_collision_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hColliderSetSolverGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_solver_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT, p.JAVA_INT)
  )
  private val hColliderGetSolverGroups: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_solver_groups"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )

  // Heightfield collider
  private val hCreateHeightfieldCollider: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_heightfield_collider"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
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

  // Additional joint creation
  private val hCreateRevoluteJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_revolute_joint"),
    p.FunctionDescriptor.of(
      p.JAVA_LONG,
      p.JAVA_LONG,
      p.JAVA_LONG,
      p.JAVA_LONG,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT
    )
  )
  private val hCreatePrismaticJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_prismatic_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hCreateMotorJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_motor_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hCreateSpringJoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_create_spring_joint"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )

  // Revolute joint limits/motors
  private val hRevoluteJointEnableLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_enable_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hRevoluteJointSetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_set_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hRevoluteJointGetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_get_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hRevoluteJointIsLimitEnabled: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_is_limit_enabled"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hRevoluteJointEnableMotor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_enable_motor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hRevoluteJointSetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_set_motor_speed"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hRevoluteJointSetMaxMotorTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_set_max_motor_torque"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hRevoluteJointGetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_get_motor_speed"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hRevoluteJointGetAngle: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_get_angle"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hRevoluteJointGetMaxMotorTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_revolute_joint_get_max_motor_torque"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Prismatic joint limits/motors
  private val hPrismaticJointEnableLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_enable_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hPrismaticJointSetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_set_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hPrismaticJointGetLimits: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_get_limits"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hPrismaticJointEnableMotor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_enable_motor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hPrismaticJointSetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_set_motor_speed"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hPrismaticJointSetMaxMotorForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_set_max_motor_force"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hPrismaticJointGetTranslation: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_get_translation"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hPrismaticJointGetMotorSpeed: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_get_motor_speed"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hPrismaticJointGetMaxMotorForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_prismatic_joint_get_max_motor_force"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Motor joint
  private val hMotorJointSetLinearOffset: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_set_linear_offset"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hMotorJointGetLinearOffset: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_get_linear_offset"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )
  private val hMotorJointSetMaxForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_set_max_force"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hMotorJointSetMaxTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_set_max_torque"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hMotorJointSetCorrectionFactor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_set_correction_factor"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hMotorJointGetMaxForce: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_get_max_force"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hMotorJointGetMaxTorque: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_get_max_torque"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hMotorJointGetCorrectionFactor: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_motor_joint_get_correction_factor"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Rope joint
  private val hRopeJointSetMaxDistance: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_rope_joint_set_max_distance"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hRopeJointGetMaxDistance: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_rope_joint_get_max_distance"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Spring joint
  private val hSpringJointSetRestLength: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_spring_joint_set_rest_length"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hSpringJointGetRestLength: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_spring_joint_get_rest_length"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hSpringJointSetParams: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_spring_joint_set_params"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )

  // Additional queries
  private val hQueryAABB: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_query_aabb"),
    p.FunctionDescriptor.of(
      p.JAVA_INT,
      p.JAVA_LONG,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.ADDRESS,
      p.JAVA_INT
    )
  )
  private val hQueryPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_query_point"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS, p.JAVA_INT)
  )
  private val hRayCastAll: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_ray_cast_all"),
    p.FunctionDescriptor.of(
      p.JAVA_INT,
      p.JAVA_LONG,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.ADDRESS,
      p.JAVA_INT
    )
  )
  private val hProjectPoint: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_project_point"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.ADDRESS)
  )

  // Contact detail queries
  private val hContactPairCount: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_contact_pair_count"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hContactPairPoints: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_contact_pair_points"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS, p.JAVA_INT)
  )

  // Intersection events
  private val hPollIntersectionStartEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_poll_intersection_start_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )
  private val hPollIntersectionStopEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_poll_intersection_stop_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )

  // Contact force events
  private val hPollContactForceEvents: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_poll_contact_force_events"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.ADDRESS, p.ADDRESS, p.ADDRESS, p.JAVA_INT)
  )
  private val hColliderSetContactForceEventThreshold: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_contact_force_event_threshold"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT)
  )
  private val hColliderGetContactForceEventThreshold: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_contact_force_event_threshold"),
    p.FunctionDescriptor.of(p.JAVA_FLOAT, p.JAVA_LONG, p.JAVA_LONG)
  )

  // Active hooks / one-way direction
  private val hColliderSetActiveHooks: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_active_hooks"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_INT)
  )
  private val hColliderGetActiveHooks: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_active_hooks"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG)
  )
  private val hColliderSetOneWayDirection: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_set_one_way_direction"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_LONG, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT, p.JAVA_FLOAT)
  )
  private val hColliderGetOneWayDirection: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_collider_get_one_way_direction"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG, p.JAVA_LONG, p.ADDRESS)
  )

  // Solver parameters
  private val hWorldSetNumSolverIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_set_num_solver_iterations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT)
  )
  private val hWorldGetNumSolverIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_get_num_solver_iterations"),
    p.FunctionDescriptor.of(p.JAVA_INT, p.JAVA_LONG)
  )
  private val hWorldSetNumAdditionalFrictionIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_set_num_additional_friction_iterations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT)
  )
  private val hWorldSetNumInternalPgsIterations: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_world_set_num_internal_pgs_iterations"),
    p.FunctionDescriptor.ofVoid(p.JAVA_LONG, p.JAVA_INT)
  )

  // Queries
  private val hRayCast: MethodHandle = linker.downcallHandle(
    lookup("sge_phys3d_ray_cast"),
    p.FunctionDescriptor.of(
      p.JAVA_INT,
      p.JAVA_LONG,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.JAVA_FLOAT,
      p.ADDRESS
    )
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

  override def bodyApplyTorqueImpulse(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit =
    hBodyApplyTorqueImpulse.invoke(world, body, tx, ty, tz)

  override def bodyResetForces(world: Long, body: Long): Unit =
    hBodyResetForces.invoke(world, body)

  override def bodyResetTorques(world: Long, body: Long): Unit =
    hBodyResetTorques.invoke(world, body)

  override def bodyGetType(world: Long, body: Long): Int =
    hBodyGetType.invoke(world, body).asInstanceOf[Int]

  override def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean, allowZ: Boolean): Unit =
    hBodySetEnabledTranslations.invoke(world, body, if (allowX) 1 else 0, if (allowY) 1 else 0, if (allowZ) 1 else 0)

  override def bodyIsTranslationLockedX(world: Long, body: Long): Boolean = {
    val result = hBodyIsTranslationLockedX.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyIsTranslationLockedY(world: Long, body: Long): Boolean = {
    val result = hBodyIsTranslationLockedY.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyIsTranslationLockedZ(world: Long, body: Long): Boolean = {
    val result = hBodyIsTranslationLockedZ.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodySetEnabledRotations(world: Long, body: Long, allowX: Boolean, allowY: Boolean, allowZ: Boolean): Unit =
    hBodySetEnabledRotations.invoke(world, body, if (allowX) 1 else 0, if (allowY) 1 else 0, if (allowZ) 1 else 0)

  override def bodyIsRotationLockedX(world: Long, body: Long): Boolean = {
    val result = hBodyIsRotationLockedX.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyIsRotationLockedY(world: Long, body: Long): Boolean = {
    val result = hBodyIsRotationLockedY.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyIsRotationLockedZ(world: Long, body: Long): Boolean = {
    val result = hBodyIsRotationLockedZ.invoke(world, body).asInstanceOf[Int]
    result != 0
  }

  override def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hBodyGetWorldCenterOfMass.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hBodyGetLocalCenterOfMass.invoke(world, body, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

  override def bodyGetInertia(world: Long, body: Long): Float =
    hBodyGetInertia.invoke(world, body).asInstanceOf[Float]

  override def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, pz: Float, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hBodyGetVelocityAtPoint.invoke(world, body, px, py, pz, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

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
      val seg = arena.allocateElems(p.JAVA_FLOAT, 7L)
      hColliderGetPositionWrtParent.invoke(world, collider, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 7)
    } finally arena.arenaClose()
  }

  override def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Unit =
    hColliderSetPositionWrtParent.invoke(world, collider, x, y, z, qx, qy, qz, qw)

  override def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 7L)
      hColliderGetPosition.invoke(world, collider, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 7)
    } finally arena.arenaClose()
  }

  override def colliderGetShapeType(world: Long, collider: Long): Int =
    hColliderGetShapeType.invoke(world, collider).asInstanceOf[Int]

  override def colliderGetAabb(world: Long, collider: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 6L)
      hColliderGetAabb.invoke(world, collider, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 6)
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

  // ─── Heightfield collider ─────────────────────────────────────────────

  override def createHeightfieldCollider(
    world:   Long,
    body:    Long,
    heights: Array[Float],
    nrows:   Int,
    ncols:   Int,
    scaleX:  Float,
    scaleY:  Float,
    scaleZ:  Float
  ): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, heights.length.toLong)
      p.MemorySegment.copyFromFloats(heights, 0, seg, 0L, heights.length)
      hCreateHeightfieldCollider.invoke(world, body, seg, nrows, ncols, scaleX, scaleY, scaleZ).asInstanceOf[Long]
    } finally arena.arenaClose()
  }

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    hCreateFixedJoint.invoke(world, body1, body2).asInstanceOf[Long]

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long =
    hCreateRopeJoint.invoke(world, body1, body2, maxDist).asInstanceOf[Long]

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float, anchorZ: Float, axisX: Float, axisY: Float, axisZ: Float): Long =
    hCreateRevoluteJoint.invoke(world, body1, body2, anchorX, anchorY, anchorZ, axisX, axisY, axisZ).asInstanceOf[Long]

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float, axisZ: Float): Long =
    hCreatePrismaticJoint.invoke(world, body1, body2, axisX, axisY, axisZ).asInstanceOf[Long]

  override def createMotorJoint(world: Long, body1: Long, body2: Long): Long =
    hCreateMotorJoint.invoke(world, body1, body2).asInstanceOf[Long]

  override def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long =
    hCreateSpringJoint.invoke(world, body1, body2, restLength, stiffness, damping).asInstanceOf[Long]

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

  // ─── Motor joint ──────────────────────────────────────────────────────

  override def motorJointSetLinearOffset(world: Long, joint: Long, x: Float, y: Float, z: Float): Unit =
    hMotorJointSetLinearOffset.invoke(world, joint, x, y, z)

  override def motorJointGetLinearOffset(world: Long, joint: Long, out: Array[Float]): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val seg = arena.allocateElems(p.JAVA_FLOAT, 3L)
      hMotorJointGetLinearOffset.invoke(world, joint, seg)
      p.MemorySegment.copyToFloats(seg, 0L, out, 0, 3)
    } finally arena.arenaClose()
  }

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

  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float): Unit =
    hSpringJointSetRestLength.invoke(world, joint, restLength)

  override def springJointGetRestLength(world: Long, joint: Long): Float =
    hSpringJointGetRestLength.invoke(world, joint).asInstanceOf[Float]

  override def springJointSetParams(world: Long, joint: Long, stiffness: Float, damping: Float): Unit =
    hSpringJointSetParams.invoke(world, joint, stiffness, damping)

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

  // ─── Additional queries ───────────────────────────────────────────────

  override def queryAABB(
    world:        Long,
    minX:         Float,
    minY:         Float,
    minZ:         Float,
    maxX:         Float,
    maxY:         Float,
    maxZ:         Float,
    outColliders: Array[Long],
    maxResults:   Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg   = arena.allocateElems(p.JAVA_LONG, maxResults.toLong)
      val count = hQueryAABB.invoke(world, minX, minY, minZ, maxX, maxY, maxZ, seg, maxResults).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outColliders(i) = seg.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  override def queryPoint(world: Long, x: Float, y: Float, z: Float, outBodies: Array[Long], maxResults: Int): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val seg   = arena.allocateElems(p.JAVA_LONG, maxResults.toLong)
      val count = hQueryPoint.invoke(world, x, y, z, seg, maxResults).asInstanceOf[Int]
      var i     = 0
      while (i < count) {
        outBodies(i) = seg.getLong(i.toLong * 8L)
        i += 1
      }
      count
    } finally arena.arenaClose()
  }

  override def rayCastAll(
    world:   Long,
    originX: Float,
    originY: Float,
    originZ: Float,
    dirX:    Float,
    dirY:    Float,
    dirZ:    Float,
    maxDist: Float,
    outHits: Array[Float],
    maxHits: Int
  ): Int = {
    val arena = p.Arena.ofConfined()
    try {
      val totalFloats = maxHits.toLong * 9L
      val seg         = arena.allocateElems(p.JAVA_FLOAT, totalFloats)
      val count       = hRayCastAll.invoke(world, originX, originY, originZ, dirX, dirY, dirZ, maxDist, seg, maxHits).asInstanceOf[Int]
      if (count > 0) {
        p.MemorySegment.copyToFloats(seg, 0L, outHits, 0, count * 9)
      }
      count
    } finally arena.arenaClose()
  }

  override def projectPoint(world: Long, x: Float, y: Float, z: Float, out: Array[Float]): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      val seg      = arena.allocateElems(p.JAVA_FLOAT, 6L)
      val foundInt = hProjectPoint.invoke(world, x, y, z, seg).asInstanceOf[Int]
      val found    = foundInt != 0
      if (found) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, 6)
      }
      found
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
      val totalFloats = maxPoints.toLong * 7L
      val seg         = arena.allocateElems(p.JAVA_FLOAT, totalFloats)
      val count       = hContactPairPoints.invoke(world, collider1, collider2, seg, maxPoints).asInstanceOf[Int]
      if (count > 0) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, count * 7)
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

  override def colliderSetOneWayDirection(world: Long, collider: Long, nx: Float, ny: Float, nz: Float, allowedAngle: Float): Unit =
    hColliderSetOneWayDirection.invoke(world, collider, nx, ny, nz, allowedAngle)

  override def colliderGetOneWayDirection(world: Long, collider: Long, out: Array[Float]): Boolean = {
    val arena = p.Arena.ofConfined()
    try {
      val seg    = arena.allocateElems(p.JAVA_FLOAT, 4L)
      val result = hColliderGetOneWayDirection.invoke(world, collider, seg).asInstanceOf[Int]
      if (result != 0) {
        p.MemorySegment.copyToFloats(seg, 0L, out, 0, 4)
      }
      result != 0
    } finally arena.arenaClose()
  }
}
