# Audit: sge.graphics.g3d.environment

Audited: 8/8 files | Pass: 8 | Minor: 0 | Major: 0
Last updated: 2026-03-04

---

### BaseLight.scala -- pass
All 2 methods ported: `setColor(r,g,b,a)`, `setColor(Color)`.
Java `final` field mapped to Scala `val`. F-bounded type parameter `T <: BaseLight[T]` preserved.
Fluent builder setters (return `this`) — correctly kept as named methods.

### ShadowMap.scala -- pass
Java interface mapped to Scala trait. Both methods ported as property accessors:
`def projViewTrans: Matrix4`, `def depthMap: TextureDescriptor[?]`.
Raw `TextureDescriptor` in Java mapped to `TextureDescriptor[?]` (existential wildcard).

### DirectionalLight.scala -- pass
All 9 methods ported: `setDirection` x2, `set` x5, `equals` x2.
Java null checks (`if (color != null)`) converted to `Nullable.foreach`.
Typed `equals(DirectionalLight)` uses `Nullable.fold` for null safety.

### PointLight.scala -- pass
All 9 methods ported: `setPosition` x2, `setIntensity`, `set` x4, `equals` x2.
Java null checks converted to `Nullable.foreach`.
Typed `equals(PointLight)` uses `Nullable.fold` for null safety.

### SpotLight.scala -- pass
All 13 methods ported: `setPosition` x2, `setDirection` x2, `setIntensity`,
`setCutoffAngle`, `setExponent`, `set` x4, `setTarget`, `equals` x2.
Java null checks converted to `Nullable.foreach`.
Typed `equals(SpotLight)` uses `Nullable.fold` with `MathUtils.isEqual` for float comparisons.

### DirectionalShadowLight.scala -- pass
All 11 methods ported: `update` x2, `begin` x3, `end`, `frameBuffer`,
`camera`, `projViewTrans`, `depthMap`, `close`.
`Disposable` mapped to `AutoCloseable`; `dispose()` mapped to `close()`.
`Gdx.gl` mapped to `Sge().graphics.gl` via context parameter `(using Sge)`.
`fbo` changed from `FrameBuffer` to `Nullable[FrameBuffer]` for null safety;
`begin()` wraps operations in `fbo.foreach`. Java-style getters converted to
property accessors: `frameBuffer`, `camera`, `projViewTrans`, `depthMap`.

### SphericalHarmonics.scala -- pass
All 4 methods ported: `set(Array[Float])`, `set(AmbientCubemap)`, `set(Color)`, `set(r,g,b)`.
Two constructors: no-arg and array-validated via companion `apply`.
`coeff` array commented out (unused in upstream LibGDX).
`clamp` method omitted (unused in upstream LibGDX).
`GdxRuntimeException` mapped to `SgeError.InvalidInput`.

### AmbientCubemap.scala -- pass
All 13 methods ported: `set` x4, `getColor`, `clear`, `clamp`, `add` x5, `toString`.
Three constructors: no-arg, array (validated via companion `apply`), copy.
`dst()` renamed to `distance()` (SGE Vector3 rename).
`GdxRuntimeException` mapped to `SgeError.InvalidInput`.
`clamp` helper preserved as private in companion object.
