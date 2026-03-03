# Cross-Backend Comparison & Sharing Analysis

**Date**: 2026-03-03
**Purpose**: Identify what backend functionality can be shared in SGE core vs what must stay platform-specific.
**Input**: Individual analyses of all 6 LibGDX backends + SGE core interfaces.

---

## 1. Backend Overview

| Backend | Files | ~LOC | Primary Platform | SGE Target |
|---------|------:|-----:|-----------------|-----------|
| LWJGL3 | 39 | 8,331 | Desktop JVM | `backend-desktop-jvm` |
| LWJGL (legacy) | 32 | ~6,000 | Desktop JVM (old) | **Skip** (LWJGL3 is strictly superior) |
| GWT | 24+78 emu | ~5,000+emu | Browser | `backend-browser` (Scala.js) |
| Android | 50 | 8,500 | Mobile (Android) | `backend-android` (Scala JVM on Android) |
| iOS/RoboVM | 38 | 3,950 | Mobile (iOS) | `backend-ios` (Scala JVM via MobiVM) |
| Headless | 15 | 1,050 | Testing/Server | `core` (mock objects) + platform-specific shell |

---

## 2. Subsystem Comparison Matrix

### 2.1 Application Lifecycle

| Feature | LWJGL3 | GWT | Android | iOS | Headless |
|---------|--------|-----|---------|-----|----------|
| Entry point | GLFW init + main loop | `requestAnimationFrame` | Activity.onCreate | UIApplicationDelegate | Thread + sleep loop |
| Main loop | Synchronous poll+render | rAF callback | GLSurfaceView.Renderer.onDrawFrame | GLKViewController delegate | Thread.sleep cycle |
| Runnable queue | `synchronized` ArrayList | JS array | `synchronized` ArrayList | `synchronized` ArrayList | `synchronized` ArrayList |
| Multi-window | Yes (GLFW windows) | No | No | No | No |
| Pause/resume | Window focus/iconify | visibilitychange | Activity lifecycle | UIApplication delegate | N/A |

**Sharable in core**: Runnable queue management, lifecycle listener dispatch, log level state.
**Platform-specific**: Entry point, main loop mechanism, pause/resume detection.

### 2.2 Graphics / GL

| Feature | LWJGL3 | GWT | Android | iOS |
|---------|--------|-----|---------|-----|
| GL binding | LWJGL static methods | WebGL context methods | android.opengl.GLES20/30 | RoboVM native methods |
| GL20 LOC | 850 | ~800 | 705 | 437 |
| GL30 LOC | 676 | ~600 | 872 | 249 |
| GL31/GL32 | Yes (748 LOC) | No | No | No |
| Handle mapping | Direct ints | **Int→JS object maps** | Direct ints | Direct ints |
| Context creation | GLFW+GL capabilities | Canvas.getContext("webgl") | EGL via GLSurfaceView | EAGLContext |
| Frame timing | System.nanoTime / glfwGetTime | performance.now | System.nanoTime | GLKViewController |
| Display modes | GLFW monitor queries | screen.width/height | Display/DisplayMetrics | UIScreen |
| HiDPI | GLFW content scale | devicePixelRatio | DisplayMetrics.density | UIScreen.nativeScale |
| Fullscreen | GLFW fullscreen mode | Fullscreen API | Always fullscreen | Always fullscreen |
| Safe insets | N/A | N/A | DisplayCutout (API 28+) | UIEdgeInsets (notch) |
| Context loss | N/A (desktop) | Possible (WebGL) | EGL context loss | GLKit manages |

**Sharable in core**:
- Frame timing (delta, FPS, frameId) — already in `AbstractGraphics`
- DisplayMode / Monitor data classes
- BufferFormat data class
- Continuous rendering flag logic
- GL version detection helpers

**Platform-specific**: GL bindings (100% per-platform), context creation, display mode enumeration, HiDPI handling, safe insets.

**Key insight — GL bindings**: All ~2,274+ LOC of GL wrappers per platform are purely mechanical 1:1 delegations. Consider **code generation** for all platforms.

**Key insight — WebGL handle mapping**: Scala.js GL implementation needs the int→JS-object mapping pattern (WebGL returns JS objects, but the GL20 interface uses int handles). All other platforms use direct int handles.

### 2.3 Audio

