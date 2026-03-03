# Audit: sge.graphics.g3d.model

Audited: 6/6 files | Pass: 4 | Minor: 2 | Major: 0
Last updated: 2026-03-03

---

### NodeKeyframe.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/model/NodeKeyframe.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/model/NodeKeyframe.java` |
| Status | pass |
| Tested | No |

**Completeness**: All Java public fields present (keytime, value). Constructor matches Java.
**Renames**: None
**Convention changes**: Constructor params instead of field + constructor body
**TODOs**: None
**Issues**: None

---

### Animation.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/model/Animation.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/model/Animation.java` |
| Status | pass |
| Tested | No |

**Completeness**: All Java public fields present (id, duration, nodeAnimations).
**Renames**: None
**Convention changes**: `Array<NodeAnimation>` -> `DynamicArray[NodeAnimation]`; `id` uses `scala.compiletime.uninitialized`
**TODOs**: None
**Issues**: None

---

### NodeAnimation.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/model/NodeAnimation.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/model/NodeAnimation.java` |
| Status | pass |
| Tested | No |

**Completeness**: All Java public fields present (node, translation, rotation, scaling).
**Renames**: None
**Convention changes**: Java `null` -> `Nullable.empty` for keyframe arrays; `Array` -> `DynamicArray`
**TODOs**: None
**Issues**: None

---

### Node.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/model/Node.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/model/Node.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All Java public methods and fields present. Static `getNode` correctly in companion object.
**Renames**: None
**Convention changes**: `parent` uses `Nullable[Node]`; `getChildren` returns `DynamicArray` (Java `Iterable`); `addChildren`/`insertChildren` take `DynamicArray[T]` (Java `Iterable<T>`); `removeChild` uses `indexWhere`/`removeIndex` (Java `removeValue` identity); `getNode` uses `boundary`/`break` (Java `return`); `calculateBoneTransforms` restructured with `Nullable.fold` (Java null checks + `continue`).
**TODOs**: FIXME comment preserved from Java (id uniqueness question)
**Issues**:
- `minor`: Unused import `scala.language.implicitConversions` (no implicit conversions used in file)
- `minor`: `DynamicArray` default capacity not specified (Java uses `Array<NodePart>(2)` and `Array<Node>(2)` with initial capacity 2)

---

### MeshPart.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/model/MeshPart.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/model/MeshPart.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All Java public methods and fields present. Constructors (no-arg, 5-arg, copy) all present. `set(MeshPart)`, `set(5-arg)`, `update()`, `equals(MeshPart)`, `equals(Any)`, `render(ShaderProgram, Boolean)`, `render(ShaderProgram)` all present.
**Renames**: `id` type changed from `String` to `Nullable[String]` (Java allows null id); `halfExtents.len()` -> `halfExtents.length` (SGE Vector3 renames `len` to `length`)
**Convention changes**: Static `bounds` -> companion object `private val`; pattern-match `equals(Any)`
**TODOs**: None
**Issues**:
- `minor`: `hashCode()` override added in Scala but NOT present in Java source (SGE improvement/addition)

---

### NodePart.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/graphics/g3d/model/NodePart.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/model/NodePart.java` |
| Status | pass |
| Tested | No |

**Completeness**: All Java public methods and fields present (meshPart, material, invBoneBindTransforms, bones, enabled, constructors, setRenderable, copy, set).
**Renames**: None
**Convention changes**: `invBoneBindTransforms`/`bones` use `Nullable`; `setRenderable` wraps `material` in `Nullable()` (Renderable.material is `Nullable[Material]`); `set()` uses `Nullable.fold` pattern (Java null checks); FIXME comment preserved
**TODOs**: FIXME from Java preserved ("add copy constructor and override #equals")
**Issues**: None
