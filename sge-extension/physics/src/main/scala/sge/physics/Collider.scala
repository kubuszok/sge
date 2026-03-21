/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *   Audited: 2026-03-08
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

  /** Sets the density of this collider. Affects the parent body's mass and inertia. */
  def density_=(d: Float): Unit =
    world.ops.colliderSetDensity(world.handle, handle, d)

  /** Sets the friction coefficient (0 = no friction, typically 0..1). */
  def friction_=(f: Float): Unit =
    world.ops.colliderSetFriction(world.handle, handle, f)

  /** Sets the restitution (bounciness, 0 = no bounce, 1 = perfectly elastic). */
  def restitution_=(r: Float): Unit =
    world.ops.colliderSetRestitution(world.handle, handle, r)

  /** Sets whether this collider is a sensor.
    *
    * Sensor colliders detect overlaps (generating contact events) but do not produce a physical collision response.
    */
  def isSensor_=(sensor: Boolean): Unit =
    world.ops.colliderSetSensor(world.handle, handle, sensor)
}
