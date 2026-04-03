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

  // Collider
  def sge_phys_create_circle_collider(world:   Long, body:     Long, radius:      CFloat):                           Long = extern
  def sge_phys_create_box_collider(world:      Long, body:     Long, halfWidth:   CFloat, halfHeight:       CFloat): Long = extern
  def sge_phys_create_capsule_collider(world:  Long, body:     Long, halfHeight:  CFloat, radius:           CFloat): Long = extern
  def sge_phys_create_polygon_collider(world:  Long, body:     Long, vertices:    Ptr[CFloat], vertexCount: CInt):   Long = extern
  def sge_phys_destroy_collider(world:         Long, collider: Long):                                                Unit = extern
  def sge_phys_collider_set_density(world:     Long, collider: Long, density:     CFloat):                           Unit = extern
  def sge_phys_collider_set_friction(world:    Long, collider: Long, friction:    CFloat):                           Unit = extern
  def sge_phys_collider_set_restitution(world: Long, collider: Long, restitution: CFloat):                           Unit = extern
  def sge_phys_collider_set_sensor(world:      Long, collider: Long, sensor:      CInt):                             Unit = extern

  // Joints
  def sge_phys_create_revolute_joint(world:  Long, body1: Long, body2: Long, anchorX: CFloat, anchorY: CFloat): Long = extern
  def sge_phys_create_prismatic_joint(world: Long, body1: Long, body2: Long, axisX:   CFloat, axisY:   CFloat): Long = extern
  def sge_phys_create_fixed_joint(world:     Long, body1: Long, body2: Long):                                   Long = extern
  def sge_phys_destroy_joint(world:          Long, joint: Long):                                                Unit = extern

  // Queries
  def sge_phys_ray_cast(world:    Long, originX: CFloat, originY: CFloat, dirX:      CFloat, dirY:          CFloat, maxDist: CFloat, out: Ptr[CFloat]): CInt = extern
  def sge_phys_query_point(world: Long, x:       CFloat, y:       CFloat, outBodies: Ptr[Long], maxResults: CInt):                                      CInt = extern

  // Contact events
  def sge_phys_poll_contact_start_events(world: Long, outCollider1: Ptr[Long], outCollider2: Ptr[Long], maxEvents: CInt): CInt = extern
  def sge_phys_poll_contact_stop_events(world:  Long, outCollider1: Ptr[Long], outCollider2: Ptr[Long], maxEvents: CInt): CInt = extern
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

  // ─── Collider ─────────────────────────────────────────────────────────

  override def createCircleCollider(world: Long, body: Long, radius: Float): Long =
    PhysicsC.sge_phys_create_circle_collider(world, body, radius).toLong

  override def createBoxCollider(world: Long, body: Long, halfWidth: Float, halfHeight: Float): Long =
    PhysicsC.sge_phys_create_box_collider(world, body, halfWidth, halfHeight).toLong

  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    PhysicsC.sge_phys_create_capsule_collider(world, body, halfHeight, radius).toLong

  override def createPolygonCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long =
    PhysicsC.sge_phys_create_polygon_collider(world, body, vertices.at(0), vertexCount).toLong

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

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long =
    PhysicsC.sge_phys_create_revolute_joint(world, body1, body2, anchorX, anchorY).toLong

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long =
    PhysicsC.sge_phys_create_prismatic_joint(world, body1, body2, axisX, axisY).toLong

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    PhysicsC.sge_phys_create_fixed_joint(world, body1, body2).toLong

  override def destroyJoint(world: Long, joint: Long): Unit =
    PhysicsC.sge_phys_destroy_joint(world, joint)

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
}
