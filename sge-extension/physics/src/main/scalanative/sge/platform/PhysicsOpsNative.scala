/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: Scala Native @extern bindings to Rust C ABI
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import scala.scalanative.unsafe.*

@link("sge_physics")
@extern
private object PhysicsC {
  // World lifecycle
  def sge_phys_create_world(gravityX:   CFloat, gravityY: CFloat):             Long = extern
  def sge_phys_destroy_world(world:     Long):                                 Unit = extern
  def sge_phys_world_step(world:        Long, dt:         CFloat):             Unit = extern
  def sge_phys_world_set_gravity(world: Long, gx:         CFloat, gy: CFloat): Unit = extern
  def sge_phys_world_get_gravity(world: Long, out:        Ptr[CFloat]):        Unit = extern

  // Rigid body
  def sge_phys_create_dynamic_body(world:       Long, x:    CFloat, y:     CFloat, angle: CFloat): Long   = extern
  def sge_phys_create_static_body(world:        Long, x:    CFloat, y:     CFloat, angle: CFloat): Long   = extern
  def sge_phys_create_kinematic_body(world:     Long, x:    CFloat, y:     CFloat, angle: CFloat): Long   = extern
  def sge_phys_destroy_body(world:              Long, body: Long):                                 Unit   = extern
  def sge_phys_body_get_position(world:         Long, body: Long, out:     Ptr[CFloat]):           Unit   = extern
  def sge_phys_body_get_angle(world:            Long, body: Long):                                 CFloat = extern
  def sge_phys_body_get_linear_velocity(world:  Long, body: Long, out:     Ptr[CFloat]):           Unit   = extern
  def sge_phys_body_get_angular_velocity(world: Long, body: Long):                                 CFloat = extern
  def sge_phys_body_set_position(world:         Long, body: Long, x:       CFloat, y:     CFloat): Unit   = extern
  def sge_phys_body_set_angle(world:            Long, body: Long, angle:   CFloat):                Unit   = extern
  def sge_phys_body_set_linear_velocity(world:  Long, body: Long, vx:      CFloat, vy:    CFloat): Unit   = extern
  def sge_phys_body_set_angular_velocity(world: Long, body: Long, omega:   CFloat):                Unit   = extern
  def sge_phys_body_apply_force(world:          Long, body: Long, fx:      CFloat, fy:    CFloat): Unit   = extern
  def sge_phys_body_apply_impulse(world:        Long, body: Long, ix:      CFloat, iy:    CFloat): Unit   = extern
  def sge_phys_body_apply_torque(world:         Long, body: Long, torque:  CFloat):                Unit   = extern
  def sge_phys_body_set_linear_damping(world:   Long, body: Long, damping: CFloat):                Unit   = extern
  def sge_phys_body_set_angular_damping(world:  Long, body: Long, damping: CFloat):                Unit   = extern
  def sge_phys_body_set_gravity_scale(world:    Long, body: Long, scale:   CFloat):                Unit   = extern
  def sge_phys_body_is_awake(world:             Long, body: Long):                                 CInt   = extern
  def sge_phys_body_wake_up(world:              Long, body: Long):                                 Unit   = extern
  def sge_phys_body_set_fixed_rotation(world:   Long, body: Long, fixed:   CInt):                  Unit   = extern

