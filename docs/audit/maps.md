# Audit: sge.maps

Audited: 9/9 files | Pass: 9 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### ImageResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/ImageResolver.scala` |
| Java source(s) | `com/badlogic/gdx/maps/ImageResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: 1 trait + 3 companion object classes (DirectImageResolver, AssetManagerImageResolver, TextureAtlasImageResolver) -- 1:1 match.
**Convention changes**: Java interface -> Scala trait; static inner classes -> companion object classes; ObjectMap -> mutable.Map; return type Nullable[TextureRegion] instead of nullable TextureRegion.
**Idiom**: DirectImageResolver defensively wraps Option.fold for null-safety; TextureAtlasImageResolver maps AtlasRegion to TextureRegion via supertype.
**Issues**: None

---

### Map.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/Map.scala` |
| Java source(s) | `com/badlogic/gdx/maps/Map.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 fields + `close()` -- 1:1 match.
**Convention changes**: Disposable -> AutoCloseable; dispose() -> close(); getLayers/getProperties -> public vals.
**Issues**: None

---

### MapGroupLayer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapGroupLayer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapGroupLayer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 1 field + `getLayers()` + `invalidateRenderOffset()` override -- 1:1 match.
**Convention changes**: for loop -> while loop; `layers.size()` -> `layers.size`.
**Issues**: None

---

### MapLayer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapLayer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapLayer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 14 fields, 14 public methods, 1 protected method -- all ported.
**Convention changes**: null parent -> Nullable[MapLayer]; GdxRuntimeException -> SgeError.InvalidInput; null checks -> Nullable.fold/foreach; no-logic getters/setters (name, visible, objects, properties, parallaxX, parallaxY) -> public vars/vals; opacity backing field renamed to `_opacity` (getter has parent multiplication logic).
**Idiom**: getOpacity and getCombinedTintColor use Nullable.fold for parent-conditional logic; setParent uses Nullable.foreach for self-check; calculateRenderOffsets uses Nullable.fold.
**Issues**: None

---

### MapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 5 fields -- 1:1 match.
**Convention changes**: All fields public vars/vals (no-logic getters/setters removed).
**Issues**: None

---

### MapRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: 4 abstract methods (setView x2, render x2) -- 1:1 match.
**Convention changes**: Java interface -> Scala trait; `int[]` -> `Array[Int]`.
**Issues**: None

---

### MapProperties.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapProperties.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapProperties.java` |
| Status | pass |
| Tested | No |

**Completeness**: 11 methods + equals/hashCode/toString -- all ported.
**Convention changes**: ObjectMap -> mutable.HashMap; Object -> Any; containsKey uses .contains.
**Idiom**: `get(key, defaultValue, clazz)` uses Nullable(obj).fold; equals uses pattern match.
**Issues**: None (raw null in `get(key)` is intentional Java-interop boundary, documented in code)

---

### MapObjects.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapObjects.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapObjects.java` |
| Status | pass |
| Tested | No |

**Completeness**: 10 methods + iterator -- all ported.
**Convention changes**: Array -> DynamicArray; ClassReflection.isInstance -> clazz.isInstance; implements Iterable -> extends Iterable; for loop -> while loop.
**Idiom**: get(name) returns Nullable[MapObject] via boundary/break; getIndex(name) uses Nullable.fold(-1)(getIndex).
**Issues**: None

---

### MapLayers.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/MapLayers.scala` |
| Java source(s) | `com/badlogic/gdx/maps/MapLayers.java` |
| Status | pass |
| Tested | No |

**Completeness**: 11 methods + iterator -- all ported.
**Convention changes**: Array -> DynamicArray; ClassReflection.isInstance -> clazz.isInstance; implements Iterable -> extends Iterable; for loop -> while loop; size() -> override def size.
**Idiom**: get(name) returns Nullable[MapLayer] via boundary/break; getIndex(name) uses Nullable.fold(-1)(getIndex).
**Issues**: None
