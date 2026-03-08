import _root_.scalafix.sbt.{ BuildInfo => ScalafixBuildInfo }
import _root_.sge.sbt.{SgeNativeLibs, SgePlugin}

// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI
ThisBuild / semanticdbEnabled := true
ThisBuild / version           := "0.1.0-SNAPSHOT"

/** Collect all files from a class directory as (File, relative-path) pairs for JAR mappings. */
def collectClassFiles(classDir: File): Seq[(File, String)] =
  Path.allSubpaths(classDir).toSeq

val versions = new {
  val scala = SgePlugin.scalaVersion

  val kindlings = "361026ca2b848637931d65ffece9023592179ada-SNAPSHOT"
  val jsoniter  = "2.38.9"
  val sttp      = "4.0.19"
  val xml       = "2.3.0"
  val scribe    = "3.17.0"
  val scalajsDom = "2.8.1"

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
  .settings(
    name := "sge-core",
    organization := "com.kubuszok",
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    libraryDependencies ++= Seq(
      "com.kubuszok" %%% "kindlings-jsoniter-json" % versions.kindlings,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % versions.jsoniter % "provided",
      "com.softwaremill.sttp.client4" %%% "core" % versions.sttp,
      "org.scala-lang.modules" %%% "scala-xml" % versions.xml,
      "com.outr" %%% "scribe" % versions.scribe,
      "org.scalameta" %%% "munit" % versions.munit % Test,
      "org.scalameta" %%% "munit-scalacheck" % versions.munitScalacheck % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(`scalafix-rules` % ScalafixConfig)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings() ++ Seq(
      libraryDependencies ++= Seq(
        "org.jcraft" % "jorbis" % "0.0.17",
        "com.badlogicgames.jlayer" % "jlayer" % "1.0.1-gdx"
      ),
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

val demo = (projectMatrix in file("demo"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.relaxedSettings *)
  .settings(
    name := "sge-demo",
    organization := "com.kubuszok",
    publish / skip := true
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "demo")
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings ++ Seq(
      scalaJSUseMainModuleInitializer := true
    )
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings(projectDir = "demo") ++ SgeNativeLibs.hostSettings ++ Seq(
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

lazy val `sge-jvm-platform-api` = (project in file("sge-jvm-platform-api"))
  .disablePlugins(ScalafixPlugin)
  .settings(
    scalaVersion := versions.scala,
    organization := "com.kubuszok",
    publish / skip := true,
    // Target JDK 17 bytecode — no java.lang.foreign or android.* references
    scalacOptions ++= Seq("-release", "17")
  )

lazy val `sge-jvm-platform-jdk` = (project in file("sge-jvm-platform-jdk"))
  .disablePlugins(ScalafixPlugin)
  .dependsOn(`sge-jvm-platform-api`)
  .settings(
    scalaVersion := versions.scala,
    organization := "com.kubuszok",
    publish / skip := true
    // No -release flag — needs java.lang.foreign (JDK 22+)
  )

lazy val hasAndroidSdk: Boolean = _root_.sge.sbt.AndroidSdk
  .findSdkRoot(new File("."))
  .exists(r => _root_.sge.sbt.AndroidSdk.androidJar(r).exists())

lazy val `sge-jvm-platform-android` = (project in file("sge-jvm-platform-android"))
  .disablePlugins(ScalafixPlugin)
  .dependsOn(`sge-jvm-platform-api`)
  .settings(
    scalaVersion := versions.scala,
    organization := "com.kubuszok",
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
    libraryDependencies ++= Seq(
      // PanamaPort — Panama FFM backport for Android (API 26+)
      // TODO: add when published to Maven Central
      // "com.v7878" % "panama-port-core" % versions.panamaPort
    )
  )

// ── Extension modules ─────────────────────────────────────────────────
//
// sge-tools    — JVM-only build tools (TexturePacker)
// sge-freetype — Cross-platform FreeType font rasterization
// sge-physics  — Cross-platform 2D physics via Rapier2D
//
// These are published separately from sge-core. They depend on sge.

lazy val `sge-tools` = (project in file("sge-tools"))
  .disablePlugins(ScalafixPlugin)
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.relaxedSettings *)
  .settings(
    name := "sge-tools",
    organization := "com.kubuszok",
    scalaVersion := versions.scala,
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    Compile / mainClass := Some("sge.tools.texturepacker.TexturePacker"),
    libraryDependencies ++= Seq(
      "com.kubuszok" %% "kindlings-jsoniter-json" % versions.kindlings,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % versions.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % versions.jsoniter % "provided",
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

val `sge-freetype` = (projectMatrix in file("sge-freetype"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(
    name := "sge-freetype",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-freetype") ++ Seq(
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
    settings = SgePlugin.nativeSettings(projectDir = "sge-freetype") ++ SgeNativeLibs.hostSettings ++ Seq(
      nativeConfig := {
        val c      = nativeConfig.value
        val libDir = SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
      }
    )
  )

val `sge-physics` = (projectMatrix in file("sge-physics"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala))
  .settings(SgePlugin.commonSettings *)
  .settings(
    name := "sge-physics",
    organization := "com.kubuszok",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % versions.munit % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(sge)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings(projectDir = "sge-physics") ++ Seq(
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
    settings = SgePlugin.nativeSettings(projectDir = "sge-physics") ++ SgeNativeLibs.hostSettings ++ Seq(
      nativeConfig := {
        val c      = nativeConfig.value
        val libDir = SgeNativeLibs.sgeNativeLibDir.value
        c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
      }
    )
  )

// ── Integration tests ─────────────────────────────────────────────────
//
// Separate non-published modules with isolated classpaths to verify:
//   - JVM platform provider discovery (PanamaProvider, JdkPanama)
//   - Android ops API interfaces (self-contained, JDK types only)
//   - Runtime feature detection in sge core (future)
//
// These modules depend directly on the platform modules (NOT sge) so
// they test the API/impl boundaries in isolation.

lazy val `sge-it-jvm-platform` = (project in file("sge-it-tests/jvm-platform"))
  .disablePlugins(ScalafixPlugin)
  .dependsOn(`sge-jvm-platform-api`, `sge-jvm-platform-jdk`, `sge-jvm-platform-android`)
  .settings(
    scalaVersion := versions.scala,
    organization := "com.kubuszok",
    publish / skip := true,
    libraryDependencies += "org.scalameta" %% "munit" % versions.munit % Test,
    testFrameworks += new TestFramework("munit.Framework")
  )
