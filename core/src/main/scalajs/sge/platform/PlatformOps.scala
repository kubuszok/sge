// SGE Native Ops — Scala.js platform bridge for shared tests
//
// Provides JS-specific ETC1Ops and BufferOps implementations
// to the shared test suites. Uses pure Scala fallback implementations.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires JS-specific impls (BufferOpsJs, ETC1OpsJs) into PlatformOps
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = ETC1OpsJs
  val buffer: BufferOps = BufferOpsJs
}