| Feature | LWJGL3 | GWT | Android | iOS |
|---------|--------|-----|---------|-----|
| Sound API | OpenAL (LWJGL) | Web Audio API | SoundPool | ObjectAL (OpenAL wrapper) |
| Music API | OpenAL buffer streaming | Web Audio API | MediaPlayer | AVAudioPlayer (via ObjectAL) |
| AudioDevice | OpenAL raw PCM output | **Not supported** | AudioTrack | OpenAL via ObjectAL |
| AudioRecorder | javax.sound (JVM only) | **Not supported** | AudioRecord | **Not supported** |
| Format decoders | Mp3 (JLayer), Ogg (JOrbis/STBVorbis), Wav | Browser-native decode | Android-native decode | iOS-native decode |
| Device switching | SOFT_reopenDevice ext | setSinkId (Chrome) | N/A | N/A |
| Source management | Pool of N sources | Per-element sources | SoundPool (max 8) | ALChannelSource pool |

**Sharable in core**:
- **Audio format decoders** (biggest sharing opportunity):
  - `Wav.WavInputStream` — pure Java RIFF parser, ~184 LOC, works on JVM+Native
  - `Mp3` decoder via JLayer — pure Java, ~143 LOC, works on JVM+Native
  - `OggInputStream` via JOrbis — pure Java, ~469 LOC, works on JVM+Native
  - Total: ~796 LOC of shareable decoder code
- Mock audio (5 classes, ~366 LOC) — already platform-independent
- Pan-to-stereo-volume calculation (identical formula in Android, iOS, desktop)
- Float-to-short PCM sample conversion
- Sound/Music base traits with shared state management

**Platform-specific**: OpenAL engine (JVM+Native), Web Audio (JS), SoundPool/MediaPlayer (Android), ObjectAL (iOS).

**Key insight**: On JS and mobile, format decoding is handled by the platform natively (browser decodes MP3/Ogg/WAV, Android MediaPlayer/SoundPool decode natively, iOS AVAudioPlayer decodes natively). The JLayer/JOrbis decoders are only needed for JVM desktop and Scala Native where we use OpenAL directly.

### 2.4 Input

| Feature | LWJGL3 | GWT | Android | iOS |
|---------|--------|-----|---------|-----|
| Keyboard | GLFW key callback | DOM keydown/keyup | Android KeyEvent | UIKey/UIPress |
| Mouse | GLFW mouse callbacks | DOM mouse events | MotionEvent | N/A (touch only) |
| Touch | N/A (mouse = pointer 0) | DOM touch events | MotionEvent (20 pointers) | UITouch (20 pointers) |
| Gamepad | Not in backend | Not in backend | Not in backend | Not in backend |
| Accelerometer | Stub (returns 0) | Generic Sensor API | SensorManager | UIAccelerometer (deprecated) |
| Gyroscope | Stub (returns 0) | Generic Sensor API | SensorManager | N/A (not implemented) |
| Vibration | Stub (no-op) | N/A | Vibrator + VibrationEffect | Core Haptics + UIImpactFeedback |
| Cursor capture | GLFW cursor mode | Pointer Lock API | N/A | N/A |
| Text input | N/A | DOM events | IME + AlertDialog | UITextField + UIAlertController |
| Key mapping LOC | ~245 lines | ~200 lines | ~200 lines | ~270 lines |

**Sharable in core**:
- `AbstractInput` base class (already shared)
- `InputEventQueue` (already in core)
- Key event / touch event data structures
- Button mapping constants
- Default implementations for unsupported features (accelerometer returns 0 on desktop, etc.)

**Platform-specific**: Key code mapping tables, event source (GLFW/DOM/Android/UIKit), text input UI, sensor APIs, vibration.

**Key insight — Key mapping**: Each platform has ~200-270 lines of mechanical key mapping. These could potentially be generated from a single mapping table definition.

### 2.5 Files

| Feature | LWJGL3 | GWT | Android | iOS |
|---------|--------|-----|---------|-----|
| Internal | Classpath/CWD | Preloader (HTTP) | AssetManager (APK) | NSBundle |
| External | `~/` (user home) | Not supported | `/sdcard/` | `$HOME/Documents/` |
| Local | CWD | Not supported | `filesDir` | `$HOME/Library/local/` |
| Absolute | Yes | Not supported | Yes | Yes |
| Classpath | Yes | Via preloader | Via AssetManager | Via NSBundle |
| Write support | Full | Not supported | External + Local | External + Local |

**Sharable in core**:
- `FileHandle` base class (already shared)
- `FileType` enum (already in core)
- Path resolution pattern (prefix + relative path)

