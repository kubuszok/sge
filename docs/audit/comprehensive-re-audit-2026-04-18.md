# SGE Comprehensive Re-Audit Report

**Date**: 2026-04-18
**Scope**: All 14 ported libraries (core + 16 extensions), source files + tests
**Method**: `re-scale enforce compare`, `re-scale enforce shortcuts`, test-by-test mapping

---

## Executive Summary

### Source Audit (via `re-scale enforce compare`)

| Metric | Value |
|--------|-------|
| Total source files in scan targets | ~1,343 |
| Files compared against Java originals | ~1,105 (all 4 agents complete) |
| Files skipped (SGE-original, no Java counterpart) | ~79 |
| Files clean (0 missing members) | ~291 (26%) |
| Files with gaps | ~814 (74%) |
| Raw "missing" member count | ~5,956 |
| **True functional gaps** (after filtering getter/setter renames, type refs, dispose→close) | **~250-350 across all libraries** |

### Enforcement Baseline

| Metric | Value |
|--------|-------|
| Total shortcuts found | 344 |
| Truly actionable shortcuts | ~17 (simplified-comment: 7, not-yet-comment: 7, ???: 3) |
| Stale stubs | 8 in 4 files (all inherited from original sources) |
| Files with covenant headers | ~35 (2.6%) |
| Minor issues in audit DB | 30 (10 represent real functional gaps) |

### Test Audit

| Metric | Value |
|--------|-------|
| SGE test files | 148 |
| SGE test methods | ~1,825 |
| Original unit test files (across all 14 libraries) | 51 |
| Original unit test methods | 344 |
| Original test methods ported to SGE | 174 (51%) |
| Original test methods missing | 170 (49%) |
| **Adjusted missing** (excluding replaced classes) | **~30-40 genuinely portable** |

### Key Finding

**The SGE port is substantially complete.** The raw numbers (74% of files show "gaps", ~5,956 "missing" members) are misleading — **~85-90% of reported missing members are systematic naming convention changes** (Java getters/setters → Scala properties, dispose → close, type reference imports, anonymous inner class names). True functional gaps are concentrated in ~25-30 files across the entire project, with the largest in VisTextField (102), FileChooser (77), Font (39), TextraField (35), and CaseInsensitiveIntMap (31).

### Cross-Agent Totals

| Agent | Scope | Compared | Clean | Gaps | Raw Missing |
|-------|-------|----------|-------|------|-------------|
| A: Core Graphics | g2d, g3d, glutils, profiling | 181 | 51 | 130 | ~1,500 |
| B: Core Other | math, utils, scenes, maps, net, assets, audio, input | 213 | 30 | 183 | ~1,475 |
| C: High-Risk Ext | ai, textra, visui, gltf | 503 | 163 | 334 | 2,275 |
| D: Other Ext | ecs, colorful, screens, graphs, anim8, jbump, noise, controllers, vfx, tools | 208 | 47 | 161 | 1,206 |
| **TOTAL** | **All 14+ libraries** | **1,105** | **291** | **808** | **~6,456** |

---

## Part 1: Shortcut Inventory (344 total)

### By Pattern Type

| Pattern | Count | Actionable? | Notes |
|---------|-------|-------------|-------|
| unsupported-op | 82 | Mostly no | ~37 from NullLimiter (by-design), ~10 from ParticleEffectCodecs (type dispatch), ~12 from FileHandles (abstract placeholder), rest legitimate |
| null-cast | 73 | No | All @nowarn annotated, Java interop boundaries |
| fixme | 31 | Review | Inherited from original Java source — same FIXMEs exist in LibGDX |
| flag-break-var | 25 | Ideally refactor | Java break pattern workarounds — should use boundary/break |
| todo | 24 | Review | Mix of original TODOs and port-specific |
| minimal-comment | 24 | No | False positives — comments containing "minimal" |
| approximation-comment | 18 | No | False positives — math approximation documentation |
| pending-comment | 14 | Mostly no | Comments about "pending" operations, not actual incomplete work |
| stub-comment | 13 | No | Controllers platform stubs (by-design cross-platform pattern) |
| placeholder-comment | 10 | Mostly no | Comments about placeholder concepts, not actual stubs |
| simplified-comment | **7** | **YES** | Code was simplified vs original |
| not-yet-comment | **7** | **YES** | Unfinished work |
| best-effort-comment | 4 | No | False positives |
| scala-unimpl (???) | **3** | **YES** | Missing implementation markers |
| get-or-else-null | 2 | No | Java interop in textra/Font.scala |
| for-now-comment | 2 | Review | Temporary implementations |
| return-comment | 2 | No | Comments about return values |
| xxx | 1 | Review | gltf/GLTFLoaderBase.scala:122 |
| break-comment | 1 | No | graphs/BinaryHeap.scala:88 |
| deferred-comment | 1 | No | Actor.scala deferred listener removal |

