// SGE — Panama provider detection
//
// Chooses between JdkPanama (Desktop JVM) and PanamaPortProvider (Android)
// based on runtime class availability. The unused provider is never class-loaded.
//
// Uses reflection to avoid compile-time dependency on either implementation.
// JdkPanama lives in sge-panama-jvm (JDK 22+), PanamaPortProvider in sge-panama-android (JDK 17).
// Both are merged into sge's JAR at package time without sbt dependsOn.

package sge
package platform

/** Runtime selection of the Panama FFM provider.
  *
  * On JDK 22+ (Desktop): `java.lang.foreign.MemorySegment` exists → JdkPanama. On Android with PanamaPort: that class is missing → PanamaPortProvider.
  */
private[sge] object Panama {

  /** The active Panama provider for the current runtime. */
  lazy val provider: PanamaProvider = detect()

  private def detect(): PanamaProvider =
    try {
      Class.forName("java.lang.foreign.MemorySegment")
      // JDK 22+ — load JdkPanama via reflection (compiled separately with JDK 22+)
      loadProvider("sge.platform.JdkPanama$")
    } catch {
      case _: ClassNotFoundException =>
        // Android — load PanamaPortProvider via reflection (compiled with JDK 17)
        loadProvider("sge.platform.PanamaPortProvider$")
    }

  private def loadProvider(objectClassName: String): PanamaProvider = {
    val cls = Class.forName(objectClassName)
    cls.getField("MODULE$").get(null).asInstanceOf[PanamaProvider]
  }
}
