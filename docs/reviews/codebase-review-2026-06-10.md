# SGE Codebase Review — 2026-06-10

Full-codebase review by 13 parallel domain agents (Claude / Fable 5). Unlike the
2026-06-09 pass, this review covered the areas that pass admitted skipping:
all of scene2d (102 files), the full g3d/particles subtree (143 files), every
GL binding file line-by-line (AngleGL20–32, WebGL20/30, Native externs), all
asset loaders, and every extension module. ~1,400 of 1,826 Scala files were
read directly (deep or skimmed); the remainder were covered by marker scans,
method-set comparison (`re-scale enforce compare`), and the covenant/audit DB.

## Relationship to the 2026-06-09 review

A dedicated verification agent checked every prior P0/P1 against current code:

- **No commits have landed since the review** (HEAD `07ace28d`, 2026-06-08).
  **All 7 prior P0s and all 33 prior P1 rows are STILL-PRESENT.** That report
  remains fully actionable; its findings are not repeated here except where
  this review found them to be deeper than reported.
- **The issues DB does not track either review.** "482/482 resolved" reflects
  pre-review bookkeeping (~2026-03-19). Spot-checks: ISS-445/447 genuinely
  fixed; ISS-429 half-fixed (vibration still stubbed); ISS-428 "resolved" but
  the user-visible defect persists (init never called); ISS-436 "resolved" but
  feeds into a no-op stub (`BlenderShapeKeys.parse`).

## Executive summary

This pass found a tier of breakage **worse** than anything in the prior
review: core 2D text rendering, PNG writing, SpriteCache, 3D particles, glTF
scene rendering, delayed AI messaging, and Delaunay triangulation are
**confirmed broken at the algorithm level** — not platform-wiring gaps, but
logic destroyed in translation (mistranslated `break`/`return`, inverted
guards, dead stores, wrong-instance initialization). Most cluster around four
recurring port-failure patterns:

1. **Mistranslated control flow** — Java `break`/`continue`/early-`return`
   turned into wrong `boundary.break` targets, no-op `()` branches, or dropped
   write-backs (GlyphLayout, Selection.choose, ai PriorityQueue, truncateRun,
   getTileIds, PolygonRegionLoader).
2. **Two halves never connected** — fully ported machinery with zero call
   sites (ParticleEffectCodecs 2,124 lines dead; LzmaUtils 2,580 lines dead;
   controller `init()`s; DistributionAdapters; keyboard-height providers;
   fd-based Android audio).
3. **Resources/classpath mismatches** — gltf shaders bundled at
   `sge/gltf/shaders/` but loaded from `net/mgsx/gltf/shaders/`; VisUI ships
   no resources at all; textra prefers `.json.lzma` files it cannot parse.
4. **Covenant/audit trust failure** — every one of the above carries
   `Covenant: full-port` and audit `pass`. Several covenants reference
   `SGE-original` instead of the real Java source (severing `enforce
   compare`), ~119 ai files + others have duplicated covenant headers, and
   the CI covenant gate is `continue-on-error: true`.

The root enabler is the test gap: the broken paths (multi-line GlyphLayout,
`SpriteCache.begin`, `VisUI.load()`, gltf `SceneManager` creation, 3D particle
load, two queued telegrams) have **plausibly never been executed once**. And
on desktop JVM/Native — the flagship targets — CI has never executed a single
GL call (see Architecture soundness).

---

## P0 — confirmed broken (new findings, verified against originals)

### Core text rendering (g2d) — broken end-to-end

1. **GlyphLayout: any multi-line text crashes or truncates**
   `sge/src/main/scala/sge/graphics/g2d/GlyphLayout.scala:110-142,175,248`.
   The `'\n'` case never advances `i`; second iteration rescans the newline,
   produces an empty run, and `setLastGlyphXAdvance(fontData, lineRun.get)`
   NPEs on `Nullable.get` — or on the alternate path silently drops everything
   after the first `'\n'`. A string *starting* with `'\n'` loops forever. The
   `runEnd > 0` guard conflates "no delimiter" with "delimiter at index 0", so
   `"[RED]text"` emits the tag characters as glyphs.
2. **GlyphLayout: truncation is a no-op that aborts layout** (`:300-356,200-204`).
   Java's loop-`break` was ported as a method-level `boundary.break`, so
   `truncateRun` always returns before removing a glyph; the call site then
   breaks out of `setText`'s outer boundary, skipping `height`,
   `calculateWidths`, and `alignRuns`.
