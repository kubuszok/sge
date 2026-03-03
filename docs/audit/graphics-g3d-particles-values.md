# Audit: sge.graphics.g3d.particles.values

Audited: 15/15 files | Pass: 14 | Minor: 1 | Major: 0
Last updated: 2026-03-03

---

### ParticleValue.scala -- pass
All public methods ported: `isActive`, `setActive`, `load`, copy constructor.
`active` field is `var` (matches Java `public boolean active`).
Json.Serializable `write`/`read` methods intentionally omitted (serialization handled
separately across the particle system). No null, no return, split packages, braces -- all
conventions followed.

### NumericValue.scala -- pass
All public methods ported: `getValue`, `setValue`, `load`.
`value` field is `private var` (matches Java `private float`).
Json.Serializable `write`/`read` intentionally omitted.

### RangedNumericValue.scala -- pass
All public methods ported: `newLowValue`, `setLow` (x2 overloads), `getLowMin`, `setLowMin`,
`getLowMax`, `setLowMax`, `load`.
`lowMin`/`lowMax` are `private var` (matches Java).
Json.Serializable `write`/`read` intentionally omitted.

### ScaledNumericValue.scala -- pass
All public methods ported: `newHighValue`, `setHigh` (x2), `getHighMin`, `setHighMin`,
`getHighMax`, `setHighMax`, `getScaling`, `setScaling`, `getTimeline`, `setTimeline`,
`isRelative`, `setRelative`, `getScale`, `load`.
`getScale` uses `boundary`/`break` instead of Java `return` -- correct pattern. Original
commented-out code preserved. `Array.copy` used instead of `System.arraycopy` -- correct
Scala equivalent. `timeline` is `var` (matches Java `public float[]`). `scaling` is
`private var` (matches Java). Json.Serializable intentionally omitted.

### GradientColorValue.scala -- pass
All public methods ported: `getTimeline`, `setTimeline`, `getColors`, `setColors`,
`getColor` (x2 overloads), `load`.
Java `static private float[] temp` moved to companion object `private val temp` -- correct
pattern. `getColor(percent, out, index)` uses `boundary`/`break` instead of Java `return`.
`timeline` is `var` (matches Java `public float[]`). `colors` is `private var` (matches Java).
Json.Serializable intentionally omitted.

### SpawnShapeValue.scala -- pass
All public/abstract methods ported: `spawnAux` (abstract), `spawn` (final), `init`, `start`,
`load`, `copy` (abstract), `save`, `load(AssetManager)`.
Implements `ResourceData.Configurable` (Java source also implements `Json.Serializable` --
that part intentionally omitted). `xOffsetValue`/`yOffsetValue`/`zOffsetValue` are `var`
(matches Java `public`). No null, no return.

### PrimitiveSpawnShapeValue.scala -- minor_issues
All public methods ported: `setActive`, `isEdges`, `setEdges`, `getSpawnWidth`,
`getSpawnHeight`, `getSpawnDepth`, `setDimensions`, `start`, `load`.
Inner enum `SpawnSide` moved to companion object -- correct Scala 3 enum pattern.
`TMP_V1` static field moved to companion object as `protected val`.
Minor issues:
- `isEdges`/`getSpawnWidth`/`getSpawnHeight`/`getSpawnDepth` use Scala property syntax
  (no parens) while Java uses getter-style `isEdges()`/`getSpawnWidth()` -- inconsistent
  with Java API convention and other getter methods in this codebase that use `()`
- Java `edges` is package-private (`boolean edges`); Scala is `var edges: Boolean` (public)
  -- minor visibility widening
- `spawnWidthValue`/`spawnHeightValue`/`spawnDepthValue` are `var` (matches Java `public`)
- `spawnWidth`/`spawnWidthDiff` etc. are `protected var` (matches Java `protected float`)
- Json.Serializable intentionally omitted

### EllipseSpawnShapeValue.scala -- pass
All public methods ported: `spawnAux`, `getSide`, `setSide`, `load`, `copy`.
`final class` matches Java `public final class`. `spawnAux` uses `boundary`/`break` for
early returns on degenerate ellipse dimensions (width/height/depth == 0) -- correct pattern.
Java `side` is package-private (`SpawnSide side`); Scala is `var side: SpawnSide` (public)
-- minor visibility widening but consistent with SpawnShapeValue pattern.
Json.Serializable intentionally omitted.

