import _root_.multiarch.sbt.Platform
import _root_.multiarch.sbt.MultiArchResourcesPlugin
import _root_.sge.sbt.{SgeNativeLibs, SgePlugin}
import commandmatrix.extra.*
import kubuszok.sbt._
import kubuszok.sbt.KubuszokPlugin.autoImport._
import xsbti.{FileConverter, HashedVirtualFileRef}

// sbt 2.0: classpath entries are `Attributed[HashedVirtualFileRef]`, while
// `Compile / products` still yields `Seq[File]`. Route product dirs through the
// build's FileConverter to build classpath entries.
def blankCp(files: Seq[File], conv: FileConverter): Seq[Attributed[HashedVirtualFileRef]] =
  files.map(f => Attributed.blank(conv.toVirtualFile(f.toPath)))

// sbt 2.0: `packageBin / mappings` element type is now
// `(HashedVirtualFileRef, String)` rather than `(File, String)`. Route the
// source files through the build's FileConverter.
def blankMappings(mappings: Seq[(File, String)], conv: FileConverter): Seq[(HashedVirtualFileRef, String)] =
  mappings.map { case (f, path) => conv.toVirtualFile(f.toPath) -> path }

// Versions live in project/Versions.scala (flattened from the former anonymous
// `val versions = new {…}` refinement, which does not survive the sbt-2.0
// Scala-3 build dialect).

val dev = new DevProperties(
  scala213 = None,
  scala3 = Some(Versions.scala3),
  platforms = Versions.platforms
)

lazy val al = new Aliases(
  published = Seq(
    sge,
    `sge-ai`,
    `sge-anim8`,
    `sge-colorful`,
    `sge-controllers`,
    `sge-ecs`,
    `sge-freetype`,
    `sge-gltf`,
    `sge-graphs`,
    `sge-jbump`,
    `sge-noise`,
    `sge-physics`,
    `sge-physics3d`,
    `sge-screens`,
    `sge-textra`,
    `sge-tools`,
    `sge-vfx`,
    `sge-visui`
  ),
  testOnly = Seq(
    regressionTest,
    `sge-android-robolectric`
  ),
  compileOnly = Seq(
    `sge-jvm-platform-api`,
    `sge-jvm-platform-android`,
    `sge-android-smoke`,
    `sge-it-desktop`,
    `sge-it-jvm-platform`,
    `sge-it-browser`,
    `sge-it-android`,
    `sge-it-native-ffi`
  )
)

// The platform × Scala-binary combinations this build cross-publishes. The
// former `al.usefulTasks(...)` helper registered ci-*/test-*/publishLocal-*
// command aliases per combination as a side effect; on the sbt-2.0 axis that
// helper is gone, so we register the same aliases explicitly via
// addCommandAlias. Scala 3.8.x → binary "3"; platforms JVM/JS/Native.
lazy val sgeAliasCombinations: Seq[(String, String)] =
  Versions.platforms.collect {
    case VirtualAxis.jvm    => "JVM"
    case VirtualAxis.js     => "JS"
    case VirtualAxis.native => "Native"
  }.map(p => (p, "3"))

// Mirrors the (protected) Aliases.aliasName: e.g. ("ci", "JVM", "3") -> "ci-jvm-3".
def sgeAliasName(prefix: String, platform: String, scalaBin: String): String =
  s"$prefix-${platform.toLowerCase}-${scalaBin.replace('.', '_')}"

// sbt 2.0 changed `test` to an INCREMENTAL/cached task — on a fresh CI checkout
// `<id>/test` runs 0 suites (silent false-green). The Aliases helper still emits
// `<id>/test`, so rewrite the task to `<id>/testFull` (the non-incremental form)
// in the ci-*/test-* aliases. Only the `/test` task suffix is rewritten; `/compile`,
// `coverage`, `clean` etc. are untouched (no project id ends in a bare `/test`).
def testFullify(cmd: String): String = cmd.replace("/test", "/testFull")

// scoverage produces NO usable aggregate report on Scala 3.8.x under sbt 2.0 AND
// its per-test measurement files race under parallel execution, intermittently
// failing the JVM run with "scoverage.measurements.<uuid> (No such file)". Since
// coverage yields no report anyway (the codecov upload is already non-fatal),
// strip the `coverage`/`coverageAggregate`/`coverageOff` steps from the ci alias
// so the JVM test run is deterministic. (JS/Native ci aliases carry no coverage,
// so this is a no-op there.) Restore when scoverage supports 3.8.x.
def dropCoverage(cmd: String): String =
  cmd.replace("coverage ; ", "").replace(" ; coverageAggregate ; coverageOff", "")

lazy val sgeCommandAliases: Seq[Def.Setting[State => State]] =
  sgeAliasCombinations.flatMap { case (platform, scalaBin) =>
    addCommandAlias(sgeAliasName("ci", platform, scalaBin), dropCoverage(testFullify(al.ci(platform, scalaBin)))) ++
      addCommandAlias(sgeAliasName("test", platform, scalaBin), testFullify(al.test(platform, scalaBin))) ++
      addCommandAlias(sgeAliasName("publishLocal", platform, scalaBin), al.publishLocal(platform, scalaBin).mkString(" ; "))
  }

def commonSettings(
  projectDir: String = "sge",
  rustLibPath: Option[String] = None
) = Seq(
  MatrixAction.ForAll.Configure(
    _.settings(SgePlugin.commonSettings *).settings(SgePlugin.strictSettings *).settings(
      libraryDependencies ++= Seq(
        "org.scalameta"     %% "munit"             % Versions.munit % Test,
        "org.scalameta"     %% "munit-scalacheck"  % Versions.munitScalacheck % Test
      ),
      resolvers += Resolver.mavenLocal,
      testFrameworks += new TestFramework("munit.Framework")
    )
  ),
  MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
    Compile / unmanagedSourceDirectories ++= {
      val desktopDir = (ThisBuild / baseDirectory).value / projectDir / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    },
    fork := true,
    javaOptions ++= {
      val rustLib = rustLibPath.getOrElse {
        ((ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release").getAbsolutePath
      }
      val macFlags = if (sys.props("os.name").toLowerCase.contains("mac"))
        Seq("-XstartOnFirstThread") else Seq.empty
      Seq(s"-Djava.library.path=$rustLib", "--enable-native-access=ALL-UNNAMED") ++ macFlags
    },
    Test / fork := true,
    Test / javaOptions ++= (javaOptions).value
  )),
  MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
    Compile / unmanagedSourceDirectories ++= {
      val desktopDir = (ThisBuild / baseDirectory).value / projectDir / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    },
    // scalacheck 1.19 pulls scala-native test-interface 0.5.8 while scala-native
    // 0.5.12 brings 0.5.12 (strict eviction). They are compatible in practice;
    // downgrade the eviction error to a warning on the native test classpath.
    evictionErrorLevel := Level.Warn
  ))
)

