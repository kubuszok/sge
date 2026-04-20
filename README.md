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

### Core library

- **539 / 605** core library files converted from Java to Scala 3
- **124 / 152** backend files done (desktop, browser, headless, Android; iOS deferred)
- **534+ files audited** against original LibGDX sources
- **11 demo applications** build and run on all 4 platforms
- All 11 demos produce signed Android APKs

### Extensions

17 extensions ported from the LibGDX ecosystem:

| Extension | Ported from | Platforms |
|-----------|-------------|-----------|
| sge-ai | [gdx-ai](https://github.com/libgdx/gdx-ai) | JVM / JS / Native |
| sge-ecs | [Ashley ECS](https://github.com/libgdx/ashley) | JVM / JS / Native |
| sge-controllers | [gdx-controllers](https://github.com/libgdx/gdx-controllers) | JVM / JS / Native |
| sge-freetype | LibGDX FreeType | JVM / JS / Native |
| sge-physics | [Rapier2D](https://rapier.rs/) | JVM / JS / Native |
| sge-physics3d | [Rapier3D](https://rapier.rs/) | JVM / JS / Native |
| sge-gltf | [gdx-gltf](https://github.com/mgsx-dev/gdx-gltf) | JVM / JS / Native |
| sge-vfx | [gdx-vfx](https://github.com/crashinvaders/gdx-vfx) | JVM / JS / Native |
| sge-textra | [TextraTypist](https://github.com/tommyettinger/textratypist) | JVM / JS / Native |
| sge-colorful | [colorful-gdx](https://github.com/tommyettinger/colorful-gdx) | JVM / JS / Native |
| sge-visui | [VisUI](https://github.com/kotcrab/vis-ui) | JVM / JS / Native |
| sge-screens | [libgdx-screenmanager](https://github.com/crykn/libgdx-screenmanager) | JVM / JS / Native |
| sge-anim8 | [anim8-gdx](https://github.com/tommyettinger/anim8-gdx) | JVM / JS / Native |
| sge-noise | [noise4j](https://github.com/czyzby/noise4j) | JVM / JS / Native |
| sge-graphs | [simple-graphs](https://github.com/earlygrey/simple-graphs) | JVM / JS / Native |
| sge-jbump | [jbump](https://github.com/tommyettinger/jbump) | JVM / JS / Native |
| sge-tools | LibGDX TexturePacker | JVM only |

## Prerequisites

- **JDK 25+** (Azul Zulu recommended)
- **sbt 1.12+**
- **Node.js 18+** (for Scala.js linking)

### macOS (Homebrew)

```sh
brew install sbt coursier node
brew install --cask zulu
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
re-scale test unit             # Run JVM unit tests
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
sge/
├── sge/                              Core library (JVM / JS / Native)
├── sge-jvm-platform/
│   ├── api/                          Platform abstraction interfaces (JDK 17)
│   ├── jdk/                          JDK 22+ Panama FFM implementation
│   └── android/                      Android backend (PanamaPort)
├── sge-extension/
│   ├── ai/                           AI: behavior trees, FSM, pathfinding
│   ├── ecs/                          Entity Component System
│   ├── controllers/                  Game controller input
│   ├── freetype/                     FreeType font rasterization
│   ├── physics/                      2D physics (Rapier2D)
│   ├── physics3d/                    3D physics (Rapier3D)
│   ├── gltf/                         glTF model loading
│   ├── vfx/                          Visual effects (bloom, blur, CRT, ...)
│   ├── textra/                       Advanced text rendering with effects
│   ├── colorful/                     Color space utilities (Oklab, HSL, ...)
│   ├── visui/                        UI widget library
│   ├── screens/                      Screen management + transitions
│   ├── anim8/                        Animation recording (PNG/GIF export)
│   ├── noise/                        Procedural generation (noise, dungeons)
│   ├── graphs/                       Graph algorithms (A*, BFS, DFS, ...)
│   ├── jbump/                        AABB tile-based collision
│   └── tools/                        TexturePacker CLI (JVM-only)
├── sge-build/                        sbt plugin (SgePlugin, SgePackaging)
├── sge-test/
│   ├── regression/                   Cross-platform regression tests
│   ├── it-desktop/                   Desktop IT (GLFW + ANGLE + audio)
│   ├── it-browser/                   Browser IT (Playwright + Chromium)
│   ├── it-jvm-platform/              Panama provider + Android ops tests
│   ├── it-android/                   Android emulator tests
│   ├── it-native-ffi/                Scala Native FFI validation
│   └── android-smoke/                Minimal Android smoke APK
├── demos/                            11 feature demos (separate sub-build)
├── original-src/libgdx/              Reference LibGDX Java source
└── docs/                             Architecture, guides, improvements
```

## Architecture

### Cross-platform core

SGE uses sbt-projectmatrix to compile a single Scala 3 codebase to JVM, JS, and
Native. Platform-specific code lives in `sge/src/main/{scalajvm,scalajs,scalanative}/`.
Desktop-shared code (JVM + Native, not JS) is in `sge/src/main/scaladesktop/`.

### Build tooling

The `sge-build/` directory contains an sbt plugin with SGE-specific conventions
(compiler flags, packaging). Generic cross-platform infrastructure (Platform
detection, AndroidBuild, JvmPackaging, NativeProviderPlugin, ZigCross) lives in
[sbt-multiarch-scala](https://github.com/kubuszok/sbt-multiarch-scala).

### Native components

Native libraries are built in the external
[sge-native-providers](https://github.com/kubuszok/sge-native-providers)
repository and distributed as provider JARs from Maven. No local Rust build
is needed — the sbt build resolves native libraries automatically.

Four native libraries are produced:

- **`libsge_native_ops`** — buffer operations, ETC1 codec, GLFW windowing, miniaudio
- **`libsge_freetype`** — FreeType font rasterization
- **`libsge_physics`** — Rapier2D physics engine
- **`libsge_physics3d`** — Rapier3D physics engine

Two provider JAR types distribute these:
- `sn-provider-*` — static libraries for Scala Native linking
- `pnm-provider-*-desktop` — shared libraries for JVM/Panama FFM

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
re-scale test unit                    # JVM unit tests
re-scale test unit --js               # Scala.js unit tests
re-scale test unit --native           # Scala Native unit tests
re-scale runner browser-it            # Playwright browser integration tests
re-scale runner desktop-it            # Desktop end-to-end (GLFW + ANGLE + audio)
re-scale runner android-it            # Android emulator smoke test
```

## CI

GitHub Actions runs ~20 jobs on every push:

- JVM unit tests on 6 platforms (Linux, macOS ARM + x86_64, Windows)
- Scala Native tests on 5 platforms
- Native FFI headless validation on 5 platforms
- Desktop IT with headless library loading validation
- Code coverage report (JVM, via scoverage)
- Browser smoke tests (Playwright + headless Chromium)
- Android smoke test (emulator + SwiftShader)
- All 11 demos compiled on JVM + JS + Native
- Release packaging verification on 6 platforms
- Covenant verification (enforce gate)

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
