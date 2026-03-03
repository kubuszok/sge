# LWJGL3 Backend Deep Analysis

Comprehensive analysis of every file in the LibGDX LWJGL3 desktop backend
(`libgdx/backends/gdx-backend-lwjgl3/src/com/badlogic/gdx/backends/lwjgl3/`).

**Total files: 39** (23 main + 11 audio + 5 mock audio)
**Test files: 0** (no test directory exists)

---

## Table of Contents

1. [File Inventory](#1-file-inventory)
2. [Application Lifecycle](#2-application-lifecycle)
3. [Window Management](#3-window-management)
4. [Graphics / GL Wrappers](#4-graphics--gl-wrappers)
5. [Input Handling](#5-input-handling)
6. [File System](#6-file-system)
7. [Networking](#7-networking)
8. [Preferences](#8-preferences)
9. [Audio Subsystem](#9-audio-subsystem)
10. [Utility Classes](#10-utility-classes)
11. [Mock Audio](#11-mock-audio)
12. [External Dependency Map](#12-external-dependency-map)
13. [Platform-Specific vs Shareable Analysis](#13-platform-specific-vs-shareable-analysis)
14. [SGE Porting Recommendations](#14-sge-porting-recommendations)

---

## 1. File Inventory

### Main Directory (23 files)

| # | File | Implements/Extends | ~LOC | Platform-Specific? |
|---|------|--------------------|------|--------------------|
| 1 | `Lwjgl3Application.java` | `Lwjgl3ApplicationBase` (which extends `Application`) | 682 | Yes (GLFW+GL) |
| 2 | `Lwjgl3ApplicationBase.java` | `Application` (interface) | 13 | No (interface) |
| 3 | `Lwjgl3ApplicationConfiguration.java` | extends `Lwjgl3WindowConfiguration` | 389 | Yes (GLFW) |
| 4 | `Lwjgl3ApplicationLogger.java` | `ApplicationLogger` | 57 | No (pure Java) |
| 5 | `Lwjgl3Clipboard.java` | `Clipboard` | 43 | Yes (GLFW) |
| 6 | `Lwjgl3Cursor.java` | `Cursor` | 153 | Yes (GLFW) |
| 7 | `Lwjgl3FileHandle.java` | extends `FileHandle` | 63 | No (pure Java) |
| 8 | `Lwjgl3Files.java` | `Files` | 80 | No (pure Java) |
| 9 | `Lwjgl3GL20.java` | `com.badlogic.gdx.graphics.GL20` | 850 | Yes (LWJGL GL) |
| 10 | `Lwjgl3GL30.java` | `com.badlogic.gdx.graphics.GL30`, extends `Lwjgl3GL20` | 676 | Yes (LWJGL GL) |
| 11 | `Lwjgl3GL31.java` | `com.badlogic.gdx.graphics.GL31`, extends `Lwjgl3GL30` | 388 | Yes (LWJGL GL) |
| 12 | `Lwjgl3GL32.java` | `com.badlogic.gdx.graphics.GL32`, extends `Lwjgl3GL31` | 360 | Yes (LWJGL GL) |
| 13 | `Lwjgl3Graphics.java` | extends `AbstractGraphics`, `Disposable` | 594 | Yes (GLFW+GL) |
| 14 | `DefaultLwjgl3Input.java` | `Lwjgl3Input` (extends `AbstractInput`) | 728 | Yes (GLFW) |
| 15 | `Lwjgl3Input.java` | `Input`, `Disposable` (interface) | 17 | No (interface) |
| 16 | `Lwjgl3Net.java` | `Net` | 100 | Mostly no (Java stdlib) |
| 17 | `Lwjgl3NativesLoader.java` | (standalone) | 31 | Yes (native libs) |
| 18 | `Lwjgl3Preferences.java` | `Preferences` | 192 | No (pure Java) |
| 19 | `Lwjgl3Window.java` | `Disposable` | 525 | Yes (GLFW) |
| 20 | `Lwjgl3WindowAdapter.java` | `Lwjgl3WindowListener` | 41 | No (adapter) |
| 21 | `Lwjgl3WindowConfiguration.java` | (standalone) | 171 | Minimal (type refs) |
| 22 | `Lwjgl3WindowListener.java` | (interface) | 73 | No (interface) |
| 23 | `Sync.java` | (standalone) | 170 | Partial (uses `glfwGetTime`) |

### Audio Subdirectory (11 files)

| # | File | Implements/Extends | ~LOC | Platform-Specific? |
|---|------|--------------------|------|--------------------|
| 24 | `Lwjgl3Audio.java` | `Audio` (interface) | 10 | No (interface) |
| 25 | `OpenALLwjgl3Audio.java` | `Lwjgl3Audio` | 461 | Yes (LWJGL OpenAL) |
| 26 | `OpenALMusic.java` | `Music` (abstract) | 299 | Yes (LWJGL OpenAL) |
| 27 | `OpenALSound.java` | `Sound` | 215 | Yes (LWJGL OpenAL) |
| 28 | `OpenALAudioDevice.java` | `AudioDevice` | 218 | Yes (LWJGL OpenAL) |
| 29 | `OpenALUtils.java` | (standalone) | 54 | Yes (OpenAL constants) |
| 30 | `Mp3.java` | inner `Music` extends `OpenALMusic`, `Sound` extends `OpenALSound` | 143 | **Decoder: No** (JLayer) |
| 31 | `Ogg.java` | inner `Music` extends `OpenALMusic`, `Sound` extends `OpenALSound` | 94 | **Decoder: Partial** (STBVorbis via LWJGL) |
| 32 | `OggInputStream.java` | extends `InputStream` | 469 | **No** (pure Java via JOrbis) |
| 33 | `Wav.java` | inner `Music` extends `OpenALMusic`, `Sound` extends `OpenALSound` | 184 | **No** (pure Java) |
| 34 | `JavaSoundAudioRecorder.java` | `AudioRecorder` | 60 | JVM only (javax.sound) |

### Mock Audio Subdirectory (5 files)

| # | File | Implements/Extends | ~LOC | Platform-Specific? |
|---|------|--------------------|------|--------------------|
| 35 | `MockAudio.java` | `Lwjgl3Audio` | 68 | No (stubs) |
| 36 | `MockAudioDevice.java` | `AudioDevice` | 65 | No (stubs) |
| 37 | `MockAudioRecorder.java` | `AudioRecorder` | 35 | No (stubs) |
| 38 | `MockMusic.java` | `Music` | 89 | No (stubs) |
| 39 | `MockSound.java` | `Sound` | 109 | No (stubs) |

---

## 2. Application Lifecycle

### Lwjgl3Application (682 LOC)

The central class. Implements `Lwjgl3ApplicationBase` (which extends `Application`).

**Constructor flow:**
1. Load ANGLE if configured for GLES20 emulation
2. `initializeGlfw()` -- loads natives, sets error callback, inits GLFW
3. Create audio (OpenAL or MockAudio on failure)
4. Create Files, Net, Clipboard, Sync
5. Create first window via `createWindow()` --> `createGlfwWindow()`
6. Enter `loop()` -- the main game loop
7. On exit: `cleanupWindows()`, `cleanup()`

**Main loop (`loop()`):**
- Audio update every frame
- Iterate all windows, call `window.update()` for each
- `GLFW.glfwPollEvents()` after all windows processed
- Execute application-level runnables
- Handle closed windows (dispose lifecycle listeners, then window)
- Sleep for idle FPS or use `Sync.sync(targetFramerate)`

**Key dependencies:** GLFW, OpenGL (GL11/GL43/etc), LWJGL system callbacks

**Multi-window support:** Yes, via `newWindow()`. Additional windows created deferred via `postRunnable`.

### Lwjgl3ApplicationBase (13 LOC)

Simple interface extending `Application` adding two factory methods:
- `createAudio(config)` -> `Lwjgl3Audio`
- `createInput(window)` -> `Lwjgl3Input`

### Lwjgl3ApplicationConfiguration (389 LOC)

Extends `Lwjgl3WindowConfiguration`. Holds all application-level config:
- Audio settings (simultaneous sources, buffer size/count)
- GL emulation mode (GL20, GL30, GL31, GL32, ANGLE_GLES20)
- Framebuffer format (RGBA bits, depth, stencil, samples)
- FPS settings (foreground, idle)
- Pause behavior (on minimize, on focus loss)
- Preferences directory/type
- HDPI mode
- Debug GL output

Also has **static monitor/display mode queries** that initialize GLFW and use GLFW APIs.

---

## 3. Window Management

### Lwjgl3Window (525 LOC)

Manages a single GLFW window. Holds:
- `windowHandle` (GLFW long)
- `ApplicationListener`
- `Lwjgl3Graphics` and `Lwjgl3Input` instances
- GLFW callbacks: focus, iconify, maximize, close, drop, refresh

**`update()` method** (called each frame from main loop):
1. Run queued runnables
2. Update input (if not iconified)
3. Handle async resize (for glfw_async mode)
4. If should render: update graphics timing, call `listener.render()`, swap buffers
5. Prepare input for next frame

**`makeCurrent()`**: Sets `Gdx.graphics`, `Gdx.gl*`, `Gdx.input` statics + `glfwMakeContextCurrent`.

### Lwjgl3WindowConfiguration (171 LOC)

Plain data class with window settings: position, size, min/max size, resizable, decorated, maximized, icons, fullscreen mode, title, background color, vsync.

### Lwjgl3WindowListener (73 LOC)

Interface for window events: `created`, `iconified`, `maximized`, `focusLost`, `focusGained`, `closeRequested` (returns boolean to cancel), `filesDropped`, `refreshRequested`.

### Lwjgl3WindowAdapter (41 LOC)

Empty default implementation of `Lwjgl3WindowListener`.

---

## 4. Graphics / GL Wrappers

### Lwjgl3GL20 (850 LOC) -- THIN WRAPPER

Implements `com.badlogic.gdx.graphics.GL20`. **Every method is a direct 1:1 delegation** to LWJGL's static OpenGL bindings:

- `GL11.*` for core OpenGL 1.1 (clear, blend, textures, draw, viewport, etc.)
- `GL13.*` for multitexture, compressed textures
- `GL14.*` for blend equation, blend func separate
- `GL15.*` for buffer objects (VBOs)
- `GL20.*` for shaders, programs, uniforms, vertex attribs
- `EXTFramebufferObject.*` for FBOs (GL20 path uses EXT, not core)

Has internal `ByteBuffer`/`FloatBuffer`/`IntBuffer` for array-to-buffer conversions.
Two methods throw `UnsupportedOperationException`: `glGetShaderPrecisionFormat`, `glGetVertexAttribPointerv`, `glShaderBinary`.

### Lwjgl3GL30 (676 LOC) -- THIN WRAPPER

Extends `Lwjgl3GL20`, implements `GL30`. Adds:
- `GL12.*` (3D textures, draw range elements)
- `GL21.*` (non-square matrix uniforms)
- `GL30.*` (FBOs via core, VAOs, transform feedback, etc.)
- `GL31.*` (uniform blocks, instanced drawing, copy buffer)
- `GL32.*` (integer64, buffer parameter i64)
- `GL33.*` (samplers, vertex attrib divisor)
- `GL40.*` (transform feedback objects)
- `GL41.*` (program parameters)
- `GL43.*` (invalidate framebuffer)

**Overrides** FBO methods from GL20 to use core GL30 instead of EXT.

### Lwjgl3GL31 (388 LOC) -- THIN WRAPPER

Extends `Lwjgl3GL30`, implements `GL31`. Adds:
- `GL40.*` (indirect drawing)
- `GL41.*` (program pipelines, program uniforms)
- `GL42.*` (image textures, memory barriers)
- `GL43.*` (compute shaders, program interface query, SSBO)
- `GL46.*` (memory barrier by region)
- `GL32.*` (multisample)

### Lwjgl3GL32 (360 LOC) -- THIN WRAPPER

Extends `Lwjgl3GL31`, implements `GL32`. Adds:
- `KHRBlendEquationAdvanced` (blend barrier)
- `GL43.*` (debug messages, copy image, tex buffer range)
- `GL45.*` (robustness: readnPixels, getnUniform)
- `GL40.*` (indexed blend, min sample shading, tessellation)
- `GL30.*` (enablei/disablei, tex parameter I)
- `GL33.*` (sampler parameter I)
- `GL31.*` (tex buffer)

### Lwjgl3Graphics (594 LOC)

Extends `AbstractGraphics`, implements `Disposable`. Per-window graphics context.

**Key responsibilities:**
- Creates appropriate GL implementation based on config (GL20/GL30/GL31/GL32/ANGLE)
- Tracks framebuffer dimensions (back buffer vs logical for HDPI)
- Frame timing (delta time, FPS counter, frame ID)
- Monitor and display mode queries (delegates to GLFW)
- Fullscreen/windowed mode switching
- Cursor management (delegates to `Lwjgl3Cursor`)
- VSync control
- Cubemap seamless support

**Inner classes:**
- `Lwjgl3DisplayMode` -- wraps a GLFW monitor handle + resolution
- `Lwjgl3Monitor` -- wraps a GLFW monitor handle + virtual position + name

---

## 5. Input Handling

### Lwjgl3Input (17 LOC)

Interface extending `Input` + `Disposable`. Adds:
- `windowHandleChanged(long)` -- re-register callbacks on handle change
- `update()` -- drain event queue
- `prepareNext()` -- reset per-frame state
- `resetPollingStates()` -- clear all pressed/just-pressed state

### DefaultLwjgl3Input (728 LOC)

Extends `AbstractInput`, implements `Lwjgl3Input`.

**GLFW callbacks registered:**
- `GLFWKeyCallback` -- key press/release/repeat
- `GLFWCharCallback` -- character input (Unicode codepoints)
- `GLFWScrollCallback` -- mouse wheel
- `GLFWCursorPosCallback` -- mouse movement (with HDPI scaling)
- `GLFWMouseButtonCallback` -- mouse button press/release (5 buttons)

**Key code mapping:** 245-line `getGdxKeyCode()` switch statement mapping GLFW key constants to LibGDX `Input.Keys` constants.

**Desktop stubs** (return 0/false/no-op): accelerometer, gyroscope, vibrate, rotation, orientation, onscreen keyboard, text input field.

**Single pointer:** Desktop only supports pointer 0 (mouse).

---

## 6. File System

### Lwjgl3Files (80 LOC)

Implements `Files`. Factory for `Lwjgl3FileHandle` instances.
- `externalPath` = `user.home + separator`
- `localPath` = `cwd + separator`
- All file types supported: Classpath, Internal, External, Absolute, Local

**Pure Java -- no native dependencies.** Could be shared across any JVM backend.

### Lwjgl3FileHandle (63 LOC)

Extends `FileHandle`. Overrides `child()`, `sibling()`, `parent()`, `file()` to return `Lwjgl3FileHandle` instances and resolve External/Local paths against `Lwjgl3Files` static paths.

**Pure Java -- no native dependencies.** Trivially shareable.

---

## 7. Networking

### Lwjgl3Net (100 LOC)

Implements `Net`. **Despite the name, has no LWJGL dependency.**

- HTTP: Delegates to `NetJavaImpl` (core LibGDX class using `HttpURLConnection`)
- Sockets: Delegates to `NetJavaServerSocketImpl` / `NetJavaSocketImpl` (core)
- `openURI()`: Platform-aware -- uses `ProcessBuilder("open", ...)` on macOS, `Desktop.browse()` on others, `xdg-open` on Linux

**Almost entirely shareable.** Only `openURI()` needs platform dispatch.

---

## 8. Preferences

### Lwjgl3Preferences (192 LOC)

Implements `Preferences`. Uses `java.util.Properties` with XML serialization.
- Load: `properties.loadFromXML(inputStream)`
- Save: `properties.storeToXML(outputStream, null)`
- Backed by a `FileHandle`

**Pure Java -- completely shareable across JVM backends.** Not usable on JS/Native without a Properties replacement.

---

## 9. Audio Subsystem

### Architecture Overview

```
Lwjgl3Audio (interface, extends Audio)
  |
  +-- OpenALLwjgl3Audio (LWJGL OpenAL implementation)
  |     |
  |     +-- manages: OpenALSound, OpenALMusic, OpenALAudioDevice
  |     +-- format decoders registered by extension:
  |           "ogg" -> Ogg.Sound / Ogg.Music
  |           "wav" -> Wav.Sound / Wav.Music
  |           "mp3" -> Mp3.Sound / Mp3.Music
  |
  +-- MockAudio (headless/no-device fallback)
```

### OpenALLwjgl3Audio (461 LOC) -- CORE AUDIO ENGINE

**LWJGL OpenAL dependencies:** `AL10`, `ALC10`, `AL` capabilities, `SOFTDirectChannels`, `SOFTReopenDevice`, `SOFTXHoldOnDisconnect`, `EXTDisconnect`, `EnumerateAllExt`.

**Initialization:**
1. Open default device via `alcOpenDevice(null)`
2. Create context, make current, create AL capabilities
3. Pre-allocate `simultaneousSources` (default 16) OpenAL sources
4. Set up listener orientation/velocity/position
5. Start daemon thread for device hot-plug monitoring (reconnect, switch)

**Source management:**
- `obtainSource(isMusic)` -- finds idle source, resets it, returns sourceID
- `freeSource(sourceID)` -- stops source, returns to idle pool
- Sound ID tracking via `LongMap<Integer>` and `IntMap<Long>` bidirectional maps

**`update()`:** Called every frame from main loop. Iterates all active `OpenALMusic` instances calling their `update()`.

**Device switching:** Uses `SOFTReopenDevice.alcReopenDeviceSOFT()` for hot-plug support.

### OpenALMusic (299 LOC) -- STREAMING PLAYBACK

Abstract class implementing `Music`. Streams audio in chunks using OpenAL buffer queuing.

- 3 buffers of 40960 bytes each (`bufferSize = 4096 * 10`)
- Subclasses implement `read(byte[])` and `reset()` for format-specific decoding
- `update()`: Unqueues processed buffers, refills them, requeues. Handles looping and completion callbacks.
- Position seeking by reading and discarding data until target position reached

**OpenAL calls:** `alGenBuffers`, `alSourceQueueBuffers`, `alSourceUnqueueBuffers`, `alBufferData`, `alSourcePlay/Pause/Stop`, `alSourcef(AL_GAIN/AL_PITCH)`, `alSource3f(AL_POSITION)`.

### OpenALSound (215 LOC) -- BUFFERED PLAYBACK

Implements `Sound`. Entire audio loaded into single OpenAL buffer.

- `setup(byte[], channels, bitDepth, sampleRate)` -- loads PCM into OpenAL buffer
- `setup(ShortBuffer, ...)` -- alternate for pre-decoded data
- `play(volume)` -- obtains source, binds buffer, plays. Falls back to evicting least-recent sound if no sources available.

### OpenALAudioDevice (218 LOC) -- RAW PCM OUTPUT

Implements `AudioDevice`. For programmatic audio output (e.g., synthesizers, procedural audio).

- Accepts `float[]` or `short[]` samples, converts to bytes
- Uses OpenAL buffer queuing (like Music but driven by `writeSamples` calls)
- Blocks when all buffers are full

### OpenALUtils (54 LOC) -- FORMAT HELPER

Single static method `determineFormat(channels, bitDepth)` mapping to OpenAL format constants:
- Mono/Stereo: 8, 16, 32, 64-bit
- Quad/5.1/6.1/7.1: 8, 16, 32-bit
- Uses `EXTFloat32`, `EXTDouble`, `EXTMCFormats` extensions

### Audio Decoders

#### Mp3.java (143 LOC)

Uses **JLayer** (`javazoom.jl.decoder.*`):
- `Mp3.Sound`: Decodes entire file to byte array via `Bitstream`/`MP3Decoder`/`OutputBuffer`
- `Mp3.Music`: Streaming decoder, creates new `Bitstream` on each `reset()`

**JLayer is pure Java.** The decoder logic is completely platform-independent. Only the OpenAL integration (via `OpenALSound`/`OpenALMusic` superclass) is platform-specific.

#### Ogg.java (94 LOC)

Two different decoders used:
- `Ogg.Sound`: Uses **STBVorbis** via LWJGL (`org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory`) -- **native dependency**
- `Ogg.Music`: Uses **OggInputStream** (JOrbis) -- **pure Java**

#### OggInputStream.java (469 LOC)

Pure Java Ogg Vorbis decoder using **JOrbis** (`com.jcraft.jogg.*`, `com.jcraft.jorbis.*`).
- Extends `InputStream`
- Reads Ogg pages, decodes Vorbis packets, converts float PCM to 16-bit signed integers
- Handles endianness (big/little)
- Reuses buffers across stream instances for looping

**Completely platform-independent.** No LWJGL dependency (only LWJGL `BufferUtils` for the PCM buffer, which could be replaced).

#### Wav.java (184 LOC)

- `Wav.Sound`: Reads entire WAV via `WavInputStream`, loads PCM. Detects MP3-in-WAV (type 0x0055) and delegates.
- `Wav.Music`: Streaming via `WavInputStream`.
- `WavInputStream`: Parses RIFF/WAVE headers, supports PCM (8/16-bit) and IEEE float (32/64-bit).

**Completely platform-independent** except for OpenAL superclass.

### JavaSoundAudioRecorder (60 LOC)

Uses `javax.sound.sampled.TargetDataLine` for microphone input. **JVM-only** (not available on JS or Native).

---

## 10. Utility Classes

### Sync (170 LOC) -- FRAME RATE LIMITER

Adaptive frame rate limiter using sleep + yield strategy.

- Uses `glfwGetTime()` for high-precision timing (only GLFW dependency)
- `RunningAvg` inner class for adaptive sleep/yield duration tracking
- Windows workaround: spawns daemon thread to improve `Thread.sleep()` precision
- Could trivially be ported to use `System.nanoTime()` instead of `glfwGetTime()`

### Lwjgl3Clipboard (43 LOC)

Clipboard via GLFW: `glfwGetClipboardString` / `glfwSetClipboardString`.

### Lwjgl3Cursor (153 LOC)

Custom and system cursor management via GLFW:
- Custom cursors from `Pixmap` via `GLFWImage` + `glfwCreateCursor`
- System cursors: Arrow, Crosshair, Hand, HResize, VResize, Ibeam, NWSEResize, NESWResize, AllResize, NotAllowed, None
- `None` cursor uses `GLFW_CURSOR_HIDDEN` input mode

### Lwjgl3NativesLoader (31 LOC)

Sets `org.lwjgl.input.Mouse.allowNegativeMouseCoords` property, delegates to `GdxNativesLoader.load()`.

### Lwjgl3ApplicationLogger (57 LOC)

Implements `ApplicationLogger`. Prints to `System.out` (log/debug) and `System.err` (error). **Pure Java, completely shareable.**

---

## 11. Mock Audio

Five no-op implementations for headless/disabled-audio mode:

| Class | Implements | Purpose |
|-------|-----------|---------|
| `MockAudio` | `Lwjgl3Audio` | Returns mock devices/sounds/music |
| `MockAudioDevice` | `AudioDevice` | No-op write/dispose |
| `MockAudioRecorder` | `AudioRecorder` | No-op read/dispose |
| `MockMusic` | `Music` | No-op play/pause/stop |
| `MockSound` | `Sound` | No-op play/loop/stop, returns 0 for IDs |

All pure Java, completely shareable.

---

## 12. External Dependency Map

### LWJGL Modules Used

| LWJGL Module | Files Using It | Purpose |
|-------------|---------------|---------|
| `org.lwjgl.glfw.GLFW` | Application, Config, Graphics, Input, Window, Clipboard, Cursor, Sync | Window management, input, timing |
| `org.lwjgl.glfw.GLFW*Callback` | Input (5 callbacks), Window (6 callbacks), Graphics (1) | Event handling |
| `org.lwjgl.glfw.GLFWImage` | Cursor, Window | Custom cursors, window icons |
| `org.lwjgl.glfw.GLFWVidMode` | Config | Display mode enumeration |
| `org.lwjgl.opengl.GL11-GL46` | GL20, GL30, GL31, GL32, Graphics, Application | OpenGL rendering |
| `org.lwjgl.opengl.EXTFramebufferObject` | GL20 | FBO support (GL20 path) |
| `org.lwjgl.opengl.GL` | Application | Create GL capabilities |
| `org.lwjgl.opengl.GLUtil` | Application | Debug message callback |
| `org.lwjgl.opengl.KHRDebug` | Application, GL32 | Debug output |
| `org.lwjgl.opengl.AMDDebugOutput` | Application | Debug output (AMD) |
| `org.lwjgl.opengl.ARBDebugOutput` | Application | Debug output (ARB) |
| `org.lwjgl.opengl.KHRBlendEquationAdvanced` | GL32 | Blend barrier |
| `org.lwjgl.openal.AL10/AL11` | OpenALLwjgl3Audio, OpenALMusic, OpenALSound, OpenALAudioDevice | Audio playback |
| `org.lwjgl.openal.ALC10` | OpenALLwjgl3Audio | Audio device/context |
| `org.lwjgl.openal.AL/ALC` | OpenALLwjgl3Audio | Capabilities |
| `org.lwjgl.openal.SOFT*` | OpenALLwjgl3Audio | Direct channels, reopen device, hold on disconnect |
| `org.lwjgl.openal.EXT*` | OpenALLwjgl3Audio, OpenALUtils | Disconnect detection, float/double/multichannel formats |
| `org.lwjgl.stb.STBVorbis` | Ogg (Sound only) | Vorbis decoding (native) |
| `org.lwjgl.BufferUtils` | Config, Graphics, OpenALLwjgl3Audio, OpenALMusic, OpenALAudioDevice, OggInputStream | NIO buffer allocation |
| `org.lwjgl.PointerBuffer` | Config, Graphics, GL32 | Monitor enumeration, pointers |
| `org.lwjgl.system.Configuration` | Config, Graphics | GLFW library name config |
| `org.lwjgl.system.Callback` | Application | Debug callback cleanup |
| `org.lwjgl.system.MemoryUtil` | GL32 | Debug message string decoding |

### Non-LWJGL External Dependencies

| Library | Files Using It | Purpose |
|---------|---------------|---------|
| `javazoom.jl.decoder.*` (JLayer) | Mp3 | MP3 decoding (pure Java) |
| `com.jcraft.jogg.*` / `com.jcraft.jorbis.*` (JOrbis) | OggInputStream | Ogg Vorbis streaming decode (pure Java) |
| `javax.sound.sampled.*` (Java Sound) | JavaSoundAudioRecorder | Microphone recording (JVM only) |
| `java.awt.Desktop` | Lwjgl3Net | URI opening |

---

## 13. Platform-Specific vs Shareable Analysis

### Fully Platform-Specific (require LWJGL / native APIs)

| Component | Reason | SGE Equivalent Needed |
|-----------|--------|----------------------|
| GL20/GL30/GL31/GL32 | Direct LWJGL OpenGL bindings | Yes, per platform (LWJGL, WebGL, etc.) |
| Lwjgl3Graphics | GLFW window + GL context management | Yes, per platform |
| DefaultLwjgl3Input | GLFW keyboard/mouse callbacks | Yes, per platform |
| Lwjgl3Application | GLFW init, main loop, window creation | Yes, per platform |
| Lwjgl3Clipboard | GLFW clipboard API | Yes, per platform |
| Lwjgl3Cursor | GLFW cursor API | Yes, per platform |
| Lwjgl3Window | GLFW window lifecycle | Yes, per platform |
| OpenALLwjgl3Audio | LWJGL OpenAL device/context/source management | Yes, per platform |
| OpenALMusic | LWJGL OpenAL buffer queuing | Yes, per platform |
| OpenALSound | LWJGL OpenAL buffer playback | Yes, per platform |
| OpenALAudioDevice | LWJGL OpenAL raw PCM output | Yes, per platform |
| OpenALUtils | OpenAL format constants | Yes, per platform |

### Shareable Across JVM Backends (pure Java)

| Component | Notes |
|-----------|-------|
| Lwjgl3Files / Lwjgl3FileHandle | Pure Java `File` I/O |
| Lwjgl3Preferences | `java.util.Properties` XML |
| Lwjgl3ApplicationLogger | `System.out`/`System.err` |
| Lwjgl3Net (mostly) | `HttpURLConnection`, sockets; only `openURI()` varies |
| Wav decoder | RIFF parser + raw PCM read |
| Mp3 decoder (JLayer) | Pure Java MP3 decoding |
| OggInputStream (JOrbis) | Pure Java Ogg Vorbis streaming decode |
| Mock audio (all 5 files) | No-op stubs |

### Partially Shareable

| Component | Shareable Part | Platform Part |
|-----------|---------------|---------------|
| Sync | Timing algorithm | Uses `glfwGetTime()` (trivially replaceable) |
| Ogg.Sound | -- | Uses `STBVorbis` native decoder |
| Ogg.Music | OggInputStream is pure Java | Only OpenAL superclass is platform-specific |
| Lwjgl3ApplicationConfiguration | Config data model | Static monitor queries use GLFW |
| Lwjgl3WindowConfiguration | Config data model | References to GLFW-specific types |

---

## 14. SGE Porting Recommendations

### GL Implementation Strategy

The GL20/GL30/GL31/GL32 files are **purely mechanical 1:1 wrappers**. Each method body is a single delegation to the corresponding LWJGL static method. For SGE:

- **JVM backend**: These can be ported almost verbatim, just mapping LWJGL OpenGL calls
- **JS backend**: WebGL2RenderingContext provides similar API
- **Native backend**: Could use raw OpenGL bindings or a Scala Native OpenGL wrapper

Total GL wrapper LOC: ~2,274. This is high-volume but low-complexity code. Consider code generation.

### Audio Architecture for SGE

The audio system has a clean layered architecture that can be exploited:

```
Layer 1: Format Decoders (Mp3, Ogg, Wav)  -- SHAREABLE
    |
    v
Layer 2: Audio Abstractions (OpenALMusic, OpenALSound)  -- PLATFORM-SPECIFIC
    |
    v
Layer 3: Audio Engine (OpenALLwjgl3Audio)  -- PLATFORM-SPECIFIC
```

**Recommendation:** Extract the decoders into shared core code:
- `Wav.WavInputStream` -- pure Java RIFF parser (184 LOC)
- `Mp3` decoding via JLayer -- pure Java (143 LOC, but needs JLayer dependency)
- `OggInputStream` via JOrbis -- pure Java (469 LOC, needs JOrbis dependency)
- `Ogg.Sound` uses STBVorbis (native) -- JVM/Native only, not JS

The OpenAL layer (`OpenALSound`, `OpenALMusic`, `OpenALAudioDevice`, `OpenALLwjgl3Audio`) would be JVM/Native specific. For JS, Web Audio API would replace OpenAL.

### Networking

`Lwjgl3Net` is nearly platform-independent. The only LWJGL-specific aspect is its name. For SGE:
- HTTP via `NetJavaImpl` is already in core
- Sockets via `NetJavaServerSocketImpl`/`NetJavaSocketImpl` are already in core
- Only `openURI()` needs platform dispatch

### Files and Preferences

Both `Lwjgl3Files`/`Lwjgl3FileHandle` and `Lwjgl3Preferences` are pure Java. They can be directly shared across JVM and Native backends. JS backend would need browser storage (localStorage) for preferences.

### Window System

The multi-window architecture (`Lwjgl3Window` + `Lwjgl3WindowListener`) is desktop-specific. For SGE:
- Single-window model for mobile/web
- Multi-window optional for desktop
- The listener pattern (`Lwjgl3WindowListener`) is a good abstraction to keep

### Frame Rate Limiter

`Sync` is well-isolated with only a `glfwGetTime()` dependency. Port to use `System.nanoTime()` for a platform-agnostic version. The adaptive sleep/yield algorithm is valuable and non-trivial.

### Input System

`DefaultLwjgl3Input` at 728 LOC is the largest single file after the GL wrappers. The key mapping table (~245 lines) maps GLFW constants to LibGDX key codes -- this is mechanical but must be done per platform. The event queue pattern (`InputEventQueue`) is already in core and shareable.

### Estimated Total Porting Effort

| Category | Files | ~LOC | Difficulty |
|----------|-------|------|-----------|
| GL wrappers | 4 | 2,274 | Low (mechanical, consider codegen) |
| Application lifecycle | 3 | 1,084 | High (complex GLFW interaction) |
| Window management | 4 | 810 | High (GLFW window API) |
| Graphics context | 1 | 594 | Medium (GLFW + GL setup) |
| Input | 2 | 745 | Medium (GLFW callbacks + key map) |
| Audio engine | 4 | 993 | High (OpenAL state management) |
| Audio decoders | 4 | 890 | Low (mostly shareable) |
| Files/Prefs/Net/Logger | 5 | 492 | Low (mostly pure Java) |
| Clipboard/Cursor/Sync | 3 | 366 | Low-Medium |
| Mock audio | 5 | 366 | Trivial |
| Config classes | 3 | 573 | Low (data classes) |
| Interfaces/adapters | 4 | 144 | Trivial |
| **TOTAL** | **39** | **~8,331** | |
