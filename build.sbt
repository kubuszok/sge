import _root_.multiarch.sbt.Platform
import _root_.sge.sbt.{SgeNativeLibs, SgePlugin}

// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / packageDoc / publishArtifact := false
ThisBuild / scalafmtOnCompile := !isCI

// Version from git tags: tagged commits get clean versions (e.g. "0.1.0"),
// untagged commits get SNAPSHOT versions (e.g. "0.1.0-SNAPSHOT").
// When a vX.Y.Z tag exists, git describe produces that version directly.
git.useGitDescribe       := true
git.uncommittedSignifier := Some("SNAPSHOT")
// Sonatype ignores isSnapshot setting and only looks at -SNAPSHOT suffix in version:
//   https://central.sonatype.org/publish/publish-maven/#performing-a-snapshot-deployment
// meanwhile sbt-git used to set up SNAPSHOT if there were uncommitted changes:
//   https://github.com/sbt/sbt-git/issues/164
// (now this suffix is empty by default) so we need to fix it manually.
git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty
// I don't want any 0.1.0 crap, every commit that is not tag, gets last-tag-SHA-SNAPSHOT version like god intended.
//git.formattedShaVersion  := git.gitHeadCommit.value.map(_ => s"${git.baseVersion.value}-SNAPSHOT")

// Used to publish snapshots to Maven Central.
val mavenCentralSnapshots = "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"

val publishSettings = Seq(
  organization := "com.kubuszok",
  homepage := Some(url("https://github.com/kubuszok/sge")),
  organizationHomepage := Some(url("https://kubuszok.com")),
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/kubuszok/sge/"),
      "scm:git:git@github.com:kubuszok/sge.git"
    )
  ),
  startYear := Some(2026),
  developers := List(
    Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://github.com/MateuszKubuszok"))
  ),
  pomExtra := (
    <issueManagement>
      <system>GitHub issues</system>
      <url>https://github.com/kubuszok/sge/issues</url>
    </issueManagement>
  ),
  publishTo := {
    if (isSnapshot.value) Some(mavenCentralSnapshots)
    else localStaging.value
  },
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  versionScheme := Some("early-semver")
)

val noPublishSettings =
  Seq(publish / skip := true, publishArtifact := false)

/** Collect all files from a class directory as (File, relative-path) pairs for JAR mappings. */
def collectClassFiles(classDir: File): Seq[(File, String)] =
  Path.allSubpaths(classDir).toSeq

val versions = new {
  val scala = SgePlugin.scalaVersion

  val kindlings = "0.1.2"
  val sttp      = "4.0.22"
  val xml       = "2.3.0"
  val scribe    = "3.17.0"
  val scalajsDom = "2.8.1"
  val gears      = "0.2.0"

  val panamaPort = "v0.1.0"

  val scalaSaxParser   = "0.1.0"
  val scalaJavaTime    = "2.6.0"
  val scalaJavaLocales = "1.5.4"

  val munit           = "1.2.3"
  val munitScalacheck = "1.2.0"

  // Native component providers (from sge-native-providers repo)
  val nativeComponents = "0.1.1"
  val curlProvider     = "0.1.1"
}

