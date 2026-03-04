# Audit: sge.utils.viewport

Audited: 7/7 files | Pass: 7 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### Viewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/Viewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/Viewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 26 methods + 7 fields ported.
**Renames**: `Gdx.graphics` -> `Sge().graphics`
**Convention changes**: `int`-to-`float` widening done explicitly via `.toFloat`
**Issues**: None

---

### ScalingViewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/ScalingViewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/ScalingViewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `update()` + `getScaling()`/`setScaling()` — 1:1 match.
**Convention changes**: `Scaling` is a SAM trait (not Java enum)
**Issues**: None

---

### ExtendViewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/ExtendViewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/ExtendViewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: 4 constructors + `update()` + 8 getters/setters — 1:1 match.
**Issues**: None

---

### ScreenViewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/ScreenViewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/ScreenViewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors + `update()` + `getUnitsPerPixel()`/`setUnitsPerPixel()` — 1:1 match.
**Issues**: None

---

### FillViewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/FillViewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/FillViewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors delegating to `ScalingViewport(Scaling.fill, ...)` — 1:1 match.
**Issues**: None (trailing semicolons on imports removed during audit)

---

### StretchViewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/StretchViewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/StretchViewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors delegating to `ScalingViewport(Scaling.stretch, ...)` — 1:1 match.
**Issues**: None (trailing semicolons on imports removed during audit)

---

### FitViewport.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/viewport/FitViewport.scala` |
| Java source(s) | `com/badlogic/gdx/utils/viewport/FitViewport.java` |
| Status | pass |
| Tested | No |

**Completeness**: 2 constructors delegating to `ScalingViewport(Scaling.fit, ...)` — 1:1 match.
**Issues**: None (trailing semicolons on imports removed during audit)
