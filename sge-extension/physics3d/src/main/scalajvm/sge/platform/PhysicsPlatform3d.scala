/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: JVM platform bridge for PhysicsOps3d via Panama
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 17
 * Covenant-baseline-methods: PhysicsPlatform3d,ops
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package platform

/** JVM platform bridge — wires [[PhysicsOpsPanama3d]] using the runtime-detected [[PanamaProvider]]. */
private[sge] object PhysicsPlatform3d {
  val ops: PhysicsOps3d = new PhysicsOpsPanama3d(Panama.provider)
}
