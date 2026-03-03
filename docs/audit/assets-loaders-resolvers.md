# Audit: sge.assets.loaders.resolvers

Audited: 7/7 files | Pass: 6 | Minor: 1 | Major: 0
Last updated: 2026-03-03

---

### AbsoluteFileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/AbsoluteFileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/AbsoluteFileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single `resolve(String): FileHandle` method faithfully ported.
**Convention changes**: Java uses static `Gdx.files.absolute()`; Scala class gains `(using Sge)` constructor parameter to access `Sge().files.absolute()`.
**Issues**: None

---

### ClasspathFileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/ClasspathFileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/ClasspathFileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single `resolve(String): FileHandle` method faithfully ported.
**Convention changes**: Java uses static `Gdx.files.classpath()`; Scala class gains `(using Sge)` constructor parameter to access `Sge().files.classpath()`.
**Issues**: None

---

### ExternalFileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/ExternalFileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/ExternalFileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single `resolve(String): FileHandle` method faithfully ported.
**Convention changes**: Java uses static `Gdx.files.external()`; Scala class gains `(using Sge)` constructor parameter to access `Sge().files.external()`.
**Issues**: None

---

### InternalFileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/InternalFileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/InternalFileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single `resolve(String): FileHandle` method faithfully ported.
**Convention changes**: Java uses static `Gdx.files.internal()`; Scala class gains `(using Sge)` constructor parameter to access `Sge().files.internal()`.
**Issues**: None

---

### LocalFileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/LocalFileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/LocalFileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single `resolve(String): FileHandle` method faithfully ported.
**Convention changes**: Java uses static `Gdx.files.local()`; Scala class gains `(using Sge)` constructor parameter to access `Sge().files.local()`.
**Issues**: None

---

### PrefixFileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/PrefixFileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/PrefixFileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 methods present: constructor, `setBaseResolver`, `getBaseResolver`, `setPrefix`, `getPrefix`, `resolve`. No `(using Sge)` needed — does not call `Gdx.files` directly.
**Issues**: None

---

### ResolutionFileResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/resolvers/ResolutionFileResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/resolvers/ResolutionFileResolver.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: Both constructors and all instance methods present. `Resolution` static inner class -> companion object `case class`. `choose()` stubbed with TODO.
**Convention changes**: Java `new FileHandle(fileName)` -> `new FileHandle(new java.io.File(fileName), FileType.Internal)`; varargs secondary constructor added alongside primary array-based constructor; `choose()` static method signature gains `(using Sge)`.
**Issues**:
- `minor`: `choose()` always returns `descriptors(0)` — screen-size selection not implemented; needs `Sge().graphics.backBufferWidth` / `backBufferHeight`
