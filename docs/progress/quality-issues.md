# Quality Issues

Systemic code quality issues found across the SGE codebase. These should be addressed
during verification and idiomatization passes.

Last updated: 2026-02-27

## Summary

| Issue Type | Files Affected | Total Occurrences | Status |
|-----------|---------------|-------------------|--------|
| Missing license header | 0 | 0 | **Complete** (454 files) |
| `return` keyword usage | 44 | ~125 | In progress (13 files done) |
| Direct `null` checks | 68 | ~233 | In progress (6 files done) |
| `null.asInstanceOf` | 23 | ~69 | Not started |
| Remaining Java syntax | 363 | ~2875 | Not started |
| TODO/FIXME markers | 57 | ~119 | Not started |
| ArrayBuffer→DynamicArray | 0 | 0 | **Complete** (145 files) |

## 1. Missing License Headers — COMPLETE

All 454 Scala files now have proper license headers. Ported files include the original
source path(s), original authors (extracted from `@author` tags in the Java source),
Apache 2.0 license notice, and Scala port copyright. SGE-only files (no Java equivalent)
have a simpler copyright-only header. Merged files (e.g., `Matrices.scala` from
`Matrix3.java` + `Matrix4.java`) list all original sources.

## 2. `return` Keyword Usage (44 files, ~125 occurrences)

All `return` keywords must be replaced with `scala.util.boundary`/`break` patterns.
See [control-flow-guide.md](../contributing/control-flow-guide.md).

**Batch Q1 complete** (13 files, ~70 occurrences removed): Matrices, GlyphLayout,
ShaderProgram, DefaultTextureBinder, Decoder, TimSort, PolygonRegionLoader, BinTree,
DataInput, ParticleEmitter, Vectors, FrameBufferCubemap, BaseAnimationController.

Highest remaining: Sprite (18), Encoder (17), ComparableTimSort (6), GLTexture (6),
Pool (6), Affine2 (4), BitmapFontCache (4), EarClippingTriangulator (4).

Run `just sge-quality return` to see current occurrences.

## 3. Direct `null` Checks (68 files, ~233 occurrences)

Patterns like `== null`, `!= null` should be replaced with `Nullable[A]` methods.
See [nullable-guide.md](../contributing/nullable-guide.md).

**Batch Q2 complete** (6 files converted): FacedCubemapData, NinePatch, BitmapFont,
TextureAtlas, ParticleEmitter, plus cascade fix in Cubemap and BitmapFontLoader.

Note: Many remaining `== null` / `!= null` checks are at Java interop boundaries
(e.g., `reader.readLine()` returns) or array-element nullability. These are intentional
and lower priority than field-level null patterns.

Additionally, 23 files have ~69 `null.asInstanceOf` occurrences used for pool recycling
and `uninitialized` field resets. These are largely intentional JVM patterns.

Run `just sge-quality null` to see current occurrences.

## 4. Remaining Java Syntax (363 files, ~2875 occurrences)

Patterns like `public`, `private`, `protected`, `static`, `void`, `boolean`, `final`,
`abstract class`, `implements`, and `extends ... {` that indicate unconverted Java code.

Run `just sge-quality java_syntax` to see current occurrences.

## 5. TODO/FIXME Markers (57 files, ~119 occurrences)

Files with TODO, FIXME, HACK, or XXX comments that need attention.

Run `just sge-quality todo` to see current occurrences.

## 6. Naming Issues

- **`SdeError.scala`** and **`SdeNativesLoader.scala`** — Still use old "Sde" prefix instead of
  "Sge". The enum inside `SdeError.scala` was renamed to `SgeError` but the filenames were not
  updated.
- **`NativeImputConfiguration.scala`** — Typo: "Imput" should be "Input"
- **`TextinputWrapper.scala`** — Inconsistent casing: "input" should be "Input"

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
