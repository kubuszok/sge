// Explicit root project enabling SbtPlugin so the `scripted` task + its keys
// (scriptedLaunchOpts / scriptedBufferLog) are in scope (ISS-562). SbtPlugin
// also sets `sbtPlugin := true` and brings in the ScriptedPlugin.
lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    // sbt 2.0 plugins are Scala 3, but some transitive plugin deps (e.g.
    // sbt-scalafmt → scalafmt-dynamic) still pull Scala-2.13 builds of
    // scala-xml / scala-collection-compat alongside the Scala-3 ones, tripping
    // the conflicting-cross-version-suffix guard. These are runtime-compatible
    // here (plugin classpath), so don't fail the build on the mixed suffixes.
    conflictWarning := conflictWarning.value.copy(failOnConflict = false),
    // conflictWarning above does NOT cover sbt 2.0's separate "conflicting
    // cross-version suffixes" guard (it hard-fails when both _3 and _2.13 builds
    // of scala-xml / scala-collection-compat appear). Declare the Always scheme so
    // the runtime-compatible mixed suffixes are allowed.
    libraryDependencySchemes ++= Seq(
      "org.scala-lang.modules" %% "scala-xml"               % VersionScheme.Always,
      "org.scala-lang.modules" %% "scala-collection-compat" % VersionScheme.Always
    ),
    name         := "sge-build",
    organization := "com.kubuszok",
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
    },
    // sbt 2.0 plugins are built for Scala 3 (the sbt-2.0 meta-build dialect);
    // no explicit scalaVersion needed (SbtPlugin defaults it).
    // Generate sge-build.properties with the plugin version baked in.
    // SgePlugin.sgeVersion reads this from the classpath at runtime.
    Compile / resourceGenerators += Def.task {
      val file = (Compile / resourceManaged).value / "sge-build.properties"
      IO.write(file, s"sge.version=${version.value}\n")
      Seq(file)
    }.taskValue,
    // Plugin dependencies — these are available to projects that enable SgePlugin.
    // sbt-projectmatrix is merged into sbt 2.0 (no longer added separately).
    addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.22.0"),
    addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.12"),
    addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.6"),
    // multiarch-scala — provides Platform, NativeProviderPlugin, ZigCross, JvmPackaging
    addSbtPlugin("com.kubuszok"     % "sbt-multiarch-scala" % "0.3.0"),
    // Sonatype snapshots for sbt-multi-arch-release
    resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
    // Test scope for unit-testing pure plugin data (e.g. the strict/lenient
    // scalacOptions partition). ISS-556.
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test,
    // ── sbt scripted tests (ISS-562) ─────────────────────────────────────
    // Each test under src/sbt-test/<group>/<name>/ gets a throwaway sbt project
    // that resolves THIS plugin via the locally-published version. `scripted`
    // runs publishLocal first; we forward the resulting version through the
    // `plugin.version` system property so test projects can wire
    // `addSbtPlugin("com.kubuszok" % "sge-build" % sys.props("plugin.version"))`.
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
