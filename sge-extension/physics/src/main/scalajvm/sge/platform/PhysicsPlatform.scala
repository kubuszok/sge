/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: JVM platform bridge for PhysicsOps via Panama
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: PhysicsPlatform,ops
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package platform

/** JVM platform bridge — wires [[PhysicsOpsPanama]] using the runtime-detected [[PanamaProvider]]. */
private[sge] object PhysicsPlatform {
  val ops: PhysicsOps = new PhysicsOpsPanama(Panama.provider)
}
