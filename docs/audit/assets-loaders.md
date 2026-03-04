# Audit: sge.assets.loaders

Audited: 16/16 files | Pass: 14 | Minor: 2 | Major: 0
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
**Convention changes**: `Array<AssetDescriptor>` -> `DynamicArray[AssetDescriptor[?]]`; split packages (fixed 2026-03-04)
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
**Convention changes**: `null` fields → `Nullable[T]`; `(using Sge)` constructor
**TODOs**: test: getDependencies resolves font textures; loadSync assembles BitmapFont
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
**TODOs**: test: loadAsync/loadSync with KTX and parameter-supplied data
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
| Status | minor_issues |
| Tested | No |

**Completeness**: All 8 methods present. `ModelParameters` with textureParameter field.
**Convention changes**: `ObjectMap.Entry<String, ModelData>` → `DynamicArray[(String, ModelData)]`
**TODOs**: test: getDependencies collects texture dependencies; loadSync assembles Model
**Issues**:
- `minor`: `null.asInstanceOf[P]` at 5 sites — mirrors Java `null` parameter semantics but violates no-null convention
- `minor`: Dead code in `loadSync` — `break(null.asInstanceOf[Model])` makes fold default unreachable

---

### MusicLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/MusicLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/MusicLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `getLoadedMusic` returns `Nullable[Music]`.
**Convention changes**: `Gdx.audio` → `Sge().audio`; `loadSync` throws `SgeError` instead of returning null
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
**Convention changes**: `getDependencies` returns empty `DynamicArray` instead of Java `null` when no atlas
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
| Status | minor_issues |
| Tested | No |

**Completeness**: All methods ported. `ShaderProgramParameter` with 5 fields. Two constructors (default params).
**TODOs**: test: file suffix resolution (.vert/.frag) and code prepend logic
**Issues**:
- `minor`: Logging routes through `Sge().application.error()` instead of `manager.getLogger().error()` — different log level control

---

### SkinLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/assets/loaders/SkinLoader.scala` |
| Java source(s) | `com/badlogic/gdx/assets/loaders/SkinLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `newSkin` protected factory preserved.
**Convention changes**: `ObjectMap<String, Object>` → `mutable.Map[String, Any]`; `SkinParameter` uses default params instead of 4 constructors
**TODOs**: test: getDependencies resolves atlas; loadSync applies resources
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
**Convention changes**: `Gdx.audio` → `Sge().audio`; `loadSync` throws `SgeError` instead of returning null
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
**Convention changes**: `data` wrapped in `Nullable`; `TextureParameter.format` wrapped in `Nullable`
**TODOs**: test: getDependencies resolves page textures; load assembles atlas
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
**TODOs**: test: loadAsync/loadSync with parameter-supplied and file-loaded data
**Issues**: None