### By Module

| Module | Count | Top patterns |
|--------|-------|-------------|
| Core sge/ | 189 | fixme(24), flag-break-var(18), unsupported-op(25), null-cast(8), todo(12) |
| sge-extension/graphs | 54 | null-cast(50), flag-break-var(2), break-comment(1), unsupported-op(1) |
| sge-extension/ai | 41 | unsupported-op(37 NullLimiter), todo(2), flag-break-var(1), fixme(1) |
| sge-extension/gltf | 21 | unsupported-op(6 IBLBuilder), todo(6), null-cast(1), placeholder(1), not-yet(3), xxx(1), minimal(1), simplified(0) |
| sge-extension/colorful | 19 | approximation-comment(2), minimal-comment(17) |
| sge-extension/controllers | 10 | stub-comment(8), pending-comment(2) |
| sge-extension/textra | 6 | simplified-comment(3), get-or-else-null(2), unsupported-op(1) |
| sge-extension/visui | 2 | simplified-comment(2) |
| sge-extension/vfx | 2 | todo(1), not-yet-comment(1) |

### Truly Actionable Shortcuts (17)

**Simplified implementations (7)** — code logic was reduced vs original:
1. `textra/Font.scala:564` — simplified width measurement skips curly content
2. `textra/Font.scala:567` — simplified width measurement
3. `textra/Font.scala:942` — simplified truncation with ellipsis
4. `visui/FloatDigitsOnlyFilter.scala:29` — simplified text filtering
5. `visui/TabbedPane.scala:202` — simplified tab removal (Dialogs dependency not ported)
6. `colorful/cielab/ColorfulBatch.scala:231` — (minimal-comment, may be false positive)
7. `I18nBundle.scala:145` — simplified message pattern syntax flag

**Not-yet-implemented (7)**:
1. `gltf/IBLBuilder.scala:67` — buildIrradianceMap (needs FrameBufferCubemap)
2. `gltf/IBLBuilder.scala:76` — buildRadianceMap (needs FrameBufferCubemap)
3. `gltf/IBLBuilder.scala:85` — buildLUT (needs FrameBuffer + ScreenUtils.getFrameBufferPixmap)
4. `gltf/IBLBuilder.scala:88-100` — 3 more IBL methods (unsupported-op)
5. `gltf/IBLBuilder.scala:157` — close/dispose (unsupported-op)
6. `vfx/VfxGlExtension.scala:19` — platform-specific GL extension methods
7. `graphics/glutils/ShaderProgram.scala:210,225` — cache markers (minor)

**??? markers (3)**:
1. `I18nBundle.scala:95` — key-not-found returns "???" + key + "???"
2. `I18nBundle.scala:103` — same pattern
3. `I18nBundle.scala:151` — missing key documentation

---

## Part 2: Stale Stubs (8 in 4 files)

| File | Count | Detail |
|------|-------|--------|
| `graphics/g3d/loader/ObjLoader.scala:304` | 3 | TODO about HashMap.get vs iteration (inherited from Java) |
| `graphics/g3d/decals/Decal.scala:707` | 2 | TODO about Texture.getFormat() (inherited from Java) |
| `vfx/framebuffer/VfxFrameBufferPool.scala:22` | 2 | TODO for scaladoc |
| `gltf/data/extensions/KHRLightsPunctual.scala:74` | 1 | TODO about GLTF Blender exporter conversion |

All are inherited from original sources or documentation gaps — none represent missing functionality.

---

## Part 3: Minor Issues Files (30 in audit DB)

