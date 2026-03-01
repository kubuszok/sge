// SGE Native Ops — Scala.js platform bridge for shared tests
//
// Provides JS-specific ETC1Ops and BufferOps implementations
// to the shared test suites. Uses pure Scala fallback implementations.

package sge
package platform

private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = ETC1OpsJs
  val buffer: BufferOps = BufferOpsJs
}
