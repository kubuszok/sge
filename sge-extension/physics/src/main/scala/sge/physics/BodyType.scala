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

/** The type of a rigid body, determining how the physics engine simulates it. */
enum BodyType {

  /** Affected by forces, collisions, and gravity. */
  case Dynamic

  /** Never moves — infinite mass, used for walls and terrain. */
  case Static

  /** Moves only when explicitly set — not affected by forces or collisions. */
  case Kinematic
}