3. **GlyphLayout: base color never pushed to `colorStack`** (`:83-87`) —
   `[]` pop tags misbehave or crash on empty stack.
4. **GlyphLayout: wrap uses a stale run + off-by-one `removeRange`**
   (`:186-234,388`). 3+ line wraps measure the wrong run; lls
   `DynamicArray.removeRange` is end-exclusive where LibGDX's is
   end-inclusive — adjusted at line 382 but not at 388, leaving a stale
   xadvance on every wrapped line.
5. **BitmapFontCache: glyph x-advance is commented out**
   `BitmapFontCache.scala:402-413` — `// gx += xAdvances[ii]` with `gx` a
   `val`. **Every glyph of a run is cached at the same x position** — all
   cache-based `BitmapFont.draw` renders overlapping glyphs. Line 419 also
   drops Java's `currentTint = WHITE_FLOAT_BITS` reset.
6. **SpriteCache unusable: inverted state guard**
   `SpriteCache.scala:874` — `if (currentCache.isEmpty) throw ...` is the
   exact inverse of Java's `if (currentCache != null) throw ...`. After a
   correct `beginCache/add/endCache` sequence, `begin()` always throws; the
   `rendering {}` bracket makes the bug un-bypassable. Also `:182-247`:
   redefining a non-last cache truncates the mesh upload.
7. **PixmapIO PNG writer emits structurally corrupt PNGs**
   `PixmapIO.scala:333-350` — `ChunkBuffer` writes into anonymous
   constructor-local buffer/CRC instances but `endChunk` reads from separate,
   forever-empty field instances: chunk length −4, zero payload, CRC of
   nothing. Breaks `PixmapIO.writePNG` and `PixmapPackerIO.save(PNG)`.

### Other core graphics

8. **Pixmap scale-filter semantics inverted** `Gdx2dDraw.scala:677` —
   LINEAR dispatches to nearest-neighbour and vice versa (vs gdx2d.c:937-944).
9. **PolygonSpriteBatch renders stale triangles when one draw call exceeds
   batch capacity** `PolygonSpriteBatch.scala:781-801` — recomputed
   `triangleIdx` is a dead store; the final partial flush uses the first
   batch's full triangle count.
10. **CameraGroupStrategy decal sort inverted** (front-to-back instead of
    back-to-front) `graphics/g3d/decals/CameraGroupStrategy.scala:75-81` —
    `Ordering.fromLessThan` flipped the comparator sign; transparent decals
    blend wrongly. (Sibling `SimpleOrthoGroupStrategy` is correct.)
11. **ObjLoader drops triangles for any face with >3 vertices**
    `graphics/g3d/loader/ObjLoader.scala:135-159` — the port "fixed" Java's
    decoy `i--` but kept the two inner `++i`, advancing 3 per iteration: a
    quad yields one triangle instead of two. Quads are the most common OBJ
    content.
12. **3D particle effects: load and save both dead; 2,124-line codec file has
    zero callers** `graphics/g3d/particles/ParticleEffectLoader.scala:161,70,115`
    — `loadSync` throws because nothing ever populates `ResourceData.resource`
    (`fromJson` documents that the caller must, and no caller does), and
    `save` serializes everything *except* the effect definition. The complete
    `ParticleEffectCodecs.scala` that could do both is never referenced. This
    loader **is** registered as an AssetManager default (`AssetManager.scala:89`),
    so any `.pfx` load throws.

### Math

13. **DelaunayTriangulator and ConvexHull return wrong indices for unsorted
    input** `math/DelaunayTriangulator.scala:304`, `math/ConvexHull.scala:213,291`
    — `quicksortPartition` receives `originalIndices.toArray`, a **fresh copy
    per loop iteration** (lls `DynamicArray.toArray` copies), so all index
    swaps are discarded and the final remap is an identity no-op. Returned
    indices refer to the internally sorted array, not the caller's points.
    Existing tests assert only index *sets*, which masks it.
14. **DelaunayTriangulator.circumCircle: wrong COMPLETE predicate + dropped
    degenerate branch** (`:279,247-254`) — uses full squared distance instead
    of x-distance² and omits Java's `y2y3 < EPSILON → INCOMPLETE` bail; yields
    non-Delaunay triangulations.

### scene2d

