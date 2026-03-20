# Development Setup

External dependencies required to build, test, and release SGE.

## Required (all developers)

| Dependency | Version | Install (macOS) | Purpose |
|------------|---------|-----------------|---------|
| JDK | 23+ | `brew install --cask graalvm-jdk` | Scala compiler, Panama FFM |
| sbt | 1.12+ | `brew install sbt` | Build tool |
| Rust | stable | `brew install rustup && rustup-init` | native-components (GLFW, miniaudio, buffer ops) |
| Node.js | 18+ | `brew install node` | Scala.js linking |
| FreeType | 2.x | `brew install freetype` | Font rasterization (linked by Rust native-components) |

### Quick install (macOS)

```sh
brew install sbt node rustup freetype
brew install --cask graalvm-jdk
rustup-init
```

### First build

```sh
sge-dev native build          # Compile Rust native library
sge-dev native angle setup    # Download ANGLE OpenGL ES libraries
sge-dev build compile         # Compile SGE core (JVM)
sge-dev test unit             # Run JVM unit tests
```

## Optional (per workflow)

### Cross-platform native releases

For building Scala Native binaries targeting non-host platforms (e.g. Linux
from macOS):

| Dependency | Install (macOS) | Purpose |
|------------|-----------------|---------|
| zig | `brew install zig` | Cross-compilation C toolchain for Scala Native |
| cargo-zigbuild | `cargo install cargo-zigbuild` | Rust cross-compilation for Linux targets |
| cargo-xwin | `cargo install cargo-xwin` | Rust cross-compilation for Windows targets |

Or run `sge-dev native setup-toolchain` to install all three.

### Browser integration tests

| Dependency | Install | Purpose |
|------------|---------|---------|
| Playwright + Chromium | `npx playwright@1.49.0 install chromium` | Headless browser testing |

### Android

No system packages needed. The SDK, build-tools, and emulator are
auto-downloaded:

```sh
sge-dev test android setup    # Downloads Android SDK, NDK, emulator, system image
```

### Scala CLI (for sge-dev development)

| Dependency | Install (macOS) | Purpose |
|------------|-----------------|---------|
| Scala CLI | `brew install coursier && cs install scala-cli` | Building sge-dev toolkit |

## No longer needed

These were previously required but have been replaced:

| Package | Was used for | Replaced by |
|---------|-------------|-------------|
| `angle` (Homebrew tap `startergo/angle/angle`) | ANGLE libraries for desktop OpenGL ES | `sge-dev native angle setup` (downloads from [sge-angle-natives](https://github.com/MateuszKubuszok/sge-angle-natives) GitHub Releases) |
| `libcurl4-openssl-dev` (apt) | Scala Native sttp curl backend | `sge-dev native curl setup` (downloads from [stunnel/static-curl](https://github.com/stunnel/static-curl) GitHub Releases) |
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

- JDK 21 (via `actions/setup-java` with Temurin)
- sbt (via `sbt/setup-sbt`)
- Rust stable (via `dtolnay/rust-toolchain`)

### Linux runners

```sh
# Scala Native system dependencies
sudo apt-get install -y clang libstdc++-12-dev

# Static curl (replaces system libcurl/libidn2 — downloaded from stunnel/static-curl)
# See .github/workflows/ci.yml for the download step

# Rust cross-compilation
cargo install cargo-zigbuild cargo-xwin
# Zig (via mlugg/setup-zig)
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
[sge-angle-natives](https://github.com/MateuszKubuszok/sge-angle-natives)
repository and downloaded automatically.

```sh
sge-dev native angle setup          # Download for host platform
sge-dev native angle check          # Verify ANGLE is present
sge-dev native angle cross-collect  # Download for all 6 desktop platforms
```

The ANGLE version is pinned in `scripts/src/native/NativeCmd.scala`
(`AngleVersion` constant). To upgrade, update the constant and ensure a
matching release exists in sge-angle-natives.

## Native components

The Rust library in `native-components/` vendors all its C dependencies
(GLFW, miniaudio). Only FreeType is linked from the system. The library
produces:

| Output | Purpose |
|--------|---------|
| `libsge_native_ops` (.dylib/.so/.dll/.a) | Buffer ops, ETC1 codec |
| `libsge_audio` (.dylib/.so/.a) | miniaudio wrapper |
| `libglfw3` (.a) | GLFW 3.4 (static) |

Build with `sge-dev native build`. For all 6 desktop targets:
`sge-dev native cross-all`.

### Static curl (Scala Native only)

For self-contained Scala Native releases, static curl libraries are
downloaded from [stunnel/static-curl](https://github.com/stunnel/static-curl):

```sh
sge-dev native curl setup          # Download for host platform
sge-dev native curl check          # Verify curl is present
sge-dev native curl cross-collect  # Download for all 6 desktop platforms
```

This provides `libcurl.a` and all transitive dependencies (`libssl.a`,
`libcrypto.a`, `libidn2.a`, etc.) so Scala Native binaries don't require
system `libcurl` or `libidn2` packages.
