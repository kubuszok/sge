sbtPlugin := true

name         := "sge-build"
organization := "com.kubuszok"

// Version matches the SGE library. Read from ../.sge-version (written by
// root build's writeDemoVersion task) or fall back to git SHA snapshot.
version := {
  val versionFile = new File("../.sge-version")
  if (versionFile.exists())
    scala.io.Source.fromFile(versionFile).mkString.trim
  else {
    val sha = scala.sys.process.Process(Seq("git", "rev-parse", "HEAD"), new File("..")).!!.trim
    s"$sha-SNAPSHOT"
  }
}

// Match the sbt version used by projects consuming this plugin
scalaVersion := "2.12.20"

// Generate sge-build.properties with the plugin version baked in.
// SgePlugin.sgeVersion reads this from the classpath at runtime.
Compile / resourceGenerators += Def.task {
  val file = (Compile / resourceManaged).value / "sge-build.properties"
  IO.write(file, s"sge.version=${version.value}\n")
  Seq(file)
}.taskValue

// Plugin dependencies — these are available to projects that enable SgePlugin
addSbtPlugin("com.eed3si9n"     % "sbt-projectmatrix"  % "0.11.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.21.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.10")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.4")

// multiarch-scala — provides Platform, NativeProviderPlugin, ZigCross, JvmPackaging
addSbtPlugin("com.kubuszok"     % "sbt-multiarch-scala" % "0.1.1")

// Sonatype snapshots for sbt-multi-arch-release
resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
