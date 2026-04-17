/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: Scala Native @extern bindings to Rust C ABI
 *   Idiom: split packages
 */
package sge
package platform

import scala.scalanative.unsafe.*

@link("sge_physics3d")
@extern
private object Physics3dC {
  // World lifecycle
  def sge_phys3d_create_world(gx: CFloat, gy: CFloat, gz: CFloat): Long = extern
  def sge_phys3d_destroy_world(world: Long): Unit = extern
  def sge_phys3d_world_step(world: Long, dt: CFloat): Unit = extern
  def sge_phys3d_world_set_gravity(world: Long, gx: CFloat, gy: CFloat, gz: CFloat): Unit = extern
  def sge_phys3d_world_get_gravity(world: Long, out: Ptr[CFloat]): Unit = extern

  // Rigid body creation — position (x, y, z) + quaternion (qx, qy, qz, qw)
  def sge_phys3d_create_dynamic_body(world: Long, x: CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Long = extern
  def sge_phys3d_create_static_body(world: Long, x: CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Long = extern
  def sge_phys3d_create_kinematic_body(world: Long, x: CFloat, y: CFloat, z: CFloat, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Long = extern
  def sge_phys3d_destroy_body(world: Long, body: Long): Unit = extern

  // Body getters — positions are [x,y,z], rotations are [qx,qy,qz,qw], velocities are [vx,vy,vz]
  def sge_phys3d_body_get_position(world: Long, body: Long, out: Ptr[CFloat]): Unit = extern
  def sge_phys3d_body_get_rotation(world: Long, body: Long, out: Ptr[CFloat]): Unit = extern
  def sge_phys3d_body_get_linear_velocity(world: Long, body: Long, out: Ptr[CFloat]): Unit = extern
  def sge_phys3d_body_get_angular_velocity(world: Long, body: Long, out: Ptr[CFloat]): Unit = extern

  // Body setters
  def sge_phys3d_body_set_position(world: Long, body: Long, x: CFloat, y: CFloat, z: CFloat): Unit = extern
  def sge_phys3d_body_set_rotation(world: Long, body: Long, qx: CFloat, qy: CFloat, qz: CFloat, qw: CFloat): Unit = extern
  def sge_phys3d_body_set_linear_velocity(world: Long, body: Long, vx: CFloat, vy: CFloat, vz: CFloat): Unit = extern
  def sge_phys3d_body_set_angular_velocity(world: Long, body: Long, wx: CFloat, wy: CFloat, wz: CFloat): Unit = extern

  // Body forces (3D vectors)
  def sge_phys3d_body_apply_force(world: Long, body: Long, fx: CFloat, fy: CFloat, fz: CFloat): Unit = extern
  def sge_phys3d_body_apply_impulse(world: Long, body: Long, ix: CFloat, iy: CFloat, iz: CFloat): Unit = extern
  def sge_phys3d_body_apply_torque(world: Long, body: Long, tx: CFloat, ty: CFloat, tz: CFloat): Unit = extern
  def sge_phys3d_body_apply_force_at_point(world: Long, body: Long, fx: CFloat, fy: CFloat, fz: CFloat, px: CFloat, py: CFloat, pz: CFloat): Unit = extern
  def sge_phys3d_body_apply_impulse_at_point(world: Long, body: Long, ix: CFloat, iy: CFloat, iz: CFloat, px: CFloat, py: CFloat, pz: CFloat): Unit = extern

  // Body properties
  def sge_phys3d_body_set_linear_damping(world: Long, body: Long, damping: CFloat): Unit = extern
  def sge_phys3d_body_get_linear_damping(world: Long, body: Long): CFloat = extern
  def sge_phys3d_body_set_angular_damping(world: Long, body: Long, damping: CFloat): Unit = extern
  def sge_phys3d_body_get_angular_damping(world: Long, body: Long): CFloat = extern
  def sge_phys3d_body_set_gravity_scale(world: Long, body: Long, scale: CFloat): Unit = extern
  def sge_phys3d_body_get_gravity_scale(world: Long, body: Long): CFloat = extern
  def sge_phys3d_body_is_awake(world: Long, body: Long): CInt = extern
  def sge_phys3d_body_wake_up(world: Long, body: Long): Unit = extern
  def sge_phys3d_body_sleep(world: Long, body: Long): Unit = extern
  def sge_phys3d_body_set_fixed_rotation(world: Long, body: Long, fixed: CInt): Unit = extern
  def sge_phys3d_body_enable_ccd(world: Long, body: Long, enable: CInt): Unit = extern
  def sge_phys3d_body_is_ccd_enabled(world: Long, body: Long): CInt = extern
  def sge_phys3d_body_set_enabled(world: Long, body: Long, enabled: CInt): Unit = extern
  def sge_phys3d_body_is_enabled(world: Long, body: Long): CInt = extern
  def sge_phys3d_body_set_dominance_group(world: Long, body: Long, group: CInt): Unit = extern
  def sge_phys3d_body_get_dominance_group(world: Long, body: Long): CInt = extern
  def sge_phys3d_body_get_mass(world: Long, body: Long): CFloat = extern
  def sge_phys3d_body_recompute_mass_properties(world: Long, body: Long): Unit = extern

  // Collider creation (3D shapes)
  def sge_phys3d_create_sphere_collider(world: Long, body: Long, radius: CFloat): Long = extern
  def sge_phys3d_create_box_collider(world: Long, body: Long, hx: CFloat, hy: CFloat, hz: CFloat): Long = extern
  def sge_phys3d_create_capsule_collider(world: Long, body: Long, halfHeight: CFloat, radius: CFloat): Long = extern
  def sge_phys3d_create_cylinder_collider(world: Long, body: Long, halfHeight: CFloat, radius: CFloat): Long = extern
  def sge_phys3d_create_cone_collider(world: Long, body: Long, halfHeight: CFloat, radius: CFloat): Long = extern
  def sge_phys3d_create_convex_hull_collider(world: Long, body: Long, vertices: Ptr[CFloat], vertexCount: CInt): Long = extern
  def sge_phys3d_create_trimesh_collider(world: Long, body: Long, vertices: Ptr[CFloat], vertexCount: CInt, indices: Ptr[CInt], indexCount: CInt): Long = extern
  def sge_phys3d_destroy_collider(world: Long, collider: Long): Unit = extern

  // Collider properties
  def sge_phys3d_collider_set_density(world: Long, collider: Long, density: CFloat): Unit = extern
  def sge_phys3d_collider_set_friction(world: Long, collider: Long, friction: CFloat): Unit = extern
  def sge_phys3d_collider_set_restitution(world: Long, collider: Long, restitution: CFloat): Unit = extern
  def sge_phys3d_collider_set_sensor(world: Long, collider: Long, sensor: CInt): Unit = extern

  // Joints
  def sge_phys3d_create_fixed_joint(world: Long, body1: Long, body2: Long): Long = extern
  def sge_phys3d_create_rope_joint(world: Long, body1: Long, body2: Long, maxDist: CFloat): Long = extern
  def sge_phys3d_destroy_joint(world: Long, joint: Long): Unit = extern

  // Queries
  def sge_phys3d_ray_cast(world: Long, ox: CFloat, oy: CFloat, oz: CFloat, dx: CFloat, dy: CFloat, dz: CFloat, maxDist: CFloat, out: Ptr[CFloat]): CInt = extern

  // Contact events
  def sge_phys3d_poll_contact_start_events(world: Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern
  def sge_phys3d_poll_contact_stop_events(world: Long, out1: Ptr[Long], out2: Ptr[Long], max: CInt): CInt = extern
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

  // ─── Joints ───────────────────────────────────────────────────────────

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long =
    Physics3dC.sge_phys3d_create_fixed_joint(world, body1, body2).toLong

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long =
    Physics3dC.sge_phys3d_create_rope_joint(world, body1, body2, maxDist).toLong

  override def destroyJoint(world: Long, joint: Long): Unit =
    Physics3dC.sge_phys3d_destroy_joint(world, joint)

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
}