  // Body forces at point / additional properties
  def sge_phys_body_apply_force_at_point(world:     Long, body: Long, fx:      CFloat, fy: CFloat, px:  CFloat, py: CFloat): Unit   = extern
  def sge_phys_body_apply_impulse_at_point(world:   Long, body: Long, ix:      CFloat, iy: CFloat, px:  CFloat, py: CFloat): Unit   = extern
  def sge_phys_body_apply_torque_impulse(world:     Long, body: Long, impulse: CFloat):                                      Unit   = extern
  def sge_phys_body_reset_forces(world:             Long, body: Long):                                                       Unit   = extern
  def sge_phys_body_reset_torques(world:            Long, body: Long):                                                       Unit   = extern
  def sge_phys_body_get_linear_damping(world:       Long, body: Long):                                                       CFloat = extern
  def sge_phys_body_get_angular_damping(world:      Long, body: Long):                                                       CFloat = extern
  def sge_phys_body_get_gravity_scale(world:        Long, body: Long):                                                       CFloat = extern
  def sge_phys_body_get_type(world:                 Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_set_enabled(world:              Long, body: Long, enabled: CInt):                                        Unit   = extern
  def sge_phys_body_is_enabled(world:               Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_set_enabled_translations(world: Long, body: Long, x:       CInt, y:    CInt):                            Unit   = extern
  def sge_phys_body_is_translation_locked_x(world:  Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_is_translation_locked_y(world:  Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_is_rotation_locked(world:       Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_set_dominance_group(world:      Long, body: Long, group:   CInt):                                        Unit   = extern
  def sge_phys_body_get_dominance_group(world:      Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_get_world_center_of_mass(world: Long, body: Long, out:     Ptr[CFloat]):                                 Unit   = extern
  def sge_phys_body_enable_ccd(world:               Long, body: Long, enable:  CInt):                                        Unit   = extern
  def sge_phys_body_is_ccd_enabled(world:           Long, body: Long):                                                       CInt   = extern
  def sge_phys_body_sleep(world:                    Long, body: Long):                                                       Unit   = extern
  def sge_phys_body_get_velocity_at_point(world:    Long, body: Long, px:      CFloat, py: CFloat, out: Ptr[CFloat]):        Unit   = extern

  // Collider
  def sge_phys_create_circle_collider(world:   Long, body:     Long, radius:      CFloat):                                                   Long = extern
  def sge_phys_create_box_collider(world:      Long, body:     Long, halfWidth:   CFloat, halfHeight:       CFloat):                         Long = extern
  def sge_phys_create_capsule_collider(world:  Long, body:     Long, halfHeight:  CFloat, radius:           CFloat):                         Long = extern
  def sge_phys_create_polygon_collider(world:  Long, body:     Long, vertices:    Ptr[CFloat], vertexCount: CInt):                           Long = extern
  def sge_phys_create_segment_collider(world:  Long, body:     Long, x1:          CFloat, y1:               CFloat, x2: CFloat, y2: CFloat): Long = extern
  def sge_phys_create_polyline_collider(world: Long, body:     Long, vertices:    Ptr[CFloat], vertexCount: CInt):                           Long = extern
  def sge_phys_destroy_collider(world:         Long, collider: Long):                                                                        Unit = extern
  def sge_phys_collider_set_density(world:     Long, collider: Long, density:     CFloat):                                                   Unit = extern
  def sge_phys_collider_set_friction(world:    Long, collider: Long, friction:    CFloat):                                                   Unit = extern
  def sge_phys_collider_set_restitution(world: Long, collider: Long, restitution: CFloat):                                                   Unit = extern
  def sge_phys_collider_set_sensor(world:      Long, collider: Long, sensor:      CInt):                                                     Unit = extern

  // Collider getters/properties
  def sge_phys_collider_get_density(world:                Long, collider: Long):                                            CFloat = extern
  def sge_phys_collider_get_friction(world:               Long, collider: Long):                                            CFloat = extern
  def sge_phys_collider_get_restitution(world:            Long, collider: Long):                                            CFloat = extern
  def sge_phys_collider_is_sensor(world:                  Long, collider: Long):                                            CInt   = extern
  def sge_phys_collider_set_enabled(world:                Long, collider: Long, enabled: CInt):                             Unit   = extern
  def sge_phys_collider_is_enabled(world:                 Long, collider: Long):                                            CInt   = extern
  def sge_phys_collider_get_position_wrt_parent(world:    Long, collider: Long, out:     Ptr[CFloat]):                      Unit   = extern
  def sge_phys_collider_set_position_wrt_parent(world:    Long, collider: Long, x:       CFloat, y: CFloat, angle: CFloat): Unit   = extern
  def sge_phys_collider_get_position(world:               Long, collider: Long, out:     Ptr[CFloat]):                      Unit   = extern
  def sge_phys_collider_get_shape_type(world:             Long, collider: Long):                                            CInt   = extern
  def sge_phys_collider_get_aabb(world:                   Long, collider: Long, out:     Ptr[CFloat]):                      Unit   = extern
  def sge_phys_collider_get_parent_body(world:            Long, collider: Long):                                            Long   = extern
  def sge_phys_collider_get_mass(world:                   Long, collider: Long):                                            CFloat = extern
  def sge_phys_collider_set_mass(world:                   Long, collider: Long, mass:    CFloat):                           Unit   = extern
  def sge_phys_collider_set_contact_skin(world:           Long, collider: Long, skin:    CFloat):                           Unit   = extern
  def sge_phys_collider_set_active_events(world:          Long, collider: Long, flags:   CInt):                             Unit   = extern
  def sge_phys_collider_get_active_events(world:          Long, collider: Long):                                            CInt   = extern
  def sge_phys_collider_set_active_collision_types(world: Long, collider: Long, flags:   CInt):                             Unit   = extern
  def sge_phys_collider_get_active_collision_types(world: Long, collider: Long):                                            CInt   = extern

  // New shapes
  def sge_phys_create_trimesh_collider(world:     Long, body: Long, vertices: Ptr[CFloat], vertexCount: CInt, indices: Ptr[CInt], indexCount: CInt):   Long = extern
  def sge_phys_create_heightfield_collider(world: Long, body: Long, heights:  Ptr[CFloat], numCols:     CInt, scaleX:  CFloat, scaleY:        CFloat): Long = extern

  // Collision filtering
  def sge_phys_collider_set_collision_groups(world: Long, collider: Long, memberships: CInt, filter: CInt): Unit = extern
  def sge_phys_collider_get_collision_groups(world: Long, collider: Long, out:         Ptr[CInt]):          Unit = extern
  def sge_phys_collider_set_solver_groups(world:    Long, collider: Long, memberships: CInt, filter: CInt): Unit = extern
  def sge_phys_collider_get_solver_groups(world:    Long, collider: Long, out:         Ptr[CInt]):          Unit = extern

  // Joints
  def sge_phys_create_revolute_joint(world:  Long, body1: Long, body2: Long, anchorX: CFloat, anchorY: CFloat): Long = extern
  def sge_phys_create_prismatic_joint(world: Long, body1: Long, body2: Long, axisX:   CFloat, axisY:   CFloat): Long = extern
  def sge_phys_create_fixed_joint(world:     Long, body1: Long, body2: Long):                                   Long = extern
  def sge_phys_create_rope_joint(world:      Long, body1: Long, body2: Long, maxDist: CFloat):                  Long = extern
  def sge_phys_destroy_joint(world:          Long, joint: Long):                                                Unit = extern

  // Revolute joint limits/motors
  def sge_phys_revolute_joint_enable_limits(world:        Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys_revolute_joint_set_limits(world:           Long, joint: Long, lower:  CFloat, upper: CFloat): Unit   = extern
  def sge_phys_revolute_joint_get_limits(world:           Long, joint: Long, out:    Ptr[CFloat]):           Unit   = extern
  def sge_phys_revolute_joint_is_limit_enabled(world:     Long, joint: Long):                                CInt   = extern
  def sge_phys_revolute_joint_enable_motor(world:         Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys_revolute_joint_set_motor_speed(world:      Long, joint: Long, speed:  CFloat):                Unit   = extern
  def sge_phys_revolute_joint_set_max_motor_torque(world: Long, joint: Long, torque: CFloat):                Unit   = extern
  def sge_phys_revolute_joint_get_motor_speed(world:      Long, joint: Long):                                CFloat = extern
  def sge_phys_revolute_joint_get_angle(world:            Long, joint: Long):                                CFloat = extern

  // Prismatic joint limits/motors
  def sge_phys_prismatic_joint_enable_limits(world:       Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys_prismatic_joint_set_limits(world:          Long, joint: Long, lower:  CFloat, upper: CFloat): Unit   = extern
  def sge_phys_prismatic_joint_get_limits(world:          Long, joint: Long, out:    Ptr[CFloat]):           Unit   = extern
  def sge_phys_prismatic_joint_enable_motor(world:        Long, joint: Long, enable: CInt):                  Unit   = extern
  def sge_phys_prismatic_joint_set_motor_speed(world:     Long, joint: Long, speed:  CFloat):                Unit   = extern
  def sge_phys_prismatic_joint_set_max_motor_force(world: Long, joint: Long, force:  CFloat):                Unit   = extern
  def sge_phys_prismatic_joint_get_translation(world:     Long, joint: Long):                                CFloat = extern

  // Motor joint
  def sge_phys_create_motor_joint(world:                Long, body1: Long, body2:  Long):              Long   = extern
  def sge_phys_motor_joint_set_linear_offset(world:     Long, joint: Long, x:      CFloat, y: CFloat): Unit   = extern
  def sge_phys_motor_joint_get_linear_offset(world:     Long, joint: Long, out:    Ptr[CFloat]):       Unit   = extern
  def sge_phys_motor_joint_set_angular_offset(world:    Long, joint: Long, angle:  CFloat):            Unit   = extern
  def sge_phys_motor_joint_get_angular_offset(world:    Long, joint: Long):                            CFloat = extern
  def sge_phys_motor_joint_set_max_force(world:         Long, joint: Long, force:  CFloat):            Unit   = extern
  def sge_phys_motor_joint_set_max_torque(world:        Long, joint: Long, torque: CFloat):            Unit   = extern
  def sge_phys_motor_joint_set_correction_factor(world: Long, joint: Long, factor: CFloat):            Unit   = extern

  // Joint getters
  def sge_phys_revolute_joint_get_max_motor_torque(world: Long, joint: Long): CFloat = extern
  def sge_phys_prismatic_joint_get_motor_speed(world:     Long, joint: Long): CFloat = extern
  def sge_phys_prismatic_joint_get_max_motor_force(world: Long, joint: Long): CFloat = extern
  def sge_phys_motor_joint_get_max_force(world:           Long, joint: Long): CFloat = extern
  def sge_phys_motor_joint_get_max_torque(world:          Long, joint: Long): CFloat = extern
  def sge_phys_motor_joint_get_correction_factor(world:   Long, joint: Long): CFloat = extern

  // Spring joint
  def sge_phys_create_spring_joint(world:          Long, body1: Long, body2:      Long, restLength: CFloat, stiffness: CFloat, damping: CFloat): Long   = extern
  def sge_phys_spring_joint_set_rest_length(world: Long, joint: Long, restLength: CFloat):                                                       Unit   = extern
  def sge_phys_spring_joint_get_rest_length(world: Long, joint: Long):                                                                           CFloat = extern
  def sge_phys_spring_joint_set_params(world:      Long, joint: Long, stiffness:  CFloat, damping:  CFloat):                                     Unit   = extern

  // Rope joint
  def sge_phys_rope_joint_set_max_distance(world: Long, joint: Long, maxDist: CFloat): Unit   = extern
  def sge_phys_rope_joint_get_max_distance(world: Long, joint: Long):                  CFloat = extern

  // Body mass/inertia
  def sge_phys_body_get_mass(world:                  Long, body: Long):                   CFloat = extern
  def sge_phys_body_get_inertia(world:               Long, body: Long):                   CFloat = extern
  def sge_phys_body_get_local_center_of_mass(world:  Long, body: Long, out: Ptr[CFloat]): Unit   = extern
  def sge_phys_body_recompute_mass_properties(world: Long, body: Long):                   Unit   = extern

  // Queries
  def sge_phys_query_aabb(world:  Long, minX:    CFloat, minY:    CFloat, maxX:      CFloat, maxY:          CFloat, outColliders: Ptr[Long], maxResults: CInt):        CInt = extern
  def sge_phys_ray_cast(world:    Long, originX: CFloat, originY: CFloat, dirX:      CFloat, dirY:          CFloat, maxDist:      CFloat, out:           Ptr[CFloat]): CInt = extern
  def sge_phys_query_point(world: Long, x:       CFloat, y:       CFloat, outBodies: Ptr[Long], maxResults: CInt):                                                     CInt = extern

  // Contact detail queries
  def sge_phys_contact_pair_count(world:  Long, collider1: Long, collider2: Long):                                    CInt = extern
  def sge_phys_contact_pair_points(world: Long, collider1: Long, collider2: Long, out: Ptr[CFloat], maxPoints: CInt): CInt = extern

  // Contact events
  def sge_phys_poll_contact_start_events(world: Long, outCollider1: Ptr[Long], outCollider2: Ptr[Long], maxEvents: CInt): CInt = extern
  def sge_phys_poll_contact_stop_events(world:  Long, outCollider1: Ptr[Long], outCollider2: Ptr[Long], maxEvents: CInt): CInt = extern

  // Advanced queries
  def sge_phys_cast_shape(world: Long, shapeType: CInt, shapeParams: Ptr[CFloat], originX: CFloat, originY: CFloat, dirX: CFloat, dirY: CFloat, maxDist: CFloat, out: Ptr[CFloat]): CInt = extern
  def sge_phys_ray_cast_all(world:  Long, ox: CFloat, oy: CFloat, dx:  CFloat, dy: CFloat, maxDist: CFloat, outHits: Ptr[CFloat], maxHits: CInt): CInt = extern
  def sge_phys_project_point(world: Long, x:  CFloat, y:  CFloat, out: Ptr[CFloat]):                                                              CInt = extern

  // Intersection events
  def sge_phys_poll_intersection_start_events(world: Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern
  def sge_phys_poll_intersection_stop_events(world:  Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern

  // Solver parameters
  def sge_phys_world_set_num_solver_iterations(world:              Long, iters: CInt): Unit = extern
  def sge_phys_world_get_num_solver_iterations(world:              Long):              CInt = extern
  def sge_phys_world_set_num_additional_friction_iterations(world: Long, iters: CInt): Unit = extern
  def sge_phys_world_set_num_internal_pgs_iterations(world:        Long, iters: CInt): Unit = extern

  // Shape intersection
  def sge_phys_intersect_shape(world: Long, shapeType: CInt, shapeParams: Ptr[CFloat], posX: CFloat, posY: CFloat, angle: CFloat, outColliders: Ptr[Long], maxResults: CInt): CInt = extern

  // Contact force events
  def sge_phys_poll_contact_force_events(world:                  Long, out1:     Ptr[Long], out2: Ptr[Long], outForce: Ptr[CFloat], max: CInt): CInt   = extern
  def sge_phys_collider_set_contact_force_event_threshold(world: Long, collider: Long, threshold: CFloat):                                      Unit   = extern
  def sge_phys_collider_get_contact_force_event_threshold(world: Long, collider: Long):                                                         CFloat = extern

  // Active hooks / one-way direction
  def sge_phys_collider_set_active_hooks(world:      Long, collider: Long, flags: CInt):                                     Unit = extern
  def sge_phys_collider_get_active_hooks(world:      Long, collider: Long):                                                  CInt = extern
  def sge_phys_collider_set_one_way_direction(world: Long, collider: Long, nx:    CFloat, ny: CFloat, allowedAngle: CFloat): Unit = extern
  def sge_phys_collider_get_one_way_direction(world: Long, collider: Long, out:   Ptr[CFloat]):                              CInt = extern
}

/** Scala Native implementation of [[PhysicsOps]] using `@extern` bindings to the Rust `sge_physics` library. */
private[platform] object PhysicsOpsNative extends PhysicsOps {

  // ─── World lifecycle ──────────────────────────────────────────────────

  override def createWorld(gravityX: Float, gravityY: Float): Long =
    PhysicsC.sge_phys_create_world(gravityX, gravityY).toLong

  override def destroyWorld(world: Long): Unit =
    PhysicsC.sge_phys_destroy_world(world)

  override def worldStep(world: Long, dt: Float): Unit =
    PhysicsC.sge_phys_world_step(world, dt)

  override def worldSetGravity(world: Long, gx: Float, gy: Float): Unit =
    PhysicsC.sge_phys_world_set_gravity(world, gx, gy)

  override def worldGetGravity(world: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_world_get_gravity(world, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  // ─── Rigid body ───────────────────────────────────────────────────────

  override def createDynamicBody(world: Long, x: Float, y: Float, angle: Float): Long =
    PhysicsC.sge_phys_create_dynamic_body(world, x, y, angle).toLong

  override def createStaticBody(world: Long, x: Float, y: Float, angle: Float): Long =
    PhysicsC.sge_phys_create_static_body(world, x, y, angle).toLong

  override def createKinematicBody(world: Long, x: Float, y: Float, angle: Float): Long =
    PhysicsC.sge_phys_create_kinematic_body(world, x, y, angle).toLong

  override def destroyBody(world: Long, body: Long): Unit =
    PhysicsC.sge_phys_destroy_body(world, body)

  override def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_body_get_position(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def bodyGetAngle(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_angle(world, body)

  override def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_body_get_linear_velocity(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def bodyGetAngularVelocity(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_angular_velocity(world, body)

  override def bodySetPosition(world: Long, body: Long, x: Float, y: Float): Unit =
    PhysicsC.sge_phys_body_set_position(world, body, x, y)

  override def bodySetAngle(world: Long, body: Long, angle: Float): Unit =
    PhysicsC.sge_phys_body_set_angle(world, body, angle)

  override def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float): Unit =
    PhysicsC.sge_phys_body_set_linear_velocity(world, body, vx, vy)

  override def bodySetAngularVelocity(world: Long, body: Long, omega: Float): Unit =
    PhysicsC.sge_phys_body_set_angular_velocity(world, body, omega)

  override def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float): Unit =
    PhysicsC.sge_phys_body_apply_force(world, body, fx, fy)

  override def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float): Unit =
    PhysicsC.sge_phys_body_apply_impulse(world, body, ix, iy)

  override def bodyApplyTorque(world: Long, body: Long, torque: Float): Unit =
    PhysicsC.sge_phys_body_apply_torque(world, body, torque)

  override def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit =
    PhysicsC.sge_phys_body_set_linear_damping(world, body, damping)

  override def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit =
    PhysicsC.sge_phys_body_set_angular_damping(world, body, damping)

  override def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit =
    PhysicsC.sge_phys_body_set_gravity_scale(world, body, scale)

  override def bodyIsAwake(world: Long, body: Long): Boolean =
    PhysicsC.sge_phys_body_is_awake(world, body) != 0

  override def bodyWakeUp(world: Long, body: Long): Unit =
    PhysicsC.sge_phys_body_wake_up(world, body)

  override def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit =
    PhysicsC.sge_phys_body_set_fixed_rotation(world, body, if (fixed) 1 else 0)

  override def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, px: Float, py: Float): Unit =
    PhysicsC.sge_phys_body_apply_force_at_point(world, body, fx, fy, px, py)

  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, px: Float, py: Float): Unit =
    PhysicsC.sge_phys_body_apply_impulse_at_point(world, body, ix, iy, px, py)

  override def bodyApplyTorqueImpulse(world: Long, body: Long, impulse: Float): Unit =
    PhysicsC.sge_phys_body_apply_torque_impulse(world, body, impulse)

  override def bodyResetForces(world: Long, body: Long): Unit =
    PhysicsC.sge_phys_body_reset_forces(world, body)

  override def bodyResetTorques(world: Long, body: Long): Unit =
    PhysicsC.sge_phys_body_reset_torques(world, body)

  override def bodyGetLinearDamping(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_linear_damping(world, body)

  override def bodyGetAngularDamping(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_angular_damping(world, body)

  override def bodyGetGravityScale(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_gravity_scale(world, body)

  override def bodyGetType(world: Long, body: Long): Int =
    PhysicsC.sge_phys_body_get_type(world, body)

  override def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit =
    PhysicsC.sge_phys_body_set_enabled(world, body, if (enabled) 1 else 0)

  override def bodyIsEnabled(world: Long, body: Long): Boolean =
    PhysicsC.sge_phys_body_is_enabled(world, body) != 0

  override def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean): Unit =
    PhysicsC.sge_phys_body_set_enabled_translations(world, body, if (allowX) 1 else 0, if (allowY) 1 else 0)

  override def bodyIsTranslationLockedX(world: Long, body: Long): Boolean =
    PhysicsC.sge_phys_body_is_translation_locked_x(world, body) != 0

  override def bodyIsTranslationLockedY(world: Long, body: Long): Boolean =
    PhysicsC.sge_phys_body_is_translation_locked_y(world, body) != 0

  override def bodyIsRotationLocked(world: Long, body: Long): Boolean =
    PhysicsC.sge_phys_body_is_rotation_locked(world, body) != 0

  override def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit =
    PhysicsC.sge_phys_body_set_dominance_group(world, body, group)

  override def bodyGetDominanceGroup(world: Long, body: Long): Int =
    PhysicsC.sge_phys_body_get_dominance_group(world, body)

  override def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_body_get_world_center_of_mass(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit =
    PhysicsC.sge_phys_body_enable_ccd(world, body, if (enable) 1 else 0)

  override def bodyIsCcdEnabled(world: Long, body: Long): Boolean =
    PhysicsC.sge_phys_body_is_ccd_enabled(world, body) != 0

  override def bodySleep(world: Long, body: Long): Unit =
    PhysicsC.sge_phys_body_sleep(world, body)

  override def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_body_get_velocity_at_point(world, body, px, py, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  // ─── Collider ─────────────────────────────────────────────────────────

  override def createCircleCollider(world: Long, body: Long, radius: Float): Long =
    PhysicsC.sge_phys_create_circle_collider(world, body, radius).toLong

  override def createBoxCollider(world: Long, body: Long, halfWidth: Float, halfHeight: Float): Long =
    PhysicsC.sge_phys_create_box_collider(world, body, halfWidth, halfHeight).toLong

  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    PhysicsC.sge_phys_create_capsule_collider(world, body, halfHeight, radius).toLong

  override def createPolygonCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long =
    PhysicsC.sge_phys_create_polygon_collider(world, body, vertices.at(0), vertexCount).toLong

  override def createSegmentCollider(world: Long, body: Long, x1: Float, y1: Float, x2: Float, y2: Float): Long =
    PhysicsC.sge_phys_create_segment_collider(world, body, x1, y1, x2, y2).toLong

  override def createPolylineCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long =
    PhysicsC.sge_phys_create_polyline_collider(world, body, vertices.at(0), vertexCount).toLong

  override def destroyCollider(world: Long, collider: Long): Unit =
    PhysicsC.sge_phys_destroy_collider(world, collider)

  override def colliderSetDensity(world: Long, collider: Long, density: Float): Unit =
    PhysicsC.sge_phys_collider_set_density(world, collider, density)

  override def colliderSetFriction(world: Long, collider: Long, friction: Float): Unit =
    PhysicsC.sge_phys_collider_set_friction(world, collider, friction)

  override def colliderSetRestitution(world: Long, collider: Long, restitution: Float): Unit =
    PhysicsC.sge_phys_collider_set_restitution(world, collider, restitution)

  override def colliderSetSensor(world: Long, collider: Long, sensor: Boolean): Unit =
    PhysicsC.sge_phys_collider_set_sensor(world, collider, if (sensor) 1 else 0)

  override def colliderGetDensity(world: Long, collider: Long): Float =
    PhysicsC.sge_phys_collider_get_density(world, collider)

  override def colliderGetFriction(world: Long, collider: Long): Float =
    PhysicsC.sge_phys_collider_get_friction(world, collider)

  override def colliderGetRestitution(world: Long, collider: Long): Float =
    PhysicsC.sge_phys_collider_get_restitution(world, collider)

  override def colliderIsSensor(world: Long, collider: Long): Boolean =
    PhysicsC.sge_phys_collider_is_sensor(world, collider) != 0

  override def colliderSetEnabled(world: Long, collider: Long, enabled: Boolean): Unit =
    PhysicsC.sge_phys_collider_set_enabled(world, collider, if (enabled) 1 else 0)

  override def colliderIsEnabled(world: Long, collider: Long): Boolean =
    PhysicsC.sge_phys_collider_is_enabled(world, collider) != 0

  override def colliderGetPositionWrtParent(world: Long, collider: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    PhysicsC.sge_phys_collider_get_position_wrt_parent(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, angle: Float): Unit =
    PhysicsC.sge_phys_collider_set_position_wrt_parent(world, collider, x, y, angle)

  override def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](3)
    PhysicsC.sge_phys_collider_get_position(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
  }

  override def colliderGetShapeType(world: Long, collider: Long): Int =
    PhysicsC.sge_phys_collider_get_shape_type(world, collider)

  override def colliderGetAabb(world: Long, collider: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](4)
    PhysicsC.sge_phys_collider_get_aabb(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
    out(2) = buf(2)
    out(3) = buf(3)
  }

  override def colliderGetParentBody(world: Long, collider: Long): Long =
    PhysicsC.sge_phys_collider_get_parent_body(world, collider).toLong

  override def colliderGetMass(world: Long, collider: Long): Float =
    PhysicsC.sge_phys_collider_get_mass(world, collider)

  override def colliderSetMass(world: Long, collider: Long, mass: Float): Unit =
    PhysicsC.sge_phys_collider_set_mass(world, collider, mass)

  override def colliderSetContactSkin(world: Long, collider: Long, skin: Float): Unit =
    PhysicsC.sge_phys_collider_set_contact_skin(world, collider, skin)

  override def colliderSetActiveEvents(world: Long, collider: Long, flags: Int): Unit =
    PhysicsC.sge_phys_collider_set_active_events(world, collider, flags)

  override def colliderGetActiveEvents(world: Long, collider: Long): Int =
    PhysicsC.sge_phys_collider_get_active_events(world, collider)

  override def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags: Int): Unit =
    PhysicsC.sge_phys_collider_set_active_collision_types(world, collider, flags)

  override def colliderGetActiveCollisionTypes(world: Long, collider: Long): Int =
    PhysicsC.sge_phys_collider_get_active_collision_types(world, collider)

  // ─── New shapes ───────────────────────────────────────────────────────

  override def createTriMeshCollider(
    world:       Long,
    body:        Long,
    vertices:    Array[Float],
    vertexCount: Int,
    indices:     Array[Int],
    indexCount:  Int
  ): Long =
    PhysicsC.sge_phys_create_trimesh_collider(world, body, vertices.at(0), vertexCount, indices.at(0), indexCount).toLong

  override def createHeightfieldCollider(
    world:   Long,
    body:    Long,
    heights: Array[Float],
    numCols: Int,
    scaleX:  Float,
    scaleY:  Float
  ): Long =
    PhysicsC.sge_phys_create_heightfield_collider(world, body, heights.at(0), numCols, scaleX, scaleY).toLong

  // ─── Collision filtering ──────────────────────────────────────────────

  override def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    PhysicsC.sge_phys_collider_set_collision_groups(world, collider, memberships, filter)

  override def colliderGetCollisionGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val buf = stackalloc[CInt](2)
    PhysicsC.sge_phys_collider_get_collision_groups(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def colliderSetSolverGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    PhysicsC.sge_phys_collider_set_solver_groups(world, collider, memberships, filter)

  override def colliderGetSolverGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val buf = stackalloc[CInt](2)
    PhysicsC.sge_phys_collider_get_solver_groups(world, collider, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long =
    PhysicsC.sge_phys_create_revolute_joint(world, body1, body2, anchorX, anchorY).toLong

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long =
    PhysicsC.sge_phys_create_prismatic_joint(world, body1, body2, axisX, axisY).toLong

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    PhysicsC.sge_phys_create_fixed_joint(world, body1, body2).toLong

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long =
    PhysicsC.sge_phys_create_rope_joint(world, body1, body2, maxDist).toLong

  override def destroyJoint(world: Long, joint: Long): Unit =
    PhysicsC.sge_phys_destroy_joint(world, joint)

  // ─── Revolute joint limits and motors ─────────────────────────────────

  override def revoluteJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit =
    PhysicsC.sge_phys_revolute_joint_enable_limits(world, joint, if (enable) 1 else 0)

  override def revoluteJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit =
    PhysicsC.sge_phys_revolute_joint_set_limits(world, joint, lower, upper)

  override def revoluteJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_revolute_joint_get_limits(world, joint, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def revoluteJointIsLimitEnabled(world: Long, joint: Long): Boolean =
    PhysicsC.sge_phys_revolute_joint_is_limit_enabled(world, joint) != 0

  override def revoluteJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit =
    PhysicsC.sge_phys_revolute_joint_enable_motor(world, joint, if (enable) 1 else 0)

  override def revoluteJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit =
    PhysicsC.sge_phys_revolute_joint_set_motor_speed(world, joint, speed)

  override def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float): Unit =
    PhysicsC.sge_phys_revolute_joint_set_max_motor_torque(world, joint, torque)

  override def revoluteJointGetMotorSpeed(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_revolute_joint_get_motor_speed(world, joint)

  override def revoluteJointGetAngle(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_revolute_joint_get_angle(world, joint)

  // ─── Prismatic joint limits and motors ────────────────────────────────

  override def prismaticJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit =
    PhysicsC.sge_phys_prismatic_joint_enable_limits(world, joint, if (enable) 1 else 0)

  override def prismaticJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit =
    PhysicsC.sge_phys_prismatic_joint_set_limits(world, joint, lower, upper)

  override def prismaticJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_prismatic_joint_get_limits(world, joint, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def prismaticJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit =
    PhysicsC.sge_phys_prismatic_joint_enable_motor(world, joint, if (enable) 1 else 0)

  override def prismaticJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit =
    PhysicsC.sge_phys_prismatic_joint_set_motor_speed(world, joint, speed)

  override def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force: Float): Unit =
    PhysicsC.sge_phys_prismatic_joint_set_max_motor_force(world, joint, force)

  override def prismaticJointGetTranslation(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_prismatic_joint_get_translation(world, joint)

  // ─── Motor joint ───────────────────────────────────────────────────────

  override def createMotorJoint(world: Long, body1: Long, body2: Long): Long =
    PhysicsC.sge_phys_create_motor_joint(world, body1, body2).toLong

  override def motorJointSetLinearOffset(world: Long, joint: Long, x: Float, y: Float): Unit =
    PhysicsC.sge_phys_motor_joint_set_linear_offset(world, joint, x, y)

  override def motorJointGetLinearOffset(world: Long, joint: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_motor_joint_get_linear_offset(world, joint, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def motorJointSetAngularOffset(world: Long, joint: Long, angle: Float): Unit =
    PhysicsC.sge_phys_motor_joint_set_angular_offset(world, joint, angle)

  override def motorJointGetAngularOffset(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_motor_joint_get_angular_offset(world, joint)

  override def motorJointSetMaxForce(world: Long, joint: Long, force: Float): Unit =
    PhysicsC.sge_phys_motor_joint_set_max_force(world, joint, force)

  override def motorJointSetMaxTorque(world: Long, joint: Long, torque: Float): Unit =
    PhysicsC.sge_phys_motor_joint_set_max_torque(world, joint, torque)

  override def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float): Unit =
    PhysicsC.sge_phys_motor_joint_set_correction_factor(world, joint, factor)

  // ─── Joint getters ────────────────────────────────────────────────────

  override def revoluteJointGetMaxMotorTorque(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_revolute_joint_get_max_motor_torque(world, joint)

  override def prismaticJointGetMotorSpeed(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_prismatic_joint_get_motor_speed(world, joint)

  override def prismaticJointGetMaxMotorForce(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_prismatic_joint_get_max_motor_force(world, joint)

  override def motorJointGetMaxForce(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_motor_joint_get_max_force(world, joint)

  override def motorJointGetMaxTorque(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_motor_joint_get_max_torque(world, joint)

  override def motorJointGetCorrectionFactor(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_motor_joint_get_correction_factor(world, joint)

  // ─── Spring joint ─────────────────────────────────────────────────────

  override def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long =
    PhysicsC.sge_phys_create_spring_joint(world, body1, body2, restLength, stiffness, damping).toLong

  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float): Unit =
    PhysicsC.sge_phys_spring_joint_set_rest_length(world, joint, restLength)

  override def springJointGetRestLength(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_spring_joint_get_rest_length(world, joint)

  override def springJointSetParams(world: Long, joint: Long, stiffness: Float, damping: Float): Unit =
    PhysicsC.sge_phys_spring_joint_set_params(world, joint, stiffness, damping)

  // ─── Rope joint ───────────────────────────────────────────────────────

  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit =
    PhysicsC.sge_phys_rope_joint_set_max_distance(world, joint, maxDist)

  override def ropeJointGetMaxDistance(world: Long, joint: Long): Float =
    PhysicsC.sge_phys_rope_joint_get_max_distance(world, joint)

  // ─── Body mass/inertia ────────────────────────────────────────────────

  override def bodyGetMass(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_mass(world, body)

  override def bodyGetInertia(world: Long, body: Long): Float =
    PhysicsC.sge_phys_body_get_inertia(world, body)

  override def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val buf = stackalloc[CFloat](2)
    PhysicsC.sge_phys_body_get_local_center_of_mass(world, body, buf)
    out(0) = buf(0)
    out(1) = buf(1)
  }

  override def bodyRecomputeMassProperties(world: Long, body: Long): Unit =
    PhysicsC.sge_phys_body_recompute_mass_properties(world, body)

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
    val buf   = stackalloc[Long](maxResults)
    val count = PhysicsC.sge_phys_query_aabb(world, minX, minY, maxX, maxY, buf, maxResults)
    var i     = 0
    while (i < count) {
      outColliders(i) = buf(i).toLong
      i += 1
    }
    count
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
    val buf = stackalloc[CFloat](7)
    val hit = PhysicsC.sge_phys_ray_cast(world, originX, originY, dirX, dirY, maxDist, buf) != 0
    if (hit) {
      var i = 0
      while (i < 7) {
        out(i) = buf(i)
        i += 1
      }
    }
    hit
  }

  override def queryPoint(world: Long, x: Float, y: Float, outBodies: Array[Long], maxResults: Int): Int = {
    val buf   = stackalloc[Long](maxResults)
    val count = PhysicsC.sge_phys_query_point(world, x, y, buf, maxResults)
    var i     = 0
    while (i < count) {
      outBodies(i) = buf(i).toLong
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
    val count = PhysicsC.sge_phys_poll_contact_start_events(world, buf1, buf2, maxEvents)
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
    val count = PhysicsC.sge_phys_poll_contact_stop_events(world, buf1, buf2, maxEvents)
    var i     = 0
    while (i < count) {
      outCollider1(i) = buf1(i).toLong
      outCollider2(i) = buf2(i).toLong
      i += 1
    }
    count
  }

  // ─── Contact detail queries ───────────────────────────────────────────

  override def contactPairCount(world: Long, collider1: Long, collider2: Long): Int =
    PhysicsC.sge_phys_contact_pair_count(world, collider1, collider2)

  override def contactPairPoints(
    world:     Long,
    collider1: Long,
    collider2: Long,
    out:       Array[Float],
    maxPoints: Int
  ): Int = {
    val totalFloats = maxPoints * 5
    val buf         = stackalloc[CFloat](totalFloats)
    val count       = PhysicsC.sge_phys_contact_pair_points(world, collider1, collider2, buf, maxPoints)
    val copyCount   = count * 5
    var i           = 0
    while (i < copyCount) {
      out(i) = buf(i)
      i += 1
    }
    count
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
    val buf = stackalloc[CFloat](7)
    val hit = PhysicsC.sge_phys_cast_shape(world, shapeType, shapeParams.at(0), originX, originY, dirX, dirY, maxDist, buf) != 0
    if (hit) {
      var i = 0
      while (i < 7) {
        out(i) = buf(i)
        i += 1
      }
    }
    hit
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
    val totalFloats = maxHits * 7
    val buf         = stackalloc[CFloat](totalFloats)
    val count       = PhysicsC.sge_phys_ray_cast_all(world, originX, originY, dirX, dirY, maxDist, buf, maxHits)
    val copyCount   = count * 7
    var i           = 0
    while (i < copyCount) {
      outHits(i) = buf(i)
      i += 1
    }
    count
  }

  override def projectPoint(world: Long, x: Float, y: Float, out: Array[Float]): Boolean = {
    val buf    = stackalloc[CFloat](5)
    val result = PhysicsC.sge_phys_project_point(world, x, y, buf) != 0
    if (result) {
      out(0) = buf(0)
      out(1) = buf(1)
      out(2) = buf(2)
      out(3) = buf(3)
      out(4) = buf(4)
    }
    result
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
    val count = PhysicsC.sge_phys_poll_intersection_start_events(world, buf1, buf2, maxEvents)
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
    val count = PhysicsC.sge_phys_poll_intersection_stop_events(world, buf1, buf2, maxEvents)
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
    PhysicsC.sge_phys_world_set_num_solver_iterations(world, iters)

  override def worldGetNumSolverIterations(world: Long): Int =
    PhysicsC.sge_phys_world_get_num_solver_iterations(world)

  override def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit =
    PhysicsC.sge_phys_world_set_num_additional_friction_iterations(world, iters)

  override def worldSetNumInternalPgsIterations(world: Long, iters: Int): Unit =
    PhysicsC.sge_phys_world_set_num_internal_pgs_iterations(world, iters)

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
    val buf   = stackalloc[Long](maxResults)
    val count = PhysicsC.sge_phys_intersect_shape(world, shapeType, shapeParams.at(0), posX, posY, angle, buf, maxResults)
    var i     = 0
    while (i < count) {
      outColliders(i) = buf(i).toLong
      i += 1
    }
    count
  }

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
    val count = PhysicsC.sge_phys_poll_contact_force_events(world, buf1, buf2, bufF, maxEvents)
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
    PhysicsC.sge_phys_collider_set_contact_force_event_threshold(world, collider, threshold)

  override def colliderGetContactForceEventThreshold(world: Long, collider: Long): Float =
    PhysicsC.sge_phys_collider_get_contact_force_event_threshold(world, collider)

  // ─── Active hooks / one-way direction ─────────────────────────────────

  override def colliderSetActiveHooks(world: Long, collider: Long, flags: Int): Unit =
    PhysicsC.sge_phys_collider_set_active_hooks(world, collider, flags)

  override def colliderGetActiveHooks(world: Long, collider: Long): Int =
    PhysicsC.sge_phys_collider_get_active_hooks(world, collider)

  override def colliderSetOneWayDirection(world: Long, collider: Long, nx: Float, ny: Float, allowedAngle: Float): Unit =
    PhysicsC.sge_phys_collider_set_one_way_direction(world, collider, nx, ny, allowedAngle)

  override def colliderGetOneWayDirection(world: Long, collider: Long, out: Array[Float]): Boolean = {
    val buf    = stackalloc[CFloat](3)
    val result = PhysicsC.sge_phys_collider_get_one_way_direction(world, collider, buf)
    if (result != 0) {
      out(0) = buf(0)
      out(1) = buf(1)
      out(2) = buf(2)
    }
    result != 0
  }
}
