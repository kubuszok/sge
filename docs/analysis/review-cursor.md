# SGE Codebase Review (Cursor)

Independent review for model comparison; findings appended incrementally.

**Date:** 2026-03-09

## Scope and method

- **In scope:** `sge/` (main Scala port), `sge-it-tests/`, and extension modules `sge-freetype`, `sge-physics`, `sge-tools` where relevant.
- **Reference only:** LibGDX Java sources under `libgdx/` — used for parity checks, not as part of the product.
- **Independence:** This review was conducted without reading `review-claude.md` or `review-codex.md`; findings are from direct code inspection only.

## Summary

| Severity  | Count |
|-----------|-------|
| Blocker   | 3 (Gdx2DPixmap stubs, physics native lib name, integration gaps) |
| High      | 12 |
| Medium    | 11 |
| Low       | 2 |

**By category:** Missing tests / flow gaps (6); stub-only / incomplete (6); null / getOrElse / error-prone Scala (11); port errors / boundary scope (2); error handling (1); shared mutable state (1); release blockers / platform (3).

## Findings

*(Append-only; each entry has ID, category, severity, description, and file references.)*

---

### Batch 1: Core lifecycle, stubs, null safety, and graphics

#### CR-001. No tests for Game or Screen flow
- **Category:** missing test, flow gap  
- **Severity:** high  
- **What’s wrong:** `Game` and `Screen` are never exercised in tests. There are no tests for `screen_=`, show/hide/resize delegation, or render/pause/resume forwarding.  
- **Why it matters:** The main app pattern (Game + multiple screens) is completely untested; regressions in screen switching or lifecycle would go unnoticed.  
- **Files:** `sge/src/main/scala/sge/Game.scala`, `sge/src/main/scala/sge/Screen.scala`; test search shows no `Game`/`Screen`/`setScreen`/`screen_=` in tests.

#### CR-002. Gdx2DPixmap is stub-only; Pixmap-backed rendering cannot work
- **Category:** stub-only, release blocker  
- **Severity:** blocker  
- **What’s wrong:** `Gdx2DPixmap` implements all native operations as stubs: `load`, `loadByteBuffer`, `newPixmap`, `free`, `clear`, `setPixel`, `getPixel`, `drawLine`, `drawRect`, `drawCircle`, `fillRect`, `fillCircle`, `fillTriangle`, `drawPixmap`, `setBlend`, `setScale` (and `getFailureReason` returns a constant).  
- **Why it matters:** Pixmap creation from dimensions or encoded data does nothing useful; texture upload and readback paths that depend on pixmaps cannot work.  
- **Files:** `sge/src/main/scala/sge/graphics/g2d/Gdx2DPixmap.scala` (lines 207–275, 273–275).

#### CR-003. Raw null assignments in Stage.reset()
- **Category:** error-prone Scala (null usage)  
- **Severity:** high  
- **What’s wrong:** `Stage.reset()` sets `listenerActor = null`, `listener = null`, `target = null`, violating the project rule to use `Nullable[A]` instead of raw null.  
- **Why it matters:** NPE risk and inconsistent null discipline; makes static analysis and refactors harder.  
- **Files:** `sge/src/main/scala/sge/scenes/scene2d/Stage.scala` (lines 937–939).

#### CR-004. GlyphLayout uses raw null for hot-path state
- **Category:** error-prone Scala (null usage)  
- **Severity:** high  
- **What’s wrong:** `wrapGlyphs` uses `var lineRun: GlyphRun = null` and `var lastGlyph: BitmapFont.Glyph = null`, plus `lineRun = null` and `lastGlyph = null` assignments. Also uses `wrappedLine.orNull` in a hot loop.  
- **Why it matters:** Violates project null-safety rules and increases NPE risk in text layout.  
- **Files:** `sge/src/main/scala/sge/graphics/g2d/GlyphLayout.scala` (lines 82–83, 164, 202, 207, 225–226).

#### CR-005. SgeHttpResponse.getHeader returns null when absent
- **Category:** error-prone Scala (getOrElse(null))  
- **Severity:** medium  
- **What’s wrong:** `response.header(name).getOrElse(null)` returns null to the caller when the header is missing, exposing null in the API.  
- **Why it matters:** Callers can NPE if they don’t null-check; API should return `Option[String]` or a non-null sentinel.  
- **Files:** `sge/src/main/scala/sge/net/SgeHttpResponse.scala` (line 39).

