# SGE - Scala Game Engine

SGE is a cross-platform 2D/3D game engine written in Scala 3, ported from
[LibGDX](https://libgdx.com/). It targets four platforms from a single codebase:

| Platform | Backend | Rendering |
|----------|---------|-----------|
| **JVM** (desktop) | GLFW + Rust FFI via Panama FFM | OpenGL ES via ANGLE |
| **Browser** (Scala.js) | Canvas/WebGL DOM APIs | WebGL / WebGL2 |
| **Native** (Scala Native) | GLFW + Rust FFI via C ABI | OpenGL ES via ANGLE |
| **Android** | Android SDK + PanamaPort FFM | OpenGL ES (native) |

## Status

- **539 / 605** core library files converted from Java to Scala 3
- **124 / 152** backend files done (desktop, browser, headless, Android complete; iOS deferred)
- **534+ files audited** against original LibGDX sources
- **1450 JVM + 1096 JS + 1096 Native** unit tests passing
- **10 demo applications** build and run on all 4 platforms
- All 10 demos produce signed Android APKs
- 9/10 demos link on Scala Native (NetChat excluded: scala-xml SAX dependency)

## Prerequisites

- **JDK 23+** (GraalVM CE recommended)
- **sbt 1.12+**
- **Rust toolchain** (for native-components)
- **Node.js 18+** (for Scala.js linking)

### macOS (Homebrew)

```sh
brew install sbt coursier node rustup freetype
rustup-init
```

Optional (for specific workflows):

| Package | Needed for |
|---------|-----------|
| `zig` | Cross-platform Scala Native releases |
| Playwright | Browser integration tests (`npx playwright install chromium`) |

See [docs/contributing/setup.md](docs/contributing/setup.md) for the full
dependency list, CI setup, and what can be removed.

### Android (optional)

```sh
sge-dev test android setup    # Downloads SDK, build-tools, system image, emulator
```

## Quick Start

### Build and test (JVM)

```sh
sge-dev native build          # Build Rust native library (first time only)
sge-dev build compile         # Compile SGE core (JVM)
sge-dev test unit             # Run 1450 JVM unit tests
```

### All platforms

```sh
sge-dev test unit --all               # JVM + JS + Native unit tests
sge-dev test integration --all        # All integration tests (desktop + browser + android)
```

### Run a demo

The `demos/` directory is a separate sbt sub-build that depends on SGE as a
published library. First build and publish SGE locally, then run any demo:

```sh
# One-time: build native libraries
sge-dev native build              # Rust native-components (GLFW, miniaudio, buffer ops)
sge-dev native angle setup        # ANGLE OpenGL ES libraries (libEGL, libGLESv2)

# Publish SGE to local Ivy/Maven cache
sge-dev build publish-local       # JVM only (fastest — ~30s)
# or: sge-dev build publish-local --all  # All platforms: JVM + JS + Native (~2min)

# Run a demo (from demos/ sub-build)
cd demos && sbt --client 'pong/run'              # JVM (GLFW window)
cd demos && sbt --client 'pongNative/run'        # Scala Native
cd demos && sbt --client 'pongJS/fastLinkJS'     # Scala.js (output in target/)
```

Available demos: `pong`, `spaceShooter`, `tileWorld`, `hexTactics`,
`curvePlayground`, `shaderLab`, `viewer3d`, `particleShow`, `netChat`,
`viewportGallery`. Append platform suffix (`JS`, `Native`) for non-JVM targets.

### Build demo APKs (Android)

```sh
sge-dev test android setup                       # One-time setup
cd demos && sbt --client 'androidAll'             # Build all 10 demo APKs
```

### Build demos for browser

```sh
cd demos && sbt --client 'pongJS/fastLinkJS'   # Single demo
# Output: demos/pong/target/js-3/fastLinkJS/main.js
```

## Project Structure

```
sge/                          Core library (JVM / JS / Native)
sge-jvm-platform/api/         Platform abstraction interfaces (JDK 17)
sge-jvm-platform/jdk/         JDK 22+ Panama FFM implementation
sge-jvm-platform/android/     Android backend (PanamaPort + Android APIs)
sge-extension/freetype/       FreeType font extension (JVM / JS / Native)
sge-extension/physics/        2D physics via Rapier2D (JVM / JS / Native)
sge-extension/tools/          TexturePacker CLI (JVM-only)
sge-build/                    sbt plugin (SgePlugin, AndroidBuild, packaging)
sge-deps/native-components/   Rust: GLFW, miniaudio, FreeType, buffer ops
sge-test/regression/          Single cross-platform demo (root build)
demos/                        10 feature demos (separate sbt sub-build)
sge-test/android-smoke/       Minimal Android smoke-test APK
sge-test/                     Integration tests (desktop, browser, android)
scalafix-rules/               Custom Scalafix lint rules
original-src/libgdx/          Original LibGDX Java source (reference only)
docs/                         Architecture, guides, audit trail, progress
```

## Architecture

### Cross-platform core

SGE uses sbt-projectmatrix to compile a single Scala 3 codebase to JVM, JS, and
Native. Platform-specific code lives in `sge/src/main/{scalajvm,scalajs,scalanative}/`.
Desktop-shared code (JVM + Native, not JS) is in `sge/src/main/scaladesktop/`.

### Native components

The Rust library in `sge-deps/native-components/` provides:

- **Buffer operations** (memcpy, direct allocation) via `libsge_native_ops`
- **Audio engine** (miniaudio wrapper) via `libsge_audio`
- **Windowing** (GLFW, compiled from vendored source) via `libglfw3`
- **FreeType** font rasterization (optional feature)
- **Rapier2D** physics (optional feature)

On JVM, these are accessed via Panama FFM (JDK 22+ `java.lang.foreign`).
On Scala Native, via `@extern` C FFI. On Android, via PanamaPort (backport
of FFM to Android API 26+).

### Sge context

SGE replaces LibGDX's global statics (`Gdx.graphics`, `Gdx.input`, etc.) with
an explicit `(using Sge)` context parameter on class constructors. This enables
parallel testing, multiple app instances, and eliminates global mutable state.

## Demos

| Demo | Description |
|------|-------------|
| Pong | Classic 2-player paddle game |
| Space Shooter | Bullet-hell shooter |
| Curve Playground | Interactive Bezier curves |
| Viewport Gallery | Camera and viewport showcase |
| Shader Lab | Custom GLSL shader workbench |
| Tile World | Tile-based map rendering |
| Hex Tactics | Hexagonal grid tactics |
| Viewer 3D | G3DJ model viewer |
| Particle Show | Particle effects showcase |
| Net Chat | TCP/UDP networking demo |

All demos are in the `demos/` directory (separate sbt sub-build). Each supports
JVM, JS, Native, and Android targets.

## Testing

```sh
sge-dev test unit                     # JVM unit tests (1450)
sge-dev test unit --js                # Scala.js unit tests (1096)
sge-dev test unit --native            # Scala Native unit tests (1096)
sge-dev test browser                  # Playwright browser integration tests
sge-dev test integration --desktop    # Desktop end-to-end (GLFW + ANGLE + audio)
sge-dev test android test             # Android emulator smoke test
```

## CI

GitHub Actions runs on every push:

- Rust cross-compilation (6 desktop targets + 3 Android targets)
- Unit tests on JVM, JS, and Native (Linux, macOS, Windows)
- Browser smoke tests (Playwright + headless Chromium)
- Android smoke test (emulator + SwiftShader)
- All 10 demos compiled on all 3 platforms
- Demo JS bundles verified in browser

## License

Apache License 2.0 (same as LibGDX). See [LICENSE](LICENSE) for the full text.

SGE is a derivative work of [libGDX](https://github.com/libgdx/libgdx) by
Mario Zechner and Nathan Sweet. The original libGDX contributors are listed
in [CONTRIBUTORS](CONTRIBUTORS). See [NOTICE](NOTICE) for full attribution.

Vendored native libraries (GLFW, miniaudio) have their own compatible licenses
documented in [THIRD-PARTY-LICENSES](THIRD-PARTY-LICENSES).

## AI-Assisted Development

The Java-to-Scala 3 port was performed primarily using AI code generation tools
(Anthropic Claude Code). All AI-generated code was reviewed, tested, and audited
against the original LibGDX sources by the project maintainer. Each ported file
contains a header comment documenting its original source, migration notes, and
audit date.
