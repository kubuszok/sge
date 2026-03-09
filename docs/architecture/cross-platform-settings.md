# Cross-Platform Settings Reference

Complete reference for configuring SGE's multi-platform sbt build. All settings
documented here have been **validated in practice** via the `hello-world/`
prototype (2026-03-01) and the `research/` prototype (2026-02-28).

## Validated Toolchain Versions

| Tool | Version | Notes |
|------|---------|-------|
| **Scala** | 3.8.2 | Requires JDK 17+; emits Java 17 class files (version 61) |
| **sbt** | 1.10.7 | Thin client mode via `sbt --client` |
| **sbt-projectmatrix** | 0.11.0 | Cross-platform project definition; will be built-in with sbt 2.0 |
| **sbt-scalajs** | 1.20.2 | Scala.js compiler + linker; must match Scala version expectations |
| **sbt-scala-native** | 0.5.10 | Scala Native compiler + linker; Zone API uses context functions |
| **JDK** | 21.0.6 | Tested; any JDK 17+ should work |
| **Node.js** | 22+ | Required for `sbt browser/run`; 23+ for WASM flags |

### Version Compatibility Constraints

**Scala 3.8.2 requires sbt-scalajs 1.20.0+.** The Scala 3.8.2 compiler backend
emits Scala.js IR that references runtime library features (e.g., `js.async`)
introduced in Scala.js 1.20.x. Using older sbt-scalajs versions (1.17.x, 1.18.x)
causes a compiler crash:

```
dotty.tools.dotc.core.TypeError: package scala.scalajs.js does not have a member method async
```

**Scala Native 0.5.x changed the Zone API.** The `Zone.apply` method now accepts
a context function `(Zone) ?=> A` instead of `Zone => A`. Code must use:

```scala
// Correct (0.5.x) — Zone is a given, not an explicit parameter
Zone {
  val ptr = alloc[Float](len)
  // ...
}

// Wrong (0.4.x pattern) — fails with "Missing parameter type"
Zone { implicit z =>
  val ptr = alloc[Float](len)
}
```

## Plugin Configuration

### project/plugins.sbt

```scala
addSbtPlugin("com.eed3si9n"      % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"       % "1.20.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native"  % "0.5.10")
```

### project/build.properties

```
sbt.version=1.10.7
```

## build.sbt Structure

### Shared settings

```scala
val scala3 = "3.8.2"

val commonSettings = Seq(
  scalaVersion := scala3,
  scalacOptions ++= Seq("-deprecation", "-feature", "-no-indent")
)
```

For the full SGE build, add the full compiler flag set:

```scala
val sgeSettings = Seq(
  scalaVersion := scala3,
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-no-indent",
    "-rewrite",
    "-Werror",
    "-Wimplausible-patterns",
    "-Wrecurse-with-default",
    "-Wenum-comment-discard",
    "-Wunused:imports,privates,locals,patvars,nowarn"
  )
)
```

### Core module (projectMatrix)

```scala
lazy val core = (projectMatrix in file("core"))
  .settings(commonSettings)
  .settings(name := "sge")
  .jvmPlatform(scalaVersions = Seq(scala3))
  .jsPlatform(scalaVersions = Seq(scala3))
  .nativePlatform(scalaVersions = Seq(scala3))
```

**How it works:**

- `projectMatrix in file("core")` uses `core/src/main/scala/` as the shared
  source directory for all platforms
- `.jvmPlatform()` generates subproject `core3` (JVM has empty ID suffix)
- `.jsPlatform()` generates subproject `coreJS3`
- `.nativePlatform()` generates subproject `coreNative3`
- The `3` suffix is the Scala binary version (3.8.2 → binary `3`)

### App module (projectMatrix, depends on core)

```scala
lazy val app = (projectMatrix in file("app"))
  .settings(commonSettings)
  .settings(name := "sge-app")
  .dependsOn(core)
  .jvmPlatform(scalaVersions = Seq(scala3))
  .jsPlatform(scalaVersions = Seq(scala3))
  .nativePlatform(scalaVersions = Seq(scala3))
```