15. **`Selection.choose` fires spurious ChangeEvents on no-op clicks**
    `scenes/scene2d/utils/Selection.scala:64-93` — three Java early-`return`s
    became no-op `()` branches / were dropped, so the fire/changed block is
    always reached. Every click on an already-selected item in `SgeList`,
    `Tree`, `SelectBox` fires a ChangeEvent and resets shift-range anchors.

### AI / extensions

16. **ai `PriorityQueue.siftDown` corrupts the heap — delayed messaging
    broken with ≥2 pending telegrams**
    `sge-extension/ai/src/main/scala/sge/ai/msg/PriorityQueue.scala:160-176` —
    Java's unconditional post-loop `queue[k] = x` became a three-way
    conditional wrong in every path (empirically verified: polling `{1,2}`
    yields `1,1`; 10 elements yield `0,1,2,3,4,5,6,6,6,6`). `MessageDispatcher`
    double-dispatches stale pooled telegrams and drops others. The existing
    suite only ever queues one delayed telegram.
17. **jbump: null-item `project()` always NPEs on collision**
    `sge-extension/jbump/src/main/scala/sge/jbump/Collisions.scala:108-110` —
    `items += item.get` on the documented `Nullable.Null` projection path;
    `World.scala:350` also passes a raw `null` into a non-Nullable
    `CollisionFilter.filter` parameter.
18. **BehaviorTree text parser unusable out of the box**
    `ai/btree/utils/BehaviorTreeParser.scala:196-242` — `TaskRegistry` starts
    empty while `defaultImports` maps to FQCN *strings*; parsing any standard
    tree throws `Unknown task type: 'sge.ai.btree.branch.Selector'`. Built-in
    task attributes (`repeat times:5` etc.) have no `TaskMeta`;
    `DistributionAdapters` is fully ported but has zero callers. The module's
    own tests hand-register everything — confirming the out-of-box breakage.

### glTF

19. **All shader/texture classpath resources point to `net/mgsx/gltf/shaders/*`
    but are bundled under `sge/gltf/shaders/*`** — `PBRShaderProvider.scala:308,317`,
    `PBRDepthShaderProvider.scala:65,74`, `PBREmissiveShaderProvider.scala:71-72`,
    `SceneSkybox.scala:104-106`, `IBLBuilder.scala:70-71`. `SceneManager`
    construction, every PBR/depth/emissive shader, skybox, and IBLBuilder
    throw file-not-found at first use — **the entire scene3d half of the
    extension is unusable**. `gdx-pbr.vs.glsl` and `brdfLUT.png` are missing
    from the bundle entirely.
20. **CUBICSPLINE animations crash on load** — `CubicVector3`/`CubicQuaternion`
    no longer extend `Vector3`/`Quaternion`, but `AnimationLoader.scala:102-170`
    force-casts them anyway (`.asInstanceOf[AnyRef].asInstanceOf[Vector3]` —
    a real checkcast against an unrelated final case class, always CCE).
21. **`KHR_lights_punctual` crashes any file that declares it** —
    `GLTFCodecs.scala:215-223` stores the raw Json AST in extensions;
    `GLTFLoaderBase.scala:155-160,277-281` casts it to typed light classes →
    CCE. The typed codecs exist 500 lines below and are never used by the
    dispatcher.
22. **`Scene.animations` is null in every common constructor; user-supplied
    `AnimationsPlayer` ignored** — `Scene.scala:43-52,74,125`: the "re-assign
    later" comment is never honored; `update()` uses a separate private lazy
    val. `scene.animations.playAll()` (documented gdx-gltf API) NPEs.
23. **`ModelInstanceHack(model, rootNodeIds*)` silently discards rootNodeIds**
    (`ModelInstanceHack.scala:33-37`) — `Scene(sceneModel, nodeIds*)` loads
    the whole model.

### textra

24. **KnownFonts crashes on the upstream-documented asset format** —
    `Font.scala:3714-3721` probes `.json.lzma`/`.ubj` first (same as
    upstream), but `loadJSON` (`:3357-3359`) parses neither; the LZMA binary
    is fed to the JSON reader. Meanwhile `LzmaUtils.scala` — 2,580 lines, a
    complete working LZMA codec — has **zero callers**, and
    `BitmapFontSupport.scala:142-147` still says LZMA is "not yet ported".

### VisUI

25. **No `resources/` directory exists at all** — `VisUI.load()` (skin x1/x2),
    `Locales` (6 `.properties` bundles), and `PickerCommons` (6 ColorPicker
    shader files) all load from classpath; nothing is packaged. Every no-arg
    widget constructor routes through `VisUI.getSkin`. The module has
    plausibly never been executed. (Carried over from 06-09, now confirmed
    deeper: ColorPicker shaders and i18n too. All files sit ready in
    `original-src/vis-ui/ui/src/main/resources/`.)

