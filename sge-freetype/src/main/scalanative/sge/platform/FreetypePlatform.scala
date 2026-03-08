// SGE FreeType — Scala Native platform bridge
//
// Provides Native-specific FreetypeOps implementation that delegates
// to Rust via C ABI @extern bindings.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires Native-specific impl into FreetypePlatform
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

private[sge] object FreetypePlatform {
  val ops: FreetypeOps = FreetypeOpsNative
}
