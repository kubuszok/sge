/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *   Audited: 2026-03-08
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 215
 * Covenant-baseline-methods: Collider,aabb,activeCollisionTypes,activeCollisionTypes_,activeEvents,activeEvents_,activeHooks,activeHooks_,buf3,buf4,collisionGroups,collisionGroups_,contactForceEventThreshold,contactForceEventThreshold_,contactSkin_,density,density_,friction,friction_,groupsBuffer,handle,isEnabled,isEnabled_,isSensor,isSensor_,mass,mass_,oneWayBuf,oneWayDirection,parentBody,positionWrtParent,restitution,restitution_,setOneWayDirection,setPositionWrtParent,shapeType,solverGroups,solverGroups_,world,worldPosition
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package physics

/** A handle to a collision shape attached to a [[RigidBody]].
  *
  * Colliders define the shape, density, friction, and restitution of a body's collision geometry. A body can have multiple colliders. Colliders are created via [[RigidBody.attachCollider]] and
  * destroyed via [[PhysicsWorld.destroyCollider]].
  */
class Collider private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) {

  /** Scratch buffer for 3-float outputs (position). */
  private val buf3 = new Array[Float](3)

  /** Scratch buffer for 4-float outputs (AABB). */
  private val buf4 = new Array[Float](4)

  // ─── Material properties ──────────────────────────────────────────────

  /** Gets the density of this collider. */
  def density: Float =
    world.ops.colliderGetDensity(world.handle, handle)

  /** Sets the density of this collider. Affects the parent body's mass and inertia. */
  def density_=(d: Float): Unit =
    world.ops.colliderSetDensity(world.handle, handle, d)

  /** Gets the friction coefficient. */
  def friction: Float =
    world.ops.colliderGetFriction(world.handle, handle)

  /** Sets the friction coefficient (0 = no friction, typically 0..1). */
  def friction_=(f: Float): Unit =
    world.ops.colliderSetFriction(world.handle, handle, f)

  /** Gets the restitution (bounciness). */
  def restitution: Float =
    world.ops.colliderGetRestitution(world.handle, handle)

  /** Sets the restitution (bounciness, 0 = no bounce, 1 = perfectly elastic). */
  def restitution_=(r: Float): Unit =
    world.ops.colliderSetRestitution(world.handle, handle, r)

  /** Returns true if this collider is a sensor. */
  def isSensor: Boolean =
    world.ops.colliderIsSensor(world.handle, handle)

  /** Sets whether this collider is a sensor.
    *
    * Sensor colliders detect overlaps (generating contact events) but do not produce a physical collision response.
    */
  def isSensor_=(sensor: Boolean): Unit =
    world.ops.colliderSetSensor(world.handle, handle, sensor)

  // ─── Enable/disable ───────────────────────────────────────────────────

  /** Returns true if this collider is enabled. */
  def isEnabled: Boolean =
    world.ops.colliderIsEnabled(world.handle, handle)

  /** Enables or disables this collider. Disabled colliders don't participate in collision detection. */
  def isEnabled_=(enabled: Boolean): Unit =
    world.ops.colliderSetEnabled(world.handle, handle, enabled)

  // ─── Mass ─────────────────────────────────────────────────────────────

  /** Gets the mass of this collider. */
  def mass: Float =
    world.ops.colliderGetMass(world.handle, handle)

  /** Sets the mass of this collider (overrides density-based mass). */
  def mass_=(m: Float): Unit =
    world.ops.colliderSetMass(world.handle, handle, m)

  /** Sets the contact skin (margin around the collider for early contact detection). */
  def contactSkin_=(skin: Float): Unit =
    world.ops.colliderSetContactSkin(world.handle, handle, skin)

  // ─── Position ─────────────────────────────────────────────────────────

  /** Gets the collider position relative to its parent body as (x, y, angle). */
  def positionWrtParent: (Float, Float, Float) = {
    world.ops.colliderGetPositionWrtParent(world.handle, handle, buf3)
    (buf3(0), buf3(1), buf3(2))
  }

  /** Sets the collider position relative to its parent body. */
  def setPositionWrtParent(x: Float, y: Float, angle: Float): Unit =
    world.ops.colliderSetPositionWrtParent(world.handle, handle, x, y, angle)

  /** Gets the collider world position as (x, y, angle). */
  def worldPosition: (Float, Float, Float) = {
    world.ops.colliderGetPosition(world.handle, handle, buf3)
    (buf3(0), buf3(1), buf3(2))
  }

  // ─── Shape info ───────────────────────────────────────────────────────

