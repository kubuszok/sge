// SGE Native Ops — JVM platform bridge for shared tests
//
// Provides JVM-specific ETC1Ops and BufferOps implementations
// to the shared test suites.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires JVM-specific impls (BufferOpsJvm, ETC1OpsJvm) into PlatformOps
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = ETC1OpsJvm
  val buffer: BufferOps = BufferOpsJvm
}
