# Headless Backend Analysis

Analysis of `libgdx/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/`.

## Summary

The headless backend contains **15 Java files** totaling approximately **1,050 lines of code**. It implements the full LibGDX `Application` interface using stub/mock objects for audio, graphics, and input, while providing real implementations for files, networking, preferences, and the main application loop.

The backend is organized into two layers:
- **Top-level** (8 files): Application shell, configuration, files, networking, preferences
- **Mock subdirectories** (7 files): No-op stubs for audio, graphics, input

---

## File-by-File Analysis

### Top-Level Classes

#### 1. HeadlessApplication (~160 lines)
- **Implements**: `Application`
- **Functionality**: The central application class. Creates a dedicated `Thread` that runs the main game loop. Wires together all subsystems (files, net, audio, graphics, input) and sets the global `Gdx.*` static fields. The main loop calls `listener.render()` at the configured frame rate (default 60 fps), using `Thread.sleep` for pacing. Handles lifecycle events (create, pause, dispose) and a runnable queue for cross-thread task posting.
- **Platform dependencies**:
  - `java.lang.Thread` (game loop thread) -- **JVM-only** (no Scala.js equivalent)
  - `java.lang.Runtime.getRuntime()` for `getJavaHeap()` -- **JVM-only**
  - `synchronized` blocks -- JVM threading primitive
  - `HeadlessNativesLoader.load()` -- loads JNI native library

#### 2. HeadlessApplicationConfiguration (~13 lines)
- **Implements**: Plain class (no interface)
- **Functionality**: Configuration holder with three fields:
  - `updatesPerSecond` (default 60)
  - `preferencesDirectory` (default `.prefs/`)
  - `maxNetThreads` (default `Integer.MAX_VALUE`)
- **Platform dependencies**: None. Pure data class.

#### 3. HeadlessApplicationLogger (~40 lines)
- **Implements**: `ApplicationLogger`
- **Functionality**: Logs messages to `System.out` (log, debug) and `System.err` (error) with `[tag] message` format. Prints stack traces via `exception.printStackTrace()`.
- **Platform dependencies**:
  - `System.out` / `System.err` -- available on all platforms
  - `exception.printStackTrace(PrintStream)` -- JVM-specific overload, but `printStackTrace()` (no-arg) is cross-platform
  - **Verdict**: Mostly cross-platform. The `PrintStream` overload would need a trivial adjustment for JS/Native.

#### 4. HeadlessFileHandle (~46 lines)
- **Implements**: Extends `FileHandle` (concrete class)
- **Functionality**: Overrides `child()`, `sibling()`, `parent()`, and `file()` to return `HeadlessFileHandle` instances. The `file()` method prepends external/local path prefixes.
- **Platform dependencies**:
  - `java.io.File` -- **JVM-only** (not available on Scala.js or Scala Native without polyfill)

#### 5. HeadlessFiles (~53 lines)
- **Implements**: `Files`
- **Functionality**: Factory for `HeadlessFileHandle` instances. Resolves external path to `System.getProperty("user.home")` and local path to `new File("").getAbsolutePath()`.
- **Platform dependencies**:
  - `java.io.File` -- **JVM-only**
  - `System.getProperty("user.home")` -- **JVM-only**

#### 6. HeadlessNativesLoader (~11 lines)
- **Implements**: Plain class (no interface)
- **Functionality**: Trivial wrapper that calls `GdxNativesLoader.load()`, which uses `SharedLibraryLoader` to extract and load the `gdx` native library (JNI).
- **Platform dependencies**: **JVM-only** (JNI native library loading). Not needed in SGE.

#### 7. HeadlessNet (~75 lines)
- **Implements**: `Net`
- **Functionality**: Delegates HTTP requests to `NetJavaImpl` (using `java.net.HttpURLConnection` and `ThreadPoolExecutor`). Socket operations delegate to `NetJavaServerSocketImpl` and `NetJavaSocketImpl`. The `openURI()` method uses `java.awt.Desktop.browse()`.
- **Platform dependencies**:
  - `java.net.HttpURLConnection` -- **JVM-only**
  - `java.util.concurrent.ThreadPoolExecutor` -- **JVM-only**
  - `java.net.ServerSocket` / `java.net.Socket` -- **JVM-only**
  - `java.awt.Desktop` / `java.awt.GraphicsEnvironment` -- **JVM-only** (AWT)
  - This is the **least portable** class in the headless backend.

#### 8. HeadlessPreferences (~175 lines)
- **Implements**: `Preferences`
- **Functionality**: Full key-value store backed by `java.util.Properties`. Loads from and saves to XML files via `properties.loadFromXML()` / `storeToXML()`. Supports boolean, integer, long, float, and string values.
- **Platform dependencies**:
  - `java.util.Properties` -- **JVM-only** (not in Scala.js javalib)
  - `properties.loadFromXML()` / `storeToXML()` -- **JVM-only** XML serialization

---

### Mock Audio Classes (`mock/audio/`)

#### 9. MockAudio (~48 lines)
- **Implements**: `Audio` (extends `Disposable`)
- **Functionality**: Factory returning mock instances. `switchOutputDevice()` returns true, `getAvailableOutputDevices()` returns empty array, `dispose()` is no-op.
- **Platform dependencies**: **None**. Pure stubs.

#### 10. MockAudioDevice (~48 lines)
- **Implements**: `AudioDevice` (extends `Disposable`)
- **Functionality**: All methods are no-ops. `isMono()` returns false, `getLatency()` returns 0.
- **Platform dependencies**: **None**. Pure stubs.

#### 11. MockAudioRecorder (~18 lines)
- **Implements**: `AudioRecorder` (extends `Disposable`)
- **Functionality**: Both methods are no-ops.
- **Platform dependencies**: **None**. Pure stubs.

