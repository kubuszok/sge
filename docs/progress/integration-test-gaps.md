# Integration Test Gaps

Tracks untested areas across all platforms. Check off items as they're addressed.

**Last updated**: 2026-03-09

---

## 1. Gdx2DPixmap Native Stubs (BLOCKER)

All 14 native methods in `sge/src/main/scala/sge/graphics/g2d/Gdx2DPixmap.scala` are stubs.
This blocks Texture, SpriteBatch, FBO readback, and the full 2D/3D rendering pipeline.

- [ ] `newPixmap(nativeData, width, height, format)` → allocate pixel buffer
- [ ] `free(basePtr)` → free pixel buffer
- [ ] `load(buffer: Array[Byte])` → decode image (PNG/JPG/BMP) into pixel buffer
- [ ] `loadByteBuffer(buffer: ByteBuffer)` → decode image from ByteBuffer
- [ ] `clear(basePtr, color)` → fill with color
- [ ] `setPixel(basePtr, x, y, color)` → write single pixel
- [ ] `getPixel(basePtr, x, y)` → read single pixel
- [ ] `drawLine(basePtr, x, y, x2, y2, color)`
- [ ] `drawRect(basePtr, x, y, w, h, color)`
- [ ] `drawCircle(basePtr, x, y, radius, color)`
- [ ] `fillRect(basePtr, x, y, w, h, color)`
- [ ] `fillCircle(basePtr, x, y, radius, color)`
- [ ] `fillTriangle(basePtr, x1, y1, x2, y2, x3, y3, color)`
- [ ] `drawPixmap(srcPtr, dstPtr, ...)` → blit one pixmap onto another
- [ ] `setBlend(basePtr, blend)` → set blending mode
- [ ] `setScale(basePtr, scale)` → set scaling mode
- [ ] `getFailureReason()` → returns `"Unknown error"` (minor)

**Implementation plan**: Add Rust functions to `native-components/src/` (C ABI), wire to
JVM via Panama downcall handles in a new `Gdx2DPixmapOpsJvm.scala`, wire to Scala Native
via `@extern`. Image decoding can use the `image` or `stb_image` crate in Rust.

---

## 2. Desktop IT Gaps

Currently passing: bootstrap, fileio, json_xml, gl2d, gl3d, audio (6/6).
All checks avoid Pixmap due to stubs above.

### Rendering pipeline (blocked by Gdx2DPixmap)
- [ ] Pixmap creation + pixel read/write
- [ ] Texture upload from Pixmap
- [ ] SpriteBatch rendering (needs default white texture → Pixmap)
- [ ] FrameBuffer readback via glReadPixels → Pixmap
- [ ] FBO color verification (rendered pixels match expected)
- [ ] Texture atlas / region slicing

### Windowing & lifecycle
- [ ] Window resize callback + viewport update
- [ ] Window iconify/restore callbacks
- [ ] Fullscreen toggle (setFullscreenMode / setWindowedMode)
- [ ] Multiple window support
- [ ] Window close callback + lifecycle

### Input
- [ ] Keyboard event dispatch (key down/up/typed)
- [ ] Mouse event dispatch (move, click, scroll)
- [ ] Cursor management (standard cursors, custom cursors)
- [ ] Input mode changes (raw mouse, hidden cursor)
- [ ] Clipboard read/write

### Audio (deeper)
- [ ] Music streaming (load + play + seek + duration)
- [ ] Multiple simultaneous sounds
- [ ] Audio device enumeration
- [ ] Volume/pan/pitch control verification

---

## 3. Browser IT Gaps

Currently: 3 tests (load without errors, WebGL context available, canvas has pixels).

### Subsystem checks (not yet added)
- [ ] GL2D: shader compilation in WebGL
- [ ] GL3D: shader with uniforms + depth in WebGL
- [ ] FileIO: BrowserFileHandle read from bundled assets
- [ ] JSON/XML: parse operations in JS runtime
- [ ] Input: keyboard/mouse/touch event simulation via Playwright

### Rendering verification
- [ ] Canvas pixel readback with specific expected colors
- [ ] WebGL extension availability checks
- [ ] Multiple render frames without errors

### Platform-specific
- [ ] BrowserAudio (Web Audio API): sound load + play
- [ ] BrowserPreferences (localStorage): read/write/roundtrip
- [ ] BrowserClipboard: read/write
- [ ] BrowserNet: HTTP fetch
- [ ] BrowserFileHandle: asset loading from bundled files

---

## 4. Android IT Gaps

Currently: 1 smoke test (APK launches, logcat markers checked).
Smoke app has 5 checks: bootstrap, gl2d, fileio, json_xml, audio.

### Missing subsystem checks
- [ ] GL3D: shader with uniforms + depth + matrix
- [ ] Audio: real audio playback (currently noop with -noaudio emulator flag)
- [ ] Input: touch event simulation via adb
- [ ] Sensors: accelerometer/gyroscope (mocked or via emulator console)
- [ ] Screen density / safe insets / display metrics

### Structured result parsing
- [ ] Parse per-subsystem PASS/FAIL from logcat (infrastructure exists, checks incomplete)
- [ ] Match desktop harness result count (6 checks vs current 5)
- [ ] Add GL3D check to SmokeListener

### Android-specific APIs
- [ ] FileHandle: internal vs external storage
- [ ] Preferences: SharedPreferences roundtrip
- [ ] Clipboard: Android clipboard manager
- [ ] Activity lifecycle (pause/resume/stop/destroy)

---

## 5. JVM Platform IT Gaps

Currently: 77 tests across 8 suites. Covers API shape + Panama FFM symbol availability.

### Buffer operations (data correctness)
- [ ] `sge_copy_bytes` with real data verification
- [ ] `sge_transform_v4_m4` with known matrix → expected output
- [ ] `sge_transform_v3_m4` / `sge_transform_v3_m3` correctness
- [ ] `sge_transform_v2_m4` / `sge_transform_v2_m3` correctness
- [ ] `sge_find_vertex` / `sge_find_vertex_epsilon` with real vertex data

### ETC1 operations
- [ ] `etc1_encode_block` / `etc1_decode_block` roundtrip
- [ ] `etc1_encode_image` / `etc1_decode_image` roundtrip
- [ ] PKM header format verification

---

## 6. Cross-Platform Compilation

- [ ] `sgeJS/compile` — Scala.js compilation of all shared + JS sources
- [ ] `sgeNative/compile` — Scala Native compilation of all shared + Native sources
- [ ] `just test-js` — JS unit tests
- [ ] `just test-native` — Native unit tests
- [ ] `just it-all` — run all IT tests in sequence (desktop + browser + android)

---

## Priority Order

1. **Gdx2DPixmap native impl** — unblocks Texture/SpriteBatch/FBO for all platforms
2. **Desktop rendering verification** — FBO readback with pixel checks (after Gdx2DPixmap)
3. **Browser subsystem checks** — GL2D, GL3D, FileIO, JSON/XML via Playwright
4. **Android GL3D + audio** — match desktop coverage
5. **Cross-platform compilation** — verify JS + Native still compile
6. **Input/windowing** — keyboard, mouse, resize, fullscreen
7. **Buffer ops data correctness** — real vertex transform verification
8. **Audio deep testing** — music streaming, multiple sounds, device enum
