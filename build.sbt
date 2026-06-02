import _root_.multiarch.sbt.Platform
import _root_.sge.sbt.{SgeNativeLibs, SgePlugin}
import sbtwelcome.UsefulTask
import commandmatrix.extra.*
import kubuszok.sbt._
import kubuszok.sbt.KubuszokPlugin.autoImport._

// Versions

val versions = new {
  // Versions we are publishing for.
  val scala3 = SgePlugin.scalaVersion

  // Which versions should be cross-compiled for publishing.
  val scalas = List(scala3)
  val platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)

  // Dependencies
  val gears            = "0.3.1"
  val kindlings        = "0.1.2"
  val lls              = "0.1.0"
  val scribe           = "3.17.0"
  val scalajsDom       = "2.8.1"
  val scalaSaxParser   = "0.1.0"
  val scalaJavaTime    = "2.6.0"
  val scalaJavaLocales = "1.5.4"
  val sttp             = "4.0.22"
  val xml              = "2.3.0"

  // Tests
  val munit           = "1.2.3"
  val munitScalacheck = "1.2.0"

  // Native component providers (from sge-native-providers repo)
  val multiarch        = "0.2.0"
  val nativeComponents = "0.1.2"
  val curlProvider     = multiarch
}

val dev = new DevProperties(
  scala213 = None,
  scala3 = Some(versions.scala3),
  platforms = versions.platforms
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
    regressionTest
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

def commonSettings(
  projectDir: String = "sge",
  rustLibPath: Option[String] = None
) = Seq(
  MatrixAction.ForAll.Configure(
    _.settings(SgePlugin.commonSettings *).settings(
      libraryDependencies ++= Seq(
        "org.scalameta"     %%% "munit"             % versions.munit % Test,
        "org.scalameta"     %%% "munit-scalacheck"  % versions.munitScalacheck % Test
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
    }
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
    Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://github.com/MateuszKubuszok"))
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
  Compile / unmanagedClasspath ++= {
    val apiDirs = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
    apiDirs.map(Attributed.blank)
  }
))

// Core library

val sge = (projectMatrix in file("sge"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings() ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      (SgeNativeLibs.validationSettings ++ Seq(
        libraryDependencies += "ch.epfl.lamp" %% "gears" % versions.gears,
        libraryDependencies += "com.kubuszok" %% "multiarch-core" % versions.multiarch,
        libraryDependencies += "com.kubuszok" %% "multiarch-panama-jdk" % versions.multiarch,
        libraryDependencies += "com.kubuszok" %  "pnm-provider-sge-desktop" % versions.nativeComponents,
        Compile / unmanagedClasspath ++= {
          val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
          val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
          (apiDirs ++ androidDirs).map(Attributed.blank)
        },
        Test / unmanagedClasspath ++= {
          val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
          val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
          (apiDirs ++ androidDirs).map(Attributed.blank)
        },
        Compile / packageBin / mappings ++= {
          val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
          val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
          (apiDirs ++ androidDirs).flatMap(collectClassFiles)
        },
        Compile / packageBin / mappings ++= {
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
          crossMappings ++ hostMappings ++ androidMappings
        }
      )) *
    )),
    MatrixAction.ForPlatforms(VirtualAxis.js).Configure(_.settings(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % versions.scalajsDom
    )),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies ++= Seq(
        "com.kubuszok" % "sn-provider-sge"  % versions.nativeComponents,
        "com.kubuszok" % "sn-provider-curl" % versions.curlProvider
      )
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(
    name := "sge",
    libraryDependencies ++= Seq(
      "com.kubuszok" %%% "lls" % versions.lls,
      "com.kubuszok" %%% "kindlings-fast-show-pretty" % versions.kindlings,
      "com.kubuszok" %%% "kindlings-jsoniter-derivation" % versions.kindlings,
      "com.kubuszok" %%% "kindlings-jsoniter-json" % versions.kindlings,
      "com.kubuszok" %%% "kindlings-ubjson-derivation" % versions.kindlings,
      "com.softwaremill.sttp.client4" %%% "core" % versions.sttp,
      // TODO: replace by kindlings-xml ?
      "org.scala-lang.modules" %%% "scala-xml" % versions.xml,
      "com.kubuszok" %%% "scala-sax-parser" % versions.scalaSaxParser,
      "com.outr" %%% "scribe" % versions.scribe,
      "io.github.cquiroz" %%% "scala-java-time" % versions.scalaJavaTime,
      "io.github.cquiroz" %%% "scala-java-locales" % versions.scalaJavaLocales
    )
  )

val regressionTest = (projectMatrix in file("sge-test/regression"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-test/regression") ++ dev.only1VersionInIDE ++ Seq(
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-jvm-platform/api") ++ dev.only1VersionInIDE) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    scalacOptions ++= Seq("-release", "17"),
    libraryDependencies += "com.kubuszok" %% "multiarch-panama-api" % "0.2.0"
  )

// SGE convention: downloaded Android SDK lives under sge-deps/
lazy val sgeAndroidSdkCacheDir: File = new File("./sge-deps/android-sdk")
lazy val hasAndroidSdk: Boolean      = _root_.multiarch.sbt.AndroidSdk
  .findSdkRoot(sgeAndroidSdkCacheDir)
  .exists(r => _root_.multiarch.sbt.AndroidSdk.androidJar(r).exists())

val `sge-jvm-platform-android` = (projectMatrix in file("sge-jvm-platform/android"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-jvm-platform/android") ++ dev.only1VersionInIDE ++ Seq(
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
    Compile / unmanagedJars ++= {
      val base     = (ThisBuild / baseDirectory).value
      val cacheDir = base / "sge-deps" / "android-sdk"
      _root_.multiarch.sbt.AndroidSdk.findSdkRoot(cacheDir).toSeq.flatMap { sdkRoot =>
        val jar = _root_.multiarch.sbt.AndroidSdk.androidJar(sdkRoot)
        if (jar.exists()) Seq(Attributed.blank(jar)) else Seq.empty
      }
    },
    Compile / unmanagedSourceDirectories ++= {
      if (hasAndroidSdk)
        Seq((ThisBuild / baseDirectory).value / "sge-jvm-platform" / "android" / "src" / "main" / "scala-android")
      else Seq.empty
    }
  )
  .dependsOn(`sge-jvm-platform-api`)

// ── Extension modules ─────────────────────────────────────────────────
//
// Published separately from sge-core. They depend on sge.

val `sge-ai` = (projectMatrix in file("sge-extension/ai"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/ai") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-ai")
  .dependsOn(sge)

val `sge-anim8` = (projectMatrix in file("sge-extension/anim8"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/anim8") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-anim8")
  .dependsOn(sge)

val `sge-colorful` = (projectMatrix in file("sge-extension/colorful"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/colorful") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-colorful")
  .dependsOn(sge)

val `sge-controllers` = (projectMatrix in file("sge-extension/controllers"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/controllers") ++ dev.only1VersionInIDE ++ Seq(
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/ecs") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-ecs")
  .dependsOn(sge)

val `sge-freetype` = (projectMatrix in file("sge-extension/freetype"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/freetype") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-freetype-desktop" % versions.nativeComponents,
      Compile / packageBin / mappings ++= {
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
        crossMappings ++ hostMappings
      }
    )),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-freetype" % versions.nativeComponents
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-freetype")
  .dependsOn(sge)

val `sge-gltf` = (projectMatrix in file("sge-extension/gltf"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/gltf") ++ dev.only1VersionInIDE ++ Seq(
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/graphs") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-graphs")

val `sge-jbump` = (projectMatrix in file("sge-extension/jbump"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/jbump") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-jbump")

val `sge-noise` = (projectMatrix in file("sge-extension/noise"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/noise") ++ dev.only1VersionInIDE) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-noise")

val `sge-physics` = (projectMatrix in file("sge-extension/physics"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/physics") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(Seq(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-physics-desktop" % versions.nativeComponents,
      Test / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
        apiDirs.map(Attributed.blank)
      },
      Compile / packageBin / mappings ++= {
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
        crossMappings ++ hostMappings
      }
    ) *)),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-physics" % versions.nativeComponents
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-physics")
  .dependsOn(sge)

val `sge-physics3d` = (projectMatrix in file("sge-extension/physics3d"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/physics3d") ++ dev.only1VersionInIDE ++ Seq(
    nativeProviderSettings,
    jvmPlatformApiClasspath,
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(Seq(
      libraryDependencies += "com.kubuszok" % "pnm-provider-sge-physics3d-desktop" % versions.nativeComponents,
      Test / unmanagedClasspath ++= {
        val apiDirs = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
        apiDirs.map(Attributed.blank)
      },
      Compile / packageBin / mappings ++= {
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
        crossMappings ++ hostMappings
      }
    ) *)),
    MatrixAction.ForPlatforms(VirtualAxis.native).Configure(_.settings(
      libraryDependencies += "com.kubuszok" % "sn-provider-sge-physics3d" % versions.nativeComponents
    ))
  )) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-physics3d")
  .dependsOn(sge)

val `sge-screens` = (projectMatrix in file("sge-extension/screens"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/screens") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-screens")
  .dependsOn(sge)

val `sge-textra` = (projectMatrix in file("sge-extension/textra"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/textra") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-textra")
  .dependsOn(sge)

val `sge-tools` = (projectMatrix in file("sge-extension/tools"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-extension/tools") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      Compile / unmanagedClasspath ++= {
        val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
        (apiDirs ++ androidDirs).map(Attributed.blank)
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/vfx") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
  .settings(publishSettings)
  .settings(mimaSettings)
  .settings(name := "sge-extension-vfx")
  .dependsOn(sge)

val `sge-visui` = (projectMatrix in file("sge-extension/visui"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, versions.platforms)((commonSettings("sge-extension/visui") ++ dev.only1VersionInIDE ++ Seq(jvmPlatformApiClasspath)) *)
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/android-smoke") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *)),
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(
      Compile / unmanagedClasspath ++= {
        val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
        (apiDirs ++ androidDirs).map(Attributed.blank)
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-desktop") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForPlatforms(VirtualAxis.jvm).Configure(_.settings(Seq(
      resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots",
      libraryDependencies += "com.outr" %% "scribe" % versions.scribe,
      Compile / unmanagedClasspath ++= {
        val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
        (apiDirs ++ androidDirs).map(Attributed.blank)
      },
      Test / unmanagedClasspath ++= {
        val apiDirs     = (`sge-jvm-platform-api`.jvm(versions.scala3) / Compile / products).value
        val androidDirs = (`sge-jvm-platform-android`.jvm(versions.scala3) / Compile / products).value
        (apiDirs ++ androidDirs).map(Attributed.blank)
      },
      // Don't pass -XstartOnFirstThread — the test launches a subprocess with that flag
      Test / javaOptions := {
        val rustLib = ((ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release").getAbsolutePath
        Seq(s"-Djava.library.path=$rustLib", "--enable-native-access=ALL-UNNAMED")
      }
    ) *)),
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .dependsOn(sge, `sge-freetype`, `sge-physics`)

val `sge-it-jvm-platform` = (projectMatrix in file("sge-test/it-jvm-platform"))
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-jvm-platform") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    libraryDependencies += "com.kubuszok" %% "multiarch-panama-jdk" % "0.2.0"
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-browser") ++ dev.only1VersionInIDE ++ Seq(
    MatrixAction.ForAll.Configure(_.settings(SgePlugin.relaxedSettings *))
  )) *)
  .settings(noPublishSettings)
  .settings(mimaSettings)
  .settings(
    Test / baseDirectory := (ThisBuild / baseDirectory).value,
    libraryDependencies += "com.microsoft.playwright" % "playwright" % "1.60.0" % Test,
    Test / resourceGenerators += Def.task {
      val jsDir = (sge.js(versions.scala3) / Compile / fullClasspath).value
      Seq.empty[File]
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
  .defaultAxes(VirtualAxis.jvm, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((commonSettings("sge-test/it-android") ++ dev.only1VersionInIDE ++ Seq(
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
  .defaultAxes(VirtualAxis.native, VirtualAxis.scalaABIVersion(versions.scala3))
  .someVariations(versions.scalas, List(VirtualAxis.native))((commonSettings("sge-test/it-native-ffi") ++ dev.only1VersionInIDE ++ Seq(
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
  // Integration tests
  .aggregate(`sge-it-desktop`.projectRefs *)
  .aggregate(`sge-it-jvm-platform`.projectRefs *)
  .aggregate(`sge-it-browser`.projectRefs *)
  .aggregate(`sge-it-android`.projectRefs *)
  .aggregate(`sge-it-native-ffi`.projectRefs *)
  .settings(
    name := "sge-root",
    logo :=
      s"""SGE ${version.value} for Scala ${versions.scala3} x (Scala JVM, Scala.js $scalaJSVersion, Scala Native $nativeVersion)
         |
         |This build uses sbt-projectmatrix:
         | - Scala JVM adds no suffix to a project name seen in build.sbt
         | - Scala.js adds the "JS" suffix to a project name seen in build.sbt
         | - Scala Native adds the "Native" suffix to a project name seen in build.sbt
         |
         |When working with IntelliJ or Scala Metals, edit dev.properties to control which platform you're currently working with.
         |
         |Library depends on artifacts developed in:
         | - https://github.com/kubuszok/lls
         | - https://github.com/kubuszok/sge-native-providers
         |When working with them, it might be necessary to create PRs and test the SNAPSHOTs published before merging all changes.
         |""".stripMargin,
    usefulTasks := al.usefulTasks(extra = Seq(
      UsefulTask("scalafmtAll", "Format all sources").noAlias
    ))
  )
  .settings(noPublishSettings)
  .settings(mimaSettings)