When one `projectMatrix` depends on another via `.dependsOn(core)`, sbt
automatically matches platform axes: the JVM variant of `app` depends on the
JVM variant of `core`, the JS variant depends on the JS variant, etc.

### JVM backend (regular project)

```scala
lazy val desktopJvm = project.in(file("desktop-jvm"))
  .settings(commonSettings)
  .settings(
    name := "sge-desktop-jvm",
    Compile / mainClass := Some("sge.backend.jvm.Main"),
    fork := true  // Required for System.loadLibrary (JNI) and proper stdout
  )
  .dependsOn(app.jvm(scala3))
```

**Key settings:**

- `fork := true` — required for JNI (`System.loadLibrary`) and ensures stdout
  is properly forwarded through sbt
- `.dependsOn(app.jvm(scala3))` — selects the JVM variant of the `app`
  projectMatrix. The argument must be the exact Scala version string.

### Scala.js backend (ScalaJSPlugin)

```scala
lazy val browser = project.in(file("browser"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)
  .settings(
    name := "sge-browser",
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("sge.backend.js.Main")
  )
  .dependsOn(app.js(scala3))
```

**Key settings:**

- `.enablePlugins(ScalaJSPlugin)` — required for Scala.js compilation
- `scalaJSUseMainModuleInitializer := true` — generates a `main()` call in the
  output JS so it runs when loaded
- `.dependsOn(app.js(scala3))` — selects the JS variant

**Commands:**

| Command | Purpose |
|---------|---------|
| `browser/compile` | Compile Scala to Scala.js IR |
| `browser/fastLinkJS` | Link IR to JavaScript (dev mode, fast) |
| `browser/fullLinkJS` | Link IR to JavaScript (prod mode, optimized) |
| `browser/run` | fastLinkJS + execute via Node.js |

### Scala Native backend (ScalaNativePlugin)

```scala
lazy val desktopNative = project.in(file("desktop-native"))
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings)
  .settings(
    name := "sge-desktop-native",
    Compile / mainClass := Some("sge.backend.native.Main")
  )
  .dependsOn(app.native(scala3))
```

**Key settings:**

- `.enablePlugins(ScalaNativePlugin)` — required for Scala Native compilation
- `.dependsOn(app.native(scala3))` — selects the Native variant

**Commands:**

| Command | Purpose |
|---------|---------|
| `desktopNative/compile` | Compile Scala to NIR |
| `desktopNative/nativeLink` | Link NIR to native binary via LLVM |
| `desktopNative/run` | nativeLink + execute binary |

**Known issue:** `sbt --client 'desktopNative/run'` executes the binary but
stdout goes to the sbt server process, not the thin client terminal. Use
`sbt --client 'desktopNative/nativeLink'` then run the binary directly:

```bash
./desktop-native/target/scala-3.8.2/sge-desktop-native
```

## sbt-projectmatrix Project ID Naming

Understanding the generated project IDs is essential for sbt commands and
cross-project dependency wiring.

### ID Formula

```
<matrixName><platformSuffix><scalaBinaryVersion>
```

| Platform | Suffix | Example (matrix = "core") |
|----------|--------|---------------------------|
| JVM | *(empty)* | `core3` |
| Scala.js | `JS` | `coreJS3` |
| Scala Native | `Native` | `coreNative3` |

**The JVM platform has an empty suffix** — this is intentional. JVM is treated as
the "default" platform by sbt-projectmatrix. The binary version `3` comes from
Scala 3.8.2.

### Referencing Variants in build.sbt

```scala
// From a projectMatrix .dependsOn():
lazy val app = (projectMatrix in file("app"))
  .dependsOn(core)  // auto-matches platform axes

// From a regular project .dependsOn():
lazy val desktopJvm = project
  .dependsOn(app.jvm(scala3))     // returns Project for JVM variant
  .dependsOn(app.js(scala3))      // returns Project for JS variant   (unlikely)
  .dependsOn(app.native(scala3))  // returns Project for Native variant (unlikely)
```

**Do NOT use** `app.finder(JSPlatform)(scala3)` — this API does not work with
sbt-projectmatrix 0.10.x/0.11.x. Use `.jvm()`, `.js()`, `.native()` instead.

