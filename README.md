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
- **11 demo applications** build and run on all 4 platforms
- All 11 demos produce signed Android APKs
- 10/11 demos link on Scala Native (NetChat excluded: scala-xml SAX dependency)

## Prerequisites

- **JDK 25+** (Azul Zulu recommended)
- **sbt 1.12+**
- **Node.js 18+** (for Scala.js linking)

### macOS (Homebrew)

```sh
brew install sbt coursier node freetype
```

Optional (for specific workflows):

| Package | Needed for |
|---------|-----------|
| `zig` | Cross-platform Scala Native releases |
| Playwright | Browser integration tests (`npx playwright install chromium`) |

See [docs/contributing/setup.md](docs/contributing/setup.md) for the full
dependency list, CI setup, and what can be removed.

### Android (optional)

The Android SDK auto-downloads on the first invocation of any
`androidXxx` sbt task — no manual setup needed.

## Quick Start

The dev workflow uses [`re-scale`](https://github.com/kubuszok/re-scale)
(install once via `git clone … && ./scripts/install.sh`) which keeps
sbt warm via `--client` to avoid the 30s JVM startup tax.

### Build and test (JVM)

```sh
re-scale doctor                # Check toolchain (first time)
re-scale build compile         # Compile SGE core (JVM)
re-scale test unit             # Run 1450 JVM unit tests
```

### All platforms

```sh
re-scale test unit --all       # JVM + JS + Native unit tests
re-scale runner desktop-it     # Desktop integration tests (GLFW + ANGLE + miniaudio)
re-scale runner browser-it     # Playwright browser smoke tests
re-scale runner android-it     # Android emulator integration tests
```

### Run a demo

The `demos/` directory is a separate sbt sub-build that depends on SGE as a
published library. First publish SGE locally, then run any demo:

```sh
# Publish SGE to local Ivy/Maven cache
re-scale build publish-local       # JVM only (fastest — ~30s)
# or: re-scale build publish-local --all  # All platforms: JVM + JS + Native (~2min)

# Run a demo (from demos/ sub-build)
cd demos && sbt --client 'pong/run'              # JVM (GLFW window)
cd demos && sbt --client 'pongNative/run'        # Scala Native
cd demos && sbt --client 'pongJS/fastLinkJS'     # Scala.js (output in target/)
```

Available demos: `pong`, `spaceShooter`, `tileWorld`, `hexTactics`,
`curvePlayground`, `shaderLab`, `viewer3d`, `particleShow`, `netChat`,
`viewportGallery`, `assetShowcase`. Append platform suffix (`JS`, `Native`)
for non-JVM targets.

### Build demo APKs (Android)

```sh
re-scale runner android-build-all          # Build all 11 demo APKs (sbt androidAll inside demos/)
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
sge-test/regression/          Single cross-platform demo (root build)
demos/                        11 feature demos (separate sbt sub-build)
sge-test/android-smoke/       Minimal Android smoke-test APK
sge-test/                     Integration tests (desktop, browser, android)
original-src/libgdx/          Original LibGDX Java source (reference only)
docs/                         Architecture, guides, audit trail, progress
```

## Architecture

### Cross-platform core

SGE uses sbt-projectmatrix to compile a single Scala 3 codebase to JVM, JS, and
Native. Platform-specific code lives in `sge/src/main/{scalajvm,scalajs,scalanative}/`.
Desktop-shared code (JVM + Native, not JS) is in `sge/src/main/scaladesktop/`.

### Native components

Native libraries are built in the external
[sge-native-components](https://github.com/kubuszok/sge-native-components)
repository and distributed as provider JARs from Maven. No local Rust build
is needed — the sbt build resolves native libraries automatically.

Three separate libraries are produced:

- **`libsge_native_ops`** — buffer operations, ETC1 codec, GLFW windowing, miniaudio
- **`libsge_freetype`** — FreeType font rasterization
- **`libsge_physics`** — Rapier2D physics engine

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
| Net Chat | XML parsing, clipboard, and time utilities |
| Asset Showcase | Asset loading and management demo |

All demos are in the `demos/` directory (separate sbt sub-build). Each supports
JVM, JS, Native, and Android targets.

## Testing

```sh
re-scale test unit                    # JVM unit tests (1450)
re-scale test unit --js               # Scala.js unit tests (1096)
re-scale test unit --native           # Scala Native unit tests (1096)
re-scale runner browser-it            # Playwright browser integration tests
re-scale runner desktop-it            # Desktop end-to-end (GLFW + ANGLE + audio)
re-scale runner android-it            # Android emulator smoke test
```

## CI

GitHub Actions runs on every push:

- Unit tests on JVM, JS, and Native (Linux, macOS, Windows)
- Native FFI headless validation
- Browser smoke tests (Playwright + headless Chromium)
- Android smoke test (emulator + SwiftShader)
- Native linking verification for all 11 demos
- All 11 demos compiled on all 3 platforms
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