### Platform backends

26. **Android lifecycle callbacks never reach the listener** —
    `sge/src/main/scalajvm/sge/AndroidApplication.scala:137-161`:
    `onResume/onPause/onDestroy` notify lifecycleListeners + audio but never
    call `listener.pause()/resume()/dispose()`. The smoke test works around it
    by hand (`SmokeActivity.scala:131`); the demo launcher does not. (Matches
    the CI "LIFECYCLE" exclusion.)
27. **Android hardware keys and mouse are unreachable** —
    `AndroidInput.scala:158-189,385-396`: `onKeyDown/onKeyUp/onKeyTyped` are
    `private[sge]` with zero callers; `onGenericMotionEvent` never wired. BACK
    key, `setCatchKey`, hardware keyboards, and mice do not function. Even if
    wired: dispatch is on the UI thread (LibGDX queues to GL thread) and
    `justPressedKeys` is cleared before the game could observe it.
28. **Android sounds/music from APK assets cannot load** —
    `AndroidApplication.scala:272-293` passes `file.getPath()` of an
    `Internal` file to `SoundPool.load(path)`/`MediaPlayer.setDataSource(path)`;
    APK assets are not filesystem paths. The fd-based engine methods exist and
    are never called.
29. **Scala Native: ShortBuffer pointer math off by 2×** —
    `NativeGlHelper.scala:60-66`: `elementSize` has no ShortBuffer case
    (falls to 1). Hit by `Mesh.render` with non-zero index offset
    (`Mesh.scala:653-657`) → corrupted glDrawElements reads on Native.
30. **Browser `density` is 96× too small** — `BrowserGraphics.scala:120`
    computes `devicePixelRatio / 160f` instead of `(96 * dpr) / 160`.
31. **`supportsExtension` always false on desktop** —
    windows are `GLFW_NO_API` (`DesktopApplication.scala:331`), so
    `glfwExtensionSupported` has no current context
    (`DesktopGraphics.scala:403-404`; same on Native). Needs the
    `glGetStringi(GL_EXTENSIONS)` approach AndroidGraphics already uses.

---

## P1 — likely bugs / port gaps (new)

### Platform / FFI

- **Desktop multi-window structurally broken**: per-window EGL contexts created
  with `EGL_NO_CONTEXT` share (textures/shaders not shared across windows;
  `sharedContext` param threaded through and then ignored), and
  `destroyContext` calls `eglTerminate(display)`, killing EGL for **all**
  windows when one closes (`GlOpsJvm.scala:199,246-252`, `GlOpsNative.scala:146,185-192`).
- **gl31/gl32 advertised on an ES 3.0 context** — context requested as 3.0
  but one `AngleGL32` instance installed as gl20/30/31/32 with availability
  unconditionally true (`DesktopApplicationFactory.scala:39`,
  `DesktopApplication.scala:295-296`). `glDispatchCompute` → GL errors.
- **`glGetActiveUniformBlockName` hardcodes bufSize 1024** regardless of the
  caller's buffer capacity → native heap overflow risk
  (`AngleGL30.scala:438-439`, `AngleGL30Native.scala:405-406`).
- **`GLintptr`/`GLsizeiptr` declared 32-bit** in glBufferData/glBufferSubData/
  glMapBufferRange/glBindBufferRange/glCopyBufferSubData/glTexBufferRange on
  both JVM Panama and Native externs — ABI mismatch that happens to work on
  current ABIs (`AngleGL20.scala:320-326`, `AngleGL30.scala:169-178,263-265,399-401`,
  `AngleGL32.scala:462-465`).
- **Browser `keyTyped` fired from `keyup`** instead of `keypress`
  (`DefaultBrowserInput.scala:286-287,402-414`) — no key auto-repeat, typed
  chars arrive after keyUp; text fields feel broken on browser.
- **WebGL30 `copyUnsigned` never grows its 12,000-element scratch array**
  (`WebGL30.scala:42-56`) — large uint uniform uploads silently truncated.
- **`glDebugMessageCallback` (JVM) closes the previous upcall arena before
  installing the new one** (`AngleGL32.scala:134-166`); callback stubs also
  leak per registration (`WindowingOpsJvm.scala:568-826`).
