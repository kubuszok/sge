import _root_.multiarch.sbt.{EmbeddedResourcesGen, JvmPackaging, Platform}
import _root_.sge.sbt.SgePlugin
import sbt.ProjectMatrix

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  // CI publishes the SGE core + extension SNAPSHOTs via the `publishLocal-{jvm,js,native}-3`
  // aliases, which deliver to the Ivy local repo (~/.ivy2/local, sbt's "local"
  // resolver / Resolver.defaultLocal). The release/verify jobs additionally
  // publishM2 (mavenLocal) for sge core. Resolve from both so extension artifacts
  // (e.g. sge-extension-noise) are found wherever the publishing job put them.
  Resolver.defaultLocal,
  "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
)
ThisBuild / updateOptions := updateOptions.value.withCachedResolution(false)

// ── Azul Zulu JDK 25 URLs for distribution packaging ─────────────

val zuluBase = "https://cdn.azul.com/zulu/bin"

val jdkUrls: Map[Platform, String] = Map(
  Platform.LinuxX86_64    -> s"$zuluBase/zulu25.32.21-ca-jdk25.0.2-linux_x64.tar.gz",
  Platform.LinuxAarch64   -> s"$zuluBase/zulu25.32.21-ca-jdk25.0.2-linux_aarch64.tar.gz",
  Platform.MacosX86_64    -> s"$zuluBase/zulu25.32.21-ca-jdk25.0.2-macosx_x64.tar.gz",
  Platform.MacosAarch64   -> s"$zuluBase/zulu25.32.21-ca-jdk25.0.2-macosx_aarch64.tar.gz",
  Platform.WindowsX86_64  -> s"$zuluBase/zulu25.32.21-ca-jdk25.0.2-win_x64.zip",
  Platform.WindowsAarch64 -> s"$zuluBase/zulu25.32.21-ca-jdk25.0.2-win_aarch64.zip"
)

val demoDistSettings: Seq[Setting[_]] = Seq(
  JvmPackaging.releaseTargets := jdkUrls,
  JvmPackaging.releaseJlinkModules := Seq(
    "java.base", "java.desktop", "java.logging", "java.management",
    "jdk.unsupported", "jdk.zipfs", "java.net.http"
  )
)

// ── Shared demo framework ───────────────────────────────────────────

val shared = (projectMatrix in file("shared"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(SgePlugin.scalaVersion))
  .enablePlugins(SgePlugin)
  .settings(
    name           := "sge-demos-shared",
    organization   := "com.kubuszok",
    publish / skip := true,
    libraryDependencies += "com.kubuszok" %% "lls" % "0.2.0"
  )
  .jvmPlatform()
  .jsPlatform()
  .nativePlatform()

// ── Browser asset embedding ─────────────────────────────────────────
// Scala.js has no classpath, so each browser app must embed its OWN
// Compile resources at build time into a self-registering generated
// object (multiarch.resources.EmbeddedResources.register at init).
// `embeddedResourcesSettings` embeds a SINGLE directory, so we first
// STAGE the full Compile resource set (unmanaged src/main/resources +
// managed/generated resources like asset-showcase's PNG/WAV — see
// AssetGenerator) into one directory, then embed that. The generated
// object's FQN is unique per demo (demos.<pkg>.GeneratedEmbeddedResources)
// so a demo+sge link never clashes with sge's own
// sge.platform.GeneratedEmbeddedResources. The per-demo BrowserMain
// references the object once to defeat Scala.js DCE.

def browserEmbeddedResources(pkg: String): Seq[Setting[_]] = {
  // The multiarch helper (`MultiArchResourcesPlugin.embeddedResourcesSettings`)
  // embeds a SINGLE setting-derived directory (default: src/main/resources). That
  // misses generated/managed resources (e.g. asset-showcase's PNG/WAV from
  // AssetGenerator's resourceGenerators). So we wire our own `sourceGenerators`
  // entry that (1) STAGES the FULL `Compile / resources` set — unmanaged +
  // managed — into one directory, then (2) calls the SAME generator the plugin
  // uses (`EmbeddedResourcesGen.generate`) on that staged dir.
  //
  // Depending on `Compile / resources` from the SOURCE-generation path is safe:
  // it does NOT create the `resourceGenerators -> resources -> resourceGenerators`
  // cycle that adding the staging task to `resourceGenerators` would.
  //
  // The generated object's FQN is unique per demo
  // (demos.<pkg>.GeneratedEmbeddedResources), so a demo+sge link never clashes
  // with sge's own sge.platform.GeneratedEmbeddedResources; the per-demo
  // BrowserMain references it once to defeat Scala.js DCE.
  val objectName = s"demos.$pkg.GeneratedEmbeddedResources"
  Seq(
    Compile / sourceGenerators += Def.task {
      val staged = (Compile / resourceManaged).value.getParentFile / "embedded-resources-staged"
      val dirs   = (Compile / resourceDirectories).value
      val all    = (Compile / resources).value
      val log    = streams.value.log
      IO.delete(staged)
      IO.createDirectory(staged)
      all.foreach { f =>
        if (f.isFile) {
          // Preserve each resource's path relative to its resource-root so the
          // embedded classpath key matches the JVM/Native classpath key.
          val root = dirs.find(d => f.getAbsolutePath.startsWith(d.getAbsolutePath + java.io.File.separator))
          val rel  = root.map(r => r.toPath.relativize(f.toPath).toString).getOrElse(f.getName)
          IO.copyFile(f, staged / rel)
        }
      }
      val segments = objectName.split('.').toSeq
      val dirPart  = segments.init.foldLeft((Compile / sourceManaged).value)(_ / _)
      val outFile  = dirPart / (segments.last + ".scala")
      // sbt-2.0 refuses to cache a File/Seq[File] task result without a HashWriter;
      // opt out, matching the multiarch helper's own Compat.uncached usage.
      Def.uncached(Seq(EmbeddedResourcesGen.generate(staged, outFile, objectName, log)))
    }.taskValue
  )
}

// ── Demo projects ───────────────────────────────────────────────────

def demo(dir: String, sbtName: String, pkg: String, title: String,
         extensions: Set[sge.sbt.SgeExtension] = Set.empty)(matrix: ProjectMatrix): ProjectMatrix =
  matrix
    .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(SgePlugin.scalaVersion))
    .enablePlugins(SgePlugin)
    .settings(name := sbtName, organization := "com.kubuszok", publish / skip := true,
      JvmPackaging.releaseAppName := title,
      sgeExtensions := extensions)
    .dependsOn(shared)
    .jvmPlatform(demoDistSettings ++ Seq(Compile / mainClass := Some(s"demos.$pkg.DesktopMain")))
    .jsPlatform(browserEmbeddedResources(pkg))
    .nativePlatform()
    .withCrossNative

