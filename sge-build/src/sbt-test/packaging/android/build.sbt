import sge.sbt.SgePlugin
import sge.sbt.SgePlugin.autoImport._
import multiarch.sbt.{ AndroidBuild, AndroidPlugin, AndroidSdk }

import scala.sys.process.{ Process => SysProcess, ProcessLogger }

// Minimal SGE game project used purely to exercise the ANDROID signing pipeline
// shipped by the sge-build plugin (`androidSign`, ISS-562). It enables SgePlugin
// (so SgeAndroidPlatform triggers) and AndroidPlugin (so AndroidBuild.taskSettings
// — androidDex → androidPackage → androidSign — are wired in). The pipeline is:
// scalac → D8 (DEX) → aapt2 (link) → zipalign → apksigner (debug keystore).
//
// SgeAndroidPlatform defaults `androidSdkCacheDir` to <baseDirectory>/sge-deps/
// android-sdk, which does NOT exist inside the scripted temp copy. We override it
// below to the locally-installed SDK (env first, then the standard OS locations),
// so `androidSdkRoot` resolves WITHOUT triggering a multi-hundred-MB download.
lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin, AndroidPlugin)
  .settings(
    name         := "hello-sge-android",
    organization := "com.example",
    // Drop the sge engine + extension deps injected by the SGE platform plugins —
    // not published for this scripted plugin version, and not needed to DEX/sign
    // the fixture (HelloGame has no SGE imports).
    libraryDependencies := libraryDependencies.value.filterNot { m =>
      m.organization == "com.kubuszok" &&
      (m.name.startsWith("sge_") || m.name.startsWith("sge-extension"))
    },
    Compile / mainClass := Some("com.example.HelloGame"),
    // Resolve a locally-installed Android SDK for the androidSdkRoot task. Order:
    // ANDROID_HOME, ANDROID_SDK_ROOT, then the conventional OS install dirs. This
    // keeps the test hermetic (no SDK download) on both dev machines (macOS
    // ~/Library/Android/sdk) and CI (ANDROID_HOME / ~/Android/Sdk on Linux).
    AndroidPlugin.autoImport.androidSdkCacheDir := {
      val candidates = Seq(
        sys.env.get("ANDROID_HOME"),
        sys.env.get("ANDROID_SDK_ROOT"),
        Some(sys.props("user.home") + "/Library/Android/sdk"),
        Some(sys.props("user.home") + "/Android/Sdk")
      ).flatten.map(file)
      candidates
        .find(d => d.isDirectory && (d / "build-tools").isDirectory)
        .getOrElse(baseDirectory.value / "android-sdk")
    }
  )
  .jvmPlatform(scalaVersions = Seq(SgePlugin.scalaVersion))

// Custom task driven by the scripted `test` script. Runs `androidSign` and
// asserts the produced APK is (a) structurally an APK — a valid ZIP carrying
// classes.dex plus the v1 JAR-signature block (META-INF/MANIFEST.MF + *.SF +
// *.RSA) — and (b) cryptographically signed, verified end-to-end by the SDK's
// own `apksigner verify`. The CANARY regression must trip this task.
lazy val checkAndroidSign = taskKey[Unit]("Assert androidSign produced a validly-signed APK (structure + apksigner verify)")

checkAndroidSign := Def.uncached {
  val log = streams.value.log
  val apk = (game.jvm(SgePlugin.scalaVersion) / AndroidBuild.androidSign).value
  val sdk = (game.jvm(SgePlugin.scalaVersion) / AndroidBuild.androidSdkRoot).value

  if (!apk.isFile) sys.error(s"[check] signed APK missing: $apk")

  // ── (a) structural: valid ZIP with classes.dex + v1 signature block ─────
  val names = {
    val zf = new java.util.zip.ZipFile(apk)
    try {
      val b = List.newBuilder[String]
      val es = zf.entries()
      while (es.hasMoreElements) b += es.nextElement().getName
      b.result()
    } finally zf.close()
  }
  if (!names.exists(_.endsWith(".dex"))) {
    sys.error(s"[check] APK contains no .dex entry (not a real app package): $apk\nentries: ${names.mkString(", ")}")
  }
  if (!names.contains("META-INF/MANIFEST.MF")) {
    sys.error(s"[check] APK missing META-INF/MANIFEST.MF (unsigned): $apk\nentries: ${names.mkString(", ")}")
  }
  if (!names.exists(n => n.startsWith("META-INF/") && n.endsWith(".SF"))) {
    sys.error(s"[check] APK missing META-INF/*.SF v1 signature file (unsigned): $apk\nentries: ${names.mkString(", ")}")
  }
  if (!names.exists(n => n.startsWith("META-INF/") && (n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC")))) {
    sys.error(s"[check] APK missing META-INF/*.{RSA,DSA,EC} v1 signature block (unsigned): $apk\nentries: ${names.mkString(", ")}")
  }

  // ── (b) cryptographic: apksigner verify (authoritative signature check) ──
  val apksigner = AndroidSdk.apksigner(sdk)
  val out       = new StringBuilder
  val collect   = ProcessLogger(l => { out.append(l).append('\n'); log.info(l) })
  val verifyCmd = Seq(apksigner.getAbsolutePath, "verify", "--verbose", apk.getAbsolutePath)
  val exit      = SysProcess(verifyCmd) ! collect
  if (exit != 0) {
    sys.error(s"[check] apksigner verify FAILED (exit $exit) for $apk:\n${out.toString}")
  }
  if (!out.toString.contains("Verifies")) {
    sys.error(s"[check] apksigner verify did not report 'Verifies' for $apk:\n${out.toString}")
  }

  log.info(s"[check] signed APK OK: $apk")
}
