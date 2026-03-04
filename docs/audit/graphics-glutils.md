# Audit: sge.graphics.glutils

Audited: 35/35 files | Pass: 25 | Minor: 8 | Major: 2
Last updated: 2026-03-04

---

### CustomTexture3DData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/CustomTexture3DData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/CustomTexture3DData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods ported. `getPixels()`, `consume3DData()`, accessors all present.
**Renames**: `dispose()` -> `close()` (inherits from Texture3DData)
**Convention changes**: Constructor params are private vals; GL constant references use `GL20`/`GL30` from `sge.graphics`. Uses `Nullable(pixels).isEmpty` instead of `pixels == null` for `isManaged`.
**TODOs**: None
**Issues**: None

---

### ETC1.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/ETC1.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/ETC1.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods/constants ported. `ETC1Data` inner class faithful. Native JNI calls replaced with `PlatformOps.etc1` delegation through `Array[Byte]` adapter helpers.
**Renames**: `dispose()` -> `close()` on ETC1Data; `ETC1Data(FileHandle)` constructor -> companion `ETC1Data.apply(FileHandle)` factory.
**Convention changes**: ByteBuffer-based JNI calls replaced with Array[Byte] intermediaries via `extractBytes`/`putBytes` adapters. Uses `PlatformOps.buffer.newDisposableByteBuffer` for native alloc.
**TODOs**: None
**Issues**: None

---

### ETC1TextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/ETC1TextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/ETC1TextureData.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All TextureData interface methods ported.
**Renames**: `dispose()` -> `close()` on inner ETC1Data
**Convention changes**: Fields use `Nullable[A]` instead of raw null.
**TODOs**: `consumeCustomData` body is **completely stubbed out** as a TODO comment block. The Java source has full GL texture upload logic with `glTexImage2D`, `glCompressedTexImage2D`, and `MipMapGenerator.generateMipMap` calls. This makes ETC1TextureData non-functional.
**Issues**:
- `consumeCustomData()` is a no-op stub -- textures using ETC1 compression will silently fail
- Needs `(using Sge)` to access `Sge().graphics` for GL calls

---

### FacedCubemapData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FacedCubemapData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FacedCubemapData.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All constructors and public methods ported.
**Renames**: `dispose()` -> `close()` on pixmaps
**Convention changes**: `data` array is `Array[Nullable[TextureData]]` instead of `TextureData[]` with null entries. Uses `Nullable.fold` / `Nullable.foreach` idioms.
**TODOs**: None
**Issues**:
- Raw `null` passed as `Format` argument to `PixmapTextureData` constructor (14 occurrences) -- should use `Nullable.empty` or the Nullable-accepting overload

---

### FileTextureArrayData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FileTextureArrayData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FileTextureArrayData.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All TextureArrayData interface methods ported.
**Renames**: None
**Convention changes**: Constructor uses varargs `files: FileHandle*` instead of `FileHandle[]`. Uses `boundary`/`break` for early return in `isManaged`.
**TODOs**: None
**Issues**:
- Missing second constructor `(Format, Boolean, TextureData[])` from Java source -- only the FileHandle-based constructor exists
- `orNull` usage on line 73 for `gl30` -- should be wrapped with `@nowarn` (it already is)
- `prepare()` always calls `data.prepare()` -- Java skips already-prepared data with `if (!data.isPrepared())` guard

---

### FileTextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FileTextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FileTextureData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods ported. Constructor uses default params instead of single 4-arg constructor.
**Renames**: None
**Convention changes**: Fields use `Nullable[Pixmap]` / `Nullable[Format]` instead of null. `format` defaults to `Format.RGBA8888` via `getOrElse`.
**TODOs**: None
**Issues**: None

---

### FloatFrameBuffer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FloatFrameBuffer.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FloatFrameBuffer.java` |
| Status | pass |
| Tested | No |

**Completeness**: All constructors and `createTexture` override ported.
**Renames**: None
**Convention changes**: Uses `GL20.GL_RGBA` and `GL20.GL_FLOAT` where Java uses `GL30.GL_RGBA` / `GL30.GL_FLOAT` (values are identical).
**TODOs**: None
**Issues**: None

---

### FloatTextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FloatTextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FloatTextureData.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All TextureData methods ported. Extra `getBuffer()` method present.
**Renames**: None
**Convention changes**: `buffer` is `Nullable[FloatBuffer]`. Uses `@nowarn` for `orNull` at Java interop boundaries (GL null buffer).
**TODOs**: None
**Issues**:
- Java `prepare()` only checks GL format for `amountOfFloats` when `GLVersion.Type == OpenGL` -- Scala version always checks all formats regardless of GL type. Minor behavioral difference.
- `getBuffer()` returns `Nullable[FloatBuffer]` instead of raw `FloatBuffer` -- callers may need adjustment

---

### FrameBuffer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FrameBuffer.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FrameBuffer.java` |
| Status | pass |
| Tested | No |

**Completeness**: All constructors and `createTexture` override ported. Companion `unbind()` present.
**Renames**: None
**Convention changes**: Uses `(using Sge)` context parameter. Handles WebGL depth texture workaround.
**TODOs**: None
**Issues**: None

---

### FrameBufferCubemap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/FrameBufferCubemap.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/FrameBufferCubemap.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All constructors, `createTexture`, `bind`, `nextSide`, `getSide` ported.
**Renames**: `dispose()` -> `close()` inherited
**Convention changes**: Uses `(using Sge)` context parameter.
**TODOs**: None
**Issues**:
- `getSide()` on line 141 returns raw `null` instead of `Nullable` when `currentSide < 0` -- violates no-null convention

---

### GLFrameBuffer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/GLFrameBuffer.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/GLFrameBuffer.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods ported including `begin`, `end`, `bind`, `close`, `transfer`, builder hierarchy. Inner types `FrameBufferTextureAttachmentSpec`, `FrameBufferRenderBufferAttachmentSpec`, `GLFrameBufferBuilder`, `FrameBufferBuilder`, `FloatFrameBufferBuilder`, `FrameBufferCubemapBuilder` all present.
**Renames**: `dispose()` -> `close()`; `unbind()` moved to companion
**Convention changes**: Uses `DynamicArray` for managed buffer tracking. Builder pattern uses Scala 3 type parameters.
**TODOs**: None
**Issues**: None

---

### GLOnlyTextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/GLOnlyTextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/GLOnlyTextureData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All TextureData methods ported.
**Renames**: None
**Convention changes**: Uses `(using Sge)` for GL calls. Raw `null` in `glTexImage2D` call (Java interop for GPU-only texture).
**TODOs**: None
**Issues**: None -- `null` in `glTexImage2D` is correct Java interop for GPU-only allocation

---

### GLVersion.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/GLVersion.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/GLVersion.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods and inner `Type` enum ported.
**Renames**: Inner `Type` enum moved to companion object.
**Convention changes**: Pattern match instead of if-else chain for type detection. `TAG` field removed.
**TODOs**: None
**Issues**:
- Logging calls in `extractVersion` and `parseInt` are commented out instead of using `Sge().application.log(...)` -- missing error logging on invalid version strings
- Java `NONE` case sets `vendorString = ""` and `rendererString = ""` -- Scala retains the original constructor args, slight behavior difference

---

### HdpiMode.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/HdpiMode.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/HdpiMode.java` |
| Status | pass |
| Tested | No |

**Completeness**: Both enum values `Logical` and `Pixels` ported.
**Renames**: None
**Convention changes**: Scala 3 `enum` instead of Java `enum`.
**TODOs**: None
**Issues**: None

---

### HdpiUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/HdpiUtils.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/HdpiUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 static methods ported as object methods.
**Renames**: None
**Convention changes**: Uses `(using Sge)` context parameter instead of `Gdx.` statics.
**TODOs**: None
**Issues**: None

---

### ImmediateModeRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/ImmediateModeRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/ImmediateModeRenderer.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 12 interface methods ported 1:1 as trait methods.
**Renames**: None
**Convention changes**: Java `interface` -> Scala `trait`. `dispose()` kept as-is (not `close()`) since this is a rendering interface, not a resource handle.
**TODOs**: None
**Issues**: None

---

### ImmediateModeRenderer20.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/ImmediateModeRenderer20.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/ImmediateModeRenderer20.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods ported. Companion `createDefaultShader`, `createVertexShader`, `createFragmentShader` all present.
**Renames**: None
**Convention changes**: Uses `DynamicArray[VertexAttribute]` for builder. `close()` delegates to `dispose()`.
**TODOs**: None
**Issues**: None

---

### IndexArray.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/IndexArray.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/IndexArray.java` |
| Status | pass |
| Tested | No |

