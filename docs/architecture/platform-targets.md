# Platform Targets

SGE targets four compilation platforms via Scala's cross-compilation ecosystem.

## Target Matrix

| Target | Strategy | Reference Backend | Status |
|--------|----------|-------------------|--------|
| Core | Direct port, platform-agnostic | `libgdx/gdx/` | Complete (539/605 files) |
| JVM | GLFW + Rust FFI via Panama FFM, ANGLE GL ES | `libgdx/backends/gdx-backend-lwjgl3/` | Complete |
| Scala.js | WebGL/WebGL2 via scalajs-dom | `libgdx/backends/gdx-backends-gwt/` | Complete |
| Scala Native | GLFW + Rust FFI via C ABI, ANGLE GL ES | No direct reference | Complete |
| Android | Android SDK + PanamaPort FFM | `libgdx/backends/gdx-backend-android/` | Complete |
| iOS | Scala Native + SDL3 + ANGLE | `libgdx/backends/gdx-backend-robovm/` | Deferred |

**All platforms passing**: JVM 1450 tests, JS 1096 tests, Native 1096 tests.
10 demo applications build and run on all 4 active platforms.

## Core (Platform-Agnostic)

The `sge` module contains all platform-independent game logic as a `projectMatrix`
(JVM/JS/Native). The core port is complete: 539 of 605 files converted, 66 skipped
(stdlib replacements).

**Source**: `libgdx/gdx/src/com/badlogic/gdx/` → `sge/src/main/scala/sge/`

All APIs in core must be expressible without platform-specific dependencies. Platform backends
implement traits defined in core (`Graphics`, `Audio`, `Input`, `Files`, `Net`).
See [sge-core-interfaces.md](sge-core-interfaces.md) for the full trait inventory.

SGE replaces LibGDX's global `Gdx` singleton with Scala 3 context parameters.
User code receives platform services via `(using Sge)` on class constructors
rather than accessing mutable global state.

## JVM (Desktop)

The primary desktop target. SGE uses:
- **GLFW** — windowing and input (vendored in `native-components/`)
- **ANGLE** — OpenGL ES via `libEGL` + `libGLESv2` (pre-built from sge-angle-natives)
- **miniaudio** — audio (vendored in `native-components/`)
- **Rust FFI via Panama FFM** — JDK 23+, `java.lang.foreign` downcall handles

Platform-specific sources: `sge/src/main/scalajvm/sge/platform/`
Desktop-shared sources (JVM + Native): `sge/src/main/scaladesktop/sge/platform/`

## Scala.js (Browser)

Browser target using WebGL/WebGL2 and Web Audio APIs via `scalajs-dom`.

Platform-specific sources: `sge/src/main/scalajs/sge/platform/`

Key characteristics:
- `scalajs-dom` provides typed access to WebGL, Web Audio, DOM
- Asset loading via `fetch`; persistence via IndexedDB
- Single-threaded (no `Thread`, uses `setTimeout` callbacks)
- WASM backend available as zero-code-change optimization (see [wasm-strategy.md](wasm-strategy.md))

## Scala Native (Desktop)

Native compilation target sharing the same C libraries as JVM via Rust C ABI:
- **GLFW** + **ANGLE** + **miniaudio** (same as JVM, via `@extern` instead of Panama)
- Binary distribution without JVM dependency
- Static curl bundled for self-contained sttp HTTP (see `sge-dev native curl setup`)

Platform-specific sources: `sge/src/main/scalanative/sge/platform/`

## Android

Android target using system GL ES with PanamaPort for FFM on ART:
- API 36+ minimum (PanamaPort constraint)
- PanamaPort (com.v7878.foreign) provides Panama FFM on ART — same C ABI as desktop, no JNI
- Audio via miniaudio (OpenSL ES backend)

See [android-native-constraints.md](android-native-constraints.md) for details.

## iOS (Deferred)

Deferred pending Scala Native iOS runtime fixes. See
[ios-backend-feasibility.md](ios-backend-feasibility.md) for the full analysis.
