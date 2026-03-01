# Platform Targets

SGE targets four compilation platforms via Scala's cross-compilation ecosystem.

## Target Matrix

| Target | Strategy | Reference Backend | Status |
|--------|----------|-------------------|--------|
| Core | Direct port, platform-agnostic | `libgdx/gdx/` | Complete (539/605 files) |
| JVM | Port LWJGL3 backend | `libgdx/backends/gdx-backend-lwjgl3/` | Architecture validated |
| Scala.js | Reverse-engineer GWT backend, use scalajs-dom | `libgdx/backends/gdx-backends-gwt/` | Architecture validated |
| Scala Native | Same C libs as LWJGL, new Scala Native bindings | No direct reference | Architecture validated |

**Validation status (2026-03-01):** All three platform backends compile and run
correctly with the `hello-world/` prototype. The same user application code
produces identical behavior on JVM, Scala.js (Node.js), and Scala Native (binary).
See [platform-validation.md](platform-validation.md) for full results.

## Core (Platform-Agnostic)

The `core` module contains all platform-independent game logic. The core port is
complete: 539 of 605 files converted, 66 skipped (stdlib replacements).

**Source**: `libgdx/gdx/src/com/badlogic/gdx/` → `core/src/main/scala/sge/`

All APIs in core must be expressible without platform-specific dependencies. Platform backends
implement traits defined in core (`Graphics`, `Audio`, `Input`, `Files`, `Net`).

SGE replaces LibGDX's global `Gdx` singleton with Scala 3 context parameters.
User code receives platform services via `(using app: Application)` rather than
accessing mutable global state. See [platform-validation.md](platform-validation.md)
for the validated pattern.

## JVM (LWJGL3)

The primary desktop target. LibGDX uses LWJGL3 which wraps:
- **GLFW** — windowing and input
- **OpenGL** — graphics rendering
- **OpenAL** — audio
- **stb** — image loading, font rasterization

The JVM backend is the most straightforward port since LWJGL3 is a Java library
accessible directly from Scala.

**Validated (2026-03-01):** JVM backend compiles and runs with `sbt 'desktopJvm/run'`.
Pure Scala NativeOps produces correct results. `fork := true` required for JNI.
See [cross-platform-settings.md](cross-platform-settings.md) for LWJGL dependency
configuration.

## Scala.js (Browser)

Browser target using the WebGL and Web Audio APIs.

LibGDX's GWT backend (`gdx-backends-gwt`) is the reference but requires significant
rethinking since:
- GWT compiles Java to JavaScript; Scala.js compiles Scala to JavaScript directly
- GWT has an `emu/` directory for stdlib emulation that is not needed
- WebGL API access via `scalajs-dom` instead of GWT's JSNI

Key considerations:
- `scalajs-dom` provides typed access to WebGL, Web Audio, DOM
- Asset loading via `XMLHttpRequest` / `fetch`
- No filesystem access (use IndexedDB for persistence)
- Single-threaded (no `Thread`, use `Future`/`Promise`)
- WASM backend available as zero-code-change optimization (30% faster numeric, experimental)

**Validated (2026-03-01):** Scala.js backend compiles, links via fastLinkJS, and runs
via Node.js with `sbt 'browser/run'`. Pure Scala NativeOps produces correct results.
Requires sbt-scalajs 1.20.0+ to match Scala 3.8.2 compiler expectations.
See [cross-platform-settings.md](cross-platform-settings.md) for configuration and
[research/findings/03-scalajs-wasm.md](../../research/findings/03-scalajs-wasm.md)
for WASM evaluation.

## Scala Native

Native compilation target for desktop/embedded without JVM overhead.

No direct LibGDX reference backend exists. Strategy:
- Use the same C libraries as LWJGL (GLFW, OpenGL, OpenAL, stb) via Scala Native C interop
- Write bindings using `@extern` and `CStruct`
- Memory management via `Zone` allocators

Key considerations:
- No JNI — direct C FFI via `@extern` + `@link` annotations
- No garbage collector pressure for native buffers
- Binary distribution without JVM dependency
- `Zone { ... }` for scoped allocation (0.5.x context function API)
- @extern bindings are ~40% less code than JNI bridges (52 LoC vs ~80-100)
- Library linking resolved at compile time (not runtime like JNI)

**Validated (2026-03-01):** Scala Native backend compiles, links (909 classes →
498 after optimization), and executes as standalone binary. Pure Scala NativeOps
produces correct results. The binary runs without JVM dependency.
See [cross-platform-settings.md](cross-platform-settings.md) for @extern FFI
configuration and [research/findings/02-jni-vs-scala-native.md](../../research/findings/02-jni-vs-scala-native.md)
for the JNI vs @extern comparison.
