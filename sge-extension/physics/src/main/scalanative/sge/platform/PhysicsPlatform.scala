/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: Scala Native platform bridge for PhysicsOps via @extern
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

/** Scala Native platform bridge — provides [[PhysicsOpsNative]] via C ABI. */
private[sge] object PhysicsPlatform {
  val ops: PhysicsOps = PhysicsOpsNative
}