val sge = (projectMatrix in file("sge"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge",
    resolvers += mavenCentralSnapshots,
    libraryDependencies ++= Seq(
      "com.kubuszok" %%% "kindlings-jsoniter-derivation" % versions.kindlings,
      "com.kubuszok" %%% "kindlings-jsoniter-json" % versions.kindlings,
      "com.kubuszok" %%% "kindlings-ubjson-derivation" % versions.kindlings,
      "com.softwaremill.sttp.client4" %%% "core" % versions.sttp,
      "org.scala-lang.modules" %%% "scala-xml" % versions.xml,
      "com.kubuszok" %%% "scala-sax-parser" % versions.scalaSaxParser,
      "com.outr" %%% "scribe" % versions.scribe,
      "io.github.cquiroz" %%% "scala-java-time" % versions.scalaJavaTime,
      "io.github.cquiroz" %%% "scala-java-locales" % versions.scalaJavaLocales,
      "org.scalameta" %%% "munit" % versions.munit % Test,
      "org.scalameta" %%% "munit-scalacheck" % versions.munitScalacheck % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings() ++ SgeNativeLibs.validationSettings ++ Seq(
      libraryDependencies += "ch.epfl.lamp" %% "gears" % versions.gears,
      // multiarch-core for NativeLibLoader (runtime shared library loading)
      libraryDependencies += "com.kubuszok" %% "multiarch-core" % versions.curlProvider,
      // JVM platform modules on classpath (no dependsOn — avoids transitive dep for consumers).
      // All 3 needed: API for compilation, JDK + Android for runtime provider detection.
      Compile / unmanagedClasspath ++= {
        val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
        (apiDirs ++ jdkDirs ++ androidDirs).map(Attributed.blank)
      },
      // Also on test classpath (forked JVM doesn't inherit Compile / unmanagedClasspath)
      Test / unmanagedClasspath ++= {
        val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
        (apiDirs ++ jdkDirs ++ androidDirs).map(Attributed.blank)
      },
      // Merge all 3 platform modules' class files into sge's JVM JAR
      Compile / packageBin / mappings ++= {
        val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
        (apiDirs ++ jdkDirs ++ androidDirs).flatMap(collectClassFiles)
      },
      // Bundle core + ANGLE native shared libraries for all platforms into the JAR.
      // Extension libs (sge_freetype, sge_physics) are bundled by their respective extension JARs.
      Compile / packageBin / mappings ++= {
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isSharedLib(name: String) = sharedLibExts.exists(name.endsWith)
        // Only bundle core + ANGLE shared libs in the sge JAR (exclude extension libs)
        def isCoreLib(name: String) = isSharedLib(name) && !name.contains("sge_freetype") && !name.contains("sge_physics")
        // Cross-compiled: all 6 platforms from target/cross/<platform>/
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isCoreLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        // Fallback: host platform only from target/release/ (local dev)
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isCoreLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        // Android: native libs from target/<rustTarget>/release/
        val androidMappings = Seq(
          ("aarch64-linux-android",    "android-aarch64"),
          ("armv7-linux-androideabi",  "android-armv7"),
          ("x86_64-linux-android",     "android-x86_64")
        ).flatMap { case (rustTarget, classifier) =>
          val dir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / rustTarget / "release"
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isCoreLib(f.getName))
            .map(f => f -> s"native/$classifier/${f.getName}")
            .toSeq
          else Seq.empty
        }
        crossMappings ++ hostMappings ++ androidMappings
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings ++ Seq(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % versions.scalajsDom
    )
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings() ++ _root_.multiarch.sbt.NativeProviderPlugin.projectSettings ++ Seq(
      // Native library providers (fat JARs with sn-provider.json manifests)
      // sn-provider-sge transitively pulls sn-provider-sge-angle
      libraryDependencies ++= Seq(
        "com.kubuszok" % "sn-provider-sge"  % versions.nativeComponents,
        "com.kubuszok" % "sn-provider-curl" % versions.curlProvider
      )
    )
  )

val regressionTest = (projectMatrix in file("sge-test/regression"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.relaxedSettings *)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    name := "sge-test-regression",
    // Generate minimal test assets at compile time (PNG + text) so we can
    // exercise the full AssetManager → FileHandle → Texture pipeline.
    Compile / resourceGenerators += Def.task {
      val dir = (Compile / resourceManaged).value / "regression"
      IO.createDirectory(dir)
      // Generate a 4×4 red PNG via ImageIO (guaranteed valid across all platforms)
      val pngFile = dir / "test-texture.png"
      if (!pngFile.exists()) {
        val img = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g   = img.createGraphics()
        g.setColor(java.awt.Color.RED)
        g.fillRect(0, 0, 4, 4)
        g.dispose()
        javax.imageio.ImageIO.write(img, "PNG", pngFile)
      }
      // Simple text file for FileHandle read test
      val txtFile = dir / "test-data.txt"
      IO.write(txtFile, "SGE_REGRESSION_TEST_DATA")
      // Asset manifest for browser preloading (BrowserAssetLoader reads assets.txt)
      val manifestFile = (Compile / resourceManaged).value / "assets.txt"
      IO.write(manifestFile, "regression/test-texture.png\nregression/test-data.txt\n")
      Seq(pngFile, txtFile, manifestFile)
    }.taskValue
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-test/regression")
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings ++ Seq(
      scalaJSUseMainModuleInitializer := true
    )
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-test/regression") ++
      _root_.multiarch.sbt.NativeProviderPlugin.projectSettings
  )

// ── JVM Platform modules ──────────────────────────────────────────────
//
// Three modules isolate JDK-version-dependent and platform-specific code:
//   sge-jvm-platform-api     — PanamaProvider trait + Android ops interfaces (JDK 17)
//   sge-jvm-platform-jdk     — JdkPanama impl (JDK 22+, java.lang.foreign)
//   sge-jvm-platform-android — PanamaPort + Android backend impls (JDK 17, android.jar)
//
// None are published. Their class files are merged into sge's JVM JAR
// at package time (see sge jvmPlatform settings). Runtime feature
// detection in sge picks the right provider/implementation.

lazy val `sge-jvm-platform-api` = (project in file("sge-jvm-platform/api"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    // Target JDK 17 bytecode — no java.lang.foreign or android.* references
    scalacOptions ++= Seq("-release", "17")
  )

lazy val `sge-jvm-platform-jdk` = (project in file("sge-jvm-platform/jdk"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    // No -release flag — needs java.lang.foreign (JDK 22+)
  )
  .dependsOn(`sge-jvm-platform-api`)

// SGE convention: downloaded Android SDK lives under sge-deps/
lazy val sgeAndroidSdkCacheDir: File = new File("./sge-deps/android-sdk")
lazy val hasAndroidSdk: Boolean      = _root_.multiarch.sbt.AndroidSdk
  .findSdkRoot(sgeAndroidSdkCacheDir)
  .exists(r => _root_.multiarch.sbt.AndroidSdk.androidJar(r).exists())

lazy val `sge-jvm-platform-android` = (project in file("sge-jvm-platform/android"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    publish / skip := true,
    // Target JDK 17 bytecode (Android ART)
    scalacOptions ++= Seq("-release", "17"),
    // src/main/scala/ always compiles (PanamaPortProvider — no Android SDK deps).
    // src/main/scala-android/ only compiled when android.jar is present.
    Compile / unmanagedJars ++= {
      val base     = (ThisBuild / baseDirectory).value
      val cacheDir = base / "sge-deps" / "android-sdk"
      _root_.multiarch.sbt.AndroidSdk.findSdkRoot(cacheDir).toSeq.flatMap { sdkRoot =>
        val jar = _root_.multiarch.sbt.AndroidSdk.androidJar(sdkRoot)
        if (jar.exists()) Seq(Attributed.blank(jar)) else Seq.empty
      }
    },
    Compile / unmanagedSourceDirectories ++= {
      if (hasAndroidSdk)
        Seq(baseDirectory.value / "src" / "main" / "scala-android")
      else Seq.empty
    },
    // PanamaPort — Panama FFM backport for Android (API 26+).
    // Published as AAR (not JAR), so we resolve via coursier, extract classes.jar, and add as unmanaged.
    Compile / unmanagedJars ++= {
      val aarUrl  = s"https://repo1.maven.org/maven2/io/github/vova7878/panama/Core/${versions.panamaPort}/Core-${versions.panamaPort}.aar"
      val cacheDir = streams.value.cacheDirectory / "panama-port"
      val aarFile  = cacheDir / s"Core-${versions.panamaPort}.aar"
      val jarFile  = cacheDir / s"Core-${versions.panamaPort}-classes.jar"
      val log = streams.value.log
      if (!jarFile.exists()) {
        IO.createDirectory(cacheDir)
        if (!aarFile.exists()) {
          log.info(s"Downloading PanamaPort AAR from $aarUrl")
          val in = new java.net.URL(aarUrl).openStream()
          try { IO.transfer(in, aarFile) }
          finally { in.close() }
        }
        // AAR is a ZIP; extract classes.jar from it
        log.info(s"Extracting classes.jar from PanamaPort AAR")
        val zip = new java.util.zip.ZipFile(aarFile)
        try {
          val entry = zip.getEntry("classes.jar")
          if (entry == null) sys.error("PanamaPort AAR does not contain classes.jar")
          val in = zip.getInputStream(entry)
          try { IO.transfer(in, jarFile) }
          finally { in.close() }
        } finally { zip.close() }
      }
      Seq(Attributed.blank(jarFile))
    }
  )
  .dependsOn(`sge-jvm-platform-api`)

// ── Extension modules ─────────────────────────────────────────────────
//
// sge-tools    — JVM-only build tools (TexturePacker)
// sge-freetype — Cross-platform FreeType font rasterization
// sge-physics  — Cross-platform 2D physics via Rapier2D
//
// These are published separately from sge-core. They depend on sge.

lazy val `sge-tools` = (project in file("sge-extension/tools"))
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.relaxedSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-tools",
    scalaVersion := versions.scala,
    resolvers += mavenCentralSnapshots,
    Compile / mainClass := Some("sge.tools.texturepacker.TexturePacker"),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    // sge-tools needs sge JVM classes on its classpath
    Compile / unmanagedClasspath ++= {
      val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
      val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
      val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
      (apiDirs ++ jdkDirs ++ androidDirs).map(Attributed.blank)
    }
  )
  .dependsOn(sge.jvm(versions.scala))

val `sge-freetype` = (projectMatrix in file("sge-extension/freetype"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-freetype",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/freetype") ++ Seq(
      resolvers += mavenCentralSnapshots,
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      },
      // Bundle sge_freetype shared libs into the extension JAR
      Compile / packageBin / mappings ++= {
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isFreetypeLib(name: String) = sharedLibExts.exists(name.endsWith) && name.contains("sge_freetype")
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isFreetypeLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isFreetypeLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        crossMappings ++ hostMappings
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/freetype") ++
      _root_.multiarch.sbt.NativeProviderPlugin.projectSettings ++ Seq(
      resolvers += mavenCentralSnapshots,
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-freetype" % versions.nativeComponents
    )
  )
  .dependsOn(sge)

val `sge-physics` = (projectMatrix in file("sge-extension/physics"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-physics",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/physics") ++ Seq(
      resolvers += mavenCentralSnapshots,
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-physics-desktop" % versions.nativeComponents,
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      },
      Test / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      },
      // Bundle sge_physics shared libs into the extension JAR
      Compile / packageBin / mappings ++= {
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isPhysicsLib(name: String) = sharedLibExts.exists(name.endsWith) && name.contains("sge_physics")
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isPhysicsLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isPhysicsLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        crossMappings ++ hostMappings
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/physics") ++
      _root_.multiarch.sbt.NativeProviderPlugin.projectSettings ++ Seq(
      resolvers += mavenCentralSnapshots,
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-physics" % versions.nativeComponents
    )
  )

val `sge-physics3d` = (projectMatrix in file("sge-extension/physics3d"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-physics3d",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/physics3d") ++ Seq(
      resolvers += mavenCentralSnapshots,
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-physics3d-desktop" % versions.nativeComponents,
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      },
      Test / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      },
      // Bundle sge_physics3d shared libs into the extension JAR
      Compile / packageBin / mappings ++= {
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isPhysics3dLib(name: String) = sharedLibExts.exists(name.endsWith) && name.contains("sge_physics3d")
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isPhysics3dLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isPhysics3dLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        crossMappings ++ hostMappings
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/physics3d") ++
      _root_.multiarch.sbt.NativeProviderPlugin.projectSettings ++ Seq(
      resolvers += mavenCentralSnapshots,
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-physics3d" % versions.nativeComponents
    )
  )

val `sge-ai` = (projectMatrix in file("sge-extension/ai"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-ai",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/ai")
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/ai")
  )

val `sge-ecs` = (projectMatrix in file("sge-extension/ecs"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-ecs",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/ecs")
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/ecs")
  )

val `sge-controllers` = (projectMatrix in file("sge-extension/controllers"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-controllers",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/controllers") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings ++ Seq(
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "sge-extension" / "controllers" / "src" / "main" / "scala-js"
    )
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/controllers") ++ Seq(
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "sge-extension" / "controllers" / "src" / "main" / "scalanative"
    )
  )

val `sge-gltf` = (projectMatrix in file("sge-extension/gltf"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.relaxedSettings *)
  .settings(publishSettings *)
  .settings(
    // Relax warnings for in-progress gltf port:
    //   - implicit conversions (Nullable A→Nullable[A] auto-wrapping)
    //   - implicit parameters clause style (Ordering.comparatorToOrdering)
    //   - non-local returns (TODO: convert to boundary/break)
    //   - unreachable case patterns (null handling)
    scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-Wconf:msg=Implicit parameters should be provided:s",
      "-Wconf:msg=Non local returns:s",
      "-Wconf:msg=Unreachable case:s"
    )
  )
  .settings(
    name := "sge-extension-gltf",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/gltf") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/gltf")
  )

val `sge-vfx` = (projectMatrix in file("sge-extension/vfx"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-vfx",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/vfx") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/vfx")
  )

val `sge-textra` = (projectMatrix in file("sge-extension/textra"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-textra",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/textra") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/textra")
  )

val `sge-colorful` = (projectMatrix in file("sge-extension/colorful"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-colorful",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/colorful") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/colorful")
  )

val `sge-visui` = (projectMatrix in file("sge-extension/visui"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-visui",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/visui") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/visui")
  )

val `sge-screens` = (projectMatrix in file("sge-extension/screens"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-screens",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/screens") ++ Seq(
      Compile / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api` / Compile / products).value
        val jdkDirs = (`sge-jvm-platform-jdk` / Compile / products).value
        (apiDirs ++ jdkDirs).map(Attributed.blank)
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/screens")
  )

val `sge-anim8` = (projectMatrix in file("sge-extension/anim8"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-anim8",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-extension/anim8")
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/anim8")
  )

val `sge-noise` = (projectMatrix in file("sge-extension/noise"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-noise",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.jvmSettings(projectDir = "sge-extension/noise"))
  .jsPlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.jsSettings)
  .nativePlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.nativeSettings(projectDir = "sge-extension/noise"))

val `sge-graphs` = (projectMatrix in file("sge-extension/graphs"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-graphs",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.jvmSettings(projectDir = "sge-extension/graphs"))
  .jsPlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.jsSettings)
  .nativePlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.nativeSettings(projectDir = "sge-extension/graphs"))

val `sge-jbump` = (projectMatrix in file("sge-extension/jbump"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(publishSettings *)
  .settings(
    name := "sge-extension-jbump",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.jvmSettings(projectDir = "sge-extension/jbump"))
  .jsPlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.jsSettings)
  .nativePlatform(scalaVersions = Seq(versions.scala), settings = SgePlugin.nativeSettings(projectDir = "sge-extension/jbump"))

// ── Android smoke test APK ────────────────────────────────────────────
//
// Minimal Android app that bootstraps SGE, renders 30 frames, and
// exits. Built into an APK via AndroidBuild pipeline (d8 → aapt2 →
// apksigner). Used by sge-it-android to catch runtime crashes.
//
// Build: sbt 'sge-android-smoke/androidSign'
// Prerequisites: Android SDK (auto-downloaded by the sbt androidSdkRoot task on first invocation)

lazy val `sge-android-smoke` = (project in file("sge-test/android-smoke"))
  .settings(SgePlugin.commonSettings *)
  .settings(_root_.multiarch.sbt.AndroidBuild.taskSettings *)
  .settings(
    _root_.multiarch.sbt.AndroidBuild.androidSdkCacheDir := (ThisBuild / baseDirectory).value / "sge-deps" / "android-sdk"
  )
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    name := "sge-test-android-smoke",
    // Conditional android sources
    Compile / unmanagedSourceDirectories ++= {
      if (hasAndroidSdk)
        Seq(baseDirectory.value / "src" / "main" / "scala-android")
      else Seq.empty
    },
    // Also add sge's JVM platform modules to classpath
    Compile / unmanagedClasspath ++= {
      val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
      val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
      val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
      (apiDirs ++ jdkDirs ++ androidDirs).map(Attributed.blank)
    }
  )
  .dependsOn(sge.jvm(versions.scala))

// ── Integration tests ─────────────────────────────────────────────────
//
// Separate non-published modules with isolated classpaths to verify:
//   - JVM platform provider discovery (PanamaProvider, JdkPanama)
//   - Android ops API interfaces (self-contained, JDK types only)
//   - Desktop end-to-end: GLFW + ANGLE + miniaudio + FileIO + JSON/XML
//   - Browser JS output in real headless Chromium (Playwright)
//   - Android APK on headless emulator with SwiftShader GL ES
//
// These modules depend directly on the platform modules (NOT sge) so
// they test the API/impl boundaries in isolation.

// Desktop integration tests — launches a real GLFW + ANGLE window with
// miniaudio audio engine and exercises all subsystems end-to-end:
// bootstrap, GL2D, GL3D, audio, file I/O, JSON/XML parsing.
//
// Prerequisites: native libs from sge-native-providers provider JARs (auto-resolved by sbt)
//
// Run: sbt --client 'sge-it-desktop/test'  or  re-scale runner desktop-it
lazy val `sge-it-desktop` = (project in file("sge-test/it-desktop"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"  % versions.munit % Test,
      "com.outr"      %% "scribe" % versions.scribe,
      // Panama provider JARs — multiarch-core extracts native libs from these at runtime
      "com.kubuszok" % "pnm-provider-sge-desktop"          % versions.nativeComponents % Test,
      "com.kubuszok" % "pnm-provider-sge-freetype-desktop" % versions.nativeComponents % Test,
      "com.kubuszok" % "pnm-provider-sge-physics-desktop"  % versions.nativeComponents % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    // Need JVM platform modules on classpath
    Compile / unmanagedClasspath ++= {
      val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
      val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
      val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
      (apiDirs ++ jdkDirs ++ androidDirs).map(Attributed.blank)
    },
    Test / unmanagedClasspath ++= {
      val apiDirs     = (`sge-jvm-platform-api` / Compile / products).value
      val jdkDirs     = (`sge-jvm-platform-jdk` / Compile / products).value
      val androidDirs = (`sge-jvm-platform-android` / Compile / products).value
      (apiDirs ++ jdkDirs ++ androidDirs).map(Attributed.blank)
    },
    Test / fork := true,
    Test / javaOptions ++= {
      // Native libs are built externally in sge-native-providers and distributed as provider JARs.
      // CI extracts provider JARs to sge-deps/native-components/target/release/ for linking/testing.
      // ANGLE (EGL, GLESv2) is also packaged into the same provider JARs and extracted there.
      val rustLib = ((ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release").getAbsolutePath
      // Note: -XstartOnFirstThread is NOT passed here — the munit test launches
      // the harness as a subprocess with that flag so GLFW runs on thread 0.
      Seq(
        s"-Djava.library.path=$rustLib",
        "--enable-native-access=ALL-UNNAMED"
      )
    }
  )
  .dependsOn(sge.jvm(versions.scala), `sge-freetype`.jvm(versions.scala), `sge-physics`.jvm(versions.scala))

lazy val `sge-it-jvm-platform` = (project in file("sge-test/it-jvm-platform"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    libraryDependencies += "org.scalameta" %% "munit" % versions.munit % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(`sge-jvm-platform-api`, `sge-jvm-platform-jdk`, `sge-jvm-platform-android`)

// Browser integration tests — JVM-based Playwright tests that exercise compiled
// Scala.js output in a real headless Chromium browser. Catches runtime JS errors
// (ReferenceError, TypeError, null/undefined, TypedArray conversions) that
// Node.js can't detect.
//
// Prerequisites: run `npx playwright@1.49.0 install chromium` once to install
// the browser binary. Playwright Java auto-manages the driver.
//
// Run: sbt --client 'sge-it-browser/test'  or  re-scale runner browser-it
lazy val `sge-it-browser` = (project in file("sge-test/it-browser"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    // Playwright builds its own JS test harness and loads it in a real browser.
    libraryDependencies ++= Seq(
      "org.scalameta"           %% "munit"      % versions.munit % Test,
      "com.microsoft.playwright" % "playwright"  % "1.49.0"      % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    // The test needs the fastLinkJS output from sge. We make it a resource generator
    // so it's available on the test classpath.
    Test / resourceGenerators += Def.task {
      val jsDir = (sge.js(versions.scala) / Compile / fullClasspath).value
      Seq.empty[File] // resources come from sgeJS classpath, not generated
    }.taskValue
  )

// Android integration tests — JVM-based tests that deploy the smoke APK
// to a headless Android emulator (AVD with SwiftShader) and monitor
// logcat for runtime crashes. Catches ClassNotFoundException, NPE,
// UnsatisfiedLinkError, GL errors, and any FATAL exception during
// app startup.
//
// Prerequisites:
//   1. Android SDK + emulator + system image: auto-installed by androidSdkRoot
//   2. Build smoke APK: sbt 'sge-test-android-smoke/androidSign'
//   3. Create + start AVD via android-emulator-runner GitHub Action (or local AVD manager)
//
// Run: sbt --client 'sge-it-android/test'  or  re-scale runner android-it
lazy val `sge-it-android` = (project in file("sge-test/it-android"))
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    libraryDependencies += "org.scalameta" %% "munit" % versions.munit % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )

// Native FFI wiring validation — Scala Native executable that exercises every
// native C ABI endpoint (sge_native_ops, sge_audio, GLFW, EGL, GLESv2) to verify
// correct symbol resolution, ABI compatibility, and pointer calculations.
// Catches runtime SIGSEGVs from wrong parameter types or buffer offset bugs.
//
// Run: sbt --client 'sge-it-native-ffi/run'  or  re-scale runner native-ffi-it
lazy val `sge-it-native-ffi` = (project in file("sge-test/it-native-ffi"))
  .enablePlugins(ScalaNativePlugin)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .dependsOn(sge.native(versions.scala))
  .settings(
    scalaVersion := versions.scala
  )
  .settings(_root_.multiarch.sbt.NativeProviderPlugin.projectSettings *)

// Write published version to demos/.sge-version so the demos sub-build
// resolves the same version without depending on sbt-git or env vars.
val writeDemoVersion = taskKey[Unit]("Write SGE version to .sge-version")
ThisBuild / writeDemoVersion := {
  val v = version.value
  val base = baseDirectory.value
  // Write to both root (for sge-build plugin version) and demos/ (for demo dependency resolution)
  IO.write(base / ".sge-version", v)
  IO.write(base / "demos" / ".sge-version", v)
  streams.value.log.info(s"[sge] Wrote .sge-version: $v")
}

// ── Root project — git-based versioning ──────────────────────────────
lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  // Core library
  .aggregate(sge.projectRefs *)
  .aggregate(regressionTest.projectRefs *)
  // Extensions
  .aggregate(`sge-tools`)
  .aggregate(`sge-freetype`.projectRefs *)
  .aggregate(`sge-physics`.projectRefs *)
  .aggregate(`sge-physics3d`.projectRefs *)
  .aggregate(`sge-ai`.projectRefs *)
  .aggregate(`sge-ecs`.projectRefs *)
  .aggregate(`sge-controllers`.projectRefs *)
  .aggregate(`sge-gltf`.projectRefs *)
  .aggregate(`sge-vfx`.projectRefs *)
  .aggregate(`sge-textra`.projectRefs *)
  .aggregate(`sge-colorful`.projectRefs *)
  .aggregate(`sge-visui`.projectRefs *)
  .aggregate(`sge-screens`.projectRefs *)
  .aggregate(`sge-anim8`.projectRefs *)
  .aggregate(`sge-noise`.projectRefs *)
  .aggregate(`sge-graphs`.projectRefs *)
  .aggregate(`sge-jbump`.projectRefs *)
  // Integration tests
  .aggregate(`sge-it-desktop`)
  .aggregate(`sge-it-jvm-platform`)
  .aggregate(`sge-it-browser`)
  .aggregate(`sge-it-android`)
  .aggregate(`sge-it-native-ffi`)

// ── Test aggregation aliases ─────────────────────────────────────────
// Run all unit tests for a given platform (core + all extensions).

addCommandAlias("test-jvm",
  List(
    // Core library
    "sge/test",
    // Extensions
    "sge-ai/test", "sge-ecs/test", "sge-controllers/test",
    "sge-gltf/test", "sge-vfx/test", "sge-textra/test",
    "sge-colorful/test", "sge-visui/test", "sge-screens/test",
    "sge-anim8/test", "sge-noise/test", "sge-graphs/test",
    "sge-jbump/test", "sge-physics/test", "sge-physics3d/test",
    "sge-freetype/test", "sge-tools/test",
    // Regression tests
    "regressionTest/test"
  ).mkString("; ")
)

addCommandAlias("test-js",
  List(
    // Core library
    "sgeJS/test",
    // Extensions
    "sge-aiJS/test", "sge-ecsJS/test", "sge-controllersJS/test",
    "sge-gltfJS/test", "sge-vfxJS/test", "sge-textraJS/test",
    "sge-colorfulJS/test", "sge-visuiJS/test", "sge-screensJS/test",
    "sge-anim8JS/test", "sge-noiseJS/test", "sge-graphsJS/test",
    "sge-jbumpJS/test", "sge-physicsJS/test", "sge-physics3dJS/test",
    "sge-freetypeJS/test",
    // Regression tests
    "regressionTestJS/test"
  ).mkString("; ")
)

addCommandAlias("test-native",
  List(
    // Core library
    "sgeNative/test",
    // Extensions
    "sge-aiNative/test", "sge-ecsNative/test", "sge-controllersNative/test",
    "sge-gltfNative/test", "sge-vfxNative/test", "sge-textraNative/test",
    "sge-colorfulNative/test", "sge-visuiNative/test", "sge-screensNative/test",
    "sge-anim8Native/test", "sge-noiseNative/test", "sge-graphsNative/test",
    "sge-jbumpNative/test", "sge-physicsNative/test", "sge-physics3dNative/test",
    "sge-freetypeNative/test",
    // Regression tests
    "regressionTestNative/test"
  ).mkString("; ")
)

// ── Coverage alias (JVM-only) ────────────────────────────────────────
// Scala 3 coverage instrumentation is incompatible with JS/Native runtimes
// (java.io.FileWriter references). Only run on JVM projects via test-jvm.
// Strips -Werror to avoid false-positive warnings from instrumented code.

addCommandAlias("test-coverage",
  List(
    "coverage",
    """set ThisBuild / scalacOptions -= "-Werror"""",
    "test-jvm",
    "coverageReport",
    "coverageAggregate",
    "coverageOff"
  ).mkString("; ")
)
