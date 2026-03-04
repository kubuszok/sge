# Audit: sge.assets.loaders

Audited: 16/16 files | Pass: 16 | Minor: 0 | Major: 0
Last updated: 2026-03-04

Note: resolver subpackage classes consolidated into `FileHandleResolver` companion object — see [assets-loaders-resolvers.md](assets-loaders-resolvers.md).

---

### AssetLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/AssetLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/AssetLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: `resolve()` and `getDependencies()` ported.
**Convention changes**: `Array<AssetDescriptor>` -> `DynamicArray[AssetDescriptor[?]]`; split packages
**Issues**: None

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

### FileHandleResolver.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/FileHandleResolver.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/FileHandleResolver.java` + 7 resolver classes (see [assets-loaders-resolvers.md](assets-loaders-resolvers.md)) |
| Status | pass |
| Tested | No |

**Completeness**: Trait with single `resolve` method + companion object containing all 7 resolver classes (`Absolute`, `Classpath`, `External`, `Internal`, `Local`, `Prefix`, `ForResolution`).
**Fixes**: `ForResolution.resolve` used `FileType.Internal` instead of `FileType.Absolute` (matching Java's `new FileHandle(fileName)` default); empty-array validation moved to primary constructor
**TODOs**: test: ForResolution.choose() picks best resolution for screen size; Prefix.resolve prepends prefix
**Issues**: None

---

### BitmapFontLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/BitmapFontLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/BitmapFontLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `BitmapFontParameter` with 6 fields (flip, genMipMaps, minFilter, magFilter, bitmapFontData, atlasName).
**Convention changes**: `null` fields → `Nullable[T]`; `(using Sge)` constructor; `manager(...)` apply syntax
**TODOs**: test: getDependencies resolves font textures; loadSync assembles BitmapFont (requires GL context)
**Issues**: None

---

### CubemapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/CubemapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/CubemapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `CubemapParameter` with 7 fields. Java dead code `if (info == null) return null` in `loadSync` correctly omitted.
**TODOs**: test: loadAsync/loadSync with KTX and parameter-supplied data (requires GL context)
**Issues**: None

---

### I18NBundleLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/I18NBundleLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/I18NBundleLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `I18NBundleParameter` with locale/encoding as `Nullable` vals.
**Convention changes**: Java `final` fields → Scala `val` constructor params with `Nullable` defaults; `loadSync` throws `SgeError` instead of returning null
**TODOs**: test: loadAsync/loadSync with locale and encoding parameters
**Issues**: None

---

### ModelLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/ModelLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/ModelLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 8 methods present. `ModelParameters` with textureParameter field.
**Convention changes**: `ObjectMap.Entry<String, ModelData>` → `DynamicArray[(String, ModelData)]`; `loadModelData`/`loadModel` parameters changed from `P` to `Nullable[P]`; `loadSync` uses `fold` pattern; `null.asInstanceOf[Model]` at Java API boundary (documented)
**TODOs**: test: getDependencies collects texture dependencies; loadSync assembles Model (requires GL context)
**Issues**: None

---

### MusicLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/MusicLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/MusicLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `getLoadedMusic` returns `Nullable[Music]`.
**Convention changes**: `Gdx.audio` → `Sge().audio`; `loadSync` throws `SgeError` instead of returning null; `getDependencies` returns empty DynamicArray instead of Java null
**TODOs**: test: loadAsync/loadSync (requires audio backend)
**Issues**: None

---

### ParticleEffectLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/ParticleEffectLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/ParticleEffectLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: `load()` and `getDependencies()` ported. `ParticleEffectParameter` with 3 fields.
**Convention changes**: `getDependencies` returns empty `DynamicArray` instead of Java `null` when no atlas; `manager(...)` apply syntax
**TODOs**: test: load with atlas file, images directory, and default parameters
**Issues**: None

---

### PixmapLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/PixmapLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/PixmapLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `getDependencies` returns empty `DynamicArray` instead of Java `null`.
**Convention changes**: `loadSync` throws `SgeError` instead of returning null
**TODOs**: test: loadAsync/loadSync (requires GL context)
**Issues**: None

---

### ShaderProgramLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/ShaderProgramLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/ShaderProgramLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `ShaderProgramParameter` with 5 fields. Primary constructor with default suffix params covers Java's 2 constructors.
**Convention changes**: Logging uses `manager.getLogger.error()` matching Java source; `getDependencies` returns empty DynamicArray instead of Java null
**Fixes**: Removed redundant secondary constructor (primary constructor defaults cover it)
**TODOs**: test: file suffix resolution (.vert/.frag) and code prepend logic
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
**Convention changes**: `ObjectMap<String, Object>` → `mutable.Map[String, Any]`; `SkinParameter` uses default params instead of 4 constructors; `manager(...)` apply syntax
**TODOs**: test: getDependencies resolves atlas; loadSync applies resources (requires GL context)
**Issues**: None

---

### SoundLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/SoundLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/SoundLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `getLoadedSound` returns `Nullable[Sound]`.
**Convention changes**: `Gdx.audio` → `Sge().audio`; `loadSync` throws `SgeError` instead of returning null; `getDependencies` returns empty DynamicArray instead of Java null
**TODOs**: test: loadAsync/loadSync (requires audio backend)
**Issues**: None

---

### TextureAtlasLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/TextureAtlasLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/TextureAtlasLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: `load()` and `getDependencies()` ported. `TextureAtlasParameter` with flip field.
**Convention changes**: `data` wrapped in `Nullable`; `TextureParameter.format` wrapped in `Nullable`; null-safe iteration via `page.textureFile.foreach`; `manager(...)` apply syntax
**TODOs**: test: getDependencies resolves page textures; load assembles atlas (requires GL context)
**Issues**: None

---

### TextureLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/TextureLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/TextureLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `TextureParameter` with 8 fields. Java dead code `if (info == null) return null` in `loadSync` correctly omitted.
**Convention changes**: `getDependencies` returns empty DynamicArray instead of Java null
**TODOs**: test: loadAsync/loadSync with parameter-supplied and file-loaded data (requires GL context)
**Issues**: None
