// Explicit root project enabling SbtPlugin so the `scripted` task + its keys
// (scriptedLaunchOpts / scriptedBufferLog) are in scope (ISS-562). SbtPlugin
// also sets `sbtPlugin := true` and brings in the ScriptedPlugin.
lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    // sbt 2.0 plugins are Scala 3, and sbt's own metabuild classpath supplies
    // the Scala-3 builds of scala-xml / scala-collection-compat (via
    // org.scala-sbt:librarymanagement-core_3 / main_3). But sbt-scalafmt drags
    // scalafmt-dynamic — a Scala-2.13 artifact — which transitively pulls
    // coursier_2.13 → scala-xml_2.13 / scala-collection-compat_2.13. The two
    // suffixes (_3 from sbt core, _2.13 from the scalafmt chain) coexist on the
    // plugin classpath and trip sbt 2.0's "conflicting cross-version suffixes"
    // guard (ISS-738). That guard is NOT silenced by conflictWarning or by a
    // VersionScheme.Always scheme (both were tried and the error still printed).
    //
    // The _3 and _2.13 builds of these two modules are runtime-compatible here,
    // and only sbt core genuinely needs its copy, so drop the intruding _2.13
    // variants and let coursier_2.13 use the sbt-core-supplied _3 build. This
    // leaves a single consistent suffix (_3), removing the conflict at its root.
    conflictWarning := conflictWarning.value.copy(failOnConflict = false),
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-xml_2.13",
      "org.scala-lang.modules" % "scala-collection-compat_2.13"
    ),
    name         := "sge-build",
    organization := "com.kubuszok",
    // ISS-750: sge-build is a SEPARATE sbt build, so its scalafmt looks for a
    // config in this directory and finds none — leaving sge-build's own sources
    // unchecked (SgePlugins.scala shipped misformatted). Point at the repo-root
    // .scalafmt.conf so there is a single source of truth (no drift) and
    // `scalafmtCheckAll` here enforces the same rules as the main build.
    scalafmtConfig := Def.uncached(file("../.scalafmt.conf")),
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
    // ISS-679: the vendored rapier2d-compat regeneration workspace lives under
    // src/main/resources/rapier2d-compat/. Its `npm install` writes a local
    // node_modules/ (gitignored, never committed) — keep it OUT of the plugin
    // JAR so a dev machine that regenerated the bundle does not ship ~30MB of
    // node_modules (incl. the esbuild binary). Only the committed inputs
    // (wrapper.mjs, package.json, README.md) and the built rapier2d-compat.umd.js
    // are packaged. Filter the final JAR mappings by in-jar path so the whole
    // node_modules subtree is dropped regardless of how it was collected.
    Compile / packageBin / mappings := (Compile / packageBin / mappings).value.filterNot { case (_, path) =>
      path.split('/').contains("node_modules")
    },
    // Plugin dependencies — these are available to projects that enable SgePlugin.
    // sbt-projectmatrix is merged into sbt 2.0 (no longer added separately).
    addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.22.0"),
    addSbtPlugin("org.scala-native" % "sbt-scala-native"   % "0.5.12"),
    addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.5.6"),
    // multiarch-scala — provides Platform, NativeProviderPlugin, ZigCross, JvmPackaging
    addSbtPlugin("com.kubuszok"     % "sbt-multiarch-scala" % "0.4.0"),
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
