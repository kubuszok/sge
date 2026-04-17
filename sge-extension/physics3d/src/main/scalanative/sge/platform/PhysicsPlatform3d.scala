/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: Scala Native platform bridge for PhysicsOps3d via @extern
 *   Idiom: split packages
 */
package sge
package platform

/** Scala Native platform bridge — provides [[PhysicsOpsNative3d]] via C ABI. */
private[sge] object PhysicsPlatform3d {
  val ops: PhysicsOps3d = PhysicsOpsNative3d
}
