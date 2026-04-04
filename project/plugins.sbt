// git
addSbtPlugin("com.github.sbt"   % "sbt-git"            % "2.1.0")
// linters
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.4")
// cross-compilation
addSbtPlugin("com.eed3si9n"     % "sbt-projectmatrix"  % "0.11.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.20.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.10")
// publishing
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
// multi-arch release (Platform, NativeLibBundle, ZigCross, JvmPackaging)
addSbtPlugin("com.kubuszok" % "sbt-multi-arch-release" % "6bbb192b266adc226810c90f820297660c8e89b0-SNAPSHOT")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
// Sonatype snapshots for sbt-multi-arch-release
ThisBuild / resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