val pong             = demo("pong",              "sge-demo-pong",         "pong",         "SGE Pong")(projectMatrix in file("pong"))
val spaceShooter     = demo("space-shooter",     "sge-demo-spaceshooter", "spaceshooter", "SGE Space Shooter")(projectMatrix in file("space-shooter"))
val tileWorld        = demo("tile-world",        "sge-demo-tileworld",    "tileworld",    "SGE Tile World",
  extensions = Set(sge.sbt.SgeExtension.Noise))(projectMatrix in file("tile-world"))
val hexTactics       = demo("hex-tactics",       "sge-demo-hextactics",   "hextactics",   "SGE Hex Tactics")(projectMatrix in file("hex-tactics"))
val curvePlayground  = demo("curve-playground",  "sge-demo-curves",       "curves",       "SGE Curves")(projectMatrix in file("curve-playground"))
val shaderLab        = demo("shader-lab",        "sge-demo-shaders",      "shaders",      "SGE Shader Lab")(projectMatrix in file("shader-lab"))
val viewer3d         = demo("viewer-3d",         "sge-demo-viewer3d",     "viewer3d",     "SGE 3D Viewer")(projectMatrix in file("viewer-3d"))
val particleShow     = demo("particle-show",     "sge-demo-particles",    "particles",    "SGE Particles")(projectMatrix in file("particle-show"))
val netChat          = demo("net-chat",          "sge-demo-netchat",      "netchat",      "SGE Net Chat")(projectMatrix in file("net-chat"))
val viewportGallery  = demo("viewport-gallery",  "sge-demo-viewports",    "viewports",    "SGE Viewports")(projectMatrix in file("viewport-gallery"))
// Game/Screen demo: built directly on SGE's public Game/Screen API (not the
// DemoScene abstraction the other demos use). JVM/JS/Native only — Android
// Game-launch is blocked by ISS-554 (no library Game-launcher Activity yet).
val gameScreens      = demo("game-screens",      "sge-demo-gamescreens",  "gamescreens",  "SGE Game/Screen")(projectMatrix in file("game-screens"))

