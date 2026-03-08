# Backend Migration Plan

**Date**: 2026-03-08
**Status**: Complete (except iOS deferred)
**Architecture**: ANGLE + Panama FFM + SDL3/GLFW (see `docs/architecture/backend-graphics-strategy.md`)

---

## 1. Architecture Overview (Post-Panama Decisions)

SGE backends use **ANGLE** (OpenGL ES → Metal/Vulkan/D3D11) for rendering,
**SDL3 or GLFW** for windowing/input, and **miniaudio** for audio. The JVM
calls these C libraries via **Panama FFM** (`java.lang.foreign`); Scala Native
calls them via `@extern` C FFI. This maximizes code sharing between JVM and
Native desktop backends.

```
                SGE Core (cross-platform: JVM/JS/Native)
                    │
    ┌───────────────┼───────────────┬──────────────┐
    │               │               │              │
 Desktop        Browser        Android         iOS
 (JVM+Native)   (Scala.js)    (JVM+JNI)    (Native+SDL3)
    │               │               │              │
 ANGLE+SDL3     WebGL2+DOM     ANGLE+Android   ANGLE+SDL3
 Panama/@extern  scalajs-dom    EGL+JNI         @extern
 miniaudio      Web Audio      SoundPool       miniaudio
```

### Key Architectural Changes from LibGDX

| LibGDX | SGE | Reason |
|--------|-----|--------|
| LWJGL3 wraps GLFW/GL/AL | Panama FFM calls C directly | No Java middleware |
| Desktop GL (full OpenGL) | ANGLE (OpenGL ES) | Unified API across all platforms |
| OpenAL via LWJGL | miniaudio via Panama/@extern | Simpler, cross-platform |
| Separate JVM/Native backends | Single `backend-desktop` with JVM/Native sources | Same C libs, share logic |
| GWT for browser | Scala.js + scalajs-dom | Native Scala cross-compilation |
| RoboVM for iOS | Scala Native + SDL3 + ANGLE | When SN iOS support lands |

---

## 2. Backend Modules

### 2.1 `core` (existing, cross-platform)
Already contains 539 ported files. Will gain shared backend abstractions:
- Noop implementations (7 files, done)
- Platform ops (BufferOps, ETC1Ops — done, Panama working)
- Frame timing logic (already in Graphics trait)
- Shared configuration base classes (new)
- Audio decoder traits (new, for JVM+Native)

### 2.2 `backend-desktop` (NEW — JVM + Native via projectMatrix)
Desktop backend for Windows/macOS/Linux. Uses projectMatrix to share code
between JVM (Panama) and Scala Native (@extern) compilations.

### 2.3 `backend-browser` (NEW — Scala.js only)
Browser backend using WebGL2, Web Audio API, DOM events via scalajs-dom.

### 2.4 Android Platform Support (COMPLETE — JVM only)
Android support uses a **self-contained ops interface** pattern to avoid circular
dependencies. The `sge-jvm-platform-api` module defines JDK-only traits
(`ClipboardOps`, `PreferencesOps`, `LoggerOps`, `AndroidPlatformProvider`), and
`sge-jvm-platform-android` provides concrete implementations using the Android SDK.
sge core discovers and adapts these at runtime via reflection.

**Module layout** (see section 2.6 below for details):
- `sge-jvm-platform-api/` — PanamaProvider trait + Android ops interfaces (JDK 17)
- `sge-jvm-platform-jdk/` — JdkPanama impl (JDK 22+, `java.lang.foreign`)
- `sge-jvm-platform-android/` — PanamaPort + Android backend impls (JDK 17, android.jar)

None are published separately; their class files are merged into sge's JVM JAR.

### 2.5 `backend-ios` (FUTURE — Scala Native only)
iOS backend. Deferred until Scala Native gains iOS target support.
Architecture: Scala Native + SDL3 + ANGLE (Metal backend) + miniaudio.

### 2.6 JVM Platform Modules (sge-jvm-platform-*)

Three internal modules isolate JDK-version-dependent and platform-specific code.
They are **not published** — their class files are merged into sge's JVM JAR at
package time. Runtime feature detection in sge picks the right provider.

