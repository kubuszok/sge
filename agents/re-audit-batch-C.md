# Re-Audit Batch C: 3D Graphics, GL Utils, Profiling

**Auditor**: Automated re-audit agent
**Date**: 2026-04-18
**Scope**: `graphics.g3d.*`, `graphics.glutils`, `graphics.profiling` + `gltf.scene3d.utils.IBLBuilder`
**Files audited**: ~140 files (105 g3d, 35 glutils, 7 profiling, 1 gltf)

## Executive Summary

All 140+ files in scope have been previously audited with status `pass` or `minor_issues`. This re-audit confirms the prior assessments are substantially correct. No files require status downgrade. Key findings:

- **FIXME/TODO comments**: 51 shortcut markers across 26 g3d files, all preserved from Java originals
- **Serialization replacement**: All particle `read`/`write` (Json.Serializable) methods properly replaced by `ParticleEffectCodecs`
- **Method renames**: Consistent application of `dispose()->close()`, `getX()->x`, `setX()->x_=` across all files
- **DefaultShader.compareTo**: Improved over Java original (which returns 0 with FIXME) to compare by `attributesMask`
- **Lights attributes compareTo**: Improved over Java original (which returns 0 with FIXME) to compare by `lights.size`

---

## High-Suspicion Files (Audited First)

### IBLBuilder.scala (gltf extension)
- **Original**: `original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/utils/IBLBuilder.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: The suspicion of "6 methods throw UnsupportedOperationException" is outdated -- no UnsupportedOperationException in this file. All methods fully ported: `createOutdoor`, `createIndoor`, `createCustom`, `buildEnvMap`, `buildIrradianceMap`, `buildRadianceMap`, `renderGradientForSide` (was `renderGradient`), `renderLightsForSide` (was `renderLights`), `close` (was `dispose`). Inner class `Light` with both `render` overloads and companion object statics. Shortcuts scan found only 2 XXX comments preserved from Java original (lines 280, 286). Method rename `rect()` -> `rectangle()` is consistent.

### DefaultShader.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/shaders/DefaultShader.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none -- `compareTo` is IMPROVED (actual comparison by attributesMask vs Java's `return 0; // FIXME`)
- **Missing branches**: none
- **Mechanism changes without tests**: `compareTo` behavior difference from Java (returns attribute-based ordering vs always 0)
- **Notes**: Extremely large file (~1900 lines). All 5 constructors present. Config class with all fields. Inputs and Setters objects complete with all 30+ uniform definitions. Inner classes `Bones`, `ACubemap` faithfully ported. `createPrefix` method verified line-by-line -- all #define flags match Java. `init()`, `begin()`, `render()`, `end()`, `bindMaterial()`, `bindLights()`, `canRender()`, `close()`, `equals()` all present with equivalent logic. 8 shortcut markers are all preserved from Java source (6 FIXME UV mapping, 1 FIXME cache, 1 TODO). Line 345 has orphan `Matrix3()` statement (harmless, corresponds to Java's `private final Matrix3 normalMatrix = new Matrix3()` field). The `defaultCullFace`/`defaultDepthFunc` deprecated statics removed in favor of Config fields -- documented in migration notes.

### DirectionalLightsAttribute.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/attributes/DirectionalLightsAttribute.java`
- **Prior status**: minor_issues
- **New status**: PASS (upgrade)
- **Missing methods**: none
- **Simplified methods**: none -- `compare` is IMPROVED over Java's `return 0; // FIXME implement comparing`
- **Missing branches**: none
- **Mechanism changes without tests**: compare now returns `lights.size - other.lights.size` instead of 0
- **Notes**: All fields, constructors, `copy`, `hashCode` (with correct multiplier 1229), `compare` present. The "FIXME compareTo" was the prior minor_issues reason, but Scala actually implements the comparison properly. Hash multiplier matches Java exactly. `DynamicArray` default capacity vs Java `new Array<>(1)` is a minor difference.

### PointLightsAttribute.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/attributes/PointLightsAttribute.java`
- **Prior status**: minor_issues
- **New status**: PASS (upgrade)
- **Missing methods**: none
- **Simplified methods**: none -- same improvement as DirectionalLightsAttribute
- **Missing branches**: none
- **Mechanism changes without tests**: compare now returns `lights.size - other.lights.size` instead of 0
- **Notes**: Identical pattern to DirectionalLightsAttribute. Hash multiplier 1231 matches Java. All methods present.

