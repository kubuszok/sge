// SGE Native Ops — per-axis availability policy (Scala Native)
//
// ISS-724 c1, wave 2026-07-18-H territory H2. See the JVM sibling for the full contract.
//
// Native: PlatformOps.buffer / PlatformOps.etc1 resolve to BufferOpsNative / ETC1OpsNative,
// which call the Rust ops through the C ABI. Those externs are resolved at link time — if the
// test binary linked, the capability is present. The axis therefore GUARANTEES availability:
// a false probe here is a real link/ABI regression and must fail the suite loudly rather than
// silently skip.

package sge
package platform

private[platform] object NativeOpsAvailabilityPolicy {

  /** Whether the native-ops capability is guaranteed present on this platform axis. */
  val guaranteed: Boolean = true
}
