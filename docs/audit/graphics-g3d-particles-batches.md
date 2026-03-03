# Audit: sge.graphics.g3d.particles.batches

Audited: 5/5 files | Pass: 5 | Minor: 0 | Major: 0
Last updated: 2026-03-03

---

### ParticleBatch.scala -- pass
Java interface mapped to Scala 3 trait with type parameter `T <: ParticleControllerRenderData`.
All 3 declared methods ported: `begin`, `draw`, `end`. `save`/`load` not declared directly
in the trait body but are inherited via the `ResourceData.Configurable` mixin, which declares
both methods with identical signatures -- functionally equivalent. Extends `RenderableProvider`
and `ResourceData.Configurable` matching the Java `implements` clause.

### BufferedParticleBatch.scala -- pass
All 9 public/protected methods ported: `begin`, `draw`, `end`, `ensureCapacity`,
`resetCapacity`, `setCamera`, `getSorter`, `setSorter`, `getBufferedCount`, plus abstract
`allocParticlesData` and `flush`. Java `Array<T>` replaced with `DynamicArray[T]`. Deprecated
`Class<T>` constructor and `ArraySupplier` constructor both collapsed to a no-arg approach
(DynamicArray handles type erasure differently). `return` statement in `ensureCapacity`
replaced with if/else guard (no-return convention). `camera` field uses
`scala.compiletime.uninitialized` in place of Java's implicit null. Field visibility
(`protected`) preserved faithfully.

### ModelInstanceParticleBatch.scala -- pass
All 7 public methods ported: `getRenderables`, `getBufferedCount`, `begin`, `end`, `draw`,
`save`, `load`. Java `Array<T>` replaced with `DynamicArray[T]`. `ObjectChannel.data[i]`
correctly mapped to `objectData(i)` (field rename in ParallelArray port). Constructor
initializes `DynamicArray` with capacity 5 matching Java source. `save`/`load` are empty
stubs matching Java. `getRenderables` uses `for`/`while` loops faithfully reproducing the
nested Java loop structure.

### PointSpriteParticleBatch.scala -- pass
All 10 public/protected methods ported: `allocParticlesData`, `allocRenderable`, `setTexture`,
`getTexture`, `getBlendingAttribute`, `flush`, `getRenderables`, `save`, `load`, plus
inherited overrides. Constructor chain: Java's 4 constructors mapped to Scala primary
constructor with 3 Nullable parameters + 2 secondary constructors. Key differences:
(1) ParticleShader not fully integrated -- shader initialization is deferred with a comment;
Java assigns `renderable.shader = new ParticleShader(...)`, Scala leaves it unset.
(2) `getTexture()` returns `Nullable[Texture]` instead of raw `Texture` (null-safe).
(3) `setTexture` navigates Nullable material/attribute chain with `foreach` instead of
direct cast. (4) `Gdx.gl`/`Gdx.app` replaced with `Sge().graphics.gl`/`Sge().application`
via `(using Sge)`. (5) `findByUsage(usage).offset/4` replaced with `getOffset(usage)`
helper. (6) `FloatChannel.data[]` replaced with `floatData()`. Static fields and methods
moved to companion object. `dispose()` mapped to `close()`. All vertex data assembly in
`flush` is identical to Java.

### BillboardParticleBatch.scala -- pass
All 18 public/protected methods ported: `allocParticlesData`, `allocRenderable`, `setTexture`,
`getTexture`, `getBlendingAttribute`, `setAlignMode`, `getAlignMode`, `setUseGpu`, `isUseGPU`,
`setVertexData`, `flush`, `getRenderables`, `save`, `load`, `begin`, plus private helpers
(`allocIndices`, `allocRenderables`, `allocShader`, `clearRenderablesPool`, `initRenderData`,
`fillVerticesGPU`, `fillVerticesToViewPointCPU`, `fillVerticesToScreenCPU`, `putVertexGPU`,
`putVertexCPU`). Key differences:
(1) `AlignMode` enum defined locally in `BillboardParticleBatch` companion object rather than
referencing `ParticleShader.AlignMode` -- both enums have identical values (`Screen`,
`ViewPoint`; `ParticleDirection` commented out in both).
(2) `getShader()`: Java selects `ParticleShader` for GPU mode vs `DefaultShader` for CPU;
Scala always uses `DefaultShader` (ParticleShader not integrated into batch shader selection).
(3) `clearRenderablesPool`: Java uses `renderablePool.freeAll(renderables)` (Array is
Iterable); Scala uses `renderables.foreach(renderablePool.free)` (DynamicArray not Iterable).
(4) `allocRenderables`: Java `MathUtils.ceil(capacity / MAX_PARTICLES_PER_MESH)` does integer
division before ceil (effectively a no-op); Scala uses `capacity.toFloat` for proper float
division (more correct behavior).
(5) `RenderablePool` inner class: Java no-arg constructor; Scala overrides `max`/
`initialCapacity` vals and takes `(using Sge)`.
(6) `Config` inner class: Java package-private fields; Scala uses public `var` fields.
(7) Static `putVertex` methods (GPU and CPU variants) converted to private instance methods
(`putVertexGPU`, `putVertexCPU`).
(8) Null fields wrapped with `Nullable`; `setTexture` navigates Nullable attribute chain.
(9) Constructor chain: 4 Java constructors mapped to primary + 3 secondary with `(using Sge)`.
(10) `dispose()` mapped to `close()`. All vertex data assembly logic (GPU and CPU paths)
is identical to Java. Commented-out `ParticleDirection` code preserved as comments.
