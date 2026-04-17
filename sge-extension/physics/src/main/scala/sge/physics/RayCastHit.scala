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

/** Result of a ray cast query against the physics world.
  *
  * @param hitX
  *   world-space x coordinate of the hit point
  * @param hitY
  *   world-space y coordinate of the hit point
  * @param normalX
  *   x component of the surface normal at the hit point
  * @param normalY
  *   y component of the surface normal at the hit point
  * @param timeOfImpact
  *   the parametric distance along the ray (0 = origin, 1 = origin + dir * maxDist)
  * @param bodyHandle
  *   the native handle of the rigid body that was hit (via collider's parent)
  * @param colliderHandle
  *   the native handle of the specific collider shape that was hit
  */
final case class RayCastHit(
  hitX:           Float,
  hitY:           Float,
  normalX:        Float,
  normalY:        Float,
  timeOfImpact:   Float,
  bodyHandle:     Long,
  colliderHandle: Long
)

/** Result of a shape cast (sweep) query against the physics world.
  *
  * @param hitX
  *   world-space x coordinate of the hit point
  * @param hitY
  *   world-space y coordinate of the hit point
  * @param normalX
  *   x component of the surface normal at the hit point
  * @param normalY
  *   y component of the surface normal at the hit point
  * @param timeOfImpact
  *   the parametric distance along the sweep direction
  * @param colliderHandle
  *   the native handle of the collider that was hit
  */
final case class ShapeCastHit(
  hitX:           Float,
  hitY:           Float,
  normalX:        Float,
  normalY:        Float,
  timeOfImpact:   Float,
  colliderHandle: Long
)

/** Result of projecting a point onto the nearest collider surface.
  *
  * @param projX
  *   x coordinate of the projected point on the surface
  * @param projY
  *   y coordinate of the projected point on the surface
  * @param isInside
  *   true if the query point is inside the collider
  * @param colliderHandle
  *   the native handle of the nearest collider
  */
final case class PointProjection(
  projX:          Float,
  projY:          Float,
  isInside:       Boolean,
  colliderHandle: Long
)
