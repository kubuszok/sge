// sbt-projectmatrix, sbt-scalajs, sbt-scala-native come transitively from sge-build.
// No additional plugins needed.

// Sonatype snapshots for sbt-multi-arch-release (transitive dep of sge-build)
resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
