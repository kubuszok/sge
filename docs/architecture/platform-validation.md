# Platform Validation (Historical)

> **Note (2026-03-20):** This document records the initial architecture validation
> from 2026-03-01. The `hello-world/` prototype has been archived — the architecture
> is now fully implemented in the main `sge/` module with all backends complete and
> 3,642 tests passing across JVM (1450), JS (1096), and Native (1096). The patterns
> validated here (context parameters, NativeOps trait, projectMatrix) are all in
> production use.

Practical validation that SGE's cross-platform architecture works end-to-end.
Conducted 2026-03-01 using the `hello-world/` prototype on the `platform-validation`
branch.

## Purpose

Prove — in practice, not theory — that:

1. A user writes application code using **only** the core API
2. The user pulls in a platform backend (JVM, browser, or native)
3. The user compiles and runs on that platform
4. **The code just works** — identical behavior, zero platform-specific imports in user code
5. Native operations (pixel blending, vector math) can be abstracted behind a trait
   and swapped per platform (pure Scala now, Rust JNI / @extern / WASM later)

## Architecture

```
┌───────────────────────────────────────────────────────┐
│  User Application (demo.HelloWorld)                   │
│  • imports only sge.*                                 │
│  • zero platform-specific code                        │
│  • receives Application via context parameter         │
├───────────────────────────────────────────────────────┤
│  Core API (sge.*)                                     │
│  • ApplicationListener — lifecycle (create/render/    │
│    dispose)                                           │
│  • Application — services (log, appType, nativeOps)   │
│  • ApplicationType — platform enum                    │
│  • NativeOps — native operation trait + pure fallback  │
├──────────┬──────────────┬─────────────────────────────┤
│ JVM      │ Scala.js     │ Scala Native                │
│ Backend  │ Backend      │ Backend                     │
│          │              │                             │
│ LWJGL    │ scala-js-dom │ GLFW via @extern            │
│ JNI      │ WebGL2       │ Rust C ABI                  │
│ Rust JNI │ Rust WASM    │ OpenGL/Vulkan via @extern   │
└──────────┴──────────────┴─────────────────────────────┘
```

### Context Parameter Pattern

SGE replaces LibGDX's global mutable `Gdx` singleton with Scala 3 context
parameters. User code declares `(using app: Application)` and the backend provides
it via `given Application = this`:

```scala
// User code — no global state, no platform imports
class HelloWorld extends ApplicationListener {
  def create()(using app: Application): Unit = {
    app.log("HelloWorld", s"Created on ${app.appType}")
    val blended = app.nativeOps.blend(0xff0000, 0x0000ff, 0.5f)
    app.log("HelloWorld", s"Blended: 0x${blended.toHexString}")
  }
  // ...
}

// Backend code — provides the Application implementation
class DesktopJvmApplication(listener: ApplicationListener) extends Application {
  def run(): Unit = {
    given Application = this  // inject into user code
    listener.create()
    listener.render()
    listener.dispose()
  }
  // ...
}
```

Benefits over LibGDX's `Gdx.*` globals:

- **Thread-safe by construction** — no shared mutable state
- **Testable** — mock Application in unit tests without singletons
- **Multi-instance safe** — multiple Application instances can coexist
- **Explicit dependencies** — compiler enforces that Application is available

### NativeOps Abstraction

The `NativeOps` trait abstracts operations that LibGDX implements in C
(`gdx2d.c`, `BufferUtils`, `ETC1`). Each backend provides an implementation:

| Platform | NativeOps implementation | Notes |
|----------|------------------------|-------|
| JVM | `NativeOps.pure` (now) → Rust JNI (later) | LWJGL provides many native ops already |
| Scala.js | `NativeOps.pure` (now) → Rust WASM (later) | WebGL handles GPU-side ops |
| Scala Native | `NativeOps.pure` (now) → Rust @extern (later) | Direct C ABI, zero overhead |

The pure Scala fallback is always available as a baseline. Platform-specific
implementations can be swapped in when performance demands it, without changing
any user code or core API code.

## Validation Results

### Platform Matrix

