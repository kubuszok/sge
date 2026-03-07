# Quality Issues

Systemic code quality issues found across the SGE codebase. These should be addressed
during verification and idiomatization passes.

Last updated: 2026-03-07

## Summary

| Issue Type | Files Affected | Total Occurrences | Status |
|-----------|---------------|-------------------|--------|
| Missing license header | 0 | 0 | **Complete** (454 files) |
| `return` keyword usage | 0 | 0 | **Complete** (54 files, 4 batches + Pixmap) |
| Direct `null` checks | 2 | 2 | **Complete** (only Nullable internals + 1 deferred remain) |
| `null.asInstanceOf` | 4 | 9 | **Mostly complete** (49 removed, 9 deferred) |
| Remaining Java syntax | 2 | 4 | **Triaged** — false positives only (`void` as method name) |
| TODO/FIXME markers | 65 | 93 | **Triaged** (all actionable resolved; ~5 upstream, rest blocked/info) |
| ArrayBuffer→DynamicArray | 0 | 0 | **Complete** (145 files) |

## 1. Missing License Headers — COMPLETE

All 454 Scala files now have proper license headers. Ported files include the original
source path(s), original authors (extracted from `@author` tags in the Java source),
Apache 2.0 license notice, and Scala port copyright. SGE-only files (no Java equivalent)
have a simpler copyright-only header. Merged files (e.g., `Matrices.scala` from
`Matrix3.java` + `Matrix4.java`) list all original sources.

## 2. `return` Keyword Usage (53 files, ~197 occurrences) — COMPLETE

All `return` keywords must be replaced with `scala.util.boundary`/`break` patterns.
See [control-flow-guide.md](../contributing/control-flow-guide.md).

**Batch Q1 complete** (13 files, ~70 occurrences removed): Matrices, GlyphLayout,
ShaderProgram, DefaultTextureBinder, Decoder, TimSort, PolygonRegionLoader, BinTree,
DataInput, ParticleEmitter, Vectors, FrameBufferCubemap, BaseAnimationController.

**Batch Q2 complete** (6 files, ~50 occurrences removed): Intersector (~19),
Sprite (~18), ComparableTimSort (~6), GLTexture (~5), Pool (~6), BitmapFontCache (~4).

**Batch Q3 complete** (4 files, ~12 occurrences removed): BaseTmxMapLoader (6),
BaseTiledMapLoader (4), JsonValue (1), Skin (1).

**Batch Q4 complete** (30 files, ~72 occurrences removed): Affine2 (4),
EarClippingTriangulator (4), GeometryUtils (3), Polyline (3), Quaternion (1),
Polygon (1), WindowedMean (1), DelaunayTriangulator (1), Circle (1),
CumulativeDistribution (1), SpriteBatch (3), PolygonSpriteBatch (2),
PolygonSprite (4), SpriteCache (2), PixmapPacker (2), BitmapFont (1),
Animation (1), ParticleEffect (1), CpuSpriteBatch (1), Texture (2),
TextureArray (1), Cubemap (1), Mesh (1), ShapeRenderer (2), GLFrameBuffer (1),
ImmediateModeRenderer20 (1), VertexBufferObjectWithVAO (1), GLProfiler (2),
Encoder (14), InWindow (3), OutWindow (1), Timer (3), BinaryHeap (2),
BaseTmjMapLoader (7), TmjMapLoader (1).

All `return` keywords have been replaced with `boundary`/`break` patterns or
`if`/`else` restructuring. Verified: `rg '\breturn\b'` finds only `@return`
doc tags and error message strings.

## 3. Direct `null` Checks (68 files, ~233 occurrences)

Patterns like `== null`, `!= null` should be replaced with `Nullable[A]` methods.
See [nullable-guide.md](../contributing/nullable-guide.md).

**Batch Q2 complete** (6 files converted): FacedCubemapData, NinePatch, BitmapFont,
TextureAtlas, ParticleEmitter, plus cascade fix in Cubemap and BitmapFontLoader.

**Batch N1 complete** (6 files, ~44 null checks removed): DirectionalLight, PointLight,
SpotLight (parameter guards + equals), SpriteBatch, PolygonSpriteBatch, SpriteCache
(field init + shader state + constructor params). Cascade updates in Batch trait
(setShader signature) and CpuSpriteBatch (constructor param).