- **Android ACTION_CANCEL posts cancel for only one pointer**
  (`AndroidTouchHandler.scala:66-90`) — other active pointers get stuck in
  `Stage` et al.
- **macOS Native missing the JVM's HiDPI sublayer-scale and resize-transaction
  fixes**; single cached layer ptr breaks multi-window
  (`WindowingOpsNative.scala:265-275` vs `WindowingOpsJvm.scala:287-342`).
- **No GLFW error callback installed** on either desktop backend — GLFW
  failures are silent.
- **`AngleGL31Native.glDrawArraysIndirect/glDrawElementsIndirect` truncate the
  Long offset via `.toInt`** (`AngleGL31Native.scala:148,151`).

### Core

- **Timer lost-wakeup**: nothing waits on `threadLock`, so `notifyAll` is a
  no-op and newly scheduled tasks can fire up to 5 s late
  (`utils/Timer.scala:67,89,284` + all three `TimerPlatformOps`).
- **`SgeHttpClient.cancel`/`close` double-free pooled requests** — the
  in-flight `onComplete` frees again; two later `obtainRequest()` calls return
  the same instance (`SgeHttpClient.scala:88-97,112-116`).
- **TMX premature-EOF check defeated**: `if (curr == -1) read = temp.length`
  makes the truncation guard unreachable — corrupt maps load with garbage
  tiles (`maps/tiled/BaseTmxMapLoader.scala:1009-1012`).
- **Skin style loading swallows missing-resource errors** — typo'd skin JSON
  loads "successfully" with uninitialized mandatory fields, NPEing later in
  `draw()` far from the cause (`SkinStyleReader.scala:72-134`). Custom widget
  styles **cannot** be loaded at all: unknown type names silently skipped,
  `getJsonClassTags` returns an immutable map despite docs saying it's
  modifiable, `SkinStyleReader.registry` is closed (`Skin.scala:104-121,624-627`).
- **`UIUtils` OS detection wrong on JS/Native** (no `os.name` shim) — Cmd vs
  Ctrl shortcuts wrong for Mac browser users; Android/iOS Enter-traversal
  never triggers in mobile browsers (`scenes/scene2d/utils/UIUtils.scala:35-54`).
- **`Pixmap.downloadFromUrl` never ported** while its response-listener trait
  remains as dead API (`Pixmap.scala:483-493`); covenant was baselined without
  the method so `enforce verify` can't notice.
- **ResourceData round-trip corrupts non-string SaveData values** (Config and
  nested-array payloads saved as `toString`; Long→Int unboxing CCEs) —
  currently masked by P0 #12 (`ResourceData.scala:214-240,292-336`).
- **`ParticleShader` sets `u_screenWidth` from `camera.viewportWidth`** instead
  of framebuffer pixels — point-sprite particles wrongly sized
  (`ParticleShader.scala:452-458`).

### Extensions

- **textra widgets are detached from SGE's scene2d** although SGE ships a full
  scene2d: `TextraLabel`/`TextraButton`/`TextraWindow`/dialogs/tooltips are
  standalone classes with no-op `setStage`, placeholder actions
  (`"fadeOut:0.4" /* standalone action placeholder */`), and deferred
  Container/TooltipManager/Scaling — they cannot be added to a Stage or Table
  (`TextraLabel.scala:205-212`, `TextraDialog.scala:356-363`, etc.).
- **`LinkEffect` is a stub** — `{LINK=url}` clicks do nothing
  (`textra/effects/LinkEffect.scala:25-26`); `TextraField` never connects
  clipboard or emoji replacement (`TextraField.scala:107-108,192-194`).
- **colorful `Shaders` intentionally omits HSLuv shaders and all
  `make*Batch()`/`make*Shader()` factories** with a "highly experimental"
  rationalization (`Shaders.scala:1198-1203`) — the exact pattern CLAUDE.md
  forbids; covenant baked without those names.
