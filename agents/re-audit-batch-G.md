# Re-Audit Batch G: GLTF, Maps, Net

**Auditor**: Opus 4.6 agent
**Date**: 2026-04-18
**Scope**: ~170 files across gltf.* (~115), maps.* (~43), net.* (~15)
**Method**: Glob/Read originals + ports, re-scale enforce compare --strict, re-scale enforce shortcuts, body-level comparison on all high-suspicion and complex files

---

## Summary of Findings

| Category | PASS | MINOR_ISSUES | MAJOR_ISSUES | NEEDS_DEEP_REVIEW |
|----------|------|-------------|-------------|-------------------|
| GLTF data (~42) | 42 | 0 | 0 | 0 |
| GLTF loaders (~30) | 28 | 2 | 0 | 0 |
| GLTF exporters (~11) | 10 | 1 | 0 | 0 |
| GLTF scene3d/attributes (~15) | 15 | 0 | 0 | 0 |
| GLTF scene3d/lights (~4) | 4 | 0 | 0 | 0 |
| GLTF scene3d/model (~7) | 7 | 0 | 0 | 0 |
| GLTF scene3d/scene (~10) | 7 | 1 | 2 | 0 |
| GLTF scene3d/shaders (~7) | 7 | 0 | 0 | 0 |
| GLTF scene3d/utils (~7) | 7 | 0 | 0 | 0 |
| Maps core (~9) | 9 | 0 | 0 | 0 |
| Maps objects (~8) | 8 | 0 | 0 | 0 |
| Maps tiled (~20) | 19 | 1 | 0 | 0 |
| Maps tiled/renderers (~6) | 6 | 0 | 0 | 0 |
| Maps tiled/tiles (~2) | 2 | 0 | 0 | 0 |
| Maps tiled/objects (~1) | 1 | 0 | 0 | 0 |
| Net (~15) | 14 | 0 | 0 | 1 |
| **Total** | **186** | **5** | **2** | **1** |

---

## HIGH-SUSPICION FILES (audited first)

### SceneSkybox.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/scene/SceneSkybox.java
- **Prior status**: pass
- **New status**: MAJOR_ISSUES
- **Missing methods**: SkyboxShader inner class (with init/bindMaterial overrides), SkyboxShaderProvider inner class (with createShader override)
- **Simplified methods**: `createShaderProvider` — uses plain `DefaultShaderProvider(shaderConfig)` instead of custom `SkyboxShaderProvider` that creates `SkyboxShader` instances. The SkyboxShader overrides `init()` to look up the `u_lod` uniform and `bindMaterial()` to set it from `lodBias`. The SkyboxShaderProvider overrides `createShader()` to prepend the fragment shader prefix (GLSL version, SRGB defines, LOD defines). Without these inner classes, the LOD bias feature (`lodBias` field) has no effect at runtime, and the SRGB/gamma prefix is never prepended to the fragment shader.
- **Missing branches**: none (all constructor variants present)
- **Mechanism changes without tests**: LOD rendering is silently broken
- **Notes**: Lines 99: `this.shaderProvider = DefaultShaderProvider(shaderConfig)` should be `SkyboxShaderProvider(shaderConfig, sb.toString)`. The `lodBias` field (line 29) exists but is dead code without the SkyboxShader uniform binding.

### SceneManager.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/scene/SceneManager.java
- **Prior status**: pass
- **New status**: MAJOR_ISSUES
- **Missing methods**: none
- **Simplified methods**: `removeScene` — light removal body is stubbed with `// TODO: proper light removal from environment` (line 321). Original Java iterates `scene.lights` entries and calls `environment.remove(e.value)` for each.
- **Missing branches**: none
- **Mechanism changes without tests**: Light leaks when scenes are removed
- **Notes**: Lines 317-323: The `removeScene` method removes the scene from `renderableProviders` but does NOT remove its lights from the environment. The original Java (line 383-388) does `for(Entry<Node, BaseLight> e : scene.lights) { environment.remove(e.value); }`. The `renderTransmission` method (lines 221-232) iterates renderableProviders individually via `transmissionSource.render(renderableProviders(i), environment)` rather than `transmissionSource.render(renderableProviders, environment)` — this is a minor difference but functionally equivalent.

