// git
addSbtPlugin("com.github.sbt"   % "sbt-git"            % "2.1.0")
// linters
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.4")
// cross-compilation
addSbtPlugin("com.eed3si9n"     % "sbt-projectmatrix"  % "0.11.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.21.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.10")
// publishing
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
// multiarch-scala (Platform, NativeProviderPlugin, ZigCross, JvmPackaging)
addSbtPlugin("com.kubuszok" % "sbt-multiarch-scala" % "0.1.1")
// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
// Sonatype snapshots for sbt-multiarch-scala
ThisBuild / resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
