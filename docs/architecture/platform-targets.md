# Platform Targets

SGE targets four compilation platforms via Scala's cross-compilation ecosystem.

## Target Matrix

| Target | Strategy | Reference Backend | Status |
|--------|----------|-------------------|--------|
| Core | Direct port, platform-agnostic | `libgdx/gdx/` | In progress |
| JVM | Port LWJGL3 backend | `libgdx/backends/gdx-backend-lwjgl3/` | Not started |
| Scala.js | Reverse-engineer GWT backend, use scalajs-dom | `libgdx/backends/gdx-backends-gwt/` | Not started |
| Scala Native | Same C libs as LWJGL, new Scala Native bindings | No direct reference | Not started |

## Core (Platform-Agnostic)

The `core` module contains all platform-independent game logic. This is the primary focus
of the current porting effort.

**Source**: `libgdx/gdx/src/com/badlogic/gdx/` → `core/src/main/scala/sge/`

All APIs in core must be expressible without platform-specific dependencies. Platform backends
implement traits defined in core (`Graphics`, `Audio`, `Input`, `Files`, `Net`).

## JVM (LWJGL3)

The primary desktop target. LibGDX uses LWJGL3 which wraps:
- **GLFW** — windowing and input
- **OpenGL** — graphics rendering
- **OpenAL** — audio
- **stb** — image loading, font rasterization

The JVM backend is the most straightforward port since LWJGL3 is a Java library
accessible directly from Scala.

## Scala.js (Browser)

Browser target using the WebGL and Web Audio APIs.

LibGDX's GWT backend (`gdx-backends-gwt`) is the reference but requires significant
rethinking since:
- GWT compiles Java to JavaScript; Scala.js compiles Scala to JavaScript directly
- GWT has an `emu/` directory for stdlib emulation that is not needed
- WebGL API access via `scalajs-dom` instead of GWT's JSNI

Key considerations:
- `scalajs-dom` provides typed access to WebGL, Web Audio, DOM
- Asset loading via `XMLHttpRequest` / `fetch`
- No filesystem access (use IndexedDB for persistence)
- Single-threaded (no `Thread`, use `Future`/`Promise`)

## Scala Native

Native compilation target for desktop/embedded without JVM overhead.

No direct LibGDX reference backend exists. Strategy:
- Use the same C libraries as LWJGL (GLFW, OpenGL, OpenAL, stb) via Scala Native C interop
- Write bindings using `@extern` and `CStruct`
- Memory management via `Zone` allocators

Key considerations:
- No JNI — direct C FFI
- No garbage collector pressure for native buffers
- Binary distribution without JVM dependency
