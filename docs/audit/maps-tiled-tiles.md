# Audit: sge.maps.tiled.tiles

Audited: 2/2 files | Pass: 2 | Minor: 0 | Major: 0
Last updated: 2026-03-03

---

### StaticTiledMapTile.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/tiles/StaticTiledMapTile.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/tiles/StaticTiledMapTile.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors (primary TextureRegion + copy constructor), 7 fields (id, blendMode, properties, objects, textureRegion, offsetX, offsetY), 14 accessor methods (get/set for all TiledMapTile interface methods) -- all match Java 1:1.
**Convention changes**: Java null-initialized `properties`/`objects` replaced with `Nullable.empty`. getProperties/getObjects use Nullable lazy-init pattern with isEmpty check (matches Java null-check-then-create). Copy constructor uses `_properties.foreach` instead of Java null check.
**Issues**: None

---

### AnimatedTiledMapTile.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/tiles/AnimatedTiledMapTile.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/tiles/AnimatedTiledMapTile.java` |
| Status | pass |
| Tested | No |

**Completeness**: Private primary constructor + 2 public auxiliary constructors, 6 fields (id, blendMode, properties, objects, frameTiles, animationIntervals, loopDuration), 18 methods (getId, setId, getBlendMode, setBlendMode, getCurrentFrameIndex, getCurrentFrame, getTextureRegion, setTextureRegion, getOffsetX, setOffsetX, getOffsetY, setOffsetY, getAnimationIntervals, setAnimationIntervals, getProperties, getObjects, getFrameTiles + companion updateAnimationBaseTime) -- all match Java 1:1.
**Convention changes**: `GdxRuntimeException` replaced with `SgeError.GraphicsError`. Java `return` in loop (getCurrentFrameIndex) replaced with `boundary`/`break`. Java `Array<StaticTiledMapTile>` replaced with `DynamicArray[StaticTiledMapTile]`. Java `IntArray` replaced with `Array[Int]`. Java null-initialized `properties`/`objects` replaced with `Nullable.empty`. Companion object holds static fields and updateAnimationBaseTime.
**Issues**: None
