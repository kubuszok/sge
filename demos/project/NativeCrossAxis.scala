import sbt._

/** Custom VirtualAxis for cross-compiled Scala Native targets.
  *
  * Each non-host platform gets a separate sbt subproject with zig-based
  * cross-compilation settings. For example, building for linux-x86_64 from
  * macOS produces a subproject named `pongNativeLinuxX86_64`.
  */
case class NativeCrossAxis(platform: sge.sbt.Platform) extends VirtualAxis.WeakAxis {
  val idSuffix: String = "Native" + platform.classifier.split('-').map(_.capitalize).mkString
  val directorySuffix: String = "native-" + platform.classifier
}
