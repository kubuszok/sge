import sge.sbt.SgePackaging
import multiarch.sbt.{ JvmPackaging, Platform }

// Exercises the NATIVE packaging task (`sgePackageNative`) without Scala Native: a tiny C program
// stands in for the linked executable, linked the way NativeProviderPlugin links a game (the
// build machine's absolute library directory AND the executable's own directory as rpath), and a
// stub directory stands in for the extracted native providers (ANGLE's libEGL/libGLESv2 plus a
// static archive that must not be shipped).

val isMac     = sys.props("os.name").toLowerCase.contains("mac")
val isLinux   = sys.props("os.name").toLowerCase.contains("linux")
val isWindows = sys.props("os.name").toLowerCase.contains("win")
val libExt    = if (isMac) "dylib" else if (isWindows) "dll" else "so"
val appName   = "hello-native"

def runCmd(cmd: Seq[String], dir: File): String = {
  val proc = new ProcessBuilder(cmd*).directory(dir).redirectErrorStream(true).start()
  val out  = new String(proc.getInputStream.readAllBytes())
  val exit = proc.waitFor()
  if (exit != 0) sys.error(s"[check] ${cmd.mkString(" ")} failed (exit $exit): $out")
  out
}

lazy val game = (project in file("game"))
  .settings(SgePackaging.nativeSettings)
  .settings(
    JvmPackaging.releaseAppName := appName,
    SgePackaging.sgeNativeLibDirs := Def.uncached {
      val dir = target.value / "stub-native-libs"
      IO.createDirectory(dir)
      IO.write(dir / s"libEGL.$libExt", "stub EGL")
      IO.write(dir / s"libGLESv2.$libExt", "stub GLESv2")
      IO.write(dir / "libstatic.a", "stub static archive")
      Seq(dir)
    },
    SgePackaging.sgeNativeBinary := Def.uncached {
      val libDir = (target.value / "stub-native-libs").getAbsoluteFile
      val dir    = target.value / "stub-exe"
      IO.createDirectory(dir)
      val exe = dir / "game"
      if (isWindows) IO.write(exe, "stub executable")
      else {
        IO.write(dir / "main.c", "int main(void) { return 0; }\n")
        val self = if (isMac) "@executable_path" else "$ORIGIN"
        runCmd(Seq("cc", "-o", exe.getAbsolutePath, "main.c", s"-Wl,-rpath,${libDir.getAbsolutePath}", s"-Wl,-rpath,$self"), dir)
      }
      exe
    }
  )

lazy val checkNativePackage = taskKey[Unit]("Assert sgePackageNative ships the shared libraries next to the executable, in the folder and the archive, and leaves only a relocatable rpath")

checkNativePackage := Def.uncached {
  val archive = (game / SgePackaging.sgePackageNative).value
  val outDir  = (game / target).value / "sge-native" / appName
  val libDir  = ((game / target).value / "stub-native-libs").getAbsolutePath
  val exeName = if (isWindows) s"$appName.exe" else appName
  val exe     = outDir / exeName
  val log     = streams.value.log

  // ── the folder: executable and shared libraries side by side, no static archive ──
  val expected = Seq(exeName, s"libEGL.$libExt", s"libGLESv2.$libExt")
  val missing  = expected.filterNot(n => (outDir / n).isFile)
  if (missing.nonEmpty) sys.error(s"[check] package folder is missing ${missing.mkString(", ")}: ${IO.listFiles(outDir).map(_.getName).mkString(", ")}")
  if ((outDir / "libstatic.a").exists()) sys.error("[check] a static archive was shipped in the package")
  if ((outDir / "lib").exists()) sys.error("[check] libraries went into lib/, which is not on the executable's search path")

  // ── the archive: the same entries ──
  val entries =
    if (isWindows) {
      val zip = new java.util.zip.ZipFile(archive)
      try { import scala.jdk.CollectionConverters._; zip.entries().asScala.map(_.getName.replace('\\', '/')).toList }
      finally zip.close()
    } else runCmd(Seq("tar", "tzf", archive.getAbsolutePath), archive.getParentFile).linesIterator.toList
  val missingInArchive = expected.map(n => s"$appName/$n").filterNot(entries.contains)
  if (missingInArchive.nonEmpty) sys.error(s"[check] archive is missing ${missingInArchive.mkString(", ")}: ${entries.mkString(", ")}")

  // ── the rpath: only the executable's own directory ──
  if (isMac) {
    val rpaths = runCmd(Seq("otool", "-l", exe.getAbsolutePath), outDir).linesIterator.map(_.trim).toList
      .sliding(3).collect { case List("cmd LC_RPATH", _, p) => p.stripPrefix("path ").replaceAll("""\s+\(offset \d+\)$""", "") }.toList
    if (rpaths != List("@executable_path")) sys.error(s"[check] LC_RPATH should be only @executable_path, was: ${rpaths.mkString(", ")}")
  } else if (isLinux) {
    val dyn = runCmd(Seq("readelf", "-d", exe.getAbsolutePath), outDir)
    val paths = dyn.linesIterator.filter(l => l.contains("(RUNPATH)") || l.contains("(RPATH)")).toList
    if (paths.isEmpty || !paths.forall(_.contains("[$ORIGIN]"))) sys.error(s"[check] RUNPATH should be only $$ORIGIN, was: ${paths.mkString("; ")}")
  }
  if (!isWindows) {
    val bytes = new String(java.nio.file.Files.readAllBytes(exe.toPath), "ISO-8859-1")
    if (bytes.contains(libDir)) sys.error(s"[check] the executable still names the build machine's library directory $libDir")
    // the edited executable still loads and runs
    runCmd(Seq(exe.getAbsolutePath), outDir)
  }

  log.info(s"[check] native package OK: $outDir")
}
