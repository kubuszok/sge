import _root_.sge.sbt.{SgeNativeLibs, SgePackaging, SgePlugin}

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

// ── JVM runtime: native libs live in the main repo (one level up) ───

val demoJvmOpts: Def.Initialize[Seq[String]] = Def.setting {
  val repoRoot = (ThisBuild / baseDirectory).value / ".."
  val rustLib  = (repoRoot / "native-components" / "target" / "release").getAbsolutePath
  val brewLib = if (sys.props("os.name").toLowerCase.contains("mac")) {
    s"${java.io.File.pathSeparator}/opt/homebrew/lib${java.io.File.pathSeparator}/usr/local/lib"
  } else ""
  Seq(
    s"-Djava.library.path=$rustLib$brewLib",
    "--enable-native-access=ALL-UNNAMED"
  )
}

val demoJvmFixes: Seq[Setting[_]] = Seq(
  javaOptions      := demoJvmOpts.value,
  Test / javaOptions := demoJvmOpts.value
)

// ── Scala Native linking: Rust library in main repo ─────────────────

val demoNativeLibSettings: Seq[Setting[_]] = SgeNativeLibs.settings ++ Seq(
  SgeNativeLibs.sgeNativeLibDir := {
    val repoRoot = (ThisBuild / baseDirectory).value / ".."
    repoRoot / "native-components" / "target" / "release"
  }
)

val demoNativeLinking: Seq[Setting[_]] = demoNativeLibSettings ++ Seq(
  nativeConfig := {
    val c      = nativeConfig.value
    val libDir = SgeNativeLibs.sgeNativeLibDir.value
    c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
  }
)

// ── Common demo settings ────────────────────────────────────────────

val demoCommon: Seq[Setting[_]] =
  (SgePlugin.commonSettings ++ SgePlugin.relaxedSettings) :+
    (organization := "com.kubuszok") :+
    (publish / skip := true)

// ── Adoptium JDK 22 URLs for distribution packaging ──────────────

val adoptiumBase = "https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22.0.2%2B9"

val adoptiumJdkUrls: Map[String, String] = Map(
  "linux-x86_64"    -> s"$adoptiumBase/OpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz",
  "linux-aarch64"   -> s"$adoptiumBase/OpenJDK22U-jdk_aarch64_linux_hotspot_22.0.2_9.tar.gz",
  "macos-x86_64"    -> s"$adoptiumBase/OpenJDK22U-jdk_x64_mac_hotspot_22.0.2_9.tar.gz",
  "macos-aarch64"   -> s"$adoptiumBase/OpenJDK22U-jdk_aarch64_mac_hotspot_22.0.2_9.tar.gz",
  "windows-x86_64"  -> s"$adoptiumBase/OpenJDK22U-jdk_x64_windows_hotspot_22.0.2_9.zip",
  "windows-aarch64" -> s"$adoptiumBase/OpenJDK22U-jdk_aarch64_windows_hotspot_22.0.2_9.zip"
)

def demoJvm(dir: String, pkg: String, title: String): Seq[Setting[_]] =
  SgePlugin.jvmSettings(projectDir = dir) ++ demoJvmFixes ++
    SgePackaging.jvmSettings ++ SgePackaging.distSettings ++ Seq(
      Compile / mainClass        := Some(s"sge.demos.$pkg.DesktopMain"),
      SgePackaging.sgeAppName    := title,
      SgePackaging.sgeNativeLibDirs := Seq(
        (ThisBuild / baseDirectory).value / ".." / "native-components" / "target" / "release"
      ),
      SgePackaging.sgeTargets    := adoptiumJdkUrls,
      SgePackaging.sgeJlinkModules := Seq(
        "java.base", "java.desktop", "java.logging", "java.management",
        "jdk.unsupported", "jdk.zipfs", "java.net.http"
      )
    )

val demoJs: Seq[Setting[_]] =
  SgePlugin.jsSettings :+ (scalaJSUseMainModuleInitializer := true)

def demoNative(dir: String): Seq[Setting[_]] =
  SgePlugin.nativeSettings(projectDir = dir) ++ demoNativeLinking

// ── Shared demo framework ───────────────────────────────────────────

val shared = (projectMatrix in file("shared"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.relaxedSettings *)
  .settings(
    name           := "sge-demos-shared",
    organization   := "com.kubuszok",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.kubuszok" %%% "sge-core" % sgeVersion
    )
  )
  .jvmPlatform(scalaVersions = Seq(sv),
    settings = SgePlugin.jvmSettings(projectDir = "shared") ++ demoJvmFixes)
  .jsPlatform(scalaVersions = Seq(sv),
    settings = SgePlugin.jsSettings)
  .nativePlatform(scalaVersions = Seq(sv),
    settings = SgePlugin.nativeSettings(projectDir = "shared") ++ demoNativeLinking)

// ── Demo projects ───────────────────────────────────────────────────
// projectMatrix must be assigned directly to a val (sbt macro constraint).

val pong = (projectMatrix in file("pong"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-pong")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("pong", "pong", "SGE Pong"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("pong"))

val spaceShooter = (projectMatrix in file("space-shooter"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-spaceshooter")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("space-shooter", "spaceshooter", "SGE Space Shooter"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("space-shooter"))

val tileWorld = (projectMatrix in file("tile-world"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-tileworld")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("tile-world", "tileworld", "SGE Tile World"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("tile-world"))

val hexTactics = (projectMatrix in file("hex-tactics"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-hextactics")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("hex-tactics", "hextactics", "SGE Hex Tactics"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("hex-tactics"))

val curvePlayground = (projectMatrix in file("curve-playground"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-curves")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("curve-playground", "curves", "SGE Curves"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("curve-playground"))

val shaderLab = (projectMatrix in file("shader-lab"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-shaders")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("shader-lab", "shaders", "SGE Shader Lab"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("shader-lab"))

val viewer3d = (projectMatrix in file("viewer-3d"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-viewer3d")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("viewer-3d", "viewer3d", "SGE 3D Viewer"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("viewer-3d"))

val particleShow = (projectMatrix in file("particle-show"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-particles")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("particle-show", "particles", "SGE Particles"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("particle-show"))

val netChat = (projectMatrix in file("net-chat"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-netchat")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("net-chat", "netchat", "SGE Net Chat"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("net-chat"))

val viewportGallery = (projectMatrix in file("viewport-gallery"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(sv))
  .settings(demoCommon *).settings(name := "sge-demo-viewports")
  .dependsOn(shared)
  .jvmPlatform(scalaVersions = Seq(sv), settings = demoJvm("viewport-gallery", "viewports", "SGE Viewports"))
  .jsPlatform(scalaVersions = Seq(sv), settings = demoJs)
  .nativePlatform(scalaVersions = Seq(sv), settings = demoNative("viewport-gallery"))
