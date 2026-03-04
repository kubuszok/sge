# Audit: sge.maps.objects

Audited: 8/8 files | Pass: 8 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### CircleMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/CircleMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/CircleMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `val circle` — 1:1 match.
**Renames**: `getCircle()` -> `val circle`
**Convention changes**: `circle` field promoted from non-final to `val`
**Issues**: None

---

### EllipseMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/EllipseMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/EllipseMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `val ellipse` — 1:1 match.
**Renames**: `getEllipse()` -> `val ellipse`
**Convention changes**: `ellipse` field promoted from non-final to `val`
**Issues**: None

---

### PointMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/PointMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/PointMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `val point` — 1:1 match.
**Renames**: `getPoint()` -> `val point`
**Issues**: None

---

### PolygonMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/PolygonMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/PolygonMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 3 constructors + `var polygon` — 1:1 match.
**Renames**: `getPolygon()`/`setPolygon()` -> `var polygon`
**Convention changes**: Idiomatic constructor reordering (no-arg is primary)
**Issues**: None

---

### PolylineMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/PolylineMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/PolylineMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 3 constructors + `var polyline` — 1:1 match.
**Renames**: `getPolyline()`/`setPolyline()` -> `var polyline`
**Issues**: None

---

### RectangleMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/RectangleMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/RectangleMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `val rectangle` — 1:1 match.
**Renames**: `getRectangle()` -> `val rectangle`
**Issues**: None

---

### TextMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/TextMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/TextMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors, 12 public vars, `val rectangle` — all ported.
**Renames**: 16 getter/setter pairs -> public vars; `getRectangle()` -> `val rectangle`
**Issues**: None

---

### TextureMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/TextureMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/TextureMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors, 8 public vars — all ported.
**Renames**: 8 getter/setter pairs -> public vars
**Convention changes**: Nullable `TextureRegion` properly wrapped as `Nullable[TextureRegion]`
**Issues**: None