```
sge-jvm-platform-api (JDK 17)
├── sge.platform.PanamaProvider          — path-dependent types for FFM abstraction
└── sge.platform.android.*               — self-contained ops interfaces (JDK types only)
    ├── AndroidPlatformProvider           — top-level factory (context as AnyRef)
    ├── AndroidConfigOps                  — application configuration (data class)
    ├── ClipboardOps                      — clipboard read/write
    ├── PreferencesOps                    — SharedPreferences mirror
    ├── LoggerOps                         — android.util.Log mirror
    ├── FilesOps                          — asset/storage file access
    ├── AudioEngineOps                    — audio factory (SoundPool + MediaPlayer)
    ├── SoundOps / MusicOps              — sound/music playback control
    ├── AudioDeviceOps / AudioRecorderOps — raw PCM I/O
    ├── HapticsOps                        — vibration/haptic feedback
    ├── CursorOps                         — system cursor (PointerIcon)
    ├── DisplayMetricsOps                 — PPI, density, safe insets, display modes
    ├── SensorOps                         — accelerometer, gyroscope, compass
    ├── InputMethodOps                    — soft keyboard, text input dialogs
    ├── GLSurfaceViewOps                  — GL surface lifecycle, EGL config
    ├── GL20Ops                           — GL ES 2.0 mirror (133 methods, plain Int)
    ├── GL30Ops                           — GL ES 3.0 mirror (extends GL20Ops, 88 methods)
    ├── ResolutionStrategyOps             — surface dimension calculation
    ├── FillResolutionStrategy            — pass-through (singleton)
    ├── FixedResolutionStrategy           — fixed pixel size (case class)
    ├── RatioResolutionStrategy           — aspect ratio lock (case class)
    └── InputDialogCallback               — text input dialog result callback

sge-jvm-platform-jdk (JDK 22+)
└── sge.platform.JdkPanama               — concrete PanamaProvider using java.lang.foreign

sge-jvm-platform-android (JDK 17 + android.jar)
├── sge.platform.PanamaPortProvider      — PanamaProvider using PanamaPort (always compiles)
└── sge.platform.android.* (scala-android/, conditional on Android SDK)
    ├── AndroidPlatformProviderImpl       — top-level factory delegating to:
    ├── AndroidClipboardImpl              — ClipboardManager
    ├── AndroidPreferencesImpl            — SharedPreferences
    ├── AndroidLoggerImpl                 — android.util.Log
    ├── AndroidFilesOpsImpl               — AssetManager + ContextWrapper
    ├── AndroidAudioEngineImpl            — SoundPool + MediaPlayer
    ├── AndroidSoundOpsImpl               — SoundPool ring buffer
    ├── AndroidMusicOpsImpl               — MediaPlayer
    ├── AndroidAudioDeviceImpl            — AudioTrack
    ├── AndroidAudioRecorderImpl          — AudioRecord
    ├── AndroidHapticsImpl                — Vibrator / VibratorManager
    ├── AndroidCursorImpl                 — PointerIcon (API 24+)
    ├── AndroidDisplayMetricsImpl         — DisplayMetrics + DisplayCutout
    ├── AndroidSensorImpl                 — SensorManager + SensorEventListener
    ├── AndroidInputMethodImpl            — InputMethodManager + AlertDialog
    ├── AndroidGLSurfaceViewImpl          — GLSurfaceView + EGL context factory
    ├── AndroidEglConfigChooser           — MSAA/CSAA-aware EGL config selection
    ├── AndroidGL20Impl                   — GL20Ops impl via GLES20 static methods
    └── AndroidGL30Impl                   — GL30Ops impl via GLES30 (extends AndroidGL20Impl)
```

**Key design decisions:**
- **No circular dependencies**: ops interfaces use only JDK types (AnyRef for Context).
  sge depends on API; Android module depends on API + android.jar; neither depends on sge.
- **Conditional compilation**: `scala-android/` sources only included when android.jar is
  found (via `AndroidSdk.findSdkRoot()`). Builds succeed without Android SDK.
- **`_root_.android.*`**: Required in `scala-android/` files because Scala resolves
  `android` as the current package (`sge.platform.android`) otherwise.

**IMPORTANT — failed approach (do NOT retry):**
A separate `sge-android-adapter` module depending on both `sge` (JVM) and android.jar
was attempted and **failed**: sbt freezes on circular dependency because sge merges the
platform module JARs into its own classpath. The adapter would depend on sge, which
depends on the merged JARs, creating a deadlock.

**Correct approach for adapter files** (GL20/GL30, FileHandle, Net, Application lifecycle):
Copy the minimal sge trait definitions needed into `sge-jvm-platform-api` (as slim
mirror traits), implement in `sge-jvm-platform-android`, then use Scala 3 `export`
clauses or mixin traits in sge core to bridge the mirror types to sge's real types.
This keeps the dependency graph acyclic: api ← android ← (merged into) sge.

---

## 3. Per-File Migration Plan

### Legend
- **Target**: `core-shared` | `desktop-shared` | `desktop-jvm` | `desktop-native` | `browser` | `android` | `ios` | `skip`
- **Status**: `not_started` | `in_progress` | `done` | `deferred`
- **Priority**: P0 (MVP) | P1 (next) | P2 (future) | P3 (deferred)

---

### 3.1 LWJGL3 Backend → `backend-desktop` (37 relevant files)