### IBLBuilder.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/utils/IBLBuilder.java
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: Complete port. All 3 factory methods (createOutdoor, createIndoor, createCustom), buildEnvMap, buildIrradianceMap, buildRadianceMap, renderGradient, renderLights, Light inner class with both render overloads, close/dispose — all present and body-equivalent. The XXX comments (lines 280, 286) are from the original Java source. Method comparison: `buildRadianceMap` — identical loop structure with `while` replacing `for`, `Pixels()` wrapping, `ClearMask.ColorBufferBit` replacing `GL20.GL_COLOR_BUFFER_BIT`, `close()` replacing `dispose()`.

### GLTFExporter.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/exporters/GLTFExporter.java
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Mechanism changes without tests**: `end()` method uses `GLTFExporterJson.writeGltf(root)` instead of LibGDX `Json` class — intentional architecture change (SGE has no LibGDX Json). GLTFExporterJson.scala is an SGE-original replacement.
- **Notes**: All 8 export variants present (exportMesh, exportModel, exportScene, exportSceneModel, exportAsset, exportSceneModels, exportScenes + private overloads). The `getImageName` visibility changed from `protected` to `private[exporters]`. Method `exportNodes` comparison: identical recursive structure with `while` loops, Nullable wrapping, `indexOfByRef` replacing `indexOf(obj, true)`.

### TideMapLoader.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/maps/tiled/TideMapLoader.java
- **Prior status**: minor_issues
- **New status**: MINOR_ISSUES
- **Missing methods**: none
- **Simplified methods**: none — `getRelativeFileHandle` delegates to `BaseTiledMapLoader.getRelativeFileHandle` (shared implementation)
- **Missing branches**: none
- **Mechanism changes without tests**: none
- **Notes**: The TODO at line 290 (`// TODO: Reuse existing animated tiles`) is from the original Java (line 261). Minor: `loadTileSheet` skips reading `Description` child text (Java reads it but never uses the value). The `loadTileSheet` wraps tile creation in `texture.foreach` for null safety — behavior differs only when imageResolver returns null (throws NPE in Java, silently skips in Scala). All 8 methods present.

---

## GLTF DATA CLASSES (~42 files)

### GLTF.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTF.java
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Simplified methods**: none
- **Missing branches**: none
- **Notes**: Data class with public fields. All fields present.

### GLTFAsset.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTFAsset.java
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: Simple data class, all fields present (version, generator, minVersion, copyright).

### GLTFEntity.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTFEntity.java
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Base class with name field and extensions map. Ported correctly.

### GLTFExtras.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTFExtras.java
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Simple data holder class.

### GLTFObject.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTFObject.java
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Extends GLTFEntity, adds extras field.

### GLTFExtensions.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTFExtensions.java
- **Prior status**: pass
- **New status**: PASS
- **Notes**: Extensions container class.

### GLTFAnimation.scala through GLTFAnimationTarget.scala (4 files)
- **Original**: animation sub-package
- **New status**: PASS (all 4)
- **Notes**: Simple data classes with typed fields. All fields match.

### GLTFCamera.scala, GLTFOrthographic.scala, GLTFPerspective.scala (3 files)
- **Original**: camera sub-package
- **New status**: PASS (all 3)
- **Notes**: Camera data classes with numeric fields.

### GLTFAccessor.scala through GLTFBufferView.scala (6 files)
- **Original**: data sub-package
- **New status**: PASS (all 6)
- **Notes**: Accessor, buffer, and sparse data classes.

### KHRLightsPunctual.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/extensions/KHRLightsPunctual.java
- **Prior status**: pass
- **New status**: PASS
- **Missing methods**: none
- **Notes**: TODO at line 74 is from original Java source.

