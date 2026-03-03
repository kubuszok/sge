# Audit: sge.files

Audited: 2/2 files | Pass: 1 | Minor: 1 | Major: 0
Last updated: 2026-03-03

---

### FileType.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/files/FileType.scala` |
| Java source(s) | `com/badlogic/gdx/Files.java` (nested `FileType` enum) |
| Status | pass |
| Tested | No |

**Completeness**: All 5 enum values (Classpath, Internal, External, Absolute, Local) faithfully ported.
**Renames**: `Files.FileType` (nested enum) -> `sge.files.FileType` (top-level enum)
**Convention changes**: Nested Java enum extracted to top-level Scala 3 enum; split packages
**TODOs**: None
**Issues**: None

---

### FileHandles.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/files/FileHandles.scala` |
| Java source(s) | `com/badlogic/gdx/files/FileHandle.java`, `com/badlogic/gdx/files/FileHandleStream.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public instance methods ported. Static factory methods `tempFile`/`tempDirectory` omitted. Convenience constructors omitted (only `(File, FileType)` primary constructor remains). `FileHandleStream` merged into this file.
**Renames**: `type()` -> `fileType` (val), `GdxRuntimeException` -> `SgeError.FileReadError`/`SgeError.FileWriteError`
**Convention changes**: Merged `FileHandleStream.java`; null params -> `Nullable`; split packages
**TODOs**: 1 — `getFile()` needs rewriting once Sge/Files integration is wired
**Issues**:
- `minor`: Static factory methods `tempFile(prefix)` and `tempDirectory(prefix)` omitted
- `minor`: Convenience constructors `FileHandle()`, `FileHandle(String)`, `FileHandle(File)` omitted
- `minor`: `reader(bufferSize, charset)` has stream leak matching Java bug (new stream opened in catch)
