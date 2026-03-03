# iOS/RoboVM Backend Analysis

Deep analysis of the LibGDX iOS backend at
`libgdx/backends/gdx-backend-robovm/src/com/badlogic/gdx/backends/iosrobovm/`.

**Total files**: 38 Java files (21 root, 13 objectal/, 4 custom/)

**RoboVM/MobiVM context**: RoboVM (now MobiVM) compiles JVM bytecode to native
ARM code for iOS. Java classes can bind to Objective-C classes via annotations
(`@NativeClass`, `@Library`, `@Method`, `@Property`). This means Scala JVM code
compiled to bytecode can theoretically run on iOS through MobiVM without
modification, provided no JVM-only dependencies are used.

---

## Root Package (21 files)

### IOSApplication.java
- **Implements**: `Application` (libGDX core interface)
- **Lines**: ~467
- **iOS APIs used**:
  - `UIApplicationDelegateAdapter` — app lifecycle delegate (nested `Delegate` class)
  - `UIApplication` — `setIdleTimerDisabled`, `getStatusBarFrame`, `canOpenURL`
  - `UIWindow` — root window creation, `makeKeyAndVisible`
  - `UIScreen` — `getNativeScale`, `getBounds`
  - `UIDevice` — `getUserInterfaceIdiom`, `getSystemVersion`
  - `UIPasteboard` — clipboard (get/set/hasStrings)
  - `NSProcessInfo` — OS version
  - `NSMutableDictionary` — preferences storage (.plist files)
  - `CGRect` — screen bounds computation
  - `Bro.IS_64BIT` — architecture detection
- **Key functionality**:
  - App lifecycle management (didFinishLaunching, didBecomeActive, willResignActive, willTerminate)
  - Creates and wires all subsystems: graphics, audio, input, files, net
  - Computes screen bounds accounting for status bar and retina scale
  - Clipboard via UIPasteboard
  - Preferences via NSMutableDictionary .plist files
  - Runnable queue processing (postRunnable)
- **Shareable logic**: Runnable queue, lifecycle listener management, logging
  delegation, preferences key-value storage pattern. The overall Application
  wiring pattern is reused across all backends.

### IOSApplicationConfiguration.java
- **Implements**: (standalone config class, no interface)
- **Lines**: ~118
- **iOS APIs used**:
  - `GLKViewDrawableColorFormat`, `GLKViewDrawableDepthFormat`,
    `GLKViewDrawableStencilFormat`, `GLKViewDrawableMultisample` — GLKit enums
  - `UIRectEdge` — edge gesture deferral
- **Key functionality**:
  - Configuration flags: orientation, screen dimming, accelerometer, compass,
    haptics, iPod audio, GL30, status bar, home indicator, ringer switch override
  - Audio device buffer size/count
  - HdpiMode (Logical vs Pixels)
  - Known device PPI map (`ObjectMap<String, IOSDevice>`)
- **Shareable logic**: Many config fields are conceptually identical to Android
  (orientation, GL version, audio settings). Could share a base config trait.

### IOSGraphics.java
- **Implements**: `AbstractGraphics` (libGDX core)
- **Lines**: ~639
- **iOS APIs used**:
  - `EAGLContext` — OpenGL ES context creation (ES2 / ES3)
  - `EAGLRenderingAPI` — rendering API selection
  - `GLKView` — OpenGL view (subclassed inline for touch handling)
  - `GLKViewController` — frame loop controller
  - `GLKViewDelegate`, `GLKViewControllerDelegate` — delegate protocols
  - `GLKViewDrawableColorFormat/DepthFormat/StencilFormat/Multisample` — buffer config
  - `UIScreen` — `getMaximumFramesPerSecond`, `getNativeScale`
  - `UIEdgeInsets` — safe area insets (notch handling)
  - `UIEvent`, `CGRect` — touch/drawing events
