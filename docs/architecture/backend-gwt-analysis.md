# LibGDX GWT Backend -- Deep Analysis for SGE Scala.js Target

Comprehensive analysis of every file in `libgdx/backends/gdx-backends-gwt/src/com/badlogic/gdx/backends/gwt/`.

---

## Part 1: Main Backend Files (24 files)

### 1. GwtApplication.java

- **Implements**: `EntryPoint` (GWT), `Application` (LibGDX)
- **Browser APIs**: `requestAnimationFrame`, `document.visibilitychange`, `window.performance.memory`, `window.navigator.userAgent`, DOM element manipulation
- **Key functionality**:
  - Entry point for the entire GWT application (`onModuleLoad()`)
  - Sets up the main game loop via `AnimationScheduler.requestAnimationFrame`
  - Manages preloader (asset pre-fetching before game starts)
  - Handles browser resize events (`ResizeHandler`)
  - Visibility change detection (pause/resume on tab switch)
  - User agent sniffing for browser detection (`AgentInfo` via JSNI)
  - Creates all subsystems: graphics, audio, input, net, files, clipboard
  - Queues runnables for main-thread execution
- **Scala.js mapping**:
  - `requestAnimationFrame` -> `dom.window.requestAnimationFrame`
  - `visibilitychange` -> `dom.document.addEventListener("visibilitychange", ...)`
  - `EntryPoint.onModuleLoad()` -> Scala.js `@JSExportTopLevel` or `def main()`
  - `performance.memory` -> `js.Dynamic` access (non-standard Chrome API)
  - `navigator.userAgent` -> `dom.window.navigator.userAgent`
  - GWT widgets (Panel, VerticalPanel) -> direct DOM manipulation via scalajs-dom

### 2. GwtApplicationConfiguration.java

- **Implements**: None (configuration POJO)
- **Browser APIs**: None
- **Key functionality**:
  - Canvas dimensions (width, height), resizable vs fixed-size
  - WebGL options: stencil, antialiasing, alpha, premultipliedAlpha, preserveDrawingBuffer
  - `useGL30` flag for WebGL2 vs WebGL1
  - `usePhysicalPixels` for HiDPI/Retina support
  - `xrCompatible` for WebXR
  - `fullscreenOrientation` for mobile fullscreen
  - Padding for resizable applications
  - `useDebugGL` for error-checking GL wrapper
- **Scala.js mapping**: Direct translation to a Scala case class or config object. No browser APIs involved.

### 3. GwtGraphics.java

- **Implements**: `AbstractGraphics` (LibGDX)
- **Browser APIs**: `Canvas`, `WebGLRenderingContext`, `WebGL2RenderingContext`, `WebGLContextAttributes`, `screen.width/height/colorDepth`, `devicePixelRatio`, `requestFullscreen`, `exitFullscreen`, `fullscreenElement`, `screen.orientation.lock/unlock`, `document.title`
- **Key functionality**:
  - Creates HTML5 Canvas element and obtains WebGL context
  - WebGL1 (GL20) and WebGL2 (GL30) context creation with fallback
  - Canvas sizing with HiDPI support (`devicePixelRatio`)
  - Fullscreen API with vendor prefixes (standard, webkit, moz, ms)
  - Screen orientation locking for mobile
  - FPS/delta time tracking
  - Display mode and monitor information
  - Cursor management (delegates to GwtCursor)
  - GL version detection
- **Scala.js mapping**:
  - `Canvas.createIfSupported()` -> `dom.document.createElement("canvas").asInstanceOf[dom.html.Canvas]`
  - `WebGLRenderingContext` -> `canvas.getContext("webgl")` via scalajs-dom
  - `WebGL2RenderingContext` -> `canvas.getContext("webgl2")`
  - `requestFullscreen` -> `dom.Element.requestFullscreen()` (scalajs-dom has this)
  - `devicePixelRatio` -> `dom.window.devicePixelRatio`
  - Vendor-prefixed APIs mostly unnecessary now (modern browsers use standard APIs)

### 4. GwtGL20.java

- **Implements**: `GL20` (LibGDX)
- **Browser APIs**: `WebGLRenderingContext` (all WebGL1 methods), TypedArrays (`Float32Array`, `Int32Array`, `Int16Array`, `Int8Array`, `Uint8Array`)
- **Key functionality**:
  - Complete WebGL1 binding (~120 methods)
  - Integer-keyed maps to translate GL integer handles to WebGL object references (`WebGLProgram`, `WebGLShader`, `WebGLBuffer`, `WebGLTexture`, `WebGLFramebuffer`, `WebGLRenderbuffer`, `WebGLUniformLocation`)
  - Buffer conversion: Java NIO buffers -> TypedArray views for GL calls
  - Special Pixmap handling in `glTexImage2D` (uses HTMLImageElement/VideoElement/CanvasElement directly)
  - Some methods throw `GdxRuntimeException` (compressed textures, vertex arrays with client buffers)
