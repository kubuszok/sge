# Development Setup

External dependencies required to build, test, and release SGE.

## Required (all developers)

| Dependency | Version | Install (macOS) | Purpose |
|------------|---------|-----------------|---------|
| JDK | 25+ | `brew install --cask zulu` | Scala compiler, Panama FFM |
| sbt | 1.12+ | `brew install sbt` | Build tool |
| Node.js | 18+ | `brew install node` | Scala.js linking |
| ~~FreeType~~ | ~~2.x~~ | ~~`brew install freetype`~~ | No longer needed — bundled in provider JARs |

Native libraries (GLFW, miniaudio, buffer ops) are distributed as provider JARs
from Maven and resolved automatically by sbt. No local Rust toolchain is needed.
To modify native code, clone the
[sge-native-providers](https://github.com/kubuszok/sge-native-providers) repo.

### Quick install (macOS)

```sh
brew install sbt node
brew install --cask zulu
```

### First build

```sh
re-scale doctor               # Verify toolchain (first time)
re-scale build compile        # Compile SGE core (JVM)
re-scale test unit            # Run JVM unit tests
```

ANGLE OpenGL ES libraries (`libEGL`, `libGLESv2`) are bundled in the
`sge-angle-natives` provider JAR resolved automatically by sbt — no
manual download step needed.

## Optional (per workflow)

### Cross-platform native releases

For building Scala Native binaries targeting non-host platforms (e.g. Linux
from macOS):

| Dependency | Install (macOS) | Purpose |
|------------|-----------------|---------|
| zig | `brew install zig` | Cross-compilation C toolchain for Scala Native |

### Browser integration tests

| Dependency | Install | Purpose |
|------------|---------|---------|
| Playwright + Chromium | `npx playwright@1.49.0 install chromium` | Headless browser testing |

### Android

No system packages needed. The SDK, build-tools, and emulator
auto-download on the first invocation of any `androidXxx` sbt task —
no manual setup step is required. To force-download up front, run:

```sh
re-scale runner android-smoke-build   # Triggers androidSdkRoot, builds smoke APK
```

## No longer needed

These were previously required but have been replaced:

| Package | Was used for | Replaced by |
|---------|-------------|-------------|
| `angle` (Homebrew tap `startergo/angle/angle`) | ANGLE libraries for desktop OpenGL ES | `sge-angle-natives` provider JAR auto-resolved by sbt |
| `libcurl4-openssl-dev` (apt) | Scala Native sttp curl backend | Static curl provider JAR auto-resolved by sbt (from [stunnel/static-curl](https://github.com/stunnel/static-curl) Releases) |
| `libidn2-dev` (apt) | Scala Native sttp IDN support | Bundled in static curl package (libidn2.a included) |

If you have the ANGLE Homebrew tap installed, you can remove it:

```sh
brew uninstall angle
brew untap startergo/angle
```

## CI dependencies

What GitHub Actions CI installs beyond the base runner image. These are
handled automatically by the workflow and don't need manual installation.

### All platforms

- JDK 25 (via `actions/setup-java` with Zulu)
- sbt (via `sbt/setup-sbt`)

### Linux runners

```sh
# Scala Native system dependencies
sudo apt-get install -y clang libstdc++-12-dev
```

### Windows runners

```sh
choco install llvm --yes    # Scala Native system dependency
```

### macOS runners

No additional system packages (Xcode Command Line Tools provide clang).

### Android CI

```sh
# Via android-actions/setup-android
sdkmanager "platforms;android-35" "build-tools;35.0.0"
# Emulator via reactivecircus/android-emulator-runner
```

### Browser CI

```sh
npx playwright@1.49.0 install chromium
```

## ANGLE libraries

SGE uses [ANGLE](https://chromium.googlesource.com/angle/angle) to provide
OpenGL ES on desktop platforms (JVM and Scala Native). ANGLE binaries are
built from source via CI in the
[sge-angle-natives](https://github.com/kubuszok/sge-angle-natives)
repository and packaged into the per-platform provider JARs published to
Maven. The sbt build resolves them automatically — no manual download.

## Native components

Native libraries are built in the external
[sge-native-providers](https://github.com/kubuszok/sge-native-providers)
repository and distributed as provider JARs from Maven. Three libraries
are produced:

| Library | Purpose |
|---------|---------|
| `libsge_native_ops` | Buffer ops, ETC1 codec, GLFW windowing, miniaudio |
| `libsge_freetype` | FreeType font rasterization |
| `libsge_physics` | Rapier2D physics engine |

No local Rust build is needed. The sbt build resolves provider JARs
automatically via `sbt-multi-arch-release`.

### Static curl (Scala Native only)

For self-contained Scala Native releases, static curl libraries are
downloaded from [stunnel/static-curl](https://github.com/stunnel/static-curl)
and packaged into the curl provider JAR (`com.kubuszok:scala-native-curl-*-provider`)
that sbt resolves automatically.

This provides `libcurl.a` and all transitive dependencies (`libssl.a`,
`libcrypto.a`, `libidn2.a`, etc.) so Scala Native binaries don't require
system `libcurl` or `libidn2` packages.
