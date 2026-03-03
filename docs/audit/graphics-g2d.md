# Audit: sge.graphics.g2d

Audited: 25/25 files | Pass: 14 | Minor: 9 | Major: 2
Last updated: 2026-03-03

---

### Animation.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/Animation.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/Animation.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `getKeyFrame(2)`, `getKeyFrameIndex`, `getKeyFrames`, `setKeyFrames`, `getPlayMode`, `setPlayMode`, `isAnimationFinished`, `setFrameDuration`, `getFrameDuration`, `getAnimationDuration`. Inner enum `PlayMode` uses Scala 3 enum with helper methods `isLooping`/`isReversed`.
**Renames**: `Array<T>` -> `DynamicArray[? <: T]`; generic `<T>` -> `[T: ClassTag]`
**Convention changes**: Java enum -> Scala 3 enum; `return` eliminated; uses `match` instead of `switch`
**TODOs**: None
**Issues**: None

---

### Batch.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/Batch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/Batch.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 30+ interface methods present. All 20 vertex index constants (`X1..V4`) in companion object. `setShader` takes `Nullable[ShaderProgram]` instead of nullable `ShaderProgram`.
**Renames**: `Disposable` -> `AutoCloseable`
**Convention changes**: Java interface -> Scala trait extending `AutoCloseable`; `dispose()` -> `close()`; null parameter -> `Nullable[ShaderProgram]`
**TODOs**: None
**Issues**: None

---

### BitmapFont.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/BitmapFont.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/BitmapFont.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All main `BitmapFont` methods present: 5 `draw` overloads, `getColor`, `setColor`, `getScaleX/Y`, `getRegion(s)`, `getLineHeight`, `getSpaceXadvance`, `getXHeight`, `getCapHeight`, `getAscent`, `getDescent`, `isFlipped`, `close`, `setFixedWidthGlyphs`, `setUseIntegerPositions`, `usesIntegerPositions`, `getCache`, `getData`, `ownsTexture/setOwnsTexture`, `newFontCache`, `toString`. Inner class `Glyph` fully ported. `BitmapFontData` is a separate top-level class.
**Missing from BitmapFontData**: `setScale(Float,Float)`, `setScale(Float)`, `scale(Float)`, `getImagePath(Int)`, `getImagePaths()`, `getFontFile()`, `isBreakChar(Char)`. The `load` method body is stub ("simplified for now").
**Renames**: `Disposable` -> `AutoCloseable`; `ownsTexture()` -> `hasOwnTexture()`
**Convention changes**: Uses `Nullable` throughout; `dispose()` -> `close()`; `using Sge` context parameter
**TODOs**: BitmapFontData.load() is a stub implementation
**Issues**: BitmapFontData missing 7 public methods from Java source; load() body not implemented

---

### BitmapFontCache.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/BitmapFontCache.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/BitmapFontCache.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods present: `setPosition`, `translate`, `tint`, `setAlphas`, `setColors(5)`, `getColor`, `setColor(2)`, `draw(3)`, `clear`, `setText(5)`, `addText(5)`, `getX`, `getY`, `getFont`, `setUseIntegerPositions`, `usesIntegerPositions`, `getPageCount`, `getVertices(2)`, `getVertexCount`, `getLayouts`.
**Convention changes**: Uses `boundary`/`break` for early returns; `Nullable` for null safety
**TODOs**: None
**Issues**: Flat package declaration `package sge.graphics.g2d` -- should be split: `package sge` / `package graphics` / `package g2d`

---

### CpuSpriteBatch.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/CpuSpriteBatch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/CpuSpriteBatch.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 3 constructors, `flushAndSyncTransformMatrix`, `getTransformMatrix`, `setTransformMatrix(Matrix4)`, `setTransformMatrix(Affine2)`, all 13 `draw` overrides. Companion object helpers for affine math.
**Renames**: None significant
**Convention changes**: Uses `Nullable` for default shader parameter; `using Sge` context parameter
**TODOs**: None
**Issues**: None

---