#### CR-006. BitmapFontCache.setColors / draw(Batch, start, end) — break() exits whole method
- **Category:** port error (boundary/break scope)  
- **Severity:** high  
- **What’s wrong:** In `setColors`, when `glyphIndex >= end` the code does `scala.util.boundary.break()`, which exits the entire method. In `draw(spriteBatch, start, end)`, the same pattern is used for “glyph out of bounds” and “page doesn’t need to be rendered”. So after the first page that hits one of these conditions, remaining pages are not processed.  
- **Why it matters:** Intended behavior was likely to break only the inner loop and continue to the next page; current behavior can skip rendering or color updates for other pages.  
- **Files:** `sge/src/main/scala/sge/graphics/g2d/BitmapFontCache.scala` (lines 202, 253, 264).

#### CR-007. ComparableTimSort and ObjLoader use raw null
- **Category:** error-prone Scala (null usage)  
- **Severity:** medium  
- **What’s wrong:** `ComparableTimSort` sets `this.a = null` and `tmp(i) = null`. `ObjLoader` uses `var line/tokens = null` and `line = null` in parsing loops.  
- **Why it matters:** Violates project rule; maintains Java-style null in Scala code.  
- **Files:** `sge/src/main/scala/sge/utils/ComparableTimSort.scala` (96, 99); `sge/src/main/scala/sge/graphics/g3d/loader/ObjLoader.scala` (82–83, 100, 122, 347–348).

#### CR-008. SgeError extends Exception(cause.orNull) without @nowarn
- **Category:** error-prone Scala (orNull at API boundary)  
- **Severity:** low  
- **What’s wrong:** `SgeError` passes `cause.orNull` to `Exception`; project rule is to use `@nowarn` when using `orNull` at boundaries.  
- **Why it matters:** Linter/IDE may flag deprecated usage; consistency with project rules.  
- **Files:** `sge/src/main/scala/sge/utils/SgeError.scala` (line 19).

---

### Batch 2: Assets and loaders

#### CR-009. No flow test for AssetManager → Loader → resource
- **Category:** missing test, flow gap  
- **Severity:** high  
- **What’s wrong:** There is no integration test that loads a real asset type (e.g. Texture, BitmapFont, TextureAtlas) through AssetManager with a real loader and resolver. AssetManagerUnitTest only uses a stub `TestAsset` and `TestAssetLoader`.  
- **Why it matters:** Loader bugs (dependencies, loadSync/loadAsync, parameters) and resolver/FileHandle interaction are not validated end-to-end.  
- **Files:** `sge/src/test/scala/sge/assets/AssetManagerUnitTest.scala`; all loaders under `sge/src/main/scala/sge/assets/loaders/`.

#### CR-010. All concrete asset loaders documented as untested
- **Category:** missing test  
- **Severity:** high  
- **What’s wrong:** Migration notes in every loader list TODOs like “test: TextureAtlasLoader getDependencies/load”, “test: BitmapFontLoader …”, “test: PixmapLoader loadAsync/loadSync”, etc. None of these tests exist.  
- **Why it matters:** Texture, Font, Atlas, Pixmap, Shader, Sound, Music, Skin, I18N, Cubemap, Model, ParticleEffect loaders have no unit or integration tests.  
- **Files:** `sge/src/main/scala/sge/assets/loaders/*.scala` (TextureAtlasLoader, BitmapFontLoader, PixmapLoader, ShaderProgramLoader, SoundLoader, MusicLoader, SkinLoader, I18NBundleLoader, TextureLoader, CubemapLoader, ModelLoader, ParticleEffectLoader, FileHandleResolver).

#### CR-011. RefCountedContainer uses var obj: Any = null
- **Category:** error-prone Scala (null usage)  
- **Severity:** medium  
- **What’s wrong:** `AssetManager.RefCountedContainer` is defined with `var obj: Any = null` (with a comment and SuppressWarnings). Internal use of null for “not yet loaded” is still a project-rule violation.  
- **Why it matters:** Any code that assumes `obj != null` after load could be wrong if initialization ordering changes; Nullable would make the state explicit.  
- **Files:** `sge/src/main/scala/sge/assets/AssetManager.scala` (lines 853–854).

---

### Batch 3: Graphics (g2d, glutils, g3d)

#### CR-012. compareTo / compare stubs in light attributes and DefaultShader
- **Category:** stub-only, port error  
- **Severity:** medium  
- **What’s wrong:** `SpotLightsAttribute`, `PointLightsAttribute`, `DirectionalLightsAttribute` (and similar) use `else 0` in `compare(that: Attribute)` with FIXME “implement comparing”. `DefaultShader.compareTo(other: Shader)` also has `else 0 // FIXME compare shaders`.  
- **Why it matters:** Material/shader sorting can be wrong, leading to incorrect draw order and possible rendering artifacts or extra state changes.  
- **Files:** `sge/src/main/scala/sge/graphics/g3d/attributes/SpotLightsAttribute.scala` (56), `PointLightsAttribute.scala`, `DirectionalLightsAttribute.scala`; `sge/src/main/scala/sge/graphics/g3d/shaders/DefaultShader.scala` (650–653).