#### 12. MockMusic (~72 lines)
- **Implements**: `Music` (extends `Disposable`)
- **Functionality**: All methods are no-ops. Getters return false/0.
- **Platform dependencies**: **None**. Pure stubs.

#### 13. MockSound (~92 lines)
- **Implements**: `Sound` (extends `Disposable`)
- **Functionality**: All methods are no-ops. `play()` and `loop()` return 0.
- **Platform dependencies**: **None**. Pure stubs.

---

### Mock Graphics Class (`mock/graphics/`)

#### 14. MockGraphics (~208 lines)
- **Implements**: Extends `AbstractGraphics` which implements `Graphics`
- **Functionality**: The most substantial mock. Tracks frame timing (`deltaTime`, `fps`, `frameId`) using `System.nanoTime()`. Provides `updateTime()` and `incrementFrameId()` called by `HeadlessApplication`. `setForegroundFPS()` computes target render interval. All GL methods return null. `getType()` returns `GraphicsType.Mock`.
- **Platform dependencies**:
  - `System.nanoTime()` -- available on JVM and Scala Native; on Scala.js use `performance.now()`
  - **Mostly cross-platform**, with the nanoTime caveat for JS (replaceable with `TimeUtils.nanoTime()`)

---

### Mock Input Class (`mock/input/`)

#### 15. MockInput (~163 lines)
- **Implements**: `Input`
- **Functionality**: All methods return zero/false/no-op. `getInputProcessor()` lazily creates an `InputAdapter`. `getNativeOrientation()` returns `Orientation.Landscape`.
- **Platform dependencies**: **None**. Pure stubs.

---

## Platform Independence Assessment

### Fully Cross-Platform (can live in `core`)

| Class | Lines | Notes |
|-------|-------|-------|
| `HeadlessApplicationConfiguration` | 13 | Pure data |
| `MockAudio` | 48 | Pure stubs |
| `MockAudioDevice` | 48 | Pure stubs |
| `MockAudioRecorder` | 18 | Pure stubs |
| `MockMusic` | 72 | Pure stubs |
| `MockSound` | 92 | Pure stubs |
| `MockGraphics` | 208 | Uses `System.nanoTime()` -- replaceable with `TimeUtils.nanoTime()` |
| `MockInput` | 163 | Pure stubs |
| **Total** | **~662** | **8 of 15 files are fully cross-platform** |

### JVM-Only (require platform-specific alternatives)

| Class | Lines | JVM Dependencies |
|-------|-------|------------------|
| `HeadlessApplication` | 160 | `Thread`, `Runtime`, `synchronized` |
| `HeadlessApplicationLogger` | 40 | `PrintStream` overload (minor) |
| `HeadlessFileHandle` | 46 | `java.io.File` |
| `HeadlessFiles` | 53 | `java.io.File`, `System.getProperty` |
| `HeadlessNativesLoader` | 11 | JNI native loading |
| `HeadlessNet` | 75 | `HttpURLConnection`, `java.awt.Desktop`, sockets |
| `HeadlessPreferences` | 175 | `java.util.Properties`, XML serialization |
| **Total** | **~560** | **7 of 15 files are JVM-only** |

---

## Interface Contract Summary

A minimal SGE backend must implement these core traits:

| Interface | Headless Impl | Strategy |
|-----------|---------------|----------|
| `Application` | `HeadlessApplication` | Real impl (main loop, lifecycle, runnables) |
| `Graphics` | `MockGraphics` | Mock (frame timing only, no GL) |
| `Audio` | `MockAudio` | Mock (returns mock sub-objects) |
| `Input` | `MockInput` | Mock (all zeros/false) |
| `Files` | `HeadlessFiles` + `HeadlessFileHandle` | Real impl (filesystem access) |
| `Net` | `HeadlessNet` | Real impl (HTTP, sockets) |
| `Preferences` | `HeadlessPreferences` | Real impl (Properties-based persistence) |
| `ApplicationLogger` | `HeadlessApplicationLogger` | Real impl (stdout/stderr) |
| `Clipboard` | (returns null) | Not implemented |

---

## Recommendations for SGE

### 1. Move mock implementations to `core` shared sources

The 8 cross-platform mock classes should live in `core/src/main/scala/sge/mock/` or `core/src/main/scala/sge/testing/`. They contain zero platform-specific code and are useful for:
- Unit testing on all platforms (JVM, JS, Native)
- Server-side game logic
- CI environments without displays

### 2. Platform-specific classes need per-platform implementations

- **HeadlessApplication**: The main loop could use platform-agnostic scheduling (Gears async)
- **HeadlessFiles**: Already addressed by SGE's cross-platform `FileHandle`
- **HeadlessNet**: Needs HTTP client per platform (JVM: HttpURLConnection, JS: fetch, Native: curl)
- **HeadlessPreferences**: Replace `Properties` with JSON-based storage using `sge.utils.JsonReader`/`JsonValue`

### 3. HeadlessNativesLoader is not needed

SGE already has `sge.platform.PlatformOps` for native operations. The JNI loader pattern is a LibGDX-specific concern replaced by the platform module architecture.

### 4. MockGraphics timing should use `TimeUtils.nanoTime()`

Replace `System.nanoTime()` with `TimeUtils.nanoTime()` which is already cross-platform in SGE. This is the only change needed to make MockGraphics fully portable.

### 5. HeadlessApplicationLogger is nearly cross-platform

The only issue is `exception.printStackTrace(System.out)` which takes a `PrintStream`. Use the zero-arg `printStackTrace()` or `System.out.println(exception.getMessage())` for the cross-platform version.
