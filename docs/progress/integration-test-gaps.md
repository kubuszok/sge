# Integration Test Gaps

Tracks untested areas across all platforms. Check off items as they're addressed.

**Last updated**: 2026-03-10

---

## 1. Gdx2DPixmap — RESOLVED

All drawing primitives implemented as pure Scala in `Gdx2dDraw.scala` (657 lines).
Image decoding works on JVM (javax.imageio) and Native (Rust `image` crate).
JS is a deliberate stub (browser image decoding is async, incompatible with sync API).

- [x] Pixel buffer allocation (`Gdx2dDraw.newPixelBuffer`)
- [x] Clear, setPixel, getPixel
- [x] drawLine, drawRect, drawCircle, fillRect, fillCircle, fillTriangle
- [x] drawPixmap (blit with optional scaling — nearest/bilinear)
- [x] Blend mode (SRC_OVER alpha blending)
- [x] Scale mode (nearest/bilinear)
- [x] Image decode on JVM (`Gdx2dOpsJvm` via ImageIO)
- [x] Image decode on Native (`Gdx2dOpsNative` via Rust `image` crate C ABI)
- [x] 20 unit tests passing (Gdx2DPixmapTest)

---

## 2. Desktop IT — 20 checks

Harness runs 20 checks in subprocess (GLFW + ANGLE + miniaudio):

- [x] Bootstrap: Sge context + GL20 available
- [x] FileIO: write/read temp file roundtrip
- [x] JSON/XML: jsoniter-scala + scala-xml parsing
- [x] GL2D: shader compile + mesh draw
- [x] GL3D: shader with uniform matrix + depth
- [x] Audio: miniaudio engine + sound load/play
- [x] Input: state queries (position, pointers, key press)
- [x] Pixmap: create, set pixel, read back color
- [x] Texture: upload Pixmap to GL texture, verify handle
- [x] SpriteBatch: begin/end cycle
- [x] FBO: FrameBuffer render red + glReadPixels readback
- [x] Clipboard: write/read roundtrip
- [x] Window: dimensions, GL version, buffer format, monitor
- [x] Music: streaming load, play, position query, stop
- [x] MultiSound: 3 concurrent sound instances with different pan/pitch
- [x] TextureAtlas: load .atlas fixture, findRegion by name
- [x] WindowResize: setWindowedMode + dimension readback
- [x] Cursor: system cursor switching + custom cursor from Pixmap
- [x] InputDispatch: InputProcessor set/get roundtrip
- [x] Fullscreen: fullscreen API exercise (isFullscreen, displayMode, monitors, toggle)

### Remaining gaps
- [ ] Window iconify/restore callbacks

---

## 3. Browser IT — 13 tests

Playwright headless Chromium tests:

- [x] Demo JS loads without fatal errors
- [x] WebGL context available (WebGL2/WebGL1)
- [x] WebGL shader compilation (vertex + fragment + link)
- [x] JSON and XML parsing in JS runtime
- [x] FileIO: fetch bundled text asset from server
- [x] Canvas has non-zero pixels after rendering
- [x] Web Audio API context available
- [x] localStorage read/write/remove roundtrip
- [x] Mouse click event dispatch to canvas
- [x] BrowserPreferences: localStorage protocol roundtrip (typed keys)
- [x] Keyboard input event dispatch to canvas
- [x] Touch event: synthetic TouchEvent dispatch to canvas
- [x] HTTP fetch: roundtrip via embedded server (api-test.json)

### Remaining gaps
(none feasible in headless Playwright without full Scala.js app wiring)

---

## 4. Android IT — 16 checks

SmokeActivity creates full Sge context via AndroidApplication, wires SmokeListener.
Results logged via both scribe + System.out (logcat) and Log.i (SGE-SMOKE tag).
App uses time-based milestones: initial checks at frame 5, post-adb checks at 6s, exit at 10s.

- [x] Bootstrap: Sge context + GL20
- [x] GL2D: clear calls
- [x] GL3D: shader + uniform matrix
- [x] FileIO: write/read temp file
- [x] JSON/XML: class loading + XML parse
- [x] Audio: subsystem accessible
- [x] Input: state queries (position, pointers, key press)
- [x] Preferences: SharedPreferences write/read/remove
- [x] Clipboard: set/get contents
- [x] Display: dimensions, PPI, density
- [x] FileHandle: external storage write/read roundtrip
- [x] Touch setup: InputProcessor installed for tracking
- [x] Touch dispatch: adb input tap → InputProcessor.touchDown received
- [x] Lifecycle: pause/resume via HOME key + re-launch
- [x] Sensors: accelerometer availability + gravity reading
- [x] Sensor injection: emulator console telnet → accelerometer value change

CI: `test-android` job in `.github/workflows/ci.yml` (reactivecircus/android-emulator-runner).
Test runner injects adb events mid-run: tap at 3s, sensor injection at 3s, HOME at 5s, re-launch at 7s.
Emulator console (telnet localhost:5554) used for sensor value injection with auth token support.

---

## 5. JVM Platform IT Gaps — RESOLVED

All 41 tests pass. Buffer ops and ETC1 roundtrip covered.

- [x] API shape tests (31 tests across 7 suites)
- [x] Panama FFM symbol availability
- [x] `sge_copy_bytes` data correctness
- [x] `sge_transform_v4m4` with identity and translation matrices
- [x] ETC1 encode/decode block roundtrip

---

## 6. Cross-Platform Compilation

- [x] `sgeJS/compile` — Scala.js compilation
- [x] `sgeNative/compile` — Scala Native compilation
- [x] `just test` — JVM unit tests (1450 pass)
- [ ] `just test-js` — JS unit tests
- [ ] `just test-native` — Native unit tests
- [ ] `just it-all` — run all IT tests in sequence

---

## Remaining Priorities

1. **Window iconify/restore** — desktop window state callbacks (needs GLFW callback wiring)
