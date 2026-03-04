# Audit: sge.files

Audited: 2/2 files | Pass: 2 | Minor: 0 | Major: 0
Last updated: 2026-03-04

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
**Issues**: None

---

### FileHandles.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/files/FileHandles.scala` |
| Java source(s) | `com/badlogic/gdx/files/FileHandle.java`, `com/badlogic/gdx/files/FileHandleStream.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public instance methods ported. Static factory methods `tempFile`/`tempDirectory` intentionally omitted (desktop-only utilities). Convenience constructors omitted (only `(File, FileType)` primary constructor remains). `FileHandleStream` merged into this file.
**Renames**: `type()` -> `fileType` (val), `GdxRuntimeException` -> `SgeError.FileReadError`/`SgeError.FileWriteError`
**Convention changes**: Merged `FileHandleStream.java`; null params -> `Nullable`; split packages; `getFile()` External path resolution via `externalStoragePath` constructor param (replaces Java's `Gdx.files.getExternalStoragePath()` global static); `child`/`sibling`/`parent` propagate `externalStoragePath`
**Fixes**: `deleteDirectory(File)` rewritten to match Java's `emptyDirectory` + `delete` pattern. `reader(bufferSize, charset)` stream leak fixed. `getFile()` External path resolution implemented via constructor param.
**Issues**: None