**Batch N2 complete** (3 files, ~10 null checks removed): ModelInstance (constructor
param null-or-default + null-or-empty guards), BaseAnimationController (applyAnimation
null dispatch + applyAnimations param guards), AnimationController (obtain/obtainByName
param guards). 2 deferred checks in AnimationDesc (uninitialized field guards).

**Batch N3 complete** (113 files, ~150+ null checks removed): Automated Scalafix +
manual conversion across graphics, maps, utils, math, assets, scene2d packages.

**Batch N4 complete** (36 files, ~120+ null checks removed): Deep conversion of
BaseShader (14 setter methods + init/render), G3dModelLoader (50 JSON null patterns),
tiled map loaders, BitmapFont, GlyphLayout, ParticleEmitter, PixmapPacker, Skin,
DefaultShader, MeshBuilder, TextureAtlas, FileHandles, AnimationController, Select.

**Batch N5 complete** (28 files, ~27 null checks removed): Final sweep of ObjLoader,
Gdx2DPixmap, compression utilities, DynamicArray, DefaultTextureBinder, Mesh,
SpriteBatch variants, light attributes, vertex/instance buffer objects.

All `== null` / `!= null` checks eliminated. Only comments and `Nullable` internals
remain. Run `just sge-quality null` to verify.

### `null.asInstanceOf` elimination

Reduced from 58 occurrences across 23 files to 9 occurrences across 4 files.

**Groups completed** (49 occurrences removed across 18 files):
- **Group A** (10 occ, 6 files): Try-finally resource vars → move creation before try,
  var→val. FileHandles (4), ETC1 (2), PixmapIO (2), ParticleEffect (1), KTXTextureData (1).
- **Group B** (9 occ, 1 file): GlyphLayout local vars → bare `null`.
- **Group C** (5 occ, 1 file): ObjLoader local vars → bare `null` + conditional assignment.
- **Group D** (4 occ, 2 files): Added `ObjectMap.getUnsafe` for guaranteed-key lookups in
  OrderedMap.
- **Group E** (3 occ, 3 files): Added `ObjectChannel.clearSlot` for particle influencers
  (ModelInfluencer, ParticleControllerInfluencer).
- **Group F** (3 occ, 1 file): Stage.TouchFocus pool reset → bare `null`.
- **Group G** (5 occ, 1 file): DynamicArray snapshot/recycled → bare `null`.
- **Group H** (2 occ, 2 files): DepthShader and ModelInstanceRenderer → throw on missing
  required values.
- **Group I** (2 occ, 1 file): PixmapPacker local vars → bare `null`.
- **Group J** (3 occ, 3 files): MeshPart pool reset → bare `null` (ModelCache, ModelBatch,
  RenderableShapeBuilder).
- **Group K** (2 occ, 1 file): Event getTarget/getListenerActor → `getOrElse(null)`.
- **Group L** (1 occ, 1 file): ModelCache sort camera → `getOrElse(null)`.
- **PixmapPacker stub** (1 occ): `null.asInstanceOf[TextureAtlas]` → `throw NotImplementedError`.

**Deferred** (9 occurrences, 4 files):
- ModelLoader.scala (5): Abstract method `loadModelData(fh, P)` — changing P to Nullable
  cascades to ObjLoader, G3dModelLoader, AssetManager.
- BaseShader.scala (1): Setter interface `set(shader, id, null, null)` — affects all
  shader implementations.
- TimSort.scala (2): Generic type params `Array[T]`/`Ordering[T]` for GC-assist nulling.
- Nullable.scala (1): Internal `orNull` implementation — intentional.

## 4. Remaining Java Syntax (2 files, 4 occurrences) — TRIAGED

Scan now targets Java-exclusive keywords only (`public`, `static`, `void`, `boolean`,
`implements`). Previously included `private`, `protected`, `final` which are valid Scala
keywords, causing ~3140 false positives across 368 files.

After fixing the scan, 4 remaining matches are `void` used as a valid Scala method name
in `Eval.scala` and `Resource.scala` — no code changes needed.

Run `just sge-quality java_syntax` to see current occurrences.

## 5. TODO/FIXME Markers (65 files, 93 occurrences) — TRIAGED

Categorized as of 2026-03-07:

| Category | Markers | Files | Action |
|----------|---------|-------|--------|
| Typed GL enums | 0 | 0 | **Complete** (14 opaque types in GLEnum.scala, ~147 consumer files) |
| Opaque Pixels | 0 | 0 | **Complete** (opaque Pixels type, ~151 files updated) |
| Input.Keys/Buttons opaque type | 0 | 0 | **Complete** (opaque Key/Button types, ~35 consumer files) |
| Upstream FIXMEs | ~5 | ~5 | Preserve (inherited from LibGDX source) |
| Pool.Poolable type class | ~5 | ~5 | Blocked (design decision) |
| Color immutability | ~4 | ~4 | Blocked (future) |
| Dependencies (scribe, scala-java-time, Gears) | ~3 | ~3 | Blocked (future) |
| Other (ComparableTimSort, Show, JsonReader) | ~5 | ~5 | Informational |
| Java-style getters/setters | 0 | 0 | **Complete** |
| Actionable (non-getter) | 0 | 0 | **All resolved** |

**Java-style getter/setter cleanup — COMPLETE** (3 batches):
- **Batch 1** (3 files): Image, ParticleEffectActor, ButtonGroup + callers (CheckBox,
  ImageButton, ImageTextButton)
- **Batch 2** (16 files): Window, Dialog, Touchpad, Slider, Button, ProgressBar, Label,
  TextArea, Tooltip, SplitPane, VerticalGroup, TextButton, ImageTextButton, ButtonGroup,
  ImageButton, TextTooltip
- **Retained** (7 files): Container, Table (fluent builder pattern); ScrollPane, TextField
  (complex state); SelectBox, SgeList, Tree (validation/events logic)

**Resolved actionable TODOs** (10 markers across 6 files):
- `ComparableTimSort.scala` (2): implemented `mergeLo`/`mergeHi`/`ensureCapacity`
- `DefaultTextureBinder.scala` (2): reclassified as profiling stats (used by public API)
- `Viewport.scala` (1): replaced field mutation with `.set()` call
- `BufferUtils.scala` (2): explained JNI bridge limitation
- `ParticleEffectLoader.scala` (2): reclassified as blocked (needs Json framework)
- `SgeNativesLoader.scala` (1): reclassified as blocked (needs native loading strategy)

**Resolved blocked TODOs:**
- ShapeRenderer infrastructure: 22 markers resolved (ShapeRenderer full implementation,
  Actor/Group/Table/Container/ScrollPane/HorizontalGroup/VerticalGroup debug drawing)
- Named context parameter: 19 markers resolved (14 files converted to anonymous
  `(using Sge)` + 5 flat package declarations fixed)
- TextField.DefaultOnscreenKeyboard: 2 markers resolved (added `(using Sge)` to methods)
- Button.requestRendering: 1 marker resolved (already working)

Run `just sge-quality todo` to see current occurrences.

## 6. Naming Issues — COMPLETE

All naming issues have been resolved:
- `SdeError.scala` → `SgeError.scala`, `SdeNativesLoader.scala` → `SgeNativesLoader.scala`
- `NativeImputConfiguration.scala` → `NativeInputConfiguration.scala`
- `TextinputWrapper.scala` → `TextInputWrapper.scala`

## 7. ArrayBuffer→DynamicArray Migration — COMPLETE

All 145 files using `scala.collection.mutable.ArrayBuffer` have been migrated to
`sge.utils.DynamicArray`. DynamicArray provides unboxed primitive arrays via `MkArray`,
snapshot (copy-on-write) iteration, and ordered/unordered removal modes — matching
LibGDX's custom `Array<T>` semantics that ArrayBuffer lacked.

**API additions to DynamicArray**: `foreach`, `iterator`, `exists`, `find`, `count`,
`forall`, `indexWhere`, `+=`, `-=`, `--=`, `addAll(Iterable)` — enabling mechanical
migration from ArrayBuffer patterns.

**MkArray additions**: `mkNullable` given for `DynamicArray[Nullable[A]]` support.

**DynamicArray additions**: `wrapRefUnchecked` factory for generic types with `ClassTag`
but not `MkArray`.

Files migrated by area: math (7), utils (7), g2d (13), g3d core (9), g3d/attributes (3),
g3d/decals (6), g3d/loader (2), g3d/model (3), g3d/model/data (4), g3d/particles (7),
g3d/particles/batches (4), g3d/particles/influencers (4), g3d/shaders (2), g3d/utils (7),
g3d/utils/shapebuilders (1), glutils (4), scene2d (3), scene2d/actions (2),
scene2d/ui (14), scene2d/utils (4), maps (2), maps/tiled (12), assets (2),
assets/loaders (13), root (2), graphics (6), input (1).

## 8. Missing Implementations

- **`Nullable.scala`**: `isDefined` and `isEmpty` methods are implemented and working.