**Completeness**: All IndexData methods ported 1:1.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `asInstanceOf[Buffer]` casts for JDK9+ `Buffer.flip()`/`Buffer.position()` compatibility.
**TODOs**: None
**Issues**: None

---

### IndexBufferObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/IndexBufferObject.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/IndexBufferObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 constructors and all IndexData methods ported.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `(using sde: Sge)` instead of `Gdx.gl20` statics.
**TODOs**: None
**Issues**: None

---

### IndexBufferObjectSubData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/IndexBufferObjectSubData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/IndexBufferObjectSubData.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All IndexData methods ported. Both constructors present.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `sge.graphics.glutils` flat package instead of split package.
**TODOs**: None
**Issues**:
- Package declaration uses `package sge.graphics.glutils` (flat) instead of the project convention `package sge` / `package graphics` / `package glutils` (split)

---

### IndexData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/IndexData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/IndexData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 9 interface methods ported 1:1.
**Renames**: `Disposable` -> `AutoCloseable`; `dispose()` -> `close()`
**Convention changes**: Java `interface` -> Scala `trait`.
**TODOs**: None
**Issues**: None

---

### InstanceBufferObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/InstanceBufferObject.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/InstanceBufferObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: All InstanceData methods ported. Bind/unbind handle instanced attribute divisors correctly.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `(implicit sge: Sge)` (older style) instead of `(using Sge)`.
**TODOs**: None
**Issues**: None -- the `implicit` vs `using` difference is cosmetic

---

### InstanceBufferObjectSubData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/InstanceBufferObjectSubData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/InstanceBufferObjectSubData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All InstanceData methods ported. `getBufferHandle()` accessor present.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `(implicit sge: Sge)`.
**TODOs**: None
**Issues**: None

---

### InstanceData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/InstanceData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/InstanceData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 12 interface methods ported 1:1.
**Renames**: `Disposable` -> `AutoCloseable`; `dispose()` -> `close()`
**Convention changes**: Java `interface` -> Scala `trait`. `int[]` params -> `Nullable[Array[Int]]`.
**TODOs**: None
**Issues**: None

---

### KTXTextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/KTXTextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/KTXTextureData.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods ported: `prepare`, `consumeCustomData`, `consumeCubemapData`, `disposePreparedData`, `getData`, accessors.
**Renames**: `dispose()` -> `close()` on internal data
**Convention changes**: Uses `Nullable[ByteBuffer]` for `compressedData`. Static GL constants moved to companion object.
**TODOs**: None
**Issues**:
- `useMipMapsParam` is a `val` constructor parameter but Java can reassign `useMipMaps = true` on line 125 when `numberOfMipmapLevels == 0`. The commented-out line 119 acknowledges this. MipMap auto-generation may not trigger correctly in this edge case.
- Line 274: `useMipMaps` reference in `consumeCustomData` calls the override `def useMipMaps: Boolean = useMipMapsParam`, which cannot reflect the mutation.

---

### MipMapGenerator.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/MipMapGenerator.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/MipMapGenerator.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: **Heavily stubbed**. Java has full implementation with `generateMipMapCPU`, `generateMipMapGLES20`, `generateMipMapDesktop`, and `setUseHardwareMipMap` methods.
**Renames**: None
**Convention changes**: Flat package declaration `package sge.graphics.glutils` instead of split.
**TODOs**: Both methods are stub implementations (empty body / returns `Array(pixmap)`).
**Issues**:
- `generateMipMap(target, pixmap, w, h)` has empty body -- no GL texture upload happens
- `generateMipMapChain` returns only the original pixmap -- no mipmap chain generation
- Missing `generateMipMap(pixmap, w, h)` 2-arg overload (without target)
- Missing `setUseHardwareMipMap(Boolean)` method
- Missing all private helper methods (`generateMipMapCPU`, `generateMipMapGLES20`, `generateMipMapDesktop`)
- Missing `useHWMipMap` field
- Package uses flat format instead of split

---

### MipMapTextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/MipMapTextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/MipMapTextureData.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All TextureData methods ported.
**Renames**: None
**Convention changes**: Varargs constructor. `mips` is `Array[TextureData]`.
**TODOs**: None
**Issues**:
- Constructor takes `(implicit sge: Sge)` but the Java source does not require any application context. The `Sge` context is unused and unnecessary.

---

