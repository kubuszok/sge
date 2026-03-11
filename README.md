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

- **JDK 22+** (GraalVM CE recommended)
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
| `chromedriver` | Playwright browser integration tests |
| `angle-grpc` / ANGLE libs | Native desktop rendering (JVM uses bundled ANGLE) |

### Android (optional)

```sh
just android-sdk-setup    # Downloads SDK, build-tools, system image, emulator
```

## Quick Start

### Build and test (JVM)

```sh
just rust-build       # Build Rust native library (first time only)
just compile          # Compile SGE core (JVM)
just test             # Run 1450 JVM unit tests
```

### All platforms

```sh
just test-all         # JVM + JS + Native unit tests
just it-all           # All integration tests (desktop + browser + android)
```

### Run a demo (JVM)

```sh
just demo-jvm         # Launches the built-in demo with GLFW window
```

### Build demo APKs (Android)

```sh
just android-sdk-setup              # One-time setup
cd demos && sbt --client 'androidAll'   # Build all 10 demo APKs
```

### Build demos for browser

```sh
cd demos && sbt --client 'pongJS/fastLinkJS'   # Single demo
# Output: demos/pong/target/js-3/fastLinkJS/main.js
```

## Project Structure

```
sge/                          Core library (JVM / JS / Native)
sge-jvm-platform-api/         Platform abstraction interfaces (JDK 17)
sge-jvm-platform-jdk/         JDK 22+ Panama FFM implementation
sge-jvm-platform-android/     Android backend (PanamaPort + Android APIs)
sge-freetype/                 FreeType font extension (JVM / JS / Native)
sge-physics/                  2D physics via Rapier2D (JVM / JS / Native)
sge-tools/                    TexturePacker CLI (JVM-only)
sge-build/                    sbt plugin (SgePlugin, AndroidBuild, packaging)
native-components/            Rust: GLFW, miniaudio, FreeType, buffer ops
demo/                         Single cross-platform demo (root build)
demos/                        10 feature demos (separate sbt sub-build)
sge-android-smoke/            Minimal Android smoke-test APK
sge-it-tests/                 Integration tests (desktop, browser, android)
scalafix-rules/               Custom Scalafix lint rules
libgdx/                       Original LibGDX Java source (reference only)
docs/                         Architecture, guides, audit trail, progress
```

## Architecture

### Cross-platform core

SGE uses sbt-projectmatrix to compile a single Scala 3 codebase to JVM, JS, and
Native. Platform-specific code lives in `sge/src/main/{scalajvm,scalajs,scalanative}/`.
Desktop-shared code (JVM + Native, not JS) is in `sge/src/main/scaladesktop/`.

### Native components

The Rust library in `native-components/` provides:

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
just test              # JVM unit tests (1450)
just test-js           # Scala.js unit tests (1096)
just test-native       # Scala Native unit tests (1096)
just test-browser      # Playwright browser integration tests
just it-desktop        # Desktop end-to-end (GLFW + ANGLE + audio)
just test-android      # Android emulator smoke test
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
