# Audit: sge.graphics.g3d.attributes

Audited: 10/10 files | Pass: 7 | Minor: 3 | Major: 0
Last updated: 2026-03-04

---

### IntAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/IntAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/IntAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 1 constant (`CullFace`), 1 alias, 1 factory method (`createCullFace`), `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare` (Ordered[Attribute])
**Convention changes**: Two Java constructors merged into primary constructor with default `value = 0`; split package; braces
**TODOs**: None
**Issues**: None

---

### FloatAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/FloatAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/FloatAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 2 constants (`Shininess`, `AlphaTest`), 2 aliases, 2 factory methods, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare` (Ordered[Attribute])
**Convention changes**: Two Java constructors merged into primary constructor with default `value = 0f`; split package; braces
**TODOs**: None
**Issues**: None

---

### BlendingAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/BlendingAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/BlendingAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 fields (`blended`, `sourceFunction`, `destFunction`, `opacity`), `Alias`, `Type`, `is()`, 6 constructors, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare` (Ordered[Attribute])
**Convention changes**: Java copy-ctor has null-safe fallback (`copyFrom==null` -> defaults); Scala version does not accept null per no-null convention. Java no-arg ctor delegates to `BlendingAttribute(null)`; Scala uses explicit defaults. Same runtime behavior.
**TODOs**: None
**Issues**: None

---

### ColorAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/ColorAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/ColorAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 alias/type pairs (`Diffuse`, `Specular`, `Ambient`, `Emissive`, `Reflection`, `AmbientLight`, `Fog`), `Mask`, `is()`, 14 factory methods (7 Color overloads + 7 RGBA overloads), 4 constructors, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `GdxRuntimeException` -> `SgeError.InvalidInput`
**Convention changes**: Java `(type, color)` ctor has null guard (`if (color != null)`); Scala always calls `color.set` per no-null convention.
**TODOs**: None
**Issues**: None

---

### DepthTestAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/DepthTestAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/DepthTestAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 fields (`depthFunc`, `depthRangeNear`, `depthRangeFar`, `depthMask`), `Alias`, `Type`, `Mask`, `is()`, 8 constructors (including copy), `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `GdxRuntimeException` -> `SgeError.InvalidInput`
**Convention changes**: Split package; braces
**TODOs**: None
**Issues**: None

---

### PointLightsAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/PointLightsAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/PointLightsAttribute.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All fields (`lights`), `Alias`, `Type`, `is()`, 2 constructors, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `Array<PointLight>` -> `DynamicArray[PointLight]`
**Convention changes**: Null check in hashCode -> `Nullable(light).fold(0)(_.hashCode())`; Java `Array(1)` initial capacity vs Scala `DynamicArray()` default capacity
**TODOs**: 1 -- `FIXME implement comparing` (same as Java source)
**Issues**:
- `minor`: FIXME comparing not implemented (inherited from Java source, not a port regression)

---

### DirectionalLightsAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/DirectionalLightsAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/DirectionalLightsAttribute.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All fields (`lights`), `Alias`, `Type`, `is()`, 2 constructors, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `Array<DirectionalLight>` -> `DynamicArray[DirectionalLight]`
**Convention changes**: Null check in hashCode -> `Nullable(light).fold(0)(_.hashCode())`; Java `Array(1)` initial capacity vs Scala `DynamicArray()` default capacity
**TODOs**: 1 -- `FIXME implement comparing` (same as Java source)
**Issues**:
- `minor`: FIXME comparing not implemented (inherited from Java source, not a port regression)

---

### SpotLightsAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/SpotLightsAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/SpotLightsAttribute.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All fields (`lights`), `Alias`, `Type`, `is()`, 2 constructors, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `Array<SpotLight>` -> `DynamicArray[SpotLight]`
**Convention changes**: Null check in hashCode -> `Nullable(light).fold(0)(_.hashCode())`; Java `Array(1)` initial capacity vs Scala `DynamicArray()` default capacity
**TODOs**: 1 -- `FIXME implement comparing` (same as Java source)
**Issues**:
- `minor`: FIXME comparing not implemented (inherited from Java source, not a port regression)

---

### TextureAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/TextureAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/TextureAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 alias/type pairs (`Diffuse`, `Specular`, `Bump`, `Normal`, `Ambient`, `Emissive`, `Reflection`), `Mask`, `is()`, 14 factory methods (7 Texture + 7 TextureRegion), 6 fields (`textureDescription`, `offsetU`, `offsetV`, `scaleU`, `scaleV`, `uvIndex`), `set(TextureRegion)`, `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `GdxRuntimeException` -> `SgeError.InvalidInput`
**Convention changes**: `textureDescription.texture` assignment uses `Nullable()` wrapper per no-null convention. Java generic ctor `(type, TextureDescriptor<T>)` → Scala `(type, TextureDescriptor[? <: Texture])`. Copy ctor delegates to full 7-arg constructor.
**TODOs**: None
**Issues**: None

---

### CubemapAttribute.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/attributes/CubemapAttribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/attributes/CubemapAttribute.java` |
| Status | pass |
| Tested | No |

**Completeness**: All fields (`textureDescription`), `EnvironmentMapAlias`, `EnvironmentMap`, `Mask`, `is()`, 3 constructors (type-only, type+Cubemap, copy), `copy`, `hashCode`, `compare` accounted for.
**Renames**: `compareTo` -> `compare`; `GdxRuntimeException` -> `SgeError.InvalidInput`
**Convention changes**: `textureDescription.texture` assignment uses `Nullable()` wrapper per no-null convention. Java generic ctor `(type, TextureDescriptor<T>)` erases to same as primary constructor; covered by primary `(type, TextureDescriptor[Cubemap])`. Copy ctor uses `.set()` for deep copy.
**TODOs**: None
**Issues**: None