- **Key functionality**:
  - OpenGL ES context creation and management (ES2/ES3 fallback)
  - GLKView creation with touch event overrides (touchesBegan/Ended/Moved/Cancelled)
  - Frame loop via GLKViewController delegate (update/draw cycle)
  - Delta time, FPS counting, frame ID tracking
  - Buffer format configuration
  - Device PPI/density lookup via HWMachine + IOSDevice
  - Safe area inset calculation (for notch/dynamic island)
  - Continuous/non-continuous rendering mode
  - Viewport hack: GLKView resets viewport on each draw, so last viewport is
    stored statically in IOSGLES20
- **Shareable logic**: Delta time calculation, FPS counting, frame ID tracking,
  continuous rendering logic, buffer format construction. The inner IOSViewDelegate,
  IOSDisplayMode, IOSMonitor classes are structural boilerplate.

### IOSInput.java (interface)
- **Implements**: extends `Input` (libGDX core)
- **Lines**: ~24
- **iOS APIs used**: `UIKey`, `UIView`
- **Key functionality**: Defines iOS-specific input contract: `setupPeripherals()`,
  `onTouch(long)`, `processEvents()`, `onKey(UIKey, boolean)`,
  `getActiveKeyboardTextField()`
- **Shareable logic**: None (pure iOS interface)

### DefaultIOSInput.java
- **Implements**: `AbstractInput` + `IOSInput`
- **Lines**: ~1472
- **iOS APIs used**:
  - `UITouch`, `UITouchPhase` — multi-touch handling (up to 20 pointers)
  - `UIAccelerometer`, `UIAcceleration`, `UIAccelerometerDelegate` — accelerometer
    (deprecated iOS 5.0 API, still used)
  - `UIForceTouchCapability` — 3D Touch pressure detection
  - `UITextField`, `UITextView`, `UITextFieldDelegate`, `UITextViewDelegate` —
    soft keyboard text input
  - `UIAlertController` — text input dialog
  - `UITableView`, `UITableViewDataSource` — autocomplete suggestions
  - `UIToolbar`, `UIBarButtonItem` — Done button toolbar
  - `UIKey`, `UIKeyboardHIDUsage` — hardware keyboard mapping
  - `GCKeyboard` — Game Controller framework keyboard detection
  - `UIScreen` — force touch capability, trait collection
  - `CGPoint`, `CGRect`, `CGSize` — coordinate geometry
  - `NSRange`, `NSSet`, `NSArray` — Foundation data types
  - `NSNotificationCenter` — (indirectly, via IOSUIViewController)
  - `VM` class (RoboVM internal) — low-level object handle manipulation for
    zero-allocation touch wrapper
- **Key functionality**:
  - Multi-touch processing with zero-allocation UITouch wrapper (NSObjectWrapper trick)
  - Accelerometer via deprecated UIAccelerometer API
  - Hardware keyboard key mapping (UIKeyboardHIDUsage -> libGDX Keys, ~270 key mappings)
  - Soft keyboard: hidden UITextField for invisible input, visible UITextField/UITextView
    for NativeInputConfiguration
  - Text input dialog via UIAlertController
  - Haptics delegation to IOSHaptics
  - Pressure/3D Touch support
  - Device orientation rotation reporting
  - Peripheral availability detection (accelerometer, multitouch, vibrator, haptics,
    compass, pressure, hardware keyboard)
- **Shareable logic**: Touch event pooling pattern, key event pooling, key code
  mapping table structure (not values). The commented-out CMMotionManager code
  suggests CoreMotion was attempted but reverted.

### IOSGLES20.java
- **Implements**: `GL20` (libGDX core interface)
- **Lines**: ~437
- **iOS APIs used**:
  - `NSProcessInfo` — simulator detection for MetalANGLE 16-bit workaround
  - All methods are `native` — JNI bridge to OpenGL ES 2.0 C functions
