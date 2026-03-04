# Audit: sge.maps.tiled.objects

Audited: 1/1 files | Pass: 1 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### TiledMapTileMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/objects/TiledMapTileMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/objects/TiledMapTileMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 1 constructor, 3 public vars (tile, flipHorizontally, flipVertically) — all match Java 1:1.
**Renames**: `isFlipHorizontally`/`setFlipHorizontally` -> `var flipHorizontally`, `isFlipVertically`/`setFlipVertically` -> `var flipVertically`, `getTile`/`setTile` -> `var tile`
**Convention changes**: Constructor body uses `locally{}` block for TextureRegion creation and flip. `textureRegion` assigned with `Nullable()` because SGE uses `Nullable[TextureRegion]`.
**Issues**: None
