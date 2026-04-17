/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: immutable case class for query results
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: RayCastHit3d
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package physics3d

/** Result of a 3D ray cast query against the physics world.
  *
  * @param hitX
  *   world-space x coordinate of the hit point
  * @param hitY
  *   world-space y coordinate of the hit point
  * @param hitZ
  *   world-space z coordinate of the hit point
  * @param normalX
  *   x component of the surface normal at the hit point
  * @param normalY
  *   y component of the surface normal at the hit point
  * @param normalZ
  *   z component of the surface normal at the hit point
  * @param timeOfImpact
  *   the parametric distance along the ray (0 = origin, 1 = origin + dir * maxDist)
  * @param colliderHandle
  *   the native handle of the specific collider shape that was hit
  */
final case class RayCastHit3d(
  hitX:           Float,
  hitY:           Float,
  hitZ:           Float,
  normalX:        Float,
  normalY:        Float,
  normalZ:        Float,
  timeOfImpact:   Float,
  colliderHandle: Long
)
