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
  val scalaSaxParser   = "0.1.1"
  val scalaJavaTime    = "2.7.0"
  val scalaJavaLocales = "1.5.4"
  val sttp             = "4.0.25"
  val xml              = "2.4.0"

  // Tests
  val munit           = "1.3.2"
  val munitScalacheck = "1.3.0"

  // Native component providers (from sge-native-providers repo)
  val multiarch = "0.3.0"
  // 0.1.2-33-gcf10406-SNAPSHOT completes Windows Scala Native support for the
  // manually-linked C DLLs (sge-native-providers PR #11, commits 1dda883 +
  // e521b92 + f3f34d6): (1) REAL import libraries glfw3.lib AND sge_audio.lib
  // (build.rs emits <name>.dll.lib via clang-cl /implib:, cross-all.sh renames
  // them) replacing the old bogus stubs (copies of sge_native_ops.lib) that
  // exported the wrong symbols and broke @link("glfw3")/@link("sge_audio") with
  // LNK2019 unresolved glfwInit / sge_audio_play_sound; (2) dllexported glfw
  // platform stubs (glfwGetCocoaWindow/glfwGetX11Window) so they land in
  // glfw3.lib; (3) the sn-provider-sge JAR now ships the windows runtime DLLs
  // (sge_native_ops.dll/sge_audio.dll/glfw3.dll) so the NativeProviderPlugin
  // copies them next to the linked exe — without them the binary linked but
  // exited 0xC0000135 STATUS_DLL_NOT_FOUND. Earlier 0.1.2-30-g14bab58 carried
  // the Windows sge_audio.dll/glfw3.dll + macos-x86_64 real dylibs + ANGLE fix.
  val nativeComponents = "0.1.2-33-gcf10406-SNAPSHOT"
  val curlProvider     = multiarch
}