### KHRMaterialsEmissiveStrength.scala through KHRTextureTransform.scala (9 extension files)
- **Original**: extensions sub-package
- **New status**: PASS (all 9)
- **Notes**: Simple data classes with typed fields.

### GLTFMesh.scala, GLTFMorphTarget.scala, GLTFPrimitive.scala (3 geometry files)
- **New status**: PASS (all 3)

### GLTFMaterial.scala, GLTFpbrMetallicRoughness.scala (2 material files)
- **New status**: PASS (both)

### GLTFNode.scala, GLTFScene.scala, GLTFSkin.scala (3 scene files)
- **New status**: PASS (all 3)

### GLTFImage.scala through GLTFTextureInfo.scala (6 texture files)
- **New status**: PASS (all 6)

### GLTFCodecs.scala
- **Original**: N/A (SGE-original — JSON codec support for GLTF data classes)
- **New status**: PASS
- **Notes**: SGE-original file providing JSON serialization for GLTF data types.

---

## GLTF LOADERS (~30 files)

### GLTFIllegalException.scala, GLTFRuntimeException.scala, GLTFUnsupportedException.scala
- **New status**: PASS (all 3)
- **Notes**: Simple exception classes.

### BinaryDataFileResolver.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/loaders/glb/BinaryDataFileResolver.java
- **New status**: PASS
- **Notes**: GLB binary data resolution.

### GLBAssetLoader.scala, GLBLoader.scala
- **New status**: PASS (both)

### GLTFAssetLoader.scala, GLTFLoader.scala
- **New status**: PASS (both)

### GLTFJsonParser.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/loaders/gltf/GLTFJsonParser.java
- **New status**: PASS
- **Notes**: JSON parsing for GLTF files.

### SeparatedDataFileResolver.scala
- **New status**: PASS

### GLTFLoaderBase.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/GLTFLoaderBase.java
- **New status**: MINOR_ISSUES
- **Missing methods**: none
- **Notes**: XXX comment at line 122 is from original. TODO at line referencing imageResolver is from original Java too. All methods present. Compare shows `dispose` -> `close` rename.

### GLTFTypes.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/GLTFTypes.java
- **New status**: MINOR_ISSUES
- **Notes**: TODO at line 164 is from original Java source. All type constants and conversion methods present.

### SceneAssetLoaderParameters.scala
- **New status**: PASS

### AnimationLoader.scala, Interpolation.scala (loaders/shared/animation)
- **New status**: PASS (both)

### AccessorBuffer.scala, DataFileResolver.scala, DataResolver.scala (loaders/shared/data)
- **New status**: PASS (all 3)

### MeshLoader.scala, MeshSpliter.scala, MeshTangentSpaceGenerator.scala (loaders/shared/geometry)
- **New status**: PASS (all 3)

### MaterialLoader.scala, MaterialLoaderBase.scala, PBRMaterialLoader.scala (loaders/shared/material)
- **New status**: PASS (all 3)

### NodeResolver.scala, SkinLoader.scala (loaders/shared/scene)
- **New status**: PASS (both)

### ImageResolver.scala, PixmapBinaryLoaderHack.scala, TextureResolver.scala (loaders/shared/texture)
- **New status**: PASS (all 3)

### BlenderShapeKeys.scala (loaders/blender)
- **New status**: PASS

---

## GLTF EXPORTERS (~11 files)

### GLTFExporter.scala
- See HIGH-SUSPICION section above. **PASS**.

### GLTFExporterConfig.scala
- **New status**: PASS
- **Notes**: Simple config class with boolean flags.

### GLTFExporterJson.scala
- **Original**: N/A (SGE-original — replaces LibGDX Json serialization)
- **New status**: PASS
- **Notes**: SGE-original JSON writer for GLTF export.

### GLTFAnimationExporter.scala
- **New status**: PASS

### GLTFBinaryExporter.scala
- **New status**: PASS

### GLTFCameraExporter.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/exporters/GLTFCameraExporter.java
- **New status**: MINOR_ISSUES
- **Notes**: 5 TODO comments (lines 41, 44, 45, 52, 53) are all from original Java. All methods present.