- **Key functionality**:
  - Complete GL20 binding via RoboVM native methods
  - Static viewport storage hack (x, y, width, height) to work around GLKView reset
  - 16-bit texture format conversion for MetalANGLE simulator
    (`convert16bitBufferToRGBA8888`)
  - `glViewport` override stores last values before calling native
- **Shareable logic**: The 16-bit conversion logic is platform-independent. The
  viewport storage pattern could be abstracted.

### IOSGLES30.java
- **Implements**: `GL30` (extends IOSGLES20)
- **Lines**: ~249
- **iOS APIs used**: Same native JNI approach as IOSGLES20
- **Key functionality**: GL30 extensions (VAOs, transform feedback, instanced
  rendering, samplers, etc.). Same 16-bit texture workaround for
  glTexImage3D/glTexSubImage3D.
- **Shareable logic**: Same as IOSGLES20.

### IOSHaptics.java
- **Implements**: (standalone class, no interface)
- **Lines**: ~155
- **iOS APIs used**:
  - `CHHapticEngine` — Core Haptics engine (iOS 13+)
  - `CHHapticPattern`, `CHHapticPatternDict` — haptic pattern construction
  - `CHHapticEventType`, `CHHapticEventParameterID` — event configuration
  - `UIImpactFeedbackGenerator`, `UIImpactFeedbackStyle` — UIKit haptic feedback
  - `AudioServices.playSystemSound(4095)` — legacy vibration fallback
  - `NSProcessInfo` — OS version check
  - `UIDevice` — device idiom (phone vs pad)
- **Key functionality**:
  - Core Haptics engine initialization with auto-restart handler
  - Timed vibration with intensity control via CHHapticPattern
  - Impact feedback (Light/Medium/Heavy) via UIImpactFeedbackGenerator
  - Legacy vibration fallback via AudioServices
  - Vibrator/haptics capability detection
- **Shareable logic**: The vibration duration/amplitude API is cross-platform
  conceptually. The VibrationType mapping pattern could be shared.

### IOSDevice.java
- **Implements**: (standalone data class)
- **Lines**: ~151
- **iOS APIs used**: None directly
- **Key functionality**: Maps iOS hardware machine strings (e.g. "iPhone10,6")
  to PPI values. Contains ~100 known device entries (iPhones, iPods, iPads,
  simulators).
- **Shareable logic**: Pure data class, entirely iOS-specific. No logic to share.

### IOSApplicationLogger.java
- **Implements**: `ApplicationLogger` (libGDX core interface)
- **Lines**: ~58
- **iOS APIs used**:
  - `Foundation.log()` — NSLog equivalent
  - `NSString` — string wrapper for Foundation.log
- **Key functionality**: Routes log/error/debug messages through iOS Foundation
  logging (NSLog).
- **Shareable logic**: Logging pattern is identical across backends (just
  different output targets). Could share a base formatter.

### IOSMusic.java
- **Implements**: `Music` (libGDX core interface)
- **Lines**: ~146
- **iOS APIs used**: Uses ObjectAL wrappers (OALAudioTrack, AVAudioPlayerDelegateAdapter)
- **Key functionality**:
  - Music playback via OALAudioTrack (ObjectAL wrapper around AVAudioPlayer)
  - Play, pause, stop, looping, volume, pan, position
  - Completion listener via AVAudioPlayerDelegate
  - Preload support for latency-critical scenarios
- **Shareable logic**: The Music interface implementation pattern is identical
  across backends. Only the underlying audio API differs.

### IOSSound.java
- **Implements**: `Sound` (libGDX core interface)
- **Lines**: ~170
- **iOS APIs used**: Uses ObjectAL wrappers (OALSimpleAudio, ALBuffer, ALChannelSource, ALSource)
- **Key functionality**:
  - Sound effect playback via OALSimpleAudio/ALBuffer
  - Multiple simultaneous sounds via ALChannelSource pool (max 8 tracked stream IDs)
  - Per-sound control: volume, pitch, pan, looping, pause/resume
  - Source ID tracking for individual sound instance control
