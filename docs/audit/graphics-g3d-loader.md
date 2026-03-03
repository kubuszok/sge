# Audit: sge.graphics.g3d.loader

Audited: 2/2 files | Pass: 0 | Minor: 2 | Major: 0
Last updated: 2026-03-03

---

### G3dModelLoader.scala -- minor_issues

| SGE path | `core/src/main/scala/sge/graphics/g3d/loader/G3dModelLoader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java` |

All methods present and match Java source:
- `loadModelData`, `parseModel`, `parseMeshes`, `parseType`, `parseAttributes`
- `parseMaterials`, `parseTextureUsage`, `parseColor`, `readVector2`
- `parseNodes`, `parseNodesRecursively`, `parseAnimations`

Companion object: `VERSION_HI` and `VERSION_LO` match Java static final constants.

Type mappings:
- `BaseJsonReader` -> `sge.utils.BaseJsonReader`
- `ModelLoader<ModelLoader.ModelParameters>` -> `ModelLoader[ModelLoader.ModelParameters]`
- `Array<T>` -> `DynamicArray[T]`
- `ArrayMap<String, Matrix4>` -> `ArrayMap[String, Matrix4]`
- `GdxRuntimeException` -> `SgeError.InvalidInput` / `SgeError.GraphicsError`
- `null` returns -> `Nullable[ModelData]`

JsonValue iteration: Java `for(child; !=null; =next)` -> Scala `while(isDefined)/foreach` pattern.
This works correctly as long as `.next` returns `Nullable[JsonValue]`.

Constructor: Java has 1-arg `G3dModelLoader(reader)` with `resolver=null` and 2-arg version.
Scala only has the 2-arg version (resolver is required). This is an intentional narrowing;
callers must provide a resolver. Since the 1-arg Java constructor passes `null` to
`ModelLoader(null)`, removing it avoids a null at the Scala level.

`reader` field: Java `protected final` -> Scala `val` (public). Visibility is wider but harmless.

**Minor issues found:**

1. **Bug in parseMeshes (line 66)**: The mesh part ID is read from the parent mesh node
   (`mesh.getString("id", ...)`) instead of the mesh part node (`meshPart.getString("id", ...)`).
   In Java (line 100), the code correctly reads `meshPart.getString("id", null)`. The Scala code
   on line 66 reads `mesh.getString("id", Nullable.empty)` -- this should be
   `meshPart.getString("id", Nullable.empty)`. This means mesh parts will all get the parent mesh's
   ID or throw if the parent mesh has no ID. **This is a functional bug.**

2. **parseColor**: Java version returns a `Color` with alpha=1.0f when `size >= 3`. Scala matches.
   However, Java does not check for size==4 to include alpha. Scala matches this behavior.

3. **parseAnimations**: The backwards-compatibility (v0.1) keyframe parsing and the v0.2 parsing
   both match Java exactly. `keytime` division by 1000.0f matches.

4. **tempQ field**: Java `protected final Quaternion tempQ` -> Scala `protected val tempQ`. Matches.

### ObjLoader.scala -- minor_issues

| SGE path | `core/src/main/scala/sge/graphics/g3d/loader/ObjLoader.scala` |
| Java source(s) | `com/badlogic/gdx/graphics/g3d/loader/ObjLoader.java` |

All public methods present: `loadModel(FileHandle, Boolean)`, `loadModelData` (2 overloads).
Private methods: `setActiveGroup`, `getIndex`.

Companion object:
- `logWarning` var (matches Java static boolean)
- `ObjLoaderParameters` class (matches Java static inner class)
- `Group` private class (matches Java private static inner class)
- `MtlLoader` private class (matches Java package-private `MtlLoader` class)

Type mappings:
- `FloatArray` -> `DynamicArray[Float]`
- `Array<Integer>` -> `DynamicArray[Int]`
- `Array<Group>` -> `DynamicArray[ObjLoader.Group]`

MtlLoader type mappings:
- `Array<ModelMaterial>` -> `DynamicArray[ModelMaterial]`
- `ObjMaterial` inner class: null String fields -> `Nullable[String]`
- `ObjMaterial.ambientColor`: null -> `Nullable[Color]`
- `ObjMaterial.build()`: uses `Nullable.foreach` instead of null checks

**Minor issues found:**

1. **No-arg constructor (line 44-46)**: Passes `null` to `this(null)` which passes `null` to
   `ModelLoader(resolver)`. This is a Java interop boundary and should have a `@nowarn` annotation
   and comment explaining the null usage. Currently bare `null` without annotation.

2. **Face parsing loop (line 107)**: Java has `for (int i = 1; i < tokens.length - 2; i--)` which
   is a known Java bug (should be `i++` not `i--`). The Scala version uses `i += 1` (line 118),
   which is the **corrected** behavior. This is an intentional fix during porting.

3. **Group.faces initial capacity**: Java initializes with `new Array<Integer>(200)`, Scala uses
   `DynamicArray[Int]()` (default capacity). Minor difference, not a functional issue.

4. **Empty group removal loop (lines 159-165)**: Java uses `i--` after `removeIndex(i)` to
   re-check the same index. Scala omits the `i += 1` in the removal branch, achieving the same
   effect via the `if/else` structure. Correct.

5. **`loadModelData` line 64**: Uses `var line: String = null` -- raw null assignment for Java
   `BufferedReader.readLine()` interop. This is acceptable at a Java I/O boundary but ideally
   should have a `@nowarn` comment.

6. **MtlLoader scope**: Java `MtlLoader` is a package-private top-level class. Scala nests it as
   a `private class` inside the `ObjLoader` companion object. This restricts visibility further
   but is correct since `MtlLoader` is only used within `ObjLoader`.

7. **loadModel return type**: Java `loadModel(FileHandle, boolean)` returns `Model` (from
   superclass). Scala returns `Nullable[Model]` -- matches the Nullable pattern used throughout.