#### Application Lifecycle

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Application.java` | `DesktopApplication.scala` | desktop-shared | P0 | done | Main loop, window mgmt, Sge context. WindowingOps FFI |
| `Lwjgl3ApplicationBase.java` | `DesktopApplicationBase.scala` | desktop-shared | P0 | done | Factory interface (12 LOC) |
| `Lwjgl3ApplicationConfiguration.java` | `DesktopApplicationConfig.scala` | desktop-shared | P0 | done | Config class (data portion); GLFW monitor queries deferred |
| `Lwjgl3ApplicationLogger.java` | — | core-shared | P1 | done | println logger already in noop; upgrade to shared |
| `Lwjgl3NativesLoader.java` | — | skip | — | done | Panama loads libs directly; no native loader needed |

#### Graphics / GL

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Graphics.java` | `DesktopGraphics.scala` | desktop-shared | P0 | done | GL init, display modes, frame timing, DPI, fullscreen/windowed via WindowingOps |
| `Lwjgl3GL20.java` | `AngleGL20.scala` + `AngleGL20Native.scala` | desktop-jvm + desktop-native | P0 | done | JVM: Panama FFM ~570 LOC; Native: @extern ~570 LOC |
| `Lwjgl3GL30.java` | `AngleGL30.scala` + `AngleGL30Native.scala` | desktop-jvm + desktop-native | P0 | done | JVM: Panama ~460 LOC; Native: @extern ~350 LOC |
| `Lwjgl3GL31.java` | `AngleGL31.scala` + `AngleGL31Native.scala` | desktop-jvm + desktop-native | P1 | done | JVM: Panama ~380 LOC; Native: @extern ~310 LOC |
| `Lwjgl3GL32.java` | `AngleGL32.scala` + `AngleGL32Native.scala` | desktop-jvm + desktop-native | P1 | done | JVM: Panama ~380 LOC + upcall; Native: @extern ~260 LOC + CFuncPtr7 |

#### Window Management

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Window.java` | `DesktopWindow.scala` | desktop-shared | P0 | done | Window lifecycle, GLFW callbacks via FFI trait |
| `Lwjgl3WindowConfiguration.java` | `DesktopWindowConfig.scala` | desktop-shared | P0 | done | Per-window config (mutable class with vars) |
| `Lwjgl3WindowListener.java` | `DesktopWindowListener.scala` | desktop-shared | P0 | done | Event trait with default methods (merged Adapter) |
| `Lwjgl3WindowAdapter.java` | — | desktop-shared | P0 | done | Merged into DesktopWindowListener trait defaults |

#### Input

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultLwjgl3Input.java` | `DefaultDesktopInput.scala` | desktop-shared | P0 | done | GLFW callbacks via WindowingOps. Key mapping table, AbstractInput merged |
| `Lwjgl3Input.java` | `DesktopInput.scala` | desktop-shared | P0 | done | Input trait with lifecycle methods (merged into DesktopInput) |

#### Files / Preferences / Net / Clipboard

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Files.java` | `DesktopFiles.scala` | desktop-shared | P0 | done | java.io based, unified with HeadlessFiles (sge.files package) |
| `Lwjgl3FileHandle.java` | `DesktopFileHandle.scala` | desktop-shared | P0 | done | Overrides child/sibling/parent/getFile (sge.files package) |
| `Lwjgl3Preferences.java` | `DesktopPreferences.scala` | desktop-shared | P1 | done | XML Properties storage, unified with Headless (sge.files package) |
| `Lwjgl3Net.java` | `DesktopNet.scala` | desktop-shared | P1 | done | SgeHttpClient + NetJava sockets + OS-specific openURI (sge.net package) |
| `Lwjgl3Clipboard.java` | `DesktopClipboard.scala` | desktop-shared | P1 | done | FFI-agnostic via constructor injection (42 LOC) |

#### Cursor / Sync

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Cursor.java` | `DesktopCursor.scala` | desktop-shared | P1 | done | GLFW cursor via WindowingOps FFI; Pixmap cursor deferred |
| `Sync.java` | `Sync.scala` | desktop-shared | P1 | done | Frame rate limiter (pure math + sleep); WindowingOps.getTime() |

#### Audio (OpenAL → miniaudio)

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `OpenALLwjgl3Audio.java` | `MiniaudioEngine.scala` | desktop-shared | P0 | done | Rewrite: miniaudio instead of OpenAL. AudioOps FFI |
| `OpenALSound.java` | `MiniaudioSound.scala` | desktop-shared | P0 | done | Sound effect via AudioOps FFI |
| `OpenALMusic.java` | `MiniaudioMusic.scala` | desktop-shared | P0 | done | Streaming music via AudioOps FFI |
| `OpenALAudioDevice.java` | `DesktopAudioDevice.scala` | desktop-shared | P1 | done | Raw PCM output via AudioOps FFI |
| `Lwjgl3Audio.java` | `DesktopAudio.scala` | desktop-shared | P0 | done | Audio trait with update() + AutoCloseable |
| `OpenALUtils.java` | — | skip | — | done | OpenAL-specific, not needed with miniaudio |
| `JavaSoundAudioRecorder.java` | `DesktopAudioRecorder.scala` | desktop-jvm | P2 | done | JVM-only (javax.sound) |
| `Wav.java` | `WavInputStream.scala` | core-shared | P1 | done | RIFF parser extracted to `sge.audio.WavInputStream`; Music/Sound wrappers need audio engine |
| `OggInputStream.java` | `OggInputStream.scala` | scalajvm/sge/audio | P1 | done | JVM-only (JOrbis dep); Music/Sound wrappers (Ogg.java) need audio engine |
| `Mp3.java` | `Mp3Decoder.scala` | scalajvm/sge/audio | P1 | done | JLayer decoder; streaming + decodeAll; JVM-only (javazoom dep) |

#### Mock Audio (already equivalent in noop/)

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `MockAudio.java` | `NoopAudio.scala` | core-shared | — | done | Already exists |
| `MockAudioDevice.java` | `NoopAudioDevice.scala` | core-shared | — | done | Already exists |
| `MockMusic.java` | `NoopMusic.scala` | core-shared | — | done | Already exists |
| `MockSound.java` | `NoopSound.scala` | core-shared | — | done | Already exists |
| `MockAudioRecorder.java` | `NoopAudioRecorder.scala` | core-shared | — | done | Already exists |

