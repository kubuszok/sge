# Audit: sge.math.collision

Audited: 5/5 files | Pass: 3 | Minor: 2 | Major: 0
Last updated: 2026-03-04

---

### Ray.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/Ray.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/Ray.java` |
| Status | minor_issues |
| Tested | Yes — `sge/math/collision/CollisionTest.scala` (indirect) |

**Completeness**: All Java public methods present (cpy, getEndPoint, mul, set x3, toString, equals, hashCode).
**Renames**: None
**Convention changes**: `Serializable` dropped; static `tmp` -> local allocation in `mul()`; pattern-match `equals`
**TODOs**: None
**Issues**:
- `minor`: Primary constructor stores Vector3 refs directly; Java copies and normalizes direction
- `minor`: 3-arg secondary constructor (with normalize flag) is an SGE addition

---

### OrientedBoundingBox.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/OrientedBoundingBox.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/OrientedBoundingBox.java` |
| Status | pass |
| Tested | Yes — `sge/math/collision/CollisionTest.scala` |

**Completeness**: All Java public methods present (getVertices, getBounds, setBounds, getTransform, setTransform, set, getCorner000-111, contains x3, intersects x2, mul).
**Renames**: None
**Convention changes**: `Serializable` dropped; static arrays -> companion object; `init()` -> inline field initializers; split packages
**TODOs**: None
**Issues**: None — axes bug fixed (now extracts matrix columns like Java); order of operations matches Java

---

### BoundingBox.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/BoundingBox.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/BoundingBox.java` |
| Status | pass |
| Tested | Yes — `sge/math/collision/CollisionTest.scala` |

**Completeness**: All Java public methods present including `contains(OrientedBoundingBox)`.
**Renames**: `add` -> `+`, `sub` -> `-`, `scl` -> `scale`
**Convention changes**: `Serializable` dropped; static `tmpVector` -> companion object; `java.util.List` -> `scala.collection.immutable.List`; `intersects` uses min/max comparison (Java uses SAT center/dim — logically equivalent)
**TODOs**: None
**Issues**: None

---

### Sphere.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/Sphere.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/Sphere.java` |
| Status | minor_issues |
| Tested | No — no LibGDX test exists; basic coverage via OBB/BB tests |

**Completeness**: All Java public methods present (overlaps, hashCode, equals, volume, surfaceArea).
**Renames**: `dst2` -> `distanceSq`
**Convention changes**: `Serializable` dropped; `PI_4_3` constant -> companion object; pattern-match `equals`
**TODOs**: None
**Issues**:
- `minor`: Constructor takes center Vector3 by reference; Java copies. Aliasing risk.
- `minor`: Secondary constructor `(centerX, centerY, centerZ, radius)` is an SGE addition

---

### Segment.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/Segment.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/Segment.java` |
| Status | pass |
| Tested | No — no LibGDX test exists; trivial class |

**Completeness**: All Java public methods present (len, len2, equals, hashCode).
**Renames**: `dst` -> `distance`, `dst2` -> `distanceSq`
**Convention changes**: `Serializable` dropped; primary constructor with defaults (Java has no no-arg ctor); pattern-match `equals`
**TODOs**: None
**Issues**: None — constructor stores refs (acceptable for simple value holder)