- **Scala.js mapping**:
  - This is the **most critical file** for the Scala.js backend
  - scalajs-dom provides `WebGLRenderingContext` facade
  - TypedArrays available via `scala.scalajs.js.typedarray`
  - The integer-handle-to-object mapping pattern must be replicated
  - NIO buffer -> TypedArray conversion: SGE already has `BufferOpsJs` in `sge.platform`; this is the same problem domain
  - Key challenge: Pixmap in SGE is Canvas-based on JS, same as GWT

### 5. GwtGL30.java

- **Implements**: `GL30` (LibGDX), extends `GwtGL20`
- **Browser APIs**: `WebGL2RenderingContext` (all WebGL2 methods), additional WebGL2 objects (`WebGLQuery`, `WebGLSampler`, `WebGLTransformFeedback`, `WebGLVertexArrayObject`)
- **Key functionality**:
  - Complete WebGL2 binding (extends GL20 methods with GL30 additions)
  - VAO, query, sampler, transform feedback object management
  - 3D textures (`texImage3D`, `texSubImage3D`)
  - Instanced drawing, draw buffers, blit framebuffer
  - Some operations unsupported (`glMapBufferRange`, `glUnmapBuffer`, `glProgramParameteri`)
- **Scala.js mapping**: Same pattern as GwtGL20 but targeting `WebGL2RenderingContext`. scalajs-dom should provide the facade.

### 6. GwtGL20Debug.java / GwtGL30Debug.java

- **Implements**: extends `GwtGL20` / `GwtGL30`
- **Browser APIs**: Same as parent, plus `gl.getError()`
- **Key functionality**: Debug wrappers that call `glGetError()` after every GL call and throw on error
- **Scala.js mapping**: Straightforward decorator pattern, no special browser APIs needed

### 7. DefaultGwtInput.java

- **Implements**: `AbstractInput`, `GwtInput` (which extends `Input`)
- **Browser APIs**: DOM events (`mousedown`, `mouseup`, `mousemove`, `mousewheel`/`DOMMouseScroll`, `keydown`, `keyup`, `keypress`, `touchstart`, `touchmove`, `touchcancel`, `touchend`, `blur`), `Touch` API, `pointerLockElement` / `requestPointerLock`, `movementX`/`movementY`, `screen.orientation`, `ontouchstart` detection
- **Key functionality**:
  - Full mouse input with button tracking, delta movement
  - Multi-touch support (up to 20 touches)
  - Keyboard input with keyCode-to-LibGDX-key mapping
  - Cursor capture (Pointer Lock API)
  - Accelerometer support (via `GwtAccelerometer`)
  - Gyroscope support (via `GwtGyroscope`)
  - Focus tracking (blur events)
  - Mouse wheel with browser-specific normalization
  - Screen orientation and rotation
- **Scala.js mapping**:
  - All DOM events available via `scalajs-dom`
  - `addEventListener` -> `dom.EventTarget.addEventListener`
  - `Touch` events -> `dom.TouchEvent`, `dom.Touch`
  - Pointer Lock -> `dom.Element.requestPointerLock()`
  - Key code mapping: rewrite `keyForCode` as Scala match expression
  - Accelerometer/Gyroscope -> Generic Sensor API via `js.Dynamic`

### 8. GwtInput.java

- **Implements**: `Input` (extends it)
- **Browser APIs**: None
- **Key functionality**: Interface adding `reset()` method to `Input`
- **Scala.js mapping**: Trivial trait

### 9. DefaultGwtAudio.java

- **Implements**: `GwtAudio` (which extends `Audio`)
- **Browser APIs**: Web Audio API (via `WebAudioAPIManager`), `navigator.mediaDevices.getUserMedia`, `navigator.mediaDevices.enumerateDevices`, Feature Policy for speaker-selection
- **Key functionality**:
  - Sound and Music creation via Web Audio API
  - Output device switching (`setSinkId`)
  - Device enumeration for audio output selection
  - AudioDevice and AudioRecorder not supported in browser