- **anim8 palettes + colorful oklab gamut cannot link on Scala.js**:
  `getClass.getResourceAsStream` doesn't exist there (verified empirically
  with a minimal JS link) — essentially all of anim8 and oklab is JS-unusable
  (`anim8/ConstantData.scala:28`, `colorful/oklab/Gamut.scala:28`). On plain
  Scala Native (without sge-build's embed-resources) it also fails.
- **Controllers still fully unwired** (4th review pass to find this): all four
  platform `init()`s exist, zero call sites, no doc, `Controllers.poll()`
  never invoked anywhere; plus GLFW `uniqueId` uses the model GUID so two
  identical gamepads merge into one (`GlfwControllerJvm.scala:111-114`).
- **`ShaderTransition.ignorePrepend` is dead** — GLTransitions shaders break
  if anything sets `ShaderProgram.prepend*Code`, and SGE's own extensions do
  (`screens/transition/impl/ShaderTransition.scala:45,51`).
- **No `NestableFrameBuffer` ported for screenmanager** — a screen using its
  own FBO during a transition rebinds the default framebuffer mid-capture
  (`ScreenManager.scala:35,99` vs core `GLFrameBuffer.scala:416-433`).
- **TexturePacker CLI ignores its settings-file argument** (`TODO` in a
  covenant-full-port file; the parsing capability exists in
  `TexturePackerFileProcessor.merge`) (`tools/TexturePacker.scala:1078-1101`).
- **`PathFinderQueue` no longer implements `Schedulable`** — the canonical
  gdx-ai scheduler composition cannot be assembled
  (`ai/pfa/PathFinderQueue.scala:50`).
- **VisTextField `setReadOnly` and `setEnterKeyFocusTraversal` are silent
  no-ops** — fields stored, never read; the behavior lives in core
  `TextField`, which was never extended (`VisTextField.scala:47,50,127-128,146-149`).
  ISS-445 was marked resolved on the accessors alone.
- **VisUI `Draggable.MimicActor` size always 0** — `updateSize()` has zero
  callers; keep-within-parent clamping wrong, affecting TabbedPane tab drag
  and DragPane drop math (`Draggable.scala:336-337,171-198`).
- **`MeshTangentSpaceGenerator` dropped the public Mesh-level overload**;
  **`GLTFBinaryExporter.savePNG` uses `Class.forName` reflection** (breaks
  Native/JS) when `PixmapIO.writePNG` is directly callable.

### CI / release

- **`test-desktop-it` is green but executes zero assertions** — windowed test
  `assume`-skips (no display on ubuntu-latest, no xvfb step); headless test
  `assume`-skips because `java.library.path` points at a local Rust build dir
  CI never creates (`build.sbt:679`, `DesktopIntegrationTest.scala:45,92`).
  The well-built 23-check `DesktopHarness` never runs anywhere.
- **`SGE_SKIP_NATIVE_VALIDATION=true` is set on the actual Sonatype release**
  (`release.yml:41`) and on all 9 CI publish steps — the native-lib
  completeness gate is dead code at the moment it matters most.
- **release.yml**: `cancel-in-progress: true` sits under `env:` (inert), and
  the workflow triggers `sbt ci-release` on every `pull_request`
  (`release.yml:7-8,14-16`).
- **Covenant enforce remains `continue-on-error: true`** (`ci.yml:79,85`),
  contradicting CLAUDE.md's "CI runs enforce verify --all as a gate".
- **Android smoke pass condition is weak**: `"SMOKE_TEST_PASSED" || "SGE-SMOKE:
  Frame "` — any rendered frame log passes even without the PASSED marker
  (`AssetShowcaseTest`/`AndroidSmokeTest.scala:248,301`).
- **Browser packaging has zero CI coverage**: no job invokes
  `sgePackageBrowser` or `fullLinkJS`; all Playwright tests serve `fastLinkJS`
  with synthetic HTML; AssetShowcase smoke (the only asset-over-HTTP test) is
  `.ignore`'d.

---

## Architecture soundness — what is proven vs claimed

| Evidence | JVM desktop | Browser (JS) | Scala Native | Android |
|---|---|---|---|---|
| Unit tests in CI | ✅ 6 OS/arch combos, real Panama FFI downcalls | ✅ Node, 88 shared suites | ✅ 5 platforms | ⚠️ JVM-side adapter tests only |
| Real app boots in CI | ❌ desktop-IT vacuous (assume-skips) | ✅ regression app + 10/11 demos in Chromium | ❌ `--headless` skips GLFW/EGL/GL; demos link-only | ✅ smoke APK on emulator |
| Rendering proven (pixels) | ❌ (even local harness has no readback) | ⚠️ non-blank-canvas heuristic only | ❌ | ⚠️ "no GL error" only |
| Input/lifecycle proven | ❌ | ⚠️ tests assert browser events, not SGE dispatch | ❌ | ❌ TOUCH_DISPATCH/LIFECYCLE permanently excused |
| Packaging proven | ⚠️ built, never launched | ❌ packaging path untested | ⚠️ linked, never executed | ⚠️ smoke APK only; 11 demo APKs never built in CI |

**The only platforms where CI proves a real game runs are Browser and
Android.** The flagship desktop targets (JVM + Native, the GLFW + ANGLE +
Panama stack — the riskiest code in the project) have **zero CI runtime GL
execution**. "Works on all 4 platforms" is currently an assertion, not a
proof.

## Missing-test priorities (ranked by risk)

1. Run the desktop GPU harness in CI (xvfb or headless EGL) + fix the
   `java.library.path` wiring to use provider-JAR extraction — unlocks 23
   already-written checks.
2. Headless GlyphLayout/BitmapFontCache tests (`"a\nb"`, wrap, markup,
   truncate) — would have caught P0s 1–5 instantly.
3. PNG write→decode round-trip test (catches P0 7); golden-byte GIF/PNG8
   tests in anim8.
4. One-fixture glTF end-to-end load (catches P0s 19–22) + "default shader
   resources resolve" test; same for `VisUI.load()` (catches P0 25).
5. 3D particle effect save→load round-trip (P0 12); 2-delayed-telegram
   MessageDispatcher test (P0 16); shuffled-input index-mapping tests for
   DelaunayTriangulator/ConvexHull (P0 13).
6. Execute one native-linked binary in CI (even `--headless` RegressionApp).
7. Un-ignore AssetShowcase browser smoke; test `sgePackageBrowser` output
   end-to-end on `fullLinkJS`.
8. SpriteBatch/PolygonSpriteBatch/SpriteCache vertex-output geometry tests
   (pure math, no GL needed; catches P0s 6, 9).
9. Android TOUCH_DISPATCH/LIFECYCLE — replace permanent exclusions with
   working mechanisms.
10. sbt scripted tests for sge-build (fresh project → releasePackage /
    sgePackageBrowser / androidSign).

## API ergonomics (pit of success)

**Genuinely better than LibGDX** (credit where due): `(using Sge)` makes
construct-before-create a compile error; opaque types (`Pixels`, `WorldUnits`,
`Key`, `Seconds`) kill unit confusion; `rendering {}`/`drawing {}` brackets
make begin/end mismatch impossible; `Nullable[A]` applied consistently (1,236
uses); loud, helpful error messages in VisUI/Controllers/browser-files;
4-platform build UX far better than gdx-setup.

**Where users will get hurt:**

- **P0 trap: `Actor` exposes both public `var x/y/width/height` and
  `setX/setPosition` that fire `positionChanged()`** (`Actor.scala:83-91` vs
  `:462-542`). `actor.x = 5f` — the natural Scala spelling — silently skips
  layout invalidation. Violates the project's own getter/setter rule.
- **README advertises physics/physics3d/freetype on JS** (`README.md:37-39`);
  all three throw `UnsupportedOperationException` at first use. Platform
  limitations are discoverable only at runtime, in the browser console.
- **No "first game" path exists**: README is contributor-oriented; the
  starter template (`sge-build/src/main/resources/sge-template-build.sbt`) is
  referenced by zero files; all launch boilerplate lives in unpublished
  `demos/shared`; no demo uses `Game`/`Screen`.
- **Every Android game must hand-copy a 160-line Activity from demo code**,
  including `graphics.asInstanceOf[AndroidGraphics]` and a render-before-create
  race the demos patch with a null check in a null-banning codebase
  (`AssetShowcaseGame.scala:120`).
- **`SgePlugin` forces `-Werror -no-indent` on user projects** and the
  `Nullable` assignment ergonomics require `implicitConversions` that the
  plugin doesn't enable — tutorial-style Scala 3 fails to compile.
- **`Sge` has no `@implicitNotFound`** — the most common newbie error gets the
  default cryptic message.
- **Setter dialect is two-headed**: 776 `setX` methods vs 159 `x_=` properties;
  `dispose()` vs `close()` split at the lifecycle root (`ApplicationListener.dispose`
  vs `Screen.close`); `Volume.unsafeMake` is the everyday path even in SGE's
  own demos.
- `DesktopApplication.graphics/input` return `null` via `orNull` when no
  window is current (`DesktopApplication.scala:398-406`).

## Meta: the covenant/audit system is not currently trustworthy

- Audit DB: 1,307 pass / 4 minor — yet ~25 P0s above live in `pass` files.
- Covenants stamped `full-port` on files with stubbed bodies (LinkEffect,
  GLTFCodecs encode, TexturePacker TODO), with `Covenant-source-reference:
  SGE-original` severing comparison against the real Java (GLTFCodecs,
  Scene, ModelInstanceHack, CubicVector3...).
- Duplicated covenant headers in ~119 ai files, controllers, vfx, audio, noop
  — the stamping tool double-applies; baselines are skewed.
- CI covenant gate non-blocking; shortcut scan missable by comment phrasing
  ("placeholder" text in string literals, `// suppress unused warning until
  ... implemented`).
- Issues DB does not contain any finding from either review; "all resolved"
  is pre-review bookkeeping.

**Recommendation:** treat "audit pass + covenant + compiles on 4 platforms" as
necessary but not sufficient. The gap is *execution*: every P0 in this report
is in code no test has ever run. Wire the enforcement gates to block, and
require a behavioral test (not a method-set comparison) before stamping
`full-port` on anything with logic.

## What is genuinely solid (consistent across agents)

- Math core (Matrix3/4, Quaternion, Intersector, splines) — faithful and
  well-tested; the risk is isolated to the two triangulators/hull.
- AssetManager refcount/dependency pipeline; tiled renderer culling math;
  viewport family; I18N; RandomXS128; GestureDetector.
- SpriteBatch/CpuSpriteBatch single-quad geometry, NinePatch, TextureAtlas,
  PixmapPacker, ParticleEmitter (2D), ShaderProgram (improves on LibGDX),
  Mesh/GLTexture/FrameBuffer managed-resource machinery.
- DefaultShader/DepthShader GLSL byte-identical to upstream; MeshBuilder +
  all shapebuilders; decal vertex math; particle simulation math (the
  *runtime*; the I/O is dead).
- scene2d: Table/Cell layout, ScrollPane, TextField editing internals, Tree,
  Window/Dialog, action pooling protocol, DragAndDrop — line-faithful.
- WebGL20 as a GWT port; JVM Panama Angle bindings' marshalling; desktop
  key mapping; ANGLE EGL bootstrap on JVM (macOS CALayer work is thoughtful).
- jbump sweep math, noise4j byte-faithful, simple-graphs, ashley/ecs, gdx-ai
  btree runtime + steering math, IndexedAStarPathFinder.
- VisUI big-widget fidelity (FileChooser, TabbedPane, PopupMenu, Spinner,
  ColorPicker logic — once resources exist); vfx manager/ping-pong/effects;
  ScreenManager state machine; MaxRectsPacker; FreeTypeFontGenerator;
  physics JVM FFI arena discipline.
- anim8 dithering/GIF/PNG encoding math (modulo the dead ChunkBuffer twin in
  core PixmapIO); colorful color-space matrices bit-identical.
- Browser + Android CI smoke infrastructure is genuinely substantive where it
  exists; test quality (where tests exist) is high — behavioral assertions,
  recording stubs, almost no ignored tests.

## Coverage accounting

| Agent domain | Files in scope | Deep-read | Skimmed/structural |
|---|---|---|---|
| graphics core + g2d + glutils | 98 | 51 | 47 |
| graphics/g3d (incl. particles) | 143 | 72 | 71 |
| scenes/scene2d | 102 | 40 | 62 |
| maps/utils/math/assets/net/audio/input/files/top-level | 206 | 32 | 45 (+130 indirect) |
| platform backends (4 platforms + GL bindings) | ~220 | 33 (all 11 GL binding files line-by-line) | ~20 (+140 not opened) |
| gltf | 141 | 55 | 86 |
| visui | 158 | 41 | 117 |
| textra/colorful/anim8 | 155 | 31 | 124 |
| ai/graphs/ecs/jbump/noise | 205 | 116 | 89 |
| vfx/screens/controllers/physics/freetype/tools | 143 | 44 | 99 |
| tests/CI/build/demos | ~230 + yml | — | full pass |
| API ergonomics (cross-cutting) | — | entry points + 4 demos + docs | — |
| prior-findings verification | 40 findings | all re-verified | — |

Not exhaustively covered: browser/desktop audio engine internals
(WebAudio/Miniaudio), some `sge-jvm-platform/api` mechanical ops traits,
numeric diff of the ~17 simple vfx shader constant tables, external
sge-native-providers Rust code (out of repo). No code was executed beyond
two standalone empirical checks (ai PriorityQueue extraction, Scala.js
`getResourceAsStream` link test) and read-only `re-scale` commands.

---

*Generated by 13 parallel review agents, 2026-06-10. Static analysis +
verification against original-src; no project code modified.*
