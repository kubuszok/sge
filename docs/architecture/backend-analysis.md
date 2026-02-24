# Backend Analysis

Analysis of each LibGDX backend's porting feasibility and relevance to SGE.

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

## gdx-backend-headless (Testing)

**Location**: `libgdx/backends/gdx-backend-headless/`

**Technology stack**: Mock implementations with no rendering.

**Porting strategy**: Port early — useful for running tests without a display.

**Complexity**: Low. Simple stub implementations of all platform traits.

**Files**: ~10 Java files.

## gdx-backend-android (Future)

**Location**: `libgdx/backends/gdx-backend-android/`

**Relevance**: Low priority. Android support would require Scala-on-Android tooling
(which exists but is not mainstream). Consider as a future target after the three
primary platforms are stable.

## gdx-backend-robovm (Future)

**Location**: `libgdx/backends/gdx-backend-robovm/`

**Relevance**: Low priority. iOS support via RoboVM. The Scala Native target may
eventually serve a similar purpose with direct compilation to ARM.
