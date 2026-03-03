// SGE Native Ops — Scala Native platform bridge for shared tests
//
// Provides Native-specific ETC1Ops and BufferOps implementations
// to the shared test suites. Delegates to Rust via C ABI.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires Native-specific impls (BufferOpsNative, ETC1OpsNative) into PlatformOps
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = ETC1OpsNative
  val buffer: BufferOps = BufferOpsNative
}
