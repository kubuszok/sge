package sge.sbt

/** Supported desktop build target.
  *
  * Each case encodes the platform classifier (used in JAR names, archive names, cache paths),
  * the Rust/Scala Native target triple, and OS-specific properties.
  *
  * {{{
  * val targets: Map[Platform, String] = Map(
  *   Platform.LinuxX86_64  -> "https://example.com/jdk-linux-x64.tar.gz",
  *   Platform.MacosAarch64 -> "https://example.com/jdk-macos-aarch64.tar.gz"
  * )
  * }}}
  */
sealed abstract class Platform(
    /** Classifier string used in file names and directory paths (e.g. `"linux-x86_64"`). */
    val classifier: String,
    /** Rust and Scala Native target triple (e.g. `"x86_64-unknown-linux-gnu"`). */
    val rustTarget: String
) {

  /** OS component of the classifier (e.g. `"linux"`, `"macos"`, `"windows"`). */
  def os: String = classifier.split('-').head

  /** Architecture component of the classifier (e.g. `"x86_64"`, `"aarch64"`). */
  def arch: String = classifier.split('-').last

  def isMac: Boolean     = os == "macos"
  def isWindows: Boolean = os == "windows"
  def isLinux: Boolean   = os == "linux"

  /** Scala Native target triple (identical to [[rustTarget]]). */
  def scalaNativeTarget: String = rustTarget

  /** Zig cross-compilation target triple (e.g. `"aarch64-macos"` or `"x86_64-linux-gnu"`).
    * Used as the `-target` argument for `zig cc` / `zig c++`.
    */
  def zigTarget: String = {
    val zigArch = arch match {
      case "x86_64"  => "x86_64"
      case "aarch64" => "aarch64"
    }
    val zigOs = os match {
      case "linux"   => "linux-gnu"
      case "macos"   => "macos"
      case "windows" => "windows-gnu"
    }
    s"$zigArch-$zigOs"
  }

  override def toString: String = classifier
}

object Platform {
  case object LinuxX86_64    extends Platform("linux-x86_64",    "x86_64-unknown-linux-gnu")
  case object LinuxAarch64   extends Platform("linux-aarch64",   "aarch64-unknown-linux-gnu")
  case object MacosX86_64    extends Platform("macos-x86_64",    "x86_64-apple-darwin")
  case object MacosAarch64   extends Platform("macos-aarch64",   "aarch64-apple-darwin")
  case object WindowsX86_64  extends Platform("windows-x86_64",  "x86_64-pc-windows-msvc")
  case object WindowsAarch64 extends Platform("windows-aarch64", "aarch64-pc-windows-msvc")

  /** All six supported desktop platforms. */
  val desktop: Seq[Platform] = Seq(
    LinuxX86_64, LinuxAarch64,
    MacosX86_64, MacosAarch64,
    WindowsX86_64, WindowsAarch64
  )

  /** Detect the current host platform. */
  def host: Platform = {
    val os = sys.props("os.name").toLowerCase match {
      case n if n.contains("linux") => "linux"
      case n if n.contains("mac")  => "macos"
      case n if n.contains("win")  => "windows"
      case n => throw new RuntimeException(s"Unsupported OS: $n")
    }
    val arch = sys.props("os.arch") match {
      case "amd64" | "x86_64"  => "x86_64"
      case "aarch64" | "arm64" => "aarch64"
      case a => throw new RuntimeException(s"Unsupported arch: $a")
    }
    fromClassifier(s"$os-$arch")
  }

  /** Resolve a platform from its classifier string (e.g. `"macos-aarch64"`).
    * @throws RuntimeException if the classifier is not recognized
    */
  def fromClassifier(s: String): Platform =
    desktop.find(_.classifier == s)
      .getOrElse(throw new RuntimeException(s"Unknown platform: $s"))
}

/** Supported Android ABI target.
  *
  * Each case encodes the ABI name (used in APK native lib directories) and the Rust target triple.
  */
sealed abstract class AndroidAbi(
    /** ABI directory name as used in APKs (e.g. `"arm64-v8a"`). */
    val name: String,
    /** Rust target triple for cross-compilation. */
    val rustTarget: String
) {
  override def toString: String = name
}

object AndroidAbi {
  case object Arm64  extends AndroidAbi("arm64-v8a",   "aarch64-linux-android")
  case object Arm32  extends AndroidAbi("armeabi-v7a", "armv7-linux-androideabi")
  case object X86_64 extends AndroidAbi("x86_64",      "x86_64-linux-android")

  /** All supported Android ABIs. */
  val all: Seq[AndroidAbi] = Seq(Arm64, Arm32, X86_64)

  /** Resolve an ABI from its name string (e.g. `"arm64-v8a"`).
    * @throws RuntimeException if the name is not recognized
    */
  def fromName(s: String): AndroidAbi =
    all.find(_.name == s)
      .getOrElse(throw new RuntimeException(s"Unknown Android ABI: $s"))
}
