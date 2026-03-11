import _root_.sge.sbt.{AndroidBuild, AndroidSdk, JvmReleases, BrowserReleases, NativeReleases, Platform, SgePackaging, SgePlugin, SgeProject}
import sbt.internal.ProjectMatrix

Global / onChangedBuildSource := ReloadOnSourceChanges

val sgeVersion = "0.1.0-SNAPSHOT"
val sv         = SgePlugin.scalaVersion

// All modules need the snapshot resolver for transitive kindlings dependency.
ThisBuild / resolvers ++= Seq(
  Resolver.mavenLocal,
  "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
)

// Disable cached resolution — stale Coursier cache misses local SNAPSHOT JARs.
ThisBuild / updateOptions := updateOptions.value.withCachedResolution(false)

Global / excludeLintKeys += SgeProject.autoImport.sgeRustLibDir

// ── Rust library lives in the main repo (one level up) ──────────────

ThisBuild / SgeProject.autoImport.sgeRustLibDir := {
  (ThisBuild / baseDirectory).value / ".." / "native-components" / "target" / "release"
}

// ── Adoptium JDK 22 URLs for distribution packaging ──────────────

val adoptiumBase = "https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22.0.2%2B9"

val adoptiumJdkUrls: Map[Platform, String] = Map(
  Platform.LinuxX86_64    -> s"$adoptiumBase/OpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz",
  Platform.LinuxAarch64   -> s"$adoptiumBase/OpenJDK22U-jdk_aarch64_linux_hotspot_22.0.2_9.tar.gz",
  Platform.MacosX86_64    -> s"$adoptiumBase/OpenJDK22U-jdk_x64_mac_hotspot_22.0.2_9.tar.gz",
  Platform.MacosAarch64   -> s"$adoptiumBase/OpenJDK22U-jdk_aarch64_mac_hotspot_22.0.2_9.tar.gz",
  Platform.WindowsX86_64  -> s"$adoptiumBase/OpenJDK22U-jdk_x64_windows_hotspot_22.0.2_9.zip"
  // WindowsAarch64 omitted: Adoptium JDK 22 has no Windows ARM64 build
)

// ── Android SDK detection ────────────────────────────────────────────

lazy val hasAndroidSdk: Boolean = AndroidSdk
  .findSdkRoot(new File(".."))
  .exists(r => AndroidSdk.androidJar(r).exists())

// ── Per-axis settings ────────────────────────────────────────────────

def androidJvmSettings(dir: String): Seq[Setting[_]] =
  AndroidBuild.taskSettings ++ Seq(
    // Conditional scala-android/ sources (only when android.jar is present)
    Compile / unmanagedSourceDirectories ++= {
      if (hasAndroidSdk)
        Seq((ThisBuild / baseDirectory).value / dir / "src" / "main" / "scala-android")
      else Seq.empty
    },
    // android.jar from parent SDK (demos is a sub-build; root project has android-sdk/)
    Compile / unmanagedJars ++= {
      val parentBase = (ThisBuild / baseDirectory).value / ".."
      AndroidSdk.findSdkRoot(parentBase).toSeq.flatMap { sdkRoot =>
        val jar = AndroidSdk.androidJar(sdkRoot)
        if (jar.exists()) Seq(Attributed.blank(jar)) else Seq.empty
      }
    }
  )

def jvmAxis(dir: String, pkg: String): Seq[Setting[_]] =
  JvmReleases.axisSettings ++ androidJvmSettings(dir) ++ Seq(
    SgeProject.autoImport.sgeProjectDir := dir,
    Compile / mainClass := Some(s"sge.demos.$pkg.DesktopMain"),
    SgePackaging.sgeTargets := adoptiumJdkUrls,
    SgePackaging.sgeJlinkModules := Seq(
      "java.base", "java.desktop", "java.logging", "java.management",
      "jdk.unsupported", "jdk.zipfs", "java.net.http"
    )
  )

def jsAxis: Seq[Setting[_]] = BrowserReleases.axisSettings

def nativeAxis(dir: String): Seq[Setting[_]] =
  NativeReleases.axisSettings ++ Seq(
    SgeProject.autoImport.sgeProjectDir := dir,
    SgeProject.autoImport.sgeRustLibDir := {
      (ThisBuild / baseDirectory).value / ".." / "native-components" / "target" / "release"
    }
  )

// ── Shared demo framework ───────────────────────────────────────────

val shared = (projectMatrix in file("shared"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .enablePlugins(SgeProject)
  .settings(
    name           := "sge-demos-shared",
    organization   := "com.kubuszok",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.kubuszok" %%% "sge" % sgeVersion
    )
  )
  .jvmPlatform(scalaVersions = Seq(sv),
    settings = SgePlugin.jvmSettings(projectDir = "shared") ++ androidJvmSettings("shared") ++ Seq(
      fork := true,
      javaOptions ++= {
        val rustLib = SgeProject.autoImport.sgeRustLibDir.value.getAbsolutePath
        val brewLib = if (sys.props("os.name").toLowerCase.contains("mac")) {
          s"${java.io.File.pathSeparator}/opt/homebrew/lib${java.io.File.pathSeparator}/usr/local/lib"
        } else ""
        Seq(s"-Djava.library.path=$rustLib$brewLib", "--enable-native-access=ALL-UNNAMED")
      },
      Test / fork := true,
      Test / javaOptions ++= (javaOptions).value
    ))
  .jsPlatform(scalaVersions = Seq(sv),
    settings = SgePlugin.jsSettings)
  .nativePlatform(scalaVersions = Seq(sv),
    settings = SgePlugin.nativeSettings(projectDir = "shared") ++ _root_.sge.sbt.SgeNativeLibs.settings ++ Seq(
      SgeProject.autoImport.sgeRustLibDir := {
        (ThisBuild / baseDirectory).value / ".." / "native-components" / "target" / "release"
      },
      _root_.sge.sbt.SgeNativeLibs.sgeNativeLibDir := SgeProject.autoImport.sgeRustLibDir.value,
      nativeConfig := {
        val c = nativeConfig.value
        val libDir = _root_.sge.sbt.SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ _root_.sge.sbt.SgeNativeLibs.linkerFlags(libDir))
      }
    ))

