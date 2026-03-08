/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: JS platform bridge for PhysicsOps (stub)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

/** Scala.js platform bridge — provides the stub [[PhysicsOpsJs]] implementation. */
private[sge] object PhysicsPlatform {
  val ops: PhysicsOps = PhysicsOpsJs
}
