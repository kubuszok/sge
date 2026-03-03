# LibGDX Android Backend -- In-Depth Analysis

**Date**: 2026-03-03
**Source**: `libgdx/backends/gdx-backend-android/src/com/badlogic/gdx/backends/android/`
**Total files**: 50 (41 root, 5 surfaceview/, 4 keyboardheight/)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [File-by-File Analysis](#file-by-file-analysis)
   - [Application Layer](#application-layer)
   - [Graphics Layer](#graphics-layer)
   - [Input Layer](#input-layer)
   - [Audio Layer](#audio-layer)
   - [Files Layer](#files-layer)
   - [Networking Layer](#networking-layer)
   - [Preferences Layer](#preferences-layer)
   - [GL Bindings](#gl-bindings)
   - [Surface View Subsystem](#surface-view-subsystem)
   - [Keyboard Height Subsystem](#keyboard-height-subsystem)
   - [APK Expansion Support](#apk-expansion-support)
   - [Utility / Small Classes](#utility--small-classes)
3. [Android API Dependency Summary](#android-api-dependency-summary)
4. [Cross-Platform Shareability Analysis](#cross-platform-shareability-analysis)
5. [Feasibility for Scala on Android](#feasibility-for-scala-on-android)

---

## Executive Summary

The Android backend consists of **50 Java files** totaling approximately **8,500 lines of code**. The backend is
architecturally organized around the core LibGDX interfaces (`Application`, `Graphics`, `Input`, `Audio`,
`Files`, `Net`, `Preferences`) with Android-specific implementations. It supports four distinct Android
application modes:

1. **Activity-based** (`AndroidApplication`) -- standard fullscreen game
2. **Fragment-based** (`AndroidFragmentApplication`) -- embedded in a Fragment
3. **Daydream** (`AndroidDaydream`) -- Android DreamService screensaver
4. **Live Wallpaper** (`AndroidLiveWallpaper` + `AndroidLiveWallpaperService`) -- dynamic wallpaper

The majority of the code (~65%) is deeply Android-specific and cannot be shared. The remaining ~35%
contains logic patterns (event queuing, pan/volume calculations, resolution strategies) that could
inform cross-platform abstractions.

---

## File-by-File Analysis

### Application Layer

#### AndroidApplicationBase.java
- **Class**: `AndroidApplicationBase` (interface)
- **Extends/Implements**: `Application`
- **Android APIs**: `Context`, `Intent`, `Handler`, `Window`, `WindowManager`
- **Key Functionality**: Abstract interface that decouples the application entry point from concrete
  Android component type (Activity, Fragment, DreamService). Defines factory methods for audio and
  input creation, surface holder access, runnable queues, and immersive mode.
- **Lines**: ~100
- **Shareable**: The interface pattern (abstract factory for subsystems) is directly applicable to
  any backend. The specific API surface is Android-only.

#### AndroidApplication.java
- **Class**: `AndroidApplication` extends `Activity` implements `AndroidApplicationBase`
- **Android APIs**: `Activity`, `Bundle`, `Handler`, `Window`, `WindowManager`, `FrameLayout`,
  `Gravity`, `View`, `Configuration`, `Build`, `Debug`, `Intent`
- **Key Functionality**: Primary entry point for Android games. Manages the full Android lifecycle
  (`onCreate` -> `onResume` -> `onPause` -> `onDestroy`). Initializes all subsystems (graphics,
  input, audio, files, net, clipboard). Sets up fullscreen mode, immersive mode, wake lock,
  display cutout rendering, keyboard height provider. Routes `onActivityResult` to registered
  event listeners.
- **Lines**: ~540
- **Shareable**: The initialization sequencing pattern (create subsystems, wire Gdx statics) is
  common to all backends. The lifecycle coordination logic is Android-specific but the state
  machine (created -> running -> paused -> destroyed) is universal.

#### AndroidApplicationConfiguration.java
- **Class**: `AndroidApplicationConfiguration`
- **Android APIs**: `SensorManager` (for `SENSOR_DELAY_GAME` constant), `SoundPool`
- **Key Functionality**: Configuration POJO holding: color depth (r/g/b/a), depth/stencil buffer
  bits, MSAA samples, accelerometer/gyroscope/compass/rotation vector sensor toggles, sensor
  sampling rate, wake lock, audio disable flag, max simultaneous sounds, resolution strategy,
  live wallpaper touch events, immersive mode, GL ES 3.0 toggle, max network threads, display
  cutout rendering, native loader interface.
- **Lines**: ~112
- **Shareable**: The concept of a configuration object is universal. Many fields (color depth,
  MSAA, max sounds, GL version) apply to all backends.

#### AndroidFragmentApplication.java
- **Class**: `AndroidFragmentApplication` extends `Fragment` implements `AndroidApplicationBase`
- **Android APIs**: `Fragment` (AndroidX), `Activity`, `Context`, `Handler`, `Window`,
  `WindowManager`, `View`, `Configuration`, `Log`
- **Key Functionality**: Nearly identical to `AndroidApplication` but based on `Fragment` instead
  of `Activity`. Supports embedding in parent activities. Includes `Callbacks` interface for
  exit. Checks parent fragment chain for removal state.
- **Lines**: ~480
- **Shareable**: None beyond what AndroidApplication provides. This is purely an alternative
  Android hosting mechanism.

#### AndroidDaydream.java
- **Class**: `AndroidDaydream` extends `DreamService` implements `AndroidApplicationBase`
- **Android APIs**: `DreamService`, `GLSurfaceView`, `Handler`, `Looper`, `Context`, `Window`,
  `WindowManager`, `Debug`, `Configuration`
- **Key Functionality**: Screensaver mode application host. Similar lifecycle to Activity but uses
  `onDreamingStarted`/`onDreamingStopped` instead of `onResume`/`onPause`. Does not support
  immersive mode.
- **Lines**: ~428
- **Shareable**: None. Android-specific feature.

#### AndroidLiveWallpaper.java
- **Class**: `AndroidLiveWallpaper` implements `AndroidApplicationBase`
- **Android APIs**: `Context`, `Intent`, `Handler`, `Looper`, `Build`, `Debug`, `Window`,
  `WindowManager`, `Log`
- **Key Functionality**: Companion object to `AndroidLiveWallpaperService`. Manages the live
  wallpaper application instance including lifecycle (pause/resume/destroy), wallpaper color
  notification (API 27+), and static Gdx binding.
- **Lines**: ~400
- **Shareable**: None. Android-specific feature.

#### AndroidLiveWallpaperService.java
- **Class**: `AndroidLiveWallpaperService` extends `WallpaperService`
- **Inner class**: `AndroidWallpaperEngine` extends `Engine`
- **Android APIs**: `WallpaperService`, `WallpaperService.Engine`, `SurfaceHolder`, `MotionEvent`,
  `WindowManager`, `WallpaperColors`, `Bundle`, `Build`, `Log`
- **Key Functionality**: The most complex class in the backend (~614 lines). Manages multiple
  wallpaper engine instances sharing a single GL surface. Handles surface creation/destruction
  lifecycle, visibility changes, offset changes, preview state, icon drop events, touch event
  forwarding, and wallpaper color computation (API 27+).
- **Lines**: ~614
- **Shareable**: None. Deeply tied to Android wallpaper engine lifecycle.

#### AndroidApplicationLogger.java
- **Class**: `AndroidApplicationLogger` implements `ApplicationLogger`
- **Android APIs**: `android.util.Log`
- **Key Functionality**: Thin wrapper mapping `log`/`error`/`debug` to `Log.i`/`Log.e`/`Log.d`.
- **Lines**: ~55
- **Shareable**: The interface pattern is shareable. Implementation is 100% Android-specific.

---

### Graphics Layer

#### AndroidGraphics.java
- **Class**: `AndroidGraphics` extends `AbstractGraphics` implements `GLSurfaceView.Renderer`
- **Android APIs**: `GLSurfaceView`, `GLSurfaceView.Renderer`, `EGLConfigChooser`, `Display`,
  `DisplayMetrics`, `DisplayCutout`, `DisplayManager`, `Build`, `View`, `WindowManager.LayoutParams`,
  `EGL10`, `EGLContext`, `EGLConfig`, `EGLDisplay`, `GL10`
- **Key Functionality**: Core rendering management. Creates and manages `GLSurfaceView20`.
  Implements `Renderer` callbacks (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`).
  `onDrawFrame` is the main loop -- it processes lifecycle state transitions (resume/pause/destroy),
  executes queued runnables, processes input events, calls `ApplicationListener.render()`, and
  tracks FPS. Handles EGL context creation/loss, GL version detection, managed cache invalidation
  (textures, meshes, shaders, framebuffers), DPI/density measurement, safe area insets, display
  mode reporting, continuous vs on-demand rendering.
- **Lines**: ~823
- **Shareable**: The rendering loop logic in `onDrawFrame` (state machine for resume/pause/destroy,
  runnable execution, input processing, FPS counting) is architecturally reusable. The managed
  cache invalidation pattern is needed for any context-loss-capable backend. Display metrics
  (PPI, density) patterns could be abstracted.

#### AndroidGraphicsLiveWallpaper.java
- **Class**: `AndroidGraphicsLiveWallpaper` extends `AndroidGraphics`
- **Android APIs**: `SurfaceHolder`, `GLSurfaceView20`, `EGLConfigChooser`, `Log`
- **Key Functionality**: Wallpaper-specific graphics subclass. Overrides `createGLSurfaceView` to
  use the wallpaper service's `SurfaceHolder`. Overrides `resume()` to use synchronous wait.
  Overrides `onDrawFrame` to swallow runnable exceptions (preventing wallpaper crash). Provides
  `onDestroyGLSurfaceView` for thread cleanup.
- **Lines**: ~215
- **Shareable**: None. Wallpaper-specific.

#### AndroidGL20.java
- **Class**: `AndroidGL20` implements `GL20`
- **Android APIs**: `android.opengl.GLES20` (all ~140 methods)
- **Key Functionality**: Pure delegation layer mapping every `GL20` interface method to the
  corresponding `android.opengl.GLES20` static method. Includes minor adapter logic for
  single-value versions (e.g., `glGenBuffer` wrapping `glGenBuffers`).
- **Lines**: ~705
- **Shareable**: **No logic to share** -- this is a thin wrapper. On Scala/Android, an identical
  delegation layer would be needed. The pattern itself (interface + platform delegation) is the
  same as the LWJGL backend's GL20 impl.

#### AndroidGL30.java
- **Class**: `AndroidGL30` extends `AndroidGL20` implements `GL30`
- **Android APIs**: `android.opengl.GLES30`
- **Key Functionality**: Same delegation pattern as AndroidGL20 but for GL ES 3.0 functions.
  Many methods are commented out (not exposed by Android API or not needed). Includes workarounds
  for missing offset-based overloads.
- **Lines**: ~872
- **Shareable**: Same as AndroidGL20 -- no logic, pure delegation.

#### AndroidCursor.java
- **Class**: `AndroidCursor` implements `Cursor`
- **Android APIs**: `PointerIcon`, `View`, `Build`
- **Key Functionality**: Maps LibGDX system cursor types to Android `PointerIcon` types
  (API 24+). Supports Arrow, Ibeam, Crosshair, Hand, resize cursors, AllScroll, NotAllowed, None.
- **Lines**: ~40
- **Shareable**: The cursor type mapping table is conceptually reusable across platforms.

---

### Input Layer

#### AndroidInput.java
- **Class**: `AndroidInput` (interface)
- **Extends/Implements**: `Input`, `View.OnTouchListener`, `View.OnKeyListener`,
  `View.OnGenericMotionListener`
- **Android APIs**: `View.OnTouchListener`, `View.OnKeyListener`, `View.OnGenericMotionListener`
- **Key Functionality**: Extends `Input` with Android-specific lifecycle hooks (`onPause`,
  `onResume`, `onDreamingStarted`, `onDreamingStopped`), custom listener registration, event
  processing trigger, keyboard availability setter.
- **Lines**: ~36
- **Shareable**: The event-processing pattern is universal.

#### DefaultAndroidInput.java
- **Class**: `DefaultAndroidInput` extends `AbstractInput` implements `AndroidInput`,
  `KeyboardHeightObserver`
- **Android APIs**: `SensorManager`, `Sensor`, `SensorEvent`, `SensorEventListener`,
  `MotionEvent`, `View`, `AlertDialog`, `EditText`, `AutoCompleteTextView`, `InputMethodManager`,
  `InputType`, `EditorInfo`, `InputConnection`, `InputConnectionWrapper`, `Handler`, `Context`,
  `Activity`, `DisplayMetrics`, `Surface`, `Build`, `Color`, `Animator`, `FrameLayout`,
  `RelativeLayout`, `ImageView`, `TextView`, `WindowManager`, `OnBackInvokedCallback`,
  `OnBackInvokedDispatcher`
- **Key Functionality**: The largest file in the backend (~1200+ lines). Handles:
  - **Touch input**: 20-pointer multitouch via `AndroidTouchHandler`
  - **Keyboard input**: Hardware and software keyboard, key event queuing
  - **Mouse input**: Hover and scroll via `AndroidMouseHandler`
  - **Sensors**: Accelerometer, gyroscope, compass, rotation vector (registered/unregistered on
    pause/resume)
  - **Orientation**: Device rotation detection, native orientation determination
  - **Haptics**: Via `AndroidHaptics` delegate
  - **Soft keyboard**: Show/hide IME, native text input with `NativeInputConfiguration`
    (complex overlay with EditText, autocomplete, character limits)
  - **Text input dialog**: Alert dialog with EditText
  - **Keyboard height**: Implements `KeyboardHeightObserver` for soft keyboard size reporting
  - **Predictive back**: Android 13+ back gesture handling
  - **Event processing**: Synchronized event queue (KeyEvent/TouchEvent pools) processed on GL
    thread
- **Lines**: ~1200+
- **Shareable**: Event pooling/queuing pattern (KeyEvent/TouchEvent with Pool), event dispatch
  to InputProcessor, touch state arrays (touchX/Y, deltaX/Y, touched, pressure), pan calculation.
  Sensor logic is Android-only. Soft keyboard management is Android-only.

#### AndroidTouchHandler.java
- **Class**: `AndroidTouchHandler`
- **Android APIs**: `MotionEvent`, `Context`, `PackageManager`
- **Key Functionality**: Processes `MotionEvent` actions (DOWN, UP, POINTER_DOWN, POINTER_UP,
  MOVE, CANCEL, OUTSIDE) into LibGDX `TouchEvent` objects. Maps Android pointer IDs to LibGDX
  pointer indices. Maps Android button states to LibGDX `Buttons`. Handles multitouch capability
  detection.
- **Lines**: ~165
- **Shareable**: The button mapping logic (left/right/middle/back/forward) is a reusable pattern.
  The multitouch coordination logic pattern applies to any touch-capable platform.

#### AndroidMouseHandler.java
- **Class**: `AndroidMouseHandler`
- **Android APIs**: `MotionEvent`, `InputDevice`
- **Key Functionality**: Handles hover move and scroll events from mouse/trackpad input devices.
  Filters by `InputDevice.SOURCE_CLASS_POINTER`. Converts `AXIS_VSCROLL`/`AXIS_HSCROLL` to
  scroll amounts.
- **Lines**: ~93
- **Shareable**: Scroll amount calculation pattern is universal.

#### AndroidHaptics.java
- **Class**: `AndroidHaptics`
- **Android APIs**: `Vibrator`, `VibratorManager`, `VibrationEffect`, `VibrationAttributes`,
  `AudioAttributes`, `Build`, `Context`
- **Key Functionality**: Provides three vibration modes: duration-based, vibration-type-based
  (LIGHT/MEDIUM/HEAVY via predefined effects), and duration+intensity. Handles API level
  differences (API 26 for VibrationEffect, API 29 for amplitude control, API 31 for
  VibratorManager, API 33 for VibrationAttributes).
- **Lines**: ~115
- **Shareable**: The vibration type abstraction (LIGHT/MEDIUM/HEAVY) is platform-portable. All
  implementation is Android-specific.

---

### Audio Layer

#### AndroidAudio.java
- **Class**: `AndroidAudio` (interface)
- **Extends/Implements**: `Audio`
- **Key Functionality**: Adds `pause()`, `resume()`, and `notifyMusicDisposed()` lifecycle methods
  to the core `Audio` interface.
- **Lines**: ~17
- **Shareable**: The lifecycle extension pattern is applicable to all mobile backends.

#### DefaultAndroidAudio.java
- **Class**: `DefaultAndroidAudio` implements `AndroidAudio`
- **Android APIs**: `SoundPool`, `AudioManager`, `MediaPlayer`, `AudioAttributes`, `Activity`,
  `AssetFileDescriptor`, `Build`
- **Key Functionality**: Central audio factory. Creates `SoundPool` with game audio attributes.
  Loads sounds via `SoundPool.load()` from asset file descriptors or file paths. Creates
  `MediaPlayer` instances for music streaming. Manages active music list for pause/resume.
  Routes volume control to music stream.
- **Lines**: ~222
- **Shareable**: The music tracking list (pause/resume active music) is a universal pattern.
  SoundPool/MediaPlayer usage is entirely Android-specific.

#### AndroidSound.java
- **Class**: `AndroidSound` implements `Sound`
- **Android APIs**: `SoundPool`, `AudioManager`
- **Key Functionality**: Wraps a single `SoundPool` sound ID. Manages up to 8 concurrent stream
  IDs per sound. Implements play/stop/pause/resume/loop with volume, pitch, and pan control.
  Pan is manually calculated as left/right volume split.
- **Lines**: ~173
- **Shareable**: The pan-to-stereo-volume calculation is identical across backends and could be a
  shared utility. Stream ID tracking is a reusable pattern.

#### AndroidMusic.java
- **Class**: `AndroidMusic` implements `Music`, `MediaPlayer.OnCompletionListener`
- **Android APIs**: `MediaPlayer`
- **Key Functionality**: Wraps `MediaPlayer` for streaming music playback. Manages prepared state.
  Handles play, pause, stop, loop, volume, pan, seek, position, duration. Routes completion
  callbacks to LibGDX thread via `Gdx.app.postRunnable`.
- **Lines**: ~188
- **Shareable**: The pan-to-volume calculation is identical to AndroidSound. Completion callback
  marshaling to GL thread is a universal pattern.

#### AndroidAudioDevice.java
- **Class**: `AndroidAudioDevice` implements `AudioDevice`
- **Android APIs**: `AudioTrack`, `AudioFormat`, `AudioAttributes`, `AudioManager`
- **Key Functionality**: Low-level PCM audio output. Creates `AudioTrack` in streaming mode.
  Supports both `short[]` and `float[]` sample writing (float->short conversion with clamping).
  Provides latency estimation, volume control, pause/resume.
- **Lines**: ~113
- **Shareable**: Float-to-short sample conversion is reusable.

#### AndroidAudioRecorder.java
- **Class**: `AndroidAudioRecorder` implements `AudioRecorder`
- **Android APIs**: `AudioRecord`, `AudioFormat`, `MediaRecorder`
- **Key Functionality**: Microphone recording via `AudioRecord`. Blocking read of PCM 16-bit
  samples.
- **Lines**: ~59
- **Shareable**: None.

#### AsynchronousAndroidAudio.java
- **Class**: `AsynchronousAndroidAudio` extends `DefaultAndroidAudio`
- **Android APIs**: `HandlerThread`, `Handler`
- **Key Functionality**: Performance variant that plays sounds on a background thread to avoid
  blocking the GL thread. Wraps sounds in `AsynchronousSound`.
- **Lines**: ~41
- **Shareable**: The async-sound concept is applicable to any platform with thread support.

#### AsynchronousSound.java
- **Class**: `AsynchronousSound` implements `Sound`
- **Android APIs**: `Handler`
- **Key Functionality**: Decorator that posts all sound operations to a handler thread. Maps
  caller-visible "sound numbers" to real sound IDs via a circular buffer.
- **Lines**: ~225
- **Shareable**: The async decorator pattern and circular ID mapping are reusable.

#### DisabledAndroidAudio.java
- **Class**: `DisabledAndroidAudio` implements `AndroidAudio`
- **Key Functionality**: Null object pattern. All factory methods throw `GdxRuntimeException`.
  Lifecycle methods are no-ops. Used when `config.disableAudio == true`.
- **Lines**: ~59
- **Shareable**: The null-audio pattern is universal.

---

### Files Layer

#### AndroidFiles.java
- **Class**: `AndroidFiles` (interface)
- **Extends/Implements**: `Files`
- **Key Functionality**: Adds APK expansion file methods (`setAPKExpansion`, `getExpansionFile`).
- **Lines**: ~19
- **Shareable**: Only the base `Files` interface.

#### DefaultAndroidFiles.java
- **Class**: `DefaultAndroidFiles` implements `AndroidFiles`
- **Android APIs**: `AssetManager`, `Activity`, `Fragment`, `ContextWrapper`, `Context`
- **Key Functionality**: File system implementation using Android's `AssetManager` for internal
  files, external storage directory for external files, and `filesDir` for local files. Supports
  APK expansion (OBB) files via `ZipResourceFile` fallback.
- **Lines**: ~155
- **Shareable**: The file type routing (internal/external/local/classpath/absolute) is a pattern
  shared across all backends.

#### AndroidFileHandle.java
- **Class**: `AndroidFileHandle` extends `FileHandle`
- **Android APIs**: `AssetManager`, `AssetFileDescriptor`
- **Key Functionality**: Custom `FileHandle` for Android that uses `AssetManager` for internal
  files. Overrides `read()`, `map()`, `list()` (4 overloads), `isDirectory()`, `exists()`,
  `length()`, `file()`, `child()`, `sibling()`, `parent()`. The `exists()` method is notably
  slow for directories due to Android AssetManager limitations.
- **Lines**: ~264
- **Shareable**: None. Deeply tied to Android AssetManager.

#### AndroidZipFileHandle.java
- **Class**: `AndroidZipFileHandle` extends `AndroidFileHandle`
- **Android APIs**: `AssetFileDescriptor`
- **Key Functionality**: FileHandle for files inside APK expansion (OBB) zip files. Overrides
  listing, reading, existence checking via `ZipResourceFile`.
- **Lines**: ~197
- **Shareable**: None. Android OBB-specific.

---

### Networking Layer

#### AndroidNet.java
- **Class**: `AndroidNet` implements `Net`
- **Android APIs**: `Intent`, `Uri`, `Activity`, `ActivityNotFoundException`
- **Key Functionality**: Delegates HTTP to `NetJavaImpl` (shared Java implementation). Socket
  creation uses `NetJavaServerSocketImpl`/`NetJavaSocketImpl` (shared). `openURI` uses Android
  `Intent.ACTION_VIEW`. The comment explicitly notes the implementation is duplicated between
  Android and desktop backends.
- **Lines**: ~94
- **Shareable**: **Most of this is already shared** via `NetJavaImpl`, `NetJavaServerSocketImpl`,
  `NetJavaSocketImpl`. Only `openURI` is Android-specific.

---

### Preferences Layer

#### AndroidPreferences.java
- **Class**: `AndroidPreferences` implements `Preferences`
- **Android APIs**: `SharedPreferences`, `SharedPreferences.Editor`
- **Key Functionality**: Thin wrapper around Android `SharedPreferences`. Lazy editor creation.
  Maps all `Preferences` methods (put/get for boolean, int, long, float, string, plus clear,
  flush, remove, contains, get-all).
- **Lines**: ~169
- **Shareable**: The interface is shared. Implementation is 100% Android-specific.

---

### GL Bindings

#### AndroidGL20.java
- **Lines**: ~705
- **Nature**: Pure 1:1 delegation to `android.opengl.GLES20`
- **Methods**: ~140 GL methods
- **Shareable**: Pattern only. Each platform needs its own delegation layer.

#### AndroidGL30.java
- **Lines**: ~872
- **Nature**: Pure delegation to `android.opengl.GLES30`, extends AndroidGL20
- **Methods**: ~100+ GL3 methods (many commented out)
- **Shareable**: Same as GL20.

---

### Surface View Subsystem

#### surfaceview/ResolutionStrategy.java
- **Class**: `ResolutionStrategy` (interface)
- **Key Functionality**: Interface with single method `calcMeasures(widthSpec, heightSpec)` returning
  `MeasuredDimension(width, height)`.
- **Lines**: ~39
- **Shareable**: The concept of resolution strategy is cross-platform.

#### surfaceview/FillResolutionStrategy.java
- **Class**: `FillResolutionStrategy` implements `ResolutionStrategy`
- **Android APIs**: `View.MeasureSpec`
- **Key Functionality**: Returns full available width and height. Default strategy.
- **Lines**: ~33
- **Shareable**: Trivial logic, applicable anywhere.

#### surfaceview/FixedResolutionStrategy.java
- **Class**: `FixedResolutionStrategy` implements `ResolutionStrategy`
- **Key Functionality**: Returns fixed configured width and height regardless of available space.
- **Lines**: ~34
- **Shareable**: Yes, fully platform-independent.

#### surfaceview/RatioResolutionStrategy.java
- **Class**: `RatioResolutionStrategy` implements `ResolutionStrategy`
- **Android APIs**: `View.MeasureSpec`
- **Key Functionality**: Maintains a target aspect ratio while maximizing screen usage.
  Calculates whether to letterbox horizontally or vertically.
- **Lines**: ~59
- **Shareable**: The aspect-ratio calculation is fully platform-independent.

#### surfaceview/GLSurfaceView20.java
- **Class**: `GLSurfaceView20` extends `GLSurfaceView`
- **Android APIs**: `GLSurfaceView`, `EGL10`, `EGLConfig`, `EGLContext`, `EGLDisplay`,
  `PixelFormat`, `KeyCharacterMap`, `KeyEvent`, `SystemClock`, `BaseInputConnection`,
  `EditorInfo`, `InputConnection`, `Log`, `Context`
- **Key Functionality**: Custom GLSurfaceView that:
  1. Applies resolution strategy in `onMeasure`
  2. Creates GLES 2.0 or 3.0 context via `ContextFactory` (with fallback from 3.0 to 2.0)
  3. Handles IME input connection for soft keyboard (backspace compat for Jelly Bean)
  4. Inner `ConfigChooser` selects EGL config matching desired RGBA/depth/stencil
- **Lines**: ~302
- **Shareable**: None. Deeply tied to Android GLSurfaceView/EGL.

#### surfaceview/GdxEglConfigChooser.java
- **Class**: `GdxEglConfigChooser` implements `GLSurfaceView.EGLConfigChooser`
- **Android APIs**: `EGL10`, `EGLConfig`, `EGLDisplay`, `GLSurfaceView`, `Log`
- **Key Functionality**: Advanced EGL configuration selector. Requests minimum RGBA=4 bits to get
  all possible configs, then filters for exact match. Supports MSAA (via `EGL_SAMPLES`) and CSAA
  (via NVIDIA `EGL_COVERAGE_SAMPLES_NV`). Falls back from exact match to RGB565 "safe" config.
- **Lines**: ~202
- **Shareable**: The config selection algorithm (exact match with AA preference, safe fallback)
  is a pattern applicable to EGL on any platform.

---

### Keyboard Height Subsystem

#### keyboardheight/KeyboardHeightObserver.java
- **Class**: `KeyboardHeightObserver` (interface)
- **Key Functionality**: Callback for keyboard height changes with parameters: opened (boolean),
  height, leftInset, rightInset, orientation.
- **Lines**: ~33
- **Shareable**: The concept of keyboard height observation is mobile-specific but cross-platform.

#### keyboardheight/KeyboardHeightProvider.java
- **Class**: `KeyboardHeightProvider` (interface)
- **Key Functionality**: Provider interface with start(), close(), setKeyboardHeightObserver(),
  getKeyboardLandscapeHeight(), getKeyboardPortraitHeight().
- **Lines**: ~16
- **Shareable**: Interface is cross-platform.

#### keyboardheight/StandardKeyboardHeightProvider.java
- **Class**: `StandardKeyboardHeightProvider` extends `PopupWindow` implements `KeyboardHeightProvider`
- **Android APIs**: `PopupWindow`, `Activity`, `Context`, `Point`, `Rect`, `ColorDrawable`,
  `Gravity`, `LayoutInflater`, `View`, `ViewTreeObserver.OnGlobalLayoutListener`,
  `WindowManager.LayoutParams`, `LinearLayout`, `Configuration`
- **Key Functionality**: Pre-API-30 keyboard height detection using the "invisible popup window"
  technique. Creates a zero-width, match-parent-height popup that resizes when the keyboard
  appears. Calculates keyboard height from the difference between screen height and visible frame
  bottom. Caches heights per orientation.
- **Lines**: ~186
- **Shareable**: None. Android-specific technique.

#### keyboardheight/AndroidXKeyboardHeightProvider.java
- **Class**: `AndroidXKeyboardHeightProvider` implements `KeyboardHeightProvider`
- **Android APIs**: `Activity`, `View`, `Configuration`, `WindowInsetsCompat` (AndroidX),
  `ViewCompat`, `Insets`, `OnApplyWindowInsetsListener`
- **Key Functionality**: API 30+ keyboard height detection using WindowInsets API. Registers
  `OnApplyWindowInsetsListener` that checks `WindowInsetsCompat.Type.ime()` visibility and
  calculates insets from combined system bars + IME + display cutout + mandatory gestures.
- **Lines**: ~107
- **Shareable**: None. Android-specific.

---

### APK Expansion Support

#### APKExpansionSupport.java
- **Class**: `APKExpansionSupport`
- **Android APIs**: `Context`, `Environment`
- **Key Functionality**: Locates APK expansion files (.obb) on external storage at
  `/Android/obb/<package>/`. Supports main and patch versions. Creates `ZipResourceFile`
  from found OBB files.
- **Lines**: ~78
- **Shareable**: None. Android OBB-specific.

#### ZipResourceFile.java
- **Class**: `ZipResourceFile`
- **Inner class**: `ZipEntryRO`
- **Android APIs**: `AssetFileDescriptor`, `ParcelFileDescriptor`, `Log`
- **Key Functionality**: Custom zip file reader (does not use `java.util.zip.ZipFile` for the
  initial scan). Memory-maps the central directory for efficient entry lookup. Supports both
  stored (uncompressed) and deflated entries. Stored entries can provide `AssetFileDescriptor`
  for direct media player access. Supports patch files (multiple zips merged).
- **Lines**: ~395
- **Shareable**: The zip parsing logic is pure Java but the `AssetFileDescriptor` /
  `ParcelFileDescriptor` usage is Android-specific.

---

### Utility / Small Classes

#### AndroidClipboard.java
- **Class**: `AndroidClipboard` implements `Clipboard`
- **Android APIs**: `ClipboardManager`, `ClipData`, `Context`
- **Key Functionality**: Get/set/has clipboard contents via Android ClipboardManager.
- **Lines**: ~52
- **Shareable**: Interface only.

#### AndroidEventListener.java
- **Class**: `AndroidEventListener` (interface)
- **Android APIs**: `Intent`
- **Key Functionality**: Callback for `onActivityResult` forwarding. Used by extensions.
- **Lines**: ~15
- **Shareable**: None.

#### AndroidVisibilityListener.java
- **Class**: `AndroidVisibilityListener`
- **Android APIs**: `View`, `View.OnSystemUiVisibilityChangeListener`
- **Key Functionality**: Re-enables immersive mode when system UI visibility changes.
- **Lines**: ~45
- **Shareable**: None.

#### AndroidWallpaperListener.java
- **Class**: `AndroidWallpaperListener` (interface)
- **Key Functionality**: Callbacks for wallpaper-specific events: offset change, preview state
  change, icon dropped.
- **Lines**: ~45
- **Shareable**: None.

#### GdxNativeLoader.java
- **Class**: `GdxNativeLoader` (interface)
- **Key Functionality**: Single `load()` method for native library loading.
- **Lines**: ~25
- **Shareable**: The concept is universal but implementation is platform-specific.

#### InputProcessorLW.java
- **Class**: `InputProcessorLW` extends `InputProcessor`
- **Key Functionality**: Adds `touchDrop(x, y)` for live wallpaper icon drop events.
- **Lines**: ~26
- **Shareable**: None.

---

## Android API Dependency Summary

| API Category | Classes Used | Files Using |
|---|---|---|
| **Activity/Fragment lifecycle** | Activity, Fragment, DreamService, WallpaperService, Bundle | 5 |
| **OpenGL ES** | GLES20, GLES30, GLSurfaceView, EGL10, EGLContext, EGLConfig, EGLDisplay | 5 |
| **Sensor framework** | SensorManager, Sensor, SensorEvent, SensorEventListener | 1 (DefaultAndroidInput) |
| **Audio** | SoundPool, MediaPlayer, AudioTrack, AudioRecord, AudioManager, AudioAttributes, AudioFormat | 5 |
| **View system** | View, MotionEvent, KeyEvent, InputDevice, PointerIcon, WindowManager, Display, DisplayMetrics, DisplayCutout | 8+ |
| **Input method** | InputMethodManager, InputConnection, EditorInfo, InputType | 2 |
| **Text/UI widgets** | EditText, AlertDialog, FrameLayout, RelativeLayout, TextView, ImageView, PopupWindow, LinearLayout | 3 |
| **Vibration** | Vibrator, VibratorManager, VibrationEffect, VibrationAttributes | 1 |
| **File I/O** | AssetManager, AssetFileDescriptor, ParcelFileDescriptor | 4 |
| **Preferences** | SharedPreferences, SharedPreferences.Editor | 1 |
| **Clipboard** | ClipboardManager, ClipData | 1 |
| **Network** | Intent, Uri, ActivityNotFoundException | 1 |
| **AndroidX** | ViewCompat, WindowInsetsCompat, Insets, Fragment (AndroidX) | 2 |
| **System** | Build, Debug, Handler, Looper, HandlerThread, Environment, Log, Context | 15+ |
| **Display cutout** | DisplayCutout, WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES | 2 |

---

## Cross-Platform Shareability Analysis

### Shareable Patterns (applicable to SGE backend architecture)

| Pattern | Source | Reuse Potential |
|---|---|---|
| Application lifecycle state machine (created/running/paused/destroyed) | AndroidGraphics.onDrawFrame | **High** -- all backends need this |
| Subsystem factory pattern (createAudio, createInput, etc.) | AndroidApplicationBase | **High** -- already in SGE core |
| Event queue with object pools (KeyEvent/TouchEvent) | DefaultAndroidInput | **High** -- efficient for all platforms |
| Resolution strategies (fill, fixed, ratio) | surfaceview/ | **High** -- pure math, no platform deps |
| Pan-to-stereo volume calculation | AndroidSound, AndroidMusic | **High** -- identical formula everywhere |
| Null audio pattern (DisabledAndroidAudio) | DisabledAndroidAudio | **High** -- useful for headless/test |
| Async sound decorator | AsynchronousSound | **Medium** -- useful pattern for any threaded platform |
| EGL config selection algorithm | GdxEglConfigChooser | **Medium** -- applicable to any EGL platform |
| Managed resource invalidation on context loss | AndroidGraphics | **Medium** -- mobile-specific but also needed for WebGL |
| Keyboard height observation interface | keyboardheight/ | **Medium** -- relevant for mobile targets |
| Float-to-short PCM conversion | AndroidAudioDevice | **Low** -- small utility |

### Not Shareable (100% Android-specific)

| Category | Files | Lines |
|---|---|---|
| Activity/Fragment/Daydream lifecycle | 4 files | ~1,900 |
| Live Wallpaper | 5 files | ~1,700 |
| Android sensor management | 1 file (in DefaultAndroidInput) | ~200 |
| Android GL delegation | 2 files | ~1,577 |
| Android AssetManager file I/O | 2 files | ~460 |
| Android SharedPreferences | 1 file | ~169 |
| Android keyboard/IME management | 3 files | ~500 |
| Android haptics/vibration | 1 file | ~115 |
| APK expansion support | 3 files | ~670 |

---

## Feasibility for Scala on Android

### Why It Works

Scala compiles to JVM bytecode. Android's build toolchain (D8/R8) can process any JVM bytecode
into DEX format. This means:

1. **SGE core already compiles for JVM** -- all 557 shared Scala files produce `.class` files
2. **Android Gradle Plugin** can consume these classes via a standard library dependency
3. **The Android backend would be written in Scala** targeting Android-specific APIs

### Required Android APIs

To build an SGE Android backend equivalent to LibGDX's, these Android SDK packages are needed:

| SDK Package | Purpose | Min API |
|---|---|---|
| `android.app.Activity` | Application hosting | 21 |
| `android.opengl.GLES20` / `GLES30` | GL bindings | 21 / 18 |
| `android.opengl.GLSurfaceView` | GL surface management | 21 |
| `android.media.SoundPool` | Sound effect playback | 21 |
| `android.media.MediaPlayer` | Music streaming | 21 |
| `android.media.AudioTrack` | PCM audio output | 21 |
| `android.hardware.SensorManager` | Accelerometer/gyroscope | 21 |
| `android.content.SharedPreferences` | Preferences storage | 21 |
| `android.content.res.AssetManager` | Asset loading from APK | 21 |
| `android.os.Vibrator` / `VibrationEffect` | Haptics | 21 / 26 |
| `android.view.PointerIcon` | Cursor support | 24 |
| `android.view.DisplayCutout` | Notch handling | 28 |
| `androidx.core.view.WindowInsetsCompat` | Keyboard height (modern) | AndroidX |

### Architecture Recommendation

The Android backend for SGE should be structured as:

```
backend-android/
  src/main/scala/sge/backend/android/
    SgeAndroidApplication.scala       -- extends Activity, implements Sge
    SgeAndroidGraphics.scala          -- extends AbstractGraphics, GLSurfaceView.Renderer
    SgeAndroidInput.scala             -- extends AbstractInput
    SgeAndroidAudio.scala             -- wraps SoundPool + MediaPlayer
    SgeAndroidFiles.scala             -- wraps AssetManager
    SgeAndroidNet.scala               -- delegates to shared NetImpl
    SgeAndroidPreferences.scala       -- wraps SharedPreferences
    SgeAndroidGL20.scala              -- delegates to GLES20
    SgeAndroidGL30.scala              -- delegates to GLES30
```

### Key Challenges

1. **Build tooling**: Android Gradle Plugin with Scala support. The `scala-android-plugin` or
   manual configuration of `scalac` in the AGP pipeline is needed.
2. **ProGuard/R8**: Scala's runtime library and pattern matching generate many classes. R8 shrinking
   is essential for APK size.
3. **Reflection**: SGE avoids reflection (unlike LibGDX's `Skin.load()`), which is beneficial for
   R8 optimization.
4. **No Scala Native on Android**: The backend must be JVM-only Scala, not Scala Native.
5. **Library size**: Scala 3 stdlib is ~5MB. With R8 tree-shaking this can be reduced significantly.

### Lines-of-Code Estimate for SGE Android Backend

| Component | LibGDX Lines | SGE Estimate | Notes |
|---|---|---|---|
| Application + lifecycle | 2,000 | 800 | Scala conciseness, single host type initially |
| Graphics | 1,000 | 700 | Same complexity, Scala syntax savings |
| Input | 1,500 | 1,000 | Sensors + touch + keyboard still complex |
| Audio | 900 | 500 | Simpler with Scala collections |
| Files | 600 | 400 | Same AssetManager wrapping |
| GL20 + GL30 | 1,577 | 1,200 | Pure delegation, minimal savings |
| Net + Prefs + Clipboard | 300 | 200 | Thin wrappers |
| Surfaceview | 600 | 400 | EGL config, resolution strategies |
| **Total** | **~8,500** | **~5,200** | **~39% reduction** |

The reduction comes from:
- Eliminating live wallpaper support (4 files, ~1,700 lines) -- niche feature
- Eliminating daydream support (1 file, ~428 lines) -- deprecated feature
- Eliminating APK expansion support (3 files, ~670 lines) -- deprecated by app bundles
- Scala syntax conciseness (~25% reduction on remaining code)
- Removing Fragment support initially (can be added later)
