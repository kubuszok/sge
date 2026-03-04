# Audit: sge.graphics.g3d.decals

Audited: 8/8 files | Pass: 8 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### GroupPlug.scala -- pass
Java interface mapped to Scala trait. Both methods ported: `beforeGroup(DynamicArray[Decal])`,
`afterGroup()`. `Array<Decal>` mapped to `DynamicArray[Decal]`. No behavioral differences.

### GroupStrategy.scala -- pass
Java interface mapped to Scala trait. All 6 methods ported: `getGroupShader(Int)`,
`decideGroup(Decal)`, `beforeGroup(Int, DynamicArray[Decal])`, `afterGroup(Int)`,
`beforeGroups()`, `afterGroups()`. `getGroupShader` returns `Nullable[ShaderProgram]`
instead of nullable `ShaderProgram`. `Array<Decal>` mapped to `DynamicArray[Decal]`.

### PluggableGroupStrategy.scala -- pass
Abstract class implementing `GroupStrategy`. `IntMap<GroupPlug>` mapped to
`ObjectMap[Int, GroupPlug]` (IntMap not ported). `beforeGroup`/`afterGroup` use
`.foreach()` on plug lookup instead of direct null-unsafe call -- safer behavior,
intentional improvement. `unPlug` returns `Nullable[GroupPlug]` instead of nullable
`GroupPlug`. `plugIn` method preserved.

### SimpleOrthoGroupStrategy.scala -- pass
All 6 `GroupStrategy` methods implemented. Inner `Comparator` class replaced by
`Ordering.fromLessThan` lambda -- equivalent sort behavior. `Sort.instance().sort()`
mapped to `Sort.sort()` (Sort is a Scala object). `Gdx.gl` mapped to `Sge().graphics.gl`
via `(using Sge)` context parameter. `getGroupShader` returns `Nullable.empty` (was `null`).
Companion object holds `GROUP_OPAQUE`/`GROUP_BLEND` constants with `final private` visibility.

### DecalMaterial.scala -- pass
All methods ported: `set()`, `isOpaque`, `equals(Any)`, `hashCode()`. Fields `srcBlendFactor`
and `dstBlendFactor` are public `var` (no redundant getters). `set()` takes `(using Sge)` for
GL access. `equals` uses pattern match instead of null check + unsafe cast -- safer. `hashCode`
uses `Nullable` wrapping for null-safe texture hash. `NO_BLEND` constant in companion object.

### DecalBatch.scala -- pass
All 10 methods ported: `initialize(Int)`, `getSize`, `add(Decal)`, `flush()`, `render()`,
`render(Nullable[ShaderProgram], DynamicArray[Decal])`, `flush(Nullable[ShaderProgram], Int)`,
`clear()`, `close()`, `setGroupStrategy(GroupStrategy)`. `Disposable` mapped to `AutoCloseable`;
`dispose()` mapped to `close()`. `SortedIntList<Array<Decal>>` mapped to
`mutable.TreeMap[Int, DynamicArray[Decal]]` -- both provide sorted-by-key iteration.
`Pool` anonymous subclass mapped to `Pool.Default` with lambda. `groupPool.freeAll(usedGroups)`
mapped to `usedGroups.foreach(groupPool.free)` since `DynamicArray` is not `Iterable`.
Protected `flush(shader, verticesPosition)` wraps shader in `Nullable.foreach`, skipping render
when shader is empty -- Java passes null which could NPE; Scala is safer. `Gdx.gl30 != null`
mapped to `Sge().graphics.gl30.isDefined`. Two constructors preserved.

### CameraGroupStrategy.scala -- pass
All `GroupStrategy` methods plus `setCamera`/`getCamera`/`close()` ported. `Disposable` mapped to
`AutoCloseable`. Default comparator uses `camera.position.distance()` (renamed from `dst()`) and
public `position` val instead of getter. `Pool` anonymous subclass mapped to `Pool.Default` with
lambda. `arrayPool.freeAll(usedArrays)` mapped to `usedArrays.foreach(arrayPool.free)`.
`contents.sort(cameraSorter)` uses `DynamicArray.sort()`. `materialGroups.values()` iteration
mapped to `foreachValue`. `getGroupShader` returns `Nullable(shader)`. `close()` uses
`Nullable(shader).foreach(_.close())` for null safety. `IllegalArgumentException` mapped to
`SgeError.GraphicsError`. Shader strings identical to Java. Field visibility slightly tighter
(private pools/shader vs Java package-private) -- acceptable.

### Decal.scala -- pass
All 40+ methods faithfully ported. Java-style getters/setters converted to Scala property
accessors: `x/x_=`, `y/y_=`, `z/z_=`, `scaleX/scaleX_=`, `scaleY/scaleY_=`, `width/width_=`,
`height/height_=`, `color/color_=`, `textureRegion/textureRegion_=`. Multi-param methods
preserved: `setColor(r,g,b,a)`, `setPosition(x,y,z)`, `setScale(x,y)`, `setDimensions(w,h)`,
`setRotation(yaw,pitch,roll)`. Read-only access: `position` and `rotation` are public `val`.
`vertices` property calls `update()` before returning backing `_vertices` array.
`material` is public `var` (getMaterial/setMaterial removed). All 6 `newDecal` factory overloads
in companion object. 24 vertex index constants (X1..V4) in companion. `_vertices`/`updated`/
`update()` scoped `private[decals]` for `DecalBatch` access. `transformationOffset` uses
`Nullable[Vector2]` with `Nullable.fold` replacing null check. `rotator` is `protected` in
companion. `transformVertices` quaternion math identical to Java source. No `return` statements.
No raw `null` usage.