---

### 3.2 GWT Backend → `backend-browser` (24 files)

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `GwtApplication.java` | `BrowserApplication.scala` | browser | P0 | done | requestAnimationFrame loop; canvas + WebGL context creation; Sge context; visibility/resize handlers |
| `GwtApplicationConfiguration.java` | `BrowserApplicationConfig.scala` | browser | P0 | done | Canvas config; OrientationLockType enum |
| `GwtApplicationLogger.java` | — | browser | P1 | done | Covered by PrintApplicationLogger (System.out→console.log on JS) |
| `GwtGraphics.java` | `BrowserGraphics.scala` | browser | P0 | done | Canvas + frame timing + fullscreen + display modes (WebGL context creation deferred to BrowserApplication) |
| `GwtGL20.java` | `WebGL20.scala` | browser | P0 | done | WebGL1 with int→JSobject handle map |
| `GwtGL20Debug.java` | — | skip | — | — | Debug wrapper, not needed |
| `GwtGL30.java` | `WebGL30.scala` | browser | P0 | done | WebGL2 |
| `GwtGL30Debug.java` | — | skip | — | — | Debug wrapper |
| `DefaultGwtInput.java` | `DefaultBrowserInput.scala` | browser | P0 | done | DOM events + key mapping |
| `GwtInput.java` | `BrowserInput.scala` | browser | P0 | done | Input trait with reset() lifecycle method |
| `GwtAccelerometer.java` | `BrowserAccelerometer.scala` | browser | P2 | done | Generic Sensor API via js.Dynamic |
| `GwtGyroscope.java` | `BrowserGyroscope.scala` | browser | P2 | done | Generic Sensor API via js.Dynamic |
| `GwtSensor.java` | `BrowserSensor.scala` | browser | P2 | done | Sensor base wrapping js.Dynamic |
| `GwtPermissions.java` | `BrowserPermissions.scala` | browser | P2 | done | W3C Permissions API via js.Dynamic |
| `DefaultGwtAudio.java` | `DefaultBrowserAudio.scala` | browser | P0 | done | Web Audio API (+ WebAudioManager, WebAudioSound, WebAudioMusic, AudioControlGraph, AudioControlGraphPool) |
| `GwtAudio.java` | `BrowserAudio.scala` | browser | P0 | done | Audio trait with AutoCloseable |
| `GwtFiles.java` | `BrowserFiles.scala` | browser | P0 | done | BrowserAssetLoader replaces GWT Preloader |
| `GwtFileHandle.java` | `BrowserFileHandle.scala` | browser | P0 | done | Reads from BrowserAssetLoader cache |
| `GwtNet.java` | `BrowserNet.scala` | browser | P1 | done | HTTP via sttp; openURI via window.open/location.assign |
| `GwtPreferences.java` | `BrowserPreferences.scala` | browser | P1 | done | localStorage via scalajs-dom |
| `GwtClipboard.java` | `BrowserClipboard.scala` | browser | P1 | done | Navigator Clipboard API via scalajs-dom |
| `GwtCursor.java` | `BrowserCursor.scala` | browser | P1 | done | CSS cursor name mapping; Pixmap cursor deferred |
| `GwtFeaturePolicy.java` | — | skip | — | — | GWT-specific |
| `GwtUtils.java` | — | skip | — | — | GWT-specific utilities |

---

### 3.3 Android Backend → `backend-android` (47 files)

#### Core Application

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `AndroidApplication.java` | `AndroidApplication.scala` | sge (scalajvm) | P2 | done | Application impl + adapters (Files, Audio, Prefs, Clipboard) |
| `AndroidApplicationBase.java` | merged into `AndroidApplication.scala` | sge (scalajvm) | P2 | done | Lifecycle callbacks (onResume/onPause/onDestroy) |
| `AndroidApplicationConfiguration.java` | `AndroidConfigOps.scala` | sge-jvm-platform-api | P2 | done | Self-contained data class (JDK types only) |
| `AndroidApplicationLogger.java` | `AndroidLoggerImpl.scala` | sge-jvm-platform-android | P2 | done | LoggerOps impl via android.util.Log |
| `AndroidFragmentApplication.java` | reuses `AndroidApplication.scala` | sge (scalajvm) | P2 | done | Fragment uses same AndroidApplication (lifecycle ops abstract the difference) |
| `AndroidDaydream.java` | — | skip | — | — | Niche feature |