val publishSettings = Seq(
  organization := "com.kubuszok",
  homepage := Some(url("https://github.com/kubuszok/sge")),
  organizationHomepage := Some(url("https://kubuszok.com")),
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/kubuszok/sge/"),
      "scm:git:git@github.com:kubuszok/sge.git"
    )
  ),
  startYear := Some(2026),
  developers := List(
    Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://kubuszok.com"))
  ),
  pomExtra := (
    <issueManagement>
      <system>GitHub issues</system>
      <url>https://github.com/kubuszok/sge/issues</url>
    </issueManagement>
  ),
  projectType := ProjectType.ScalaLibrary,
)

val noPublishSettings =
  Seq(projectType := ProjectType.NonPublished)

val mimaSettings = Seq(
  mimaPreviousArtifacts := Set(),
  mimaFailOnNoPrevious := false,
  packageDoc / publishArtifact := false
)

/** Collect all files from a class directory as (File, relative-path) pairs for JAR mappings. */
def collectClassFiles(classDir: File): Seq[(File, String)] =
  Path.allSubpaths(classDir).toSeq

val nativeProviderSettings = MatrixAction.ForPlatforms(VirtualAxis.native).Configure(
  _.settings(_root_.multiarch.sbt.NativeProviderPlugin.projectSettings)
)

val jvmPlatformApiClasspath = MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
  Compile / unmanagedClasspath ++= Def.uncached {
    val conv    = fileConverter.value
    val apiDirs = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
    blankCp(apiDirs, conv)
  }
))

// Core library

// Explicit type annotation: `sge` and `sge-jvm-platform-android` form a
// mutually-recursive pair of build vals — sge's Test/packageBin pull the
// android module's products, and the android module's Compile classpath pulls
// sge-core's products (for the scala-android SgeActivity shell). The sbt TASK
// graph stays acyclic (android.Compile -> sge.Compile -> api.Compile; sge.Test
// / packageBin -> android.Compile), but Scala still requires a type annotation
// to type a recursively-referenced val.
val sge: sbt.ProjectMatrix = (projectMatrix in file("sge"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings() ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      (SgeNativeLibs.validationSettings ++ Seq(
        libraryDependencies += "ch.epfl.lamp" %% "gears" % Versions.gears,
        libraryDependencies += "com.kubuszok" %% "multiarch-core" % Versions.multiarch,
        libraryDependencies += "com.kubuszok" %% "multiarch-panama-jdk" % Versions.multiarch,
        libraryDependencies += "com.kubuszok" %  "pnm-provider-sge-desktop" % Versions.nativeComponents,
        // `sge.SgeActivity` (the canonical Android host Activity shell) lives in
        // sge-core — in the SDK-gated `scala-android-host` source dir — so the
        // android module need not depend on sge (breaking the former sge<->android
        // cycle). It references the android framework (`android.app.Activity`,
        // `GLSurfaceView`, …), so the real SDK `android.jar` is put on this JVM
        // axis' COMPILE classpath (mirroring the android module). The dir is
        // added only when the SDK is present, so SDK-less builds simply skip
        // SgeActivity — exactly as the android module skips its impls. The
        // android backend it uses is the abstract `platformProvider`, supplied by
        // the game, so sge-core references NO `*Impl` class at compile time.
        Compile / unmanagedJars ++= Def.uncached {
          val conv     = fileConverter.value
          val cacheDir = (ThisBuild / baseDirectory).value / "sge-deps" / "android-sdk"
          _root_.multiarch.sbt.AndroidSdk.findSdkRoot(cacheDir).toSeq.flatMap { sdkRoot =>
            val jar = _root_.multiarch.sbt.AndroidSdk.androidJar(sdkRoot)
            if (jar.exists()) blankCp(Seq(jar), conv) else Seq.empty
          }
        },
        // COMPILE-ONLY: keep the SDK android.jar OFF the forked test classpath.
        // It is a STUB jar (android.util.Log throws "Stub!") and, more
        // importantly, multiarch's NativeLibLoader detects the host as Android
        // purely by `Class.forName("android.app.Activity")` — so android.jar on
        // the test runtime makes every sge JVM test resolve the wrong
        // native-lib path (android-aarch64) and route logging through the stub
        // android.util.Log. SgeActivity compiles against it via the Compile
        // classpath; the Test fork must not see it.
        Test / fullClasspath := Def.uncached {
          val conv = fileConverter.value
          (Test / fullClasspath).value.filterNot(e => conv.toPath(e.data).getFileName.toString == "android.jar")
        },
        Compile / unmanagedSourceDirectories ++= {
          if (hasAndroidSdk)
            Seq((ThisBuild / baseDirectory).value / "sge" / "src" / "main" / "scala-android-host")
          else Seq.empty
        },
        // sge-core compiles against the api ops interfaces only. The android
        // module's COMPILE products are intentionally NOT on sge-core's compile
        // classpath: sge-core references none of the android *Impl classes at
        // compile time. The android products are still merged into the sge JAR
        // (packageBin mappings below) and are on the test classpath at runtime —
        // a one-directional sge → android edge (no cycle, since android no longer
        // dependsOn sge).
        Compile / unmanagedClasspath ++= Def.uncached {
          val conv    = fileConverter.value
          val apiDirs = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
          blankCp(apiDirs, conv)
        },
        Test / unmanagedClasspath ++= Def.uncached {
          val conv        = fileConverter.value
          val apiDirs     = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
          val androidDirs = (`sge-jvm-platform-android`.jvm(Versions.scala3) / Compile / products).value
          blankCp(apiDirs ++ androidDirs, conv)
        },
        Compile / packageBin / mappings ++= Def.uncached {
          val conv        = fileConverter.value
          val apiDirs     = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
          val androidDirs = (`sge-jvm-platform-android`.jvm(Versions.scala3) / Compile / products).value
          blankMappings((apiDirs ++ androidDirs).flatMap(collectClassFiles), conv)
        },
        Compile / packageBin / mappings ++= Def.uncached {
          val conv       = fileConverter.value
          val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
          val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
          val sharedLibExts = Set(".so", ".dylib", ".dll")
          def isSharedLib(name: String) = sharedLibExts.exists(name.endsWith)
          def isCoreLib(name: String) = isSharedLib(name) && !name.contains("sge_freetype") && !name.contains("sge_physics")
          val crossMappings = Platform.desktop.flatMap { platform =>
            val dir = crossDir / platform.classifier
            if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isCoreLib(f.getName))
              .map(f => f -> s"native/${platform.classifier}/${f.getName}")
              .toSeq
            else Seq.empty
          }
          val hostMappings =
            if (crossMappings.nonEmpty) Seq.empty
            else if (releaseDir.exists()) {
              val host = Platform.host
              IO.listFiles(releaseDir).filter(f => f.isFile && isCoreLib(f.getName))
                .map(f => f -> s"native/${host.classifier}/${f.getName}")
                .toSeq
            } else Seq.empty
          val androidMappings = Seq(
            ("aarch64-linux-android",    "android-aarch64"),
            ("armv7-linux-androideabi",  "android-armv7"),
            ("x86_64-linux-android",     "android-x86_64")
          ).flatMap { case (rustTarget, classifier) =>
            val dir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / rustTarget / "release"
            if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isCoreLib(f.getName))
              .map(f => f -> s"native/$classifier/${f.getName}")
              .toSeq
            else Seq.empty
          }
          blankMappings(crossMappings ++ hostMappings ++ androidMappings, conv)
        }
      )) *
    )),
    MatrixAction.ForPlatforms(VirtualAxis.js).Configure(_.settings(
      libraryDependencies += "org.scala-js" %% "scalajs-dom" % Versions.scalajsDom,
      // Run sge JS unit tests under jsdom so browser components (BrowserGraphics, etc.)
      // that touch document/window can be tested (the default Node env has no DOM). ISS-672.
      Test / jsEnv := Def.uncached(new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv())
    )),
    MatrixAction.ForPlatforms(VirtualAxis.js).Configure(_.settings(
      // Embed the module's resources into a self-registering generated object so
      // `multiarch.resources.PlatformResources` can serve them synchronously on
      // Scala.js (no classpath, no HTTP/manifest preload). Referenced once from
      // BrowserApplication to defeat Scala.js DCE.
      MultiArchResourcesPlugin.embeddedResourcesSettings(
        objectName = "sge.platform.GeneratedEmbeddedResources"
      )
    )),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies ++= Seq(
        "com.kubuszok" % "sn-provider-sge"  % Versions.nativeComponents,
        "com.kubuszok" % "sn-provider-curl" % Versions.curlProvider
      )
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge",
    libraryDependencies ++= Seq(
      "com.kubuszok" %% "lls" % Versions.lls,
      "com.kubuszok" %% "multiarch-resources" % Versions.multiarch,
      "com.kubuszok" %% "kindlings-fast-show-pretty" % Versions.kindlings,
      "com.kubuszok" %% "kindlings-jsoniter-derivation" % Versions.kindlings,
      "com.kubuszok" %% "kindlings-jsoniter-json" % Versions.kindlings,
      "com.kubuszok" %% "kindlings-ubjson-derivation" % Versions.kindlings,
      "com.softwaremill.sttp.client4" %% "core" % Versions.sttp,
      // TODO: replace by kindlings-xml ?
      "org.scala-lang.modules" %% "scala-xml" % Versions.xml,
      "com.kubuszok" %% "scala-sax-parser" % Versions.scalaSaxParser,
      "com.outr" %% "scribe" % Versions.scribe,
      "io.github.cquiroz" %% "scala-java-time" % Versions.scalaJavaTime,
      "io.github.cquiroz" %% "scala-java-locales" % Versions.scalaJavaLocales
    )
  )

