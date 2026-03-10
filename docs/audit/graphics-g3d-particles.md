# Audit: sge.graphics.g3d.particles (root)

Audited: 10/10 files | Pass: 7 | Minor: 3 | Major: 0
Last updated: 2026-03-03

---

### ParallelArray.scala -- pass
All public methods ported: `addChannel` x2, `removeArray`, `addElement`, `removeElement`,
`getChannel`, `clear`, `setCapacity`. Inner types: `ChannelDescriptor`, `Channel`,
`ChannelInitializer`, `FloatChannel`, `IntChannel`, `ObjectChannel`.
Java inner class `Channel` (non-static) moved to companion object; explicit `owner: ParallelArray`
parameter added to access `size` during `add()`. `FloatChannel.data` renamed to `floatData`,
`IntChannel.data` to `intData`, `ObjectChannel.data` to `objectData` to avoid shadowing
`Channel.data`. `ObjectChannel.clearSlot` added for null clearing at Java interop boundary.
Deprecated `ChannelDescriptor(int, Class, int)` constructor omitted (used ArrayReflection).
`getChannel` returns `Nullable[T]` instead of `T|null`. Json.Serializable not implemented.

### ParticleChannels.scala -- pass
All channel descriptors (16), offsets (18 constants), and initializers (5 classes) ported.
Static members mapped to companion object `val`s. Initializer singletons use `lazy val` +
`get()` (Java used null-check lazy init). `resetIds` visibility changed from `protected` to
`protected[particles]` (Java protected accessible to same-package subclasses).
Initializer `channel.data` references correctly use `channel.floatData` (Scala rename).
Constructor initializes `currentId = currentGlobalId` directly instead of calling `resetIds()`.

### ParticleControllerComponent.scala -- pass
All 11 methods ported: `activateParticles`, `killParticles`, `update`, `init`, `start`, `end`,
`close`, `copy`, `allocateChannels`, `set`, `save`, `load`. `Disposable` mapped to
`AutoCloseable`; `dispose()` mapped to `close()`. Json.Serializable (`write`/`read`) not
implemented. Static temp fields (`TMP_V1`..`TMP_V6`, `TMP_Q`, `TMP_Q2`, `TMP_M3`, `TMP_M4`)
in companion object with `private[particles]` visibility (Java `protected static`).

### ParticleController.scala -- minor_issues
All 27 public methods ported: `setTransform` x2, `rotate` x2, `translate`, `setTranslation`,
`scale` x2, `mul`, `getTransform`, `isComplete`, `init`, `allocateChannels`, `bind`, `start`,
`reset`, `end`, `activateParticles`, `killParticles`, `update` x2, `draw`, `copy`, `dispose`,
`getBoundingBox`, `calculateBoundingBox`, `findInfluencer`, `removeInfluencer`,
`replaceInfluencer`, `save`, `load`.
Minor issues:
- Json.Serializable (`write`/`read`) not implemented -- blocks JSON de/serialization
- `update()` uses `(using Sge)` context parameter (correct SGE pattern)
- `dispose()` calls `emitter.close()`/`influencer.close()` (correct AutoCloseable mapping)
Note: `getBoundingBox()` Scaladoc corrected to describe bounding box return (not "copy of controller").

### ParticleEffect.scala -- pass
All 18 public methods ported: `init`, `start`, `end`, `reset`, `update` x2, `draw`,
`isComplete`, `setTransform`, `rotate` x2, `translate`, `scale` x2, `getControllers`,
`findController`, `close`, `getBoundingBox`, `setBatch`, `copy`, `save`, `load`.
`Disposable` mapped to `AutoCloseable`. `findController` returns `Nullable` instead of null.
`setBatch` uses `boundary`/`break` for inner break. `update()` uses `(using Sge)`.

### ParticleSystem.scala -- pass
All 12 public methods ported: `add(batch)`, `add(effect)`, `remove`, `removeAll`,
`update()`, `update(Float)`, `updateAndDraw()`, `updateAndDraw(Float)`, `begin`, `draw`,
`end`, `getRenderables`, `getBatches`. Deprecated static `get()` singleton correctly omitted.
Class is `final` (matches Java). `update`/`updateAndDraw` no-arg variants use `(using Sge)`.

### ParticleSorter.scala -- minor_issues
All methods ported in base class and two inner classes (`None`, `Distance`).
`TMP_V1` static field: orphaned `new Vector3()` side-effect preserved in companion object.
Minor issues:
- `None.currentCapacity`/`indices`: Java package-private, Scala `private` (stricter visibility)
- `Distance.qsort`: Java uses early `return` in insertion sort branch; Scala restructured
  with nested while loop (correct, but slightly different control flow structure)
- Java `val` keyword (for array variable) mapped to `values` (camera.view.values)
- `positionChannel.data` correctly uses `positionChannel.floatData`

### ResourceData.scala -- minor_issues
All public methods ported: `getAssetData`, `getAssetDescriptors`, `getAssets`, `createSaveData` x2,
`getSaveData` x2. Inner types: `Configurable`, `SaveData`, `AssetData`. `SaveData` methods:
`saveAsset`, `save`, `loadAsset`, `load`.
Minor issues:
- Json.Serializable (`write`/`read`) not implemented on `ResourceData`, `SaveData`, `AssetData`
  -- this is the primary serialization bottleneck for the particle system
- `Configurable` interface: Java used `Configurable<T>` generic; Scala uses `ResourceData[?]`
  (type parameter erased, may affect type safety at call sites)
- `resource` field: `Nullable[T]` instead of `T` (null safety improvement)
- `IntArray` → `DynamicArray[Int]` for `SaveData.assets`

### ParticleEffectLoader.scala -- minor_issues
Structure ported with correct parameter classes.
Minor issues:
- `getDependencies` is stubbed (returns empty list) -- needs JSON deserialization
- `save` method logic ported but JSON write is commented out -- needs JSON framework
- `loadSync` ported with `Nullable` handling but depends on `getDependencies` populating `items`
- `find()` private method omitted (used ClassReflection)
- `ParticleEffectSaveParameter.jsonOutputType` field omitted (no `JsonWriter.OutputType` in SGE)
- Constructor requires `(using Sge)` context parameter (correct SGE pattern)

Note: The stubbed JSON methods are a known limitation tracked across the particle system.
The `ParticleEffectLoader` will become fully functional once JSON serialization bridge is
integrated.

### ParticleShader.scala -- pass
All public methods ported: constructors (5), `init`, `canRender`, `compareTo`, `equals`,
`begin`, `render`, `end`, `bindMaterial`, `close`, `getDefaultCullFace`, `setDefaultCullFace`,
`getDefaultDepthFunc`, `setDefaultDepthFunc`. Static: `createPrefix`, `getDefaultVertexShader`,
`getDefaultFragmentShader`. Enums: `ParticleType`, `AlignMode`. Inner classes: `Config`,
`Inputs`, `Setters`.
Java enums correctly mapped to Scala 3 enums. Config null strings → `Nullable[String]`.
`Gdx.graphics.getWidth()` in `screenWidth` setter → `camera.viewportWidth` (reasonable
approximation). Setters use `GlobalSetter`/`LocalSetter` abstract classes. `dispose()` → `close()`.
`context`/`material` fields wrapped in `Nullable` with `foreach`/`fold` patterns.
