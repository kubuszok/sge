// SGE Native Ops — JVM platform bridge
//
// Wires Panama-based ETC1Ops and BufferOps implementations using the
// runtime-detected PanamaProvider (JdkPanama on Desktop, PanamaPortProvider on Android).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires Panama-based impls into PlatformOps via Panama.provider
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

private[sge] object PlatformOps {
  private val panama: PanamaProvider = Panama.provider

  val etc1:        ETC1Ops        = new ETC1OpsPanama(panama)
  val buffer:      BufferOps      = new BufferOpsPanama(panama)
  val gdx2d:       Gdx2dOps       = Gdx2dOpsJvm
  val concurrency: ConcurrencyOps = ConcurrencyOpsDesktop

  // Desktop backend FFI — set by the DesktopApplication during initialization.
  // Null until a desktop backend is running (headless mode doesn't need them).
  @volatile private[sge] var windowing: WindowingOps = scala.compiletime.uninitialized
  @volatile private[sge] var audio:     AudioOps     = scala.compiletime.uninitialized
  @volatile private[sge] var gl:        GlOps        = scala.compiletime.uninitialized
}
