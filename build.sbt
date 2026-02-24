val commonSettings = Seq(
  scalaVersion := "3.8.1",
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

val core = (project in file("core"))
  .settings(commonSettings*)
  .settings(
    name := "core"
  )
