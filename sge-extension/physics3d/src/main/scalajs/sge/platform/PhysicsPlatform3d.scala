/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: JS platform bridge for PhysicsOps3d (stub)
 *   Idiom: split packages
 */
package sge
package platform

/** Scala.js platform bridge — provides the stub [[PhysicsOpsJs3d]] implementation. */
private[sge] object PhysicsPlatform3d {
  val ops: PhysicsOps3d = PhysicsOpsJs3d
}
