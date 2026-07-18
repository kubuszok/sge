// SGE Native Ops — per-axis availability policy (Scala.js)
//
// ISS-724 c1, wave 2026-07-18-H territory H2. See the JVM sibling for the full contract.
//
// JS: PlatformOps.buffer / PlatformOps.etc1 resolve to the pure-Scala BufferOpsJs / ETC1OpsJs
// objects — there is no native library to load, so the capability can never be absent. The
// axis therefore GUARANTEES availability: a false probe here is a real regression (the fallback
// impl went missing / threw) and must fail the suite loudly rather than silently skip.

package sge
package platform

private[platform] object NativeOpsAvailabilityPolicy {

  /** Whether the native-ops capability is guaranteed present on this platform axis. */
  val guaranteed: Boolean = true
}