| File | Package | Issue |
|------|---------|-------|
| GlyphLayout.scala | graphics.g2d | Raw null for lineRun/lastGlyph in hot loop (documented exception) |
| DirectionalLightsAttribute.scala | graphics.g3d.attributes | FIXME implement comparing (same as Java) |
| PointLightsAttribute.scala | graphics.g3d.attributes | FIXME implement comparing (same as Java) |
| SpotLightsAttribute.scala | graphics.g3d.attributes | FIXME implement comparing (same as Java) |
| ModelMaterial.scala | graphics.g3d.model.data | (no notes) |
| ParticleControllerInfluencer.scala | graphics.g3d.particles.influencers | (no notes) |
| ParticleController.scala | graphics.g3d.particles | (no notes) |
| ParticleEffectLoader.scala | graphics.g3d.particles | (no notes) |
| ParticleSorter.scala | graphics.g3d.particles | (no notes) |
| ResourceData.scala | graphics.g3d.particles | (no notes) |
| CameraInputController.scala | graphics.g3d.utils | Uses `implicit` instead of `using` |
| FirstPersonCameraController.scala | graphics.g3d.utils | Uses `implicit` instead of `using` |
| ModelInstance.scala | graphics.g3d | (no notes) |
| FrameBufferCubemap.scala | graphics.glutils | All methods ported |
| IndexBufferObjectSubData.scala | graphics.glutils | All methods ported |
| KTXTextureData.scala | graphics.glutils | All methods ported |
| MipMapTextureData.scala | graphics.glutils | All methods ported |
| GL32Interceptor.scala | graphics.profiling | All 38 GL32 methods ported |
| TideMapLoader.scala | maps.tiled | All methods present |
| Button.scala | scenes.scene2d.ui | TODO requestRendering in draw() |
| Skin.scala | scenes.scene2d.ui | Java reflection for style fields; getJsonClassTags not ported |
| Actor.scala | scenes.scene2d | 3 TODOs for clipping/debug rendering with transforms |
| Group.scala | scenes.scene2d | Missing shapes.flush() calls for debug rendering |
| Pool.scala | utils | Changed from abstract class to trait |
| PropertiesUtils.scala | utils | Missing `store` method and helpers |
| ScreenUtils.scala | utils | getFrameBufferTexture/getFrameBufferPixmap commented out |
| Sort.scala | utils | Missing `instance()` static method |
| TextFormatter.scala | utils | Simplified placeholder implementation |
| Timer.scala | utils | LifecycleListener integration and postRunnable are TODOs |
| Font.scala | textra | Partial-port covenant; glyph-based underline fallback not ported |

**Assessment**: Of these 30, about 10 represent real functional gaps (PropertiesUtils.store, ScreenUtils methods, Timer lifecycle, Skin.getJsonClassTags, Actor clipping TODOs, TextFormatter simplification, Font underline fallback). The rest are style issues (implicit→using), inherited FIXMEs, or documented design decisions.

---

## Part 4: Test Coverage Audit

### Per-Library Test Gap Matrix

| Library | Orig Unit Tests | Orig Methods | SGE Tests | SGE Methods | Ported Methods | Missing Methods | Coverage |
|---------|----------------|--------------|-----------|-------------|----------------|-----------------|----------|
| **core-math** | 14 files | 43 | 20 files | 283 | 43 | 0 | **100%** |
| **core-utils** | 12 files | 149 | 14 files | 249 | 18 | 131 | **12%** |
| **core-graphics** | 2 files | 13 | 8 files | 103 | 13 | 0 | **100%** |
| **core-scene2d** | 0 | 0 | 4 files | 87 | N/A | N/A | N/A |
| **core-other** | 0 | 0 | 19 files | 227 | N/A | N/A | N/A |
| **core-jvm** | 0 | 0 | 29 files | 355 | N/A | N/A | N/A |
| **ecs (Ashley)** | 16 files | 112 | 12 files | 115 | 83 | 29 | **74%** |
| **ai (gdx-ai)** | 2 files | 10 | 7 files | 69 | 5 | 5 | **50%** |
| **graphs** | 5 files | 17 | 2 files | 26 | 12 | 5 | **71%** |
| **jbump** | 0 | 0 | 1 file | 14 | N/A | N/A | N/A |
| **Other extensions** | 0 | 0 | 39 files | 297 | N/A | N/A | N/A |
| **TOTAL** | **51 files** | **344** | **148 files** | **~1,825** | **174** | **170** | **51%** |

### Important context on "missing" tests

