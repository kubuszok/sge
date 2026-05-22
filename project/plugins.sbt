// kubuszok plugin (bundles: sbt-git, sbt-scalafmt, sbt-scoverage, sbt-projectmatrix, sbt-scalajs, sbt-scala-native, sbt-pgp, and more)
addSbtPlugin("com.kubuszok" % "sbt-kubuszok" % "0.2.0")
// multiarch-scala (Platform, NativeProviderPlugin, ZigCross, JvmPackaging)
addSbtPlugin("com.kubuszok" % "sbt-multiarch-scala" % "0.2.0")
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
