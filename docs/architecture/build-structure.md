# Build Structure

SGE's SBT multi-project layout using `sbt-projectmatrix` for cross-platform compilation.

## Why sbt-projectmatrix (not sbt-crossproject)

| Aspect | sbt-crossproject (legacy) | sbt-projectmatrix |
|--------|--------------------------|-------------------|
| Cross-building | Sequential (stateful `++` switching) | Parallel (each variant is a real subproject) |
| Extensibility | Platform axis only (JVM/JS/Native) | Arbitrary axes via `VirtualAxis` |
| sbt 2.0 status | Not in-sourced | **Built-in** (in-sourced into sbt 2.x) |
| sbt 1.x support | Yes | Yes (plugin v0.11.0) |
| Maintenance | Deprecated | Active |

## Actual Structure

```
sge/                              # Core library (projectMatrix: JVM/JS/Native)
  src/main/scala/sge/            #   Shared source (~503 files)
  src/main/scalajvm/sge/         #   JVM-specific (Panama FFM, Java stdlib)
  src/main/scalajs/sge/          #   JS-specific (scalajs-dom facades)
  src/main/scalanative/sge/      #   Native-specific (@extern, C ABI)
  src/main/scaladesktop/sge/     #   Desktop-shared (JVM + Native, not JS)
  src/test/scala/sge/            #   Cross-platform tests
  src/test/scalajvm/sge/         #   JVM-only tests
sge-jvm-platform/api/              # JVM platform interfaces (JDK 17)
sge-jvm-platform/jdk/              # JDK 22+ Panama FFM implementation
sge-jvm-platform/android/          # Android PanamaPort + ops implementations
sge-extension/freetype/            # FreeType font extension (projectMatrix)
sge-extension/physics/             # 2D physics via Rapier2D (projectMatrix)
sge-extension/tools/               # TexturePacker CLI (JVM-only)
sge-build/                        # sbt plugin (SgePlugin, packaging, Android)
demo/                             # Single cross-platform demo (root build)
demos/                            # 11 feature demos (separate sub-build)
sge-test/android-smoke/           # Minimal Android smoke-test APK
sge-test/                         # Integration tests (desktop, browser, android)
```

## sbt Project IDs

The `sge` projectMatrix generates these subprojects:

| Command | Platform |
|---------|----------|
| `sge/compile` | JVM |
| `sgeJS/compile` | Scala.js |
| `sgeNative/compile` | Scala Native |

JVM platform modules are merged into the sge JVM JAR via `packageBin/mappings`
(no `dependsOn` — avoids circular dependencies).

## Key Dependencies by Platform

| Platform | Graphics | Audio | Windowing | FFI |
|----------|----------|-------|-----------|-----|
| JVM | ANGLE (libEGL, libGLESv2) | miniaudio (Rust) | GLFW (Rust) | Panama FFM |
| Scala.js | WebGL/WebGL2 | Web Audio API | DOM/Canvas | N/A |
| Scala Native | ANGLE | miniaudio (Rust) | GLFW (Rust) | C ABI (@extern) |
| Android | System GL ES | miniaudio (Rust) | GLSurfaceView | PanamaPort |

## Demos Sub-Build

The `demos/` directory is a **separate sbt build** that depends on published SGE:

```sh
re-scale build publish-local --all  # Publish SGE to local Maven
cd demos && sbt --client compile    # Compile all 11 demos
```

Each demo uses `projectMatrix` with JVM, JS, and Native axes, plus cross-native
axes for building Scala Native binaries targeting non-host platforms (via zig).

## Validated Toolchain

| Tool | Version |
|------|---------|
| Scala | 3.8.2 |
| sbt | 1.12+ |
| sbt-projectmatrix | 0.11.0 |
| sbt-scalajs | 1.20.2 |
| sbt-scala-native | 0.5.10 |
| JDK | 23+ (distribution), 21+ (CI) |

See [cross-platform-settings.md](cross-platform-settings.md) for the full settings reference.
