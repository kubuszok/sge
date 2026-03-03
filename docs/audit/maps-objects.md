# Audit: sge.maps.objects

Audited: 8/8 files | Pass: 8 | Minor: 0 | Major: 0
Last updated: 2026-03-03

---

### CircleMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/CircleMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/CircleMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `getCircle()` — 1:1 match.
**Convention changes**: `circle` field changed from non-final to `val`
**Issues**: None

---

### EllipseMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/EllipseMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/EllipseMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `getEllipse()` — 1:1 match.
**Convention changes**: `ellipse` field changed from non-final to `val`
**Issues**: None

---

### PointMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/PointMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/PointMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `getPoint()` — 1:1 match.
**Issues**: None

---

### PolygonMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/PolygonMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/PolygonMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 3 constructors + `getPolygon()`/`setPolygon()` — 1:1 match.
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

**Completeness**: 3 constructors + `getPolyline()`/`setPolyline()` — 1:1 match.
**Issues**: None

---

### RectangleMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/RectangleMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/RectangleMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `getRectangle()` — 1:1 match.
**Issues**: None

---

### TextMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/TextMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/TextMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors, 12 fields, 24 accessor methods — all ported.
**Issues**: None

---

### TextureMapObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/maps/objects/TextureMapObject.scala` |
| Java source(s) | `com/badlogic/gdx/maps/objects/TextureMapObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors, 8 fields, 16 accessor methods — all ported.
**Convention changes**: Nullable `TextureRegion` properly wrapped as `Nullable[TextureRegion]`
**Issues**: None