#### CR-013. ModelCache.end() passes camera.getOrElse(null) to sorter
- **Category:** error-prone Scala (null passed to API)  
- **Severity:** high  
- **What’s wrong:** `sorter.sort(camera.getOrElse(null), items)` — when `camera` is empty, `null` is passed as the `Camera` argument to `sort`.  
- **Why it matters:** Sorters may assume non-null camera (e.g. for distance-based sorting); type allows null and can cause NPE or wrong behavior.  
- **Files:** `sge/src/main/scala/sge/graphics/g3d/ModelCache.scala` (line 116).

#### CR-014. DecalBatch uses getOrElse(null) for material comparison
- **Category:** error-prone Scala (getOrElse(null))  
- **Severity:** low  
- **What’s wrong:** `lastMaterial.getOrElse(null).equals(decal.material)` — when `lastMaterial` is non-empty this is safe due to short-circuit, but the pattern exposes null in the expression.  
- **Why it matters:** Style violation; could be `lastMaterial.forall(_ != decal.material)` or similar to avoid null.  
- **Files:** `sge/src/main/scala/sge/graphics/g3d/decals/DecalBatch.scala` (line 161).

#### CR-015. DecalBatch.close() sets vertices = null
- **Category:** error-prone Scala (null usage)  
- **Severity:** medium  
- **What’s wrong:** `close()` does `vertices = null` to drop the buffer.  
- **Why it matters:** Violates project “no raw null” rule; any later use of `vertices` before re-init could NPE.  
- **Files:** `sge/src/main/scala/sge/graphics/g3d/decals/DecalBatch.scala` (line 206).

#### CR-016. DefaultRenderableSorter has incomplete sorting (FIXME)
- **Category:** stub-only  
- **Severity:** low  
- **What’s wrong:** Comment “FIXME implement better sorting algorithm” and commented-out Java logic for same shader/mesh/lights/material.  
- **Why it matters:** Sorting may be suboptimal for batching.  
- **Files:** `sge/src/main/scala/sge/graphics/g3d/utils/DefaultRenderableSorter.scala` (61–64).

#### CR-017. Pool.Poolable TODOs for Scene2D and g3d types
- **Category:** stub-only / incomplete  
- **Severity:** medium  
- **What’s wrong:** Migration notes list “TODO: define given Poolable[Action]”, “Poolable[Event]”, “Poolable[Cell]”, “Poolable[Transform]”, “Poolable[FloatCounter]”. Some types extend `Pool.Poolable` but the typeclass instance may be missing or not wired.  
- **Why it matters:** Pool reuse may not work correctly for these types, increasing allocations.  
- **Files:** `sge/src/main/scala/sge/scenes/scene2d/Action.scala`, `Event.scala`, `scenes/scene2d/ui/Cell.scala`; `sge/src/main/scala/sge/graphics/g3d/utils/BaseAnimationController.scala` (Transform); `sge/src/main/scala/sge/math/FloatCounter.scala`.

#### CR-018. StreamUtils and BitmapFont swallow exceptions
- **Category:** error handling  
- **Severity:** high (StreamUtils), medium (BitmapFont)  
- **What’s wrong:** `StreamUtils.copyStream` catches `Throwable` and swallows it (empty catch). `BitmapFont` has `catch { case _: Exception => }` that silently ignores load failures.  
- **Why it matters:** Failures are invisible; debugging is hard; StreamUtils can hide OOM/StackOverflow.  
- **Files:** `sge/src/main/scala/sge/utils/StreamUtils.scala` (123); `sge/src/main/scala/sge/graphics/g2d/BitmapFont.scala` (514).

#### CR-019. SpriteCache tempVertices — possible shared mutable state
- **Category:** error-prone Scala (shared mutable state)  
- **Severity:** medium  
- **What’s wrong:** `tempVertices` is used in multiple `add(...)` methods but its definition is not in the class body (likely on companion object or inherited). If it is a static/object-level array, all SpriteCache instances share one buffer.  
- **Why it matters:** Concurrent or reentrant use of SpriteCache could corrupt vertex data.  
- **Files:** `sge/src/main/scala/sge/graphics/g2d/SpriteCache.scala` (uses of `tempVertices` 272+).

---

### Batch 4: Scene2D, maps, utils

#### CR-020. Scene2D has almost no tests
- **Category:** missing test  
- **Severity:** high  
- **What’s wrong:** Only `SkinStyleReaderTest` exists for the scene2d stack. Stage, Actor, Group, Event, Actions, and all UI widgets (Button, Label, Table, etc.) are untested.  
- **Why it matters:** The main UI framework has no regression safety; layout, hit testing, and event propagation are easy to break.  
- **Files:** `sge/src/test/scala/sge/scenes/scene2d/ui/SkinStyleReaderTest.scala`; all of `sge/src/main/scala/sge/scenes/scene2d/`.

