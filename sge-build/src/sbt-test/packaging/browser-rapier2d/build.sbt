import sge.sbt.SgePlugin
import sge.sbt.SgePlugin.autoImport._
import sge.sbt.SgePackaging
import multiarch.sbt.JvmPackaging

// ISS-679 scripted test for the sge-build BROWSER packaging opt-in that ships
// the vendored rapier2d-compat global build (window.RAPIER) for physics-using
// games. Two game projects exercise both sides of the SettingKey:
//
//   gameOn  — SgePackaging.sgeBrowserIncludeRapier2d := true  → the package MUST
//             copy rapier2d-compat.umd.js into the output AND inject its <script>
//             tag BEFORE <script src="main.js"> in index.html.
//   gameOff — default (false)                                 → CANARY: neither
//             the umd file NOR the rapier <script> tag may appear (a non-physics
//             game must not be bloated, and existing packages stay byte-identical).
//
// As in the packaging/browser test, fullLinkJS is bypassed by overriding
// sgeJsOutputDir to a stub directory containing a fake main.js, so the real
// HTML/copy logic runs without invoking the Scala.js linker.

def stubJsOutputDir: Def.Setting[_] =
  // sbt 2.0 refuses to cache a File/Path task output; this stub produces a
  // directory as a side effect, so opt out of caching.
  SgePackaging.sgeJsOutputDir := Def.uncached {
    val stubDir = target.value / "stub-js"
    IO.createDirectory(stubDir)
    IO.write(stubDir / "main.js", "// fake fullLinkJS output for ISS-679 scripted test\n")
    stubDir
  }

def commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := "com.example",
  // Drop the sge engine + extension deps injected by the SGE platform plugins —
  // not published for this scripted plugin version, and not needed to run the
  // packaging task under test (the stub bypasses any real Scala.js linking).
  libraryDependencies := libraryDependencies.value.filterNot { m =>
    m.organization == "com.kubuszok" &&
    (m.name.startsWith("sge_") || m.name.startsWith("sge-extension"))
  },
  Compile / mainClass := Some("com.example.HelloGame"),
  fork := false
)

// Physics game: opt IN to the rapier2d global build.
lazy val gameOn = (projectMatrix in file("game-on"))
  .enablePlugins(SgePlugin)
  .settings(commonSettings)
  .settings(name := "hello-rapier-on")
  .jsPlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = Seq[Def.Setting[_]](
      stubJsOutputDir,
      SgePackaging.sgeBrowserIncludeRapier2d := true
    )
  )

// Non-physics game: default (flag off). Reuses the same game sources.
lazy val gameOff = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(commonSettings)
  .settings(name := "hello-rapier-off")
  .jsPlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = Seq[Def.Setting[_]](stubJsOutputDir)
  )

lazy val checkRapier2d = taskKey[Unit]("Assert sgeBrowserIncludeRapier2d ships rapier2d-compat.umd.js + injects its <script> before main.js when true, and neither when false (ISS-679)")

checkRapier2d := Def.uncached {
  val umdName = "rapier2d-compat.umd.js"
  val log     = streams.value.log

  // ── ON case: flag true ─────────────────────────────────────────────────
  val onDir  = (gameOn.js(SgePlugin.scalaVersion) / SgePackaging.sgePackageBrowser).value
  val onHtml = IO.read(onDir / "index.html")
  val onUmd  = onDir / umdName
  if (!onUmd.exists()) {
    sys.error(s"[check] ON: $umdName was not copied into the package: $onUmd")
  }
  if (onUmd.length() <= 0L) {
    sys.error(s"[check] ON: $umdName is empty: $onUmd")
  }
  val rapierTag = s"""<script src="$umdName"></script>"""
  val mainTag   = """<script src="main.js"></script>"""
  val rapierIdx = onHtml.indexOf(rapierTag)
  val mainIdx   = onHtml.indexOf(mainTag)
  if (rapierIdx < 0) {
    sys.error(s"[check] ON: index.html missing rapier script tag $rapierTag:\n$onHtml")
  }
  if (mainIdx < 0) {
    sys.error(s"[check] ON: index.html missing $mainTag:\n$onHtml")
  }
  if (rapierIdx >= mainIdx) {
    sys.error(s"[check] ON: rapier <script> must appear BEFORE main.js (rapierIdx=$rapierIdx, mainIdx=$mainIdx):\n$onHtml")
  }

  // ── OFF case: default (flag false) — CANARY ────────────────────────────
  val offDir  = (gameOff.js(SgePlugin.scalaVersion) / SgePackaging.sgePackageBrowser).value
  val offHtml = IO.read(offDir / "index.html")
  if ((offDir / umdName).exists()) {
    sys.error(s"[check] OFF CANARY: $umdName must NOT be copied when the flag is default/false: ${offDir / umdName}")
  }
  if (offHtml.contains(umdName)) {
    sys.error(s"[check] OFF CANARY: index.html must NOT reference $umdName when the flag is default/false:\n$offHtml")
  }
  if (!offHtml.contains(mainTag)) {
    sys.error(s"[check] OFF: index.html still must contain $mainTag:\n$offHtml")
  }

  log.info(s"[check] rapier2d browser packaging OK — ON ships+injects $umdName before main.js; OFF ships neither")
}