- **Shareable logic**: Stream ID management pattern. The Sound interface
  implementation follows the same pattern on all backends.

### IOSFiles.java
- **Implements**: `Files` (libGDX core interface)
- **Lines**: ~87
- **iOS APIs used**:
  - `NSBundle.getMainBundle().getBundlePath()` — internal/classpath files
  - `System.getenv("HOME")` — app sandbox home directory
- **Key functionality**:
  - Path mapping:
    - Internal: `NSBundle.mainBundle.bundlePath` (app bundle, read-only)
    - External: `$HOME/Documents/` (user-visible, backed up)
    - Local: `$HOME/Library/local/` (app-private, not backed up)
  - Creates external/local directories on init
- **Shareable logic**: The Files interface pattern is identical across backends.
  Path resolution is platform-specific.

### IOSFileHandle.java
- **Implements**: extends `FileHandle` (libGDX core)
- **Lines**: ~67
- **iOS APIs used**: None directly (uses IOSFiles static paths)
- **Key functionality**: Overrides `child()`, `parent()`, `sibling()`, `file()`
  to use iOS-specific path prefixes from IOSFiles.
- **Shareable logic**: File handle pattern is identical to Android backend. The
  path resolution strategy could be parameterized.

### IOSNet.java
- **Implements**: `Net` (libGDX core interface)
- **Lines**: ~82
- **iOS APIs used**:
  - `NSURL` — URL creation for `openURI`
  - `UIApplication` — `canOpenURL`, `openURL` with `UIApplicationOpenURLOptions`
- **Key functionality**:
  - HTTP requests via `NetJavaImpl` (shared Java implementation)
  - Sockets via `NetJavaServerSocketImpl`/`NetJavaSocketImpl` (shared Java implementation)
  - URL opening via UIApplication.openURL
- **Shareable logic**: HTTP and socket implementations are already shared Java
  code (`NetJavaImpl`, etc.). Only `openURI` is iOS-specific.

### IOSPreferences.java
- **Implements**: `Preferences` (libGDX core interface)
- **Lines**: ~194
- **iOS APIs used**:
  - `NSMutableDictionary<NSString, NSObject>` — key-value storage
  - `NSString`, `NSNumber` — value wrappers
  - `NSAutoreleasePool` — memory management for flush
  - Dictionary `.write(File)` — plist serialization
- **Key functionality**:
  - Key-value preference storage backed by NSMutableDictionary
  - Persistence via .plist file serialization
  - Type-safe getters/setters for boolean, int, long, float, string
- **Shareable logic**: The Preferences interface pattern is identical across
  backends. Android uses SharedPreferences, desktop uses Properties files.

### IOSScreenBounds.java
- **Implements**: (standalone immutable data class)
- **Lines**: ~43
- **iOS APIs used**: None
- **Key functionality**: Holds screen bounds (x, y, width, height in points;
  backBufferWidth, backBufferHeight in pixels). Extensively documented re:
  status bar offset handling and GL coordinate system origin.
- **Shareable logic**: Pure data class, could be shared across mobile backends.

### IOSUIViewController.java
- **Implements**: extends `GLKViewController`
- **Lines**: ~178
- **iOS APIs used**:
  - `GLKViewController` — OpenGL frame loop controller
  - `UIScreen` — `getNativeScale`
  - `UIInterfaceOrientationMask`, `UIInterfaceOrientation` — orientation support
  - `UIDevice` — device idiom for portrait-upside-down on iPad
  - `UIRectEdge` — screen edge gesture deferral
  - `NSNotificationCenter` — keyboard show/hide notifications
  - `UIWindow.KeyboardWillShowNotification/KeyboardWillHideNotification` — keyboard events
  - `NSValue`, `NSDictionary`, `NSNumber` — keyboard frame info extraction
  - `UIView` — animation API for keyboard transitions
  - `UIPress`, `UIPressesEvent`, `UIKey` — hardware key events
  - `Foundation.getMajorSystemVersion()` — version-specific behavior
