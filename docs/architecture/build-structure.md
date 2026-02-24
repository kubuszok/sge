# Build Structure

Target SBT multi-project layout using `sbt-projectmatrix` for cross-platform compilation.

## Why sbt-projectmatrix (not sbt-crossproject)

| Aspect | sbt-crossproject (legacy) | sbt-projectmatrix |
|--------|--------------------------|-------------------|
| Cross-building | Sequential (stateful `++` switching) | Parallel (each variant is a real subproject) |
| Extensibility | Platform axis only (JVM/JS/Native) | Arbitrary axes via `VirtualAxis` |
| sbt 2.0 status | Not in-sourced | **Built-in** (in-sourced into sbt 2.x) |
| sbt 1.x support | Yes | Yes (plugin v0.11.0) |
| Maintenance | Deprecated — repo says "use sbt-projectmatrix" | Active; archived as standalone once in-sourced |

The `sbt-projectmatrix` plugin was in-sourced into sbt 2.x, meaning `projectMatrix` becomes
a built-in feature when sbt 2.0 reaches stable release. Using it now on sbt 1.x (via the
plugin) provides a smooth upgrade path.

## sbt 2.0 Migration Status (as of 2026-02)

sbt 2.0 is at **RC9** (Feb 16, 2026). Not yet stable. Key considerations:

| Aspect | Status | Blocker? |
|--------|--------|----------|
| sbt 2.0 stable release | RC9, not final | Wait for 2.0.0 |
| sbt-scalafmt | v2.5.6 compatible with sbt 2.x | No |
| Metals BSP support | PR in development | Possible IDE issues |
| Scala.js sbt plugin | PR pending for sbt 2.x | Yes, for JS target |
| Scala Native sbt plugin | PR pending for sbt 2.x | Yes, for Native target |
| JDK requirement | sbt 2.x requires JDK 17+ | Check CI/dev environments |
| Build DSL | Scala 3 syntax required | Migration effort |

**Recommendation**: Stay on sbt 1.x with `sbt-projectmatrix` plugin (0.11.0) for now.
Migrate to sbt 2.0 after stable release and Scala.js/Native plugin readiness.

## Current Structure

```
build.sbt
core/
  src/main/scala/sge/      # All code lives here currently
```

## Target Structure

```
build.sbt
project/
  plugins.sbt               # sbt-projectmatrix (sbt 1.x), sbt-scalajs, sbt-scala-native
core/                        # Platform-agnostic code (projectMatrix)
  src/main/scala/sge/              # Shared source
  src/main/scala-jvm/sge/          # JVM-specific core extensions
  src/main/scala-js/sge/           # JS-specific core extensions
  src/main/scala-native/sge/       # Native-specific core extensions
backend-lwjgl3/             # JVM desktop backend
  src/main/scala/sge/backend/lwjgl3/
backend-webgl/              # Scala.js browser backend
  src/main/scala/sge/backend/webgl/
backend-native/             # Scala Native backend
  src/main/scala/sge/backend/native/
backend-headless/           # Headless backend (testing)
  src/main/scala/sge/backend/headless/
```

## SBT Configuration Sketch (sbt 1.x + sbt-projectmatrix plugin)

```scala
// project/plugins.sbt
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.scala-native" % "sbt-scala-native" % scalaNativeVersion)
```

```scala
// build.sbt
lazy val core = projectMatrix
  .in(file("core"))
  .settings(
    name := "sge-core",
    scalaVersion := "3.7.1"
  )
  .jvmPlatform(scalaVersions = Seq("3.7.1"))
  .jsPlatform(scalaVersions = Seq("3.7.1"))
  .nativePlatform(scalaVersions = Seq("3.7.1"))

lazy val backendLwjgl3 = project
  .in(file("backend-lwjgl3"))
  .dependsOn(core.jvm("3.7.1"))
  .settings(
    libraryDependencies ++= Seq(
      "org.lwjgl" % "lwjgl" % lwjglVersion,
      "org.lwjgl" % "lwjgl-glfw" % lwjglVersion,
      "org.lwjgl" % "lwjgl-opengl" % lwjglVersion,
      "org.lwjgl" % "lwjgl-openal" % lwjglVersion,
      "org.lwjgl" % "lwjgl-stb" % lwjglVersion
    )
  )

lazy val backendWebgl = project
  .in(file("backend-webgl"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(core.js("3.7.1"))
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalajsDomVersion
  )

lazy val backendNative = project
  .in(file("backend-native"))
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(core.native("3.7.1"))

lazy val backendHeadless = project
  .in(file("backend-headless"))
  .dependsOn(core.jvm("3.7.1"))
```

## SBT 2.0 Equivalent (future, once stable)

In sbt 2.x, `projectMatrix` is built-in — no plugin needed:

```scala
// build.sbt (sbt 2.x syntax)
lazy val core = projectMatrix
  .in(file("core"))
  .settings(
    name := "sge-core"
  )
  .jvmPlatform(scalaVersions = Seq("3.7.1"))
  .jsPlatform(scalaVersions = Seq("3.7.1"))
  .nativePlatform(scalaVersions = Seq("3.7.1"))
```

## Migration Path

The transition from single-project to cross-project should happen after the core port
is substantially complete:

1. Add `sbt-projectmatrix` plugin (0.11.0) to `project/plugins.sbt`
2. Convert `core` from `project` to `projectMatrix` with `.jvmPlatform()` only (initially)
3. Identify platform-specific code (JNI calls, `java.nio`, threading)
4. Create platform-specific source directories for those files
5. Add backend projects one at a time (headless first for testing)
6. Add `.jsPlatform()` and `.nativePlatform()` when those backends are ready
7. When sbt 2.0 is stable: remove plugin, update `build.sbt` syntax, bump sbt version

## Key Dependencies by Platform

| Platform | Graphics | Audio | Windowing | Image Loading |
|----------|----------|-------|-----------|---------------|
| JVM | LWJGL3/OpenGL | LWJGL3/OpenAL | LWJGL3/GLFW | stb_image via LWJGL |
| Scala.js | WebGL | Web Audio API | DOM/Canvas | HTMLImageElement |
| Scala Native | OpenGL (C) | OpenAL (C) | GLFW (C) | stb_image (C) |
| Headless | No-op stubs | No-op stubs | No-op stubs | No-op stubs |