## Platform-Specific Source Directories

For code that must differ per platform (e.g., FFI bindings), add platform-specific
source directories:

```scala
lazy val core = (projectMatrix in file("core"))
  .settings(commonSettings)
  .jvmPlatform(
    scalaVersions = Seq(scala3),
    settings = Seq(
      Compile / unmanagedSourceDirectories +=
        baseDirectory.value / "src" / "main" / "scala-jvm",
      // Java sources for JNI bridge files
      Compile / unmanagedSourceDirectories +=
        baseDirectory.value / "src" / "main" / "java"
    )
  )
  .jsPlatform(
    scalaVersions = Seq(scala3),
    settings = Seq(
      Compile / unmanagedSourceDirectories +=
        baseDirectory.value / "src" / "main" / "scala-js"
    )
  )
  .nativePlatform(
    scalaVersions = Seq(scala3),
    settings = Seq(
      Compile / unmanagedSourceDirectories +=
        baseDirectory.value / "src" / "main" / "scala-native"
    )
  )
```

**Resulting directory layout:**

```
core/
├── src/main/scala/sge/          # Shared (all platforms)
├── src/main/scala-jvm/sge/     # JVM-only (LWJGL, JNI)
├── src/main/scala-js/sge/      # JS-only (scala-js-dom facades)
├── src/main/scala-native/sge/  # Native-only (@extern, Zone)
└── src/main/java/               # JNI bridge files (JVM only)
```

## Scala.js WASM Configuration (Optional, Experimental)

To enable the WebAssembly backend for Scala.js (zero code changes required):

```scala
lazy val browser = project.in(file("browser"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSLinkerConfig := {
      scalaJSLinkerConfig.value
        .withExperimentalUseWebAssembly(true)
        .withModuleKind(ModuleKind.ESModule)
    },
    // Node.js 23+ required with experimental flags
    jsEnv := {
      val config = NodeJSEnv.Config()
        .withArgs(List(
          "--experimental-wasm-exnref",
          "--experimental-wasm-jspi",
          "--experimental-wasm-imported-strings",
          "--turboshaft-wasm"
        ))
      new NodeJSEnv(config)
    }
  )
```

**Constraints:**

- `ModuleKind.ESModule` is mandatory (no CommonJS)
- `ModuleSplitStyle.FewestModules` only
- Not incremental (slower dev cycle)
- Browser requirements: Firefox 131+, Safari 18.4+, Chrome 137+
- ~30% faster for numeric computation, up to 5.6x for hash/crypto
- `@JSExport` silently ignored (only `@JSExportTopLevel` works)

See [research/findings/03-scalajs-wasm.md](../../research/findings/03-scalajs-wasm.md)
for full evaluation.

## Scala Native FFI Configuration

### Linking to C libraries

```scala
lazy val desktopNative = project.in(file("desktop-native"))
  .enablePlugins(ScalaNativePlugin)
  .settings(
    nativeLinkingOptions ++= Seq(
      "-L/usr/local/lib",           // library search path
      "-L" + baseDirectory.value / "lib"  // project-local libs
    )
  )
```

### @extern FFI pattern

```scala
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@link("vecmath")  // links -lvecmath at link time
@extern
object VecMathC {
  def vec_dot(a: Ptr[Float], b: Ptr[Float], len: CInt): Float = extern
}

// Wrapper: converts Scala arrays to native pointers
object VecMathNative {
  def dot(a: Array[Float], b: Array[Float]): Float = {
    Zone {
      val pa = alloc[Float](a.length)
      val pb = alloc[Float](b.length)
      var i = 0
      while (i < a.length) { pa(i) = a(i); pb(i) = b(i); i += 1 }
      VecMathC.vec_dot(pa, pb, a.length)
    }
  }
}
```

**Key points:**

- `@link("name")` adds `-lname` to the native linker invocation
- Missing libraries fail at **link time** (not runtime) — better than JNI
- `Zone { ... }` provides scoped allocation (memory freed at block exit)
- Array data must be **copied** into native memory (no zero-copy for dynamic arrays)
- Use `stackalloc` instead of `alloc` for hot-path code to avoid heap allocation