- **Key functionality**:
  - View lifecycle (viewWillAppear, viewDidAppear, viewDidLayoutSubviews)
  - Orientation support (portrait/landscape based on config)
  - Safe area inset updates on layout changes
  - Keyboard notification handling (height calculation, animation)
  - Hardware key press routing (pressesBegan/pressesEnded)
  - Status bar visibility, home indicator auto-hide
  - Content scale factor for retina
- **Shareable logic**: Keyboard height observer pattern.

### IOSViewControllerListener.java
- **Implements**: (standalone interface)
- **Lines**: ~32
- **iOS APIs used**: None
- **Key functionality**: Single method `viewDidAppear(boolean animated)` — callback
  for when the root view controller has appeared.
- **Shareable logic**: None (pure iOS callback).

### IOSAudio.java (interface)
- **Implements**: extends `Audio` (libGDX core)
- **Lines**: ~29
- **iOS APIs used**: None
- **Key functionality**: Adds iOS lifecycle hooks to Audio interface:
  `didBecomeActive()`, `willEnterForeground()`, `willResignActive()`.
- **Shareable logic**: Lifecycle-aware audio pattern. Android has similar
  pause/resume needs.

### DisabledIOSAudio.java
- **Implements**: `IOSAudio`
- **Lines**: ~58
- **iOS APIs used**: None
- **Key functionality**: No-op implementation of IOSAudio. All newSound/newMusic/
  newAudioDevice/newAudioRecorder throw GdxRuntimeException. Used when
  `config.useAudio = false`.
- **Shareable logic**: The disabled-audio pattern is reusable across all platforms.

---

## objectal/ Package (13 files)