### GLTFExportTypes.scala
- **New status**: PASS

### GLTFLightExporter.scala
- **New status**: PASS

### GLTFMaterialExporter.scala
- **New status**: PASS

### GLTFMeshExporter.scala
- **New status**: PASS
- **Notes**: TODO at line 86 is from original. Complete port including bone weight/joint export, morph targets, bounds computation, and primitive mode mapping. Method comparison verified: `exportMesh` — identical attribute dispatch chain (Position/Normal/Tangent/ColorUnpacked/TextureCoordinates/PositionTarget/NormalTarget/TangentTarget/BoneWeight), bone export loop structure matches with proper Nullable handling replacing null checks.

### GLTFSkinExporter.scala
- **New status**: PASS

---

## GLTF SCENE3D/ATTRIBUTES (~15 files)

All 15 attribute files pass. The `register` calls for type constants use `Attribute.register()` in companion objects. The `compareTo` method is ported as `compare` (Scala's `Ordered[Attribute]` convention).

### CascadeShadowMapAttribute.scala through PBRVolumeAttribute.scala
- **New status**: PASS (all 15)
- **Notes**: PBRTextureAttribute includes `compare` override with rotationUV comparison matching original. All type constants registered. All `copy()` methods present.

---

## GLTF SCENE3D/LIGHTS (~4 files)

### DirectionalLightEx.scala, DirectionalShadowLight.scala, PointLightEx.scala, SpotLightEx.scala
- **New status**: PASS (all 4)

---

## GLTF SCENE3D/MODEL (~7 files)

### CubicQuaternion.scala through WeightVector.scala
- **New status**: PASS (all 7)

---

## GLTF SCENE3D/SCENE (~10 files)

### SceneSkybox.scala
- See HIGH-SUSPICION section. **MAJOR_ISSUES**.

### SceneManager.scala
- See HIGH-SUSPICION section. **MAJOR_ISSUES**.

### Scene.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/scene/Scene.java
- **New status**: PASS
- **Notes**: All factory methods and the `initFromSceneModel` logic present.

### SceneAsset.scala
- **New status**: PASS

### SceneModel.scala
- **New status**: PASS

### SceneRenderableSorter.scala
- **New status**: PASS

### CascadeShadowMap.scala
- **New status**: PASS

### MirrorSource.scala
- **New status**: PASS

### TransmissionSource.scala
- **New status**: PASS

### Updatable.scala
- **New status**: PASS

---

## GLTF SCENE3D/SHADERS (~7 files)

### PBRShader.scala
- **Original**: original-src/gdx-gltf/gltf/src/net/mgsx/gltf/scene3d/shaders/PBRShader.java
- **New status**: PASS
- **Notes**: All ~40 uniform register calls present. All setter/uniform definitions match. `computeVertexColorLayers`, `render`, `bindMaterial` methods present. The `register` method is inherited from DefaultShader base class.

### PBRShaderProvider.scala
- **New status**: PASS
- **Notes**: `createShader`, `createPrefix`, factory methods (createDefault, createDefaultDepth) all present.

### PBRShaderConfig.scala, PBRCommon.scala, PBRDepthShader.scala, PBRDepthShaderProvider.scala, PBREmissiveShaderProvider.scala
- **New status**: PASS (all 5)

---

## GLTF SCENE3D/UTILS (~7 files)

### IBLBuilder.scala
- See HIGH-SUSPICION section. **PASS**.

### EnvironmentCache.scala, EnvironmentUtil.scala, FacedMultiCubemapData.scala, LightUtils.scala, MaterialConverter.scala, ShaderParser.scala
- **New status**: PASS (all 6)

---

## MAPS CORE (~9 files)

### Map.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/maps/Map.java
- **New status**: PASS

### MapObject.scala
- **New status**: PASS

### MapRenderer.scala
- **New status**: PASS

### ImageResolver.scala
- **New status**: PASS

### MapGroupLayer.scala
- **New status**: PASS

### MapLayer.scala
- **New status**: PASS

### MapLayers.scala
- **New status**: PASS

### MapObjects.scala
- **New status**: PASS

### MapProperties.scala
- **New status**: PASS
- **Notes**: `getKeys` -> `keys`, `getValues` -> `values` standard renames.

---

## MAPS OBJECTS (~8 files)

### CircleMapObject.scala through TextureMapObject.scala
- **New status**: PASS (all 8)
- **Notes**: Simple wrapper classes around geometric shapes.

---

## MAPS TILED (~20 files)

### TideMapLoader.scala
- See HIGH-SUSPICION section. **MINOR_ISSUES**.

### BaseTiledMapLoader.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/maps/tiled/BaseTiledMapLoader.java
- **New status**: PASS
- **Notes**: `return-comment` shortcut hit is a false positive — it's a code comment containing the word "return".

### BaseTmxMapLoader.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/maps/tiled/BaseTmxMapLoader.java
- **New status**: PASS
- **Notes**: Compare shows 27 "missing" items which are all imported types (AnimatedTiledMapTile, Element, etc.) and utility methods (createTileLayerCell, getRelativeFileHandle) that are present but named differently or inlined.

### BaseTmjMapLoader.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/maps/tiled/BaseTmjMapLoader.java
- **New status**: PASS
- **Notes**: Same pattern as BaseTmxMapLoader — all methods present.

### TiledMap.scala
- **New status**: PASS

### TiledMapImageLayer.scala
- **New status**: PASS

### TiledMapLoader.scala
- **New status**: PASS

### TiledMapRenderer.scala
- **New status**: PASS

### TiledMapTile.scala
- **New status**: PASS

### TiledMapTileLayer.scala
- **New status**: PASS

### TiledMapTileSet.scala
- **New status**: PASS

### TiledMapTileSets.scala
- **New status**: PASS

### TmxMapLoader.scala
- **New status**: PASS

### TmjMapLoader.scala
- **New status**: PASS

### AtlasTmxMapLoader.scala
- **New status**: PASS

### AtlasTmjMapLoader.scala
- **New status**: PASS

### TiledProjectJson.scala
- **Original**: N/A (SGE-original or new LibGDX addition)
- **New status**: PASS

### TmjJson.scala
- **Original**: N/A (SGE-original JSON support for TMJ format)
- **New status**: PASS

### TiledMapTileMapObject.scala (tiled/objects)
- **New status**: PASS

---

## MAPS TILED/RENDERERS (~6 files)

### BatchTiledMapRenderer.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/maps/tiled/renderers/BatchTiledMapRenderer.java
- **New status**: PASS
- **Notes**: `dispose` -> `close`, `getBatch`/`getMap`/`getUnitScale`/`getViewBounds` -> `batch`/`map`/`unitScale`/`viewBounds` standard renames.

### OrthoCachedTiledMapRenderer.scala
- **New status**: PASS
- **Notes**: `dispose` -> `close`, `getSpriteCache` -> direct access.

### OrthogonalTiledMapRenderer.scala
- **New status**: PASS

### IsometricTiledMapRenderer.scala
- **New status**: PASS

### IsometricStaggeredTiledMapRenderer.scala
- **New status**: PASS

### HexagonalTiledMapRenderer.scala
- **New status**: PASS

---

## MAPS TILED/TILES (~2 files)

### StaticTiledMapTile.scala
- **New status**: PASS

### AnimatedTiledMapTile.scala
- **New status**: PASS

---

## NET (~15 files)

### HttpParametersUtils.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/HttpParametersUtils.java
- **New status**: PASS
- **Notes**: Complete port. `convertHttpParameters` and `encode` methods match.

### HttpRequestHeader.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/HttpRequestHeader.java
- **New status**: PASS
- **Notes**: All 28 header constants present.

### HttpResponseHeader.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/HttpResponseHeader.java
- **New status**: PASS
- **Notes**: All 30 header constants present.

### HttpStatus.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/HttpStatus.java
- **New status**: PASS
- **Notes**: Opaque type `HttpStatus = Int` — all 40+ status code constants present.

### ServerSocket.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/ServerSocket.java
- **New status**: PASS
- **Notes**: `getProtocol` -> `protocol`. `Disposable` -> `AutoCloseable`.

### Socket.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/Socket.java
- **New status**: PASS
- **Notes**: `isConnected`, `inputStream`, `outputStream`, `remoteAddress` — all present.

### ServerSocketHints.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/ServerSocketHints.java
- **New status**: PASS
- **Notes**: All 6 fields match with identical defaults.

### SocketHints.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/SocketHints.java
- **New status**: PASS
- **Notes**: All 10 fields match with identical defaults.

### NetJavaSocketImpl.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/NetJavaSocketImpl.java
- **New status**: PASS
- **Notes**: `dispose` -> `close`, getter renames. All socket configuration logic present.

### NetJavaServerSocketImpl.scala
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/NetJavaServerSocketImpl.java
- **New status**: PASS

### SgeHttpClient.scala
- **Original**: N/A (SGE-original — replaces NetJavaImpl.java)
- **New status**: NEEDS_DEEP_REVIEW
- **Notes**: Intentional architecture divergence. LibGDX's NetJavaImpl uses HttpURLConnection + ThreadPoolExecutor. SGE's SgeHttpClient uses sttp backend + Scala Future. The API surface is different (obtainRequest/send/cancel/isPending vs sendHttpRequest/cancelHttpRequest/isHttpRequestPending). The noop backend throws UnsupportedOperationException (intentional for testing). Need to verify that the sttp-based implementation handles all the edge cases that NetJavaImpl handles (content stream, output writing, error stream fallback).

### SgeHttpRequest.scala
- **Original**: N/A (SGE-original — replaces LibGDX Net.HttpRequest inner class)
- **New status**: PASS
- **Notes**: Poolable mutable request with fluent API. Covers method, url, content, contentBytes, timeoutMs, followRedirects, headers.

### SgeHttpResponse.scala
- **Original**: N/A (SGE-original — replaces NetJavaImpl.HttpClientResponse)
- **New status**: PASS
- **Notes**: Wraps sttp Response. Provides result, resultAsString, resultAsStream, status, getHeader, headers.

### HttpBackendFactory.scala
- **Original**: N/A (SGE-original)
- **New status**: PASS

### SttpTypes.scala
- **Original**: N/A (SGE-original — sttp type aliases)
- **New status**: PASS

### HttpRequestBuilder.java — NOT PORTED
- **Original**: original-src/libgdx/gdx/src/com/badlogic/gdx/net/HttpRequestBuilder.java
- **Notes**: This file has no SGE equivalent. It provides a builder pattern for HttpRequest objects with utility methods (basicAuthentication, jsonContent, formEncodedContent, etc.). SGE uses SgeHttpRequest with fluent setters instead, which covers the basic use cases but does not have the convenience methods like `basicAuthentication()`, `jsonContent()`, `formEncodedContent()`. This may be an intentional omission since SgeHttpRequest is a different design. Should be verified with the project maintainer.

---

## CRITICAL GAPS REQUIRING FIXES

### 1. SceneSkybox.scala — Missing SkyboxShader/SkyboxShaderProvider inner classes (MAJOR)
**Impact**: The `lodBias` field is dead code. LOD-based skybox rendering does not work. SRGB/gamma correction shader prefixes are not applied.
**Fix**: Port the `SkyboxShader` inner class (extends DefaultShader, overrides `init()` + `bindMaterial()`) and `SkyboxShaderProvider` inner class (extends DefaultShaderProvider, overrides `createShader()`). Update `createShaderProvider` to instantiate `SkyboxShaderProvider` instead of `DefaultShaderProvider`.

### 2. SceneManager.removeScene — Light removal not implemented (MAJOR)
**Impact**: When a scene is removed, its lights remain in the environment, causing visual artifacts.
**Fix**: Implement the body of `removeScene` to iterate `scene.lights.foreachValue` and call `environment.remove(light)` for each light, matching the original Java behavior.
