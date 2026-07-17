// ISS-752: single source of truth for the plugin version. Read from
// ../.sge-version (written by the root build's writeDemoVersion task so the
// plugin version tracks the SGE library exactly) or fall back to a git-SHA
// snapshot. Resolved ONCE at build load. This must drive `version` AND
// `isSnapshot` at BOTH project and ThisBuild scope: sbt-kubuszok's KubuszokPlugin
// brings GitVersioning, which sets its own project- AND build-scope git version.
// A ThisBuild-only override still let GitVersioning's project-scope version reach
// projectID/makePom (publishing a dated git version), while a project-only
// override left GitVersioning's build-scope version driving isSnapshot (wrong
// snapshots-vs-release routing + doc-JAR gating). Pinning all four settings from
// this one value makes the routing deterministic regardless of git-worktree tag
// detection quirks.
lazy val sgeReleaseVersion: String = {
  val versionFile = new File("../.sge-version")
  if (versionFile.exists())
    scala.io.Source.fromFile(versionFile).mkString.trim
  else {
    val sha = scala.sys.process.Process(Seq("git", "rev-parse", "HEAD"), new File("..")).!!.trim
    s"$sha-SNAPSHOT"
  }
}

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
    // ── Publishing (ISS-752) ─────────────────────────────────────────────
    // Mirror the ROOT build's publishSettings POM metadata EXACTLY (build.sbt
    // `publishSettings`) so Maven Central accepts the plugin artifact — Central
    // rejects POMs missing name/description/url/license/scm/developers. The
    // Sonatype target + PGP signing themselves come from sbt-kubuszok's
    // KubuszokPlugin (auto-triggered via project/plugins.sbt), which sets
    // `publishTo := if (isSnapshot) snapshots else localStaging` and the
    // `ci-release` command keyed off `projectType`.
    description  := "SGE sbt plugin (SgePlugin, packaging, AndroidBuild) — cross-platform build support for SGE game projects",
    homepage             := Some(url("https://github.com/kubuszok/sge")),
    organizationHomepage := Some(url("https://kubuszok.com")),
    licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/kubuszok/sge/"),
        "scm:git:git@github.com:kubuszok/sge.git"
      )
    ),
    startYear  := Some(2026),
    developers := List(
      Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://kubuszok.com"))
    ),
    pomExtra := (
      <issueManagement>
        <system>GitHub issues</system>
        <url>https://github.com/kubuszok/sge/issues</url>
      </issueManagement>
    ),
    // Opt this project INTO publishing — KubuszokPlugin defaults projectType to
    // NonPublished (publish/skip = true). ScalaLibrary matches the root's
    // published modules; for this plugin it only flips publish/skip = false and
    // publishArtifact = true (see KubuszokPlugin.projectSettings).
    projectType := kubuszok.sbt.KubuszokPlugin.autoImport.ProjectType.ScalaLibrary,
    // Maven Central requires a javadoc/scaladoc JAR on releases; snapshots skip
    // it to keep master-push publishes cheap. Mirrors the root build's
    // mimaSettings gating (build.sbt `packageDoc / publishArtifact`). Verified
    // `sbt doc` builds for this plugin.
    packageDoc / publishArtifact := !isSnapshot.value,
    // ISS-750: sge-build is a SEPARATE sbt build, so its scalafmt looks for a
    // config in this directory and finds none — leaving sge-build's own sources
    // unchecked (SgePlugins.scala shipped misformatted). Point at the repo-root
    // .scalafmt.conf so there is a single source of truth (no drift) and
    // `scalafmtCheckAll` here enforces the same rules as the main build.
    scalafmtConfig := Def.uncached(file("../.scalafmt.conf")),
    // Version matches the SGE library (see sgeReleaseVersion above). Pinned at
    // both project and ThisBuild scope so projectID/makePom/publish AND the
    // ThisBuild-scoped isSnapshot all agree (ISS-752).
    version            := sgeReleaseVersion,
    ThisBuild / version := sgeReleaseVersion,
    isSnapshot            := sgeReleaseVersion.endsWith("-SNAPSHOT"),
    ThisBuild / isSnapshot := sgeReleaseVersion.endsWith("-SNAPSHOT"),
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
