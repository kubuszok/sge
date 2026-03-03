# Audit: sge.maps.tiled.renderers

Audited: 6/6 files | Pass: 6 | Minor: 0 | Major: 0
Last updated: 2026-03-03

---

### BatchTiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/renderers/BatchTiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/renderers/BatchTiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: Abstract class with 4 constructors, 16 methods (setView x2, render x2, renderMapLayer, renderObjects, renderObject, renderImageLayer, getImageLayerColor, getTileLayerColor, beginRender, endRender, close, getMap, setMap, getUnitScale, getBatch, getViewBounds), 7 fields, 1 companion constant (NUM_VERTICES) -- all match Java 1:1.
**Convention changes**: Disposable.dispose() mapped to AutoCloseable.close(). Java return-early in renderMapLayer/renderImageLayer replaced with if/else and pattern match. null checks on TextureRegion replaced with Nullable idiom.
**Issues**: None

---

### OrthogonalTiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/renderers/OrthogonalTiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/renderers/OrthogonalTiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 4 constructors + renderTileLayer override -- matches Java 1:1. Does not override renderImageLayer (inherits from BatchTiledMapRenderer, matching Java).
**Convention changes**: Java null checks on cell/tile replaced with Nullable.foreach chaining. Java `continue` replaced with if/else. Java switch/break replaced with match/case.
**Issues**: None

---

### IsometricTiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/renderers/IsometricTiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/renderers/IsometricTiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 4 constructors, 6 private fields, init(), translateScreenToIso(), renderTileLayer(), renderImageLayer() + private renderRepeatedImage() helper -- all match Java 1:1. The renderRepeatedImage helper was extracted from inline code in the Java renderImageLayer repeating branch (same logic).
**Convention changes**: Java null checks on cell/tile replaced with Nullable.foreach chaining. Java null==region early return replaced with Nullable isEmpty/else. Java switch/break replaced with match/case. uninitialized fields use `scala.compiletime.uninitialized`.
**Issues**: None

---

### IsometricStaggeredTiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/renderers/IsometricStaggeredTiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/renderers/IsometricStaggeredTiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 4 constructors, renderTileLayer(), renderImageLayer() -- all match Java 1:1. Includes halfTileWidth X-offset logic in renderImageLayer (matching Java).
**Convention changes**: Java null checks on cell/tile replaced with Nullable.foreach chaining. Java null==region early return replaced with Nullable isEmpty/else. Java switch/break replaced with match/case. Repeating image logic inlined (matching Java structure).
**Issues**: None

---

### HexagonalTiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/renderers/HexagonalTiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/renderers/HexagonalTiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 4 constructors, 3 private fields (staggerAxisX, staggerIndexEven, hexSideLength), initHex(), renderTileLayer() (staggerAxisX and Y branches), private renderCell(), renderImageLayer() (hex Y-offset), 6 public accessor methods -- all match Java 1:1.
**Convention changes**: Java null checks on map properties replaced with Nullable.foreach/fold. renderCell: Java null checks on cell/tile replaced with Nullable.foreach. `instanceof AnimatedTiledMapTile` return guard replaced with `!isInstanceOf` guard. renderCell only handles rotations==2 (ROTATE_180), matching Java. Java null==region early return replaced with Nullable isEmpty/else.
**Issues**: None

---

### OrthoCachedTiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/renderers/OrthoCachedTiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/renderers/OrthoCachedTiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 3 constructors, 15 protected fields, setView() x2, render() x2, renderObjects(), renderObject(), renderTileLayer(), renderImageLayer(), invalidateCache(), isCached(), setOverCache(), setMaxTileSize(), setBlending(), getSpriteCache(), close(), companion object with tolerance + NUM_VERTICES -- all match Java 1:1.
**Convention changes**: Does not extend BatchTiledMapRenderer (matches Java: standalone TiledMapRenderer + Disposable). Disposable.dispose() mapped to AutoCloseable.close(). Java Gdx.gl replaced with Sge().graphics.gl. Java null checks on cell/tile replaced with Nullable.foreach chaining. Java null==region early return replaced with Nullable isEmpty/else. Java switch/break replaced with match/case. renderImageLayer color uses combinedTint directly (no batch color multiply), matching Java source. Java `map` field is `final`; Scala uses `protected val`.
**Issues**: None
