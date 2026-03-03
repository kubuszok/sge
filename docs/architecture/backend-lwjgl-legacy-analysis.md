# LibGDX Legacy LWJGL Backend (LWJGL2) Analysis

This document analyzes the legacy LWJGL backend (`gdx-backend-lwjgl`) which uses LWJGL 2.x.
It covers all files in the main directory and the `audio/` subdirectory, with detailed
comparison to the LWJGL3 backend where applicable.

**Source path:** `libgdx/backends/gdx-backend-lwjgl/src/com/badlogic/gdx/backends/lwjgl/`

---

## Table of Contents

1. [Main Directory Files (22 files)](#main-directory-files)
2. [Audio Directory Files (10 files)](#audio-directory-files)
3. [LWJGL2 vs LWJGL3 Audio Comparison](#lwjgl2-vs-lwjgl3-audio-comparison)
4. [Summary and Recommendations](#summary-and-recommendations)

---

## Main Directory Files

### LwjglApplication.java

- **Implements:** `LwjglApplicationBase`
- **Dependencies:** `org.lwjgl.opengl.Display`, `org.lwjgl.LWJGLException`, `org.lwjgl.Sys`
- **Functionality:** Main application class with Display-based game loop thread. Uses
  `Display.processMessages()`, `Display.update()`, `Display.sync()` for the render loop.
  Creates graphics, audio, input subsystems. Supports `Canvas` parent for Swing embedding.
  Background thread mode where rendering happens on a dedicated thread.
- **Status:** Legacy. LWJGL3 equivalent (`Lwjgl3Application`) uses GLFW for window management
  and event polling. No unique code worth porting.

### LwjglApplicationBase.java

- **Implements:** Extends `Application` interface
- **Dependencies:** None beyond LibGDX core
- **Functionality:** Interface adding `createAudio()` returning `LwjglAudio` and
  `createInput()` returning `LwjglInput`. Provides factory method pattern for subsystem creation.
- **Status:** LWJGL2-specific interface. LWJGL3 does not have this intermediate interface;
  it configures subsystems directly in `Lwjgl3Application`.

### LwjglApplicationConfiguration.java

- **Implements:** None (POJO)
- **Dependencies:** `org.lwjgl.opengl.Display`, LibGDX `Color`, `Files.FileType`
- **Functionality:** Configuration class with fields for: audio settings
  (`simultaneousSources=16`, `bufferSize=512`, `bufferCount=9`), window properties
  (title, width, height, fullscreen, vSync, resizable), GL version selection
  (useGL30, gles30ContextMajorVersion/MinorVersion), HDPI mode, foreground/background FPS,
  initial color, preferences directory/type, and utility methods for display modes.
- **Status:** Legacy. LWJGL3's `Lwjgl3ApplicationConfiguration` has similar fields but adds
  GLFW-specific options (window icon, decorated, maximized, etc.).

### LwjglApplicationLogger.java

- **Implements:** `ApplicationLogger`
- **Dependencies:** None
- **Functionality:** Trivial logger that prints `[tag] message` to `System.out` (log/debug)
  or `System.err` (error). Error variant prints stack trace.
- **Status:** Identical pattern to LWJGL3's `Lwjgl3ApplicationLogger`. No unique code.

### LwjglGraphics.java

- **Implements:** `Graphics`
- **Dependencies:** `org.lwjgl.opengl.Display`, `org.lwjgl.opengl.PixelFormat`,
  `org.lwjgl.opengl.SharedDrawable`, `org.lwjgl.openal.AL`
- **Functionality:** Display-based graphics subsystem. Handles pixel format negotiation
  with progressive fallback (try 8/8/8/8 RGBA, then reduce to 0/0/0/0). GL context creation
  for GL20 or GL30. Software rendering fallback. Canvas support via `Display.setParent()`.
  Inner classes: `LwjglDisplayMode`, `LwjglMonitor`, `SetDisplayModeCallback`.
  No GL31 or GL32 support (LWJGL3 adds these).
- **Status:** Legacy. Display API is fundamentally different from GLFW. Not portable.

### DefaultLwjglInput.java

- **Implements:** `LwjglInput`
- **Dependencies:** `org.lwjgl.input.Keyboard`, `org.lwjgl.input.Mouse`,
  `org.lwjgl.opengl.Display`
- **Functionality:** Input via LWJGL2 keyboard/mouse polling. Massive `getGdxKeyCode()`
  switch statement mapping LWJGL2 key constants to LibGDX key constants. Uses
  `InputEventQueue` for buffered event processing. Delta tracking for mouse movement.
  Cursor visibility via `Mouse.setGrabbed()`.
- **Status:** Legacy. LWJGL3 uses GLFW callbacks (`glfwSetKeyCallback`, etc.) which is
  fundamentally different. The key mapping table is LWJGL2-specific.

### LwjglInput.java

- **Implements:** Extends `Input` interface
- **Dependencies:** None beyond LibGDX core
- **Functionality:** Interface adding `update()` and `processEvents()` to the standard
  `Input` interface. Called from the main game loop.
- **Status:** LWJGL2-specific interface pattern. LWJGL3 processes events directly via
  `glfwPollEvents()`.

### LwjglAWTCanvas.java

- **Implements:** `LwjglApplicationBase`
- **Dependencies:** `org.lwjgl.opengl.AWTGLCanvas`, `java.awt.*`, `javax.swing.*`
- **Functionality:** AWT GL canvas embedding. Supports shared GL context between multiple
  instances. EDT rendering via `AWTGLCanvas.paint()`. Multiple canvas support with a single
  shared audio instance. Mouse/keyboard input via AWT events (`LwjglAWTInput`).
  Resize handling, focus management, and proper shutdown coordination.
- **Status:** **LWJGL2-only.** No equivalent in LWJGL3. AWT/Swing embedding is a unique
  LWJGL2 feature. Not relevant for SGE (web and native targets don't have AWT).

### LwjglAWTFrame.java

- **Implements:** Extends `JFrame`
- **Dependencies:** `java.awt.*`, `javax.swing.*`
- **Functionality:** JFrame wrapper for `LwjglAWTCanvas`. Adds shutdown hook with
  `Runtime.halt(0)` to force exit. Window close handling. DPI-aware sizing.
- **Status:** **LWJGL2-only.** Legacy Swing wrapper. Not relevant for SGE.

### LwjglAWTInput.java

- **Implements:** `LwjglInput`, plus `MouseListener`, `MouseMotionListener`,
  `MouseWheelListener`, `KeyListener`
- **Dependencies:** `java.awt.*`, `java.awt.event.*`, `java.awt.Robot`
- **Functionality:** AWT event-based input handling. Own key translation table mapping
  `KeyEvent.VK_xxx` constants to LibGDX `Input.Keys`. Uses `java.awt.Robot` for cursor
  warping (setCursorCatched). Touch/mouse event translation from AWT to LibGDX format.
  Pressure support (always 0 for AWT). Scroll wheel handling.
- **Status:** **LWJGL2-only.** AWT-specific input. Not relevant for SGE.

### LwjglApplet.java

- **Implements:** Extends `java.applet.Applet`
- **Dependencies:** `java.applet.Applet` (deprecated/removed in modern JDKs)
- **Functionality:** Java Applet wrapper for LibGDX. Annotated with
  `@SuppressWarnings("removal")`. Creates an `LwjglCanvas` within the applet context.
- **Status:** **Obsolete.** Java Applets were removed from modern browsers and deprecated
  in Java 9+. Not relevant for any modern use.

### LwjglCanvas.java

- **Implements:** `LwjglApplicationBase`
- **Dependencies:** `org.lwjgl.opengl.Display`, `java.awt.*`, `javax.swing.*`
- **Functionality:** Canvas embedding via `Display.setParent(Canvas)`. EDT game loop using
  recursive `EventQueue.invokeLater()`. DPI scaling support. Posted-runnable stack trace
  debugging (captures creation-time stack traces for debugging). Focus management for
  Display.reshape workaround.
- **Status:** **LWJGL2-only.** Swing embedding via Display.setParent. Not available in LWJGL3.

### LwjglFrame.java

- **Implements:** Extends `JFrame`
- **Dependencies:** `java.awt.*`, `javax.swing.*`
- **Functionality:** JFrame wrapper for `LwjglCanvas`. DPI-aware. Focus workaround for
  Display.reshape issue. Window close handling with proper game loop shutdown.
- **Status:** **LWJGL2-only.** Legacy Swing wrapper.

### LwjglClipboard.java

- **Implements:** `Clipboard`
- **Dependencies:** `java.awt.Toolkit`, `java.awt.datatransfer.*`
- **Functionality:** AWT clipboard for string content and file list (hasContents checks
  both `stringFlavor` and `javaFileListFlavor`).
- **Status:** Same approach as LWJGL3's `Lwjgl3Clipboard` (both use AWT toolkit).
  No unique code.

### LwjglCursor.java

- **Implements:** `Cursor`
- **Dependencies:** `org.lwjgl.input.Cursor` (LWJGL2), `org.lwjgl.LWJGLException`
- **Functionality:** LWJGL2 cursor requiring power-of-two pixmap dimensions. RGBA8888 to
  ARGB8888 conversion with vertical flip. `getMaxCursorSize()` queries LWJGL2 cursor caps.
- **Status:** **Legacy.** LWJGL3 uses GLFW cursors with no power-of-two restriction.

### LwjglFileHandle.java

- **Implements:** Extends `FileHandle`
- **Dependencies:** `com.badlogic.gdx.Files.FileType`
- **Functionality:** Desktop file handle with constructors for `File` and `String` path.
- **Status:** Identical pattern to LWJGL3's `Lwjgl3FileHandle`. No unique code.

### LwjglFiles.java

- **Implements:** `Files`
- **Dependencies:** None beyond LibGDX core
- **Functionality:** Desktop files implementation. `externalPath = System.getProperty("user.home") + "/"`,
  `localPath = new File("").getAbsolutePath() + "/"`.
- **Status:** Identical to LWJGL3's `Lwjgl3Files`. No unique code.

### LwjglGL20.java

- **Implements:** `GL20`
- **Dependencies:** `org.lwjgl.opengl.*` (GL11, GL13, GL14, GL15, GL20, GL41,
  `EXTFramebufferObject`)
- **Functionality:** ~860 lines mapping all LibGDX GL20 methods to LWJGL2 OpenGL bindings.
  Notable: uses `EXTFramebufferObject` for FBO operations (glGenFramebuffers,
  glBindFramebuffer, glFramebufferTexture2D, etc.) since LWJGL2's GL20 doesn't include
  core FBO.
- **Status:** **Legacy.** LWJGL3's GL20 uses standard LWJGL3 bindings with core FBO support.
  The `EXTFramebufferObject` usage is LWJGL2-specific.

### LwjglGL30.java

- **Implements:** `GL30`, extends `LwjglGL20`
- **Dependencies:** `org.lwjgl.opengl.*` (GL30-GL43 classes)
- **Functionality:** GL30 extending LwjglGL20. Maps ES3.0 functions to desktop GL3.0-4.3.
  Overrides FBO methods to use core GL30 instead of `EXTFramebufferObject`.
- **Status:** **Legacy.** LWJGL3 has GL30 + GL31 + GL32 support.

### LwjglNativesLoader.java

- **Implements:** None (static utility)
- **Dependencies:** `com.badlogic.gdx.utils.SharedLibraryLoader`, `org.lwjgl.Sys`
- **Functionality:** Extracts LWJGL2 native libraries, sets `org.lwjgl.librarypath` system
  property. Java Web Start (JWS) detection and special handling. Singleton pattern with
  `nativesLoaded` flag.
- **Status:** **Legacy.** LWJGL3 manages its own native loading differently.

### LwjglNet.java

- **Implements:** `Net`
- **Dependencies:** `org.lwjgl.Sys`, `com.badlogic.gdx.net.*`
- **Functionality:** Network implementation using `Sys.openURL()` for browser opening
  (LWJGL2-specific). Rest is shared Java networking via `NetJavaImpl` and
  `NetJavaServerSocketImpl`/`NetJavaSocketImpl`.
- **Status:** `Sys.openURL()` is LWJGL2-specific. LWJGL3 uses `java.awt.Desktop.browse()`.

### LwjglPreferences.java

- **Implements:** `Preferences`
- **Dependencies:** `java.util.Properties`, `java.io.*`
- **Functionality:** XML Properties-based preferences. Loads from XML, saves to XML.
  All put/get methods with type conversion.
- **Status:** Identical to LWJGL3's `Lwjgl3Preferences`. No unique code.

---

## Audio Directory Files

### LwjglAudio.java

- **Implements:** Extends `Audio` interface
- **Dependencies:** None beyond LibGDX core
- **Functionality:** Interface adding `update()` method to the standard `Audio` interface.
  Called from the main game loop to process streaming music buffers.
- **Status:** Identical to LWJGL3's `Lwjgl3Audio` interface. Same pattern, different name.

### OpenALLwjglAudio.java

- **Implements:** `LwjglAudio`
- **Dependencies:** `org.lwjgl.openal.AL`, `org.lwjgl.openal.AL10`, `org.lwjgl.openal.AL11`
- **Functionality:** Central audio manager (~390 lines). Key responsibilities:
  - **Initialization:** `AL.create()` (LWJGL2-specific). No device selection.
  - **Codec registration:** Uses **reflection** (`Class.getConstructor().newInstance()`)
    to create Sound/Music instances from file extensions.
  - **Source pool:** Fixed array of `simultaneousSources` OpenAL sources. Tracks
    `isMusic` flag per source. LRU eviction for sounds.
  - **Sound ID mapping:** `getSoundId(sourceID)` / `stopSound(soundId)` using
    `soundIdToSource` and `sourceToSoundId` LongIntMaps.
  - **Music update:** Iterates `music` array calling `update()` on each streaming instance.
  - **No device switching.** No disconnect detection.
- **Status:** Significantly different from LWJGL3's `OpenALLwjgl3Audio`. See comparison below.

### OpenALMusic.java

- **Implements:** `Music` (abstract class)
- **Dependencies:** `org.lwjgl.BufferUtils`, `org.lwjgl.openal.AL10`, `org.lwjgl.openal.AL11`
- **Functionality:** Abstract streaming music player (~298 lines). Triple-buffered
  (3 buffers x 40960 bytes). Abstract methods: `read(byte[])`, `reset()`, `loop()`.
  Features: play/stop/pause, seeking via read-and-discard, volume/pan, looping,
  completion callback, buffer underflow recovery.
- **Status:** **98% identical to LWJGL3.** Key difference: format calculation is inline
  (`channels > 1 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16`) vs LWJGL3 using
  `OpenALUtils.determineFormat(channels, bitDepth)`. This means LWJGL2 only supports
  16-bit mono/stereo music, while LWJGL3 supports 8/16/32/64-bit up to 7.1 channels.

### OpenALSound.java

- **Implements:** `Sound`
- **Dependencies:** `org.lwjgl.openal.AL10`, `com.badlogic.gdx.utils.BufferUtils`
- **Functionality:** Fully-loaded sound effect (~211 lines). Single AL buffer with all PCM
  data. Play/loop with volume/pitch/pan. Stop by sound ID or all instances. Pause/resume.
  Duration tracking. Type chain (`setType()`/`getType()`) for codec fallback detection.
- **Status:** **90% identical to LWJGL3.** Differences:
  - LWJGL2 has only `setup(byte[], channels, bitDepth, sampleRate)`.
  - LWJGL3 adds `setup(ShortBuffer, channels, bitDepth, sampleRate)` overload.
  - LWJGL2 uses inline format: `channels > 1 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16`.
  - LWJGL3 uses `OpenALUtils.determineFormat(channels, bitDepth)` supporting more formats.
  - LWJGL2 doesn't validate byte alignment; LWJGL3 trims to valid sample boundaries.

### OpenALAudioDevice.java

- **Implements:** `AudioDevice`
- **Dependencies:** `org.lwjgl.BufferUtils`, `org.lwjgl.openal.AL10`, `org.lwjgl.openal.AL11`
- **Functionality:** Real-time audio output device (~217 lines). Converts float/short samples
  to 16-bit PCM bytes. Blocking buffer fill with sleep-wait loop. Configurable buffer size
  and count. Volume control. Position tracking via rendered seconds + AL_SEC_OFFSET.
- **Status:** **99% identical to LWJGL3.** Only difference is the audio field type
  (`OpenALLwjglAudio` vs `OpenALLwjgl3Audio`).

### Mp3.java

- **Implements:** Contains inner `Music extends OpenALMusic` and `Sound extends OpenALSound`
- **Dependencies:** `javazoom.jl.decoder.*` (JLayer)
- **Functionality:** MP3 codec (~142 lines).
  - **Music:** Streams MP3 frame-by-frame via JLayer's `Bitstream`/`MP3Decoder`/
    `OutputBuffer`. Resets by closing and reopening stream.
  - **Sound:** Fully decodes MP3 to PCM byte array, then calls `setup()`.
- **Status:** **99% identical to LWJGL3.** Only difference is constructor parameter types
  (`OpenALLwjglAudio` vs `OpenALLwjgl3Audio`).

### Ogg.java

- **Implements:** Contains inner `Music extends OpenALMusic` and `Sound extends OpenALSound`
- **Dependencies:** `OggInputStream` (local)
- **Functionality:** OGG Vorbis codec (~80 lines).
  - **Music:** Streams via `OggInputStream`. Identical to LWJGL3.
  - **Sound:** Fully decodes via `OggInputStream` loop, accumulating bytes.
- **Status:** **Music is identical to LWJGL3. Sound has a significant difference:**
  - LWJGL2 `Ogg.Sound`: Decodes via `OggInputStream` (JOrbis) in a byte-accumulation loop.
  - LWJGL3 `Ogg.Sound`: Uses `STBVorbis.stb_vorbis_decode_memory()` (native) for
    single-call full decode, returning a `ShortBuffer` directly.

### OggInputStream.java

- **Implements:** Extends `InputStream`
- **Dependencies:** `org.xiph.ogg.*` (JOgg), `com.jcraft.jorbis.*` (JOrbis)
- **Functionality:** JOrbis-based OGG Vorbis streaming decoder (~472 lines). Reads OGG pages
  and packets, decodes to PCM via JOrbis synthesis. Handles multi-channel audio. Provides
  `getChannels()`, `getSampleRate()`, `atEnd()`.
- **Status:** **98% identical to LWJGL3.** Key difference:
  - LWJGL2 clamps `getChannels()` to `Math.min(2, oggInfo.channels)` and adjusts
    `getSampleRate()` for surround (divides by channels/2 when channels > 2).
  - LWJGL3 returns raw `oggInfo.channels` and `oggInfo.rate` without adjustment.
  - This means LWJGL2 downmixes surround to stereo; LWJGL3 passes through all channels.

### Wav.java

- **Implements:** Contains inner `Music extends OpenALMusic`, `Sound extends OpenALSound`,
  and `WavInputStream extends FilterInputStream`
- **Dependencies:** `com.badlogic.gdx.utils.StreamUtils`
- **Functionality:** WAV file parser (~180 lines). `WavInputStream` reads RIFF header,
  seeks to `fmt ` chunk (reads type, channels, sampleRate, bitDepth), seeks to `data` chunk.
  Supports reading data with remaining-byte tracking.
- **Status:** **90% identical to LWJGL3.** Differences:
  - LWJGL2 supports only PCM (type `0x0001`).
  - LWJGL3 adds IEEE float WAV support (type `0x0003`) with 32/64-bit validation.
  - LWJGL3 adds MP3-in-WAV detection (type `0x0055` triggers `setType("mp3")`).
  - LWJGL2 limits channels to 2 (`channels > 2` throws exception).
  - LWJGL3 removes the channel limit (supports up to 7.1 via `OpenALUtils`).
  - LWJGL3 adds `getCodecName()` for human-readable error messages.

### JavaSoundAudioRecorder.java

- **Implements:** `AudioRecorder`
- **Dependencies:** `javax.sound.sampled.*` (TargetDataLine, AudioFormat, AudioSystem)
- **Functionality:** Audio recording via Java Sound API (~59 lines). Opens a
  `TargetDataLine` with specified sample rate and mono/stereo. Reads samples as 16-bit
  signed little-endian PCM, converts to short[] or float[]. Dispose closes the line.
- **Status:** **Identical to LWJGL3's `JavaSoundAudioRecorder`.** No differences.

---

## LWJGL2 vs LWJGL3 Audio Comparison

### Summary Table

| Class | LWJGL2 | LWJGL3 | Similarity |
|-------|--------|--------|------------|
| Audio interface | `LwjglAudio` | `Lwjgl3Audio` | 100% (name only) |
| Central manager | `OpenALLwjglAudio` | `OpenALLwjgl3Audio` | ~60% |
| OpenALMusic | `OpenALMusic` | `OpenALMusic` | ~98% |
| OpenALSound | `OpenALSound` | `OpenALSound` | ~90% |
| OpenALAudioDevice | `OpenALAudioDevice` | `OpenALAudioDevice` | ~99% |
| Mp3 | `Mp3` | `Mp3` | ~99% |
| Ogg.Music | `Ogg.Music` | `Ogg.Music` | ~99% |
| Ogg.Sound | `Ogg.Sound` | `Ogg.Sound` | ~30% |
| OggInputStream | `OggInputStream` | `OggInputStream` | ~98% |
| Wav | `Wav` | `Wav` | ~90% |
| JavaSoundAudioRecorder | `JavaSoundAudioRecorder` | `JavaSoundAudioRecorder` | 100% |
| OpenALUtils | N/A | `OpenALUtils` | LWJGL3 only |

### Detailed Differences

#### Central Audio Manager (OpenALLwjglAudio vs OpenALLwjgl3Audio)

This is where the largest differences exist:

| Feature | LWJGL2 | LWJGL3 |
|---------|--------|--------|
| **OpenAL init** | `AL.create()` | `alcOpenDevice()` + `alcCreateContext()` + `alcMakeContextCurrent()` |
| **Codec registration** | Reflection: `Class.getConstructor().newInstance()` | Lambda: `BiFunction<Audio, FileHandle, Sound/Music>` |
| **Format support** | Mono16/Stereo16 only | 8/16/32/64-bit, mono through 7.1 (via `OpenALUtils`) |
| **Device switching** | Not supported | `SOFTReopenDevice` extension |
| **Disconnect detection** | Not supported | Observer thread polls `ALC_CONNECTED` |
| **Direct channels** | Not supported | `SOFTDirectChannels` extension |
| **Device enumeration** | Not supported | `ALC_ALL_DEVICES_SPECIFIER` |
| **Lines of code** | ~390 | ~460 |
| **Error reporting** | Basic | AL error code translation |

#### OpenALSound

- LWJGL2 only has `setup(byte[], channels, bitDepth, sampleRate)`.
- LWJGL3 adds `setup(ShortBuffer, channels, bitDepth, sampleRate)` for native buffer support
  (used by STBVorbis).
- LWJGL3 validates byte alignment: `validBytes = pcm.length - (pcm.length % (channels * (bitDepth >> 3)))`.
- LWJGL3 uses `OpenALUtils.determineFormat()` supporting multi-channel and high bit-depth.

#### Ogg.Sound (Most Significant Difference)

- **LWJGL2:** Decodes OGG via `OggInputStream` (JOrbis, pure Java). Reads in a loop,
  accumulating byte arrays, then copies to final buffer.
- **LWJGL3:** Uses `STBVorbis.stb_vorbis_decode_memory()` (native C library). Single call
  returns complete decoded `ShortBuffer`. Reads the entire file into a `ByteBuffer` first,
  then hands it to STBVorbis.

This is a fundamental architectural difference: LWJGL2 uses pure Java decoding (JOrbis),
while LWJGL3 leverages native STB libraries.

#### Wav.java

- LWJGL3 adds IEEE float WAV support (format type `0x0003`), validating 32/64-bit depth.
- LWJGL3 adds MP3-in-WAV detection (format type `0x0055`), setting type to "mp3" for
  fallback handling.
- LWJGL3 removes the 2-channel limit present in LWJGL2.
- LWJGL3 adds `getCodecName()` for human-readable error messages on unsupported formats.

#### OggInputStream

- LWJGL2 clamps channels: `Math.min(2, oggInfo.channels)` and adjusts sample rate for
  surround audio (divides by `channels/2`). This effectively downmixes surround to stereo.
- LWJGL3 passes through raw channel count and sample rate. Multi-channel audio is handled
  by `OpenALUtils.determineFormat()`.

#### OpenALUtils (LWJGL3 only)

This utility class has no LWJGL2 equivalent. It provides `determineFormat(channels, bitDepth)`
which maps channel counts (1-8) and bit depths (8, 16, 32, 64) to OpenAL format constants
including `AL_FORMAT_QUAD16`, `AL_FORMAT_51CHN16`, `AL_FORMAT_61CHN16`, `AL_FORMAT_71CHN16`
and their 8/32-bit variants.

---

## Summary and Recommendations

### Classification of LWJGL2 Files

**Obsolete (not needed for SGE):**
- `LwjglApplet.java` -- Java Applets are dead
- `LwjglAWTCanvas.java` -- AWT embedding, no cross-platform equivalent
- `LwjglAWTFrame.java` -- AWT frame wrapper
- `LwjglAWTInput.java` -- AWT input handling
- `LwjglCanvas.java` -- Swing embedding via Display.setParent
- `LwjglFrame.java` -- Swing frame wrapper
- `LwjglCursor.java` -- Power-of-two cursor limitation
- `LwjglNativesLoader.java` -- LWJGL2 native extraction

**Identical to LWJGL3 (use LWJGL3 version):**
- `LwjglApplicationLogger.java`
- `LwjglClipboard.java`
- `LwjglFileHandle.java`
- `LwjglFiles.java`
- `LwjglPreferences.java`
- `JavaSoundAudioRecorder.java`
- `OpenALAudioDevice.java` (99% identical)
- `Mp3.java` (99% identical)

**LWJGL2-specific (replaced by different LWJGL3 implementations):**
- `LwjglApplication.java` -- Display vs GLFW
- `LwjglGraphics.java` -- Display vs GLFW
- `DefaultLwjglInput.java` -- LWJGL2 polling vs GLFW callbacks
- `LwjglGL20.java` -- Different LWJGL binding packages
- `LwjglGL30.java` -- Different LWJGL binding packages
- `LwjglNet.java` -- `Sys.openURL()` vs `Desktop.browse()`
- `OpenALLwjglAudio.java` -- Different init, codec registration, device management

**Near-identical audio (LWJGL3 version is strictly better):**
- `OpenALMusic.java` -- LWJGL3 adds multi-channel format support
- `OpenALSound.java` -- LWJGL3 adds ShortBuffer setup, byte alignment, multi-format
- `OggInputStream.java` -- LWJGL3 preserves surround channels
- `Wav.java` -- LWJGL3 adds float WAV, MP3-in-WAV, removes channel limit
- `Ogg.java` -- Music identical; LWJGL3 Sound uses native STBVorbis

### Recommendation for SGE Audio Architecture

The LWJGL3 audio subsystem is strictly superior to LWJGL2 in every aspect:

1. **Use LWJGL3 audio as the reference** for SGE's JVM desktop audio backend.
2. **`OpenALUtils.determineFormat()`** should be ported as a shared utility.
3. **`OpenALMusic`** and **`OpenALAudioDevice`** are nearly identical between backends --
   the LWJGL3 versions with format utility support should be the basis.
4. **`Ogg.Sound`** shows the biggest win: STBVorbis native decoding is faster and simpler
   than JOrbis. For SGE's JS target, a pure-Scala OGG decoder or Web Audio API would be
   needed instead.
5. **Device switching** (LWJGL3's `SOFTReopenDevice`) is a modern feature worth porting.
6. **Lambda-based codec registration** (LWJGL3) is cleaner than reflection (LWJGL2) and
   aligns better with Scala's functional style.
7. **No LWJGL2-specific code needs to be ported.** Everything useful in LWJGL2 audio exists
   in improved form in LWJGL3.
