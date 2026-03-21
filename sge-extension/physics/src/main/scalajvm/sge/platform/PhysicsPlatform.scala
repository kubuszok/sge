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
 */
package sge
package platform

/** JVM platform bridge — wires [[PhysicsOpsPanama]] using the runtime-detected [[PanamaProvider]]. */
private[sge] object PhysicsPlatform {
  val ops: PhysicsOps = new PhysicsOpsPanama(Panama.provider)
}
