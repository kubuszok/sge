// git
addSbtPlugin("com.github.sbt"   % "sbt-git"            % "2.1.0")
// linters
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.4")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"       % "0.14.6")
// cross-compilation
addSbtPlugin("com.eed3si9n"     % "sbt-projectmatrix"  % "0.11.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.20.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.10")
// publishing
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
