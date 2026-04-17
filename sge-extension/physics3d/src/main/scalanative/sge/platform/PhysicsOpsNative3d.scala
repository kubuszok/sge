/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: Scala Native @extern bindings to Rust C ABI
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1005
 * Covenant-baseline-methods: Physics3dC,PhysicsOpsNative3d,bodyApplyForce,bodyApplyForceAtPoint,bodyApplyImpulse,bodyApplyImpulseAtPoint,bodyApplyTorque,bodyApplyTorqueImpulse,bodyEnableCcd,bodyGetAngularDamping,bodyGetAngularVelocity,bodyGetDominanceGroup,bodyGetGravityScale,bodyGetInertia,bodyGetLinearDamping,bodyGetLinearVelocity,bodyGetLocalCenterOfMass,bodyGetMass,bodyGetPosition,bodyGetRotation,bodyGetType,bodyGetVelocityAtPoint,bodyGetWorldCenterOfMass,bodyIsAwake,bodyIsCcdEnabled,bodyIsEnabled,bodyIsRotationLockedX,bodyIsRotationLockedY,bodyIsRotationLockedZ,bodyIsTranslationLockedX,bodyIsTranslationLockedY,bodyIsTranslationLockedZ,bodyRecomputeMassProperties,bodyResetForces,bodyResetTorques,bodySetAngularDamping,bodySetAngularVelocity,bodySetDominanceGroup,bodySetEnabled,bodySetEnabledRotations,bodySetEnabledTranslations,bodySetFixedRotation,bodySetGravityScale,bodySetLinearDamping,bodySetLinearVelocity,bodySetPosition,bodySetRotation,bodySleep,bodyWakeUp,buf,buf1,buf2,bufF,colliderGetAabb,colliderGetActiveCollisionTypes,colliderGetActiveEvents,colliderGetActiveHooks,colliderGetCollisionGroups,colliderGetContactForceEventThreshold,colliderGetDensity,colliderGetFriction,colliderGetMass,colliderGetOneWayDirection,colliderGetParentBody,colliderGetPosition,colliderGetPositionWrtParent,colliderGetRestitution,colliderGetShapeType,colliderGetSolverGroups,colliderIsEnabled,colliderIsSensor,colliderSetActiveCollisionTypes,colliderSetActiveEvents,colliderSetActiveHooks,colliderSetCollisionGroups,colliderSetContactForceEventThreshold,colliderSetContactSkin,colliderSetDensity,colliderSetEnabled,colliderSetFriction,colliderSetMass,colliderSetOneWayDirection,colliderSetPositionWrtParent,colliderSetRestitution,colliderSetSensor,colliderSetSolverGroups,contactPairCount,contactPairPoints,copyCount,count,createBoxCollider,createCapsuleCollider,createConeCollider,createConvexHullCollider,createCylinderCollider,createDynamicBody,createFixedJoint,createHeightfieldCollider,createKinematicBody,createMotorJoint,createPrismaticJoint,createRevoluteJoint,createRopeJoint,createSphereCollider,createSpringJoint,createStaticBody,createTriMeshCollider,createWorld,destroyBody,destroyCollider,destroyJoint,destroyWorld,hit,i,motorJointGetCorrectionFactor,motorJointGetLinearOffset,motorJointGetMaxForce,motorJointGetMaxTorque,motorJointSetCorrectionFactor,motorJointSetLinearOffset,motorJointSetMaxForce,motorJointSetMaxTorque,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,prismaticJointEnableLimits,prismaticJointEnableMotor,prismaticJointGetLimits,prismaticJointGetMaxMotorForce,prismaticJointGetMotorSpeed,prismaticJointGetTranslation,prismaticJointSetLimits,prismaticJointSetMaxMotorForce,prismaticJointSetMotorSpeed,projectPoint,queryAABB,queryPoint,rayCast,rayCastAll,result,revoluteJointEnableLimits,revoluteJointEnableMotor,revoluteJointGetAngle,revoluteJointGetLimits,revoluteJointGetMaxMotorTorque,revoluteJointGetMotorSpeed,revoluteJointIsLimitEnabled,revoluteJointSetLimits,revoluteJointSetMaxMotorTorque,revoluteJointSetMotorSpeed,ropeJointGetMaxDistance,ropeJointSetMaxDistance,sge_phys3d_body_apply_force,sge_phys3d_body_apply_force_at_point,sge_phys3d_body_apply_impulse,sge_phys3d_body_apply_impulse_at_point,sge_phys3d_body_apply_torque,sge_phys3d_body_apply_torque_impulse,sge_phys3d_body_enable_ccd,sge_phys3d_body_get_angular_damping,sge_phys3d_body_get_angular_velocity,sge_phys3d_body_get_dominance_group,sge_phys3d_body_get_gravity_scale,sge_phys3d_body_get_inertia,sge_phys3d_body_get_linear_damping,sge_phys3d_body_get_linear_velocity,sge_phys3d_body_get_local_center_of_mass,sge_phys3d_body_get_mass,sge_phys3d_body_get_position,sge_phys3d_body_get_rotation,sge_phys3d_body_get_type,sge_phys3d_body_get_velocity_at_point,sge_phys3d_body_get_world_center_of_mass,sge_phys3d_body_is_awake,sge_phys3d_body_is_ccd_enabled,sge_phys3d_body_is_enabled,sge_phys3d_body_is_rotation_locked_x,sge_phys3d_body_is_rotation_locked_y,sge_phys3d_body_is_rotation_locked_z,sge_phys3d_body_is_translation_locked_x,sge_phys3d_body_is_translation_locked_y,sge_phys3d_body_is_translation_locked_z,sge_phys3d_body_recompute_mass_properties,sge_phys3d_body_reset_forces,sge_phys3d_body_reset_torques,sge_phys3d_body_set_angular_damping,sge_phys3d_body_set_angular_velocity,sge_phys3d_body_set_dominance_group,sge_phys3d_body_set_enabled,sge_phys3d_body_set_enabled_rotations,sge_phys3d_body_set_enabled_translations,sge_phys3d_body_set_fixed_rotation,sge_phys3d_body_set_gravity_scale,sge_phys3d_body_set_linear_damping,sge_phys3d_body_set_linear_velocity,sge_phys3d_body_set_position,sge_phys3d_body_set_rotation,sge_phys3d_body_sleep,sge_phys3d_body_wake_up,sge_phys3d_collider_get_aabb,sge_phys3d_collider_get_active_collision_types,sge_phys3d_collider_get_active_events,sge_phys3d_collider_get_active_hooks,sge_phys3d_collider_get_collision_groups,sge_phys3d_collider_get_contact_force_event_threshold,sge_phys3d_collider_get_density,sge_phys3d_collider_get_friction,sge_phys3d_collider_get_mass,sge_phys3d_collider_get_one_way_direction,sge_phys3d_collider_get_parent_body,sge_phys3d_collider_get_position,sge_phys3d_collider_get_position_wrt_parent,sge_phys3d_collider_get_restitution,sge_phys3d_collider_get_shape_type,sge_phys3d_collider_get_solver_groups,sge_phys3d_collider_is_enabled,sge_phys3d_collider_is_sensor,sge_phys3d_collider_set_active_collision_types,sge_phys3d_collider_set_active_events,sge_phys3d_collider_set_active_hooks,sge_phys3d_collider_set_collision_groups,sge_phys3d_collider_set_contact_force_event_threshold,sge_phys3d_collider_set_contact_skin,sge_phys3d_collider_set_density,sge_phys3d_collider_set_enabled,sge_phys3d_collider_set_friction,sge_phys3d_collider_set_mass,sge_phys3d_collider_set_one_way_direction,sge_phys3d_collider_set_position_wrt_parent,sge_phys3d_collider_set_restitution,sge_phys3d_collider_set_sensor,sge_phys3d_collider_set_solver_groups,sge_phys3d_contact_pair_count,sge_phys3d_contact_pair_points,sge_phys3d_create_box_collider,sge_phys3d_create_capsule_collider,sge_phys3d_create_cone_collider,sge_phys3d_create_convex_hull_collider,sge_phys3d_create_cylinder_collider,sge_phys3d_create_dynamic_body,sge_phys3d_create_fixed_joint,sge_phys3d_create_heightfield_collider,sge_phys3d_create_kinematic_body,sge_phys3d_create_motor_joint,sge_phys3d_create_prismatic_joint,sge_phys3d_create_revolute_joint,sge_phys3d_create_rope_joint,sge_phys3d_create_sphere_collider,sge_phys3d_create_spring_joint,sge_phys3d_create_static_body,sge_phys3d_create_trimesh_collider,sge_phys3d_create_world,sge_phys3d_destroy_body,sge_phys3d_destroy_collider,sge_phys3d_destroy_joint,sge_phys3d_destroy_world,sge_phys3d_motor_joint_get_correction_factor,sge_phys3d_motor_joint_get_linear_offset,sge_phys3d_motor_joint_get_max_force,sge_phys3d_motor_joint_get_max_torque,sge_phys3d_motor_joint_set_correction_factor,sge_phys3d_motor_joint_set_linear_offset,sge_phys3d_motor_joint_set_max_force,sge_phys3d_motor_joint_set_max_torque,sge_phys3d_poll_contact_force_events,sge_phys3d_poll_contact_start_events,sge_phys3d_poll_contact_stop_events,sge_phys3d_poll_intersection_start_events,sge_phys3d_poll_intersection_stop_events,sge_phys3d_prismatic_joint_enable_limits,sge_phys3d_prismatic_joint_enable_motor,sge_phys3d_prismatic_joint_get_limits,sge_phys3d_prismatic_joint_get_max_motor_force,sge_phys3d_prismatic_joint_get_motor_speed,sge_phys3d_prismatic_joint_get_translation,sge_phys3d_prismatic_joint_set_limits,sge_phys3d_prismatic_joint_set_max_motor_force,sge_phys3d_prismatic_joint_set_motor_speed,sge_phys3d_project_point,sge_phys3d_query_aabb,sge_phys3d_query_point,sge_phys3d_ray_cast,sge_phys3d_ray_cast_all,sge_phys3d_revolute_joint_enable_limits,sge_phys3d_revolute_joint_enable_motor,sge_phys3d_revolute_joint_get_angle,sge_phys3d_revolute_joint_get_limits,sge_phys3d_revolute_joint_get_max_motor_torque,sge_phys3d_revolute_joint_get_motor_speed,sge_phys3d_revolute_joint_is_limit_enabled,sge_phys3d_revolute_joint_set_limits,sge_phys3d_revolute_joint_set_max_motor_torque,sge_phys3d_revolute_joint_set_motor_speed,sge_phys3d_rope_joint_get_max_distance,sge_phys3d_rope_joint_set_max_distance,sge_phys3d_spring_joint_get_rest_length,sge_phys3d_spring_joint_set_params,sge_phys3d_spring_joint_set_rest_length,sge_phys3d_world_get_gravity,sge_phys3d_world_get_num_solver_iterations,sge_phys3d_world_set_gravity,sge_phys3d_world_set_num_additional_friction_iterations,sge_phys3d_world_set_num_internal_pgs_iterations,sge_phys3d_world_set_num_solver_iterations,sge_phys3d_world_step,springJointGetRestLength,springJointSetParams,springJointSetRestLength,totalFloats,worldGetGravity,worldGetNumSolverIterations,worldSetGravity,worldSetNumAdditionalFrictionIterations,worldSetNumInternalPgsIterations,worldSetNumSolverIterations,worldStep
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package platform

