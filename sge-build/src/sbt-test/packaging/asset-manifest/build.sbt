import sge.sbt.SgePlugin
import sge.sbt.SgePlugin.autoImport._
import sge.sbt.SgePackaging
import multiarch.sbt.JvmPackaging
import multiarch.sbt.MultiArchResourcesPlugin

// Minimal SGE game project used purely to exercise the packaging tasks shipped
// by the sge-build plugin (ISS-562 scripted test). It enables SgePlugin (so the
// SGE packaging settings — releasePackage + browser embedding — are wired in)
// but DROPS the heavy `sge` library + extension dependencies that the JVM
// platform plugin adds by default. The scripted harness only has the *plugin*
// published locally, not the full SGE library for this exact version, and the
// packaging tasks under test don't need the engine on the classpath to run. The
// single source file is a plain `main` with no SGE imports so the project
// compiles standalone.
//
// This test was REPURPOSED for the multiarch-resources migration: the bespoke
// assets.txt manifest generator (sgeGenerateAssetManifest) is gone. Browser
// assets are now embedded at compile time by
// MultiArchResourcesPlugin.embeddedResourcesSettings, which generates a
// self-registering object. This test asserts that generated object embeds the
// expected resource paths (and that AndroidManifest.xml is included verbatim as
// just another resource — there is no special classification any more).
lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(
    name         := "hello-sge",
    organization := "com.example",
    // Drop the sge engine + extension deps injected by SgeDesktopJvmPlatform —
    // not published for this scripted plugin version, and not needed to run the
    // packaging tasks under test.
    libraryDependencies := libraryDependencies.value.filterNot { m =>
      m.organization == "com.kubuszok" &&
      (m.name.startsWith("sge_") || m.name.startsWith("sge-extension"))
    },
    // The generated embedded-resources object imports
    // multiarch.resources.EmbeddedResources, so the runtime library must be on
    // the classpath (sge-core depends on it the same way).
    libraryDependencies += "com.kubuszok" %% "multiarch-resources" % "0.3.0",
    Compile / mainClass := Some("com.example.HelloGame"),
    // Don't fork test JVM with native lib paths we never built.
    fork := false
  )
  // JVM axis: exercises releasePackage (simple-mode launcher scripts + jars).
  .jvmPlatform()
  // JS (browser) axis: wire the embedded-resources sourceGenerator (as sge-core
  // does). Compiling the JS axis runs it and produces the generated object.
  .jsPlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = MultiArchResourcesPlugin.embeddedResourcesSettings(
      objectName = "com.example.GeneratedEmbeddedResources"
    )
  )

// A custom task that compiles the JS axis (running the embedded-resources
// sourceGenerator) and reads the generated object, failing loudly if any
// expected asset path is missing. This is what the scripted `test` script drives
// and what the CANARY regression must trip.
lazy val checkEmbeddedResources = taskKey[Unit]("Assert the generated embedded-resources object embeds the expected resource paths")

checkEmbeddedResources := Def.uncached {
  // Force compilation so the embedded-resources sourceGenerator runs.
  val _      = (game.js(SgePlugin.scalaVersion) / Compile / compile).value
  val srcMgd = (game.js(SgePlugin.scalaVersion) / Compile / sourceManaged).value
  val genFile = srcMgd / "com" / "example" / "GeneratedEmbeddedResources.scala"
  if (!genFile.exists()) sys.error(s"[check] generated embedded-resources object missing: $genFile")
  val genBody = IO.read(genFile)
  if (!genBody.contains("object GeneratedEmbeddedResources")) {
    sys.error(s"[check] generated file does not define object GeneratedEmbeddedResources:\n$genFile")
  }
  if (!genBody.contains("EmbeddedResources.register")) {
    sys.error(s"[check] generated file does not self-register via EmbeddedResources.register:\n$genFile")
  }
  // The generated object must embed the resource paths (classpath-absolute keys).
  val expectedKeys = Seq("/assets/hello.txt", "/assets/data/x.json")
  val missingKeys  = expectedKeys.filterNot(genBody.contains)
  if (missingKeys.nonEmpty) {
    sys.error(s"[check] generated object missing expected resource keys: ${missingKeys.mkString(", ")}\n$genFile")
  }
  // CANARY: the old assets.txt manifest path must NEVER be generated as a
  // resource directory artifact for the JS axis.
  val staleManifest = (game.js(SgePlugin.scalaVersion) / Compile / resourceManaged).value / "assets.txt"
  if (staleManifest.exists()) {
    sys.error(s"[check] CANARY: assets.txt manifest must NOT be generated (old mechanism resurfaced): $staleManifest")
  }
  streams.value.log.info(s"[check] embedded-resources object OK: $genFile")
}

// Assert the simple-mode release package produced bin/ launchers + the app jar.
lazy val checkReleasePackage = taskKey[Unit]("Assert releasePackage produced launchers + jars")

checkReleasePackage := Def.uncached {
  val outDir  = (game.jvm(SgePlugin.scalaVersion) / JvmPackaging.releasePackage).value
  val appName = (game.jvm(SgePlugin.scalaVersion) / JvmPackaging.releaseAppName).value
  val unix    = outDir / "bin" / appName
  val bat     = outDir / "bin" / s"$appName.bat"
  val libDir  = outDir / "lib"
  if (!outDir.isDirectory) sys.error(s"[check] release dir missing: $outDir")
  if (!unix.exists()) sys.error(s"[check] unix launcher missing: $unix")
  if (!bat.exists()) sys.error(s"[check] windows launcher missing: $bat")
  if (!libDir.isDirectory || IO.listFiles(libDir).isEmpty) {
    sys.error(s"[check] lib/ missing or empty: $libDir")
  }
  val unixBody = IO.read(unix)
  if (!unixBody.contains("com.example.HelloGame")) {
    sys.error(s"[check] unix launcher does not reference main class:\n$unixBody")
  }
  streams.value.log.info(s"[check] release package OK: $outDir")
}
