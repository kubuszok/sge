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
11 demo applications build and run on all 4 active platforms.

## Withdrawn (for now): macos-x86_64 and windows-aarch64 (2026-07-16)

Decision by the maintainer, recorded here and reflected in `.github/workflows/ci.yml`
(the `plan` job) and CLAUDE.md:

- **macOS Intel (macos-x86_64)** — Apple has ended Intel-platform support, so
  only users on outdated hardware remain, and people gaming on outdated
  MacBooks are a small fraction of an already-small potential userbase. The
  Rosetta CI legs were also 3-5x slower than every sibling leg (~50 min of
  the old ~73 min wall for Scala Native tests alone).
- **Windows ARM (windows-aarch64)** — Scala Native cannot target it (generates
  x64 code), and its JVM users are a small fraction of Windows users.

Consequences: no CI legs test these OS/arch combinations (JVM, Native, FFI IT,
release verification all dropped their rows); release artifacts that include
macos-x86_64 / windows-aarch64 native libraries still ship, **untested**.
Local development on these platforms is unsupported but not deliberately
broken.

**Revisit condition**: once everything else works (post-0.1.0, first game
running), reassess demand before investing test effort again. Tracked as an
open `improvement` issue in the issues DB.

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

## Architecture Rationale

### Why ANGLE (not native OpenGL)

ANGLE (Almost Native Graphics Layer Engine) translates OpenGL ES calls to
platform-native GPU APIs (Metal on macOS, Vulkan on Linux/Windows, Direct3D
on Windows). It is BSD-licensed, maintained by Google, and is the WebGL
implementation in Chrome, Edge, and Firefox. Using ANGLE means SGE writes to
a single GL ES API that works everywhere, including macOS where Apple
deprecated native OpenGL.

### Why Panama FFM (not LWJGL or JNI)

LWJGL provides Java bindings to C libraries (GLFW, OpenGL, OpenAL, stb). With
Panama FFM (`java.lang.foreign`, finalized in JDK 22), we call C libraries
directly — no LWJGL, no JNI. This gives JVM and Scala Native backends identical
native library dependencies (same Rust C ABI exports), shared knowledge, and
shared testing. On Android, PanamaPort (`com.v7878.foreign`) provides the same
API on ART.

| LWJGL module (eliminated) | Replacement |
|---------------------------|-------------|
| lwjgl-opengl / lwjgl-opengles | ANGLE via Panama |
| lwjgl-glfw | GLFW via Panama |
| lwjgl-openal | miniaudio via Panama |
| lwjgl-stb | Pure Scala decoders |
| lwjgl-core | `java.lang.foreign` |

## JVM (Desktop)

The primary desktop target. SGE uses:
- **GLFW** — windowing and input
- **ANGLE** — OpenGL ES via `libEGL` + `libGLESv2` (pre-built from sge-angle-natives)
- **miniaudio** — audio
- **Rust FFI via Panama FFM** — JDK 25+, `java.lang.foreign` downcall handles

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
- Static curl bundled for self-contained sttp HTTP (via the curl provider JAR auto-resolved by sbt)

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