val regressionTest = (projectMatrix in file("sge-test/regression"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-test/regression") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    MatrixAction.ForPlatforms(VirtualAxis.js).Configure(_.settings(
      scalaJSUseMainModuleInitializer := true
    ))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge-test-regression",
    Compile / resourceGenerators += TestAssets.regressionAssets.taskValue
  )
  .dependsOn(sge)

// ── JVM Platform modules ──────────────────────────────────────────────
//
// Two modules for SGE-specific Android platform code:
//   sge-jvm-platform-api     — PanamaProvider type alias + Android ops interfaces (JDK 17)
//   sge-jvm-platform-android — Android backend impls (JDK 17, android.jar)
//
// PanamaProvider trait, JdkPanama, and PanamaPortProvider are now in
// multiarch-panama-api / multiarch-panama-jdk (published by multiarch-scala).
//
// These modules are not published. Their class files are merged into sge's JVM JAR
// at package time (see sge jvmPlatform settings).

val `sge-jvm-platform-api` = (projectMatrix in file("sge-jvm-platform/api"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-jvm-platform/api") ++ dev.only1VersionInIDE) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    scalacOptions ++= Seq("-release", "17"),
    libraryDependencies += "com.kubuszok" %% "multiarch-panama-api" % Versions.multiarch
  )

// SGE convention: downloaded Android SDK lives under sge-deps/
lazy val sgeAndroidSdkCacheDir: File = new File("./sge-deps/android-sdk")
lazy val hasAndroidSdk: Boolean      = _root_.multiarch.sbt.AndroidSdk
  .findSdkRoot(sgeAndroidSdkCacheDir)
  .exists(r => _root_.multiarch.sbt.AndroidSdk.androidJar(r).exists())

val `sge-jvm-platform-android` = (projectMatrix in file("sge-jvm-platform/android"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-jvm-platform/android") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(
      (SgePlugin.relaxedSettings ++ Seq(
        scalacOptions += "-Wconf:cat=deprecation:s"
      )) *
    ))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    scalacOptions ++= Seq("-release", "17"),
    // The scala-android SgeActivity shell reads sge-core class signatures that
    // reference `lowlevel.Nullable` (from lls), so lls must be on this module's
    // compile classpath to resolve those signatures.
    libraryDependencies += "com.kubuszok" %% "lls" % Versions.lls,
    Compile / unmanagedJars ++= Def.uncached {
      val conv     = fileConverter.value
      val base     = (ThisBuild / baseDirectory).value
      val cacheDir = base / "sge-deps" / "android-sdk"
      _root_.multiarch.sbt.AndroidSdk.findSdkRoot(cacheDir).toSeq.flatMap { sdkRoot =>
        val jar = _root_.multiarch.sbt.AndroidSdk.androidJar(sdkRoot)
        if (jar.exists()) blankCp(Seq(jar), conv) else Seq.empty
      }
    },
    Compile / unmanagedSourceDirectories ++= {
      if (hasAndroidSdk)
        Seq((ThisBuild / baseDirectory).value / "sge-jvm-platform" / "android" / "src" / "main" / "scala-android")
      else Seq.empty
    }
  )
  // The scala-android SgeActivity shell (sge/SgeActivity.scala) references
  // sge-core types (Sge, AndroidApplication, SgeAndroidDriver, AndroidGraphics,
  // Pixels). Depend on the sge-core matrix directly so its JVM-row compile
  // products land on this module's compile classpath and the build is ordered
  // android.compile -> sge.compile. This MUST be a matrix-level `.dependsOn` (a
  // deferred reference resolved during load), NOT `sge.jvm(...).products` on
  // unmanagedClasspath: the latter eagerly forces sge's row materialization at
  // settings-construction time, which re-enters sge's own Test/packageBin
  // reference to this module and dereferences a still-initializing build val
  // (NPE). sge-core does NOT `.dependsOn` this module (it pulls these products
  // only at Test/packageBin, via tasks), so the dependsOn graph
  // android -> sge -> api stays acyclic.
  // sbt-2.0 cycle break: this module is now sge-FREE (SgeActivity moved to
  // sge-core as `sge.SgeActivity`, taking the only sge-core reference with it),
  // so it dependsOn ONLY the api module. The former `.dependsOn(sge)` + sge
  // pulling this module's products was an sge<->android mutually-recursive pair
  // that sbt 1.x tolerated lazily but sbt 2.0's eager task graph deadlocked on.
  .dependsOn(`sge-jvm-platform-api`)