| Platform | Compilation | Linking | Execution | Output Correct |
|----------|:-----------:|:-------:|:---------:|:--------------:|
| **JVM Desktop** | PASS | N/A (bytecode) | PASS | PASS |
| **Scala.js Browser** | PASS | PASS (fastLinkJS) | PASS (Node.js) | PASS |
| **Scala Native Desktop** | PASS | PASS (nativeLink) | PASS (binary) | PASS |

### Actual Output

**JVM Desktop** (`sbt 'desktopJvm/run'`):

```
[main] [SGE] Starting on DesktopJvm (JVM 21.0.6)
[main] [HelloWorld] Created on DesktopJvm
[main] [HelloWorld] Engine API is available — no platform imports needed
[main] [HelloWorld] NativeOps.blend(red, blue, 0.5) = 0x7f007f
[main] [HelloWorld] NativeOps.dot([1,2,3,4], [5,6,7,8]) = 70.0
[main] [HelloWorld] render() frame on DesktopJvm
[main] [HelloWorld] render() frame on DesktopJvm
[main] [HelloWorld] render() frame on DesktopJvm
[main] [HelloWorld] Goodbye!
[main] [SGE] Application terminated
```

**Scala.js Browser** (`sbt 'browser/run'`):

```
[SGE] Starting on Browser (Scala.js)
[HelloWorld] Created on Browser
[HelloWorld] Engine API is available — no platform imports needed
[HelloWorld] NativeOps.blend(red, blue, 0.5) = 0x7f007f
[HelloWorld] NativeOps.dot([1,2,3,4], [5,6,7,8]) = 70
[HelloWorld] render() frame on Browser
[HelloWorld] render() frame on Browser
[HelloWorld] render() frame on Browser
[HelloWorld] Goodbye!
[SGE] Application terminated
```

**Scala Native Desktop** (direct binary execution):

```
[SGE] Starting on DesktopNative (Scala Native)
[HelloWorld] Created on DesktopNative
[HelloWorld] Engine API is available — no platform imports needed
[HelloWorld] NativeOps.blend(red, blue, 0.5) = 0x7f007f
[HelloWorld] NativeOps.dot([1,2,3,4], [5,6,7,8]) = 70.0
[HelloWorld] render() frame on DesktopNative
[HelloWorld] render() frame on DesktopNative
[HelloWorld] render() frame on DesktopNative
[HelloWorld] Goodbye!
[SGE] Application terminated
```

All three platforms produce identical behavior. The `NativeOps.pure` implementation
computes correct results:

- `blend(0xff0000, 0x0000ff, 0.5)` = `0x7f007f` (midpoint of red and blue)
- `dot([1,2,3,4], [5,6,7,8])` = `70.0` (1*5 + 2*6 + 3*7 + 4*8)

Note: Scala.js displays `70` instead of `70.0` because JavaScript's `Number`
type does not distinguish integer and float representations. This is cosmetic
only — the computed value is identical.

### Linking Statistics (Scala Native)

```
Discovered 909 classes and 5584 methods after classloading
Discovered 498 classes and 2027 methods after optimization
```

The optimizer eliminates ~45% of classes and ~64% of methods, producing a lean
native binary from the same Scala source code that runs on JVM and browser.

## Project Structure

```
hello-world/
├── build.sbt                                          # 9 sbt subprojects
├── project/
│   ├── build.properties                               # sbt 1.10.7
│   └── plugins.sbt                                    # projectmatrix + scalajs + native
├── core/src/main/scala/sge/                           # Shared core API
│   ├── Application.scala                              # Platform services trait
│   ├── ApplicationListener.scala                      # User lifecycle trait
│   ├── ApplicationType.scala                          # Platform enum
│   └── NativeOps.scala                                # Native ops trait + pure impl
├── app/src/main/scala/demo/
│   └── HelloWorld.scala                               # User app (ZERO platform imports)
├── desktop-jvm/src/main/scala/sge/backend/jvm/
│   ├── DesktopJvmApplication.scala                    # JVM Application impl
│   └── Main.scala                                     # JVM entry point
├── browser/src/main/scala/sge/backend/js/
│   ├── BrowserApplication.scala                       # Scala.js Application impl
│   └── Main.scala                                     # JS entry point
└── desktop-native/src/main/scala/sge/backend/native/
    ├── NativeApplication.scala                        # Scala Native Application impl
    └── Main.scala                                     # Native entry point
```

