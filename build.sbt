import _root_.scalafix.sbt.{BuildInfo => ScalafixBuildInfo}

// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI
ThisBuild / semanticdbEnabled := true

val commonSettings = Seq(
  scalaVersion := "3.8.2",
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
    // Downgrade known-noisy categories to info until scalafix fixes them
    "-Wconf:id=E198:info",         // unused symbols (imports, privates, locals, patvars)
    "-Wconf:cat=deprecation:info"  // deprecation (including orNull trick)
  )
)

val kindlingsVersion = "361026ca2b848637931d65ffece9023592179ada-SNAPSHOT"
val jsoniterVersion = "2.38.9"
val munitVersion          = "1.0.4"
val munitScalacheckVersion = "1.2.0"

// Scalafix custom rules — separate module so rules can lint `core`
lazy val `scalafix-rules` = (project in file("scalafix-rules"))
  .disablePlugins(ScalafixPlugin)
  .settings(
    scalaVersion := "3.8.2",
    organization := "com.kubuszok",
    libraryDependencies +=
      ("ch.epfl.scala" %% "scalafix-core" % ScalafixBuildInfo.scalafixVersion)
        .cross(CrossVersion.for3Use2_13)
  )

val core = (project in file("core"))
  .settings(commonSettings*)
  .settings(
    name := "core",
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterVersion,
      "com.kubuszok" %% "kindlings-jsoniter-json" % kindlingsVersion,
      "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
      "org.scalameta" %% "munit"            % munitVersion          % Test,
      "org.scalameta" %% "munit-scalacheck" % munitScalacheckVersion % Test
    )
  )
  .dependsOn(`scalafix-rules` % ScalafixConfig)