  /** Returns the shape type code: 0=ball, 1=cuboid, 2=capsule, 3=segment, 4=triangle, 5=trimesh, 6=polyline, 7=heightfield, 8=compound, 9=convex_polygon, -1=unknown.
    */
  def shapeType: Int =
    world.ops.colliderGetShapeType(world.handle, handle)

  /** Gets the axis-aligned bounding box as (minX, minY, maxX, maxY). */
  def aabb: (Float, Float, Float, Float) = {
    world.ops.colliderGetAabb(world.handle, handle, buf4)
    (buf4(0), buf4(1), buf4(2), buf4(3))
  }

  /** Gets the handle of the parent body, or 0 if unattached. */
  def parentBody: Long =
    world.ops.colliderGetParentBody(world.handle, handle)

  // ─── Active events/collision types ────────────────────────────────────

  /** Gets the active events flags for this collider. */
  def activeEvents: Int =
    world.ops.colliderGetActiveEvents(world.handle, handle)

  /** Sets which events this collider generates (bitmask of ActiveEvents flags). */
  def activeEvents_=(flags: Int): Unit =
    world.ops.colliderSetActiveEvents(world.handle, handle, flags)

  /** Gets the active collision types flags for this collider. */
  def activeCollisionTypes: Int =
    world.ops.colliderGetActiveCollisionTypes(world.handle, handle)

  /** Sets which collision types this collider participates in (bitmask). */
  def activeCollisionTypes_=(flags: Int): Unit =
    world.ops.colliderSetActiveCollisionTypes(world.handle, handle, flags)

  // ─── Contact force events ──────────────────────────────────────────────

  /** Gets the contact force event threshold. */
  def contactForceEventThreshold: Float =
    world.ops.colliderGetContactForceEventThreshold(world.handle, handle)

  /** Sets the contact force event threshold. Force events only fire when total force exceeds this.
    *
    * Requires `activeEvents` to include the CONTACT_FORCE_EVENTS flag.
    */
  def contactForceEventThreshold_=(threshold: Float): Unit =
    world.ops.colliderSetContactForceEventThreshold(world.handle, handle, threshold)

  // ─── Active hooks ─────────────────────────────────────────────────────

  /** Gets the active hooks flags. */
  def activeHooks: Int =
    world.ops.colliderGetActiveHooks(world.handle, handle)

  /** Sets the active hooks flags. 0x04 = MODIFY_SOLVER_CONTACTS (required for one-way platforms). */
  def activeHooks_=(flags: Int): Unit =
    world.ops.colliderSetActiveHooks(world.handle, handle, flags)

  private val oneWayBuf = new Array[Float](3)

  /** Configures this collider as a one-way platform.
    *
    * Contacts are only kept if the contact normal aligns with the given direction within the allowed angle. Pass nx=0, ny=0 to disable one-way behavior.
    *
    * Requires `activeHooks` to include MODIFY_SOLVER_CONTACTS (0x04).
    */
  def setOneWayDirection(nx: Float, ny: Float, allowedAngle: Float): Unit =
    world.ops.colliderSetOneWayDirection(world.handle, handle, nx, ny, allowedAngle)

  /** Returns the one-way platform direction as Some((nx, ny, angle)), or None if not configured. */
  def oneWayDirection: Option[(Float, Float, Float)] =
    if (world.ops.colliderGetOneWayDirection(world.handle, handle, oneWayBuf)) {
      Some((oneWayBuf(0), oneWayBuf(1), oneWayBuf(2)))
    } else {
      None
    }

  // ─── Collision filtering ──────────────────────────────────────────────

  private val groupsBuffer = new Array[Int](2)

  /** Gets the collision groups for this collider. */
  def collisionGroups: CollisionGroups = {
    world.ops.colliderGetCollisionGroups(world.handle, handle, groupsBuffer)
    CollisionGroups(groupsBuffer(0), groupsBuffer(1))
  }

  /** Sets the collision groups for this collider.
    *
    * Collision groups control which colliders can detect each other.
    */
  def collisionGroups_=(groups: CollisionGroups): Unit =
    world.ops.colliderSetCollisionGroups(world.handle, handle, groups.memberships, groups.filter)

  /** Gets the solver groups for this collider. */
  def solverGroups: CollisionGroups = {
    world.ops.colliderGetSolverGroups(world.handle, handle, groupsBuffer)
    CollisionGroups(groupsBuffer(0), groupsBuffer(1))
  }

  /** Sets the solver groups for this collider.
    *
    * Solver groups control which colliders produce a force response (separate from detection). Two colliders can detect each other (via collision groups) but not apply forces (via solver groups).
    */
  def solverGroups_=(groups: CollisionGroups): Unit =
    world.ops.colliderSetSolverGroups(world.handle, handle, groups.memberships, groups.filter)
}
