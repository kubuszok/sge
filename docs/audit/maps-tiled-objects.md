# Audit: sge.maps.tiled.objects

Audited: 1/1 files | Pass: 1 | Minor: 0 | Major: 0
Last updated: 2026-03-03

---

### TiledMapTileMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/objects/TiledMapTileMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/objects/TiledMapTileMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 1 constructor, 3 fields (tile, flipHorizontally, flipVertically), 6 accessor methods (isFlipHorizontally, setFlipHorizontally, isFlipVertically, setFlipVertically, getTile, setTile) -- all match Java 1:1.
**Convention changes**: Constructor body uses `locally{}` block for TextureRegion creation and flip (matches Java constructor body). `setTextureRegion` called with `Nullable(textureRegion)` because SGE's `TextureMapObject.setTextureRegion` accepts `Nullable[TextureRegion]` (Java version accepts nullable TextureRegion implicitly).
**Issues**: None