### DistanceFieldFont.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/DistanceFieldFont.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/DistanceFieldFont.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods present: 7 constructors, `load` override, `newFontCache`, `getDistanceFieldSmoothing`, `setDistanceFieldSmoothing`, `createDistanceFieldShader` (in companion). Inner class `DistanceFieldFontCache` fully ported.
**Renames**: None significant
**Convention changes**: `static` method -> companion object; `using Sge` context parameter
**TODOs**: None
**Issues**: Vertex shader is missing `v_texCoords` assignment line. Java has `v_texCoords = a_texCoord0;` but Scala version omits this line in `createDistanceFieldShader`.

---

### Gdx2DPixmap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/Gdx2DPixmap.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/Gdx2DPixmap.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All instance methods present: 5 constructors, `close`, `clear`, `setPixel`, `getPixel`, `drawLine`, `drawRect`, `drawCircle`, `fillRect`, `fillCircle`, `fillTriangle`, `drawPixmap(2)`, `setBlend`, `setScale`, `getGLInternalFormat`, `getGLFormat`, `getGLType`, `getFormatString`, `getPixels`, `getHeight`, `getWidth`, `getFormat`. Companion object has all constants and `toGlFormat`/`toGlType`.
**Missing**: Static factory methods `newPixmap(InputStream, Int)` and `newPixmap(Int, Int, Int)` from companion object. `getFailureReason` is a stub returning "Unknown error" (Java is a JNI native method).
**Renames**: `Disposable` -> `AutoCloseable`; `dispose()` -> `close()`
**Convention changes**: JNI native methods replaced with stub implementations that return `null`
**TODOs**: All native methods are stubs -- requires platform-specific implementation
**Issues**: (1) Flat package `package sge.graphics.g2d` should be split. (2) Native method stubs return raw `null` (lines 195, 199, 203) without `Nullable` wrapping or `@nowarn`. (3) Missing 2 static factory methods. (4) All JNI stubs need real implementations per platform.

---

### GlyphLayout.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/GlyphLayout.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/GlyphLayout.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All public methods present: 3 constructors, `setText(3)`, `reset`, `toString`. Inner class `GlyphRun` fully ported with `appendRun`, `reset`, `toString`.
**Renames**: `Poolable` kept as-is
**Convention changes**: Uses `DynamicArray` instead of libGDX `Array`/`FloatArray`
**TODOs**: None
**Issues**: (1) Flat package `package sge.graphics.g2d` should be split. (2) Raw `null` usage on lines 70-71 (`lineRun`, `lastGlyph`), 152, 208-209, 275, 336, 343, 349 -- these should use `Nullable[GlyphRun]` and `Nullable[BitmapFont.Glyph]`. This is a significant violation of the no-null convention.

---

### NinePatch.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/NinePatch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/NinePatch.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 9 constructors, all public methods: `draw(2)`, `setColor`, `getColor`, `getLeftWidth/setLeftWidth`, `getRightWidth/setRightWidth`, `getTopHeight/setTopHeight`, `getBottomHeight/setBottomHeight`, `getMiddleWidth/setMiddleWidth`, `getMiddleHeight/setMiddleHeight`, `getTotalWidth`, `getTotalHeight`, `setPadding`, `getPadLeft/setPadLeft`, `getPadRight/setPadRight`, `getPadTop/setPadTop`, `getPadBottom/setPadBottom`, `scale`, `getTexture`. All 9 constants in companion object.
**Renames**: `TextureRegion... patches` -> `TextureRegion*` varargs with `Nullable[TextureRegion]`
**Convention changes**: Uses `Nullable` for optional patch regions
**TODOs**: None
**Issues**: None

---

### ParticleEffect.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/ParticleEffect.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/ParticleEffect.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: constructors, `start`, `reset(3)`, `update`, `draw(2)`, `allowCompletion`, `isComplete`, `setDuration`, `setPosition`, `setFlip`, `flipY`, `getEmitters`, `findEmitter`, `preAllocateParticles`, `save`, `load(3)`, `loadEmitters`, `loadEmitterImages(3)`, `close`, `getBoundingBox`, `scaleEffect(3)`, `setEmittersCleanUpBlendFunction`.
**Renames**: `dispose()` -> `close()`; `Disposable` -> `AutoCloseable`; `findEmitter` returns `Nullable`
**Convention changes**: Uses `Nullable` for null safety; `using Sge` context parameter; `boundary`/`break` for early returns
**TODOs**: `newEmitter` and `loadTexture` presumably defined elsewhere
**Issues**: None