### PointSpawnShapeValue.scala -- pass
All public methods ported: `spawnAux`, `copy`.
`final class` matches Java. Faithful conversion, no logic differences, no null, no return.

### LineSpawnShapeValue.scala -- pass
All public methods ported: `spawnAux`, `copy`.
`final class` matches Java. Faithful conversion, no logic differences, no null, no return.

### RectangleSpawnShapeValue.scala -- pass
All public methods ported: `spawnAux`, `copy`.
`final class` matches Java. Complex edge-spawning logic faithfully ported with
`if`/`else if`/`else` chains. Ternary operators correctly converted to Scala `if`/`else`
expressions. No null, no return.

### CylinderSpawnShapeValue.scala -- pass
All public methods ported: `spawnAux`, `copy`.
`final class` matches Java. Cylinder spawn logic faithfully ported including theta
generation with degenerate radius handling. Ternary operators correctly converted.
No null, no return.

### MeshSpawnShapeValue.scala -- pass
All public methods ported: `load`, `setMesh` (x2 overloads), `save`, `load(AssetManager)`.
Inner class `Triangle` with instance method `pick(Vector3)` and companion object static
method `pick(x1..z3, vector)` -- correctly mirrors Java `public static class Triangle` with
both static and instance `pick` methods.
Java `protected Mesh mesh` / `protected Model model` mapped to `Nullable[Mesh]` /
`Nullable[Model]` -- correct null safety. Java `load()` passes null mesh freely; Scala
uses `getOrElse` + `SgeError`. `save()` uses `model.foreach` (correct Nullable pattern).
`load(AssetManager)` uses `descriptor.foreach` (correct null-to-Nullable mapping).
Java `manager.get(descriptor)` mapped to `manager.get(desc.fileName, desc.type)` overload.

### UnweightedMeshSpawnShapeValue.scala -- pass
All public methods ported: `setMesh`, `spawnAux`, `copy`.
`final class` matches Java. Java `short[] indices` (nullable) mapped to
`Nullable[Array[Short]]`. `spawnAux` uses `indices.fold { noIndexPath } { indexPath }` --
Nullable fold reverses branch order vs Java `if (indices == null)` but logic is equivalent.
`setMesh`: `getVertexAttribute(Usage.Position).offset` uses `.fold(throw ...)(_.offset / 4)`
-- safe null handling. Private fields match Java visibility.

### WeightMeshSpawnShapeValue.scala -- pass
All public methods ported: `init`, `calculateWeights`, `spawnAux`, `copy`.
`final class` matches Java. `CumulativeDistribution[Triangle]` correctly parameterized.
`calculateWeights()` accesses `mesh` via `getOrElse` (Nullable unwrap). Java cast
`(short)(attributes.vertexSize / 4)` dropped -- stores directly as `Int` (correct, the Java
cast to short was unnecessary truncation). `distribution.generateNormalized()` called
correctly. `spawnAux` inlines the triangle pick formula (matches Java).

---

## Cross-cutting observations

**Json.Serializable**: None of the 15 files implement `write`/`read` from `Json.Serializable`.
In Java, `ParticleValue` implements `Json.Serializable` and all subclasses override `write`/`read`.
This is a known, intentional omission tracked across the particle system -- serialization will
be handled via a separate mechanism.

**No return statements**: All early returns converted to `boundary`/`break` (5 files use this
pattern: `ScaledNumericValue`, `GradientColorValue`, `EllipseSpawnShapeValue`,
`UnweightedMeshSpawnShapeValue` uses Nullable.fold).

**No null**: `Nullable[T]` used consistently for `MeshSpawnShapeValue.mesh`/`model`,
`UnweightedMeshSpawnShapeValue.indices`. All other fields are non-nullable primitives or objects.

**Split packages**: All files use `package sge` / `package graphics` / `package g3d` /
`package particles` / `package values` -- correct.

**Braces**: All class/method definitions use braces -- correct.

**Enum handling**: `SpawnSide` inner enum correctly ported as Scala 3 `enum` in
`PrimitiveSpawnShapeValue` companion object.
