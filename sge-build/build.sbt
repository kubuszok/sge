sbtPlugin := true

name         := "sge-build"
organization := "com.kubuszok"
version      := "0.1.0-SNAPSHOT"

// Match the sbt version used by projects consuming this plugin
scalaVersion := "2.12.20"

// Plugin dependencies — these are available to projects that enable SgePlugin
addSbtPlugin("com.eed3si9n"     % "sbt-projectmatrix"  % "0.11.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.20.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.10")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.4")
