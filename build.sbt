import _root_.scalafix.sbt.{ BuildInfo => ScalafixBuildInfo }
import _root_.sge.sbt.{Platform, SgeNativeLibs, SgePlugin}

// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / packageDoc / publishArtifact := false
ThisBuild / scalafmtOnCompile := !isCI
ThisBuild / semanticdbEnabled := true

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

  val kindlings = "ff47bc548aae0b3c078591bd66777172375812b3-SNAPSHOT"
  val sttp      = "4.0.19"
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
}

// Scalafix custom rules — separate module so rules can lint `core`
lazy val `scalafix-rules` = (project in file("scalafix-rules"))
  .disablePlugins(ScalafixPlugin)
  .settings(
    scalaVersion := versions.scala,
    organization := "com.kubuszok",
    libraryDependencies +=
      ("ch.epfl.scala" %% "scalafix-core" % ScalafixBuildInfo.scalafixVersion).cross(CrossVersion.for3Use2_13)
  )

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
  .dependsOn(`scalafix-rules` % ScalafixConfig)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings() ++ SgeNativeLibs.validationSettings ++ Seq(
      libraryDependencies += "ch.epfl.lamp" %% "gears" % versions.gears,
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
      // Bundle native shared libraries for all platforms into the JAR
      Compile / packageBin / mappings ++= {
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isSharedLib(name: String) = sharedLibExts.exists(name.endsWith)
        // Cross-compiled: all 6 platforms from target/cross/<platform>/
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isSharedLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        // Fallback: host platform only from target/release/ (local dev)
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isSharedLib(f.getName))
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
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isSharedLib(f.getName))
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
    settings = SgePlugin.nativeSettings() ++ SgeNativeLibs.hostSettings ++ Seq(
      nativeConfig := {
        val c      = nativeConfig.value
        val libDir = SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
      }
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
    settings = SgePlugin.nativeSettings(projectDir = "sge-test/regression") ++ SgeNativeLibs.hostSettings ++ Seq(
      nativeConfig := {
        val c      = nativeConfig.value
        val libDir = SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
      }
    )
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
  .disablePlugins(ScalafixPlugin)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    // Target JDK 17 bytecode — no java.lang.foreign or android.* references
    scalacOptions ++= Seq("-release", "17")
  )

lazy val `sge-jvm-platform-jdk` = (project in file("sge-jvm-platform/jdk"))
  .disablePlugins(ScalafixPlugin)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    // No -release flag — needs java.lang.foreign (JDK 22+)
  )
  .dependsOn(`sge-jvm-platform-api`)

lazy val hasAndroidSdk: Boolean = _root_.sge.sbt.AndroidSdk
  .findSdkRoot(new File("."))
  .exists(r => _root_.sge.sbt.AndroidSdk.androidJar(r).exists())

lazy val `sge-jvm-platform-android` = (project in file("sge-jvm-platform/android"))
  .disablePlugins(ScalafixPlugin)
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
      val base = (ThisBuild / baseDirectory).value
      _root_.sge.sbt.AndroidSdk.findSdkRoot(base).toSeq.flatMap { sdkRoot =>
        val jar = _root_.sge.sbt.AndroidSdk.androidJar(sdkRoot)
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
  .disablePlugins(ScalafixPlugin)
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
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/freetype") ++ SgeNativeLibs.hostSettings ++ Seq(
      nativeConfig := {
        val c      = nativeConfig.value
        val libDir = SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
      }
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
    settings = SgePlugin.nativeSettings(projectDir = "sge-extension/physics") ++ SgeNativeLibs.hostSettings ++ Seq(
      nativeConfig := {
        val c      = nativeConfig.value
        val libDir = SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
      }
    )
  )

// ── Android smoke test APK ────────────────────────────────────────────
//
// Minimal Android app that bootstraps SGE, renders 30 frames, and
// exits. Built into an APK via AndroidBuild pipeline (d8 → aapt2 →
// apksigner). Used by sge-it-android to catch runtime crashes.
//
// Build: sbt 'sge-android-smoke/androidSign'
// Prerequisites: Android SDK (run 'sge-dev test android setup')

lazy val `sge-android-smoke` = (project in file("sge-test/android-smoke"))
  .disablePlugins(ScalafixPlugin)
  .settings(_root_.sge.sbt.AndroidBuild.settings *)
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
// Prerequisites: Rust native lib built (sge-dev native build)
//
// Run: sbt 'sge-test-it-desktop/test'  or  sge-dev test integration --desktop
lazy val `sge-it-desktop` = (project in file("sge-test/it-desktop"))
  .disablePlugins(ScalafixPlugin)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .settings(
    scalaVersion := versions.scala,
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"  % versions.munit % Test,
      "com.outr"      %% "scribe" % versions.scribe
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
      // All native libs (sge_native_ops, sge_audio, glfw) are built from vendored source
      // and placed in sge-deps/native-components/target/release/. ANGLE (EGL, GLESv2) needs to be
      // bundled there as well (see sge-dev native angle setup).
      val rustLib = ((ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release").getAbsolutePath
      // Note: -XstartOnFirstThread is NOT passed here — the munit test launches
      // the harness as a subprocess with that flag so GLFW runs on thread 0.
      Seq(
        s"-Djava.library.path=$rustLib",
        "--enable-native-access=ALL-UNNAMED"
      )
    }
  )
  .dependsOn(sge.jvm(versions.scala))

lazy val `sge-it-jvm-platform` = (project in file("sge-test/it-jvm-platform"))
  .disablePlugins(ScalafixPlugin)
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
// Run: sbt 'sge-test-it-browser/test'  or  sge-dev test browser
lazy val `sge-it-browser` = (project in file("sge-test/it-browser"))
  .disablePlugins(ScalafixPlugin)
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
//   1. Android SDK + emulator + system image: sge-dev test android setup
//   2. Build smoke APK: sbt 'sge-test-android-smoke/androidSign'
//   3. Create + start AVD: sge-dev test android start
//
// Run: sbt 'sge-test-it-android/test'  or  sge-dev test android test
lazy val `sge-it-android` = (project in file("sge-test/it-android"))
  .disablePlugins(ScalafixPlugin)
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
// Run: sbt 'sge-test-it-native-ffi/run'  or  sge-dev test integration --native-ffi
lazy val `sge-it-native-ffi` = (project in file("sge-test/it-native-ffi"))
  .enablePlugins(ScalaNativePlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(publishSettings *)
  .settings(noPublishSettings *)
  .dependsOn(sge.native(versions.scala))
  .settings(
    scalaVersion := versions.scala,
    nativeConfig := {
      val c      = nativeConfig.value
      val libDir = SgeNativeLibs.sgeNativeLibDir.value
      c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
    }
  )
  .settings(SgeNativeLibs.hostSettings *)

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
  // Integration tests
  .aggregate(`sge-it-desktop`)
  .aggregate(`sge-it-jvm-platform`)
  .aggregate(`sge-it-browser`)
  .aggregate(`sge-it-android`)
  .aggregate(`sge-it-native-ffi`)
