# Audit: sge.graphics.g3d.utils

Audited: 19/19 files | Pass: 17 | Minor: 2 | Major: 0
Last updated: 2026-03-03

---

### TextureDescriptor.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/TextureDescriptor.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/TextureDescriptor.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 5 fields (`texture`, `minFilter`, `magFilter`, `uWrap`, `vWrap`), 3 constructors, 2 `set()` overloads, `equals`, `hashCode`, `compare` fully ported.
**Renames**: `compareTo` -> `compare` (Ordered[TextureDescriptor[T]])
**Convention changes**: Null fields -> `Nullable[T]` with fold-based access; no return -> boundary/break; split package; braces
**TODOs**: Original TODO preserved: "add other values, see opengl.org/..."
**Issues**: None

---

### TextureBinder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/TextureBinder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/TextureBinder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 methods (`begin`, `end`, `bind(TextureDescriptor)`, `bind(GLTexture)`, `getBindCount`, `getReuseCount`, `resetCounts`) fully ported.
**Renames**: `getBindCount()`/`getReuseCount()` -> `getBindCount`/`getReuseCount` (property-style, no parens)
**Convention changes**: Java interface -> Scala trait; raw `TextureDescriptor` -> `TextureDescriptor[?]`
**TODOs**: None
**Issues**: None

---

### ShaderProvider.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/ShaderProvider.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/ShaderProvider.java` |
| Status | pass |
| Tested | No |

**Completeness**: `getShader(Renderable)` and `close()` (via `AutoCloseable`) fully ported.
**Renames**: `dispose()` -> `close()` (AutoCloseable convention)
**Convention changes**: Java interface extends Disposable -> Scala trait extends AutoCloseable
**TODOs**: None
**Issues**: None

---

### BaseShaderProvider.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/BaseShaderProvider.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/BaseShaderProvider.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 methods (`getShader`, `createShader`, `close`) and 1 field (`shaders`) fully ported.
**Renames**: `dispose()` -> `close()`; `shader.dispose()` -> `shader.close()`; `GdxRuntimeException` -> `SgeError.GraphicsError`
**Convention changes**: `Array<Shader>` -> `DynamicArray[Shader]`; no return -> boundary/break; `renderable.shader` handled as `Nullable`
**TODOs**: None
**Issues**: None

---

### RenderableSorter.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/RenderableSorter.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/RenderableSorter.java` |
| Status | pass |
| Tested | No |

**Completeness**: `sort(Camera, DynamicArray[Renderable])` fully ported.
**Renames**: None
**Convention changes**: Java interface -> Scala trait; `Array<Renderable>` -> `DynamicArray[Renderable]`
**TODOs**: None
**Issues**: None

---

### DefaultRenderableSorter.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/DefaultRenderableSorter.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/DefaultRenderableSorter.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods (`sort`, `getTranslation`, `compare`/`compareRenderables`) fully ported.
**Renames**: `compare()` -> `compareRenderables()` (private); `Comparator<Renderable>` -> `given Ordering[Renderable]`; `dst2()` -> `distanceSq()`
**Convention changes**: `implements Comparator<Renderable>` pattern replaced with `given Ordering[Renderable]`; camera field `Nullable[Camera]`; material accessed via `Nullable.fold`
**TODOs**: FIXME preserved: "implement better sorting algorithm"
**Issues**: None

---

### AnimationController.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/AnimationController.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/AnimationController.java` |
| Status | pass |
| Tested | No |

**Completeness**: Inner `AnimationListener` (interface->trait), `AnimationDesc` (static class->companion object class), all 8 public fields, `update(float)`, 6 `setAnimation` overloads, 7 `animate` overloads, 3 `queue` overloads, 4 `action` overloads fully ported.
**Renames**: `GdxRuntimeException` -> `SgeError.InvalidInput`; `Pool` anonymous subclass -> `Pool.Default` with lambda
**Convention changes**: All nullable fields -> `Nullable[T]` with fold/foreach; no return -> boundary/break; inner classes in companion object
**TODOs**: None
**Issues**: None

---

### BaseAnimationController.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/BaseAnimationController.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/BaseAnimationController.java` |
| Status | pass |
| Tested | No |