**Platform-specific**: Path resolution, read implementation (classpath vs HTTP fetch vs AssetManager vs NSBundle).

**Key insight**: For JVM (desktop, Android, iOS/MobiVM), file handling is very similar (java.io.File based). GWT/Scala.js is the outlier requiring HTTP-based asset loading.

### 2.6 Networking

| Feature | LWJGL3 | GWT | Android | iOS | Headless |
|---------|--------|-----|---------|-----|----------|
| HTTP | NetJavaImpl | XMLHttpRequest | NetJavaImpl | NetJavaImpl | NetJavaImpl |
| TCP Sockets | NetJavaSocketImpl | **Not supported** | NetJavaSocketImpl | NetJavaSocketImpl | NetJavaSocketImpl |
| Open URI | ProcessBuilder("open") | window.open | Intent.ACTION_VIEW | UIApplication.openURL | Desktop.browse |

**Sharable in core**: `NetJavaImpl`, `NetJavaServerSocketImpl`, `NetJavaSocketImpl` — **already shared across JVM backends**. Only `openURI()` is platform-specific.

**Key insight**: Networking is the **most shareable** subsystem. 90% of the code is already shared. SGE can have:
- JVM shared: `NetJavaImpl` (desktop + Android + iOS via MobiVM)
- JS: Fetch API wrapper
- Native: libcurl or similar
- `openURI()`: 1-method platform dispatch

### 2.7 Preferences

| Feature | LWJGL3 | GWT | Android | iOS | Headless |
|---------|--------|-----|---------|-----|----------|
| Storage | java.util.Properties XML | localStorage | SharedPreferences | NSMutableDictionary .plist | java.util.Properties XML |
| LOC | 192 | ~100 | 169 | 194 | 175 |

**Sharable in core**: The Preferences trait interface. All implementations are ~170 LOC of trivial get/put/flush logic.

**Key insight**: A JSON-based Preferences implementation using `sge.utils.JsonValue` could be cross-platform (JVM + Native), replacing the Properties XML approach. JS would use localStorage, Android would use SharedPreferences, iOS would use NSMutableDictionary.

### 2.8 Clipboard

| Feature | LWJGL3 | GWT | Android | iOS |
|---------|--------|-----|---------|-----|
| API | GLFW clipboard | Navigator Clipboard API | ClipboardManager | UIPasteboard |
| LOC | 43 | ~40 | 52 | (in IOSApplication) |

**Sharable in core**: Clipboard trait (3 methods). All implementations are tiny (~40 LOC).

### 2.9 Logger

| Feature | LWJGL3 | GWT | Android | iOS | Headless |
|---------|--------|-----|---------|-----|----------|
| Output | System.out/err | console.log/error | android.util.Log | Foundation.log (NSLog) | System.out/err |
| LOC | 57 | ~30 | 55 | 58 | 40 |

**Sharable in core**: A `println`-based logger works on JVM, JS (console), and Native. Only Android (logcat) and iOS (NSLog) truly need custom implementations.

---

## 3. Shareable Code Summary

### 3.1 Code That Should Move to Core (shared across all platforms)

| Component | Source | ~LOC | Benefit |
|-----------|--------|-----:|---------|
| Mock Audio (6 classes) | Headless + LWJGL3 | 366 | Cross-platform testing |
| Mock Graphics | Headless | 208 | Cross-platform testing |
| Mock Input | Headless | 163 | Cross-platform testing |
| ApplicationConfiguration base | All backends | ~100 | Shared config fields |
| Frame timing logic | AbstractGraphics | ~50 | Already partially shared |
| Sync (frame rate limiter) | LWJGL3 | 170 | Useful on JVM + Native |
| Resolution strategies | Android | ~130 | Fill/Fixed/Ratio |
| Pan-to-volume formula | Android + iOS | ~10 | Utility function |
| Disabled/Null audio | Android + iOS | ~60 | Testing + headless |
| **Subtotal** | | **~1,257** | |

### 3.2 Code Shareable Across JVM Backends (Desktop + Android + iOS via MobiVM)

