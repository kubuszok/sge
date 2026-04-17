/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: JS platform bridge for PhysicsOps
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: PhysicsPlatform,ops
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package platform

/** Scala.js platform bridge — wires [[PhysicsOpsJs]] implementation. */
private[sge] object PhysicsPlatform {
  val ops: PhysicsOps = PhysicsOpsJs
}
