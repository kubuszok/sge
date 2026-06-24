import sbt.VirtualAxis
import sge.sbt.SgePlugin

/** Build version catalogue. Flattened from the former anonymous `val versions = new {…}` refinement, which does not survive the sbt-2.0 Scala-3 build dialect.
  */
object Versions {
  // Versions we are publishing for.
  val scala3 = SgePlugin.scalaVersion

  // Which versions should be cross-compiled for publishing.
  val scalas    = List(scala3)
  val platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)

  // Dependencies
  val gears            = "0.3.1"
  val kindlings        = "0.1.2"
  val lls              = "0.2.0"
  val scribe           = "3.19.0"
  val scalajsDom       = "2.8.1"
  val scalaSaxParser   = "0.1.0"
  val scalaJavaTime    = "2.6.0"
  val scalaJavaLocales = "1.5.4"
  val sttp             = "4.0.25"
  val xml              = "2.4.0"

  // Tests
  val munit           = "1.3.2"
  val munitScalacheck = "1.3.0"

  // Native component providers (from sge-native-providers repo)
  val multiarch = "0.3.0"
  // 0.1.2-30-g14bab58-SNAPSHOT carries the Windows sge_audio.dll/glfw3.dll +
  // macos-x86_64 real dylibs + ANGLE EGL/GLESv2 naming fix (sge-native-providers
  // PR #11). 0.1.2 lacked the Windows audio/glfw DLLs (sgeValidateNativeLibs gate)
  // and shipped non-functional 16KB macos-x86_64 stubs.
  val nativeComponents = "0.1.2-30-g14bab58-SNAPSHOT"
  val curlProvider     = multiarch
}
