// Reload sbt when build files change (avoids stale --client sessions).
Global / onChangedBuildSource := ReloadOnSourceChanges

// Format on compile during local development, skip on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI

val commonSettings = Seq(
  scalaVersion := "3.8.2",
  organization := "com.kubuszok",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-no-indent",
    "-rewrite",
    "-Werror"
    //"-Wconf:msg=.*Infinite loop in function body.*:error"
  )
)

val kindlingsVersion = "361026ca2b848637931d65ffece9023592179ada-SNAPSHOT"
val jsoniterVersion = "2.38.9"

val core = (project in file("core"))
  .settings(commonSettings*)
  .settings(
    name := "core",
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterVersion,
      "com.kubuszok" %% "kindlings-jsoniter-json" % kindlingsVersion,
      "org.scala-lang.modules" %% "scala-xml" % "2.3.0"
    )
  )