### Type mapping (C → Scala Native)

| C Type | Scala Native | Notes |
|--------|-------------|-------|
| `float*` | `Ptr[Float]` | `scala.scalanative.unsafe.Ptr` |
| `int` | `CInt` | `scala.scalanative.unsafe.CInt` |
| `float` | `Float` | Direct mapping |
| `void` | `Unit` | Direct mapping |
| `const char*` | `CString` | `scala.scalanative.unsafe.CString` |
| `size_t` | `CSize` | `scala.scalanative.unsigned.CSize` |

## JNI Bridge Pattern

For JVM-specific native code (LWJGL, custom Rust libraries):

### Java bridge file

```java
// core/src/main/java/com/badlogic/gdx/utils/BufferUtils.java
package com.badlogic.gdx.utils;

public class BufferUtils {
    static { System.loadLibrary("gdx"); }
    public static native void clear(java.nio.Buffer buffer, int numBytes);
    public static native void copy(float[] src, java.nio.Buffer dst, int numFloats, int offset);
}
```

### Scala wrapper

```scala
// core/src/main/scala-jvm/sge/utils/BufferUtils.scala
package sge.utils

import com.badlogic.gdx.utils.{BufferUtils => BufferUtilsJni}

object BufferUtils {
  def clear(buffer: java.nio.Buffer, numBytes: Int): Unit =
    BufferUtilsJni.clear(buffer, numBytes)
}
```

**Convention:** Import with alias (`{ClassName => ClassNameJni}`) to avoid name
collisions between the Java bridge and the Scala wrapper.

## sbt Command Reference

### Multi-command syntax

```bash
# Correct — semicolons inside single argument
sbt --client ';desktopJvm/compile;browser/fastLinkJS;desktopNative/compile'

# Wrong — separate arguments fail with thin client
sbt --client 'cmd1' 'cmd2'  # second command ignored
```

### Compilation commands

| Command | Platform | What it does |
|---------|----------|-------------|
| `core3/compile` | JVM | Compile shared core for JVM |
| `coreJS3/compile` | JS | Compile shared core for Scala.js |
| `coreNative3/compile` | Native | Compile shared core for Scala Native |
| `desktopJvm/compile` | JVM | Compile JVM backend (triggers core3 + app3) |
| `browser/compile` | JS | Compile JS backend (triggers coreJS3 + appJS3) |
| `browser/fastLinkJS` | JS | Compile + link JavaScript output |
| `browser/fullLinkJS` | JS | Compile + link optimized JavaScript |
| `desktopNative/compile` | Native | Compile Native backend |
| `desktopNative/nativeLink` | Native | Compile + link native binary |

### Run commands

| Command | Platform | Notes |
|---------|----------|-------|
| `desktopJvm/run` | JVM | Runs in forked JVM; stdout forwarded |
| `browser/run` | JS | fastLinkJS + Node.js execution |
| `desktopNative/run` | Native | nativeLink + binary execution; stdout via sbt server |

## Dependency Configuration for Real Backends

### JVM Desktop (LWJGL)

```scala
val lwjglVersion = "3.3.6"
val lwjglNatives = {
  val os = System.getProperty("os.name").toLowerCase
  val arch = System.getProperty("os.arch")
  if (os.contains("mac")) {
    if (arch == "aarch64") "natives-macos-arm64" else "natives-macos"
  } else if (os.contains("linux")) {
    if (arch == "aarch64") "natives-linux-arm64" else "natives-linux"
  } else "natives-windows"
}

lazy val desktopJvm = project.in(file("desktop-jvm"))
  .settings(
    libraryDependencies ++= Seq(
      "org.lwjgl" % "lwjgl"        % lwjglVersion,
      "org.lwjgl" % "lwjgl-glfw"   % lwjglVersion,
      "org.lwjgl" % "lwjgl-opengl" % lwjglVersion,
      "org.lwjgl" % "lwjgl-openal" % lwjglVersion,
      "org.lwjgl" % "lwjgl-stb"    % lwjglVersion,
      "org.lwjgl" % "lwjgl"        % lwjglVersion classifier lwjglNatives,
      "org.lwjgl" % "lwjgl-glfw"   % lwjglVersion classifier lwjglNatives,
      "org.lwjgl" % "lwjgl-opengl" % lwjglVersion classifier lwjglNatives,
      "org.lwjgl" % "lwjgl-openal" % lwjglVersion classifier lwjglNatives,
      "org.lwjgl" % "lwjgl-stb"    % lwjglVersion classifier lwjglNatives
    )
  )
```

