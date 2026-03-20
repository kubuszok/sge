# Backend Graphics Strategy

**Date**: 2026-03-05
**Status**: Decided

## Decision: ANGLE + Panama + SDL3

SGE will use a unified native graphics and platform layer across all backends,
eliminating the LWJGL dependency in favor of direct C library access.

### Architecture

```
                    SGE Core (Scala 3, cross-platform)
                    GL20/GL30/GL31/GL32 traits
                              |
            +-----------------+-----------------+
            |                 |                 |
      JVM Backend      Native Backend    Browser Backend
      (Panama FFM)     (@extern C FFI)   (scalajs-dom)
            |                 |                 |
    ANGLE libGLESv2    ANGLE libGLESv2    WebGL2
    SDL3 / GLFW        SDL3 / GLFW        Canvas + DOM
    miniaudio          miniaudio          Web Audio API
```

### Why ANGLE

ANGLE (Almost Native Graphics Layer Engine) translates OpenGL ES 2.0/3.0/3.1
calls to platform-native GPU APIs:

| Backend     | Platform              | Status in ANGLE |
|-------------|-----------------------|-----------------|
| Metal       | macOS, iOS            | Complete (ES 3.1) |
| Vulkan      | Windows, Linux, Android | Complete |
| Direct3D 11 | Windows               | Complete |
| Direct3D 12 | Windows               | Experimental |
| Desktop GL  | Linux (fallback)      | Legacy |

ANGLE is BSD-licensed, maintained by Google, and is the WebGL implementation
in Chrome, Edge, and Firefox (Windows). It is battle-tested at massive scale.

LibGDX already supports ANGLE via `Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20`
on desktop and via MetalANGLE on iOS. The `gdx-lwjgl3-angle` extension ships
pre-built ANGLE binaries for Windows (32/64), Linux (64), macOS (x64/arm64).

**MetalANGLE is deprecated.** Its author (kakashidinho) joined Google and merged
all Metal backend code into mainline ANGLE (June 2021). Mainline ANGLE now has
complete ES 3.1 on Metal, surpassing MetalANGLE's ~90% ES 3.0 coverage.

### Why Panama (replacing JNI)

Project Panama's Foreign Function & Memory API (java.lang.foreign) provides
zero-boilerplate native function calls from the JVM. Finalized in JDK 22;
SGE targets JDK 23+ for distribution (all 6 desktop platforms including
Windows ARM64).

Benefits over JNI:
- No Java native method declarations needed
- No C/Rust JNI bridge code (eliminates 583 lines of `jni_bridge.rs`)
- Same C ABI functions called by both JVM (Panama) and Scala Native (@extern)
- No `jni` Rust crate dependency
- Better performance (no JNI overhead for simple calls)
- Type-safe memory access via `MemorySegment`

The Rust library already exports C ABI functions (`sge_copy_bytes`,
`sge_transform_v4m4`, etc.) for Scala Native. Panama calls these same functions.

### Why eliminate LWJGL

LWJGL provides Java bindings to C libraries (GLFW, OpenGL, OpenAL, stb).
With Panama, we can call these C libraries directly:

| LWJGL Module    | Replacement           |
|-----------------|-----------------------|
| lwjgl-opengl    | ANGLE via Panama      |
| lwjgl-opengles  | ANGLE via Panama      |
| lwjgl-glfw      | GLFW via Panama       |
| lwjgl-openal    | miniaudio via Panama  |
| lwjgl-stb       | Pure Scala decoders   |
| lwjgl-core      | java.lang.foreign     |

This gives JVM and Scala Native backends identical native library dependencies,
shared knowledge, and shared testing.

### SDL3 vs GLFW

SDL3 is the stronger option for a game engine:

| Feature           | GLFW | SDL3 |
|-------------------|------|------|
| Window management | Yes  | Yes  |
| Game controllers  | No   | Yes  |
| Audio             | No   | Yes  |
| Haptics           | No   | Yes  |
| File dialogs      | No   | Yes  |
| Mobile (iOS/Android) | No | Yes |
| WebAssembly       | Limited | First-class |

Decision on SDL3 vs GLFW is deferred until backend prototyping phase.
Initial prototype uses GLFW (simpler, sufficient for desktop).

### Browser Target

Scala.js remains the browser compilation target. The Scala.js Wasm backend
(experimental since 1.17.0) provides ~30% faster Scala computation as an
opt-in optimization with zero code changes.

The async/await limitation in JavaScript is manageable for game engines
because the game loop is inherently frame-based. JSPI (JavaScript Promise
Integration) in the Wasm backend enables sync-looking async when needed.

WebAssembly still requires JavaScript glue for WebGL/WebGPU access -- there
is no "native" GPU path from Wasm.

### iOS Strategy

**Deferred** until Scala Native gains iOS support. Current blockers:

1. `sys/posix_sem.h` missing in iOS SDK (scala-native#2875)
2. `time_nano.c` macOS version guard excludes iOS
3. `Discover.scala` hardcodes macOS include paths
4. No Objective-C interop (bypassed by SDL3 for games)

When these are resolved, the architecture would be:
- Scala Native targeting `arm64-apple-ios15.0`
- ANGLE (mainline, Metal backend) for OpenGL ES
- SDL3 for windowing, input, app lifecycle (handles all UIKit internally)
- Same code as desktop Native backend

Fallback path: MobiVM (JVM bytecode to native ARM) if Scala Native iOS
remains blocked.

### Android

Project Panama (`java.lang.foreign`) is NOT available on Android. Android uses ART
(Android Runtime), a separate Java implementation that does not support Panama FFM.

| Platform | Native Call Mechanism | Notes |
|----------|----------------------|-------|
| Desktop JVM | Panama FFM | JDK 22+, zero-boilerplate |
| Scala Native | @extern C FFI | Desktop + iOS (future) |
| Scala.js | JS interop | Pure Scala fallback |
| Android | JNI (traditional) | ART doesn't support Panama |

The Rust library keeps the JNI bridge behind an `android` feature flag for this
purpose. The same core Rust functions are called by all three mechanisms (Panama,
@extern, JNI) — only the marshaling layer differs.

### Performance

- ANGLE overhead vs native GL: ~5-15% (well-optimized, Chrome default)
- Metal backend on macOS: faster than deprecated native OpenGL
- Panama vs JNI: comparable or better (no marshaling overhead for primitives)
- Scala.js Wasm: ~30% faster than JS for Scala-dominated computation
