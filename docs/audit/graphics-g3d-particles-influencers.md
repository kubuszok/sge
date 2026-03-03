# Audit: sge.graphics.g3d.particles.influencers

Audited: 11/11 files | Pass: 10 | Minor: 1 | Major: 0
Last updated: 2026-03-03

---

### Influencer.scala -- pass
Trivial abstract class extending `ParticleControllerComponent`. No public API differences.
`write`/`read`(Json) omitted (Json serialization not ported).

### SpawnInfluencer.scala -- pass
All 7 public methods ported: `init`, `allocateChannels`, `start`, `activateParticles`, `copy`,
`save`, `load`. Three constructors ported (default, SpawnShapeValue, copy).
`channel.data[]` renamed to `channel.floatData()` per SGE ParallelArray API.
`positionChannel`/`rotationChannel` promoted from package-private to public vars.
`write`/`read`(Json) omitted (Json serialization not ported).

### ColorInfluencer.scala -- pass
Abstract class with `colorChannel` field and two inner classes (`Random`, `Single`) moved to
companion object. `Random.colorChannel` removed -- inherits from parent (Java had a separate
field in the inner class). `Single`: all methods ported (`set`, `allocateChannels`,
`activateParticles`, `update`, `copy`). `write`/`read`(Json) in `Single` omitted.
`colorChannel` promoted from package-private to public var.

### SimpleInfluencer.scala -- pass
All public methods ported: `allocateChannels`, `activateParticles` (both relative/non-relative
branches), `update`. `value` field, `valueChannelDescriptor`, `interpolationChannel`,
`lifeChannel` all present. Copy constructor delegates via private `set` method.
`channel.data[]` renamed to `channel.floatData()`. `write`/`read`(Json) omitted.

### ScaleInfluencer.scala -- pass
Extends `SimpleInfluencer`. `activateParticles` overrides with scale multiplication
(`controller.scale.x`), both relative and non-relative branches faithfully ported.
Copy constructor manually copies `value` and `valueChannelDescriptor` instead of delegating to
`super(scaleInfluencer)` -- functionally equivalent. `copy` returns `ParticleControllerComponent`
(matches Java return type).

### ParticleControllerFinalizerInfluencer.scala -- pass
All 4 public methods ported: `init`, `allocateChannels`, `update`, `copy`. `init()`: null
checks replaced with `Nullable` `getChannel` + `getOrElse`/`isDefined`/`foreach`. `update()`:
`particleController.update(controller.deltaTime)` passes deltaTime explicitly; Java calls the
parameterless `update()` which internally reads `Gdx.graphics.getDeltaTime()`. The SGE
`ParticleController.update()` requires `(using Sge)` context, so the deltaTime overload is used
instead -- functionally equivalent. Fields promoted from package-private to public vars.

### DynamicsInfluencer.scala -- pass
All public methods ported: `allocateChannels`, `set`, `init`, `activateParticles`, `update`,
`copy`. `Array<DynamicsModifier>` replaced with `DynamicArray[DynamicsModifier]`.
`velocities.items[k]` replaced with `velocities(k)`. Null checks replaced with `Nullable`
`getChannel` + `isEmpty`/`foreach` pattern. `TMP_Q` moved to companion object (was static in
Java superclass). Verlet integration, 2D/3D angular velocity logic all faithfully ported.
`write`/`read`(Json) omitted.

### DynamicsModifier.scala -- pass
Base class with `isGlobal` field, `lifeChannel`, and `allocateChannels`. All 9 inner types
faithfully ported and moved to companion object:
- `FaceDirection`: rotation from acceleration direction.
- `Strength` (abstract): strength channel with interpolation.
- `Angular` (abstract): extends Strength with theta/phi channels.
- `Rotational2D`: 2D angular velocity.
- `Rotational3D`: 3D angular velocity with quaternion integration.
- `CentripetalAcceleration`: radial force from transform center.
- `PolarAcceleration`: directional force in spherical coordinates.
- `TangentialAcceleration`: cross-product tangential force.
- `BrownianAcceleration`: random directional force.