// ── Extension modules ─────────────────────────────────────────────────
//
// Published separately from sge-core. They depend on sge.

val `sge-ai` = (projectMatrix in file("sge-extension/ai"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/ai") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-ai")
  .dependsOn(sge)

val `sge-anim8` = (projectMatrix in file("sge-extension/anim8"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/anim8") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-anim8")
  .dependsOn(sge)

val `sge-colorful` = (projectMatrix in file("sge-extension/colorful"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/colorful") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-colorful")
  .dependsOn(sge)

val `sge-controllers` = (projectMatrix in file("sge-extension/controllers"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/controllers") ++ dev.only1VersionInIDE ++ Seq(
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.js).Configure(_.settings(
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "sge-extension" / "controllers" / "src" / "main" / "scala-js"
    )),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "sge-extension" / "controllers" / "src" / "main" / "scalanative"
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-controllers")
  .dependsOn(sge)

val `sge-ecs` = (projectMatrix in file("sge-extension/ecs"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/ecs") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-ecs")
  .dependsOn(sge)

val `sge-freetype` = (projectMatrix in file("sge-extension/freetype"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  // FreeType is a native C library (glyph rasterization) with no browser/WASM
  // backend — JS is intentionally NOT a target. JS games pre-bake bitmap fonts
  // (.fnt + .png) on JVM/Native and load those with BitmapFont. Dropping the JS
  // axis makes that a resolution-time signal (no sge-freetype JS artifact)
  // instead of a runtime UnsupportedOperationException. ISS-553.
  .someVariations(Versions.scalas, List(VirtualAxis.jvm, VirtualAxis.native))((commonSettings("sge-extension/freetype") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-freetype-desktop" % Versions.nativeComponents,
      Compile / packageBin / mappings ++= Def.uncached {
        val conv       = fileConverter.value
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isFreetypeLib(name: String) = sharedLibExts.exists(name.endsWith) && name.contains("sge_freetype")
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isFreetypeLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isFreetypeLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        blankMappings(crossMappings ++ hostMappings, conv)
      }
    )),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-freetype" % Versions.nativeComponents
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-freetype")
  .dependsOn(sge)

val `sge-gltf` = (projectMatrix in file("sge-extension/gltf"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/gltf") ++ dev.only1VersionInIDE ++ Seq(
    // ISS-533: gltf's own native test binary links sge_native_ops symbols (via
    // PixmapIO etc.), so the native axis needs the NativeProviderPlugin wiring
    // (manifest discovery, extraction, -L linker flags) plus the sn-provider-sge
    // native artifact — mirroring `sge` and `sge-physics`.
    //
    // ORDER MATTERS: `nativeProviderSettings` MUST precede the `ForAll`
    // `relaxedSettings` Configure. commandmatrix's `collapse` reduces all matching
    // `Configure` functions left-to-right with `andThen`, each doing `_.settings(..)`.
    // When a broad `ForAll.Configure(_.settings(relaxedSettings *))` is composed
    // *before* the native-only `nativeProviderSettings` Configure, the resulting
    // last-applied `.settings` block shadows NativeProviderPlugin's `nativeConfig :=`
    // override on the native axis, so no `-L<extractedDir>` is added and the
    // libsge_native_ops.a extraction never runs (the `-l sge_native_ops` flag still
    // arrives via sge core's @link annotations, hence "library not found" at link).
    // Putting the provider wiring first matches the working `sge`/`sge-physics`/
    // `regressionTest` ordering, where the provider's nativeConfig survives.
    nativeProviderSettings,
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge" % Versions.nativeComponents
    )),
    // ISS-533: gltf's JVM tests build VertexArray-backed meshes, which allocate
    // unsafe ByteBuffers through BufferOpsPanama → NativeLibLoader.load("sge_native_ops").
    // The `pnm-provider-sge-desktop` Panama provider (the JAR carrying
    // libsge_native_ops for desktop) is NOT transitive from `sge` core, so — exactly
    // like `sge-physics` re-adds `pnm-provider-sge-physics-desktop` on its JVM axis —
    // gltf must pull the sge-core Panama provider onto its own JVM axis or the native
    // lib is absent on the forked test classpath (UnsatisfiedLinkError).
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-desktop" % Versions.nativeComponents,
      // android.jar leaks onto gltf's test classpath transitively from `sge`
      // core's Compile/unmanagedJars (when the Android SDK is present). It is a
      // STUB jar, and multiarch's NativeLibLoader detects the host as Android
      // purely by `Class.forName("android.app.Activity")` — so its presence on
      // the test runtime makes NativeLibLoader resolve the wrong native-lib path
      // (android-aarch64) instead of the desktop provider's libsge_native_ops.
      // sge core filters it from its own Test/fullClasspath for the same reason;
      // mirror that here so the desktop Panama provider's lib is the one found.
      Test / fullClasspath := Def.uncached {
        val conv = fileConverter.value
        (Test / fullClasspath).value.filterNot(e => conv.toPath(e.data).getFileName.toString == "android.jar")
      }
    )),
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    jvmPlatformApiClasspath
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge-extension-gltf",
    scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-Wconf:msg=Implicit parameters should be provided:s",
      "-Wconf:msg=Non local returns:s",
      "-Wconf:msg=Unreachable case:s"
    )
  )
  .dependsOn(sge)

val `sge-graphs` = (projectMatrix in file("sge-extension/graphs"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/graphs") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-graphs")

val `sge-jbump` = (projectMatrix in file("sge-extension/jbump"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/jbump") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-jbump")

val `sge-noise` = (projectMatrix in file("sge-extension/noise"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/noise") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-noise")

val `sge-physics` = (projectMatrix in file("sge-extension/physics"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/physics") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(Seq(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-physics-desktop" % Versions.nativeComponents,
      Test / unmanagedClasspath ++= Def.uncached {
        val conv    = fileConverter.value
        val apiDirs = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
        blankCp(apiDirs, conv)
      },
      Compile / packageBin / mappings ++= Def.uncached {
        val conv       = fileConverter.value
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isPhysicsLib(name: String) = sharedLibExts.exists(name.endsWith) && name.contains("sge_physics")
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isPhysicsLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isPhysicsLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        blankMappings(crossMappings ++ hostMappings, conv)
      }
    ) *)),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-physics" % Versions.nativeComponents
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-physics")
  .dependsOn(sge)

val `sge-physics3d` = (projectMatrix in file("sge-extension/physics3d"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/physics3d") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(Seq(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-physics3d-desktop" % Versions.nativeComponents,
      Test / unmanagedClasspath ++= Def.uncached {
        val conv    = fileConverter.value
        val apiDirs = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
        blankCp(apiDirs, conv)
      },
      Compile / packageBin / mappings ++= Def.uncached {
        val conv       = fileConverter.value
        val crossDir   = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
        val releaseDir = (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
        val sharedLibExts = Set(".so", ".dylib", ".dll")
        def isPhysics3dLib(name: String) = sharedLibExts.exists(name.endsWith) && name.contains("sge_physics3d")
        val crossMappings = Platform.desktop.flatMap { platform =>
          val dir = crossDir / platform.classifier
          if (dir.exists()) IO.listFiles(dir).filter(f => f.isFile && isPhysics3dLib(f.getName))
            .map(f => f -> s"native/${platform.classifier}/${f.getName}")
            .toSeq
          else Seq.empty
        }
        val hostMappings =
          if (crossMappings.nonEmpty) Seq.empty
          else if (releaseDir.exists()) {
            val host = Platform.host
            IO.listFiles(releaseDir).filter(f => f.isFile && isPhysics3dLib(f.getName))
              .map(f => f -> s"native/${host.classifier}/${f.getName}")
              .toSeq
          } else Seq.empty
        blankMappings(crossMappings ++ hostMappings, conv)
      }
    ) *)),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-physics3d" % Versions.nativeComponents
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-physics3d")
  .dependsOn(sge)

val `sge-screens` = (projectMatrix in file("sge-extension/screens"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/screens") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-screens")
  .dependsOn(sge)

val `sge-textra` = (projectMatrix in file("sge-extension/textra"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/textra") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-textra")
  .dependsOn(sge)

val `sge-tools` = (projectMatrix in file("sge-extension/tools"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-extension/tools") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      Compile / unmanagedClasspath ++= Def.uncached {
        val conv        = fileConverter.value
        val apiDirs     = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(Versions.scala3) / Compile / products).value
        blankCp(apiDirs ++ androidDirs, conv)
      }
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge-extension-tools",
    Compile / mainClass := Some("sge.tools.texturepacker.TexturePacker")
  )
  .dependsOn(sge)

val `sge-vfx` = (projectMatrix in file("sge-extension/vfx"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/vfx") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-vfx")
  .dependsOn(sge)

val `sge-visui` = (projectMatrix in file("sge-extension/visui"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, Versions.platforms)((commonSettings("sge-extension/visui") ++ dev.only1VersionInIDE ++ Seq(
    jvmPlatformApiClasspath,
    // ISS-531: visui's JVM tests construct a Stage -> SpriteBatch -> Mesh -> VBO,
    // which allocates unsafe ByteBuffers via BufferOpsPanama ->
    // NativeLibLoader.load("sge_native_ops"). The `pnm-provider-sge-desktop`
    // Panama provider (the JAR carrying libsge_native_ops for desktop) is NOT
    // transitive from `sge` core, so add it on the JVM axis (mirroring sge-gltf /
    // sge-physics) and filter the android.jar stub from the test classpath (its
    // presence makes multiarch's NativeLibLoader mis-detect the host as Android
    // and resolve the wrong native-lib path).
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-desktop" % Versions.nativeComponents,
      Test / fullClasspath := Def.uncached {
        val conv = fileConverter.value
        (Test / fullClasspath).value.filterNot(e => conv.toPath(e.data).getFileName.toString == "android.jar")
      },
      // VisUI is a process-global singleton (VisUI._skin); multiple suites
      // load()/dispose() it per-test, so running suites concurrently races the
      // global state ("VisUI is not loaded!" / "cannot be loaded twice"). Run the
      // JVM suites sequentially. JS/Native are single-threaded and unaffected.
      Test / parallelExecution := false
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-visui")
  .dependsOn(sge)

// ── Android smoke test APK ────────────────────────────────────────────
//
// Minimal Android app that bootstraps SGE, renders 30 frames, and
// exits. Built into an APK via AndroidBuild pipeline (d8 → aapt2 →
// apksigner). Used by sge-it-android to catch runtime crashes.
//
// Build: sbt 'sge-android-smoke/androidSign'
// Prerequisites: Android SDK (auto-downloaded by the sbt androidSdkRoot task on first invocation)

val `sge-android-smoke` = (projectMatrix in file("sge-test/android-smoke"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/android-smoke") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      Compile / unmanagedClasspath ++= Def.uncached {
        val conv        = fileConverter.value
        val apiDirs     = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(Versions.scala3) / Compile / products).value
        blankCp(apiDirs ++ androidDirs, conv)
      }
    ))
  )) *)
  .settings(_root_.multiarch.sbt.AndroidBuild.taskSettings *)
  .settings(
    _root_.multiarch.sbt.AndroidBuild.androidSdkCacheDir := (ThisBuild / baseDirectory).value / "sge-deps" / "android-sdk"
  )
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge-test-android-smoke",
    Compile / unmanagedSourceDirectories ++= {
      if (hasAndroidSdk)
        Seq((ThisBuild / baseDirectory).value / "sge-test" / "android-smoke" / "src" / "main" / "scala-android")
      else Seq.empty
    }
  )
  .dependsOn(sge)

// ── Android Robolectric harness ───────────────────────────────────────
//
// Drives the real Android backend impl classes (the ones in
// sge-jvm-platform/android/src/main/scala-android) on a plain JVM via
// Robolectric 4.14.1 — no emulator, no Android SDK. Robolectric supplies a
// full android.* runtime (the `android-all-instrumented` framework jar), so
// the impl layer can be unit-tested off-device.
//
// This module is ADDITIVE: it does not touch how sge-jvm-platform-android
// compiles WITH the SDK (that path + the CI emulator/APK jobs stay intact).
// Because sge-jvm-platform-android compiles NOTHING without the SDK present,
// this module compiles the impl sources itself, against android-all-
// instrumented, rather than depending on that module's (empty) products.
//
// Scope boundary — impl classes that *subclass* an instrumented android
// View/Window class (GLSurfaceView, PopupWindow, AutoCompleteTextView,
// WallpaperService) cannot compile against the static android-all jar:
// Robolectric instruments those parents to implement ShadowedObject, whose
// synthetic $$robo$getData() method is only given a body at runtime via
// bytecode rewriting, so it stays abstract in the static jar and the Scala
// compiler (unlike javac) refuses to leave it unimplemented in a subclass.
// Those four files + the aggregator that instantiates them are excluded from
// this module's source set. The remaining impls (which only *call* android
// APIs — preferences, clipboard, sensors, files, audio, …) compile and run.
//
// JDK pin — Robolectric 4.14.1 bundles ASM 9.x, which cannot parse JDK-25
// (class-file major 69) bytecode; its instrumenting ClassLoader reads java.*
// classes from the running JVM, so the TEST FORK must run on JDK 21. The
// compile JVM stays at the build default. If JDK 21 is absent this module
// SKIPS its tests (empty test source set + a warning) rather than failing the
// build — it never hard-breaks environments without JDK 21.
//
// Run: sbt --client 'sge-android-robolectric/test'

// Robolectric needs two androidx .aar artifacts (androidx.test:monitor,
// androidx.tracing:tracing) on the runtime classpath for
// InstrumentationRegistry. coursier/sbt don't unpack .aar onto the classpath,
// so we declare them in a hidden `Aar` Ivy config (intransitive — their poms
// list .jar deps that 404 on maven.google.com), resolve them, and unzip each
// inner classes.jar into Test / unmanagedJars.
lazy val Aar = config("aar").hide
lazy val extractAars = taskKey[Seq[File]]("Unzip classes.jar out of resolved .aar artifacts")

// JDK 21 home for the Robolectric test fork, if available. Honours JAVA21_HOME
// first, then the sdkman graal build used in CI/dev, then any sdkman 21.* dir.
lazy val robolectricJdk21Home: Option[File] = {
  val fromEnv = sys.env.get("JAVA21_HOME").map(file).filter(_.exists)
  def fromSdkman = {
    val candidates = file(sys.props("user.home")) / ".sdkman" / "candidates" / "java"
    val pinned     = candidates / "21.0.6-graal"
    if (pinned.exists) Some(pinned)
    else if (candidates.exists)
      candidates.listFiles().toSeq.sortBy(_.getName).reverse.find(d => d.isDirectory && d.getName.startsWith("21."))
    else None
  }
  fromEnv.orElse(fromSdkman)
}

val `sge-android-robolectric` = (projectMatrix in file("sge-test/android-robolectric"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/android-robolectric") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(
      (SgePlugin.relaxedSettings ++ Seq(
        scalacOptions += "-Wconf:cat=deprecation:s"
      )) *
    ))
  )) *)
  .configs(Aar)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge-test-android-robolectric",

    ivyConfigurations += Aar,
    classpathTypes += "aar",
    resolvers += "google" at "https://maven.google.com",

    // Robolectric runs JUnit4 tests via junit-interface; not munit.
    testFrameworks := Seq(new TestFramework("com.novocode.junit.JUnitFramework")),
    testOptions += Tests.Argument(TestFramework("com.novocode.junit.JUnitFramework"), "-a", "-v"),

    libraryDependencies ++= Seq(
      "org.robolectric" % "robolectric"              % "4.14.1"                     % Test,
      // Provided (not Test): the android.* framework jar must be on the COMPILE
      // classpath so the impl sources type-check, and on the test classpath at
      // runtime. Provided keeps it off any published artifact (this module is
      // non-published anyway).
      "org.robolectric" % "android-all-instrumented" % "14-robolectric-10818077-i6" % Provided,
      // The classes in android-all-instrumented implement Robolectric's
      // org.robolectric.internal.bytecode.{InstrumentedInterface,ShadowedObject}
      // (these live in the `sandbox` artifact). The Scala compiler reads those
      // signatures while type-checking the impl sources, so `sandbox` must be on
      // the COMPILE classpath too — robolectric itself is Test-scoped and would
      // otherwise leave sandbox off compile.
      "org.robolectric" % "sandbox"                  % "4.14.1"                     % Provided,
      "com.github.sbt"  % "junit-interface"          % "0.13.3"                     % Test,
      "junit"           % "junit"                    % "4.13.2"                     % Test,
      ("androidx.test"    % "monitor" % "1.7.2" % Aar)
        .intransitive()
        .artifacts(Artifact("monitor", "aar", "aar")),
      ("androidx.tracing" % "tracing" % "1.1.0" % Aar)
        .intransitive()
        .artifacts(Artifact("tracing", "aar", "aar"))
    ),

    // Compile the impl layer here, against the Robolectric framework jar. The
    // sge-jvm-platform-android module compiles these only WITH the SDK; this
    // module compiles the SDK-independent subset itself. The framework jar
    // (android-all-instrumented) is already a Test dependency, so it's on the
    // compile classpath through Test scope at test time; we also add it to the
    // compile classpath explicitly so the impl sources type-check.
    Compile / unmanagedSourceDirectories ++= {
      val androidImplDir =
        (ThisBuild / baseDirectory).value / "sge-jvm-platform" / "android" / "src" / "main" / "scala-android"
      if (robolectricJdk21Home.isDefined && androidImplDir.exists) Seq(androidImplDir) else Seq.empty
    },
    // Exclude impl classes that subclass an instrumented android class (see
    // the scope-boundary note above) plus the aggregator that instantiates
    // them. The remaining impls compile clean against android-all.
    Compile / unmanagedSources := {
      val excluded = Set(
        "AndroidGLSurfaceViewImpl.scala",
        "AndroidInputMethodImpl.scala",
        "StandardKeyboardHeightProviderImpl.scala",
        "AndroidLiveWallpaperServiceImpl.scala",
        "AndroidPlatformProviderImpl.scala",
        // SgeActivity references sge-core types (Sge, AndroidApplication,
        // SgeAndroidDriver) that this SDK-independent harness does not put on its
        // classpath; its driver logic is covered by SgeAndroidDriverRedSuite.
        "SgeActivity.scala"
      )
      (Compile / unmanagedSources).value.filterNot(f => excluded.contains(f.getName))
    },
    // api ops interfaces (PreferencesOps, ClipboardOps, …) for the impls.
    // Needed at compile time (impl sources implement them) AND at test compile
    // time (the test references the impls, whose supertypes javac must resolve)
    // and at test runtime.
    Compile / unmanagedClasspath ++= Def.uncached {
      val conv    = fileConverter.value
      val apiDirs = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
      blankCp(apiDirs, conv)
    },
    Test / unmanagedClasspath ++= Def.uncached {
      val conv    = fileConverter.value
      val apiDirs = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
      blankCp(apiDirs, conv)
    },
    // android-all-instrumented is Provided, so it is already on the compile
    // classpath — the impl sources type-check against the framework classes.

    // Unzip classes.jar out of each resolved .aar into target/aar-classes.
    extractAars := Def.uncached {
      val log    = streams.value.log
      val report = update.value
      val outDir = target.value / "aar-classes"
      IO.createDirectory(outDir)
      val aars = report.select(configurationFilter(Aar.name)).filter(_.getName.endsWith(".aar"))
      log.info(s"extractAars: ${aars.size} AAR(s) -> $outDir")
      aars.toSeq.map { aar =>
        val baseName    = aar.getName.stripSuffix(".aar")
        val outJar      = outDir / s"$baseName-classes.jar"
        val tmp         = IO.createTemporaryDirectory
        IO.unzip(aar, tmp)
        val classesJar  = tmp / "classes.jar"
        if (!classesJar.exists)
          sys.error(s"AAR ${aar.getName} has no classes.jar (found: ${IO.listFiles(tmp).map(_.getName).mkString(", ")})")
        IO.copyFile(classesJar, outJar)
        IO.delete(tmp)
        log.info(s"extractAars: ${aar.getName} -> ${outJar.getName}")
        outJar
      }
    },
    Test / unmanagedJars ++= Def.uncached {
      val conv = fileConverter.value
      blankCp(extractAars.value, conv)
    },

    // The impl classes compiled here live in this module's product dir, which
    // is already on the test classpath; the test instantiates them directly.

    // JDK-21 handling — graceful, never hard-breaks the build.
    //   present: run the test fork on JDK 21.
    //   absent : skip this module's tests (empty test source set) + warn.
    Test / fork := robolectricJdk21Home.isDefined,
    Test / javaHome := robolectricJdk21Home,
    Test / javaOptions ++= Seq(
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
    ),
    Test / sources := {
      val log = sLog.value
      if (robolectricJdk21Home.isDefined) (Test / sources).value
      else {
        log.warn(
          "[sge-android-robolectric] JDK 21 not found (set JAVA21_HOME or install via sdkman) — " +
            "Robolectric tests SKIPPED. Robolectric 4.14.1's ASM cannot run on JDK 25+."
        )
        Seq.empty
      }
    }
  )

// ── Integration tests ─────────────────────────────────────────────────
//
// Separate non-published modules with isolated classpaths to verify:
//   - JVM platform provider discovery (PanamaProvider, JdkPanama)
//   - Android ops API interfaces (self-contained, JDK types only)
//   - Desktop end-to-end: GLFW + ANGLE + miniaudio + FileIO + JSON/XML
//   - Browser JS output in real headless Chromium (Playwright)
//   - Android APK on headless emulator with SwiftShader GL ES
//
// These modules depend directly on the platform modules (NOT sge) so
// they test the API/impl boundaries in isolation.

// Desktop integration tests — launches a real GLFW + ANGLE window with
// miniaudio audio engine and exercises all subsystems end-to-end:
// bootstrap, GL2D, GL3D, audio, file I/O, JSON/XML parsing.
//
// Prerequisites: native libs from sge-native-providers provider JARs (auto-resolved by sbt)
//
// Run: sbt --client 'sge-it-desktop/test'  or  re-scale runner desktop-it
val `sge-it-desktop` = (projectMatrix in file("sge-test/it-desktop"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-desktop") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(Seq(
      resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
      libraryDependencies += "com.outr" %% "scribe" % Versions.scribe,
      Compile / unmanagedClasspath ++= Def.uncached {
        val conv        = fileConverter.value
        val apiDirs     = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(Versions.scala3) / Compile / products).value
        blankCp(apiDirs ++ androidDirs, conv)
      },
      Test / unmanagedClasspath ++= Def.uncached {
        val conv        = fileConverter.value
        val apiDirs     = (`sge-jvm-platform-api`.jvm(Versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(Versions.scala3) / Compile / products).value
        blankCp(apiDirs ++ androidDirs, conv)
      },
      // Native libs are resolved from the provider JARs on the (test) classpath
      // by multiarch.core.NativeLibLoader at runtime — no java.library.path
      // wiring (the old sge-deps/native-components/target/release dir is a local
      // Rust build dir that CI never creates; pointing java.library.path at it
      // made the headless symbol test assume-skip, see ISS-485).
      // Don't pass -XstartOnFirstThread — the windowed test launches a
      // subprocess with that flag.
      //
      // Keep the Android SDK stub jar off the Test fork classpath. It arrives
      // transitively via sge's Compile classpath (sge adds it as unmanagedJars
      // for SgeActivity), but multiarch's NativeLibLoader detects the host as
      // Android purely by Class.forName("android.app.Activity") — so android.jar
      // on the test runtime makes every native-lib lookup resolve the wrong
      // android-<arch> path (the harness then exits 2, UnsatisfiedLinkError).
      // sge-core applies the same filter to its own Test classpath; sge-it-desktop
      // must repeat it because the jar re-enters through the dependsOn edge.
      Test / fullClasspath := Def.uncached {
        val conv = fileConverter.value
        (Test / fullClasspath).value.filterNot(e => conv.toPath(e.data).getFileName.toString == "android.jar")
      },
      // Inject the real Test/fullClasspath as a system property: sbt 2.0 forks
      // tests via sbt.ForkMain with only agent jars on the JVM -cp, so the
      // harness subprocesses can no longer read the app classpath from
      // java.class.path (DesktopIntegrationTest.harnessClasspath consumes this).
      // Reads the android.jar-filtered fullClasspath above.
      Test / javaOptions := Def.uncached {
        val conv = fileConverter.value
        val cp   = (Test / fullClasspath).value.map(e => conv.toPath(e.data).toFile.getAbsolutePath).mkString(java.io.File.pathSeparator)
        Seq("--enable-native-access=ALL-UNNAMED", s"-Dsge.it.classpath=$cp")
      }
    ) *)),
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .dependsOn(sge, `sge-freetype`, `sge-physics`)

