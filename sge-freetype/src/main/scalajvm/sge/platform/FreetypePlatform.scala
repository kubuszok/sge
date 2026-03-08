// SGE FreeType — JVM platform bridge
//
// Wires Panama-based FreetypeOps implementation using the
// runtime-detected PanamaProvider (JdkPanama on Desktop, PanamaPortProvider on Android).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires Panama-based impl into FreetypePlatform via Panama.provider
//   Idiom: split packages
//   Audited: 2026-03-08

package sge
package platform

private[sge] object FreetypePlatform {
  private val panama: PanamaProvider = Panama.provider
  val ops: FreetypeOps = new FreetypeOpsPanama(panama)
}