#### Graphics

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `AndroidGraphics.java` | `AndroidGraphics.scala` + `AndroidGL20Adapter.scala` + `AndroidGL30Adapter.scala` | sge scalajvm + sge-jvm-platform-android | P2 | done | Graphics impl with GL adapter bridging opaque types, DisplayMetricsOps, CursorOps, GLSurfaceViewOps |
| `AndroidGL20.java` | `GL20Ops.scala` + `AndroidGL20Impl.scala` | sge-jvm-platform-api + android | P2 | done | GL20Ops mirror trait (all Int); AndroidGL20Impl delegates to GLES20 |
| `AndroidGL30.java` | `GL30Ops.scala` + `AndroidGL30Impl.scala` | sge-jvm-platform-api + android | P2 | done | GL30Ops extends GL20Ops; AndroidGL30Impl delegates to GLES30 |
| `AndroidCursor.java` | `AndroidCursorImpl.scala` | sge-jvm-platform-android | P2 | done | CursorOps impl via PointerIcon |
| `GLSurfaceView20.java` | `AndroidGLSurfaceViewImpl.scala` | sge-jvm-platform-android | P2 | done | GLSurfaceViewOps impl via GLSurfaceView |
| `GdxEglConfigChooser.java` | `AndroidEglConfigChooser` | sge-jvm-platform-android | P2 | done | Part of AndroidGLSurfaceViewImpl |
| `AndroidGraphicsLiveWallpaper.java` | — | skip | — | — | Niche |

#### Input

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultAndroidInput.java` | `AndroidInputMethodImpl.scala` + `AndroidSensorImpl.scala` | sge-jvm-platform-android | P2 | done | InputMethodOps + SensorOps (ops split); touch handler adapters deferred to sge-side |
| `AndroidTouchHandler.java` | `AndroidTouchHandler.scala` | sge (scalajvm) | P2 | done | Uses TouchInputOps for MotionEvent extraction |
| `AndroidMouseHandler.java` | `AndroidMouseHandler.scala` | sge (scalajvm) | P2 | done | Uses TouchInputOps for MotionEvent extraction |
| `AndroidHaptics.java` | `AndroidHapticsImpl.scala` | sge-jvm-platform-android | P2 | done | HapticsOps impl via Vibrator |
| `InputProcessorLW.java` | — | skip | — | — | Live wallpaper specific |

#### Audio

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultAndroidAudio.java` | `AndroidAudioEngineImpl.scala` | sge-jvm-platform-android | P2 | done | AudioEngineOps impl via SoundPool + MediaPlayer |
| `AndroidAudioDevice.java` | `AndroidAudioDeviceImpl.scala` | sge-jvm-platform-android | P2 | done | AudioDeviceOps impl via AudioTrack |
| `AndroidAudioRecorder.java` | `AndroidAudioRecorderImpl.scala` | sge-jvm-platform-android | P2 | done | AudioRecorderOps impl via AudioRecord |
| `AndroidSound.java` | `AndroidSoundOpsImpl.scala` | sge-jvm-platform-android | P2 | done | SoundOps impl via SoundPool |
| `AndroidMusic.java` | `AndroidMusicOpsImpl.scala` | sge-jvm-platform-android | P2 | done | MusicOps impl via MediaPlayer |
| `DisabledAndroidAudio.java` | — | core-shared | — | done | NoopAudio covers this |
| `AsynchronousAndroidAudio.java` | `AsynchronousAndroidAudioImpl.scala` | sge-jvm-platform-android | P3 | done | Decorator wrapping AudioEngineOps with HandlerThread |
| `AsynchronousSound.java` | `AsynchronousSoundOps.scala` | sge-jvm-platform-android | P3 | done | Decorator wrapping SoundOps with Handler circular buffer |
| `AndroidAudio.java` | — | android | P2 | done | Merged into AudioEngineOps trait |