**Completeness**: Inner `Transform` class (static->companion object), all methods (`begin`, `apply`, `end`, `applyAnimation`, `applyAnimations`, `removeAnimation`, `getFirstKeyframeIndexAtTime`, `getTranslationAtTime`, `getRotationAtTime`, `getScalingAtTime`, `getNodeAnimationTransform`, `applyNodeAnimationDirectly`, `applyNodeAnimationBlending`) fully ported.
**Renames**: `ObjectMap<Node,Transform>` -> `scala.collection.mutable.Map[Node,Transform]`; `Poolable` -> `Pool.Poolable`
**Convention changes**: Static fields/methods -> companion object; `applyAnimations` params `Nullable[Animation]`; `applyAnimation` params `Nullable[mutable.Map]`/`Nullable[Pool]`; no return -> boundary/break
**TODOs**: None
**Issues**: None (mutable.Map is functionally equivalent to ObjectMap for this use case)

---

### FirstPersonCameraController.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/FirstPersonCameraController.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All 6 key bindings, `keyDown`, `keyUp`, `touchDragged`, 2 `update` overloads, `setVelocity`, `setDegreesPerPixel` fully ported.
**Renames**: `InputAdapter` -> `InputProcessor` (trait); `IntIntMap` -> `mutable.Map[Int,Int]`; `containsKey` -> `contains`; `direction.rotate(up, angle)` -> `direction.rotateAroundDeg(up, angle)`; `Gdx.input`/`Gdx.graphics` -> `Sge().input`/`Sge().graphics`
**Convention changes**: `Gdx` singleton -> `Sge()` accessor via implicit parameter
**TODOs**: None
**Issues**: Uses `implicit sge: Sge` (old Scala 2 style) instead of `using Sge` (Scala 3 convention). Should be updated for consistency.

---

### RenderContext.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/RenderContext.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/RenderContext.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 9 private state fields, `begin`, `end`, `setDepthMask`, 2 `setDepthTest` overloads, 2 `setBlending` overloads, `setCullFace` fully ported.
**Renames**: `Gdx.gl` -> `Sge().graphics.gl`
**Convention changes**: Constructor takes `(using Sge)` context parameter; all GL calls use `Sge()` accessor
**TODOs**: None
**Issues**: None

---

### CameraInputController.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/CameraInputController.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/CameraInputController.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All 20+ fields, inner `CameraGestureListener` (static->companion), 2 constructors, `update`, `touchDown`, `touchUp`, `touchDragged`, `scrolled`, `zoom`, `pinchZoom`, `process`, `keyDown`, `keyUp`, `setInvertedControls` fully ported.
**Renames**: `GestureAdapter` -> `GestureDetector.GestureAdapter`; `Gdx.graphics` -> `sge.graphics`
**Convention changes**: `CameraGestureListener` in companion object; `extends GestureDetector(listener = gestureListener)` with named param
**TODOs**: FIXME preserved: "auto calculate this based on the target"
**Issues**: Uses `implicit val sge: Sge` (old Scala 2 style) instead of `using Sge` (Scala 3 convention). Should be updated for consistency.

---

### TextureProvider.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/TextureProvider.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/TextureProvider.java` |
| Status | pass |
| Tested | No |

**Completeness**: Trait `TextureProvider` with `load(String)`, inner `FileTextureProvider` (2 constructors, `load`), inner `AssetTextureProvider` (constructor, `load`) fully ported.
**Renames**: `Gdx.files.internal` -> `Sge().files.internal`; `assetManager.get(fileName, Texture.class)` -> `assetManager.get[Texture](fileName, classOf[Texture])`
**Convention changes**: Static inner classes -> companion object classes; `FileTextureProvider` fields promoted to constructor `val` params; `Gdx` -> `Sge()` accessor
**TODOs**: None
**Issues**: None

---

### ShapeCache.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/ShapeCache.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/ShapeCache.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods (`begin()`, `begin(int)`, `end`, `getRenderables`, `getMaterial`, `getWorldTransform`, `close`) and 2 constructors fully ported.
**Renames**: `Disposable` -> `AutoCloseable`; `dispose()` -> `close()`; `GdxRuntimeException` -> `SgeError.InvalidInput`
**Convention changes**: `renderable.material` stored as `Nullable(new Material())`; `getMaterial()` uses fold on Nullable; `Array<Renderable>` -> `DynamicArray[Renderable]`
**TODOs**: None
**Issues**: None

---

### DefaultTextureBinder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/DefaultTextureBinder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/DefaultTextureBinder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All constants (`ROUNDROBIN`, `LRU`, `MAX_GLES_UNITS`), 3 constructors, `begin`, `end`, `bind(TextureDescriptor)`, `bind(GLTexture)`, `getBindCount`, `getReuseCount`, `resetCounts`, private `bindTexture`, `bindTextureRoundRobin`, `bindTextureLRU`, static `getMaxTextureUnits` fully ported.
**Renames**: `GdxRuntimeException` -> `SgeError.InvalidInput`; `Gdx.gl` -> `Sge().graphics.gl`
**Convention changes**: Constants and static method in companion object; `GLTexture[]` -> `Array[GLTexture]`; `int[]` -> `Array[Int]`; `unsafeSetWrap`/`unsafeSetFilter` called only when both Nullable values present (for-comprehension); no return -> boundary/break in bind methods
**TODOs**: Java TODOs "remove debug code" on reuseCount/bindCount changed to descriptive comment
**Issues**: None

