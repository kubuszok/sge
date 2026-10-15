/*
 * Part of sge's port of RegExodus' JUnit suite (https://github.com/tommyettinger/RegExodus, BSD-3-Clause).
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.regexodus

/** The parent of RegExodus' generated suites. A few tests also compile the same pattern with the platform's `java.util.regex` to compare, or outlast munit's timeout; where the platform cannot answer,
  * they are ignored there and the reason is part of the reported name. Every other test runs on every platform unchanged.
  */
abstract class RegExodusSuite extends munit.FunSuite {

  private val vm: String = System.getProperty("java.vm.name", "")

  /** test name -> why it runs on the JVM only, for this platform */
  private def jvmOnly: Map[String, String] =
    if (vm == "Scala.js")
      Map(
        "testASCII" -> "its java.util.regex comparison uses \\p{In...} Unicode blocks, which Scala.js' java.util.regex does not support",
        "testLongCategories" -> "its java.util.regex comparison uses \\p{In...} Unicode blocks, which Scala.js' java.util.regex does not support"
      )
    else if (vm == "Scala Native")
      Map(
        "testASCII" -> "its java.util.regex comparison uses a group name Scala Native's java.util.regex rejects",
        "testLongCategories" -> "its java.util.regex comparison uses a group name Scala Native's java.util.regex rejects",
        "testCompatibility" -> "its java.util.regex comparison uses an escape sequence Scala Native's java.util.regex does not support",
        "testFinnegan" -> "it runs past munit's 30 second timeout on Scala Native (ISS-916)"
      )
    else Map.empty

  override def munitTests(): Seq[Test] =
    super.munitTests().map { t =>
      jvmOnly.get(t.name).fold(t)(reason => t.withName(s"${t.name} (JVM only: $reason)").tag(munit.Ignore))
    }
}