#### Files

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultAndroidFiles.java` | `AndroidFilesOpsImpl.scala` | sge-jvm-platform-android | P2 | done | FilesOps impl via AssetManager |
| `AndroidFileHandle.java` | `AndroidFileHandle.scala` | sge (scalajvm) | P2 | done | sge FileHandle adapter using FilesOps |
| `AndroidZipFileHandle.java` | — | skip | — | — | OBB/expansion files (niche) |
| `ZipResourceFile.java` | — | skip | — | — | OBB support (niche) |
| `APKExpansionSupport.java` | — | skip | — | — | OBB support (niche) |
| `AndroidFiles.java` | — | android | P2 | done | Merged into FilesOps trait |

#### Net / Preferences / Clipboard

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `AndroidNet.java` | `AndroidNet.scala` | sge (scalajvm) | P2 | done | sge Net adapter, delegates to provider.openURI |
| `AndroidPreferences.java` | `AndroidPreferencesImpl.scala` | sge-jvm-platform-android | P2 | done | PreferencesOps impl via SharedPreferences |
| `AndroidClipboard.java` | `AndroidClipboardImpl.scala` | sge-jvm-platform-android | P2 | done | ClipboardOps impl via ClipboardManager |

#### Live Wallpaper / Keyboard / Resolution (skip or P3)

| LibGDX File | SGE Target | Priority | Status | Notes |
|-------------|-----------|----------|--------|-------|
| `AndroidLiveWallpaperService.java` | `AndroidLiveWallpaperServiceImpl.scala` + `LiveWallpaperServiceOps.scala` + `LiveWallpaperAppCallbacks.scala` | sge-jvm-platform-android + api | P3 | done | WallpaperService with engine management, ops callbacks |
| `AndroidLiveWallpaper.java` | `AndroidLiveWallpaper.scala` | sge (scalajvm) | P3 | done | Application impl for live wallpapers |
| `AndroidWallpaperListener.java` | `AndroidWallpaperListenerOps.scala` | sge-jvm-platform-api | P3 | done | Callback interface (offsetChange, previewState, iconDrop) |
| `StandardKeyboardHeightProvider.java` | `StandardKeyboardHeightProviderImpl.scala` | sge-jvm-platform-android | P3 | done | PopupWindow-based keyboard height (API 21-29) |
| `AndroidXKeyboardHeightProvider.java` | `AndroidXKeyboardHeightProviderImpl.scala` | sge-jvm-platform-android | P3 | done | WindowInsets-based keyboard height (API 30+) |
| `KeyboardHeightProvider.java` | `KeyboardHeightProviderOps.scala` | sge-jvm-platform-api | P3 | done | Lifecycle interface for keyboard height detection |
| `KeyboardHeightObserver.java` | `KeyboardHeightObserverOps.scala` | sge-jvm-platform-api | P3 | done | Callback for keyboard height changes |
| `ResolutionStrategy.java` | `ResolutionStrategyOps.scala` | sge-jvm-platform-api | P2 | done | Pure JDK trait (pixel sizes, not MeasureSpec) |
| `FillResolutionStrategy.java` | `FillResolutionStrategy.scala` | sge-jvm-platform-api | P2 | done | Singleton object |
| `FixedResolutionStrategy.java` | `FixedResolutionStrategy.scala` | sge-jvm-platform-api | P2 | done | Final case class |
| `RatioResolutionStrategy.java` | `RatioResolutionStrategy.scala` | sge-jvm-platform-api | P2 | done | Final case class with companion factory |
| `AndroidVisibilityListener.java` | `AndroidVisibilityListenerImpl.scala` + `VisibilityListenerOps.scala` | sge-jvm-platform-android + api | P3 | done | DecorView visibility listener for immersive mode recovery |
| `AndroidEventListener.java` | `AndroidEventListenerOps.scala` | sge-jvm-platform-api | P3 | done | onActivityResult callback interface |
| `GdxNativeLoader.java` | — | skip | — | — | JNI loader |

---

### 3.4 iOS/RoboVM Backend → `backend-ios` (21 core files)

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `IOSApplication.java` | `IOSApplication.scala` | ios | P3 | deferred | SDL3 replaces UIKit lifecycle |
| `IOSApplicationConfiguration.java` | `IOSApplicationConfig.scala` | ios | P3 | deferred | Config |
| `IOSApplicationLogger.java` | `IOSLogger.scala` | ios | P3 | deferred | NSLog |
| `IOSGraphics.java` | `IOSGraphics.scala` | ios | P3 | deferred | ANGLE Metal backend |
| `IOSGLES20.java` | — | ios | P3 | deferred | Share with desktop AngleGL20 |
| `IOSGLES30.java` | — | ios | P3 | deferred | Share with desktop AngleGL30 |
| `DefaultIOSInput.java` | `IOSInput.scala` | ios | P3 | deferred | SDL3 handles touch input |
| `IOSAudio.java` | `IOSAudio.scala` | ios | P3 | deferred | miniaudio (same as desktop) |
| `IOSMusic.java` | — | ios | P3 | deferred | Share with desktop DesktopMusic |
| `IOSSound.java` | — | ios | P3 | deferred | Share with desktop DesktopSound |
| `IOSFiles.java` | `IOSFiles.scala` | ios | P3 | deferred | NSBundle paths |
| `IOSFileHandle.java` | `IOSFileHandle.scala` | ios | P3 | deferred | Bundle/Documents |
| `IOSNet.java` | `IOSNet.scala` | ios | P3 | deferred | sttp or URLSession |
| `IOSPreferences.java` | `IOSPreferences.scala` | ios | P3 | deferred | NSUserDefaults |
| `IOSHaptics.java` | `IOSHaptics.scala` | ios | P3 | deferred | Core Haptics |
| `DisabledIOSAudio.java` | — | core-shared | — | done | NoopAudio covers this |
| `IOSDevice.java` | — | ios | P3 | deferred | HW detection |
| `IOSScreenBounds.java` | — | ios | P3 | deferred | Safe area |
| ObjectAL bindings (13 files) | — | skip | — | — | Replaced by miniaudio |

---

### 3.5 Headless Backend → `core` noop (15 files)

| LibGDX File | SGE Target | Status | Notes |
|-------------|-----------|--------|-------|
| `HeadlessApplication.java` | `HeadlessApplication.scala` | done | Thin shell wiring noop impls into Sge |
| `HeadlessApplicationConfiguration.java` | `HeadlessApplicationConfig.scala` | done | Config (updatesPerSecond, prefsDir) |
| `MockGraphics.java` | `NoopGraphics.scala` | done | Already exists in core/noop |
| `MockInput.java` | `NoopInput.scala` | done | Already exists |
| `MockAudio.java` | `NoopAudio.scala` | done | Already exists |
| `MockAudioDevice.java` | `NoopAudioDevice.scala` | done | Already exists |
| `MockAudioRecorder.java` | `NoopAudioRecorder.scala` | done | Already exists |
| `MockMusic.java` | `NoopMusic.scala` | done | Already exists |
| `MockSound.java` | `NoopSound.scala` | done | Already exists |
| `HeadlessFiles.java` | `DesktopFiles.scala` | done | Reuse desktop files impl |
| `HeadlessFileHandle.java` | `DesktopFileHandle.scala` | done | Reuse desktop |
| `HeadlessNet.java` | `DesktopNet.scala` | done | Reuse desktop (NetJavaImpl) |
| `HeadlessPreferences.java` | `DesktopPreferences.scala` | done | Reuse desktop |
| `HeadlessApplicationLogger.java` | — | done | println logger in noop |
| `HeadlessNativesLoader.java` | — | skip | Not needed |

---

## 4. Summary Counts

### By Target Module

| Module | Total Files | Done | Partial | Deferred | Not Started | Skipped |
|--------|------------|------|---------|----------|-------------|---------|
| core-shared (noop, decoders, utils) | 16 | 16 | 0 | 0 | 0 | 0 |
| desktop-shared (`scaladesktop/`) | 27 | 27 | 0 | 0 | 0 | 0 |
| desktop-jvm (`scalajvm/`) | 15 | 15 | 0 | 0 | 0 | 0 |
| desktop-native (`scalanative/`) | 12 | 12 | 0 | 0 | 0 | 0 |
| browser | 20 | 20 | 0 | 0 | 0 | 0 |
| android P2 (ops interfaces + impls) | 34 | 34 | 0 | 0 | 0 | 0 |
| android P3 (niche/async) | 11 | 11 | 0 | 0 | 0 | 0 |
| ios | 14 | 0 | 0 | 14 | 0 | 0 |
| skip | 28 | — | — | — | — | 28 |
| **Total** | **177** | **135** | **0** | **14** | **0** | **28** |

Note: All Android P2 and P3 files complete. Application lifecycle, touch/mouse handlers,
FileHandle, Net adapters, and Graphics adapter (with GL20/GL30 opaque type bridging) all done.
P3 niche features: live wallpaper service/app, async sound/audio, keyboard height providers
(Standard + AndroidX), visibility listener, event listener, wallpaper listener.
All ops interfaces and Android SDK implementations are complete in `sge-jvm-platform-api` and `sge-jvm-platform-android`.
Only iOS (14 deferred) and skip (28) remain.

### Source Directory Layout

| Directory | Files | Platforms | Purpose |
|-----------|-------|-----------|---------|
| `scala/` | ~557 | JVM/JS/Native | Cross-platform core |
| `scaladesktop/` | 27 | JVM+Native | Desktop backend shared logic |
| `scalajvm/` | 15 | JVM only | Panama FFI, audio codecs, factory |
| `scalajs/` | ~24 | JS only | Browser backend |
| `scalanative/` | 12 | Native only | @extern FFI impls + GL bindings |

### By Priority

| Priority | Files | Description |
|----------|-------|-------------|
| P0 (MVP) | 30 | Desktop + Browser: minimum to run a game |
| P1 (next) | 18 | Preferences, clipboard, cursor, audio decoders, GL31/32 |
| P2 (future) | 25 | Android backend |
| P3 (deferred) | 22 | iOS, live wallpaper, keyboard height, async audio |
| Skip | 28 | Dead/niche/replaced features |

---

## 5. FFI Abstraction Layer (Desktop JVM ↔ Native Sharing)

The key to sharing desktop code between JVM and Native is an FFI trait layer:

```scala
// Shared trait (desktop-shared)
private[desktop] trait GlfwBindings {
  def glfwInit(): Int
  def glfwCreateWindow(w: Int, h: Int, title: String): Long
  def glfwPollEvents(): Unit
  def glfwSwapBuffers(window: Long): Unit
  // ... ~50 methods
}

