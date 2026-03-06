import _root_.scalafix.sbt.{ BuildInfo => ScalafixBuildInfo }
import sge.sbt.SgePlugin

// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI
ThisBuild / semanticdbEnabled := true

val versions = new {
  val scala = SgePlugin.scalaVersion

  val kindlings = "361026ca2b848637931d65ffece9023592179ada-SNAPSHOT"
  val jsoniter  = "2.38.9"
  val sttp      = "4.0.19"
  val xml       = "2.3.0"
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

val core = (projectMatrix in file("core"))
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
      )
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
    settings = SgePlugin.nativeSettings() ++ Seq(
      nativeConfig := {
        val base        = (ThisBuild / baseDirectory).value
        val c           = nativeConfig.value
        val isWindows   = System.getProperty("os.name", "").toLowerCase.contains("win")
        val rustLibOpts = Seq(
          s"-L$base/native-components/target/release",
          "-lsge_native_ops"
        )
        // Rust std on Windows uses NtWriteFile/RtlNtStatusToDosError from ntdll.
        // sttp's curl backend declares @link("idn2") but Windows curl uses WinAPI for IDN,
        // not libidn2. We create an empty stub lib in CI to satisfy the linker.
        val windowsOpts = if (isWindows) Seq("-lntdll") else Seq.empty
        c.withLinkingOptions(c.linkingOptions ++ rustLibOpts ++ windowsOpts)
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
  .dependsOn(core)
  .jvmPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jvmSettings()
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.jsSettings ++ Seq(
      scalaJSUseMainModuleInitializer := true
    )
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = SgePlugin.nativeSettings() ++ Seq(
      nativeConfig := {
        val base      = (ThisBuild / baseDirectory).value
        val c         = nativeConfig.value
        val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
        val rustLibOpts = Seq(
          s"-L$base/native-components/target/release",
          "-lsge_native_ops"
        )
        val windowsOpts = if (isWindows) Seq("-lntdll") else Seq.empty
        c.withLinkingOptions(c.linkingOptions ++ rustLibOpts ++ windowsOpts)
      }
    )
  )
