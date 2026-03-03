# Audit: sge.assets.loaders

Audited: 16/16 files | Pass: 13 | Minor: 3 | Major: 0
Last updated: 2026-03-03

Note: resolver subpackage audited separately — see [assets-loaders-resolvers.md](assets-loaders-resolvers.md).

---

### AsynchronousAssetLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/AsynchronousAssetLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/AsynchronousAssetLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods (loadAsync, unloadAsync, loadSync) faithfully ported.
**Issues**: None

---

### FileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/FileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/FileHandleResolver.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single method `resolve(String): FileHandle` ported.
**Issues**: None

---

### SynchronousAssetLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/SynchronousAssetLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/SynchronousAssetLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: Single abstract method `load` ported.
**Issues**: None

---

### AssetLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/AssetLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/AssetLoader.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: `resolve()` and `getDependencies()` ported.
**Convention changes**: `Array<AssetDescriptor>` -> `DynamicArray[AssetDescriptor[?]]`
**Issues**:
- `minor`: Flat package `package sge.assets.loaders` instead of split form

---

### PixmapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/PixmapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/PixmapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `getDependencies` returns empty `DynamicArray` instead of Java `null`.
**Issues**: None

---

### CubemapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/CubemapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/CubemapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `CubemapParameter` with 7 fields.
**Issues**: None

---

### ModelLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/ModelLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/ModelLoader.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All 8 methods present.
**Issues**:
- `minor`: `null.asInstanceOf[P]` at 5 sites — mirrors Java semantics but violates no-null convention
- `minor`: Dead code in `loadSync` — `break(null.asInstanceOf[Model])` makes fold default unreachable

---

### TextureAtlasLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/TextureAtlasLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/TextureAtlasLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: `load()` and `getDependencies()` ported.
**Issues**: None

---

### SkinLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/SkinLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/SkinLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `newSkin` protected factory preserved.
**Issues**: None

---

### ParticleEffectLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/ParticleEffectLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/ParticleEffectLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: `load()` and `getDependencies()` ported.
**Issues**: None

---

### TextureLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/TextureLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/TextureLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `TextureParameter` with 8 fields.
**Issues**: None

---

### I18NBundleLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/I18NBundleLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/I18NBundleLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported.
**Issues**: None

---

### BitmapFontLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/BitmapFontLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/BitmapFontLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `BitmapFontParameter` with 6 fields.
**Issues**: None

---

### MusicLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/MusicLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/MusicLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported.
**Issues**: None

---

### SoundLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/SoundLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/SoundLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported.
**Issues**: None

---

### ShaderProgramLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/ShaderProgramLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/ShaderProgramLoader.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods ported. `ShaderProgramParameter` with 5 fields.
**Issues**:
- `minor`: Logging routes through `Sge().application.error()` instead of `manager.getLogger().error()` — different log level control