// Asset Showcase: procedural PNG textures and WAV audio generated at compile time
// (see project/AssetGenerator.scala).
val assetShowcase = (projectMatrix in file("asset-showcase"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(SgePlugin.scalaVersion))
  .enablePlugins(SgePlugin)
  .settings(
    name := "sge-demo-assets", organization := "com.kubuszok", publish / skip := true,
    JvmPackaging.releaseAppName := "SGE Asset Showcase"
  )
  .settings(AssetGenerator.settings)
  .dependsOn(shared)
  .jvmPlatform(demoDistSettings ++ Seq(Compile / mainClass := Some("demos.assets.DesktopMain")))
  .jsPlatform(browserEmbeddedResources("assets"))
  .nativePlatform()
  .withCrossNative

// ── Release aliases ─────────────────────────────────────────────────
// Each demo's js/native project IDs are always `${jvm}JS` / `${jvm}Native`.

def releaseAlias(name: String, jvm: String): Seq[Setting[_]] =
  addCommandAlias(s"release${name}",
    s"$jvm/releaseAll; ${jvm}JS/sgePackageBrowser; ${jvm}Native/sgePackageNative")

def androidAlias(name: String, jvm: String): Seq[Setting[_]] =
  addCommandAlias(s"android${name}", s"$jvm/androidSign")

val demoAliases: Seq[(String, String)] = Seq(
  "Pong"         -> "pong",
  "SpaceShooter" -> "spaceShooter",
  "TileWorld"    -> "tileWorld",
  "HexTactics"   -> "hexTactics",
  "Curves"       -> "curvePlayground",
  "ShaderLab"    -> "shaderLab",
  "Viewer3d"     -> "viewer3d",
  "Particles"    -> "particleShow",
  "NetChat"      -> "netChat",
  "Viewports"    -> "viewportGallery",
  "Assets"       -> "assetShowcase"
)

demoAliases.flatMap { case (n, j) => releaseAlias(n, j) }
demoAliases.flatMap { case (n, j) => androidAlias(n, j) }

addCommandAlias("releaseAll",
  demoAliases.map { case (n, _) => s"release$n" }.mkString("; ")
)

addCommandAlias("androidAll",
  demoAliases.map { case (n, _) => s"android$n" }.mkString("; ")
)

// ── Collect all release artifacts into one directory ──────────────

val collectReleases = taskKey[File]("Collect all release artifacts into target/releases/")

collectReleases := Def.uncached {
  val log = streams.value.log
  val outDir = baseDirectory.value / "target" / "releases"
  IO.delete(outDir)
  IO.createDirectory(outDir)

  val archiveExts = Set(".tar.gz", ".zip")
  def isArchive(f: File): Boolean =
    f.isFile && archiveExts.exists(e => f.getName.endsWith(e))

  val demoDirs = Seq(
    "pong", "space-shooter", "tile-world", "hex-tactics",
    "curve-playground", "shader-lab", "viewer-3d", "particle-show",
    "net-chat", "viewport-gallery", "asset-showcase"
  )

  var count = 0
  demoDirs.foreach { dir =>
    val projectTarget = baseDirectory.value / dir / "target"
    if (projectTarget.exists()) {
      // JVM dist archives
      val jvmDist = projectTarget / "jvm-3" / "release-dist"
      if (jvmDist.exists()) {
        IO.listFiles(jvmDist).filter(isArchive).foreach { f =>
          IO.copyFile(f, outDir / f.getName)
          count += 1
        }
      }
      // Browser package
      val jsBrowser = projectTarget / "js-3" / "sge-browser"
      if (jsBrowser.exists()) {
        IO.listFiles(jsBrowser).filter(_.isDirectory).foreach { appDir =>
          val archive = outDir / s"${appDir.getName}-browser.tar.gz"
          val cmd = Seq("tar", "czf", archive.getAbsolutePath, "-C", jsBrowser.getAbsolutePath, appDir.getName)
          val proc = new ProcessBuilder(cmd: _*).redirectErrorStream(true).start()
          if (proc.waitFor() == 0) { count += 1 }
          else log.warn(s"[sge] Failed to archive browser package: ${appDir.getName}")
        }
      }
      // Native archives
      val nativeDist = projectTarget / "native-3" / "sge-native"
      if (nativeDist.exists()) {
        IO.listFiles(nativeDist).filter(isArchive).foreach { f =>
          IO.copyFile(f, outDir / f.getName)
          count += 1
        }
      }
      // Android APK
      val androidApk = projectTarget / "jvm-3" / "android" / "app-debug.apk"
      if (androidApk.exists()) {
        IO.copyFile(androidApk, outDir / s"$dir.apk")
        count += 1
      }
    }
  }

  log.info(s"[sge] Collected $count artifact(s) into ${outDir.getAbsolutePath}")
  outDir
}