- **Scala.js mapping**:
  - Web Audio API -> `js.Dynamic` or typed facades (no scalajs-dom facade for AudioContext as of 2025)
  - `getUserMedia` -> `dom.MediaDevices.getUserMedia()`
  - `enumerateDevices` -> `dom.MediaDevices.enumerateDevices()`
  - Would need to port/rewrite `WebAudioAPIManager` (not in the main backend dir)

### 10. GwtAudio.java

- **Implements**: `Audio`
- **Browser APIs**: None
- **Key functionality**: Marker interface extending `Audio`
- **Scala.js mapping**: Trivial trait

### 11. GwtNet.java

- **Implements**: `Net` (LibGDX)
- **Browser APIs**: `XMLHttpRequest` (via GWT's `RequestBuilder`), `window.open`, `window.location.assign`
- **Key functionality**:
  - HTTP requests (GET, POST, PUT, DELETE, HEAD) via `RequestBuilder`
  - Response handling with headers and status codes
  - Request cancellation
  - `openURI` via `window.open` or `window.location.assign`
  - No server sockets or client sockets (unsupported in browser)
  - `getResult()` and `getResultAsStream()` throw -- only `getResultAsString()` works
- **Scala.js mapping**:
  - `XMLHttpRequest` -> `dom.XMLHttpRequest` or `dom.fetch`
  - `window.open` -> `dom.window.open`
  - Modern approach: use Fetch API instead of XHR
  - WebSocket support could be added (LibGDX GWT backend doesn't have it)

### 12. GwtFiles.java

- **Implements**: `Files` (LibGDX)
- **Browser APIs**: `Storage.getLocalStorageIfSupported()` (for LocalStorage)
- **Key functionality**:
  - Only `Internal` and `Classpath` file types supported
  - External, absolute, and local file access throws exceptions
  - Uses `Preloader` for file content (assets pre-fetched at startup)
  - No external/local storage paths
- **Scala.js mapping**:
  - Asset loading: fetch via HTTP (`dom.fetch` or `XMLHttpRequest`)
  - LocalStorage -> `dom.window.localStorage`
  - Same restrictions: no filesystem access in browser
  - Could explore File System Access API for advanced use cases

### 13. GwtFileHandle.java

- **Implements**: extends `FileHandle`
- **Browser APIs**: None directly (delegates to `Preloader`)
- **Key functionality**:
  - Read-only file handle backed by preloaded assets
  - `readString()` uses preloaded text if available, otherwise reads bytes
  - `readBytes()` reads from preloader InputStream
  - All write operations throw exceptions
  - Directory listing via preloader
  - `getAssetUrl()` returns full URL to the asset
- **Scala.js mapping**: Similar architecture needed. Assets fetched at startup or on-demand via HTTP.

### 14. GwtPreferences.java

- **Implements**: `Preferences` (LibGDX)
- **Browser APIs**: `localStorage` (via GWT `Storage`)
- **Key functionality**:
  - Key-value storage using browser localStorage
  - Type-encoded keys (suffix: b=boolean, i=int, l=long, f=float, s=string)
  - Namespace prefix per preferences instance
  - `flush()` writes all values to localStorage
- **Scala.js mapping**:
  - `dom.window.localStorage.setItem/getItem/removeItem`
  - Straightforward port, localStorage is well-supported

### 15. GwtClipboard.java

- **Implements**: `Clipboard` (LibGDX)
- **Browser APIs**: `navigator.clipboard.writeText`, Permissions API for `clipboard-write`
- **Key functionality**:
  - Write to system clipboard via Clipboard API
  - Permission checking before write
  - Read only returns locally-set content (no system clipboard read)
- **Scala.js mapping**:
  - `dom.window.navigator.asInstanceOf[js.Dynamic].clipboard.writeText()`
  - Permissions API via `js.Dynamic`

### 16. GwtCursor.java

- **Implements**: `Cursor` (LibGDX)
- **Browser APIs**: Canvas `toDataUrl`, CSS `cursor` property
- **Key functionality**:
  - Custom cursor from Pixmap via CSS `url()` with data URI
  - System cursor name mapping (Arrow->default, Hand->pointer, etc.)
  - Hotspot coordinates in CSS cursor
- **Scala.js mapping**: Direct CSS manipulation via `dom.Element.style.cursor`

### 17. GwtApplicationLogger.java

- **Implements**: `ApplicationLogger`
- **Browser APIs**: `console.log`, `console.error`
- **Key functionality**: Logging to browser console or GWT TextArea widget
- **Scala.js mapping**: `println` in Scala.js goes to console. Can also use `dom.console.log/error` directly.

### 18. GwtAccelerometer.java

- **Implements**: extends `GwtSensor`
- **Browser APIs**: `Accelerometer` (Generic Sensor API)
- **Key functionality**: Reads x/y/z acceleration values
- **Scala.js mapping**: `js.Dynamic` facade for `new Accelerometer()`, read `.x/.y/.z`

### 19. GwtGyroscope.java

- **Implements**: extends `GwtSensor`
- **Browser APIs**: `Gyroscope` (Generic Sensor API)
- **Key functionality**: Reads x/y/z angular velocity values
- **Scala.js mapping**: `js.Dynamic` facade for `new Gyroscope()`, read `.x/.y/.z`

### 20. GwtSensor.java

- **Implements**: extends `JavaScriptObject` (GWT overlay type)
- **Browser APIs**: `Sensor` interface (start/stop/activated/hasReading/timestamp)
- **Key functionality**: Base class for sensor types with start/stop lifecycle
- **Scala.js mapping**: `js.Dynamic` or a typed facade trait

### 21. GwtFeaturePolicy.java

- **Implements**: None (static utility)
- **Browser APIs**: `document.featurePolicy` (Feature Policy / Permissions Policy API)
- **Key functionality**: Check if browser features are allowed by policy (fullscreen, accelerometer, etc.)
- **Scala.js mapping**: `js.Dynamic` check on `dom.document`

### 22. GwtPermissions.java

- **Implements**: None (static utility)
- **Browser APIs**: `navigator.permissions.query`
- **Key functionality**: Query permission status (granted/denied/prompt) for browser APIs
- **Scala.js mapping**: `dom.window.navigator.asInstanceOf[js.Dynamic].permissions.query(...)`

### 23. GwtUtils.java

- **Implements**: None (utility)
- **Browser APIs**: None
- **Key functionality**: Converts GWT `JsArrayString` to `String[]`
- **Scala.js mapping**: `js.Array[String].toArray` -- trivial in Scala.js

### 24. GwtGL30Debug.java

- **Implements**: extends `GwtGL30`
- **Browser APIs**: Same as GwtGL30
- **Key functionality**: Debug wrapper for GL30 (same pattern as GwtGL20Debug)
- **Scala.js mapping**: Decorator pattern, same as GwtGL20Debug

---

## Part 2: EMU Directory Analysis

The `emu/` directory contains GWT "super-source" replacements -- classes that replace
core LibGDX or Java stdlib classes because GWT's Java-to-JS compiler cannot handle
the originals. For each, we assess whether SGE/Scala.js needs special handling.

### 2.1 Core LibGDX Class Overrides

#### emu/com/badlogic/gdx/files/FileHandle.java

- **What it does**: Strips down `FileHandle` to a skeleton (no filesystem operations)
- **SGE status**: **Not needed.** SGE's `FileHandle` already compiles cross-platform. The JS backend will use a platform-specific `Files` implementation that returns appropriate `FileHandle` instances.

#### emu/com/badlogic/gdx/files/FileHandleStream.java

- **What it does**: Stub for `FileHandleStream`
- **SGE status**: **Not needed.** Same reasoning as FileHandle.

#### emu/com/badlogic/gdx/graphics/Pixmap.java

- **What it does**: **Complete rewrite** using HTML5 Canvas2D API instead of native gdx2d. Uses `Canvas`, `Context2d`, `CanvasPixelArray`, `ImageElement`, `VideoElement`. Pixmap operations (drawLine, drawRect, drawCircle, fillTriangle, getPixel, drawPixel) are all implemented via Canvas2D. Texture upload uses `HTMLImageElement`/`HTMLVideoElement`/`HTMLCanvasElement` directly.
- **SGE status**: **Needs JS-specific implementation.** SGE's core `Pixmap` uses native pixel operations. For Scala.js, a Canvas2D-backed implementation is required (same approach as GWT). This is one of the most important platform-specific files.

#### emu/com/badlogic/gdx/graphics/TextureData.java

- **What it does**: Modified `TextureData` interface for GWT (simplified)
- **SGE status**: **Likely not needed.** If SGE's `TextureData` trait avoids JVM-specific code, it should compile on JS.

#### emu/com/badlogic/gdx/graphics/glutils/FileTextureData.java

- **What it does**: GWT-specific texture data loading using Pixmap's Canvas-based approach
- **SGE status**: **May need JS-specific variant** if it references native texture upload paths.

#### emu/com/badlogic/gdx/graphics/glutils/ETC1TextureData.java

- **What it does**: Stub that throws -- ETC1 not supported in browser
- **SGE status**: **Not needed.** SGE's `ETC1OpsJs` already provides a pure-Scala fallback.

#### emu/com/badlogic/gdx/graphics/glutils/IndexArray.java

- **What it does**: GWT replacement using typed arrays instead of native buffers for index data
- **SGE status**: **May need JS-specific variant.** Buffer handling differs on JS.

#### emu/com/badlogic/gdx/graphics/glutils/IndexBufferObject.java

- **What it does**: Uses typed arrays for index buffer data on WebGL
- **SGE status**: **May need JS-specific variant.**

#### emu/com/badlogic/gdx/graphics/glutils/VertexArray.java

- **What it does**: GWT replacement using typed arrays
- **SGE status**: **May need JS-specific variant.**

#### emu/com/badlogic/gdx/graphics/glutils/VertexBufferObject.java

- **What it does**: GWT replacement using typed arrays
- **SGE status**: **May need JS-specific variant.**

#### emu/com/badlogic/gdx/graphics/glutils/VertexBufferObjectWithVAO.java

- **What it does**: GWT replacement for VAO-based VBO
- **SGE status**: **May need JS-specific variant.**

#### emu/com/badlogic/gdx/graphics/glutils/InstanceBufferObject.java

- **What it does**: GWT replacement for instance buffer
- **SGE status**: **May need JS-specific variant.**

#### emu/com/badlogic/gdx/graphics/profiling/GLErrorListener.java

- **What it does**: Replaces System.err usage with GWT-compatible logging
- **SGE status**: **Not needed.** Scala.js handles `System.err.println` via its javalib.

#### emu/com/badlogic/gdx/math/Matrix4.java

- **What it does**: Pure-Java Matrix4 (removes JNI native matrix multiplication)
- **SGE status**: **Not needed.** SGE's Matrix4 is already pure Scala -- no native calls in core.

#### emu/com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java

- **What it does**: Replaces OS detection for keyboard shortcuts (Ctrl vs Cmd on Mac)
- **SGE status**: **Not needed if** SGE's UIUtils uses `navigator.userAgent` checks on JS. Could use browser detection instead.

#### emu/com/badlogic/gdx/assets/AssetLoadingTask.java

- **What it does**: GWT-specific async asset loading (no threading)
- **SGE status**: **Needs investigation.** Asset loading on JS must be single-threaded/async.

#### emu/com/badlogic/gdx/assets/loaders/TextureLoader.java

- **What it does**: GWT-specific texture loading using async image fetch
- **SGE status**: **Needs JS-specific variant** for async texture loading.

#### emu/com/badlogic/gdx/assets/loaders/CubemapLoader.java

- **What it does**: GWT-specific cubemap loading
- **SGE status**: **Needs JS-specific variant.**

#### emu/com/badlogic/gdx/assets/loaders/resolvers/ResolutionFileResolver.java

- **What it does**: Simplified resolution resolver for GWT
- **SGE status**: **Probably not needed.**

### 2.2 Utility Class Overrides

#### emu/com/badlogic/gdx/utils/BufferUtils.java

- **What it does**: Pure-Java buffer operations. No JNI `newDisposableByteBuffer` or `freeMemory`. Buffer allocation via `ByteBuffer.allocateDirect()` (GWT's emulated version). `transform()` and `findFloats()` reimplemented in pure Java.
- **SGE status**: **Already handled.** SGE has `BufferOpsJs` in `sge.platform` that provides JS-specific buffer operations. The `PlatformOps.buffer` abstraction handles this.

#### emu/com/badlogic/gdx/utils/NumberUtils.java

- **What it does**: `floatToIntBits`/`intBitsToFloat` via GWT's `Numbers` compatibility class (uses `DataView` under the hood)
- **SGE status**: **Not needed.** Scala.js provides `java.lang.Float.floatToIntBits` and `intBitsToFloat` in its javalib.

#### emu/com/badlogic/gdx/utils/TimeUtils.java

- **What it does**: `nanoTime()` returns `currentTimeMillis() * 1000000` (no real nanosecond precision in browser). Could use `performance.now()` for better precision.
- **SGE status**: **Not needed.** Scala.js provides `System.nanoTime()` in its javalib (uses `performance.now()` under the hood).

#### emu/com/badlogic/gdx/utils/Timer.java

- **What it does**: Replaces thread-based `Timer` with GWT `com.google.gwt.user.client.Timer` (uses `setTimeout`). Single-threaded -- no thread sleep/wait.
- **SGE status**: **Needs investigation.** SGE's Timer must work single-threaded on JS. If it uses `Thread.sleep` or `Object.wait`, it needs a JS-specific implementation using `setTimeout`/`setInterval`.

#### emu/com/badlogic/gdx/utils/SerializationException.java

- **What it does**: Removes `initCause` chain that GWT doesn't support
- **SGE status**: **Not needed.** Scala.js supports exception chaining.

#### emu/com/badlogic/gdx/utils/TextFormatter.java

- **What it does**: Simplified text formatting (GWT doesn't support `java.util.Locale` or `String.format`)
- **SGE status**: **Not needed.** Scala.js has good `String.format` support.

#### emu/com/badlogic/gdx/utils/Utf8Decoder.java

- **What it does**: UTF-8 decoding without `java.nio.charset.CharsetDecoder`
- **SGE status**: **Not needed.** Scala.js supports `new String(bytes, "UTF-8")`.

### 2.3 Reflection Emulation (emu/com/badlogic/gdx/utils/reflect/)

#### ClassReflection.java, Field.java, Method.java, Constructor.java, Annotation.java, ArrayReflection.java

- **What they do**: Complete reflection replacement using GWT's `ReflectionCache` (compile-time generated reflection metadata). Supports `forName`, `newInstance`, `getFields`, `getMethods`, `getConstructors`, annotations, array creation.
- **SGE status**: **Critical issue.** Scala.js has **no runtime reflection**. SGE already knows this is a problem (noted for `Skin.load()` which uses reflection for style field setting). Options:
  1. Compile-time macros/derivation (preferred for SGE)
  2. Manual registration tables
  3. `scala-js-reflect` library (limited)
  - The GWT approach of compile-time reflection generation is analogous to what SGE would need with Scala 3 macros.

### 2.4 Async Emulation (emu/com/badlogic/gdx/utils/async/)

#### AsyncExecutor.java

- **What it does**: `submit()` calls `task.call()` **synchronously** (no threading in browser). No thread pool, no concurrency.
- **SGE status**: **Needs JS-specific implementation.** SGE's AsyncExecutor must be synchronous on JS or use Web Workers.

#### AsyncResult.java

- **What it does**: Wraps a pre-computed result (since execution was synchronous). `isDone()` always returns `true`.
- **SGE status**: **Needs JS-specific variant** matching synchronous AsyncExecutor.

#### AsyncTask.java

- **What it does**: Same interface as core (unchanged)
- **SGE status**: **Not needed.** Interface is platform-independent.

#### ThreadUtils.java

- **What it does**: `yield()` is a no-op (no threads in browser)
- **SGE status**: **Not needed.** Scala.js `Thread.yield()` is already a no-op.

### 2.5 GWT Compatibility Layer (emu/com/google/gwt/corp/compatibility/)

#### Compatibility.java, CompatibilityImpl.java

- **What they do**: Compatibility shim for GWT runtime features
- **SGE status**: **Not needed.** Scala.js has its own runtime.

#### ConsolePrintStream.java

- **What it does**: Redirects `System.out/err` to `console.log/error`
- **SGE status**: **Not needed.** Scala.js automatically redirects `System.out/err` to console.

#### Endianness.java

- **What it does**: Detects native byte order using `DataView`
- **SGE status**: **Not needed.** Scala.js `ByteOrder.nativeOrder()` works.

#### Numbers.java

- **What it does**: `floatToIntBits`/`intBitsToFloat`/`doubleToLongBits`/`longBitsToDouble` using `DataView`
- **SGE status**: **Not needed.** Scala.js javalib provides these.

#### StringToByteBuffer.java

- **What it does**: String-to-ByteBuffer encoding
- **SGE status**: **Not needed.** Standard Java APIs work in Scala.js.

### 2.6 Java I/O Emulation (emu/java/io/)

GWT needed to emulate most of `java.io` because GWT's JRE emulation was incomplete.

| File | SGE/Scala.js Status |
|------|-------------------|
| `BufferedInputStream.java` | **Not needed.** Scala.js javalib provides this. |
| `BufferedReader.java` | **Not needed.** In Scala.js javalib. |
| `BufferedWriter.java` | **Not needed.** In Scala.js javalib. |
| `DataInput.java` | **Not needed.** In Scala.js javalib. |
| `DataInputStream.java` | **Not needed.** In Scala.js javalib. |
| `DataOutput.java` | **Not needed.** In Scala.js javalib. |
| `DataOutputStream.java` | **Not needed.** In Scala.js javalib. |
| `EOFException.java` | **Not needed.** In Scala.js javalib. |
| `File.java` | **Not needed.** Scala.js has `java.io.File` stub. |
| `FileFilter.java` | **Not needed.** In Scala.js javalib. |
| `FileNotFoundException.java` | **Not needed.** In Scala.js javalib. |
| `FileWriter.java` | **Not needed.** In Scala.js javalib (limited). |
| `FilenameFilter.java` | **Not needed.** In Scala.js javalib. |
| `InputStreamReader.java` | **Not needed.** In Scala.js javalib. |
| `OutputStreamWriter.java` | **Not needed.** In Scala.js javalib. |
| `RandomAccessFile.java` | **Not needed** (unsupported in browser anyway). |
| `Reader.java` | **Not needed.** In Scala.js javalib. |
| `StringReader.java` | **Not needed.** In Scala.js javalib. |
| `StringWriter.java` | **Not needed.** In Scala.js javalib. |
| `UTFDataFormatException.java` | **Not needed.** In Scala.js javalib. |
| `Writer.java` | **Not needed.** In Scala.js javalib. |

**Summary**: None of the `java.io` emulations are needed. Scala.js javalib is far more complete than GWT's JRE emulation.

### 2.7 Java Lang Emulation (emu/java/lang/)

| File | SGE/Scala.js Status |
|------|-------------------|
| `ClassNotFoundException.java` | **Not needed.** In Scala.js javalib. |
| `IllegalAccessException.java` | **Not needed.** In Scala.js javalib. |
| `InterruptedException.java` | **Not needed.** In Scala.js javalib. |
| `NoSuchFieldException.java` | **Not needed.** In Scala.js javalib. |
| `NoSuchMethodException.java` | **Not needed.** In Scala.js javalib. |
| `Readable.java` | **Not needed.** In Scala.js javalib. |
| `SecurityException.java` | **Not needed.** In Scala.js javalib. |
| `Thread.java` | **Not needed.** Scala.js has `Thread` stub (single-threaded). |

**Summary**: None needed. Scala.js javalib covers all of these.

### 2.8 Java NIO Emulation (emu/java/nio/)

GWT needed a complete NIO buffer implementation backed by JavaScript TypedArrays.

| File | SGE/Scala.js Status |
|------|-------------------|
| `Buffer.java` | **Not needed.** Scala.js has `java.nio.Buffer`. |
| `ByteBuffer.java` | **Not needed.** Scala.js has TypedArray-backed `ByteBuffer`. |
| `FloatBuffer.java` | **Not needed.** In Scala.js javalib. |
| `IntBuffer.java` | **Not needed.** In Scala.js javalib. |
| `ShortBuffer.java` | **Not needed.** In Scala.js javalib. |
| `LongBuffer.java` | **Not needed.** In Scala.js javalib. |
| `DoubleBuffer.java` | **Not needed.** In Scala.js javalib. |
| `CharBuffer.java` | **Not needed.** In Scala.js javalib. |
| `ByteOrder.java` | **Not needed.** In Scala.js javalib. |
| `BufferFactory.java` | **Not needed.** Internal to GWT NIO. |
| `BufferOverflowException.java` | **Not needed.** In Scala.js javalib. |
| `BufferUnderflowException.java` | **Not needed.** In Scala.js javalib. |
| `DirectByteBuffer.java` | **Not needed.** Scala.js has its own impl. |
| `DirectReadWriteByteBuffer.java` | **Not needed.** |
| `DirectReadOnlyByteBuffer.java` | **Not needed.** |
| `DirectReadWrite*Adapter.java` | **Not needed.** |
| `DirectReadOnly*Adapter.java` | **Not needed.** |
| `BaseByteBuffer.java` | **Not needed.** |
| `ByteBufferWrapper.java` | **Not needed.** |
| Various adapter classes | **Not needed.** |
| `HasArrayBufferView.java` | **Interesting.** Interface for getting the underlying `ArrayBufferView` from a buffer. Scala.js `TypedArrayBuffer` has similar capability via `TypedArrayBuffer.wrap()` / `buffer.typedArray()`. |

**Summary**: None of the `java.nio` emulations are needed. Scala.js provides complete TypedArray-backed NIO buffer implementations. The key interface `HasArrayBufferView` (used by GwtGL20 to get TypedArray views) has a Scala.js equivalent via `scala.scalajs.js.typedarray.TypedArrayBuffer`.

### 2.9 Java Net Emulation (emu/java/net/)

| File | SGE/Scala.js Status |
|------|-------------------|
| `URLEncoder.java` | **Not needed.** Scala.js has `java.net.URLEncoder`. |

### 2.10 Other Emulations

#### emu/avian/Utf8.java

- **What it does**: UTF-8 encoding/decoding for Avian VM compatibility
- **SGE status**: **Not needed.**

---

## Part 3: Summary and Recommendations

### What SGE Gets "For Free" from Scala.js (vs GWT)

Scala.js has a dramatically more complete Java standard library emulation than GWT.
The entire `emu/` directory (78 files) can be categorized:

| Category | Files | Needed for SGE? |
|----------|-------|----------------|
| java.io emulations | 21 | **None** -- Scala.js javalib |
| java.lang emulations | 8 | **None** -- Scala.js javalib |
| java.nio emulations | ~25 | **None** -- Scala.js javalib |
| java.net emulations | 1 | **None** -- Scala.js javalib |
| GWT compatibility layer | 6 | **None** -- not applicable |
| Utils (NumberUtils, TimeUtils, etc.) | 6 | **None** -- Scala.js javalib |
| Reflection emulation | 6 | **Needs alternative** (macros/derivation) |
| Async emulation | 4 | **Needs JS-specific impl** |
| Graphics (Pixmap, VBO, etc.) | 10 | **Needs JS-specific impls** |
| Asset loading | 4 | **Needs JS-specific impls** |
| Timer | 1 | **Needs investigation** |

### Priority Items for SGE Scala.js Backend

**Must have (P0):**

1. **GL20/GL30 implementation** -- Port GwtGL20/GwtGL30 pattern to Scala.js with scalajs-dom WebGL facades. This is the heart of the rendering backend.
2. **Pixmap (Canvas2D-backed)** -- JS-specific Pixmap using `HTMLCanvasElement` and `CanvasRenderingContext2D`.
3. **Application loop** -- `requestAnimationFrame`-based game loop.
4. **Input handling** -- DOM event listeners for mouse, keyboard, touch.
5. **Buffer-to-TypedArray bridge** -- Convert NIO buffers to TypedArray views for WebGL calls. SGE already has `BufferOpsJs` but may need extensions for GL parameter passing.

**Should have (P1):**

6. **Audio (Web Audio API)** -- Sound and Music via Web Audio.
7. **Files/FileHandle** -- Asset loading via HTTP fetch, preloader architecture.
8. **Net** -- HTTP requests via Fetch API.
9. **Preferences** -- localStorage wrapper.
10. **Async** -- Synchronous fallback for AsyncExecutor.

**Nice to have (P2):**

11. **Fullscreen API** -- Fullscreen mode toggling.
12. **Pointer Lock** -- Cursor capture for FPS-style games.
13. **Clipboard** -- Navigator Clipboard API.
14. **Sensors** -- Accelerometer, Gyroscope via Generic Sensor API.
15. **Permissions/Feature Policy** -- API access guards.

### Key Architecture Differences: GWT vs Scala.js

| Aspect | GWT Backend | SGE Scala.js Target |
|--------|-------------|-------------------|
| Compilation | Java -> JS via GWT compiler | Scala -> JS via Scala.js compiler |
| JS interop | JSNI (`native` methods with JS) | `@js.native`, `js.Dynamic`, facades |
| Java stdlib | Heavy emulation (emu/ dir) | Scala.js javalib (mostly complete) |
| NIO buffers | Custom TypedArray-backed impls | Scala.js javalib TypedArray-backed |
| Reflection | Compile-time `ReflectionCache` | Not available; use macros/derivation |
| Threading | Single-threaded, `synchronized` is no-op | Single-threaded, same |
| DOM access | GWT widget library | scalajs-dom |
| Typed arrays | GWT TypedArray wrappers | `scala.scalajs.js.typedarray` |
| Entry point | `EntryPoint.onModuleLoad()` | `def main()` or `@JSExportTopLevel` |
| Asset loading | Custom Preloader (build-time manifest) | HTTP fetch (runtime) |

### GL Handle Mapping Pattern

The most important architectural pattern from the GWT backend is the **integer handle to WebGL object mapping** in GwtGL20. WebGL returns JavaScript objects (`WebGLTexture`, `WebGLBuffer`, etc.) but OpenGL ES expects integer handles. GwtGL20 uses a JS array as an int-keyed map:

```
Programs:  int -> WebGLProgram
Shaders:   int -> WebGLShader
Buffers:   int -> WebGLBuffer
Textures:  int -> WebGLTexture
FBOs:      int -> WebGLFramebuffer
RBOs:      int -> WebGLRenderbuffer
Uniforms:  int -> IntMap[WebGLUniformLocation]
```

SGE's Scala.js GL implementation must replicate this pattern, likely using `js.Array` or a Scala `mutable.HashMap[Int, js.Any]`.