The raw 51% number is misleading. Of the 170 "missing" test methods:
- **~80 are for classes SGE intentionally replaced** (LongArray→PrimitiveArray, CharArray→PrimitiveArray, Queue→different impl, JsonValue/JsonMatcher→typed codec AST, AtomicQueue, PooledLinkedList, SortedIntList)
- **~29 are Ashley internal manager tests** (EntityManager, FamilyManager, SystemManager, ComponentOperationHandler) — testing internals that SGE restructured
- **~28 are JSON-related** (JsonMatcherTests alone = 28) — SGE has completely different JSON architecture
- **~10 are LongQueue** tests — class may not exist in SGE
- **~5 are gdx-ai orchestrator tests** — real gap
- **~9 are Sort edge case tests** — some are real gaps (null handling, invalid indices)
- **~5 are graphs internal structure tests** — minor gap

**Adjusted realistic missing count: ~30-40 genuinely portable test methods not yet ported.**

### Specific Missing Tests Worth Porting

**High priority:**
1. **Ashley EntityListenerTests** (7 methods) — listener priority ordering, family-scoped listeners
2. **Ashley EntityManagerTests** (8 methods) — delayed operation ordering, removeAll-and-add
3. **gdx-ai ParallelTest orchestrator** (5 methods) — Resume/Join policy edge cases
4. **gdx-ai IndexedAStarPathFinder dynamic graph** (2 methods) — graph updating on-the-fly
5. **Sort edge cases** (3 methods) — null handling, invalid range indices

**Medium priority:**
6. **Ashley FamilyManagerTests** (6 methods) and **SystemManagerTests** (4 methods)
7. **BitsTest** (5 methods) — if SGE has Bits equivalent
8. **MixedPutRemoveTest** (8 methods) — ObjectMap stress test
9. **graphs ArrayTest** (2 methods) and **StructuresTest** (1 method)

---

## Part 5: Cross-Language Source Compare Results

*Results from `re-scale enforce compare` across all ported libraries.*

### Agent A: Core Graphics — 190 files (181 compared, 9 skipped)

51 clean, 130 with gaps. Vast majority of "missing" are getter/setter→property renames.

**True functional gaps found:**
- `ParticleEmitter.scala`: 86 raw missing (largest file) — mostly get/set pairs but warrants investigation for actual logic gaps
- `GlyphLayout.scala`: Missing `truncate`, `wrap` methods (text layout logic)
- `Pixmap.scala`: Missing `downloadFromUrl`, `handleHttpResponse`, `run` (HTTP download callback)
- `ShapeRenderer.scala`: Missing `rect` method overload
- `ParticleBatch.scala`: Missing `load`, `save` (serialization methods — replaced by ParticleEffectCodecs)
- `G3dModelLoader.scala`: Missing `readVector2` utility method
- All Attribute subclasses: Missing `compareTo` (→Scala `compare`), `register` (refactored)
- All particle values: Missing `read`/`write` (→SGE JSON codec system)

### Agent B: Core Math/Utils/Scenes/Maps/Net/Assets/Audio/Input — 248 files (213 compared, 35 skipped)

30 clean, 183 with gaps. ~1,475 raw missing — ~70% getter/setter renames, ~20% type references.

**True functional gaps found:**
- `Affine2.scala`: Missing `getTranslation` (→should be `translation` property)
- `Intersector.scala`: Missing `getSide`/`setSide` on Plane (→property rename)
- `TextFormatter.scala`: Missing `replaceEscapeChars` — **real gap**, simplified placeholder impl
- `XmlReader.scala`: Missing parser state-machine initializers (init__xml_*) — **real gap**, Ragel-generated code not ported
- `BufferUtils.scala`: Missing `transformV*M*Jni` methods — **real gap**, JNI bulk transform ops not ported (platform-dependent)
- `PropertiesUtils.scala`: Missing `store` method — **real gap** confirmed in minor_issues DB
- `ScreenUtils.scala`: Missing `getFrameBufferTexture`/`getFrameBufferPixmap` — **known blocked** on Pixmap.createFromFrameBuffer
- `Timer.scala`: Missing `postRunnable` — **real gap**, lifecycle integration TODO
- `Net.scala`: Missing HTTP request/response inner classes — **architecture divergence** (SGE uses SgeHttpClient)
- `XmlReader.Element`: Missing `replaceChild`, `setAttribute`, `setText` — **real gap**
- `DynamicArray.scala`: Missing `selectRanked`, `selectRankedIndex` — **real gap**, selection algorithms
- `ObjectMap/ObjectSet/OrderedMap/OrderedSet`: Missing iterator inner classes — **architecture divergence** (SGE uses Scala iterators)

