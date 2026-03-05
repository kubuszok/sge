# Backend Migration Plan

**Date**: 2026-03-05
**Status**: Planning
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

### 2.4 `backend-android` (FUTURE — JVM only)
Android backend. Uses ANGLE (via Android's native EGL) + Android SDK APIs.
JNI bridge retained for native ops (ART doesn't support Panama).

### 2.5 `backend-ios` (FUTURE — Scala Native only)
iOS backend. Deferred until Scala Native gains iOS target support.
Architecture: Scala Native + SDL3 + ANGLE (Metal backend) + miniaudio.

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
| `Lwjgl3Application.java` | `DesktopApplication.scala` | desktop-shared | P0 | not_started | Main loop, window mgmt. SDL3/GLFW init via FFI trait |
| `Lwjgl3ApplicationBase.java` | `DesktopApplicationBase.scala` | desktop-shared | P0 | done | Factory interface (12 LOC) |
| `Lwjgl3ApplicationConfiguration.java` | `DesktopApplicationConfig.scala` | desktop-shared | P0 | done | Config class (data portion); GLFW monitor queries deferred |
| `Lwjgl3ApplicationLogger.java` | — | core-shared | P1 | done | println logger already in noop; upgrade to shared |
| `Lwjgl3NativesLoader.java` | — | skip | — | done | Panama loads libs directly; no native loader needed |

#### Graphics / GL

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Graphics.java` | `DesktopGraphics.scala` | desktop-shared | P0 | not_started | Context, display modes, frame timing. FFI for GLFW queries |
| `Lwjgl3GL20.java` | `AngleGL20.scala` | desktop-jvm + desktop-native | P0 | not_started | **Codegen candidate**: ~850 LOC of 1:1 ANGLE ES 2.0 delegation |
| `Lwjgl3GL30.java` | `AngleGL30.scala` | desktop-jvm + desktop-native | P0 | not_started | **Codegen candidate**: ~400 LOC ES 3.0 |
| `Lwjgl3GL31.java` | `AngleGL31.scala` | desktop-jvm + desktop-native | P1 | not_started | **Codegen candidate**: ~200 LOC ES 3.1 |
| `Lwjgl3GL32.java` | `AngleGL32.scala` | desktop-jvm + desktop-native | P1 | not_started | **Codegen candidate**: ~200 LOC ES 3.2 |

#### Window Management

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `Lwjgl3Window.java` | `DesktopWindow.scala` | desktop-shared | P0 | not_started | Window lifecycle, GLFW callbacks via FFI trait |
| `Lwjgl3WindowConfiguration.java` | `DesktopWindowConfig.scala` | desktop-shared | P0 | done | Per-window config (mutable class with vars) |
| `Lwjgl3WindowListener.java` | `DesktopWindowListener.scala` | desktop-shared | P0 | done | Event trait with default methods (merged Adapter) |
| `Lwjgl3WindowAdapter.java` | — | desktop-shared | P0 | done | Merged into DesktopWindowListener trait defaults |

#### Input

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultLwjgl3Input.java` | `DesktopInput.scala` | desktop-shared | P0 | not_started | GLFW callbacks. Key mapping table generated |
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
| `Sync.java` | `FrameSync.scala` | desktop-shared | P1 | done | Frame rate limiter (pure math + sleep); glfwGetTime→System.nanoTime |

#### Audio (OpenAL → miniaudio)

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `OpenALLwjgl3Audio.java` | `MiniaudioEngine.scala` | desktop-shared | P0 | not_started | Rewrite: miniaudio instead of OpenAL |
| `OpenALSound.java` | `DesktopSound.scala` | desktop-shared | P0 | not_started | Sound effect via miniaudio |
| `OpenALMusic.java` | `DesktopMusic.scala` | desktop-shared | P0 | not_started | Streaming music via miniaudio |
| `OpenALAudioDevice.java` | `DesktopAudioDevice.scala` | desktop-shared | P1 | not_started | Raw PCM output via miniaudio |
| `Lwjgl3Audio.java` | `DesktopAudio.scala` | desktop-shared | P0 | done | Audio trait with update() + AutoCloseable |
| `OpenALUtils.java` | — | skip | — | done | OpenAL-specific, not needed with miniaudio |
| `JavaSoundAudioRecorder.java` | `DesktopAudioRecorder.scala` | desktop-jvm | P2 | done | JVM-only (javax.sound) |
| `Wav.java` | `WavInputStream.scala` | core-shared | P1 | done | RIFF parser extracted to `sge.audio.WavInputStream`; Music/Sound wrappers need audio engine |
| `OggInputStream.java` | `OggInputStream.scala` | scalajvm/sge/audio | P1 | done | JVM-only (JOrbis dep); Music/Sound wrappers (Ogg.java) need audio engine |
| `Mp3.java` | `Mp3Decoder.scala` | core-shared | P1 | not_started | JLayer pure Java, shareable; Music/Sound wrappers need audio engine |

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
| `GwtAccelerometer.java` | — | browser | P2 | not_started | Generic Sensor API |
| `GwtGyroscope.java` | — | browser | P2 | not_started | Generic Sensor API |
| `GwtSensor.java` | — | browser | P2 | not_started | Sensor base |
| `GwtPermissions.java` | — | browser | P2 | not_started | Permissions API |
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
| `AndroidApplication.java` | `AndroidApplication.scala` | android | P2 | not_started | Activity lifecycle |
| `AndroidApplicationBase.java` | `AndroidApplicationBase.scala` | android | P2 | not_started | Base interface |
| `AndroidApplicationConfiguration.java` | `AndroidApplicationConfig.scala` | android | P2 | not_started | Config POJO |
| `AndroidApplicationLogger.java` | `AndroidLogger.scala` | android | P2 | not_started | android.util.Log |
| `AndroidFragmentApplication.java` | `AndroidFragmentApp.scala` | android | P2 | not_started | Fragment support |
| `AndroidDaydream.java` | — | skip | — | — | Niche feature |

#### Graphics

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `AndroidGraphics.java` | `AndroidGraphics.scala` | android | P2 | not_started | GLSurfaceView + EGL |
| `AndroidGL20.java` | `AndroidGL20.scala` | android | P2 | not_started | **Codegen**: android.opengl.GLES20 |
| `AndroidGL30.java` | `AndroidGL30.scala` | android | P2 | not_started | **Codegen**: android.opengl.GLES30 |
| `AndroidCursor.java` | `AndroidCursor.scala` | android | P2 | not_started | PointerIcon (API 24+) |
| `GLSurfaceView20.java` | `SgeGLSurfaceView.scala` | android | P2 | not_started | Custom surface view |
| `GdxEglConfigChooser.java` | `EglConfigChooser.scala` | android | P2 | not_started | EGL config selection |
| `AndroidGraphicsLiveWallpaper.java` | — | skip | — | — | Niche |

#### Input

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultAndroidInput.java` | `AndroidInput.scala` | android | P2 | not_started | Touch + sensors + IME |
| `AndroidTouchHandler.java` | `AndroidTouchHandler.scala` | android | P2 | not_started | Multitouch parsing |
| `AndroidMouseHandler.java` | `AndroidMouseHandler.scala` | android | P2 | not_started | External mouse |
| `AndroidHaptics.java` | `AndroidHaptics.scala` | android | P2 | not_started | Vibration |
| `InputProcessorLW.java` | — | skip | — | — | Live wallpaper specific |

#### Audio

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultAndroidAudio.java` | `AndroidAudio.scala` | android | P2 | not_started | SoundPool + MediaPlayer |
| `AndroidAudioDevice.java` | `AndroidAudioDevice.scala` | android | P2 | not_started | AudioTrack PCM |
| `AndroidAudioRecorder.java` | `AndroidAudioRecorder.scala` | android | P2 | not_started | AudioRecord |
| `AndroidSound.java` | `AndroidSound.scala` | android | P2 | not_started | SoundPool wrapper |
| `AndroidMusic.java` | `AndroidMusic.scala` | android | P2 | not_started | MediaPlayer wrapper |
| `DisabledAndroidAudio.java` | — | core-shared | — | done | NoopAudio covers this |
| `AsynchronousAndroidAudio.java` | `AsyncAndroidAudio.scala` | android | P3 | not_started | Background thread loading |
| `AsynchronousSound.java` | `AsyncAndroidSound.scala` | android | P3 | not_started | Async sound ops |
| `AndroidAudio.java` | — | android | P2 | not_started | Interface (merge) |

#### Files

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `DefaultAndroidFiles.java` | `AndroidFiles.scala` | android | P2 | not_started | AssetManager paths |
| `AndroidFileHandle.java` | `AndroidFileHandle.scala` | android | P2 | not_started | Asset/storage access |
| `AndroidZipFileHandle.java` | — | skip | — | — | OBB/expansion files (niche) |
| `ZipResourceFile.java` | — | skip | — | — | OBB support (niche) |
| `APKExpansionSupport.java` | — | skip | — | — | OBB support (niche) |
| `AndroidFiles.java` | — | android | P2 | not_started | Interface (merge) |

#### Net / Preferences / Clipboard

| LibGDX File | SGE Target | Target Module | Priority | Status | Notes |
|-------------|-----------|---------------|----------|--------|-------|
| `AndroidNet.java` | `AndroidNet.scala` | android | P2 | not_started | Delegates to NetJavaImpl |
| `AndroidPreferences.java` | `AndroidPreferences.scala` | android | P2 | not_started | SharedPreferences |
| `AndroidClipboard.java` | `AndroidClipboard.scala` | android | P2 | not_started | ClipboardManager |

#### Live Wallpaper / Keyboard / Resolution (skip or P3)

| LibGDX File | SGE Target | Priority | Status | Notes |
|-------------|-----------|----------|--------|-------|
| `AndroidLiveWallpaperService.java` | — | P3 | not_started | Niche feature |
| `AndroidLiveWallpaper.java` | — | P3 | not_started | Niche feature |
| `AndroidWallpaperListener.java` | — | P3 | not_started | Niche feature |
| `StandardKeyboardHeightProvider.java` | — | P3 | not_started | API 21-29 |
| `AndroidXKeyboardHeightProvider.java` | — | P3 | not_started | API 30+ |
| `KeyboardHeightProvider.java` | — | P3 | not_started | Interface |
| `KeyboardHeightObserver.java` | — | P3 | not_started | Callback |
| `ResolutionStrategy.java` | `ResolutionStrategy.scala` | core-shared | P2 | not_started | Abstract (shareable) |
| `FillResolutionStrategy.java` | — | core-shared | P2 | not_started | Fill screen |
| `FixedResolutionStrategy.java` | — | core-shared | P2 | not_started | Fixed size |
| `RatioResolutionStrategy.java` | — | core-shared | P2 | not_started | Aspect ratio |
| `AndroidVisibilityListener.java` | — | P3 | not_started | Immersive mode |
| `AndroidEventListener.java` | — | P3 | not_started | onActivityResult |
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

| Module | Total Files | Done | Not Started | Deferred | Skipped |
|--------|------------|------|-------------|----------|---------|
| core-shared (noop, decoders, utils) | 16 | 12 | 4 | 0 | 0 |
| desktop-shared | 21 | 14 | 7 | 0 | 0 |
| desktop-jvm | 1 | 0 | 1 | 0 | 0 |
| desktop-native | 0 | 0 | 0 | 0 | 0 |
| browser | 18 | 8 | 10 | 0 | 0 |
| android | 25 | 0 | 25 | 0 | 0 |
| ios | 14 | 0 | 0 | 14 | 0 |
| skip | 28 | — | — | — | 28 |
| **Total** | **123** | **34** | **47** | **14** | **28** |

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

### Phase 5: Android (P2)
1. Create `backend-android` sbt module
2. Port application lifecycle (Activity/Fragment)
3. Port graphics (GLSurfaceView + EGL)
4. Port input, audio (SoundPool/MediaPlayer)
5. Keep JNI bridge for native-ops

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
