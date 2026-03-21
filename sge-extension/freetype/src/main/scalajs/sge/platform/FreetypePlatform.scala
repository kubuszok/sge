// SGE FreeType — Scala.js platform bridge
//
// Provides JS-specific FreetypeOps stub that throws UnsupportedOperationException.
// Users should pre-generate bitmap fonts for JS builds.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires JS-specific stub into FreetypePlatform
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

private[sge] object FreetypePlatform {
  val ops: FreetypeOps = FreetypeOpsJs
}
