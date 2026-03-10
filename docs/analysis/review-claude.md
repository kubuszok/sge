# SGE Codebase Issue Analysis

Comprehensive analysis of potential bugs, missing tests, incorrect assumptions,
porting errors, and release blockers in the SGE (Scala port of LibGDX) codebase.

---

## 1. Test Coverage Gaps

### 1.1 Packages With Zero Tests

The codebase has 554 source files but only ~66 test suites (12% coverage).
The following major packages have **no tests at all**:

| Package | File Count | Risk Level |
|---------|-----------|------------|
| `graphics/g2d` (SpriteBatch, BitmapFont, Pixmap, TextureAtlas, etc.) | ~25 | **Critical** — core 2D rendering |
| `graphics/glutils` (ShaderProgram, FrameBuffer, ShapeRenderer, etc.) | ~35 | **Critical** — GPU resource management |
| `graphics/profiling` | ~7 | Low |
| `graphics/g3d/attributes` | ~10 | Medium |
| `graphics/g3d/decals` | ~8 | Medium |
| `graphics/g3d/environment` | ~8 | Medium |
| `graphics/g3d/model` | ~6 | Medium |
| `graphics/g3d/model/data` | ~10 | Medium |
| `graphics/g3d/particles` (entire particle system) | ~52 | High |
| `graphics/g3d/shaders` | ~3 | High — rendering correctness |
| `graphics/g3d/utils/shapebuilders` | ~11 | Medium |
| `scenes/scene2d` (core scene graph) | ~9 | **Critical** — UI framework |
| `scenes/scene2d/actions` (all 34 action types) | ~34 | High |
| `scenes/scene2d/utils` (21 utility classes) | ~21 | High |
| `scenes/scene2d/ui` (36 widgets, only SkinStyleReader tested) | ~35 | **Critical** — all UI widgets |
| `assets/loaders` (16 loaders, none tested) | ~16 | **Critical** — asset pipeline |
| `maps` (root: Map, MapLayer, MapRenderer) | ~9 | High |
| `maps/objects` | ~8 | Medium |
| `maps/tiled/renderers` (6 renderers) | ~6 | High |
| `maps/tiled/tiles` | ~2 | Medium |
| Root package (Application, Sge, Game, Screen, etc.) | ~19 | **Critical** — core lifecycle |

### 1.2 Missing Flow/Integration Tests

- **No application lifecycle test**: `Application` → `ApplicationListener` → `Screen` flow is untested
- **No rendering pipeline test**: Camera → SpriteBatch → Texture → GL calls
- **No asset loading flow**: AssetManager → Loader → FileHandle → Resource lifecycle
- **No scene2d UI flow**: Stage → Actor → Event propagation → Layout → Drawing
- **No input pipeline test**: Input → InputProcessor → InputMultiplexer → GestureDetector
- **No map rendering flow**: TiledMap loading → TiledMapRenderer → batch rendering

### 1.3 Existing Tests With TODOs

Multiple test files mention end-to-end tests that are planned but not written:
- All asset loaders have `TODOs: test <LoaderName>...` headers
- G3D model loading needs real `.g3dj` fixture tests
- Tiled map loading needs real `.tmx`/`.tmj` fixture tests

---

## 2. Stub and Incomplete Implementations

### 2.1 compareTo Stubs (FIXME else 0)

These always return 0 for the comparison fallback, breaking sorting:

| File | Issue |
|------|-------|
| `graphics/g3d/attributes/SpotLightsAttribute.scala:56` | `else 0 // FIXME implement comparing` |
| `graphics/g3d/attributes/PointLightsAttribute.scala:56` | `else 0 // FIXME implement comparing` |
| `graphics/g3d/attributes/DirectionalLightsAttribute.scala:56` | `else 0 // FIXME implement comparing` |
| `graphics/g3d/particles/ParticleShader.scala:113` | `else 0 // FIXME compare shaders` |
| `graphics/g3d/shaders/DefaultShader.scala:653` | Shader comparison stub |

**Impact**: Material/shader sorting will be incorrect, causing rendering artifacts
and performance issues from excessive state changes.

### 2.2 Pool.Poolable Integration Missing

Multiple classes have TODO comments about implementing `Pool.Poolable`:

- `scenes/scene2d/Stage.scala` — TouchFocus
- `scenes/scene2d/Event.scala`
- `scenes/scene2d/Action.scala`
- `scenes/scene2d/ui/Cell.scala`
- `graphics/g3d/utils/BaseAnimationController.scala` — Transform
- `graphics/g3d/utils/MeshPartBuilder.scala` — VertexInfo
- `math/FloatCounter.scala`

**Impact**: Object pooling won't work correctly, leading to excessive GC pressure
in games — a critical performance issue.

### 2.3 RemoteInput Stubs

`input/RemoteInput.scala` has many empty method bodies:
- `setOnscreenKeyboardVisible`, `openTextInputField`, `vibrate`, `setCatchKey`, etc.
- TODO: `postRunnable not yet wired`

### 2.4 Color Immutability TODO

`graphics/Color.scala` has `TODO: make immutable case class` — multiple files reference
this future change (Label, AlphaAction, ColorInfluencer, Colors). The current mutable
Color class is error-prone and affects several subsystems.

---

## 3. Null Safety Issues

### 3.1 Direct null Assignments (Violating Project Rules)

The project rule says "No null: use Nullable[A]", but many files use raw null:

| File | Context |
|------|---------|
| `scenes/scene2d/Stage.scala:937-939` | `listenerActor = null`, `listener = null`, `target = null` in `reset()` |
| `graphics/g2d/GlyphLayout.scala:82-226` | Multiple `= null` assignments for glyph processing state |
| `graphics/g3d/ModelCache.scala:95` | `result.meshPart.mesh = null` |
| `maps/tiled/TideMapLoader.scala:244` | `var currentTileSet: TiledMapTileSet = null` |
| `maps/tiled/BaseTmjMapLoader.scala:154,753` | `runOnEndOfLoadTiled = null`, `var is: InputStream = null` |
| `maps/tiled/BaseTmxMapLoader.scala:171,520,777,976` | Multiple null assignments |
| `utils/TimSort.scala:113-117` | `this.a = null.asInstanceOf`, array clearing |
| `utils/ComparableTimSort.scala:96-99` | Same pattern |
| `utils/DynamicArray.scala:57-58,512,514,698` | `snapshot = null`, `recycled = null` |
| `graphics/g3d/decals/DecalBatch.scala:207` | `vertices = null` |
| `graphics/g3d/loader/ObjLoader.scala:82-348` | Multiple `var x = null` for parsing |
| `graphics/Mesh.scala:1096-1179` | `null.asInstanceOf` for local temps |
| `net/NetJavaServerSocketImpl.scala:76` | `server = null` |
| `net/NetJavaSocketImpl.scala:76` | `socket = null` |
| `assets/AssetManager.scala:854` | `RefCountedContainer(var obj: Any = null)` |

### 3.2 Dangerous getOrElse(null) Patterns

| File | Context | Risk |
|------|---------|------|
| `maps/MapProperties.scala:46` | `properties.getOrElse(key, null)` | Returns null to caller — NPE risk |
| `net/SgeHttpResponse.scala:39` | `response.header(name).getOrElse(null)` | Returns null |
| `graphics/g3d/ModelCache.scala:116` | `camera.getOrElse(null)` passed to sorter | NPE if no camera |
| `graphics/g3d/decals/DecalBatch.scala:161` | `lastMaterial.getOrElse(null).equals(...)` | NPE if no material |
| `maps/tiled/BaseTmjMapLoader.scala:637` | `image.getOrElse(null: FileHandle)` | NPE risk |

### 3.3 Missing @nowarn on orNull

`utils/SgeError.scala:19` uses `cause.orNull` without `@nowarn("msg=deprecated")`.

---

## 4. Unsafe Type Casts (asInstanceOf)

Over 90 uses of `asInstanceOf` across ~35 files. High-risk categories:

### 4.1 Unchecked Generic Casts

| File | Issue |
|------|-------|
| `scenes/scene2d/Group.scala:407` | `children(i).asInstanceOf[T]` — unchecked generic |
| `scenes/scene2d/ui/SkinStyleReader.scala:121` | `readStyle(...).asInstanceOf[S]` |
| `scenes/scene2d/Action.scala:62` | `p.asInstanceOf[Pool[Action]]` |
| `scenes/scene2d/actions/Actions.scala:78` | `p.asInstanceOf[Pool[?]]` |
| `graphics/g3d/Attributes.scala:63` | `get(tpe).map(_.asInstanceOf[T])` |
| `graphics/g3d/particles/ParticleController.scala:242-300` | Multiple unsafe casts |
| `utils/Select.scala:50` | `items.asInstanceOf[Array[AnyRef]]` |

### 4.2 Buffer Casts (Cross-platform Risk)

Many Buffer casts to work around JDK API differences:

| File | Count |
|------|-------|
| `graphics/glutils/ShaderProgram.scala` | 8 casts |
| `graphics/glutils/InstanceBufferObject.scala` | ~10 casts |
| `graphics/glutils/VertexBufferObjectSubData.scala` | ~8 casts |
| `graphics/glutils/VertexArray.scala` | ~6 casts |
| `graphics/Mesh.scala` | ~8 casts |
| `graphics/PixmapIO.scala` | ~6 casts |

### 4.3 Style/Widget Downcasts

UI widgets downcast styles unsafely:
- `CheckBox.scala:51` — `style.asInstanceOf[CheckBoxStyle]`
- `Slider.scala:93-122` — 5 `super.getStyle.asInstanceOf[SliderStyle]`
- `Container.scala:207` — `a.asInstanceOf[Actor]`

---

## 5. Error Handling Issues

### 5.1 Swallowed Exceptions

| File | Pattern | Risk |
|------|---------|------|
| `utils/StreamUtils.scala:123` | `catch { case _: Throwable => }` | Swallows ALL throwables including OOM |
| `utils/XmlReader.scala:45` | `catch { case _: Exception => () }` | Silent XML parse failures |
| `graphics/g2d/BitmapFont.scala:514` | `catch { case _: Exception => }` | Silent font load failures |
| `input/RemoteSender.scala:59` | `case _: Exception =>` | Silent network errors |
| `utils/I18nBundle.scala:347,362` | `case _: IOException =>`, `case _: Exception => false` | Silent i18n failures |
| `graphics/profiling/GLErrorListener.scala:54` | `case _: Exception =>` | Profiler swallows its own errors |
| `graphics/g3d/loader/ObjLoader.scala:172,408` | `case _: IOException =>` | Silent model load failures |

### 5.2 StreamUtils Catching Throwable

`StreamUtils.scala:123` catches `Throwable` (not just `Exception`), which will catch
`OutOfMemoryError`, `StackOverflowError`, and other fatal JVM errors. This should be
`Exception` at most.

---

## 6. Concurrency and Thread Safety

### 6.1 Complex Synchronized Patterns

| File | Concern |
|------|---------|
| `utils/Timer.scala` | ~30 synchronized blocks; TODO notes JS compatibility issues. Complex locking that could deadlock. |
| `assets/AssetManager.scala` | ~60 synchronized methods; `RefCountedContainer` with `var obj: Any = null` |
| `InputEventQueue.scala` | 11 synchronized blocks for event queue |
| `input/RemoteSender.scala` | 18 `this.synchronized` blocks |
| `net/SgeHttpClient.scala` | 6 `pending.synchronized` blocks |

### 6.2 Timer.scala JS Compatibility

Timer.scala has a TODO about redesigning with Gears structured concurrency.
The current `synchronized`-based implementation will not work on Scala.js
(which is single-threaded and doesn't support `synchronized`).

### 6.3 Cross-Platform Compilation Concerns

Files using `synchronized` will need Scala.js alternatives. This affects:
- Timer, AssetManager, InputEventQueue, SgeHttpClient, BufferUtils, PixmapIO

---

## 7. Porting Logic Errors

### 7.1 Java-to-Scala Idiom Issues

| File | Issue |
|------|-------|
| `utils/ComparableTimSort.scala` | 10+ `asInstanceOf[Comparable[AnyRef]]` casts — TODO to replace |
| `graphics/g3d/loader/ObjLoader.scala` | Java-style null-based parsing with `var line = null` loops |
| `maps/tiled/BaseTmxMapLoader.scala` | Java-style InputStream management with null checks |
| `maps/tiled/TideMapLoader.scala` | `var currentTileSet: TiledMapTileSet = null` — Java pattern |

### 7.2 Java Collections Used Instead of Scala

| File | Java Collection Used |
|------|---------------------|
| `Net.scala:19` | `import java.util.{ List, Map }` |
| `net/SgeHttpResponse.scala:41-45` | `java.util.LinkedHashMap`, `java.util.ArrayList` for `getHeaders()` |
| `utils/I18nBundle.scala:18,273` | `ArrayList`, `Locale` |

---

## 8. TODO/FIXME Inventory (Impact Assessment)

### 8.1 High-Impact TODOs

| Location | TODO | Impact |
|----------|------|--------|
| `graphics/Color.scala:9` | Make immutable case class | Affects Label, AlphaAction, ColorInfluencer, Colors — thread safety, mutation bugs |
| `utils/Timer.scala:14` | Redesign with Gears structured concurrency | JS compatibility blocker |
| `utils/Show.scala:5` | Replace with FastShowPretty from kindlings | Minor |
| `utils/TimeUtils.scala:10` | Delegate to scala-java-time | Cross-platform time handling |
| `graphics/g3d/Model.scala:286` | FIXME uvScaling ignored | 3D texture rendering bug |
| `graphics/g3d/ModelCache.scala:266` | FIXME Make better MeshPool | Memory management |
| `graphics/g3d/utils/DefaultRenderableSorter.scala:62` | FIXME better sorting algorithm | Render performance |
| `graphics/g3d/Attributes.scala:101,153` | FIXME sort See #4186 | Attribute ordering bug (references LibGDX issue) |

### 8.2 Untested Asset Loaders

Every asset loader has a TODO header saying it needs tests:
- TextureLoader, TextureAtlasLoader, BitmapFontLoader, ModelLoader
- PixmapLoader, MusicLoader, SoundLoader, SkinLoader
- CubemapLoader, I18NBundleLoader, ShaderProgramLoader
- ParticleEffectLoader, FileHandleResolver

---

## 9. Release Blockers

### 9.1 Critical Path Items

1. **No integration tests** — Cannot verify that the ported code actually renders anything
2. **Asset loader pipeline untested** — Games cannot load resources without this working
3. **Scene2D completely untested** — The primary UI framework has 100 files and 1 test
4. **Timer.scala won't work on Scala.js** — Uses `synchronized` which doesn't exist in JS
5. **compareTo stubs in shaders/attributes** — Will cause incorrect rendering
6. **Pool.Poolable not integrated** — Will cause GC pressure issues in real games
7. **77 files still `not_started`** — Per migration-status.tsv
8. **17 files `deferred`** — Per migration-status.tsv
9. **Color mutability** — Mutable Color class is referenced across many subsystems

### 9.2 Platform-Specific Blockers

- **Scala.js**: `synchronized`, Thread, Timer, Java IO streams
- **Scala Native**: FFI boundaries, native buffer management
- **Android**: Android-specific backends need integration testing

---

## 10. Specific Bug Candidates

*(To be expanded by deeper analysis)*

### 10.1 DecalBatch NPE

`graphics/g3d/decals/DecalBatch.scala:161` calls `.equals()` on a potentially null value:
```
lastMaterial.getOrElse(null).equals(...)
```

### 10.2 MapProperties Returns null

`maps/MapProperties.scala:46` returns `null` when a key is missing via `getOrElse(key, null)`.
Callers may not expect null from a Scala API.

### 10.3 ModelCache null mesh

`graphics/g3d/ModelCache.scala:95` sets `result.meshPart.mesh = null` which could cause
NPE when the mesh part is later used for rendering.

### 10.4 GlyphLayout Null State

`graphics/g2d/GlyphLayout.scala` has multiple null assignments during glyph processing
(lines 82-226). Any interruption or exception during layout could leave the object in
an inconsistent state.

---

## 11. Sections 11-13 Reserved

*(Reserved for future analysis areas)*

---

## 14. Math Package Issues

### 14.1 MathUtils.tan() — Range reduction not applied to polynomial evaluation (CRITICAL)

**File**: `sge/src/main/scala/sge/math/MathUtils.scala:109-123`

The `tan()` method computes a range-reduced variable `r` but then evaluates the Padé
approximation polynomial using the **original `radians` parameter** instead of `r`.

```scala
def tan(radians: Float): Float = {
  var r = radians / PI       // range-reduce into local var r
  r += 0.5f
  r = (r - Math.floor(r)).toFloat
  r -= 0.5f
  r *= PI
  val x2 = radians * radians  // BUG: uses 'radians' not 'r'
  val x4 = x2 * x2
  radians * (...)              // BUG: uses 'radians' not 'r'
```

In the Java original, `radians` is a mutable parameter that is reassigned in-place.
Scala parameters are immutable, so the port created `var r` but then forgot to use it
in the polynomial evaluation.

**Impact**: `tan()` returns incorrect results for all inputs outside approximately
`[-PI/2, PI/2]`. The range-reduction code is dead.

**Fix**: Replace `radians` with `r` in the `val x2` and final return expression.

---

### 14.2 Matrix4.mulLeft() — Result not copied back to this (CRITICAL)

**File**: `sge/src/main/scala/sge/math/Matrices.scala:1046-1050`

```scala
def mulLeft(matrix: Matrix4): Matrix4 = {
  Matrix4.tmpMat.set(matrix)
  Matrix4.mul(Matrix4.tmpMat.values, values)
  this  // BUG: result is in tmpMat.values, not this.values
}
```

The Java original:

```java
public Matrix4 mulLeft (Matrix4 matrix) {
  tmpMat.set(matrix);
  mul(tmpMat.val, val);
  return set(tmpMat);  // <-- copies result back to this
}
```

`Matrix4.mul(a, b)` stores the result in array `a`. After the call, the result lives
in `tmpMat.values`, but the SGE version returns `this` (unchanged). The critical
`set(tmpMat)` call is missing.

**Impact**: `mulLeft()` is effectively a **no-op**. All code using matrix pre-multiplication
is broken. This affects camera transforms, bone transforms, scene graph hierarchy, and
any composed transformation that uses `mulLeft`.

**Fix**: Add `set(Matrix4.tmpMat)` before `this`.

---

### 14.3 Matrix3 tmp array missing M22=1 initialization (CRITICAL)

**File**: `sge/src/main/scala/sge/math/Matrices.scala:34`

```scala
private val tmp: Array[Float] = Array.ofDim(9)  // all zeros
```

The Java original:

```java
private float[] tmp = new float[9];
{
  tmp[M22] = 1;  // instance initializer sets bottom-right to 1
}
```

The `translate()`, `rotate()`, and `scale()` methods on Matrix3 build a temporary
transformation matrix in `tmp` and then multiply. These methods comment out the M22
assignment (e.g. `// tmp(Matrix3.M22) = 1`) because the Java code pre-initializes it.
But in the SGE port, `tmp(8)` remains 0.

**Impact**: Every call to `Matrix3.translate()`, `Matrix3.rotate()`, or `Matrix3.scale()`
multiplies by a matrix with a 0 in the bottom-right corner instead of 1. This causes
all 2D matrix transformations using these methods to produce incorrect results
(effectively projecting everything to zero in the third component).

**Fix**: Add `tmp(Matrix3.M22) = 1` after the array allocation (or in an init block).

---

### 14.4 Affine2.set(Matrix3) — Wrong array indices (CRITICAL)

**File**: `sge/src/main/scala/sge/math/Affine2.scala:87-97`

The SGE code uses **sequential array indices** instead of the Matrix3 column-major constants:

```scala
m00 = other(0)  // M00=0 ✓
m01 = other(1)  // WRONG: M01=3, reads M10 instead
m02 = other(2)  // WRONG: M02=6, reads M20 instead
m10 = other(3)  // WRONG: M10=1, reads M01 instead
m11 = other(4)  // M11=4 ✓
m12 = other(5)  // WRONG: M12=7, reads M21 instead
```

The Java original uses `other[Matrix3.M00]` through `other[Matrix3.M12]` which correctly
maps to indices (0, 3, 6, 1, 4, 7) in the column-major layout.

**Impact**: Setting an Affine2 from a Matrix3 produces a completely garbled transformation.
Rows and columns are swapped (reading the transpose, then mixing in the third row).

**Fix**: Use `other(Matrix3.M00)` through `other(Matrix3.M12)`.

---

### 14.5 Affine2.set(Matrix4) — Wrong array indices (CRITICAL)

**File**: `sge/src/main/scala/sge/math/Affine2.scala:108-118`

Same category of bug as 14.4. The SGE code uses sequential indices:

```scala
m00 = other(0)  // M00=0  ✓
m01 = other(1)  // WRONG: M01=4, reads M10 instead
m02 = other(3)  // WRONG: M03=12, reads M30 instead
m10 = other(4)  // WRONG: M10=1, reads M01 instead
m11 = other(5)  // M11=5  ✓
m12 = other(7)  // WRONG: M13=13, reads M31 instead
```

The Java original uses `other[Matrix4.M00]`, `other[Matrix4.M01]`, `other[Matrix4.M03]`,
`other[Matrix4.M10]`, `other[Matrix4.M11]`, `other[Matrix4.M13]` which map to
(0, 4, 12, 1, 5, 13).

**Impact**: Setting an Affine2 from a Matrix4 produces a completely garbled transformation.
This breaks any 2D rendering that extracts an Affine2 from a 4x4 projection/view matrix.

**Fix**: Use `other(Matrix4.M00)` through `other(Matrix4.M13)`.

---

### 14.6 Vector3.rotateAroundRad is a stub (CRITICAL)

**File**: `sge/src/main/scala/sge/math/Vectors.scala:898-901`

```scala
def rotateAroundRad(axis: Vector3, radians: Float): this.type =
  // This would need proper quaternion/matrix implementation
  // Placeholder for now
  this
```

This method (and its `rotateAroundDeg` wrapper) is a **no-op**. The Java original performs
a full quaternion-based rotation around an arbitrary axis. The SGE version returns `this`
unchanged.

**Impact**: Any 3D rotation around an arbitrary axis via Vector3 does nothing. This
affects camera orbiting, bone rotations, particle effects, and many 3D operations.

**Fix**: Implement using Rodrigues' rotation formula (same as the existing `rotateRad`
with axis components) or quaternion rotation.

---

### 14.7 MathUtils.randomGenerator uses java.util.Random instead of RandomXS128

**File**: `sge/src/main/scala/sge/math/MathUtils.scala:412`

```scala
val randomGenerator = new Random()  // java.util.Random
```

The Java original:

```java
static public Random random = new RandomXS128();
```

RandomXS128 is a xorshift128+ generator that is significantly faster than
`java.util.Random` and produces better-distributed random numbers. The SGE codebase
has a `RandomXS128.scala` file but doesn't use it here.

**Impact**: Slower random number generation and different distribution characteristics.
Games relying on specific random behavior will differ from LibGDX.

---

### 14.8 MathUtils.atanUncheckedDeg returns Float instead of Double — precision loss

**File**: `sge/src/main/scala/sge/math/MathUtils.scala:207`

```scala
def atanUncheckedDeg(i: Double): Float = { ... }
```

The Java original returns `double`:

```java
public static double atanUncheckedDeg (double i) { ... }
```

This means that in `atan2Deg` and `atan2Deg360`, the addition of 180.0 or 360.0
is performed in `float` precision instead of `double` precision:

- SGE: `atanUncheckedDeg(n) + 180.0f` (float + float)
- Java: `(float)(atanUncheckedDeg(n) + 180.0)` (double + double, then cast)

**Impact**: Reduced precision in degree-based atan2 calculations. The error is small
(~1e-5 degrees) but could accumulate in tight loops.

---

### 14.9 Vector3/Vector4 companion object constants are mutable singletons

**File**: `sge/src/main/scala/sge/math/Vectors.scala:976-979, 1394-1398`

```scala
object Vector3 {
  val X:    Vector3 = Vector3(1, 0, 0)
  val Y:    Vector3 = Vector3(0, 1, 0)
  val Z:    Vector3 = Vector3(0, 0, 1)
  val Zero: Vector3 = Vector3(0, 0, 0)
}
```

These are `val` references to **mutable** case classes. Any code that calls a mutating
method on `Vector3.X` (e.g., `Vector3.X.scale(2f)`) would corrupt the shared constant
for all users. This is the same issue as in LibGDX, but the Scala port has an opportunity
to fix it (e.g., by making the constants computed `def` that return fresh copies, or by
adding a frozen/immutable vector type).

**Impact**: Potential global state corruption if any code mutates these constants.
Debugging such issues is extremely difficult since the bug manifests far from the
mutation site.

---

### 14.10 Quaternion and Intersector use thread-unsafe shared mutable state

**Files**:
- `sge/src/main/scala/sge/math/Quaternion.scala:994-995` — `tmp1`, `tmp2`
- `sge/src/main/scala/sge/math/Intersector.scala:42-68` — `v0`, `v1`, `v2`, `tmp`, `ip`, `ep1`, etc.

These are static mutable fields used as scratch space in methods like `transform()`,
`slerp()`, `isPointInTriangle()`, etc. If two threads call these methods simultaneously,
they will corrupt each other's intermediate results.

