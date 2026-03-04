# Audit: sge.graphics.g3d.loader

Audited: 3/3 files | Pass: 3 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### G3dModelJson.scala -- pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/loader/G3dModelJson.scala` |
| Java source(s) | SGE-original (JSON DTO case classes for .g3dj format) |
| Status | pass |
| Tested | No |

**Completeness**: Typed JSON DTOs for .g3dj format: G3dModelJson, G3dMeshJson, G3dMeshPartJson,
G3dMaterialJson, G3dTextureJson, G3dNodeJson, G3dNodePartJson, G3dBoneJson, G3dAnimationJson,
G3dAnimBoneJson, G3dKeyframeV1Json, G3dKeyframeV2Json. All `final case class` with jsoniter-scala
codec derivation.
**Renames**: None (SGE-original)
**Convention changes**: None
**TODOs**: None
**Issues**: None

---

### G3dModelLoader.scala -- pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/loader/G3dModelLoader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `parseModel`, `parseMeshes`, `parseType`, `parseAttributes`,
`parseMaterials`, `parseTextureUsage`, `parseColor`, `parseNodes`, `parseNodesRecursively`,
`parseAnimations`. Companion object: `VERSION_HI`/`VERSION_LO`. Rewritten to use typed G3dModelJson
DTOs with jsoniter-scala codec derivation instead of JsonValue tree walking; `BaseJsonReader` field
removed (replaced by `readJson[G3dModelJson]`).
**Renames**: `GdxRuntimeException` → `SgeError.InvalidInput`/`SgeError.GraphicsError`;
`JsonValue` tree walking → typed DTOs
**Convention changes**: Java 1-arg constructor (resolver=null) removed; `loadModelData` returns
`Nullable[ModelData]`; `(using Sge)` context parameter on class; `ArrayMap` constructor uses
`(ordered, capacity)` instead of Java array factory lambdas
**TODOs**: None
**Issues**: None

---

### ObjLoader.scala -- pass

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/loader/ObjLoader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/loader/ObjLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `loadModel(FileHandle, Boolean)`, `loadModelData`
(2 overloads). Private methods: `setActiveGroup`, `getIndex`. Companion object: `logWarning` var,
`ObjLoaderParameters` class, `Group` private class, `MtlLoader` private class with `load`,
`parseColor`, `getMaterial`, `ObjMaterial` inner class.
**Renames**: `GdxRuntimeException` → `SgeError`; `Gdx.app.error` → `Sge().application.error`;
`FloatArray` → `DynamicArray[Float]`; `Array<Integer>` → `DynamicArray[Int]`
**Convention changes**: No-arg constructor passes null resolver (Java interop boundary, ModelLoader
accepts nullable); `loadModelData` returns `Nullable[ModelData]` with `boundary`/`break` for early
returns; Java face parsing loop `i--` bug corrected to `i += 1`; MtlLoader nested as private class
in companion (was Java package-private top-level class); `ObjMaterial` null String fields →
`Nullable[String]`; null Color → `Nullable[Color]`
**TODOs**: 1 — test: decode a real .obj/.mtl file end-to-end (inherited from audit, not a code issue)
**Issues**: None