### Scala.js Browser (scala-js-dom)

```scala
lazy val browser = project.in(file("browser"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0"
  )
```

### Scala Native Desktop (system libraries via @extern)

No sbt library dependencies needed for system C libraries (GLFW, OpenGL, OpenAL).
Link via `nativeLinkingOptions` and `@link` annotations:

```scala
// build.sbt
nativeLinkingOptions ++= Seq(
  "-framework", "OpenGL",    // macOS
  "-framework", "Cocoa",     // macOS
  "-lglfw"                   // GLFW
)
```

## Gotchas and Known Issues

### 1. sbt thin client + Scala Native stdout

`sbt --client 'desktopNative/run'` compiles and runs successfully but stdout
from the native binary is not forwarded to the thin client terminal. The output
goes to the sbt server's console instead. Workaround: run the binary directly
after linking.

### 2. sbt-projectmatrix JVM project ID has no "JVM" suffix

The JVM `VirtualAxis` has an empty `idSuffix`. A matrix named `core` produces
`core3` for JVM (not `coreJVM3`), `coreJS3` for JS, `coreNative3` for Native.

### 3. Scala.js version must match Scala compiler

Scala 3.8.2 ships with `scala3-library_sjs1_3-3.8.2.jar` that depends on
Scala.js 1.20.x internals. Using sbt-scalajs < 1.20.0 causes a compiler crash.
Always match the sbt-scalajs version to what the Scala compiler expects.

### 4. ModuleKind for Scala.js

`scalaJSUseMainModuleInitializer := true` requires a module kind that supports
top-level initialization. The default (`NoModule`) works for simple cases. If
you add WASM support or need ESModule imports, set `ModuleKind.ESModule`
explicitly. `ModuleKind.CommonJSModule` also works for Node.js-only targets.

### 5. fork := true for JVM backends

Always set `fork := true` for JVM backend projects that will use JNI. Without
forking, `System.loadLibrary` searches the sbt process's library path, not the
project's. Forking also ensures clean JVM state and proper stdout forwarding.

### 6. Scala Native Zone API (0.5.x)

`Zone { ... }` uses Scala 3 context functions. The `Zone` instance is available
as a `given` inside the block — do not write `Zone { implicit z => ... }`.

### 7. sbt multiple command syntax

With `sbt --client`, use semicolons inside a single quoted argument:

```bash
sbt --client ';cmd1;cmd2;cmd3'    # Correct
sbt --client 'cmd1' 'cmd2'       # Wrong — only first command runs
```

### 8. Scala.js Float display

JavaScript's `Number` type does not distinguish integer and float
representations. `70.0f.toString` produces `"70"` in Scala.js vs `"70.0"` on
JVM and Native. This is cosmetic — the values are identical.

## Migration Path from Current Single-Project Build

See [build-structure.md](build-structure.md) for the detailed migration plan.
Summary:

1. Add sbt-projectmatrix, sbt-scalajs, sbt-scala-native to `project/plugins.sbt`
2. Convert `core` from `project` to `projectMatrix` with `.jvmPlatform()` only
3. Identify platform-specific code (JNI calls, `java.nio`, threading)
4. Create `src/main/scala-jvm/` for JVM-specific code
5. Add `.jsPlatform()` and `.nativePlatform()` with platform-specific source dirs
6. Create backend projects: `backend-lwjgl3/`, `backend-webgl/`, `backend-native/`
7. Verify: `sbt ';core3/compile;coreJS3/compile;coreNative3/compile'`