| Component | Source | ~LOC | Platforms |
|-----------|--------|-----:|----------|
| Wav decoder (WavInputStream) | LWJGL3 audio | 184 | JVM + Native |
| Mp3 decoder (JLayer) | LWJGL3 audio | 143 | JVM (needs JLayer dep) |
| Ogg decoder (JOrbis OggInputStream) | LWJGL3 audio | 469 | JVM (needs JOrbis dep) |
| NetJavaImpl | Core (already shared) | ~300 | JVM (already shared) |
| NetJavaSocketImpl | Core (already shared) | ~100 | JVM (already shared) |
| Files (java.io.File based) | LWJGL3 | 143 | JVM + Native |
| Preferences (Properties XML) | LWJGL3 | 192 | JVM (not JS/Native) |
| Logger (System.out/err) | LWJGL3 | 57 | JVM + Native + JS |
| **Subtotal** | | **~1,588** | |

### 3.3 Code That Must Be Platform-Specific

| Component | Per-Platform LOC | Platforms Needing It |
|-----------|----------------:|---------------------|
| GL20 binding | 700-850 each | All 4 (JVM, JS, Android, iOS) |
| GL30 binding | 250-876 each | All 4 |
| GL31/GL32 | 748 total | Desktop only |
| Application lifecycle | 400-682 each | All 4 |
| Graphics context | 500-640 each | All 4 |
| Input handling | 728-1472 each | All 4 |
| Audio engine | 400-993 each | All 4 |
| Clipboard | 40-52 each | All 4 |
| Cursor | 40-153 each | Desktop + Browser |
| Window management | 525 | Desktop only |
| Text input UI | 200-400 each | Mobile + Browser |
| Sensors (accelerometer etc.) | 100-200 each | Mobile + Browser |
| Vibration/Haptics | 115-155 each | Mobile only |

---

## 4. SGE Backend Architecture Recommendation

### 4.1 Module Structure

```
sge/
├── core/                          # Cross-platform (JVM/JS/Native via projectMatrix)
│   ├── src/main/scala/sge/        # 557 shared Scala files (already done)
│   │   ├── mock/                  # NEW: Mock implementations for testing
│   │   │   ├── MockAudio.scala
│   │   │   ├── MockGraphics.scala
│   │   │   ├── MockInput.scala
│   │   │   └── ...
│   │   ├── audio/decoders/        # NEW: Shared audio decoders (JVM+Native)
│   │   │   ├── WavDecoder.scala
│   │   │   ├── OggDecoder.scala   # JOrbis-based
│   │   │   └── Mp3Decoder.scala   # JLayer-based
│   │   └── platform/              # Existing: BufferOps, ETC1Ops
│   ├── src/main/scalajvm/         # JVM platform layer (existing)
│   ├── src/main/scalajs/          # JS platform layer (existing)
│   └── src/main/scalanative/      # Native platform layer (existing)
│
├── backend-desktop/               # NEW: Desktop backend (JVM + Native via projectMatrix)
│   ├── src/main/scala/sge/backend/desktop/   # Shared desktop code
│   │   ├── DesktopApplicationConfiguration.scala
│   │   ├── DesktopWindowConfiguration.scala
│   │   ├── DesktopWindowListener.scala
│   │   ├── Sync.scala             # Frame rate limiter
│   │   └── ...
│   ├── src/main/scalajvm/sge/backend/desktop/ # LWJGL3-based (JVM)
│   │   ├── DesktopApplication.scala
│   │   ├── DesktopGraphics.scala
│   │   ├── DesktopInput.scala
│   │   ├── DesktopGL20.scala      # LWJGL OpenGL
│   │   ├── DesktopGL30.scala
│   │   ├── OpenALAudio.scala      # LWJGL OpenAL
│   │   ├── DesktopFiles.scala
│   │   ├── DesktopNet.scala
│   │   └── DesktopPreferences.scala
│   └── src/main/scalanative/sge/backend/desktop/ # GLFW/OpenGL/OpenAL via C FFI (Native)
│       ├── DesktopApplication.scala
│       ├── DesktopGraphics.scala
│       ├── DesktopInput.scala
│       ├── DesktopGL20.scala      # @extern OpenGL
│       ├── OpenALAudio.scala      # @extern OpenAL
│       └── ...
│
├── backend-browser/               # NEW: Browser backend (Scala.js only)
│   └── src/main/scala/sge/backend/browser/
│       ├── BrowserApplication.scala    # requestAnimationFrame loop
│       ├── BrowserGraphics.scala       # Canvas + WebGL
│       ├── BrowserGL20.scala           # WebGL1 with int→object mapping
│       ├── BrowserGL30.scala           # WebGL2
│       ├── BrowserInput.scala          # DOM events
│       ├── BrowserAudio.scala          # Web Audio API
│       ├── BrowserFiles.scala          # HTTP fetch
│       ├── BrowserNet.scala            # Fetch API
│       ├── BrowserPreferences.scala    # localStorage
│       └── ...
│
├── backend-android/               # FUTURE: Android backend (Scala JVM on Android)
│   └── src/main/scala/sge/backend/android/
│       ├── AndroidApplication.scala
│       ├── AndroidGraphics.scala
│       ├── AndroidGL20.scala       # android.opengl.GLES20
│       ├── AndroidInput.scala
│       ├── AndroidAudio.scala      # SoundPool + MediaPlayer
│       └── ...
│
├── backend-ios/                   # FUTURE: iOS backend (Scala JVM via MobiVM)
│   └── src/main/scala/sge/backend/ios/
│       ├── IOSApplication.scala
│       ├── IOSGraphics.scala
│       ├── IOSGLES20.scala
│       ├── IOSInput.scala
│       ├── IOSAudio.scala          # AVAudioEngine (modern replacement for ObjectAL)
│       └── ...
│
└── native-components/             # Existing: Rust native library
    └── src/
        ├── buffer_ops.rs
        ├── etc1.rs
        ├── openal_bindings.rs     # NEW: Rust OpenAL for Native desktop
        └── opengl_bindings.rs     # NEW: Rust OpenGL for Native desktop
```

