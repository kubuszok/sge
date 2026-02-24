# Quality Issues

Systemic code quality issues found across the SGE codebase. These should be addressed
during verification and idiomatization passes.

Last updated: 2026-02-24

## Summary

| Issue Type | Files Affected | Total Occurrences | Status |
|-----------|---------------|-------------------|--------|
| Missing license header | 0 | 0 | **Complete** (392/392 files) |
| `return` keyword usage | 128 | 593 | 9 files fixed |
| Direct `null` checks | 124 | 596 | 9 files fixed |
| Remaining Java syntax | ~278 | ~2343 | Not started |
| TODO/FIXME markers | 51 | ~131 | Not started |

## 1. Missing License Headers — COMPLETE

All 392 Scala files now have proper license headers. Ported files include the original
source path(s), original authors (extracted from `@author` tags in the Java source),
Apache 2.0 license notice, and Scala port copyright. SGE-only files (no Java equivalent)
have a simpler copyright-only header. Merged files (e.g., `Matrices.scala` from
`Matrix3.java` + `Matrix4.java`) list all original sources.

## 2. `return` Keyword Usage (135 files)

All `return` keywords must be replaced with `scala.util.boundary`/`break` patterns.
See [control-flow-guide.md](../contributing/control-flow-guide.md).

**Top offenders** (>20 occurrences):
- `sge/math/Matrices.scala` — 112
- `sge/math/Vectors.scala` — 62
- `sge/math/Quaternion.scala` — 48
- `sge/math/Affine2.scala` — 47
- `sge/Graphics.scala` — 41
- `sge/math/Rectangle.scala` — 39
- `sge/Input.scala` — 38
- `sge/math/MathUtils.scala` — 35
- `sge/graphics/glutils/ShaderProgram.scala` — 27
- `sge/graphics/g2d/ParticleEmitter.scala` — 25
- `sge/graphics/Color.scala` — 25
- `sge/math/collision/BoundingBox.scala` — 22
- `sge/graphics/Mesh.scala` — 21
- `sge/graphics/g2d/Sprite.scala` — 21
- `sge/graphics/GLTexture.scala` — 20
- `sge/math/GridPoints.scala` — 20

## 3. Direct `null` Checks (81 files)

Patterns like `== null`, `!= null`, `null.asInstanceOf` should be replaced with
`Nullable[A]` methods. See [nullable-guide.md](../contributing/nullable-guide.md).

**Top offenders** (>10 occurrences):
- `sge/graphics/g2d/NinePatch.scala` — 30
- `sge/graphics/glutils/FacedCubemapData.scala` — 24
- `sge/graphics/g2d/ParticleEmitter.scala` — 18
- `sge/graphics/g2d/BitmapFont.scala` — 12
- `sge/assets/loaders/ShaderProgramLoader.scala` — 11
- `sge/files/FileHandles.scala` — 10
- `sge/graphics/g2d/SpriteCache.scala` — 10
- `sge/graphics/g2d/PixmapPacker.scala` — 9
- `sge/net/NetJavaImpl.scala` — 9
- `sge/graphics/g2d/SpriteBatch.scala` — 9
- `sge/graphics/g2d/TextureAtlas.scala` — 8
- `sge/graphics/g2d/PolygonSpriteBatch.scala` — 8

## 4. Remaining Java Syntax (~197 files)

Patterns like `public`, `private`, `protected`, `static`, `void`, `boolean`, `final`,
`abstract class`, `implements`, and `extends ... {` that indicate unconverted Java code.

**Top offenders** (>20 occurrences):
- `sge/Input.scala` — 204
- `sge/graphics/g2d/ParticleEmitter.scala` — 97
- `sge/graphics/GL32.scala` — 77
- `sge/graphics/glutils/ShaderProgram.scala` — 41
- `sge/graphics/glutils/GLFrameBuffer.scala` — 37
- `sge/graphics/g2d/NinePatch.scala` — 35
- `sge/input/GestureDetector.scala` — 29
- `sge/graphics/PixmapIO.scala` — 26
- `sge/graphics/g2d/Gdx2DPixmap.scala` — 26
- `sge/utils/TimSort.scala` — 26
- `sge/graphics/g2d/PolygonSpriteBatch.scala` — 25
- `sge/utils/ComparableTimSort.scala` — 24
- `sge/graphics/VertexAttributes.scala` — 23

## 5. TODO/FIXME Markers (30 files)

Files with TODO, FIXME, HACK, or XXX comments that need attention.

**Files with multiple markers:**
- `sge/graphics/glutils/ShapeRenderer.scala` — 15
- `sge/graphics/glutils/GLFrameBuffer.scala` — 5
- `sge/graphics/profiling/GLProfiler.scala` — 5
- `sge/net/HttpRequestBuilder.scala` — 4
- `sge/graphics/Cubemap.scala` — 3
- `sge/math/Intersector.scala` — 3
- `sge/utils/Timer.scala` — 3

## 6. Naming Issues

- **`SdeError.scala`** and **`SdeNativesLoader.scala`** — Still use old "Sde" prefix instead of
  "Sge". The enum inside `SdeError.scala` was renamed to `SgeError` but the filenames were not
  updated.
- **`NativeImputConfiguration.scala`** — Typo: "Imput" should be "Input"
- **`TextinputWrapper.scala`** — Inconsistent casing: "input" should be "Input"

## 7. Missing Implementations

- **`Nullable.scala`**: `isDefined` and `isEmpty` methods are referenced in documentation
  but may not be implemented yet. Verify and add if missing.
