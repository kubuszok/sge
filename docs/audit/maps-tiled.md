# Audit: sge.maps.tiled

Audited: 16/16 files | Pass: 15 | Minor: 1 | Major: 0
Last updated: 2026-03-04

---

### TiledMapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 interface methods (renderObjects, renderObject, renderTileLayer, renderImageLayer) match Java 1:1.
**Convention changes**: Java `interface` -> Scala `trait`. Split package used.
**Issues**: None

---

### TiledMapTile.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapTile.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapTile.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 12 interface methods (getId, setId, getBlendMode, setBlendMode, getTextureRegion, setTextureRegion, getOffsetX, setOffsetX, getOffsetY, setOffsetY, getProperties, getObjects) match Java 1:1. Inner BlendMode enum with NONE and ALPHA cases matches.
**Convention changes**: Java `interface` -> Scala `trait`. Java inner `enum` -> Scala 3 `enum` in companion object.
**Issues**: None

---

### TiledMapTileLayer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapTileLayer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapTileLayer.java` |
| Status | pass |
| Tested | No |

**Completeness**: Constructor parameters, cells array, getWidth/getHeight/getTileWidth/getTileHeight/getCell/setCell all match Java 1:1. Inner Cell class: 4 public vars (tile, flipHorizontally, flipVertically, rotation), 4 rotation constants (ROTATE_0/90/180/270) -- all match.
**Renames**: Cell: getTile/setTile → var tile, getFlipHorizontally/setFlipHorizontally → var flipHorizontally, getFlipVertically/setFlipVertically → var flipVertically, getRotation/setRotation → var rotation
**Convention changes**: Java null returns from getCell -> `Nullable.empty`. Cell.tile is `Nullable[TiledMapTile]`. Java `static class Cell` -> `object TiledMapTileLayer { class Cell }` with companion `object Cell` for constants.
**Issues**: None

---

### TiledMapTileSet.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapTileSet.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapTileSet.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: var name, val properties, getTile, iterator, putTile, removeTile, size.
**Renames**: getName/setName → var name, getProperties → val properties
**Convention changes**: Java `IntMap<TiledMapTile>` -> `scala.collection.mutable.HashMap[Int, TiledMapTile]`. getTile returns `Nullable[TiledMapTile]` (Java returns null). Extends `Iterable[TiledMapTile]` (Java implements `Iterable`).
**Issues**: None

---

### TiledMapTileSets.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapTileSets.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapTileSets.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 methods (getTileSet(int), getTileSet(String), addTileSet, removeTileSet(int), removeTileSet(TiledMapTileSet), getTile, iterator) match Java 1:1.
**Convention changes**: Java `Array` -> `DynamicArray`. Java null returns -> `Nullable[A]`. `boundary`/`break` used for early returns in getTileSet(String) and getTile(int). Backward iteration in getTile preserved faithfully.
**Issues**: None

---

### TiledMap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMap.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMap.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods (getTileSets, setOwnedResources, close/dispose) match Java 1:1.
**Convention changes**: Java `dispose()` -> `close()` (AutoCloseable convention). Java `Array<? extends Disposable>` -> `DynamicArray[AutoCloseable]`. ownedResources: Java null -> `Nullable[DynamicArray[AutoCloseable]]`.
**Issues**: None

---

### TiledMapImageLayer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapImageLayer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapImageLayer.java` |
| Status | pass |
| Tested | No |

**Completeness**: Constructor with 5 public vars (region, x, y, repeatX, repeatY), private checkTransparencySupport/formatHasAlpha helpers, supportsTransparency method -- all match Java 1:1.
**Renames**: getTextureRegion/setTextureRegion → var region, getX/setX → var x, getY/setY → var y, isRepeatX/setRepeatX → var repeatX, isRepeatY/setRepeatY → var repeatY
**Convention changes**: Java `switch` -> Scala `match` in formatHasAlpha. Private field `_supportsTransparency` instead of `supportsTransparency` to avoid name clash with public method.
**Issues**: None

---

### BaseTiledMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/BaseTiledMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/BaseTiledMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public/protected methods (getDependencyAssetDescriptors, loadTiledMap, getIdToObject, castProperty, createTileLayerCell, addStaticTiledMapTile, loadObjectProperty, loadBasicProperty, loadProjectFile, loadJsonClassProperties, tiledColorToLibGDXColor, loadMapPropertiesClassDefaults), companion object statics (FLAG_FLIP_*, MASK_CLEAR, unsignedByteToInt, getRelativeFileHandle, tiledColorToLibGDXColor), and inner types (Parameters, ProjectClassMember) all present.
**Renames**: Cell setters in createTileLayerCell → direct var assignments
**Convention changes**: Java `IntMap<MapObject>` -> `mutable.HashMap[Int, MapObject]`. Java `Array<Runnable>` -> `DynamicArray[() => Unit]`. Java null fields -> `Nullable[A]`. Java `GdxRuntimeException` -> `IllegalArgumentException`/`IllegalStateException`. Java `StringTokenizer` -> `path.split("[/\\\\]+")`.
**Issues**: None (loadProjectFile bug fixed in prior commit; misplaced Javadoc is cosmetic)

---

### BaseTmxMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/BaseTmxMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/BaseTmxMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: loadTiledMap, loadLayer, loadLayerGroup, loadTileLayer, loadObjectGroup, loadImageLayer, loadBasicLayerInfo, loadObject (3 overloads), resolveTemplateObject, cloneElementShallow, mergeProperties, mergeParentElementWithTemplate, loadProperties, loadClassProperties, getPropertyByName, loadTileSet, addStaticTiles (abstract), addTileProperties, addTileObjectGroup, createAnimatedTile. Companion object getTileIds.
**Convention changes**: Java `return null` in getPropertyByName -> `null // scalastyle:ignore` (Java interop boundary). Java `XmlReader.Element` returns -> `Nullable[XmlReader.Element]`. `boundary`/`break` used extensively. Java `Base64Coder.decode` -> `java.util.Base64.getDecoder.decode`. `runOnEndOfLoadTiled = null` after use matches Java.
**Issues**: None -- the null usages are documented with `// scalastyle:ignore` comments and are at Java interop boundaries.

---

### BaseTmjMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/BaseTmjMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/BaseTmjMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: loadTiledMap, loadLayer, loadLayerGroup, loadTileLayer, loadObjectGroup, loadImageLayer, loadBasicLayerInfo, loadObject (3 overloads), resolveTemplateObject, mergeJsonObject, cloneElementShallow, mergeJsonProperties, mergeParentElementWithTemplate, loadProperties, loadTileSet, addStaticTiles (abstract), addTileProperties, addTileObjectGroup, createAnimatedTile, deepCopyJsonValue. Companion object getTileIds.
**Convention changes**: Java `new JsonValue(src)` copy constructor -> `deepCopyJsonValue(src)` (SGE JsonValue has no copy constructor). Java `child.remove()` -> `child.removeFromParent()`. `runOnEndOfLoadTiled = null` after use matches Java.
**Issues**: None

---

### TideMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TideMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TideMapLoader.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods present: load(String), load(AssetManager,...), getDependencies, loadMap, loadTileSheets, loadTileSheet, loadLayer, loadProperties, getRelativeFileHandle. Inner Parameters class present.
**Convention changes**: Java `ObjectMap<String,Texture>` -> `mutable.HashMap[String,Texture]`. Java `map.setOwnedResources(textures.values().toArray())` -> DynamicArray construction. Constructor requires `(using Sge)`. `currentTileSet` var uses `null // scalastyle:ignore` (Tide format's interleaved TileSheet/Static/Animated requires mutable state).
**Issues**:
- **Minor**: `loadTileSheet` skips reading the `Description` child element that Java reads (`element.getChildByName("Description").getText()`). While Java never uses the value for anything, the field is read. Harmless omission.
- **Minor**: `loadTileSheet` wraps tile creation in `texture.foreach { }` for null safety -- in Java, `imageResolver.getImage()` returns a raw `TextureRegion` (potentially null, but not checked). The Scala approach is safer but means no tiles are created if the image resolver returns `Nullable.empty`, while Java would NPE.

---

### TmjMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TmjMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TmjMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: load(String), load(String, Parameters), loadAsync, loadSync, getDependencyAssetDescriptors, getDependencyFileHandles, collectImageLayerFileHandles, getTileSetDependencyFileHandle (2 overloads), addStaticTiles.
**Convention changes**: Java `ObjectMap<String,Texture>` -> `mutable.HashMap[String,Texture]`. Java `map.setOwnedResources(textures.values().toArray())` -> DynamicArray construction. Constructor requires `(using Sge)`.
**Issues**: None

---

### TmxMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TmxMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TmxMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: load(String), load(String, Parameters), loadAsync, loadSync, getDependencyAssetDescriptors, getDependencyFileHandles, getTileSetDependencyFileHandle (2 overloads), addStaticTiles.
**Convention changes**: Java `ObjectMap<String,Texture>` -> `mutable.HashMap[String,Texture]`. Java `map.setOwnedResources(textures.values().toArray())` -> DynamicArray construction. Constructor requires `(using Sge)`.
**Issues**: None

---

### AtlasTmjMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/AtlasTmjMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/AtlasTmjMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: load(String), load(String, Parameters), loadAsync, loadSync, getDependencyAssetDescriptors, addStaticTiles, getAtlasFileHandle, setTextureFilters. Companion object: AtlasResolver trait, DirectAtlasResolver, AssetManagerAtlasResolver, parseRegionName -- all match.
**Convention changes**: Java inner `interface AtlasResolver` + inner classes -> companion object `trait AtlasResolver` + classes. Java `AtlasRegion` return -> `Nullable[TextureRegion]` via `.map`. Constructor requires `(using Sge)`.
**Issues**: None

---

### AtlasTmxMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/AtlasTmxMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/AtlasTmxMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: load(String), load(String, Parameters), loadAsync, loadSync, getDependencyAssetDescriptors, addStaticTiles, getAtlasFileHandle, setTextureFilters. Companion object: AtlasResolver trait, DirectAtlasResolver, AssetManagerAtlasResolver -- all match.
**Convention changes**: Java inner `interface AtlasResolver` + inner classes -> companion object `trait AtlasResolver` + classes. Java `parseRegionName` static method -> reuses `AtlasTmjMapLoader.parseRegionName` (good DRY improvement over Java which duplicates). Constructor requires `(using Sge)`.
**Issues**: None

---

### TiledMapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/tiled/TiledMapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/maps/tiled/TiledMapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods match Java 1:1: load(String), load(String, Parameters), getDependencies, loadAsync, loadSync, usesAtlas. All 6 private loader/reader fields match.
**Convention changes**: Java `parameter == null` check -> `Nullable(param).isEmpty`. `usesAtlas` uses `boundary`/`break` for early return. Constructor requires `(using Sge)`.
**Issues**: None