### 4.2 Backend Priority Order

| Priority | Backend | Effort | Dependency |
|----------|---------|--------|-----------|
| **P0** | Desktop JVM (LWJGL3) | ~3,000 LOC new Scala | LWJGL3 Java library |
| **P0** | Browser (Scala.js) | ~2,500 LOC new Scala | scalajs-dom |
| **P1** | Desktop Native | ~2,500 LOC new Scala | GLFW/GL/AL C bindings via Rust |
| **P1** | Headless (core mocks) | ~800 LOC new Scala | None (pure Scala) |
| **P2** | Android | ~5,200 LOC new Scala | Android SDK |
| **P3** | iOS (MobiVM) | ~3,000 LOC new Scala | MobiVM + iOS SDK |

### 4.3 Sharing via sbt-projectmatrix

Because SGE uses `sbt-projectmatrix` and Scala compiles cross-platform, **more code can be shared than in LibGDX**:

| LibGDX Approach | SGE Approach | Benefit |
|-----------------|-------------|---------|
| Separate backends per platform | `projectMatrix` with `scalajvm`/`scalajs`/`scalanative` source dirs | Single module, shared code |
| GWT emu/ directory (78 files!) | Not needed — Scala.js javalib handles ~65 of 78 | Massive reduction |
| Separate lwjgl + lwjgl3 backends | Single `backend-desktop` with JVM/Native variants | Shared config, window logic |
| Android and iOS separate | Could share mobile base (config, lifecycle state machine) | Reduced duplication |
| OpenAL code duplicated between lwjgl and lwjgl3 | Single OpenAL audio module in `backend-desktop` | No duplication |

---

## 5. Test Migration Plan

### 5.1 LibGDX Backend Tests

LibGDX backends have **zero test files** in the backend directories. All testing is done via:
- `tests/gdx-tests/` — visual/interactive test applications (not unit tests)
- Manual testing with sample applications

### 5.2 SGE Backend Test Strategy

SGE should add proper unit tests for backend code, especially for shared utilities:

| Component | Test Approach | Platform |
|-----------|--------------|----------|
| Mock audio/graphics/input | Unit tests verifying no-op behavior | All (core) |
| Wav decoder | Unit tests with sample WAV files | JVM + Native |
| Ogg decoder | Unit tests with sample OGG files | JVM + Native |
| Mp3 decoder | Unit tests with sample MP3 files | JVM + Native |
| Sync (frame rate limiter) | Unit tests for timing algorithm | JVM + Native |
| Resolution strategies | Unit tests for Fill/Fixed/Ratio | All (core) |
| Preferences (JSON-based) | Unit tests for get/put/flush | All (core) |
| Pan-to-volume formula | Property-based tests | All (core) |
| GL20 binding correctness | Integration tests with real GL context | Per-platform |
| Application lifecycle | Integration tests with mock listener | Per-platform |

### 5.3 Test File Locations

