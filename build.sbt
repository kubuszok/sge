import _root_.scalafix.sbt.{ BuildInfo => ScalafixBuildInfo }

// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI
ThisBuild / semanticdbEnabled := true

val versions = new {
  val scala = "3.8.2"

  val kindlings = "361026ca2b848637931d65ffece9023592179ada-SNAPSHOT"
  val jsoniter  = "2.38.9"
  val sttp      = "4.0.19"
  val xml       = "2.3.0"
  val scalajsDom = "2.8.1"

  val munit           = "1.2.3"
  val munitScalacheck = "1.2.0"
}

val commonSettings = Seq(
  scalaVersion := versions.scala,
  organization := "com.kubuszok",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-no-indent",
    "-rewrite",
    "-Werror",
    // Linter flags (Tier 1 — safe)
    "-Wimplausible-patterns",
    "-Wrecurse-with-default",
    "-Wenum-comment-discard",
    "-Wunused:imports,privates,locals,patvars,nowarn",
    // E198 (unused symbols) is no longer suppressed — -Werror catches regressions
    "-Wconf:cat=deprecation:info" // deprecation (including orNull trick)
  )
)
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
  .settings(commonSettings *)
  .settings(
    name := "sge-core",
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
    settings = Seq(
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "core" / "src" / "main" / "scaladesktop",
      libraryDependencies ++= Seq(
        "org.jcraft" % "jorbis" % "0.0.17",
        "com.badlogicgames.jlayer" % "jlayer" % "1.0.1-gdx"
      ),
      Test / fork := true,
      Test / javaOptions ++= {
        val base = (ThisBuild / baseDirectory).value
        val rustLib = s"$base/native-components/target/release"
        // Include Homebrew lib paths for GLFW/ANGLE integration tests
        val brewLib = if (sys.props("os.name").toLowerCase.contains("mac")) {
          val arm = "/opt/homebrew/lib"
          val x86 = "/usr/local/lib"
          s"${java.io.File.pathSeparator}$arm${java.io.File.pathSeparator}$x86"
        } else ""
        Seq(
          s"-Djava.library.path=$rustLib$brewLib",
          "--enable-native-access=ALL-UNNAMED"
        )
      }
    )
  )
  .jsPlatform(
    scalaVersions = Seq(versions.scala),
    settings = Seq(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % versions.scalajsDom
    )
  )
  .nativePlatform(
    scalaVersions = Seq(versions.scala),
    settings = Seq(
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "core" / "src" / "main" / "scaladesktop",
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