---

### ParticleEffectPool.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/ParticleEffectPool.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/ParticleEffectPool.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods present: `newObject`, `free`. Inner class `PooledEffect` with `free` and `reset`.
**Renames**: None significant
**Convention changes**: `PooledEffect` is in companion object (was inner class in Java). Pool type parameter made explicit.
**TODOs**: None
**Issues**: `PooledEffect.reset()` in Java calls `super.reset()` (Pool's reset), but the Scala version calls `this.reset(resetScaling = true, start = false)` which is different from the Java behavior where Pool.free() triggers `reset()` which just calls `super.reset()`.

---

### ParticleEmitter.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/ParticleEmitter.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/ParticleEmitter.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods present: constructors, `setMaxParticleCount`, `addParticle`, `addParticles`, `update`, `draw(2)`, `start`, `reset(2)`, `setPosition`, `setSprites`, `setSpriteMode`, `preAllocateParticles`, `allowCompletion`/`getAllowCompletion`, getters/setters for all value types, `save`, `load`, `setFlip`, `flipYValues`, `getBoundingBox`, `scaleSize(2)`, `scaleMotion`, `matchSize`, `matchXSize`, `matchYSize`, `matchMotion`. All inner classes: `Particle`, `ParticleValue`, `NumericValue`, `RangedNumericValue`, `ScaledNumericValue`, `IndependentScaledNumericValue`, `GradientColorValue`, `SpawnShapeValue`. Enums: `SpawnShape`, `SpawnEllipseSide`, `SpriteMode`.
**Renames**: `allowCompletion()` -> `allowCompletionMethod()` (avoids clash with `allowCompletion` field); `cleansUpBlendFunction()` getter exists
**Convention changes**: Java enums -> Scala 3 enums; `boundary`/`break` for early returns; `Nullable` for null safety
**TODOs**: None
**Issues**: Raw `null` on line 700: `if (sprites.nonEmpty) sprites.first else null` -- should use `Nullable`

---

### PixmapPacker.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PixmapPacker.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PixmapPacker.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods present: 3 constructors, `sort`, `pack(2)`, `getPages`, `getRect`, `getPage`, `getPageIndex`, `dispose`, `generateTextureAtlas`, `updateTextureAtlas(2)`, `updateTextureRegions`, `updatePageTextures`, getters/setters for all properties. Inner types: `PackStrategy`, `Page`, `Bounds`, `PixmapPackerRectangle`, `GuillotineStrategy`, `GuillotinePage`. Also has `getTransparentColor`/`setTransparentColor`, `getSplitPoint`, `getSplits`, `getPads`.
**Missing**: `SkylineStrategy` and `SkylinePage` classes from Java source not ported (only `GuillotineStrategy` present).
**Renames**: `dispose()` -> also has `close()` via `AutoCloseable`; `Disposable` -> `AutoCloseable`
**Convention changes**: Uses `Nullable` for null safety; `MutableMap` instead of `OrderedMap`; `implicit sge: Sge`
**TODOs**: None
**Issues**: (1) Raw `null` on lines 168-169 (`rect`, `pixmapToDispose`) -- should use `Nullable`. (2) Missing `SkylineStrategy`/`SkylinePage` inner classes.

---

### PixmapPackerIO.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PixmapPackerIO.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PixmapPackerIO.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `save(2)`. Inner types: `ImageFormat` enum with `CIM`/`PNG`, `SaveParameters` class.
**Renames**: None significant
**Convention changes**: Java enum -> Scala 3 enum; `IOException` -> `SgeError.FileWriteError`; `Nullable` for null checks
**TODOs**: None
**Issues**: None

---

### PolygonBatch.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PolygonBatch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PolygonBatch.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 interface methods present: `draw(PolygonRegion,x,y)`, `draw(PolygonRegion,x,y,w,h)`, `draw(PolygonRegion,x,y,originX,originY,w,h,scaleX,scaleY,rotation)`, `draw(Texture,polygonVertices,...)`.
**Renames**: None
**Convention changes**: Java interface -> Scala trait extending `Batch`
**TODOs**: None
**Issues**: None

---

### PolygonRegion.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PolygonRegion.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PolygonRegion.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: constructor, `getVertices`, `getTriangles`, `getTextureCoords`, `getRegion`.
**Renames**: None
**Convention changes**: Constructor parameters are `val`-equivalent through class parameter; fields are `final val` equivalent
**TODOs**: None
**Issues**: None

---

### PolygonRegionLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PolygonRegionLoader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PolygonRegionLoader.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods present: 2 constructors, `load(AssetManager,...)`, `getDependencies`, `load(TextureRegion,FileHandle)`. Inner class `PolygonRegionParameters`.
**Renames**: `GdxRuntimeException` -> `SgeError.FileReadError`
**Convention changes**: Uses `boundary`/`break` for early return; `Nullable` for null safety; `using Sge` context parameter
**TODOs**: `load(AssetManager,...)` throws "not yet implemented" -- requires `AssetManager.get()` and `getDependencies()` methods
**Issues**: `load(AssetManager,...)` is a stub that throws at runtime

---

### PolygonSprite.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PolygonSprite.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PolygonSprite.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 2 constructors, `set`, `setBounds`, `setSize`, `setPosition`, `setX`, `setY`, `translateX`, `translateY`, `translate`, `setColor(2)`, `setOrigin`, `setRotation`, `rotate`, `setScale(2)`, `scale`, `getVertices`, `getBoundingRectangle`, `draw(2)`, all getters. `setRegion`, `getRegion`.
**Renames**: `draw(PolygonSpriteBatch)` -> `draw(PolygonBatch)` (uses trait instead of concrete class)
**Convention changes**: Uses `if (!dirty)` instead of `if (dirty) return`; `Nullable` for null check in `setRegion`
**TODOs**: None
**Issues**: None

---

### PolygonSpriteBatch.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/PolygonSpriteBatch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/PolygonSpriteBatch.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 4 constructors, all `PolygonBatch` and `Batch` interface methods, `flush`, `disableBlending`/`enableBlending`, `setBlendFunction`/`setBlendFunctionSeparate`, all blend getters, `close`, `getProjectionMatrix`/`setProjectionMatrix`, `getTransformMatrix`/`setTransformMatrix`, `setShader`, `getShader`, `isBlendingEnabled`, `isDrawing`. Public fields: `renderCalls`, `totalRenderCalls`, `maxTrianglesInBatch`.
**Renames**: `dispose()` -> `close()`
**Convention changes**: `Nullable` for shader; `using Sge` context parameter; `AutoCloseable`
**TODOs**: None
**Issues**: None

---

### RepeatablePolygonSprite.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/RepeatablePolygonSprite.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/RepeatablePolygonSprite.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods present: `setPolygon(2)`, `draw`, `setColor`, `setPosition`. Private methods: `snapToGrid`, `offset`, `buildVertices`.
**Renames**: None significant
**Convention changes**: Uses `Nullable[TextureRegion]` for region; `Nullable[Array[Float]]` for nullable parts; `DynamicArray` instead of libGDX `Array`
**TODOs**: None
**Issues**: (1) `setPolygon` uses a simplified polygon intersection check instead of `Intersector.intersectPolygons` -- just checks if polygon contains any vertex of tmpPoly, which is less accurate than full polygon intersection. (2) Missing `density` field that exists in Java source.

---

### Sprite.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/Sprite.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/Sprite.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 7 constructors, `set`, `setBounds`, `setSize`, `setPosition`, `setOriginBasedPosition`, `setX`, `setY`, `setCenterX`, `setCenterY`, `setCenter`, `translateX`, `translateY`, `translate`, `setColor(2)`, `setAlpha`, `setPackedColor`, `setOrigin`, `setOriginCenter`, `setRotation`, `getRotation`, `rotate`, `rotate90`, `setScale(2)`, `scale`, `getVertices`, `getBoundingRectangle`, `draw(2)`, all getters, `setRegion` override, `setU/V/U2/V2` overrides, `setFlip`, `flip` override, `scroll` override. Companion object has `VERTEX_SIZE`, `SPRITE_SIZE`, vertex index constants.
**Renames**: None significant
**Convention changes**: Uses `boundary`/`break` for early returns; `Nullable` not needed (no null in API)
**TODOs**: None
**Issues**: None

---

### SpriteBatch.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/SpriteBatch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/SpriteBatch.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 3 constructors, all `Batch` interface methods fully implemented: `begin`, `end`, `setColor(2)`, `getColor`, `setPackedColor`, `getPackedColor`, all 13 `draw` overloads, `flush`, `disableBlending`/`enableBlending`, `setBlendFunction`/`setBlendFunctionSeparate`, blend getters, `close`, `getProjectionMatrix`/`setProjectionMatrix`, `getTransformMatrix`/`setTransformMatrix`, `setShader`, `getShader`, `isBlendingEnabled`, `isDrawing`. `createDefaultShader` in companion.
**Renames**: `dispose()` -> `close()`
**Convention changes**: `Nullable` for shader/texture; `using Sge` context parameter; `AutoCloseable`
**TODOs**: None
**Issues**: None

---

### SpriteCache.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/SpriteCache.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/SpriteCache.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 3 constructors, `setColor(2)`, `getColor`, `setPackedColor`, `getPackedColor`, `beginCache(2)`, `endCache`, `clear`, `add(8 overloads)`, `begin`, `end`, `draw(2)`, `close`, `getProjectionMatrix`/`setProjectionMatrix`, `getTransformMatrix`/`setTransformMatrix`, `setShader`, `getCustomShader`, `isDrawing`. Inner class `Cache`. `createDefaultShader` in companion.
**Renames**: `dispose()` -> `close()`; `getCustomShader` returns `Nullable[ShaderProgram]`
**Convention changes**: `Nullable` throughout; `AutoCloseable`; `using Sge` context parameter
**TODOs**: None
**Issues**: None

---

### TextureAtlas.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/TextureAtlas.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/TextureAtlas.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: 7 constructors, `load`, `addRegion(2)`, `getRegions`, `findRegion(2)`, `findRegions`, `createSprites(2)`, `createSprite(2)`, `createPatch`, `getTextures`, `close`. Inner types: `TextureAtlasData` with `load`, `getPages`, `getRegions`, `readEntry`, `Field`, `Page`, `Region`. `AtlasRegion` with `flip` override, `getRotatedPackedWidth/Height`, `findValue`, `toString`. `AtlasSprite` with all override methods.
**Renames**: `dispose()` -> `close()`; `ObjectSet<Texture>` -> `MutableSet[Texture]`; `@Null` -> `Nullable`
**Convention changes**: Uses `Nullable` throughout; `using Sge` context parameter; `MutableMap`/`MutableSet`
**TODOs**: None
**Issues**: None

---

### TextureRegion.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g2d/TextureRegion.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g2d/TextureRegion.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All constructors present (7). All instance methods: `setRegion(5)`, `getTexture`, `setTexture`, `getU/setU`, `getV/setV`, `getU2/setU2`, `getV2/setV2`, `getRegionX/setRegionX`, `getRegionY/setRegionY`, `getRegionWidth/setRegionWidth`, `getRegionHeight/setRegionHeight`, `flip`, `isFlipX`, `isFlipY`, `scroll`. Companion has `split(Texture,...)`, `createSubRegion`, `splitWithMargins`.
**Missing**: Instance method `split(tileWidth, tileHeight)` -- Java has this as an instance method returning `TextureRegion[][]`. Only the static `split(Texture,...)` is ported (in companion), plus `splitWithMargins` which is not in the original.
**Renames**: None significant
**Convention changes**: Flat package `package sge.graphics.g2d` -- should be split
**TODOs**: None
**Issues**: (1) Flat package declaration. (2) Missing instance `split(Int, Int)` method.
