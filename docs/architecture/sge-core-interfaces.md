# SGE Core Interfaces Analysis

## 1. Traits/Interfaces Backends Must Implement

A backend must supply concrete implementations of **7 core traits** plus **3 supporting traits**:

### Core module traits (package `sge`)

| Trait | File | Purpose |
|-------|------|---------|
| `Application` | `core/src/main/scala/sge/Application.scala` | Top-level entry point; owns all subsystems |
| `Graphics` | `core/src/main/scala/sge/Graphics.scala` | Display, GL context, frame timing, cursors |
| `Audio` | `core/src/main/scala/sge/Audio.scala` | Sound effects, music, recording, output devices |
| `Input` | `core/src/main/scala/sge/Input.scala` | Keyboard, mouse/touch, sensors, on-screen keyboard |
| `Files` | `core/src/main/scala/sge/Files.scala` | File system access (internal, external, classpath, local, absolute) |
| `Net` | `core/src/main/scala/sge/Net.scala` | HTTP requests, TCP sockets, URI launching |
| `Preferences` | `core/src/main/scala/sge/Preferences.scala` | Persistent key-value store (per-name) |

### Supporting traits

| Trait | File | Purpose |
|-------|------|---------|
| `Clipboard` | `core/src/main/scala/sge/utils/Clipboard.scala` | System clipboard read/write |
| `ApplicationLogger` | `core/src/main/scala/sge/ApplicationLogger.scala` | Logging backend (stdout, logcat, etc.) |
| `InputProcessor` | `core/src/main/scala/sge/InputProcessor.scala` | Backends dispatch events to it |

### Lifecycle contracts (user-implemented, called by backends)

| Trait | File | Purpose |
|-------|------|---------|
| `ApplicationListener` | `core/src/main/scala/sge/ApplicationListener.scala` | `create/resize/render/pause/resume/dispose` |
| `LifecycleListener` | `core/src/main/scala/sge/LifecycleListener.scala` | `pause/resume/dispose` for extensions |
| `Screen` | `core/src/main/scala/sge/Screen.scala` | Screen-based navigation |

### Aggregate context

| Type | File | Purpose |
|------|------|---------|
| `Sge` (case class) | `core/src/main/scala/sge/Sge.scala` | Bundles all 6 subsystems into a single context parameter |

---

## 2. What the Existing `sge.platform` Layer Provides

The `sge.platform` package provides **low-level native operations** for buffer manipulation and ETC1 texture compression. This is **not** the backend interface layer -- it is a cross-platform abstraction for performance-critical operations.

### Shared trait definitions (package `sge.platform`, visibility `private[sge]`)

| Trait | File | Methods |
|-------|------|---------|
| `BufferOps` | `core/src/main/scala/sge/platform/BufferOps.scala` | Memory management (3), copy (2), vertex transform (5), vertex find (2) -- 12 total |
| `ETC1Ops` | `core/src/main/scala/sge/platform/ETC1Ops.scala` | Compressed size, PKM header read/write/validate, encode/decode image -- 9 total |

### Platform-specific implementations

Each platform provides a `PlatformOps` object:

```scala
private[sge] object PlatformOps {
  val etc1:   ETC1Ops   = <platform-specific>
  val buffer: BufferOps = <platform-specific>
}
```

- **JVM**: Delegates to `BufferOpsBridge.java`/`ETC1Bridge.java` (JNI → Rust native library)
- **Scala.js**: Pure Scala fallback (no native code)
- **Scala Native**: `@link("sge_native_ops") @extern` C ABI bindings to Rust

---

## 3. Full Method Counts per Core Trait

### Application (22 abstract methods)

Key methods: `getApplicationListener`, `getGraphics/Audio/Input/Files/Net`, `log/error/debug` (6 logging methods), `setLogLevel/getLogLevel`, `getType`, `getVersion`, `getJavaHeap/getNativeHeap`, `getPreferences`, `getClipboard`, `postRunnable`, `exit`, `add/removeLifecycleListener`.

### Graphics (38 abstract methods + 5 convenience defaults)

GL accessors (8), display dimensions (10), frame timing (4), display modes/monitors (9), window management (5), cursor (3), rendering control (3).

### Audio (6 abstract methods)

`newAudioDevice`, `newAudioRecorder`, `newSound`, `newMusic`, `switchOutputDevice`, `getAvailableOutputDevices`.

### Input (36 abstract methods)

Sensors (6), pointers/touch (16), keyboard (4), text input (6), vibration (4), orientation (6), cursor (3).

### Files (10 abstract methods)

`getFileHandle`, 5 type-specific factories (`classpath/internal/external/absolute/local`), 4 storage path/availability queries.

### Net (7 abstract methods)

`sendHttpRequest`, `cancelHttpRequest`, `isHttpRequestPending`, 2 `newServerSocket`, `newClientSocket`, `openURI`.

### Preferences (21 abstract methods)

5 typed putters, `put(Map)`, 10 typed getters (with/without defaults), `get()`, `contains`, `clear`, `remove`, `flush`.

### Clipboard (3 abstract members)

`hasContents`, `contents` (getter), `contents_=` (setter).

### ApplicationLogger (6 abstract methods)

`log/error/debug` each with `(tag, message)` and `(tag, message, exception)` overloads.

---

## 4. The Context Parameter Pattern

### The `Sge` case class

SGE replaces LibGDX's mutable static fields (`Gdx.graphics`, `Gdx.audio`, etc.) with an immutable case class passed as a Scala 3 context parameter:

```scala
final case class Sge private[sge] (
  application: Application,
  graphics:    Graphics,
  audio:       Audio,
  files:       Files,
  input:       Input,
  net:         Net
)
```

The constructor is `private[sge]`, meaning only backend code in the `sge` package can create an `Sge` instance.

### Usage pattern: `(using sge: Sge)`

376 occurrences across 114 files in core. Methods declare:

```scala
def doSomething(args...)(using sge: Sge): ReturnType = {
  sge.graphics.getWidth()
  sge.input.getX()
}
```

### Shadowing gotcha

When `using sge: Sge` is in scope, the `sge` package path is shadowed. Use:
```scala
_root_.sge.utils.JsonReader
_root_.sge.assets.AssetManager
```

### Backend responsibility

1. Create concrete implementations of all core traits
2. Construct an `Sge` instance bundling the 6 subsystem implementations
3. Pass `Sge` as context parameter to `ApplicationListener.create()` and all subsequent lifecycle methods
4. Manage the render loop

---

## 5. Summary: What a New Backend Must Provide

| Component | Abstract Methods | Key Challenge |
|-----------|-----------------|---------------|
| `Application` | 22 | Lifecycle management, threading (`postRunnable`) |
| `Graphics` | 38 | GL context creation, display modes, frame timing |
| `Audio` | 6 | Platform audio API (OpenAL, Web Audio, etc.) |
| `Input` | 36 | Event dispatch, sensor APIs, text input dialogs |
| `Files` | 10 | Classpath/internal/external/local/absolute file access |
| `Net` | 7 | HTTP client, TCP sockets, URI opening |
| `Preferences` | 21 | Persistent storage (file, LocalStorage, SharedPrefs) |
| `Clipboard` | 3 | System clipboard integration |
| `ApplicationLogger` | 6 | Platform-appropriate logging |
| `Sge` construction | 1 | Wire everything together, must be in `sge` package |

**Total: ~150 abstract methods** across the 9 backend-implemented traits.

The `sge.platform` layer (`BufferOps` + `ETC1Ops`) is already handled by core's cross-platform `projectMatrix` -- backends do not need to implement it.
