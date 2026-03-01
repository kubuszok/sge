# Backend Analysis

Analysis of each LibGDX backend's porting feasibility and relevance to SGE.

**Update (2026-03-01):** The cross-platform architecture has been validated in
practice with the `hello-world/` prototype. All three primary backends (JVM
Desktop, Scala.js Browser, Scala Native Desktop) compile and run correctly with
identical user application behavior. See [platform-validation.md](platform-validation.md).

## gdx-backend-lwjgl3 (Primary JVM Target)

**Location**: `libgdx/backends/gdx-backend-lwjgl3/`

**Technology stack**:
- LWJGL3 (Lightweight Java Game Library) for native access
- GLFW for windowing, input, and context creation
- OpenGL/OpenGL ES for rendering
- OpenAL for audio
- stb_image, stb_truetype for image/font loading

**Porting strategy**: Direct port. LWJGL3 is a Java library, so it works from Scala on JVM
without modification. The backend code itself needs Scala conversion.

**Complexity**: Medium. Well-structured, well-documented backend.

**Files**: ~30 Java files in the backend.

**Validation status**: Architecture validated. JVM backend compiles and runs with
`sbt 'desktopJvm/run'`. LWJGL dependency configuration documented in
[cross-platform-settings.md](cross-platform-settings.md). `fork := true` required
for JNI library loading.

## gdx-backends-gwt (Scala.js Reference)

**Location**: `libgdx/backends/gdx-backends-gwt/`

**Technology stack**:
- GWT (Google Web Toolkit) for Java→JavaScript compilation
- WebGL for rendering
- Web Audio API for audio
- HTML5 Canvas for 2D fallback

**Relevance to SGE**: This backend is a **reference** for the Scala.js target, not a
direct port target. Key insights:
- Which APIs need browser-specific implementations
- How asset loading works in a browser context
- The `emu/` directory shows which Java stdlib classes GWT needed to emulate
  (Scala.js handles this differently)

**Key differences from direct port**:
- GWT uses `JSNI` (JavaScript Native Interface); Scala.js uses `@js.native` and facades
- GWT compiles Java; Scala.js compiles Scala directly
- No need for `emu/` directory — Scala.js has its own stdlib

**Validation status**: Architecture validated. Scala.js backend compiles, links via
fastLinkJS, and runs via Node.js with `sbt 'browser/run'`. Requires sbt-scalajs 1.20.0+
for Scala 3.8.2 compatibility. WASM backend available as zero-code-change opt-in (30%
faster numeric, experimental). See [research/findings/03-scalajs-wasm.md](../../research/findings/03-scalajs-wasm.md).

## gdx-backend-headless (Testing)

**Location**: `libgdx/backends/gdx-backend-headless/`

**Technology stack**: Mock implementations with no rendering.

**Porting strategy**: Port early — useful for running tests without a display.

**Complexity**: Low. Simple stub implementations of all platform traits.

**Files**: ~10 Java files.

**Note**: The hello-world Scala Native backend (`desktop-native/`) serves a similar
validation role. A headless backend for automated testing should follow the same
pattern: implement `Application` with no-op stubs for graphics/audio/input.

## gdx-backend-android (Future)

**Location**: `libgdx/backends/gdx-backend-android/`

**Relevance**: Low priority. Android support would require Scala-on-Android tooling
(which exists but is not mainstream). Consider as a future target after the three
primary platforms are stable.

## gdx-backend-robovm (Future)

**Location**: `libgdx/backends/gdx-backend-robovm/`

**Relevance**: Low priority. iOS support via MobiVM (community fork of RoboVM).

**Research finding (2026-02-28, corrected 2026-03-02)**: MobiVM 2.3.24 is the most
viable iOS path for SGE. Scala 3.8.2 emits **Java 17 class files** (version 61,
verified by `javap`). MobiVM 2.3.19+ claims Java 18 class file support (PR #672),
so version 61 should be in range, but **no one has tested Scala 3 + MobiVM**.
Scala Native iOS is NOT feasible (no official support). A 3-5 day proof-of-concept
is recommended before committing to iOS. See
[research/findings/05-robovm-scala3.md](../../research/findings/05-robovm-scala3.md).
