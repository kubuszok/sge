/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: handle-based FFI wrapper
 */
package sge
package physics3d

/** A handle to a 3D collision shape attached to a [[RigidBody3d]].
  *
  * Colliders define the shape, density, friction, and restitution of a body's collision geometry. A body can have multiple colliders. Colliders are created via [[RigidBody3d.attachCollider]] and
  * destroyed via [[PhysicsWorld3d.destroyCollider]].
  */
class Collider3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) {

  private val buf6      = new Array[Float](6)
  private val buf7      = new Array[Float](7)
  private val groupsBuf = new Array[Int](2)

  // ─── Material properties ──────────────────────────────────────────────

  def density:             Float = world.ops.colliderGetDensity(world.handle, handle)
  def density_=(d: Float): Unit  = world.ops.colliderSetDensity(world.handle, handle, d)

  def friction:             Float = world.ops.colliderGetFriction(world.handle, handle)
  def friction_=(f: Float): Unit  = world.ops.colliderSetFriction(world.handle, handle, f)

  def restitution:             Float = world.ops.colliderGetRestitution(world.handle, handle)
  def restitution_=(r: Float): Unit  = world.ops.colliderSetRestitution(world.handle, handle, r)

  def isSensor:                    Boolean = world.ops.colliderIsSensor(world.handle, handle)
  def isSensor_=(sensor: Boolean): Unit    = world.ops.colliderSetSensor(world.handle, handle, sensor)

  // ─── Enable/disable ───────────────────────────────────────────────────

  def isEnabled:                     Boolean = world.ops.colliderIsEnabled(world.handle, handle)
  def isEnabled_=(enabled: Boolean): Unit    = world.ops.colliderSetEnabled(world.handle, handle, enabled)

  // ─── Mass ─────────────────────────────────────────────────────────────

  def mass:                       Float = world.ops.colliderGetMass(world.handle, handle)
  def mass_=(m:           Float): Unit  = world.ops.colliderSetMass(world.handle, handle, m)
  def contactSkin_=(skin: Float): Unit  = world.ops.colliderSetContactSkin(world.handle, handle, skin)

  // ─── Position ─────────────────────────────────────────────────────────

  /** Gets the collider position relative to its parent body as (x, y, z, qx, qy, qz, qw). */
  def positionWrtParent: (Float, Float, Float, Float, Float, Float, Float) = {
    world.ops.colliderGetPositionWrtParent(world.handle, handle, buf7)
    (buf7(0), buf7(1), buf7(2), buf7(3), buf7(4), buf7(5), buf7(6))
  }

  /** Sets the collider position relative to its parent body. */
  def setPositionWrtParent(x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Unit =
    world.ops.colliderSetPositionWrtParent(world.handle, handle, x, y, z, qx, qy, qz, qw)

  /** Gets the collider world position as (x, y, z, qx, qy, qz, qw). */
  def worldPosition: (Float, Float, Float, Float, Float, Float, Float) = {
    world.ops.colliderGetPosition(world.handle, handle, buf7)
    (buf7(0), buf7(1), buf7(2), buf7(3), buf7(4), buf7(5), buf7(6))
  }

  // ─── Shape info ───────────────────────────────────────────────────────

  def shapeType: Int = world.ops.colliderGetShapeType(world.handle, handle)

  /** Gets the axis-aligned bounding box as (minX, minY, minZ, maxX, maxY, maxZ). */
  def aabb: (Float, Float, Float, Float, Float, Float) = {
    world.ops.colliderGetAabb(world.handle, handle, buf6)
    (buf6(0), buf6(1), buf6(2), buf6(3), buf6(4), buf6(5))
  }

  def parentBody: Long = world.ops.colliderGetParentBody(world.handle, handle)

  // ─── Active events/collision types ────────────────────────────────────

  def activeEvents:                       Int  = world.ops.colliderGetActiveEvents(world.handle, handle)
  def activeEvents_=(flags:         Int): Unit = world.ops.colliderSetActiveEvents(world.handle, handle, flags)
  def activeCollisionTypes:               Int  = world.ops.colliderGetActiveCollisionTypes(world.handle, handle)
  def activeCollisionTypes_=(flags: Int): Unit = world.ops.colliderSetActiveCollisionTypes(world.handle, handle, flags)

  // ─── Contact force events ──────────────────────────────────────────────

  def contactForceEventThreshold: Float =
    world.ops.colliderGetContactForceEventThreshold(world.handle, handle)

  def contactForceEventThreshold_=(threshold: Float): Unit =
    world.ops.colliderSetContactForceEventThreshold(world.handle, handle, threshold)

  // ─── Active hooks ─────────────────────────────────────────────────────

  def activeHooks:               Int  = world.ops.colliderGetActiveHooks(world.handle, handle)
  def activeHooks_=(flags: Int): Unit = world.ops.colliderSetActiveHooks(world.handle, handle, flags)

  private val oneWayBuf = new Array[Float](4)

  /** Configures this collider as a one-way platform. Pass nx=0, ny=0, nz=0 to disable. */
  def setOneWayDirection(nx: Float, ny: Float, nz: Float, allowedAngle: Float): Unit =
    world.ops.colliderSetOneWayDirection(world.handle, handle, nx, ny, nz, allowedAngle)

  /** Returns the one-way platform direction as Some((nx, ny, nz, angle)), or None if not configured. */
  def oneWayDirection: Option[(Float, Float, Float, Float)] =
    if (world.ops.colliderGetOneWayDirection(world.handle, handle, oneWayBuf)) {
      Some((oneWayBuf(0), oneWayBuf(1), oneWayBuf(2), oneWayBuf(3)))
    } else {
      None
    }

  // ─── Collision filtering ──────────────────────────────────────────────

  def collisionGroups: CollisionGroups3d = {
    world.ops.colliderGetCollisionGroups(world.handle, handle, groupsBuf)
    CollisionGroups3d(groupsBuf(0), groupsBuf(1))
  }

  def collisionGroups_=(groups: CollisionGroups3d): Unit =
    world.ops.colliderSetCollisionGroups(world.handle, handle, groups.memberships, groups.filter)

  def solverGroups: CollisionGroups3d = {
    world.ops.colliderGetSolverGroups(world.handle, handle, groupsBuf)
    CollisionGroups3d(groupsBuf(0), groupsBuf(1))
  }

  def solverGroups_=(groups: CollisionGroups3d): Unit =
    world.ops.colliderSetSolverGroups(world.handle, handle, groups.memberships, groups.filter)
}