These files provide Java bindings to the [ObjectAL](https://github.com/kstenerud/ObjectAL-for-iPhone)
Objective-C library, which wraps OpenAL and AVAudioPlayer for iOS audio.

### OALIOSAudio.java
- **Implements**: `IOSAudio`
- **Lines**: ~117
- **iOS APIs used**: Uses ObjectAL wrappers (OALSimpleAudio, OALAudioTrack, OALAudioSession)
- **Key functionality**:
  - Creates IOSSound and IOSMusic instances
  - Configures OALSimpleAudio (iPod allowance, ringer switch)
  - AudioDevice via OALIOSAudioDevice
  - AudioRecorder: returns null (not implemented)
  - Lifecycle: forceEndInterruption on resume (workaround for ObjectAL crash)
- **Shareable logic**: Factory pattern for Sound/Music creation.

### OALIOSAudioDevice.java
- **Implements**: `AudioDevice` (libGDX core)
- **Lines**: ~143
- **iOS APIs used**:
  - `ALSource` — OpenAL source for playback
  - `ALBuffer` — OpenAL buffer with `initWithNameDataSizeFormatFrequency`
  - `ALWrapper.bufferData()` — direct OpenAL buffer data upload
  - `OALAudioSession` — interruption state checking
  - `Struct`, `ShortPtr`, `VoidPtr` — RoboVM low-level memory management
- **Key functionality**:
  - Real-time audio output device (for procedural/streaming audio)
  - Buffer queue management (allocate N buffers, cycle through them)
  - Short/float sample conversion
  - Latency calculation
  - Volume, pause, resume control
- **Shareable logic**: Float-to-short sample conversion.

### OALSimpleAudio.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~72
- **iOS APIs used**:
  - `@NativeClass` + `@Library(Library.INTERNAL)` — binds to ObjectAL OALSimpleAudio
  - Methods: `sharedInstance()`, `preloadEffect()`, `unloadEffect()`, `playEffect()`,
    `playBuffer()`
  - Properties: `allowIpod`, `honorSilentSwitch`, `useHardwareIfAvailable`,
    `channelSource`
- **Key functionality**: Singleton access to ObjectAL's simple audio API.
  Sound effect preloading, playback with volume/pitch/pan/loop.
- **Shareable logic**: None (pure Objective-C binding).

### ALSource.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~77
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL ALSource
  - Properties: `sourceId`, `state`, `paused`, `volume`, `pitch`, `pan`, `looping`
  - Methods: `stop()`, `play()`, `buffersProcessed()`, `queueBuffer()`,
    `unqueueBuffer()`, `playing()`
- **Key functionality**: OpenAL source control — play, stop, pause, volume,
  pitch, pan, looping, buffer queue management.
- **Shareable logic**: None (pure Objective-C binding).

### ALBuffer.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~40
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL ALBuffer
  - Methods: `bufferId()`, `initWithNameDataSizeFormatFrequency()`
- **Key functionality**: OpenAL buffer wrapper — holds audio data.
- **Shareable logic**: None.

### ALChannelSource.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~22
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL ALChannelSource
  - Property: `sourcePool` (returns ALSoundSourcePool)
- **Key functionality**: Channel source that holds a pool of ALSources.
- **Shareable logic**: None.

### ALSoundSourcePool.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~23
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL ALSoundSourcePool
  - Property: `sources` (returns NSArray<ALSource>)
- **Key functionality**: Pool of OpenAL sources.
- **Shareable logic**: None.

### ALConsts.java
- **Implements**: (standalone constants)
- **Lines**: ~8
- **iOS APIs used**: None
- **Key functionality**: OpenAL format constants: `AL_FORMAT_MONO16` (0x1101),
  `AL_FORMAT_STEREO16` (0x1103).
- **Shareable logic**: These are standard OpenAL constants, identical on all platforms.

### ALWrapper.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~22
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL ALWrapper
  - Static method: `bufferData(bufferId, format, data, size, frequency)`
- **Key functionality**: Direct OpenAL `alBufferData` call via ObjectAL wrapper.
- **Shareable logic**: None.

### OALAudioSession.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~26
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL OALAudioSession
  - Methods: `sharedInstance()`, `interrupted()`, `forceEndInterruption()`
- **Key functionality**: Audio session management — interruption state check and
  force-end (workaround for background/foreground transitions).
- **Shareable logic**: None.

### OALAudioTrack.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~95
- **iOS APIs used**:
  - `@NativeClass` — binds to ObjectAL OALAudioTrack (wraps AVAudioPlayer)
  - Factory: `track()` (creates new instance)
  - Methods: `preloadFile()`, `playFile()`, `play()`, `stop()`, `clear()`
  - Properties: `paused`, `playing`, `volume`, `pan`, `currentTime`,
    `numberOfLoops`, `delegate`
- **Key functionality**: Music/streaming audio playback via AVAudioPlayer
  underneath ObjectAL.
- **Shareable logic**: None.

### AVAudioPlayerDelegate.java (interface)
- **Implements**: extends `NSObjectProtocol`
- **Lines**: ~29
- **iOS APIs used**:
  - `@Method(selector = "audioPlayerDidFinishPlaying:successfully:")` — delegate callback
- **Key functionality**: Completion callback protocol for audio playback.
- **Shareable logic**: None.

### AVAudioPlayerDelegateAdapter.java
- **Implements**: `NSObject` + `AVAudioPlayerDelegate`
- **Lines**: ~29
- **iOS APIs used**: `@NotImplemented` — RoboVM annotation for optional protocol methods
- **Key functionality**: Default adapter for AVAudioPlayerDelegate. Throws
  UnsupportedOperationException unless overridden.
- **Shareable logic**: None.

---

## custom/ Package (4 files)

These files provide Java bindings to deprecated UIKit APIs that were removed
from the RoboVM class library but are still used by the backend.

### HWMachine.java
- **Implements**: (standalone utility class)
- **Lines**: ~48
- **iOS APIs used**:
  - `@Library("c")` + `@Bridge` — direct C function binding
  - `sysctlbyname()` — BSD sysctl to query `hw.machine`
  - `BytePtr`, `MachineSizedUIntPtr`, `VoidPtr` — RoboVM low-level types
- **Key functionality**: Queries the hardware machine identifier string (e.g.
  "iPhone10,6" for iPhone X) via the `sysctlbyname` C API. Used by IOSGraphics
  to look up device PPI.
- **Shareable logic**: None (C system call).

### UIAcceleration.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~76
- **iOS APIs used**:
  - `@Library("UIKit")` + `@NativeClass` — binds to deprecated UIAcceleration class
  - Properties: `timestamp`, `x`, `y`, `z` (all double)
- **Key functionality**: Data object for accelerometer readings.
- **Note**: Deprecated since iOS 5.0. Should use CoreMotion (CMAccelerometerData)
  instead. The commented-out code in DefaultIOSInput shows CoreMotion was attempted.
- **Shareable logic**: None.

### UIAccelerometer.java
- **Implements**: extends `NSObject` (Objective-C binding)
- **Lines**: ~78
- **iOS APIs used**:
  - `@Library("UIKit")` + `@NativeClass` — binds to deprecated UIAccelerometer class
  - Properties: `updateInterval`, `delegate`
  - Static method: `sharedAccelerometer()`
- **Key functionality**: Singleton accelerometer with configurable update interval
  and delegate callback.
- **Note**: Deprecated since iOS 5.0.
- **Shareable logic**: None.

### UIAccelerometerDelegate.java (interface)
- **Implements**: extends `NSObjectProtocol`
- **Lines**: ~49
- **iOS APIs used**:
  - `@Method(selector = "accelerometer:didAccelerate:")` — accelerometer callback
- **Key functionality**: Delegate protocol for accelerometer updates.
- **Note**: Deprecated since iOS 5.0.
- **Shareable logic**: None.

### UIAccelerometerDelegateAdapter.java
- **Implements**: `NSObject` + `UIAccelerometerDelegate`
- **Lines**: ~51
- **iOS APIs used**: `@NotImplemented` — RoboVM annotation
- **Key functionality**: Default adapter for UIAccelerometerDelegate.
- **Note**: Deprecated since iOS 5.0.
- **Shareable logic**: None.

---

## Summary Statistics

| Category | Files | Approx. Total LOC |
|----------|------:|-------------------:|
| Root package | 21 | ~3,000 |
| objectal/ package | 13 | ~700 |
| custom/ package | 4 | ~250 |
| **Total** | **38** | **~3,950** |

## iOS Native API Dependency Map

| Framework | Classes Used | Used By |
|-----------|-------------|---------|
| **UIKit** | UIApplication, UIWindow, UIScreen, UIDevice, UIViewController, UIView, UITextField, UITextView, UIAlertController, UITouch, UIEvent, UIKey, UIPasteboard, UITableView, UIToolbar, UIBarButtonItem, UIImpactFeedbackGenerator, UIPress, UIPressesEvent | IOSApplication, DefaultIOSInput, IOSGraphics, IOSUIViewController, IOSHaptics, IOSNet |
| **GLKit** | GLKView, GLKViewController, GLKViewDelegate, GLKViewControllerDelegate, GLKViewDrawable* enums | IOSGraphics, IOSApplicationConfiguration |
| **OpenGL ES** | EAGLContext, EAGLRenderingAPI | IOSGraphics |
| **Foundation** | NSObject, NSString, NSNumber, NSArray, NSSet, NSDictionary, NSMutableDictionary, NSURL, NSBundle, NSProcessInfo, NSNotificationCenter, NSAutoreleasePool, NSError, NSValue | Nearly all files |
| **CoreGraphics** | CGRect, CGPoint, CGSize | IOSApplication, IOSGraphics, DefaultIOSInput, IOSUIViewController |
| **Core Haptics** | CHHapticEngine, CHHapticPattern, CHHapticPatternDict, CHHapticEventType, CHHapticEventParameterID | IOSHaptics |
| **AudioToolbox** | AudioServices | IOSHaptics (legacy vibration) |
| **GameController** | GCKeyboard | DefaultIOSInput (hardware keyboard detection) |
| **ObjectAL** (bundled) | OALSimpleAudio, OALAudioTrack, OALAudioSession, ALSource, ALBuffer, ALChannelSource, ALSoundSourcePool, ALWrapper | IOSSound, IOSMusic, OALIOSAudio, OALIOSAudioDevice |
| **C stdlib** | sysctlbyname | HWMachine |
| **RoboVM runtime** | VM, Bro, Struct, @Bridge, @NativeClass, @Library, @Method, @Property, NativeObject, ObjCRuntime | Nearly all files |

## Cross-Platform Shareability Assessment

### Already shared (via core libGDX Java code)
- `NetJavaImpl`, `NetJavaServerSocketImpl`, `NetJavaSocketImpl` — networking
- `AbstractInput`, `AbstractGraphics` — base implementations
- `FileHandle` — base file operations

### Could be shared / abstracted
1. **Application lifecycle pattern** — All backends have identical didFinishLaunching/
   willResignActive/willTerminate patterns. A trait-based approach could reduce
   duplication.
2. **Runnable queue** — `postRunnable` + `processRunnables` is identical across
   all backends.
3. **Frame timing** — Delta time, FPS calculation, frame ID tracking.
4. **Disabled audio** — No-op audio implementation.
5. **Preferences** — Key-value storage with flush; only the persistence backend differs.
6. **Screen bounds** — Data class for logical/physical screen dimensions.
7. **Config fields** — Many configuration options (orientation, GL version, hdpi mode,
   audio settings) are conceptually identical across mobile platforms.

### Entirely platform-specific (no sharing possible)
1. **GL bindings** (IOSGLES20/30) — native method bridges
2. **ObjectAL bindings** (entire objectal/ package) — Objective-C bindings
3. **UIKit integration** (touch handling, keyboard, view controllers)
4. **Core Haptics** integration
5. **HWMachine** sysctl call
6. **Deprecated UIAccelerometer** bindings

## Implications for SGE iOS Backend

For SGE's iOS backend via MobiVM (Scala JVM -> native ARM):

1. **MobiVM compatibility**: Scala 3 compiles to JVM bytecode. MobiVM can compile
   this to native ARM. The RoboVM/MobiVM binding annotations (`@NativeClass`,
   `@Library`, `@Method`, `@Property`) work at the bytecode level, so Scala classes
   using these annotations should work identically to Java.

2. **ObjectAL dependency**: The audio system depends on ObjectAL, a third-party
   Objective-C library that must be bundled with the iOS app. This is a native
   library dependency, not a JVM one.

3. **GLKit deprecation**: Apple deprecated GLKit in iOS 12 and removed it in
   later Xcode versions. The MetalANGLE backend (referenced by `IS_METALANGLE`)
   is the migration path. A modern SGE iOS backend should use MetalANGLE or
   a Metal-native approach instead of GLKit.

4. **UIAccelerometer deprecation**: The accelerometer code uses APIs deprecated
   since iOS 5.0. CoreMotion (CMMotionManager) should be used instead. The
   commented-out CMMotionManager code in DefaultIOSInput shows this was
   partially attempted.

5. **Audio alternatives**: ObjectAL is no longer actively maintained. Modern
   alternatives include AVAudioEngine (Apple's high-level audio API) or
   direct OpenAL (still available on iOS).

6. **Estimated porting effort**: ~3,950 lines of Java across 38 files. Most
   files are straightforward interface implementations with iOS API calls.
   The largest file (DefaultIOSInput at 1,472 lines) contains significant
   key mapping boilerplate (~270 lines) that could be generated.