### SpotLightsAttribute.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/g3d/attributes/SpotLightsAttribute.java`
- **Prior status**: minor_issues
- **New status**: PASS (upgrade)
- **Missing methods**: none
- **Simplified methods**: none -- same improvement as above
- **Missing branches**: none
- **Mechanism changes without tests**: compare now returns `lights.size - other.lights.size` instead of 0
- **Notes**: Hash multiplier 1237 matches Java. All methods present.

### FrameBufferCubemap.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/glutils/FrameBufferCubemap.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: All constructors (4-arg, 5-arg, builder), `createTexture`, `disposeColorTexture`, `attachFrameBufferColorTexture`, `bind`, `nextSide`, `bindSide`, `side` (was `getSide`) present. `side` returns `Nullable[CubemapSide]` instead of raw null. Companion object caches `cubemapSides` array. `nextSide` logic faithfully matches Java with correct side counting.

### IndexBufferObjectSubData.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/glutils/IndexBufferObjectSubData.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: All methods present: `setIndices` (2 overloads), `updateIndices`, `buffer`, `getBuffer`, `bind`, `unbind`, `invalidate`, `close`. Both constructors present. Buffer usage pattern with `glBufferSubData` matches Java.

### KTXTextureData.scala
- **Original**: `original-src/libgdx/gdx/src/com/badlogic/gdx/graphics/glutils/KTXTextureData.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: All methods present: `prepare`, `consumeCustomData`, `consumeCubemapData`, `consumePixmap`, `disposePixmap`, `disposePreparedData`, `getData`, `getFormat`, `useMipMaps`, `isManaged`, accessors. KTX header parsing (12-byte magic + endian tag + 12 int fields) matches Java exactly. Dummy constants `GL_TEXTURE_1D = 0x1234` etc match Java. 1D and 3D texture paths commented out (same as Java -- GL calls don't exist in GLES). 2D texture path with ETC1 decode fallback fully implemented.

---

## Systematic Audit: graphics.g3d

### Attributes.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Attributes.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Notes**: FIXME `sort()` on lines 101, 153 preserved from Java. All methods present.

### Attribute.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Attribute.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Static `register`/`getAlias` system ported. `compare` replaces `compareTo`.

### Environment.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Environment.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Short file, all `add` overloads and `shadowMap` present.

### Material.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Material.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: All constructors and `copy` method present.

### Model.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Model.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Notes**: 4 FIXME comments preserved from Java. `managedDisposables` (was `getManagedDisposables`). All model loading/conversion methods present.

### ModelBatch.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/ModelBatch.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: All constructors, `begin`/`end`/`render`/`flush`/`close` present. Pool pattern preserved.

### ModelCache.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/ModelCache.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Line 96 has `null.asInstanceOf[Mesh]` for pool reset -- legitimate Java interop boundary. FIXME on line 274 preserved from Java.

### ModelInstance.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/ModelInstance.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: 8 constructors consolidated via Nullable parameters. FIXME preserved. All methods verified.

### Renderable.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Renderable.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: All fields present as public vars.

### RenderableProvider.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/RenderableProvider.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Trait with single `getRenderables` method.

### Shader.scala
- **Original**: `com/badlogic/gdx/graphics/g3d/Shader.java`
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: All trait methods present including `compareTo`. TODO preserved from Java.

---

## Systematic Audit: graphics.g3d.attributes

### BlendingAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All 4 fields, 6 constructors, `copy`, `hashCode`, `compare` verified.

### ColorAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All 7 alias/type pairs, 14 factory methods verified.

### CubemapAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All constructors and `textureDescription` field verified.

### DepthTestAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All 4 fields and 8 constructors verified.

### FloatAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 2 constants, 2 aliases, factory methods verified.

### IntAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 1 constant, 1 alias, `createCullFace` factory verified.

### TextureAttribute.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: 7 alias/type pairs, 6 fields, 14 factory methods, `set(TextureRegion)` verified.

---

## Systematic Audit: graphics.g3d.decals

### CameraGroupStrategy.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods including shader setup and material group handling verified.

### Decal.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Extensive file with all property accessors, vertex manipulation, factory methods. TODO on line 707 preserved.

### DecalBatch.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods including mesh management and group rendering verified.

### DecalMaterial.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Short file, all fields and `equals`/`hashCode` verified.

### GroupPlug.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Marker trait/class.

### GroupStrategy.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Interface/trait with all methods.

### PluggableGroupStrategy.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods verified.

### SimpleOrthoGroupStrategy.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: FIXME "sort by material" preserved from Java.

---

## Systematic Audit: graphics.g3d.environment

### AmbientCubemap.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All cubemap manipulation methods verified.

### BaseLight.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All fields and type parameter pattern verified.

### DirectionalLight.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All `set` overloads and fields verified.

### DirectionalShadowLight.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods including FBO, camera, shadow map accessors verified. `close` replaces `dispose`.

### PointLight.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All `set` overloads and `intensity` field verified.

### ShadowMap.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Trait with `projViewTrans` and `depthMap` methods.

### SphericalHarmonics.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All coefficient methods verified.

### SpotLight.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All `set` overloads, `cutoffAngle`, `exponent` fields verified.

---

## Systematic Audit: graphics.g3d.loader

### G3dBinaryModelLoader.scala
- **Prior status**: pass (in audit DB as part of G3dModelLoader)
- **New status**: PASS
- **Notes**: Binary model loading methods verified against Java source.

### G3dModelJson.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: JSON DTOs for .g3dj format. Typed case classes replace raw JsonValue parsing.

### G3dModelLoader.scala
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `readVector2` - inlined into calling code as pattern match, functionally equivalent
- **Notes**: All parsing methods verified: `parseMeshes`, `parseType`, `parseAttributes`, `parseColor`, `parseNodes`, `parseNodesRecursively`, `parseMaterials`, `parseAnimations`. The `readVector2` helper was inlined.

### ObjLoader.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods present. TODO preserved from Java. `loadModel` and `loadModelData` verified.

---

## Systematic Audit: graphics.g3d.model & graphics.g3d.model.data

### Animation.scala, MeshPart.scala, Node.scala, NodeAnimation.scala, NodeKeyframe.scala, NodePart.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All public fields and methods verified for each file. FIXME comments preserved. MeshPart has all constructors, `render`, `equals`, `set`, `update`. Node has all tree operations (`addChild`, `removeChild`, `calculateTransforms`, etc.).

### ModelAnimation.scala, ModelData.scala, ModelMaterial.scala, ModelMesh.scala, ModelMeshPart.scala, ModelNode.scala, ModelNodeAnimation.scala, ModelNodeKeyframe.scala, ModelNodePart.scala, ModelTexture.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All are data classes with public fields. All fields match Java originals. `ModelMaterial` has `MaterialType` enum ported. `ModelTexture` has all `USAGE_*` constants.

---

## Systematic Audit: graphics.g3d.particles

### ParallelArray.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All channel types (`FloatChannel`, `IntChannel`, `ObjectChannel`), `addChannel`, `removeArray`. FIXME "make it grow" preserved.

### ParticleChannels.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All channel ID constants and `Initializer` inner class verified.

### ParticleController.scala
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `read`/`write` (Json.Serializable) -- intentionally replaced by ParticleEffectCodecs
- **Notes**: All 23 public methods verified. `DEFAULT_TIME_STEP`, `init`, `activate`, `end`, `update`, `setTransform`, `translate`, `rotate`, `scale`, `copy`, `boundingBox` all present.

### ParticleControllerComponent.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Abstract base class with lifecycle methods.

### ParticleEffect.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods including `init`, `update`, `start`, `reset`, `end`, `copy`, `close`, `controllers`, `boundingBox` verified.

### ParticleEffectLoader.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All loader methods verified. `find()` was private in Java, correctly omitted.

### ParticleEffectCodecs.scala
- **Prior status**: (no direct Java original -- replaces Json.Serializable across particle files)
- **New status**: PASS
- **Missing methods**: none
- **Notes**: 10 shortcut markers found, all are `UnsupportedOperationException` for unknown type dispatching in codec deserializers (e.g., unknown DynamicsModifier type, unknown Influencer type). These are correct error handling for malformed input, not stubs. The "For now" comment on line 1916 about re-parsing simple values is a minor style issue.

### ParticleShader.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All uniforms, Setters, Inputs, AlignMode enum, Config class verified. FIXME on compareTo preserved from Java.

### ParticleSorter.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Inner `None` and `Distance` classes verified.

### ParticleSystem.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods including batch management verified.

### ResourceData.scala
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: `read`/`write` replaced by `toJson`/`fromJson`
- **Notes**: SaveData inner class complete. Asset descriptor management verified.

---

## Systematic Audit: graphics.g3d.particles.batches

### BillboardParticleBatch.scala, BufferedParticleBatch.scala, ModelInstanceParticleBatch.scala, ParticleBatch.scala, PointSpriteParticleBatch.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All methods verified. `read`/`write` omitted consistently, covered by codecs.

---

## Systematic Audit: graphics.g3d.particles.emitters

### Emitter.scala, RegularEmitter.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All lifecycle methods and emission logic verified. `read`/`write` omitted, covered by codecs.

---

## Systematic Audit: graphics.g3d.particles.influencers

### ColorInfluencer.scala, DynamicsInfluencer.scala, DynamicsModifier.scala, Influencer.scala, ModelInfluencer.scala, ParticleControllerFinalizerInfluencer.scala, ParticleControllerInfluencer.scala, RegionInfluencer.scala, ScaleInfluencer.scala, SimpleInfluencer.scala, SpawnInfluencer.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All influencer/modifier classes verified. Inner class hierarchies (e.g., DynamicsModifier's 8 subclasses) all present. `read`/`write` omitted, covered by codecs.

---

## Systematic Audit: graphics.g3d.particles.renderers

### BillboardControllerRenderData.scala, BillboardRenderer.scala, ModelInstanceControllerRenderData.scala, ModelInstanceRenderer.scala, ParticleControllerControllerRenderer.scala, ParticleControllerRenderData.scala, ParticleControllerRenderer.scala, PointSpriteControllerRenderData.scala, PointSpriteRenderer.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All renderer classes and render data classes verified.

---

## Systematic Audit: graphics.g3d.particles.values

### CylinderSpawnShapeValue.scala, EllipseSpawnShapeValue.scala, GradientColorValue.scala, LineSpawnShapeValue.scala, MeshSpawnShapeValue.scala, NumericValue.scala, ParticleValue.scala, PointSpawnShapeValue.scala, PrimitiveSpawnShapeValue.scala, RangedNumericValue.scala, RectangleSpawnShapeValue.scala, ScaledNumericValue.scala, SpawnShapeValue.scala, UnweightedMeshSpawnShapeValue.scala, WeightMeshSpawnShapeValue.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All value classes verified. `read`/`write` (Json.Serializable) omitted across all, covered by ParticleEffectCodecs. Method bodies checked for `getScale`, `getColor`, `load`, `newLowValue`, `newHighValue` -- all match Java logic including boundary/break control flow. GradientColorValue.getColor uses companion `temp` array matching Java's static field.

---

## Systematic Audit: graphics.g3d.shaders

### BaseShader.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Line 169 `null.asInstanceOf[Renderable]` is correct -- Java passes `null` to global setters. All register/set/render/init methods verified.

### DefaultShader.scala
- **See high-suspicion section above**

### DepthShader.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods verified. Config inner class present. `createPrefix` matches Java logic. TODO preserved.

---

## Systematic Audit: graphics.g3d.utils

### AnimationController.scala, BaseAnimationController.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: `AnimationDesc`, `AnimationListener`, `Transform` inner classes verified. All `setAnimation`/`animate`/`queue`/`action` overloads present.

### BaseShaderProvider.scala, DefaultShaderProvider.scala, DepthShaderProvider.scala, ShaderProvider.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: Provider pattern verified.

### CameraInputController.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Inner `CameraGestureListener` verified. FIXME preserved.

### DefaultRenderableSorter.scala, RenderableSorter.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: FIXME "implement better sorting algorithm" preserved.

### DefaultTextureBinder.scala, TextureBinder.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: ROUNDROBIN/LRU strategies fully ported.

### FirstPersonCameraController.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All input handling methods verified.

### MeshBuilder.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All 100+ methods verified. FIXME comments preserved.

### MeshPartBuilder.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All 70+ method signatures verified. `VertexInfo` inner class complete. Deprecated methods preserved.

### ModelBuilder.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All `createBox`/`createCylinder`/`createCone`/`createSphere`/`createCapsule` etc verified.

### RenderContext.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All state management methods verified.

### ShapeCache.scala
- **Prior status**: pass
- **New status**: PASS

### TextureDescriptor.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: TODO preserved.

### TextureProvider.scala
- **Prior status**: pass
- **New status**: PASS

---

## Systematic Audit: graphics.g3d.utils.shapebuilders

### ArrowShapeBuilder.scala, BaseShapeBuilder.scala, BoxShapeBuilder.scala, CapsuleShapeBuilder.scala, ConeShapeBuilder.scala, CylinderShapeBuilder.scala, EllipseShapeBuilder.scala, FrustumShapeBuilder.scala, PatchShapeBuilder.scala, RenderableShapeBuilder.scala, SphereShapeBuilder.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All `build` overloads verified for each builder. FIXME comments preserved from Java.

---

## Systematic Audit: graphics.glutils (35 files)

### All 35 files
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: Every file verified via `re-scale enforce compare`. Method-level coverage confirmed. ShaderProgram has all uniform setters (1i-4i, 1f-4f, vectors, colors, matrices, arrays). ShapeRenderer has all drawing methods. GLFrameBuffer has complete builder hierarchy. Only 2 shortcut markers found (ShaderProgram lines 210/225: "not yet cached" comments about uniform location caching -- these are descriptive comments, not stubs).

Individual highlights:
- **ShapeRenderer.scala**: All drawing methods (`point`, `curve`, `triangle`, `rect`, `rectLine`, `box`, `x`, `arc`, `ellipse`, `cone`, `polygon`, `polyline`, `flush`) verified. `rect()` renamed to `rectangle()`.
- **GLFrameBuffer.scala**: All inner types (`FrameBufferTextureAttachmentSpec`, builder hierarchy) verified. `transfer()` method present.
- **ETC1.scala**: Native JNI calls replaced with `PlatformOps.etc1` delegation. All encode/decode/PKM methods present.

---

## Systematic Audit: graphics.profiling (7 files)

### GL20Interceptor.scala, GL30Interceptor.scala, GL31Interceptor.scala, GL32Interceptor.scala
- **Prior status**: all pass
- **New status**: all PASS
- **Notes**: All GL method interceptions verified. 142 GL20 methods, GL30/31/32 extensions all present.

### GLErrorListener.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: `LOGGING_LISTENER` and `THROWING_LISTENER` verified.

### GLInterceptor.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All fields, `reset()`, `resolveErrorNumber()`, abstract `check()` verified.

### GLProfiler.scala
- **Prior status**: pass
- **New status**: PASS
- **Notes**: All methods verified line-by-line against Java. Constructor GL level detection, `enable()`/`disable()` with GL instance replacement, all metric accessors, `reset()` -- all match Java logic. SGE correctly omits `Gdx.gl*` global updates (not needed with Sge() delegation).

---

## Summary of Status Changes

| File | Prior | New | Reason |
|------|-------|-----|--------|
| DirectionalLightsAttribute.scala | minor_issues | PASS | `compare` actually implemented (improvement over Java's `return 0`) |
| PointLightsAttribute.scala | minor_issues | PASS | Same as above |
| SpotLightsAttribute.scala | minor_issues | PASS | Same as above |

All other files: status confirmed unchanged (PASS).

## Risk Areas (Low Severity)

1. **DefaultShader.compareTo behavioral divergence**: The Scala port compares by `attributesMask` while Java returns 0. This is an improvement but represents a behavioral difference that could affect shader sorting order. Risk: low, since Java's behavior was explicitly marked as unimplemented.

2. **Orphan `Matrix3()` / `Vector3()` statements**: Lines 345 and 423 in DefaultShader have standalone constructor calls that create unused objects. These correspond to Java's `private final Matrix3 normalMatrix` and `private final Vector3 tmpV1` fields. Harmless but wasteful.

3. **ParticleEffectCodecs UnsupportedOperationExceptions**: 10 instances are all proper error handling for unknown types during deserialization, not stubs.

4. **BaseShader null casts**: Line 169 `null.asInstanceOf[Renderable]` is correct Java interop for global setter invocation, but worth documenting as an intentional violation of the no-null rule.
