# Audit: sge.graphics.g3d.utils.shapebuilders

Audited: 11/11 files | Pass: 11 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### ArrowShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/ArrowShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/ArrowShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — single `build` method ported.
**Renames**: `dst` → `distance`, `len` → `length`
**Convention changes**: Java static class → Scala object; null → Nullable
**TODOs**: None
**Issues**: None

---

### BaseShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/BaseShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/BaseShapeBuilder.java` |
| Status | pass |
| Tested | No — internal utility, no LibGDX tests |

**Completeness**: Full — all fields, pools, and methods ported.
**Renames**: None
**Convention changes**: Java protected static → Scala protected[shapebuilders]; FlushablePool → Pool.Flushable
**TODOs**: None
**Issues**: None

---

### BoxShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/BoxShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/BoxShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 6 `build` overloads ported.
**Renames**: None
**Convention changes**: null → Nullable.empty
**TODOs**: None
**Issues**: None

---

### CapsuleShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/CapsuleShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/CapsuleShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — single `build` method ported.
**Renames**: None
**Convention changes**: GdxRuntimeException → SgeError.InvalidInput
**TODOs**: None
**Issues**: None

---

### ConeShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/ConeShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/ConeShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 3 `build` overloads ported.
**Renames**: None
**Convention changes**: Java chained assignment → separate assignments; null → Nullable.empty
**TODOs**: None (FIXME comments preserved from Java source)
**Issues**: None

---

### CylinderShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/CylinderShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/CylinderShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 3 `build` overloads ported.
**Renames**: None
**Convention changes**: Java chained assignment → separate assignments; null → Nullable.empty
**TODOs**: None (FIXME comments preserved from Java source)
**Issues**: None

---

### EllipseShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/EllipseShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/EllipseShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 22 `build` overloads ported.
**Renames**: None
**Convention changes**: null → Nullable.empty
**TODOs**: None
**Issues**: None

---

### FrustumShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/FrustumShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/FrustumShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 4 methods ported (3 `build` + `middlePoint` + `centerPoint`).
**Renames**: `len` → `length`
**Convention changes**: null → Nullable
**TODOs**: None
**Issues**: None

---

### PatchShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/PatchShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/PatchShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 3 `build` overloads ported.
**Renames**: None
**Convention changes**: GdxRuntimeException → SgeError.InvalidInput; null → Nullable.empty
**TODOs**: None
**Issues**: Fixed — missing `.setUV()` calls in 2nd and 3rd `build` overloads (would have broken texture-mapped patches)

---

### RenderableShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/RenderableShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/RenderableShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 3 public methods and 4 private helpers ported.
**Renames**: None
**Convention changes**: null arrays → Array.empty; FlushablePool → Pool.Flushable
**TODOs**: None
**Issues**: None

---

### SphereShapeBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/shapebuilders/SphereShapeBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/shapebuilders/SphereShapeBuilder.java` |
| Status | pass |
| Tested | No — geometry builder, no LibGDX tests |

**Completeness**: Full — all 4 `build` overloads ported.
**Renames**: None
**Convention changes**: ShortArray → DynamicArray[Short]
**TODOs**: None
**Issues**: None
