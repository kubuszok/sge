/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: JS platform bridge for PhysicsOps3d
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 24
 * Covenant-baseline-methods: PhysicsPlatform3d,ops
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package platform

/** Scala.js platform bridge — wires [[PhysicsOpsJs3d]] implementation. */
private[sge] object PhysicsPlatform3d {
  val ops: PhysicsOps3d = PhysicsOpsJs3d
}