```
core/src/test/scala/sge/mock/          # Mock implementation tests
core/src/test/scala/sge/audio/         # Decoder tests (JVM+Native)
core/src/test/scala/sge/utils/         # Existing test dir
backend-desktop/src/test/scala/        # Desktop-specific tests
backend-browser/src/test/scala/        # Browser-specific tests
```

---

## 6. Key Technical Decisions Needed

### 6.1 Audio Decoder Sharing

**Decision**: Should audio decoders (Wav/Mp3/Ogg) live in core or in backend-desktop?

- **Pro core**: Reusable by any backend that needs software decoding
- **Pro backend**: Only JVM desktop and Native desktop use them (browser/mobile decode natively)
- **Recommendation**: Put in `core` with JVM+Native conditional compilation. JS and mobile backends won't use them but they won't be linked either.

### 6.2 GL Code Generation

**Decision**: Should GL bindings be hand-written or code-generated?

- Each platform needs 700-900 LOC for GL20 and 250-900 LOC for GL30 — all mechanical 1:1 delegation
- Total across 4 platforms: ~6,000-8,000 LOC of trivial code
- **Recommendation**: Code generation from the GL20/GL30 trait definitions. This eliminates errors and makes adding GL31/GL32 trivial.

### 6.3 Desktop JVM vs Native Sharing

**Decision**: How much code can `backend-desktop` share between JVM (LWJGL) and Native (@extern)?

- LWJGL wraps the same C libraries (GLFW, OpenGL, OpenAL) that Scala Native would access directly
- The application lifecycle, window management, input processing logic is identical
- Only the FFI layer differs (LWJGL Java calls vs @extern C calls)
- **Recommendation**: Use a trait-based abstraction for the FFI layer, with shared application/window/input logic above it.

### 6.4 iOS Audio Backend

**Decision**: ObjectAL (used by LibGDX) vs AVAudioEngine (modern Apple) vs direct OpenAL?

- ObjectAL is unmaintained
- GLKit (used by LibGDX iOS graphics) is deprecated by Apple
- **Recommendation**: Use AVAudioEngine for audio and MetalANGLE for OpenGL ES emulation on Metal.

### 6.5 Reflection Replacement for Scala.js

**Decision**: How to handle reflection-dependent code (Skin.load(), etc.) on Scala.js?

- GWT uses compile-time ReflectionCache
- Scala.js has no runtime reflection
- **Recommendation**: Scala 3 `inline`/`derived`/`Mirror` macros to generate type metadata at compile time. This is already identified as a known issue.

---

## 7. Migration Sequence

### Phase 1: Foundation (core improvements)
1. Move mock implementations to `core/src/main/scala/sge/mock/`
2. Extract audio decoders to `core/src/main/scala/sge/audio/decoders/`
3. Add resolution strategies to core
4. Add shared utilities (pan-to-volume, etc.)
5. Write tests for all shared code

### Phase 2: Desktop JVM Backend (P0)
1. Create `backend-desktop` sbt module with projectMatrix
2. Port Lwjgl3Application lifecycle
3. Port Lwjgl3Graphics (GLFW + GL context)
4. Port GL20/GL30 bindings (or generate)
5. Port DefaultLwjgl3Input (GLFW callbacks + key mapping)
6. Port OpenAL audio engine
7. Port Files, Preferences, Net, Clipboard, Cursor
8. Port Sync (frame rate limiter)
9. Integration tests with sample application

### Phase 3: Browser Backend (P0)
1. Create `backend-browser` sbt module (Scala.js only)
2. Port BrowserApplication (requestAnimationFrame loop)
3. Port BrowserGL20 (WebGL1 + handle mapping pattern)
4. Port BrowserGL30 (WebGL2)
5. Port BrowserInput (DOM events + key mapping)
6. Port BrowserAudio (Web Audio API)
7. Port BrowserFiles (HTTP fetch + preloader)
8. Port remaining subsystems

### Phase 4: Desktop Native Backend (P1)
1. Add Scala Native sources to `backend-desktop` projectMatrix
2. Create Rust/C bindings for GLFW, OpenGL, OpenAL
3. Port application/window lifecycle using shared desktop logic
4. Port GL bindings via @extern
5. Port OpenAL audio via @extern
6. Integration tests

### Phase 5: Headless Backend (P1)
1. Wire mock implementations from core into a minimal Application shell
2. Add platform-specific Application impls (JVM: thread-based, JS: timer-based)

### Phase 6+: Mobile (P2/P3)
1. Android backend
2. iOS backend (requires MobiVM proof-of-concept first)
