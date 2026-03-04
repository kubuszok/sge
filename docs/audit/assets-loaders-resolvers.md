# Audit: sge.assets.loaders.resolvers (consolidated)

All 7 resolver classes have been merged into `FileHandleResolver` companion object.
Individual files in `sge/assets/loaders/resolvers/` no longer exist.

Audited: 8 Java sources → 1 Scala file | Pass: 8 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### FileHandleResolver.scala (consolidated)

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/FileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/FileHandleResolver.java`, `com/badlogic/gdx/assets/loaders/resolvers/{Absolute,Classpath,External,Internal,Local}FileHandleResolver.java`, `PrefixFileHandleResolver.java`, `ResolutionFileResolver.java` |
| Status | pass |
| Tested | No |

**Structure**: Java `FileHandleResolver` interface → Scala `trait`. Seven resolver classes from
`resolvers/` subpackage merged into `object FileHandleResolver` as nested classes:
`Absolute`, `Classpath`, `External`, `Internal`, `Local`, `Prefix`, `ForResolution`.

**Completeness**: All public methods from all 8 Java sources are present.

**Convention changes**:
- 5 simple resolvers: `Gdx.files.xxx()` → `(using Sge)` constructor + `Sge().files.xxx()`
- `PrefixFileHandleResolver` → `Prefix`: `getBaseResolver/setBaseResolver` → `var baseResolver`;
  `getPrefix/setPrefix` → `var prefix` (idiomatic Scala public vars)
- `ResolutionFileResolver` → `ForResolution`: `Resolution` static inner class → `final case class`;
  `choose()` uses `Sge().graphics.getBackBufferWidth()/getBackBufferHeight()` (was stubbed, now complete)
- `new FileHandle(fileName)` → `new FileHandle(new java.io.File(fileName), FileType.Internal)`

**Issues**: None