### Agent C: High-Risk Extensions — 503 files (163 clean, 334 with gaps, 6 skipped)

| Library | Files | Clean | Gaps | Raw Missing | Key Concern |
|---------|-------|-------|------|-------------|-------------|
| AI | 131 | 53 | 78 | 416 | ~250 are get/set→property in steer package. Real gaps: DistributionAdapters (21 distribution types), BehaviorTreeParser (8), BehaviorTreeReader (7) |
| Textra | 80 | 11 | 69 | 439 | Effects inherit base methods (paramAsFloat etc.) flagged per-subclass. Real gaps: Font.scala (39), TextraField (35), KnownFonts (30), CaseInsensitiveIntMap (31 iterator/hash methods) |
| VisUI | 153 | 38 | 115 | 1,115 | FileChooserStyle (203) maps to wrong source. VisTextField (102) is largest real gap. TabbedPane (44), FileChooser (77) are complex widgets |
| GLTF | 139 | 61 | 72 | 305 | Data classes excellent (44 files, mostly clean). Attribute register/compareTo systematic. IBLBuilder (13), GLTFExporter (19), SceneSkybox (15) |

**2,275 raw missing → estimated ~100-150 true functional gaps** after filtering:
- AI steer: ~250 getter/setter renames
- VisUI: ~500 getter/setter + anonymous listener class names
- Textra: ~200 effect base methods + type references
- GLTF: ~150 type references + attribute registration pattern

**Top files requiring manual investigation:**
- `visui/widget/VisTextField.scala` (102 raw missing) — massive widget, inner classes, drawing
- `visui/widget/file/FileChooser.scala` (77) — complex file dialog with threading
- `textra/Font.scala` (39) — core font engine
- `textra/TextraField.scala` (35) — text input with inner listeners
- `textra/CaseInsensitiveIntMap.scala` (31) — custom map collection, iterator/hash methods
- `textra/KnownFonts.scala` (30) — font loading, shader management
- `ai/btree/utils/DistributionAdapters.scala` (21) — all distribution types missing

### Agent D completed: see table above

### Agent D: Remaining Extensions (10 libraries, 208 files compared)

| Library | Files | Clean | Gaps | Raw Missing | True Gaps |
|---------|-------|-------|------|-------------|-----------|
| ECS | 21 | 5 | 16 | 38 | ~5 (ImmutableArray: equals, hashCode, random, toArray; ComponentOperationHandler internals) |
| Colorful | 45+1skip | 6 | 39 | 521 | ~40 (TrigTools: 24 inverse trig; FloatColors: 5 hsl/hcl; Shaders: 7 factories; Palette: appendToKnownColors, editKnownColors across 6 spaces) |
| Screens | 19+1skip | 8 | 11 | 28 | ~3 (SlidingDirection.DOWN, ScreenFboUtils.restoreFboStatus/retrieveFboStatus) |
| Graphs | 25 | 10 | 15 | 83 | ~15 (Path: remove/removeAll/removeIf/retainAll; InternalArray: containsAll/toArray/resize; BinaryHeap: contains/equals/hashCode/toString; DepthFirstSearch: 2 methods) |
| Anim8 | 16+1skip | 5 | 11 | 95 | ~30 (PNG8: centralizePalette, editPalette*, hueShift, oklabToRGB, readChunks, swapPalette, writeChunks, writePreciseSection, writeWrenOriginalDithered; PaletteReducer: analyzeMC, analyzeReductive, buildBigPalette, loadBigPalette, writeBigPalette, randomColor, reduceWrenOriginal) |
| JBump | 14+1skip | 3 | 11 | 70 | ~3 (Collisions: keySort, rect_getSquareDistance; Rect: nearest) |
| Noise | 10 | 2 | 8 | 63 | ~0 (all are get/set->property renames) |
| Controllers | 9+4skip | 4 | 5 | 26 | ~3 (Controllers: pause/resume lifecycle; DefaultControllerManager: substantially different impl) |
| VFX | 41 | 4 | 37 | 208 | ~15 (PrioritizedArray: Pool/Iterator restructured; VfxFrameBuffer: BatchRendererAdapter/ShapeRendererAdapter inner classes) |
| Tools | 8 | 0 | 8 | 74 | ~10 (TexturePacker/ImageProcessor property renames + compare implementations) |
| **TOTAL** | **208** | **47** | **161** | **1,206** | **~124** |