#### CR-021. MapProperties.get(key) returns null when key missing
- **Category:** error-prone Scala (API returns null)  
- **Severity:** high  
- **What’s wrong:** `get(key: String): Any` is implemented as `properties.getOrElse(key, null)`, so callers receive null when the key is absent.  
- **Why it matters:** Callers that don’t null-check can NPE; the API could return `Option[Any]` or a dedicated “missing” type.  
- **Files:** `sge/src/main/scala/sge/maps/MapProperties.scala` (lines 45–46).

#### CR-022. Tiled map loaders use null / orNull
- **Category:** error-prone Scala (null usage)  
- **Severity:** medium  
- **What’s wrong:** BaseTmjMapLoader and BaseTmxMapLoader use `texture.orNull`, `image.getOrElse(null: FileHandle)`, and `runOnEndOfLoadTiled = null` (and similar).  
- **Why it matters:** Keeps null in the codebase and at boundaries; increases NPE risk.  
- **Files:** `sge/src/main/scala/sge/maps/tiled/BaseTmjMapLoader.scala` (154, 278, 637, 753); `BaseTmxMapLoader.scala` (316 and similar).

#### CR-023. Timer uses synchronized; not JS-safe
- **Category:** release blocker (platform), incorrect assumption  
- **Severity:** high  
- **What’s wrong:** Timer is built on `synchronized` and `Thread.sleep`; migration notes say “TODO: redesign with Gears structured concurrency — synchronized+Thread.sleep won’t work well on JS”.  
- **Why it matters:** Scala.js does not support `synchronized`; Timer will not work on the JS backend without a different implementation.  
- **Files:** `sge/src/main/scala/sge/utils/Timer.scala` (and docs/improvements/dependencies.md).

#### CR-024. RemoteInput has unwired postRunnable and stub behavior
- **Category:** stub-only  
- **Severity:** medium  
- **What’s wrong:** TODO states “postRunnable not yet wired (EventTrigger.run() called directly)”. Various Input methods (e.g. setOnscreenKeyboardVisible, vibrate, setCatchKey) may be no-ops or stubs for desktop.  
- **Why it matters:** Remote input and desktop input behavior may diverge from LibGDX or be incomplete.  
- **Files:** `sge/src/main/scala/sge/input/RemoteInput.scala` (migration notes and method bodies).

---

### Batch 5: Extensions and release blockers

#### CR-025. Physics extension expects wrong native library name (blocker)
- **Category:** release blocker  
- **Severity:** blocker  
- **What’s wrong:** JVM code uses `System.mapLibraryName("sge_physics")` and Scala Native uses `@link("sge_physics")`, but the Rust crate in `native-components/` is built as `[lib] name = "sge_native_ops"`. The resulting shared library is `libsge_native_ops.so` (or equivalent), not `libsge_physics.so`.  
- **Why it matters:** The physics extension cannot load the native library on JVM or Native; runtime will fail when physics is used.  
- **Files:** `sge-physics/src/main/scalajvm/sge/platform/PhysicsOpsPanama.scala` (29); `sge-physics/src/main/scalanative/sge/platform/PhysicsOpsNative.scala` (17); `native-components/Cargo.toml` (lib name).

#### CR-026. No integration tests for rendering, input, or asset flows
- **Category:** missing test, flow gap  
- **Severity:** high  
- **What’s wrong:** There are no tests that run the full flow: Application → Screen → Camera → Batch → Texture/Pixmap, or Input → Processor → Stage, or AssetManager → real loader → resource. Integration-test-gaps.md lists many unchecked items (Pixmap, FBO readback, window lifecycle, input dispatch, etc.).  
- **Why it matters:** End-to-end behavior is unverified; regressions in wiring or lifecycle are likely to slip through.  
- **Files:** `docs/progress/integration-test-gaps.md`; test layout under `sge/src/test` and `sge-it-tests/`.

#### CR-027. Gdx2DPixmap stubs block desktop/browser rendering confidence
- **Category:** release blocker, stub-only  
- **Severity:** blocker  
- **What’s wrong:** All Gdx2DPixmap native operations are stubs (see CR-002). Integration-test-gaps.md explicitly lists this as blocking Pixmap creation, texture upload, SpriteBatch default white texture, FBO readback, and atlas/region flows.  
- **Why it matters:** Any release claiming working 2D rendering on desktop or browser is misleading until pixmap-backed paths are implemented and tested.  
- **Files:** `sge/src/main/scala/sge/graphics/g2d/Gdx2DPixmap.scala`; `docs/progress/integration-test-gaps.md`.