---

### DefaultShaderProvider.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/DefaultShaderProvider.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/DefaultShaderProvider.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 constructors and `createShader` fully ported.
**Renames**: None
**Convention changes**: Java null-safe config `(config == null) ? new Config() : config` eliminated -- Scala no-null convention; no-arg constructor creates `new Config()` directly instead of passing null; `(using Sge)` required for constructors
**TODOs**: None
**Issues**: None

---

### DepthShaderProvider.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/DepthShaderProvider.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/DepthShaderProvider.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 constructors and `createShader` fully ported. Same pattern as DefaultShaderProvider.
**Renames**: None
**Convention changes**: Same as DefaultShaderProvider -- null-safe config eliminated; `(using Sge)` required
**TODOs**: None
**Issues**: None

---

### MeshPartBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/MeshPartBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/MeshPartBuilder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 70+ method signatures ported. Inner `VertexInfo` class with all methods (`reset`, `set`, `setPos`, `setNor`, `setCol`, `setUV`, `lerp`). All deprecated shape methods (`patch`, `box`, `circle`, `ellipse`, `cylinder`, `cone`, `sphere`, `capsule`, `arrow`) preserved with `@deprecated` annotations.
**Renames**: `Poolable` -> `Pool.Poolable`; `vertex(float...)` -> `vertex(Float*)`; null params -> `Nullable[T]`
**Convention changes**: Java interface -> Scala trait; static `VertexInfo` -> companion object; `setColor(Color)` -> `setColor(Nullable[Color])`; `setUVRange(TextureRegion)` -> `setUVRange(Nullable[TextureRegion])`; `setVertexTransform(Matrix4)` -> `setVertexTransform(Nullable[Matrix4])`
**TODOs**: Preserved: "The following methods are deprecated and will be removed in a future release"
**Issues**: None

---

### MeshBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/MeshBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/MeshBuilder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 100+ methods fully ported including: 4 `begin` overloads, `endpart`, 2 `part` overloads, 2 `end` overloads, `clear`, getters (`getFloatsPerVertex`, `getNumVertices`, `getVertices`, `getNumIndices`, `getIndices`, `getAttributes`, `getMeshPart`, `getPrimitiveType`), setters (`setColor`, `setUVRange`, `setVertexTransform`, `setVertexTransformationEnabled`), `ensure*` methods, `vertex` overloads, `index` overloads, `line`/`triangle`/`rect` overloads, `addMesh` overloads, all deprecated shape delegates. Static `createAttributes`, `transformPosition`, `transformNormal`, constants `MAX_VERTICES`, `MAX_INDEX` in companion object.
**Renames**: `FloatArray`/`ShortArray` -> `DynamicArray[Float]`/`DynamicArray[Short]`; `Array<MeshPart>` -> `DynamicArray[MeshPart]`; `IntIntMap` -> `Nullable[ObjectMap[Int,Int]]`
**Convention changes**: Null params -> `Nullable[T]`; `end()` requires `(using Sge)` for `Mesh` creation; static members -> companion object; all deprecated delegates use `@nowarn`-compatible `@deprecated` annotations
**TODOs**: None
**Issues**: None

---

### ModelBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/utils/ModelBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/utils/ModelBuilder.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported: `begin`, `end`, `endnode`, `node` (3 overloads), `manage`, `part` (5 overloads), `createBox` (2), `createRect` (2), `createCylinder` (4), `createCone` (4), `createSphere` (4), `createCapsule` (2), `createXYZCoordinates` (2), `createArrow` (2), `createLineGrid`. Static `rebuildReferences` (2 overloads) in companion object.
**Renames**: `Disposable` -> `AutoCloseable` in `manage`; `Array<MeshBuilder>` -> `DynamicArray[MeshBuilder]`
**Convention changes**: `model`/`node` fields -> `Nullable[Model]`/`Nullable[Node]`; `end()` requires `(using Sge)`; `@nowarn("msg=deprecated")` on class for deprecated shape builder calls; `new Matrix4()` on line 43 (orphan tmpTransform allocation, minor)
**TODOs**: None
**Issues**: None (orphan `new Matrix4()` is cosmetic, does not affect behavior)