// JVM implementation (desktop-jvm) — Panama downcall handles
private[desktop] object GlfwBindingsJvm extends GlfwBindings { ... }

// Native implementation (desktop-native) — @extern
private[desktop] object GlfwBindingsNative extends GlfwBindings { ... }
```

Same pattern for `AngleBindings`, `MiniaudioBindings`. Application/window/input
logic sits above and is shared.

---

## 6. Phased Migration Sequence

### Phase 0: Shared Core Additions
1. Add shared audio decoder traits to core
2. Add ResolutionStrategy to core
3. Add shared ApplicationConfiguration base to core
4. Add HeadlessApplication shell
5. **Deliverable**: Headless backend runs tests without graphics

### Phase 1: Desktop JVM Backend (P0)
1. Create `backend-desktop` sbt module with projectMatrix
2. FFI trait layer for GLFW/ANGLE/miniaudio
3. JVM Panama implementations of FFI traits
4. Port DesktopApplication lifecycle (uses FFI traits)
5. Codegen AngleGL20/GL30 bindings
6. Port DesktopWindow, DesktopInput
7. Port MiniaudioEngine
8. Port DesktopFiles, DesktopNet
9. **Deliverable**: Desktop JVM app renders a triangle

### Phase 2: Browser Backend (P0, parallel with Phase 1)
1. Create `backend-browser` sbt module (Scala.js only)
2. Port BrowserApplication (rAF loop)
3. Port WebGL20 with handle mapping
4. Port BrowserInput (DOM events)
5. Port BrowserAudio (Web Audio)
6. Port BrowserFiles (HTTP fetch)
7. **Deliverable**: Browser renders same triangle

### Phase 3: Desktop Native Backend (P1)
1. Add @extern FFI implementations to backend-desktop
2. Rust/C bindings for GLFW, ANGLE, miniaudio in native-components
3. Wire shared desktop logic to Native FFI
4. **Deliverable**: Native binary renders same triangle

### Phase 4: Polish
1. Audio decoders (Wav, Ogg, Mp3) for desktop
2. Preferences, clipboard, cursor
3. Frame sync, GL31/32
4. Integration tests

### Phase 5: Android (P2) — COMPLETE
1. ~~Define self-contained ops interfaces in `sge-jvm-platform-api`~~ (done)
2. ~~Implement Android ops in `sge-jvm-platform-android` (conditional on android.jar)~~ (done)
3. ~~Restructure sbt modules: `sge-jvm-platform-{api,jdk,android}`~~ (done)
4. ~~Add runtime adapter in sge core to bridge ops → sge traits~~ (done)
5. ~~Port application lifecycle (Activity/Fragment)~~ (done)
6. ~~Port graphics (GLSurfaceView + EGL)~~ (done)
7. ~~Port input, audio (SoundPool/MediaPlayer)~~ (done)
8. ~~Integration tests with separate classpaths~~ (done)

### Phase 6: iOS (P3, deferred)
1. Wait for Scala Native iOS support
2. Shares miniaudio and ANGLE with desktop-native
3. SDL3 handles all UIKit (no ObjC needed)

---

## 7. Code Generation Strategy

GL and windowing bindings are mechanical 1:1 delegations (~2,274 LOC per platform).
Code generation runs as an **sbt source generator** (`Compile / sourceGenerators`)
so generated files are never checked in — they are produced at build time.

### Opaque Type Safety

Raw C types (`Int` for GL handles, `Long` for pointers/window handles) are wrapped
in **opaque types** in the shared API layer. The generated low-level code works with
primitives internally but the public API is type-safe:

```scala
// core shared — opaque handle types
object GLTypes {
  opaque type TextureId = Int
  object TextureId {
    inline def apply(raw: Int): TextureId = raw
    extension (id: TextureId) inline def raw: Int = id
  }