**1,206 raw "missing" → ~124 true functional gaps** after filtering:
- ~600 Java getter/setter→Scala property renames
- ~200 Java type reference imports
- ~40 dispose→close renames
- ~240 other naming conventions (getClass, StringBuilder, IllegalArgumentException, etc.)

---

## Part 6: Original Test Classification

*(pending — Agent E still running)*

---

## Part 7: Priority Recommendations

### Critical (functional gaps affecting users)

1. **gltf/IBLBuilder.scala** — 6 methods throw UnsupportedOperationException (buildIrradianceMap, buildRadianceMap, buildLUT, etc.). Blocks IBL lighting workflow.
2. **textra/Font.scala** — 3 simplified implementations (width measurement, truncation). Partial-port covenant. Glyph-based underline fallback missing.
3. **utils/Timer.scala** — LifecycleListener integration and postRunnable not connected. Posted tasks won't execute on main thread.
4. **utils/ScreenUtils.scala** — getFrameBufferTexture and getFrameBufferPixmap commented out (blocked on Pixmap.createFromFrameBuffer).
5. **utils/PropertiesUtils.scala** — Missing `store` method.

### Major (test coverage gaps)

6. Port Ashley EntityListener/EntityManager tests (15 methods)
7. Port gdx-ai orchestrator and dynamic pathfinding tests (7 methods)
8. Port Sort edge case tests (null handling, invalid indices)
9. Port graphs algorithm tests (directed disconnection, MST edge cases)

### Minor (style/convention issues)

10. CameraInputController/FirstPersonCameraController: `implicit` → `using`
11. 25 flag-break-var patterns could be refactored to boundary/break
12. Skin: Java reflection dependency for style fields (cross-platform concern)

---

## Part 8: Consolidated True Gaps (All Libraries)

This is the definitive list of real functional gaps found across the entire project, after filtering out naming convention changes. Organized by priority.

### Tier 1: Missing functionality that users may hit

| File | Missing | Impact |
|------|---------|--------|
| `gltf/IBLBuilder.scala` | 6 methods throw UnsupportedOperationException (buildIrradianceMap, buildRadianceMap, buildLUT, etc.) | Blocks IBL lighting workflow |
| `textra/Font.scala` | 3 simplified impls (width measurement, truncation, ellipsis); glyph-based underline fallback missing | Text rendering edge cases |
| `utils/Timer.scala` | postRunnable not connected, LifecycleListener integration TODO | Posted tasks won't execute on main thread |
| `utils/ScreenUtils.scala` | getFrameBufferTexture, getFrameBufferPixmap commented out | Screenshot/framebuffer capture unavailable |
| `utils/TextFormatter.scala` | replaceEscapeChars missing, simplified implementation | I18n curly-brace escaping broken |
| `utils/BufferUtils.scala` | transformV2M3Jni, transformV3M4Jni etc. (6 JNI bulk transform methods) | Bulk vertex transforms unavailable |
| `utils/DynamicArray.scala` | selectRanked, selectRankedIndex | Nth-element selection unavailable |
| `utils/PropertiesUtils.scala` | store method + helpers | Can't write .properties files |
| `utils/XmlReader.scala` | replaceChild, setAttribute, setText on Element; Ragel parser internals | XML manipulation limited |
| `anim8/PNG8.scala` | centralizePalette, editPalette*, hueShift, oklabToRGB, readChunks, swapPalette, writeChunks, writePreciseSection, writeWrenOriginalDithered (~12 methods) | Advanced palette/dither features missing |
| `anim8/PaletteReducer.scala` | analyzeMC, analyzeReductive, buildBigPalette, loadBigPalette, writeBigPalette, randomColor, reduceWrenOriginal (~10 methods) | Advanced palette reduction missing |
| `visui/widget/VisTextField.scala` | 102 raw missing — inner classes (KeyRepeatTask, TextFieldClickListener, filters), drawing methods, cursor handling | Full text input widget largely unported internals |
| `visui/widget/file/FileChooser.scala` | 77 raw missing — threading services, file browsing, inner listeners | Complex file dialog |
| `textra/CaseInsensitiveIntMap.scala` | 31 missing — iterator inner classes, hash/resize methods | Custom collection internals |
| `textra/KnownFonts.scala` | 30 missing — font loading factories, shader getters, atlas parsing | Font catalog incomplete |
| `ai/btree/utils/DistributionAdapters.scala` | 21 missing — all distribution type adapters (Gaussian, Triangular, Uniform for Float/Double/Int/Long) | BTree parameter parsing limited |

