# SGE Codebase Review — 2026-06-09

Parallel agent review of the SGE (Scala Game Engine) codebase. Eight domain-scoped agents read ~350–400 unique source files (not a line-by-line audit of every file). Findings below are synthesized from their reports.

## Methodology

| Agent domain | Scope | Files read / analyzed |
|--------------|-------|----------------------|
| Core graphics / g3d | `sge/graphics/**` + platform GL | 48 direct reads |
| Scenes & input | scene2d, input, application lifecycle | 52 direct reads |
| Files, assets, maps, net | I/O, loaders, HTTP, utils, math | 44 direct reads |
| Platform backends | JVM/JS/Native/Android/desktop/noop | 52 direct reads |
| Extensions (non-gltf/textra/visui) | ai, physics, controllers, vfx, etc. | 52 reads + header scan of 458 files |
| gltf / textra / visui | Largest extension debt areas | 62 direct reads |
| Tests, CI, demos, build | sge-test, demos, CI, docs | 38 direct reads |
| LibGDX fidelity | 28 high-traffic classes vs Java | 28 classes, `re-scale enforce compare --strict` |

**Ceiling:** Agents sampled within domains. Not covered exhaustively: ~85 remaining scene2d files, full `AngleGL20`–`32` (~3000+ LOC), all particle influencers/emitters, external `sge-native-providers` Rust FFI, runtime test execution.

Automated scans also run: covenant verification (136 files with shortcut drift), audit stats (1,307 pass / 4 minor_issues), enforcement shortcuts.

---

## Executive Summary

SGE’s **core rendering and scene2d logic are in strong shape** — most high-traffic LibGDX classes rate complete, and algorithm extensions (ai, ecs, graphs, noise, jbump, anim8, vfx) are mature.

The biggest problems cluster in:

1. **Platform wiring** — controller init never called, Android keyboard provider unwired
2. **Browser/JS paths** — WebGL texture param bug, touchcancel routing, pixmap preload-only, physics/freetype stubs
3. **Extension packaging** — VisUI skin/i18n missing, gltf/textra runtime stubs
4. **CI blind spots** — covenant enforce non-blocking, desktop GPU harness never runs headless CI, native validation skipped on publish

Covenant/audit status **overstates completeness** in several places (stubs marked pass, stale issue DB entries, headers claiming partial-port while implementation exists or vice versa).

---

## P0 — Fix First (Confirmed Bugs / Broken Out-of-the-Box)

### 1. WebGL texture parameters call wrong API (Scala.js)

`WebGL20.glTexParameteri` / `glTexParameteriv` delegate to `texParameterf` instead of `texParameteri`/`texParameteriv`. Affects every `Texture.setWrap`, `setFilter`, and cubemap/3D wrap path on browser.

**File:** `sge/src/main/scalajs/sge/graphics/WebGL20.scala` (lines ~779–783)

### 2. Browser `touchcancel` dispatches `touchUp`, not `touchCancelled`

OS-level touch cancellation is indistinguishable from normal release. `Stage.touchCancelled`, `GestureDetector.cancel()`, and cancel-aware listeners never run on WebGL. Android correctly routes cancel.

**File:** `sge/src/main/scalajs/sge/input/DefaultBrowserInput.scala` (lines ~291–292, ~481–495)

### 3. HTTP binary responses corrupted (SGE-specific regression)