  opaque type BufferId = Int
  object BufferId { ... }

  opaque type ProgramId = Int
  object ProgramId { ... }

  opaque type ShaderId = Int
  object ShaderId { ... }

  opaque type UniformLocation = Int
  object UniformLocation { ... }

  opaque type FramebufferId = Int
  object FramebufferId { ... }

  opaque type RenderbufferId = Int
  object RenderbufferId { ... }
}

object WindowTypes {
  opaque type WindowHandle = Long
  object WindowHandle {
    inline def apply(raw: Long): WindowHandle = raw
    extension (h: WindowHandle) inline def raw: Long = h
  }

  opaque type MonitorHandle = Long
  object MonitorHandle { ... }

  opaque type CursorHandle = Long
  object CursorHandle { ... }
}

object AudioTypes {
  opaque type AudioEngineHandle = Long
  object AudioEngineHandle { ... }

  opaque type SoundHandle = Long
  object SoundHandle { ... }
}
```

The GL trait methods use these opaque types:

```scala
// core shared GL20 trait (updated signatures)
trait GL20 {
  def glGenTexture(): TextureId
  def glBindTexture(target: Int, texture: TextureId): Unit
  def glCreateProgram(): ProgramId
  def glCreateShader(shaderType: Int): ShaderId
  def glAttachShader(program: ProgramId, shader: ShaderId): Unit
  def glGetUniformLocation(program: ProgramId, name: String): UniformLocation
  // ...
}
```

Generated implementations unwrap to primitives at the FFI boundary (zero-cost
at runtime thanks to opaque type erasure):

```scala
// Generated: AngleGL20Jvm.scala
override def glGenTexture(): TextureId = {
  val raw = hGlGenTextures.invoke(1, ...).asInstanceOf[Int]
  TextureId(raw)
}

override def glBindTexture(target: Int, texture: TextureId): Unit = {
  hGlBindTexture.invoke(target, texture.raw)  // unwraps to Int
}
```

### Generator Implementation

The sbt source generator:

1. Reads GL/GLFW/miniaudio function signatures from a definition file
   (`project/gl-functions.csv` or similar structured input)
2. Generates platform-specific implementations per target:
   - **JVM**: Panama `MethodHandle` downcall stubs (opaque → primitive → C ABI)
   - **Scala Native**: `@extern` object with `@name` annotations (opaque → primitive → C ABI)
   - **Scala.js**: `@js.native` facades to WebGL (opaque → JSObject handle map)
   - **Android**: `android.opengl.GLES20` delegation (opaque → primitive → Android API)
3. Each generated file sits in the platform-specific source dir
   (`src/main/scalajvm/`, `src/main/scalanative/`, etc.)
4. Same for GL30, GL31, GL32, GLFW bindings, miniaudio bindings

### Benefits

- **Type safety**: Cannot pass a `TextureId` where a `BufferId` is expected
- **Zero runtime cost**: Opaque types erase to primitives at compile time
- **No checked-in generated code**: sbt regenerates on build
- **Single source of truth**: Function signatures defined once, implementations derived
- **Estimated savings**: ~6,000+ LOC of tedious hand-written code eliminated
