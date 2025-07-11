val commonSettings = Seq(
  scalaVersion := "3.7.1",
  organization := "com.kubuszok",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-no-indent",
    "-rewrite",
    "-Xfatal-warnings"
    //"-Wconf:msg=.*Infinite loop in function body.*:error"
  )
)

val core = (project in file("core"))
  .settings(commonSettings*)
  .settings(
    name := "core"
  )
