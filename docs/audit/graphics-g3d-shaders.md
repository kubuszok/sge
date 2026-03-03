# Audit: sge.graphics.g3d.shaders

Audited: 3/3 files | Pass: 2 | Minor: 1 | Major: 0
Last updated: 2026-03-03

---

### BaseShader.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/shaders/BaseShader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/shaders/BaseShader.java` |

All public methods match Java source: `register` (6 overloads), `getUniformID`, `getUniformAlias`,
`init`, `begin`, `render` (2 overloads), `end`, `close`, `has`, `loc`, `set` (Matrix4, Matrix3,
Vector3, Vector2, Color, TextureDescriptor, GLTexture), `setFloat` (4 overloads),
`setInt` (4 overloads).

Inner types correctly ported:
- `Validator` interface -> trait
- `Setter` interface -> trait (2 methods: `isGlobal`, `set`)
- `GlobalSetter` abstract class -> abstract class
- `LocalSetter` abstract class -> abstract class
- `Uniform` class -> class (4 constructors, `validate` method)

Type mappings:
- `IntIntMap` -> `ObjectMap[Int, Int]` (functional equivalent)
- `IntArray` -> `DynamicArray[Int]`
- `Array<String>` -> `DynamicArray[String]`
- `ShaderProgram program` (public) -> `var program: Nullable[ShaderProgram]`
- `RenderContext context` (public) -> `var context: Nullable[RenderContext]`
- `Camera camera` (public) -> `var camera: Nullable[Camera]`
- `dispose()` -> `close()` (AutoCloseable convention)

Conventions: split packages (correct), no return (boundary/break used), no null (Nullable used),
braces on all defs. `set(float)` renamed to `setFloat` and `set(int)` renamed to `setInt` to
disambiguate Scala overloads -- intentional and correct.

Minor difference: `begin()` passes `null.asInstanceOf[Renderable]` and `null.asInstanceOf[Attributes]`
to global setters, matching Java's null-passing semantics. This is an acceptable Java interop pattern.

### DepthShader.scala -- pass

| SGE path | `core/src/main/scala/sge/graphics/g3d/shaders/DepthShader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/shaders/DepthShader.java` |

All public methods match: `begin`, `end`, `canRender`, `render`.
5 constructors match Java's 5 constructors (reordered for Scala 3's secondary constructor rules).

Companion object:
- `Config` class extends `DefaultShader.Config` with `depthBufferOnly` and `defaultAlphaTest` fields
- `getDefaultVertexShader()` / `getDefaultFragmentShader()` (lazy-init, `(using Sge)`)
- `createPrefix(Renderable, Config)` -- delegates to `DefaultShader.createPrefix` + PackedDepthFlag
- `combineAttributes` -- private helper (matches Java private static)

`Config` primary constructor correctly sets `defaultCullFace = GL20.GL_FRONT` (line 150).
`Config` secondary constructor sets vertex/fragment shader (line 152-156) but does NOT reset
`defaultCullFace` because it delegates to `this()` which already sets it. This matches Java
behavior where the 2-arg constructor calls `super(vertexShader, fragmentShader)` on
`DefaultShader.Config`, which does not set `defaultCullFace` -- but the Java default constructor
explicitly sets it. Behavior is equivalent.

`numBones` val and `alphaTestAttribute` private val match Java `final int numBones` and
`private final FloatAttribute alphaTestAttribute`.

`Sge().files.classpath(...)` replaces `Gdx.files.classpath(...)`.

TODO comment preserved from Java source (line 185).

### DefaultShader.scala -- minor_issues

| SGE path | `core/src/main/scala/sge/graphics/g3d/shaders/DefaultShader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/shaders/DefaultShader.java` |

This is the largest file (1340 lines Scala, 958 lines Java). All major structures match.

**Config class** (companion object):
All 9 fields match Java: `vertexShader`, `fragmentShader`, `numDirectionalLights`, `numPointLights`,
`numSpotLights`, `numBones`, `numBoneWeights`, `ignoreUnimplemented`, `defaultCullFace`,
`defaultDepthFunc`. `vertexShader`/`fragmentShader` use `Nullable[String]` instead of null String.

**Inputs object** (companion):
All 25 uniform definitions match Java `Inputs` static fields exactly.

**Setters object** (companion):
All 22 setter instances match Java: `projTrans`, `viewTrans`, `projViewTrans`, `cameraPosition`,
`cameraDirection`, `cameraUp`, `cameraNearFar`, `worldTrans`, `viewWorldTrans`,
`projViewWorldTrans`, `normalMatrix`, `shininess`, `diffuseColor`, `diffuseTexture`,
`diffuseUVTransform`, `specularColor`, `specularTexture`, `specularUVTransform`, `emissiveColor`,
`emissiveTexture`, `emissiveUVTransform`, `reflectionColor`, `reflectionTexture`,
`reflectionUVTransform`, `normalTexture`, `normalUVTransform`, `ambientTexture`,
`ambientUVTransform`, `environmentCubemap`.
Inner classes `Bones` and `ACubemap` match Java.

**Instance fields** (all 18 uniform IDs, 17 light offset vars, plus lighting/shadow/cubemap booleans,
light arrays, ambientCubemap): all match Java.

**5 constructors**: match Java's 5-constructor chain.

**Methods**:
All present: `init`, `begin`, `render`, `end`, `close`, `canRender`, `compareTo`, `equals`,
`bindMaterial`, `bindLights`, `getDefaultCullFace`, `setDefaultCullFace`, `getDefaultDepthFunc`,
`setDefaultDepthFunc`.

**Static/companion members**:
`implementedFlags`, `defaultCullFace` (@deprecated), `defaultDepthFunc` (@deprecated),
`optionalAttributes`, `combineAttributes`, `combineAttributeMasks`, `createPrefix`,
`getDefaultVertexShader`, `getDefaultFragmentShader` -- all present.

**Minor issues found:**

1. **Stray `new Matrix3()` on line 325**: Java has `private final Matrix3 normalMatrix = new Matrix3()`
   as a field (used in `bindMaterial` in Java but not referenced in Scala). The Scala port has
   `new Matrix3()` as a bare expression (creates and discards an object). This is harmless but is
   dead code.

2. **Stray `new Vector3()` on line 403**: Similar to above -- Java has
   `private final Vector3 tmpV1 = new Vector3()` used in `bindLights`. The Scala port doesn't use
   a `tmpV1` in `bindLights`. This bare `new Vector3()` statement creates and discards an object.
   Harmless but dead code.

3. **`bindLights` control flow**: The Java version uses `continue` and `break` in light-binding
   loops. The Scala version emulates `continue` by nesting `if`/`else` blocks, and emulates `break`
   by setting `i = array.length`. This is functionally equivalent but more complex to read.

4. **`bindLights` uniform setting is inside `program.foreach`**: In Java, `program.setUniformf(...)`
   is called directly (program is never null at this point). The Scala port wraps every call in
   `program.foreach { prog => ... }`. This is safe but adds nesting. Correct behavior.

5. **`equals` method**: Java has two overloads: `equals(Object)` and `equals(DefaultShader)`.
   Scala has only `equals(Any)` with pattern match. Functionally equivalent.

All FIXME comments preserved from Java source.
