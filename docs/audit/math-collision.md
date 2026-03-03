# Audit: sge.math.collision

Audited: 5/5 files | Pass: 0 | Minor: 4 | Major: 1
Last updated: 2026-03-03

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
| Status | major_issues |
| Tested | Yes — `sge/math/collision/CollisionTest.scala` |

**Completeness**: All Java public methods present (getVertices, getBounds, setBounds, getTransform, setTransform, set, getCorner000-111, contains x3, intersects x2, mul).
**Renames**: None
**Convention changes**: `Serializable` dropped; static arrays -> companion object; `init()` -> inline field initializers
**TODOs**: None
**Issues**:
- `major`: **BUG in `update()`** — axes computed via `set(1,0,0).mul(transform).nor()` includes translation component. Java extracts matrix columns directly: `axes[0].set(M00, M10, M20).nor()`. Produces incorrect axes for any non-identity translation.
- `minor`: Order of operations in `update()` differs from Java

---

### BoundingBox.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/BoundingBox.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/BoundingBox.java` |
| Status | minor_issues |
| Tested | Yes — `sge/math/collision/CollisionTest.scala` |

**Completeness**: Nearly complete. Missing: `contains(OrientedBoundingBox)` method.
**Renames**: `add` -> `+`, `sub` -> `-`, `scl` -> `scale`
**Convention changes**: `Serializable` dropped; static `tmpVector` -> companion object; `java.util.List` -> `scala.collection.immutable.List`
**TODOs**: None
**Issues**:
- `minor`: Missing `contains(OrientedBoundingBox)` method present in Java source
- `minor`: `intersects()` algorithm differs (min/max vs Java's SAT center/dim) — logically equivalent

---

### Sphere.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/math/collision/Sphere.scala` |
| Java source(s) | `com/badlogic/gdx/math/collision/Sphere.java` |
| Status | minor_issues |
| Tested | No |

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
| Status | minor_issues |
| Tested | No |

**Completeness**: All Java public methods present (len, len2, equals, hashCode).
**Renames**: `dst` -> `distance`, `dst2` -> `distanceSq`
**Convention changes**: `Serializable` dropped; primary constructor with defaults (Java has no no-arg ctor); pattern-match `equals`
**TODOs**: None
**Issues**:
- `minor`: Constructor stores Vector3 refs directly; Java copies via `this.a.set(a)`. Aliasing risk.
- `minor`: Default no-arg constructor path exists in Scala but not in Java