### sbt Subproject Graph

```
core (projectMatrix: JVM/JS/Native)
  ↑
app (projectMatrix: JVM/JS/Native)
  ↑                ↑                ↑
desktopJvm     browser         desktopNative
(project)      (ScalaJSPlugin) (ScalaNativePlugin)
```

The `core` and `app` modules use `projectMatrix` to compile the **same source
code** on all three platforms. The backend projects are regular sbt `project`s
that depend on the correct platform variant:

- `desktopJvm.dependsOn(app.jvm(scala3))`
- `browser.dependsOn(app.js(scala3))`
- `desktopNative.dependsOn(app.native(scala3))`

## What This Validates for SGE

### Validated (proven in practice)

1. **Core API compiles identically on JVM, JS, and Native** — same Scala files,
   same semantics, no `#ifdef` or platform splits needed
2. **Context parameters replace global state** — `(using app: Application)` is
   ergonomic and the compiler enforces its availability
3. **Backend injection works** — each platform provides `Application` independently
4. **NativeOps pattern works** — pure Scala fallback is functional; the trait is
   ready for platform-specific implementations (Rust JNI, @extern, WASM)
5. **sbt-projectmatrix works** with Scala 3.8.2 on all three platforms
6. **Regular projects can depend on projectMatrix variants** via `.jvm()`, `.js()`,
   `.native()` selectors

### Not yet validated (future work)

| Gap | What's needed | Priority |
|-----|---------------|----------|
| Android | DEX compilation + Activity lifecycle | P2 |
| iOS (MobiVM) | Scala 3 bytecode → MobiVM AOT compilation | P3 |
| Rust JNI | `System.loadLibrary` + JNI native methods in JVM backend | P2 |
| Rust @extern | `@link("libname")` + C ABI calls in Native backend | P1 |
| Rust WASM | `WebAssembly.instantiate` + JS facade in browser backend | P2 |
| WASM output | `withExperimentalUseWebAssembly(true)` build flag | P2 |
| Real graphics | LWJGL/WebGL/GLFW integration | P0-P1 |
| Real audio | OpenAL/WebAudio integration | P1 |
| Threading | Gears async or platform threads | P1 |
| Asset loading | Platform-specific file I/O | P1 |

### Architecture confidence level

The hello-world prototype validates the **structural architecture**: module
boundaries, dependency injection pattern, cross-compilation, and native ops
abstraction. The remaining work is implementing the actual platform services
(graphics, audio, input, files) behind the same trait interfaces — which is
engineering effort, not architectural risk.

## Relationship to Research Prototype

The `research/` prototype (5 experiments, see [research findings](../../research/findings/))
established theoretical feasibility. The `hello-world/` prototype establishes
practical feasibility:

| Question | Research answer | Hello-world proof |
|----------|----------------|-------------------|
| Does sbt-projectmatrix work? | Yes (Exp 1) | Yes — 9 subprojects compile |
| Can the same code run everywhere? | Yes (Exp 1) | Yes — identical output on 3 platforms |
| Does @extern FFI work? | Yes (Exp 2) | Ready — NativeOps trait in place |
| Does Scala.js work? | Yes (Exp 3) | Yes — fastLinkJS + Node.js execution |
| Can Rust serve all platforms? | Probably (Exp 4) | Ready — NativeOps.pure is swappable |
| iOS feasible? | Probably (Exp 5) | Not yet tested |

## Running the Prototype

```bash
cd hello-world

# JVM Desktop
sbt --client 'desktopJvm/run'

# Scala.js Browser (runs via Node.js)
sbt --client 'browser/run'

# Scala Native Desktop
sbt --client 'desktopNative/nativeLink'
./desktop-native/target/scala-3.8.2/sge-desktop-native

# Compile all (no run)
sbt --client ';desktopJvm/compile;browser/fastLinkJS;desktopNative/compile'
```

Note: `sbt --client 'desktopNative/run'` compiles and executes successfully but
stdout is not forwarded through the sbt thin client. Run the binary directly to
see output, or use `sbt --client 'desktopNative/nativeLink'` + direct execution.
