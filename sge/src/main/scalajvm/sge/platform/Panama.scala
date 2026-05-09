// SGE — Panama provider detection
//
// Delegates to multiarch.panama.Panama for runtime provider selection.
// JdkPanama (JDK 22+) and PanamaPortProvider (Android PanamaPort) are
// provided by the multiarch-panama-jdk / multiarch-panama-api libraries.

package sge
package platform

/** Runtime selection of the Panama FFM provider.
  *
  * Delegates to [[multiarch.panama.Panama]] which uses reflection to detect JdkPanama (JDK 22+) or PanamaPortProvider (Android).
  */
private[sge] object Panama {

  /** The active Panama provider for the current runtime. */
  lazy val provider: PanamaProvider = multiarch.panama.Panama.provider
}