val `sge-it-jvm-platform` = (projectMatrix in file("sge-test/it-jvm-platform"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-jvm-platform") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    libraryDependencies += "com.kubuszok" %% "multiarch-panama-jdk" % Versions.multiarch
  )
  .dependsOn(`sge-jvm-platform-api`, `sge-jvm-platform-android`)

// Browser integration tests — JVM-based Playwright tests that exercise compiled
// Scala.js output in a real headless Chromium browser. Catches runtime JS errors
// (ReferenceError, TypeError, null/undefined, TypedArray conversions) that
// Node.js can't detect.
//
// Prerequisites: run `npx playwright@1.49.0 install chromium` once to install
// the browser binary. Playwright Java auto-manages the driver.
//
// Run: sbt --client 'sge-it-browser/test'  or  re-scale runner browser-it
val `sge-it-browser` = (projectMatrix in file("sge-test/it-browser"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-browser") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    Test / baseDirectory := (ThisBuild / baseDirectory).value,
    libraryDependencies += "com.microsoft.playwright" % "playwright" % "1.60.0" % Test,
    Test / resourceGenerators += Def.task {
      Def.uncached {
        val _ = (sge.js(Versions.scala3) / Compile / fullClasspath).value
        Seq.empty[File]
      }
    }.taskValue
  )

// Android integration tests — JVM-based tests that deploy the smoke APK
// to a headless Android emulator (AVD with SwiftShader) and monitor
// logcat for runtime crashes. Catches ClassNotFoundException, NPE,
// UnsatisfiedLinkError, GL errors, and any FATAL exception during
// app startup.
//
// Prerequisites:
//   1. Android SDK + emulator + system image: auto-installed by androidSdkRoot
//   2. Build smoke APK: sbt 'sge-test-android-smoke/androidSign'
//   3. Create + start AVD via android-emulator-runner GitHub Action (or local AVD manager)
//
// Run: sbt --client 'sge-it-android/test'  or  re-scale runner android-it
val `sge-it-android` = (projectMatrix in file("sge-test/it-android"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-android") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    Test / baseDirectory := (ThisBuild / baseDirectory).value
  )

// Native FFI wiring validation — Scala Native executable that exercises every
// native C ABI endpoint (sge_native_ops, sge_audio, GLFW, EGL, GLESv2) to verify
// correct symbol resolution, ABI compatibility, and pointer calculations.
// Catches runtime SIGSEGVs from wrong parameter types or buffer offset bugs.
//
// Run: sbt --client 'sge-it-native-ffi/run'  or  re-scale runner native-ffi-it
val `sge-it-native-ffi` = (projectMatrix in file("sge-test/it-native-ffi"))
  .defaultAxes(VirtualAxis.native, VirtualAxis.scalaABIVersion(Versions.scala3))
  .someVariations(Versions.scalas, List(VirtualAxis.native))((commonSettings("sge-test/it-native-ffi") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    nativeProviderSettings
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .dependsOn(sge)

// Write published version to demos/.sge-version so the demos sub-build
// resolves the same version without depending on sbt-git or env vars.
val writeDemoVersion = taskKey[Unit]("Write SGE version to .sge-version")
ThisBuild / writeDemoVersion := {
  val v = version.value
  val base = baseDirectory.value
  // Write to both root (for sge-build plugin version) and demos/ (for demo dependency resolution)
  IO.write(base / ".sge-version", v)
  IO.write(base / "demos" / ".sge-version", v)
  streams.value.log.info(s"[sge] Wrote .sge-version: $v")
}

// ── Root project — git-based versioning ──────────────────────────────
lazy val root = (project in file("."))
  .enablePlugins(KubuszokRootPlugin)
  // Core library
  .aggregate(sge.projectRefs *)
  // JVM platform modules (merged into sge JAR)
  .aggregate(`sge-jvm-platform-api`.projectRefs *)
  .aggregate(`sge-jvm-platform-android`.projectRefs *)
  // Extensions
  .aggregate(`sge-ai`.projectRefs *)
  .aggregate(`sge-anim8`.projectRefs *)
  .aggregate(`sge-colorful`.projectRefs *)
  .aggregate(`sge-controllers`.projectRefs *)
  .aggregate(`sge-ecs`.projectRefs *)
  .aggregate(`sge-freetype`.projectRefs *)
  .aggregate(`sge-gltf`.projectRefs *)
  .aggregate(`sge-graphs`.projectRefs *)
  .aggregate(`sge-jbump`.projectRefs *)
  .aggregate(`sge-noise`.projectRefs *)
  .aggregate(`sge-physics`.projectRefs *)
  .aggregate(`sge-physics3d`.projectRefs *)
  .aggregate(`sge-screens`.projectRefs *)
  .aggregate(`sge-textra`.projectRefs *)
  .aggregate(`sge-tools`.projectRefs *)
  .aggregate(`sge-vfx`.projectRefs *)
  .aggregate(`sge-visui`.projectRefs *)
  // Tests
  .aggregate(regressionTest.projectRefs *)
  .aggregate(`sge-android-smoke`.projectRefs *)
  .aggregate(`sge-android-robolectric`.projectRefs *)
  // Integration tests
  .aggregate(`sge-it-desktop`.projectRefs *)
  .aggregate(`sge-it-jvm-platform`.projectRefs *)
  .aggregate(`sge-it-browser`.projectRefs *)
  .aggregate(`sge-it-android`.projectRefs *)
  .aggregate(`sge-it-native-ffi`.projectRefs *)
  .settings(
    name := "sge-root"
    // sbt-welcome (logo / usefulTasks) has no sbt-2.0 build and is no longer
    // bundled by sbt-kubuszok on the sbt-2.0 axis. The command aliases the old
    // `al.usefulTasks(...)` registered (ci-*, test-*, publishLocal-*) are now
    // registered explicitly via addCommandAlias (see sgeCommandAliases below).
  )
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(sgeCommandAliases *)
