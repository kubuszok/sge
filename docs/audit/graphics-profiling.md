# Audit: sge.graphics.profiling

Audited: 7/7 files | Pass: 3 | Minor: 3 | Major: 1
Last updated: 2026-03-03

---

### GL20Interceptor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GL20Interceptor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GL20Interceptor.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 142 GL20 methods faithfully ported 1:1.
**Renames**: None
**Convention changes**: `gl20` field promoted to public val constructor param; `check()` promoted to `override protected`
**TODOs**: None
**Issues**: None

---

### GL30Interceptor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GL30Interceptor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GL30Interceptor.java` |
| Status | pass |
| Tested | No |

**Completeness**: All GL20 + GL30-specific methods faithfully ported 1:1.
**Renames**: None
**Convention changes**: `gl30` field promoted to public val constructor param; `check()` promoted to `override protected`
**TODOs**: None
**Issues**: None

---

### GL31Interceptor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GL31Interceptor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GL31Interceptor.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 55 GL31-specific methods faithfully ported 1:1.
**Renames**: None
**Convention changes**: `gl31` field promoted to public val constructor param
**TODOs**: None
**Issues**: None

---

### GL32Interceptor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GL32Interceptor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GL32Interceptor.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All 38 GL32-specific methods faithfully ported.
**Renames**: None
**Convention changes**: `gl32` field promoted to public val; removed duplicate `check()` call from Java (likely Java bug)
**TODOs**: None
**Issues**:
- `minor`: `glDebugMessageCallback` uses `asInstanceOf[gl32.DebugProc]` cast — fragile if `DebugProc` is not a shared type alias

---

### GLErrorListener.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GLErrorListener.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GLErrorListener.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: Both `LOGGING_LISTENER` and `THROWING_LISTENER` implemented.
**Renames**: `GdxRuntimeException` -> `RuntimeException`, `Gdx.app.error()` -> `println()`
**Convention changes**: Java interface -> Scala trait; static finals -> companion object vals; `null` -> `Nullable`
**TODOs**: None
**Issues**:
- `minor`: `LOGGING_LISTENER` uses `println` instead of `Sge.app.error()` — loses structured logging tag
- `minor`: Empty `place` fallback uses `new Exception().printStackTrace()` instead of logger

---

### GLInterceptor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GLInterceptor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GLInterceptor.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All fields, getters, `reset()`, and `resolveErrorNumber()` present. Added abstract `protected def check()`.
**Renames**: None
**Convention changes**: Static `resolveErrorNumber` -> companion object method; `switch` -> `match`; added abstract `check()` (improvement)
**TODOs**: None
**Issues**:
- `minor`: Java-style getters retained (`getCalls()`, `getTextureBindings()`, etc.) — simple pass-throughs that could be Scala properties

---

### GLProfiler.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/profiling/GLProfiler.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/profiling/GLProfiler.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: Structure matches Java. Constructor, enable/disable, listener, metric accessors, reset all present. But `enable()`/`disable()` bodies are entirely commented out.
**Renames**: `GdxRuntimeException` -> `RuntimeException`
**Convention changes**: Null fields -> `scala.compiletime.uninitialized`; GL level detection hardcoded to GL20
**TODOs**: 5 — constructor GL detection, enable/disable Graphics methods, enable/disable Gdx globals
**Issues**:
- `major`: Constructor does not detect GL30/31/32 — always creates `GL20Interceptor`
- `major`: `enable()` body commented out — interceptor not installed into Graphics instance
- `major`: `disable()` body commented out — original GL instances not restored
- `minor`: Java-style getters retained (`getListener()`, `isEnabled()`, `getCalls()`, etc.)