### PixmapTextureData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/PixmapTextureData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/PixmapTextureData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All TextureData methods ported. Both constructors present with Nullable format handling.
**Renames**: `managed` -> `isManaged` (val param)
**Convention changes**: Private primary constructor with public secondary constructors. `Nullable[Format]` for nullable format parameter.
**TODOs**: None
**Issues**: None

---

### ShaderProgram.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/ShaderProgram.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/ShaderProgram.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: Nearly complete. All uniform setter methods (1i through 4i, 1f through 4f, vectors, colors, matrices), vertex attribute methods, bind/dispose, managed shader tracking.
**Renames**: `dispose()` -> `close()`; `begin()`/`end()` methods removed (Java has them but they just call `bind()` -- Scala only has `bind()`)
**Convention changes**: Uses `(using Sge)` context parameter. `ObjectMap` for uniform/attribute maps.
**TODOs**: None
**Issues**:
- Missing `setUniform1iv`, `setUniform2iv`, `setUniform3iv`, `setUniform4iv` (integer array uniform setters) -- Java has 8 methods (name + location overloads for each)
- Missing `begin()` / `end()` convenience aliases (Java `begin()` calls `bind()`, `end()` is empty). These are used by some client code.

---

### ShapeRenderer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/ShapeRenderer.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/ShapeRenderer.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: **Partial**. Many drawing primitives are missing.
**Renames**: `dispose()` -> `close()`; `rect()` -> `rectangle()`
**Convention changes**: Inner `ShapeType` enum in companion object. Uses `(using Sge)`.
**TODOs**: Line 261 has `// TODO: Check if we need to flush due to too many vertices`
**Issues**:
- Missing `begin()` (no-arg, uses auto shape type)
- Missing `point(x, y, z)`
- Missing `curve(x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments)`
- Missing `triangle` (2 overloads: basic + colored)
- Missing `rect` overloads with origin/rotation/scale (2 overloads)
- Missing `rectLine(x1, y1, x2, y2, width)` (1-color version) and `rectLine(Vector2, Vector2, width)`
- Missing `box(x, y, z, width, height, depth)`
- Missing `x(x, y, size)` and `x(Vector2, size)`
- Missing `arc` (2 overloads)
- Missing `ellipse` (4 overloads)
- Missing `cone` (2 overloads)
- Missing `polygon` (2 overloads)
- Missing `polyline` (2 overloads)
- Missing `flush()` and `getCurrentType()`
- Overall: ~25+ method overloads missing out of ~40+ in the Java source

---

### VertexArray.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/VertexArray.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/VertexArray.java` |
| Status | pass |
| Tested | No |

**Completeness**: All VertexData methods ported 1:1 including bind/unbind with location arrays.
**Renames**: `dispose()` -> `close()`
**Convention changes**: `Nullable[Array[Int]]` for optional locations parameter.
**TODOs**: None
**Issues**: None

---

### VertexBufferObject.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/VertexBufferObject.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/VertexBufferObject.java` |
| Status | pass |
| Tested | No |

**Completeness**: All VertexData methods ported. Both constructors (VertexAttribute varargs and VertexAttributes) present.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `(using sde: Sge)` for GL calls.
**TODOs**: None
**Issues**: None

---

### VertexBufferObjectSubData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/VertexBufferObjectSubData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/VertexBufferObjectSubData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All VertexData methods ported. `getBufferHandle()` extra accessor present.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `(using sde: Sge)` for GL calls.
**TODOs**: None
**Issues**: None

---

### VertexBufferObjectWithVAO.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/VertexBufferObjectWithVAO.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/VertexBufferObjectWithVAO.java` |
| Status | pass |
| Tested | No |

**Completeness**: All VertexData methods ported. VAO caching logic present. All 3 constructors (varargs, VertexAttributes, unmanaged ByteBuffer) present.
**Renames**: `dispose()` -> `close()`
**Convention changes**: Uses `DynamicArray[Int]` for cached locations. Uses `(using sde: Sge)`.
**TODOs**: None
**Issues**: None

---

### VertexData.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/glutils/VertexData.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/glutils/VertexData.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 11 interface methods ported 1:1.
**Renames**: `Disposable` -> `AutoCloseable`; `dispose()` -> `close()`
**Convention changes**: Java `interface` -> Scala `trait`. `int[]` params -> `Nullable[Array[Int]]`.
**TODOs**: None
**Issues**: None