// ── Demo projects ───────────────────────────────────────────────────
// projectMatrix must be assigned directly to a val (sbt macro constraint).

def demo(dir: String, sbtName: String, pkg: String, title: String)(matrix: ProjectMatrix): ProjectMatrix =
  matrix
    .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
    .enablePlugins(SgeProject)
    .settings(name := sbtName, organization := "com.kubuszok", publish / skip := true,
      SgeProject.autoImport.sgeAppName := title)
    .dependsOn(shared)
    .jvmPlatform(scalaVersions = Seq(sv), settings = jvmAxis(dir, pkg))
    .jsPlatform(scalaVersions = Seq(sv), settings = jsAxis)
    .nativePlatform(scalaVersions = Seq(sv), settings = nativeAxis(dir))

val pong             = demo("pong",              "sge-demo-pong",         "pong",         "SGE Pong")(projectMatrix in file("pong"))
val spaceShooter     = demo("space-shooter",     "sge-demo-spaceshooter", "spaceshooter", "SGE Space Shooter")(projectMatrix in file("space-shooter"))
val tileWorld        = demo("tile-world",        "sge-demo-tileworld",    "tileworld",    "SGE Tile World")(projectMatrix in file("tile-world"))
val hexTactics       = demo("hex-tactics",       "sge-demo-hextactics",   "hextactics",   "SGE Hex Tactics")(projectMatrix in file("hex-tactics"))
val curvePlayground  = demo("curve-playground",  "sge-demo-curves",       "curves",       "SGE Curves")(projectMatrix in file("curve-playground"))
val shaderLab        = demo("shader-lab",        "sge-demo-shaders",      "shaders",      "SGE Shader Lab")(projectMatrix in file("shader-lab"))
val viewer3d         = demo("viewer-3d",         "sge-demo-viewer3d",     "viewer3d",     "SGE 3D Viewer")(projectMatrix in file("viewer-3d"))
val particleShow     = demo("particle-show",     "sge-demo-particles",    "particles",    "SGE Particles")(projectMatrix in file("particle-show"))
val netChat          = demo("net-chat",          "sge-demo-netchat",      "netchat",      "SGE Net Chat")(projectMatrix in file("net-chat"))
val viewportGallery  = demo("viewport-gallery",  "sge-demo-viewports",    "viewports",    "SGE Viewports")(projectMatrix in file("viewport-gallery"))

// ── Release aliases ─────────────────────────────────────────────────
// Usage: sbt releasePong    — builds JVM (all 6 platforms) + Browser + Native
//        sbt releaseAll     — builds all demos

def releaseAlias(name: String, jvm: String, js: String, native: String): Seq[Setting[_]] =
  addCommandAlias(s"release${name}", s"$jvm/sgePackageAll; $js/sgePackageBrowser; $native/sgePackageNative")

releaseAlias("Pong",         "pong",            "pongJS",            "pongNative")
releaseAlias("SpaceShooter", "spaceShooter",    "spaceShooterJS",    "spaceShooterNative")
releaseAlias("TileWorld",    "tileWorld",       "tileWorldJS",       "tileWorldNative")
releaseAlias("HexTactics",   "hexTactics",      "hexTacticsJS",      "hexTacticsNative")
releaseAlias("Curves",       "curvePlayground", "curvePlaygroundJS", "curvePlaygroundNative")
releaseAlias("ShaderLab",    "shaderLab",       "shaderLabJS",       "shaderLabNative")
releaseAlias("Viewer3d",     "viewer3d",        "viewer3dJS",        "viewer3dNative")
releaseAlias("Particles",    "particleShow",    "particleShowJS",    "particleShowNative")
releaseAlias("NetChat",      "netChat",         "netChatJS",         "netChatNative")
releaseAlias("Viewports",    "viewportGallery", "viewportGalleryJS", "viewportGalleryNative")

addCommandAlias("releaseAll",
  "releasePong; releaseSpaceShooter; releaseTileWorld; releaseHexTactics; " +
  "releaseCurves; releaseShaderLab; releaseViewer3d; releaseParticles; " +
  "releaseNetChat; releaseViewports"
)

// ── Android APK aliases ───────────────────────────────────────────────
// Usage: sbt androidPong        — builds APK for Pong
//        sbt androidAll         — builds APKs for all demos
// Requires Android SDK: just android-sdk-setup

def androidAlias(name: String, jvm: String): Seq[Setting[_]] =
  addCommandAlias(s"android${name}", s"$jvm/androidSign")

androidAlias("Pong",         "pong")
androidAlias("SpaceShooter", "spaceShooter")
androidAlias("TileWorld",    "tileWorld")
androidAlias("HexTactics",   "hexTactics")
androidAlias("Curves",       "curvePlayground")
androidAlias("ShaderLab",    "shaderLab")
androidAlias("Viewer3d",     "viewer3d")
androidAlias("Particles",    "particleShow")
androidAlias("NetChat",      "netChat")
androidAlias("Viewports",    "viewportGallery")

addCommandAlias("androidAll",
  "androidPong; androidSpaceShooter; androidTileWorld; androidHexTactics; " +
  "androidCurves; androidShaderLab; androidViewer3d; androidParticles; " +
  "androidNetChat; androidViewports"
)
