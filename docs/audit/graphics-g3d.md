# Audit: sge.graphics.g3d

Audited: 11/11 files | Pass: 10 | Minor: 1 | Major: 0
Last updated: 2026-03-03

---

### Renderable.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Renderable.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Renderable.java` |

All 7 fields match (worldTransform, meshPart, material, environment, bones, shader, userData).
`set()` method present and correct. Nullable used for all nullable fields. `worldTransform`
and `meshPart` are `val` (matching Java `final`).

### Shader.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Shader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Shader.java` |

Java `interface Shader extends Disposable` -> Scala `trait Shader extends AutoCloseable`.
All 6 methods present: `init`, `compareTo`, `canRender`, `begin`, `render`, `end`.
Original TODO comment preserved.

### Attribute.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Attribute.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Attribute.java` |

`Comparable<Attribute>` -> `Ordered[Attribute]`. Static members moved to companion object.
`register()` changed from `protected` to `private[g3d]` (correct: callers are companion objects
in `g3d.attributes`, not subclasses). `getAttributeAlias` returns `Nullable[String]` with
`toString` using `.getOrElse("unknown")` fallback instead of null. All methods match:
`getAttributeType`, `getAttributeAlias`, `register`, `copy`, `equals` (2), `toString`, `hashCode`.

### Environment.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Environment.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Environment.java` |

`shadowMap` field present as `Nullable[ShadowMap]`. Light `add()` methods (6 overloads) all present.
Java `remove(BaseLight)` renamed to `removeLight()` and `removeLights()` to avoid name clash with
`Attributes.remove(Long)` -- intentional and correct. All light removal methods (6 overloads)
present. Pattern match for light dispatch instead of instanceof chain.

### RenderableProvider.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/RenderableProvider.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/RenderableProvider.java` |

Java `interface` -> Scala `trait`. Single method `getRenderables` matches.
`Array<Renderable>` -> `DynamicArray[Renderable]`, `Pool<Renderable>` -> `sge.utils.Pool[Renderable]`.

### Attributes.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Attributes.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Attributes.java` |

`Iterable<Attribute> + Comparator<Attribute> + Comparable<Attributes>` ->
`Iterable[Attribute] with Ordered[Attributes]`. `Comparator` extracted to companion object as
`given Ordering[Attribute]`. All 20 methods present: `sort`, `getMask`, `get` (3 overloads),
`clear`, `size`, `set` (6 overloads), `remove`, `has`, `indexOf`, `same` (2), `iterator`,
`attributesHash`, `hashCode`, `equals`, `compare`. `get(Long)` returns `Nullable[Attribute]`.

### Model.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Model.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Model.java` |

`Disposable` -> `AutoCloseable`. All 6 public fields (materials, nodes, animations, meshes,
meshParts, disposables) match. Constructors require `(using Sge)` for texture loading. All 14
public/protected methods present: `load`, `loadAnimations`, `loadNodes`, `loadNode`, `loadMeshes`,
`convertMesh`, `loadMaterials`, `convertMaterial`, `manageDisposable`, `getManagedDisposables`,
`close`, `calculateTransforms`, `calculateBoundingBox`, `extendBoundingBox`, `getAnimation` (2),
`getMaterial` (2), `getNode` (3). `nodePartBones` uses `scala.collection.mutable.Map` (Java
`ObjectMap`). `convertMaterial` uses `mutable.Map` for texture dedup. All FIXME comments preserved.

### Material.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/Material.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/Material.java` |

All 8 constructors match (no-arg, String, varargs, String+varargs, DynamicArray, String+DynamicArray,
Material, String+Material). Static counter moved to companion object. `copy()`, `hashCode()`,
`equals()` all match. `id` is constructor parameter (Java: public field).

### ModelInstance.scala -- minor_issues

| SGE path | `core/src/main/scala/sge/graphics/g3d/ModelInstance.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/ModelInstance.java` |

Implements `RenderableProvider`. Java has ~15 constructor overloads; Scala consolidates into
fewer constructors using `Nullable` params and default arguments. All core functionality preserved.
`defaultShareKeyframes` in companion object. All public methods present (copy, getRenderables,
getRenderable x3, calculateTransforms, calculateBoundingBox, extendBoundingBox, getAnimation x2,
getMaterial x2, getNode x3, copyAnimations x2, copyAnimation x2).

**Minor issues**:
- `invalidate(Node)` bone rebinding logic: Java directly mutates `bindPose.keys[j]` to replace
  the old node reference, but Scala code uses `bindPose.put(replacement, value)` which adds a
  new entry but does not remove the stale key. The commented-out code at line 173-174 suggests
  awareness of the issue but no actual removal of the old entry occurs.
- `getRenderable(out, node, nodePart)`: Java has a 3-way branch checking `nodePart.bones == null`
  AND `transform != null` (and a fallback `out.worldTransform.idt()`), but Scala only checks
  `nodePart.bones.isEmpty` (transform is always non-null in SGE, so the simplification is
  acceptable, but the `idt()` fallback is lost).
- Some convenience constructors omitted (e.g. `(Model, String, bool, bool)` family that creates
  with nodeId + parentTransform + mergeTransform shorthand), though equivalent functionality
  exists through the more general constructor.

### ModelCache.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/ModelCache.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/ModelCache.java` |

`Disposable` -> `AutoCloseable`. `FlushablePool` -> `Pool.Flushable`. All inner types moved to
companion object: `MeshPool` (trait), `SimpleMeshPool`, `TightMeshPool`, `Sorter`. Constructor
requires `(using Sge)`. `end()` uses `boundary`/`break` for early return. All public methods
present: `begin` (2), `end`, `add` (3), `getRenderables`, `close`. FIXME comments preserved.

### ModelBatch.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/ModelBatch.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/ModelBatch.java` |

`Disposable` -> `AutoCloseable`. Java's single 3-arg constructor with null checks converted to
Scala primary constructor + multiple `Nullable` overloads. `FlushablePool` -> `Pool.Default` with
`Pool.Flushable` mixin. `RenderablePool.obtain()` resets `meshPart.id` to `Nullable.empty`
(Java uses `""`). Camera is `Nullable[Camera]` (Java nullable Camera field). All 11 constructors
match. All public methods present: `begin`, `setCamera`, `getCamera`, `ownsRenderContext`,
`getRenderContext`, `getShaderProvider`, `getRenderableSorter`, `flush`, `end`, `render` (8
overloads), `close`.