`SgeHttpClient` uses `SttpRequest[Either[String, String]]`; bodies round-trip through UTF-8 strings. LibGDX platform HTTP backends delivered raw bytes. `resultAsStream` is a `ByteArrayInputStream` over UTF-8 re-encoding, not a live socket stream.

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala`, `SgeHttpResponse.scala`, `Net.scala` (Javadoc mismatch)

### 4. Controller backends never auto-initialized

`GlfwControllerJvmInit.init()`, `GlfwControllerNativeInit.init()`, and `BrowserControllerInit.init()` exist with working implementations but are **never called** from platform startup. Gamepads stay disconnected unless apps wire init manually. Header still says "JVM polling stub."

**Files:** `sge-extension/controllers/src/main/scala/sge/controllers/GlfwControllerBackend.scala`, `scalajvm/.../GlfwControllerJvm.scala`, etc.

### 5. Native desktop MSAA: `EGL_SAMPLES` missing

`GlOpsNative` sets `EGL_SAMPLE_BUFFERS` but not `EGL_SAMPLES`; JVM sets both. Multisampling silently wrong on Scala Native desktop.

**Files:** `sge/src/main/scalanative/sge/platform/GlOpsNative.scala`, `sge/src/main/scalajvm/sge/platform/GlOpsJvm.scala`

### 6. `BlenderShapeKeys.parse` is a no-op stub

Despite audit `pass`, parsing always returns empty even when extras exist. Blender morph target names never reach `NodePlus.morphTargetNames`.

**File:** `sge-extension/gltf/src/main/scala/sge/gltf/loaders/blender/BlenderShapeKeys.scala` (lines ~50–52)

### 7. VisUI runtime assets not packaged

`VisUI.load()` expects `com/kotcrab/vis/ui/skin/x1/uiskin.json` on classpath; `sge-extension/visui/src/main/resources/` is empty. Default skin and i18n fail at runtime unless consumers bundle assets manually.

**Files:** `sge-extension/visui/.../VisUI.scala`, `Locales.scala`

---

## P1 — High Impact (Correctness, Security, Integration)

### Graphics & rendering

| Issue | File(s) | Notes |
|-------|---------|-------|
| JS pixmap decode preload-only; `offset`/`len` ignored | `Gdx2dOpsJs.scala`, `Pixmap.scala` | Direct `Pixmap(file)` fails without `BrowserAssetLoader` cache |
| Anisotropic filter hardcodes `Texture2D` target | `GLTexture.scala` | Cubemap/3D/array targets wrong |
| `ObjLoader` swallows MTL I/O errors | `ObjLoader.scala` | Silent wrong materials |
| `ParallelArray.addElement` throws at capacity | `ParallelArray.scala` | Particle crash; same FIXME as LibGDX |
| `DefaultShader` multi-UV maps all to `texCoord0` | `DefaultShader.scala` | Inherited FIXME |
| Light attribute `compare` is size-only | `DirectionalLightsAttribute.scala` (+ Point/Spot) | Dedup/sort treats different light sets as equal |

### Input & scene2d

| Issue | File(s) | Notes |
|-------|---------|-------|
| `Stage.touchCancelled` leaves `pointerTouched[pointer] == true` | `Stage.scala` | Inherited from LibGDX; wrong for Android cancel |
| `inputProcessor` typed non-null; implementations return null | `Input.scala`, `DefaultDesktopInput.scala`, `DefaultBrowserInput.scala` | NPE risk; processor uninitialized on browser until set |
| Browser `isButtonPressed` requires `touchedArr(0)` | `DefaultBrowserInput.scala` | Differs from desktop GLFW polling |
| `InputEventQueue` has no `touchCancelled` event type | `InputEventQueue.scala` | Desktop/browser cannot queue cancel even if fixed |

### Files, assets, maps, net

| Issue | File(s) | Notes |
|-------|---------|-------|
| Map loaders use mutable instance state | `TiledMapLoader.scala`, `BaseTmxMapLoader.scala`, `BaseTmjMapLoader.scala` | Concurrent async loads can cross-contaminate |
| Path traversal via Tiled `..` in `getRelativeFileHandle` | `BaseTiledMapLoader.scala` | LibGDX-compatible; unsafe for untrusted maps |
| Decompression / memory bombs in tile layers | `BaseTmxMapLoader.scala`, `BaseTmjMapLoader.scala` | No output-size cap on gzip/zlib |
| `SgeHttpClient.cancel` does not abort in-flight HTTP | `SgeHttpClient.scala` | Pool reuse races |
| `XmlReader` full-document in-memory parse | `XmlReader.scala` + platform impls | DoS surface; different failure modes vs LibGDX hand parser |
| `Class.forName` in particle codecs (JVM) | `ParticleEffectCodecs.scala` | Untrusted JSON reflection hole; `ResourceData` uses allowlist |

### Platform backends

| Issue | File(s) | Notes |
|-------|---------|-------|
| iOS `ApplicationType` with zero backend | `Application.scala`, core branches | Dead code paths; misleading docs |
| Android keyboard-height provider unwired | `StandardKeyboardHeightProviderImpl.scala`, `AndroidInput.scala` | API exists, never connected |
| `AndroidXKeyboardHeightProviderImpl` always throws | `AndroidXKeyboardHeightProviderImpl.scala` | Misleading "requires AndroidX" message |
| `BufferOps` memory semantics diverge JVM vs Native/JS | `BufferOpsPanama/Native/Js.scala`, `BufferUtils.scala` | `freeMemory` no-op on Native/JS; `getBufferAddress` throws |
| `NativeGlHelper` fragile address probing + `wrapPtr` leak | `NativeGlHelper.scala` | FFI safety risk |
| Android input gaps: `rotation` stub, simplified `isButtonJustPressed` | `AndroidInput.scala` | LibGDX parity gaps |

### Extensions

| Issue | Module | Notes |
|-------|--------|-------|
| Physics 2D/3D throws on Scala.js | physics, physics3d | Compiles, crashes at runtime |
| FreeType throws on Scala.js | freetype | Must pre-bake fonts on JVM/Native |
| Android controllers require manual `AndroidControllerInit.init()` | controllers | Unlike original auto-hook |
| Controller vibration not implemented | `DefaultControllerManager.scala` | Partial-port debt |
| `HierarchicalPathFinder` upstream FIXME preserved | ai | Potential path init bug |
| `GLTFCodecs` encode stubbed (41× `writeNull`) | gltf | Decode works; codec round-trip broken |
| Textra: clipboard stub, LZMA/UBJSON fonts rejected | textra | `LzmaUtils` ported but not wired |
| KHR punctual lights intensity `/10f` hack | gltf | Rendering may be 10× off |

### CI & testing

| Issue | Evidence |
|-------|----------|
| Covenant enforce **non-blocking** in CI | `ci.yml`: `continue-on-error: true` |
| `SGE_SKIP_NATIVE_VALIDATION=true` on publish steps | Packaged JAR native completeness not gated |
| Desktop 23-check harness never runs in CI | Headless skip on `ubuntu-latest` |
| Native GLFW/EGL/GL FFI skipped with `--headless` | `NativeFfiValidation.scala` |
| `regressionTest` never executed as app in CI | Only `fastLinkJS` built for browser |
| Browser smoke uses `fastLinkJS`, not `sgePackageBrowser` | Asset manifest/preload path untested |
| AssetShowcase smoke disabled | `DemoSmokeTest` `.ignore` |

---

## P2 — LibGDX Migrator Friction & Documentation Drift

### API inconsistency

- **Two dialects:** graphics uses Scala properties + `close()` + `(using Sge)`; scene2d retains LibGDX `getX`/`setX`; top-level `Preferences`/`Graphics`/`Input` mixed
- **Lifecycle split:** `Screen.close()` vs `ApplicationListener.dispose()` vs `ParticleController.dispose()`
- **Nullable vs Option vs orNull** at Java/GL boundaries

### LibGDX fidelity gaps (28-class audit)

| Class | Status | Gap |
|-------|--------|-----|
| SpriteBatch, Stage, Actor, Table, Texture, Mesh, ModelBatch, BitmapFont, etc. | Complete | Covenant-verified |
| AssetManager | Partial | No covenant; no per-instance logger |
| FileHandle | Partial | `tempFile`/`tempDirectory` omitted |
| Pixmap | Partial | `downloadFromUrl` missing |
| Input | Partial | `KeyboardHeightObserver` show/hide callbacks removed |
| Application | Partial | `log`/`error`/`debug` removed → `utils.Log` |
| XmlReader | Partial | Library delegation vs hand parser |
| Json | Missing | Replaced by `JsonCodecs` (not drop-in) |

**Summary:** 22 complete · 5 partial · 1 missing

### Stale documentation & issue tracking

- `GlfwControllerBackend` header says JVM stub; `GlfwControllerJvm.scala` is complete
- ISS-425–441 in issues DB appear stale vs current code
- Audit marks `BlenderShapeKeys` pass while stubbed
- Docs: Android API 36 in architecture vs API 35 in CI; `sge-jvm-platform/jdk/` documented but absent; FreeType required in doctor but not setup.md; CLAUDE.md mentions Pong demo APK — CI uses smoke APK only

---

## What Is Genuinely Solid

- **Core 2D/3D rendering pipeline:** SpriteBatch, Mesh, ShaderProgram, ModelBatch, GL framebuffers — covenant coverage ~200 files under `graphics/**`
- **Scene2d event model:** InputMultiplexer reentrancy, capture/bubble, ClickListener/DragListener/GestureDetector faithful to LibGDX
- **Algorithm extensions:** ecs, graphs, noise, jbump, anim8, colorful runtime, vfx effects (21/21), screens transitions — covenanted with tests
- **Platform abstraction:** `PlatformOps`, Panama on JVM, `@extern` on Native, pure Scala on JS — coherent structure
- **Asset pipeline loaders:** Faithful to LibGDX for trusted local assets (TextureAtlas, Skin, TiledMap dispatch)
- **Security positives:** No hardcoded secrets, no TLS bypass; particle `ResourceData` uses class allowlist; browser disables raw TCP sockets
- **ObjLoader:** Scala fixed Java face-loop `i--` bug (improvement over LibGDX)

---

## Module Health Summary

| Area | Verdict |
|------|---------|
| Core LibGDX port | ~complete; gaps in utilities/logging/mobile callbacks |
| Platform JVM desktop | Mature; Panama ANGLE/GLFW |
| Platform Scala.js | Critical WebGL bug; pixmap preload; many throws |
| Platform Scala Native | MSAA bug; buffer ops partial vs JVM |
| Platform Android | Input/keyboard gaps; smoke whitelists 5 failures |
| Platform iOS | Enum only; no backend |
| Extensions (algorithm) | Strong |
| Extensions (platform-facing) | Controllers, physics JS, freetype JS — integration gaps |
| gltf / textra / visui | High file-count parity; runtime stubs and packaging gaps |
| Tests / CI | Layered strategy; high-value paths compile-only or skipped |

---

## Recommended Priorities

### Immediate (P0 fixes)

1. Fix `WebGL20.glTexParameteri`/`iv` → `texParameteri`/`texParameteriv`
2. Route browser `touchcancel` → `processor.touchCancelled(...)`
3. Switch HTTP to `Array[Byte]` response bodies; fix docs and cancellation
4. Wire controller `*Init.init()` from platform startup
5. Add `EGL_SAMPLES` to `GlOpsNative.createContext`
6. Implement `BlenderShapeKeys.parse`
7. Package VisUI skin + i18n into `sge-extension/visui/src/main/resources/`

### Short-term (P1)

8. Map loader per-load state or synchronization for concurrent AssetManager loads
9. Replace `Class.forName` in `ParticleEffectCodecs` with `ResourceData` allowlist
10. Wire Android `StandardKeyboardHeightProviderImpl` into app lifecycle
11. Make covenant enforce blocking in CI
12. Add display-capable desktop IT job (Xvfb or macOS runner)
13. Browser IT: test `sgePackageBrowser` path; un-ignore AssetShowcase smoke

### Medium-term (P2)

14. Reconcile audit TSV and issue DB with stub scans
15. Document or gate iOS references until backend exists
16. Align `Input.KeyboardHeightObserver` API or document breaking change
17. Add `AssetManager` covenant
18. LibGDX migration guide: `close()` vs `dispose()`, two API dialects, controller bootstrap, physics JS limitation
19. Update docs: build-structure, doctor.yaml, Android API level, controller init

---

## Coverage Gaps (Honest)

Not exhaustively reviewed:

- ~85 remaining scene2d files (actions, Tree, Slider, full Skin, Tooltips)
- ~40 g3d/particles files (influencers, emitters, renderers)
- Full `AngleGL20`–`AngleGL32` (~3000+ LOC combined)
- Android GLSurfaceView renderer loop, audio engine internals
- All 21 asset loaders (representative sample only)
- External `sge-native-providers` Rust C ABI bounds checking
- Runtime execution of tests/demos (static review only)

---

## Agent Reports (Internal Reference)

Domain reports produced by parallel review agents during this session. Paths are under the Cursor agent transcripts directory for this conversation.

| Domain | Task ID |
|--------|---------|
| Core graphics / g3d | 721984b7-5177-4c6b-acff-88de643a2e48 |
| Scenes & input | 4194683b-6a57-43cc-90e9-99a2388b20b4 |
| Files, assets, maps, net | f5d7c831-61db-4d5b-8210-c8bb1c5f6bff |
| Platform backends | 6a714fec-ebdb-4aa1-b3ce-80f68133c866 |
| Extensions (other) | 943b84ab-db54-4e80-83ae-2a24a98ce371 |
| gltf / textra / visui | 9c18081e-4bd1-4668-929c-9bf4ad84513b |
| Tests, CI, demos, build | 84a198a3-6bc1-43bf-9dbf-85c1daa0d275 |
| LibGDX fidelity (28 classes) | 718c7a01-2fe1-4d3c-b0cf-07f43cafd0fd |

---

*Generated from parallel agent review, 2026-06-09. Static analysis only — no compile/test/CI execution during review.*
