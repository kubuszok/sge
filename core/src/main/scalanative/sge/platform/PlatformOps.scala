// SGE Native Ops — Scala Native platform bridge for shared tests
//
// Provides Native-specific ETC1Ops and BufferOps implementations
// to the shared test suites. Delegates to Rust via C ABI.

package sge
package platform

private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = ETC1OpsNative
  val buffer: BufferOps = BufferOpsNative
}