`TMP_V1`/`TMP_V2`/`TMP_V3`/`TMP_Q` moved to companion object (Java `protected static` fields).
`PolarAcceleration`/`TangentialAcceleration`: `TMP_V3.mul(TMP_Q)` replaced with
`TMP_Q.transform(TMP_V3)` -- `Vector3.mul(Quaternion)` not available in SGE; functionally
equivalent. `write`/`read`(Json) in base/`Strength`/`Angular` omitted.

### ParticleControllerInfluencer.scala -- minor_issues
Abstract class with `templates` (`DynamicArray[ParticleController]`), `particleControllerChannel`,
and inner classes `Single`, `Random` in companion object.

All public methods ported: `allocateChannels`, `end`, `close` (Java `dispose`), `save`, `load`.
`Single`: `init`, `activateParticles`, `killParticles`, `copy`.
`Random`: `init`, `close` (Java `dispose`), `activateParticles`, `killParticles`, `copy`.

`ParticleControllerPool` inner class replaced with `Pool.Default` + lambda.
`data[i] = null` replaced with `clearSlot(i)`.
`Random.init`: uses `pool.obtain()` instead of `pool.newObject()` (minor behavioral difference
in Pool accounting but functionally equivalent for pre-allocation).

**Minor issue**: `save()` is partially stubbed -- `AssetManager.getAll` is not yet available in
SGE, so the effects lookup loop is a placeholder. `load()` works but uses `descriptor.fold`
pattern with `keepLoading` flag instead of Java's `while ((descriptor = data.loadAsset()) != null)`
idiom.

### ModelInfluencer.scala -- pass
Abstract class with `models` (`DynamicArray[Model]`), `modelChannel`, and inner classes `Single`,
`Random` in companion object. All public methods ported: `allocateChannels`, `save`, `load`.
`Single`: `init`, `copy`. `Random`: `init`, `activateParticles`, `killParticles`, `copy`.

`ModelInstancePool` inner class replaced with `Pool.Default` + lambda. `data[i] = null` replaced
with `clearSlot(i)`. Load: null check on loaded model not explicitly thrown (model assumed
non-null via typed AssetManager.get). `write`/`read`(Json) omitted.

### RegionInfluencer.scala -- pass
Abstract class with `regions` (`DynamicArray[AspectTextureRegion]`), `regionChannel`,
`atlasName` (`Nullable[String]`). Inner classes in companion object: `AspectTextureRegion`,
`Single`, `Random`, `Animated`.

Default constructor logic extracted to `initDefault()` helper called by subclasses. Java
default constructor directly initializes `regions` + adds default region; Scala equivalent via
`initDefault()`.

`AspectTextureRegion`: `imageName` is `Nullable[String]` (Java `String`). `set(TextureRegion)`:
uses pattern match on `TextureAtlas.AtlasRegion` (Java `instanceof`). `updateUV`: null check
+ return replaced with `Nullable` `isEmpty`/`foreach`; also wraps `atlas.findRegion` result in
`foreach` for null safety (Java assumes non-null return).

`Single.init`, `Random.activateParticles`, `Animated.update`: all faithfully ported with correct
channel offset arithmetic. `load`/`save`: `SaveData` null checks use `Nullable` pattern.
`write`/`read`(Json) omitted.

---

## Common patterns across all files

| Pattern | Java | Scala |
|---------|------|-------|
| Array access | `channel.data[i]` | `channel.floatData(i)` / `channel.objectData(i)` |
| Collections | `Array<T>` | `DynamicArray[T]` |
| Static inner classes | `public static class` | Companion object classes |
| Null checks | `== null` / `!= null` | `Nullable` + `isEmpty`/`isDefined`/`foreach` |
| Null assignment | `data[i] = null` | `clearSlot(i)` |
| Json serialization | `write`/`read`(Json) | Omitted (deferred) |
| Pool inner classes | Named inner class extends `Pool` | `Pool.Default` + lambda |
| `dispose()` | `void dispose()` | `def close()` (AutoCloseable) |
| `Vector3.mul(Quaternion)` | `TMP_V3.mul(TMP_Q)` | `TMP_Q.transform(TMP_V3)` |
