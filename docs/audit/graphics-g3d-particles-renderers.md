# Audit: sge.graphics.g3d.particles.renderers

Audited: 9/9 files | Pass: 9 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### ParticleControllerRenderData.scala -- pass
Abstract base class with 2 public fields (`controller`, `positionChannel`). Both faithfully
ported as vars with `scala.compiletime.uninitialized`. Exact 1:1 match with Java source.

### BillboardControllerRenderData.scala -- pass
Concrete class extending `ParticleControllerRenderData` with 4 additional `FloatChannel` fields
(`regionChannel`, `colorChannel`, `scaleChannel`, `rotationChannel`). All 4 public fields
faithfully ported. Exact 1:1 match with Java source.

### PointSpriteControllerRenderData.scala -- pass
Concrete class extending `ParticleControllerRenderData` with 4 additional `FloatChannel` fields
(`regionChannel`, `colorChannel`, `scaleChannel`, `rotationChannel`). Identical structure to
`BillboardControllerRenderData`. Exact 1:1 match with Java source.

### ModelInstanceControllerRenderData.scala -- pass
Concrete class extending `ParticleControllerRenderData` with 4 fields: 1 `ObjectChannel[ModelInstance]`
(`modelInstanceChannel`) and 3 `FloatChannel` (`colorChannel`, `scaleChannel`, `rotationChannel`).
All faithfully ported. Exact 1:1 match with Java source.

### ParticleControllerRenderer.scala -- pass
Abstract generic base class for all particle renderers. All 4 methods ported: `update`, `setBatch`,
`isCompatible` (abstract), `set`. Key Scala improvements: `renderData` typed as `Nullable[D]`
instead of bare `D` (eliminates NPE risk); `update()` wraps `batch.draw(renderData)` in
`renderData.foreach` (Java would NPE if renderData is null); `set()` uses `renderData.foreach`
instead of `if (renderData != null)`. `setBatch()` unchecked cast preserved with `asInstanceOf`.
Both constructors (no-arg protected and renderData-accepting protected) faithfully ported.

### BillboardRenderer.scala -- pass
All 4 methods ported: `allocateChannels`, `copy`, `isCompatible`, secondary constructor.
`allocateChannels()` channels (Position, TextureRegion, Color, Scale, Rotation2D) match Java
exactly. Wrapped in `renderData.foreach` for null safety. `import scala.language.implicitConversions`
is present but appears unused (no functional impact, may be needed for Nullable conversions).

### PointSpriteRenderer.scala -- pass
All 4 methods ported: `allocateChannels`, `copy`, `isCompatible`, secondary constructor.
Structurally identical to `BillboardRenderer` but targeting `PointSpriteParticleBatch` and
`PointSpriteControllerRenderData`. Channel allocations match Java exactly.
`import scala.language.implicitConversions` is present but appears unused (same as BillboardRenderer).

### ParticleControllerControllerRenderer.scala -- pass
All 4 methods ported: `init`, `update`, `copy`, `isCompatible`. Java uses raw type
`ParticleControllerRenderer` (no generics); Scala provides explicit type parameters
`[ParticleControllerRenderData, ParticleBatch[ParticleControllerRenderData]]`.
`controllerChannel` widened from Java package-private to Scala public `var` (minor visibility
expansion, standard in port). `init()` converts Java null check + `GdxRuntimeException` to
`getOrElse` + `SgeError.InvalidInput`. `update()` loop uses `objectData(i)` (Scala
`ObjectChannel` rename from Java `data[i]`). `isCompatible` correctly returns `false`.

### ModelInstanceRenderer.scala -- pass
Most complex file in the package. All 6 methods ported: `allocateChannels`, `init`, `update`,
`copy`, `isCompatible`, secondary constructor. `init()` converts Java null channel assignments +
boolean checks to Scala `Nullable` `getChannel` + `isDefined`/`foreach`; `modelInstanceChannel`
null case throws `SgeError.InvalidInput` (matches Java `GdxRuntimeException`). `update()` is the
most intricate method: position/scale/rotation/color per-particle updates all faithfully ported
with correct stride offsets and `ParticleChannels` offset constants. Java `data[]` array access
converted to `floatData()`/`objectData()`. Java cast of `BlendingAttribute` + null check converted
to `Nullable().map().foreach`. `super.update()` called at end (matches Java). Three private boolean
flags (`hasColor`, `hasScale`, `hasRotation`) preserved as `private var`.
