/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: Scala 3 enum for body types
 */
package sge
package physics3d

/** The type of a 3D rigid body, determining how the physics engine simulates it. */
enum BodyType3d {

  /** Affected by forces, collisions, and gravity. */
  case Dynamic

  /** Never moves — infinite mass, used for walls and terrain. */
  case Static

  /** Moves only when explicitly set — not affected by forces or collisions. */
  case Kinematic
}