### Tier 2: Missing functionality with workarounds or lower impact

| File | Missing | Impact |
|------|---------|--------|
| `colorful/TrigTools.scala` | 24 inverse trig functions (acos, asin, atan, etc.) | Can use stdlib Math instead |
| `colorful/FloatColors.scala` | 5 hsl/hcl conversion methods | Color space conversion limited |
| `colorful/Shaders.scala` | 7 factory methods (makeBatch*, makeShader*) | Must construct batches manually |
| `colorful/*/Palette.scala` | appendToKnownColors, editKnownColors, compare (across 6 color spaces) | Dynamic palette editing missing |
| `colorful/*/SimplePalette.scala` | unevenMix, bestMatch (across 6 color spaces) | Color matching/mixing limited |
| `graphs/Path.scala` | remove, removeAll, removeIf, retainAll | Path modification limited |
| `graphs/InternalArray.scala` | containsAll, toArray, resize | Collection operations limited |
| `graphs/BinaryHeap.scala` | contains, equals, hashCode, toString | Heap comparison/debugging |
| `graphs/DepthFirstSearch.scala` | depthFirstSearch, recursiveDepthFirstSearch | Must use Algorithm facade |
| `vfx/PrioritizedArray.scala` | Pool/Iterator pattern (13 members) | Internal restructure — may not affect users |
| `screens/SlidingDirection.scala` | DOWN enum value | One direction missing |
| `screens/ScreenFboUtils.scala` | restoreFboStatus, retrieveFboStatus | FBO state management |
| `ecs/ImmutableArray.scala` | equals, hashCode, random, toArray | Collection comparison |
| `controllers/DefaultControllerManager.scala` | Substantially different impl from AbstractControllerManager | Feature-complete but different design |

### Tier 3: Architecture divergences (intentional, not gaps)

These show as "missing" in compare but are deliberate SGE design decisions:

- **JSON system**: SGE uses typed codecs + Json AST instead of LibGDX's reflection-based Json. JsonMatcher, JsonValue, Json inner classes don't apply.
- **HTTP system**: SGE uses SgeHttpClient (sttp-based) instead of LibGDX's Net.HttpRequest/Response. ~27 "missing" from Net.scala are architecture divergence.
- **Collections**: SGE replaces Array→DynamicArray, uses Scala iterators instead of Java iterator inner classes. Iterator/Iterable "missing" across all collection types are intentional.
- **Particle serialization**: SGE replaces individual read/write methods with ParticleEffectCodecs.scala (centralized JSON codec). ~40 "missing" read/write methods across particle values/influencers/renderers.
- **Platform ops**: JNI methods replaced by platform-abstracted ops (BufferOps, Gdx2dOps, ETC1Ops, etc.)

### Tier 4: Noise (not real gaps)

~85% of all raw "missing" members fall here:
- `getX`/`setX` → Scala `x`/`x_=` property (~60% of all raw missing)
- `dispose` → `close` (SGE convention)
- Java type reference names (GdxRuntimeException, Vector3, Matrix4, etc. as import-level names)
- `compareTo` → `compare` (Scala Ordered convention)
- `instance()` → Scala object singleton

---

## Part 9: Missing Tests Worth Porting

| Priority | Test | Original | Missing Methods | Why |
|----------|------|----------|-----------------|-----|
| High | Ashley EntityListenerTests | 7 methods | listener priority, family-scoped listeners | Tests edge cases in entity lifecycle |
| High | Ashley EntityManagerTests | 8 methods | delayed operation ordering, removeAll-and-add | Tests concurrent modification scenarios |
| High | gdx-ai ParallelTest orchestrator | 5 methods | Resume/Join policy variants | Tests BT execution semantics |
| High | gdx-ai IndexedAStarPathFinder | 2 methods | dynamic graph updates during search | Tests live pathfinding |
| Medium | Sort edge cases | 3 methods | null handling, invalid indices | Robustness tests |
| Medium | Ashley FamilyManager/SystemManager | 10 methods | internal edge cases | Component system reliability |
| Medium | simple-graphs ArrayTest + StructuresTest | 3 methods | internal data structures | Graph algorithm correctness |
| Low | BitsTest | 5 methods | bitwise operations | If SGE has Bits equivalent |
| Low | MixedPutRemoveTest | 8 methods | ObjectMap stress | Partially covered by SGE tests |