**Impact**: Incorrect results or crashes in multithreaded contexts. LibGDX has the same
issue (it's designed for single-threaded game loops), but SGE should document this
restriction clearly.

---

### 14.11 Intersector — Orphaned Vector3() allocations

**File**: `sge/src/main/scala/sge/math/Intersector.scala:66-67`

```scala
private val i    = Vector3()
Vector3()  // orphaned — result discarded
Vector3()  // orphaned — result discarded
```

Two `Vector3()` allocations appear after the `val i` declaration with their results
discarded. These likely correspond to Java fields `private static Vector3 v2tmp = ...`
or similar that were not assigned to a variable during porting.

**Impact**: Missing scratch vectors may cause later methods to fail or use wrong variables.
Need to compare against Java to identify which fields were intended.

---

### 14.12 Vector2.angleDeg(x, y) ignores the vector instance

**File**: `sge/src/main/scala/sge/math/Vectors.scala:400-404`

```scala
def angleDeg(x: Float, y: Float): Float = {
  var angle: Float = (Math.atan2(y, x) * MathUtils.radiansToDegrees).toFloat
  if (angle < 0) angle += 360
  angle
}
```

This instance method takes `x` and `y` parameters but computes the angle of the
point `(x, y)` without any reference to `this` vector. Calling `myVec.angleDeg(1, 0)`
returns the angle of `(1, 0)`, not the angle between `myVec` and `(1, 0)`. This method
does not exist in the LibGDX Java Vector2 class.

**Impact**: Confusing API. Callers expecting a relative angle calculation will get the
absolute angle of the parameter point instead.

---

### 14.13 Matrix3 constructor does not call idt() — values array left as all zeros

**File**: `sge/src/main/scala/sge/math/Matrices.scala:32-34`

```scala
class Matrix3 {
  val values: Array[Float] = Array.ofDim(9)  // all zeros
  private val tmp: Array[Float] = Array.ofDim(9)
```

The Java original calls `idt()` in the no-arg constructor:

```java
public Matrix3 () {
  idt();
}
```

The SGE Matrix3 default constructor leaves `values` as all zeros (a non-invertible,
non-identity matrix). Any code that creates a `Matrix3()` and expects the identity
matrix will get a zero matrix instead.

**Impact**: All newly created Matrix3 instances are zero matrices instead of identity
matrices. This will cause rendering transforms, camera setup, and any matrix-based
computation starting from `new Matrix3()` to produce wrong results.

**Fix**: Add `idt()` call in the class body or as an initializer.

---

### 14.14 Summary of Critical Math Issues

| # | File | Issue | Severity |
|---|------|-------|----------|
| 14.1 | MathUtils.scala | `tan()` ignores range reduction | **Critical** |
| 14.2 | Matrices.scala | `Matrix4.mulLeft()` is a no-op | **Critical** |
| 14.3 | Matrices.scala | Matrix3 `tmp[M22]` not initialized to 1 | **Critical** |
| 14.4 | Affine2.scala | `set(Matrix3)` uses wrong indices | **Critical** |
| 14.5 | Affine2.scala | `set(Matrix4)` uses wrong indices | **Critical** |
| 14.6 | Vectors.scala | `Vector3.rotateAroundRad` is a stub | **Critical** |
| 14.7 | MathUtils.scala | Uses `Random` instead of `RandomXS128` | High |
| 14.8 | MathUtils.scala | `atanUncheckedDeg` returns Float not Double | Medium |
| 14.9 | Vectors.scala | Mutable constants in companion objects | Medium |
| 14.10 | Quaternion/Intersector | Thread-unsafe shared mutable state | Medium |
| 14.11 | Intersector.scala | Orphaned `Vector3()` allocations | Low |
| 14.12 | Vectors.scala | `angleDeg(x, y)` ignores `this` | Low |
| 14.13 | Matrices.scala | `Matrix3()` not initialized to identity | **Critical** |

Seven **critical** bugs that will cause incorrect math results in any application
using these core utilities. Issues 14.1–14.6 and 14.13 should be fixed before any
release.

---

## 11. Graphics G2D Porting Issues

### 11.1 GlyphLayout.wrapGlyphs — boundary.break exits entire method instead of loop

**File:** `graphics/g2d/GlyphLayout.scala:352-354,358-360`
**Severity:** CRITICAL — breaks word wrapping entirely

In the Java version, `wrapGlyphs` uses `break` to exit two while loops that scan for
whitespace boundaries. In the Scala port, `scala.util.boundary.break(Nullable.empty)`
exits the *entire method*, returning `Nullable.empty`:

```scala
// Skip whitespace before the wrap index.
var firstEnd = wrapIndex
while (firstEnd > 0) {
  if (!fontData.isWhitespace(glyphs2(firstEnd - 1).id.toChar))
    scala.util.boundary.break(Nullable.empty)  // BUG: exits method, should break loop
  firstEnd -= 1
}
// Skip whitespace after the wrap index.
var secondStart = wrapIndex
while (secondStart < glyphCount) {
  if (!fontData.isWhitespace(glyphs2(secondStart).id.toChar))
    scala.util.boundary.break(Nullable.empty)  // BUG: exits method, should break loop
  secondStart += 1
}
```

In Java, finding a non-whitespace character just stops scanning (breaks the loop) and
continues to the split/copy logic below. In Scala, it immediately returns null from the
method, aborting the wrap. Since non-whitespace at boundaries is the *normal case* for
word wrapping, this means **virtually every text wrap attempt will fail**.

**Impact:** Text that should wrap will either not wrap or produce garbled output in
BitmapFont rendering. Any UI using BitmapFont with wrapping is broken.

---

### 11.2 BitmapFontCache.draw(Batch, Int, Int) — boundary.break exits method instead of inner loop

**File:** `graphics/g2d/BitmapFontCache.scala:234-268`
**Severity:** CRITICAL — multi-page font rendering broken

The `draw(spriteBatch, start, end)` method for multi-page fonts uses
`scala.util.boundary.break()` where Java uses `break` (inner loop) and `continue`
(outer loop):

```scala
for (i <- pageVertices.indices) {
  // ...
  while (ii < glyphIndices.size) {
    val glyphIndex = glyphIndices(ii)
    if (glyphIndex >= end) scala.util.boundary.break()  // BUG: exits entire method!
    // ...
  }
  if (offset == -1 || count == 0) scala.util.boundary.break()  // BUG: exits entire method!
  spriteBatch.draw(...)
}
```

In Java: `break` exits the inner while loop (page iteration continues); `continue`
skips to the next page. In Scala: both exit the *entire method*, meaning **only the
first page of a multi-page font gets rendered**, and pages with no matching glyphs
cause all subsequent pages to be skipped.

**Impact:** Multi-page BitmapFonts (fonts with multiple texture pages) will render
incompletely when using the `draw(batch, start, end)` overload.

---

### 11.3 BitmapFontCache.setColors(Float, Int, Int) — same boundary.break bug

**File:** `graphics/g2d/BitmapFontCache.scala:180-215`
**Severity:** HIGH — multi-page font coloring broken

Same pattern as 11.2. The inner `break` and outer `continue` in Java both become
`scala.util.boundary.break()` in Scala, which exits the method entirely:

```scala
while (j < glyphIndices.size) {
  val glyphIndex = glyphIndices(j)
  if (glyphIndex >= end) scala.util.boundary.break()  // BUG: exits method, not loop
  // ...
}
```

**Impact:** Setting colors on a range of glyphs in multi-page fonts will only affect
the first page, leaving remaining pages with stale colors.

---

### 11.4 BitmapFontCache.setAlphas — method completely broken (commented-out logic)

**File:** `graphics/g2d/BitmapFontCache.scala:137-157`
**Severity:** CRITICAL — alpha modulation does not work

The `setAlphas` method has two bugs:
1. `newColor` is declared as `val` (immutable) initialized to `0f`, never updated
2. The alpha recalculation logic is **commented out**

```scala
def setAlphas(alpha: Float): Unit = {
  var prev     = 0f
  val newColor = 0f    // BUG: should be var, and computed below
  for (j <- pageVertices.indices) {
    val vertices = pageVertices(j)
    var i        = 2
    while (i < idx(j)) {
      val c = vertices(i)
      if (c == prev && i != 2) {
        vertices(i) = newColor          // Always sets to 0f (black transparent)
      } else {
        prev = c
        // val rgba = NumberUtils.floatToIntColor(c)    // COMMENTED OUT
        // rgba = (rgba & 0x00FFFFFF) | alphaBits       // COMMENTED OUT
        // newColor = NumberUtils.intToFloatColor(rgba)  // COMMENTED OUT
        vertices(i) = newColor          // Always sets to 0f (black transparent)
      }
      i += 5
    }
  }
}
```

The Java version computes `alphaBits = ((int)(254 * alpha)) << 24`, then for each
packed color in the vertex data: decodes it, replaces the alpha channel with `alphaBits`,
and re-encodes. The Scala version sets all colors to 0f.

**Impact:** Any code calling `setAlphas` (e.g., fading text in/out) will make all text
invisible (black transparent) regardless of the alpha parameter.

---

### 11.5 Sprite.set() — property setters corrupt UV coordinates via rounding

**File:** `graphics/g2d/Sprite.scala:132-153`
**Severity:** MEDIUM — subtle UV coordinate drift when copying sprites

In Java, `Sprite.set()` directly accesses package-private fields (`u`, `v`, `u2`, `v2`,
`regionWidth`, `regionHeight`). In Scala, these go through property setters with
side effects:

```scala
def set(sprite: Sprite): Unit = {
  // ...
  u = sprite.u          // calls u_= → recalculates _regionWidth from wrong _u2
  v = sprite.v          // calls v_= → recalculates _regionHeight from wrong _v2
  u2 = sprite.u2        // calls u2_= → now _regionWidth is correct
  v2 = sprite.v2        // calls v2_= → now _regionHeight is correct
  // ...
  regionWidth = sprite.regionWidth    // calls regionWidth_= → MODIFIES u or u2!
  regionHeight = sprite.regionHeight  // calls regionHeight_= → MODIFIES v or v2!
}
```

The `regionWidth_=` setter recalculates `u` or `u2` from the integer `regionWidth`
divided by texture width. Due to integer rounding, the resulting UV coordinate differs
from the original:
- Original `u2 = 0.3` on a 256px texture
- `regionWidth = round(abs(0.3 - 0.1) * 256) = round(51.2) = 51`
- `regionWidth_=` sets `u2 = 0.1 + 51/256 = 0.29921875` (not 0.3!)

**Impact:** Sprites copied via `set()` or copy constructor will have slightly incorrect
texture coordinates, causing sub-pixel texture sampling artifacts.

---

### 11.6 SpriteBatch.disableBlending/enableBlending — missing early-return optimization

**File:** `graphics/g2d/SpriteBatch.scala:985-993`
**Severity:** LOW — performance issue, not correctness

In Java, `disableBlending()` returns immediately if blending is already disabled
(`if (blendingDisabled) return;`). The Scala port always flushes and sets the state:

```scala
override def disableBlending(): Unit = {
  if (drawing) flush()    // Java: only flushes if state is actually changing
  blendingDisabled = true
}
```

**Impact:** Unnecessary GPU flushes if `disableBlending()` is called when blending is
already disabled (and vice versa for `enableBlending()`). Performance impact in
tight render loops.

---

### 11.7 SpriteCache.tempVertices — instance field moved to companion object (shared state)

**File:** `graphics/g2d/SpriteCache.scala:990`
**Severity:** MEDIUM — thread-safety / multi-instance corruption

In Java, `tempVertices` is an instance field:
```java
private float[] tempVertices = new float[VERTEX_SIZE * 6];
```

In Scala, it's in the companion object (static/shared):
```scala
object SpriteCache {
  private val tempVertices: Array[Float] = Array.ofDim[Float](VERTEX_SIZE * 6)
```

If multiple `SpriteCache` instances write to `tempVertices` concurrently (or even
interleaved on the same thread during nested cache building), they will corrupt
each other's vertex data.

**Impact:** Incorrect rendering when using multiple SpriteCache instances.

---

### 11.8 NinePatch — missing flipY sign handling in region constructor

**File:** `graphics/g2d/NinePatch.scala:136-177`
**Severity:** HIGH — NinePatch from flipped regions renders incorrectly

The Java constructor computes `final int sign = region.isFlipY() ? -1 : 1;` and uses
`sign * top`, `sign * middleHeight`, `sign * bottom` when creating sub-regions. The
Scala version omits the `sign` entirely:

Java:
```java
if (top > 0) {
  if (left > 0) patches[TOP_LEFT] = new TextureRegion(region, 0, 0, left, sign * top);
  // ...
}
```

Scala:
```scala
if (top > 0) {
  if (left > 0) patches(NinePatch.TOP_LEFT) = Nullable(TextureRegion(region, 0, 0, left, top))
  // ...
}
```

**Impact:** NinePatches created from vertically-flipped TextureRegions will have
inverted row ordering, causing the top/bottom rows to be swapped. Common when
using TextureAtlas regions that may be flipped.

---

### 11.9 PolygonSpriteBatch.draw(Texture, Array[Float], ...) — triangle indices not incrementing

**File:** `graphics/g2d/PolygonSpriteBatch.scala:735-786`
**Severity:** HIGH — batch sprite drawing produces wrong geometry

The triangle index generation loop has two bugs:
1. `vertex` is computed once as a `val` (never incremented for subsequent sprites)
2. The loop variable `_` is unused; `triangleIdx` is not incremented inside the loop

```scala
val vertex = (vertexIdx / VERTEX_SIZE).toShort  // computed once
var triangleIdx = this.triangleIndex
for (_ <- triangleIdx until triangleIdx + triangleCount by 6) {
  triangles(triangleIdx) = vertex          // same index written every iteration
  triangles(triangleIdx + 1) = (vertex + 1).toShort
  // ...
}
```

This means:
- All triangle indices point to the same first quad's vertices
- Only the last iteration's writes survive (overwriting the same 6 array positions)

In the Java version, both `vertex` and the triangle write index are properly incremented
per quad.

**Impact:** `draw(texture, spriteVertices, offset, count)` on PolygonSpriteBatch renders
only the first sprite correctly; all subsequent sprites in the batch reference wrong
vertices, producing visual garbage.

---

### 11.10 CpuSpriteBatch.draw(Texture, Float, Float) — wrong srcY parameter

**File:** `graphics/g2d/CpuSpriteBatch.scala:186`
**Severity:** MEDIUM — full-texture draw produces wrong UVs when adjusted

The fallback to `drawAdjusted` passes `srcY=1` instead of `srcY=0`:

```scala
override def draw(texture: Texture, x: Float, y: Float): Unit =
  if (!adjustNeeded) {
    super.draw(texture, x, y);
  } else {
    drawAdjusted(texture, x, y, 0, 0, texture.getWidth.toFloat, texture.getHeight.toFloat,
      1, 1, 0, 0, 1, 1, 0, false, false);
    //           srcX=0, srcY=1, srcWidth=1, srcHeight=0  ← wrong!
  }
```

The Java version calls:
```java
drawAdjustedUV(texture, x, y, 0, 0, texture.getWidth(), texture.getHeight(), 1, 1, 0,
  0, 1, 1, 0, false, false);
```

Which passes UV coordinates directly (u=0, v=1, u2=1, v2=0). But in the Scala version
it goes through `drawAdjusted` which interprets these as pixel source coords
(srcX=0, srcY=1, srcWidth=1, srcHeight=0), then computes UVs from them. This produces
completely wrong texture coordinates.

**Impact:** `CpuSpriteBatch.draw(texture, x, y)` renders an incorrect/invisible texture
when transform adjustment is active.

---

### 11.11 TextureRegion.split (companion object) — uses different implementation than Java

**File:** `graphics/g2d/TextureRegion.scala:332-344`
**Severity:** LOW — functionally equivalent but diverges from Java pattern

In Java, the static `split(Texture, tileWidth, tileHeight)` delegates to the instance
`split` method: `return region.split(tileWidth, tileHeight)`. The Scala companion object
version reimplements the logic independently instead of delegating. This means bug fixes
to the instance `split` won't automatically apply to the companion version.

---

### 11.12 PolygonSpriteBatch.flush — uses triangles.slice instead of direct index

**File:** `graphics/g2d/PolygonSpriteBatch.scala:1204`
**Severity:** LOW — unnecessary allocation per flush

```scala
mesh.setIndices(triangles.slice(0, trianglesInBatch))
```

This creates a new array every flush. The Java version uses `mesh.setIndices(triangles, 0,
trianglesInBatch)` which avoids the allocation. If `setIndices` accepts offset/length
parameters in SGE's Mesh, this should use those instead.

---

### 11.13 Animation.getKeyFrame(stateTime, looping) — faithfully ported Java bug

**File:** `graphics/g2d/Animation.scala:88-93`
**Severity:** LOW — pre-existing LibGDX bug

When `looping=false` and current playMode is `LOOP` (or `LOOP_PINGPONG`, `LOOP_RANDOM`),
the method sets playMode to `LOOP` instead of `NORMAL`:

```scala
} else if (!looping && ...) {
  if (playMode == Animation.PlayMode.LOOP_REVERSED)
    playMode = Animation.PlayMode.REVERSED
  else
    playMode = Animation.PlayMode.LOOP  // Should be NORMAL
}
```

This is a bug in the original LibGDX that was faithfully ported.

---

### 11.14 GlyphLayout.setText — raw null sentinels in hot loop

**File:** `graphics/g2d/GlyphLayout.scala:82-83`
**Severity:** LOW — style violation, not a correctness bug

```scala
var lineRun:   GlyphRun         = null
var lastGlyph: BitmapFont.Glyph = null
```

These use raw `null` instead of `Nullable[A]`. The migration notes acknowledge this
is intentional for hot-loop performance (50+ direct access sites), but it violates
the project's no-null rule and risks NPE if the surrounding logic changes.

---

### Summary of G2D Issues by Severity

| Severity | Count | Issues |
|----------|-------|--------|
| **CRITICAL** | 3 | 11.1 (GlyphLayout wrapping), 11.2 (BitmapFontCache multi-page draw), 11.4 (setAlphas broken) |
| **HIGH** | 3 | 11.3 (multi-page coloring), 11.8 (NinePatch flip), 11.9 (PolygonSpriteBatch triangles) |
| **MEDIUM** | 3 | 11.5 (Sprite.set UV drift), 11.7 (SpriteCache shared state), 11.10 (CpuSpriteBatch draw) |
| **LOW** | 5 | 11.6, 11.11, 11.12, 11.13, 11.14 |

The `boundary.break` misuse pattern (11.1, 11.2, 11.3) is the most dangerous class of
bug — Java `break`/`continue` in nested loops was mechanically translated to
`scala.util.boundary.break()` which exits the enclosing `boundary` (the method), not
just the innermost loop. This pattern should be audited across the entire codebase.

---

## 11. Scene2D UI Framework — Thread Safety & Reentrancy

### 11.1 InputListener.tmpCoords Shared Mutable State

`InputListener.tmpCoords` (companion object `Vector2`) is shared across ALL
InputListener instances. If an event handler fires a nested event (which is
common in Scene2D — e.g., a touchDown handler calls `stage.setKeyboardFocus`
which fires FocusEvents), the tmpCoords will be overwritten mid-computation.

**Files**: `InputListener.scala:143`, `Group.scala:543`

**Same issue exists in LibGDX** — but worth flagging as Scene2D events are
designed to be reentrant.

### 11.2 Group.tmp Shared Mutable State

`Group.tmp` (`companion object val tmp: Vector2`) is used in `hit()`. If a
custom `hit()` override fires events that recurse into another Group's `hit()`,
the shared Vector2 is corrupted. Same pattern as LibGDX.

---

## 12. Scene2D Porting Issues

### 12.1 Actor.fire() — Ascendants Array Not Pooled (Performance Bug)

In LibGDX, the ascendants array used during event propagation is obtained from
`POOLS.obtain(Array.class)` and returned to the pool in the `finally` block:

```java
// LibGDX — pooled
Array<Group> ascendants = POOLS.obtain(Array.class);
try { ... } finally {
    ascendants.clear();
    POOLS.free(ascendants);
}
```

In SGE, a new `DynamicArray[Group]()` is allocated on every `fire()` call
(Actor.scala:134) and only `clear()`ed — never pooled:

```scala
// SGE — allocates every time
val ascendants = DynamicArray[Group]()
try { ... } finally ascendants.clear()
```

**Impact**: Every event fire (mouse moves, touches, enter/exit — potentially
hundreds per frame) allocates and discards an array, creating significant GC
pressure in UI-heavy applications.

**Fix**: Obtain the ascendants array from `Actor.POOLS` and free it in the
finally block, matching the LibGDX pattern.

### 12.2 Actor.notify() — Snapshot Allocation Per Notification (Performance Bug)

Every call to `Actor.notify()` creates a new snapshot array via
`listenersToNotify.toArray` (Actor.scala:198). LibGDX uses
`DelayedRemovalArray` with `begin()`/`end()` which avoids allocation:

```java
// LibGDX — no allocation during iteration
listeners.begin();
for (int i = 0, n = listeners.size; i < n; i++)
    if (listeners.get(i).handle(event)) event.handle();
listeners.end();
```

```scala
// SGE — allocates a snapshot array every time
val snapshot = listenersToNotify.toArray
var i = 0
while (i < snapshot.length) { ... }
```

**Impact**: Combined with `fire()`, a single event propagation through a
hierarchy of N actors creates N+1 array allocations (1 for ascendants, N for
each actor's listener notification).

### 12.3 Group.act() / drawChildren() / drawDebugChildren() — Snapshot Allocation Per Frame (Performance Bug)

Multiple Group methods take snapshots of the children array every frame:

| Method | File:Line | Allocates |
|--------|-----------|-----------|
| `act()` | Group.scala:45 | `children.toArray` |
| `drawChildren()` | Group.scala:68 | `children.toArray` |
| `drawDebugChildren()` | Group.scala:162 | `children.toArray` |
| `clearChildren()` | Group.scala:374 | `children.toArray` |
| `toString()` | Group.scala:522 | `children.toArray` |

LibGDX uses `SnapshotArray` with `begin()`/`end()` which reuses a cached
snapshot array:

```java
// LibGDX — cached snapshot, no allocation
Actor[] actors = children.begin();
for (int i = 0, n = children.size; i < n; i++)
    actors[i].act(delta);
children.end();
```

**Impact**: For a UI with 50 actors in 10 groups, each frame allocates ~30
arrays (act + drawChildren + drawDebugChildren per group). Over time this
creates significant GC pressure compared to LibGDX's zero-allocation approach.

### 12.4 DelegateAction Subclasses Silently Complete When Action Is Empty

Multiple `DelegateAction` subclasses use `action.forall(_.act(delta))` to
delegate to the wrapped action. Since `action` is `Nullable[Action]`, `forall`
returns `true` when the action is empty (vacuous truth). In LibGDX, calling
`action.act(delta)` on a null would NPE, immediately surfacing the bug.

| File | Pattern | Java Behavior |
|------|---------|---------------|
| `DelayAction.scala:37,40` | `action.forall(_.act(actionDelta))` | NPE if null |
| `RepeatAction.scala:34` | `action.forall { a => ... }` | NPE if null |
| `AfterAction.scala:54` | `action.forall(_.act(delta))` | NPE if null |

**Impact**: If a DelegateAction is constructed without setting its wrapped
action (e.g., obtained from pool but not configured), it silently completes
instead of failing. This could cause actions to be "swallowed" without any
visible effect, making debugging very difficult.

**Fix**: Use `action.fold(throw new IllegalStateException("..."))(_.act(delta))`
or require the action to be set before execution.

### 12.5 AfterAction.delegate() Silently Completes When Target Is Empty

`AfterAction.delegate()` (AfterAction.scala:44) uses `target.forall { t => ... }`
which returns `true` when target is empty. In LibGDX, `target.getActions()`
would NPE if target is null.

```scala
// SGE — silently completes
override protected def delegate(delta: Seconds): Boolean =
    target.forall { t => ... }  // true when target is empty
```

**Impact**: Same as 12.4 — the action silently completes without executing,
masking configuration bugs.

### 12.6 Group.addActor() — Doesn't Clear Actor's Stage When Group Has No Stage

In LibGDX, `addActor()` always calls `actor.setStage(getStage())`, which
passes `null` if the group isn't in a stage. This explicitly clears any
previous stage reference:

```java
// LibGDX — always sets stage (even null)
actor.setStage(getStage());
```

In SGE, the stage is only set when the group HAS a stage:

```scala
// SGE — only sets if stage exists
stage.foreach(s => actor.setStage(Nullable(s)))
```

**Impact**: If an actor was previously in a stage and is then added to a group
that is NOT in any stage, the actor retains its old (stale) stage reference.
This could cause events to be fired on a stage that no longer owns the actor.
Same pattern exists in `addActorAt`, `addActorBefore`, `addActorAfter`.

**Fix**: Change to `actor.setStage(stage)` to always propagate the group's
stage (including empty).

### 12.7 Stage.setKeyboardFocus / setScrollFocus — FocusEvent Not Reset Between Fires

Both `setKeyboardFocus` and `setScrollFocus` reuse the same `FocusEvent` for
both the unfocus notification (old actor) and the focus notification (new actor)
without resetting the event between fires:

```scala
// Stage.scala:686-704
val event = pools.obtain[FocusListener.FocusEvent]
// ... fire unfocus on old actor ...
oldKeyboardFocus.foreach { old =>
    event.focused = false
    old.fire(event)   // May set handled/stopped
}
// ... check cancelled, then fire focus on new actor ...
actor.foreach { a =>
    event.focused = true
    a.fire(event)    // handled/stopped flags carry over!
}
```

**Impact**: If the unfocus event is `handled` or `stopped` (but not cancelled),
those flags carry over to the focus event. This could cause the focus event to
appear pre-handled to listeners. Same issue exists in LibGDX (not a porting
regression), but worth noting as it could cause subtle focus behavior bugs.

### 12.8 TemporalAction — duration and time Are Public Vars

In LibGDX, `TemporalAction.duration` and `time` are private with getters/setters.
In SGE (TemporalAction.scala:29,32), both are public constructor parameters
and public vars respectively. While this follows SGE's "no Java-style getters/
setters" convention, it allows external code to directly modify `time` during
execution, potentially causing actions to skip frames or run backwards.

More critically, `complete` is also a public var (line 39), allowing external
code to force-complete an action without going through `finish()`, bypassing
the `end()` callback.

### 12.9 RepeatAction.count Uses Different Field Name Than LibGDX

In LibGDX, the field is called `repeatCount` and is accessed via
`getCount()`/`setCount()`. In SGE (RepeatAction.scala:27), it's renamed to
`count` (a public var). The `Actions.repeat()` factory method sets
`action.count = count` (Actions.scala:460), which works correctly.

However, the FOREVER sentinel comparison uses `count` in both the `delegate`
method (`executedCount == count`) and the factory (`action.count = RepeatAction.FOREVER`).
In LibGDX, `FOREVER = -1` and the check is `executedCount == repeatCount`.
Since `executedCount` starts at 0 and `FOREVER = -1`, they're never equal,
so the action repeats forever. This is correct in both versions.

### 12.10 Stage.drawDebug() — Different ShapeRenderer Begin/End Pattern

In LibGDX (Stage.java:170-176), the ShapeRenderer's `begin()` call passes
`ShapeType.Line` implicitly via `setAutoShapeType(true)`:

```java
debugShapes.begin();
root.drawDebug(debugShapes);
debugShapes.end();
```

In SGE (Stage.scala:166), the `begin()` explicitly passes a ShapeType:

```scala
shapes.begin(ShapeRenderer.ShapeType.Line)
root.drawDebug(shapes)
shapes.end()
```

LibGDX's `begin()` with no args uses `ShapeType.Line` as default when
`autoShapeType` is true. The SGE version explicitly passing `ShapeType.Line`
is equivalent, so this is not a bug.

### 12.11 Event Fields Are Public Vars (Encapsulation Difference)

In LibGDX, `Event` fields (`stage`, `target`, `listenerActor`, `capture`,
`bubbles`) are all private with getters/setters. In SGE (Event.scala:36-51),
these are all public `var` fields.

While this follows the SGE convention, it means:
- Any code can modify `event.target` or `event.stage` during propagation
- `event.capture` and `event.bubbles` can be changed mid-fire
- No validation is possible on field changes

**Impact**: Lower encapsulation could lead to subtle bugs if user code
accidentally modifies event state during handling.

### 12.12 ClickListener — Button Default Semantics

In LibGDX, `ClickListener` defaults to `button = 0` (LEFT), and `-1` means
"any button". In SGE, `button = Button(0)` and `Button(-1)` is the "any button"
sentinel.

The check `this.button != Button(-1)` (ClickListener.scala:51) works correctly
if `Button` is an opaque wrapper around `Int` with structural equality.
However, if `Button` uses reference equality or the opaque type strips the
wrapping, `Button(-1) != Button(-1)` could occur, breaking the sentinel check.

**Severity**: Low (likely works, but fragile).

---

## 13. Utils/Data Structure Issues

### 13.1 [Sort.scala] - sort(DynamicArray[Comparable]) is a complete no-op

**Severity**: Critical — Sorting doesn't work for Comparable types

```scala
def sort[T <: Comparable[T]](a: DynamicArray[T]): Unit =
  comparableTimSort.doSort(a.toArray.asInstanceOf[Array[AnyRef]], 0, a.size)
```

`a.toArray` creates a **copy** of the backing array. The copy is sorted, but the result
is never written back to the DynamicArray. The method has no effect whatsoever.

The Java original sorts `a.items` in-place:
```java
comparableTimSort.doSort(a.items, 0, a.size);
```

**Impact**: Any code calling `Sort.sort(dynamicArray)` with Comparable elements
silently does nothing. The array remains unsorted.

### 13.2 [ComparableTimSort.scala:152] - Dead expression `base1 + k` in mergeAt causes incorrect merge positions

**Severity**: Critical — Sorting produces incorrect results

In `mergeAt`, after finding where the first element of run2 belongs in run1:

```scala
val base1 = runBase(i)  // val, not var!
...
val k = ComparableTimSort.gallopRight(a(base2).asInstanceOf[Comparable[AnyRef]], a, base1, len1, 0)
base1 + k   // DEAD EXPRESSION — computes a value and discards it
len1 -= k
```

The Java original correctly mutates `base1`:
```java
int base1 = this.runBase[i];  // mutable local
...
base1 += k;  // Advances the start position
len1 -= k;
```

Because `base1` is a `val` in the Scala version, `base1 + k` computes a new value that
is immediately discarded. The merge operations (`mergeLo`/`mergeHi`) then receive the
wrong starting position, causing incorrect sorting results for arrays >= 32 elements.

### 13.3 [TimSort.scala:191-203] - mergeLo and mergeHi are placeholder stubs

**Severity**: Critical — TimSort merge logic is not implemented

Both `mergeLo` and `mergeHi` in `TimSort.scala` are **placeholder implementations**
that fall back to `java.util.Arrays.sort`:

```scala
private def mergeLo(base1: Int, len1: Int, base2: Int, len2: Int): Unit = {
  val start = Math.min(base1, base2)
  val end   = Math.max(base1 + len1, base2 + len2)
  java.util.Arrays.sort(a.asInstanceOf[Array[AnyRef]], start, end,
    c.asInstanceOf[java.util.Comparator[AnyRef]])
}
```

This defeats the purpose of TimSort entirely — the merge operations are the core of the
algorithm. The fallback to `Arrays.sort` loses TimSort's adaptive properties and
stability guarantees for the merge phase, and introduces unnecessary `asInstanceOf` casts.

### 13.4 [TimSort.scala:330-344] - gallopLeft and gallopRight are placeholder stubs

**Severity**: High — Galloping optimization disabled

Both gallop methods are simple linear searches:

```scala
private def gallopRight[T](key: T, a: Array[T], base: Int, len: Int, hint: Int,
    c: Ordering[T]): Int = {
  var i = 0
  while (i < len && c.compare(a(base + i), key) <= 0) i += 1
  i
}
```

The real galloping algorithm uses exponential search to achieve O(log n) performance
on partially-sorted data. The linear placeholder is O(n), negating TimSort's key
optimization. The `hint` parameter is completely ignored.

### 13.5 [TimSort.scala:50] - minGallop field missing (dead expression)

**Severity**: Medium — Field initialization is a no-op

```scala
class TimSort[T] {
  ...
  TimSort.MIN_GALLOP  // Line 50: standalone expression, does nothing
  ...
}
```

This should declare `private var minGallop: Int = TimSort.MIN_GALLOP` as in the Java
original. The field is never declared, which is consistent with the stub merge methods
but confirms the TimSort implementation is incomplete.

### 13.6 [Sort.scala:41-47] - sort(DynamicArray, Ordering) copies array out and back instead of sorting in-place

**Severity**: Medium — Unnecessary O(n) allocation and copying

```scala
def sort[T](a: DynamicArray[T], c: Ordering[T]): Unit = {
  val array = a.toArray        // Copies entire array
  TimSort.sort(array, c)       // Sorts the copy
  a.clear()                    // Clears original
  a.addAll(array, 0, array.length)  // Copies sorted data back
}
```

The Java original sorts `a.items` directly in-place:
```java
timSort.doSort(a.items, c, 0, a.size);
```

The SGE version creates an unnecessary temporary array, clears the DynamicArray,
then copies everything back. This doubles memory usage during sort and triggers the
snapshot mechanism if a snapshot is active.

### 13.7 [Sort.scala:28] - Singleton ComparableTimSort is not thread-safe

**Severity**: Medium — Concurrent sorts corrupt shared state

```scala
object Sort {
  private val comparableTimSort = ComparableTimSort()
  ...
}
```

`ComparableTimSort` has mutable state (`this.a`, `stackSize`, `runBase`, `runLen`, `tmp`,
`minGallop`). The Scala `object` ensures exactly one instance exists. If two threads call
`Sort.sort()` concurrently, they share this instance and will corrupt each other's state.

The Java original has the same issue (noted in its doc: "Multiple threads must not use
this instance"), but the Scala `object` makes it a guaranteed global singleton.

### 13.8 [Pool.scala:63-66] - `fill` method's peak update executes at trait initialization, not inside fill()

**Severity**: Critical — Pool.fill never updates peak

```scala
def fill(size: Int): Unit =
  for (_ <- 0 until size)
    if (freeObjects.size < max) freeObjects.add(newObject())
peak = peak max freeObjects.size  // ← This is a TRAIT BODY statement, not part of fill()
```

In Scala, `def fill(size: Int): Unit = expr` — the method body is the single expression
after `=`. The `for` loop is that expression. The `peak = ...` line is at the trait body
level, meaning it executes once during trait initialization (when `freeObjects` is empty,
so `peak` stays 0). The `fill` method never updates `peak`.

The Java original correctly has both statements inside the method:
```java
public void fill (int size) {
  for (int i = 0; i < size; i++)
    if (freeObjects.size < max) freeObjects.add(newObject());
  peak = Math.max(peak, freeObjects.size);
}
```

### 13.9 [Pool.scala:45] - obtain() removes from index 0 instead of popping from end

**Severity**: Medium — O(n) instead of O(1) for every pool obtain

```scala
def obtain(): A =
  if (freeObjects.isEmpty) newObject() else freeObjects.removeIndex(0)
```

`removeIndex(0)` removes the first element and shifts all remaining elements left — O(n).
The Java original uses `freeObjects.pop()` which removes the last element — O(1).

```java
return freeObjects.size == 0 ? newObject() : freeObjects.pop();
```

For pools that are heavily used (e.g., particle systems, event objects), this turns
every `obtain()` into a linear-time operation proportional to pool size.

### 13.10 [Timer.scala:337] - removeLifecycleListener executes at TimerThread construction, not in dispose()

**Severity**: Critical — Timer lifecycle management is broken

```scala
private class TimerThread(using sge.Sge) extends Runnable with LifecycleListener {
  ...
  sge.Sge().application.addLifecycleListener(this)  // Line 261: constructor
  resume()
  Future(this.run())(using ExecutionContext.global)

  // ... method definitions ...

  def dispose(): Unit = // OK to call multiple times.
    threadLock.synchronized {
      ...
    }
  sge.Sge().application.removeLifecycleListener(this)  // Line 337: CLASS BODY, not dispose()!
}
```

In Scala, `def dispose(): Unit = expr` makes the `synchronized` block the entire method
body. The `removeLifecycleListener(this)` line is a separate statement at the class body
level — it executes during **construction**, immediately after `addLifecycleListener`.

This means:
- The lifecycle listener is added then immediately removed during construction
- `pause()`, `resume()`, and `dispose()` lifecycle callbacks are never received
- The timer thread never pauses when the app goes to background
- The timer thread is never properly disposed on shutdown

### 13.11 [BinaryHeap.scala:63-68] - contains() identity parameter is broken

**Severity**: Medium — Identity comparison uses structural equality

```scala
def contains(node: T, identity: Boolean): Boolean =
  if (identity) {
    nodes.take(size).exists(_ == node)    // _ == node calls .equals() in Scala!
  } else {
    nodes.take(size).exists(_.equals(node))
  }
```

In Scala, `==` calls `.equals()`, not reference equality. Both branches do the same thing.
When `identity = true`, it should use `eq` for reference comparison:

```scala
if (identity) nodes.take(size).exists(_.asInstanceOf[AnyRef] eq node.asInstanceOf[AnyRef])
```

The Java original correctly uses `==` (which IS reference equality in Java):
```java
if (identity) { for (Node n : nodes) if (n == node) return true; }
```

### 13.12 [ObjectMap.scala:186-191] - clear() doesn't null key/value arrays (memory leak)

**Severity**: High — GC cannot reclaim cleared entries

```scala
def clear(): Unit =
  if (_size == 0) ()
  else {
    _size = 0
    java.util.Arrays.fill(filled, false)
    // keyTable and valueTable are NOT cleared!
  }
```

The Java original clears both:
```java
public void clear () {
  if (size == 0) return;
  size = 0;
  Arrays.fill(keyTable, null);   // ← Releases key references
  Arrays.fill(valueTable, null); // ← Releases value references
}
```

After `clear()`, all key and value objects remain strongly referenced by the backing
arrays, preventing garbage collection. For maps holding large objects or many entries,
this is a significant memory leak.

### 13.13 [ObjectMap.scala:118-139] - remove() doesn't null the vacated slot (memory leak)

**Severity**: High — Removed entries retain references

After backward-shift deletion, the code sets `filled(i) = false` but doesn't clear
`keyTable(i)` or `valueTable(i)`. The Java original nulls both:

```java
keyTable[i] = null;
valueTable[i] = null;
```

This means removed keys and values remain referenced until the slot is reused or the
map is resized, preventing timely garbage collection.

### 13.14 [ObjectSet.scala:160-165, 113-133] - clear() and remove() don't null key array (memory leak)

**Severity**: High — Same issue as ObjectMap

`ObjectSet.clear()` only fills the `filled` array with false but doesn't null `keyTable`.
`ObjectSet.remove()` sets `filled(i) = false` but doesn't null `keyTable(i)`.

Both retain strong references to removed/cleared keys.

### 13.15 [DynamicArray.scala] - Systematic memory leaks in all removal/clear operations

**Severity**: High — All DynamicArray operations for reference types leak memory

The following methods don't null vacated slots in the backing array (the Java `Array.java`
original nulls them in all cases):

| Method | Line | Java behavior |
|--------|------|---------------|
| `removeIndex()` | 158-169 | Java: `items[size] = null` after shift/swap |
| `pop()` | 247-252 | Java: `items[size] = null` after decrement |
| `clear()` | 255-258 | Java: `Arrays.fill(items, 0, size, null)` |
| `truncate()` | 261-265 | Java: nulls elements from newSize to old size |
| `removeRange()` | 198-212 | Java: nulls vacated trailing slots |

For `DynamicArray[String]`, `DynamicArray[Actor]`, etc., all removed/cleared elements
remain strongly referenced by the backing array. Since DynamicArray is the most widely
used collection in the codebase, this affects nearly every subsystem.

**Note**: For primitive DynamicArrays (`DynamicArray[Int]`, `DynamicArray[Float]`, etc.),
null-clearing is not applicable or necessary. The `MkArray` type class would need a
`clear(array, from, to)` method that no-ops for primitives and nulls for reference types.

### 13.16 [ArrayMap.scala:108-118, 169-170] - removeIndex() and clear() don't null arrays (memory leak)

**Severity**: High — Same pattern as DynamicArray

`ArrayMap.removeIndex()` shifts or swaps elements but doesn't null the vacated last slot.
`ArrayMap.clear()` just sets `_size = 0`, never nulling any array elements.

### 13.17 [ObjectMap.scala:292-304] - hashCode() doesn't handle null values

**Severity**: Low — Latent NPE if null values exist

```scala
override def hashCode(): Int = {
  var h = _size
  ...
  if (filled(i)) {
    h += keyTable(i).hashCode()
    h += valueTable(i).hashCode()  // NPE if value is null
  }
}
```

The Java original checks: `if (value != null) h += value.hashCode()`. The SGE version
unconditionally calls `hashCode()` on the value. While the project convention disallows
null values, this could NPE if a null slips through (e.g., via `asInstanceOf`).

### 13.18 [ObjectMap.scala:306-326] - equals() logic differs from Java for null values

**Severity**: Low — Latent incorrect equality for null-valued entries

The Java `equals` uses a `dummy` sentinel to distinguish "key not found" from "key found
with null value":
```java
if (value == null) {
  if (other.get(key, dummy) != null) return false;
} else {
  if (!value.equals(other.get(key))) return false;
}
```

The SGE version uses `Nullable` semantics:
```scala
val otherVal = otherMap.get(keyTable(i))
if (otherVal.isEmpty || otherVal.getOrElse(value) != value) equal = false
```

If both maps have the same key with a null value, `Nullable(null)` is empty, so
`otherVal.isEmpty` returns true and the maps are incorrectly considered unequal.

### 13.19 [OrderedMap.scala:88-101] - alter() could leave map in inconsistent state if put fails

**Severity**: Low — Defensive concern

In `alter`:
```scala
val value = map.remove(before)
value.foreach { v => map.put(after, v) }
_keys.update(idx, after)
```

If `map.put(after, v)` triggers a resize that throws (e.g., out of memory), the key
array has been updated but the map is missing the entry. A `try`/`catch` with rollback
would make this safer, matching the Java original's simpler implementation.

### 13.20 [OrderedSet.scala:105-112] - alter() searches for `before` in _items AFTER removing it from set

**Severity**: Low — Correct but fragile

```scala
def alter(before: A, after: A): Boolean =
  if (set.contains(after)) false
  else if (!set.remove(before)) false
  else {
    set.add(after)
    _items.update(_items.indexOf(before), after)  // indexOf uses ==
    true
  }
```

`_items.indexOf(before)` still finds `before` because `_items` hasn't been modified.
But if `before`'s `equals` depends on mutable state that changes between `set.remove`
and `indexOf`, this could return -1, passing it to `update` which would throw.

### 13.21 [BinaryHeap.scala:93-108] - remove() doesn't handle edge case where node.index == size

**Severity**: Low — Potential index-out-of-bounds

If `remove` is called on the last node (where `node.index == size - 1`), after
`size -= 1`, the code does `nodes(size) = null` and `nodes(node.index) = moved` where
`node.index` equals the old size - 1, which equals the new `size`. So `moved = nodes(size)`
reads the same slot that was just nulled if the node IS the last element. This means
`moved` would be null, and `moved.index = node.index` would NPE.

Actually, the Java original handles this the same way — when `node == nodes[size]`,
the moved node is the node being removed, which is a valid (but degenerate) case.
However, the Java code checks `if (node.index != size)` before doing the swap.
The SGE port doesn't have this guard:

```java
// Java original has a guard:
if (node.index != size) {
  Node moved = nodes[size];
  nodes[node.index] = moved;
  ...
}
```

Wait — checking the Java source more carefully, the Java `remove` doesn't have that
guard either. It just does `nodes[size]` and uses the result. But since `node.index`
can equal `size` (the new size after decrement), `nodes(node.index) = moved` would
write `moved` (which is the node at position `size`, potentially the same node) back
to `node.index`. This works because if node IS the last node, `moved = node` and the
swap is a no-op. The null written to `nodes(size)` correctly clears the slot. So this
is actually correct in both versions.

### 13.22 [DynamicArray.scala:496-516] - Snapshot begin()/end() has no nesting guard

**Severity**: Medium — Multiple `begin()` calls corrupt snapshot state

The snapshot mechanism allows nested `begin()` calls (via `snapshots` counter), but
`end()` logic has issues:

```scala
def end(): Unit = {
  snapshots -= 1
  if (Nullable(snapshot).isEmpty) {
    // No snapshot was active — nothing to do
  } else if (snapshot ne _items) {
    if (Nullable(recycled).isEmpty || recycled.length < snapshot.length)
      recycled = snapshot
    snapshot = null         // Nulls snapshot even if snapshots > 0!
  } else if (snapshots == 0) {
    snapshot = null
  }
}
```

When `snapshot ne _items` (items were copied), `snapshot` is set to null regardless
of the `snapshots` counter. If a second `begin()` was called before the first `end()`,
the second `end()` will find `snapshot == null` and skip cleanup. The `recycled` array
management could also be incorrect with nested snapshots.

### 13.23 [Timer.scala:269-270] - Missing exception handling in timer update loop

**Severity**: Medium — One failing timer task kills all timers

The Java original wraps each timer update in try/catch:
```java
for (int i = 0, n = instances.size; i < n; i++) {
  try {
    waitMillis = instances.get(i).update(this, timeMillis, waitMillis);
  } catch (Throwable ex) {
    throw new GdxRuntimeException("Task failed: " + ..., ex);
  }
}
```

The SGE version has no exception handling:
```scala
var i = 0
while (i < instances.size) {
  waitMillis = instances(i).update(this, timeMillis, waitMillis)
  i += 1
}
```

If any timer's `update` throws, the entire timer thread exits its loop via `dispose()`,
killing all timers in the application.

### 13.24 [Timer.scala:46-63] - Nested synchronized blocks risk deadlock

**Severity**: Medium — Lock ordering: threadLock → this → task

```scala
def scheduleTask(...): Task = {
  threadLock.synchronized {        // Lock 1
    this.synchronized {             // Lock 2
      task.synchronized {           // Lock 3
        ...
      }
    }
  }
  task
}
```

While `cancel()` acquires locks in a different order (threadLock → timer → task),
the timer's `update` method acquires `this` (timer) first, then `task`. If a task calls
`cancel()` from its `run()` method (which runs on the main thread), it acquires
`threadLock` → `timer`. Meanwhile the timer thread holds `threadLock` in its loop.
The Java original has the same complex locking, but the SGE version additionally wraps
things in `this.synchronized` inside `threadLock.synchronized` in `scheduleTask`,
adding another potential deadlock path.

### 13.25 [Sort.scala:31] - comparableTimSort instance sort called on DynamicArray but never sorted to the DynamicArray

**Severity**: Critical (duplicate of 13.1 for emphasis — affects all Comparable sorting)

The `Sort` object has two overloads for DynamicArray:
- `sort[T <: Comparable[T]](a: DynamicArray[T])` — Sorts a copy, never writes back (**broken**)
- `sort[T](a: DynamicArray[T], c: Ordering[T])` — Copies out, sorts, copies back (**works but wasteful**)

Any user code that relies on `Comparable`-based sorting of DynamicArrays gets silently
broken sorting.

---

## 17. GL/Rendering Pipeline Issues

### 17.1 [GLFrameBuffer.scala] - glDeleteBuffer used instead of glDeleteRenderbuffer on error path (CRITICAL)

**File**: `graphics/glutils/GLFrameBuffer.scala:336`
**Severity**: CRITICAL — GL resource leak and potential GL error

When framebuffer completeness check fails and `hasDepthStencilPackedBuffer` is true,
the error cleanup path calls `glDeleteBuffer` instead of `glDeleteRenderbuffer`:

```scala
if (hasDepthStencilPackedBuffer) {
    gl.glDeleteBuffer(depthStencilPackedBufferHandle)  // BUG: wrong GL call!
} else {
    if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
    if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
}
```

The Java original correctly uses `glDeleteRenderbuffer`:

```java
if (hasDepthStencilPackedBuffer) {
    gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle);
}
```

`depthStencilPackedBufferHandle` was created by `glGenRenderbuffer()` (line 198), so it
must be deleted with `glDeleteRenderbuffer()`. Using `glDeleteBuffer()` operates on a
different GL object namespace — the renderbuffer is leaked, and a random VBO/IBO with
the same handle number could be accidentally deleted.

**Impact**: On framebuffer creation failure with packed depth-stencil, the renderbuffer
leaks and a potentially unrelated buffer object could be destroyed, causing rendering
corruption or crashes.

**Fix**: Change `gl.glDeleteBuffer(depthStencilPackedBufferHandle)` to
`gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)`.

---

### 17.2 [GLFrameBuffer.scala] - close() does not delete colorBufferHandles (HIGH - GL resource leak)

**File**: `graphics/glutils/GLFrameBuffer.scala:361-376`
**Severity**: HIGH — renderbuffer leak on every FBO disposal

The `close()` method deletes texture attachments, depth/stencil renderbuffers, and the
framebuffer object, but does NOT delete the color renderbuffer handles:

```scala
override def close(): Unit = {
    val gl = Sge().graphics.gl20
    textureAttachments.foreach(disposeColorTexture(_))
    gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
    gl.glDeleteRenderbuffer(depthbufferHandle)
    gl.glDeleteRenderbuffer(stencilbufferHandle)
    gl.glDeleteFramebuffer(framebufferHandle)
    // Missing: colorBufferHandles are never deleted!
    val managedResources = GLFrameBuffer.buffers.get(Sge().application)
    if (managedResources.isDefined) managedResources.get.removeValueByRef(this)
}
```

The Java original iterates and deletes each color buffer handle:

```java
for (int colorBufferHandle : colorBufferHandles) {
    gl.glDeleteRenderbuffer(colorBufferHandle);
}
```

Each handle was allocated with `gl.glGenRenderbuffer()` during `build()` (line 244).

**Impact**: Every frame buffer using color renderbuffer attachments (common in
multisample FBOs) leaks renderbuffer objects. Over time, this exhausts GPU memory.

**Fix**: Add `colorBufferHandles.foreach(gl.glDeleteRenderbuffer(_))` before the
framebuffer deletion in `close()`.

---

### 17.3 [ShaderProgram.scala] - Orphaned FloatBuffer allocation (LOW - memory waste)

**File**: `graphics/glutils/ShaderProgram.scala:97-98`
**Severity**: LOW — direct buffer allocation with result discarded

```scala
/** matrix float buffer * */
BufferUtils.newFloatBuffer(16)
```

This allocates a 64-byte direct FloatBuffer and immediately discards the result.
In the Java original, this was assigned to the `matrix` field:

```java
/** matrix float buffer **/
private final FloatBuffer matrix;
...
matrix = BufferUtils.newFloatBuffer(16);
```

In newer LibGDX, the matrix buffer was replaced by array-based uniform uploads
(`gl.glUniformMatrix4fv(location, 1, transpose, matrix.values, 0)`), making the
buffer obsolete. The SGE port correctly uses array-based uploads but kept the
buffer allocation as a dead statement.

**Impact**: Every ShaderProgram construction leaks a 64-byte direct buffer. Since
direct buffers are off-heap and not managed by GC, they accumulate until the
ShaderProgram is garbage collected and its finalizer runs (if any).

**Fix**: Remove the orphaned `BufferUtils.newFloatBuffer(16)` statement.

---

### 17.4 [ShaderProgram.scala] - checkManaged does not re-fetch uniforms/attributes after recompilation (HIGH)

**File**: `graphics/glutils/ShaderProgram.scala:749-753`
**Severity**: HIGH — all uniform/attribute locations stale after context loss

When a shader is invalidated (e.g., after GL context loss), `checkManaged()` recompiles
the shaders but does NOT re-query the uniform and attribute locations:

```scala
private def checkManaged(): Unit =
    if (invalidated) {
      compileShaders(vertexShaderSource, fragmentShaderSource)
      invalidated = false
    }
```

After recompilation, `program` gets a new GL handle, but the `uniforms`, `attributes`,
`uniformTypes`, `uniformSizes`, `attributeTypes`, `attributeSizes`, `uniformNames`, and
`attributeNames` maps all retain stale data from the OLD program handle.

The `fetchUniformLocation` method checks the cache first:

```scala
def fetchUniformLocation(name: String, pedantic: Boolean): Int =
    uniforms.get(name) match {
      case Some(location) => location  // Returns STALE location!
      case None => ...
    }
```

Since `fetchUniforms()` already populated all uniform names during construction, every
subsequent lookup returns the cached (stale) location without re-querying GL.

**Note**: The Java original has the same behavior — this is a pre-existing LibGDX issue
faithfully ported. However, it means that after GL context loss on Android, all shader
uniform/attribute operations silently use wrong locations, causing rendering to fail
or produce garbage.

**Impact**: After GL context loss and shader recompilation, every `setUniformf`,
`setUniformMatrix`, `setVertexAttribute`, etc. operates on stale locations. Rendering
is broken until the application is fully restarted.

**Fix**: Clear all maps and re-call `fetchAttributes()` / `fetchUniforms()` after
successful recompilation in `checkManaged()`:

```scala
private def checkManaged(): Unit =
    if (invalidated) {
      compileShaders(vertexShaderSource, fragmentShaderSource)
      if (compiledSuccessfully) {
        uniforms.clear()
        attributes.clear()
        uniformTypes.clear()
        uniformSizes.clear()
        attributeTypes.clear()
        attributeSizes.clear()
        fetchAttributes()
        fetchUniforms()
      }
      invalidated = false
    }
```

---

### 17.5 [VertexBufferObjectSubData.scala] - bind() uses glBufferData instead of glBufferSubData (MEDIUM)

**File**: `graphics/glutils/VertexBufferObjectSubData.scala:137-140`
**Severity**: MEDIUM — defeats the SubData optimization

The `bind()` method uses `glBufferData` (which reallocates the entire buffer) instead
of `glBufferSubData` (which only updates a portion):

```scala
override def bind(shader: ShaderProgram, locations: Nullable[Array[Int]]): Unit = {
    val gl = Sge().graphics.gl20
    gl.glBindBuffer(BufferTarget.ArrayBuffer, bufferHandle)
    if (isDirty) {
      byteBuffer.asInstanceOf[Buffer].limit(buffer.limit() * 4)
      gl.glBufferData(BufferTarget.ArrayBuffer, byteBuffer.limit(), byteBuffer, usage)
      // ^^^ Should be glBufferSubData!
      isDirty = false
    }
```

The Java original correctly uses `glBufferSubData`:

```java
if (isDirty) {
    ((Buffer)byteBuffer).limit(buffer.limit() * 4);
    gl.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
    isDirty = false;
}
```

The `SubData` variant pre-allocates the GPU buffer during construction via
`createBufferObject()` (which calls `glBufferData` with null data). Updates should use
`glBufferSubData` to avoid reallocation. The SGE port accidentally uses `glBufferData`,
which re-allocates the buffer on every dirty bind.

Note: `bufferChanged()` (line 88-91) correctly uses `glBufferSubData`, making the
inconsistency more obvious.

**Impact**: `VertexBufferObjectSubData` loses its performance advantage over regular
`VertexBufferObject`. The GPU driver re-allocates the buffer storage on every bind,
causing pipeline stalls and defeating the SubData optimization. This matters for
frequently-updated dynamic vertex data.

**Fix**: Change `gl.glBufferData(...)` to `gl.glBufferSubData(BufferTarget.ArrayBuffer, 0, byteBuffer.limit(), byteBuffer)`.

---

### 17.6 [Mesh.scala] - copy() off-by-one: vertex index 0 can never be deduplicated (LOW)

**File**: `graphics/Mesh.scala:1156`
**Severity**: LOW — faithfully ported LibGDX bug

In the `copy` method's duplicate removal logic:

```scala
if (newIndex > 0) {        // BUG: should be >= 0
    indices(i) = newIndex
} else {
    val idx = size * newVertexSize
    for (j <- checks.indices)
      tmp(idx + j) = verts(idx1 + checks(j))
    indices(i) = size.toShort
    size += 1
}
```

`newIndex` is initialized to `-1` and set to `j` when a duplicate is found. If the
duplicate matches vertex 0 (`j = 0`), then `newIndex = 0`, and `newIndex > 0` is false.
The code falls through to the else branch, re-adding the vertex instead of reusing
index 0. The Java original has the same bug (`if (newIndex > 0)`).

**Impact**: The first vertex can never be deduplicated. Meshes copied with
`removeDuplicates=true` may have one extra vertex. This is a minor inefficiency, not
a correctness issue for rendering.

---

### 17.7 [GLTexture.scala] - unsafeSetAnisotropicFilter hardcodes TextureTarget.Texture2D (LOW)

**File**: `graphics/GLTexture.scala:186`
**Severity**: LOW — affects cubemaps and 3D textures

Both `unsafeSetAnisotropicFilter` and `setAnisotropicFilter` hardcode
`TextureTarget.Texture2D` instead of using `this.glTarget`:

```scala
Sge().graphics.gl20.glTexParameterf(TextureTarget.Texture2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, adjustedLevel)
```

For a `Cubemap` (which extends `GLTexture` with `TextureTarget.TextureCubeMap`), this
sets the anisotropic filter on the wrong target. The GL call would either fail silently
or affect whatever 2D texture happens to be currently bound.

The Java original has the same hardcoded `GL20.GL_TEXTURE_2D`.

**Impact**: Anisotropic filtering has no effect on cubemaps and other non-2D textures.
Low impact since anisotropic filtering on cubemaps is uncommon.

**Fix**: Replace `TextureTarget.Texture2D` with `glTarget` in both methods.

---

### 17.8 [VertexBufferObjectWithVAO.scala] - Member fields are public instead of private (MEDIUM - encapsulation)

**File**: `graphics/glutils/VertexBufferObjectWithVAO.scala:41-51`
**Severity**: MEDIUM — breaks encapsulation, allows external corruption

```scala
var attributes:   VertexAttributes = scala.compiletime.uninitialized
var buffer:       FloatBuffer      = scala.compiletime.uninitialized
var byteBuffer:   ByteBuffer       = scala.compiletime.uninitialized
var ownsBuffer:   Boolean          = scala.compiletime.uninitialized
var bufferHandle: Int              = scala.compiletime.uninitialized
var isStatic:     Boolean          = scala.compiletime.uninitialized
var usage:        BufferUsage      = scala.compiletime.uninitialized
var isDirty         = false
var isBound         = false
var vaoHandle       = -1
var cachedLocations = DynamicArray[Int]()
```

All internal fields are public `var`s. In the Java original, these are `private` or
package-private. External code could accidentally modify `bufferHandle`, `vaoHandle`,
`isDirty`, etc., causing GL state corruption that would be extremely hard to debug.

By contrast, `VertexBufferObject` correctly uses `private var` for its fields.

**Impact**: No immediate correctness issue, but allows accidental corruption of GL
state. Any code that reads/writes these fields (e.g., in tests or downstream code)
creates a hidden coupling to internal VBO state.

---

### 17.9 [GLFrameBuffer.scala] - Error path also missing colorBufferHandles cleanup (MEDIUM)

**File**: `graphics/glutils/GLFrameBuffer.scala:330-354`
**Severity**: MEDIUM — GL resource leak on FBO creation failure

When framebuffer completeness check fails, the error cleanup deletes textures,
depth/stencil buffers, and the framebuffer, but does NOT delete color renderbuffer
handles:

```scala
if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
    textureAttachments.foreach(disposeColorTexture(_))
    if (hasDepthStencilPackedBuffer) {
      gl.glDeleteBuffer(depthStencilPackedBufferHandle)  // Also bug 17.1
    } else {
      if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
      if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
    }
    gl.glDeleteFramebuffer(framebufferHandle)
    // Missing: colorBufferHandles not deleted!
    throw ...
}
```

**Impact**: If FBO creation fails on a device that doesn't support the requested
format, all color renderbuffers created before the failure are leaked.

---

### 17.10 [ShaderProgram.scala] - loadShader leaks GL shader object on compilation failure (LOW)

**File**: `graphics/glutils/ShaderProgram.scala:145-165`
**Severity**: LOW — same as Java original

When shader compilation fails, the allocated GL shader object is not deleted:

```scala
private def loadShader(shaderType: ShaderType, source: String): Int = scala.util.boundary {
    val shader = gl.glCreateShader(shaderType)
    if (shader == 0) scala.util.boundary.break(-1)
    gl.glShaderSource(shader, source)
    gl.glCompileShader(shader)
    gl.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intbuf)
    val compiled = intbuf.get(0)
    if (compiled == 0) {
      val infoLog = gl.glGetShaderInfoLog(shader)
      log += ...
      scala.util.boundary.break(-1)  // Shader object leaked!
    }
    shader
}
```

After `glCreateShader` succeeds but `glCompileShader` fails, the method returns -1
without calling `glDeleteShader(shader)`. The Java original has the same omission.

**Impact**: Each failed shader compilation leaks one GL shader object. Minor in
practice since compilation failures are rare after development.

---

### 17.11 [Mesh.scala] - calculateRadiusSquared only works with indexed meshes (LOW)

**File**: `graphics/Mesh.scala:923-926`
**Severity**: LOW — same as Java original

```scala
def calculateRadiusSquared(...): Float = {
    val numIndicesVal = getNumIndices()
    if (offset < 0 || count < 1 || offset + count > numIndicesVal) {
      throw SgeError.GraphicsError("Not enough indices")
    }
```

The method always indexes through the index buffer (`index.get(i)`), with no fallback
for non-indexed meshes. Calling `calculateRadiusSquared` on a mesh with no indices
throws immediately since `numIndicesVal = 0` and `count >= 1`.

The `extendBoundingBox` method (line 837) correctly handles both indexed and non-indexed
meshes, but `calculateRadiusSquared` does not. Same limitation in Java.

**Impact**: Cannot compute bounding spheres for non-indexed meshes.

---

### 17.12 [Texture.scala] - textureData is val but load() doesn't update it (MEDIUM - stale field after external reload)

**File**: `graphics/Texture.scala:43,107`
**Severity**: MEDIUM — inconsistency with Java original

```scala
class Texture(..., data: TextureData)(using Sge) extends GLTexture(...) {
  val textureData: TextureData = data   // Immutable!
  ...
  def load(data: TextureData): Unit = {
    // Java original: this.data = data; (updates the field)
    // SGE: textureData is val, cannot be updated
    ...
  }
}
```

In Java, `Texture.load()` updates `this.data = data`, allowing the texture data to be
replaced. In SGE, `textureData` is a `val` and is never updated by `load()`. This means:

- If `load()` is called with different data, `textureData` still points to the old data
- `getWidth` / `getHeight` return the old data's dimensions
- `reload()` reloads the OLD data, not the newly loaded data
- `toString()` shows the old data's info

In practice, `load()` is only called from the constructor and `reload()` (both with the
same data), so this hasn't manifested. But the API contract differs from Java.

**Impact**: If any code calls `texture.load(newData)` directly, the texture's metadata
becomes inconsistent with the actual GPU data.

---

### 17.13 [ShapeRenderer.scala] - arc() emits extra orphaned vertex at end (LOW)

**File**: `graphics/glutils/ShapeRenderer.scala:876-882`
**Severity**: LOW — extra vertex at origin, may cause visual artifact

After the main arc drawing loop (both Line and Filled modes), the method unconditionally
emits an extra vertex at the origin:

```scala
    cx = 0
    cy = 0
    renderer.color(colorBits)
    renderer.vertex(x + cx, y + cy, 0)  // Always emits vertex at (x, y, 0)
```

This vertex is emitted after the arc's closing vertex and before any subsequent draw
call. For `ShapeType.Line`, this creates a stray line segment from the last arc point
to the center. For `ShapeType.Filled`, it creates a degenerate triangle.

The Java original has the same code. This appears to be intentional (closing the arc
back to center for filled mode) but the unconditional execution for Line mode is
questionable.

---

### 17.14 Summary of GL/Rendering Pipeline Issues

| # | File | Issue | Severity |
|---|------|-------|----------|
| 17.1 | GLFrameBuffer.scala | `glDeleteBuffer` instead of `glDeleteRenderbuffer` on error path | **Critical** |
| 17.2 | GLFrameBuffer.scala | `close()` doesn't delete `colorBufferHandles` | **High** |
| 17.3 | ShaderProgram.scala | Orphaned `FloatBuffer(16)` allocation | Low |
| 17.4 | ShaderProgram.scala | `checkManaged` doesn't re-fetch uniforms/attributes after recompilation | **High** |
| 17.5 | VertexBufferObjectSubData.scala | `bind()` uses `glBufferData` instead of `glBufferSubData` | **Medium** |
| 17.6 | Mesh.scala | `copy()` off-by-one: vertex 0 can't be deduplicated | Low |
| 17.7 | GLTexture.scala | Anisotropic filter hardcodes `Texture2D` target | Low |
| 17.8 | VertexBufferObjectWithVAO.scala | All fields public instead of private | Medium |
| 17.9 | GLFrameBuffer.scala | Error path missing `colorBufferHandles` cleanup | Medium |
| 17.10 | ShaderProgram.scala | `loadShader` leaks GL shader on failure | Low |
| 17.11 | Mesh.scala | `calculateRadiusSquared` only works with indexed meshes | Low |
| 17.12 | Texture.scala | `textureData` is `val` but Java's `load()` updates it | Medium |
| 17.13 | ShapeRenderer.scala | `arc()` emits extra orphaned vertex at origin | Low |

Two issues are **SGE-specific porting bugs** (17.1, 17.5), two are **SGE-specific
omissions** (17.2, 17.9), one is **both Java and SGE but fixable** (17.4), and the
rest are faithfully ported Java limitations or minor style issues.

Issues 17.1 and 17.2 should be fixed immediately — they cause GL resource corruption
and leaks. Issue 17.4 should be fixed to properly support Android context loss. Issue
17.5 should be fixed to restore the intended performance optimization.

---

## 18. Maps/Tiled System Issues

### 18.1 [BaseTmxMapLoader.scala] — mergeProperties: missing break in duplicate property search loop

**File**: `maps/tiled/BaseTmxMapLoader.scala:579-587`
**Severity**: Medium — behavioral difference from Java; could cause wrong property to be removed

In the Java `mergeProperties`, when searching for a duplicate property by name in the
merged element, the loop `break`s on the first match:

```java
Element existing = null;
for (int i = 0; i < merged.getChildCount(); i++) {
    Element child = merged.getChild(i);
    if ("property".equals(child.getName()) && name.equals(child.getAttribute("name", null))) {
        existing = child;
        break;  // EXIT INNER LOOP on first match
    }
}
```

In the Scala port, the loop always scans ALL children:

```scala
var existing: Nullable[XmlReader.Element] = Nullable.empty
var i = 0
while (i < merged.childCount) {
  val child = merged.getChild(i)
  if ("property" == child.name && name.isDefined &&
      name.getOrElse("") == child.getAttribute("name", Nullable.empty).getOrElse("")) {
    existing = Nullable(child)
    // BUG: no break — continues scanning
  }
  i += 1
}
```

If there are duplicate properties with the same name (from a malformed template), the
Java version removes the first match while the Scala version removes the last. Additionally,
the Scala version always does O(n) work per property instead of O(1) for early matches.

**Impact**: Normally properties have unique names so behavior matches. With duplicates,
different property gets removed. Performance impact on large property sets.

---

### 18.2 [TideMapLoader.scala] — loadTileSheet: spacing variable is a dead expression; spacingParts reads from margin

**File**: `maps/tiled/TideMapLoader.scala:163,177`
**Severity**: Medium — tiles spaced incorrectly when margin ≠ spacing (faithfully ported Java bug)

The spacing attribute is read from the XML but its result is discarded (dead expression):

```scala
alignment.getAttribute("Spacing")  // line 163: result discarded!
```

Later, `spacingParts` is parsed from `margin` instead of `spacing`:

```scala
val spacingParts = margin.split(" x ")  // line 177: should be spacing, not margin
val spacingX = Integer.parseInt(spacingParts(0))
val spacingY = Integer.parseInt(spacingParts(1))
```

This means spacing always equals margin, regardless of the actual spacing specified in
the `.tide` file. This is a faithfully ported bug from the LibGDX Java original (where
the Java code has the same `String[] spacingParts = margin.split(" x ");`).

**Impact**: Tile sheets where spacing differs from margin will have tiles cut from
incorrect positions, causing visual artifacts. Since `.tide` format is rare, real-world
impact is low.

---

### 18.3 [TideMapLoader.scala] — loadTileSheet: sheetSizeX/Y parsed but result discarded

**File**: `maps/tiled/TideMapLoader.scala:166-167`
**Severity**: Low — dead expressions, no functional impact

```scala
val sheetSizeParts = sheetSize.split(" x ")
Integer.parseInt(sheetSizeParts(0))  // dead expression — result discarded
Integer.parseInt(sheetSizeParts(1))  // dead expression — result discarded
```

In Java, these were assigned to `int sheetSizeX` and `int sheetSizeY` which were also
unused variables. The parsing still validates the format but the values are never used.

---

### 18.4 [TideMapLoader.scala] — loadTileSheet: missing texture silently skips tile creation

**File**: `maps/tiled/TideMapLoader.scala:194-211`
**Severity**: Medium — silent failure instead of error

In Java, `imageResolver.getImage(...)` returns `TextureRegion` (non-null), and the tile
creation loop runs unconditionally. In Scala, `imageResolver.getImage(...)` returns
`Nullable[TextureRegion]`, and the entire tile creation loop is wrapped in
`texture.foreach`:

```scala
texture.foreach { tex =>
  val stopWidth = tex.regionWidth - tileSizeX
  val stopHeight = tex.regionHeight - tileSizeY
  var y = marginY
  while (y <= stopHeight) {
    // ... create tiles ...
  }
}
```

If the image resolver cannot find the texture, the loop is silently skipped and the
tileset ends up with zero tiles. In Java, this would throw an NPE, immediately surfacing
the problem.

**Impact**: A missing texture file for a `.tide` tileset silently produces an empty
tileset instead of failing with a clear error. The map loads but tiles are invisible.
Debugging requires knowing that the texture wasn't found.

---

### 18.5 [TideMapLoader.scala] — loadLayer: lenient default values mask malformed map files

**File**: `maps/tiled/TideMapLoader.scala:261,264,269`
**Severity**: Low — more lenient than Java

Several `getIntAttribute` and `getInt` calls add default values that the Java original
doesn't have:

| SGE | Java | Context |
|-----|------|---------|
| `getIntAttribute("Count", 0)` | `getIntAttribute("Count")` | Null tile count |
| `getIntAttribute("Index", 0)` | `getIntAttribute("Index")` | Tile index in tileset |
| `getInt("Interval", 0)` | `getInt("Interval")` | Animation interval |

In Java, missing required attributes throw `NumberFormatException` or similar. In Scala,
they silently default to 0. A tile index of 0 might load the wrong tile; an interval of 0
creates a zero-duration animation frame.

**Impact**: Malformed `.tide` files that should fail validation load silently with
incorrect data. Low real-world impact since `.tide` files are rare and typically
well-formed.

---

### 18.6 [BaseTmxMapLoader.scala / BaseTmjMapLoader.scala] — loadImageLayer: y-offset not adjusted when texture resolution fails

**File**: `maps/tiled/BaseTmxMapLoader.scala:308-313`, `maps/tiled/BaseTmjMapLoader.scala:271-274`
**Severity**: Medium — image layer positioned incorrectly on texture failure

In Java, when an image element exists, the texture is resolved and `y -= texture.getRegionHeight()`
is called unconditionally:

```java
if (image != null) {
    texture = imageResolver.getImage(handle.path());
    y -= texture.getRegionHeight();  // always executed
}
```

In Scala, the y-adjustment is inside a `Nullable.foreach`:

```scala
texture = imageResolver.getImage(handle.path())
texture.foreach(t => y -= t.regionHeight)  // only if texture resolved
```

If the image resolver returns `Nullable.empty` (texture not found), the y-coordinate
is not adjusted. The `TiledMapImageLayer` is then created with the wrong y-position.
In Java, this scenario would NPE.

**Impact**: If an image layer's texture fails to load, the layer is positioned at the
wrong y-coordinate instead of crashing. This makes the image appear offset by its own
height, and the error is silent.

---

### 18.7 [BaseTmxMapLoader.scala] — loadBasicLayerInfo: layer name defaults to "" instead of null

**File**: `maps/tiled/BaseTmxMapLoader.scala:328`
**Severity**: Low — behavioral difference

```scala
// SGE
val name = element.getAttribute("name", Nullable.empty).getOrElse("")

// Java
String name = element.getAttribute("name", null);
```

Java sets the layer name to `null` when the "name" attribute is absent. Scala sets it
to `""`. Code that checks for null layer names (e.g., `layer.name == null`) would behave
differently. Since SGE's `MapLayer.name` is initialized to `""`, this is internally
consistent, but any LibGDX porting guides that mention null names would be inaccurate.

---

### 18.8 [BaseTmjMapLoader.scala] — loadTiledMap: map properties stored as Scala Int instead of java.lang.Integer

**File**: `maps/tiled/BaseTmjMapLoader.scala:103-108`
**Severity**: Info — no actual bug due to auto-boxing, but inconsistent with TMX loader

The TMJ loader stores map dimensions as bare Scala `Int`:

```scala
mapProperties.put("width", mapWidth)       // Scala Int
mapProperties.put("height", mapHeight)     // Scala Int
mapProperties.put("tilewidth", tileWidth)  // Scala Int
```

While the TMX loader explicitly ascribes `java.lang.Integer`:

```scala
mapProperties.put("width", mapWidth: java.lang.Integer)
```

Both work correctly because Scala auto-boxes `Int` to `java.lang.Integer` when storing
as `Any`, and `getAs[Integer]` succeeds in both cases. However, the inconsistency could
confuse maintainers.

---

### 18.9 [HexagonalTiledMapRenderer.scala] — renderCell skips AnimatedTiledMapTile (faithfully ported Java limitation)

**File**: `maps/tiled/renderers/HexagonalTiledMapRenderer.scala:176`
**Severity**: Medium — animated tiles never render in hex maps

```scala
if (!t.isInstanceOf[AnimatedTiledMapTile]) {
  // ... render tile ...
}
```

This matches the Java original exactly:

```java
if (tile instanceof AnimatedTiledMapTile) return;
```

Animated tiles in hexagonal maps are silently skipped and never rendered. This is a
known LibGDX limitation, not a porting bug, but it means hex maps with animated tiles
will have invisible tiles.

---

### 18.10 [OrthoCachedTiledMapRenderer.scala] — does not handle MapGroupLayer

**File**: `maps/tiled/renderers/OrthoCachedTiledMapRenderer.scala:116-126`
**Severity**: Medium — group layers silently produce empty caches

The cache-building loop only handles `TiledMapTileLayer` and `TiledMapImageLayer`:

```scala
map.layers.foreach { layer =>
  spriteCache.beginCache()
  layer match {
    case tileLayer: TiledMapTileLayer   => renderTileLayer(tileLayer)
    case imageLayer: TiledMapImageLayer => renderImageLayer(imageLayer)
    case _                              => ()  // group layers silently skipped
  }
  spriteCache.endCache()
}
```

`MapGroupLayer` instances get empty caches. Their child layers are never rendered.
This matches the Java original (which also doesn't handle group layers), but it means
`OrthoCachedTiledMapRenderer` cannot render maps that use Tiled's layer group feature.

---

### 18.11 [BaseTmxMapLoader.scala] — resolveTemplateObject: var parsed uses raw null

**File**: `maps/tiled/BaseTmxMapLoader.scala:520`
**Severity**: Low — style violation, functional correctness OK

```scala
var parsed: XmlReader.Element = null // scalastyle:ignore
try
  parsed = xml.parse(templateFile)
```

Uses raw `null` with scalastyle ignore comment. The null is only live between the `var`
declaration and the `try` body assignment. If the parse throws, the `catch` block re-throws.
No functional issue but violates the project's no-null convention.

---

### 18.12 [BaseTmxMapLoader.scala / BaseTmjMapLoader.scala] — loadObject: tile GID processing uses Option/Nullable differently

**File**: `maps/tiled/BaseTmxMapLoader.scala:445-465`, `maps/tiled/BaseTmjMapLoader.scala:385-405`
**Severity**: Info — no bug but divergent patterns

In the TMX loader, GID handling is:

```scala
val gid = element.getAttribute("gid", Nullable.empty)
gid.foreach { g =>
  val id = java.lang.Long.parseLong(g).toInt
```

In the TMJ loader:

```scala
element.gid.foreach { g =>
  val id = g.toInt  // g is Long from JSON
```

The TMX version parses a String GID via `Long.parseLong`, while TMJ gets a typed `Long`
from JSON. Both correctly handle the unsigned 32-bit GID with flip flags. The TMJ path
is cleaner but the TMX path is more fragile (could fail on non-numeric GID strings).

---

### 18.13 [TideMapLoader.scala] — loadLayer: currentTileSet is raw null, risks NPE

**File**: `maps/tiled/TideMapLoader.scala:244`
**Severity**: Medium — potential NPE on malformed tide files

```scala
var currentTileSet: TiledMapTileSet = null // scalastyle:ignore
```

If a `.tide` file has a `<Static>` or `<Animated>` element before any `<TileSheet>`
element in a row, `currentTileSet` is null when accessed:

```scala
cell.tile = currentTileSet.getTile(firstgid + ...)  // NPE if no TileSheet yet
```

In Java, this would also NPE, but it would be caught by the IDE as a potential null
dereference. In Scala, the raw `null` bypasses the `Nullable` safety system.

**Impact**: Malformed `.tide` files with missing `TileSheet` references in tile rows
cause an unguarded NPE instead of a descriptive error.

---

### 18.14 [AnimatedTiledMapTile.scala] — getCurrentFrameIndex: boundary/break used correctly

**File**: `maps/tiled/tiles/AnimatedTiledMapTile.scala:84-96`
**Severity**: None (verified correct)

The `boundary`/`break` pattern here is correctly used. The `break(i)` inside the `while`
loop exits the enclosing `boundary` block and returns `i` from the method. This matches
the Java `return i;` inside the `for` loop. Unlike the bugs found in GlyphLayout and
BitmapFontCache (Section 11), this use of `boundary`/`break` is correct because the
`boundary` wraps exactly the scope that should be exited.

---

### 18.15 Summary of Maps/Tiled Issues

| # | File | Issue | Severity | Porting Bug? |
|---|------|-------|----------|--------------|
| 18.1 | BaseTmxMapLoader | mergeProperties missing break in search | Medium | Yes |
| 18.2 | TideMapLoader | spacing parsed from margin | Medium | No (Java bug) |
| 18.3 | TideMapLoader | sheetSize values discarded | Low | No (Java behavior) |
| 18.4 | TideMapLoader | missing texture silently skipped | Medium | Yes (Nullable semantics) |
| 18.5 | TideMapLoader | lenient defaults mask errors | Low | Yes (added defaults) |
| 18.6 | BaseTmx/TmjMapLoader | y-offset not adjusted on texture failure | Medium | Yes (Nullable semantics) |
| 18.7 | BaseTmxMapLoader | name defaults to "" not null | Low | Yes (Nullable conversion) |
| 18.8 | BaseTmjMapLoader | Int vs Integer inconsistency | Info | No (auto-boxing OK) |
| 18.9 | HexagonalTiledMapRenderer | animated tiles skipped | Medium | No (Java limitation) |
| 18.10 | OrthoCachedTiledMapRenderer | group layers not handled | Medium | No (Java limitation) |
| 18.11 | BaseTmxMapLoader | raw null in template parsing | Low | Style only |
| 18.12 | BaseTmx/TmjMapLoader | GID parsing divergent patterns | Info | No |
| 18.13 | TideMapLoader | currentTileSet raw null risk | Medium | Style/safety |

Four porting bugs (18.1, 18.4, 18.5, 18.6) and two Nullable semantics changes (18.6, 18.7)
that alter error behavior from crash-on-failure to silent-failure. The renderers are
faithfully ported with no logic errors found. The TMX and TMJ loaders' tile ID handling
(GID masking, flip flags) is correct.

---

*Analysis continues in sections below as more findings are uncovered...*

---

## 15. Asset Loading Pipeline Issues

### 15.1 [AssetLoadingTask.scala] — Empty getDependencies return bypasses immediate-load optimization (HIGH)

**Severity**: HIGH — doubles or triples loading time for all no-dependency async assets

In Java, loaders with no dependencies return `null` from `getDependencies()`. In SGE, all
loaders return an empty `DynamicArray[AssetDescriptor[?]]()` instead. The `apply()` method
in AssetLoadingTask checks `dependencies.isEmpty` to decide whether to call `loadAsync`
immediately:

```scala
// AssetLoadingTask.scala:62-73
dependencies = Nullable(
  asyncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), loaderParams)
)
dependencies.foreach { deps =>
  removeDuplicates(deps)
  manager.addDependencies(assetDesc.fileName, deps)
}
if (dependencies.isEmpty) {   // BUG: checks Nullable.empty, not empty array!
  asyncLoader.loadAsync(...)
  asyncDone = true
}
```

`Nullable(emptyDynamicArray).isEmpty` is `false` (it's `Some(emptyArray)`, not `None`).
So the `loadAsync` call is never made here. The loading proceeds through the slower
multi-step path:

**Java flow (2 update cycles):**
1. submit Future → `call()` → getDependencies returns null → `loadAsync` called → `asyncDone = true`
2. depsFuture done → `dependenciesLoaded = true`, `asyncDone = true` → `loadSync` → done

**SGE flow (4+ update cycles):**
1. submit depsFuture → `apply()` → getDependencies returns emptyArray → adds empty deps → `asyncDone` stays false
2. depsFuture done → `dependenciesLoaded = true`, `asyncDone = false`
3. submit loadFuture → `apply()` → `loadAsync` called → `asyncDone = true`
4. loadFuture done → `loadSync` → done

**Affected loaders** (all return empty array instead of Java null):
- TextureLoader, CubemapLoader, ShaderProgramLoader, PixmapLoader
- MusicLoader, SoundLoader, I18NBundleLoader

For a loading screen with 100 textures, this means ~200 extra update() cycles — roughly
3–5 seconds of unnecessary loading time at 60fps.

**Fix**: Either:
- Change `dependencies.isEmpty` to check if the wrapped array is also empty:
  `if (dependencies.forall(_.size == 0))`
- Or have no-dependency loaders return `null` (wrapped as `Nullable.empty` by the caller)

---

### 15.2 [AssetLoadingTask.scala] — Same issue in handleSyncLoader for sync loaders (MEDIUM)

**Severity**: MEDIUM — extra update cycle per sync asset with no dependencies

```scala
// AssetLoadingTask.scala:96-116
dependencies = Nullable(
  syncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), loaderParams)
)
dependencies match {
  case dep if dep.isEmpty =>       // Only matches Nullable.empty (null from getDependencies)
    asset = Nullable(syncLoader.load(...))
    boundary.break()
  case deps =>                      // Matches Some(emptyArray) — defers load!
    deps.foreach(removeDuplicates)
    manager.addDependencies(...)
}
```

When a sync loader returns an empty `DynamicArray`, the asset is NOT loaded immediately.
Instead, dependencies are "injected" (empty no-op), and the load is deferred to the
next `update()` call.

**Impact**: ParticleEffectLoader (when no atlas is used) and any future sync loaders
returning empty arrays take an extra update cycle.

---

### 15.3 [CubemapLoader.scala] — NPE for non-KTX cubemap files (HIGH)

**Severity**: HIGH — crash when loading non-KTX cubemaps without explicit data

```scala
// CubemapLoader.scala:38-59
if (param.forall(_.cubemapData.isEmpty)) {
  // ...
  if (fileName.contains(".ktx") || fileName.contains(".zktx")) {
    info.data = KTXTextureData(file, genMipMaps)
  }
  // No else branch! info.data stays uninitialized for non-KTX files
} else {
  info.data = parameter.cubemapData  // Only set when cubemapData is provided
}
if (!info.data.isPrepared) info.data.prepare()  // NPE: info.data is uninitialized!
```

`info.data` is declared as `scala.compiletime.uninitialized` (null at runtime). If the
file is not `.ktx`/`.zktx` AND no `cubemapData` parameter is provided, `info.data` is
never assigned. The subsequent `info.data.isPrepared` call throws NPE.

**Note**: The Java original has the same bug — it's a pre-existing LibGDX limitation
where cubemaps can only be loaded from KTX files or explicit CubemapData.

**Fix**: Throw a descriptive error instead of NPE:
```scala
if (info.data == null)
  throw SgeError.InvalidInput(s"Cannot load cubemap '$fileName': only .ktx/.zktx supported without explicit cubemapData")
```

---

### 15.4 [AssetLoadingTask.scala] — unload() uses try/catch for type checking (LOW)

**Severity**: LOW — incorrect pattern, works by accident

```scala
// AssetLoadingTask.scala:166-173
def unload(): Unit =
  try {
    val asyncLoader = loader.asInstanceOf[AsynchronousAssetLoader[Any, AssetLoaderParameters[Any]]]
    asyncLoader.unloadAsync(...)
  } catch {
    case _: ClassCastException =>  // Not an async loader, nothing to do
  }
```

Java uses `instanceof`:
```java
if (loader instanceof AsynchronousAssetLoader)
  ((AsynchronousAssetLoader)loader).unloadAsync(...);
```

Using exception-based control flow for type checking is an anti-pattern. While it works
(SynchronousAssetLoader cannot be cast to AsynchronousAssetLoader), it's fragile —
a ClassCastException from within `unloadAsync` would be silently swallowed.

**Fix**: Use `loader match { case a: AsynchronousAssetLoader[?, ?] => a.unloadAsync(...); case _ => }`

---

### 15.5 [AssetManager.scala] — contains() checks all tasks vs Java checks only first (LOW)

**Severity**: LOW — behavioral improvement over Java, but semantic difference

Java's `contains(fileName)` only checks `tasks.first()` (the root task):
```java
if (tasks.size > 0 && tasks.first().assetDesc.fileName.equals(fileName)) return true;
```

SGE checks ALL tasks (including dependency tasks):
```scala
var i = 0
while (i < tasks.size) {
  if (tasks(i).assetDesc.fileName == fileName) boundary.break(true)
  i += 1
}
```

This is arguably more correct — if a dependency named "texture.png" is actively loading,
`contains("texture.png")` returns true in SGE but false in Java. However, code ported
from LibGDX that relies on the Java semantics could behave differently.

---

### 15.6 [AssetManager.scala] — update() catches NonFatal vs Java catches Throwable (LOW)

**Severity**: LOW — actually an improvement

```scala
// AssetManager.scala:429
catch { case NonFatal(t) => handleTaskError(t) ... }
```

Java catches all `Throwable` including `OutOfMemoryError`, `StackOverflowError`, etc.
SGE only catches non-fatal exceptions, letting fatal JVM errors propagate. This is better
Scala practice but means fatal errors during loading skip `handleTaskError` and the
error listener.

---

### 15.7 [AssetManager.scala] — RefCountedContainer uses raw null (LOW)

**Severity**: LOW — internal implementation detail, violates project no-null rule

```scala
// AssetManager.scala:854
private[assets] class RefCountedContainer(var obj: Any = null, var refCount: Int = 1)
```

The `obj` field defaults to `null` and is set to the loaded asset later. While annotated
with `@SuppressWarnings`, this is the only place in the asset pipeline that uses raw null.
If `addAsset` is called with a null asset (which shouldn't happen), the container silently
stores null.

---

### 15.8 [TextureLoader.scala / CubemapLoader.scala] — info fields use compiletime.uninitialized (MEDIUM)

**Severity**: MEDIUM — will NPE if loadSync is called before loadAsync

```scala
// TextureLoader.scala:80-81
class TextureLoaderInfo {
  var filename: String      = scala.compiletime.uninitialized  // null at runtime
  var data:     TextureData = scala.compiletime.uninitialized  // null at runtime
  var texture:  Nullable[Texture] = Nullable.empty
}
```

`scala.compiletime.uninitialized` produces `null` for reference types at runtime.
If `loadSync` is called before `loadAsync` (e.g., due to a loader lifecycle error),
`info.data` is null, causing NPE in `Texture(info.data)`. The Java version has the same
issue (fields are null by default), but the SGE version is more surprising because
`uninitialized` looks like a deliberate compile-time construct.

Same pattern in `CubemapLoader.CubemapLoaderInfo`.

---

### 15.9 [ModelLoader.scala] — getDependencies passes Nullable(parameters) instead of raw parameters (LOW)

**Severity**: LOW — defensive difference from Java

```scala
// ModelLoader.scala:59
val data = loadModelData(file, Nullable(parameters))
```

Java:
```java
ModelData data = loadModelData(file, parameters);
```

In Java, `parameters` can be null (when no params are provided). In SGE,
`Nullable(parameters)` wraps null as `Nullable.empty`. The `loadModelData` method
accepts `Nullable[P]`, so this is correct. But it means model loaders always receive
a wrapped parameter, never a raw null reference.

---

### 15.10 [AssetManager.scala] — Loader instance shared across concurrent loads (DESIGN)

**Severity**: LOW (design concern, same as Java)

Each asset type has a SINGLE loader instance registered in the AssetManager:
```scala
private val loaders: ObjectMap[Class[?], ObjectMap[String, AssetLoader[?, ?]]] = ObjectMap()
```

Loaders store intermediate state in instance fields (e.g., TextureLoader.info,
BitmapFontLoader.data). If two assets of the same type are loaded concurrently
(or their loading interleaves), the shared mutable state would be corrupted.

In both Java and SGE, this is safe ONLY because:
1. The AssetManager uses a single-threaded executor
2. The task stack ensures dependencies complete before parents
3. `synchronized` on AssetManager prevents concurrent update() calls

This means the asset pipeline cannot be parallelized without redesigning loaders.

---

### 15.11 [AssetManager.scala] — handleTaskError clears all tasks without unloading their dependencies (MEDIUM)

**Severity**: MEDIUM — resource leak on loading failure

```scala
// AssetManager.scala:666-688
private def handleTaskError(t: Throwable): Unit = {
  val task = tasks.pop()       // Pop the faulty task
  if (task.dependenciesLoaded && task.dependencies.isDefined) {
    task.dependencies.foreach { deps =>
      deps.foreach { desc => unload(desc.fileName) }
    }
  }
  tasks.clear()                // Clear remaining tasks — NO unloading!
  // ...
}
```

When a loading error occurs, only the faulty task's dependencies are unloaded. All
remaining tasks in the stack (other assets and their dependencies that were partially
loaded) are simply cleared without cleanup. Any already-loaded dependency assets remain
in the manager with refCount=1 but no parent asset to reference them.

The Java original has the same behavior — this is a pre-existing LibGDX limitation.

**Impact**: If loading a texture atlas fails after some of its texture dependencies were
loaded, those textures remain in the AssetManager, consuming GPU memory, with no way to
reference or unload them (their parent atlas was never registered).

---

### 15.12 [AssetManager.scala] — clear() potential infinite loop with circular dependencies (LOW)

**Severity**: LOW — defensive concern

```scala
// AssetManager.scala:765
while (assetTypes.size > 0) {
  // ... compute dependency counts ...
  // only dispose root assets (not referenced by others)
  assetNames.foreach { asset =>
    if (dependencyCount.getOrElse(asset, 0) == 0) unload(asset)
  }
}
```

If a circular dependency exists in the asset dependency graph (A depends on B depends
on A), no asset would have a dependency count of 0, and the while loop would run forever.

In practice, circular dependencies shouldn't occur (they would also cause infinite loading
loops), but no guard exists against this degenerate case. Same in Java.

---

### 15.13 [TextureAtlasLoader.scala] — getDependencies doesn't pass page wrapU/wrapV to TextureParameter (LOW)

**Severity**: LOW — pre-existing LibGDX limitation, not a porting bug

```scala
// TextureAtlasLoader.scala:56-62
val params = TextureLoader.TextureParameter()
params.format = Nullable(page.format)
params.genMipMaps = page.useMipMaps
params.minFilter = page.minFilter
params.magFilter = page.magFilter
// wrapU and wrapV NOT set — default to ClampToEdge
```

TextureAtlas pages can specify wrap modes (Repeat, MirroredRepeat), but these aren't
passed to the TextureParameter when creating the dependency. The wrap modes are only
applied when the TextureAtlas constructs TextureRegions from the loaded textures.

Same in Java — texture wrap modes from atlas pages rely on later application, not the
initial texture load. The texture may briefly have wrong wrap modes between loading and
atlas construction.

---

### 15.14 Summary of Asset Pipeline Issues

| # | File | Issue | Severity |
|---|------|-------|----------|
| 15.1 | AssetLoadingTask | Empty array vs null skips async immediate-load | **High** |
| 15.2 | AssetLoadingTask | Same issue for sync loaders | **Medium** |
| 15.3 | CubemapLoader | NPE for non-KTX files | **High** |
| 15.4 | AssetLoadingTask | try/catch for type checking | Low |
| 15.5 | AssetManager | contains() checks all tasks | Low |
| 15.6 | AssetManager | NonFatal vs Throwable | Low |
| 15.7 | AssetManager | RefCountedContainer uses null | Low |
| 15.8 | TextureLoader/CubemapLoader | uninitialized fields | **Medium** |
| 15.9 | ModelLoader | Nullable-wrapped parameters | Low |
| 15.10 | AssetManager | Shared loader instances | Low (design) |
| 15.11 | AssetManager | handleTaskError leaks deps | **Medium** |
| 15.12 | AssetManager | clear() potential infinite loop | Low |
| 15.13 | TextureAtlasLoader | Missing wrapU/wrapV | Low |

The most impactful issue is **15.1**: the null-vs-empty-array semantic mismatch in
`AssetLoadingTask.apply()` causes all no-dependency async loaders (textures, sounds,
music, pixmaps, shaders, cubemaps, i18n bundles) to take 2-3 extra update cycles per
asset. For games loading 100+ assets, this can add several seconds to loading screens.

Issue **15.3** is a crash bug (shared with Java LibGDX) that will NPE if anyone tries
to load a cubemap from a non-KTX file format without providing explicit CubemapData.

---

## 16. boundary/break Misuse Audit

**Background**: In Java, `break` exits the innermost `for`/`while` loop and `continue`
skips to the next iteration. In Scala 3, `scala.util.boundary { break(value) }` exits
the enclosing `boundary` block. If the `boundary` is placed at the method level but
`break` is used where Java had a loop `break` or `continue`, it will exit the **entire
method** instead of just the loop.

This audit examined **all 120+ files** under `sge/src/main/scala/sge/` that use
`boundary`/`break` (~350+ break call sites across ~95 files). The vast majority are
correct `return` replacements (boundary at method level, break replaces `return`). The
bugs below are the cases where `break` was intended to exit a loop but exits the method,
or where nested boundary scoping causes `return`-replacement breaks to only exit an
inner boundary instead of the method.

### Categorization of Patterns

| Pattern | Count | Correctness |
|---------|-------|-------------|
| `boundary` at method level, `break(value)` replaces `return value` | ~300 | **Correct** |
| `boundary` wraps a single while loop, `break(())` replaces loop `break` | ~15 | **Correct** |
| Nested boundary: outer=method, inner=loop body (for `continue`) | ~5 | **Correct** |
| `break` inside loop exits method instead of loop (code after loop skipped) | 6 | **BUG** |
| `break` inside nested boundary exits inner scope instead of method | 3 | **BUG** |

---

### 16.1 [GlyphLayout.scala:353,360] — wrapGlyphs break exits method instead of while loop (CRITICAL)

**Already documented as 11.1** — included here for completeness.

In `wrapGlyphs`, two while loops scan for whitespace boundaries around a wrap index.
In Java, finding a non-whitespace character `break`s the loop and continues to the
split/copy logic below. In Scala, `boundary.break(Nullable.empty)` exits the entire
method, returning `Nullable.empty`:

```scala
// Skip whitespace before the wrap index.
var firstEnd = wrapIndex
while (firstEnd > 0) {
  if (!fontData.isWhitespace(glyphs2(firstEnd - 1).id.toChar))
    scala.util.boundary.break(Nullable.empty)  // BUG: exits method
  firstEnd -= 1
}
// Skip whitespace after the wrap index.
var secondStart = wrapIndex
while (secondStart < glyphCount) {
  if (!fontData.isWhitespace(glyphs2(secondStart).id.toChar))
    scala.util.boundary.break(Nullable.empty)  // BUG: exits method
  secondStart += 1
}
```

**Java original**: `break` exits the while loop; execution continues at the copy/split
logic that creates the second GlyphRun.

**Impact**: Virtually every text wrap attempt returns null (aborting the wrap), because
non-whitespace at word boundaries is the normal case. All BitmapFont wrapping is broken.

**Fix**: Wrap each while loop in its own `boundary { }` block so `break` only exits
that loop.

---

### 16.2 [GlyphLayout.scala:309] — truncateRun break exits method instead of while loop (CRITICAL)

In `truncateRun` (line 287), a while loop counts how many glyphs fit within the target
width. When a glyph exceeds the width, Java `break`s the loop and then appends the
truncation string ("..."). In Scala, `break(())` exits the entire `truncateRun` method:

```scala
private def truncateRun(...): Unit = scala.util.boundary {
  // ... compute truncateWidth, adjustedTargetWidth ...

  var count = 0
  var width = run.x
  while (count < run.xAdvances.size) {
    val xAdvance = xAdvances(count)
    width += xAdvance
    if (width > adjustedTargetWidth) scala.util.boundary.break(())  // BUG: exits method
    count += 1
  }

  if (count > 1) {
    // Append truncation glyphs ("...") — THIS CODE IS NEVER REACHED
    run.glyphs.removeRange(count - 1, run.glyphs.size)
    // ... append truncate run glyphs ...
  } else {
    // Replace entire run with truncate string — ALSO NEVER REACHED
    // ...
  }
}
```

**Java original**: `break` exits the while loop; `if (count > 1)` is evaluated and the
truncation string is appended.

**Impact**: Text truncation (e.g., "Hello Wor..." when text exceeds width) never appends
the truncation string. The method returns without modifying the run at all when the text
overflows, leaving the original untruncated text.

**Fix**: Wrap the while loop in its own `boundary { }` block.

---

### 16.3 [BitmapFontCache.scala:253,264] — draw(batch, start, end) break exits method instead of loop (CRITICAL)

**Already documented as 11.2** — included here for completeness.

The multi-page font rendering path uses `break()` in two places where Java used `break`
(inner while loop) and `continue` (outer for loop):

```scala
def draw(spriteBatch: Batch, start: Int, end: Int): Unit = scala.util.boundary {
  if (pageVertices.length == 1) { ... boundary.break() }  // OK: early return

  for (i <- pageVertices.indices) {
    // ...
    while (ii < glyphIndices.size) {
      if (glyphIndex >= end) scala.util.boundary.break()  // BUG: exits method
      // ...
    }
    if (offset == -1 || count == 0) scala.util.boundary.break()  // BUG: exits method
    spriteBatch.draw(...)
  }
}
```

**Java original**: Inner `break` exits the while loop (continues to offset/count check).
Outer `continue` skips to next page. Both are within the for loop.

**Impact**: Multi-page BitmapFonts only render the first page (or stop rendering when a
page has no matching glyphs in the range). All subsequent pages are skipped.

**Fix**: Wrap the while loop in its own boundary for the inner break; use a nested
boundary around the for loop body for the continue.

---

### 16.4 [BitmapFontCache.scala:202] — setColors(float, start, end) break exits method instead of loop (HIGH)

**Already documented as 11.3** — included here for completeness.

Same pattern as 16.3. The inner `break` (while loop) and outer `continue` (for loop)
both became `boundary.break()` which exits the method:

```scala
while (j < glyphIndices.size) {
  val glyphIndex = glyphIndices(j)
  if (glyphIndex >= end) scala.util.boundary.break()  // BUG: exits method
  // ...
}
```

**Impact**: Setting colors on a glyph range in multi-page fonts only processes the first
page, leaving remaining pages with stale colors.

---

### 16.5 [ButtonGroup.scala:109,112] — break(false) exits inner boundary instead of method (HIGH)

In `canCheck`, an inner boundary wraps a `while(true)` loop. Inside this loop,
`break(false)` was intended to exit the METHOD (replacing Java's `return false`), but
it exits only the inner boundary (the while loop), because the inner boundary's inferred
type captures both `break(false)` and `break()`:

```scala
protected[ui] def canCheck(...): Boolean = scala.util.boundary {  // outer: Label[Boolean]
  // ...
  if (maxCheckCount != -1 && allChecked.size >= maxCheckCount) {
    if (!uncheckLast) scala.util.boundary.break(false)  // OK: no inner boundary yet
    var tries = 0
    scala.util.boundary {  // inner: Label[AnyVal] (inferred from both break types)
      while (true) {
        // ... uncheck last button ...
        if (button.isChecked == newState) scala.util.boundary.break(false)  // BUG: exits inner
        if (allChecked.size < maxCheckCount) scala.util.boundary.break()    // OK: exits inner
        tries += 1
        if (tries > 10) scala.util.boundary.break(false)  // BUG: exits inner
      }
    }
  }
  allChecked.add(button)  // WRONGLY EXECUTED after break(false)
  // ...
  true  // WRONGLY RETURNED instead of false
}
```

**Java original**:
- `if (button.isChecked == newState) return false;` — exits the METHOD
- `if (allChecked.size < maxCheckCount) break;` — exits the while loop
- `if (tries > 10) return false;` — exits the METHOD

**Scala behavior**: Because `break(false)` (Boolean) and `break()` (Unit) both appear
inside the inner boundary, the inner boundary's label type is inferred as `AnyVal`
(LUB of Boolean and Unit). Both breaks resolve to the inner label (it's the closest
matching scope). So `break(false)` on lines 109 and 112 exit only the while loop.

After the inner boundary, execution continues at `allChecked.add(button)` (line 116),
adding the button to the checked set even though the method should have returned `false`
(rejecting the check). The method then returns `true` (line 120), allowing the check.

**Impact**: When `uncheckLast` is true and the max check count is reached:
- If unchecking the last button causes `button.isChecked == newState` (e.g., via a
  listener that re-checks the button), the check is incorrectly allowed instead of
  being rejected.
- After 10 failed attempts to uncheck, the check is incorrectly allowed instead of
  being rejected.
This can cause ButtonGroups to exceed their `maxCheckCount`, breaking radio button
behavior.

**Fix**: The `break(false)` calls on lines 109 and 112 must explicitly use the outer
boundary's label. Either:
1. Name the outer boundary's label and pass it to `break` explicitly, or
2. Replace the inner `scala.util.boundary { while(true) { ... } }` with a
   `var`-based loop, and use `break(false)` only for the outer boundary.

---

### 16.6 [ParticleEmitter.scala:205] — addParticles break exits method, skipping _activeCount update (HIGH)

In `addParticles`, `break(())` exits the method when no free particle slot is found,
but this skips the `_activeCount += actualCount` update on line 208:

```scala
def addParticles(count: Int): Unit = scala.util.boundary {
  val actualCount = Math.min(count, _maxParticleCount - _activeCount)
  if (actualCount == 0) scala.util.boundary.break(())
  val active = this._active
  var index  = 0
  val n      = active.length
  var i      = 0
  while (i < actualCount) {
    var found = false
    while (index < n && !found)
      if (!active(index)) { activateParticle(index); active(index) = true; index += 1; found = true }
      else { index += 1 }
    if (!found) scala.util.boundary.break(())  // BUG: exits method
    i += 1
  }
  this._activeCount += actualCount  // SKIPPED when breaking early
}
```

**Java original**: Uses `break` to exit the outer labeled for loop, then
`this.activeCount += count` still executes (though Java also has a subtle bug here —
it adds the full clamped count even if fewer particles were activated).

**Scala behavior**: When the `active` array is exhausted before all `actualCount`
particles are activated, the method exits without updating `_activeCount` at all.
The particles that WERE activated are now invisible to the count tracking.

**Impact**: `_activeCount` becomes out of sync with the actual number of active
particles. Subsequent calls to `addParticle()`/`addParticles()` may think there's
room for more particles (because `_activeCount` is too low) but find all slots taken,
or the emitter may report incorrect active counts.

**Fix**: Track the actual number activated in a local counter and update `_activeCount`
before any early exit:
```scala
var activated = 0
while (i < actualCount) {
  // ... find and activate ...
  if (!found) { this._activeCount += activated; return }
  activated += 1
  i += 1
}
this._activeCount += activated
```

---

### 16.7 Summary of boundary/break Misuse

| # | File:Line | Pattern | Severity | Status |
|---|-----------|---------|----------|--------|
| 16.1 | GlyphLayout.scala:353,360 | break exits method instead of while loop | **Critical** | Already in 11.1 |
| 16.2 | GlyphLayout.scala:309 | break exits method instead of while loop | **Critical** | **New** |
| 16.3 | BitmapFontCache.scala:253,264 | break exits method instead of while/for loop | **Critical** | Already in 11.2 |
| 16.4 | BitmapFontCache.scala:202 | break exits method instead of while/for loop | **High** | Already in 11.3 |
| 16.5 | ButtonGroup.scala:109,112 | break(false) exits inner boundary not method | **High** | **New** |
| 16.6 | ParticleEmitter.scala:205 | break exits method, skips count update | **High** | **New** |

### 16.8 Files Audited and Confirmed Correct

The following files use `boundary`/`break` correctly (all breaks replace `return`
statements or are correctly scoped to their enclosing loop):

**Simple return replacements (no loops involved):**
ImageTextButton.scala, TextButton.scala, ImageButton.scala, CheckBox.scala,
Slider.scala, ProgressBar.scala, Container.scala, SplitPane.scala, Window.scala,
Touchpad.scala, Label.scala, Sprite.scala, ShapeRenderer.scala, GLErrorListener.scala,
MipMapGenerator.scala, AnimatedTiledMapTile.scala, I18nBundle.scala, Timer.scala,
WidgetGroup.scala, Pool.scala, XmlReader.scala, PolygonRegionLoader.scala,
RemoteSender.scala, DataOutput.scala, DataInput.scala, BitmapFontLoader.scala,
GLTexture.scala, FileTextureArrayData.scala, DepthShader.scala, Attribute.scala,
ModelBuilder.scala, TextureDescriptor.scala, PolygonSprite.scala, Interpolation.scala,
Skin.scala, InputMultiplexer.scala, WavInputStream.scala, FloatTextureData.scala,
ModelInstance.scala, Model.scala, Gdx2DPixmap.scala, TextureArray.scala,
AssetLoadingTask.scala, ShaderProgram.scala, ParticleEffect(g3d).scala, ParticleEffect(g2d).scala,
ParallelAction.scala, ObjLoader.scala, PolygonRegion.scala

**Return replacements inside loops (break at method boundary, replaces return):**
ObjectMap.scala, ArrayMap.scala, Mesh.scala, DynamicArray.scala (all 14 uses),
Intersector.scala (all 21 uses), AssetManager.scala (all 20 uses), Tree.scala,
TextField.scala, SgeList.scala, Actor.scala, Group.scala, Table.scala, TextArea.scala,
TextureAtlas.scala, BitmapFont.scala, BitmapFontCache.scala (translate, tint, addToCache),
ParticleEmitter.scala (addParticle, draw, getScale, getColor, setSprites),
GestureDetector.scala, ScrollPane.scala, ComparableTimSort.scala, TimSort.scala,
Stage.scala, Frustum.scala, EarClippingTriangulator.scala, CumulativeDistribution.scala,
DefaultTextureBinder.scala, BaseShaderProvider.scala, BaseShader.scala,
AnimationController.scala, ModelCache.scala, InputEventQueue.scala,
DragAndDrop.scala, PixmapPacker.scala, MapLayers.scala, MapObjects.scala,
TiledMapTileSets.scala, TiledMapLoader.scala, BaseTiledMapLoader.scala,
BaseTmxMapLoader.scala, ObjectSet.scala, BinaryHeap.scala, RemoteInput.scala,
Octree.scala, ParallelArray.scala, Attributes.scala, ParticleController.scala,
Node.scala, GradientColorValue.scala, ScaledNumericValue.scala,
EllipseSpawnShapeValue.scala, ResourceData.scala, BaseAnimationController.scala,
ButtonGroup.scala (lines 79, 92, 96, 101 — the outer boundary breaks are correct),
SelectBox.scala, Batch.scala, SpriteBatch.scala, PolygonSpriteBatch.scala,
CpuSpriteBatch.scala, PolygonBatch.scala, RepeatablePolygonSprite.scala,
SpriteCache.scala, NinePatch.scala, VertexAttributes.scala, Gdx2DPixmap.scala,
NetJavaServerSocketImpl.scala, NetJavaSocketImpl.scala, SgeHttpClient.scala,
PixmapPackerIO.scala, GLFrameBuffer.scala

**Correctly scoped loop boundaries (boundary wraps just the loop):**
TextField.scala:239,253,268,498 — boundary wraps single while loop
ButtonGroup.scala:73 — boundary wraps while loop for setChecked
Tree.scala:336 — boundary wraps while loop in drawIcons
Tree.scala:443,449 — nested boundaries for break/continue pattern
Stage.scala:134 — boundary wraps while loop
GradientColorValue.scala:47 — `boundary[Unit]` wraps while loop
ScaledNumericValue.scala:59 — `boundary[Unit]` wraps while loop
Table.scala:220 — boundary wraps nested while loops (labeled break equivalent)
BitmapFont.scala:346,406 — boundary wraps while(true) read loops
TextureAtlas.scala:374,385,395,409 — boundary wraps while(true) parse loops

## 20. Net/Files/IO Issues

### 20.1 SgeHttpClient — Double pool-free race on cancel vs. completion

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala` (lines 58–91)

When `cancel()` is called while a `Future` is completing concurrently:

1. `cancel()` removes the entry from `pending`, calls `listener.cancelled()`, and calls
   `requestPool.free(request)`.
2. `future.onComplete` fires, sees `entry.cancelled = true` (good — skips listener),
   but then unconditionally calls `freeRequest(request)` which calls
   `requestPool.free(request)` a **second time**.

The request object gets returned to the pool twice, so two subsequent `obtain()` calls
can return the **same instance** simultaneously. This causes aliased mutation: two
callers configure the same `SgeHttpRequest` object, corrupting each other's headers,
URL, body, etc.

**Severity:** High — silent data corruption in concurrent HTTP usage.

**Fix:** `freeRequest` should only call `requestPool.free` if `pending.remove` actually
found and removed the entry (i.e., it hadn't already been removed by `cancel`).

### 20.2 SgeHttpClient — Missing URL validation before send

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala` (line 59)

LibGDX's `NetJavaImpl.sendHttpRequest` validates the URL and calls
`listener.failed(GdxRuntimeException("can't process a HTTP request without URL set"))`
when it is null/empty. SGE calls `Uri.unsafeParse(req.url)` directly, which throws on
an empty/invalid URL. The exception propagates into the `Future`, where `onComplete`
calls `listener.failed(ex)` — so the listener IS notified, but the error message is an
opaque sttp parse failure rather than a clear "URL not set" diagnostic. More importantly,
if `buildSttpRequest` throws *before* the Future is created, the exception propagates
directly to the caller with no listener notification at all.

**Severity:** Medium — poor error messages; potential unhandled exception.

### 20.3 SgeHttpClient — GET/HEAD content not appended as query string

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala` (lines 137–145)

LibGDX appends `HttpRequest.getContent()` as a query string for GET and HEAD methods:
```java
if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD")) {
    String queryString = "";
    String value = httpRequest.getContent();
    if (value != null && !"".equals(value)) queryString = "?" + value;
    url = new URL(httpRequest.getUrl() + queryString);
}
```

SGE's `buildSttpRequest` sets the content as the **request body** regardless of method.
Sending a body with GET is non-standard (RFC 7231 §4.3.1 says servers MAY reject it).
Any code relying on LibGDX's GET-with-parameters pattern will silently fail.

**Severity:** Medium — behavioral regression for GET requests with content.

### 20.4 SgeHttpClient — Missing connect timeout (only read timeout set)

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala` (lines 129–131)

LibGDX sets both `connectTimeout` and `readTimeout` from the same `timeoutMs` value:
```java
connection.setConnectTimeout(httpRequest.getTimeOut());
connection.setReadTimeout(httpRequest.getTimeOut());
```

SGE only sets `readTimeout` via `r.readTimeout(...)`. The sttp `connectionTimeout` is
never set, so connections to unresponsive hosts will hang until the OS-level TCP timeout
(typically 75–120 seconds).

**Severity:** Medium — requests to unreachable hosts hang much longer than expected.

### 20.5 SgeHttpClient — Thread safety of pending map is fragile

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala` (lines 46, 63–65, 83–91)

The `pending` map uses ad-hoc `synchronized` blocks. The `onComplete` callback reads
`entry.cancelled` (a `@volatile var`) outside synchronization. While `@volatile`
provides visibility, the ordering between `cancel()` setting `cancelled = true` and
removing from `pending`, versus `onComplete` reading `cancelled` and calling
`freeRequest`, creates a window where the listener can be called after cancellation
(race between lines 68 and 88). Using `ConcurrentHashMap` or an `AtomicReference`-based
approach would be more robust.

**Severity:** Low-Medium — unlikely in practice but theoretically unsound.

### 20.6 SgeHttpResponse — Binary response data corruption

**Files:** `sge/src/main/scala/sge/net/SgeHttpResponse.scala` (lines 24–28)

The sttp request type is `Request[Either[String, String]]`, meaning the response body is
always decoded as a UTF-8 string by sttp. Then:

```scala
private lazy val bodyString: String = response.body.merge
private lazy val bodyBytes: Array[Byte] = bodyString.getBytes("UTF-8")
```

This means **binary responses** (images, protobuf, compressed data, etc.) are:
1. Decoded from raw bytes to String by sttp (corrupting non-UTF-8 byte sequences)
2. Re-encoded to UTF-8 bytes (double corruption)

LibGDX's `getResult()` returns raw bytes from the `InputStream`; SGE's returns
string-round-tripped UTF-8 bytes. Any binary HTTP response is silently corrupted.

**Severity:** High — completely breaks binary HTTP downloads (textures, assets, etc.).

**Fix:** Change the sttp request to `Request[Either[String, Array[Byte]]]` or use
`asByteArray` response handling, then derive the string from bytes only when
`getResultAsString()` is called.

### 20.7 SgeHttpResponse — Original response charset ignored

**Files:** `sge/src/main/scala/sge/net/SgeHttpResponse.scala` (lines 27)

`bodyBytes` always uses `getBytes("UTF-8")` regardless of the `Content-Type` charset
header (e.g., `text/html; charset=iso-8859-1`). If the server sends ISO-8859-1 encoded
text, `getResultAsString()` returns the correct string (sttp handles decoding), but
`getResult()` re-encodes to UTF-8, changing the byte representation.

LibGDX reads raw bytes from the connection and separately decodes to string using UTF-8.
The byte representation always matches what the server sent.

**Severity:** Medium — affects code that processes raw response bytes with non-UTF-8
charsets.

### 20.8 NetJavaSocketImpl — Socket leak on connection failure

**Files:** `sge/src/main/scala/sge/net/NetJavaSocketImpl.scala` (lines 29–42)

In the connecting constructor, `new JSocket()` is created, then `applyHints` and
`socket.connect` are called inside `try`. If either throws, the exception is caught and
rethrown as `RuntimeException`, but the `JSocket` is never closed:

```scala
def this(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints) = {
  this(new JSocket())  // socket created
  try {
    NetJavaSocketImpl.applyHints(socket, hints)
    socket.connect(address, ...)
  } catch {
    case e: Exception =>
      throw new RuntimeException(...)  // socket leaked!
  }
}
```

The socket's file descriptor leaks. Under heavy connection-failure load, this exhausts
file descriptors.

**Severity:** Medium — resource leak on connection failure. (Same bug exists in LibGDX.)

### 20.9 NetJavaServerSocketImpl — Server socket leak on bind failure

**Files:** `sge/src/main/scala/sge/net/NetJavaServerSocketImpl.scala` (lines 36–60)

In `initializeServer`, `server = new JServerSocket()` is created, then configuration
and `server.bind(...)` are called. If `bind` or any hint-setting method throws, the
`JServerSocket` is never closed:

```scala
server = new JServerSocket()  // created
hintsOpt.foreach { h =>
  server.setPerformancePreferences(...)
  // ... if any of these throw ...
}
server.bind(address, h.backlog)  // if this throws ...
// catch wraps and rethrows — server socket leaked
```

**Severity:** Medium — leaks server socket on bind failure (port in use, permission
denied, etc.). (Same bug exists in LibGDX.)

### 20.10 NetJavaServerSocketImpl — `uninitialized` field risks NPE

**Files:** `sge/src/main/scala/sge/net/NetJavaServerSocketImpl.scala` (line 28)

```scala
private var server: JServerSocket = scala.compiletime.uninitialized
```

`scala.compiletime.uninitialized` compiles to JVM `null`. If `initializeServer` throws
(line 34), the constructor propagates the exception, but if any code catches it and
still holds a reference to the partially-constructed object, `server` is `null`.
Subsequent calls to `close()` will silently succeed (the `Nullable(server).foreach`
finds empty), but `accept()` will NPE on `server.accept()`.

**Severity:** Low — only manifests if caller catches constructor exception and reuses
the object.

### 20.11 NetJavaSocketImpl — `getRemoteAddress()` NPE after close

**Files:** `sge/src/main/scala/sge/net/NetJavaSocketImpl.scala` (lines 69–70)

After `close()` sets `socket = null`, calling `getRemoteAddress()` crashes:

```scala
override def getRemoteAddress(): String =
  socket.getRemoteSocketAddress().toString()  // NPE if socket is null
```

No null check. LibGDX has the same bug, but SGE could improve on it.

**Severity:** Low — calling methods after close is a usage error, but should throw a
clear message rather than NPE.

### 20.12 FileHandle — `readBytes(bytes, offset, size)` returns wrong value

**Files:** `sge/src/main/scala/sge/files/FileHandles.scala` (lines 232–247)

```scala
def readBytes(bytes: Array[Byte], offset: Int, size: Int): Int = {
  val input    = read()
  var position = 0
  try {
    var count = input.read(bytes, offset + position, size - position)
    while (count > 0) {
      position += count
      count = input.read(bytes, offset + position, size - position)
    }
    position - offset  // BUG: should be just `position`
  }
```

`position` starts at 0 and accumulates the number of bytes read. The return value
`position - offset` is negative when `offset > 0`. The correct return is `position`.

This is inherited from LibGDX (`FileHandle.java:262`), which has the same bug.

**Severity:** Medium — wrong return value when offset > 0; callers using offset will
get incorrect byte counts.

### 20.13 HttpBackendFactoryImpl (JVM) — Singleton backend never truly closes

**Files:** `sge/src/main/scalajvm/sge/net/HttpBackendFactoryImpl.scala` (lines 22–28)

The JVM backend is a singleton `object` with `private val backend = DefaultFutureBackend()`.
When `SgeHttpClient.close()` calls `backend.close()`, the sttp backend is shut down.
But since `HttpBackendFactoryImpl` is a Scala `object` (initialized once per classloader),
any subsequent `SgeHttpClient.apply()` call will reuse the **closed** backend.

LibGDX uses a `ThreadPoolExecutor` per `NetJavaImpl` instance, avoiding this problem.

**Severity:** Medium — creating a second `SgeHttpClient` after closing the first will
fail because the shared backend is closed.

**Fix:** Either make `HttpBackendFactoryImpl` create a fresh backend per client, or use
lazy re-initialization with a guard.

### 20.14 HttpBackendFactoryImpl (Native) — Sync backend blocks thread pool

**Files:** `sge/src/main/scalanative/sge/net/HttpBackendFactoryImpl.scala` (lines 25–28)

The Native backend wraps synchronous curl calls in `Future { backend.send(request) }`
using `ExecutionContext.global`. Scala Native's global execution context has limited
parallelism. Multiple concurrent HTTP requests will serialize on the thread pool,
potentially blocking the game's main thread if it awaits results.

**Severity:** Low — only affects Scala Native; adequate for typical game workloads but
could deadlock under heavy concurrent HTTP usage.

### 20.15 FileHandle — `list(FileFilter)` uses `getFile()` for filter but `child()` for result

**Files:** `sge/src/main/scala/sge/files/FileHandles.scala` (lines 412–419)

```scala
def list(filter: FileFilter): Array[FileHandle] = {
  ...
  relativePaths.map(path => child(path))
    .filter(childHandle => filter.accept(childHandle.getFile()))
  ...
}
```

This is actually correct (Java also does `filter.accept(child.file())`), but the SGE
version creates all child handles first, then filters — whereas Java creates handles
only for accepted children. For large directories, SGE allocates more temporary objects.

**Severity:** Low — performance regression for large directories, not a correctness bug.

### 20.16 FileHandle — `copyFile` double-closes input stream

**Files:** `sge/src/main/scala/sge/files/FileHandles.scala` (lines 610–616)

```scala
private def copyFile(source: FileHandle, dest: FileHandle): Unit = {
  val input = source.read()
  try
    dest.write(input, false)  // write(InputStream, Boolean) closes `input` internally
  finally
    utils.StreamUtils.closeQuietly(input)  // closes `input` again
}
```

The `write(InputStream, Boolean)` method (line 306) already closes `input` in its own
`finally` block. Then `copyFile`'s `finally` closes it a second time. Double-close is
generally harmless for `FileInputStream` but could cause issues with streams that don't
tolerate it.

**Severity:** Low — harmless for standard streams but technically incorrect.

### 20.17 Preferences — `put()` type signature incompatible with LibGDX

**Files:** `sge/src/main/scala/sge/Preferences.scala` (line 42)

LibGDX:
```java
public Preferences put(Map<String, ?> vals);
```

SGE:
```scala
def put(vals: Map[String, Boolean | Int | Long | Float | String]): Preferences
```

The LibGDX signature accepts `Map<String, ?>` — any value type, with runtime type
checking in implementations. SGE restricts to a union type, which is type-safe but
**breaks source compatibility** with any code that passes a `Map[String, Any]` or stores
`Double` values (which LibGDX implementations silently coerce).

**Severity:** Low — API incompatibility, not a runtime bug. The union type is arguably
an improvement.

### 20.18 SgeHttpClient — `ExecutionContext.global` used implicitly

**Files:** `sge/src/main/scala/sge/net/SgeHttpClient.scala` (line 35)

```scala
private given ExecutionContext = ExecutionContext.global
```

The global execution context is used for all `Future.onComplete` callbacks. These
callbacks invoke user-provided `HttpResponseListener` methods, which may perform
blocking I/O, update game state, or interact with the render thread. Running on
`ExecutionContext.global` means:

1. Listener callbacks run on the global fork-join pool, not the game's main thread
   (LibGDX callbacks also ran on a background thread, so this is consistent).
2. If a listener blocks, it can starve the global pool, affecting all other `Future`
   operations in the application.

**Severity:** Low — consistent with LibGDX behavior but worth documenting. Game code
should use `Application.postRunnable` to marshal results to the render thread.

### 20.19 FileHandle.readString — InputStream leak when charset is invalid

**Files:** `sge/src/main/scala/sge/files/FileHandles.scala` (lines 184–200)

The `reader` val is created **outside** the `try` block:

```scala
def readString(charset: Nullable[String] = Nullable.empty): String = {
  val output = new StringBuilder(estimateLength())
  val reader = charset.fold(new InputStreamReader(read()))(cs => new InputStreamReader(read(), cs))
  try {
    // ... read loop ...
  } catch {
    case ex: IOException =>
      throw utils.SgeError.FileReadError(this, "Error reading layout file", Some(ex))
  } finally
    utils.StreamUtils.closeQuietly(reader)
}
```

If the charset is invalid, `new InputStreamReader(read(), cs)` throws
`UnsupportedEncodingException`. Because the exception occurs before the `try` block:
1. The `InputStream` from `read()` is **leaked** (never closed)
2. The exception propagates uncaught — the `try/catch` is never entered
3. `closeQuietly(reader)` in `finally` never runs

In the Java original, reader creation is **inside** the try block, so the
`IOException` catch handles it (the stream still leaks in Java, but the exception is
at least properly wrapped).

**Severity:** Medium — resource leak + different error propagation behavior from Java.

**Fix:** Move the reader creation inside the `try` block, and close the raw
`InputStream` in a nested try if the `InputStreamReader` constructor fails.

### 20.20 FileHandle.writer — FileOutputStream leak on charset failure

**Files:** `sge/src/main/scala/sge/files/FileHandles.scala` (lines 327–340)

```scala
def writer(append: Boolean, charset: Nullable[String] = Nullable.empty): Writer = {
  // ... classpath/internal checks ...
  parent().mkdirs()
  try {
    val output = new FileOutputStream(getFile(), append)
    charset.fold(new OutputStreamWriter(output): Writer)(cs => new OutputStreamWriter(output, cs))
  } catch {
    case ex: IOException => ...
  }
}
```

If `new OutputStreamWriter(output, cs)` throws `UnsupportedEncodingException` (an
`IOException`), the `FileOutputStream` in `output` is **not closed** in the catch
block. The catch handler only rethrows as `SgeError` without closing `output`.

The Java original has the same leak. However, since SGE uses `Future`-based concurrency
more heavily, leaked file descriptors accumulate faster in error scenarios.

**Severity:** Low — only triggers with invalid charsets; same bug as Java original.

**Fix:** Wrap the `OutputStreamWriter` creation in a nested try that closes `output`
on failure.

### 20.21 SgeHttpResponse.getResultAsStream — entire body forced into memory

**Files:** `sge/src/main/scala/sge/net/SgeHttpResponse.scala` (lines 24–33)

LibGDX's `getResultAsStream()` was designed to provide the raw HTTP connection's
`InputStream` for efficient streaming of large responses (e.g., downloading assets).
SGE's implementation:

```scala
private lazy val bodyString: String = response.body.merge
private lazy val bodyBytes: Array[Byte] = bodyString.getBytes("UTF-8")
override def getResultAsStream(): InputStream = new ByteArrayInputStream(bodyBytes)
```

The entire response is materialized as a `String` by sttp (due to the
`Request[Either[String, String]]` type), then converted to bytes, then wrapped in
`ByteArrayInputStream`. For multi-megabyte responses (texture packs, audio files, map
data), this:
1. Doubles memory usage (String + byte array both in memory)
2. Defeats the streaming contract — callers expecting to process data incrementally
   will still have the entire body in RAM
3. Combined with issue 20.6 (binary corruption), binary streaming is completely broken

This is an architectural consequence of using sttp's string-based response type rather
than a byte-stream-based one.

**Severity:** Medium — memory pressure for large downloads; blocks incremental
processing.

**Fix:** Use `asByteArrayAlways` or a streaming response body type in sttp, and
provide the `InputStream` from the raw response rather than buffering everything.

### 20.22 HttpParametersUtils — mutable global state not thread-safe

**Files:** `sge/src/main/scala/sge/net/HttpParametersUtils.scala` (lines 24–26)

```scala
object HttpParametersUtils {
  var defaultEncoding:    String = "UTF-8"
  var nameValueSeparator: String = "="
  var parameterSeparator: String = "&"
```

These are mutable `var`s on a global singleton `object`. The Java original has the
same mutable statics, but in LibGDX's single-threaded HTTP model (requests sent from
one thread), this was less risky. SGE dispatches HTTP requests via `Future` on the
global execution context, so if one thread modifies `defaultEncoding` while another
calls `convertHttpParameters`, the encoding used is a data race (no synchronization,
no `@volatile`).

**Severity:** Low — unlikely to be modified at runtime in practice; same as Java
original but riskier in SGE's concurrent model.

### 20.23 HttpBackendFactoryImpl (JS) — same singleton close problem as JVM

**Files:** `sge/src/main/scalajs/sge/net/HttpBackendFactoryImpl.scala` (lines 20–29)

The JS backend is a singleton `object` with `private val backend = DefaultFutureBackend()`
— the exact same pattern as the JVM backend (issue 20.13). Calling `close()` shuts
down the Fetch API backend, and any subsequent `SgeHttpClient.apply()` will reuse the
closed backend.

**Severity:** Medium — same as 20.13, affects Scala.js target.

### 20.24 FileHandle.readBytes(bytes, offset, size) — loop can read beyond array bounds

**Files:** `sge/src/main/scala/sge/files/FileHandles.scala` (lines 232–247)

Related to 20.12 but a separate correctness concern. The read call is:

```scala
input.read(bytes, offset + position, size - position)
```

If `offset + size > bytes.length`, the read will eventually attempt to write past the
end of the array, throwing `IndexOutOfBoundsException`. Neither SGE nor Java validate
that `offset + size <= bytes.length` before entering the loop. While callers are
expected to pass valid bounds, the lack of a precondition check means the error manifests
as an obscure exception deep in the I/O loop rather than a clear argument validation
failure.

**Severity:** Low — caller error, but a defensive check would improve diagnostics.
Same as Java original.

## 19. Platform/Cross-Compilation Issues

This section catalogs issues that affect cross-compilation to Scala.js and Scala Native,
runtime assumptions tied to the JVM, and gaps in the `(using Sge)` context propagation pattern.

### 19.1 `synchronized` blocks — incompatible with Scala.js single-threaded model

**8 files** in shared source use `synchronized` (JVM monitor-based locking). On Scala.js,
`synchronized` compiles but is a **no-op** — it provides zero thread safety. This is
harmless in single-threaded JS code *unless* the design relies on `synchronized` for
correctness of state visible across `Future` callbacks or async boundaries. On Scala Native,
`synchronized` works but maps to a pthreads mutex, with different performance characteristics.

| File | Usage pattern | JS risk |
|------|--------------|---------|
| `AssetManager.scala` | ~40 synchronized methods wrapping every public API | **Low** — single-threaded on JS, but the design assumes multi-threaded callers |
| `InputEventQueue.scala` | Every event method + `drain()` synchronized on `this` | **Low** — input is main-thread only on JS |
| `SgeHttpClient.scala` | `pending` map synchronized for concurrent Future callbacks | **Medium** — `Future.onComplete` runs on JS microtask queue but is still sequential; safe in practice |
| `Timer.scala` | Nested `synchronized` (threadLock → Timer → Task → postedTasks) + `wait()`/`notifyAll()` | **High** — `Object.wait()` is **not available** on Scala.js. This file will not work at all on JS without a redesign (already noted in a TODO) |
| `BufferUtils.scala` | `unsafeBuffers.synchronized` for thread-safe bookkeeping | **Low** — bookkeeping only |
| `PixmapIO.scala` | `writeBuffer.synchronized` / `readBuffer.synchronized` | **Low** — I/O classes unlikely to be used on JS |
| `ParticleEffectLoader.scala` (g3d) | `items.synchronized` for cache access | **Low** |
| `SgeNativesLoader.scala` | `synchronized` on companion object for one-time init guard | **Low** — init is sequential |
| `RemoteSender.scala` | `this.synchronized` on every input callback | **Medium** — networking class, unlikely on JS but uses `java.net.Socket` |

**Recommendation**: For `Timer.scala`, redesign is required (Gears structured concurrency or
a JS-compatible scheduler). For others, evaluate whether `synchronized` can be replaced
with Scala.js-safe alternatives (e.g., `AtomicReference` patterns, or simply documenting
single-threaded assumptions).

### 19.2 `Object.wait()` / `notifyAll()` — JVM-only monitor operations

`Timer.scala` uses `threadLock.wait(waitMillis)` (line 285) and `threadLock.notifyAll()`
(line 156). These are **JVM-only** — they do not exist on Scala.js and will fail at runtime
on Scala Native without proper pthread support.

- **Scala.js**: `Object.wait()` is not implemented; will throw at runtime.
- **Scala Native**: Supported but depends on the pthreads threading model.

**Files affected**: `Timer.scala`

### 19.3 `Thread.currentThread().getStackTrace` — unavailable on Scala.js

`GLErrorListener.scala` (line 44) calls `Thread.currentThread().getStackTrace` to
walk the call stack for error reporting. On Scala.js, `Thread.currentThread()` returns
a stub that does not support `getStackTrace`. This will fail silently or throw.

- **Scala.js**: Will not work. Need a JS-specific fallback (e.g., `new Error().stack`).
- **Scala Native**: Works but stack traces may be less detailed.

**Files affected**: `GLErrorListener.scala`

### 19.4 `ExecutionContext.global` — behavior differences across platforms

Three files use `ExecutionContext.global`:

| File | Usage |
|------|-------|
| `Timer.scala` | `Future(this.run())(using ExecutionContext.global)` — spawns timer thread |
| `SgeHttpClient.scala` | `private given ExecutionContext = ExecutionContext.global` |
| `AssetLoadingTask.scala` | `Future(apply())` with injected executor |

- **Scala.js**: `ExecutionContext.global` maps to the microtask queue (single-threaded).
  `Future` blocks that assume concurrent execution (like Timer's infinite loop with
  `wait()`) will **deadlock** the event loop.
- **Scala Native**: `ExecutionContext.global` uses a thread pool, similar to JVM.

**Recommendation**: `Timer.scala`'s `Future`-based background thread is fundamentally
incompatible with Scala.js. `SgeHttpClient` and `AssetLoadingTask` are fine since they
use futures for async I/O completion, not long-running loops.

### 19.5 `java.nio.ByteBuffer` / `java.nio.FloatBuffer` — partial Scala.js support

**35+ files** import `java.nio.*` types. These are in the shared source tree:

- **Scala.js**: `java.nio.ByteBuffer` et al. are partially implemented in scala-js-javalib.
  `ByteBuffer.allocateDirect()` is **not supported** (throws `UnsupportedOperationException`).
  Heap buffers work. `ByteOrder.nativeOrder()` returns `BIG_ENDIAN` on JS (which may differ
  from expectations).
- **Scala Native**: `java.nio` is reasonably well-supported, including direct buffers.

Key files with `allocateDirect` or `nativeOrder()` assumptions:
- `BufferUtils.scala` — `newUnsafeByteBuffer` calls `newDisposableByteBuffer` (platform-abstracted)
- `GLFrameBuffer.scala` — `ByteBuffer.allocateDirect(16 * Integer.SIZE / 8)`
- `ShaderProgram.scala` — `ByteBuffer` with `nativeOrder()`
- `FileHandles.scala` — `RandomAccessFile` + `FileChannel.map()` → memory-mapped I/O

The `PlatformOps.buffer` abstraction correctly handles `newDisposableByteBuffer` per-platform,
but raw `ByteBuffer.allocateDirect()` calls in shared code bypass this abstraction.

**Files with raw `allocateDirect`**: `GLFrameBuffer.scala`, `ShaderProgram.scala`

### 19.6 `java.io.File` / `java.io.FileInputStream` / `RandomAccessFile` — JVM/Native only

`FileHandles.scala` is heavily JVM-centric:
- Uses `java.io.File`, `FileInputStream`, `FileOutputStream`, `RandomAccessFile`
- Uses `FileChannel.map()` for memory-mapped I/O
- Uses `java.nio.channels.FileChannel`

- **Scala.js**: `java.io.File` is **not available**. `FileHandles.scala` cannot compile on
  Scala.js without a platform-specific replacement (virtual filesystem or fetch-based).
- **Scala Native**: `java.io.File` is supported.

**Files affected**: `FileHandles.scala`, `ParticleEffect.scala` (g2d, imports `java.io.File`)

### 19.7 `java.net.Socket` / `ServerSocket` — JVM/Native only

Four files use Java networking that is unavailable on Scala.js:

| File | Java API used |
|------|--------------|
| `NetJavaSocketImpl.scala` | `java.net.Socket`, `InetSocketAddress` |
| `NetJavaServerSocketImpl.scala` | `java.net.ServerSocket`, `InetSocketAddress` |
| `RemoteInput.scala` | `java.net.ServerSocket`, `InetAddress` |
| `RemoteSender.scala` | `java.net.Socket`, `DataOutputStream` |

- **Scala.js**: None of `java.net.*` is available. These files must be moved to
  JVM/Native-specific source trees or guarded with platform-specific alternatives.
- **Scala Native**: `java.net.Socket` is supported in recent Scala Native versions.

**Recommendation**: Move `NetJavaSocketImpl`, `NetJavaServerSocketImpl`, `RemoteInput`,
and `RemoteSender` to `src/main/scalajvm/` (and optionally `scalanative/`). Provide
WebSocket-based alternatives for Scala.js if needed.

### 19.8 `java.util.zip.*` — partial Scala.js support

Five files use `java.util.zip` classes:

| File | Classes used |
|------|-------------|
| `PixmapIO.scala` | `CRC32`, `CheckedOutputStream`, `Deflater`, `DeflaterOutputStream`, `InflaterInputStream` |
| `ETC1.scala` | `GZIPInputStream`, `GZIPOutputStream` |
| `KTXTextureData.scala` | `GZIPInputStream` |
| `BaseTmxMapLoader.scala` | `GZIPInputStream`, `InflaterInputStream` |
| `BaseTmjMapLoader.scala` | `GZIPInputStream`, `InflaterInputStream` |

- **Scala.js**: `java.util.zip` is **not available** in scala-js-javalib. These files
  will not compile on Scala.js without a polyfill (e.g., pako.js via facade).
- **Scala Native**: Supported via native zlib bindings.

### 19.9 `java.util.regex.Pattern` — Scala.js limitations

`GLVersion.scala` and `BitmapFont.scala` use `java.util.regex.Pattern`. Scala.js
implements `java.util.regex` via JavaScript RegExp, which mostly works but has edge cases
(e.g., no lookbehind support in older JS engines, no `CANON_EQ` flag).

- **Risk**: Low unless exotic regex features are used.
- **Files affected**: `GLVersion.scala`, `BitmapFont.scala`

### 19.10 `java.util.Locale` — Scala.js limitations

`I18NBundleLoader.scala` and `TextFormatter.scala` use `java.util.Locale`.
- **Scala.js**: `java.util.Locale` has limited support. `Locale.getDefault()` returns
  `Locale.ROOT` on JS. Locale-sensitive formatting may behave differently.
- **Scala Native**: Supported.

### 19.11 `System.getProperty("os.name")` — unavailable on Scala.js

`UIUtils.scala` uses `System.getProperty("os.name")` to detect the OS at initialization
(lines 27-43). This determines `isAndroid`, `isMac`, `isWindows`, `isLinux`, `isIos`.

- **Scala.js**: `System.getProperty` returns `null` for all properties.
  All five OS detection flags will be `false` on JS, which may cause `ctrl()` and
  other platform-adaptive methods to behave incorrectly.
- **Scala Native**: Supported.

**Recommendation**: Replace with a platform-detection abstraction. On JS, use
`window.navigator.platform` or `window.navigator.userAgent`.

### 19.12 `System.arraycopy` — works on all platforms (no issue)

`System.arraycopy` is used extensively (~80+ call sites across 25+ files). This is
well-supported on all three platforms (JVM, Scala.js, Scala Native). **No action needed.**

### 19.13 `System.nanoTime()` / `System.currentTimeMillis()` — platform differences

`TimeUtils.scala` wraps these in opaque types. Used directly in `Timer.scala`,
`DragAndDrop.scala`, `DragScrollListener.scala`, `TextField.scala`, `SgeList.scala`.

- **Scala.js**: `System.nanoTime()` is implemented via `performance.now()` with
  microsecond precision (not nanosecond). `System.currentTimeMillis()` works fine.
- **Scala Native**: Both are supported.
- **Risk**: Low — precision differences are unlikely to matter for game timing.

### 19.14 `@volatile` fields — semantic differences on Scala.js

Four files use `@volatile`:
- `AssetLoadingTask.scala` — 7 volatile fields for async state
- `Timer.scala` — `timer: Option[Timer]` volatile field
- `SgeHttpClient.scala` — `cancelled: Boolean` volatile field
- `Cell.scala` — volatile fields

- **Scala.js**: `@volatile` is a no-op (single-threaded). Safe but misleading.
- **Scala Native**: Supported with proper memory barriers.

### 19.15 Platform abstraction layer — well-designed but incomplete for JS

The `sge.platform` package provides a clean abstraction:
- `PlatformOps` object with per-platform implementations (JVM/JS/Native source trees)
- `BufferOps`, `ETC1Ops` traits with platform-specific implementations
- `GlOps`, `AudioOps`, `WindowingOps` for desktop FFI (not applicable to JS)

**Gaps**:
1. **JS `PlatformOps`** only provides `etc1` and `buffer` — no `windowing`, `audio`, or `gl`.
   This is expected (JS uses WebGL/WebAudio directly) but means shared code that accesses
   `PlatformOps.windowing` etc. will fail on JS.
2. **JVM `PlatformOps`** uses `scala.compiletime.uninitialized` for `windowing`/`audio`/`gl`,
   meaning they're `null` until the desktop backend sets them. Accessing before init → NPE.
3. The JS PlatformOps declares only `etc1` and `buffer` fields — there's no way for
   shared code to conditionally check for `windowing`/`audio`/`gl` availability.

### 19.16 `Sge` context propagation — comprehensive but with gaps

The `(using Sge)` pattern is used extensively (150+ files). The pattern is:
- `Sge` is a `final case class` holding `Application`, `Graphics`, `Audio`, `Files`,
  `Input`, `Net` — passed as a context parameter.
- `SgeAware` trait provides `sgeAvailable(sge: Sge)` for listeners that need early access.
- `Sge()` companion summons the implicit.

**Consistency**:
- Most classes that need platform services correctly declare `(using Sge)`.
- `Timer.scala` correctly uses `(using sge.Sge)` on `Timer`, `Task`, `TimerThread`, and
  static methods.
- `RemoteInput` / `RemoteSender` correctly require `(using Sge)`.

**Potential gaps**:
1. `AssetManager.scala` has ~40 `synchronized` methods but the class itself does **not**
   declare `(using Sge)`. It accesses loaders and file resolvers but doesn't need direct
   Sge context — loaders receive it separately. This appears intentional.
2. `InputEventQueue.scala` does not use `(using Sge)` — correct, since it's a pure data
   structure for event buffering.
3. `SgeNativesLoader.scala` does not use `(using Sge)` — correct, since native loading is
   a static one-time initialization.

### 19.17 `HttpBackendFactory` — correctly platform-split

The HTTP backend is properly abstracted:
- Shared trait: `HttpBackendFactory` in `sge/src/main/scala/`
- Per-platform: `HttpBackendFactoryImpl` in `scalajvm/`, `scalajs/`, `scalanative/`

This is the correct pattern. `SgeHttpClient` uses the shared trait and the concrete
implementation is injected per-platform. **No cross-compilation issues.**

### 19.18 `RemoteInput` / `RemoteSender` — should be JVM/Native only

Both `RemoteInput.scala` and `RemoteSender.scala` are in the **shared** source tree
(`sge/src/main/scala/`) but use:
- `java.net.ServerSocket` / `java.net.Socket` (not available on Scala.js)
- `java.io.DataInputStream` / `java.io.DataOutputStream`
- Blocking I/O patterns (infinite loops reading from sockets)

These files are fundamentally incompatible with Scala.js and should be moved to
platform-specific source trees.

### 19.19 `PixmapIO` — multiple JVM-only dependencies in shared source

`PixmapIO.scala` uses:
- `java.util.zip.Deflater`, `DeflaterOutputStream`, `InflaterInputStream`, `CRC32`,
  `CheckedOutputStream` — not available on Scala.js
- `synchronized` blocks for concurrent PNG writing
- Blocking I/O patterns

This file should either be moved to JVM/Native source or have the zip dependency
abstracted behind a platform trait.

### 19.20 `java.util.Random` — works but with caveats

`MathUtils.scala` and `RandomXS128.scala` use `java.util.Random`.
- **Scala.js**: Supported but `Random.nextGaussian()` has a different implementation
  than the JVM version, potentially causing non-deterministic cross-platform behavior
  in seeded RNG scenarios.
- **Scala Native**: Supported.

### 19.21 Summary: files that must be platform-split for Scala.js compilation

| Priority | File | Blocking dependency |
|----------|------|-------------------|
| **Must move** | `RemoteInput.scala` | `java.net.ServerSocket` |
| **Must move** | `RemoteSender.scala` | `java.net.Socket` |
| **Must move** | `NetJavaSocketImpl.scala` | `java.net.Socket` |
| **Must move** | `NetJavaServerSocketImpl.scala` | `java.net.ServerSocket` |
| **Must abstract** | `FileHandles.scala` | `java.io.File`, `FileChannel` |
| **Must abstract** | `PixmapIO.scala` | `java.util.zip.*` |
| **Must abstract** | `ETC1.scala` | `java.util.zip.GZIP*` |
| **Must redesign** | `Timer.scala` | `Object.wait()`, `notifyAll()` |
| **Should fix** | `GLFrameBuffer.scala` | `ByteBuffer.allocateDirect()` |
| **Should fix** | `ShaderProgram.scala` | `ByteBuffer.allocateDirect()` |
| **Should fix** | `UIUtils.scala` | `System.getProperty("os.name")` |
| **Should fix** | `GLErrorListener.scala` | `Thread.currentThread().getStackTrace` |
| **Low priority** | `BaseTmxMapLoader.scala` | `java.util.zip.*` |
| **Low priority** | `BaseTmjMapLoader.scala` | `java.util.zip.*` |
| **Low priority** | `KTXTextureData.scala` | `java.util.zip.GZIPInputStream` |

### 19.22 `ModelLoader.scala` — `synchronized` in shared source (addendum to 19.1)

`ModelLoader.scala` uses `items.synchronized` in two places (loadAsync and loadSync)
to protect a shared cache of loaded model data across async/sync boundaries. This was
omitted from the 19.1 synchronized inventory.

- **Scala.js**: Low risk — `synchronized` is a no-op but loading is sequential on JS.
- **Scala Native**: Works normally.

**Recommendation**: Add to the 19.1 table for completeness.

### 19.23 `BufferUtils.scala` — 7 direct `ByteBuffer.allocateDirect()` calls bypass `PlatformOps`

Section 19.5 notes that `GLFrameBuffer.scala` and `ShaderProgram.scala` call
`ByteBuffer.allocateDirect()` directly, bypassing the platform abstraction. However,
`BufferUtils.scala` itself contains **7** such calls in its factory methods:

| Method | Direct `allocateDirect` call |
|--------|-----------------------------|
| `newFloatBuffer(n)` | `ByteBuffer.allocateDirect(n * 4)` |
| `newDoubleBuffer(n)` | `ByteBuffer.allocateDirect(n * 8)` |
| `newByteBuffer(n)` | `ByteBuffer.allocateDirect(n)` |
| `newShortBuffer(n)` | `ByteBuffer.allocateDirect(n * 2)` |
| `newCharBuffer(n)` | `ByteBuffer.allocateDirect(n * 2)` |
| `newIntBuffer(n)` | `ByteBuffer.allocateDirect(n * 4)` |
| `newLongBuffer(n)` | `ByteBuffer.allocateDirect(n * 8)` |

These are the most-called buffer allocation methods in the codebase. The separate
`newUnsafeByteBuffer` correctly delegates to `PlatformOps.buffer.newDisposableByteBuffer`,
but these 7 methods do not.

- **Scala.js**: `ByteBuffer.allocateDirect()` throws `UnsupportedOperationException` on
  Scala.js. Every call to these factory methods will fail.
- **Scala Native**: Works, but does not use the Rust-backed allocator.

**Recommendation**: Route these through `PlatformOps.buffer.newDisposableByteBuffer` or
provide a platform-aware buffer factory. This is the **highest-impact** `allocateDirect`
issue since these methods are used throughout the codebase by GL buffer objects, mesh data,
shader programs, and texture data.

### 19.24 `HttpParametersUtils.scala` — `java.net.URLEncoder` in shared source

`HttpParametersUtils.scala` imports `java.net.URLEncoder` for URL parameter encoding.

- **Scala.js**: `java.net.URLEncoder` is **not available** in scala-js-javalib.
  Will fail to compile. Should use `js.URIUtils.encodeURIComponent` on JS or
  provide a platform-split implementation.
- **Scala Native**: Supported.

### 19.25 `FileHandleResolver.scala` — creates `java.io.File` in shared source

`FileHandleResolver.scala` (line 123) creates `new java.io.File(fileName)` directly.
This is in addition to the `FileHandles.scala` dependency documented in 19.6.

- **Scala.js**: Will not compile (`java.io.File` unavailable).
- **Scala Native**: Works.

**Recommendation**: This should be refactored to use `FileHandle` abstractions instead
of `java.io.File` directly.

### 19.26 `StreamUtils.scala` — `java.io` stream classes with mixed Scala.js support

`StreamUtils.scala` imports:
- `java.io.ByteArrayOutputStream` — available on Scala.js
- `java.io.Closeable` — available on Scala.js
- `java.io.InputStream` — available on Scala.js
- `java.io.InputStreamReader` — available on Scala.js
- `java.io.OutputStream` — available on Scala.js
- `java.io.StringWriter` — available on Scala.js

Unlike `FileHandles.scala`, `StreamUtils` uses **only stream-based I/O** (no `File` or
`FileChannel`). All of these classes are available in scala-js-javalib.

- **Scala.js**: Should compile and work. **No action needed.**
- **Scala Native**: Works.

### 19.27 Platform traits use `java.nio` types in shared API signatures

The `BufferOps` platform trait (in `sge/src/main/scala/sge/platform/`) declares methods
with `java.nio.Buffer`, `java.nio.ByteBuffer` parameter and return types:

```
def newDisposableByteBuffer(numBytes: Int): ByteBuffer
def freeMemory(buffer: ByteBuffer): Unit
def getBufferAddress(buffer: Buffer): Long
```

This means **even the JS implementation** (`BufferOpsJs`) must work with `java.nio.ByteBuffer`.
On Scala.js, `ByteBuffer` is heap-only (no direct buffers), and operations like
`getBufferAddress` are meaningless. The JS implementation correctly exists but must
handle these constraints (e.g., `getBufferAddress` should throw `UnsupportedOperationException`).

- **Scala.js**: Compiles (heap ByteBuffer works), but `getBufferAddress` cannot return
  a meaningful native pointer.
- **Impact**: Low — the JS PlatformOps already exists and handles this.

### 19.28 `GlOps`/`WindowingOps`/`AudioOps` — `Long` handle pattern incompatible with JS

The desktop FFI traits (`GlOps`, `WindowingOps`, `AudioOps`) use `Long` values for
native pointers and handles:

```
def createContext(windowHandle: Long, ...): Long
def createWindow(width: Int, height: Int, title: String): Long
def initEngine(simultaneousSources: Int, ...): Long
```

This pattern is correct for JVM (Panama) and Native (C FFI), where pointers are 64-bit
integers. However:

- **Scala.js**: These traits are **not intended for JS** (JS uses WebGL2/WebAudio, not
  native FFI). The JS `PlatformOps` correctly omits `windowing`, `audio`, and `gl` fields.
- **Design concern**: If shared code accesses `PlatformOps.windowing` etc., it will fail
  on JS at **compile time** (field doesn't exist), which is the correct failure mode.
  The JVM/Native `PlatformOps` uses `scala.compiletime.uninitialized` (effectively `null`)
  for these fields until the backend sets them, risking **runtime NPEs** if accessed before
  backend initialization.

**Recommendation**: Consider wrapping `windowing`/`audio`/`gl` in `Option` or `Nullable`
to make the "not yet initialized" state explicit and fail with a clear error message rather
than NPE.

### 19.29 No `java.util.concurrent.*` usage — positive finding

Despite concerns, **no files** in the shared source tree import `java.util.concurrent.*`
classes (`ConcurrentHashMap`, `ExecutorService`, `CountDownLatch`, `AtomicInteger`, etc.).
Thread synchronization relies solely on `synchronized` blocks and `@volatile` fields.

This is a positive finding for cross-compilation:
- **Scala.js**: No `j.u.concurrent` blockers to resolve.
- **Scala Native**: No dependency on JVM-specific concurrent data structures.

### 19.30 `WindowingOps.setWindowIcon` — platform FFI trait coupled to graphics module

`WindowingOps.setWindowIcon` (line 189) takes `Array[sge.graphics.Pixmap]` as a parameter.
This creates a dependency from the low-level platform FFI layer (`sge.platform`) up to the
graphics module (`sge.graphics`). While this works on JVM/Native, it creates architectural
coupling:

- The platform layer should ideally depend only on primitive types and `java.nio` buffers.
- If `Pixmap` itself needs platform-specific implementations, this creates a circular
  dependency risk.
- On Scala.js, `Pixmap` would use `HTMLCanvasElement` internally, making the `Pixmap` type
  itself platform-specific.

**Recommendation**: Change the signature to accept raw pixel data (e.g.,
`Array[(ByteBuffer, Int, Int)]` for `(pixels, width, height)` tuples) instead of `Pixmap`
objects.

### 19.31 Updated summary: additional files requiring attention for Scala.js

| Priority | File | Blocking dependency |
|----------|------|-------------------|
| **Must fix** | `BufferUtils.scala` (7 factory methods) | `ByteBuffer.allocateDirect()` |
| **Must fix** | `HttpParametersUtils.scala` | `java.net.URLEncoder` |
| **Must fix** | `FileHandleResolver.scala` | `java.io.File` |
| **Should fix** | `ModelLoader.scala` | `synchronized` (low risk, document only) |
| **Should fix** | `WindowingOps.setWindowIcon` | Pixmap coupling to FFI layer |
| **Design** | `PlatformOps` windowing/audio/gl | `uninitialized` → `Nullable` for safety |
| **No issue** | `StreamUtils.scala` | All `java.io` stream types available on JS |
| **No issue** | No `java.util.concurrent.*` usage | Clean — no concurrent collections |

## 22. Audio Subsystem Issues

### 22.1 WavInputStream.read(buffer) — Potential infinite loop when buffer > dataRemaining

**File:** `sge/src/main/scala/sge/audio/WavInputStream.scala` (lines 123–139)

When the read buffer is larger than the remaining audio data and the WAV file contains
content after the `data` chunk (e.g., metadata chunks like `LIST`, `INFO`, `bext`), the
loop can spin indefinitely:

```scala
while (offset < buffer.length) {
  val length = Math.min(super.read(buffer, offset, buffer.length - offset), dataRemaining)
  if (length == -1) { ... }
  offset += length
  dataRemaining -= length
}
```

Once `dataRemaining` reaches 0, `Math.min(super.read(...), 0)` returns 0. The loop
continues with `offset` unchanged and `dataRemaining` stuck at 0 — an infinite loop. This
only terminates if the underlying stream also reaches EOF (returns -1), but if the WAV
file has chunks after the data chunk, `super.read()` returns > 0 and the loop never exits.

This is a pre-existing LibGDX bug (Java `Wav.WavInputStream.read()` has the identical
`do-while` pattern), faithfully ported to SGE.

**Severity:** High — hangs audio streaming for WAV files with trailing metadata chunks.
Short sound effects loaded via `Sound` (which reads all data at once) are especially
vulnerable since the buffer is typically larger than the data.

**Fix:** Limit the read request to `dataRemaining`:
```scala
val toRead = Math.min(buffer.length - offset, dataRemaining)
if (toRead == 0) break(offset)
val length = super.read(buffer, offset, toRead)
```

### 22.2 WavInputStream.read(buffer) — Reads past data chunk boundary

**File:** `sge/src/main/scala/sge/audio/WavInputStream.scala` (line 129)

The call `super.read(buffer, offset, buffer.length - offset)` reads up to
`buffer.length - offset` bytes from the underlying stream. This can be much larger than
`dataRemaining`. The `Math.min` with `dataRemaining` only caps the *counted* bytes, not
the actual I/O — the stream position has already advanced past the data chunk boundary.

For example, with `buffer.length = 4096` and `dataRemaining = 100`:
1. `super.read(buffer, 0, 4096)` reads up to 4096 bytes, consuming past the data chunk
2. `Math.min(4096, 100) = 100` — only 100 bytes are counted
3. The stream is now positioned 3996 bytes into the next chunk(s)

This causes corruption if the `WavInputStream` is later reset and re-read (as happens in
the `Wav.Music` streaming pattern).

Pre-existing LibGDX bug, faithfully ported.

**Severity:** Medium — causes stream position corruption; likely contributes to issue 22.1.

**Fix:** Same as 22.1 — use `Math.min(buffer.length - offset, dataRemaining)` as the
read length parameter, not a post-hoc cap.

### 22.3 WavInputStream — Missing read(buffer, offset, length) override

**File:** `sge/src/main/scala/sge/audio/WavInputStream.scala`

Only `read(buffer: Array[Byte]): Int` is overridden. The 3-argument form
`read(buffer: Array[Byte], offset: Int, length: Int): Int` is inherited from
`FilterInputStream`, which reads directly from the underlying stream without consulting
or decrementing `dataRemaining`.

Any caller using the 3-argument form bypasses data boundary tracking entirely, potentially
reading past the `data` chunk. This is the same omission as in LibGDX's
`Wav.WavInputStream`.

**Severity:** Medium — callers using the wrong `read` overload silently corrupt state.

### 22.4 WebAudioSound — SoundId Int key truncation and overflow

**File:** `sge/src/main/scalajs/sge/audio/WebAudioSound.scala` (lines 45–46, 68–69, 153–154)

`WebAudioSound` uses an `Int` counter (`nextKey`) for sound instance keys, but `SoundId`
wraps `Long`. Two issues:

1. **Truncation**: All per-instance methods (`stop`, `pause`, `resume`, `setPitch`, etc.)
   convert `SoundId` to `Int` via `soundId.toLong.toInt`. If a `SoundId` from a different
   source contains a value > `Int.MaxValue`, it silently truncates to the wrong key.

2. **Overflow to failure sentinel collision**: After `Int.MaxValue + 2` plays (~2.15
   billion), `nextKey` wraps through negative values and eventually reaches `-1`.
   `playInternal` then returns `SoundId(-1L)`, which is the documented failure sentinel.
   The caller would interpret a successful play as a failure, and subsequent operations
   on this ID would collide with the actual failure value.

**Severity:** Low (overflow requires ~2 billion plays) but architecturally unsound. The
Int-to-Long mapping is a latent truncation risk across the entire web audio backend.

**Fix:** Use `Long` for `nextKey`, or add a `SoundId.invalid` constant and avoid -1 in
the key space.

### 22.5 SoundId — No failure sentinel constant

**File:** `sge/src/main/scala/sge/audio/SoundId.scala`

`Sound.play()` and `Sound.loop()` document returning `-1` on failure. But `SoundId` has
no `invalid` constant or `isValid` method:

```scala
opaque type SoundId = Long
object SoundId {
  def apply(value: Long): SoundId = value
  extension (soundId: SoundId) {
    inline def toLong: Long = soundId
  }
}
```

Users must write `soundId.toLong == -1L` to check for failure, defeating the purpose of
the opaque type. Every backend manually constructs `SoundId(-1L)` for failures.

**Severity:** Low — API usability gap, not a runtime bug.

**Fix:** Add `val invalid: SoundId = -1L` and
`extension (soundId: SoundId) def isValid: Boolean = soundId != -1L`.

### 22.6 NoopMusic.setPan — Ignores volume parameter, inconsistent with state tracking

**File:** `sge/src/main/scala/sge/noop/NoopMusic.scala` (line 47)

`NoopMusic` tracks state for `looping`, `volume`, and `position` (an enhancement over
Java's `MockMusic` which tracks nothing). However, `setPan` is a complete no-op:

```scala
override def setPan(pan: Pan, volume: Volume): Unit = {}
```

This is inconsistent: if code calls `setPan(Pan.center, Volume.unsafeMake(0.8f))` and
then reads `music.volume`, it gets the old value. In a real backend, `setPan` updates both
pan and volume. The Java `MockMusic` is consistently stateless (tracks nothing), but SGE's
`NoopMusic` is inconsistently stateful.

**Severity:** Low — only affects testing scenarios that use `NoopMusic` for state
verification.

**Fix:** Update `_volume` in `setPan`:
```scala
override def setPan(pan: Pan, volume: Volume): Unit = _volume = volume
```

### 22.7 Audio trait — Missing Closeable, asymmetric with sub-interfaces

**File:** `sge/src/main/scala/sge/Audio.scala`

LibGDX `Audio extends Disposable` with a `dispose()` method for cleaning up the audio
subsystem (releasing OpenAL contexts, stopping all sounds, etc.). SGE's `Audio` trait
does **not** extend `java.io.Closeable`:

```scala
trait Audio {  // no Closeable
  def newAudioDevice(...): AudioDevice
  def newSound(...): Sound
  // ...
}
```

While documented as intentional ("backends manage lifecycle"), this creates asymmetry:
- `Sound`, `Music`, `AudioDevice`, `AudioRecorder` all extend `Closeable`
- `Audio` itself does not
- `BrowserAudio` adds `AutoCloseable` back, but the base `Audio` trait lacks it
- `NoopAudio` has no lifecycle method at all

Backend implementations must cast to their concrete type for cleanup, preventing
polymorphic resource management.

**Severity:** Low — design decision with documented rationale, but creates API
inconsistency across the audio hierarchy.

### 22.8 WebAudioMusic.close() — Does not remove "ended" event listener

**File:** `sge/src/main/scalajs/sge/audio/WebAudioMusic.scala` (lines 48–53, 88–91)

The constructor adds an `"ended"` event listener to the `HTMLAudioElement`:

```scala
audioElement.addEventListener("ended", { (_: dom.Event) =>
  completionListener.foreach(_(this))
}: js.Function1[dom.Event, Unit])
```

But `close()` only pauses and frees the audio graph:

```scala
override def close(): Unit = {
  audioElement.pause()
  audioControlGraphPool.free(audioControlGraph)
}
```

The event listener is never removed. If the `audioElement` is reused or the garbage
collector delays cleanup, the stale listener can fire the completion callback on a
disposed `Music` instance, potentially causing use-after-free of the freed
`AudioControlGraph`.

**Severity:** Medium — resource leak and potential callback on disposed object.

**Fix:** Store the listener function reference and call
`audioElement.removeEventListener("ended", listener)` in `close()`.

### 22.9 WebAudioSound.setAudioBuffer — Pause/resume race drops sounds

**File:** `sge/src/main/scalajs/sge/audio/WebAudioSound.scala` (lines 49–57)

When `setAudioBuffer` is called (asynchronously after `decodeAudioData` completes), it
iterates `activeSounds.keys`, pauses each, then resumes from offset 0:

```scala
def setAudioBuffer(buffer: js.Dynamic): Unit = {
  audioBuffer = buffer
  val keys = activeSounds.keys.toArray
  keys.foreach { key =>
    pause(SoundId(key.toLong))
    resumeFrom(key, Nullable(0f))
  }
}
```

`pause` calls `stopSource`, which fires the DOM `onended` event **synchronously**. The
`onended` handler calls `soundDone(key)`, which removes the key from both `activeSounds`
and `activeGraphs`. Then `resumeFrom` looks up the key in `activeSounds` — but
`soundDone` already removed it. The `foreach` in `resumeFrom` finds nothing and the
sound is silently lost.

**Severity:** Medium — sounds started before audio data is decoded are dropped instead of
resuming with the real buffer.

**Fix:** In `setAudioBuffer`, remove the entry from `activeSounds` before calling
`stopSource` to prevent `soundDone` from interfering, then re-insert the new source.

### 22.10 DefaultBrowserAudio.switchOutputDevice — Always returns false

**File:** `sge/src/main/scalajs/sge/audio/DefaultBrowserAudio.scala` (line 41)

```scala
override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean = false
```

`WebAudioManager` has a `setSinkId(sinkId: String)` method that calls
`audioContext.setSinkId(sinkId)` when the API is available (modern Chrome/Edge).
`DefaultBrowserAudio` never delegates to this method, so output device switching is
non-functional on the web backend despite the underlying capability existing.

**Severity:** Low — output device switching is a desktop-oriented feature, but the web
backend reports failure even when the browser API is available.

### 22.11 AndroidAudioDeviceAdapter.latency — Hardcoded to 0

**File:** `sge/src/main/scalajvm/sge/AndroidApplication.scala` (line 418)

```scala
override def latency: Int = 0 // Not available through AudioDeviceOps currently
```

The `AudioDevice.latency` contract returns "the latency in samples." The Android adapter
always returns 0 because `AudioDeviceOps` does not expose a latency method. Applications
relying on accurate latency for audio synchronization will miscalculate on Android.

**Severity:** Low — functionality gap in the Android platform abstraction.

### 22.12 DesktopAudioRecorder.read — No bounds checking on offset + numSamples

**File:** `sge/src/main/scalajvm/sge/audio/DesktopAudioRecorder.scala` (lines 49–63)

No validation that `offset + numSamples <= samples.length`. If the caller passes an
offset/numSamples combination that exceeds the array bounds, an
`ArrayIndexOutOfBoundsException` is thrown deep in the byte-to-short conversion loop,
with no helpful error message. Same issue exists in the LibGDX original.

**Severity:** Low — caller responsibility per API contract, but poor error diagnostics.

### 22.13 WebAudioManager — Null audioContext propagated to constructors

**File:** `sge/src/main/scalajs/sge/audio/WebAudioManager.scala` (lines 33–54)

When Web Audio API is unavailable, `audioContext` is set to `null`. `createSound` and
`createMusic` pass this null context to `WebAudioSound` and `WebAudioMusic` constructors
without checking, leading to NPE when those constructors access `audioContext.state` or
`audioContext.createMediaElementSource(...)`.

**Severity:** Low — only affects browsers without Web Audio API (very rare in 2026), but
the crash path has no graceful fallback or error message.

### 22.14 Audio subsystem test coverage gaps

Existing test suites provide good coverage for core abstractions:
- `WavInputStreamTest` — 6 tests (header parsing, data reading, error handling)
- `AudioUtilsTest` — 5 tests (pan-to-stereo conversion)
- `NoopAudioTest` — 11 tests (all noop implementations)
- `AndroidAudioAdapterTest` — 19 tests (adapter delegation)

**Missing test coverage:**
- No test for `WavInputStream.read()` with buffer larger than `dataRemaining` (would
  expose the infinite loop in 22.1)
- No test for WAV files with metadata chunks after the `data` chunk
- No test for `WavInputStream` with MP3 codec type (0x0055) or IEEE float (0x0003)
- No tests for `WebAudioSound`, `WebAudioMusic`, `WebAudioManager` (Scala.js-only)
- No tests for `DesktopAudioRecorder` (requires audio hardware)
- No integration tests for Audio -> Sound/Music create-play-dispose lifecycle
- Opaque type validation (`Volume.parse`, `Pitch.parse`, `Pan.parse`, `Position.parse`)
  untested

**Severity:** Medium — the most dangerous bug (22.1) has no test that would catch it.

### 22.15 WavInputStream.seekToChunk — Fragile partial-EOF detection

**File:** `sge/src/main/scala/sge/audio/WavInputStream.scala` (lines 98–112)

The EOF check `if (chunkLength == -1)` only catches EOF when all four length bytes
return -1 from `read()`. Each `read()` returning -1 is masked via `& 0xff` to produce
0xff (255). The combined 32-bit value `0xff | (0xff << 8) | (0xff << 16) | (0xff << 24)`
= `0xffffffff` = `-1` in two's complement — so the check passes.

However, if the stream reaches EOF *partway through* the 4-byte length field (e.g., only
the last 1–3 reads return -1), the mask converts those -1s to 0xff while the earlier
bytes contain real data. The result is a large but not-quite-`-1` garbage value (e.g.,
if the first byte is 0x10 and the remaining three are EOF: `0xff_ff_ff_10`). The
`chunkLength == -1` check fails, and `skipFully` attempts to skip ~4 billion bytes,
eventually throwing an `EOFException` with a misleading "Unable to skip" message.

The same issue exists in the chunk ID reads (`read() == c1`, etc.) — a partial-EOF
during the ID bytes silently miscompares without raising an error.

This is a pre-existing LibGDX bug, faithfully ported. It does not cause silent data
corruption (the stream eventually fails), but the error message is unhelpful for
diagnosing truncated WAV files.

**Severity:** Low — fails loudly (just with a misleading error), not silently.

### 22.16 WavInputStream.seekToChunk — Does not handle RIFF chunk padding

**File:** `sge/src/main/scala/sge/audio/WavInputStream.scala` (line 109)

Per the RIFF specification (AVI RIFF file format), chunks with an odd `chunkLength`
must be followed by a single padding byte to maintain 2-byte alignment. The
`seekToChunk` method skips exactly `chunkLength` bytes when the chunk ID does not match:

```scala
skipFully(chunkLength)
```

If a non-matching chunk has an odd length, the parser is now off by one byte. The next
iteration reads the padding byte as the first byte of the next chunk's ID, causing a
cascade of misalignment. Depending on the byte values, this eventually results in either
a misleading "Chunk not found" IOException or parsing garbage as chunk lengths.

This manifests with WAV files that have metadata chunks (e.g., `LIST`, `fact`, `bext`)
with odd-length content appearing before the `fmt ` or `data` chunks.

Pre-existing LibGDX bug, faithfully ported.

**Severity:** Medium — causes parse failure on spec-compliant WAV files with odd-length
metadata chunks. The fix is `skipFully(chunkLength + (chunkLength & 1))` (or equivalently
`skipFully((chunkLength + 1) & ~1)` to round up to the next 2-byte boundary).

### 22.17 NoopMusic — Default volume (0) differs from real backend defaults

**File:** `sge/src/main/scala/sge/noop/NoopMusic.scala` (line 24)

```scala
private var _volume: Volume = Volume.min  // 0.0f
```

`NoopMusic` initializes volume to `Volume.min` (0.0f), consistent with LibGDX's
`MockMusic` (which returns 0 from `getVolume()`). However, real backend implementations
initialize volume to 1.0f (e.g., `OpenALMusic` has `protected float volume = 1`).

Test code using `NoopMusic` to verify default state will see `volume == 0.0f`, while
production code running on any real backend sees `volume == 1.0f`. This can mask bugs
where code assumes a non-zero default volume without explicitly setting it.

**Severity:** Low — test fidelity issue, not a runtime bug. Matches LibGDX's MockMusic
behavior, so this is a faithful port of a pre-existing design inconsistency.

**Fix:** Initialize `_volume` to `Volume.max` (1.0f) to match real backend defaults.

## 21. Core Lifecycle and Context Issues

This section catalogs bugs and design issues in the core application lifecycle
(`Sge`, `SgeAware`, `Application`, `Game`, `Screen`, `ApplicationListener`,
`InputMultiplexer`, `InputEventQueue`) — particularly around `(using Sge)` propagation,
lifecycle ordering, and behavioral differences from LibGDX.

### 21.1 BrowserApplication — stale Sge context captured by DefaultBrowserInput

**Files:** `sge/src/main/scalajs/sge/BrowserApplication.scala` (lines 103–118),
`sge/src/main/scalajs/sge/input/DefaultBrowserInput.scala` (line 37)

`BrowserApplication.start()` creates the Sge context in three stages:

1. **First Sge** (line 105): `input = placeholderInput` where `placeholderInput` is
   `Nullable.empty[Input].orNull` — i.e. **null**.
2. `DefaultBrowserInput` is constructed with `given Sge = _sge` (lines 115–118), capturing
   the **first** Sge (with null input) as its context parameter.
3. **Second Sge** (line 121): rebuilt with real input. **Third Sge** (line 134): optionally
   rebuilt with real audio.

Since `Sge` is an immutable `final case class`, `DefaultBrowserInput` permanently holds a
reference to the first Sge. Any code inside `DefaultBrowserInput` that calls `Sge().input`
gets **null**. This includes event handlers that dispatch to `InputProcessor` callbacks — if
a processor accesses `Sge().input`, it sees null.

Concretely, `DefaultBrowserInput.handleDocMouseDown` (line 324) calls
`Sge().graphics.getWidth()`, which works because `graphics` is valid in the first Sge. But
any callback that chains through to `Sge().input` (e.g., from a UI framework InputProcessor)
would fail.

**Severity:** Medium — latent NPE in browser input path. Works in practice only because
most InputProcessor implementations don't access `Sge().input` from within callbacks.

**Fix:** Either (a) make `DefaultBrowserInput` not require `(using Sge)` and instead accept
the canvas/config directly, or (b) restructure `BrowserApplication.start()` to create input
before Sge, or (c) use a by-name / lazy reference pattern so `Sge()` always resolves to the
latest context.

### 21.2 DesktopApplication — immutable Sge context breaks multi-window rendering

**Files:** `sge/src/main/scaladesktop/sge/DesktopApplication.scala` (lines 120–127, 142),
`sge/src/main/scaladesktop/sge/DesktopWindow.scala` (lines 289, 326)

The Sge context is built once at initialization with the main window's graphics and input:

```scala
_sge = Sge(
  application = this,
  graphics = mainWindow.getGraphics(),  // fixed to main window
  input = mainWindow.getInput(),        // fixed to main window
  ...
)
```

In the main loop, `loop()(using Sge)` passes this same immutable Sge to all windows.
When `DesktopWindow.update()(using sge: Sge)` calls `listener.render()` for a secondary
window, the listener's `Sge().graphics` and `Sge().input` still point to the **main
window's** subsystems.

In contrast, the Java `Lwjgl3Application` updates `Gdx.graphics` and `Gdx.input` per
window each frame:

```java
Gdx.graphics = window.getGraphics();
Gdx.input = window.getInput();
```

`DesktopApplication.getGraphics()` correctly delegates to `_currentWindow`, so
`Sge().application.getGraphics()` returns the right window's graphics. But the direct
path `Sge().graphics` (used by 150+ files) always returns the main window's.

**Severity:** High for multi-window applications — renders to wrong GL context. Low for
single-window applications (the common case).

**Fix:** The Sge context should either (a) be rebuilt per-window each frame (expensive),
(b) delegate `graphics`/`input` through the Application trait (breaking the direct-field
pattern), or (c) use a mutable holder behind the immutable facade.

### 21.3 Sge immutability — fundamental tension with the mutable Gdx.* global pattern

**Files:** `sge/src/main/scala/sge/Sge.scala`

LibGDX's `Gdx` class has mutable static fields (`Gdx.graphics`, `Gdx.input`, etc.) that
backends update dynamically. SGE replaced this with an immutable `final case class Sge`
passed as a context parameter.

This works correctly in single-window scenarios, where the Sge context is created once and
never changes. But the immutability assumption breaks in two cases:

1. **Multi-window** (21.2): `graphics` and `input` must change per-window per-frame.
2. **BrowserApplication bootstrap** (21.1): subsystems are created incrementally, requiring
   multiple Sge reconstructions.
3. **GL profiling** (`GLProfiler.scala`): In LibGDX, `GLProfiler.enable()` replaces
   `Gdx.gl20`/`Gdx.gl30` with profiling wrappers. In SGE, this can't work because
   `Sge().graphics.gl20` is fixed at Sge creation time (the Graphics trait has mutable
   internal state, so this specific case actually works through `Graphics.setGL20()`, but
   it illustrates the design tension).

**Severity:** Architectural — affects multi-window correctness and any future feature that
requires dynamic subsystem swapping.

### 21.4 InputMultiplexer — missing getProcessors() method

**Files:** `sge/src/main/scala/sge/InputMultiplexer.scala`

LibGDX's `InputMultiplexer` has a `getProcessors()` method (line 75) returning the
`SnapshotArray<InputProcessor>`:

```java
public SnapshotArray<InputProcessor> getProcessors() {
    return processors;
}
```

SGE's `InputMultiplexer` exposes `val processors = DynamicArray[InputProcessor]()` as a
public field but omits the `getProcessors()` method. Any code that calls
`multiplexer.getProcessors()` will fail to compile.

**Severity:** Low — the field is public so the data is accessible, but API compatibility
with LibGDX-style code is broken.

### 21.5 InputMultiplexer — missing null-safety checks on addProcessor

**Files:** `sge/src/main/scala/sge/InputMultiplexer.scala` (lines 31, 37)

LibGDX's `addProcessor` methods throw `NullPointerException` if the processor is null:

```java
public void addProcessor(InputProcessor processor) {
    if (processor == null) throw new NullPointerException("processor cannot be null");
    processors.add(processor);
}
```

SGE's version has no validation. While SGE avoids null in general via `Nullable[A]`, the
`InputProcessor` parameter is not wrapped in `Nullable`, so a null value could be passed
(especially at Java interop boundaries). A null processor in the list would cause an NPE
during `processEvent` iteration with a confusing stack trace.

**Severity:** Low — defensive programming gap. The `Nullable` convention makes this unlikely
but not impossible.

### 21.6 InputMultiplexer — removeProcessor(Int) return type changed from InputProcessor to Unit

**Files:** `sge/src/main/scala/sge/InputMultiplexer.scala` (line 34)

LibGDX:

```java
public InputProcessor removeProcessor(int index) {
    return processors.removeIndex(index);
}
```

SGE:

```scala
def removeProcessor(index: Int): Unit =
  processors.removeIndex(index)
```

The return value (the removed processor) is silently discarded. Any code that depends on
the return value to, e.g., dispose or re-add the removed processor would silently fail.

**Severity:** Low — API behavioral difference; unlikely to cause bugs unless user code
chains `removeProcessor` return values.

### 21.7 InputMultiplexer — processors field is public val (was private in LibGDX)

**Files:** `sge/src/main/scala/sge/InputMultiplexer.scala` (line 24)

LibGDX:

```java
private SnapshotArray<InputProcessor> processors = new SnapshotArray(4);
```

SGE:

```scala
val processors = DynamicArray[InputProcessor]()
```

The `processors` collection is fully public and mutable. This allows external code to
directly mutate the processor list (e.g., `multiplexer.processors.clear()`) bypassing any
future encapsulation or validation. LibGDX kept this field private and exposed controlled
accessors (`addProcessor`, `removeProcessor`, `getProcessors`).

**Severity:** Low — design/encapsulation issue, not a runtime bug.

### 21.8 InputEventQueue — missing touchCancelled event support

**Files:** `sge/src/main/scala/sge/InputEventQueue.scala`

`InputProcessor` defines a `touchCancelled` method, but `InputEventQueue` has no
corresponding event type constant or enqueue method. This means `touchCancelled` events
cannot be queued and are either dropped or must be delivered synchronously.

This is **consistent with LibGDX** — the Java `InputEventQueue` also lacks `touchCancelled`.
However, since SGE added `touchCancelled` to `InputProcessor` (matching a later LibGDX
addition), the queue should arguably support it for completeness.

**Severity:** Low — consistent with Java, but a potential event delivery gap on platforms
(Android, iOS) where touch cancellation occurs.

### 21.9 Game — _screen visibility narrowed from protected to private

**Files:** `sge/src/main/scala/sge/Game.scala` (line 22)

LibGDX:

```java
protected Screen screen;
```

SGE:

```scala
private var _screen: Nullable[Screen] = Nullable.empty
```

The backing field is `private` with a public getter (`def screen`) and setter
(`def screen_=`). In Java, subclasses could access `this.screen` directly; in SGE,
they must use the property. The functional behavior is equivalent since the Java getter
`getScreen()` is a trivial field read and the Java setter `setScreen()` had the same
hide/show/resize logic now in `screen_=`.

**Severity:** None — this is a correct idiomatic Scala conversion. Noted for completeness.

### 21.10 Application trait — missing logging API (intentional removal)

**Files:** `sge/src/main/scala/sge/Application.scala`

LibGDX's `Application` interface defines 8 logging methods and 4 log-level constants:

```java
void log(String tag, String message);
void log(String tag, String message, Throwable exception);
void error(String tag, String message);
void error(String tag, String message, Throwable exception);
void debug(String tag, String message);
void debug(String tag, String message, Throwable exception);
void setLogLevel(int logLevel);
int getLogLevel();
```

And 4 static constants:

```java
public static final int LOG_NONE = 0;
public static final int LOG_DEBUG = 3;
public static final int LOG_INFO = 2;
public static final int LOG_ERROR = 1;
```

SGE's `Application` trait omits all of these. The migration notes state "logging decoupled
to scribe library". Any LibGDX code calling `Gdx.app.log(...)` or `Gdx.app.error(...)`
must be migrated to use scribe.

**Severity:** None for new code; affects API compatibility for ported LibGDX extensions.

### 21.11 Application.ApplicationType — enum doesn't extend java.lang.Enum

**Files:** `sge/src/main/scala/sge/Application.scala` (lines 133–135)

```scala
enum ApplicationType {
  case Android, Desktop, HeadlessDesktop, Applet, WebGL, iOS
}
```

Per CLAUDE.md conventions: "No `scala.Enumeration`: use Scala 3 `enum`, preferably
`extends java.lang.Enum`". This enum does not extend `java.lang.Enum[ApplicationType]`.
While "preferably" means it's not mandatory, extending `java.lang.Enum` provides:
- `ordinal()` and `name()` for free
- Java interop compatibility
- Stable serialization

**Severity:** Low — style/convention issue. May matter for Java interop or serialization.

### 21.12 DesktopWindow.initializeListener — SgeAware + create + resize ordering is correct

**Files:** `sge/src/main/scaladesktop/sge/DesktopWindow.scala` (lines 336–345)

The initialization sequence is:

1. `SgeAware.sgeAvailable(sge)` — if listener implements SgeAware
2. `listener.create()`
3. `listener.resize(width, height)`

This matches the expected lifecycle and is consistent across all three backends
(DesktopWindow, BrowserApplication, HeadlessApplication). HeadlessApplication omits the
`resize()` call, matching the Java HeadlessApplication behavior (headless has no window
dimensions to report).

**Severity:** None — correct implementation.

### 21.13 Screen transition safety — setScreen during render is safe

**Files:** `sge/src/main/scala/sge/Game.scala` (lines 31–38, 49–50)

When `screen_=` is called during `render()` (e.g., from within `Screen.render(delta)`):

```scala
def screen_=(newScreen: Nullable[Screen]): Unit = {
  _screen.foreach(_.hide())
  _screen = newScreen
  _screen.foreach { s =>
    s.show()
    s.resize(...)
  }
}

override def render(): Unit =
  _screen.foreach(_.render(Sge().graphics.getDeltaTime()))
```

If `screen_=` is called from within `_screen.foreach(_.render(...))`, the `_screen`
reference changes mid-iteration. However, since `Nullable.foreach` is not an iterator
over a collection — it's a single-value operation — there's no concurrent modification.
The `render()` call completes on the old screen, then `screen_=` runs fully. The next
frame calls `render()` on the new screen. This is safe and matches LibGDX behavior.

**Severity:** None — correct behavior, same as Java.

### 21.14 HeadlessApplication — missing resize() call after create()

**Files:** `sge/src/main/scaladesktop/sge/HeadlessApplication.scala` (lines 72–76)

```scala
listener match {
  case aware: SgeAware => aware.sgeAvailable(sgeContext)
  case _ => ()
}
listener.create()
// No listener.resize() call here
```

Both `DesktopWindow.initializeListener` and `BrowserApplication.start()` call
`listener.resize(width, height)` after `create()`. HeadlessApplication does not.

This matches the Java `HeadlessApplication` behavior (Java's version also omits resize
after create). Since `NoopGraphics` reports 0x0 dimensions, calling resize(0, 0) would
be meaningless. However, this means `ApplicationListener.resize()` is never called in
headless mode, which could cause issues for listeners that initialize viewport/camera
state in `resize()`.

**Severity:** Low — matches Java, but headless test code that depends on resize being
called after create will behave differently from desktop/browser.

### 21.15 BrowserApplication.postRunnable — not synchronized (correct for JS)

**Files:** `sge/src/main/scalajs/sge/BrowserApplication.scala` (line 371)

```scala
override def postRunnable(runnable: Runnable): Unit = runnables += runnable
```

Unlike `DesktopApplication.postRunnable` which synchronizes on the runnables list, the
browser version does no synchronization. This is correct because JavaScript is
single-threaded — `requestAnimationFrame` callbacks and DOM event handlers cannot
interleave.

**Severity:** None — correct for the platform.

### 21.16 Game()(using Sge) — chicken-and-egg problem makes Game unusable

**Files:** `sge/src/main/scala/sge/Game.scala` (line 21)

```scala
abstract class Game()(using Sge) extends ApplicationListener {
```

`Game` requires `(using Sge)` at construction time. But the standard Application
bootstrapping pattern creates the ApplicationListener FIRST, then passes it to the
Application constructor, which creates the Sge context later:

```scala
// Typical LibGDX pattern:
object Main {
  def main(args: Array[String]): Unit = {
    val listener = new MyGame()          // needs (using Sge) — doesn't exist yet!
    DesktopApplicationFactory(listener, config)  // creates Sge internally
  }
}
```

The `SgeAware` trait was designed exactly for this gap — it lets listeners receive Sge
after construction but before `create()`. However, `Game` does not implement `SgeAware`;
it demands Sge at construction via the class constructor parameter.

**Evidence:** No code in the entire codebase extends `Game`. A grep for `extends Game`
returns zero results in SGE source, suggesting the class is ported but currently unusable
with the standard application lifecycle.

In LibGDX, `Game` has no such requirement — it accesses `Gdx.graphics` etc. at runtime
through the mutable static fields, so construction never needs the context.

**Severity:** High — `Game` is a core API class for multi-screen applications and is
currently broken. Subclasses cannot be instantiated in the standard bootstrapping pattern.

**Fix:** Either (a) remove `(using Sge)` from Game's constructor and implement `SgeAware`
to receive Sge lazily, storing it in a private field and providing it as a `given` in
method bodies, or (b) use a `lazy val` / by-name pattern so the Sge reference is resolved
at first use rather than construction.

### 21.17 InputEventQueue.drain() and next() — toArray allocates on every call

**Files:** `sge/src/main/scala/sge/InputEventQueue.scala` (lines 42, 88)

```scala
// In drain():
val q = processingQueue.toArray   // allocates fresh array every frame

// In next():
val q = queue.toArray             // allocates fresh array per call
```

LibGDX uses `processingQueue.items` and `queue.items` which return the internal backing
array directly (zero allocation). SGE uses `DynamicArray.toArray` which calls
`copyOfRange(_items, 0, _size)`, creating a new array.

`next()` is called multiple times per `touchDragged()` and `mouseMoved()` event (once per
existing queued event of the same type), so the allocation cost compounds. `drain()` is
called once per frame on the render thread, creating a per-frame allocation.

In a game running at 60fps with active mouse movement, this produces at minimum 60
array allocations per second from `drain()` alone, plus additional allocations from
`next()` during event coalescing.

**Severity:** Low-Medium — no correctness issue, but creates unnecessary GC pressure in a
hot path that the Java version avoids entirely. The Java pattern of using the internal
`items` array with a captured `size` bound is both safe and allocation-free in this
context (single-threaded drain, synchronized next).

**Fix:** Add a `def items: Array[A]` accessor to `DynamicArray` that returns the backing
array directly (matching Java's `IntArray.items` public field), or use `begin()`/`end()`
snapshot iteration which also returns the backing array.

### 21.18 InputEventQueue.drain() — boundary/break thrown from inside synchronized block

**Files:** `sge/src/main/scala/sge/InputEventQueue.scala` (lines 29–39)

```scala
def drain(processor: Nullable[InputProcessor]): Unit = scala.util.boundary {
  synchronized {
    processor.fold {
      queue.clear()
      scala.util.boundary.break()   // throws Break from inside synchronized
    } { proc =>
      processingQueue.clear()
      processingQueue.addAll(queue: DynamicArray[Int])
      queue.clear()
    }
  }
  // ... processing continues outside synchronized
}
```

The `scala.util.boundary.break()` call in the null-processor path throws a `Break`
exception from within a `synchronized` block. While JVM monitors are correctly released
when an exception propagates through a synchronized region, this pattern mixes structured
concurrency primitives (`boundary`/`break`) with synchronization in a way that is
non-obvious and fragile.

In the Java version, this is simply `return` inside `synchronized`, which is well-understood:
```java
synchronized (this) {
    if (processor == null) {
        queue.clear();
        return;
    }
```

**Severity:** Low — correct behavior, but the `break()` throwing through `synchronized` is
a maintenance hazard. A future refactor adding try/finally around the synchronized block
could interact unexpectedly with the non-local `break()`.

### 21.19 SgeAware implementations use scala.compiletime.uninitialized (effectively null)

**Files:** `demo/src/main/scala/sge/demo/DemoApp.scala` (line 21),
`sge-android-smoke/src/main/scala/sge/smoke/SmokeListener.scala` (line 23),
`sge-it-tests/desktop/src/main/scala/sge/it/desktop/DesktopHarness.scala` (line 21),
`demos/shared/src/main/scala/sge/demos/shared/SingleSceneApp.scala` (line 17)

All `SgeAware` implementations follow the same pattern:

```scala
class MyListener extends ApplicationListener with SgeAware {
  private var sge: Sge = scala.compiletime.uninitialized  // effectively null

  override def sgeAvailable(sge: Sge): Unit =
    this.sge = sge
}
```

`scala.compiletime.uninitialized` initializes the field to `null` at runtime. This
violates the codebase's "no null" rule. If `sgeAvailable` is not called before `create()`
(e.g., a backend doesn't check for `SgeAware`), the field is null and any access crashes.

`DemoApp` even includes a null check with early return as a guard:
```scala
override def create(): Unit = {
    if (sge == null) return  // violates both no-null and no-return rules
```

This is a structural problem with the `SgeAware` pattern: there is no way to represent
"Sge not yet available" without using null or `Nullable[Sge]`, and using `Nullable`
would require `.foreach`/`.fold` on every access in every lifecycle method.

**Severity:** Medium — the pattern works in practice (all backends call `sgeAvailable`
before `create`), but violates core codebase invariants and has no compile-time safety
net. If a new backend or test harness forgets the `SgeAware` check, it produces an NPE
with no helpful error message.

### 21.20 Screen extends AutoCloseable but Game.dispose() never calls close()

**Files:** `sge/src/main/scala/sge/Screen.scala` (line 20),
`sge/src/main/scala/sge/Game.scala` (line 40)

```scala
// Screen.scala
trait Screen extends AutoCloseable {
  def close(): Unit   // replaces LibGDX's dispose()
}

// Game.scala
override def dispose(): Unit =
  _screen.foreach(_.hide())   // only calls hide(), never close()
```

LibGDX's `Screen` extends `Disposable` and `Game.dispose()` similarly only calls
`screen.hide()`, not `screen.dispose()`. This is documented behavior: "Screens are
not disposed automatically."

However, SGE changed `Screen` to extend `AutoCloseable`, which carries a stronger
API contract in the JVM ecosystem. `AutoCloseable` types are expected to be used with
try-with-resources (Java) or `Using` (Scala), where `close()` is called automatically.
A Screen that is never `close()`d violates this contract and may cause resource leaks
that tools like linters or IDE inspections would flag.

The `screen_=` setter also only calls `hide()` on the old screen, never `close()`.

**Severity:** Low — matches LibGDX's deliberate design ("dispose screens yourself"), but
the `AutoCloseable` supertype creates misleading expectations. Code analysis tools may
report unclosed `Screen` instances as potential resource leaks.

### 21.21 InputEventQueue — currentEventTime read is not thread-safe for 32-bit JVMs

**Files:** `sge/src/main/scala/sge/InputEventQueue.scala` (lines 27, 217)

```scala
private var _currentEventTime: Nanos = Nanos.zero    // written in drain()

def currentEventTime: Nanos = _currentEventTime       // read from any thread
```

`_currentEventTime` is a `Nanos` (opaque type wrapping `Long`). It is written during
`drain()` (on the render thread, outside `synchronized`) and can be read from any thread
via the public `currentEventTime` accessor.

On 32-bit JVMs, `Long` reads and writes are not atomic — they consist of two 32-bit
operations. A concurrent read during a write could observe a "torn" value (high bits from
one write, low bits from another). This would produce a nonsensical timestamp.

LibGDX has the identical issue (`currentEventTime` is a plain `long` field with no
synchronization or `volatile`). This is a faithfully ported limitation.

**Severity:** Very Low — 32-bit JVMs are rare in modern usage, and `currentEventTime` is
typically only read from the render thread (same thread as `drain()`). But it's
technically a data race per the Java Memory Model.

## 23. 3D Rendering Pipeline Issues

### 23.1 DefaultShader.scala — Stray `Matrix3()` expression (orphan statement)

**Lines 345, 423**

```scala
Matrix3()     // line 345 — immediately after init()
// ...
Vector3()     // line 423 — immediately before bindLights()
```

In the Java original (`DefaultShader.java` line 779), the field is declared as:
```java
private final Matrix3 normalMatrix = new Matrix3();
```

The Scala port has `Matrix3()` as a bare statement (not assigned to any field). This allocates
and discards a `Matrix3` each time the class is instantiated. While harmless at runtime, it
means the `normalMatrix` field **does not exist** in the SGE version. The Java `normalMatrix`
field was a private instance member used nowhere in Java either (the actual normal matrix
computation happens in the `Setters.normalMatrix` local setter which uses its own `tmpM`), so
this is functionally benign but represents wasted allocation and a confusing port artifact.

Similarly, `Vector3()` on line 423 is a bare expression (the Java original has
`private final Vector3 tmpV = new Vector3()` which is also unused in `DefaultShader` itself).

**Severity:** Very Low — No functional impact, just wasted allocation and confusing dead code.

### 23.2 DefaultShader.scala — `bindLights` continue-pattern incorrectly structured

**Lines 437–491 (directional lights), 494–549 (point lights), 552–604 (spot lights)**

In the Java original, the `for` loop uses `continue` to skip uniform uploads when the light
hasn't changed:

```java
// Java — clean continue skips the uniform upload
if (dirs == null || i >= dirs.size) {
    if (lightsSet && directionalLights[i].color.r == 0f && ...) continue;
    directionalLights[i].color.set(0, 0, 0, 1);
} else if (lightsSet && directionalLights[i].equals(dirs.get(i)))
    continue;
else
    directionalLights[i].set(dirs.get(i));
// uniform upload always runs here (unless continue was hit)
int idx = dirLightsLoc + i * dirLightsSize;
program.setUniformf(idx + dirLightsColorOffset, ...);
```

In the Scala port, the `continue` is replaced by nested `if/else` blocks. The problem is that
in the "no lights available" branch, when the `lightsSet && color == 0` condition is true, the
code enters an empty block (`// continue - skip this iteration`) but the uniform upload code
for the **clear-to-zero** case is inside the `else` block — this is correct. However, the
structure duplicates the entire uniform upload code inside **both** the "set to zero" branch
and the "set from source" branch, making the code ~3x as long as needed. More critically:

When `lightsSet` is true and the light matches (`directionalLights(i).equals(dirs.exists(...))`),
the `continue` equivalent skips the uniform upload — **this is correct behavior**. But the
`dirs.exists(d => directionalLights(i).equals(d(i)))` uses `exists` instead of `forall`, which
means it checks if *any* dir array satisfies the condition. Since `dirs` is
`Nullable[DynamicArray[DirectionalLight]]`, the `.exists()` operates on the `Nullable` wrapper
(checking if the contained value satisfies the predicate), which is semantically correct —
it's "if dirs is defined AND `directionalLights(i).equals(dirs(i))`". So the logic is
actually correct, just harder to read.

**Severity:** Low — The logic is functionally equivalent but significantly harder to maintain.
The triplication of uniform-upload code makes it easy to introduce bugs during future edits.

### 23.3 DefaultShader.scala — `canRender` bones check differs from Java

**Lines 624–648**

The Java original checks bones first without duplicating the rest:
```java
if (renderable.bones != null) {
    if (renderable.bones.length > config.numBones) return false;
    if (...getBoneWeights() > config.numBoneWeights) return false;
}
if (...getTextureCoordinates() != textureCoordinates) return false;
// shared check
```

The Scala version duplicates the entire remaining check body inside both the `bones.isDefined`
and `else` branches:

```scala
if (renderable.bones.isDefined) {
    if (...) false
    else if (...) false
    else {
        if (...getTextureCoordinates() != textureCoordinates) false
        else { /* full check */ }
    }
} else {
    if (...getTextureCoordinates() != textureCoordinates) false
    else { /* duplicated full check */ }
}
```

While functionally correct, the duplicated logic means any future fix to the shared check
must be applied in two places.

**Severity:** Very Low — Code duplication, no functional bug.

### 23.4 DefaultShader.scala — `u_time` set via `setFloat` instead of `set`

**Line 362**

Java: `set(u_time, time += Gdx.graphics.getDeltaTime());`
Scala: `setFloat(u_time, time)`

The Java version uses `set(int, float)` on `BaseShader`, while Scala uses `setFloat`. These
are equivalent (`set(int, float)` was renamed to `setFloat` in the SGE port to avoid overload
ambiguity), so this is correct.

However, the Scala version updates `time` first (`time += ...`) and then passes `time` to
`setFloat`. The Java version does this in a single expression. Both are equivalent.

**Severity:** None — Correct port, just noting the rename.

### 23.5 DefaultRenderableSorter.scala — Sorting distance uses `distanceSq` but cast differs from Java

**Lines 67–69**

Java original:
```java
final float dst = (int)(1000f * camera.position.dst2(tmpV1))
                - (int)(1000f * camera.position.dst2(tmpV2));
```

Scala port:
```scala
val dst = (1000f * cam.position.distanceSq(tmpV1)).toInt
        - (1000f * cam.position.distanceSq(tmpV2)).toInt
```

The Java version casts each multiplication to `int` separately (truncating), then subtracts.
The Scala version does the same — `.toInt` is applied to each term before subtraction. This
is functionally equivalent.

**Severity:** None — Correct port.

### 23.6 Attributes.scala — `attributeOrdering` may produce incorrect results for large type differences

**Line 241**

```scala
given attributeOrdering: Ordering[Attribute] =
  (arg0: Attribute, arg1: Attribute) => (arg0.`type` - arg1.`type`).toInt
```

`Attribute.type` is a `Long`. When two attribute types have values that differ by more than
`Int.MaxValue`, the `.toInt` truncation will produce incorrect comparison results. For example,
if `arg0.type = 0x8000000000000000L` and `arg1.type = 1L`, the difference is a large negative
`Long` which, when cast to `Int`, may produce an incorrect sign.

The Java `Attributes` class uses `Comparator<Attribute>` with the same pattern:
```java
(arg0, arg1) -> (int)(arg0.type - arg1.type)
```

So this is technically a pre-existing bug inherited from LibGDX. In practice, attribute types
are assigned from a counter that starts near 1 and increments, so the difference is always
small. But if custom attributes use high bit positions, this comparator could fail.

**Severity:** Low — Inherited from LibGDX, unlikely to trigger in practice.

### 23.7 BaseAnimationController.scala — `transforms` map is a shared mutable singleton

**Lines 154–155 (companion object)**

```scala
private val transforms: mutable.Map[Node, Transform] = mutable.Map.empty
private val tmpT: Transform = Transform()
```

These are in the **companion object** (static equivalent), meaning they are shared across all
`BaseAnimationController` instances. The Java original also makes these `static`:

```java
private static final ObjectMap<Node, Transform> transforms = new ObjectMap();
private static final Transform tmpT = new Transform();
```

This means using multiple `AnimationController` instances concurrently (e.g., on different
threads) will corrupt the shared `transforms` map and `tmpT` temporary. In LibGDX this was
acceptable since all rendering happens on the GL thread, but it's worth documenting as a
thread-safety concern.

**Severity:** Low — Same as Java original; only matters for multi-threaded animation updates.

### 23.8 ModelBatch.scala — `render(RenderableProvider, Shader)` overwrites shader twice

**Lines 297–307**

```scala
def render(renderableProvider: RenderableProvider, shader: Shader): Unit = {
    val offset = renderables.size
    renderableProvider.getRenderables(renderables, renderablesPool)
    var i = offset
    while (i < renderables.size) {
      val renderable = renderables(i)
      renderable.shader = Nullable(shader)           // first assignment
      renderable.shader = Nullable(shaderProvider.getShader(renderable))  // overwrite!
      i += 1
    }
}
```

The first `renderable.shader = Nullable(shader)` is immediately overwritten by
`renderable.shader = Nullable(shaderProvider.getShader(renderable))`. This means the explicit
shader passed by the user is **ignored** — the `ShaderProvider` selects the shader instead.

Looking at the Java original (`ModelBatch.java`), the same pattern exists:
```java
renderable.shader = shader;
renderable.shader = shaderProvider.getShader(renderable);
```

The intent is that `shaderProvider.getShader(renderable)` may *choose* to respect the
`renderable.shader` hint that was just set. The `DefaultShaderProvider.getShader()` checks if
`renderable.shader` can render the renderable and returns it if so. So the first assignment
sets a "preferred shader" hint, and the provider validates and potentially returns it.

**However**, line 304 sets `renderable.shader` **before** calling `getShader`, so the provider
*does* see the hint. This is actually correct behavior, matching Java.

**Severity:** None — Correct behavior; the double assignment is intentional (set hint, then validate).

### 23.9 ModelBatch.scala — Same double-assignment in `render(RenderableProvider, Environment, Shader)`

**Lines 329–340**

Same pattern as 23.8:
```scala
renderable.shader = Nullable(shader)
renderable.shader = Nullable(shaderProvider.getShader(renderable))
```

Same analysis applies — intentional hint-then-validate pattern.

**Severity:** None — Correct behavior.

### 23.10 Model.scala — `convertMesh` updates all meshParts, not just newly created ones

**Lines 265–267**

```scala
for (part <- meshParts)
  part.update()
```

This loop iterates over **all** `meshParts` accumulated so far, not just the mesh parts created
in this `convertMesh` call. If a model has multiple meshes, each call to `convertMesh` will
redundantly call `update()` on all previously created mesh parts.

The Java original has the same code:
```java
for (int i = 0; i < meshParts.size; i++) {
    meshParts.get(i).update();
}
```

So this is inherited behavior — each mesh triggers a full re-update of all parts. The
redundancy is harmless but wasteful for models with many meshes.

**Severity:** Very Low — Inherited from Java, only a minor performance concern.

### 23.11 ModelInstance.scala — `getAnimation` default `ignoreCase` differs from `Model.getAnimation`

`Model.getAnimation(id)` calls `getAnimation(id, true)` — case-insensitive by default.
`ModelInstance.getAnimation(id)` calls `getAnimation(id, false)` — case-sensitive by default.

The Java originals:
- `Model.getAnimation(String)` calls `getAnimation(id, true)` — i.e., recursive search with
  *case-sensitive* comparison (the second param is `recursive`, not `ignoreCase`).
- `ModelInstance.getAnimation(String)` calls `getAnimation(id, false)` — non-recursive,
  case-sensitive.

Wait — looking more carefully at the Java `Model`:
```java
public Animation getAnimation(String id) {
    return getAnimation(id, true);
}
public Animation getAnimation(String id, boolean ignoreCase) { ... }
```

In Java `Model`, `getAnimation(String)` delegates to `getAnimation(id, true)` where the second
parameter IS `ignoreCase = true`. So the SGE port correctly makes Model's default case-insensitive.

For Java `ModelInstance`:
```java
public Animation getAnimation(String id) {
    return getAnimation(id, false);
}
```
Where `false` is `ignoreCase = false` → case-sensitive. The SGE port matches.

So the asymmetry is **by design** — `Model` defaults to case-insensitive, `ModelInstance`
defaults to case-sensitive. This matches LibGDX exactly.

**Severity:** None — Correct port.

### 23.12 DefaultShader.scala — `bindMaterial` uses `setFloat` for `u_opacity` and `u_alphaTest`

**Lines 401, 405**

In Java:
```java
set(u_opacity, ((BlendingAttribute)attr).opacity);
set(u_alphaTest, ((FloatAttribute)attr).value);
```

In Scala:
```scala
setFloat(u_opacity, ba.opacity)
setFloat(u_alphaTest, attr.asInstanceOf[FloatAttribute].value)
```

`set(int, float)` was renamed to `setFloat(int, float)` in the Scala port. This is correct.

**Severity:** None — Correct rename.

### 23.13 DefaultShader.scala — `environmentCubemap` initialization has redundant check

**Line 71–72**

```scala
protected val environmentCubemap: Boolean =
  _combinedAttributes.has(CubemapAttribute.EnvironmentMap) ||
  (lighting && _combinedAttributes.has(CubemapAttribute.EnvironmentMap))
```

The condition `A || (B && A)` simplifies to just `A`. The `lighting &&` part is redundant
because if `_combinedAttributes.has(CubemapAttribute.EnvironmentMap)` is true, the whole
expression is already true from the first disjunct.

The Java original:
```java
protected final boolean environmentCubemap = attribute.has(CubemapAttribute.EnvironmentMap)
    || (lighting && attribute.has(CubemapAttribute.EnvironmentMap));
```

This is the same redundancy in Java. It appears to be a copy-paste oversight where the intent
may have been `attributes.has(EnvironmentMap) || (lighting && attributes.has(SomeOtherThing))`.

**Severity:** Very Low — Inherited from Java, logic is harmlessly redundant.

### 23.14 MeshBuilder.scala — `vertex` method `cpOffset` check uses `>` instead of `>=`

**Line 504**

```scala
} else if (cpOffset > 0) {
```

This should be `cpOffset >= 0` since offset 0 is a valid vertex attribute offset. If the packed
color attribute happens to be at offset 0 in the vertex format, it will be skipped.

The Java original (`MeshBuilder.java`):
```java
} else if (cpOffset > 0) {
```

The same bug exists in LibGDX. In practice, the packed color attribute is rarely at offset 0
(the position attribute usually occupies that slot), but it's technically a bug that would
cause the packed color to be ignored if it were first in the vertex layout.

**Severity:** Low — Inherited from LibGDX, unlikely to trigger with standard vertex layouts
but technically incorrect.

### 23.15 ModelCache.scala — `SimpleMeshPool.obtain` calculates index count with potential bug for `indexCount = 0`

**Line 292**

```scala
val ic = Math.max(vc, 1 << (32 - Integer.numberOfLeadingZeros(indexCount - 1)))
```

When `indexCount = 0`, `indexCount - 1 = -1`, and `Integer.numberOfLeadingZeros(-1) = 0`,
so `1 << (32 - 0) = 1 << 32`. In Java/Scala, `1 << 32 = 1` (shift wraps modulo 32),
producing `ic = max(vc, 1)` which is fine. But `indexCount = 1` gives
`Integer.numberOfLeadingZeros(0) = 32`, so `1 << (32 - 32) = 1 << 0 = 1`, which gives
`ic = max(vc, 1)` — also fine.

The Java original has the same formula. This is a "round up to next power of 2" calculation
that handles edge cases through the modular arithmetic of bit shifts.

**Severity:** None — Correct (inherited from Java).

### 23.16 BaseAnimationController.scala — `applyAnimation` resets `isAnimated` before re-applying

**Lines 285–293**

```scala
for (node <- m.keys)
  node.isAnimated = false
for (nodeAnim <- animation.nodeAnimations)
  applyNodeAnimationBlending(nodeAnim, m, p, alpha, time)
for ((node, transform) <- m)
  if (!node.isAnimated) {
    node.isAnimated = true
    transform.lerp(node.translation, node.rotation, node.scale, alpha)
  }
```

The final loop lerps transforms for nodes that are in the map but were NOT animated by the
current animation. This handles blending between two animations where the second animation
doesn't affect all nodes. The Java original has identical logic. Functionally correct.

**Severity:** None — Correct port.

### 23.17 DefaultShader.scala — `combineAttributes` uses shared mutable `tmpAttributes`

**Line 1253–1261**

```scala
private val tmpAttributes: Attributes = Attributes()

private[shaders] def combineAttributes(renderable: Renderable): Attributes = {
    tmpAttributes.clear()
    renderable.environment.foreach(tmpAttributes.set(_))
    renderable.material.foreach(tmpAttributes.set(_))
    tmpAttributes
}
```

This method returns a reference to a **shared mutable singleton** (`tmpAttributes` in the
companion object). Any caller that stores or modifies the return value will affect all
subsequent calls. This is a common pattern in LibGDX for avoiding allocation, but it's
dangerous if the result is captured. In the current code, this is called from:
- `createPrefix()` — which reads from it immediately
- The constructor — which reads `_combinedAttributes` (a local copy)

At line 66, the constructor does:
```scala
private val _combinedAttributes: Attributes = DefaultShader.combineAttributes(renderable)
```

This stores a **reference** to `tmpAttributes`, not a copy. So `_combinedAttributes` and
`tmpAttributes` are the **same object**. Any subsequent call to `combineAttributes()` (e.g.,
when creating another `DefaultShader`) will clear and overwrite `_combinedAttributes` of the
first shader.

However, `_combinedAttributes` is only read during construction (to compute `attributesMask`,
`environmentCubemap`, `shadowMap`, etc.), and `combineAttributes` is only called during
construction of a new shader. Since shader construction is sequential (on the GL thread),
the reference aliasing doesn't cause issues in practice. But it's fragile.

**Severity:** Low — Thread-safe in practice (GL-thread-only), but a latent aliasing bug if
shader construction ever becomes non-sequential.

### 23.18 DepthShader.scala — `combineAttributes` duplicated in companion object

**Lines 193–201**

`DepthShader` companion object has its own `combineAttributes` that is identical to
`DefaultShader.combineAttributes`. The `canRender` method calls
`DepthShader.combineAttributes(renderable)` while the parent class uses
`DefaultShader.combineAttributes`. Both modify their own `tmpAttributes` singletons, but this
duplication means any fix to one must be mirrored in the other.

The Java original also has this duplication, with a TODO comment:
```java
// TODO: Move responsibility for combining attributes to RenderableProvider
```

**Severity:** Very Low — Code duplication inherited from Java.

### 23.19 Node.scala — `extendBoundingBox` always passes `transform=true` to children

**Lines 160–161**

```scala
for (child <- children)
  child.extendBoundingBox(out)
```

The `extendBoundingBox(BoundingBox, Boolean)` method takes a `transform` parameter, but when
recursing into children, it calls the single-arg `extendBoundingBox(out)` which defaults to
`transform = true`. This means even if the parent was called with `transform = false`, children
will always use their global transforms.

The Java original has the same behavior:
```java
for (final Node child : getChildren())
    child.extendBoundingBox(out);
```

**Severity:** Very Low — Inherited from Java. The `transform=false` overload is rarely used.

### 23.20 AnimationController.scala — `AnimationDesc.update` time wrapping may lose precision

**Lines 371–403**

The `update` method computes:
```scala
time = (time % duration).abs
```

When `speed < 0`:
```scala
var invTime = duration - time
loops = Math.abs(invTime / duration).toInt
invTime = (invTime % duration).abs
time = duration - invTime
```

The use of floating-point modulo for time wrapping can accumulate rounding errors over many
loops. This is the same algorithm as Java and is a known LibGDX limitation.

More importantly, the `loops` calculation uses `Math.abs(invTime / duration).toInt` where
both `invTime` and `duration` are `Seconds` (opaque type wrapping `Float`). The division
produces a `Seconds` value, and `.toInt` truncates. If `Seconds` division returns a `Seconds`
rather than a plain `Float`, the `.toInt` call may not exist on the opaque type, which could
cause a compilation error. But since the code compiles, the `Seconds` type apparently supports
`.toInt` conversion.

**Severity:** Very Low — Same precision characteristics as Java original.

### 23.21 ModelInstance.scala — `invalidate` bone rebinding may not properly handle ArrayMap mutation

**Lines 178–198**

The `invalidate(node)` method iterates over `bindPose` (an `ArrayMap`) and replaces keys:

```scala
part.invBoneBindTransforms.foreach { bindPose =>
    for (j <- 0 until bindPose.size) {
        val boneNode = bindPose.getKeyAt(j)
        getNode(boneNode.id).foreach { replacement =>
            bindPose.setKeyAt(j, replacement)
        }
    }
}
```

This replaces keys by index while iterating. If `setKeyAt` doesn't trigger a rehash (which it
shouldn't in an ArrayMap since it's backed by parallel arrays), this is safe. The Java original
does the same:

```java
for (int j = 0; j < bindPose.size; j++) {
    ...
    bindPose.keys[j] = replacement;
}
```

The migration notes flag this as a potential issue: "invalidate() bone rebinding logic may not
properly replace old ArrayMap keys." This depends on whether SGE's `ArrayMap.setKeyAt` properly
replaces the key without disrupting the array. If `setKeyAt` is implemented correctly (simple
array assignment), this is fine.

**Severity:** Low — Depends on `ArrayMap.setKeyAt` implementation correctness.

## 24. Migration Status Gap Analysis

Cross-referencing `migration-status.tsv`, `quality-issues.md`, `bugs-and-ambiguities.md`,
and `integration-test-gaps.md` to identify remaining gaps, risks, and missing functionality.

### 24.1 Not-Started Files — Desktop Backend (20 files)

The desktop backend (`backend-desktop`) has 20 not-started files, including critical P0
infrastructure:

**P0 (blocks desktop app launch):**
- `DesktopApplicationBase.scala` — base application lifecycle (from Lwjgl3ApplicationBase)
- `DesktopApplicationConfig.scala` — application configuration (from Lwjgl3ApplicationConfiguration)
- `DesktopWindowConfig.scala` — window configuration (from Lwjgl3WindowConfiguration)
- `DesktopWindowListener.scala` — window lifecycle callbacks (from Lwjgl3WindowListener)
- `DesktopFiles.scala` / `DesktopFileHandle.scala` — file I/O (from Lwjgl3Files/Lwjgl3FileHandle)
- `MiniaudioEngine.scala` — audio engine (rewrite from OpenAL to miniaudio)
- `DesktopSound.scala` / `DesktopMusic.scala` — sound/music playback

**P1 (important but not blocking launch):**
- `DesktopPreferences.scala` — user preferences persistence
- `DesktopNet.scala` — networking
- `DesktopClipboard.scala` — clipboard access
- `DesktopCursor.scala` — custom cursor support
- `FrameSync.scala` — frame rate synchronization (from Sync.java)
- `DesktopAudioDevice.scala` — raw audio output
- `WavDecoder.scala`, `OggDecoder.scala`, `Mp3Decoder.scala` — audio format decoders (core-shared)

**P2:** `DesktopAudioRecorder.scala` — microphone recording

**Impact:** Desktop applications can boot (`DesktopApplication` and `DesktopGraphics` are
done), but configuration, file I/O, audio, and window management are incomplete. The
headless backend (`HeadlessApplication`, P1) also depends on `DesktopFiles`/`DesktopFileHandle`.

### 24.2 Not-Started Files — Browser Backend (16 files)

The browser backend (`backend-browser`) has 16 not-started files:

**P0:**
- `BrowserApplicationConfig.scala` — application configuration
- `BrowserGraphics.scala` — graphics subsystem (canvas/WebGL management)
- `WebGL20.scala` / `WebGL30.scala` — WebGL bindings (handle-mapping from JS types)
- `BrowserInput.scala` — keyboard/mouse/touch input
- `BrowserAudio.scala` — Web Audio API
- `BrowserFiles.scala` / `BrowserFileHandle.scala` — virtual file system for web assets

**P1:**
- `BrowserLogger.scala` — console logging
- `BrowserNet.scala` — HTTP fetch API
- `BrowserPreferences.scala` — localStorage persistence
- `BrowserClipboard.scala` — clipboard API
- `BrowserCursor.scala` — CSS cursor management

**P2:**
- `BrowserSensors.scala` — accelerometer/gyroscope (3 source files merged)

**Impact:** `BrowserApplication` exists (done) but has no subsystem implementations.
Browser target is non-functional for any real application.

### 24.3 Not-Started Files — Android Backend (30 files)

The Android backend has 30 not-started files, all at P2 or P3:

**P2 (26 files):** Full Android application stack — Application, Graphics, GL20/GL30,
Input (touch/mouse/haptics), Audio (device/recorder/sound/music), Files, Net, Preferences,
Clipboard, Cursor, GLSurfaceView, EglConfigChooser, plus ResolutionStrategy variants
(4 files, core-shared).

**P3 (4 files):** `AsyncAndroidAudio.scala`, `AsyncAndroidSound.scala`, plus keyboard
height measurement (4 files: KeyboardHeightProvider, KeyboardHeightObserver,
StandardKeyboardHeightProvider, AndroidXKeyboardHeightProvider).

**Impact:** Android target is non-functional. All 30 files are P2/P3 priority.

### 24.4 Not-Started Files — Headless Backend (6 files)

- `HeadlessApplication.scala` (P1, core-shared) — headless app lifecycle + configuration
- `DesktopFiles.scala` / `DesktopFileHandle.scala` (P0, reuse from desktop)
- `DesktopNet.scala` / `DesktopPreferences.scala` (P1, reuse from desktop)

These overlap with desktop backend files — once desktop file/net/prefs are done, headless
gets them for free.

### 24.5 Deferred Files — iOS Backend (17 files)

All 17 deferred files are iOS backend, blocked on `scala-native-ios` toolchain support:

- `IOSApplication`, `IOSApplicationConfig`, `IOSLogger` — app lifecycle
- `IOSGraphics` — graphics subsystem
- `AngleGL20`/`AngleGL30` — shared with desktop (GLES via ANGLE)
- `IOSInput` — touch/gesture input
- `IOSAudio`, `DesktopMusic`/`DesktopSound` — shared miniaudio backend
- `IOSFiles`, `IOSFileHandle` — iOS file system
- `IOSNet`, `IOSPreferences`, `IOSHaptics` — platform services
- `IOSDevice`, `IOSScreenBounds` — device info

**Reason for deferral:** Scala Native does not yet support iOS as a compilation target.
The plan is to share GL bindings with desktop (ANGLE) and audio with desktop (miniaudio),
reducing unique iOS code once the toolchain exists.

### 24.6 High-Risk ai_converted Files — Complex Java Originals

These `ai_converted` files correspond to highly complex Java originals (500+ lines,
intricate state machines, or subtle algorithms) and are the most likely to harbor
undetected conversion bugs:

**Rendering pipeline (GL state machines):**
- `SpriteBatch.scala` / `PolygonSpriteBatch.scala` — batch rendering with flush/blend state
- `SpriteCache.scala` — cached sprite rendering
- `DefaultShader.scala` — ~1200-line uber-shader with dozens of uniforms
- `ShapeRenderer.scala` — immediate-mode GL with shape type state machine
- `GLFrameBuffer.scala` — FBO lifecycle and attachment management
- `MeshBuilder.scala` — vertex assembly with multiple primitive types

**Text rendering:**
- `GlyphLayout.scala` — complex line-wrapping, color markup, bidirectional text
- `BitmapFont.scala` / `BitmapFontCache.scala` — font parsing and glyph caching
- `TextField.scala` / `TextArea.scala` — text editing with selection, cursor, clipboard

**Layout and scene graph:**
- `Table.scala` — constraint-based layout (LibGDX's most complex widget)
- `Stage.scala` — event dispatch, hit testing, focus management
- `ScrollPane.scala` — touch/fling/overscroll physics
- `Tree.scala` — hierarchical expand/collapse with selection
- `SelectBox.scala` — dropdown with popup scrolling

**Asset and model loading:**
- `AssetManager.scala` — async dependency graph loading
- `Model.scala` / `ModelInstance.scala` — scene graph construction from model data
- `G3dModelLoader.scala` — JSON model parsing (~50 null-check patterns converted)
- `Skin.scala` — JSON-driven style resolution with type lookup

**Math and algorithms:**
- `Intersector.scala` — ~40 geometric intersection methods
- `EarClippingTriangulator.scala` / `DelaunayTriangulator.scala` — computational geometry
- `ParticleEmitter.scala` — particle simulation with many interpolated parameters

**Tiled maps:**
- `BaseTmxMapLoader.scala` / `BaseTmjMapLoader.scala` — XML/JSON map parsing
- `TmxMapLoader.scala` / `TmjMapLoader.scala` — concrete loaders with texture management

### 24.7 Known Bugs and Ambiguities — Current Status

From `bugs-and-ambiguities.md`: **All 13 critical bugs identified during audit have been
resolved.** No open ambiguities remain.

Resolved bugs included:
- OrientedBoundingBox axis computation (verified correct)
- Camera.rotate using wrong matrix multiply (fixed)
- CumulativeDistribution.ensureCapacity missing assignment (fixed)
- DistanceFieldFont vertex shader missing v_texCoords (fixed)
- BaseTiledMapLoader.loadProjectFile missing add() call (fixed)
- G3dModelLoader.parseMeshes reading wrong ID (fixed)
- Pixmap drawing methods were stubs (implemented)
- Mesh missing calculateRadius/scale/transform (added)
- MipMapGenerator public methods were stubs (implemented)
- ETC1TextureData.consumeCustomData commented out (implemented)
- 2 compression-package bugs removed with dead code deletion

### 24.8 Quality Issues — Remaining Deferred Items

From `quality-issues.md`, nearly all systemic issues are resolved. Remaining deferred items:

**`null.asInstanceOf` (9 occurrences, 4 files):**
- `ModelLoader.scala` (5) — changing parameter to `Nullable` cascades to ObjLoader,
  G3dModelLoader, and AssetManager
- `BaseShader.scala` (1) — setter interface `set(shader, id, null, null)` affects all
  shader implementations
- `TimSort.scala` (2) — generic `Array[T]`/`Ordering[T]` GC-assist nulling
- `Nullable.scala` (1) — intentional internal implementation

**TODO/FIXME markers (~22 remaining, all blocked or informational):**
- ~5 upstream FIXMEs inherited from LibGDX
- ~5 Pool.Poolable type class (blocked on design decision)
- ~4 Color immutability (blocked, future)
- ~3 dependency TODOs (scribe, scala-java-time, Gears)
- ~5 informational (ComparableTimSort, Show, JsonReader)

**Retained Java-style getters (7 files):**
- Container, Table (fluent builder pattern — intentional)
- ScrollPane, TextField (complex state — intentional)
- SelectBox, SgeList, Tree (validation/events logic — intentional)

### 24.9 Integration Test Gaps — Summary

From `integration-test-gaps.md`:

**Blocker:** Gdx2DPixmap has 14+ native method stubs. This blocks Texture upload,
SpriteBatch rendering, FBO readback, and the entire 2D/3D visual rendering pipeline on
all platforms.

**Desktop (6/6 IT checks pass, but rendering untested):**
- Rendering pipeline entirely blocked by Gdx2DPixmap
- Windowing (resize, iconify, fullscreen, multi-window) untested
- Input events (keyboard, mouse, scroll, cursor management) untested
- Deep audio (music streaming, multiple sounds, volume/pan/pitch) untested

**Browser (3 basic checks pass):**
- No subsystem-level checks (GL2D, GL3D, FileIO, JSON/XML, Input)
- No rendering verification (canvas pixel readback)
- No platform-specific checks (Web Audio, localStorage, clipboard, fetch, file loading)

**Android (1 smoke test, 5 embedded checks):**
- GL3D not checked
- Real audio playback not tested (emulator runs with -noaudio)
- Touch/sensor input not simulated
- Android-specific APIs (storage types, SharedPreferences, lifecycle) untested

**JVM Platform (77 tests, API shape + Panama symbol availability):**
- Buffer operations lack data correctness verification
- ETC1 encode/decode roundtrip not tested
- PKM header format not verified

**Cross-platform:**
- `sgeJS/compile` not verified in CI
- `sgeNative/compile` not verified in CI
- JS and Native unit tests not running

### 24.10 Migration Statistics Summary

| Category | Count | Notes |
|----------|-------|-------|
| Total files in TSV | 758 | core + all backends |
| `ai_converted` | 544 | core: 539, backend noop: 5 |
| `done` (hand-written backend) | 6 | DesktopApplication, DesktopGraphics, DesktopWindow, DefaultDesktopInput, AngleGL20-32, BrowserApplication |
| `skipped` | 99 | stdlib replacements (66 core) + backend-specific (33) |
| `not_started` | 92 | desktop: 20, browser: 16, android: 30, headless: 6, core-shared audio decoders: 3, core-shared resolution: 4, android keyboard: 4, headless app: 2, remaining: 7 |
| `deferred` | 17 | all iOS backend |
| Known bugs open | 0 | all 13 audit bugs resolved |
| Deferred `null.asInstanceOf` | 9 | across 4 files |
| Test gap blockers | 1 | Gdx2DPixmap native stubs |

### 24.11 Priority Assessment for Remaining Work

**P0 — Required for any desktop application to work:**
1. Gdx2DPixmap native implementation (unblocks all rendering)
2. DesktopApplicationBase + DesktopApplicationConfig (app lifecycle)
3. DesktopFiles + DesktopFileHandle (file I/O)
4. DesktopWindowConfig + DesktopWindowListener (window management)
5. MiniaudioEngine + DesktopSound + DesktopMusic (audio)

**P1 — Required for full desktop functionality:**
6. Audio format decoders (WAV, OGG, MP3) — core-shared
7. HeadlessApplication — enables server-side and testing use cases
8. Desktop preferences, networking, clipboard, cursor, frame sync
9. Integration tests for rendering pipeline (after Gdx2DPixmap)

**P2 — Browser and Android targets:**
10. Browser backend (16 files) — enables web deployment
11. Android backend (26 files) — enables mobile deployment

**P3 — iOS and niche features:**
12. iOS backend (17 files, deferred) — blocked on Scala Native iOS support
13. Android async audio, keyboard height measurement (4 files)

---

## 25. Executive Summary

### 25.1 Issue Statistics

| Severity | Count | Description |
|----------|-------|-------------|
| **Critical** | 17 | Produce wrong results or crash in normal usage; must fix before any release |
| **High** | 24 | Significant correctness or performance bugs affecting major subsystems |
| **Medium** | ~48 | Behavioral regressions, resource leaks, silent failures, performance issues |
| **Low** | ~65 | Style violations, API differences, edge cases, faithfully ported Java bugs |
| **Info/None** | ~15 | Design notes, verified-correct code, intentional differences |
| **Total** | **~169** | Across 22 analysis sections covering all major subsystems |

Additional systemic concerns not counted as individual issues:
- **12% test coverage** (66 test suites for 554 source files)
- **6 major packages with zero tests** at Critical risk level
- **15+ files** in shared source that cannot compile on Scala.js
- **~90 uses of `asInstanceOf`** across ~35 files

### 25.2 Top 10 Most Impactful Bugs

These bugs would prevent a basic game engine from functioning correctly:

| Rank | Issue | Section | Impact |
|------|-------|---------|--------|
| 1 | **Matrix4.mulLeft() is a no-op** — missing `set(tmpMat)` call | 14.2 | All camera transforms, bone transforms, and scene graph hierarchies using pre-multiplication are broken |
| 2 | **Matrix3 default constructor produces zero matrix** — missing `idt()` call | 14.13 | Every `new Matrix3()` is a zero matrix instead of identity; breaks all code expecting default identity |
| 3 | **Matrix3 tmp array M22=0** — translate/rotate/scale all broken | 14.3 | All 2D matrix transformations via Matrix3 project the third component to zero |
| 4 | **GlyphLayout.wrapGlyphs exits method** — boundary.break misuse | 11.1 | All BitmapFont text wrapping is broken; wrapped text produces garbled output or null |
| 5 | **GlyphLayout.truncateRun exits method** — boundary.break misuse | 16.2 | Text truncation ("Hello Wor...") never appends the truncation string |
| 6 | **BitmapFontCache.setAlphas is a no-op** — logic commented out | 11.4 | Any text fade in/out makes all text invisible (sets all colors to transparent black) |
| 7 | **Sort.sort(DynamicArray) is a no-op** for Comparable types | 13.1 | Sorts a copy of the array that is immediately discarded; original array unchanged |
| 8 | **Timer lifecycle listener removed at construction** | 13.10 | Timer never pauses/resumes with app lifecycle; timer thread never properly disposed |
| 9 | **Game class is unusable** — requires `(using Sge)` at construction | 21.16 | Core multi-screen API class cannot be instantiated in the standard bootstrapping pattern |
| 10 | **Affine2.set(Matrix3) uses wrong array indices** — row/column swap | 14.4 | Setting an Affine2 from a Matrix3 produces a completely garbled transformation |

Runners-up: Affine2.set(Matrix4) wrong indices (14.5), Vector3.rotateAroundRad stub (14.6),
MathUtils.tan() ignores range reduction (14.1), TimSort merge stubs (13.3),
PolygonSpriteBatch triangle indices broken (11.9), GLFrameBuffer wrong delete call (17.1).

### 25.3 Bug Pattern Categories

**1. boundary/break Misuse (6 bugs — 3 Critical, 3 High)**

Java `break`/`continue` inside loops was mechanically translated to
`scala.util.boundary.break()`, which exits the enclosing `boundary` block (typically
the entire method) instead of just the loop. This is the single most dangerous class
of porting bug, affecting text rendering, font caching, particle emission, and UI
button groups. A codebase-wide audit identified 6 confirmed bugs out of ~350 break
call sites.

Files: GlyphLayout.scala, BitmapFontCache.scala, ButtonGroup.scala, ParticleEmitter.scala

**2. Mutable Parameter / Dead Expression (12+ bugs — 7 Critical)**

Java code that reassigns method parameters or uses mutable local variables was ported
with Scala `val` instead of `var`, causing the reassignment to become a dead expression
(computed but discarded). This silently produces wrong results with no compiler warning.

Files: Matrices.scala (mulLeft, tmp init), Affine2.scala, ComparableTimSort.scala,
TimSort.scala (minGallop), Pool.scala (fill), Timer.scala (dispose)

**3. Memory Leaks in Collections (6 bugs — all High)**

Collection `remove()`, `clear()`, `pop()`, and `truncate()` operations don't null
vacated array slots, preventing GC from reclaiming removed objects. Since DynamicArray,
ObjectMap, ObjectSet, and ArrayMap are the most-used collections in the codebase, this
affects virtually every subsystem that removes or clears collection entries.

Files: DynamicArray.scala, ObjectMap.scala, ObjectSet.scala, ArrayMap.scala

**4. Stub/Incomplete Implementations (10+ bugs — Critical to High)**

Core algorithms left as placeholders: TimSort's merge operations fall back to
`Arrays.sort`, gallop methods are linear scans, `Vector3.rotateAroundRad` is a no-op,
shader/attribute `compareTo` always returns 0, and `BitmapFontCache.setAlphas` has its
core logic commented out.

Files: TimSort.scala, Sort.scala, Vectors.scala, BitmapFontCache.scala,
SpotLightsAttribute.scala, DefaultShader.scala

**5. Null Safety Violations (20+ locations — varied severity)**

Despite the project's "no null" rule, ~15 files use raw `null` assignments, 5 files use
`getOrElse(null)` patterns that expose null to callers, and several Nullable-wrapped
APIs silently swallow errors that Java would surface as NPEs. The `getOrElse(null)`
pattern in `DecalBatch.scala` is an immediate NPE waiting to happen.

Files: Stage.scala, GlyphLayout.scala, MapProperties.scala, DecalBatch.scala,
BaseTmxMapLoader.scala, ObjLoader.scala, AssetManager.scala, and others

### 25.4 Recommended Fix Priority

**P0 — Math Package (1 day, 7 bugs)**
Fix all Critical math issues: Matrix4.mulLeft, Matrix3 identity init, Matrix3 tmp[M22],
Affine2 indices (x2), MathUtils.tan range reduction, Vector3.rotateAroundRad stub.
These are mostly one-line fixes with massive blast radius.

**P1 — boundary/break Bugs (2 days, 6 bugs)**
Fix all boundary/break misuse: GlyphLayout.wrapGlyphs, GlyphLayout.truncateRun,
BitmapFontCache.draw, BitmapFontCache.setColors, ButtonGroup.canCheck,
ParticleEmitter.addParticles. Requires wrapping loops in their own `boundary {}` blocks
or using labeled breaks.

**P2 — Data Structure Correctness (3 days, 12+ bugs)**
Fix Sort.sort no-op, TimSort merge/gallop stubs, ComparableTimSort dead expression,
Pool.fill scoping, Pool.obtain O(n), and all collection memory leaks (DynamicArray,
ObjectMap, ObjectSet, ArrayMap null-clearing on removal/clear).

**P3 — GL/Rendering Pipeline (1 day, 4 bugs)**
Fix GLFrameBuffer.glDeleteRenderbuffer, GLFrameBuffer.close() colorBufferHandles,
VertexBufferObjectSubData.glBufferSubData, BitmapFontCache.setAlphas commented logic.

**P4 — Core Lifecycle & Timer (2 days, 3 bugs)**
Fix Game class chicken-and-egg (remove `(using Sge)`, implement SgeAware), Timer
lifecycle listener scoping, Timer exception handling. Requires design decisions.

**P5 — Asset Loading & HTTP (3 days, 6 bugs)**
Fix AssetLoadingTask null-vs-empty-array semantic mismatch (doubles loading time),
SgeHttpResponse binary corruption, SgeHttpClient double pool-free, and resource leaks
in file handle operations.

**P6 — Platform & Cross-Compilation (ongoing)**
Move 4 networking files to JVM-only source tree, abstract 7 BufferUtils factory methods
for Scala.js, redesign Timer.scala for JS compatibility, fix remaining null safety
violations and style issues.

### 25.5 Effort Estimates

| Priority | Bug Count | Est. Effort | Complexity | Notes |
|----------|-----------|-------------|------------|-------|
| **P0** Math | 7 | **1 day** | Low | Mostly 1–3 line fixes; highest ROI |
| **P1** boundary/break | 6 | **2 days** | Medium | Requires careful scoping analysis per site |
| **P2** Data structures | 12+ | **3 days** | Medium | Repetitive pattern; needs null-clearing + TimSort rewrite |
| **P3** GL pipeline | 4 | **1 day** | Low | Direct API call corrections |
| **P4** Core lifecycle | 3 | **2 days** | High | Design decisions for Game class and Timer |
| **P5** Asset/HTTP | 6 | **3 days** | Medium | Semantic changes in loading pipeline; sttp response type change |
| **P6** Platform | 15+ | **5+ days** | High | Architectural changes for Scala.js compatibility |
| **Total P0–P5** | **38** | **~12 days** | | Fixes all Critical and High bugs |
| **Total P0–P6** | **53+** | **~17 days** | | Full remediation including platform work |

**Key takeaway**: 7 one-line fixes in the math package (P0) would eliminate the most
damaging class of bugs in the entire codebase. The top 10 bugs are concentrated in just
4 files: `Matrices.scala`, `Affine2.scala`, `GlyphLayout.scala`, and `Sort.scala`.
