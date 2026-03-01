// SGE Native Ops — JVM platform bridge for shared tests
//
// Provides JVM-specific ETC1Ops and BufferOps implementations
// to the shared test suites.

package sge
package platform

private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = ETC1OpsJvm
  val buffer: BufferOps = BufferOpsJvm
}
