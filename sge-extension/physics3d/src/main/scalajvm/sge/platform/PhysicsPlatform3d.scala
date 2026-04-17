/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: JVM platform bridge for PhysicsOps3d via Panama
 *   Idiom: split packages
 */
package sge
package platform

/** JVM platform bridge — wires [[PhysicsOpsPanama3d]] using the runtime-detected [[PanamaProvider]]. */
private[sge] object PhysicsPlatform3d {
  val ops: PhysicsOps3d = new PhysicsOpsPanama3d(Panama.provider)
}