import scala.scalanative.unsafe.*

@link("sge_physics3d")
@extern
private object Physics3dC {
  // World lifecycle
  def sge_phys3d_create_world(gx:         CFloat, gy: CFloat, gz: CFloat):             Long = extern
  def sge_phys3d_destroy_world(world:     Long):                                       Unit = extern
  def sge_phys3d_world_step(world:        Long, dt:   CFloat):                         Unit = extern
  def sge_phys3d_world_set_gravity(world: Long, gx:   CFloat, gy: CFloat, gz: CFloat): Unit = extern
  def sge_phys3d_world_get_gravity(world: Long, out:  Ptr[CFloat]):                    Unit = extern

  // Rigid body creation — position (x, y, z) + quaternion (qx, qy, qz, qw)
  def sge_phys3d_create_dynamic_body(world:   Long, x:    CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Long = extern
  def sge_phys3d_create_static_body(world:    Long, x:    CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Long = extern
  def sge_phys3d_create_kinematic_body(world: Long, x:    CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Long = extern
  def sge_phys3d_destroy_body(world:          Long, body: Long):                                                                         Unit = extern

  // Body getters — positions are [x,y,z], rotations are [qx,qy,qz,qw], velocities are [vx,vy,vz]
  def sge_phys3d_body_get_position(world:         Long, body: Long, out: Ptr[CFloat]): Unit = extern
  def sge_phys3d_body_get_rotation(world:         Long, body: Long, out: Ptr[CFloat]): Unit = extern
  def sge_phys3d_body_get_linear_velocity(world:  Long, body: Long, out: Ptr[CFloat]): Unit = extern
  def sge_phys3d_body_get_angular_velocity(world: Long, body: Long, out: Ptr[CFloat]): Unit = extern

  // Body setters
  def sge_phys3d_body_set_position(world:         Long, body: Long, x:  CFloat, y:  CFloat, z:  CFloat):             Unit = extern
  def sge_phys3d_body_set_rotation(world:         Long, body: Long, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Unit = extern
  def sge_phys3d_body_set_linear_velocity(world:  Long, body: Long, vx: CFloat, vy: CFloat, vz: CFloat):             Unit = extern
  def sge_phys3d_body_set_angular_velocity(world: Long, body: Long, wx: CFloat, wy: CFloat, wz: CFloat):             Unit = extern

  // Body forces (3D vectors)
  def sge_phys3d_body_apply_force(world:            Long, body: Long, fx: CFloat, fy: CFloat, fz: CFloat):                                     Unit = extern
  def sge_phys3d_body_apply_impulse(world:          Long, body: Long, ix: CFloat, iy: CFloat, iz: CFloat):                                     Unit = extern
  def sge_phys3d_body_apply_torque(world:           Long, body: Long, tx: CFloat, ty: CFloat, tz: CFloat):                                     Unit = extern
  def sge_phys3d_body_apply_force_at_point(world:   Long, body: Long, fx: CFloat, fy: CFloat, fz: CFloat, px: CFloat, py: CFloat, pz: CFloat): Unit = extern
  def sge_phys3d_body_apply_impulse_at_point(world: Long, body: Long, ix: CFloat, iy: CFloat, iz: CFloat, px: CFloat, py: CFloat, pz: CFloat): Unit = extern

  // Body properties
  def sge_phys3d_body_set_linear_damping(world:        Long, body: Long, damping: CFloat): Unit   = extern
  def sge_phys3d_body_get_linear_damping(world:        Long, body: Long):                  CFloat = extern
  def sge_phys3d_body_set_angular_damping(world:       Long, body: Long, damping: CFloat): Unit   = extern
  def sge_phys3d_body_get_angular_damping(world:       Long, body: Long):                  CFloat = extern
  def sge_phys3d_body_set_gravity_scale(world:         Long, body: Long, scale:   CFloat): Unit   = extern
  def sge_phys3d_body_get_gravity_scale(world:         Long, body: Long):                  CFloat = extern
  def sge_phys3d_body_is_awake(world:                  Long, body: Long):                  CInt   = extern
  def sge_phys3d_body_wake_up(world:                   Long, body: Long):                  Unit   = extern
  def sge_phys3d_body_sleep(world:                     Long, body: Long):                  Unit   = extern
  def sge_phys3d_body_set_fixed_rotation(world:        Long, body: Long, fixed:   CInt):   Unit   = extern
  def sge_phys3d_body_enable_ccd(world:                Long, body: Long, enable:  CInt):   Unit   = extern
  def sge_phys3d_body_is_ccd_enabled(world:            Long, body: Long):                  CInt   = extern
  def sge_phys3d_body_set_enabled(world:               Long, body: Long, enabled: CInt):   Unit   = extern
  def sge_phys3d_body_is_enabled(world:                Long, body: Long):                  CInt   = extern
  def sge_phys3d_body_set_dominance_group(world:       Long, body: Long, group:   CInt):   Unit   = extern
  def sge_phys3d_body_get_dominance_group(world:       Long, body: Long):                  CInt   = extern
  def sge_phys3d_body_get_mass(world:                  Long, body: Long):                  CFloat = extern
  def sge_phys3d_body_recompute_mass_properties(world: Long, body: Long):                  Unit   = extern

  // Body extras
  def sge_phys3d_body_apply_torque_impulse(world:     Long, body: Long, tx:  CFloat, ty: CFloat, tz: CFloat):                   Unit   = extern
  def sge_phys3d_body_reset_forces(world:             Long, body: Long):                                                        Unit   = extern
  def sge_phys3d_body_reset_torques(world:            Long, body: Long):                                                        Unit   = extern
  def sge_phys3d_body_get_type(world:                 Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_set_enabled_translations(world: Long, body: Long, x:   CInt, y:    CInt, z:    CInt):                     Unit   = extern
  def sge_phys3d_body_is_translation_locked_x(world:  Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_is_translation_locked_y(world:  Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_is_translation_locked_z(world:  Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_set_enabled_rotations(world:    Long, body: Long, x:   CInt, y:    CInt, z:    CInt):                     Unit   = extern
  def sge_phys3d_body_is_rotation_locked_x(world:     Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_is_rotation_locked_y(world:     Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_is_rotation_locked_z(world:     Long, body: Long):                                                        CInt   = extern
  def sge_phys3d_body_get_world_center_of_mass(world: Long, body: Long, out: Ptr[CFloat]):                                      Unit   = extern
  def sge_phys3d_body_get_local_center_of_mass(world: Long, body: Long, out: Ptr[CFloat]):                                      Unit   = extern
  def sge_phys3d_body_get_inertia(world:              Long, body: Long):                                                        CFloat = extern
  def sge_phys3d_body_get_velocity_at_point(world:    Long, body: Long, px:  CFloat, py: CFloat, pz: CFloat, out: Ptr[CFloat]): Unit   = extern

  // Collider creation (3D shapes)
  def sge_phys3d_create_sphere_collider(world:      Long, body:     Long, radius:     CFloat):                                                               Long = extern
  def sge_phys3d_create_box_collider(world:         Long, body:     Long, hx:         CFloat, hy:               CFloat, hz:    CFloat):                      Long = extern
  def sge_phys3d_create_capsule_collider(world:     Long, body:     Long, halfHeight: CFloat, radius:           CFloat):                                     Long = extern
  def sge_phys3d_create_cylinder_collider(world:    Long, body:     Long, halfHeight: CFloat, radius:           CFloat):                                     Long = extern
  def sge_phys3d_create_cone_collider(world:        Long, body:     Long, halfHeight: CFloat, radius:           CFloat):                                     Long = extern
  def sge_phys3d_create_convex_hull_collider(world: Long, body:     Long, vertices:   Ptr[CFloat], vertexCount: CInt):                                       Long = extern
  def sge_phys3d_create_trimesh_collider(world:     Long, body:     Long, vertices:   Ptr[CFloat], vertexCount: CInt, indices: Ptr[CInt], indexCount: CInt): Long = extern
  def sge_phys3d_destroy_collider(world:            Long, collider: Long):                                                                                   Unit = extern

  // Collider properties
  def sge_phys3d_collider_set_density(world:     Long, collider: Long, density:     CFloat): Unit = extern
  def sge_phys3d_collider_set_friction(world:    Long, collider: Long, friction:    CFloat): Unit = extern
  def sge_phys3d_collider_set_restitution(world: Long, collider: Long, restitution: CFloat): Unit = extern
  def sge_phys3d_collider_set_sensor(world:      Long, collider: Long, sensor:      CInt):   Unit = extern

  // Collider getters
  def sge_phys3d_collider_get_density(world:                Long, collider: Long):                                                                                        CFloat = extern
  def sge_phys3d_collider_get_friction(world:               Long, collider: Long):                                                                                        CFloat = extern
  def sge_phys3d_collider_get_restitution(world:            Long, collider: Long):                                                                                        CFloat = extern
  def sge_phys3d_collider_is_sensor(world:                  Long, collider: Long):                                                                                        CInt   = extern
  def sge_phys3d_collider_set_enabled(world:                Long, collider: Long, enabled: CInt):                                                                         Unit   = extern
  def sge_phys3d_collider_is_enabled(world:                 Long, collider: Long):                                                                                        CInt   = extern
  def sge_phys3d_collider_get_position_wrt_parent(world:    Long, collider: Long, out:     Ptr[CFloat]):                                                                  Unit   = extern
  def sge_phys3d_collider_set_position_wrt_parent(world:    Long, collider: Long, x:       CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Unit   = extern
  def sge_phys3d_collider_get_position(world:               Long, collider: Long, out:     Ptr[CFloat]):                                                                  Unit   = extern
  def sge_phys3d_collider_get_shape_type(world:             Long, collider: Long):                                                                                        CInt   = extern
  def sge_phys3d_collider_get_aabb(world:                   Long, collider: Long, out:     Ptr[CFloat]):                                                                  Unit   = extern
  def sge_phys3d_collider_get_parent_body(world:            Long, collider: Long):                                                                                        Long   = extern
  def sge_phys3d_collider_get_mass(world:                   Long, collider: Long):                                                                                        CFloat = extern
  def sge_phys3d_collider_set_mass(world:                   Long, collider: Long, mass:    CFloat):                                                                       Unit   = extern
  def sge_phys3d_collider_set_contact_skin(world:           Long, collider: Long, skin:    CFloat):                                                                       Unit   = extern
  def sge_phys3d_collider_set_active_events(world:          Long, collider: Long, flags:   CInt):                                                                         Unit   = extern
  def sge_phys3d_collider_get_active_events(world:          Long, collider: Long):                                                                                        CInt   = extern
  def sge_phys3d_collider_set_active_collision_types(world: Long, collider: Long, flags:   CInt):                                                                         Unit   = extern
  def sge_phys3d_collider_get_active_collision_types(world: Long, collider: Long):                                                                                        CInt   = extern

  // Collision filtering
  def sge_phys3d_collider_set_collision_groups(world: Long, collider: Long, memberships: CInt, filter: CInt): Unit = extern
  def sge_phys3d_collider_get_collision_groups(world: Long, collider: Long, out:         Ptr[CInt]):          Unit = extern
  def sge_phys3d_collider_set_solver_groups(world:    Long, collider: Long, memberships: CInt, filter: CInt): Unit = extern
  def sge_phys3d_collider_get_solver_groups(world:    Long, collider: Long, out:         Ptr[CInt]):          Unit = extern

  // New shapes
  def sge_phys3d_create_heightfield_collider(world: Long, body: Long, heights: Ptr[CFloat], nrows: CInt, ncols: CInt, scaleX: CFloat, scaleY: CFloat, scaleZ: CFloat): Long = extern

  // Joints
  def sge_phys3d_create_fixed_joint(world:     Long, body1: Long, body2: Long):                                                                                                      Long = extern
  def sge_phys3d_create_rope_joint(world:      Long, body1: Long, body2: Long, maxDist:    CFloat):                                                                                  Long = extern
  def sge_phys3d_create_revolute_joint(world:  Long, body1: Long, body2: Long, ax:         CFloat, ay:        CFloat, az:      CFloat, axisX: CFloat, axisY: CFloat, axisZ: CFloat): Long = extern
  def sge_phys3d_create_prismatic_joint(world: Long, body1: Long, body2: Long, axisX:      CFloat, axisY:     CFloat, axisZ:   CFloat):                                              Long = extern
  def sge_phys3d_create_motor_joint(world:     Long, body1: Long, body2: Long):                                                                                                      Long = extern
  def sge_phys3d_create_spring_joint(world:    Long, body1: Long, body2: Long, restLength: CFloat, stiffness: CFloat, damping: CFloat):                                              Long = extern
  def sge_phys3d_destroy_joint(world:          Long, joint: Long):                                                                                                                   Unit = extern

  // Revolute joint limits/motors
  def sge_phys3d_revolute_joint_enable_limits(world:        Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys3d_revolute_joint_set_limits(world:           Long, joint: Long, lower:  CFloat, upper: CFloat): Unit   = extern
  def sge_phys3d_revolute_joint_get_limits(world:           Long, joint: Long, out:    Ptr[CFloat]):           Unit   = extern
  def sge_phys3d_revolute_joint_is_limit_enabled(world:     Long, joint: Long):                                CInt   = extern
  def sge_phys3d_revolute_joint_enable_motor(world:         Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys3d_revolute_joint_set_motor_speed(world:      Long, joint: Long, speed:  CFloat):                Unit   = extern
  def sge_phys3d_revolute_joint_set_max_motor_torque(world: Long, joint: Long, torque: CFloat):                Unit   = extern
  def sge_phys3d_revolute_joint_get_motor_speed(world:      Long, joint: Long):                                CFloat = extern
  def sge_phys3d_revolute_joint_get_angle(world:            Long, joint: Long):                                CFloat = extern
  def sge_phys3d_revolute_joint_get_max_motor_torque(world: Long, joint: Long):                                CFloat = extern

  // Prismatic joint limits/motors
  def sge_phys3d_prismatic_joint_enable_limits(world:       Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys3d_prismatic_joint_set_limits(world:          Long, joint: Long, lower:  CFloat, upper: CFloat): Unit   = extern
  def sge_phys3d_prismatic_joint_get_limits(world:          Long, joint: Long, out:    Ptr[CFloat]):           Unit   = extern
  def sge_phys3d_prismatic_joint_enable_motor(world:        Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys3d_prismatic_joint_set_motor_speed(world:     Long, joint: Long, speed:  CFloat):                Unit   = extern
  def sge_phys3d_prismatic_joint_set_max_motor_force(world: Long, joint: Long, force:  CFloat):                Unit   = extern
  def sge_phys3d_prismatic_joint_get_translation(world:     Long, joint: Long):                                CFloat = extern
  def sge_phys3d_prismatic_joint_get_motor_speed(world:     Long, joint: Long):                                CFloat = extern
  def sge_phys3d_prismatic_joint_get_max_motor_force(world: Long, joint: Long):                                CFloat = extern

  // Motor joint
  def sge_phys3d_motor_joint_set_linear_offset(world:     Long, joint: Long, x:      CFloat, y: CFloat, z: CFloat): Unit   = extern
  def sge_phys3d_motor_joint_get_linear_offset(world:     Long, joint: Long, out:    Ptr[CFloat]):                  Unit   = extern
  def sge_phys3d_motor_joint_set_max_force(world:         Long, joint: Long, force:  CFloat):                       Unit   = extern
  def sge_phys3d_motor_joint_set_max_torque(world:        Long, joint: Long, torque: CFloat):                       Unit   = extern
  def sge_phys3d_motor_joint_set_correction_factor(world: Long, joint: Long, factor: CFloat):                       Unit   = extern
  def sge_phys3d_motor_joint_get_max_force(world:         Long, joint: Long):                                       CFloat = extern
  def sge_phys3d_motor_joint_get_max_torque(world:        Long, joint: Long):                                       CFloat = extern
  def sge_phys3d_motor_joint_get_correction_factor(world: Long, joint: Long):                                       CFloat = extern

  // Rope joint
  def sge_phys3d_rope_joint_set_max_distance(world: Long, joint: Long, maxDist: CFloat): Unit   = extern
  def sge_phys3d_rope_joint_get_max_distance(world: Long, joint: Long):                  CFloat = extern

  // Spring joint
  def sge_phys3d_spring_joint_set_rest_length(world: Long, joint: Long, restLength: CFloat):                  Unit   = extern
  def sge_phys3d_spring_joint_get_rest_length(world: Long, joint: Long):                                      CFloat = extern
  def sge_phys3d_spring_joint_set_params(world:      Long, joint: Long, stiffness:  CFloat, damping: CFloat): Unit   = extern

  // Queries
  def sge_phys3d_ray_cast(world:   Long, ox:   CFloat, oy:   CFloat, oz:   CFloat, dx:   CFloat, dy:   CFloat, dz:   CFloat, maxDist:      CFloat, out:           Ptr[CFloat]): CInt = extern
  def sge_phys3d_query_aabb(world: Long, minX: CFloat, minY: CFloat, minZ: CFloat, maxX: CFloat, maxY: CFloat, maxZ: CFloat, outColliders: Ptr[Long], maxResults: CInt):        CInt = extern
  def sge_phys3d_query_point(world: Long, x: CFloat, y: CFloat, z: CFloat, outBodies: Ptr[Long], maxResults: CInt): CInt = extern
  def sge_phys3d_ray_cast_all(world:  Long, ox: CFloat, oy: CFloat, oz: CFloat, dx:  CFloat, dy: CFloat, dz: CFloat, maxDist: CFloat, outHits: Ptr[CFloat], maxHits: CInt): CInt = extern
  def sge_phys3d_project_point(world: Long, x:  CFloat, y:  CFloat, z:  CFloat, out: Ptr[CFloat]):                                                                          CInt = extern

  // Contact detail queries
  def sge_phys3d_contact_pair_count(world:  Long, collider1: Long, collider2: Long):                                    CInt = extern
  def sge_phys3d_contact_pair_points(world: Long, collider1: Long, collider2: Long, out: Ptr[CFloat], maxPoints: CInt): CInt = extern

  // Contact events
  def sge_phys3d_poll_contact_start_events(world: Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern
  def sge_phys3d_poll_contact_stop_events(world:  Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern

  // Intersection events
  def sge_phys3d_poll_intersection_start_events(world: Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern
  def sge_phys3d_poll_intersection_stop_events(world:  Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern

  // Solver parameters
  def sge_phys3d_world_set_num_solver_iterations(world:              Long, iters: CInt): Unit = extern
  def sge_phys3d_world_get_num_solver_iterations(world:              Long):              CInt = extern
  def sge_phys3d_world_set_num_additional_friction_iterations(world: Long, iters: CInt): Unit = extern
  def sge_phys3d_world_set_num_internal_pgs_iterations(world:        Long, iters: CInt): Unit = extern

  // Contact force events
  def sge_phys3d_poll_contact_force_events(world:                  Long, out1:     Ptr[Long], out2: Ptr[Long], outForce: Ptr[CFloat], max: CInt): CInt   = extern
  def sge_phys3d_collider_set_contact_force_event_threshold(world: Long, collider: Long, threshold: CFloat):                                      Unit   = extern
  def sge_phys3d_collider_get_contact_force_event_threshold(world: Long, collider: Long):                                                         CFloat = extern

  // Active hooks / one-way direction
  def sge_phys3d_collider_set_active_hooks(world:      Long, collider: Long, flags: CInt):                                                 Unit = extern
  def sge_phys3d_collider_get_active_hooks(world:      Long, collider: Long):                                                              CInt = extern
  def sge_phys3d_collider_set_one_way_direction(world: Long, collider: Long, nx:    CFloat, ny: CFloat, nz: CFloat, allowedAngle: CFloat): Unit = extern
  def sge_phys3d_collider_get_one_way_direction(world: Long, collider: Long, out:   Ptr[CFloat]):                                          CInt = extern
}

/** Scala Native implementation of [[PhysicsOps3d]] using `@extern` bindings to the Rust `sge_physics3d` library. */
private[platform] object PhysicsOpsNative3d extends PhysicsOps3d {

  // ─── World lifecycle ──────────────────────────────────────────────────

  override def createWorld(gravityX: Float, gravityY: Float, gravityZ: Float): Long =
    Physics3dC.sge_phys3d_create_world(gravityX, gravityY, gravityZ).toLong

  override def destroyWorld(world: Long): Unit =
    Physics3dC.sge_phys3d_destroy_world(world)

  override def worldStep(world: Long, dt: Float): Unit =
    Physics3dC.sge_phys3d_world_step(world, dt)

  override def worldSetGravity(world: Long, gx: Float, gy: Float, gz: Float): Unit =
    Physics3dC.sge_phys3d_world_set_gravity(world, gx, gy, gz)

  override def worldGetGravity(world: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_world_get_gravity(world, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  // ─── Rigid body ───────────────────────────────────────────────────────

  override def createDynamicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long =
    Physics3dC.sge_phys3d_create_dynamic_body(world, x, y, z, qx, qy, qz, qw).toLong

  override def createStaticBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long =
    Physics3dC.sge_phys3d_create_static_body(world, x, y, z, qx, qy, qz, qw).toLong

  override def createKinematicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long =
    Physics3dC.sge_phys3d_create_kinematic_body(world, x, y, z, qx, qy, qz, qw).toLong

  override def destroyBody(world: Long, body: Long): Unit =
    Physics3dC.sge_phys3d_destroy_body(world, body)

  override def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_body_get_position(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def bodyGetRotation(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](4)
    Physics3dC.sge_phys3d_body_get_rotation(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
    out(3) = buf(3)
  }

  override def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_body_get_linear_velocity(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def bodyGetAngularVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_body_get_angular_velocity(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def bodySetPosition(world: Long, body: Long, x: Float, y: Float, z: Float): Unit =
    Physics3dC.sge_phys3d_body_set_position(world, body, x, y, z)

  override def bodySetRotation(world: Long, body: Long, qx: Float, qy: Float, qz: Float, qw: Float): Unit =
    Physics3dC.sge_phys3d_body_set_rotation(world, body, qx, qy, qz, qw)

  override def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float, vz: Float): Unit =
    Physics3dC.sge_phys3d_body_set_linear_velocity(world, body, vx, vy, vz)

  override def bodySetAngularVelocity(world: Long, body: Long, wx: Float, wy: Float, wz: Float): Unit =
    Physics3dC.sge_phys3d_body_set_angular_velocity(world, body, wx, wy, wz)

  override def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float, fz: Float): Unit =
    Physics3dC.sge_phys3d_body_apply_force(world, body, fx, fy, fz)

  override def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float, iz: Float): Unit =
    Physics3dC.sge_phys3d_body_apply_impulse(world, body, ix, iy, iz)

  override def bodyApplyTorque(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit =
    Physics3dC.sge_phys3d_body_apply_torque(world, body, tx, ty, tz)

  override def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, fz: Float, px: Float, py: Float, pz: Float): Unit =
    Physics3dC.sge_phys3d_body_apply_force_at_point(world, body, fx, fy, fz, px, py, pz)

  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, iz: Float, px: Float, py: Float, pz: Float): Unit =
    Physics3dC.sge_phys3d_body_apply_impulse_at_point(world, body, ix, iy, iz, px, py, pz)

  override def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit =
    Physics3dC.sge_phys3d_body_set_linear_damping(world, body, damping)

  override def bodyGetLinearDamping(world: Long, body: Long): Float =
    Physics3dC.sge_phys3d_body_get_linear_damping(world, body)

  override def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit =
    Physics3dC.sge_phys3d_body_set_angular_damping(world, body, damping)

  override def bodyGetAngularDamping(world: Long, body: Long): Float =
    Physics3dC.sge_phys3d_body_get_angular_damping(world, body)

  override def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit =
    Physics3dC.sge_phys3d_body_set_gravity_scale(world, body, scale)

  override def bodyGetGravityScale(world: Long, body: Long): Float =
    Physics3dC.sge_phys3d_body_get_gravity_scale(world, body)

  override def bodyIsAwake(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_awake(world, body) != 0

  override def bodyWakeUp(world: Long, body: Long): Unit =
    Physics3dC.sge_phys3d_body_wake_up(world, body)

  override def bodySleep(world: Long, body: Long): Unit =
    Physics3dC.sge_phys3d_body_sleep(world, body)

  override def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit =
    Physics3dC.sge_phys3d_body_set_fixed_rotation(world, body, if (fixed) 1 else 0)

  override def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit =
    Physics3dC.sge_phys3d_body_enable_ccd(world, body, if (enable) 1 else 0)

  override def bodyIsCcdEnabled(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_ccd_enabled(world, body) != 0

  override def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit =
    Physics3dC.sge_phys3d_body_set_enabled(world, body, if (enabled) 1 else 0)

  override def bodyIsEnabled(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_enabled(world, body) != 0

  override def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit =
    Physics3dC.sge_phys3d_body_set_dominance_group(world, body, group)

  override def bodyGetDominanceGroup(world: Long, body: Long): Int =
    Physics3dC.sge_phys3d_body_get_dominance_group(world, body)

  override def bodyGetMass(world: Long, body: Long): Float =
    Physics3dC.sge_phys3d_body_get_mass(world, body)

  override def bodyRecomputeMassProperties(world: Long, body: Long): Unit =
    Physics3dC.sge_phys3d_body_recompute_mass_properties(world, body)

  override def bodyApplyTorqueImpulse(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit =
    Physics3dC.sge_phys3d_body_apply_torque_impulse(world, body, tx, ty, tz)

  override def bodyResetForces(world: Long, body: Long): Unit =
    Physics3dC.sge_phys3d_body_reset_forces(world, body)

  override def bodyResetTorques(world: Long, body: Long): Unit =
    Physics3dC.sge_phys3d_body_reset_torques(world, body)

  override def bodyGetType(world: Long, body: Long): Int =
    Physics3dC.sge_phys3d_body_get_type(world, body)

  override def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean, allowZ: Boolean): Unit =
    Physics3dC.sge_phys3d_body_set_enabled_translations(world, body, if (allowX) 1 else 0, if (allowY) 1 else 0, if (allowZ) 1 else 0)

  override def bodyIsTranslationLockedX(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_translation_locked_x(world, body) != 0

  override def bodyIsTranslationLockedY(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_translation_locked_y(world, body) != 0

  override def bodyIsTranslationLockedZ(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_translation_locked_z(world, body) != 0

  override def bodySetEnabledRotations(world: Long, body: Long, allowX: Boolean, allowY: Boolean, allowZ: Boolean): Unit =
    Physics3dC.sge_phys3d_body_set_enabled_rotations(world, body, if (allowX) 1 else 0, if (allowY) 1 else 0, if (allowZ) 1 else 0)

  override def bodyIsRotationLockedX(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_rotation_locked_x(world, body) != 0

  override def bodyIsRotationLockedY(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_rotation_locked_y(world, body) != 0

  override def bodyIsRotationLockedZ(world: Long, body: Long): Boolean =
    Physics3dC.sge_phys3d_body_is_rotation_locked_z(world, body) != 0

  override def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_body_get_world_center_of_mass(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_body_get_local_center_of_mass(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def bodyGetInertia(world: Long, body: Long): Float =
    Physics3dC.sge_phys3d_body_get_inertia(world, body)

  override def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, pz: Float, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_body_get_velocity_at_point(world, body, px, py, pz, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  // ─── Collider ─────────────────────────────────────────────────────────

  override def createSphereCollider(world: Long, body: Long, radius: Float): Long =
    Physics3dC.sge_phys3d_create_sphere_collider(world, body, radius).toLong

  override def createBoxCollider(world: Long, body: Long, hx: Float, hy: Float, hz: Float): Long =
    Physics3dC.sge_phys3d_create_box_collider(world, body, hx, hy, hz).toLong

  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    Physics3dC.sge_phys3d_create_capsule_collider(world, body, halfHeight, radius).toLong

  override def createCylinderCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    Physics3dC.sge_phys3d_create_cylinder_collider(world, body, halfHeight, radius).toLong

  override def createConeCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    Physics3dC.sge_phys3d_create_cone_collider(world, body, halfHeight, radius).toLong

  override def createConvexHullCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long =
    Physics3dC.sge_phys3d_create_convex_hull_collider(world, body, vertices.at(0), vertexCount).toLong

  override def createTriMeshCollider(
    world:       Long,
    body:        Long,
    vertices:    Array[Float],
    vertexCount: Int,
    indices:     Array[Int],
    indexCount:  Int
  ): Long =
    Physics3dC.sge_phys3d_create_trimesh_collider(world, body, vertices.at(0), vertexCount, indices.at(0), indexCount).toLong

  override def destroyCollider(world: Long, collider: Long): Unit =
    Physics3dC.sge_phys3d_destroy_collider(world, collider)

  override def colliderSetDensity(world: Long, collider: Long, density: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_density(world, collider, density)

  override def colliderSetFriction(world: Long, collider: Long, friction: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_friction(world, collider, friction)

  override def colliderSetRestitution(world: Long, collider: Long, restitution: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_restitution(world, collider, restitution)

  override def colliderSetSensor(world: Long, collider: Long, sensor: Boolean): Unit =
    Physics3dC.sge_phys3d_collider_set_sensor(world, collider, if (sensor) 1 else 0)

  override def colliderGetDensity(world: Long, collider: Long): Float =
    Physics3dC.sge_phys3d_collider_get_density(world, collider)

  override def colliderGetFriction(world: Long, collider: Long): Float =
    Physics3dC.sge_phys3d_collider_get_friction(world, collider)

  override def colliderGetRestitution(world: Long, collider: Long): Float =
    Physics3dC.sge_phys3d_collider_get_restitution(world, collider)

  override def colliderIsSensor(world: Long, collider: Long): Boolean =
    Physics3dC.sge_phys3d_collider_is_sensor(world, collider) != 0

  override def colliderSetEnabled(world: Long, collider: Long, enabled: Boolean): Unit =
    Physics3dC.sge_phys3d_collider_set_enabled(world, collider, if (enabled) 1 else 0)

  override def colliderIsEnabled(world: Long, collider: Long): Boolean =
    Physics3dC.sge_phys3d_collider_is_enabled(world, collider) != 0

  override def colliderGetPositionWrtParent(world: Long, collider: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](7)
    Physics3dC.sge_phys3d_collider_get_position_wrt_parent(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
    out(3) = buf(3)
    out(4) = buf(4)
    out(5) = buf(5)
    out(6) = buf(6)
  }

  override def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_position_wrt_parent(world, collider, x, y, z, qx, qy, qz, qw)

  override def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](7)
    Physics3dC.sge_phys3d_collider_get_position(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
    out(3) = buf(3)
    out(4) = buf(4)
    out(5) = buf(5)
    out(6) = buf(6)
  }

  override def colliderGetShapeType(world: Long, collider: Long): Int =
    Physics3dC.sge_phys3d_collider_get_shape_type(world, collider)

  override def colliderGetAabb(world: Long, collider: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](6)
    Physics3dC.sge_phys3d_collider_get_aabb(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
    out(3) = buf(3)
    out(4) = buf(4)
    out(5) = buf(5)
  }

  override def colliderGetParentBody(world: Long, collider: Long): Long =
    Physics3dC.sge_phys3d_collider_get_parent_body(world, collider).toLong

  override def colliderGetMass(world: Long, collider: Long): Float =
    Physics3dC.sge_phys3d_collider_get_mass(world, collider)

  override def colliderSetMass(world: Long, collider: Long, mass: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_mass(world, collider, mass)

  override def colliderSetContactSkin(world: Long, collider: Long, skin: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_contact_skin(world, collider, skin)

  override def colliderSetActiveEvents(world: Long, collider: Long, flags: Int): Unit =
    Physics3dC.sge_phys3d_collider_set_active_events(world, collider, flags)

  override def colliderGetActiveEvents(world: Long, collider: Long): Int =
    Physics3dC.sge_phys3d_collider_get_active_events(world, collider)

  override def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags: Int): Unit =
    Physics3dC.sge_phys3d_collider_set_active_collision_types(world, collider, flags)

  override def colliderGetActiveCollisionTypes(world: Long, collider: Long): Int =
    Physics3dC.sge_phys3d_collider_get_active_collision_types(world, collider)

  // ─── Collision filtering ──────────────────────────────────────────────

  override def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    Physics3dC.sge_phys3d_collider_set_collision_groups(world, collider, memberships, filter)

  override def colliderGetCollisionGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val buf = stackalloc[CInt](2)
    Physics3dC.sge_phys3d_collider_get_collision_groups(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def colliderSetSolverGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    Physics3dC.sge_phys3d_collider_set_solver_groups(world, collider, memberships, filter)

  override def colliderGetSolverGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val buf = stackalloc[CInt](2)
    Physics3dC.sge_phys3d_collider_get_solver_groups(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  // ─── New shapes ───────────────────────────────────────────────────────

  override def createHeightfieldCollider(
    world:   Long,
    body:    Long,
    heights: Array[Float],
    nrows:   Int,
    ncols:   Int,
    scaleX:  Float,
    scaleY:  Float,
    scaleZ:  Float
  ): Long =
    Physics3dC.sge_phys3d_create_heightfield_collider(world, body, heights.at(0), nrows, ncols, scaleX, scaleY, scaleZ).toLong

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    Physics3dC.sge_phys3d_create_fixed_joint(world, body1, body2).toLong

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long =
    Physics3dC.sge_phys3d_create_rope_joint(world, body1, body2, maxDist).toLong

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float, anchorZ: Float, axisX: Float, axisY: Float, axisZ: Float): Long =
    Physics3dC.sge_phys3d_create_revolute_joint(world, body1, body2, anchorX, anchorY, anchorZ, axisX, axisY, axisZ).toLong

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float, axisZ: Float): Long =
    Physics3dC.sge_phys3d_create_prismatic_joint(world, body1, body2, axisX, axisY, axisZ).toLong

  override def createMotorJoint(world: Long, body1: Long, body2: Long): Long =
    Physics3dC.sge_phys3d_create_motor_joint(world, body1, body2).toLong

  override def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long =
    Physics3dC.sge_phys3d_create_spring_joint(world, body1, body2, restLength, stiffness, damping).toLong

  override def destroyJoint(world: Long, joint: Long): Unit =
    Physics3dC.sge_phys3d_destroy_joint(world, joint)

  // ─── Revolute joint limits and motors ─────────────────────────────────

  override def revoluteJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit =
    Physics3dC.sge_phys3d_revolute_joint_enable_limits(world, joint, if (enable) 1 else 0)

  override def revoluteJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit =
    Physics3dC.sge_phys3d_revolute_joint_set_limits(world, joint, lower, upper)

  override def revoluteJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    Physics3dC.sge_phys3d_revolute_joint_get_limits(world, joint, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def revoluteJointIsLimitEnabled(world: Long, joint: Long): Boolean =
    Physics3dC.sge_phys3d_revolute_joint_is_limit_enabled(world, joint) != 0

  override def revoluteJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit =
    Physics3dC.sge_phys3d_revolute_joint_enable_motor(world, joint, if (enable) 1 else 0)

  override def revoluteJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit =
    Physics3dC.sge_phys3d_revolute_joint_set_motor_speed(world, joint, speed)

  override def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float): Unit =
    Physics3dC.sge_phys3d_revolute_joint_set_max_motor_torque(world, joint, torque)

  override def revoluteJointGetMotorSpeed(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_revolute_joint_get_motor_speed(world, joint)

  override def revoluteJointGetAngle(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_revolute_joint_get_angle(world, joint)

  override def revoluteJointGetMaxMotorTorque(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_revolute_joint_get_max_motor_torque(world, joint)

  // ─── Prismatic joint limits and motors ────────────────────────────────

  override def prismaticJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit =
    Physics3dC.sge_phys3d_prismatic_joint_enable_limits(world, joint, if (enable) 1 else 0)

  override def prismaticJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit =
    Physics3dC.sge_phys3d_prismatic_joint_set_limits(world, joint, lower, upper)

  override def prismaticJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    Physics3dC.sge_phys3d_prismatic_joint_get_limits(world, joint, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def prismaticJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit =
    Physics3dC.sge_phys3d_prismatic_joint_enable_motor(world, joint, if (enable) 1 else 0)

  override def prismaticJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit =
    Physics3dC.sge_phys3d_prismatic_joint_set_motor_speed(world, joint, speed)

  override def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force: Float): Unit =
    Physics3dC.sge_phys3d_prismatic_joint_set_max_motor_force(world, joint, force)

  override def prismaticJointGetTranslation(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_prismatic_joint_get_translation(world, joint)

  override def prismaticJointGetMotorSpeed(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_prismatic_joint_get_motor_speed(world, joint)

  override def prismaticJointGetMaxMotorForce(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_prismatic_joint_get_max_motor_force(world, joint)

  // ─── Motor joint ──────────────────────────────────────────────────────

  override def motorJointSetLinearOffset(world: Long, joint: Long, x: Float, y: Float, z: Float): Unit =
    Physics3dC.sge_phys3d_motor_joint_set_linear_offset(world, joint, x, y, z)

  override def motorJointGetLinearOffset(world: Long, joint: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    Physics3dC.sge_phys3d_motor_joint_get_linear_offset(world, joint, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def motorJointSetMaxForce(world: Long, joint: Long, force: Float): Unit =
    Physics3dC.sge_phys3d_motor_joint_set_max_force(world, joint, force)

  override def motorJointSetMaxTorque(world: Long, joint: Long, torque: Float): Unit =
    Physics3dC.sge_phys3d_motor_joint_set_max_torque(world, joint, torque)

  override def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float): Unit =
    Physics3dC.sge_phys3d_motor_joint_set_correction_factor(world, joint, factor)

  override def motorJointGetMaxForce(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_motor_joint_get_max_force(world, joint)

  override def motorJointGetMaxTorque(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_motor_joint_get_max_torque(world, joint)

  override def motorJointGetCorrectionFactor(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_motor_joint_get_correction_factor(world, joint)

  // ─── Rope joint ───────────────────────────────────────────────────────

  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit =
    Physics3dC.sge_phys3d_rope_joint_set_max_distance(world, joint, maxDist)

  override def ropeJointGetMaxDistance(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_rope_joint_get_max_distance(world, joint)

  // ─── Spring joint ─────────────────────────────────────────────────────

  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float): Unit =
    Physics3dC.sge_phys3d_spring_joint_set_rest_length(world, joint, restLength)

  override def springJointGetRestLength(world: Long, joint: Long): Float =
    Physics3dC.sge_phys3d_spring_joint_get_rest_length(world, joint)

  override def springJointSetParams(world: Long, joint: Long, stiffness: Float, damping: Float): Unit =
    Physics3dC.sge_phys3d_spring_joint_set_params(world, joint, stiffness, damping)

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
    val buf = stackalloc[CFloat](9)
    val hit = Physics3dC.sge_phys3d_ray_cast(world, originX, originY, originZ, dirX, dirY, dirZ, maxDist, buf) != 0
    if (hit) {
      var i = 0
      while (i < 9) {
        out(i) = buf(i)
        i += 1
      }
    }
    hit
  }

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
    val buf   = stackalloc[Long](maxResults)
    val count = Physics3dC.sge_phys3d_query_aabb(world, minX, minY, minZ, maxX, maxY, maxZ, buf, maxResults)
    var i     = 0
    while (i < count) {
      outColliders(i) = buf(i).toLong
      i += 1
    }
    count
  }

  override def queryPoint(world: Long, x: Float, y: Float, z: Float, outBodies: Array[Long], maxResults: Int): Int = {
    val buf   = stackalloc[Long](maxResults)
    val count = Physics3dC.sge_phys3d_query_point(world, x, y, z, buf, maxResults)
    var i     = 0
    while (i < count) {
      outBodies(i) = buf(i).toLong
      i += 1
    }
    count
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
    val totalFloats = maxHits * 9
    val buf         = stackalloc[CFloat](totalFloats)
    val count       = Physics3dC.sge_phys3d_ray_cast_all(world, originX, originY, originZ, dirX, dirY, dirZ, maxDist, buf, maxHits)
    val copyCount   = count * 9
    var i           = 0
    while (i < copyCount) {
      outHits(i) = buf(i)
      i += 1
    }
    count
  }

  override def projectPoint(world: Long, x: Float, y: Float, z: Float, out: Array[Float]): Boolean = {
    val buf    = stackalloc[CFloat](6)
    val result = Physics3dC.sge_phys3d_project_point(world, x, y, z, buf) != 0
    if (result) {
      out(0) = buf(0)
      out(1) = buf(1)
      out(2) = buf(2)
      out(3) = buf(3)
      out(4) = buf(4)
      out(5) = buf(5)
    }
    result
  }

  // ─── Contact detail queries ───────────────────────────────────────────

  override def contactPairCount(world: Long, collider1: Long, collider2: Long): Int =
    Physics3dC.sge_phys3d_contact_pair_count(world, collider1, collider2)

  override def contactPairPoints(
    world:     Long,
    collider1: Long,
    collider2: Long,
    out:       Array[Float],
    maxPoints: Int
  ): Int = {
    val totalFloats = maxPoints * 7
    val buf         = stackalloc[CFloat](totalFloats)
    val count       = Physics3dC.sge_phys3d_contact_pair_points(world, collider1, collider2, buf, maxPoints)
    val copyCount   = count * 7
    var i           = 0
    while (i < copyCount) {
      out(i) = buf(i)
      i += 1
    }
    count
  }

  // ─── Contact events ───────────────────────────────────────────────────

  override def pollContactStartEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val buf1  = stackalloc[Long](maxEvents)
    val buf2  = stackalloc[Long](maxEvents)
    val count = Physics3dC.sge_phys3d_poll_contact_start_events(world, buf1, buf2, maxEvents)
    var i     = 0
    while (i < count) {
      outCollider1(i) = buf1(i).toLong
      outCollider2(i) = buf2(i).toLong
      i += 1
    }
    count
  }

  override def pollContactStopEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val buf1  = stackalloc[Long](maxEvents)
    val buf2  = stackalloc[Long](maxEvents)
    val count = Physics3dC.sge_phys3d_poll_contact_stop_events(world, buf1, buf2, maxEvents)
    var i     = 0
    while (i < count) {
      outCollider1(i) = buf1(i).toLong
      outCollider2(i) = buf2(i).toLong
      i += 1
    }
    count
  }

  // ─── Intersection events ──────────────────────────────────────────────

  override def pollIntersectionStartEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val buf1  = stackalloc[Long](maxEvents)
    val buf2  = stackalloc[Long](maxEvents)
    val count = Physics3dC.sge_phys3d_poll_intersection_start_events(world, buf1, buf2, maxEvents)
    var i     = 0
    while (i < count) {
      outCollider1(i) = buf1(i).toLong
      outCollider2(i) = buf2(i).toLong
      i += 1
    }
    count
  }

  override def pollIntersectionStopEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    maxEvents:    Int
  ): Int = {
    val buf1  = stackalloc[Long](maxEvents)
    val buf2  = stackalloc[Long](maxEvents)
    val count = Physics3dC.sge_phys3d_poll_intersection_stop_events(world, buf1, buf2, maxEvents)
    var i     = 0
    while (i < count) {
      outCollider1(i) = buf1(i).toLong
      outCollider2(i) = buf2(i).toLong
      i += 1
    }
    count
  }

  // ─── Solver parameters ────────────────────────────────────────────────

  override def worldSetNumSolverIterations(world: Long, iters: Int): Unit =
    Physics3dC.sge_phys3d_world_set_num_solver_iterations(world, iters)

  override def worldGetNumSolverIterations(world: Long): Int =
    Physics3dC.sge_phys3d_world_get_num_solver_iterations(world)

  override def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit =
    Physics3dC.sge_phys3d_world_set_num_additional_friction_iterations(world, iters)

  override def worldSetNumInternalPgsIterations(world: Long, iters: Int): Unit =
    Physics3dC.sge_phys3d_world_set_num_internal_pgs_iterations(world, iters)

  // ─── Contact force events ─────────────────────────────────────────────

  override def pollContactForceEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    outForce:     Array[Float],
    maxEvents:    Int
  ): Int = {
    val buf1  = stackalloc[Long](maxEvents)
    val buf2  = stackalloc[Long](maxEvents)
    val bufF  = stackalloc[CFloat](maxEvents)
    val count = Physics3dC.sge_phys3d_poll_contact_force_events(world, buf1, buf2, bufF, maxEvents)
    var i     = 0
    while (i < count) {
      outCollider1(i) = buf1(i).toLong
      outCollider2(i) = buf2(i).toLong
      outForce(i) = bufF(i)
      i += 1
    }
    count
  }

  override def colliderSetContactForceEventThreshold(world: Long, collider: Long, threshold: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_contact_force_event_threshold(world, collider, threshold)

  override def colliderGetContactForceEventThreshold(world: Long, collider: Long): Float =
    Physics3dC.sge_phys3d_collider_get_contact_force_event_threshold(world, collider)

  // ─── Active hooks / one-way direction ─────────────────────────────────

  override def colliderSetActiveHooks(world: Long, collider: Long, flags: Int): Unit =
    Physics3dC.sge_phys3d_collider_set_active_hooks(world, collider, flags)

  override def colliderGetActiveHooks(world: Long, collider: Long): Int =
    Physics3dC.sge_phys3d_collider_get_active_hooks(world, collider)

  override def colliderSetOneWayDirection(world: Long, collider: Long, nx: Float, ny: Float, nz: Float, allowedAngle: Float): Unit =
    Physics3dC.sge_phys3d_collider_set_one_way_direction(world, collider, nx, ny, nz, allowedAngle)

  override def colliderGetOneWayDirection(world: Long, collider: Long, out: Array[Float]): Boolean = {
    val buf    = stackalloc[CFloat](4)
    val result = Physics3dC.sge_phys3d_collider_get_one_way_direction(world, collider, buf)
    if (result != 0) {
      out(0) = buf(0)
      out(1) = buf(1)
      out(2) = buf(2)
      out(3) = buf(3)
    }
    result != 0
  }
}
